# Tap Sentry

Tap Sentry is a local-first Android 7+ gesture automation tool. It combines accessibility gestures, a movable floating controller, click indicators, reusable macro profiles, and region-scoped visual auto-stop.

## Build with GitHub Actions

Open the repository's **Actions** tab and select **Build Android APKs**. Builds run automatically for pushes to `main` and `codex/**` branches and for pull requests targeting `main`. After this workflow is merged into `main`, you can also select **Run workflow** to build a chosen branch manually.

Each successful run executes the unit tests and Android lint, then provides these downloadable artifacts:

- `TapSentry-debug-<run number>`: extract the ZIP and install `app-debug.apk`. Its application ID is `dev.tapsentry.debug`.
- `TapSentry-release-test-<run number>`: extract the ZIP and install `app-release.apk`. Its application ID is `dev.tapsentry`. This is a release-mode test build signed with Android's debug key, not a distribution-signed release.
- `TapSentry-reports-<run number>`: unit-test and lint reports, uploaded even when checks fail if reports were generated.

APK artifacts are kept for 30 days; reports are kept for 14 days. These are GitHub Actions artifacts, not GitHub Releases. Hosted runners can generate different debug signing keys, so updating a previous test installation may require uninstalling it first (which removes local profiles). Use a stable external release key for distribution and reliable updates.

GitHub installs Java 17, Android SDK 35, and Gradle 8.10.2 for you. No local Android Studio installation or signing secrets are needed for these test APKs.

## Build locally

1. Install Android SDK Platform 35 and Build Tools 35.
2. Set `ANDROID_HOME` (or create `local.properties` with `sdk.dir=...`).
3. Install a compatible Gradle version (Gradle 8.10.2 is recommended).
4. From the repository root, regenerate the wrapper bootstrap JAR:

   ```bash
   gradle wrapper
   ```

   This creates `gradle/wrapper/gradle-wrapper.jar` locally.
5. Run the normal checks and builds:

   ```bash
   ./gradlew test
   ./gradlew assembleDebug
   ./gradlew assembleRelease
   ```

`gradle-wrapper.jar` is intentionally not committed because the pull-request system used by this repository does not support binary files. The wrapper scripts and properties are committed; developers only need to regenerate the bootstrap JAR once before using them.

Install `app/build/outputs/apk/release/app-release.apk`, then grant **Display over other apps** and enable **Tap Sentry** under Android Accessibility. Screen-capture consent is requested only when visual detection is enabled.

### Release signing

Signing keys and credentials must remain outside this repository. For a distribution-signed release, provide all four values as environment variables (or as identically named entries in your user-level `~/.gradle/gradle.properties`):

```bash
export TAPSENTRY_KEYSTORE_PATH=/absolute/path/to/tapsentry-release.jks
export TAPSENTRY_KEYSTORE_PASSWORD='your-store-password'
export TAPSENTRY_KEY_ALIAS='your-key-alias'
export TAPSENTRY_KEY_PASSWORD='your-key-password'
./gradlew assembleRelease
```

When all four values are present, `assembleRelease` signs with that external key. When any value is absent, release builds fall back to Android's standard debug signing configuration so the APK remains directly installable for local testing. `./gradlew assembleDebug` never requires release credentials and writes `app/build/outputs/apk/debug/app-debug.apk`.

## Visual auto-stop

The MediaProjection frame is sampled every configured interval. Only pixels inside the normalized detection rectangle are averaged. HSV-weighted distance is checked against the target color and must match for the selected number of consecutive frames. New gestures stop immediately (or after the configured additional clicks), the final click ring remains for the stop delay, and the session reports `STOPPED`.

## Permissions

* `SYSTEM_ALERT_WINDOW`: floating controls, markers, and click feedback.
* Accessibility service binding: user-authorized tap, long-press, and swipe gestures.
* `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_MEDIA_PROJECTION`: reliable on-device visual sampling while backgrounded.
* `POST_NOTIFICATIONS`: foreground-service disclosure on Android 13+.

There is intentionally no Internet permission. Frames are not saved. Profiles use private local preferences.

## Known limitations

Android secure/DRM surfaces cannot be captured. OEM battery management may stop the service. Visual selection uses normalized numeric bounds in V1; the overlay displays click feedback but does not yet provide drag handles for resizing the rectangle. Android requires fresh MediaProjection consent for every visual session.
