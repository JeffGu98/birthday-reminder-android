package com.shukun.birthdayreminder.model;

import org.json.JSONException;
import org.json.JSONObject;

public final class BirthdayPerson {
    public static final int MAX_NOTE_LENGTH = 5_000;

    public final String id;
    public final String name;
    public final int birthYear;
    public final int birthMonth;
    public final int birthDay;
    public final int lunarMonth;
    public final int lunarDay;
    public final boolean lunarLeapMonth;
    public final String note;
    public final boolean enabled;

    public BirthdayPerson(
            String id,
            String name,
            int birthYear,
            int birthMonth,
            int birthDay,
            int lunarMonth,
            int lunarDay,
            boolean lunarLeapMonth,
            String note,
            boolean enabled
    ) {
        this.id = id;
        this.name = name;
        this.birthYear = birthYear;
        this.birthMonth = birthMonth;
        this.birthDay = birthDay;
        this.lunarMonth = lunarMonth;
        this.lunarDay = lunarDay;
        this.lunarLeapMonth = lunarLeapMonth;
        this.note = note == null ? "" : note;
        this.enabled = enabled;
    }

    public BirthdayPerson withEnabled(boolean newValue) {
        return new BirthdayPerson(id, name, birthYear, birthMonth, birthDay,
                lunarMonth, lunarDay, lunarLeapMonth, note, newValue);
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("name", name);
        json.put("birthYear", birthYear);
        json.put("birthMonth", birthMonth);
        json.put("birthDay", birthDay);
        json.put("lunarMonth", lunarMonth);
        json.put("lunarDay", lunarDay);
        json.put("lunarLeapMonth", lunarLeapMonth);
        json.put("note", note);
        json.put("enabled", enabled);
        return json;
    }

    public static BirthdayPerson fromJson(JSONObject json) throws JSONException {
        return new BirthdayPerson(
                json.getString("id"),
                json.getString("name"),
                json.getInt("birthYear"),
                json.getInt("birthMonth"),
                json.getInt("birthDay"),
                json.getInt("lunarMonth"),
                json.getInt("lunarDay"),
                json.getBoolean("lunarLeapMonth"),
                json.optString("note", ""),
                json.optBoolean("enabled", true)
        );
    }
}
