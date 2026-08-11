# Kalima workspace instructions

For every completed application change that produces a new version:

1. Increment `versionCode` and `versionName`.
2. Add a dated section for the version at the top of `CHANGELOG.md`, summarizing its user-visible features and fixes.
3. Run the relevant unit tests, Android lint, and APK assembly.
4. Never overwrite or delete an existing file in `releases/`.
5. Copy the validated APK to `releases/kalima-<version>-debug.apk`.
6. Commit the complete source state and create the matching annotated Git tag `v<version>`.
7. Create `releases/kalima-<version>-source.zip` from that tag.
8. Update `releases/SHA256SUMS.txt` with checksums for the APK and source archive.

Preserve existing progress-compatible identifiers and unrelated user changes. If a requested release name already exists, stop instead of replacing the backup.
