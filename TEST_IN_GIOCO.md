# FutureTek – Guida di prova in gioco

Avvia con **"FutureTek - Avvia"** (Desktop). Crea un **mondo nuovo in Creativa** (i minerali, gli alberi e gli alveari
compaiono solo nelle zone nuove). Tutti gli oggetti sono nella scheda **FutureTek - Risorse**.
Segna ✅ / ❌ e scrivi a Claude cosa non va (Claude legge da solo il registro in `build-logs/client.log`).

## 0. Avvio
- [ ] Il gioco parte; nel menu Mods c'è FutureTek; a destra dell'inventario compare **JEI**
- [ ] Nessun oggetto/blocco a scacchi viola e neri nella scheda creativa

## 1. Materiali e mondo
- [ ] Martello + lingotto = piastra (il martello resta); 3 polvere di rame + 1 di stagno = 4 polvere di bronzo
- [ ] Alberello della resina + farina d'ossa → albero; rubinetto sul tronco con la striscia arancione → resina
- [ ] In Sopravvivenza: minerali di stagno/piombo/argento/uranio/voltite nelle grotte

## 2. Energia
- [ ] Generatore a combustione + carbone → cavi → banco accumulatore: la barra sale
- [ ] Multimetro su un cavo (dati della rete) e su un banco (energia)
- [ ] Geotermico con secchio di lava; pannello solare di giorno; cella nello slot "Carica"
- [ ] Chiave inglese: ruota / Shift = smonta

