package com.shukun.birthdayreminder.update;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;

import com.shukun.birthdayreminder.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

public final class UpdateManager {
    private static final String LATEST_RELEASE_API =
            "https://api.github.com/repos/JeffGu98/birthday-reminder-android/releases/latest";
    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 20_000;

    public interface Callback {
        void onComplete(UpdateInfo update, Exception error);
    }

    private UpdateManager() {}

    public static void checkAsync(Context context, Callback callback) {
        new Thread(() -> {
            try {
                UpdateInfo latest = fetchLatest();
                UpdateInfo update = VersionComparator.isNewer(latest.version, BuildConfig.VERSION_NAME)
                        ? latest : null;
                callback.onComplete(update, null);
            } catch (Exception error) {
                callback.onComplete(null, error);
            }
        }, "github-update-check").start();
    }

    public static UpdateInfo fetchLatest() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(LATEST_RELEASE_API).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
        connection.setRequestProperty("User-Agent", "BirthdayReminder-Android");
        try {
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                throw new IllegalStateException("GitHub 返回状态码 " + status);
            }
            String json = readAll(connection.getInputStream());
            JSONObject release = new JSONObject(json);
            String tag = release.optString("tag_name", "");
            String version = tag.replaceFirst("^[vV]", "");
            JSONArray assets = release.optJSONArray("assets");
            JSONObject apkAsset = null;
            if (assets != null) {
                for (int index = 0; index < assets.length(); index++) {
                    JSONObject asset = assets.getJSONObject(index);
                    if (asset.optString("name", "").toLowerCase(Locale.ROOT).endsWith(".apk")) {
                        apkAsset = asset;
                        if (asset.optString("name", "").startsWith("birthday-reminder-")) break;
                    }
                }
            }
            if (version.isEmpty() || apkAsset == null) {
                throw new IllegalStateException("最新发布版本缺少版本号或 APK");
            }
            String digest = apkAsset.optString("digest", "");
            String sha256 = digest.startsWith("sha256:") ? digest.substring(7) : "";
            return new UpdateInfo(
                    version,
                    release.optString("name", "版本 " + version),
                    release.optString("body", ""),
                    release.optString("html_url", ""),
                    apkAsset.getString("browser_download_url"),
                    apkAsset.getString("name"),
                    sha256
            );
        } finally {
            connection.disconnect();
        }
    }

    public static long download(Context context, UpdateInfo update) {
        if (!update.sha256.matches("(?i)[0-9a-f]{64}")) {
            throw new IllegalStateException("发布文件缺少 SHA-256 校验值");
        }
        long existing = UpdatePreferences.downloadId(context);
        int existingStatus = downloadStatus(context, existing);
        boolean sameVersion = update.version.equals(UpdatePreferences.downloadVersion(context));
        if (existing > 0 && sameVersion && (existingStatus == DownloadManager.STATUS_PENDING
                || existingStatus == DownloadManager.STATUS_RUNNING
                || (existingStatus == DownloadManager.STATUS_SUCCESSFUL
                && UpdatePreferences.isVerified(context)))) {
            return existing;
        }
        if (existing > 0) {
            context.getSystemService(DownloadManager.class).remove(existing);
            UpdatePreferences.clearDownload(context);
        }

        File directory = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (directory == null) throw new IllegalStateException("无法访问下载目录");
        File destination = new File(directory, update.apkName);
        if (destination.exists() && !destination.delete()) {
            throw new IllegalStateException("无法替换旧的安装包");
        }

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(update.apkUrl))
                .setTitle("生日管家 " + update.version)
                .setDescription("正在下载新版安装包")
                .setMimeType("application/vnd.android.package-archive")
                .setAllowedOverMetered(!UpdatePreferences.wifiOnly(context))
                .setAllowedOverRoaming(false)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, update.apkName);
        long id = context.getSystemService(DownloadManager.class).enqueue(request);
        UpdatePreferences.saveDownload(context, id, update.version, update.sha256.toLowerCase(Locale.ROOT));
        return id;
    }

    public static int downloadStatus(Context context, long id) {
        if (id <= 0) return -1;
        DownloadManager.Query query = new DownloadManager.Query().setFilterById(id);
        try (Cursor cursor = context.getSystemService(DownloadManager.class).query(query)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
            }
        }
        return -1;
    }

    public static boolean verifyDownloadedApk(Context context, long id) {
        if (id != UpdatePreferences.downloadId(context)) return false;
        Uri uri = context.getSystemService(DownloadManager.class).getUriForDownloadedFile(id);
        if (uri == null) return false;
        String expected = UpdatePreferences.expectedSha256(context);
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            if (input == null) return false;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[16 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) digest.update(buffer, 0, read);
            StringBuilder actual = new StringBuilder(64);
            for (byte value : digest.digest()) actual.append(String.format(Locale.ROOT, "%02x", value));
            boolean valid = expected.equalsIgnoreCase(actual.toString());
            if (valid) UpdatePreferences.markVerified(context);
            else UpdatePreferences.clearDownload(context);
            return valid;
        } catch (Exception error) {
            UpdatePreferences.clearDownload(context);
            return false;
        }
    }

    public static void cleanupInstalledUpdate(Context context) {
        String downloadedVersion = UpdatePreferences.downloadVersion(context);
        if (downloadedVersion.isEmpty()
                || VersionComparator.isNewer(downloadedVersion, BuildConfig.VERSION_NAME)) return;
        long id = UpdatePreferences.downloadId(context);
        if (id > 0) context.getSystemService(DownloadManager.class).remove(id);
        UpdatePreferences.clearDownload(context);
    }

    private static String readAll(InputStream input) throws Exception {
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) result.append(line).append('\n');
        }
        return result.toString();
    }
}
