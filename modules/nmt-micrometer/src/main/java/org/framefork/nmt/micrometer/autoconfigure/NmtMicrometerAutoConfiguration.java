package org.framefork.nmt.micrometer.autoconfigure;

import io.micrometer.core.instrument.MeterRegistry;
import org.framefork.nmt.core.NmtDataCollector;
import org.framefork.nmt.core.autoconfigure.NmtCoreAutoConfiguration;
import org.framefork.nmt.micrometer.NmtMeterBinder;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for JVM Native Memory Tracking metrics with Micrometer.
 *
 * <p>Activates when:</p>
 * <ul>
 *   <li>Micrometer's {@link MeterRegistry} is on the classpath and available as a bean</li>
 *   <li>An {@link NmtDataCollector} bean exists, which {@link NmtCoreAutoConfiguration} creates when
 *   Native Memory Tracking is enabled and NMT data can be read</li>
 * </ul>
 */
@AutoConfiguration(after = NmtCoreAutoConfiguration.class)
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
