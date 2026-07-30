package com.shukun.birthdayreminder.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class BirthdayPersonTest {
    @Test
    public void changingNotePreservesBirthdayAndReminderState() {
        BirthdayPerson original = new BirthdayPerson(
                "id", "妈妈", 1974, 4, 10,
                3, 18, false, "旧备注", true);

        BirthdayPerson updated = original.withNote("新备注");

        assertEquals("id", updated.id);
        assertEquals("妈妈", updated.name);
        assertEquals(1974, updated.birthYear);
        assertEquals("新备注", updated.note);
        assertTrue(updated.enabled);
    }
}
