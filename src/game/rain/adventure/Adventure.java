package game.rain.adventure;

import game.rain.adventure.Actors.*;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

/** Fixed-step campaign simulation. No display or audio device is needed to test it. */
public final class Adventure {
    public record Input(double x,double y,boolean sprint,boolean attack,boolean crown,boolean dash,
                        boolean slam,boolean interact,boolean jewelA,boolean jewelB) {
        public static final Input NONE=new Input(0,0,false,false,false,false,false,false,false,false);
    }
    public Progress progress;
    public WorldMap world;
    public double x=392,y=392,faceX=0,faceY=1,moveX,moveY,stamina=100,capeX,capeY;
    public int health=12,tick,areaAge,attackTime,attackCooldown,combo,comboWindow,dashTime,dashCooldown,slamTime,slamCooldown;
    public int hurtTime,hitstop,shake,empowered,healCooldown,slowTime,foodTime,footTimer,kills,killChain,chainTimer;
    public int bellStep,bellTimer,waterLevel=1,lightCharge,shadowTime,victoryTime,catchFlash;
    public final int[] statues={0,0,0};
    public boolean crownResting,dead,moving,dirty,saveFailed;
    public String toast="Welcome home, Your Majesty. Find Sir Bramble by the southern path.",speaker="THE QUIET CROWN";
    public int toastTime=340;
    public Crown crown;
    public final List<Shot> shots=new ArrayList<>();
    public final List<Loot> loot=new ArrayList<>();
    public final List<Particle> particles=new ArrayList<>();
    public final List<Patch> patches=new ArrayList<>();
    public final Queue<String> sounds=new ArrayDeque<>();
    public final EnumMap<Area,WorldMap> maps=new EnumMap<>(Area.class);
    public final Set<Area> visited=new HashSet<>();
    private final Random random=new Random(6716);
    private final SaveStore store;
    private final Set<Integer> swingHits=new HashSet<>();
    private int nextEnemyId=10000;

