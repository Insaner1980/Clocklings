"""Anatomical paint IDs. Black leaves wardrobe items and their highlights untouched."""

# Separate native paint from new sculpts so the original Blender finish passes through exactly.
PASSES = (("body", "detail", "cream"), ("blush", "mark", "native"))
MANE = ("pink_mane", "blue_mane", "lilac_mane")

# Two small patches inside the native iris and the white of the eye. The robot
# has a dark bezel and a luminous display in these positions.
EYE_SAMPLES = {
    "car": ((81, 701, 104, 739), (36, 702, 57, 721)),
    "cat": ((355, 466, 380, 524), (327, 486, 333, 503)),
    "dog": ((530, 485, 550, 544), (503, 508, 508, 526)),
    "bunny": ((723, 766, 749, 823), (697, 785, 703, 801)),
    "dinosaur": ((46, 599, 67, 641), (26, 599, 32, 614)),
    "unicorn": ((253, 617, 271, 662), (237, 628, 242, 642)),
    "doll": ((1747, 1424, 1766, 1462), (1736, 1432, 1739, 1447)),
    "robot": ((7, 367, 12, 385), (25, 358, 48, 390)),
}


def pigment_passes(kind):
    return (*PASSES, MANE) if kind == "unicorn" else PASSES


# Rectangles contain a single native paint, in the base wardrobe atlas. Keeping the
# corresponding texels for every dye preserves e.g. a cream belly under a blue coat.
SAMPLES = {
    "cat": {
        "body": (168, 514, 235, 732),
        "detail": (390, 574, 418, 633),
        "cream": (750, 104, 815, 218),
        "blush": (686, 509, 706, 530),
    },
    "dog": {
        "body": (557, 194, 605, 365),
        "detail": (30, 559, 97, 762),
        "cream": (650, 106, 712, 187),
        "blush": (80, 271, 99, 294),
    },
    "bunny": {
        "body": (119, 120, 284, 242),
        "cream": (184, 299, 206, 320),
        "blush": (117, 698, 157, 830),
    },
    "dinosaur": {
        "body": (8, 389, 150, 545),
        "detail": (996, 346, 1050, 482),
        "mark": (647, 587, 658, 607),
        "cream": (812, 490, 875, 679),
        "blush": (151, 245, 180, 263),
    },
    "dragon": {"body": (0, 466, 116, 788)},
    "unicorn": {
        "body": (627, 517, 664, 642),
        "detail": (607, 667, 660, 690),
        "blush": (318, 145, 342, 174),
        "cream": (137, 402, 180, 428),
        "pink_mane": (72, 647, 101, 662),
        "blue_mane": (357, 260, 402, 352),
        "lilac_mane": (208, 158, 282, 189),
    },
    "car": {"body": (345, 150, 449, 206), "detail": (331, 66, 439, 116)},
    "robot": {
        "body": (299, 481, 372, 545),
        "detail": (301, 421, 354, 438),
        "cream": (168, 112, 250, 190),
    },
    "doll": {
        "body": (449, 1422, 477, 1484),
        "detail": (815, 1069, 914, 1111),
        "blush": (449, 1184, 465, 1202),
    },
}


def pigment(kind, obj, material):
    name = obj.name.lower()
    paint = material.name.lower()
    if material.get("coat_mask_image") and obj.get("coat_palette"):
        return "native"
    if kind == "dragon":
        return None
    if kind == "dinosaur":
        if name.startswith(("rounded dorsal plate", "rounded forest-green back plate")):
            return "detail"
        if name.startswith(
            (
                "dark green flank",
                "dinosaur flank spot",
                "hatchling flank",
                "hatchling tail spot",
                "gentle green brow",
                "softly raised moss brow",
                "recessed nostril",
            )
        ):
            return "mark"
        if "claw" in name or name.startswith(
            ("ivory pushing toe", "soft yellow belly", "warm cream belly", "cream tail spike")
        ):
            return "cream"
    elif kind in ("cat", "bunny", "dog"):
        if name.startswith(("dog folded ear", "chocolate eye patch", "tapered cinnamon tabby")):
            return "detail"
        if name.startswith(
            (
                "cream muzzle",
                "cream-colored fur",
                "tapered cream chest",
                "cream bib",
                "soft cream chest",
                "kitten captain's cream bib",
                "continuous upper muzzle",
            )
        ):
            return "cream"
        if (
            name.startswith(("blush inner ear", "soft blush", "velvet bunny paw pad"))
            or "rose velvet ear lining" in name
        ):
            return "blush"
        if paint == kind + " fur" or name.startswith(
            ("tapered crease between resting toes", "bunny toe furrow")
        ):
            return "body"
    elif kind == "doll":
        if paint in {
            "warm peach doll skin",
            "warm porcelain with soft cheeks",
            "warm matte doll hands",
            "warm peach crease",
        }:
            return "body"
        if paint in {"polished chestnut hair", "chestnut strand ridge"}:
            return "detail"
        return None
    elif kind == "car":
        if name.startswith(
            (
                "tall rounded cab casting",
                "unbroken red nose",
                "continuous vanilla roof",
                "vanilla door mirror",
                "rounded apple green cab",
                "continuous aerodynamic orange hull",
                "raised orange cockpit coaming",
            )
        ):
            return "body"
        if paint.startswith(("toy 495b88 ", "toy 657ca6 ")):
            return "detail"
        return None
    elif kind == "robot":
        # A bottle, rover chassis and cockpit can use the same enamel as the robot.
        if name.startswith(
            (
                "sea glass head casing",
                "receiver earpiece",
                "neck seal",
                "shin",
                "ankle ring",
                "rounded moulded boot",
                "bubble robot body",
                "clock torso",
                "tubular upper arm",
                "wrist coupling",
                "rounded mitten",
                "shoulder socket",
                "extended forearm barrel",
                "shoulder ball",
            )
        ):
            return "detail" if paint == "deep teal articulation" else "body"
        if name.startswith("curved midnight display"):
            return "cream"
        return None
    elif kind == "unicorn":
        for prefix, role in (
            ("toy ad64db ", "lilac_mane"),
            ("toy 68aaec ", "blue_mane"),
            ("toy ec5cad ", "pink_mane"),
            ("toy f36ec0 ", "pink_mane"),
        ):
            if paint.startswith(prefix):
                return role
        if name.startswith(("near ear", "far ear", "near velvet ear", "far velvet ear")):
            return "blush" if "inside" in name else "body"
        if "foal hoof" in name:
            return "detail"
    return "body" if obj.get("coat_palette") else None
