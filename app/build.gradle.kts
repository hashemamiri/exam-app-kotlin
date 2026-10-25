import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("androidx.baselineprofile") // V215 — مصرف پروفایل اندازه‌گیری‌شده (تولید فقط در گردش کار دستی)
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

// V232 — پلاگین google-services فقط وقتی فایل google-services.json هست (در CI از Secret GOOGLE_SERVICES_JSON_B64 ساخته می‌شود؛
// بدون آن، اپ کامپایل می‌شود ولی اعلان‌ها خاموش می‌مانند).
val hasGoogleServices = file("google-services.json").exists()
if (hasGoogleServices) apply(plugin = "com.google.gms.google-services")
// V232.3 — بستهٔ debug پسوند «.native» دارد؛ اگر آن نام در google-services.json ثبت نشده باشد، پردازش فایل برای آن variant
// خاموش می‌شود و PUSH_ENABLED همان variant false است (به‌جای شکست build با «No matching client found»).
val googleServicesPackages: Set<String> = if (hasGoogleServices)
    Regex("\"package_name\"\\s*:\\s*\"([^\"]+)\"").findAll(file("google-services.json").readText()).map { it.groupValues[1] }.toSet() else emptySet()
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}
// V298 — نسخهٔ آزمایشی (شاخهٔ staging): APP_BETA=1 در local.properties → بستهٔ «ir.exam.app.beta» کنار اپ اصلی نصب می‌شود،
// نام «آزمون (آزمایشی)»، پسوند نسخه «-beta»، بدون به‌روزرسانی خودکار. اعلان‌ها فقط اگر آن بسته در google-services.json باشد.
val isBetaBuild = localProperties.getProperty("APP_BETA", "0").trim() == "1"
val pushEnabledRelease = (if (isBetaBuild) "ir.exam.app.beta" else "ir.exam.app") in googleServicesPackages
val pushEnabledDebug = "ir.exam.app.native" in googleServicesPackages
val supabaseUrl = localProperties.getProperty("SUPABASE_URL", "")
val supabaseAnonKey = localProperties.getProperty("SUPABASE_ANON_KEY", "")
val googleWebClientId = localProperties.getProperty("GOOGLE_WEB_CLIENT_ID", "")
// V335.20 — فهرست مسیرهای جایگزین Supabase (جدا با ویرگول). خالی = پل پیش‌فرض در SupabaseRouter.DEFAULT_RELAY
val supabaseRelayUrls = localProperties.getProperty("SUPABASE_RELAY_URLS", "").trim().replace("\\", "").replace("\"", "")
val appVersionCode = localProperties.getProperty("APP_VERSION_CODE")
    ?.toIntOrNull()
    ?.takeIf { it > 0 }
    ?: 3
