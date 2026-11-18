package org.framefork.nmt.core;

import org.framefork.nmt.testing.RequiresNativeMemoryTracking;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs both runners against the test JVM itself, which the {@code nmtTest} task starts with Native Memory Tracking on.
 */
@RequiresNativeMemoryTracking
class NmtRunnersNmtOnTest
{

    private static final String[] NATIVE_MEMORY_SUMMARY = {"VM.native_memory", "summary", "scale=b"};

    @BeforeAll
    static void testJvmHasNmtOn()
    {
        assertThat(NmtAvailability.getNmtMode()).isEqualTo(NmtMode.SUMMARY);
        assertThat(NmtAvailability.isDiagnosticCommandAvailable()).isTrue();
        assertThat(NmtAvailability.isJcmdAvailable()).isTrue();
    }

    @Test
    void mbeanRunner_returnsNativeMemorySummary()
    {
        var output = new DiagnosticCommandMBeanRunner().run(NATIVE_MEMORY_SUMMARY);

        assertThat(output)
            .doesNotStartWith(ProcessHandle.current().pid() + ":")
            .contains("Native Memory Tracking:")
            .containsPattern("Total: reserved=\\d+, committed=\\d+")
            .containsPattern("-\\s+Java Heap \\(reserved=\\d+, committed=\\d+\\)");
    }

    @Test
    void jcmdRunner_returnsNativeMemorySummary()
    {
        var output = new JcmdProcessRunner().run(NATIVE_MEMORY_SUMMARY);

        assertThat(output)
            .startsWith(ProcessHandle.current().pid() + ":")
            .containsPattern("Total: reserved=\\d+, committed=\\d+")
            .containsPattern("-\\s+Java Heap \\(reserved=\\d+, committed=\\d+\\)");
    }

    @Test
    void bothRunners_areParsedIntoTheSameCategories()
    {
        var fromMBean = new DiagnosticCommandNmtDataCollector(new DiagnosticCommandMBeanRunner()).collect();
        var fromJcmd = new DiagnosticCommandNmtDataCollector(new JcmdProcessRunner()).collect();

        // categories that are always non-zero; others are printed only when non-zero and can come and go between calls
        assertThat(fromMBean.getCategories()).containsKeys("total", "java_heap", "class", "thread", "code", "gc", "compiler", "internal", "symbol", "native_memory_tracking");
        assertThat(fromJcmd.getCategories()).containsKeys("total", "java_heap", "class", "thread", "code", "gc", "compiler", "internal", "symbol", "native_memory_tracking");

        var heapFromMBean = fromMBean.getCategory("java_heap");
        var heapFromJcmd = fromJcmd.getCategory("java_heap");
        assertThat(heapFromMBean).isNotNull();
        assertThat(heapFromJcmd).isNotNull();
        assertThat(heapFromMBean.reserved()).isPositive().isEqualTo(heapFromJcmd.reserved());
        assertThat(heapFromMBean.committed()).isPositive();
    }

    @Test
    void mbeanRunner_detailWithSummaryTracking_reportsItInTheOutput()
    {
        assertThat(new DiagnosticCommandMBeanRunner().run("VM.native_memory", "detail")).contains("Detail tracking is not enabled");
    }

    @Test
    void mbeanRunner_invalidArgument_throwsDiagnosticCommandException()
    {
        assertThatThrownBy(() -> new DiagnosticCommandMBeanRunner().run("VM.native_memory", "bogus"))
            .isInstanceOf(DiagnosticCommandException.class)
            .hasMessageContaining("vmNativeMemory failed");
    }

}