    public Adventure(SaveStore store) {
        this.store=store;
        progress=new Progress();
        if(store!=null)try{progress=store.load();}catch(IOException e){saveFailed=true;toast="Save could not be read. Your existing file is protected; this session will not overwrite it.";}
        health=progress.maxHealth(); enter(Area.CASTLE);
        if(progress.has("won"))say("HOME AGAIN","Your kingdom is alive. Restore the last buildings, find secrets, or declare a royal hunt.");
    }
    public void newCampaign() {
        if(store!=null)try{store.backup();}catch(IOException e){say("SAVE","Could not back up the previous campaign. New campaign cancelled.");return;}
        progress=new Progress(); maps.clear();visited.clear();saveFailed=false;dead=false;health=12;stamina=100;
        enter(Area.CASTLE);dirty=true;save();say("THE STOLEN CROWN","Three jewels. Three lost villagers. One rather unconvincing king. Your story begins here.");
    }
    public void save() {
        if(store==null||saveFailed)return;
        try{store.save(progress);dirty=false;}catch(IOException e){say("SAVE","Progress could not be saved. Check the save-folder permissions.");}
    }
    public void say(String who,String text) { speaker=who;toast=text;toastTime=300; }
    public void sound(String id) { if(sounds.size()<48)sounds.add(id); }
    public int restoration(){return progress.restored();}
    public String objective() {
        if(progress.has("won"))return restoration()<4?"Bring the kingdom back to life":"Explore the secrets of your kingdom";
        if(!progress.has("ruby"))return "Briarfield: ring the three crown bells";
        if(!progress.has("emerald"))return "Mosswater: power the flooded mill";
        if(!progress.has("sapphire"))return "Temple: unite the three sunbeams";
        if(!progress.has("release"))return "Keep: leave your crown, cross the gate";
        return "Defeat the Pretender and reclaim the throne";
    }
    public void enter(Area area) {
        if(progress.decree==2&&area!=Area.CASTLE&&area!=Area.THRONE)maps.remove(area);
        world=maps.computeIfAbsent(area,a->new WorldMap(a,progress.decree==2,progress.has("won")));
        visited.add(area);x=24*16+8;y=27*16+8;faceX=0;faceY=-1;
        if(area==Area.CASTLE){y=23*16+8;health=progress.maxHealth();}
        if(area==Area.MARSH){x=4*16+8;y=16*16+8;}
        crown=null;crownResting=false;shots.clear();loot.clear();patches.clear();particles.clear();
        areaAge=0;dashTime=0;attackTime=0;slamTime=0;hurtTime=60;dead=false;bellStep=0;
        if(area!=Area.CASTLE)say(area.title.toUpperCase(),area.subtitle+". Press H for your journal and local clues.");
        save();sound("travel");
    }
    public void respawn() {
        if(dead){progress.deaths++;progress.coins=Math.max(0,progress.coins-Math.min(10,progress.coins/10));}
        enter(Area.CASTLE);stamina=100;dirty=true;save();say("THE CROWN ENDURES","Safe at the fountain. Your discoveries and rescued villagers are remembered.");
    }
    public void update(Input input) {
        tick++;areaAge++;if(toastTime>0)toastTime--;if(shake>0)shake--;if(catchFlash>0)catchFlash--;
        updateParticles();
        if(victoryTime>0){victoryTime--;if(victoryTime==150){sound("victory");say("A KINGDOM REAWAKENS","The Pretender has fallen. The marsh clears; your people can finally come home.");}}
        if(dead)return;
        if(hitstop>0){hitstop--;return;}
        if(hurtTime>0)hurtTime--;if(attackCooldown>0)attackCooldown--;if(attackTime>0)attackTime--;
        if(comboWindow>0)comboWindow--;else combo=0;
        if(dashCooldown>0)dashCooldown--;if(slamCooldown>0)slamCooldown--;if(empowered>0)empowered--;
        if(healCooldown>0)healCooldown--;if(slowTime>0)slowTime--;if(foodTime>0)foodTime--;
        if(chainTimer>0)chainTimer--;else killChain=0;
        stamina=Math.min(100,stamina+(foodTime>0?.7:.42));
        double aimLength=Math.hypot(input.x(),input.y());
        if(aimLength>0&&dashTime==0&&slamTime==0){faceX=input.x()/aimLength;faceY=input.y()/aimLength;}
        if(input.jewelA())cycleJewel(true);if(input.jewelB())cycleJewel(false);
        if(input.interact())interact();
        if(input.crown())throwCrown();
        if(input.dash()&&dashCooldown==0&&stamina>=24&&slamTime==0){
            stamina-=24;dashTime=9;dashCooldown=24;hurtTime=Math.max(hurtTime,12);sound("dash");
            if(input.x()!=0||input.y()!=0){double length=Math.hypot(input.x(),input.y());faceX=input.x()/length;faceY=input.y()/length;}
        }
        if(input.slam()&&slamCooldown==0&&stamina>=42&&dashTime==0){stamina-=42;slamTime=22;slamCooldown=110;sound("jump");}
        if(input.attack()&&attackCooldown==0&&slamTime==0){
            combo=comboWindow>0?combo%3+1:1;comboWindow=52;attackTime=15;attackCooldown=combo==3?25:17;
            swingHits.clear();sound(combo==3?"heavy":"slash");
        }
        moveX=input.x();moveY=input.y();double length=Math.hypot(moveX,moveY);
        moving=length>0;
        if(length>0){moveX/=length;moveY/=length;if(dashTime==0){faceX=moveX;faceY=moveY;}}
        if(slamTime>0){slamTime--;moving=false;if(slamTime==4)slam();}
        else if(dashTime>0){dashTime--;movePlayer(faceX*5.5,faceY*5.5);particle(x,y,0,0,15,0xa8394c,8);}
        else {
            double speed=input.sprint()?2.05:1.45;if(slowTime>0)speed*=.5;
            if(attackTime>4)speed*=.72;movePlayer(moveX*speed,moveY*speed);
        }
        capeX+=( -faceX*(moving?10:4)-capeX)*.18;capeY+=(-faceY*(moving?10:5)-capeY)*.18;
        if(moving&&++footTimer%13==0){int tile=world.tile((int)x/16,(int)y/16);sound(world.area==Area.KEEP&&distance(x,y,568,376)<25?"hollow":tile==WorldMap.WOOD?"woodstep":tile==WorldMap.WATER?"splash":tile==WorldMap.GRASS?"grassstep":"step");burst(x,y+5,3,0x9b9568);}
        if(attackTime>=7&&attackTime<=12)swordHits();
        updateCrown();updateEnemies();updateShots();updateLoot();updatePatches();updateMechanisms();
        for(Prop p:world.props){if(p.bend>0)p.bend--;if(!p.broken&&p.kind==PropKind.GRASS&&distance(x,y,p.x,p.y)<16)p.bend=20;
            if(p.burning>0){p.burning--;if(tick%6==0)particle(p.x,p.y,random.nextDouble()-.5,-.5,25,0xffa94f,2);if(p.burning==1)breakProp(p);}}
        if(tick%360==0&&world.area.ordinal()<3)sound("bird");
        if(tick%1200==0&&progress.has("tower"))sound("bell0");
        if(dirty&&tick%300==0)save();
    }
    public boolean solid(double px,double py) {
        int tx=(int)Math.floor(px/16),ty=(int)Math.floor(py/16),tile=world.tile(tx,ty);
        if(tile==WorldMap.WALL)return true;
        if(tile==WorldMap.GATE){
            return switch(world.area){case MEADOW->!progress.has("bells");case MARSH->waterLevel<2&&!progress.has("won");
                case TEMPLE->!progress.has("light");case KEEP->!crownResting&&!progress.has("release");default->true;};
        }
        if(tile==WorldMap.WATER&&!(world.area==Area.MARSH&&(waterLevel==0||progress.has("won"))))return true;
        for(Prop p:world.props)if(p.solid()&&Math.abs(px-p.x)<6&&Math.abs(py-p.y)<6)return true;
        return false;
    }
    public boolean projectileBlocked(double px,double py){
        // The crown and arrows fly above water; only physical walls stop them.
        if(world.tile((int)Math.floor(px/16),(int)Math.floor(py/16))==WorldMap.WATER)return false;
        return solid(px,py);
    }
    private boolean blocked(double px,double py,double r){
        if(solid(px-r,py-r)||solid(px+r,py-r)||solid(px-r,py+r)||solid(px+r,py+r))return true;
        // Corner sampling alone misses props smaller than the collision box.
        for(Prop prop:world.props)if(prop.solid()&&Math.abs(px-prop.x)<r+6&&Math.abs(py-prop.y)<r+6)return true;
        return false;
    }
    public void movePlayer(double dx,double dy) {
        int steps=Math.max(1,(int)Math.ceil(Math.max(Math.abs(dx),Math.abs(dy))));
        for(int i=0;i<steps;i++){if(!blocked(x+dx/steps,y,5))x+=dx/steps;if(!blocked(x,y+dy/steps,5))y+=dy/steps;}
    }
    private boolean moveEnemy(Enemy e,double dx,double dy) {
        boolean moved=false;int steps=Math.max(1,(int)Math.ceil(Math.max(Math.abs(dx),Math.abs(dy))));
        for(int i=0;i<steps;i++){
            if(!blocked(e.x+dx/steps,e.y,Math.min(8,e.radius()))){e.x+=dx/steps;moved=true;}
            if(!blocked(e.x,e.y+dy/steps,Math.min(8,e.radius()))){e.y+=dy/steps;moved=true;}
        }return moved;
    }
    public void throwCrown() {
        if(crownResting){say("CROWN","It is powering the gate. Pull the release lever on the other side.");return;}
        if(crown!=null){
            if(crown.returning&&distance(crown.x,crown.y,x,y)<44){empowered=180;catchFlash=24;health=Math.min(progress.maxHealth(),health+(progress.jewel(Jewel.EMERALD)?1:0));sound("perfect");crown=null;}
            else{crown.returning=true;crown.struck.clear();sound("recall");}return;
        }
        crown=new Crown();crown.x=x+faceX*10;crown.y=y+faceY*10;crown.vx=faceX*4.6;crown.vy=faceY*4.6;
        bellStep=0;bellTimer=0;sound(progress.jewel(Jewel.RUBY)?"throw_ruby":progress.jewel(Jewel.SAPPHIRE)?"throw_sapphire":progress.jewel(Jewel.EMERALD)?"throw_emerald":"throw");
    }
    private void updateCrown() {
        if(crown==null)return;Crown c=crown;c.age++;
        if(c.age>=40&&!c.returning){c.returning=true;c.struck.clear();sound("recall");}
        if(c.returning){double d=distance(c.x,c.y,x,y);if(d<8){
            if(progress.jewel(Jewel.EMERALD)&&c.hits>0&&healCooldown==0){health=Math.min(progress.maxHealth(),health+1);healCooldown=180;}
            sound("catch");burst(x,y,8,0xf5d58a);crown=null;return;}
            c.vx=(x-c.x)/d*5.4;c.vy=(y-c.y)/d*5.4;
        }
        if(!c.returning&&projectileBlocked(c.x+c.vx,c.y+c.vy)){
            if(progress.jewel(Jewel.SAPPHIRE)&&c.bounces<3){
                if(projectileBlocked(c.x+c.vx,c.y))c.vx=-c.vx;else c.vy=-c.vy;c.bounces++;sound("ricochet");
            }else{c.returning=true;c.struck.clear();}
        }
        c.x+=c.vx;c.y+=c.vy;if(tick%2==0)particle(c.x,c.y,0,0,12,progress.jewel(Jewel.RUBY)?0xff9c64:0xe8cb7a,2);
        for(Enemy e:new ArrayList<>(world.enemies))if(!e.dead&&distance(c.x,c.y,e.x,e.y)<e.radius()+9&&c.struck.add(e.id)){
            if(hitEnemy(e,3+progress.weapon,c.x-c.vx,c.y-c.vy,false)){c.hits++;if(progress.jewel(Jewel.RUBY))e.burn=150;}
        }
        for(Prop p:world.props)if(!p.broken&&distance(c.x,c.y,p.x,p.y)<13){
            if(p.kind==PropKind.GRASS||p.kind==PropKind.POT||p.kind==PropKind.ROPE)breakProp(p);
            else if(progress.jewel(Jewel.RUBY)&&p.kind!=PropKind.CRACKED_WALL)p.burning=60;
        }
        if(world.area==Area.MEADOW&&!progress.has("bells"))for(int i=0;i<3;i++){
            if(distance(c.x,c.y,(18+i*4)*16+8,23*16+8)<12){ringBell(i);}
        }
        if(world.area==Area.MEADOW&&!progress.has("bells")&&!c.returning&&distance(c.x,c.y,22*16+8,23*16+8+Math.sin(tick*.035)*33)<11){c.returning=true;c.struck.clear();sound("block");}
        for(Loot l:loot)if(distance(c.x,c.y,l.x,l.y)<18)collect(l);
    }
    public void ringBell(int index){
        if(index==bellStep){bellStep++;bellTimer=150;sound("bell"+index);burst((18+index*4)*16+8,23*16+8,12,0xf4d995);
            if(bellStep==3){progress.set("bells");dirty=true;say("THE BELLS ANSWER","The goblin gate is open. Rescue the blacksmith and recover the ruby.");sound("secret");}}
        else if(index>bellStep){bellStep=0;sound("block");}
    }
    private void swordHits(){
        for(Enemy e:new ArrayList<>(world.enemies))if(!e.dead&&!swingHits.contains(e.id)&&inArc(e.x,e.y,45+e.radius())&&clearAttackLine(e.x,e.y)){
            swingHits.add(e.id);hitEnemy(e,(combo==3?5:3)+progress.weapon+(empowered>0?3:0),x,y,combo==3&&progress.has("training"));}
        for(Prop p:world.props)if(!p.broken&&inArc(p.x,p.y,42)&&p.kind!=PropKind.CRACKED_WALL)breakProp(p);
        if(attackTime==10&&empowered>0)empowered=0;
    }
    private boolean clearAttackLine(double px,double py){
        double length=distance(x,y,px,py);for(double step=6;step<length-5;step+=6)
            if(projectileBlocked(x+(px-x)*step/length,y+(py-y)*step/length))return false;
        return true;
    }
    private boolean inArc(double px,double py,double range){double dx=px-x,dy=py-y,d=Math.hypot(dx,dy);return d<range&&(d<10||(dx*faceX+dy*faceY)/d>.05);}
    public boolean hitEnemy(Enemy e,int damage,double fromX,double fromY,boolean unblockable){
        if(e.dead)return false;double dx=fromX-e.x,dy=fromY-e.y,d=Math.max(1,Math.hypot(dx,dy));
        if(e.kind==Kind.KNIGHT&&!unblockable&&e.stun==0&&e.charging==0&&(dx*e.aimX+dy*e.aimY)/d>.35){sound("block");burst(e.x,e.y,5,0xc5d3d6);return false;}
        if(e.kind==Kind.BOSS&&e.stun==0)damage=Math.max(1,damage/2);
        e.hp-=damage;e.flash=8;e.vx=-dx/d*2.5;e.vy=-dy/d*2.5;
        if(e.kind!=Kind.BOSS&&e.charging==0)e.stun=Math.max(e.stun,8);
        hitstop=Math.max(hitstop,3);shake=Math.max(shake,3);sound("hit");burst(e.x,e.y,10,0xf0c779);
        if(e.hp<=0)kill(e);return true;
    }
    private void kill(Enemy e){
        if(e.dead)return;e.dead=true;kills++;killChain++;chainTimer=180;progress.bestCombo=Math.max(progress.bestCombo,killChain);
        if(e.kind==Kind.BOSS){
            progress.set("won");dirty=true;
            for(Enemy other:world.enemies)other.dead=true;
            for(Shot shot:shots)shot.life=0;
            for(Patch patch:patches)patch.life=0;
            victoryTime=190;hitstop=28;shake=22;sound("bossfall");
            burst(e.x,e.y,90,0xf9d48c);world.portals.add(new Portal(e.x,e.y,Area.CASTLE,"Bring the crown home"));
            progress.coins+=75;save();return;
        }
        int value=(e.kind==Kind.KNIGHT?6:3)+(progress.decree==2?3:0)+e.stolen;
        loot.add(new Loot(e.x,e.y,value,false));if(random.nextInt(4)==0)loot.add(new Loot(e.x+10,e.y,2,true));
        burst(e.x,e.y,16,e.kind==Kind.SLIME||e.kind==Kind.LARGE_SLIME?0x89b56f:0xbd9b68);sound("defeat");
        if(e.kind==Kind.LARGE_SLIME){for(int i=0;i<2;i++)world.enemies.add(new Enemy(nextEnemyId++,Kind.SLIME,e.x+(i*2-1)*10,e.y,false));}
        if(e.kind==Kind.BOMB_SLIME)explode(e.x,e.y,50,e.id);
    }
    private void hurt(int amount,double fromX,double fromY){
        if(hurtTime>0||dead||dashTime>0||world.area==Area.THRONE&&progress.has("won"))return;
        health-=amount;hurtTime=65;shake=7;killChain=0;sound("hurt");
        double d=Math.max(1,distance(x,y,fromX,fromY));movePlayer((x-fromX)/d*12,(y-fromY)/d*12);
        if(health<=0){health=0;dead=true;progress.set("fallen");sound("fall");save();}
    }
    private void slam(){
        sound("slam");shake=12;hitstop=5;burst(x,y,50,0xcebb91);
        for(Enemy e:new ArrayList<>(world.enemies))if(!e.dead&&distance(x,y,e.x,e.y)<76){hitEnemy(e,7+progress.weapon,x,y,true);e.stun=e.kind==Kind.BOSS?Math.max(e.stun,18):75;}
        for(Prop p:world.props)if(!p.broken&&distance(x,y,p.x,p.y)<66)breakProp(p);
        if(world.area==Area.KEEP&&distance(x,y,35*16+8,23*16+8)<44){progress.set("crypt");world.set(35,23,WorldMap.PATH);dirty=true;say("A HOLLOW ANSWER","A stairway opens beneath the broken stone. Press E to descend.");sound("secret");}
    }
    private void breakProp(Prop p){
        if(p.broken)return;p.broken=true;sound(p.kind==PropKind.GRASS?"cut":"break");
        if(p.kind==PropKind.ROPE&&world.area==Area.MARSH){for(int yy=18;yy<=22;yy++)world.set(22,yy,WorldMap.WOOD);say("A FALLING BRIDGE","The crown cuts the support rope. A wooden bridge drops across the water.");sound("crash");}
        burst(p.x,p.y,12,p.kind==PropKind.GRASS?0x87a260:0xb79b78);
        if(p.kind==PropKind.POT||p.kind==PropKind.CRACKED_WALL)loot.add(new Loot(p.x,p.y,p.kind==PropKind.CRACKED_WALL?5:2,false));
        else if(random.nextInt(6)==0)loot.add(new Loot(p.x,p.y,1,false));
        if(p.kind==PropKind.POT&&random.nextInt(12)==0){world.enemies.add(new Enemy(nextEnemyId++,Kind.CHICKEN,p.x,p.y,false));say("CLUCK!","That pot belonged to someone.");}
    }
    private void updateEnemies(){
        for(Enemy e:new ArrayList<>(world.enemies)){
            if(e.dead)continue;e.age++;if(e.flash>0)e.flash--;if(e.cooldown>0)e.cooldown--;
            if(e.burn>0){e.burn--;if(e.burn%30==0){e.hp--;burst(e.x,e.y,4,0xffad57);if(e.hp<=0){kill(e);continue;}}}
            double dx=x-e.x,dy=y-e.y,d=Math.max(1,Math.hypot(dx,dy));
            if(e.stun>0){e.stun--;moveEnemy(e,e.vx,e.vy);e.vx*=.8;e.vy*=.8;continue;}
            if(e.kind==Kind.BOSS){boss(e,dx,dy,d);continue;}
            if(e.charging>0){
                e.charging--;boolean wall=blocked(e.x+e.aimX*8,e.y+e.aimY*8,8);
                moveEnemy(e,e.aimX*4.3,e.aimY*4.3);if(tick%3==0)burst(e.x,e.y,2,0xa4a594);
                if(wall){e.charging=0;e.stun=75;e.hp-=2;sound("crash");shake=7;
                    for(Prop p:world.props)if(!p.broken&&distance(e.x,e.y,p.x,p.y)<30)breakProp(p);
                    if(e.hp<=0)kill(e);
                }
                for(Enemy other:new ArrayList<>(world.enemies))if(other!=e&&!other.dead&&distance(e.x,e.y,other.x,other.y)<22)hitEnemy(other,5,e.x-e.aimX,e.y-e.aimY,true);
                if(d<20)hurt(3,e.x,e.y);continue;
            }
            if(e.windup>0){e.windup--;if(e.windup==0){
                switch(e.kind){
                    case KNIGHT -> {e.charging=36;e.cooldown=100;sound("charge");}
                    case ARCHER,EMBER -> {shots.add(new Shot(e.x,e.y,e.aimX*2.7,e.aimY*2.7,e.id,e.kind==Kind.EMBER,false));e.cooldown=110;sound("arrow");}
                    case BOMB_SLIME -> {e.dead=true;explode(e.x,e.y,58,e.id);}
                    default -> {moveEnemy(e,e.aimX*12,e.aimY*12);if(d<30)hurt(1,e.x,e.y);e.cooldown=65;}
                }
            }continue;}
            if(d>300){if(e.age%150<50)moveEnemy(e,Math.sin(e.id+e.age*.02)*.2,Math.cos(e.id+e.age*.02)*.2);continue;}
            e.aimX=dx/d;e.aimY=dy/d;
            switch(e.kind){
                case GOBLIN -> {
                    Loot target=null;for(Loot l:loot)if(!l.heart&&l.value>0&&distance(e.x,e.y,l.x,l.y)<100){target=l;break;}
                    if(target!=null){double ld=Math.max(1,distance(e.x,e.y,target.x,target.y));moveEnemy(e,(target.x-e.x)/ld*1.3,(target.y-e.y)/ld*1.3);if(ld<12){e.stolen+=target.value;target.value=0;sound("steal");}}
                    else if(e.stolen>0||d<85){boolean moved=moveEnemy(e,-dx/d*1.15,-dy/d*1.15);
                        if(e.cooldown==0&&(!moved||d<100)){shots.add(new Shot(e.x,e.y,dx/d*3,dy/d*3,e.id,false,false));e.cooldown=55;sound("cointhrow");}}
                    else moveEnemy(e,dx/d*.85,dy/d*.85);
                    if(d<18){hurt(1,e.x,e.y);if(progress.coins>0&&e.cooldown==0){progress.coins--;e.stolen++;e.cooldown=60;dirty=true;}}
                }
                case KNIGHT -> {if(e.cooldown==0&&d<210){e.windup=38;sound("warning");}else if(d>45)moveEnemy(e,dx/d*.48,dy/d*.48);}
                case ARCHER,EMBER -> {if(e.cooldown==0&&d<280){e.windup=35;sound("draw");}}
                case BOMB_SLIME -> {if(d<47){e.windup=35;sound("fuse");}else moveEnemy(e,dx/d*.65,dy/d*.65);}
                case CHICKEN -> {moveEnemy(e,dx/d*1.7,dy/d*1.7);if(d<15)hurt(1,e.x,e.y);}
                default -> {
                    if(e.age%62<24)moveEnemy(e,dx/d*(e.kind==Kind.LARGE_SLIME?.6:.9),dy/d*(e.kind==Kind.LARGE_SLIME?.6:.9));
                    if(e.kind==Kind.STICKY_SLIME&&e.age%40==0)patches.add(new Patch(e.x,e.y,240,false));
                    if(d<22&&e.cooldown==0){e.windup=17;sound("squelch");}
                }
            }
        }
    }
    private void boss(Enemy e,double dx,double dy,double distance){
        if(areaAge<100||victoryTime>0)return;
        int nextPhase=e.hp>e.maxHp*2/3?1:e.hp>e.maxHp/3?2:3;
        if(nextPhase>e.phase){e.phase=nextPhase;e.stun=50;shake=15;sound("roar");
            say("THE PRETENDER",e.phase==2?"MY crown. MY castle. Stop touching things!":"Fine. EVERYONE gets a crown!");
            if(e.phase==3){world.enemies.add(new Enemy(nextEnemyId++,Kind.KNIGHT,200,240,false));world.enemies.add(new Enemy(nextEnemyId++,Kind.GOBLIN,560,240,false));}
        }
        if(e.charging>0){e.charging--;boolean wall=blocked(e.x+e.aimX*10,e.y+e.aimY*10,15);
            moveEnemy(e,e.aimX*4.7,e.aimY*4.7);if(tick%2==0)burst(e.x,e.y,3,0x8a6a76);
            if(wall){e.charging=0;e.stun=100;shake=13;sound("crash");for(Prop p:world.props)if(!p.broken&&distance(e.x,e.y,p.x,p.y)<42)breakProp(p);}
            if(distance<30)hurt(3,e.x,e.y);if(e.charging==0&&e.stun==0)e.stun=35;return;
        }
        if(e.windup>0){e.windup--;if(e.windup==0){int action=e.stolen++%3;
            if(action==0){e.charging=55;sound("charge");}
            else if(action==1){shots.add(new Shot(e.x,e.y,e.aimX*3.5,e.aimY*3.5,e.id,false,true));sound("throw");}
            else{explode(e.x,e.y,105,e.id);if(e.phase>=2){
                for(int i=0;i<10+e.phase*2;i++){double a=i*Math.PI*2/(10+e.phase*2);shots.add(new Shot(e.x,e.y,Math.cos(a)*1.8,Math.sin(a)*1.8,e.id,true,false));}
                if(e.phase==3)for(int i=0;i<6;i++)patches.add(new Patch(150+random.nextInt(460),150+random.nextInt(200),160,true));
            }}e.cooldown=e.phase==3?50:80;
        }return;}
        if(e.cooldown==0){e.aimX=dx/distance;e.aimY=dy/distance;e.windup=e.phase==3?30:45;sound("warning");}
        else if(distance>100)moveEnemy(e,dx/distance*.5,dy/distance*.5);
    }
    private void updateShots(){
        for(Shot s:new ArrayList<>(shots)){
            if(--s.life<=0)continue;s.x+=s.vx;s.y+=s.vy;
            if(distance(s.x,s.y,x,y)<(s.royal?18:9)){hurt(s.royal?3:1,s.x-s.vx*4,s.y-s.vy*4);if(!s.royal)s.life=0;}
            for(Enemy e:new ArrayList<>(world.enemies))if(!e.dead&&e.id!=s.owner&&distance(s.x,s.y,e.x,e.y)<e.radius()+5){
                hitEnemy(e,s.fire?4:2,s.x-s.vx,s.y-s.vy,true);if(s.fire)e.burn=120;s.life=0;break;}
            if(world.area==Area.MARSH&&distance(s.x,s.y,40*16+8,20*16+8)<16){
                if(progress.set("arrowBell")){progress.coins+=15;dirty=true;say("A CLEVER SHOT","The guardian's arrow rang the bell. A forgotten purse drops from the mill.");sound("secret");}s.life=0;
            }
            for(Prop p:world.props)if(!p.broken&&distance(s.x,s.y,p.x,p.y)<10){
                if(s.fire&&p.kind!=PropKind.CRACKED_WALL)p.burning=65;
                if(p.kind==PropKind.POT||p.kind==PropKind.GRASS)breakProp(p);
                if(p.solid()&&!s.royal)s.life=0;
            }
            if(s.life>0&&projectileBlocked(s.x,s.y)){
                if(s.royal&&(s.vx!=0||s.vy!=0)){s.vx=0;s.vy=0;s.life=Math.min(s.life,45);for(Enemy e:world.enemies)if(e.id==s.owner){e.stun=Math.max(e.stun,80);e.cooldown=110;}sound("crash");}
                else if(!s.royal){s.life=0;if(s.fire)patches.add(new Patch(s.x,s.y,110,true));}
            }
        }
        shots.removeIf(s->s.life<=0);
    }
    private void explode(double px,double py,double radius,int owner){
        burst(px,py,40,0xffb769);shake=10;sound("explode");
        if(distance(px,py,x,y)<radius)hurt(3,px,py);
        for(Enemy e:new ArrayList<>(world.enemies))if(!e.dead&&e.id!=owner&&distance(px,py,e.x,e.y)<radius)hitEnemy(e,8,px,py,true);
        for(Prop p:world.props)if(!p.broken&&distance(px,py,p.x,p.y)<radius)breakProp(p);
    }
    private void collect(Loot item){if(item.value<=0)return;
        if(item.heart){health=Math.min(progress.maxHealth(),health+item.value);sound("heal");}
        else{progress.coins+=item.value;dirty=true;sound("coin");}item.value=0;burst(item.x,item.y,5,item.heart?0xd9959b:0xe8c67b);
    }
    private void updateLoot(){for(Loot l:loot){l.age++;double d=distance(l.x,l.y,x,y);if(d<42&&d>1){l.x+=(x-l.x)/d*2.8;l.y+=(y-l.y)/d*2.8;}if(d<12)collect(l);}loot.removeIf(l->l.value<=0);}
    private void updatePatches(){for(Patch p:patches){if(p.life<=0)continue;p.life--;if(distance(x,y,p.x,p.y)<18){if(p.fire)hurt(1,p.x,p.y);else slowTime=25;}
        if(p.fire&&tick%30==0)for(Enemy e:new ArrayList<>(world.enemies))if(!e.dead&&distance(e.x,e.y,p.x,p.y)<20){e.hp--;e.burn=40;if(e.hp<=0)kill(e);}}
        patches.removeIf(p->p.life<=0);
    }
    public void updateMechanisms(){
        if(bellTimer>0&&--bellTimer==0&&!progress.has("bells"))bellStep=0;
        if(world.area==Area.TEMPLE&&!progress.has("light")){
            boolean aligned=statues[0]==1&&statues[1]==2&&statues[2]==3;
            boolean blockedLight=distanceToSegment(x,y,264,328,392,328)<9||distanceToSegment(x,y,392,200,392,328)<9||distanceToSegment(x,y,520,328,392,328)<9;
            if(aligned&&!blockedLight)lightCharge++;else lightCharge=Math.max(0,lightCharge-3);
            if(lightCharge>=90){progress.set("light");dirty=true;sound("secret");say("THREE SUNS, ONE CROWN","The sanctuary is open. Light returns to the royal seal.");}
        }
        if(world.area==Area.KEEP){
            if(x>151&&x<183&&y>70&&y<186){shadowTime=180;if(!progress.has("shadowHint")){progress.set("shadowHint");say("YOUR SHADOW","The light falters behind you. The treasury spikes sleep for a moment.");}}
            else if(shadowTime>0)shadowTime--;
            if(x>152&&x<218&&y>82&&y<180&&shadowTime==0&&tick%20==0)hurt(1,168,100);
            if(!progress.has("heavyPlate"))for(Enemy e:world.enemies)if(!e.dead&&e.kind==Kind.KNIGHT&&distance(e.x,e.y,168,296)<21){progress.set("heavyPlate");progress.coins+=15;dirty=true;sound("secret");say("A HEAVY FOOTSTEP","The knight opens the pressure seal. Fifteen forgotten coins are yours.");break;}
        }
        if(world.area==Area.MARSH&&waterLevel==0&&!progress.has("won")&&world.tile((int)x/16,(int)y/16)==WorldMap.WATER&&tick%90==0)slowTime=30;
    }
    public static double distanceToSegment(double px,double py,double ax,double ay,double bx,double by){
        double dx=bx-ax,dy=by-ay,t=Math.max(0,Math.min(1,((px-ax)*dx+(py-ay)*dy)/(dx*dx+dy*dy)));
        return Math.hypot(px-ax-t*dx,py-ay-t*dy);
    }
    public String prompt(){
        if(dead)return "ENTER  Return to the fountain";
        if(world.area==Area.KEEP&&progress.has("crypt")&&distance(x,y,568,376)<32)return "E  Descend into the forgotten treasury";
        Landmark landmark=nearestLandmark();if(landmark!=null)return "E  "+interactionLabel(landmark);
        for(Portal p:world.portals)if(distance(x,y,p.x(),p.y())<34)return "E  "+p.label();
        if(world.area==Area.KEEP&&distance(x,y,568,376)<58&&!progress.has("crypt"))return "A hollow footstep. X  Ground slam";
        return "";
    }
    public String interactionLabel(Landmark l){
        return switch(l.id()){
            case "smith" -> !progress.has("smithRescued")?"Cold forge — rescue the blacksmith":!progress.has("forge")?"Restore forge — "+price(20,true)+" coins":progress.weapon<3?"Upgrade blade — "+price(18+progress.weapon*14,false)+" coins":progress.hearts<5?"Heart container — "+price(35,false)+" coins":"Speak to the blacksmith";
            case "cook" -> !progress.has("cookRescued")?"Empty garden — rescue the cook":!progress.has("garden")?"Restore garden — "+price(20,true)+" coins":"Royal meal — "+price(8,false)+" coins";
            case "cartographer" -> !progress.has("mapRescued")?"Silent tower — rescue the cartographer":!progress.has("tower")?"Restore bell tower — "+price(25,true)+" coins":"Ask about secrets";
            default -> l.label();
        };
    }
    public Landmark nearestLandmark(){Landmark result=null;double best=33;
        for(Landmark l:world.landmarks){double d=distance(x,y,l.x(),l.y());if(d<best){best=d;result=l;}}
        return result;
    }
    public void interact(){
        if(world.area==Area.KEEP&&progress.has("crypt")&&distance(x,y,568,376)<32){enter(Area.CRYPT);return;}
        Landmark nearby=nearestLandmark();if(nearby!=null){useLandmark(nearby.id());return;}
        for(Portal p:world.portals)if(distance(x,y,p.x(),p.y())<34){
            if(p.target()==Area.THRONE&&(!progress.has("ruby")||!progress.has("emerald")||!progress.has("sapphire"))){say("THE SEALED THRONE","Recover all three crown jewels before challenging the Pretender.");return;}
            if(world.area==Area.THRONE&&!progress.has("won")){say("THE PRETENDER","Leaving already? A king stands his ground!");return;}
            if(victoryTime>100)return;
            enter(p.target());return;
        }
    }
    public void useLandmark(String id){
        switch(id){
            case "mentor" -> {
                if(!progress.has("training")){progress.set("training");dirty=true;say("SIR BRAMBLE","J: slash. SPACE: dodge. Q: throw / recall your crown. X: slam. Your third slash can now break a knight's guard.");sound("upgrade");}
                else say("SIR BRAMBLE","Catch a RETURNING crown with Q just before it reaches you. Your next slash gains royal power. H opens the journal.");
            }
            case "fountain" -> {health=progress.maxHealth();stamina=100;save();sound("heal");say("THE FOUNTAIN",saveFailed?"Restored to full health. Your existing unreadable save remains protected.":"Health restored. Your kingdom's progress is saved. Explore the paths to the north, east and west.");}
            case "decree" -> {progress.decree=(progress.decree+1)%3;dirty=true;sound("decree");say("BY ROYAL DECREE",decreeDescription());}
            case "smith" -> shopSmith();case "cook" -> shopCook();case "cartographer" -> shopMap();
            case "rescueSmith" -> rescue("smithRescued","THE BLACKSMITH","My forge still stands in the west courtyard. Bring twenty coins and I will light it again.");
            case "rescueCook" -> rescue("cookRescued","THE COOK","A royal garden! A kitchen! Anything but mushroom soup. Find me in the eastern courtyard.");
            case "rescueMap" -> rescue("mapRescued","THE CARTOGRAPHER","I have mapped these ruins. Rescue the bell tower and I will mark their secrets. M opens your atlas.");
            case "ruby" -> {if(progress.has("bells"))grantJewel(Jewel.RUBY,"ruby");else say("SEALED","The three bells must answer first.");}
            case "emerald" -> {if(waterLevel==2||progress.has("won"))grantJewel(Jewel.EMERALD,"emerald");else say("SEALED","The waterwheel needs the high flood.");}
            case "sapphire" -> {if(progress.has("light"))grantJewel(Jewel.SAPPHIRE,"sapphire");else say("SEALED","Unite the three sunbeams.");}
            case "ropeBridge" -> say("THE SUPPORT ROPE","Throw your crown north to cut the rope and drop a bridge across the water.");
            case "waterLever" -> {waterLevel=(waterLevel+1)%3;sound("lever");say("MOSSWATER MILL",new String[]{"LOW: the submerged passage is exposed. Mud slows your feet.","MID: the bridge floats, but the waterwheel is still sleeping.","HIGH: the wheel turns and the island gate opens. Follow the floating bridge."}[waterLevel]);}
            case "lowCache" -> {if(waterLevel==0)treasure("lowCache",20,"Beneath the waterline: twenty coins and an old royal seal.");else say("UNDER THE WATER","Drain the mill to expose this passage.");}
            case "arrowBell" -> say("THE PROJECTILE BELL",progress.has("arrowBell")?"The mill remembers your clever shot.":"Place this bell between yourself and the mushroom archer. Dodge at the last moment.");
            case "archerBell" -> {if(world.enemies.stream().noneMatch(e->!e.dead&&e.kind==Kind.ARCHER)){world.enemies.add(new Enemy(nextEnemyId++,Kind.ARCHER,648,168,false));sound("bell0");}say("MILL GUARDIAN","The old guardian answers the bell. Its arrows can reach what your hands cannot.");}
            case "statue0","statue1","statue2" -> {int index=id.charAt(6)-'0';statues[index]=(statues[index]+1)%4;sound("stone");say("SUN STATUE",new String[]{"North","East","South","West"}[statues[index]]+". Guide all three beams into the central seal; keep your shadow out of their way.");}
            case "sunSeal" -> say("THE ROYAL SEAL",progress.has("light")?"All three suns are awake.":"Three statues face one seal. Your body can interrupt the beams.");
            case "pedestal" -> {
                if(progress.has("release")){say("CROWN GATE","The inner release has freed the mechanism permanently.");break;}
                crown=null;crownResting=!crownResting;sound("lever");say("CROWN GATE",crownResting?"Your crown powers the gate. Cross it with sword and courage, then pull the inner release.":"The crown returns to your hand. The gate closes.");}
            case "release" -> {progress.set("release");crownResting=false;crown=null;dirty=true;sound("secret");say("THE INNER RELEASE","The gate stays open. Your crown returns. The Pretender waits beyond the northern door.");}
            case "shadowCache" -> {if(shadowTime>0)treasure("shadowCache",25,"Your shadow held back the trap. The treasury yields twenty-five coins.");else say("TREASURY OF SHADOWS","Step through the light below the treasury. Your shadow puts the spikes to sleep briefly.");}
            case "heavyPlate" -> say("PRESSURE SEAL",progress.has("heavyPlate")?"A knight's heavy foot has opened this seal.":"Too heavy for a king. Bait a charging knight across the seal.");
            case "hollowFloor" -> say("HOLLOW STONE",progress.has("crypt")?"E: descend into the forgotten treasury.":"A draft tugs at your cape. A ground slam might reveal what lies beneath.");
            case "chargeCache" -> treasure("chargeCache",20,"A forgotten goblin hoard. Brute force has its uses.");
            case "cryptTreasure" -> {
                if(nearEnemies(95)){say("THE OLD KING'S HEART","The treasury's guardians are still standing.");break;}
                if(progress.set("cryptHeart")){progress.hearts=Math.min(5,progress.hearts+1);health=progress.maxHealth();progress.coins+=25;dirty=true;sound("secret");say("THE OLD KING'S HEART","A permanent heart container, twenty-five coins, and the blessing of a forgotten king.");}else say("THE OLD KING'S HEART","The old king has already given you his blessing.");}
            case "victoryThrone" -> say("THE HOLLOW THRONE",progress.has("won")?"You could sit here forever. Your people are waiting at home.":"An oversized chair for an undersized soul.");
            case "bellHint","bell0","bell1","bell2" -> say("THE THREE BELLS","Stand west of the bells. Throw EAST through 1, 2, 3 in one flight. Time it past the moving shutter.");
            default -> say("KINGDOMKING","There is always another secret.");
        }
        if(dirty)save();
    }
    private boolean nearEnemies(double radius){return world.enemies.stream().anyMatch(e->!e.dead&&distance(x,y,e.x,e.y)<radius);}
    private void rescue(String flag,String name,String text){
        if(progress.has(flag)){say(name,"I will meet you back at the castle.");return;}
        if(nearEnemies(100)){say(name,"The guards are too close. Clear this corner first!");return;}
        progress.set(flag);progress.coins+=12;dirty=true;sound("rescue");say(name,text);
    }
    public void grantJewel(Jewel jewel,String flag){
        if(progress.has(flag)){say("CROWN JEWEL","This jewel is already yours. U / I cycle your two equipped slots.");return;}
        progress.set(flag);progress.jewels|=1<<jewel.ordinal();
        if(progress.slotA==0)progress.slotA=jewel.ordinal();else if(progress.slotB==0)progress.slotB=jewel.ordinal();
        progress.coins+=15;dirty=true;sound("jewel");burst(x,y,40,0xeccf83);
        say("CROWN JEWEL RECOVERED",switch(jewel){case RUBY->"Ruby: your crown ignites enemies and wood. Two sockets, three jewels: U / I change your build.";
            case EMERALD->"Emerald: a crown that struck an enemy heals you on return. Perfect catches also restore health.";
            case SAPPHIRE->"Sapphire: your crown ricochets from walls up to three times. Try reaching switches around corners.";default->"";});
    }
    private void cycleJewel(boolean first){int current=first?progress.slotA:progress.slotB,other=first?progress.slotB:progress.slotA;
        for(int i=1;i<=4;i++){int next=(current+i)%4;if(next==0||((progress.jewels&(1<<next))!=0&&next!=other)){
            if(first)progress.slotA=next;else progress.slotB=next;dirty=true;sound("equip");say("CROWN SOCKETS","Equipped: "+Jewel.values()[progress.slotA]+" / "+Jewel.values()[progress.slotB]+". U / I change the two slots.");return;}}
    }
    private void treasure(String flag,int coins,String text){if(progress.set(flag)){progress.coins+=coins;dirty=true;sound("secret");say("SECRET FOUND",text);}else say("SECRET FOUND","You have already claimed this treasure.");}
    public int price(int base,boolean restoration){return progress.decree==1?(restoration?base+5:Math.max(1,base*3/4)):base;}
    private boolean spend(int amount){if(progress.coins<amount){say("ROYAL TREASURY","You need "+amount+" coins. Your purse holds "+progress.coins+".");return false;}progress.coins-=amount;dirty=true;return true;}
    private void shopSmith(){
        if(!progress.has("smithRescued")){say("COLD FORGE","The blacksmith is captive in Briarfield, north of the castle.");return;}
        if(!progress.has("forge")){if(spend(price(20,true))){progress.set("forge");sound("restore");say("THE FORGE IS LIT","Hammer, heat, hope. The blacksmith is home. Speak again to improve your blade.");}return;}
        if(progress.weapon<3){int cost=price(18+progress.weapon*14,false);if(spend(cost)){progress.weapon++;sound("upgrade");say("THE BLACKSMITH","Blade improved to level "+progress.weapon+". Every sword and crown strike now hits harder.");}}
        else if(progress.hearts<5){if(spend(price(35,false))){progress.hearts++;health=progress.maxHealth();sound("upgrade");say("THE BLACKSMITH","A royal heart container. You can endure two more points of damage.");}}
        else say("THE BLACKSMITH","Your arms and armor are fit for a true king.");
    }
    private void shopCook(){
        if(!progress.has("cookRescued")){say("OVERGROWN GARDEN","The cook is stranded at Mosswater Mill, east of the castle.");return;}
        if(!progress.has("garden")){if(spend(price(20,true))){progress.set("garden");sound("restore");say("THE GARDEN RETURNS","Flowers, butterflies, and an alarming amount of soup. Speak again for a royal meal.");}return;}
        if(spend(price(8,false))){foodTime=60*180;health=progress.maxHealth();sound("heal");say("THE ROYAL COOK","Three minutes of hearty food: faster stamina recovery and full health. Go cause trouble responsibly.");}
    }
    private void shopMap(){
        if(!progress.has("mapRescued")){say("SILENT BELL TOWER","The cartographer is trapped in the Temple of Three Suns, west of the castle.");return;}
        if(!progress.has("tower")){if(spend(price(25,true))){progress.set("tower");sound("restore");say("THE BELLS RETURN","Your atlas now marks secrets. The tower's bells carry across the kingdom.");}return;}
        say("THE CARTOGRAPHER","M opens your atlas. Look for hollow floors in the keep, a submerged passage in the marsh, and treasure behind a brittle wall.");
    }
    public String decreeDescription(){return switch(progress.decree){case 1->"NO TAXES: supplies cost 25% less; rebuilding costs five extra coins. Change this decree whenever you wish.";
        case 2->"ROYAL HUNT: new expeditions contain tougher enemies with richer rewards. Revisit a cleared region to hunt again.";
        default->"FAIR RULE: ordinary prices, ordinary dangers. Your people appreciate a quiet afternoon.";};}
    private void particle(double px,double py,double vx,double vy,int life,int color,int size){if(particles.size()<450)particles.add(new Particle(px,py,vx,vy,life,color,size));}
    public void burst(double px,double py,int count,int color){for(int i=0;i<count;i++){double angle=random.nextDouble()*Math.PI*2,speed=random.nextDouble()*2+.2;particle(px,py,Math.cos(angle)*speed,Math.sin(angle)*speed,15+random.nextInt(20),color,1+random.nextInt(3));}}
    private void updateParticles(){for(Particle p:particles){p.x+=p.vx;p.y+=p.vy;p.vx*=.93;p.vy*=.93;p.life--;}particles.removeIf(p->p.life<=0);}
    public static double distance(double ax,double ay,double bx,double by){return Math.hypot(ax-bx,ay-by);}
}
