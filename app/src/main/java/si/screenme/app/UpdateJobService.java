package si.screenme.app;

import android.app.job.JobParameters;
import android.app.job.JobService;

public class UpdateJobService extends JobService {
    @Override public boolean onStartJob(JobParameters params) {
        UpdateChecker.check(this, true, (info, error) -> jobFinished(params, false));
        return true;
    }

    @Override public boolean onStopJob(JobParameters params) {
        return true;
    }
}
