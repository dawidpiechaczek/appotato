plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    id("detekt.library")
    id("kover.library")
    alias(libs.plugins.googleServices)
    alias(libs.plugins.firebaseCrashlytics)
}

kotlin {
    androidTarget()

    // No iosX64 — see AndroidLibraryPlugin for why, and how to put it back.
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            // Swift implements Telemetry, RemoteConfig and AttestationTokens itself, so those
            // contracts have to be visible in the framework.
            export(projects.shared.telemetry.api)
            export(projects.shared.remoteConfig.api)
            export(projects.shared.attestation.api)
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.koin.android)
            implementation(projects.shared.telemetry.implementation)
            implementation(projects.shared.remoteConfig.implementation)
            implementation(projects.shared.attestation.implementation)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(projects.shared.uiComponents)

            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            api(projects.shared.telemetry.api)
            api(projects.shared.remoteConfig.api)
            api(projects.shared.attestation.api)

            implementation(projects.shared.appUpdate.api)
            implementation(projects.shared.appUpdate.implementation)

            implementation(projects.shared.dispatchers)
            implementation(projects.shared.database)
            implementation(projects.shared.network)
            // Only to hand Coil the HttpClient above and place its cache; the image Composables
            // live in ui-components.
            implementation(libs.coil)
            implementation(libs.coil.network)
            implementation(libs.okio)
            implementation(projects.shared.billing.api)
            implementation(projects.shared.billing.implementation)
            implementation(projects.shared.productLookup.implementation)
            implementation(projects.shared.recipeSource.implementation)
            implementation(projects.features.paywall.implementation)
            implementation(projects.features.recipes.implementation)
            implementation(projects.features.pantry.implementation)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.appotato"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.appotato"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
        }
        create("staging") {
            dimension = "environment"
            applicationIdSuffix = ".staging"
        }
        create("prod") {
            dimension = "environment"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

