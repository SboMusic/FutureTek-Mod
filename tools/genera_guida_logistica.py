"""
FutureTek - Guida "Magazzino automatico" (smistamento, terminale, crafting automatico, triplicazione minerali)
Scrive docs/guida_logistica.html usando le texture della mod come icone.
Uso (dalla cartella del progetto):  python tools/genera_guida_logistica.py
"""
import base64
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TEX = ROOT / "src/main/resources/assets/futuretek/textures"
OUT = ROOT / "docs/guida_logistica.html"


def uri(rel):
    return "data:image/png;base64," + base64.b64encode((TEX / rel).read_bytes()).decode()


ICONE = {
    "G": ("block/combustion_generator_front_on.png", "Generatore"),
    "w": ("block/tin_cable.png", "Cavo di stagno"),
    "K": ("block/logistics_core_front_on.png", "Nucleo logistico"),
    "M": ("block/iron_chest_front.png", "Baule PRINCIPALE"),
    "p": ("block/item_conduit.png", "Tubo logistico"),
    "D": ("block/sorting_conduit.png", "Tubo di destinazione (filtro)"),
    "N": ("block/analyzer_conduit.png", "Tubo analizzatore"),
    "C": ("block/iron_chest_front.png", "Baule"),
    "E": ("block/extractor_conduit.png", "Tubo estrattore"),
    "I": ("block/iron_chest_front.png", "Baule di arrivo"),
    "T": ("block/network_terminal_front.png", "Terminale di rete"),
    "A": ("block/auto_crafter_top.png", "Banco di assemblaggio"),
    "Q": ("block/quarry_front_on.png", "Escavatore / talpa"),
}
SEGNI = {"M": "1", "C": "", "I": "", "D": ""}


def schema(righe, etichette=None):
    """Disegna una piantina vista dall'alto: ogni lettera e' un blocco"""
    etichette = etichette or {}
    celle = []
    for y, riga in enumerate(righe):
        for x, ch in enumerate(riga.split()):
            if ch == ".":
                celle.append('<div class="cella vuota"></div>')
                continue
            src, nome = ICONE[ch]
            et = etichette.get((x, y), "")
            celle.append(f'<div class="cella" title="{nome}"><img src="{uri(src)}" alt="{nome}">'
                         + (f'<span class="et">{et}</span>' if et else "") + "</div>")
    colonne = len(righe[0].split())
    return f'<div class="schema" style="--col:{colonne}">' + "".join(celle) + "</div>"


def legenda(lettere):
    voci = []
    for ch in lettere:
        src, nome = ICONE[ch]
        voci.append(f'<li><img src="{uri(src)}" alt="">{nome}</li>')
    return '<ul class="legenda">' + "".join(voci) + "</ul>"


def catena(passi):
    out = []
    for i, (src, testo) in enumerate(passi):
        if i:
            out.append('<span class="freccia" aria-hidden="true">→</span>')
        out.append(f'<div class="passo"><img src="{uri(src)}" alt=""><span>{testo}</span></div>')
    return '<div class="catena">' + "".join(out) + "</div>"


SCHEMA_BASE = schema([
    ". . M . . . .",
    "G w K p p p p",
    ". . . D . N .",
    ". . . C . C .",
], {(2, 0): "1", (3, 3): "2", (5, 3): "3"})

SCHEMA_ENTRATA = schema([
    "I E p p",
    ". . . p",
    "Q p p p",
])

SCHEMA_CRAFT = schema([
    "M . . . .",
    "K p p p p",
    ". T . A .",
], {(1, 2): "", (3, 2): ""})

CATENA_MINERALI = catena([
    ("item/raw_tin.png", "1 grezzo"),
    ("block/chemical_bath_front_on.png", "Bagno chimico<br><small>+ acqua</small>"),
    ("item/purified_crushed_tin.png", "3 frantumati puri"),
    ("block/thermal_centrifuge_front_on.png", "Centrifuga termica"),
    ("item/tin_dust.png", "3 polveri"),
    ("block/electric_furnace_front_on.png", "Fornace / forno"),
    ("item/tin_ingot.png", "<b>3 lingotti</b>"),
])
CATENA_DOPPIA = catena([
    ("item/raw_tin.png", "1 grezzo"),
    ("block/crusher_front_on.png", "Frantumatore"),
    ("item/tin_dust.png", "2 polveri"),
    ("block/electric_furnace_front_on.png", "Fornace"),
    ("item/tin_ingot.png", "<b>2 lingotti</b>"),
])

