package ir.exam.app.ui.app

import ir.exam.app.core.update.ReleaseNotes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V208 — پنجرهٔ بروزرسانی فقط تیترهای نسخهٔ جدید؛ «درباره» سه نسخهٔ آخر با تیتر. */
class V208_ReleaseNotesTest {
    private fun src(path: String): String = File(path).readText()

    @Test
    fun parsesNewFormatAndOldParagraphs() {
        val lines = listOf(
            "V208 — خلاصهٔ نسخه",
            "- تیتر یک",
            "- تیتر دو",
            "",
            "V207 — سایت دسکتاپ: کادر بالا حذف شد؛ کد آزمون همچنان دیده می‌شود.",
            "V206 — مورد سوم",
            "V205 — مورد چهارم"
        )
        val blocks = ReleaseNotes.parse(lines)
        assertEquals(listOf("V208", "V207", "V206", "V205"), blocks.map { it.version })
        assertEquals(listOf("تیتر یک", "تیتر دو"), ReleaseNotes.latestBullets(lines))
        assertEquals(listOf("سایت دسکتاپ: کادر بالا حذف شد", "کد آزمون همچنان دیده می‌شود"), blocks[1].bullets)
        assertEquals(3, ReleaseNotes.lastVersions(lines).size)
        // بدون عنوان نسخه (یادداشت عمومی CI)
        assertEquals(listOf("انتشار خودکار"), ReleaseNotes.latestBullets(listOf("انتشار خودکار", "ساخت GitHub")))
    }

    @Test
    fun changelogTopBlockHasHeadlineBullets() {
        val lines = src("text/CHANGELOG_FA.txt").lines()
        assertTrue(lines.first().startsWith("V"))
        val top = ReleaseNotes.parse(lines.take(40)).first()
        assertTrue(top.bullets.size in 2..ReleaseNotes.MAX_BULLETS)
        assertTrue(top.bullets.all { it.length <= 160 })
    }

    @Test
    fun uiAndCiWiring() {
        assertTrue("ReleaseNotes.latestBullets(remote.notesFa)" in src("app/src/main/java/ir/exam/app/ui/app/ExamApp.kt"))
        assertTrue("ReleaseNotes.lastVersions(remote.notesFa, 3)" in src("app/src/main/java/ir/exam/app/ui/update/AboutScreen.kt"))
        val ci = src(".github/workflows/android.yml")
        assertTrue("if headers > 3:" in ci && "if len(payload_notes) >= 40:" in ci && "removeprefix(\"-\")" !in ci)
    }
}
