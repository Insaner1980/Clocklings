"""Sculpt and render any of the veterinarian, roller-skates and dragon-costume doll rewards."""
from math import cos, exp, pi, sin, sqrt, tau
from pathlib import Path
import sys
import bpy
import numpy as np
from mathutils import Vector

sys.path.insert(0,str(Path(__file__).resolve().parent))
from transformation_shapes import (
    at, ellipse_points, line, mesh_object, oval, patch, pigment, ring_mesh, save_render, seam, since,
    smooth_outline, solid, studio, surface, tags,
)
from transformation_robot import mechanical, spindle


def fabric(name,color,plush=False):
    m=pigment(name,color,.72 if plush else .47,0,.03 if plush else .13)
    tree=m.node_tree;p=next(n for n in tree.nodes if n.type=="BSDF_PRINCIPLED")
    p.inputs["Sheen Weight"].default_value=.30 if plush else .12
    noise=tree.nodes.new("ShaderNodeTexNoise");noise.inputs["Scale"].default_value=68 if plush else 225
    bump=tree.nodes.new("ShaderNodeBump");bump.inputs["Strength"].default_value=.29 if plush else .13
    bump.inputs["Distance"].default_value=.013 if plush else .004
    tree.links.new(noise.outputs["Fac"],bump.inputs["Height"]);tree.links.new(bump.outputs[0],p.inputs["Normal"])
    return m


def fallback_surface(objects,x,y,default):
    hits=[]
    for obj in objects:
        try:hits.append(surface(obj,x,y))
        except ValueError:pass
    return min(hits,default=default)


def eyes(face,name,cx,cy,rx,ry,rot,dx,dy,iris_scale=.78,blink=True,lashes=True):
    before=set(bpy.context.scene.objects)
    chocolate=pigment("Rich upper lashes and pupils","39251E",.34)
    rim=patch(name+" eye upper lash line",ellipse_points(cx,cy,rx,ry,angle=rot),face,chocolate,1.9,3.2)
    white=patch(name+" eye white",ellipse_points(cx,cy+1.9,rx*.947,ry*.89,angle=rot),face,pigment("Warm white sclera","FFF4DF",.37),4.5,2.8)
    def project_or_eye(x,y):return fallback_surface([white],x,y,surface(face,x,y)-5)
    c=(cx+dx,cy+dy);irx=rx*iris_scale;iry=ry*iris_scale
    if iris_scale>=.74:
        irx*=1.13;iry*=1.15
    brown=solid(name+" warm brown iris",ellipse_points(*c,irx,iry,angle=rot),lambda x,y:project_or_eye(x,y)-2.6,2.0,pigment("Amber brown iris","694022",.34,0,.07),.78)
    pupil=patch(name+" dark pupil",ellipse_points(c[0],c[1]-1.3,irx*.74,iry*.77,angle=rot),brown,pigment("Deep soft pupil","271A18",.33,0,.07),2.0,1.2)
    for part in (white,brown,pupil):
        shader=next(n for n in part.data.materials[0].node_tree.nodes if n.type=="BSDF_PRINCIPLED")
        shader.inputs["Specular IOR Level"].default_value=.17
    spot=(c[0]-irx*.25,c[1]-iry*.43)
    solid(name+" single bright catchlight",ellipse_points(*spot,irx*.24,iry*.21),
          lambda x,y:fallback_surface([pupil,brown,white],x,y,-75)-2.1,1.2,pigment("Bright eye reflections","FFFFFF",.25))
    if lashes:
        s=-1 if "left" in name.lower() else 1
        coords=[(cx+s*rx*.83,cy-ry*.35),(cx+s*rx*.98,cy-ry*.29),(cx+s*rx*1.15,cy-ry*.38),(cx+s*rx*1.25,cy-ry*.48)]
        seam(name+" upswept lashes",[(x,y,surface(face,x,y)-4.0) for x,y in coords],3.6,chocolate)
    if blink:tags(since(before),"blink_part")


def base_eye(face, name, x, y, scale_x, scale_y, tilt, *, left, gaze=(0,0)):
    """Fit the wardrobe doll's painted eye and outer lash to the posed face."""
    # Same eye regions used by RenderedDollRig. Crop the studio skin around the lens.
    source = Path(__file__).resolve().parents[2] / "art/models/atlases/doll_rendered.png"
    atlas = bpy.data.images.load(str(source),check_existing=False)
    width, height = atlas.size
    pixels = np.empty(width*height*4, dtype=np.float32)
    atlas.pixels.foreach_get(pixels)
    origin = 1711 if left else 1799
    eye = pixels.reshape(height,width,4)[height-1392-96:height-1392,origin:origin+84].copy()
    atlas_space = atlas.colorspace_settings.name
    bpy.data.images.remove(atlas)
    texture = bpy.data.images.new(name+" wardrobe eye paint",84,96,alpha=True)
    texture.colorspace_settings.name = atlas_space
    texture.pixels.foreach_set(eye.ravel())
    texture.pack()

    material = bpy.data.materials.new(name+" wardrobe lens")
    material.use_nodes = True
    tree = material.node_tree
    tree.nodes.remove(tree.nodes.get("Principled BSDF"))
    paint = tree.nodes.new("ShaderNodeTexImage")
    paint.image = texture
    emission = tree.nodes.new("ShaderNodeEmission")
    emission.inputs["Strength"].default_value = 2 ** -bpy.context.scene.view_settings.exposure
    tree.links.new(paint.outputs["Color"],emission.inputs["Color"])
    tree.links.new(emission.outputs[0],tree.nodes.get("Material Output").inputs["Surface"])

    outline = [(12,33),(17,34),(20,31),(22,26),(27,22),(33,19),(39,17),
               (45,17),(51,19),(57,23),(61,28),(65,34),(66,44),(65,57),
               (63,65),(59,72),(53,76),(46,77),(39,76),(32,73),(26,68),
               (22,61),(20,52),(19,44),(18,40),(15,37)]
    c,s = cos(tilt),sin(tilt)
    contour = []
    for u,v in outline:
        dx = ((u if left else 84-u)-42)*scale_x
        dy = (v-47)*scale_y
        contour.append((x+c*dx-s*dy,y+s*dx+c*dy))
    lens = patch(name+" fitted wardrobe eye",contour,face,material,2.6,2)
    uv = lens.data.uv_layers.new(name="Wardrobe eye")
    for loop in lens.data.loops:
        point = lens.data.vertices[loop.vertex_index].co
        dx,dy = point.x*200+627-x,1130-point.z*200-y
        u,v = 42+(c*dx+s*dy)/scale_x,47+(-s*dx+c*dy)/scale_y
        # Shift the gaze inside the fixed eyelid, fading out at the rim.
        weight = max(0,1-((u-42)/25)**2-((v-47)/30)**2)
        uv.data[loop.index].uv = ((u-gaze[0]*weight)/84,1-(v-gaze[1]*weight)/96)
    lens["blink_part"] = True
    lens["base_eye"] = True


def blush(face,centers):
    colors=face.data.color_attributes.new(name="Skin blush",type="FLOAT_COLOR",domain="POINT")
    mat=face.data.materials[0].copy();mat.name="Warm porcelain with soft cheeks"
    tree=mat.node_tree;shader=next(n for n in tree.nodes if n.type=="BSDF_PRINCIPLED")
    skin=tuple(shader.inputs["Base Color"].default_value)
    tint=(1,.34,.22,1)
    for i,vert in enumerate(face.data.vertices):
        x=627+200*vert.co.x;y=1130-200*vert.co.z
        strength=sum(.39*exp(-((x-a)/w)**2-((y-b)/h)**2) for a,b,w,h in centers)
        colors.data[i].color=tuple(skin[j]*(1-strength)+tint[j]*strength for j in range(3))+(1,)
    attr=tree.nodes.new("ShaderNodeVertexColor");attr.layer_name=colors.name
    tree.links.new(attr.outputs[0],shader.inputs["Base Color"]);face.data.materials[0]=mat


def brow(face,coords):
    seam("Hand-painted arched brow",[(x,y,surface(face,x,y)-3) for x,y in coords],4.2,HAIR)


def smile(face,coords):
    pts=[(x,y,surface(face,x,y)-2.8) for x,y in coords]
    line("Rose smile",pts,3.6,LIPS,[.57]+[1]*(len(pts)-2)+[.57])


