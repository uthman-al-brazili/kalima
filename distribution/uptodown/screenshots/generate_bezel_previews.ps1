param(
    [string]$WorkspaceRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..\.."))
)

Add-Type -AssemblyName System.Drawing

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

$outputDirectory = Join-Path $WorkspaceRoot "distribution\uptodown\promotional\bezel-previews"
New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null

$canvasWidth = 1080
$canvasHeight = 2400
$phoneWidth = 828
$phoneX = [int](($canvasWidth - $phoneWidth) / 2)
$phoneCenterY = 1370
$screenRadius = 40
$bezelInset = 8

$forest = [System.Drawing.Color]::FromArgb(255, 12, 68, 53)
$cream = [System.Drawing.Color]::FromArgb(255, 249, 247, 240)
$sand = [System.Drawing.Color]::FromArgb(255, 241, 232, 213)
$gold = [System.Drawing.Color]::FromArgb(255, 190, 153, 64)
$muted = [System.Drawing.Color]::FromArgb(255, 105, 124, 117)
$shadow = [System.Drawing.Color]::FromArgb(45, 0, 0, 0)
$shellTop = [System.Drawing.Color]::FromArgb(255, 105, 124, 117)
$shellBottom = [System.Drawing.Color]::FromArgb(255, 19, 27, 25)
$bezel = [System.Drawing.Color]::FromArgb(255, 5, 9, 9)
$rim = [System.Drawing.Color]::FromArgb(255, 137, 158, 150)
$speaker = [System.Drawing.Color]::FromArgb(255, 72, 82, 79)

$variants = @(
    @{ Number = 1; Thickness = 24; Output = "01-bezel-24px.png" },
    @{ Number = 2; Thickness = 27; Output = "02-bezel-27px.png" },
    @{ Number = 3; Thickness = 30; Output = "03-bezel-30px.png" }
)

