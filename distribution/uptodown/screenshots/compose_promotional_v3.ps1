param(
    [string]$WorkspaceRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..\.."))
)

Add-Type -AssemblyName System.Drawing

$canvasWidth = 1080
$canvasHeight = 2400
$phoneWidth = 828
$phoneHeight = 1791
$phoneX = [int](($canvasWidth - $phoneWidth) / 2)
$phoneY = 510
$screenWidth = 788
$screenHeight = 1751
$screenX = $phoneX + 20
$screenY = $phoneY + 20
$screenRadius = 44
$phoneRadius = 64
$bezelInset = 6
$bezelRadius = $phoneRadius - $bezelInset

$forest = [System.Drawing.Color]::FromArgb(255, 12, 68, 53)
$cream = [System.Drawing.Color]::FromArgb(255, 249, 247, 240)
$gold = [System.Drawing.Color]::FromArgb(255, 190, 153, 64)
$shadow = [System.Drawing.Color]::FromArgb(45, 0, 0, 0)
$shellTop = [System.Drawing.Color]::FromArgb(255, 224, 224, 217)
$shellBottom = [System.Drawing.Color]::FromArgb(255, 75, 78, 74)
$bezel = [System.Drawing.Color]::FromArgb(255, 5, 9, 9)
$rim = [System.Drawing.Color]::FromArgb(255, 42, 45, 43)

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

function Resolve-WorkspacePath([string]$relativePath) {
    return Join-Path $WorkspaceRoot $relativePath
}

