package org.framefork.nmt.micrometer;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.framefork.nmt.core.JcmdNmtDataCollector;
import org.framefork.nmt.testing.CannedJcmdRunner;
import org.framefork.nmt.testing.NmtTestResources;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NmtMeterBinderTest
{

    @Test
    void bindTo_registersCommittedAndReservedGauges()
    {
        var registry = new SimpleMeterRegistry();
        var binder = createBinder();

        binder.bindTo(registry);

        var committedMeters = registry.getMeters().stream()
            .filter(m -> m.getId().getName().equals("jvm.memory.nmt.committed"))
            .toList();
        var reservedMeters = registry.getMeters().stream()
            .filter(m -> m.getId().getName().equals("jvm.memory.nmt.reserved"))
            .toList();

        assertThat(committedMeters).isNotEmpty();
        assertThat(reservedMeters).isNotEmpty();
    }

    @Test
    void bindTo_gaugeValuesMatchCannedData()
    {
        var registry = new SimpleMeterRegistry();
        var binder = createBinder();

        binder.bindTo(registry);

        var javaHeapCommitted = registry.get("jvm.memory.nmt.committed")
            .tag("category", "java_heap")
            .gauge();
        assertThat(javaHeapCommitted.value()).isEqualTo(1_174_405_120.0);

        var javaHeapReserved = registry.get("jvm.memory.nmt.reserved")
            .tag("category", "java_heap")
            .gauge();
        assertThat(javaHeapReserved.value()).isEqualTo(16_777_216_000.0);

        var gcCommitted = registry.get("jvm.memory.nmt.committed")
            .tag("category", "gc")
            .gauge();
        assertThat(gcCommitted.value()).isEqualTo(89_267_720.0);
    }

    @Test
    void bindTo_gaugesHaveCategoryTag()
    {
        var registry = new SimpleMeterRegistry();
        var binder = createBinder();

        binder.bindTo(registry);

        var anyGauge = registry.getMeters().stream()
            .filter(m -> m.getId().getName().equals("jvm.memory.nmt.committed"))
            .findFirst()
            .orElseThrow();

        assertThat(anyGauge.getId().getTag("category")).isNotNull();
    }

    @Test
    void bindTo_baseUnitIsBytes()
    {
        var registry = new SimpleMeterRegistry();
        var binder = createBinder();

        binder.bindTo(registry);

        var gauge = registry.getMeters().stream()
            .filter(m -> m.getId().getName().equals("jvm.memory.nmt.committed"))
            .map(Meter::getId)
            .findFirst()
            .orElseThrow();

        assertThat(gauge.getBaseUnit()).isEqualTo("bytes");
    }

    private static NmtMeterBinder createBinder()
    {
        var output = NmtTestResources.loadSample("nmt-summary-jdk17.txt");
        var jcmdRunner = new CannedJcmdRunner(output);
        var collector = new JcmdNmtDataCollector(jcmdRunner);
        return new NmtMeterBinder(collector);
    }

}
