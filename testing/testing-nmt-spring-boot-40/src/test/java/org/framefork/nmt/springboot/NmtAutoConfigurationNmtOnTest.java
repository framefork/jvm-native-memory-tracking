package org.framefork.nmt.springboot;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import org.framefork.nmt.core.JcmdProcessRunner;
import org.framefork.nmt.core.DiagnosticCommandMBeanRunner;
import org.framefork.nmt.core.DiagnosticCommandRunner;
import org.framefork.nmt.core.NmtAvailability;
import org.framefork.nmt.core.NmtDataCollector;
import org.framefork.nmt.core.NmtMode;
import org.framefork.nmt.core.NmtSummary;
import org.framefork.nmt.core.autoconfigure.NmtCoreAutoConfiguration;
import org.framefork.nmt.core.autoconfigure.NmtProperties;
import org.framefork.nmt.micrometer.NmtMeterBinder;
import org.framefork.nmt.micrometer.autoconfigure.NmtMicrometerAutoConfiguration;
import org.framefork.nmt.opentelemetry.NmtOpenTelemetryMetrics;
import org.framefork.nmt.opentelemetry.autoconfigure.NmtOpenTelemetryAutoConfiguration;
import org.framefork.nmt.testing.CannedDiagnosticCommandRunner;
import org.framefork.nmt.testing.NmtTestResources;
import org.framefork.nmt.testing.RequiresNativeMemoryTracking;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the auto-configurations in the test JVM itself, which the {@code nmtTest} task starts with Native Memory Tracking on.
 */
@RequiresNativeMemoryTracking
class NmtAutoConfigurationNmtOnTest
{

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(NmtCoreAutoConfiguration.class, NmtMicrometerAutoConfiguration.class, NmtOpenTelemetryAutoConfiguration.class))
        .withBean(MeterRegistry.class, SimpleMeterRegistry::new);

    @BeforeAll
    static void testJvmHasNmtOn()
    {
        assertThat(NmtAvailability.getNmtMode()).isEqualTo(NmtMode.SUMMARY);
    }

    @Test
    void readsNmtThroughTheMBeanByDefault()
    {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).getBean(DiagnosticCommandRunner.class).isInstanceOf(DiagnosticCommandMBeanRunner.class);
            assertThat(context).hasSingleBean(NmtDataCollector.class);
            assertThat(javaHeapCommitted(context)).isPositive();
        });
    }

    @Test
    void readsNmtThroughTheMBeanWhenSelected()
    {
        contextRunner.withPropertyValues("framefork.nmt.runner=mbean").run(context -> {
            assertThat(context).getBean(DiagnosticCommandRunner.class).isInstanceOf(DiagnosticCommandMBeanRunner.class);
            assertThat(javaHeapCommitted(context)).isPositive();
        });
    }

    @Test
    void readsNmtThroughJcmdWhenSelected()
    {
        contextRunner.withPropertyValues("framefork.nmt.runner=jcmd").run(context -> {
            assertThat(context).getBean(DiagnosticCommandRunner.class).isInstanceOf(JcmdProcessRunner.class);
            assertThat(javaHeapCommitted(context)).isPositive();
        });
    }

    @Test
    void applicationRunnerReplacesTheBuiltInOne()
    {
        var cannedRunner = new CannedDiagnosticCommandRunner(NmtTestResources.loadSample("nmt-summary-jdk17.txt"));
        contextRunner.withBean(DiagnosticCommandRunner.class, () -> cannedRunner).run(context -> {
            assertThat(context).getBean(DiagnosticCommandRunner.class).isSameAs(cannedRunner);
            assertThat(javaHeapCommitted(context)).isEqualTo(1_174_405_120.0);
        });
    }

    @Test
    void applicationMeterBinderReplacesTheBuiltInOne()
    {
        contextRunner.withBean("customNmtMeterBinder", NmtMeterBinder.class, () -> new NmtMeterBinder(() -> new NmtSummary(Map.of())))
            .run(context -> {
                assertThat(context).hasSingleBean(NmtMeterBinder.class);
                assertThat(context).hasBean("customNmtMeterBinder");
            });
    }

    @Test
    void exportsNmtThroughOpenTelemetry()
    {
        var reader = InMemoryMetricReader.create();
        var openTelemetry = OpenTelemetrySdk.builder()
            .setMeterProvider(SdkMeterProvider.builder().registerMetricReader(reader).build())
            .build();

        contextRunner.withBean(OpenTelemetry.class, () -> openTelemetry).run(context -> {
            assertThat(context).hasSingleBean(NmtOpenTelemetryMetrics.class);

            var committed = reader.collectAllMetrics().stream()
                .filter(metric -> metric.getName().equals("jvm.memory.nmt.committed"))
                .map(MetricData::getLongGaugeData)
                .flatMap(gauge -> gauge.getPoints().stream())
                .filter(point -> "java_heap".equals(point.getAttributes().get(AttributeKey.stringKey("category"))))
                .findFirst()
                .orElseThrow();
            assertThat(committed.getValue()).isPositive();
        });
    }

    @Test
    void registersNothingWhenDisabled()
    {
        contextRunner.withBean(OpenTelemetry.class, OpenTelemetry::noop).withPropertyValues("framefork.nmt.enabled=false").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(NmtProperties.class);
            assertThat(context).doesNotHaveBean(DiagnosticCommandRunner.class);
            assertThat(context).doesNotHaveBean(NmtDataCollector.class);
            assertThat(context).doesNotHaveBean(NmtMeterBinder.class);
            assertThat(context).doesNotHaveBean(NmtOpenTelemetryMetrics.class);
        });
    }

    /**
     * Binds the meters the way Spring Boot's metrics auto-configuration does, which the context runner does not include.
     */
    private static double javaHeapCommitted(ApplicationContext context)
    {
        var registry = context.getBean(MeterRegistry.class);
        context.getBean(NmtMeterBinder.class).bindTo(registry);
        return registry.get("jvm.memory.nmt.committed").tag("category", "java_heap").gauge().value();
    }

}
