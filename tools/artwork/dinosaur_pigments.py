"""Dinosaur sculpt pigments sampled from the original wardrobe atlas, in sRGB."""

from pathlib import Path

import bpy
import numpy as np
from companion_pigments import SAMPLES


def original_pigments():
    path = Path(__file__).resolve().parents[2] / "art/models/atlases/dinosaur_rendered_original.png"
    image = bpy.data.images.load(str(path), check_existing=False)
    try:
        image.colorspace_settings.name = "Non-Color"
        width, height = image.size
        pixels = np.empty(width * height * 4, dtype=np.float32)
        image.pixels.foreach_get(pixels)
        pixels = pixels.reshape(height, width, 4)[::-1]

        def sample(box):
            left, top, right, bottom = box
            patch = pixels[top:bottom, left:right]
            rgb = np.median(patch[:, :, :3][patch[:, :, 3] > 0.99], axis=0)
            return bytes(np.rint(rgb * 255).astype(np.uint8)).hex().upper()

        source = SAMPLES["dinosaur"]
        return (
            sample(source["body"]),
            sample(source["mark"]),
            sample(source["detail"]),
            sample(source["cream"]),
            sample((689, 793, 704, 801)),
            sample(source["blush"]),
        )
    finally:
        bpy.data.images.remove(image)


SKIN, SPOT, PLATE, BELLY, CLAW, CHEEK = original_pigments()
