package si.screenme.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class UpdateReceiver extends BroadcastReceiver {
    static final String DEFAULT_URL = UpdateChecker.DEFAULT_URL;

    @Override public void onReceive(Context context, Intent intent) {
        PendingResult pending = goAsync();
        UpdateChecker.check(context, true, (info, error) -> pending.finish());
    }
}
