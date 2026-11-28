"""Editable toy volumes from silhouette, contact and face landmarks on the concept sheets.

Artwork coordinates are expressed in pixels on the 1254-square layout. Depth is
independent of the layout; every principal form has a closed front and back.
"""
import math
import bpy
from mathutils import Vector
from mathutils.geometry import interpolate_bezier
from transformation_studio import ball, box, paint, setup, tube
from render_car_layers import rgba
from transformation_pets import mesh, pose, grain

UNIT=180
TAU=math.tau
FUR={"dog":"E39652","cat":"F3A54C","bunny":"F3CBA5"}
DARK="483028"
CREAM="FFE4B9"
EAR="94522F"


def point(x,y,d=0):
    return Vector(((x-626)/UNIT,d/UNIT,(1110-y)/UNIT))


def start(kind,cx,cy,size,keep_body=False):
    scene=setup(kind,point(cx,cy),(.0,-1,.0),size/UNIT,keep_body)
    scene.render.resolution_x=scene.render.resolution_y=800
    scene.render.threads=10
    scene.cycles.samples=38
    scene.view_settings.exposure=-.31
    scene.view_settings.view_transform="Standard"
    scene.view_settings.look="None"
    background=next(n for n in scene.world.node_tree.nodes if n.type=="BACKGROUND")
    background.inputs["Strength"].default_value=.26
    target=point(cx,cy)
    for lamp in list(scene.objects):
        if lamp.type=="LIGHT":
            bpy.data.objects.remove(lamp,do_unlink=True)
    for name,offset,power,diameter in [
        ("Warm left softbox",(-3.9,-5.1,5.3),580,4.6),
        ("Right portrait fill",(4.0,-5,1.6),245,5.4),
        ("Edge light",(-2.7,2.4,4.6),145,4.4),
        ("Low bounce",(.1,-4.1,-1.9),56,4.2),
    ]:
        lamp=bpy.data.lights.new(name,"AREA")
        lamp.shape="DISK";lamp.size=diameter;lamp.energy=power
        obj=bpy.data.objects.new(name,lamp);scene.collection.objects.link(obj)
        obj.location=target+Vector(offset)
        obj.rotation_euler=(target-obj.location).to_track_quat("-Z","Y").to_euler()
    bpy.context.view_layer.active_layer_collection=bpy.context.view_layer.layer_collection
    mat=bpy.data.materials.get(kind.capitalize()+" fur")
    if mat is None:
        mat=paint(FUR[kind]).copy()
        mat.name=kind.capitalize()+" fur"
    shader=next(n for n in mat.node_tree.nodes if n.type=="BSDF_PRINCIPLED")
    shader.inputs["Base Color"].default_value=rgba(FUR[kind])
    shader.inputs["Roughness"].default_value=.43
    grain(mat,170,.006,.43)
    return mat


def oval(x,y,rx,ry,angle=0,count=36):
    a=math.radians(angle)
    return [(x+rx*math.cos(t)*math.cos(a)-ry*math.sin(t)*math.sin(a),
             y+rx*math.cos(t)*math.sin(a)+ry*math.sin(t)*math.cos(a))
            for t in [TAU*i/count for i in range(count)]]


def smooth(points,steps=7,closed=True):
    curve=bpy.data.curves.new("Design silhouette construction","CURVE")
    s=curve.splines.new("BEZIER")
    s.bezier_points.add(len(points)-1)
    for p,co in zip(s.bezier_points,points):
        p.co=(*co,0) if len(co)==2 else co
        p.handle_left_type=p.handle_right_type="AUTO"
    s.use_cyclic_u=closed
    result=[]
    for i in range(len(points) if closed else len(points)-1):
        p=s.bezier_points[i]; q=s.bezier_points[(i+1)%len(points)]
        result.extend(interpolate_bezier(p.co,p.handle_right,q.handle_left,q.co,steps+1)[:-1])
    if not closed:
        result.append(s.bezier_points[-1].co.copy())
    bpy.data.curves.remove(curve)
    return result


