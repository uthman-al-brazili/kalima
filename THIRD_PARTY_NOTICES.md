# Fontes e avisos de terceiros

Os snapshots em `quran_vocabulary.tsv.gz` e `quran_arabic.tsv.gz`, além dos
metadados em `GeneratedQuranSurahs.kt`, usam dados e serviços externos. Este
arquivo registra a procedência técnica; ele não substitui uma análise jurídica
antes da publicação do aplicativo.

## Quranic Arabic Corpus

- Projeto original: [Quranic Arabic Corpus](https://corpus.quran.com/)
- Download e termos: [Morphological Annotation](https://corpus.quran.com/download/)
- Snapshot processado pelo gerador: dados compatíveis com a versão 0.4 mantidos em [mustafa0x/quran-morphology](https://github.com/mustafa0x/quran-morphology)

A análise morfológica é distribuída sob a GNU GPL e requer atribuição à fonte. O gerador não altera o arquivo-fonte baixado; ele produz 42.101 cartões derivados com forma, lema, raiz, classe gramatical, frequência e localização. Confirme as obrigações da GPL para o modelo de distribuição pretendido antes de publicar.

## Quran Foundation Content API

- [Documentação da Content API v4](https://api-docs.quran.com/docs/category/content-apis/)
- [Referência dos campos](https://api-docs.quran.com/docs/api/field-reference/)

O gerador consulta a API para obter a forma Uthmani, a transliteração, o
contexto do versículo, o glossário palavra por palavra em inglês e os 6.236
ayahs em árabe usados na aba de leitura offline. O uso em produção deve
respeitar os termos e o fluxo de autenticação vigentes da Quran Foundation.

## Áudio palavra por palavra do Quran.com

- [Documentação de áudio da Quran Foundation](https://api-docs.quran.foundation/docs/sdk/javascript/audio/)
- CDN público: `https://audio.qurancdn.com/wbw/`

O aplicativo Android transmite, sem incorporar ao APK, o mesmo arquivo de áudio
palavra por palavra servido quando uma palavra é selecionada no Quran.com. O
usuário pode manter cópias locais desses arquivos para uso offline, individualmente
ou para o conteúdo de estudo selecionado. A voz sintetizada do Android não é usada
para palavras individuais. O uso em produção deve continuar respeitando os termos
vigentes da Quran Foundation e do Quran.com.

## Recitação de ayahs por Mahmoud Khalil Al-Hussary

- Catálogo: [EveryAyah](https://everyayah.com/)
- Coleção usada: `Husary_128kbps`
- Formato público: `https://everyayah.com/data/Husary_128kbps/SSSAAA.mp3`

O aplicativo Android transmite a recitação Murattal de Mahmoud Khalil
Al-Hussary, ayah por ayah, sem incorporar os arquivos ao APK. Cada gravação
ouvida pode ser mantida no aparelho, e as gravações dos ayahs do conteúdo de
estudo selecionado podem ser baixadas para uso offline. O uso em produção deve
continuar respeitando os termos vigentes do EveryAyah e dos titulares das
gravações.

## Voz do alfabeto árabe

- Serviço de geração: ElevenLabs
- Voz selecionada: Haytham
- Uso no Android: 28 clipes MP3 incorporados ao APK, um para o nome de cada
  letra do alfabeto árabe, sem transmissão pela internet e sem Google TTS.

Os clipes foram separados da gravação completa aprovada, sem regenerar ou
alterar as pronúncias. A distribuição deve respeitar os termos aplicáveis à
conta ElevenLabs usada para gerar a gravação.

## Significados em português

Os glossários em inglês foram convertidos em um primeiro rascunho em português e receberam correções editoriais pontuais. Eles são apresentados como apoio contextual, não como tradução oficial do Alcorão. O cache em `tools/pt_gloss_cache.json` e cada cartão devem passar por revisão linguística e religiosa antes da publicação.

## Java Native Access (JNA)

- Projeto: [Java Native Access](https://github.com/java-native-access/jna)
- Uso: detecção local de inatividade, janelas em tela cheia e inicialização com
  o Windows no aplicativo desktop.
- Licença: Apache License 2.0 ou LGPL 2.1 ou posterior, conforme permitido pelo
  projeto JNA.
