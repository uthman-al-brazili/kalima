# Uptodown submission checklist for Kalima

Official references checked on 2026-08-20:

- [How to publish an app on Uptodown](https://support.uptodown.com/hc/en-us/articles/360053260491-How-to-publish-an-app-on-Uptodown)
- [Uptodown publication criteria](https://support.uptodown.com/hc/en-us/articles/360052792972-Uptodown-s-app-publication-criteria)
- [Uptodown ASO guide](https://support.uptodown.com/hc/en-us/articles/37101277576205-ASO-guide-and-best-practices-when-publishing-apps-on-Uptodown)
- [Supported file formats](https://support.uptodown.com/hc/en-us/articles/360053260051-Supported-file-formats-on-Uptodown)

## Prepared in this directory

- English and Brazilian Portuguese descriptions and 0.30.3 version notes, positioning opt-in lock-screen Quranic Arabic learning first.
- `assets/kalima-icon-512.png`, a square 512×512 PNG.
- `assets/kalima-featured-1024x500.png`, an exact-size featured image.
- Plain-language permission disclosure and `PRIVACY_POLICY.md`.
- Raw English and Brazilian Portuguese portrait screenshots captured from the Android UI in `screenshots/en/` and `screenshots/pt-BR/`; retain these as evidence of the real app UI.
- Separate edited promotional images for the listing in `promotional/en/` and `promotional/pt-BR/`. Upload these edited images as the public screenshot set, while keeping the raw screenshots available for verification.

## Developer-controlled steps before submission

- Register and verify an Uptodown Developers Console account.
- Fill the public organization profile with developer/author name `Uthman (Gustavo)`, support email `uthman-al-brazili@proton.me`, website `https://kalima-h1f.pages.dev/`, and the developer's nationality.
- Use the published privacy-policy URL `https://kalima-h1f.pages.dev/privacy` in the Uptodown privacy-policy field.
- Accept the current Uptodown developer terms.
- Create and securely back up a long-lived Android release signing key. Every future update to `com.kalima.quran` must use the same key.
- Build and verify a release-signed APK. Do not upload the debug-signed backup APK from `releases/` as the long-term public package.
- Upload the APK and select Android, Education, Free, Beta, no ads, no country restrictions, and English + Portuguese.
- Before every screenshot upload, capture from the candidate APK, record its SHA-256 in `website/screenshots-manifest.json`, run the website's `pnpm check`, and visually confirm each raw capture reflects the final UI. Edited promotional images must preserve the truthful UI and be traceable to that evidence.
- Upload the icon, featured image, and four accurate portrait promotional images per language from `promotional/final/en/` and `promotional/final/pt-BR/`.
- Paste both descriptions and both localized version-note sets from this directory.
- Submit for review and answer any permission, content-license, or support questions from Uptodown’s editorial team.

## Content and rights review

Before asserting final publication rights, confirm that every bundled Quran text, morphology record, translation/gloss, and audio source is distributable under the terms documented in `THIRD_PARTY_NOTICES.md`. The app visibly treats meanings and grammar as study aids rather than definitive religious guidance.
