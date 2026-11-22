#!/usr/bin/env python3
"""
Builds the face art from the photos in tools/source_art/ (img1..img4.jpg):

  img1 -> android/assets/enemy_head.png   the pig head (square crop; Assets.kt cuts it to a circle)
  img2 -> android/assets/bird.png         the red bird
  img3 -> android/assets/bird_yellow.png  the yellow bird
  img4 -> android/assets/bird_blue.png    the blue bird
  img1 -> android/assets/pig_icon.png     the pig icon of the HUD

A bird sprite keeps the tail and the crest of the original bird (tools/source_art/bird_*_orig.png), a round
body in the bird's color that replaces the original eyes and beak, and the face on top. Sprites are 512 px;
the body circle is 156/256 of the old 256 px sprite, i.e. the same size relative to the image as before, so
GameRenderer.BIRD_SPRITE_RATIO stays valid.

Faces are found with OpenCV's Haar cascade (tools/source_art/haarcascade_frontalface_default.xml).

Usage: python3 tools/make_faces.py        (needs Pillow, numpy, scipy and opencv-python-headless)
"""
import os
import cv2
import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(__file__)
SRC = os.path.join(HERE, "source_art")
ASSETS = os.path.join(HERE, "..", "android", "assets")

SPRITE = 512
FACE_RADIUS_FRACTION = 0.84   # the face fills this part of the body radius
HEAD_PX = 512
FACE_MARGIN = 1.22            # crop side = detected face box * this
FACE_LIFT = 0.04              # the crop center is moved up by this part of the box, to keep the forehead

BIRDS = {
    # name: (photo, original sprite, light, base, outline) colors of the body disc
    "bird": ("img2.jpg", "bird_orig.png", (255, 98, 72), (214, 52, 38), (105, 12, 10)),
    "bird_yellow": ("img3.jpg", "bird_yellow_orig.png", (255, 236, 90), (250, 205, 35), (126, 82, 8)),
    "bird_blue": ("img4.jpg", "bird_blue_orig.png", (112, 205, 255), (52, 150, 235), (14, 62, 140)),
}


def bleed(img):
    """Copies the color of the nearest visible pixel into fully transparent pixels (no dark fringes in mip-maps)."""
    from scipy import ndimage
    arr = np.array(img.convert("RGBA"))
    empty = arr[..., 3] == 0
    if not empty.any() or empty.all():
        return img
    idx = ndimage.distance_transform_edt(empty, return_distances=False, return_indices=True)
    arr[..., :3] = arr[idx[0], idx[1], :3]
    return Image.fromarray(arr, "RGBA")


def face_square(photo):
    """The square part of [photo] around the face, as a PIL image."""
    img = cv2.imread(os.path.join(SRC, photo))
    cascade = cv2.CascadeClassifier(os.path.join(SRC, "haarcascade_frontalface_default.xml"))
    faces = cascade.detectMultiScale(cv2.cvtColor(img, cv2.COLOR_BGR2GRAY), 1.1, 5, minSize=(150, 150))
    if len(faces) == 0:
        raise SystemExit(f"no face found in {photo}")
    x, y, w, h = max(faces, key=lambda f: f[2] * f[3])
    side = int(w * FACE_MARGIN)
    cx, cy = x + w / 2, y + h / 2 - h * FACE_LIFT
    left = int(min(max(cx - side / 2, 0), img.shape[1] - side))
    top = int(min(max(cy - side / 2, 0), img.shape[0] - side))
    side = min(side, img.shape[1] - left, img.shape[0] - top)
    crop = img[top:top + side, left:left + side]
    return Image.fromarray(cv2.cvtColor(crop, cv2.COLOR_BGR2RGB)).resize((HEAD_PX, HEAD_PX), Image.LANCZOS)


