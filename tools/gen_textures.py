"""Génère toutes les textures pixel art de Wisplight dans src/main/resources/assets/wisplight/textures."""
import math, random, os, sys
from PIL import Image


def hx(c):
    c = c.lstrip('#'); return tuple(int(c[i:i+2], 16) for i in (0, 2, 4)) + (255,)

T = (0, 0, 0, 0)
# rampes : 0 = ombre profonde ... 5 = blanc
RAMPS = {
    "azur":    ["#0e2a4f", "#1d4e89", "#3a8fd6", "#7fd0ff", "#bdf3ff", "#ffffff"],
    "emeraude":["#0b3320", "#1b5e3a", "#2fa35f", "#6fe39a", "#c8ffd4", "#ffffff"],
    "braise":  ["#3d1004", "#7a2408", "#d4561a", "#ff9a3c", "#ffe08a", "#fffbe6"],
    "givre":   ["#1e3347", "#4a6d8c", "#8fb8d6", "#cfeaff", "#f0fbff", "#ffffff"],
}
LUMEN = {"o": "#5a3d14", "s": "#a8742a", "m": "#e8b84a", "l": "#ffe27a", "h": "#fff6c8"}

def grid(rows, pal, w=16, h=16):
    img = Image.new("RGBA", (w, h), T)
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal and pal[ch] is not None:
                img.putpixel((x, y), hx(pal[ch]) if isinstance(pal[ch], str) else pal[ch])
    return img


# ---------- follet (procédural) ----------
def wisp(ramp, n=16, eyes=True):
    pal = [hx(c) for c in RAMPS[ramp]]
    img = Image.new("RGBA", (n, n), T)
    k = n / 16
    for y in range(n):
        for x in range(n):
            px, py = (x + .5) / k, (y + .5) / k
            d = math.hypot(px - 8, py - 10)
            core = 1 - d / 5.2
            flame = 0
            if 1.5 < py < 10.5:
                t = (10.5 - py) / 9          # 0 en bas, 1 en haut
                cx = 8 + 1.6 * math.sin(py * 0.85)
                w = 3.6 * (1 - t) ** 0.8 + 0.25
                flame = (1 - abs(px - cx) / w) * (1 - t * 0.55)
            v = max(core, flame)
            if v > .78: i = 5
            elif v > .6: i = 4
            elif v > .42: i = 3
            elif v > .24: i = 2
            elif v > .08: i = 1
            else: continue
            img.putpixel((x, y), pal[i])
    if eyes and n >= 16:
        for ex in (6, 9):
            img.putpixel((ex, 10), pal[0]); img.putpixel((ex, 11), pal[1])
    return img

# ---------- pierre / deepslate ----------
def noise_block(cols, seed):
    rnd = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), hx(rnd.choices(cols, weights=[5, 3, 3, 2, 1][:len(cols)])[0]))
    return img

STONE = ["#7d7d7d", "#747474", "#868686", "#6b6b6b", "#8f8f8f"]
DEEP = ["#4d4d55", "#45454c", "#56565e", "#3c3c42", "#5f5f67"]

ORE = [
    "................",
    "................",
    "..hl.......h....",
    "..lms.....hlm...",
    "...so.....lms...",
    "...........so...",
    "......hl........",
    ".....hlmm.......",
    ".....lmms.......",
    "......sso.......",
    "...........hl...",
    ".hl.......hlm...",
    ".lms.......mso..",
    "..so............",
    "................",
    "................",
]

def ore(base, seed):
    img = noise_block(base, seed)
    img.alpha_composite(grid(ORE, LUMEN))
    return img

SHARD = [
    "................",
    ".......h........",
    "......hlm.......",
    "......hlm.......",
    "...h..hlmo......",
    "..hlm.hlmo..h...",
    "..hlmohlmo.hlm..",
    "..hlmohlmsohlmo.",
    "..hlmshlmsohlmo.",
    "..ohlshlmsolmso.",
    "...olmslmsomso..",
    "...oomssmsoso...",
    ".....ooossoo....",
    ".......ooo......",
    "................",
    "................",
]

INGOT = [
    "................",
    "................",
    "................",
    "................",
    "........oooooo..",
    "......oohhhhhlo.",
    "....oohhhhhllmo.",
    "..oohhhhhllmmso.",
    ".ohhhhhllmmmsso.",
    ".ollllmmmmssso..",
    ".ommmmmmsssoo...",
    ".osssssssoo.....",
    ".oooooooo.......",
    "................",
    "................",
    "................",
]

SWORD = [
    ".............ooo",
    "............ohho",
    "...........ohlo.",
    "..........ohlo..",
    ".........ohlo...",
    "........ohlo....",
    ".......ohlo.....",
    "......ohlo......",
    "..oo.ohlo.......",
    "..ogohlo........",
    "...oggo.........",
    "...owgo.........",
    "..owoogo........",
    ".owo..oo........",
    "owo.............",
    "oo..............",
]
SWORD_PAL = dict(LUMEN, g="#8a5a1c", w="#5b3a1e")

STAFF = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "..........gg....",
    ".........gwo....",
    "........owo.....",
    ".......owo......",
    "......owo.......",
    ".....owo........",
    "....owo.........",
    "...owo..........",
    "..owo...........",
    ".owo............",
    ".oo.............",
]
STAFF_PAL = {"o": "#2e1c0e", "w": "#7a4f2a", "g": "#e8b84a"}

def staff(ramp):
    img = grid(STAFF, STAFF_PAL)
    pal = [hx(c) for c in RAMPS[ramp]]
    for y in range(0, 7):
        for x in range(8, 16):
            d = math.hypot(x + .5 - 12, y + .5 - 3.5)
            if d < 3.2:
                v = 1 - d / 3.2
                i = 5 if v > .7 else 4 if v > .5 else 3 if v > .3 else 2
                if d > 2.5: i = 1
                img.putpixel((x, y), pal[i])
    # griffes dorées
    for p in [(9, 5), (14, 5), (9, 1), (14, 1)]:
        img.putpixel(p, hx("#e8b84a"))
    return img

