$ErrorActionPreference = 'Stop'

Add-Type -AssemblyName System.Drawing

$websiteRoot = Split-Path -Parent $PSScriptRoot
$workspaceRoot = Split-Path -Parent $websiteRoot
$captureRoot = Join-Path $workspaceRoot 'distribution\uptodown\screenshots'
$outputRoot = Join-Path $websiteRoot 'public\screens'
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

function New-HexColor {
    param(
        [string]$Hex,
        [int]$Alpha = 255
    )

    $value = $Hex.TrimStart('#')
    return [System.Drawing.Color]::FromArgb(
        $Alpha,
        [Convert]::ToInt32($value.Substring(0, 2), 16),
        [Convert]::ToInt32($value.Substring(2, 2), 16),
        [Convert]::ToInt32($value.Substring(4, 2), 16)
    )
}

function New-Brush {
    param(
        [string]$Hex,
        [int]$Alpha = 255
    )

    return [System.Drawing.SolidBrush]::new((New-HexColor -Hex $Hex -Alpha $Alpha))
}

function Write-WebsiteScreen {
    param(
        [hashtable]$Screen
    )

    $canvasWidth = 1080
    $canvasHeight = 1350
    $bitmap = [System.Drawing.Bitmap]::new(
        $canvasWidth,
        $canvasHeight,
        [System.Drawing.Imaging.PixelFormat]::Format32bppArgb
    )
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality

    $backgroundBrush = New-Brush $Screen.Background
    $graphics.FillRectangle($backgroundBrush, 0, 0, $canvasWidth, $canvasHeight)
    $backgroundBrush.Dispose()

    # Quiet geometric details echo the website without competing with the app UI.
    $haloBrush = New-Brush '#FFFFFF' 105
    $graphics.FillEllipse($haloBrush, 700, -190, 560, 560)
    $haloBrush.Dispose()

    $softBrush = New-Brush $Screen.Accent 52
    $graphics.FillEllipse($softBrush, -270, 310, 610, 760)
    $softBrush.Dispose()

    $goldBrush = New-Brush '#F2C94C' 245
    $graphics.FillEllipse($goldBrush, 70, 48, 54, 54)
    $graphics.FillEllipse($goldBrush, 1002, 315, 25, 25)
    $goldBrush.Dispose()

    $arcPen = [System.Drawing.Pen]::new((New-HexColor '#C99722' 150), 5)
    $graphics.DrawArc($arcPen, 780, -80, 410, 410, 25, 125)
    $arcPen.Dispose()

    $sparkPen = [System.Drawing.Pen]::new((New-HexColor '#123D32' 92), 5)
    $graphics.DrawLine($sparkPen, 1012, 65, 1012, 113)
    $graphics.DrawLine($sparkPen, 988, 89, 1036, 89)
    $sparkPen.Dispose()

    # A deliberately close phone crop keeps the real app text readable in narrow cards.
    $shadowPath = New-RoundedPath -X 14 -Y 112 -Width 1072 -Height 1550 -Radius 104
    $shadowBrush = New-Brush '#06110E' 45
    $graphics.FillPath($shadowBrush, $shadowPath)
    $shadowBrush.Dispose()
    $shadowPath.Dispose()

    $rimPath = New-RoundedPath -X 0 -Y 84 -Width 1080 -Height 1520 -Radius 104
    $rimBrush = New-Brush '#526B63'
    $graphics.FillPath($rimBrush, $rimPath)
    $rimBrush.Dispose()
    $rimPath.Dispose()

    $phonePath = New-RoundedPath -X 12 -Y 96 -Width 1056 -Height 1500 -Radius 94
    $phoneBrush = New-Brush '#071410'
    $graphics.FillPath($phoneBrush, $phonePath)
    $phoneBrush.Dispose()
    $phonePath.Dispose()

    $screenX = 38
    $screenY = 142
    $screenWidth = 1004
    $screenHeight = [int][Math]::Round($screenWidth * $Screen.CropHeight / $Screen.CropWidth)
    $screenPath = New-RoundedPath -X $screenX -Y $screenY -Width $screenWidth -Height $screenHeight -Radius 64
    $screenState = $graphics.Save()
    $graphics.SetClip($screenPath)

    $sourceImage = [System.Drawing.Image]::FromFile($Screen.Source)
    $destination = [System.Drawing.Rectangle]::new($screenX, $screenY, $screenWidth, $screenHeight)
    $source = [System.Drawing.Rectangle]::new(
        $Screen.CropX,
        $Screen.CropY,
        $Screen.CropWidth,
        $Screen.CropHeight
    )
    $graphics.DrawImage(
        $sourceImage,
        $destination,
        $source.X,
        $source.Y,
        $source.Width,
        $source.Height,
        [System.Drawing.GraphicsUnit]::Pixel
    )
    $sourceImage.Dispose()
    $graphics.Restore($screenState)
    $screenPath.Dispose()

    $screenOutline = [System.Drawing.Pen]::new((New-HexColor '#89A198' 170), 4)
    $outlinePath = New-RoundedPath -X $screenX -Y $screenY -Width $screenWidth -Height $screenHeight -Radius 64
    $graphics.DrawPath($screenOutline, $outlinePath)
    $screenOutline.Dispose()
    $outlinePath.Dispose()

    $speakerBrush = New-Brush '#536A63'
    $graphics.FillRectangle($speakerBrush, 464, 113, 152, 11)
    $graphics.FillEllipse($speakerBrush, 459, 113, 11, 11)
    $graphics.FillEllipse($speakerBrush, 610, 113, 11, 11)
    $speakerBrush.Dispose()

    $outputDirectory = Join-Path $outputRoot $Screen.Locale
    [System.IO.Directory]::CreateDirectory($outputDirectory) | Out-Null
    $outputPath = Join-Path $outputDirectory $Screen.Output
    $temporaryPngPath = "$outputPath.render.png"
    $bitmap.Save($temporaryPngPath, [System.Drawing.Imaging.ImageFormat]::Png)

    $graphics.Dispose()
    $bitmap.Dispose()

    try {
        & $ffmpeg.Source -hide_banner -loglevel error -y -i $temporaryPngPath `
            -c:v libwebp -lossless 1 -compression_level 6 $outputPath
        if ($LASTEXITCODE -ne 0) {
            throw "FFmpeg failed to encode $($Screen.Output) as lossless WebP."
        }
    }
    finally {
        Remove-Item -LiteralPath $temporaryPngPath -ErrorAction SilentlyContinue
    }
    Write-Host "Rendered $($Screen.Locale)/$($Screen.Output)"
}

$screens = @(
    @{
        Locale = 'en'; Output = 'lock-screen-learning.webp'; Background = '#F6E0D0'; Accent = '#F2C94C'
        Source = Join-Path $captureRoot 'device-captures\lock-screen-en.png'; CropX = 0; CropY = 0; CropWidth = 1080; CropHeight = 1460
    },
    @{
        Locale = 'en'; Output = 'word-study.webp'; Background = '#DDEFE2'; Accent = '#7BBFA8'
        Source = Join-Path $captureRoot 'device-captures\study-current-en.png'; CropX = 0; CropY = 120; CropWidth = 1080; CropHeight = 1460
    },
    @{
        Locale = 'en'; Output = 'quran-reading.webp'; Background = '#E7DFF2'; Accent = '#B7A4D7'
        Source = Join-Path $captureRoot 'en\quran.png'; CropX = 0; CropY = 135; CropWidth = 1080; CropHeight = 1460
    },
    @{
        Locale = 'pt-BR'; Output = 'lock-screen-learning.webp'; Background = '#F6E0D0'; Accent = '#F2C94C'
        Source = Join-Path $captureRoot 'device-captures\lock-screen-secure-pt-BR.png'; CropX = 0; CropY = 0; CropWidth = 1080; CropHeight = 1460
    },
    @{
        Locale = 'pt-BR'; Output = 'word-study.webp'; Background = '#DDEFE2'; Accent = '#7BBFA8'
        Source = Join-Path $captureRoot 'device-captures\study-current-pt-BR.png'; CropX = 0; CropY = 120; CropWidth = 1080; CropHeight = 1460
    },
    @{
        Locale = 'pt-BR'; Output = 'quran-reading.webp'; Background = '#E7DFF2'; Accent = '#B7A4D7'
        Source = Join-Path $captureRoot 'pt-BR\quran.png'; CropX = 0; CropY = 135; CropWidth = 1080; CropHeight = 1460
    }
)

foreach ($screen in $screens) {
    Write-WebsiteScreen -Screen $screen
}
