import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import io.gitlab.arturbosch.detekt.DetektPlugin
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType

class LintDetektPlugin : Plugin<Project> {
    val Project.libs: VersionCatalog
        get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

    override fun apply(target: Project) = with(target) {
        apply<DetektPlugin>()

        extensions.configure<DetektExtension> {
            config.from("$rootDir/config/detekt/detekt.yml")
            source.setFrom("src")
        }

        tasks.withType<Detekt>().configureEach {
            jvmTarget = "21"
        }
        tasks.withType<DetektCreateBaselineTask>().configureEach {
            jvmTarget = "21"
        }

        dependencies {
            add("detektPlugins", libs.findLibrary("detekt.formatting").get())
        }

    }

}
