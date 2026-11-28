"""Shared toy-model primitives and fixed-view render settings for wardrobe transformations."""
import math
from pathlib import Path

import bpy
from mathutils import Vector

from reference_finish import configure, studio
from render_car_layers import material

ROOT = Path(__file__).resolve().parents[2]
DIRECTORY = ROOT / "art/models/transformations"
SCRATCH = ROOT / ".scratch/outfit-studio"
CELL = 448


def paint(color, roughness=0.34, metallic=0):
    name = f"Toy {color} {roughness} {metallic}"
    mat = bpy.data.materials.get(name)
    if not mat:
        mat = material(name, color, roughness)
        next(n for n in mat.node_tree.nodes if n.type == "BSDF_PRINCIPLED").inputs["Metallic"].default_value = metallic
    return mat


def finish(name, obj, color):
    obj.name = name
    obj.data.materials.append(paint(color) if isinstance(color, str) else color)
    return obj


def box(name, loc, size, color, bevel=0.1):
    bpy.ops.mesh.primitive_cube_add(size=1, location=loc)
    obj = bpy.context.object
    obj.dimensions = size
    bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)
    mod = obj.modifiers.new("Soft moulded edges", "BEVEL")
    mod.width = bevel
    mod.segments = 5
    obj.modifiers.new("Face normals", "WEIGHTED_NORMAL")
    return finish(name, obj, color)


def ball(name, loc, radii, color):
    bpy.ops.mesh.primitive_uv_sphere_add(segments=40, ring_count=24, location=loc)
    obj = bpy.context.object
    obj.scale = radii
    for face in obj.data.polygons:
        face.use_smooth = True
    return finish(name, obj, color)


def rod(name, start, end, radius, color, radius2=None):
    a, b = Vector(start), Vector(end)
    bpy.ops.mesh.primitive_cone_add(vertices=48, radius1=radius, radius2=radius if radius2 is None else radius2,
                                    depth=(b-a).length, location=(a+b)/2)
    obj = bpy.context.object
    obj.rotation_euler = (b-a).to_track_quat("Z", "Y").to_euler()
    for face in obj.data.polygons:
        face.use_smooth = len(face.vertices) == 4
    mod = obj.modifiers.new("Rolled edges", "BEVEL")
    mod.width = min(radius * 0.15, 0.035)
    mod.segments = 3
    obj.modifiers.new("Face normals", "WEIGHTED_NORMAL")
    return finish(name, obj, color)


def tube(name, points, radius, color, radii=None, cyclic=False):
    curve = bpy.data.curves.new(name, "CURVE")
    curve.dimensions = "3D"
    curve.bevel_depth = radius
    curve.bevel_resolution = 4
    curve.use_fill_caps = True
    s = curve.splines.new("BEZIER")
    s.bezier_points.add(len(points)-1)
    for i,(p,co) in enumerate(zip(s.bezier_points,points)):
        p.co = co
        p.handle_left_type = p.handle_right_type = "AUTO"
        if radii:
            p.radius = radii[i]
    s.use_cyclic_u = cyclic
    obj = bpy.data.objects.new(name,curve)
    bpy.context.collection.objects.link(obj)
    return finish(name,obj,color)


def setup(kind, target, direction, scale, keep_body=False):
    bpy.ops.wm.open_mainfile(filepath=str(ROOT / f"art/models/{kind}.blend"))
    scene = bpy.context.scene
    if not keep_body:
        for obj in list(scene.objects):
            if obj.type not in {"LIGHT","CAMERA"}:
                bpy.data.objects.remove(obj, do_unlink=True)
    bpy.context.preferences.filepaths.save_version = 0
    configure(scene)
    studio(scene)
    scene.cycles.samples = 12
    scene.render.threads = 6
    scene.render.resolution_x = scene.render.resolution_y = CELL
    camera = scene.camera
    camera.location = Vector(target) + Vector(direction).normalized()*18
    camera.rotation_euler = (-Vector(direction)).to_track_quat("-Z","Y").to_euler()
    camera.data.ortho_scale = scale
    for screen in bpy.data.screens:
        for area in screen.areas:
            if area.type == "VIEW_3D":
                area.spaces.active.region_3d.view_perspective = "CAMERA"
    return scene


def save_render(item):
    DIRECTORY.mkdir(exist_ok=True)
    SCRATCH.mkdir(exist_ok=True, parents=True)
    scene = bpy.context.scene
    scene.render.filepath = "//" + item + ".png"
    bpy.ops.outliner.orphans_purge(do_recursive=True)
    bpy.ops.wm.save_as_mainfile(filepath=str(DIRECTORY / (item + ".blend")), compress=True)
    scene.render.filepath = str(SCRATCH / (item + ".png"))
    bpy.ops.render.render(write_still=True)

