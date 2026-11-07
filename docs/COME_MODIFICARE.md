# Come modificare FutureTek

Guida pratica per cambiare o aggiungere qualcosa alla mod. Non serve essere esperti: basta seguire i passi.

## 1. Preparare il computer
1. **Java 21** (JDK). Su Windows c'è `scripts\installa_java.bat` (usa winget per installare Eclipse Temurin 21).
2. **Git**, per scaricare il progetto e salvare le modifiche.
3. **Python 3** con **Pillow** (`pip install pillow`): serve solo se vuoi rigenerare risorse, texture o guide.
4. Un editor di testo. Consigliati IntelliJ IDEA Community o VS Code (con le estensioni Java), ma non sono obbligatori.

```bash
git clone https://github.com/SboMusic/FutureTek-Mod.git
cd FutureTek-Mod
```

## 2. Provare la mod
| Cosa | Windows | Linux / Mac |
|---|---|---|
| Minecraft con la mod | `scripts\avvia_client.bat` | `scripts/avvia_client.sh` |
| Server con la mod | `scripts\avvia_server.bat` | `scripts/avvia_server.sh` |
| Tutti i test automatici | `gradlew.bat runGameTestServer` | `./gradlew runGameTestServer` |
| Creare il jar | `scripts\crea_jar.bat` | `scripts/crea_jar.sh` |

- Il mondo del client di prova è in `run/`, quello del server in `runs/server/`.
- I registri finiscono in `build-logs/` (`client.log`, `server.log`). Se qualcosa si rompe, cerca `ERROR` o `Exception`.
- Il server dedicato chiede la prima volta di accettare l'EULA di Minecraft.

## 3. Dove sono le cose
| Voglio cambiare… | File |
|---|---|
| un numero (costo, raggio, velocità…) | prima guarda `FutureTekConfig.java`: se è lì basta modificare `config/futuretek-common.toml`, senza ricompilare |
| il comportamento di un blocco o di una macchina | `src/main/java/com/futuretek/<argomento>/…` |
| la registrazione di blocchi e oggetti | `registry/ModBlocks.java`, `ModItems.java`, `ModBlockEntities.java` |
| ricette, nomi, descrizioni, tag, loot | `tools/genera_risorse.py` (poi rigenera, vedi sotto) |
| una texture | lo script `tools/grafica*.py` che la disegna (oppure il PNG in `src/main/resources/assets/futuretek/textures`) |
| i blocchi da costruzione | `tools/blocchi_costruzione.json` → `python tools/genera_java_costruzione.py` |
| i valori dell'Essenza, gli incroci delle api | i JSON prodotti da `genera_risorse.py` in `src/generated/resources/data/futuretek/` |

⚠️ **Non modificare a mano i file in `src/generated/resources`**: vengono ricreati da zero ogni volta che lanci
`genera_risorse.py`. Le modifiche vanno fatte nello script.

## 4. Rigenerare risorse, texture e guide
Dalla cartella del progetto:

```bash
python tools/genera_risorse.py                  # modelli, ricette, tag, loot, traduzioni (le texture esistenti restano)
python tools/genera_risorse.py --forza-texture  # come sopra, ma ridisegna TUTTE le texture
python tools/controlla_risorse.py               # controlla che a ogni blocco/oggetto non manchino modello, texture o traduzioni
python tools/genera_guida.py                    # docs/guida_craft.html (ricettario)
python tools/genera_guida_automazione.py        # docs/guida_automazione.html
python tools/genera_guida_logistica.py          # docs/guida_logistica.html
```

## 5. Esempio: aggiungere un blocco semplice
1. In `registry/ModBlocks.java` aggiungi una riga come quelle già presenti, per esempio:
   ```java
   public static final DeferredBlock<Block> MIO_BLOCCO = registerWithItem("mio_blocco", Block::new,
           BlockBehaviour.Properties.of().strength(2.0F).requiresCorrectToolForDrops());
   ```
2. In `tools/genera_risorse.py`, in fondo alle fasi, aggiungi:
   ```python
   blocco("mio_blocco", "My Block", "Il mio blocco")      # modello, oggetto, loot e nomi EN/IT
   tag(tag_blocchi, "minecraft:mineable/pickaxe", ft("mio_blocco"))
   r_con_forma("mio_blocco", ["##", "##"], {"#": "minecraft:stone"}, ft("mio_blocco"))   # ricetta
   ```
3. Metti la texture `src/main/resources/assets/futuretek/textures/block/mio_blocco.png` (16x16), oppure disegnala in
   uno degli script `tools/grafica*.py`.
4. Lancia `python tools/genera_risorse.py` e poi `python tools/controlla_risorse.py`.
5. Prova con `scripts/avvia_client`. Il blocco è nella scheda creativa FutureTek.

## 6. Aggiungere un test automatico
I test sono in `src/main/java/com/futuretek/gametest/`. Ogni test è un metodo `@GameTest`: costruisce una piccola
scena, aspetta e controlla il risultato. Guarda quelli esistenti, per esempio `Phase15GameTests.shelterBuildsAndDisappears`.
I test che fanno esplosioni o costruiscono cose grandi usano un `batch` separato, così non disturbano gli altri.
Prima di pubblicare, `runGameTestServer` deve finire con *All required tests passed*.

## 7. Le regole del progetto
- **Originalità**: nessun nome, codice, texture o suono copiato da altre mod o da Minecraft. Ispirarsi alle
  meccaniche va bene.
- **Multiplayer**: la logica gira sul server (`if (level.isClientSide) return;`). Il client mostra soltanto.
- **Numeri nella configurazione o nei JSON**, non scritti fissi nel codice quando ha senso cambiarli.
- **Testi in due lingue**: ogni nome o messaggio ha la versione inglese e quella italiana (in `genera_risorse.py`).
- Aggiorna `DESIGN.md`, `PROGRESS.md` e `TEST_IN_GIOCO.md` quando aggiungi qualcosa, e fai un commit per ogni passo.

## 8. Pubblicare una nuova versione
1. Cambia `mod_version` in `gradle.properties` (per esempio `1.1.0`) e scrivi le novità in `CHANGELOG.md`.
2. Fai girare tutti i test (`runGameTestServer`).
3. Crea il jar (`scripts/crea_jar`): lo trovi in `build/libs/futuretek-<versione>.jar`.
4. Fai commit e push, poi su GitHub crea una **Release** con il tag `v<versione>` e allega il jar.
