# Kalima update notes

This file records what changed in every published Kalima version. New releases
must add their notes here before the release commit and tag are created.

## 0.29.2 — 2026-08-23

- Removed search from the Arabic alphabet reference and laid out each row from
  right to left, beginning with the base letter on the right.
- Kept vowel-form playback on Arabic script and re-applied the selected Arabic
  device-voice locale for every utterance, including alif with kasra (`إِ`).
- Added explicit no-internet feedback when uncached word or Al-Hussary ayah audio
  cannot play, including connections that drop after playback begins.

## 0.29.1 — 2026-08-23

- Split the Arabic alphabet reference into seven compact four-letter pages and
  added search by Arabic symbol, transliterated name, or vowel form.
- Corrected the initial alif vowel forms to use hamza (`أَ`, `إِ`, `أُ`, `أْ`),
  with matching transliterations and device-voice playback text.
- Restored the full inline explanations and status details in Settings, removing
  the compact per-setting information buttons and dialogs.
- Removed approximate proportional word highlighting during Al-Hussary audio
  while preserving complete-ayah playback and the static word explorer.

## 0.29.0 — 2026-08-22

- Added an always-available Arabic alphabet table in Basics with all 28 letters,
  fatḥa, kasra, ḍamma, and sukūn variants, transliteration, and tap-to-hear audio.
- Added an information button to each setting and moved lengthy explanations into
  on-demand dialogs, making the Settings screen substantially more compact.
- Replaced the in-app donation action with a direct link to the Kalima website.
- Kept the current Study card, reveal and scroll position, and the exact Quiz
  session, answer, score, mode, and scroll position when switching tabs.
- Show the tested word's translation after every quiz answer and calculate the
  result percentage from the current quiz, so a perfect 5/5 displays 100%.
- Allow multiple guided paths to be selected together, studying their stable,
  duplicate-free union while preserving legacy progress and backup compatibility.
- Made the Quran reader show its first offline page before the optional word-detail
  index finishes, and highlight each word in sequence during Al-Hussary ayah audio.

## 0.28.5 — 2026-08-22

- Fixed the Quiz tab closing the app when a saved study selection did not have
  enough distinct answers, a state that could remain after every scheduled
  review was completed and disappear after reinstalling the app.
- Kept quiz questions inside the selected content while safely supplementing
  their answer choices with eligible vocabulary, with a clear empty state if
  four distinct choices still cannot be formed.

## 0.28.4 — 2026-08-21

- Made the Android app reach its first usable screen sooner by keeping all 604
  offline Quran pages and their word-lookup index out of the cold-start path.
- Load the offline Quran reader only when its tab is opened, with a clear
  loading state, while preserving complete word details after it is ready.
- Added a mandatory startup regression contract alongside the lock-screen
  contract so later builds cannot silently restore the expensive startup work.

## 0.28.3 — 2026-08-21

- Restored study and quiz cards directly over the still-locked keyguard when
  the display turns on, including the Samsung Galaxy M23 5G on Android 14,
  without dismissing the system lock or biometrics.
- Added a mandatory build-time lock-screen regression contract so every future
  Android build verifies the manifest, window, screen-wake, and safety paths
  required for this behavior before an APK can be assembled.

## 0.28.2 — 2026-08-21

- Kept the “New” badge visible for a word's entire first presentation so it
  does not appear to become a review before the learner moves on.

## 0.28.1 — 2026-08-21

- Made brand-new words reveal their meaning and learning details immediately,
  with a single “Next word” action in Study and no “Hard” or “Easy” grading on
  return-to-phone cards until the learner encounters the word again.
- Recorded each first presentation as a neutral introduction that counts the
  word as encountered and schedules its first recall without treating it as a
  correct answer, a mistake, or a lapse.

## 0.28.0 — 2026-08-21

- Reworked root and surah statistics as clearly explained word coverage with
  progress bars, replacing the ambiguous “familiar” mastery counts.
