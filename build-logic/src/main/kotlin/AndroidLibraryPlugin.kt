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
            plugins.apply("org.jetbrains.compose")
            plugins.apply("com.android.library")
            plugins.apply("org.jetbrains.kotlin.plugin.serialization")

            extensions.configure<KotlinMultiplatformExtension> {
                androidTarget {
                    compilations.all {
                        kotlinOptions {
                            jvmTarget = JvmTarget.JVM_21.target
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
                        baseName = "${project.name.replace("-", ".")}Kit"
                    }
                }

                sourceSets.commonMain.dependencies {
                    implementation(libs.findLibrary("kotlin.stdlib").get())
                    implementation(libs.findLibrary("compose.material").get())
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
                    sourceCompatibility = JavaVersion.VERSION_21
                    targetCompatibility = JavaVersion.VERSION_21
                }
            }
        }
    }
}
