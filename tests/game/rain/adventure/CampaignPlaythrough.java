package game.rain.adventure;

import game.rain.adventure.Actors.*;
import java.util.ArrayDeque;
import java.util.Arrays;

/** Deterministic playthrough using player inputs, without changing health or progression. */
public final class CampaignPlaythrough {
    private final Adventure a=new Adventure(null);
    private int frames;
    public static void main(String[] args){new CampaignPlaythrough().run();}
    private void run(){
        go(536,392);use();travel(392,40,Area.MEADOW);
        go(264,376);boolean solved=false;
        for(int attempt=0;attempt<12&&!solved;attempt++){
            fightNearby();go(264,376);tick(1,0,false,false,false,false,false);tick(0,0,false,true,false,false,false);
            for(int i=0;i<95;i++)tick(0,0,false,false,false,false,false);solved=a.progress.has("bells");
        }
        require(solved,"crown bells");go(392,200);use();require(a.progress.has("smithRescued"),"blacksmith rescue");
        go(456,200);use();require(a.progress.has("ruby"),"ruby");travel(392,472,Area.CASTLE);
        go(168,200);use();use();go(392,328);use();travel(728,264,Area.MARSH);
        go(152,376);use();require(a.waterLevel==2,"high flood");
        go(552,168);use();require(a.progress.has("cookRescued"),"cook rescue");go(600,168);use();require(a.progress.has("emerald"),"emerald");
        travel(40,264,Area.CASTLE);go(600,200);use();go(392,328);use();travel(40,264,Area.TEMPLE);
        go(264,350);use();go(392,224);use();use();go(520,350);use();use();use();
        go(392,370);for(int i=0;i<110;i++)tick(0,0,false,false,false,false,false);require(a.progress.has("light"),"sun seal");
        go(376,104);use();require(a.progress.has("sapphire"),"sapphire");go(440,104);use();require(a.progress.has("mapRescued"),"cartographer rescue");
        travel(392,472,Area.CASTLE);go(200,392);use();go(168,200);use();go(392,328);use();
        travel(392,472,Area.KEEP);go(392,328);use();require(a.crownResting,"pedestal");go(392,136);use();require(a.progress.has("release"),"inner release");
        travel(392,40,Area.THRONE);for(int i=0;i<16000&&!a.progress.has("won");i++){combatStep();if(a.dead)fail("died fighting the Pretender");}
        require(a.progress.has("won"),"boss victory");
        System.out.println("PASS full-input campaign: three puzzles, three rescues, three jewels, castle upgrades and boss victory in "+frames+" simulation frames; health="+a.health+", coins="+a.progress.coins);
    }
    private void use(){tick(0,0,false,false,false,false,true);for(int i=0;i<5;i++)tick(0,0,false,false,false,false,false);}
    private void travel(double x,double y,Area expected){go(x,y);use();require(a.world.area==expected,"travel to "+expected);System.out.println("PLAYTEST "+expected+" frame="+frames+" health="+a.health+" coins="+a.progress.coins);}
    private void go(double x,double y){
        for(int i=0;i<12000;i++){
            if(a.dead)fail("died travelling to "+x+","+y);
            Enemy danger=danger();if(danger!=null){combatStep();continue;}
            if(Adventure.distance(a.x,a.y,x,y)<9)return;
            double[] next=waypoint(x,y);double dx=next[0]-a.x,dy=next[1]-a.y;
            tick(dx,dy,false,false,false,false,false);
        }fail("navigation timed out to "+x+","+y+" from "+a.x+","+a.y);
    }
    private void fightNearby(){for(int i=0;i<6000&&danger()!=null;i++){combatStep();if(a.dead)fail("died clearing approach");}}
    private Enemy danger(){Enemy nearest=null;double best=180;
        for(Enemy e:a.world.enemies)if(!e.dead){double d=Adventure.distance(a.x,a.y,e.x,e.y);if(d<best&&line(e.x,e.y)){nearest=e;best=d;}}
        return nearest;
    }
    private boolean line(double x,double y){double distance=Adventure.distance(a.x,a.y,x,y);for(double t=8;t<distance;t+=8)if(a.projectileBlocked(a.x+(x-a.x)*t/distance,a.y+(y-a.y)*t/distance))return false;return true;}
    private void combatStep(){
        Enemy e=danger();if(e==null){for(Enemy candidate:a.world.enemies)if(!candidate.dead){e=candidate;break;}}
        if(e==null){tick(0,0,false,false,false,false,false);return;}
        double dx=e.x-a.x,dy=e.y-a.y,d=Math.max(1,Math.hypot(dx,dy));
        boolean dodge=e.windup>0&&e.windup<9&&d<130||e.charging>0&&d<75;
        double mx=dx/d,my=dy/d;if(dodge){mx=-dy/d;my=dx/d;}
        boolean throwNow=a.crown==null&&d>45&&frames%20==0||a.crown!=null&&a.crown.returning&&Adventure.distance(a.x,a.y,a.crown.x,a.crown.y)<35;
        tick(mx,my,d<55,throwNow,dodge,d<65&&a.stamina>44&&a.slamCooldown==0,false);
    }
    private double[] waypoint(double goalX,double goalY){
        int start=(int)a.x/16+(int)a.y/16*48,goal=(int)goalX/16+(int)goalY/16*48;
        if(start==goal)return new double[]{goalX,goalY};
        int[] parent=new int[48*32];Arrays.fill(parent,-1);ArrayDeque<Integer> queue=new ArrayDeque<>();queue.add(start);parent[start]=start;
        while(!queue.isEmpty()&&parent[goal]<0){int tile=queue.remove();for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){int x=tile%48+d[0],y=tile/48+d[1],next=x+y*48;
            if(x>0&&y>0&&x<47&&y<31&&parent[next]<0&&!a.solid(x*16+8,y*16+8)){parent[next]=tile;queue.add(next);}}}
        if(parent[goal]<0)fail("no route to "+goalX+","+goalY);
        int next=goal;while(parent[next]!=start)next=parent[next];return new double[]{next%48*16+8,next/48*16+8};
    }
    private void tick(double x,double y,boolean attack,boolean crown,boolean dash,boolean slam,boolean interact){
        double length=Math.hypot(x,y);if(length>1){x/=length;y/=length;}
        a.update(new Adventure.Input(x,y,false,attack,crown,dash,slam,interact,false,false));a.sounds.clear();frames++;
        if(frames>150000)fail("playthrough exceeded its frame budget");
    }
    private void require(boolean condition,String milestone){if(!condition)fail(milestone);}
    private void fail(String message){throw new AssertionError(message+" | area="+a.world.area+", frame="+frames+", pos="+(int)a.x+","+(int)a.y+", health="+a.health+", flags="+a.progress.flags);}
}
