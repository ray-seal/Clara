pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
}
plugins {
    id("com.android.application") version "8.4.0"
    id("com.google.gms.google-services") version "4.4.1"
}
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
}
}
rootProject.name = "Clara"
include(":app")