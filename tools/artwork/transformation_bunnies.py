"""Four separate lop-eared bunny poses using measured features of their outfit sheets."""
import math,sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parent))
from pet_sculpt import *
from bunny_identity import lop_ears, magician_head
from transformation_dogs import leg
from transformation_studio import save_render


def bunny_face(kind,fur):
    if kind=="car":
        outline=[(622,206),(753,209),(819,248),(853,314),(875,383),(888,454),(879,496),
                 (841,531),(772,556),(697,566),(604,564),(534,543),(492,507),(478,470),(481,385),(520,286)]
        eyes=[(625,411,43,44,8,0),(804,393,36,43,-4,1)]
        brow=[[(584,342),(602,325),(625,321)],[(777,301),(796,300),(815,310)]]
        muzzle=(731,480,77,60);nose=(732,447,22,14);smile=(731,481,43,39)
        cheeks=[(583,475,34,23),(849,447,25,20)];tuft=(661,203,74,63)
    elif kind=="moon":
        outline=[(543,245),(638,246),(708,268),(763,319),(801,390),(820,463),(816,506),
                 (783,552),(704,595),(623,620),(532,631),(444,622),(377,584),(336,539),
                 (316,474),(320,393),(362,320),(439,269)]
        eyes=[(496,454,52,54,10,3),(700,424,45,52,-5,3)]
        brow=[[(435,375),(458,351),(486,342)],[(657,314),(684,313),(708,329)]]
        muzzle=(620,534,87,63);nose=(623,496,24,15);smile=(621,535,52,34)
        cheeks=[(455,542,43,28),(763,491,31,28)];tuft=(546,251,74,56)
    else:
        outline=[(555,168),(666,169),(744,190),(812,236),(847,301),(862,378),(858,449),
                 (836,491),(785,533),(703,558),(620,567),(523,554),(456,535),(402,499),
                 (386,445),(385,360),(417,268),(473,210)]
        eyes=[(520,380,47,51,8,0),(743,379,44,52,-8,0)]
        brow=[[(481,295),(503,276),(534,272)],[(722,272),(749,276),(772,293)]]
        muzzle=(636,471,87,67);nose=(637,435,26,16);smile=(635,472,52,37)
        cheeks=[(470,453,40,28),(786,449,34,27)];tuft=None
    lop_ears(kind,fur)
    head,m=face("bunny",fur,outline,[],eyes,brow,muzzle,nose,smile,cheeks,sleep=kind=="moon")
    if tuft:
        x,y,w,h=tuft
        for i in range(2):
            sc=1 if i==0 else .70
            shape=[(x-37+i*43,y+10),(x-30+i*43,y-h*.27),(x-6+i*43,y-h*.71*sc),
                   (x+21+i*43,y-h*.95*sc),(x+45+i*43,y-h*.99*sc),(x+42+i*43,y-h*.69*sc),
                   (x+16+i*43,y+1)]
            tagged(cushion("Velvety tuft",shape,19,20,20,fur))
    return head


def palm(name,x,y,w,h,d,fur,angle=0,carved=False):
    def coords(pts):
        a=math.radians(angle)
        return [(x+px*w*math.cos(a)-py*h*math.sin(a),y+px*w*math.sin(a)+py*h*math.cos(a)) for px,py in pts]
    o=tagged(cushion(name,coords([(-1,-.04),(-.84,-.58),(-.30,-.94),(.27,-.98),(.77,-.62),
       (1,-.03),(.98,.44),(.73,.78),(.37,.83),(.05,.99),(-.29,.98),(-.53,.85),(-.83,.75),(-.98,.46)]),d,w*.66,w*.60,fur))
    if carved:
        return o
    for xx in (-.32,.37):
        surface_line("Bunny toe furrow",o,coords([(xx,-.18),(xx+.055,.29),(xx+.07,.80)]),2.3,"CC9D76",[.16,.75,.22],1)
    return o


def sparkle(x,y,d,r):
    return cushion("Little golden magic sparkle",[(x,y-r),(x+r*.27,y-r*.35),(x+r,y),
                   (x+r*.30,y+r*.24),(x,y+r),(x-r*.25,y+r*.29),(x-r,y),(x-r*.27,y-r*.23)],
                   d,7,7,"FFD163")


