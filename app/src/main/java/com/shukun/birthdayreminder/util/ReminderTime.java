package com.shukun.birthdayreminder.util;

import java.util.Locale;

public final class ReminderTime {
    public static final ReminderTime MIDNIGHT = new ReminderTime(0, 0);

    public final int hour;
    public final int minute;

    public ReminderTime(int hour, int minute) {
        if (hour < 0 || hour > 23) {
            throw new IllegalArgumentException("hour out of range");
        }
        if (minute < 0 || minute > 59) {
            throw new IllegalArgumentException("minute out of range");
        }
        this.hour = hour;
        this.minute = minute;
    }

    public static ReminderTime fromStored(int hour, int minute) {
        return new ReminderTime(clamp(hour, 0, 23), clamp(minute, 0, 59));
    }

    public boolean isEarlyMorning() {
        return hour < 6;
    }

    public String format() {
        return String.format(Locale.CHINA, "%02d:%02d", hour, minute);
    }

    @Override
    public boolean equals(Object value) {
        if (!(value instanceof ReminderTime)) return false;
        ReminderTime other = (ReminderTime) value;
        return hour == other.hour && minute == other.minute;
    }

    @Override
    public int hashCode() {
        return hour * 60 + minute;
    }

    @Override
    public String toString() {
        return format();
    }

    private static int clamp(int value, int min, int max) {
        return Math.min(max, Math.max(min, value));
    }
}
