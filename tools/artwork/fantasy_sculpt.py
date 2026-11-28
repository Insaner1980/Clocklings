"""Closed studio sculpts in the square reference camera's coordinate system."""
import math
from pathlib import Path
import sys

import bpy
import numpy as np
from mathutils import Vector
from mathutils.geometry import interpolate_bezier

sys.path.insert(0,str(Path(__file__).resolve().parent))
from transformation_studio import ball, box, paint, rod, save_render, setup, tube
from render_unicorn_layers import rgba
from reference_finish import fuse

W=600


def world(x,y,d=0):
    return ((x-300)/100,d/100,(600-y)/100)


def begin(kind):
    s=setup(kind,(0,0,3),(0,-1,0),6,keep_body=False)
    s.render.resolution_x=s.render.resolution_y=816
    s.render.threads=10
    s.cycles.samples=40
    s.view_settings.view_transform="Standard"
    s.view_settings.look="None"
    s.view_settings.exposure=-.30
    for o in list(s.objects):
        if o.type=="LIGHT":bpy.data.objects.remove(o,do_unlink=True)
    for name,location,power,diameter,tint in [
        ("Silk portrait softbox",(-3.6,-5.5,7.6),640,4.7,(1,.94,.92)),
        ("Reflected front fill",(4.4,-5.3,4.7),305,5.2,(1,.93,.99)),
        ("Soft rim",(-1.8,2.3,7.1),208,4.1,(.94,.94,1)),
        ("Low reflector",(-1,-4.9,1.0),84,4.1,(1,.94,.90))]:
        data=bpy.data.lights.new(name,"AREA");data.energy=power;data.size=diameter;data.color=tint;data.shape="DISK"
        o=bpy.data.objects.new(name,data);s.collection.objects.link(o);o.location=location
        o.rotation_euler=(Vector((0,0,3))-o.location).to_track_quat("-Z","Y").to_euler()
    bg=next(n for n in s.world.node_tree.nodes if n.type=="BACKGROUND")
    bg.inputs["Color"].default_value=(1,.92,.96,1)
    bg.inputs["Strength"].default_value=.30
    if kind=="unicorn":
        s.view_settings.view_transform="Standard"
        s.view_settings.look="None"
        s.view_settings.exposure=-.42
    return s


def mat(col,rough=.34,metal=0):
    return paint(col,rough,metal) if isinstance(col,str) else col


def ell(name,x,y,d,rx,ry,rd,c,coat=False):
    o=ball(name,world(x,y,d),(rx/100,rd/100,ry/100),mat(c))
    if coat:o["coat_palette"]=True
    return o


def line(name,pts,r,c,rad=None,closed=False):
    return tube(name,[world(*p) for p in pts],r/100,mat(c),rad,closed)


def cylinder(name,a,b,r,c,r2=None):
    return rod(name,world(*a),world(*b),r/100,mat(c),r2/100 if r2 is not None else None)


def mesh(name,vs,fs,col,colors=None,coat=False):
    data=bpy.data.meshes.new(name)
    data.from_pydata(vs,[],fs)
    data.update()
    o=bpy.data.objects.new(name,data)
    bpy.context.scene.collection.objects.link(o)
    for p in data.polygons:p.use_smooth=True
    m=mat(col)
    if colors is not None:
        m=m.copy()
        m.name=name+" pigments"
        attr=data.color_attributes.new(name="Studio pigments",type="FLOAT_COLOR",domain="POINT")
        arr=np.array(colors,dtype=np.float32)
        attr.data.foreach_set("color",arr.reshape(-1))
        node=m.node_tree.nodes.new("ShaderNodeVertexColor")
        node.layer_name=attr.name
        shader=next(n for n in m.node_tree.nodes if n.type=="BSDF_PRINCIPLED")
        m.node_tree.links.new(node.outputs["Color"],shader.inputs["Base Color"])
    data.materials.append(m)
    if coat:o["coat_palette"]=True
    return o


