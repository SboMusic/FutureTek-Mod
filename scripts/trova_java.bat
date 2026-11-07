@echo off
rem Trova Java 21 anche se Windows non ha ancora aggiornato JAVA_HOME
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" goto :eof
for /d %%D in ("C:\Program Files\Eclipse Adoptium\jdk-21*") do set "JAVA_HOME=%%D"
if defined JAVA_HOME (
    echo Uso Java in: %JAVA_HOME%
) else (
    echo ATTENZIONE: Java 21 non trovato. Avvia prima scripts\installa_java.bat
    pause
    exit /b 1
)
