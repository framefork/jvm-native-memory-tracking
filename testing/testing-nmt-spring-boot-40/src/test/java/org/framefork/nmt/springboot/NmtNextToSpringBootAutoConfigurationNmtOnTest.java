package org.framefork.nmt.springboot;

import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
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
    void springBootMeterRegistryGetsNoNmtMetrics()
    {
        // The NMT auto-configuration runs before Spring Boot's, so its @ConditionalOnBean does not see the MeterRegistry yet.
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(MeterRegistry.class);
            assertThat(context).doesNotHaveBean(NmtMeterBinder.class);
            assertThat(context.getBean(MeterRegistry.class).find("jvm.memory.nmt.committed").gauges()).isEmpty();
        });
    }

    @Test
    void springBootOpenTelemetryGetsNoNmtMetrics()
    {
        // The NMT auto-configuration runs before Spring Boot's, so its @ConditionalOnBean does not see the OpenTelemetry yet.
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(OpenTelemetry.class);
            assertThat(context).doesNotHaveBean(NmtOpenTelemetryMetrics.class);
            assertThat(context.getBean(InMemoryMetricReader.class).collectAllMetrics())
                .noneMatch(metric -> metric.getName().startsWith("jvm.memory.nmt."));
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
