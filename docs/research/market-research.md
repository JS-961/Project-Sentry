# Survey + Market Research Pack

Date: 2026-04-11

## Goal

Collect lightweight evidence for:

- feasibility
- practicality
- user trust
- market demand

This document is for field research and handoff, not the final report.

## Target Respondents

Primary group:

- students who drive regularly
- commuters
- parents who drive family members

Optional secondary group:

- taxi drivers
- delivery drivers

Recommended sample size:

- survey: `30-50` respondents
- short interviews: `5-10` respondents

## Survey Setup

Recommended tool:

- Google Forms or Microsoft Forms

Keep the survey under 3 minutes.
Do not ask for identifying personal information unless absolutely necessary.

## Recommended Google Form Draft

Suggested form intro:

`We are testing interest in a mobile app that a driver starts before a trip. During the trip, the app uses phone motion data to detect a severe collision. If a likely crash is detected, it shows a short countdown. If the driver does not respond, it sends an alert with location to emergency contacts.`

Keep every question as single-choice multiple choice so it stays fast to answer.

1. Which best describes you as a driver?
   - student who drives regularly
   - daily commuter
   - parent or family driver
   - taxi or ride-hailing driver
   - delivery driver
   - other regular driver

2. How often do you drive in a typical week?
   - daily
   - 4-6 days per week
   - 2-3 days per week
   - 1 day per week or less
   - rarely

3. Have you ever experienced a crash or serious near-miss while driving?
   - yes, a crash
   - yes, a serious near-miss
   - yes, both
   - no

4. How likely would you be to install this app for your own driving?
   - very likely
   - somewhat likely
   - not sure
   - somewhat unlikely
   - very unlikely

5. Where is your phone usually kept while driving?
   - mounted on the dashboard or windshield
   - in a cup holder or storage compartment
   - on the passenger seat
   - in my pocket, bag, or backpack
   - it varies a lot

6. During an active trip, would you allow the app to use motion sensors to detect a severe collision?
   - yes, with no major concerns
   - yes, but only if I manually start each trip
   - yes, but only if battery use stays low
   - yes, but only if privacy controls are clear
   - no

7. If a likely crash is detected, would you allow the app to share your location only for that emergency alert?
   - yes, with no major concerns
   - yes, but only with my emergency contacts
   - yes, but only after a countdown or confirmation step
   - maybe
   - no

8. What would concern you most about using this app?
   - false alarms
   - missing a real crash
   - privacy or location sharing
   - battery drain
   - needing to keep the phone in a specific position
   - too many alerts or too much complexity

9. Which outcome would bother you more?
   - a false alarm that alerts someone unnecessarily
   - the app missing a real emergency
   - both would bother me equally
   - not sure

10. Which alert workflow would you trust most?
    - show a countdown first, then alert my emergency contacts if I do not respond
    - alert my emergency contacts immediately
    - show a countdown first, then contact emergency services automatically
    - alert both emergency contacts and emergency services automatically
    - I would not trust automatic alerts

11. If this feature worked reliably, how would you prefer to get it?
    - free only
    - one-time low-cost purchase
    - low monthly subscription
    - included inside a broader safety or family app
    - I would not use it even if free

## Short Interview Guide

Keep this to `5-7` interviews, about `8-12` minutes each. Ask the core question first, then use follow-ups only if the answer is vague.

1. Tell me about the last time you had a crash or a serious near-miss. What happened in the first 30 seconds, and what kind of help would have mattered most?
2. If this app detected a severe crash and started a short countdown, what would make you trust it enough to keep using it?
3. What would make you hesitate to allow motion sensing during a trip or location sharing during an emergency?
4. Walk me through where your phone usually is while driving. In what situations would this app feel reliable, and in what situations would it feel unreliable or annoying?
5. Which feels worse to you and why: a false alarm, or the app missing a real emergency?
6. If the app had to alert someone, who should it contact first, and what should happen before the alert is sent?

Optional follow-up prompts:

- What would make you uninstall it after one week?
- What would make this feel useful enough to recommend to someone else?

## Interview Findings Snapshot

Based on a small set of driver interviews, these were the clearest recurring responses:

1. Tell me about the last time you had a crash or a serious near-miss. What happened in the first 30 seconds, and what kind of help would have mattered most?
   - In the immediate aftermath of a crash or serious near-miss, participants said the most important response would be fast emergency support, especially ambulance access and a quick wellbeing check for everyone involved.

2. If this app detected a severe crash and started a short countdown, what would make you trust it enough to keep using it?
   - Participants were generally open to trusting and continuing to use the app, provided it behaved reliably during serious incidents and clearly communicated what it was doing during the countdown.

3. What would make you hesitate to allow motion sensing during a trip or location sharing during an emergency?
   - Most interviewees showed little hesitation about motion sensing or emergency-only location sharing. Several felt that, in a real emergency, having more information available would improve the chances of a faster and more effective response.

4. Walk me through where your phone usually is while driving. In what situations would this app feel reliable, and in what situations would it feel unreliable or annoying?
   - Participants said the app would feel most reliable when the phone is in a predictable position, such as mounted or placed openly in the car. Confidence dropped when the phone might be buried in a backpack or another enclosed space, where sensor readings could feel less dependable.

5. Which feels worse to you and why: a false alarm, or the app missing a real emergency?
   - Participants viewed a missed real emergency as more serious than a false alarm, mainly because a failure to detect a genuine crash could delay critical assistance when it is needed most.

6. If the app had to alert someone, who should it contact first, and what should happen before the alert is sent?
   - Many participants preferred contacting ambulance or emergency services first, while also wanting the flexibility to choose a dedicated emergency contact based on their personal situation.

## What To Measure

At minimum, summarize:

- willingness to install
- willingness to grant motion permissions
- willingness to grant location permissions
- strongest adoption concern
- preference for emergency-contact workflow
- willingness to pay

## Competitor Matrix Template

Build a one-page comparison with these products:

- Apple Crash Detection
- Google Pixel Crash Detection
- Life360 safety features

Suggested columns:

- product
- supported devices
- automatic detection
- emergency-contact workflow
- emergency-service workflow
- location sharing
- privacy posture
- pricing
- notable limitation

## Source-Backed Positioning Notes

Use these existing project sources when writing the feasibility section later:

- WHO road traffic injuries fact sheet
- NHTSA April 1, 2026 traffic safety release
- Pew mobile ownership fact sheet
- Life360 Q2 2025 results
- Google Pixel crash detection product/support pages
- Apple crash detection support pages

## Handoff Tasks

1. Create the survey form using the questions above.
2. Collect at least 30 responses.
3. Run 5 short interviews and summarize recurring concerns.
4. Fill the competitor matrix from official product pages only.
5. Store raw findings in `docs/` before they are folded into the final report.
