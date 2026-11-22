"""
Professional UI art: round icon buttons and glyph icons, a dark glass panel, the tower-style level list
(sky with a fairground wheel, themed floors, doors, number tags) and the ability badges shown on flight trails.
All artwork is original and procedurally drawn. Imported by generate_art.py.
"""
import math
import random

import numpy as np
from PIL import Image, ImageChops, ImageDraw, ImageFilter

from generate_art import Art, clamp8  # noqa: E402


# --------------------------------------------------------------------------- round buttons and icons
def make_btn_round(top, bottom, edge):
    """A round button without a symbol (the symbol is a separate icon texture): soft shadow, white rim, gradient, gloss."""
    a = Art(256, 256, 3)
    a.paint(a.circle(128, 142, 116), (0, 0, 0), opacity=0.35)                              # drop shadow
    a.paint(a.circle(128, 134, 116), tuple(int(v * 0.5) for v in edge))                    # thickness
    a.paint(a.circle(128, 126, 116), (255, 255, 255))                                      # white rim
    disc = a.circle(128, 126, 104)
    a.paint(disc, a.linear((0, 24), (0, 228), top, bottom), outline=edge, ow=2.5)
    a.paint(a.ellipse(128, 70, 80, 34), (255, 255, 255), clip=disc, opacity=0.30)          # gloss
    return a.finish(256, 256)


def _glyph(draw_fn, size=128, S=4):
    """Draws a white glyph with a soft dark shadow. draw_fn(a, color) paints onto an Art in a 128x128 design space."""
    a = Art(size, size, S)
    draw_fn(a, (0, 0, 0, 255), 3, 0.28)       # shadow pass (offset 3 px down)
    draw_fn(a, (255, 255, 255, 255), 0, 1.0)  # white pass
    return a.finish(size, size)


def make_icon_play():
    def fn(a, col, dy, op):
        tri = a.rounded(a.poly([(40, 26 + dy), (100, 64 + dy), (40, 102 + dy)]), 7)
        a.paint(tri, col, opacity=op)
    return _glyph(fn)


def make_icon_pause():
    def fn(a, col, dy, op):
        a.paint(a.rrect(36, 28 + dy, 56, 100 + dy, 6), col, opacity=op)
        a.paint(a.rrect(72, 28 + dy, 92, 100 + dy, 6), col, opacity=op)
    return _glyph(fn)


def make_icon_restart():
    def fn(a, col, dy, op):
        cx, cy, r = 64, 66 + dy, 34
        pts = [(cx + r * math.cos(math.radians(a0)), cy + r * math.sin(math.radians(a0))) for a0 in range(-60, 241, 6)]
        a.paint(a.stroke(pts, 15), col, opacity=op)
        # arrow head at the start of the arc (pointing clockwise)
        ex, ey = cx + r * math.cos(math.radians(-60)), cy + r * math.sin(math.radians(-60))
        a.paint(a.rounded(a.poly([(ex - 6, ey - 22), (ex + 28, ey + 4), (ex - 14, ey + 12)]), 4), col, opacity=op)
    return _glyph(fn)


def make_icon_home():
    def fn(a, col, dy, op):
        a.paint(a.rounded(a.poly([(64, 24 + dy), (104, 60 + dy), (24, 60 + dy)]), 5), col, opacity=op)
        a.paint(a.rrect(34, 58 + dy, 94, 102 + dy, 5), col, opacity=op)
        a.paint(a.rrect(79, 30 + dy, 90, 52 + dy, 2), col, opacity=op)                   # chimney
        if col[0] == 255:
            a.paint(a.rrect(54, 72 + dy, 74, 102 + dy, 4), (60, 110, 190), opacity=1.0)  # door (cut-out look)
    return _glyph(fn)


def make_icon_back():
    def fn(a, col, dy, op):
        head = a.rounded(a.poly([(22, 64 + dy), (64, 24 + dy), (64, 104 + dy)]), 6)
        a.paint(head, col, opacity=op)
        a.paint(a.rrect(52, 46 + dy, 104, 82 + dy, 6), col, opacity=op)
    return _glyph(fn)


