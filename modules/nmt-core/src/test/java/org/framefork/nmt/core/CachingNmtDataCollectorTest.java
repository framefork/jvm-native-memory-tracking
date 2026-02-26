package org.framefork.nmt.core;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class CachingNmtDataCollectorTest {

    @Test
    void collect_withinTtl_returnsCachedValueWithoutCallingDelegate() {
        var callCount = new AtomicInteger(0);
        NmtDataCollector delegate = () -> {
            callCount.incrementAndGet();
            return new NativeMemoryTrackingSummary(new LinkedHashMap<>());
        };

        var caching = new CachingNmtDataCollector(delegate, Duration.ofSeconds(60));

        caching.collect();
        caching.collect();

        assertThat(callCount.get()).isEqualTo(1);
    }

    @Test
    void collect_afterTtlExpires_callsDelegateAgain() throws InterruptedException {
        var callCount = new AtomicInteger(0);
        NmtDataCollector delegate = () -> {
            callCount.incrementAndGet();
            return new NativeMemoryTrackingSummary(new LinkedHashMap<>());
        };

        var caching = new CachingNmtDataCollector(delegate, Duration.ofMillis(50));

        caching.collect();
        assertThat(callCount.get()).isEqualTo(1);

        Thread.sleep(100);

        caching.collect();
        assertThat(callCount.get()).isEqualTo(2);
    }

}
