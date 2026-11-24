# JVM Native Memory Tracking

A Java library that collects JVM [Native Memory Tracking (NMT)](https://docs.oracle.com/en/java/javase/17/vm/native-memory-tracking.html) data from inside the JVM and exposes it as metrics through [Micrometer](https://micrometer.io/) and/or [OpenTelemetry](https://opentelemetry.io/).

NMT provides visibility into the JVM's native memory usage beyond the Java heap -- thread stacks, GC overhead, compiled code, class metadata, and more. This library makes that data available as standard metrics for your monitoring stack.

## Prerequisites

The JVM must be started with Native Memory Tracking enabled:

```
-XX:NativeMemoryTracking=summary
```

The library reads the data in-process through the `com.sun.management:type=DiagnosticCommand` MBean, which the `jdk.management` module provides. JDK and JRE images include that module, so neither the `jcmd` executable nor a full JDK is needed. If you build a custom runtime with `jlink`, see [Docker and Custom JRE Images](#docker-and-custom-jre-images).

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
- The runner selected by `framefork.nmt.runner` is available
- `framefork.nmt.enabled` is not `false`

No additional configuration is needed. Just add the dependency and the NMT flag.

### Configuration

| Property                  | Default | Values                              | Description                                                                                                                                               |
|---------------------------|---------|-------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------|
| `framefork.nmt.enabled`   | `true`  | `true`, `false`                     | Whether to register the NMT metrics. With `false`, the library registers no beans and logs nothing, even when the application defines its own `NmtDataCollector`. |
| `framefork.nmt.runner`    | `auto`  | `auto`, `mbean`, `jcmd`             | How the NMT data is read. `mbean` calls the DiagnosticCommand MBean in-process, `jcmd` runs the `jcmd` executable as a subprocess. `auto` uses the MBean when it is registered, and `jcmd` otherwise. |
| `framefork.nmt.cache-ttl` | `5s`    | a `Duration`, such as `10s` or `1m` | How long a read of the NMT data is reused before the diagnostic command runs again. Must be positive: with `0s` or a negative value, the application fails to start. |

If NMT is enabled but the selected runner is not available, the auto-configuration logs a warning and does not register any metrics. The application still starts.

Each layer backs off when the application defines its own bean: `DiagnosticCommandRunner`, `NmtDataCollector`, `NmtMeterBinder` and `NmtOpenTelemetryMetrics`. For example, define a `DiagnosticCommandRunner` bean to read the data in a different way, and keep the caching, parsing and metrics of the library.

## Manual Setup (without Spring Boot)

### Micrometer

```java
var collector = new CachingNmtDataCollector(
    new DiagnosticCommandNmtDataCollector(new DiagnosticCommandMBeanRunner())
);
var binder = new NmtMeterBinder(collector);
binder.bindTo(meterRegistry);
```

### OpenTelemetry

```java
var collector = new CachingNmtDataCollector(
    new DiagnosticCommandNmtDataCollector(new DiagnosticCommandMBeanRunner())
);
var metrics = new NmtOpenTelemetryMetrics(openTelemetry, collector);

// On shutdown:
metrics.close();
```

To run the `jcmd` executable instead, replace `new DiagnosticCommandMBeanRunner()` with `new JcmdProcessRunner()`.

## Metrics

Both modules register the same two gauges for each NMT memory category:

| Metric name                 | Description                                      | Unit  |
|-----------------------------|--------------------------------------------------|-------|
| `jvm.memory.nmt.committed`  | Committed memory (physical/swap memory in use)   | bytes |
| `jvm.memory.nmt.reserved`   | Reserved memory (virtual address space reserved)  | bytes |

Each metric is tagged/attributed with `category` identifying the NMT memory type (e.g., `java_heap`, `gc`, `thread`, `code`, `class`, `compiler`, `metaspace`, etc.).

The categories are extracted dynamically from the NMT output, so new categories added in future JDK versions will be picked up automatically without library changes.

A category gets its series from the first read that reports it. A failed read at startup, or a category that the JVM reports only later, such as `Logging`, therefore does not leave the category out for the life of the JVM. When a category is missing from a later read, OpenTelemetry records no value for it, and the Micrometer gauge reads 0.

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
| `nmt-core`         | MBean and jcmd runners, NMT output parser, data model, caching, Spring Boot auto-configuration of the collector |
| `nmt-micrometer`   | Micrometer `MeterBinder` integration + Spring Boot auto-configuration|
| `nmt-opentelemetry`| OpenTelemetry gauge registration + Spring Boot auto-configuration    |

## How It Works

1. The library runs the `VM.native_memory summary scale=b` diagnostic command against its own JVM
2. The text output is parsed with regex into structured data (category name, reserved bytes, committed bytes)
3. Results are cached with a TTL, 5 seconds by default (`framefork.nmt.cache-ttl`), to avoid running the command too frequently
4. Metric callbacks read from the cache, which is refreshed on the next collection cycle

### Runners

A `DiagnosticCommandRunner` runs the diagnostic command. The library has two:

- `DiagnosticCommandMBeanRunner` (default) invokes the `vmNativeMemory` operation of the `com.sun.management:type=DiagnosticCommand` MBean. The JVM runs the same diagnostic command code as for `jcmd`, on the calling thread, in well under a millisecond. It needs no subprocess, no `jcmd` executable and no attach mechanism, so it also works with `-XX:+DisableAttachMechanism`.
- `JcmdProcessRunner` runs `jcmd <pid> VM.native_memory summary scale=b` as a subprocess. Each call starts a new JVM process, which takes about 50 ms.

Both runners give the same output, with two differences:

- `jcmd` prints a `<pid>:` header line before the summary. The parser ignores it.
- On JDK 21 and later, `jcmd` reports about 1 MB more in `internal` and `total`. That is the output buffer of the attach listener that answers `jcmd`, which exists only while `jcmd` is connected. The MBean does not count it.

Both runners have a 5-second timeout. The MBean call runs native code that cannot be interrupted, so `DiagnosticCommandMBeanRunner` runs it on a daemon worker thread and abandons a call that takes longer. Until the abandoned call returns, the next calls fail instead of queueing behind it.

### Output Format

The `VM.native_memory summary scale=b` command produces text output like:

```
Native Memory Tracking:

Total: reserved=1607492304, committed=196489936
       malloc: 30716624 #15376
       mmap:   reserved=1576775680, committed=165773312

-                 Java Heap (reserved=104857600, committed=104857600)
                            (mmap: reserved=104857600, committed=104857600)

-                     Class (reserved=1073862359, committed=317143)
                            (classes #1093)
                            ...

-                    Thread (reserved=23194136, committed=1092120)
                            ...
```

The parser dynamically extracts category names and values using regex, so it is forward-compatible with new JDK versions that may introduce additional categories.

### NMT Categories

The following categories are typically reported by the JVM (varies by JDK version):

| NMT label  | Metric tag value | Description |
|---|---|---|
| `Total` | `total` | Sum of all categories |
| `Java Heap` | `java_heap` | Heap memory |
| `Class` | `class` | Class metadata |
| `Thread` | `thread` | Thread stacks and structures |
| `Code` | `code` | Generated/compiled code |
| `GC` | `gc` | Garbage collector data structures |
| `GCCardSet` | `gccard_set` | G1 card set remembered set |
| `Compiler` | `compiler` | JIT compiler |
| `Internal` | `internal` | VM internal use |
| `Symbol` | `symbol` | String table, symbol table |
| `Native Memory Tracking` | `native_memory_tracking` | NMT's own overhead |
| `Shared class space` | `shared_class_space` | CDS archive |
| `Arena Chunk` | `arena_chunk` | Arena allocations |
| `Tracing` | `tracing` | Tracing infrastructure |
| `Logging` | `logging` | Logging infrastructure |
| `Arguments` | `arguments` | VM arguments |
| `Module` | `module` | Module system |
| `Safepoint` | `safepoint` | Safepoint infrastructure |
| `Synchronization` | `synchronization` | Synchronization primitives |
| `Metaspace` | `metaspace` | Metaspace memory |
| `String Deduplication` | `string_deduplication` | G1 string deduplication |
| `Object Monitors` | `object_monitors` | Object monitors |

Category labels are converted to lowercase snake_case for metric tag/attribute values. New categories added in future JDK versions are picked up automatically.

Reference: [OpenJDK memory types in `allocation.hpp`](https://github.com/openjdk/jdk/blob/0ee196bef199c3d32c1f88b26eb4333a7ea73c10/src/hotspot/share/memory/allocation.hpp#L102)

### Caching

Since multiple metric callbacks fire independently during a single collection cycle (one per category per metric = ~46 calls), the diagnostic command should only run once per interval. The `CachingNmtDataCollector` stores the parsed result with a TTL of 5 seconds, which the `framefork.nmt.cache-ttl` property changes in Spring Boot. The TTL must be positive, because without the cache one scrape would run the command once per callback, and the categories of one scrape would come from different reads. When any metric callback fires, it reads from the cache and only runs the command if the cache has expired.

### Error Handling

- **Runner and parser failures:** A failed or timed-out command, any exception from a custom `DiagnosticCommandRunner`, and any exception from the parser are logged and result in empty metrics (zeros) rather than exceptions propagating to the metrics system.
- **Container shutdown (exit code 143):** With the `jcmd` runner, `jcmd` may return exit code 143 (SIGTERM) while the container is shutting down. This is logged as a warning, and empty values are returned.
- **NMT not enabled:** If NMT is not enabled, the command returns `"Native memory tracking is not enabled"`. The parser handles this and returns an empty summary.

### Docker and Custom JRE Images

Prebuilt JRE images, such as `eclipse-temurin:21-jre`, include the `jdk.management` module, so the default runner works there without `jcmd`.

If you build a custom runtime image with `jlink`, include the `jdk.management` module:

```
jlink --add-modules ...,jdk.management --output /opt/jre
```

To use `framefork.nmt.runner=jcmd`, also include the `jdk.jcmd` module. Without the module that the selected runner needs, the Spring Boot auto-configuration logs a warning and does not register the metrics.

## License

[Apache License 2.0](LICENSE)
