# Native Android Bug Fixing And QA Report

## Overview

This document summarizes the main bug-fixing and quality assurance work completed
for the native Android version of Project Sentry. The focus of this phase was to
stabilize the Android MVP, especially the permission flow, foreground driving
service, crash countdown behavior, notification behavior, and demo readiness.

The Flutter app was treated only as a reference for UI ideas. All functional bug
fixing was focused on `android-native/`.

## Testing And Debugging Approach

I tested the app as a production-style Android MVP rather than only as a UI demo.
The main checks included:

- launching the app from a fresh install state
- requesting Android runtime permissions
- starting and stopping Driving Mode
- testing behavior while the app was in the foreground and background
- simulating a crash countdown
- checking SMS, call, notification, and TTS escalation behavior
- reviewing the foreground service lifecycle
- checking Room persistence for trips, risk events, and crash alert outcomes
- running Gradle build, unit-test, and lint verification

During testing, several issues were discovered and fixed. The fixes were kept
small and targeted where possible to avoid breaking the working parts of the app.

## Bug 1: First-Launch Crash During Permission Request

### Problem Observed

On the first app launch, the app crashed during the initial permission request.
After permissions were manually granted, the app worked normally. This behavior
suggested that the issue was not with the core sensors or UI, but with the
startup and permission flow.

### Likely Cause

The app was attempting to start the foreground driving service before the required
runtime permissions were fully granted. This created a race condition between:

- the permission request dialog
- foreground service startup
- location access
- notification requirements on newer Android versions

On Android 13+ and Android 14/15 behavior, foreground service and notification
rules are stricter, so starting service work too early can cause a crash or an
unstable startup state.

### Fix Applied

The permission flow was changed so that Driving Mode only starts after permission
results are returned and validated. The app now checks missing permissions before
starting the service, stores the pending service action, and resumes the action
only if required permissions are granted.

The service also checks location permission internally before monitoring begins.
If location permission is missing, it publishes a user-visible status message and
stops safely instead of crashing.

### Files Involved

- `MainActivity.kt`
- `DrivingModeService.kt`
- `PermissionStatus.kt`

### Result

The startup flow is now safer because permission approval happens before service
startup. The app no longer depends on permissions being granted before the first
launch.

## Bug 2: Unsafe Foreground Service Restart Behavior

### Problem Observed

The original service lifecycle was fragile because Android could potentially
restart the service without the original action intent. This is risky for a
driving-monitoring service because a null or missing intent could cause
unexpected auto-start behavior.

### Cause

Using sticky service behavior for this type of monitoring can be unsafe. A
service restart without an explicit user action could leave the app in a confusing
state or attempt to monitor without the proper context.

### Fix Applied

The service command handling was adjusted to return `START_NOT_STICKY`. If the
system stops the service, Android should not automatically recreate it without a
clear action. Unknown or missing service actions are ignored safely.

### Files Involved

- `DrivingModeService.kt`

### Result

Driving Mode now starts only through an explicit user or app action. This makes
the service behavior easier to reason about and safer for a demo.

## Bug 3: Repeated Notifications For Very Low Risk Scores

### Problem Observed

During testing, the app kept sending or updating notifications even when risk
increased to a very small value such as `1/100`. This was annoying and made the
app feel unstable, especially when pressing Start Driving.

### Cause

The driving notification was being refreshed too aggressively. Even small risk or
speed changes could cause repeated notification updates. On Android, repeated
updates can feel like repeated alerts if the channel and notification behavior are
not configured carefully.

### Fix Applied

The foreground Driving Mode notification was changed to behave like a quiet
status notification:

- notification priority was lowered
- the notification was marked silent
- `setOnlyAlertOnce(true)` was used
- risk and speed changes must be meaningful before refreshing the notification
- notification refreshes are throttled by a minimum update interval

Crash alerts still use a separate high-priority notification channel, so the app
can remain quiet during normal monitoring but urgent during an actual crash flow.

### Files Involved

- `DrivingModeService.kt`

### Result

The normal Driving Mode notification now acts like a stable background status
indicator instead of repeatedly alerting the user for tiny risk changes.

