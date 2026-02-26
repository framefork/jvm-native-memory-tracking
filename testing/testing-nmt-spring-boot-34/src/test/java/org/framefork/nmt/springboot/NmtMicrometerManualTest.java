package org.framefork.nmt.springboot;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.framefork.nmt.core.JcmdNmtDataCollector;
import org.framefork.nmt.micrometer.NmtMeterBinder;
import org.framefork.nmt.testing.CannedJcmdRunner;
import org.framefork.nmt.testing.NmtTestResources;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NmtMicrometerManualTest {

    @Test
    void manualWiringRegistersGauges() {
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
    void gaugeValuesMatchCannedData() {
        var registry = new SimpleMeterRegistry();
        var binder = createBinder();

        binder.bindTo(registry);

        var javaHeapCommitted = registry.get("jvm.memory.nmt.committed")
            .tag("category", "java_heap")
            .gauge();
        assertThat(javaHeapCommitted.value()).isEqualTo(1_174_405_120.0);
    }

    @Test
    void gaugesHaveCorrectBaseUnit() {
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

    private static NmtMeterBinder createBinder() {
        var output = NmtTestResources.loadSample("nmt-summary-jdk17.txt");
        var jcmdRunner = new CannedJcmdRunner(output);
        var collector = new JcmdNmtDataCollector(jcmdRunner);
        return new NmtMeterBinder(collector);
    }
}
