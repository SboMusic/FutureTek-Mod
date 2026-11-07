#!/usr/bin/env bash
# FutureTek - avvia Minecraft (client di sviluppo) con la mod. Serve Java 21.
cd "$(dirname "$0")/.." || exit 1
mkdir -p build-logs
echo "Avvio di Minecraft con FutureTek..."
echo "La PRIMA volta scarica Gradle, NeoForge e Minecraft: puo' durare 10-20 minuti."
./gradlew runClient --console=plain 2>&1 | tee build-logs/client.log
