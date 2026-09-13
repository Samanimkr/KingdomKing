package game.rain.adventure;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/** Original procedural soundtrack and effects; no downloaded or licensed audio assets. */
public final class AudioEngine implements AutoCloseable {
    private static final int RATE=22050;
    private static final AudioFormat FORMAT=new AudioFormat(RATE,16,1,true,false);
    private final ConcurrentLinkedQueue<String> pending=new ConcurrentLinkedQueue<>();
    private final List<Voice> voices=new ArrayList<>();
    private final Random random=new Random(733);
    private volatile boolean running,muted;
    private volatile int restoration;
    private volatile boolean boss,victory;
    private SourceDataLine line;
    private Thread thread;
    private long frame;
    private long lastBeat=-1;
    private int victorySamples;

    public boolean start(){
        try {
            DataLine.Info info=new DataLine.Info(SourceDataLine.class,FORMAT);
            line=(SourceDataLine)AudioSystem.getLine(info);line.open(FORMAT,4096);line.start();
            running=true;thread=new Thread(this::loop,"KingdomKing audio");thread.setDaemon(true);thread.start();return true;
        }catch(LineUnavailableException|IllegalArgumentException|SecurityException e){if(line!=null)line.close();return false;}
    }
    public void play(String event){if(pending.size()<64)pending.add(event);}
    public void configure(boolean muted,int restoration,boolean boss,boolean victory){this.muted=muted;this.restoration=restoration;this.boss=boss;this.victory=victory;}
    private void loop(){byte[] buffer=new byte[1024];
        try{while(running){for(int i=0;i<buffer.length;i+=2){double sample=nextSample();short pcm=(short)(Math.max(-.95,Math.min(.95,muted?0:sample))*32767);buffer[i]=(byte)pcm;buffer[i+1]=(byte)(pcm>>8);}line.write(buffer,0,buffer.length);}}
        catch(IllegalStateException ignored){/* Closing the audio line releases a pending write. */}
        finally{running=false;}
    }
    private double nextSample(){
        if(frame%128==0){String event;while((event=pending.poll())!=null){if(event.equals("bossfall"))victorySamples=RATE*2;effect(event);}}
        double beatSeconds=boss?.32:.48;long beat=(long)(frame/(RATE*beatSeconds));
        if(beat!=lastBeat){lastBeat=beat;if(victorySamples==0)music((int)(beat%32));}
        if(victorySamples>0)victorySamples--;
        double out=0;
        for(Iterator<Voice> iterator=voices.iterator();iterator.hasNext();){Voice voice=iterator.next();out+=voice.sample(random);if(voice.age>=voice.length)iterator.remove();}
        frame++;return Math.tanh(out*.72);
    }
    private void music(int beat){
        int[] melody={0,7,12,10,7,3,5,7,0,7,15,12,10,7,3,5};
        int[] roots={45,41,48,43};int root=roots[(beat/8)%4];
        double pitch=midi((victory?60:57)+melody[(beat/2)%16]);
        if(beat%2==0)add(pitch,pitch,.58,.085,0);
        if(beat%4==0)add(midi(root),midi(root),1.3,.055,1);
        if(restoration>=1&&beat%2==1)add(midi(root+19),midi(root+19),.35,.025,0);
        if(restoration>=2&&beat%4==2)add(midi(root+24),midi(root+24),.7,.032,0);
        if(restoration>=3&&beat%4==0){add(85,40,.12,.09,1);add(2200,1300,.035,.015,2);}
        if(restoration>=4&&beat%8==0){add(midi(root+31),midi(root+31),1.8,.026,0);}
        if(boss){if(beat%2==0)add(100,45,.15,.10,1);if(beat%4==3)add(1800,600,.06,.035,2);}
    }
    private static double midi(int note){return 440*Math.pow(2,(note-69)/12.0);}
    private void add(double start,double end,double seconds,double volume,int type){if(voices.size()<32)voices.add(new Voice(start,end,(int)(seconds*RATE),volume,type));}
    private void effect(String id){
        switch(id){
            case "slash" -> {add(1500,300,.09,.18,2);add(330,160,.08,.1,1);}
            case "heavy" -> {add(1100,90,.16,.23,2);add(150,45,.14,.2,1);}
            case "hit" -> {add(160,50,.09,.21,1);add(900,300,.04,.18,2);}
            case "block" -> {add(960,860,.13,.15,0);add(1460,1400,.07,.08,0);}
            case "throw_ruby" -> {effect("throw");add(800,150,.12,.055,2);}
            case "throw_sapphire" -> {effect("throw");add(1760,1760,.22,.045,0);}
            case "throw_emerald" -> {effect("throw");add(660,880,.25,.05,0);}
            case "hollow" -> {add(110,95,.20,.08,0);add(230,200,.14,.045,0);}
            case "throw" -> {add(850,430,.18,.12,0);add(1300,1400,.07,.07,0);}
            case "recall" -> add(400,1200,.22,.095,0);
            case "catch" -> {add(1320,1320,.13,.13,0);add(1980,1980,.09,.06,0);}
            case "perfect" -> {add(880,1760,.2,.13,0);add(1320,2640,.22,.08,0);}
            case "dash" -> {add(700,180,.13,.10,2);add(300,800,.07,.05,0);}
            case "jump" -> add(170,550,.20,.11,1);
            case "slam","explode","crash","bossfall" -> {add(140,25,.48,.3,1);add(800,90,.25,.24,2);}
            case "hurt" -> {add(300,110,.25,.14,1);add(380,130,.19,.07,1);}
            case "fall" -> {add(330,80,.65,.13,1);add(440,110,.55,.07,0);}
            case "coin","cointhrow" -> add(1480,1900,.10,.09,0);
            case "steal" -> add(1000,450,.13,.10,0);
            case "grassstep","cut" -> add(800,500,.04,.045,2);
            case "woodstep" -> add(200,95,.06,.065,1);
            case "step" -> {add(400,180,.028,.05,2);add(170,150,.035,.045,0);}
            case "splash","squelch" -> {add(250,90,.09,.05,0);add(800,250,.08,.04,2);}
            case "arrow","draw" -> add(1200,350,.11,.075,2);
            case "charge","roar" -> {add(110,170,.4,.16,1);add(140,210,.3,.08,1);}
            case "warning","fuse" -> add(680,740,.18,.075,1);
            case "break" -> {add(600,70,.12,.1,2);add(240,80,.09,.07,1);}
            case "bell0" -> bell(523.25);case "bell1" -> bell(659.25);case "bell2" -> bell(783.99);
            case "ricochet" -> bell(1050);
            case "secret","jewel","restore","rescue","upgrade","victory" -> {bell(523.25);add(659.25,659.25,.8,.10,0);add(783.99,783.99,1,.10,0);if(id.equals("victory"))add(1046.5,1046.5,1.4,.08,0);}
            case "heal" -> {add(440,660,.35,.07,0);add(660,880,.4,.05,0);}
            case "lever","stone" -> {add(160,100,.16,.07,2);add(120,110,.15,.07,1);}
            case "defeat" -> add(500,130,.18,.1,1);
            case "bird" -> {add(1300,1900,.07,.025,0);add(1800,1500,.09,.02,0);}
            default -> add(660,880,.08,.055,0);
        }
    }
    private void bell(double hz){add(hz,hz,1.0,.12,0);add(hz*2.76,hz*2.76,.30,.045,0);}
    @Override public void close(){running=false;if(line!=null){line.stop();line.close();}if(thread!=null&&thread!=Thread.currentThread())try{thread.join(1000);}catch(InterruptedException e){Thread.currentThread().interrupt();}}
    private static final class Voice {
        final double start,end,volume;final int length,type;int age;double phase;
        Voice(double start,double end,int length,double volume,int type){this.start=start;this.end=end;this.length=length;this.volume=volume;this.type=type;}
        double sample(Random random){double t=(double)age/length;phase+=2*Math.PI*(start+(end-start)*t)/RATE;
            double wave=type==2?random.nextDouble()*2-1:type==1?Math.sin(phase)+Math.sin(phase*3)*.22:Math.sin(phase);
            double envelope=Math.min(1,age/80.0)*Math.pow(1-t,type==2?2:1.8);age++;return wave*volume*envelope;}
    }
    /** Offline verification artifact, independent of the host sound device. */
    public static void writeDemo(Path path) throws IOException {
        AudioEngine synth=new AudioEngine();synth.restoration=3;
        String[] events={"throw","recall","catch","slash","hit","dash","slam","jewel","victory"};
        byte[] bytes=new byte[RATE*8*2];
        for(int i=0;i<bytes.length/2;i++){if(i%(RATE*3/4)==0)synth.effect(events[Math.min(events.length-1,i/(RATE*3/4))]);short value=(short)(synth.nextSample()*32767);bytes[i*2]=(byte)value;bytes[i*2+1]=(byte)(value>>8);}
        try(AudioInputStream input=new AudioInputStream(new ByteArrayInputStream(bytes),FORMAT,bytes.length/2)){AudioSystem.write(input,AudioFileFormat.Type.WAVE,path.toFile());}
    }
}
