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
include(":features:login:api")
include(":features:login:implementation")
include(":features:paywall:implementation")
include(":shared:app-update:api")
include(":shared:app-update:implementation")
include(":shared:app-update:fake")
include(":shared:billing:api")
include(":shared:billing:implementation")
include(":shared:billing:fake")
include(":shared:dispatchers")
include(":shared:remote-config:api")
include(":shared:remote-config:implementation")
include(":shared:remote-config:fake")
include(":shared:serialization:api")
include(":shared:serialization:implementation")
include(":shared:serialization:fake")
include(":shared:storage:api")
include(":shared:storage:implementation")
include(":shared:telemetry:api")
include(":shared:telemetry:implementation")
include(":shared:telemetry:fake")
include(":shared:ui-components")
