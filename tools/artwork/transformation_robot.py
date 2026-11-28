"""Sculpt and render any of the cuckoo, rover, bubble and driver robot rewards."""
from math import cos, pi, sin, tau
from pathlib import Path
import sys
import bpy
from mathutils import Vector

sys.path.insert(0,str(Path(__file__).resolve().parent))
from transformation_shapes import (
    at, box, ellipse_points, line, mesh_object, oval, patch, pigment, pill, ring_mesh,
    save_render, seam, since, smooth_outline, solid, studio, surface, tags, axis,
)


def mechanical(name,a,b,radii,material):
    start,end=at(*a),at(*b);v=end-start
    cross=v.normalized().cross(Vector((0,1,0))).normalized()
    other=v.normalized().cross(cross).normalized()
    n=48;points=[];faces=[]
    for t,r in radii:
        mid=start+t*v
        points += [mid+(cos(i*tau/n)*cross+sin(i*tau/n)*other)*r/200 for i in range(n)]
    for j in range(len(radii)-1):
        faces.extend((j*n+i,j*n+(i+1)%n,(j+1)*n+(i+1)%n,(j+1)*n+i) for i in range(n))
    faces += [tuple(range(n)),tuple(reversed(range((len(radii)-1)*n,len(radii)*n)))]
    return mesh_object(name,points,faces,material)


def spindle(name,a,b,r1,r2,m):
    return mechanical(name,a,b,[(0,r1*.88),(.035,r1),(.12,r1*1.015),(.83,r2*1.01),(.97,r2),(1,r2*.88)],m)


def head(shape,screen,ears,eyes,cheeks,smile,gaze,shell,joint,glass,led,thickness,depth=0,brows=False,small=False):
    for i,(x,y,rx,ry) in enumerate(ears):
        oval("Receiver earpiece "+str(i),(x,y,depth+5),(rx,ry,rx*.99),joint)
    casing=solid("Sea glass head casing",shape,depth,thickness,shell,.47)
    visor=patch("Curved midnight display",screen,casing,glass,2.1,8)
    before=set(bpy.context.scene.objects)
    glow=pigment("Pure LED reflection","E6FFF3",.22)
    pupil=pigment("Eye tracking dark teal","174B52",.26)
    for i,(x,y,rx,ry,angle) in enumerate(eyes):
        eye=patch("Lit mint eye "+str(i),ellipse_points(x,y,rx,ry,angle=angle),visor,led,2,2)
        if gaze is not None:
            dx,dy=gaze[i] if isinstance(gaze[0],tuple) else gaze
            ex,ey=x+dx*rx*.82,y+dy*ry*.82
            patch("Pupil "+str(i),ellipse_points(ex,ey,rx*.33,ry*.32,angle=angle),eye,pupil,2,1)
        if not small:
            gx,gy=(x-rx*.20,y-ry*.49) if gaze is None else (x+(gaze[i][0] if isinstance(gaze[0],tuple) else gaze[0])*rx*.6,y-ry*.05)
            patch("LED glint "+str(i),ellipse_points(gx,gy,rx*.239,ry*.22),eye,glow,2,1)
    if not small:tags(since(before),"blink_part")
    for x,y,rx,ry,ang in cheeks:
        patch("Rosy status light",ellipse_points(x,y,rx,ry,angle=ang),visor,pigment("Berry display blush","AC5678",.37),1.3,1.0)
    coords=[(x,y,surface(visor,x,y)-3.8) for x,y in smile]
    r=(2.6 if small else 4.35)
    line("Curved LED smile",coords,r,led)
    for p in [coords[0],coords[-1]]:
        oval("Rounded smile corner",p,(r,r,r),led)
    if brows:
        for x,y,rx,ry,ang in eyes:
            upper=[(x-rx*.95,y-ry*1.80),(x,y-ry*2.12),(x+rx*.66,y-ry*1.99)]
            seam("Subtle display eyebrow",[(a,b,surface(visor,a,b)-1.8) for a,b in upper],3.9,glass)
    return casing


def hand(name,x,y,s,r,shell):
    pts=[(-.56,-.94),(.04,-1.03),(.58,-.83),(.86,-.48),(.98,-.02),(.88,.37),(.67,.80),(.37,1),(-.16,1.01),(-.57,.81),(-.90,.45),(-.98,.00),(-.86,-.55)]
    solid(name,[(x+s*px*r,y+py*r) for px,py in pts],-21,r*.87,shell,.86)
    thumb=oval(name+" folded thumb",(x+s*.55*r,y+.15*r,-r*.63-20),(r*.32,r*.48,r*.29),shell)
    thumb.rotation_euler.y=s*.30


