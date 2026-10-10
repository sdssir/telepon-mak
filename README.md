# Telefon Mak v2

Android home launcher for older adults, designed for OPPO A6x.

## Features
- Offline contacts A-Z, 3 tiles per row, photo or first-letter fallback
- Double tap a contact tile to open large contact details
- Large green CALL button makes the call after Phone permission is granted
- Bottom CONTACTS and APPS menu; Apps opens installed launchable apps
- Default HOME launcher support

## Install and build
Open GitHub Actions > Build Android APK > Run workflow, then download the `Telefon-Mak-v2-APK` artifact and install its debug APK. Alternatively build with Android Studio/Gradle 8.9 and JDK 17.

Grant Contacts and Phone permissions, then select Telefon Mak as the default Home app in phone settings. Keep the stock Phone app as the default dialer for receiving calls.

## Important limitations
- **Automatic loudspeaker is not implemented.** Stock OPPO Phone does not grant an ordinary home launcher control over speaker routing. It would require a more involved default-dialer implementation and testing on the OPPO A6x.
- Contacts with multiple phone numbers are displayed as separate tiles (one per distinct name-number pair).
- This code has not yet been built or tested on the device. GitHub Actions build status must be checked before installing.