def hair_lock(name,outline,under,depth,thickness):
    corners=[o.matrix_world@Vector(point) for o in under for point in o.bound_box]
    xmin,xmax=min(v.x for v in corners)*200+627,max(v.x for v in corners)*200+627
    ymin,ymax=1130-max(v.z for v in corners)*200,1130-min(v.z for v in corners)*200
    cx,cy=(xmin+xmax)/2,(ymin+ymax)/2
    rx,ry=(xmax-xmin)*.51,(ymax-ymin)*.52
    def curved_depth(x,y):
        h=max(.045,1-((x-cx)/rx)**2-((y-cy)/ry)**2)
        return -4.-94*sqrt(h)
    return solid(name,outline,curved_depth,thickness,HAIR,.78)


def strand(obj,coords,r=3):
    pts=[(x,y,surface(obj,x,y)-.35) for x,y in coords]
    seam("Sculpted strand in hair",pts,r,pigment("Chestnut strand ridge","A65C31",.35,0,.18))


def sleeve(name,outline,depth,thick,mat):return solid(name,outline,depth,thick,mat,.75)


def vet():
    # The torso and limbs are behind the puppy and both forearms wrap around its back and feet.
    for x in (539,694):
        oval("Doll ankle",(x,1009,4),(53,75,36),SKIN)
        solid("Ivory domed shoe",[(x-58,1051),(x-37,1030),(x-5,1026),(x+36,1035),(x+58,1060),(x+67,1092),(x+53,1116),(x+20,1126),(x-32,1124),(x-62,1111),(x-71,1091)],-15,51,IVORY,.61)
        seam("Ivory outsole welting",[(x-63,1092,-38),(x-38,1101,-64),(x+5,1105,-70),(x+42,1101,-61),(x+59,1093,-43)],2.9,pigment("Vanilla shoe welting","E9C596",.43))
        solid("Orange trouser leg",[(x-62,754),(x+49,749),(x+64,790),(x+65,943),(x+59,988),(x-52,990),(x-63,969)],20,54,SCRUB,.38)
        solid("Rolled orange trouser cuff",[(x-65,951),(x-46,945),(x+55,949),(x+67,958),(x+65,988),(x+56,997),(x-49,997),(x-67,986)],0,59,SCRUB,.35)
    solid("Central scrub top",[(502,520),(537,478),(660,464),(689,505),(711,578),(707,799),(670,843),(506,823),(481,737)],12,65,SCRUB,.52)
    solid("Cream scrub undershirt",[(535,474),(567,448),(627,443),(674,461),(677,504),(656,527),(577,530),(546,518)],5,48,IVORY,.54)
    oval("Doll neck",(599,449,-6),(57,47,40),SKIN)
    left=solid("Left scrub coat",[(511,472),(540,461),(565,493),(583,548),(594,662),(582,761),(565,853),(551,870),(479,864),(422,844),(415,819),(438,741),(449,651),(452,574),(473,500)],6,77,COAT,.56,center=(503,664))
    right=solid("Right scrub coat",[(640,469),(662,453),(709,475),(754,518),(774,583),(777,692),(798,788),(810,834),(793,850),(714,868),(682,868),(674,839),(667,746),(651,643),(649,557)],6,77,COAT,.56,center=(707,659))
    for side,obj,outline in [(-1,left,[(463,718),(483,716),(541,730),(538,757),(528,792),(505,809),(472,802),(450,788)]),(1,right,[(691,735),(720,727),(763,720),(776,766),(772,788),(748,803),(709,806),(695,788)])]:
        pocket=patch("Rounded patch pocket",outline,obj,pigment("Scrub pocket turquoise","38BFAF",.47),2.8,3.5)
        a,b=outline[0],outline[2]
        pts=[(a[0]*.90+b[0]*.10,a[1]+6),((a[0]+b[0])/2,(a[1]+b[1])/2+6),(a[0]*.10+b[0]*.90,b[1]+6)]
        seam("Pocket topstitch",[(x,y,surface(pocket,x,y)-1) for x,y in pts],1.8,pigment("Scrub thread","62D3BF",.55))
    sleeve("Soft clinic sleeve",[(464,511),(495,488),(519,515),(528,556),(511,599),(493,622),(460,635),(425,632),(415,621),(423,580),(442,537)],-25,45,COAT)
    # Stethoscope tube descends from behind the neck to its metal chestpiece.
    tubepts=[(523,449,-22),(515,470,-43),(526,502,-63),(548,548,-96),(556,571,-112)]
    line("Coral stethoscope",tubepts,11,pigment("Coral stethoscope silicone","F87978",.33))
    for r,d,c in [(40,-112,"F47673"),(32,-125,"CFB599"),(25,-129,"F4EAD2")]:oval("Chestpiece",(558,576,d),(r,r,10),pigment("Chestpiece "+c,c,.25,.21))
    metal=pigment("Brushed nickel diaphragm","CDC9C1",.27,.62)
    line("Rolled edge of listening diaphragm",[(558+26*cos(i*tau/40),576+26*sin(i*tau/40),-138) for i in range(40)],3.1,metal,cyclic=True)
    spindle("Metal chestpiece stem",(548,546,-95),(557,557,-120),6.5,7.8,metal)
    # Rear forearm cups the puppy's side. Front arm supports its bandaged paw.
    rightarm=solid("Right arm cradles puppy",[(763,582),(805,582),(839,606),(851,632),(848,666),(827,689),(785,707),(745,721),(711,715),(700,696),(714,667),(752,637)],-58,49,SKIN)
    dog()
    leftarm=solid("Left arm and supporting palm",[(448,598),(472,595),(505,610),(523,628),(557,645),(586,650),(615,644),(634,648),(628,666),(663,667),(684,674),(693,683),(684,696),(648,709),(610,716),(561,709),(524,699),(478,683),(447,661),(435,634)],-99,36,SKIN,.76,center=(527,655))
    rightpalm=solid("Puppy held against chest",[(731,670),(753,659),(787,654),(816,646),(842,636),(848,654),(838,681),(814,700),(779,712),(745,718),(719,707),(711,695)],-112,26,SKIN)
    for finger in [[(621,685),(644,682),(665,679)],[(616,697),(637,695),(656,691)],
                   [(752,694),(776,689),(793,680)]]:
        hand=leftarm if finger[0][0]<700 else rightpalm
        coords=[(x,y,surface(hand,x,y)-1.1) for x,y in finger]
        line("Softly bent fingers supporting the patient",coords,1.65,pigment("Muted palm crease","DB997F",.52),[.12,1,.15])
    # Pigtails feed into their curved elastics; the bangs meet at the crown's side part.
    tail1=solid("Chestnut left pigtail",[(392,357),(420,345),(449,368),(454,411),(448,466),(427,515),(410,538),(384,529),(357,508),(339,472),(336,430),(353,389)],31,53,HAIR,.85)
    tail2=solid("Chestnut right pigtail",[(734,272),(755,272),(792,294),(815,333),(826,375),(812,414),(781,442),(749,451),(733,425),(721,389),(715,335)],31,49,HAIR,.85)
    face=solid("Round veterinary doll face",[(425,306),(446,238),(493,184),(569,157),(645,167),(699,202),(726,263),(733,322),(729,371),(709,406),(673,438),(624,457),(568,461),(518,445),(465,418),(429,383)],-6,89,SKIN,.83)
    blush(face,[(491,402,45,35),(701,372,38,32)])
    back=solid("Chestnut crown",[(382,283),(396,211),(434,151),(493,114),(559,99),(619,110),(657,136),(688,144),(720,181),(737,226),(736,283),(719,303),(688,249),(651,204),(575,191),(510,235),(459,316),(425,361),(396,350)],21,76,HAIR,.74,center=(530,185))
    hair_lock("Left swept fringe",[(383,289),(402,211),(449,150),(512,112),(580,99),(625,112),(651,137),(657,175),(641,201),(595,241),(548,268),(500,285),(459,320),(430,348),(405,344)], [back,face],-49,25)
    hair_lock("Right temple lock",[(652,142),(682,148),(715,179),(731,215),(736,252),(729,281),(714,271),(698,233),(677,212),(647,195)], [back,face],-52,15)
    for x,y,a in [(415,370,-.65),(738,291,.18)]:
        o=oval("Coral pigtail elastic",(x,y,-25),(17,43,21),CORAL);o.rotation_euler.y=a
    brow(face,[(500,331),(512,312),(531,302),(541,303)]);brow(face,[(658,266),(669,257),(684,255),(695,264)])
    base_eye(face, "Left vet", 557,367, 1.16,1.12, -.16, left=True, gaze=(4.5,6))
    base_eye(face, "Right vet", 683,329, .91,1.07, -.16, left=False, gaze=(4.5,6))
    oval("Button nose",(635,386,surface(face,635,386)-5),(16,13,13),SKIN)
    smile(face,[(610,419),(627,425),(646,422),(661,414),(671,400)])


