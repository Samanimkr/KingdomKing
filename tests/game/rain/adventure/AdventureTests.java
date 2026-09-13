package game.rain.adventure;

import game.rain.adventure.Actors.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioSystem;

public final class AdventureTests {
    private static int groups;
    private static final Adventure.Input NONE=Adventure.Input.NONE;
    private static final Path OUTPUT=Path.of("build/test-output");
    public static void main(String[] args)throws Exception{
        Files.createDirectories(OUTPUT);
        test("normalized movement, walls and stamina",AdventureTests::movement);
        test("three-hit combo and shield counter",AdventureTests::combo);
        test("outbound crown, return and perfect catch",AdventureTests::crown);
        test("crown bells solved by an actual flight",AdventureTests::bells);
        test("water levels and submerged passage",AdventureTests::water);
        test("three sunbeams and player shadow",AdventureTests::light);
        test("crown pedestal gate and release",AdventureTests::pedestal);
        test("ground slam reveals the crypt",AdventureTests::slam);
        test("slime splitting, explosions and destructibles",AdventureTests::slimes);
        test("goblin theft and stolen-coin recovery",AdventureTests::goblin);
        test("knight charge breaks walls and hits enemies",AdventureTests::knight);
        test("archer telegraph, projectiles and enemy-powered bell",AdventureTests::archer);
        test("fire, wood and enemy interactions",AdventureTests::fire);
        test("two jewel slots and all three modifiers",AdventureTests::jewels);
        test("rescue, rebuilding, upgrades and decrees",AdventureTests::kingdom);
        test("boss phases, defeat and permanent world restoration",AdventureTests::boss);
        test("death, respawn and durable progress",AdventureTests::death);
        test("shadow treasury and heavy pressure plate",AdventureTests::secrets);
        test("all key destinations are reachable after their puzzles",AdventureTests::navigation);
        test("unavailable paths are gated before solving",AdventureTests::gates);
        test("atomic save round-trip and corrupted save protection",AdventureTests::saves);
        test("deterministic readable campaign screens",AdventureTests::screens);
        test("offline synthesized soundtrack and effects",AdventureTests::audio);
        test("extended simulated combat stays within bounds",AdventureTests::stress);
        test("campaign desktop input and thread lifecycle",AdventureTests::desktop);
        System.out.println("PASS: "+groups+" campaign regression groups");
    }
    private static void test(String label,Checked test)throws Exception{test.run();groups++;System.out.println("PASS "+label);}
    @FunctionalInterface private interface Checked{void run()throws Exception;}
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private static Adventure clean(Area area){Adventure a=new Adventure(null);a.enter(area);a.world.enemies.clear();a.hurtTime=0;a.areaAge=200;a.toastTime=0;return a;}
    private static void step(Adventure a,int frames){for(int i=0;i<frames;i++){a.update(NONE);a.sounds.clear();}}
    private static Adventure.Input input(double x,double y,boolean attack,boolean crown,boolean dash,boolean slam){return new Adventure.Input(x,y,false,attack,crown,dash,slam,false,false,false);}
    private static Enemy enemy(Adventure a,int id,Kind kind,double x,double y){Enemy e=new Enemy(id,kind,x,y,false);a.world.enemies.add(e);return e;}
    private static void movement(){
        Adventure a=clean(Area.CASTLE);a.x=392;a.y=392;double oldX=a.x,oldY=a.y;
        a.update(input(1,1,false,false,false,false));check(Math.abs(Adventure.distance(a.x,a.y,oldX,oldY)-1.45)<.001,"diagonal movement is normalized");
        a.update(input(1,0,false,false,true,false));check(a.dashTime>0&&a.stamina<80,"dash spends stamina");
        for(int i=0;i<200;i++)a.movePlayer(-10,0);check(a.x>=21,"world edge collision");
        a.stamina=0;a.dashCooldown=0;a.dashTime=0;a.update(input(0,0,false,false,true,false));check(a.dashTime==0,"cannot dash without stamina");
    }
    private static void combo(){
        Adventure a=clean(Area.CASTLE);a.x=392;a.y=392;a.faceX=1;a.faceY=0;
        a.update(input(0,0,true,false,false,false));check(a.combo==1,"first slash");
        a.attackCooldown=0;a.update(input(0,0,true,false,false,false));check(a.combo==2,"second slash");
        a.attackCooldown=0;a.update(input(0,0,true,false,false,false));check(a.combo==3,"finishing slash");
        Enemy knight=enemy(a,1,Kind.KNIGHT,420,392);knight.aimX=-1;int hp=knight.hp;
        check(!a.hitEnemy(knight,4,a.x,a.y,false)&&knight.hp==hp,"shield blocks front");
        check(a.hitEnemy(knight,5,a.x,a.y,true)&&knight.hp==hp-5,"trained finishing strike breaks guard");
        knight.stun=0;check(a.hitEnemy(knight,3,440,392,false),"attacks from behind bypass shield");
    }
    private static void crown(){
        Adventure a=clean(Area.CASTLE);a.x=392;a.y=392;a.faceX=0;a.faceY=-1;
        a.update(input(1,0,false,true,false,false));check(a.crown.vx>0&&a.crown.vy==0,"turn and crown throw use the same input frame");
        a.crown=null;
        a.throwCrown();check(a.crown!=null,"crown launched");step(a,8);check(a.crown.x>a.x+30,"crown travels outward");
        a.throwCrown();check(a.crown.returning,"second press recalls");
        a.crown.x=a.x+20;a.crown.y=a.y;a.throwCrown();check(a.crown==null&&a.empowered>0,"timed returning catch empowers next attack");
        a.throwCrown();step(a,100);check(a.crown==null,"crown always comes home");
    }
    private static void bells(){
        boolean solved=false;
        for(int attempt=0;attempt<15&&!solved;attempt++){
            Adventure a=clean(Area.MEADOW);a.x=264;a.y=376;a.faceX=1;a.faceY=0;a.tick=attempt*17;
            a.throwCrown();step(a,95);solved=a.progress.has("bells");
        }
        check(solved,"a correctly timed eastward crown throw must open the bell gate");
        Adventure a=clean(Area.MEADOW);a.ringBell(2);check(a.bellStep==0,"out-of-order bell does not advance");
        a.ringBell(0);a.bellTimer=1;a.updateMechanisms();check(a.bellStep==0,"unfinished sequence expires");
    }
    private static void water(){
        Adventure a=clean(Area.MARSH);check(a.solid(400,180),"water blocks at mid flood");
        a.useLandmark("waterLever");check(a.waterLevel==2&&!a.solid(552,232),"high flood opens island gate");
        a.useLandmark("waterLever");check(a.waterLevel==0&&!a.solid(400,180),"draining exposes basin");
        a.useLandmark("lowCache");check(a.progress.has("lowCache")&&a.progress.coins==20,"low-water passage treasure");
        a.useLandmark("emerald");check(!a.progress.has("emerald"),"emerald needs power from the wheel");
        a.waterLevel=2;a.useLandmark("emerald");check(a.progress.has("emerald"),"high-water reward");
        a.x=360;a.y=400;a.faceX=0;a.faceY=-1;a.throwCrown();step(a,40);
        check(a.world.props.stream().anyMatch(p->p.kind==PropKind.ROPE&&p.broken),"crown flies over water and cuts bridge rope");
        check(a.world.tile(22,20)==WorldMap.WOOD,"cut rope drops a traversable bridge");
    }
    private static void light(){
        Adventure a=clean(Area.TEMPLE);a.statues[0]=1;a.statues[1]=2;a.statues[2]=3;a.x=392;a.y=285;
        for(int i=0;i<120;i++)a.updateMechanisms();check(!a.progress.has("light"),"body blocks the north beam");
        a.y=370;for(int i=0;i<90;i++)a.updateMechanisms();check(a.progress.has("light"),"aligned unobstructed beams power the seal");
        check(!a.solid(392,168),"sanctuary gate opens");
    }
    private static void pedestal(){
        Adventure a=clean(Area.KEEP);check(a.solid(392,200),"keep starts closed");
        a.useLandmark("pedestal");check(a.crownResting&&!a.solid(392,200),"leaving crown powers gate");
        a.throwCrown();check(a.crown==null,"deposited crown cannot be thrown");
        a.useLandmark("release");check(a.progress.has("release")&&!a.crownResting&&!a.solid(392,200),"release permanently opens gate and returns crown");
        a.throwCrown();check(a.crown!=null,"crown usable after release");
    }
    private static void slam(){
        Adventure a=clean(Area.KEEP);a.x=568;a.y=376;a.update(input(0,0,false,false,false,true));step(a,24);
        check(a.progress.has("crypt")&&a.world.tile(35,23)==WorldMap.PATH,"ground slam opens crypt floor");
        a.interact();check(a.world.area==Area.CRYPT,"player can enter the revealed stairway");
    }
    private static void slimes(){
        Adventure a=clean(Area.CASTLE);Enemy big=enemy(a,3,Kind.LARGE_SLIME,450,380);a.hitEnemy(big,99,a.x,a.y,true);
        check(a.world.enemies.stream().filter(e->!e.dead&&e.kind==Kind.SLIME).count()==2,"large slime splits into two");
        Enemy bomb=enemy(a,4,Kind.BOMB_SLIME,510,380),target=enemy(a,5,Kind.ARCHER,525,380);
        a.world.props.add(new Prop(PropKind.CRACKED_WALL,515,390));a.hitEnemy(bomb,99,480,380,true);
        check(target.dead,"explosion hits other enemies");check(a.world.props.get(a.world.props.size()-1).broken,"explosion breaks brittle walls");
    }
    private static void goblin(){
        Adventure a=clean(Area.CASTLE);a.x=392;a.y=392;Enemy goblin=enemy(a,5,Kind.GOBLIN,450,392);
        a.loot.add(new Loot(450,392,7,false));step(a,2);check(goblin.stolen==7,"goblin steals loose coins");
        a.hitEnemy(goblin,99,a.x,a.y,true);check(a.loot.stream().anyMatch(l->!l.heart&&l.value>=10),"defeated goblin returns stolen coins");
    }
    private static void knight(){
        Adventure a=clean(Area.CASTLE);a.x=500;a.y=400;
        Enemy knight=enemy(a,7,Kind.KNIGHT,330,380);knight.charging=40;knight.aimX=1;knight.aimY=0;
        Prop brittle=new Prop(PropKind.CRACKED_WALL,360,380);a.world.props.add(brittle);step(a,15);
        check(brittle.broken&&knight.stun>0,"charge shatters a brittle wall and stuns knight");
        Adventure b=clean(Area.CASTLE);b.x=600;b.y=450;Enemy k=enemy(b,1,Kind.KNIGHT,320,380);Enemy slime=enemy(b,2,Kind.SLIME,337,380);k.charging=15;k.aimX=1;step(b,3);check(slime.dead,"charging knight knocks aside other enemies");
    }
    private static void archer(){
        Adventure a=clean(Area.MARSH);a.world.props.clear();a.x=648;a.y=410;Enemy archer=enemy(a,8,Kind.ARCHER,648,280);archer.cooldown=0;step(a,1);
        check(archer.windup>0,"archer telegraphs before firing");step(a,40);check(!a.shots.isEmpty(),"archer releases projectile after windup");
        a.world.props.clear();a.shots.clear();a.shots.add(new Shot(648,321,0,3,8,false,false));step(a,5);
        check(a.progress.has("arrowBell"),"hostile projectile activates mill bell");
    }
    private static void fire(){
        Adventure a=clean(Area.CASTLE);a.x=392;a.y=392;Prop wood=new Prop(PropKind.WOOD,450,380);a.world.props.add(wood);
        a.shots.add(new Shot(444,380,3,0,99,true,false));step(a,75);check(wood.broken,"fire burns wooden scenery");
        Enemy target=enemy(a,22,Kind.SLIME,500,392);a.shots.add(new Shot(492,392,3,0,99,true,false));step(a,5);check(target.hp<target.maxHp||target.dead,"hostile fire can hurt another enemy");
    }
    private static void jewels(){
        Adventure a=clean(Area.CASTLE);a.grantJewel(Jewel.RUBY,"ruby");a.grantJewel(Jewel.EMERALD,"emerald");a.grantJewel(Jewel.SAPPHIRE,"sapphire");
        check(a.progress.slotA==1&&a.progress.slotB==2,"first two jewels equip in different sockets");
        a.update(new Adventure.Input(0,0,false,false,false,false,false,false,true,false));
        check(a.progress.slotA!=a.progress.slotB,"cycling sockets never duplicates an equipped jewel");
        a.progress.slotA=3;a.progress.slotB=2;a.x=40;a.y=392;a.faceX=-1;a.faceY=0;a.throwCrown();step(a,6);
        check(a.crown!=null&&a.crown.bounces>0,"sapphire ricochets from a wall");
        a.health=8;a.crown.returning=true;a.crown.hits=1;a.crown.x=a.x+2;a.crown.y=a.y;step(a,1);check(a.health==9,"emerald heals a successful returning crown");
        a.progress.slotA=1;a.progress.slotB=0;a.x=392;a.y=392;a.faceX=1;a.faceY=0;
        Enemy e=enemy(a,11,Kind.LARGE_SLIME,435,392);a.throwCrown();step(a,8);check(e.burn>0,"ruby ignites struck enemies");
    }
    private static void kingdom(){
        Adventure a=clean(Area.CASTLE);a.progress.coins=200;
        a.useLandmark("smith");check(!a.progress.has("forge"),"cannot restore forge before rescue");
        a.useLandmark("rescueSmith");a.useLandmark("smith");check(a.progress.has("forge"),"rescued smith restores forge");
        a.useLandmark("smith");check(a.progress.weapon==1,"blacksmith upgrades blade");
        a.useLandmark("rescueCook");a.useLandmark("cook");a.useLandmark("cook");check(a.progress.has("garden")&&a.foodTime>0,"restored garden provides meal buff");
        a.useLandmark("rescueMap");a.useLandmark("cartographer");check(a.restoration()==3,"all three buildings restored");
        a.useLandmark("decree");check(a.price(20,false)==15&&a.price(20,true)==25,"no-taxes decree trades cheaper supplies for slower restoration");
        a.useLandmark("decree");a.enter(Area.MEADOW);check(a.world.enemies.get(0).hp>7,"hunt creates tougher enemies");
        a.world.enemies.clear();a.enter(Area.CASTLE);a.enter(Area.MEADOW);check(!a.world.enemies.isEmpty(),"hunt replenishes an expedition");
    }
    private static void boss(){
        Adventure a=clean(Area.THRONE);Enemy boss=enemy(a,100,Kind.BOSS,392,180);a.x=392;a.y=300;
        boss.hp=64;step(a,1);check(boss.phase==2,"boss enters second phase");boss.stun=0;boss.hp=30;step(a,1);
        check(boss.phase==3&&a.world.enemies.size()>=3,"final phase summons reinforcements");
        boss.stun=90;a.hitEnemy(boss,200,a.x,a.y,true);check(a.progress.has("won")&&a.victoryTime>0,"boss victory persists and begins celebration");
        check(a.world.enemies.stream().allMatch(e->e.dead),"victory clears the remaining hostile reinforcements");
        a.enter(Area.MARSH);check(!a.solid(400,180),"winning purifies the marsh");
        a.maps.remove(Area.THRONE);a.enter(Area.THRONE);check(a.world.enemies.stream().noneMatch(e->e.kind==Kind.BOSS),"defeated boss stays defeated");
    }
    private static void death(){
        Adventure a=clean(Area.CASTLE);a.progress.coins=100;a.progress.set("ruby");a.health=1;a.hurtTime=0;
        a.shots.add(new Shot(a.x,a.y,0,0,10,false,false));step(a,1);check(a.dead,"lethal attack enters death state");
        a.respawn();check(!a.dead&&a.health==a.progress.maxHealth()&&a.progress.coins==90,"respawn restores health with bounded coin loss");check(a.progress.has("ruby"),"progress survives death");
    }
    private static void secrets(){
        Adventure a=clean(Area.KEEP);a.x=168;a.y=160;a.updateMechanisms();check(a.shadowTime>0,"king's shadow silences treasury spikes");
        a.useLandmark("shadowCache");check(a.progress.has("shadowCache"),"shadow treasury reward");
        Enemy knight=enemy(a,3,Kind.KNIGHT,168,296);a.updateMechanisms();check(a.progress.has("heavyPlate")&&knight.hp>0,"heavy enemy powers pressure seal");
    }
    private static Set<Integer> reachable(Adventure a){
        Set<Integer> visited=new HashSet<>();ArrayDeque<Integer> queue=new ArrayDeque<>();int start=(int)a.x/16+(int)a.y/16*48;queue.add(start);visited.add(start);
        while(!queue.isEmpty()){int tile=queue.remove(),x=tile%48,y=tile/48;for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){int nx=x+d[0],ny=y+d[1],next=nx+ny*48;if(nx>0&&ny>0&&nx<47&&ny<31&&!a.solid(nx*16+8,ny*16+8)&&visited.add(next))queue.add(next);}}
        return visited;
    }
    private static boolean accessible(Set<Integer> reachable,double x,double y){for(int tile:reachable)if(Adventure.distance(tile%48*16+8,tile/48*16+8,x,y)<=30)return true;return false;}
    private static void navigation(){
        for(Area area:Area.values()){
            Adventure a=clean(area);a.progress.flags.addAll(List.of("bells","light","release","crypt","won"));a.waterLevel=2;
            Set<Integer> reached=reachable(a);
            for(Portal p:a.world.portals)check(accessible(reached,p.x(),p.y()),area+" portal unreachable: "+p.label());
            for(Landmark l:a.world.landmarks)check(accessible(reached,l.x(),l.y()),area+" landmark unreachable: "+l.id());
        }
    }
    private static void gates(){
        Adventure a=clean(Area.MEADOW);check(!accessible(reachable(a),392,200),"smith cage area remains behind bell gate");
        a=clean(Area.TEMPLE);check(!accessible(reachable(a),376,104),"temple reward remains behind light gate");
        a=clean(Area.KEEP);check(!accessible(reachable(a),392,40),"boss entrance is behind crown pedestal gate");
        a.x=392;a.y=40;a.interact();check(a.world.area==Area.KEEP,"boss cannot be entered without all three jewels");
    }
    private static void saves()throws IOException{
        Path dir=Files.createTempDirectory("kingdomking-save-test-");Path path=dir.resolve("campaign.properties");SaveStore store=new SaveStore(path);
        Progress p=new Progress();p.coins=57;p.weapon=2;p.hearts=2;p.jewels=14;p.slotA=1;p.slotB=3;p.flags.addAll(List.of("won","forge","ruby"));store.save(p);
        Progress read=store.load();check(read.coins==57&&read.weapon==2&&read.has("won")&&read.slotB==3,"save restores campaign progression");
        store.backup();check(Files.exists(dir.resolve("campaign.previous.properties")),"new game can preserve previous campaign");
        Files.writeString(path,"version=1\ncoins=not-a-number\n");
        Adventure a=new Adventure(store);check(a.saveFailed,"invalid save disables overwriting");a.progress.coins=900;a.save();check(Files.readString(path).contains("not-a-number"),"invalid save remains untouched");
        Files.delete(path);Files.delete(dir.resolve("campaign.previous.properties"));Files.delete(dir);
    }
    private static void screens()throws IOException{
        Adventure a=clean(Area.CASTLE);AdventureRenderer renderer=new AdventureRenderer();
        BufferedImage image=renderer.render(a,false,false,false,false,false,true);check(image.getWidth()==480&&image.getHeight()==270,"native logical resolution");
        Set<Integer> colors=new HashSet<>();for(int y=0;y<270;y++)for(int x=0;x<480;x++)colors.add(image.getRGB(x,y));check(colors.size()>40,"scene contains artwork, UI and world detail");
        for(Area area:Area.values()){a.enter(area);a.areaAge=200;a.toastTime=0;ImageIO.write(renderer.render(a,false,false,false,false,false,true),"png",OUTPUT.resolve("campaign-"+area.name().toLowerCase()+".png").toFile());}
        renderer.render(a,true,false,false,false,false,true);renderer.render(a,false,true,false,false,false,true);
        renderer.render(a,false,false,true,false,false,true);renderer.render(a,false,false,false,true,false,true);
        int first=Arrays.hashCode(image.getRGB(0,0,480,270,null,0,480));
        int second=Arrays.hashCode(renderer.render(a,false,false,false,true,false,true).getRGB(0,0,480,270,null,0,480));check(first==second,"same state produces same journal pixels");
    }
    private static void audio()throws Exception{
        Path demo=OUTPUT.resolve("soundtrack-demo.wav");AudioEngine.writeDemo(demo);
        try(var input=AudioSystem.getAudioInputStream(demo.toFile())){check(input.getFormat().getSampleRate()==22050&&input.getFrameLength()==22050*8,"eight-second PCM demo");byte[] bytes=input.readAllBytes();long nonzero=0;for(byte value:bytes)if(value!=0)nonzero++;check(nonzero>10000,"audio contains synthesized signal");}
    }
    private static void desktop(){
        game.rain.Game game=new game.rain.Game(null);
        game.rain.input.Keyboard key=(game.rain.input.Keyboard)game.getKeyListeners()[0];
        BufferedImage image=game.renderFrame();int before=Arrays.hashCode(image.getRGB(0,0,480,270,null,0,480));
        key.keyPressed(new java.awt.event.KeyEvent(game,java.awt.event.KeyEvent.KEY_PRESSED,0,0,java.awt.event.KeyEvent.VK_ENTER,'\n'));
        game.update();image=game.renderFrame();int after=Arrays.hashCode(image.getRGB(0,0,480,270,null,0,480));
        check(before!=after,"Enter changes the title into the playable scene");
        key.keyReleased(new java.awt.event.KeyEvent(game,java.awt.event.KeyEvent.KEY_RELEASED,0,0,java.awt.event.KeyEvent.VK_ENTER,'\n'));
        game.stop();game.start();game.start();
        check(Thread.getAllStackTraces().keySet().stream().filter(t->t.isAlive()&&t.getName().equals("KingdomKing campaign")).count()==1,"campaign starts only one loop");
        game.stop();check(Thread.getAllStackTraces().keySet().stream().noneMatch(t->t.isAlive()&&t.getName().equals("KingdomKing campaign")),"campaign stops cleanly");
    }
    private static void stress(){
        for(Area area:Area.values()){
            Adventure a=new Adventure(null);a.enter(area);a.areaAge=200;
            for(int i=0;i<2400;i++){
                if(a.dead)a.respawn();double angle=i*.031;
                a.update(input(Math.cos(angle),Math.sin(angle),i%15==0,i%65==0,i%100==0,i%130==0));
                check(Double.isFinite(a.x)&&Double.isFinite(a.y),"positions remain finite");check(a.particles.size()<=450,"particle count bounded");a.sounds.clear();
            }
        }
    }
}
