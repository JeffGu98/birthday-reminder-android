package com.shukun.birthdayreminder.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.shukun.birthdayreminder.data.BirthdayRepository;
import com.shukun.birthdayreminder.model.BirthdayPerson;
import com.shukun.birthdayreminder.notify.NotificationHelper;

public final class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String personId = intent.getStringExtra(ReminderScheduler.EXTRA_PERSON_ID);
        String kind = intent.getStringExtra(ReminderScheduler.EXTRA_KIND);
        if (personId == null || kind == null) return;

        BirthdayPerson person = new BirthdayRepository(context).findById(personId);
        if (person == null || !person.enabled) return;

        NotificationHelper.show(context, person, kind);
        // The fired one-shot alarm is replaced with next year's occurrence.
        PendingResult pendingResult = goAsync();
        new Thread(() -> {
            try {
                new ReminderScheduler(context).schedulePerson(person);
            } finally {
                pendingResult.finish();
            }
        }, "birthday-reminder-reschedule").start();
    }
}
