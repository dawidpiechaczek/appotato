plugins {
    id("fake.library")
    id("detekt.library")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.shared.remoteConfig.api)
            }
        }
    }
}
