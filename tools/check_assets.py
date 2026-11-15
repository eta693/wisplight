"""Vérifie la cohérence des assets : modèles → textures, états → modèles, traductions FR/EN,
sons référencés. Sort avec le code 1 en cas de problème."""
import glob
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "..", "src", "main", "resources")
A = os.path.join(RES, "assets", "wisplight")
JAVA = os.path.join(HERE, "..", "src", "main", "java")
errors = []


def load(p):
    with open(p, encoding="utf-8") as f:
        return json.load(f)


def res_path(ref, kind, ext):
    ns, path = ref.split(":", 1) if ":" in ref else ("minecraft", ref)
    return ns, os.path.join(RES, "assets", ns, kind, path + ext)


# Modèles → textures et parents
for m in glob.glob(os.path.join(A, "models", "**", "*.json"), recursive=True):
    data = load(m)
    for key, tex in data.get("textures", {}).items():
        if tex.startswith("#"):
            continue
        ns, p = res_path(tex, "textures", ".png")
        if ns == "wisplight" and not os.path.exists(p):
            errors.append(f"{os.path.relpath(m, A)} : texture manquante {tex}")
    parent = data.get("parent")
    if parent and parent.startswith("wisplight:"):
        ns, p = res_path(parent, "models", ".json")
        if not os.path.exists(p):
            errors.append(f"{os.path.relpath(m, A)} : parent manquant {parent}")

# États de blocs → modèles
for b in glob.glob(os.path.join(A, "blockstates", "*.json")):
    for v in load(b).get("variants", {}).values():
        for entry in v if isinstance(v, list) else [v]:
            ns, p = res_path(entry["model"], "models", ".json")
            if ns == "wisplight" and not os.path.exists(p):
                errors.append(f"blockstates/{os.path.basename(b)} : modèle manquant {entry['model']}")

# Noms enregistrés dans le code Java
java_src = "\n".join(open(f, encoding="utf-8").read() for f in glob.glob(os.path.join(JAVA, "**", "*.java"), recursive=True))
items = set(re.findall(r'ITEMS\.register\("([a-z0-9_]+)"', java_src))
items |= set(re.findall(r'(?:simple|block|armor)\("([a-z0-9_]+)"', java_src))
VARS = ("azur", "emeraude", "braise", "givre")
items_src = open(os.path.join(JAVA, "fr", "leho", "wisplight", "registry", "ModItems.java"), encoding="utf-8").read()
blocks_src = open(os.path.join(JAVA, "fr", "leho", "wisplight", "registry", "ModBlocks.java"), encoding="utf-8").read()
for prefix in re.findall(r'"([a-z_]+_)" \+ variant\.id\(\)', items_src):
    items |= {prefix + v for v in VARS}
items = {i for i in items if not i.endswith("_")}
blocks = {b for b in re.findall(r'(?<![A-Z_])BLOCKS\.register\("([a-z0-9_]+)"', java_src) if not b.endswith("_")}
for prefix in re.findall(r'"([a-z_]+_)" \+ variant\.id\(\)', blocks_src):
    blocks |= {prefix + v for v in VARS}
entities = set(re.findall(r'(?<![A-Z_])ENTITIES\.register\("([a-z0-9_]+)"', java_src))

langs = {l: load(os.path.join(A, "lang", l + ".json")) for l in ("fr_fr", "en_us")}
for it in sorted(items):
    if not os.path.exists(os.path.join(A, "models", "item", it + ".json")):
        errors.append(f"objet {it} : modèle d'objet manquant")
    for l, data in langs.items():
        if f"item.wisplight.{it}" not in data and f"block.wisplight.{it}" not in data:
            errors.append(f"objet {it} : traduction {l} manquante")
for bl in sorted(blocks):
    if not os.path.exists(os.path.join(A, "blockstates", bl + ".json")):
        errors.append(f"bloc {bl} : blockstate manquant")
    for l, data in langs.items():
        if f"block.wisplight.{bl}" not in data:
            errors.append(f"bloc {bl} : traduction {l} manquante")
for en in sorted(entities):
    for l, data in langs.items():
        if f"entity.wisplight.{en}" not in data:
            errors.append(f"entité {en} : traduction {l} manquante")

# Les deux langues ont les mêmes clés
fr, en = set(langs["fr_fr"]), set(langs["en_us"])
for k in sorted(fr ^ en):
    errors.append(f"clé de traduction présente dans une seule langue : {k}")

# Sons
sounds = load(os.path.join(A, "sounds.json"))
for event, spec in sounds.items():
    for s in spec["sounds"]:
        name = s if isinstance(s, str) else s["name"]
        ns, p = res_path(name, "sounds", ".ogg")
        if not os.path.exists(p):
            errors.append(f"son {event} : fichier manquant {name}")
    if spec.get("subtitle") and spec["subtitle"] not in langs["fr_fr"]:
        errors.append(f"son {event} : sous-titre non traduit")
sound_ids = set(re.findall(r'register\("((?:entity|block|item)\.[a-z0-9_.]+)"\)', java_src))
for sid in sound_ids - set(sounds):
    errors.append(f"son {sid} enregistré en Java mais absent de sounds.json")

if errors:
    print(f"{len(errors)} problème(s) :")
    for e in errors:
        print(" -", e)
    sys.exit(1)
print(f"Assets OK : {len(items)} objets, {len(blocks)} blocs, {len(entities)} entités, {len(sounds)} sons.")
