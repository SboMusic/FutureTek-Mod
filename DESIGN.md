# FutureTek – Documento di design

> Mod originale per **Minecraft 1.21.1 / NeoForge 21.1.x / Java 21**.
> Mod id: `futuretek` · Pacchetto Java: `com.futuretek`
> Ispirata alle *meccaniche* dei modpack tecnologici classici (stile Tekkit Legends).
> Nomi, design, texture e codice sono **originali**: non si copia nulla da altre mod.

Questo file descrive *cosa* vogliamo costruire. Lo stato dei lavori è in `PROGRESS.md`.
I numeri di bilanciamento qui sotto sono **valori iniziali**: nel gioco staranno in file
JSON (data pack) o nel config, quindi si potranno cambiare senza toccare il codice.

## Stato di implementazione (Fasi 0-10 + 4b scritte e verificate con 59 test automatici)
| Fase | Implementato | Rimandato |
|---|---|---|
| 1 Base | metalli, minerali, leghe, albero della resina, strumenti/armature | altoforno dedicato |
| 2 Energia | TU, 3 generatori, 4 cavi con reti, 3 banchi, celle, chiave, multimetro | trasformatori, perdita, sovratensione |
| 3 Macchine | 7 macchine (incl. triplicazione), 4 potenziamenti, ricette JSON, ricette in JEI | EMI |
| 4 Essenza | valori JSON + calcolo da ricette, Tavolo, Pietra, Cristalli, Captatori, Replicatore | relè |
| 4b Alchimia | valori per tutti gli oggetti ottenibili, ricerca nel Tavolo, Tavoletta, braci, Nocturnite/Ardorite (attrezzi, magli, armature), catalizzatore, lente, carica stellare, bisaccia, 5 anelli, clessidra, 3 amuleti, gemma, bacchette, talismano, tomo, lanterna | bisacce colorate, forziere alchemico |
| 5 Logistica | condotti, estrattori, smistatore, smistamento automatico, terminale, crafting automatico a catena, 8 porte logiche | condotti per fluidi, cavo dati a 16 colori, oggetti visibili nei tubi |
| 6 Avanzata | reattore (config esplosioni), escavatore, esoarmatura, zaino a reazione | radiazioni, zaino a carburante, marcatori d'area |
| 7 Ferrovie | binari rinforzato/induzione/blocco/carico/scarico, carrello merci 54 slot | locomotiva elettrica, carrello cisterna, binario rilevatore |
| 8 Agricoltura | acceleratore, raccoglitore, fertilizzante, 10 specie di api, incroci JSON, alveari selvatici | serra multiblocco, tratti genetici, analizzatore |
| 10 Automazione | bauli grandi, nucleo logistico con baule principale e animazione, scorta automatica, talpa mineraria, escavatore con segnalini/telaio/testa, carbonaia, generatori avanzati/solari II-III/acqua/vento, macchine avanzate | trasformatori, computer programmabili |
| 9 Esplorazione | Margine Cristallino (terreno, bioma, minerali), Ancora del Margine | mob originali, razzi e pianeti |

---

## 1. Visione

Una progressione unica e coerente, dalla prima officina a carbone fino ai viaggi tra i pianeti:

1. **Officina** – minerali nuovi, prime polveri, strumenti di bronzo e acciaio.
2. **Elettrificazione** – generatori, cavi, accumulatori.
3. **Industria** – macchine che raddoppiano/triplicano i minerali e automatizzano tutto.
4. **Alchimia tecnica** – il sistema di *Essenza* per trasformare la materia.
5. **Logistica** – condotti, smistatori, logica.
6. **Alta tecnologia** – reattore, escavatore automatico, esoarmatura, zaino a reazione.
7. **Ferrovie, agricoltura, api** – sistemi paralleli che arricchiscono la base.
8. **Esplorazione** – una nuova dimensione, poi razzi e pianeti.

