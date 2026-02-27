package org.framefork.nmt.core;

import org.jspecify.annotations.Nullable;

import java.time.Duration;

/**
 * Wraps an {@link NmtDataCollector} with time-based caching to avoid
 * running jcmd too frequently when multiple metric callbacks fire
 * within the same collection cycle.
 */
public final class CachingNmtDataCollector implements NmtDataCollector
{

    private static final Duration DEFAULT_TTL = Duration.ofSeconds(5);

    private final NmtDataCollector delegate;
    private final long ttlNanos;

    private volatile @Nullable NmtSummary cachedValue;
    private volatile long cachedAt;

    public CachingNmtDataCollector(NmtDataCollector delegate)
    {
        this(delegate, DEFAULT_TTL);
    }

    public CachingNmtDataCollector(NmtDataCollector delegate, Duration ttl)
    {
        this.delegate = delegate;
        this.ttlNanos = ttl.toNanos();
    }

    @Override
    public NmtSummary collect()
    {
        var now = System.nanoTime();
        var cached = cachedValue;
        if (cached != null && (now - cachedAt) < ttlNanos) {
            return cached;
        }

        synchronized (this) {
            // Double-check after acquiring lock
            cached = cachedValue;
            if (cached != null && (System.nanoTime() - cachedAt) < ttlNanos) {
                return cached;
            }

            var result = delegate.collect();
            cachedValue = result;
            cachedAt = System.nanoTime();
            return result;
        }
    }

}
