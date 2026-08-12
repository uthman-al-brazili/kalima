# Kalima update notes

This file records what changed in every published Kalima version. New releases
must add their notes here before the release commit and tag are created.

## 0.13.2 — 2026-08-12

- Fixed quiz answers unexpectedly switching to a different question while the
  tap was being recorded, which could show a correct choice as a failure.

## 0.13.1 — 2026-08-12

- Fixed the English Progress screen so surah mastery rows use “Surah” instead
  of the Portuguese word “Surata”.

## 0.13.0 — 2026-08-12

- Added first-run setup and guided First 50, Top 100/300/500, prayer, short-surah,
  full-corpus, favorite, custom-list, and selected-surah learning paths.
- Study cards now hide the answer until reveal, support favorites and personal
  lists, provide slow word and verse playback using the device voice, and show
  an honest editorial-draft status with a structured correction report.
- Added focused listening, verse-cloze, root-family, due-only, and
  difficult-only quiz modes on top of the existing mixed practice and
  spaced-repetition scheduling.
- Expanded Progress with 7- and 30-day accuracy, new-versus-reviewed activity,
  a 14-day calendar, difficult cards, mastery by root and surah, and path
  switching that preserves existing progress.
- Added quiet hours, a daily lock-screen card cap, one-hour/today pauses, and a
  service-health summary, plus explicit offline privacy and generated-audio
  disclosures in Settings.
- Replaced symbolic bottom navigation labels with accessible vector icons,
  improved Arabic RTL/language metadata, and expanded unit coverage for review
  history, guided paths, quiz modes, cloze generation, and lock-screen policy.

## 0.12.0 — 2026-08-12

- Added persistent, per-word spaced-repetition schedules with a 10-minute
  relearning step, one- and three-day graduation steps, and ease-adjusted
  intervals that expand after successful recall.
- Study, quiz, and lock-screen sessions now prioritize overdue cards, introduce
  new cards only after pending reviews, and hold back cards until their due
  time. Existing learned and reviewing progress is migrated into the schedule.
- Study actions preview the resulting interval, quiz feedback shows the next
  review, Progress reports the number currently due, and a new caught-up state
  appears when no review or new card is available.
- Quiz sessions no longer repeat a word merely to fill five questions when the
  due queue contains fewer cards.

## 0.11.0 — 2026-08-11

- Added light, dark, and automatic appearance modes, with a dark palette across
  study, vocabulary, quiz, progress, settings, and system bars.
- Added a dedicated **Settings** tab with straightforward appearance, language,
  reminder, and daily-goal controls. Lock-screen automation, quiz timing, and
  word limits remain available behind an optional advanced-settings switch.
- New installations now use English unless the phone language is Portuguese;
  existing manually selected languages remain unchanged.
- The currently displayed study word is now saved locally so reopening Kalima
  continues on the same card. Intentional lock-screen card launches still take
  priority.

## 0.10.0 — 2026-08-11

- Added an optional, persistent maximum-word setting under **Progress > Choose
  words**, with an exact user-entered limit.
- Study cards, quizzes, daily reminders, and lock-screen learning now stop
  introducing unseen words when the maximum is reached while continuing to
  offer previously learned and reviewing words.
- Lowering the limit never removes existing progress, and switching the limit
  off restores the full selected study set.

## 0.9.3 — 2026-08-11

- Opening Kalima from a lock-screen study or quiz card now selects the **Study**
  tab and shows that card's exact word first.
- The same destination is honored when Kalima is already open on another tab,
  including repeated openings of the same word.

## 0.9.2 — 2026-08-11

- Removed the redundant context-meaning line from lock-screen study cards.
- Fixed the rounded verse panel being visually cut off by placing the **Open
  Kalima** action in the scrollable content on shorter screens.

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