def cushion(name,outline,d,front,back,color,center=None,steps=7):
    outline=smooth(outline,steps)
    n=len(outline)
    if center is None:
        center=(sum(p.x for p in outline)/n,sum(p.y for p in outline)/n)
    cx,cy=center
    rings=22
    vs=[point(cx,cy,d-front)]
    fs=[]
    for j in range(1,rings*2):
        a=math.pi*j/(2*rings); t=math.sin(a)
        depth=d-math.cos(a)*(front if j<=rings else back)
        for p in outline:
            vs.append(point(cx+(p.x-cx)*t,cy+(p.y-cy)*t,depth))
    last=len(vs)
    vs.append(point(cx,cy,d+back))
    fs += [(0,1+i,1+(i+1)%n) for i in range(n)]
    for j in range(2*rings-2):
        for i in range(n):
            u=1+j*n+i; v=1+j*n+(i+1)%n
            fs.append((u,u+n,v+n,v))
    fs += [(last,last-n+(i+1)%n,last-n+i) for i in range(n)]
    obj=mesh(name,vs,fs,color,True)
    # Curves can be entered clockwise or counterclockwise on the layout.
    if obj.data.polygons[0].normal.y>0:
        obj.data.flip_normals()
    return obj


def depth_at(obj,x,y,default=None):
    hit,p,_,_=obj.ray_cast(point(x,y,-1700),Vector((0,1,0)))
    return p.y*UNIT if hit else default


def patch(name,target,outline,color,bulge=1,edge=.7,center=None,mark=False,straight=False,fade=None):
    """Curve a marking or inset over the actual volume beneath it."""
    if straight:
        line=[Vector((p[0]+(q[0]-p[0])*k/8,p[1]+(q[1]-p[1])*k/8,0))
              for p,q in zip(outline,outline[1:]+outline[:1]) for k in range(8)]
    else:line=smooth(outline,4)
    n=len(line); rings=26 if fade else 16
    cx,cy=center or (sum(v.x for v in line)/n,sum(v.y for v in line)/n)
    base=depth_at(target,cx,cy,-90)
    vs=[point(cx,cy,base-edge-bulge)]
    fs=[(0,1+(i+1)%n,1+i) for i in range(n)]
    for j in range(1,rings+1):
        t=j/rings
        for p in line:
            x,y=cx+(p.x-cx)*t,cy+(p.y-cy)*t
            d=depth_at(target,x,y)
            if d is None:
                lo,hi=0.0,1.0
                for _ in range(10):
                    mid=(lo+hi)/2
                    if depth_at(target,cx+(x-cx)*mid,cy+(y-cy)*mid) is None:hi=mid
                    else:lo=mid
                x,y=cx+(x-cx)*lo,cy+(y-cy)*lo
                d=depth_at(target,x,y)
            vs.append(point(x,y,(base if d is None else d)-edge-bulge*math.sqrt(max(0,1-t*t))))
    for j in range(rings-1):
        for i in range(n):
            a=1+j*n+i;b=1+j*n+(i+1)%n
            fs.append((a,b,b+n,a+n))
    obj=mesh(name,vs,fs,color,True)
    if obj.data.polygons[0].normal.y>0: obj.data.flip_normals()
    if mark: obj["coat_palette"]=True
    if fade:
        mat=obj.data.materials[0].copy()
        mat.name=name+" softly blended pigment"
        obj.data.materials[0]=mat
        at=obj.data.attributes.new("Inlay coverage","FLOAT","POINT")
        weights=[1.0]+[min(1,(1-j/rings)*5) for j in range(1,rings+1) for i in range(n)]
        at.data.foreach_set("value",weights)
        nodes=mat.node_tree.nodes
        a=nodes.new("ShaderNodeAttribute");a.attribute_name=at.name
        mix=nodes.new("ShaderNodeMixRGB")
        mix.inputs[1].default_value=rgba(fade);mix.inputs[2].default_value=rgba(color)
        mat.node_tree.links.new(a.outputs["Fac"],mix.inputs[0])
        mat.node_tree.links.new(mix.outputs[0],next(n for n in nodes if n.type=="BSDF_PRINCIPLED").inputs["Base Color"])
    return obj


