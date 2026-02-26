plugins {
    id("framefork.java-public")
}

dependencies {
    api(libs.errorprone.annotations)

    compileOnly(libs.jetbrains.annotations)

    implementation(libs.slf4j.api)

    testImplementation(project(":nmt-testing"))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testImplementation(libs.logback.classic)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

project.description = "JVM Native Memory Tracking data collector via jcmd"
