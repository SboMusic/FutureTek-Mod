@echo off
chcp 65001 >nul
title FutureTek - Client di sviluppo
cd /d "%~dp0.."
call scripts\trova_java.bat || exit /b 1
if not exist build-logs mkdir build-logs
echo Avvio di Minecraft con FutureTek...
echo La PRIMA volta scarica Gradle, NeoForge e Minecraft: puo' durare 10-20 minuti.
echo L'output viene salvato anche in build-logs\client.log (Claude lo legge da li').
echo.
powershell -NoProfile -Command "& .\gradlew.bat runClient --console=plain 2>&1 | Tee-Object -FilePath build-logs\client.log"
echo.
echo Minecraft e' stato chiuso.
pause
