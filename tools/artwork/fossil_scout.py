"""The balancing fossil scout, sculpted in the fixed 600-unit portrait camera."""
import math

import bmesh
import bpy
import numpy as np
from mathutils import Matrix, Vector
from mathutils.geometry import delaunay_2d_cdt

from fantasy_sculpt import (
    begin, blend, fuse, mark_skin, mat, mesh, outline, pillow, rgba,
    softstep, spline, stroke, surface, world,
)
from reference_finish import ground_contact

from dinosaur_pigments import SKIN, SPOT, PLATE, BELLY, CLAW, CHEEK


def closed_loft(name, sections, material, cap=True):
    n=len(sections[0])
    faces=[(j*n+i,j*n+(i+1)%n,(j+1)*n+(i+1)%n,(j+1)*n+i)
           for j in range(len(sections)-1) for i in range(n)]
    if cap:
        faces.extend((tuple(reversed(range(n))),tuple(range(n*(len(sections)-1),n*len(sections)))))
    obj=mesh(name,[v for section in sections for v in section],faces,material)
    bm=bmesh.new();bm.from_mesh(obj.data)
    bmesh.ops.recalc_face_normals(bm,faces=bm.faces)
    bm.to_mesh(obj.data);bm.free()
    return obj


def skin(name, contour, center, depth, bulge, **kwargs):
    return pillow(name,contour,depth,bulge,mat(SKIN,.63),center,coat=True,**kwargs)


def relief(name, target, contour, center, color, height):
    depth=surface(target,*center)
    return pillow(name,contour,depth,height,color,center,
                  front=lambda x,y:surface(target,x,y,depth)-.45,rings=23)


def contour_distance(points, curve, signed=False):
    a=curve;b=np.roll(a,-1,axis=0);delta=b-a
    ss=(delta*delta).sum(axis=1)
    result=[]
    for chunk in np.array_split(points,max(1,math.ceil(len(points)/400))):
        rel=chunk[:,None,:]-a
        t=np.clip((rel*delta).sum(axis=2)/ss,0,1)
        distance=np.sqrt(np.min(((rel-delta*t[:,:,None])**2).sum(axis=2),axis=1))
        if signed:
            x,y=chunk[:,0,None],chunk[:,1,None]
            inside=np.logical_xor.reduce(((a[:,1]>y)!=(b[:,1]>y))&
                (x<delta[:,0]*(y-a[:,1])/(delta[:,1]+1.e-12)+a[:,0]),axis=1)
            distance=np.where(inside,distance,-distance)
        result.extend(distance)
    return np.asarray(result)


def sculpt_mass(name,shape,base,lobes,roll,adjust=None):
    """Triangulate the concave silhouette and round its front and back around the muscles."""
    border=np.array(outline(shape,8))
    grid=list(border);steps=1.45
    edge_delta=np.roll(border,-1,axis=0)-border
    tangent=np.roll(border,-1,axis=0)-np.roll(border,1,axis=0)
    inward=np.column_stack((-tangent[:,1],tangent[:,0]))/np.linalg.norm(tangent,axis=1)[:,None]
    if np.sum(border[:,0]*edge_delta[:,1]-border[:,1]*edge_delta[:,0])<0:inward*=-1
    for width in (.18,.58,1.25):
        grid.extend(border+inward*width)
    for row,y in enumerate(np.arange(border[:,1].min()+.65,border[:,1].max(),steps)):
        crosses=(border[:,1]>y)!=((border[:,1]+edge_delta[:,1])>y)
        b=border[crosses];e=edge_delta[crosses]
        xs=sorted(b[:,0]+(y-b[:,1])*e[:,0]/e[:,1])
        for left,right in zip(xs[::2],xs[1::2]):
            phase=row%2*steps*.5
            start=math.ceil((left+1.65-phase)/steps)*steps+phase
            grid.extend((x,y) for x in np.arange(start,right-1.65,steps))
    verts,_,triangles,sources,_,_=delaunay_2d_cdt([Vector(p) for p in grid],[],
        [list(range(len(border)))],1,.00001)
    pts=np.array([(p.x,p.y) for p in verts])
    rim=np.array([any(i<len(border) for i in origin) for origin in sources])
    x,y=pts.T
    distance=contour_distance(pts,border)
    edge=np.sqrt(-np.expm1(-distance/roll));edge[rim]=0
    height=np.zeros(len(x))
    for cx,cy,rx,ry,h in lobes:
        lobe=h*np.sqrt(np.maximum(0,1-((x-cx)/rx)**2-((y-cy)/ry)**2))
        softness=min(32,h*.62)
        height=np.maximum(height,lobe)+np.maximum(0,softness-np.abs(height-lobe))**2/(4*softness)
    height=np.maximum(height,5)*edge
    mid=np.broadcast_to(base(x,y),(len(x),))
    front=mid-height
    if adjust:front+=adjust(pts)*edge
    vertices=[world(px,py,d) for (px,py),d in zip(pts,front)]
    back=np.arange(len(pts))
    back[~rim]=len(pts)+np.arange(np.count_nonzero(~rim))
    vertices.extend(world(px,py,d) for (px,py),d in zip(pts[~rim],(mid+.72*height)[~rim]))
    faces=[tuple(f) for f in triangles]+[tuple(back[i] for i in reversed(f)) for f in triangles]
    obj=mesh(name,vertices,faces,mat(SKIN,.54),coat=True)
    if sum(obj.data.polygons[i].normal.y for i in range(0,len(triangles),100))>0:obj.data.flip_normals()
    return mark_skin(fuse([obj.name],name,.009))


