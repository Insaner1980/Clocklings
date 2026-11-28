"""The Clocklings sports car on monster tyres and exposed coilovers."""
import math

import bmesh
import bpy
import numpy as np
from mathutils import Matrix, Vector
from mathutils.geometry import delaunay_2d_cdt, tessellate_polygon

from render_car_layers import front_depth, point
from transformation_studio import rod
from vehicle_sculpt import UNIT, begin, finish, mesh_form, pigment, pos, solid, stroke


def tyre(name, x, y, depth, height, face_ratio, width, tread_phase, with_rim=True):
    # The tread, bead, spokes and axle cap rotate around this same oblique axle.
    sine = math.sqrt(1 - face_ratio ** 2)

    def point(across, radius, angle):
        radial = height * radius * math.cos(angle)
        return pos(x + across * sine + radial * face_ratio,
                   y + height * radius * math.sin(angle) - across * (.15 if name=="Far front" else .025),
                   depth - across * face_ratio + radial * sine)

    rubber = pigment("3C303C", .54)
    wheel_paint = pigment("1BC3C6", .30, .11)
    inner_paint = pigment("188387", .45)
    def lathe(label, profile, material, grooves=False):
        vertices = []
        faces = []
        groove_shade = []
        steps = 384 if grooves else 96
        for across, radius, sculpt in profile:
            u = min(1., max(0., across / width))
            # Each sweeping shoulder leads to a softly rounded V on the rolling surface.
            v = (u - .45) / (.56 if u < .45 else .68)
            bend = .44 * (1. - math.sqrt(v*v + .042**2))
            for i in range(steps):
                angle = math.tau * i / steps
                r = radius
                cavity = 0.
                if grooves and sculpt:
                    wave = (angle - bend - tread_phase) * 11 / math.tau
                    # Wide bearing faces with a softly undercut leading lip. The rounded
                    # shoulder closes the end of each groove before it reaches the wheel bead.
                    phase = (wave + .17) % 1.
                    if phase < .14:
                        k = phase / .14
                        drop = .5 - .5*math.cos(math.pi*k)
                    elif phase < .42:
                        drop = 1.
                    elif phase < .75:
                        k = (phase-.42)/.33
                        drop = .5 + .5*math.cos(math.pi*k)
                    else:
                        drop = 0.
                    r -= .079 * sculpt * drop
                    cavity = sculpt * drop
                if grooves:
                    groove_shade.append(cavity)
                vertices.append(point(across, r, angle))
        for j in range(len(profile)):
            a, b = j * steps, (j + 1) % len(profile) * steps
            for i in range(steps):
                k = (i + 1) % steps
                faces.append((a+i, a+k, b+k, b+i))
        obj = mesh_form(name + " " + label, vertices, faces, material, "wheel")
        if grooves:
            layer = obj.data.attributes.new("Recess in mould", "FLOAT", "POINT")
            layer.data.foreach_set("value", groove_shade)
        return obj

    shader=next(n for n in rubber.node_tree.nodes if n.type=="BSDF_PRINCIPLED")
    shader.inputs["Specular IOR Level"].default_value=.34
    if not shader.inputs["Base Color"].is_linked:
        tree=rubber.node_tree
        cavity=tree.nodes.new("ShaderNodeAttribute")
        cavity.attribute_name="Recess in mould"
        ramp=tree.nodes.new("ShaderNodeValToRGB")
        ramp.color_ramp.elements[0].color=(.053,.037,.049,1)
        ramp.color_ramp.elements[1].color=(.023,.014,.022,1)
        tree.links.new(cavity.outputs["Fac"],ramp.inputs[0])
        tree.links.new(ramp.outputs[0],shader.inputs["Base Color"])
        grain=tree.nodes.new("ShaderNodeTexNoise")
        grain.inputs["Scale"].default_value=280
        grain.inputs["Detail"].default_value=1
        bump=tree.nodes.new("ShaderNodeBump")
        bump.inputs["Strength"].default_value=.13
        bump.inputs["Distance"].default_value=.004
        tree.links.new(grain.outputs["Fac"],bump.inputs["Height"])
        tree.links.new(bump.outputs["Normal"],shader.inputs["Normal"])

    # A convex, wide rolling crown. The grooves die out before reaching the bead.
    side = [(-.012,.482,0),(-.016,.53,0),(-.013,.60,0),(-.005,.68,.12),
            (.010,.754,.36),(.036,.823,.64),(.070,.884,.86),
            (.117,.934,.97),(.175,.972,1.),(.244,.992,.98)]
    # Subdivide the curved shoulders as densely as the crown, so the grooves wrap without a seam.
    keys = side + [(.32,.998,1.),(.5,1.,1.),(.68,.998,1.)] + [(1-t,r,g) for t,r,g in reversed(side)]
    profile = []
    guide=np.array(keys)
    for i,(first,last) in enumerate(zip(guide,guide[1:])):
        prev,next_=guide[max(0,i-1)],guide[min(len(guide)-1,i+2)]
        length=np.linalg.norm(last-first)
        m0=(last-prev)*length/(length+np.linalg.norm(first-prev))
        m1=(next_-first)*length/(length+np.linalg.norm(next_-last))
        for t in np.linspace(0,1,max(5,round(abs(last[0]-first[0])*155)),endpoint=False):
            profile.append((2*t**3-3*t*t+1)*first+(t**3-2*t*t+t)*m0
                           +(-2*t**3+3*t*t)*last+(t**3-t*t)*m1)
    profile.append(keys[-1])
    lathe("continuous moulded chevrons", [(u*width,r,g) for u,r,g in profile], rubber, True)
    if not with_rim:
        lathe("inboard closure", [(0,.49,0),(0,.005,0),(width,.005,0),(width,.49,0)], rubber)
        return

    lathe("rolled alloy lip", [(-3,.487,0),(-4,.482,0),(-5,.465,0),(-4,.448,0),
                                (0,.432,0),(8,.418,0),(15,.405,0),(20,.405,0),
                                (20,.489,0)], wheel_paint)
    lathe("recessed barrel", [(18,.415,0),(37,.401,0),(51,.345,0),(51,.004,0),
                              (56,.004,0),(56,.415,0)], inner_paint)

    # Five open cast pockets. A second, smaller contour forms the fillet inside each opening.
    face_radius = .430
    outer = [(math.cos(a), math.sin(a)) for a in np.linspace(0,math.tau,96,endpoint=False)]
    coords = list(outer)
    holes = []
    for i in range(5):
        axis = -1.87 + math.tau*i/5
        loop = []
        for a in np.linspace(0,math.tau,40,endpoint=False):
            radial = .61 + .30 * math.cos(a)
            lateral = .32 * math.sin(a) * (1 + .27 * math.cos(a))
            loop.append((radial*math.cos(axis)-lateral*math.sin(axis),
                         radial*math.sin(axis)+lateral*math.cos(axis)))
        holes.append(loop)
        coords.extend(loop)
    edges = []
    first = 0
    for contour in [outer, *holes]:
        n = len(contour)
        edges.extend((first+j, first+(j+1)%n) for j in range(n))
        first += n
    verts, _, tris, _, _, _ = delaunay_2d_cdt([Vector(c) for c in coords], edges,
                                              [list(range(len(outer)))], 1, .000001)

    def inside(u, v, poly):
        cross = False
        for (a,b),(c,d) in zip(poly, poly[1:]+poly[:1]):
            if (b>v) != (d>v) and u < (c-a)*(v-b)/(d-b)+a:
                cross = not cross
        return cross

    def plate_point(u,v,extra=0):
        r = math.hypot(u,v)
        return point(14 + 15*(1-r*r) + extra, face_radius*r, math.atan2(v,u))

    vertices = [plate_point(p.x,p.y) for p in verts]
    faces = []
    for tri in tris:
        center = sum((verts[j] for j in tri), Vector((0.,0.))) / 3
        if not any(inside(center.x,center.y,loop) for loop in holes):
            faces.append(tuple(tri))
    indexes = {(round(p.x,5),round(p.y,5)):i for i,p in enumerate(verts)}
    pocket_faces = []
    for loop in holes:
        cu = sum(p[0] for p in loop)/len(loop)
        cv = sum(p[1] for p in loop)/len(loop)
        last = [indexes[(round(u,5),round(v,5))] for u,v in loop]
        for size,depth_step in [(.97,.6),(.945,1.7),(.935,3.6),(.92,14)]:
            row = []
            for u,v in loop:
                row.append(len(vertices))
                vertices.append(plate_point(cu+size*(u-cu),cv+size*(v-cv),depth_step))
            for j,idx in enumerate(last):
                k = (j+1)%len(loop)
                if depth_step >= 3.6:
                    pocket_faces.append(len(faces))
                faces.append((idx,last[k],row[k],row[j]))
            last = row
    casting = mesh_form(name+" five cast spokes",vertices,faces,wheel_paint,"wheel")
    casting.data.materials.append(inner_paint)
    for face in pocket_faces:
        casting.data.polygons[face].material_index = 1
    cap_profile=[(31,.13),(27,.151),(23,.161),(18,.159),(14,.150),
                 (11,.133),(9,.111),(7.8,.082),(7,.05),(6.65,.022)]
    cap=[]
    faces=[]
    count=96
    for across,r in cap_profile:
        cap.extend(point(across,r,i*math.tau/count) for i in range(count))
    for j in range(len(cap_profile)-1):
        for i in range(count):
            k=(i+1)%count
            faces.append((j*count+i,j*count+k,(j+1)*count+k,(j+1)*count+i))
    pole=len(cap)
    cap.append(point(6.6,0,0))
    last=pole-count
    faces.extend((last+i,last+(i+1)%count,pole) for i in range(count))
    faces.append(tuple(reversed(range(count))))
    mesh_form(name+" rounded centre cap",cap,faces,wheel_paint,"wheel")


