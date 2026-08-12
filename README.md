# Kalima — árabe corânico em pequenos momentos

Kalima é um MVP Android inspirado na ideia de aprendizado frequente do WordBit, mas com identidade, interface e implementação próprias. O foco é vocabulário do árabe clássico encontrado no Alcorão, apresentado em português ou inglês e dentro de contexto.

## O que já funciona

- cartão diário com árabe, transliteração, significado, raiz e informação gramatical;
- pronúncia das palavras em árabe pelo mecanismo de voz do Android, disponível nos cartões, no vocabulário, nos quizzes e no estudo da tela bloqueada, com encaminhamento para instalar uma voz árabe quando necessário;
- seletor de idioma em **Progresso**, com interface, significados, quizzes, notificações e cartões de tela bloqueada em português ou inglês;
- trecho corânico contextualizado e paráfrase de estudo em português;
- repetição espaçada persistente por palavra: erros voltam após 10 minutos;
  acertos avançam para 1 dia, 3 dias e intervalos progressivamente maiores,
  ajustados pelas falhas de memória;
- fila de estudo que prioriza revisões vencidas, depois apresenta palavras
  novas e não antecipa cartões programados para o futuro;
- ações “De novo” e “Acertei” com prévia do próximo intervalo;
- meta diária e sequência de dias;
- biblioteca offline com busca em árabe, português, transliteração, raiz e referência;
- 100 formas entre as mais frequentes no Alcorão;
- todas as 42.001 formas de vocabulário únicas das 114 suras, preservando um cartão por forma e por sura;
- seleção pesquisável do estudo por uma ou várias suras, incluindo combinações como 2 + 36 + 114;
- modos “Todo o conteúdo”, “Mais usadas” e “Por sura”;
- limite máximo opcional e persistente para impedir que novos cartões ampliem
  o conjunto de aprendizado além do total escolhido;
- aba **Quiz** com sessões tranquilas de cinco perguntas e quatro alternativas;
- distribuição por sessão: duas perguntas árabe → português, uma português → árabe e duas com palavra destacada em uma ayah;
- feedback do quiz com a data relativa da próxima revisão e sessões sem
  repetição artificial quando há menos de cinco cartões pendentes;
- quiz opcional ao ligar a tela, com intervalo configurável entre 1 e 10 palavras;
- filtros de palavras novas, em revisão e aprendidas;
- persistência local, sem conta e sem coleta de dados;
- estudo automático a cada tela ligada, com uma palavra diferente em sequência;
- cartão em tela cheia sobre o bloqueio, fechado ao desbloquear, apagar a tela ou responder;
- serviço em primeiro plano opt-in e permissão explícita “Aparecer sobre outros apps”;
- lembrete diário opcional às 8h, exibido como notificação e compatível com a tela bloqueada conforme as configurações do Android;
- 42.117 cartões offline no total e testes de integridade do conteúdo.

## Tecnologia

- Kotlin 2.2
- Jetpack Compose + Material 3
- Android Gradle Plugin 8.13
- minSdk 26 e targetSdk 36
- SharedPreferences para o estado local do MVP
- AlarmManager para lembretes inexatos, sem permissão de alarme exato
- serviço em primeiro plano do tipo specialUse para escutar eventos de tela ligada
- TextToSpeech do Android com seleção automática de uma voz árabe instalada

## Executar

Abra a pasta no Android Studio e aguarde a sincronização. Pela linha de comando:

    .\gradlew.bat -g .gradle-cache testDebugUnitTest assembleDebug

O APK de desenvolvimento é gerado em:

    app/build/outputs/apk/debug/app-debug.apk

## Estudo na tela de bloqueio

O Android moderno restringe atividades abertas em segundo plano. O Kalima implementa esta função de forma opt-in:

1. o usuário concede “Aparecer sobre outros apps”;
2. um serviço em primeiro plano, visível por uma notificação persistente, escuta o evento de tela ligada;
3. uma atividade marcada para aparecer sobre o bloqueio apresenta o próximo cartão;
4. o cartão fecha sem alterar ou contornar a biometria, PIN ou senha do aparelho.

O app não usa Acessibilidade nem full-screen intent de alarme/chamada.

### Galaxy M23 5G com Android 14

Depois de instalar:

