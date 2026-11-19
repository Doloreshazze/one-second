# Google Play preparation — 1.5.0

Package: `com.doloreshazze.onesecond`. Version code: `7`. Target API: `36`.
Minimum Android version: Android 7.0 (API 24).

This version contains 19 levels and 12 interface languages: English, Russian,
German, French, Spanish, Portuguese, Italian, Polish, Turkish, Arabic, Hindi
and Simplified Chinese. Japanese and Korean are not advertised because complete
translations are not included.

## Declarations grounded in this build

- Ads: **No**.
- Data collected by the game: **No**.
- Data shared by the game: **No**.
- Account creation or login: **No**.
- Sensitive or runtime Android permissions: **None**.
- Advertising ID, AdMob, Firebase Analytics, Crashlytics: **Not included**.
- Core gameplay works without a network connection.
- Privacy policy: https://github.com/Doloreshazze/one-second/blob/main/docs/privacy.md
- Privacy policy in the app: Settings → Privacy policy.

These answers describe the application, including its runtime dependencies.
Do not declare planned SDK integrations as if they were already present.
The separate correspondence described in the policy is initiated in the user's
email service, not collected through the game.

## Remaining console steps

Upload the release AAB signed with the retained upload key and enable Play App
Signing. Complete the main store listing, developer contact details, age-rating
questionnaire and target-audience declaration based on the actual intended
audience. Configure countries, closed-test participants and a feedback contact.
Resolve the release review page's errors, then submit the closed track.

The repository does not assert that these account-specific steps are complete.
Check the current requirements shown in the app's Play Console before submitting.

## Checks in GitHub Actions

The workflow checks all translation keys and format arguments, verifies all
19 puzzle solutions, runs Android Lint, builds release APK/AAB files, checks the
packaged permissions and absence of advertising/analytics classes and native
libraries, plays the complete game on an Android 16 emulator, and checks launch,
settings and localized screens on Android 7.0 and Android 16.

CI distribution artifacts labelled `unsigned` require signing before uploading
to Google Play. Temporary keys used for emulator checks are not upload keys.
Signing keys and passwords must never be committed to the public repository.