def coilover(name, top, bottom, depth, radius, turns, wire, color, rod_extension=58):
    x0,y0 = top
    x1,y1 = bottom
    stroke(name+" damper rod",[(x0,y0-12,depth),(x1+5,y1+rod_extension,depth)],radius*.57,"302A33")
    points=[]
    for t in np.linspace(0,1,round(turns*42)):
        a=math.tau*(t*turns+.61)
        # Let the last half turn wind into its seat behind the piston.
        taper=min(1., (1.-t)/.058)
        dx=radius*(.22+.78*taper)*math.cos(a)
        points.append((x0+(x1-x0)*t+dx, y0+(y1-y0)*t+8*math.sin(a)-.15*dx,
                       depth+radius*.92*math.sin(a)))
    stroke(name+" wound spring",points,wire,color)


def rear_suspension(aqua):
    # The near rear arch is narrower than the front one. Its coil sits behind the
    # orange lip, with the lower pin carried by the arm on the inboard side of the tyre.
    steel = pigment("565360", .32, .48)
    piston = pigment("BBB9BF", .25, .71)
    def cylinder(name, start, end, radius, material, end_radius=None):
        rod(name, pos(*start), pos(*end), radius/UNIT, material,
            None if end_radius is None else end_radius/UNIT)

    cylinder("Rear shock tower", (311,324,-166), (307,363,-115), 19, steel, 22)
    cylinder("Rear upper spring cup", (308,355,-111), (304,375,-108), 23, steel, 38)
    coilover("Near rear", (305,361), (274,522), -108, 29.5, 4.2, 9.5, aqua, rod_extension=2)
    cylinder("Rear lower spring cup", (277,515,-108), (273,534,-108), 35, steel, 20)
    cylinder("Rear shock piston", (273,530,-108), (261,594,-107), 10, piston)
    cylinder("Rear rod seal", (273,532,-108), (271,543,-108), 15, steel, 13)
    solid("Rear axle mounting ear", [(242,623),(246,596),(251,581),(260,575),
          (271,578),(280,593),(290,614),(309,638),(293,660),(256,649)],
          -94, "403641", 6, 9, 34)
    cylinder("Rear shock pivot", (262,594,-102), (262,594,-78), 14, steel)
    cylinder("Rear pivot bolt head", (262,594,-79), (262,594,-74), 8, piston)
    stroke("Rear axle carrier", [(280,624,-110),(256,685,-106),(218,769,-83),(189,822,-67)],
           18,"38313A")