- Made every Study and return-to-phone card ask learners to recall the
  translation before revealing it, followed by direct “Hard” and “Easy”
  grading, and removed internal lemma terminology from learner-facing notes.
- Matched alphabet playback to the fully vocalized Arabic letter name shown
  beside its transliteration and preferred a clear Saudi Arabic device voice.
- Hardened return-to-phone launching so notification-only screen wakes never
  open Kalima, while a real completed unlock is detected once and retried
  briefly if Android is still dismissing the keyguard.
- Kept Study, Quiz, reminders, and return-to-phone cards available when a
  learner voluntarily restarts the alphabet after already knowing, finishing,
  or skipping it.
- Replaced the visible activity dates with a learner-friendly Hijri calendar,
  including Arabic and translated weekdays and month names plus tap-to-hear
  Arabic pronunciation for the current weekday and month.

## 0.27.2 — 2026-08-20

- Renamed the number lesson heading to “Arabic-Indic number” so learners can
  study multiple digits on the same day without a misleading daily label.

## 0.27.1 — 2026-08-20

- Renamed the number lesson’s advance button to “Next number” so its action is
  direct and clear.
- Removed the Excluded words shortcut from the active Study screen to leave
  more room for the lesson; excluded words remain available in Vocabulary.
- Kept bottom-navigation labels such as “Progresso” on one line on narrow
  phone screens.

## 0.27.0 — 2026-08-20

- Moved alphabet and Arabic-Indic number learning into a separate, optional
  Basics tab so the Study tab stays focused on Quranic words.
- Added clear routes to the Basics tab for learners who still need the alphabet,
  and made both alphabet and number courses available to restart at any time.
- Moved the “Content under review” control into a labeled footer within each
  word card, keeping it attached to the word’s source and editorial details.

## 0.26.4 — 2026-08-20

- Moved “I already know this word” beside the Arabic study word and added a
  clear confirmation message with an immediate Undo action after exclusion.
- Added a prominent Excluded words entry from Study and Vocabulary that shows
  every excluded word, including words outside the current study set, with a
  one-tap option to add each word back to studies.
- Added confirmation feedback when a word is excluded from a return-to-phone
  study card and clarified the same recovery path in both supported languages.

## 0.26.3 — 2026-08-20

- Moved “I already know this” and the editorial-review status into a quiet
  utility row below the vocabulary card, leaving the card itself focused on the
  Arabic word and its lesson content.
- Added Arabic device-voice playback for every alphabet symbol and number, with
  fully vocalized letter and number names and guidance when an Arabic voice is
  not installed on the device.
- Made alphabet lessons optional and reversible: learners can skip without
  losing partial progress, resume later, or restart the alphabet from Study
  after finishing it or saying they already knew it during onboarding.

## 0.26.2 — 2026-08-20

- Reworked alphabet lessons into focused, one-symbol-at-a-time learning followed
  by a required recognition check with immediate retry and review options.
- Replaced the collapsed content-review banner with a compact status icon while
  keeping the full editorial details and reporting action one tap away.
- Removed the duplicate pronunciation button from quiz feedback when the Arabic
  question card already provides the same audio action.

## 0.26.1 — 2026-08-20

- Hid word-study path and daily word-goal choices from first-time learners who
  do not yet know the Arabic alphabet, keeping onboarding focused on starting
  their letter foundations.

## 0.26.0 — 2026-08-20

- Added first-launch questions about Arabic alphabet and number knowledge, with
  a personalized foundation plan for learners who need either course.
- Added eight right-to-left alphabet lessons covering all 28 letters, joining,
  and short vowels; complete-word study and quizzes now wait until required
  alphabet foundations are finished.
- Added a parallel course for the Arabic-Indic digits ٠–٩ that can continue
  alongside alphabet or vocabulary study without blocking either path.
- Added a pronunciation control directly to quiz questions that display an
  Arabic word.
- Simplified study-card audio to one icon-only action, removed the three-repeat
  control, and moved “I already know this” to a compact action at the top.