def star_patch(name,x,y,d,r,target=None):
    pts=[]
    for i in range(10):
        a=-math.pi/2+TAU*i/10;rr=r*(1 if i%2==0 else .54)
        pts.append((x+rr*math.cos(a),y+rr*math.sin(a)))
    # Ease just the tips and valleys, retaining the five-point silhouette.
    shape=[]
    for i,p in enumerate(pts):
        for q in (pts[i-1],pts[(i+1)%10]):
            shape.append(tuple(p[n]*.89+q[n]*.11 for n in (0,1)))
    return (patch(name,target,shape,"F6C04E",9,2) if target else
            cushion(name,shape,d,13,9,"F5C04D"))


def hat():
    fur=start("bunny",625,622,1160,keep_body=True)
    magician_head()
    navy="242066"
    # The front edge is lower in the layout than the back, making a hollow hat.
    crown=turned("Magician's hollow felt crown",[(621,1082,100,6,6,5),(621,1080,100,291,212,83),
        (621,1055,100,308,229,89),(621,793,103,290,217,69),(621,686,100,298,222,68),
        (621,673,100,264,193,49),(621,1024,100,257,190,61),(621,1035,100,4,6,0)],navy)
    ribbon=turned("Crimson satin hatband",[(621,791,93,289,219,66),(621,786,92,295,224,69),
        (621,678,92,300,223,62),(621,677,94,289,215,58)],"C91C52")
    grain(ribbon.data.materials[0],96,.011,.40)
    brim=turned("Rolled felt brim with open center",[(615,586,68,258,148,37),(615,597,68,274,152,50),
        (615,637,68,405,228,86),(615,632,67,443,251,100),(615,614,67,450,256,107),
        (615,584,67,435,248,109),(615,572,68,382,221,85),(615,554,68,297,166,47),
        (615,555,68,263,145,40)],"302A74")
    grain(brim.data.materials[0],130,.015,.66)
    rounding=brim.modifiers.new("Soft rolled fabric edges","SUBSURF")
    rounding.levels=rounding.render_levels=2
    for obj,strength in [(brim,.22),(crown,.13),(ribbon,.14)]:
        shader=next(n for n in obj.active_material.node_tree.nodes if n.type=="BSDF_PRINCIPLED")
        shader.inputs["Sheen Weight"].default_value=strength
        shader.inputs["Specular IOR Level"].default_value=.27
        if obj==ribbon:
            shader.inputs["Metallic"].default_value=.18
    for y in (682,782):
        coords=[(621+294*math.cos(a),y-65*math.sin(a),91+224*math.sin(a))
                for a in [TAU*i/100 for i in range(100)]]
        line("Woven edge of magician's satin ribbon",coords,2.4,"AF3658",cyclic=True)
    star_patch("Gold star on the satin band",627,788,-145,55)
    cushion("Deep indigo lining inside the open hat",oval(615,577,277,75),108,13,17,"1F1B42")
    tagged(cushion("Shoulders emerging inside the hat",[(504,497),(555,466),(685,459),
        (733,477),(764,512),(792,579),(781,617),(473,617),(475,569)],6,91,71,fur))
    lop_ears("hat",fur)
    for x,angle in ((557,5),(762,-5)):
        inward=1 if x<625 else -1
        tagged(swept("Forearm reaching out of the hat",[
            (x+inward*28,501,-17,12,11),(x+inward*18,519,-40,32,29),
            (x+inward*7,546,-67,38,31),(x,567,-92,37,28),
            (x,581,-110,29,19),(x,590,-119,9,9)],fur))
        paw=palm("Curled paw resting on the rolled brim",x,583,64,44,-124,fur,angle,carved=True)
        # Press the grooves into the fur as the three toes curl forward around the lip.
        for vertex in paw.data.vertices:
            px=626+vertex.co.x*UNIT; py=1110-vertex.co.z*UNIT
            inset=sum(math.exp(-((px-x-gap-.09*(py-581))/4.3)**2) for gap in (-20,21))
            if vertex.co.y*UNIT < -124 and py>569:
                vertex.co.y+=inset*min(9,(py-569)*.25)/UNIT
        paw.data.update()
        for gap in (-20,21):
            crease=[(x+gap-1.5,574),(x+gap,585),(x+gap+2,598),(x+gap+3.5,610)]
            surface_line("Tapered crease between resting toes",paw,crease,2.1,"D9AE88",[.12,.82,.88,.14],1)
    sparkle(224,333,25,34);sparkle(1076,506,28,32)


