import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class ApiLibraryPlugin : Plugin<Project> {
    val Project.libs: VersionCatalog
        get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

    override fun apply(target: Project) {
        with(target) {
            plugins.apply("org.jetbrains.kotlin.multiplatform")

            extensions.configure<KotlinMultiplatformExtension> {
                // No iosX64: that architecture is only the simulator on Intel Macs. Restore it here
                // and in AndroidLibraryPlugin and :composeApp if the project ever has to build there.
                iosSimulatorArm64()
                iosArm64()
                jvm()
            }
        }
    }
}
