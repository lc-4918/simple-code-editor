plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

/*
 * Version taken from the git tag rather than written here, so that every
 * release declares its own versionCode and versionName. Two releases carrying
 * the same pair would stop Android from updating one with the other.
 *
 * On a commit that is exactly tagged, which is what the release build runs on,
 * git describe gives the bare tag; anywhere else it gives a descriptive name
 * such as v1.0.0-3-g413b197, which is what a development build wants.
 *
 * Whether the tree is clean is deliberately left out. It says nothing the file
 * name needs, and a checkout that looks modified for reasons of its own, which
 * is what happens on a runner, would name the release after a state instead of
 * after its tag.
 *
 * Read through the provider so that the configuration cache knows about it: a
 * process started straight from the build script would not be recorded and the
 * cache would hand back a stale version.
 */
val gitVersion: String = runCatching {
    providers.exec {
        commandLine("git", "describe", "--tags", "--always")
    }.standardOutput.asText.get().trim()
}.getOrNull()?.ifBlank { null } ?: "v0.0.0"

private val semver = Regex("""^v?(\d+)\.(\d+)\.(\d+)""").find(gitVersion)

/** Same shape as the tag: major, minor and patch each keep their own decades. */
val appVersionCode: Int = semver?.destructured?.let { (major, minor, patch) ->
    major.toInt() * 10_000 + minor.toInt() * 100 + patch.toInt()
} ?: 1

android {
    namespace = "fr.lc4918.simplecodeeditor"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "fr.lc4918.simplecodeeditor"
        minSdk = 24
        targetSdk = 37
        versionCode = appVersionCode.coerceAtLeast(1)
        versionName = gitVersion.removePrefix("v")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Release signing is driven by environment variables, which only the
    // release job sets. Built anywhere else, the release stays unsigned rather
    // than failing for want of a key.
    val keystorePath: String? = System.getenv("KEYSTORE_PATH")
    if (keystorePath != null) {
        signingConfigs {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
            if (keystorePath != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            // A different application id from the release, which is signed with
            // another key: without it, installing one over the other fails on
            // the signature rather than updating.
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    androidResources {
        generateLocaleConfig = true
    }
}

/** Names the built file after the version, which is what the release publishes. */
androidComponents {
    onVariants { variant ->
        val name = if (variant.buildType == "release") {
            "simple-code-$gitVersion"
        } else {
            "simple-code-debug-$gitVersion"
        }
        variant.outputs.forEach { output ->
            (output as com.android.build.api.variant.impl.VariantOutputImpl)
                .outputFileName.set("$name.apk")
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.compose.material.icons.extended)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}