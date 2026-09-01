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
$outputPath = Join-Path $outputDirectory "04-reference-style.png"

$canvasWidth = 1080
$canvasHeight = 2400
$phoneWidth = 828
$phoneHeight = 1792
$phoneX = [int](($canvasWidth - $phoneWidth) / 2)
$phoneY = 510
$metalInset = 6
$screenInset = 20
$screenX = $phoneX + $screenInset
$screenY = $phoneY + $screenInset
$screenWidth = $phoneWidth - ($screenInset * 2)
$screenHeight = $phoneHeight - ($screenInset * 2)
$phoneRadius = 64
$blackRadius = $phoneRadius - $metalInset
$screenRadius = $phoneRadius - $screenInset

$sand = [System.Drawing.Color]::FromArgb(255, 241, 232, 213)
$forest = [System.Drawing.Color]::FromArgb(255, 12, 68, 53)
$cream = [System.Drawing.Color]::FromArgb(255, 249, 247, 240)
$muted = [System.Drawing.Color]::FromArgb(255, 105, 124, 117)
$gold = [System.Drawing.Color]::FromArgb(255, 190, 153, 64)
$shadow = [System.Drawing.Color]::FromArgb(42, 0, 0, 0)
$metalLight = [System.Drawing.Color]::FromArgb(255, 224, 224, 217)
$metalDark = [System.Drawing.Color]::FromArgb(255, 75, 78, 74)
$black = [System.Drawing.Color]::FromArgb(255, 5, 7, 7)

$bitmap = New-Object System.Drawing.Bitmap $canvasWidth, $canvasHeight
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
try {
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $graphics.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
    $graphics.Clear($sand)

    $titleFont = New-Object System.Drawing.Font "Arial", 57, ([System.Drawing.FontStyle]::Bold), ([System.Drawing.GraphicsUnit]::Pixel)
    $subtitleFont = New-Object System.Drawing.Font "Arial", 30, ([System.Drawing.FontStyle]::Regular), ([System.Drawing.GraphicsUnit]::Pixel)
    $screenFont = New-Object System.Drawing.Font "Arial", 26, ([System.Drawing.FontStyle]::Bold), ([System.Drawing.GraphicsUnit]::Pixel)
    $titleBrush = New-Object System.Drawing.SolidBrush $forest
    $mutedBrush = New-Object System.Drawing.SolidBrush $muted
    $goldPen = New-Object System.Drawing.Pen $gold, 4
    $shadowBrush = New-Object System.Drawing.SolidBrush $shadow
    $blackBrush = New-Object System.Drawing.SolidBrush $black
    $screenBrush = New-Object System.Drawing.SolidBrush $cream
    $cameraHighlightBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 51, 56, 55))
    $metalEdgePen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(255, 42, 45, 43)), 2
    $centerFormat = New-Object System.Drawing.StringFormat
    $centerFormat.Alignment = [System.Drawing.StringAlignment]::Center
    $centerFormat.LineAlignment = [System.Drawing.StringAlignment]::Center

    $phoneRect = New-Object System.Drawing.Rectangle $phoneX, $phoneY, $phoneWidth, $phoneHeight
    $metalBrush = New-Object System.Drawing.Drawing2D.LinearGradientBrush $phoneRect, $metalLight, $metalDark, 12
    $shadowPath = New-RoundedRectanglePath ($phoneX + 12) ($phoneY + 18) $phoneWidth $phoneHeight $phoneRadius
    $phonePath = New-RoundedRectanglePath $phoneX $phoneY $phoneWidth $phoneHeight $phoneRadius
    $blackPath = New-RoundedRectanglePath ($phoneX + $metalInset) ($phoneY + $metalInset) ($phoneWidth - ($metalInset * 2)) ($phoneHeight - ($metalInset * 2)) $blackRadius
    $screenPath = New-RoundedRectanglePath $screenX $screenY $screenWidth $screenHeight $screenRadius

    try {
        $titleRect = New-Object System.Drawing.RectangleF 55, 65, 970, 105
        $subtitleRect = New-Object System.Drawing.RectangleF 55, 170, 970, 65
        $screenLabelRect = New-Object System.Drawing.RectangleF $screenX, ($screenY + ($screenHeight / 2) - 55), $screenWidth, 110
        $graphics.DrawString("REFERENCE-STYLE FRAME", $titleFont, $titleBrush, $titleRect, $centerFormat)
        $graphics.DrawString("slim metal edge, narrow bezel, punch-hole camera", $subtitleFont, $mutedBrush, $subtitleRect, $centerFormat)
        $graphics.DrawLine($goldPen, 450, 282, 630, 282)

        $graphics.FillPath($shadowBrush, $shadowPath)

        # Slim side controls inspired by the reference silhouette.
        $graphics.FillRectangle($blackBrush, ($phoneX + $phoneWidth - 1), ($phoneY + 235), 5, 108)
        $graphics.FillRectangle($blackBrush, ($phoneX + $phoneWidth - 1), ($phoneY + 380), 5, 154)
        $graphics.FillRectangle($blackBrush, ($phoneX - 4), ($phoneY + 270), 5, 116)

        $graphics.FillPath($metalBrush, $phonePath)
        $graphics.DrawPath($metalEdgePen, $phonePath)
        $graphics.FillPath($blackBrush, $blackPath)
        $graphics.FillPath($screenBrush, $screenPath)

        # A centered punch-hole sits inside the display, as in the reference.
        $cameraCenterX = [int]($canvasWidth / 2)
        $cameraCenterY = $screenY + 18
        $graphics.FillEllipse($cameraHighlightBrush, ($cameraCenterX - 10), ($cameraCenterY - 10), 20, 20)
        $graphics.FillEllipse($blackBrush, ($cameraCenterX - 8), ($cameraCenterY - 8), 16, 16)

        $graphics.DrawString("NO SCREENSHOT APPLIED", $screenFont, $mutedBrush, $screenLabelRect, $centerFormat)
    }
    finally {
        $titleFont.Dispose()
        $subtitleFont.Dispose()
        $screenFont.Dispose()
        $titleBrush.Dispose()
        $mutedBrush.Dispose()
        $goldPen.Dispose()
        $shadowBrush.Dispose()
        $blackBrush.Dispose()
        $screenBrush.Dispose()
        $cameraHighlightBrush.Dispose()
        $metalEdgePen.Dispose()
        $centerFormat.Dispose()
        $metalBrush.Dispose()
        $shadowPath.Dispose()
        $phonePath.Dispose()
        $blackPath.Dispose()
        $screenPath.Dispose()
    }

    $bitmap.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)
}
finally {
    $graphics.Dispose()
    $bitmap.Dispose()
}

Write-Host "Created reference-style bezel preview at $outputPath"
