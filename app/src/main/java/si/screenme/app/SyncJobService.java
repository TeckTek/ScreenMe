package si.screenme.app;

import android.app.job.JobParameters;
import android.app.job.JobService;

public class SyncJobService extends JobService {
    private volatile boolean stopped;

    @Override public boolean onStartJob(JobParameters params) {
        if (!SyncScheduler.isRunning(this)) return false;
        stopped = false;
        new Thread(() -> {
            Storage.SyncResult result = Storage.syncPending(this, () -> stopped);
            if (!stopped) jobFinished(params, result.failed > 0);
        }, "ScreenMe Drive sync").start();
        return true;
    }

    @Override public boolean onStopJob(JobParameters params) {
        stopped = true;
        return SyncScheduler.isRunning(this);
    }
}
