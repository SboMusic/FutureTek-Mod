# FutureTek – Stato dei lavori

> Da incollare all'inizio di una nuova conversazione insieme a `DESIGN.md`.

## Riepilogo
- **Mod**: FutureTek (`futuretek`), pacchetto `com.futuretek`
- **Versioni**: Minecraft 1.21.1 · NeoForge 21.1.265 · ModDevGradle 2.0.148 · Gradle 9.2.1 · Java 21
- **Cartella del progetto**: la cartella dove hai scaricato/clonato il repository
- **Sistema**: sviluppata su Windows (funziona anche su Linux/Mac con gli script `.sh`) · codice su GitHub
- **Fase attuale**: **versione 1.0.0 completa** (fasi 0-17) · prove in gioco in `TEST_IN_GIOCO.md`

## Fatto
### Fase 0 – Preparazione ✅
- Progetto dal template ufficiale NeoForge 1.21.1 (ModDevGradle), classi `FutureTek`, `FutureTekClient`, `FutureTekConfig`
- Logo segnaposto, `DESIGN.md`, Git locale
- Avvio verificato: client e server dedicato (giocatore connesso via localhost)
- Collegamenti sul Desktop (facoltativi): "FutureTek - Avvia" (installa Java se manca + runClient) e "FutureTek - Server" (EULA + runServer)

### Fase 1 – Base ✅ (provata in gioco dall'utente: tutto ok, texture da migliorare in futuro)
- **Tappa 1 – Metalli**: stagno, piombo, argento, uranio, bronzo, acciaio → grezzo, lingotto, pepita, polvere, piastra,
  blocco, blocco di grezzo; polveri di ferro/oro/rame/carbone/diamante/ossidiana; piastre di ferro/oro/rame;
  leghe (bronzo, acciaio); martello da forgia; scheda creativa "FutureTek - Risorse"
- **Tappa 2 – Minerali**: 5 minerali + varianti deepslate (stagno, piombo, argento, uranio, voltite), frammento di voltite,
  generazione nell'Overworld (misurata prima dell'ultimo ritocco: stagno ~30/chunk, piombo ~25, uranio ~3;
  argento e voltite poi ridotti)
- **Tappa 3 – Albero e strumenti**: tronco (con vena di resina che si riforma), assi, foglie, alberello, albero nei
  biomi foresta e giungla; rubinetto, resina grezza, gomma; strumenti e armature di bronzo e acciaio

### Fase 2 – Energia ✅ (provata in gioco dall'utente: funziona)
- Sistema TU (= FE) con capability NeoForge standard; livelli BT 32 / MT 128 / AT 512 / ET 2048 TU/t
- Generatori: a combustione (GUI, 20 TU/t), pannello solare (4 TU/t di giorno), geotermico (lava, GUI, 24 TU/t)
- Banchi accumulatori I/II/III (40k / 600k / 10M TU): ingresso da 5 lati, uscita dal fronte, slot carica/scarica
- Cavi: stagno, rame isolato, oro isolato, fibra di voltite; reti automatiche (`energy/network`) con limite di portata
- Celle portatili (10k, 100k), tagliacavi + fili, circuito di base, telaio macchina, chiave inglese, multimetro
- Interfacce disegnate via codice (`client/screen/MachineScreen`), nessuna immagine necessaria
- Valori dei generatori in config (`config/futuretek-common.toml`, sezione generators)
- Test automatici: `gametest/EnergyGameTests` (6 test, tutti superati)
- Totale dopo la Fase 2: 69 oggetti, 35 blocchi, 128 ricette