def berry_leaf(name,startp,endp,width,depth,green):
    x,y=startp;ex,ey=endp;dx,dy=ex-x,ey-y
    L=math.hypot(dx,dy);nx,ny=-dy/L,dx/L
    coords=[]
    for t,w in [(0,0),(.26,.70),(.53,1),(.77,.71),(1,0),(.77,-.69),(.51,-.98),(.23,-.62)]:
        coords.append((x+dx*t+nx*w*width,y+dy*t+ny*w*width))
    ob=cushion(name,coords,depth,14,9,green)
    surface_line("Soft crease on the leaf",ob,[(x+dx*.20,y+dy*.20),(x+dx*.50,y+dy*.50),
          (x+dx*.89,y+dy*.89)],1.8,"839544",[.15,.55,.08],1)
    return ob


def foot(name,x,y,rx,ry,angle,d):
    sole=cushion(name,oval(x,y,rx,ry,angle),d,58,45,"F3D5AC",steps=2)
    sole["coat_palette"]=True
    a=math.radians(angle)
    def p(u,v):return (x+u*math.cos(a)-v*math.sin(a),y+u*math.sin(a)+v*math.cos(a))
    for u,v,w,h in [(0,.33*ry,.44*rx,.34*ry),(-.50*rx,-.18*ry,.17*rx,.16*ry),
                     (0,-.50*ry,.18*rx,.17*ry),(.45*rx,-.23*ry,.17*rx,.16*ry)]:
        px,py=p(u,v);patch("Velvet bunny paw pad",sole,oval(px,py,w,h,angle),"EB9388",2,1)
    return sole


def strawberry():
    fur=start("bunny",627,589,1140)
    fruit=cushion("Plump strawberry suit with a pointed base",[(602,529),(754,532),(838,572),
        (916,643),(967,740),(972,828),(948,906),(885,988),(762,1069),(672,1094),(616,1103),
        (538,1085),(413,1036),(320,956),(271,867),(257,782),(278,697),(329,617),(431,554)],
        42,167,150,"CD3D4D",center=(618,810))
    grain(fruit.data.materials[0],92,.022,.70)
    seeds=[(498,698,13),(758,698,-14),(632,812,0),(495,895,-2),(757,897,8)]
    for v in fruit.data.vertices:
        x=626+v.co.x*UNIT;y=1110-v.co.z*UNIT
        dip=sum(10*math.exp(-((x-a)/30)**2-((y-b)/41)**2) for a,b,_ in seeds)
        v.co.y+=dip*max(0,-v.normal.y)/UNIT
    fruit.data.update()
    shader=next(n for n in fruit.active_material.node_tree.nodes if n.type=="BSDF_PRINCIPLED")
    shader.inputs["Sheen Weight"].default_value=.34
    for x,y,dx in [(356,747,1),(872,749,-1)]:
        ring=cushion("Strawberry sleeve opening",oval(x,y,76,69,-22*dx),-91,24,18,"BE3946")
        palm("Bunny mitten through fruit sleeve",x+dx*15,y+11,74,60,-114,fur,31*dx)
    for x,y,angle in seeds:
        pts=[(x,y-46),(x+17,y-13),(x+26,y+14),(x+16,y+33),(x-5,y+40),(x-24,y+25),(x-22,y-2)]
        angle=math.radians(angle)
        pts=[(x+(px-x)*math.cos(angle)-(py-y)*math.sin(angle),y+(px-x)*math.sin(angle)+(py-y)*math.cos(angle)) for px,py in pts]
        patch("Embroidered gold strawberry pip",fruit,pts,"F8C466",7.5,1)
    for a,b,w,d in [((496,505),(350,600),42,-61),((541,511),(385,649),61,-91),((613,522),(626,689),78,-109),
                   ((686,512),(858,657),63,-91),((754,510),(888,600),42,-61)]:
        berry_leaf("Broad green strawberry collar leaf",a,b,w,d,"769538")
    bunny_face("strawberry",fur)
    for a,b,w,d in [((601,147),(437,250),36,-15),((618,147),(632,250),57,-49),
                    ((660,146),(808,249),38,-20),((623,144),(485,170),26,6)]:
        berry_leaf("Leafy strawberry crown",a,b,w,d,"829C3A")
    swept("Short curved strawberry stalk",[(619,166,15,15,12),(626,137,12,23,20),(641,107,15,25,23),
          (659,86,18,24,23),(685,79,18,24,21),(710,81,16,2,2)],"82A139")
    foot("Left grounded bunny foot",401,1016,109,128,-41,-98)
    foot("Right grounded bunny foot",828,1015,109,129,41,-98)