VIAL = [
    "................",
    ".......cc.......",
    "......cCCc......",
    ".......gg.......",
    ".......gg.......",
    "......g11g......",
    ".....g1221g.....",
    "....gW23321g....",
    "....g234432g....",
    "....g234432g....",
    "....g123321g....",
    ".....g1221g.....",
    "......gggg......",
    "................",
    "................",
    "................",
]

def vial(ramp):
    r = RAMPS[ramp]
    pal = {"c": "#6b4423", "C": "#8b5a2b", "g": "#cfe3ea", "W": "#ffffff",
           "1": r[1], "2": r[2], "3": r[3], "4": r[4]}
    return grid(VIAL, pal)

JAR = [
    "................",
    ".....cccccc.....",
    ".....cCCCCc.....",
    "....gggggggg....",
    "....g......g....",
    "...g........g...",
    "...gW.......g...",
    "...gW.......g...",
    "...g........g...",
    "...g........g...",
    "...g........g...",
    "...g........g...",
    "...g........g...",
    "...gg......gg...",
    "....gggggggg....",
    "................",
]

def jar(ramp):
    img = grid(JAR, {"c": "#6b4423", "C": "#8b5a2b", "g": "#cfe3ea", "W": "#ffffff"})
    # teinte intérieure légère
    tint = hx(RAMPS[ramp][1])
    for y in range(4, 14):
        for x in range(4, 12):
            if img.getpixel((x, y))[3] == 0:
                img.putpixel((x, y), tint[:3] + (70,))
    w = wisp(ramp, 8, eyes=False)
    img.alpha_composite(w, (4, 5))
    return img

CHARM = [
    "................",
    ".....oooooo.....",
    "....o......o....",
    "...o........o...",
    "...o........o...",
    "....o......o....",
    ".....o....o.....",
    "......ollo......",
    ".....lmmmml.....",
    "....lm2343ml....",
    "....lm3454ms....",
    "....lm2343ms....",
    ".....mm22ms.....",
    "......mmss......",
    ".......ss.......",
    "................",
]

def charm(ramp):
    r = RAMPS[ramp]
    pal = {"o": "#4a2f17", "l": "#ffe27a", "m": "#e8b84a", "s": "#a8742a",
           "2": r[2], "3": r[3], "4": r[4], "5": r[5]}
    return grid(CHARM, pal)

def bricks(seed=3, moss=True):
    rnd = random.Random(seed)
    cols = ["#5e6470", "#575d69", "#666c78", "#6f7581"]
    mortar = "#3d414b"
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        row = y // 4
        off = 0 if row % 2 == 0 else 4
        for x in range(16):
            if y % 4 == 3 or (x + off) % 8 == 7:
                c = mortar
            else:
                c = rnd.choice(cols)
                if y % 4 == 0: c = "#737a87" if rnd.random() < .6 else c
            img.putpixel((x, y), hx(c))
    if moss:
        for (x, y) in [(0, 15), (1, 15), (1, 14), (2, 15), (13, 3), (14, 3), (14, 2), (6, 11)]:
            img.putpixel((x, y), hx(rnd.choice(["#4a6b3a", "#5b7f45"])))
    return img

RUNES = {
    "azur": ["..x..x..", "..x..x..", "...xx...", "x..xx..x", ".x.xx.x.", "..xxxx..", "...xx...", "...xx..."],
    "emeraude": ["...xx...", "..x..x..", ".x.xx.x.", "x..xx..x", "...xx...", "..xxxx..", "...xx...", "..x..x.."],
    "braise": ["...x....", "...xx...", "..x.x...", "..x..x..", ".x...x..", ".x.xx.x.", ".xx..xx.", "..xxxx.."],
    "givre": ["...xx...", "x..xx..x", ".x.xx.x.", "..xxxx..", "xxxxxxxx", "..xxxx..", ".x.xx.x.", "x..xx..x"],
}

def runestone(ramp, seed=7, glow=1):
    rnd = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            edge = x in (0, 15) or y in (0, 15)
            c = "#2c2f38" if edge else rnd.choice(["#3a3e4a", "#363a45", "#40444f"])
            img.putpixel((x, y), hx(c))
    r = RAMPS[ramp]
    for y, row in enumerate(RUNES[ramp]):
        for x, ch in enumerate(row):
            if ch == "x":
                core = [r[1], r[3], r[5]][glow]
                img.putpixel((x + 4, y + 4), hx(core))
                for dx, dy in ((1, 0), (0, 1)):
                    p = (x + 4 + dx, y + 4 + dy)
                    if img.getpixel(p)[:3] != hx(core)[:3]:
                        img.putpixel(p, hx([r[0], r[1], r[3]][glow]))
    return img

SHADE = [
    "......oooo......",
    ".....oddddo.....",
    "....oddpddo.....",
    "....odWddWdo....",
    "....odWddWdo....",
    ".....oddddo.....",
    "...ooddpdddoo...",
    "..odddpppddddo..",
    ".odd.odpddo.ddo.",
    ".od..oddddo..do.",
    ".od..oddpdo..do.",
    ".od..oddddo..do.",
    ".pd..oddddo..dp.",
    "..d..odpddo..d..",
    "..p..oddddo..p..",
    ".....odddddo....",
    ".....oddoddo....",
    "....oddo.ddo....",
    "....odo..odo....",
    "....pd....dp....",
    "....p......p....",
    "................",
    "................",
    "................",
]
SHADE_PAL = {"o": "#0b0810", "d": "#1a1424", "p": "#3b2a55", "W": "#f4f0ff"}

