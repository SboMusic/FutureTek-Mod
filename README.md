# FutureTek

**Una mod tecnologica per Minecraft 1.21.1 (NeoForge)** — energia, macchine, trasmutazione, logistica, nucleare,
robot, ferrovie, api ed esplorazione: dalla prima officina fino al reattore.

Ispirata alle *meccaniche* dei grandi modpack tecnologici classici (come Tekkit Legends), ma con **nomi, codice,
texture e regole tutti originali**: niente è copiato da altre mod né da Minecraft.

Autore: **SboMusic** · Versione **1.0.0** · Minecraft **1.21.1** · NeoForge **21.1.265** · Java **21**

> *English summary below.*

---

## Cosa c'è dentro

| Parte | In breve |
|---|---|
| **Materiali** | stagno, piombo, argento, uranio, bronzo, acciaio; albero della resina; 200 blocchi da costruzione (4 pietre, 8 legni completi di porte, botole, pulsanti, pedane) |
| **Energia (TU)** | generatori a carbone, geotermici, solari I-III, a vento, ad acqua, avanzati; cavi con reti; batterie e banchi accumulatori |
| **Macchine** | frantumatore, fornace elettrica, estrattore, compressore, lavatrice di minerali e molte altre, con potenziamenti |
| **Essenza** | trasmutazione: ogni oggetto ha un valore, si trasforma e si replica; anelli, amuleti, attrezzi e armature alchemici |
| **Logistica** | condotti, estrattori, smistatori, terminale, crafting automatico a catena, porte logiche |
| **Alta tecnologia** | reattore a fissione a camere (può esplodere!), TNT nucleare, escavatore, esoarmatura, zaino a reazione |
| **Automazione** | robot autonomi (contadino, guardia, sentinella, minatore, taglialegna) con pannello, talpa mineraria, casa automatica |
| **Nether** | incantatrice elettrica con ricerca degli incantesimi, incubatore di creature e torretta laser |
| **Ferrovie e api** | binari speciali e carrello merci; 10 specie di api con incroci |
| **Esplorazione** | i **vulcani** di roccia vulcanica, la dimensione *Margine Cristallino*, la fabbrica abbandonata nei mondi nuovi, il **rifugio portatile** |

Guide illustrate (si aprono nel browser): [`docs/guida_craft.html`](docs/guida_craft.html) (tutte le ricette),
[`docs/guida_automazione.html`](docs/guida_automazione.html), [`docs/guida_logistica.html`](docs/guida_logistica.html).

---

## Giocare

### Solo giocare (senza programmare)
1. Installa **NeoForge 21.1.265** per Minecraft 1.21.1 (installer da <https://neoforged.net>).
2. Scarica `futuretek-1.0.0.jar` dalla pagina [**Releases**](https://github.com/SboMusic/FutureTek-Mod/releases) di questo repository.
3. Mettilo nella cartella `mods` di Minecraft e avvia il profilo NeoForge.

Il server dedicato funziona allo stesso modo: installa il server NeoForge e metti lo stesso jar nella sua cartella `mods`.
Tutti i dettagli (anche la configurazione) sono in [`docs/GIOCARE_E_SERVER.md`](docs/GIOCARE_E_SERVER.md).

### Dal codice (launcher inclusi)
Serve **Java 21** (su Windows: `scripts\installa_java.bat`).

| Cosa | Windows | Linux / Mac |
|---|---|---|
| Avviare Minecraft con la mod | `scripts\avvia_client.bat` | `scripts/avvia_client.sh` |
| Avviare un server con la mod | `scripts\avvia_server.bat` | `scripts/avvia_server.sh` |
| Creare il file `.jar` | `scripts\crea_jar.bat` | `scripts/crea_jar.sh` |

La prima volta Gradle scarica Minecraft e NeoForge: ci vogliono 10-20 minuti.

---

## Documentazione

| File | Cosa contiene |
|---|---|
| [`docs/COME_E_STATA_FATTA.md`](docs/COME_E_STATA_FATTA.md) | come è nata la mod, le regole, gli strumenti e il metodo di lavoro |
| [`docs/COME_MODIFICARE.md`](docs/COME_MODIFICARE.md) | come cambiare o aggiungere cose: codice, texture, ricette, traduzioni, test |
| [`docs/GIOCARE_E_SERVER.md`](docs/GIOCARE_E_SERVER.md) | installazione per giocatori e server, configurazione, regole di gioco |
| [`DESIGN.md`](DESIGN.md) | il progetto completo: tutte le meccaniche e i numeri |
| [`PROGRESS.md`](PROGRESS.md) | il diario delle fasi di sviluppo |
| [`TEST_IN_GIOCO.md`](TEST_IN_GIOCO.md) | la lista delle prove da fare in gioco |
| [`CHANGELOG.md`](CHANGELOG.md) | le versioni |

---

## English summary

**FutureTek** is an original tech mod for **Minecraft 1.21.1 / NeoForge 21.1.265** inspired by the gameplay of classic
tech modpacks (Tekkit Legends style): energy networks, processing machines, item transmutation, logistics, a chamber
fission reactor that can melt down, nuclear TNT, autonomous robots, a mob farm, enchantment research, railways, bees,
a new dimension and a portable shelter. All code, names and textures are original. The in-game text is available in
Italian and English.

- **Play:** install NeoForge 21.1.265, drop `futuretek-1.0.0.jar` (see *Releases*) into `mods`.
- **Build from source:** Java 21, then `./gradlew build` (jar in `build/libs`) or `./gradlew runClient` / `runServer`.
- Docs are in Italian (`docs/`).

## Licenza / License

**MIT** — puoi usare, modificare e ridistribuire la mod citando l'autore (SboMusic). Testo completo in [`LICENSE`](LICENSE).
Minecraft è un marchio di Mojang/Microsoft; questa mod non è affiliata né approvata da loro.
