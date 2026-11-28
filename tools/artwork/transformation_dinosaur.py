"""Four editable dinosaur sculpts with individually calibrated dinosaur and prop silhouettes."""
import math
from pathlib import Path
import sys

import bpy
from mathutils import Matrix
sys.path.insert(0,str(Path(__file__).resolve().parent))
from fantasy_sculpt import *
from fossil_scout import fossil, sculpt_mass
from dinosaur_pigments import SKIN, SPOT, PLATE, BELLY, CLAW, CHEEK
from reference_finish import ground_contact


def animal_head(shape,center,depth,bulge,cheeks,eyes,brows,nostrils,mouth,tongue=None):
    nose=(shape[-2][0],shape[-2][1],50,36)
    h=face("Sculpted dinosaur skull and rounded muzzle",shape,center,depth,bulge,SKIN,nose,cheeks,SKIN,CHEEK)
    for i,(bounds,lash,iris,pupil,glint) in enumerate(eyes):
        eye(("Near" if i==0 else "Far")+" curious dinosaur eye",h,bounds,lash,iris,pupil,glint,"765135","322821")
    for points,w in brows:
        stroke("Gentle green brow",h,points,w,"4A713C",[.25,1,.25],.80)
    for x,y,r in nostrils:
        patch("Recessed nostril",h,x,y,r*.72,r,"689948")
    if tongue:
        o=pillow("Open dinosaur smile",mouth,-40,.75,"42282C",front=lambda x,y:surface(h,x,y)-.85,rings=14)
        pillow("Peach smiling tongue",tongue,-40,.50,"EB8A86",front=lambda x,y:surface(o,x,y)-.50,rings=9)
    else:
        stroke("Curving dinosaur smile",h,mouth,1.6,"455333",[.28,.83,1,.72,.16],1.0)
    return h


def plate(cx,base,h,w,depth,c,angle=0,name="Rounded dorsal plate"):
    pts=[(-.82,.03),(-1,-.26),(-.79,-.70),(-.27,-.99),(.15,-1.04),(.76,-.67),(1,-.29),(.73,.03)]
    ca,sa=math.cos(angle),math.sin(angle)
    anchors=[(cx+x*w*ca-y*h*sa,base+x*w*sa+y*h*ca) for x,y in pts]
    pillow(name,anchors,depth,min(w*.47,15),c)


def ridge_list(data,c=PLATE):
    for x,y,h,w,d,angle in data:plate(x,y,h,w,d,c,angle)


def spot(name,body,x,y,rx,ry,angle=0,c=SPOT):
    ca,sa=math.cos(angle),math.sin(angle)
    anchor=[]
    for i in range(12):
        a=math.tau*i/12
        dx=rx*math.cos(a);dy=ry*math.sin(a)
        anchor.append((x+dx*ca-dy*sa,y+dx*sa+dy*ca))
    return pillow(name,anchor,-24,.38,c,(x,y),front=lambda xx,yy:surface(body,xx,yy)-.38,rings=8)


def padded_leg(name,knots,d,sole):
    x,y,w,h,th=sole
    fx,fy,fr=knots[-1]
    body_knots=knots[:-1]+[(fx,fy,fr*.53),(x,y,.9)] if math.hypot(fx-x,fy-y)>1 else knots
    leg=limb(name,body_knots,d,.82,SKIN,True)
    pad=ell("Rounded sole",x,y,d-2,w,h,th,SKIN,True)
    return mark_skin(fuse([leg.name,pad.name],name+" and foot",.014))

def toes(data):
    for x,y,d,rx,ry in data:ell("Ivory rounded claw",x,y,d,rx,ry,rx*.80,CLAW)


def foot(name,knots,d,claws):
    limb(name,knots,d,.80,SKIN,True)
    toes(claws)


