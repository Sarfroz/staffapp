# AsuliaTech Field Staff

Native Kotlin / Jetpack Compose Android starter, Android 7+ (API 24).

## GitHub builds

Publish the **contents of this directory as the root of the selected repository**.
Its `.github/workflows/android.yml` runs on main/master pushes, pull requests and
manual dispatch. Download `AsuliaTech-FieldStaff-Debug-APK` from the successful
Actions run. No Node, Expo or local Android Studio is required for CI builds.
The workflow is intentionally inside this standalone directory until the target
repository is confirmed; GitHub does not run nested workflow directories.

The debug APK is for testing. Production release signing and distribution are
not configured yet. Keep any future signing keys and credentials in GitHub Secrets.

## Included

- Home, school details, multiple-terminal entry and installation review.
- Exactly six ASCII digits for IMEI/SIM suffixes; duplicate terminal ID prevention.
- Form state survives activity recreation. This is a session draft, not durable storage.
- No background services, polling, GPS or camera permissions in this first preview.

## Next integration work

Authentication, school lookup, backend activation API, Room draft persistence,
WorkManager uploads, camera evidence, location at capture, server call verification,
and private staff reports are pending. Activation is disabled rather than simulated.
Backend must resolve IMEI/SIM suffixes against inventory and reject ambiguous matches.

## Toolchain

JDK 17, Gradle 8.11.1, AGP 8.9.2, Kotlin/Compose compiler 2.1.20, SDK 35.
Run `./gradlew assembleDebug lintDebug` with an Android SDK available to validate.
