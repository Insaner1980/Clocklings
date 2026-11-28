"""Open the game's original dragon and dress its existing wardrobe meshes."""
from pathlib import Path

import bpy
import numpy as np
from mathutils import Vector

from fantasy_sculpt import line, world
from render_transformation_layers import cutout
from render_unicorn_layers import rgba


SOURCE = Path(__file__).resolve().parents[2] / "art/models/dragon.blend"


def open_dragon(replace_wings=False, replace_horns=False):
    bpy.ops.wm.open_mainfile(filepath=str(SOURCE))
    scene = bpy.context.scene
    bpy.context.preferences.filepaths.save_version = 0
    # Only the finished fitting collection is drawn by the ordinary wardrobe.
    for obj in list(bpy.data.collections["Construction sculpts"].objects):
        bpy.data.objects.remove(obj, do_unlink=True)
    for obj in bpy.data.collections["Wardrobe fitting model"].objects:
        layer = obj["atlas_layer"]
        obj.hide_render = ((replace_wings and layer in ("wing_left","wing_right"))
                           or (replace_horns and layer == "horns"))
        obj.hide_set(obj.hide_render)
        if layer == "eyes":
            obj["blink_part"] = True
        elif layer != "horns":
            obj["coat_palette"] = True
            finish = obj.active_material
            finish["coat_mask_image"] = finish.node_tree.nodes["Image Texture"].image.name
    scene.camera.location = (0, -18, 2.65)
    scene.camera.data.ortho_scale = 6.8
    scene.render.resolution_x = scene.render.resolution_y = 816
    scene.render.resolution_percentage = 100
    scene.render.threads_mode = "FIXED"
    scene.render.threads = 8
    scene.cycles.samples = 32
    if replace_wings:
        # The original limb fitting includes a sliver of each ordinary wing.
        # Crop those slivers at the outer arm contour when the wings are swapped.
        y,x = np.indices((384,390))
        edge = np.interp(y,[0,12,26,39,54,67,78,91,102,120],
                         [79,63,51,44,40,39,41,46,39,30])
        visible = (y>119) | ((x>=edge) & (377-x>=edge))
        crop_fitting(scene.objects["Dragon limbs"], mask_image("Wing fittings",visible),56,158)
    if replace_horns:
        crop_fitting(scene.objects["Dragon head"], mask_image("Crown fitting", 1-horn_roots()),20,42)
    # The open eyes are separate original meshes. Rest the eyelid underneath each.
    head = scene.objects["Dragon head"].evaluated_get(bpy.context.evaluated_depsgraph_get())
    for cx in (239,362):
        points = []
        for dx, y in [(-25,237),(-14,244),(0,247),(14,244),(25,237)]:
            hit, point, _, _ = head.ray_cast(Vector(world(cx+dx,y,-400)), Vector((0,1,0)))
            if not hit:
                raise ValueError("Dragon eyelid misses its face")
            points.append((cx+dx,y,point.y*100-.45))
        line("Resting dragon eyelid",points,2.1,"574055",[.12,.77,1,.77,.12])


def mask_image(name, values):
    height,width = values.shape
    image = bpy.data.images.new(name,width,height,alpha=False)
    image.colorspace_settings.name = "Non-Color"
    pixels = np.ones((height,width,4),dtype=np.float32)
    pixels[:,:,:3] = values[::-1,:,None]
    image.pixels.foreach_set(pixels.ravel())
    image.pack()
    return image


def crop_fitting(obj, mask, left, top):
    # Front and back share this orthographic cut. Cutting just the painted front
    # exposes the solid purple return surface of the original wardrobe fitting.
    data = obj.data
    original_uv = data.uv_layers.active
    evaluated = obj.evaluated_get(bpy.context.evaluated_depsgraph_get())
    coords = np.array([evaluated.matrix_world @ v.co for v in evaluated.data.vertices])
    u = 3*(120+50*coords[:,0]-left)/mask.size[0]
    v = 1-3*(280-50*coords[:,2]-top)/mask.size[1]
    uv = data.uv_layers.new(name="Fitting crop")
    loops = np.empty(len(data.loops),dtype=np.int32)
    data.loops.foreach_get("vertex_index",loops)
    uv.data.foreach_set("uv",np.column_stack((u,v))[loops].astype(np.float32).ravel())
    data.uv_layers.active = original_uv
    original_uv.active_render = True
    for slot in obj.material_slots:
        material = slot.material.copy()
        slot.material = material
        cutout(material, mask)