def scout_eye(name,head,shape,lid,iris,glint):
    ix,iy,rx,ry,tilt=iris
    ca,sa=math.cos(tilt),math.sin(tilt)
    white,shade=rgba("FFFAF1"),rgba("DEDBD1")
    brown,gold,black=rgba("352511"),rgba("816539"),rgba("17130C")
    def pigment(x,y):
        u=((x-ix)*ca+(y-iy)*sa)/rx
        v=(-(x-ix)*sa+(y-iy)*ca)/ry
        r=math.hypot(u,v)
        sclera=blend(white,shade,.42*(1-softstep(-1,.3,v)))
        col=blend(brown,gold,.87*softstep(-.7,1,v))
        col=blend(col,rgba("291F11"),softstep(.81,1,r))
        col=blend(col,black,1-softstep(.63,.71,math.hypot(u-.06,(v+.08)*.99)))
        col=blend(col,sclera,softstep(.985,1.035,r))
        gx,gy,gr=glint
        return blend(col,white,1-softstep(.88,1.02,math.hypot(x-gx,y-gy)/gr))
    center=tuple(np.mean(shape,axis=0))
    depth=surface(head,*center)
    eye=pillow(name,shape,depth,2.85,mat("FFFAF1",.35),center,pigment,
               front=lambda x,y:surface(head,x,y,depth)-1,rings=62)
    next(n for n in eye.active_material.node_tree.nodes if n.type=="BSDF_PRINCIPLED").inputs["Specular IOR Level"].default_value=.13
    eye["blink_part"]=True
    lash=stroke(name+" curved upper lid",head,lid,2.15,"33291D",
                [.17]+[1]*(len(lid)-2)+[.17],2.1)
    lash["blink_part"]=True


