package org.framefork.nmt.opentelemetry.autoconfigure;

import io.opentelemetry.api.OpenTelemetry;
import org.framefork.nmt.core.CachingNmtDataCollector;
import org.framefork.nmt.core.DefaultJcmdRunner;
import org.framefork.nmt.core.JcmdNmtDataCollector;
import org.framefork.nmt.core.NmtAvailability;
import org.framefork.nmt.core.NmtDataCollector;
import org.framefork.nmt.opentelemetry.NmtOpenTelemetryMetrics;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Auto-configuration for JVM Native Memory Tracking metrics with OpenTelemetry.
 *
 * <p>Activates when:</p>
 * <ul>
 *   <li>OpenTelemetry's {@link OpenTelemetry} is on the classpath and available as a bean</li>
 *   <li>JVM was started with {@code -XX:NativeMemoryTracking=summary} (or detail)</li>
 *   <li>The {@code jcmd} executable is available</li>
 * </ul>
 */
@AutoConfiguration
@ConditionalOnClass(OpenTelemetry.class)
@ConditionalOnBean(OpenTelemetry.class)
@Conditional(NmtOpenTelemetryAutoConfiguration.NmtAvailableCondition.class)
public class NmtOpenTelemetryAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(NmtDataCollector.class)
    public NmtDataCollector nmtDataCollector() {
        return new CachingNmtDataCollector(new JcmdNmtDataCollector(new DefaultJcmdRunner()));
    }

    @Bean(destroyMethod = "close")
    public NmtOpenTelemetryMetrics nmtOpenTelemetryMetrics(OpenTelemetry openTelemetry, NmtDataCollector collector) {
        return new NmtOpenTelemetryMetrics(openTelemetry, collector);
    }

    static class NmtAvailableCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return NmtAvailability.isNmtEnabled() && NmtAvailability.isJcmdAvailable();
        }
    }

}
