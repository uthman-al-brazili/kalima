#!/usr/bin/env python3
"""Generate the offline Kalima vocabulary snapshot.

Sources:
- Quranic Arabic Corpus morphology v0.4-compatible data:
  https://github.com/mustafa0x/quran-morphology
- Quran Foundation Content API v4 for Uthmani context and word glosses:
  https://api.quran.com/api/v4

The generated file is deterministic once ``pt_gloss_cache.json`` exists. The
first run uses Google Translate only to create a Portuguese editorial draft;
those glosses must be reviewed before publication.
"""

from __future__ import annotations

import argparse
import collections
import dataclasses
import hashlib
import html
import json
import re
import sys
import time
import unicodedata
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path


MORPHOLOGY_URL = (
    "https://raw.githubusercontent.com/mustafa0x/"
    "quran-morphology/master/quran-morphology.txt"
)
API_ROOT = "https://api.quran.com/api/v4"
LAST_SURAHS = range(101, 115)
FREQUENT_LIMIT = 100

SURAH_NAMES = {
    101: "Al-Qāriʿah",
    102: "At-Takāthur",
    103: "Al-ʿAṣr",
    104: "Al-Humazah",
    105: "Al-Fīl",
    106: "Quraysh",
    107: "Al-Māʿūn",
    108: "Al-Kawthar",
    109: "Al-Kāfirūn",
    110: "An-Naṣr",
    111: "Al-Masad",
    112: "Al-Ikhlāṣ",
    113: "Al-Falaq",
    114: "An-Nās",
}

KNOWN_TRANSLATIONS = {
    "(in) a Fire": "em um Fogo",
    "(is) the Striking Calamity": "é a Calamidade que golpeia",
    "(of the) Quraish": "dos coraixitas",
    "(of) the whisperer": "do sussurrador",
    "(the) breasts": "os peitos",
    "(the) carrier": "a carregadora",
    "(will be the) Pit": "será o Abismo",
    "A Fire": "um Fogo",
    "Al-Kauthar": "a Abundância (Al-Kawthar)",
    "Allah": "Deus",
    "By the time": "pelo tempo",
    "Diverts you": "distrai-vos",
    "For (the) familiarity": "pela familiaridade",
    "For you": "para vós",
    "Have you seen": "viste",
    "the Lord": "o Senhor",
    "Lord": "Senhor",
    "He is begotten": "Ele foi gerado",
    "I seek refuge": "busco refúgio",
    "I worship": "eu adoro",
    "Oft-Returning": "Aceitador do arrependimento",
    "Perish": "pereça",
    "Say": "dize",
    "So let them worship": "que adorem",
    "So pray": "ora, pois",
    "So woe": "ai, pois",
    "The Striking Calamity": "a Calamidade que golpeia",
    "This (is)": "isto é",
    "We have given you": "Nós te concedemos",
    "[the] small kindnesses": "as pequenas ajudas",
    "a worshipper": "um adorador",
    "and ask His forgiveness": "e pede-Lhe perdão",
    "and counts it": "e o conta",
    "and enjoin (each other)": "e recomendam uns aos outros",
    "and gives them security": "e lhes deu segurança",
    "and perish he": "e que ele pereça",
    "and sacrifice": "e sacrifica",
    "and the Victory": "e a vitória",
    "any [one]": "qualquer um",
    "astray": "em fracasso",
    "backbiter": "difamador",
    "baked clay": "barro cozido",
    "believe[d]": "creram",
    "closed over": "fechado sobre eles",
    "dealt": "tratou",
    "disbelieve[d]": "descreram",
    "eaten up": "devorada",
    "fluffed up": "cardada",
    "he earned": "ele ganhou",
    "his scales": "suas balanças",
    "in (the) Lord": "no Senhor",
    "intensely hot": "escaldante",
    "kindled": "aceso",
    "like straw": "como palha mastigada",
    "make show": "agem para serem vistos",
    "mounts up": "alcança",
    "palm-fiber": "fibra de palmeira",
    "pleasant": "agradável",
    "repulses": "repele",
    "the Crusher": "a Destruidora",
    "the Crusher (is)": "a Destruidora é",
    "the Eternal, the Absolute": "o Eterno, o Absoluto",
    "the competition to increase": "a competição por acumular",
    "the one cut off": "privado de posteridade",
    "the one who withdraws": "que se retira",
    "to [the] patience": "à perseverança",
    "with (the) praises": "com louvores",
    "you visit": "visitardes",
    "He": "Ele",
    "And": "e",
    "Not": "não",
    "from": "de",
    "in": "em",
    "with": "com",
    "you": "tu",
    "they": "eles",
    "We": "Nós",
    "I": "eu",
}


