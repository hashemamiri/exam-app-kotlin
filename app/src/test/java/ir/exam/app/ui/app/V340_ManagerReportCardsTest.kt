package ir.exam.app.ui.app

import ir.exam.app.core.calendar.JalaliDate
import ir.exam.app.data.repository.ManagerClassReport
import ir.exam.app.data.repository.ManagerClassStat
import ir.exam.app.data.repository.ManagerClassStats
import ir.exam.app.data.repository.ManagerMonthStat
import ir.exam.app.data.repository.ManagerReportExam
import ir.exam.app.data.repository.ManagerReportGrade
import ir.exam.app.data.repository.ManagerReportStudent
import ir.exam.app.data.repository.ManagerTopExam
import ir.exam.app.domain.model.OfficialPrintHeader
import ir.exam.app.ui.reports.ReportCardPayload
import java.io.File
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V340 — کارنامه برای مدیر، همانند معلم (کلاس / دانش‌آموز / دفترچهٔ مدرسه).
 *
 * قراردادهای نگهبان:
 *  • مدیر در اپ بخش مستقل «کارنامه» دارد و صفحهٔ REPORTS برای نقش مدیر مجاز است؛
 *  • دادهٔ کارنامهٔ مدیر فقط از RPCهای موجود ساخته می‌شود (بدون SQL تازه)؛
 *  • نمرهٔ تصحیح‌نشده و غایب هرگز به‌عنوان صفر وارد کارنامه نمی‌شود؛
 *  • دفترچهٔ مدرسه = نمای کلی + خلاصهٔ مقایسه + کارنامهٔ کامل هر کلاس؛
 *  • هر ۱۰ قالب در سطح مدرسه هم در دسترس است (فیلتر نمی‌شوند)؛
 *  • خروجی کارنامهٔ معلم و دانش‌آموز نسبت به V339.2 تغییر نمی‌کند.
 */
