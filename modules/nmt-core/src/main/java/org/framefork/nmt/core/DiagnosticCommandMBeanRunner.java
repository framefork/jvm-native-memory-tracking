package org.framefork.nmt.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * {@link DiagnosticCommandRunner} that runs diagnostic commands in-process through the
 * {@code com.sun.management:type=DiagnosticCommand} MBean of the platform MBean server.
 *
 * <p>The MBean runs the same diagnostic command code in the JVM as {@code jcmd} does, and returns the same text,
 * without the {@code <pid>:} header line that {@code jcmd} prints. It needs no {@code jcmd} executable, no subprocess
 * and no attach mechanism, only the {@code jdk.management} module, which JRE images include.</p>
 *
 * <p>The MBean call runs native code that cannot be interrupted. To enforce the timeout, the call runs on a single
 * daemon worker thread, and a call that takes longer than the timeout is abandoned. While a call is still running,
 * including an abandoned one, further calls fail immediately instead of queueing behind it.</p>
 */
public final class DiagnosticCommandMBeanRunner implements DiagnosticCommandRunner
{

    private static final Logger log = LoggerFactory.getLogger(DiagnosticCommandMBeanRunner.class);

    /**
     * Name of the MBean registered by the {@code jdk.management} module.
     */
    public static final String MBEAN_NAME = "com.sun.management:type=DiagnosticCommand";

    /**
     * Descriptor field that holds the diagnostic command name (e.g. {@code VM.native_memory}) of an MBean operation.
     */
    private static final String DCMD_NAME_DESCRIPTOR_FIELD = "dcmd.name";