def warden(phase_ramp="givre", lit=False):
    """Le Veilleur Éteint : lanterne géante 24x32."""
    W_, H_ = 24, 32
    img = Image.new("RGBA", (W_, H_), T)
    iron, iron2, iron3 = hx("#2b2d33"), hx("#44474f"), hx("#62656e")
    def rect(x0, y0, x1, y1, c):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                img.putpixel((x, y), c)
    # anneau + chaîne
    for p in [(11, 0), (12, 0), (10, 1), (13, 1), (10, 2), (13, 2), (11, 3), (12, 3)]:
        img.putpixel(p, iron3)
    rect(11, 4, 12, 5, iron2)
    # chapeau
    rect(8, 6, 15, 6, iron3); rect(6, 7, 17, 7, iron2); rect(4, 8, 19, 9, iron)
    rect(5, 8, 18, 8, iron2)
    # cage
    rect(4, 10, 19, 10, iron2); rect(4, 27, 19, 28, iron)
    for x in (4, 9, 14, 19):
        rect(x, 11, x, 26, iron if x in (4, 19) else iron2)
    # verre (intérieur sombre ou lumineux)
    r = [hx(c) for c in RAMPS[phase_ramp]]
    for y in range(11, 27):
        for x in range(5, 19):
            if x in (9, 14):
                continue
            d = math.hypot(x + .5 - 12, (y + .5 - 21) * 0.8)
            base = hx("#141a26") if not lit else r[0]
            img.putpixel((x, y), base)
            v = 1 - d / (6.5 if lit else 4)
            if v > 0:
                i = 5 if v > .75 else 4 if v > .55 else 3 if v > .35 else 2 if v > .15 else 1
                if not lit: i = max(1, i - 2)
                img.putpixel((x, y), r[i])
    # yeux dans la flamme
    for p in [(10, 18), (13, 18)]:
        img.putpixel(p, hx("#0b0810"))
    # socle + pieds
    rect(3, 29, 20, 29, iron2); rect(5, 30, 7, 31, iron); rect(16, 30, 18, 31, iron)
    return img

def altar():
    rows = [
        "................",
        "................",
        "..oooooooooooo..",
        ".ollllllllllllo.",
        ".ommmmmmmmmmmmo.",
        "..oooooooooooo..",
        "....bbbbbbbb....",
        "....bBbbbbBb....",
        "....bbbbbbbb....",
        "....bBbbbbBb....",
        "....bbbbbbbb....",
        "...bbbbbbbbbb...",
        "..bbbbbbbbbbbb..",
        "..BBBBBBBBBBBB..",
        "................",
        "................",
    ]
    return grid(rows, {"o": "#5a3d14", "l": "#ffe27a", "m": "#e8b84a", "b": "#5e6470", "B": "#444956"})

def call_lantern():
    rows = [
        "......oooo......",
        ".....o....o.....",
        "......oooo......",
        ".....oiiiio.....",
        "....oiiiiiio....",
        "....ogfffgio....",
        "....ogfhffgo....",
        "....ogfhhfgo....",
        "....ogffffgo....",
        "....ogfffggo....",
        "....oiiiiiio....",
        ".....oiiiio.....",
        "................",
        "................",
        "................",
        "................",
    ]
    r = RAMPS["azur"]
    return grid(rows, {"o": "#2b2d33", "i": "#44474f", "g": "#cfe3ea", "f": r[3], "h": r[5]})


# ======================================================================
# Nouveaux sprites (phase 1-2)
# ======================================================================
TOOL_PAL = dict(LUMEN, w="#7a4f2a", W="#5b3a1e")

PICKAXE = [
    "................",
    "....oooooo......",
    "...ohhhhllo.....",
    "....ooooolmo....",
    "........owlmo...",
    ".......owo.omo..",
    "......owo...oso.",
    ".....owo.....oo.",
    "....owo.........",
    "...owo..........",
    "..owo...........",
    ".owo............",
    "owo.............",
    "oo..............",
    "................",
    "................",
]

AXE = [
    "................",
    ".......oooo.....",
    "......ohhlmo....",
    ".....ohhllmmo...",
    ".....ohllmowo...",
    "......ollowo....",
    ".......oowo.....",
    ".......owo......",
    "......owo.......",
    ".....owo........",
    "....owo.........",
    "...owo..........",
    "..owo...........",
    ".owo............",
    "oo..............",
    "................",
]

SHOVEL = [
    "................",
    "...........oo...",
    "..........ohho..",
    ".........ohhllo.",
    ".........ohlmmo.",
    "..........omso..",
    ".........owoo...",
    "........owo.....",
    ".......owo......",
    "......owo.......",
    ".....owo........",
    "....owo.........",
    "...owo..........",
    "..owo...........",
    ".owo............",
    ".oo.............",
]

HOE = [
    "................",
    ".......ooooo....",
    "......ohhllmo...",
    ".......ooooowo..",
    "..........owo...",
    ".........owo....",
    "........owo.....",
    ".......owo......",
    "......owo.......",
    ".....owo........",
    "....owo.........",
    "...owo..........",
    "..owo...........",
    ".owo............",
    ".oo.............",
    "................",
]

HELMET = [
    "................",
    "................",
    "................",
    "................",
    "....oooooooo....",
    "...ohhhhhllmo...",
    "..ohhllllllmmo..",
    "..ohlmmmmmmmso..",
    "..ohlo....omso..",
    "..ohlo....omso..",
    "..oooo....oooo..",
    "................",
    "................",
    "................",
    "................",
    "................",
]

CHESTPLATE = [
    "................",
    "..ooo......ooo..",
    ".ohhlo....olmso.",
    ".ohllooooooomso.",
    ".ohlllllllmmmso.",
    "..oohlllllmmoo..",
    "...ohlllllmso...",
    "...ohllhllmso...",
    "...ohlllllmso...",
    "...ohlllllmso...",
    "...ohllllmmso...",
    "...oooooooooo...",
    "................",
    "................",
    "................",
    "................",
]

LEGGINGS = [
    "................",
    "...oooooooooo...",
    "...ohhllllmmo...",
    "...ohlmmmmmso...",
    "...ohlloollmo...",
    "...ohlo..olmo...",
    "...ohlo..olmo...",
    "...ohlo..olmo...",
    "...ohlo..omso...",
    "...ohlo..omso...",
    "...ohlo..omso...",
    "...oooo..oooo...",
    "................",
    "................",
    "................",
    "................",
]

BOOTS = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "...oooo..oooo...",
    "...ohlo..olmo...",
    "...ohlo..olmo...",
    "...ohlo..olmo...",
    "..ohhlo..olmmo..",
    ".ohhllo..olmmso.",
    ".oooooo..oooooo.",
    "................",
    "................",
    "................",
    "................",
]

DUST = [
    "................",
    "................",
    "................",
    "........W.......",
    "................",
    "....W...........",
    "...........W....",
    "................",
    "......aab.......",
    ".....aabbc..W...",
    "....abbccbd.....",
    "...abbccbbdd....",
    "..abbbcccbbdd...",
    "..ddddddddddd...",
    "................",
    "................",
]
DUST_PAL = {"a": "#bdf3ff", "b": "#7fd0ff", "c": "#3a8fd6", "d": "#1d4e89", "W": "#ffffff"}


