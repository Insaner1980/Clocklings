"""Reference-coordinate 3D sculpts for the four unicorn transformations."""
import math
from pathlib import Path
import sys

import bpy
sys.path.insert(0,str(Path(__file__).resolve().parent))
from fantasy_sculpt import *
from mathutils import Vector
def soap_bubble(x,y,z,r):
    mat=paint("D3B8EF",.16,.12).copy()
    mat.name="Iridescent, transparent bubble film"
    nt=mat.node_tree
    shader=next(n for n in nt.nodes if n.type=="BSDF_PRINCIPLED")
    shader.inputs["Coat Weight"].default_value=.6
    shader.inputs["Thin Film IOR"].default_value=1.38
    shader.inputs["Thin Film Thickness"].default_value=380
    fresnel=nt.nodes.new("ShaderNodeFresnel")
    fresnel.inputs["IOR"].default_value=1.21
    ramp=nt.nodes.new("ShaderNodeValToRGB")
    ramp.color_ramp.elements[0].color=(.025,.025,.025,1)
    ramp.color_ramp.elements[1].color=(.55,.55,.55,1)
    nt.links.new(fresnel.outputs[0],ramp.inputs[0])
    clear=nt.nodes.new("ShaderNodeBsdfTransparent")
    mix=nt.nodes.new("ShaderNodeMixShader")
    nt.links.new(ramp.outputs[0],mix.inputs[0])
    nt.links.new(clear.outputs[0],mix.inputs[1])
    nt.links.new(shader.outputs[0],mix.inputs[2])
    nt.links.new(mix.outputs[0],next(n for n in nt.nodes if n.type=="OUTPUT_MATERIAL").inputs["Surface"])
    ball("Floating soap bubble",(x,y,z),(r,r,r),mat)
    for a0,a1,c in [(17,68,"FFF7CE"),(74,123,"F2B5E8"),(141,186,"BDE8FC"),(202,262,"8CDBEA"),(278,322,"ECBADD")]:
        pts=[]
        for i in range(9):
            ang=math.radians(a0+(a1-a0)*i/8)
            pts.append((x+r*.957*math.cos(ang),y-r*.26,z+r*.957*math.sin(ang)))
        tube("Soap-film rim reflection",pts,r*.040,c,[.10,.55,.84,.98,1,.95,.85,.54,.10])
    sparkle=ball("Bright curved bubble highlight",(x-r*.45,y-r*.70,z+r*.51),(.12*r,.045*r,.24*r),"FFFFFF")
    sparkle.rotation_euler.y=math.radians(38)

COAT="F7DFE9"
MUZZLE="FFD8BC"
BLUSH="F39BB9"
PINK="EC5CAD"
LILAC="AD64DB"
BLUE="68AAEC"
GOLD="FFBE3D"


def coat_body(outline_pts,center,thickness,legs,upper=None):
    torso=pillow("Unicorn barrel and neck",outline_pts,6,thickness,COAT,center,coat=True)
    shapes=[torso]
    for i,(knots,d) in enumerate(legs):
        x,y,r=knots[0];cx,cy=center
        lead=[(x+.36*(cx-x),y-15,.5),(x+.17*(cx-x),y-9,r*.71)]
        leg=limb("Foal leg "+str(i+1),lead+knots,d,.91,COAT,True)
        shapes.append(leg)
    if upper:shapes.extend(upper)
    return mark_skin(fuse([o.name for o in shapes],"Sculpted foal body and posed legs",.014))


def hoof(top,bottom,w,d):
    x,y=top;bx,by=bottom
    upper=Vector(world(x,y,d))
    lower=Vector(world(bx,by,d-3.6))
    axis=(lower-upper).normalized()
    across=axis.cross(Vector((0,1,0))).normalized()
    forward=across.cross(axis).normalized()
    verts=[];faces=[];sides=48
    profile=[(-.10,.78),(-.03,.91),(.10,.98),(.52,1.00),(.77,1.035),
             (.90,1.01),(.98,.92),(1.0,.72)]
    for h,r in profile:
        for i in range(sides):
            a=math.tau*i/sides
            center=upper.lerp(lower,h)
            verts.append(center+across*(w/100*r*math.cos(a))
                         +forward*(w/100*(r*.90*math.sin(a)-.085*h)))
    for j in range(len(profile)-1):
        faces.extend((j*sides+i,j*sides+(i+1)%sides,(j+1)*sides+(i+1)%sides,
                      (j+1)*sides+i) for i in range(sides))
    faces.extend([tuple(reversed(range(sides))),tuple(range(len(verts)-sides,len(verts)))])
    mesh("Rounded lilac foal hoof",verts,faces,mat("B66DD4",.42))


