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
                // Composables and Koin wiring. Covering these needs a Compose UI test and a running
                // graph, which would assert that Compose and Koin work rather than that the paywall
                // does — the reducer below it is where the logic lives.
                classes(
                    "com.appotato.features.paywall.implementation.PaywallScreenKt*",
                    "com.appotato.features.paywall.implementation.PaywallModuleKt*",
                    // Holder the Compose compiler generates for the screen's lambdas.
                    "com.appotato.features.paywall.implementation.ComposableSingletons*"
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
                implementation(libs.kotlinx.coroutines.core)

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
