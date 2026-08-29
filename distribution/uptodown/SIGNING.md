# Production signing for Uptodown

Android updates must keep the same application ID and signing certificate.
Create the key once, store it outside this repository, and keep at least two
encrypted backups. Do not commit the keystore or passwords.

The Android build reads these environment variables only for the `release`
variant:

```text
KALIMA_KEYSTORE_FILE=C:\secure\kalima-release.jks
KALIMA_KEYSTORE_PASSWORD=<secret>
KALIMA_KEY_ALIAS=kalima
KALIMA_KEY_PASSWORD=<secret>
```

With all four variables set, build and verify the public APK:

```powershell
$env:ANDROID_USER_HOME = "$PWD\.android-home"
.\gradlew.bat '-Pkotlin.compiler.execution.strategy=in-process' -g .gradle-cache assembleRelease
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\<version>\apksigner.bat" verify --verbose --print-certs app\build\outputs\apk\release\app-release.apk
```

The build intentionally fails `packageRelease` when any signing value is
missing. Release work does not require a separate debug APK backup; build and
verify the signed public candidate needed for distribution.

For an interactive local walkthrough that keeps passwords out of chat and
shell history, run:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\distribution\uptodown\Sign-Current-Beta.ps1
```

The walkthrough can reuse the original production keystore or create the first
permanent key for an unpublished draft. A newly created key cannot update an APK
that has already been published under a different certificate.
