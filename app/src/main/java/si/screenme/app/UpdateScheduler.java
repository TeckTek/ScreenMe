package si.screenme.app;
import android.app.*;import android.content.*;import android.os.*;
final class UpdateScheduler{
    static void schedule(Context c){AlarmManager a=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);Intent i=new Intent(c,UpdateReceiver.class);PendingIntent p=PendingIntent.getBroadcast(c,91,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);a.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,SystemClock.elapsedRealtime()+60_000,6*60*60*1000L,p);}
}
