package org.framefork.nmt.opentelemetry;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import org.framefork.nmt.core.DiagnosticCommandNmtDataCollector;
import org.framefork.nmt.core.NmtSummary;
import org.framefork.nmt.testing.CannedDiagnosticCommandRunner;
import org.framefork.nmt.testing.NmtTestResources;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class NmtOpenTelemetryMetricsTest
{

    @Test
    void constructor_registersCommittedAndReservedGauges()
    {
        var reader = InMemoryMetricReader.create();
        var sdkMeterProvider = SdkMeterProvider.builder()
            .registerMetricReader(reader)
            .build();
        var openTelemetry = OpenTelemetrySdk.builder()
            .setMeterProvider(sdkMeterProvider)
            .build();

        try (var metrics = new NmtOpenTelemetryMetrics(openTelemetry, createCollector())) {
            assertThat(metrics).isNotNull();

            Collection<MetricData> metricData = reader.collectAllMetrics();

            var metricNames = metricData.stream()
                .map(MetricData::getName)
                .toList();

            assertThat(metricNames).contains("jvm.memory.nmt.committed");
            assertThat(metricNames).contains("jvm.memory.nmt.reserved");
        }
    }

    @Test
    void constructor_gaugesContainCategoryAttribute()
    {
        var reader = InMemoryMetricReader.create();
        var sdkMeterProvider = SdkMeterProvider.builder()
            .registerMetricReader(reader)
            .build();
        var openTelemetry = OpenTelemetrySdk.builder()
            .setMeterProvider(sdkMeterProvider)
            .build();

        try (var metrics = new NmtOpenTelemetryMetrics(openTelemetry, createCollector())) {
            assertThat(metrics).isNotNull();

            Collection<MetricData> metricData = reader.collectAllMetrics();

            var committedMetric = metricData.stream()
                .filter(m -> m.getName().equals("jvm.memory.nmt.committed"))
                .findFirst()
                .orElseThrow();

            var points = committedMetric.getLongGaugeData().getPoints();
            assertThat(points).isNotEmpty();

            var categoryValues = points.stream()
                .map(p -> p.getAttributes().get(AttributeKey.stringKey("category")))
                .toList();

            assertThat(categoryValues).contains("java_heap", "gc", "total");
        }
    }

    @Test
    void close_doesNotThrow()
    {
        var reader = InMemoryMetricReader.create();
        var sdkMeterProvider = SdkMeterProvider.builder()
            .registerMetricReader(reader)
            .build();
        var openTelemetry = OpenTelemetrySdk.builder()
            .setMeterProvider(sdkMeterProvider)
            .build();

        var metrics = new NmtOpenTelemetryMetrics(openTelemetry, createCollector());

        assertThatCode(metrics::close).doesNotThrowAnyException();
    }

    @Test
    void exportsTheCategoriesOnceAReadAfterAnEmptyFirstReadReturnsThem()
    {
        var reader = InMemoryMetricReader.create();
        var openTelemetry = OpenTelemetrySdk.builder()
            .setMeterProvider(SdkMeterProvider.builder().registerMetricReader(reader).build())
            .build();
        var summary = new AtomicReference<>(new NmtSummary(Map.of()));

        try (var metrics = new NmtOpenTelemetryMetrics(openTelemetry, summary::get)) {
            assertThat(metrics).isNotNull();
            assertThat(committedByCategory(reader.collectAllMetrics())).isEmpty();

            summary.set(loadSummary("nmt-summary-jdk17.txt"));

            assertThat(committedByCategory(reader.collectAllMetrics()))
                .containsEntry("java_heap", 1_174_405_120L)
                .containsEntry("gc", 89_267_720L);
        }
    }

    @Test
    void exportsCategoriesThatAppearAfterTheFirstRead()
    {
        var reader = InMemoryMetricReader.create();
        var openTelemetry = OpenTelemetrySdk.builder()
            .setMeterProvider(SdkMeterProvider.builder().registerMetricReader(reader).build())
            .build();
        var summary = new AtomicReference<>(loadSummary("nmt-summary-minimal.txt"));

        try (var metrics = new NmtOpenTelemetryMetrics(openTelemetry, summary::get)) {
            assertThat(metrics).isNotNull();
            assertThat(committedByCategory(reader.collectAllMetrics())).containsKey("java_heap").doesNotContainKey("metaspace");

            summary.set(loadSummary("nmt-summary-jdk17.txt"));

            assertThat(committedByCategory(reader.collectAllMetrics())).containsEntry("metaspace", 38_997_520L);
        }
    }

    private static Map<String, Long> committedByCategory(Collection<MetricData> metricData)
    {
        return metricData.stream()
            .filter(metric -> metric.getName().equals("jvm.memory.nmt.committed"))
            .flatMap(metric -> metric.getLongGaugeData().getPoints().stream())
            .collect(Collectors.toMap(point -> String.valueOf(point.getAttributes().get(AttributeKey.stringKey("category"))), LongPointData::getValue));
    }

    private static NmtSummary loadSummary(String sampleName)
    {
        return new DiagnosticCommandNmtDataCollector(new CannedDiagnosticCommandRunner(NmtTestResources.loadSample(sampleName))).collect();
    }

    private static DiagnosticCommandNmtDataCollector createCollector()
    {
        var output = NmtTestResources.loadSample("nmt-summary-jdk17.txt");
        var runner = new CannedDiagnosticCommandRunner(output);
        return new DiagnosticCommandNmtDataCollector(runner);
    }

}
