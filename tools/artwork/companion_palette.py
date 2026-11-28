"""Read each character's pigments, including its dyes, from its own finished game atlases."""

from functools import lru_cache
from pathlib import Path

import numpy as np
from PIL import Image

from companion_pigments import EYE_SAMPLES, SAMPLES, pigment_passes

ATLASES = Path(__file__).resolve().parents[2] / "art/models/atlases"


def value(rgb):
    return rgb @ np.array([0.299, 0.587, 0.114], dtype=np.float32)


@lru_cache(maxsize=12)
def native(kind):
    prefix = ATLASES / (kind + "_rendered")
    paths = (
        [prefix.with_suffix(".png")] if kind == "doll" else sorted(ATLASES.glob(prefix.name + "_*.png"))
    )
    return {
        ("original" if kind == "doll" else path.stem.split("_")[-1]): np.asarray(
            Image.open(path).convert("RGBA")
        )
        for path in paths
    }


@lru_cache(maxsize=None)
def ramp(kind, role):
    sources = native(kind)
    x, y, right, bottom = SAMPLES[kind][role]
    tile = sources["original"][y:bottom, x:right]
    visible = tile[:, :, 3] == 255
    pixels = tile[:, :, :3][visible].astype(np.float32)
    levels = value(pixels)
    # Exclude e.g. an undyed brow crossing a fur tile. A single dark brow pixel
    # must never turn all the shadows on a blue bunny brown.
    if "blue" in sources:
        delta = sources["blue"][y:bottom, x:right, :3][visible].astype(float) - pixels
        direction = np.median(delta, axis=0)
        length = np.linalg.norm(direction)
        if length > 20:
            coat = delta @ direction > 0.5 * length**2
            visible[visible] = coat
            levels = levels[coat]
    if levels.size < 8:
        raise ValueError(f"Empty native pigment {kind}/{role}")
    ranges = [np.percentile(levels, interval) for interval in ((10, 25), (40, 60), (75, 90))]
    bands = [(levels >= low) & (levels <= high) for low, high in ranges]
    centers = [np.median(levels[band]) for band in bands]
    curves = {}
    for dye, image in sources.items():
        colors = image[y:bottom, x:right, :3][visible]
        stops = np.array([np.median(colors[band], axis=0) for band in bands])
        highlight = np.minimum(255, stops[-1] + (255 - centers[-1]))
        stops = np.vstack([[0, 0, 0], stops, highlight])
        anchors = [0, *centers, 255]
        curves[dye] = np.stack([np.interp(np.arange(256), anchors, c) for c in stops.T], axis=-1)
    return float(np.median(levels)), curves


def surfaces(kind, ownership):
    masks = {}
    for image, roles in zip(ownership, pigment_passes(kind), strict=True):
        channels = np.asarray(image.convert("RGB"), dtype=np.float32).transpose(2, 0, 1) / 255
        masks.update(zip(roles, channels, strict=True))
    return masks


def paint_eyes(kind, image, eye_mask):
    """Use the native iris and sclera while keeping the sculpt's gaze and reflections."""
    rgba = np.asarray(image)
    inside = np.asarray(eye_mask) > 150
    light = value(rgba[inside, :3])
    # Lenses on these sculpts pack the pupil, iris, sclera and glints together.
    # Sample dark and light parts independently so a sideways gaze or a larger
    # pupil doesn't change either paint. Pure white is the specular highlight.
    bands = []
    for box, dark in zip(EYE_SAMPLES[kind], (True, False), strict=True):
        tones = light[light < 130] if dark else light[(light > 145) & (light < 251)]
        if not tones.size:
            continue
        left, top, right, bottom = box
        sample = native(kind)["original"][top:bottom, left:right]
        sample_light = value(sample[:, :, :3])
        ink = sample_light < 130 if dark else (sample_light > 135) & (sample_light < 251)
        visible = (sample[:, :, 3] == 255) & ink
        for low, high in ((8, 22), (40, 60), (78, 92)):
            lo, hi = np.percentile(sample_light[visible], (low, high), method="nearest")
            band = visible & (sample_light >= lo) & (sample_light <= hi)
            source = np.percentile(tones, (low + high) / 2)
            bands.append((source, box, band))
    result = {}
    for dye, atlas in native(kind).items():
        stops = {0: (0, 0, 0), 255: (255, 255, 255)}
        for source, (left, top, right, bottom), band in bands:
            stops[source] = np.median(atlas[top:bottom, left:right, :3][band], axis=0)
        anchors = sorted(stops)
        channels = np.array([stops[level] for level in anchors]).T
        rgb = np.stack([np.interp(light, anchors, channel) for channel in channels], axis=-1)
        painted = rgba.copy()
        painted[inside, :3] = np.rint(rgb).astype(np.uint8)
        result[dye] = Image.fromarray(painted)
    return result


def paints(kind, image, ownership, native_images):
    rgba = np.asarray(image).copy()
    rgb = rgba[:, :, :3].astype(np.float32)
    masks = surfaces(kind, ownership)
    visible = rgba[:, :, 3] > 225
    light = value(rgb)
    output = {dye: rgb.copy() for dye in native(kind)}
    for role, weight in masks.items():
        if not visible.any() or weight[visible].max() < 0.01:
            continue
        core = (weight >= 0.99 * weight[visible].max()) & visible
        area = weight > 0.001
        ink = rgb[area]
        amount = weight[area, None]
        if role == "native":
            for dye, variant in output.items():
                if dye != "original":
                    dyed = np.asarray(native_images(dye).convert("RGB"))[area].astype(np.float32)
                    variant[area] += amount * (dyed - ink)
            continue
        center, curves = ramp(kind, role)
        shade = np.clip(np.rint(light[area] - np.median(light[core]) + center), 0, 255).astype(int)
        for dye, curve in curves.items():
            output[dye][area] += amount * (curve[shade] - ink)
    result = {}
    for dye, color in output.items():
        rgba[:, :, :3] = np.clip(np.rint(color), 0, 255).astype(np.uint8)
        result[dye] = Image.fromarray(rgba.copy())
    return result
