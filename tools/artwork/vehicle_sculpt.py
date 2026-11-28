"""Solid modelling in the orthographic camera coordinates of the 1254px vehicle references.

Coordinates describe editable contours and the depth of real mesh surfaces. Wheel revolutions,
rolled edges, glazing and fittings are modelled in 3D; the images are not sampled or imported.
"""
import math
import bpy
import numpy as np
from mathutils import Matrix, Vector
from mathutils.geometry import delaunay_2d_cdt
from mathutils.bvhtree import BVHTree
from transformation_studio import ball, paint, setup, tube, save_render
from render_car_layers import mesh_form

UNIT = 200
WHITE="FFF5DF"
RUBBER="372E38"
STEEL="C5BFCA"


def pos(x,y,d):
    return ((x-627)/UNIT, -d/UNIT, (1020-y)/UNIT)


def begin(keep_body=False):
    scene=setup("car",(0,0,(1020-627)/UNIT),(0,-1,0),1254/UNIT,keep_body=True)
    if not keep_body:
        for obj in list(scene.objects):
            if obj.type not in {"LIGHT","CAMERA"} and obj.name not in {"Car eye left","Car eye right"}:
                bpy.data.objects.remove(obj, do_unlink=True)
    for obj in list(scene.objects):
        if obj.type=="LIGHT":
            bpy.data.objects.remove(obj,do_unlink=True)
    for name,location,power,size in [
        ("Upper left softbox",(-4,-5,8),540,4.5),("Front fill",(4,-5,4),290,4.0),
        ("Roof reflection",(.5,1.5,8),490,3.0),("Left rim",(-4,1,4),230,3.0)
    ]:
        bpy.ops.object.light_add(type="AREA",location=location)
        obj=bpy.context.object
        obj.name=name
        obj.rotation_euler=(Vector((0,0,2))-obj.location).to_track_quat("-Z","Y").to_euler()
        obj.data.energy, obj.data.size=power,size
    scene.view_settings.view_transform="Standard"
    scene.view_settings.look="None"
    scene.view_settings.exposure=-.48
    scene.cycles.samples=24
    scene.world.node_tree.nodes.get("Background").inputs["Strength"].default_value=.45
    # Render a high resolution master before resampling the assembled artwork to Android sizes.
    scene.render.resolution_x=scene.render.resolution_y=720
    if not keep_body:
        scene.render.resolution_x=scene.render.resolution_y=840
        scene.render.threads=10
        scene.cycles.samples=36
        scene.view_settings.exposure=-.41
        scene.objects["Front fill"].data.energy=295
        bpy.ops.object.light_add(type="AREA",location=(.0,-5.0,1.9))
        fill=bpy.context.object
        fill.name="Low white card reflected in the wheels and lower paint"
        fill.data.energy=85
        fill.data.size=5.1
        fill.rotation_euler=(Vector((0,0,1.5))-fill.location).to_track_quat("-Z","Y").to_euler()
        scene.objects["Roof reflection"].data.energy=360
        scene.world.node_tree.nodes.get("Background").inputs["Strength"].default_value=.33


def pigment(color, roughness=.29, metal=0):
    return paint(color,roughness,metal) if isinstance(color,str) else color


def curve_points(knots):
    points=np.asarray(knots,float)
    out=[]
    for i,p in enumerate(points):
        a=points[i-1]; b=points[(i+1)%len(points)]; c=points[(i+2)%len(points)]
        # Length-weighted tangents keep tight moulded corners beside long straight panels.
        distance=np.linalg.norm(b-p)
        m0=(b-a)*(distance/max(1,np.linalg.norm(b-p)+np.linalg.norm(p-a)))
        m1=(c-p)*(distance/max(1,np.linalg.norm(c-b)+distance))
        for t in np.linspace(0,1,max(3,int(distance/10)),endpoint=False):
            out.append((2*t**3-3*t*t+1)*p+(t**3-2*t*t+t)*m0+(-2*t**3+3*t*t)*b+(t**3-t*t)*m1)
    return np.array(out)


