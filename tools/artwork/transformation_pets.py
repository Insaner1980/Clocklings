"""Build all measured pet transformation scenes from their editable 3D sculpts."""
import math
import sys
from pathlib import Path

import bpy
from mathutils import Matrix, Vector

sys.path.insert(0, str(Path(__file__).resolve().parent))
from transformation_studio import finish, save_render


def pose(objects, origin=(0, 0, 0), offset=(0, 0, 0), angles=(0, 0, 0), scale=(1, 1, 1)):
    from mathutils import Euler
    bpy.context.view_layer.update()
    p = Vector(origin)
    transform = (Matrix.Translation(p + Vector(offset)) @
                 Euler(tuple(math.radians(a) for a in angles)).to_matrix().to_4x4() @
                 Matrix.Diagonal((*scale, 1)) @ Matrix.Translation(-p))
    for obj in objects:
        obj.matrix_world = transform @ obj.matrix_world


def mesh(name, vertices, faces, color, smooth=False, bevel=0):
    data = bpy.data.meshes.new(name)
    data.from_pydata(vertices, [], faces)
    data.update()
    obj = bpy.data.objects.new(name, data)
    bpy.context.scene.collection.objects.link(obj)
    finish(name, obj, color)
    for f in data.polygons:
        f.use_smooth = smooth
    if bevel:
        mod = obj.modifiers.new("Rounded edge", "BEVEL")
        mod.width, mod.segments = bevel, 4
        obj.modifiers.new("Surface normals", "WEIGHTED_NORMAL")
    return obj


def grain(mat,scale,depth,roughness):
    nodes=mat.node_tree.nodes
    shader=next(n for n in nodes if n.type=="BSDF_PRINCIPLED")
    shader.inputs["Roughness"].default_value=roughness
    noise=nodes.new("ShaderNodeTexNoise")
    noise.inputs["Scale"].default_value=scale
    bump=nodes.new("ShaderNodeBump")
    bump.inputs["Strength"].default_value=.26
    bump.inputs["Distance"].default_value=depth
    mat.node_tree.links.new(noise.outputs["Fac"],bump.inputs["Height"])
    mat.node_tree.links.new(bump.outputs["Normal"],shader.inputs["Normal"])


if __name__ == "__main__":
    from transformation_bunnies import BUNNIES
    from transformation_cats import CATS
    from transformation_dogs import DOGS

    designs = {**BUNNIES, **CATS, **DOGS}
    for item in sys.argv[sys.argv.index("--") + 1:] or designs:
        designs[item]()
        save_render(item)
