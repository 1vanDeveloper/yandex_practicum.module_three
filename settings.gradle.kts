rootProject.name = "bank"

pluginManagement {
    repositories {
        maven {
            url = uri("https://artifactory.tcsbank.ru/artifactory/maven-all")
            isAllowInsecureProtocol = true
            name = "maven-all"
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        maven {
            url = uri("https://artifactory.tcsbank.ru/artifactory/maven-all")
            isAllowInsecureProtocol = true
            name = "maven-all"
        }
        mavenCentral()
    }
}

include(":frontend")
include(":accounts")
include(":notifications")
include(":cash")
include(":transfer")
include(":gateway")
