plugins {
    id("api.library")
    id("detekt.library")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                // api, not implementation: StateFlow is part of the Billing signature, so consumers
                // need the type on their own compile classpath.
                api(libs.kotlinx.coroutines.core)
            }
        }
    }
}