def make_icon_gear():
    def fn(a, col, dy, op):
        cx, cy = 64, 64 + dy
        for k in range(8):
            ang = math.radians(k * 45)
            c, s = math.cos(ang), math.sin(ang)
            nx, ny = -s, c
            r0, r1, hw0, hw1 = 30, 49, 11, 8
            pts = [(cx + c * r0 + nx * hw0, cy + s * r0 + ny * hw0), (cx + c * r1 + nx * hw1, cy + s * r1 + ny * hw1),
                   (cx + c * r1 - nx * hw1, cy + s * r1 - ny * hw1), (cx + c * r0 - nx * hw0, cy + s * r0 - ny * hw0)]
            a.paint(a.rounded(a.poly(pts), 2), col, opacity=op)
        a.paint(a.circle(cx, cy, 38), col, opacity=op)
        if col[0] == 255:
            a.paint(a.circle(cx, cy, 15), (60, 110, 190), opacity=1.0)                   # hub hole (cut-out look)
    return _glyph(fn)


def make_icon_music():
    def fn(a, col, dy, op):
        a.paint(a.ellipse(44, 92 + dy, 19, 14, -18), col, opacity=op)                    # note heads
        a.paint(a.ellipse(86, 82 + dy, 19, 14, -18), col, opacity=op)
        a.paint(a.rrect(55, 28 + dy, 65, 90 + dy, 4), col, opacity=op)                  # stems
        a.paint(a.rrect(97, 18 + dy, 107, 80 + dy, 4), col, opacity=op)
        a.paint(a.poly([(55, 26 + dy), (107, 14 + dy), (107, 40 + dy), (55, 52 + dy)]), col, opacity=op)  # beam
    return _glyph(fn)


def make_icon_sound():
    def fn(a, col, dy, op):
        body = a.poly([(18, 50 + dy), (40, 50 + dy), (66, 26 + dy), (66, 102 + dy), (40, 78 + dy), (18, 78 + dy)])
        a.paint(a.rounded(body, 3), col, opacity=op)
        for r, w in ((20, 8), (36, 8)):                                                  # sound waves
            pts = [(66 + r * math.cos(math.radians(t)) * 0.9, 64 + dy + r * math.sin(math.radians(t)) * 1.25)
                   for t in range(-48, 49, 6)]
            a.paint(a.stroke(pts, w), col, opacity=op)
    return _glyph(fn)


def make_icon_close():
    def fn(a, col, dy, op):
        a.paint(a.stroke([(34, 34 + dy), (94, 94 + dy)], 17), col, opacity=op)
        a.paint(a.stroke([(94, 34 + dy), (34, 94 + dy)], 17), col, opacity=op)
    return _glyph(fn)


def make_icon_slash():
    """A red diagonal bar with a white rim, laid over a muted icon."""
    a = Art(128, 128, 4)
    a.paint(a.stroke([(24, 26), (104, 106)], 16), (0, 0, 0), opacity=0.30)
    a.paint(a.stroke([(24, 22), (104, 102)], 19), (255, 255, 255))
    a.paint(a.stroke([(24, 22), (104, 102)], 11), (232, 52, 48))
    return a.finish(128, 128)


def make_btn_pill():
    """Wide, modern pill button (used for 'Continue campaign')."""
    a = Art(512, 160, 3)
    a.paint(a.rrect(6, 14, 506, 156, 70), (0, 0, 0), opacity=0.35)
    a.paint(a.rrect(6, 8, 506, 150, 70), (22, 86, 40))
    a.paint(a.rrect(6, 4, 506, 144, 70), (255, 255, 255))
    body = a.rrect(12, 10, 500, 138, 64)
    a.paint(body, a.linear((0, 10), (0, 138), (112, 214, 96), (40, 150, 60)), outline=(24, 104, 44), ow=2.5)
    a.paint(a.rrect(30, 16, 482, 66, 40), (255, 255, 255), clip=body, opacity=0.25)
    return a.finish(512, 160)


def make_panel():
    """Dark glass panel with a thin light border (pause and result screens)."""
    a = Art(512, 384, 3)
    a.paint(a.rrect(2, 2, 510, 382, 40), (130, 176, 236))                                  # light border
    inner = a.rrect(6, 6, 506, 378, 36)
    a.paint(inner, a.linear((0, 6), (0, 378), (44, 62, 104), (22, 32, 66)))
    a.paint(a.rrect(14, 12, 498, 110, 30), (255, 255, 255), clip=inner, opacity=0.07)     # soft top highlight
    a.paint(a.rrect(6, 330, 506, 378, 36), (0, 0, 0), clip=inner, opacity=0.18)           # bottom shade
    return a.finish(512, 384)


