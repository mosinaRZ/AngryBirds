#!/usr/bin/env python3
"""
Builds the launcher icons android/res/mipmap-*/ic_launcher.png from tools/source_art/icon.jpg.
The picture is cut to a centered square (the flag in the corner falls outside) and scaled to each density.

Usage: python3 tools/make_launcher_icon.py        (needs Pillow)
"""
import os
from PIL import Image

HERE = os.path.dirname(__file__)
SOURCE = os.path.join(HERE, "source_art", "icon.jpg")
RES = os.path.join(HERE, "..", "android", "res")
SIZES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}

img = Image.open(SOURCE).convert("RGB")
side = min(img.size)
left, top = (img.width - side) // 2, (img.height - side) // 2
square = img.crop((left, top, left + side, top + side))
for density, size in SIZES.items():
    folder = os.path.join(RES, f"mipmap-{density}")
    os.makedirs(folder, exist_ok=True)
    square.resize((size, size), Image.LANCZOS).save(os.path.join(folder, "ic_launcher.png"), optimize=True)
print("launcher icons written:", ", ".join(f"{d} {s}px" for d, s in SIZES.items()))
