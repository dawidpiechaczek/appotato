import org.gradle.api.Project
import org.gradle.kotlin.dsl.extra

private const val MIN_LINE_COVERAGE_KEY = "kover.min.line.coverage"
private const val MIN_INSTRUCTION_COVERAGE_KEY = "kover.min.instruction.coverage"

fun Project.getKoverMinLineCoverage(): Int? {
    return if (extra.has(MIN_LINE_COVERAGE_KEY)) {
        extra[MIN_LINE_COVERAGE_KEY] as? Int
    } else {
        null
    }
}

fun Project.setKoverMinLineCoverage(value: Int) {
    extra[MIN_LINE_COVERAGE_KEY] = value
}

fun Project.getKoverMinInstructionCoverage(): Int? {
    return if (extra.has(MIN_INSTRUCTION_COVERAGE_KEY)) {
        extra[MIN_INSTRUCTION_COVERAGE_KEY] as? Int
    } else {
        null
    }
}

fun Project.setKoverMinInstructionCoverage(value: Int) {
    extra[MIN_INSTRUCTION_COVERAGE_KEY] = value
}
