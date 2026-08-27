package si.screenme.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

final class UpdateScheduler {
    private static final int PERIODIC_JOB = 9101;
    private static final int IMMEDIATE_JOB = 9102;
    private static final long INTERVAL = 3L * 60L * 60L * 1000L;

    private UpdateScheduler() {}

    static void schedule(Context context) {
        cancelLegacyAlarm(context);
        JobScheduler scheduler =
                (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        ComponentName service = new ComponentName(context, UpdateJobService.class);
        JobInfo existing = scheduler.getPendingJob(PERIODIC_JOB);
        if (existing == null) {
            scheduler.schedule(new JobInfo.Builder(PERIODIC_JOB, service)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPeriodic(INTERVAL)
                    .setPersisted(true)
                    .build());
        }
    }

    private static void cancelLegacyAlarm(Context context) {
        Intent intent = new Intent(context, UpdateReceiver.class);
        PendingIntent legacy = PendingIntent.getBroadcast(context, 91, intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (legacy == null) return;
        AlarmManager alarms =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarms.cancel(legacy);
        legacy.cancel();
    }

    static void scheduleImmediate(Context context) {
        JobScheduler scheduler =
                (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        ComponentName service = new ComponentName(context, UpdateJobService.class);
        scheduler.schedule(new JobInfo.Builder(IMMEDIATE_JOB, service)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setMinimumLatency(1_000)
                .setOverrideDeadline(30_000)
                .build());
    }
}
