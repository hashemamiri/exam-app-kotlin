package ir.exam.app

import ir.exam.app.core.update.ReleaseNotes
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V356.1 — regresیون شکست CI بستهٔ V356.
 *
 * چه اتفاق افتاد: آزمون V355 شمارهٔ نسخه را سخت‌رمز کرده بود
 * («1.03.72»). وقتی V356 نسخه را به 1.03.73 برد، همان آزمون شکست و
 * کل CI قرمز شد — در حالی که ۱۶۳۵ آزمون دیگر و خودِ اپ سالم بودند.
 * ریشه: پین به «یک عدد خاص» به‌جای «هم‌خوانی دو منبع».
 *
 * این کلاس آن ریشه را می‌بندد: نگهبان باید وجود داشته باشد، در CI
 * اجرا شود، و هیچ آزمونی نسخه را سخت‌رمز نکند.
 */
class V356_1_VersionPinRegressionTest {

    private fun root(): File {
        var d = File(System.getProperty("user.dir") ?: ".")
        while (!File(d, "settings.gradle.kts").exists() && d.parentFile != null) d = d.parentFile!!
        return d
    }

    private fun src(path: String): String {
        val f = File(root(), path)
        assertTrue("فایل پیدا نشد: $path", f.exists())
        return f.readText()
    }

    @Test
    fun versionPinGuardExistsAndIsWiredIntoCi() {
        val guard = src("scripts/check_test_version_pins.py")
        assertTrue(guard.contains("APP_VERSION"))
        assertTrue(guard.contains("top.appVersion"))
        assertTrue(guard.contains("top\\.version"))
        assertTrue("ریشه باید پوشهٔ مخزن باشد", guard.contains("resolve().parent.parent"))
        val ci = src(".github/workflows/android.yml")
        assertTrue("نگهبان پین نسخه باید در CI اجرا شود", ci.contains("check_test_version_pins"))
    }

    @Test
    fun guardCatchesTheExactPatternThatBrokeCi() {
        // همان سه خطی که در V355 سخت‌رمز شده بودند باید ممنوع باشند.
        val guard = src("scripts/check_test_version_pins.py")
        assertTrue(guard.contains("BANNED"))
        // به‌جای تکرار عین الگو (که نگهبان گریز را می‌آزارد)،
        // وجود هر سه منبع در جدول ممنوعه سنجیده می‌شود.
        assertTrue("منبع فایل نسخه", guard.contains("APP_VERSION"))
        assertTrue("الگوی appVersion", guard.contains("appVersion"))
        assertTrue("الگوی نام نسخه", guard.contains("version"))
        assertTrue("پیام راهنما باید راه درست را نشان دهد",
            guard.contains("assertEquals(version, top.appVersion)"))
    }

    @Test
    fun noTestHardcodesTheAppVersion() {
        // همان قاعدهٔ نگهبان، این‌بار درون خودِ آزمون‌ها تا اگر نگهبان
        // از حلقهٔ CI حذف شد، این آزمون بگیرد.
        val dir = File(root(), "app/src/test/java")
        val bad = mutableListOf<String>()
        // تشخیص بدون regex: هر خطِ assertEquals که یک نسخهٔ ثابت را با
        // یکی از سه منبع زنده مقایسه کند، ممنوع است.
        val sources = listOf("src(\"text/APP_VERSION.txt\"", "top.appVersion", "top.version")
        val fixedVersion = Regex("[\"](?:V[0-9]+(?:[.][0-9]+)?|[0-9]+[.][0-9]+(?:[.][0-9]+)?)[\"]")
        dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { f ->
            f.readText().lines().forEachIndexed { i, line ->
                if (line.trim().startsWith("//")) return@forEachIndexed
                if (!line.contains("assertEquals(")) return@forEachIndexed
                if (sources.none { line.contains(it) }) return@forEachIndexed
                if (fixedVersion.containsMatchIn(line)) bad.add("${f.name}:${i + 1}")
            }
        }
        assertTrue("نسخه سخت‌رمز پیدا شد: $bad", bad.isEmpty())
    }