val appVersionName = localProperties.getProperty("APP_VERSION_NAME")
    ?.trim()
    ?.takeIf { it.isNotEmpty() }
    ?: rootProject.file("text/APP_VERSION.txt").takeIf { it.exists() }?.readText()?.trim()?.takeIf { it.isNotEmpty() } ?: "1.01.01"

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
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" // V276 — تست UI روی شبیه‌ساز (گردش کار دستی app-ui-test.yml)
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
        buildConfigField("String", "SUPABASE_RELAY_URLS", "\"$supabaseRelayUrls\"") // V335.20
        buildConfigField("Boolean", "PUSH_ENABLED", "false") // V232 — در buildTypes بر اساس google-services.json مقدار می‌گیرد
        buildConfigField("Boolean", "IS_BETA", "$isBetaBuild") // V298
        resValue("string", "app_label", if (isBetaBuild) "آزمون (آزمایشی)" else "آزمون آنلاین") // V298
        if (isBetaBuild) { applicationIdSuffix = ".beta"; versionNameSuffix = "-beta" } // V298

        // V78.2 — کتابخانهٔ بومیِ OCR برای هر چهار ABI ساخته می‌شود و ~۱۲٫۲MB
        // به APK اضافه می‌کرد. گوشی‌های واقعی همگی ARM هستند؛ x86/x86_64 فقط
        // برای شبیه‌سازِ کامپیوتری لازم است. حذفشان ~۶٫۴MB صرفه‌جویی می‌کند
        // بدون هیچ اثری روی دستگاه‌های واقعی.
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
            // V215 — فقط هنگام تولید Baseline Profile روی شبیه‌ساز x86_64 (گردش کار baseline-profile.yml)
            if (project.hasProperty("baselineAbi")) abiFilters += project.property("baselineAbi").toString()
        }
    }

    // V276 — شبیه‌ساز مدیریت‌شدهٔ Gradle برای تست UI (فقط گردش کار دستی؛ در ساخت عادی نقشی ندارد)
    testOptions.managedDevices.devices {
        create<com.android.build.api.dsl.ManagedVirtualDevice>("uiPixel6Api34") {
            device = "Pixel 6"
            apiLevel = 34
            systemImageSource = "aosp"
        }
    }

    packaging { resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}") } // V276 — تداخل مجوزها در APK تست

    androidResources {
        // V76.9 — دادهٔ زبانِ OCR نباید فشرده شود (کپیِ سالم از assets).
        noCompress += "traineddata"
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
        getByName("debug") {
            applicationIdSuffix = ".native"
            buildConfigField("Boolean", "PUSH_ENABLED", "$pushEnabledDebug") // V232.3
        }
        getByName("release") {
            buildConfigField("Boolean", "PUSH_ENABLED", "$pushEnabledRelease") // V232.3
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

// V215 — Baseline Profile: هرگز در ساخت عادی تولید نمی‌شود؛ فقط `generateReleaseBaselineProfile` در گردش کار دستی
baselineProfile {
    automaticGenerationDuringBuild = false
    saveInSrc = true
    mergeIntoMain = false
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.06.01")) // V215 — Compose 1.8.3 / Material3 1.3.2 (compileSdk 35، بدون نیاز به AGP 9)
    implementation("androidx.core:core-ktx:1.15.0")
    // V212 — نصب Baseline Profile روی دستگاه‌های نصبِ مستقیم (بدون Play) برای شروع و پیمایش سریع‌تر
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")
    // V215 — پروفایل اندازه‌گیری‌شده از ماژول baselineprofile (در صورت وجود app/src/release/generated/baselineProfiles)
    "baselineProfile"(project(":baselineprofile"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.datastore:datastore-preferences:1.1.2")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    // V76.9 — OCR فارسیِ آفلاین (Tesseract 4). فقط روی JitPack منتشر شده است؛
    // دادهٔ زبان در app/src/main/assets/tessdata/fas.traineddata است.
    implementation("com.github.adaptech-cz.Tesseract4Android:tesseract4android:4.8.0")
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
    implementation("io.github.jan-tennert.supabase:auth-kt:3.1.4")
    implementation("io.github.jan-tennert.supabase:postgrest-kt:3.1.4")
    implementation("io.github.jan-tennert.supabase:storage-kt:3.1.4")
    implementation("io.github.jan-tennert.supabase:functions-kt:3.1.4")
    // V60.1 — ثبت‌نام/ورود گوگل با Credential Manager مستقیم (مسیر رسمی مستندات
    // Supabase؛ پلاگین قبلی روی برخی دستگاه‌ها callback را گم می‌کرد).
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation(platform("com.google.firebase:firebase-bom:33.7.0")) // V232
    implementation("com.google.firebase:firebase-messaging") // V232 — اعلان‌ها
    implementation("io.ktor:ktor-client-okhttp:3.0.3")
    // V335.27 — وابستگی صریح OkHttp برای حل نام با نشانی تثبیت‌شده (RelayDns)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    testImplementation("junit:junit:4.13.2")
    // V276 — تست UI خودکار (Compose) روی شبیه‌ساز
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.06.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}

// V232.3 — پردازش google-services فقط برای variantهایی که نام بسته‌شان در فایل هست
if (hasGoogleServices) {
    tasks.matching { it.name.startsWith("process") && it.name.endsWith("GoogleServices") }.configureEach {
        val debugTask = name.contains("Debug")
        enabled = if (debugTask) pushEnabledDebug else pushEnabledRelease
    }
}
