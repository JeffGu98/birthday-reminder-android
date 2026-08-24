package com.shukun.birthdayreminder.ui;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.shukun.birthdayreminder.BuildConfig;
import com.shukun.birthdayreminder.R;
import com.shukun.birthdayreminder.update.UpdateInstallerActivity;
import com.shukun.birthdayreminder.update.UpdateManager;
import com.shukun.birthdayreminder.update.UpdatePreferences;

public final class HomeCards {
    public interface Listener {
        boolean hasNotificationPermission();
        boolean canScheduleExact();
        void requestNotificationPermission();
        void openExactAlarmSettings();
        void chooseBackupDestination();
        void chooseBackupFile();
        void checkForUpdates(Button button);
        void renderHome();
    }

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private HomeCards() {}

    public static View permissionCard(Context context, Listener listener) {
        boolean notificationReady = listener.hasNotificationPermission();
        boolean exactReady = listener.canScheduleExact();
        boolean ready = notificationReady && exactReady;

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(Views.dp(context, 16), Views.dp(context, 14), Views.dp(context, 16), Views.dp(context, 14));
        card.setBackgroundResource(ready ? R.drawable.bg_permission_ok : R.drawable.bg_permission_warning);

        String status = ready ? "提醒已就绪" : "需要完成提醒授权";
        TextView heading = Views.text(context, (ready ? "✓  " : "!  ") + status, 15,
                context.getColor(ready ? R.color.success : R.color.warning), Typeface.BOLD);
        card.addView(heading);

        String detail;
        if (ready) {
            detail = "通知和精确闹钟权限正常，将按 00:00 安排提醒。";
        } else if (!notificationReady && !exactReady) {
            detail = "还需要通知权限和“闹钟和提醒”权限。";
        } else if (!notificationReady) {
            detail = "还需要通知权限，否则提醒无法显示。";
        } else {
            detail = "还需要“闹钟和提醒”权限，才能尽量准时在 00:00 弹出。";
        }
        TextView body = Views.text(context, detail, 13, context.getColor(R.color.text_secondary), Typeface.NORMAL);
        body.setLineSpacing(0, 1.15f);
        card.addView(body, Views.marginParams(context, 0, 5, 0, ready ? 0 : 8));

        if (!notificationReady) {
            Button button = Views.compactButton(context, "允许通知");
            button.setOnClickListener(view -> listener.requestNotificationPermission());
            card.addView(button, Views.wrapParams(Gravity.START));
        }
        if (!exactReady) {
            Button button = Views.compactButton(context, "允许精确提醒");
            button.setOnClickListener(view -> listener.openExactAlarmSettings());
            card.addView(button, Views.wrapParams(Gravity.START));
        }
        return card;
    }

    public static View updateCard(Context context, Listener listener) {
        Context appContext = context.getApplicationContext();
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(Views.dp(context, 16), Views.dp(context, 14), Views.dp(context, 16), Views.dp(context, 14));
        card.setBackgroundResource(R.drawable.bg_card);

        card.addView(Views.text(context, "软件更新", 15, context.getColor(R.color.text_primary), Typeface.BOLD));

        long downloadId = UpdatePreferences.downloadId(appContext);
        TextView detail = Views.text(context,
                "当前版本 " + BuildConfig.VERSION_NAME + "，每天自动检查 GitHub Releases。",
                13, context.getColor(R.color.text_secondary), Typeface.NORMAL);
        card.addView(detail, Views.marginParams(context, 0, 5, 0, 6));

        Switch autoDownload = new Switch(context);
        autoDownload.setText("发现新版后自动下载");
        autoDownload.setTextColor(context.getColor(R.color.text_primary));
        autoDownload.setChecked(UpdatePreferences.autoDownload(appContext));
        autoDownload.setOnCheckedChangeListener((button, checked) -> {
            UpdatePreferences.setAutoDownload(appContext, checked);
            listener.renderHome();
        });
        card.addView(autoDownload);

        Switch wifiOnly = new Switch(context);
        wifiOnly.setText("仅在 Wi-Fi 下自动下载");
        wifiOnly.setTextColor(context.getColor(R.color.text_primary));
        wifiOnly.setChecked(UpdatePreferences.wifiOnly(appContext));
        wifiOnly.setEnabled(UpdatePreferences.autoDownload(appContext));
        wifiOnly.setOnCheckedChangeListener((button, checked) ->
                UpdatePreferences.setWifiOnly(appContext, checked));
        card.addView(wifiOnly);

        Button action = Views.compactButton(context, "立即检查更新");
        action.setOnClickListener(view -> listener.checkForUpdates(action));
        card.addView(action, Views.wrapParams(Gravity.START));

        refreshUpdateStatus(appContext, detail, action, downloadId);
        return card;
    }

    public static View backupCard(Context context, Listener listener) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(Views.dp(context, 16), Views.dp(context, 14), Views.dp(context, 16), Views.dp(context, 14));
        card.setBackgroundResource(R.drawable.bg_card);

        card.addView(Views.text(context, "本地备份", 15, context.getColor(R.color.text_primary), Typeface.BOLD));
        TextView detail = Views.text(context,
                "生日默认保存在本机应用内。卸载前导出 JSON 备份；重装或换手机后可从该文件恢复。",
                13, context.getColor(R.color.text_secondary), Typeface.NORMAL);
        detail.setLineSpacing(0, 1.15f);
        card.addView(detail, Views.marginParams(context, 0, 5, 0, 8));

        LinearLayout actions = new LinearLayout(context);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button export = Views.compactButton(context, "导出备份");
        export.setOnClickListener(view -> listener.chooseBackupDestination());
        actions.addView(export);

        Button importButton = Views.compactButton(context, "导入备份");
        importButton.setOnClickListener(view -> listener.chooseBackupFile());
        LinearLayout.LayoutParams importParams = Views.wrapParams(Gravity.START);
        importParams.leftMargin = Views.dp(context, 10);
        actions.addView(importButton, importParams);
        card.addView(actions);
        return card;
    }

    private static void refreshUpdateStatus(Context appContext, TextView detail, Button action, long downloadId) {
        if (downloadId <= 0) return;
        new Thread(() -> {
            int status = UpdateManager.downloadStatus(appContext, downloadId);
            MAIN_HANDLER.post(() -> {
                if (detail.getParent() == null) return;
                applyUpdateStatus(appContext, detail, action, downloadId, status);
            });
        }, "birthday-update-status").start();
    }

    private static void applyUpdateStatus(Context appContext, TextView detail, Button action, long downloadId, int status) {
        boolean ready = status == DownloadManager.STATUS_SUCCESSFUL && UpdatePreferences.isVerified(appContext);
        if (ready) {
            detail.setText("版本 " + UpdatePreferences.downloadVersion(appContext) + " 已下载并通过安全校验。");
            action.setText("安装已下载版本");
            action.setOnClickListener(view -> action.getContext().startActivity(
                    new Intent(action.getContext(), UpdateInstallerActivity.class)
                            .putExtra(UpdateInstallerActivity.EXTRA_DOWNLOAD_ID, downloadId)));
        } else if (status == DownloadManager.STATUS_PENDING || status == DownloadManager.STATUS_RUNNING) {
            detail.setText("正在下载版本 " + UpdatePreferences.downloadVersion(appContext) + "。");
        } else {
            detail.setText("当前版本 " + BuildConfig.VERSION_NAME + "，每天自动检查 GitHub Releases。");
        }
    }
}