@dataclasses.dataclass(frozen=True)
class Segment:
    surface: str
    pos: str
    features: str


@dataclasses.dataclass(frozen=True)
class MorphWord:
    surah: int
    verse: int
    position: int
    surface: str
    normalized: str
    lemma: str
    root: str
    grammar: str

    @property
    def location(self) -> tuple[int, int, int]:
        return self.surah, self.verse, self.position


@dataclasses.dataclass(frozen=True)
class ApiContext:
    arabic: str
    transliteration: str
    english_gloss: str
    verse_arabic: str


@dataclasses.dataclass(frozen=True)
class OutputWord:
    id: str
    arabic: str
    lemma: str
    transliteration: str
    meaning: str
    root: str
    grammar: str
    category: str
    reference: str
    verse_arabic: str
    verse_meaning: str
    insight: str
    frequency: int
    surah_number: int | None
    is_frequent: bool


def request_json(url: str, attempts: int = 4) -> dict | list:
    request = urllib.request.Request(
        url,
        headers={"User-Agent": "KalimaVocabularyGenerator/1.0"},
    )
    for attempt in range(attempts):
        try:
            with urllib.request.urlopen(request, timeout=30) as response:
                return json.load(response)
        except (urllib.error.URLError, TimeoutError, json.JSONDecodeError):
            if attempt == attempts - 1:
                raise
            time.sleep(1.5 * (attempt + 1))
    raise RuntimeError("unreachable")


def download_morphology(destination: Path) -> None:
    destination.parent.mkdir(parents=True, exist_ok=True)
    request = urllib.request.Request(
        MORPHOLOGY_URL,
        headers={"User-Agent": "KalimaVocabularyGenerator/1.0"},
    )
    with urllib.request.urlopen(request, timeout=60) as response:
        destination.write_bytes(response.read())


def feature_value(features: str, key: str) -> str:
    match = re.search(rf"(?:^|\|){re.escape(key)}:([^|]+)", features)
    return match.group(1) if match else ""


def normalize_arabic(value: str) -> str:
    letter_map = str.maketrans({"ٱ": "ا", "أ": "ا", "إ": "ا", "آ": "ا", "ى": "ي"})
    return "".join(
        character
        for character in value.translate(letter_map).replace("ـ", "")
        if unicodedata.category(character) != "Mn" and not character.isspace()
    )


def grammar_for(segment: Segment) -> str:
    features = segment.features
    if "PN" in features.split("|"):
        return "nome próprio"
    if segment.pos == "V":
        return "verbo"
    if "ADJ" in features.split("|"):
        return "adjetivo"
    if segment.pos == "N":
        return "substantivo"
    if segment.pos == "PRON":
        return "pronome"
    if segment.pos in {"P", "CONJ", "NEG", "EMPH", "COND", "INTG"}:
        return "partícula"
    if segment.pos in {"DEM", "REL"}:
        return "pronome"
    return "forma corânica"


def choose_lexical_segment(segments: list[Segment]) -> Segment:
    stems = [
        segment
        for segment in segments
        if "PREF" not in segment.features and "SUFF" not in segment.features
    ]
    rooted = [segment for segment in stems if feature_value(segment.features, "ROOT")]
    return (rooted or stems or segments)[-1]


