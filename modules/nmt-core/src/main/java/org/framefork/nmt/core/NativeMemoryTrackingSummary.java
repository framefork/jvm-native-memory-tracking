package org.framefork.nmt.core;

import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable summary of JVM Native Memory Tracking data, as reported by {@code jcmd VM.native_memory summary}.
 */
public final class NativeMemoryTrackingSummary {

    private final Map<String, NativeMemoryCategory> categories;

    public NativeMemoryTrackingSummary(Map<String, NativeMemoryCategory> categories) {
        this.categories = Collections.unmodifiableMap(new LinkedHashMap<>(categories));
    }

    /**
     * Returns all NMT categories, keyed by normalized name (e.g., "java_heap", "gc", "total").
     */
    public Map<String, NativeMemoryCategory> getCategories() {
        return categories;
    }

    /**
     * Returns a specific category by normalized name, or {@code null} if not present.
     */
    public @Nullable NativeMemoryCategory getCategory(String normalizedName) {
        return categories.get(normalizedName);
    }

    public boolean isEmpty() {
        return categories.isEmpty();
    }

    @Override
    public String toString() {
        return "NativeMemoryTrackingSummary{categories=" + categories.keySet() + "}";
    }
}
