package com.shukun.birthdayreminder.alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import com.shukun.birthdayreminder.data.BirthdayRepository;
import com.shukun.birthdayreminder.lunar.LunarCalendarService;
import com.shukun.birthdayreminder.lunar.LunarDate;
import com.shukun.birthdayreminder.model.BirthdayPerson;
import com.shukun.birthdayreminder.util.SolarDate;
import com.shukun.birthdayreminder.util.SolarDateRules;

import java.util.List;

public final class ReminderScheduler {
    public static final String EXTRA_PERSON_ID = "person_id";
    public static final String EXTRA_KIND = "kind";
    public static final String KIND_SOLAR = "solar";
    public static final String KIND_LUNAR = "lunar";
    public static final String KIND_BOTH = "both";

    private final Context context;
    private final AlarmManager alarmManager;
    private final LunarCalendarService lunarService = new LunarCalendarService();

    public ReminderScheduler(Context context) {
        this.context = context.getApplicationContext();
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    public boolean canScheduleExact() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms();
    }

    public void rescheduleAll() {
        List<BirthdayPerson> people = new BirthdayRepository(context).getAll();
        for (BirthdayPerson person : people) {
            try {
                schedulePerson(person);
            } catch (RuntimeException error) {
                // One broken entry must not stop the remaining reminders from being scheduled.
            }
        }
    }

    public void schedulePerson(BirthdayPerson person) {
        cancelPerson(person.id);
        if (!person.enabled) return;

        long now = System.currentTimeMillis();
        SolarDate solarDate = SolarDateRules.nextBirthday(person.birthMonth, person.birthDay, now);
        SolarDate lunarDate = null;
        try {
            LunarDate lunarBirthday = new LunarDate(
                    person.lunarMonth, person.lunarDay, person.lunarLeapMonth, 1);
            lunarDate = lunarService.nextLunarBirthday(lunarBirthday, now);
        } catch (IllegalStateException error) {
            // Unresolvable lunar data falls back to a solar-only reminder.
        }

        if (lunarDate == null) {
            schedule(person.id, KIND_SOLAR, solarDate.atLocalMidnightMillis());
        } else if (solarDate.equals(lunarDate)) {
            schedule(person.id, KIND_BOTH, solarDate.atLocalMidnightMillis());
        } else {
            schedule(person.id, KIND_SOLAR, solarDate.atLocalMidnightMillis());
            schedule(person.id, KIND_LUNAR, lunarDate.atLocalMidnightMillis());
        }
    }

    public void cancelPerson(String personId) {
        cancel(personId, KIND_SOLAR);
        cancel(personId, KIND_LUNAR);
        cancel(personId, KIND_BOTH);
    }

    private void schedule(String personId, String kind, long triggerAtMillis) {
        PendingIntent pendingIntent = pendingIntent(personId, kind, PendingIntent.FLAG_UPDATE_CURRENT);
        if (canScheduleExact()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        } else {
            // Keep a best-effort reminder until the user enables exact alarm access.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        }
    }

    private void cancel(String personId, String kind) {
        PendingIntent pendingIntent = pendingIntent(personId, kind, PendingIntent.FLAG_NO_CREATE);
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    private PendingIntent pendingIntent(String personId, String kind, int lookupFlag) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction("com.shukun.birthdayreminder.REMIND");
        intent.setData(Uri.parse("birthday://reminder/" + personId + "/" + kind));
        intent.putExtra(EXTRA_PERSON_ID, personId);
        intent.putExtra(EXTRA_KIND, kind);
        int flags = lookupFlag | PendingIntent.FLAG_IMMUTABLE;
        int requestCode = (personId + ':' + kind).hashCode() & 0x7fffffff;
        return PendingIntent.getBroadcast(context, requestCode, intent, flags);
    }
}