def dog():
    fur=fabric("Patient puppy honey plush","D98D47",True);ears=fabric("Warm folded puppy ears","C38142",True)
    muzzle=fabric("Soft cream muzzle","FBE0B0",True)
    # Bounding landmarks include the head tilt, receding left ear and the drooping right ear.
    solid("Puppy's curled body",[(708,538),(758,532),(803,552),(825,599),(826,642),(808,683),(771,698),(713,693),(689,665),(684,605)],-70,43,fur)
    solid("Rounded rear puppy paw",[(804,667),(839,667),(854,689),(850,718),(835,733),(805,729),(786,712),(782,691)],-74,25,fur)
    solid("Near hind puppy paw",[(691,644),(721,651),(737,674),(725,699),(702,708),(684,695),(682,666)],-126,23,fur)
    skull=solid("Puppy's upturned face",[(663,487),(681,457),(710,433),(748,415),(783,413),(814,425),(840,450),(855,477),(856,515),(840,545),(811,566),(778,575),(737,572),(695,558),(671,535),(661,509)],-99,51,fur,.81)
    solid("Puppy's long outside ear",[(838,467),(863,480),(885,511),(898,548),(896,571),(878,597),(852,609),(833,604),(820,583),(818,549),(822,511)],-106,33,ears)
    solid("Far folded ear",[(693,448),(713,438),(719,450),(702,471),(678,486),(665,487),(666,476)],-82,22,ears)
    nosepad=patch("Puppy ivory muzzle",[(672,500),(683,477),(705,467),(729,468),(750,485),(764,510),(761,532),(741,547),(710,548),(685,534)],skull,muzzle,3,16)
    dark=pigment("Puppy's cocoa nose and mouth","593421",.34)
    oval("Puppy's brown nose",(719,481,surface(nosepad,719,481)-5),(16,12,11),dark)
    for nm,x,y,rx,ry,r in [("Left puppy",725,454,12,15,.52),("Right puppy",779,480,18,18,.18)]:
        eyes(skull,nm,x,y,rx,ry,r,-rx*.29,-ry*.30,.75,False,False)
    for coords in [[(726,427),(738,423),(749,426)],[(787,445),(799,449),(809,462)]]:
        seam("Puppy brow",[(x,y,surface(skull,x,y)-2) for x,y in coords],2.1,dark)
    for coords in [[(718,489),(714,499),(704,505),(696,502),(691,492)],[(715,498),(721,511),(734,516),(742,509)]]:
        seam("Puppy smiling up",[(x,y,surface(nosepad,x,y)-1.9) for x,y in coords],2.3,dark)
    arm=solid("Puppy's far front paw",[(749,573),(782,571),(802,588),(805,609),(789,638),(764,655),(746,656),(721,643),(718,626)],-125,26,fur)
    for x,y in ((738,641),(749,645)):
        seam("Puppy toe crease",[(x,y-4,-145),(x+1,y+3,-147),(x-1,y+7,-145)],1.3,ears)
    limb=solid("Bandaged puppy paw",[(660,579),(680,568),(705,582),(713,602),(697,633),(676,667),(657,676),(628,669),(621,652),(633,627)],-140,22,fur)
    for j,c in enumerate(("B491D0","C7A4DE","BFA0D7","CAAADF")):
        xa=670-j*6;ya=584+j*11
        band=solid("Lavender bandage turn",[(xa,ya),(xa+19,ya+2),(xa+35,ya+15),(xa+35,ya+23),(xa+27,ya+29),(xa+7,ya+20),(xa-16,ya+14),(xa-16,ya+8)],-158-j,11,fabric("Bandage "+c,c))

        coords=[(xa-11,ya+11),(xa+2,ya+14),(xa+16,ya+20),(xa+29,ya+24)]
        line("Woven bandage selvedge",[(x,y,surface(band,x,y)-1.1) for x,y in coords],1.25,fabric("Pale bandage fibre","DFBFE9"),[.25,1,1,.25])

def materials():
    global SKIN,IVORY,SCRUB,HAIR,LIPS,COAT,CORAL,VIOLET,GOLD
    SKIN=pigment("Warm peach doll skin","F8C29B",.44,0,.06)
    IVORY=pigment("Buttermilk ivory","FFEBCC",.37,0,.20)
    SCRUB=fabric("Apricot scrub cloth","F8A34E")
    HAIR=pigment("Polished chestnut hair","98552E",.40,0,.10)
    LIPS=pigment("Warm rosy smile","CD5A4D",.38)
    COAT=fabric("Seafoam scrub coat","40C4B4")
    CORAL=pigment("Coral elastic","F57481",.30,0,.22)
    VIOLET=fabric("Soft purple playsuit","A35BC2",True)
    GOLD=pigment("Warm bright gold","FEBE38",.26,.10,.31)


def render_doll(kind):
    frames={"veterinarian":(616,612,1103),"roller-skates":(575,598,1218),"dragon-costume":(591,598,1222)}
    scene=studio("doll",frames[kind]);materials()
    if kind in ("roller-skates","veterinarian"):
        scene.render.resolution_x=scene.render.resolution_y=832
        scene.render.threads=10
        scene.cycles.samples=40
        scene.view_settings.exposure=-.37
        scene.objects["Cool front fill"].data.energy=250
        scene.objects["Overhead satin highlight"].data.energy=355
        scene.world.node_tree.nodes.get("Background").inputs["Strength"].default_value=.28
    if kind=="veterinarian":vet()
    elif kind=="roller-skates":skater()
    else:dragon_suit()
    save_render("shoes-roller-skates" if kind=="roller-skates" else "model-"+kind)


def skating_wheels(centers,axis_delta,rad):
    # Four separated rollers on two metal axles; a shallow hub recess ends each cylinder.
    for coords in ((centers[0],centers[1]),(centers[2],centers[3])):
        spindle("Ivory skating axle",coords[0],coords[1],rad*.252,rad*.252,IVORY)
    for x,y,d in centers:
        dx,dy,dd=axis_delta
        a=(x-dx,y-dy,d-dd);b=(x+dx,y+dy,d+dd)
        mechanical("Rounded golden roller",a,b,[(0,rad*.79),(.11,rad*.96),(.23,rad),(.78,rad),(.92,rad*.98),(1,rad*.83)],GOLD)
        oval("Ivory wheel bearing",(x+dx,y+dy,d+dd-2),(rad*.30,rad*.38,rad*.17),IVORY)


