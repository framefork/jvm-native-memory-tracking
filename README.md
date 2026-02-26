# JVM Native Memory Tracking

A Java library that collects JVM [Native Memory Tracking (NMT)](https://docs.oracle.com/en/java/javase/17/vm/native-memory-tracking.html) data via `jcmd` and exposes it as metrics through [Micrometer](https://micrometer.io/) and/or [OpenTelemetry](https://opentelemetry.io/).

NMT provides visibility into the JVM's native memory usage beyond the Java heap -- thread stacks, GC overhead, compiled code, class metadata, and more. This library makes that data available as standard metrics for your monitoring stack.

## Prerequisites

The JVM must be started with Native Memory Tracking enabled:

```
-XX:NativeMemoryTracking=summary
```

The `jcmd` tool must be available on the system (it's part of the JDK). If you're using a custom JRE built with `jlink`, make sure to include the `jdk.jcmd` module.

Minimum supported Java is 17.

## Installation

Pick the module matching your metrics API:

| Metrics API    | Artifact                                                                                                                         |
|----------------|----------------------------------------------------------------------------------------------------------------------------------|
| Micrometer     | [org.framefork:nmt-micrometer](https://central.sonatype.com/artifact/org.framefork/nmt-micrometer)                               |
| OpenTelemetry  | [org.framefork:nmt-opentelemetry](https://central.sonatype.com/artifact/org.framefork/nmt-opentelemetry)                         |
| Core only      | [org.framefork:nmt-core](https://central.sonatype.com/artifact/org.framefork/nmt-core)                                           |

Find the latest version in this project's [GitHub releases](https://github.com/framefork/jvm-native-memory-tracking/releases) or on [Maven Central](https://central.sonatype.com/namespace/org.framefork).

### Gradle

```kotlin
implementation("org.framefork:nmt-micrometer:${version}")
// or
implementation("org.framefork:nmt-opentelemetry:${version}")
```

### Maven

```xml
<dependency>
    <groupId>org.framefork</groupId>
    <artifactId>nmt-micrometer</artifactId>
    <version>${version}</version>
</dependency>
```

## Spring Boot Auto-Configuration

Both `nmt-micrometer` and `nmt-opentelemetry` include Spring Boot auto-configuration that activates automatically when all conditions are met:

- The corresponding metrics API is on the classpath (`MeterRegistry` or `OpenTelemetry` bean)
- NMT is enabled on the JVM (`-XX:NativeMemoryTracking=summary`)
- The `jcmd` executable is available

No additional configuration is needed. Just add the dependency and the NMT flag.

## Manual Setup (without Spring Boot)

### Micrometer

```java
var collector = new CachingNmtDataCollector(
    new JcmdNmtDataCollector(new DefaultJcmdRunner())
);
var binder = new NmtMeterBinder(collector);
binder.bindTo(meterRegistry);
```

### OpenTelemetry

```java
var collector = new CachingNmtDataCollector(
    new JcmdNmtDataCollector(new DefaultJcmdRunner())
);
var metrics = new NmtOpenTelemetryMetrics(openTelemetry, collector);

// On shutdown:
metrics.close();
```

## Metrics

Both modules register the same two gauges for each NMT memory category:

| Metric name                 | Description                                      | Unit  |
|-----------------------------|--------------------------------------------------|-------|
| `jvm.memory.nmt.committed`  | Committed memory (physical/swap memory in use)   | bytes |
| `jvm.memory.nmt.reserved`   | Reserved memory (virtual address space reserved)  | bytes |

Each metric is tagged/attributed with `category` identifying the NMT memory type (e.g., `java_heap`, `gc`, `thread`, `code`, `class`, `compiler`, `metaspace`, etc.).

The categories are extracted dynamically from `jcmd` output, so new categories added in future JDK versions will be picked up automatically without library changes.

## Supported Versions

| Spring Boot | Micrometer | OpenTelemetry API | Status |
|---|---|---|---|
| 3.2.x | 1.12.x | 1.31.x | Tested |
| 3.3.x | 1.13.x | 1.38.x | Tested |
| 3.4.x | 1.14.x | 1.43.x | Tested |
| 3.5.x | 1.15.x | 1.49.x | Tested |
| 4.0.x | 1.16.x | 1.55.x | Tested |

## Module Structure

| Module             | Description                                                          |
|--------------------|----------------------------------------------------------------------|
| `nmt-core`         | jcmd runner, NMT output parser, data model, caching                  |
| `nmt-micrometer`   | Micrometer `MeterBinder` integration + Spring Boot auto-configuration|
| `nmt-opentelemetry`| OpenTelemetry gauge registration + Spring Boot auto-configuration    |

## How It Works

1. The library runs `jcmd <pid> VM.native_memory summary scale=b` as a subprocess
2. The text output is parsed with regex into structured data (category name, reserved bytes, committed bytes)
3. Results are cached with a 5-second TTL to avoid spawning `jcmd` too frequently
4. Metric callbacks read from the cache, which is refreshed on the next collection cycle

For more details, see the [design documentation](docs/).

## License

[Apache License 2.0](LICENSE)
