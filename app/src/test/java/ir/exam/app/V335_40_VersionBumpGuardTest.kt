package ir.exam.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V335.40 — نگهبان «شمارهٔ نسخه پیش از push».
 *
 * درس V335.39: نسخهٔ فقط‌سایتی به `main` رفت و شمارهٔ نسخه بالا نرفت؛ گام
 * «بررسی نسخه» در `.github/workflows/android.yml` با پیام
 * «این نسخه قبلاً منتشر شده است» شکست و هیچ APK تازه‌ای منتشر نشد.
 *
 * این تست تضمین می‌کند ابزار `scripts/check_version_bump.py` بماند، در
 * `preflight.py` صدا زده شود و نسخهٔ `text/APP_VERSION.txt` با خط اول
 * `text/CHANGELOG_FA.txt` هم‌خوان باشد.
 */
class V335_40_VersionBumpGuardTest {

    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: error("ریشهٔ مخزن پیدا نشد")
    }

    private fun source(relative: String): String = File(root(), relative).readText(Charsets.UTF_8)

    private val guard = source("scripts/check_version_bump.py")
    private val preflight = source("scripts/preflight.py")
    private val version = source("text/APP_VERSION.txt").trim()
    private val changelogFirst = source("text/CHANGELOG_FA.txt").lineSequence().first()

    @Test
    fun `guard script exists and covers app changes`() {
        assertTrue("نام فایل نسخه", "text/APP_VERSION.txt" in guard)
        assertTrue("قالب نسخه در ابزار", "VERSION_RE" in guard)
        assertTrue("دسته‌های فقط‌سایتی", "site/" in guard && "docs/" in guard)
        assertTrue("خطای نسخهٔ تکراری", "قبلاً منتشر شده" in guard)
    }

    @Test
    fun `preflight runs the guard`() {
        assertTrue("فراخوانی در preflight", "scripts/check_version_bump.py" in preflight)
    }

    @Test
    fun `app version matches changelog first line`() {
        assertTrue("خط اول CHANGELOG باید نسخهٔ $version را داشته باشد: $changelogFirst", version in changelogFirst)
        assertTrue("قالب نسخه", Regex("""^\d{1,2}\.\d{2}\.\d{2}$""").matches(version))
    }
}
