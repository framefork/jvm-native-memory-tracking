package org.framefork.nmt.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Collects NMT data by running the {@code VM.native_memory summary scale=b} diagnostic command
 * through a {@link DiagnosticCommandRunner} and parsing the output.
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
        } catch (DiagnosticCommandException e) {
            log.error("Failed to collect NMT data: {}", e.getMessage(), e);
            return new NmtSummary(Map.of());
        }
    }

}
