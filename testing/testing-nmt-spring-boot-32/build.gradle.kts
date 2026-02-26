plugins {
    id("framefork.java")
}

dependencies {
    api(platform(libs.spring.boot.bom.v32))

    implementation(project(":nmt-micrometer"))
    implementation(project(":nmt-opentelemetry"))

    // Spring Boot BOM controls micrometer + otel versions
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("io.micrometer:micrometer-core")
    implementation("io.opentelemetry:opentelemetry-api")

    testImplementation(project(":nmt-testing"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

project.description = "NMT Spring Boot 3.2.x compatibility tests"