def egg_surface(name):
    n=112;rows=36
    vs=[];fs=[]
    def pos(i,t):
        u=math.tau*i/n
        # Unequal shard widths and heights continue around the back of the egg.
        fracture=[(0,.26),(6,.53),(13,.20),(19,.42),(25,.18),(32,.48),(37,.31),(42,.55),
                  (50,.17),(57,.43),(64,.28),(69,.53),(75,.20),
                  (82,.305),(89,.245),(94,.335),(98,.29),(103,.27),(108,.42),(112,.26)]
        edge=float(np.interp(i%n,*zip(*fracture)))
        v=-math.pi/2+.005+(edge+math.pi/2-.005)*t
        x=303+156*math.cos(v)*math.cos(u);dep=4+91*math.cos(v)*math.sin(u)
        y=379-167*math.sin(v)-dep*.18
        return x,y,dep
    for j in range(rows+1):
        for i in range(n):vs.append(world(*pos(i,j/rows)))
    for j in range(rows):
        for i in range(n):
            x,y,d=pos(i+.5,(j+.5)/rows)
            if d<5 and x<204+7*math.sin(y*.14) and 383+9*math.cos(x*.087)<y<465+7*math.sin(x*.10):
                continue
            fs.append((j*n+i,j*n+(i+1)%n,(j+1)*n+(i+1)%n,(j+1)*n+i))
    fs.append(tuple(reversed(range(n))))
    o=mesh(name,vs,fs,"FFF0CE")
    solid=o.modifiers.new("Visible thickness at every broken edge","SOLIDIFY");solid.thickness=.067
    o.data.materials.append(mat("E7C59F"))
    solid.material_offset=1
    solid.material_offset_rim=1
    bevel=o.modifiers.new("Gently chipped edges","BEVEL");bevel.width=.021;bevel.segments=3
    for x,y,rx,ry,a in [(282,422,29,40,.75),(431,400,15,31,.37),(357,490,23,34,.99)]:
        spot("Turquoise shell markings",o,x,y,rx,ry,a,"41BFBA")
    return o


def shell_cap_glaze():
    # Project the two pigments onto the curved exterior. Their edges stay smooth at any mesh density.
    material=mat("F7DEB7",.58).copy()
    material.name="Ivory shell with two turquoise spots"
    tree=material.node_tree
    coords=tree.nodes.new("ShaderNodeTexCoord")
    distances=[]
    for x,y,rx,ry,angle in [(319.5,40.8,23.8,13.1,.035),(263,73.6,15,6.2,.902)]:
        offset=tree.nodes.new("ShaderNodeVectorMath");offset.operation="SUBTRACT"
        tree.links.new(coords.outputs["Object"],offset.inputs[0])
        offset.inputs[1].default_value=world(x,y)
        rotate=tree.nodes.new("ShaderNodeVectorRotate");rotate.rotation_type="Y_AXIS"
        tree.links.new(offset.outputs[0],rotate.inputs["Vector"])
        rotate.inputs["Angle"].default_value=angle
        scale=tree.nodes.new("ShaderNodeVectorMath");scale.operation="MULTIPLY"
        tree.links.new(rotate.outputs[0],scale.inputs[0])
        scale.inputs[1].default_value=(100/rx,0,100/ry)
        distance=tree.nodes.new("ShaderNodeVectorMath");distance.operation="LENGTH"
        tree.links.new(scale.outputs[0],distance.inputs[0])
        distances.append(distance.outputs["Value"])
    nearest=tree.nodes.new("ShaderNodeMath");nearest.operation="MINIMUM"
    for i,distance in enumerate(distances):tree.links.new(distance,nearest.inputs[i])
    ramp=tree.nodes.new("ShaderNodeValToRGB")
    # Half-scale the distances so both sides of the edge fit the colour ramp.
    scale=tree.nodes.new("ShaderNodeMath");scale.operation="MULTIPLY";scale.inputs[1].default_value=.5
    tree.links.new(nearest.outputs[0],scale.inputs[0]);tree.links.new(scale.outputs[0],ramp.inputs[0])
    teal,cream=ramp.color_ramp.elements
    teal.position=.494;teal.color=rgba("49C4BA")
    cream.position=.506;cream.color=rgba("F7DEB7")
    shader=next(node for node in tree.nodes if node.type=="BSDF_PRINCIPLED")
    tree.links.new(ramp.outputs[0],shader.inputs["Base Color"])
    return material


