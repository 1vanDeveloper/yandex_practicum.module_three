rootProject.name = "bank"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        mavenCentral()
    }
}

include(":frontend")
include(":accounts")
include(":notifications")
include(":cash")
include(":transfer")
include(":gateway")
