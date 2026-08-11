# Fontes e avisos de terceiros

O snapshot de vocabulário em `quran_vocabulary.tsv.gz` e os metadados em `GeneratedQuranSurahs.kt` usam dados e serviços externos. Este arquivo registra a procedência técnica; ele não substitui uma análise jurídica antes da publicação do aplicativo.

## Quranic Arabic Corpus

- Projeto original: [Quranic Arabic Corpus](https://corpus.quran.com/)
- Download e termos: [Morphological Annotation](https://corpus.quran.com/download/)
- Snapshot processado pelo gerador: dados compatíveis com a versão 0.4 mantidos em [mustafa0x/quran-morphology](https://github.com/mustafa0x/quran-morphology)

A análise morfológica é distribuída sob a GNU GPL e requer atribuição à fonte. O gerador não altera o arquivo-fonte baixado; ele produz 42.101 cartões derivados com forma, lema, raiz, classe gramatical, frequência e localização. Confirme as obrigações da GPL para o modelo de distribuição pretendido antes de publicar.

## Quran Foundation Content API

- [Documentação da Content API v4](https://api-docs.quran.com/docs/category/content-apis/)
- [Referência dos campos](https://api-docs.quran.com/docs/api/field-reference/)

O gerador consulta a API para obter a forma Uthmani, a transliteração, o contexto do versículo e o glossário palavra por palavra em inglês. O uso em produção deve respeitar os termos e o fluxo de autenticação vigentes da Quran Foundation.

## Significados em português

Os glossários em inglês foram convertidos em um primeiro rascunho em português e receberam correções editoriais pontuais. Eles são apresentados como apoio contextual, não como tradução oficial do Alcorão. O cache em `tools/pt_gloss_cache.json` e cada cartão devem passar por revisão linguística e religiosa antes da publicação.
