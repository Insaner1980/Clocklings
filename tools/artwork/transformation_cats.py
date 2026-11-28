"""The four kitten costumes, shaped in their own illustrated poses."""
import math,sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parent))
from pet_sculpt import *
from transformation_dogs import leg,puppy_paw
from transformation_studio import rod,save_render
from reference_finish import fuse


def kitten_tail(kind,fur):
    shapes={
      "sailor":([(391,750),(365,715),(339,679),(332,649),(340,630),(363,620),(387,605),(397,585),(394,557),(380,540),(353,527),(314,525),(282,540),(254,569),(236,605),(230,651),(235,698),(255,740),(306,761)],
                 [(248,586),(272,549),(308,529),(348,526),(376,537),(396,565),(391,594),(367,617),(340,631),(317,625),(293,608),(271,603)]),
      "detective":([(772,1069),(841,1051),(907,1008),(945,952),(973,875),(966,803),(946,754),(954,733),(988,724),(1016,703),(1030,677),(1025,651),(1009,633),(980,623),(944,627),(901,650),(867,683),(844,729),(833,783),(849,844),(849,897),(835,956),(790,1008)],
                    [(837,776),(843,736),(867,687),(909,649),(950,626),(990,626),(1016,641),(1031,668),(1022,698),(994,720),(964,731),(946,748),(945,769),(916,789),(878,795)]),
      "yarn":([(836,999),(888,1001),(916,995),(946,980),(971,960),(987,940),
               (999,920),(1007,900),(1013,875),(1015,849),(1013,819),(1009,791),
               (1009,771),(1013,758),(1021,748),(1035,740),(1054,730),(1065,717),
               (1072,698),(1071,681),(1065,668),(1055,656),(1040,647),(1018,642),
               (997,644),(975,650),(955,661),(938,677),(923,701),(914,726),
               (910,748),(909,773),(912,800),(916,827),(916,850),(913,864),
               (908,878),(899,893),(880,920),(853,946),(833,974)],
              [(910,747),(914,726),(923,701),(938,677),(955,661),(975,650),
               (997,644),(1018,642),(1040,647),(1055,656),(1065,668),(1071,681),
               (1072,698),(1065,717),(1054,730),(1035,740),(1021,748),(1013,758),
               (1009,769),(980,771),(949,765),(926,757)]),
      "boots":([(759,981),(821,952),(871,915),(903,857),(917,797),(917,733),(905,683),(909,661),(930,650),(955,641),(972,618),(971,594),(957,572),(929,558),(900,554),(865,559),(840,577),(820,605),(807,639),(801,680),(812,732),(823,788),(818,849),(797,918)],
               [(803,669),(808,633),(826,600),(851,573),(886,556),(919,554),(948,565),(968,590),(973,613),(959,638),(927,652),(910,665),(905,684),(868,688),(837,680)])}
    shape,tip=shapes[kind]
    ob=tagged(sliced("Continuous curled kitten tail",shape,72,55,56,fur))
    tip_fur=patch("Cream-colored fur at tail tip",ob,tip,CREAM,1,.8,fade=FUR["cat"])
    return ob,tip_fur


def blend_limbs(bib_outline=None):
    body=bpy.context.scene.objects.get("Kitten seated torso")
    if not body:return
    pieces=[o for o in bpy.context.scene.objects if o==body or (
        o.get("coat_palette") and any(s in o.name.lower() for s in
        ["hind leg","foreleg","grounded front leg","resting kitten foreleg","shoulder bridge"]))]
    bib=body["bib_landmarks"][:]
    if bib_outline:
        # Place the fur marking on the chest so the foreground leg can cover its edge.
        patch("Tapered cream chest behind the foreleg",body,bib_outline,CREAM,.6,1.6,fade=FUR["cat"])
    fused=fuse([o.name for o in pieces],"Continuous seated torso and articulated legs",voxel=.014 if bib_outline else .022)
    fused["coat_palette"]=True
    if bib_outline:
        return fused
    cx,cy,cw,ch=bib
    patch("Cream bib following the reposed chest",fused,[(cx,cy-ch),(cx+cw*.78,cy-ch*.68),(cx+cw,cy-ch*.14),
        (cx+cw*.65,cy+ch*.69),(cx,cy+ch),(cx-cw*.60,cy+ch*.71),(cx-cw,cy-ch*.13),(cx-cw*.78,cy-ch*.67)],
        CREAM,2,3.4,fade=FUR["cat"])


def kitten_head(fur,kind):
    if kind=="sailor":
        outline=[(641,250),(741,258),(819,308),(861,391),(881,463),(868,517),(823,563),(749,590),
                 (659,600),(561,587),(486,557),(446,504),(440,445),(460,363),(518,297)]
        ears=[[(456,367),(441,283),(449,226),(466,209),(510,224),(577,271),(551,318)],
              [(731,263),(780,211),(821,186),(840,199),(849,252),(836,343),(791,326)]]
        eyes=[(583,442,40,45,5,3),(767,427,38,44,-5,1)]
        brows=[[(551,361),(573,347),(596,352)],[(741,340),(761,333),(784,344)]]
        muzzle=(677,521,119,66);nose=(688,486,24,17);smile=(685,519,51,28)
        stripes=[(598,274,635,346),(637,260,676,345),(686,252,714,338)]
        whisk=[[(550,509),(500,498),(438,501)],[(551,526),(499,532),(455,545)],
               [(808,493),(856,476),(902,470)],[(809,511),(853,507),(897,514)]]
    elif kind=="detective":
        outline=[(596,194),(720,216),(813,280),(865,357),(901,453),(903,520),(865,596),
                 (788,646),(670,674),(575,679),(451,663),(363,629),(305,569),(285,494),(292,418),(343,309),(458,233)]
        ears=[[(326,334),(306,224),(315,131),(336,102),(385,111),(462,159),(477,206),(430,299)],
              [(724,217),(787,151),(862,113),(900,113),(918,143),(925,236),(892,329),(855,365)]]
        eyes=[(488,454,86,89,19,4),(754,456,55,63,-12,4)]
        brows=[[(451,287),(486,275),(519,282)],[(732,334),(760,320),(796,338)]]
        muzzle=(637,580,152,89);nose=(639,540,33,23);smile=(639,584,68,27)
        stripes=[(538,238,589,325),(600,224,651,327),(670,232,703,325)]
        whisk=[[(406,528),(331,515),(258,520)],[(404,555),(341,556),(262,578)],
               [(811,541),(873,522),(936,526)],[(812,568),(870,571),(927,591)]]
    elif kind=="yarn":
        outline=[(601,218),(735,239),(811,298),(850,385),(851,493),(815,562),(743,614),(638,640),
                 (525,629),(443,600),(386,549),(365,478),(374,374),(414,291),(500,239)]
        ears=[[(351,231),(341,142),(354,97),(373,87),(424,105),(486,142),(510,203),(441,255)],
              [(761,150),(822,111),(877,89),(901,103),(916,157),(904,232),(837,258)]]
        eyes=[(519,466,55,57,15,28),(727,490,52,52,-15,23)]
        brows=[[(488,358),(518,343),(551,356)],[(718,376),(744,366),(776,381)]]
        muzzle=(616,571,123,58);nose=(618,551,26,18);smile=(616,583,56,29)
        stripes=[(554,223,590,340),(607,220,640,344),(669,228,691,346)]
        whisk=[[(470,522),(416,491),(360,481)],[(469,543),(410,530),(362,533)],
               [(759,557),(808,539),(865,536)],[(760,577),(805,575),(851,587)]]
    else:
        outline=[(544,246),(661,252),(734,276),(787,339),(820,416),(827,485),(797,546),
                 (732,584),(652,606),(559,610),(475,597),(410,562),(374,508),(369,447),(407,354),(460,286)]
        ears=[[(398,309),(384,226),(392,171),(407,155),(449,167),(513,213),(528,252),(470,302)],
              [(677,295),(730,258),(788,241),(816,247),(829,268),(823,310),(794,381),(755,365)]]
        eyes=[(538,424,46,41,8,0),(726,406,34,40,-5,1)]
        brows=[[(490,359),(514,342),(546,344)],[(696,329),(718,322),(743,337)]]
        muzzle=(642,511,117,75);nose=(656,455,23,16);smile=(658,486,53,27)
        stripes=[(535,269,595,340),(589,253,631,337),(638,253,668,333)]
        whisk=[[(517,494),(459,488),(383,503)],[(518,513),(452,525),(398,552)],
               [(765,470),(810,455),(852,454)],[(769,488),(818,487),(857,498)]]
    for outline_ear in ears:
        ear=tagged(cushion("Rounded triangular kitten ear",outline_ear,27,45,37,fur))
        cx=sum(x for x,y in outline_ear)/len(outline_ear);cy=sum(y for x,y in outline_ear)/len(outline_ear)
        patch("Blush inner ear",ear,[(cx+(x-cx)*.69,cy+(y-cy)*.67) for x,y in outline_ear],"F59695",2,.9)
    head,_=face("cat",fur,outline,[],eyes,brows,muzzle,nose,smile)
    for x,y,ex,ey in stripes:
        patch("Tapered cinnamon tabby marking",head,[(x,y),(x+34,y-7),(ex+8,ey-28),
               (ex,ey),(ex-8,ey-2),(x+2,y+24)],"AC652F",.75,1.4,mark=True)
    for pts in whisk:
        line("Dark tapered whisker",[(x,y,depth_at(head,x,y,-100)-24 if i==0 else -158+i*6)
             for i,(x,y) in enumerate(pts)],3.2,"633923",[1.1,1,.58])


