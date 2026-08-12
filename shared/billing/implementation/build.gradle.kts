plugins {
    id("android.library")
    id("detekt.library")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.shared.billing.api)
                implementation(libs.kotlinx.coroutines.core)

                api(project.dependencies.platform(libs.koin.bom))
                api(libs.koin.core)
            }
        }
    }
}
