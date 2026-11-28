"""Wardrobe transformations fitted to the original, editable Clocklings dragon."""
import math
from pathlib import Path
import sys

import bpy
import numpy as np
from mathutils import Matrix, Vector

sys.path.insert(0, str(Path(__file__).resolve().parent))
from dragon_finish import open_dragon, frost_horns, tint_tail
from fantasy_sculpt import cylinder, ell, limb, line, mat, mesh, pillow, rgba, save_render, strip, stroke, world


def fit_props(build, x=0, y=0, depth=0, scale=1):
    before = set(bpy.context.scene.objects)
    build()
    bpy.context.view_layer.update()
    pose = Matrix.Translation((x/100,depth/100,-y/100)) @ Matrix.Scale(scale,4)
    for obj in set(bpy.context.scene.objects)-before:
        obj.matrix_world = pose @ obj.matrix_world


def marshmallow():
    open_dragon()
    # The rounded handle rests against the outer finger pad, clear of the forearm.
    stick=cylinder("Wooden roasting stick",(92,304,-131),(230,440,-78.07),3.7,"AF773D",4.5)
    stick.modifiers[0].width=.03
    o=cylinder("Large sugar-puffed marshmallow",(106,318,-125.6),(146,357,-110.3),39,"FFF0CE")
    o.modifiers[0].width=.20;o.modifiers[0].segments=7
    marsh=o.data.materials[0].copy();o.data.materials[0]=marsh;marsh.name="Toasted marshmallow sugar"
    nt=marsh.node_tree;coords=nt.nodes.new("ShaderNodeTexCoord");noise=nt.nodes.new("ShaderNodeTexNoise")
    noise.inputs["Scale"].default_value=33;noise.inputs["Detail"].default_value=3
    sep=nt.nodes.new("ShaderNodeSeparateXYZ");nt.links.new(coords.outputs["Generated"],sep.inputs[0])
    nt.links.new(coords.outputs["Generated"],noise.inputs[0]);math_node=nt.nodes.new("ShaderNodeMath");math_node.operation="MULTIPLY_ADD"
    nt.links.new(noise.outputs["Fac"],math_node.inputs[0]);math_node.inputs[1].default_value=.66;nt.links.new(sep.outputs["Z"],math_node.inputs[2])
    ramp=nt.nodes.new("ShaderNodeValToRGB");ramp.color_ramp.elements[0].position=.64;ramp.color_ramp.elements[0].color=rgba("FFF4DE")
    ramp.color_ramp.elements[1].position=1;ramp.color_ramp.elements[1].color=rgba("E8A648")
    nt.links.new(math_node.outputs[0],ramp.inputs[0]);nt.links.new(ramp.outputs[0],next(n for n in nt.nodes if n.type=="BSDF_PRINCIPLED").inputs["Base Color"])
    caramel=ramp.color_ramp.elements.new(.87);caramel.color=rgba("E4A44C")
    ramp.color_ramp.elements[-1].color=rgba("C98431")
    grain=nt.nodes.new("ShaderNodeBump");grain.inputs["Distance"].default_value=.018
    grain.inputs["Strength"].default_value=.35;nt.links.new(noise.outputs["Fac"],grain.inputs["Height"])
    nt.links.new(grain.outputs["Normal"],next(n for n in nt.nodes if n.type=="BSDF_PRINCIPLED").inputs["Normal"])
    strip("Curled amber flame",[(265,295,265,298),(248,299,248,304),(225,300,225,307),
          (207,296,207,313),(189,301,189,316),(171,309,171,322),(152,318,152,324)],
          -95,.31,"FFAD34")
    strip("Bright heart of the flame",[(262,297,262,298),(247,301,247,303),(225,303,225,305),
          (207,303,207,310),(189,307,189,313),(171,314,171,319),(152,320,152,323)],
          -99,.27,"FFE492")