Principi:
- **Compatibilità**: energia, oggetti e fluidi usano le *capability* standard di NeoForge
  (`IEnergyStorage`, `IItemHandler`, `IFluidHandler`). Ricette visibili in **JEI** ed **EMI**.
- **Multiplayer prima di tutto**: la logica gira sul server; il client mostra solo ciò che riceve.
- **Configurabile**: ricette, valori di Essenza, generazione dei minerali e rischi in JSON/config.

---

## 2. Materiali e mondo (Fase 1)

### Minerali (con variante *deepslate*)
| ID | Nome IT | Altezza (Y) | Vene/chunk | Note |
|---|---|---|---|---|
| `tin_ore` | Minerale di stagno | 0 → 90 | 10 | Bronzo, cavi base |
| `lead_ore` | Minerale di piombo | -40 → 40 | 6 | Schermature, batterie |
| `silver_ore` | Minerale d'argento | -60 → 20 | 3 | Elettronica avanzata |
| `uranium_ore` | Minerale di uranio | -64 → 0 | 2 | Combustibile del reattore (raro) |
| `voltite_ore` | Voltite | -64 → 16 | 3 | Cristallo energetico originale, rilascia 2-4 *frammenti di voltite* (+ esperienza) |

Rame e ferro sono quelli di Minecraft. I numeri si cambiano in `tools/genera_risorse.py` (`MINERALI_MONDO`).
Fortuna aumenta i drop, Tocco di seta raccoglie il blocco di minerale.

### Per ogni metallo (stagno, piombo, argento, uranio + bronzo, acciaio)
`raw_<metallo>`, `<metallo>_ingot`, `<metallo>_nugget`, `<metallo>_dust`, `<metallo>_plate`, blocco di metallo e blocco di grezzo.
Polveri anche per ferro, oro, rame, carbone, diamante, ossidiana; piastre anche per ferro, oro, rame.
Polvere di diamante e di ossidiana si otterranno solo col Frantumatore (Fase 3).
Tutti hanno i tag comuni `c:` (es. `c:ingots/tin`) per essere compatibili con le altre mod.

### Leghe
- **Bronzo** = 3 rame + 1 stagno (in polvere, poi fusa). Primi strumenti e corazza.
- **Acciaio** = polvere di ferro + 2 polvere di carbone → polvere d'acciaio → si fonde **solo nell'altoforno** di Minecraft.
  (Un altoforno FutureTek dedicato è rimandato: non serve per la progressione.)

### Albero della resina
- `resin_log`, `resin_leaves`, `resin_sapling`. Genera nelle foreste e nelle giungle.
- 1 tronco su 4 nasce con una **vena di resina** (visibile sulla corteccia). Con il **Rubinetto** (`tree_tap`)
  si raccolgono 1-3 **resine grezze** e la vena si svuota; col tempo si riempie di nuovo (solo tronchi verticali).
- Resina grezza cotta in fornace → **gomma** (tag `c:rubbers`). Albero: tronco dritto, chioma a cono, 1 ogni ~4 chunk di foresta/giungla.
- La gomma serve a isolare i cavi della Fase 2: per questo l'albero sta già in Fase 1.

### Strumenti
- Bronzo e acciaio: spada, piccone, ascia, pala, zappa + armatura completa.
- **Martello da forgia** (`forge_hammer`, 256 usi): lingotto → piastra, grezzo → 1 polvere, carbone → polvere di carbone.
- **Tagliacavi** (`cutters`): piastra → filo. *Spostato in Fase 2, insieme ai cavi.*
- Statistiche: bronzo = 350 usi, velocità 6.5, armatura come il ferro; acciaio = 900 usi, velocità 7, armatura 3/7/6/2 + robustezza 1.

### Scheda creativa
- "FutureTek – Risorse" in Fase 1 (contiene in automatico tutti gli oggetti della mod); più avanti "Macchine", "Essenza", "Logistica".

---

## 3. Energia (Fase 2)

### Unità
**TU – Tek Unit**. Internamente **1 TU = 1 FE** (Forge Energy), così cavi e macchine
scambiano energia con le altre mod senza convertitori.

