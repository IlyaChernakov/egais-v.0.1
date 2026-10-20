<#
.SYNOPSIS
    Генерирует значок программы src\main\resources\egais-writeoff.ico.

.DESCRIPTION
    Значок рисуется средствами System.Drawing и упаковывается в контейнер ICO
    (записи в формате PNG — этот вариант поддерживается Windows Vista и новее).
    Скрипт нужен только для воспроизводимости: готовый .ico уже лежит в проекте.
#>
[CmdletBinding()]
param(
    [string]$OutputPath
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

if (-not $OutputPath) {
    $OutputPath = Join-Path (Split-Path -Parent $PSScriptRoot) 'src\main\resources\egais-writeoff.ico'
}

$sizes = @(16, 24, 32, 48, 64, 128, 256)

function New-IconBitmap {
    param([int]$Size)

    $scale = $Size / 256.0
    $px = {
        param([double]$value)
        return [int][Math]::Round($value * $scale)
    }

    $bitmap = [System.Drawing.Bitmap]::new($Size, $Size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.Clear([System.Drawing.Color]::Transparent)

    # Скруглённый фон с градиентом.
    $inset = [int][Math]::Max(1, (& $px 3))
    $innerSize = $Size - 2 * $inset
    $background = [System.Drawing.Rectangle]::new($inset, $inset, $innerSize, $innerSize)
    $radius = (& $px 48)
    $diameter = $radius * 2

    $path = [System.Drawing.Drawing2D.GraphicsPath]::new()
    $path.AddArc($background.X, $background.Y, $diameter, $diameter, 180, 90)
    $path.AddArc($background.Right - $diameter, $background.Y, $diameter, $diameter, 270, 90)
    $path.AddArc($background.Right - $diameter, $background.Bottom - $diameter, $diameter, $diameter, 0, 90)
    $path.AddArc($background.X, $background.Bottom - $diameter, $diameter, $diameter, 90, 90)
    $path.CloseFigure()

    $topColor = [System.Drawing.Color]::FromArgb(255, 59, 124, 245)
    $bottomColor = [System.Drawing.Color]::FromArgb(255, 22, 62, 168)
    $gradient = [System.Drawing.Drawing2D.LinearGradientBrush]::new(
        $background, $topColor, $bottomColor, [System.Drawing.Drawing2D.LinearGradientMode]::ForwardDiagonal)
    $graphics.FillPath($gradient, $path)

    # Лист документа.
    $docX = (& $px 66)
    $docY = (& $px 46)
    $docW = (& $px 124)
    $docH = (& $px 158)
    $docRadius = (& $px 10)
    $docDiameter = $docRadius * 2

    $docPath = [System.Drawing.Drawing2D.GraphicsPath]::new()
    $docPath.AddArc($docX, $docY, $docDiameter, $docDiameter, 180, 90)
    $docPath.AddArc($docX + $docW - $docDiameter, $docY, $docDiameter, $docDiameter, 270, 90)
    $docPath.AddArc($docX + $docW - $docDiameter, $docY + $docH - $docDiameter, $docDiameter, $docDiameter, 0, 90)
    $docPath.AddArc($docX, $docY + $docH - $docDiameter, $docDiameter, $docDiameter, 90, 90)
    $docPath.CloseFigure()

    $white = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::White)
    $graphics.FillPath($white, $docPath)

    # Строки текста в документе.
    $lineColor = [System.Drawing.Color]::FromArgb(255, 168, 190, 226)
    $lineBrush = [System.Drawing.SolidBrush]::new($lineColor)
    $lineHeight = [int][Math]::Max(1, (& $px 10))
    $lineX = (& $px 84)
    $lineY = (& $px 74)
    $lineStep = (& $px 26)
    foreach ($lineWidth in @(88, 72, 80, 56)) {
        $graphics.FillRectangle($lineBrush, $lineX, $lineY, (& $px $lineWidth), $lineHeight)
        $lineY = $lineY + $lineStep
    }

    # Зелёная отметка о выполнении.
    $circleSize = (& $px 112)
    $circleX = $Size - $circleSize - (& $px 24)
    $circleY = $Size - $circleSize - (& $px 24)
    $green = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 46, 190, 110))
    $graphics.FillEllipse($green, $circleX, $circleY, $circleSize, $circleSize)

    $ringWidth = [single][Math]::Max(1, (& $px 5))
    $ring = [System.Drawing.Pen]::new([System.Drawing.Color]::White, $ringWidth)
    $graphics.DrawEllipse($ring, $circleX, $circleY, $circleSize, $circleSize)

    $checkWidth = [single][Math]::Max(1, (& $px 13))
    $check = [System.Drawing.Pen]::new([System.Drawing.Color]::White, $checkWidth)
    $check.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
    $check.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
    $check.LineJoin = [System.Drawing.Drawing2D.LineJoin]::Round

    $centerX = $circleX + $circleSize / 2.0
    $centerY = $circleY + $circleSize / 2.0
    $points = [System.Drawing.PointF[]]@(
        [System.Drawing.PointF]::new([single]($centerX - (& $px 26)), [single]($centerY + (& $px 2))),
        [System.Drawing.PointF]::new([single]($centerX - (& $px 8)), [single]($centerY + (& $px 20))),
        [System.Drawing.PointF]::new([single]($centerX + (& $px 26)), [single]($centerY - (& $px 18)))
    )
    $graphics.DrawLines($check, $points)

    $graphics.Dispose()
    $gradient.Dispose()
    $white.Dispose()
    $lineBrush.Dispose()
    $green.Dispose()
    $ring.Dispose()
    $check.Dispose()
    $path.Dispose()
    $docPath.Dispose()

    return $bitmap
}

$pngData = @()
foreach ($size in $sizes) {
    $bitmap = New-IconBitmap -Size $size
    $stream = [System.IO.MemoryStream]::new()
    $bitmap.Save($stream, [System.Drawing.Imaging.ImageFormat]::Png)
    $pngData += , $stream.ToArray()
    $stream.Dispose()
    $bitmap.Dispose()
}

$directory = Split-Path -Parent $OutputPath
if (-not (Test-Path $directory)) {
    New-Item -ItemType Directory -Force -Path $directory | Out-Null
}

$fileStream = [System.IO.File]::Create($OutputPath)
$writer = [System.IO.BinaryWriter]::new($fileStream)

# ICONDIR
$writer.Write([uint16]0)
$writer.Write([uint16]1)
$writer.Write([uint16]$sizes.Count)

# ICONDIRENTRY для каждого размера
$offset = 6 + 16 * $sizes.Count
for ($i = 0; $i -lt $sizes.Count; $i++) {
    $size = $sizes[$i]
    $bytes = $pngData[$i]
    $dimension = 0
    if ($size -lt 256) { $dimension = $size }
    $writer.Write([byte]$dimension)
    $writer.Write([byte]$dimension)
    $writer.Write([byte]0)
    $writer.Write([byte]0)
    $writer.Write([uint16]1)
    $writer.Write([uint16]32)
    $writer.Write([uint32]$bytes.Length)
    $writer.Write([uint32]$offset)
    $offset = $offset + $bytes.Length
}

foreach ($bytes in $pngData) {
    $writer.Write($bytes)
}

$writer.Flush()
$writer.Dispose()
$fileStream.Dispose()

Write-Host "Значок записан: $OutputPath ($((Get-Item $OutputPath).Length) байт, размеры: $($sizes -join ', '))"