def running_gear():
    tyre("Far rear",824,829,-410,191,.46,151,.21,False)
    tyre("Near rear",152,826,6,227,.438,198,-.16)
    tyre("Far front",891,872,165,239,.453,249,-.15)
    solid("Boxed chassis",[(282,505),(354,497),(908,514),(936,550),(914,611),(894,628),
                           (720,638),(579,634),(360,587),(298,574)],-76,"312731",20,15,62)
    for name,start,end,r in [
        ("Rear trailing arm",(388,565,18),(335,699,18),22),
        ("Near radius arm",(609,603,15),(681,793,50),23),
        ("Near damper clevis",(649,610,18),(600,782,70),28),
        ("Far radius arm",(798,602,-20),(731,792,20),23),
        ("Far damper clevis",(748,606,-24),(808,804,28),21),
    ]:
        stroke(name,[start,end],r,"332A34")
    # The bar terminates at the inner hub. Only its span between the front tyres is exposed.
    solid("Top of differential casing",[(658,785),(664,767),(679,757),(744,757),(756,771),(761,799),(746,812),(669,810)],
          216,"352C35",10,10,18)
    solid("Horizontal driven axle",[(603,806),(619,796),(863,797),(883,802),(889,815),
                                   (888,858),(879,874),(623,878),(610,872)],
          lambda x,y:293-(x-615)*.29,"3B303A",12,12,36)
    solid("Axle centre housing",[(682,797),(693,786),(706,783),(770,783),(783,789),
                                 (790,802),(791,863),(783,879),(770,884),(705,883),(691,879),(684,869)],
          305,"3C313A",12,12,36)
    aqua=pigment("30DDDE",.30,.06)
    coil_shader=next(n for n in aqua.node_tree.nodes if n.type=="BSDF_PRINCIPLED")
    coil_shader.inputs["Emission Color"].default_value=(.015,.40,.41,1)
    coil_shader.inputs["Emission Strength"].default_value=.12
    coilover("Near front", (574,477),(561,699),95,46,5.1,16.5,aqua)
    coilover("Far front", (822,561),(843,745),78,43,4.1,17,aqua)
    rear_suspension(aqua)
    tyre("Near front",371,882,565,237,.477,236,-.125)



