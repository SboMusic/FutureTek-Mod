# Giocare con FutureTek (client e server)

## Giocatore
1. Avvia una volta Minecraft **1.21.1** dal launcher ufficiale.
2. Scarica l'installer di **NeoForge 21.1.265** (o successivo della serie 21.1) da <https://neoforged.net>, aprilo e
   scegli **Install client**.
3. Scarica `futuretek-1.0.0.jar` dalla pagina **Releases** del repository.
4. Apri la cartella di Minecraft (Windows: `%APPDATA%\.minecraft`, Mac: `~/Library/Application Support/minecraft`,
   Linux: `~/.minecraft`) e metti il jar nella cartella `mods` (creala se non c'è).
5. Nel launcher scegli il profilo **NeoForge** e premi Gioca.

Consigliata, ma non obbligatoria: la mod **JEI** (Just Enough Items) per vedere le ricette in gioco. Le ricette sono
anche nella guida [`guida_craft.html`](guida_craft.html).

## Server dedicato
1. Serve **Java 21**.
2. Con l'installer di NeoForge scegli **Install server** in una cartella vuota.
3. Metti `futuretek-1.0.0.jar` nella cartella `mods` del server. **Anche ogni giocatore deve avere la mod.**
4. Avvia con `run.bat` (Windows) o `run.sh` (Linux/Mac). La prima volta accetta l'EULA in `eula.txt`
   (<https://aka.ms/MinecraftEULA>).
5. La memoria si regola in `user_jvm_args.txt`, per esempio `-Xmx4G`.

Per provare la mod direttamente dal codice ci sono anche `scripts/avvia_server.bat` / `scripts/avvia_server.sh`.

## Configurazione
Il file `config/futuretek-common.toml` (sul server, o nella cartella di Minecraft in singolo giocatore) viene creato
al primo avvio. Le voci principali:

| Voce | Predefinito | Cosa fa |
|---|---|---|
| `solarRate`, `combustionRate`, `geothermalRate` | | produzione dei generatori (TU/tick) |
| `nukesEnabled` | true | la TNT nucleare esplode (false = si spegne) |
| `nukeRadius` | 32 | raggio del cratere della TNT nucleare |
| `reactorExplosions` | true | il reattore surriscaldato esplode (false = spegnimento d'emergenza) |
| `reactorBlastRadius` | 24 | raggio massimo dell'esplosione del reattore |
| `shelterEnergyPerUse` / `shelterUses` | 25000 / 6 | energia per ogni casa del rifugio portatile e numero di case |
| `quarrySize`, `quarryEnergyPerBlock` | | area e consumo dell'escavatore |
| `treeFelling`, `treeFellingMaxLogs` | | abbattimento di alberi interi con l'ascia elettrica |

## Regole di gioco
- `futuretekAbandonedFactory` (predefinita: attiva): nei mondi nuovi compare una fabbrica abbandonata vicino al punto
  di nascita. Si cambia in *Altre opzioni del mondo → Regole di gioco*, oppure con
  `/gamerule futuretekAbandonedFactory false`.
- Il comando `/futuretek fabbrica` (operatori) costruisce una fabbrica davanti a te.

## Lingue
Inglese e italiano (si sceglie nelle opzioni di Minecraft).
