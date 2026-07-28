package com.shukun.birthdayreminder.update;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

public final class UpdateInstallerActivity extends Activity {
    public static final String EXTRA_DOWNLOAD_ID = "download_id";
    private static final int REQUEST_UNKNOWN_SOURCES = 2001;
    private long downloadId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        downloadId = getIntent().getLongExtra(EXTRA_DOWNLOAD_ID, -1L);
        if (downloadId <= 0 || !UpdatePreferences.isVerified(this)) {
            Toast.makeText(this, "没有可安装且已校验的更新", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && !getPackageManager().canRequestPackageInstalls()) {
            explainUnknownSourcesPermission();
        } else {
            launchInstaller();
        }
    }

    private void explainUnknownSourcesPermission() {
        new AlertDialog.Builder(this)
                .setTitle("允许安装新版")
                .setMessage("Android 需要你先允许“生日管家”安装来自 GitHub 的更新。开启后会继续进入系统安装页面。")
                .setNegativeButton("取消", (dialog, which) -> finish())
                .setPositiveButton("去开启", (dialog, which) -> {
                    Intent settings = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse("package:" + getPackageName()));
                    startActivityForResult(settings, REQUEST_UNKNOWN_SOURCES);
                })
                .setOnCancelListener(dialog -> finish())
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_UNKNOWN_SOURCES
                && (Build.VERSION.SDK_INT < Build.VERSION_CODES.O
                || getPackageManager().canRequestPackageInstalls())) {
            launchInstaller();
        } else {
            Toast.makeText(this, "未获得安装权限", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void launchInstaller() {
        Uri apk = getSystemService(DownloadManager.class).getUriForDownloadedFile(downloadId);
        if (apk == null) {
            Toast.makeText(this, "安装包不存在，请重新检查更新", Toast.LENGTH_LONG).show();
            UpdatePreferences.clearDownload(this);
            finish();
            return;
        }
        Intent install = new Intent(Intent.ACTION_VIEW)
                .setDataAndType(apk, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(install);
        } catch (Exception error) {
            Toast.makeText(this, "无法打开系统安装程序", Toast.LENGTH_LONG).show();
        }
        finish();
    }
}
