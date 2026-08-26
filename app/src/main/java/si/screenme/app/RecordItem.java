package si.screenme.app;
import java.io.*;import java.text.*;import java.util.*;

final class RecordItem {
    final File dir,image;final String project,note,title,severity;final long time;
    RecordItem(File d,String p){dir=d;File meta=new File(d,"metadata.json");project=json(meta,"profile",p);File edited=new File(d,"annotated.png");image=edited.exists()?edited:new File(d,"screenshot.png");note=read(new File(d,"note.md"));title=json(meta,"title",first(note,"Brez naslova"));severity=json(meta,"severity","Običajna");time=Math.max(d.lastModified(),Math.max(image.lastModified(),meta.lastModified()));}
    static ArrayList<RecordItem> list(File root,String filter){ArrayList<RecordItem>out=new ArrayList<>();if(root==null)return out;File projects=new File(root,"ScreenMe/projekti");File[] ps=projects.listFiles(File::isDirectory);if(ps!=null)for(File p:ps){if(filter!=null&&!filter.isEmpty()&&!p.getName().equals(ProjectStore.safe(filter)))continue;File[] rs=p.listFiles(File::isDirectory);if(rs!=null)for(File r:rs)if(new File(r,"screenshot.png").exists())out.add(new RecordItem(r,p.getName()));}out.sort((a,b)->Long.compare(b.time,a.time));return out;}
    static String read(File f){try{byte[]b=new byte[(int)f.length()];try(FileInputStream i=new FileInputStream(f)){i.read(b);}return new String(b,java.nio.charset.StandardCharsets.UTF_8);}catch(Exception e){return "";}}
    static String first(String s,String fallback){for(String x:s.split("\\R")){x=x.replaceFirst("^#+\\s*","").trim();if(!x.isEmpty())return x;}return fallback;}
    static String json(File f,String key,String fallback){String s=read(f);java.util.regex.Matcher m=java.util.regex.Pattern.compile("\\\""+key+"\\\"\\s*:\\s*\\\"([^\\\"]*)").matcher(s);return m.find()?m.group(1):fallback;}
    String date(){return new SimpleDateFormat("dd. MMM yyyy · HH:mm",new Locale("sl","SI")).format(new Date(time));}
}
