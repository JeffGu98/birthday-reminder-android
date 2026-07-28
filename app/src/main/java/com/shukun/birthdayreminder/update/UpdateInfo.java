package com.shukun.birthdayreminder.update;

public final class UpdateInfo {
    public final String version;
    public final String title;
    public final String notes;
    public final String releaseUrl;
    public final String apkUrl;
    public final String apkName;
    public final String sha256;

    public UpdateInfo(String version, String title, String notes, String releaseUrl,
                      String apkUrl, String apkName, String sha256) {
        this.version = version;
        this.title = title;
        this.notes = notes;
        this.releaseUrl = releaseUrl;
        this.apkUrl = apkUrl;
        this.apkName = apkName;
        this.sha256 = sha256;
    }
}