def tform(pts,xf):
    return [xf(x,y) for x,y in pts]


def forlock(xf,depth=-25):
    specs=[
        ("Swept left forelock",[(215,145,216,146),(210,123,222,124),(219,101,235,112),
                                  (241,86,253,107),(266,81,276,103),(282,82,290,101)],
         PINK),
        ("Deep forelock curl",[(280,88,293,96),(267,92,295,107),(254,107,284,119),
                               (250,128,270,135),(255,148,264,146),(260,153,261,153)],
         "F36EC0"),
        ("Parted right forelock",[(284,94,293,111),(303,77,307,115),(325,74,328,119),
                                  (348,83,345,129),(366,104,360,139),(369,127,366,141)],
         LILAC),
    ]
    for i,(name,profiles,c) in enumerate(specs):
        sec=[(*xf(x,y),*xf(u,v)) for x,y,u,v in profiles]
        strip(name,sec,depth-i*.8,.77,c)


def face_details(config):
    o=face("Young unicorn face",config["shape"],config["center"],-4,config["bulge"],COAT,
           config["nose"],config["cheeks"],MUZZLE,BLUSH)
    for i,(boundary,lid,iris,pupil,glint) in enumerate(config["eyes"]):
        eye(("Far" if i==0 else "Near")+" violet eye",o,boundary,lid,iris,pupil,glint,"9959BF","372337")
    for brow in config["brows"]:
        stroke("Arched lilac eyebrow",o,brow,1.85,"9563A5",[.2,1,.18],.8)
    for x,y,r in config["nostrils"]:
        patch("Peach nostril",o,x,y,r*.72,r,"D79986")
    if "opening" in config:
        mouth=pillow("Open joyful smile",config["opening"],-25,.65,"512536",
                     front=lambda x,y:surface(o,x,y)-.62,rings=12)
        pillow("Little pink tongue",config["tongue"],-25,.3,"F593A3",
               front=lambda x,y:surface(mouth,x,y)-.54,rings=8)
    else:
        stroke("Smiling mouth",o,config["smile"],1.32,"58313E",[.2,.85,1,.18],1.0)
    return o


def sea_face():
    return face_details(dict(
        shape=[(194,195),(213,163),(222,132),(244,105),(282,91),(328,96),(356,120),(373,157),
               (369,189),(355,221),(332,243),(307,255),(271,259),(231,248),(201,231),(191,214)],
        center=(284,178),bulge=46,nose=(216,212,55,35),cheeks=[(308,224,19,13),(196,196,9,9)],
        nostrils=[(205,202,2.6),(229,208,3.1)],smile=[(209,225),(219,230),(229,231),(242,222)],
        eyes=[
            ([(211,183),(214,165),(221,151),(229,145),(235,146),(236,156),(231,173),(222,184)],
             [(211,180),(214,161),(225,147),(232,145),(235,149)],(222,165,10,18),(221,163,6.1,10.6),(225,158,3.4)),
            ([(267,201),(271,183),(281,168),(296,163),(309,166),(317,178),(317,193),(311,207),(290,210),(274,207)],
             [(266,201),(271,181),(283,166),(299,162),(313,170),(320,183),(316,202)],
             (286,184,15.8,23),(284,181,9.8,14),(287,176,5.1))],
        brows=[[(227,134),(234,127),(242,128)],[(305,139),(316,142),(326,154)]]))


def sea_mane():
    strip("Lavender wave",[(322,86,336,142),(352,85,354,152),(382,91,376,163),(410,110,406,175),
                            (445,138,441,185),(469,146,469,178),(481,141,481,153)],14,.66,LILAC)
    strip("Long sky-blue wave",[(343,124,343,181),(370,124,368,219),(402,146,401,251),(437,174,434,253),
                                (475,186,472,239),(505,198,503,239),(517,228,516,242)],7,.63,BLUE)
    strip("Magenta neck curl",[(333,182,322,229),(352,199,333,271),(378,230,356,302),(404,249,384,308),
                               (429,246,417,303),(451,247,438,316),(457,278,445,316),(453,300,448,311)],15,.67,PINK)


