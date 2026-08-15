plugins {
    id("android.library")
    id("detekt.library")
}

compose.resources {
    // Icons stay internal: features ask for AppotatoIcon.Delete, never for a drawable.
    publicResClass = false
    packageOfResClass = "com.appotato.shared.ui.components.generated.resources"
    generateResClass = always
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(compose.components.resources)
                implementation(libs.coil)
                implementation(libs.coil.network)
            }
        }
    }

}
