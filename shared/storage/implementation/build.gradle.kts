plugins {
    id("android.library")
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