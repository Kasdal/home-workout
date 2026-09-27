import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
}

// Deterministic versionCode derived from the SemVer version that Axion resolves
// from the nearest v* tag (MAJOR*10000 + MINOR*100 + PATCH). Every build from the
// same tag gets the same code, codes only grow when the tag does (including
// hotfix branches), and no git subprocess runs at configuration time.
// Constraint: MINOR and PATCH must stay below 100 within a release line.
fun getVersionCode(version: String): Int {
    val match = Regex("""(\d+)\.(\d+)\.(\d+)""").find(version)
    return if (match != null) {
        val (major, minor, patch) = match.destructured
        major.toInt() * 10000 + minor.toInt() * 100 + patch.toInt()
    } else {
        1
    }
}

val appVersion = rootProject.version.toString()

// Release signing material is never committed. Values come from, in order:
//   1. Gradle properties passed on the command line (-PstoreFile=...)
//   2. Environment variables (used by CI)
//   3. keystore/signing.properties, which is gitignored (used for local builds)
// If none resolve, only the debug build type is signed and release signing
// fails loudly instead of silently shipping an unsigned APK.
val signingProps = Properties().apply {
    val local = rootProject.file("keystore/signing.properties")
    if (local.exists()) local.inputStream().use { load(it) }
}
fun signingValue(vararg keys: String): String? =
    keys.firstNotNullOfOrNull { key ->
        (project.findProperty(key) as String?)?.takeIf { it.isNotBlank() }
            ?: System.getenv(key)?.takeIf { it.isNotBlank() }
            ?: signingProps.getProperty(key)?.takeIf { it.isNotBlank() }
    }

// storeFile may be given as a bare filename or as a path relative to the repo
// root. Try the literal value first, then the keystore/ directory, so a bare
// "release-v2.jks" resolves the way a human expects it to.
val releaseStoreFile = signingValue("storeFile", "KEYSTORE_FILE")?.let { raw ->
    val direct = rootProject.file(raw)
    val underKeystore = rootProject.file("keystore/$raw")
    when {
        direct.isFile -> direct
        underKeystore.isFile -> underKeystore
        else -> direct
    }
}
val releaseStorePassword = signingValue("storePassword", "KEYSTORE_PASSWORD")
val releaseKeyPassword = signingValue("keyPassword", "KEY_PASSWORD")
val releaseKeyAlias = signingValue("keyAlias", "KEY_ALIAS") ?: "workoutapp"
val signingConfigured = releaseStoreFile != null &&
    !releaseStorePassword.isNullOrBlank() &&
    !releaseKeyPassword.isNullOrBlank()

android {
    namespace = "com.example.workoutapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.workoutapp"
        minSdk = 26
        targetSdk = 34
        versionCode = getVersionCode(appVersion)
        versionName = appVersion

        buildConfigField("String", "GITHUB_REPO_OWNER", "\"Kasdal\"")
        buildConfigField("String", "GITHUB_REPO_NAME", "\"home-workout\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        // The keystore lives outside version control. Local builds read
        // keystore/signing.properties (gitignored). CI supplies the same values
        // as KEYSTORE_FILE / KEYSTORE_PASSWORD / KEY_PASSWORD env vars, which the
        // release workflow base64-decodes into a temporary file.
        if (signingConfigured) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            // Debug builds sign with a local debug key so they can never collide
            // with, or silently overwrite, a real release install.
            if (signingConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (signingConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation(platform("com.google.firebase:firebase-bom:32.8.1"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // Credential Manager (Google ID token sign-in)
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    implementation("androidx.work:work-runtime-ktx:2.9.0")

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    // Provides collectAsStateWithLifecycle, which stops collecting when the
    // screen is not at least STARTED. Without it every screen keeps its Firestore
    // listeners and the 5 Hz sensor poll alive while backgrounded.
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("com.google.android.material:material:1.11.0")
    
    // Hilt
    implementation("com.google.dagger:hilt-android:2.50")
    ksp("com.google.dagger:hilt-android-compiler:2.50")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // Icons
    implementation("androidx.compose.material:material-icons-extended")
    
    // Lottie animations
    implementation("com.airbnb.android:lottie-compose:6.1.0")
    
    // Image loading
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Logging
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Networking for ESP sensor
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("app.cash.turbine:turbine:1.0.0")

androidTestImplementation("androidx.test.ext:junit:1.2.1")
androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

// Unit-test throughput for this machine (16C/24T, 32 GB RAM):
// - 8 parallel worker JVMs (Robolectric is memory-hungry; 8 x ~2 GB fits comfortably)
// - workers recycle after 40 classes so Robolectric sandboxes cannot leak memory forever
// - per-test console logging gives live progress in the detached run log
tasks.withType<Test>().configureEach {
    maxParallelForks = 8
    forkEvery = 40
    maxHeapSize = "2560m"
    testLogging {
        events("passed", "failed", "skipped")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showCauses = true
        showStackTraces = true
    }
}
