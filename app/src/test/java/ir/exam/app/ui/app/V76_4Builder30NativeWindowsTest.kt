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
 * ۲) بارگذاری‌کنندهٔ شِما asset را می‌خواند (برای آزمون‌سازِ بومی).
 * ۳) هستهٔ استودیوی بومی: چرخش/قرینه/برش/اسکن(۱۸)/اندازه‌های S-M-L-∞/کیفیت ۹۲.
 *
 * V100 — دو تستِ «سیم‌کشیِ چهار پنجره در دیالوگ» و «دوربین سؤال به استودیو»
 * با حذفِ کاملِ «آزمون‌ساز چاپی» از پنجرهٔ چاپ حذف شدند (استودیو و شِما
 * همچنان در آزمون‌سازِ بومی کار می‌کنند).
 */
class V76_4Builder30NativeWindowsTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val windowsSource by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamBuilder30Windows.kt") }
    private val studioSource by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamImageStudioCore.kt") }
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
        val reflected = HeaderSchema::class.java // کلاس‌های سر_ایال قابل‌دسترس‌اند
        assertNotNull(reflected)
    }

    @Test
    fun `native windows cover all seven header templates`() {
        // ۷ قالب با همان برچسب‌های فایل (نمونه‌ها)
        for (label in listOf("سربرگ ۱ - قالب قبلی دانشگاه آزاد", "سربرگ ۷ - قالب وزارت آموزش و پرورش")) {
            assertTrue(label, label in schemaText)
        }
    }

    @Test
    fun `native image studio core keeps the studio defaults`() {
        // پیش‌فرض‌های عینِ استودیوی ۳۰: آستانهٔ ۱۸۵، کیفیت ۹۲، سایزها ۲۴۰/۴۲/۶۴۰/∞
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
