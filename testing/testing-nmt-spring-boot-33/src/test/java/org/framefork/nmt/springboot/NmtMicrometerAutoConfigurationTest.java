package org.framefork.nmt.springboot;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.framefork.nmt.core.NmtDataCollector;
import org.framefork.nmt.micrometer.NmtMeterBinder;
import org.framefork.nmt.micrometer.autoconfigure.NmtMicrometerAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class NmtMicrometerAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(NmtMicrometerAutoConfiguration.class))
        .withBean(MeterRegistry.class, SimpleMeterRegistry::new);

    @Test
    void contextLoads() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
        });
    }

    @Test
    void nmtMeterBinderNotCreatedWhenNmtDisabled() {
        // NMT is not enabled in the test JVM, so the condition should not match
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(NmtMeterBinder.class);
        });
    }

    @Test
    void nmtDataCollectorNotCreatedWhenNmtDisabled() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(NmtDataCollector.class);
        });
    }
}
