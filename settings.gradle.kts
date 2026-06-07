rootProject.name = "bank"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

include(":frontend")
include(":accounts")
include(":notifications")
include(":cash")
include(":transfer")
