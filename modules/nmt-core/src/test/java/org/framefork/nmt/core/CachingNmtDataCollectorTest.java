package org.framefork.nmt.core;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CachingNmtDataCollectorTest
{

    @Test
    void collect_withinTtl_returnsCachedValueWithoutCallingDelegate()
    {
        var callCount = new AtomicInteger(0);
        NmtDataCollector delegate = () -> {
            callCount.incrementAndGet();
            return new NmtSummary(new LinkedHashMap<>());
        };

        var caching = new CachingNmtDataCollector(delegate, Duration.ofSeconds(60));

        caching.collect();
        caching.collect();

        assertThat(callCount.get()).isEqualTo(1);
    }

    @Test
    void collect_afterTtlExpires_callsDelegateAgain() throws InterruptedException
    {
        var callCount = new AtomicInteger(0);
        NmtDataCollector delegate = () -> {
            callCount.incrementAndGet();
            return new NmtSummary(new LinkedHashMap<>());
        };

        var caching = new CachingNmtDataCollector(delegate, Duration.ofMillis(50));

        caching.collect();
        assertThat(callCount.get()).isEqualTo(1);

        Thread.sleep(100);

        caching.collect();
        assertThat(callCount.get()).isEqualTo(2);
    }

    @Test
    void constructor_nullTtl_throws()
    {
        NmtDataCollector delegate = () -> new NmtSummary(new LinkedHashMap<>());

        assertThatThrownBy(() -> new CachingNmtDataCollector(delegate, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("ttl");
    }

    @Test
    void constructor_zeroTtl_throws()
    {
        NmtDataCollector delegate = () -> new NmtSummary(new LinkedHashMap<>());

        assertThatThrownBy(() -> new CachingNmtDataCollector(delegate, Duration.ZERO))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("The cache TTL must be positive, got PT0S");
    }

    @Test
    void constructor_negativeTtl_throws()
    {
        NmtDataCollector delegate = () -> new NmtSummary(new LinkedHashMap<>());

        assertThatThrownBy(() -> new CachingNmtDataCollector(delegate, Duration.ofSeconds(-1)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("The cache TTL must be positive, got PT-1S");
    }

}