def circle_mask(size, radius, supersample=4):
    big = Image.new("L", (size * supersample, size * supersample), 0)
    c = size * supersample / 2
    ImageDraw.Draw(big).ellipse((c - radius * supersample, c - radius * supersample,
                                 c + radius * supersample, c + radius * supersample), fill=255)
    return big.resize((size, size), Image.LANCZOS)


def body_disc(light, base, outline):
    """The round body: outline ring, color gradient lit from the upper left, soft highlight."""
    n = SPRITE
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32)
    c = n / 2
    r = np.hypot(xx - c, yy - c)
    t = np.clip(np.hypot(xx - (c - 0.35 * 156), yy - (c - 0.4 * 156)) / (2 * 156), 0, 1)[..., None]
    color = np.array(light, np.float32) * (1 - t) + np.array(base, np.float32) * t
    rim = np.clip((r - 156 * 0.80) / (156 * 0.20), 0, 1)[..., None]
    color = color * (1 - 0.25 * rim)
    ring = np.clip(r - (156 - 7), 0, 1)[..., None]
    color = color * (1 - ring) + np.array(outline, np.float32) * ring
    rgb = Image.fromarray(color.round().astype(np.uint8), "RGB").convert("RGBA")
    rgb.putalpha(circle_mask(n, 156))
    return rgb


def bird_sprite(name):
    photo, orig, light, base, outline = BIRDS[name]
    original = Image.open(os.path.join(SRC, orig)).convert("RGBA").resize((SPRITE, SPRITE), Image.LANCZOS)

    # Keep only what sticks out of the body circle (tail, crest) and drop the old beak tip.
    a = np.array(original)
    yy, xx = np.mgrid[0:SPRITE, 0:SPRITE]
    r = np.hypot(xx - SPRITE / 2, yy - SPRITE / 2)
    beak = (xx > SPRITE / 2 + 0.55 * 156) & (yy > SPRITE / 2 - 0.15 * 156) & (yy < SPRITE / 2 + 0.7 * 156)
    a[(r < 156 + 5) | beak, 3] = 0
    sprite = Image.fromarray(a, "RGBA")

    sprite.alpha_composite(body_disc(light, base, outline))

    face_r = 156 * FACE_RADIUS_FRACTION
    d = int(face_r * 2)
    face = face_square(photo).resize((d, d), Image.LANCZOS).convert("RGBA")
    face.putalpha(circle_mask(d, face_r))
    # thin dark line around the face so it separates from the colored body
    ring = Image.new("RGBA", (d + 8, d + 8), (0, 0, 0, 0))
    ring.putalpha(circle_mask(d + 8, face_r + 3))
    dark = Image.new("RGBA", ring.size, outline + (255,))
    dark.putalpha(ring.getchannel("A"))
    sprite.alpha_composite(dark, (SPRITE // 2 - (d + 8) // 2, SPRITE // 2 - (d + 8) // 2))
    sprite.alpha_composite(face, (SPRITE // 2 - d // 2, SPRITE // 2 - d // 2))
    return sprite


def pig_icon(head):
    """A round icon of the pig face with a dark green ring, 128 px."""
    n, r = 128, 60
    icon = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    ring = Image.new("RGBA", (n, n), (38, 96, 20, 255))
    ring.putalpha(circle_mask(n, r + 4))
    icon.alpha_composite(ring)
    face = head.resize((2 * r, 2 * r), Image.LANCZOS).convert("RGBA")
    face.putalpha(circle_mask(2 * r, r))
    icon.alpha_composite(face, (n // 2 - r, n // 2 - r))
    return icon


def main():
    head = face_square("img1.jpg")
    head.save(os.path.join(ASSETS, "enemy_head.png"), optimize=True)
    bleed(pig_icon(head)).save(os.path.join(ASSETS, "pig_icon.png"), optimize=True)
    for name in BIRDS:
        bleed(bird_sprite(name)).save(os.path.join(ASSETS, f"{name}.png"), optimize=True)
    print("faces written")


if __name__ == "__main__":
    main()