def skater():
    jacket=pigment("Turquoise satin bomber","22C7B9",.29,0,.28)
    rib=fabric("Turquoise knitted welts","24AAA6")
    violet=fabric("Grape disco shorts","8951AD")
    pink=pigment("Pink skate protectors","F580AF",.32,0,.15)
    coral=pigment("Coral skate trim","FF846F",.29)
    # Rear thigh points out from the shorts and the free knee rises against the jacket.
    solid("Lifted skater leg",[(675,662),(711,641),(744,635),(767,646),(784,666),(803,700),(854,728),(890,764),(892,808),(861,830),(827,827),(803,803),(775,764),(752,747),(697,737),(668,711)],5,53,SKIN)
    solid("Weight-bearing leg",[(475,716),(575,725),(600,742),(575,789),(546,851),(519,927),(508,961),(418,962),(419,928),(441,862),(458,802)],8,48,SKIN)
    solid("Deep magenta tank",[(529,453),(602,441),(649,457),(688,530),(701,618),(673,648),(572,660),(484,651),(475,604),(493,504)],3,59,pigment("Raspberry dance top","F55D91",.44))
    solid("Purple skating shorts",[(471,627),(517,612),(617,613),(672,600),(707,609),(746,629),(721,643),(693,677),(676,727),(648,751),(603,740),(568,758),(484,751),(455,739),(455,683)],10,72,violet,.58)
    solid("Rolled shorts hem at free hip",[(683,637),(709,620),(730,618),(744,630),(716,653),(687,709),(680,742),(661,750),(638,739),(643,701),(659,662)],-16,39,violet,.62)
    solid("Leg opening above standing knee",[(456,701),(504,699),(567,710),(604,731),(604,754),(588,768),(541,755),(489,746),(452,738)],-8,58,violet,.43)
    for x,y,r,angle in [(509,837,48,.13),(793,691,45,-.56)]:
        o=oval("Elastic knee band",(x,y,0),(r*1.20,r*.76,47),pink);o.rotation_euler.y=angle
        o=oval("Domed knee protector",(x+2,y,-38),(r,r*.85,27),pink);o.rotation_euler.y=angle
        o=oval("Padded inset",(x+2,y-1,-56),(r*.75,r*.58,12),pigment("Pink knee pad face","FF91B8",.37));o.rotation_euler.y=angle
    # Raised boot is foreshortened, exposing all four honey wheels and both axle bars.
    solid("Rise skate padded boot",[(816,775),(855,754),(891,747),(925,720),(958,717),(983,728),(1009,754),(1025,785),(1018,816),(962,852),(918,913),(889,923),(861,904),(834,880),(815,855),(805,827)],-17,59,jacket)
    solid("Free skate coral sock",[(806,782),(823,761),(848,744),(870,737),(887,741),(899,755),(884,774),(855,791),(836,821),(820,836),(807,830),(795,812)],-40,33,coral)
    sole=solid("Upturned cream skate sole",[(868,907),(874,873),(894,830),(926,789),(947,755),(969,741),(994,745),(1019,760),(1032,790),(1032,817),(1013,856),(980,903),(946,944),(920,957),(895,945)],-78,15,IVORY,.55)
    spindle("Angled raised skate truck",(901,929,-99),(972,820,-100),12,13,IVORY)
    skating_wheels([(884,921,-115),(965,936,-110),(939,832,-116),(1013,849,-108)],(8,2,-14),32)
    spindle("Raised coral toe brake",(976,782,-92),(999,801,-127),32,29,coral)
    oval("Toe brake hollow",(1000,805,-140),(14,6,3),pigment("Toe brake shadow","C65F50",.48))
    # The standing skate's far wheels peek either side of its near axle.
    skating_wheels([(360,1127,8),(435,1136,-47),(474,1110,16),(521,1122,-47)],(8,1,-13),35)
    solid("Standing skate leather",[(357,1044),(391,1006),(407,970),(433,950),(492,951),(518,964),(527,999),(536,1043),(529,1063),(489,1081),(408,1091),(353,1081)],-7,48,jacket)
    solid("Standing skate sole",[(347,1067),(371,1078),(431,1079),(487,1068),(535,1053),(539,1068),(530,1083),(470,1100),(404,1104),(359,1095),(347,1088)],-18,48,IVORY,.36)
    solid("Turned coral ankle sock",[(413,929),(460,935),(513,939),(532,946),(524,978),(511,985),(455,981),(409,973),(407,957)],-13,48,coral,.36)
    for y in (983,996,1010):
        seam("Standing skate leather lace",[(411,y+9,-40),(426,y+4,-53),(449,y+3,-57),(461,y+9,-52)],3.9,rib)
    spindle("Standing toe brake",(400,1097,-28),(389,1134,-64),25,27,coral)
    # The left and right sides of the bomber flare open around its pink shirt.
    left=solid("Left satin bomber front",[(521,450),(549,473),(563,504),(571,538),(563,592),(549,643),(535,674),(510,684),(475,677),(433,655),(432,630),(445,601),(457,554),(455,503),(479,469)],-7,58,jacket,.71)
    right=solid("Right satin bomber front",[(622,449),(652,449),(685,465),(704,499),(708,553),(721,579),(744,607),(744,624),(719,635),(682,644),(664,632),(649,598),(633,551),(621,495)],-6,57,jacket,.71)
    for obj,side,coords in [(left,-1,[(438,633),(459,649),(491,663),(519,667),(532,658)]),(right,1,[(674,626),(694,628),(715,623),(737,611)])]:
        stitched("Jacket hem topstitch",[(x,y-9,surface(obj,x,y-9)-1) for x,y in coords],rib,9,1.25)
        seam("Ribbed bomber hem",[(x,y,surface(obj,x,y)-1) for x,y in coords],8,rib)
    sleeve("Left outstretched bomber sleeve",[(341,467),(364,451),(390,443),(422,446),(462,455),(490,467),(506,496),(497,527),(470,549),(429,557),(391,564),(360,554),(344,533),(335,492)],-3,52,jacket)
    sleeve("Right outstretched bomber sleeve",[(661,456),(711,441),(751,426),(779,418),(797,420),(817,443),(824,469),(818,484),(791,500),(750,517),(713,529),(684,531),(661,506)],-3,50,jacket)
    for a,b,r in [((364,497,-6),(324,499,-6),40),((802,458,-4),(839,447,-4),38)]:
        spindle("Rib-knit sleeve cuff",a,b,r,r,rib)
        for k in (.19,.48,.77):
            x=a[0]+(b[0]-a[0])*k;y=a[1]+(b[1]-a[1])*k
            spindle("Ribbed cuff stitch",(x-2,y,-5),(x+2,y,-5),r*1.016,r*1.016,jacket)
    solid("Open left balance hand",[(321,468),(327,475),(326,509),(312,526),(278,526),(263,521),(248,524),(223,526),(215,519),(220,505),(203,495),(184,484),(175,473),(179,462),(191,464),(179,455),(184,444),(203,442),(233,451),(260,458),(286,467)],-10,29,SKIN,.79,center=(265,487))
    solid("Open right balance hand",[(839,424),(857,419),(889,401),(908,387),(925,378),(942,377),(949,383),(934,401),(954,387),(963,390),(967,399),(954,418),(929,441),(938,445),(942,454),(935,464),(907,469),(882,474),(851,482)],-11,30,SKIN,.79,center=(904,427))
    for coords in [[(181,458,-20),(195,458,-28),(213,465,-32)],[(923,397,-27),(936,386,-26),(944,382,-21)]]:
        seam("Rounded toy finger crease",coords,1.6,pigment("Warm peach crease","EEB288",.48))
    stars=[(685+34*(1 if i%2==0 else .46)*sin(i*pi/5),518-36*(1 if i%2==0 else .46)*cos(i*pi/5)) for i in range(10)]
    solid("Club star patch",stars,-72,6,GOLD,.39,center=(685,518))
    oval("Neck",(583,456,-5),(43,37,39),SKIN)
    pony=solid("Sculpted curling high ponytail",[(109,284),(112,244),(132,213),(167,195),(185,193),(175,170),(183,147),(202,130),(226,124),(239,131),(224,142),(217,160),(228,169),(244,161),(258,137),(266,101),(286,67),(326,42),(370,29),(412,31),(447,49),(467,83),(473,117),(442,144),(402,169),(374,201),(355,249),(332,283),(293,307),(258,317),(220,310),(199,299),(183,302),(180,318),(196,325),(213,315),(222,323),(211,341),(189,353),(155,348),(125,331)],24,63,HAIR,.76,center=(297,181))
    strand(pony,[(153,242),(173,224),(198,216),(216,220),(254,229),(297,211),(326,172),(350,123),(378,90),(416,77)],2.2)
    strand(pony,[(191,187),(218,195),(254,190),(281,165),(300,117),(337,71),(372,55),(406,53)],1.9)
    for contour,centre in [
        ([(361,52),(325,63),(292,99),(277,146),(257,174),(234,182),(215,177),(229,193),(260,193),(291,175),(310,138),(323,100)],(289,139)),
        ([(304,224),(285,249),(250,273),(213,279),(185,270),(169,279),(165,304),(153,299),(151,275),(168,253),(195,248),(223,251),(260,243)],(207,262))]:
        solid("Turning ribbon of ponytail hair",contour,lambda x,y:surface(pony,x,y)-2.5,9,HAIR,.76,center=centre)
    face=solid("Happy disco doll face",[(414,272),(450,202),(506,165),(573,150),(630,164),(679,197),(710,253),(715,315),(701,365),(670,409),(621,440),(567,452),(519,442),(473,418),(437,388),(413,351)],-5,89,SKIN,.82)
    blush(face,[(467,361,40,31),(686,353,35,32)])
    crown=solid("Chestnut skater crown",[(362,272),(381,211),(407,165),(458,125),(514,109),(552,103),(602,114),(631,137),(661,151),(696,185),(717,234),(724,286),(713,315),(700,282),(684,234),(648,198),(608,179),(537,190),(486,232),(442,286),(427,359),(410,389),(390,364)],21,73,HAIR,.71,center=(514,187))
    hair_lock("Bouncing swept skate fringe",[(370,266),(389,206),(434,158),(492,120),(550,105),(591,108),(625,124),(642,148),(634,173),(603,194),(545,218),(494,240),(453,275),(432,326),(437,358),(423,362),(400,340),(378,306)],[face,crown],-60,24)
    hair_lock("Skater temple lock",[(630,143),(664,157),(691,181),(711,221),(717,250),(714,299),(705,310),(696,272),(678,233),(658,209),(625,178)],[face,crown],-48,15)
    band=solid("Coral high ponytail elastic",[(398,133),(411,110),(432,93),(451,85),(474,89),(489,106),(493,116),(470,123),(437,148),(418,166),(406,159)],-24,23,coral)
    brow(face,[(479,273),(491,256),(507,250),(522,253)]);brow(face,[(638,237),(651,231),(665,233),(678,242)])
    base_eye(face, "Left skater", 519,320, 1.09,1.10, -.13, left=True)
    base_eye(face, "Right skater", 658,299, .91,1.04, -.13, left=False)
    oval("Doll button nose",(604,349,surface(face,604,349)-5),(15,12,13),SKIN)
    mouth=patch("Laughing smile",[(571,380),(584,383),(604,382),(620,378),(633,371),(632,382),(626,397),(616,407),(603,413),(592,412),(580,404),(573,393)],face,pigment("Warm mouth","8F3033",.43),3,3)
    patch("Tongue in smile",[(583,398),(596,392),(610,393),(619,399),(615,405),(603,411),(593,410)],mouth,pigment("Warm tongue","EA747D",.38),2,1)

