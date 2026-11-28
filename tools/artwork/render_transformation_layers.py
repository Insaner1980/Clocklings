"""Reopen editable reward scenes and export the colour and eyelid layers used by the Android rig.

blender -b --factory-startup -P tools/artwork/render_transformation_layers.py -- car-model-fire-engine
Omit item IDs to render all saved models. Intermediate passes are stored in the project scratch area.
"""
from pathlib import Path
import json
import sys
import bpy
import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
from transformation_studio import DIRECTORY, SCRATCH
from companion_pigments import pigment_passes, pigment
from export_wardrobe import dye


def render(scene, path):
    scene.render.filepath = str(path)
    bpy.ops.render.render(write_still=True)


def flat_material(name, color, shader="ShaderNodeEmission"):
    mat = bpy.data.materials.new(name)
    mat.use_nodes = True
    nodes = mat.node_tree.nodes
    nodes.clear()
    emission = nodes.new(shader)
    emission.inputs[0].default_value = (*color, 1)
    output = nodes.new("ShaderNodeOutputMaterial")
    mat.node_tree.links.new(emission.outputs[0], output.inputs[0])
    return mat


def cutout(material, image):
    """Apply the same fitted-piece outline in the beauty and ID passes."""
    tree = material.node_tree
    surface = tree.nodes["Material Output"].inputs["Surface"]
    opaque = surface.links[0].from_socket
    texture = tree.nodes.new("ShaderNodeTexImage")
    texture.image = image
    uv = tree.nodes.new("ShaderNodeUVMap")
    uv.uv_map = "Fitting crop"
    tree.links.new(uv.outputs["UV"], texture.inputs["Vector"])
    mix = tree.nodes.new("ShaderNodeMixShader")
    tree.links.new(texture.outputs["Color"], mix.inputs[0])
    tree.links.new(tree.nodes.new("ShaderNodeBsdfTransparent").outputs[0], mix.inputs[1])
    tree.links.new(opaque, mix.inputs[2])
    tree.links.new(mix.outputs[0], surface)
    material["wardrobe_cutout"] = image.name


def inlay(obj, kind, role):
    if obj.type != "MESH":
        return None
    if role in {"cream","blush"} and obj.data.attributes.get("Inlay coverage"):
        return role,"Inlay coverage"
    if role != "body":
        return None
    if kind == "doll" and obj.data.color_attributes.get("Skin blush"):
        paint = obj.data.color_attributes["Skin blush"]
        shader = next(n for n in obj.active_material.node_tree.nodes if n.type=="BSDF_PRINCIPLED")
        base = np.array(shader.inputs["Base Color"].default_value[:3])
        # Read the painted skin-to-cheek transition including the hooded doll's warmer blush.
        second = "blush"
        target = (1,.34,.22)
    elif kind == "dinosaur" and obj.name.startswith(("Scout head with projecting muzzle","Sculpted dinosaur skull",
                                                      "Flowing dinosaur back","Stegosaurus flank flowing")):
        from dinosaur_pigments import SKIN, BELLY, CHEEK
        from render_dinosaur_layers import rgba
        second = "blush" if "head" in obj.name.lower() or "skull" in obj.name else "cream"
        base = np.array(rgba(SKIN)[:3])
        target = rgba(CHEEK if second=="blush" else BELLY)[:3]
        paint = obj.data.color_attributes[0]
    else:
        return None
    delta = np.asarray(target)-base
    colors = np.empty(len(paint.data)*4,dtype=np.float32)
    paint.data.foreach_get("color",colors)
    weights = np.clip((colors.reshape(-1,4)[:,:3]-base)@delta / (delta@delta),0,1)
    coverage = obj.data.attributes.new("Pigment coverage","FLOAT",paint.domain)
    coverage.data.foreach_set("value",weights.astype(np.float32))
    return second,coverage.name


def foal_face_mask(obj, channels):
    from transformation_unicorn import COAT, MUZZLE, BLUSH
    from render_unicorn_layers import rgba
    paint = obj.data.color_attributes[0]
    colors = np.empty(4*len(paint.data),dtype=np.float32)
    paint.data.foreach_get("color",colors)
    base,nose,blush = (np.array(rgba(c)[:3]) for c in (COAT,MUZZLE,BLUSH))
    # The face painter interpolates three pigments on one surface. Recover the same
    # weights before lighting so foliage cannot tint the ID for the white coat.
    axes = np.stack([nose-base,blush-base])
    warm,pink = np.clip((colors.reshape(-1,4)[:,:3]-base)@np.linalg.pinv(axes),0,1).T
    weights = {"body": np.maximum(0,1-warm-pink), "cream": warm, "blush": pink}
    encoded = np.zeros((len(warm),4),dtype=np.float32)
    for i,role in enumerate(channels):
        if role in weights:
            encoded[:,i] = weights[role]
    encoded[:,3] = 1
    layer = obj.data.color_attributes.new(name="Face pigment IDs",type="FLOAT_COLOR",domain=paint.domain)
    layer.data.foreach_set("color",encoded.ravel())
    mask = flat_material("Foal face IDs",(0,0,0))
    node = mask.node_tree.nodes.new("ShaderNodeVertexColor")
    node.layer_name = layer.name
    mask.node_tree.links.new(node.outputs["Color"],mask.node_tree.nodes["Emission"].inputs[0])
    return mask


