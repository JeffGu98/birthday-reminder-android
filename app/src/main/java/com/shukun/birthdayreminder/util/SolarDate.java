package com.shukun.birthdayreminder.util;

import java.util.Calendar;
import java.util.GregorianCalendar;

public final class SolarDate implements Comparable<SolarDate> {
    public final int year;
    public final int month;
    public final int day;

    public SolarDate(int year, int month, int day) {
        this.year = year;
        this.month = month;
        this.day = day;
    }

    public long atLocalMidnightMillis() {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(year, month - 1, day, 0, 0, 0);
        return calendar.getTimeInMillis();
    }

    public SolarDate plusDays(int days) {
        Calendar calendar = new GregorianCalendar(year, month - 1, day);
        calendar.add(Calendar.DAY_OF_MONTH, days);
        return from(calendar);
    }

    public static SolarDate from(Calendar calendar) {
        return new SolarDate(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH)
        );
    }

    @Override
    public int compareTo(SolarDate other) {
        if (year != other.year) return Integer.compare(year, other.year);
        if (month != other.month) return Integer.compare(month, other.month);
        return Integer.compare(day, other.day);
    }

    @Override
    public boolean equals(Object value) {
        if (!(value instanceof SolarDate)) return false;
        SolarDate other = (SolarDate) value;
        return year == other.year && month == other.month && day == other.day;
    }

    @Override
    public int hashCode() {
        return year * 10_000 + month * 100 + day;
    }

    @Override
    public String toString() {
        return String.format(java.util.Locale.CHINA, "%04d-%02d-%02d", year, month, day);
    }
}
