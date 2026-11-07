#!/usr/bin/env bash
# FutureTek - crea il file .jar della mod (da mettere nella cartella mods) in build/libs
cd "$(dirname "$0")/.." || exit 1
./gradlew build --console=plain && echo "Fatto: il jar e' in build/libs/"
