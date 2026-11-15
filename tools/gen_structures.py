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
