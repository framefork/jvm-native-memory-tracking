package org.framefork.nmt.opentelemetry;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.ObservableLongGauge;
import io.opentelemetry.api.metrics.ObservableLongMeasurement;
import org.framefork.nmt.core.NmtCategory;
import org.framefork.nmt.core.NmtDataCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToLongFunction;

/**
 * Registers JVM Native Memory Tracking metrics as OpenTelemetry async long gauges.
 *
 * <p>Registers the following metrics:</p>
 * <ul>
 *   <li>{@code jvm.memory.nmt.committed} -- committed memory per NMT category (real memory used)</li>
 *   <li>{@code jvm.memory.nmt.reserved} -- reserved memory per NMT category (virtual address space)</li>
 * </ul>
 *
 * <p>Each metric has a {@code category} attribute identifying the NMT memory category
 * (e.g., "java_heap", "gc", "thread", "code", etc.).</p>
 *
 * <p>Each gauge records every category that the collector returns at the time of the collection.
 * A category that appears later is exported from that collection on, and a failed read leaves out only that one collection.</p>
 *
 * <p>Implements {@link AutoCloseable} to deregister gauge callbacks on shutdown.</p>
 */
public final class NmtOpenTelemetryMetrics implements AutoCloseable
{

    private static final Logger log = LoggerFactory.getLogger(NmtOpenTelemetryMetrics.class);

    private static final String INSTRUMENTATION_SCOPE = "org.framefork.nmt";
    private static final AttributeKey<String> CATEGORY_KEY = AttributeKey.stringKey("category");

    private final List<ObservableLongGauge> gauges = new ArrayList<>();

    public NmtOpenTelemetryMetrics(OpenTelemetry openTelemetry, NmtDataCollector collector)
    {
        var meter = openTelemetry.getMeter(INSTRUMENTATION_SCOPE);

        gauges.add(
            meter.gaugeBuilder("jvm.memory.nmt.committed")
                .setDescription("JVM Native Memory Tracking: committed memory")
                .setUnit("By")
                .ofLongs()
                .buildWithCallback(measurement -> recordAllCategories(measurement, collector, NmtCategory::committed))
        );

        gauges.add(
            meter.gaugeBuilder("jvm.memory.nmt.reserved")
                .setDescription("JVM Native Memory Tracking: reserved memory")
                .setUnit("By")
                .ofLongs()
                .buildWithCallback(measurement -> recordAllCategories(measurement, collector, NmtCategory::reserved))
        );
    }

    private static void recordAllCategories(ObservableLongMeasurement measurement, NmtDataCollector collector, ToLongFunction<NmtCategory> value)
    {
        for (var entry : collector.collect().getCategories().entrySet()) {
            measurement.record(value.applyAsLong(entry.getValue()), Attributes.of(CATEGORY_KEY, entry.getKey()));
        }
    }

    @Override
    public synchronized void close()
    {
        var unclosed = List.copyOf(gauges);
        gauges.clear();
        for (var gauge : unclosed) {
            try {
                gauge.close();
            } catch (Exception e) {
                log.error("Failed to close NMT gauge: {}", e.getMessage(), e);
            }
        }
    }

}
