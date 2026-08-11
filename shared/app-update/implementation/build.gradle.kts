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
                // Koin wiring plus a PackageManager/NSBundle lookup — it needs a real app to run
                // at all, so a unit test here would only assert that Koin still works.
                classes("com.appotato.shared.app.update.implementation.AppUpdateModule*Kt")
            }
        }
    }
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.shared.appUpdate.api)
                implementation(projects.shared.remoteConfig.api)

                api(project.dependencies.platform(libs.koin.bom))
                api(libs.koin.core)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.koin.android)
            }
        }

        commonTest {
            dependencies {
                implementation(projects.shared.remoteConfig.fake)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}