# --------------------------------------------------------------------------- ability badges for the flight trail
def make_mark_dash():
    a = Art(128, 128, 4)
    a.paint(a.circle(64, 64, 58), (255, 255, 255))
    disc = a.circle(64, 64, 52)
    a.paint(disc, a.linear((0, 12), (0, 116), (255, 224, 80), (240, 150, 20)), outline=(150, 84, 0), ow=2.5)
    for y, l in ((44, 26), (64, 36), (84, 26)):                                           # speed lines
        a.paint(a.stroke([(16, y), (16 + l, y)], 5), (255, 255, 255), clip=disc, opacity=0.8)
    bolt = a.poly([(76, 14), (44, 66), (62, 66), (50, 114), (88, 54), (68, 54)])
    a.paint(bolt, (255, 255, 255), outline=(150, 84, 0), ow=3)
    return a.finish(128, 128)


def make_mark_split():
    a = Art(128, 128, 4)
    a.paint(a.circle(64, 64, 58), (255, 255, 255))
    disc = a.circle(64, 64, 52)
    a.paint(disc, a.linear((0, 12), (0, 116), (120, 206, 255), (30, 108, 214)), outline=(12, 56, 140), ow=2.5)
    # one stem that splits into three arrows
    a.paint(a.stroke([(20, 64), (50, 64)], 9), (255, 255, 255))
    for ang in (-38, 0, 38):
        r = math.radians(ang)
        ex, ey = 50 + 56 * math.cos(r), 64 + 56 * math.sin(r)
        a.paint(a.stroke([(50, 64), (ex - 10 * math.cos(r), ey - 10 * math.sin(r))], 8), (255, 255, 255))
        hx, hy = math.cos(r), math.sin(r)
        nx, ny = -hy, hx
        a.paint(a.rounded(a.poly([(ex + hx * 9, ey + hy * 9), (ex - hx * 8 + nx * 11, ey - hy * 8 + ny * 11),
                                  (ex - hx * 8 - nx * 11, ey - hy * 8 - ny * 11)]), 2), (255, 255, 255))
    return a.finish(128, 128)


