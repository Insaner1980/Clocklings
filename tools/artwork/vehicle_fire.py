"""Fire engine. The side and cab share a real wheel opening; the original car lenses sit on its bonnet."""
from vehicle_sculpt import *

RED="E63228"
CRIMSON="BB2420"


def build():
    begin()
    SILVER=pigment("C2BDCA",.24,.43)
    side=lambda x,y:36+.188*(x-90)
    cab=lambda x,y:min(side(x,y),164-.32*(x-774))
    wheel("Far rear tyre",465,920,84,92,30,-130,far=True)
    wheel("Far front tyre",962,924,83,101,26,-100,far=True)
    solid("Ladder truck chassis",[(100,870),(420,853),(1004,840),(1077,868),(1045,944),(740,979),(212,952),(106,926)],
          -50,"39313A",16,15)
    wheel("Rear road wheel",232,930,73,96,37,53,"D1CAD0")
    wheel("Cab road wheel",666,939,79,111,47,131,"D1CAD0")
    body=[(94,682),(113,663),(224,658),(540,642),(609,641),(625,673),(626,790),(597,833),
          (580,903),(579,970),(354,967),(335,961),(328,913),(310,860),(281,833),(247,822),
          (212,824),(185,846),(164,881),(150,926),(97,926),(91,911)]
    solid("Fire equipment body with rear wheel arch",body,side,RED,17,22,155,True)
    solid("Tank roof behind shutter bank",[(394,648),(412,623),(459,611),(608,593),(653,610),(640,648)],
          30,RED,16,15,54,True)
    stroke("Rear black wheel arch",[(152,926,side(152,926)+8),(167,875,side(167,875)+8),
        (207,833,side(207,833)+8),(247,827,side(247,827)+8),(291,850,side(291,850)+8),
        (313,896,side(313,896)+8),(324,956,side(324,956)+8)],12,"5A5057",[.8,1,1,1,1,1,.8])
    for left,right,top,bottom in [(144,330,688,818),(356,563,678,821)]:
        def rect(a,b,c,d):
            return [(a+7,c+1),(b-6,c-3),(b,c+5),(b,d-8),(b-6,d),(a+6,d-2),(a,d-8),(a,c+8)]
        solid("Flush aluminium shutter frame",rect(left,right,top,bottom),lambda x,y:side(x,y)+20,SILVER,5,5,10)
        solid("Recessed silver roller blind",rect(left+9,right-9,top+9,bottom-10),
              lambda x,y:side(x,y)+24,"AAA5B1",5,4,5)
        for i in range(8):
            y=top+17+(bottom-top-30)*i/8
            stroke("Rolled aluminium shutter slat",[(left+11,y+2,side(left+11,y)+32),
                (right-10,y-2,side(right-10,y)+32)],2.5,"D8D0D5")
        stroke("Recessed shutter grip",[((left+right)/2-16,bottom-18,side((left+right)/2,bottom)+38),
                ((left+right)/2+16,bottom-18,side((left+right)/2,bottom)+38)],5.5,"524850")
    solid("Inset hose locker",[(368,843),(389,837),(532,838),(546,849),(547,934),(538,951),(371,949),(359,935)],
          lambda x,y:side(x,y)+22,"67332C",6,8,6)
    for i in range(8):
        x=384+i*19
        oval("Reel cheek" if i in (0,7) else "Coiled golden hose",x,894,22,51,
             side(x,894)+36+i*.35,12,SILVER if i in (0,7) else "F5B623")
    for i in range(6):
        x=405+i*15
        stroke("Wound hose catching the light",[(x-4,853,side(x,853)+48),(x,854,side(x,854)+51),
               (x+5,863,side(x,863)+50)],2.2,"FFD273",[.20,1,.25])
    stroke("Curved silver hose nozzle",[(510,895,side(510,895)+63),(520,908,side(520,908)+67),
            (543,932,side(543,932)+74)],12,SILVER,[1.35,1,.77])
    oval("Nozzle opening",543,933,6,6,side(543,933)+81,2,"554C51")
    solid("Amber rear marker",[(91,843),(102,841),(109,847),(109,871),(104,878),(92,875)],
          side(101,860)+27,"FFB51B",5,4)
    silhouette=[(598,811),(603,638),(615,553),(631,530),(667,516),(783,499),(937,504),
          (1042,528),(1071,550),(1094,639),(1107,696),(1116,736),(1118,858),(1109,881),
          (784,891),(765,880),(753,850),(730,821),(692,800),(658,801),(627,817),(598,853)]
    solid("Tall rounded cab casting",silhouette,cab,RED,22,28,165,True)
    side_window=[(644,689),(648,626),(653,580),(667,566),(722,563),(738,574),(751,681),(744,705),(662,701)]
    glazing("Driver door glazing",side_window,lambda x,y:cab(x,y)+21)
    glass=[(777,563),(793,550),(855,544),(1006,545),(1054,556),(1068,575),(1089,672),(1083,691),
           (1005,692),(809,692),(792,682)]
    glazing("Panoramic blue windshield",glass,lambda x,y:cab(x,y)+22)
    front=[(777,713),(797,695),(995,696),(1094,695),(1114,709),(1120,765),(1120,851),
           (1113,867),(1042,875),(796,880),(775,869)]
    nose=solid("Unbroken red nose below windshield",front,cab,RED,32,30,23,True)
    stroke("Driver door seam",[(628,567,side(628,567)+23),(620,694,side(620,694)+23),
          (621,783,side(621,783)+23),(637,792,side(637,792)+23),(673,793,side(673,793)+23)],2.8,CRIMSON)
    stroke("Door handle",[(638,748,side(638,748)+28),(665,748,side(665,748)+28)],6,CRIMSON)
    stroke("Dark front arch",[(583,947,136),(588,888,139),(610,839,145),(656,808,150),(695,813,157),
        (729,841,163),(747,891,170),(762,958,174)],13,"51484E",[.8,1,1,1,1,1,1,.9])
    for x,y,dx in [(724,658,0),(1111,652,-3)]:
        front_d=cab(x,y)+55
        stroke("Mirror mounting arm",[(x+18,y+72,front_d-14),(x+15,y+79,front_d),(x+11,y+37,front_d)],9,"453D41")
        solid("Rounded black mirror",[(x-26,y-43),(x-19,y-54),(x+15,y-54),(x+27,y-41),
             (x+29,y+33),(x+19,y+48),(x-17,y+48),(x-27,y+34)],front_d,"51484D",11,13,22)
    bumper=[(771,894),(783,880),(961,877),(1106,866),(1126,872),(1134,893),(1134,935),
            (1123,950),(1035,961),(796,972),(775,963),(769,943)]
    solid("Ivory bumper face",bumper,lambda x,y:cab(x,y)+34,"F3DECE",19,18,23)
    fit_car_eyes(nose, (852,789,125), (1054,775,86))
    stroke("Fire engine smile",[(924,835,nose.depth(924,835)+4),(947,848,nose.depth(947,848)+5),
          (971,849,nose.depth(971,849)+5),(995,834,nose.depth(995,834)+4)],4.6,"322730",[.65,1,1,.65])
    for x,y in [(777,498),(993,507)]:
        solid("Beacon rubber base",[(x-52,y+3),(x-45,y),(x+45,y+2),(x+55,y+10),(x+51,y+22),(x-52,y+21)],
              82,"443F47",6,5,20)
        solid("Polished cobalt beacon",[(x-46,y),(x-42,y-28),(x-35,y-43),(x-17,y-48),(x+20,y-48),
            (x+36,y-43),(x+43,y-26),(x+46,y)],82,pigment("1576E9",.21,.17),24,14,25)
    solid("Swivelling ladder base",[(148,634),(164,625),(382,625),(395,633),(391,650),(151,658)],72,SILVER,9,10,80)
    solid("Ladder pivot casting",[(161,626),(164,560),(183,530),(213,524),(239,545),(259,588),
          (357,594),(377,604),(381,626)],80,RED,16,13,68,True)
    solid("Rear ladder cradle",[(115,582),(115,557),(165,483),(187,475),(266,460),(271,476),
          (215,508),(203,548),(176,597),(132,604)],36,RED,13,14,38,True)
    for offset,d in [(38,50),(0,100)]:
        def low(x):return 551-(x-193)*.205
        def high(x):return 476-(x-211)*.222
        for j in range(9):
            x=220+j*80
            stroke("Ladder truss diagonal",[(x+offset,low(x),d),(x+32+offset,high(x+32),d)],8.2,SILVER)
            stroke("Ladder rung",[(x,low(x),104),(x+38,low(x)-5,49)],6.4,SILVER)
        stroke("Ladder foot rail",[(193+offset,551,d),(507+offset,487,d),(877+offset,411,d)],11,SILVER)
        stroke("Ladder top rail",[(182+offset,542,d),(211+offset,480,d),(252+offset,467,d),(892+offset,323,d)],11,SILVER)
    solid("Basket levelling knuckle",[(873,351),(895,335),(921,338),(938,352),(956,367),
          (965,391),(947,408),(911,403),(882,388)],64,SILVER,12,9,32)
    oval("Basket hinge",915,368,24,26,97,7,SILVER)
    oval("Basket hinge pin",915,368,14,15,106,4,"A6A1AB")
    oval("Ladder pivot pin",196,573,31,38,117,12,SILVER)
    oval("Pivot cap",193,572,19,24,130,6,"E1D7DC")
    solid("Basket platform",[(935,403),(950,395),(1047,382),(1162,397),(1174,406),(1171,432),(1157,439),
          (1069,452),(1040,450),(941,432)],88,SILVER,13,9,35)
    floor=[(953,407,125),(1048,391,87),(1154,404,68),(1066,425,114)]
    solid("Recessed nonslip rescue basket floor",[(x,y) for x,y,d in floor],
          lambda x,y:115-(x-951)*.24,"9F9DA5",4,2,8)
    for j in range(1,6):
        x=956+j*15
        stroke("Basket floor grating",[(x,406-j*2,131-j*5),(x+85,421-j*2,126-j*5)],1.8,"D3CCD1")
    corners=[(951,275,124),(1054,252,84),(1155,276,65),(1047,306,112)]
    for x,y,d in corners:
        stroke("Basket safety upright",[(x,y,d),(x,y+122,d)],8.5,SILVER)
    for drop,r in [(0,12),(65,6.8)]:
        for j,(x,y,d) in enumerate(corners):
            xx,yy,dd=corners[(j+1)%4]
            stroke("Rounded basket rail",[(x,y+drop,d),(xx,yy+drop,dd)],r,SILVER)
    finish("car-model-fire-engine")