def outline(anchors,res=5):
    c=bpy.data.curves.new("Contour tangents","CURVE")
    c.dimensions="3D"
    s=c.splines.new("BEZIER")
    s.bezier_points.add(len(anchors)-1)
    for p,co in zip(s.bezier_points,anchors):
        p.co=(*co,0)
        p.handle_left_type=p.handle_right_type="AUTO"
    s.use_cyclic_u=True
    values=[]
    for i,p in enumerate(s.bezier_points):
        q=s.bezier_points[(i+1)%len(anchors)]
        values.extend(interpolate_bezier(p.co,p.handle_right,q.handle_left,q.co,res+1)[:-1])
    bpy.data.curves.remove(c)
    return [(v.x,v.y) for v in values]


def surface(obj,x,y,otherwise=0):
    hit,p,_,_=obj.ray_cast(Vector(world(x,y,-400)),Vector((0,1,0)))
    return p.y*100 if hit else otherwise


def pillow(name,anchors,depth,bulge,c,center=None,color_fn=None,front=None,extra=None,coat=False,rings=25):
    points=outline(anchors,6)
    cx,cy=center or (sum(x for x,y in anchors)/len(anchors),sum(y for x,y in anchors)/len(anchors))
    n=len(points)
    vs=[];fs=[];colors=[]
    def add(x,y,d):
        vs.append(world(x,y,d))
        if color_fn: colors.append(color_fn(x,y))
    def d(x,y):
        return front(x,y) if front else depth
    for side in (-1,1):
        start=len(vs)
        add(cx,cy,d(cx,cy)+side*bulge+(extra(cx,cy) if extra and side<0 else 0))
        for j in range(1,rings+1):
            u=math.sin(.5*math.pi*j/rings)
            bump=math.cos(.5*math.pi*j/rings)
            for px,py in points:
                x,y=cx+(px-cx)*u,cy+(py-cy)*u
                add(x,y,d(x,y)+side*bulge*bump+(extra(x,y)*bump if extra and side<0 else 0))
        faces=[(start,start+i+1,start+1+(i+1)%n) for i in range(n)]
        for j in range(rings-1):
            for i in range(n):
                a=start+1+j*n+i;b=start+1+j*n+(i+1)%n
                faces.append((a,a+n,b+n,b))
        fs.extend(faces if side<0 else [tuple(reversed(f)) for f in faces])
    # Join both hemispheres at one shared equator. Voxel-fused necks, hands and
    # torsos need a closed surface to keep their contours at the joints.
    rim = 1+(rings-1)*n
    duplicate = len(vs)-n
    fs = [tuple(rim+i-duplicate if i>=duplicate else i for i in f) for f in fs]
    obj = mesh(name,vs[:duplicate],fs,c,colors[:duplicate] if color_fn else None,coat)
    if obj.data.polygons[0].normal.y>0:obj.data.flip_normals()
    return obj


def spline(values,steps=6):
    # Interpolating coordinates and radii together keeps silhouettes continuous.
    p=np.array(values,dtype=float); result=[]
    for i in range(len(p)-1):
        a,b,c,d=p[max(0,i-1)],p[i],p[i+1],p[min(len(p)-1,i+2)]
        for j in range(steps):
            t=j/steps
            result.append(.5*(2*b+(-a+c)*t+(2*a-5*b+4*c-d)*t*t+(-a+3*b-3*c+d)*t*t*t))
    result.append(p[-1])
    return np.array(result)