def head():
    shape=[(198,156),(201,128),(213,105),(229,87),(251,74),(275,68),(298,67),
        (322,73),(342,87),(358,109),(366,134),(365,157),(360,179),(356,193),
        (359,199),(373,202),(387,210),(394,221),(395,232),(391,246),(382,257),
        (368,265),(351,270),(331,269),(313,263),(302,256),(297,257),(286,258),
        (276,253),(256,246),(232,230),(215,212),(202,188)]
    mouth=[(267,209),(270,215),(277,229),(286,242),(296,251),(301,254),
           (295,256),(285,255),(276,249),(271,240),(268,228)]
    mouth_curve=np.array(outline(mouth,5))
    obj=sculpt_mass("Scout head with projecting muzzle",shape,lambda x,y:-9,
        [(282,153,100,108,57),(337,230,79,50,78)],13,
        lambda p:5*np.clip((contour_distance(p,mouth_curve,True)+.8)/3.0,0,1))
    skin_color=rgba(SKIN);nostril=rgba("568C38")
    colors=[]
    for v in obj.data.vertices:
        x=300+v.co.x*100;y=600-v.co.z*100;d=v.co.y*100
        weight=softstep(28,48,-d)
        dx,dy=x-242.2,y-183.1
        cheek=math.hypot((.78*dx+.63*dy)/15.4,(-.63*dx+.78*dy)/10.9)
        blush=rgba(CHEEK)
        light=math.exp(-((x-308)/60)**2-((y-211)/23)**2)+.65*math.exp(-((x-273)/70)**2-((y-94)/46)**2)
        shade=.17*softstep(213,267,y)+.055*softstep(152,203,y)*(1-softstep(206,261,x))
        local=(skin_color[0]*(1-shade),skin_color[1]*(1-.65*shade),skin_color[2]*(1-.9*shade)+.06*light,1)
        color=blend(local,blush,(1-softstep(.80,1.06,cheek))*weight)
        cavities=0
        for cx,cy,rx,ry in [(358.7,236,4.8,5.4),(384.1,238.7,3.25,4.8)]:
            u=x-cx;w=y-cy
            r=math.hypot((u+.22*w)/rx,w/ry)
            depression=math.exp(-1.25*r**2)
            v.co.y+=(.057*depression-.011*math.exp(-((r-1.06)/.27)**2))*weight
            cavities=max(cavities,(1-softstep(.45,1.12,r))*weight)
        colors.append(blend(color,nostril,.91*cavities))
    attr=obj.data.color_attributes.new(name="Cheek and nostril pigments",type="FLOAT_COLOR",domain="POINT")
    attr.data.foreach_set("color",np.asarray(colors,dtype=np.float32).reshape(-1))
    material=obj.data.materials[0].copy();obj.data.materials[0]=material
    tree=material.node_tree
    node=tree.nodes.new("ShaderNodeVertexColor");node.layer_name=attr.name
    tree.links.new(node.outputs[0],next(n for n in tree.nodes if n.type=="BSDF_PRINCIPLED").inputs["Base Color"])
    obj.data.update()
    def mouth_color(x,y):
        u=x-282;v=y-243
        tongue=math.hypot((u-.52*v)/6.8,v/11.0)
        return blend(rgba("451D12"),rgba("C75D4E"),(1-softstep(.69,1.05,tongue))*.94)
    pillow("Small open smile with tongue",mouth,-35,.7,mat("572618",.65),(279,237),
           mouth_color,front=lambda x,y:surface(obj,x,y,-32)-.6,rings=33)
    scout_eye("Near downward-looking eye",obj,
        [(254,166),(255,154),(261,142),(269,135),(278,132),(286,133),(292,139),
         (295,149),(294,160),(288,174),(280,183),(272,185),(262,181),(256,175)],
        [(254,163),(256,151),(262,141),(271,134),(280,132),(287,134),(293,141),(295,151)],
        (278.5,169,13.5,18.9,.52),(283.9,161.1,3.9))
    scout_eye("Far downward-looking eye",obj,[(331.7,190),(334,178),(341,165),(348,157),
         (354,155),(357,158),(357,169),(351,184),(344,195),(338,199),(333,198)],
        [(332.5,188),(336,176),(342,164),(350,156),(356,157),(357,165)],
        (341.4,186.1,8.4,16.7,.49),(345.5,178.6,2.55))
    for brow in [[(269,109),(274,103),(282,99),(292,98),(301,102)],
                 [(356,123),(359,121),(363,126),(365,135)]]:
        stroke("Softly raised moss brow",obj,brow,2.65,"446B30",[.28]+[1]*(len(brow)-2)+[.25],1.15)


def dorsal_plates():
    contours=[
        [(252,76),(252,61),(257,43),(267,29),(278,24),(288,26),(298,35),(307,51),(314,74)],
        [(210,120),(198,101),(193,84),(193,66),(197,55),(205,49),(218,49),(234,55),(250,68),(259,82)],
        [(205,171),(194,169),(179,159),(165,145),(157,133),(157,122),(166,114),(179,109),(194,109),(207,114)],
        [(195,227),(181,226),(168,218),(160,206),(160,196),(169,185),(183,177),(198,174),(205,182)],
        [(123,384),(113,379),(107,368),(105,355),(109,345),(120,339),(140,335),(151,339)],
        [(76,424),(73,416),(73,401),(78,389),(85,381),(94,380),(105,384),(116,394)],
        [(46,431),(49,420),(57,411),(66,409),(73,413),(78,422)]]
    for i,shape in enumerate(contours):
        pillow("Rounded forest-green back plate "+str(i+1),shape,11,12,mat(PLATE,.68))


