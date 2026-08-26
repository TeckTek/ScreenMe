package si.screenme.app;

import android.content.Context;import android.net.Uri;import android.provider.DocumentsContract;
import android.graphics.Bitmap;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

final class Storage {
    static File newRecord(Context c) {
        String profile=c.getSharedPreferences("screenme",0).getString("profile","Default").replaceAll("[^a-zA-Z0-9._-]","_");
        File root=new File(c.getExternalFilesDir(null),"ScreenMe/projekti/"+profile);
        File dir=new File(root,new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS",Locale.ROOT).format(new Date())); dir.mkdirs(); return dir;
    }
    static boolean bitmap(Bitmap b,File f){try(FileOutputStream o=new FileOutputStream(f)){return b.compress(Bitmap.CompressFormat.PNG,100,o);}catch(Exception e){return false;}}
    static void text(File f,String s){try(FileWriter w=new FileWriter(f)){w.write(s);}catch(Exception ignored){}}
    static void sync(Context c,File record){String raw=c.getSharedPreferences("screenme",0).getString("syncTree","");if(raw.isEmpty())return;new Thread(()->{try{Uri tree=Uri.parse(raw);Uri root=DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));Uri project=dir(c,root,"ScreenMe");project=dir(c,project,record.getParentFile().getName());Uri target=dir(c,project,record.getName());File[] files=record.listFiles();if(files!=null)for(File f:files)copy(c,target,f);}catch(Exception ignored){}}).start();}
    static Uri dir(Context c,Uri parent,String name)throws Exception{try(android.database.Cursor q=c.getContentResolver().query(DocumentsContract.buildChildDocumentsUriUsingTree(parent,DocumentsContract.getDocumentId(parent)),new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE},null,null,null)){while(q!=null&&q.moveToNext())if(name.equals(q.getString(1))&&DocumentsContract.Document.MIME_TYPE_DIR.equals(q.getString(2)))return DocumentsContract.buildDocumentUriUsingTree(parent,q.getString(0));}return DocumentsContract.createDocument(c.getContentResolver(),parent,DocumentsContract.Document.MIME_TYPE_DIR,name);}
    static void copy(Context c,Uri parent,File f)throws Exception{String mime=f.getName().endsWith(".png")?"image/png":f.getName().endsWith(".json")?"application/json":"text/markdown";Uri out=DocumentsContract.createDocument(c.getContentResolver(),parent,mime,f.getName());if(out==null)return;try(InputStream in=new FileInputStream(f);OutputStream o=c.getContentResolver().openOutputStream(out)){byte[] b=new byte[8192];int n;while((n=in.read(b))>0)o.write(b,0,n);}}
    static String json(String profile,String note,boolean edited){
        return "{\n  \"profile\": \""+esc(profile)+"\",\n  \"note\": \""+esc(note)+"\",\n  \"edited\": "+edited+",\n  \"createdAt\": \""+new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX",Locale.ROOT).format(new Date())+"\"\n}";
    }
    static String esc(String s){return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n");}
}