def strip(name,profiles,d,thick,c,coat=False,color_fn=None):
    """Each section specifies two opposite silhouette points, with elliptical depth."""
    groomed=any(word in name.lower() for word in ("mane","forelock","tail curl","curled tail","wave","neck curl"))
    profiles=list(profiles)
    if groomed:
        x,y,u,v=profiles[-1]
        width=math.hypot(x-u,y-v)
        if width>4:
            prev=np.array(profiles[-2],dtype=float).reshape(2,2).mean(axis=0)
            tip=np.array(((x+u)/2,(y+v)/2))
            tangent=tip-prev
            tangent/=max(np.linalg.norm(tangent),.1)
            tip+=tangent*min(width*.66,8)
            profiles.append((*tip,tip[0]+.28,tip[1]+.28))
    sections=spline(profiles,7)
    sides=48;vs=[];colors=[]
    for station,(x0,y0,x1,y1) in enumerate(sections):
        cx=(x0+x1)*.5;cy=(y0+y1)*.5
        w=math.hypot(x1-x0,y1-y0)*.5
        for i in range(sides):
            a=math.tau*i/sides
            x=cx+(x1-x0)*.5*math.cos(a);y=cy+(y1-y0)*.5*math.cos(a)
            if "streaming carousel ribbon" in name:
                end=softstep(.68,1,station/(len(sections)-1))
                y-=end*(10+4*math.cos(a))
                x-=end*6.5*(1-abs(math.cos(a)))
            wave=math.sin(a)
            if groomed and wave<0:
                # Shallow longitudinal channels divide each lock into flowing strands.
                across=math.cos(a)
                groove=sum(math.exp(-((across-c)/.14)**2) for c in (-.47,.24))
                wave*=1-.064*groove
            vs.append(world(x,y,d+thick*w*wave))
            if color_fn:colors.append(color_fn(x,y))
    fs=[(j*sides+i,j*sides+(i+1)%sides,(j+1)*sides+(i+1)%sides,(j+1)*sides+i)
        for j in range(len(sections)-1) for i in range(sides)]
    fs.extend([tuple(reversed(range(sides))),tuple(range((len(sections)-1)*sides,len(sections)*sides))])
    return mesh(name,vs,fs,c,colors if color_fn else None,coat)


def limb(name,knots,d,aspect,c,coat=False):
    # Round tapered muscles and hair sections may turn through any angle.
    p=spline(knots,8);v=[];sides=36
    for j,(x,y,r) in enumerate(p):
        dx,dy=p[min(len(p)-1,j+1),:2]-p[max(0,j-1),:2]
        norm=max(.001,math.hypot(dx,dy))
        for i in range(sides):
            a=math.tau*i/sides
            v.append(world(x-dy/norm*max(r,.12)*math.cos(a),y+dx/norm*max(r,.12)*math.cos(a),
                           d+max(r,.12)*aspect*math.sin(a)))
    f=[(j*sides+i,j*sides+(i+1)%sides,(j+1)*sides+(i+1)%sides,(j+1)*sides+i)
       for j in range(len(p)-1) for i in range(sides)]
    f.extend([tuple(reversed(range(sides))),tuple(range((len(p)-1)*sides,len(p)*sides))])
    return mesh(name,v,f,c,coat=coat)


def blend(a,b,t):
    t=max(0,min(1,t))
    return tuple(x+(y-x)*t for x,y in zip(a,b))


def softstep(a,b,x):
    t=max(0,min(1,(x-a)/(b-a)))
    return t*t*(3-2*t)


def face(name,anchors,center,depth,bulge,coat,muzzle,cheeks,nose_color="FADBCA",blush="F4A4BD"):
    cx,cy,rx,ry=muzzle
    coat_color=rgba(coat);nose=rgba(nose_color);rose=rgba(blush)
    def pigments(x,y):
        n=((x-cx)/rx)**2+((y-cy)/ry)**2
        col=blend(coat_color,nose,1-softstep(.43,1.30,n))
        for ax,ay,w,h in cheeks:
            r=((x-ax)/w)**2+((y-ay)/h)**2
            col=blend(col,rose,(1-softstep(.32,1.10,r))*.86)
        return col
    def bulge_nose(x,y):
        return -bulge*.31*math.exp(-1.5*(((x-cx)/rx)**2+((y-cy)/ry)**2))
    return pillow(name,anchors,depth,bulge,coat,center,pigments,extra=bulge_nose,coat=True,rings=55)


def stroke(name,obj,xy,r,c,rad,offset=1.1):
    return line(name,[(x,y,surface(obj,x,y)-offset) for x,y in xy],r,c,rad)