def hatchling_shell_cap():
    # The photographed crown and fracture are independent contours of one hollow shard.
    crest=spline([(233,107),(237,92),(248,70),(263,52),(278,40),(296,31),
                  (313,27.8),(329,29.8),(344,36),(357,45),(369,56),(374,63)],8)
    lip=spline([(233,107.5),(237,108),(244,101),(255,100.5),(261,98),
                (272,85),(281,77),(292,77.5),(315,81),(321,78.5),
                (338,57),(344,55.5),(357,57.5),(371,63),(374,63.5)],6)
    vertices=[];faces=[];materials=[]
    steps=64
    for x in np.linspace(233,374,169):
        crown=np.interp(x,*crest.T)
        front=sum(weight*np.interp(x+offset,*lip.T) for offset,weight in
                  [(-3,.09),(-1.5,.23),(0,.36),(1.5,.23),(3,.09)])
        front+=4.7*math.exp(-((x-242)/11)**2)
        span=max(0,math.sin(math.pi*(x-233)/141))**.55
        depth=2+60*span
        thickness=4.2+4.3*span
        seat=math.exp(-((x-315)/37)**4)
        arch=[]
        for j in range(steps+1):
            a=math.pi*j/steps
            t=1-math.sin(a)
            d=-9-depth*math.cos(a)
            if j<=steps//2:
                round_rise=np.interp(x,[233,374],[107,63])-crown
                y=crown+round_rise*t+(front-crown-round_rise)*t**2.3
            else:
                # The rear of the shard cups the scalp behind the visible zigzag.
                y=crown+(3+29*span)*t+39*seat*t**1.5
                d-=38*seat*t*t
            arch.append((y,d))
        section=arch+[(arch[-1][0]+thickness*.48,arch[-1][1]+.55)]
        section += [(y+thickness,d+2.0*math.cos(math.pi*j/steps))
                    for j,(y,d) in reversed(list(enumerate(arch)))]
        section += [(front+thickness*.79,-9-depth-.25),(front+thickness*.24,-9-depth-.7)]
        vertices.extend(world(x,y,d) for y,d in section)
    count=len(section)
    for start in range(0,len(vertices)-count,count):
        for j in range(count):
            k=(j+1)%count
            faces.append((start+j,start+count+j,start+count+k,start+k))
            materials.append(0 if j<steps else 2 if steps+2<=j<count-3 else 1)
    faces.extend([tuple(range(count)),tuple(reversed(range(len(vertices)-count,len(vertices))))])
    cap=mesh("Tilted crown shard with a broken ivory lip",vertices,faces,shell_cap_glaze())
    cap.data.materials.append(mat("DBB887",.65))
    cap.data.materials.append(mat("C8A879",.7))
    cap.data.polygons.foreach_set("material_index",materials+[1,1])
    return cap


def hatchling_paw(name, shoulder, contour, palm, depth, clefts):
    path=spline(shoulder,7)
    centers=[Vector(world(*p[:3])) for p in path]
    vertices=[];faces=[];sides=40
    for i,center in enumerate(centers):
        tangent=(centers[min(i+1,len(path)-1)]-centers[max(0,i-1)]).normalized()
        across=Vector((1,0,0))
        across=(across-tangent*tangent.dot(across)).normalized()
        normal=tangent.cross(across)
        for j in range(sides):
            a=math.tau*j/sides
            vertices.append(center+(across*math.cos(a)+normal*(.88*math.sin(a)))*float(path[i,3]/100))
    for j in range(len(path)-1):
        for k in range(sides):
            a=j*sides+k;b=j*sides+(k+1)%sides
            faces.append((a,b,b+sides,a+sides))
    faces.extend((tuple(reversed(range(sides))),tuple(range(len(vertices)-sides,len(vertices)))))
    arm=mesh(name+" curved forearm",vertices,faces,SKIN)
    def grooves(x,y):
        return sum(5.5*softstep(start,start+16,y)*math.exp(-((x-cx-slope*(y-start))/2.65)**2)
                   for cx,start,slope in clefts)
    hand=pillow(name+" three curled digits",contour,depth,19,SKIN,palm,
                front=lambda x,y:depth-.31*(y-palm[1]),extra=grooves,rings=52)
    return [arm.name,hand.name]


