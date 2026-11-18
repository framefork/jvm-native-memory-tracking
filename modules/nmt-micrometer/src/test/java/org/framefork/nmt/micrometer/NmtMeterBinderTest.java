package org.framefork.nmt.micrometer;

import io.micrometer.core.instrument.Measurement;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.framefork.nmt.core.DiagnosticCommandNmtDataCollector;
import org.framefork.nmt.core.NmtSummary;
import org.framefork.nmt.testing.CannedDiagnosticCommandRunner;
import org.framefork.nmt.testing.NmtTestResources;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

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

    @Test
    void bindTo_registersTheCategoriesOnceAReadAfterAnEmptyFirstReadReturnsThem()
    {
        var registry = new SimpleMeterRegistry();
        var summary = new AtomicReference<>(new NmtSummary(Map.of()));
        new NmtMeterBinder(summary::get).bindTo(registry);

        summary.set(loadSummary("nmt-summary-jdk17.txt"));
        scrape(registry);

        assertThat(registry.get("jvm.memory.nmt.committed").tag("category", "java_heap").gauge().value()).isEqualTo(1_174_405_120.0);
        assertThat(registry.get("jvm.memory.nmt.reserved").tag("category", "gc").gauge().value()).isEqualTo(394_010_120.0);
    }

    @Test
    void bindTo_registersCategoriesThatAppearAfterTheFirstRead()
    {
        var registry = new SimpleMeterRegistry();
        var summary = new AtomicReference<>(loadSummary("nmt-summary-minimal.txt"));
        new NmtMeterBinder(summary::get).bindTo(registry);
        assertThat(registry.find("jvm.memory.nmt.committed").tag("category", "metaspace").gauge()).isNull();

        summary.set(loadSummary("nmt-summary-jdk17.txt"));
        scrape(registry);

        assertThat(registry.get("jvm.memory.nmt.committed").tag("category", "metaspace").gauge().value()).isEqualTo(38_997_520.0);
    }

    /**
     * Reads every meter, the way a registry does when it publishes or is scraped.
     */
    private static void scrape(MeterRegistry registry)
    {
        for (var meter : registry.getMeters()) {
            // a Measurement calls the gauge function only when its value is read
            meter.measure().forEach(Measurement::getValue);
        }
    }

    private static NmtSummary loadSummary(String sampleName)
    {
        return new DiagnosticCommandNmtDataCollector(new CannedDiagnosticCommandRunner(NmtTestResources.loadSample(sampleName))).collect();
    }

    private static NmtMeterBinder createBinder()
    {
        var output = NmtTestResources.loadSample("nmt-summary-jdk17.txt");
        var runner = new CannedDiagnosticCommandRunner(output);
        var collector = new DiagnosticCommandNmtDataCollector(runner);
        return new NmtMeterBinder(collector);
    }

}
