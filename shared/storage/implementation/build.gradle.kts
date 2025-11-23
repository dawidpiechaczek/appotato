plugins {
    id("android.library")
    id("detekt.library")
}

kotlin {

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.shared.storage.api)
            }
        }

        commonTest {}
    }

}