def legs(coords,shell,joint):
    for i,(hip,ankle,boot) in enumerate(coords):
        hx,hy=hip;ax,ay=ankle
        rad=(max(p[0] for p in boot)-min(p[0] for p in boot))*.23
        spindle("Shin", (hx,hy,11),(ax,ay,7),rad*1.09,rad,shell)
        spindle("Ankle ring",(ax+(hx-ax)*.07,ay-14,4),(ax,ay+10,3),rad*1.025,rad*1.03,joint)
        solid("Rounded moulded boot "+str(i),boot,-13,rad*1.52,shell,.58)


def arm(a,b,c,r,s,shell,joint):
    # Shoulder cup, barrel and wrist ring nest into one another along their joint axis.
    ax,ay=a;bx,by=b;cx,cy=c
    oval("Shoulder ball",(ax,ay,5),(r*1.03,r*1.08,r*1.02),joint)
    spindle("Tubular upper arm",(ax+(bx-ax)*.13,ay+(by-ay)*.13,3),(bx,by,-10),r*.88,r,shell)
    spindle("Wrist coupling",(bx+(ax-bx)*.09,by+(ay-by)*.09,-11),(bx+(cx-bx)*.43,by+(cy-by)*.43,-14),r*1.007,r*1.06,joint)
    hand("Rounded mitten",cx,cy,s,r*1.32,shell)


def housing_ring(name,outer,inner,front,depth,wall,back):
    # Front rim and inner wall form a through-opening; the seat is mounted on its back panel.
    n=72
    def sample(points):
        border=smooth_outline(points,9)
        if len(border)==n:return list(border)
        return [border[int(i*len(border)/n)] for i in range(n)]
    outside,inside=sample(outer),sample(inner)
    def ring(frac,d):
        return [(a.x+(b.x-a.x)*frac,a.y+(b.y-a.y)*frac,d) for a,b in zip(outside,inside)]
    ring_mesh(name+" rounded rim",[ring(0,front+9),ring(.10,front-2),ring(.40,front-6),ring(.76,front-4),ring(1,front+3),ring(1,front+13),ring(1,depth)],wall)
    return solid(name+" rear wall",inner,depth+5,8,back,.55)


