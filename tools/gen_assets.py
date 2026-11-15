"""Génère tous les fichiers JSON du mod (modèles, états de blocs, loot tables, recettes,
tags, génération du monde, traductions, sons) dans src/main/resources.

Lancer : python3 tools/gen_assets.py
"""
import json
import os
import shutil

MOD = "wisplight"
HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "..", "src", "main", "resources")
ASSETS = os.path.join(RES, "assets", MOD)
DATA = os.path.join(RES, "data")

VARIANTS = ["azur", "emeraude", "braise", "givre"]
VARIANT_FR = {"azur": "Azur", "emeraude": "Émeraude", "braise": "Braise", "givre": "Givre"}
VARIANT_EN = {"azur": "Azure", "emeraude": "Emerald", "braise": "Ember", "givre": "Frost"}
ESSENCE_FR = {"azur": "lumière", "emeraude": "vie", "braise": "feu", "givre": "froid"}
ESSENCE_EN = {"azur": "Light", "emeraude": "Life", "braise": "Fire", "givre": "Frost"}

written = []


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")
    written.append(path)


def a(*p):
    return os.path.join(ASSETS, *p)


def d(ns, *p):
    return os.path.join(DATA, ns, *p)


def rid(name):
    return f"{MOD}:{name}"


# ---------------------------------------------------------------- modèles
def cube_block(name, texture=None):
    w(a("blockstates", name + ".json"), {"variants": {"": {"model": rid("block/" + name)}}})
    w(a("models", "block", name + ".json"),
      {"parent": "minecraft:block/cube_all", "textures": {"all": rid("block/" + (texture or name))}})
    w(a("models", "item", name + ".json"), {"parent": rid("block/" + name)})


def item_generated(name, texture=None, parent="minecraft:item/generated"):
    w(a("models", "item", name + ".json"),
      {"parent": parent, "textures": {"layer0": rid("item/" + (texture or name))}})


def jar_model():
    side = {"uv": [0, 0, 16, 16], "texture": "#glass"}
    lid = {"uv": [5, 5, 11, 11], "texture": "#lid"}
    lid_side = {"uv": [5, 0, 11, 2], "texture": "#lid"}
    w(a("models", "block", "wisp_jar.json"), {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:translucent",
        "ambientocclusion": False,
        "textures": {
            "glass": rid("block/wisp_jar_glass"),
            "lid": rid("block/wisp_jar_lid"),
            "particle": rid("block/wisp_jar_glass"),
        },
        "elements": [
            {"from": [3, 0, 3], "to": [13, 12, 13],
             "faces": {f: dict(side) for f in ("north", "south", "east", "west", "up", "down")}},
            {"from": [5, 12, 5], "to": [11, 14, 11],
             "faces": {"north": lid_side, "south": lid_side, "east": lid_side, "west": lid_side,
                       "up": lid, "down": lid}},
        ],
    })


def trace_light_model():
    w(a("models", "block", "trace_light.json"), {"textures": {"particle": rid("item/wisp_dust")}})
    w(a("blockstates", "trace_light.json"),
      {"variants": {f"age={i}": {"model": rid("block/trace_light")} for i in range(4)}})


# ---------------------------------------------------------------- loot tables
def loot_self(name):
    w(d(MOD, "loot_table", "blocks", name + ".json"), {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "bonus_rolls": 0,
                   "entries": [{"type": "minecraft:item", "name": rid(name)}],
                   "conditions": [{"condition": "minecraft:survives_explosion"}]}],
        "random_sequence": rid("blocks/" + name),
    })