def ice_wings():
    # The four overlapping softly swollen crystals have independent faces and delicate chipped highlights.
    feathers=[
        ([(230,306),(217,277),(198,244),(177,214),(153,190),(128,175),(107,171),(77,174),
          (50,182),(50,193),(60,210),(76,237),(100,258),(139,280),(193,300)],
         (143,231),19,14,[(55,187),(89,183),(84,214)],[(89,183),(121,178),(111,194)]),
        ([(223,317),(207,287),(181,261),(152,243),(127,233),(105,231),(79,236),(58,249),
          (36,267),(35,275),(52,281),(81,293),(109,299),(157,304),(199,315)],
         (126,267),12,14,[(39,270),(71,251),(64,272)],[(75,249),(103,236),(98,255)]),
        ([(214,320),(196,298),(169,282),(146,279),(125,283),(104,299),(89,318),
          (79,339),(80,345),(104,344),(130,339),(157,337),(185,328)],
         (147,309),5,10,[(85,337),(105,309),(103,327)],[(109,306),(127,289),(124,309)]),
        ([(214,317),(204,301),(190,299),(173,306),(157,322),(146,347),(143,365),
          (147,369),(166,360),(191,344),(209,332)],
         (179,332),0,8,[(147,362),(158,335),(157,352)],[(160,330),(175,313),(175,325)])]
    for side in (False,True):
        def mirror(x,y):return (584-x if side else x,y)
        for points,center,depth,bulge,facet1,facet2 in feathers:
            # A longitudinal ridge and bevelled girdle break each blade into actual
            # crystal facets; no highlights are painted across the rounded outline.
            tip=min(range(len(points)),key=lambda i:points[i][0]+.12*points[i][1])
            indices=list(range(len(points)))
            border=[Vector(mirror(*points[i])) for i in indices]
            root,point=Vector(mirror(*points[0])),Vector(mirror(*points[tip]))
            a,b=root.lerp(point,.23),root.lerp(point,.72)
            verts=[world(p.x,p.y,depth) for p in border]
            edge=len(border)
            verts += [world(p.x,p.y,depth+bulge*.62) for p in border]
            verts += [world(a.x,a.y,depth-bulge*1.68),world(b.x,b.y,depth-bulge*1.38)]
            ca,cb=2*edge,2*edge+1
            faces=[];shades=[]
            stations=[(p-root).dot(point-root)/(point-root).length_squared for p in border]
            for k,p in enumerate(border):
                j=(k+1)%edge
                c1=ca if stations[k]<.45 else cb
                c2=ca if stations[j]<.45 else cb
                faces.append((k,j,c2) if c1==c2 else (k,j,c2,c1))
                shades.append(0 if p.y<(a.y+b.y)*.5 else 1)
                faces.append((k,k+edge,j+edge,j));shades.append(2)
            faces.append(tuple(reversed(range(edge,2*edge))));shades.append(2)
            crystal=mesh("Faceted ice wing blade",verts,faces,mat("83DDF1",.21,.15))
            crystal.data.materials.append(mat("40AACB",.25,.16))
            crystal.data.materials.append(mat("B3EDFA",.25,.08))
            for f,idx in zip(crystal.data.polygons,shades):
                f.use_smooth=False;f.material_index=idx
            edge_softness=crystal.modifiers.new("Polished ice edges","BEVEL")
            edge_softness.width=.046;edge_softness.segments=3
            crystal.modifiers.new("Wide facet normals","WEIGHTED_NORMAL")



def ice():
    open_dragon(replace_wings=True)
    frost_horns()
    tint_tail(.92)
    fit_props(ice_wings, x=8, y=69, depth=45)
    cx,cy=500,445
    for i in range(6):
        angle=math.tau*i/6
        u=np.array([math.sin(angle),-math.cos(angle)]);v=np.array([-u[1],u[0]])
        c=np.array([cx,cy]); seg=[(c,c+u*56),(c+u*28,c+u*42+v*15),(c+u*28,c+u*42-v*15)]
        for a,b in seg:
            for off,rad,color in [(3,8.4,"66C7E9"),(-1,5.3,"A1E8FA")]:
                line("Sculpted snowflake branch",[(a[0],a[1],off),(b[0],b[1],off)],rad,color)
                ell("Rounded ice tip",b[0],b[1],off,rad,rad,rad,color)
    ell("Crystal centre",cx,cy,-4,9.5,9.5,4,"A8EEFA")