def claw(name,foot,shape,center):
    depths=[surface(foot,x,y,None) for x,y in [center,*outline(shape,3)]]
    depth=float(np.percentile([v for v in depths if v is not None],10))-1.2
    # The root grows from the toe. The rounded tip keeps its volume beyond the sole.
    obj=pillow(name,shape,depth,7.5,mat(CLAW,.49),center,rings=29)
    for vertex in obj.data.vertices:
        if vertex.co.y*100>depth:vertex.co.y+=(vertex.co.y-depth/100)*1.5
    obj.data.update()
    return obj


def body_and_feet():
    tail=skin("Counterbalancing tail",[(33,439),(38,430),(48,427),(69,425),
        (93,418),(117,401),(139,377),(156,351),(175,344),(190,364),
        (175,400),(161,426),(143,446),(118,459),(91,465),(65,465),(46,460),(36,451)],
        (123,421),8,30)
    trunk=skin("Upright rib cage and hips",[(151,363),(152,320),(169,270),(184,246),
        (194,224),(201,184),(219,189),(252,219),(270,245),(283,275),(287,319),
        (275,350),(255,374),(218,395),(185,403),(161,392)],(216,309),6,37)
    haunch=skin("Round supporting haunch",[(132,362),(137,344),(150,332),(174,327),
        (194,340),(200,365),(196,392),(183,409),(166,411),(150,403),(137,386)],
        (167,370),-12,28)
    grounded=skin("Planted leg and broad sole",[(178,391),(202,378),(216,395),(216,440),
        (223,449),(237,453),(248,461),(251,477),(245,483),(225,487),(197,487),
        (176,482),(168,475),(168,463),(173,436),(179,415)],(198,444),7,33)
    main=mark_skin(fuse([trunk.name,tail.name,haunch.name,grounded.name],"Balanced torso tail and planted leg",.011))
    points=np.array([v.co[:] for v in main.data.vertices]);x=300+points[:,0]*100;y=600-points[:,2]*100
    coverage=np.zeros(len(points))
    for cx,cy,rx,ry,angle in [(112.8,412.4,7.3,13.8,.61),(85.7,430.8,6.2,8.3,.82)]:
        ca,sa=math.cos(angle),math.sin(angle)
        u=(x-cx)*ca+(y-cy)*sa;v=-(x-cx)*sa+(y-cy)*ca
        coverage=np.maximum(coverage,np.clip((1.06-np.hypot(u/rx,v/ry))/.13,0,1))
    coverage=coverage[:,None]
    colors=(1-coverage)*np.array(rgba(SKIN))+coverage*np.array(rgba(SPOT))
    attr=main.data.color_attributes.new(name="Tail scales",type="FLOAT_COLOR",domain="POINT")
    attr.data.foreach_set("color",colors.astype(np.float32).reshape(-1))
    material=main.data.materials[0].copy();main.data.materials[0]=material
    tree=material.node_tree
    node=tree.nodes.new("ShaderNodeVertexColor");node.layer_name=attr.name
    tree.links.new(node.outputs[0],next(n for n in tree.nodes if n.type=="BSDF_PRINCIPLED").inputs["Base Color"])
    def fold(x,y):
        return sum(math.exp(-((y-c+.008*(x-237)**2)/1.0)**2) for c in (290,327))
    pillow("Warm cream belly",[(208,311),(213,287),(223,264),(237,253),(250,253),
        (264,259),(275,275),(283,294),(285,315),(281,339),(273,356),(262,369),(242,375),
        (222,361),(209,338)],-24,5.0,mat(BELLY,.64),(247,311),
        lambda x,y:blend(rgba(BELLY),rgba("DFB578"),.56*fold(x,y)),
        front=lambda x,y:surface(main,x,y,-17)-1,extra=lambda x,y:.6*fold(x,y),rings=72)
    lifted=sculpt_mass("Lifted thigh heel and upturned foot",[(197,338),(210,330),(226,327),(240,330),
        (253,340),(266,355),(278,370),(289,366),(303,362),(314,362),(322,360.6),(332,363),
        (342,369),(349,378),(350,390),(344,398),(324,411),(302,424),(285,430),
        (273,431),(259,425),(250,413),(241,398),(226,387),(209,372),(197,356)],
        lambda x,y:-32,[(229,352,45,48,34),(265,392,34,48,26),
        (304,395,48,39,38),(326,378,25,21,34)],6.6)
    digits=[]
    for cx,cy,rx,ry in [(301,389,20,18),(316,380,18,17),(330,375,15,13)]:
        depth=surface(lifted,cx,cy)
        shape=[(cx+rx*math.cos(a),cy+ry*math.sin(a)-.24*rx*math.cos(a))
               for a in np.linspace(0,math.tau,16,endpoint=False)]
        digits.append(skin("Rounded toe beneath claw",shape,(cx,cy),depth+9,15).name)
    lifted=mark_skin(fuse([lifted.name,*digits],"Lifted thigh heel and upturned foot",.009))
    for name,foot,shape,center in [
        ("Inner planted claw",main,[(202,482),(203,474),(207,468),(213,466),(219,470),(222,478),(221,486),(214,487),(205,487)],(212,478)),
        ("Middle planted claw",main,[(223,480),(224,470),(230,464),(235,464),(240,473),(241,482),(235,486),(226,486)],(232,477)),
        ("Outer planted claw",main,[(241,476),(241,467),(244,461),(248,461),(252,468),(253,478),(249,482),(243,482)],(247,472)),
        ("Inner raised claw",lifted,[(295,396),(298,387),(304,385),(313,389),(320,398),(318,406),(310,413),(301,415),(296,410)],(306,400)),
        ("Middle raised claw",lifted,[(313,386),(316,378),(321,375),(327,376),(335,383),(339,392),(336,397),(326,404),(320,402)],(325,389)),
        ("Outer raised claw",lifted,[(328,376),(329,370),(333,367),(339,368),(346,375),(350,382),(350,387),(344,392),(337,393)],(339,380))]:
        claw(name,foot,shape,center)