def egg():
    begin("dinosaur")
    ridge_list([(261,337,55,32,16,-.88),(273,275,50,35,16,-1.14),(266,226,56,34,16,-1.29),
                (267,163,67,37,17,-.91),(274,99,54,29,16,-.37)])
    body=pillow("Hatchling body",[(228,360),(228,311),(250,275),(276,238),(294,220),(338,228),
                 (365,249),(379,271),(397,303),(414,347),(386,389),(266,392)],4,36,SKIN,(314,308),coat=True)
    pillow("Soft yellow belly",[(310,345),(311,305),(328,278),(350,270),(372,277),(388,301),
              (395,340),(386,374),(335,378)],-18,21,BELLY,(353,323))
    for x,y,rx,ry,a in [(278,273,5,9,.72),(258,297,6,13,.44),(245,330,5,12,.20)]:
        spot("Hatchling flank markings",body,x,y,rx,ry,a)
    tail=limb("Tail escaping the broken egg",[(235,413,29),(190,431,33),(146,434,28),(113,421,23),
              (92,397,17),(86,377,8),(85,361,.55)],-34,.77,SKIN,True)
    ridge_list([(106,383,30,12,-19,.18),(138,404,29,14,-19,.30),(168,409,21,12,-20,.18)])
    for x,y,r in [(111,405,7),(138,420,10),(170,422,7)]:spot("Hatchling tail spot",tail,x,y,r,r*.86)
    egg_surface("Hollow broken eggshell")
    near_clefts=[(308,335,.29),(327,333,.16)]
    far_clefts=[(401,332,-.19),(416,334,-.19)]
    near=hatchling_paw("Near forepaw",[(293,281,2,5),(293,299,-2,10),
        (291,316,-18,13),(289,326,-45,14),(296,330,-71,15),(302,332,-84,12)],
        [(271,327),(275,317),(286,309),(297,307),(312,310),(327,319),(336,331),
         (340,344),(338,353),(333,359),(329,359),(325,362),(320,364),(315,362.5),
         (310,364),(304,362),(299,356),(295,348),(291,345),(280,345),(273,342)],
        (304,333),-88,near_clefts)
    far=hatchling_paw("Far forepaw",[(371,276,8,5),(382,295,5,10),
        (391,309,-13,13),(397,322,-37,13),(401,329,-62,13),(403,332,-71,9)],
        [(378,332),(381,319),(388,311),(398,307),(410,309),(420,317),
         (426,329),(426,340),(422,351),(416,357),(412,357),(407,359.5),
         (402,359),(397.5,356),(393,356.5),(387,355),(383,350),(380,342)],
        (403,333),-73,far_clefts)
    body=mark_skin(fuse([body.name,*near,*far],"Hatchling torso and gripping forepaws",.009))
    paw_material=mat(SKIN,.64).copy()
    paw_material.name="Hatchling knuckles and curled fingertips"
    body.data.materials.append(paw_material)
    pigment=body.data.color_attributes.new(name="Forepaw pigment",type="FLOAT_COLOR",domain="POINT")
    base=np.array(rgba(SKIN)[:3])
    values=[]
    for vertex in body.data.vertices:
        px,depth,pz=vertex.co
        x,y=300+100*px,600-100*pz
        shading=.17*softstep(318,361,y)
        for cx,start,slope in (near_clefts if x<355 else far_clefts):
            dist=(x-cx-slope*(y-start))/2.65
            shading+=.17*softstep(start,start+15,y)*math.exp(-dist*dist)
        weight=shading*softstep(42,74,-100*depth)
        values.extend((*base*(1-weight),1))
    pigment.data.foreach_set("color",values)
    tree=paw_material.node_tree
    pigment_node=tree.nodes.new("ShaderNodeVertexColor");pigment_node.layer_name=pigment.name
    shader=next(n for n in tree.nodes if n.type=="BSDF_PRINCIPLED")
    tree.links.new(pigment_node.outputs["Color"],shader.inputs["Base Color"])
    for face in body.data.polygons:
        if face.center.y<-.41:
            face.material_index=1
    animal_head([(242,146),(249,111),(272,83),(311,69),(349,68),(376,80),(398,106),(412,147),
                 (444,144),(462,151),(473,169),(473,189),(463,211),(444,231),(410,247),(372,254),
                 (331,255),(286,244),(260,225),(245,198)],(339,165),-12,45,
                [(292,210,17,13)],
                [([(278,168),(280,151),(289,140),(301,138),(313,146),(321,163),(323,180),(317,191),(302,194),(289,188)],
                  [(278,169),(280,152),(288,141),(299,139),(310,144),(317,154)],
                  (294,179,14,19),(291,178,9,12),(288,168,4.8)),
                 ([(365,144),(365,126),(373,116),(383,114),(391,121),(398,135),(401,151),(390,163),(377,165)],
                  [(365,143),(365,126),(371,116),(381,113),(390,119),(397,132)],
                  (377,150,12,17),(375,148,7.8,10.5),(372,141,3.9))],
                [([(270,130),(279,115),(293,110)],2.6),([(354,90),(365,90),(377,96)],2.5)],
                [(426,176,4.8),(455,163,4.1)],[(344,217),(359,226),(386,229),(419,220),(441,204)])
    hatchling_shell_cap()
    fragment=pillow("Fallen piece of eggshell",[(86,546),(99,532),(122,520),(151,519),(177,532),
                  (178,549),(161,552),(145,555),(128,564),(114,558),(98,554),(87,555)],-34,7,"FFF0CF",(133,540))
    spot("Spot on broken eggshell",fragment,135,531,20,11,.05,"46C6BE")


