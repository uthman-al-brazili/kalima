[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"
$scriptDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
$repositoryRoot = (Resolve-Path (Join-Path $scriptDirectory "..\..")).Path
$gradleFile = Join-Path $repositoryRoot "app\build.gradle.kts"
$priorApk = Join-Path $scriptDirectory "kalima-0.28.4-release.apk"
$statusPath = Join-Path $env:TEMP "kalima-signing-status.txt"

trap {
    $failureMessage = $_.Exception.Message
    [IO.File]::WriteAllText($statusPath, "FAILED`r`n$failureMessage")
    Write-Host ""
    Write-Host "Signing did not finish: $failureMessage" -ForegroundColor Red
    Read-Host "Press Enter to close"
    break
}

[IO.File]::WriteAllText($statusPath, "WAITING_FOR_INPUT")

function ConvertFrom-SecureStringInMemory {
    param([Parameter(Mandatory)][Security.SecureString]$SecureValue)

    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecureValue)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
}

function Read-RequiredSecret {
    param([Parameter(Mandatory)][string]$Prompt)

    while ($true) {
        $secureValue = Read-Host $Prompt -AsSecureString
        $plainValue = ConvertFrom-SecureStringInMemory $secureValue
        if (-not [string]::IsNullOrEmpty($plainValue)) {
            return $plainValue
        }
        Write-Warning "The value cannot be empty."
    }
}

function Get-CertificateDigest {
    param([Parameter(Mandatory)][string]$ApkSignerOutput)

    $match = [regex]::Match(
        $ApkSignerOutput,
        "Signer #1 certificate SHA-256 digest:\s*([0-9a-fA-F]+)"
    )
    if (-not $match.Success) {
        throw "Could not read the APK signing certificate digest."
    }
    return $match.Groups[1].Value.ToLowerInvariant()
}

if (-not (Test-Path -LiteralPath $gradleFile -PathType Leaf)) {
    throw "Run this script from the Kalima repository."
}

$gradleText = Get-Content -Raw -LiteralPath $gradleFile
$versionMatch = [regex]::Match($gradleText, 'versionName\s*=\s*"([^"]+)"')
$versionCodeMatch = [regex]::Match($gradleText, 'versionCode\s*=\s*(\d+)')
if (-not $versionMatch.Success -or -not $versionCodeMatch.Success) {
    throw "Could not determine the current Android version."
}
$versionName = $versionMatch.Groups[1].Value
$versionCode = $versionCodeMatch.Groups[1].Value

Write-Host ""
Write-Host "Kalima $versionName beta signing" -ForegroundColor Cyan
Write-Host "Passwords stay inside this PowerShell process and are cleared after the build."
Write-Host "The keystore must be kept permanently for every future update."
Write-Host ""

$knownKeystore = Join-Path $env:USERPROFILE "Documents\KalimaSigning\kalima-release.jks"
if (Test-Path -LiteralPath $knownKeystore -PathType Leaf) {
    $enteredPath = Read-Host "Original keystore path (press Enter to use $knownKeystore)"
    if ([string]::IsNullOrWhiteSpace($enteredPath)) {
        $enteredPath = $knownKeystore
    }
}
else {
    $enteredPath = Read-Host "Original keystore path, or press Enter to create a new permanent key"
}
$enteredPath = $enteredPath.Trim().Trim([char]34).Trim([char]39).Trim([char]96)
$createdNewKey = [string]::IsNullOrWhiteSpace($enteredPath)

