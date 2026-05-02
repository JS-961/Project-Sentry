from __future__ import annotations

import argparse
import copy
import re
import shutil
import tempfile
import zipfile
from dataclasses import dataclass, field
from pathlib import Path
from typing import Iterable, Sequence
import xml.etree.ElementTree as ET


W_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
R_NS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
PKG_REL_NS = "http://schemas.openxmlformats.org/package/2006/relationships"
CONTENT_TYPES_NS = "http://schemas.openxmlformats.org/package/2006/content-types"
XML_NS = "http://www.w3.org/XML/1998/namespace"
MC_NS = "http://schemas.openxmlformats.org/markup-compatibility/2006"
A_NS = "http://schemas.openxmlformats.org/drawingml/2006/main"
PIC_NS = "http://schemas.openxmlformats.org/drawingml/2006/picture"
WP_NS = "http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing"
IMAGE_REL_TYPE = "http://schemas.openxmlformats.org/officeDocument/2006/relationships/image"
EMU_PER_INCH = 914400
CONTENT_WIDTH_INCHES = 6.0
MAX_SINGLE_IMAGE_HEIGHT_INCHES = 7.0
MAX_GRID_IMAGE_HEIGHT_INCHES = 5.75
MAX_SURVEY_GRID_IMAGE_HEIGHT_INCHES = 1.6

NSMAP = {
    "a": "http://schemas.openxmlformats.org/drawingml/2006/main",
    "a14": "http://schemas.microsoft.com/office/drawing/2010/main",
    "cx": "http://schemas.microsoft.com/office/drawing/2014/chartex",
    "cx1": "http://schemas.microsoft.com/office/drawing/2015/9/8/chartex",
    "cx2": "http://schemas.microsoft.com/office/drawing/2015/10/21/chartex",
    "cx3": "http://schemas.microsoft.com/office/drawing/2016/5/9/chartex",
    "cx4": "http://schemas.microsoft.com/office/drawing/2016/5/10/chartex",
    "cx5": "http://schemas.microsoft.com/office/drawing/2016/5/11/chartex",
    "cx6": "http://schemas.microsoft.com/office/drawing/2016/5/12/chartex",
    "cx7": "http://schemas.microsoft.com/office/drawing/2016/5/13/chartex",
    "cx8": "http://schemas.microsoft.com/office/drawing/2016/5/14/chartex",
    "mc": MC_NS,
    "pic": "http://schemas.openxmlformats.org/drawingml/2006/picture",
    "r": R_NS,
    "w": W_NS,
    "w14": "http://schemas.microsoft.com/office/word/2010/wordml",
    "wp": "http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing",
    "wp14": "http://schemas.microsoft.com/office/word/2010/wordprocessingDrawing",
}

for prefix, uri in NSMAP.items():
    ET.register_namespace(prefix, uri)


def w_tag(name: str) -> str:
    return f"{{{W_NS}}}{name}"


def r_tag(name: str) -> str:
    return f"{{{R_NS}}}{name}"


def rel_tag(name: str) -> str:
    return f"{{{PKG_REL_NS}}}{name}"


def ct_tag(name: str) -> str:
    return f"{{{CONTENT_TYPES_NS}}}{name}"


def xml_tag(name: str) -> str:
    return f"{{{XML_NS}}}{name}"


def a_tag(name: str) -> str:
    return f"{{{A_NS}}}{name}"


def pic_tag(name: str) -> str:
    return f"{{{PIC_NS}}}{name}"


def wp_tag(name: str) -> str:
    return f"{{{WP_NS}}}{name}"


@dataclass
class Block:
    kind: str
    text: str | None = None
    items: list[str] = field(default_factory=list)
    rows: list[list[str]] = field(default_factory=list)
    info: str | None = None


@dataclass
class Section:
    level: int
    title: str
    raw_lines: list[str] = field(default_factory=list)
    children: list["Section"] = field(default_factory=list)


@dataclass
class BuildOptions:
    include_appendices: bool = False
    keep_template_guidelines: bool = False
    compact_tables: bool = True


@dataclass
class ImageAsset:
    source_path: Path
    package_name: str
    rel_id: str
    width_px: int
    height_px: int
    content_type: str
    doc_id: int


class ImageRegistry:
    def __init__(self, source_base: Path, rels_root: ET.Element) -> None:
        self.source_base = source_base
        self.rels_root = rels_root
        self.assets: list[ImageAsset] = []
        self._by_source: dict[Path, ImageAsset] = {}

    def register(self, image_ref: str) -> ImageAsset:
        raw_path = Path(image_ref)
        source_path = raw_path if raw_path.is_absolute() else self.source_base / raw_path
        source_path = source_path.resolve()
        if source_path in self._by_source:
            return self._by_source[source_path]
        if not source_path.exists():
            raise FileNotFoundError(f"Markdown image not found: {source_path}")

        width_px, height_px = read_image_dimensions(source_path)
        suffix = source_path.suffix.lower()
        content_type = image_content_type(suffix)
        asset_index = len(self.assets) + 1
        doc_id = 1000 + asset_index
        package_name = f"word/media/report_image_{asset_index}{suffix}"
        rel_id = next_relationship_id(self.rels_root)

        rel = ET.SubElement(self.rels_root, rel_tag("Relationship"))
        rel.set("Id", rel_id)
        rel.set("Type", IMAGE_REL_TYPE)
        rel.set("Target", package_name.removeprefix("word/"))

        asset = ImageAsset(
            source_path=source_path,
            package_name=package_name,
            rel_id=rel_id,
            width_px=width_px,
            height_px=height_px,
            content_type=content_type,
            doc_id=doc_id,
        )
        self.assets.append(asset)
        self._by_source[source_path] = asset
        return asset


def extract_text(element: ET.Element) -> str:
    texts = []
    for node in element.iter():
        if node.tag == w_tag("t") and node.text:
            texts.append(node.text)
    return "".join(texts).strip()


def is_page_break_paragraph(paragraph: ET.Element) -> bool:
    return paragraph.find(f".//{w_tag('br')}[@{w_tag('type')}='page']") is not None


def read_zip_xml(path: Path, entry_name: str) -> ET.Element:
    with zipfile.ZipFile(path, "r") as zf:
        return ET.fromstring(zf.read(entry_name))


def read_zip_bytes(path: Path, entry_name: str) -> bytes:
    with zipfile.ZipFile(path, "r") as zf:
        return zf.read(entry_name)


def image_content_type(suffix: str) -> str:
    normalized = suffix.lower().lstrip(".")
    if normalized == "png":
        return "image/png"
    if normalized in {"jpg", "jpeg"}:
        return "image/jpeg"
    raise ValueError(f"Unsupported image extension: {suffix}")