$items = @(
    @{
        Source = "distribution\uptodown\screenshots\current-v0.34.0\en\01-lock-screen.png"
        Output = "distribution\uptodown\promotional\screenshots-v3\en\01-learn-before-you-unlock.png"
        Title = "learn Arabic`nbefore you unlock"
        Background = [System.Drawing.Color]::FromArgb(255, 241, 232, 213)
        TitleColor = $forest
    },
    @{
        Source = "distribution\uptodown\screenshots\current-v0.34.0\pt-BR\01-lock-screen.png"
        Output = "distribution\uptodown\promotional\screenshots-v3\pt-BR\01-aprenda-antes-de-desbloquear.png"
        Title = "aprenda $([char]0x00E1)rabe`nantes de desbloquear"
        Background = [System.Drawing.Color]::FromArgb(255, 241, 232, 213)
        TitleColor = $forest
    },
    @{
        Source = "distribution\uptodown\screenshots\current-v0.34.0\en\02-daily-mission.png"
        Output = "distribution\uptodown\promotional\screenshots-v3\en\02-short-daily-sessions.png"
        Title = "build a habit with`nshort sessions"
        Background = [System.Drawing.Color]::FromArgb(255, 220, 231, 224)
        TitleColor = $forest
    },
    @{
        Source = "distribution\uptodown\screenshots\current-v0.34.0\pt-BR\02-daily-mission.png"
        Output = "distribution\uptodown\promotional\screenshots-v3\pt-BR\02-sessoes-curtas.png"
        Title = "crie o h$([char]0x00E1)bito com`nsess$([char]0x00F5)es curtas"
        Background = [System.Drawing.Color]::FromArgb(255, 220, 231, 224)
        TitleColor = $forest
    },
    @{
        Source = "distribution\uptodown\screenshots\current-v0.34.0\en\03-context-quiz.png"
        Output = "distribution\uptodown\promotional\screenshots-v3\en\03-words-in-context.png"
        Title = "practice words`nin context"
        Background = [System.Drawing.Color]::FromArgb(255, 233, 228, 216)
        TitleColor = $forest
    },
    @{
        Source = "distribution\uptodown\screenshots\current-v0.34.0\pt-BR\03-context-quiz.png"
        Output = "distribution\uptodown\promotional\screenshots-v3\pt-BR\03-palavras-no-contexto.png"
        Title = "pratique palavras`nno contexto"
        Background = [System.Drawing.Color]::FromArgb(255, 233, 228, 216)
        TitleColor = $forest
    },
    @{
        Source = "distribution\uptodown\screenshots\current-v0.34.0\en\04-quran-reading.png"
        Output = "distribution\uptodown\promotional\screenshots-v3\en\04-quran-word-by-word.png"
        Title = "read the Quran`nword by word"
        Background = [System.Drawing.Color]::FromArgb(255, 18, 62, 52)
        TitleColor = $cream
    },
    @{
        Source = "distribution\uptodown\screenshots\current-v0.34.0\pt-BR\04-quran-reading.png"
        Output = "distribution\uptodown\promotional\screenshots-v3\pt-BR\04-alcorao-palavra-por-palavra.png"
        Title = "leia o Alcor$([char]0x00E3)o`npalavra por palavra"
        Background = [System.Drawing.Color]::FromArgb(255, 18, 62, 52)
        TitleColor = $cream
    }
)

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
                $graphics.Clear($item.Background)
                $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
                $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
                $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
                $graphics.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit

                $titleFont = New-Object System.Drawing.Font "Segoe UI Black", 72, ([System.Drawing.FontStyle]::Bold), ([System.Drawing.GraphicsUnit]::Pixel)
                $titleBrush = New-Object System.Drawing.SolidBrush $item.TitleColor
                $goldPen = New-Object System.Drawing.Pen $gold, 4
                $shadowBrush = New-Object System.Drawing.SolidBrush $shadow
                $bezelBrush = New-Object System.Drawing.SolidBrush $bezel
                $cameraBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 15, 22, 20))
                $cameraHighlightBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 51, 56, 55))
                $rimPen = New-Object System.Drawing.Pen $rim, 2
                $centerFormat = New-Object System.Drawing.StringFormat
                $centerFormat.Alignment = [System.Drawing.StringAlignment]::Center
                $centerFormat.LineAlignment = [System.Drawing.StringAlignment]::Center
                $centerFormat.Trimming = [System.Drawing.StringTrimming]::EllipsisWord

                $phoneRect = New-Object System.Drawing.Rectangle $phoneX, $phoneY, $phoneWidth, $phoneHeight
                $shellBrush = New-Object System.Drawing.Drawing2D.LinearGradientBrush $phoneRect, $shellTop, $shellBottom, 12
                # Reference-style phone: a slim metallic edge, narrow black
                # bezel, softly rounded display, and centered punch-hole camera.
                $shadowPath = New-RoundedRectanglePath ($phoneX + 12) ($phoneY + 18) $phoneWidth $phoneHeight $phoneRadius
                $phonePath = New-RoundedRectanglePath $phoneX $phoneY $phoneWidth $phoneHeight $phoneRadius
                $bezelPath = New-RoundedRectanglePath ($phoneX + $bezelInset) ($phoneY + $bezelInset) ($phoneWidth - ($bezelInset * 2)) ($phoneHeight - ($bezelInset * 2)) $bezelRadius
                $screenPath = New-RoundedRectanglePath $screenX $screenY $screenWidth $screenHeight $screenRadius
                try {
                    $titleRect = New-Object System.Drawing.RectangleF 55, 35, 970, 390
                    $graphics.DrawString($item.Title, $titleFont, $titleBrush, $titleRect, $centerFormat)
                    $graphics.DrawLine($goldPen, 450, 458, 630, 458)

                    $graphics.FillPath($shadowBrush, $shadowPath)
                    $graphics.FillRectangle($bezelBrush, ($phoneX + $phoneWidth - 1), ($phoneY + 235), 5, 108)
                    $graphics.FillRectangle($bezelBrush, ($phoneX + $phoneWidth - 1), ($phoneY + 380), 5, 154)
                    $graphics.FillRectangle($bezelBrush, ($phoneX - 4), ($phoneY + 270), 5, 116)
                    $graphics.FillPath($shellBrush, $phonePath)
                    $graphics.DrawPath($rimPen, $phonePath)
                    $graphics.FillPath($bezelBrush, $bezelPath)
                    $clipState = $graphics.Save()
                    try {
                        $graphics.SetClip($screenPath)
                        $destination = New-Object System.Drawing.Rectangle $screenX, $screenY, $screenWidth, $screenHeight
                        $graphics.DrawImage(
                            $source,
                            $destination,
                            0,
                            0,
                            $source.Width,
                            $source.Height,
                            [System.Drawing.GraphicsUnit]::Pixel
                        )
                    }
                    finally {
                        $graphics.Restore($clipState)
                    }
                    $cameraCenterX = [int]($canvasWidth / 2)
                    $cameraCenterY = $screenY + 18
                    $graphics.FillEllipse($cameraHighlightBrush, ($cameraCenterX - 10), ($cameraCenterY - 10), 20, 20)
                    $graphics.FillEllipse($cameraBrush, ($cameraCenterX - 8), ($cameraCenterY - 8), 16, 16)
                }
                finally {
                    $titleFont.Dispose()
                    $titleBrush.Dispose()
                    $goldPen.Dispose()
                    $shadowBrush.Dispose()
                    $bezelBrush.Dispose()
                    $cameraBrush.Dispose()
                    $cameraHighlightBrush.Dispose()
                    $rimPen.Dispose()
                    $centerFormat.Dispose()
                    $shellBrush.Dispose()
                    $shadowPath.Dispose()
                    $phonePath.Dispose()
                    $bezelPath.Dispose()
                    $screenPath.Dispose()
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

Write-Output "Created $($items.Count) Duolingo-inspired promotional images from Kalima 0.34.0 screenshots."
