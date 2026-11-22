#!/usr/bin/env python3
"""
Generates android/assets/font.fnt + font.png (BMFont text format, loaded by libGDX BitmapFont).

Font: ROGENZ (demo version by Nirfonic, tools/fonts/ROGENZ-Demo.ttf). White glyphs with a dark baked
outline: tint the fill with font.setColor(...) and the outline stays dark.

The demo has only letters, digits, space, "." and ",". The punctuation the game needs (! ' : / % + - =) is
therefore built here from the brush strokes of the font itself ("I", "." and "O"), so every character in the
UI keeps the same hand-drawn look. A character that the font does not have and that is not built here gets no
glyph (libGDX skips it).

The metrics (64 px em, base 62, line height 81) are the same as the previous font, so no layout changes.

Usage: python3 tools/generate_font.py        (needs Pillow and opencv-python-headless)
"""
import os
import cv2
import numpy as np
from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(__file__)
FONT_PATH = os.path.join(HERE, "fonts", "ROGENZ-Demo.ttf")
OUT_DIR = os.path.join(HERE, "..", "android", "assets")

SIZE = 64            # em size in the atlas, in pixels
SS = 4               # supersampling factor for smooth edges
STROKE = 5           # outline width in pixels
BOLD = 1.2           # extra fill thickness in pixels: the brush strokes of the font are thin
OUTLINE = (43, 22, 8)
PAD = 3
TRACKING = 2         # extra advance so the outlines of neighbouring letters do not merge
ATLAS_W, ATLAS_H = 1024, 1024
BASE = 62            # distance from the top of the line to the baseline
LINE_HEIGHT = 81

CANVAS_W, CANVAS_H = 220 * SS, 220 * SS
ORIGIN = (60 * SS, 150 * SS)  # pen position (left end of the baseline) on the supersampled canvas

font = ImageFont.truetype(FONT_PATH, SIZE * SS)
CAP = font.getbbox("I", anchor="ls")  # (l, t, r, b) of a capital I; -t is the cap height


def glyph_mask(ch):
    """The glyph as an 8 bit mask on the supersampled canvas."""
    img = Image.new("L", (CANVAS_W, CANVAS_H), 0)
    ImageDraw.Draw(img).text(ORIGIN, ch, font=font, anchor="ls", fill=255)
    return np.array(img)


def tight(mask):
    """The part of [mask] that is not empty, as (array, left, top) in canvas pixels."""
    ys, xs = np.where(mask > 8)
    return mask[ys.min():ys.max() + 1, xs.min():xs.max() + 1], xs.min(), ys.min()


def put(piece, center_x, center_y, sx=1.0, sy=1.0, angle=0.0):
    """
    A new canvas with [piece] (from [tight]) scaled by [sx], [sy], turned by [angle] degrees (counter-clockwise)
    and centered at ([center_x], [center_y]) given in pixels (not supersampled) from the pen position, y up.
    """
    arr = piece[0]
    h, w = arr.shape
    arr = cv2.resize(arr, (max(1, int(w * sx)), max(1, int(h * sy))), interpolation=cv2.INTER_LINEAR)
    if angle:
        h, w = arr.shape
        diag = int(np.hypot(w, h)) + 4
        padded = np.zeros((diag, diag), np.uint8)
        y0, x0 = (diag - h) // 2, (diag - w) // 2
        padded[y0:y0 + h, x0:x0 + w] = arr
        rot = cv2.getRotationMatrix2D((diag / 2, diag / 2), angle, 1.0)
        arr = cv2.warpAffine(padded, rot, (diag, diag), flags=cv2.INTER_LINEAR)
    out = np.zeros((CANVAS_H, CANVAS_W), np.uint8)
    h, w = arr.shape
    left = int(ORIGIN[0] + center_x * SS - w / 2)
    top = int(ORIGIN[1] - center_y * SS - h / 2)
    out[top:top + h, left:left + w] = arr
    return out


def union(*masks):
    return np.maximum.reduce(masks)