def read_image_dimensions(path: Path) -> tuple[int, int]:
    data = path.read_bytes()
    if data.startswith(b"\x89PNG\r\n\x1a\n") and len(data) >= 24:
        return int.from_bytes(data[16:20], "big"), int.from_bytes(data[20:24], "big")
    if data.startswith(b"\xff\xd8"):
        index = 2
        while index + 9 < len(data):
            if data[index] != 0xFF:
                index += 1
                continue
            marker = data[index + 1]
            index += 2
            if marker in {0xD8, 0xD9}:
                continue
            segment_length = int.from_bytes(data[index:index + 2], "big")
            if marker in {0xC0, 0xC1, 0xC2, 0xC3, 0xC5, 0xC6, 0xC7, 0xC9, 0xCA, 0xCB, 0xCD, 0xCE, 0xCF}:
                height = int.from_bytes(data[index + 3:index + 5], "big")
                width = int.from_bytes(data[index + 5:index + 7], "big")
                return width, height
            index += segment_length
    raise ValueError(f"Unsupported or unreadable image file: {path}")


def parse_markdown_sections(markdown_text: str) -> Section:
    root = Section(level=0, title="ROOT")
    stack = [root]

    for line in markdown_text.splitlines():
        heading = re.match(r"^(#{1,4})\s+(.*?)\s*$", line)
        if heading:
            level = len(heading.group(1))
            title = heading.group(2).strip()
            while stack and stack[-1].level >= level:
                stack.pop()
            new_section = Section(level=level, title=title)
            stack[-1].children.append(new_section)
            stack.append(new_section)
            continue
        stack[-1].raw_lines.append(line)

    return root


def is_table_separator(line: str) -> bool:
    stripped = line.strip()
    if not (stripped.startswith("|") and stripped.endswith("|")):
        return False
    cells = [cell.strip() for cell in stripped.strip("|").split("|")]
    return bool(cells) and all(re.fullmatch(r":?-{3,}:?", cell) for cell in cells)


def is_unordered_item(line: str) -> bool:
    return re.match(r"^\s*[-*]\s+.+", line) is not None


def is_ordered_item(line: str) -> bool:
    return re.match(r"^\s*\d+\.\s+.+", line) is not None


def parse_image_line(line: str) -> tuple[str, str] | None:
    match = re.fullmatch(r"!\[([^\]]*)\]\(([^)]+)\)", line.strip())
    if not match:
        return None
    return match.group(1).strip(), match.group(2).strip()


def parse_pipe_row(line: str) -> list[str]:
    stripped = line.strip().strip("|")
    return [cell.strip() for cell in stripped.split("|")]


def is_block_start(lines: Sequence[str], index: int) -> bool:
    line = lines[index]
    if not line.strip():
        return True
    if re.match(r"^(#{1,4})\s+", line):
        return True
    if line.startswith("```"):
        return True
    if parse_image_line(line) is not None:
        return True
    if is_unordered_item(line) or is_ordered_item(line):
        return True
    if line.strip().startswith("|"):
        if index + 1 < len(lines) and is_table_separator(lines[index + 1]):
            return True
    return False


def parse_blocks(lines: Sequence[str]) -> list[Block]:
    blocks: list[Block] = []
    index = 0

    while index < len(lines):
        line = lines[index]
        stripped = line.strip()

        if not stripped:
            index += 1
            continue

        if stripped.startswith("```"):
            info = stripped[3:].strip() or None
            index += 1
            code_lines: list[str] = []
            while index < len(lines) and not lines[index].strip().startswith("```"):
                code_lines.append(lines[index].rstrip("\n"))
                index += 1
            if index < len(lines) and lines[index].strip().startswith("```"):
                index += 1
            blocks.append(Block(kind="code", items=code_lines, info=info))
            continue

        image = parse_image_line(stripped)
        if image is not None:
            alt_text, image_path = image
            blocks.append(Block(kind="image", text=alt_text, info=image_path))
            index += 1
            continue

        if stripped.startswith("|") and index + 1 < len(lines) and is_table_separator(lines[index + 1]):
            table_lines = [stripped]
            index += 2
            while index < len(lines) and lines[index].strip().startswith("|"):
                table_lines.append(lines[index].strip())
                index += 1
            rows = [parse_pipe_row(row) for row in table_lines]
            blocks.append(Block(kind="table", rows=rows))
            continue

        if is_unordered_item(line):
            items: list[str] = []
            while index < len(lines) and is_unordered_item(lines[index]):
                items.append(re.sub(r"^\s*[-*]\s+", "", lines[index]).strip())
                index += 1
            blocks.append(Block(kind="ul", items=items))
            continue

        if is_ordered_item(line):
            items = []
            while index < len(lines) and is_ordered_item(lines[index]):
                items.append(re.sub(r"^\s*\d+\.\s+", "", lines[index]).strip())
                index += 1
            blocks.append(Block(kind="ol", items=items))
            continue

        paragraph_lines = [stripped]
        index += 1
        while index < len(lines):
            next_line = lines[index]
            if not next_line.strip():
                break
            if is_block_start(lines, index):
                break
            paragraph_lines.append(next_line.strip())
            index += 1
        paragraph_text = " ".join(paragraph_lines)
        if paragraph_text.startswith("> "):
            paragraph_text = paragraph_text[2:].strip()
        blocks.append(Block(kind="p", text=paragraph_text))

    return blocks


def find_section(root: Section, title: str) -> Section | None:
    if root.title == title:
        return root
    for child in root.children:
        found = find_section(child, title)
        if found is not None:
            return found
    return None


def child_by_title(section: Section, title: str) -> Section:
    for child in section.children:
        if child.title == title:
            return child
    raise KeyError(f"Missing section: {title}")


def markdown_inline_to_plain(text: str) -> str:
    text = re.sub(r"\[([^\]]+)\]\(([^)]+)\)", r"\1 (\2)", text)
    text = text.replace("**", "").replace("*", "").replace("`", "")
    return text.strip()


def inline_tokens(text: str) -> list[tuple[str, str]]:
    normalized = re.sub(r"\[([^\]]+)\]\(([^)]+)\)", r"\1 (\2)", text)
    pattern = re.compile(r"(`[^`]+`|\*\*.+?\*\*|\*[^*]+\*)")
    position = 0
    tokens: list[tuple[str, str]] = []

    for match in pattern.finditer(normalized):
        if match.start() > position:
            tokens.append(("text", normalized[position:match.start()]))
        token = match.group(0)
        if token.startswith("**") and token.endswith("**"):
            tokens.append(("bold", token[2:-2]))
        elif token.startswith("*") and token.endswith("*"):
            tokens.append(("italic", token[1:-1]))
        elif token.startswith("`") and token.endswith("`"):
            tokens.append(("code", token[1:-1]))
        position = match.end()

    if position < len(normalized):
        tokens.append(("text", normalized[position:]))

    return tokens