def horn_roots():
    image = bpy.data.images["head-glaze.png"]
    width,height = image.size
    rgb = np.empty(width*height*4,dtype=np.float32)
    image.pixels.foreach_get(rgb)
    rgb = rgb.reshape(height,width,4)[::-1]
    gold = np.clip((rgb[:,:,1]-rgb[:,:,2])*18+.05,0,1)
    gold[65:] = 0
    return gold


def frost_horns():
    for name in ("Dragon horns","Dragon head"):
        tree = bpy.data.objects[name].active_material.node_tree
        surface = tree.nodes["Emission"].inputs["Color"]
        original = surface.links[0].from_socket
        ice = tree.nodes.new("ShaderNodeValToRGB")
        ice.label = "Ice glaze over original horn ridges"
        ice.color_ramp.elements[0].color = rgba("307EAA")
        ice.color_ramp.elements[1].color = rgba("C9F8FF")
        stop = ice.color_ramp.elements.new(.65)
        stop.color = rgba("62CBE6")
        tree.links.new(original, ice.inputs["Fac"])
        if name == "Dragon horns":
            tree.links.new(ice.outputs["Color"],surface)
        else:
            roots = tree.nodes.new("ShaderNodeTexImage")
            roots.image = mask_image("Icy horn roots",horn_roots())
            mix = tree.nodes.new("ShaderNodeMixRGB")
            tree.links.new(roots.outputs["Color"],mix.inputs[0])
            tree.links.new(original,mix.inputs[1])
            tree.links.new(ice.outputs["Color"],mix.inputs[2])
            tree.links.new(mix.outputs[0],surface)


def tint_tail(hue, value=1):
    # Only the gold paint beyond the purple flank belongs to the original tail tip.
    for obj_name in ("Dragon body", "Dragon body fitting from wing_right"):
        obj = bpy.data.objects[obj_name]
        finish = obj.active_material.copy()
        obj.active_material = finish
        tree = finish.node_tree
        shader = tree.nodes["Emission"]
        color = shader.inputs["Color"].links[0].from_socket
        xyz = tree.nodes.new("ShaderNodeSeparateXYZ")
        tree.links.new(tree.nodes.new("ShaderNodeNewGeometry").outputs["Position"], xyz.inputs[0])
        tail = tree.nodes.new("ShaderNodeMath")
        tail.operation = "GREATER_THAN"
        tree.links.new(xyz.outputs["X"], tail.inputs[0])
        tail.inputs[1].default_value = 1.56
        rgb = tree.nodes.new("ShaderNodeSeparateColor")
        tree.links.new(color, rgb.inputs[0])
        gold = tree.nodes.new("ShaderNodeMath")
        gold.operation = "GREATER_THAN"
        tree.links.new(rgb.outputs["Green"], gold.inputs[0])
        tree.links.new(rgb.outputs["Blue"], gold.inputs[1])
        mask = tree.nodes.new("ShaderNodeMath")
        mask.operation = "MULTIPLY"
        tree.links.new(tail.outputs[0], mask.inputs[0])
        tree.links.new(gold.outputs[0], mask.inputs[1])
        tint = tree.nodes.new("ShaderNodeHueSaturation")
        tint.label = "Wardrobe colour for the gold leaf"
        tint.inputs["Hue"].default_value = hue
        tint.inputs["Value"].default_value = value
        tree.links.new(mask.outputs[0], tint.inputs["Fac"])
        tree.links.new(color, tint.inputs["Color"])
        tree.links.new(tint.outputs[0], shader.inputs["Color"])

