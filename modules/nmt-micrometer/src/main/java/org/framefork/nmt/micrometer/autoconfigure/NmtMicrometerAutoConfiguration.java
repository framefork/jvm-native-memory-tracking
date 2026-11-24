package org.framefork.nmt.micrometer.autoconfigure;

import io.micrometer.core.instrument.MeterRegistry;
import org.framefork.nmt.core.NmtDataCollector;
import org.framefork.nmt.core.autoconfigure.NmtCoreAutoConfiguration;
import org.framefork.nmt.core.autoconfigure.NmtProperties;
import org.framefork.nmt.micrometer.NmtMeterBinder;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for JVM Native Memory Tracking metrics with Micrometer.
 *
 * <p>Activates when:</p>
 * <ul>
 *   <li>{@code framefork.nmt.enabled} is not {@code false}</li>
 *   <li>Micrometer's {@link MeterRegistry} is on the classpath and available as a bean</li>
 *   <li>An {@link NmtDataCollector} bean exists, which {@link NmtCoreAutoConfiguration} creates when
 *   Native Memory Tracking is enabled and NMT data can be read</li>
 * </ul>
 *
 * <p>It runs after Spring Boot's metrics auto-configurations, so that {@code @ConditionalOnBean} sees the {@link MeterRegistry} they create.
 * Spring Boot 4 moved them to another package, so both names are listed. A name that is not on the classpath is ignored.</p>
 */
@AutoConfiguration(
    after = NmtCoreAutoConfiguration.class,
    afterName = {
        // Spring Boot 3.x
        "org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration",
        "org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration",
        // Spring Boot 4.0
        "org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration",
        "org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration",
    }
)
@ConditionalOnProperty(prefix = NmtProperties.PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnBean({MeterRegistry.class, NmtDataCollector.class})
public class NmtMicrometerAutoConfiguration
{

    @Bean
    @ConditionalOnMissingBean(NmtMeterBinder.class)
    public NmtMeterBinder nmtMeterBinder(NmtDataCollector collector)
    {
        return new NmtMeterBinder(collector);
    }

}