def is_figure_caption(text: str) -> bool:
    return re.match(r"^Figure\s+(?:\d+|[A-Z]\.\d+)\.", text) is not None


def is_table_caption(text: str) -> bool:
    return re.match(r"^Table\s+(?:\d+|[A-Z]\.\d+)\.", text) is not None


def is_placeholder_note(text: str) -> bool:
    return text.startswith("Platform:") or text.startswith("Image placeholder:")


def make_run(text: str, kind: str = "text") -> ET.Element:
    run = ET.Element(w_tag("r"))
    if kind != "text":
        run_properties = ET.SubElement(run, w_tag("rPr"))
        if kind == "bold":
            ET.SubElement(run_properties, w_tag("b"))
        elif kind == "italic":
            ET.SubElement(run_properties, w_tag("i"))
        elif kind == "code":
            fonts = ET.SubElement(run_properties, w_tag("rFonts"))
            fonts.set(w_tag("ascii"), "Courier New")
            fonts.set(w_tag("hAnsi"), "Courier New")
            ET.SubElement(run_properties, w_tag("sz")).set(w_tag("val"), "24")
    text_element = ET.SubElement(run, w_tag("t"))
    if text.startswith(" ") or text.endswith(" ") or "  " in text:
        text_element.set(xml_tag("space"), "preserve")
    text_element.text = text
    return run


def make_paragraph(
    text: str | None = None,
    *,
    style: str | None = None,
    align: str | None = None,
    runs: Sequence[tuple[str, str]] | None = None,
    spacing: bool = True,
    line_twips: str = "480",
    keep_next: bool = False,
    keep_lines: bool = False,
) -> ET.Element:
    paragraph = ET.Element(w_tag("p"))
    paragraph_properties = ET.SubElement(paragraph, w_tag("pPr"))

    if style:
        ET.SubElement(paragraph_properties, w_tag("pStyle")).set(w_tag("val"), style)
    if keep_next:
        ET.SubElement(paragraph_properties, w_tag("keepNext"))
    if keep_lines:
        ET.SubElement(paragraph_properties, w_tag("keepLines"))
    if spacing:
        spacing_element = ET.SubElement(paragraph_properties, w_tag("spacing"))
        spacing_element.set(w_tag("line"), line_twips)
        spacing_element.set(w_tag("lineRule"), "auto")
        spacing_element.set(w_tag("after"), "0")
    if align:
        ET.SubElement(paragraph_properties, w_tag("jc")).set(w_tag("val"), align)

    if runs is not None:
        for kind, value in runs:
            paragraph.append(make_run(value, kind))
    elif text:
        for kind, value in inline_tokens(text):
            paragraph.append(make_run(value, kind))

    return paragraph


def make_page_break_paragraph() -> ET.Element:
    paragraph = ET.Element(w_tag("p"))
    run = ET.SubElement(paragraph, w_tag("r"))
    break_element = ET.SubElement(run, w_tag("br"))
    break_element.set(w_tag("type"), "page")
    return paragraph


def max_image_width_inches(asset: ImageAsset) -> float:
    filename = asset.source_path.name.lower()
    if "android" in filename and asset.height_px > asset.width_px:
        return 3.0
    return CONTENT_WIDTH_INCHES


def image_extent_emu(
    asset: ImageAsset,
    *,
    max_width_inches: float,
    max_height_inches: float,
) -> tuple[int, int]:
    natural_width_inches = asset.width_px / 96.0
    width_inches = min(max_width_inches, natural_width_inches)
    height_inches = width_inches * asset.height_px / max(asset.width_px, 1)
    if height_inches > max_height_inches:
        scale = max_height_inches / height_inches
        width_inches *= scale
        height_inches = max_height_inches
    cx = int(width_inches * EMU_PER_INCH)
    cy = int(height_inches * EMU_PER_INCH)
    return cx, cy


def make_image_run(
    asset: ImageAsset,
    alt_text: str,
    *,
    max_width_inches: float,
    max_height_inches: float,
) -> ET.Element:
    cx, cy = image_extent_emu(
        asset,
        max_width_inches=max_width_inches,
        max_height_inches=max_height_inches,
    )

    run = ET.Element(w_tag("r"))
    drawing = ET.SubElement(run, w_tag("drawing"))
    inline = ET.SubElement(drawing, wp_tag("inline"))
    inline.set("distT", "0")
    inline.set("distB", "0")
    inline.set("distL", "0")
    inline.set("distR", "0")

    extent = ET.SubElement(inline, wp_tag("extent"))
    extent.set("cx", str(cx))
    extent.set("cy", str(cy))
    effect_extent = ET.SubElement(inline, wp_tag("effectExtent"))
    effect_extent.set("l", "0")
    effect_extent.set("t", "0")
    effect_extent.set("r", "0")
    effect_extent.set("b", "0")
    doc_properties = ET.SubElement(inline, wp_tag("docPr"))
    doc_properties.set("id", str(asset.doc_id))
    doc_properties.set("name", alt_text or asset.source_path.name)
    c_nv_graphic = ET.SubElement(inline, wp_tag("cNvGraphicFramePr"))
    locks = ET.SubElement(c_nv_graphic, a_tag("graphicFrameLocks"))
    locks.set("noChangeAspect", "1")

    graphic = ET.SubElement(inline, a_tag("graphic"))
    graphic_data = ET.SubElement(graphic, a_tag("graphicData"))
    graphic_data.set("uri", PIC_NS)
    pic = ET.SubElement(graphic_data, pic_tag("pic"))
    nv_pic_pr = ET.SubElement(pic, pic_tag("nvPicPr"))
    c_nv_pr = ET.SubElement(nv_pic_pr, pic_tag("cNvPr"))
    c_nv_pr.set("id", "0")
    c_nv_pr.set("name", asset.source_path.name)
    ET.SubElement(nv_pic_pr, pic_tag("cNvPicPr"))

    blip_fill = ET.SubElement(pic, pic_tag("blipFill"))
    blip = ET.SubElement(blip_fill, a_tag("blip"))
    blip.set(r_tag("embed"), asset.rel_id)
    stretch = ET.SubElement(blip_fill, a_tag("stretch"))
    ET.SubElement(stretch, a_tag("fillRect"))

    shape_properties = ET.SubElement(pic, pic_tag("spPr"))
    transform = ET.SubElement(shape_properties, a_tag("xfrm"))
    off = ET.SubElement(transform, a_tag("off"))
    off.set("x", "0")
    off.set("y", "0")
    ext = ET.SubElement(transform, a_tag("ext"))
    ext.set("cx", str(cx))
    ext.set("cy", str(cy))
    geometry = ET.SubElement(shape_properties, a_tag("prstGeom"))
    geometry.set("prst", "rect")
    ET.SubElement(geometry, a_tag("avLst"))
    return run


