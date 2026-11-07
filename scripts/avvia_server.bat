@echo off
chcp 65001 >nul
title FutureTek - Server di sviluppo
cd /d "%~dp0.."
call scripts\trova_java.bat || exit /b 1
if not exist build-logs mkdir build-logs

rem Il server richiede di accettare la licenza (EULA) di Minecraft: la decisione e' tua.
if not exist runs\server mkdir runs\server
findstr /c:"eula=true" runs\server\eula.txt >nul 2>&1
if errorlevel 1 (
    echo Per avviare un server bisogna accettare l'EULA di Minecraft:
    echo https://aka.ms/MinecraftEULA
    set /p RISPOSTA="Accetti l'EULA di Minecraft? (s/n): "
    call :controlla
)
echo.
echo Avvio del server... quando compare "Done" e' pronto.
echo Per spegnerlo scrivi:  stop   e premi Invio.
echo.
powershell -NoProfile -Command "& .\gradlew.bat runServer --console=plain 2>&1 | Tee-Object -FilePath build-logs\server.log"
pause
goto :eof

:controlla
if /i "%RISPOSTA%"=="s" (
    echo eula=true> runs\server\eula.txt
    echo EULA accettata.
) else (
    echo Senza EULA il server non puo' partire.
    pause
    exit
)
goto :eof
