plugins {
    id("fake.library")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.shared.serialization.api)
            }
        }
    }
}
