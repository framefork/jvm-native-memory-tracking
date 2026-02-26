plugins {
    id("framefork.java")
}

dependencies {
    api(project(":nmt-core"))
    api(libs.junit.jupiter)
    api(libs.assertj.core)

    implementation(libs.logback.classic)
}

project.description = "Shared test utilities for NMT modules"
