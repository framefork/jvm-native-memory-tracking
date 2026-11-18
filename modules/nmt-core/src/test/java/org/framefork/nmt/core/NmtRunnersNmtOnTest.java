package org.framefork.nmt.core;

import org.framefork.nmt.testing.RequiresNativeMemoryTracking;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the runner against the test JVM itself, which the {@code nmtTest} task starts with Native Memory Tracking on.
 */
@RequiresNativeMemoryTracking
class NmtRunnersNmtOnTest
{

    private static final String[] NATIVE_MEMORY_SUMMARY = {"VM.native_memory", "summary", "scale=b"};

    @BeforeAll
    static void testJvmHasNmtOn()
    {
        assertThat(NmtAvailability.getNmtMode()).isEqualTo(NmtMode.SUMMARY);
        assertThat(NmtAvailability.isJcmdAvailable()).isTrue();
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
    void jcmdRunner_isParsedIntoCategories()
    {
        var summary = new DiagnosticCommandNmtDataCollector(new JcmdProcessRunner()).collect();

        // categories that are always non-zero; others are printed only when non-zero and can come and go between calls
        assertThat(summary.getCategories()).containsKeys("total", "java_heap", "class", "thread", "code", "gc", "compiler", "internal", "symbol", "native_memory_tracking");
        var heap = summary.getCategory("java_heap");
        assertThat(heap).isNotNull();
        assertThat(heap.committed()).isPositive();
    }

}
