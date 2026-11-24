import groovy.json.JsonSlurper

plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
val appVersion = (JsonSlurper().parse(file("../../package.json")) as Map<*, *>)["version"].toString()
val numbers = appVersion.split('.').map(String::toInt)
val signingFile = System.getenv("FLUSIVA_ANDROID_KEYSTORE")
android {
    namespace = "info.cosmonet.flusiva"
    compileSdk = 36
    buildToolsVersion = "36.0.0"
    ndkVersion = "27.0.12077973"
    defaultConfig {
        applicationId = "info.cosmonet.flusiva"
        minSdk = 29
        targetSdk = 36
        versionCode = numbers[0] * 1000000 + numbers[1] * 1000 + numbers[2]
        versionName = appVersion
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64") }
    }
    signingConfigs {
        if (!signingFile.isNullOrBlank()) create("distribution") {
            storeFile = file(signingFile)
            storePassword = System.getenv("FLUSIVA_ANDROID_STORE_PASSWORD")
            keyAlias = "flusiva"
            keyPassword = System.getenv("FLUSIVA_ANDROID_KEY_PASSWORD")
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            if (!signingFile.isNullOrBlank()) signingConfig = signingConfigs.getByName("distribution")
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { buildConfig = true }
    packaging {
        jniLibs.useLegacyPackaging = true
        jniLibs.keepDebugSymbols += "**/*.zip.so"
        resources.excludes += listOf("META-INF/DEPENDENCIES", "META-INF/INDEX.LIST")
    }
    sourceSets["main"].assets.srcDirs("src/main/assets", "../../vendor-android")
    sourceSets["main"].jniLibs.srcDir("../../vendor-android-jni")
}
dependencies {
    implementation("androidx.media3:media3-exoplayer:1.11.1")
    implementation("androidx.media3:media3-session:1.11.1")
    implementation("io.github.junkfood02.youtubedl-android:library:0.18.1")
    implementation("io.github.junkfood02.youtubedl-android:ffmpeg:0.18.1")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.4")
    implementation("androidx.activity:activity-ktx:1.11.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
}
