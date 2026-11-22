"""Makes the bronze and silver stars (and their glow rays) from the golden originals.

Usage (from the project root):  python3 tools/recolor_stars.py
Needs Pillow and numpy. Writes into android/assets/.
"""
import colorsys
import numpy as np
from PIL import Image

ASSETS = "android/assets/"


def recolor(src, dst, hue_deg, sat_mul, val_mul):
    im = np.array(Image.open(ASSETS + src).convert("RGBA")).astype(np.float32) / 255
    rgb, alpha = im[..., :3], im[..., 3]
    out = np.zeros_like(rgb)
    for y in range(alpha.shape[0]):
        for x in range(alpha.shape[1]):
            if alpha[y, x] == 0:
                continue
            _, s, v = colorsys.rgb_to_hsv(*rgb[y, x])
            out[y, x] = colorsys.hsv_to_rgb(hue_deg / 360, min(1, s * sat_mul), min(1, v * val_mul))
    Image.fromarray((np.dstack([out, alpha]) * 255).astype(np.uint8), "RGBA").save(ASSETS + dst)


recolor("star_on.png", "star_bronze.png", 24, 1.0, 0.8)
recolor("star_on.png", "star_silver.png", 215, 0.2, 0.88)
recolor("star_glow.png", "star_glow_bronze.png", 24, 1.0, 0.9)
recolor("star_glow.png", "star_glow_silver.png", 215, 0.3, 1.0)
