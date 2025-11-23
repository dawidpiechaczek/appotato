plugins {
    id("fake.library")
    id("detekt.library")
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
