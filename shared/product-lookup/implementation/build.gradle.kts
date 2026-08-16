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
                // Koin wiring: a single binding, and exercising it needs a started graph.
                classes("com.appotato.shared.product.lookup.implementation.ProductLookupModuleKt*")
                // The response DTOs. They declare field names and defaults; what counts as
                // uncovered in them is the `copy`/`equals`/`toString`/serializer set the compiler
                // writes, which no test should be shaped around. The mapping over them is where
                // the behaviour is, and that is covered.
                annotatedBy("kotlinx.serialization.Serializable")
            }
        }
    }
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.shared.productLookup.api)
                // api: the tests build the shipping client over `MockEngine` through it.
                api(projects.shared.network)
                implementation(libs.kotlinx.serialization.json)

                api(project.dependencies.platform(libs.koin.bom))
                api(libs.koin.core)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.ktor.client.mock)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}
