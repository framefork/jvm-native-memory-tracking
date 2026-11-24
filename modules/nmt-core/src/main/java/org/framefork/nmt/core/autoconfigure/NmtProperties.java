package org.framefork.nmt.core.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration of the JVM Native Memory Tracking metrics.
 */
@ConfigurationProperties(prefix = NmtProperties.PREFIX)
public class NmtProperties
{

    public static final String PREFIX = "framefork.nmt";

    /**
     * Whether to register the NMT metrics. When false, the library registers no beans.
     */
    private boolean enabled = true;

    /**
     * How the NMT data is read. AUTO uses the DiagnosticCommand MBean when it is registered, and the jcmd executable otherwise.
     */
    private Runner runner = Runner.AUTO;

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public Runner getRunner()
    {
        return runner;
    }

    public void setRunner(Runner runner)
    {
        this.runner = runner;
    }

    public enum Runner
    {

        /**
         * The DiagnosticCommand MBean when it is registered, the jcmd executable otherwise.
         */
        AUTO,

        /**
         * The {@code com.sun.management:type=DiagnosticCommand} MBean, in-process. Needs the {@code jdk.management} module.
         */
        MBEAN,

        /**
         * The {@code jcmd} executable, run as a subprocess. Needs the {@code jdk.jcmd} module.
         */
        JCMD,

    }

}
