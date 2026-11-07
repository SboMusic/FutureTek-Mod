@echo off
chcp 65001 >nul
title FutureTek - Installazione Java 21
echo ============================================================
echo  Installazione di Java 21 (Eclipse Temurin) tramite winget
echo ============================================================
echo.
echo Se Windows chiede l'autorizzazione dell'amministratore, clicca "Si".
echo Se winget chiede di accettare i termini, leggi e rispondi tu.
echo.
winget install --id EclipseAdoptium.Temurin.21.JDK -e
echo.
echo Fatto. Chiudi questa finestra e scrivi a Claude "Java installato".
pause