### Fase 3a – Macchine di lavorazione
- Tipo di ricetta generico `recipe/ProcessingRecipe` (+ `ProcessingKind`): crushing, compressing, extracting, wire_pressing
- Un solo "cervello" per tutte le macchine: `blockentity/ProcessingMachineBlockEntity` (macchine in `machine/ProcessingMachine`)
- Frantumatore (raddoppio), forno elettrico (ricette fornace), compressore, estrattore, pressa per fili
- 4 potenziamenti: overclock, efficienza, accumulo, espulsione
- 74 ricette JSON delle macchine (in `src/generated/resources/data/futuretek/recipe/<tipo>/`)
- Test automatici: `gametest/MachineGameTests` (7) + energia (6) = 13, tutti superati
- JEI aggiunto a `build.gradle` (versione `19.+`): si scarica al prossimo avvio dell'utente
- Totale mod: **73 oggetti, 40 blocchi, 202 ricette**

### Fase 3b – Triplicazione ✅ (test automatici)
- Bagno chimico (acqua) + centrifuga termica, frantumati puri; generazione dello stagno corretta (~42/chunk)

### Fase 4 – Trasmutazione ✅ (test automatici)
- `essence/`: valori JSON (`essence_values/base.json`, scala originale) + calcolo dalle ricette (965 oggetti)
- Dati del giocatore (attachment sincronizzato), Tavolo di Sintesi, Pietra del Mutamento, Cristalli I-III,
  Captatori Stellari I-III, Replicatore

### Fase 5 – Logistica ✅ (test automatici)
- `logistics/`: rete di condotti, estrattori (normale/veloce), smistatore con filtro, smistamento automatico,
  Terminale di rete (pacchetti client/server), Banco di assemblaggio automatico + `CraftingPlanner` (richieste a catena),
  8 porte logiche

### Fase 6 – Tecnologia avanzata ✅ (test automatici)
- Reattore a fissione (griglia 9x3, calore, camere; config `reactorExplosions`=false), escavatore (config area/costo),
  esoarmatura (poteri in `item/ArmorAbilities`), zaino a reazione (spinta lato client + pacchetto)

### Fase 7 – Ferrovie ✅ (test automatici)
- Binari rinforzato, a induzione (energia), di blocco, di carico, di scarico; carrello merci 54 slot (entità)

### Fase 8 – Agricoltura e api ✅ (test automatici)
- Acceleratore di crescita, raccoglitore automatico, fertilizzante; 10 specie di api, arnia tecnica,
  prodotti e incroci in `bee_data/bees.json`, alveari selvatici (misurati nel mondo), favi → centrifuga

### Fase 9 – Il Margine Cristallino ✅ (verificato sul server di prova)
- Dimensione `futuretek:crystal_edge` (JSON: dimension_type, noise_settings, biome), Ancora del Margine
- Nota: il server dei test automatici usa un mondo piatto senza dimensioni extra → la dimensione si verifica
  sul server normale con `execute in futuretek:crystal_edge ...`

### Fase 4b – Alchimia avanzata ✅ (test automatici) – richiesta dell'utente dopo le prove
- Nuova scala dell'Essenza scelta dall'utente: pietra 1, ferro 256 (tutto il resto raddoppiato, tronco 32)
- Calcolo dai valori: anche fornaci, tagliapietre, forgiatura; sezione `copies`; usura, incantesimi, Essenza nei cristalli
- TUTTI gli oggetti ottenibili hanno un valore (test `everyObtainableItemHasValue`); comandi `/futuretek essenza ...`
- Tavolo di Sintesi con ricerca (pacchetto `SynthesisRequestPayload`), Tavoletta portatile, banco portatile della Pietra
- Pacchetto `alchemy/`: braci, Nocturnite/Ardorite + attrezzi/magli/armature, scavo ad area, Catalizzatore, Lente,
  Carica Stellare, Bisaccia (attachment), 5 anelli, Clessidra, amuleti, Gemma, bacchette, Talismano, Tomo, Lanterna
- Anti-duplicazione: grezzi, minerali, polveri, frantumati, favi e api si sciolgono ma non si creano
- Corretto: ricetta del Captatore Stellare (usava un tag inesistente → non era fabbricabile)
- JEI: plugin `compat/jei/FutureTekJeiPlugin` (6 schede delle macchine + forno elettrico); versione JEI fissata 19.70.0.469

