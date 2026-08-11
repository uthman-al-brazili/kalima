# Kalima workspace instructions

For every completed application change that produces a new version:

1. Increment `versionCode` and `versionName`.
2. Run the relevant unit tests, Android lint, and APK assembly.
3. Never overwrite or delete an existing file in `releases/`.
4. Copy the validated APK to `releases/kalima-<version>-debug.apk`.
5. Commit the complete source state and create the matching annotated Git tag `v<version>`.
6. Create `releases/kalima-<version>-source.zip` from that tag.
7. Update `releases/SHA256SUMS.txt` with checksums for the APK and source archive.

Preserve existing progress-compatible identifiers and unrelated user changes. If a requested release name already exists, stop instead of replacing the backup.
