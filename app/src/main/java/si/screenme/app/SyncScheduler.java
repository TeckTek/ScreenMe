package si.screenme.app;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;

final class SyncScheduler {
    private static final int NOW = 9201;
    private static final int PERIODIC = 9202;
    private static final long INTERVAL = 6L * 60L * 60L * 1000L;

    private SyncScheduler() {}

    static void scheduleNow(Context context) {
        if (context.getSharedPreferences("screenme", 0).getString("syncTree", "").isEmpty()) return;
        schedulePeriodic(context);
        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        scheduler.schedule(new JobInfo.Builder(NOW, new ComponentName(context, SyncJobService.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setMinimumLatency(500)
                .setOverrideDeadline(15_000)
                .setBackoffCriteria(30_000, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
                .setPersisted(true)
                .build());
    }

    static void schedulePeriodic(Context context) {
        if (context.getSharedPreferences("screenme", 0).getString("syncTree", "").isEmpty()) return;
        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (scheduler.getPendingJob(PERIODIC) != null) return;
        scheduler.schedule(new JobInfo.Builder(PERIODIC, new ComponentName(context, SyncJobService.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(INTERVAL)
                .setPersisted(true)
                .build());
    }

    static void cancel(Context context) {
        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        scheduler.cancel(NOW);
        scheduler.cancel(PERIODIC);
    }
}
