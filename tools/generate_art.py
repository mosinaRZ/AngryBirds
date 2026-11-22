#!/usr/bin/env python3
"""
Generates every image in android/assets/ as original, procedurally drawn artwork
(cartoon red bird, green pig, wood/stone/glass tiles, slingshot, ground, background, UI).

Nothing here is copied from any commercial game. To use your own artwork, simply
overwrite the PNG files in android/assets/ with the same file names (see ASSETS.md).

Usage: python3 tools/generate_art.py [name ...]     (no argument = everything)
Requires: Pillow, numpy
"""
import math
import os
import random
import sys

import numpy as np
from PIL import Image, ImageChops, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "android", "assets")
os.makedirs(OUT, exist_ok=True)


# --------------------------------------------------------------------------- toolkit
class Art:
    """A supersampled RGBA canvas. All coordinates are in design units (before supersampling)."""

    def __init__(self, w, h, S=3, ox=0, oy=0):
        self.w, self.h, self.S = w, h, S
        self.ox, self.oy = ox, oy  # design-space origin shift (lets a sprite use a roomier canvas)
        self.W, self.H = w * S, h * S
        self.img = Image.new("RGBA", (self.W, self.H), (0, 0, 0, 0))
        self._grid = None

    # ---- grids
    def grid(self):
        if self._grid is None:
            xs = (np.arange(self.W, dtype=np.float32) + 0.5) / self.S - self.ox
            ys = (np.arange(self.H, dtype=np.float32) + 0.5) / self.S - self.oy
            self._grid = np.meshgrid(xs, ys)
        return self._grid

    # ---- masks
    def _new(self):
        return Image.new("L", (self.W, self.H), 0)

    def _pt(self, p):
        return ((p[0] + self.ox) * self.S, (p[1] + self.oy) * self.S)

    def poly(self, pts):
        m = self._new()
        ImageDraw.Draw(m).polygon([self._pt(p) for p in pts], fill=255)
        return m

    def ellipse(self, cx, cy, rx, ry, ang=0.0):
        a = math.radians(ang)
        ca, sa = math.cos(a), math.sin(a)
        pts = []
        for i in range(160):
            t = 2 * math.pi * i / 160
            x, y = rx * math.cos(t), ry * math.sin(t)
            pts.append((cx + x * ca - y * sa, cy + x * sa + y * ca))
        return self.poly(pts)

    def circle(self, cx, cy, r):
        return self.ellipse(cx, cy, r, r)

    def rrect(self, x0, y0, x1, y1, r):
        m = self._new()
        ImageDraw.Draw(m).rounded_rectangle(
            [(x0 + self.ox) * self.S, (y0 + self.oy) * self.S, (x1 + self.ox) * self.S, (y1 + self.oy) * self.S],
            radius=r * self.S, fill=255)
        return m

    def stroke(self, pts, width):
        """Polyline with round caps and joints."""
        m = self._new()
        d = ImageDraw.Draw(m)
        sp = [self._pt(p) for p in pts]
        w = width * self.S
        d.line(sp, fill=255, width=int(round(w)), joint="curve")
        for p in sp:
            d.ellipse([p[0] - w / 2, p[1] - w / 2, p[0] + w / 2, p[1] + w / 2], fill=255)
        return m

    def tube(self, pts, r0, r1):
        """A thick curve whose radius changes linearly from r0 to r1."""
        m = self._new()
        d = ImageDraw.Draw(m)
        n = len(pts)
        for i, p in enumerate(pts):
            r = (r0 + (r1 - r0) * i / (n - 1)) * self.S
            x, y = self._pt(p)
            d.ellipse([x - r, y - r, x + r, y + r], fill=255)
        return m

    @staticmethod
    def bezier(p0, p1, p2, p3, n=160):
        pts = []
        for i in range(n):
            t = i / (n - 1)
            u = 1 - t
            x = u**3 * p0[0] + 3 * u * u * t * p1[0] + 3 * u * t * t * p2[0] + t**3 * p3[0]
            y = u**3 * p0[1] + 3 * u * u * t * p1[1] + 3 * u * t * t * p2[1] + t**3 * p3[1]
            pts.append((x, y))
        return pts

    def rounded(self, mask, r):
        """Rounds the corners of a mask."""
        b = mask.filter(ImageFilter.GaussianBlur(r * self.S))
        return b.point(lambda v: 255 if v > 128 else 0).filter(ImageFilter.GaussianBlur(0.6 * self.S / 3))

    # ---- fills
    def solid(self, c):
        c = c if len(c) == 4 else (c[0], c[1], c[2], 255)
        return Image.new("RGBA", (self.W, self.H), c)

    @staticmethod
    def _col(c):
        return np.array(c if len(c) == 4 else (c[0], c[1], c[2], 255), dtype=np.float32)

    def linear(self, p0, p1, c0, c1):
        X, Y = self.grid()
        dx, dy = p1[0] - p0[0], p1[1] - p0[1]
        t = np.clip(((X - p0[0]) * dx + (Y - p0[1]) * dy) / (dx * dx + dy * dy), 0, 1)[..., None]
        arr = self._col(c0) * (1 - t) + self._col(c1) * t
        return Image.fromarray(arr.astype(np.uint8), "RGBA")

    def radial(self, cx, cy, r, c0, c1, power=1.0):
        X, Y = self.grid()
        t = (np.clip(np.sqrt((X - cx) ** 2 + (Y - cy) ** 2) / r, 0, 1) ** power)[..., None]
        arr = self._col(c0) * (1 - t) + self._col(c1) * t
        return Image.fromarray(arr.astype(np.uint8), "RGBA")

    # ---- compositing
    def _composite(self, layer, mask, opacity=1.0):
        a = ImageChops.multiply(layer.getchannel("A"), mask)
        if opacity < 1.0:
            a = a.point(lambda v: int(v * opacity))
        layer = layer.copy()
        layer.putalpha(a)
        self.img = Image.alpha_composite(self.img, layer)

    def paint(self, mask, fill, outline=None, ow=0, clip=None, opacity=1.0):
        if clip is not None:
            mask = ImageChops.multiply(mask, clip)
        if outline is not None and ow > 0:
            dil = mask.filter(ImageFilter.GaussianBlur(ow * self.S * 0.7)).point(lambda v: 255 if v > 20 else 0)
            self._composite(self.solid(outline), dil)
        layer = fill if isinstance(fill, Image.Image) else self.solid(fill)
        self._composite(layer, mask, opacity)

    def finish(self, out_w=None, out_h=None):
        out_w = out_w or self.w
        out_h = out_h or self.h
        return self.img.convert("RGBa").resize((out_w, out_h), Image.LANCZOS).convert("RGBA")


