package game.rain;

import game.rain.adventure.Adventure;
import game.rain.adventure.AdventureRenderer;
import game.rain.adventure.Area;
import game.rain.adventure.AudioEngine;
import game.rain.adventure.SaveStore;
import game.rain.input.Keyboard;
import java.awt.Canvas;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.KeyboardFocusManager;
import java.awt.KeyEventDispatcher;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.LockSupport;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.MenuSelectionManager;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/** Desktop host for the campaign. The restored 2017 prototype remains in --classic mode. */
@SuppressWarnings("serial")
public final class Game extends Canvas implements Runnable {
    public static final int WIDTH=480,HEIGHT=270,SCALE=2;
    public static final String TITLE="KingdomKing — The Stolen Crown";
    private static final long STEP=1_000_000_000L/60;
    private final Keyboard key=new Keyboard();
    private final Adventure adventure;
    private final AdventureRenderer renderer=new AdventureRenderer();
    private final AudioEngine audio=new AudioEngine();
    private final ConcurrentLinkedQueue<Runnable> commands=new ConcurrentLinkedQueue<>();
    private volatile boolean running,focused=true,clickAttack,clickCrown;
    private boolean menu=true,paused,atlas,journal,muted,confirmNew;
    private Thread thread;
    private JFrame frame;
    private JButton playButton;
    private boolean playButtonShown=true;
    private String playButtonLabel="Begin / continue adventure";
    private KeyEventDispatcher keyDispatcher;