def stego():
    begin("dinosaur")
    for x,y,h,w,a in [(105,324,17,9,-.32),(123,316,25,12,-.27),(145,306,30,15,-.35),(164,288,43,20,-.38),
          (185,280,55,22,-.43),(218,245,72,33,-.22),(264,228,67,28,-.15),(310,231,94,42,-.01),
          (350,223,59,24,.25),(377,266,76,32,.40),(400,277,45,19,.30),(419,264,39,18,.15),(443,267,24,11,-.04)]:
        plate(x,y+10,h+10,w,17 if x in (264,350,419) else 4,"F59A26" if x in (264,350,419) else "FFAB35",a,"Amber stegosaurus plate")
    tail=limb("Tapered horizontal tail",[(260,331,52),(216,333,46),(169,340,31),(127,342,19),(81,341,11),(54,340,.6)],
              8,.75,SKIN,True)
    trunk=pillow("Arched herbivore body",[(161,325),(183,291),(211,261),(247,235),(282,225),
          (312,226),(343,237),(374,262),(400,288),(427,294),(448,307),(443,343),(410,359),
          (371,380),(326,390),(288,393),(245,385),(202,377),(178,360)],6,43,SKIN,(286,314),coat=True)
    # Green upper flank rolls into the soft cream underside on one continuous sculpt.
    trunk=mark_skin(fuse([tail.name,trunk.name],"Continuous body and tail of the stegosaurus",.016))
    joined=[trunk.name]
    far_hind = sculpt_mass("Far hind leg with bent hock and planted foot", [
        (195,326),(215,339),(212,356),(198,372),(183,388),(168,406),
        (169,412),(181,414),(187,419),(187,424),(179,427),(156,428),
        (147,425),(145,419),(148,407),(155,389),(164,374),(168,360),(175,340)],
        lambda x,y: 27, [(192,347,36,36,28),(171,381,24,35,20),(164,417,25,15,18)], 6)
    for x,y,rx,ry in [(163,423,6.8,5.4),(175,422,6,5.1),(184,420,4.8,4.5)]:
        ell("Ivory rounded claw",x,y,surface(far_hind,x,y)-1,rx,ry,rx*.82,CLAW)
    for knots,d,cs in [
        ([(391,338,13),(402,364,21),(412,391,20),(426,413,24)],16,[(433,421,0,8,8),(447,420,0,7,8)]),
        ([(239,285,.4),(245,306,27),(240,329,40),(232,360,36),(217,391,26),(220,416,31)],-20,
         [(224,426,-43,9,9),(244,425,-41,9,9),(255,423,-36,6,8)]),
        ([(347,319,.4),(350,337,24),(346,360,29),(337,390,24),(343,417,27)],-21,
         [(347,427,-43,9,8),(363,426,-38,8,8),(372,425,-32,5,7)])]:
        x,y,r=knots[-1]
        leg=padded_leg("Rounded weight-bearing leg",knots,d,(x+2,y+2,r*1.12,15,r*.85))
        if d<0: joined.append(leg.name)
        toes(cs)
    trunk=mark_skin(fuse(joined,"Stegosaurus flank flowing into hips and shoulders",.014))
    belly_skin(trunk,lambda x,y,d:softstep(340,355,y)*softstep(393,364,y)*softstep(-30,-16,d))
    for x,y,rx,ry,a in [(234,259,15,8,-.51),(263,264,16,9,-.26),(224,287,15,9,-.53),
                         (194,297,13,7,-.76)]:spot("Dark green flank oval",trunk,x,y,rx,ry,a)
    for a,b,r in [((86,329,9),(55,313,15),6),((97,321,13),(78,302,16),7),
                  ((84,343,-8),(58,360,-18),6),((105,343,-8),(88,368,-19),7)]:
        cylinder("Cream tail spike",a,b,r,CLAW,1.4)
    existing = set(bpy.context.scene.objects)
    animal_head([(412,294),(420,270),(437,255),(458,249),(480,253),(495,263),(509,282),(513,301),
                 (531,297),(542,302),(548,315),(543,334),(529,349),(506,360),(477,363),(450,357),
                 (428,345),(416,330)],(469,309),-13,29,[(448,330,13,9)],
                [([(442,304),(444,293),(449,287),(455,287),(463,293),(468,303),(469,313),(465,318),(453,318),(445,314)],
                   [(442,303),(443,294),(449,287),(456,287),(461,292)],(459,303,9.4,13.3),(459,301,6,8.5),(456,295,3.2)),
                 ([(499,293),(498,284),(500,278),(504,279),(509,286),(512,298),(509,303),(504,303)],
                  [(497,287),(498,280),(501,277),(505,280),(509,287)],(505,293,5.6,10),(505,291,3.4,6.4),(504,285,2.3))],
                [([(437,279),(443,270),(454,269)],1.8),([(489,264),(494,263),(499,267)],1.15)],
                [(524,314,3.0),(539,310,2.4)],[(477,336),(487,341),(502,345),(518,343),(533,333)])

    # Enlarge the head around its attachment to the neck, including every facial feature.
    pivot = Vector(world(435,322,-13))
    transform = Matrix.Translation(pivot + Vector((-.07,0,.095))) @ Matrix.Scale(1.42,4) @ Matrix.Translation(-pivot)
    bpy.context.view_layer.update()
    for obj in set(bpy.context.scene.objects) - existing:
        obj.matrix_world = transform @ obj.matrix_world