def bleed(img):
    """Copies the colour of the nearest visible pixel into fully transparent pixels.
    Prevents dark fringes when the GPU filters / mip-maps the sprite."""
    from scipy import ndimage
    arr = np.array(img.convert("RGBA"))
    empty = arr[..., 3] == 0
    if not empty.any() or empty.all():
        return img
    idx = ndimage.distance_transform_edt(empty, return_distances=False, return_indices=True)
    arr[..., :3] = arr[idx[0], idx[1], :3]
    return Image.fromarray(arr, "RGBA")


SOURCE_ART = os.path.join(os.path.dirname(__file__), "source_art")


def save_source(img, name):
    """Saves a base image for tools/make_faces.py (the bird bodies) instead of an asset."""
    img.save(os.path.join(SOURCE_ART, name), optimize=True)


def save(img, name):
    path = os.path.join(OUT, name)
    if img.mode == "RGBA":
        img = bleed(img)
    img.save(path, optimize=True)
    print("  wrote", name, img.size)


def clamp8(a):
    return np.clip(a, 0, 255).astype(np.uint8)


# --------------------------------------------------------------------------- bird
def make_bird():
    """Round red bird with an angry brow, facing right (+x). Body circle: center (256,256), r=190."""
    a = Art(640, 640, 3, ox=64, oy=64)  # roomier canvas: tail and tuft stick out of the body circle
    cx, cy, R = 256, 256, 190

    # tail feathers (behind the body)
    for ang, length, width in ((158, 170, 38), (182, 186, 40), (206, 168, 38)):
        pivot = (140, 285)
        rad = math.radians(ang)
        d = (math.cos(rad), -math.sin(rad))
        c = (pivot[0] + d[0] * length / 2, pivot[1] + d[1] * length / 2)
        m = a.ellipse(c[0], c[1], length / 2, width / 2, -ang)
        a.paint(m, a.linear((pivot[0], pivot[1]), (pivot[0] + d[0] * length, pivot[1] + d[1] * length),
                            (92, 16, 16), (30, 12, 14)), outline=(22, 8, 8), ow=5)

    # head tuft (behind the body)
    for ang, length, width in ((118, 100, 34), (92, 118, 36), (66, 96, 32)):
        pivot = (232, 130)
        rad = math.radians(ang)
        d = (math.cos(rad), -math.sin(rad))
        c = (pivot[0] + d[0] * length / 2, pivot[1] + d[1] * length / 2)
        m = a.ellipse(c[0], c[1], length / 2, width / 2, -ang)
        a.paint(m, a.linear(pivot, (pivot[0] + d[0] * length, pivot[1] + d[1] * length),
                            (160, 20, 18), (40, 10, 10)), outline=(22, 8, 8), ow=5)

    # body
    body = a.circle(cx, cy, R)
    a.paint(body, a.radial(205, 185, 330, (247, 78, 56), (170, 18, 18), 1.25), outline=(86, 8, 8), ow=8)

    # belly
    belly = a.ellipse(290, 378, 128, 84, 8)
    a.paint(belly, a.linear((290, 300), (290, 462), (253, 232, 198), (228, 190, 142)), clip=body)

    # soft shine
    a.paint(a.ellipse(160, 150, 60, 28, -38), (255, 255, 255, 255), clip=body, opacity=0.28)

    # eyes
    a.paint(a.ellipse(282, 206, 34, 40, 8), (255, 255, 255), outline=(40, 8, 8), ow=4)
    a.paint(a.ellipse(340, 216, 32, 38, 8), (255, 255, 255), outline=(40, 8, 8), ow=4)
    a.paint(a.circle(294, 211, 11), (12, 8, 8))
    a.paint(a.circle(351, 221, 10), (12, 8, 8))
    a.paint(a.circle(298, 206, 3.4), (255, 255, 255))
    a.paint(a.circle(355, 216, 3.2), (255, 255, 255))

    # angry brow (slopes down toward the beak)
    a.paint(a.stroke([(240, 168), (316, 182), (390, 212)], 24), (14, 6, 6))

    # beak
    upper = a.rounded(a.poly([(338, 252), (470, 284), (344, 306)]), 6)
    a.paint(upper, a.linear((338, 252), (470, 306), (255, 218, 70), (238, 146, 22)), outline=(112, 60, 6), ow=5)
    lower = a.rounded(a.poly([(342, 304), (442, 312), (346, 336)]), 6)
    a.paint(lower, a.linear((342, 304), (442, 336), (236, 134, 22), (196, 98, 14)), outline=(112, 60, 6), ow=5)
    return a.finish(256, 256)