def stitched(name,points,thread,interval=11,r=1.1):
    for a,b in zip(points,points[1:]):
        va,vb=Vector(a),Vector(b)
        length=(vb-va).length
        count=max(1,round(length/interval))
        for i in range(count):
            p=va.lerp(vb,(i+.16)/count);q=va.lerp(vb,(i+.74)/count)
            line(name,[p,q],r,thread)


def fleece(name, color):
    """Dense, irregular fleece in world units, with the same pile size on every pattern piece."""
    mat = fabric(name, color, True)
    tree = mat.node_tree
    shader = next(node for node in tree.nodes if node.type == "BSDF_PRINCIPLED")
    shader.inputs["Sheen Weight"].default_value = .19
    shader.inputs["Specular IOR Level"].default_value = .16
    base = tuple(shader.inputs["Base Color"].default_value)
    coords = tree.nodes.new("ShaderNodeNewGeometry")
    noise = next(node for node in tree.nodes if node.type == "TEX_NOISE")
    noise.inputs["Scale"].default_value = 79
    noise.inputs["Detail"].default_value = 2.1
    noise.inputs["Roughness"].default_value = .74
    tree.links.new(coords.outputs["Position"], noise.inputs["Vector"])
    ramp = tree.nodes.new("ShaderNodeValToRGB")
    for stop, position, multiplier in zip(ramp.color_ramp.elements, (.30, .70), (.83, 1.20)):
        stop.position = position
        stop.color = tuple(channel * multiplier for channel in base[:3]) + (1,)
    tree.links.new(noise.outputs["Fac"], ramp.inputs["Fac"])
    tree.links.new(ramp.outputs["Color"], shader.inputs["Base Color"])
    bump = next(node for node in tree.nodes if node.type == "BUMP")
    bump.inputs["Strength"].default_value = .38
    bump.inputs["Distance"].default_value = .006
    mat["dragon_fleece"] = True
    return mat


def cloth_folds(obj, ridges):
    """Sculpt rounded gathers into the cloth, including the outer silhouette."""
    for vert in obj.data.vertices:
        x, y = 627 + vert.co.x * 200, 1130 - vert.co.z * 200
        displacement = 0
        for cx, cy, slope, half_width, spread, height in ridges:
            dx = (x - cx) / half_width
            dy = (y - cy - (x - cx) * slope) / spread
            if abs(dx) < 2.6 and abs(dy) < 4:
                displacement += height * exp(-dx*dx) * (exp(-dy*dy) - .45*exp(-(dy-1.5)**2))
        vert.co += vert.normal * (displacement / 200)
    obj.data.update()
    bpy.context.view_layer.update()


def fleece_pile():
    """Short tufts follow the surface normals, including along the hood, wing and tail silhouettes."""
    from random import Random
    rng = Random(18)
    fabric_parts = [obj for obj in bpy.context.scene.objects if obj.type == "MESH"
                    and obj.active_material and obj.active_material.get("dragon_fleece")]
    bpy.context.view_layer.update()
    for obj in fabric_parts:
        mesh = obj.data
        mesh.calc_loop_triangles()
        verts, faces = [], []
        world = obj.matrix_world
        normal_matrix = world.to_3x3().inverted().transposed()
        for triangle in mesh.loop_triangles:
            indices = triangle.vertices
            p, q, r = [world @ mesh.vertices[i].co for i in indices]
            area = (q-p).cross(r-p).length / 2
            normal = (normal_matrix @ triangle.normal).normalized()
            if normal.y > .25:
                continue
            for _ in range(int(1800 * area + rng.random())):
                u, v = rng.random(), rng.random()
                if u + v > 1:
                    u, v = 1-u, 1-v
                root = p + u*(q-p) + v*(r-p)
                n = (normal_matrix @ (mesh.vertices[indices[0]].normal*(1-u-v)
                     + mesh.vertices[indices[1]].normal*u + mesh.vertices[indices[2]].normal*v)).normalized()
                tangent = n.cross(Vector((.4, -.2, 1))).normalized()
                across = n.cross(tangent)
                tilt = tangent*rng.uniform(-.62,.62) + across*rng.uniform(-.5,.7)
                strand = (n + tilt) * rng.uniform(.007,.014)
                width = tangent * rng.uniform(.0016,.0032)
                base = len(verts)
                verts.extend([root - width, root + width, root + strand + tilt*.003])
                faces.append((base,base+1,base+2))
        if faces:
            pile = bpy.data.meshes.new(obj.name+" pile")
            pile.from_pydata(verts,[],faces)
            pile.update()
            tufts = bpy.data.objects.new(obj.name+" short fleece pile",pile)
            bpy.context.scene.collection.objects.link(tufts)
            pile.materials.append(obj.active_material)


def dragon_eye(face, name, x, y, rx, ry, side):
    before = set(bpy.context.scene.objects)
    outline = [(-1,-.08),(-.87,-.58),(-.52,-.91),(-.02,-1),(.51,-.88),
               (.85,-.54),(1,.03),(.92,.49),(.63,.84),(.18,.99),
               (-.38,.93),(-.80,.62)]
    sclera = patch(name+" ivory sclera",[(x+u*rx,y+v*ry) for u,v in outline],
                   face,pigment("Doll warm ivory sclera","FFF4EB",.42),3,4)
    brown = pigment("Cocoa glass irises","58321F",.30)
    if not brown.node_tree.nodes.get("Iris gradient"):
        tree = brown.node_tree
        shader = next(n for n in tree.nodes if n.type == "BSDF_PRINCIPLED")
        tex = tree.nodes.new("ShaderNodeTexCoord")
        split = tree.nodes.new("ShaderNodeSeparateXYZ")
        tree.links.new(tex.outputs["Generated"],split.inputs["Vector"])
        ramp = tree.nodes.new("ShaderNodeValToRGB")
        ramp.name = "Iris gradient"
        ramp.color_ramp.elements[0].color = (.16,.061,.020,1)
        ramp.color_ramp.elements[1].color = (.019,.008,.006,1)
        tree.links.new(split.outputs["Z"],ramp.inputs[0])
        tree.links.new(ramp.outputs["Color"],shader.inputs["Base Color"])
        shader.inputs["Specular IOR Level"].default_value = .10
    iris_x, iris_y = x+rx*.12,y-ry*.035
    def iris_depth(px,py):
        return fallback_surface([sclera],px,py,surface(face,px,py)-3)-1.8
    iris = solid(name+" brown iris",ellipse_points(iris_x,iris_y,rx*.81,ry*.965,angle=-.10),
                 iris_depth,3,brown)
    pupil = patch(name+" dark pupil",ellipse_points(iris_x,iris_y-ry*.08,rx*.51,ry*.61,angle=-.10),
                  iris,pigment("Deep espresso pupils","221617",.44),1.7,1.6)
    pupil_shader = next(n for n in pupil.active_material.node_tree.nodes if n.type == "BSDF_PRINCIPLED")
    pupil_shader.inputs["Specular IOR Level"].default_value = .10
    solid(name+" reflected light",ellipse_points(iris_x-rx*.22,iris_y-ry*.44,rx*.19,ry*.16),
          lambda px,py: fallback_surface([pupil,iris],px,py,surface(sclera,px,py))-2.3,
          1.2,pigment("Softbox eye glint","FFF9F2",.38))
    lid = [(-.94,.05),(-.95,-.24),(-.75,-.65),(-.42,-.90),
           (.00,-.99),(.48,-.91),(.82,-.64),(.95,-.27)]
    points = [(x+u*rx,y+v*ry) for u,v in lid]
    lash = pigment("Cocoa upper eyelid","38211C",.6)
    line(name+" painted upper eyelid",
         [(px,py,fallback_surface([iris,sclera],px,py,surface(face,px,py))-2) for px,py in points],
         3.7,lash,[.30,.72,1,1.2,1.2,1,.78,.32])
    points = [(x+side*rx*.76,y-ry*.57),(x+side*rx*.96,y-ry*.43),
              (x+side*rx*1.21,y-ry*.61)]
    seam(name+" outer eyelash",[(px,py,surface(face,px,py)-5) for px,py in points],4.3,lash)
    tags(since(before),"blink_part")


