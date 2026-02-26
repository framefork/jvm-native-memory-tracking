package org.framefork.nmt.core;

/**
 * Collects JVM Native Memory Tracking data.
 */
public interface NmtDataCollector {

    /**
     * Collects NMT data. Returns an empty summary if NMT is not enabled or data is unavailable.
     */
    NativeMemoryTrackingSummary collect();

}
