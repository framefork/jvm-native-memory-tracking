package org.framefork.nmt.core.autoconfigure;

import org.framefork.nmt.core.CachingNmtDataCollector;
import org.framefork.nmt.core.JcmdProcessRunner;
import org.framefork.nmt.core.DiagnosticCommandMBeanRunner;
import org.framefork.nmt.core.DiagnosticCommandNmtDataCollector;
import org.framefork.nmt.core.DiagnosticCommandRunner;
import org.framefork.nmt.core.NmtAvailability;
import org.framefork.nmt.core.NmtDataCollector;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Auto-configuration of the {@link NmtDataCollector} that the Micrometer and OpenTelemetry auto-configurations export.
 *
 * <p>Activates when:</p>
 * <ul>
 *   <li>{@code framefork.nmt.enabled} is not {@code false}</li>
 *   <li>JVM was started with {@code -XX:NativeMemoryTracking=summary} (or detail)</li>
 *   <li>the runner selected by {@code framefork.nmt.runner} is available: the DiagnosticCommand MBean for {@code mbean},
 *   the {@code jcmd} executable for {@code jcmd}, either of them for {@code auto}</li>
 * </ul>
 *
 * <p>Both the {@link DiagnosticCommandRunner} and the {@link NmtDataCollector} back off when the application defines its own bean.</p>
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = NmtProperties.PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
@Conditional(NmtCoreAutoConfiguration.NmtAvailableCondition.class)
@EnableConfigurationProperties(NmtProperties.class)
public class NmtCoreAutoConfiguration
{

    private static final Logger log = LoggerFactory.getLogger(NmtCoreAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean(DiagnosticCommandRunner.class)
    public DiagnosticCommandRunner nmtDiagnosticCommandRunner(NmtProperties properties)
    {
        var runner = resolveRunner(properties.getRunner());
        if (runner == null) {
            // the condition has already checked the availability, this only happens when it changed in between
            throw new IllegalStateException("No NMT runner is available for framefork.nmt.runner=" + properties.getRunner());
        }
        log.debug("Reading NMT data through the {} runner", runner);
        return switch (runner) {
            case JCMD -> new JcmdProcessRunner();
            case MBEAN, AUTO -> new DiagnosticCommandMBeanRunner();
        };
    }

    @Bean
    @ConditionalOnMissingBean(NmtDataCollector.class)
    public NmtDataCollector nmtDataCollector(DiagnosticCommandRunner nmtDiagnosticCommandRunner, NmtProperties properties)
    {
        return new CachingNmtDataCollector(new DiagnosticCommandNmtDataCollector(nmtDiagnosticCommandRunner), properties.getCacheTtl());
    }

    /**
     * Returns the runner to use for the requested one, {@link NmtProperties.Runner#MBEAN} or {@link NmtProperties.Runner#JCMD},
     * or {@code null} if it is not available.
     */
    static NmtProperties.@Nullable Runner resolveRunner(NmtProperties.Runner requested)
    {
        return switch (requested) {
            case MBEAN -> NmtAvailability.isDiagnosticCommandAvailable() ? NmtProperties.Runner.MBEAN : null;
            case JCMD -> NmtAvailability.isJcmdAvailable() ? NmtProperties.Runner.JCMD : null;
            case AUTO -> {
                if (NmtAvailability.isDiagnosticCommandAvailable()) {
                    yield NmtProperties.Runner.MBEAN;
                }
                yield NmtAvailability.isJcmdAvailable() ? NmtProperties.Runner.JCMD : null;
            }
        };
    }

    static class NmtAvailableCondition extends SpringBootCondition
    {

        @Override
        public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata)
        {
            if (!NmtAvailability.isNmtEnabled()) {
                return ConditionOutcome.noMatch("Native Memory Tracking is not enabled (-XX:NativeMemoryTracking)");
            }

            var requested = Binder.get(context.getEnvironment())
                .bind(NmtProperties.PREFIX + ".runner", NmtProperties.Runner.class)
                .orElse(NmtProperties.Runner.AUTO);

            var runner = resolveRunner(requested);
            if (runner == null) {
                var message = switch (requested) {
                    case MBEAN -> "the DiagnosticCommand MBean is not registered (is the jdk.management module missing?)";
                    case JCMD -> "the jcmd executable was not found in $JAVA_HOME/bin or $PATH (is the jdk.jcmd module missing?)";
                    case AUTO -> "neither the DiagnosticCommand MBean nor the jcmd executable is available";
                };
                log.warn("NMT metrics are disabled although Native Memory Tracking is enabled: {}", message);
                return ConditionOutcome.noMatch("No NMT runner for framefork.nmt.runner=" + requested + ": " + message);
            }

            return ConditionOutcome.match("Native Memory Tracking is enabled, reading it through the " + runner + " runner");
        }

    }

}