1. Abra Kalima e toque em **Ativar** no cartão “Estude sempre que ligar a tela”.
2. Na tela da Samsung, permita que Kalima apareça sobre outros aplicativos.
3. Volte ao app e confirme que “Estudo ao ligar a tela” está ativo.
4. Em **Configurações do app > Bateria**, selecione **Sem restrições** se a One UI encerrar o serviço.
5. Apague a tela pelo botão lateral e ligue-a novamente. Um novo cartão deve aparecer antes do desbloqueio.

Uma notificação persistente indica que a detecção está ativa. Se o usuário forçar a parada do aplicativo, será necessário abri-lo e ativar o serviço novamente.

## Escolher as suras

Na aba **Progresso**, abra **Escolher palavras**:

1. escolha **Por sura**;
2. toque em **Pesquisar e escolher suras**;
3. busque pelo número, nome transliterado ou nome árabe;
4. marque uma ou várias das suras 1–114;
5. volte à aba **Estudar**.

A seleção é persistida no aparelho e controla o estudo normal, a biblioteca, o lembrete diário e os cartões apresentados ao ligar a tela. Se a última sura for desmarcada, o aplicativo retorna automaticamente para **Todo o conteúdo**.

## Repetição espaçada e quiz

A aba **Quiz** usa o mesmo conteúdo selecionado em **Tudo**, **Mais usadas** ou **Por sura**. Cada sessão contém até cinco palavras novas ou com revisão pendente, sem digitação, cronômetro, vidas ou punições. Depois de cada resposta, o aplicativo mostra a alternativa correta, a transliteração, a raiz, a referência corânica e quando a palavra voltará.

Cada palavra possui um registro local independente de repetições, intervalo,
fator de facilidade, esquecimentos e próximo horário de revisão. **Acertei**
agenda inicialmente para o dia seguinte, depois para três dias, e então amplia
o intervalo pelo fator de facilidade. **De novo** reduz esse fator, reinicia a
graduação e agenda uma etapa de reaprendizado em dez minutos. Acertos antes do
horário previsto não ampliam artificialmente o intervalo.

Em **Progresso > Estudo ao ligar a tela**, o usuário pode ativar **Quiz ao ligar a tela** e escolher depois de quantas palavras — entre 1 e 10 — uma pergunta aparecerá. O valor inicial é três palavras.

## Conteúdo e precisão

O corpus embutido combina 16 cartões editoriais iniciais com um snapshot compactado de 42.101 cartões gerados. Antes de publicação:

1. um especialista em árabe corânico deve revisar raízes, lemas, morfologia e paráfrases;
2. o texto árabe deve ser importado sem alterações de uma fonte verificada;
3. cada fonte e tradução deve ter licença e atribuição documentadas;
4. significados devem permanecer vinculados ao contexto do versículo, evitando equivalências absolutas;
5. testes automatizados devem garantir referência, forma textual e procedência de cada registro.

Fontes recomendadas para a etapa de produção:

- [Tanzil Quran Text](https://tanzil.net/docs/download) para texto árabe Unicode verificado, observando seus termos;
- [Quranic Arabic Corpus](https://corpus.quran.com/download/) para morfologia e raízes, com atribuição e conformidade com a GPL;
- [Quran Foundation API](https://api-docs.quran.com/docs/sdk/javascript/verses/) para conteúdo servido por backend autenticado.

Veja também [CONTENT_REVIEW.md](CONTENT_REVIEW.md).

### Regenerar o snapshot

O gerador reproduz as frequências, as 114 suras, os metadados e o corpus offline compactado. Na primeira execução ele baixa a morfologia e cria um cache editorial de glossários em português:

    C:\caminho\para\python.exe tools\generate_full_quran_corpus.py

O arquivo `tools/pt_gloss_cache.json` deve ser revisado por um especialista antes de uma publicação. Consulte também [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

## Backups de versão

Cada versão concluída deve ser preservada com commit, tag Git, APK e arquivo ZIP do código-fonte. As mudanças de cada atualização ficam em [CHANGELOG.md](CHANGELOG.md). Os artefatos locais ficam em `releases/` e o procedimento completo está documentado em [RELEASE_PROCESS.md](RELEASE_PROCESS.md).

## Próximas etapas

- revisão acadêmica/religiosa do conteúdo inicial;
- revisão especializada e versionada dos glossários contextuais em português;
- repetição espaçada baseada em intervalos e histórico, em vez de apenas estados;
- áudio por palavra e por versículo com licença rastreável;
- widget de tela inicial e ações de resposta na notificação;
- testes instrumentados de RTL, acessibilidade, modo escuro e tamanhos de fonte;
- exportação/importação do progresso e sincronização opcional.
