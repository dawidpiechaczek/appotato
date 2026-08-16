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
                // Generated compose-resources accessors: a table of string ids, not behaviour.
                packages("com.appotato.features.pantry.implementation.generated.resources")
                // Composables, Koin wiring, the system clock and the Room-backed repository. Each
                // needs a real device, graph or database to exercise; the reducer and the mappers
                // below them are where the logic actually is.
                classes(
                    "com.appotato.features.pantry.implementation.PantryScreenKt*",
                    "com.appotato.features.pantry.implementation.ScannerScreenKt*",
                    "com.appotato.features.pantry.implementation.AddItemSheetKt*",
                    "com.appotato.features.pantry.implementation.PantryCardKt*",
                    "com.appotato.features.pantry.implementation.ComposableSingletons*",
                    "com.appotato.features.pantry.implementation.PantryModuleKt*",
                    "com.appotato.features.pantry.implementation.SystemToday",
                    "com.appotato.features.pantry.implementation.data.RoomPantryRepository"
                )
            }
        }
    }
}

compose.resources {
    // Generated accessors stay internal — strings are an implementation detail of the feature.
    publicResClass = false
    packageOfResClass = "com.appotato.features.pantry.implementation.generated.resources"
    generateResClass = always
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.androidx.lifecycle.viewmodel)
                implementation(libs.androidx.lifecycle.runtimeCompose)

                implementation(compose.components.resources)
                implementation(projects.shared.uiComponents)
                implementation(projects.shared.dispatchers)
                implementation(projects.shared.billing.api)
                implementation(projects.shared.productLookup.api)
                implementation(projects.shared.ingredients)
                implementation(projects.shared.database)
                implementation(projects.shared.barcodeScanner)
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
                implementation(projects.shared.productLookup.fake)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}
