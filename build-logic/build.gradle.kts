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
            implementationClass = "AndroidLibraryConventionPlugin"
        }
    }
}

dependencies {
    // Dependency for the Android Gradle Plugin API (`com.android.library`)
    implementation("com.android.tools.build:gradle:8.1.2") // Use the AGP version you use in your project

    // Dependency for the Kotlin Multiplatform Gradle Plugin API (`org.jetbrains.kotlin.multiplatform`)
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.20") // Use the Kotlin version you use in your project
}