# --------------------------------------------------------------------------- other birds
def _eyes_and_brow(a, ex, ey, brow_slope=1.0, scale=1.0):
    a.paint(a.ellipse(ex, ey, 30 * scale, 36 * scale, 8), (255, 255, 255), outline=(30, 20, 10), ow=4)
    a.paint(a.ellipse(ex + 54 * scale, ey + 9 * scale, 28 * scale, 34 * scale, 8), (255, 255, 255), outline=(30, 20, 10), ow=4)
    a.paint(a.circle(ex + 11 * scale, ey + 4 * scale, 10 * scale), (12, 8, 8))
    a.paint(a.circle(ex + 62 * scale, ey + 13 * scale, 9 * scale), (12, 8, 8))
    a.paint(a.circle(ex + 14 * scale, ey, 3 * scale), (255, 255, 255))
    a.paint(a.circle(ex + 65 * scale, ey + 9 * scale, 3 * scale), (255, 255, 255))
    a.paint(a.stroke([(ex - 28 * scale, ey - 36 * scale * brow_slope), (ex + 30 * scale, ey - 26 * scale),
                      (ex + 88 * scale, ey - 4 * scale)], 20 * scale), (14, 8, 6))


def make_bird_yellow():
    """Fast yellow bird (triangular body). Same canvas rules as the red bird: body fits the circle r=190."""
    a = Art(640, 640, 3, ox=64, oy=64)
    cx, cy, R = 256, 256, 188
    # black tail + head tuft behind the body
    for ang, length, width in ((172, 130, 34), (196, 118, 30)):
        pivot = (150, 300)
        rad = math.radians(ang)
        d = (math.cos(rad), -math.sin(rad))
        c = (pivot[0] + d[0] * length / 2, pivot[1] + d[1] * length / 2)
        a.paint(a.ellipse(c[0], c[1], length / 2, width / 2, -ang), (28, 24, 22), outline=(10, 8, 8), ow=4)
    for ang, length, width in ((112, 92, 28), (88, 104, 30)):
        pivot = (226, 100)
        rad = math.radians(ang)
        d = (math.cos(rad), -math.sin(rad))
        c = (pivot[0] + d[0] * length / 2, pivot[1] + d[1] * length / 2)
        a.paint(a.ellipse(c[0], c[1], length / 2, width / 2, -ang), (34, 28, 24), outline=(10, 8, 8), ow=4)
    verts = []
    for deg in (-98, 143, 24):
        verts.append((cx + R * math.cos(math.radians(deg)), cy + R * math.sin(math.radians(deg))))
    body = a.rounded(a.poly(verts), 34)
    a.paint(body, a.radial(220, 170, 330, (255, 236, 70), (240, 164, 12), 1.2), outline=(122, 78, 0), ow=7)
    belly = a.ellipse(270, 372, 120, 70, 12)
    a.paint(belly, (255, 244, 190), clip=body, opacity=0.9)
    a.paint(a.ellipse(180, 150, 50, 22, -40), (255, 255, 255), clip=body, opacity=0.35)
    _eyes_and_brow(a, 262, 218, 1.0, 0.95)
    upper = a.rounded(a.poly([(338, 262), (462, 292), (344, 314)]), 6)
    a.paint(upper, a.linear((338, 262), (462, 314), (255, 160, 40), (230, 110, 20)), outline=(120, 56, 6), ow=5)
    lower = a.rounded(a.poly([(342, 312), (436, 318), (346, 338)]), 6)
    a.paint(lower, (214, 100, 18), outline=(120, 56, 6), ow=5)
    return a.finish(256, 256)


