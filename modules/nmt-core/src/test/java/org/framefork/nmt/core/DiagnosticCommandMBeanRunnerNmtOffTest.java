package org.framefork.nmt.core;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs against the test JVM itself, which the {@code test} task starts with Native Memory Tracking off.
 */
class DiagnosticCommandMBeanRunnerNmtOffTest
{

    private final DiagnosticCommandMBeanRunner runner = new DiagnosticCommandMBeanRunner();

    @BeforeAll
    static void testJvmHasNmtOff()
    {
        assertThat(NmtAvailability.getNmtMode()).isEqualTo(NmtMode.OFF);
    }

    @Test
    void mbeanIsRegisteredEvenWithNmtOff()
    {
        assertThat(DiagnosticCommandMBeanRunner.isAvailable()).isTrue();
        assertThat(NmtAvailability.isDiagnosticCommandAvailable()).isTrue();
    }

    @Test
    void nativeMemorySummary_reportsNmtNotEnabled_andCollectorReturnsEmptySummary()
    {
        assertThat(runner.run("VM.native_memory", "summary", "scale=b")).contains("Native memory tracking is not enabled");
        assertThat(new DiagnosticCommandNmtDataCollector(runner).collect().isEmpty()).isTrue();
    }

    @Test
    void otherDiagnosticCommandsAreResolvedFromTheMBeanDescriptors()
    {
        // VM.version maps to vmVersion, Compiler.CodeHeap_Analytics keeps the case after the separator
        assertThat(runner.run("VM.version")).contains(System.getProperty("java.vm.version"));
        assertThat(runner.run("Compiler.CodeHeap_Analytics")).isNotBlank();
    }

    @Test
    void backToBackCalls_runOneAfterAnother()
    {
        // the next call must not be rejected while the worker thread is still finishing the previous one
        for (int i = 0; i < 2_000; i++) {
            assertThat(runner.run("VM.version")).isNotBlank();
        }
    }

    @Test
    void unknownCommand_throwsDiagnosticCommandException()
    {
        assertThatThrownBy(() -> runner.run("VM.does_not_exist"))
            .isInstanceOf(DiagnosticCommandException.class)
            .hasMessageContaining("VM.does_not_exist is not available");
    }

    @Test
    void invalidArgument_throwsDiagnosticCommandException()
    {
        // the MBean server reports it as RuntimeMBeanException, which is not a JMException
        assertThatThrownBy(() -> runner.run("VM.version", "bogus"))
            .isInstanceOf(DiagnosticCommandException.class)
            .hasMessageContaining("vmVersion failed");
    }

    @Test
    void noCommand_throwsDiagnosticCommandException()
    {
        assertThatThrownBy(runner::run)
            .isInstanceOf(DiagnosticCommandException.class)
            .hasMessageContaining("No diagnostic command given");
    }

}