if ($createdNewKey) {
    $signingDirectory = Join-Path $env:USERPROFILE "KalimaSigning"
    $keystorePath = Join-Path $signingDirectory "kalima-release.jks"

    Write-Warning "A new key will not match the certificate on the unpublished 0.28.4 Uptodown draft."
    Write-Host "We will replace that draft with the newly signed $versionName APK before review."
    $confirmation = Read-Host "Type CREATE to create the permanent key at $keystorePath"
    if ($confirmation -cne "CREATE") {
        throw "Signing cancelled."
    }
    if (Test-Path -LiteralPath $keystorePath) {
        throw "The destination already exists. Restart and enter it as the original keystore path."
    }

    New-Item -ItemType Directory -Path $signingDirectory -Force | Out-Null
    $keytool = if (-not [string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
        Join-Path $env:JAVA_HOME "bin\keytool.exe"
    }
    else {
        $null
    }
    if ([string]::IsNullOrWhiteSpace($keytool) -or -not (Test-Path -LiteralPath $keytool -PathType Leaf)) {
        $keytool = (Get-Command keytool.exe -ErrorAction Stop).Source
    }

    Write-Host ""
    Write-Host "Create a strong password and save it in your password manager." -ForegroundColor Yellow
    Write-Host "When keytool asks for the key password, press Enter to reuse the keystore password."
    Write-Host ""
    & $keytool `
        -genkeypair `
        -alias kalima `
        -keyalg RSA `
        -keysize 2048 `
        -sigalg SHA256withRSA `
        -validity 10000 `
        -storetype JKS `
        -keystore $keystorePath `
        -dname "CN=Gustavo Duarte, OU=Kalima, O=Kalima, L=Vicosa, ST=Minas Gerais, C=BR"
    if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $keystorePath -PathType Leaf)) {
        throw "Key creation failed."
    }

    Write-Host ""
    Write-Host "Permanent key created at: $keystorePath" -ForegroundColor Green
    Write-Host "Before publishing, make two encrypted backups outside this computer." -ForegroundColor Yellow
}
else {
    $resolvedInput = Get-Item -LiteralPath $enteredPath -ErrorAction Stop
    if ($resolvedInput.PSIsContainer) {
        $keystoreCandidates = Get-ChildItem -LiteralPath $resolvedInput.FullName -File |
            Where-Object {
                $_.Extension -in @(".jks", ".keystore") -and
                $_.Name -notmatch "DO-NOT-USE"
            }
        if ($keystoreCandidates.Count -ne 1) {
            throw "Enter the exact .jks file path; the selected folder does not contain exactly one usable keystore."
        }
        $keystorePath = $keystoreCandidates[0].FullName
        Write-Host "Using keystore: $keystorePath"
    }
    else {
        $keystorePath = $resolvedInput.FullName
    }
    if ($keystorePath -match "DO-NOT-USE") {
        throw "This keystore is marked DO-NOT-USE. Select the active kalima-release.jks file."
    }
}

$aliasInput = Read-Host "Key alias (press Enter for kalima)"
$keyAlias = if ([string]::IsNullOrWhiteSpace($aliasInput)) { "kalima" } else { $aliasInput.Trim() }
$storePassword = Read-RequiredSecret "Keystore password"
$keyPasswordSecure = Read-Host "Key password (press Enter if it is the same)" -AsSecureString
$keyPassword = ConvertFrom-SecureStringInMemory $keyPasswordSecure
if ([string]::IsNullOrEmpty($keyPassword)) {
    $keyPassword = $storePassword
}

$environmentNames = @(
    "KALIMA_KEYSTORE_FILE",
    "KALIMA_KEYSTORE_PASSWORD",
    "KALIMA_KEY_ALIAS",
    "KALIMA_KEY_PASSWORD",
    "ANDROID_HOME",
    "ANDROID_SDK_ROOT"
)

$sdkCandidates = @(
    $env:ANDROID_HOME,
    $env:ANDROID_SDK_ROOT,
    (Join-Path $env:LOCALAPPDATA "Android\Sdk"),
    (Join-Path $env:USERPROFILE "android-sdk")
) | Where-Object {
    -not [string]::IsNullOrWhiteSpace($_) -and
    (Test-Path -LiteralPath (Join-Path $_ "build-tools") -PathType Container)
}
$sdkRoot = $sdkCandidates | Select-Object -First 1
if ([string]::IsNullOrWhiteSpace($sdkRoot)) {
    throw "An Android SDK with build-tools was not found."
}

try {
    [Environment]::SetEnvironmentVariable("KALIMA_KEYSTORE_FILE", $keystorePath, "Process")
    [Environment]::SetEnvironmentVariable("KALIMA_KEYSTORE_PASSWORD", $storePassword, "Process")
    [Environment]::SetEnvironmentVariable("KALIMA_KEY_ALIAS", $keyAlias, "Process")
    [Environment]::SetEnvironmentVariable("KALIMA_KEY_PASSWORD", $keyPassword, "Process")
    [Environment]::SetEnvironmentVariable("ANDROID_USER_HOME", (Join-Path $repositoryRoot ".android-home"), "Process")
    [Environment]::SetEnvironmentVariable("ANDROID_HOME", $sdkRoot, "Process")
    [Environment]::SetEnvironmentVariable("ANDROID_SDK_ROOT", $sdkRoot, "Process")

    $gradleUserHome = Join-Path $repositoryRoot ".gradle-cache"
    $gradleVersionCaches = Join-Path $gradleUserHome "caches"
    Push-Location $repositoryRoot
    try {
        & "cmd.exe" "/d" "/c" "call" ".\gradlew.bat" "-g" ".gradle-cache" "--stop" | Out-Host
        if ($LASTEXITCODE -ne 0) {
            Write-Warning "Gradle daemon shutdown reported an error; continuing with cache cleanup."
        }
    }
    finally {
        Pop-Location
    }
    if (Test-Path -LiteralPath $gradleVersionCaches -PathType Container) {
        Get-ChildItem -LiteralPath $gradleVersionCaches -Directory | ForEach-Object {
            $dependencyAccessors = Join-Path $_.FullName "dependencies-accessors"
            if (Test-Path -LiteralPath $dependencyAccessors -PathType Container) {
                Remove-Item -LiteralPath $dependencyAccessors -Recurse -Force
            }
        }
    }

    $signedApk = Join-Path $repositoryRoot "app\build\outputs\apk\release\app-release.apk"
    if (Test-Path -LiteralPath $signedApk -PathType Leaf) {
        Remove-Item -LiteralPath $signedApk -Force
    }

    Write-Host ""
    Write-Host "Running tests, lint, lock-screen verification, and the signed release build..." -ForegroundColor Cyan
    $releaseBuildLog = Join-Path $env:TEMP "kalima-release-build-$versionName.log"
    Push-Location $repositoryRoot
    try {
        & "cmd.exe" "/d" "/c" "call" ".\gradlew.bat" `
            "-Pkotlin.compiler.execution.strategy=in-process" `
            "-g" ".gradle-cache" `
            "clean" `
            ":app:testDebugUnitTest" `
            ":app:lint" `
            ":app:verifyLockScreenRegression" `
            ":app:assembleRelease" 2>&1 | Tee-Object -FilePath $releaseBuildLog
        $gradleExitCode = $LASTEXITCODE
        if ($gradleExitCode -ne 0) {
            throw (
                "Gradle validation or release assembly failed with exit code $gradleExitCode. " +
                "See $releaseBuildLog."
            )
        }
    }
    finally {
        Pop-Location
    }

    if (-not (Test-Path -LiteralPath $signedApk -PathType Leaf)) {
        throw "The signed APK was not created."
    }

    $apkSigner = Get-ChildItem -LiteralPath (Join-Path $sdkRoot "build-tools") `
        -Recurse -Filter "apksigner.bat" -File -ErrorAction Stop |
        Sort-Object { [version]$_.Directory.Name } -Descending |
        Select-Object -First 1
    if ($null -eq $apkSigner) {
        throw "Android apksigner was not found."
    }
    $aapt = Join-Path $apkSigner.Directory.FullName "aapt.exe"
    if (-not (Test-Path -LiteralPath $aapt -PathType Leaf)) {
        throw "Android aapt was not found."
    }

    $badging = (& $aapt dump badging $signedApk 2>&1 | Out-String)
    if ($LASTEXITCODE -ne 0) {
        throw "Could not inspect the APK package metadata.`n$badging"
    }
    $packageMatch = [regex]::Match(
        $badging,
        "package: name='([^']+)' versionCode='([^']+)' versionName='([^']+)'"
    )
    if (-not $packageMatch.Success) {
        throw "Could not read the APK package name and version."
    }
    if ($packageMatch.Groups[1].Value -ne "com.kalima.quran" -or
        $packageMatch.Groups[2].Value -ne $versionCode -or
        $packageMatch.Groups[3].Value -ne $versionName) {
        throw (
            "Stale or incorrect APK: expected com.kalima.quran " +
            "$versionName ($versionCode), found " +
            "$($packageMatch.Groups[1].Value) " +
            "$($packageMatch.Groups[3].Value) ($($packageMatch.Groups[2].Value))."
        )
    }

    $newVerification = (& $apkSigner.FullName verify --verbose --print-certs $signedApk 2>&1 | Out-String)
    if ($LASTEXITCODE -ne 0) {
        throw "APK signature verification failed.`n$newVerification"
    }
    Write-Host $newVerification
    $newDigest = Get-CertificateDigest $newVerification

    if (Test-Path -LiteralPath $priorApk -PathType Leaf) {
        $priorVerification = (& $apkSigner.FullName verify --print-certs $priorApk 2>&1 | Out-String)
        if ($LASTEXITCODE -ne 0) {
            throw "The prior Uptodown APK signature could not be verified."
        }
        $priorDigest = Get-CertificateDigest $priorVerification
        if ($newDigest -ne $priorDigest) {
            if ($createdNewKey) {
                Write-Warning "The new certificate differs from the unpublished 0.28.4 draft, as expected."
                Write-Warning "Do not submit 0.28.4. Replace it with this APK before Uptodown review."
            }
            else {
                throw "This keystore does not match the 0.28.4 APK. Do not upload the new APK as an update."
            }
        }
        else {
            Write-Host "The certificate matches the prior Uptodown APK." -ForegroundColor Green
        }
    }

    $distributionApk = Join-Path $scriptDirectory "kalima-$versionName-release.apk"
    if (Test-Path -LiteralPath $distributionApk) {
        $existingHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $distributionApk).Hash
        $newHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $signedApk).Hash
        if ($existingHash -ne $newHash) {
            throw "Refusing to overwrite the existing $distributionApk file."
        }
    }
    else {
        Copy-Item -LiteralPath $signedApk -Destination $distributionApk
    }

    Write-Host ""
    Write-Host "Signed beta ready:" -ForegroundColor Green
    Write-Host $distributionApk
    Write-Host "Certificate SHA-256: $newDigest"
    [IO.File]::WriteAllText(
        $statusPath,
        "READY`r`n$distributionApk`r`nCertificate SHA-256: $newDigest"
    )
}
finally {
    foreach ($name in $environmentNames) {
        [Environment]::SetEnvironmentVariable($name, $null, "Process")
    }
    $storePassword = $null
    $keyPassword = $null
}

Write-Host ""
Read-Host "Press Enter to close"
