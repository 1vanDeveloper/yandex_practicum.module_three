import org.gradle.kotlin.dsl.maven

rootProject.name = "bank"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

include("frontend")
include("accounts")
