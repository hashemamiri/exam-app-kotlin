package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V79 — سه رفع/بهبود:
 *
 *  V79.0  ویرایشگر فرمول در WebView باز نمی‌شد. iframe بدون src مبدأ
 *         about:blank داشت و چون allowUniversalAccessFromFileURLs خاموش است،
 *         دسترسی به contentDocument مسدود می‌شد و doc.write هرگز اجرا نمی‌شد.
 *         حالا بدنهٔ ویرایشگر یک asset جداست و با src هم‌مبدأ لود می‌شود.
 *         محتوای ویرایشگر تغییری نکرده — فقط روشِ بارگذاری.
 *  V79.1  «آزمون جدید» در صفحهٔ چاپ، آزمون‌سازِ بومی را باز می‌کند.
 *  V79.2  چهار لوگوی base64 به فایل PNG منتقل شدند (۳۸۶KB).
 */
class V79_0FormulaAndNativeBuilderTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val assetFile by lazy { File(root(), "app/src/main/assets/print/exam_print.html") }
    private val assetText by lazy { assetFile.readText() }
    private val mathFile by lazy { File(root(), "app/src/main/assets/print/math_editor.html") }
    private val center by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamPrintCenterScreen.kt") }
    private val app by lazy { source("app/src/main/java/ir/exam/app/ui/app/ExamApp.kt") }

    // ---------- V79.0 ----------

    @Test
    fun `math editor now lives in its own same-origin asset`() {
        assertTrue("فایل ویرایشگر فرمول نیست", mathFile.isFile)
        assertTrue("فایل ویرایشگر خیلی کوچک است", mathFile.length() > 900_000)
        val math = mathFile.readText()
        listOf("openMath", "mfApply", "closeMath").forEach {
            assertTrue("تابع $it در ویرایشگر نیست", it in math)
        }
        assertTrue(math.trimStart().startsWith("<!DOCTYPE html>"))
    }

    @Test
    fun `iframe is loaded by src instead of doc write`() {
        assertTrue("MATH_EDITOR_URL = \"/print/math_editor.html\"" in assetText)
        assertTrue("f.src = MATH_EDITOR_URL" in assetText)
        // ریشهٔ باگ نباید برگردد
        assertFalse("doc.write(" in assetText)
        assertFalse("MATH_EDITOR_HTML" in assetText)
    }

    @Test
    fun `boot still waits for the editor and keeps its fallback`() {
        // شرط آمادگی و پشتیبان هر دو سرجایشان
        assertTrue("w.openMath && w.mfApply && w.closeMath" in assetText)
        assertTrue("__openFallbackMathModal" in assetText)
        assertTrue("injectHostTheme()" in assetText)
    }

    @Test
    fun `formula path from a question card is unchanged`() {
        // مسیر کلیک: کارت سؤال ← openQuestionTool ← دکمهٔ مخفی ← __openMathEditor
        assertTrue("is-fx formula-btn" in assetText)
        assertTrue("openQuestionTool(" in assetText)
        // V82.0 — استثنای فرمول برداشته شد (پل بومی برای همه)
        assertTrue("ExamPrintNative.openFigureTool" in assetText)
        assertTrue("id=\"openFormulaEditor3\"" in assetText)
        assertTrue("window.__openMathEditor" in assetText)
    }

    // ---------- V79.1 ----------

    @Test
    fun `new exam button opens the native builder`() {
        assertTrue("onNewNativeExam" in center)
        assertTrue("onNewNativeExam = {" in app)
        assertTrue("page = MainPage.BUILDER" in app)
    }

    @Test
    fun `returning from the builder goes back to the print screen`() {
        assertTrue("builderCameFromPrint" in app)
        assertTrue("if (builderCameFromPrint) MainPage.PRINT else MainPage.HOME" in app)
    }

    @Test
    fun `the v30 builder is still reachable so nothing is lost`() {
        assertTrue("\"آزمون‌ساز چاپی\"" in center)
        assertTrue("htmlPrintOpen = true" in center)
        assertTrue("ExamHtmlPrintDialog(" in center)
    }

    // ---------- V79.2 ----------

    @Test
    fun `all four logos became real files`() {
        listOf("logo_azad.png", "logo_formal.png", "logo_sama.png", "logo_ministry.png").forEach { name ->
            val f = File(root(), "app/src/main/assets/print/logos/$name")
            assertTrue("لوگوی $name نیست", f.isFile)
            assertTrue("لوگوی $name خالی است", f.length() > 1000)
            // فایل واقعاً PNG باشد
            val head = f.readBytes().take(4)
            assertEquals(0x89.toByte(), head[0])
            assertEquals('P'.code.toByte(), head[1])
        }
    }

    @Test
    fun `no base64 image blob is left in the asset`() {
        assertFalse("data:image/png;base64" in assetText)
        assertTrue("const LOGO_IMG = '/print/logos/logo_azad.png'" in assetText)
        assertTrue("const LOGO_IMG_FORMAL = '/print/logos/logo_formal.png'" in assetText)
    }

    @Test
    fun `logos are preloaded so printing never loses the header`() {
        // چاپ فقط ۸۰ms بعد از renderPreview اجرا می‌شود
        assertTrue("im.decoding = 'sync'" in assetText)
        assertTrue("[LOGO_IMG, LOGO_IMG_FORMAL, LOGO_IMG_SAMA, LOGO_IMG_MINISTRY]" in assetText)
    }

    @Test
    fun `all seven headers still reference their logo`() {
        listOf("\${LOGO_IMG}", "\${LOGO_IMG_FORMAL}", "\${LOGO_IMG_SAMA}", "\${LOGO_IMG_MINISTRY}")
            .forEach { assertTrue("ارجاع $it گم شده", it in assetText) }
    }

    @Test
    fun `the asset got substantially smaller`() {
        // پیش از V79 حدود ۵٫۸۱MB بود
        assertTrue("حجم کم نشده: ${assetFile.length()}", assetFile.length() in 1L until 4_400_000L)
    }

    @Test
    fun `out of scope areas remain untouched`() {
        listOf("function printStudent", "function printTeacher", "function renderPreview",
               "function renderEditor", "function renderFigToken", "qimgUploaderJs")
            .forEach { assertTrue("بخش خارج از دامنه تغییر کرده: $it", it in assetText) }
    }
}
