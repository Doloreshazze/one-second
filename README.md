# ONE SECOND! 1.5.1

A one-swipe physics puzzle for Android by Play Everywhere. Time is frozen:
choose a direction, throw a marble and watch a chain of events unfold.

The game has 19 illustrated levels, animated characters, quiet background music,
sound effects, saved progress and 12 interface languages. Version 1.5.1 works
offline and contains no advertising, analytics SDKs or Android permissions.

## Play

Swipe inside the scene to aim and release the marble. After the result, retry or
continue. The chapter counter opens unlocked levels. The speaker button opens
sound settings, language selection and the offline privacy policy.

## Build and verification

Use JDK 17, Gradle 8.11.1 and Android SDK 36. The Android Gradle Plugin is 8.10.1.
GitHub Actions performs the Android builds and emulator checks.

```sh
python3 .github/scripts/verify-release.py
gradle assembleDebug assembleRelease bundleRelease lintRelease --no-daemon
```

Release files from CI are unsigned and must be signed with the retained upload
key before Play submission. Never use a temporary emulator or Android Debug key
as the upload key. No signing material belongs in this repository.

See [release preparation](docs/play-console.md) and the
[privacy policy](docs/privacy.md).

## Languages

English, Русский, Deutsch, Français, Español, Português, Italiano, Polski,
Türkçe, العربية, हिन्दी and 简体中文. Unsupported device languages use English.
