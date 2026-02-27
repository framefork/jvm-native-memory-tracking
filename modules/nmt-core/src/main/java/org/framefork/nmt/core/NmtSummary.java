package org.framefork.nmt.core;

import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable summary of JVM Native Memory Tracking data, as reported by {@code jcmd VM.native_memory summary}.
 */
public final class NmtSummary
{

    private final Map<String, NmtCategory> categories;

    public NmtSummary(Map<String, NmtCategory> categories)
    {
        this.categories = Collections.unmodifiableMap(new LinkedHashMap<>(categories));
    }

    /**
     * Returns all NMT categories, keyed by normalized name (e.g., "java_heap", "gc", "total").
     */
    public Map<String, NmtCategory> getCategories()
    {
        return categories;
    }

    /**
     * Returns a specific category by normalized name, or {@code null} if not present.
     */
    public @Nullable NmtCategory getCategory(String normalizedName)
    {
        return categories.get(normalizedName);
    }

    public boolean isEmpty()
    {
        return categories.isEmpty();
    }

    @Override
    public String toString()
    {
        return "NmtSummary{categories=" + categories.keySet() + "}";
    }

}