def lumen_block():
    rnd = random.Random(21)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                c = LUMEN["s"] if (x + y) % 2 else LUMEN["o"]
            elif x in (1, 14) or y in (1, 14):
                c = LUMEN["m"]
            else:
                band = ((x + y) // 3) % 3
                c = [LUMEN["l"], LUMEN["m"], LUMEN["h"]][band] if rnd.random() > 0.08 else LUMEN["s"]
            img.putpixel((x, y), hx(c))
    for (x, y) in [(3, 3), (4, 3), (3, 4), (11, 10), (12, 10), (12, 11)]:
        img.putpixel((x, y), hx("#ffffff"))
    return img


def empty_jar():
    return grid(JAR, {"c": "#6b4423", "C": "#8b5a2b", "g": "#cfe3ea", "W": "#ffffff"})


def jar_glass():
    img = Image.new("RGBA", (16, 16), (210, 235, 245, 40))
    for i in range(16):
        for p in [(i, 0), (i, 15), (0, i), (15, i)]:
            img.putpixel(p, (207, 227, 234, 210))
    for y in range(2, 12):
        img.putpixel((3, y), (255, 255, 255, 150))
    img.putpixel((4, 3), (255, 255, 255, 110))
    return img


def jar_lid():
    rnd = random.Random(4)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), hx(rnd.choice(["#8b5a2b", "#7d5026", "#6b4423", "#9a6634"])))
    return img


def armor_layer(kind, pal=None):
    """Couches d'armure 64x32 (disposition humanoïde vanilla)."""
    LUMEN = pal or globals()["LUMEN"]
    rnd = random.Random(9 if kind == 1 else 10)
    img = Image.new("RGBA", (64, 32), T)

    def face(x0, y0, w, h, mask=None):
        for y in range(y0, y0 + h):
            for x in range(x0, x0 + w):
                if mask and not mask(x - x0, y - y0, w, h):
                    continue
                edge = x in (x0, x0 + w - 1) or y in (y0, y0 + h - 1)
                c = LUMEN["s"] if edge else rnd.choice([LUMEN["m"], LUMEN["m"], LUMEN["l"], LUMEN["h"]])
                img.putpixel((x, y), hx(c))

    def box(u, v, w, h, d, mask_front=None, only_rows=None):
        rects = [
            (u + d, v, w, d), (u + d + w, v, w, d),          # dessus, dessous
            (u, v + d, d, h), (u + d, v + d, w, h),          # droite, devant
            (u + d + w, v + d, d, h), (u + 2 * d + w, v + d, w, h),  # gauche, derrière
        ]
        for i, (x0, y0, ww, hh) in enumerate(rects):
            if only_rows is not None and i >= 2:
                r0, r1 = only_rows
                face(x0, y0 + r0, ww, r1 - r0)
            elif only_rows is not None:
                continue
            else:
                face(x0, y0, ww, hh, mask_front if i == 3 else None)

    if kind == 1:
        # casque : visage ouvert sous les 3 premières lignes
        box(0, 0, 8, 8, 8, mask_front=lambda x, y, w, h: y < 3 or x in (0, w - 1) or y == h - 1 and x < 2)
        box(16, 16, 8, 12, 4)          # torse
        box(40, 16, 4, 12, 4)          # bras
        box(0, 16, 4, 12, 4, only_rows=(8, 12))  # bottes (bas des jambes)
    else:
        box(16, 16, 8, 12, 4, only_rows=(8, 12))  # ceinture
        box(0, 16, 4, 12, 4, only_rows=(0, 9))    # jambes
    return img



def proc_tool(head, handle_top_x, handle_top_y):
    """Outil procédural : tête (ensemble de pixels) + manche diagonal + contour."""
    handle = set()
    x, y = handle_top_x, handle_top_y
    while x >= 0 and y <= 15:
        handle.add((x, y)); x -= 1; y += 1
    union = head | handle
    img = Image.new("RGBA", (16, 16), T)
    for (px, py) in union:
        if (px, py) in head:
            up = (px, py - 1) in head; left = (px - 1, py) in head
            down = (px, py + 1) in head; right = (px + 1, py) in head
            if not up or not left:
                c = LUMEN["h"]
            elif not down or not right:
                c = LUMEN["s"]
            else:
                c = LUMEN["l"] if (px + py) % 3 else LUMEN["m"]
        else:
            c = "#7a4f2a" if (px + py) % 4 else "#5b3a1e"
        img.putpixel((px, py), hx(c))
    for yy in range(16):
        for xx in range(16):
            if (xx, yy) in union:
                continue
            if any((xx + dx, yy + dy) in union for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                img.putpixel((xx, yy), hx(LUMEN["o"]))
    return img


def span(rows):
    return {(x, y) for y, (a, b) in rows.items() for x in range(a, b + 1)}


AXE_HEAD = span({1: (6, 9), 2: (4, 10), 3: (3, 10), 4: (3, 9), 5: (3, 8), 6: (4, 7), 7: (5, 6)})
SHOVEL_HEAD = {(x, y) for y in range(0, 8) for x in range(8, 16) if (x - 11.5) ** 2 + (y - 3.0) ** 2 <= 7.5}
HOE_HEAD = span({1: (5, 11), 2: (5, 12), 3: (11, 12)})


# ======================================================================
# Phase 3 : autel, lanternes, bâton, orbes, interface
# ======================================================================
RAMPS["lumen"] = ["#5a3d14", "#a8742a", "#e8b84a", "#ffe27a", "#fff6c8", "#ffffff"]


def orb(ramp):
    pal = [hx(c) for c in RAMPS[ramp]]
    img = Image.new("RGBA", (16, 16), T)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            if d > 6.2:
                continue
            v = 1 - d / 6.2
            i = 5 if v > .7 else 4 if v > .5 else 3 if v > .3 else 2 if v > .12 else 1
            img.putpixel((x, y), pal[i])
    for p in [(5, 5), (6, 5), (5, 6)]:
        img.putpixel(p, pal[5])
    return img


def altar_top():
    rnd = random.Random(31)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            c = rnd.choice(["#3a3e4a", "#363a45", "#40444f"])
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            if x in (0, 15) or y in (0, 15):
                c = "#2a2d36"
            elif 5.2 < d < 6.3:
                c = LUMEN["m"] if (x + y) % 3 else LUMEN["l"]
            elif d < 2.2:
                c = LUMEN["h"] if d < 1.0 else LUMEN["l"]
            elif x in (1, 14) or y in (1, 14):
                c = "#4a4f5c"
            img.putpixel((x, y), hx(c))
    for (x, y) in [(8, 3), (8, 12), (3, 8), (12, 8)]:
        img.putpixel((x, y), hx(LUMEN["h"]))
    return img


def altar_side():
    rnd = random.Random(32)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            if y == 0:
                c = LUMEN["l"]
            elif y == 1:
                c = LUMEN["m"]
            elif y == 2:
                c = LUMEN["s"]
            elif y < 6:
                c = "#2a2d36"
            else:
                c = rnd.choice(["#3a3e4a", "#363a45", "#40444f"])
                if x in (3, 12):
                    c = "#2a2d36"
                if y == 6 or y == 15:
                    c = "#2a2d36"
                if x in (7, 8) and 8 <= y <= 13:
                    c = LUMEN["s"] if y % 2 else LUMEN["m"]
            img.putpixel((x, y), hx(c))
    return img


def altar_bottom():
    rnd = random.Random(33)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), hx(rnd.choice(["#2a2d36", "#30333d", "#363a45"])))
    return img