def sliced(name,outline,d,front,back,color,axis=1):
    """Round each section of a bent form across its width, retaining the true outside contour."""
    border=smooth(outline,7);op=1-axis
    low=min(p[axis] for p in border);high=max(p[axis] for p in border)
    max_width=(max(p[op] for p in border)-min(p[op] for p in border))/2
    vs=[];fs=[];count=88;seg=40
    for i in range(count+1):
        t=(i+.002)/(count+.004);val=low+(high-low)*t
        crosses=[a[op]+(b[op]-a[op])*(val-a[axis])/(b[axis]-a[axis])
                 for a,b in zip(border,border[1:]+border[:1])
                 if min(a[axis],b[axis])<=val<max(a[axis],b[axis])]
        x0,x1=min(crosses),max(crosses)
        c=(x0+x1)/2;w=(x1-x0)/2
        for j in range(seg):
            a=TAU*j/seg
            p=c+w*math.cos(a);v=math.sin(a)
            thick=min(1,w/(max_width*.35))
            dep=d+v*(front if v<0 else back)*thick
            vs.append(point(val,p,dep) if axis==0 else point(p,val,dep))
    for i in range(count):
        fs.extend((i*seg+j,i*seg+(j+1)%seg,(i+1)*seg+(j+1)%seg,(i+1)*seg+j) for j in range(seg))
    fs.extend((tuple(reversed(range(seg))),tuple(range(count*seg,(count+1)*seg))))
    ob=mesh(name,vs,fs,color,True)
    if ob.data.polygons[count//2*seg+3*seg//4].normal.y>0:ob.data.flip_normals()
    return ob


def line(name,coords,r,color,radii=None,cyclic=False):
    return tube(name,[point(*p) for p in coords],r/UNIT,color,radii,cyclic)


def surface_line(name,target,coords,r,color,radii=None,lift=2):
    return line(name,[(x,y,depth_at(target,x,y,-100)-lift) for x,y in coords],r,color,radii)


def ellipsoid(name,x,y,d,rx,ry,rd,color,angle=0):
    obj=ball(name,point(x,y,d),(rx/UNIT,rd/UNIT,ry/UNIT),color)
    obj.rotation_euler.y=math.radians(angle)
    return obj


def swept(name,samples,color,section=36,inner=None):
    """A bent, rounded, closed limb. Samples specify x, y, depth, width and thickness."""
    path=smooth([p[:3] for p in samples],8,False)
    sizes=smooth([p[3:] for p in samples],8,False)
    vs=[];fs=[]
    for i,(v,sz) in enumerate(zip(path,sizes)):
        u=(path[min(i+1,len(path)-1)]-path[max(i-1,0)]).normalized()
        length=math.hypot(u.x,u.y) or 1
        nx,ny=-u.y/length,u.x/length
        for j in range(section):
            a=TAU*j/section
            vs.append(point(v.x+nx*max(.3,sz.x)*math.cos(a),v.y+ny*max(.3,sz.x)*math.cos(a),
                            v.z+max(.3,sz.y)*math.sin(a)))
    for i in range(len(path)-1):
        for j in range(section):
            fs.append((i*section+j,i*section+(j+1)%section,(i+1)*section+(j+1)%section,(i+1)*section+j))
    fs.extend((tuple(reversed(range(section))),tuple(range(len(vs)-section,len(vs)))))
    ob=mesh(name,vs,fs,color,True)
    middle=(len(path)//2*section+section*3//4)
    if ob.data.polygons[middle].normal.y>0: ob.data.flip_normals()
    if inner:
        second,color_region=inner
        ob.data.materials.append(paint(second) if isinstance(second,str) else second)
        for i,f in enumerate(ob.data.polygons[:-2]):
            h,j=divmod(i,section)
            if color_region(h/(len(path)-1),TAU*(j+.5)/section):f.material_index=1
    return ob


def eye(name,target,cx,cy,rx,ry,dx=0,dy=0,family="dog",sleep=False):
    old=set(bpy.context.scene.objects)
    base=patch(name+" fine outer lid",target,oval(cx,cy,rx,ry),"422D30",7)
    white=patch(name+" warm sclera",base,oval(cx,cy+2,rx-3.0,ry-4.0),"FFF4EB",8)
    down=dy>ry*.25
    irx,iry=(rx*.62,ry*.54) if down else (rx*.79,ry*.87)
    ix,iy=cx+dx,cy+dy
    substrate=patch(name+" warm iris",white,oval(ix,iy,irx,iry),
                    "765439" if family!="bunny" else "654958",6,1)
    patch(name+" deep pupil",substrate,oval(ix,iy-iry*.085,irx*.79,iry*.81),"271E23",4,1)
    patch(name+" catchlight",substrate,oval(ix-irx*.29,iy-iry*.44,irx*.23,iry*.21),"FFFDFC",3,10)
    for part in (base,white,substrate):
        shader=next(n for n in part.active_material.node_tree.nodes if n.type=="BSDF_PRINCIPLED")
        shader.inputs["Specular IOR Level"].default_value=.13
    for obj in set(bpy.context.scene.objects)-old:
        if "deep pupil" in obj.name:
            shader=next(n for n in obj.active_material.node_tree.nodes if n.type=="BSDF_PRINCIPLED")
            shader.inputs["Specular IOR Level"].default_value=.10
    if sleep:
        lidoutline=[(cx-rx-1,cy+ry*.07),(cx-rx*.90,cy-ry*.63),(cx-rx*.43,cy-ry*.99),
                    (cx+rx*.20,cy-ry),(cx+rx*.86,cy-ry*.65),(cx+rx,cy-ry*.13),
                    (cx+rx*.68,cy-ry*.07),(cx,cy+ry*.03),(cx-rx*.73,cy+ry*.15)]
        lid=cushion(name+" half closed lid",lidoutline,depth_at(target,cx,cy)-49,9,9,
                    bpy.data.materials["Bunny fur"])
        lid["coat_palette"]=True
        surface_line(name+" sleepy lash",lid,[(cx-rx*.91,cy+ry*.07),(cx-rx*.37,cy+ry*.055),
                     (cx+rx*.27,cy-ry*.04),(cx+rx*.94,cy-ry*.13)],4.4,"513629",[.40,1,1,.45],5)
    for o in set(bpy.context.scene.objects)-old:
        if not o.get("coat_palette") and "sleepy lash" not in o.name: o["blink_part"]=True


def face(kind,fur,outline,earpaths,eyes,brows,muzzle,nose,smile,cheeks=None,eye_patch=None,sleep=False):
    """Build one expression from the unique landmarks of its own illustration."""
    head=cushion(kind.title()+" face",outline,0,159,135,fur)
    head["coat_palette"]=True
    for i,ear in enumerate(earpaths):
        pos,depth,bulge=ear
        e=cushion(kind.title()+" folded ear",pos,depth,bulge,bulge*.78,EAR if kind=="dog" else fur)
        e["coat_palette"]=True
    if eye_patch:
        patch("Chocolate eye patch",head,eye_patch,EAR,1,.7,mark=True)
    if cheeks:
        for x,y,rx,ry in cheeks:
            patch("Soft blush",head,oval(x,y,rx,ry),"F4AE97" if kind=="bunny" else "EE9B6C",.5,fade=FUR[kind])
    for e in eyes:
        eye(kind.title()+" eye",head,*e,family=kind,sleep=sleep)
    for br in brows:
        surface_line("Expressive brow",head,br,6 if kind=="bunny" else 8,
                     "BE875E" if kind=="bunny" else "764327",[.2,1,.23],2.5)
    x,y,rx,ry=muzzle
    muzzle_obj=patch("Cream muzzle on curved cheeks",head,oval(x,y,rx,ry),
                     "FFE9CA" if kind=="bunny" else CREAM,19,1,fade=FUR[kind])
    nx,ny,nrx,nry=nose
    nose_outline=[(nx-nrx,ny-nry*.30),(nx-nrx*.77,ny-nry*.74),(nx,ny-nry*.91),
                  (nx+nrx*.79,ny-nry*.75),(nx+nrx,ny-nry*.30),(nx+nrx*.61,ny+nry*.40),
                  (nx+nrx*.10,ny+nry),(nx-nrx*.36,ny+nry*.74)]
    patch("Soft triangular nose",muzzle_obj,nose_outline,
          "E98586" if kind=="bunny" else "603629",11,5)
    sx,sy,sw,sh=smile
    if kind=="bunny":
        surface_line("Fine philtrum above the smile",muzzle_obj,[(nx,ny+nry*.48),((nx+sx)/2,(ny+nry+sy)/2),(sx,sy)],
                     2.0,"C59376",[.45,.8,.4],2)
        mouth=patch("Open bunny smile",muzzle_obj,[(sx-sw,sy-sh*.32),(sx-sw*.65,sy+sh*.04),
             (sx-sw*.30,sy+sh*.09),(sx,sy-sh*.07),(sx+sw*.30,sy+sh*.09),
             (sx+sw*.64,sy+sh*.02),(sx+sw,sy-sh*.32),(sx+sw*.64,sy+sh*.72),
             (sx,sy+sh),(sx-sw*.64,sy+sh*.73)],"58262A",1.3,2.0)
        patch("Pink tongue inside smile",mouth,oval(sx,sy+sh*.73,sw*.46,sh*.22),"E88787",2.0,1)
        for s in (-1,1):
            patch("Bunny front tooth",mouth,[(sx+s*1,sy),(sx+s*sw*.36,sy),
                  (sx+s*sw*.34,sy+sh*.42),(sx+s*sw*.25,sy+sh*.50),
                  (sx+s*sw*.07,sy+sh*.51),(sx+s*1,sy+sh*.43)],"FFF4E0",2,2,straight=True)
    else:
        surface_line("Muzzle philtrum",muzzle_obj,[(nx,ny+nry*.72),((nx+sx)/2,(ny+nry+sy)/2),(sx,sy)],
                     3.6,"6C3C29",[1,1,.9],4)
        for s in (-1,1):
            surface_line("Smiling muzzle crease",muzzle_obj,[(sx,sy),(sx+s*sw*.28,sy+sh*.31),
                         (sx+s*sw*.74,sy+sh*.21),(sx+s*sw,sy-sh*.33)],4,"643924",[.8,1,1,.35],4)
    return head,muzzle_obj


def toes(obj,positions,shade):
    for x,y,dx,dy in positions:
        surface_line("Soft toe separation",obj,[(x,y),(x+dx*.67,y+dy*.50),(x+dx,y+dy)],
                     2.3,shade,[.12,.68,.16],1.2)


def tagged(obj):
    obj["coat_palette"]=True
    return obj


def turned(name,profile,color,n=80):
    """Closed turned shell, with perspective and off-centre rings from its front elevation.

    Each ring gives the centre x, y and depth, horizontal radius, depth radius,
    and downward projection of its near edge. Inner rings describe the lining.
    """
    vs=[]
    for x,y,d,rx,rd,tilt in profile:
        for j in range(n):
            a=TAU*j/n
            vs.append(point(x+rx*math.cos(a),y-tilt*math.sin(a),d+rd*math.sin(a)))
    fs=[(i*n+j,i*n+(j+1)%n,(i+1)%len(profile)*n+(j+1)%n,(i+1)%len(profile)*n+j)
        for i in range(len(profile)) for j in range(n)]
    return mesh(name,vs,fs,color,True)


def tail(name,samples,fur,tip=None):
    def tip_section(t,a):
        return t>.74+.019*math.sin(9*a)
    return tagged(swept(name,samples,fur,48,(tip,tip_section) if tip else None))
