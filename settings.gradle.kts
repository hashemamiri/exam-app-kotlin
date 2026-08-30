pluginManagement { repositories { maven { url = uri("https://dl.google.com/android/maven2/") }; mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://dl.google.com/android/maven2/") }
        // iText 7 for Android is distributed from the official iText Android repository.
        maven { url = uri("https://repo.itextsupport.com/android") }
        mavenCentral()
    }
}
rootProject.name = "ExamAppNative"
include(":app")