def ruby():
    open_dragon()
    # A ruby's flat table, girdle and point are individually editable facets.
    pix=[(124,303,-59),(180,253,-57),(219,247,-55),(303,269,-55),(326,321,-59),
         (309,370,-58),(189,442,-68),(144,377,-59),(178,294,-90),(200,257,-75),
         (279,280,-81),(267,316,-94),(187,366,-83),(220,409,-75),(230,325,-34)]
    fs=[(1,9,8,0),(1,2,9),(2,3,10,9),(3,4,11,10),(9,10,11,8),(0,8,12,7),
        (8,11,6),(8,6,12),(11,4,5,13),(11,13,6),(12,6,7),(6,13,5)]
    fs += [(14,(i+1)%8,i) for i in range(8)]
    gem=mesh("Giant brilliant-cut red ruby",[world(300+(x-225)*.72,365+(y-250)*.80,-82+(d+60)*.55) for x,y,d in pix],fs,mat("CE1544",.22,.16))
    palette=["DA1743","FD8EA4","F35F83","9E0D32","FF94A7","971331","BC1242","F43962","A50837","DA1746","940E37","EF315D"]
    for col in palette:
        pigment=mat(col,.22,.14).copy();shader=next(n for n in pigment.node_tree.nodes if n.type=="BSDF_PRINCIPLED")
        shader.inputs["Emission Color"].default_value=rgba(col);shader.inputs["Emission Strength"].default_value=.135
        shader.inputs["Coat Weight"].default_value=.24;gem.data.materials.append(pigment)
    for i,f in enumerate(gem.data.polygons):
        f.use_smooth=False
        f.material_index=i+1 if i<len(palette) else 0
    polish=gem.modifiers.new("Fine polished bevel on ruby facets","BEVEL")
    polish.width=.016;polish.segments=3
    gem.modifiers.new("Cut stone face normals","WEIGHTED_NORMAL")
    fit_props(treasure, x=66, y=83, depth=96)


def panel(name,coords,thickness,color,bevel=1.8):
    n=len(coords)
    vs=[world(x,y,d+offset) for offset in (0,thickness) for x,y,d in coords]
    fs=[tuple(range(n)),tuple(reversed(range(n,2*n)))]+[(i,(i+1)%n,(i+1)%n+n,i+n) for i in range(n)]
    o=mesh(name,vs,fs,mat(color,.33,.11) if isinstance(color,str) else color)
    for f in o.data.polygons:f.use_smooth=False
    mod=o.modifiers.new("Rounded joinery","BEVEL");mod.width=bevel/100;mod.segments=4
    o.modifiers.new("Polished board normals","WEIGHTED_NORMAL")
    return o


def treasure():
    panel("Front of open treasure chest",[(344,447,-38),(458,448,-38),(458,520,-38),(344,521,-38)],9,"9E6039")
    panel("Right side of treasure chest",[(458,448,-38),(485,437,-3),(485,507,-3),(458,520,-38)],6,"81472C")
    panel("Dim inside the chest",[(345,447,-34),(371,432,-2),(482,435,-2),(457,448,-34)],5,"60361F")
    for y in (469,493,512):
        line("Wood plank seam",[(348,y,-38.6),(392,y+.6,-39),(456,y,-38.8)],.82,"825035",[.16,.85,.18])
    for x in (344,435):
        panel("Vertical brass strap",[(x,448,-41),(x+19,448,-41),(x+19,521,-41),(x,521,-41)],5,"E8AD4C",2.1)
    panel("Back of raised lid",[(359,402,5),(460,405,5),(483,444,8),(377,443,8)],5,"60351D")
    profile=[(359,401,-14),(361,393,-12),(366,382,-8),(374,372,-2),(383,366,5),(393,364,10)]
    def dome(name,left,w,material):
        rows=len(profile)
        vs=[world(x+left+side*w,y+(left+side*w)*.045,d) for x,y,d in profile for side in (0,1)]
        fs=[(2*j,2*j+1,2*j+3,2*j+2) for j in range(rows-1)]
        o=mesh(name,vs,fs,material)
        mod=o.modifiers.new("Thickness of lid","SOLIDIFY");mod.thickness=.045
        be=o.modifiers.new("Soft curled edge","BEVEL");be.width=.025;be.segments=3
    dome("Curved wooden treasure lid",0,95,"A3653D")
    dome("Left lid band",-1,16,mat("E7B259",.28,.15))
    dome("Right lid band",78,16,mat("E7B259",.28,.15))
    panel("Domed right cheek of lid",[(455,405,-13),(458,393,-9),(465,382,-5),(475,372,6),(481,376,9),
             (489,389,12),(495,406,14),(495,425,11),(490,442,7),(480,443,5)],5,"824A2D")
    for x,y,r in [(400,440,21),(435,448,19),(407,454,18)]:
        ell("Treasure gold coin",x,y,-28,r,r*.92,4,"E7A83B")
        ell("Embossed coin face",x,y,-32,r*.77,r*.73,2.2,"FFD360")
    panel("Golden chest latch",[(383,452,-45),(411,452,-45),(411,470,-45),(407,478,-45),(398,482,-45),
             (389,478,-45),(383,471,-45)],5,"F1BD65",2)
    ell("Dark keyhole",398,465,-47.8,2.8,4.8,1.0,"754C31")