def sea():
    begin("unicorn")
    sea_mane()
    ear("Far velvet ear",[(228,92),(227,68),(234,49),(247,51),(269,80),(260,97)],
        [(232,84),(234,62),(240,59),(254,80),(251,86)],3,8)
    ear("Near velvet ear",[(353,100),(371,78),(393,67),(403,74),(409,98),(402,129),(391,143),(365,137)],
        [(364,105),(378,84),(391,77),(397,86),(395,110),(385,129),(368,125)],-3,9)
    horn((301,87),(306,18),20,0)
    sea_face()
    forlock(lambda x,y:(x,y),-49)
    # One continuous swept body curves down from the neck and back up into both flukes.
    knots=[(288,239,28),(294,279,35),(312,315,42),(333,350,48),(353,393,51),
           (357,443,53),(343,486,50),(319,521,45),(283,541,31),(238,540,25),
           (204,522,21),(184,496,17),(179,475,13)]
    body=limb("Continuous mermaid chest, flank and curling tail",knots,8,.83,"49CDC6")
    # The neck and tail share the same mesh, including the gradual change of pigment.
    coat=rgba(COAT);mint=rgba("48D0C7");pale=rgba("7FE0D3")
    m=body.data.materials[0].copy()
    body.data.materials[0]=m
    modifier=body.modifiers.new("Continuous scale relief","SUBSURF")
    modifier.subdivision_type="SIMPLE";modifier.levels=2
    bpy.context.view_layer.objects.active=body
    bpy.ops.object.modifier_apply(modifier=modifier.name)
    layer=body.data.color_attributes.new(name="Sea pigments",type="FLOAT_COLOR",domain="POINT")
    vals=[]
    for vert in body.data.vertices:
        y=600-vert.co.z*100
        x=300+vert.co.x*100
        color=blend(blend(coat,pale,softstep(313,387,y)),mint,softstep(359,440,y))
        toward_camera=max(0,-vert.normal.y)
        relief=0.0
        highlight=0.0
        for cx,cy,w in [(378,371,19),(363,399,22),(377,431,20),(351,454,22),
                        (358,478,19),(328,497,21),(301,518,19)]:
            u=(x-cx)/w
            if abs(u)>=1: continue
            v=y-cy-10*(1-u*u)
            ends=(1-u*u)**.65
            relief+=ends*(3.2*math.exp(-((v+2)/4)**2)-1.3*math.exp(-((v-3)/3)**2))
            highlight+=ends*.47*math.exp(-((v+1)/4.2)**2)
        vert.co.y-=toward_camera*relief/100
        vals.append(blend(color,rgba("CDF0DF"),highlight*toward_camera))
    body.data.update()
    layer.data.foreach_set("color",np.array(vals,dtype=np.float32).reshape(-1))
    node=m.node_tree.nodes.new("ShaderNodeVertexColor");node.layer_name=layer.name
    shader=next(n for n in m.node_tree.nodes if n.type=="BSDF_PRINCIPLED")
    m.node_tree.links.new(node.outputs["Color"],shader.inputs["Base Color"])
    for knots,d,top,bottom,w in [
        ([(290,308,22),(252,308,19),(231,331,18)],2,(232,326),(228,352),20),
        ([(322,323,25),(291,324,22),(274,349,20)],-17,(278,340),(270,368),21)]:
        x,y,r=knots[0]
        o=limb("Raised foreleg",[(x+8,y-11,.5),(x+4,y-9,r*.55)]+knots,d,.92,COAT,True)
        hoof(top,bottom,w,d)
    c1=rgba("4FD7D0");c2=rgba("D98EE9")
    def fin_col(x,y):return blend(c2,c1,softstep(385,468,y))
    left_fluke=strip("Wide left sea-glass fluke",[(102,365,104,371),(129,361,102,396),(156,373,100,425),
        (179,394,109,454),(187,422,132,478),(186,455,158,490),(182,482,170,495)],7,.40,"69DCD5",color_fn=fin_col)
    right_fluke=strip("Wide right sea-glass fluke",[(178,489,187,494),(179,463,200,482),(184,433,223,471),
        (202,407,245,455),(226,397,258,435),(252,396,262,414),(264,403,264,407)],6,.43,"75D7D2",color_fn=fin_col)
    for fluke,paths in [(left_fluke,[[(175,481),(151,451),(129,415),(110,380)],
            [(178,482),(169,448),(151,406),(131,378)]]),(right_fluke,[
            [(183,480),(203,451),(228,425),(252,407)],[(184,485),(214,466),(241,440)]])]:
        for points in paths:
            stroke("Pearly fin ray",fluke,points,1.55,"AFEAE4",[.16]+[1]*(len(points)-2)+[.12],.42)
    # soap_bubble uses world units and transparency, while the surface is fully three-dimensional.
    x,d,z=world(166,162,-12)
    soap_bubble(x,d,z,.445)