class V340_ManagerReportCardsTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "text/APP_VERSION.txt").isFile }
    private fun src(path: String) = File(root(), path).readText()

    private val engineJs = "app/src/main/assets/reportcard/reportcard.js"
    private val engineCss = "app/src/main/assets/reportcard/reportcard.css"
    private val screen = "app/src/main/java/ir/exam/app/ui/manager/ManagerReportsScreen.kt"
    private val app = "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt"
    private val cards = "app/src/main/java/ir/exam/app/ui/app/TeacherManagementCardsScreen.kt"
    private val repo = "app/src/main/java/ir/exam/app/data/repository/SupabaseManagerRepository.kt"
    private val payload = "app/src/main/java/ir/exam/app/ui/reports/ReportCardPayload.kt"
    private val launcher = "app/src/main/java/ir/exam/app/ui/reports/ReportCardLauncher.kt"
    private val host = "site/src/reportcard.js"
    private val admin = "site/src/admin.js"

    private val today = JalaliDate(1405, 8, 14)
    private val header = OfficialPrintHeader(school = "دبیرستان نمونهٔ شهید بهشتی")

    private val report = ManagerClassReport(
        className = "دهم ۱۰۱",
        exams = listOf(
            ManagerReportExam("e1", "ریاضی — مستمر", 20.0),
            ManagerReportExam("e2", "فیزیک", 15.0)
        ),
        students = listOf(
            ManagerReportStudent("s1", "سارا محمدی", "sara", mapOf("e1" to ManagerReportGrade(18.0, 20.0, true), "e2" to ManagerReportGrade(12.0, 15.0, true))),
            ManagerReportStudent("s2", "علی رضایی", "ali", mapOf("e1" to ManagerReportGrade(0.0, 20.0, false), "e2" to ManagerReportGrade(9.0, 15.0, true))),
            ManagerReportStudent("s3", "مریم کریمی", "maryam", emptyMap())
        )
    )

    private val stats = ManagerClassStats(
        classes = listOf(
            ManagerClassStat("c1", "دهم ۱۰۱", "دهم", "ریاضی", "دبیرستان نمونهٔ شهید بهشتی", "آقای معلم ۱", 20, 16, 288, 270, 58.4, 12.0, 3),
            ManagerClassStat("c2", "دهم ۱۰۲", "دهم", "تجربی", "دبیرستان نمونهٔ شهید بهشتی", "آقای معلم ۲", 23, 16, 331, 268, 62.8, 11.0, 4)
        ),
        months = listOf(ManagerMonthStat("1405-03", 60, 61.0)),
        topExams = listOf(ManagerTopExam("ریاضی — مستمر", "آقای معلم ۱", 60, 64.0))
    )

    // ---------------------------------------------------------------- دادهٔ کارنامهٔ کلاس مدیر

    @Test
    fun managerClassPayloadUsesOnlyGradedScores() {
        val p = ReportCardPayload.forManagerClass(report, header, "آقای معلم ۱", "دبیرستان نمونهٔ شهید بهشتی", today)
        assertEquals("class", p["scope"]!!.jsonPrimitive.content)
        assertEquals("دهم ۱۰۱", p["className"]!!.jsonPrimitive.content)
        val rows = p["rows"]!!.jsonArray
        assertEquals(3, rows.size)
        val ali = rows[1].jsonObject["vals"]!!.jsonObject
        assertTrue("نمرهٔ تصحیح‌نشده باید غایب بماند", ali["e1"] is JsonNull)
        assertEquals(9.0, ali["e2"]!!.jsonPrimitive.doubleOrNull!!, 0.001)
        val maryam = rows[2].jsonObject["vals"]!!.jsonObject
        assertTrue("دانش‌آموز بدون پاسخ هیچ نمره‌ای ندارد", maryam.values.all { it is JsonNull })
    }

    @Test
    fun managerClassColumnsCarryPeakMax() {
        val p = ReportCardPayload.forManagerClass(report, header, today = today)
        val cols = p["cols"]!!.jsonArray
        assertEquals(2, cols.size)
        assertEquals("ریاضی — مستمر", cols[0].jsonObject["title"]!!.jsonPrimitive.content)
        assertEquals(20.0, cols[0].jsonObject["max"]!!.jsonPrimitive.doubleOrNull!!, 0.001)
        assertEquals(15.0, cols[1].jsonObject["max"]!!.jsonPrimitive.doubleOrNull!!, 0.001)
        assertEquals(3, p["classInfo"]!!.jsonObject["count"]!!.jsonPrimitive.intOrNull)
    }

    @Test
    fun managerStudentPayloadKeepsClassContextForRank() {
        val p = ReportCardPayload.forManagerStudent(report, "سارا محمدی", header, "دبیرستان نمونهٔ شهید بهشتی", today)
        assertEquals("student", p["scope"]!!.jsonPrimitive.content)
        assertEquals("سارا محمدی", p["student"]!!.jsonObject["name"]!!.jsonPrimitive.content)
        // رتبه از کل کلاس حساب می‌شود، پس همهٔ سطرها می‌مانند
        assertEquals(3, p["rows"]!!.jsonArray.size)
        assertEquals("", p["teacher"]!!.jsonPrimitive.content)
    }

    // ---------------------------------------------------------------- دفترچهٔ مدرسه

    @Test
    fun managerSchoolPayloadCarriesOverviewAndPerClassReports() {
        val classPayloads = listOf(
            ReportCardPayload.forManagerClass(report, header, "آقای معلم ۱", today = today),
            ReportCardPayload.forManagerClass(report, header, "آقای معلم ۲", today = today)
        )
        val p = ReportCardPayload.forManagerSchool(stats, classPayloads, header, today)
        assertEquals("school", p["scope"]!!.jsonPrimitive.content)
        assertEquals(2, p["classes"]!!.jsonArray.size)
        assertEquals(1, p["months"]!!.jsonArray.size)
        assertEquals(1, p["topExams"]!!.jsonArray.size)
        assertEquals(2, p["reports"]!!.jsonArray.size)
        assertEquals("دبیرستان نمونهٔ شهید بهشتی", p["school"]!!.jsonObject["name"]!!.jsonPrimitive.content)
        val k = p["classes"]!!.jsonArray[0].jsonObject
        assertEquals("c1", k["id"]!!.jsonPrimitive.content)
        assertEquals(58.4, k["average_percent"]!!.jsonPrimitive.doubleOrNull!!, 0.001)
    }

    @Test
    fun emptySchoolStillBuildsBooklet() {
        val empty = ManagerClassStats(emptyList(), emptyList(), emptyList())
        val p = ReportCardPayload.forManagerSchool(empty, emptyList(), header, today)
        assertEquals("school", p["scope"]!!.jsonPrimitive.content)
        assertEquals(0, p["reports"]!!.jsonArray.size)
    }

    // ---------------------------------------------------------------- موتور: سطح مدرسه

    @Test
    fun engineRendersSchoolScopeWithAllTenTemplates() {
        val js = src(engineJs)
        assertTrue("payload.scope === 'school'", js.contains("payload.scope === 'school'") || js.contains("p.scope === 'school'"))
        assertTrue(js.contains("function renderSchool("))
        assertTrue(js.contains("function schoolOverview("))
        assertTrue(js.contains("function schoolSummaryPayload("))
        assertTrue(js.contains("function schoolClassPayload("))
        assertTrue(js.contains("function schoolCols("))
        // هر ۱۰ قالب بدون فیلتر در سطح مدرسه هم فهرست می‌شوند
        for (i in 1..10) assertTrue("template $i", js.contains("id: $i,") || js.contains("id: $i "))
        assertFalse("قالب‌ها در سطح مدرسه فیلتر نمی‌شوند", js.contains("scope === 'school' && t.id > 5"))
    }

    @Test
    fun engineSchoolLabelsAreClassAware() {
        val js = src(engineJs)
        assertTrue(js.contains("rowLabel"))
        assertTrue(js.contains("countLabel"))
        assertTrue(js.contains("bestLabel"))
        assertTrue(js.contains("avgLabel"))
        assertTrue(js.contains("نام کلاس"))
        assertTrue(js.contains("تعداد کلاس"))
        assertTrue(js.contains("برترین کلاس"))
        assertTrue(js.contains("میانگین مدرسه"))
        // برچسب پیش‌فرض نباید به خودش ارجاع بدهد (باگ جای‌گذاری سراسری)
        assertFalse(js.contains("avgLabel: p.avgLabel || m.avgLabel"))
    }

    @Test
    fun engineAutoFitNeverExplodesPageCount() {
        val js = src(engineJs)
        assertTrue("بهترین حالت قبلی نگه داشته می‌شود", js.contains("bestLimit"))
        assertTrue("وقتی کوتاه‌تر نمی‌شود، فشرده‌سازی", js.contains("function tighten("))
        assertTrue(js.contains("classList.add('tight')"))
        assertTrue(js.contains("classList.add('tighter')"))
        val css = src(engineCss)
        assertTrue(css.contains(".page.tight"))
        assertTrue(css.contains(".page.tighter"))
        // هندسهٔ کاغذ عوض نمی‌شود
        assertTrue(css.contains(".page{width:210mm"))
    }

    // ---------------------------------------------------------------- اپ: بخش کارنامهٔ مدیر

    @Test
    fun managerHasDedicatedReportCardSection() {
        val s = src(screen)
        assertTrue(s.contains("fun ManagerReportsScreen("))
        assertTrue(s.contains("کارنامهٔ کلاس"))
        assertTrue(s.contains("کارنامهٔ دانش‌آموز"))
        assertTrue(s.contains("کارنامهٔ مدرسه (دفترچه)"))
        assertTrue(s.contains("ReportCardPrintDialog("))
        assertTrue(s.contains("ReportCardLauncher.forManagerClass("))
        assertTrue(s.contains("ReportCardLauncher.forManagerStudent("))
        assertTrue(s.contains("ReportCardLauncher.forManagerSchool("))
        assertTrue("سقف تعداد کلاس در دفترچه", s.contains("MAX_BOOKLET_CLASSES"))
    }

    @Test
    fun reportsPageIsAllowedForManager() {
        val a = src(app)
        assertTrue(a.contains("MainPage.CARDS, MainPage.REPORTS, MainPage.SETTINGS"))
        assertTrue(a.contains("UserRole.MANAGER -> ir.exam.app.ui.manager.ManagerReportsScreen()"))
        assertTrue(a.contains("onReportCards = { page = MainPage.REPORTS }"))
        assertTrue("عنوان هدر مدیر", a.contains("if (user.role == UserRole.MANAGER) \"کارنامه\""))
    }

    @Test
    fun managerCardsStackHasReportCardEntry() {
        val c = src(cards)
        assertTrue(c.contains("onReportCards: () -> Unit"))
        assertTrue(c.contains("\"کارنامه (PDF)\""))
        assertTrue(c.contains("Design69Icons.ReportCard,"))
    }

    @Test
    fun launcherExposesThreeManagerEntries() {
        val l = src(launcher)
        assertTrue(l.contains("internal suspend fun forManagerClass("))
        assertTrue(l.contains("internal suspend fun forManagerStudent("))
        assertTrue(l.contains("internal suspend fun forManagerSchool("))
        val p = src(payload)
        assertTrue(p.contains("internal fun forManagerClass("))
        assertTrue(p.contains("internal fun forManagerStudent("))
        assertTrue(p.contains("internal fun forManagerSchool("))
    }

    /**
     * V340.1 — مدل‌های مدیر `internal` هستند، پس سازنده‌های کارنامه هم باید `internal` باشند.
     * اگر روزی این‌ها public شوند، کامپایلر کاتلین با
     * «Function 'public' exposes its 'internal' parameter type» ساخت را می‌شکند
     * (همان خطای CI نسخهٔ V340). این تست جلوی بازگشت آن را می‌گیرد.
     */
    @Test
    fun managerBuildersAreInternalLikeTheirModels() {
        val r = src(repo)
        for (model in listOf("ManagerClassReport", "ManagerClassStats", "ManagerClassStat", "ManagerMonthStat", "ManagerTopExam")) {
            assertTrue("مدل $model باید internal بماند", r.contains("internal data class $model("))
        }
        for (path in listOf(payload, launcher)) {
            val text = src(path)
            val leaked = Regex("(?m)^    fun forManager\\w*\\(").containsMatchIn(text) ||
                Regex("(?m)^    suspend fun forManager\\w*\\(").containsMatchIn(text)
            assertFalse("سازندهٔ کارنامهٔ مدیر در $path نباید public باشد", leaked)
        }
    }

    @Test
    fun managerDataComesFromExistingRpcsOnly() {
        val r = src(repo)
        assertTrue(r.contains("suspend fun classStats("))
        assertTrue(r.contains("\"native_manager_class_stats_v1\""))
        assertTrue(r.contains("\"native_manager_class_report_v1\""))
        val s = src(screen)
        assertTrue(s.contains("repo.classStats()"))
        assertTrue(s.contains("repo.classReport("))
        assertFalse("SQL تازه‌ای برای کارنامهٔ مدیر لازم نیست", s.contains("native_manager_report_cards"))
    }

    // ---------------------------------------------------------------- سایت

    @Test
    fun siteHostBuildsManagerStudentAndSchoolPayloads() {
        val h = src(host)
        assertTrue(h.contains("function fromManagerStudent("))
        assertTrue(h.contains("function fromManagerSchool("))
        assertTrue(h.contains("fromManagerStudent: fromManagerStudent"))
        assertTrue(h.contains("fromManagerSchool: fromManagerSchool"))
    }

    @Test
    fun siteManagerPanelWiresBothEntries() {
        val a = src(admin)
        assertTrue(a.contains("async function managerBooklet("))
        assertTrue(a.contains("P.fromManagerSchool(stats, reports)"))
        assertTrue(a.contains("P.fromManagerStudent(d,"))
        assertTrue(a.contains("کارنامهٔ مدرسه (PDF)"))
        assertFalse("چاپ سادهٔ مقایسه جای خود را به دفترچه داد", a.contains("S.printSection('مقایسهٔ کلاس‌ها', box)"))
    }

    // ---------------------------------------------------------------- حریم و نسخه

    @Test
    fun noNetworkEndpointsOrDemoDataInNewCode() {
        for (path in listOf(screen, payload, launcher, repo)) {
            val s = src(path)
            val urls = Regex("https?://[^\\s\"')]+").findAll(s).map { it.value }.filter { !it.contains("onlineexam.ir") }.toList()
            assertTrue("نشانی غیرمجاز در $path: $urls", urls.isEmpty())
            assertFalse("console.log در $path", s.contains("console.log("))
        }
    }

    @Test
    fun versionIsAtLeastV340AndChangelogHasBlock() {
        val v = src("text/APP_VERSION.txt").trim()
        val parts = v.split(".").map { it.toInt() }
        val floor = listOf(1, 3, 59)
        val cmp = (0..2).firstOrNull { parts[it] != floor[it] }?.let { parts[it].compareTo(floor[it]) } ?: 0
        assertTrue("نسخه باید دست‌کم 1.03.59 باشد (اکنون $v)", cmp >= 0)
        assertTrue(src("text/CHANGELOG_FA.txt").contains("V340 (اپ"))
    }
}