def patch(name,base,x,y,rx,ry,col,raise_by=.5):
    pts=[(x+rx*math.cos(math.tau*i/10),y+ry*math.sin(math.tau*i/10)) for i in range(10)]
    return pillow(name,pts,-40,.52,col,(x,y),front=lambda xx,yy:surface(base,xx,yy)-raise_by,rings=8)


def eye(name,head,boundary,lid,iris,pupil,glint,iris_color="9851BD",lash="362237"):
    ix,iy,rx,ry=iris
    px,py,prx,pry=pupil
    gx,gy,gr=glint
    white=rgba("FFF8FD");edge=rgba("43284F");top=rgba("39203F");base=rgba(iris_color);black=rgba("24182C")
    def iris_paint(x,y):
        r=math.sqrt(((x-ix)/rx)**2+((y-iy)/ry)**2)
        sclera=blend(white,rgba("D9C4DE"),.25*(1-softstep(-1,1,(y-iy)/ry)))
        core=blend(top,base,softstep(-.45,.90,(y-iy)/ry))
        col=blend(sclera,blend(edge,core,1-softstep(.86,1,r)),1-softstep(.98,1.08,r))
        pr=math.sqrt(((x-px)/prx)**2+((y-py)/pry)**2)
        col=blend(col,black,1-softstep(.91,1.04,pr))
        gradius=math.hypot(x-gx,y-gy)/gr
        return blend(col,(1,1,1,1),1-softstep(.84,1.06,gradius))
    cx=sum(x for x,y in boundary)/len(boundary);cy=sum(y for x,y in boundary)/len(boundary)
    o=pillow(name,boundary,-35,2.5,mat("FFF8FC",.26),(cx,cy),iris_paint,front=lambda x,y:surface(head,x,y,-23)-.8,rings=38)
    shader=next(n for n in o.active_material.node_tree.nodes if n.type=="BSDF_PRINCIPLED")
    shader.inputs["Specular IOR Level"].default_value=.12
    o["blink_part"]=True
    l=stroke(name+" upper lash",head,lid,2.35,lash,[.12]+[1]*(len(lid)-2)+[.16],2.2)
    l["blink_part"]=True


def horn(root,tip,r,depth):
    bx,by=root;tx,ty=tip
    sides=48;rings=54;vs=[]
    for j in range(rings+1):
        t=j/rings
        rr=max(.16,r*(1-t)**.75)
        for i in range(sides):
            a=math.tau*i/sides
            ripple=1-.045*max(0,math.cos(3.3*math.tau*t-a))**6
            vs.append(world(bx+(tx-bx)*t+rr*ripple*math.cos(a),by+(ty-by)*t,
                            depth+rr*ripple*.84*math.sin(a)))
    fs=[(j*sides+i,j*sides+(i+1)%sides,(j+1)*sides+(i+1)%sides,(j+1)*sides+i)
        for j in range(rings) for i in range(sides)]
    fs.extend((tuple(reversed(range(sides))),tuple(range(rings*sides,(rings+1)*sides))))
    horn_obj=mesh("Tapering golden horn",vs,fs,mat("FFBE48",.32,.11))
    pts=[];rs=[]
    for j in range(95):
        t=.04+.92*j/94; a=3.3*math.tau*t; rad=r*(1-t)**.75
        pts.append((bx+(tx-bx)*t+rad*math.cos(a),by+(ty-by)*t,depth+rad*.84*math.sin(a)))
        rs.append(1-t*.80)
    line("Golden spiral ridge",pts,r*.105,"EBA535",rs)
    return horn_obj


def ear(name,anchors,inner,d,thick):
    o=pillow(name,anchors,d,thick,"FCE6ED")
    pillow(name+" velvet pink inside",inner,d-1,thick*.25,"F6A3BD",
           front=lambda x,y:surface(o,x,y)-.32)
    return o


def mark_skin(obj):
    obj["coat_palette"]=True
    return obj