## Bug 4: False Crash Calls On Speed Bumps And Turns

### Problem Observed

During vehicle testing, the app repeatedly initialized false crash calls or crash
countdowns when driving over speed bumps or taking turns. This was one of the
most important issues because false emergency escalation would be unacceptable in
a real driving app.

### Cause

The crash detection logic was too sensitive to raw sensor spikes. Speed bumps,
sharp turns, and normal phone movement can create acceleration or gyroscope peaks
that look like crash-like events if they are evaluated in isolation.

The original behavior did not sufficiently require supporting evidence such as:

- vehicle speed before the impact
- speed drop after the event
- repeated impact samples
- cooldown between crash detections
- separation between risk events and crash events

### Fix Applied

Crash detection was changed from immediate triggering to a staged candidate
validation flow:

- a crash candidate is staged when acceleration or jerk crosses a high threshold
- the candidate waits through a short validation window
- the app checks whether speed dropped significantly or the vehicle reached a
  hard-stop state
- repeated impact samples or severe impact thresholds are required
- a crash cooldown prevents repeated countdowns from one bumpy segment

Risk events such as harsh braking, harsh acceleration, sharp cornering, and
speeding still update the risk score, but they do not automatically trigger a
crash countdown.

### Files Involved

- `DrivingModeService.kt`

### Result

The app is less likely to treat normal bumps or turns as crashes. The crash flow
now requires stronger evidence before starting the countdown.

## Bug 5: Crash Countdown Not Appearing Reliably Outside The App

### Problem Observed

After stabilizing the service and notification behavior, I noticed a regression:
when the app was outside the foreground, the crash countdown did not always open
automatically, and crash notifications/calls were not always visible in the way
expected for a demo.

### Cause

Android restricts background activity launches. A service cannot always open an
activity directly when the app is in the background. Relying only on
`startActivity()` from the service is not platform-safe.

### Fix Applied

The crash flow was changed to use a safer alert path:

- crash alerts use a separate high-priority notification channel
- the crash notification includes a full-screen intent
- the service still attempts to launch the countdown activity as a best-effort
  path for demo visibility
- if Android blocks the activity launch, the notification remains available
- the manifest includes full-screen intent permission support

### Files Involved

- `DrivingModeService.kt`
- `CrashCountdownActivity.kt`
- `AndroidManifest.xml`

### Result

The crash countdown is more reliable outside the app because the notification is
now the primary platform-safe path, with direct activity launch only used as a
best-effort enhancement.

## Bug 6: Placeholder Phone Numbers Could Be Used Accidentally

### Problem Observed

The settings flow previously risked falling back to placeholder/demo phone
numbers. This was unsafe because the app could appear configured even when the
user had not entered real demo-safe contact information.

### Cause

Settings defaults were too forgiving. In an emergency-style app, missing contact
configuration should be obvious instead of silently replaced with fake numbers.

### Fix Applied

Settings now default to empty contact and call-number values. The settings screen
validates phone numbers before saving or testing the crash flow. The status UI
also shows whether SMS, phone, contacts, and call escalation are ready.

### Files Involved

- `AppSettings.kt`
- `SettingsRepository.kt`
- `SettingsScreen.kt`
- `PermissionStatus.kt`
- `HomeScreen.kt`

### Result

The app now forces setup problems to be visible. This is better for both safety
and capstone demo clarity.

## Bug 7: Crash Countdown Actions Were Confusing

### Problem Observed

The crash countdown worked, but the action buttons were not clear enough. Some
actions overlapped in behavior, which could confuse the driver and the demo
audience.

### Cause

The countdown UI did not clearly explain what would happen on timeout and did
not separate the meaning of "I am OK" from escalation behavior.

### Fix Applied

The crash countdown UI was redesigned to show:

- a clear "Potential Crash Detected" screen
- countdown seconds remaining
- what happens when the timer reaches zero
- how many contacts are configured
- what call number will be used
- separate actions for "I'm OK" and "Call Now"

### Files Involved

- `CrashCountdownActivity.kt`

### Result

The crash flow is easier to understand and more convincing for a capstone demo.

