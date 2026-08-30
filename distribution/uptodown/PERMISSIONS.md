# Kalima Android permission disclosure

Kalima does not use advertising, analytics, accounts, location, contacts,
camera, accessibility, or exact-alarm permissions.

## Network

- `INTERNET`: streams Quran.com word audio and Mahmoud Khalil Al-Hussary ayah
  audio, downloads selected audio for offline playback, and opens an external
  support page only after the user taps it.
- `ACCESS_NETWORK_STATE`: avoids starting an online audio request when Android
  has not validated an internet connection.

## Optional reminders

- `POST_NOTIFICATIONS`: shows the daily study reminder and the visible status
  notification required while return-to-phone study is enabled. Requested only
  when needed on Android versions that require runtime permission.
- `RECEIVE_BOOT_COMPLETED`: restores a reminder or return-to-phone study service
  that the user previously enabled after the device restarts.

## Optional return-to-phone study

- `SYSTEM_ALERT_WINDOW`: lets a study card appear over the lock screen after the
  user turns the feature on and grants Android’s “Appear on top” permission.
  Kalima does not dismiss, replace, or bypass the system PIN, password,
  fingerprint, or face authentication.
- `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_SPECIAL_USE`: keep an opt-in,
  user-visible service ready to detect screen-on/unlock events. Android shows a
  persistent notification while this service is active. The manifest declares
  the special-use subtype in plain language.

All optional background features are disabled by default. If the learner needs
the alphabet foundation, word reminders and return-to-phone word cards cannot
be enabled until that foundation is complete.
