package ir.exam.app.ui.app

import ir.exam.app.core.calendar.JalaliDate
import ir.exam.app.data.dto.ExamDashboardDto
import ir.exam.app.domain.model.ClassGradeRow
import ir.exam.app.domain.model.OfficialPrintHeader
import ir.exam.app.ui.reports.ReportCardPayload
import ir.exam.app.ui.reports.StudentGradeCard
import java.io.File
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V339 — انتخاب ۱۰ قالب کارنامه، پیش‌نمایش A4 و چاپ (سایت و اپ).
 *
 * قراردادهای نگهبان:
 *  • یک موتور رندر بیشتر نداریم: اپ همان assets/reportcard را بارگذاری می‌کند که
 *    build_site.py داخل سایت تعبیه می‌کند؛
 *  • دادهٔ کارنامهٔ کلاس و تکی درست ساخته می‌شود (بارم هر آزمون از پاسخ‌ها، غایب = null)؛
 *  • هر ۱۰ قالب در موتور فهرست شده و هر سه ساختار جدول پشتیبانی می‌شود؛
 *  • مسیر چاپ کارنامه پرداخت ندارد و روی A4 چاپ می‌شود؛
 *  • خروجی PDF سادهٔ قبلی به‌عنوان جایگزین خطا باقی می‌ماند.
 */
