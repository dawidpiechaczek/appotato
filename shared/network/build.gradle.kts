plugins {
    id("android.library")
    id("detekt.library")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                // api: consumers of this module take an `HttpClient` from Koin and call it, so the
                // type has to resolve on their compile classpath too.
                api(libs.ktor.client.core)
                implementation(libs.ktor.client.contentNegotiation)
                implementation(libs.ktor.serialization.json)
                implementation(libs.kotlinx.serialization.json)

                api(project.dependencies.platform(libs.koin.bom))
                api(libs.koin.core)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.ktor.client.okhttp)
            }
        }

        iosMain {
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
    }
}