def moon():
    fur=start("bunny",614,634,1130)
    moon_shape=swept("Rounded golden crescent cradle",[(554,128,58,2,2),(542,131,60,18,18),
        (508,140,67,34,32),(447,160,72,46,43),(340,222,77,61,55),
        (241,326,77,72,65),(172,456,78,68,69),(136,592,66,53,64),
        (148,723,46,58,67),(215,872,10,75,79),(327,1001,-36,81,81),(461,1075,-88,72,80),
        (621,1108,-115,57,75),(757,1093,-119,66,73),(908,1009,-124,77,70),
        (1019,902,-119,81,72),(1073,801,-110,59,53),(1094,728,-96,31,33),
        (1098,688,-91,14,15),(1098,674,-90,2,2)],paint("F5B638",.29,.16),56)
    for v in moon_shape.data.vertices:
        x=626+v.co.x*UNIT;y=1110-v.co.z*UNIT
        if x<454 and 248<y<1004:
            f=max(0,min(1,(x-95)/150))
            v.co.x+=173*f*f*math.sin(math.pi*(y-248)/756)**2/UNIT
    tagged(cushion("Bunny curled up in the moon",[(405,592),(624,558),(731,585),(812,667),
        (823,759),(750,878),(583,930),(403,893),(330,823),(334,763),(365,702)],39,96,92,fur))
    foot("Bunny foot peeking from under the blanket",628,967,99,92,43,-94)
    cushion("Shadowed blanket lining behind the foot",oval(653,918,273,104),20,46,47,"343868")
    blanket=sliced("Draped blanket with an opening around the foot",[(349,854),(371,804),(437,764),
        (529,724),(634,706),(718,703),(781,672),(828,665),(884,683),(933,734),(973,806),
        (980,842),(951,900),(884,967),(804,1007),(752,1014),(750,973),(741,920),(709,889),
        (658,871),(610,878),(571,905),(541,949),(511,994),(484,1012),(450,1007),(405,977),(373,927)],
        -66,104,65,"383E77",axis=0)
    # Model diagonal folds in the drape, strongest across the hanging near hem.
    for v in blanket.data.vertices:
        x=v.co.x*UNIT+626;y=1110-v.co.z*UNIT
        f=max(0,min(1,(-v.co.y*UNIT-66)/99))
        drape=24*math.cos((x-.46*y)/43)+10*math.sin((x+.20*y)/28)
        fall=max(0,min(1,(y-730)/220))
        v.co.y-=drape*f/UNIT
        v.co.z+=fall*fall*math.sin((x-.37*y)/38)*9/UNIT
    blanket.data.update()
    grain(blanket.data.materials[0],145,.015,.76)
    for x,y,r in [(465,854,51),(685,798,57),(891,780,49)]:
        star=star_patch("Embroidered golden blanket star",x,y,0,r,blanket)
        for i in range(10):
            a=-math.pi/2+TAU*i/10;b=a+TAU/10
            r0=r*(.84 if i%2==0 else .46);r1=r*(.46 if i%2==0 else .84)
            x0,y0=x+math.cos(a)*r0,y+math.sin(a)*r0
            x1,y1=x+math.cos(b)*r1,y+math.sin(b)*r1
            for u in (.10,.56):
                coords=[(x0+(x1-x0)*t,y0+(y1-y0)*t) for t in (u,u+.25)]
                surface_line("Blanket star stitch",star,coords,1.35,"FFD78C",[.75,.75],1)
    bunny_face("moon",fur)
    tagged(cushion("Bunny forearm resting on the blanket",[(415,628),(468,622),(489,647),(509,673),
        (555,669),(583,695),(568,732),(509,748),(463,761),(410,783),(384,773),(373,743),
        (371,697),(385,655)],-91,49,44,fur))
    palm("Sleepy paw holding blanket",546,714,58,43,-122,fur,1)


