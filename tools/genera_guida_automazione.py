"""
FutureTek - Guida "Casa automatica, Nether e robot" (Fasi 12-14)
Scrive docs/guida_automazione.html usando le texture della mod (stesso stile della guida logistica).
Uso (dalla cartella del progetto):  python tools/genera_guida_automazione.py
"""
import os
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import genera_guida_logistica as gl  # noqa: E402

gl.ICONE.update({
    "w": ("block/tin_cable.png", "Cavo di stagno"),
    "S": ("block/energy_switch_front_on.png", "Interruttore elettrico (con leva)"),
    "L": ("block/electric_lamp_on.png", "Lampada elettrica"),
    "P": ("block/light_panel_on.png", "Pannello luminoso"),
    "G": ("block/combustion_generator_front_on.png", "Generatore / pannello solare"),
    "F": ("block/tree_farm_front_on.png", "Fattoria di alberi"),
    "a": ("item/oak_sapling.png" if (gl.TEX / "item/oak_sapling.png").exists() else "block/tree_farm_top.png", "Alberello (lo pianta la macchina)"),
    "C": ("block/iron_chest_front.png", "Baule"),
    "H": ("block/harvester_front_on.png", "Raccoglitore"),
    "N": ("block/planter_front_on.png", "Piantatrice"),
    "B": ("block/auto_fertilizer_front_on.png", "Fertilizzatore automatico"),
    "I": ("block/mob_incubator_front_on.png", "Incubatore di creature"),
    "T": ("block/laser_turret_front_on.png", "Torretta laser"),
    "R": ("block/robot_dock_front_on.png", "Base di ricarica"),
    "K": ("block/reactor_core_front_on.png", "Nucleo del reattore"),
    "H": ("block/reactor_chamber.png", "Camera del reattore"),
    "Z": ("block/reactor_maintainer_front_on.png", "Manutentore del reattore"),
    "W": ("block/electric_lamp.png", "Leva / segnale redstone"),
    "A": ("block/iron_chest_front.png", "Tramoggia o tubo con i pezzi di ricambio"),
})
schema, legenda = gl.schema, gl.legenda

CSS = re.search(r"<style>.*?</style>", gl.HTML, re.S).group(0)
FONTS = '''<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Chakra+Petch:wght@500;700&family=IBM+Plex+Sans:wght@400;500;600&family=IBM+Plex+Mono:wght@500&display=swap">'''


def foto(nome):
    import base64
    with open(os.path.join(gl.ROOT, "docs/img", nome + ".png"), "rb") as f:
        b = base64.b64encode(f.read()).decode()
    return f'<figure class="foto"><img src="data:image/png;base64,{b}" alt=""></figure>'


def ico(rel):
    return f'<img class="i" src="{gl.uri(rel)}" alt="">'


LUCI = schema([
    "G w S w L L L",
    ". . . . . . L",
    ". . . . P P L",
])
ALBERI = schema([
    "a . . a . . a",
    ". . . . . . .",
    ". . . . . . .",
    "a . . F . . a",
    ". . . C . . .",
    ". . . . . . .",
    "a . . a . . a",
])
CAMPO = schema([
    ". . . . .",
    ". N . H .",
    ". . B C .",
    ". . . . .",
])
MOSTRI = schema([
    ". . . . .",
    ". I . T .",
    ". . . . .",
])
ROBOT = schema(["G w R C"])
REATTORE = schema([
    ". . H . .",
    ". H K H w",
    ". W H Z A",
])

