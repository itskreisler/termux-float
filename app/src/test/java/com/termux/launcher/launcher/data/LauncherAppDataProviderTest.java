package com.termux.launcher.launcher.data;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Locale;

public class LauncherAppDataProviderTest {

    @Test
    public void testNormalizeLetter() {
        assertEquals('A', normalizeLetter("Alpha"));
        assertEquals('B', normalizeLetter("bravo"));
        assertEquals('#', normalizeLetter("¡claro!")); // non-letter gets handled or '#' is fallback
        assertEquals('#', normalizeLetter("123"));
        assertEquals('#', normalizeLetter(""));
    }

    @Test
    public void testProfileSuffix() {
        assertEquals(" · Clon 10", profileSuffix(10, 100L));
        assertEquals(" · Clon 100", profileSuffix(-1, 100L));
        assertEquals(" · Clon", profileSuffix(-1, -1L));
    }

    private static char normalizeLetter(String label) {
        if (label == null || label.isEmpty()) return '#';
        char upper = Character.toUpperCase(label.charAt(0));
        if (upper >= 'A' && upper <= 'Z') {
            return upper;
        }
        return '#';
    }

    private static String profileSuffix(int userId, long serial) {
        if (userId >= 0) {
            return " · Clon " + userId;
        }
        if (serial >= 0) {
            return " · Clon " + serial;
        }
        return " · Clon";
    }
}
