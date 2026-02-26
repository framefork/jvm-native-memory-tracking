package org.framefork.nmt.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Parses the text output of {@code jcmd <pid> VM.native_memory summary scale=b}
 * into a {@link NativeMemoryTrackingSummary}.
 *
 * <p>The parser dynamically extracts whatever category names jcmd reports,
 * making it forward-compatible with new JDK versions that may add categories.</p>
 */
public final class NmtOutputParser {

    private static final Logger log = LoggerFactory.getLogger(NmtOutputParser.class);

    private static final String NMT_NOT_ENABLED = "Native memory tracking is not enabled";

    /**
     * Matches the total line: {@code Total: reserved=1607492304, committed=196489936}
     */
    private static final Pattern TOTAL_PATTERN = Pattern.compile(
        "Total:\\s+reserved=(?<reserved>\\d+),\\s+committed=(?<committed>\\d+)",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Matches per-category lines like: {@code -  Java Heap (reserved=104857600, committed=104857600)}
     */
    private static final Pattern CATEGORY_PATTERN = Pattern.compile(
        "-\\s+(?<category>.+?)\\s+\\(reserved=(?<reserved>\\d+),\\s+committed=(?<committed>\\d+)",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Parses jcmd NMT output into a structured summary.
     *
     * @param output the raw text output from {@code jcmd VM.native_memory summary scale=b}
     * @return parsed summary, empty if NMT is not enabled or output is empty
     */
    public NativeMemoryTrackingSummary parse(String output) {
        if (output == null || output.isBlank()) {
            return new NativeMemoryTrackingSummary(new LinkedHashMap<>());
        }

        if (output.contains(NMT_NOT_ENABLED)) {
            log.warn("Native memory tracking is not enabled on this JVM");
            return new NativeMemoryTrackingSummary(new LinkedHashMap<>());
        }

        var categories = new LinkedHashMap<String, NativeMemoryCategory>();

        // Parse total line
        var totalMatcher = TOTAL_PATTERN.matcher(output);
        if (totalMatcher.find()) {
            var reserved = Long.parseLong(totalMatcher.group("reserved"));
            var committed = Long.parseLong(totalMatcher.group("committed"));
            var normalized = normalizeCategoryName("Total");
            categories.put(normalized, new NativeMemoryCategory("Total", normalized, reserved, committed));
        }

        // Parse per-category lines
        var categoryMatcher = CATEGORY_PATTERN.matcher(output);
        while (categoryMatcher.find()) {
            var label = categoryMatcher.group("category").trim();
            var reserved = Long.parseLong(categoryMatcher.group("reserved"));
            var committed = Long.parseLong(categoryMatcher.group("committed"));
            var normalized = normalizeCategoryName(label);

            categories.put(normalized, new NativeMemoryCategory(label, normalized, reserved, committed));
        }

        return new NativeMemoryTrackingSummary(categories);
    }

    /**
     * Normalizes a category label to snake_case.
     * Examples: "Java Heap" -> "java_heap", "GCCardSet" -> "gc_card_set",
     * "Native Memory Tracking" -> "native_memory_tracking"
     */
    static String normalizeCategoryName(String label) {
        // Insert underscore before uppercase letters that follow lowercase letters (camelCase boundaries)
        var result = label.replaceAll("([a-z])([A-Z])", "$1_$2");
        // Replace spaces with underscores
        result = result.replace(' ', '_');
        // Convert to lowercase
        result = result.toLowerCase(Locale.ROOT);
        // Collapse multiple underscores
        result = result.replaceAll("_+", "_");
        // Trim leading/trailing underscores
        result = result.replaceAll("^_|_$", "");
        return result;
    }

}