- Redirected optional support to Uthman (Gustavo), clarified that it funds
  Kalima and future apps for Muslims, and added a direct developer email action.

## 0.25.0 — 2026-08-19

- Prevented notification-only display wakes from opening Kalima on devices such
  as the Samsung Galaxy M23 5G running Android 14; return-to-phone cards now wait
  until Android confirms that the user has unlocked the device.
- Added persistent A− and A+ text-size controls to the Quran reader, with a
  comfortable range that reflows every page without clipping Arabic words.
- Added real Quran.com audio for each tapped Quran word and a direct button to
  add or remove that word from the personal study list.
- Simplified word details and Settings by removing “Copy with citation” and the
  redundant synthesized-audio sentence, and by tightening labels and reader
  controls for a calmer, less overwhelming interface.
- Added an optional Quran.Foundation donation entry in Settings. Kalima still
  has no ads, accounts, or tracking, and the donation page opens only when the
  user taps the button.
- Improved responsiveness on lower-end phones by indexing word lookups, caching
  corpus identity and surah-page locations, moving large audio/search work off
  the UI thread, composing fewer Quran pages, and avoiding repeated preference
  writes while settings sliders are being dragged.

## 0.24.7 — 2026-08-14

- Fixed missing meanings and grammar when tapping words in the complete ayah
  shown on study and vocabulary cards.
- Matched complete-ayah words by their exact Quran positions and verified every
  tappable token across all 6,236 ayahs in English and Portuguese.

## 0.24.6 — 2026-08-14

- Made meanings and grammar ready for every Quran word before the reader can be
  used, instead of waiting for a deferred word-detail index.
- Added full Arabic-form and detail coverage checks for every tappable Quran
  token in both English and Portuguese.

## 0.24.5 — 2026-08-14

- Fixed Quran word details occasionally showing an unindexed fallback instead
  of the available meaning and grammar, including وَإِذْ in Al-Baqarah 2:34.

## 0.24.4 — 2026-08-14

- Reflowed Quran page text across the bundled Mushaf row boundaries, keeping
  the larger readable type without producing isolated one-word lines.
- Preserved surah headings and tap-for-word-details while the page text reflows.

## 0.24.3 — 2026-08-14

- Fixed the Quiz tab restoring the previously selected second answer when it
  was reopened; every newly opened quiz now starts unanswered.

## 0.24.2 — 2026-08-14

- Kept Quran text at a comfortably readable size instead of shrinking long
  Mushaf rows to fit narrow screens.
- Wrapped long Arabic rows within the page margins so letters and words are no
  longer clipped, while preserving tap-for-details and vertical scrolling.

## 0.24.1 — 2026-08-14

- Made Quran pages turn in right-to-left reading order, including swipe gestures
  and the previous/next controls.
- Kept every bundled Mushaf source line together and fitted it to the available
  width, preventing overflow words from appearing alone on extra lines.
- Preserved word-by-word details when tapping the newly composed Quran lines.

## 0.24.0 — 2026-08-14

- Replaced synthesized full-ayah speech with Mahmoud Khalil Al-Hussary’s
  verse-by-verse Murattal recitation throughout study cards and Quran word
  details.
- Cached Al-Hussary recitations after online playback and added each selected
  ayah to the existing offline audio download, with no synthesized Quran audio
  fallback.
- Rebuilt the Quran tab as a calm 604-page reader that follows the bundled
  Mushaf page and line layout, with swipe navigation and direct jumps by page
  or surah.
- Made every Quran word tappable for offline transliteration, meaning, grammar,
  verse context, and citation details.
- Reduced visual clutter in the reader to keep attention on the Arabic page,
  while leaving navigation and word details available only when needed.

## 0.23.1 — 2026-08-14

- Fixed the startup crash introduced in 0.23.0 by opening the offline Quran
  text under the filename Android assigns to the packaged asset.
- Kept the rest of the app available if a bundled Quran text asset cannot be
  opened or decoded.

## 0.23.0 — 2026-08-14

