package com.shukun.birthdayreminder.update;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;

import com.shukun.birthdayreminder.R;

public final class UpdateNotificationHelper {
    private static final String CHANNEL_ID = "app_updates_v1";
    private static final int NOTIFICATION_ID = 0x55504454;

    private UpdateNotificationHelper() {}

    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "软件更新", NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("新版本下载和安装提醒");
        context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    public static void showAvailable(Context context, UpdateInfo update) {
        Intent browser = new Intent(Intent.ACTION_VIEW, Uri.parse(update.releaseUrl));
        PendingIntent action = PendingIntent.getActivity(context, NOTIFICATION_ID, browser,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        notify(context, "发现新版本 " + update.version,
                "打开 GitHub 查看并下载新版", action, false);
    }

    public static void showDownloading(Context context, String version) {
        notify(context, "正在下载新版本 " + version,
                "下载完成并校验后会提醒你安装", null, true);
    }

    public static void showReadyToInstall(Context context, long downloadId, String version) {
        Intent install = new Intent(context, UpdateInstallerActivity.class)
                .putExtra(UpdateInstallerActivity.EXTRA_DOWNLOAD_ID, downloadId);
        PendingIntent action = PendingIntent.getActivity(context, NOTIFICATION_ID, install,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        notify(context, "新版本 " + version + " 已下载", "点击完成安装", action, false);
    }

    public static void showVerificationFailed(Context context) {
        notify(context, "新版安装包校验失败", "文件已拒绝安装，请稍后重新检查更新", null, false);
    }

    private static void notify(Context context, String title, String text,
                               PendingIntent action, boolean ongoing) {
        createChannel(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) return;

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(context, CHANNEL_ID)
                : new Notification.Builder(context).setPriority(Notification.PRIORITY_DEFAULT);
        builder.setSmallIcon(R.drawable.ic_notification)
                .setColor(context.getColor(R.color.primary))
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setAutoCancel(!ongoing)
                .setOngoing(ongoing)
                .setCategory(Notification.CATEGORY_STATUS);
        if (action != null) builder.setContentIntent(action);
        context.getSystemService(NotificationManager.class).notify(NOTIFICATION_ID, builder.build());
    }
}
