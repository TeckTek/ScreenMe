package si.screenme.app;
import android.content.*;
public class BootReceiver extends BroadcastReceiver{
    @Override public void onReceive(Context c,Intent i){
        String action=i.getAction();
        if(Intent.ACTION_BOOT_COMPLETED.equals(action)||Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)){
            UpdateScheduler.schedule(c);
            UpdateScheduler.scheduleImmediate(c);
        }
    }
}