def contact_shadow(x,y,rx,ry,opacity):
    from reference_finish import ground_contact
    corners=[pos(x+a*rx,y+b*ry,-720) for a,b in [(-1,-1),(1,-1),(1,1),(-1,1)]]
    ground_contact("Contact below tyre",corners,opacity,"Wheel contact shadow")


def door_flash(body):
    # Draw the vinyl on the narrow panel between the original intake and front arch.
    outline = [(56, 203), (68, 205), (59, 214), (65, 216), (51, 231),
               (55, 218), (50.5, 217)]
    points = [Vector(p) for p in outline]
    triangles = tessellate_polygon([points])
    vertices, faces, indices = [], [], {}

    def vertex(p):
        x, y = p
        key = (round(x, 4), round(y, 4))
        if key not in indices:
            indices[key] = len(vertices)
            vertices.append(point(x, y, front_depth(body, x, y, .20)))
        return indices[key]

    # Sample across each triangle so the decal follows the curved flank all the way to its tips.
    steps = 16
    for ia, ib, ic in triangles:
        a, b, c = (points[i] for i in (ia, ib, ic))
        rows = [[vertex(a + (b-a)*(i/steps) + (c-a)*(j/steps))
                 for j in range(steps-i+1)] for i in range(steps+1)]
        for row, following in zip(rows, rows[1:]):
            for j in range(len(following)):
                faces.append((row[j], following[j], row[j+1]))
                if j+1 < len(following):
                    faces.append((row[j+1], following[j], following[j+1]))
    flash = mesh_form("Cream lightning door vinyl", vertices, faces, pigment("FFE3AC", .43), "decal")
    flash.visible_shadow = False
    return flash


def smooth_paint_outline(obj):
    """Remove narrow notches between the printed front and the return edge on the fitted copy."""
    mesh = bmesh.new()
    mesh.from_mesh(obj.data)
    seam = {}
    for edge in mesh.edges:
        if len({face.material_index for face in edge.link_faces}) > 1:
            a, b = edge.verts
            seam.setdefault(a, []).append(b)
            seam.setdefault(b, []).append(a)
    updates = {}
    for vertex, adjacent in seam.items():
        middle = sum((v.co for v in adjacent), Vector()) / len(adjacent)
        distance = math.hypot(vertex.co.x-middle.x, vertex.co.z-middle.z)
        spread = max(math.hypot(v.co.x-middle.x,v.co.z-middle.z) for v in adjacent)
        if distance > max(.013, spread*1.35):
            updates[vertex] = middle
    uv = mesh.loops.layers.uv.active
    for vertex, middle in updates.items():
        # The wardrobe UVs are an orthographic projection on the original 240 x 300 stage.
        # Move their coordinates with the silhouette using the painted face's local UV gradient.
        for loop in vertex.link_loops:
            if loop.face.material_index != 0:
                continue
            a, b = loop.link_loop_next, loop.link_loop_prev
            edges = np.array([[u.vert.co.x-vertex.co.x, u.vert.co.z-vertex.co.z] for u in (a,b)])
            if abs(np.linalg.det(edges)) > 1e-9:
                gradients = np.linalg.solve(edges, np.array([u[uv].uv-loop[uv].uv for u in (a,b)]))
                delta = np.array([middle.x-vertex.co.x,middle.z-vertex.co.z]) @ gradients
                loop[uv].uv += Vector(delta)
    for vertex, middle in updates.items():
        vertex.co.x, vertex.co.z = middle.x, middle.z
    mesh.to_mesh(obj.data)
    mesh.free()