    public Game(){this(GraphicsEnvironment.isHeadless()?null:new SaveStore(SaveStore.defaultPath()));}
    public Game(SaveStore save){
        adventure=new Adventure(save);setPreferredSize(new Dimension(WIDTH*SCALE,HEIGHT*SCALE));
        setFocusable(true);setFocusTraversalKeysEnabled(false);setIgnoreRepaint(true);
        addKeyListener(key);addFocusListener(key);
        addFocusListener(new FocusAdapter(){
            @Override public void focusLost(FocusEvent e){focused=false;clickAttack=false;clickCrown=false;}
            @Override public void focusGained(FocusEvent e){focused=true;}
        });
        addMouseListener(new MouseAdapter(){
            @Override public void mousePressed(MouseEvent e){requestFocusInWindow();if(menu&&e.getButton()==MouseEvent.BUTTON1){commands.add(()->{if(confirmNew)adventure.newCampaign();begin();});return;}if(e.getButton()==MouseEvent.BUTTON1)clickAttack=true;if(e.getButton()==MouseEvent.BUTTON3)clickCrown=true;}
        });
    }
    public synchronized void start(){if(thread!=null&&thread.isAlive())return;running=true;thread=new Thread(this,"KingdomKing campaign");thread.start();}
    public void stop(){Thread stopping;synchronized(this){running=false;stopping=thread;}
        if(stopping!=null&&stopping!=Thread.currentThread()){stopping.interrupt();try{stopping.join(2000);}catch(InterruptedException e){Thread.currentThread().interrupt();}}
        adventure.save();audio.close();
        if(keyDispatcher!=null)KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(keyDispatcher);
    }
    @Override public void run(){long previous=System.nanoTime(),accumulator=0;
        try{while(running){long now=System.nanoTime();accumulator+=Math.min(now-previous,STEP*5);previous=now;
            if(accumulator>=STEP){while(accumulator>=STEP&&running){update();accumulator-=STEP;}if(running)render();}
            LockSupport.parkNanos(Math.max(1,STEP-accumulator-(System.nanoTime()-now)));
        }}catch(RuntimeException e){e.printStackTrace();SwingUtilities.invokeLater(()->{JOptionPane.showMessageDialog(frame,e.toString(),"KingdomKing error",JOptionPane.ERROR_MESSAGE);if(frame!=null)frame.dispose();});}
        finally{running=false;audio.close();}
    }
    public void begin(){menu=false;paused=false;atlas=false;journal=false;confirmNew=false;renderer.confirmNew=false;}
    public void update(){
        Runnable command;while((command=commands.poll())!=null)command.run();
        synchronized(key){
            key.update();
            if(key.consumePress(KeyEvent.VK_F))muted=!muted;
            if(key.consumePress(KeyEvent.VK_F2))renderer.reducedMotion=!renderer.reducedMotion;
            if(key.consumePress(KeyEvent.VK_ESCAPE)){
                if(confirmNew){confirmNew=false;renderer.confirmNew=false;}
                else if(atlas||journal){atlas=false;journal=false;}else if(!menu)paused=!paused;
            }
            if(key.consumePress(KeyEvent.VK_N)&&(menu||paused)){menu=true;confirmNew=true;renderer.confirmNew=true;}
            if(key.consumePress(KeyEvent.VK_ENTER)){
                if(confirmNew){adventure.newCampaign();begin();}
                else if(adventure.dead){adventure.respawn();begin();}else begin();
            }
            if(!menu&&key.consumePress(KeyEvent.VK_H)){journal=!journal;atlas=false;}
            if(!menu&&key.consumePress(KeyEvent.VK_M)){atlas=!atlas;journal=false;}
            if(!menu&&key.consumePress(KeyEvent.VK_R)){adventure.respawn();paused=false;atlas=false;journal=false;}
            if(menu){adventure.tick++;}
            else if(focused&&!paused&&!atlas&&!journal){
                double mx=(key.right?1:0)-(key.left?1:0),my=(key.down?1:0)-(key.up?1:0);
                adventure.update(new Adventure.Input(mx,my,key.dash,key.isDown(KeyEvent.VK_J)||key.isDown(KeyEvent.VK_Z)||clickAttack,
                        key.consumePress(KeyEvent.VK_Q)||clickCrown,key.consumePress(KeyEvent.VK_SPACE),key.consumePress(KeyEvent.VK_X),
                        key.consumePress(KeyEvent.VK_E),key.consumePress(KeyEvent.VK_U),key.consumePress(KeyEvent.VK_I)));
            }else{key.consumePress(KeyEvent.VK_Q);key.consumePress(KeyEvent.VK_SPACE);key.consumePress(KeyEvent.VK_X);key.consumePress(KeyEvent.VK_E);key.consumePress(KeyEvent.VK_U);key.consumePress(KeyEvent.VK_I);}
            clickAttack=false;clickCrown=false;
        }
        audio.configure(muted,adventure.restoration(),adventure.world.area==Area.THRONE&&!adventure.progress.has("won"),adventure.progress.has("won"));
        String effect;while((effect=adventure.sounds.poll())!=null)audio.play(effect);
        if(playButton!=null&&playButtonShown!=menu){playButtonShown=menu;boolean visible=menu;
            SwingUtilities.invokeLater(()->{playButton.setVisible(visible);frame.pack();requestFocusInWindow();});}
        if(playButton!=null){String label=confirmNew?"Start a new kingdom (previous save will be backed up)":"Begin / continue adventure";
            if(!label.equals(playButtonLabel)){playButtonLabel=label;SwingUtilities.invokeLater(()->playButton.setText(label));}}

    }
    public BufferedImage renderFrame(){return renderer.render(adventure,menu,paused,atlas,journal,muted,focused);}
    public void render(){if(!isDisplayable())return;BufferStrategy buffers=getBufferStrategy();if(buffers==null)return;BufferedImage image=renderFrame();
        do{do{Graphics2D g=(Graphics2D)buffers.getDrawGraphics();try{g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);g.drawImage(image,0,0,getWidth(),getHeight(),null);}finally{g.dispose();}}while(buffers.contentsRestored()&&running);
            buffers.show();Toolkit.getDefaultToolkit().sync();}while(buffers.contentsLost()&&running);
    }
    private JMenuItem item(String text,Runnable action){JMenuItem item=new JMenuItem(text);item.addActionListener(e->{commands.add(action);requestFocusInWindow();});return item;}
    private void openWindow(){
        frame=new JFrame(TITLE);frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);frame.setResizable(false);frame.add(this,BorderLayout.CENTER);
        playButton=new JButton("Begin / continue adventure");playButton.setFont(new Font(Font.SANS_SERIF,Font.BOLD,14));
        playButton.setBackground(new Color(0x203931));playButton.setForeground(new Color(0xead7a6));playButton.setOpaque(true);playButton.setBorderPainted(false);
        playButton.getAccessibleContext().setAccessibleDescription("Start or continue KingdomKing. Enter is the keyboard shortcut.");
        playButton.addActionListener(e->commands.add(()->{if(confirmNew)adventure.newCampaign();begin();}));frame.add(playButton,BorderLayout.SOUTH);
        keyDispatcher=event->{
            if(KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow()!=frame||MenuSelectionManager.defaultManager().getSelectedPath().length>0)return false;
            if(event.getID()==KeyEvent.KEY_PRESSED)key.keyPressed(event);else if(event.getID()==KeyEvent.KEY_RELEASED)key.keyReleased(event);
            return false;
        };
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(keyDispatcher);
        JMenuBar bar=new JMenuBar();JMenu game=new JMenu("Game"),settings=new JMenu("Settings");
        game.add(item("Start / resume",this::begin));game.add(item("Pause",()->paused=!paused));
        game.add(item("Field journal",()->{menu=false;journal=!journal;atlas=false;}));game.add(item("Atlas",()->{menu=false;atlas=!atlas;journal=false;}));
        game.add(item("Return to fountain",()->{adventure.respawn();begin();}));
        game.add(item("New kingdom...",()->{menu=true;confirmNew=true;renderer.confirmNew=true;}));
        settings.add(item("Toggle sound",()->muted=!muted));settings.add(item("Toggle calm effects",()->renderer.reducedMotion=!renderer.reducedMotion));
        bar.add(game);bar.add(settings);frame.setJMenuBar(bar);
        frame.addWindowListener(new WindowAdapter(){@Override public void windowClosing(WindowEvent event){stop();frame.dispose();}});
        frame.addWindowFocusListener(new WindowAdapter(){
            @Override public void windowGainedFocus(WindowEvent event){requestFocusInWindow();}
            @Override public void windowLostFocus(WindowEvent event){focused=false;key.clear();}
        });
        frame.pack();frame.setLocationRelativeTo(null);frame.setVisible(true);createBufferStrategy(3);requestFocusInWindow();
        audio.start();start();
    }
    public static void main(String[] args)throws IOException{
        if(args.length>0&&args[0].equals("--classic")){ClassicGame.main(Arrays.copyOfRange(args,1,args.length));return;}
        if(args.length==1&&args[0].equals("--help")){
            System.out.println("KingdomKing — The Stolen Crown\nWASD/arrows move; Shift sprint; J/Z/left click slash; Q/right click crown; Space dodge; X slam; E interact.\nU/I jewel sockets; H journal; M atlas; F mute; F2 calm effects; Esc pause; R return to fountain.\nUsage: java -jar KingdomKing.jar [--classic | --screenshot file.png [--scene menu|castle|meadow|marsh|temple|keep|boss|restored|atlas|journal] | --sound-demo file.wav]\nCampaign saves in the platform application-data directory. Override with -Dkingdomking.save=/path/campaign.properties.");return;
        }
        if(args.length==2&&args[0].equals("--sound-demo")){Path path=Path.of(args[1]).toAbsolutePath();Files.createDirectories(path.getParent());AudioEngine.writeDemo(path);System.out.println("Wrote "+path);return;}
        if((args.length==2||args.length==4)&&args[0].equals("--screenshot")){
            String scene=args.length==4&&args[2].equals("--scene")?args[3]:"castle";
            Adventure world=new Adventure(null);AdventureRenderer renderer=new AdventureRenderer();
            switch(scene){
                case "castle","menu","atlas","journal" -> {}
                case "meadow" -> {world.enter(Area.MEADOW);world.x=340;world.y=395;}
                case "marsh" -> {world.enter(Area.MARSH);world.x=470;world.y=275;}
                case "temple" -> {world.enter(Area.TEMPLE);world.x=392;world.y=355;world.statues[0]=1;world.statues[1]=2;world.statues[2]=3;}
                case "keep" -> {world.enter(Area.KEEP);world.x=392;world.y=300;}
                case "boss" -> {world.enter(Area.THRONE);world.x=392;world.y=270;world.world.enemies.get(0).y=220;world.world.enemies.get(0).windup=25;world.world.enemies.get(0).aimY=1;}
                case "restored" -> {world.progress.flags.addAll(java.util.List.of("forge","garden","tower","won","smithRescued","cookRescued","mapRescued"));world.x=560;world.y=230;}
                default -> throw new IllegalArgumentException("Unknown screenshot scene: "+scene);
            }
            world.areaAge=200;world.toastTime=0;world.tick=150;
            Path output=Path.of(args[1]).toAbsolutePath();Files.createDirectories(output.getParent());
            ImageIO.write(renderer.render(world,scene.equals("menu"),false,scene.equals("atlas"),scene.equals("journal"),false,true),"png",output.toFile());System.out.println("Rendered "+scene+" to "+output);return;
        }
        if(args.length!=0)throw new IllegalArgumentException("Unknown arguments. Use --help.");
        if(GraphicsEnvironment.isHeadless()){System.err.println("A graphical desktop is required. Use --screenshot for a headless render.");System.exit(1);}
        SwingUtilities.invokeLater(()->{try{new Game().openWindow();}catch(RuntimeException|LinkageError e){e.printStackTrace();JOptionPane.showMessageDialog(null,e.toString(),TITLE,JOptionPane.ERROR_MESSAGE);}});
    }
}
