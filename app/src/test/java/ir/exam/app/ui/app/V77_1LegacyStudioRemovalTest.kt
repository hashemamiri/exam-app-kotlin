package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V77.1 — پایانِ مهاجرت: استودیوی تصویرِ HTML (که از V76.4 به‌عنوان «پلِ موقت»
 * نگه داشته شده بود) از asset حذف شد، چون همهٔ ابزارهایش بومی شده‌اند:
 * دوربین/برش/چرخش/اسکن (V76.4)، صاف‌سازی و پرسپکتیو (V76.5)، تفکیک چندسؤاله و
 * مدیریت تصویرها (V76.6)، ابزارهای رسم (V76.7)، OCR فارسی (V76.9)، فیلترهای
 * اسکن کتاب و قطره‌چکان و لایهٔ اشیاء و فلش منحنی (V77.0).
 *
 * این تست هم حذف را تثبیت می‌کند و هم مطمئن می‌شود چیزی که باید بماند نرفته است.
 */
class V77_1LegacyStudioRemovalTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val assetFile by lazy { File(root(), "app/src/main/assets/print/exam_print.html") }
    private val assetText by lazy { assetFile.readText() }
    private val studio by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamImageStudioCore.kt") }
    private val dialog by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintDialog.kt") }

    @Test
    fun `html studio payload is gone from the asset`() {
        assertFalse("قالبِ base64 استودیو هنوز هست", "qimgStudioSrc" in assetText)
        assertFalse("میان‌افزارِ والد هنوز هست", "qimgStudioParent" in assetText)
        assertFalse("هیچ ارجاعی به __qimgStudio نباید بماند", "__qimgStudio" in assetText)
    }

    @Test
    fun `legacy bridge and its kotlin caller are gone`() {
        assertFalse("window.__qmfOpenLegacyStudio" in assetText)
        assertFalse("onLegacyStudio" in studio)
        assertFalse("onLegacyStudio" in dialog)
        assertFalse("ابزارهای کامل" in studio)
    }

    @Test
    fun `the asset actually got smaller`() {
        // پیش از حذف ~6.18MB بود؛ حذف ~504KB آزاد کرد.
        assertTrue(
            "asset هنوز بزرگ است: ${assetFile.length()}",
            assetFile.length() in 1L until 5_900_000L
        )
    }

    @Test
    fun `image insertion path still works without the studio`() {
        // بلوکِ افزودن تصویر و رندرِ آن باید سرجایش باشد
        assertTrue("qimgUploaderJs" in assetText)
        assertTrue("qimg-file" in assetText)
        assertTrue("data-qimg-block" in assetText)
        // مسیرِ بدون استودیو مستقیم پردازش می‌کند (بدون شاخهٔ مردهٔ st.open)
        assertTrue("if (!addImage(block, r.url, r.w, r.h)) insertFallback(block, f);" in assetText)
        assertFalse("st.open(f, function (imgs) {" in assetText)
    }

    @Test
    fun `native studio is still the camera destination`() {
        assertTrue("openImageStudio" in assetText)
        assertTrue("fun openImageStudio(questionId: String?)" in dialog)
        assertTrue("ExamImageStudioDialog(" in dialog)
    }

    @Test
    fun `every native bridge survived`() {
        listOf(
            "__qmfAddQuestionImage", "__qmfExportJson", "__qmfSetFields", "__qmfSaveNow",
            "__qmfQuestionImages", "__qmfRemoveQuestionImage", "__qmfReplaceQuestionImage",
            "__qmfSplitQuestion", "__qmfAppendQuestionText"
        ).forEach { bridge ->
            assertTrue("پل $bridge حذف شده است", "window.$bridge" in assetText)
        }
    }

    @Test
    fun `native studio still offers every ported tool`() {
        // اگر یکی از این‌ها نباشد، حذفِ HTML یعنی از دست رفتنِ یک امکان
        listOf(
            "📐 صفحه‌ای (۴ گوشه)", "🎯 تشخیص خودکار زاویه",   // V76.5
            "✂️ تفکیک چندسؤاله",                              // V76.6
            "➡️ فلش", "🖍️ هایلایتر", "🚫 سانسور", "🔤 متن",   // V76.7
            "🔎 استخراج متن (OCR فارسی)",                     // V76.9
            "📖 حذف سایه و زردی", "🧽 حذف نویز و لکه",
            "✂️ برش خودکار حاشیه", "💧 قطره‌چکان",
            "🪝 فلش منحنی", "🗂 لایهٔ اشیاء", "🌙 حالت تاریک"   // V77.0
        ).forEach { tool ->
            assertTrue("ابزارِ «$tool» در استودیوی بومی نیست", tool in studio)
        }
    }

    @Test
    fun `math editor was not collateral damage`() {
        assertTrue("MATH_EDITOR_URL" in assetText)  // V79.0
        assertTrue("id=\"mathEditorFrame\"" in assetText)
    }
}
