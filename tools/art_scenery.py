"""
Scenery, block materials and UI of the "bright cartoon" theme (soft pastel scenery, lime grass strip,
blue-grey rock underground, orange wood, ice-blue glass). Imported by generate_art.py.
"""
import math
import random

import numpy as np
from PIL import Image, ImageChops, ImageDraw, ImageFilter, ImageFont

from generate_art import Art, clamp8, _periodic_noise  # noqa: E402

TILE = 256
FONT_PATH = "/usr/share/fonts/truetype/google-fonts/ofl/archivoblack/ArchivoBlack-Regular.ttf"


# --------------------------------------------------------------------------- blocks (tileable, 256 px = 0.5 m)
def make_wood():
    """Bright orange wood with soft grain (cubes and stilts)."""
    rng = np.random.default_rng(7)
    size = TILE
    y, x = np.mgrid[0:size, 0:size].astype(np.float32)
    ly = y / size
    base = np.array([236, 150, 54], np.float32)
    shade = 1.12 - 0.26 * ly
    ph = rng.uniform(0, 6.283, 4)
    grain = (0.06 * np.sin(2 * np.pi * (ly * 9 + 0.5 * np.sin(2 * np.pi * 2 * x / size + ph[0])
                                         + 0.2 * np.sin(2 * np.pi * 5 * x / size + ph[1])))
             + 0.03 * np.sin(2 * np.pi * (ly * 31 + 0.4 * np.sin(2 * np.pi * 3 * x / size + ph[2]))))
    streak = np.zeros_like(ly)
    for _ in range(7):
        yl = rng.uniform(0.05, 0.95)
        c, p = rng.integers(1, 4), rng.uniform(0, 6.283)
        streak += np.exp(-(((ly - yl - 0.03 * np.sin(2 * np.pi * c * x / size + p)) * size) / 2.2) ** 2)
    col = base[None, None, :] * (shade * (1 + grain) * (1 - 0.30 * np.clip(streak, 0, 1)))[..., None]
    out = Image.fromarray(clamp8(col), "RGB").convert("RGBA")
    d = ImageDraw.Draw(out, "RGBA")
    d.rectangle([0, 0, size, 5], fill=(255, 214, 140, 150))           # light top edge
    for kx, ky in ((70, 70), (190, 180)):
        for r, c in ((14, (196, 108, 30)), (9, (170, 88, 22)), (4, (130, 62, 14))):
            d.ellipse([kx - r * 1.5, ky - r * 0.75, kx + r * 1.5, ky + r * 0.75], fill=c + (255,))
    return out


def make_stone():
    """Flat grey stone with soft mottling and a few hairline cracks."""
    rng = np.random.default_rng(11)
    size = TILE
    n = _periodic_noise(size, rng)
    n2 = _periodic_noise(size, rng, ((4, 0.5), (7, 0.3), (11, 0.2)))
    y = np.mgrid[0:size, 0:size][0].astype(np.float32) / size
    base = np.array([158, 158, 166], np.float32)
    img = base[None, None, :] * ((1.08 - 0.16 * y)[..., None] * (1 + 0.06 * n[..., None] + 0.04 * n2[..., None]))
    out = Image.fromarray(clamp8(img), "RGB").convert("RGBA")
    d = ImageDraw.Draw(out, "RGBA")
    for _ in range(4):
        x0, y0 = rng.uniform(10, size - 10), rng.uniform(10, size - 10)
        pts = [(x0, y0)]
        for _k in range(5):
            x0 += rng.uniform(-18, 18)
            y0 += rng.uniform(6, 20)
            pts.append((x0, y0))
        d.line(pts, fill=(96, 96, 106, 150), width=2)
    d.rectangle([0, 0, size, 5], fill=(226, 226, 232, 120))
    return out


def make_glass():
    """Ice-blue glass: opaque light blue with diagonal glints."""
    size = TILE
    y, x = np.mgrid[0:size, 0:size].astype(np.float32)
    t = (y / size)[..., None]
    top = np.array([204, 240, 252], np.float32)
    bot = np.array([128, 200, 236], np.float32)
    img = np.zeros((size, size, 4), np.float32)
    img[..., :3] = top * (1 - t) + bot * t
    img[..., 3] = 245
    d = (x + y) % size
    glint = ((d > 28) & (d < 62)).astype(np.float32) * 0.6 + ((d > 84) & (d < 98)).astype(np.float32) * 0.4
    img[..., :3] = img[..., :3] * (1 - glint[..., None]) + 255 * glint[..., None]
    return Image.fromarray(clamp8(img), "RGBA")


