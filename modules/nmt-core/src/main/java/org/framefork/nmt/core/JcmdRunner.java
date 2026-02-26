package org.framefork.nmt.core;

/**
 * Abstraction for running jcmd commands against the current JVM.
 */
public interface JcmdRunner {

    /**
     * Runs a jcmd command with the given arguments against the current JVM process.
     *
     * @param arguments the jcmd command arguments (e.g., "VM.native_memory", "summary", "scale=b")
     * @return the command's stdout output
     * @throws JcmdException if the command fails or times out (unchecked)
     */
    String run(String... arguments);

}
