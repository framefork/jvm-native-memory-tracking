package org.framefork.nmt.micrometer.autoconfigure;

import io.micrometer.core.instrument.MeterRegistry;
import org.framefork.nmt.core.CachingNmtDataCollector;
import org.framefork.nmt.core.DefaultJcmdRunner;
import org.framefork.nmt.core.JcmdNmtDataCollector;
import org.framefork.nmt.core.NmtAvailability;
import org.framefork.nmt.core.NmtDataCollector;
import org.framefork.nmt.micrometer.NmtMeterBinder;
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
 * Auto-configuration for JVM Native Memory Tracking metrics with Micrometer.
 *
 * <p>Activates when:</p>
 * <ul>
 *   <li>Micrometer's {@link MeterRegistry} is on the classpath and available as a bean</li>
 *   <li>JVM was started with {@code -XX:NativeMemoryTracking=summary} (or detail)</li>
 *   <li>The {@code jcmd} executable is available</li>
 * </ul>
 */
@AutoConfiguration
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnBean(MeterRegistry.class)
@Conditional(NmtMicrometerAutoConfiguration.NmtAvailableCondition.class)
public class NmtMicrometerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(NmtDataCollector.class)
    public NmtDataCollector nmtDataCollector() {
        return new CachingNmtDataCollector(new JcmdNmtDataCollector(new DefaultJcmdRunner()));
    }

    @Bean
    public NmtMeterBinder nmtMeterBinder(NmtDataCollector collector) {
        return new NmtMeterBinder(collector);
    }

    static class NmtAvailableCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return NmtAvailability.isNmtEnabled() && NmtAvailability.isJcmdAvailable();
        }
    }

}
