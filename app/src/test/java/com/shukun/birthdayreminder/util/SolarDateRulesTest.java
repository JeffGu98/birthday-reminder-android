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

    @Test
    public void countdownTreatsTheWholeBirthdayAsToday() {
        Calendar now = new GregorianCalendar(2026, Calendar.JULY, 29, 18, 30, 0);
        long reference = SolarDateRules.justBeforeTodayMillis(now.getTimeInMillis());

        assertEquals(new SolarDate(2026, 7, 29),
                SolarDateRules.nextBirthday(7, 29, reference));
        assertEquals(0, SolarDateRules.daysUntil(
                new SolarDate(2026, 7, 29), now.getTimeInMillis()));
        assertEquals(1, SolarDateRules.daysUntil(
                new SolarDate(2026, 7, 30), now.getTimeInMillis()));
        assertEquals(184, SolarDateRules.daysUntil(
                new SolarDate(2027, 1, 29), now.getTimeInMillis()));
    }
}
