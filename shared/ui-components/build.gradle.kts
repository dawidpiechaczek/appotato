plugins {
    id("android.library")
    id("detekt.library")
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