def carousel_face():
    return face_details(dict(
        shape=[(120,169),(132,146),(151,128),(179,118),(209,124),(230,137),(246,158),
               (251,184),(246,207),(234,226),(214,243),(183,252),(157,250),(131,238),(122,223)],
        center=(184,187),bulge=35,nose=(155,227,36,22),cheeks=[(216,216,12,9),(130,218,7,7)],
        nostrils=[(145,225,2.2),(166,224,2.2)],smile=[(144,236),(154,241),(166,239),(175,232)],
        eyes=[
            ([(126,203),(126,190),(131,181),(140,181),(144,191),(145,202),(141,212),(133,214)],
             [(126,202),(126,190),(132,181),(139,181),(144,187)],(137,197,7.1,14),(137,195,4.25,8.6),(138,190,2.7)),
            ([(178,203),(181,187),(188,177),(198,173),(207,177),(211,188),(210,202),(204,209),(194,212),(184,210)],
             [(178,200),(182,184),(192,175),(201,175),(209,182),(211,193)],
             (197,194,12.8,16.5),(196,192,8.2,10.7),(198,185,3.8))],
        brows=[[(128,166),(134,160),(140,161)],[(191,154),(201,155),(210,162)]]))


def carousel():
    begin("unicorn")
    # Lathed plinth with an angled top and softly rounded enamel edges.
    rings=[(537,0),(538,118),(533,128),(527,130),(522,127),(519,124),(491,122),(487,121),
           (485,124),(478,122),(473,116),(473,102),(473,0)]
    sides=96;vs=[];fs=[]
    for y,r in rings:
        for i in range(sides):
            a=math.tau*i/sides
            vs.append(world(289+r*math.cos(a),y+r*.18*math.sin(a),19-r*math.sin(a)))
    for j in range(len(rings)-1):
        for i in range(sides):
            fs.append((j*sides+i,j*sides+(i+1)%sides,(j+1)*sides+(i+1)%sides,(j+1)*sides+i))
    base=mesh("Turned three-colour carousel plinth",vs,fs,GOLD)
    base.data.materials.append(mat("13B8B4"))
    base.data.materials.append(mat("FFDEB6"))
    for i,p in enumerate(base.data.polygons):
        p.material_index=1 if i//sides in (5,6) else (2 if i//sides>=10 else 0)
    cylinder("Ivory pole",(289,473,25),(297,45,25),15,"FFE4BB")
    vs=[];fs=[]
    for j in range(600):
        u=j/599*8.4
        a=math.tau*u
        for off in (0,23):
            vs.append(world(289+u+15.25*math.cos(a),min(472,max(45,472-u*53.4+off)),25+15.25*math.sin(a)))
        if j:fs.append((2*j-2,2*j-1,2*j+1,2*j))
    mesh("Broad spiral gilding on carousel pole",vs,fs,mat(GOLD,.26,.15))
    ell("Golden pole finial",296,44,24,26.5,25.5,25.5,mat(GOLD,.22,.42))
    # Ribbons sit behind the rump and lift in independently curved arcs.
    for name,off,c in [("Cerise",0,"E74B69"),("Turquoise",26,"21C5C5"),("Golden",53,"FFC34D")]:
        strip(name+" streaming carousel ribbon",[(354,244+off*.30,356,250+off*.30),(385,233+off*.50,388,248+off*.50),
            (423,193+off*.73,425,217+off*.71),(461,180+off*.9,458,202+off*.9),(483,183+off,484,207+off),
            (497 if not off else 527,194+off,493 if not off else 519,214+off)],19,.40,c)
    strip("Outer lilac curled tail",[(347,272,350,300),(390,257,379,307),(431,258,402,313),
         (461,286,427,324),(466,309,437,330),(457,318,448,326)],16,.66,LILAC)
    strip("Rose toy tail curl",[(359,282,356,299),(401,268,391,306),(438,283,405,315),(455,307,413,329),
         (454,326,425,349),(465,333,445,359),(477,321,466,354),(482,329,477,345)],7,.63,PINK)
    strip("Sky-blue inner tail",[(359,284,354,307),(393,279,379,315),(424,289,394,330),(436,309,404,345),
         (433,332,420,354),(448,351,432,355)],3,.63,BLUE)
    coat_body([(177,234),(208,218),(250,238),(280,263),(331,261),(365,269),(389,293),(389,330),
               (368,352),(334,358),(294,353),(266,359),(227,354),(196,344),(179,319),(173,286)],(274,297),30,
              [([(207,306,19),(164,306,14),(147,309,15),(127,341,14)],9),
               ([(222,318,19),(204,329,17),(181,341,15),(171,378,17)],-18),
               ([(326,329,22),(341,351,17),(356,368,15),(376,398,16)],13),
               ([(363,326,23),(379,347,18),(400,357,14),(430,391,16)],-7)])
    for top,bottom,w,d in [((126,331),(120,352),18,9),((171,370),(173,394),18,-18),
                           ((371,390),(383,409),16,13),((424,379),(439,402),16,-7)]:
        hoof(top,bottom,w,d)
    for off,c in [(0,GOLD),(7,"16BDB7")]:
        outline_pts=[(250,261),(269,258),(311,267),(356,252),(367,273),(377,305),(374,321),
                     (362,324),(355,338),(342,344),(328,344),(314,337),(301,345),(284,346),
                     (275,340),(270,328),(254,327),(243,317),(241,300)]
        if off:outline_pts=[(310+(x-310)*.89,293+(y-293)*.84) for x,y in outline_pts]
        pillow("Scalloped saddle blanket",outline_pts,-33-off*.65,7,c,(309,300))
    strip("Cerise saddle seat",[(256,244,255,256),(269,243,269,273),(297,248,299,284),
           (320,244,326,284),(341,224,349,278),(358,218,363,268),(369,224,372,254),(376,237,376,243)],-22,1.12,"E74765")
    golden_star("Raised saddle star",313,307,24,-50)
    for y,rx,ry in [(480,116,20),(521,126,22)]:
        line("Rolled gilded plinth moulding",[(289+rx*math.cos(a),y+ry*math.sin(a),
             19-rx*math.sin(a)) for a in [math.tau*i/96 for i in range(96)]],
             1.6,mat("DF9C2E",.29,.32),closed=True)
    strip("Sky-blue curled mane",[(212,121,214,150),(240,125,231,166),(266,144,240,192),
          (285,169,239,211),(285,191,244,235),(268,217,256,250),(261,232,260,250)],0,.61,BLUE)
    strip("Lower rose mane curl",[(226,186,210,199),(244,208,210,235),(247,237,207,263),
          (229,264,207,279),(216,277,213,282)],9,.56,PINK)
    strip("Upper lilac mane",[(207,120,211,135),(244,111,244,148),(273,126,261,157),(287,150,277,162)],12,.56,LILAC)
    ear("Far ear",[(120,148),(119,115),(122,95),(132,94),(154,122),(152,146)],
        [(125,132),(125,104),(130,102),(143,122),(140,135)],-1,6)
    ear("Near ear",[(225,118),(236,99),(251,88),(261,97),(265,119),(259,141),(247,149)],
        [(233,120),(242,105),(252,98),(256,106),(255,126),(248,139)],-2,7)
    horn((182,117),(176,53),17,0)
    carousel_face()
    forlock(lambda x,y:(110+(x-210)*.80,96+(y-77)*.99),-38)