def make_bird_blue():
    a = Art(640, 640, 3, ox=64, oy=64)
    cx, cy, R = 256, 256, 190
    for ang, length, width in ((168, 120, 30), (192, 120, 30)):
        pivot = (150, 290)
        rad = math.radians(ang)
        d = (math.cos(rad), -math.sin(rad))
        c = (pivot[0] + d[0] * length / 2, pivot[1] + d[1] * length / 2)
        a.paint(a.ellipse(c[0], c[1], length / 2, width / 2, -ang), (22, 70, 150), outline=(8, 30, 80), ow=4)
    for ang, length, width in ((120, 80, 26), (92, 96, 28), (64, 78, 26)):
        pivot = (240, 118)
        rad = math.radians(ang)
        d = (math.cos(rad), -math.sin(rad))
        c = (pivot[0] + d[0] * length / 2, pivot[1] + d[1] * length / 2)
        a.paint(a.ellipse(c[0], c[1], length / 2, width / 2, -ang), (40, 110, 210), outline=(8, 30, 80), ow=4)
    body = a.circle(cx, cy, R)
    a.paint(body, a.radial(200, 180, 330, (112, 206, 255), (30, 112, 214), 1.2), outline=(10, 50, 120), ow=8)
    a.paint(a.ellipse(285, 380, 124, 78, 8), a.linear((285, 300), (285, 462), (226, 244, 255), (176, 212, 244)), clip=body)
    a.paint(a.ellipse(160, 150, 58, 26, -38), (255, 255, 255), clip=body, opacity=0.3)
    _eyes_and_brow(a, 268, 212, 0.7, 1.0)
    upper = a.rounded(a.poly([(338, 252), (466, 284), (344, 306)]), 6)
    a.paint(upper, a.linear((338, 252), (466, 306), (255, 218, 70), (238, 146, 22)), outline=(112, 60, 6), ow=5)
    lower = a.rounded(a.poly([(342, 304), (440, 312), (346, 334)]), 6)
    a.paint(lower, (226, 128, 20), outline=(112, 60, 6), ow=5)
    return a.finish(256, 256)


