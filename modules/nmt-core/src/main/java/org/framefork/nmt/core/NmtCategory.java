package org.framefork.nmt.core;

/**
 * A single NMT memory category with its original label, normalized name, and memory values in bytes.
 *
 * @param label the original category label from jcmd output (e.g., "Java Heap", "GCCardSet")
 * @param normalizedName the normalized snake_case name (e.g., "java_heap", "gc_card_set")
 * @param reserved reserved memory in bytes (virtual address space)
 * @param committed committed memory in bytes (physical/swap memory)
 */
public record NmtCategory(
    String label,
    String normalizedName,
    long reserved,
    long committed
) {
}