def parse_morphology(path: Path) -> list[MorphWord]:
    grouped: dict[tuple[int, int, int], list[Segment]] = collections.defaultdict(list)
    with path.open(encoding="utf-8") as source:
        for line in source:
            if not line.strip() or line.startswith("#"):
                continue
            location, surface, pos, features = line.rstrip("\n").split("\t", 3)
            surah, verse, word, _segment = map(int, location.split(":"))
            grouped[(surah, verse, word)].append(Segment(surface, pos, features))

    words: list[MorphWord] = []
    for (surah, verse, position), segments in sorted(grouped.items()):
        surface = "".join(segment.surface for segment in segments)
        lexical = choose_lexical_segment(segments)
        words.append(
            MorphWord(
                surah=surah,
                verse=verse,
                position=position,
                surface=surface,
                normalized=normalize_arabic(surface),
                lemma=feature_value(lexical.features, "LEM") or lexical.surface,
                root=feature_value(lexical.features, "ROOT"),
                grammar=grammar_for(lexical),
            )
        )
    return words


def clean_text(value: str | None) -> str:
    if not value:
        return ""
    without_tags = re.sub(r"<[^>]+>", "", value)
    return " ".join(html.unescape(without_tags).split())


def parse_verse_contexts(verse: dict) -> dict[tuple[int, int, int], ApiContext]:
    surah, verse_number = map(int, verse["verse_key"].split(":"))
    result: dict[tuple[int, int, int], ApiContext] = {}
    for word in verse.get("words", []):
        if word.get("char_type_name") != "word":
            continue
        result[(surah, verse_number, int(word["position"]))] = ApiContext(
            arabic=clean_text(word.get("text_uthmani") or word.get("text")),
            transliteration=clean_text((word.get("transliteration") or {}).get("text")),
            english_gloss=clean_text((word.get("translation") or {}).get("text")),
            verse_arabic=clean_text(verse.get("text_uthmani")),
        )
    return result


def chapter_contexts(surah: int) -> dict[tuple[int, int, int], ApiContext]:
    query = urllib.parse.urlencode(
        {
            "language": "en",
            "words": "true",
            "fields": "text_uthmani",
            "word_fields": "text_uthmani,transliteration",
            "per_page": 50,
        }
    )
    payload = request_json(f"{API_ROOT}/verses/by_chapter/{surah}?{query}")
    result: dict[tuple[int, int, int], ApiContext] = {}
    for verse in payload["verses"]:
        result.update(parse_verse_contexts(verse))
    return result


def verse_contexts(surah: int, verse: int) -> dict[tuple[int, int, int], ApiContext]:
    query = urllib.parse.urlencode(
        {
            "language": "en",
            "words": "true",
            "fields": "text_uthmani",
            "word_fields": "text_uthmani,transliteration",
        }
    )
    payload = request_json(f"{API_ROOT}/verses/by_key/{surah}:{verse}?{query}")
    return parse_verse_contexts(payload["verse"])


def translate_gloss(english: str) -> str:
    if english in KNOWN_TRANSLATIONS:
        return KNOWN_TRANSLATIONS[english]
    query = urllib.parse.urlencode(
        {"client": "gtx", "sl": "en", "tl": "pt", "dt": "t", "q": english}
    )
    payload = request_json(
        f"https://translate.googleapis.com/translate_a/single?{query}"
    )
    translated = "".join(part[0] for part in payload[0] if part and part[0])
    return clean_text(translated).replace("Alá", "Deus").replace("Allah", "Deus")


