package org.framefork.nmt.core;

import org.framefork.nmt.testing.CannedDiagnosticCommandRunner;
import org.framefork.nmt.testing.NmtTestResources;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DiagnosticCommandNmtDataCollectorTest
{

    @Test
    void cannedOutput_isParsedIntoCategories()
    {
        var collector = new DiagnosticCommandNmtDataCollector(new CannedDiagnosticCommandRunner(NmtTestResources.loadSample("nmt-summary-jdk17.txt")));

        assertThat(collector.collect().getCategories()).containsKeys("total", "java_heap");
    }

    @Test
    void runnerThrowsDiagnosticCommandException_returnsEmptySummary()
    {
        DiagnosticCommandRunner runner = arguments -> {
            throw new DiagnosticCommandException("jcmd exited with code 143");
        };

        assertThat(new DiagnosticCommandNmtDataCollector(runner).collect().isEmpty()).isTrue();
    }

    @Test
    void customRunnerThrowsOtherRuntimeException_returnsEmptySummary()
    {
        DiagnosticCommandRunner runner = arguments -> {
            throw new IllegalStateException("custom runner failed");
        };

        assertThat(new DiagnosticCommandNmtDataCollector(runner).collect().isEmpty()).isTrue();
    }

}