class V339_ReportCardTemplatesTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "text/APP_VERSION.txt").isFile }
    private fun src(path: String) = File(root(), path).readText()

    private val engineJs = "app/src/main/assets/reportcard/reportcard.js"
    private val engineCss = "app/src/main/assets/reportcard/reportcard.css"
    private val engineHtml = "app/src/main/assets/reportcard/reportcard.html"
    private val dialog = "app/src/main/java/ir/exam/app/ui/reports/ReportCardPrintDialog.kt"

    private val exams = listOf(
        ExamDashboardDto(id = "e1", title = "ریاضی — مستمر", subject = "ریاضی", totalScore = 20.0),
        ExamDashboardDto(id = "e2", title = "ریاضی — پایانی", subject = "ریاضی", totalScore = 20.0),
        ExamDashboardDto(id = "e3", title = "فیزیک", subject = "فیزیک", totalScore = 15.0)
    )
    private val rows = listOf(
        ClassGradeRow("s1", "سارا محمدی", mapOf("e1" to 18.0, "e2" to 16.0, "e3" to 12.0), 85.0, mapOf("e3" to 15.0)),
        ClassGradeRow("s2", "علی رضایی", mapOf("e1" to 10.0, "e2" to null, "e3" to 7.5), 50.0)
    )

    @Test
    fun `class payload carries every exam column and every student row`() {
        val p = ReportCardPayload.forClass("دهم ۱۰۱", exams, rows, OfficialPrintHeader(school = "شهید بهشتی"), "آقای رحیمی")
        assertEquals("class", p["scope"]!!.jsonPrimitive.content)
        assertEquals("دهم ۱۰۱", p["className"]!!.jsonPrimitive.content)
        assertEquals("شهید بهشتی", p["school"]!!.jsonObject["name"]!!.jsonPrimitive.content)
        val cols = p["cols"]!!.jsonArray
        assertEquals(3, cols.size)
        assertEquals("ریاضی", cols[0].jsonObject["subject"]!!.jsonPrimitive.content)
        val data = p["rows"]!!.jsonArray
        assertEquals(2, data.size)
        assertEquals("سارا محمدی", data[0].jsonObject["name"]!!.jsonPrimitive.content)
        assertEquals(18.0, data[0].jsonObject["vals"]!!.jsonObject["e1"]!!.jsonPrimitive.doubleOrNull!!, 0.001)
    }

    @Test
    fun `absent score stays null so the form prints absent not zero`() {
        val p = ReportCardPayload.forClass("کلاس", exams, rows)
        val second = p["rows"]!!.jsonArray[1].jsonObject["vals"]!!.jsonObject
        assertTrue("نمرهٔ ثبت‌نشده باید null بماند", second["e2"].toString() == "null")
    }

    @Test
    fun `exam max comes from the recorded answer max not the exam total`() {
        // پیک تصادفی: سقف پاسخ دانش‌آموز (۱۰) بر بارم کل آزمون (۱۵) اولویت دارد
        val custom = listOf(ClassGradeRow("s1", "الف", mapOf("e3" to 8.0), 80.0, mapOf("e3" to 10.0)))
        val p = ReportCardPayload.forClass("کلاس", exams, custom)
        assertEquals(10.0, p["cols"]!!.jsonArray[2].jsonObject["max"]!!.jsonPrimitive.doubleOrNull!!, 0.001)
        // بدون سقف ثبت‌شده، بارم خود آزمون ملاک است
        val plain = ReportCardPayload.forClass("کلاس", exams, rows)
        assertEquals(15.0, plain["cols"]!!.jsonArray[2].jsonObject["max"]!!.jsonPrimitive.doubleOrNull!!, 0.001)
    }

    @Test
    fun `single student payload keeps the whole class so rank is real`() {
        val p = ReportCardPayload.forStudent("علی رضایی", "کلاس", exams, rows)
        assertEquals("student", p["scope"]!!.jsonPrimitive.content)
        assertEquals("علی رضایی", p["student"]!!.jsonObject["name"]!!.jsonPrimitive.content)
        assertEquals("کل کلاس باید برای رتبه فرستاده شود", 2, p["rows"]!!.jsonArray.size)
        assertEquals(2, p["classInfo"]!!.jsonObject["count"]!!.jsonPrimitive.doubleOrNull!!.toInt())
    }

    @Test
    fun `my grades payload maps score total and percent`() {
        val grades = listOf(
            StudentGradeCard("e1", "ریاضی", "ریاضی", 17.5, 20.0, "خوب بود", null),
            StudentGradeCard("e2", "فیزیک", "فیزیک", 6.0, 15.0, "", null)
        )
        val p = ReportCardPayload.forGrades("زهرا کریمی", grades, today = JalaliDate(1405, 8, 14))
        assertEquals("student", p["scope"]!!.jsonPrimitive.content)
        val cols = p["cols"]!!.jsonArray
        assertEquals(2, cols.size)
        assertEquals(20.0, cols[0].jsonObject["max"]!!.jsonPrimitive.doubleOrNull!!, 0.001)
        val row = p["rows"]!!.jsonArray[0].jsonObject
        assertEquals(17.5, row["vals"]!!.jsonObject["g0"]!!.jsonPrimitive.doubleOrNull!!, 0.001)
        assertEquals(40.0, row["pcts"]!!.jsonObject["g1"]!!.jsonPrimitive.doubleOrNull!!, 0.001)
        assertEquals("خوب بود", p["feedback"]!!.jsonPrimitive.content)
    }

    @Test
    fun `academic year switches at mehr`() {
        assertEquals("سال تحصیلی ۱۴۰۵–۱۴۰۶", ReportCardPayload.academicYear(JalaliDate(1405, 7, 1)))
        assertEquals("سال تحصیلی ۱۴۰۴–۱۴۰۵", ReportCardPayload.academicYear(JalaliDate(1405, 6, 31)))
    }

    @Test
    fun `engine lists all ten templates and three table structures`() {
        val js = src(engineJs)
        (1..10).forEach { id -> assertTrue("قالب $id باید در موتور باشد", "id: $id," in js) }
        assertTrue("ساختار ماتریس", "'matrix'" in js)
        assertTrue("ساختار تحلیل درس‌ها", "'subjects'" in js)
        assertTrue("ساختار ترکیبی", "'combined'" in js)
        assertTrue("رابط عمومی موتور", "window.ReportCard = {" in js)
        assertTrue("مرحلهٔ تناظر آزمون‌ها", "function showMapping(cfg)" in js)
        assertTrue("نقش مستمر و پایانی", "mostamar" in js && "payani" in js)
        assertTrue("نقش نوبت اول و دوم", "nobat1" in js && "nobat2" in js)
        assertTrue("فرمول نمرهٔ نوبت", "(a + 2 * b) / 3" in js)
        assertTrue("ارقام فارسی از کد ساخته شوند نه دستی", "String.fromCharCode(0x6F0" in js)
    }

    @Test
    fun `engine prints on A4 and hides its own toolbar when printing`() {
        val css = src(engineCss)
        assertTrue("اندازهٔ صفحه A4", "@page{size:A4;margin:0}" in css)
        assertTrue("برگهٔ A4", ".page{width:210mm;min-height:297mm" in css)
        assertTrue("نوار ابزار در چاپ پنهان شود", ".rc-bar,.rc-picker,.rc-map" in css)
        val html = src(engineHtml)
        assertTrue("پوستهٔ موتور، همان CSS و JS را صدا بزند", "reportcard.css" in html && "reportcard.js" in html)
    }

    @Test
    fun `app loads the shared engine from assets and prints A4 without payment`() {
        val d = src(dialog)
        assertTrue("نشانی موتور مشترک", "https://exam-print.local/reportcard/reportcard.html" in d)
        assertTrue("خواندن از assets", "view.context.assets.open(assetPath)" in d)
        assertTrue("پل چاپ", "@JavascriptInterface" in d && "fun print(templateId: String?)" in d)
        assertTrue("چاپ روی A4", "PrintAttributes.MediaSize.ISO_A4" in d)
        assertFalse("کارنامه رایگان است؛ نباید هزینه بگیرد", "chargePrint" in d || "printPay" in d || "ensurePrintPaid" in d)
    }

    @Test
    fun `both report screens open the picker instead of printing raw tables`() {
        val reports = src("app/src/main/java/ir/exam/app/ui/reports/ReportsScreen.kt")
        val results = src("app/src/main/java/ir/exam/app/ui/reports/StudentResultsScreen.kt")
        assertTrue("کارنامهٔ کلاس", "ReportCardLauncher.forClass(" in reports)
        assertTrue("کارنامهٔ تکی معلم", "ReportCardLauncher.forStudent(" in reports)
        assertTrue("پنجرهٔ قالب‌ها در پنل کارنامه", "ReportCardPrintDialog(" in reports)
        assertTrue("کارنامهٔ دانش‌آموز", "ReportCardLauncher.forGrades(" in results)
        assertTrue("پنجرهٔ قالب‌ها در نتایج من", "ReportCardPrintDialog(" in results)
        assertTrue("جایگزین خطا: همان PDF سادهٔ قبلی", "pdfExporter.launch(\"my-grades.pdf\")" in results)
        assertTrue("قرارداد V264 حفظ شود", "ActivityResultContracts.CreateDocument(\"application/pdf\")" in results)
    }

    @Test
    fun `site embeds the same engine and wires every report card button`() {
        val build = src("site/build_site.py")
        assertTrue("موتور کارنامه در سازندهٔ سایت", "def build_reportcard_engine():" in build)
        assertTrue("تعبیه در موتورها", "reportcard: %s" in build)
        assertTrue("فایل میزبان سایت در هسته", "reportcard.js" in build)
        val host = src("site/src/reportcard.js")
        assertTrue("پل چاپ سایت", "__reportCardBridge" in host)
        assertTrue("آداپتور کارنامهٔ دانش‌آموز", "function fromStudentGrades(" in host)
        assertTrue("آداپتور معلم", "function fromTeacher(" in host)
        assertTrue("آداپتور مدیر", "function fromManagerClass(" in host)
        assertTrue("نمرهٔ سایت از total_grade خوانده شود", "g.total_grade" in host && "g.total_score" in host)
        assertTrue("دکمهٔ کارنامهٔ دانش‌آموز", "P.fromStudentGrades(grades)" in src("site/src/app.js"))
        val extras = src("site/src/extras.js")
        assertTrue("دکمهٔ کارنامهٔ تکی معلم", "P.fromTeacher(st.rows || [], sel, cls, r.name)" in extras)
        assertTrue("دکمهٔ کارنامهٔ کلاس معلم", "P.fromTeacher(rows, sel, cls, null)" in extras)
        assertTrue("دکمهٔ کارنامهٔ کلاس مدیر", "P.fromManagerClass(d)" in src("site/src/admin.js"))
    }

    @Test
    fun `long reports paginate and totals stay global`() {
        val js = src(engineJs)
        assertTrue("صفحهٔ ادامه فقط جدول اصلی را دارد", "function contBody(m, cfg)" in js)
        assertTrue("آزمون‌های هم‌درس از هم جدا نشوند", "function chunkCols(m, cfg, limit)" in js)
        assertTrue("جمع کل از همهٔ ستون‌ها، نه تکهٔ جاری", "m._allCols || m.cols" in js)
        assertTrue("معدل از همهٔ درس‌ها", "m._allSubjects || m.subjects" in js)
        assertTrue("جاگذاری خودکار روی A4", "state.limit = Math.max(2, Math.floor(" in js)
        assertTrue("شمارهٔ ردیف در صفحهٔ ادامه سراسری است", "m._offset" in js)
    }

    @Test
    fun `web e2e covers picker preview and class form`() {
        val e2e = src("scripts/web_ui_e2e.py")
        assertTrue("گام انتخابگر", "09b_report_card_templates.png" in e2e)
        assertTrue("گام پیش‌نمایش", "09c_report_card_preview.png" in e2e)
        assertTrue("گام فرم کلاس", "09d_report_card_class_form.png" in e2e)
        assertTrue("سنجش سرریز A4", "1123" in e2e)
        assertTrue("۱۰ قالب", "card_count == 10" in e2e)
    }

    @Test
    fun `engine has no syntax level leftovers and no network dependency`() {
        val js = src(engineJs)
        assertFalse("موتور نباید به شبکه وابسته باشد", "http://" in js || "https://" in js.replace("onlineexam.ir", ""))
        assertFalse("دادهٔ ساختگی نباید در موتور بماند", "سارا محمدی" in js || "زهرا کریمی" in js)
        assertFalse("کد دیباگ باقی نماند", "console.log(" in js)
    }
}
