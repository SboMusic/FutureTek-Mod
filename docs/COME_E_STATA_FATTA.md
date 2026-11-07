# Come è stata fatta FutureTek

## L'idea
FutureTek nasce dal desiderio di rigiocare l'esperienza dei modpack tecnologici classici (tipo Tekkit Legends) su
Minecraft moderno, in **una sola mod** e con **contenuti tutti nostri**.

La regola fin dal primo giorno:

> La mod deve essere ORIGINALE: non si copiano codice, texture, suoni o nomi di oggetti da IndustrialCraft, ProjectE,
> BuildCraft, RedPower/Blue Power, Railcraft, Forestry, Galacticraft o altre mod. Si prende ispirazione dalle
> meccaniche, ma con nomi, design e implementazione nostri.

Anche le texture di Minecraft non sono copiate: ogni immagine è disegnata da zero, pixel per pixel, da script Python,
nello *stesso stile* di quelle originali.

## Chi l'ha fatta
L'autore e direttore del progetto è **SboMusic**: ha deciso le meccaniche, provato tutto in gioco e chiesto le
correzioni. Il codice, le risorse e i documenti sono stati scritti insieme a **Claude** (assistente IA di Anthropic),
una fase alla volta, con spiegazioni semplici in italiano a ogni passo.

## Il metodo: a fasi
Il progetto è diviso in fasi (vedi [`PROGRESS.md`](../PROGRESS.md) per il diario completo):

| Fase | Contenuto |
|---|---|
| 0 | preparazione: progetto dal template ufficiale NeoForge (ModDevGradle), Git, launcher |
| 1 | materiali, minerali, leghe, albero della resina, attrezzi e armature |
| 2 | energia: unità TU, generatori, cavi a rete, accumulatori |
| 3 | macchine di lavorazione con potenziamenti, ricette in JSON, integrazione JEI |
| 4 / 4b | Essenza (trasmutazione) e alchimia avanzata |
| 5 | logistica: condotti, smistamento, terminale, crafting automatico, logica |
| 6 | alta tecnologia: reattore, escavatore, esoarmatura, zaino a reazione |
| 7 | ferrovie |
| 8 | agricoltura e api |
| 9 | dimensione *Margine Cristallino* |
| 10-11 | automazione avanzata, utensili elettrici |
| 12-14 | casa automatica, Nether (incantatrice, incubatore, torretta), robot autonomi |
| 15 | reattore a camere che esplode, TNT nucleare, fabbrica abbandonata |
| 16 | blocchi da costruzione, grafica in stile vanilla, ricerca degli incantesimi, fattoria di creature |
| 17 | rifugio portatile → **versione 1.0** |

Ogni fase segue lo stesso giro:
1. **Progetto**: si scrive cosa fare in [`DESIGN.md`](../DESIGN.md).
2. **Codice**: classi Java in `src/main/java/com/futuretek`, risorse generate dagli script in `tools/`.
3. **Test automatici**: per ogni meccanica c'è un *GameTest* (`src/main/java/com/futuretek/gametest`). Un server
   Minecraft vero li esegue tutti; la 1.0 ne ha **88** e passano tutti.
4. **Prova in gioco**: l'autore segue [`TEST_IN_GIOCO.md`](../TEST_IN_GIOCO.md) e segnala cosa non va.
5. **Commit Git** a fine passo.

## Le scelte tecniche
- **NeoForge 1.21.1** con **ModDevGradle**, Java 21, nomi ufficiali (mojmap) + Parchment.
- Registrazione con `DeferredRegister`; energia, oggetti e fluidi passano dalle **capability** di NeoForge, così
  funzionano anche con altre mod.
- **Multiplayer sicuro**: tutta la logica gira sul server; il client mostra soltanto.
- **Numeri regolabili**: costi, raggi, velocità stanno nella configurazione (`config/futuretek-common.toml`) o in file
  JSON (ricette, valori dell'Essenza, incroci delle api).
- **Risorse generate**: modelli, stati dei blocchi, ricette, tag, loot e traduzioni non si scrivono a mano: li
  produce `tools/genera_risorse.py`. Così sono sempre coerenti e senza errori di battitura.
- **Grafica**: le texture 16x16 sono disegnate da script Python con PIL (`tools/grafica*.py`,
  `tools/blocchi_costruzione.py`), con tavolozze e ombreggiature nello stile di Minecraft.
- **Guide**: le pagine HTML in `docs/` sono generate dagli script `tools/genera_guida*.py` e usano le texture vere
  della mod; le immagini passo-passo (`docs/img`) sono disegnate da `tools/disegna_*.py`.

## Struttura del repository
```
src/main/java/com/futuretek/   codice della mod (un pacchetto per argomento)
src/main/resources/            texture e logo
src/generated/resources/       risorse generate (JSON) - NON modificare a mano
src/main/templates/            scheda della mod (neoforge.mods.toml)
tools/                         script Python: risorse, texture, guide, controlli
scripts/                       launcher per Windows (.bat) e Linux/Mac (.sh)
docs/                          guide e documentazione
```
