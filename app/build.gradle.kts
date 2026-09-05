import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

/*
 * The Google Services plugin hard-fails the build when google-services.json is
 * missing, which would make the project unbuildable for anyone who has not set
 * up Firebase yet. Applying it conditionally keeps the app compiling and
 * running without Firebase; drop the file into app/ and sign-in activates with
 * no code change. AuthAvailability performs the matching check at runtime.
 */
val googleServicesConfig = project.file("google-services.json")
if (googleServicesConfig.exists()) {
    apply(plugin = "com.google.gms.google-services")
} else {
    logger.lifecycle(
        "FlyDrop: app/google-services.json not found - Google Sign-In will be " +
            "inactive. Add the file from your Firebase project to enable it.",
    )
}

/*
 * Release signing is read from keystore.properties, which is gitignored along
 * with the keystore itself. When it is absent the release build simply stays
 * unsigned rather than failing, so a fresh clone still builds.
 */
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val hasReleaseSigning = keystoreProperties.getProperty("storeFile")
    ?.let { rootProject.file(it).exists() } == true

android {
    namespace = "com.flydrop.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.flydrop.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 8
        versionName = "1.0.7"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // About > Source code and Report a bug open these. Kept here rather
        // than in a composable so moving the repository is a one-line change.
        buildConfigField("String", "SOURCE_URL", "\"https://github.com/vabxsen/flydrop\"")
        buildConfigField(
            "String",
            "RELEASES_API_URL",
            "\"https://api.github.com/repos/vabxsen/flydrop/releases/latest\"",
        )
        // Where an invited contact is sent to get the app. The releases page
        // rather than the API, because a person opens this one in a browser.
        buildConfigField(
            "String",
            "DOWNLOAD_URL",
            "\"https://github.com/vabxsen/flydrop/releases/latest\"",
        )
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        // About > Version reads the version name and code from BuildConfig.
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    testImplementation(libs.junit)
    // Unit tests run against the JVM, where android.jar's org.json is a stub
    // that throws. This is the real implementation, for tests only.
    testImplementation(libs.json)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
}
