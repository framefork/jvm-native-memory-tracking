package org.framefork.nmt.opentelemetry;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import org.framefork.nmt.core.JcmdNmtDataCollector;
import org.framefork.nmt.testing.CannedJcmdRunner;
import org.framefork.nmt.testing.NmtTestResources;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class NmtOpenTelemetryMetricsTest {

    @Test
    void constructor_registersCommittedAndReservedGauges() {
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
    void constructor_gaugesContainCategoryAttribute() {
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
    void close_doesNotThrow() {
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

    private static JcmdNmtDataCollector createCollector() {
        var output = NmtTestResources.loadSample("nmt-summary-jdk17.txt");
        var jcmdRunner = new CannedJcmdRunner(output);
        return new JcmdNmtDataCollector(jcmdRunner);
    }

}
