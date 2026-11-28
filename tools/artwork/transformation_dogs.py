"""Pose the wardrobe puppy with its four individually sculpted outfits."""
import math
from functools import partial
import sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parent))
from pet_sculpt import *
from transformation_studio import save_render

NAVY="253450"


def use_wardrobe_paint(obj):
    material = obj.active_material
    if "coat_mask_image" not in material:
        nodes = material.node_tree.nodes
        material["coat_mask_image"] = next(n.image.name for n in nodes if n.type == "TEX_IMAGE")
        emission = next(n for n in nodes if n.type == "EMISSION")
        emission.inputs["Strength"].default_value *= 2 ** -bpy.context.scene.view_settings.exposure
    obj["coat_palette"] = True


def wardrobe_dog(outfit, camera, mouth, scale, tilt):
    """Fit the original wardrobe meshes in camera space, pivoting at the mouth."""
    from mathutils import Matrix
    from render_dog_layers import point as base_point

    fur = start("dog", *camera, keep_body=True)
    scene = bpy.context.scene
    face_layers = {"head", "eyes", "ear_left", "ear_right"}
    pose = (Matrix.Translation(point(*mouth))
            @ Matrix.Rotation(math.radians(tilt), 4, "Y")
            @ Matrix.Scale(scale * 50 / UNIT, 4)
            @ Matrix.Translation(-Vector(base_point(120, 148, 0))))
    for obj in list(scene.objects):
        if obj.type in {"LIGHT", "CAMERA"}:
            continue
        layer = obj.get("atlas_layer")
        if obj.name.startswith("Dog ") and (layer in face_layers or outfit is branch):
            use_wardrobe_paint(obj)
            if layer == "eyes":
                obj["blink_part"] = True
                obj["base_eye"] = True
            obj.matrix_world = pose @ obj.matrix_world
        elif obj.name == "Dog body" and outfit in (rescue, post):
            # A temporary source for the native chest paint, removed after fitting.
            obj.hide_render = True
        else:
            bpy.data.objects.remove(obj, do_unlink=True)
    # Opening dog.blend replaces the window context in a live Blender MCP session.
    with bpy.context.temp_override(window=bpy.context.window_manager.windows[0]):
        outfit(fur, pose)


def fit_chest(torso, pose):
    """Transfer the base chest's paint coordinates to the exposed, posed rib cage."""
    from mathutils.bvhtree import BVHTree
    from mathutils.geometry import barycentric_transform
    from render_dog_layers import point as base_point

    source = bpy.context.scene.objects["Dog body"]
    data = source.data
    data.calc_loop_triangles()
    triangles = data.loop_triangles
    tree = BVHTree.FromPolygons([v.co for v in data.vertices],
                                [tri.vertices[:] for tri in triangles], all_triangles=True)
    # Border of the bib on the original 240 x 300 wardrobe grid, including its tapered tip.
    outline = [(116,172),(126,174),(135,181),(141,191),(143,201),(141,211),
               (135,228),(130,246),(125,258),(120,264),(115,257),(111,244),
               (107,229),(100,211),(98,201),(98,190),(102,180),(109,174)]
    inverse = pose.inverted()
    border = []
    for x,y in outline:
        v = pose @ Vector(base_point(x,y,0))
        border.append((626+v.x*UNIT,1110-v.z*UNIT))
    marking = patch("Native puppy chest marking",torso,border,source.active_material,0,.90)
    use_wardrobe_paint(marking)
    native_uv = data.uv_layers.active.data
    uvs = []
    for vertex in marking.data.vertices:
        local = inverse @ vertex.co
        # The dark tip on the seated dog is the shadow between its front paws.
        # Keep the same outline but sample lit cream where the posed chest is exposed.
        y = 280 - 50 * local.z
        if y > 205:
            lit_y = 205 + 12 * (1 - math.exp(-(y - 205) / 12))
            local.z = (280 - lit_y) / 50
        local.y = -10
        hit, _, index, _ = tree.ray_cast(local, Vector((0,1,0)))
        tri = triangles[index]
        corners = [data.vertices[i].co for i in tri.vertices]
        coords = [native_uv[i].uv.to_3d() for i in tri.loops]
        uvs.append(barycentric_transform(hit,*corners,*coords).xy)
    layer = marking.data.uv_layers.new(name="Studio paint")
    for loop in marking.data.loops:
        layer.data[loop.index].uv = uvs[loop.vertex_index]
    bpy.data.objects.remove(source,do_unlink=True)