def make_tnt():
    """A wooden TNT crate (not tiled): planks, metal straps and a white TNT label."""
    a = Art(128, 128, 4)
    box = a.rrect(4, 4, 124, 124, 10)
    a.paint(box, a.linear((0, 4), (0, 124), (214, 138, 70), (170, 98, 44)), outline=(70, 36, 14), ow=4)
    for x in (34, 64, 94):
        a.paint(a.rrect(x - 1, 8, x + 1, 120, 1), (110, 60, 24), clip=box, opacity=0.8)
    a.paint(a.rrect(4, 22, 124, 32, 2), (120, 68, 30), clip=box, opacity=0.85)
    a.paint(a.rrect(4, 96, 124, 106, 2), (120, 68, 30), clip=box, opacity=0.85)
    a.paint(a.rrect(12, 10, 116, 16, 3), (255, 214, 160), clip=box, opacity=0.35)
    img = a.finish(128, 128)
    label = Image.new("RGBA", (96, 44), (0, 0, 0, 0))
    ld = ImageDraw.Draw(label)
    ld.rounded_rectangle([0, 0, 95, 43], radius=5, fill=(250, 246, 240, 255), outline=(150, 120, 100, 255), width=2)
    font = ImageFont.truetype(FONT_PATH, 30)
    ld.text((48, 23), "TNT", font=font, fill=(214, 40, 40, 255), anchor="mm")
    label = label.rotate(8, expand=True, resample=Image.BICUBIC)
    img.alpha_composite(label, (64 - label.width // 2, 64 - label.height // 2))
    return img


# --------------------------------------------------------------------------- ground strip (4 m x 1.95 m)
def make_ground():
    """1024 x 512 tile = 4 m x 1.95 m. A bright lime grass strip on top of a blue-grey rock underground
    whose lower part is covered by dark jungle leaves. Tiles seamlessly in x."""
    S = 2
    W, H = 1024 * S, 512 * S
    rng = np.random.default_rng(5)
    y, x = np.mgrid[0:H, 0:W].astype(np.float32)
    strip = 62 * S + 3 * S * np.sin(2 * np.pi * 4 * x / W) + 1.5 * S * np.sin(2 * np.pi * 11 * x / W + 0.7)
    t = np.clip((y - strip) / (H - strip), 0, 1)[..., None]
    top = np.array([110, 152, 160], np.float32)
    bot = np.array([62, 104, 116], np.float32)
    soil = top * (1 - t) + bot * t
    soil *= 1 + 0.06 * _noise_wh(W, H, rng)[..., None]
    gt = np.clip(y / strip, 0, 1)[..., None]
    grass = np.array([226, 244, 36], np.float32) * (1 - gt) + np.array([176, 208, 0], np.float32) * gt
    img = np.where((y < strip)[..., None], grass, soil)
    lip = (y >= strip) & (y < strip + 5 * S)
    img[lip] = np.array([122, 160, 8], np.float32)
    rgba = np.dstack([img, np.full((H, W), 255, np.float32)])
    tile = Image.fromarray(clamp8(rgba), "RGBA")
    big = Image.new("RGBA", (W * 3, H))
    for i in range(3):
        big.paste(tile, (i * W, 0))
    d = ImageDraw.Draw(big, "RGBA")

    def wrap(fn, px):
        for ox in (0, W, 2 * W):
            fn(px + ox)

    def sy(px):
        px = px % W
        return 62 * S + 3 * S * math.sin(2 * math.pi * 4 * px / W) + 1.5 * S * math.sin(2 * math.pi * 11 * px / W + 0.7)

    # layered rock bands with scalloped edges
    for k in range(6):
        yb = (96 + k * 42) * S
        pts = []
        for i in range(0, W + 1, 8 * S):
            pts.append((i, yb + 7 * S * math.sin(2 * math.pi * (3 + k % 3) * i / W + k * 1.7)))
        poly = [(0, yb + 22 * S)] + pts + [(W, yb + 22 * S)]
        for ox in (0, W, 2 * W):
            d.polygon([(px + ox, py) for px, py in poly], fill=(132, 172, 180, 34))
            d.line([(px + ox, py) for px, py in pts], fill=(150, 190, 196, 60), width=2 * S)
    # grass blades above the strip + tiny lime tufts
    for _ in range(260):
        px = rng.uniform(0, W)
        hh = rng.uniform(5, 13) * S
        lean = rng.uniform(-4, 4) * S
        c = (236, 250, 70, 255) if rng.random() < 0.5 else (200, 226, 20, 255)
        s0 = sy(px)
        wrap(lambda X, s0=s0, hh=hh, lean=lean, c=c: d.polygon([(X - 3 * S, s0 + 2 * S), (X + lean, s0 - hh), (X + 3 * S, s0 + 2 * S)], fill=c), px)
    # scalloped dark-lime edge under the strip
    for i in range(0, W, 9 * S):
        px = i + rng.uniform(-2, 2) * S
        s0 = sy(px)
        wrap(lambda X, s0=s0: d.polygon([(X - 5 * S, s0 + 3 * S), (X, s0 + 11 * S), (X + 5 * S, s0 + 3 * S)], fill=(134, 170, 10, 255)), px)
    # jungle leaves at the bottom (two layers: dark back, mid front with veins)
    def leaf(cx, cy, length, width, ang, col, vein):
        a = math.radians(ang)
        pts = []
        for i in range(0, 21):
            u = i / 20
            w = math.sin(u * math.pi) ** 0.8 * width
            pts.append((u * length, w))
        for i in range(20, -1, -1):
            u = i / 20
            w = math.sin(u * math.pi) ** 0.8 * width
            pts.append((u * length, -w))
        rot = [(cx + px * math.cos(a) - py * math.sin(a), cy + px * math.sin(a) + py * math.cos(a)) for px, py in pts]
        for ox in (0, W, 2 * W):
            d.polygon([(px + ox, py) for px, py in rot], fill=col)
            d.line([(cx + ox, cy), (cx + ox + length * math.cos(a), cy + length * math.sin(a))], fill=vein, width=2 * S)

    for layer, (n, cols, vein) in enumerate([(70, [(20, 62, 70, 255), (26, 78, 82, 255)], (46, 104, 100, 255)),
                                           (58, [(32, 92, 90, 255), (44, 112, 104, 255)], (74, 142, 124, 255))]):
        for _ in range(n):
            cx = rng.uniform(0, W)
            cy = rng.uniform(H * (0.62 if layer == 0 else 0.74), H + 10 * S)
            leaf(cx, cy, rng.uniform(70, 150) * S, rng.uniform(14, 26) * S, rng.uniform(-125, -55),
                 tuple(cols[int(rng.integers(0, 2))]), vein)
    out = big.crop((W, 0, 2 * W, H)).convert("RGBa").resize((1024, 512), Image.LANCZOS).convert("RGBA")
    return out


def _noise_wh(W, H, rng):
    n = rng.normal(0, 1, (H // 8, W // 8)).astype(np.float32)
    n = np.array(Image.fromarray(n).resize((W, H), Image.BICUBIC))
    return n / 3.0


# --------------------------------------------------------------------------- background (soft pastel)
def make_background():
    W, H = 2048, 1024
    rng = np.random.default_rng(3)
    ys = np.linspace(0, 1, H, dtype=np.float32)
    stops = [(0.0, (140, 204, 222)), (0.45, (176, 224, 230)), (0.78, (206, 236, 226)), (1.0, (214, 232, 168))]
    col = np.stack([np.interp(ys, [p for p, _ in stops], [c[k] for _, c in stops]) for k in range(3)], axis=1)
    img = Image.fromarray(clamp8(np.broadcast_to(col[:, None, :], (H, W, 3)).copy()), "RGB").convert("RGBA")
    xs = np.arange(W, dtype=np.float32)

    def soft(layer, blur):
        return layer.filter(ImageFilter.GaussianBlur(blur)) if blur else layer

    def ridge(base, amps, phases, freqs):
        r = np.full(W, base, np.float32)
        for am, ph, fr in zip(amps, phases, freqs):
            r += am * np.sin(2 * np.pi * fr * xs / W + ph)
        return r

    def fill_ridge(ridge_y, top_color, bottom_color):
        m = Image.new("L", (W, H), 0)
        ImageDraw.Draw(m).polygon([(0, H)] + [(int(x), float(ridge_y[int(x)])) for x in range(0, W, 4)] + [(W, H)], fill=255)
        ymin = float(ridge_y.min())
        t = np.clip((np.arange(H, dtype=np.float32)[:, None] - ymin) / (H - ymin), 0, 1)
        c = np.array(top_color, np.float32) * (1 - t[..., None]) + np.array(bottom_color, np.float32) * t[..., None]
        layer = Image.fromarray(clamp8(np.broadcast_to(c, (H, W, 3)).copy()), "RGB").convert("RGBA")
        layer.putalpha(m)
        return layer

    # distant snowy peaks (right)
    d = ImageDraw.Draw(img, "RGBA")
    peaks = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    pd = ImageDraw.Draw(peaks, "RGBA")
    for px, h, w in ((1560, 330, 190), (1730, 420, 210), (1880, 300, 170), (1960, 360, 160)):
        base_y = 700
        pd.polygon([(px - w, base_y), (px, base_y - h), (px + w, base_y)], fill=(160, 196, 206, 235))
        pd.polygon([(px, base_y - h), (px - w * 0.55, base_y - h + h * 0.55), (px - w * 0.15, base_y - h + h * 0.42),
                    (px + w * 0.1, base_y - h + h * 0.6), (px + w * 0.5, base_y - h + h * 0.5)], fill=(240, 248, 250, 240))
    img = Image.alpha_composite(img, soft(peaks, 2.0))

    def mesa(cx, width, base_y, tiers, color, vine=False):
        layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        md = ImageDraw.Draw(layer, "RGBA")
        y = base_y
        w = width
        for i, (th, dw, shift) in enumerate(tiers):
            x0, x1 = cx - w / 2 + shift, cx + w / 2 + shift
            tone = tuple(int(v * (1.0 - 0.06 * (i % 2)) + (8 if i % 2 else 0)) for v in color)
            md.rounded_rectangle([x0, y - th, x1, y], radius=14, fill=tone + (255,))
            md.rounded_rectangle([x0, y - th, x1, y - th + 7], radius=7, fill=(188, 222, 226, 255))
            md.line([(x0 + 8, y - th * 0.45), (x1 - 8, y - th * 0.5)], fill=(116, 168, 176, 140), width=3)
            y -= th - 6
            w += dw
        top_y = y + 6
        cap = [(cx - w / 2 - 6, top_y + 4)] + [(cx - w / 2 + k * (w + 12) / 14, top_y - 10 - 7 * math.sin(k * 1.9 + cx)) for k in range(15)]
        cap += [(cx + w / 2 + 6, top_y + 14)]
        md.polygon(cap + [(cx + w / 2 + 6, top_y + 24), (cx - w / 2 - 6, top_y + 24)], fill=(196, 226, 112, 255))
        if vine:
            pts = [(cx - w / 2 + 6 + t * (w * 0.9), top_y + 40 + 150 * (t ** 1.6) * 1.0 - 60 * math.sin(t * 3.1)) for t in np.linspace(0, 1, 30)]
            md.line(pts, fill=(196, 226, 112, 255), width=16, joint="curve")
        return layer

    far = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    far = Image.alpha_composite(far, mesa(690, 70, 780, [(250, 14, 0), (170, 18, 6), (160, 10, -4), (120, 8, 4)], (150, 198, 204)))
    far = Image.alpha_composite(far, mesa(1560, 150, 780, [(190, 40, 0), (170, 30, 10), (150, 30, -8), (120, 20, 6)], (146, 194, 202)))
    far = Image.alpha_composite(far, mesa(1850, 170, 780, [(300, 30, 0), (190, 24, 8), (170, 20, -6), (140, 14, 4)], (150, 198, 206)))
    img = Image.alpha_composite(img, soft(far, 1.4))
    img = Image.alpha_composite(img, soft(mesa(210, 400, 820, [(230, 30, 0), (180, 24, 12), (190, 20, -10), (170, 16, 8), (130, 10, 0)],
                                                (138, 190, 198), vine=True), 0.8))
    # hazy sea at the foot of the mesas
    sea = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(sea).rectangle([560, 700, 960, 760], fill=(120, 206, 206, 190))
    img = Image.alpha_composite(img, soft(sea, 3))

    # rolling hills (back to front), pale yellow-green like a soft focus painting
    h1 = ridge(690, (46, 26, 14), (0.6, 2.1, 3.3), (1.1, 2.6, 5.8))
    img = Image.alpha_composite(img, soft(fill_ridge(h1, (196, 224, 130), (186, 218, 120)), 1.2))
    h2 = ridge(760, (40, 22, 12), (2.2, 0.4, 4.4), (1.4, 3.2, 6.6))
    img = Image.alpha_composite(img, soft(fill_ridge(h2, (210, 232, 132), (192, 222, 116)), 0.8))
    h3 = ridge(830, (34, 18, 8), (1.0, 3.3, 5.0), (1.2, 2.9, 6.1))
    img = Image.alpha_composite(img, fill_ridge(h3, (208, 230, 120), (190, 220, 104)))
    d = ImageDraw.Draw(img, "RGBA")
    for _ in range(26):                          # soft darker bushes on the hills
        bx = int(rng.uniform(0, W))
        by = float(h2[bx]) + rng.uniform(10, 60)
        s = rng.uniform(0.7, 1.5)
        for ox, oy, r in ((-26, 0, 22), (0, -12, 28), (26, 0, 22), (-8, 6, 20)):
            d.ellipse([bx + ox * s - r * s, by + oy * s - r * s, bx + ox * s + r * s, by + oy * s + r * s], fill=(168, 204, 100, 150))
    for _ in range(120):                         # short grass strokes
        gx = rng.uniform(0, W)
        gy = float(h3[int(gx)]) + rng.uniform(20, 170)
        d.line([(gx, gy), (gx + rng.uniform(-6, 6), gy - rng.uniform(8, 18))], fill=(168, 204, 84, 140), width=3)
    # soft light from the top left and a gentle vignette
    arr = np.array(img.convert("RGB")).astype(np.float32)
    xx = (np.arange(W, dtype=np.float32)[None, :] - W / 2) / (W / 2)
    yy = (np.arange(H, dtype=np.float32)[:, None] - H / 2) / (H / 2)
    arr *= (1 - 0.07 * np.clip(xx ** 2 + yy ** 2 - 0.4, 0, 1))[..., None]
    return Image.fromarray(clamp8(arr), "RGB")


# --------------------------------------------------------------------------- level terrain (cliff and mound)
def make_terrain():
    """Dark teal rock of level 1 at 64 px per meter. Polygons come from tools/generate_level.py."""
    import generate_level as gl
    px_per_m = 64
    W, H = int(13.6 * px_per_m), int(7.05 * px_per_m)

    def T(px, py):
        return ((px / 100 - 9.8) * px_per_m, (py / 100 - 1.8) * px_per_m)

    a = Art(W, H, 2)
    rng = random.Random(4)
    masks = []
    for k, poly in enumerate(gl.terrain):
        pts = [T(x, y) for x, y in poly]
        m = a.poly(pts)
        masks.append(m)
        mound = k == 0
        top = (92, 142, 148) if mound else (74, 134, 140)
        bot = (70, 118, 126) if mound else (48, 100, 110)
        ymin = min(p[1] for p in pts)
        ymax = max(p[1] for p in pts)
        a.paint(m, a.linear((0, ymin), (0, ymax), top, bot))
        # wavy horizontal strata
        if not mound:
            for j in range(int(ymin) // 22 - 1, int(ymax) // 22 + 1):
                yb = j * 22 + 8
                wave = [(x, yb + 3 * math.sin(x / 26 + j * 1.3)) for x in range(0, W + 1, 6)]
                band = [(0, yb + 10)] + wave + [(W, yb + 10)]
                a.paint(a.poly(band), (150, 200, 206, 255), clip=m, opacity=0.16)
                a.paint(a.stroke(wave, 1.6), (30, 80, 90, 255), clip=m, opacity=0.18)
    # dark recesses (caves) with a light rim on the top-left
    for (x0, y0, x1, y1) in ((1936, 658, 2026, 758), (1765, 791, 1854, 885)):
        (ax, ay), (bx, by) = T(x0, y0), T(x1, y1)
        r = a.poly([(ax, ay), (bx, ay), (bx, by), (ax, by)])
        clip = ImageChops.lighter(masks[2], masks[1])
        a.paint(r, (36, 66, 76), clip=clip)
        a.paint(a.stroke([(ax, by), (ax, ay), (bx, ay)], 2.2), (86, 140, 150), clip=clip, opacity=0.6)
    img = a.finish(W, H)
    d = ImageDraw.Draw(img, "RGBA")

    def tuft(cx, cy, spread, count=34):
        for _ in range(count):
            x = cx + rng.uniform(-spread, spread)
            hh = rng.uniform(8, 20)
            lean = rng.uniform(-9, 9)
            c = (88, 182, 54, 255) if rng.random() < 0.55 else (52, 134, 44, 255)
            d.polygon([(x - 4, cy + 7), (x + lean, cy - hh), (x + 4, cy + 7)], fill=c)
        for _ in range(8):
            x = cx + rng.uniform(-spread, spread)
            d.polygon([(x - 6, cy + 4), (x, cy + 18), (x + 6, cy + 4)], fill=(46, 120, 44, 255))

    def grass_line(px0, px1, py):
        x0, y0 = T(px0, py)
        x1, _ = T(px1, py)
        n = max(2, int((x1 - x0) / 34))
        for i in range(n + 1):
            tuft(x0 + (x1 - x0) * i / n, y0 + 1, 24, 7)

    grass_line(1040, 1190, 818)       # mound
    grass_line(1758, 1930, 758)       # ledge
    grass_line(1938, 2062, 525)       # terrace
    grass_line(1938, 2040, 758)       # floor of the cave
    for (gx0, gx1, gy) in ((2080, 2165, 590), (2170, 2255, 665)):   # floating clumps on the cliff face
        grass_line(gx0, gx1, gy)
    # blue gems
    for gx, gy in ((1985, 742), (1810, 865)):
        cx, cy = T(gx, gy)
        pts = [(cx - 15, cy - 4), (cx - 8, cy - 12), (cx + 8, cy - 12), (cx + 15, cy - 4), (cx, cy + 14)]
        d.polygon(pts, fill=(176, 232, 252, 255), outline=(50, 120, 160, 255))
        d.polygon([(cx - 8, cy - 12), (cx + 8, cy - 12), (cx, cy - 3)], fill=(236, 250, 255, 255))
        d.polygon([(cx - 15, cy - 4), (cx - 8, cy - 12), (cx, cy - 3)], fill=(150, 214, 244, 255))
        d.polygon([(cx + 15, cy - 4), (cx + 8, cy - 12), (cx, cy - 3)], fill=(120, 196, 236, 255))
    # flower bush at the foot of the rock
    bx, by = T(2200, 862)
    for ox, oy, r in ((-26, 4, 20), (0, -6, 26), (26, 4, 20), (-10, 10, 18), (14, 10, 18)):
        d.ellipse([bx + ox - r, by + oy - r, bx + ox + r, by + oy + r], fill=(44, 124, 62, 255), outline=(26, 84, 46, 255))
    for ox, oy in ((-24, -6), (6, -22), (28, -4), (-6, 4), (16, -10)):
        for k in range(5):
            ang = k * 1.2566
            d.ellipse([bx + ox + 7 * math.cos(ang) - 5, by + oy + 7 * math.sin(ang) - 5,
                       bx + ox + 7 * math.cos(ang) + 5, by + oy + 7 * math.sin(ang) + 5], fill=(255, 226, 70, 255))
        d.ellipse([bx + ox - 3, by + oy - 3, bx + ox + 3, by + oy + 3], fill=(250, 140, 30, 255))
    return img


# --------------------------------------------------------------------------- bench the slingshot stands on
def make_bench():
    a = Art(512, 128, 3)
    wood = a.linear((0, 0), (0, 128), (214, 150, 86), (150, 92, 44))
    for lx in (34, 430):                                           # legs
        a.paint(a.poly([(lx, 40), (lx + 52, 40), (lx + 44, 126), (lx + 8, 126)]), wood, outline=(70, 38, 16), ow=4)
    a.paint(a.poly([(70, 66), (76, 56), (440, 56), (446, 66), (446, 78), (70, 78)]), (170, 108, 56), outline=(70, 38, 16), ow=3)
    a.paint(a.rrect(6, 14, 506, 48, 8), a.linear((0, 14), (0, 48), (222, 160, 96), (176, 112, 58)), outline=(70, 38, 16), ow=4)   # top plank
    a.paint(a.rrect(12, 38, 500, 58, 4), (146, 90, 42), outline=(70, 38, 16), ow=3)                                              # apron
    a.paint(a.rrect(18, 18, 494, 24, 3), (255, 226, 180), opacity=0.45)
    for x in range(60, 480, 90):
        a.paint(a.stroke([(x, 30), (x + 36, 31)], 2), (130, 80, 38), opacity=0.5)
    return a.finish(512, 128)


# --------------------------------------------------------------------------- stars, rays, sparkles
def _star_points(cx, cy, ro, ri, rot=-90):
    return [(cx + (ro if i % 2 == 0 else ri) * math.cos(math.radians(rot + i * 36)),
             cy + (ro if i % 2 == 0 else ri) * math.sin(math.radians(rot + i * 36))) for i in range(10)]


def make_star(on):
    """256x256 star with rounded points, beveled facets, an outline and a glossy highlight."""
    a = Art(256, 256, 3)
    cx, cy = 128, 136
    outer = a.rounded(a.poly(_star_points(cx, cy, 118, 52)), 7)
    if on:
        a.paint(outer, a.linear((0, 20), (0, 250), (255, 232, 96), (255, 150, 16)), outline=(176, 84, 0), ow=9)
        inner = a.rounded(a.poly(_star_points(cx, cy + 2, 92, 40)), 6)
        a.paint(inner, a.linear((0, 40), (0, 230), (255, 244, 150), (255, 184, 30)))
        # facets: alternate light / dark triangles from the center to each tip
        for i in range(5):
            a0 = math.radians(-90 + i * 72)
            tip = (cx + 92 * math.cos(a0), cy + 2 + 92 * math.sin(a0))
            l = (cx + 40 * math.cos(a0 - math.radians(36)), cy + 2 + 40 * math.sin(a0 - math.radians(36)))
            r = (cx + 40 * math.cos(a0 + math.radians(36)), cy + 2 + 40 * math.sin(a0 + math.radians(36)))
            a.paint(a.poly([(cx, cy + 2), tip, l]), (255, 255, 255), clip=inner, opacity=0.30)
            a.paint(a.poly([(cx, cy + 2), tip, r]), (220, 110, 0), clip=inner, opacity=0.22)
        a.paint(a.ellipse(100, 74, 42, 14, -52), (255, 255, 255), clip=outer, opacity=0.65)
        a.paint(a.circle(168, 190, 7), (255, 255, 255), clip=outer, opacity=0.5)
    else:
        a.paint(outer, a.linear((0, 20), (0, 250), (96, 112, 138), (52, 62, 86)), outline=(26, 32, 48), ow=9)
        inner = a.rounded(a.poly(_star_points(cx, cy + 2, 92, 40)), 6)
        a.paint(inner, a.linear((0, 40), (0, 230), (76, 90, 114), (44, 54, 76)))
        a.paint(a.ellipse(100, 74, 42, 14, -52), (255, 255, 255), clip=outer, opacity=0.18)
    return a.finish(256, 256)


def make_star_glow():
    """Rays for behind an earned star (rotated by the game), 256x256."""
    a = Art(256, 256, 2)
    for i in range(12):
        ang = math.radians(i * 30)
        w = math.radians(6.5)
        pts = [(128, 128), (128 + 128 * math.cos(ang - w), 128 + 128 * math.sin(ang - w)),
               (128 + 128 * math.cos(ang + w), 128 + 128 * math.sin(ang + w))]
        a.paint(a.poly(pts), a.radial(128, 128, 128, (255, 246, 170, 220), (255, 230, 120, 0), 1.1))
    a.paint(a.circle(128, 128, 70), a.radial(128, 128, 70, (255, 244, 160, 170), (255, 220, 100, 0), 1.0))
    return a.finish(256, 256)


def make_sparkle():
    a = Art(64, 64, 4)
    pts = []
    for i in range(8):
        r = 30 if i % 2 == 0 else 6
        ang = math.radians(-90 + i * 45)
        pts.append((32 + r * math.cos(ang), 32 + r * math.sin(ang)))
    a.paint(a.poly(pts), (255, 255, 255), outline=(255, 232, 120), ow=1.5)
    return a.finish(64, 64)


# --------------------------------------------------------------------------- UI
def make_panel():
    a = Art(512, 384, 3)
    a.paint(a.rrect(2, 2, 510, 382, 42), (24, 66, 140))                                   # dark blue outer rim
    a.paint(a.rrect(8, 8, 504, 376, 36), (255, 255, 255))                                 # white rim
    a.paint(a.rrect(14, 14, 498, 370, 31), a.linear((0, 14), (0, 370), (128, 208, 244), (62, 138, 214)))
    a.paint(a.rrect(24, 22, 488, 120, 24), (255, 255, 255), opacity=0.16)                 # soft top shine
    for x in range(40, 480, 56):                                                          # subtle diagonal pattern
        a.paint(a.stroke([(x, 370), (x + 90, 20)], 8), (255, 255, 255), clip=a.rrect(14, 14, 498, 370, 31), opacity=0.05)
    return a.finish(512, 384)


def make_button(top, bottom, edge):
    a = Art(512, 192, 3)
    a.paint(a.rrect(4, 10, 508, 188, 52), tuple(int(v * 0.55) for v in edge))             # 3D base
    a.paint(a.rrect(4, 4, 508, 176, 52), (255, 255, 255))                                 # white rim
    shape = a.rrect(12, 12, 500, 168, 44)
    a.paint(shape, a.linear((0, 12), (0, 168), top, bottom), outline=edge, ow=3)
    a.paint(a.rrect(26, 20, 486, 82, 32), (255, 255, 255), clip=shape, opacity=0.28)
    return a.finish(512, 192)


def make_pause_icon():
    a = Art(128, 128, 4)
    a.paint(a.circle(64, 68, 56), (20, 54, 120))                                          # 3D base
    a.paint(a.circle(64, 62, 56), (255, 255, 255))
    disc = a.circle(64, 62, 49)
    a.paint(disc, a.linear((0, 14), (0, 110), (126, 196, 252), (52, 118, 218)), outline=(36, 90, 190), ow=2.5)
    a.paint(a.ellipse(64, 36, 38, 16), (255, 255, 255), clip=disc, opacity=0.35)
    a.paint(a.rrect(40, 36, 56, 88, 5), (255, 255, 255), outline=(24, 70, 150), ow=3)
    a.paint(a.rrect(72, 36, 88, 88, 5), (255, 255, 255), outline=(24, 70, 150), ow=3)
    return a.finish(128, 128)


def make_pig_icon():
    a = Art(128, 128, 4)
    a.paint(a.circle(64, 64, 60), (255, 255, 255))
    a.paint(a.circle(64, 64, 54), a.radial(50, 46, 90, (150, 224, 84), (86, 164, 36), 1.2), outline=(34, 96, 16), ow=3)
    for ex in (46, 82):
        a.paint(a.circle(ex, 52, 12), (255, 255, 255), outline=(30, 60, 10), ow=2)
        a.paint(a.circle(ex + (2 if ex < 64 else -2), 55, 5), (14, 14, 10))
    a.paint(a.ellipse(64, 80, 24, 17), (180, 236, 110), outline=(44, 100, 14), ow=2.5)
    a.paint(a.ellipse(55, 80, 3.5, 6), (36, 82, 12))
    a.paint(a.ellipse(73, 80, 3.5, 6), (36, 82, 12))
    for sx in (-1, 1):
        a.paint(a.rounded(a.poly([(64 + sx * 36, 20), (64 + sx * 52, 8), (64 + sx * 54, 30)]), 4), (96, 176, 44), outline=(34, 96, 16), ow=2)
    return a.finish(128, 128)


def make_node(open_):
    a = Art(256, 256, 3)
    if open_:
        a.paint(a.circle(128, 144, 106), (24, 78, 150))                                   # 3D base
        a.paint(a.circle(128, 126, 106), (255, 255, 255))
        disc = a.circle(128, 126, 94)
        a.paint(disc, a.linear((0, 30), (0, 220), (130, 214, 255), (46, 126, 224)), outline=(30, 94, 196), ow=3)
        a.paint(a.ellipse(128, 70, 68, 28), (255, 255, 255), clip=disc, opacity=0.34)
    else:
        a.paint(a.circle(128, 144, 106), (84, 94, 112))
        a.paint(a.circle(128, 126, 106), (230, 234, 242))
        disc = a.circle(128, 126, 94)
        a.paint(disc, a.linear((0, 30), (0, 220), (176, 186, 204), (120, 132, 154)), outline=(96, 108, 130), ow=3)
        a.paint(a.ellipse(128, 70, 68, 28), (255, 255, 255), clip=disc, opacity=0.25)
    return a.finish(256, 256)