def lantern_block_texture(ramp, metal):
    """Disposition propre à nos modèles de lanterne (voir gen_assets.lantern_models)."""
    r = RAMPS[ramp]
    m_dark, m_mid, m_light = metal
    img = Image.new("RGBA", (16, 16), T)
    # corps : (0,0)-(6,7)
    for y in range(7):
        for x in range(6):
            if x in (0, 5) or y in (0, 6):
                c = m_mid if (x + y) % 2 else m_dark
            else:
                d = math.hypot(x - 2.5, y - 3)
                c = r[5] if d < 1.0 else r[4] if d < 1.8 else r[3]
            img.putpixel((x, y), hx(c))
    # dessus/dessous du corps : (0,7)-(6,13)
    for y in range(7, 13):
        for x in range(6):
            c = m_dark if x in (0, 5) or y in (7, 12) else (r[3] if 2 <= x <= 3 and 9 <= y <= 10 else m_mid)
            img.putpixel((x, y), hx(c))
    # côtés du chapeau : (6,0)-(10,2)
    for y in range(0, 2):
        for x in range(6, 10):
            img.putpixel((x, y), hx(m_light if y == 0 else m_mid))
    # dessus du chapeau : (6,2)-(10,6)
    for y in range(2, 6):
        for x in range(6, 10):
            img.putpixel((x, y), hx(m_dark if x in (6, 9) or y in (2, 5) else m_light))
    # chaîne / anse : (11,0)-(14,6)
    for y in range(0, 6):
        for x in range(11, 14):
            if (y % 2 == 0 and x != 12) or (y % 2 == 1 and x == 12):
                img.putpixel((x, y), hx(m_mid))
    return img


def lantern_item(ramp, metal):
    r = RAMPS[ramp]
    m_dark, m_mid, m_light = metal
    rows = [
        "................",
        ".......cc.......",
        "......c..c......",
        ".......cc.......",
        "......LLLL......",
        ".....oMMMMo.....",
        "....oMMMMMMo....",
        "....og1221go....",
        "....og2332go....",
        "....og3443go....",
        "....og2332go....",
        "....og1221go....",
        "....oMMMMMMo....",
        ".....oooooo.....",
        "................",
        "................",
    ]
    pal = {"c": m_mid, "L": m_light, "M": m_mid, "o": m_dark, "g": m_dark,
           "1": r[2], "2": r[3], "3": r[4], "4": r[5]}
    return grid(rows, pal)


