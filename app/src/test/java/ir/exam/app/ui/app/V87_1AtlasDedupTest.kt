package ir.exam.app.ui.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.net.URI

/**
 * V87.1 — تصاویرِ اطلس دو نسخه داشتند: یکی base64 داخلِ exam_print.html و یکی
 * فایل در figure_atlas/ که پنجرهٔ بومی می‌خواند. نسخهٔ درون‌خطی حذف شد.
 */
class V87_1AtlasDedupTest {

    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val assetsDir by lazy { File(root(), "app/src/main/assets") }
    private val asset by lazy { File(assetsDir, "print/exam_print.html").readText() }
    private val dialog by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintDialog.kt").readText()
    }

    private val base = "https://exam-print.local/print/exam_print.html"

    /** همان تبدیلی که رهگیرِ WebView انجام می‌دهد. */
    private fun intercept(path: String): String? = when {
        path.startsWith("/print/") -> "print/" + path.removePrefix("/print/")
        path.startsWith("/figure_atlas/") -> path.removePrefix("/")
        else -> null
    }

    @Test
    fun `no atlas image is inlined as base64 any more`() {
        assertEquals(0, Regex("data:image/jpeg;base64,").findAll(asset).count())
    }

    @Test
    fun `but all one hundred and thirty seven are still reachable`() {
        // pinscan:exact-count — این عدد عمدی است: هر ۱۳۷ تصویرِ اطلس باید
        // مقصد داشته باشد و گاردِ verify هم همین را قفل می‌کند.
        val refs = Regex("'(\\.\\./figure_atlas/[^']+)'").findAll(asset).map { it.groupValues[1] }.toList()
        assertEquals(137, refs.size)
        val missing = refs.filter { ref ->
            val path = URI(base).resolve(ref).path
            val assetPath = intercept(path)
            assetPath == null || !File(assetsDir, assetPath).isFile
        }
        assertTrue("این فایل‌ها روی دیسک نیستند: $missing", missing.isEmpty())
    }

    @Test
    fun `the relative path resolves outside the print folder as intended`() {
        val resolved = URI(base).resolve("../figure_atlas/anatomy/atlas-01.jpg")
        assertEquals("/figure_atlas/anatomy/atlas-01.jpg", resolved.path)
        assertEquals("exam-print.local", resolved.host)
    }

    @Test
    fun `the interceptor serves the atlas folder and still refuses anything else`() {
        assertTrue("path.startsWith(\"/figure_atlas/\") -> path.removePrefix(\"/\")" in dialog)
        assertEquals("print/math_editor.html", intercept("/print/math_editor.html"))
        assertEquals("figure_atlas/anatomy/atlas-01.jpg", intercept("/figure_atlas/anatomy/atlas-01.jpg"))
        assertEquals(null, intercept("/etc/passwd"))
        // گاردِ پیمایشِ مسیر باید سرِ جایش باشد
        assertTrue("assetPath.contains(\"..\")" in dialog)
    }

    @Test
    fun `the javascript fallback for a missing atlas is untouched`() {
        assertTrue("if (window.ATLAS && window.ATLAS[id])" in asset)
    }

    @Test
    fun `the font stays inline because print depends on it`() {
        assertTrue("data:font/woff2;base64," in asset)
    }

    @Test
    fun `the file is now a quarter of its old size`() {
        val size = File(assetsDir, "print/exam_print.html").length()
        assertTrue("حجم باید زیرِ ۱٫۵MB باشد ولی $size است", size < 1_500_000)
    }
}
