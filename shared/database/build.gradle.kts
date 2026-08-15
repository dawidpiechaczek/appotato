plugins {
    id("android.library")
    id("detekt.library")
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

room {
    // Exported schemas are the input to every future migration test — keep them in version control.
    schemaDirectory("$projectDir/schemas")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(libs.room.runtime)
                api(libs.sqlite.bundled)
                api(libs.kotlinx.datetime)
                implementation(libs.kotlinx.coroutines.core)
                implementation(projects.shared.dispatchers)

                api(project.dependencies.platform(libs.koin.bom))
                api(libs.koin.core)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.koin.android)
            }
        }
    }
}

// KSP has no multiplatform configuration — the processor has to be added per target by hand, and a
// target missing from this list compiles until Room's generated code is referenced. This list has to
// match the targets the convention plugin declares, or Gradle fails with
// `Configuration with name 'kspIosX64' not found`.
dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
}
