# Processo de backup e lançamento

Cada versão Android concluída do Kalima deve possuir três formas de restauração:

- um commit Git contendo todo o código-fonte;
- uma tag anotada no formato `vX.Y.Z`;
- artefatos locais em `releases/`: APK, ZIP do código e checksums SHA-256.

A versão Windows está congelada. Não alterar, versionar, testar, empacotar ou
publicar o aplicativo Windows, salvo quando o usuário pedir isso explicitamente.

## Procedimento

1. Atualizar `versionCode` e `versionName` em `app/build.gradle.kts`.
2. Adicionar no topo de `CHANGELOG.md` uma seção datada para a nova versão,
   resumindo as funcionalidades e correções visíveis para o usuário.
3. Executar os testes e builds Android:

       .\gradlew.bat -g .gradle-cache testDebugUnitTest lintDebug assembleDebug

4. Copiar o APK validado sem substituir arquivos existentes:

       releases/kalima-X.Y.Z-debug.apk

5. Criar um commit e a tag anotada `vX.Y.Z`.
6. Gerar o código-fonte restaurável a partir da tag:

       git archive --format=zip --output=releases/kalima-X.Y.Z-source.zip vX.Y.Z

7. Calcular SHA-256 do APK e do ZIP e registrá-los em `releases/SHA256SUMS.txt`.

Os binários e checksums são ignorados pelo Git para evitar aumentar o repositório, mas permanecem na pasta local compartilhada. O arquivo `releases/README.md` registra as versões preservadas.

## Restaurar uma versão

Para apenas consultar o código:

    git switch --detach vX.Y.Z

Para iniciar uma correção baseada naquela versão:

    git switch -c codex/correcao-X.Y.Z vX.Y.Z

Também é possível extrair o ZIP correspondente sem alterar o repositório atual.
