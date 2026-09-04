package ir.exam.app.ui.app

import ir.exam.app.domain.model.OfficialExamPrintable
import ir.exam.app.domain.model.OfficialPrintHeader
import ir.exam.app.domain.model.OfficialPrintQuestion
import ir.exam.app.ui.printing.ExamHtmlPrintPayloadBuilder
import java.io.File
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V73.0 / V76.0 — آزمون‌های یکپارچگی «نسخهٔ 30» (آزمون‌ساز/چاپ تعاملی HTML):
 * ۱) نگاشت هر ۶ نوع سؤال به ساختار questions نسخهٔ 30 با پل window.setExamData.
 * ۲) فیلدهای سربرگ با شناسه‌های واقعی فرم فایل (f_headerTemplate/f_course/…).
 * ۳) asset نسخهٔ 30: بدون آیکن‌های درج شکل (ویرایش شکل فقط در برنامه)، بدون
 *    اسکریپت‌های Cloudflare، با پل بومی ExamPrintNative و پل میزبان setExamData.
 * ۴) صفحهٔ چاپ: کارت آزمون فقط مداد + پرینتر؛ دکمهٔ وسط‌چین «آزمون جدید».
 */
class V73_0HtmlPrintIntegrationTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val printCenter by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamPrintCenterScreen.kt") }
    private val dialogSource by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintDialog.kt") }
    private val payloadBuilder by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintPayload.kt") }
    private val htmlAsset by lazy { File(root(), "app/src/main/assets/print/exam_print.html") }

    @Test
    fun `payload builder maps all six question types to json`() {
        val printable = OfficialExamPrintable(
            documentTitle = "آزمون نوبت اول",
            header = OfficialPrintHeader(
                province = "فارس",
                city = "شیراز",
                district = "۱",
                school = "شهید دستغیب",
                grade = "یازدهم",
                fieldOfStudy = "تجربی",
                subject = "زیست‌شناسی",
                examDate = "۱۴۰۳/۱۰/۲۰",
                examDuration = "۶۰"
            ),
            subject = "زیست‌شناسی",
            durationMinutes = 60,
            totalScore = 20.0,
            questions = listOf(
                OfficialPrintQuestion(
                    number = 1,
                    text = "سؤال چهارگزینه‌ای",
                    score = 1.5,
                    options = listOf("گزینه ۱", "گزینه ۲", "گزینه ۳", "گزینه ۴"),
                    answerText = "گزینه ۲"
                ),
                OfficialPrintQuestion(
                    number = 2,
                    text = "سؤال صحیح و غلط",
                    score = 1.0,
                    options = listOf("صحیح", "غلط"),
                    answerText = "صحیح"
                ),
                OfficialPrintQuestion(
                    number = 3,
                    text = "سؤال تشریحی",
                    score = 2.0,
                    answerLines = 4,
                    answerLineStyle = "lined"
                ),
                OfficialPrintQuestion(
                    number = 4,
                    text = "پایتخت ایران [...] است.",
                    score = 1.0
                ),
                OfficialPrintQuestion(
                    number = 5,
                    text = "حاصل ۴ × ۳",
                    score = 0.5,
                    answerText = "12 ± 0"
                ),
                OfficialPrintQuestion(
                    number = 6,
                    text = "جورکردنی",
                    score = 2.0,
                    matchingLeft = listOf("الف", "ب"),
                    matchingRight = listOf("۱", "۲")
                )
            )
        )

        val json = ExamHtmlPrintPayloadBuilder.build(printable)
        assertEquals(false, json["reset"]?.jsonPrimitive?.booleanOrNull)
        val fields = json["fields"]?.jsonObject!!
        assertEquals("classic", fields["f_headerTemplate"]?.jsonPrimitive?.content)
        assertEquals("زیست‌شناسی", fields["f_course"]?.jsonPrimitive?.content)
        assertEquals("شهید دستغیب", fields["f_branch"]?.jsonPrimitive?.content)
        assertEquals("۱۴۰۳/۱۰/۲۰", fields["f_examDate"]?.jsonPrimitive?.content)
        assertEquals("60 دقیقه", fields["f_duration"]?.jsonPrimitive?.content)
        assertEquals(6, json["qIdCounter"]?.jsonPrimitive?.intOrNull)

        val qArray = json["questions"]?.jsonArray!!
        assertEquals(6, qArray.size)

        val mc = qArray[0].jsonObject
        assertEquals("multiple", mc["type"]?.jsonPrimitive?.content)
        assertEquals("1.5", mc["score"]?.jsonPrimitive?.content)
        val mcOpts = mc["options"]?.jsonArray!!
        assertEquals(4, mcOpts.size)
        assertTrue(mcOpts[1].jsonObject["correct"]?.jsonPrimitive?.booleanOrNull == true)
        assertFalse(mcOpts[0].jsonObject["correct"]?.jsonPrimitive?.booleanOrNull == true)

        val tf = qArray[1].jsonObject
        assertEquals("truefalse", tf["type"]?.jsonPrimitive?.content)
        val tfOpts = tf["options"]?.jsonArray!!
        assertTrue(tfOpts[0].jsonObject["correct"]?.jsonPrimitive?.booleanOrNull == true)
        assertFalse(tfOpts[1].jsonObject["correct"]?.jsonPrimitive?.booleanOrNull == true)

        val essay = qArray[2].jsonObject
        assertEquals("long", essay["type"]?.jsonPrimitive?.content)
        assertEquals(4, essay["answerLines"]?.jsonPrimitive?.intOrNull)
        assertEquals("lined", essay["answerStyle"]?.jsonPrimitive?.content)

        val fill = qArray[3].jsonObject
        assertEquals("fill", fill["type"]?.jsonPrimitive?.content)

        val num = qArray[4].jsonObject
        assertEquals("numeric", num["type"]?.jsonPrimitive?.content)
        assertEquals("12", num["answer"]?.jsonPrimitive?.content)

        val match = qArray[5].jsonObject
        assertEquals("matching", match["type"]?.jsonPrimitive?.content)
        val pairs = match["pairs"]?.jsonArray!!
        assertEquals(2, pairs.size)
        assertEquals("الف", pairs[0].jsonObject["left"]?.jsonPrimitive?.content)
        assertEquals("۱", pairs[0].jsonObject["right"]?.jsonPrimitive?.content)
        assertEquals("ب", pairs[1].jsonObject["left"]?.jsonPrimitive?.content)
        assertEquals("۲", pairs[1].jsonObject["right"]?.jsonPrimitive?.content)
    }

    @Test
    fun `printable exam payload builder populates json`() {
        val printable = OfficialExamPrintable(
            documentTitle = "فیزیک دهم",
            header = OfficialPrintHeader(
                school = "دبیرستان رازی",
                examDate = "۱۴۰۳/۰۹/۱۵",
                grade = "دهم"
            ),
            subject = "فیزیک",
            durationMinutes = 75,
            totalScore = 20.0,
            questions = listOf(
                OfficialPrintQuestion(
                    number = 1,
                    text = "سؤال تست",
                    score = 2.0,
                    options = listOf("الف", "ب"),
                    answerText = "الف"
                )
            )
        )

        val json = ExamHtmlPrintPayloadBuilder.build(printable)
        val fields = json["fields"]?.jsonObject!!
        assertEquals("فیزیک", fields["f_course"]?.jsonPrimitive?.content)
        assertEquals("دبیرستان رازی", fields["f_branch"]?.jsonPrimitive?.content)
        assertEquals("75 دقیقه", fields["f_duration"]?.jsonPrimitive?.content)

        val qArray = json["questions"]?.jsonArray!!
        assertEquals(1, qArray.size)
        val q0 = qArray[0].jsonObject
        assertEquals("multiple", q0["type"]?.jsonPrimitive?.content)
        assertTrue(q0["options"]?.jsonArray?.get(0)?.jsonObject?.get("correct")?.jsonPrimitive?.booleanOrNull == true)
    }

    @Test
    fun `html asset exists and supports the builder-30 host bridge`() {
        assertTrue(htmlAsset.isFile)
        // V79.0/V79.2 — ویرایشگر فرمول و لوگوها به asset جدا رفتند؛ آستانه
        // پایین آمد و مجموعِ سه فایل پین می‌شود تا جابه‌جایی مجاز و حذف ممنوع باشد.
        assertTrue("asset too small", htmlAsset.length() > 4_000_000L)
        val mathAsset = File(htmlAsset.parentFile, "math_editor.html")
        val logoDir = File(htmlAsset.parentFile, "logos")
        assertTrue("math editor asset missing", mathAsset.isFile)
        val moved = htmlAsset.length() + mathAsset.length() +
            (logoDir.listFiles()?.sumOf { it.length() } ?: 0L)
        assertTrue("content vanished instead of moving: $moved", moved > 5_400_000L)
        val content = htmlAsset.readText()
        assertTrue("window.setExamData" in content)
        assertTrue("__qmfHostBridge" in content)
        assertTrue("normQ" in content)
        assertTrue("ExamPrintNative" in content)
        assertTrue("updateHeaderSettingsVisibility()" in content)
        assertTrue("renderAll()" in content)
        // V76.1 — هر ۷ ابزار درج داخل خود آزمون‌ساز فعال است (شکل/نمودار/جدول/…)
        assertTrue("q-tool-btn is-fig" in content)
        assertTrue("title=\"درج شکل\"" in content)
        assertTrue("title=\"درج نمودار\"" in content)
        // V79.0 — همان ویرایشگر اصلی، ولی از asset هم‌مبدأ لود می‌شود
        assertTrue("f.src = MATH_EDITOR_URL" in content)
        // گاردِ اشتباه V76.1 که مسیرِ ویرایشگر اصلی را می‌دزدید نباید برگردد
        assertFalse("if (!EXACT_MATH_EDITOR_B64)" in content)
        // موتور رندر توکن‌ها حفظ شده است
        assertTrue("is-fx formula-btn" in content)
        // V76.0 — بدون اسکریپت‌های تزریقی Cloudflare (استفادهٔ آفلاین/WebView)
        assertFalse("cloudflareinsights" in content)
        assertFalse("challenge-platform" in content)
    }

    @Test
    fun `print center opens builder 30 from pencil printer and new exam button`() {
        // V76.0 — کارت آزمون فقط مداد + پرینتر دارد
        assertTrue("contentDescription = \"ویرایش آزمون\"" in printCenter)
        assertTrue("Icons.Outlined.Print" in printCenter)
        assertTrue("contentDescription = \"چاپ آزمون\"" in printCenter)
        assertFalse("Text(\"چاپ\")" in printCenter)
        assertFalse("Text(\"چاپ برگه\")" in printCenter)
        // دکمهٔ وسط‌چین «آزمون جدید» جایگزین «سربرگ»
        assertTrue("Text(\"آزمون جدید\")" in printCenter)
        assertFalse("بستن سربرگ" in printCenter)
        assertFalse("PrintHeaderDialog" in printCenter)
        // پنجرهٔ تمام‌صفحهٔ نسخهٔ 30
        assertTrue("ExamHtmlPrintDialog(" in printCenter)
        assertTrue("htmlPrintExam" in printCenter)
        assertTrue("htmlPrintOpen" in printCenter)
        assertTrue("htmlPrintLoading" in printCenter)
        assertTrue("ExamHtmlImageInliner.inline(" in printCenter)
    }

    @Test
    fun `html dialog implements secure webview and print bridge`() {
        assertTrue("fun ExamHtmlPrintDialog(" in dialogSource)
        assertTrue("printable: OfficialExamPrintable?" in dialogSource)
        assertTrue("ExamPrintNative" in dialogSource)
        assertTrue("exam-print.local" in dialogSource)
        assertTrue("print/exam_print.html" in dialogSource)
        assertTrue("createPrintDocumentAdapter" in dialogSource)
        assertTrue("ExamHtmlPrintPayloadBuilder" in dialogSource)
    }
}