def clock(shell,joint):
    ivory=pigment("Warm ivory enamel","FFE8B7",.29,0,.23); amber=pigment("Honey varnish","E5A442",.28,.16,.29)
    legs([((550,857),(524,941),[(500,943),(543,943),(568,967),(584,1002),(591,1044),(585,1064),(548,1070),(458,1068),(440,1057),(444,1019),(466,971)]),((703,856),(733,942),[(711,940),(747,939),(782,967),(805,1007),(813,1050),(803,1065),(755,1071),(681,1069),(663,1059),(664,1023),(678,976)])],shell,joint)
    arm((483,674),(422,745),(410,804),41,1,shell,joint);arm((770,675),(833,746),(849,803),41,-1,shell,joint)
    solid("Clock torso",[(505,616),(571,603),(696,601),(758,620),(777,682),(782,802),(762,853),(701,877),(553,878),(488,853),(474,786),(477,686)],8,71,shell,.53)
    axis("Pendulum rod",(622,849,-33),(641,945,-34),7,amber)
    oval("Polished pendulum bob",(641,949,-37),(37,37,20),pigment("Golden pendulum","F4BA38",.24,.44,.36))
    for z,r,m in [(-85,140,amber),(-95,125,ivory)]:
        axis("Clock dial", (629,731,z+14),(629,731,z),r,m)
    navy=pigment("Midnight clock hands","144B66",.26,.21)
    for i in range(12):
        a=i*tau/12
        pill("Rounded dial marker",(628+sin(a)*89,731-cos(a)*89,-100),(628+sin(a)*103,731-cos(a)*103,-100),4.7,navy)
    pill("Minute hand",(628,729,-110),(628,642,-110),9.0,navy)
    pill("Hour hand",(628,730,-112),(686,730,-112),10.0,navy)
    oval("Clock axle cap",(628,729,-123),(15,15,8),navy)
    head([(390,381),(428,323),(519,288),(721,291),(810,318),(855,374),(873,469),(856,549),(800,595),(706,608),(529,609),(437,588),(394,538),(381,468)],[(433,439),(454,395),(513,371),(626,365),(739,369),(796,393),(819,433),(821,493),(797,536),(748,551),(610,556),(494,550),(451,524),(435,486)],[(378,458,35,60),(875,458,34,59)],[(546,427,30,34,.13),(708,426,30,34,-.13)],[(498,502,22,16,0),(762,502,22,16,0)],[(596,485),(610,496),(631,500),(648,496),(659,486)],[a for a in ((.43,-.59),(-.40,-.59))],shell,joint,GLASS,LED,84,depth=3)
    wood=pigment("Caramel cuckoo house","C58334",.34,.03,.20);dark=pigment("Inside birdhouse","4E2A1C",.55)
    # Maple shell, deep dark opening, then doors turned outward in front of the hinges.
    cabinet=[(510,241),(521,222),(622,158),(642,158),(736,222),(746,247),(744,345),(731,359),(525,359),(510,347)]
    solid("Carved cuckoo cabinet",cabinet,-84,38,wood,.23)
    aperture=[(529,247),(535,236),(627,175),(639,177),(719,233),(725,245),(724,333),(716,340),(540,340),(530,333)]
    solid("Recess with perch",aperture,-122,6,dark,.25)
    scarlet=pigment("Cherry lacquer roof","DA3538",.29,.05,.30)
    solid("Glossy gable roof",[(472,231),(481,211),(612,128),(629,122),(646,129),(782,218),(791,232),(778,250),(762,252),(634,169),(627,165),(497,254),(481,252)],-113,21,scarlet,.55,center=(630,160))
    for s in (-1,1):
        xx=628+s*155
        coords=[(-36,-61),(-19,-66),(6,-58),(30,-33),(40,-14),(39,56),(31,64),(-28,64),(-39,53)]
        shutter=solid("Open maple shutter",[(xx-s*u,275+v) for u,v in coords],lambda x,y:-142-abs(x-628)*.042,15,wood,.30)
        for shift in (-18,5,23):
            grain=[(xx+shift,252),(xx+shift+3,273),(xx+shift-1,306),(xx+shift,320)]
            seam("Carved grain in maple shutter",[(x,y,surface(shutter,x,y)-.8) for x,y in grain],1.45,pigment("Maple latewood","A16432",.5))
        hx=628+s*113
        for yy in (273,304):
            axis("Brass hinge pivot",(hx,yy-9,-143),(hx,yy+10,-143),5.3,amber)
            oval("Hinge leaf",(hx+s*7,yy,-148),(10,5,2.4),amber)
            oval("Hinge screw",(hx+s*12,yy,-151),(2.6,2.6,1.7),navy)
    yellow=pigment("Canary yellow","FFCC44",.32,0,.28);wing=pigment("Warm cuckoo feathers","F4B734",.36)
    oval("Cuckoo belly",(630,295,-137),(42,44,27),yellow)
    oval("Singing cuckoo head",(627,251,-144),(42,44,29),yellow)
    for s in (-1,1):
        for j,(reach,offset) in enumerate(((48,-18),(44,-4),(33,10))):
            bx=628+s*54;by=285+offset
            w=oval("Layered songbird wing",(bx,by,-151),(reach*.68,11.5,7.7),wing)
            w.rotation_euler.y=s*.43
        for j in (-1,0,1):
            oval("Perching cuckoo toe",(628+s*30+j*8,337,-151),(5.1,9,6.5),pigment("Bird feet and beak","F49623",.28))
    oval("Cuckoo black eye",(622,238,-174),(7.9,8.0,5),"382820");oval("Cuckoo eye reflection",(620,235,-177),(2.1,2.1,1.4),"FFF9E5")
    solid("Opening upper beak",[(639,241),(654,230),(678,226),(676,235),(661,245),(644,250)],-166,7,ORANGE_M)
    solid("Opening lower beak",[(640,249),(653,252),(672,259),(663,267),(649,264),(641,258)],-166,6,ORANGE_M)


def bubble_material(name,soap):
    m=pigment(name,"F0E4F6",.13,0,.30)
    tree=m.node_tree;nodes=tree.nodes;links=tree.links
    p=next(n for n in nodes if n.type=="BSDF_PRINCIPLED")
    p.inputs["Specular IOR Level"].default_value=.42;p.inputs["Emission Strength"].default_value=.16
    facing=nodes.new("ShaderNodeLayerWeight");facing.inputs["Blend"].default_value=.60
    mult=nodes.new("ShaderNodeMath");mult.operation="MULTIPLY_ADD";mult.inputs[1].default_value=.78 if soap else .28;mult.inputs[2].default_value=.039 if soap else .014
    links.new(facing.outputs["Facing"],mult.inputs[0])
    if soap:
        noise=nodes.new("ShaderNodeTexNoise"); noise.inputs["Scale"].default_value=2.45;noise.inputs["Detail"].default_value=.4
        color=nodes.new("ShaderNodeValToRGB")
        for i,(pos,v) in enumerate([(.15,(.17,.93,.99)),(.30,(.49,.64,1)),(.40,(.86,.48,1)),(.51,(1,.43,.77)),(.60,(1,.79,.44)),(.70,(.50,1,.91)),(.82,(.6,.72,1))]):
            e=color.color_ramp.elements[i] if i<2 else color.color_ramp.elements.new(pos)
            e.position=pos;e.color=(*v,1)
        links.new(noise.outputs["Fac"],color.inputs[0]);links.new(color.outputs[0],p.inputs["Base Color"]);links.new(color.outputs[0],p.inputs["Emission Color"])
    mix=nodes.new("ShaderNodeMixShader");clear=nodes.new("ShaderNodeBsdfTransparent")
    links.new(mult.outputs[0],mix.inputs[0]);links.new(clear.outputs[0],mix.inputs[1]);links.new(p.outputs["BSDF"],mix.inputs[2]);links.new(mix.outputs[0],next(n for n in nodes if n.type=="OUTPUT_MATERIAL").inputs[0])
    return m