    private static final String[] OPERATION_SIGNATURE = {String[].class.getName()};

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);

    private static final Duration WORKER_KEEP_ALIVE = Duration.ofMinutes(1);

    private final MBeanServer mbeanServer;
    private final ObjectName objectName;
    private final Duration timeout;
    private final ThreadPoolExecutor worker;
    // Taken by the caller before the call is submitted and released by the worker when the MBean call returns. The pool
    // cannot be the gate: the worker thread is still busy for a moment after a call's result reaches the caller.
    private final Semaphore callInFlight = new Semaphore(1);

    private volatile Map<String, String> operationNames = Map.of();

    public DiagnosticCommandMBeanRunner()
    {
        this(ManagementFactory.getPlatformMBeanServer(), DEFAULT_TIMEOUT);
    }

    public DiagnosticCommandMBeanRunner(MBeanServer mbeanServer, Duration timeout)
    {
        this.mbeanServer = mbeanServer;
        this.objectName = diagnosticCommandObjectName();
        this.timeout = timeout;
        // One worker thread; the idle thread ends after the keep-alive and a new one starts on demand.
        this.worker = new ThreadPoolExecutor(
            1,
            1,
            WORKER_KEEP_ALIVE.toMillis(),
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(),
            runnable -> {
                var thread = new Thread(runnable, "nmt-diagnostic-command");
                thread.setDaemon(true);
                return thread;
            }
        );
        this.worker.allowCoreThreadTimeOut(true);
    }

    /**
     * Runs a diagnostic command through the MBean.
     *
     * @param arguments the command name followed by its arguments (e.g., "VM.native_memory", "summary", "scale=b")
     * @return the command's text output
     * @throws DiagnosticCommandException if the MBean is not available, the command is unknown or fails, or the call times out
     */
    @Override
    public String run(String... arguments)
    {
        if (arguments.length == 0) {
            throw new DiagnosticCommandException("No diagnostic command given");
        }

        var command = arguments[0];
        var commandArguments = Arrays.copyOfRange(arguments, 1, arguments.length);

        log.debug("Running diagnostic command through the MBean: {} {}", command, Arrays.toString(commandArguments));

        var operationName = resolveOperationName(command);

        if (!callInFlight.tryAcquire()) {
            throw new DiagnosticCommandException("Diagnostic command " + command + " not started, an earlier call is still running");
        }

        try {
            var future = submit(operationName, commandArguments);
            try {
                return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);

            } catch (TimeoutException e) {
                // Not cancelled: an interrupt cannot stop the native call, and the worker releases callInFlight when it returns.
                throw new DiagnosticCommandException("Diagnostic command " + command + " timed out after " + timeout, e);
            }

        } catch (ExecutionException e) {
            var cause = e.getCause();
            if (cause instanceof DiagnosticCommandException commandException) {
                throw commandException;
            }
            throw new DiagnosticCommandException("Diagnostic command " + command + " failed: " + cause, cause != null ? cause : e);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DiagnosticCommandException("Diagnostic command " + command + " interrupted", e);
        }
    }

    private Future<String> submit(String operationName, String[] commandArguments)
    {
        try {
            return worker.submit(() -> {
                try {
                    return invoke(operationName, commandArguments);
                } finally {
                    callInFlight.release();
                }
            });

        } catch (RejectedExecutionException e) {
            callInFlight.release();
            throw new DiagnosticCommandException("Diagnostic command operation " + operationName + " not started: " + e.getMessage(), e);
        }
    }

    private String invoke(String operationName, String[] commandArguments)
    {
        try {
            var result = mbeanServer.invoke(objectName, operationName, new Object[]{commandArguments}, OPERATION_SIGNATURE);
            return result == null ? "" : result.toString();

        } catch (JMException | RuntimeException e) {
            // RuntimeMBeanException (a bad argument, a SecurityManager denial) is a JMRuntimeException, not a JMException
            throw new DiagnosticCommandException("Diagnostic command operation " + operationName + " failed: " + e.getMessage(), e);
        }
    }

    /**
     * Finds the MBean operation for a diagnostic command by the {@code dcmd.name} field of the operation descriptors,
     * so the runner relies on the JDK's own mapping (e.g. {@code VM.native_memory} to {@code vmNativeMemory}).
     */
    private String resolveOperationName(String command)
    {
        var operationName = operationNames.get(command);
        if (operationName != null) {
            return operationName;
        }

        synchronized (this) {
            if (operationNames.isEmpty()) {
                operationNames = loadOperationNames();
            }
        }

        operationName = operationNames.get(command);
        if (operationName == null) {
            throw new DiagnosticCommandException("Diagnostic command " + command + " is not available through the " + MBEAN_NAME + " MBean");
        }
        return operationName;
    }

    private Map<String, String> loadOperationNames()
    {
        try {
            var names = new HashMap<String, String>();
            for (var operation : mbeanServer.getMBeanInfo(objectName).getOperations()) {
                if (operation.getDescriptor().getFieldValue(DCMD_NAME_DESCRIPTOR_FIELD) instanceof String dcmdName) {
                    names.put(dcmdName, operation.getName());
                }
            }
            return Map.copyOf(names);

        } catch (JMException | RuntimeException e) {
            throw new DiagnosticCommandException("Failed to read the operations of the " + MBEAN_NAME + " MBean: " + e.getMessage(), e);
        }
    }

    /**
     * Returns {@code true} if the DiagnosticCommand MBean is registered in the platform MBean server.
     *
     * <p>The MBean is registered whether or not Native Memory Tracking is enabled.
     * It is missing on runtimes without the {@code jdk.management} module.</p>
     */
    public static boolean isAvailable()
    {
        try {
            return ManagementFactory.getPlatformMBeanServer().isRegistered(diagnosticCommandObjectName());

        } catch (RuntimeException e) {
            log.debug("The {} MBean is not available: {}", MBEAN_NAME, e.getMessage(), e);
            return false;
        }
    }

    private static ObjectName diagnosticCommandObjectName()
    {
        try {
            return new ObjectName(MBEAN_NAME);

        } catch (MalformedObjectNameException e) {
            throw new IllegalStateException(e);
        }
    }

}
