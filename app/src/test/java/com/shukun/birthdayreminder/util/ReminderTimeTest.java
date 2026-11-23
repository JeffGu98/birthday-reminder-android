package com.shukun.birthdayreminder.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ReminderTimeTest {
    @Test
    public void formatsWithTwoDigits() {
        assertEquals("00:00", ReminderTime.MIDNIGHT.format());
        assertEquals("09:05", new ReminderTime(9, 5).format());
        assertEquals("23:59", new ReminderTime(23, 59).format());
    }

    @Test
    public void earlyMorningIsBeforeSix() {
        assertTrue(new ReminderTime(0, 0).isEarlyMorning());
        assertTrue(new ReminderTime(5, 59).isEarlyMorning());
        assertFalse(new ReminderTime(6, 0).isEarlyMorning());
        assertFalse(new ReminderTime(9, 0).isEarlyMorning());
    }

    @Test
    public void fromStoredClampsInvalidValues() {
        assertEquals(new ReminderTime(0, 0), ReminderTime.fromStored(-1, -8));
        assertEquals(new ReminderTime(23, 59), ReminderTime.fromStored(30, 80));
        assertEquals(new ReminderTime(8, 30), ReminderTime.fromStored(8, 30));
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsInvalidHour() {
        new ReminderTime(24, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsInvalidMinute() {
        new ReminderTime(8, 60);
    }
}
