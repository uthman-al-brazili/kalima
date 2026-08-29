$ErrorActionPreference = 'Stop'

Add-Type -AssemblyName System.Drawing

$websiteRoot = Split-Path -Parent $PSScriptRoot
$screenRoot = Join-Path $websiteRoot 'public\screens'
$ffmpeg = Get-Command 'ffmpeg' -ErrorAction Stop

function New-RoundedPath {
    param(
        [float]$X,
        [float]$Y,
        [float]$Width,
        [float]$Height,
        [float]$Radius
    )

    $diameter = $Radius * 2
    $path = [System.Drawing.Drawing2D.GraphicsPath]::new()
    $path.AddArc($X, $Y, $diameter, $diameter, 180, 90)
    $path.AddArc($X + $Width - $diameter, $Y, $diameter, $diameter, 270, 90)
    $path.AddArc($X + $Width - $diameter, $Y + $Height - $diameter, $diameter, $diameter, 0, 90)
    $path.AddArc($X, $Y + $Height - $diameter, $diameter, $diameter, 90, 90)
    $path.CloseFigure()
    return $path
}

function Write-AlignedLockScreen {
    param(
        [string]$Locale
    )

    $localeRoot = Join-Path $screenRoot $Locale
    $targetPath = Join-Path $localeRoot 'lock-screen-learning.webp'
    # The shell is language-neutral; use one shared reference so both locale
    # variants receive identical bezel geometry and no locale-specific backdrop.
    $referencePath = Join-Path (Join-Path $screenRoot 'en') 'word-study.webp'
    $temporaryRoot = Join-Path ([System.IO.Path]::GetTempPath()) "kalima-lock-screen-artwork-$Locale"
    [System.IO.Directory]::CreateDirectory($temporaryRoot) | Out-Null
    $targetPng = Join-Path $temporaryRoot 'target.png'
    $referencePng = Join-Path $temporaryRoot 'reference.png'
    $renderPng = Join-Path $temporaryRoot 'render.png'

    try {
        & $ffmpeg.Source -hide_banner -loglevel error -y -i $targetPath $targetPng
        & $ffmpeg.Source -hide_banner -loglevel error -y -i $referencePath $referencePng
        if ($LASTEXITCODE -ne 0) { throw "Unable to decode the $Locale artwork." }

        $target = [System.Drawing.Bitmap]::FromFile($targetPng)
        $reference = [System.Drawing.Bitmap]::FromFile($referencePng)
        $output = [System.Drawing.Bitmap]::new(1080, 1350, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        $graphics = [System.Drawing.Graphics]::FromImage($output)
        $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
        $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
        $graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality

        # Keep every pixel of the original artwork and copy only the bezel ring
        # from the adjacent word-study composition. The screen opening is
        # excluded from the clip, so the app screenshot is never redrawn,
        # resized, moved, or re-encoded separately.
        $graphics.DrawImageUnscaled($target, 0, 0)
        $phonePath = New-RoundedPath -X 65 -Y 47 -Width 950 -Height 1515 -Radius 102
        $screenPath = New-RoundedPath -X 103 -Y 87 -Width 874 -Height 1950 -Radius 62
        $bezelRegion = [System.Drawing.Region]::new($phonePath)
        $bezelRegion.Exclude($screenPath)
        $phoneState = $graphics.Save()
        $graphics.SetClip($bezelRegion, [System.Drawing.Drawing2D.CombineMode]::Replace)
        $graphics.DrawImageUnscaled($reference, 0, 0)
        $graphics.Restore($phoneState)
        $bezelRegion.Dispose()
        $screenPath.Dispose()
        $phonePath.Dispose()

        $output.Save($renderPng, [System.Drawing.Imaging.ImageFormat]::Png)
        $graphics.Dispose()
        $output.Dispose()
        $reference.Dispose()
        $target.Dispose()

        & $ffmpeg.Source -hide_banner -loglevel error -y -i $renderPng `
            -c:v libwebp -lossless 1 -compression_level 6 $targetPath
        if ($LASTEXITCODE -ne 0) { throw "Unable to encode the $Locale artwork." }
    }
    finally {
        Remove-Item -LiteralPath $temporaryRoot -Recurse -Force -ErrorAction SilentlyContinue
    }
}

Write-AlignedLockScreen -Locale 'en'
Write-AlignedLockScreen -Locale 'pt-BR'
