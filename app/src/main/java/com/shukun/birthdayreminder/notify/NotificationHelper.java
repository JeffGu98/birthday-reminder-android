package com.shukun.birthdayreminder.notify;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;

import com.shukun.birthdayreminder.MainActivity;
import com.shukun.birthdayreminder.R;
import com.shukun.birthdayreminder.alarm.ReminderScheduler;
import com.shukun.birthdayreminder.lunar.LunarCalendarService;
import com.shukun.birthdayreminder.model.BirthdayPerson;

public final class NotificationHelper {
    public static final String CHANNEL_ID = "birthday_reminders_v1";

    private NotificationHelper() {}

    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription(context.getString(R.string.notification_channel_description));
        channel.enableVibration(true);
        channel.enableLights(true);
        channel.setLightColor(Color.rgb(166, 61, 91));
        manager.createNotificationChannel(channel);
    }

    public static void show(Context context, BirthdayPerson person, String kind) {
        createChannel(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        String calendarLabel;
        if (ReminderScheduler.KIND_BOTH.equals(kind)) {
            calendarLabel = "公历和农历生日恰逢同一天";
        } else if (ReminderScheduler.KIND_LUNAR.equals(kind)) {
            calendarLabel = "今天是农历生日（"
                    + new LunarCalendarService().formatBirthday(
                    person.lunarMonth, person.lunarDay, person.lunarLeapMonth) + "）";
        } else {
            calendarLabel = String.format(java.util.Locale.CHINA,
                    "今天是公历生日（%02d月%02d日）", person.birthMonth, person.birthDay);
        }

        Intent launchIntent = new Intent(context, MainActivity.class);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                person.id.hashCode() & 0x7fffffff,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(context, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(context)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setDefaults(Notification.DEFAULT_ALL);
        }

        Notification notification = builder
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(context.getColor(R.color.primary))
                .setContentTitle("今天是" + person.name + "的生日 🎂")
                .setContentText(calendarLabel)
                .setStyle(new Notification.BigTextStyle().bigText(calendarLabel + "，记得送上祝福。"))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_REMINDER)
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .build();

        context.getSystemService(NotificationManager.class)
                .notify((person.id + kind).hashCode() & 0x7fffffff, notification);
    }
}
