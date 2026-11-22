#!/usr/bin/env python3
"""
Generates core/.../level/Levels.kt from a layout measured in pixels on a 2340x1080 reference screenshot.

Scale: 100 px = 1 m.  x_m = x_px / 100,  y_m = (1080 - y_px) / 100.  The ground surface (py 885) is y = 1.95 m.
Each block is a rectangle (x0, y0, x1, y1) in pixels; it is shrunk by 1 cm on every side pair so stacked
pieces never start overlapping (they settle in a few frames).

Usage: python3 tools/generate_level.py
"""
import os

K = 100.0
H = 1080
GAP = 0.01
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "core", "src", "main", "kotlin",
                   "com", "example", "slingshot", "level", "Levels.kt")

blocks = []   # (material, x0, y0, x1, y1)
pigs = []     # (cx_px, bottom_py)
terrain = []  # polygons in px


def B(mat, x0, y0, x1, y1):
    blocks.append((mat, x0, y0, x1, y1))


# ---------------------------------------------------------------- left glass tower (on the mound, base py 818)
for x0, x1 in ((1070, 1086), (1107, 1123), (1143, 1159)):
    B("GLASS", x0, 745, x1, 818)
    B("GLASS", x0, 673, x1, 745)
for cols in ((1061, 1095), (1099, 1133), (1135, 1169)):
    for y0, y1 in ((638, 672), (603, 637), (568, 602)):
        B("GLASS", cols[0], y0, cols[1], y1)
B("GLASS", 1041, 550, 1189, 566)
B("GLASS", 1045, 478, 1060, 549)
B("GLASS", 1170, 478, 1186, 549)
B("GLASS", 1042, 461, 1190, 477)
B("GLASS", 1080, 443, 1152, 459)
pigs.append((1111, 550))

# ---------------------------------------------------------------- middle structure
B("TNT", 1256, 825, 1317, 885)                              # crate on the ground
for x0, x1 in ((1322, 1340), (1411, 1447), (1555, 1591), (1666, 1681)):
    B("STONE", x0, 812, x1, 885)
for x0, x1 in ((1283, 1429), (1429, 1575), (1575, 1721)):
    B("STONE", x0, 797, x1, 812)
for x0, x1 in ((1314, 1330), (1351, 1366), (1386, 1402), (1602, 1617), (1638, 1654), (1674, 1691)):
    B("GLASS", x0, 725, x1, 796)
for cols in ((1304, 1338), (1341, 1375), (1378, 1412), (1592, 1627), (1630, 1664), (1666, 1700)):
    for y0, y1 in ((691, 724), (655, 690), (619, 654)):
        B("GLASS", cols[0], y0, cols[1], y1)
B("GLASS", 1412, 779, 1594, 795)
B("GLASS", 1449, 707, 1466, 778)
B("GLASS", 1539, 707, 1555, 778)
B("GLASS", 1412, 691, 1594, 706)
B("GLASS", 1454, 618, 1469, 690)
B("GLASS", 1535, 618, 1550, 690)
B("TNT", 1472, 629, 1533, 689)
for x0, x1 in ((1266, 1412), (1412, 1592), (1591, 1739)):
    B("GLASS", x0, 602, x1, 617)
B("GLASS", 1305, 529, 1321, 601)
B("GLASS", 1396, 529, 1429, 601)
B("GLASS", 1576, 529, 1609, 601)
B("GLASS", 1683, 529, 1699, 601)
for x0, x1 in ((1266, 1412), (1414, 1592), (1592, 1739)):
    B("GLASS", x0, 514, x1, 529)
B("GLASS", 1357, 493, 1502, 509)
B("GLASS", 1502, 493, 1649, 509)
B("GLASS", 1431, 422, 1447, 492)
B("GLASS", 1557, 422, 1574, 492)
B("GLASS", 1429, 406, 1577, 422)
B("GLASS", 1466, 387, 1539, 403)
pigs.append((1355, 602))
pigs.append((1650, 602))

# ---------------------------------------------------------------- right glass tower (on the ledge, base py 758)
for x0, x1 in ((1792, 1809), (1828, 1845), (1863, 1880)):
    B("GLASS", x0, 688, x1, 758)
for cols in ((1782, 1815), (1818, 1851), (1855, 1888)):
    for y0, y1 in ((655, 688), (620, 654)):
        B("GLASS", cols[0], y0, cols[1], y1)