def golden_star(name,cx,cy,r,depth):
    points=[]
    for i in range(80):
        a=math.tau*i/80
        five=math.cos(5*(a+math.pi/2))
        radius=r*(.70+.24*five+.045*(2*five*five-1))
        points.append((cx+radius*math.cos(a),cy+radius*math.sin(a)))
    return pillow(name,points,depth,r*.31,mat("FFCA50",.37,.09),(cx,cy),rings=32)


def shooting_face():
    return face_details(dict(
        shape=[(33,164),(46,134),(55,106),(75,88),(106,80),(139,85),(164,104),(178,134),
               (179,164),(170,193),(151,215),(124,227),(95,231),(67,225),(44,209),(31,191)],
        center=(105,153),bulge=40,nose=(61,188,43,31),cheeks=[(121,199,14,10),(34,169,8,9)],
        nostrils=[(52,171,2.3),(75,180,2.8)],
        opening=[(49,184),(57,188),(69,190),(80,188),(88,185),(82,201),(73,211),(64,214),(57,207),(53,196)],
        tongue=[(58,205),(65,201),(73,200),(80,201),(72,210),(65,212)],
        eyes=[([(44,158),(46,145),(53,132),(64,125),(69,128),(69,136),(64,151),(56,160)],
               [(44,158),(46,144),(54,131),(65,125),(70,129)],(55,145,9.0,15),(56,142,5.6,9.1),(60,135,3.3)),
              ([(98,175),(102,159),(112,148),(123,145),(135,148),(141,158),(140,171),(134,182),(120,185),(105,180)],
               [(97,173),(102,157),(114,146),(126,145),(136,149),(142,159),(140,169)],
               (116,163,13.3,18.1),(116,159,8.2,11.5),(120,155,4.0))],
        brows=[[(59,118),(68,110),(76,111)],[(131,126),(143,130),(149,140)]]))


