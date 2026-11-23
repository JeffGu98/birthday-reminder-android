package com.shukun.birthdayreminder.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.shukun.birthdayreminder.util.ReminderTime;

public final class ReminderPreferences {
    private static final String PREFS = "reminder_settings";
    private static final String KEY_HOUR = "reminder_hour";
    private static final String KEY_MINUTE = "reminder_minute";

    private ReminderPreferences() {}

    public static ReminderTime get(Context context) {
        SharedPreferences prefs = prefs(context);
        return ReminderTime.fromStored(
                prefs.getInt(KEY_HOUR, ReminderTime.MIDNIGHT.hour),
                prefs.getInt(KEY_MINUTE, ReminderTime.MIDNIGHT.minute));
    }

    public static void set(Context context, ReminderTime time) {
        prefs(context).edit()
                .putInt(KEY_HOUR, time.hour)
                .putInt(KEY_MINUTE, time.minute)
                .apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
