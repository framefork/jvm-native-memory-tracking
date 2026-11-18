package org.framefork.nmt.core;

import java.lang.management.ManagementFactory;

/**
 * Utility to detect whether JVM Native Memory Tracking is enabled
 * and which ways of reading its data are available.
 */
public final class NmtAvailability
{

    private static final String NMT_FLAG_PREFIX = "-XX:NativeMemoryTracking=";

    private NmtAvailability()
    {
    }

    /**
     * Returns the NMT mode configured for the current JVM.
     */
    public static NmtMode getNmtMode()
    {
        var arguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
        for (var argument : arguments) {
            if (argument.startsWith(NMT_FLAG_PREFIX)) {
                return NmtMode.fromString(argument.substring(NMT_FLAG_PREFIX.length()));
            }
        }
        return NmtMode.OFF;
    }

    /**
     * Returns {@code true} if NMT is enabled (summary or detail mode).
     */
    public static boolean isNmtEnabled()
    {
        return getNmtMode() != NmtMode.OFF;
    }

    /**
     * Returns {@code true} if the {@code com.sun.management:type=DiagnosticCommand} MBean is registered,
     * so {@link DiagnosticCommandMBeanRunner} can read NMT data in-process.
     */
    public static boolean isDiagnosticCommandAvailable()
    {
        return DiagnosticCommandMBeanRunner.isAvailable();
    }

    /**
     * Returns {@code true} if the jcmd executable can be found on this system.
     */
    public static boolean isJcmdAvailable()
    {
        return JcmdProcessRunner.isJcmdAvailable();
    }

}