class Surface:
    def __init__(self, outline, base, rise, roll):
        self.bvh=None
        self.outline=curve_points(outline)
        self.base=base
        self.rise=rise
        self.roll=roll
        self.start=self.outline
        self.edge=np.roll(self.outline,-1,axis=0)-self.outline
        self.squared=(self.edge**2).sum(axis=1).clip(1e-9)

    def depth(self,x,y):
        if self.bvh is not None:
            hit,_,_,_=self.bvh.ray_cast(Vector(pos(x,y,2000)),Vector((0,1,0)))
            if hit is not None:
                return -hit.y*UNIT
        delta=np.array([x,y])-self.start
        t=np.clip((delta*self.edge).sum(axis=1)/self.squared,0,1)
        distance=np.sqrt(((delta-self.edge*t[:,None])**2).sum(axis=1).min())
        return self.level(x,y)+self.rise*math.sqrt(max(0,1-math.exp(-distance/self.roll)))

    def level(self,x,y):
        return self.base(x,y) if callable(self.base) else self.base


def solid(name, outline, base, color, rise=16, roll=18, thickness=34, coat=False):
    """Capped and backed freeform casting with its own rolled rim and convex front skin."""
    surface=Surface(outline,base,rise,roll)
    border=surface.outline
    lo,hi=border.min(axis=0),border.max(axis=0)
    step=max(3,min(8,min(hi-lo)/8))
    points=list(border)
    for y in np.arange(lo[1]+step*.52,hi[1],step):
        edges=surface.edge
        crossings=(border[:,1]>y)!=((border[:,1]+edges[:,1])>y)
        bx=border[crossings]; be=edges[crossings]
        xs=sorted(bx[:,0]+(y-bx[:,1])*be[:,0]/be[:,1])
        for left,right in zip(xs[::2],xs[1::2]):
            for x in np.arange(left+step*.38,right-step*.30,step):
                points.append((x,y))
    n=len(border)
    # CDT retains concave cut-outs such as wheel arches and gives the face real supporting topology.
    verts,_,triangles,_,_,_=delaunay_2d_cdt([Vector(v) for v in points],[],[list(range(n))],1,.00001)
    coordinates=[pos(p.x,p.y,surface.depth(p.x,p.y)) for p in verts]
    lookup={(round(p.x,3),round(p.y,3)):i for i,p in enumerate(verts)}
    rim=[lookup.get((round(x,3),round(y,3))) for x,y in border]
    for i,v in enumerate(rim):
        if v is None:
            rim[i]=min(range(len(verts)), key=lambda k:(verts[k]-Vector(border[i])).length_squared)
    surface.bvh=BVHTree.FromPolygons(coordinates,triangles,all_triangles=True)
    faces=list(triangles)
    rear=len(coordinates)
    coordinates.extend(pos(x,y,surface.level(x,y)-thickness) for x,y in border)
    faces.append(tuple(rear+i for i in reversed(range(n))))
    for i in range(n):
        j=(i+1)%n
        faces.append((rim[i],rim[j],rear+j,rear+i))
    obj=mesh_form(name,coordinates,faces,pigment(color),"vehicle")
    for edge in obj.data.edges:
        a,b=edge.vertices
        if a<rear<=b or b<rear<=a:
            edge.use_edge_sharp=True
    obj["coat_palette"]=coat
    return surface


def oval(name,x,y,rx,ry,d,bulge,color,blink=False,coat=False):
    obj=ball(name,pos(x,y,d),(rx/UNIT,bulge/UNIT,ry/UNIT),pigment(color))
    obj["blink_part"]=blink
    obj["coat_palette"]=coat
    return obj


def stroke(name, points, radius, color, radii=None, cyclic=False, coat=False):
    obj=tube(name,[pos(*p) for p in points],radius/UNIT,pigment(color),radii,cyclic)
    obj["coat_palette"]=coat
    return obj


def ellipse(name,x,y,rx,ry,d,r,color):
    return stroke(name,[(x+rx*math.cos(i*math.tau/20),y+ry*math.sin(i*math.tau/20),d)
                       for i in range(20)],r,color,cyclic=True)


