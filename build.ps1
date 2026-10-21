<#
.SYNOPSIS
    Сборка программы «Генерация актов списания ЕГАИС» под Windows x64.

.DESCRIPTION
    Скрипт делает три вещи:
      1. компилирует исходники (target 11) в build\classes;
      2. собирает исполняемый jar build\egais-writeoff.jar;
      3. через jpackage создаёт готовое приложение dist\EgaisWriteOff\EgaisWriteOff.exe
         со встроенной средой выполнения Java (JRE не требуется на машине пользователя).

    Для шага 3 нужен JDK 14 или новее (проверяется наличие jpackage).

.PARAMETER JdkHome
    Каталог JDK, которым выполняется сборка. По умолчанию берётся JAVA_HOME,
    затем — первый подходящий JDK из %USERPROFILE%\.jdks.

.PARAMETER SkipPackage
    Собрать только jar, без создания exe.

.PARAMETER AppVersion
    Версия приложения для jpackage. По умолчанию 1.0.

.EXAMPLE
    .\build.ps1
    .\build.ps1 -SkipPackage
#>
[CmdletBinding()]
param(
    [string]$JdkHome,
    [switch]$SkipPackage,
    [string]$AppVersion = '1.0'
)

$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot

function Write-Step([string]$text) {
    Write-Host ''
    Write-Host "==> $text" -ForegroundColor Cyan
}

function Resolve-JdkHome {
    param([string]$Explicit)

    $candidates = New-Object System.Collections.Generic.List[string]
    if ($Explicit) { $candidates.Add($Explicit) }
    if ($env:JAVA_HOME) { $candidates.Add($env:JAVA_HOME) }

    $jdksRoot = Join-Path $env:USERPROFILE '.jdks'
    if (Test-Path $jdksRoot) {
        # Сначала JDK 17+ (нужен jpackage), затем остальные.
        Get-ChildItem $jdksRoot -Directory |
            Where-Object { $_.Name -notlike '.*' -and (Test-Path (Join-Path $_.FullName 'bin\javac.exe')) } |
            Sort-Object { [int]([regex]::Match($_.Name, '(\d+)').Groups[1].Value) } -Descending |
            ForEach-Object { $candidates.Add($_.FullName) }
    }

    foreach ($candidate in $candidates) {
        $javac = Join-Path $candidate 'bin\javac.exe'
        if (Test-Path $javac) { return (Resolve-Path $candidate).Path }
    }
    throw 'Не найден JDK. Укажите его через -JdkHome или переменную JAVA_HOME.'
}

$jdk = Resolve-JdkHome -Explicit $JdkHome
$javac = Join-Path $jdk 'bin\javac.exe'
$jar = Join-Path $jdk 'bin\jar.exe'
$jpackage = Join-Path $jdk 'bin\jpackage.exe'

Write-Host "JDK: $jdk"
& $javac -version

$buildDir = Join-Path $projectRoot 'build'
$classesDir = Join-Path $buildDir 'classes'
$jarFile = Join-Path $buildDir 'egais-writeoff.jar'
# jpackage копирует в приложение всё содержимое --input, поэтому jar кладётся
# в отдельный чистый каталог.
$packageDir = Join-Path $buildDir 'package'
$distDir = Join-Path $projectRoot 'dist'
$appDir = Join-Path $distDir 'EgaisWriteOff'

Write-Step 'Очистка каталогов сборки'
foreach ($dir in @($buildDir, $distDir)) {
    if (Test-Path $dir) { Remove-Item $dir -Recurse -Force }
}
New-Item -ItemType Directory -Force -Path $classesDir | Out-Null

Write-Step 'Компиляция исходников'
$sources = Get-ChildItem -Path (Join-Path $projectRoot 'src\main\java') -Recurse -Filter *.java |
    Select-Object -ExpandProperty FullName
if (-not $sources) { throw 'Не найдены исходные файлы *.java' }

$sourceList = Join-Path $buildDir 'sources.txt'
# UTF-8 без BOM: javac не понимает метку BOM в начале файла со списком источников.
[System.IO.File]::WriteAllLines($sourceList, $sources, (New-Object System.Text.UTF8Encoding($false)))
& $javac --release 11 -encoding UTF-8 -d $classesDir "@$sourceList"
if ($LASTEXITCODE -ne 0) { throw "Компиляция завершилась с ошибкой (код $LASTEXITCODE)." }
Write-Host ("Скомпилировано классов: " + (Get-ChildItem $classesDir -Recurse -Filter *.class).Count)

Write-Step 'Сборка исполняемого jar'
& $jar --create --file $jarFile --main-class app.EgaisWriteOffApp -C $classesDir .
if ($LASTEXITCODE -ne 0) { throw "Сборка jar завершилась с ошибкой (код $LASTEXITCODE)." }
Write-Host "Готово: $jarFile"

New-Item -ItemType Directory -Force -Path $packageDir | Out-Null
Copy-Item $jarFile $packageDir -Force

if ($SkipPackage) {
    Write-Host ''
    Write-Host 'Создание exe пропущено (-SkipPackage).' -ForegroundColor Yellow
    return
}

if (-not (Test-Path $jpackage)) {
    Write-Warning "В JDK нет jpackage (нужен JDK 14+), поэтому exe не создаётся."
    Write-Host "Исполняемый jar: $jarFile" -ForegroundColor Yellow
    return
}

Write-Step 'Создание приложения Windows (jpackage)'
# jpackage требует, чтобы каталог назначения уже существовал.
New-Item -ItemType Directory -Force -Path $distDir | Out-Null
$iconFile = Join-Path $projectRoot 'src\main\resources\egais-writeoff.ico'
$jpackageArgs = @(
    '--type', 'app-image',
    '--name', 'EgaisWriteOff',
    '--app-version', $AppVersion,
    '--description', 'Формирование актов списания и документа переноса остатков в магазин ЕГАИС',
    '--vendor', 'egais-v.0.1',
    '--input', $packageDir,
    '--main-jar', 'egais-writeoff.jar',
    '--main-class', 'app.EgaisWriteOffApp',
    '--dest', $distDir,
    # Только реально используемые модули: иначе в приложение попадёт весь JDK
    # и каталог вырастет примерно до 130 МБ вместо ~50 МБ.
    '--add-modules', 'java.base,java.desktop,java.xml,java.prefs',
    '--java-options', '-Dfile.encoding=UTF-8'
)
if (Test-Path $iconFile) { $jpackageArgs += @('--icon', $iconFile) }

& $jpackage @jpackageArgs
if ($LASTEXITCODE -ne 0) { throw "jpackage завершился с ошибкой (код $LASTEXITCODE)." }

$exe = Join-Path $appDir 'EgaisWriteOff.exe'
Write-Host ''
if (Test-Path $exe) {
    Write-Host 'Сборка завершена.' -ForegroundColor Green
    Write-Host "Программа: $exe"
    Write-Host "Jar:       $jarFile"
} else {
    Write-Warning "jpackage не создал $exe"
}
