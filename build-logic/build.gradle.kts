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
    }
}

dependencies {
    implementation(libs.gradle)
    implementation(libs.kotlin.gradle.plugin)
}
