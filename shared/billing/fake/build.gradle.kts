plugins {
    id("fake.library")
    id("detekt.library")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                // api: the fake publicly implements the api contract.
                api(projects.shared.billing.api)
            }
        }
    }
}
