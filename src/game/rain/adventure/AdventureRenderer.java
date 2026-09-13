package game.rain.adventure;

import game.rain.adventure.Actors.*;
import game.rain.graphics.Sprite;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.IdentityHashMap;
import java.util.Map;

/** Pixel-scaled world rendering, animation, journal and campaign UI. */
public final class AdventureRenderer {
    public static final int WIDTH=480,HEIGHT=270;
    private static final Color INK=new Color(0x0c192c),PAPER=new Color(0xfff4d6),GOLD=new Color(0xffd34e),MUTED=new Color(0xb6d6d8);
    private static final Font SMALL=new Font(Font.MONOSPACED,Font.PLAIN,8),TEXT=new Font(Font.SANS_SERIF,Font.PLAIN,9);
    private static final Font TITLE=new Font(Font.SERIF,Font.BOLD,16);
    private final BufferedImage image=new BufferedImage(WIDTH,HEIGHT,BufferedImage.TYPE_INT_RGB);
    private final Map<Sprite,BufferedImage> sprites=new IdentityHashMap<>();
    public boolean reducedMotion,confirmNew;
    private Graphics2D g;
    private Adventure a;
    private int camX,camY;

    public BufferedImage render(Adventure adventure,boolean menu,boolean paused,boolean atlas,boolean journal,boolean muted,boolean focused) {
        a=adventure;g=image.createGraphics();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        g.setFont(SMALL);camX=(int)Math.max(0,Math.min(WorldMap.WIDTH*16-WIDTH,a.x-WIDTH/2));
        camY=(int)Math.max(0,Math.min(WorldMap.HEIGHT*16-HEIGHT,a.y-HEIGHT/2));
        if(!reducedMotion&&a.shake>0){camX+=(int)(Math.sin(a.tick*2.3)*Math.min(3,a.shake));camY+=(int)(Math.cos(a.tick*1.7)*Math.min(2,a.shake));}
        drawWorld();
        if(menu)drawMenu();else{drawHud(muted);if(a.dead)drawDeath();else if(atlas)drawAtlas();else if(journal)drawJournal();else if(paused||!focused)drawPause(paused);}
        g.dispose();return image;
    }
    private void drawWorld(){
        g.setColor(new Color(a.world.area.dark));g.fillRect(0,0,WIDTH,HEIGHT);
        for(int y=Math.max(0,camY/16);y<=Math.min(31,(camY+HEIGHT)/16);y++)for(int x=Math.max(0,camX/16);x<=Math.min(47,(camX+WIDTH)/16);x++)tile(x,y);
        for(Patch p:a.patches){int x=sx(p.x),y=sy(p.y);g.setColor(new Color(p.fire?0xbb693e:0x6dbb48));g.fillOval(x-15,y-6,30,12);g.setColor(new Color(p.fire?0xffe171:0xa9bb79));for(int i=0;i<4;i++)g.fillRect(x-10+i*6,y-(p.fire?(a.tick+i)%7:1),2,2);}
        architecture();
        for(Portal p:a.world.portals)portal(p);
        for(Landmark l:a.world.landmarks)landmark(l);
        for(Prop p:a.world.props)prop(p);
        for(Loot l:a.loot)loot(l);
        for(Enemy e:a.world.enemies)if(!e.dead)enemy(e);
        hero();
        for(Shot s:a.shots)shot(s);
        if(a.crown!=null)crown(sx(a.crown.x),sy(a.crown.y)-4,a.tick*.35,1.0);
        for(Particle p:a.particles){g.setColor(new Color(p.color));int size=Math.max(1,p.size*p.life/p.total);g.fillRect(sx(p.x)-size/2,sy(p.y)-size/2,size,size);}
        ambient();
        if(a.world.area==Area.KEEP||a.world.area==Area.CRYPT||a.world.area==Area.THRONE){g.setColor(new Color(15,20,37,35));g.fillRect(0,0,WIDTH,HEIGHT);}
        if(a.victoryTime>0){g.setColor(new Color(240,221,164,Math.min(75,a.victoryTime/3)));g.fillRect(0,0,WIDTH,HEIGHT);}
    }
    private int sx(double value){return (int)Math.round(value)-camX;}
    private int sy(double value){return (int)Math.round(value)-camY;}
    private Color tone(int color,int delta){return new Color(Math.max(0,Math.min(255,((color>>16)&255)+delta)),Math.max(0,Math.min(255,((color>>8)&255)+delta)),Math.max(0,Math.min(255,(color&255)+delta)));}
    private void tile(int tx,int ty){
        int tile=a.world.tile(tx,ty),x=tx*16-camX,y=ty*16-camY;int h=Math.floorMod(tx*71+ty*37,13);
        int base=a.world.area.ground;
        if(tile==WorldMap.WALL){g.setColor(tone(a.world.area.dark,-10));g.fillRect(x,y,16,20);g.setColor(tone(base,-14));g.fillRect(x,y-6,16,17);g.setColor(tone(base,22));g.fillRect(x,y-6,16,2);g.setColor(tone(base,-30));g.drawLine(x,y+2,x+15,y+2);g.drawLine(x+((ty%2)*8),y-5,x+((ty%2)*8),y+1);return;}
        if(tile==WorldMap.WATER){
            boolean mud=a.world.area==Area.MARSH&&a.waterLevel==0&&!a.progress.has("won");
            int water=a.progress.has("won")?0x21c8c3:0x167db7;g.setColor(tone(mud?0x625f47:water,h-6));g.fillRect(x,y,16,16);
            g.setColor(tone(mud?0x756b4e:water,20));int wave=(a.tick/10+tx*3+ty)%13;
            g.fillRect(x+wave/3,y+4,5,1);g.fillRect(x+8-wave/4,y+11,6,1);return;
        }
        if(tile==WorldMap.GATE){
            g.setColor(tone(base,-8));g.fillRect(x,y,16,16);
            if(a.solid(tx*16+8,ty*16+8)){g.setColor(new Color(0xa39879));for(int i=2;i<16;i+=4)g.fillRect(x+i,y-10,2,26);g.setColor(GOLD);g.fillRect(x,y-3,16,3);}
            else{g.setColor(new Color(0xb6aa7f));g.fillRect(x,y+14,16,2);}return;
        }
        if(tile==WorldMap.WOOD){g.setColor(new Color(0xb98143));g.fillRect(x,y,16,16);g.setColor(new Color(0x5e5442));for(int i=0;i<16;i+=4)g.drawLine(x,y+i,x+15,y+i);g.fillRect(x+2,y+1,1,1);return;}
        if(tile==WorldMap.PATH)base=a.world.area.ordinal()<3?0xd2ae69:0x77756d;
        if(tile==WorldMap.STONE||tile==WorldMap.CRACK){
            base=a.world.area==Area.CASTLE?0xabb8b9:a.world.area.ground;g.setColor(tone(base,h-8));g.fillRect(x,y,16,16);
            g.setColor(tone(base,-14));g.drawLine(x,y+15,x+15,y+15);g.drawLine(x+15,y,x+15,y+15);
            g.setColor(tone(base,9));g.drawLine(x+1,y+1,x+14,y+1);
        }else{g.setColor(tone(base,h-6));g.fillRect(x,y,16,16);g.setColor(tone(base,13));g.fillRect(x+h,y+4,2,1);g.fillRect(x+3,y+h,1,2);}
        if(tile==WorldMap.CRACK){g.setColor(new Color(0x2b3540));g.drawLine(x+3,y+2,x+8,y+7);g.drawLine(x+8,y+7,x+5,y+12);g.drawLine(x+8,y+7,x+13,y+9);}
    }
    private void architecture(){
        if(a.world.area==Area.CASTLE||a.world.area==Area.KEEP){
            for(int bx:new int[]{292,492}){int xx=sx(bx),yy=sy(185);g.setColor(new Color(0xe6c27c));g.fillRect(xx,yy-56,2,58);
                int wave=(int)(Math.sin(a.tick*.04+bx)*3);g.setColor(new Color(a.world.area==Area.CASTLE?0xde3e6b:0xa15fe1));
                g.fillPolygon(new int[]{xx+2,xx+22,xx+20+wave,xx+2},new int[]{yy-54,yy-51+wave,yy-29,yy-34},4);
                g.setColor(GOLD);g.fillRect(xx+9,yy-45,5,6);}
        }
        if(a.world.area==Area.CASTLE){
            building(168,176,"FORGE",a.progress.has("forge"),0x775147);
            building(600,176,"GARDEN",a.progress.has("garden"),0x56a853);
            building(200,360,"BELL TOWER",a.progress.has("tower"),0x727f82);
            int tx=sx(392),ty=sy(112);g.setColor(new Color(0xa83b68));g.fillRect(tx-18,ty-19,36,32);g.setColor(GOLD);g.drawRect(tx-18,ty-19,36,32);
            for(int i=0;i<3;i++)g.fillRect(tx-14+i*12,ty-26,5,9);
            if(a.progress.has("garden")){for(int i=0;i<18;i++){int xx=sx(555+i%6*16),yy=sy(220+i/6*13);g.setColor(new Color(0x69825c));g.fillRect(xx,yy,1,6);g.setColor(new Color(i%2==0?0xd49b8c:0xdfcb82));g.fillRect(xx-2,yy-2,5,3);}}
        }
        if(a.world.area==Area.MEADOW&&!a.progress.has("bells")){
            int x=sx(360),y=sy(376+Math.sin(a.tick*.035)*33);g.setColor(new Color(0x61585b));g.fillRect(x-5,y-10,10,20);g.setColor(new Color(0xbcaa7d));g.fillRect(x-5,y-10,10,2);
        }
        if(a.world.area==Area.MARSH){
            int x=sx(568),y=sy(249);g.setColor(new Color(0x382f2e));g.fillOval(x-17,y-17,34,34);g.setColor(new Color(0xc1a272));g.setStroke(new BasicStroke(3));
            double spin=a.waterLevel==2||a.progress.has("won")?a.tick*.025:0;
            for(int i=0;i<8;i++){double angle=spin+i*Math.PI/4;g.drawLine(x,y,x+(int)(Math.cos(angle)*16),y+(int)(Math.sin(angle)*16));}g.setStroke(new BasicStroke(1));
        }
        if(a.world.area==Area.TEMPLE){
            int[][] pos={{264,328},{392,200},{520,328}};int[] dx={0,1,0,-1},dy={-1,0,1,0};
            for(int i=0;i<3;i++){int d=a.statues[i],x=sx(pos[i][0]),y=sy(pos[i][1]);
                g.setColor(new Color(242,211,127,65));g.setStroke(new BasicStroke(7));g.drawLine(x,y,x+dx[d]*128,y+dy[d]*128);
                g.setStroke(new BasicStroke(1));g.setColor(new Color(0xf8df9a));g.drawLine(x,y,x+dx[d]*128,y+dy[d]*128);
                g.setColor(new Color(0xbdb294));g.fillRect(x-7,y-12,14,14);g.setColor(GOLD);g.fillRect(x-3+dx[d]*4,y-7+dy[d]*4,5,5);
            }
        }
        if(a.world.area==Area.KEEP){
            int x=sx(168),y=sy(65);g.setColor(new Color(239,204,116,60));g.fillRect(x-13,y,26,125);
            if(a.shadowTime>0){g.setColor(new Color(20,26,39,160));g.fillRect(x-13,sy(a.y),26,75);}
            for(int j=0;j<5;j++)for(int i=0;i<4;i++){int xx=sx(154+i*16),yy=sy(85+j*18);g.setColor(new Color(a.shadowTime>0?0x535568:0xb9b7bd));g.fillPolygon(new int[]{xx,xx+4,xx+8},new int[]{yy+5,yy-(a.shadowTime>0?0:5),yy+5},3);}
        }
        if(a.world.area==Area.THRONE){int x=sx(392),y=sy(96);g.setColor(new Color(0x793e55));g.fillRect(x-30,y-30,60,40);g.setColor(GOLD);g.drawRect(x-30,y-30,60,40);crown(x,y-27,0,1.4);}
    }
    private void building(int wx,int wy,String name,boolean restored,int color){
        int x=sx(wx),y=sy(wy);g.setColor(new Color(0x26302e));g.fillRect(x-30,y-20,64,38);g.setColor(tone(color,restored?15:-10));g.fillRect(x-29,y-47,60,56);
        g.setColor(tone(color,-24));g.fillPolygon(new int[]{x-36,x,x+37},new int[]{y-44,y-66,y-44},3);
        g.setColor(new Color(restored?0xe4b679:0x263536));g.fillRect(x-19,y-31,12,17);g.fillRect(x+9,y-31,12,17);
        g.setColor(new Color(0x343a34));g.fillRect(x-5,y-14,12,23);g.setColor(MUTED);center(name,x,y+22,SMALL);
        if(!restored){g.setColor(new Color(0x333f39));g.drawLine(x+10,y-58,x+20,y-43);g.drawLine(x-18,y-16,x+17,y+1);}
        else if(name.equals("FORGE")){g.setColor(new Color(0xd4ac73));g.fillRect(x+23,y-65,8,20);for(int i=0;i<3;i++){g.setColor(new Color(176,175,160,100-i*25));g.fillRect(x+24+(a.tick/8+i*2)%8,y-72-i*10,7+i*3,7+i*3);}}
        else if(name.equals("BELL TOWER")){g.setColor(new Color(0x69787a));g.fillRect(x-10,y-86,24,36);g.setColor(GOLD);g.fillOval(x-4,y-78,12,14);g.fillRect(x-7,y-66,18,3);}
    }
    private void portal(Portal p){int x=sx(p.x()),y=sy(p.y());
        g.setColor(new Color(0x203a39));g.fillOval(x-17,y-5,34,12);g.setColor(new Color(0xd9c08a));g.drawOval(x-17,y-5,34,12);
        g.fillPolygon(new int[]{x-4,x,x+4},new int[]{y-12,y-16,y-12},3);
        if(Adventure.distance(a.x,a.y,p.x(),p.y())<95){g.setColor(PAPER);center(p.label(),x,y-22,SMALL);}
    }
    private boolean taken(String id){return a.progress.has(id)||switch(id){case "rescueSmith"->a.progress.has("smithRescued");case "rescueCook"->a.progress.has("cookRescued");case "rescueMap"->a.progress.has("mapRescued");default->false;};}
    private void landmark(Landmark l){int x=sx(l.x()),y=sy(l.y());String id=l.id();
        if(id.startsWith("statue"))return;
        if(id.startsWith("bell")&&!id.equals("bellHint")){int i=id.charAt(4)-'0';boolean active=a.progress.has("bells")||a.bellStep>i;
            g.setColor(new Color(0x4c5146));g.fillRect(x-9,y-20,3,22);g.fillRect(x+7,y-20,3,22);g.fillRect(x-9,y-21,19,3);
            g.setColor(active?GOLD:new Color(0x93886b));g.fillOval(x-5,y-17,11,11);g.fillRect(x-6,y-9,13,3);g.setColor(PAPER);center(""+(i+1),x,y+10,SMALL);return;}
        if(id.startsWith("rescue")){
            if(taken(id))return;npc(x,y,0xa9bbae);
            g.setColor(new Color(0x485d5c));g.drawRect(x-12,y-29,25,35);for(int i=-8;i<=8;i+=8)g.fillRect(x+i,y-28,1,34);
            g.setColor(GOLD);center("!",x,y-36,SMALL);return;
        }
        if(id.equals("mentor")){npc(x,y,0x9fa9ba);g.setColor(GOLD);center("SIR BRAMBLE",x,y-34,SMALL);return;}
        if(id.equals("smith")||id.equals("cook")||id.equals("cartographer")){
            boolean home=a.progress.has(id.equals("smith")?"smithRescued":id.equals("cook")?"cookRescued":"mapRescued");
            if(home)npc(x,y,id.equals("smith")?0xbc9077:id.equals("cook")?0xdbd0b0:0x99b6b9);
            return;
        }
        if(id.equals("ruby")||id.equals("emerald")||id.equals("sapphire")||id.contains("Cache")||id.equals("cryptTreasure")){
            boolean open=taken(id)||id.equals("cryptTreasure")&&a.progress.has("cryptHeart");
            g.setColor(new Color(0x53413b));g.fillRect(x-9,y-9,19,12);g.setColor(new Color(open?0x827552:0xc0985e));g.fillRect(x-9,y-(open?15:12),19,6);g.setColor(GOLD);g.fillRect(x-1,y-8,3,4);
            if(!open&&(id.equals("ruby")||id.equals("emerald")||id.equals("sapphire"))){g.setColor(new Color(id.equals("ruby")?0xe69079:id.equals("emerald")?0x94c78d:0x8db9dc));int bob=(int)Math.sin(a.tick*.06)*2;g.fillPolygon(new int[]{x,x+4,x,x-4},new int[]{y-24+bob,y-20+bob,y-15+bob,y-20+bob},4);}
            return;
        }
        if(id.equals("fountain")){g.setColor(new Color(0x657e7b));g.fillOval(x-23,y-12,46,26);g.setColor(new Color(0x80b3b4));g.fillOval(x-18,y-10,36,18);g.setColor(new Color(0xc4d8c4));g.drawOval(x-13+(a.tick/10)%4,y-8,23,11);g.fillRect(x-4,y-28,8,20);g.fillRect(x-8,y-29,16,4);return;}
        if(id.equals("waterLever")||id.equals("release")){g.setColor(new Color(0x515b56));g.fillRect(x-8,y-4,16,9);g.setColor(GOLD);g.setStroke(new BasicStroke(3));g.drawLine(x,y,x+(a.waterLevel-1)*7,y-16);g.setStroke(new BasicStroke(1));return;}
        if(id.equals("pedestal")){g.setColor(new Color(0x9593a1));g.fillRect(x-10,y-6,20,12);g.setColor(new Color(0xd2bf8f));g.fillRect(x-12,y-9,24,4);if(a.crownResting)crown(x,y-17,0,1);return;}
        if(id.equals("sunSeal")||id.equals("heavyPlate")){boolean on=a.progress.has(id.equals("sunSeal")?"light":"heavyPlate");g.setColor(on?GOLD:new Color(0x747674));g.drawOval(x-14,y-7,28,14);g.drawOval(x-10,y-5,20,10);if(id.equals("sunSeal")&&a.lightCharge>0)g.fillRect(x-14,y+10,a.lightCharge*28/90,2);return;}
        if(id.equals("hollowFloor")){if(a.progress.has("crypt")){g.setColor(new Color(0x172936));g.fillRect(x-8,y-8,16,16);g.setColor(new Color(0x6f898d));for(int i=0;i<4;i++)g.fillRect(x-7+i,y-6+i*4,13-i*2,1);}return;}
        if(id.equals("arrowBell")||id.equals("archerBell")){g.setColor(new Color(0x66533f));g.fillRect(x-2,y-20,4,24);g.setColor(GOLD);g.fillOval(x-7,y-24,14,12);return;}
        if(id.equals("decree")||id.equals("victoryThrone"))return;
        g.setColor(new Color(0x685c44));g.fillRect(x-1,y-12,3,17);g.setColor(new Color(0xb7a175));g.fillRect(x-10,y-18,21,10);
    }
    private void npc(int x,int y,int color){shadow(x,y,13);g.setColor(new Color(0x253435));g.fillRect(x-5,y-4,4,6);g.fillRect(x+2,y-4,4,6);g.setColor(new Color(color));g.fillRect(x-7,y-17,14,15);g.setColor(new Color(0xd1b68e));g.fillRect(x-5,y-26,10,10);g.setColor(new Color(0x3c3835));g.fillRect(x-6,y-29,12,5);g.fillRect(x-3,y-22,1,1);g.fillRect(x+3,y-22,1,1);}
    private void prop(Prop p){int x=sx(p.x),y=sy(p.y);if(p.broken){if(p.kind!=PropKind.GRASS){g.setColor(new Color(0x6b6850));g.fillRect(x-6,y,5,2);g.fillRect(x+3,y-2,3,3);}return;}
        switch(p.kind){
            case GRASS->{g.setColor(new Color(0x849855));int bend=p.bend>0?3:0;for(int i=0;i<3;i++)g.drawLine(x-4+i*4,y+2,x-6+i*4+bend,y-5+(i%2)*2);}
            case POT->{shadow(x,y,10);g.setColor(new Color(0xb38464));g.fillOval(x-6,y-10,13,13);g.setColor(new Color(0xdbb08a));g.fillRect(x-6,y-12,13,4);g.setColor(new Color(0x665143));g.fillRect(x-4,y-12,9,2);}
            case FENCE,WOOD->{g.setColor(new Color(0x8a7054));g.fillRect(x-7,y-17,3,22);g.fillRect(x+5,y-17,3,22);g.fillRect(x-8,y-12,17,3);g.fillRect(x-8,y-4,17,3);}
            case CRACKED_WALL->{g.setColor(new Color(0x929087));g.fillRect(x-8,y-24,16,29);g.setColor(new Color(0xc0b596));g.fillRect(x-9,y-25,18,3);g.setColor(new Color(0x535b5a));g.drawLine(x+1,y-21,x-3,y-12);g.drawLine(x-3,y-12,x+3,y-5);g.drawLine(x+3,y-5,x+1,y+3);}
            case SIGN->{g.setColor(new Color(0xa5916b));g.fillRect(x-10,y-18,20,10);g.fillRect(x-1,y-8,3,12);}
            case ROPE->{g.setColor(new Color(0xddc79a));g.drawLine(x-13,y-20,x+13,y-20);g.drawLine(x,y-20,x,y+5);g.setColor(new Color(0x82674d));g.fillRect(x-15,y-22,3,30);g.fillRect(x+13,y-22,3,30);}
        }
        if(p.burning>0){g.setColor(new Color(0xf5b45f));for(int i=0;i<3;i++)g.fillRect(x-5+i*4,y-6-(a.tick+i*4)%10,3,7);}
    }
    private void shadow(int x,int y,int width){g.setColor(new Color(16,28,31,90));g.fillOval(x-width/2,y-2,width,5);}
    private void loot(Loot item){if(item.value<=0)return;int x=sx(item.x),y=sy(item.y)-(int)(Math.sin(item.age*.1)*2)-4;shadow(x,y+5,7);g.setColor(item.heart?new Color(0xd58288):GOLD);if(item.heart)heart(x,y,true);else{g.fillOval(x-3,y-4,6,8);g.setColor(PAPER);g.fillRect(x-1,y-2,1,4);}}
    private void enemy(Enemy e){
        int x=sx(e.x),y=sy(e.y);if(x<-40||y<-50||x>WIDTH+40||y>HEIGHT+50)return;int bob=e.windup>0?(e.age%4==0?1:-1):0;
        shadow(x,y,(int)e.radius()*2);
        if(e.windup>0){g.setColor(new Color(226,137,112,100));if(e.kind==Kind.BOMB_SLIME||e.kind==Kind.BOSS&&e.stolen%3==2)g.drawOval(x-55,y-35,110,70);
            else g.drawLine(x,y,x+(int)(e.aimX*110),y+(int)(e.aimY*110));g.setColor(new Color(0xffc182));center("!",x,y-37,SMALL);}
        if(e.kind==Kind.KNIGHT){g.setColor(new Color(e.flash>0?0xffefcb:0x75858a));g.fillRect(x-7,y-21+bob,15,20);g.fillRect(x-8,y-29+bob,17,12);g.setColor(new Color(0x26363e));g.fillRect(x-6,y-24+bob,13,3);g.fillRect(x-6,y,4,4);g.fillRect(x+3,y,4,4);g.setColor(new Color(0xccb58a));int shield=(int)(e.aimX*10);g.fillRect(x+shield-4,y-15,9,14);g.setColor(new Color(0x454957));g.fillRect(x+shield-1,y-12,3,8);}
        else if(e.kind==Kind.GOBLIN){g.setColor(new Color(e.flash>0?0xffefcb:0x87a779));g.fillRect(x-7,y-23+bob,14,11);g.fillRect(x-11,y-22+bob,4,5);g.fillRect(x+7,y-22+bob,4,5);g.setColor(new Color(0x665a4d));g.fillRect(x-6,y-12,12,12);g.setColor(new Color(0x263a34));g.fillRect(x-4,y-19,2,2);g.fillRect(x+3,y-19,2,2);if(e.stolen>0){g.setColor(GOLD);g.fillOval(x+6,y-5,6,7);}}
        else if(e.kind==Kind.ARCHER||e.kind==Kind.EMBER){g.setColor(new Color(0xc8bba1));g.fillRect(x-5,y-16,10,17);g.setColor(new Color(e.flash>0?0xffefcb:e.kind==Kind.EMBER?0xd77852:0xb8907a));g.fillOval(x-13,y-32+bob,26,20);g.fillRect(x-14,y-21+bob,28,5);g.setColor(new Color(0xe4d2a9));g.fillRect(x-7,y-28+bob,4,3);g.fillRect(x+5,y-23+bob,3,3);g.setColor(new Color(0x2b3435));g.fillRect(x-3,y-12,2,2);g.fillRect(x+2,y-12,2,2);g.setColor(new Color(0xc7a272));g.drawArc(x+7,y-18,12,18,-80,160);}
        else if(e.kind==Kind.BOSS){
            g.setColor(new Color(e.flash>0?0xffeec4:0x624460));g.fillPolygon(new int[]{x-19,x+19,x+28,x-28},new int[]{y-28,y-28,y+2,y+2},4);
            g.setColor(new Color(0x999ba3));g.fillRect(x-15,y-38,30,33);g.setColor(new Color(0xceba98));g.fillRect(x-11,y-48,22,17);g.setColor(new Color(0x302c3d));g.fillRect(x-8,y-40,5,3);g.fillRect(x+3,y-40,5,3);
            crown(x,y-51,0,2);g.setColor(GOLD);g.fillRect(x-2,y-23,4,12);g.fillRect(x-6,y-19,12,4);
        }
        else if(e.kind==Kind.CHICKEN){g.setColor(PAPER);g.fillOval(x-7,y-12,14,11);g.fillRect(x+3,y-18,7,9);g.setColor(new Color(0xd17769));g.fillRect(x+4,y-21,4,4);g.setColor(GOLD);g.fillRect(x+9,y-14,4,3);}
        else {int color=switch(e.kind){case STICKY_SLIME->0xa3b768;case BOMB_SLIME->0xc09260;case LARGE_SLIME->0x80a992;default->0x91b1a0;};int size=e.kind==Kind.LARGE_SLIME?27:18;
            int jump=e.age%62<24?(int)(Math.sin(e.age%62/24.0*Math.PI)*6):0;
            int height=size*2/3+(e.windup>0?-3:0);g.setColor(new Color(e.flash>0?0xffefcb:color));g.fillOval(x-size/2,y-height-jump+bob,size,height);g.fillRect(x-size/2+3,y-5-jump,size-6,5);
            g.setColor(tone(color,30));g.fillRect(x-size/3,y-height+3-jump,5,2);g.setColor(new Color(0x293f3f));g.fillRect(x-4,y-8-jump,2,3);g.fillRect(x+3,y-8-jump,2,3);
            if(e.kind==Kind.BOMB_SLIME){g.setColor(new Color(0xe4c285));g.drawLine(x,y-height-jump,x+3,y-height-6-jump);}
        }
        if(e.hp<e.maxHp&&e.kind!=Kind.BOSS){g.setColor(new Color(0x293138));g.fillRect(x-10,y+7,20,2);g.setColor(new Color(0xd79d83));g.fillRect(x-10,y+7,Math.max(0,20*e.hp/e.maxHp),2);}
        if(e.stun>10){g.setColor(GOLD);g.fillRect(x-11+(e.age/5)%20,y-35,2,2);}
        if(e.burn>0){g.setColor(new Color(0xf3b961));g.fillRect(x-5,y-12-e.age%5,3,6);}
    }
    private void hero(){
        int x=sx(a.x),y=sy(a.y);int lift=a.slamTime>4?(int)(Math.sin((22-a.slamTime)/18.0*Math.PI)*26):0;
        shadow(x,y,18);if(a.hurtTime>0&&a.hurtTime%8<3&&a.hurtTime<60)return;
        int sway=(int)(Math.sin(a.tick*(a.moving?.24:.05))*(a.moving?3:1));
        if(a.world.area==Area.KEEP&&Adventure.distance(a.x,a.y,568,376)<80)sway+=3;
        g.setColor(new Color(0xe83263));g.fillPolygon(new int[]{x-6,x+5,x+(int)a.capeX+7+sway,x+(int)a.capeX-7+sway},new int[]{y-19-lift,y-19-lift,y+(int)a.capeY-lift,y+(int)a.capeY-lift},4);
        boolean side=Math.abs(a.faceX)>Math.abs(a.faceY);int anim=a.moving?(a.tick/8)%3:0;
        Sprite sprite=side?(anim==0?Sprite.player_side:anim==1?Sprite.player_side_1:Sprite.player_side_2):a.faceY<0?(anim==0?Sprite.player_up:anim==1?Sprite.player_up_1:Sprite.player_up_2):(anim==0?Sprite.player_down:anim==1?Sprite.player_down_1:Sprite.player_down_2);
        BufferedImage player=sprite(sprite);
        if(side&&a.faceX<0)g.drawImage(player,x+16,y-27-lift,-32,32,null);else g.drawImage(player,x-16,y-27-lift,null);
        if(a.crown==null&&!a.crownResting)crown(x,y-28-lift,!a.moving&&a.tick%900>830?Math.sin(a.tick*.2)*.15:0,.65);
        if(a.attackTime>0){double angle=Math.atan2(a.faceY,a.faceX);double sweep=(15-a.attackTime)/15.0*2.7-1.35;
            int reach=a.combo==3?39:32;g.setColor(new Color(a.empowered>0?0xffe8a4:0xe6e5cd));g.setStroke(new BasicStroke(a.combo==3?3:2));
            g.drawLine(x+(int)(Math.cos(angle+sweep)*12),y-9+(int)(Math.sin(angle+sweep)*12),x+(int)(Math.cos(angle+sweep)*reach),y-9+(int)(Math.sin(angle+sweep)*reach));
            g.setColor(new Color(240,223,171,110));g.drawArc(x-reach,y-9-reach,reach*2,reach*2,(int)(-Math.toDegrees(angle)-70),140);g.setStroke(new BasicStroke(1));}
        if(a.slamTime>0&&a.slamTime<=4){g.setColor(GOLD);int r=(5-a.slamTime)*16;g.drawOval(x-r,y-r/2,r*2,r);}
        if(a.empowered>0){g.setColor(GOLD);g.drawOval(x-13,y-28,26,30);}
        if(a.catchFlash>0){g.setColor(GOLD);center("ROYAL CATCH",x,y-42,SMALL);}
        if(!a.moving&&a.tick%1200>1060&&a.attackTime==0){g.setColor(PAPER);center(a.tick%1200>1140?"z Z":"...",x+13,y-39-(a.tick/15)%5,SMALL);}
    }
    private BufferedImage sprite(Sprite sprite){return sprites.computeIfAbsent(sprite,s->{BufferedImage result=new BufferedImage(s.SIZE,s.SIZE,BufferedImage.TYPE_INT_ARGB);for(int i=0;i<s.pixels.length;i++)result.setRGB(i%s.SIZE,i/s.SIZE,s.pixels[i]==0xffff00ff?0:s.pixels[i]);return result;});}
    private void crown(int x,int y,double angle,double scale){var old=g.getTransform();g.translate(x,y);g.rotate(angle);g.scale(scale,scale);g.setColor(new Color(0x78572f));g.fillRect(-8,0,16,6);g.setColor(GOLD);g.fillPolygon(new int[]{-8,-9,-3,0,3,9,8},new int[]{4,-7,-3,-9,-3,-7,4},7);g.setColor(PAPER);g.fillRect(-6,1,12,2);g.setColor(new Color(0xb95a69));g.fillRect(-1,-2,3,3);g.setTransform(old);}
    private void shot(Shot s){int x=sx(s.x),y=sy(s.y);if(s.royal){crown(x,y,s.vx==0&&s.vy==0?0:a.tick*.17,1.6);return;}g.setColor(new Color(s.fire?0xf7b364:0xd4c29a));g.drawLine(x,y,x-(int)(s.vx*4),y-(int)(s.vy*4));g.fillRect(x-1,y-1,3,3);}
    private void ambient(){
        if(a.world.area.ordinal()<3){
            for(int i=0;i<9;i++){double wx=75+i*77,wy=80+(i*119)%350;boolean startled=Adventure.distance(a.x,a.y,wx,wy)<70;
                int x=sx(wx+(startled?(a.tick%50)*2:Math.sin(a.tick*.015+i)*5)),y=sy(wy-(startled?a.tick%50:0));
                g.setColor(new Color(0xd1d0ae));int wing=(a.tick/6+i)%2==0?2:-1;g.drawLine(x-3,y+wing,x,y);g.drawLine(x,y,x+3,y+wing);
            }
            if(a.world.area==Area.MARSH){for(int i=0;i<4;i++){double wx=210+i*77,wy=350;int jump=(int)Math.max(0,60-Adventure.distance(a.x,a.y,wx,wy))/3;g.setColor(new Color(0x9bac6d));g.fillRect(sx(wx),sy(wy)-jump,5,3);}}
            if(a.progress.has("garden")&&a.world.area==Area.CASTLE)for(int i=0;i<8;i++){int x=sx(575+Math.sin(a.tick*.02+i)*35),y=sy(215+Math.cos(a.tick*.025+i)*28);g.setColor(new Color(0xe4c99d));g.fillRect(x-2,y,2,2);g.fillRect(x+1,y,2,2);}
        }
        if(a.world.area==Area.KEEP||a.world.area==Area.CRYPT)for(int i=0;i<7;i++){int x=sx(568+Math.sin(a.tick*.025+i)*26),y=sy(366+Math.cos(a.tick*.021+i)*17);g.setColor(new Color(0xb2c5a0));g.fillRect(x,y,1,1);}
        int[][] torches={{80,80},{688,80},{80,432},{688,432}};
        if(a.world.area.ordinal()>=3||a.world.area==Area.CASTLE)for(int[] t:torches){int x=sx(t[0]),y=sy(t[1]);g.setColor(new Color(240,171,85,13));g.fillOval(x-25,y-35,50,50);g.setColor(new Color(0x55473c));g.fillRect(x-2,y-5,4,13);g.setColor(new Color(0xeeb768));g.fillRect(x-3,y-12,6,9);g.setColor(new Color(0xfce1a2));g.fillRect(x-1,y-14-(a.tick/4)%3,3,8);}
    }
    private void panel(int x,int y,int w,int h){g.setColor(new Color(16,29,37,238));g.fillRect(x,y,w,h);g.setColor(new Color(0x69766e));g.drawRect(x,y,w-1,h-1);g.setColor(new Color(0x273e43));g.drawRect(x+2,y+2,w-5,h-5);}
    private void drawHud(boolean muted){
        g.setColor(INK);g.fillRect(0,0,WIDTH,31);g.setColor(new Color(0x61766b));g.fillRect(0,30,WIDTH,1);
        for(int i=0;i<a.progress.maxHealth()/2;i++)heart(13+i*10,10,a.health>=i*2+2);
        if(a.health%2==1){g.setColor(new Color(0xd28586));g.fillRect(10+(a.health/2)*10,8,3,4);}
        g.setColor(new Color(0x384c4c));g.fillRect(10,20,86,3);g.setColor(new Color(0x9fbd9b));g.fillRect(10,20,(int)(a.stamina*.86),3);
        g.setColor(PAPER);center(a.world.area.title,WIDTH/2,12,new Font(Font.SERIF,Font.BOLD,12));
        g.setColor(MUTED);center(a.progress.has("won")?"THE KINGDOM REAWAKENS":"THE STOLEN CROWN",WIDTH/2,23,SMALL);
        g.setColor(GOLD);text("● "+a.progress.coins,380,12,SMALL);g.setColor(MUTED);text(muted?"F: MUTED":"F: SOUND",425,12,SMALL);
        text("U "+jewelShort(a.progress.slotA)+"  I "+jewelShort(a.progress.slotB),375,23,SMALL);
        miniMap();
        if(a.areaAge<180){panel(132,44,216,35);g.setColor(GOLD);center(a.world.area.title,240,59,TITLE);g.setColor(MUTED);center(a.world.area.subtitle,240,71,SMALL);}
        for(Enemy e:a.world.enemies)if(!e.dead&&e.kind==Kind.BOSS){panel(132,177,216,22);g.setColor(PAPER);center("THE PRETENDER  /  PHASE "+e.phase,240,187,SMALL);g.setColor(new Color(0x503b49));g.fillRect(142,191,196,3);g.setColor(new Color(0xce917f));g.fillRect(142,191,196*e.hp/e.maxHp,3);}
        if(a.foodTime>0){g.setColor(GOLD);text("HEARTY MEAL "+a.foodTime/60+"s",10,43,SMALL);}
        if(a.killChain>1){g.setColor(GOLD);text(a.killChain+" DEFEATED",10,55,SMALL);}
        if(a.toastTime>0){panel(10,204,460,41);g.setColor(GOLD);text(a.speaker,19,215,SMALL);g.setColor(PAPER);wrap(a.toast,19,226,443,10,TEXT);}
        g.setColor(INK);g.fillRect(0,249,WIDTH,21);g.setColor(new Color(0x4d6963));g.fillRect(0,249,WIDTH,1);
        String prompt=a.prompt();g.setColor(prompt.isEmpty()?MUTED:GOLD);text(prompt.isEmpty()?a.objective():prompt,10,259,SMALL);
        g.setColor(MUTED);text("J slash  Q crown  SPACE dodge  X slam  E use  M atlas  H journal",10,267,new Font(Font.MONOSPACED,Font.PLAIN,7));
    }
    private String jewelShort(int value){return new String[]{"—","RUBY","EMER","SAPP"}[value];}
    private void heart(int x,int y,boolean full){g.setColor(full?new Color(0xd58d91):new Color(0x4e4b56));g.fillRect(x-3,y-2,3,4);g.fillRect(x+1,y-2,3,4);g.fillRect(x-2,y+2,5,2);g.fillRect(x,y+4,1,1);}
    private void miniMap(){int ox=407,oy=43;panel(ox-3,oy-3,66,46);
        for(int y=0;y<32;y++)for(int x=0;x<48;x++){int tile=a.world.tile(x,y);g.setColor(new Color(tile==WorldMap.WALL?0x67776e:tile==WorldMap.WATER?0x497b8d:0x31474a));g.fillRect(ox+x,oy+y,1,1);}
        for(Portal p:a.world.portals){g.setColor(GOLD);g.fillRect(ox+(int)p.x()/16,oy+(int)p.y()/16,2,2);}
        if(a.progress.has("tower"))for(Landmark l:a.world.landmarks)if(l.id().contains("Cache")||l.id().equals("hollowFloor")){g.setColor(new Color(0xc7a0cf));g.fillRect(ox+(int)l.x()/16,oy+(int)l.y()/16,2,2);}
        g.setColor(new Color(0xf0deb1));g.fillRect(ox+(int)a.x/16-1,oy+(int)a.y/16-1,3,3);g.setColor(MUTED);text("M  ATLAS",ox+3,oy+41,SMALL);
    }
    private void shade(){g.setColor(new Color(8,18,25,210));g.fillRect(0,0,WIDTH,HEIGHT);}
    private void drawMenu(){shade();
        g.setColor(new Color(0x37534b));for(int i=0;i<12;i++){int x=285+i*18;g.fillRect(x,80+(i*31)%60,15,170);}
        g.setColor(GOLD);text("A LITTLE KING. A LOST KINGDOM.",32,42,SMALL);
        g.setColor(PAPER);text("KINGDOM",30,91,new Font(Font.SERIF,Font.BOLD,43));text("KING",30,133,new Font(Font.SERIF,Font.BOLD,49));
        g.setColor(GOLD);text("THE STOLEN CROWN",34,157,new Font(Font.MONOSPACED,Font.PLAIN,12));
        g.setColor(MUTED);wrap("Three jewels. A ruined castle. A crown with a mind of its own.",34,178,238,12,TEXT);
        g.setColor(PAPER);text(confirmNew?"ENTER  Confirm a new kingdom":"ENTER  Begin / continue your adventure",34,216,TEXT);g.setColor(MUTED);text(confirmNew?"Esc cancels. Your previous save will be backed up.":"N  New kingdom     F  Sound     F2  Calm effects",34,234,SMALL);
        BufferedImage hero=sprite(Sprite.player_down);g.drawImage(hero,319,124,96,96,null);crown(367,103,Math.sin(a.tick*.025)*.08,3.1);
        g.setColor(new Color(0x7f9787));text("THE 2017 KINGDOMKING, REAWAKENED",34,256,SMALL);
    }
    private void drawPause(boolean paused){shade();panel(88,69,304,122);g.setColor(PAPER);center(paused?"A MOMENT OF QUIET":"YOUR KINGDOM CAN WAIT",240,97,TITLE);
        g.setColor(MUTED);center(paused?"Esc to return to the adventure":"Click the game to return",240,117,TEXT);
        center("H journal  ·  M atlas  ·  F sound  ·  F2 calm effects",240,137,SMALL);
        g.setColor(GOLD);center("R returns to the fountain and saves your discoveries",240,157,SMALL);
        g.setColor(MUTED);center("Progress saves automatically. Closing the game also saves.",240,175,SMALL);
    }
    private void drawDeath(){shade();g.setColor(GOLD);center("THE CROWN ENDURES",240,98,new Font(Font.SERIF,Font.BOLD,25));g.setColor(PAPER);center("Even a king can have a very bad afternoon.",240,125,TEXT);g.setColor(MUTED);center("Your jewels, upgrades and rescued villagers are safe.",240,148,TEXT);center("Lose at most 10 coins. Return with full health.",240,164,TEXT);g.setColor(GOLD);center("ENTER  Return to the fountain",240,197,TEXT);}
    private void drawAtlas(){shade();panel(24,38,432,205);g.setColor(GOLD);text("ATLAS OF A SMALL KINGDOM",38,57,TITLE);g.setColor(MUTED);text("M to close · gold marks your current region",38,71,SMALL);
        int[][] positions={{240,132},{240,94},{363,132},{113,132},{240,179},{363,205},{113,205}};
        int[][] links={{0,1},{0,2},{0,3},{0,4},{4,5},{4,6}};g.setColor(new Color(0x698077));
        for(int[] pair:links)g.drawLine(positions[pair[0]][0],positions[pair[0]][1],positions[pair[1]][0],positions[pair[1]][1]);
        for(Area area:Area.values()){int x=positions[area.ordinal()][0],y=positions[area.ordinal()][1];
            if(area==Area.CRYPT&&!a.progress.has("crypt")&&!a.progress.has("tower"))continue;
            g.setColor(area==a.world.area?GOLD:a.visited.contains(area)?new Color(0xb4c6ab):new Color(0x6b7c76));g.fillRect(x-4,y-4,9,9);
            g.setColor(PAPER);center(area==Area.CASTLE?"Castle":area==Area.MEADOW?"Briarfield":area==Area.MARSH?"Mosswater":area==Area.TEMPLE?"Sun Temple":area==Area.KEEP?"The Keep":area==Area.THRONE?"The Pretender":"Forgotten Crypt",x,y+17,SMALL);
        }
        g.setColor(MUTED);text("Castle restored: "+a.restoration()+" / 4    Crown jewels: "+Integer.bitCount(a.progress.jewels)+" / 3",38,232,SMALL);
    }
    private void drawJournal(){shade();panel(15,35,450,212);g.setColor(GOLD);text("THE ROYAL FIELD JOURNAL",28,55,TITLE);g.setColor(MUTED);text("H to close",392,54,SMALL);
        g.setColor(PAPER);text("YOUR NEXT CHAPTER",28,74,SMALL);g.setColor(GOLD);wrap(a.objective(),28,88,419,11,TEXT);
        g.setColor(PAPER);text("THE KING'S ARTS",28,109,SMALL);g.setColor(MUTED);
        String[] help={"WASD / arrows move · Shift sprint · Space dodge", "J three-hit slash · Q throw / recall / perfect catch", "X ground slam · E interact · U / I crown sockets", "Ruby burns · Emerald heals on return · Sapphire ricochets"};
        for(int i=0;i<help.length;i++)text(help[i],28,123+i*11,SMALL);
        g.setColor(PAPER);text("LOCAL WISDOM",28,176,SMALL);g.setColor(MUTED);
        String clue=switch(a.world.area){
            case CASTLE->"North: bells and blacksmith. East: mill and cook. West: sun temple and cartographer. Restore their buildings, then head south with all three jewels.";
            case MEADOW->"Stand west of the bells at the southern gate. Throw your crown east through 1, 2, 3, timing the moving shutter. Kill the guards inside. Knights can break the brittle northern fence.";
            case MARSH->"The southwestern lever cycles LOW, MID, HIGH. LOW reveals a submerged cache. HIGH opens the island gate. Follow the bridge east, then north. Bait a guardian arrow into the eastern bell.";
            case TEMPLE->"Turn the west statue EAST, the northern statue SOUTH and the east statue WEST. Step clear of all three beams for a moment. Your shadow can interrupt their power.";
            case KEEP->"Leave the crown on the pedestal, cross north through the gate, then pull the release. Step through the western light to silence the treasury spikes. Slam the hollow floor southeast.";
            case THRONE->"Dodge the charge into a pillar. Attack while the Pretender is stunned. Bait his thrown crown into a wall. Dash through the slam and watch for fire in the final phase.";
            case CRYPT->"The old king's heart waits beyond the guardians. Explosive slimes can injure their companions. Claim the chest for a permanent heart container.";
        };wrap(clue,28,191,421,11,TEXT);
        g.setColor(GOLD);text("Decree: "+new String[]{"Fair rule","No taxes","Royal hunt"}[a.progress.decree]+"     Blade: "+a.progress.weapon+" / 3     Best chain: "+a.progress.bestCombo,28,235,SMALL);
    }
    private void text(String text,int x,int y,Font font){g.setFont(font);g.drawString(text,x,y);}
    private void center(String text,int x,int y,Font font){g.setFont(font);g.drawString(text,x-g.getFontMetrics().stringWidth(text)/2,y);}
    private void wrap(String text,int x,int y,int width,int leading,Font font){g.setFont(font);String line="";
        for(String word:text.split(" ")){String candidate=line.isEmpty()?word:line+" "+word;if(g.getFontMetrics().stringWidth(candidate)>width&&!line.isEmpty()){g.drawString(line,x,y);y+=leading;line=word;}else line=candidate;}g.drawString(line,x,y);
    }
}