def belly_skin(obj,weight):
    green=rgba(SKIN);cream=rgba(BELLY)
    col=obj.data.color_attributes.new(name="Sculpted skin pigments",type="FLOAT_COLOR",domain="POINT")
    vals=[]
    for v in obj.data.vertices:
        p=obj.matrix_world@v.co
        vals.append(blend(green,cream,weight(300+p.x*100,600-p.z*100,p.y*100)))
    col.data.foreach_set("color",np.array(vals,dtype=np.float32).reshape(-1))
    m=obj.data.materials[0].copy();obj.data.materials[0]=m
    a=m.node_tree.nodes.new("ShaderNodeVertexColor");a.layer_name=col.name
    m.node_tree.links.new(a.outputs["Color"],next(n for n in m.node_tree.nodes if n.type=="BSDF_PRINCIPLED").inputs["Base Color"])


def push_leg():
    # A single silhouette runs from the hip through the extended ankle to the grounded toes.
    leg = sculpt_mass("Pushing leg from hip to planted sole", [
        (211,353),(218,366),(205,386),(184,403),(163,412),(147,428),
        (134,446),(131,451),(133,454),(141,457),(148,463),(149,472),(143,478),
        (129,481),(109,481),(94,480),(86,475),(84,467),(88,450),
        (99,429),(111,409),(125,390),(140,376),(164,361),(187,350)],
        lambda x,y: 15, [(181,374,46,36,32),(145,399,32,32,27),
                         (114,431,28,40,23),(113,466,37,20,26)], 7)
    for x,y,rx,ry in [(110,472,10,10),(127,471,9,10),(141,469,7,9)]:
        depth = surface(leg,x,y)
        ell("Ivory pushing toe",x,y,depth-1,rx,ry,rx*.88,CLAW)
    return leg


def skate_knee_pad(leg, name, x, y, depth, tilt, width, height):
    # Wrap a closed elastic cuff around the actual leg at two cross sections.
    across = Vector((math.cos(tilt),0,-math.sin(tilt)))
    down = Vector((-math.sin(tilt),0,-math.cos(tilt)))
    center = Vector(world(x,y,depth))
    vertices = []
    segments = 72
    bpy.context.view_layer.update()
    for offset in (-6,-4.5,4.5,6):
        origin = center + down*(offset/100)
        for i in range(segments):
            angle = math.tau*i/segments
            normal = across*math.cos(angle) + Vector((0,math.sin(angle),0))
            hit,point,_,_ = leg.ray_cast(origin+normal*1.4,-normal)
            if not hit:
                raise ValueError(f"Unfitted knee strap on {name}")
            vertices.append(point + normal*(.014 if abs(offset)==6 else .028))
    faces = [(j*segments+i,j*segments+(i+1)%segments,(j+1)*segments+(i+1)%segments,
              (j+1)*segments+i) for j in range(3) for i in range(segments)]
    band = mesh(name+" fitted elastic",vertices,faces,"D65354")
    band.modifiers.new("Cuff thickness","SOLIDIFY").thickness=.018
    front = surface(leg,x,y)
    for label,dx,dy,rx,ry,bulge,color in [
        (" bound cushion",0,0,width,height,6.0,"D94E4D"),
        (" rounded cap",.8,-1,width*.85,height*.83,8.5,"F77469")]:
        z = front - (3 if dx==0 else 7)
        o = ell(name+label,x+dx,y+dy,z,rx,ry,bulge,color)
        o.rotation_euler.y = -tilt


