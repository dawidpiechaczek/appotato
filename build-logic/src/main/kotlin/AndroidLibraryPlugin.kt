import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class AndroidLibraryPlugin : Plugin<Project> {
    val Project.libs: VersionCatalog
        get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

    override fun apply(target: Project) {
        with(target) {
            plugins.apply("org.jetbrains.kotlin.multiplatform")
            plugins.apply("org.jetbrains.kotlin.plugin.compose")
            plugins.apply("com.android.library")

            extensions.configure<KotlinMultiplatformExtension> {
                androidTarget {
                    compilations.all {
                        kotlinOptions {
                            jvmTarget = JvmTarget.JVM_11.target
                        }
                    }
                }

                val iosFamily = listOf(
                    iosX64(),
                    iosArm64(),
                    iosSimulatorArm64()
                )

                // Optional: Configure all iOS targets in one go
                iosFamily.forEach {
                    it.binaries.framework {
                        baseName = "shared"
                    }
                }

                sourceSets.commonMain.dependencies {
                    implementation(libs.findLibrary("kotlin.stdlib").get())
                }

                sourceSets.commonTest.dependencies {
                    implementation(libs.findLibrary("kotlin.test").get())
                }
            }

            extensions.configure<LibraryExtension> {
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
