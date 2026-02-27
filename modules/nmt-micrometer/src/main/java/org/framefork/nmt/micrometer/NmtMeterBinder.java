package org.framefork.nmt.micrometer;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.BaseUnits;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.framefork.nmt.core.NmtDataCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link MeterBinder} that registers JVM Native Memory Tracking gauges.
 *
 * <p>Registers the following metrics:</p>
 * <ul>
 *   <li>{@code jvm.memory.nmt.committed} -- committed memory per NMT category (real memory used)</li>
 *   <li>{@code jvm.memory.nmt.reserved} -- reserved memory per NMT category (virtual address space)</li>
 * </ul>
 *
 * <p>Each metric is tagged with {@code category} identifying the NMT memory category
 * (e.g., "java_heap", "gc", "thread", "code", etc.).</p>
 */
public final class NmtMeterBinder implements MeterBinder
{

    private static final Logger log = LoggerFactory.getLogger(NmtMeterBinder.class);

    private final NmtDataCollector collector;
    private final Tags extraTags;

    public NmtMeterBinder(NmtDataCollector collector)
    {
        this(collector, Tags.empty());
    }

    public NmtMeterBinder(NmtDataCollector collector, Iterable<Tag> extraTags)
    {
        this.collector = collector;
        this.extraTags = Tags.of(extraTags);
    }

    @Override
    public void bindTo(MeterRegistry registry)
    {
        // We need to register gauges lazily based on what categories the collector returns.
        // First collection determines the categories.
        var summary = collector.collect();

        if (summary.isEmpty()) {
            log.warn("NMT data collection returned no categories; no NMT metrics will be registered");
            return;
        }

        for (var entry : summary.getCategories().entrySet()) {
            var categoryName = entry.getKey();
            var tags = Tags.of("category", categoryName).and(extraTags);

            Gauge.builder(
                    "jvm.memory.nmt.committed", collector, c -> {
                        var cat = c.collect().getCategory(categoryName);
                        return cat != null ? cat.committed() : 0;
                    }
                )
                .tags(tags)
                .description("JVM Native Memory Tracking: committed memory")
                .baseUnit(BaseUnits.BYTES)
                .register(registry);

            Gauge.builder(
                    "jvm.memory.nmt.reserved", collector, c -> {
                        var cat = c.collect().getCategory(categoryName);
                        return cat != null ? cat.reserved() : 0;
                    }
                )
                .tags(tags)
                .description("JVM Native Memory Tracking: reserved memory")
                .baseUnit(BaseUnits.BYTES)
                .register(registry);
        }
    }

}
