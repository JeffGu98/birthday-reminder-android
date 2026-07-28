package com.shukun.birthdayreminder.update;

public final class VersionComparator {
    private VersionComparator() {}

    public static boolean isNewer(String candidate, String installed) {
        int[] left = parse(candidate);
        int[] right = parse(installed);
        int length = Math.max(left.length, right.length);
        for (int index = 0; index < length; index++) {
            int a = index < left.length ? left[index] : 0;
            int b = index < right.length ? right[index] : 0;
            if (a != b) return a > b;
        }
        return false;
    }

    private static int[] parse(String value) {
        String normalized = value == null ? "" : value.trim().replaceFirst("^[vV]", "");
        normalized = normalized.split("[-+]", 2)[0];
        String[] parts = normalized.split("\\.");
        int[] result = new int[Math.max(1, parts.length)];
        for (int index = 0; index < parts.length; index++) {
            try {
                result[index] = Integer.parseInt(parts[index].replaceAll("[^0-9].*$", ""));
            } catch (NumberFormatException error) {
                result[index] = 0;
            }
        }
        return result;
    }
}
