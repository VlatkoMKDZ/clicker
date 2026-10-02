plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "dev.tapsentry"
    compileSdk = 35
    defaultConfig {
        applicationId = "dev.tapsentry"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    val signingPropertyNames = listOf(
        "TAPSENTRY_KEYSTORE_PATH",
        "TAPSENTRY_KEYSTORE_PASSWORD",
        "TAPSENTRY_KEY_ALIAS",
        "TAPSENTRY_KEY_PASSWORD",
    )
    fun signingValue(name: String): String? = providers.gradleProperty(name)
        .orElse(providers.environmentVariable(name))
        .orNull
        ?.takeIf(String::isNotBlank)

    val releaseSigning = if (signingPropertyNames.all { signingValue(it) != null }) {
        signingConfigs.create("release") {
            storeFile = file(signingValue("TAPSENTRY_KEYSTORE_PATH")!!)
            storePassword = signingValue("TAPSENTRY_KEYSTORE_PASSWORD")
            keyAlias = signingValue("TAPSENTRY_KEY_ALIAS")
            keyPassword = signingValue("TAPSENTRY_KEY_PASSWORD")
        }
    } else {
        null
    }
    buildTypes {
        debug { applicationIdSuffix = ".debug"; versionNameSuffix = "-debug" }
        release {
            isMinifyEnabled = false
            // A locally installable release remains available without committing secrets.
            // Distribution builds should provide all four TAPSENTRY_* values.
            signingConfig = releaseSigning ?: signingConfigs.getByName("debug")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-service:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}
