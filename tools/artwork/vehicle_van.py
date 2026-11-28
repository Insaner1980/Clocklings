"""Ice cream van with a moulded two-colour body, recessed counter and twisted rooftop soft serve."""
from vehicle_sculpt import *


def build():
    begin()
    pink="F26B98"; cream="F4D5AF"; mint="A4C56C"
    side=lambda x,y:48+(x-220)*.22+(y-700)*.10
    front=lambda x,y:151-(x-690)*.31+(y-700)*.14
    body=lambda x,y:min(side(x,y),front(x,y))
    wheel("Far rear tyre",463,931,56,67,18,-160,far=True)
    wheel("Far front tyre",896,932,65,87,33,-70,far=True)
    solid("Van undercarriage",[(248,861),(948,863),(992,926),(948,968),(643,986),(282,952)],-70,"4C3C40",15)
    wheel("Ice cream van rear wheel",315,933,67,92,42,55,"FDD799")
    wheel("Ice cream van front wheel",585,942,68,100,47,122,"FDD799")
    skirt=[(219,795),(230,770),(519,775),(599,789),(698,810),(1038,821),(1063,832),
           (1069,867),(1061,931),(1034,957),(965,972),(706,983),(669,976),(658,948),
           (643,893),(618,856),(585,846),(561,850),(536,876),(520,917),(518,958),
           (501,964),(395,953),(379,936),(367,885),(346,853),(317,835),(290,838),
           (268,859),(250,910),(243,943),(225,930),(220,899)]
    solid("Bubblegum pink wheel-arched sill",skirt,body,pink,24,23,88,True)
    top=[(222,788),(231,711),(249,627),(273,566),(301,523),(356,495),(443,480),(609,476),
         (718,483),(790,506),(836,546),(865,595),(907,688),(929,745),(982,754),(1024,766),(1053,793),
         (1066,827),(1058,854),(1021,879),(940,910),(853,917),(779,906),(713,888),
         (692,845),(670,819),(622,798),(573,791)]
    nose=solid("Continuous vanilla roof, bonnet and nose",top,body,cream,37,47,130)
    glazing("Sloped windscreen",[(681,596),(691,583),(733,575),(807,573),(846,583),(875,614),
          (906,691),(919,726),(912,737),(862,742),(735,746),(713,732),(698,686)],
          lambda x,y:body(x,y)+40)
    glazing("Quarter light",[(623,692),(627,639),(631,615),(638,612),(645,622),(663,691),
          (655,705),(632,707)],lambda x,y:body(x,y)+36)
    stroke("Vanilla door mirror stem",[(920,731,body(920,731)+40),
           (934,724,body(934,724)+47),(950,710,body(950,710)+54)],7,cream)
    for x,y,rx in [(644,720,34),(953,701,24)]:
        oval("Vanilla door mirror",x,y,rx,32,body(x,y)+53,25,cream)
    aperture=[(249,759),(253,684),(268,625),(285,597),(568,595),(591,610),(585,708),
              (580,775),(563,793),(249,786),(237,778)]
    solid("Pistachio service hatch moulding",aperture,lambda x,y:side(x,y)+39,mint,9,12,22)
    solid("Dark interior of serving hatch",[(270,755),(279,666),(293,618),(557,613),(573,631),
          (570,696),(561,765),(274,768)],lambda x,y:side(x,y)+44,"805638",5,23,5)
    for x,c in [(320,"995A3D"),(412,"EA7B9C"),(507,"AED368")]:
        d=side(x,731)+58
        oval("Hand scooped ice cream",x,731,43,44,d,26,c)
        for j in range(3):
            stroke("Scoop curl",[(x-34+j*5,730-j*7,d+18),(x-26+j*5,718-j*7,d+23),
               (x-8+j*5,709-j*7,d+23)],2.7,c,[.3,1,.28])
        solid("Paper sundae cup",[(x-44,739),(x+42,739),(x+36,763),(x+28,771),(x-35,768)],
              d+22,"F2D1A1",7,9,20)
        for offset in (-24,-11,2,15,28):
            stroke("Pleated sundae cup",[(x+offset,749,d+33),(x+offset*.86,761,d+31)],1.45,"E0B68B")
        stroke("Cream cup lip",[(x-44,741,d+32),(x,744,d+34),(x+44,741,d+32)],5,WHITE)
    stroke("Glossy counter lip",[(244,774,side(244,774)+94),(315,777,side(315,777)+95),
            (561,783,side(561,783)+95),(575,779,side(575,779)+93)],11,mint)
    for j in range(7):
        left=208+j*53; right=left+54; d=side(left,600)+84
        col=pink if j%2==0 else WHITE
        solid("Striped awning with rolled scallop",[(left+78,538),(left+89,534),(right+76,534),
              (right+78,542),(right+4,598),(right,616),(right-6,632),(right-22,640),
              (left+13,638),(left+2,627),(left,609),(left+4,596)],lambda x,y,v=d:v+(y-537)*.88,
              col,7,9,12)
        seam_d=lambda x,y:d+(y-537)*.88+8
        edging=[(left+4,618),(left+10,630),(left+24,636),(left+38,632),(left+49,619)]
        stroke("Turned edge of canvas scallop",[(x,y,seam_d(x,y)) for x,y in edging],2.2,
               "F6A6BF" if j%2==0 else "F3DFC9",[.18,.8,1,.8,.18])
    fit_car_eyes(nose, (772,822,134), (1000,807,90))
    stroke("Van smile",[(855,881,nose.depth(855,881)+4),(877,895,nose.depth(877,895)+5),
            (912,894,nose.depth(912,894)+5),(933,878,nose.depth(933,878)+4)],4.1,"4B3028",[.6,1,1,.65])
    for x,y,flip in [(763,940,1),(1007,922,-1)]:
        solid("Pink bumper inset",[(x-39,y-15),(x+26,y-11),(x+37,y-6),(x+27,y+11),
             (x+16,y+16),(x-38,y+13),(x-44,y+4)],lambda x,y:body(x,y)+26,"AB4D63",5,6,8)
    # Cone and its waffle lattice are built as a circular tapered volume about the Z axis.
    from transformation_studio import rod
    def cone(z,top,r1,r2,col):
        return rod("Waffle cone",pos(573,z,12),pos(573,top,12),r1/UNIT,pigment(col),r2/UNIT)
    cone(477,360,62,96,"E5A03F")
    oval("Cone mounting flange",573,476,85,12,15,72,"DF9F45")
    for sign in [-1,1]:
        for i in range(13):
            pts=[]
            for j in range(14):
                t=j/13; a=math.tau*i/13+sign*.99*t; r=62+t*34
                pts.append((573+r*math.cos(a),474-111*t,12+r*math.sin(a)))
            stroke("Raised waffle lattice",pts,3.3,"F2B851")
    oval("Cone rim",574,362,105,23,12,100,"EBA640")
    for phase,col in [(math.pi,"FFDFAC"),(0,"F481A9")]:
        pts=[]; widths=[]
        for j in range(65):
            t=j/64*(.80 if phase else .915); a=t*math.tau*1.67+phase; r=86*(1-t)**.70
            x=575+r*math.cos(a)+5*t**3-28*t**10
            y=338-177*t+10*math.sin(a)+17*t**15
            pts.append((x,y,18+r*math.sin(a))); widths.append(max(.05,(1-j/64)**.44))
        stroke("Twisted soft serve",pts,38,col,widths)
    stroke("Folded tip of strawberry soft serve",[(569,191,10),(583,178,14),(584,165,17),
           (577,155,18),(566,152,18),(557,157,18),(558,162,18)],17,"F481A9",[1,.93,.77,.57,.39,.22,.055])
    finish("car-model-ice-cream")

