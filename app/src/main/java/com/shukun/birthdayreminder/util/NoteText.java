package com.shukun.birthdayreminder.util;

public final class NoteText {
    private NoteText() {
    }

    public static String preview(String note, int maxCodePoints) {
        if (note == null || maxCodePoints < 1) return "";
        String compact = note.replace('\r', ' ').replace('\n', ' ').trim();
        int count = compact.codePointCount(0, compact.length());
        if (count <= maxCodePoints) return compact;
        int end = compact.offsetByCodePoints(0, maxCodePoints);
        return compact.substring(0, end) + "…";
    }
}
