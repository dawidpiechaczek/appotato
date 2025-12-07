plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
}

gradlePlugin {
    plugins {
        register("androidLibrary") {
            id = "android.library"
            implementationClass = "AndroidLibraryPlugin"
        }
        register("apiLibrary") {
            id = "api.library"
            implementationClass = "ApiLibraryPlugin"
        }
        register("fakeLibrary") {
            id = "fake.library"
            implementationClass = "FakeLibraryPlugin"
        }
        register("detektLibrary") {
            id = "detekt.library"
            implementationClass = "LintDetektPlugin"
        }
        register("koverLibrary") {
            id = "kover.library"
            implementationClass = "KoverPlugin"
        }
    }
}

dependencies {
    implementation(libs.detekt.gradle.plugin)
    implementation(libs.gradle)
    implementation(libs.kover)
    implementation(libs.kotlin.gradle.plugin)
}
