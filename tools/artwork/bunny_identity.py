"""The game's bunny face and its lop-ear proportions in the four reward poses."""
import math

import bpy
import numpy as np
from mathutils import Matrix, Vector

from pet_sculpt import UNIT, mesh, point, smooth, tagged
from render_bunny_layers import ear_center, point as base_point


# Layout coordinates are (across, down, depth). Both ears use the same length
# relative to the skull. The pose sets the bend, including the part hidden by a prop.
EAR_POSES = {
    "hat": (3.22,
        [(542,182,58),(438,199,66),(363,313,62),(313,467,-49),(302,586,-116),(314,668,-119)],
        [(747,159,58),(848,185,66),(905,306,62),(951,457,-46),(979,582,-113),(970,657,-117)]),
    "car": (2.90,
        [(559,263,0),(474,290,10),(398,357,25),(316,407,45),(223,434,79),(170,501,126)],
        [(803,252,8),(878,293,19),(915,371,40),(953,463,65),(956,552,73),(919,627,45)]),
    "moon": (3.52,
        [(446,283,-33),(330,290,-51),(234,376,-69),(179,516,-83),(153,660,-99),(190,795,-113)],
        [(705,281,14),(785,312,18),(850,413,8),(865,537,-19),(828,642,-96),(723,710,-163)]),
    "strawberry": (3.35,
        [(521,193,3),(420,216,14),(351,316,12),(313,434,4),(265,563,-4),(247,649,-14)],
        [(749,191,4),(851,214,15),(912,309,19),(956,436,15),(1002,563,-1),(1015,650,-17)]),
}


def lop_ears(kind, fur):
    scale, *poses = EAR_POSES[kind]
    native_lengths = []
    for right in (False, True):
        centers = np.array([ear_center(t, right)[0] for t in np.linspace(0, 1, 257)])
        native_lengths.append(np.linalg.norm(np.diff(centers, axis=0), axis=1).sum())
    length = scale * np.mean(native_lengths)
    for side, controls in zip(("Left", "Right"), poses):
        samples = np.array([tuple(p) for p in smooth(controls, 45, closed=False)])
        arc = np.r_[0, np.cumsum(np.linalg.norm(np.diff(samples, axis=0), axis=1))]
        path = samples[0] + (samples - samples[0]) * length / arc[-1]
        tangent = np.gradient(path, axis=0)
        tangent /= np.linalg.norm(tangent, axis=1)[:, None]
        width_axis = np.column_stack((tangent[:, 1], -tangent[:, 0], np.zeros(len(path))))
        width_axis /= np.linalg.norm(width_axis, axis=1)[:, None]
        depth_axis = np.cross(width_axis, tangent)
        arc /= arc[-1]

        def ring(t, angles, offset=0):
            i = min(np.searchsorted(arc, t, side="right")-1, len(arc)-2)
            u = (t-arc[i]) / (arc[i+1]-arc[i])
            center, width, depth = [(1-u)*a[i]+u*a[i+1] for a in (path,width_axis,depth_axis)]
            taper = math.sin(math.pi*t)**.3
            root = min(1,t/.35)
            radius = 23*scale*taper*(.65+.35*root*root*(3-2*root))
            return [point(*(center+width*radius*math.cos(a)+depth*(13*scale*taper*math.sin(a)-offset)))
                    for a in angles]

        rings, segments = 128, 64
        angles = np.arange(segments)*math.tau/segments
        verts = [point(*path[0])]
        for j in range(1,rings):
            verts.extend(ring(j/rings, angles))
        tip = len(verts)
        verts.append(point(*path[-1]))
        faces = [(0,1+i,1+(i+1)%segments) for i in range(segments)]
        for j in range(rings-2):
            for i in range(segments):
                a=1+j*segments+i
                b=1+j*segments+(i+1)%segments
                faces.append((a,a+segments,b+segments,b))
        faces.extend((tip,tip-segments+(i+1)%segments,tip-segments+i) for i in range(segments))
        tagged(mesh(side+" soft lop ear",verts,faces,fur,True))

        verts=[]
        rows, cols = 90, 28
        for s in np.linspace(0,1,rows+1):
            half=1.04*math.sin(math.pi*s)**.42
            verts.extend(ring(.20+.75*s, 4.8+half*np.linspace(-1,1,cols+1), .17*scale))
        faces=[]
        for row in range(rows):
            for column in range(cols):
                a=row*(cols+1)+column
                faces.append((a,a+cols+1,a+cols+2,a+1))
        mesh(side+" rose velvet ear lining",verts,faces,"EE9189",True)


def magician_head():
    # The same finished head and paired lenses are drawn by the ordinary wardrobe.
    # The alpha in the original head finish is its fur dye mask.
    scene = bpy.context.scene
    head, eyes = (scene.objects[name] for name in ("Bunny head", "Bunny eyes"))
    for obj in list(scene.objects):
        if obj.type not in {"LIGHT", "CAMERA"} and obj not in (head, eyes):
            bpy.data.objects.remove(obj, do_unlink=True)
    nodes = head.active_material.node_tree.nodes
    head.active_material["coat_mask_image"] = nodes["Image Texture"].image.name
    head["coat_palette"] = True
    eyes["blink_part"] = True
    scale = EAR_POSES["hat"][0]*50/UNIT
    pose = (Matrix.Translation(point(645,323,5))
            @ Matrix.Rotation(math.radians(-5),4,"Y")
            @ Matrix.Scale(scale,4)
            @ Matrix.Translation(-Vector(base_point(120.5,112.5,0))))
    for obj in (head, eyes):
        material = obj.active_material
        material.node_tree.nodes["Emission"].inputs["Strength"].default_value *= 2**-scene.view_settings.exposure
        obj.matrix_world = pose @ obj.matrix_world
