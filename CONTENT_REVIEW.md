# Protocolo editorial do corpus

O objetivo deste arquivo é impedir que conteúdo de demonstração seja confundido com uma edição acadêmica ou religiosa pronta para publicação.

## Campos obrigatórios por palavra

Cada registro de produção deve ter:

- forma exatamente como aparece no versículo;
- lema;
- raiz;
- segmentação morfológica;
- classe gramatical e flexão;
- transliteração padronizada;
- significado curto específico ao contexto;
- referência de surata e versículo;
- identificador e versão da fonte;
- revisor e data da revisão.

## Fluxo de aprovação

1. Importar texto árabe verificado sem normalizá-lo silenciosamente.
2. Importar ou associar análise morfológica rastreável.
3. Produzir a explicação em português como material de estudo, identificada como tradução aproximada quando aplicável.
4. Fazer revisão linguística por alguém qualificado em árabe clássico.
5. Fazer revisão religiosa/editorial independente.
6. Bloquear a publicação quando fonte, licença ou revisão estiver ausente.

O snapshot atual cobre as 114 suras e contém 21 mil glossários contextuais em português produzidos como rascunho assistido. Cobertura técnica não significa revisão editorial: cada glossário continua pendente de validação especializada.

## Regras de interface

- Nunca sugerir que uma palavra árabe possui apenas uma tradução possível.
- Mostrar o contexto e a referência próximos ao significado.
- Diferenciar forma no versículo, lema e raiz.
- Manter árabe com direção RTL e português com direção LTR.
- Não alterar o texto corânico para simplificar busca ou exibição; a busca pode usar uma cópia normalizada apenas no índice.
- Permitir que o usuário reporte um problema em um cartão específico.

O aplicativo aplica essas duas últimas regras diretamente: cada cartão
expandido mostra o estado **rascunho editorial — validação especializada
pendente** e oferece **Compartilhar correção**, preenchendo identificador,
referência e forma árabe. Esse relatório pode ser devolvido junto da planilha
de revisão do lote de 100 palavras sem alterar o texto corânico no aparelho.

## Porta de publicação

A presença do corpus no APK não constitui aprovação. Promoção pública que faça
afirmações de autoridade editorial permanece bloqueada até que o lote-alvo
tenha, para cada registro, decisão do revisor, fonte verificável, nome ou ID do
revisor, data e resolução de todas as divergências. Áudio de recitação também
permanece fora do APK até existir arquivo, licença e atribuição rastreáveis; a
voz atual do Android é rotulada no app como áudio gerado pelo aparelho.

## Licenças

Textos, traduções, análises morfológicas e áudios podem ter licenças diferentes. O produto deve manter um inventário de procedência por artefato e exibir os créditos exigidos. O fato de o texto ser religioso não torna automaticamente livre uma edição digital, tradução, recitação ou anotação.