def shooting_star():
    begin("unicorn")
    # Three ribbons widen at independent points along the trail, then taper to tips.
    strip("Upper rose comet ribbon",[(179,299,179,309),(226,278,233,302),(274,267,277,310),(334,276,321,332),
        (380,313,367,361),(431,352,424,385),(487,363,481,390),(527,376,518,399),
        (561,407,549,412),(578,441,577,443)],21,.56,"F48EC0")
    strip("Lower lavender comet ribbon",[(184,296,186,307),(224,296,221,322),(269,319,251,368),
        (318,378,296,425),(376,425,355,463),(428,458,413,488),(491,478,479,499),
        (538,493,529,501),(568,515,568,517)],18,.59,"B779E1")
    strip("Golden comet ribbon",[(182,299,182,307),(227,289,230,317),(274,291,268,337),(324,314,312,375),
        (381,358,359,410),(437,386,420,437),(490,402,479,451),(536,433,523,469),
        (568,475,557,488),(578,511,576,514)],-1,.55,"FFCA49")
    star=golden_star("Golden shooting star",504,435,57,-36)
    # Each rounded point catches a broad highlight instead of collapsing into a dark facet.
    for face in star.data.polygons: face.use_smooth=True
    shader=next(n for n in star.active_material.node_tree.nodes if n.type=="BSDF_PRINCIPLED")
    shader.inputs["Coat Weight"].default_value=.24
    shader.inputs["Coat Roughness"].default_value=.26
    coat_body([(79,214),(113,211),(130,227),(142,253),(168,268),(192,277),(210,292),(218,313),
               (214,335),(197,349),(175,349),(151,339),(122,331),(106,316),(84,308),(74,285)],(133,285),26,
              [([(91,284,19),(65,271,16),(51,271,13),(46,299,15)],9),
               ([(119,295,21),(98,293,19),(85,299,16),(85,326,18)],-15),
               ([(151,323,18),(163,347,14),(177,360,12),(193,397,15)],13),
               ([(193,323,23),(208,349,18),(225,363,14),(249,399,17)],-8)])
    for a,b,w,d in [((45,295),(48,318),17,9),((85,325),(92,350),19,-15),
                    ((191,388),(202,408),15,13),((243,392),(258,414),17,-8)]:hoof(a,b,w,d)
    strip("Lilac flying mane",[(143,94,145,128),(173,100,166,147),(208,111,190,168),
        (242,142,219,183),(268,152,253,184),(282,143,276,175),(285,155,282,164)],9,.61,LILAC)
    strip("Sky-blue flowing mane",[(153,127,145,171),(182,144,157,207),(208,163,179,226),
        (241,180,216,237),(274,177,253,229),(299,178,279,213),(314,187,301,207),(324,209,323,213)],7,.61,BLUE)
    strip("Lower pink flying mane",[(143,177,132,208),(171,200,140,243),(198,222,166,262),
        (228,226,198,267),(254,213,232,254),(267,211,254,238)],14,.68,PINK)
    ear("Far ear",[(59,86),(62,66),(75,47),(85,46),(97,66),(104,87)],
        [(67,80),(72,62),(80,54),(87,62),(94,80)],1,7)
    ear("Near ear",[(175,111),(189,95),(211,84),(222,87),(224,105),(215,131),(201,144),(181,139)],
        [(183,113),(199,97),(214,92),(217,98),(212,118),(201,132),(189,132)],-2,8)
    horn((123,80),(139,17),19,-1)
    shooting_face()
    forlock(lambda x,y:(42+(x-210)*.95,72+(y-77)*.76),-43)