# --------------------------------------------------------------------------- tower level list
def make_tower_bg():
    """Sky with soft clouds and a pastel fairground wheel."""
    W, H = 2048, 1024
    rng = np.random.default_rng(9)
    ys = np.linspace(0, 1, H, dtype=np.float32)
    stops = [(0.0, (146, 214, 240)), (0.6, (186, 232, 244)), (1.0, (222, 244, 238))]
    col = np.stack([np.interp(ys, [p for p, _ in stops], [c[k] for _, c in stops]) for k in range(3)], axis=1)
    img = Image.fromarray(clamp8(np.broadcast_to(col[:, None, :], (H, W, 3)).copy()), "RGB").convert("RGBA")
    d = ImageDraw.Draw(img, "RGBA")
    # soft clouds
    cl = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    cd = ImageDraw.Draw(cl)
    for cx, cy, s in ((300, 170, 1.2), (900, 110, 0.9), (1500, 220, 1.1), (1850, 90, 0.8), (700, 330, 0.7)):
        for px, py, r in ((-90, 10, 50), (-30, -14, 66), (36, -22, 72), (96, 2, 58), (140, 20, 40)):
            cd.ellipse([cx + (px - r) * s, cy + (py - r) * s, cx + (px + r) * s, cy + (py + r) * s], fill=(255, 255, 255, 215))
        cd.rounded_rectangle([cx - 140 * s, cy + 6 * s, cx + 180 * s, cy + 62 * s], radius=int(28 * s), fill=(255, 255, 255, 215))
    img = Image.alpha_composite(img, cl.filter(ImageFilter.GaussianBlur(3)))
    # fairground wheel (pastel, low contrast)
    wheel = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    wd = ImageDraw.Draw(wheel)
    cx, cy, R = 560, 640, 400
    rim = (150, 188, 214, 200)
    wd.ellipse([cx - R, cy - R, cx + R, cy + R], outline=rim, width=14)
    wd.ellipse([cx - R + 46, cy - R + 46, cx + R - 46, cy + R - 46], outline=rim, width=8)
    for k in range(16):
        a0 = math.radians(k * 22.5)
        wd.line([(cx, cy), (cx + R * math.cos(a0), cy + R * math.sin(a0))], fill=rim, width=7)
        gx, gy = cx + R * math.cos(a0), cy + R * math.sin(a0)
        wd.rounded_rectangle([gx - 22, gy - 4, gx + 22, gy + 40], radius=8, fill=(170, 206, 228, 210))
    wd.ellipse([cx - 34, cy - 34, cx + 34, cy + 34], fill=(150, 188, 214, 230))
    wd.polygon([(cx - 26, cy + 30), (cx + 26, cy + 30), (cx + 120, H), (cx - 120, H)], fill=(160, 196, 220, 190))
    img = Image.alpha_composite(img, wheel.filter(ImageFilter.GaussianBlur(1.2)))
    # distant hills / tents along the bottom
    d = ImageDraw.Draw(img, "RGBA")
    for x0, w, h, c in ((80, 360, 120, (186, 220, 180, 255)), (520, 500, 90, (176, 214, 176, 255)), (1100, 600, 130, (190, 224, 182, 255)),
                        (1640, 520, 100, (178, 216, 178, 255))):
        d.ellipse([x0, H - h, x0 + w, H + h], fill=c)
    for tx, ty, col2 in ((1500, 900, (240, 150, 160, 230)), (1640, 930, (150, 170, 240, 230)), (260, 940, (250, 210, 120, 230))):
        d.polygon([(tx - 90, ty), (tx, ty - 80), (tx + 90, ty)], fill=col2)
        d.rectangle([tx - 80, ty, tx + 80, ty + 60], fill=tuple(min(255, v + 12) for v in col2[:3]) + (230,))
    # purple flowers bottom right
    for fx, fy, fr in ((1860, 960, 90), (1950, 900, 70), (1780, 1000, 60)):
        for k in range(6):
            ang = k * math.pi / 3
            d.ellipse([fx + fr * 0.6 * math.cos(ang) - fr * 0.5, fy + fr * 0.6 * math.sin(ang) - fr * 0.5,
                       fx + fr * 0.6 * math.cos(ang) + fr * 0.5, fy + fr * 0.6 * math.sin(ang) + fr * 0.5], fill=(170, 120, 220, 235))
        d.ellipse([fx - fr * 0.3, fy - fr * 0.3, fx + fr * 0.3, fy + fr * 0.3], fill=(250, 214, 110, 255))
    return img.convert("RGB")


