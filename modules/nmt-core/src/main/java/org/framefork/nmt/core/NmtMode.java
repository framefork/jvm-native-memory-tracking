package org.framefork.nmt.core;

import java.util.Locale;

/**
 * NMT tracking modes as configured via {@code -XX:NativeMemoryTracking=<mode>}.
 */
public enum NmtMode {

    OFF,
    SUMMARY,
    DETAIL;

    /**
     * Parses a mode string (case-insensitive) into an {@link NmtMode}.
     * Returns {@link #OFF} for unrecognized values.
     */
    public static NmtMode fromString(String value) {
        try {
            return valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return OFF;
        }
    }

}
