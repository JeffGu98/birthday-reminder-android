package com.shukun.birthdayreminder.data;

import android.content.Context;
import android.net.Uri;

import com.shukun.birthdayreminder.model.BirthdayPerson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

public final class BirthdayBackupService {
    private static final String FORMAT = "birthday-reminder-backup";
    private static final int FORMAT_VERSION = 1;
    private static final int MAX_PEOPLE = 1_000;
    private static final int MAX_FILE_BYTES = 32 * 1024 * 1024;

    public static final class BackupPerson {
        public final String name;
        public final int birthYear;
        public final int birthMonth;
        public final int birthDay;
        public final String note;
        public final boolean enabled;

        BackupPerson(String name, int birthYear, int birthMonth, int birthDay,
                     String note, boolean enabled) {
            this.name = name;
            this.birthYear = birthYear;
            this.birthMonth = birthMonth;
            this.birthDay = birthDay;
            this.note = note;
            this.enabled = enabled;
        }
    }

    public void write(Context context, Uri destination, List<BirthdayPerson> people) throws Exception {
        byte[] bytes = serialize(people).getBytes(StandardCharsets.UTF_8);
        try (OutputStream output = context.getContentResolver().openOutputStream(destination, "wt")) {
            if (output == null) throw new IllegalStateException("无法打开备份文件");
            output.write(bytes);
            output.flush();
        }
    }

    public List<BackupPerson> read(Context context, Uri source) throws Exception {
        try (InputStream input = context.getContentResolver().openInputStream(source)) {
            if (input == null) throw new IllegalStateException("无法读取备份文件");
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8 * 1024];
            int total = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > MAX_FILE_BYTES) throw new IllegalArgumentException("备份文件过大");
                output.write(buffer, 0, read);
            }
            return parse(new String(output.toByteArray(), StandardCharsets.UTF_8));
        }
    }

    public String serialize(List<BirthdayPerson> people) throws JSONException {
        if (people.size() > MAX_PEOPLE) throw new IllegalArgumentException("生日人数过多，无法导出");
        JSONObject root = new JSONObject();
        root.put("format", FORMAT);
        root.put("version", FORMAT_VERSION);
        root.put("exportedAt", System.currentTimeMillis());
        JSONArray array = new JSONArray();
        for (BirthdayPerson person : people) {
            JSONObject item = new JSONObject();
            item.put("name", person.name);
            item.put("birthYear", person.birthYear);
            item.put("birthMonth", person.birthMonth);
            item.put("birthDay", person.birthDay);
            item.put("note", person.note);
            item.put("enabled", person.enabled);
            array.put(item);
        }
        root.put("people", array);
        return root.toString(2);
    }

    public List<BackupPerson> parse(String raw) throws JSONException {
        JSONObject root = new JSONObject(raw);
        if (!FORMAT.equals(root.optString("format"))) {
            throw new IllegalArgumentException("不是生日管家的备份文件");
        }
        if (root.optInt("version", -1) != FORMAT_VERSION) {
            throw new IllegalArgumentException("暂不支持这个备份版本");
        }
        JSONArray array = root.getJSONArray("people");
        if (array.length() > MAX_PEOPLE) throw new IllegalArgumentException("备份人数过多");

        List<BackupPerson> result = new ArrayList<>();
        for (int index = 0; index < array.length(); index++) {
            JSONObject item = array.getJSONObject(index);
            String name = item.getString("name").trim();
            int year = item.getInt("birthYear");
            int month = item.getInt("birthMonth");
            int day = item.getInt("birthDay");
            String note = item.optString("note", "");
            validate(name, year, month, day, note);
            result.add(new BackupPerson(
                    name, year, month, day, note, item.optBoolean("enabled", true)));
        }
        return result;
    }

    private void validate(String name, int year, int month, int day, String note) {
        if (name.isEmpty() || name.length() > 80) {
            throw new IllegalArgumentException("备份中存在无效姓名");
        }
        if (year < 1901) throw new IllegalArgumentException("备份中存在过早的出生日期");
        if (note.length() > BirthdayPerson.MAX_NOTE_LENGTH) {
            throw new IllegalArgumentException("备份中存在过长的备注");
        }
        Calendar date = new GregorianCalendar();
        date.setLenient(false);
        date.clear();
        try {
            date.set(year, month - 1, day, 0, 0, 0);
            date.getTime();
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("备份中存在无效出生日期");
        }
        Calendar endOfToday = Calendar.getInstance();
        endOfToday.set(Calendar.HOUR_OF_DAY, 23);
        endOfToday.set(Calendar.MINUTE, 59);
        endOfToday.set(Calendar.SECOND, 59);
        if (date.after(endOfToday)) throw new IllegalArgumentException("备份中存在未来出生日期");
    }
}
