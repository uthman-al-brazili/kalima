param(
    [string]$WorkspaceRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..\.."))
)

Add-Type -AssemblyName System.Drawing

$canvasWidth = 1080
$canvasHeight = 2400
$phoneWidth = 864
$phoneHeight = 1920
$phoneX = [int](($canvasWidth - $phoneWidth) / 2)
$phoneY = 420
$screenWidth = 828
$screenHeight = 1840
$screenX = [int](($canvasWidth - $screenWidth) / 2)
$screenY = 460

$background = [System.Drawing.Color]::FromArgb(255, 249, 247, 240)
$forest = [System.Drawing.Color]::FromArgb(255, 12, 68, 53)
$sage = [System.Drawing.Color]::FromArgb(255, 100, 119, 110)
$gold = [System.Drawing.Color]::FromArgb(255, 226, 190, 91)
$shadow = [System.Drawing.Color]::FromArgb(48, 0, 0, 0)
$shellTop = [System.Drawing.Color]::FromArgb(255, 91, 113, 106)
$shellBottom = [System.Drawing.Color]::FromArgb(255, 20, 30, 29)
$bezel = [System.Drawing.Color]::FromArgb(255, 5, 9, 9)
$rim = [System.Drawing.Color]::FromArgb(255, 143, 168, 158)
$speaker = [System.Drawing.Color]::FromArgb(255, 73, 84, 81)

function New-RoundedRectanglePath(
    [float]$x,
    [float]$y,
    [float]$width,
    [float]$height,
    [float]$radius
) {
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $diameter = $radius * 2
    $path.AddArc($x, $y, $diameter, $diameter, 180, 90)
    $path.AddArc($x + $width - $diameter, $y, $diameter, $diameter, 270, 90)
    $path.AddArc($x + $width - $diameter, $y + $height - $diameter, $diameter, $diameter, 0, 90)
    $path.AddArc($x, $y + $height - $diameter, $diameter, $diameter, 90, 90)
    $path.CloseFigure()
    return $path
}

$items = @(
    @{
        Locale = "en"
        Source = "distribution\uptodown\screenshots\device-captures\lock-screen-secure-en.png"
        Output = "distribution\uptodown\promotional\screenshots-v2\en\01-lock-screen-learning.png"
        Title = "LEARN ON YOUR LOCK SCREEN"
        Subtitle = "A Quranic Arabic card appears while your phone stays protected."
    },
    @{
        Locale = "pt-BR"
        Source = "distribution\uptodown\screenshots\device-captures\lock-screen-secure-pt-BR.png"
        Output = "distribution\uptodown\promotional\screenshots-v2\pt-BR\01-aprenda-na-tela-bloqueada.png"
        Title = "APRENDA NA TELA DE BLOQUEIO"
        Subtitle = "Um cartão de árabe corânico aparece enquanto seu celular continua protegido."
    },
    @{
        Locale = "en"
        Source = "distribution\uptodown\screenshots\en\study.png"
        Output = "distribution\uptodown\promotional\screenshots-v2\en\02-word-study.png"
        Title = "ONE WORD AT A TIME"
        Subtitle = "Review Arabic, pronunciation, meaning, and Quran context."
    },
    @{
        Locale = "pt-BR"
        Source = "distribution\uptodown\screenshots\pt-BR\study.png"
        Output = "distribution\uptodown\promotional\screenshots-v2\pt-BR\02-estudo-de-palavras.png"
        Title = "UMA PALAVRA DE CADA VEZ"
        Subtitle = "Revise árabe, pronúncia, significado e contexto do Alcorão."
    },
    @{
        Locale = "en"
        Source = "distribution\uptodown\screenshots\en\quran.png"
        Output = "distribution\uptodown\promotional\screenshots-v2\en\03-quran-reading.png"
        Title = "READ WORDS IN QURAN CONTEXT"
        Subtitle = "Explore Al-Fatihah and tap words as you learn."
    },
    @{
        Locale = "pt-BR"
        Source = "distribution\uptodown\screenshots\pt-BR\quran.png"
        Output = "distribution\uptodown\promotional\screenshots-v2\pt-BR\03-leitura-do-alcorao.png"
        Title = "LEIA PALAVRAS NO CONTEXTO DO ALCORÃO"
        Subtitle = "Explore a Al-Fatihah e toque nas palavras enquanto aprende."
    },
    @{
        Locale = "en"
        Source = "distribution\uptodown\screenshots\en\foundations.png"
        Output = "distribution\uptodown\promotional\screenshots-v2\en\04-arabic-foundations.png"
        Title = "BUILD YOUR ARABIC FOUNDATION"
        Subtitle = "Practice the alphabet and Arabic-Indic numbers at your own pace."
    },
    @{
        Locale = "pt-BR"
        Source = "distribution\uptodown\screenshots\pt-BR\foundations.png"
        Output = "distribution\uptodown\promotional\screenshots-v2\pt-BR\04-fundamentos-do-arabe.png"
        Title = "CONSTRUA SUA BASE EM ÁRABE"
        Subtitle = "Pratique o alfabeto e os números indo-arábicos no seu ritmo."
    }
)

function Resolve-WorkspacePath([string]$relativePath) {
    return Join-Path $WorkspaceRoot $relativePath
}