def relaxed_dragon_hand(binding):
    """A tapered wrist and palm with overlapping fingers, posed in three-quarter view."""
    from reference_finish import fuse
    skin = pigment("Warm matte doll hands","F6BE96",.52)
    shader = next(n for n in skin.node_tree.nodes if n.type == "BSDF_PRINCIPLED")
    shader.inputs["Specular IOR Level"].default_value = .22
    shader.inputs["Subsurface Weight"].default_value = .055
    shader.inputs["Subsurface Scale"].default_value = .034

    def loft(name, sections):
        # Each cross-section is perpendicular to the finger in the image plane.
        # Its depth controls which finger overlaps its neighbour at the curled tips.
        sections = [Vector(row) for row in sections]
        path = []
        for i in range(len(sections)-1):
            a,b,c,d = (sections[max(0,min(len(sections)-1,k))] for k in (i-1,i,i+1,i+2))
            for step in range(4):
                t = step / 4
                path.append((2*b+(-a+c)*t+(2*a-5*b+4*c-d)*t*t+(-a+3*b-3*c+d)*t**3)/2)
        path.append(sections[-1])
        rings = []
        for i,(x,y,z,width,thickness) in enumerate(path):
            delta = path[min(i+1,len(path)-1)] - path[max(i-1,0)]
            lateral = Vector((delta[1],-delta[0])).normalized()
            ring = []
            for k in range(28):
                angle = tau*k/28
                offset = lateral * max(.4,width) * cos(angle)
                ring.append((x+offset.x,y+offset.y,z+max(.4,thickness)*sin(angle)))
            rings.append(ring)
        # The voxel union needs closed volumes for the palm, wrist and each digit.
        count = len(rings[0])
        vertices = [at(*point) for ring in rings for point in ring]
        faces = [(row*count+i,row*count+(i+1)%count,(row+1)*count+(i+1)%count,(row+1)*count+i)
                 for row in range(len(rings)-1) for i in range(count)]
        faces.append(tuple(reversed(range(count))))
        faces.append(tuple(range(len(vertices)-count,len(vertices))))
        return mesh_object(name,vertices,faces,skin)

    parts = [loft("Wrist flowing into back of hand", [
        (388,751,-26,16,11),(380,767,-39,29,18),(372,781,-48,34,19),
        (362,796,-53,40,22),(351,810,-57,43,23),(343,821,-58,40,20),
        (338,829,-58,32,16),(335,837,-59,22,12),(330,843,-60,3,3)])]
    fingers = {
        "Tucked little finger": [(320,810,-46,10,9),(310,823,-47,10,9),
            (301,841,-45,9,8),(301,851,-40,7,7),(302,855,-38,.7,.7)],
        "Far ring finger": [(323,806,-48,14,11),(313,818,-51,14,12),
            (303,835,-52,13,12),(297,850,-53,11,10),(297,861,-53,7,7),
            (298,866,-51,.7,.7)],
        "Middle finger": [(336,811,-62,15,12),(326,825,-65,15,14),
            (316,842,-68,15,13),(308,860,-70,13,12),(306,873,-68,9,9),
            (306,881,-65,.7,.7)],
        "Near index finger": [(353,816,-67,17,14),(344,832,-73,17,14),
            (335,848,-78,16,15),(326,866,-81,15,14),(321,878,-78,10,10),
            (317,885,-73,.7,.7)],
        "Relaxed opposing thumb": [(381,795,-47,16,14),(384,810,-49,18,16),
            (383,823,-54,18,15),(377,838,-62,16,14),(370,851,-68,15,13),
            (365,862,-69,12,11),(360,869,-67,7,7),(357,872,-64,.7,.7)],
    }
    for name, sections in fingers.items():
        parts.append(loft(name,sections))
    hand = fuse([part.name for part in parts],"Relaxed hand with softly curled fingers",voxel=.008)
    patch("Soft satin thumbnail",ellipse_points(366,858,4.8,7.1,angle=.46),hand,
          pigment("Natural peach fingernail","F4C6A7",.48),.55,1)

    # The cuff turns back into the sleeve. Its near lip passes over the wrist
    # and the darker inside is visible between the wrist and the far lip.
    rings = []
    for x,y,z,width,depth in [(390,746,-21,46,30),(390,749,-25,51,34),
            (386,758,-30,54,35),(378,776,-37,55,35),(374,785,-42,52,34),
            (373,787,-42,46,30),(374,785,-42,39,25),(379,773,-36,39,24),
            (388,750,-23,40,24)]:
        ring = []
        for i in range(64):
            angle = i*tau/64
            across = width*cos(angle)
            lip = depth*.38*sin(angle)
            ring.append((x+.87*across-.49*lip,y+.49*across+.87*lip,z+depth*sin(angle)))
        rings.append(ring)
    ring_mesh("Turned fleece cuff around relaxed wrist",rings,binding)


