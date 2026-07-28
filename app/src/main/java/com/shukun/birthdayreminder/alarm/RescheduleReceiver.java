package com.shukun.birthdayreminder.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class RescheduleReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        PendingResult pendingResult = goAsync();
        new Thread(() -> {
            try {
                new ReminderScheduler(context).rescheduleAll();
            } finally {
                pendingResult.finish();
            }
        }, "birthday-reschedule-all").start();
    }
}
