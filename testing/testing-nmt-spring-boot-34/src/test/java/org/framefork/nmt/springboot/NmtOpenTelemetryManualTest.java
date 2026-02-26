package org.framefork.nmt.springboot;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import org.framefork.nmt.core.JcmdNmtDataCollector;
import org.framefork.nmt.opentelemetry.NmtOpenTelemetryMetrics;
import org.framefork.nmt.testing.CannedJcmdRunner;
import org.framefork.nmt.testing.NmtTestResources;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

class NmtOpenTelemetryManualTest {

    @Test
    void manualWiringRegistersGauges() {
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
    void gaugesContainCategoryAttribute() {
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
            var categoryValues = points.stream()
                .map(p -> p.getAttributes().get(AttributeKey.stringKey("category")))
                .toList();

            assertThat(categoryValues).contains("java_heap", "gc", "total");
        }
    }

    private static JcmdNmtDataCollector createCollector() {
        var output = NmtTestResources.loadSample("nmt-summary-jdk17.txt");
        var jcmdRunner = new CannedJcmdRunner(output);
        return new JcmdNmtDataCollector(jcmdRunner);
    }
}