def bubble(shell,joint):
    legs([((645,854),(635,951),[(601,950),(657,944),(680,960),(693,996),(693,1041),(685,1057),(597,1062),(551,1053),(549,1021),(571,977)]),((766,862),(782,948),[(750,947),(794,944),(817,953),(848,994),(871,1036),(869,1050),(850,1062),(754,1062),(736,1054),(733,1013)])],shell,joint)
    tank=pigment("Clear soap flask","BEECED",.19)
    vessel=solid("Clear soap canister",[(898,678),(924,661),(1007,659),(1044,677),(1069,722),(1072,794),(1064,844),(1038,880),(997,894),(945,891),(907,868),(886,817),(886,731)],28,63,bubble_material("Transparent canister wall",False),.67)
    fluid=solid("Aqua soap in bottle",[(892,765),(924,755),(970,754),(1030,755),(1066,762),(1063,815),(1044,860),(1007,883),(943,879),(912,858),(896,824)],31,54,pigment("Liquid seafoam","28CBD1",.18,.03,.57),.63)
    oval("Soap meniscus",(977,762,-5),(87,10,48),pigment("Liquid highlight","78EBE7",.18,0,.55))
    for x,y,r in ((1001,791,13),(1021,828,17),(983,859,16)):
        dep=surface(fluid,x,y)-1.5
        oval("Bubble suspended in soap",(x,y,dep),(r,r,r*.44),pigment("Small transparent seafoam bead","60DCDD",.14,.10,.52))
        oval("Bead glint",(x-r*.35,y-r*.43,dep-r*.41),(r*.19,r*.19,r*.09),"DFFFF3")
    spindle("Bottle screw cap",(962,629,23),(962,662,23),49,51,shell)
    solid("Bubble robot body",[(617,641),(641,628),(740,625),(808,642),(837,670),(846,724),(841,811),(818,844),(759,861),(651,855),(605,840),(589,820),(586,769),(593,682)],3,74,shell,.48)
    chest=solid("Vanilla chest inset",[(653,682),(694,675),(768,678),(799,692),(816,717),(817,773),(805,794),(779,805),(688,804),(647,796),(630,781),(627,725),(634,694)],-64,17,IVORY,.37)
    for x,y,c in ((681,736,ORANGE_M),(764,739,shell)):
        oval("Enamel pushbutton",(x,y,-84),(27,28,13),c)
    arm((852,700),(886,781),(907,843),40,-1,shell,joint)
    # The wand replaces the entire left hand, with a coupling socket screwed directly to the barrel.
    spindle("Shoulder socket",(590,705,0),(571,702,0),46,42,joint)
    spindle("Extended forearm barrel",(568,699,-4),(441,677,-7),40,54,shell)
    spindle("Dark pressure coupling",(451,679,-5),(433,679,-5),54,56,joint)
    spindle("Coral nozzle coupling",(436,680,-8),(404,678,-10),54,58,ORANGE_M)
    coords=[(856,860,10),(797,881,7),(700,882,3),(597,873,-7),(518,846,-10),(480,810,-8),(460,753,-5),(460,738,-6)]
    line("Flexible soap hose",coords,17,shell);spindle("Hose swivel",(461,755,-4),(459,735,-4),27,28,joint)
    head([(544,452),(573,390),(637,343),(719,326),(807,331),(894,361),(947,402),(975,470),(978,533),(947,605),(904,643),(849,654),(742,645),(637,616),(567,582),(535,531)],[(579,454),(603,413),(643,390),(692,382),(780,395),(863,417),(905,444),(920,484),(913,542),(889,583),(850,600),(792,596),(687,575),(606,552),(579,529),(570,495)],[(542,460,30,55),(976,542,32,56)],[(662,458,27,35,.23),(815,490,28,35,.23)],[(607,506,18,17,.3),(849,558,21,17,.35)],[(692,516),(705,529),(729,536),(748,536),(758,531)],None,shell,joint,GLASS,LED,85)
    frame=[(311+107*cos(i*tau/28),658+78*sin(i*tau/28),-15) for i in range(28)]
    line("Open coral bubble wand",frame,19.5,ORANGE_M,cyclic=True)
    # Horizontal rings keep the soap envelope continuous all the way into the wand.
    rings=[]
    for j in range(65):
        theta=.012+(2.73-.012)*j/64
        rings.append((300,306-207*cos(theta),211*sin(theta),150*sin(theta)))
    rings.extend([(302,507,64,44),(305,518,44,29),(308,531,29,19),(310,544,24,16),
                  (311,554,26,17),(311,563,32,22),(311,573,45,29),(311,580,45,27)])
    vertices=[]; faces=[]; steps=96
    for cx,cy,rx,rz in rings:
        vertices.extend(at(cx+rx*cos(k*tau/steps),cy,-17+rz*sin(k*tau/steps)) for k in range(steps))
    for j in range(len(rings)-1):
        faces.extend((j*steps+k,j*steps+(k+1)%steps,(j+1)*steps+(k+1)%steps,(j+1)*steps+k) for k in range(steps))
    faces+=[tuple(reversed(range(steps))),tuple(range(len(vertices)-steps,len(vertices)))]
    film=mesh_object("Giant iridescent bubble growing from wand",vertices,faces,bubble_material("Rainbow thin soap film",True))
    bpy.context.view_layer.update()
    film.visible_shadow=False
    for x,y,rx,ry,a in [(215,181,49,15,-.63),(159,256,14,7,-.99),(411,439,45,8,-.58)]:
        patch("Reflected strip light",ellipse_points(x,y,rx,ry,angle=a),film,pigment("Soap white glint","FFF6FF",.21),0.5,.54)
    for xx,yy in ((290,553),(332,553)):
        pts=[(xx,yy,surface(film,xx,yy)-2),(xx-2,yy+10,surface(film,xx-2,yy+10)-2)]
        seam("Soap gathers at the mouth of the wand",pts,2.8,pigment("Wet lilac film edge","F0B4DA",.24))
    # Curved coloured ribbons follow the visible rim and the narrowing soap neck.
    for a,b,c in [(8,62,"F6C68F"),(66,129,"D398DF"),(131,193,"79D9EC"),(196,246,"DF8FDC"),(247,299,"8AAFE5"),(300,350,"F6B0D5")]:
        pts=[]
        for j in range(7):
            t=(a+(b-a)*j/6)*pi/180;x=300+202*cos(t);y=306+197*sin(t)
            pts.append((x,y,surface(film,x,y)-.9))
        seam("Thin-film interference",pts,4.8,pigment("Iridescence "+c,c,.23,0,.32))

