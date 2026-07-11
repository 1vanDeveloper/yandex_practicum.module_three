rootProject.name = "bank"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
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
