plugins {
    id("android.library")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.compose.material)
                implementation(libs.coil)
                implementation(libs.coil.network)
            }
        }
    }

}