plugins {
    id("base")
    id("org.flywaydb.flyway") version "10.17.0" apply false
}

allprojects {
    group = "ru.yandex.practicum"
    version = "1.0-SNAPSHOT"
    repositories {
        mavenCentral()
    }
}
