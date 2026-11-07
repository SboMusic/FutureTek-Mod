"""
Controlla che ogni blocco/oggetto registrato abbia modello, texture e traduzioni.
Uso: python tools/controlla_risorse.py   (dopo genera_risorse.py)
"""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
MODID = "futuretek"
GEN = ROOT / "src/generated/resources/assets" / MODID
MAIN = ROOT / "src/main/resources/assets" / MODID
errori = []


def trova(rel):
    for base in (GEN, MAIN):
        if (base / rel).exists():
            return base / rel
    return None


def controlla_modello(ref, visti):
    if ":" not in ref:
        ref = "minecraft:" + ref
    ns, path = ref.split(":", 1)
    if ns != MODID or ref in visti:
        return
    visti.add(ref)
    f = trova(f"models/{path}.json")
    if f is None:
        errori.append(f"modello mancante: {ref}")
        return
    dati = json.loads(f.read_text(encoding="utf-8"))
    if "parent" in dati:
        controlla_modello(dati["parent"], visti)
    for tex in dati.get("textures", {}).values():
        if tex.startswith("#"):
            continue
        tns, tpath = (tex.split(":", 1) if ":" in tex else ("minecraft", tex))
        if tns == MODID and trova(f"textures/{tpath}.png") is None:
            errori.append(f"texture mancante: {tex} (in {ref})")


lingua = {l: json.loads((GEN / f"lang/{l}.json").read_text(encoding="utf-8")) for l in ("en_us", "it_it")}
visti = set()
for riga in (ROOT / "tools/elenco_registro.txt").read_text().split("\n"):
    if not riga.strip():
        continue
    tipo, id_ = riga.split()
    chiave = f"{'block' if tipo == 'block' else 'item'}.{MODID}.{id_}"
    for l, d in lingua.items():
        if chiave not in d:
            errori.append(f"traduzione {l} mancante: {chiave}")
    if tipo == "block":
        bs = trova(f"blockstates/{id_}.json")
        if bs is None:
            errori.append(f"blockstate mancante: {id_}")
            continue
        dati = json.loads(bs.read_text(encoding="utf-8"))
        varianti = list(dati.get("variants", {}).values()) + [p["apply"] for p in dati.get("multipart", [])]
        for v in varianti:
            for vv in (v if isinstance(v, list) else [v]):
                controlla_modello(vv["model"], visti)
    if trova(f"models/item/{id_}.json") is None:
        errori.append(f"modello oggetto mancante: {id_}")
    else:
        controlla_modello(f"{MODID}:item/{id_}", visti)

if errori:
    print("\n".join(errori))
    print(f"ERRORI: {len(errori)}")
    sys.exit(1)
print(f"Tutto a posto: {len(visti)} modelli controllati.")
