# Google Play preparation — 1.5.1

Package: `com.playeverywhere.onesecond`. Version code: `8`. Target API: `36`.
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

## Console checkpoint — 20 November 2026

- Correct app card: https://play.google.com/console/u/1/developers/8194510566929948539/app/4972959309598544278/app-dashboard
- Package: `com.playeverywhere.onesecond`.
- Closed track: **Alpha**, release **1.5.1 (8) - Closed alpha**.
- Publishing status verified: **Changes in review**. Submission is confirmed;
  Google was still running automatic quick checks before the review. Approval
  and availability to testers were not yet confirmed.
- Availability: all 178 selectable country/region targets, including Rest of World.
- Testers: existing **Test_1** email list (52 configured addresses). This is
  not a claim that 52 people have opted in.
- Feedback and store support: **daw81bk@gmail.com**.
- Test opt-in URL: https://play.google.com/apps/testing/com.playeverywhere.onesecond
  (becomes available after the closed release is approved/published).
- Target audience: ages 13 and older. Ratings include PEGI 3, ESRB Everyone
  and USK All ages.
- Store listing: English description, matching marble icon, feature graphic
  and four genuine gameplay screenshots. Illustrations are labelled as
  created or edited using AI.
- Advertising ID declaration: **No**.
- Portrait-capable hardware remains an intentional requirement for this
  portrait game. The availability advisory was acknowledged with that reason.
- Uploaded AAB uses the retained permanent upload certificate:
  SHA-256 `b13d94c7ceca7e2aef1351bccb3844d8324d4673b7b1b5b6a497f034e404a750`.
  Keep the private keystore and its password outside this public repository.

The previous draft card (app ID `4973979380107358254`) was already bound to
`com.doloreshazze.onesecond`. Google Play rejected the renamed bundle there,
so the correct package was registered in the new card above. Use the new
card for future releases.

## Checks in GitHub Actions

The workflow checks all translation keys and format arguments, verifies all
19 puzzle solutions, runs Android Lint, builds release APK/AAB files, checks the
packaged permissions and absence of advertising/analytics classes and native
libraries, plays the complete game on an Android 16 emulator, and checks launch,
settings and localized screens on Android 7.0 and Android 16.

CI distribution artifacts labelled `unsigned` require signing before uploading
to Google Play. Temporary keys used for emulator checks are not upload keys.
Signing keys and passwords must never be committed to the public repository.