def make_image_paragraph(asset: ImageAsset, alt_text: str) -> ET.Element:
    paragraph = make_paragraph(style="Normal", align="center", line_twips="240", keep_next=True)
    paragraph.append(
        make_image_run(
            asset,
            alt_text,
            max_width_inches=max_image_width_inches(asset),
            max_height_inches=MAX_SINGLE_IMAGE_HEIGHT_INCHES,
        )
    )
    return paragraph


def make_image_grid_table(entries: Sequence[tuple[ImageAsset, str]]) -> ET.Element:
    columns = 2
    cell_width_twips = int(CONTENT_WIDTH_INCHES * 1440 / columns)
    cell_width_inches = CONTENT_WIDTH_INCHES / columns - 0.18
    max_height_inches = MAX_GRID_IMAGE_HEIGHT_INCHES
    if len(entries) > 2:
        max_height_inches = MAX_SURVEY_GRID_IMAGE_HEIGHT_INCHES

    table = ET.Element(w_tag("tbl"))
    table_properties = ET.SubElement(table, w_tag("tblPr"))
    ET.SubElement(table_properties, w_tag("tblW")).set(w_tag("w"), str(int(CONTENT_WIDTH_INCHES * 1440)))
    table_properties.find(w_tag("tblW")).set(w_tag("type"), "dxa")
    ET.SubElement(table_properties, w_tag("jc")).set(w_tag("val"), "center")
    ET.SubElement(table_properties, w_tag("tblLayout")).set(w_tag("type"), "fixed")
    borders = ET.SubElement(table_properties, w_tag("tblBorders"))
    for border_name in ("top", "left", "bottom", "right", "insideH", "insideV"):
        ET.SubElement(borders, w_tag(border_name)).set(w_tag("val"), "nil")

    grid = ET.SubElement(table, w_tag("tblGrid"))
    for _ in range(columns):
        ET.SubElement(grid, w_tag("gridCol")).set(w_tag("w"), str(cell_width_twips))

    for row_start in range(0, len(entries), columns):
        row = ET.SubElement(table, w_tag("tr"))
        row_properties = ET.SubElement(row, w_tag("trPr"))
        ET.SubElement(row_properties, w_tag("cantSplit"))
        for asset, alt_text in entries[row_start:row_start + columns]:
            cell = ET.SubElement(row, w_tag("tc"))
            cell_properties = ET.SubElement(cell, w_tag("tcPr"))
            ET.SubElement(cell_properties, w_tag("tcW")).set(w_tag("w"), str(cell_width_twips))
            cell_properties.find(w_tag("tcW")).set(w_tag("type"), "dxa")
            ET.SubElement(cell_properties, w_tag("vAlign")).set(w_tag("val"), "center")
            cell_margins = ET.SubElement(cell_properties, w_tag("tcMar"))
            for margin_name in ("top", "left", "bottom", "right"):
                margin = ET.SubElement(cell_margins, w_tag(margin_name))
                margin.set(w_tag("w"), "60")
                margin.set(w_tag("type"), "dxa")
            paragraph = make_paragraph(style="Normal", align="center", line_twips="240")
            paragraph.append(
                make_image_run(
                    asset,
                    alt_text,
                    max_width_inches=cell_width_inches,
                    max_height_inches=max_height_inches,
                )
            )
            cell.append(paragraph)
        for _ in range(columns - len(entries[row_start:row_start + columns])):
            cell = ET.SubElement(row, w_tag("tc"))
            cell_properties = ET.SubElement(cell, w_tag("tcPr"))
            ET.SubElement(cell_properties, w_tag("tcW")).set(w_tag("w"), str(cell_width_twips))
            cell_properties.find(w_tag("tcW")).set(w_tag("type"), "dxa")
            cell.append(make_paragraph("", style="Normal", line_twips="240"))

    return table


def make_section_properties(
    *,
    default_footer_rid: str | None,
    first_footer_rid: str | None = None,
    page_format: str | None = None,
    page_start: int | None = None,
    next_page: bool,
) -> ET.Element:
    section = ET.Element(w_tag("sectPr"))

    if default_footer_rid:
        footer = ET.SubElement(section, w_tag("footerReference"))
        footer.set(w_tag("type"), "default")
        footer.set(r_tag("id"), default_footer_rid)

    if first_footer_rid:
        footer = ET.SubElement(section, w_tag("footerReference"))
        footer.set(w_tag("type"), "first")
        footer.set(r_tag("id"), first_footer_rid)

    if next_page:
        ET.SubElement(section, w_tag("type")).set(w_tag("val"), "nextPage")

    page_size = ET.SubElement(section, w_tag("pgSz"))
    page_size.set(w_tag("w"), "12240")
    page_size.set(w_tag("h"), "15840")

    page_margins = ET.SubElement(section, w_tag("pgMar"))
    page_margins.set(w_tag("top"), "1440")
    page_margins.set(w_tag("right"), "1440")
    page_margins.set(w_tag("bottom"), "1440")
    page_margins.set(w_tag("left"), "2160")
    page_margins.set(w_tag("header"), "720")
    page_margins.set(w_tag("footer"), "720")
    page_margins.set(w_tag("gutter"), "0")

    if page_format or page_start is not None:
        page_number = ET.SubElement(section, w_tag("pgNumType"))
        if page_format:
            page_number.set(w_tag("fmt"), page_format)
        if page_start is not None:
            page_number.set(w_tag("start"), str(page_start))

    ET.SubElement(section, w_tag("cols")).set(w_tag("space"), "720")
    ET.SubElement(section, w_tag("docGrid")).set(w_tag("linePitch"), "360")
    return section


def make_section_break_paragraph(section_properties: ET.Element) -> ET.Element:
    paragraph = ET.Element(w_tag("p"))
    paragraph_properties = ET.SubElement(paragraph, w_tag("pPr"))
    paragraph_properties.append(section_properties)
    return paragraph


