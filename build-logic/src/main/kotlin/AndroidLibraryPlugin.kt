import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class AndroidLibraryPlugin : Plugin<Project> {
    val Project.libs: VersionCatalog
        get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

    // Derived from the full Gradle path, not the name: sibling modules are commonly called
    // "implementation"/"api", so a name-only value collides across features.
    // ":shared:ui-components" -> "shared.ui.components"
    private val Project.modulePackageSuffix: String
        get() = path.removePrefix(":").replace(":", ".").replace("-", ".")

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
                            jvmTarget = "23"
                        }
                    }
                }

                // Targets only — deliberately no `binaries.framework`. Xcode links exactly one
                // framework, ComposeApp, and it statically embeds every klib below it. Giving each
                // module its own produced 8 more link tasks per module (debug/release × arm64,
                // x64, simulatorArm64, fat) that nothing ever consumed, and Kotlin/Native release
                // linking is the slowest step in the build.
                // No iosX64: that architecture is only the simulator on Intel Macs, and it was a
                // third of every iOS compile in the build. Restore it here, in ApiLibraryPlugin,
                // FakeLibraryPlugin and :composeApp if the project ever has to build there.
                iosArm64()
                iosSimulatorArm64()

                sourceSets.commonMain.dependencies {
                    implementation(libs.findLibrary("kotlin.stdlib").get())
                    implementation(libs.findLibrary("compose.material").get())
                }

                sourceSets.commonTest.dependencies {
                    implementation(libs.findLibrary("kotlin.test").get())
                }
            }

            extensions.configure<LibraryExtension> {
                namespace = "com.appotato.$modulePackageSuffix"
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
