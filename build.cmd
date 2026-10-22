@echo off
rem Build script wrapper for the EGAIS write-off acts generator.
rem It exists because many machines block execution of .ps1 files by policy.
rem All arguments are forwarded to build.ps1, for example: build.cmd -SkipPackage
rem This file is intentionally ASCII-only: cmd.exe decodes .cmd files using the
rem OEM code page, so non-ASCII text here would be garbled.

setlocal
set "SCRIPT_DIR=%~dp0"

where pwsh >nul 2>nul
if %ERRORLEVEL%==0 (
    pwsh -NoProfile -NoLogo -ExecutionPolicy Bypass -File "%SCRIPT_DIR%build.ps1" %*
) else (
    powershell -NoProfile -NoLogo -ExecutionPolicy Bypass -File "%SCRIPT_DIR%build.ps1" %*
)

set "RESULT=%ERRORLEVEL%"
if not "%RESULT%"=="0" (
    echo.
    echo BUILD FAILED. Exit code: %RESULT%
)
endlocal & exit /b %RESULT%