HTML = f"""<title>FutureTek Magazzino automatico</title>
<meta name="claude:viewport-fit-sides" content="cover">
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Chakra+Petch:wght@500;700&family=IBM+Plex+Sans:wght@400;500;600&family=IBM+Plex+Mono:wght@500&display=swap">
<style>
/* Layout: manuale d'officina — una colonna di lettura, piantine "viste dall'alto" fatte con i blocchi veri della mod */
:root {{
  --bg: #e6e9ec; --panel: #f7f8f9; --ink: #1b212b; --muted: #5b6573; --line: #c9cfd6;
  --accent: #b5582a; --ok: #2f7d4f; --warn: #a1402b; --grid: #8d939b;
  --display: "Chakra Petch", "Segoe UI", system-ui, sans-serif;
  --body: "IBM Plex Sans", "Segoe UI", system-ui, sans-serif;
  --mono: "IBM Plex Mono", ui-monospace, Consolas, monospace;
}}
@media (prefers-color-scheme: dark) {{
  :root:not([data-theme="light"]) {{ --bg: #14171c; --panel: #1d2128; --ink: #e5e8ec; --muted: #9aa3ae; --line: #2f353e;
    --accent: #e0864c; --ok: #6cc28c; --warn: #f08a6a; --grid: #3a4048; color-scheme: dark; }}
}}
:root[data-theme="dark"] {{ --bg: #14171c; --panel: #1d2128; --ink: #e5e8ec; --muted: #9aa3ae; --line: #2f353e;
  --accent: #e0864c; --ok: #6cc28c; --warn: #f08a6a; --grid: #3a4048; color-scheme: dark; }}
* {{ box-sizing: border-box; }}
body {{ background: var(--bg); color: var(--ink); font: 16px/1.6 var(--body); }}
main {{ max-width: 860px; margin: 0 auto; padding-block: 28px 70px;
  padding-left: max(16px, env(safe-area-inset-left, 0px)); padding-right: max(16px, env(safe-area-inset-right, 0px)); }}
h1 {{ font: 700 30px/1.15 var(--display); margin: 0 0 6px; text-wrap: balance; }}
.sotto {{ color: var(--muted); margin: 0 0 22px; max-width: 65ch; }}
h2 {{ font: 700 21px/1.25 var(--display); margin: 40px 0 8px; text-wrap: balance; display: flex; gap: 10px; align-items: baseline; }}
h2 .n {{ font: 500 13px var(--mono); color: var(--accent); }}
h3 {{ font: 600 16px var(--body); margin: 20px 0 6px; }}
p, li {{ max-width: 68ch; }}
nav {{ display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 8px; }}
nav a {{ font-size: 13px; color: var(--ink); text-decoration: none; border: 1px solid var(--line); border-radius: 999px; padding: 4px 10px; background: var(--panel); }}
nav a:hover, nav a:focus-visible {{ border-color: var(--accent); color: var(--accent); outline: none; }}
.riquadro {{ background: var(--panel); border: 1px solid var(--line); border-radius: 8px; padding: 14px 16px; margin: 14px 0; }}
.schema {{ display: grid; grid-template-columns: repeat(var(--col), 48px); gap: 2px; padding: 8px; background: var(--grid); border-radius: 6px; width: max-content; max-width: 100%; overflow-x: auto; }}
.cella {{ width: 48px; height: 48px; position: relative; background: rgba(0,0,0,.12); }}
.cella img {{ width: 48px; height: 48px; image-rendering: pixelated; display: block; }}
.cella.vuota {{ background: rgba(255,255,255,.06); }}
.et {{ position: absolute; right: -4px; top: -6px; background: var(--accent); color: #fff; font: 700 12px var(--mono); border-radius: 999px; width: 20px; height: 20px; display: grid; place-items: center; }}
.legenda {{ list-style: none; padding: 0; margin: 10px 0 0; display: flex; flex-wrap: wrap; gap: 6px 16px; font-size: 14px; color: var(--muted); }}
.legenda img {{ width: 20px; height: 20px; image-rendering: pixelated; vertical-align: -5px; margin-right: 6px; }}
ol.passi {{ padding-left: 22px; }} ol.passi li {{ margin: 6px 0; }}
kbd {{ font: 500 13px var(--mono); border: 1px solid var(--line); border-bottom-width: 2px; border-radius: 4px; padding: 0 5px; background: var(--panel); }}
.regole {{ counter-reset: r; list-style: none; padding: 0; }}
.regole li {{ counter-increment: r; position: relative; padding-left: 38px; margin: 10px 0; }}
.regole li::before {{ content: counter(r); position: absolute; left: 0; top: 0; width: 26px; height: 26px; border-radius: 6px; background: var(--accent); color: #fff; font: 700 14px var(--mono); display: grid; place-items: center; }}
.catena {{ display: flex; flex-wrap: wrap; align-items: center; gap: 8px; }}
.passo {{ display: flex; flex-direction: column; align-items: center; gap: 4px; font-size: 13px; text-align: center; width: 92px; }}
.passo img {{ width: 40px; height: 40px; image-rendering: pixelated; }}
.freccia {{ color: var(--muted); font-size: 20px; }}
table {{ border-collapse: collapse; width: 100%; font-size: 14px; }}
td, th {{ text-align: left; padding: 7px 8px; border-bottom: 1px solid var(--line); vertical-align: top; }}
th {{ font-weight: 600; }}
.tabella {{ overflow-x: auto; }}
.ok {{ color: var(--ok); font-weight: 600; }} .no {{ color: var(--warn); font-weight: 600; }}
</style>
<main>
<h1>FutureTek Magazzino automatico</h1>
<p class="sotto">Come costruire passo per passo il sistema che smista da solo gli oggetti nei bauli, ti mostra tutto in un terminale e fabbrica in automatico. Le piantine sono viste dall'alto: ogni quadretto è un blocco.</p>
<nav>
  <a href="#cosa-serve">Cosa serve</a><a href="#smistamento">Smistamento</a><a href="#regole">Dove va un oggetto</a>
  <a href="#entrata">Far entrare la roba</a><a href="#terminale">Terminale</a><a href="#crafting">Crafting automatico</a>
  <a href="#problemi">Se non funziona</a><a href="#minerali">Bagno chimico</a><a href="#escavatore">Escavatore</a>
</nav>

<h2 id="cosa-serve"><span class="n">00</span>Cosa serve</h2>
<div class="riquadro tabella"><table>
<tr><th>Blocco</th><th>Ricetta (in breve)</th><th>Quanti</th></tr>
<tr><td>Generatore a combustione (o pannelli solari)</td><td>vedi Ricettario, sezione Energia</td><td>1</td></tr>
<tr><td>Cavo di stagno</td><td>3 fili di stagno → 6 cavi</td><td>quanti ne servono</td></tr>
<tr><td>Nucleo logistico</td><td>piastre di ferro, perla dell'End, 2 circuiti, telaio macchina, tubo logistico</td><td>1</td></tr>
<tr><td>Tubo logistico</td><td>pietra + vetro + pietra → 8</td><td>tanti</td></tr>
<tr><td><b>Tubo analizzatore</b> (consigliato)</td><td>tubo + osservatore + polvere di redstone</td><td>1 per ogni baule del magazzino</td></tr>
<tr><td>Tubo di destinazione (filtro)</td><td>tubo + lingotto di ferro + comparatore</td><td>solo se ti serve un filtro preciso</td></tr>
<tr><td>Tubo estrattore</td><td>tubo + tramoggia</td><td>1 per ogni baule "di arrivo"</td></tr>
<tr><td>Bauli (meglio quelli grandi FutureTek)</td><td>baule di rame = 8 lingotti di rame + baule</td><td>quanti vuoi</td></tr>
<tr><td>Terminale di rete, Banco di assemblaggio</td><td>vedi Ricettario, sezione Logistica</td><td>1 + 1</td></tr>
</table></div>

<h2 id="smistamento"><span class="n">01</span>Lo smistamento: costruzione</h2>
<div class="riquadro">
{SCHEMA_BASE}
{legenda("GwKMpDNC")}
</div>
<ol class="passi">
<li><b>Energia.</b> Metti il generatore e collega il <b>Nucleo logistico</b> con un cavo. Il nucleo consuma poco: 2 TU/t, più 2 TU per ogni oggetto spostato.</li>
<li><b>Baule principale ①.</b> Metti un baule che <b>tocca il nucleo</b>, il più grande possibile. È il "magazzino generale": tutto quello che non ha un posto preciso finisce qui.</li>
<li><b>Tubi.</b> Dal nucleo fai partire una fila di <b>tubi logistici</b>. I tubi si collegano da soli fra loro e ai bauli che toccano.</li>
<li><b>Baule con filtro ②.</b> Metti un <b>tubo di destinazione</b> attaccato ai tubi e un baule che lo tocca. Fai <b>click destro sul tubo di destinazione</b>: si apre il filtro con 9 caselle. Prendi in mano un oggetto, per esempio un diamante, e clicca una casella per aggiungerlo. Per toglierlo, clicca la casella a mano vuota. Quel baule riceverà <b>solo</b> gli oggetti del filtro.</li>
<li><b>Baule del magazzino ③ (il modo consigliato).</b> Metti un <b>tubo analizzatore</b> e un baule che lo tocca. Dentro al baule metti a mano <b>1 oggetto di esempio</b> per ogni tipo che vuoi lì, per esempio 1 pietrisco, 1 terra e 1 ghiaia. Il tubo <b>impara</b> da solo quegli oggetti entro 2 secondi e da quel momento il nucleo ci manda tutto il pietrisco, la terra e la ghiaia. Se ne aggiungi altri a mano, impara anche quelli.
  <ul>
  <li>Il tubo <b>si ricorda</b> gli oggetti anche se svuoti il baule, così il magazzino non "si rompe".</li>
  <li>Click destro sul tubo: <b>Lascia 1: SÌ</b> fa sì che il terminale e il crafting automatico lascino sempre 1 oggetto di ogni tipo nel baule. <b>Dimentica</b> cancella la memoria e reimpara da quello che c'è adesso. Le 9 caselle sono un filtro extra, se vuoi aggiungere oggetti senza metterli nel baule.</li>
  <li>Un baule che tocca solo un tubo normale funziona in modo simile, ma senza memoria: se lo svuoti del tutto, perde la "prenotazione".</li>
  </ul></li>
<li><b>Prova.</b> Fai click destro sul nucleo: deve dire "Nucleo logistico attivo". Poi butta un po' di roba nel baule principale ①: i diamanti partono verso ②, il pietrisco verso ③, e li vedi viaggiare nei tubi.</li>
</ol>

<h2 id="regole"><span class="n">02</span>Dove va un oggetto (in quest'ordine)</h2>
<ol class="regole">
<li><b>Nei bauli con un filtro o un analizzatore</b> che lo accetta (tubo di destinazione o tubo analizzatore).</li>
<li><b>Nei bauli che lo contengono già.</b> Per questo basta 1 oggetto di esempio per "prenotare" un baule.</li>
<li><b>Altrimenti resta nel baule principale.</b> Senza nucleo, invece, va in un baule libero qualsiasi.</li>
</ol>
<p>Ogni mezzo secondo il nucleo guarda nel baule principale e manda via ciò che ha trovato posto. Se un baule di destinazione si riempie, il resto aspetta nel principale.</p>

<h2 id="entrata"><span class="n">03</span>Far entrare la roba da sola</h2>
<div class="riquadro">
{SCHEMA_ENTRATA}
{legenda("IEpQ")}
</div>
<ul>
<li><b>Baule di arrivo + tubo estrattore:</b> l'estrattore tira fuori 8 oggetti al secondo dal baule che tocca e li manda nella rete. Il tubo estrattore veloce ne tira fuori 64. Attento: l'estrattore svuota <b>tutti</b> i contenitori che tocca, quindi deve toccare solo il baule di arrivo.</li>
<li><b>Escavatore, talpa e macchine</b> collegati a un tubo mettono direttamente nella rete quello che producono.</li>
<li>Un segnale di <b>redstone</b> sull'estrattore lo mette in pausa.</li>
</ul>

<h2 id="terminale"><span class="n">04</span>Il terminale: vedere e prendere tutto</h2>
<div class="riquadro">
{SCHEMA_CRAFT}
{legenda("KMpTA")}
</div>
<ol class="passi">
<li>Metti il <b>Terminale di rete</b> in modo che tocchi un tubo, in qualsiasi punto della rete.</li>
<li>Click destro: vedi <b>tutti</b> gli oggetti di <b>tutti</b> i bauli collegati, con la quantità. In alto c'è la casella di <b>ricerca</b>.</li>
<li><b>Click</b> su un oggetto = prendi una pila · <b>click destro</b> = prendine 1 · rotellina del mouse = scorri.</li>
</ol>

<h2 id="crafting"><span class="n">05</span>Il crafting automatico</h2>
<ol class="passi">
<li>Metti il <b>Banco di assemblaggio</b> in modo che tocchi un tubo della rete.</li>
<li>Aprilo e disegna la ricetta nella griglia 3×3: con l'oggetto in mano clicca le caselle, che restano "fantasma" (l'oggetto non viene consumato). A mano vuota cancelli una casella.</li>
<li><b>A richiesta:</b> premi <kbd>+1</kbd> o <kbd>+16</kbd> per mettere in coda le fabbricazioni. Il banco prende gli ingredienti dai bauli della rete e rimette il risultato nella rete, quindi nel baule giusto secondo le regole del punto 02.</li>
<li><b>Dal terminale:</b> gli oggetti che si possono fabbricare hanno un <b>+</b> verde. <kbd>click centrale</kbd> o <kbd>Ctrl</kbd>+<kbd>click</kbd> ne chiede 1, con <kbd>Shift</kbd> ne chiede 64.</li>
<li><b>Scorta automatica:</b> con <kbd>+</kbd> e <kbd>-</kbd> imposti per esempio "Scorta: 64". Ogni 2 secondi il banco controlla e, se nella rete ce ne sono meno di 64, ne fabbrica da solo. <kbd>0</kbd> la spegne.</li>
<li><b>A catena:</b> metti più banchi, per esempio uno per le assi e uno per i bastoncini. Se chiedi bastoncini e hai solo tronchi, la rete fa prima le assi e poi i bastoncini.</li>
</ol>

<h2 id="problemi"><span class="n">06</span>Se non funziona</h2>
<div class="riquadro tabella"><table>
<tr><th>Cosa succede</th><th>Perché / cosa fare</th></tr>
<tr><td>Gli oggetti restano nel baule principale</td><td>Non hanno una destinazione: aggiungi un filtro o 1 oggetto di esempio in un baule. Oppure il nucleo è senza energia: fai click destro sul nucleo per controllare.</td></tr>
<tr><td>Un baule non riceve niente</td><td>Non tocca un tubo, oppure lo tocca un tubo di destinazione con il filtro vuoto o sbagliato.</td></tr>
<tr><td>L'estrattore svuota il baule sbagliato</td><td>Tocca più contenitori: spostalo in modo che tocchi solo il baule di arrivo.</td></tr>
<tr><td>Il banco di assemblaggio non fabbrica</td><td>Mancano ingredienti nella rete (anche 1 solo), la ricetta non è valida, oppure non c'è posto per il risultato.</td></tr>
<tr><td>Non vedo gli oggetti viaggiare</td><td>L'animazione si vede solo entro 48 blocchi e con percorsi fino a 96 tubi; lo spostamento avviene comunque.</td></tr>
</table></div>

<h2 id="minerali"><span class="n">07</span>A cosa serve il bagno chimico</h2>
<p>Serve a <b>triplicare i minerali</b>. Con la fornace 1 grezzo dà 1 lingotto, con il frantumatore 2, con il bagno chimico e la centrifuga termica <b>3</b>.</p>
<div class="riquadro">
<h3>Raddoppio (facile)</h3>
{CATENA_DOPPIA}
<h3>Triplicazione</h3>
{CATENA_MINERALI}
</div>
<ul>
<li>Il bagno chimico ha bisogno di <b>acqua</b>: click destro con un secchio d'acqua (8 secchi lo riempiono). Ogni lavorazione ne usa 250 mB. L'<b>Amuleto delle Maree</b> lo riempie all'infinito.</li>
<li>Tutte e due le macchine vogliono energia. Puoi collegarle con i tubi: il bagno manda i frantumati nella centrifuga e la centrifuga manda le polveri nel forno.</li>
</ul>

<h2 id="escavatore"><span class="n">08</span>L'escavatore</h2>
<ul>
<li><b>Area:</b> un segnalino accanto all'escavatore e uno nell'angolo opposto, alla stessa altezza. L'escavatore costruisce un telaio a barre sottili (anello in basso, 4 colonne e anello in alto) e un carrello che scorre in alto e cala la trivella. Se rompi l'escavatore, il telaio sparisce.</li>
<li><b>Uscita:</b> un baule o un tubo che lo tocca, meglio <b>sopra</b>. Se non c'è posto, sputa gli oggetti fuori dall'alto e <b>non si ferma</b>.</li>
<li><b>Velocità:</b> ogni blocco costa 64 TU e più energia gli arriva, più va veloce, fino a 8 blocchi per tick. Con un pannello solare normale (4 TU/t) scava 1 blocco ogni 0,8 secondi; con due pannelli solari III (256 TU/t) ne scava 4 per tick.</li>
<li>Click destro mostra lo stato: "scavo lo strato Y=…", "aspetto energia", "in pausa (redstone)", "sputo gli oggetti".</li>
</ul>
</main>
"""

if __name__ == "__main__":
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(HTML, encoding="utf-8")
    print("Guida scritta in", OUT)