HTML = f"""<title>FutureTek Casa e robot</title>
<meta name="claude:viewport-fit-sides" content="cover">
{FONTS}
{CSS}
<style>
.foto{{margin:18px 0;text-align:center}}.foto img{{max-width:100%;height:auto;border-radius:10px;image-rendering:auto;box-shadow:0 2px 12px rgba(0,0,0,.35)}}
.nota{{font-size:.95em;opacity:.85;border-left:3px solid currentColor;padding-left:10px}}
.i {{ width: 20px; height: 20px; image-rendering: pixelated; vertical-align: -5px; margin-right: 4px; }}
</style>
<main>
<h1>FutureTek Casa e robot</h1>
<p class="sotto">Le macchine delle Fasi 12-14: luci elettriche, fattorie automatiche, incantatore, fattoria di mostri e robot. Le piantine sono viste dall'alto: ogni quadretto è un blocco.</p>
<nav>
  <a href="#luci">Luci</a><a href="#alberi">Fattoria di alberi</a><a href="#campo">Campo automatico</a>
  <a href="#incanti">Incantatore</a><a href="#mostri">Fattoria di mostri</a><a href="#robot">Robot</a>
  <a href="#reattore">Reattore</a><a href="#tnt">TNT nucleare</a><a href="#fabbrica">Fabbrica abbandonata</a><a href="#rifugio">Rifugio portatile</a>
</nav>

<h2 id="luci"><span class="n">01</span>Luci elettriche</h2>
<div class="riquadro">{LUCI}{legenda("GwSLP")}</div>
<ol class="passi">
<li>Collega con un cavo il generatore all'<b>interruttore elettrico</b> e mettici sopra (o accanto) una <b>leva</b>.</li>
<li>Dal <b>davanti</b> dell'interruttore esce un cavo che arriva a <b>una</b> lampada. Le lampade e i pannelli luminosi che si toccano si passano l'energia da soli, quindi basta una fila continua, anche su pareti e soffitto.</li>
<li>Leva su ON: le luci si accendono. Leva su OFF: si spengono e smettono di consumare.</li>
<li>Ogni luce consuma <b>2 TU/t</b>: 10 lampade = 20 TU/t, più di quanto fa un generatore a combustione (12). Con poca energia conviene usarne poche, o tornare alle torce.</li>
</ol>

<h2 id="alberi"><span class="n">02</span>Fattoria di alberi</h2>
<div class="riquadro">{ALBERI}{legenda("FaC")}</div>
<ul>
<li>Mettila al centro di un prato o di terra piatta (9x9). Pianta gli alberelli <b>uno ogni 3 blocchi</b>, al suo stesso livello.</li>
<li>Gli alberelli vanno nelle <b>prime 4 caselle</b>. Quelli che cadono dalle foglie tornano lì da soli.</li>
<li>Quando un albero cresce lo taglia tutto, tronco e foglie, e mette il legno nel baule accanto (o nei tubi).</li>
<li>Taglia solo i tronchi con foglie naturali vicino, quindi le case di legno sono al sicuro.</li>
<li>Con un <b>fertilizzatore automatico</b> accanto (farina d'ossa) gli alberi crescono molto più in fretta.</li>
</ul>

<h2 id="campo"><span class="n">03</span>Campo automatico</h2>
<div class="riquadro">{CAMPO}{legenda("NHBC")}</div>
<ol class="passi">
<li><b>Piantatrice</b> con i semi dentro: zappa la terra intorno (9x9) e pianta. Funziona con grano, carote, patate, barbabietole, zucche e angurie. Metti un po' d'acqua vicino, se no la terra si asciuga.</li>
<li><b>Fertilizzatore automatico</b> con la farina d'ossa: fa crescere le piante (60 TU a colpo).</li>
<li><b>Raccoglitore</b>: raccoglie quello che è maturo, ripianta e mette il raccolto nel baule che tocca.</li>
<li>Tutte e tre lavorano sullo stesso quadrato 9x9 intorno a sé, quindi mettile vicine.</li>
</ol>

<h2 id="incanti"><span class="n">04</span>Incantatore elettrico</h2>
<p>Metti l'oggetto nella prima casella e i lapislazzuli nella seconda, poi scegli il grado:</p>
<div class="riquadro tabella"><table>
<tr><th>Grado</th><th>Come un tavolo con</th><th>Livelli</th><th>Lapislazzuli</th><th>Energia</th></tr>
<tr><td>I</td><td>potenza 10</td><td>2</td><td>1</td><td>25.000 TU</td></tr>
<tr><td>II</td><td>potenza 20</td><td>5</td><td>2</td><td>75.000 TU</td></tr>
<tr><td>III</td><td>potenza 30 (tutte le librerie)</td><td>9</td><td>3</td><td>200.000 TU</td></tr>
</table></div>
<p>Il tavolo normale chiede di avere 30 livelli per l'incanto migliore; qui ne bastano 9, ma serve tanta energia: la batteria interna tiene 250.000 TU.</p>

<h2 id="mostri"><span class="n">05</span>Fattoria di mostri (dopo il Nether)</h2>
<p>Prima ti serve il {ico("item/soul_core.png")}<b>Nucleo d'anima</b>: nel Nether prendi <b>sabbia delle anime, quarzo, polvere di blaze e lacrime di ghast</b>. Serve per fare il campionatore e l'incubatore.</p>
{foto("fattoria_0_serve")}
{foto("fattoria_1_campione")}
<ol class="passi">
<li>Tieni in mano il {ico("item/mob_sampler.png")}<b>Campionatore di creature</b> e fai <b>click destro</b> su un mob (mucca, zombie, scheletro...). Diventa luccicante. I boss non si possono campionare.</li>
</ol>
{foto("fattoria_2_incubatore")}
<ol class="passi" start="2">
<li>Metti l'<b>Incubatore</b> per terra, in un posto all'aperto e piatto.</li>
<li>Metti un <b>Pannello solare III sopra</b> l'incubatore e un altro <b>attaccato di lato</b>. Niente cavi: il pannello passa l'energia ai blocchi che tocca.</li>
<li>Click destro sull'incubatore e metti il campionatore pieno nel suo slot. Le creature compaiono nell'area <b>gialla 5x5</b> intorno. Ognuna costa 6.000 TU: di giorno ne arriva una ogni pochi secondi, di notte più piano. Mai più di 6 vicine.</li>
</ol>
{foto("fattoria_3_torretta")}
<ol class="passi" start="5">
<li>Metti la <b>Torretta laser attaccata</b> all'incubatore (proprio a contatto, così la sua zona copre tutta l'area gialla).</li>
<li>Metti un <b>baule sopra la torretta</b> e un <b>Pannello solare III</b> attaccato dietro la torretta.</li>
<li>Click destro sulla torretta. Per le mucche e gli altri animali premi <b>Cambia bersagli</b> finché c'è scritto <b>Tutte le creature</b>. Per zombie e scheletri va bene <b>Solo mostri</b>.</li>
</ol>
{foto("fattoria_4_finita")}
<ol class="passi" start="8">
<li>Fatto! La torretta colpisce le creature nella zona rossa 7x7. Muoiono come se le avessi uccise tu e ogni secondo la torretta mette i loro oggetti <b>nel baule</b>. L'esperienza resta per terra: stai vicino per prenderla.</li>
</ol>
<p class="nota">Non colpisce mai giocatori, animali domestici, abitanti, robot o creature con un nome. Se non succede niente: controlla che il campionatore sia luccicante, che i pannelli vedano il cielo e che sia giorno.</p>

<h2 id="robot"><span class="n">06</span>I robot</h2>
<div class="riquadro">{ROBOT}{legenda("GwRC")}</div>
<h3>Accendere un robot</h3>
<ol class="passi">
<li>Costruisci la <b>Base di ricarica</b> e collegala all'energia con un cavo. Accanto mettici un <b>baule</b>: è il baule del robot.</li>
<li>Fai il {ico("item/robot.png")}<b>Robot</b> e fai click destro <b>sulla base</b>. Da quel momento usa <b>sempre quella base</b> per ricaricarsi e quel baule per scaricare.</li>
<li><b>Click destro sul robot a mano vuota</b> cambia lavoro. Fai <b>click destro sul robot con un attrezzo</b> per darglielo, e lui ti ridà quello che aveva.</li>
</ol>
<h3>I lavori</h3>
<div class="riquadro tabella"><table>
<tr><th>Lavoro</th><th>Attrezzo</th><th>Cosa fa</th></tr>
<tr><td><b>Minatore</b></td><td>piccone</td><td>Parte dalla base, dalla parte opposta al baule, e scava una <b>scala</b> verso il basso fino a sopra la bedrock. Poi scava gallerie a serpentina (32 blocchi avanti, 4 di lato, 32 indietro…). Prende tutti i minerali che vede, segue le vene, tappa acqua e lava e mette un blocco dove manca il pavimento. Se nel baule ci sono delle torce, le prende e ne mette una ogni 10 passi. Per scaricare risale dalla sua scala.</td></tr>
<tr><td><b>Taglialegna</b></td><td>ascia</td><td>Abbatte gli alberi entro 12 blocchi dalla base, raccoglie gli alberelli e li ripianta (ne prende anche dal baule).</td></tr>
<tr><td><b>Contadino</b></td><td>zappa</td><td>Raccoglie le colture mature entro 12 blocchi dalla base e ripianta.</td></tr>
<tr><td><b>Guardia del corpo</b></td><td>(spada facoltativa)</td><td>Ti segue e attacca i mostri.</td></tr>
<tr><td><b>Sentinella</b></td><td>(spada facoltativa)</td><td>Difende la zona della base.</td></tr>
</table></div>
<ul>
<li>Rompe i blocchi col tempo giusto per l'attrezzo, come un giocatore: col piccone di diamante va più veloce che con quello di pietra. Ogni blocco consuma l'attrezzo.</li>
<li><b>Attrezzo rotto:</b> va al baule e ne prende un altro dello stesso tipo. Se non ce ne sono, aspetta alla base finché non ne metti uno nel baule.</li>
<li><b>Zaino pieno:</b> torna al baule e lascia tutto (tiene solo qualche blocco di pietra, le torce, gli alberelli e i semi che gli servono).</li>
<li><b>Baule pieno:</b> si ferma alla base. Svuota il baule e poi digli di <b>continuare</b>, con un click destro sul robot o col pannello.</li>
<li><b>Energia bassa (sotto il 25%):</b> torna alla base, si ricarica e riparte da dove era arrivato.</li>
</ul>
<h3>Il pannello di controllo</h3>
<ol class="passi">
<li>Fai il {ico("item/robot_controller.png")}<b>Pannello di controllo robot</b> e fai click destro sul robot per <b>collegarlo</b>.</li>
<li>Click destro su un <b>baule qualsiasi, anche lontano</b>, per esempio il baule principale del magazzino automatico: il robot porterà tutto lì e la logistica lo smisterà e lo cuocerà da sola.</li>
<li>Click destro su un'altra base di ricarica per spostargli la base.</li>
<li>Click destro <b>in aria</b> per vedere lo stato: lavoro, cosa sta facendo, energia, usi rimasti dell'attrezzo e posizione. <b>Shift + click destro in aria</b> gli dice di <b>continuare</b>.</li>
</ol>
<ul>
<li>Con il {ico("item/infinite_core.png")}<b>Nucleo infinito</b> (click destro sul robot) non si scarica mai.</li>
<li><b>Shift + click destro</b> a mano vuota: riprendi il robot come oggetto, e lui lascia per terra attrezzo e zaino.</li>
</ul>

<h2 id="reattore"><span class="n">07</span>Il reattore nucleare</h2>
<div class="riquadro">{REATTORE}{legenda("KHZwWA")}</div>
<ol class="passi">
<li>Il <b>Nucleo del reattore</b> da solo ha una griglia di 6 righe per 3 colonne. Ogni <b>Camera del reattore</b> che lo tocca, sopra, sotto o di lato, aggiunge una colonna: con 6 camere arrivi a 9 colonne, cioè 54 caselle.</li>
<li>Si accende <b>solo con un segnale redstone</b>, per esempio una leva. Togli il segnale per spegnerlo.</li>
<li>Le <b>barre di uranio</b> fanno energia. Più barre sono vicine, più energia fanno, ma il calore sale <b>molto</b> di più.</li>
<li>Il calore va ai pezzi accanto alle barre:
  <ul>
  <li><b>Dissipatore</b>: smaltisce 12 calore/s e dura per sempre, se non si surriscalda.</li>
  <li><b>Cella refrigerante 10k / 30k / 60k</b>: assorbe il calore ma <b>si consuma</b>. Quando è piena diventa "esausta": ricaricala con un secchio d'acqua.</li>
  <li><b>Piastra del reattore</b>: +1.000 calore massimo.</li>
  </ul></li>
<li>Se nessun pezzo prende il calore, va nello <b>scafo</b>. Oltre il 70% suona l'allarme ed esce fumo, oltre l'85% prende fuoco quello che c'è intorno. Al <b>100% il reattore ESPLODE</b> con un cratere nucleare: niente spegnimento automatico.</li>
<li>Il <b>Manutentore del reattore</b>, attaccato al nucleo o a una camera, cambia da solo le celle oltre il 90% di calore, i dissipatori consumati e le barre esaurite, prendendo i pezzi nuovi dalle sue 16 caselle. Mettili con una tramoggia o un tubo. Se il manutentore <b>tocca l'acqua</b>, ricarica da solo le celle esauste: così il giro non si ferma mai.</li>
</ol>

<h2 id="tnt"><span class="n">08</span>TNT nucleare</h2>
<ul>
<li>Si fa con 4 lingotti di uranio, 4 TNT e un circuito avanzato.</li>
<li>Si accende come la TNT: acciarino, redstone, fuoco o un'altra esplosione. La miccia dura <b>10 secondi</b> e si sentono i rintocchi: allontanati di almeno 50 blocchi.</li>
<li>Cancella una <b>sfera di raggio 32</b>. I bauli dentro spariscono con tutto il contenuto e restano solo la bedrock e qualche fuoco sul fondo.</li>
<li>Nella config si può cambiare il raggio (<code>nukeRadius</code>) o spegnerla (<code>nukesEnabled = false</code>).</li>
</ul>

<h2 id="fabbrica"><span class="n">09</span>La fabbrica abbandonata</h2>
<ul>
<li>Quando crei un mondo nuovo, vicino al punto di nascita compare un capannone in rovina con <b>una copia di ogni macchina della mod</b>, divisa in reparti (Energia, Lavorazione, Logistica, Agricoltura, Miniera, Nucleare, Avanzate, Alchimia), con un cartello su ogni macchina.</li>
<li>È "in disuso": le macchine sono spente, un cavo su otto manca e i generatori non hanno carbone. Vicino all'entrata c'è un baule con carbone, cavi, lingotti, leve, semi e alberelli per ripartire.</li>
<li>Quando entri nel mondo per la prima volta, un messaggio ti dice le coordinate dell'entrata.</li>
<li>Per non averla: crea il mondo, apri <b>Altre opzioni del mondo → Regole di gioco</b> e spegni "FutureTek: fabbrica abbandonata".</li>
<li>Nei mondi che hai già, o per le prove, usa il comando <code>/futuretek fabbrica</code>: ne costruisce una davanti a te.</li>
<li>La fabbrica si costruisce leggendo l'elenco delle macchine della mod: quando ne aggiungiamo di nuove, ci sono già.</li>
</ul>

<h2 id="rifugio"><span class="n">10</span>Rifugio portatile</h2>
<p>Una "tenda" elettrica: quando esplori lontano e non hai un letto o i materiali, lo appoggi per terra e in un attimo hai una casetta sicura.</p>
{foto("rifugio_fuori")}
<ol class="passi">
<li>Fai il {ico("block/shelter_core.png")}<b>Rifugio portatile</b>: piastre di ferro, lana, fornace, banco da lavoro, circuito avanzato e cella di energia.</li>
<li><b>Caricalo</b> nel banco accumulatore, come una cella di energia. Pieno = <b>6 case</b> (25.000 TU ciascuna). Il tooltip dice quante case restano.</li>
<li>Su un terreno libero e piatto (7x7, alto 4) fai click destro col rifugio in mano: il nucleo va a terra e la casa si costruisce intorno, con la <b>porta verso di te</b>. Erba, fiori e foglie vengono tolti; se il terreno ha dei buchi, mette lui il pavimento.</li>
</ol>
{foto("rifugio_dentro")}
<ol class="passi" start="4">
<li>Dentro ci sono <b>letto, banco da lavoro e fornace</b>, e il nucleo fa luce, così i mostri non compaiono. Muri, tetto e finestre non si possono rompere né far esplodere.</li>
<li>Per ripartire <b>rompi il nucleo</b> (anche a mano): la casa sparisce, il terreno resta com'era e il nucleo torna nell'inventario con la carica rimasta. Se nella fornace c'era qualcosa cade per terra.</li>
<li>Letto, banco, fornace e porta non si possono prendere: sono "in prestito" finché la casa esiste.</li>
</ol>
</main>
"""

if __name__ == "__main__":
    out = gl.ROOT / "docs/guida_automazione.html"
    out.write_text(HTML, encoding="utf-8")
    print("Guida scritta in", out)
