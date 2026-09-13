package game.rain.adventure;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/** Small, versioned, atomic campaign saves. Tests use an isolated temporary folder. */
public final class SaveStore {
    private final Path file;
    public SaveStore(Path file) { this.file=file; }
    public static Path defaultPath() {
        String override=System.getProperty("kingdomking.save");
        if(override!=null)return Path.of(override);
        String os=System.getProperty("os.name");
        if(os.startsWith("Mac"))return Path.of(System.getProperty("user.home"),"Library","Application Support","KingdomKing","campaign.properties");
        String appdata=System.getenv("APPDATA");
        if(os.startsWith("Windows")&&appdata!=null)return Path.of(appdata,"KingdomKing","campaign.properties");
        String xdg=System.getenv("XDG_DATA_HOME");
        return (xdg==null?Path.of(System.getProperty("user.home"),".local","share"):Path.of(xdg)).resolve("KingdomKing/campaign.properties");
    }
    public Progress load() throws IOException {
        Progress result=new Progress();
        if(!Files.exists(file))return result;
        Properties data=new Properties();
        try(Reader input=Files.newBufferedReader(file)){data.load(input);}
        catch(IllegalArgumentException e){throw new IOException("Invalid save file",e);}
        if(!"1".equals(data.getProperty("version")))throw new IOException("Unsupported save version");
        result.coins=number(data,"coins",0,99999); result.weapon=number(data,"weapon",0,3);
        result.hearts=number(data,"hearts",0,5); result.jewels=number(data,"jewels",0,14);
        result.decree=number(data,"decree",0,2); result.deaths=number(data,"deaths",0,99999);
        result.slotA=number(data,"slotA",0,3); result.slotB=number(data,"slotB",0,3);
        result.bestCombo=number(data,"bestCombo",0,99999);
        for(String flag:data.getProperty("flags","").split(","))if(flag.matches("[A-Za-z0-9_-]{1,48}"))result.flags.add(flag);
        if((result.jewels&(1<<result.slotA))==0)result.slotA=0;
        if(result.slotA==result.slotB||(result.jewels&(1<<result.slotB))==0)result.slotB=0;
        return result;
    }
    private static int number(Properties data,String key,int min,int max) throws IOException {
        try{return Math.max(min,Math.min(max,Integer.parseInt(data.getProperty(key,"0"))));}
        catch(NumberFormatException e){throw new IOException("Invalid saved "+key,e);}
    }
    public void save(Progress p) throws IOException {
        Path parent=file.toAbsolutePath().getParent(); Files.createDirectories(parent);
        Properties data=new Properties(); data.setProperty("version","1");
        data.setProperty("coins",""+p.coins); data.setProperty("weapon",""+p.weapon); data.setProperty("hearts",""+p.hearts);
        data.setProperty("jewels",""+p.jewels); data.setProperty("decree",""+p.decree); data.setProperty("deaths",""+p.deaths);
        data.setProperty("slotA",""+p.slotA);data.setProperty("slotB",""+p.slotB);data.setProperty("bestCombo",""+p.bestCombo);
        data.setProperty("flags",String.join(",",p.flags.stream().sorted().toList()));
        Path temp=Files.createTempFile(parent,"campaign-",".tmp");
        try {
            try(Writer out=Files.newBufferedWriter(temp)){data.store(out,"KingdomKing campaign v1");}
            try{Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException e){Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);}
        }finally{Files.deleteIfExists(temp);}
    }
    public void backup() throws IOException {
        if(Files.exists(file))Files.copy(file,file.resolveSibling("campaign.previous.properties"),StandardCopyOption.REPLACE_EXISTING);
    }
}
