package org.framefork.nmt.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Collects NMT data by running {@code jcmd VM.native_memory summary scale=b}
 * and parsing the output.
 */
public final class JcmdNmtDataCollector implements NmtDataCollector {

    private static final Logger log = LoggerFactory.getLogger(JcmdNmtDataCollector.class);

    private final JcmdRunner jcmdRunner;
    private final NmtOutputParser parser;

    public JcmdNmtDataCollector(JcmdRunner jcmdRunner) {
        this.jcmdRunner = jcmdRunner;
        this.parser = new NmtOutputParser();
    }

    @Override
    public NativeMemoryTrackingSummary collect() {
        try {
            var output = jcmdRunner.run("VM.native_memory", "summary", "scale=b");
            return parser.parse(output);
        } catch (JcmdException e) {
            log.error("Failed to collect NMT data: {}", e.getMessage(), e);
            return new NativeMemoryTrackingSummary(Map.of());
        }
    }

}
