# Optional alphabet exercise recordings

Kalima resolves the new foundation audio by Android `res/raw` resource name. When a recording is
absent, the app displays an “audio not added yet” message and does not use the device voice.
Listening-only questions appear automatically after their matching recording is added.

Add `.mp3`, `.ogg`, or `.wav` files to `app/src/main/res/raw/` using these names:

- Pure letter sounds: `arabic_sound_<letter_slug>`. Example: `arabic_sound_baa.mp3`.
- Vowelled syllable clips: `arabic_sound_<letter_slug>_<vowel>` where vowel is `a`, `i`, or `u`.
  Example: `arabic_sound_baa_a.mp3`.
- Qur'anic milestone words: `arabic_word_tabbat`, `arabic_word_nahnu`, `arabic_word_rabb`,
  `arabic_word_sharr`, `arabic_word_siraat`, `arabic_word_qul`, and `arabic_word_huwa`.

Letter slugs are defined in `ArabicFoundations.kt` beside each letter. Existing
`arabic_alphabet_01_alif` through `arabic_alphabet_28_yaa` files remain the approved letter-name
recordings and are not replaced by these new phoneme/syllable clips.
