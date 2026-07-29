package com.shukun.birthdayreminder.util;

import java.util.Calendar;
import java.util.GregorianCalendar;

public final class SolarDateRules {
    private SolarDateRules() {}

    public static SolarDate birthdayInYear(int year, int month, int day) {
        if (month == 2 && day == 29 && !new GregorianCalendar().isLeapYear(year)) {
            return new SolarDate(year, 2, 28);
        }
        Calendar calendar = new GregorianCalendar();
        calendar.setLenient(false);
        calendar.clear();
        calendar.set(year, month - 1, day);
        calendar.getTime();
        return new SolarDate(year, month, day);
    }

    public static SolarDate nextBirthday(int birthMonth, int birthDay, long nowMillis) {
        Calendar now = Calendar.getInstance();
        now.setTimeInMillis(nowMillis);
        int year = now.get(Calendar.YEAR);
        SolarDate candidate = birthdayInYear(year, birthMonth, birthDay);
        if (candidate.atLocalMidnightMillis() <= nowMillis) {
            candidate = birthdayInYear(year + 1, birthMonth, birthDay);
        }
        return candidate;
    }

    public static long justBeforeTodayMillis(long nowMillis) {
        Calendar today = Calendar.getInstance();
        today.setTimeInMillis(nowMillis);
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        return today.getTimeInMillis() - 1L;
    }

    public static int daysUntil(SolarDate target, long nowMillis) {
        Calendar cursor = Calendar.getInstance();
        cursor.setTimeInMillis(nowMillis);
        cursor.set(Calendar.HOUR_OF_DAY, 12);
        cursor.set(Calendar.MINUTE, 0);
        cursor.set(Calendar.SECOND, 0);
        cursor.set(Calendar.MILLISECOND, 0);

        Calendar targetDay = Calendar.getInstance();
        targetDay.clear();
        targetDay.set(target.year, target.month - 1, target.day, 12, 0, 0);
        if (!cursor.before(targetDay)) return 0;

        int days = 0;
        while (cursor.before(targetDay)) {
            cursor.add(Calendar.DAY_OF_MONTH, 1);
            days++;
        }
        return days;
    }
}
