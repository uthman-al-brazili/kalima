# Processo de backup e lançamento

Cada versão Android concluída do Kalima deve possuir três formas de restauração:

- um commit Git contendo todo o código-fonte;
- uma tag anotada no formato `vX.Y.Z`;
- artefatos locais em `releases/`: ZIP do código e checksums SHA-256.

Os APKs de debug preservados para versões antigas permanecem intactos, mas não
é mais necessário compilar ou copiar um APK de debug separado como backup de
uma nova versão.

A versão Windows está congelada. Não alterar, versionar, testar, empacotar ou
publicar o aplicativo Windows, salvo quando o usuário pedir isso explicitamente.

## Procedimento

Criar commits ou tags e enviar commits ou tags ao remoto sempre exige
autorização explícita do usuário no pedido atual. Um pedido para preparar,
empacotar, lançar ou publicar o aplicativo não concede por si só permissão para
ações Git. Sem essa autorização, preparar e validar tudo localmente e parar
antes de qualquer commit, tag ou push.

1. Atualizar `versionCode` e `versionName` em `app/build.gradle.kts`.
2. Adicionar no topo de `CHANGELOG.md` uma seção datada para a nova versão,
   resumindo as funcionalidades e correções visíveis para o usuário.
3. Atualizar em `website/src/App.tsx` os links versionados da release e do APK,
   além dos textos de download em inglês e português, incluindo essa alteração
   do site no commit da release. Não alterar artes promocionais, capturas de tela
   ou o manifesto de artes sem um pedido explícito do usuário.
4. Em uma única invocação Gradle, quando possível, executar os testes unitários,
   o lint, `verifyLockScreenRegression` e a montagem do artefato distribuível
   solicitado, seguindo o fluxo de assinatura correspondente. Não montar um APK
   de debug separado apenas para backup.
5. Somente com autorização Git explícita do usuário, criar um commit e a tag
   anotada `vX.Y.Z`.
6. Gerar o código-fonte restaurável a partir da tag:

       git archive --format=zip --output=releases/kalima-X.Y.Z-source.zip vX.Y.Z

7. Calcular o SHA-256 do ZIP e de qualquer artefato distribuível retido para a
   versão e registrá-los em `releases/SHA256SUMS.txt`.
8. Publicar a release e seus artefatos no GitHub e validar localmente a
   compilação do site. Nunca implantar `website/` nem criar uma prévia pública
   sem autorização explícita do usuário para essa publicação no pedido atual.
   Um pedido geral para publicar uma release do app não autoriza a implantação
   do site.

Os artefatos gerados e checksums são ignorados pelo Git para evitar aumentar o
repositório, mas permanecem na pasta local compartilhada. O arquivo
`releases/README.md` registra as versões preservadas.

## Restaurar uma versão

Para apenas consultar o código:

    git switch --detach vX.Y.Z

Para iniciar uma correção baseada naquela versão:

    git switch -c codex/correcao-X.Y.Z vX.Y.Z

Também é possível extrair o ZIP correspondente sem alterar o repositório atual.
