plugins {
    id("org.framefork.build.library-internal")
}

dependencies {
    api(platform(libs.spring.boot.bom.v34))

    implementation(project(":nmt-micrometer"))
    implementation(project(":nmt-opentelemetry"))

    // Spring Boot BOM controls micrometer + otel versions
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("io.micrometer:micrometer-core")
    implementation("io.opentelemetry:opentelemetry-api")

    testImplementation(testFixtures(project(":nmt-core")))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
    // Spring Boot's own metrics and OpenTelemetry auto-configurations, so the tests run the NMT ones next to them
    testImplementation("org.springframework.boot:spring-boot-actuator-autoconfigure")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

project.description = "NMT Spring Boot 3.4.x compatibility tests"

// Native Memory Tracking can only be turned on at JVM start, so the tests that need it run in their own test JVM.
// The regular test task runs with NMT off and covers the paths where the library backs off.
tasks.test {
    useJUnitPlatform {
        excludeTags("nmt")
    }
}

val nmtTest = tasks.register<Test>("nmtTest") {
    description = "Runs the tests tagged nmt in a JVM started with -XX:NativeMemoryTracking=summary."
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("nmt")
    }
    jvmArgs("-XX:NativeMemoryTracking=summary")
    shouldRunAfter(tasks.test)
}

tasks.check {
    dependsOn(nmtTest)
}