# --------------------------------------------------------------------------- pig
def make_pig_body():
    """Green round body with ears. Body circle: center (256,256), r=190 (matches the physics circle)."""
    a = Art(512, 512, 3)
    cx, cy, R = 256, 256, 190
    for sx in (-1, 1):
        base = (cx + sx * 118, 112)
        ear = a.rounded(a.poly([(cx + sx * 60, 118), (cx + sx * 160, 22), (cx + sx * 200, 150)]), 14)
        a.paint(ear, a.linear((cx + sx * 60, 22), (cx + sx * 200, 150), (96, 176, 44), (56, 120, 24)),
                outline=(34, 82, 12), ow=7)
        inner = a.rounded(a.poly([(cx + sx * 92, 118), (cx + sx * 150, 62), (cx + sx * 172, 136)]), 10)
        a.paint(inner, (160, 226, 100))
    body = a.circle(cx, cy, R)
    a.paint(body, a.radial(205, 190, 340, (150, 224, 84), (84, 160, 34), 1.2), outline=(34, 82, 12), ow=8)
    a.paint(a.ellipse(170, 150, 62, 28, -35), (255, 255, 255), clip=body, opacity=0.22)
    return a.finish(256, 256)


def make_pig_face():
    """Default face (eyes + snout) on a transparent 256x256 square. Used only when no enemy_head.png is provided."""
    a = Art(256, 256, 4)
    for ex in (86, 170):
        a.paint(a.circle(ex, 96, 25), (255, 255, 255), outline=(30, 60, 10), ow=3.5)
        a.paint(a.circle(ex + (3 if ex < 128 else -3), 100, 10), (14, 14, 10))
        a.paint(a.circle(ex + (6 if ex < 128 else 0), 95, 3.5), (255, 255, 255))
    # brow ridges give the pig a smug look
    a.paint(a.stroke([(60, 70), (104, 76)], 9), (40, 90, 14))
    a.paint(a.stroke([(196, 70), (152, 76)], 9), (40, 90, 14))
    snout = a.ellipse(128, 162, 56, 42)
    a.paint(snout, a.linear((128, 120), (128, 204), (190, 240, 120), (140, 205, 74)), outline=(44, 100, 14), ow=5)
    a.paint(a.ellipse(108, 162, 8, 14, -8), (36, 82, 12))
    a.paint(a.ellipse(148, 162, 8, 14, 8), (36, 82, 12))
    # small tusk-like teeth
    a.paint(a.rounded(a.poly([(98, 192), (108, 192), (103, 208)]), 1.5), (255, 250, 230), outline=(80, 80, 60), ow=1.5)
    a.paint(a.rounded(a.poly([(148, 192), (158, 192), (153, 208)]), 1.5), (255, 250, 230), outline=(80, 80, 60), ow=1.5)
    return a.finish(256, 256)


