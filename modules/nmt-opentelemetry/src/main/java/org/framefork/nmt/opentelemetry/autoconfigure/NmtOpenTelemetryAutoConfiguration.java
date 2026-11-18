package org.framefork.nmt.opentelemetry.autoconfigure;

import io.opentelemetry.api.OpenTelemetry;
import org.framefork.nmt.core.NmtDataCollector;
import org.framefork.nmt.core.autoconfigure.NmtCoreAutoConfiguration;
import org.framefork.nmt.opentelemetry.NmtOpenTelemetryMetrics;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for JVM Native Memory Tracking metrics with OpenTelemetry.
 *
 * <p>Activates when:</p>
 * <ul>
 *   <li>OpenTelemetry's {@link OpenTelemetry} is on the classpath and available as a bean</li>
 *   <li>An {@link NmtDataCollector} bean exists, which {@link NmtCoreAutoConfiguration} creates when
 *   Native Memory Tracking is enabled and NMT data can be read</li>
 * </ul>
 */
@AutoConfiguration(after = NmtCoreAutoConfiguration.class)
@ConditionalOnClass(OpenTelemetry.class)
@ConditionalOnBean({OpenTelemetry.class, NmtDataCollector.class})
public class NmtOpenTelemetryAutoConfiguration
{

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(NmtOpenTelemetryMetrics.class)
    public NmtOpenTelemetryMetrics nmtOpenTelemetryMetrics(OpenTelemetry openTelemetry, NmtDataCollector collector)
    {
        return new NmtOpenTelemetryMetrics(openTelemetry, collector);
    }

}
