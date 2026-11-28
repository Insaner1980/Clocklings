"""Volumetric toy sculpts in front-view reference coordinates (200 units per metre)."""

from math import cos, pi, sin, sqrt, tau
from pathlib import Path
import sys
import bpy
from mathutils import Vector
sys.path.insert(0,str(Path(__file__).resolve().parent))
from transformation_studio import setup, save_render, ball, rod, box, tube, finish
from render_car_layers import material


def at(x, y, depth=0):
    return Vector(((x-627)/200, depth/200, (1130-y)/200))


def pigment(name, color, rough=.33, metal=0, coat=0):
    m=bpy.data.materials.get(name)
    if m:return m
    m=material(name,color,rough)
    p=next(n for n in m.node_tree.nodes if n.type=="BSDF_PRINCIPLED")
    p.inputs["Metallic"].default_value=metal
    p.inputs["Coat Weight"].default_value=coat
    p.inputs["Coat Roughness"].default_value=.19
    return m


def studio(kind, frame):
    x,y,width=frame
    scene=setup(kind,at(x,y), (0,-1,0),width/200,keep_body=False)
    scene.cycles.samples=36
    scene.render.threads=3
    scene.view_settings.view_transform="Standard"
    scene.view_settings.exposure=-.18
    for light in [o for o in scene.objects if o.type=="LIGHT"]:
        bpy.data.objects.remove(light,do_unlink=True)
    for name,loc,power,size in [
        ("Left photographic softbox",(-3.8,-5.4,8.4),690,4.1),
        ("Cool front fill",(4.3,-5.8,5.4),390,5.5),
        ("Overhead satin highlight",(-.9,1.3,8.2),590,3.9),
        ("Portrait reflector",(.3,-5.5,1.6),64,3.7),
    ]:
        data=bpy.data.lights.new(name,"AREA");data.energy=power;data.shape="DISK";data.size=size
        obj=bpy.data.objects.new(name,data);scene.collection.objects.link(obj);obj.location=loc
        obj.rotation_euler=(Vector((0,0,2.7))-obj.location).to_track_quat("-Z","Y").to_euler()
    return scene


def oval(name,center,radii,mat):
    x,y,d=center;rx,ry,rd=radii
    return ball(name,at(x,y,d),(rx/200,rd/200,ry/200),mat)


def line(name,points,r,mat,radii=None,cyclic=False):
    return tube(name,[at(*p) for p in points],r/200,mat,radii,cyclic)


def axis(name,a,b,r,mat,r2=None):
    return rod(name,at(*a),at(*b),r/200,mat,None if r2 is None else r2/200)


def smooth_outline(outline,resolution=5):
    points=[Vector(p) for p in outline];n=len(points);result=[]
    for i in range(n):
        a,b,c,d=[points[k%n] for k in (i-1,i,i+1,i+2)]
        for j in range(resolution):
            t=j/resolution
            result.append((2*b+(-a+c)*t+(2*a-5*b+4*c-d)*t*t+(-a+3*b-3*c+d)*t**3)/2)
    return result


def mesh_object(name,verts,faces,mat,smooth=True):
    import bmesh
    me=bpy.data.meshes.new(name);me.from_pydata(verts,[],faces);me.update()
    bm=bmesh.new();bm.from_mesh(me);bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));bm.to_mesh(me);bm.free()
    o=bpy.data.objects.new(name,me);bpy.context.scene.collection.objects.link(o)
    finish(name,o,mat)
    if smooth:
        for f in me.polygons:f.use_smooth=True
    return o


def solid(name,outline,depth,thickness,mat,roundness=.78,center=None):
    border=smooth_outline(outline)
    c=Vector(center) if center else sum(border,Vector((0,0)))/len(border)
    n=len(border);steps=32
    def depth_at(x,y):return depth(x,y) if callable(depth) else depth
    points=[at(c.x,c.y,depth_at(*c)-thickness)]
    for j in range(1,steps):
        q=j*pi/steps;r=sin(q)**roundness
        for p in border:
            x,y=c+(p-c)*r
            points.append(at(x,y,depth_at(x,y)-thickness*cos(q)))
    end=len(points);points.append(at(c.x,c.y,depth_at(*c)+thickness))
    f=[(0,1+i,1+(i+1)%n) for i in range(n)]
    f.extend((1+j*n+i,1+j*n+(i+1)%n,1+(j+1)*n+(i+1)%n,1+(j+1)*n+i) for j in range(steps-2) for i in range(n))
    f.extend((end,1+(steps-2)*n+(i+1)%n,1+(steps-2)*n+i) for i in range(n))
    return mesh_object(name,points,f,mat)


def surface(obj,x,y):
    inv=obj.matrix_world.inverted()
    start=inv@at(x,y,-1900)
    direction=inv.to_3x3()@Vector((0,1,0))
    hit,loc,_,_=obj.ray_cast(start,direction)
    if not hit:
        raise ValueError(f"Missed {obj.name} at {x:.1f},{y:.1f}")
    return (obj.matrix_world@loc).y*200


def patch(name,outline,onto,mat,raise_by=3,bulge=3):
    # A closed, moulded inset follows the underlying solid at every sampled vertex.
    return solid(name,outline,lambda x,y:surface(onto,x,y)-raise_by,bulge,mat,.72)


def ellipse_points(cx,cy,rx,ry,count=16,angle=0):
    return [(cx+rx*cos(a*tau/count)*cos(angle)-ry*sin(a*tau/count)*sin(angle),
             cy+rx*cos(a*tau/count)*sin(angle)+ry*sin(a*tau/count)*cos(angle)) for a in range(count)]


def pill(name,a,b,r,mat,end_radius=None):
    r2=end_radius or r
    return line(name,[a,b],r,mat,[1,r2/r]),oval(name+" end A",a,(r,r,r),mat),oval(name+" end B",b,(r2,r2,r2),mat)


def seam(name,points,r,mat):
    return line(name,points,r,mat,[.32]+[1]*(len(points)-2)+[.32])


def tags(objects,key):
    for o in objects:o[key]=True


def since(objects):
    return set(bpy.context.scene.objects)-objects


def ring_mesh(name,rings,mat):
    count=len(rings[0]);v=[at(*p) for ring in rings for p in ring]
    f=[(j*count+i,j*count+(i+1)%count,(j+1)*count+(i+1)%count,(j+1)*count+i)
       for j in range(len(rings)-1) for i in range(count)]
    return mesh_object(name,v,f,mat)
