# Contributing to Kalima

Kalima is an Android-first Quranic Arabic learning project. Keep changes focused,
preserve unrelated work, and do not include generated packages or private signing
material in commits.

## Development setup

Use Android Studio with JDK 17, or run the Gradle wrapper from PowerShell:

```powershell
.\gradlew.bat -g .gradle-cache :app:testDebugUnitTest
```

After the final Android source edit, run the mandatory lock-screen contract check:

```powershell
.\gradlew.bat -g .gradle-cache :app:verifyLockScreenRegression
```

Run lint or APK assembly when the affected manifest, resources, packaging, or
integration surface requires it.

## Pull requests

- Explain the user-visible outcome and the checks you ran.
- Add or update focused tests for behavior changes.
- Keep Android lock-screen cards launching on `ACTION_SCREEN_ON` while the
  keyguard is still locked. Do not wait for `ACTION_USER_PRESENT`.
- Preserve `showWhenLocked`, the explicit locked-device safety allowance, and
  the rule that Kalima never bypasses PIN, password, or biometrics.
- Do not commit `local.properties`, environment files, keystores, passwords,
  APKs, source archives, build directories, or generated working output.

## Content changes

Quranic text, morphology, roots, meanings, and translations require traceable
sources and specialist review. Follow `CONTENT_REVIEW.md` and preserve the
editorial-review status until the relevant material has been audited.