def rover(shell,joint):
    tiremat=pigment("Graphite lunar tires","3E3F50",.47);treadmat=pigment("Moon tread shoulders","454656",.48)
    grey=pigment("Brushed wheel hub","CBC5BC",.27,.49,.22)
    def wheel(x,y,d,r):
        # Axle comes toward the viewer and left. The tire shows its sidewall and tread surface.
        a=Vector((x+35,y-7,d+45));b=Vector((x-27,y+4,d-45))
        tire=mechanical("Rounded lunar tire",a,b,[(0,r*.83),(.06,r*.94),(.20,r),(.77,r),(.95,r*.95),(1,r*.84)],tiremat)
        end=at(*b);vec=(at(*b)-at(*a)).normalized()
        middle=(at(*a)+end)*.5;u=vec.cross(Vector((0,0,1))).normalized();v=vec.cross(u).normalized()
        for i in range(20):
            t=tau*i/20
            normal=u*cos(t)+v*sin(t)
            loc=middle+normal*r*.985/200
            obj=box("Beveled tire tread",loc,(r*.42/200,(end-at(*a)).length*.76,r*.126/200),treadmat,.028)
            localx=vec.cross(normal).normalized()
            from mathutils import Matrix
            obj.rotation_euler=Matrix((localx,vec,normal)).transposed().to_euler()
        tip=b+Vector((-8,1,-11))
        mechanical("Recessed wheel socket",b,tip,[(0,r*.54),(.5,r*.55),(1,r*.51)],tiremat)
        mechanical("Silver wheel hub",tip,tip+Vector((-5,1,-7)),[(0,r*.47),(.4,r*.47),(1,r*.40)],grey)
    for x,y in [(263,880),(491,893),(805,884)]:
        wheel(x,y,118,107)
    solid("Rover dark lower frame",[(205,762),(310,750),(746,755),(825,780),(827,865),(740,888),(334,871),(210,844)],40,59,tiremat,.39)
    body=solid("Sand ivory rover chassis",[(169,734),(190,690),(252,665),(397,652),(599,641),(748,644),(826,664),(851,703),(862,755),(852,792),(816,815),(704,825),(423,810),(247,799),(184,779)],12,108,IVORY,.45)
    for x,y,r in [(211,914,116),(445,930,120),(680,944,121)]:
        sx=x+57;sy=y-159
        oval("Chassis suspension pivot",(sx,sy,-82),(35,34,23),joint)
        spindle("Angled rocker link",(sx,sy,-99),(x,y-39,-157),21,27,shell)
        wheel(x,y,-141,r)
    for x,y in [(441,707),(761,725)]:
        solid("Amber headlight surround",ellipse_points(x,y,38,25),-99,8,pigment("Amber reflector","F4AF35",.23),.35)
        solid("Cream running lamp",ellipse_points(x,y,30,17),-109,8,pigment("Lit pale headlight","FFFFBB",.22),.28)
    panel=solid("Control bezel",[(538,668),(553,649),(647,646),(674,652),(696,673),(701,718),(688,742),(664,750),(552,746),(531,726)],-99,15,IVORY,.40)
    for x,c in [(575,ORANGE_M),(651,shell)]:
        oval("Rover control switch",(x,701,-116),(26,29,15),c)
    spindle("Solar stand",(263,664,29),(248,607,12),33,30,joint)
    solar=solid("Rounded solar-panel frame",[(134,523),(148,507),(280,501),(298,508),(342,591),(346,611),(334,636),(202,646),(181,634)],21,19,pigment("Ice blue solar frame","93BFDF",.25,.12),.31)
    for i in range(3):
        for j in range(3):
            x=160+i*42+j*14;y=512+j*33-i*.5
            patch("Blue photovoltaic wafer",[(x+3,y),(x+36,y),(x+49,y+28),(x+14,y+28)],solar,pigment("Cobalt photovoltaics","345FAE",.19,.19,.32),1.0,1.3)
            for offset in (11,22):
                wire=[(x+offset+1,y+3),(x+offset+10,y+24)]
                seam("Silver collecting line",[(u,v,surface(solar,u,v)-3.3) for u,v in wire],.86,pigment("Pale blue busbar","93B9DB",.35,.25))
    spindle("Sample arm boom",(822,710,-36),(937,566,-51),27,30,IVORY)
    for x,y,r in ((816,707,36),(841,673,40),(940,567,39)):
        oval("Articulated sampling joint",(x,y,-57),(r,r,22),joint)
        oval("Aqua bearing cover",(x-2,y,-76),(r*.74,r*.76,11),shell)
        oval("Axle peg",(x-3,y,-85),(r*.44,r*.45,7),pigment("Aqua pivot cap","85D1CC",.31))
    # Orange sample cup with an open, rolled elliptical rim.
    bowl_outline=[(893,467),(911,445),(958,424),(1041,408),(1115,413),(1148,428),(1159,449),(1145,490),(1120,532),(1087,558),(1033,570),(971,570),(929,555),(904,519)]
    solid("Orange sample scoop",bowl_outline,-21,48,ORANGE_M,.55)
    basin=solid("Scoop interior",[(909,469),(926,449),(966,434),(1024,422),(1095,420),(1133,429),(1142,442),(1127,465),(1093,483),(1043,498),(980,504),(937,500),(914,490)],-67,7,pigment("Scoop shadow orange","C96624",.36),.53)
    rock=solid("Pitted lunar rock",[(955,456),(974,421),(996,390),(1017,378),(1047,375),(1063,385),(1080,399),(1107,418),(1115,437),(1116,467),(1092,482),(1055,490),(1005,494),(966,488)],-65,37,pigment("Moon basalt","8D8794",.68),.77)
    rockmat=rock.data.materials[0]
    for x,y,r in ((1014,410,18),(1070,451,16),(986,453,8)):
        d=surface(rock,x,y)
        cutter=oval("Crater cutter",(x,y,d-4),(r,r*.94,r*.8),rockmat)
        mod=rock.modifiers.new("Impact depression","BOOLEAN");mod.operation="DIFFERENCE";mod.object=cutter
        bpy.context.view_layer.objects.active=rock;bpy.ops.object.modifier_apply(modifier=mod.name)
        bpy.data.objects.remove(cutter,do_unlink=True)
    ledge=[(x,y,-116) for x,y in [(896,467),(914,489),(949,499),(997,503),(1050,498),(1105,480),(1141,456),(1151,439)]]
    seam("Scoop rolled lip",ledge,6.8,pigment("Lighter scoop lip","FFA54D",.25))
    head([(348,427),(375,367),(427,328),(509,304),(600,294),(667,295),(710,318),(745,368),(765,423),(761,494),(744,546),(702,567),(614,585),(509,596),(431,595),(386,578),(359,535),(349,485)],[(429,438),(445,400),(482,378),(554,359),(629,346),(678,344),(712,356),(736,393),(745,450),(739,487),(718,508),(661,524),(557,541),(485,549),(449,538),(434,514)],[(357,483,29,56),(757,420,24,54)],[(533,447,29,31,-.20),(683,417,26,32,-.19)],[(493,507,23,15,-.18),(720,468,16,17,-.18)],[(584,482),(600,490),(619,493),(639,487),(653,471)],(.49,.065),shell,joint,GLASS,LED,75,-5)
    spindle("Neck seal",(560,579,0),(560,629,0),65,76,joint)
    shellglass=solid("Astronaut bubble helmet",[(305,466),(319,382),(357,315),(411,269),(487,235),(561,225),(633,233),(694,261),(751,311),(788,381),(804,451),(797,526),(773,584),(741,620),(637,632),(469,631),(389,618),(343,577),(316,526)],-5,155,bubble_material("Clear space helmet",False),.94,center=(555,440));shellglass.visible_shadow=False
    patch("Broad curved dome reflection",[(363,337),(390,301),(429,272),(460,260),(477,257),(461,269),(424,294),(390,324),(368,350)],shellglass,pigment("Helmet soft white reflection","F8F9FF",.25),.4,.48)
    solid("Domed helmet base collar",[(371,622),(389,615),(720,613),(744,618),(748,640),(734,652),(389,652),(371,647)],-57,17,shell,.34)


