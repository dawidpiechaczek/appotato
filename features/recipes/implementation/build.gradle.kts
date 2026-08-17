plugins {
    id("android.library")
    id("detekt.library")
    id("kover.library")
}

setKoverMinLineCoverage(90)
setKoverMinInstructionCoverage(90)

kover {
    reports {
        filters {
            excludes {
                packages("com.appotato.features.recipes.implementation.generated.resources")
                // Composables, Koin wiring, the system clock and the Room-backed repository: each
                // needs a real device, graph or database to exercise. The reducer below them is
                // where the logic is, and that is covered.
                classes(
                    "com.appotato.features.recipes.implementation.RecipesScreenKt*",
                    "com.appotato.features.recipes.implementation.RecipeCardKt*",
                    "com.appotato.features.recipes.implementation.ComposableSingletons*",
                    "com.appotato.features.recipes.implementation.RecipesModuleKt*",
                    "com.appotato.features.recipes.implementation.SystemToday",
                    "com.appotato.features.recipes.implementation.data.RoomExpiringItems"
                )
            }
        }
    }
}

compose.resources {
    publicResClass = false
    packageOfResClass = "com.appotato.features.recipes.implementation.generated.resources"
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
                implementation(projects.shared.database)
                implementation(projects.shared.recipeSource.api)
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
                implementation(projects.shared.recipeSource.fake)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}
