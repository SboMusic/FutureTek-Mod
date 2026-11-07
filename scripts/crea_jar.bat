@echo off
chcp 65001 >nul
title FutureTek - Crea il jar
cd /d "%~dp0.."
call scripts\trova_java.bat || exit /b 1
call gradlew.bat build --console=plain
echo.
echo Se e' andato tutto bene il jar della mod e' in build\libs\
pause
