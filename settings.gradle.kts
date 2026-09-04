pluginManagement { repositories { maven { url = uri("https://dl.google.com/android/maven2/") }; mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://dl.google.com/android/maven2/") }
        mavenCentral()
        // V76.9 — tesseract4android (موتور OCR فارسی) فقط روی JitPack منتشر می‌شود
        // و در Maven Central نیست؛ دامنه محدود شده تا بقیهٔ وابستگی‌ها از این
        // مخزن کشیده نشوند.
        maven {
            url = uri("https://jitpack.io")
            content { includeGroupByRegex("com\\.github\\.adaptech-cz.*") }
        }
    }
}
rootProject.name = "ExamAppNative"
include(":app")
