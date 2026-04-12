# Demo Script (MVP)

## Pre-Demo Setup

- Ensure app permissions are granted (location, SMS, phone, notifications as needed).
- Confirm emergency SMS contacts use placeholder/demo numbers only.
- Confirm demo call number is a non-emergency line.
- Verify TTS template is short (<=10 seconds spoken intro).

## Demo Flow

1. Open app and explain the two pillars: crash response and prevention.
2. On Home screen, start Driving Mode.
3. Show live risk score and event counters updating (or via simulated motion input).
4. Trigger "Simulate Crash".
5. Show countdown screen and available user controls:
   - Cancel
   - I'm OK
   - Call Now
6. Let countdown timeout to show escalation sequence:
   - SMS payload format (location + maps link)
   - Call intent to demo number
   - TTS intro playback
7. End with trip summary concept and stored local event history.

## Talking Points

- Privacy-first local architecture.
- Configurable contacts and no hardcoded emergency numbers.
- Android OS limitations and human confirmation constraints.
- Prototype limitations and false positive/false negative tradeoff.

## Fallback Plan

- If SMS or call permissions are denied, show graceful error handling path.
- If location unavailable, show last-known location fallback behavior.
