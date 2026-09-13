package game.rain.adventure;

import game.rain.adventure.Actors.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class WorldMap {
    public static final int TILE=16, WIDTH=48, HEIGHT=32;
    public static final int GRASS=0, STONE=1, WATER=2, WALL=3, PATH=4, GATE=5, WOOD=6, CRACK=7;
    public final Area area;
    public final int[] tiles = new int[WIDTH*HEIGHT];
    public final List<Enemy> enemies=new ArrayList<>();
    public final List<Prop> props=new ArrayList<>();
    public final List<Portal> portals=new ArrayList<>();
    public final List<Landmark> landmarks=new ArrayList<>();
    private int serial;

    public WorldMap(Area area, boolean hunt, boolean won) {
        this.area=area;
        java.util.Arrays.fill(tiles, area.ordinal()>=3 ? STONE : GRASS);
        rectangle(0,0,48,1,WALL); rectangle(0,31,48,1,WALL);
        rectangle(0,0,1,32,WALL); rectangle(47,0,1,32,WALL);
        rectangle(22,1,5,30,PATH); rectangle(1,14,46,4,PATH);
        switch(area) {
            case CASTLE -> castle(); case MEADOW -> meadow(hunt); case MARSH -> marsh(hunt,won);
            case TEMPLE -> temple(hunt); case KEEP -> keep(hunt); case THRONE -> throne(won); case CRYPT -> crypt(hunt);
        }
        Random random=new Random(901L+area.ordinal());
        for(int i=0;i<72;i++) {
            int x=2+random.nextInt(44), y=3+random.nextInt(26);
            if(tile(x,y)==GRASS && !nearLandmark(x*16,y*16,34)) props.add(new Prop(PropKind.GRASS,x*16+8,y*16+8));
        }
    }
    private boolean nearLandmark(double x,double y,double distance) {
        return landmarks.stream().anyMatch(l->Math.hypot(l.x()-x,l.y()-y)<distance);
    }
    public int tile(int x,int y) { return x<0||y<0||x>=WIDTH||y>=HEIGHT ? WALL : tiles[x+y*WIDTH]; }
    public void set(int x,int y,int value) { if(x>=0&&y>=0&&x<WIDTH&&y<HEIGHT) tiles[x+y*WIDTH]=value; }
    private void rectangle(int x,int y,int w,int h,int value) { for(int j=y;j<y+h;j++)for(int i=x;i<x+w;i++)set(i,j,value); }
    private void room(int x,int y,int w,int h) {
        rectangle(x,y,w,h,STONE); rectangle(x,y,w,1,WALL); rectangle(x,y+h-1,w,1,WALL);
        rectangle(x,y,1,h,WALL); rectangle(x+w-1,y,1,h,WALL);
    }
    private void landmark(int x,int y,String id,String label) { landmarks.add(new Landmark(x*16+8,y*16+8,id,label)); }
    private void portal(int x,int y,Area target,String label) { portals.add(new Portal(x*16+8,y*16+8,target,label)); }
    private void enemy(Kind kind,int x,int y,boolean hunt) { enemies.add(new Enemy(area.ordinal()*1000+serial++,kind,x*16+8,y*16+8,hunt)); }
    private void prop(PropKind kind,int x,int y) { props.add(new Prop(kind,x*16+8,y*16+8)); }
    private void castle() {
        room(15,4,19,8); rectangle(23,11,3,1,PATH); rectangle(23,4,3,1,PATH);
        landmark(24,7,"decree","Royal decrees");
        landmark(10,12,"smith","Restore the forge"); landmark(37,12,"cook","Restore the garden");
        landmark(12,24,"cartographer","Restore the bell tower"); landmark(33,24,"mentor","Sir Bramble");
        landmark(24,20,"fountain","Rest / save");
        portal(24,2,Area.MEADOW,"Briarfield"); portal(45,16,Area.MARSH,"Mosswater Mill");
        portal(2,16,Area.TEMPLE,"Temple of Three Suns"); portal(24,29,Area.KEEP,"The Pretender's Keep");
        for(int x=6;x<43;x+=6){if(x!=24)prop(PropKind.POT,x,20); prop(PropKind.POT,x,9);}
    }
    private void meadow(boolean hunt) {
        room(13,7,23,14); set(24,20,GATE); set(25,20,GATE);
        for(int i=0;i<3;i++)landmark(18+i*4,23,"bell"+i,"Crown bell "+(i+1));
        landmark(24,12,"rescueSmith","Rescue the blacksmith"); landmark(28,12,"ruby","Ruby of Embers");
        landmark(14,26,"bellHint","Throw EAST through three bells in order. Watch the moving shutter.");
        enemy(Kind.GOBLIN,20,17,hunt); enemy(Kind.GOBLIN,29,16,hunt);
        enemy(Kind.SLIME,12,25,hunt); enemy(Kind.LARGE_SLIME,34,25,hunt);
        enemy(Kind.BOMB_SLIME,8,11,hunt); enemy(Kind.KNIGHT,39,12,hunt);
        for(int x=6;x<12;x++)prop(PropKind.FENCE,x,8);
        prop(PropKind.SIGN,8,25); prop(PropKind.CRACKED_WALL,8,8); landmark(8,5,"chargeCache","A wall a charging knight could break");
        portal(24,29,Area.CASTLE,"Return to the castle"); portal(45,16,Area.MARSH,"Mosswater Mill");
        for(int i=0;i<9;i++)prop(PropKind.POT,16+i*2,10);
    }
    private void marsh(boolean hunt,boolean won) {
        rectangle(12,7,25,16,WATER); rectangle(10,15,29,3,WOOD);
        rectangle(31,8,8,7,STONE); rectangle(18,9,4,4,STONE);
        rectangle(31,14,8,1,WALL); set(34,14,GATE); set(35,14,GATE);
        landmark(9,23,"waterLever","Waterwheel: turn the flood"); landmark(34,10,"rescueCook","Rescue the cook");
        landmark(37,10,"emerald","Emerald of Renewal"); landmark(25,10,"lowCache","Submerged passage");
        landmark(40,20,"arrowBell","Projectile bell"); landmark(43,24,"archerBell","Summon the mill guardian");
        enemy(Kind.ARCHER,40,10,hunt); enemy(Kind.ARCHER,8,11,hunt);
        enemy(Kind.STICKY_SLIME,8,19,hunt); enemy(Kind.STICKY_SLIME,40,24,hunt);
        enemy(Kind.EMBER,38,27,hunt); enemy(Kind.BOMB_SLIME,17,26,hunt);
        prop(PropKind.ROPE,22,19); landmark(22,24,"ropeBridge","Drop the rope bridge");
        prop(PropKind.WOOD,40,17); prop(PropKind.WOOD,41,17); prop(PropKind.POT,41,18);
        portal(2,16,Area.CASTLE,"Return to the castle"); portal(24,29,Area.TEMPLE,"Temple of Three Suns");
    }
    private void temple(boolean hunt) {
        room(17,3,15,8); set(24,10,GATE); set(25,10,GATE);
        landmark(16,20,"statue0","West sun statue"); landmark(24,12,"statue1","North sun statue");
        landmark(32,20,"statue2","East sun statue"); landmark(24,20,"sunSeal","Three beams, one seal");
        landmark(23,6,"sapphire","Sapphire of Echoes"); landmark(27,6,"rescueMap","Rescue the cartographer");
        enemy(Kind.KNIGHT,10,12,hunt); enemy(Kind.ARCHER,37,12,hunt);
        enemy(Kind.SLIME,12,26,hunt); enemy(Kind.LARGE_SLIME,36,26,hunt);
        for(int i=0;i<4;i++){prop(PropKind.POT,10+i*8,7);prop(PropKind.POT,10+i*8,24);}
        portal(24,29,Area.CASTLE,"Return to the castle");
    }
    private void keep(boolean hunt) {
        rectangle(1,12,46,1,WALL); set(24,12,GATE); set(25,12,GATE);
        landmark(24,20,"pedestal","Leave your crown to open the gate");
        landmark(24,8,"release","Release the crown gate");
        landmark(11,8,"shadowCache","Treasury of shadows"); landmark(10,18,"heavyPlate","Heavy pressure seal");
        landmark(35,23,"hollowFloor","The stone sounds hollow. Ground slam here."); set(35,23,CRACK);
        enemy(Kind.KNIGHT,10,24,hunt); enemy(Kind.KNIGHT,37,18,hunt);
        enemy(Kind.GOBLIN,31,24,hunt); enemy(Kind.EMBER,35,7,hunt); enemy(Kind.ARCHER,14,5,hunt);
        prop(PropKind.CRACKED_WALL,10,15); prop(PropKind.WOOD,15,18);
        portal(24,29,Area.CASTLE,"Return to the castle"); portal(24,2,Area.THRONE,"Challenge the Pretender");
    }
    private void throne(boolean won) {
        rectangle(4,3,40,25,STONE);
        for(int x:new int[]{12,35})for(int y:new int[]{10,21})prop(PropKind.CRACKED_WALL,x,y);
        if(!won)enemy(Kind.BOSS,24,10,false);
        landmark(24,6,"victoryThrone","The true throne");
        portal(24,29,Area.KEEP,"Leave the throne room");
        if(won)portal(24,16,Area.CASTLE,"Bring the crown home");
    }
    private void crypt(boolean hunt) {
        room(6,4,36,23); rectangle(23,26,3,1,STONE);
        landmark(24,10,"cryptTreasure","The old king's heart");
        enemy(Kind.BOMB_SLIME,15,17,hunt); enemy(Kind.STICKY_SLIME,31,17,hunt); enemy(Kind.KNIGHT,24,20,hunt);
        for(int x=10;x<40;x+=4){prop(PropKind.POT,x,8);prop(PropKind.POT,x,24);}
        portal(24,29,Area.KEEP,"Back to the keep");
    }
}