### Grafica v2 ✅ (richiesta dell'utente prima dei test)
- `tools/grafica.py`: 162 texture originali ridisegnate in stile Minecraft moderno: attrezzi e armi in diagonale
  (bronzo, acciaio, Nocturnite, Ardorite, magli), armature, lingotti/pepite/piastre/polveri/grezzi, minerali
  (pietra a macchie morbide), blocchi di metallo, tutte le macchine (involucro d'acciaio con cornice e bulloni,
  pannello frontale incassato, versione accesa), banchi accumulatori, pannello solare, reattore, cavi, condotti,
  tavolo di sintesi, captatori, replicatore, carica stellare
- `genera_risorse.py --forza-texture` richiama anche grafica.py

### Guida ai craft ✅
- `tools/genera_guida.py` (+ modello `tools/guida_modello.html`) crea `docs/guida_craft.html`: tutte le 326 ricette
  (banco, fornace, macchine) con icone delle texture della mod, ricerca e collegamenti. Da rigenerare dopo genera_risorse.

### Minerali più frequenti ✅ (richiesta dell'utente dopo le prove, 2 nov 2026)
- Misurati su un mondo nuovo: stagno ~72/chunk, piombo ~45, argento ~20, voltite ~18, uranio ~11
  (prima: 42 / 24 / 8 / 6 / 3; ferro di Minecraft ~67). Valori in MINERALI_MONDO di genera_risorse.py

### Comodità ✅ (richieste dell'utente, 2 nov 2026) – pacchetto `utility/`
- Abbattimento alberi: qualsiasi ascia abbatte l'intero albero (solo con foglie naturali; Shift = un tronco;
  config `gameplay.treeFelling`, max 256 tronchi; si ferma prima di rompere l'ascia)
- Fornace di ferro (8 lingotti di ferro): ricette e interfaccia della fornace, 1,5× più veloce, tramogge/condotti
- Polveri di rilevamento I/II/III (carbone o carbonella + pietra / redstone + ferro / ferro + diamante; verde, blu, ciano)
  e Rilevatori di minerali I/II/III: area 3×3 (16 in giù), 9×9 (32), 16×16 (64); elenco in chat + scintille sul più vicino
- Sacca del minatore (baule + 5 lana + le 3 polveri): 27 slot dentro l'oggetto, raccolta automatica di minerali,
  grezzi, gemme, polveri, carbone (tag `futuretek:miner_bag_collects`)
- Catalizzatore del Vuoto caricabile: click in aria = carica 1→4 (3×3×3 … 9×9×9), Shift = solo lo strato davanti
- Test: `gametest/UtilityGameTests` (6)

### Fase 10 ✅ (richieste dell'utente, 2 nov 2026)
- Bauli `storage/Storage`: rame 45, ferro 54, oro 81, diamante 108, ossidiana 108 (resiste alle esplosioni);
  pulsanti "Deposita" (porta dentro ciò che il baule contiene già) e "Ordina"; tramogge/tubi
- Logistica: Nucleo logistico (`logistics/LogisticsCoreBlockEntity`, energia 2 TU/t + 2 TU/oggetto): il baule che lo tocca
  è il BAULE PRINCIPALE (default di tutto ciò che non ha destinazione) e il nucleo smista da lì verso i tubi di
  destinazione/bauli che contengono già l'oggetto; animazione degli oggetti nei tubi (`ItemTransit` + `client/TransitRenderer`);
  tubi rinominati (Tubo logistico/estrattore/di destinazione); banco di assemblaggio con pulsanti +1/+16 e SCORTA automatica
- Talpa mineraria `mining/MiningMole`: si muove da sola, scava strati 3×3…17×17 fino in fondo, torna al baule di casa
  quando è piena o a corto di carburante, scarica, prende combustibile dal baule e riparte (provata su mondo vero)