def floral_face():
    return face_details(dict(
        shape=[(122,192),(128,164),(152,135),(184,121),(222,118),(256,129),(284,151),(302,183),
               (304,213),(293,248),(274,276),(244,296),(218,311),(188,315),(166,310),
               (151,298),(142,278),(132,256),(125,227)],
        center=(210,212),bulge=47,nose=(181,280,47,35),cheeks=[(251,258,17,12),(145,250,9,9)],
        nostrils=[(172,273,2.6),(194,278,3.1)],smile=[(174,300),(188,303),(201,298),(209,287)],
        eyes=[([(132,224),(134,216),(142,214),(151,221),(159,234),(163,248),(159,261),(149,260),(139,249)],
               [(132,233),(132,219),(137,213),(144,214),(154,227),(161,245)],
               (152,242,10.7,17.7),(154,241,6.6,10.8),(151,233,3.4)),
              ([(200,228),(203,213),(213,200),(227,196),(240,201),(249,213),(253,233),(250,246),
                (235,254),(219,254),(206,246)],
               [(200,230),(204,212),(215,199),(229,196),(241,201),(249,212),(253,230)],
               (215,237,15.3,20.3),(214,236,9.7,12.5),(210,226,5.0))],
        brows=[[(125,201),(129,188),(136,185)],[(212,170),(225,167),(238,173)]]))


def daisy(x,y,d,r):
    for i in range(6):
        a=math.tau*(i+.2)/6
        cx=x+r*.57*math.sin(a);cy=y-r*.57*math.cos(a)
        p=ell("Ivory daisy petal",cx,cy,d,r*.30,r*.50,r*.23,"FFF5DE" if i%2 else "FFF9E9")
        p.rotation_euler.y=a
    ell("Golden pollen center",x,y,d-r*.20,r*.33,r*.32,r*.25,"FFC537")
    for i in range(7):
        a=math.tau*i/7
        ell("Rounded pollen grain",x+r*.17*math.cos(a),y+r*.17*math.sin(a),
            d-r*.419,r*.041,r*.043,r*.029,"EEB33A")


def leafy(name,a,b,w,d,col):
    ax,ay=a;bx,by=b
    dx=bx-ax;dy=by-ay;l=math.hypot(dx,dy)
    nx=-dy/l;ny=dx/l
    sections=[]
    for t,r in ((0,.035),(.27,.77),(.55,1),(.76,.70),(.95,.15),(1,.015)):
        x=ax+dx*t+ny*w*.25*math.sin(math.pi*t)
        y=ay+dy*t-nx*w*.25*math.sin(math.pi*t)
        sections.append((x+nx*w*r,y+ny*w*r,x-nx*w*r,y-ny*w*r))
    leaf=strip(name,sections,d,.47,col)
    if "mane" in name.lower():
        ridge=[((x+u)*.5,(y+v)*.5) for x,y,u,v in sections[1:-1]]
        stroke("Fold down leafy mane lock",leaf,ridge,1.22,"A5C74D",[.18,.82,1,.16],.66)
    return leaf


