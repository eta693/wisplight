"""Génère les modèles de structure NBT utilisés par les GameTests."""
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import nbt  # noqa: E402

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "data", "wisplight", "structure")


def floor_template(name, w, h, d, floor_block):
    palette = [{"Name": floor_block}]
    blocks = [(x, 0, z, 0) for x in range(w) for z in range(d)]
    os.makedirs(ROOT, exist_ok=True)
    nbt.write(os.path.join(ROOT, name + ".nbt"), nbt.structure((w, h, d), blocks, palette))


if __name__ == "__main__":
    floor_template("empty", 5, 5, 5, "minecraft:stone")
    floor_template("platform", 9, 6, 9, "minecraft:grass_block")
    print("structures OK")


# ----------------------------------------------------------------------
# Sanctuaire oublié (21 x 14 x 21) — le sol de surface est en y = 6.
# ----------------------------------------------------------------------
def sanctuary():
    import math
    import random
    rnd = random.Random(1945)
    W, H, D = 21, 14, 21
    C = 10
    blocks = {}  # (x, y, z) -> (Name, props, nbt)

    def put(x, y, z, name, props=None, nbt_data=None):
        blocks[(x, y, z)] = (name, props or {}, nbt_data)

    B = "wisplight:sanctuary_bricks"
    CR = "wisplight:cracked_sanctuary_bricks"
    SLAB = ("wisplight:sanctuary_brick_slab", {"type": "bottom", "waterlogged": "false"})
    WALL = ("wisplight:sanctuary_brick_wall", {"up": "true", "north": "none", "east": "none",
                                               "south": "none", "west": "none", "waterlogged": "false"})

    # --- Crypte (y 0..5)
    for x in range(6, 15):
        for z in range(6, 15):
            put(x, 0, z, B)
            put(x, 5, z, B)
            for y in range(1, 5):
                if x in (6, 14) or z in (6, 14):
                    put(x, y, z, CR if rnd.random() < 0.2 else B)
                else:
                    put(x, y, z, "minecraft:air")
    put(10, 1, 10, "minecraft:spawner", None, {
        "id": "minecraft:mob_spawner",
        "SpawnData": {"entity": {"id": "wisplight:creeping_shade"}},
        "Delay": 20, "MinSpawnDelay": 200, "MaxSpawnDelay": 600, "SpawnCount": 2,
        "MaxNearbyEntities": 4, "RequiredPlayerRange": 16, "SpawnRange": 3})
    put(10, 1, 13, "minecraft:chest", {"facing": "north", "type": "single", "waterlogged": "false"},
        {"id": "minecraft:chest", "LootTable": "wisplight:chests/sanctuary_crypt"})
    for (x, z) in [(7, 13), (13, 7), (13, 13)]:
        put(x, 4, z, "minecraft:cobweb")
    put(12, 1, 8, "minecraft:skeleton_skull", {"rotation": "6", "powered": "false"})

    # --- Sol de surface (y 6) : disque de rayon 10
    for x in range(W):
        for z in range(D):
            d = math.hypot(x - C, z - C)
            if d <= 10.2:
                put(x, 6, z, B)
                for y in range(7, H):
                    put(x, y, z, "minecraft:air")
    # Échelle d'accès à la crypte
    for y in range(1, 7):
        put(8, y, 7, "minecraft:ladder", {"facing": "south", "waterlogged": "false"})

    # --- Estrade et autel
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            if dx or dz:
                put(C + dx, 7, C + dz, *SLAB)
    put(C, 7, C, "wisplight:sanctuary_altar")
    runes = {"azur": (C, 7, C - 4), "emeraude": (C + 4, 7, C), "braise": (C, 7, C + 4), "givre": (C - 4, 7, C)}
    for v, (x, y, z) in runes.items():
        put(x, y, z, f"wisplight:runestone_{v}", {"charged": "false", "lit": "false"})

    # --- Piliers brisés (rayon 8)
    heights = [6, 3, 5, 2, 6, 4, 1, 5]
    for k, h in enumerate(heights):
        a = math.radians(k * 45)
        x, z = round(C + 8 * math.cos(a)), round(C + 8 * math.sin(a))
        for y in range(7, 7 + h):
            put(x, y, z, CR if rnd.random() < 0.3 else B)
        if h >= 5:
            put(x, 7 + h, z, *WALL)

    # --- Muret circulaire en ruine (rayon ~10)
    for deg in range(0, 360, 8):
        if (deg // 8) % 4 == 0:
            continue
        a = math.radians(deg)
        x, z = round(C + 10 * math.cos(a)), round(C + 10 * math.sin(a))
        if 0 <= x < W and 0 <= z < D:
            put(x, 7, z, *SLAB) if rnd.random() < 0.5 else put(x, 7, z, B)

    # --- Coffres de surface et végétation
    put(5, 7, 12, "minecraft:chest", {"facing": "east", "type": "single", "waterlogged": "false"},
        {"id": "minecraft:chest", "LootTable": "wisplight:chests/sanctuary_surface"})
    put(15, 7, 8, "minecraft:chest", {"facing": "west", "type": "single", "waterlogged": "false"},
        {"id": "minecraft:chest", "LootTable": "wisplight:chests/sanctuary_surface"})
    for x in range(W):
        for z in range(D):
            if blocks.get((x, 7, z), ("minecraft:air",))[0] == "minecraft:air" and math.hypot(x - C, z - C) < 9.5:
                if rnd.random() < 0.10 and not (x == 8 and z == 7):
                    put(x, 7, z, "minecraft:moss_carpet")

    # --- Écriture NBT
    palette, index = [], {}
    entries = []
    for (x, y, z), (name, props, nbt_data) in sorted(blocks.items(), key=lambda kv: (kv[0][1], kv[0][2], kv[0][0])):
        key = (name, tuple(sorted(props.items())))
        if key not in index:
            index[key] = len(palette)
            entry = {"Name": name}
            if props:
                entry["Properties"] = dict(props)
            palette.append(entry)
        entries.append((x, y, z, index[key], nbt_data))
    out = os.path.join(ROOT, "sanctuary")
    os.makedirs(out, exist_ok=True)
    nbt.write(os.path.join(out, "main.nbt"), nbt.structure((W, H, D), entries, palette))
    return len(entries), len(palette)


if __name__ == "__main__":
    print("sanctuaire :", sanctuary())
