package ir.exam.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V333.4 — ورک‌فلوی «تست UI اپ روی شبیه‌ساز» (.github/workflows/app-ui-test.yml)
 * طبق دستور صریح مالک محصول فقط به‌صورت دستی (workflow_dispatch) اجرا می‌شود
 * و دیگر با هیچ push یا pull_request به‌طور خودکار اجرا نمی‌شود.
 */
class V333_4_ManualOnlyEmulatorWorkflowTest {

    private fun repoRoot(): File {
        val candidates = listOf(File("."), File(".."))
        return candidates.first { File(it, "settings.gradle.kts").isFile }
    }

    @Test
    fun emulatorUiWorkflowIsStrictlyManualOnly() {
        val root = repoRoot()
        val appUiYml = File(root, ".github/workflows/app-ui-test.yml").readText()

        assertTrue("workflow_dispatch:" in appUiYml)
        assertFalse("app-ui-test.yml must not auto-run on push", "\n  push:" in appUiYml)
        assertFalse("app-ui-test.yml must not auto-run on pull_request", "pull_request:" in appUiYml)
        assertFalse("app-ui-test.yml must not auto-run on schedule", "schedule:" in appUiYml)
        assertFalse("app-ui-test.yml must not auto-run on workflow_run", "workflow_run:" in appUiYml)

        // پین‌های سازگاری اجرای دستی ایزوله و تست‌های قبلی حفظ شده‌اند
        assertTrue(":app:uiPixel6Api34DebugAndroidTest" in appUiYml)
        assertTrue("scripts/mock_supabase_server.py" in appUiYml)
        assertTrue("UI_TEST_TEACHER_PASS" in appUiYml)
        assertTrue("add-mask" in appUiYml)
    }

    @Test
    fun androidReleaseWorkflowIgnoresManualEmulatorWorkflowEdits() {
        val root = repoRoot()
        val androidYml = File(root, ".github/workflows/android.yml").readText()
        assertTrue("!.github/workflows/app-ui-test.yml" in androidYml)
    }
}