def flower():
    begin("unicorn")
    limb("Curved living tail",[(357,353,13),(392,361,14),(419,355,12),(437,336,9),
                              (448,305,8),(465,274,6),(474,221,3)],13,.89,"80AB34")
    for p in [[(437,330,7),(452,307,6),(482,284,3)],[(437,326,7),(430,305,5),(429,276,4)]]:
        limb("Bloom stem",p,14,.8,"80AA31")
    for i,(a,b,w) in enumerate([((447,310),(499,352),15),((460,253),(542,238),18),((459,240),(398,220),17),
                                  ((436,310),(416,332),8)]):
        leafy("Garden leaf",a,b,w,14,"80B137" if i%2 else "8FB941")
    for x,y,r in [(471,207,35),(431,264,34),(499,271,33)]:daisy(x,y,2,r)
    coat_body([(202,292),(223,284),(246,296),(262,325),(305,328),(351,332),(380,340),(395,362),
               (397,389),(383,412),(353,429),(310,437),(277,433),(244,427),(216,421),(200,404),
               (190,382),(191,344)],(283,373),35,
              [([(219,394,23),(198,415,20),(181,435,18),(180,477,22)],11),
               ([(251,404,25),(252,434,23),(246,465,21),(241,517,24)],-18),
               ([(309,412,24),(321,441,19),(324,469,18),(319,505,22)],16),
               ([(365,396,29),(367,433,24),(383,459,18),(392,508,23)],-6)])
    for a,b,w,d in [((180,469),(185,497),24,11),((242,509),(242,537),26,-18),
                    ((318,499),(317,525),24,16),((392,506),(393,533),25,-6)]:hoof(a,b,w,d)
    # Successive leaf-shaped locks frame the face, passing behind individual flowers.
    for i,(a,b,w,d) in enumerate([((200,103),(111,198),29,-12),((234,97),(360,171),26,15),
            ((255,115),(350,277),32,14),((328,194),(306,325),32,-8),((303,263),(245,391),26,-39),
            ((322,234),(245,341),21,-41)]):
        leafy("Foliage mane lock",a,b,w,d,"7FA938" if i%2 else "91BA38")
    ear("Far ear",[(113,130),(111,91),(117,68),(131,69),(155,87),(167,115),(153,137)],
        [(119,118),(118,88),(124,77),(137,87),(151,111),(147,121)],1,10)
    ear("Near ear",[(270,112),(288,81),(307,65),(321,75),(331,103),(330,137),(318,155),(287,144)],
        [(283,112),(299,88),(310,79),(319,95),(319,125),(313,137),(291,132)],-2,10)
    horn((204,106),(186,16),26,0)
    floral_face()
    strip("Sky-blue mane beside the flowers",[(299,132,301,133),(327,141,307,154),
          (349,161,315,179),(358,189,318,205),(354,220,312,233),(336,247,302,250),
          (315,262,299,263),(291,272,291,272)],-48,.72,BLUE)
    forlock(lambda x,y:(89+(x-210)*1.57,98+(y-77)*1.25),-48)
    for x,y,r in [(159,127,36),(261,128,37),(332,217,37),(286,322,41)]:daisy(x,y,-63,r)
    # Soft coral wings cup forward on either side of the tiny body touching the nose.
    for i,(points,inner) in enumerate([
        ([(160,271),(144,257),(127,252),(117,255),(116,265),(127,276),(147,283),(160,278)],(136,267,12,5)),
        ([(160,276),(144,279),(132,285),(126,295),(132,299),(144,296),(157,285)],(141,289,9,4)),
        ([(160,271),(159,260),(165,251),(170,251),(171,259),(165,272)],(166,260,2,4))]):
        pillow("Coral butterfly wing",points,-64,3.2,"F06B5E")
        x,y,rx,ry=inner
        dot=ell("Peach butterfly wing marking",x,y,-68,rx,ry,1.3,"FFAA8E")
        dot.rotation_euler.y=.55 if i==1 else -.46
    ell("Butterfly velvet body",161,273,-69,3.7,6.2,2.8,"B85344")
    for sign in (-1,1):
        line("Butterfly antenna",[(161,269,-69),(164+sign*2,259,-68),(172+sign*5,253,-68)],1.05,"A54E46",[.9,.76,.28])
        ell("Butterfly antenna tip",172+sign*5,253,-68,2.5,2.5,1.8,"DF6B57")


BUILDERS={"unicorn-model-sea":sea,"unicorn-model-carousel":carousel,
          "unicorn-model-shooting-star":shooting_star,"unicorn-head-butterfly":flower}

if __name__=="__main__":
    for item in sys.argv[sys.argv.index("--")+1:]:
        BUILDERS[item]()
        save_render(item)


