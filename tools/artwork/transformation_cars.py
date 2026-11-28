"""Rebuild the five editable vehicles from their measured camera-space construction profiles.

blender -b --factory-startup -P tools/artwork/transformation_cars.py -- fire van monster recycling rocket
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import vehicle_fire
import vehicle_monster
import vehicle_recycling
import vehicle_rocket
import vehicle_van

if __name__ == "__main__":
    designs = {"fire":vehicle_fire, "van":vehicle_van, "monster":vehicle_monster,
               "recycling":vehicle_recycling, "rocket":vehicle_rocket}
    names = sys.argv[sys.argv.index("--")+1:] if "--" in sys.argv else designs
    for name in names:
        designs[name].build()