### Livelli di tensione
| Livello | Nome | Max TU/tick per collegamento |
|---|---|---|
| T1 | Bassa (BT) | 32 |
| T2 | Media (MT) | 128 |
| T3 | Alta (AT) | 512 |
| T4 | Estrema (ET) | 2048 |

Il livello è un **limite di portata**: una rete di cavi trasporta al massimo il valore del suo cavo più debole,
e ogni macchina accetta al massimo il proprio livello per tick.
*(Sovratensione con esplosione: rimandata, sarà un'opzione di config disattivata di default.)*

### Generatori
| Blocco | Produzione | Funzionamento |
|---|---|---|
| Generatore a combustione | 20 TU/t | Brucia ciò che va in una fornace; durata = tempo fornace × 1 |
| Pannello solare | 4 TU/t | Solo di giorno, cielo libero sopra; **Matrice solare** (8 pannelli) = 40 TU/t |
| Generatore geotermico | 24 TU/t | Consuma lava (1000 mB = 20 000 TU), accetta lava da tubi/secchi |
| Turbina eolica (dopo) | 2–16 TU/t | Più alta = più produzione |
| Generatore a vapore (dopo) | 64 TU/t | Usa vapore dal reattore o da caldaie |

### Cavi (TU/t massimi della rete)
| Cavo | Max | Ricetta |
|---|---|---|
| Cavo di stagno | 32 | 3 fili di stagno → 6 |
| Cavo di rame isolato | 128 | 3 fili di rame + 6 gomma → 6 |
| Cavo d'oro isolato | 512 | 3 fili d'oro + 6 gomma → 6 |
| Fibra di voltite | 2048 | 3 frammenti di voltite + 6 vetro → 4 |

Fili: tagliacavi + piastra → 2 fili. I cavi si collegano da soli tra loro e alle macchine (anche di altre mod).
L'energia viene divisa in parti uguali tra tutti i blocchi collegati che la accettano.
*(Perdita di energia sulla distanza e scossa dei cavi nudi: rimandate.)*

### Accumulo
| Blocco/oggetto | Capacità | Livello |
|---|---|---|
| Cella portatile (oggetto) | 10 000 TU | T1 |
| Cella avanzata (oggetto) | 100 000 TU | T2 |
| Banco accumulatore | 40 000 TU | T1 |
| Banco accumulatore II | 600 000 TU | T2 |
| Banco accumulatore III | 10 000 000 TU | T3 |
| Nucleo di voltite | 40 000 000 TU | T4 |

### Trasformatori
- *Rimandati*: con il sistema a portata non sono indispensabili. Li aggiungeremo insieme alla sovratensione.

### Strumenti elettrici (Fase 2)
- **Chiave inglese**: ruota le macchine; Shift + click le smonta.
- **Multimetro**: mostra l'energia di qualsiasi blocco; su un cavo mostra la rete (cavi, collegamenti, TU passate).
- **Circuito di base** e **Telaio macchina**: componenti delle macchine.

---

## 4. Lavorazione dei minerali (Fase 3)

Tutte le macchine: interfaccia grafica, barra di progresso, barra d'energia, slot
di potenziamento, accettano oggetti da ogni lato (lati configurabili più avanti).
Ricette in `data/futuretek/recipe/*.json` con tipi personalizzati.

| Macchina | Consumo | Tempo | Funzione |
|---|---|---|---|
| Frantumatore | 4 TU/t | 300 tick | Grezzo/minerale → **2 polveri** (+10% polvere extra); lingotti → polvere; pietrisco → ghiaia → sabbia… |
| Forno elettrico | 6 TU/t | 130 tick | Usa le ricette della fornace di Minecraft, più veloce |
| Compressore | 4 TU/t | 300 tick | Lingotto → piastra; 4 polveri di ossidiana → ossidiana; polvere di diamante → diamante; neve → ghiaccio |
| Estrattore | 4 TU/t | 300 tick | Resina → 3 gomma; tronco/alberello della resina → gomma; TNT → polvere da sparo |
| Pressa per fili | 4 TU/t | 200 tick | Piastra → 3 fili (il tagliacavi a mano ne dà 2) |
| Bagno chimico *(3b)* | 8 TU/t | 400 tick | Grezzo + acqua → **3 frantumati puri** (+ sottoprodotto) |
| Centrifuga termica *(3b)* | 8 TU/t | 500 tick | Frantumati puri → polvere + polvere rara |

Le ricette sono file JSON in `data/futuretek/recipe/<tipo>/` (tipi: `futuretek:crushing`, `compressing`,
`extracting`, `wire_pressing`) con `ingredient` (+ `count`), `result`, facoltativi `secondary` (+ `chance`) e `time`.
Interfaccia: ingresso → freccia di avanzamento → prodotto + prodotto extra; 4 slot di potenziamento; barra dell'energia.
Tubi e tramogge inseriscono solo nell'ingresso ed estraggono solo dai prodotti.

Catena:
- Base: minerale → Frantumatore → 2 polveri → Forno → **2 lingotti** (×2).
- Avanzata *(3b)*: minerale → Bagno chimico → 3 frantumati → Centrifuga → **3 polveri + extra** (×3).

### Potenziamenti (4 slot in ogni macchina)
- **Overclock**: +50% velocità, +60% consumo per tick (max 4 → 3 volte più veloce).
- **Efficienza**: −20% consumo per tick (max 4).
- **Accumulo**: +10.000 TU di capacità (max 4).
- **Espulsione**: spinge i prodotti in bauli/tubi vicini ogni secondo.

### Componenti
Circuito base (rame + gomma + redstone), circuito avanzato (+ argento + voltite),
motore elettrico, telaio macchina (base / avanzato).

---

## 5. Trasmutazione – l'Essenza (Fase 4)

### Concetto
Ogni oggetto ha un **valore di Essenza (ESS)**. Si può "sciogliere" un oggetto per
imparare la sua forma e ottenere Essenza, poi usare l'Essenza per creare
copie di ciò che si è imparato.

### Valori base (JSON `data/futuretek/essence_values/base.json`) – scala scelta dall'utente
**Pietra = 1, lingotto di ferro = 256** (servono 256 pietre per un lingotto).
| Oggetto | ESS | | Oggetto | ESS |
|---|---|---|---|---|
| Pietra, pietrisco, terra, sabbia | 1 | | Rame | 128 |
| Asse (calcolata: tronco / 4) | 8 | | Stagno | 160 |
| Tronco (qualsiasi) | 32 | | Piombo | 200 |
| Carbone | 128 | | Argento | 768 |
| Ferro | 256 | | Uranio | 3000 |
| Oro | 2048 | | Frammento di voltite | 400 |
| Diamante | 8192 | | Stella del Nether | 139264 |
| Smeraldo | 16384 | | Netherite (calcolata) | 57344 |

Nel JSON ci sono solo i materiali "di partenza" (~400 righe: minerali, piante, drop dei mostri, blocchi dei biomi).
Tutti gli altri oggetti ricevono il valore **calcolato dalle ricette**: banco da lavoro, fornaci (anche altoforno,
affumicatore, falò), tagliapietre e tavolo da forgiatura (somma degli ingredienti / quantità prodotta, ricetta più
economica). Se nel banco resta qualcosa (secchio vuoto, martello…) conta solo la differenza.
Le ricette delle **macchine non contano** (raddoppiano i minerali: creerebbero Essenza dal nulla).
La sezione `copies` dice che un oggetto vale come un altro (rame ossidato = rame, cemento = polvere di cemento…).
**Risultato verificato da un test automatico: TUTTI gli oggetti ottenibili hanno un valore** (1473); restano senza solo
16 blocchi da creativa/comandi e le uova generatrici.

**Si sciolgono ma non si creano** (tag `futuretek:essence_not_creatable`): minerali, grezzi, polveri, frantumati
purificati, favi, api e alveari. Le macchine li moltiplicano (1 grezzo → 3 lingotti): se si potessero creare,
il giro "crea grezzo → macchine → sciogli lingotti" darebbe Essenza infinita.

Valore di una pila reale: oggetti consumati valgono in proporzione alla durata rimasta; ogni livello di incantesimo
vale +2048 (anche nei libri incantati); i Cristalli contano l'Essenza che contengono.

Comandi: `/futuretek essenza valore` (oggetto in mano), `/futuretek essenza mancanti` (scrive in `config/` l'elenco
degli oggetti senza valore e la tabella completa dei valori), `/futuretek essenza aggiungi N` e
`/futuretek essenza impara_tutto` (solo operatori, per le prove).

### Blocchi e oggetti
| ID | Nome | Funzione |
|---|---|---|
| `shifting_stone` | Pietra del Mutamento | Nel banco (non si consuma): 8 ferro ↔ 1 oro, 4 oro ↔ 1 diamante, 2 carbone → 1 ferro, 5 stagno → 1 argento, braci. Click destro: trasforma blocchi, Shift = 3×3. **Shift + click destro in aria: banco da lavoro portatile** |
| `synthesis_table` | Tavolo di Sintesi | Slot "sciogli" (anche Shift+click dall'inventario). Griglia di 40 oggetti conosciuti con **casella di ricerca**, pagine e rotellina; click = crea 1, Shift+click = una pila |
| `essence_crystal` | Cristallo d'Essenza (I-IV: 100k / 2M / 50M / 1 miliardo) | Si carica sul Captatore; click destro in aria = versa l'Essenza nella riserva. Gli oggetti alchemici prendono Essenza prima dalla riserva, poi dai cristalli |
| `replicator` | Replicatore | Slot modello + 9 materiali (diventano Essenza) + uscita; riceve Essenza dai Captatori accanto |
| `star_collector` | Captatore Stellare (I-III) | 8 / 24 / 80 ESS al secondo di giorno (¼ di notte) |

### Alchimia avanzata (Fase 4b) – ispirata alle meccaniche classiche, con nomi, disegni e regole nostri
| ID | Nome | Funzione |
|---|---|---|
| `arcane_ember` / `stellar_ember` / `eternal_ember` | Braci Arcana / Stellare / Eterna | Pietra + 4 carbone → 1 Arcana (e così via ×4). Combustibili: 32 / 128 / 512 oggetti cotti |
| `nocturnite`, `ardorite` (+ blocchi) | Nocturnite, Ardorite | Materiali alchemici: 8 Braci Eterne + blocco di diamante; 5 Nocturnite + 4 Braci Eterne |
| attrezzi `*_pickaxe/axe/shovel/hoe/sword/hammer` | di Nocturnite / Ardorite | Indistruttibili. Shift + click destro in aria = scavo ad area 3×3 (Shift mentre scavi = blocco singolo). Maglio: sempre 3×3 (Ardorite 5×5) |
| armature `*_helmet…boots` | di Nocturnite / Ardorite | Indistruttibili. Set completo: niente danni da caduta, immune al fuoco; Ardorite anche −50% danni e respiro in acqua |
| `void_catalyst` | Catalizzatore del Vuoto | Rompe 3×3×3 (Shift 5×5×5), gli oggetti vanno nell'inventario. 4 ESS/blocco |
| `kinetic_lens` | Lente Cinetica | Scava una sfera di raggio 2 dove guardi (64 blocchi). 8 ESS/blocco |
| `star_charge` | Carica Stellare | Redstone o acciarino: dopo 4 s rompe tutto entro 6 blocchi, gli oggetti cadono al centro (niente va distrutto) |
| `dimensional_satchel` | Bisaccia Dimensionale | 54 slot personali (salvati sul giocatore, restano dopo la morte) |
| `attractor_ring` | Anello Attrattore | Acceso: attira oggetti ed esperienza (8 blocchi) |
| `windrider_ring` | Anello dei Venti | Acceso: volo come in creativa, 2 ESS/s mentre voli |
| `ember_ring` | Anello Ardente | Immune al fuoco; click destro = palla di fuoco (16 ESS) |
| `frost_ring` | Anello Glaciale | L'acqua ghiaccia sotto i piedi; click destro congela acqua/lava intorno (32 ESS) |
| `verdant_ring` | Anello Verdeggiante | Acceso: fa crescere coltivazioni e alberelli (raggio 5) |
| `arcane_hourglass` | Clessidra Arcana | Accesa: macchine/fornaci/piante entro 4 blocchi lavorano 4× (4 ESS/s) |
| `vitality_amulet` | Amuleto Vitale | Acceso: fame (16 ESS) e salute (32 ESS) |
| `memory_gem` | Gemma della Memoria | Conserva/restituisce l'esperienza |
| `tide_amulet` / `magma_amulet` | Amuleto delle Maree / del Magma | Acqua infinita (anche per il bagno chimico) / lava (64 ESS) |
| `dowsing_rod` | Bacchetta Rabdomante | Essenza contenuta nell'area 3×3×8 dietro un blocco |
| `builder_wand` | Bacchetta del Costruttore | Piazza uno strato 3×3 del blocco in mano sinistra pagandolo in Essenza |
| `repair_talisman` | Talismano Riparatore | Ripara piano piano tutto l'inventario |
| `synthesis_tablet` | Tavoletta di Sintesi | Il Tavolo di Sintesi in tasca |
| `omniscience_tome` | Tomo dell'Onniscienza | Impari tutti gli oggetti con un valore (si consuma; 2 stelle del Nether + 4 Ardorite) |
| `repulsion_lantern` | Lanterna Repulsiva | Tiene lontani mostri e proiettili (8 blocchi) |

Bilanciamento: il primo Captatore richiede un diamante e polveri di voltite; il
sistema arriva **dopo** l'industria, non al posto suo.

---

## 6. Logistica e logica (Fase 5)

### Condotti (trasporto oggetti)
| Condotto | Velocità | Note |
|---|---|---|
| Condotto di pietra | normale | Economico |
| Condotto di ferro | normale | Una direzione sola (si ruota con la chiave) |
| Condotto d'oro | veloce | Accelera gli oggetti |
| Condotto smistatore | — | Ogni lato ha una lista di oggetti ammessi |
| Condotto estrattore | — | Estrae da un inventario con un impulso di redstone o con energia |

Gli oggetti si vedono scorrere dentro i condotti (rendering lato client).

### Condotti per fluidi
Pietra (1000 mB/s), ferro (4000), d'oro (16 000). Con vetro a vista del liquido.

### Smistamento e crafting automatico (richiesta dell'utente)
- **Rete logistica**: condotti "intelligenti" + **Terminale** che mostra tutti gli oggetti dei bauli collegati,
  da cui si può prendere qualsiasi oggetto.
- **Smistatore automatico**: ogni oggetto in arrivo va da solo nel baule giusto (in base a filtri o a cosa contiene già).
- **Banco di assemblaggio automatico**: memorizza una ricetta e la esegue da solo prendendo gli ingredienti dalla rete;
  con le **richieste** si può chiedere "fammi 64 cavi" e la rete fabbrica anche i passaggi intermedi.

### Dispositivi
- **Filtro** – blocco che fa passare solo certi oggetti.
- **Smistatore** – riceve oggetti e li manda ai lati in base a regole e colori.
- **Mantice** (estrattore temporizzato), **Riempitore**, **Retriever** (richiama oggetti dalla rete).

### Logica avanzata
- Porte **AND, OR, NOT, XOR, NAND**, **timer**, **contatore**, **latch RS**, **randomizzatore**.
- **Cavo dati a 16 colori**: trasporta 16 segnali separati in un solo cavo.
- **Pannello di controllo** e lampade colorate.

---

## 7. Alta tecnologia (Fase 6)

### Reattore a fissione
- Blocco **Nucleo del reattore** + fino a 6 **Camere** adiacenti (inventario più grande).
- Celle di **combustibile all'uranio** (singola/doppia/quadrupla), **dissipatori**, **celle refrigeranti**.
- Produce TU (o vapore, opzionale). Calore gestito dalla disposizione delle celle.
- Config: `reactorExplosions` (sì/no), `reactorMaxBlastRadius`, `reactorHeatMultiplier`,
  `reactorRadiation` (effetti di veleno nelle vicinanze).
- Interfaccia con temperatura e simulazione della griglia.

### Escavatore automatico
- Blocco + **marcatori di area**. Scava strato per strato fino al bedrock.
- 160 TU per blocco (config), potenziamenti di velocità e **fortuna**/**tocco di seta**.
- Butta l'output in un inventario o in un condotto collegato.
- Config: blocchi esclusi, limite d'area, rispetto delle protezioni.

### Esoarmatura
- Elmetto, petto, gambali, stivali alimentati a TU (1 000 000 TU ciascuno).
- Moduli: visione notturna, nessuna caduta, salto potenziato, velocità, scudo energetico.

### Zaino a reazione
- **Zaino a reazione a carburante** (combustibile liquido) e **elettrico** (TU).
- Modalità hover. Sincronizzazione del volo client/server con pacchetti propri.

---

## 8. Ferrovie (Fase 7)

- **Binario rinforzato**: velocità massima più alta.
- **Binario a induzione**: accelera usando TU.
- **Binario di carico / scarico**: trasferisce tra carrello e inventario sotto il binario.
- **Binario rilevatore** filtrabile (per tipo di carrello / contenuto).
- **Binario di blocco/rilascio**: ferma il carrello finché non riceve redstone.
- Carrelli: **Locomotiva elettrica**, **carrello cisterna**, **carrello con cassa grande**,
  **carrello di carico** con aggancio a convoglio.

---

## 9. Agricoltura e api (Fase 8)

### Agricoltura
- **Serra** (multiblocco semplice): accelera le colture al suo interno.
- **Coltivatore automatico** e **Raccoglitore**: piantano e raccolgono in un'area.
- **Fertilizzante** e **compostiera industriale**.
- Nuovi alberi da frutto (limone, castagno, …) e incroci di alberi.

### Api
- **Arnia tecnica** e **Apiario** con regina + fuchi.
- Specie originali con caratteristiche ereditarie (velocità, fertilità, durata, clima):
  Comune → Forestale / Prateria → Mineraria → Metallica (polvere di metalli),
  Glaciale, Ardente (Nether), Abissale (End), Voltaica (frammenti di voltite), Stellare (Essenza).
- **Incroci**: due specie possono generare una terza con una certa probabilità (JSON).
- Prodotti: favi, cera, propoli, pappa reale. **Centrifuga per favi** e **spremitrice**.
- Strumenti: **Analizzatore genetico**, abito da apicoltore.

---

## 10. Esplorazione (Fase 9)

### Dimensione: *Il Margine Cristallino*
- Si raggiunge con un portale a cornice di **blocchi di voltite**, acceso con una **Scintilla di voltite**.
- Mondo di altopiani di cristallo, cieli viola, minerali più ricchi, strutture abbandonate
  con bottino tecnologico.
- Mob originali: **Sentinella cristallina** (ostile), **Lucciola di voltite** (passiva).

### In futuro: razzi e pianeti
- Rampa di lancio, razzo a stadi, tuta spaziale con ossigeno.
- **Selene** (luna, bassa gravità), **Korrath** (pianeta rosso, minerali rari).

---

## 11. Strumenti di supporto

- **Chiave inglese** – ruota i blocchi, smonta le macchine senza perdere il contenuto.
- **Multimetro** – mostra energia, flusso e stato di un blocco.
- **Vulcani** (Fase 17b) – rari coni di roccia vulcanica (raggio 10-14, alti 16-26) in pianure, savane, badlands,
  deserti, taighe, colline e foreste: cratere con lago di lava, magma sulla bocca, colate di pietrisco sui fianchi e un
  camino di lava che scende 14 blocchi sotto terra con un guscio di ossidiana (`world/Volcano.java`, 1 chunk su 260).
- **Rifugio portatile** (Fase 17) – "tenda" elettrica per chi esplora: il nucleo costruisce una casetta 7x7
  (muri, tetto e finestre indistruttibili, letto, banco da lavoro, fornace, porta verso il giocatore).
  Rompendo il nucleo la casa sparisce, il terreno resta com'era e il nucleo torna con la carica.
  Ogni casa costa 25.000 TU, carica piena = 6 case (config `shelterEnergyPerUse`, `shelterUses`).
- Integrazioni: JEI/EMI (Fase 3b), Jade/WTHIT (opzionale, info sui blocchi).

---

## 12. Ordine delle fasi (rivisto)

| Fase | Contenuto |
|---|---|
| 0 | Preparazione ambiente, progetto, documentazione |
| 1 | Minerali e generazione, lingotti, polveri, leghe, **albero della resina**, strumenti, scheda creativa |
| 2 | Energia: TU, generatori, cavi, accumulatori, trasformatori |
| **3a** | Macchine di lavorazione (5), ricette JSON, potenziamenti |
| **3b** | Bagno chimico + centrifuga (triplicazione), integrazione JEI |
| 4 | Essenza e trasmutazione |
| 5 | Logistica e logica |
| 6 | Reattore, escavatore, esoarmatura, zaino a reazione |
| 7 | Ferrovie |
| 8 | Agricoltura e api |
| 9 | Dimensione (e poi razzi/pianeti) |
| 10 | Logistica avanzata, escavatore classico, talpa, bauli, generatori rinnovabili (fatto) |
| 10b | Correzioni dopo i test: talpa, telaio dell'escavatore, tubo analizzatore, bilanciamento generatori (fatto) |
| 11 | Utensili elettrici: trapano (ferro/diamante), motosega, lama al plasma, fucile a impulsi, esoarmatura a 3 livelli (fatto) |
| 12 | Automazione casa: lampade elettriche con interruttore, fattoria di alberi (pianta + taglia), piantatrice, fertilizzatore a farina d'ossa (fatto) |
| 13 | Tavolo d'incantamento elettrico; generatore di mostri da Nether (materiali del Nether, energia = frequenza), laser che uccidono (fatto) |
| 14 | Robot: base di ricarica, robot contadino, guardia, minatore, nucleo infinito (fatto) |
| 15 | Nucleare: TNT nucleare, reattore a camere che esplode, celle che si consumano, manutentore; fabbrica abbandonata (fatto) |
| 16 | Grafica in stile vanilla (blocchi e oggetti), 200 blocchi da costruzione, vulcani, minatore più robusto (fatto) |
| 17 | Rifinitura, bilanciamento, multiplayer completo, pubblicazione |

**Perché queste modifiche:**
- L'albero della resina va in Fase 1 perché la gomma serve per i cavi della Fase 2.
- La Fase 3 è divisa perché la prima macchina insegna tutte le basi (interfacce,
  sincronizzazione client/server, ricette): conviene farla da sola e bene, poi replicarla.

---

## 13. Convenzioni tecniche

- Pacchetti: `com.futuretek.registry` (registrazioni), `.block`, `.item`, `.blockentity`,
  `.menu`, `.client` (solo client), `.recipe`, `.energy`, `.network`, `.datagen`, `.compat`.
- ID in inglese minuscolo con `_`; nomi visibili in `en_us.json` e `it_it.json`.
- Texture segnaposto generate da script Python in `tools/`, sostituibili in seguito.
- Modelli, stati dei blocchi, loot table, tag, ricette, generazione del mondo e traduzioni sono generati
  da `tools/genera_risorse.py` nella cartella `src/generated/resources` (non modificarla a mano).
