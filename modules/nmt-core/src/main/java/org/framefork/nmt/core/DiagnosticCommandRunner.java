package org.framefork.nmt.core;

/**
 * Runs HotSpot diagnostic commands, the commands that {@code jcmd} sends, against the current JVM.
 *
 * <p>{@link DiagnosticCommandMBeanRunner} runs them in-process through the DiagnosticCommand MBean,
 * {@link JcmdProcessRunner} runs the {@code jcmd} executable as a subprocess.</p>
 */
public interface DiagnosticCommandRunner
{

    /**
     * Runs a diagnostic command with the given arguments against the current JVM.
     *
     * @param arguments the command name followed by its arguments (e.g., "VM.native_memory", "summary", "scale=b")
     * @return the command's text output
     * @throws DiagnosticCommandException if the command fails or times out (unchecked)
     */
    String run(String... arguments);

}
