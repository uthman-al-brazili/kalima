# Kalima workspace instructions

Use subagents for concrete, bounded subtasks whenever project work can be
usefully divided or parallelized. The primary agent remains responsible for
integrating the results and verifying the completed work. Trivial atomic tasks
do not need artificial delegation.

For every delegated subtask, explicitly select the subagent's model and
reasoning effort using current official OpenAI model guidance. Choose the least
costly available model and the lowest effort reasonably expected to meet the
subtask's acceptance criteria, considering complexity, risk, required quality,
latency, and expected token use. Escalate the model or effort only when the
task's difficulty, impact, or verification results justify it. Do not hard-code
a model/effort matrix that may become stale; re-check the official guidance as
the available model lineup changes.

Application changes target the Android version only by default. Do not change,
version, build, test, package, or release the Windows version unless the user
explicitly requests Windows work. Preserve the existing Windows source and
release artifacts when completing Android changes.

For every completed Android application change that produces a new version:

1. Increment Android `versionCode` and `versionName` in `app/build.gradle.kts`.
2. Add a dated section for the version at the top of `CHANGELOG.md`, summarizing its user-visible features and fixes.
3. Run the relevant unit tests, Android lint, and APK assembly.
4. Never overwrite or delete an existing file in `releases/`.
5. Copy the validated APK to `releases/kalima-<version>-debug.apk`.
6. Commit the complete source state and create the matching annotated Git tag `v<version>`.
7. Create `releases/kalima-<version>-source.zip` from that tag.
8. Update `releases/SHA256SUMS.txt` with checksums for the APK and source archive.

Preserve existing progress-compatible identifiers and unrelated user changes. If a requested release name already exists, stop instead of replacing the backup.

Lock-screen cards are a critical Android regression contract. They must launch
over the still-locked keyguard when the display turns on, including on a Samsung
Galaxy M23 5G running Android 14; they must not wait for `ACTION_USER_PRESENT`.
For every Android application change, run `verifyLockScreenRegression` in
addition to the relevant tests, lint, and assembly. Never remove or bypass the
manifest/window `showWhenLocked` declarations, the `ACTION_SCREEN_ON` launch
path, or the explicit locked-device safety allowance. When a compatible Android
14 device is connected, also install the candidate APK and exercise a real
screen-off/screen-on cycle before completing the release.