def satchel():
    leather=mat("D18C27",.52)
    def ring(cx,cy,d,rx,rd,lift):
        return [world(cx+rx*math.cos(a),cy-.32*rx*math.cos(a)-lift*math.sin(a),d+rd*math.sin(a))
                for a in np.linspace(0,math.tau,84,endpoint=False)]
    stations=[(134,333,10,3,3,1),(136,327,9,22,18,3),(136,309,9,36,24,4),
              (136,284,10,40,26,6),(138,258,10,35,25,8),(139,244,10,32,25,9),
              (139,240,10,32,25,10),(139,240,10,27,20,8),(139,257,10,24,17,7),
              (137,298,10,8,7,2),(136,304,10,2,2,1)]
    bag=closed_loft("Open golden field pack",[ring(*r) for r in spline(stations,4)],leather)
    collar=[(139,254,10,32,26,9),(139,253,10,36,28,9),(139,249,10,36.5,28,10),
            (139,239,10,34,27,10),(139,237,10,31,24,9),(139,238,10,27.5,20,8),
            (139,248,10,27,19,8),(139,254,10,32,26,9)]
    rim=closed_loft("Turned leather lip and dark lining",[ring(*r) for r in spline(collar,4)],mat("D59838",.51),False)
    rim.data.materials.append(mat("94642A",.72))
    for f in rim.data.polygons:
        if 17<=f.index//84<=25:f.material_index=1
    relief("Field pack side panel",bag,[(127,327),(129,307),(133,282),(139,264),(145,262),
          (143,281),(139,303),(138,325),(134,330)],(137,294),mat("C78A2F",.58),1.7)
    # A single sheet winds from the loose outer edge to the curl visible in the opening.
    sections=[]
    for t in np.linspace(0,1,35):
        points=[]
        for u in np.linspace(0,1,145):
            a=-.28+math.tau*1.30*u
            r=19.2-15*u
            cx,cy=128+14*t,196+65*t
            dx=r*math.cos(a)
            points.append(world(cx+dx,cy-.30*dx-r*.49*math.sin(a),5+r*math.sin(a)-9*t))
        sections.append(points)
    verts=[p for row in sections for p in row];n=len(sections[0])
    faces=[(j*n+i,j*n+i+1,(j+1)*n+i+1,(j+1)*n+i) for j in range(len(sections)-1) for i in range(n-1)]
    scroll=mesh("Single spiral of rolled field notes",verts,faces,mat("F2CF93",.68))
    solid=scroll.modifiers.new("Parchment thickness","SOLIDIFY");solid.thickness=.012


