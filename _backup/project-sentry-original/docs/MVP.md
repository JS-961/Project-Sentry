MVP Scope

User Story
- As a driver, I want my phone to detect a likely crash and ask if I'm OK
  so that help is notified if I cannot respond.

Primary Flow
1. App runs a trip session when the user presses Start.
2. Sensors are sampled and analyzed in real time.
3. On suspected crash, a full-screen alert appears with a countdown.
4. If user taps "I'm OK", the alert is cancelled and event logged.
5. If no response before timeout, emergency contacts are notified with location.

Acceptance Criteria
- Can start/stop a trip session
- Detects a simulated crash trigger within 3 seconds
- Shows confirmation UI within 1 second of detection
- Sends an alert message with location to a contact (mocked or real)
- Stores a local event log entry for every alert
- Can export the event log as JSON for reporting

De-scoped for MVP
- Always-on background monitoring
- Integration with vehicle systems
- ML-based classification (rules first)