- Hid the complete ayah on word cards by default and added a show/hide control
  whose choice persists across study, vocabulary, and lock-screen cards.
- Added an offline Quran tab with all 114 surahs and 6,236 Arabic ayahs,
  searchable surah selection, and previous/next navigation.

## 0.22.0 — 2026-08-14

- Made individual words use only Quran.com’s real human recordings, including
  offline playback, with no synthesized word-audio fallback.
- Added resumable downloads for the selected study content and automatically
  preserved every recording played online for later offline use.
- Added download-size confirmation, progress, cancellation, and retry-friendly
  reuse of recordings already saved on the device.

## 0.21.0 — 2026-08-14

- Made every individual word playable without an internet connection by
  automatically using the Android Arabic voice while offline.
- Preserved Quran.com’s exact word-by-word recording when connected and added
  clearer guidance for installing Arabic voice data for offline playback.

## 0.20.0 — 2026-08-14

- Replaced Android-generated speech for individual words with the exact
  word-by-word audio stream used when clicking a word on Quran.com, across
  study, vocabulary, quiz, and return-to-phone cards.
- Kept full-verse previews on the Android Arabic voice and clarified that word
  audio requires an internet connection.

## 0.19.3 — 2026-08-13

- Kept cooldown values such as “5 minutos” on one readable horizontal line in
  advanced settings, including on narrow screens.
- Restored study cards on the Android lock screen when the display turns on,
  including Android 14 devices, without dismissing the system lock or biometrics.

## 0.19.2 — 2026-08-13

- Made the app show its first screen immediately while the full Quran vocabulary
  and saved progress load away from the interface thread.
- Deferred search and word-detail indexes, background-feature synchronization,
  and text-to-speech startup so they no longer hold up opening the app.

## 0.19.1 — 2026-08-13

- Fixed the word limit so words removed as already known no longer occupy
  learning slots, including words that had earlier study or review history.

## 0.19.0 — 2026-08-13

- Added a reversible “I already know this” choice to study cards and unlock
  cards so familiar words stop appearing in study, quizzes, reminders, and
  unlock-card rotation.
- Added an “Already known” status and Vocabulary filter for finding excluded
  words and returning any of them to practice.
- Included already-known words in local progress backups while preserving
  compatibility with backups created by earlier Kalima versions.

## 0.18.0 — 2026-08-13

- Merged Favorites and My List into one clear My List collection across study,
  quiz, progress, library filters, and saved-word actions.
- Preserved every previously saved word by automatically combining existing
  favorites and custom-list entries, including those restored from old backups.

## 0.17.2 — 2026-08-13

- Renamed the study choices to “Still learning” and “Got it” so they make
  sense for both brand-new words and words returning for review.

## 0.17.1 — 2026-08-13

- Kept the study reveal and review choices in a persistent, thumb-friendly
  action area above the app navigation so they remain easy to reach while the
  word card scrolls.

## 0.17.0 — 2026-08-13

- Changed return-to-phone study cards to appear only after an intentional,
  secure-device unlock, with configurable cooldowns and automatic deferral for
  calls, alarms, media, car mode, battery saver, and thermal pressure.
- Made unlock-card grading interruption-safe: choices require explicit
  confirmation, duplicate commits are rejected, and interrupted cards resume
  without silently changing progress.
- Added precomputed unlock sessions, a measurable 700 ms launch budget, and
  launch-latency and safety-skip diagnostics in advanced settings.
- Added checksum-validated local progress export and restore, including a
  restore preview and an automatic recovery backup before current data changes.
- Turned the complete ayah into a tappable word explorer, with indexed word
  details, other occurrences in the offline corpus, and graceful partial
  details for tokens that are not yet indexed.
- Added citation-safe copy and share actions that always include the Quran
  reference, Kalima card identity, and corpus identity.

## 0.16.2 — 2026-08-13

- Made study-card audio and saved-word controls more compact, using short,
  recognizable actions that stay on one row.
- Reworded review choices to distinguish “didn’t remember” from “remembered”
  and show when the word will return.
