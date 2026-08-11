# Kalima update notes

This file records what changed in every published Kalima version. New releases
must add their notes here before the release commit and tag are created.

## 0.9.1 — 2026-08-11

- When an Arabic voice is missing, the pronunciation button now opens Android's
  voice-data installer for the active text-to-speech engine.
- Added fallbacks to the text-to-speech settings and general device settings on
  phones that do not expose the standard voice installer.
- The app now checks for an Arabic voice again after installation, allowing
  playback without restarting Kalima.

## 0.9.0 — 2026-08-11

- Added an **Open Kalima** button to study and quiz cards on the lock screen.
- Opening the full app from a card now asks Android to unlock the device first,
  preserving the phone's PIN, pattern, password, and biometric protection.

## 0.8.2 — 2026-08-11

- Removed the redundant context-meaning line from expanded Words cards while
  preserving the Arabic verse and the additional usage note.

## 0.8.1 — 2026-08-11

- Removed the redundant context-meaning line from Study cards, leaving the
  Arabic verse as the focused usage example without repeating the word meaning.

## 0.8.0 — 2026-08-11

- Added Arabic pronunciation playback to study cards, vocabulary entries, quiz
  feedback, and lock-screen learning.
- Added clear localized guidance when the device is still preparing speech or
  does not have an Arabic text-to-speech voice installed.
- Normalized Quranic word forms before speech playback while preserving the
  vowel marks that guide pronunciation.

## 0.7.1 — 2026-08-11

- Reduced the launcher emblem's visual size so the book fits comfortably inside
  Android's adaptive icon masks without clipping and aligns better with other
  home-screen icons.

## 0.7.0 — 2026-08-11

- Added English as an optional app language while keeping Portuguese as the
  default.
- Added a language selector under **Progress**. The selected language now
  applies to the interface, word meanings, quizzes, notifications, and
  lock-screen study cards.
- Expanded the offline vocabulary data with English meanings while preserving
  the identifiers used by existing progress data.

## 0.6.1 — 2026-08-11

- Corrected the launcher artwork's cover color so the icon displays with the
  intended background.
- Updated the Play Store and Android launcher images with the corrected artwork.

## 0.6.0 — 2026-08-11

- Replaced the original launcher graphic with a complete adaptive app-icon set.
- Added round, monochrome, and density-specific launcher assets for supported
  Android versions.
- Added the matching high-resolution Play Store icon.

## 0.5.0 — 2026-08-11

- Published the first preserved Kalima release.
- Added offline Quranic Arabic vocabulary study across all 114 surahs, including
  frequent-word and surah-based study modes.
- Added searchable vocabulary, learning-status filters, daily goals, streaks,
  and local progress storage.
- Added five-question quiz sessions and automatic mastery tracking across
  different study days.
- Added optional study cards and quizzes when the screen turns on, plus a daily
  reminder notification.
