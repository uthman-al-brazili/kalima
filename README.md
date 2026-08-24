# Kalima — árabe corânico na tela bloqueada e em pequenos momentos

Kalima é um aplicativo Android de árabe corânico centrado em uma forma própria de
estudo: cartões curtos e quizzes opcionais podem aparecer sobre a tela ainda
bloqueada quando o display acende. Assim, o retorno ao celular vira uma pequena
oportunidade de aprender antes mesmo do desbloqueio, sem contornar o PIN, a senha
ou a biometria do Android. O mesmo corpus e progresso também estão disponíveis em
um aplicativo Windows separado.

## O que já funciona

- cartões opcionais de palavra e quiz sobre a tela ainda bloqueada quando o
  display acende, com horário silencioso, limite diário e pausas;
- proteção integral da tela de bloqueio: o Kalima nunca desbloqueia o aparelho
  nem contorna PIN, senha ou biometria;
- widget Android **Palavra diária do Alcorão**, com árabe, transliteração,
  significado contextual, referência, próxima palavra e abertura da lição exata;
- cartão de recordação que apresenta imediatamente o significado de palavras
  novas e o oculta primeiro nas revisões para testar a memória;
- gravação humana palavra por palavra do Quran.com, salva após a primeira
  reprodução e disponível por download para o conteúdo selecionado, sem usar
  voz sintetizada nas palavras individuais;
- recitação Murattal de Mahmoud Khalil Al-Hussary para cada ayah, transmitida
  sob demanda, salva após a primeira reprodução e incluída no download do
  conteúdo selecionado, sem usar voz sintetizada para o Alcorão;
- seletor de idioma em **Configurações**, com interface, significados, quizzes, notificações e cartões de tela bloqueada em português ou inglês;
- trecho corânico contextualizado e paráfrase de estudo em português;
- ayah completo oculto por padrão nos cartões, com uma escolha persistente para
  mostrar ou ocultar o texto em cartões seguintes;
- aba **Alcorão** com as 114 suras e 6.236 ayahs em árabe para leitura offline,
  seleção pesquisável e navegação entre suras;
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
- configuração inicial que pergunta sobre o alfabeto e os números árabes, cria
  um plano de fundamentos quando necessário e só libera palavras completas
  depois das letras, ligações e vogais breves;
- curso dos algarismos árabes de ٠ a ٩ em paralelo às lições de letras ou
  vocabulário;
- escolha guiada para 100 essenciais, oração, suras curtas ou corpus completo;
- caminhos persistentes Primeiras 50, Top 100, Top 300, Top 500, oração, suras 101–114,
  todo o corpus e seleção por sura;
- favoritos e lista de estudo personalizada, disponíveis como coleções independentes;
- limite máximo opcional e persistente para impedir que novos cartões ampliem
  o conjunto de aprendizado além do total escolhido;
- aba **Quiz** com sessões tranquilas de até cinco perguntas e quatro alternativas;
- modos misto, reconhecimento por escuta, palavra ausente no versículo, família
  de raiz e somente revisões vencidas;
- feedback do quiz com a data relativa da próxima revisão e sessões sem
  repetição artificial quando há menos de cinco cartões pendentes;
- filtros de palavras novas, em revisão, aprendidas e favoritas;
- painel de progresso com precisão em 7/30 dias, novas e revisadas no dia,
  calendário de atividade, palavras difíceis e domínio por raiz;
- persistência local, sem conta e sem coleta de dados;
- serviço em primeiro plano opt-in e permissão explícita “Aparecer sobre outros apps”;
- lembrete diário opcional às 8h, exibido como notificação e compatível com a tela bloqueada conforme as configurações do Android;
- status editorial visível por cartão e relatório de correção compartilhável,
  mantendo explícito que a validação especializada ainda está pendente;
- navegação por ícones reconhecíveis, árabe em RTL com localidade informada a
  leitores de tela e tipografia escalável;
- 42.117 cartões offline no total e testes de integridade do conteúdo.
- aplicativo nativo para Windows com as mesmas palavras, IDs, repetição
  espaçada, quiz, biblioteca, progresso, caminhos e configurações essenciais;
- cartões opcionais de boas-vindas no Windows ao voltar depois de um período
  ausente, com bandeja do sistema, início automático, horário silencioso,
  limite diário, quiz ocasional e adiamento durante aplicativos em tela cheia;
- instalador Windows autocontido, sem necessidade de instalar Java, com dados
  persistidos localmente em `%APPDATA%\Kalima` e lembrete enquanto o app está aberto.