def make_pig_damage(level):
    """Bruises and bandages drawn over the face (256x256). level 1 = hurt, 2 = badly hurt."""
    a = Art(256, 256, 4)
    a.paint(a.ellipse(62, 150, 26, 20, 20), (120, 70, 150), opacity=0.55)
    a.paint(a.ellipse(196, 66, 20, 15, -20), (110, 60, 140), opacity=0.5)
    plaster = a.rrect(150, 30, 214, 54, 8)
    a.paint(plaster, (246, 214, 168), outline=(150, 110, 70), ow=2.5, opacity=1.0)
    a.paint(a.circle(172, 42, 2.6), (200, 150, 100))
    a.paint(a.circle(192, 42, 2.6), (200, 150, 100))
    if level >= 2:
        a.paint(a.ellipse(86, 98, 34, 30), (96, 40, 120), opacity=0.55)           # black eye
        a.paint(a.ellipse(190, 168, 24, 18, -30), (170, 40, 40), opacity=0.5)
        for pts in (((100, 40), (118, 66), (110, 86)), ((60, 190), (84, 176), (96, 196))):
            a.paint(a.stroke(pts, 4.5), (60, 20, 20))
        for sx, sy in ((128, 20), (176, 14), (80, 18)):                            # dizzy stars
            star = [(sx + (9 if i % 2 == 0 else 4) * math.cos(math.radians(-90 + i * 36)),
                     sy + (9 if i % 2 == 0 else 4) * math.sin(math.radians(-90 + i * 36))) for i in range(10)]
            a.paint(a.poly(star), (255, 230, 80), outline=(160, 110, 10), ow=1.2)
    return a.finish(256, 256)


# --------------------------------------------------------------------------- block tiles
TILE = 256  # one tile = 0.5 m


def _periodic_noise(size, rng, octaves=((2, 0.5), (3, 0.3), (5, 0.2))):
    y, x = np.mgrid[0:size, 0:size].astype(np.float32)
    n = np.zeros((size, size), np.float32)
    for f, amp in octaves:
        for _ in range(3):
            ph = rng.uniform(0, 6.283, 2)
            fx, fy = rng.integers(1, f + 1), rng.integers(1, f + 1)
            n += amp * np.sin(2 * np.pi * (fx * x / size) + ph[0]) * np.sin(2 * np.pi * (fy * y / size) + ph[1])
    return n / 3.0


def make_cracks(level):
    rng = random.Random(100 + level)
    a = Art(TILE, TILE, 3)
    count = 3 if level == 1 else 8
    for _ in range(count):
        x, y = rng.uniform(20, TILE - 20), rng.uniform(20, TILE - 20)
        ang = rng.uniform(0, 6.283)
        pts = [(x, y)]
        for _ in range(rng.randint(4, 8)):
            ang += rng.uniform(-0.7, 0.7)
            L = rng.uniform(14, 34)
            x += math.cos(ang) * L
            y += math.sin(ang) * L
            pts.append((x, y))
            if rng.random() < 0.35:
                bx, by, ba = x, y, ang + rng.choice([-1, 1]) * rng.uniform(0.5, 1.1)
                bp = [(bx, by)]
                for _ in range(2):
                    bx += math.cos(ba) * rng.uniform(10, 22)
                    by += math.sin(ba) * rng.uniform(10, 22)
                    bp.append((bx, by))
                a.paint(a.stroke(bp, 2.2), (26, 18, 14, 215))
        a.paint(a.stroke(pts, 3.0), (26, 18, 14, 225))
        a.paint(a.stroke([(px + 1.5, py + 1.5) for px, py in pts], 1.2), (255, 255, 255, 90))
    return a.finish(TILE, TILE)