def driver(shell,joint):
    legs([((511,862),(507,968),[(449,958),(533,961),(565,991),(586,1040),(592,1092),(577,1120),(531,1128),(419,1127),(382,1116),(378,1081),(391,1026),(416,982)]),((701,854),(738,955),[(678,970),(736,942),(777,947),(816,968),(849,1012),(873,1061),(873,1088),(844,1109),(778,1128),(711,1137),(668,1130),(652,1107),(650,1062)])],shell,joint)
    arm((437,579),(336,698),(314,774),60,1,shell,joint);arm((786,587),(918,694),(933,778),58,-1,shell,joint)
    outer=[(401,620),(433,554),(490,517),(572,500),(691,496),(759,511),(799,544),(823,612),(838,710),(835,793),(806,850),(764,878),(694,891),(586,899),(485,885),(432,854),(406,810),(395,743)]
    creamouter=[(431,649),(455,582),(488,548),(549,531),(654,527),(719,534),(752,553),(788,602),(813,694),(821,755),(810,793),(776,820),(722,837),(623,844),(527,831),(455,803),(433,780),(424,736)]
    opening=[(456,639),(477,593),(511,565),(559,548),(642,546),(700,550),(735,569),(766,610),(792,679),(799,726),(788,750),(747,770),(690,779),(625,782),(550,777),(492,759),(460,735),(452,698)]
    housing_ring("Aqua cockpit shell",outer,creamouter,-64,49,shell,pigment("Cockpit interior shadow","9B713F",.45))
    housing_ring("Recessed vanilla cockpit",creamouter,opening,-83,2,IVORY,pigment("Bronze cockpit lining","B28E58",.48))
    seat=pigment("Tangerine seat upholstery","F68A37",.35,0,.18)
    solid("Contoured bucket seat",[(476,719),(481,671),(497,650),(534,643),(604,651),(655,649),(716,639),(756,655),(770,693),(774,754),(739,771),(559,777),(491,758)],-15,20,seat,.50)
    for x in (507,738):
        oval("Seat bolster",(x,714,-28),(33,68,23),seat)
    blue=pigment("Pilot cobalt enamel","2C83CA",.28,.04,.37)
    oval("Pilot suit",(641,719,-40),(60,66,26),blue)
    head([(542,611),(553,580),(577,564),(623,557),(673,558),(705,569),(722,591),(729,621),(728,658),(715,680),(682,691),(640,697),(590,692),(561,682),(543,660)],[(568,620),(578,598),(598,590),(644,588),(685,591),(704,608),(712,635),(704,660),(684,672),(640,678),(596,671),(574,657)],[(536,631,13,27),(730,632,13,26)],[(606,626,14,15,0),(680,629,14,14,0)],[],[(627,650),(639,657),(652,658),(663,652)],None,blue,pigment("Pilot deep navy trim","214C75",.31),GLASS,LED,29,-38,small=True)
    axis("Pilot aerial",(637,561,-77),(638,527,-92),4.3,blue)
    oval("Scarlet pilot beacon",(638,523,-96),(16,16,14),pigment("Pilot beacon red","F64148",.25,0,.30))
    wheelmat=pigment("Petrol steering wheel","244F59",.31,0,.16)
    # The lower rim is hidden behind the cockpit edge.
    line("Driver steering wheel",[(642+93*cos(i*tau/32),782+79*sin(i*tau/32),-64) for i in range(32)],12,wheelmat,cyclic=True)
    for endpoint in ((570,734,-61),(713,738,-61),(642,708,-61)):
        axis("Steering wheel spoke",(643,778,-61),endpoint,8,wheelmat)
    oval("Steering wheel centre",(643,778,-73),(28,26,11),wheelmat)
    for x,w in ((561,542),(722,741)):
        spindle("Pilot arm",(w,748,-47),(x,730,-64),15,19,blue)
        oval("Pilot glove gripping wheel",(x,731,-72),(25,24,16),blue)
        for i in (-1,0,1):
            digit=oval("Curled pilot glove digit",(x+i*9,739-i*1.4,-82),(6.3,11,6.8),blue)
            digit.rotation_euler.y=.30 if x<640 else -.30
    head([(288,280),(306,219),(343,166),(421,119),(524,85),(653,70),(747,73),(808,91),(860,135),(890,190),(900,266),(892,349),(864,413),(814,452),(717,486),(607,510),(494,520),(424,513),(364,490),(321,453),(297,396),(286,338)],[(374,320),(393,275),(440,244),(536,215),(652,194),(746,185),(792,192),(828,214),(849,261),(857,328),(847,387),(815,423),(738,447),(639,466),(541,475),(467,476),(418,462),(388,434),(374,391)],[(284,355,36,77),(905,260,39,78)],[(519,364,39,42,-.44),(734,329,36,45,.22)],[(455,429,28,19,-.05),(798,381,27,20,-.46)],[(587,411),(609,420),(637,422),(662,416),(681,395)],[a for a in ((.47,.54),(-.34,.54))],shell,joint,GLASS,LED,112,0,True)
    toe=bpy.data.objects["Rounded moulded boot 1"]
    patch("Raised foot rubber sole",[(663,1111),(696,1087),(761,1066),(826,1059),(851,1065),(862,1078),(848,1095),(798,1117),(742,1131),(691,1132),(667,1126)],toe,joint,1.1,2.3)


