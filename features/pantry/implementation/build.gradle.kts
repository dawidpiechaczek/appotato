plugins {
    id("android.library")
    id("detekt.library")
    id("kover.library")
}

setKoverMinLineCoverage(100)
setKoverMinInstructionCoverage(100)

kover {
    reports {
        filters {
            excludes {
                // Composables, Koin wiring, the system clock and the Room-backed repository. Each
                // needs a real device, graph or database to exercise; the reducer and the mappers
                // below them are where the logic actually is.
                classes(
                    "com.appotato.features.pantry.implementation.PantryScreenKt*",
                    "com.appotato.features.pantry.implementation.ComposableSingletons*",
                    "com.appotato.features.pantry.implementation.PantryModuleKt*",
                    "com.appotato.features.pantry.implementation.SystemToday",
                    "com.appotato.features.pantry.implementation.data.RoomPantryRepository"
                )
            }
        }
    }
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.androidx.lifecycle.viewmodel)
                implementation(libs.androidx.lifecycle.runtimeCompose)

                implementation(projects.shared.uiComponents)
                implementation(projects.shared.dispatchers)
                implementation(projects.shared.billing.api)
                implementation(projects.shared.database)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)

                api(project.dependencies.platform(libs.koin.bom))
                api(libs.koin.core)
                implementation(libs.koin.core.viewmodel)
                implementation(libs.koin.compose.viewmodel)
            }
        }

        commonTest {
            dependencies {
                implementation(projects.shared.billing.fake)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}
