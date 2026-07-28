package com.shukun.birthdayreminder.update;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;

public final class UpdateScheduler {
    private static final int JOB_ID = 0x42524459;
    private static final long DAY_MS = 24L * 60L * 60L * 1000L;
    private static final long FLEX_MS = 6L * 60L * 60L * 1000L;

    private UpdateScheduler() {}

    public static void schedule(Context context) {
        JobInfo job = new JobInfo.Builder(
                JOB_ID, new ComponentName(context, UpdateCheckJobService.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(DAY_MS, FLEX_MS)
                .setPersisted(true)
                .build();
        context.getSystemService(JobScheduler.class).schedule(job);
    }
}