## Tecnologia

- Kotlin 2.2
- Jetpack Compose + Material 3
- Compose Multiplatform 1.11 para a interface nativa do Windows
- Android Gradle Plugin 8.13
- minSdk 26 e targetSdk 36
- SharedPreferences para o estado local do MVP
- AlarmManager para lembretes inexatos, sem permissão de alarme exato
- serviço em primeiro plano do tipo specialUse para escutar eventos de tela ligada
- TextToSpeech do Android com seleção automática de uma voz árabe instalada

## Executar no Android

Abra a pasta no Android Studio e aguarde a sincronização. Pela linha de comando:

    .\gradlew.bat -g .gradle-cache testDebugUnitTest assembleDebug

O APK de desenvolvimento é gerado em:

    app/build/outputs/apk/debug/app-debug.apk

## Executar no Windows

Para iniciar pelo código-fonte:

    .\gradlew.bat -g .gradle-cache :desktop:run

Para gerar o instalador autocontido:

    .\gradlew.bat -g .gradle-cache :desktop:packageExe

O instalador é criado em `desktop/build/compose/binaries/main/exe/`. Ele inclui
o runtime necessário e não exige uma instalação separada do Java. O aplicativo
salva o progresso em `%APPDATA%\Kalima\progress.properties`.

O estudo, quiz, biblioteca, progresso, idiomas, temas, voz do dispositivo,
lembretes locais e cartões ao voltar ao computador estão disponíveis no Windows.
Os cartões aparecem somente depois da entrada no Windows e nunca cobrem a tela
segura de autenticação. A pronúncia requer uma voz
árabe instalada em **Configurações > Hora e idioma > Fala**; quando ela não
está disponível, o próprio Kalima oferece um atalho para essa tela.

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

A aba **Quiz** usa o caminho ativo, inclusive favoritos e a lista personalizada.
Cada sessão contém até cinco palavras novas ou com revisão pendente, sem
cronômetro, vidas ou punições. É possível concentrar a sessão em escuta,
lacunas no versículo, raízes ou apenas cartões vencidos. Depois de cada
resposta, o aplicativo mostra a alternativa correta, a transliteração, a raiz,
a referência corânica e quando a palavra voltará.

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
2. o texto árabe deve ser verificado contra uma fonte publicada e auditável;
3. cada fonte e tradução deve ter licença e atribuição documentadas;
4. significados devem permanecer vinculados ao contexto do versículo, evitando equivalências absolutas;
5. testes automatizados devem garantir referência, forma textual e procedência de cada registro;
6. o status visível no app deve permanecer como **rascunho editorial** até que
   os campos de fonte, revisor e data sejam devolvidos e auditados.

Fontes recomendadas para a etapa de produção:

- [Tanzil Quran Text](https://tanzil.net/docs/download) para texto árabe Unicode verificado, observando seus termos;
- [Quranic Arabic Corpus](https://corpus.quran.com/download/) para morfologia e raízes, com atribuição e conformidade com a GPL;
- [Quran Foundation API](https://api-docs.quran.com/docs/sdk/javascript/verses/) para conteúdo servido por backend autenticado.

Veja também [CONTENT_REVIEW.md](CONTENT_REVIEW.md).

### Regenerar o snapshot

O gerador reproduz as frequências, as 114 suras, os metadados, o corpus de
vocabulário e o texto árabe completo para leitura offline. Na primeira execução
ele baixa a morfologia e cria um cache editorial de glossários em português:

    C:\caminho\para\python.exe tools\generate_full_quran_corpus.py

O arquivo `tools/pt_gloss_cache.json` deve ser revisado por um especialista antes de uma publicação. Consulte também [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

## Backups de versão

Cada versão concluída deve ser preservada com commit, tag Git, APK e arquivo ZIP do código-fonte. As mudanças de cada atualização ficam em [CHANGELOG.md](CHANGELOG.md). Os artefatos locais ficam em `releases/` e o procedimento completo está documentado em [RELEASE_PROCESS.md](RELEASE_PROCESS.md).

## Próximas etapas

- revisão acadêmica/religiosa do conteúdo inicial;
- revisão especializada e versionada dos glossários contextuais em português;
- áudio por palavra e por versículo com licença rastreável;
- importação versionada das decisões do primeiro lote de revisão editorial;
- widget de tela inicial e ações de resposta na notificação;
- testes instrumentados de RTL, acessibilidade, modo escuro e tamanhos de fonte;
- exportação/importação do progresso e sincronização opcional.
