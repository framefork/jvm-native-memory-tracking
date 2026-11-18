package org.framefork.nmt.springboot;

import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import org.framefork.nmt.core.autoconfigure.NmtCoreAutoConfiguration;
import org.framefork.nmt.micrometer.NmtMeterBinder;
import org.framefork.nmt.micrometer.autoconfigure.NmtMicrometerAutoConfiguration;
import org.framefork.nmt.opentelemetry.NmtOpenTelemetryMetrics;
import org.framefork.nmt.opentelemetry.autoconfigure.NmtOpenTelemetryAutoConfiguration;
import org.framefork.nmt.testing.RequiresNativeMemoryTracking;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.opentelemetry.autoconfigure.OpenTelemetrySdkAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the NMT auto-configurations next to Spring Boot's own metrics and OpenTelemetry auto-configurations,
 * so the {@link MeterRegistry} and the {@link OpenTelemetry} come from Spring Boot, the way an application gets them.
 */
@RequiresNativeMemoryTracking
class NmtNextToSpringBootAutoConfigurationNmtOnTest
{

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(
            MetricsAutoConfiguration.class,
            SimpleMetricsExportAutoConfiguration.class,
            CompositeMeterRegistryAutoConfiguration.class,
            OpenTelemetrySdkAutoConfiguration.class,
            NmtCoreAutoConfiguration.class,
            NmtMicrometerAutoConfiguration.class,
            NmtOpenTelemetryAutoConfiguration.class
        ))
        .withUserConfiguration(InMemoryMetricReaderConfiguration.class);

    @Test
    void exportsNmtThroughSpringBootMeterRegistry()
    {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(MeterRegistry.class);
            assertThat(context).hasSingleBean(NmtMeterBinder.class);
            var javaHeapCommitted = context.getBean(MeterRegistry.class).get("jvm.memory.nmt.committed").tag("category", "java_heap").gauge();
            assertThat(javaHeapCommitted.value()).isPositive();
        });
    }

    @Test
    void exportsNmtThroughSpringBootOpenTelemetry()
    {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(OpenTelemetry.class);
            assertThat(context).hasSingleBean(NmtOpenTelemetryMetrics.class);
            var javaHeapCommitted = context.getBean(InMemoryMetricReader.class).collectAllMetrics().stream()
                .filter(metric -> metric.getName().equals("jvm.memory.nmt.committed"))
                .flatMap(metric -> metric.getLongGaugeData().getPoints().stream())
                .filter(point -> "java_heap".equals(point.getAttributes().get(AttributeKey.stringKey("category"))))
                .findFirst()
                .orElseThrow();
            assertThat(javaHeapCommitted.getValue()).isPositive();
        });
    }

    /**
     * Spring Boot builds its {@link OpenTelemetry} with the application's {@link SdkMeterProvider}, if there is one.
     */
    @Configuration(proxyBeanMethods = false)
    static class InMemoryMetricReaderConfiguration
    {

        @Bean
        InMemoryMetricReader inMemoryMetricReader()
        {
            return InMemoryMetricReader.create();
        }

        @Bean
        SdkMeterProvider sdkMeterProvider(InMemoryMetricReader inMemoryMetricReader)
        {
            return SdkMeterProvider.builder().registerMetricReader(inMemoryMetricReader).build();
        }

    }

}
