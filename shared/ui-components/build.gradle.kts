plugins {
    id("android.library")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {

                implementation(libs.coil)
                implementation(libs.coil.network)
            }
        }
    }

}