## 3. Macchine (collega all'energia)
- [ ] Frantumatore: 1 grezzo → 2 polveri; Forno elettrico; Compressore; Estrattore (resina → 3 gomma); Pressa per fili
- [ ] Bagno chimico (riempilo con un secchio d'acqua): 1 grezzo → 3 frantumati puri; Centrifuga termica: frantumato → polvere
- [ ] Potenziamenti: 4 overclock = ~3 volte più veloce; espulsione con un baule accanto

## 4. Trasmutazione (Essenza)
- [ ] Il suggerimento degli oggetti mostra "Essenza: N"
- [ ] Tavolo di Sintesi: metti un diamante nello slot in alto a destra → sparisce, l'Essenza sale e il diamante compare nella griglia; click per crearne uno
- [ ] Pietra del Mutamento: nel banco 8 ferro + pietra = 1 oro; click destro sulla pietra la trasforma in pietrisco
- [ ] Captatore Stellare all'aperto accanto a un Replicatore con modello "pietrisco" e terra negli slot

## 4b. Alchimia avanzata (comodo: scrivi `/futuretek essenza aggiungi 10000000` per avere Essenza)
- [ ] Il suggerimento di quasi ogni oggetto mostra "Essenza: N" (pietra 1, lingotto di ferro 256, diamante 8192)
- [ ] Tavolo di Sintesi: scrivi nella casella di ricerca (es. "diam") → la griglia si filtra; rotellina = pagine
- [ ] `/futuretek essenza impara_tutto`, poi nel Tavolo cerca e crea qualcosa
- [ ] Tavoletta di Sintesi: click destro apre il Tavolo ovunque · Pietra del Mutamento: Shift + click in aria = banco da lavoro
- [ ] Braci nel generatore a combustione (durano molto di più del carbone)
- [ ] Piccone di Nocturnite: Shift + click destro in aria → "Scavo ad area: ATTIVO", scava 3×3; Maglio di Ardorite 5×5
- [ ] Set completo di armatura di Nocturnite: salta da molto in alto (niente danni), cammina nella lava
- [ ] Catalizzatore del Vuoto su una parete (e con Shift); Lente Cinetica guardando lontano
- [ ] Carica Stellare + leva/blocco di redstone (allontanati!): buca pulita e oggetti al centro
- [ ] Bisaccia: metti oggetti, chiudi, riapri anche con un'altra bisaccia
- [ ] Anelli: dei Venti (in Sopravvivenza: doppio salto per volare), Attrattore, Ardente (palla di fuoco), Glaciale (cammina sull'acqua), Verdeggiante (campo di grano)
- [ ] Clessidra Arcana accesa vicino a una fornace o a una macchina: va molto più veloce
- [ ] Amuleto Vitale, Gemma della Memoria (prendi livelli, click: spariscono; Shift+click: tornano)
- [ ] Amuleto delle Maree: acqua ovunque e click destro sul bagno chimico; Amuleto del Magma: lava
- [ ] Bacchetta Rabdomante sul terreno; Bacchetta del Costruttore con un blocco nella mano sinistra
- [ ] Talismano Riparatore con un piccone usurato nell'inventario; Lanterna Repulsiva di notte
- [ ] JEI: cerca "Frantumatore" e premi U (usi) → si vedono le ricette della macchina con tempo ed energia

## 4c. Comodità
- [ ] Ascia su un albero vero: cade tutto; con Shift un tronco solo; su una casa di legno (senza foglie) un tronco solo
- [ ] Fornace di ferro (8 lingotti di ferro): cuoce più in fretta di una fornace normale accanto
- [ ] Polveri di rilevamento I/II/III e Rilevatori I/II/III: click destro in una grotta → elenco minerali in chat
- [ ] Sacca del minatore: Shift+click = raccolta automatica; rompi minerali di ferro/diamante → vanno nella sacca
- [ ] Catalizzatore del Vuoto: click in aria per caricarlo (1→4), poi click su una parete: buco più grande

## 10. Fase 10
- [ ] Bauli di rame/ferro/oro/diamante/ossidiana: pulsanti "Deposita" e "Ordina"
- [ ] Nucleo logistico + cavo di energia + baule che tocca il nucleo (principale) + tubi + tubo di destinazione con filtro
      + baule: metti oggetti nel principale → partono da soli e si vedono viaggiare nei tubi
- [ ] Banco di assemblaggio: ricetta + "Scorta" 64 → fabbrica da solo finché nella rete ce ne sono 64
- [ ] Talpa mineraria accanto a un baule, carbone nello slot, raggio 2, Avvia → scava e torna a scaricare
- [ ] Escavatore: segnalino accanto + segnalino nell'angolo opposto → telaio giallo e testa che si muove
- [ ] Carbonaia piena di tronchi; mulino ad acqua nell'acqua; mulino a vento in alto (click destro = produzione)
- [ ] Generatore avanzato, pannelli solari II/III, frantumatore avanzato, forno a induzione

## 5. Logistica
- [ ] Baule → condotto estrattore → condotti → baule: gli oggetti si spostano da soli
- [ ] Condotto smistatore (click destro → filtro): i bauli accanto ricevono solo quegli oggetti
- [ ] Terminale di rete accanto ai condotti: vedi tutti gli oggetti, ricerca, click per prendere, Shift+click dall'inventario per mettere
- [ ] Banco di assemblaggio automatico: imposta tronco → assi; un altro assi → bastoncini; dal terminale Ctrl+click (o click centrale) sui bastoncini
- [ ] Porte logiche: AND con due leve ai lati accende una lampada davanti

## 6. Tecnologia avanzata
- [ ] Reattore: barra + 2 dissipatori ai lati, segnale redstone → produce TU (in alto a destra); barra del calore
- [ ] Escavatore con un baule sopra ed energia: scava davanti a sé
- [ ] Esoarmatura carica (banco accumulatore): visione notturna, salto, niente danni da caduta
- [ ] Zaino a reazione carico: tieni premuto SPAZIO per volare

## 7. Ferrovie
- [ ] Binario rinforzato più veloce; binario a induzione collegato a un cavo accelera
- [ ] Binario di carico sopra un baule pieno riempie un carrello con baule; carrello merci (54 slot)

## 8. Agricoltura e api
- [ ] Raccoglitore accanto a un campo di grano maturo (+ baule): raccoglie e ripianta
- [ ] Acceleratore di crescita fa crescere le piante vicine; fertilizzante 3x3
- [ ] Alveare selvatico (foreste/pianure) → regina + fuchi; Arnia tecnica all'aperto di giorno → favi; dopo 6 cicli nuova regina

## 9. Il Margine Cristallino
- [ ] Ancora del Margine: click destro → nuova dimensione viola con grappoli di voltite; c'è un'ancora accanto per tornare

## 10b. Correzioni dopo i test
- [ ] Talpa con raggio 1 accanto a un baule: scava il 3x3 che parte da lei e va dalla parte opposta al baule (sotto il baule non scava)
- [ ] Escavatore con 2 segnalini: telaio a barre sottili (anello in basso, 4 colonne, anello in alto), carrello e trivella che si muovono
- [ ] Rompi l'escavatore: il telaio sparisce
- [ ] Escavatore senza baule: sputa gli oggetti dall'alto e continua; con un baule sopra li mette lì
- [ ] Escavatore con 1 pannello solare: lento ma non si ferma; con 2 pannelli solari III: molto più veloce
- [ ] Tubo analizzatore + baule con 1 pietrisco: il pietrisco del baule principale arriva lì; svuotalo e riprova: arriva ancora
- [ ] "Lascia 1: SÌ": dal terminale prendi tutto il pietrisco, nel baule ne resta 1

## 11. Utensili ed esoarmatura elettrici
- [ ] Trapano elettrico nel banco accumulatore (casella in alto) fino a carica piena; scava pietra, terra, ferro velocemente
- [ ] Trapano scarico: lentissimo e non raccoglie i minerali
- [ ] Trapano di diamante: raccoglie l'ossidiana; Shift + click destro → 3x3 (la barra scende più in fretta)
- [ ] Motosega su un albero: cade tutto l'albero; le foglie si tagliano subito
- [ ] Lama al plasma carica contro uno zombie (2 colpi); scarica fa pochissimo danno
- [ ] Fucile a impulsi: click destro verso un mob lontano → raggio di scintille e danno
- [ ] Esoarmatura I (piastre di ferro + cella) e III (II + netherite): guarda i valori di protezione; il III ti tiene sazio e non ti bruci nella lava

## 12. Casa automatica
- [ ] Generatore → cavo → interruttore (con leva) → cavo → una lampada; altre lampade e pannelli attaccati: leva ON si accendono tutte, OFF si spengono
- [ ] Fattoria di alberi su terra piatta con alberelli + baule sopra: pianta, e quando l'albero cresce lo taglia (legno nel baule)
- [ ] Piantatrice con semi su terra vicino all'acqua: zappa e pianta; Raccoglitore accanto raccoglie; Fertilizzatore con farina d'ossa fa crescere

## 13. Nether
- [ ] Nucleo d'anima → Campionatore: click destro su uno zombie → tooltip "Campione: Zombie"
- [ ] Incubatore con il campionatore + energia: compaiono zombie sopra; Torretta laser accanto li uccide e cade esperienza
- [ ] Incantatore elettrico: spada + 3 lapislazzuli + 9 livelli + energia → grado III

## 14. Robot
- [ ] Base di ricarica collegata a un generatore + baule accanto; Robot (click destro sulla base) → si vede il robot
- [ ] Click destro sul robot: cambia lavoro (Contadino / Guardia / Sentinella / Minatore), il colore del corpo cambia
- [ ] Contadino vicino a un campo di grano maturo: raccoglie, ripianta, porta il grano nel baule
- [ ] Guardia: ti segue e attacca gli zombie; Shift + click destro: torna oggetto
- [ ] Minatore con piccone di ferro + torce nel baule: scava la scala verso il basso, mette le torce, torna a scaricare dalla scala
- [ ] Togli il piccone (o aspetta che si rompa) e metti un piccone nel baule: il robot va a prenderlo e riparte
- [ ] Riempi il baule: il robot si ferma alla base; svuotalo, Shift + click destro in aria col pannello → riparte
- [ ] Pannello: click destro sul robot, poi sul baule principale del magazzino (lontano): il robot porta lì il bottino
- [ ] Taglialegna con l'ascia vicino a una foresta: abbatte gli alberi interi e ripianta

## 15. Nucleare e fabbrica
- [ ] Mondo NUOVO: al primo ingresso arriva il messaggio con le coordinate della fabbrica; dentro ci sono tutte le macchine con i cartelli
- [ ] Nella fabbrica: rimetti i cavi mancanti, carbone nel generatore → le macchine si accendono
- [ ] /futuretek fabbrica in un mondo vecchio: ne costruisce una davanti a te
- [ ] Reattore: nucleo + 2 camere → la griglia ha 5 colonne; leva = acceso
- [ ] Barre + celle refrigeranti: le celle si riempiono e diventano esauste; secchio d'acqua + cella esausta = cella nuova
- [ ] Manutentore attaccato al reattore con celle di ricambio (e acqua accanto): cambia da solo le celle
- [ ] (MONDO DI PROVA!) Reattore senza raffreddamento: allarme, fumo, fuoco... poi esplode
- [ ] (MONDO DI PROVA!) TNT nucleare: 10 secondi di miccia e cratere enorme

## 16. Grafica e blocchi da costruzione
- [ ] Guarda le macchine, i minerali, gli attrezzi e le armature: stile simile a Minecraft
- [ ] Creativa → scheda FutureTek: blocchi di roccia vulcanica, marmo, travertino, ardesia e legni colorati; prova scale, lastre (anche doppie), muretti, staccionate e cancelli
- [ ] Assi di quercia x8 + colorante rosso → assi color rubino; tagliapietre con la roccia vulcanica
- [ ] Mondo nuovo: in miniera si trovano vene di roccia vulcanica (sotto y 24), marmo, travertino, ardesia
- [ ] Robot minatore vicino a una grotta: quando sotto la scala c'è il vuoto mette un blocco e continua

## 16b. Ricerca degli incantesimi
- [ ] Ricercatore di incantesimi + archivio + energia: dopo 1 minuto l'archivio conosce un incantesimo (tooltip)
- [ ] Libro incantato (es. Fortuna III) nella seconda casella: salvato subito, esce un libro vuoto
- [ ] Incantatore: piccone + lapislazzuli + archivio → scegli Fortuna con le frecce, livello con - / +, "Incanta"
- [ ] Attrezzi accanto a quelli normali nella barra: stessa forma, colori diversi

## 16d. Fattoria di creature
- [ ] Campionatore: click destro su una mucca → diventa luccicante
- [ ] Incubatore all'aperto + pannello solare III sopra + uno di lato + campionatore dentro → di giorno compaiono mucche intorno
- [ ] Torretta laser attaccata all'incubatore, baule sopra, pannello solare III dietro, "Tutte le creature" → la carne e la pelle finiscono nel baule

## 17. Rifugio portatile
- [ ] Crea il rifugio, caricalo nel banco accumulatore: tooltip "Case rimaste: 6 / 6"
- [ ] In sopravvivenza, su un prato piatto: click destro → casa con porta verso di te, letto, banco, fornace; "Case rimaste: 5"
- [ ] Dormi nel letto; prova a rompere letto/fornace/muri: non si rompono
- [ ] Metti qualcosa nella fornace, rompi il nucleo: la casa sparisce, la roba della fornace cade, il nucleo torna con 5 case
- [ ] Dentro una foresta fitta o in montagna: messaggio "Non c'è posto"
- [ ] Rifugio scarico: messaggio "Energia insufficiente"

## 17b. Vulcano e blocchi completi
- [ ] `/place feature futuretek:volcano` su un prato: cono nero con lava in cima e magma sul bordo
- [ ] Esplorando un mondo nuovo si trova un vulcano (sono rari: uno ogni qualche centinaio di blocchi)
- [ ] Ascia su un tronco colorato → tronco scortecciato
- [ ] Porta, botola, pulsante e pedana colorati: si aprono / danno segnale redstone
- [ ] Pulsante e pedana di roccia vulcanica / marmo / travertino / ardesia
- [ ] Fornace e attrezzi di pietra fatti con il pietrisco di roccia vulcanica

## Multiplayer
- [ ] "FutureTek - Server" + "FutureTek - Avvia" → Multigiocatore → `localhost`: ripeti 2-3 prove (macchina, terminale, Tavolo di Sintesi)
