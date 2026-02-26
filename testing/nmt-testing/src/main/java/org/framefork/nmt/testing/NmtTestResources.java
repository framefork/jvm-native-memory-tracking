package org.framefork.nmt.testing;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Provides access to sample NMT output files for testing.
 */
public final class NmtTestResources {

    private NmtTestResources() {
    }

    /**
     * Loads a sample NMT output file from the classpath.
     *
     * @param resourceName the resource name (e.g., "nmt-summary-jdk17.txt")
     * @return the file content as a string
     */
    public static String loadSample(String resourceName) {
        try (var stream = NmtTestResources.class.getResourceAsStream("/nmt-samples/" + resourceName)) {
            Objects.requireNonNull(stream, "Sample resource not found: " + resourceName);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load sample resource: " + resourceName, e);
        }
    }

}