def loot_ore(name, drop, min_c, max_c):
    w(d(MOD, "loot_table", "blocks", name + ".json"), {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1, "bonus_rolls": 0,
            "entries": [{
                "type": "minecraft:alternatives",
                "children": [
                    {"type": "minecraft:item", "name": rid(name),
                     "conditions": [{"condition": "minecraft:match_tool", "predicate": {
                         "predicates": {"minecraft:enchantments": [
                             {"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}]},
                    {"type": "minecraft:item", "name": rid(drop),
                     "functions": [
                         {"function": "minecraft:set_count",
                          "count": {"type": "minecraft:uniform", "min": min_c, "max": max_c}, "add": False},
                         {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune",
                          "formula": "minecraft:ore_drops"},
                         {"function": "minecraft:explosion_decay"}]},
                ],
            }],
        }],
        "random_sequence": rid("blocks/" + name),
    })


def loot_wisp():
    w(d(MOD, "loot_table", "entities", "wisp.json"), {
        "type": "minecraft:entity",
        "pools": [{"rolls": 1, "bonus_rolls": 0,
                   "entries": [{"type": "minecraft:item", "name": rid("wisp_dust"),
                                "functions": [
                                    {"function": "minecraft:set_count",
                                     "count": {"type": "minecraft:uniform", "min": 1, "max": 2}},
                                    {"function": "minecraft:enchanted_count_increase",
                                     "enchantment": "minecraft:looting",
                                     "count": {"type": "minecraft:uniform", "min": 0, "max": 1}}]}]}],
        "random_sequence": rid("entities/wisp"),
    })


# ---------------------------------------------------------------- recettes
def ing(x):
    return {"tag": x[1:]} if x.startswith("#") else {"item": x}


def shaped(name, pattern, key, result, count=1, category="misc"):
    w(d(MOD, "recipe", name + ".json"), {
        "type": "minecraft:crafting_shaped", "category": category,
        "pattern": pattern, "key": {k: ing(v) for k, v in key.items()},
        "result": {"id": result, "count": count},
    })


def shapeless(name, ingredients, result, count=1, category="misc"):
    w(d(MOD, "recipe", name + ".json"), {
        "type": "minecraft:crafting_shapeless", "category": category,
        "ingredients": [ing(i) for i in ingredients],
        "result": {"id": result, "count": count},
    })


def cooking(name, kind, ingredient, result, xp, time):
    w(d(MOD, "recipe", name + ".json"), {
        "type": "minecraft:" + kind, "category": "misc",
        "ingredient": ing(ingredient), "result": {"id": result},
        "experience": xp, "cookingtime": time,
    })


def tag(ns, kind, name, values, replace=False):
    w(d(ns, "tags", kind, name + ".json"), {"replace": replace, "values": values})


# ---------------------------------------------------------------- traductions
def lang():
    fr = {
        "itemGroup.wisplight": "Wisplight",
        "block.wisplight.lumen_ore": "Minerai de lumen",
        "block.wisplight.deepslate_lumen_ore": "Minerai de lumen des abîmes",
        "block.wisplight.lumen_block": "Bloc de lumen",
        "block.wisplight.trace_light": "Trace lumineuse",
        "item.wisplight.lumen_shard": "Éclat de lumen",
        "item.wisplight.lumen_ingot": "Lingot de lumen",
        "item.wisplight.wisp_dust": "Poussière de follet",
        "item.wisplight.empty_jar": "Bocal vide",
        "item.wisplight.lumen_sword": "Épée en lumen",
        "item.wisplight.lumen_pickaxe": "Pioche en lumen",
        "item.wisplight.lumen_axe": "Hache en lumen",
        "item.wisplight.lumen_shovel": "Pelle en lumen",
        "item.wisplight.lumen_hoe": "Houe en lumen",
        "item.wisplight.lumen_helmet": "Casque en lumen",
        "item.wisplight.lumen_chestplate": "Plastron en lumen",
        "item.wisplight.lumen_leggings": "Jambières en lumen",
        "item.wisplight.lumen_boots": "Bottes en lumen",
        "item.wisplight.wisp_spawn_egg": "Œuf d'apparition de feu follet",
        "entity.wisplight.wisp": "Feu follet",
        "message.wisplight.wisp_restless": "Le follet est trop agité… approche-toi accroupi.",
        "subtitles.wisplight.entity.wisp.ambient": "Un follet tinte",
        "subtitles.wisplight.entity.wisp.hurt": "Un follet vacille",
        "subtitles.wisplight.entity.wisp.death": "Un follet s'éteint",
        "subtitles.wisplight.entity.wisp.capture": "Un follet est capturé",
        "subtitles.wisplight.entity.wisp.release": "Un follet s'envole",
        "subtitles.wisplight.block.wisp_jar.essence_ready": "Une essence est prête",
        "subtitles.wisplight.block.wisp_jar.harvest": "Essence récoltée",
    }
    en = {
        "itemGroup.wisplight": "Wisplight",
        "block.wisplight.lumen_ore": "Lumen Ore",
        "block.wisplight.deepslate_lumen_ore": "Deepslate Lumen Ore",
        "block.wisplight.lumen_block": "Block of Lumen",
        "block.wisplight.trace_light": "Light Trace",
        "item.wisplight.lumen_shard": "Lumen Shard",
        "item.wisplight.lumen_ingot": "Lumen Ingot",
        "item.wisplight.wisp_dust": "Wisp Dust",
        "item.wisplight.empty_jar": "Empty Jar",
        "item.wisplight.lumen_sword": "Lumen Sword",
        "item.wisplight.lumen_pickaxe": "Lumen Pickaxe",
        "item.wisplight.lumen_axe": "Lumen Axe",
        "item.wisplight.lumen_shovel": "Lumen Shovel",
        "item.wisplight.lumen_hoe": "Lumen Hoe",
        "item.wisplight.lumen_helmet": "Lumen Helmet",
        "item.wisplight.lumen_chestplate": "Lumen Chestplate",
        "item.wisplight.lumen_leggings": "Lumen Leggings",
        "item.wisplight.lumen_boots": "Lumen Boots",
        "item.wisplight.wisp_spawn_egg": "Wisp Spawn Egg",
        "entity.wisplight.wisp": "Wisp",
        "message.wisplight.wisp_restless": "The wisp is too restless… sneak up on it.",
        "subtitles.wisplight.entity.wisp.ambient": "Wisp chimes",
        "subtitles.wisplight.entity.wisp.hurt": "Wisp flickers",
        "subtitles.wisplight.entity.wisp.death": "Wisp fades out",
        "subtitles.wisplight.entity.wisp.capture": "Wisp captured",
        "subtitles.wisplight.entity.wisp.release": "Wisp flies away",
        "subtitles.wisplight.block.wisp_jar.essence_ready": "Essence ready",
        "subtitles.wisplight.block.wisp_jar.harvest": "Essence harvested",
    }
    fr.update({
        "block.wisplight.infusion_altar": "Autel d'infusion",
        "block.wisplight.lingering_glow": "Lueur flottante",
        "block.wisplight.calling_lantern": "Lanterne d'appel",
        "item.wisplight.wisp_staff": "Bâton des follets",
        "entity.wisplight.wisp_orb": "Orbe de lumière",
        "container.wisplight.infusion_altar": "Autel d'infusion",
        "gui.wisplight.altar.jar_ok": "Un follet en bocal prête sa lumière",
        "gui.wisplight.altar.jar_missing": "Pose un bocal à follet à 3 blocs maximum",
        "tooltip.wisplight.infusion": "Infusion : %s",
        "tooltip.wisplight.charm.hint": "Actif dans la barre rapide ou en main secondaire",
        "tooltip.wisplight.charm.azur": "Vision nocturne dans le noir total",
        "tooltip.wisplight.charm.emeraude": "Régénération après 10 s sans dégâts",
        "tooltip.wisplight.charm.braise": "Résistance au feu quand tu prends feu (recharge 60 s)",
        "tooltip.wisplight.charm.givre": "Insensible au gel, plus rapide sur la neige et la glace",
        "tooltip.wisplight.staff.plain": "Orbe de lumière : 4 dégâts, éclaire 30 s. S'infuse à l'autel.",
        "essence.wisplight.azur": "Lumière",
        "essence.wisplight.emeraude": "Vie",
        "essence.wisplight.braise": "Feu",
        "essence.wisplight.givre": "Froid",
        "tooltip.wisplight.infusion.azur.weapon": "Les cibles touchées brillent 5 s",
        "tooltip.wisplight.infusion.azur.tool": "Laisse une lueur quand tu mines dans le noir",
        "tooltip.wisplight.infusion.azur.armor": "Éclaire autour de toi",
        "tooltip.wisplight.infusion.azur.staff": "Orbe qui éclaire pendant 2 min",
        "tooltip.wisplight.infusion.emeraude.weapon": "Vol de vie : 10 % des dégâts",
        "tooltip.wisplight.infusion.emeraude.tool": "Se répare lentement au soleil",
        "tooltip.wisplight.infusion.emeraude.armor": "+1 cœur",
        "tooltip.wisplight.infusion.emeraude.staff": "Orbe qui soigne les créatures non hostiles",
        "tooltip.wisplight.infusion.braise.weapon": "Enflamme la cible",
        "tooltip.wisplight.infusion.braise.tool": "Pioche : les minerais sortent déjà fondus",
        "tooltip.wisplight.infusion.braise.armor": "−15 % de dégâts de feu",
        "tooltip.wisplight.infusion.braise.staff": "Orbe de feu",
        "tooltip.wisplight.infusion.givre.weapon": "Lenteur II pendant 3 s",
        "tooltip.wisplight.infusion.givre.tool": "Hache et pelle : +20 % de vitesse sur neige et glace",
        "tooltip.wisplight.infusion.givre.armor": "Bottes : l'eau gèle sous tes pas",
        "tooltip.wisplight.infusion.givre.staff": "Orbe qui gèle l'eau et ralentit",
        "subtitles.wisplight.block.infusion_altar.working": "L'autel bourdonne",
        "subtitles.wisplight.block.infusion_altar.done": "Infusion terminée",
        "subtitles.wisplight.item.wisp_staff.shoot": "Un bâton projette un orbe",
        "subtitles.wisplight.entity.wisp_orb.impact": "Un orbe éclate",
    })
    en.update({
        "block.wisplight.infusion_altar": "Infusion Altar",
        "block.wisplight.lingering_glow": "Lingering Glow",
        "block.wisplight.calling_lantern": "Calling Lantern",
        "item.wisplight.wisp_staff": "Wisp Staff",
        "entity.wisplight.wisp_orb": "Light Orb",
        "container.wisplight.infusion_altar": "Infusion Altar",
        "gui.wisplight.altar.jar_ok": "A jarred wisp lends its light",
        "gui.wisplight.altar.jar_missing": "Place a wisp jar within 3 blocks",
        "tooltip.wisplight.infusion": "Infusion: %s",
        "tooltip.wisplight.charm.hint": "Works in the hotbar or off hand",
        "tooltip.wisplight.charm.azur": "Night vision in total darkness",
        "tooltip.wisplight.charm.emeraude": "Regeneration after 10 s without damage",
        "tooltip.wisplight.charm.braise": "Fire resistance when set on fire (60 s cooldown)",
        "tooltip.wisplight.charm.givre": "Immune to freezing, faster on snow and ice",
        "tooltip.wisplight.staff.plain": "Light orb: 4 damage, lights up for 30 s. Can be infused at the altar.",
        "essence.wisplight.azur": "Light",
        "essence.wisplight.emeraude": "Life",
        "essence.wisplight.braise": "Fire",
        "essence.wisplight.givre": "Frost",
        "tooltip.wisplight.infusion.azur.weapon": "Struck targets glow for 5 s",
        "tooltip.wisplight.infusion.azur.tool": "Leaves a glow when mining in the dark",
        "tooltip.wisplight.infusion.azur.armor": "Lights up your surroundings",
        "tooltip.wisplight.infusion.azur.staff": "Orb that lights up for 2 min",
        "tooltip.wisplight.infusion.emeraude.weapon": "Life steal: 10% of damage",
        "tooltip.wisplight.infusion.emeraude.tool": "Slowly repairs in sunlight",
        "tooltip.wisplight.infusion.emeraude.armor": "+1 heart",
        "tooltip.wisplight.infusion.emeraude.staff": "Orb that heals non-hostile creatures",
        "tooltip.wisplight.infusion.braise.weapon": "Sets the target on fire",
        "tooltip.wisplight.infusion.braise.tool": "Pickaxe: ores drop already smelted",
        "tooltip.wisplight.infusion.braise.armor": "−15% fire damage",
        "tooltip.wisplight.infusion.braise.staff": "Fire orb",
        "tooltip.wisplight.infusion.givre.weapon": "Slowness II for 3 s",
        "tooltip.wisplight.infusion.givre.tool": "Axe and shovel: +20% speed on snow and ice",
        "tooltip.wisplight.infusion.givre.armor": "Boots: water freezes under your steps",
        "tooltip.wisplight.infusion.givre.staff": "Orb that freezes water and slows",
        "subtitles.wisplight.block.infusion_altar.working": "Altar hums",
        "subtitles.wisplight.block.infusion_altar.done": "Infusion complete",
        "subtitles.wisplight.item.wisp_staff.shoot": "Staff casts an orb",
        "subtitles.wisplight.entity.wisp_orb.impact": "Orb bursts",
    })
    for v in VARIANTS:
        fr[f"block.wisplight.lumen_lantern_{v}"] = f"Lanterne de lumen {VARIANT_FR[v].lower()}"
        en[f"block.wisplight.lumen_lantern_{v}"] = f"{VARIANT_EN[v]} Lumen Lantern"
        fr[f"item.wisplight.charm_{v}"] = f"Charme de {ESSENCE_FR[v]}"
        en[f"item.wisplight.charm_{v}"] = f"Charm of {ESSENCE_EN[v]}"
    for v in VARIANTS:
        fr[f"block.wisplight.wisp_jar_{v}"] = f"Bocal à follet {VARIANT_FR[v]}"
        en[f"block.wisplight.wisp_jar_{v}"] = f"{VARIANT_EN[v]} Wisp Jar"
        fr[f"item.wisplight.essence_{v}"] = f"Essence de {ESSENCE_FR[v]}"
        en[f"item.wisplight.essence_{v}"] = f"Essence of {ESSENCE_EN[v]}"
    w(a("lang", "fr_fr.json"), fr)
    w(a("lang", "en_us.json"), en)
    return fr, en


SOUNDS = {
    "entity.wisp.ambient": ["wisp/ambient1", "wisp/ambient2", "wisp/ambient3"],
    "entity.wisp.hurt": ["wisp/hurt"],
    "entity.wisp.death": ["wisp/death"],
    "entity.wisp.capture": ["wisp/capture"],
    "entity.wisp.release": ["wisp/release"],
    "block.wisp_jar.essence_ready": ["jar/essence_ready"],
    "block.wisp_jar.harvest": ["jar/harvest"],
    "block.infusion_altar.working": ["altar/working"],
    "block.infusion_altar.done": ["altar/done"],
    "item.wisp_staff.shoot": ["staff/shoot"],
    "entity.wisp_orb.impact": ["staff/impact"],
}


def sounds():
    w(a("sounds.json"), {
        k: {"subtitle": f"subtitles.wisplight.{k}", "sounds": [rid(s) for s in v]}
        for k, v in SOUNDS.items()
    })


# ---------------------------------------------------------------- génération du monde
BIOMES = {
    "azur": ["minecraft:forest", "minecraft:flower_forest", "minecraft:birch_forest",
             "minecraft:old_growth_birch_forest", "minecraft:dark_forest", "minecraft:plains",
             "minecraft:sunflower_plains", "minecraft:meadow", "minecraft:cherry_grove"],
    "emeraude": ["minecraft:swamp", "minecraft:mangrove_swamp", "minecraft:jungle",
                 "minecraft:sparse_jungle", "minecraft:bamboo_jungle"],
    "braise": ["minecraft:soul_sand_valley", "minecraft:crimson_forest"],
    "givre": ["minecraft:snowy_plains", "minecraft:ice_spikes", "minecraft:snowy_taiga",
              "minecraft:grove", "minecraft:snowy_slopes", "minecraft:frozen_peaks"],
}


def worldgen():
    w(d(MOD, "worldgen", "configured_feature", "ore_lumen.json"), {
        "type": "minecraft:ore",
        "config": {
            "size": 6,
            "discard_chance_on_air_exposure": 0.0,
            "targets": [
                {"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables"},
                 "state": {"Name": rid("lumen_ore")}},
                {"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:deepslate_ore_replaceables"},
                 "state": {"Name": rid("deepslate_lumen_ore")}},
            ],
        },
    })
    w(d(MOD, "worldgen", "placed_feature", "ore_lumen.json"), {
        "feature": rid("ore_lumen"),
        "placement": [
            {"type": "minecraft:count", "count": 6},
            {"type": "minecraft:in_square"},
            {"type": "minecraft:height_range", "height": {
                "type": "minecraft:trapezoid",
                "min_inclusive": {"absolute": -48}, "max_inclusive": {"absolute": 32}}},
            {"type": "minecraft:biome"},
        ],
    })
    w(d(MOD, "neoforge", "biome_modifier", "add_lumen_ore.json"), {
        "type": "neoforge:add_features",
        "biomes": "#minecraft:is_overworld",
        "features": rid("ore_lumen"),
        "step": "underground_ores",
    })
    for v, biomes in BIOMES.items():
        tag(MOD, "worldgen/biome", f"wisp_{v}_biomes", biomes)
        w(d(MOD, "neoforge", "biome_modifier", f"add_wisp_{v}.json"), {
            "type": "neoforge:add_spawns",
            "biomes": f"#{MOD}:wisp_{v}_biomes",
            "spawners": {"type": rid("wisp"), "weight": 12 if v == "braise" else 10, "minCount": 1, "maxCount": 3},
        })


# ---------------------------------------------------------------- phase 3
def faces(uvs, tex="#lantern"):
    return {f: {"uv": uv, "texture": tex} for f, uv in uvs.items()}


def box_faces(side, top, bottom=None, tex="#lantern"):
    bottom = bottom or top
    return faces({"north": side, "south": side, "east": side, "west": side, "up": top, "down": bottom}, tex)


def lantern_templates():
    rot = {"angle": 45, "axis": "y", "origin": [8, 8, 8]}
    for hanging in (False, True):
        y0 = 1 if hanging else 0
        elements = [
            {"from": [5, y0, 5], "to": [11, y0 + 7, 11], "faces": box_faces([0, 0, 6, 7], [0, 7, 6, 13])},
            {"from": [6, y0 + 7, 6], "to": [10, y0 + 9, 10], "faces": box_faces([6, 0, 10, 2], [6, 2, 10, 6])},
        ]
        top, h = (y0 + 9, 16 - (y0 + 9)) if hanging else (9, 2)
        uv = [11, 0, 14, h]
        elements.append({"from": [6.5, top, 8], "to": [9.5, top + h, 8], "rotation": rot,
                         "faces": faces({"north": uv, "south": uv})})
        elements.append({"from": [8, top, 6.5], "to": [8, top + h, 9.5], "rotation": rot,
                         "faces": faces({"east": uv, "west": uv})})
        name = "template_lumen_lantern" + ("_hanging" if hanging else "")
        w(a("models", "block", name + ".json"), {
            "parent": "minecraft:block/block",
            "render_type": "minecraft:cutout",
            "ambientocclusion": False,
            "textures": {"particle": "#lantern"},
            "elements": elements,
        })


def lantern_block(name, texture, hanging_prop="hanging"):
    for hanging in (False, True):
        suffix = "_hanging" if hanging else ""
        w(a("models", "block", name + suffix + ".json"), {
            "parent": rid("block/template_lumen_lantern" + suffix),
            "textures": {"lantern": rid("block/" + texture)},
        })
    w(a("blockstates", name + ".json"), {"variants": {
        f"{hanging_prop}=false": {"model": rid("block/" + name)},
        f"{hanging_prop}=true": {"model": rid("block/" + name + "_hanging")},
    }})
    item_generated(name)
    loot_self(name)


def altar_model():
    w(a("models", "block", "infusion_altar.json"), {
        "parent": "minecraft:block/block",
        "textures": {
            "top": rid("block/infusion_altar_top"),
            "side": rid("block/infusion_altar_side"),
            "bottom": rid("block/infusion_altar_bottom"),
            "particle": rid("block/infusion_altar_side"),
        },
        "elements": [
            {"from": [3, 0, 3], "to": [13, 10, 13], "faces": {
                **faces({"north": [3, 6, 13, 16], "south": [3, 6, 13, 16], "east": [3, 6, 13, 16], "west": [3, 6, 13, 16]}, "#side"),
                **faces({"down": [3, 3, 13, 13], "up": [3, 3, 13, 13]}, "#bottom")}},
            {"from": [1, 10, 1], "to": [15, 13, 15], "faces": {
                **faces({"north": [1, 0, 15, 3], "south": [1, 0, 15, 3], "east": [1, 0, 15, 3], "west": [1, 0, 15, 3]}, "#side"),
                **faces({"up": [1, 1, 15, 15]}, "#top"),
                **faces({"down": [1, 1, 15, 15]}, "#bottom")}},
        ],
    })
    w(a("blockstates", "infusion_altar.json"), {"variants": {"": {"model": rid("block/infusion_altar")}}})
    w(a("models", "item", "infusion_altar.json"), {"parent": rid("block/infusion_altar")})
    loot_self("infusion_altar")


def glow_model():
    w(a("models", "block", "lingering_glow.json"), {"textures": {"particle": rid("item/wisp_dust")}})
    w(a("blockstates", "lingering_glow.json"),
      {"variants": {f"persistent={b}": {"model": rid("block/lingering_glow")} for b in ("false", "true")}})


def infusion_recipe(name, ingredient, essence, result=None, infuse=None):
    obj = {"type": rid("infusion"), "ingredient": ing(ingredient),
           "essence_a": ing(essence), "essence_b": ing(essence)}
    if result:
        obj["result"] = {"id": result, "count": 1}
    if infuse:
        obj["infuse"] = infuse
    w(d(MOD, "recipe", "infusion", name + ".json"), obj)


def phase3():
    lantern_templates()
    altar_model()
    glow_model()
    lantern_block("calling_lantern", "calling_lantern")
    for v in VARIANTS:
        lantern_block(f"lumen_lantern_{v}", f"lumen_lantern_{v}")
        item_generated(f"charm_{v}")
        shaped(f"lumen_lantern_{v}", [" S ", "SES", " S "],
               {"S": rid("lumen_shard"), "E": rid(f"essence_{v}")}, rid(f"lumen_lantern_{v}"), 2, "decorations")
        infusion_recipe(f"charm_{v}", rid("lumen_ingot"), rid(f"essence_{v}"), result=rid(f"charm_{v}"))
        infusion_recipe(f"infuse_{v}", f"#{MOD}:infusable", rid(f"essence_{v}"), infuse=v)
    item_generated("wisp_staff", parent="minecraft:item/handheld")
    shaped("infusion_altar", [" L ", "DJD", "DDD"],
           {"L": rid("lumen_ingot"), "J": rid("empty_jar"), "D": "minecraft:polished_deepslate"}, rid("infusion_altar"))
    shapeless("calling_lantern", ["minecraft:lantern", rid("wisp_dust"), rid("wisp_dust"), rid("lumen_shard")],
              rid("calling_lantern"), category="decorations")
    shaped("wisp_staff", [" DL", " SD", "S  "],
           {"L": rid("lumen_ingot"), "D": rid("wisp_dust"), "S": "minecraft:stick"}, rid("wisp_staff"), category="equipment")
    tag(MOD, "item", "essences", [rid(f"essence_{v}") for v in VARIANTS])
    tag(MOD, "item", "infusable", [rid("lumen_" + t) for t in ("sword", "pickaxe", "axe", "shovel", "hoe",
                                                               "helmet", "chestplate", "leggings", "boots")]
        + [rid("wisp_staff")])


# ---------------------------------------------------------------- tout
def main():
    for sub in ("blockstates", "models", "lang", "sounds.json"):
        p = a(sub)
        if os.path.isdir(p):
            shutil.rmtree(p)
    for sub in ("loot_table", "recipe", "tags", "worldgen", "neoforge"):
        p = d(MOD, sub)
        if os.path.isdir(p):
            shutil.rmtree(p)
    for ns in ("minecraft", "c"):
        p = d(ns, "tags")
        if os.path.isdir(p):
            shutil.rmtree(p)

    # Blocs
    for b in ("lumen_ore", "deepslate_lumen_ore", "lumen_block"):
        cube_block(b)
    trace_light_model()
    jar_model()
    for v in VARIANTS:
        name = f"wisp_jar_{v}"
        w(a("blockstates", name + ".json"), {"variants": {"": {"model": rid("block/wisp_jar")}}})
        item_generated(name)
        loot_self(name)

    # Objets
    for it in ["lumen_shard", "lumen_ingot", "wisp_dust", "empty_jar",
               "lumen_helmet", "lumen_chestplate", "lumen_leggings", "lumen_boots"] + [f"essence_{v}" for v in VARIANTS]:
        item_generated(it)
    for tool in ("sword", "pickaxe", "axe", "shovel", "hoe"):
        item_generated("lumen_" + tool, parent="minecraft:item/handheld")
    w(a("models", "item", "wisp_spawn_egg.json"), {"parent": "minecraft:item/template_spawn_egg"})

    # Loot
    loot_ore("lumen_ore", "lumen_shard", 1, 3)
    loot_ore("deepslate_lumen_ore", "lumen_shard", 1, 3)
    loot_self("lumen_block")
    loot_wisp()

    # Recettes
    shapeless("lumen_ingot", [rid("lumen_shard")] * 4 + ["minecraft:gold_ingot"], rid("lumen_ingot"))
    shaped("lumen_block", ["###", "###", "###"], {"#": rid("lumen_shard")}, rid("lumen_block"), category="building")
    shapeless("lumen_shard_from_block", [rid("lumen_block")], rid("lumen_shard"), 9)
    cooking("lumen_shard_from_smelting", "smelting", f"#{MOD}:lumen_ores", rid("lumen_shard"), 0.7, 200)
    cooking("lumen_shard_from_blasting", "blasting", f"#{MOD}:lumen_ores", rid("lumen_shard"), 0.7, 100)
    shaped("empty_jar", [" S ", "G G", "GGG"],
           {"S": "#minecraft:wooden_slabs", "G": "#c:glass_blocks"}, rid("empty_jar"), 2)
    L, S = rid("lumen_ingot"), "minecraft:stick"
    shaped("lumen_sword", ["L", "L", "S"], {"L": L, "S": S}, rid("lumen_sword"), category="equipment")
    shaped("lumen_pickaxe", ["LLL", " S ", " S "], {"L": L, "S": S}, rid("lumen_pickaxe"), category="equipment")
    shaped("lumen_axe", ["LL", "LS", " S"], {"L": L, "S": S}, rid("lumen_axe"), category="equipment")
    shaped("lumen_shovel", ["L", "S", "S"], {"L": L, "S": S}, rid("lumen_shovel"), category="equipment")
    shaped("lumen_hoe", ["LL", " S", " S"], {"L": L, "S": S}, rid("lumen_hoe"), category="equipment")
    shaped("lumen_helmet", ["LLL", "L L"], {"L": L}, rid("lumen_helmet"), category="equipment")
    shaped("lumen_chestplate", ["L L", "LLL", "LLL"], {"L": L}, rid("lumen_chestplate"), category="equipment")
    shaped("lumen_leggings", ["LLL", "L L", "L L"], {"L": L}, rid("lumen_leggings"), category="equipment")
    shaped("lumen_boots", ["L L", "L L"], {"L": L}, rid("lumen_boots"), category="equipment")

    # Tags
    ores = [rid("lumen_ore"), rid("deepslate_lumen_ore")]
    tag(MOD, "block", "lumen_ores", ores)
    tag(MOD, "item", "lumen_ores", ores)
    tag("minecraft", "block", "mineable/pickaxe", ores + [rid("lumen_block"), rid("infusion_altar"), rid("calling_lantern")]
        + [rid(f"lumen_lantern_{v}") for v in VARIANTS])
    tag("minecraft", "block", "needs_iron_tool", ores)
    tag("minecraft", "block", "needs_stone_tool", [rid("lumen_block")])
    tag("minecraft", "item", "swords", [rid("lumen_sword")])
    tag("minecraft", "item", "pickaxes", [rid("lumen_pickaxe")])
    tag("minecraft", "item", "axes", [rid("lumen_axe")])
    tag("minecraft", "item", "shovels", [rid("lumen_shovel")])
    tag("minecraft", "item", "hoes", [rid("lumen_hoe")])
    tag("minecraft", "item", "head_armor", [rid("lumen_helmet")])
    tag("minecraft", "item", "chest_armor", [rid("lumen_chestplate")])
    tag("minecraft", "item", "leg_armor", [rid("lumen_leggings")])
    tag("minecraft", "item", "foot_armor", [rid("lumen_boots")])
    tag("minecraft", "item", "trimmable_armor",
        [rid("lumen_helmet"), rid("lumen_chestplate"), rid("lumen_leggings"), rid("lumen_boots")])
    tag("c", "block", "ores", ores)
    tag("c", "item", "ores", ores)
    tag("c", "item", "ingots", [rid("lumen_ingot")])
    tag("c", "block", "storage_blocks", [rid("lumen_block")])

    phase3()
    worldgen()
    sounds()
    lang()
    print(len(written), "fichiers JSON écrits")


if __name__ == "__main__":
    main()
