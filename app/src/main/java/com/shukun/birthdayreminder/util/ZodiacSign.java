package com.shukun.birthdayreminder.util;

public final class ZodiacSign {
    private ZodiacSign() {}

    // SIGNS[i] is the sign that begins in month i+1; START_DAY[i] is its first day.
    private static final String[] SIGNS = {
        "水瓶座", "双鱼座", "白羊座", "金牛座", "双子座", "巨蟹座",
        "狮子座", "处女座", "天秤座", "天蝎座", "射手座", "摩羯座"
    };
    private static final int[] START_DAY = {
        20, 19, 21, 20, 21, 22, 23, 23, 23, 24, 23, 22
    };

    public static String signName(int month, int day) {
        if (month < 1 || month > 12) return "";
        int index = month - 1;
        if (day < START_DAY[index]) {
            index = index == 0 ? 11 : index - 1;
        }
        return SIGNS[index];
    }
}