def dragon_suit():
    from reference_finish import fuse
    scene = bpy.context.scene
    scene.render.resolution_x = scene.render.resolution_y = 960
    scene.cycles.samples = 48
    scene.render.threads = 12
    scene.view_settings.view_transform = "Standard"
    scene.view_settings.look = "None"
    scene.view_settings.exposure = -.09
    for name, power in (("Left photographic softbox", 600), ("Cool front fill", 283),
                        ("Overhead satin highlight", 218), ("Portrait reflector", 110)):
        scene.objects[name].data.energy = power
    scene.objects["Left photographic softbox"].data.size = 4.9
    world = next(n for n in scene.world.node_tree.nodes if n.type == "BACKGROUND")
    world.inputs["Strength"].default_value = .29
    plush = fleece("Orchid purple romper fleece", "B05AC3")
    binding=fleece("Plum fleece binding","9A4DAB")
    membrane=fleece("Lilac wing and muzzle velour","C481D0")
    cream=fleece("Biscuit cream belly plush","FBE0B9")
    thread=pigment("Apricot stitch thread","CE9E72",.70)
    leaf=fleece("Marigold tail velour","FFBD41")
    rope=pigment("Dusty lilac topstitch","D18FC4",.70)
    # The two hood flaps rest on the shoulders behind the chin.
    solid("Quilted neckline under hood",[(436,549),(490,545),(578,558),(654,549),
          (736,522),(766,525),(753,556),(717,585),(680,617),(617,643),(536,638),
          (479,612),(452,587)],29,39,plush,.72)
    # One continuous soft taper bends from the rump up into the child's raised hand.
    trail=[(572,934,200),(689,995,156),(785,1010,110),(887,1002,65),(964,955,33),(1009,876,17),(1022,793,8),(1008,717,-4),(976,638,-25),(953,560,-57),(954,511,-63)]
    tail=line("Heavy curling plush tail",trail,89,plush,[1.32,1.18,1.04,.96,.91,.86,.74,.58,.41,.25,.17])
    bpy.ops.object.select_all(action="DESELECT");tail.select_set(True);bpy.context.view_layer.objects.active=tail;bpy.ops.object.convert(target="MESH")
    outline=[(695,954,-20),(769,975,-44),(856,966,-52),(937,926,-50),(981,859,-48),(1001,780,-46),(990,711,-39),(963,640,-27),(946,570,-25)]
    outline=[(x,y,surface(tail,x,y)-1.1) for x,y,_ in outline]
    line("Tail centre seam",outline,2.4,binding);stitched("Tail saddle stitch",[(x,y,d-2.5) for x,y,d in outline],rope,13,1.13)
    for j,pts in enumerate([[(723,914),(737,885),(760,870),(777,881),(789,912),(786,939)],[(810,939),(824,906),(843,882),(866,890),(889,915),(882,936)],[(899,907),(908,873),(929,856),(950,863),(958,885)],[(957,816),(942,793),(953,763),(966,737),(985,743),(995,771)]]):
        solid("Stuffed triangular tail spine",pts,18,18,binding,.81)
    # Left wing's concave scallops stretch between broad leading edges and sewn ribs.
    lw=solid("Left dragon wing",[(128,647),(159,589),(208,546),(269,518),(316,511),(342,519),(365,543),(390,581),(427,611),(453,619),(433,666),(402,707),(375,731),(338,704),(309,698),(284,704),(252,724),(241,719),(229,693),(204,671),(178,659),(151,658),(129,668),(120,659)],42,20,membrane,.73,center=(288,607))
    rw=solid("Folded opposite dragon wing",[(681,623),(710,597),(735,585),(755,593),(779,641),(789,690),(800,753),(785,778),(764,759),(751,738),(725,730),(714,739),(702,723)],73,17,membrane)
    for pts,r in [([(125,657),(160,598),(212,551),(272,524),(315,515),(344,524),(368,552)],7.1), ([(337,526),(311,552),(282,602),(260,660),(248,718)],5.1), ([(344,526),(334,573),(337,628),(347,681),(375,728)],4.7)]:
        line("Plush wing piping",[(x,y,fallback_surface([lw],x,y,37)-3) for x,y in pts],r,binding)
    # Join the romper shoulders, torso and leg volumes before adding cuffs and belly quilting.
    suit_parts=[]
    for nm,pts,dep,th in [
        ("Round onesie body",[(464,593),(505,570),(567,567),(633,577),(675,598),(710,653),(736,730),(754,806),(745,868),(711,913),(649,942),(566,950),(486,937),(419,907),(394,854),(406,774),(424,691)],16,99),
        ("Weight-bearing left leg",[(430,842),(491,862),(545,917),(524,972),(487,1028),(465,1055),(412,1058),(348,1048),(327,1029),(339,986),(367,941),(391,879)],15,65),
        ("Right leg in onesie",[(552,886),(620,875),(702,879),(706,935),(692,995),(681,1045),(651,1061),(583,1064),(540,1053),(519,1023),(529,965)],12,69),
        ("Left sleeve sewn to shoulder",[(444,605),(468,588),(505,591),(526,616),(514,661),(483,706),(454,745),(421,791),(399,804),(360,791),(334,768),(348,730),(379,676),(403,632)],6,53),
        ("Raised sleeve sewn to shoulder",[(655,596),(686,583),(721,590),(749,605),(779,605),(811,587),(839,563),(855,569),(872,603),(879,630),(853,655),(819,674),(781,693),(734,700),(696,690),(659,663)],6,54),
    ]:
        suit_parts.append(solid(nm,pts,dep,th,plush,.69))
    suit=fuse([o.name for o in suit_parts],"Continuous stuffed dragon romper",voxel=.023)
    cloth_folds(suit, [
        (389, 987, -.23, 52, 14, 11), (425, 1019, .20, 64, 12, 10),
        (591, 1007, -.22, 68, 13, 13), (655, 1026, .18, 48, 11, 8),
        (390, 757, .57, 47, 13, 12), (421, 735, .71, 29, 12, 6),
        (815, 613, 1.65, 27, 32, 6.5), (784, 639, 1.9, 20, 29, 4.8),
        (447, 899, -.78, 40, 15, 6),
    ])
    relaxed_dragon_hand(binding)
    solid("Raised sleeve cuff",[(826,570),(839,555),(858,559),(875,582),(889,615),
          (884,637),(868,648),(851,641),(840,622)],-30,31,binding)
    # The lifted fingers close around the narrow stem, opposite the thumb.
    palm = solid("Palm holding tail",[(863,585),(881,565),(906,548),(920,533),
        (935,520),(951,523),(963,540),(966,575),(954,596),(935,604),
        (913,601),(885,627),(870,617)],-55,24,SKIN,.75)
    grip = [palm]
    for index, (x, y, length, radius) in enumerate([(970,533,26,10.5),(973,552,26,10.5),
                                                    (970,571,22,9.5),(962,585,16,8)]):
        path = [(x-3,y-4,-48),(x+5,y,-65),(x+4,y+4,-79),
                (x-length*.38,y+1,-86),(x-length*.63,y-2,-86)]
        grip += [line("Curved finger around stem",path,radius,SKIN,[1,1,.95,.86,.78]),
                 oval("Finger pad",path[-1],(radius*.81,radius*.80,radius*.80),SKIN)]
    thumb = [(911,557,-66),(923,539,-78),(936,526,-90),(950,521,-91)]
    grip += [line("Thumb holding golden tip",thumb,12,SKIN,[1.45,1.24,1,.87]),
             oval("Thumb pad",thumb[-1],(11,10.8,10.4),SKIN)]
    fuse([part.name for part in grip], "Hand wrapped around tail", voxel=.012)
    tip=solid("Upright golden leaf-shaped tail tip",[(938,520),(920,499),(916,474),(923,438),(943,410),(972,394),(1000,386),(1033,371),(1042,373),(1056,399),(1067,434),(1065,466),(1049,495),(1023,516),(990,528),(959,525)], -59,36,leaf,.70,center=(982,458))
    tip_path = [(945,507),(950,483),(961,458),(976,438),(995,419),(1016,400),(1037,380)]
    seam("Leaf-shaped golden tip vein", [(x,y,surface(tip,x,y)-1) for x,y in tip_path],
         2.1, pigment("Golden seam","DF9E34",.68))
    edge = [(963,514),(990,515),(1018,503),(1040,485),(1054,458),(1056,434),(1049,405)]
    stitched("Golden tip edge stitching",[(x,y,surface(tip,x,y)-1.5) for x,y in edge],thread,9,1.25)
    belly=patch("Sewn cream belly quilt",[(555,669),(585,629),(623,611),(660,613),(688,631),(710,669),(728,721),(739,785),(735,841),(718,887),(682,911),(635,922),(582,916),(541,900),(516,871),(509,835),(514,784),(530,718)],suit,cream,6,17)
    # Two recessed, gently curved seams divide one padded panel into three lobes.
    for vert in belly.data.vertices:
        x, y = 627 + vert.co.x * 200, 1130 - vert.co.z * 200
        u = (x - 625) / 100
        upper, lower = 708 + 12*u - 5*u*u, 807 + 13*u - 9*u*u
        edge = max(0, 1 - u*u*.69)
        relief = 8*exp(-((y-upper)/7)**2) + 9*exp(-((y-lower)/8)**2)
        relief -= 9*exp(-((y-(upper+lower)/2)/34)**2) + 7*exp(-((y-lower-42)/31)**2)
        vert.co.y += relief * edge * max(0, -vert.normal.y) / 200
    belly.data.update()
    bpy.context.view_layer.update()
    for coords in [[(548,696),(574,701),(612,706),(650,711),(690,714),(714,715)],[(522,784),(553,793),(597,803),(642,809),(686,812),(729,811)]]:
        pts=[(x,y,surface(belly,x,y)-1) for x,y in coords]
        seam("Quilted belly piping",pts,1.8,thread)
        stitched("Belly seam stitch",[(x,y-2,d-2) for x,y,d in pts],thread,10.0,1.65)
    perimeter=[(558,678),(585,642),(623,623),(657,628),(681,644),(702,680),(718,727),(727,785),(722,839),(702,881),(673,899),(634,908),(585,902),(550,886),(530,862),(522,831),(528,783),(544,721)]
    stitched("Belly patch blanket stitch",[(x,y,surface(belly,x,y)-1.8) for x,y in perimeter+perimeter[:1]],thread,9.8,1.4)
    for x,rx in ((375,98),(622,111)):
        foot=solid("Rounded stuffed dragon slipper",[(x-rx*.82,1075),(x-rx*.40,1041),(x+7,1034),(x+rx*.55,1045),(x+rx*.86,1073),(x+rx,1111),(x+rx*.93,1141),(x+rx*.7,1156),(x+5,1168),(x-rx*.55,1167),(x-rx*.91,1152),(x-rx,1129)],-18,61,plush,.65)
        for j in (-1,0,1):
            xx=x+j*rx*.51; yy=1140-abs(j)*3
            oval("Small cream slipper claw",(xx,yy,surface(foot,xx,yy)-1),(19,22,15),cream)
        pts=[(x+rx*.87*cos(a*pi/16),1129-rx*.73*sin(a*pi/16)) for a in range(2,15)]
        stitched("Slipper toe seam",[(xx,yy,surface(foot,xx,yy)-2) for xx,yy in pts],thread,12,1.1)
    # Stuffed horns and bat frills are sewn into the shell of a genuinely open hood.
    for pts,ribpoints in [
        ([(339,341),(308,344),(288,332),(284,297),(268,278),(239,269),(234,258),(247,244),(268,219),(274,196),(266,171),(264,151),(280,141),(312,141),(351,153),(390,183),(407,209),(390,269)],
         [[(271,153),(293,163),(321,184),(347,218),(368,248)],[(240,256),(277,247),(316,245),(356,252)],[(298,331),(313,300),(339,275),(363,260)]]),
        ([(692,192),(713,153),(749,119),(786,106),(807,109),(811,123),(804,150),(811,166),(838,181),(850,195),(845,208),(820,222),(806,243),(796,271),(779,282),(750,264),(713,227)],
         [[(800,115),(776,124),(747,152),(730,179)],[(840,193),(811,189),(777,193),(746,211)]])]:
        frill=solid("Soft hood ear frill",pts,37,20,membrane)
        for path in ribpoints:
            seam("Frill stitched rib",[(x,y,fallback_surface([frill],x,y,16)-1) for x,y in path],5,binding)
    horn_felt = fleece("Warm ivory horn fleece","EAC9A6")
    horn_thread = pigment("Apricot horn stitching","CFAB86",.73)
    for pts,marks in [
        ([(391,48),(405,43),(427,53),(454,76),(480,106),(507,152),(503,174),(468,194),(422,199),(404,181),(390,133),(383,94),(382,64)], [[(393,87),(414,76),(445,69)],[(398,135),(425,118),(463,104)]]),
        ([(580,139),(600,103),(621,65),(647,33),(675,23),(690,34),(700,64),(704,107),(700,145),(684,167),(649,170),(607,157)], [[(617,78),(651,78),(692,95)],[(601,113),(638,116),(699,134)]])]:
        horn=solid("Plush cream hood horn",pts,17,25,horn_felt,.74)
        for path in marks:
            a,m,b = [Vector(point) for point in path]
            points = []
            for k in range(-9,37):
                t=k/24
                x,y = (1-t)*(1-2*t)*a + 4*t*(1-t)*m + t*(2*t-1)*b
                try:
                    points.append((x,y,surface(horn,x,y)-.8))
                except ValueError:
                    continue
            seam("Recessed horn band",points,1.4,horn_thread)
            stitched("Horn blanket stitching",points[::2],horn_thread,7,1.25)
    face=solid("Dragon-costumed doll face",[(418,380),(445,305),(502,244),(583,215),(659,217),(713,246),(756,311),(778,388),(767,448),(734,490),(683,529),(621,557),(556,562),(502,545),(455,514),(428,471)],-16,98,SKIN,.82)
    blush(face,[(483,477,63,42),(730,444,54,42)])
    cheek = face.data.color_attributes["Skin blush"]
    for i, vert in enumerate(face.data.vertices):
        x,y = 627+200*vert.co.x,1130-200*vert.co.z
        warmth = .29 * (exp(-((x-496)/49)**2-((y-478)/32)**2)
                       + exp(-((x-723)/43)**2-((y-449)/31)**2))
        col = cheek.data[i].color
        cheek.data[i].color = (col[0]*(1-warmth)+warmth,
                               col[1]*(1-warmth)+.19*warmth,
                               col[2]*(1-warmth)+.12*warmth,1)
    brow(face,[(489,366),(504,350),(524,343),(536,346)]);brow(face,[(670,313),(683,306),(700,308),(715,319)])
    dragon_eye(face,"Left dragon girl",532,422,35,40,-1)
    dragon_eye(face,"Right dragon girl",697,385,30,42,1)
    patch("Soft peach button nose",ellipse_points(628,449,16,12),face,SKIN,1.5,10)
    mouth=[(597,487),(610,495),(626,498),(642,495),(656,487),(668,473)]
    line("Embroidered rose smile",[(x,y,surface(face,x,y)-2.5) for x,y in mouth],
         4.9,pigment("Strawberry pink smile","CF3440",.63),[.42,.86,1,1,.90,.49])
    hair=solid("Hooded chestnut cap",[(384,358),(404,288),(455,226),(525,196),(600,185),(680,203),(735,242),(776,315),(781,366),(763,386),(741,330),(702,283),(665,253),(614,252),(533,293),(480,344),(436,430),(410,434),(385,401)],6,60,HAIR,.70,center=(532,246))
    def fringe_depth(x,y):
        radius = ((x-598)/201)**2 + ((y-388)/188)**2
        return -24 - 96*sqrt(max(.08,1-radius))
    solid("Swept fringe beneath dragon hood",[(385,361),(395,317),(424,271),
          (464,235),(518,211),(585,198),(638,198),(666,211),(657,234),
          (634,255),(605,274),(578,286),(555,291),(537,290),(544,284),
          (516,286),(482,305),(459,336),(443,372),(429,415),(408,433),(392,409)],
          fringe_depth,21,HAIR,.74,center=(505,265))
    for x,y,sign in [(433,528,-1),(789,479,1)]:
        tuft=oval("Pigtail tucked into hood",(x,y,-43),(45,69,39),HAIR);tuft.rotation_euler.y=sign*.37
        bpy.context.view_layer.update()
        strand(tuft, [(x-9,y-36),(x-14,y-8),(x-9,y+22),(x+1,y+43)], 2.1)
        tie=oval("Pink pigtail elastic",(x+sign*22,y-68,-53),(16,37,16),CORAL);tie.rotation_euler.y=-sign*.67
    outer=[(313,402),(335,299),(381,211),(439,158),(530,128),(613,120),(694,136),(754,177),(800,240),(829,325),(840,416),(834,488),(803,548),(746,582),(665,607),(575,612),(479,599),(404,572),(349,536),(323,487)]
    opening=[(376,419),(392,358),(419,309),(455,275),(511,246),(583,222),(649,211),(698,223),(738,252),(768,293),(787,350),(798,413),(793,464),(772,503),(734,532),(666,557),(582,574),(504,574),(438,558),(396,534),(376,501),(369,464)]
    solid("Plum hood lining",outer,111,42,binding,.60)
    oval("Neck inside hood collar",(602,561,28),(53,45,36),SKIN)
    def polar(points):
        b=smooth_outline(points);o=Vector((584,392));result=[]
        for i in range(88):
            u=Vector((cos(i*tau/88),sin(i*tau/88)));hit=[]
            for a,c in zip(b,b[1:]+b[:1]):
                v=c-a;det=u.x*v.y-u.y*v.x
                if abs(det)<.00001:continue
                w=a-o;dist=(w.x*v.y-w.y*v.x)/det;f=(w.x*u.y-w.y*u.x)/det
                if dist>0 and 0<=f<=1:hit.append(dist)
            result.append(o+u*max(hit))
        return result
    A,B=polar(outer),polar(opening)
    def hoop(f,d):
        return [(a.x+(b.x-a.x)*f,a.y+(b.y-a.y)*f,d) for a,b in zip(A,B)]
    hood=ring_mesh("Cushioned open purple hood",[hoop(1,0),hoop(1.00,-39),hoop(.90,-59),hoop(.62,-64),hoop(.27,-47),hoop(.04,-20),hoop(0,14),hoop(.08,45),hoop(.45,73),hoop(1.14,93)],plush)
    pts = hoop(.64,-65)
    for start, end in zip(pts,pts[1:]+pts[:1]):
        if (start[1]+end[1])/2 < 544:
            stitched("Stitches around hood",[start,end],rope,11,1.28)
    muzzle=solid("Stuffed lavender dragon muzzle",[(469,232),(487,197),(526,170),(580,153),(628,149),(668,165),(690,192),(696,220),(682,244),(645,256),(593,267),(541,269),(491,258)],-76,33,membrane,.76)
    for x,y,ang in ((554,204,-.44),(638,190,-.15)):
        patch("Sewn dragon nostril",ellipse_points(x,y,7.8,11,angle=ang),muzzle,pigment("Plum embroidered nostrils","60346E",.8),2,1.6)
    fleece_pile()
    # Collar overlaps the rim and connects directly to the romper's neckline.
    line("Folded hood collar",[(474,574,-27),(506,583,-44),(554,588,-60),(600,592,-65),(643,607,-61),(679,589,-46),(714,576,-33)],10.5,plush,[.4,.88,1,.89,.76,.88,.3])


if __name__=="__main__":
    for choice in sys.argv[sys.argv.index("--")+1:]:render_doll(choice)