def build_extra_glyphs():
    """Punctuation built from the font's own strokes. Returns {char: (mask, advance in pixels)}."""
    cap = -CAP[1] / SS                      # cap height in pixels
    # Pieces: the crossbar and the stem of a "T", a dot and a round "O".
    t_mask = glyph_mask("T")
    ys, _ = np.where(t_mask > 8)
    top_row = ys.min()
    cut = int(top_row + cap * SS * 0.26)
    bar_mask, stem_mask = t_mask.copy(), t_mask.copy()
    bar_mask[cut:] = 0
    stem_mask[:cut] = 0
    bar, stem = tight(bar_mask), tight(stem_mask)
    dot, ring = tight(glyph_mask(".")), tight(glyph_mask("O"))
    stem_h = stem[0].shape[0] / SS
    bar_w = bar[0].shape[1] / SS
    extra = {}

    # "!": the upper part of the stem and the dot below it.
    upper = (stem[0][: int(stem[0].shape[0] * 0.72)], stem[1], stem[2])
    extra["!"] = (union(put(upper, 8, cap - upper[0].shape[0] / SS / 2), put(dot, 6, dot[0].shape[0] / SS / 2)), 24)

    # ":": two dots.
    d = dot[0].shape[0] / SS / 2
    extra[":"] = (union(put(dot, 6, d), put(dot, 8, cap * 0.55)), 22)

    # "'": a short piece of the stem at the top.
    short = (stem[0][: int(stem[0].shape[0] * 0.38)], stem[1], stem[2])
    extra["'"] = (put(short, 6, cap - short[0].shape[0] / SS / 2), 16)

    # "/": the stem leaning to the right and made a little longer.
    extra["/"] = (put(stem, 12, cap * 0.5, sy=cap * 1.12 / stem_h, angle=-24), 34)

    # "-": the crossbar, shortened.
    extra["-"] = (put(bar, 14, cap * 0.38, sx=0.5), 30)

    # "=": two crossbars.
    extra["="] = (union(put(bar, 16, cap * 0.28, sx=0.55), put(bar, 16, cap * 0.55, sx=0.55)), 36)

    # "+": a crossbar crossed by a stem.
    extra["+"] = (union(put(bar, 16, cap * 0.40, sx=0.55), put(stem, 16, cap * 0.40, sy=cap * 0.62 / stem_h)), 36)

    # "%": a slash with a small ring at the top left and one at the bottom right.
    small = cap * 0.42 / (ring[0].shape[0] / SS)
    extra["%"] = (union(
        put(stem, 22, cap * 0.5, sy=cap * 1.0 / stem_h, angle=-24),
        put(ring, 9, cap * 0.78, sx=small, sy=small),
        put(ring, 36, cap * 0.22, sx=small, sy=small),
    ), 50)
    return extra


def outlined(mask):
    """Turns a glyph mask into (RGBA image, left, top) cropped to the outline, in supersampled pixels."""
    k = lambda r: cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (2 * int(r * SS) + 1, 2 * int(r * SS) + 1))
    fill = cv2.dilate(mask, k(BOLD))
    ring = cv2.dilate(mask, k(BOLD + STROKE))
    ys, xs = np.where(ring > 8)
    l, r, t, b = xs.min(), xs.max() + 1, ys.min(), ys.max() + 1
    fill, ring = fill[t:b, l:r].astype(np.float32) / 255, ring[t:b, l:r].astype(np.float32) / 255
    rgba = np.zeros((b - t, r - l, 4), np.float32)
    rgba[..., :3] = np.array(OUTLINE, np.float32) * (1 - fill[..., None]) + 255 * fill[..., None]
    rgba[..., 3] = np.maximum(ring, fill) * 255
    img = Image.fromarray(rgba.round().astype(np.uint8), "RGBA")
    w, h = max(1, round(img.width / SS)), max(1, round(img.height / SS))
    return img.resize((w, h), Image.LANCZOS), l, t


def main():
    extra = build_extra_glyphs()
    atlas = Image.new("RGBA", (ATLAS_W, ATLAS_H), (0, 0, 0, 0))
    chars = []
    cx, cy, row_h = PAD, PAD, 0
    for code in range(32, 127):
        ch = chr(code)
        if ch == " ":
            chars.append((code, 0, 0, 0, 0, 0, 0, round(font.getlength("I") / SS) + TRACKING))
            continue
        if ch in extra:
            mask, advance = extra[ch]
        else:
            if font.getmask(ch).getbbox() is None or not _has_glyph(ch):
                continue
            mask, advance = glyph_mask(ch), round(font.getlength(ch) / SS)
        glyph, l, t = outlined(mask)
        w, h = glyph.size
        if cx + w + PAD > ATLAS_W:
            cx, cy, row_h = PAD, cy + row_h + PAD, 0
        atlas.paste(glyph, (cx, cy))
        xoff = round((l - ORIGIN[0]) / SS)
        yoff = BASE + round((t - ORIGIN[1]) / SS)
        chars.append((code, cx, cy, w, h, xoff, yoff, advance + TRACKING))
        cx += w + PAD
        row_h = max(row_h, h)

    os.makedirs(OUT_DIR, exist_ok=True)
    atlas.save(os.path.join(OUT_DIR, "font.png"), optimize=True)
    lines = [
        f'info face="ROGENZ" size={SIZE} bold=0 italic=0 charset="" unicode=0 stretchH=100 smooth=1 aa=1 padding=0,0,0,0 spacing=0,0',
        f"common lineHeight={LINE_HEIGHT} base={BASE} scaleW={ATLAS_W} scaleH={ATLAS_H} pages=1 packed=0",
        'page id=0 file="font.png"',
        f"chars count={len(chars)}",
    ]
    for code, x, y, w, h, xo, yo, adv in chars:
        lines.append(f"char id={code} x={x} y={y} width={w} height={h} xoffset={xo} yoffset={yo} xadvance={adv} page=0 chnl=15")
    lines.append("kernings count=0")
    with open(os.path.join(OUT_DIR, "font.fnt"), "w") as f:
        f.write("\n".join(lines) + "\n")
    print("font written:", len(chars), "glyphs:", "".join(chr(c[0]) for c in chars))


_CMAP = None


def _has_glyph(ch):
    global _CMAP
    if _CMAP is None:
        from fontTools.ttLib import TTFont
        _CMAP = TTFont(FONT_PATH).getBestCmap()
    return ord(ch) in _CMAP


if __name__ == "__main__":
    main()