    @Test
    fun theTwoFormerlyPinnedTestsNowCompareSources() {
        listOf(
            "app/src/test/java/ir/exam/app/V355_GradingSimilarityTest.kt",
            "app/src/test/java/ir/exam/app/V356_CodeGradingTest.kt"
        ).forEach { path ->
            val t = src(path)
            assertTrue("باید نسخه را از فایل بخواند: $path",
                t.contains("""val version = src("text/APP_VERSION.txt").trim()"""))
            assertTrue("باید هم‌خوانی را بسنجد: $path",
                t.contains("assertEquals(\"فایل نسخه و بلوک بالای فهرست تغییرات باید یکی باشند\", version, top.appVersion)"))
        }
    }

    @Test
    fun versionAndChangelogStillAgree() {
        val version = src("text/APP_VERSION.txt").trim()
        assertTrue("قالب شمارهٔ نسخه نامعتبر است: $version", version.matches(Regex("\\d+\\.\\d+\\.\\d+")))
        val lines = src("text/CHANGELOG_FA.txt").lines()
        listOf(40, 60).forEach { take ->
            val top = ReleaseNotes.parse(lines.take(take)).first()
            assertTrue(top.appAffecting)
            assertEquals("فایل نسخه و بلوک بالای فهرست تغییرات باید یکی باشند", version, top.appVersion)
            assertTrue(top.bullets.size in 1..ReleaseNotes.MAX_BULLETS)
            top.bullets.forEach { assertFalse(it.contains(Regex("\\bV\\d"))) }
        }
        // این انتشار قابلیت تازه ندارد؛ فهرست تغییرات نباید وعدهٔ دروغ بدهد.
        val top = ReleaseNotes.parse(lines.take(60)).first()
        assertTrue("نباید ادعای قابلیت تازه شود",
            top.bullets.any { it.contains("تغییر نکرده") || it.contains("قابلیت تازه‌ای ندارد") })
    }

    /**
     * V356.1 — نقص انتشار: چانک پنل با «تازه‌ترین زمان تغییر فایل»
     * انتخاب می‌شد. چون فهرست نگه‌داشت چند چانک دارد، ممکن بود کد
     * کهنه منتشر یا آزموده شود. مرجع باید __PANELS_URL باشد.
     */
    @Test
    fun panelChunkIsChosenByIndexReferenceNotFileTime() {
        val html = src("site/index.html")
        assertTrue("index.html باید چانک مرجع را اعلام کند", html.contains("window.__PANELS_URL"))
        listOf("scripts/build_site_out.py", "scripts/web_ui_e2e.py", ".github/workflows/site.yml").forEach { rel ->
            val text = src(rel)
            if (!text.contains("getmtime")) return@forEach
            assertTrue("باید چانک مرجع را بخواند: $rel", text.contains("__PANELS_URL"))
            assertTrue("خواندن مرجع باید پیش از انتخاب زمانی باشد: $rel",
                text.indexOf("__PANELS_URL") < text.indexOf("getmtime"))
        }
        val guard = src("scripts/check_site_chunk_ref.py")
        assertTrue(guard.contains("SELECTORS"))
        assertTrue("نگهبان باید در CI اجرا شود",
            src(".github/workflows/android.yml").contains("check_site_chunk_ref"))
    }

    /** پین حلقهٔ نگهبان در V352.2 نباید با افزودن عضو تازه بشکند. */
    @Test
    fun guardLoopPinSurvivesNewMembers() {
        val ci = src(".github/workflows/android.yml")
        // حلقه ممکن است با «\» به خط بعد ادامه یابد؛ باید پیوسته شود.
        val joined = ci.replace("\\\n", " ")
        val loop = joined.lines().firstOrNull { it.contains("check_site_pins") }.orEmpty()
        assertTrue("حلقهٔ نگهبان پیدا نشد", loop.isNotEmpty())
        assertTrue("حلقه باید بسته شود", loop.contains("; do"))
        assertTrue("نگهبان تازه باید در همان حلقه باشد", loop.contains("check_test_version_pins"))
        val old = src("app/src/test/java/ir/exam/app/V352_2_ChangelogGuardTest.kt")
        assertFalse("پین نباید به دنبالهٔ پایانی حلقه چسبیده باشد",
            old.contains("check_site_sync check_changelog; do"))
    }
}