# --------------------------------------------------------------------------- slingshot
def make_slingshot():
    """Returns (front, back) images, 256x1024. The artwork fills the bottom 608 px (aspect 0.8 : 1.9);
    the quad is drawn 0.8 x 3.2 m so POT textures can be used. Band tips: back (64,48+416), front (192,80+416)."""
    off = 1024 - 608

    def wood_fill(art, x0, x1, dark=1.0):
        base = art.linear((x0, 0), (x1, 0), (176, 116, 58), (112, 68, 30))
        if dark != 1.0:
            arr = np.array(base).astype(np.float32)
            arr[..., :3] *= dark
            base = Image.fromarray(clamp8(arr), "RGBA")
        return base

    def prong_points(side):
        if side == "back":
            return Art.bezier((128, 372 + off), (112, 250 + off), (46, 210 + off), (64, 58 + off))
        return Art.bezier((128, 372 + off), (144, 270 + off), (208, 236 + off), (192, 90 + off))

    # back prong
    back = Art(256, 1024, 2)
    bp = prong_points("back")
    back.paint(back.tube(bp, 21, 15), wood_fill(back, 30, 130, 0.78), outline=(46, 24, 10), ow=5)
    for t in (0.74, 0.86):  # leather wrap near the tip
        px, py = bp[int(t * (len(bp) - 1))]
        back.paint(back.ellipse(px, py, 19, 8, 18), (92, 52, 22), outline=(46, 24, 10), ow=3)

    # front prong + trunk, painted as ONE shape so there is no seam where they join
    front = Art(256, 1024, 2)
    trunk = Art.bezier((128, 1040), (126, 800), (130, 600), (128, 400 + off), 120)
    fp = prong_points("front")
    union = ImageChops.lighter(front.tube(trunk, 31, 25), front.tube(fp, 22, 15))
    front.paint(union, wood_fill(front, 96, 232), outline=(46, 24, 10), ow=5)
    for t in (0.74, 0.86):
        px, py = fp[int(t * (len(fp) - 1))]
        front.paint(front.ellipse(px, py, 19, 8, -18), (92, 52, 22), outline=(46, 24, 10), ow=3)
    # highlights and bark lines
    for y in range(off + 440, 1024, 46):
        front.paint(front.stroke([(110, y), (122, y + 12)], 3), (84, 48, 20), opacity=0.6)
    front.paint(front.stroke([(110, off + 400), (110, 1024)], 5), (255, 220, 160), opacity=0.25)
    return front.finish(256, 1024), back.finish(256, 1024)


# --------------------------------------------------------------------------- ground
# --------------------------------------------------------------------------- background
# --------------------------------------------------------------------------- UI
def make_lock():
    a = Art(128, 128, 4)
    a.paint(a.stroke([(40, 58), (40, 38), (52, 24), (76, 24), (88, 38), (88, 58)], 12), (96, 100, 112), outline=(50, 54, 66), ow=3)
    a.paint(a.rrect(28, 54, 100, 112, 12), a.linear((0, 54), (0, 112), (255, 214, 90), (230, 160, 30)), outline=(120, 76, 8), ow=5)
    a.paint(a.circle(64, 80, 8), (110, 70, 10))
    a.paint(a.rrect(61, 80, 67, 98, 3), (110, 70, 10))
    return a.finish(128, 128)


def make_dot():
    a = Art(64, 64, 4)
    a.paint(a.circle(32, 32, 30), (30, 24, 20, 255), opacity=0.35)
    a.paint(a.circle(32, 32, 24), (255, 255, 255, 255))
    return a.finish(64, 64)


