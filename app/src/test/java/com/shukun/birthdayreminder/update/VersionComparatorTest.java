package com.shukun.birthdayreminder.update;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class VersionComparatorTest {
    @Test
    public void comparesSemanticVersions() {
        assertTrue(VersionComparator.isNewer("v1.2.0", "1.1.9"));
        assertTrue(VersionComparator.isNewer("2.0", "1.99.99"));
        assertFalse(VersionComparator.isNewer("1.2.0", "1.2.0"));
        assertFalse(VersionComparator.isNewer("1.1.9", "1.2.0"));
    }
}
