plugins {
    id("base")
    id("idea")
    id("org.barfuin.gradle.taskinfo") version ("2.2.0") // ./gradlew tiTree publish
}

group = "org.framefork"

allprojects {
    group = rootProject.group
}
