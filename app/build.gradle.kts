import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}
val supabaseUrl = localProperties.getProperty("SUPABASE_URL", "")
val supabaseAnonKey = localProperties.getProperty("SUPABASE_ANON_KEY", "")
val googleWebClientId = localProperties.getProperty("GOOGLE_WEB_CLIENT_ID", "")
val appVersionCode = localProperties.getProperty("APP_VERSION_CODE")
    ?.toIntOrNull()
    ?.takeIf { it > 0 }
    ?: 3
val appVersionName = localProperties.getProperty("APP_VERSION_NAME")
    ?.trim()
    ?.takeIf { it.isNotEmpty() }
    ?: "1.1.1-native"

val signingProperties = Properties()
val signingPropertiesFile = rootProject.file("app/keystore.properties")
if (signingPropertiesFile.exists()) {
    signingPropertiesFile.inputStream().use { signingProperties.load(it) }
}

android {
    namespace = "ir.exam.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "ir.exam.app"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
    }

    signingConfigs {
        create("release") {
            val storeFileValue = signingProperties.getProperty("storeFile", "")
            if (storeFileValue.isNotBlank()) {
                storeFile = file(storeFileValue)
                storePassword = signingProperties.getProperty("storePassword")
                keyAlias = signingProperties.getProperty("keyAlias")
                keyPassword = signingProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        getByName("debug") { applicationIdSuffix = ".native" }
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.datastore:datastore-preferences:1.1.2")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-svg:2.7.0")
    // V53.1 — همان AndroidSVG بسته‌بندی‌شدهٔ coil-svg، به‌صورت صریح برای رندر
    // برداری شکل/جدول در PDF (OfficialPdfPrintAdapter) بدون WebView.
    implementation("com.caverock:androidsvg-aar:1.4")
    // V70.0 — خروجی PDF مستقیم (فایل، بدون پنجرهٔ چاپ) با openPDF: فورک آزاد
    // iText 5 (همان کتابخانهٔ اپ قدیمی؛ LGPL/MPL — برخلاف iText 5 اصلی که AGPL
    // است). DirectPdfExporter از همین کتابخانه استفاده می‌کند.
    implementation("com.github.librepdf:openpdf:1.3.43")
    implementation("io.github.jan-tennert.supabase:auth-kt:3.1.4")
    implementation("io.github.jan-tennert.supabase:postgrest-kt:3.1.4")
    implementation("io.github.jan-tennert.supabase:storage-kt:3.1.4")
    implementation("io.github.jan-tennert.supabase:functions-kt:3.1.4")
    // V60.1 — ثبت‌نام/ورود گوگل با Credential Manager مستقیم (مسیر رسمی مستندات
    // Supabase؛ پلاگین قبلی روی برخی دستگاه‌ها callback را گم می‌کرد).
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("io.ktor:ktor-client-okhttp:3.0.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}
