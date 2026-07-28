package com.shukun.birthdayreminder.update;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class DownloadCompleteReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) return;
        long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L);
        if (id != UpdatePreferences.downloadId(context)) return;

        PendingResult pendingResult = goAsync();
        new Thread(() -> {
            try {
                if (UpdateManager.verifyDownloadedApk(context, id)) {
                    UpdateNotificationHelper.showReadyToInstall(
                            context, id, UpdatePreferences.downloadVersion(context));
                } else {
                    UpdateNotificationHelper.showVerificationFailed(context);
                }
            } finally {
                pendingResult.finish();
            }
        }, "apk-sha256-check").start();
    }
}
