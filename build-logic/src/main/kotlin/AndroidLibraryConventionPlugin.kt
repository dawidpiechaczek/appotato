import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class AndroidLibraryConventionPlugin : Plugin<Project> {
    val Project.libs: VersionCatalog
        get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

    override fun apply(target: Project) {
        with(target) {
            // Apply the essential plugins for a KMP library module
            plugins.apply("org.jetbrains.kotlin.multiplatform")
            plugins.apply("org.jetbrains.kotlin.plugin.compose")
            plugins.apply("com.android.library")

            // Configure the Kotlin Multiplatform extension
            extensions.configure<KotlinMultiplatformExtension> {
                // Add the Android library target
                androidTarget {
                    compilations.all {
                        kotlinOptions {
                            jvmTarget = JvmTarget.JVM_11.target
                        }
                    }
                }

                // Example: Add common iOS targets
                // You can customize this list based on your needs
                val iosFamily = listOf(
                    iosX64(),
                    iosArm64(),
                    iosSimulatorArm64()
                )

                // Optional: Configure all iOS targets in one go
                iosFamily.forEach {
                    it.binaries.framework {
                        baseName = "shared" // Change this to your desired framework name
                    }
                }

                // Configure source sets (optional, but good practice)
                sourceSets.commonMain.dependencies {
                    // Add dependencies common to all KMP library modules here if you have any
                    // e.g., implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:...")
                }
            }

            // Configure the Android extension
            extensions.configure<com.android.build.api.dsl.LibraryExtension> {
                namespace = "com.appotato.${project.name.replace("-", ".")}"
                compileSdk = libs.findVersion("android.compileSdk").get().toString().toInt()

                defaultConfig {
                    minSdk = libs.findVersion("android.minSdk").get().toString().toInt()
                }

                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_11
                    targetCompatibility = JavaVersion.VERSION_11
                }
            }
        }
    }
}