def sports_body():
    # These are the finished meshes that supply the ordinary car's wardrobe atlas.
    names = ["Car body", "Car eye left", "Car eye right", "Car wheel housings"]
    scene = bpy.context.scene
    body, left_eye, right_eye, housings = (scene.objects[name] for name in names)
    for obj in list(scene.objects):
        if obj.type not in {"CAMERA", "LIGHT"} and obj.name not in names:
            bpy.data.objects.remove(obj, do_unlink=True)
    bpy.context.view_layer.update()
    deps = bpy.context.evaluated_depsgraph_get()
    parts = (body, left_eye, right_eye, housings)
    # Bake the original assembly-fit depths before cleaning the finish mesh.
    for obj in parts:
        if obj.data.shape_keys:
            obj.data = bpy.data.meshes.new_from_object(obj.evaluated_get(deps),
                                                      preserve_all_data_layers=True, depsgraph=deps)
        smooth_paint_outline(obj)
    # The fitted paint was authored at zero exposure. Keep its brightness in the truck studio.
    for mat in {slot.material for obj in parts for slot in obj.material_slots}:
        emission = mat.node_tree.nodes.get("Emission")
        if emission:
            emission.inputs["Strength"].default_value *= 2 ** -scene.view_settings.exposure
    bpy.context.view_layer.update()
    flash = door_flash(body)
    for eye in (left_eye, right_eye):
        eye["blink_part"] = True
    body["coat_palette"] = True
    for slot in body.material_slots:
        tree = slot.material.node_tree
        dye = tree.nodes.get("Wardrobe print color")
        if dye and dye.inputs.get("Paint mask"):
            dye.inputs["Dye"].default_value = 0
            mask = dye.inputs["Paint mask"].links[0].from_node.image
            slot.material["coat_mask_image"] = mask.name
    # Rigidly mount the complete shell, lenses and spoiler together. The cut-outs expose the shocks.
    mount = Matrix.Translation((.23, -.16, 1.94)) @ Matrix.Scale(.965, 4)
    for obj in (body, left_eye, right_eye, housings, flash):
        obj.matrix_world = mount @ obj.matrix_world


def build():
    begin(keep_body=True)
    sports_body()
    scene = bpy.context.scene
    scene.cycles.samples = 40
    scene.render.resolution_x = scene.render.resolution_y = 900
    scene.objects["Front fill"].data.energy = 175
    scene.objects["Roof reflection"].data.energy = 345
    bpy.ops.object.light_add(type="AREA", location=(-3.8,-6.5,5.7))
    key=bpy.context.object
    key.name="Broad near-side reflection on the rubber and springs"
    key.rotation_euler=(Vector((-.6,-1.0,1.35))-key.location).to_track_quat("-Z","Y").to_euler()
    key.data.energy=380
    key.data.size=3.8
    scene.world.node_tree.nodes.get("Background").inputs["Strength"].default_value=.39
    running_gear()
    solid("Upper near spring seat", [(516,470),(528,462),(553,454),(585,452),
          (612,458),(622,467),(615,476),(593,481),(554,486),(528,485),(518,480)],
          130,"353039",5,8,45)
    solid("Inner strut tower",[(521,471),(524,445),(538,414),(567,404),(585,444),
          (604,458),(583,474),(552,479)],68,"332A34",9,15,34)
    for x,y,rx,ry,opacity in [(179,1040,134,19,.33),(464,1105,186,26,.40),(1006,1081,171,21,.37)]:
        contact_shadow(x,y,rx,ry,opacity)
    finish("car-model-monster")
