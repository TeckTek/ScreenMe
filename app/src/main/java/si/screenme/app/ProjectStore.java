package si.screenme.app;

import android.content.*;import org.json.*;import java.io.*;import java.util.*;

final class ProjectStore {
    static final String PREF="screenme";
    static ArrayList<String> all(Context c){Set<String>s=new HashSet<>(c.getSharedPreferences(PREF,0).getStringSet("projects",Collections.emptySet()));String current=current(c);if(!current.isEmpty())s.add(current);ArrayList<String>a=new ArrayList<>(s);Collections.sort(a,String.CASE_INSENSITIVE_ORDER);return a;}
    static String current(Context c){return c.getSharedPreferences(PREF,0).getString("profile","");}
    static void select(Context c,String p){p=p.trim();if(p.isEmpty())return;ArrayList<String>a=all(c);a.add(p);c.getSharedPreferences(PREF,0).edit().putString("profile",p).putStringSet("projects",new HashSet<>(a)).apply();}
    static void selectForPackage(Context c,String packageName,String suggestedName){String key="autoProject:"+packageName;String mapped=c.getSharedPreferences(PREF,0).getString(key,"");String project=mapped.isEmpty()?suggestedName:mapped;select(c,project);if(mapped.isEmpty())c.getSharedPreferences(PREF,0).edit().putString(key,project).apply();}
    static void remove(Context c,String p){ArrayList<String>a=all(c);a.remove(p);String next=a.isEmpty()?"":a.get(0);c.getSharedPreferences(PREF,0).edit().putStringSet("projects",new HashSet<>(a)).putString("profile",next).apply();}
    static boolean contains(Context c,String p){for(String existing:all(c))if(existing.equalsIgnoreCase(p.trim()))return true;return false;}
    static boolean rename(Context c,String oldName,String newName){newName=newName.trim();if(newName.isEmpty()||(!oldName.equalsIgnoreCase(newName)&&contains(c,newName)))return false;File oldDir=folder(c,oldName),newDir=folder(c,newName);if(!oldDir.equals(newDir)&&newDir.exists())return false;if(oldDir.exists()&&!oldDir.equals(newDir)&&!oldDir.renameTo(newDir))return false;File recordsDir=newDir.exists()?newDir:oldDir;updateProfiles(recordsDir,newName);ArrayList<String>a=all(c);a.remove(oldName);a.add(newName);String current=current(c);android.content.SharedPreferences prefs=c.getSharedPreferences(PREF,0);android.content.SharedPreferences.Editor edit=prefs.edit().putStringSet("projects",new HashSet<>(a)).putString("profile",current.equals(oldName)?newName:current);for(Map.Entry<String,?>entry:prefs.getAll().entrySet())if(entry.getKey().startsWith("autoProject:")&&oldName.equals(entry.getValue()))edit.putString(entry.getKey(),newName);edit.apply();return true;}
    static boolean deleteWithRecords(Context c,String p){File dir=folder(c,p);if(dir.exists()&&!deleteTree(dir))return false;clearPackageMappings(c,p);remove(c,p);return true;}
    static File folder(Context c,String p){return new File(c.getExternalFilesDir(null),"ScreenMe/projekti/"+safe(p));}
    static boolean deleteTree(File f){File[]children=f.listFiles();if(children!=null)for(File child:children)if(!deleteTree(child))return false;return f.delete();}
    static void updateProfiles(File projectDir,String name){File[]records=projectDir.listFiles(File::isDirectory);if(records==null)return;for(File record:records){File meta=new File(record,"metadata.json");if(!meta.isFile())continue;try{JSONObject json=new JSONObject(RecordItem.read(meta));json.put("profile",name);Storage.text(meta,json.toString(2));}catch(Exception ignored){}}}
    static void clearPackageMappings(Context c,String project){android.content.SharedPreferences prefs=c.getSharedPreferences(PREF,0);android.content.SharedPreferences.Editor edit=prefs.edit();for(Map.Entry<String,?>entry:prefs.getAll().entrySet())if(entry.getKey().startsWith("autoProject:")&&project.equals(entry.getValue()))edit.remove(entry.getKey());edit.apply();}
    static String safe(String s){return s.replaceAll("[^a-zA-Z0-9._-]","_");}
}
