package com.shukun.birthdayreminder.update;

import android.app.job.JobParameters;
import android.app.job.JobService;

public final class UpdateCheckJobService extends JobService {
    @Override
    public boolean onStartJob(JobParameters parameters) {
        UpdateManager.checkAsync(this, (update, error) -> {
            if (error == null && update != null) {
                try {
                    if (UpdatePreferences.autoDownload(this)) {
                        UpdateManager.download(this, update);
                        UpdateNotificationHelper.showDownloading(this, update.version);
                    } else {
                        UpdateNotificationHelper.showAvailable(this, update);
                    }
                } catch (Exception downloadError) {
                    UpdateNotificationHelper.showAvailable(this, update);
                }
            }
            jobFinished(parameters, error != null);
        });
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters parameters) {
        return true;
    }
}
