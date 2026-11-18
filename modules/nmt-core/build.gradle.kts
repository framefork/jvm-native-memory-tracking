plugins {
    id("org.framefork.build.library-published")
    `java-test-fixtures`
}

dependencies {
    api(libs.errorprone.annotations)

    compileOnly(libs.jetbrains.annotations)

    implementation(libs.slf4j.api)

    testFixturesApi(libs.junit.jupiter)
    testFixturesApi(libs.assertj.core)
    testFixturesImplementation(libs.logback.classic)

    testImplementation(testFixtures(project))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testImplementation(libs.logback.classic)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

project.description = "JVM Native Memory Tracking data collector"

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