- Clarified the 14-day activity chart, grouped study paths by purpose with
  plain-language names and a current-selection summary, and marked today.
- Reduced editorial notices to a compact “Content under review” row with
  details and correction sharing available on demand.

## 0.16.1 — 2026-08-13

- Improved the contrast of the “Selected content” heading on the Progress screen
  in both light and dark themes.

## 0.16.0 — 2026-08-13

- Decoupled the Quiz tab from spaced repetition: quizzes now use random words
  from the selected content and keep quiz accuracy without changing study
  schedules, daily study progress, or review timing.
- Added an advanced setting to disable spaced repetition, with a visible warning
  explaining why timed reviews remain recommended. Existing schedules are kept
  so they can be resumed later.
- Added clear Google text-to-speech guidance in Audio settings and when an
  Arabic voice is unavailable, including a shortcut to Android's text-to-speech
  settings and automatic voice-engine refresh after returning to Kalima.

## 0.15.1 — 2026-08-12

- New words now show their meaning and learning details immediately the first
  time they are presented. Words already being reviewed still hide the answer
  first so the learner can test their recall.

## 0.15.0 — 2026-08-12

- Added optional Windows welcome-back study cards that detect renewed keyboard
  or mouse activity after a configurable period away from the PC, including
  returns after Windows has been locked.
- Matched the Android screen-on experience with Arabic pronunciation, review
  actions, occasional quizzes, study-scope and spaced-repetition selection,
  quiet hours, daily limits, snoozing, and a non-intrusive 45-second timeout.
- Added Windows tray background operation, optional start-with-Windows support,
  a preview action, and full-screen deferral so games, presentations, and videos
  are not covered. Android wording now describes the corresponding behavior as
  studying when the user returns to the phone.

## 0.14.4 — 2026-08-12

- Fixed Windows pronunciation buttons silently doing nothing by using modern
  Windows speech voices with a classic SAPI fallback, detecting whether an
  Arabic voice is installed, and showing a localized recovery dialog that
  opens Speech settings when one is needed.
- Standardized speech text cleanup across Android and Windows so both device
  voices receive the same normalized Arabic text.
- Applied the official book-and-kāf logo consistently to Android onboarding
  and notifications and to Windows onboarding, sidebar, title bar, tray,
  executable, installer, and Start menu surfaces.

## 0.14.3 — 2026-08-12

- Brought the Windows release up to date with Android: Arabic vocabulary
  searches now ignore vowel marks and common alif variants, and root searches
  accept either spaced or joined Arabic letters.
- Made the Windows onboarding foreground and background colors explicit so its
  text and controls retain readable contrast in dark mode.
- Synchronized Android and Windows version metadata and release artifacts.

## 0.14.2 — 2026-08-12

- Fixed low-contrast onboarding text and controls when the phone uses dark mode.
- Arabic vocabulary searches now ignore vowel marks and common alif variants,
  and root searches work with either spaced or joined Arabic letters.

## 0.14.1 — 2026-08-12

- Fixed open Windows screens continuing to show the previous language's word
  content after switching between Portuguese and English.
- Fixed the Windows tray reminder remaining registered after the main window
  was closed.

## 0.14.0 — 2026-08-12

- Added a native, self-contained Windows application with onboarding, study
  cards, spaced repetition, all quiz modes, searchable library, progress
  dashboard, surah/path selection, themes, Portuguese/English localization,
  device speech, and local reminders.
- The Windows build shares the Android app's 42,117-card offline corpus,
  progress-compatible word identifiers, review scheduler, and quiz engine.
- Windows progress is saved locally under `%APPDATA%\Kalima`; no account,
  network connection, or separate Java installation is required at runtime.
- Added a Windows `.exe` installer, desktop persistence tests, a packaged-app
  smoke test, and Windows build/run instructions.
- Kept secure lock-screen integration Android-only, with an explanation in the
  Windows settings because regular apps cannot overlay the Windows sign-in
  screen.

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
