#!/usr/bin/env python3
"""Generate Kalima's compact offline vocabulary for all 114 Quran surahs."""

from __future__ import annotations

import argparse
import collections
import gzip
import io
import json
import sys
import time
import urllib.parse
from dataclasses import dataclass
from pathlib import Path

import generate_quran_vocabulary as legacy


ALL_SURAHS = range(1, 115)
FREQUENT_LIMIT = 100
FORMAT_HEADER = "#kalima-quran-v2"


@dataclass(frozen=True)
class CardRecord:
    morph: legacy.MorphWord
    frequency: int
    study_surah: int
    is_frequent: bool


def cached_json(url: str, destination: Path) -> dict | list:
    if destination.exists():
        return json.loads(destination.read_text(encoding="utf-8"))
    payload = legacy.request_json(url)
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(
        json.dumps(payload, ensure_ascii=False),
        encoding="utf-8",
    )
    return payload


def fetch_chapters(cache_dir: Path) -> list[dict]:
    payload = cached_json(
        f"{legacy.API_ROOT}/chapters?language=en",
        cache_dir / "chapters.json",
    )
    return payload["chapters"]


def fetch_required_contexts(
    required_locations: set[tuple[int, int, int]],
    cache_dir: Path,
) -> dict[tuple[int, int, int], legacy.ApiContext]:
    contexts: dict[tuple[int, int, int], legacy.ApiContext] = {}
    for surah in ALL_SURAHS:
        page = 1
        while True:
            query = urllib.parse.urlencode(
                {
                    "language": "en",
                    "words": "true",
                    "fields": "text_uthmani",
                    "word_fields": "text_uthmani,transliteration",
                    "per_page": 50,
                    "page": page,
                }
            )
            payload = cached_json(
                f"{legacy.API_ROOT}/verses/by_chapter/{surah}?{query}",
                cache_dir / f"chapter-{surah:03d}-page-{page:02d}.json",
            )
            for verse in payload["verses"]:
                parsed = legacy.parse_verse_contexts(verse)
                for location, context in parsed.items():
                    if location in required_locations:
                        contexts[location] = context
            pagination = payload.get("pagination") or {}
            total_pages = int(pagination.get("total_pages") or 1)
            print(f"Fetched surah {surah}/114, page {page}/{total_pages}")
            if page >= total_pages:
                break
            page += 1

    absent = sorted(required_locations - contexts.keys())
    if absent:
        sample = ", ".join(":".join(map(str, item)) for item in absent[:10])
        raise RuntimeError(f"Missing {len(absent)} API word contexts: {sample}")
    return contexts


def build_card_records(morph_words: list[legacy.MorphWord]) -> list[CardRecord]:
    total_counts = collections.Counter(word.normalized for word in morph_words)
    first_global: dict[str, legacy.MorphWord] = {}
    for word in morph_words:
        first_global.setdefault(word.normalized, word)

    records = [
        CardRecord(
            morph=first_global[form],
            frequency=count,
            study_surah=0,
            is_frequent=True,
        )
        for form, count in total_counts.most_common(FREQUENT_LIMIT)
    ]

    words_by_surah: dict[int, list[legacy.MorphWord]] = collections.defaultdict(list)
    for word in morph_words:
        words_by_surah[word.surah].append(word)

    for surah in ALL_SURAHS:
        surah_words = words_by_surah[surah]
        counts = collections.Counter(word.normalized for word in surah_words)
        first_in_surah: dict[str, legacy.MorphWord] = {}
        for word in surah_words:
            first_in_surah.setdefault(word.normalized, word)
        records.extend(
            CardRecord(
                morph=morph,
                frequency=counts[form],
                study_surah=surah,
                is_frequent=False,
            )
            for form, morph in first_in_surah.items()
        )
    return records


def translate_batch(glosses: list[str]) -> list[str]:
    query = urllib.parse.urlencode(
        {
            "client": "gtx",
            "sl": "en",
            "tl": "pt",
            "dt": "t",
            "q": "\n".join(glosses),
        }
    )
    payload = legacy.request_json(
        f"https://translate.googleapis.com/translate_a/single?{query}"
    )
    translated_text = "".join(part[0] for part in payload[0] if part and part[0])
    translations = [legacy.clean_text(value) for value in translated_text.splitlines()]
    if len(translations) == len(glosses):
        return [
            value.replace("Alá", "Deus").replace("Allah", "Deus")
            for value in translations
        ]
    if len(glosses) == 1:
        return [legacy.translate_gloss(glosses[0])]
    middle = len(glosses) // 2
    return translate_batch(glosses[:middle]) + translate_batch(glosses[middle:])