B("GLASS", 1761, 601, 1909, 618)
B("GLASS", 1763, 529, 1780, 600)
B("GLASS", 1890, 529, 1907, 600)
B("GLASS", 1761, 513, 1909, 529)
B("GLASS", 1800, 495, 1872, 511)
pigs.append((1831, 601))

# ---------------------------------------------------------------- top tower (on the terrace, base py 525)
for x0, x1 in ((1951, 1969), (1988, 2005), (2025, 2042)):
    B("WOOD", x0, 455, x1, 525)
    B("WOOD", x0, 386, x1, 455)
for cols in ((1944, 1978), (1980, 2015), (2017, 2052)):
    for y0, y1 in ((352, 385), (317, 350), (281, 314)):
        B("WOOD", cols[0], y0, cols[1], y1)
B("STONE", 1923, 262, 2071, 278)
B("STONE", 1926, 188, 1942, 262)
B("STONE", 2053, 188, 2069, 262)
B("STONE", 1923, 173, 2071, 190)
pigs.append((1994, 262))

# ---------------------------------------------------------------- terrain (static rock), convex pieces
terrain.append([(985, 885), (1035, 818), (1195, 818), (1245, 885)])           # mound under the left tower
terrain.append([(1765, 758), (1936, 758), (1936, 885), (1765, 885)])          # ledge
terrain.append([(1936, 525), (2063, 525), (2063, 885), (1936, 885)])          # terrace
terrain.append([(2063, 300), (2308, 300), (2308, 885), (2063, 885)])          # tall rock
terrain.append([(2063, 300), (2186, 181), (2308, 300)])                       # rock peak


def m(px):
    return px / K


def my(py):
    return (H - py) / K


def f(v):
    return ("%.3f" % v).rstrip("0").rstrip(".") + "f"


def write():
    lines = []
    for mat, x0, y0, x1, y1 in blocks:
        w = (x1 - x0) / K - GAP
        h = (y1 - y0) / K - GAP
        cx = (x0 + x1) / 2 / K
        bottom = my(y1) + GAP / 2
        lines.append("            BlockSpec(Material.%s, %s, %s, %s, %s)" % (mat, f(cx), f(bottom), f(w), f(h)))
    pig_lines = ["            EnemySpec(x = %s, bottomY = %s)" % (f(px / K), f(my(py) + 0.005)) for px, py in pigs]
    terr_lines = []
    for poly in terrain:
        flat = ", ".join("%s, %s" % (f(m(x)), f(my(y))) for x, y in poly)
        terr_lines.append("            floatArrayOf(%s)" % flat)

    src = '''package com.example.slingshot.level

    import com.example.slingshot.entities.BirdType
    import com.example.slingshot.entities.Material

    /**
     * The hardcoded levels. GENERATED by tools/generate_level.py from a layout measured on a reference
     * screenshot (100 px = 1 m); edit the script and run it again instead of editing the numbers by hand.
     *
     * Level 1: three glass towers, a big structure with two TNT crates, a wooden stilt tower with a stone
     * cage on a tall rock, five pigs. Birds (in firing order): blue, yellow, blue, red, red.
     */
    object Levels {

        /** Where the level's terrain picture is drawn (meters): lower-left corner and size. */
        const val TERRAIN_X = 9.8f
        const val TERRAIN_Y = 1.95f
        const val TERRAIN_WIDTH = 13.6f
        const val TERRAIN_HEIGHT = 7.05f

        val LEVEL_1 = LevelData(
            blocks = listOf(
    %s
            ),
            enemies = listOf(
    %s
            ),
            birds = listOf(BirdType.BLUE, BirdType.YELLOW, BirdType.BLUE, BirdType.RED, BirdType.RED),
            terrain = listOf(
    %s
            )
        )

        /** All levels that exist, in order. */
        val ALL: List<LevelData> = listOf(LEVEL_1)

        /** How many level nodes the level menu shows (the ones beyond [ALL] are locked). */
        const val MENU_SLOTS = 14

        fun isUnlocked(index: Int): Boolean = index in ALL.indices

        fun get(index: Int): LevelData = ALL[index.coerceIn(0, ALL.size - 1)]
    }
    ''' % (",\n".join(lines), ",\n".join(pig_lines), ",\n".join(terr_lines))
    open(OUT, "w").write(src)
    print("blocks:", len(blocks), "pigs:", len(pigs), "terrain pieces:", len(terrain))


if __name__ == "__main__":
    write()
