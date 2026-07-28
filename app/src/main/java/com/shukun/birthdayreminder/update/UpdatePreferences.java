package com.shukun.birthdayreminder.update;

import android.content.Context;
import android.content.SharedPreferences;

public final class UpdatePreferences {
    private static final String FILE = "app_updates";
    private static final String AUTO_DOWNLOAD = "auto_download";
    private static final String WIFI_ONLY = "wifi_only";
    private static final String DOWNLOAD_ID = "download_id";
    private static final String DOWNLOAD_VERSION = "download_version";
    private static final String EXPECTED_SHA256 = "expected_sha256";
    private static final String VERIFIED = "verified";

    private UpdatePreferences() {}

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static boolean autoDownload(Context context) {
        return preferences(context).getBoolean(AUTO_DOWNLOAD, true);
    }

    public static void setAutoDownload(Context context, boolean enabled) {
        preferences(context).edit().putBoolean(AUTO_DOWNLOAD, enabled).apply();
    }

    public static boolean wifiOnly(Context context) {
        return preferences(context).getBoolean(WIFI_ONLY, true);
    }

    public static void setWifiOnly(Context context, boolean enabled) {
        preferences(context).edit().putBoolean(WIFI_ONLY, enabled).apply();
    }

    public static long downloadId(Context context) {
        return preferences(context).getLong(DOWNLOAD_ID, -1L);
    }

    public static String downloadVersion(Context context) {
        return preferences(context).getString(DOWNLOAD_VERSION, "");
    }

    public static String expectedSha256(Context context) {
        return preferences(context).getString(EXPECTED_SHA256, "");
    }

    public static boolean isVerified(Context context) {
        return preferences(context).getBoolean(VERIFIED, false);
    }

    public static void saveDownload(Context context, long id, String version, String sha256) {
        preferences(context).edit()
                .putLong(DOWNLOAD_ID, id)
                .putString(DOWNLOAD_VERSION, version)
                .putString(EXPECTED_SHA256, sha256)
                .putBoolean(VERIFIED, false)
                .apply();
    }

    public static void markVerified(Context context) {
        preferences(context).edit().putBoolean(VERIFIED, true).apply();
    }

    public static void clearDownload(Context context) {
        preferences(context).edit()
                .remove(DOWNLOAD_ID)
                .remove(DOWNLOAD_VERSION)
                .remove(EXPECTED_SHA256)
                .remove(VERIFIED)
                .apply();
    }
}
