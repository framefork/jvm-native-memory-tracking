package org.framefork.nmt.micrometer;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.BaseUnits;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.framefork.nmt.core.NmtCategory;
import org.framefork.nmt.core.NmtDataCollector;
import org.framefork.nmt.core.NmtSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.ToLongFunction;

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
 *
 * <p>Micrometer has one gauge per tag set, and the categories are known only from the NMT output.
 * The binder registers the gauges of every category that a read returns, at bind time and on every gauge read after it,
 * so a category gets its gauges on the first read that reports it. The gauges of the {@code total} category are registered at bind time
 * even when that first read fails, so that later reads happen. A category that a read does not report reads 0.</p>
 */
public final class NmtMeterBinder implements MeterBinder
{

    private static final Logger log = LoggerFactory.getLogger(NmtMeterBinder.class);

    /**
     * The normalized name of the {@code Total} line, which the NMT summary always prints.
     */
    private static final String TOTAL_CATEGORY = "total";

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
        var binding = new RegistryBinding(registry);
        binding.registerGauges(TOTAL_CATEGORY);

        var summary = collector.collect();
        if (summary.isEmpty()) {
            log.warn("NMT data collection returned no categories; the NMT gauges get registered when a later read returns them");
        }
        binding.registerMissingGauges(summary);
    }

    /**
     * The gauges registered in one registry. The binder can be bound to several registries, each one gets its own.
     */
    private final class RegistryBinding
    {

        private final MeterRegistry registry;
        private final Set<String> registeredCategories = ConcurrentHashMap.newKeySet();

        RegistryBinding(MeterRegistry registry)
        {
            this.registry = registry;
        }

        void registerMissingGauges(NmtSummary summary)
        {
            for (var categoryName : summary.getCategories().keySet()) {
                registerGauges(categoryName);
            }
        }

        void registerGauges(String categoryName)
        {
            if (!registeredCategories.add(categoryName)) {
                return;
            }

            var tags = Tags.of("category", categoryName).and(extraTags);

            // The gauge function holds this binding strongly, so the binding lives as long as the gauges.
            Gauge.builder("jvm.memory.nmt.committed", collector, c -> read(categoryName, NmtCategory::committed))
                .tags(tags)
                .description("JVM Native Memory Tracking: committed memory")
                .baseUnit(BaseUnits.BYTES)
                .register(registry);

            Gauge.builder("jvm.memory.nmt.reserved", collector, c -> read(categoryName, NmtCategory::reserved))
                .tags(tags)
                .description("JVM Native Memory Tracking: reserved memory")
                .baseUnit(BaseUnits.BYTES)
                .register(registry);
        }

        private double read(String categoryName, ToLongFunction<NmtCategory> value)
        {
            var summary = collector.collect();
            registerMissingGauges(summary);
            var category = summary.getCategory(categoryName);
            return category != null ? value.applyAsLong(category) : 0;
        }

    }

}
