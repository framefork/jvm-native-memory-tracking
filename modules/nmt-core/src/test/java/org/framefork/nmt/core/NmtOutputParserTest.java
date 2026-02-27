package org.framefork.nmt.core;

import org.framefork.nmt.testing.NmtTestResources;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NmtOutputParserTest
{

    private final NmtOutputParser parser = new NmtOutputParser();

    @Test
    void parsesSampleOutput_returnsNonEmptySummaryWithExpectedCategories()
    {
        var output = NmtTestResources.loadSample("nmt-summary-jdk17.txt");

        var summary = parser.parse(output);

        assertThat(summary.isEmpty()).isFalse();
        assertThat(summary.getCategories()).containsKey("total");
        assertThat(summary.getCategories()).containsKey("java_heap");
        assertThat(summary.getCategories()).containsKey("class");
        assertThat(summary.getCategories()).containsKey("thread");
        assertThat(summary.getCategories()).containsKey("gc");
        assertThat(summary.getCategories()).containsKey("code");
    }

    @Test
    void parsesSampleOutput_totalLineHasCorrectValues()
    {
        var output = NmtTestResources.loadSample("nmt-summary-jdk17.txt");

        var summary = parser.parse(output);
        var total = summary.getCategory("total");

        assertThat(total).isNotNull();
        assertThat(total.label()).isEqualTo("Total");
        assertThat(total.normalizedName()).isEqualTo("total");
        assertThat(total.reserved()).isEqualTo(18_669_619_469L);
        assertThat(total.committed()).isEqualTo(1_387_264_269L);
    }

    @Test
    void parsesSampleOutput_categoriesHaveCorrectValues()
    {
        var output = NmtTestResources.loadSample("nmt-summary-jdk17.txt");

        var summary = parser.parse(output);

        var javaHeap = summary.getCategory("java_heap");
        assertThat(javaHeap).isNotNull();
        assertThat(javaHeap.reserved()).isEqualTo(16_777_216_000L);
        assertThat(javaHeap.committed()).isEqualTo(1_174_405_120L);

        var classCategory = summary.getCategory("class");
        assertThat(classCategory).isNotNull();
        assertThat(classCategory.reserved()).isEqualTo(1_075_309_736L);
        assertThat(classCategory.committed()).isEqualTo(8_580_264L);

        var thread = summary.getCategory("thread");
        assertThat(thread).isNotNull();
        assertThat(thread.reserved()).isEqualTo(41_152_432L);
        assertThat(thread.committed()).isEqualTo(2_305_968L);

        var gc = summary.getCategory("gc");
        assertThat(gc).isNotNull();
        assertThat(gc.reserved()).isEqualTo(394_010_120L);
        assertThat(gc.committed()).isEqualTo(89_267_720L);

        var code = summary.getCategory("code");
        assertThat(code).isNotNull();
        assertThat(code.reserved()).isEqualTo(274_193_592L);
        assertThat(code.committed()).isEqualTo(36_883_640L);
    }

    @Test
    void parsesSampleOutput_allCategoriesParsed()
    {
        var output = NmtTestResources.loadSample("nmt-summary-jdk17.txt");

        var summary = parser.parse(output);

        // 24 categories + 1 total = 25 entries
        assertThat(summary.getCategories()).hasSize(25);
    }

    @Test
    void normalizeCategoryName_convertsExpectedLabels()
    {
        assertThat(NmtOutputParser.normalizeCategoryName("Java Heap")).isEqualTo("java_heap");
        assertThat(NmtOutputParser.normalizeCategoryName("GCCardSet")).isEqualTo("gccard_set");
        assertThat(NmtOutputParser.normalizeCategoryName("Native Memory Tracking")).isEqualTo("native_memory_tracking");
        assertThat(NmtOutputParser.normalizeCategoryName("Total")).isEqualTo("total");
        assertThat(NmtOutputParser.normalizeCategoryName("GC")).isEqualTo("gc");
        assertThat(NmtOutputParser.normalizeCategoryName("Shared class space")).isEqualTo("shared_class_space");
        assertThat(NmtOutputParser.normalizeCategoryName("Object Monitors")).isEqualTo("object_monitors");
        assertThat(NmtOutputParser.normalizeCategoryName("String Deduplication")).isEqualTo("string_deduplication");
    }

    // --- Minimal output ---

    @Test
    void parsesMinimalOutput_returnsNonEmptySummaryWithTotal()
    {
        var output = NmtTestResources.loadSample("nmt-summary-minimal.txt");

        var summary = parser.parse(output);

        assertThat(summary.isEmpty()).isFalse();
        assertThat(summary.getCategories()).containsKey("total");
    }

    @Test
    void parsesMinimalOutput_totalHasCorrectValues()
    {
        var output = NmtTestResources.loadSample("nmt-summary-minimal.txt");

        var summary = parser.parse(output);
        var total = summary.getCategory("total");

        assertThat(total).isNotNull();
        assertThat(total.reserved()).isEqualTo(536_879_104L);
        assertThat(total.committed()).isEqualTo(134_230_016L);
    }

    @Test
    void parsesMinimalOutput_hasFewCategoriesOnly()
    {
        var output = NmtTestResources.loadSample("nmt-summary-minimal.txt");

        var summary = parser.parse(output);

        // 3 categories + 1 total = 4 entries
        assertThat(summary.getCategories()).hasSize(4);
        assertThat(summary.getCategories()).containsKey("java_heap");
        assertThat(summary.getCategories()).containsKey("thread");
        assertThat(summary.getCategories()).containsKey("gc");
    }

    @Test
    void parsesMinimalOutput_categoriesHaveCorrectValues()
    {
        var output = NmtTestResources.loadSample("nmt-summary-minimal.txt");

        var summary = parser.parse(output);

        var javaHeap = summary.getCategory("java_heap");
        assertThat(javaHeap).isNotNull();
        assertThat(javaHeap.reserved()).isEqualTo(536_870_912L);
        assertThat(javaHeap.committed()).isEqualTo(134_217_728L);

        var thread = summary.getCategory("thread");
        assertThat(thread).isNotNull();
        assertThat(thread.reserved()).isEqualTo(4_104_192L);
        assertThat(thread.committed()).isEqualTo(4_104_192L);

        var gc = summary.getCategory("gc");
        assertThat(gc).isNotNull();
        assertThat(gc.reserved()).isEqualTo(4_096L);
        assertThat(gc.committed()).isEqualTo(4_096L);
    }

    // --- Future categories ---

    @Test
    void parsesFutureCategoriesOutput_containsAllOriginalCategories()
    {
        var output = NmtTestResources.loadSample("nmt-summary-future-categories.txt");

        var summary = parser.parse(output);

        assertThat(summary.isEmpty()).isFalse();
        assertThat(summary.getCategories()).containsKey("total");
        assertThat(summary.getCategories()).containsKey("java_heap");
        assertThat(summary.getCategories()).containsKey("class");
        assertThat(summary.getCategories()).containsKey("thread");
        assertThat(summary.getCategories()).containsKey("gc");
        assertThat(summary.getCategories()).containsKey("code");
    }

    @Test
    void parsesFutureCategoriesOutput_picksUpNewCategories()
    {
        var output = NmtTestResources.loadSample("nmt-summary-future-categories.txt");

        var summary = parser.parse(output);

        // Verify the new fictional categories are parsed
        var virtualThread = summary.getCategory("virtual_thread");
        assertThat(virtualThread).isNotNull();
        assertThat(virtualThread.label()).isEqualTo("Virtual Thread");
        assertThat(virtualThread.normalizedName()).isEqualTo("virtual_thread");
        assertThat(virtualThread.reserved()).isEqualTo(102_400L);
        assertThat(virtualThread.committed()).isEqualTo(51_200L);

        var fiberStack = summary.getCategory("fiber_stack");
        assertThat(fiberStack).isNotNull();
        assertThat(fiberStack.label()).isEqualTo("Fiber Stack");
        assertThat(fiberStack.normalizedName()).isEqualTo("fiber_stack");
        assertThat(fiberStack.reserved()).isEqualTo(81_920L);
        assertThat(fiberStack.committed()).isEqualTo(40_960L);

        var compactObjectHeaders = summary.getCategory("compact_object_headers");
        assertThat(compactObjectHeaders).isNotNull();
        assertThat(compactObjectHeaders.label()).isEqualTo("Compact Object Headers");
        assertThat(compactObjectHeaders.normalizedName()).isEqualTo("compact_object_headers");
        assertThat(compactObjectHeaders.reserved()).isEqualTo(15_680L);
        assertThat(compactObjectHeaders.committed()).isEqualTo(7_840L);
    }

    @Test
    void parsesFutureCategoriesOutput_allCategoriesParsed()
    {
        var output = NmtTestResources.loadSample("nmt-summary-future-categories.txt");

        var summary = parser.parse(output);

        // 24 original categories + 3 new categories + 1 total = 28 entries
        assertThat(summary.getCategories()).hasSize(28);
    }

    // --- Zero values ---

    @Test
    void parsesZeroValuesOutput_returnsNonEmptySummary()
    {
        var output = NmtTestResources.loadSample("nmt-summary-zero-values.txt");

        var summary = parser.parse(output);

        assertThat(summary.isEmpty()).isFalse();
        assertThat(summary.getCategories()).containsKey("total");
    }

    @Test
    void parsesZeroValuesOutput_zeroCategoriesArePresent()
    {
        var output = NmtTestResources.loadSample("nmt-summary-zero-values.txt");

        var summary = parser.parse(output);

        // Categories with reserved=0, committed=0 must still be present
        var code = summary.getCategory("code");
        assertThat(code).isNotNull();
        assertThat(code.reserved()).isEqualTo(0L);
        assertThat(code.committed()).isEqualTo(0L);

        var compiler = summary.getCategory("compiler");
        assertThat(compiler).isNotNull();
        assertThat(compiler.reserved()).isEqualTo(0L);
        assertThat(compiler.committed()).isEqualTo(0L);

        var tracing = summary.getCategory("tracing");
        assertThat(tracing).isNotNull();
        assertThat(tracing.reserved()).isEqualTo(0L);
        assertThat(tracing.committed()).isEqualTo(0L);

        var unknown = summary.getCategory("unknown");
        assertThat(unknown).isNotNull();
        assertThat(unknown.reserved()).isEqualTo(0L);
        assertThat(unknown.committed()).isEqualTo(0L);
    }

    @Test
    void parsesZeroValuesOutput_nonZeroCategoriesHaveCorrectValues()
    {
        var output = NmtTestResources.loadSample("nmt-summary-zero-values.txt");

        var summary = parser.parse(output);

        var total = summary.getCategory("total");
        assertThat(total).isNotNull();
        assertThat(total.reserved()).isEqualTo(537_919_488L);
        assertThat(total.committed()).isEqualTo(134_742_016L);

        var javaHeap = summary.getCategory("java_heap");
        assertThat(javaHeap).isNotNull();
        assertThat(javaHeap.reserved()).isEqualTo(536_870_912L);
        assertThat(javaHeap.committed()).isEqualTo(134_217_728L);
    }

    @Test
    void parsesZeroValuesOutput_allCategoriesParsed()
    {
        var output = NmtTestResources.loadSample("nmt-summary-zero-values.txt");

        var summary = parser.parse(output);

        // 12 categories + 1 total = 13 entries
        assertThat(summary.getCategories()).hasSize(13);
    }

    // --- NMT not enabled (from file) ---

    @Test
    void parse_nmtNotEnabledFromFile_returnsEmptySummary()
    {
        var output = NmtTestResources.loadSample("nmt-not-enabled.txt");

        var summary = parser.parse(output);

        assertThat(summary.isEmpty()).isTrue();
        assertThat(summary.getCategories()).isEmpty();
    }

    // --- NMT not enabled (hardcoded) ---

    @Test
    void parse_nmtNotEnabled_returnsEmptySummary()
    {
        var output = "Native memory tracking is not enabled";

        var summary = parser.parse(output);

        assertThat(summary.isEmpty()).isTrue();
        assertThat(summary.getCategories()).isEmpty();
    }

    @SuppressWarnings("NullAway")
    @Test
    void parse_nullInput_returnsEmptySummary()
    {
        var summary = parser.parse(null);

        assertThat(summary.isEmpty()).isTrue();
        assertThat(summary.getCategories()).isEmpty();
    }

    @Test
    void parse_blankInput_returnsEmptySummary()
    {
        var summary = parser.parse("   ");

        assertThat(summary.isEmpty()).isTrue();
        assertThat(summary.getCategories()).isEmpty();
    }

}
