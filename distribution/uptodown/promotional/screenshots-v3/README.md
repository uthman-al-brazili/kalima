# Uptodown promotional screenshots v3

This is the current upload-ready screenshot set for Kalima 0.34.0.

- Upload the four images under `en/` or `pt-BR/` for the matching listing locale.
- Canvas: 1080×2400 PNG.
- Visual direction: concise lowercase benefit headline, Kalima's restrained forest/sage/sand palette, and a complete consistent Android phone around every real screenshot.
- The rhythm is inspired by Duolingo's September 1, 2026 Google Play screenshots, without reusing its characters, illustrations, colors, copy, logo, or interface.
- Every interface shown is an unedited Kalima 0.34.0 Android screenshot. The composition only scales it into the rounded phone display.
- Raw captures are under `distribution/uptodown/screenshots/current-v0.34.0/`.
- The lock-screen captures were taken on a clean Android 14 emulator after a real screen-off/screen-on cycle. Android reported the device locked, the keyguard showing, and `LockScreenStudyActivity` focused.

Regenerate the compositions with:

```powershell
./distribution/uptodown/screenshots/compose_promotional_v3.ps1
```

The generated files and their raw screenshot inputs are checksummed in `SHA256SUMS.txt`.