def shoulder_strap():
    sections=[]
    points=spline([(149,313,-8,4),(149,281,-14,5),(160,255,-17,6.5),
        (174,239,-23,8),(187,236,-27,9),(201,240,-28,10),(214,251,-27,10),
        (218,265,-26,10),(213,283,-29,10),(203,296,-31,9),(195,307,-30,6)],7)
    for i,(x,y,d,w) in enumerate(points):
        dx,dy=points[min(i+1,len(points)-1),:2]-points[max(0,i-1),:2]
        size=max(.001,math.hypot(dx,dy))
        sections.append([world(x-w*dy/size*math.cos(a),y+w*dx/size*math.cos(a),d+5.5*math.sin(a))
                         for a in np.linspace(0,math.tau,40,endpoint=False)])
    closed_loft("Padded strap around the near shoulder",sections,mat("D79838",.52))
    pillow("Leather strap keeper",[(139,311),(146,309),(157,310),(159,319),(155,328),
        (143,328),(138,325)],-21,4,mat("CA8B2E",.57))
    pillow("Far backpack strap behind the arm",[(267,247),(273,247),(279,255),
        (277,278),(270,283),(268,272)],0,5,mat("B57B28",.6))


def field_brush():
    # Width, thickness and sections are perpendicular to the slant of the handle.
    origin=np.array((185.0,329.0))
    along=np.array((.485,.8745));across=np.array((along[1],-along[0]))
    def section(t,w,z,rd,power=1,bristles=False):
        row=[]
        for a in np.linspace(0,math.tau,120,endpoint=False):
            ca,sa=math.cos(a),math.sin(a)
            u=math.copysign(abs(ca)**power,ca);v=math.copysign(abs(sa)**power,sa)
            grooves=sum(math.exp(-((u-k)/.039)**2) for k in (-.67,-.32,.04,.39,.72))
            amount=softstep(76,103,t) if bristles else 0
            tip=t-amount*(2.6*u*u+3.1*grooves)
            xy=origin+along*tip+across*w*u
            row.append(world(*xy,z+rd*v+1.6*grooves*amount*max(0,-v)))
        return row
    wood=mat("AD6D32",.55)
    closed_loft("Honeysuckle wood brush handle",[section(*r) for r in [
        (-8,1,-62,1),(-6,5.2,-63,5),(1,6.2,-64,6),(47,7.1,-64,6),(57,8,-65,7)]],wood)
    closed_loft("Rounded ochre brush ferrule",[section(*r,.77) for r in [
        (51,6.5,-65,6),(53,14,-65,7),(56,20,-65,8.5),(60,24.5,-65,9.5),
        (66,25.5,-65,10),(70,23,-65,9)]],mat("BE853F",.47))
    bristle_sections=[section(t,w,-65,rd,.61,True) for t,w,rd in
        [(66,21,8),(70,22,8.5),(79,23,9),(90,25,9.5),(100,28,10),(105,30,10),
         (108,29,9),(109,26.5,7)]]
    tuft=closed_loft("Fanned and fluted cream bristles",bristle_sections,mat("F1CC91",.76))
    base=rgba("FFE0A8");shadow=rgba("D6AE73")
    colors=[]
    for v in tuft.data.vertices:
        x=300+v.co.x*100;y=600-v.co.z*100
        off=np.array((x,y))-origin;t=float(off@along);u=float(off@across)/max(22,22+(t-70)*.175)
        folded=sum(math.exp(-((u-k)/.039)**2) for k in (-.67,-.32,.04,.39,.72))
        colors.append(blend(base,shadow,min(.85,.08*(1+math.sin(u*150))+.50*folded+.21*softstep(106,114,t))))
    attr=tuft.data.color_attributes.new(name="Bristle fibres",type="FLOAT_COLOR",domain="POINT")
    attr.data.foreach_set("color",np.array(colors,dtype=np.float32).reshape(-1))
    material=tuft.data.materials[0].copy();tuft.data.materials[0]=material
    tree=material.node_tree;node=tree.nodes.new("ShaderNodeVertexColor");node.layer_name=attr.name
    tree.links.new(node.outputs[0],next(n for n in tree.nodes if n.type=="BSDF_PRINCIPLED").inputs["Base Color"])


