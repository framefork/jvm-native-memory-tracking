plugins {
    id("framefork.java-public")
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

project.description = "JVM Native Memory Tracking data collector via jcmd"
