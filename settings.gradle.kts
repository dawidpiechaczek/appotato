rootProject.name = "Appotato"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":composeApp")
include(":shared:ui-components")
include(":shared:login:api")
include(":shared:login:implementation")
include(":shared:storage:api")
include(":shared:storage:implementation")
include(":shared:serialization:api")
include(":shared:serialization:implementation")
include(":shared:dispatchers")
include(":shared:serialization:fake")