def leg(name,outline,d,front,fur,creases):
    border=smooth(outline,8)
    top=min(p.y for p in border);bottom=max(p.y for p in border)
    rings,segments=76,56;vs=[];faces=[]
    for i in range(rings+1):
        t=(i+.004)/(rings+.008);y=top+(bottom-top)*t
        hits=[]
        for a,b in zip(border,border[1:]+border[:1]):
            if (a.y<=y<b.y) or (b.y<=y<a.y):
                hits.append(a.x+(b.x-a.x)*(y-a.y)/(b.y-a.y))
        left,right=min(hits),max(hits)
        x=(left+right)/2;w=(right-left)/2
        for j in range(segments):
            angle=TAU*j/segments;u=math.cos(angle);v=math.sin(angle)
            grooves=sum(math.exp(-((u-at)/.09)**2) for at in (-.37,.34))
            toe=max(0,min(1,(t-.72)*4.3))*max(0,-v)
            depth=max(.2,min(front*1.09,w*.97))*(1-.090*grooves*toe)
            vs.append(point(x+w*u,y-.9*grooves*toe,d+depth*v-15*t))
    for i in range(rings):
        for j in range(segments):
            faces.append((i*segments+j,i*segments+(j+1)%segments,(i+1)*segments+(j+1)%segments,(i+1)*segments+j))
    faces.extend((tuple(reversed(range(segments))),tuple(range(rings*segments,(rings+1)*segments))))
    obj=tagged(mesh(name,vs,faces,fur,True))
    if obj.data.polygons[segments*20+segments*3//4].normal.y>0:obj.data.flip_normals()
    toes(obj,creases,"C38A58")
    return obj


def haunch(name,cx,cy,rx,ry,d,fur,foot):
    tagged(cushion(name+" haunch",oval(cx,cy,rx,ry),d,rx*.87,rx*.78,fur,steps=2))
    x,y,w,h=foot
    leg(name+" grounded back paw",[(x-w,y),(x-w*.76,y-h*.64),(x,y-h),(x+w*.68,y-h*.80),
        (x+w,y-h*.25),(x+w*.94,y+h*.47),(x+w*.62,y+h*.72),(x,y+h*.75),
        (x-w*.59,y+h*.71),(x-w*.89,y+h*.50)],d-46,w*.80,fur,[(x-w*.32,y-h*.22,-4,h*.79),(x+w*.22,y-h*.20,-3,h*.75)])


def posed_torso(outline,fur):
    return tagged(cushion("Puppy chest, belly and shoulders",outline,33,108,122,fur))


def branch(_fur, _pose):
    bark="AE672F"
    line("Forked branch through the mouth",[(132,661,-162),(179,632,-165),(235,604,-171),(321,606,-176),
         (408,593,-171),(510,579,-167),(595,568,-142),(650,565,-140),(786,568,-165),(912,569,-165),
         (1004,543,-157),(1046,503,-148),(1065,470,-143)],19,bark,[.20,.76,1.06,1.1,1.14,1.10,1,1,1.03,1.03,.82,.58,.20])
    line("Secondary fork",[(245,607,-166),(216,581,-170),(185,560,-171),(145,548,-166)],15,bark,[1.2,.99,.81,.40])
    for x,y in [(405,591),(912,566)]:
        ellipsoid("Branch knot",x,y,-183,21,21,10,"A9632D")
        ellipsoid("Recess in bark",x,y,-192,9,9,4,"8A552C")
    blade=cushion("Green leaf on the twig",[(1043,504),(1038,468),(1050,436),(1080,415),(1125,411),
             (1122,435),(1113,466),(1088,489)],-149,16,12,"82B439")
    surface_line("Leaf midrib",blade,[(1046,496),(1069,467),(1096,439),(1121,417)],2,"B4C965",[.5,1,.6,.1])
    for vein in [[(1063,476),(1056,466),(1053,455)],[(1071,464),(1090,465),(1101,458)],
                 [(1082,450),(1080,438),(1081,431)],[(1093,439),(1105,438),(1116,430)]]:
        surface_line("Leaf side vein",blade,vein,1.5,"A8C05B",[.22,1,.18])
    for coords in [[(275,604,-190),(308,601,-194),(348,598,-191),(373,593,-188)],
                   [(734,565,-183),(776,562,-185),(822,566,-183),(853,565,-182)],
                   [(949,559,-178),(973,550,-177),(990,544,-173)]]:
        line("Raised grain on branch",coords,1.9,"985827",[.25]+[1]*(len(coords)-2)+[.2])


def puppy_paw(name, center, width, height, depth, angle, fur, limb=None):
    """Three soft toes share one palm; angle is the paw's tilt onto the ring."""
    from reference_finish import fuse
    x,y,d = center
    a = math.radians(angle)
    across,down = Vector((math.cos(a),math.sin(a))),Vector((-math.sin(a),math.cos(a)))
    # Shape the toe furrows into one broad pad. The pad is flatter where it bears weight.
    verts, faces = [], []
    sections, segments = 50, 72
    for i in range(sections+1):
        latitude = math.pi*((i+.002)/(sections+.004)-.5)
        h = math.sin(latitude)
        spread = max(0,math.cos(latitude))**.73
        distal = max(0,min(1,(h+.18)*1.18))
        for k in range(segments):
            angle = TAU*k/segments
            u,v=spread*math.cos(angle),spread*math.sin(angle)
            groove=sum(math.exp(-((u-c)/.082)**2) for c in (-.36,.35))
            floor=height*(1.29*h if h>0 else h)
            lift=height*.075*distal*groove
            px,py=Vector((x,y))+across*(width*u)+down*(floor-lift)
            pz=d+depth*v+depth*.27*groove*distal*max(0,-math.sin(angle))
            verts.append(point(px,py,pz))
    for i in range(sections):
        for k in range(segments):
            faces.append((i*segments+k,i*segments+(k+1)%segments,
                          (i+1)*segments+(k+1)%segments,(i+1)*segments+k))
    faces += [tuple(reversed(range(segments))),tuple(range(sections*segments,(sections+1)*segments))]
    pad=mesh(name+" sculpted paw",verts,faces,fur,True)
    if pad.data.polygons[segments*25+segments*3//4].normal.y>0:
        pad.data.flip_normals()
    if limb:
        shaft=swept(name+" leg",limb,fur)
        return tagged(fuse([shaft.name,pad.name],name,voxel=.011))
    return tagged(pad)


def rescue_rope(points, radius, name):
    """Lay three strands about the rope axis, following the changing direction of the lead."""
    from transformation_studio import tube
    axis = smooth(points,12,False)
    arc = 0
    strands = [[],[],[]]
    for i, loc in enumerate(axis):
        if i:
            arc += (loc-axis[i-1]).length
        tangent = (axis[min(i+1,len(axis)-1)]-axis[max(0,i-1)]).normalized()
        side = tangent.cross(Vector((0,0,1))).normalized()
        normal = tangent.cross(side).normalized()
        for k,strand in enumerate(strands):
            angle = arc/(radius*1.13) + k*TAU/3
            center = loc+(math.cos(angle)*side+math.sin(angle)*normal)*(radius*.35)
            strand.append(point(*center))
    for k,strand in enumerate(strands):
        tube(name+" strand",strand,radius*.67/UNIT,("263956","304867","2A405D")[k])


def float_ring(x,y,rx,ry,thick):
    vs=[];fs=[];seg=180;cross=32
    for i in range(seg):
        a=i*TAU/seg
        for j in range(cross):
            b=j*TAU/cross
            vs.append(point(x+(rx-thick+thick*math.cos(b))*math.cos(a),
                            y+(ry-thick+thick*math.cos(b))*math.sin(a),-126-thick*.79*math.sin(b)))
    for i in range(seg):
        for j in range(cross):
            fs.append((i*cross+j,((i+1)%seg)*cross+j,((i+1)%seg)*cross+(j+1)%cross,i*cross+(j+1)%cross))
    ob=mesh("Buoyant red and cream life ring",vs,fs,"DA4535",True)
    ob.data.materials.append(paint("FBE7BC"))
    for i,f in enumerate(ob.data.polygons):
        angle=(i//cross+.5)*360/seg
        f.material_index=int(55 <= angle < 100 or 145 <= angle < 188 or 242 <= angle < 305)
    ob.data.flip_normals()


def rescue(fur, pose):
    from reference_finish import fuse
    tail_part=tail("Tail growing from right hip",[(918,957,65,12,13),(977,951,64,57,51),
         (1040,911,59,60,55),(1083,856,58,57,49),(1100,795,54,48,41),
         (1096,739,49,35,34),(1076,693,46,24,24),(1054,673,45,14,14),
         (1049,671,45,4,4)],fur)
    body=posed_torso([(625,474),(776,482),(824,559),(925,632),(1005,726),(1047,859),(1025,967),
        (975,1034),(884,1061),(737,1050),(646,1028),(606,964),(597,849),(561,750),(550,642),(580,543)],fur)
    fit_chest(body,pose)
    hip=cushion("Seated hip",oval(997,926,99,134,-8),41,91,77,fur)
    tagged(fuse([hip.name,tail_part.name],"Right hip and continuous wagging tail",voxel=.015))
    tagged(cushion("Far tucked thigh",oval(645,931,57,92),53,62,53,fur))
    puppy_paw("Far grounded hind paw",(620,1014,-9),72,42,45,-3,fur)
    puppy_paw("Near grounded hind paw",(952,1034,-35),89,43,51,-3,fur)
    # The belly strap wraps under the rib cage behind the near foreleg.
    cushion("Navy strap under the belly",[(866,823),(924,846),(920,900),(887,956),(838,989),
        (781,1000),(753,988),(757,962),(804,947),(835,906)],-52,22,18,NAVY)
    # The side panel follows the arched top of the leg opening and turns under the flank.
    vest=sliced("Padded side panel with open foreleg arch",[(685,735),(716,656),(761,603),
        (805,587),(865,599),(945,629),(1007,674),(1043,734),(1053,791),(1049,811),
        (1008,842),(945,863),(876,866),(839,850),(826,835),(835,786),(829,757),
        (810,731),(784,719),(752,721),(726,737),(711,756),(700,788),(686,792)],
        -33,100,82,"F47F33",axis=0)
    for outline in [[(843,622),(882,634),(914,661),(937,722),(937,787),(923,840),(904,866),
                     (860,877),(875,831),(888,765),(884,703),(866,651)],
                    [(955,647),(986,665),(1016,707),(1030,751),(1034,801),(1028,831),
                     (990,850),(997,810),(997,756),(987,704)]]:
        patch("Reflective tailored ivory stripe",vest,outline,"FFEAC3",1.2,1)
    surface_line("Flotation jacket lower binding",vest,[(832,835),(862,853),(920,856),(980,843),(1023,824),(1047,799)],
                 14,"EC762A",[.45,1,1,1,1,.65],6)
    line("Jacket carry handle",[(836,605,22),(851,573,31),(873,560,31),(914,571,29),
         (945,594,23),(954,629,16)],18,NAVY,[1,1,.96,1,1,1])
    line("Rolled jacket shoulder edge",[(809,593,-7),(865,607,-19),(932,627,-21),(987,662,-12)],
         13,"E76F30",[.6,1,1,.6])
    float_ring(306,886,221,224,58)
    puppy_paw("Near supporting foreleg and paw",(708,1042,-144),77,45,52,2,fur,
        [(775,723,-58,24,29),(774,752,-75,52,53),(765,811,-95,58,57),
         (749,881,-106,55,52),(729,960,-115,52,50),(715,1025,-130,53,48)])
    # This lip stands in front of the shoulder and returns behind the arm under load.
    line("Rolled armhole binding",[(694,792,-116),(705,758,-128),(728,730,-127),
         (759,715,-123),(790,718,-116),(814,735,-105),(832,768,-94),(833,801,-81),
         (825,834,-66)],6.5,"EA7328",[.3,.8,1,1,1,1,1,.8,.35])
    cushion("Chest bridge between the two armholes",[(548,695),(588,703),(654,711),(707,715),
        (720,733),(706,776),(691,791),(639,788),(578,781),(551,769),(540,738)],-110,30,24,"F67E34")
    line("Near flotation lapel",[(816,585,-20),(782,601,-65),(746,629,-96),(718,671,-119),(701,713,-134)],
         20,"F1843D",[.6,1.06,1.06,.91,.55])
    line("Far flotation lapel",[(585,593,-12),(566,624,-61),(553,671,-87),(557,707,-100)],
         14,"EF7A32",[.4,1,1,.65])
    # The lead leaves the left corner of the native smile, beside the tongue.
    rope=[(643,494,-181),(623,510,-196),(579,548,-178),(527,581,-162),
          (464,599,-150),(401,605,-137),(358,616,-136),(332,641,-134),(319,658,-135)]
    rescue_rope(rope,12,"Lead between muzzle and life ring")
    for off in (-7,7):
        line("Line secured around ring",[(321+off,650,-142),(315+off,681,-168),(316+off,733,-188),
             (307+off,776,-167)],10,NAVY,[.9,1,1,.68])
    ellipsoid("Hand-tied rescue knot",319,654,-162,39,18,22,NAVY,-5)


def hat_badge(center,w,h,tilt):
    x,y,d=center
    before=set(bpy.context.scene.objects)
    envelope=box("Golden envelope badge",point(x,y,d),(w/UNIT,9/UNIT,h/UNIT),"F6D298",.025)
    line("Postal badge fold",[(x-w*.43,y-h*.32,d-5),(x,y+h*.14,d-6),(x+w*.43,y-h*.32,d-5)],2.7,"BB914F",[.35,1,.35])
    pose(set(bpy.context.scene.objects)-before,point(x,y,d),angles=(0,tilt,0))
    return envelope


def planted_postal_leg(name, joints, paw, fur):
    # The soles share one projected ground plane. The far paws appear a little higher.
    x,d,width,height,depth=paw
    floor=1059-(d+132)*.26
    ankle_x,_,ankle_d,ankle_w,ankle_depth=joints[-1]
    rows=joints+[
        (ankle_x,floor-height*1.25,ankle_d,ankle_w,ankle_depth),
        (x+2,floor-height*.77,d+5,width*.84,depth*.83),
        (x,floor-height*.46,d,width,depth),
        (x,floor-height*.20,d-2,width*.97,depth*.96),
        (x,floor-3,d,width*.81,depth*.8),(x,floor,d,width*.64,depth*.59)]
    centers=smooth([r[:3] for r in rows],5,False)
    sizes=smooth([r[3:] for r in rows],5,False)
    vertices=[];faces=[];segments=72
    for center,size in zip(centers,sizes):
        cx,cy,cd=center
        toe=max(0,min(1,(cy-floor+height*.98)/(height*.65)))
        for k in range(segments):
            a=TAU*k/segments
            u,v=math.cos(a),math.sin(a)
            clefts=sum(math.exp(-((u-gap)/.092)**2) for gap in (-.53,0,.53))
            front=max(0,-v)
            vertices.append(point(cx+size.x*u,cy-toe*(4*clefts+3*abs(u)**3)*front,
                cd+size.y*v+depth*.28*toe*clefts*front))
    for row in range(len(centers)-1):
        for k in range(segments):
            a=row*segments+k;b=row*segments+(k+1)%segments
            faces.append((a,b,b+segments,a+segments))
    faces.extend((tuple(reversed(range(segments))),tuple(range(len(vertices)-segments,len(vertices)))))
    obj=tagged(mesh(name,vertices,faces,fur,True))
    if obj.data.polygons[segments*8+segments*3//4].normal.y>0:obj.data.flip_normals()
    from reference_finish import ground_contact
    footprint=[point(x+a*width*1.38,floor-1+b*height*.15,450)
               for a,b in [(-1,-1),(1,-1),(1,1),(-1,1)]]
    ground_contact(name+" contact",footprint,.24,"Soft paw shadow")
    return obj


def post(fur, pose):
    tail("Raised postal puppy tail",[(809,815,103,2,2),(864,775,93,51,42),(919,717,82,48,42),
        (950,659,74,35,33),(941,611,69,31,29),(925,586,70,19,17),(914,576,74,4,4)],fur)
    body=posed_torso([(477,576),(646,547),(731,614),(811,669),(881,701),(905,773),(865,847),
        (790,869),(748,865),(700,881),(632,908),(561,917),(489,894),
        (432,853),(407,796),(401,737),(420,660)],fur)
    fit_chest(body,pose)
    # Front legs descend from the rib cage. Each rear leg folds through a forward
    # stifle and a rearward hock before the shorter pastern reaches the ground.
    limbs=[
        planted_postal_leg("Far foreleg under chest",[
            (461,643,20,27,34),(440,719,10,45,45),(429,804,14,46,44),
            (432,875,36,37,35),(437,927,45,32,30)],(424,46,49,49,49),fur),
        planted_postal_leg("Near foreleg under shoulder",[
            (563,684,-13,30,37),(568,756,-48,52,51),(568,827,-76,51,47),
            (561,889,-97,43,40),(556,953,-111,37,34)],(542,-132,59,57,57),fur),
        planted_postal_leg("Far rear stifle hock and paw",[
            (758,733,111,32,40),(751,796,141,54,56),(713,853,153,46,49),
            (729,884,157,36,36),(750,912,154,30,29),(746,926,145,28,27)],
            (727,132,43,43,45),fur),
        planted_postal_leg("Near rear stifle hock and paw",[
            (818,734,16,35,42),(813,802,2,62,60),(781,857,-9,53,53),
            (797,889,-9,43,41),(828,932,-7,35,34),(824,957,-17,30,29)],
            (803,-42,52,49,50),fur)]
    from reference_finish import fuse
    standing=tagged(fuse([body.name,*[leg.name for leg in limbs]],"Standing postal puppy on four paws",.013))
    fur_surface=standing.data.materials[0].copy()
    fur_surface.name="Postal puppy short body fur"
    standing.data.materials[0]=fur_surface
    next(n for n in fur_surface.node_tree.nodes if n.type=="BSDF_PRINCIPLED").inputs["Roughness"].default_value=.69
    line("Adjustable honey leather strap",[(464,598,-63),(471,648,-93),(515,706,-122),(577,756,-135),
         (677,790,-142),(777,849,-147),(833,872,-159)],20,"D49839",[.55,1,1,1,1,1,.75])
    cushion("Blue post satchel body",[(815,739),(903,718),(976,737),(1004,789),(1024,898),
        (1011,936),(942,969),(848,983),(796,963),(777,927),(775,839)],-75,61,65,"537EB7")
    cushion("Deep satchel opening",[(804,786),(856,765),(948,753),(991,765),(993,808),(956,846),(858,852)],-122,27,14,"303E65")
    for x,y,color,tilt in [(969,804,"ABD4A1",10),(930,817,"F1A3B1",-15)]:
        envelope=hat_badge((x,y,-158),77,63,tilt)
        envelope.data.materials[0]=paint(color)
    cushion("Curved satchel flap",[(796,773),(834,746),(944,723),(977,737),(1004,765),(1012,789),
        (1005,803),(983,790),(951,793),(899,810),(856,831),(834,827)],-151,22,20,"668AC2")
    before_cap = set(bpy.context.scene.objects)
    crown=cushion("Tilted postman's blue cap",[(331,288),(357,235),(408,180),(484,141),(575,122),
        (621,133),(645,161),(648,207),(635,230),(549,260),(425,298),(367,314)],-20,74,54,"507AB7")
    cushion("Curved blue cap peak",[(394,307),(416,279),(475,249),(565,220),(632,216),
        (673,229),(684,248),(679,262),(657,257),(613,254),(539,268),(463,300),(418,324)],
          -81,22,29,"406CAE")
    hat_badge((520,190,-98),113,67,-15)
    for obj in set(bpy.context.scene.objects) - before_cap:
        obj.location.y -= 125 / UNIT
    # Large letter: its upper-left corner is caught directly under the mouth.
    outline=[(556,561),(579,556),(857,637),(871,654),(799,877),(783,894),(499,790),(486,777)]
    letter=cushion("Yellow letter held in the mouth",outline,-166,18,12,"F7CC69",steps=3)
    patch("Overlapping envelope flap",letter,[(562,564),(856,643),(850,653),(685,707),(660,711),(641,698)],"FFDD83",4,2)
    surface_line("Fold shadow along letter flap",letter,[(559,568),(616,659),(651,703),
        (668,713),(690,708),(769,677),(853,646)],2.1,"D99742",[.32,.8,1,1,1,.8,.3],4.7)
    for x,y,r,rz in [(744,809,41,32),(707,772,15,20),(738,745,16,22),(774,748,16,21),(798,779,15,18)]:
        patch("Ochre paw-print postal stamp",letter,oval(x,y,r,rz),"B88241",.5,2)


def welly(name,cx,cy,d,width,worn=False):
    # A single continuous upper wraps from the front toe into the open boot shaft.
    w=width
    def ringrow(dx,dy,off,r,rd,tilt):return (cx+dx*w,cy+dy*w,d+off*w,r*w,rd*w,tilt*w)
    rows=[(.15,.0,-.10,.02,.02,.0),(.17,-.02,-.22,1.04,.91,.20),(.17,-.11,-.22,1.08,.93,.21),
          (.15,-.25,-.23,1.06,.95,.25),(.09,-.46,-.15,.98,.87,.25),
          (-.04,-.68,-.03,.79,.73,.20),(-.13,-.94,.06,.75,.65,.20),
          (-.15,-1.37,.09,.79,.67,.25),(-.15,-1.54,.10,.82,.68,.26),
          (-.15,-1.63,.10,.80,.66,.26),(-.15,-1.65,.10,.69,.56,.21),
          (-.15,-1.58,.10,.66,.53,.20),(-.15,-1.15,.10,.61,.48,.16),
          (-.10,-.65,.10,.53,.39,.12),(-.09,-.56,.10,.01,.01,.0)]
    boot=turned(name,[ringrow(*r) for r in rows],"F6BA23")
    boot.data.materials.append(paint("A16E24"))
    for p in boot.data.polygons:
        if 10<=p.index//80<=12:p.material_index=1
    turned(name+" honey sole",[ringrow(*r) for r in [(.17,-.05,-.23,1.04,.91,.19),
           (.17,-.07,-.23,1.075,.94,.21),(.17,-.15,-.23,1.08,.94,.21),
           (.17,-.17,-.23,1.04,.92,.21)]],"E7A824")
    return boot


def raincoat(fur, _pose):
    teal="18AEB8"; highlight="48CFD2"
    # The near puddle lip and the muddy footprint both remain below the boot soles.
    water=cushion("Shallow sky-blue rain puddle",[(166,1067),(212,1004),(373,961),(530,947),
        (759,945),(956,950),(1097,983),(1148,1024),(1155,1060),(1106,1088),(1069,1102),
        (1040,1144),(897,1169),(688,1189),(468,1185),(283,1161),(180,1123),(155,1100)],
        125,19,9,paint("64BFF1",.19),center=(660,1058))
    for x,y,rx,ry in [(275,1060,170,66),(282,1055,130,45)]:
        line("Low ripple around the empty boot",[(x+rx*math.cos(a),y+ry*math.sin(a),89)
             for a in [i*TAU/50 for i in range(50)]],3,"CCEAF1",cyclic=True)
    mud=cushion("Muddy print under the yellow boot",[(75,1063),(93,1034),(124,1019),(122,1007),
        (167,995),(229,1012),(319,1027),(369,1043),(393,1061),(367,1081),(310,1093),(190,1095),
        (115,1086)],70,18,8,"A5785E")
    tail("Tail rising from the right hip",[(841,906,79,2,2),(906,859,74,50,49),(955,793,68,57,54),
         (979,731,56,42,40),(970,678,51,32,28),(948,629,45,25,24),(938,616,46,15,14),(930,610,48,3,3)],fur)
    posed_torso([(527,479),(696,466),(827,550),(908,679),(960,831),(939,939),
        (854,984),(706,976),(558,977),(463,944),(431,853),(438,730),(462,581)],fur)
    for x in (555,895):
        leg("Bent hind leg and grounded paw",[(x-80,841),(x-53,785),(x+25,786),(x+77,839),(x+68,904),
            (x+84,943),(x+82,973),(x+61,992),(x+12,1002),(x-34,1002),(x-72,993),
            (x-77,967),(x-69,935),(x-82,903)],33,68,fur,[(x-25,935,-8,48),(x+20,934,2,53)])
    coat=cushion("Flared tailored raincoat body",[(560,507),(743,494),(819,533),(864,593),(906,670),
        (965,754),(998,830),(1003,855),(982,877),(921,886),(862,877),(807,875),
        (744,891),(666,902),(611,900),(525,875),(447,858),(404,877),(400,844),
        (425,774),(460,706),(482,622),(493,551)],-18,120,91,teal,center=(690,712))
    for s,pts in [(1,[(678,610),(677,672),(677,761),(671,855),(673,890)]),
                  (-1,[(435,842),(478,844),(538,861),(598,879),(657,884)]),
                  (1,[(865,864),(918,851),(967,844),(992,855)])]:
        surface_line("Rounded raincoat seam",coat,pts,7,highlight,[.35]+[1]*(len(pts)-2)+[.35],3)
    hood=cushion("Open folded hood behind the head",[(446,560),(448,506),(495,470),(558,455),(706,447),
        (814,452),(878,488),(900,545),(890,596),(865,640),(830,607),(768,568),(710,592),
        (675,611),(621,584),(536,548),(487,584),(458,606)],-28,62,86,"27B6BF")
    patch("Dark lining inside hood",hood,[(480,522),(503,487),(563,465),(721,454),(803,465),
         (851,487),(868,514),(858,535),(816,544),(771,549),(716,570),(676,602),
         (649,581),(570,545),(510,534)],"197E8B",3,2,center=(670,513))
    right=cushion("Right raincoat sleeve",[(801,553),(857,575),(883,634),(897,723),(905,792),
         (891,831),(848,849),(791,842),(740,821),(731,807),(746,744),(749,664),(766,595)],
         -107,71,62,teal)
    cuff=cushion("Rolled right sleeve cuff",[(735,795),(763,766),(818,757),(872,766),(896,789),
        (901,816),(877,839),(812,847),(763,841),(734,826)],-144,30,24,highlight)
    patch("Right cuff interior",cuff,oval(815,821,65,19),"1C7780",1,1)
    leg("Foreleg inside the worn wellington",[(764,814),(796,800),(842,807),(868,819),(865,873),
        (850,917),(811,930),(769,902)],-162,38,fur,[])
    left=cushion("Bent sleeve holding the raised paw",[(500,565),(553,575),(585,610),(594,659),
        (562,714),(510,750),(458,754),(416,737),(392,707),(398,661),(426,608)],
        -114,66,58,teal)
    cuff=cushion("Raised sleeve cuff",oval(511,667,75,72,-30),-156,22,16,highlight)
    patch("Raised sleeve lining",cuff,oval(510,661,58,59,-30),"22828A",1.5,1)
    paw=leg("Bare paw lifted out of the sleeve",[(449,673),(444,636),(454,582),(475,543),
        (499,529),(515,533),(532,528),(550,537),(553,547),(571,548),(584,565),
        (585,582),(593,594),(583,616),(567,632),(551,656),(536,687),(508,711),(479,708)],
         -175,61,fur,[(519,541,-27,35),(550,551,-26,37),(575,573,-23,34)])
    for x,y,rx,ry in [(486,566,14,21),(517,546,14,20),(549,550,13,18)]:
        bump=ellipsoid("Bent knuckle on lifted paw",x,y,depth_at(paw,x,y,-194)+4,rx,ry,9,fur,-28)
        tagged(bump)
    for x,y in [(559,603),(567,595),(550,613),(556,620),(575,588)]:
        patch("Mud speck on the raised paw",paw,oval(x,y,4,3),"AD784F",.5,1)
    for y in (663,757):
        d=depth_at(coat,676,y,-128)-10
        for x in (648,668,689):
            box("Buttercup-yellow toggle",point(x,y,d),(.145,.115,.197),"FFC42F",.052)
    for t,x,y,r in [(coat,544,810,12),(right,821,662,13),(right,857,718,8),(left,414,691,9)]:
        patch("Raised drop of rain",t,[(x,y-r*1.7),(x+r*.64,y-r*.20),(x+r,y+r*.50),
              (x+r*.35,y+r),(x-r*.63,y+r*.89),(x-r*.95,y+r*.30)],"ADE8EF",5,2)
    welly("Worn yellow rubber boot",803,1037,-164,89,True)
    welly("Loose muddy yellow wellington",247,1051,-91,99)


DOGS = {
    "dog-model-branch": partial(wardrobe_dog, branch, (628,634,1070), (630,571,3), 2.72, 0),
    "dog-model-rescue": partial(wardrobe_dog, rescue, (615,623,1150), (673,496,8), 3.22, 4),
    "dog-model-post": partial(wardrobe_dog, post, (625,599,1055), (581,551,12), 3.04, -8),
    "dog-extra-raincoat": partial(wardrobe_dog, raincoat, (612,632,1195), (663,493,-12), 3.35, -10),
}

if __name__=="__main__":
    for item in sys.argv[sys.argv.index("--")+1:] or DOGS:
        DOGS[item]()
        save_render(item)