def skateboard():
    begin("dinosaur")
    # Four real wheels and trucks are attached below the fish-shaped wooden deck.
    for x in (236,443):
        pillow("Dark steel truck bracket",[(x-21,477),(x+21,477),(x+18,505),(x+12,518),(x-9,518),(x-19,502)],11,11,"776863",(x,495))
        for d in (20,-22):
            o=cylinder("Orange polyurethane wheel",(x,525,d-9),(x,525,d+9),32,"F58E27")
            o.modifiers[0].width=.055
            ell("Recessed amber bearing seat",x,525,d-10,16,16,3,"CF6526")
            ell("Cream wheel bearing",x,525,d-13,11,11,3,"FFE7BF")
            ell("Recessed steel axle end",x,525,d-16,4,4,2,"8B7B6F")
        cylinder("Metal wheel axle",(x,525,-25),(x,525,25),8,"7E6D64")
    silhouette=[(150,474),(159,460),(178,451),(159,446),(155,436),(171,424),(201,421),
                (242,430),(300,421),(386,410),(456,408),(495,415),(518,431),(518,450),
                (496,469),(452,480),(380,487),(301,489),(244,481),(220,480),(177,490),(155,487)]
    outline_pts=outline([(x,y-9) for x,y in silhouette],5)
    n=len(outline_pts);vs=[world(x,y+off,(447-y)*2+d) for off,d in [(0,0),(12,.8)] for x,y in outline_pts]
    fs=[tuple(range(n)),tuple(reversed(range(n,2*n)))]+[(i,(i+1)%n,n+(i+1)%n,n+i) for i in range(n)]
    for width,colour in [(3,"F8DB83"),(7,"BD793C")]:
        line("Maple deck ply",[(x,y+width,(447-y)*2-.8) for x,y in outline_pts],1.15,colour,closed=True)
    deck=mesh("Yellow laminated fish-shaped skateboard deck",vs,fs,GOLD)
    rounding=deck.modifiers.new("Rounded deck edges","BEVEL");rounding.width=.037;rounding.segments=4
    deck.modifiers.new("Deck face normals","WEIGHTED_NORMAL")
    inset=[(327+(x-327)*.953,442+(y-442)*.79) for x,y in outline_pts]
    mesh("Turquoise skateboard grip",[world(x,y,(447-y)*2-.8) for x,y in inset],[tuple(range(n))],"27C4C1")
    ell("White deck eye",470,429,26,21,12,2,"FFF9E9")
    ell("Black deck pupil",476,428,22,13,8,2,"332B2B")
    ell("Deck eye glint",479,425,21,4.5,3,1,"FFFFFF")
    ridge_list([(329,89,48,26,16,-.43),(295,136,57,30,16,-.58),(277,197,47,28,15,-.99),
                (289,252,42,32,16,-.93),(234,284,40,26,15,-.31),(175,293,36,26,15,-.16),(126,290,26,21,15,-.20)])
    trunk=pillow("Streamlined skating torso",[(286,220),(323,196),(360,207),(383,240),(388,276),
          (376,320),(359,357),(330,380),(291,389),(252,391),(218,384),(184,371),(167,344),
          (193,308),(234,282)],7,41,SKIN,(286,312),coat=True)
    tail=limb("Long balancing skate tail",[(265,307,48),(219,325,44),(177,328,36),(135,314,26),
                     (101,295,18),(81,279,13),(69,273,2)],9,.82,SKIN,True)
    body=mark_skin(fuse([trunk.name,tail.name],"Flowing dinosaur back and long tail",.014))
    belly_skin(body,lambda x,y,d:softstep(0,13,y-(392-.48*(x-160)))*softstep(137,193,x))
    for x,y,rx,ry,a in [(154,305,12,8,-.18),(198,305,15,10,-.36),(235,292,13,9,-.55),(267,269,9,6,-.7)]:
        spot("Dinosaur flank spot",body,x,y,rx,ry,a)
    pushing = push_leg()
    ell("Soft flexed thigh",320,353,-13,44,31,28,SKIN,True)
    riding = padded_leg("Bent board leg",[(330,352,6),(342,355,16),(356,361,25),(356,387,23),
                          (347,414,28),(347,443,27),(380,440,20)],-17,(363,446,42,20,31))
    toes([(363,453,-39,14,13),(383,449,-34,11,13),(399,442,-28,9,12)])
    skate_knee_pad(pushing,"Pushing knee",145,399,15,.54,22,25)
    skate_knee_pad(riding,"Riding knee",361,368,-17,.17,23,25)
    for knots,d in [([(364,258,13),(389,272,18),(424,281,16),(453,279,17)],4),
                    ([(313,259,11),(326,271,19),(351,285,18),(377,296,17),(392,296,15)],-27)]:
        limb("Forward balancing forearm",knots,d,.94,SKIN,True)
        x,y,r=knots[-1];ell("Balancing dinosaur fist",x,y,d,r*1.1,r*1.03,r*.95,SKIN,True)
    animal_head([(330,157),(339,125),(360,104),(390,98),(419,100),(439,115),(452,139),(462,157),
              (484,156),(500,163),(511,180),(511,196),(503,218),(484,234),(459,244),(427,250),
              (399,247),(372,237),(349,219),(335,194)],(415,174),-12,37,[(372,202,16,13)],
            [([(365,168),(367,155),(375,146),(386,147),(395,156),(400,173),(398,184),(389,192),(376,189)],
              [(365,168),(367,154),(375,147),(385,147),(394,154)],(389,174,13,18),(390,172,8.3,12),(387,159,4.4)),
             ([(434,148),(434,136),(441,131),(449,137),(454,151),(454,165),(448,170),(440,165)],
              [(434,148),(434,137),(440,132),(447,135),(451,143)],(446,154,8.4,14),(446,153,5.4,8.9),(443,143,3.1))],
            [([(361,137),(367,129),(377,127)],2.1),([(426,116),(433,115),(440,121)],1.9)],
            [(480,179,3.7),(499,172,3.5)],
            [(406,207),(420,213),(439,219),(457,219),(450,231),(437,240),(424,237),(415,228)],
            [(415,225),(427,223),(441,229),(447,233),(437,239),(426,237)])
    helmet=pillow("Coral skater helmet",[(278,130),(288,102),(311,79),(342,63),(376,58),(410,64),
          (433,80),(449,99),(450,110),(420,108),(384,115),(357,132),(337,153),(324,180),
          (305,202),(286,205),(277,189),(272,159)],-15,26,"F16B68",(349,116))
    line("Warm ivory visor",[(349,126,-48),(367,110,-53),(397,97,-50),(428,95,-35),(449,103,-25),(451,115,-22)],
         8.8,"FFE7B8",[.68,.96,1,1,.90,.42])
    line("Coral helmet chinstrap",[(316,166,-28),(321,192,-30),(339,222,-29),(365,242,-30),(391,249,-25)],
         7.2,"DF5551",[.86,1,.96,.68,.22])
    shape=[(316,101),(330,92),(326,110),(337,110),(302,150),(312,125),(300,128)]
    pillow("Golden lightning helmet emblem",shape,-47,1.1,"FFDF33",(317,117),
           front=lambda x,y:surface(helmet,x,y,-31)-.5,rings=14)
    for name,x,y,rx,ry in [("Planted toes",116,481,39,5),
                            ("Rear wheel",236,557,35,5),("Front wheel",443,557,35,5)]:
        ground_contact(name+" on ground",[world(x+u*rx,y+v*ry,180)
                       for u,v in [(-1,-1),(1,-1),(1,1),(-1,1)]],.36,"Skating contact shadow")


GOLD="FFCF36"
BUILDERS={"dinosaur-model-stegosaurus":stego,"dinosaur-model-egg":egg,
          "dinosaur-extra-skateboard":skateboard,"dinosaur-model-fossil":fossil}

if __name__=="__main__":
    for item in sys.argv[sys.argv.index("--")+1:]:
        BUILDERS[item]()
        save_render(item)