def fit_car_eyes(surface, near, far):
    """Mount the two original game lenses on the bonnet, retaining the native aspect and UVs."""
    bpy.context.view_layer.update()
    deps = bpy.context.evaluated_depsgraph_get()
    for name, (x, y, width) in zip(("Car eye left", "Car eye right"), (near, far)):
        lens = bpy.context.scene.objects[name]
        evaluated = lens.evaluated_get(deps)
        mesh = bpy.data.meshes.new_from_object(evaluated, preserve_all_data_layers=True, depsgraph=deps)
        positions = [evaluated.matrix_world @ v.co for v in mesh.vertices]
        left, right = min(p.x for p in positions), max(p.x for p in positions)
        mid_x = (left + right) / 2
        mid_z = (min(p.z for p in positions) + max(p.z for p in positions)) / 2
        back = max(p.y for p in positions)
        scale = width / (right - left)
        for vertex, point in zip(mesh.vertices, positions):
            px = x + (point.x - mid_x) * scale
            py = y - (point.z - mid_z) * scale
            vertex.co = pos(px, py, surface.depth(px, py) + 3 + (back - point.y) * scale)
        lens.modifiers.clear()
        lens.data = mesh
        lens.matrix_world = Matrix.Identity(4)
        lens["blink_part"] = True
        lens["base_eye"] = True
        for slot in lens.material_slots:
            emission = slot.material.node_tree.nodes.get("Emission")
            if emission and not slot.material.get("lens_exposure_compensated"):
                emission.inputs["Strength"].default_value *= 2 ** -bpy.context.scene.view_settings.exposure
                slot.material["lens_exposure_compensated"] = True


def smile(left,mid,right,d,r=4):
    stroke("Upturned smile",[(left[0],left[1],d),(mid[0],mid[1],d+1),(right[0],right[1],d)],
           r,"30232A",[.68,1,.68])


def wheel(name,x,y,rx,ry,offset,depth,hub="EEE0CE",monster=False,far=False):
    """Turned rubber sidewall, recessed hub and optional continuous moulded chevron tread."""
    vertices=[]; faces=[]; steps=112 if monster else 72
    profile=[(0,.02),(0,.49),(.016,.61),(-.018,.74),(.017,.88),(.08,.96)]
    profile += [(t,1) for t in np.linspace(.15,.84,22 if monster else 7)]
    profile += [(.92,.965),(.98,.89),(1,.71),(1.01,.03)]
    slope=math.sqrt(max(0,ry*ry-rx*rx))/rx
    axle_length=max(36,offset/max(.3,slope))
    for t,r in profile:
        for j in range(steps):
            a=math.tau*j/steps
            amplitude=min(1,max(0,(t-.03)/.15),max(0,(.99-t)/.16)) if monster else 0
            ridge=max(0,math.cos(10*a+abs(t-.50)*9.5))**.85
            radius=r*(1-.068*amplitude+.10*amplitude*ridge)
            vertices.append(pos(x+offset*t+rx*radius*math.cos(a),
                                y-8*t+ry*radius*math.sin(a),depth-t*axle_length+slope*rx*radius*math.cos(a)))
    for k in range(len(profile)-1):
        for j in range(steps):
            a=k*steps+j; b=k*steps+(j+1)%steps
            faces.append((a,b,b+steps,a+steps))
    faces.append(tuple(reversed(range(steps))))
    faces.append(tuple(range(len(vertices)-steps,len(vertices))))
    mesh_form(name,vertices,faces,pigment("302B34" if not far else "27232C",.48),"tyre")
    if far: return
    d=depth+axle_length*.063
    hx,hy=rx*(.55 if not monster else .52),ry*(.57 if not monster else .53)
    pierced_hub(x,y,hx,hy,d,hub,slope)


