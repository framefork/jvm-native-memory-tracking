package org.framefork.nmt.opentelemetry;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.metrics.ObservableLongGauge;
import org.framefork.nmt.core.NmtDataCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

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
 * <p>Implements {@link AutoCloseable} to deregister gauge callbacks on shutdown.</p>
 */
public final class NmtOpenTelemetryMetrics implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(NmtOpenTelemetryMetrics.class);

    private static final String INSTRUMENTATION_SCOPE = "org.framefork.nmt";
    private static final AttributeKey<String> CATEGORY_KEY = AttributeKey.stringKey("category");

    private final List<ObservableLongGauge> gauges = new ArrayList<>();

    public NmtOpenTelemetryMetrics(OpenTelemetry openTelemetry, NmtDataCollector collector) {
        var meter = openTelemetry.getMeter(INSTRUMENTATION_SCOPE);

        // Do an initial collection to discover categories
        var summary = collector.collect();

        for (var entry : summary.getCategories().entrySet()) {
            var categoryName = entry.getKey();
            var attributes = Attributes.of(CATEGORY_KEY, categoryName);

            var committedGauge = meter.gaugeBuilder("jvm.memory.nmt.committed")
                .setDescription("JVM Native Memory Tracking: committed memory")
                .setUnit("By")
                .ofLongs()
                .buildWithCallback(measurement -> {
                    var cat = collector.collect().getCategory(categoryName);
                    if (cat != null) {
                        measurement.record(cat.committed(), attributes);
                    }
                });
            gauges.add(committedGauge);

            var reservedGauge = meter.gaugeBuilder("jvm.memory.nmt.reserved")
                .setDescription("JVM Native Memory Tracking: reserved memory")
                .setUnit("By")
                .ofLongs()
                .buildWithCallback(measurement -> {
                    var cat = collector.collect().getCategory(categoryName);
                    if (cat != null) {
                        measurement.record(cat.reserved(), attributes);
                    }
                });
            gauges.add(reservedGauge);
        }

        log.debug("Registered NMT metrics for {} categories", summary.getCategories().size());
    }

    @Override
    public synchronized void close() {
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