def forepaws():
    near=[(175,249),(188,242),(198,245),(204,252),(207,261),(205,273),(200,284),(194,291),
          (196,297),(203,306),(208,316),(209,327),(206,338),(201,341),(196,339),
          (193,338),(188,344),(182,346),(177,344),(174,343),(168,344),(162,340),
          (158,334),(155,323),(150,311),(149,297),(153,283),(162,270),(174,258)]
    def palm_folds(points):
        x,y=points.T
        return (4.3*np.clip((y-325)/16,0,1)*np.exp(-((x-173-.09*(y-326))/2.1)**2)
                +4.5*np.clip((y-321)/17,0,1)*np.exp(-((x-196+.18*(y-326))/2.0)**2))
    def ease(x):
        t=np.clip(x,0,1);return t*t*(3-2*t)
    brush_arm = sculpt_mass("Near arm with curled brush-gripping digits",near,
        lambda x,y:-32-34*ease((y-258)/78),
        [(186,265,32,43,21),(171,297,25,36,19),(181,323,33,31,23)],8,palm_folds)
    # Mirror the sculpt at the shoulders before turning the free palm towards the flank.
    free_arm = brush_arm.copy()
    free_arm.name = "Far arm with resting digits"
    free_arm.data = brush_arm.data.copy()
    bpy.context.scene.collection.objects.link(free_arm)
    pose = (
        Matrix.Translation(world(267, 249, 5))
        @ Matrix.Rotation(math.radians(13), 4, "Z")
        @ Matrix.Rotation(math.radians(-7), 4, "Y")
        @ Matrix.Diagonal((-1, 1, 1, 1))
        @ Matrix.Translation(-Vector(world(188, 249, -32)))
    )
    free_arm.data.transform(pose)
    free_arm.data.flip_normals()


