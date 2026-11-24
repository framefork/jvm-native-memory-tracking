package org.framefork.nmt.springboot;

import io.opentelemetry.api.OpenTelemetry;
import org.framefork.nmt.core.DiagnosticCommandRunner;
import org.framefork.nmt.core.NmtDataCollector;
import org.framefork.nmt.core.NmtSummary;
import org.framefork.nmt.core.autoconfigure.NmtCoreAutoConfiguration;
import org.framefork.nmt.opentelemetry.NmtOpenTelemetryMetrics;
import org.framefork.nmt.opentelemetry.autoconfigure.NmtOpenTelemetryAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NmtOpenTelemetryAutoConfigurationTest
{

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(NmtCoreAutoConfiguration.class, NmtOpenTelemetryAutoConfiguration.class))
        .withBean(OpenTelemetry.class, OpenTelemetry::noop);

    @Test
    void contextLoads()
    {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
        });
    }

    @Test
    void nmtOpenTelemetryMetricsNotCreatedWhenNmtDisabled()
    {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(NmtOpenTelemetryMetrics.class);
        });
    }

    @Test
    void nmtDataCollectorNotCreatedWhenNmtDisabled()
    {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(NmtDataCollector.class);
            assertThat(context).doesNotHaveBean(DiagnosticCommandRunner.class);
        });
    }

    @Test
    void nmtDataCollectorNotCreatedWhenNmtDisabledWhicheverRunnerIsSelected()
    {
        contextRunner.withPropertyValues("framefork.nmt.runner=jcmd").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(NmtDataCollector.class);
        });
    }

    @Test
    void applicationCollectorGetsMetricsWhenJvmNmtOff()
    {
        contextRunner.withBean(NmtDataCollector.class, () -> () -> new NmtSummary(Map.of())).run(context -> {
            assertThat(context).hasSingleBean(NmtOpenTelemetryMetrics.class);
        });
    }

    @Test
    void applicationCollectorGetsNoMetricsWhenSwitchedOff()
    {
        contextRunner.withBean(NmtDataCollector.class, () -> () -> new NmtSummary(Map.of())).withPropertyValues("framefork.nmt.enabled=false").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(NmtOpenTelemetryMetrics.class);
        });
    }

}
