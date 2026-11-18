package org.framefork.nmt.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.DynamicMBean;
import javax.management.ImmutableDescriptor;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The JVM's own MBean answers in well under a millisecond, so the timeout is tested against a slow stand-in
 * registered under the same name in a separate MBean server.
 */
class DiagnosticCommandMBeanRunnerTimeoutTest
{

    private final CountDownLatch release = new CountDownLatch(1);

    @AfterEach
    void releaseBlockedCall()
    {
        release.countDown();
    }

    @Test
    void slowCall_timesOut_andBlocksFurtherCallsUntilItFinishes() throws Exception
    {
        var mbeanServer = MBeanServerFactory.newMBeanServer();
        mbeanServer.registerMBean(new BlockingDiagnosticCommand(release), new ObjectName(DiagnosticCommandMBeanRunner.MBEAN_NAME));
        var runner = new DiagnosticCommandMBeanRunner(mbeanServer, Duration.ofMillis(100));

        assertThatThrownBy(() -> runner.run("VM.native_memory", "summary"))
            .isInstanceOf(DiagnosticCommandException.class)
            .hasMessageContaining("timed out after PT0.1S");

        assertThatThrownBy(() -> runner.run("VM.native_memory", "summary"))
            .isInstanceOf(DiagnosticCommandException.class)
            .hasMessageContaining("an earlier call is still running");

        release.countDown();
        // the worker thread is free again once the abandoned call returns
        var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        String output = null;
        while (output == null && System.nanoTime() < deadline) {
            try {
                output = runner.run("VM.native_memory", "summary");
            } catch (DiagnosticCommandException e) {
                Thread.sleep(10);
            }
        }
        assertThat(output).isEqualTo("released");
    }

    private static final class BlockingDiagnosticCommand implements DynamicMBean
    {

        private final CountDownLatch release;

        BlockingDiagnosticCommand(CountDownLatch release)
        {
            this.release = release;
        }

        @Override
        public Object invoke(String actionName, Object[] params, String[] signature)
        {
            // ignore interrupts, like the uninterruptible native call does
            while (true) {
                try {
                    release.await();
                    break;
                } catch (InterruptedException ignored) {
                    // keep waiting
                }
            }
            return "released";
        }

        @Override
        public MBeanInfo getMBeanInfo()
        {
            var operation = new MBeanOperationInfo(
                "vmNativeMemory",
                "Blocks until released",
                new MBeanParameterInfo[]{new MBeanParameterInfo("arguments", String[].class.getName(), "")},
                String.class.getName(),
                MBeanOperationInfo.ACTION_INFO,
                new ImmutableDescriptor("dcmd.name=VM.native_memory")
            );
            return new MBeanInfo(getClass().getName(), "", null, null, new MBeanOperationInfo[]{operation}, null);
        }

        @Override
        public Object getAttribute(String attribute)
        {
            throw new UnsupportedOperationException(attribute);
        }

        @Override
        public void setAttribute(Attribute attribute)
        {
            throw new UnsupportedOperationException(attribute.getName());
        }

        @Override
        public AttributeList getAttributes(String[] attributes)
        {
            return new AttributeList();
        }

        @Override
        public AttributeList setAttributes(AttributeList attributes)
        {
            return new AttributeList();
        }

    }

}
