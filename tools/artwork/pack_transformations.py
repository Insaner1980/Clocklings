"""Pack reopened Blender renders into per-companion Android atlases and write their sprite regions.

Run render_transformation_layers.py first. Then run this script with Pillow installed.
"""
import argparse
from dataclasses import dataclass
from pathlib import Path
from PIL import Image, ImageChops, ImageDraw, ImageFilter
from companion_palette import paint_eyes, paints
from companion_pigments import pigment_passes
from optimize_atlases import ROOT, publish

CELL = 448
KINDS = {"bunny","car","cat","dinosaur","dog","dragon","robot","unicorn"}
SOURCE = ROOT / "art/models/transformations"
MASTERS = ROOT / "art/models/atlases/transformations"
ANDROID = ROOT / "app/src/main/res/drawable-nodpi"
REGIONS = ROOT / "app/src/main/java/fi/clocklings/ui/companion/TransformationRegions.kt"


@dataclass
class Sprite:
    image: Image.Image
    left: int
    top: int
    x: int = 0
    y: int = 0

    def kotlin(self):
        return f"ModelSprite({self.x}, {self.y}, {self.image.width}, {self.image.height}, {self.left}, {self.top})"


def eye_regions(mask):
    remaining = mask.filter(ImageFilter.MaxFilter(3)).point(lambda x: 255 if x > 80 else 0)
    while remaining.getbbox():
        x,y = remaining.getbbox()[:2]
        row = remaining.crop((0,y,remaining.width,y+1)).getbbox()
        before = remaining.copy()
        ImageDraw.floodfill(remaining,(row[0],y),0)
        region = ImageChops.subtract(before,remaining)
        box = region.getbbox()
        if box[2]-box[0] >= 2 and box[3]-box[1] >= 2:
            yield region


def pieces(kind, item, directory):
    def load(suffix):
        with Image.open(directory / (item + suffix + ".png")) as im:
            return im.convert("RGBA")
    original = load("")
    bounds = original.getchannel("A").getbbox()
    if not bounds or 0 in bounds[:2] or bounds[2] == original.width or bounds[3] == original.height:
        raise ValueError(f"Camera clips {item}: {bounds}")
    mask = load("-mask")
    base = original.copy()
    eye_mask = mask.getchannel("G").point(lambda x: 255 if x > 150 else 0)
    regions = list(eye_regions(eye_mask))
    if regions:
        closed = load("-closed")
        for region in regions:
            base.paste(closed,mask=region)
    masks = [load(f"-pigments{i}") for i in range(len(pigment_passes(kind)))]
    has_native = masks[1].getchannel("B").getextrema()[1] > 2
    bodies = paints(kind,base,masks,lambda dye: load("-native-"+dye))
    finish = mask.getchannel("R")
    if finish.getbbox():
        bodies["original"] = Image.composite(base,bodies["original"],finish)
    native_lenses = has_native or mask.getchannel("B").getextrema()[1] > 150
    painted_eyes = paint_eyes(kind, original, eye_mask) if regions and not native_lenses else {}
    # These four scenes share the base dragon's camera. Preserve its origin
    # and scale so equipping a wide prop cannot shrink or displace the dragon.
    fixed_camera = item.startswith("dragon-model-")
    if fixed_camera:
        bounds = (0,0,*original.size)
    inset = 0 if fixed_camera else 13
    ratio = (CELL-2*inset)/max(bounds[2]-bounds[0],bounds[3]-bounds[1])
    width = round((bounds[2]-bounds[0])*ratio)
    height = round((bounds[3]-bounds[1])*ratio)
    def sprite(im):
        fitted = im.crop(bounds).resize((width,height),Image.Resampling.LANCZOS)
        box = fitted.getchannel("A").getbbox()
        if not box:
            return None
        return Sprite(fitted.crop(box), (CELL-width)//2 + box[0], CELL-inset-height + box[1])
    frames = {}
    for dye,body in bodies.items():
        native_eyes = directory / f"{item}-eyes-{dye}.png"
        if dye!="original" and native_lenses:
            with Image.open(native_eyes) as lenses:
                opened = Image.alpha_composite(body,lenses.convert("RGBA"))
        else:
            opened = Image.composite(painted_eyes.get(dye, original),body,eye_mask)
        eyes = []
        for region in regions:
            overlay = opened.copy()
            overlay.putalpha(ImageChops.multiply(original.getchannel("A"),region))
            eyes.append(sprite(overlay))
        frames[dye] = (sprite(body),eyes)
    return frames


def layout(sprites):
    x=y=row_height=width=0
    for sprite in sorted(sprites,key=lambda s:-s.image.height):
        if x and x+sprite.image.width>1024:
            x=0
            y+=row_height+2
            row_height=0
        sprite.x,sprite.y=x,y
        x+=sprite.image.width+2
        width=max(width,x-2)
        row_height=max(row_height,sprite.image.height)
    canvas=Image.new("RGBA",(width,y+row_height))
    for sprite in sprites:
        canvas.paste(sprite.image,(sprite.x,sprite.y))
    return canvas


def pack(directory):
    groups = {}
    for source in sorted(SOURCE.glob("*.blend")):
        item = source.stem
        kind = item.split("-")[0]
        kind = kind if kind in KINDS else "doll"
        groups.setdefault(kind,[]).append(item)
    MASTERS.mkdir(exist_ok=True)
    entries = []
    dye_entries = []
    for kind,items in sorted(groups.items()):
        frames = {item:pieces(kind,item,directory) for item in items}
        name = f"{kind}_transformations_rendered"
        dyes = list(frames[items[0]])
        for dye in dyes:
            atlas_name = name if dye=="original" else name+"_"+dye
            sprites = []
            for frame in frames.values():
                body,eyes = frame[dye]
                sprites.extend([body,*eyes])
            layout(sprites).save(MASTERS / (atlas_name+".png"))
        alternatives = [f"CompanionColor.{dye.upper()} to R.drawable.{name}_{dye}" for dye in dyes if dye!="original"]
        if alternatives:
            dye_entries.extend([
                f"        R.drawable.{name} to",
                "            mapOf(",
                *[f"                {pair}," for pair in alternatives],
                "            ),",
            ])
        for item,frame in frames.items():
            body,eyes = frame["original"]
            layers = ", ".join(eye.kotlin() for eye in eyes)
            entries.extend([
                f'        "{item}" to',
                "            ModelFrame(",
                f"                R.drawable.{name},",
                f"                {body.kotlin()},",
                f"                listOf({layers})," if eyes else "                emptyList(),",
                "            ),",
            ])
    publish(MASTERS,ANDROID,False)
    REGIONS.write_text("\n".join(["// Generated by tools/artwork/pack_transformations.py.",
        '@file:Suppress("MagicNumber")',"","package fi.clocklings.ui.companion","","import fi.clocklings.R",
        "import fi.clocklings.domain.CompanionColor","","internal val transformationDyes =","    mapOf(",
        *dye_entries,"    )","","internal val transformationArt =","    mapOf(",*entries,"    )",""]),encoding="utf-8")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--renders",type=Path,default=ROOT/".scratch/outfit-studio")
    pack(parser.parse_args().renders)

