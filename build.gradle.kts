plugins {
    id("base")
}

allprojects {
    group = "ru.yandex.practicum"
    version = "1.0-SNAPSHOT"
    repositories {
        maven {
            url = uri("https://artifactory.tcsbank.ru/artifactory/maven-all")
            isAllowInsecureProtocol = true
            name = "maven-all"
        }
        mavenCentral()
    }
}
