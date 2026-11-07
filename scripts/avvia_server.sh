#!/usr/bin/env bash
# FutureTek - avvia un server di sviluppo con la mod. Serve Java 21.
cd "$(dirname "$0")/.." || exit 1
mkdir -p build-logs runs/server
if ! grep -q "eula=true" runs/server/eula.txt 2>/dev/null; then
    echo "Per avviare un server bisogna accettare l'EULA di Minecraft: https://aka.ms/MinecraftEULA"
    read -r -p "Accetti l'EULA di Minecraft? (s/n): " R
    if [ "$R" = "s" ] || [ "$R" = "S" ]; then echo "eula=true" > runs/server/eula.txt; else echo "Senza EULA il server non parte."; exit 1; fi
fi
echo "Avvio del server... quando compare \"Done\" e' pronto. Per spegnerlo scrivi: stop"
./gradlew runServer --console=plain 2>&1 | tee build-logs/server.log