def make_floor(theme):
    """One floor of the tower: dark stone frame around a themed room with curtains, a baseboard and decorations."""
    W, H = 1024, 320
    S = 2
    rng = random.Random(30 + theme)
    img = Image.new("RGBA", (W * S, H * S), (58, 52, 56, 255))
    d = ImageDraw.Draw(img, "RGBA")
    # brick texture on the frame
    for _ in range(420):
        x, y = rng.uniform(0, W), rng.uniform(0, H)
        w, h = rng.uniform(14, 40), rng.uniform(6, 12)
        shade = rng.randint(-8, 14)
        d.rectangle([x * S, y * S, (x + w) * S, (y + h) * S], fill=(66 + shade, 60 + shade, 64 + shade, 255))
    left, right, top, bottom = 24, W - 24, 26, H - 44
    # room wall pattern
    room = Image.new("RGBA", ((right - left) * S, (bottom - top) * S), (0, 0, 0, 255))
    rd = ImageDraw.Draw(room, "RGBA")
    rw, rh = right - left, bottom - top
    if theme in (0, 1, 3):
        base, light = {0: ((156, 150, 204), (176, 170, 224)), 1: ((244, 192, 112), (252, 216, 144)), 3: ((118, 80, 140), (140, 102, 164))}[theme]
        rd.rectangle([0, 0, rw * S, rh * S], fill=base + (255,))
        step = 74
        for gx in range(-step, rw + step, step):
            for gy in range(-step, rh + step, step):
                for ox, oy in ((0, 0), (step // 2, step // 2)):
                    cx, cy = gx + ox, gy + oy
                    rd.polygon([(cx * S, (cy - 32) * S), ((cx + 24) * S, cy * S), (cx * S, (cy + 32) * S), ((cx - 24) * S, cy * S)],
                               fill=light + (140,))
    else:
        for i in range(0, rw, 44):
            c = (232, 84, 92) if (i // 44) % 2 == 0 else (246, 216, 216)
            rd.rectangle([i * S, 0, (i + 44) * S, rh * S], fill=c + (255,))
    # soft shading near the top and bottom of the wall
    shade = Image.new("L", room.size, 0)
    sd = ImageDraw.Draw(shade)
    for k in range(40):
        sd.rectangle([0, k * S, room.width, (k + 1) * S], fill=int(90 * (1 - k / 40)))
    room.paste((0, 0, 0, 255), (0, 0), shade)
    img.paste(room, (left * S, top * S))
    d = ImageDraw.Draw(img, "RGBA")
    # bunting garland on the striped floor, posters on the others
    if theme == 2:
        pts = [(left + 20 + i * 8, top + 36 + 18 * math.sin(i / 100 * math.pi * 3)) for i in range(0, 960, 8)]
        for k in range(0, len(pts) - 6, 6):
            x, y = pts[k]
            c = [(250, 180, 60), (240, 90, 120), (90, 170, 240), (250, 240, 200)][(k // 6) % 4]
            d.polygon([(x * S, y * S), ((x + 26) * S, y * S), ((x + 13) * S, (y + 30) * S)], fill=c + (255,))
        d.line([(x * S, y * S) for x, y in pts], fill=(120, 80, 60, 255), width=3 * S)
    for px, py, pw, ph in ((rng.uniform(70, 150), rng.uniform(70, 110), 40, 54), (rng.uniform(840, 920), rng.uniform(80, 120), 36, 48)):
        d.rectangle([px * S, py * S, (px + pw) * S, (py + ph) * S], fill=(250, 214, 224, 220), outline=(200, 150, 170, 255), width=2 * S)
        d.rectangle([(px + 6) * S, (py + 8) * S, (px + pw - 6) * S, (py + 18) * S], fill=(236, 120, 150, 200))
    # curtains with stars on both sides
    for side in (0, 1):
        x0 = left if side == 0 else right
        sgn = 1 if side == 0 else -1
        poly = [(x0, top), (x0 + sgn * 70, top), (x0 + sgn * 46, top + 90), (x0 + sgn * 62, bottom - 80), (x0 + sgn * 24, bottom), (x0, bottom)]
        d.polygon([(px * S, py * S) for px, py in poly], fill=(28, 62, 150, 255))
        d.polygon([(px * S, py * S) for px, py in [(x0, top), (x0 + sgn * 34, top), (x0 + sgn * 18, top + 100), (x0 + sgn * 30, bottom - 70), (x0, bottom)]],
                  fill=(44, 90, 190, 255))
        for k in range(9):
            sx = x0 + sgn * rng.uniform(6, 34)
            sy = top + 20 + k * (bottom - top - 40) / 9
            r = rng.uniform(4, 7)
            d.polygon([(sx * S + r * S * (1 if j % 2 == 0 else 0.45) * math.cos(math.radians(-90 + j * 36)),
                        sy * S + r * S * (1 if j % 2 == 0 else 0.45) * math.sin(math.radians(-90 + j * 36))) for j in range(10)],
                      fill=(255, 236, 170, 235))
    # baseboard and the dark ledge below the room
    d.rectangle([left * S, (bottom - 12) * S, right * S, bottom * S], fill=(190, 40, 48, 255))
    d.rectangle([left * S, (bottom - 12) * S, right * S, (bottom - 10) * S], fill=(236, 120, 124, 255))
    d.rectangle([0, (bottom + 4) * S, W * S, (bottom + 8) * S], fill=(88, 80, 84, 255))
    # inner shadow of the frame
    d.rectangle([left * S, top * S, right * S, (top + 3) * S], fill=(0, 0, 0, 110))
    return img.convert("RGBa").resize((W, H), Image.LANCZOS).convert("RGBA")


def _arch_mask(a, cx, top, bottom, half):
    pts = [(cx - half, bottom)]
    for k in range(0, 181, 6):
        ang = math.radians(180 + k)
        pts.append((cx + half * math.cos(ang), top + half + half * math.sin(ang)))
    pts.append((cx + half, bottom))
    return a.poly(pts)


def _door(inner_top, inner_bot, glow=None):
    a = Art(256, 384, 3)
    if glow:
        a.paint(_arch_mask(a, 128, 4, 380, 124), glow, opacity=0.35)
    frame = _arch_mask(a, 128, 12, 378, 112)
    a.paint(frame, a.linear((0, 12), (0, 378), (240, 244, 250), (200, 208, 224)), outline=(96, 106, 130), ow=3)
    inner = _arch_mask(a, 128, 34, 378, 88)
    a.paint(inner, a.linear((0, 34), (0, 378), inner_top, inner_bot), outline=(10, 20, 60), ow=2.5)
    a.paint(a.stroke([(52, 150), (52, 372)], 3), (0, 0, 0), clip=inner, opacity=0.18)
    a.paint(_arch_mask(a, 128, 34, 378, 88), (255, 255, 255), clip=a.rrect(0, 0, 112, 384, 0), opacity=0.06)
    return a, inner


def make_door_open():
    a, inner = _door((38, 78, 180), (20, 44, 120))
    for x in (92, 128, 164):
        a.paint(a.stroke([(x, 120), (x, 372)], 2.5), (8, 20, 70), clip=inner, opacity=0.55)
    rng = random.Random(2)
    for _ in range(4):
        x, y = rng.uniform(70, 186), rng.uniform(150, 350)
        a.paint(a.stroke([(x, y), (x + rng.uniform(-18, 18), y + 26), (x + rng.uniform(-18, 18), y + 50)], 2), (8, 20, 70), clip=inner, opacity=0.6)
    return a.finish(256, 384)


def make_door_gold():
    a, inner = _door((255, 220, 80), (244, 160, 24), glow=(255, 226, 120))
    cx, cy = 128, 220
    for rad_i in range(6):
        pts = []
        for k in range(0, 360 * 2, 12):
            r = 12 + k / 12 * 3.4 + rad_i * 0
            pts.append((cx + r * math.cos(math.radians(k + rad_i * 60)), cy + r * 0.95 * math.sin(math.radians(k + rad_i * 60))))
        a.paint(a.stroke(pts[:int(len(pts) * 0.8)], 5), (255, 246, 190), clip=inner, opacity=0.35)
    a.paint(a.ellipse(100, 130, 36, 70, -15), (255, 255, 255), clip=inner, opacity=0.2)
    return a.finish(256, 384)


def make_door_locked():
    a, inner = _door((26, 40, 104), (14, 22, 66))
    # two crossed planks with nails
    for (y0, y1, ang) in ((190, 214, -14), (262, 288, 12)):
        cx, cy = 128, (y0 + y1) / 2
        plank = a.poly([(cx - 118, cy - 24 + ang * 1.2), (cx + 118, cy - 24 - ang * 1.2), (cx + 118, cy + 24 - ang * 1.2), (cx - 118, cy + 24 + ang * 1.2)])
        a.paint(plank, a.linear((0, cy - 24), (0, cy + 24), (152, 98, 54), (104, 62, 30)), outline=(56, 30, 12), ow=3, clip=_arch_mask(a, 128, 4, 380, 124))
        for nx in (cx - 84, cx + 84):
            a.paint(a.circle(nx, cy + (ang * 1.2 if nx < cx else -ang * 1.2), 5), (60, 56, 60))
    # padlock
    a.paint(a.stroke([(100, 232), (100, 206), (112, 190), (144, 190), (156, 206), (156, 232)], 14), (176, 182, 196), outline=(70, 76, 90), ow=3)
    a.paint(a.rrect(80, 224, 176, 300, 14), a.linear((0, 224), (0, 300), (196, 202, 214), (126, 132, 148)), outline=(60, 66, 80), ow=3)
    a.paint(a.circle(128, 254, 9), (40, 44, 56))
    a.paint(a.rrect(124, 254, 132, 278, 3), (40, 44, 56))
    return a.finish(256, 384)


def make_tag(top, bottom, edge):
    """Number banner above a door (256x160 canvas, number is drawn on it by the game)."""
    a = Art(192, 120, 3)
    shape = a.poly([(10, 14), (182, 14), (186, 70), (150, 108), (42, 108), (6, 70)])
    shape = a.rounded(shape, 8)
    a.paint(shape, a.linear((0, 14), (0, 108), top, bottom), outline=edge, ow=4)
    a.paint(a.rrect(18, 20, 174, 52, 12), (255, 255, 255), clip=shape, opacity=0.25)
    return a.finish(192, 120)
