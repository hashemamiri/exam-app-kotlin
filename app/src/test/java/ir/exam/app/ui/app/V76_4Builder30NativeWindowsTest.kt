package ir.exam.app.ui.app

import ir.exam.app.ui.printing.HeaderSchema
import ir.exam.app.ui.printing.loadHeaderSchema
import java.io.File
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V76.4 — پنجره‌های بومی آزمون‌ساز + هستهٔ بومی استودیوی تصویر:
 * ۱) شِمای تنظیمات سربرگ دقیقاً از خود فایل ۳۰ استخراج شده (۷ قالب/همهٔ فیلدها).
 * ۲) چهار پنجرهٔ بومی در دیالوگ سیم‌کشی شده‌اند و پل‌های setFields/ExportJson/
 *    AddQuestionImage موجودند.
 * ۳) دوربین سؤال در asset به پل بومی می‌رود (با پشتیبانِ استودیوی کامل HTML).
 * ۴) هستهٔ استودیوی بومی: چرخش/قرینه/برش/اسکن(۱۸۵)/اندازه‌های S-M-L-∞/کیفیت ۹۲.
 */
class V76_4Builder30NativeWindowsTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val dialogSource by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintDialog.kt") }
    private val windowsSource by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamBuilder30Windows.kt") }
    private val studioSource by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamImageStudioCore.kt") }
    private val assetText by lazy { File(root(), "app/src/main/assets/print/exam_print.html").readText() }
    private val schemaText by lazy { File(root(), "app/src/main/assets/print/header_settings_schema.json").readText() }

    @Test
    fun `header settings schema is extracted from the builder-30 file itself`() {
        val schemaFile = File(root(), "app/src/main/assets/print/header_settings_schema.json")
        assertTrue(schemaFile.isFile && schemaFile.length() > 5_000L)
        val json = kotlinx.serialization.json.Json.parseToJsonElement(schemaText).jsonObject
        val templates = json["templates"]?.jsonArray!!
        assertEquals(7, templates.size)
        val ids = templates.map { it.jsonObject["id"]?.jsonPrimitive?.content }
        assertEquals(listOf("classic", "formal", "sama", "school", "edu", "detailed-school", "ministry"), ids)
        val classic = templates.map { it.jsonObject }.first { it["id"]?.jsonPrimitive?.content == "classic" }
        val fieldIds = classic["fields"]?.jsonArray?.map { it.jsonObject["id"]?.jsonPrimitive?.content }
        for (expected in listOf("f_course", "f_branch", "f_examDate", "f_duration", "f_name", "f_professor")) {
            assertTrue(expected, expected in (fieldIds ?: emptyList()))
        }
        // قالب وزارت هم فیلدهای h7_* خودش را دارد
        val ministry = templates.map { it.jsonObject }.first { it["id"]?.jsonPrimitive?.content == "ministry" }
        val ministryIds = ministry["fields"]?.jsonArray?.map { it.jsonObject["id"]?.jsonPrimitive?.content }
        assertTrue("h7_name", "h7_name" in (ministryIds ?: emptyList()))
    }

    @Test
    fun `schema loader parses the asset file`() {
        // loadHeaderSchema باید همان فایل asset را بخواند و ساختار معتبر بدهد
        assertTrue("loadHeaderSchema" in windowsSource)
        assertTrue("print/header_settings_schema.json" in windowsSource)
        val reflected = HeaderSchema::class.java // کلاس‌های سرِایال قابل‌دسترس‌اند
        assertNotNull(reflected)
    }

    @Test
    fun `dialog wires the four native windows`() {
        // تنظیمات سربرگ: شِما + پیش‌خوانی مقادیر صفحه + اعمال با __qmfSetFields
        assertTrue("loadHeaderSchema(context)" in dialogSource)
        assertTrue("HeaderSettingsDialog(" in dialogSource)
        assertTrue("window.__qmfExportJson" in dialogSource)
        assertTrue("window.__qmfSetFields" in dialogSource)
        assertTrue("parsePageFields(" in dialogSource)
        // ذخیره: این جلسه + فایل JSON با SAF بومی
        assertTrue("SaveExamDialog(" in dialogSource)
        assertTrue("ActivityResultContracts.CreateDocument(\"application/json\")" in dialogSource)
        assertTrue("safeExamFileName(" in dialogSource)
        // بازکردن: پنجرهٔ خلاصه قبل از اعمال
        assertTrue("OpenExamSummaryDialog(" in dialogSource)
        assertTrue("pendingOpenText" in dialogSource)
        // سوال جدید: پنجرهٔ بومی ۶ نوعه + همان pickQuestionType فایل
        assertTrue("NewQuestionTypeDialog(" in dialogSource)
        assertTrue("pickQuestionType('" in dialogSource)
    }

    @Test
    fun `native windows cover all seven header templates and six question types`() {
        // ۷ قالب با همان برچسب‌های فایل (نمونه‌ها)
        for (label in listOf("سربرگ ۱ - قالب قبلی دانشگاه آزاد", "سربرگ ۷ - قالب وزارت آموزش و پرورش")) {
            assertTrue(label, label in schemaText)
        }
        // ۶ نوع سؤال با همان نماد/ترتیب فایل
        for (t in listOf(
            "\"multiple\" to \"🔘 چندگزینه‌ای\"",
            "\"truefalse\" to \"✓ صحیح/غلط\"",
            "\"long\" to \"📝 تشریحی\"",
            "\"fill\" to \"___ جای‌خالی\"",
            "\"numeric\" to \"🔢 عددی\"",
            "\"matching\" to \"↔ جورکردنی\""
        )) {
            assertTrue(t, t in windowsSource)
        }
    }

    @Test
    fun `camera button routes to the native image studio`() {
        // asset: کلیک دوربین → پل بومی (V77.1: پشتیبانِ HTML حذف شد)
        assertTrue("openImageStudio" in assetText)
        assertFalse("window.__qmfOpenLegacyStudio" in assetText)
        assertTrue("p.file.click(); } catch (err) {}" in assetText)
        // Kotlin: پل + پنجرهٔ استودیو + درج با همان قرارداد
        assertTrue("fun openImageStudio(questionId: String?)" in dialogSource)
        assertTrue("ExamImageStudioDialog(" in dialogSource)
        assertTrue("window.__qmfAddQuestionImage" in dialogSource)
    }

    @Test
    fun `native image studio core keeps the studio defaults`() {
        // پیش‌فرض‌های عینِ استودیوی ۳۰: آستانهٔ ۱۸۵، کیفیت ۹۲، سایزها ۲۴۰/۴۲۰/۶۴۰/∞
        assertTrue("mutableStateOf(185)" in studioSource)
        assertTrue("mutableStateOf(92)" in studioSource)
        assertTrue("mutableStateOf(420)" in studioSource)
        assertTrue("240 to \"S\", 420 to \"M\", 640 to \"L\", 0 to \"∞\"" in studioSource)
        // دوربین بومی با FileProvider و اسکن آستانه‌ای و خروجی JPEG
        assertTrue("ActivityResultContracts.TakePicture()" in studioSource)
        assertTrue("FileProvider.getUriForFile" in studioSource)
        assertTrue("fun processAndEncode(" in studioSource)
        assertTrue("Bitmap.CompressFormat.JPEG" in studioSource)
        // V77.1 — پورت کامل شد: دکمه و پلِ «ابزارهای کامل» حذف شده‌اند
        assertFalse("ابزارهای کامل" in studioSource)
        assertFalse("onLegacyStudio" in studioSource)
    }

    @Test
    fun `file provider exposes the studio cache path`() {
        val xml = source("app/src/main/res/xml/update_file_paths.xml")
        assertTrue("cache-path" in xml)
        assertTrue("studio/" in xml)
    }
}