def make_table(rows: list[list[str]], *, compact: bool) -> ET.Element:
    table = ET.Element(w_tag("tbl"))
    table_properties = ET.SubElement(table, w_tag("tblPr"))
    style = ET.SubElement(table_properties, w_tag("tblStyle"))
    style.set(w_tag("val"), "TableGrid")
    width = ET.SubElement(table_properties, w_tag("tblW"))
    width.set(w_tag("w"), "0")
    width.set(w_tag("type"), "auto")

    borders = ET.SubElement(table_properties, w_tag("tblBorders"))
    for border_name in ("top", "left", "bottom", "right", "insideH", "insideV"):
        border = ET.SubElement(borders, w_tag(border_name))
        border.set(w_tag("val"), "single")
        border.set(w_tag("sz"), "8")
        border.set(w_tag("space"), "0")
        border.set(w_tag("color"), "000000")

    grid = ET.SubElement(table, w_tag("tblGrid"))
    column_count = max(len(row) for row in rows)
    for _ in range(column_count):
        ET.SubElement(grid, w_tag("gridCol")).set(w_tag("w"), str(9000 // max(column_count, 1)))

    header = rows[0]
    data_rows = rows[1:]

    table_line = "240" if compact else "480"

    def add_row(values: list[str], *, header_row: bool) -> None:
        row_element = ET.SubElement(table, w_tag("tr"))
        padded = values + [""] * (column_count - len(values))
        for cell_text in padded:
            cell = ET.SubElement(row_element, w_tag("tc"))
            cell_properties = ET.SubElement(cell, w_tag("tcPr"))
            ET.SubElement(cell_properties, w_tag("tcW")).set(w_tag("type"), "auto")
            paragraph = ET.SubElement(cell, w_tag("p"))
            paragraph_properties = ET.SubElement(paragraph, w_tag("pPr"))
            spacing_element = ET.SubElement(paragraph_properties, w_tag("spacing"))
            spacing_element.set(w_tag("line"), table_line)
            spacing_element.set(w_tag("lineRule"), "auto")
            spacing_element.set(w_tag("after"), "0")
            for kind, value in inline_tokens(cell_text):
                run_kind = "bold" if header_row and kind == "text" else kind
                paragraph.append(make_run(value, run_kind))

    add_row(header, header_row=True)
    for row in data_rows:
        add_row(row, header_row=False)

    return table


def render_blocks(
    blocks: Sequence[Block],
    elements: list[ET.Element],
    *,
    compact_tables: bool,
    image_registry: ImageRegistry,
) -> None:
    index = 0
    while index < len(blocks):
        block = blocks[index]
        if block.kind == "p" and block.text:
            if is_figure_caption(block.text) or is_table_caption(block.text):
                caption_align = "center" if is_figure_caption(block.text) else None
                elements.append(make_paragraph(block.text, style="Caption", align=caption_align, line_twips="240"))
            elif is_placeholder_note(block.text):
                elements.append(
                    make_paragraph(
                        style="Normal",
                        runs=[("italic", block.text)],
                        line_twips="240",
                    )
                )
            else:
                elements.append(make_paragraph(block.text, style="Normal"))
        elif block.kind == "ul":
            for item in block.items:
                elements.append(make_paragraph(item, style="ListBullet"))
        elif block.kind == "ol":
            for item_number, item in enumerate(block.items, start=1):
                elements.append(make_paragraph(f"{item_number}. {item}", style="Normal"))
        elif block.kind == "table":
            elements.append(make_table(block.rows, compact=compact_tables))
        elif block.kind == "image" and block.info:
            image_blocks = [block]
            while index + len(image_blocks) < len(blocks):
                candidate = blocks[index + len(image_blocks)]
                if candidate.kind != "image" or not candidate.info:
                    break
                image_blocks.append(candidate)
            next_block_index = index + len(image_blocks)
            next_block = blocks[next_block_index] if next_block_index < len(blocks) else None
            if len(image_blocks) > 1 and next_block and next_block.kind == "p" and next_block.text and is_figure_caption(next_block.text):
                entries = [
                    (
                        image_registry.register(image_block.info or ""),
                        image_block.text or Path(image_block.info or "").name,
                    )
                    for image_block in image_blocks
                ]
                elements.append(make_image_grid_table(entries))
                index += len(image_blocks)
                continue
            asset = image_registry.register(block.info)
            elements.append(make_image_paragraph(asset, block.text or asset.source_path.name))
        elif block.kind == "code":
            for line in block.items:
                code_runs = [("code", line if line else " ")]
                elements.append(make_paragraph(style="Normal", runs=code_runs, line_twips="240"))
        index += 1


def render_section(
    section: Section,
    elements: list[ET.Element],
    *,
    heading_style_override: str | None = None,
    compact_tables: bool,
    image_registry: ImageRegistry,
) -> None:
    style_map = {2: "Heading1", 3: "Heading2", 4: "Heading3"}
    heading_style = heading_style_override or style_map.get(section.level, "Heading3")
    elements.append(make_paragraph(section.title, style=heading_style, keep_next=True, keep_lines=True))
    render_blocks(parse_blocks(section.raw_lines), elements, compact_tables=compact_tables, image_registry=image_registry)
    for child in section.children:
        render_section(child, elements, compact_tables=compact_tables, image_registry=image_registry)


def extract_list_items(section: Section) -> list[str]:
    items: list[str] = []
    for block in parse_blocks(section.raw_lines):
        if block.kind in {"ul", "ol"}:
            items.extend(block.items)
    return items


def parse_key_value_items(items: Iterable[str]) -> dict[str, str]:
    pairs: dict[str, str] = {}
    for item in items:
        if ":" not in item:
            continue
        key, value = item.split(":", 1)
        pairs[key.strip()] = markdown_inline_to_plain(value.strip())
    return pairs


def build_title_page(title_section: Section, logo_paragraph: ET.Element | None) -> list[ET.Element]:
    items = parse_key_value_items(extract_list_items(title_section))
    elements: list[ET.Element] = []
    if logo_paragraph is not None:
        elements.append(copy.deepcopy(logo_paragraph))
    elements.append(make_paragraph("", align="center"))

    project_title = items.get("Project Title", "Project Sentry")
    elements.append(make_paragraph(project_title, style="Title", align="center"))
    elements.append(make_paragraph("", align="center"))

    for key in (
        "Student Name(s)",
        "Student ID(s)",
        "Supervisor/Advisor",
        "Department",
        "Course",
        "Semester",
        "Submission Date",
    ):
        value = items.get(key, "TBD")
        elements.append(make_paragraph(f"{key}: {value}", style="Normal", align="center"))
    return elements


def build_signature_page(signature_section: Section) -> list[ET.Element]:
    items = extract_list_items(signature_section)
    elements = [make_paragraph("SIGNATURE PAGE", style="Heading1", align="center")]
    for item in items:
        elements.append(make_paragraph(markdown_inline_to_plain(item), style="Normal"))
    return elements


def extract_template_parts(template_doc_root: ET.Element) -> tuple[ET.Element | None, list[ET.Element]]:
    body = template_doc_root.find(w_tag("body"))
    if body is None:
        return None, []

    paragraphs = [element for element in list(body) if element.tag == w_tag("p")]
    logo_paragraph: ET.Element | None = None
    for paragraph in paragraphs:
        if paragraph.find(f".//{w_tag('drawing')}") is not None:
            logo_paragraph = copy.deepcopy(paragraph)
            break

    start_index = None
    end_index = None
    for idx, paragraph in enumerate(paragraphs):
        text = extract_text(paragraph)
        if start_index is None and text == "General guidelines":
            start_index = idx
        if text == "Acknowledgments":
            end_index = idx
            break

    instruction_paragraphs: list[ET.Element] = []
    if start_index is not None and end_index is not None and start_index < end_index:
        for paragraph in paragraphs[start_index:end_index]:
            instruction_paragraphs.append(copy.deepcopy(paragraph))
        while instruction_paragraphs and not extract_text(instruction_paragraphs[-1]) and is_page_break_paragraph(instruction_paragraphs[-1]):
            instruction_paragraphs.pop()

    return logo_paragraph, instruction_paragraphs


def update_styles_xml(styles_root: ET.Element) -> bytes:
    def ensure_child(parent: ET.Element, child_name: str) -> ET.Element:
        child = parent.find(w_tag(child_name))
        if child is None:
            child = ET.SubElement(parent, w_tag(child_name))
        return child

    def set_fonts_and_size(run_properties: ET.Element, *, size: str | None = None) -> None:
        fonts = ensure_child(run_properties, "rFonts")
        fonts.set(w_tag("ascii"), "Times New Roman")
        fonts.set(w_tag("hAnsi"), "Times New Roman")
        fonts.set(w_tag("cs"), "Times New Roman")
        if size is not None:
            ensure_child(run_properties, "sz").set(w_tag("val"), size)
            ensure_child(run_properties, "szCs").set(w_tag("val"), size)

    def set_color(run_properties: ET.Element, color: str) -> None:
        color_element = ensure_child(run_properties, "color")
        color_element.set(w_tag("val"), color)
        for themed_attr in ("themeColor", "themeTint", "themeShade"):
            color_element.attrib.pop(w_tag(themed_attr), None)

    def set_spacing(paragraph_properties: ET.Element) -> None:
        spacing = ensure_child(paragraph_properties, "spacing")
        spacing.set(w_tag("line"), "480")
        spacing.set(w_tag("lineRule"), "auto")
        spacing.set(w_tag("after"), "0")

    doc_defaults = styles_root.find(w_tag("docDefaults"))
    if doc_defaults is not None:
        run_default = doc_defaults.find(f"{w_tag('rPrDefault')}/{w_tag('rPr')}")
        if run_default is None:
            rpr_default = ensure_child(doc_defaults, "rPrDefault")
            run_default = ensure_child(rpr_default, "rPr")
        set_fonts_and_size(run_default, size="24")

        paragraph_default = doc_defaults.find(f"{w_tag('pPrDefault')}/{w_tag('pPr')}")
        if paragraph_default is None:
            ppr_default = ensure_child(doc_defaults, "pPrDefault")
            paragraph_default = ensure_child(ppr_default, "pPr")
        set_spacing(paragraph_default)

    body_styles = {
        "Normal",
        "ListParagraph",
        "ListBullet",
        "ListBullet2",
        "ListBullet3",
        "ListNumber",
        "ListNumber2",
        "ListNumber3",
        "Footer",
    }
    heading_styles = {"Heading1", "Heading2", "Heading3", "Title", "Subtitle"}

    for style in styles_root.findall(w_tag("style")):
        style_id = style.get(w_tag("styleId"))
        if not style_id:
            continue
        if style_id in body_styles:
            run_properties = ensure_child(style, "rPr")
            paragraph_properties = ensure_child(style, "pPr")
            set_fonts_and_size(run_properties, size="24")
            set_color(run_properties, "000000")
            set_spacing(paragraph_properties)
        elif style_id in heading_styles:
            run_properties = ensure_child(style, "rPr")
            paragraph_properties = ensure_child(style, "pPr")
            set_fonts_and_size(run_properties, size=None)
            color_map = {
                "Title": "183B3F",
                "Heading1": "183B3F",
                "Heading2": "243B4A",
                "Heading3": "333333",
                "Subtitle": "555555",
            }
            set_color(run_properties, color_map.get(style_id, "000000"))
            ensure_child(run_properties, "b")
            keep_next = ensure_child(paragraph_properties, "keepNext")
            keep_next.attrib.clear()
            ensure_child(paragraph_properties, "keepLines")
            set_spacing(paragraph_properties)
        elif style_id == "Caption":
            run_properties = ensure_child(style, "rPr")
            paragraph_properties = ensure_child(style, "pPr")
            set_fonts_and_size(run_properties, size="24")
            set_color(run_properties, "444444")
            ensure_child(run_properties, "i")
            set_spacing(paragraph_properties)

    return ET.tostring(styles_root, encoding="utf-8", xml_declaration=True)


def build_footer_xml(centered: bool) -> bytes:
    footer = ET.Element(w_tag("ftr"))
    paragraph = ET.SubElement(footer, w_tag("p"))
    paragraph_properties = ET.SubElement(paragraph, w_tag("pPr"))
    ET.SubElement(paragraph_properties, w_tag("pStyle")).set(w_tag("val"), "Footer")
    if centered:
        ET.SubElement(paragraph_properties, w_tag("jc")).set(w_tag("val"), "center")

    if centered:
        run = ET.SubElement(paragraph, w_tag("r"))
        ET.SubElement(run, w_tag("fldChar")).set(w_tag("fldCharType"), "begin")
        run = ET.SubElement(paragraph, w_tag("r"))
        instruction = ET.SubElement(run, w_tag("instrText"))
        instruction.set(xml_tag("space"), "preserve")
        instruction.text = " PAGE "
        run = ET.SubElement(paragraph, w_tag("r"))
        ET.SubElement(run, w_tag("fldChar")).set(w_tag("fldCharType"), "separate")
        run = ET.SubElement(paragraph, w_tag("r"))
        ET.SubElement(run, w_tag("t")).text = "1"
        run = ET.SubElement(paragraph, w_tag("r"))
        ET.SubElement(run, w_tag("fldChar")).set(w_tag("fldCharType"), "end")

    return ET.tostring(footer, encoding="utf-8", xml_declaration=True)


def next_relationship_id(rels_root: ET.Element) -> str:
    highest = 0
    for rel in rels_root.findall(rel_tag("Relationship")):
        rel_id = rel.get("Id", "")
        match = re.fullmatch(r"rId(\d+)", rel_id)
        if match:
            highest = max(highest, int(match.group(1)))
    return f"rId{highest + 1}"


def ensure_footer_relationship(rels_root: ET.Element) -> str:
    footer2_id: str | None = None
    for rel in rels_root.findall(rel_tag("Relationship")):
        if rel.get("Target") == "footer2.xml":
            footer2_id = rel.get("Id")
            break

    if footer2_id is None:
        footer2_id = next_relationship_id(rels_root)
        rel = ET.SubElement(rels_root, rel_tag("Relationship"))
        rel.set("Id", footer2_id)
        rel.set("Type", "http://schemas.openxmlformats.org/officeDocument/2006/relationships/footer")
        rel.set("Target", "footer2.xml")

    return footer2_id


def ensure_footer_override(content_types_root: ET.Element, image_assets: Sequence[ImageAsset] = ()) -> bytes:
    expected_part = "/word/footer2.xml"
    has_footer_override = False
    for override in content_types_root.findall(ct_tag("Override")):
        if override.get("PartName") == expected_part:
            has_footer_override = True
            break

    if not has_footer_override:
        override = ET.SubElement(content_types_root, ct_tag("Override"))
        override.set("PartName", expected_part)
        override.set(
            "ContentType",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.footer+xml",
        )

    existing_defaults = {
        default.get("Extension", "").lower()
        for default in content_types_root.findall(ct_tag("Default"))
    }
    for asset in image_assets:
        extension = asset.source_path.suffix.lower().lstrip(".")
        if extension not in existing_defaults:
            default = ET.SubElement(content_types_root, ct_tag("Default"))
            default.set("Extension", extension)
            default.set("ContentType", asset.content_type)
            existing_defaults.add(extension)

    return ET.tostring(content_types_root, encoding="utf-8", xml_declaration=True)


def build_document_xml(
    template_doc_root: ET.Element,
    root_section: Section,
    *,
    default_footer_rid: str,
    blank_footer_rid: str,
    options: BuildOptions,
    image_registry: ImageRegistry,
) -> bytes:
    draft_root = root_section.children[0] if root_section.children and root_section.children[0].level == 1 else root_section
    preliminary_pages = child_by_title(draft_root, "Preliminary Pages")
    title_section = child_by_title(preliminary_pages, "Title Page")
    signature_section = child_by_title(preliminary_pages, "Signature Page")
    acknowledgments = child_by_title(preliminary_pages, "Acknowledgments")
    abstract = child_by_title(preliminary_pages, "Abstract")
    toc = child_by_title(preliminary_pages, "Table of Contents")
    lof = child_by_title(preliminary_pages, "List of Figures")
    lot = child_by_title(preliminary_pages, "List of Tables")

    chapters = [child for child in draft_root.children if re.match(r"^\d+\.", child.title)]
    references = child_by_title(draft_root, "References")
    appendices = child_by_title(draft_root, "Appendices")

    logo_paragraph, instruction_paragraphs = extract_template_parts(template_doc_root)

    document = ET.Element(template_doc_root.tag, template_doc_root.attrib)
    body = ET.SubElement(document, w_tag("body"))

    elements: list[ET.Element] = []

    elements.extend(build_title_page(title_section, logo_paragraph))
    title_section_break = make_section_break_paragraph(
        make_section_properties(
            default_footer_rid=blank_footer_rid,
            page_format=None,
            page_start=None,
            next_page=True,
        )
    )
    elements.append(title_section_break)

    elements.extend(build_signature_page(signature_section))

    if options.keep_template_guidelines:
        for paragraph in instruction_paragraphs:
            elements.append(copy.deepcopy(paragraph))

    for prelim_section in (acknowledgments, abstract, toc, lof, lot):
        elements.append(make_page_break_paragraph())
        render_section(
            prelim_section,
            elements,
            heading_style_override="Heading1",
            compact_tables=options.compact_tables,
            image_registry=image_registry,
        )

    prelim_section_break = make_section_break_paragraph(
        make_section_properties(
            default_footer_rid=default_footer_rid,
            page_format="lowerRoman",
            page_start=1,
            next_page=True,
        )
    )
    elements.append(prelim_section_break)

    first_body_section = True
    body_sections = chapters + [references]
    if options.include_appendices:
        body_sections.append(appendices)

    for chapter in body_sections:
        if not first_body_section:
            elements.append(make_page_break_paragraph())
        render_section(
            chapter,
            elements,
            heading_style_override="Heading1",
            compact_tables=options.compact_tables,
            image_registry=image_registry,
        )
        first_body_section = False

    for element in elements:
        body.append(element)

    body.append(
        make_section_properties(
            default_footer_rid=default_footer_rid,
            page_format="decimal",
            page_start=1,
            next_page=False,
        )
    )

    return ET.tostring(document, encoding="utf-8", xml_declaration=True)


def build_docx(template_path: Path, source_path: Path, output_path: Path, *, options: BuildOptions) -> None:
    markdown_text = source_path.read_text(encoding="utf-8")
    markdown_root = parse_markdown_sections(markdown_text)

    template_doc_root = read_zip_xml(template_path, "word/document.xml")
    rels_root = read_zip_xml(template_path, "word/_rels/document.xml.rels")
    styles_root = read_zip_xml(template_path, "word/styles.xml")
    content_types_root = read_zip_xml(template_path, "[Content_Types].xml")

    default_footer_rid = None
    for rel in rels_root.findall(rel_tag("Relationship")):
        if rel.get("Target") == "footer1.xml":
            default_footer_rid = rel.get("Id")
            break
    if default_footer_rid is None:
        raise RuntimeError("Template is missing a footer1.xml relationship.")

    blank_footer_rid = ensure_footer_relationship(rels_root)
    image_registry = ImageRegistry(source_path.parent.resolve(), rels_root)
    updated_styles_xml = update_styles_xml(styles_root)
    updated_document_xml = build_document_xml(
        template_doc_root,
        markdown_root,
        default_footer_rid=default_footer_rid,
        blank_footer_rid=blank_footer_rid,
        options=options,
        image_registry=image_registry,
    )
    updated_rels_xml = ET.tostring(rels_root, encoding="utf-8", xml_declaration=True)
    updated_content_types_xml = ensure_footer_override(content_types_root, image_registry.assets)
    updated_footer1_xml = build_footer_xml(centered=True)
    updated_footer2_xml = build_footer_xml(centered=False)

    output_path.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(template_path, output_path)

    replacements = {
        "word/document.xml": updated_document_xml,
        "word/_rels/document.xml.rels": updated_rels_xml,
        "word/styles.xml": updated_styles_xml,
        "word/footer1.xml": updated_footer1_xml,
        "word/footer2.xml": updated_footer2_xml,
        "[Content_Types].xml": updated_content_types_xml,
    }
    media_replacements = {
        asset.package_name: asset.source_path.read_bytes()
        for asset in image_registry.assets
    }
    replacements.update(media_replacements)

    with tempfile.NamedTemporaryFile(delete=False, suffix=".docx") as temp_file:
        temp_path = Path(temp_file.name)

    try:
        with zipfile.ZipFile(output_path, "r") as src_zip, zipfile.ZipFile(temp_path, "w", compression=zipfile.ZIP_DEFLATED) as dst_zip:
            existing_names = set(src_zip.namelist())
            for item in src_zip.infolist():
                if item.filename in replacements:
                    dst_zip.writestr(item.filename, replacements[item.filename])
                else:
                    dst_zip.writestr(item, src_zip.read(item.filename))
            for filename, content in replacements.items():
                if filename not in existing_names:
                    dst_zip.writestr(filename, content)
        temp_path.replace(output_path)
    finally:
        if temp_path.exists():
            temp_path.unlink()

    validate_docx(output_path, options=options)


def validate_docx(output_path: Path, *, options: BuildOptions) -> None:
    required_entries = {
        "word/document.xml",
        "word/styles.xml",
        "word/footer1.xml",
        "word/footer2.xml",
        "word/_rels/document.xml.rels",
        "[Content_Types].xml",
    }
    with zipfile.ZipFile(output_path, "r") as zf:
        names = set(zf.namelist())
        missing = required_entries - names
        if missing:
            raise RuntimeError(f"Generated DOCX is missing entries: {sorted(missing)}")
        if not any(name.startswith("word/media/") for name in names):
            raise RuntimeError("Generated DOCX lost template media assets.")
        if "word/theme/theme1.xml" not in names:
            raise RuntimeError("Generated DOCX lost the template theme.")

        document_xml = zf.read("word/document.xml")
        styles_xml = zf.read("word/styles.xml")
        document_text = document_xml.decode("utf-8", errors="ignore")
        footer_text = zf.read("word/footer1.xml").decode("utf-8", errors="ignore")
        if "PROJECT TITLE" in document_text:
            raise RuntimeError("Generated DOCX still contains the template title placeholder.")
        if "1. Introduction" not in document_text or "References" not in document_text:
            raise RuntimeError("Generated DOCX is missing expected report headings.")
        if options.include_appendices and "Appendices" not in document_text:
            raise RuntimeError("Generated DOCX is missing the appendices heading.")
        if not options.include_appendices and "Appendix A. Installation Guide" in document_text:
            raise RuntimeError("Generated DOCX still contains appendices despite exclusion.")
        if "| --- |" in document_text:
            raise RuntimeError("Generated DOCX still contains raw Markdown table separators.")
        if "Page" in footer_text and "| Page" in footer_text:
            raise RuntimeError("Generated DOCX still contains the old footer format.")

        document_root = ET.fromstring(document_xml)
        styles_root = ET.fromstring(styles_xml)
        normal_style = styles_root.find(f"{w_tag('style')}[@{w_tag('styleId')}='Normal']")
        if normal_style is not None:
            normal_run = normal_style.find(w_tag("rPr"))
            normal_paragraph = normal_style.find(w_tag("pPr"))
            normal_fonts = normal_run.find(w_tag("rFonts")) if normal_run is not None else None
            normal_size = normal_run.find(w_tag("sz")) if normal_run is not None else None
            normal_spacing = normal_paragraph.find(w_tag("spacing")) if normal_paragraph is not None else None
            if normal_fonts is None or normal_fonts.get(w_tag("ascii")) != "Times New Roman":
                raise RuntimeError("Generated DOCX Normal style is not Times New Roman.")
            if normal_size is None or normal_size.get(w_tag("val")) != "24":
                raise RuntimeError("Generated DOCX Normal style is not 12 pt.")
            if normal_spacing is None or normal_spacing.get(w_tag("line")) != "480":
                raise RuntimeError("Generated DOCX Normal style is not double-spaced.")

        for section in document_root.iter(w_tag("sectPr")):
            margins = section.find(w_tag("pgMar"))
            if margins is None:
                raise RuntimeError("Generated DOCX has a section without margins.")
            expected_margins = {
                "top": "1440",
                "right": "1440",
                "bottom": "1440",
                "left": "2160",
            }
            for margin_name, expected_value in expected_margins.items():
                if margins.get(w_tag(margin_name)) != expected_value:
                    raise RuntimeError(f"Generated DOCX margin {margin_name} does not match the template requirement.")

        body = document_root.find(w_tag("body"))
        if body is not None:
            previous_page_break = False
            for child in list(body):
                has_page_break = child.find(f".//{w_tag('br')}[@{w_tag('type')}='page']") is not None
                if previous_page_break and has_page_break:
                    raise RuntimeError("Generated DOCX contains consecutive manual page breaks.")
                previous_page_break = has_page_break


def parse_args() -> argparse.Namespace:
    default_source = Path("docs/Final_Report/final-report-draft.md")
    default_output = Path("docs/Final_Report/Project-Sentry-Final-Report-final-draft.docx")

    parser = argparse.ArgumentParser(description="Build the capstone report DOCX from the Markdown draft and template.")
    parser.add_argument("--template", required=True, type=Path, help="Path to the Capstone_Report_Template.docx file.")
    parser.add_argument("--source", default=default_source, type=Path, help="Path to the Markdown report source.")
    parser.add_argument("--output", default=default_output, type=Path, help="Path to the generated DOCX output.")
    parser.add_argument(
        "--include-appendices",
        action="store_true",
        help="Include the appendices in the generated DOCX. Omitted by default to keep the main report within capstone length expectations.",
    )
    parser.add_argument(
        "--keep-template-guidelines",
        action="store_true",
        help="Keep the template's instructional guideline pages in the generated DOCX.",
    )
    parser.add_argument(
        "--no-compact-tables",
        action="store_true",
        help="Disable compact single-spaced table rendering.",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    options = BuildOptions(
        include_appendices=args.include_appendices,
        keep_template_guidelines=args.keep_template_guidelines,
        compact_tables=not args.no_compact_tables,
    )
    build_docx(args.template.resolve(), args.source.resolve(), args.output.resolve(), options=options)
    print(f"Generated {args.output}")


if __name__ == "__main__":
    main()
