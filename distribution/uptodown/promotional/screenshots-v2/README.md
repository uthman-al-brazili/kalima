# Uptodown screenshot-based promotional set

Use the files in `en/` and `pt-BR/` for the current submission.

- Canvas: 1080×2400 PNG.
- Phone frame: 864×1920, an exact 9:20 width-to-height ratio.
- Rounded display area: 828×1840, also an exact 9:20 ratio.
- The app UI is a real screenshot and is not regenerated or redrawn.
- English and pt-BR use the same frame, dimensions, colors, typography, and
  screenshot placement.
- The first image in each locale is a real Android 14 lock-screen capture from
  Kalima 0.30.3. Android reported the device locked and the keyguard showing
  while `LockScreenStudyActivity` was focused.
- The remaining images use the existing real Study, Quran, and Arabic Basics
  screenshots from `distribution/uptodown/screenshots/`.

Regenerate the frames with:

```powershell
./distribution/uptodown/screenshots/compose_promotional.ps1
```

The pt-BR lock-screen capture intentionally preserves the app exactly as it
rendered. It currently exposes three untranslated content labels (`Surah 2`,
`noun`, and `It appears 16 times in this surah.`); those are app-localization
issues rather than artwork edits.
