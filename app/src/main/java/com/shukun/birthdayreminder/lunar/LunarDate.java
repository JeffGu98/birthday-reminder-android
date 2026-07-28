package com.shukun.birthdayreminder.lunar;

public final class LunarDate {
    public final int month;
    public final int day;
    public final boolean leapMonth;
    public final int extendedYear;

    public LunarDate(int month, int day, boolean leapMonth, int extendedYear) {
        this.month = month;
        this.day = day;
        this.leapMonth = leapMonth;
        this.extendedYear = extendedYear;
    }

    public boolean sameBirthday(LunarDate other) {
        return month == other.month && day == other.day && leapMonth == other.leapMonth;
    }
}
