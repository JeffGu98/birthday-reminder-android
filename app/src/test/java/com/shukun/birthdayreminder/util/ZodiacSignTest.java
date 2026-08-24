package com.shukun.birthdayreminder.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ZodiacSignTest {
    @Test
    public void returnsCapricornForEarlyJanuary() {
        assertEquals("摩羯座", ZodiacSign.signName(1, 15));
    }

    @Test
    public void returnsBoundarySignOnItsFirstDay() {
        assertEquals("水瓶座", ZodiacSign.signName(1, 20));
        assertEquals("白羊座", ZodiacSign.signName(3, 21));
        assertEquals("摩羯座", ZodiacSign.signName(12, 22));
    }

    @Test
    public void returnsPreviousSignJustBeforeBoundary() {
        assertEquals("双鱼座", ZodiacSign.signName(3, 20));
        assertEquals("射手座", ZodiacSign.signName(12, 21));
    }

    @Test
    public void coversRemainingSigns() {
        assertEquals("金牛座", ZodiacSign.signName(5, 1));
        assertEquals("狮子座", ZodiacSign.signName(8, 1));
        assertEquals("处女座", ZodiacSign.signName(9, 1));
        assertEquals("天蝎座", ZodiacSign.signName(11, 1));
    }
}