def render_robot(item):
    global GLASS,LED,IVORY,ORANGE_M
    settings={"cuckoo":(627,597,1024),"rover":(632,645,1138),"bubble":(579,583,1066),"driver":(623,605,1148)}
    scene=studio("robot",settings[item])
    scene.render.resolution_x=scene.render.resolution_y=816
    scene.cycles.samples=40
    scene.render.threads=10
    scene.view_settings.exposure=-.40
    scene.world.node_tree.nodes.get("Background").inputs["Strength"].default_value=.30
    scene.objects["Cool front fill"].data.energy=240
    scene.objects["Overhead satin highlight"].data.energy=375
    shell=pigment("Robot sea-glass enamel","55BFBE",.31,.025,.22)
    joint=pigment("Deep teal articulation","246775",.31,.18,.23)
    GLASS=pigment("Midnight blue glass","163449",.235,.05,.35)
    LED=pigment("Emissive mint phosphor","60F3BC",.29)
    p=next(n for n in LED.node_tree.nodes if n.type=="BSDF_PRINCIPLED")
    p.inputs["Emission Color"].default_value=(.11,.86,.43,1);p.inputs["Emission Strength"].default_value=.16
    IVORY=pigment("Warm cream cockpit enamel","FFE8BC",.30,.025,.22)
    ORANGE_M=pigment("Persimmon lacquer","F78343",.27,.04,.31)
    {"cuckoo":clock,"rover":rover,"bubble":bubble,"driver":driver}[item](shell,joint)
    for o in bpy.context.scene.objects:
        if o.type in ("MESH","CURVE") and o.data.materials and all(m in (shell,joint) for m in o.data.materials):
            o["coat_palette"]=True
    save_render("robot-extra-bubble-pack" if item=="bubble" else "robot-model-"+item)


if __name__=="__main__":
    for i in sys.argv[sys.argv.index("--")+1:]:
        render_robot(i)
