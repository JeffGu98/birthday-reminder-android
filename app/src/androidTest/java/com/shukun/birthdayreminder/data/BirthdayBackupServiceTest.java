package com.shukun.birthdayreminder.data;

import com.shukun.birthdayreminder.model.BirthdayPerson;

import junit.framework.TestCase;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

public final class BirthdayBackupServiceTest extends TestCase {
    public void testRoundTripUsesPortableFields() throws Exception {
        BirthdayBackupService service = new BirthdayBackupService();
        List<BirthdayPerson> source = Arrays.asList(
                new BirthdayPerson("internal-id", "妈妈", 1974, 4, 10,
                        3, 18, false, "喜欢旅行和鲜花", true),
                new BirthdayPerson("another-id", "朋友", 1990, 7, 29,
                        6, 8, false, "", false)
        );

        String json = service.serialize(source);
        List<BirthdayBackupService.BackupPerson> restored = service.parse(json);

        assertEquals(2, restored.size());
        assertEquals("妈妈", restored.get(0).name);
        assertEquals(1974, restored.get(0).birthYear);
        assertEquals("喜欢旅行和鲜花", restored.get(0).note);
        assertTrue(restored.get(0).enabled);
        assertEquals("朋友", restored.get(1).name);
        assertEquals(29, restored.get(1).birthDay);
        assertFalse(restored.get(1).enabled);
        assertFalse(json.contains("internal-id"));
        assertFalse(json.contains("lunarMonth"));

        List<BirthdayBackupService.BackupPerson> oldBackup = service.parse(
                "{\"format\":\"birthday-reminder-backup\",\"version\":1,"
                        + "\"people\":[{\"name\":\"旧记录\",\"birthYear\":1990,"
                        + "\"birthMonth\":1,\"birthDay\":1}]}");
        assertEquals("", oldBackup.get(0).note);
    }

    public void testRejectsUnknownOrInvalidBackups() throws Exception {
        BirthdayBackupService service = new BirthdayBackupService();
        try {
            service.parse("{\"format\":\"other\",\"version\":1,\"people\":[]}");
            fail("Unknown formats must be rejected");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }

        try {
            service.parse("{\"format\":\"birthday-reminder-backup\",\"version\":1,"
                    + "\"people\":[{\"name\":\"坏日期\",\"birthYear\":2020,"
                    + "\"birthMonth\":2,\"birthDay\":31}]}");
            fail("Invalid dates must be rejected");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    public void testOldLocalRecordsDefaultToEmptyNote() throws Exception {
        JSONObject oldRecord = new JSONObject()
                .put("id", "old-id")
                .put("name", "旧数据")
                .put("birthYear", 1990)
                .put("birthMonth", 1)
                .put("birthDay", 1)
                .put("lunarMonth", 12)
                .put("lunarDay", 5)
                .put("lunarLeapMonth", false)
                .put("enabled", true);

        assertEquals("", BirthdayPerson.fromJson(oldRecord).note);
    }
}
