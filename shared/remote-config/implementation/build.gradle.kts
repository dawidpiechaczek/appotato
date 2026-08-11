plugins {
    id("android.library")
    id("detekt.library")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.shared.remoteConfig.api)
            }
        }

        androidMain {
            dependencies {
                api(project.dependencies.platform(libs.koin.bom))
                api(libs.koin.core)
                implementation(libs.koin.android)

                implementation(project.dependencies.platform(libs.firebase.bom))
                implementation(libs.firebase.config)
            }
        }
    }
}