def pigment_pass(path, output, channels):
    """Render the closed face used by the body sprite, independently of lighting."""
    bpy.ops.wm.open_mainfile(filepath=str(path))
    scene = bpy.context.scene
    kind = path.stem.split("-")[0]
    if kind in {"model","shoes"}:
        kind = "doll"
    ids = {role: flat_material(role, tuple(int(i==j) for j in range(3))) for i,role in enumerate(channels)}
    black = flat_material("Equipment", (0,0,0))
    clear = flat_material("Clear", (1,1,1), "ShaderNodeBsdfTransparent")
    for obj in scene.objects:
        if obj.get("blink_part"):
            obj.hide_render = True
        if obj.hide_render or obj.is_holdout:
            continue
        for slot in obj.material_slots:
            original = slot.material
            if not original:
                continue
            role = pigment(kind,obj,original)
            mask = ids.get(role,black)
            if kind=="unicorn" and obj.name == "Young unicorn face":
                mask = foal_face_mask(obj,channels)
            elif blend := inlay(obj,kind,role):
                second,attribute = blend
                mask = mask.copy()
                tree = mask.node_tree
                attr = tree.nodes.new("ShaderNodeAttribute")
                attr.attribute_name = attribute
                mix = tree.nodes.new("ShaderNodeMixRGB")
                mix.inputs[1].default_value = (*[int(c=="body") for c in channels],1)
                mix.inputs[2].default_value = (*[int(c==second) for c in channels],1)
                tree.links.new(attr.outputs["Fac"],mix.inputs[0])
                tree.links.new(mix.outputs[0],tree.nodes["Emission"].inputs[0])
            if outline := original.get("wardrobe_cutout"):
                mask = mask.copy()
                cutout(mask,bpy.data.images[outline])
            elif not role and original.use_nodes and any(n.type == "BSDF_TRANSPARENT" for n in original.node_tree.nodes):
                mask = clear
            slot.link = "OBJECT"
            slot.material = mask
    scene.view_settings.view_transform = "Raw"
    scene.view_settings.look = "None"
    scene.view_settings.exposure = 0
    scene.view_settings.gamma = 1
    scene.cycles.samples = 6
    scene.cycles.use_denoising = False
    render(scene,output)


def native_paints(path, directory):
    bpy.ops.wm.open_mainfile(filepath=str(path))
    scene = bpy.context.scene
    native_body = any(m.get("coat_mask_image") for m in bpy.data.materials)
    if not native_body and not (path.stem.startswith("car-") and any(obj.get("base_eye") for obj in scene.objects)):
        return
    spec = json.loads(scene["wardrobe_export"])
    visible = [obj for obj in scene.objects if obj.type in {"MESH","CURVE","SURFACE","FONT"} and not obj.hide_render]
    for shade,color in spec["colors"].items():
        if color:
            dye(color,spec.get("tones",{}).get(shade,(1,1,1)))
            for obj in visible:
                obj.hide_render = bool(obj.get("blink_part"))
            if native_body:
                render(scene,directory / f"{path.stem}-native-{shade}.png")
            for obj in visible:
                obj.hide_render = not obj.get("blink_part")
            render(scene,directory / f"{path.stem}-eyes-{shade}.png")


def layers(path):
    bpy.ops.wm.open_mainfile(filepath=str(path))
    scene = bpy.context.scene
    scene.render.threads = 3
    item = path.stem
    open_image = SCRATCH / (item+".png")
    if not open_image.exists() or open_image.stat().st_mtime < path.stat().st_mtime:
        render(scene, open_image)
    objects = [o for o in scene.objects if o.type in {"CURVE","MESH","SURFACE","FONT"} and not o.hide_render]
    eyes = [o for o in objects if o.get("blink_part")]
    for obj in eyes:
        obj.hide_render = True
    if eyes:
        render(scene, SCRATCH / (item+"-closed.png"))
    for obj in eyes:
        obj.hide_render = False
    black = flat_material("Occluding geometry",(0,0,0))
    fur = flat_material("Original sculpt finish",(1,0,0))
    green = flat_material("Eyelids",(0,1,0))
    native_eye = flat_material("Wardrobe eye",(0,1,1))
    clear = flat_material("Transparent surface",(1,1,1),"ShaderNodeBsdfTransparent")
    for obj in objects:
        if obj.is_holdout:
            continue
        replacement = (native_eye if obj.get("base_eye") else green) if obj in eyes else black
        if obj.get("preserve_original_fur"):
            replacement = fur
        # Shared meshes may have different roles (for instance a small pilot and its cockpit).
        for slot in obj.material_slots:
            identifier = replacement
            if slot.material and slot.material.use_nodes:
                nodes = slot.material.node_tree.nodes
                if not slot.material.get("wardrobe_cutout") and any(n.type == "BSDF_TRANSPARENT" for n in nodes):
                    identifier = clear
                if outline := slot.material.get("wardrobe_cutout"):
                    identifier = identifier.copy()
                    cutout(identifier, bpy.data.images[outline])
            slot.link = "OBJECT"
            slot.material = identifier
    scene.view_settings.view_transform = "Standard"
    scene.view_settings.look = "None"
    scene.view_settings.exposure = 0
    scene.view_settings.gamma = 1
    scene.cycles.samples = 6
    scene.cycles.use_denoising = False
    scene.world.color = (0,0,0)
    render(scene, SCRATCH / (item+"-mask.png"))


if __name__ == "__main__":
    SCRATCH.mkdir(exist_ok=True, parents=True)
    ids = sys.argv[sys.argv.index("--")+1:] if "--" in sys.argv else []
    paths = [DIRECTORY / (item+".blend") for item in ids] if ids else sorted(DIRECTORY.glob("*.blend"))
    for path in paths:
        layers(path)
        for index,channels in enumerate(pigment_passes(path.stem.split("-")[0])):
            pigment_pass(path,SCRATCH / f"{path.stem}-pigments{index}.png",channels)
        native_paints(path,SCRATCH)

