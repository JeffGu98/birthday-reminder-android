package com.shukun.birthdayreminder.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class SolarDateRulesTest {
    @Test
    public void february29FallsBackToFebruary28InCommonYear() {
        assertEquals(new SolarDate(2025, 2, 28), SolarDateRules.birthdayInYear(2025, 2, 29));
    }

    @Test
    public void february29StaysInLeapYear() {
        assertEquals(new SolarDate(2028, 2, 29), SolarDateRules.birthdayInYear(2028, 2, 29));
    }

    @Test
    public void passedBirthdayMovesToNextYear() {
        Calendar now = new GregorianCalendar(2026, Calendar.JULY, 28, 12, 0, 0);
        assertEquals(new SolarDate(2027, 7, 28), SolarDateRules.nextBirthday(7, 28, now.getTimeInMillis()));
    }
}
