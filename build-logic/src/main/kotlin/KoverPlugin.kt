import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType

class KoverPlugin : Plugin<Project> {
    val Project.libs: VersionCatalog
        get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

    override fun apply(target: Project) = with(target) {
        apply(plugin = libs.findPlugin("kover").get().get().pluginId)
        afterEvaluate {
            configure<KoverProjectExtension> {
                reports {
                    filters {
                        excludes {
                            androidGeneratedClasses()
                        }
                    }
                    verify {
                        val minLineCoverage = project.getKoverMinLineCoverage() ?: 0
                        val minInstructionCoverage = project.getKoverMinInstructionCoverage() ?: 0

                        rule("Minimum code coverage") {
                            bound {
                                minValue.set(minLineCoverage)
                                coverageUnits.set(CoverageUnit.LINE)
                            }
                            bound {
                                minValue.set(minInstructionCoverage)
                                coverageUnits.set(CoverageUnit.INSTRUCTION)
                            }
                        }
                    }
                }
            }
        }
    }

}