def leaf_wings():
    for sign in (-1,1):
        def m(x,y):return (x if sign<0 else 586-x,y+(1 if sign>0 else 0))
        pts=[(211,331),(196,305),(185,274),(162,253),(138,242),(112,240),(87,246),
             (62,261),(47,275),(43,270),(47,260),(40,258),(33,264),(32,278),(39,292),
             (58,318),(76,351),(98,368),(123,375),(150,372),(168,362),(187,349)]
        if sign>0:pts=[(x,y-6 if y>310 else y+3) for x,y in pts]
        obj=pillow("Broad green leaf wing",[m(x,y) for x,y in pts],13,12,"8DB836",(m(128,309)))
        for j,line_pts in enumerate([[(202,341),(177,320),(145,306),(110,294),(75,287),(39,283)],
                                     [(177,320),(174,297),(163,270)],[(151,308),(145,284),(130,258)],
                                     [(121,298),(110,277),(98,261)],[(158,311),(136,325),(111,350)],
                                     [(126,302),(105,310),(84,326)]]):
            stroke("Wing leaf midrib" if not j else "Wing leaf vein",obj,[m(x,y) for x,y in line_pts],
                   3.5 if not j else 1.8,"B6CD46",[.89]+[.85]*(len(line_pts)-2)+[.10],.68)


def woodland_crown():
    for pts in [[(237,149,11),(222,124,10),(210,111,10),(183,106,7)],
                [(345,154,11),(364,132,10),(375,121,10),(397,119,7)],
                [(365,135,9),(367,113,9),(361,89,8)]]:
        limb("Branched copperwood horn",pts,11,.84,"B4793D")
    for a,b in [((183,106),(149,79)),((376,114),(411,76))]:
        ax,ay=a;bx,by=b
        strip("Fresh green horn leaf",[(ax,ay,ax,ay+2),(ax-3,ay-11,ax+8,ay-2) if bx>ax else (ax-8,ay-12,ax-3,ay),
               (bx-1,by+4,bx-3,by+18) if bx>ax else (bx+2,by+3,bx+8,by+18),(bx,by,bx+.6,by+.7)],7,.66,"8FBB39")
    # The bird actually perches on the left branch and watches the dragon below.
    ell("Little yellow bird body",211,78,-2,34,25,21,"FFD447")
    ell("Curious bird head",229,60,-4,33,32,25,"FFDA50")
    bird_wing=strip("Little folded wing",[(173,48,173,49),(187,49,171,67),(199,58,180,79),
          (208,70,194,83),(208,76,203,83)],-19,.54,"F8B938")
    for coords in [[(179,58),(184,67),(190,73)],[(183,58),(190,67),(197,73)]]:
        stroke("Bird wing feather vane",bird_wing,coords,1.0,"EBA929",[.14,1,.11],.6)
    strip("Tapering little tail feather",[(183,77,184,84),(171,74,174,83),(163,72,165,78),
          (159,71,160,72)],5,.50,"EFB532")
    strip("Bird crest",[(219,29,229,39),(223,19,230,30),(234,26,237,33)],1,.44,"FFCF4B")
    for x,y,rx,ry in [(232,61,8.5,10),(257,65,4.8,7.1)]:
        ell("Bird warm white eye",x,y,-27,rx,ry,3.2,"FFF6D8")
        ell("Downward bird pupil",x+1,y+3,-30,rx*.67,ry*.66,2.7,"442E20")
        ell("Bird eye glint",x+2.1,y,-33,rx*.25,rx*.25,1,"FFFFFF")
    pillow("Amber bird beak",[(232,76),(241,70),(249,75),(253,81),(247,89),(242,86),(236,83)],-25,4,"F39F2E")
    for x in (199,207,220):
        limb("Bird toes wrapped around the horn",[(x-3,100,2.7),(x+1,103,3.2),(x+2,109,2.8),(x,113,1.4)],-1,.8,"D6902C")


def woodland():
    open_dragon(replace_wings=True, replace_horns=True)
    fit_props(leaf_wings, x=7, y=50, depth=39)
    fit_props(woodland_crown, x=7, y=0)
    tint_tail(.64, .66)
    limb("Curl growing from the leaf tip",[(505,440,8),(515,422,8),(514,408,7),
         (502,401,7),(490,407,6),(489,417,5),(497,420,4),(500,415,2.2)],21,.86,"81AA41")


BUILDERS={"dragon-model-marshmallow":marshmallow,"dragon-model-ice":ice,
          "dragon-model-ruby":ruby,"dragon-model-woodland":woodland}


if __name__=="__main__":
    for item in sys.argv[sys.argv.index("--")+1:]:
        BUILDERS[item]()
        save_render(item)