def detective_foreleg(fur):
    # Horizontal sections of one weight-bearing limb: height, left/right edges,
    # depth of the centre and half-thickness. The shoulder begins inside the chest.
    sections=[
        (639,670,704,32,10),(658,640,735,25,38),(685,630,750,17,52),
        (723,633,766,8,63),(760,646,780,-2,67),(799,663,787,-18,70),
        (828,663,784,-37,71),(869,645,777,-57,71),(921,625,761,-75,69),
        (977,610,746,-91,68),(1014,600,737,-108,64),(1031,596,736,-119,64),
        (1048,591,742,-127,71),(1069,585,750,-138,81),(1094,584,751,-144,86),
        (1114,588,748,-146,81),(1128,597,743,-146,67),(1135,605,736,-145,49),
        (1139,612,729,-145,29),(1140,618,722,-145,14),(1140.5,623,716,-145,4)]
    vertices=[];faces=[];n=112
    for y,left,right,depth,thickness in sections:
        split=min(1,max(0,(y-1054)/53))
        split=split*split*(3-2*split)
        for i in range(n):
            angle=TAU*i/n
            across=math.cos(angle); forward=max(0,-math.sin(angle))
            x=(left+right)/2+(right-left)*across/2
            # Two shallow furrows separate the three toe pads on the camera-facing surface.
            grooves=sum(math.exp(-((x-t)/6.1)**2)
                        for t in (647+(1115-y)*.055,705-(1115-y)*.04))
            toe_depth=depth+thickness*math.sin(angle)+13*split*grooves*forward
            sole=y-3*split*grooves*min(1,max(0,(y-1115)/19))
            vertices.append(point(x,sole,toe_depth))
    for j in range(len(sections)-1):
        for i in range(n):
            k=(i+1)%n
            faces.append((j*n+i,j*n+k,(j+1)*n+k,(j+1)*n+i))
    faces.extend([tuple(reversed(range(n))),tuple(range(len(vertices)-n,len(vertices)))])
    obj=tagged(mesh("Detective grounded front leg and three rounded toes",vertices,faces,fur,True))
    if obj.data.polygons[3*n//4].normal.y>0:obj.data.flip_normals()
    obj.modifiers.new("Soft transitions from shoulder to paw","SUBSURF").levels=2


def finish_detective_paw(body):
    # Carry the soft fur and the narrow shadow inside each sculpted toe cleft to the sole.
    material=body.data.materials[0].copy()
    material.name="Detective kitten fur with soft toe clefts"
    body.data.materials[0]=material
    shader=next(n for n in material.node_tree.nodes if n.type=="BSDF_PRINCIPLED")
    base=shader.inputs["Base Color"].default_value[:3]
    roughness=shader.inputs["Roughness"].default_value
    pigment=body.data.color_attributes.new(name="Forepaw fur",type="FLOAT_COLOR",domain="POINT")
    values=[]
    for v in body.data.vertices:
        p=body.matrix_world@v.co
        x,y,d=p.x*UNIT+626,1110-p.z*UNIT,p.y*UNIT
        matte=min(1,max(0,(-d-55)/75))*min(1,max(0,(x-548)/48))*min(1,max(0,(y-665)/115))
        shade=0
        if 1057<y and d < -142:
            strength=min(1,(y-1057)/43)*min(1,(-142-d)/24)
            for center in (647+(1115-y)*.055,705-(1115-y)*.04):
                shade+=strength*math.exp(-((x-center)/4.0)**2)
        values.extend((base[0]*(1-.32*shade),base[1]*(1-.43*shade),
                       base[2]*(1-.48*shade),roughness+(.73-roughness)*matte))
    pigment.data.foreach_set("color",values)
    color=material.node_tree.nodes.new("ShaderNodeVertexColor")
    color.layer_name=pigment.name
    material.node_tree.links.new(color.outputs["Color"],shader.inputs["Base Color"])
    material.node_tree.links.new(color.outputs["Alpha"],shader.inputs["Roughness"])


def seated_body(fur,kind):
    conf={
        "detective":((600,860,204,246),(600,824,84,139),[(407,970,87,106,(405,1077,83,56)),
                    (789,963,84,109,(802,1074,84,57))]),
        "boots":((592,805,174,236),(591,741,74,122),[(428,919,113,157,(347,1038,74,59)),
                    (746,915,111,158,(839,1043,66,58))])}
    (x,y,rx,ry),(cx,cy,cw,ch),ha=conf[kind]
    if kind=="detective":
        chest=[(451,673),(468,638),(532,622),(606,620),(690,632),(736,651),
               (752,681),(775,738),(797,810),(809,882),(810,947),(793,1006),
               (762,1061),(711,1097),(650,1109),(550,1105),(483,1086),
               (443,1047),(421,990),(404,911),(397,835),(402,765),(425,712)]
        body=tagged(cushion("Kitten seated torso",chest,28,117,114,fur,center=(604,859)))
    else:
        chest=[(485,601),(524,576),(656,572),(704,597),(731,641),(747,699),
               (760,767),(764,850),(752,923),(710,986),(598,1033),
               (491,1010),(434,957),(408,879),(409,800),(422,731),(447,660)]
        body=tagged(cushion("Kitten seated torso",chest,21,111,110,fur,center=(591,802)))
    body["bib_landmarks"]=[cx,cy,cw,ch]
    for i,(xx,yy,ww,hh,foot) in enumerate(ha):
        tagged(cushion("Kitten hind leg "+str(i),oval(xx,yy,ww,hh),48,ww*.87,ww*.78,fur))
        fx,fy,fw,fh=foot
        puppy_paw("Tucked back kitten paw",(fx,fy,1),fw,fh*.72,fw*.57,0,fur)
    if kind=="detective":
        kitten_tail(kind,fur)
        detective_foreleg(fur)
    else:
        kitten_tail(kind,fur)
        # The shoulders grow out of the chest. The wrists sit inside the leather openings.
        tagged(swept("Foreleg in upright boot",[
            (510,607,15,16,18),(502,642,-6,37,35),(488,692,-32,46,43),
            (482,743,-74,50,47),(483,781,-132,51,47),(484,813,-174,50,45),
            (485,850,-179,48,43),(484,879,-179,38,33)],fur))
        tagged(swept("Foreleg in slouched boot",[
            (679,606,15,16,18),(692,643,-5,36,34),(703,693,-31,45,43),
            (704,746,-76,50,46),(699,790,-133,50,47),(694,833,-173,50,45),
            (690,862,-179,48,43),(686,898,-179,39,36)],fur))


def panel(name,points,color,thick=8,bevel=3):
    n=len(points);vs=[point(x,y,d+k) for k in (0,thick) for x,y,d in points]
    fs=[tuple(range(n)),tuple(reversed(range(n,2*n)))]+[(i,(i+1)%n,(i+1)%n+n,i+n) for i in range(n)]
    return mesh(name,vs,fs,color,bevel=bevel/UNIT)


def sailor():
    fur=start("cat",630,624,1070)
    kitten_tail("sailor",fur)
    body=tagged(cushion("Captain standing inside the boat",[(568,558),(739,553),(784,603),(827,709),
        (830,792),(476,792),(466,747),(502,649)],32,105,105,fur))
    patch("Kitten captain's cream bib",body,oval(687,683,67,81),CREAM,2)
    kitten_head(fur,"sailor")
    for name,pts,clr in [
        ("Stern wall",[(161,743,-84),(389,678,99),(390,921,125),(186,1046,-78)],"B58152"),
        ("Rear hull",[(389,678,99),(1109,694,108),(906,1003,125),(390,921,125)],"B6814C"),
        ("Bow side panel",[(870,761,-95),(1116,696,102),(914,1046,75),(822,1098,-107)],"C2925E"),
        ("Front cardboard panel",[(159,741,-105),(869,750,-110),(819,1100,-113),(185,1055,-103)],"D39A5E")]:
        ob=panel(name,pts,clr,8,5);grain(ob.data.materials[0],115,.03,.76)
    for aa,bb in [((874,772,-108),(1115,702,93)),((867,770,-115),(820,1095,-117))]:
        ax,ay,ad=aa;bx,by,bd=bb
        n=16 if abs(bx-ax)>100 else 25
        pts=[]
        for i in range(n*5+1):
            t=i/(n*5)
            pts.append((ax+(bx-ax)*t,ay+(by-ay)*t-5.5*math.sin(TAU*i/5),ad+(bd-ad)*t-2))
        line("Exposed corrugated flute",pts,2.3,"835838")
    for x,y,r in [(361,908,97),(648,917,101)]:
        for j in range(5):
            pts=[(x+(r+(j-2)*3.3+3*math.sin(a*31+j))*math.cos(a),
                  y+(r+(j-2)*3.1+3.8*math.sin(a*26+j*2))*math.sin(a),-116-j*.15) for a in [TAU*i/160 for i in range(160)]]
            line("Scribbled blue crayon porthole",pts,1.6,"547CB0",cyclic=True)
    panel("Cream tape joining the bow",[(810,855,-122),(907,893,-121),(887,947,-122),(787,910,-122)],"F2D5A0",3,2)
    rod("Wooden flagstaff",point(201,744,61),point(163,288,60),20/UNIT,"B5804A")
    flag=panel("Teal wavy fishbone flag",[(180,342,53),(246,321,45),(306,328,52),(365,354,61),
        (419,344,25),(413,419,29),(420,477,25),(363,493,56),(289,484,57),(248,489,46),(188,514,50)],"28656D",4,3)
    grain(flag.data.materials[0],80,.04,.68)
    line("Ivory fishbone spine",[(235,415,17),(287,405,20),(353,416,18)],6,"F6D9A2")
    panel("Fish tail",[(346,413,14),(385,387,13),(370,420,11),(382,451,13)],"F6D9A2",3,1)
    panel("Fish head",[(216,419,14),(252,374,14),(267,373,14),(272,447,14),(244,442,14)],"F6D9A2",3,2)
    ellipsoid("Fish eye",247,404,10,5,5,2,"396E73")
    for x in (288,323):line("Fish ribs",[(x,377,15),(x-1,412,15),(x+5,442,15)],5,"F6D9A2")
    for yy in (333,352,515):
        line("Wound mast twine",[(153,yy+5,38),(168,yy+1,35),(184,yy-5,39)],7,"EBD1A7")
    panel("Folded paper crown",[(526,258,-53),(616,124,-45),(742,224,-47),(735,254,-51),(565,302,-65)],"F1D5A3",4,1.5)
    panel("Origami paper left fold",[(526,258,-78),(641,205,-79),(565,301,-79)],"F6DFB4",3,1)
    panel("Origami paper right fold",[(641,205,-82),(745,166,-78),(741,253,-76),(565,301,-81)],"F8E3BD",3,1)
    line("Crease on paper cap",[(618,131,-51),(641,204,-82),(741,253,-80)],1.2,"D7B98E")
    for x in (575,790):
        puppy_paw("Captain paw hooked over gunwale",(x,734,-137),65,39,46,0,fur)
    a,b=(871,757,-99),(1113,693,100)
    for offset in (0,10):
        line("Kraft paper facing on corrugated edge",
             [(a[0],a[1]+offset,a[2]),(b[0],b[1]+offset,b[2])],2.1,"CB9B67")
    for i in range(25):
        t=i/25
        def edge(q,down):
            return (a[0]+(b[0]-a[0])*q,a[1]+(b[1]-a[1])*q+down,a[2]+(b[2]-a[2])*q-1)
        line("Fold of corrugated cardboard",[edge(t,8),edge(t+.019,1.5),edge(t+.04,8)],1.5,"946941")



def detective():
    fur=start("cat",630,601,1165)
    seated_body(fur,"detective");kitten_head(fur,"detective")
    cap=cushion("Teal and cream tailored deerstalker",[(387,269),(417,222),(450,172),(514,117),
        (581,91),(642,88),(711,104),(764,139),(795,193),(813,244),(827,285),(816,297),
        (737,278),(659,255),(588,250),(517,255),(434,277),(399,280)],-49,108,85,"31818A")
    for pts in [[(429,210),(468,157),(521,127),(532,146),(512,210),(476,223)],
                [(529,137),(550,106),(589,88),(616,88),(617,133)],
                [(618,135),(697,145),(715,209),(608,198)],
                [(711,110),(754,134),(783,171),(704,148)],
                [(475,221),(516,208),(605,198),(602,248),(481,261)],
                [(715,210),(793,239),(813,281),(732,262)]]:
        patch("Cream check following crown",cap,pts,"F9E5BF",.6,1.2,straight=True)
    ellipsoid("Teal cap button",619,75,-47,40,21,30,"367D83")
    tagged(swept("Foreleg lifted to grip magnifying glass",[(470,651,-25,28,30),(457,704,-56,57,51),
        (439,765,-72,61,58),(417,804,-99,59,58),(386,807,-124,55,54),
        (352,783,-166,50,49),(327,748,-198,45,43)],fur))
    cx,cy=465,448
    glass=paint("C4E8EB",.14).copy()
    glass.name="Clear convex magnifier lens"
    tree=glass.node_tree
    shader=next(n for n in tree.nodes if n.type=="BSDF_PRINCIPLED")
    transparent=tree.nodes.new("ShaderNodeBsdfTransparent")
    mix=tree.nodes.new("ShaderNodeMixShader")
    mix.inputs[0].default_value=.014
    tree.links.new(transparent.outputs[0],mix.inputs[1]);tree.links.new(shader.outputs[0],mix.inputs[2])
    tree.links.new(mix.outputs[0],next(n for n in tree.nodes if n.type=="OUTPUT_MATERIAL").inputs["Surface"])
    ellipsoid("Convex glass in the brass bezel",cx,cy,-226,129,134,11,glass)
    for r,w,col in [(137,10,"BB822F"),(139,6.4,"E7AD41"),(128,3.0,"F4C765")]:
        line("Polished brass magnifier bezel",[(cx+r*math.cos(a),cy+r*1.04*math.sin(a),-228)
             for a in [i*TAU/60 for i in range(60)]],w,col,cyclic=True)
    line("Lens reflection",[(369,407,-230),(396,365,-232),(435,341,-229)],8,"FFF2D9",[.25,1,.24])
    line("Walnut magnifier handle",[(420,583,-222),(392,649,-223),(349,752,-223),(316,826,-224)],26,"92552F",[.52,1,1.05,.85])
    line("Brass handle ferrule",[(415,589,-225),(404,625,-225)],27,"CC9035")
    palm=leg("Curled toes around the wooden handle",[(284,678),(316,649),(354,638),(387,642),(416,660),
        (426,688),(419,710),(409,728),(407,749),(389,765),(379,780),(359,788),(321,786),
        (294,777),(270,781),(265,754),(266,716)],-237,44,fur,[(377,678,29,24),(363,718,36,24)])
    palm.name="Raised foreleg paw holding the handle"
    body=blend_limbs(bib_outline=[(583,698),(622,701),(657,719),(678,759),(680,795),
        (666,834),(645,884),(622,950),(604,1018),(589,1060),(571,1024),(548,966),
        (523,893),(509,824),(511,772),(527,731),(551,708)])
    finish_detective_paw(body)


# Each row gives the height and outside edges of one closed limb on the design sheet.
# The near foreleg spreads lower and farther forward over the free end of the yarn.
YARN_LEGS = (
    ("Far foreleg", ((703,472,526),(745,455,550),(775,450,549),(800,449,552),
        (825,452,561),(850,456,573),(875,462,584),(900,471,593),(925,481,598),
        (945,490,604),(965,498,609),(975,500,612),(986,498,619),
        (1005,490,626),(1018,487,628),(1030,487,628),(1041,490,625),
        (1050,495,619),(1057,510,607),(1060,536,586)), (535,586), -115, 69),
    ("Near foreleg", ((703,742,785),(745,737,800),(775,735,804),(800,737,810),
        (825,731,814),(850,725,820),(875,717,820),(900,712,817),(925,702,811),
        (945,695,804),(965,690,800),(975,688,798),(987,682,805),
        (1005,674,815),(1017,670,820),(1028,668,820),(1043,670,819),
        (1055,674,814),(1066,682,804),(1073,700,790),(1077,721,770)), (720,773), -159, 73),
    ("Far hind paw", ((942,452,464),(944,439,477),(949,424,490),(956,412,497),(963,404,503),(980,392,510),
        (995,386,511),(1007,387,510),(1021,392,508),(1033,404,506),
        (1039,418,498),(1041,433,484)), (411,452), -46, 53),
    ("Near hind paw", ((941,852,859),(944,839,872),(952,821,890),(970,807,902),(986,797,914),
        (1000,795,917),(1013,795,916),(1025,798,912),(1036,809,903),
        (1041,822,893),(1043,839,878)), (850,891), -48, 55),
)


def yarn_limbs(fur, tail, tail_tip, far_thigh):
    import numpy as np
    swatch_path = Path(__file__).resolve().parents[2] / "art/models/transformations/yarn-paw-pigments.png"
    swatch = bpy.data.images.load(str(swatch_path), check_existing=False)
    colors = np.empty(len(swatch.pixels), dtype=np.float32)
    swatch.pixels.foreach_get(colors)
    colors = colors.reshape(swatch.size[1], swatch.size[0], 4)[::-1].copy()
    bpy.data.images.remove(swatch)
    # Blender exposes the encoded bytes of this 8-bit pigment strip through Image.pixels.
    rgb = colors[:,:,:3]
    colors[:,:,:3] = np.where(rgb <= .04045,rgb/12.92,((rgb+.055)/1.055)**2.4)
    mat = fur.copy()
    mat.name = "Short fur on the seated yarn kitten"
    tree = mat.node_tree
    shader = next(n for n in tree.nodes if n.type == "BSDF_PRINCIPLED")
    pigment = tree.nodes.new("ShaderNodeVertexColor")
    pigment.layer_name = "Seated coat pigment"
    tree.links.new(pigment.outputs["Color"], shader.inputs["Base Color"])
    emission = tree.nodes.new("ShaderNodeEmission")
    tree.links.new(pigment.outputs["Color"],emission.inputs["Color"])
    emission.inputs["Strength"].default_value = 2**.31
    mix = tree.nodes.new("ShaderNodeMixShader")
    mix.inputs[0].default_value = .94
    tree.links.new(shader.outputs[0],mix.inputs[1])
    tree.links.new(emission.outputs[0],mix.inputs[2])
    tree.links.new(mix.outputs[0],next(n for n in tree.nodes if n.type=="OUTPUT_MATERIAL").inputs["Surface"])
    shader.inputs["Specular IOR Level"].default_value = .12
    shader.inputs["Roughness"].default_value = .8
    def apply_pigment(obj, values):
        obj.data.materials[0] = mat
        obj["preserve_original_fur"] = True
        layer = obj.data.color_attributes.new(name=pigment.layer_name,type="FLOAT_COLOR",domain="POINT")
        layer.data.foreach_set("color",values)

    # Two more swatches follow the tail and the far haunch in design coordinates.
    # The tip retains its separate cream ID for the wardrobe dyes.
    for obj,index,bounds in ((tail,4,(828,642,1074,1002)),
                             (tail_tip,4,(828,642,1074,1002)),
                             (far_thigh,5,(373,802,543,1009))):
        vertices = np.empty(len(obj.data.vertices)*3,dtype=np.float32)
        obj.data.vertices.foreach_get("co",vertices)
        vertices = vertices.reshape(-1,3)
        x0,y0,x1,y1 = bounds
        sx = np.clip((626+UNIT*vertices[:,0]-x0)*63/(x1-x0),0,63)
        sy = np.clip((1110-UNIT*vertices[:,2]-y0)*127/(y1-y0),0,127)
        ix,iy = sx.astype(int),sy.astype(int)
        jx,jy = np.minimum(ix+1,63),np.minimum(iy+1,127)
        u,v = (sx-ix)[:,None],(sy-iy)[:,None]
        strip = colors[:,index*64:(index+1)*64]
        upper = strip[iy,ix]*(1-u)+strip[iy,jx]*u
        lower = strip[jy,ix]*(1-u)+strip[jy,jx]*u
        apply_pigment(obj,((1-v)*upper+v*lower).astype("float32").ravel())

    for index, (name, rows, clefts, depth, thickness) in enumerate(YARN_LEGS):
        # Cross sections of the wrist and paw are continuous. The furrows turn down
        # the front and underneath the sole rather than ending in separate toe beads.
        segments = 112
        vertices, faces, paint_values = [], [], []
        positions, left, right = np.array(rows).T
        top, sole = positions[0], positions[-1]
        ys = np.unique(np.r_[positions, np.arange(top, sole, 3)])
        strip = colors[:, index*64:(index+1)*64]
        lower_edge = smooth(list(zip(left, positions)),8,False)
        upper_edge = smooth(list(zip(right, positions)),8,False)
        left_y,left_x = zip(*sorted((p.y,p.x) for p in lower_edge))
        right_y,right_x = zip(*sorted((p.y,p.x) for p in upper_edge))
        for y in ys:
            l, r = np.interp(y,left_y,left_x), np.interp(y,right_y,right_x)
            distal = min(1, max(0, (y-sole+76)/57))
            sole_weight = min(1, max(0, (y-sole+22)/22))
            taper = min(1., math.sqrt(max(.003, (sole-y)/24)), math.sqrt(max(.005, (y-top)/28)))
            # Keep the upper far leg between the ruff and the chest.
            shoulder = max(0,min(1,(y-723)/42,(852-y)/44)) if index==0 else 0
            for j in range(segments):
                a = TAU*j/segments
                u = (1+math.cos(a))/2
                x = l+(r-l)*u
                front = max(0,-math.sin(a))
                grooves = sum(math.exp(-((x-c+(sole-y)*s)/5.4)**2)
                              for c,s in zip(clefts,(.065,-.07)))
                slope = (sole-y)*(.29 if index<2 else .07)
                if index<2:
                    slope += 103*min(1,max(0,(849-y)/98))
                d = depth+slope+thickness*taper*math.sin(a)+20*grooves*distal*front
                d += shoulder*((-23+32*math.sin(a))-d)
                vertices.append(point(x, y-3.2*grooves*sole_weight, d))
                sy,sx=127*(y-top)/(sole-top),63*u
                iy,ix=int(sy),int(sx)
                jy,jx=min(127,iy+1),min(63,ix+1)
                lower=(1-sx+ix)*strip[iy,ix,:3]+(sx-ix)*strip[iy,jx,:3]
                upper=(1-sx+ix)*strip[jy,ix,:3]+(sx-ix)*strip[jy,jx,:3]
                rgb=lower*(1-sy+iy)+upper*(sy-iy)
                paint_values.extend((*rgb,1))
        for row in range(len(ys)-1):
            for j in range(segments):
                k = (j+1)%segments
                faces.append((row*segments+j,row*segments+k,(row+1)*segments+k,(row+1)*segments+j))
        faces.extend((tuple(reversed(range(segments))),tuple(range(len(vertices)-segments,len(vertices)))))
        obj = tagged(mesh(name,vertices,faces,mat,True))
        if obj.data.polygons[3*segments//4].normal.y>0:
            obj.data.flip_normals()
        apply_pigment(obj,paint_values)


def seated_yarn_kitten(fur):
    tail,tail_tip=kitten_tail("yarn",fur)
    torso = tagged(cushion("Kitten seated torso",
        [(463,754),(555,683),(710,686),(807,758),(828,854),(814,963),(762,1012),
         (704,1027),(622,1027),(542,1017),(482,965),(502,867),(504,809)],34,110,103,fur))
    # Recess the left side of the chest beneath the foreleg, including its cream inlay.
    for v in torso.data.vertices:
        x,y=626+v.co.x*UNIT,1110-v.co.z*UNIT
        upper=max(0,min(1,(865-y)/60))
        left=max(0,min(1,(613-x)/55))
        v.co.y+=104*upper*left/UNIT
    torso.data.update()
    belly = patch("Tapered cream chest behind the foreleg",torso,
        [(540,727),(661,690),(743,750),(730,851),(707,923),(690,979),
         (675,1027),(621,1027),(602,966),(580,901),(558,847)],"FFE4B9",1,1)
    # Allow for the curved marking's coarser triangulation over the recessed shoulder.
    for v in belly.data.vertices:
        x,y=626+v.co.x*UNIT,1110-v.co.z*UNIT
        lift=min(1,max(0,(655-x)/22))*min(1,max(0,(895-y)/26))
        v.co.y-=11*lift/UNIT
    belly.data.update()
    for obj in (torso,belly):
        obj["preserve_original_fur"] = True
        material = obj.data.materials[0].copy()
        obj.data.materials[0] = material
        tree = material.node_tree
        shader = next(n for n in tree.nodes if n.type=="BSDF_PRINCIPLED")
        values = []
        stops = [(742,"BE7945"),(808,"D69563"),(865,"F2BE90"),
                 (913,"E2A676"),(952,"BF8150"),(993,"935126"),(1030,"6C310D")]
        if obj == torso:
            stops = [(740,"C36A28"),(875,"E8853B"),(960,"B6672E"),(1030,"74330D")]
        for v in obj.data.vertices:
            y = 1110-(obj.matrix_world @ v.co).z*UNIT
            lo,hi = next(((a,b) for a,b in zip(stops,stops[1:]) if y<b[0]),stops[-2:])
            t = min(1,max(0,(y-lo[0])/(hi[0]-lo[0])))
            c = Vector(rgba(lo[1])[:3]).lerp(Vector(rgba(hi[1])[:3]),t)
            v_world=obj.matrix_world @ v.co
            x=626+v_world.x*UNIT
            bight_shadow=math.exp(-((x-693)/52)**4-((y-771)/55)**4)
            c*=1-.62*bight_shadow
            if obj==belly:
                # Warm reflected light softens the transition from the shoulder to the bib.
                bounce=math.exp(-((y-814)/26)**2)*min(1,max(0,(630-x)/43))
                for channel,gain in enumerate((.36,.87,.91)):
                    c[channel]*=1+gain*bounce
            values.extend((*c,1))
        layer=obj.data.color_attributes.new(name="Seated chest fur",type="FLOAT_COLOR",domain="POINT")
        layer.data.foreach_set("color",values)
        color=tree.nodes.new("ShaderNodeVertexColor")
        color.layer_name=layer.name
        tree.links.new(color.outputs["Color"],shader.inputs["Base Color"])
        tree.links.new(color.outputs["Color"],shader.inputs["Emission Color"])
        shader.inputs["Emission Strength"].default_value=.24
    far_thigh=tagged(cushion("Far folded thigh",
        [(455,803),(438,808),(417,818),(400,834),(387,855),(378,878),
         (375,901),(377,923),(384,946),(395,964),(424,989),(462,1000),
         (509,981),(536,947),(541,901),(531,859),(502,823),(477,807)],
        58,66,72,fur,center=(459,903)))
    tagged(cushion("Folded back thigh",oval(822,903,86,112),58,66,72,fur))
    yarn_limbs(fur,tail,tail_tip,far_thigh)


def wool_strand(name, path, radius, wool):
    """One continuous strand with three softly twisted plies along its surface."""
    vertices, faces = [], []
    segments = 30
    length = 0
    distances = []
    side = None
    for i,center in enumerate(path):
        tangent=(path[min(i+1,len(path)-1)]-path[max(0,i-1)]).normalized()
        if i:
            length+=(center-path[i-1]).length
        distances.append(length/radius)
        if side is None:
            side=tangent.cross(Vector((0,0,1))).normalized()
        side=(side-tangent*side.dot(tangent)).normalized()
        normal=tangent.cross(side)
        for j in range(segments):
            a=TAU*j/segments
            groove=(.5+.5*math.cos(3*a-TAU*length/(radius*2.45)))**5
            offset=radius*(1-.095*groove)*(side*math.cos(a)+normal*math.sin(a))
            vertices.append(point(*(center+offset)))
    uv=[]
    for row in range(len(path)-1):
        for j in range(segments):
            k=(j+1)%segments
            faces.append((row*segments+j,row*segments+k,(row+1)*segments+k,(row+1)*segments+j))
            for i,across in ((row,j),(row,j+1),(row+1,j+1),(row+1,j)):
                uv.extend((distances[i],across/segments))
    faces.extend((tuple(reversed(range(segments))),tuple(range(len(vertices)-segments,len(vertices)))))
    uv.extend([0.]*(4*segments))
    obj=mesh(name,vertices,faces,wool,True)
    obj.data.uv_layers.new(name="Spun wool").data.foreach_set("uv",uv)


def spun_wool(shadow="C36A07",light="FFCA58",gold=False,under_chin=False):
    material=paint(light,.74).copy()
    material.name="Spun wool "+light
    grain(material,190,.002,.67)
    tree=material.node_tree
    shader=next(n for n in tree.nodes if n.type=="BSDF_PRINCIPLED")
    shader.inputs["Sheen Weight"].default_value=.10
    shader.inputs["Specular IOR Level"].default_value=.24
    coords=tree.nodes.new("ShaderNodeUVMap")
    coords.uv_map="Spun wool"
    twist=tree.nodes.new("ShaderNodeVectorMath")
    twist.operation="DOT_PRODUCT"
    twist.inputs[1].default_value=(-TAU*2.86,TAU*21,0)
    wave=tree.nodes.new("ShaderNodeMath")
    wave.operation="SINE"
    tree.links.new(coords.outputs["UV"],twist.inputs[0])
    tree.links.new(twist.outputs["Value"],wave.inputs[0])
    # Fine fibres modulate the nap. Overlapping hanks cast a wider contact shadow.
    bump=next(n for n in tree.nodes if n.type=="BUMP")
    nap=tree.nodes.new("ShaderNodeBump")
    nap.inputs["Strength"].default_value=.12
    nap.inputs["Distance"].default_value=.0011
    tree.links.new(wave.outputs[0],nap.inputs["Height"])
    tree.links.new(bump.outputs["Normal"],nap.inputs["Normal"])
    tree.links.new(nap.outputs["Normal"],shader.inputs["Normal"])
    occlusion=tree.nodes.new("ShaderNodeAmbientOcclusion")
    occlusion.inputs["Distance"].default_value=.22
    ramp=tree.nodes.new("ShaderNodeValToRGB")
    ramp.color_ramp.elements[0].color=rgba(shadow)
    ramp.color_ramp.elements[1].color=rgba(light)
    geo=tree.nodes.new("ShaderNodeNewGeometry")
    normal=tree.nodes.new("ShaderNodeVectorMath")
    normal.operation="DOT_PRODUCT"
    normal.inputs[1].default_value=Vector((-.42,-.58,.72)).normalized()
    tree.links.new(geo.outputs["Normal"],normal.inputs[0])
    light_wrap=tree.nodes.new("ShaderNodeMath")
    light_wrap.operation="MULTIPLY_ADD"
    light_wrap.inputs[1].default_value=.56
    light_wrap.inputs[2].default_value=.44
    tree.links.new(normal.outputs["Value"],light_wrap.inputs[0])
    contact=tree.nodes.new("ShaderNodeMath")
    contact.operation="MULTIPLY"
    tree.links.new(occlusion.outputs["AO"],contact.inputs[0])
    tree.links.new(light_wrap.outputs[0],contact.inputs[1])
    tree.links.new(contact.outputs[0],ramp.inputs[0])
    # Irregular fibres follow the same twist as the three sculpted plies.
    fibre=tree.nodes.new("ShaderNodeMath")
    fibre.operation="MULTIPLY_ADD"
    fibre.inputs[1].default_value=.051
    fibre.inputs[2].default_value=.89
    tree.links.new(wave.outputs[0],fibre.inputs[0])
    noise=next(n for n in tree.nodes if n.type=="TEX_NOISE")
    noise.inputs["Scale"].default_value=84
    tree.links.new(geo.outputs["Position"],noise.inputs["Vector"])
    fleck=tree.nodes.new("ShaderNodeMath")
    fleck.operation="MULTIPLY_ADD"
    fleck.inputs[1].default_value=.22
    tree.links.new(noise.outputs["Fac"],fleck.inputs[0])
    tree.links.new(fibre.outputs[0],fleck.inputs[2])
    fibre_path=Path(__file__).resolve().parents[2]/"art/models/transformations/spun-yarn-fibres.png"
    nap_map=tree.nodes.new("ShaderNodeTexImage")
    nap_map.image=bpy.data.images.load(str(fibre_path),check_existing=True)
    nap_map.image.colorspace_settings.name="Non-Color"
    if not nap_map.image.packed_file:
        nap_map.image.pack()
    repeat=tree.nodes.new("ShaderNodeVectorMath")
    repeat.operation="MULTIPLY"
    repeat.inputs[1].default_value=(.12,3,1)
    tree.links.new(coords.outputs["UV"],repeat.inputs[0])
    tree.links.new(repeat.outputs[0],nap_map.inputs["Vector"])
    texture=tree.nodes.new("ShaderNodeMath")
    texture.operation="MULTIPLY_ADD"
    texture.inputs[1].default_value=.88
    texture.inputs[2].default_value=.57
    tree.links.new(nap_map.outputs["Color"],texture.inputs[0])
    textured=tree.nodes.new("ShaderNodeMath")
    textured.operation="MULTIPLY"
    tree.links.new(texture.outputs[0],textured.inputs[0])
    tree.links.new(fleck.outputs[0],textured.inputs[1])
    dyed=tree.nodes.new("ShaderNodeVectorMath")
    dyed.operation="SCALE"
    tree.links.new(ramp.outputs[0],dyed.inputs[0])
    tree.links.new(textured.outputs[0],dyed.inputs["Scale"])
    finish=dyed.outputs[0]
    if gold:
        # Golden wool has amber shadows, while lit fibres keep their pale tips.
        grade=tree.nodes.new("ShaderNodeRGBCurve")
        for channel,points in zip(grade.mapping.curves,(
                ((.32,.38),(.57,.78),(.79,.95)),
                ((.16,.10),(.32,.26),(.57,.56)),
                ((.018,.002),(.050,.012),(.095,.073)))):
            for x,y in points:
                channel.points.new(x,y)
        grade.mapping.update()
        tree.links.new(finish,grade.inputs["Color"])
        finish=grade.outputs[0]
    if under_chin:
        height=tree.nodes.new("ShaderNodeSeparateXYZ")
        tree.links.new(geo.outputs["Position"],height.inputs[0])
        shade=tree.nodes.new("ShaderNodeMapRange")
        shade.inputs["From Min"].default_value=(1110-935)/UNIT
        shade.inputs["From Max"].default_value=(1110-800)/UNIT
        shade.inputs["To Min"].default_value=1
        shade.inputs["To Max"].default_value=.57
        tree.links.new(height.outputs["Z"],shade.inputs["Value"])
        shaded=tree.nodes.new("ShaderNodeVectorMath")
        shaded.operation="SCALE"
        tree.links.new(finish,shaded.inputs[0])
        tree.links.new(shade.outputs[0],shaded.inputs["Scale"])
        finish=shaded.outputs[0]
    tree.links.new(finish,shader.inputs["Base Color"])
    emission=tree.nodes.new("ShaderNodeEmission")
    tree.links.new(finish,emission.inputs[0])
    emission.inputs["Strength"].default_value=1.27
    mix=tree.nodes.new("ShaderNodeMixShader")
    mix.inputs[0].default_value=.49
    tree.links.new(shader.outputs[0],mix.inputs[1])
    tree.links.new(emission.outputs[0],mix.inputs[2])
    tree.links.new(mix.outputs[0],next(n for n in tree.nodes if n.type=="OUTPUT_MATERIAL").inputs["Surface"])
    return material


def wound_ball(wool):
    previous=set(bpy.context.scene.objects)
    center=Vector((238,988,-98))
    ellipsoid("Wool beneath the outer windings",*center,119,119,95,"C68131")
    # Wind the core continuously; its tilted axis exposes curved turns at both sides.
    axis=Vector((-.69,.65,-.32)).normalized()
    side=axis.cross(Vector((0,0,1))).normalized()
    back=axis.cross(side)
    path=[]
    for i in range(1701):
        t=i/1700
        angle=TAU*(10.5*t-.25)
        lat=-1.53+3.06*t
        v=axis*math.sin(lat)+(side*math.cos(angle)+back*math.sin(angle))*math.cos(lat)
        shell=105+9*(1-abs(v.z))**2
        path.append(center+Vector((v.x*shell,v.y*shell*.99,v.z*101)))
    wool_strand("Continuous inner wool winding",path,14.6,wool)

    def wrap(name, front, radius=119, gauge=15.3):
        # Surface guides follow the visible loop. Complete it around the back hemisphere.
        guides=[]
        for x,y in front:
            u,v=(x-center.x)/124,(y-center.y)/124
            k=max(1,math.hypot(u,v)/.999)
            u,v=u/k,v/k
            guides.append(Vector((u,v,-math.sqrt(max(0,1-u*u-v*v)))))
        entry,exit=guides[0],guides[-1]
        far=lambda p: Vector((p.x,p.y,max(.3,-p.z))).normalized()
        guides.extend([far(exit),(far(exit)+far(entry)+Vector((0,0,2))).normalized(),far(entry)])
        curve=smooth(guides,15,True)
        path=[]
        for v in curve:
            v=v.normalized()
            r=111+(radius-111)*min(1,max(0,-v.z*2.15))
            path.append(center+Vector((r*v.x,r*.99*v.y,r*.95*v.z)))
        wool_strand(name,path+[path[0]],gauge,wool)

    # Shorter windings change direction at the crown and pass under the long diagonals.
    inner_turns = [
        [(200,870),(189,890),(187,912),(193,936),(207,953)],
        [(174,881),(161,903),(162,932),(174,956),(191,973)],
        [(149,900),(138,926),(140,949),(154,974),(173,994)],
        [(128,925),(120,951),(123,975),(136,994),(152,1012)],
        [(191,904),(213,880),(242,871),(271,878),(286,887)],
        [(183,932),(211,903),(242,887),(273,886),(300,900)],
        [(329,947),(349,964),(358,985),(360,1009)],
        [(318,971),(341,992),(353,1016),(351,1040)],
        [(303,998),(330,1020),(342,1044),(340,1063)],
    ]
    for i,guide in enumerate(inner_turns):
        wrap(f"Underlying cross winding {i+1}",guide,118,15)
    # The diagonal turns fan out on the front and converge again below the ball.
    diagonals=[
        [(170,1081),(161,1055),(170,1015),(192,973),(224,937),(262,906),(284,881)],
        [(195,1097),(184,1069),(191,1032),(215,987),(247,946),(280,914),(306,891)],
        [(219,1104),(211,1079),(219,1043),(243,1006),(274,967),(302,930),(326,909)],
        [(246,1105),(237,1082),(249,1051),(274,1016),(302,979),(326,944),(344,927)],
        [(271,1096),(266,1073),(281,1049),(306,1016),(329,978),(346,950),(354,939)],
    ]
    for i,guide in enumerate(diagonals):
        wrap(f"Diagonal outer turn {i+1}",guide,119,16.5)
    wrap("Upper locking turn",[(145,1072),(133,1048),(138,1016),(161,982),
         (195,950),(237,927),(278,912),(312,908),(336,916),(349,931)],145,17.8)
    # Compress the turns into the ball at its rim where successive strands touch.
    for obj in set(bpy.context.scene.objects)-previous:
        if obj.name.startswith("Wool beneath"):
            continue
        for v in obj.data.vertices:
            x,z=v.co.x*UNIT+626-center.x, 1110-v.co.z*UNIT-center.y
            r=math.hypot(x/129,z/128)
            if r>.90:
                factor=(.90+.102*math.tanh((r-.90)/.102))/r
                v.co.x=(center.x+x*factor-626)/UNIT
                v.co.z=(1110-center.y-z*factor)/UNIT
        obj.data.update()



def yarn():
    fur=start("cat",610,588,1170)
    with bpy.context.temp_override(window=bpy.context.window_manager.windows[0]):
        dress_yarn_kitten(fur)


def dress_yarn_kitten(fur):
    seated_yarn_kitten(fur)
    kitten_head(fur,"yarn")
    nose=bpy.context.scene.objects["Soft triangular nose"]
    nose.data.materials[0]=paint("EA6857",.50)
    colors=["D9781C","F9AA27","B85D20","E98723"]
    wool=spun_wool(gold=True)
    mane=[spun_wool("C14E0C","FD9431"), wool, spun_wool("A83E13","E7792C")]
    # Wool filling behind the folded loops prevents stray, free-standing rings.
    for i in range(31):
        a=TAU*i/31
        ellipsoid("Packed underside of the yarn mane",623+295*math.cos(a),430+250*math.sin(a),
                  20,72,62,52,colors[(i+2)%4],math.degrees(a))
    # A back tier fills the spaces between the traced front folds.
    for i in range(14):
        a=i*TAU/14
        if math.sin(a)<-.48 and abs(math.cos(a))>.36:
            continue
        points=[]
        for j in range(18):
            t=j*math.pi/17
            radius=125*math.sin(t)
            wide=49*math.cos(t)
            # At the far shoulder the back fold lies to the outside of the foreleg.
            shoulder_fold=math.sin(t)**2 if i==4 else 0
            points.append((623+(276+radius)*math.cos(a)-wide*math.sin(a)-96*shoulder_fold,
                           430+(220+radius)*math.sin(a)+wide*math.cos(a)-18*shoulder_fold,
                           48-29*math.sin(t)))
        wool_strand("Rear mane fold",smooth(points,7,False),23,mane[2 if i==4 else (i+2)%3])
    # The folds return behind the face. Their buried ends cannot cross the cheeks.
    folds = [
        (0,[(528,263),(523,196),(510,148),(535,127),(558,148),(582,247)]),
        (1,[(575,247),(589,164),(600,97),(619,67),(652,67),(665,90),(652,159),(634,242)]),
        (2,[(633,248),(670,169),(705,123),(741,127),(751,150),(731,207),(710,258)]),
        (1,[(711,267),(770,224),(810,225),(838,254),(831,281),(786,311)]),
        (0,[(788,317),(850,276),(893,253),(923,263),(934,291),(905,317),(834,346)]),
        (2,[(814,341),(888,315),(950,316),(977,343),(973,370),(938,389),(838,396)]),
        (1,[(836,405),(925,399),(978,412),(997,448),(981,470),(937,474),(840,450)]),
        (0,[(848,477),(932,489),(980,517),(988,548),(970,573),(932,574),(835,528)]),
        (2,[(836,550),(901,578),(943,621),(939,661),(912,682),(873,673),(791,600)]),
        (0,[(771,595),(819,651),(841,714),(820,753),(784,752),(754,720),(714,625)]),
        (1,[(671,626),(661,689),(649,729),(613,753),(573,740),(554,701),(575,621)]),
        (2,[(555,613),(544,682),(545,731),(534,761),(507,772),(479,766),(465,746),(467,709),(487,615)]),
        (1,[(478,592),(450,652),(410,702),(375,724),(349,704),(353,668),(423,588)]),
        (0,[(427,564),(369,605),(320,641),(280,637),(268,608),(286,574),(383,526)]),
        (1,[(380,509),(311,525),(271,515),(251,485),(263,454),(308,437),(376,440)]),
        (2,[(370,428),(313,400),(284,369),(294,331),(321,310),(355,321),(397,366)]),
        (1,[(395,341),(347,302),(333,267),(351,238),(391,230),(430,249),(459,287)]),
        (0,[(448,287),(430,233),(434,190),(466,168),(497,185),(514,215),(524,267)]),
    ]
    for i,(tone,outline) in enumerate(folds):
        path = [(x,y,22 if j in (0,len(outline)-1) else -63-11*math.sin(j*math.pi/(len(outline)-1)))
                for j,(x,y) in enumerate(outline)]
        cushion(f"Shadowed wool inside fold {i+1}",outline,18,27,25,("C36824","DA8A29","AD5128")[tone])
        wool_strand(f"Folded mane loop {i+1}",smooth(path,15,False),25,mane[tone])
    # This open central bight hangs below the other loops. The working end comes
    # out of its opening, then disappears underneath the planted right paw.
    collar=[(659,642,-48),(682,686,-87),(666,735,-121),(647,779,-148),
            (649,809,-159),(672,829,-165),(699,824,-154),(721,803,-144),
            (724,776,-132),(710,748,-101),(683,720,-80),(670,691,-35)]
    hanging=spun_wool("A24B04","FFC04D",gold=True,under_chin=True)
    wool_strand("Hanging gold bight",smooth(collar,14,False),21,hanging)
    wound_ball(wool)
    # Pass behind the inner toes before emerging below the loaded paw onto the floor.
    loose=[(657,786,-118),(665,793,-130),(678,803,-151),(688,819,-175),(694,837,-189),(686,870,-158),
           (669,916,-134),(659,954,-122),(665,981,-116),(682,1011,-123),
           (704,1041,-151),(685,1066,-204),(621,1106,-179),(518,1124,-156),
           (427,1125,-147),(361,1109,-142),(323,1083,-158),(306,1060,-175),(284,1042,-166)]
    wool_strand("Working strand from the bight under the near paw",smooth(loose,16,False),16,hanging)
    from reference_finish import ground_contact
    for x,y,width,alpha in [(638,1034,304,.45),(238,1110,137,.39),(553,1059,97,.28),(746,1076,104,.34)]:
        footprint=[point(x+u*width,y+v*12,420) for u,v in [(-1,-1),(1,-1),(1,1),(-1,1)]]
        ground_contact("Wool and paw floor contact",footprint,alpha,"Kitten contact shade")


def cavalier_boot(name,x,y,mirror):
    def row(dx,dy,dd,rx,rd,tilt):
        return (x+mirror*dx,y+dy,-191+dd,rx,rd,tilt)
    def rim_drop(px):
        return 20+.17*(px-x) if mirror==-1 else -3-.035*(px-x)
    def shape_upper(obj):
        for vertex in obj.data.vertices:
            px=626+vertex.co.x*UNIT
            height=vertex.co.z*UNIT+y-1110
            vertex.co.z-=rim_drop(px)*max(0,min(1,(height-157)/104))/UNIT
        obj.data.update()
    leather="9C593A"
    profile=[(-12,-9,-30,2,2,0),(-12,-11,-32,118,127,16),(-11,-23,-32,124,133,17),
        (-9,-47,-31,123,130,22),(-7,-76,-30,115,124,23),(0,-114,-17,91,96,21),
        (4,-143,0,89,80,16),(4,-202,8,88,75,13),(4,-260,10,87,71,13),
        (2,-306,12,92,76,15),(2,-322,12,90,72,16),(2,-324,12,76,62,13),
        (2,-304,12,75,60,12),(3,-208,10,70,59,9),(0,-138,0,58,52,8),(0,-131,0,2,2,0)]
    if mirror==-1:
        profile[6:9]=[(8,-139,2,98,83,17),(14,-160,5,107,85,18),(0,-188,8,89,73,16),
                         (-7,-207,9,95,78,15),(1,-225,10,101,81,16),(9,-245,10,89,71,14)]
    shoe=turned(name+" continuous leather upper",[row(*r) for r in profile],leather)
    shape_upper(shoe)
    grain(shoe.data.materials[0],114,.017,.49)
    turned(name+" sewn welt and sole",[row(*r) for r in [(-12,-10,-32,117,126,16),
           (-12,-10,-32,123,130,17),(-12,-20,-32,125,133,17),(-12,-27,-32,122,130,17)]],"844A2B")
    cuff=turned(name+" hollow folded calf cuff",[row(*r) for r in [(2,-261,10,92,75,15),
        (2,-263,11,105,84,16),(2,-274,12,108,85,16),(2,-333,12,105,83,19),
        (2,-344,12,99,78,20),(2,-345,12,83,65,17),(2,-339,12,77,60,15),
        (2,-278,12,74,59,12)]],"AB6546")
    shape_upper(cuff)
    cuff.data.materials.append(paint("663C2B"))
    for f in cuff.data.polygons:
        if f.index//80 in (5,6):f.material_index=1
    for j in range(17):
        a=math.pi*(.08+j*.052)
        b=a+.065
        points=[(x+96*math.cos(t),y-321+18*math.sin(t)+rim_drop(x+96*math.cos(t)),
                 -92-80*math.sin(t)-1.5) for t in (a,b)]
        line("Boot cuff topstitch",points,1.5,"CC926B")
    surface_line("Toe cap seam",shoe,[(x-98,y-74),(x-80,y-104),(x-35,y-132),
        (x+26,y-139),(x+74,y-118),(x+94,y-89)],1.7,"834A33",[.15,.85,1,.85,.6,.15],1)


def boots():
    fur=start("cat",640,604,1220)
    seated_body(fur,"boots")
    brim=cushion("Broad upturned felt brim behind kitten",[(246,456),(288,394),(394,293),
        (485,223),(603,169),(720,179),(809,193),(854,230),(876,295),(878,351),
        (852,417),(804,473),(695,530),(560,596),(453,612),(349,591),(276,541),(249,506)],
        73,36,39,"792E52",center=(556,393))
    cushion("Soft plum hat crown",[(356,351),(374,282),(428,218),(494,173),(561,151),
        (622,149),(677,166),(702,200),(642,233),(540,278),(443,319)],38,67,65,"893658")
    kitten_head(fur,"boots")
    rolled=sliced("Sweeping bound felt edge",[(246,473),(250,437),(282,399),(355,352),
        (432,310),(546,259),(645,218),(730,189),(793,189),(832,202),(855,229),(874,279),
        (861,266),(827,248),(784,242),(731,258),(687,280),(633,279),(566,294),
        (487,325),(413,362),(340,410),(287,461),(265,499),(249,486)],-92,23,24,"893458",axis=0)
    grain(rolled.data.materials[0],120,.018,.78)
    feather=sliced("Sweeping notched gold plume",[(688,211),(694,163),(717,117),(756,78),
        (748,119),(778,78),(818,48),(853,39),(836,76),(873,44),(904,33),(942,38),
        (927,49),(970,49),(1017,75),(1041,113),(1050,164),(1046,198),(1032,177),
        (1006,165),(964,169),(925,154),(896,135),(923,160),(952,175),(921,194),
        (877,205),(842,202),(811,191),(847,214),(804,207),(766,190),(729,197)],-59,24,16,"F2B236",axis=0)
    surface_line("Golden feather quill",feather,[(702,191),(761,140),(839,98),(921,78),(1002,88)],
                 4.5,"DEA137",[1,1,.89,.52,.15])
    for xs,ys,xe,ye in [(738,158,737,128),(767,141,790,94),(808,117,832,72),
                        (846,101,879,62),(848,100,884,130),(887,87,928,120),
                        (918,84,959,111),(952,85,995,110)]:
        surface_line("Fine feather vane",feather,[(xs,ys),((xs+xe)/2,(ys+ye)/2),(xe,ye)],
                     1.65,"F9CB65",[.2,1,.15],.8)
    cavalier_boot("Left cavalier boot",472,1160,1)
    cavalier_boot("Slouched right cavalier boot",702,1164,-1)
    body=blend_limbs(bib_outline=[(590,618),(624,623),(650,647),(667,680),(671,710),
        (658,752),(646,805),(619,876),(594,925),(572,881),(550,822),
        (533,763),(524,713),(525,681),(539,647),(563,627)])
    fur_surface=body.data.materials[0].copy()
    fur_surface.name="Booted kitten short matte fur"
    body.data.materials[0]=fur_surface
    grain(fur_surface,205,.004,.8)


CATS={"cat-head-sailor-hat":sailor,"cat-extra-yarn":yarn,"cat-model-detective":detective,"cat-model-boots":boots}
if __name__=="__main__":
    for item in sys.argv[sys.argv.index("--")+1:] or CATS:
        CATS[item]()
        save_render(item)
