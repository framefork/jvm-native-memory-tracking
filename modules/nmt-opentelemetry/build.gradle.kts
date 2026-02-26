plugins {
    id("framefork.java-public")
}

dependencies {
    api(project(":nmt-core"))
    api(libs.opentelemetry.api)

    implementation(libs.slf4j.api)

    compileOnly(libs.jetbrains.annotations)
    compileOnly(libs.spring.boot.autoconfigure)
    annotationProcessor(libs.spring.boot.configuration.processor)

    testImplementation(testFixtures(project(":nmt-core")))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testImplementation(libs.logback.classic)
    testImplementation(libs.opentelemetry.sdk.testing)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

project.description = "JVM Native Memory Tracking metrics for OpenTelemetry"