## Bug 8: Missing Setup And Monitoring Status Visibility

### Problem Observed

The app worked technically, but it was difficult to quickly tell whether the
system was ready for a demo. Permissions, contacts, GPS, sensors, and stored data
were not surfaced clearly.

### Cause

The original home screen placed too many controls and values in one place. It
did not clearly separate live driving, history, and system readiness.

### Fix Applied

The UI was reorganized into a bottom navigation structure:

- Drive page for live monitoring and crash demo controls
- History page for trips, risk events, and crash alert outcomes
- Status page for permission checks, runtime health, local data, and demo script

Settings remains a separate configuration screen.

### Files Involved

- `HomeScreen.kt`
- `SettingsScreen.kt`
- `TripDao.kt`
- `RiskEventDao.kt`
- `CrashAlertDao.kt`

### Result

The app now feels more polished and easier to explain during a capstone
presentation. The interface also helps detect setup problems before testing.

## Bug 9: Room Data Was Written But Not Presented Well

### Problem Observed

The app stored trip, risk event, and crash alert data, but there was no strong UI
path to show that data during a demo.

### Cause

The database layer had insert/update behavior, but the app needed read queries
and UI collection to display recent records and counts.

### Fix Applied

Room DAO read queries were added for:

- recent trips
- recent risk events
- recent crash alerts
- trip/event/alert counts

The home navigation now collects those flows and displays the data in the
History and Status pages.

### Files Involved

- `TripDao.kt`
- `RiskEventDao.kt`
- `CrashAlertDao.kt`
- `MainActivity.kt`
- `HomeScreen.kt`

### Result

The app can now demonstrate persistence instead of only showing live values.

## Bug 10: Manifest Lint Error For SMS And Phone Permissions

### Problem Observed

During QA verification, Android lint failed because `SEND_SMS` and `CALL_PHONE`
permissions implied that the app required telephony hardware.

### Cause

Android and Google Play treat some permissions as implied hardware requirements
unless the manifest explicitly marks the hardware feature as optional.

### Fix Applied

The manifest now declares telephony hardware as optional:

```xml
<uses-feature
    android:name="android.hardware.telephony"
    android:required="false" />
```

### Files Involved

- `AndroidManifest.xml`

### Result

Lint now passes. The app can still use SMS/call features on phones, but it does
not unnecessarily exclude devices that do not have telephony hardware.

## Verification Performed

After the fixes, I ran the following verification commands from `android-native/`:

```powershell
.\gradlew :app:assembleDebug
.\gradlew :app:testDebugUnitTest
.\gradlew :app:lintDebug
```

The results were:

- debug build passed
- unit-test task passed
- lint passed with 0 errors
- only non-blocking warnings remained, mostly dependency version warnings and
  minor cleanup suggestions

I also ran a whitespace/diff sanity check on the Android native files. It passed,
with only normal Windows CRLF line-ending warnings.

## Current Stability Summary

At the end of this bug-fixing phase, the Android native app is more stable in
the following areas:

- first-launch permission handling
- foreground service lifecycle
- quiet normal driving notification behavior
- crash alert notification path
- crash countdown clarity
- false-positive reduction for bumps and turns
- settings validation
- Room-backed history and status display
- Gradle build and lint verification

## Remaining Limitations

Some items still need future work before the app can be considered production
ready:

- The detector should be tested more on real roads with different phones and car
  mounting positions.
- A fresh-install test should be repeated on at least one API 34/35 physical
  device.
- Crash detection and risk scoring are still inside `DrivingModeService`; they
  should eventually be extracted into testable classes.
- More unit tests should be added around detector thresholds, cooldown behavior,
  and state transitions.
- Detection sensitivity should eventually become configurable through profiles
  instead of hardcoded constants.
- Room schema migrations should be planned before changing database tables in
  future versions.

## Conclusion

This bug-fixing phase moved the Android native app from a working prototype
toward a more stable demo-ready MVP. The most important improvements were fixing
the first-launch permission crash, reducing false crash triggers, making normal
notifications quiet, improving background crash alert behavior, and adding clear
status/history screens for the capstone demo.