def write_translation_cache(cache: dict[str, str], destination: Path) -> None:
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(
        json.dumps(dict(sorted(cache.items())), ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )


def load_translations(
    glosses: set[str],
    cache_path: Path,
    batch_size: int,
) -> dict[str, str]:
    if cache_path.exists():
        cache = json.loads(cache_path.read_text(encoding="utf-8"))
    else:
        cache = {}
    cache.update(legacy.KNOWN_TRANSLATIONS)
    missing = sorted(gloss for gloss in glosses if gloss and gloss not in cache)
    batches = [missing[index : index + batch_size] for index in range(0, len(missing), batch_size)]
    for index, batch in enumerate(batches, start=1):
        translated = translate_batch(batch)
        cache.update(zip(batch, translated, strict=True))
        write_translation_cache(cache, cache_path)
        print(f"Translated batch {index}/{len(batches)} ({len(batch)} glosses)")
        time.sleep(0.05)
    write_translation_cache(cache, cache_path)
    return cache


def safe_field(value: str) -> str:
    return value.replace("\t", " ").replace("\r", " ").replace("\n", " ")


def card_id(record: CardRecord) -> str:
    morph = record.morph
    if record.is_frequent:
        import hashlib

        digest = hashlib.sha1(morph.normalized.encode("utf-8")).hexdigest()[:10]
        return f"freq-{digest}"
    return f"s{record.study_surah}-v{morph.verse:03d}-w{morph.position:03d}"


def write_corpus(
    records: list[CardRecord],
    contexts: dict[tuple[int, int, int], legacy.ApiContext],
    translations: dict[str, str],
    destination: Path,
) -> None:
    destination.parent.mkdir(parents=True, exist_ok=True)
    with destination.open("wb") as raw_file:
        with gzip.GzipFile(fileobj=raw_file, mode="wb", mtime=0) as compressed:
            with io.TextIOWrapper(compressed, encoding="utf-8", newline="\n") as output:
                output.write(FORMAT_HEADER + "\n")
                for record in records:
                    morph = record.morph
                    context = contexts[morph.location]
                    meaning = translations.get(context.english_gloss, context.english_gloss)
                    fields = [
                        card_id(record),
                        context.arabic or morph.surface,
                        morph.lemma,
                        context.transliteration,
                        meaning,
                        context.english_gloss,
                        legacy.spaced_root(morph.root),
                        morph.grammar,
                        str(morph.surah),
                        str(morph.verse),
                        str(record.frequency),
                        str(record.study_surah),
                        str(record.is_frequent).lower(),
                        context.verse_arabic,
                    ]
                    output.write("\t".join(safe_field(field) for field in fields) + "\n")


def kotlin_string(value: str) -> str:
    return legacy.kotlin_string(value)


def write_surah_metadata(chapters: list[dict], destination: Path) -> None:
    lines = [
        "// Generated by tools/generate_full_quran_corpus.py. Do not edit manually.",
        "package com.kalima.quran.data",
        "",
        "internal object GeneratedQuranSurahs {",
        "    val all: List<QuranSurah> = listOf(",
    ]
    for chapter in chapters:
        lines.append(
            "        QuranSurah(%d, %s, %s),"
            % (
                int(chapter["id"]),
                kotlin_string(legacy.clean_text(chapter["name_arabic"])),
                kotlin_string(legacy.clean_text(chapter["name_simple"])),
            )
        )
    lines.extend(["    )", "}", ""])
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text("\n".join(lines), encoding="utf-8")


def main() -> None:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    parser = argparse.ArgumentParser()
    parser.add_argument("--morphology", type=Path)
    parser.add_argument(
        "--corpus-output",
        type=Path,
        default=Path("app/src/main/assets/quran_vocabulary.tsv.gz"),
    )
    parser.add_argument(
        "--surahs-output",
        type=Path,
        default=Path("app/src/main/java/com/kalima/quran/data/GeneratedQuranSurahs.kt"),
    )
    parser.add_argument(
        "--translation-cache",
        type=Path,
        default=Path("tools/pt_gloss_cache.json"),
    )
    parser.add_argument(
        "--api-cache",
        type=Path,
        default=Path("tools/cache/quran-api"),
    )
    parser.add_argument("--translation-batch-size", type=int, default=30)
    args = parser.parse_args()

    morphology_path = args.morphology or Path("tools/cache/quran-morphology.txt")
    if not morphology_path.exists():
        legacy.download_morphology(morphology_path)

    print("Parsing Quranic Arabic Corpus morphology")
    morph_words = legacy.parse_morphology(morphology_path)
    records = build_card_records(morph_words)
    required_locations = {record.morph.location for record in records}
    print(f"Prepared {len(records)} cards from {len(morph_words)} word occurrences")

    chapters = fetch_chapters(args.api_cache)
    contexts = fetch_required_contexts(required_locations, args.api_cache)
    glosses = {contexts[location].english_gloss for location in required_locations}
    translations = load_translations(
        glosses,
        args.translation_cache,
        args.translation_batch_size,
    )
    write_corpus(records, contexts, translations, args.corpus_output)
    write_surah_metadata(chapters, args.surahs_output)

    print(f"Generated {len(records)} corpus cards")
    print(f"Corpus asset size: {args.corpus_output.stat().st_size} bytes")
    print(f"Portuguese glosses in cache: {len(translations)}")


if __name__ == "__main__":
    main()
