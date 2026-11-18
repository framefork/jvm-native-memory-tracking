package org.framefork.nmt.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Collects NMT data by running the {@code VM.native_memory summary scale=b} diagnostic command
 * through a {@link DiagnosticCommandRunner} and parsing the output.
 *
 * <p>{@link #collect()} never throws: when the runner or the parser fails, it logs the error and returns an empty summary.</p>
 */
public final class DiagnosticCommandNmtDataCollector implements NmtDataCollector
{

    private static final Logger log = LoggerFactory.getLogger(DiagnosticCommandNmtDataCollector.class);

    private final DiagnosticCommandRunner runner;
    private final NmtOutputParser parser;

    public DiagnosticCommandNmtDataCollector(DiagnosticCommandRunner runner)
    {
        this.runner = runner;
        this.parser = new NmtOutputParser();
    }

    @Override
    public NmtSummary collect()
    {
        try {
            var output = runner.run("VM.native_memory", "summary", "scale=b");
            return parser.parse(output);
        } catch (RuntimeException e) {
            // a custom DiagnosticCommandRunner or a parser bug can throw anything; the metrics callback must get empty values, not an exception
            log.error("Failed to collect NMT data: {}", e.getMessage(), e);
            return new NmtSummary(Map.of());
        }
    }

}
