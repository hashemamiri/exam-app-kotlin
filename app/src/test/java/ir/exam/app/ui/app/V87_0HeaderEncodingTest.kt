package ir.exam.app.ui.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Base64

/**
 * V87.0 — سربرگ به‌صورت «Ø¯Ø§Ù†Ø´Ú¯Ø§Ù‡» چاپ می‌شد. مشکلِ فونت نبود:
 * `atob` هر بایت را یک نویسهٔ Latin-1 می‌کند ولی Kotlin با UTF-8 رمز کرده بود.
 */
class V87_0HeaderEncodingTest {

    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val dialog by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintDialog.kt").readText()
    }
    private val asset by lazy {
        File(root(), "app/src/main/assets/print/exam_print.html").readText()
    }

    /** همان کاری که `atob` می‌کند: هر بایت یک نویسه. */
    private fun atob(b64: String) = String(Base64.getDecoder().decode(b64), Charsets.ISO_8859_1)

    /** همان کاری که `decodeURIComponent(escape(...))` می‌کند. */
    private fun unescapeUtf8(latin1: String) =
        String(latin1.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8)

    @Test
    fun `persian text really is mangled without the fix`() {
        val original = "دانشگاه آزاد اسلامی واحد فسا"
        val b64 = Base64.getEncoder().encodeToString(original.toByteArray(Charsets.UTF_8))
        val mangled = atob(b64)
        assertTrue("باید mojibake شود", mangled != original)
        assertTrue("نشانهٔ کلاسیکِ mojibake", mangled.contains("Ø") || mangled.contains("Ù"))
    }

    @Test
    fun `and comes back exactly with the fix`() {
        for (original in listOf(
            "دانشگاه آزاد اسلامی واحد فسا",
            "دکتر نمازی",
            "ریاضی ۲ — پایان‌ترم",
            "۱۴۰۴/۰۳/۰۱"
        )) {
            val b64 = Base64.getEncoder().encodeToString(original.toByteArray(Charsets.UTF_8))
            assertEquals(original, unescapeUtf8(atob(b64)))
        }
    }

    @Test
    fun `both injection points decode as utf8`() {
        assertTrue("var t=decodeURIComponent(escape(atob('" in dialog)
        assertTrue("window.setExamData(decodeURIComponent(escape(atob('" in dialog)
    }

    @Test
    fun `no unguarded atob is left in the bridge`() {
        val all = Regex("atob\\(").findAll(dialog).count()
        val guarded = Regex("decodeURIComponent\\(escape\\(atob\\(").findAll(dialog).count()
        assertEquals("هر atob باید از راهِ decodeURIComponent(escape(...)) بگذرد", all, guarded)
    }

    @Test
    fun `the duplicated html toolbar is hidden but its functions live on`() {
        assertTrue("<div class=\"toolbar no-print\" id=\"qmfLegacyToolbar\">" in asset)
        assertTrue("#qmfLegacyToolbar{display:none !important}" in asset)
        // نوار پنهان است ولی پلِ بومی همین توابع را صدا می‌زند
        assertTrue("function printStudent(" in asset)
        assertTrue("function printTeacher(" in asset)
        assertTrue("window.togglePreviewWindow =" in asset)
        assertTrue("printStudent()" in dialog)
        assertTrue("printTeacher()" in dialog)
    }

    @Test
    fun `the anatomy atlas is still reachable after the images moved to files`() {
        // V87.1 — این تصاویر دو نسخه داشتند: base64 اینجا و فایل در figure_atlas.
        // نسخهٔ درون‌خطی حذف شد، ولی هر ۱۳۷ ورودی باید همچنان مقصد داشته باشد.
        assertEquals(0, Regex("data:image/jpeg;base64,").findAll(asset).count())
        assertEquals(137, Regex("'\\.\\./figure_atlas/").findAll(asset).count())
        val host = File(root(), "app/src/main/java/ir/exam/app/ui/printing/ExamFigureToolHost.kt").readText()
        assertTrue("AtlasToolFlow" in host)
    }
}
