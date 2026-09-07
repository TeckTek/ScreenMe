package si.screenme.app;

import android.app.job.JobParameters;
import android.app.job.JobService;

public class SyncJobService extends JobService {
    private volatile boolean stopped;

    @Override public boolean onStartJob(JobParameters params) {
        stopped = false;
        new Thread(() -> {
            Storage.SyncResult result = Storage.syncPending(this, () -> stopped);
            jobFinished(params, result.failed > 0 && !stopped);
        }, "ScreenMe Drive sync").start();
        return true;
    }

    @Override public boolean onStopJob(JobParameters params) {
        stopped = true;
        return true;
    }
}
