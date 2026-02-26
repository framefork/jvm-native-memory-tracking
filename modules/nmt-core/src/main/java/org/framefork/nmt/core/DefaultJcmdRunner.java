package org.framefork.nmt.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Default {@link JcmdRunner} that locates the {@code jcmd} executable
 * and runs it as a subprocess against the current JVM.
 */
public final class DefaultJcmdRunner implements JcmdRunner {

    private static final Logger log = LoggerFactory.getLogger(DefaultJcmdRunner.class);

    private final Path jcmdPath;
    private final long pid;
    private final Duration timeout;

    public DefaultJcmdRunner() {
        this(findJcmdPath(), ProcessHandle.current().pid(), Duration.ofSeconds(5));
    }

    public DefaultJcmdRunner(Path jcmdPath, long pid, Duration timeout) {
        this.jcmdPath = jcmdPath;
        this.pid = pid;
        this.timeout = timeout;
    }

    @Override
    public String run(String... arguments) {
        var command = new ArrayList<String>();
        command.add(jcmdPath.toString());
        command.add(String.valueOf(pid));
        command.addAll(Arrays.asList(arguments));

        log.debug("Running jcmd command: {}", command);

        try {
            var process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();

            var completed = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!completed) {
                process.destroyForcibly();
                throw new JcmdException("jcmd command timed out after " + timeout);
            }

            var output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            var exitCode = process.exitValue();

            if (exitCode == 143) {
                // Container shutdown signal -- not an error
                log.warn("jcmd exited with code 143 (likely container shutdown)");
                return "";
            }

            if (exitCode != 0) {
                throw new JcmdException("jcmd exited with code " + exitCode + ": " + output.trim());
            }

            return output;
        } catch (IOException e) {
            throw new JcmdException("Failed to execute jcmd", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JcmdException("jcmd execution interrupted", e);
        }
    }

    /**
     * Returns {@code true} if the jcmd executable can be found on this system.
     */
    public static boolean isJcmdAvailable() {
        try {
            findJcmdPath();
            return true;
        } catch (JcmdException e) {
            return false;
        }
    }

    static Path findJcmdPath() {
        // First try $JAVA_HOME/bin/jcmd
        var javaHome = System.getProperty("java.home");
        if (javaHome != null) {
            var jcmd = Path.of(javaHome, "bin", "jcmd");
            if (Files.isExecutable(jcmd)) {
                return jcmd;
            }
        }

        // Fall back to PATH lookup
        var pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            for (var dir : pathEnv.split(System.getProperty("path.separator", ":"))) {
                var jcmd = Path.of(dir, "jcmd");
                if (Files.isExecutable(jcmd)) {
                    return jcmd;
                }
            }
        }

        throw new JcmdException("jcmd executable not found in $JAVA_HOME/bin or $PATH");
    }

}
