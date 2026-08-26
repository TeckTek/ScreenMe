package si.screenme.app;

import android.content.*;import java.util.*;

final class ProjectStore {
    static final String PREF="screenme";
    static ArrayList<String> all(Context c){Set<String>s=new HashSet<>(c.getSharedPreferences(PREF,0).getStringSet("projects",Collections.emptySet()));String current=current(c);if(!current.isEmpty())s.add(current);ArrayList<String>a=new ArrayList<>(s);Collections.sort(a,String.CASE_INSENSITIVE_ORDER);return a;}
    static String current(Context c){return c.getSharedPreferences(PREF,0).getString("profile","");}
    static void select(Context c,String p){p=p.trim();if(p.isEmpty())return;ArrayList<String>a=all(c);a.add(p);c.getSharedPreferences(PREF,0).edit().putString("profile",p).putStringSet("projects",new HashSet<>(a)).apply();}
    static void remove(Context c,String p){ArrayList<String>a=all(c);a.remove(p);String next=a.isEmpty()?"":a.get(0);c.getSharedPreferences(PREF,0).edit().putStringSet("projects",new HashSet<>(a)).putString("profile",next).apply();}
    static String safe(String s){return s.replaceAll("[^a-zA-Z0-9._-]","_");}
}
