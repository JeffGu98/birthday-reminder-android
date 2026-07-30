package com.shukun.birthdayreminder.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class NoteTextTest {
    @Test
    public void keepsShortNotesAndTruncatesLongNotes() {
        assertEquals("喜欢鲜花", NoteText.preview("喜欢鲜花", 10));
        assertEquals("一二三四五六七八九十…", NoteText.preview("一二三四五六七八九十一", 10));
    }

    @Test
    public void countsEmojiAsOneCharacterAndFlattensNewlines() {
        assertEquals("😀一…", NoteText.preview("😀一二", 2));
        assertEquals("第一行 第二行", NoteText.preview("第一行\n第二行", 10));
    }
}