- Escavatore in stile classico: segnalini d'area (rettangolo fino a 64), telaio giallo automatico, testa di perforazione
  che si sposta sopra il blocco scavato, scavo a serpentina; senza segnalini quadrato davanti (config)
- Carbonaia (18+18 slot, 16 tronchi → 16 carbonella ogni 10 s, senza combustibile)
- Generatori: avanzato (64 TU/t), solare II (24) e III (96, di notte 1/8), mulino ad acqua (2 TU/t per lato bagnato),
  mulino a vento (da y 64, più in alto = di più, bloccato dai blocchi vicini, ×1,5 pioggia ×2 temporale); circuito avanzato
- Macchine avanzate: frantumatore avanzato e forno a induzione (4× più veloci); JEI mostra tutte le macchine
- Grafica: lato di ogni macchina con fascia colorata e simbolo; minerali con forme diverse (scaglie, blocchi, tondi
  luminosi, cristalli), uranio più brillante; texture di tutti i blocchi nuovi
- Test: `gametest/Phase10GameTests` (6) + escavatore con segnalini

### Totale: 160 oggetti, 104 blocchi, 368 ricette, 59 test automatici (tutti superati), 433 modelli controllati

## Come lavoriamo (tecnica)
- Il codice e le risorse li scrive Claude. Le risorse JSON e le texture si rigenerano con `python tools/genera_risorse.py`
  (le texture esistenti NON vengono sovrascritte; `src/generated/resources` viene ricreata ogni volta).
- Claude compila (javac) e avvia un server di prova; i test automatici partono con `-Dneoforge.gameTestServer=true`.
- Strumenti: `tools/genera_risorse.py` (risorse), `tools/controlla_risorse.py` (modelli/texture/traduzioni)
- Claude compila e avvia un server di prova nel suo spazio di lavoro usando i file di Minecraft/NeoForge copiati dalla
  cache Gradle dell'utente (`build/claude-cp.tar`): verifica registrazioni, ricette, tag, loot e generazione del mondo.
- Poi copia il progetto sul PC (`build/futuretek-sync.tar` + `build/applica_sync.sh`) e fa il commit Git.
- L'utente prova in gioco con le icone sul Desktop. I log finiscono in `build-logs/` e Claude li legge da lì.

## Da fare (prossimo passo)
- L'utente prova tutto con `TEST_IN_GIOCO.md`; Claude corregge cio' che non va (registro in `build-logs/client.log`)
- Fase 10 (rifinitura): texture vere, bilanciamento, contenuti rimandati (vedi tabella in DESIGN.md), file .jar per pubblicare

- Decisione dell'utente (1 nov 2026): **si resta su Minecraft 1.21.1** (non si porta alla 26.3).
- Il "Catalogo ricercabile di Tekkit Legends" dell'utente si usa come LISTA DI CONTROLLO delle meccaniche mancanti
  (115 famiglie): si prendono le meccaniche, non nomi e ricette originali. Candidati: forno a induzione, riciclatore e
  materia UU, mulini a vento/acqua, motori e petrolio, segnali ferroviari, computer/tartarughe, strutture mobili,
  bauli migliorati, relè/forziere alchemico.

## Problemi aperti / da ricordare
- Non testabili sul server di prova (servono un giocatore e lo schermo): interfacce grafiche, rubinetto, zaino a reazione,
  poteri dell'esoarmatura, Ancora del Margine, colori delle api, modelli dei binari/condotti
- Rimandati: altoforno FutureTek (si usa quello di Minecraft), polveri di diamante/ossidiana (Fase 3),
  trasformatori + sovratensione, perdita sui cavi, scossa dei cavi nudi
- Smontando una macchina con la chiave inglese l'energia contenuta va persa (da migliorare)

## Note
- Lezione: nei file in `src/main/templates` non usare mai il simbolo dollaro nei commenti.
- IntelliJ non installato (per ora non serve).

