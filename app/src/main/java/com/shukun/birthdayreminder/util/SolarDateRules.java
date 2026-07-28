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
}