def carrot_shell(outline):
    border=smooth(outline,8)
    xmin=min(v.x for v in border);xmax=max(v.x for v in border)
    vs=[];fs=[];stations=124;segments=76
    near_edge=[];back_edge=[]
    for i in range(stations+1):
        t=(i+.001)/(stations+.002)
        x=xmin+(xmax-xmin)*t
        hits=[a.y+(b.y-a.y)*(x-a.x)/(b.x-a.x) for a,b in zip(border,border[1:]+border[:1])
              if min(a.x,b.x)<=x<max(a.x,b.x)]
        top,bottom=min(hits),max(hits);cy=(top+bottom)/2;h=(bottom-top)/2
        thick=min(158,h*1.07)
        u=(x-653)/225
        opening=math.sqrt(max(0,1-u*u))
        cut=min(1.55,math.acos(max(-1,min(1,1-140*opening/max(h,1)))))
        rear=cut*.57
        def section(a):
            return (x,cy-h*math.cos(a),31-thick*math.sin(a))
        for j in range(segments+1):
            angle=cut+(TAU-cut-rear)*j/segments
            vs.append(point(*section(angle)))
        if opening:
            near_edge.append(section(cut));back_edge.append(section(TAU-rear))
    for i in range(stations):
        for j in range(segments):
            a=i*(segments+1)+j
            fs.append((a,a+1,a+segments+2,a+segments+1))
        # Both edge vertices coincide around the closed portions of the carrot.
        a=i*(segments+1)
        x=vs[a].x*UNIT+626
        if abs(x-653)>225:fs.append((a+segments,a,a+segments+1,a+2*segments+1))
    fs.extend((tuple(reversed(range(segments+1))),tuple(range(stations*(segments+1),(stations+1)*(segments+1)))))
    shell=mesh("Single carved carrot body with open cockpit",vs,fs,"F08A32",True)
    if shell.data.polygons[segments//4].normal.y>0:shell.data.flip_normals()
    opening=near_edge+list(reversed(back_edge))
    wall=[point(x,y,d) for x,y,d in opening]
    floor=[point(653+(x-653)*.89,y+63,d*.79+6) for x,y,d in opening]
    n=len(wall)
    inner=mesh("Recessed solid cockpit walls",wall+floor,[(j,(j+1)%n,(j+1)%n+n,j+n) for j in range(n)]+[tuple(range(n,2*n))],"D67B32",True)
    line("Rounded top of the carrot cockpit",opening,5,"F5A14C",cyclic=True)
    return shell


def carrot():
    fur=start("bunny",626,606,1100)
    # The cockpit, padded seat and driver are built first, inside the open carrot.
    for xx,yy,w,h in [(520,936,86,62),(916,949,75,71)]:
        cushion("Far wooden wheel",oval(xx,yy,w,h),113,34,29,"B88752",steps=2)
    for pts in [[(333,666),(292,602),(237,550),(200,534),(178,552),(179,588),(222,639),(288,687)],
                [(315,711),(264,659),(213,628),(156,634),(128,653),(124,681),(145,704),(204,715),(270,723)],
                [(307,744),(274,720),(215,718),(171,734),(148,769),(150,795),(173,815),(205,812),(249,791),(292,784)]]:
        cushion("Plump sprig of carrot greens",pts,45,28,24,"78AE3C")
    hull=[(280,854),(285,762),(308,697),(358,643),(427,610),(500,600),(595,609),
          (713,636),(829,676),(940,724),(1033,779),(1092,822),(1121,858),(1118,888),
          (1086,915),(1026,934),(915,954),(795,976),(644,982),(498,973),(388,950),(314,919),(280,890)]
    near=carrot_shell(hull)
    seat=cushion("Curved wooden seat inside cockpit",[(450,701),(448,635),(462,592),(504,568),
        (546,568),(574,585),(589,620),(589,724),(549,751),(480,734)],32,42,35,"B47B48")
    tagged(cushion("Bunny seated inside the carrot",[(595,539),(747,537),(792,574),(813,661),
        (803,747),(757,773),(587,766),(526,740),(515,703),(541,630)],-23,76,65,fur))
    for x,y,dx in [(360,677,-18),(535,781,-19),(661,799,-18),(922,742,-33),(1028,789,-29)]:
        surface_line("Tapered carrot groove",near,[(x,y),(x+dx*.59,y+28),(x+dx,y+72)],
                     3.2,"D8853A",[.18,1,.14],2)
    for xx,yy,w,h in [(380,924,106,117),(791,947,111,117)]:
        wheel=cushion("Thick turned maple wheel",oval(xx,yy,w,h,-7),-143,29,41,"DCA76C",steps=2)
        for f in (.77,.85):
            surface_line("Wood wheel growth ring",wheel,oval(xx-2,yy,w*f,h*f,-7,54),1.1,"C89965",lift=1)
        surface_line("Turned maple wheel rim",wheel,oval(xx,yy,w*.933,h*.933,-7,72),2.5,"E7B77D",lift=1.2)
        for j in (-1,0,1):
            pts=[(xx+u*w,yy+(j*.24+.13*math.sin(u*3+j))*h) for u in (-.69,-.40,0,.33,.66)]
            surface_line("Subtle wood grain",wheel,pts,1.5,"C89661",[.12,.7,1,.65,.12],.82)
        ellipsoid("Wooden wheel hub",xx-7,yy+4,-176,31,33,24,"D79552")
    wheelpts=[(751+103*math.cos(a),711+89*math.sin(a),-95+28*math.sin(a))
              for a in [TAU*i/64 for i in range(64)]]
    line("Round wooden steering wheel",wheelpts,16,"A16A3C",cyclic=True)
    for x,y in [(653,676),(835,669),(780,782)]:
        line("Steering wheel spoke",[(749,710,-97),(x,y,-85)],12,"AA7240")
    ellipsoid("Steering column hub",751,712,-104,31,32,24,"A16A3C")
    bunny_face("car",fur)
    for pts,x,y,angle in [([(600,566),(631,565),(646,598),(677,617),(678,663),(652,682),
        (602,685),(563,668),(559,626)],661,650,-20), ([(745,552),(772,560),(798,584),
        (845,594),(860,624),(847,651),(812,652),(771,624)],842,633,22)]:
        tagged(cushion("Bent forearm connected to steering paw",pts,-82,41,43,fur))
        palm("Paw curled around the steering wheel",x,y,43,41,-115,fur,angle)
    for x,y,w,h,angle in [(604,248,79,49,-18),(793,233,73,47,21)]:
        rim=cushion("Aqua aviator goggles frame",oval(x,y,w+9,h+10,angle),-82,26,24,"32B0AF")
        lens=patch("Pale blue aviator lens",rim,oval(x,y,w-9,h-7,angle),"ACD4D3",5,2)
        surface_line("Reflection in the goggles",lens,[(x-37,y+14),(x-21,y-5),(x+2,y-27)],
                     6,"F0F1DF",[.4,1,.35],3.5)
        shader=next(n for n in lens.active_material.node_tree.nodes if n.type=="BSDF_PRINCIPLED")
        shader.inputs["Coat Weight"].default_value=.35
        shader.inputs["Coat Roughness"].default_value=.17
    line("Curved turquoise goggles bridge",[(681,241,-86),(706,231,-88),(724,237,-83)],12,"31AAA6")


BUNNIES={"bunny-head-top-hat":hat,"bunny-model-carrot-car":carrot,"bunny-model-moon":moon,"bunny-model-strawberry":strawberry}
if __name__=="__main__":
    for item in sys.argv[sys.argv.index("--")+1:] or BUNNIES:
        BUNNIES[item]();save_render(item)