def fossil_stone():
    # Signed distance to a single track boundary controls both the carving and the mineral pigment.
    footprint=[(345,478),(349,464),(363,448),(381,433),(404,420),(421,415),(428,417),
        (429,422),(423,431),(413,441),(409,447),(426,441),(444,435),(458,434),
        (465,437),(464,443),(454,451),(440,459),(435,463),(451,462),(467,464),
        (476,468),(475,473),(466,479),(447,488),(420,497),(391,501),(366,499),(352,494),(345,487)]
    curve=np.array(outline(footprint,5))
    border=[(262,518),(267,496),(278,479),(290,454),(307,430),(331,415),
        (360,406),(398,397),(431,390),(452,390),(478,398),(509,409),(530,422),
        (546,450),(555,484),(556,501),(552,516),(539,532),(514,545),
        (479,555),(438,561),(402,565),(367,567),(324,564),(291,557),(272,546),(264,535)]
    edge=np.array(outline(border,5))
    center=np.array((405,467))
    wet= np.clip((edge[:,1]-390)/176,0,1)
    table=edge.copy();table[:,0]=center[0]+(edge[:,0]-center[0])*.946
    table[:,1]-=2+28*wet**1.23
    table[:,1]-=(3+5*np.sin(edge[:,0]*.052)+2.2*np.sin(edge[:,0]*.094))*np.clip((edge[:,1]-456)/76,0,1)
    radial=np.linspace(.003,1,111)
    xy=center+(table[None,:,:]-center)*radial[:,None,None]
    distances=contour_distance(xy.reshape((-1,2)),curve,True)
    stone=rgba("EBC38B");well=rgba("CC9A55");colors=[];sections=[]
    def top_depth(x,y):return -110-.84*(y-460)
    def weathering(x,y):
        return 1.24*math.sin(x*.137+.6*math.sin(y*.097))*math.cos(.135*y)+.48*math.sin(.35*x+.25*y)
    def variation(x,y):
        return .059*math.sin(x*.081+.5*math.sin(y*.055))*math.cos(y*.096)+.022*math.sin(.31*x+.23*y)
    def pigment(x,y,amount,side=0):
        color=blend(stone,well,amount);v=1+variation(x,y)
        return tuple(k*(v-side*rate) for k,rate in zip(color[:3],(.85,1.03,1.23)))+(1,)
    for j,row in enumerate(xy):
        section=[]
        for i,(x,y) in enumerate(row):
            signed=distances[j*len(edge)+i]
            depth=13*softstep(-.55,7,signed)+4.6*softstep(5,22,signed)
            section.append(world(x,y,top_depth(x,y)+depth+weathering(x,y)))
            color=pigment(x,y,softstep(-.3,5.4,signed)*.9)
            dark=.15*math.exp(-((signed-4.2)/2.9)**2)
            colors.append(tuple(c*(1-dark) for c in color[:3])+(1,))
        sections.append(section)
    for angle in np.linspace(.10,math.pi/2,13):
        t=math.sin(angle);xy_ring=(1-t)*table+t*edge
        section=[]
        for i,((x,y),(tx,ty)) in enumerate(zip(xy_ring,table)):
            z=(24+10*wet[i])*(1-math.cos(angle))
            section.append(world(x,y,top_depth(x,y)+z+weathering(x,y)*(1-t*.6)))
            colors.append(pigment(x,y,.27*t,.58*softstep(.08,.88,t)))
        sections.append(section)
    n=len(edge);verts=[v for row in sections for v in row]
    faces=[(j*n+i,j*n+(i+1)%n,(j+1)*n+(i+1)%n,(j+1)*n+i) for j in range(len(sections)-1) for i in range(n)]
    faces.extend((tuple(reversed(range(n))),tuple(range(len(verts)-n,len(verts)))))
    rock=mesh("Honey sandstone with a recessed three-toed track",verts,faces,mat("E4B77C",.83),colors)
    if rock.data.polygons[n*10].normal.y>0:rock.data.flip_normals()
    m=rock.active_material;t=m.node_tree
    noise=t.nodes.new("ShaderNodeTexNoise");noise.inputs["Scale"].default_value=48;noise.inputs["Detail"].default_value=3.8
    noise.inputs["Roughness"].default_value=.69;noise.inputs["Distortion"].default_value=1.1
    bump=t.nodes.new("ShaderNodeBump");bump.inputs["Strength"].default_value=.53;bump.inputs["Distance"].default_value=.026
    t.links.new(noise.outputs["Fac"],bump.inputs["Height"])
    shader=next(n for n in t.nodes if n.type=="BSDF_PRINCIPLED")
    t.links.new(bump.outputs[0],shader.inputs["Normal"])
    mineral=t.nodes.new("ShaderNodeValToRGB")
    dark,light=mineral.color_ramp.elements
    dark.position=.32;dark.color=(.52,.44,.29,1)
    light.position=.68;light.color=(1.25,1.23,1.17,1)
    t.links.new(noise.outputs["Fac"],mineral.inputs[0])
    mix=t.nodes.new("ShaderNodeMixRGB");mix.blend_type="MULTIPLY";mix.inputs[0].default_value=.70
    pigment_node=next(n for n in t.nodes if n.type=="VERTEX_COLOR")
    t.links.new(pigment_node.outputs[0],mix.inputs[1]);t.links.new(mineral.outputs[0],mix.inputs[2])
    t.links.new(mix.outputs[0],shader.inputs["Base Color"])


def fossil():
    scene=begin("dinosaur")
    lights={o.name:o for o in scene.objects if o.type=="LIGHT"}
    lights["Silk portrait softbox"].location=Vector((-3.7,-4.1,9.1))
    lights["Silk portrait softbox"].rotation_euler=(Vector((0,0,3))-lights["Silk portrait softbox"].location).to_track_quat("-Z","Y").to_euler()
    lights["Silk portrait softbox"].data.energy=760
    lights["Reflected front fill"].data.energy=238
    lights["Low reflector"].data.energy=110
    lights["Soft rim"].data.energy=150
    scene.world.node_tree.nodes["Background"].inputs["Strength"].default_value=.22
    dorsal_plates()
    body_and_feet()
    satchel()
    shoulder_strap()
    field_brush()
    forepaws()
    head()
    fossil_stone()
    for label,x,y,rx,ry,opacity in [("Tail",88,463,65,12,.36),("Planted foot",208,485,54,11,.39),
                                    ("Sandstone",410,557,155,23,.44)]:
        ground_contact(label+" resting contact",[world(x+u*rx,y+v*ry,185) for u,v in
                       [(-1,-1),(1,-1),(1,1),(-1,1)]],opacity,"Warm studio contact shadow")
