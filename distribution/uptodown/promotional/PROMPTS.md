# Kalima promotional image brief

## Current upload-ready set

The current Uptodown artwork is in `screenshots-v2/en/` and
`screenshots-v2/pt-BR/`. It uses unmodified 1080×2400 Android screenshots
inside a deterministic, matching text frame. The English and Brazilian
Portuguese versions use the same layout; only the localized headline,
explanation, and real in-app locale differ.

The first image in each locale was captured from Kalima 0.30.3 running on an
Android 14 emulator during a real screen-off/screen-on cycle. At capture time,
Android reported `deviceLocked=1`, `isKeyguardShowing=true`, and Kalima's
`LockScreenStudyActivity` as the focused activity.

The earlier generated mockups under `final/`, `en/`, and `pt-BR/` are retained
only as design history and must not be submitted.

## Archived generated-art brief

These localized portrait images were created with OpenAI's built-in image
generation and then reviewed against the implemented Android UI and corpus
facts. They are edited store artwork, not literal screenshots. The unedited
screenshots in `../screenshots/` remain the reference evidence.

## Shared direction

- 1024×1536 portrait store artwork with generous safe margins.
- Kalima's original cream, deep-green, mint, and warm-yellow palette.
- A modern, friendly learning rhythm inspired by concise lesson cards,
  visible progress, and focused recall interactions, without copying any
  third-party character, logo, screen, or trade dress.
- Show Android hardware generically and keep every product claim traceable to
  the shipped app.
- Keep front-facing phone mockups physically realistic and consistent: a
  fixed 20:9 phone proportion, slim even bezels, centered punch-hole camera,
  and balanced rounded corners. Use 20:9 for every phone created or edited in
  this project unless the user explicitly requests a different ratio.
- Arabic words, transliterations, Quran references, counts, and localized
  labels must remain exact and readable.

## Final image set

1. Lock-screen learning — show the implemented locked-keyguard word card and
   the headline “LEARN BEFORE YOU UNLOCK” / “APRENDA ANTES DE DESBLOQUEAR”.
   Include the truthful security note that Kalima never unlocks the phone or
   bypasses Android authentication.
2. Lock-screen quiz — show a compact answer interaction over the locked
   keyguard with the headline “A QUICK QUIZ WHEN YOUR SCREEN TURNS ON” /
   “UM QUIZ RÁPIDO QUANDO A TELA ACENDE”.
3. Daily word widget — show the implemented home-screen widget layout using
   `أَخَذْنَا`, transliteration `akhadhnā`, contextual meaning “We took” /
   “Nós pegamos”, reference `Al-Baqarah 2:63`, and the Next/Próxima action.
   Keep the launcher icons identical in both localized images: a forest-green
   phone, cream message, sage profile, and warm-gold camera, using the same
   filled glyphs, proportions, shadows, and spacing.
4. Quran context — show the real Al-Fatihah reading experience and the verified
   product facts: 114 surahs, 6,236/6.236 ayahs, and 42,117/42.117 offline word
   cards.

The generated outputs are archived under `final/`, `en/`, and `pt-BR/` and are
not the current Uptodown submission set.
