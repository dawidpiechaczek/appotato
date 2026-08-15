plugins {
    id("android.library")
    id("detekt.library")
}

compose.resources {
    publicResClass = false
    packageOfResClass = "com.appotato.features.recipes.implementation.generated.resources"
    generateResClass = always
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(compose.components.resources)
                implementation(projects.shared.uiComponents)
            }
        }
    }
}
