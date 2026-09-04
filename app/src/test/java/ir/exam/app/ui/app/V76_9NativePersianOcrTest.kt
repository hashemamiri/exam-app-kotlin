package ir.exam.app.ui.app

import ir.exam.app.ui.printing.ExamImageOcr
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V76.9 — OCR فارسیِ بومی و آفلاین (جایگزینِ OCR استودیوی HTML که به آینه‌های
 * آنلاین وابسته بود): موتور Tesseract 4 + دادهٔ زبانِ `fas` داخل assets.
 */
class V76_9NativePersianOcrTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val ocr by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamImageOcr.kt") }
    private val studio by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamImageStudioCore.kt") }
    private val dialog by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintDialog.kt") }
    private val assetText by lazy { source("app/src/main/assets/print/exam_print.html") }
    private val gradle by lazy { source("app/build.gradle.kts") }
    private val settings by lazy { source("settings.gradle.kts") }

    @Test
    fun `language data ships inside the apk`() {
        val data = File(root(), "app/src/main/assets/tessdata/fas.traineddata")
        assertTrue("دادهٔ زبان فارسی در assets نیست", data.isFile)
        // فایل واقعی tessdata است، نه placeholder خالی
        assertTrue("دادهٔ زبان خیلی کوچک است", data.length() > 300_000L)
        // دادهٔ زبان نباید فشرده شود وگرنه کپی/خواندنش شکننده می‌شود
        assertTrue("noCompress += \"traineddata\"" in gradle)
    }

    @Test
    fun `engine dependency and its repository are declared`() {
        assertTrue(
            "com.github.adaptech-cz.Tesseract4Android:tesseract4android:4.8.0" in gradle
        )
        // این کتابخانه در Maven Central نیست؛ مخزن JitPack با دامنهٔ محدود لازم است
        assertTrue("https://jitpack.io" in settings)
        assertTrue("includeGroupByRegex" in settings)
    }

    @Test
    fun `ocr engine uses the verified tesseract api`() {
        assertTrue("import com.googlecode.tesseract.android.TessBaseAPI" in ocr)
        assertTrue("fun ensureTessData(context: Context): String" in ocr)
        assertTrue("if (!engine.init(dataPath, LANG)) return@runCatching null" in ocr)
        assertTrue("engine.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO)" in ocr)
        assertTrue("val raw = engine.getUTF8Text()" in ocr)
        assertTrue("engine.meanConfidence()" in ocr)
        // نشتِ منابع ممنوع — موتور همیشه بازیافت شود
        assertTrue("runCatching { api?.recycle() }" in ocr)
        assertEquals("fas", ExamImageOcr.LANG)
    }

    @Test
    fun `studio exposes an ocr button wired to the same prepared image`() {
        assertTrue("🔎 استخراج متن (OCR فارسی)" in studio)
        assertTrue("private fun prepareForOcr(" in studio)
        assertTrue("ExamImageOcr.recognize(context, prepared)" in studio)
        assertTrue("onOcrText: (String) -> Unit = {}," in studio)
        // نتیجه پیش از درج قابل ویرایش است
        assertTrue("درج در متن سؤال" in studio)
    }

    @Test
    fun `ocr text is appended to the question through a bridge`() {
        assertTrue("window.__qmfAppendQuestionText" in dialog)
        assertTrue("window.__qmfAppendQuestionText = function (qid, b64Text) {" in assetText)
        assertTrue("""    q.text = cur ? (cur + "\n" + add) : add;""" in assetText)
        assertTrue("""    return "ok";""" in assetText)
    }

    // ---------- ریاضیِ واقعی: نرمال‌سازی متن ----------

    @Test
    fun `arabic letters are normalized to persian`() {
        val out = ExamImageOcr.normalizePersian("كيف يكون")
        assertTrue("ك عربی نرمال نشد", '\u0643' !in out)
        assertTrue("ي عربی نرمال نشد", '\u064A' !in out)
        assertTrue("ک فارسی تولید نشد", '\u06A9' in out)
        assertTrue("ی فارسی تولید نشد", '\u06CC' in out)
    }

    @Test
    fun `eastern digits become latin digits`() {
        assertEquals("12345", ExamImageOcr.normalizePersian("۱۲۳۴۵"))
        assertEquals("67890", ExamImageOcr.normalizePersian("٦٧٨٩٠"))
        assertEquals("2 نمره", ExamImageOcr.normalizePersian("۲ نمره"))
    }

    @Test
    fun `whitespace is collapsed without destroying paragraphs`() {
        val out = ExamImageOcr.normalizePersian("  خط اول   دوم  \n\n\n\nخط سوم  \n")
        assertEquals("خط اول دوم\n\nخط سوم", out)
    }

    @Test
    fun `normalizer never returns leading or trailing blanks`() {
        listOf("", "   ", "\n\n\n", " متن ").forEach { input ->
            val out = ExamImageOcr.normalizePersian(input)
            assertEquals(out, out.trim())
        }
        assertEquals("متن", ExamImageOcr.normalizePersian(" متن "))
    }
}