def load_translations(glosses: set[str], cache_path: Path) -> dict[str, str]:
    if cache_path.exists():
        cache = json.loads(cache_path.read_text(encoding="utf-8"))
    else:
        cache = {}
    cache.update(KNOWN_TRANSLATIONS)
    missing = sorted(gloss for gloss in glosses if gloss and gloss not in cache)
    for index, gloss in enumerate(missing, start=1):
        cache[gloss] = translate_gloss(gloss)
        print(f"Translated {index}/{len(missing)}: {gloss} -> {cache[gloss]}")
        if index % 10 == 0:
            cache_path.parent.mkdir(parents=True, exist_ok=True)
            cache_path.write_text(
                json.dumps(dict(sorted(cache.items())), ensure_ascii=False, indent=2) + "\n",
                encoding="utf-8",
            )
        time.sleep(0.04)
    cache_path.parent.mkdir(parents=True, exist_ok=True)
    cache_path.write_text(
        json.dumps(dict(sorted(cache.items())), ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    return cache


def spaced_root(root: str) -> str:
    return " ".join(root) if root else "—"


def build_output_words(
    morph_words: list[MorphWord],
    api_contexts: dict[tuple[int, int, int], ApiContext],
    translations: dict[str, str],
) -> list[OutputWord]:
    total_counts = collections.Counter(word.normalized for word in morph_words)
    first_global: dict[str, MorphWord] = {}
    for word in morph_words:
        first_global.setdefault(word.normalized, word)

    frequent_forms = [form for form, _count in total_counts.most_common(FREQUENT_LIMIT)]
    output: list[OutputWord] = []
    for form in frequent_forms:
        morph = first_global[form]
        context = api_contexts[morph.location]
        count = total_counts[form]
        meaning = translations.get(context.english_gloss, context.english_gloss)
        output.append(
            OutputWord(
                id=f"freq-{hashlib.sha1(form.encode('utf-8')).hexdigest()[:10]}",
                arabic=context.arabic or morph.surface,
                lemma=morph.lemma,
                transliteration=context.transliteration,
                meaning=meaning,
                root=spaced_root(morph.root),
                grammar=morph.grammar,
                category="Mais frequentes",
                reference=f"Alcorão {morph.surah}:{morph.verse}",
                verse_arabic=context.verse_arabic,
                verse_meaning=f"Neste contexto, o sentido de apoio é: {meaning}.",
                insight=(
                    f"Esta forma aparece {count} vezes no Alcorão. "
                    f"Lema registrado: {morph.lemma}."
                ),
                frequency=count,
                surah_number=None,
                is_frequent=True,
            )
        )

    for surah in LAST_SURAHS:
        surah_words = [word for word in morph_words if word.surah == surah]
        counts = collections.Counter(word.normalized for word in surah_words)
        first_in_surah: dict[str, MorphWord] = {}
        for word in surah_words:
            first_in_surah.setdefault(word.normalized, word)
        for form, morph in first_in_surah.items():
            context = api_contexts[morph.location]
            count = counts[form]
            meaning = translations.get(context.english_gloss, context.english_gloss)
            occurrence = "vez" if count == 1 else "vezes"
            output.append(
                OutputWord(
                    id=f"s{surah}-v{morph.verse:03d}-w{morph.position:03d}",
                    arabic=context.arabic or morph.surface,
                    lemma=morph.lemma,
                    transliteration=context.transliteration,
                    meaning=meaning,
                    root=spaced_root(morph.root),
                    grammar=morph.grammar,
                    category=f"Sura {surah}",
                    reference=f"{SURAH_NAMES[surah]} {surah}:{morph.verse}",
                    verse_arabic=context.verse_arabic,
                    verse_meaning=f"Neste contexto, o sentido de apoio é: {meaning}.",
                    insight=(
                        f"Aparece {count} {occurrence} nesta sura. "
                        f"Lema registrado: {morph.lemma}."
                    ),
                    frequency=count,
                    surah_number=surah,
                    is_frequent=False,
                )
            )
    return output


def kotlin_string(value: str) -> str:
    escaped = (
        value.replace("\\", "\\\\")
        .replace('"', '\\"')
        .replace("$", "\\$")
        .replace("\r", "")
        .replace("\n", "\\n")
    )
    return f'"{escaped}"'


def write_kotlin(words: list[OutputWord], destination: Path) -> None:
    lines = [
        "// Generated by tools/generate_quran_vocabulary.py. Do not edit manually.",
        "// Morphology: Quranic Arabic Corpus v0.4-compatible data (GPL).",
        "// Arabic context and English word glosses: Quran Foundation Content API v4.",
        "package com.kalima.quran.data",
        "",
        "internal object GeneratedQuranVocabulary {",
        "    val words: List<QuranWord> = listOf(",
    ]
    for word in words:
        fields = [
            kotlin_string(word.id),
            kotlin_string(word.arabic),
            kotlin_string(word.lemma),
            kotlin_string(word.transliteration),
            kotlin_string(word.meaning),
            kotlin_string(word.root),
            kotlin_string(word.grammar),
            kotlin_string(word.category),
            kotlin_string(word.reference),
            kotlin_string(word.verse_arabic),
            kotlin_string(word.verse_meaning),
            kotlin_string(word.insight),
            str(word.frequency),
            "null" if word.surah_number is None else str(word.surah_number),
            str(word.is_frequent).lower(),
        ]
        lines.append(f"        QuranWord({', '.join(fields)}),")
    lines.extend(["    )", "}", ""])
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text("\n".join(lines), encoding="utf-8")


def main() -> None:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    parser = argparse.ArgumentParser()
    parser.add_argument("--morphology", type=Path)
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("app/src/main/java/com/kalima/quran/data/GeneratedQuranVocabulary.kt"),
    )
    parser.add_argument(
        "--translation-cache",
        type=Path,
        default=Path("tools/pt_gloss_cache.json"),
    )
    args = parser.parse_args()

    morphology_path = args.morphology or Path("tools/cache/quran-morphology.txt")
    if not morphology_path.exists():
        print(f"Downloading morphology to {morphology_path}")
        download_morphology(morphology_path)

    print("Parsing morphology")
    morph_words = parse_morphology(morphology_path)
    by_location = {word.location: word for word in morph_words}
    total_counts = collections.Counter(word.normalized for word in morph_words)
    frequent_forms = [form for form, _ in total_counts.most_common(FREQUENT_LIMIT)]
    required_locations = {
        next(word.location for word in morph_words if word.normalized == form)
        for form in frequent_forms
    }
    required_locations.update(
        word.location for word in morph_words if word.surah in LAST_SURAHS
    )

    contexts: dict[tuple[int, int, int], ApiContext] = {}
    for surah in LAST_SURAHS:
        print(f"Fetching surah {surah}")
        contexts.update(chapter_contexts(surah))

    missing_verses = sorted(
        {(surah, verse) for surah, verse, word in required_locations if (surah, verse, word) not in contexts}
    )
    for index, (surah, verse) in enumerate(missing_verses, start=1):
        print(f"Fetching frequent-word context {index}/{len(missing_verses)}: {surah}:{verse}")
        contexts.update(verse_contexts(surah, verse))

    absent = sorted(required_locations - contexts.keys())
    if absent:
        samples = ", ".join(":".join(map(str, item)) for item in absent[:10])
        raise RuntimeError(f"Missing API contexts for {len(absent)} words: {samples}")

    glosses = {contexts[location].english_gloss for location in required_locations}
    translations = load_translations(glosses, args.translation_cache)
    words = build_output_words(morph_words, contexts, translations)
    write_kotlin(words, args.output)

    per_surah = collections.Counter(word.surah_number for word in words if word.surah_number)
    print(f"Generated {len(words)} cards: {FREQUENT_LIMIT} frequent + {len(words) - FREQUENT_LIMIT} surah cards")
    print("Per surah: " + ", ".join(f"{number}={per_surah[number]}" for number in LAST_SURAHS))


if __name__ == "__main__":
    main()