def pierced_hub(x,y,hx,hy,d,color,slope):
    before=set(bpy.context.scene.objects)
    outer=[(math.cos(a),math.sin(a)) for a in np.linspace(0,math.tau,64,endpoint=False)]
    contours=[outer]; holes=[]
    for i in range(5):
        a=math.tau*i/5+math.pi*.30
        loop=[]
        for t in np.linspace(0,math.tau,24,endpoint=False):
            u=.59+.25*math.cos(t)
            v=.24*math.sin(t)*(.82+.24*math.cos(t))
            loop.append((u*math.cos(a)-v*math.sin(a),u*math.sin(a)+v*math.cos(a)))
        contours.append(loop); holes.append((math.cos(a),math.sin(a)))
    coordinates=[]; edges=[]
    for loop in contours:
        start=len(coordinates); coordinates.extend(Vector(co) for co in loop)
        edges.extend((start+j,start+(j+1)%len(loop)) for j in range(len(loop)))
    verts,_,tris,_,_,_=delaunay_2d_cdt(coordinates,edges,[list(range(len(outer)))],1,.000001)
    def in_hole(p):
        for cx,cy in holes:
            u=p.x*cx+p.y*cy; v=-p.x*cy+p.y*cx
            if ((u-.59)/.25)**2+(v/(.24*(.82+.24*(u-.59)/.25)))**2 < .985:
                return True
        return False
    verts3=[pos(x+p.x*hx,y+p.y*hy,d+6+9*(1-p.length_squared)*s) for s in [1,-.5] for p in verts]
    n=len(verts)
    triangles=[tri for tri in tris if not in_hole(sum((verts[j] for j in tri),Vector((0,0)))/len(tri))]
    faces=list(triangles)+[tuple(n+j for j in reversed(tri)) for tri in triangles]
    boundary={}
    for tri in triangles:
        for i,j in zip(tri,tri[1:]+tri[:1]):
            pair=tuple(sorted((i,j)))
            boundary[pair]=boundary.get(pair,0)+1
    faces += [(i,j,j+n,i+n) for (i,j),count in boundary.items() if count==1]
    oval("Inside of wheel rim",x,y,hx*.99,hy*.99,d-1,3,"514447")
    mesh_form("Five-spoke cast wheel",verts3,faces,pigment(color,.28,.17),"wheel")
    ellipse("Rounded rim lip",x,y,hx*.96,hy*.96,d+8,2.2,color)
    oval("Domed axle cap",x,y,hx*.26,hy*.28,d+15,4.5,color)
    bpy.context.view_layer.update()
    for obj in set(bpy.context.scene.objects)-before:
        if obj.type=="CURVE":
            for bezier in obj.data.splines[0].bezier_points:
                co=bezier.co.copy()
                bezier.co.y-=slope*(co.x*UNIT+627-x)/UNIT
            continue
        for vertex in obj.data.vertices:
            world=obj.matrix_world @ vertex.co
            vertex.co.y-=slope*(world.x*UNIT+627-x)/(UNIT*obj.scale.y)




def glazing(name,outline,base):
    """Dark rubber gasket and a softly convex blue-violet windshield."""
    points=np.array(outline); centre=points.mean(axis=0)
    solid(name+" rubber seal",outline,base,"453F51",5,6,9)
    inside=centre+(points-centre)*(.976,.959)
    glass=pigment("495B88",.31,.16)
    shader=next(n for n in glass.node_tree.nodes if n.type=="BSDF_PRINCIPLED")
    if not shader.inputs["Base Color"].is_linked:
        nodes,links=glass.node_tree.nodes,glass.node_tree.links
        coord=nodes.new("ShaderNodeTexCoord")
        distance=nodes.new("ShaderNodeVectorMath")
        distance.operation="DISTANCE"
        distance.inputs[1].default_value=(.19,.32,.84)
        gradient=nodes.new("ShaderNodeValToRGB")
        gradient.color_ramp.elements[0].color=(.36,.42,.64,1)
        gradient.color_ramp.elements.new(.48).color=(.13,.16,.30,1)
        gradient.color_ramp.elements[2].color=(.065,.082,.16,1)
        links.new(coord.outputs["Generated"],distance.inputs[0])
        links.new(distance.outputs["Value"],gradient.inputs[0])
        links.new(gradient.outputs[0],shader.inputs["Base Color"])
    return solid(name,inside,lambda x,y:base(x,y)+6 if callable(base) else base+6,
                 glass,14,23,7)


def finish(item):
    save_render(item)

