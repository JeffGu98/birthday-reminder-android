package com.shukun.birthdayreminder.lunar;

import android.icu.util.Calendar;
import android.icu.util.ChineseCalendar;
import android.icu.util.TimeZone;
import android.icu.util.ULocale;

import com.shukun.birthdayreminder.util.SolarDate;

import java.util.GregorianCalendar;
import java.util.Locale;

public final class LunarCalendarService {
    private static final TimeZone CHINA_ICU_ZONE = TimeZone.getTimeZone("GMT+08:00");
    private static final java.util.TimeZone CHINA_JAVA_ZONE = java.util.TimeZone.getTimeZone("GMT+08:00");

    private static final String[] MONTH_NAMES = {
            "", "正月", "二月", "三月", "四月", "五月", "六月",
            "七月", "八月", "九月", "十月", "冬月", "腊月"
    };
    private static final String[] DAY_NAMES = {
            "", "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
            "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
            "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
    };
    private static final String[] STEMS = {"甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸"};
    private static final String[] BRANCHES = {"子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥"};

    public LunarDate solarToLunar(int year, int month, int day) {
        GregorianCalendar solar = new GregorianCalendar(CHINA_JAVA_ZONE, Locale.CHINA);
        solar.clear();
        // Noon prevents an instant near a time-zone boundary from resolving to the adjacent date.
        solar.set(year, month - 1, day, 12, 0, 0);

        ChineseCalendar lunar = new ChineseCalendar(CHINA_ICU_ZONE, ULocale.CHINA);
        lunar.setTimeInMillis(solar.getTimeInMillis());
        return new LunarDate(
                lunar.get(Calendar.MONTH) + 1,
                lunar.get(Calendar.DAY_OF_MONTH),
                lunar.get(Calendar.IS_LEAP_MONTH) == 1,
                lunar.get(Calendar.EXTENDED_YEAR)
        );
    }

    public SolarDate nextLunarBirthday(LunarDate birthday, long nowMillis) {
        java.util.Calendar now = java.util.Calendar.getInstance();
        now.setTimeInMillis(nowMillis);
        int startYear = now.get(java.util.Calendar.YEAR);
        for (int year = startYear; year <= startYear + 3; year++) {
            SolarDate candidate = occurrenceInGregorianYear(birthday, year);
            if (candidate != null && candidate.atLocalMidnightMillis() > nowMillis) {
                return candidate;
            }
        }
        throw new IllegalStateException("未来三年内未找到农历生日");
    }

    public SolarDate occurrenceInGregorianYear(LunarDate birthday, int gregorianYear) {
        SolarDate exact = null;
        SolarDate leapFallback = null;
        SolarDate shortMonthFallback = null;
        SolarDate shortLeapFallback = null;

        GregorianCalendar cursor = new GregorianCalendar(CHINA_JAVA_ZONE, Locale.CHINA);
        cursor.clear();
        cursor.set(gregorianYear, java.util.Calendar.JANUARY, 1, 12, 0, 0);

        while (cursor.get(java.util.Calendar.YEAR) == gregorianYear) {
            SolarDate solar = SolarDate.from(cursor);
            LunarDate current = solarToLunar(solar.year, solar.month, solar.day);
            if (current.month == birthday.month) {
                if (current.day == birthday.day) {
                    if (current.leapMonth == birthday.leapMonth) {
                        exact = solar;
                    } else if (birthday.leapMonth && !current.leapMonth) {
                        leapFallback = solar;
                    }
                }
                if (birthday.day == 30 && current.day == 29 && isLastDayOfLunarMonth(solar, current)) {
                    if (current.leapMonth == birthday.leapMonth) {
                        shortMonthFallback = solar;
                    } else if (birthday.leapMonth && !current.leapMonth) {
                        shortLeapFallback = solar;
                    }
                }
            }
            cursor.add(java.util.Calendar.DAY_OF_MONTH, 1);
        }

        if (exact != null) return exact;
        if (leapFallback != null) return leapFallback;
        if (shortMonthFallback != null) return shortMonthFallback;
        return shortLeapFallback;
    }

    private boolean isLastDayOfLunarMonth(SolarDate solar, LunarDate current) {
        SolarDate tomorrow = solar.plusDays(1);
        LunarDate next = solarToLunar(tomorrow.year, tomorrow.month, tomorrow.day);
        return next.month != current.month || next.leapMonth != current.leapMonth;
    }

    public String format(LunarDate date, boolean includeYear) {
        String prefix = date.leapMonth ? "闰" : "";
        String value = prefix + MONTH_NAMES[date.month] + DAY_NAMES[date.day];
        if (!includeYear) return value;
        int cycleIndex = Math.floorMod(date.extendedYear - 1, 60);
        return STEMS[cycleIndex % 10] + BRANCHES[cycleIndex % 12] + "年" + value;
    }

    public String formatBirthday(int month, int day, boolean leapMonth) {
        return format(new LunarDate(month, day, leapMonth, 1), false);
    }
}