foreach ($item in $items) {
    $sourcePath = Resolve-WorkspacePath $item.Source
    $outputPath = Resolve-WorkspacePath $item.Output
    $outputDirectory = Split-Path -Parent $outputPath
    New-Item -ItemType Directory -Force $outputDirectory | Out-Null

    $source = [System.Drawing.Image]::FromFile($sourcePath)
    try {
        if ($source.Width -ne 1080 -or $source.Height -ne 2400) {
            throw "Expected a 1080x2400 real screenshot: $sourcePath"
        }

        $canvas = New-Object System.Drawing.Bitmap $canvasWidth, $canvasHeight, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        try {
            $graphics = [System.Drawing.Graphics]::FromImage($canvas)
            try {
                $graphics.Clear($background)
                $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
                $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
                $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
                $graphics.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit

                $titleFont = New-Object System.Drawing.Font "Segoe UI", 50, ([System.Drawing.FontStyle]::Bold), ([System.Drawing.GraphicsUnit]::Pixel)
                $subtitleFont = New-Object System.Drawing.Font "Segoe UI", 30, ([System.Drawing.FontStyle]::Regular), ([System.Drawing.GraphicsUnit]::Pixel)
                $titleBrush = New-Object System.Drawing.SolidBrush $forest
                $subtitleBrush = New-Object System.Drawing.SolidBrush $sage
                $goldPen = New-Object System.Drawing.Pen $gold, 5
                $shadowBrush = New-Object System.Drawing.SolidBrush $shadow
                $bezelBrush = New-Object System.Drawing.SolidBrush $bezel
                $speakerBrush = New-Object System.Drawing.SolidBrush $speaker
                $cameraBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 19, 29, 28))
                $cameraHighlightBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 65, 101, 91))
                $rimPen = New-Object System.Drawing.Pen $rim, 5
                $screenPen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(255, 0, 0, 0)), 3

                $shadowPath = New-RoundedRectanglePath ($phoneX + 14) ($phoneY + 18) $phoneWidth $phoneHeight 92
                $phonePath = New-RoundedRectanglePath $phoneX $phoneY $phoneWidth $phoneHeight 92
                $bezelPath = New-RoundedRectanglePath ($phoneX + 8) ($phoneY + 8) ($phoneWidth - 16) ($phoneHeight - 16) 84
                $screenPath = New-RoundedRectanglePath $screenX $screenY $screenWidth $screenHeight 66
                $speakerPath = New-RoundedRectanglePath 487 438 106 8 4
                $phoneRect = New-Object System.Drawing.Rectangle $phoneX, $phoneY, $phoneWidth, $phoneHeight
                $shellBrush = New-Object System.Drawing.Drawing2D.LinearGradientBrush $phoneRect, $shellTop, $shellBottom, 90

                try {
                    $centerFormat = New-Object System.Drawing.StringFormat
                    $centerFormat.Alignment = [System.Drawing.StringAlignment]::Center
                    $centerFormat.LineAlignment = [System.Drawing.StringAlignment]::Center
                    $centerFormat.Trimming = [System.Drawing.StringTrimming]::EllipsisWord

                    $titleRect = New-Object System.Drawing.RectangleF 55, 45, 970, 185
                    $subtitleRect = New-Object System.Drawing.RectangleF 80, 225, 920, 115
                    $graphics.DrawString($item.Title, $titleFont, $titleBrush, $titleRect, $centerFormat)
                    $graphics.DrawString($item.Subtitle, $subtitleFont, $subtitleBrush, $subtitleRect, $centerFormat)
                    $graphics.DrawLine($goldPen, 430, 380, 650, 380)

                    # The outer phone and inner display are both exact 9:20
                    # width-to-height ratios. The real screenshot is only scaled
                    # and rounded-clipped; none of its app UI is redrawn.
                    $graphics.FillPath($shadowBrush, $shadowPath)
                    $graphics.FillPath($shellBrush, $phonePath)
                    $graphics.DrawPath($rimPen, $phonePath)
                    $graphics.FillPath($bezelBrush, $bezelPath)

                    $graphics.FillPath($speakerBrush, $speakerPath)
                    $graphics.FillEllipse($cameraBrush, 622, 436, 13, 13)
                    $graphics.FillEllipse($cameraHighlightBrush, 626, 439, 4, 4)

                    $destination = New-Object System.Drawing.Rectangle $screenX, $screenY, $screenWidth, $screenHeight
                    $clipState = $graphics.Save()
                    try {
                        $graphics.SetClip($screenPath)
                        $graphics.DrawImage($source, $destination, 0, 0, $source.Width, $source.Height, [System.Drawing.GraphicsUnit]::Pixel)
                    }
                    finally {
                        $graphics.Restore($clipState)
                    }
                    $graphics.DrawPath($screenPen, $screenPath)
                }
                finally {
                    $centerFormat.Dispose()
                    $titleFont.Dispose()
                    $subtitleFont.Dispose()
                    $titleBrush.Dispose()
                    $subtitleBrush.Dispose()
                    $goldPen.Dispose()
                    $shadowBrush.Dispose()
                    $bezelBrush.Dispose()
                    $speakerBrush.Dispose()
                    $cameraBrush.Dispose()
                    $cameraHighlightBrush.Dispose()
                    $rimPen.Dispose()
                    $screenPen.Dispose()
                    $shellBrush.Dispose()
                    $shadowPath.Dispose()
                    $phonePath.Dispose()
                    $bezelPath.Dispose()
                    $screenPath.Dispose()
                    $speakerPath.Dispose()
                }
            }
            finally {
                $graphics.Dispose()
            }

            $canvas.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)
        }
        finally {
            $canvas.Dispose()
        }
    }
    finally {
        $source.Dispose()
    }
}

Write-Output "Created $($items.Count) screenshot-based promotional images."
