package org.framefork.nmt.springboot;

import io.opentelemetry.api.OpenTelemetry;
import org.framefork.nmt.core.NmtDataCollector;
import org.framefork.nmt.opentelemetry.NmtOpenTelemetryMetrics;
import org.framefork.nmt.opentelemetry.autoconfigure.NmtOpenTelemetryAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class NmtOpenTelemetryAutoConfigurationTest
{

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(NmtOpenTelemetryAutoConfiguration.class))
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
        });
    }

}