## Correzioni dopo i test (Fase 10b)
- Bauli: al massimo 6 righe (oro 14×6, diamante/ossidiana 18×6) così l'inventario del giocatore resta visibile.
- Escavatore: costo predefinito 64 TU per blocco (prima 160); click destro mostra lo stato (energia, baule pieno, redstone, strato).
- Tooltip descrittivi su tutti i blocchi della mod (bagno chimico, centrifuga, escavatore, ...).
- Nuova guida illustrata `docs/guida_logistica.html` (generata da `tools/genera_guida_logistica.py`).

## Fase 10b (seconda parte, dopo gli appunti del 3/11)
- Talpa: l'area parte dalla talpa e si allarga dalla parte opposta al baule (raggio 1 = 3x3), dal suo stesso livello.
- Escavatore: telaio a barre sottili collegate (anello basso, 4 colonne, anello alto a +4); carrello, asta e trivella disegnati dal client (`client/QuarryRenderer`); telaio rimosso quando si rompe l'escavatore; senza posto sputa gli oggetti dall'alto (stato "ejecting"); velocità proporzionale all'energia che entra (riserva 4.000 TU, fino a `quarryMaxBlocksPerTick` = 8 blocchi/tick); uscita preferita: sopra.
- Nuovo **Tubo analizzatore** (`logistics/AnalyzerConduitBlockEntity`): impara e ricorda gli oggetti del baule che tocca, filtro extra, "Lascia 1" (terminale e crafting non prendono l'ultimo), "Dimentica". Un contenitore toccato da più tubi conta una volta sola (vince il tubo speciale).
- Generatori ribilanciati: combustione 12, geotermico 16, mulino ad acqua 1 per lato (max 6), mulino a vento max 8, solare 4, solare II 32, solare III 128, generatore avanzato 64.
- Test automatici: **62** (nuovi: quarryEjectsWithoutChest, quarryFrameRemovedWithQuarry, analyzerLearnsAndKeepsOne).

## Fase 11 — Utensili, armi ed esoarmatura elettrici (fatto)
- `item/ElectricTools.java`: **Trapano elettrico** (piccone + pala, livello ferro, 50 TU/blocco, 40k TU), **Trapano di diamante** (livello diamante, 80 TU/blocco, 200k TU, Shift + click destro = 3x3), **Motosega** (ascia veloce, foglie/lana istantanee, abbatte alberi interi), **Lama al plasma** (13 di danno, 250 TU a colpo; scarica 3), **Fucile a impulsi** (raggio fino a 32 blocchi, 8 di danno, 400 TU, ricarica 0,5 s).
- Scarichi: velocità 1 e niente minerali; non si consumano mai; si ricaricano nel banco accumulatore.
- Esoarmatura a 3 livelli: **I** (ferro, 300k TU, scudo -15%, Velocità I, Salto I, metà danni da caduta), **II** (quella di prima, diamante, 1M TU, -30%), **III** (netherite, 3M TU, -45%, resistenza al fuoco, respirazione, sazietà, Velocità III, Salto III). La III si fa dalla II + lingotto di netherite.
- Tag: `futuretek:mineable/drill` (piccone + pala), `futuretek:electric_tools`; incantabili come attrezzi/spade.
- Test automatici: **65** (nuovi: drillsNeedEnergy, plasmaSaberUsesEnergy, exoArmorTiers).

## Fasi 12-14 — Casa automatica, Nether, robot (fatto)
- Base comune `automation/SlotMachines` (blocco + entità + menu + `client/screen/SlotMachineScreen`): fino a 16 caselle, barra energia, riga di stato e fino a 3 pulsanti; testi `gui.futuretek.<id>.statusN / buttonN`.
- **Fase 12** (`automation/HomeMachines`): Fattoria di alberi (pianta ogni 3 blocchi in 9x9, taglia solo tronchi con foglie naturali vicine, legno nel baule accanto), Piantatrice (zappa e pianta semi/gambi), Fertilizzatore automatico (farina d'ossa, 60 TU), Lampada elettrica e Pannello luminoso (2 TU/t, si passano l'energia fra loro), Interruttore elettrico (energia esce dal davanti solo con redstone).
- **Fase 13** (`automation/NetherMachines`): Nucleo d'anima (materiali del Nether), Campionatore di creature, Incubatore di creature (6000 TU a mob, min 2 s, max 6 vicini), Torretta laser (8 danni, 150 TU, giocatore finto → drop ed esperienza; solo mostri / tutti), Incantatore elettrico (gradi I/II/III = potenza 10/20/30 con 2/5/9 livelli, 1/2/3 lapislazzuli, 25k/75k/200k TU).
- **Fase 14** (`robot/`): RobotEntity (contadino, guardia del corpo, sentinella, minatore; 200k TU, 4 TU/t, torna a ricaricarsi sotto il 20%), Base di ricarica, Telaio di robot, Robot (oggetto, conserva energia e lavoro), Nucleo infinito. Render `client/RobotRenderer` (modello umanoide, texture originali per lavoro).
- Nuova guida `docs/guida_automazione.html` (`tools/genera_guida_automazione.py`).
- Totali: 178 oggetti, 115 blocchi, 397 ricette. Test automatici: **74**.

## Fase 14b — Robot autonomi (fatto)
- `robot/RobotEntity` riscritto con un "cervello" unico (`BrainGoal` → `think()`): ricarica → attesa (baule pieno / fine / niente baule) → scarico → attrezzo → lavoro.
- Lavori: Contadino (zappa), Guardia, Sentinella, **Minatore** (piccone: scala alta 3 fino a sopra la bedrock, poi gallerie a serpentina 32/4 per 31 segmenti; vene di minerale, tappa liquidi, pavimento, torce ogni 10 passi; percorso salvato in `MinePath` per risalire), **Taglialegna** (nuovo, ascia: albero intero + ripianta).
- Rottura a tempo come un giocatore (durezza × 30 / velocità dell'attrezzo), crepe visibili, l'attrezzo si consuma; attrezzo rotto → ne prende un altro dal baule o aspetta (stato NO_TOOL); zaino 27 caselle; raccoglie gli oggetti per terra vicino.
- Baule: quello accanto alla base o quello scelto col **Pannello di controllo robot** (anche lontano). Baule pieno → stato CHEST_FULL alla base finché non riceve "continua" (click destro sul robot o Shift + click in aria col pannello).
- Se resta bloccato per 10 secondi si teletrasporta vicino alla meta.
- Prova in un mondo vero (seme 12345): in circa 3 minuti la scala è arrivata al deepslate, con rame, stagno e ferro raccolti.
- Test automatici: **77** (nuovi: robotMinerDigsStairs, robotTakesToolFromChest, robotStopsWhenChestFull; robotFarmerHarvests ora usa la zappa e il baule lontano).

## Fase 15 — Nucleare e fabbrica abbandonata (fatto)
- **TNT nucleare** (`nuclear/NukeBlock`, entità `primed_nuke`): si accende come la TNT, miccia 10 s con rintocchi, cancella una sfera di raggio `nukeRadius` (32). `nuclear/NuclearBlast` lavora a fette (3 strati per tick), non rompe la bedrock, i contenitori spariscono senza spargere oggetti, danni enormi alle creature, fuoco sul fondo. Prova vera: cratere di raggio ~32 e profondo ~38, nessun "Can't keep up".
- **Reattore stile classico**: griglia 6 righe × (3 + camere) colonne, fino a 54 caselle; le camere passano energia e oggetti al nucleo. Celle refrigeranti 10k/30k/60k che si consumano (diventano "esauste", si ricaricano col secchio d'acqua); dissipatori invariati. Allarme oltre il 70%, fuoco oltre l'85%, al 100% **esplode** (raggio 8 + barre, max `reactorBlastRadius` 24); `reactorExplosions` ora è true di base.
- **Manutentore del reattore** (`nuclear/ReactorMaintainer`): cambia celle oltre il 90%, celle esauste, dissipatori consumati e barre esaurite; se tocca l'acqua ricarica da solo le celle esauste.
- **Fabbrica abbandonata** (`world/AbandonedFactory`): regola del mondo `futuretekAbandonedFactory` (attiva di base, si cambia nelle regole di gioco quando crei il mondo); costruita una volta per mondo vicino allo spawn nel posto più piatto; reparti generati dall'elenco dei blocchi della mod (si aggiorna da sola con le macchine nuove); cartelli, cavi rotti, baule di partenza; messaggio con le coordinate al primo ingresso; comando `/futuretek fabbrica`.
- Test automatici: **85** (nuovi: nuclearBlastClearsSphere, nuclearTntPrimes, reactorChambersAddColumns, coolantCellsGetSpent, reactorExplodesWhenCritical, maintainerSwapsCoolant, factoryHasEveryMachine, factoryBuilds).

## Fase 16 — Minatore più robusto, grafica in stile vanilla, blocchi da costruzione (fatto)
- Robot minatore: `repairStairs()` controlla il pavimento dei gradini vicini e ci mette un blocco (pietrisco, pietre, terra...) se manca; se il robot è caduto nel buco risale sul gradino e lo tappa; non scava più i minerali che fanno da pavimento alla scala/galleria; se è fuori strada riparte dal punto del percorso più vicino (non dall'inizio); dopo 5 s bloccato si teletrasporta vicino alla meta.
- Grafica dei blocchi rifatta in stile Minecraft 1.21 (tools/grafica.py): involucri metallici smussati, pannelli incassati, minerali incastonati, bauli, cavi, tubi, pannelli solari (disegnati da zero).
- Grafica degli oggetti rifatta (nuovo tools/grafica_oggetti.py, chiamato dopo grafica.main()): attrezzi con le sagome classiche (piccone ad arco, ascia, pala, zappa, spada) e la palette del materiale, armature, lingotti, polveri, piastre, fili, gemme.
- **136 blocchi da costruzione** (tools/blocchi_costruzione.py → texture + tools/blocchi_costruzione.json; tools/genera_java_costruzione.py → registry/ModBuildingBlocks.java):
  - pietre: Roccia vulcanica, Marmo bianco, Travertino, Ardesia — base, pietrisco, levigata, mattoni, mattoni crepati, piastrelle, cesellata + scale/lastre/muretti; generate nel mondo come vene (come granito/diorite);
  - legni: 8 colori (rubino, ambra, miele, giada, azzurro, violetto, avorio, ombra) — assi (assi + colorante), tronchi (tronchi + colorante), scale, lastre, staccionate, cancelli.
- Ricerca: le pietre decorative di Tekkit Legends venivano da Blue Power (vulcani di basalto, marmo), i legni colorati da Forestry; nomi e grafica nostri.
- Totali: 184 oggetti, 253 blocchi, 621 ricette. Test automatici: 85.

## Fase 16b — Ricerca degli incantesimi, grafica rifinita (fatto)
- **Archivio degli incantesimi** (oggetto): ricorda gli incantesimi conosciuti e il livello massimo.
- **Ricercatore di incantesimi** (`automation/Enchanting.Researcher`): con energia e tempo (1 minuto, 60.000 TU) scopre da solo un incantesimo nuovo o un livello in più (niente maledizioni); se gli dai un libro incantato lo legge subito e salva i suoi incantesimi (esce un libro vuoto); con un oggetto incantato lo legge senza consumarlo.
- **Incantatore elettrico**: terza casella per l'archivio; con l'archivio scegli incantesimo (frecce) e livello (- / +) e premi "Incanta" (costo: 3 livelli, 1 lapislazzuli e 40.000 TU per livello); controlla la compatibilità; i libri diventano libri incantati. Senza archivio restano i 3 gradi a caso.
- Grafica: attrezzi con le proporzioni dei vanilla (testa spessa, manico largo 2 pixel a scaletta, contorno scuro); blocchi da costruzione con grana fitta come pietra/deepslate vanilla, mattoni con fughe strette, assi a 4 tavole.
- Test automatici: **86** (nuovo: enchantResearchAndApply).

## Fase 16c — Tutti gli oggetti in stile vanilla (fatto)
- Attrezzi (tools/grafica_oggetti.py): piccone, ascia, pala, zappa, spada, martello con le proporzioni dei vanilla, stesso manico, cambia solo la palette.
- Armature e materiali (nuovo tools/grafica_materiali.py): armature come oggetto e indossate (64x32), lingotti, pepite, polveri, piastre, bobine di filo, grezzi, frantumati, gemme.
- Tutti gli altri oggetti (nuovo tools/grafica_varie.py, 83 texture): attrezzi elettrici, circuiti, celle, reattore, anelli/amuleti, alchimia, scanner, borse, robot, api e favi...
- Ordine di generazione con --forza-texture: grafica → grafica_oggetti → grafica_materiali → grafica_varie → blocchi_costruzione. Tutto disegnato da zero.

## Fase 16d — Fattoria di creature più semplice (fatto)
- La **Torretta laser** ogni secondo raccoglie gli oggetti caduti nella sua zona e li mette nel baule (o contenitore) che la tocca: niente più tramogge.
- Guida dell'automazione: sezione "Fattoria di mostri" con 5 immagini passo-passo (tools/disegna_fattoria.py → docs/img/fattoria_*.png, disegnate solo con texture della mod).
- Test automatici: **87** (nuovo: laserTurretCollectsDrops).

## Fase 17 — Rifugio portatile (fatto) · versione 1.0
- **Rifugio portatile** (`utility/Shelter`): click destro su terreno libero 7x7 → nucleo a terra e casetta intorno (pareti azzurre, tetto di tegole, 3 finestre, porta verso il giocatore, letto, banco da lavoro, fornace; il nucleo fa luce 15).
- Controlla lo spazio (solo erba/fiori/neve/foglie si possono sostituire) e che nessuno sia dove vanno i muri; mette il pavimento solo nei buchi.
- Muri/tetto/finestre: indistruttibili, a prova di esplosione, niente drop. Letto/banco/fornace/porta della casa non si possono rompere (né esplodere) finché c'è il nucleo.
- Rotto il nucleo: la casa sparisce, la fornace lascia cadere il contenuto, il nucleo torna oggetto con la carica rimasta (in creativa niente drop).
- Energia: 25.000 TU per casa, 6 case con la carica piena (config `shelterEnergyPerUse`, `shelterUses`); si ricarica nel banco accumulatore; il tooltip mostra le case rimaste. In creativa non consuma.
- Guida dell'automazione: sezione 10 con due immagini (tools/disegna_rifugio.py). Texture: tools/grafica_rifugio.py.
- Test automatici: **88** (nuovo: shelterBuildsAndDisappears).

## Fase 17b — Vulcano e blocchi completi (fatto, sempre versione 1.0.0)
- **Vulcano** (`world/Volcano.java`, feature `futuretek:volcano`): cono di roccia vulcanica con cratere di lava, bordo di magma, colate di pietrisco e camino di lava con ossidiana; raro (1 chunk su 260) nei biomi di terra (tag `futuretek:has_volcano`); funziona anche con `/place feature futuretek:volcano`. Verificato in un mondo vero.
- **Legni colorati completi**: per ognuno degli 8 legni ora ci sono anche tronco scortecciato (con l'ascia), legno, legno scortecciato, porta, botola, pulsante e pedana a pressione; le assi si fanno da tutti i tronchi del colore (tag `futuretek:<colore>_logs`).
- **Pietre**: pulsante e pedana a pressione per roccia vulcanica, marmo, travertino e ardesia; i pietrischi valgono come il pietrisco normale (fornace, attrezzi di pietra, tag `c:cobblestones`).
- Blocchi da costruzione: **200** (prima 136). Test automatici: **90** (nuovi: volcanoGenerates, colouredWoodStripsAndDoors).
