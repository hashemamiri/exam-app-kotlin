package ir.exam.app

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V350.1 — دو دسته شکست CI در V350 بسته شد:
 *
 * ۱) شانزده آزمون فایل مخزن را با مسیر نسبیِ تنها باز می‌کردند. دایرکتوری
 *    کاریِ تست واحد پوشهٔ app است، نه ریشهٔ مخزن؛ پس فایل پیدا نمی‌شد.
 * ۲) یک سنجه، پایان بلوک را با indexOf از میانهٔ رشته می‌خواست که همان
 *    بک‌تیک آغازین را می‌یافت؛ کد درست بود و انتظارِ آزمون غلط.
 *
 * این آزمون خودِ همان دو نکته را پاس می‌دارد.
 */
class V350_1_TestPathGuardTest {

    private fun root(): File {
        var d = File(System.getProperty("user.dir") ?: ".")
        while (!File(d, "settings.gradle.kts").exists() && d.parentFile != null) d = d.parentFile!!
        return d
    }

    private fun read(path: String): String = File(root(), path).readText()

    @Test
    fun repositoryRootIsFoundFromTheTestWorkingDirectory() {
        // اگر این سنجه پاس شود، یعنی راه‌یاب ریشه در محیط تست واقعی کار می‌کند
        assertTrue("settings.gradle.kts باید در ریشه باشد", File(root(), "settings.gradle.kts").exists())
        assertTrue(File(root(), "site/src/builder.js").exists())
        assertTrue(File(root(), "app/build.gradle.kts").exists())
    }

    @Test
    fun wiringTestUsesTheRootWalker() {
        val s = read("app/src/test/java/ir/exam/app/V350_CodeChipWiringTest.kt")
        assertTrue("راه‌یاب ریشه", s.contains("while (!File(d, \"settings.gradle.kts\").exists() && d.parentFile != null)"))
        assertTrue("خواندن با ریشه", s.contains("val f = File(root(), path)"))
        assertTrue("سنجش نگهبان‌ها با ریشه", s.contains("File(root(), \"scripts/check_test_paths.py\").exists()"))
    }

    @Test
    fun blockEndUsesTheLastFenceNotTheFirst() {
        val src = "قبل\n```python\nprint(1)\n```\nبعد"
        val occ = ir.exam.app.core.text.CodeBlockCodec.occurrences(src).single()
        // انتظار درست: آخرین بک‌تیک، نه اولین بک‌تیک از میانهٔ رشته
        assertEquals(src.lastIndexOf("```") + 3, occ.endExclusive)
        assertEquals(src.indexOf("```"), occ.start)
        // و آنچه انتظار غلط قبلی می‌خواست، واقعاً غلط است
        assertTrue(src.indexOf("```", 4) + 3 != occ.endExclusive)
    }

    @Test
    fun pathGuardIsPresentAndDocumentsTheRule() {
        val s = read("scripts/check_test_paths.py")
        assertTrue("قاعده توضیح داده شده", s.contains("testDebugUnitTest"))
        assertTrue("نام فایل مرجع", s.contains("ROOT_MARK = 'settings.gradle.kts'"))
        assertTrue("توضیحات کنار گذاشته می‌شوند", s.contains("def strip_comments(src: str) -> str:"))
        assertTrue("فهرست جایگزین مستثناست", s.contains("if 'listOf(' in line:"))
        assertTrue("سنجش پوچ مستثناست", s.contains(".let { true }"))
    }
}