def gui_altar():
    W, H = 256, 256
    img = Image.new("RGBA", (W, H), T)
    px = img.load()

    def rect(x0, y0, w, h, c):
        for y in range(y0, y0 + h):
            for x in range(x0, x0 + w):
                px[x, y] = hx(c)

    def slot(x, y, big=False):
        s = 26 if big else 18
        x0, y0 = (x - 5, y - 5) if big else (x - 1, y - 1)
        rect(x0, y0, s, s, "#0e1228")
        rect(x0, y0, s, 1, "#070914"); rect(x0, y0, 1, s, "#070914")
        rect(x0, y0 + s - 1, s, 1, "#4a5590"); rect(x0 + s - 1, y0, 1, s, "#4a5590")
        if big:
            rect(x0 - 1, y0 - 1, s + 2, 1, LUMEN["s"]); rect(x0 - 1, y0 + s, s + 2, 1, LUMEN["s"])
            rect(x0 - 1, y0 - 1, 1, s + 2, LUMEN["s"]); rect(x0 + s, y0 - 1, 1, s + 2, LUMEN["s"])

    # panneau
    rect(0, 0, 176, 166, "#1b2142")
    rect(0, 0, 176, 2, "#0d1024"); rect(0, 164, 176, 2, "#0d1024")
    rect(0, 0, 2, 166, "#0d1024"); rect(174, 0, 2, 166, "#0d1024")
    rect(2, 2, 172, 1, "#3a4680"); rect(2, 2, 1, 162, "#3a4680")
    rnd = random.Random(5)
    for _ in range(40):
        x, y = rnd.randrange(4, 172), rnd.randrange(4, 74)
        px[x, y] = hx(rnd.choice(["#2c3566", "#34407a", "#bdf3ff"]))
    # séparation inventaire
    rect(7, 76, 162, 1, "#3a4680")
    # emplacements machine
    slot(27, 35)
    slot(63, 17)
    slot(63, 53)
    slot(134, 35, big=True)
    # liens essence → flèche
    for x in range(81, 85):
        px[x, 25] = hx("#3a4680"); px[x, 61] = hx("#3a4680")
    for y in range(25, 62):
        px[84, y] = hx("#3a4680")
    for x in range(45, 61):
        px[x, 43] = hx("#3a4680")
    # flèche vide (85,35) 32x16 et pleine (176,0)
    def arrow(ox, oy, body, head):
        for y in range(16):
            for x in range(32):
                inside_body = 5 <= y <= 10 and x < 22
                inside_head = x >= 22 and abs(y - 7.5) <= (31 - x) * 0.8
                if inside_body:
                    px[ox + x, oy + y] = hx(body)
                elif inside_head:
                    px[ox + x, oy + y] = hx(head)
    arrow(85, 35, "#2c3566", "#2c3566")
    arrow(176, 0, LUMEN["l"], LUMEN["h"])
    # témoins follet (176,16) allumé, (192,16) éteint
    img.alpha_composite(wisp("azur"), (176, 16))
    grey = wisp("givre").convert("LA").convert("RGBA")
    for y in range(16):
        for x in range(16):
            r_, g_, b_, a_ = grey.getpixel((x, y))
            if a_:
                grey.putpixel((x, y), (r_ // 3, g_ // 3, b_ // 3 + 20, a_))
    img.alpha_composite(grey, (192, 16))
    # inventaire joueur
    for row in range(3):
        for col in range(9):
            slot(8 + col * 18, 84 + row * 18)
    for col in range(9):
        slot(8 + col * 18, 142)
    return img


IRON = ("#2b2d33", "#44474f", "#62656e")
LUMEN_METAL = (LUMEN["o"], LUMEN["s"], LUMEN["m"])


def phase3_textures():
    out = {
        "block/infusion_altar_top": altar_top(),
        "block/infusion_altar_side": altar_side(),
        "block/infusion_altar_bottom": altar_bottom(),
        "block/calling_lantern": lantern_block_texture("azur", IRON),
        "item/calling_lantern": call_lantern(),
        "item/wisp_staff": staff("lumen"),
        "entity/wisp_orb/plain": orb("lumen"),
        "gui/infusion_altar": gui_altar(),
    }
    for r in ("azur", "emeraude", "braise", "givre"):
        out[f"block/lumen_lantern_{r}"] = lantern_block_texture(r, LUMEN_METAL)
        out[f"item/lumen_lantern_{r}"] = lantern_item(r, LUMEN_METAL)
        out[f"item/charm_{r}"] = charm(r)
        out[f"entity/wisp_orb/{r}"] = orb(r)
    return out


# ======================================================================
# Phase 4 : Ombres, Armure du Veilleur
# ======================================================================
WATCHER = {"o": "#0b0810", "s": "#241a36", "m": "#3b2a55", "l": "#5d4590", "h": "#ffe27a"}


def humanoid_texture(fill, seed):
    """Texture 64x64 de modèle humanoïde (disposition zombie) : fill(x, y, part, face) -> couleur ou None."""
    img = Image.new("RGBA", (64, 64), T)
    parts = {"head": (0, 0, 8, 8, 8), "body": (16, 16, 8, 12, 4), "arm": (40, 16, 4, 12, 4), "leg": (0, 16, 4, 12, 4)}
    for part, (u, v, w, h, d) in parts.items():
        rects = {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
                 "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}
        for face, (x0, y0, ww, hh) in rects.items():
            for y in range(hh):
                for x in range(ww):
                    c = fill(x, y, ww, hh, part, face)
                    if c:
                        img.putpixel((x0 + x, y0 + y), hx(c) if isinstance(c, str) else c)
    return img


def shade_fill(x, y, w, h, part, face):
    r = random.Random(x * 131 + y * 17 + len(part) * 7 + len(face))
    base = r.choice(["#1a1424", "#1a1424", "#140f1d", "#221a30"])
    if part == "head" and face == "front" and y in (3, 4) and x in (1, 2, 5, 6):
        return "#f4f0ff"
    if part in ("arm", "leg") and face in ("front", "back", "left", "right") and x in (0, w - 1) and r.random() < 0.6:
        return None  # membres effilés
    if part in ("arm", "leg") and y >= h - 3 and r.random() < 0.5:
        return None  # extrémités qui se dissipent
    if r.random() < 0.08:
        return "#3b2a55"
    return base


def shade_eyes_fill(x, y, w, h, part, face):
    if part == "head" and face == "front" and y in (3, 4) and x in (1, 2, 5, 6):
        return "#f4f0ff" if y == 3 else "#b9a6e8"
    return None


SHADOW_FRAGMENT = [
    "................",
    "................",
    "..........ho....",
    ".........hlmo...",
    "........hlmso...",
    ".......hlmso....",
    "...o..hlmso.....",
    "..olohlmso......",
    "..ommlmso.......",
    "...omlmso.......",
    "...ommsso.......",
    "....osso........",
    ".....oo.........",
    "................",
    "................",
    "................",
]
SHADOW_PAL = {"o": "#0b0810", "s": "#1a1424", "m": "#3b2a55", "l": "#6a4f9a", "h": "#b9a6e8"}


def reinforced_jar():
    img = grid(JAR, {"c": "#2b2d33", "C": "#44474f", "g": "#cfe3ea", "W": "#ffffff"})
    for y in (5, 12):
        for x in range(3, 13):
            if img.getpixel((x, y))[3]:
                img.putpixel((x, y), hx("#3b2a55"))
    for x in range(4, 12):
        img.putpixel((x, 5), hx("#3b2a55"))
        img.putpixel((x, 12), hx("#3b2a55"))
    return img


def phase4_textures():
    return {
        "entity/creeping_shade": humanoid_texture(shade_fill, 1),
        "entity/creeping_shade_eyes": humanoid_texture(shade_eyes_fill, 2),
        "item/shadow_fragment": grid(SHADOW_FRAGMENT, SHADOW_PAL),
        "item/reinforced_jar": reinforced_jar(),
        "item/watcher_helmet": grid(HELMET, WATCHER),
        "item/watcher_chestplate": grid(CHESTPLATE, WATCHER),
        "item/watcher_leggings": grid(LEGGINGS, WATCHER),
        "item/watcher_boots": grid(BOOTS, WATCHER),
        "models/armor/watcher_layer_1": armor_layer(1, WATCHER),
        "models/armor/watcher_layer_2": armor_layer(2, WATCHER),
    }


# ======================================================================
# Phase 5 : sanctuaire, boussole, Veilleur Éteint
# ======================================================================
def cracked(img, seed=8):
    rnd = random.Random(seed)
    out = img.copy()
    x, y = rnd.randrange(2, 6), 0
    while y < 16:
        out.putpixel((x % 16, y), hx("#2a2d36"))
        y += 1
        x += rnd.choice([-1, 0, 1, 1])
    for _ in range(5):
        out.putpixel((rnd.randrange(16), rnd.randrange(16)), hx("#3d414b"))
    return out


def sanctuary_altar_tex(face):
    rnd = random.Random({"top": 41, "side": 42, "bottom": 43}[face])
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            c = rnd.choice(["#2c2f38", "#30333d", "#363a45"])
            if x in (0, 15) or y in (0, 15):
                c = LUMEN["s"] if face != "bottom" else "#24262d"
            img.putpixel((x, y), hx(c))
    if face == "top":
        for y in range(16):
            for x in range(16):
                d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
                if 4.6 < d < 5.6:
                    img.putpixel((x, y), hx(LUMEN["m"]))
        for (x, y), v in {(8, 2): "azur", (13, 8): "emeraude", (8, 13): "braise", (2, 8): "givre"}.items():
            img.putpixel((x, y), hx(RAMPS[v][4]))
            img.putpixel((x - 1, y), hx(RAMPS[v][3]))
        for (x, y) in [(7, 7), (8, 7), (7, 8), (8, 8)]:
            img.putpixel((x, y), hx("#141a26"))
    elif face == "side":
        for x in range(2, 14):
            img.putpixel((x, 3), hx(LUMEN["m"]))
            img.putpixel((x, 12), hx(LUMEN["s"]))
        for (x, y) in [(5, 7), (6, 8), (7, 7), (9, 8), (10, 7), (11, 8)]:
            img.putpixel((x, y), hx(LUMEN["l"]))
    return img


def compass_frame(f):
    img = Image.new("RGBA", (16, 16), T)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            if d <= 7.4:
                img.putpixel((x, y), hx(LUMEN["s"] if d > 6.3 else LUMEN["m"] if d > 5.6 else "#1b2142"))
    angle = math.radians((f / 32.0 - 0.5) * 360.0)
    dx, dy = math.sin(angle), -math.cos(angle)
    for i in range(0, 50):
        t_ = i / 10.0
        if t_ <= 4.6:
            px_, py_ = 7.5 + dx * t_, 7.5 + dy * t_
            img.putpixel((int(round(px_)), int(round(py_))), hx(RAMPS["azur"][4] if t_ > 2.5 else RAMPS["azur"][3]))
        if t_ <= 2.6:
            px_, py_ = 7.5 - dx * t_, 7.5 - dy * t_
            img.putpixel((int(round(px_)), int(round(py_))), hx("#8a90a8"))
    img.putpixel((7, 7), hx(LUMEN["h"]))
    img.putpixel((8, 8), hx(LUMEN["h"]))
    return img


HEART = [
    "................",
    "................",
    "...ooo....ooo...",
    "..o445o..o554o..",
    ".o45554oo45554o.",
    ".o45555445555So.",
    ".o44555555554So.",
    ".o34455555543So.",
    "..o344555443So..",
    "...o3445543So...",
    "....o34443So....",
    ".....o343So.....",
    "......o3So......",
    ".......oo.......",
    "................",
    "................",
]


def lantern_heart():
    r = RAMPS["braise"]
    return grid(HEART, {"o": "#3d1004", "3": r[2], "4": r[3], "5": r[4], "S": r[1]})


def watcher_staff():
    img = grid(STAFF, {"o": "#0b0810", "w": "#3b2a55", "g": "#ffe27a"})
    pal = [hx(c) for c in RAMPS["braise"]]
    for y in range(0, 7):
        for x in range(8, 16):
            d = math.hypot(x + .5 - 12, y + .5 - 3.5)
            if d < 3.2:
                v = 1 - d / 3.2
                i = 5 if v > .7 else 4 if v > .5 else 3 if v > .3 else 2
                if d > 2.5: i = 1
                img.putpixel((x, y), pal[i])
    for p in [(9, 5), (14, 5), (9, 1), (14, 1), (11, 0), (12, 0)]:
        img.putpixel(p, hx("#ffe27a"))
    return img


def box_regions(u, v, w, h, d):
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
            "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


def warden_body():
    img = Image.new("RGBA", (128, 128), T)
    rnd = random.Random(77)
    iron = ["#2b2d33", "#33363d", "#3a3d45"]

    def paint(box, fn):
        for face, (x0, y0, w, h) in box_regions(*box).items():
            for y in range(h):
                for x in range(w):
                    c = fn(x, y, w, h, face)
                    if c:
                        img.putpixel((x0 + x, y0 + y), hx(c))

    def metal(trim=None):
        def fn(x, y, w, h, face):
            if trim and (y == 0 or y == h - 1) and face not in ("top", "bottom"):
                return trim
            if face in ("top", "bottom") and (x in (0, w - 1) or y in (0, h - 1)):
                return trim or "#44474f"
            if (x * 7 + y * 3) % 11 == 0:
                return "#62656e"
            return rnd.choice(iron)
        return fn

    paint((0, 0, 20, 4, 20), metal("#a8742a"))          # socle
    paint((0, 24, 3, 32, 3), lambda x, y, w, h, f: "#44474f" if y % 8 == 0 else rnd.choice(iron))  # barreaux
    paint((0, 64, 24, 4, 24), metal("#e8b84a"))         # toit
    paint((0, 92, 14, 4, 14), metal("#a8742a"))         # toit supérieur
    paint((60, 92, 4, 6, 2), lambda x, y, w, h, f: "#62656e" if (x + y) % 2 else "#44474f")  # anneau
    paint((60, 24, 2, 20, 2), lambda x, y, w, h, f: "#62656e" if y % 3 else "#2b2d33")      # chaînes
    return img


def warden_flame(ramp, eyes=True):
    img = Image.new("RGBA", (128, 128), T)
    pal = RAMPS[ramp]
    for face, (x0, y0, w, h) in box_regions(16, 24, 10, 18, 10).items():
        for y in range(h):
            for x in range(w):
                if face in ("top", "bottom"):
                    d = math.hypot(x - w / 2 + 0.5, y - h / 2 + 0.5) / (w / 2)
                    i = 5 if d < 0.4 else 4 if d < 0.8 else 3
                else:
                    cx = abs(x - w / 2 + 0.5) / (w / 2)
                    fy = y / h
                    v = (1 - cx) * (0.4 + fy * 0.8)
                    i = 5 if v > 0.75 else 4 if v > 0.5 else 3 if v > 0.28 else 2 if v > 0.12 else 1
                img.putpixel((x0 + x, y0 + y), hx(pal[i]))
        if eyes and face == "front":
            for (ex, ey) in [(2, 8), (3, 8), (6, 8), (7, 8), (2, 9), (7, 9)]:
                img.putpixel((x0 + ex, y0 + ey), (0, 0, 0, 255))
    return img


def warden_orb():
    img = orb("givre")
    pal = ["#1a1424", "#3b2a55", "#6a4f9a", "#b9a6e8", "#e3dcff", "#ffffff"]
    out = Image.new("RGBA", (16, 16), T)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            if d > 6.5:
                continue
            v = 1 - d / 6.5
            i = 5 if v > .75 else 4 if v > .55 else 3 if v > .35 else 2 if v > .15 else 1
            out.putpixel((x, y), hx(pal[i]))
    return out


def phase5_textures():
    bricks_img = bricks()
    out = {
        "block/sanctuary_bricks": bricks_img,
        "block/cracked_sanctuary_bricks": cracked(bricks_img),
        "block/sanctuary_altar_top": sanctuary_altar_tex("top"),
        "block/sanctuary_altar_side": sanctuary_altar_tex("side"),
        "block/sanctuary_altar_bottom": sanctuary_altar_tex("bottom"),
        "item/lantern_heart": lantern_heart(),
        "item/eternal_lantern": lantern_item("lumen", ("#a8742a", "#e8b84a", "#fff6c8")),
        "item/watcher_staff": watcher_staff(),
        "entity/hollow_warden/body": warden_body(),
        "entity/hollow_warden/flame_1": warden_flame("givre"),
        "entity/hollow_warden/flame_2": warden_flame("azur"),
        "entity/hollow_warden/flame_3": warden_flame("braise"),
        "entity/warden_orb": warden_orb(),
    }
    for v in ("azur", "emeraude", "braise", "givre"):
        out[f"block/runestone_{v}"] = runestone(v, glow=0)
        out[f"block/runestone_{v}_charged"] = runestone(v, glow=1)
        out[f"block/runestone_{v}_lit"] = runestone(v, glow=2)
    for f in range(32):
        out[f"item/sanctuary_compass_{f:02d}"] = compass_frame(f)
    return out

# ======================================================================
# Écriture
# ======================================================================
if __name__ == "__main__":
    ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "wisplight", "textures")
    out = {
        "block/lumen_ore": ore(STONE, 11),
        "block/deepslate_lumen_ore": ore(DEEP, 12),
        "block/lumen_block": lumen_block(),
        "block/wisp_jar_glass": jar_glass(),
        "block/wisp_jar_lid": jar_lid(),
        "item/lumen_shard": grid(SHARD, LUMEN),
        "item/lumen_ingot": grid(INGOT, LUMEN),
        "item/wisp_dust": grid(DUST, DUST_PAL),
        "item/empty_jar": empty_jar(),
        "item/lumen_sword": grid(SWORD, SWORD_PAL),
        "item/lumen_pickaxe": grid(PICKAXE, TOOL_PAL),
        "item/lumen_axe": proc_tool(AXE_HEAD, 12, 2),
        "item/lumen_shovel": proc_tool(SHOVEL_HEAD, 9, 6),
        "item/lumen_hoe": proc_tool(HOE_HEAD, 11, 4),
        "item/lumen_helmet": grid(HELMET, LUMEN),
        "item/lumen_chestplate": grid(CHESTPLATE, LUMEN),
        "item/lumen_leggings": grid(LEGGINGS, LUMEN),
        "item/lumen_boots": grid(BOOTS, LUMEN),
        "models/armor/lumen_layer_1": armor_layer(1),
        "models/armor/lumen_layer_2": armor_layer(2),
    }
    for r in ("azur", "emeraude", "braise", "givre"):
        out[f"entity/wisp/{r}"] = wisp(r)
        out[f"item/wisp_jar_{r}"] = jar(r)
        out[f"item/essence_{r}"] = vial(r)
    out.update(phase3_textures())
    out.update(phase4_textures())
    out.update(phase5_textures())
    import shutil
    if os.path.isdir(ROOT):
        shutil.rmtree(ROOT)
    for name, img in out.items():
        path = os.path.join(ROOT, name + ".png")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        img.save(path)
    # planche contact (hors resources) pour validation visuelle
    sheet_items = [k for k in out if not k.startswith(("models/", "gui/", "entity/creeping", "entity/hollow"))
                   and "compass_" not in k[:-2]]
    S = 64
    sheet = Image.new("RGBA", (S * 8 + 16, S * ((len(sheet_items) + 7) // 8) + 16), hx("#20242e"))
    for i, n in enumerate(sheet_items):
        sheet.alpha_composite(out[n].resize((S - 8, S - 8), Image.NEAREST), (8 + (i % 8) * S, 8 + (i // 8) * S))
    os.makedirs("build", exist_ok=True)
    sheet.save("build/contact_sheet.png")
    for k in ("models/armor/lumen_layer_1", "models/armor/lumen_layer_2"):
        out[k].resize((256, 128), Image.NEAREST).save("build/" + k.split("/")[-1] + "_preview.png")
    out["entity/creeping_shade"].resize((256, 256), Image.NEAREST).save("build/shade_preview.png")
    wb = out["entity/hollow_warden/body"].copy()
    wb.alpha_composite(out["entity/hollow_warden/flame_3"])
    wb.resize((384, 384), Image.NEAREST).save("build/warden_preview.png")
    out["gui/infusion_altar"].crop((0, 0, 230, 166)).resize((690, 498), Image.NEAREST).save("build/gui_preview.png")
    print(len(out), "textures écrites")
