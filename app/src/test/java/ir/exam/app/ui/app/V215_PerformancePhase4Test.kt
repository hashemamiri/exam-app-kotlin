package ir.exam.app.ui.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V215 — سرعت فاز ۴: به‌روزرسانی Compose BOM و زیرساخت تولید Baseline Profile اندازه‌گیری‌شده. */
class V215_PerformancePhase4Test {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }
    private fun src(rel: String): String = File(root(), rel).readText()

    @Test
    fun composeBomUpgradedWithoutChangingSdkFloor() {
        val gradle = src("app/build.gradle.kts")
        assertTrue("androidx.compose:compose-bom:2025.06.01" in gradle)
        assertFalse("compose-bom:2024.12.01" in gradle)
        assertTrue("compileSdk = 35" in gradle)
        assertTrue("minSdk = 26" in gradle)
    }

    @Test
    fun baselineProfileModuleOnlyRunsFromManualWorkflow() {
        val gradle = src("app/build.gradle.kts")
        assertTrue("id(\"androidx.baselineprofile\")" in gradle)
        assertTrue("automaticGenerationDuringBuild = false" in gradle)
        assertTrue("\"baselineProfile\"(project(\":baselineprofile\"))" in gradle)
        assertTrue("if (project.hasProperty(\"baselineAbi\")) abiFilters += project.property(\"baselineAbi\").toString()" in gradle)
        assertFalse("\"x86_64\"" in gradle)
        assertTrue("include(\":baselineprofile\")" in src("settings.gradle.kts"))
        assertTrue("id(\"androidx.baselineprofile\") version \"1.3.4\" apply false" in src("build.gradle.kts"))
        val module = src("baselineprofile/build.gradle.kts")
        assertTrue("targetProjectPath = \":app\"" in module)
        assertTrue("useConnectedDevices = false" in module)
        assertTrue("benchmark-macro-junit4:1.3.4" in module)
        val gen = src("baselineprofile/src/main/java/ir/exam/app/baselineprofile/BaselineProfileGenerator.kt")
        assertTrue("includeInStartupProfile = true" in gen)
        assertTrue("packageName = \"ir.exam.app\"" in gen)
        val wf = src(".github/workflows/baseline-profile.yml")
        assertTrue("workflow_dispatch:" in wf)
        assertFalse("push:" in wf)
        assertTrue(":app:generateReleaseBaselineProfile" in wf)
        assertTrue("-PbaselineAbi=x86_64" in wf)
        assertTrue("scripts/bump_app_version.py" in wf)
        assertTrue("gh workflow run android.yml --ref main" in wf)
        // پروفایل دستی فاز ۱ هم‌چنان همراه برنامه است
        assertTrue(File(root(), "app/src/main/baseline-prof.txt").isFile)
    }
}