foreach ($variant in $variants) {
    $thickness = $variant.Thickness
    $screenWidth = $phoneWidth - ($thickness * 2)
    $screenHeight = [int][Math]::Round($screenWidth * 20 / 9)
    $phoneHeight = $screenHeight + ($thickness * 2)
    $phoneY = [int]($phoneCenterY - ($phoneHeight / 2))
    $screenX = $phoneX + $thickness
    $screenY = $phoneY + $thickness
    $phoneRadius = $screenRadius + $thickness
    $bezelRadius = $phoneRadius - $bezelInset

    $bitmap = New-Object System.Drawing.Bitmap $canvasWidth, $canvasHeight
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    try {
        $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
        $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $graphics.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
        $graphics.Clear($sand)

        $titleFont = New-Object System.Drawing.Font "Arial", 60, ([System.Drawing.FontStyle]::Bold), ([System.Drawing.GraphicsUnit]::Pixel)
        $subtitleFont = New-Object System.Drawing.Font "Arial", 32, ([System.Drawing.FontStyle]::Regular), ([System.Drawing.GraphicsUnit]::Pixel)
        $screenFont = New-Object System.Drawing.Font "Arial", 26, ([System.Drawing.FontStyle]::Bold), ([System.Drawing.GraphicsUnit]::Pixel)
        $titleBrush = New-Object System.Drawing.SolidBrush $forest
        $mutedBrush = New-Object System.Drawing.SolidBrush $muted
        $goldPen = New-Object System.Drawing.Pen $gold, 4
        $shadowBrush = New-Object System.Drawing.SolidBrush $shadow
        $bezelBrush = New-Object System.Drawing.SolidBrush $bezel
        $screenBrush = New-Object System.Drawing.SolidBrush $cream
        $speakerBrush = New-Object System.Drawing.SolidBrush $speaker
        $cameraBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 15, 22, 20))
        $cameraHighlightBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 70, 102, 92))
        $rimPen = New-Object System.Drawing.Pen $rim, 5
        $screenPen = New-Object System.Drawing.Pen ([System.Drawing.Color]::Black), 3
        $centerFormat = New-Object System.Drawing.StringFormat
        $centerFormat.Alignment = [System.Drawing.StringAlignment]::Center
        $centerFormat.LineAlignment = [System.Drawing.StringAlignment]::Center

        $phoneRect = New-Object System.Drawing.Rectangle $phoneX, $phoneY, $phoneWidth, $phoneHeight
        $shellBrush = New-Object System.Drawing.Drawing2D.LinearGradientBrush $phoneRect, $shellTop, $shellBottom, 90
        $shadowPath = New-RoundedRectanglePath ($phoneX + 14) ($phoneY + 18) $phoneWidth $phoneHeight $phoneRadius
        $phonePath = New-RoundedRectanglePath $phoneX $phoneY $phoneWidth $phoneHeight $phoneRadius
        $bezelPath = New-RoundedRectanglePath ($phoneX + $bezelInset) ($phoneY + $bezelInset) ($phoneWidth - ($bezelInset * 2)) ($phoneHeight - ($bezelInset * 2)) $bezelRadius
        $screenPath = New-RoundedRectanglePath $screenX $screenY $screenWidth $screenHeight $screenRadius
        $speakerPath = New-RoundedRectanglePath (($canvasWidth / 2) - 53) ($phoneY + 6) 106 6 3

        try {
            $titleRect = New-Object System.Drawing.RectangleF 55, 70, 970, 100
            $subtitleRect = New-Object System.Drawing.RectangleF 55, 175, 970, 65
            $screenLabelRect = New-Object System.Drawing.RectangleF $screenX, ($screenY + ($screenHeight / 2) - 55), $screenWidth, 110
            $optionLabel = "OP$([char]0x00C7)$([char]0x00C3)O $($variant.Number)"
            $graphics.DrawString($optionLabel, $titleFont, $titleBrush, $titleRect, $centerFormat)
            $graphics.DrawString("moldura de $thickness px", $subtitleFont, $mutedBrush, $subtitleRect, $centerFormat)
            $graphics.DrawLine($goldPen, 450, 285, 630, 285)

            $graphics.FillPath($shadowBrush, $shadowPath)
            $graphics.FillPath($shellBrush, $phonePath)
            $graphics.DrawPath($rimPen, $phonePath)
            $graphics.FillPath($bezelBrush, $bezelPath)
            $graphics.FillPath($screenBrush, $screenPath)
            $graphics.DrawPath($screenPen, $screenPath)
            $graphics.FillPath($speakerBrush, $speakerPath)
            $graphics.FillEllipse($cameraBrush, ($canvasWidth / 2) + 160, ($phoneY + 4), 10, 10)
            $graphics.FillEllipse($cameraHighlightBrush, ($canvasWidth / 2) + 163, ($phoneY + 7), 3, 3)
            $graphics.DrawString("TELA SEM CAPTURA APLICADA", $screenFont, $mutedBrush, $screenLabelRect, $centerFormat)
        }
        finally {
            $titleFont.Dispose()
            $subtitleFont.Dispose()
            $screenFont.Dispose()
            $titleBrush.Dispose()
            $mutedBrush.Dispose()
            $goldPen.Dispose()
            $shadowBrush.Dispose()
            $bezelBrush.Dispose()
            $screenBrush.Dispose()
            $speakerBrush.Dispose()
            $cameraBrush.Dispose()
            $cameraHighlightBrush.Dispose()
            $rimPen.Dispose()
            $screenPen.Dispose()
            $centerFormat.Dispose()
            $shellBrush.Dispose()
            $shadowPath.Dispose()
            $phonePath.Dispose()
            $bezelPath.Dispose()
            $screenPath.Dispose()
            $speakerPath.Dispose()
        }

        $outputPath = Join-Path $outputDirectory $variant.Output
        $bitmap.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)
    }
    finally {
        $graphics.Dispose()
        $bitmap.Dispose()
    }
}

Write-Host "Created 3 bezel-only previews in $outputDirectory"
