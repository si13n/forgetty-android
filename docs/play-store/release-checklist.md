# Forgetty Google Play release checklist

## Already implemented in the release branch

- [x] Package name is stable: `com.si13.forgetty`
- [x] Minimum SDK 26, target SDK 36
- [x] Semantic first-release name `1.0.0`, version code `1`
- [x] Optimized release build enabled
- [x] Android App Bundle is built in CI
- [x] Upload signing can be supplied through environment variables or GitHub Secrets
- [x] Cleartext HTTP disabled
- [x] Guest task database excluded from Android backup and device transfer
- [x] Runtime notification permission requested where required
- [x] In-app account and cloud-data deletion with recent Google reauthentication
- [x] Privacy Policy, Terms, account-deletion page, Data safety draft, and localized listing copy
- [x] PR gate includes debug build, release bundle, unit tests, blocking lint, Espresso, and Maestro

## Required manual setup before production

- [ ] Create or confirm the Play Console developer account and app record
- [ ] Enroll in Play App Signing and create a dedicated upload key
- [ ] Configure the four repository secrets described below
- [ ] Add a monitored public support email to Play Console and the policy pages
- [ ] Publish stable public URLs for the Privacy Policy and account-deletion page
- [ ] Confirm production Firebase Authentication authorized apps and SHA-1/SHA-256 fingerprints
- [ ] Review and deploy least-privilege production Firestore Security Rules
- [ ] Complete the Play Data safety form using `data-safety.md`
- [ ] Complete App access, Ads, Content rating, Target audience, News, and Data deletion declarations
- [ ] Upload the 512 × 512 app icon
- [ ] Upload the 1024 × 500 feature graphic
- [ ] Upload at least 2 phone screenshots; 4 portrait screenshots at 1080 × 1920 or higher are recommended
- [ ] Add English and Russian store listings from `store-listing.md`
- [ ] Test the signed bundle on the Play internal-testing track
- [ ] Run pre-launch report, review accessibility and stability findings
- [ ] Verify sign-in, synchronization, reminders, account deletion, export, widgets, and backup behavior from the Play-delivered build
- [ ] Promote through closed/open testing as required for this developer account, then production

## GitHub Secrets for a signed bundle

Configure these Actions secrets:

- `ANDROID_UPLOAD_KEYSTORE_BASE64` — base64-encoded upload keystore
- `ANDROID_UPLOAD_STORE_PASSWORD`
- `ANDROID_UPLOAD_KEY_ALIAS`
- `ANDROID_UPLOAD_KEY_PASSWORD`

Run **Build Play release bundle** from GitHub Actions. The workflow restores the key only in the temporary runner directory and uploads the signed AAB artifact.

Never commit the keystore, passwords, service-account JSON, or Play publishing credentials.

## Store asset requirements

- App icon: 32-bit PNG with alpha, exactly 512 × 512 px, up to 1024 KB.
- Feature graphic: JPEG or 24-bit PNG without alpha, exactly 1024 × 500 px.
- Phone screenshots: JPEG or 24-bit PNG without alpha, each dimension 320–3840 px, with the long side no more than twice the short side.
- Provide at least two screenshots. Four screenshots at 1080 px or higher and 9:16 portrait ratio are recommended for broader promotion eligibility.
- Screenshots must match the shipped app and avoid fake rankings, prices, calls to action, or misleading device frames.

## Versioning after first upload

Google Play never accepts a reused version code. Increment `versionCode` for every uploaded bundle, including internal-test replacements. Keep `versionName` user-facing and semantic.
