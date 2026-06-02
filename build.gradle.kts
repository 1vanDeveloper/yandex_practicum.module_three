plugins {
    id("base")
}

allprojects {
    repositories {
        maven {
            url = uri("https://artifactory.tcsbank.ru/artifactory/maven-all")
            isAllowInsecureProtocol = true
            name = "maven-all"
        }
    }
}
