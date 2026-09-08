package si.screenme.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class SyncForegroundService extends Service {
    private static final String CHANNEL = "screenme_sync";
    private static final int NOTIFICATION = 1204;
    private boolean working;
    private boolean rerun;
    private volatile boolean stopped;
    private int latestStartId;

    @Override public void onCreate() {
        super.onCreate();
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.createNotificationChannel(new NotificationChannel(CHANNEL,
                "ScreenMe prenosi", NotificationManager.IMPORTANCE_LOW));
        startForeground(NOTIFICATION, notification());
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        synchronized (this) {
            stopped = false;
            rerun = true;
            latestStartId = startId;
            if (working) return START_NOT_STICKY;
            working = true;
        }
        new Thread(this::upload, "ScreenMe immediate Drive sync").start();
        return START_NOT_STICKY;
    }

    private void upload() {
        while (!stopped && SyncScheduler.isRunning(this)) {
            synchronized (this) { rerun = false; }
            Storage.SyncResult result = Storage.syncPending(this,
                    () -> stopped || !SyncScheduler.isRunning(this));
            synchronized (this) {
                if (stopped || result.failed > 0 || !rerun) {
                    working = false;
                    break;
                }
            }
        }
        int id;
        synchronized (this) { id = latestStartId; }
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelfResult(id);
    }

    private Notification notification() {
        PendingIntent open = PendingIntent.getActivity(this, 1204,
                new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_screenme_mono)
                .setContentTitle("ScreenMe pošilja v Drive")
                .setContentText("Posnetki in opombe se nalagajo samodejno")
                .setContentIntent(open)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build();
    }

    @Override public void onDestroy() {
        stopped = true;
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