def make_puff():
    a = Art(128, 128, 2)
    a.paint(a.circle(64, 64, 62), a.radial(64, 64, 62, (255, 255, 255, 255), (232, 236, 242, 0), 1.6))
    return a.finish(128, 128)


# --------------------------------------------------------------------------- main
def build_all(only):
    import art_scenery as sc

    def want(name):
        return not only or name in only

    if want("bird"):
        # The bird sprites in android/assets carry the faces: run tools/make_faces.py after this.
        save_source(make_bird(), "bird_orig.png")
        save_source(make_bird_yellow(), "bird_yellow_orig.png")
        save_source(make_bird_blue(), "bird_blue_orig.png")
    if want("pig"):
        save(make_pig_body(), "enemy_body.png")
        save(make_pig_face(), "pig_face.png")
        save(make_pig_damage(1), "enemy_dmg1.png")
        save(make_pig_damage(2), "enemy_dmg2.png")
        # pig_icon.png is built from the pig face by tools/make_faces.py
    if want("blocks"):
        save(sc.make_wood(), "wood.png")
        save(sc.make_stone(), "stone.png")
        save(sc.make_glass(), "glass.png")
        save(sc.make_tnt(), "tnt.png")
        save(make_cracks(1), "crack_1.png")
        save(make_cracks(2), "crack_2.png")
    if want("slingshot"):
        front, back = make_slingshot()
        save(front, "slingshot.png")
        save(back, "slingshot_back.png")
        save(sc.make_bench(), "bench.png")
    if want("ground"):
        save(sc.make_ground(), "ground.png")
        save(sc.make_terrain(), "terrain_l1.png")
    if want("bg"):
        save(sc.make_background(), "bg.png")
    if want("ui"):
        import art_ui as ui
        save(sc.make_star(True), "star_on.png")
        save(sc.make_star(False), "star_off.png")
        save(sc.make_star_glow(), "star_glow.png")
        save(sc.make_sparkle(), "sparkle.png")
        save(ui.make_panel(), "panel.png")
        save(ui.make_btn_round((255, 196, 72), (236, 126, 14), (150, 72, 0)), "btn_orange.png")
        save(ui.make_btn_round((112, 184, 244), (34, 104, 202), (14, 56, 140)), "btn_blue.png")
        save(ui.make_btn_round((132, 220, 96), (44, 156, 56), (20, 100, 36)), "btn_green.png")
        save(ui.make_btn_pill(), "btn_pill.png")
        save(ui.make_icon_play(), "icon_play.png")
        save(ui.make_icon_pause(), "icon_pause.png")
        save(ui.make_icon_restart(), "icon_restart.png")
        save(ui.make_icon_home(), "icon_home.png")
        save(ui.make_icon_back(), "icon_back.png")
        save(ui.make_icon_gear(), "icon_gear.png")
        save(ui.make_icon_music(), "icon_music.png")
        save(ui.make_icon_sound(), "icon_sound.png")
        save(ui.make_icon_close(), "icon_close.png")
        save(ui.make_icon_slash(), "icon_slash.png")
        save(ui.make_mark_dash(), "mark_dash.png")
        save(ui.make_mark_split(), "mark_split.png")
        save(make_dot(), "dot.png")
        save(make_puff(), "puff.png")
    if want("tower"):
        import art_ui as ui
        save(ui.make_tower_bg(), "tower_bg.png")
        for k in range(4):
            save(ui.make_floor(k), "tower_floor_%d.png" % k)
        save(ui.make_door_open(), "door_open.png")
        save(ui.make_door_gold(), "door_gold.png")
        save(ui.make_door_locked(), "door_locked.png")
        save(ui.make_tag((236, 40, 26), (190, 14, 10), (110, 8, 8)), "tag_red.png")
        save(ui.make_tag((150, 60, 200), (106, 30, 156), (60, 12, 96)), "tag_purple.png")

if __name__ == "__main__":
    build_all(set(sys.argv[1:]))
