package ir.exam.app

import ir.exam.app.core.calendar.PersianDigits
import ir.exam.app.core.update.ReleaseNotes
import ir.exam.app.domain.model.AppealStatus
import ir.exam.app.domain.model.BlindMarking
import ir.exam.app.domain.model.GradeAppeal
import ir.exam.app.domain.model.GradeHistoryRow
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V354 — بستهٔ ۱: اعتراض به نمره، تاریخچهٔ تغییر نمره، تصحیح ناشناس.
 *
 * چرا این آزمون نوشته شد:
 *  ۱) نسخهٔ نخست مهاجرت SQL، شناسه‌ها را uuid فرض کرده بود، در حالی که در
 *     پایگاه‌دادهٔ واقعی answers.id و exams.id از نوع text هستند. نتیجه،
 *     خطای «operator does not exist: text = uuid» و شکست کل نصب بود.
 *     این آزمون نوع text را پین می‌کند تا آن اشتباه برنگردد.
 *  ۲) درهم‌سازی تصحیح ناشناس باید دقیقاً همان ترتیب سایت را بدهد، وگرنه
 *     معلم در اپ و سایت دو ترتیب متفاوت می‌بیند. مقادیر هش زیر از خود
 *     الگوریتم جاوااسکریپت سایت گرفته شده است.
 */
class V354_GradingAppealsTest {

    private fun root(): File {
        var d = File(System.getProperty("user.dir") ?: ".")
        while (!File(d, "settings.gradle.kts").exists() && d.parentFile != null) d = d.parentFile!!
        return d
    }

    private fun src(path: String): String {
        val f = File(root(), path)
        assertTrue("فایل پیدا نشد: $path", f.exists())
        return f.readText()
    }

    // ------------------------------------------------------------------
    // ۱) درهم‌سازی تصحیح ناشناس — آینهٔ دقیق سایت
    // ------------------------------------------------------------------

    @Test
    fun blindHashMatchesSiteAlgorithm() {
        // مقادیر از admin.js (تابع blindOrder) استخراج شده‌اند.
        assertEquals(3056, BlindMarking.hash("a1"))
        assertEquals(3057, BlindMarking.hash("a2"))
        assertEquals(1118293131, BlindMarking.hash("ans-12345678"))
        assertEquals(0, BlindMarking.hash(""))
        assertEquals(0, BlindMarking.hash(null))
        assertEquals(97, BlindMarking.hash("a"))
        assertEquals(3105, BlindMarking.hash("ab"))
        assertEquals(48528449, BlindMarking.hash("برگه"))
        // سرریز ۳۲ بیتی علامت‌دار: باید منفی شود، نه اینکه بزرگ بماند.
        assertEquals(-1343044828, BlindMarking.hash("11111111-1111-4111-8111-111111111111"))
    }

    @Test
    fun blindOrderIsStableAndAscending() {
        val ids = listOf("a2", "a1", "ab", "a")
        val ordered = BlindMarking.order(ids) { it }
        assertEquals(listOf("a", "a1", "a2", "ab"), ordered)
        // اجرای دوباره همان ترتیب را می‌دهد (پایدار است).
        assertEquals(ordered, BlindMarking.order(ordered) { it })
        // لیست ورودی دست‌نخورده می‌ماند.
        assertEquals(listOf("a2", "a1", "ab", "a"), ids)
    }

    @Test
    fun blindLabelUsesPersianDigits() {
        assertEquals("برگهٔ ۱", BlindMarking.label(0))
        assertEquals("برگهٔ ۲", BlindMarking.label(1))
        assertEquals("برگهٔ ۱۲", BlindMarking.label(11))
    }

    @Test
    fun blindDisplayNameHidesRealNameOnlyWhenOn() {
        assertEquals("علی رضایی", BlindMarking.displayName("علی رضایی", 0, false))
        assertEquals("برگهٔ ۱", BlindMarking.displayName("علی رضایی", 0, true))
    }

    // ------------------------------------------------------------------
    // ۲) وضعیت اعتراض
    // ------------------------------------------------------------------

    @Test
    fun appealStatusRoundTripsWireValues() {
        assertEquals(AppealStatus.OPEN, AppealStatus.fromWire("open"))
        assertEquals(AppealStatus.SECOND, AppealStatus.fromWire("second"))
        assertEquals(AppealStatus.ACCEPTED, AppealStatus.fromWire("accepted"))
        assertEquals(AppealStatus.REJECTED, AppealStatus.fromWire("rejected"))
        // مقدار ناشناخته نباید باعث سقوط صفحه شود.
        assertEquals(AppealStatus.OPEN, AppealStatus.fromWire("چیزی-که-نمی‌شناسم"))
        assertEquals(AppealStatus.OPEN, AppealStatus.fromWire(null))
        AppealStatus.entries.forEach { assertEquals(it, AppealStatus.fromWire(it.wire)) }
    }

    @Test
    fun onlyOpenAndSecondAreOpenAppeals() {
        assertTrue(AppealStatus.OPEN.isOpen)
        assertTrue(AppealStatus.SECOND.isOpen)
        assertFalse(AppealStatus.ACCEPTED.isOpen)
        assertFalse(AppealStatus.REJECTED.isOpen)
    }

    // ------------------------------------------------------------------
    // ۳) قید طول دلیل اعتراض — همان محدودیت پایگاه‌داده
    // ------------------------------------------------------------------

    @Test
    fun reasonLengthMatchesDatabaseConstraint() {
        assertEquals(5, GradeAppeal.REASON_MIN)
        assertEquals(2000, GradeAppeal.REASON_MAX)
        assertNotNull(GradeAppeal.reasonError(null))
        assertNotNull(GradeAppeal.reasonError(""))
        assertNotNull(GradeAppeal.reasonError("   "))
        assertNotNull(GradeAppeal.reasonError("کم"))
        // «کوتاه» در فارسی ۵ نویسه است، پس نباید رد شود.
        assertEquals(5, "کوتاه".length)
        assertNull(GradeAppeal.reasonError("کوتاه"))
        assertNull(GradeAppeal.reasonError("نمرهٔ سؤال تشریحی کم داده شده است"))
        assertNull(GradeAppeal.reasonError("x".repeat(2000)))
        assertNotNull(GradeAppeal.reasonError("x".repeat(2001)))
        // فاصلهٔ ابتدا و انتها نباید به شمار بیاید.
        assertNotNull(GradeAppeal.reasonError("  کم  "))
    }

    // ------------------------------------------------------------------
    // ۴) خواندن پاسخ سرور
    // ------------------------------------------------------------------

    @Test
    fun appealParsesServerRow() {
        val row = Json.parseToJsonElement(
            """
            {"id":"ap-1","answer_id":"a1","exam_id":"e1","exam_title":"ریاضی ۱",
             "student_id":"s1","student_name":"علی","question_index":-1,
             "reason":"نمره کم است","status":"second","second_score":17.5,
             "second_note":"بازبینی شد","resolution_note":null,
             "created_at":"2026-11-03T10:00:00Z","resolved_at":null}
            """.trimIndent()
        ).let { it as kotlinx.serialization.json.JsonObject }
        val appeal = GradeAppeal.fromJson(row)
        assertEquals("ap-1", appeal.id)
        assertEquals("a1", appeal.answerId)
        assertEquals("ریاضی ۱", appeal.examTitle)
        assertEquals("علی", appeal.studentName)
        assertTrue(appeal.isWholePaper)
        assertEquals("کل برگه", appeal.targetLabel)
        assertEquals(AppealStatus.SECOND, appeal.status)
        assertEquals(17.5, appeal.secondScore!!, 0.0001)
        assertNull(appeal.resolutionNote)
    }

    @Test
    fun appealQuestionTargetLabelIsOneBased() {
        val row = Json.parseToJsonElement(
            """{"id":"ap-2","answer_id":"a1","exam_id":"e1","question_index":2,
                "reason":"دلیل کافی","status":"open"}"""
        ).let { it as kotlinx.serialization.json.JsonObject }
        val appeal = GradeAppeal.fromJson(row)
        assertFalse(appeal.isWholePaper)
        assertEquals("سؤال ۳", appeal.targetLabel)
        assertNull(appeal.secondScore)
    }

    @Test
    fun appealListSkipsNonObjectEntries() {
        val array = Json.parseToJsonElement(
            """[{"id":"a","answer_id":"x","exam_id":"e","reason":"دلیل کافی","status":"open"}, 7, null]"""
        ) as JsonArray
        assertEquals(1, GradeAppeal.listFrom(array).size)
        assertEquals(0, GradeAppeal.listFrom(Json.parseToJsonElement("[]") as JsonArray).size)
    }

    @Test
    fun historyRowParsesScoresAsNumbersAndDetectsChange() {
        val array = Json.parseToJsonElement(
            """
            [{"id":1,"answer_id":"a1","student_name":"علی","changed_by_name":"معلم الف",
              "before_score":"15","after_score":"18","before_graded":"true",
              "after_graded":"true","created_at":"2026-11-03T10:00:00Z"},
             {"id":2,"answer_id":"a2","student_name":"رضا","changed_by_name":"معلم الف",
              "before_score":"10","after_score":"10","before_graded":"false",
              "after_graded":"true","created_at":"2026-11-03T11:00:00Z"}]
            """.trimIndent()
        ) as JsonArray
        val rows = GradeHistoryRow.listFrom(array)
        assertEquals(2, rows.size)
        assertEquals(15.0, rows[0].beforeScore!!, 0.0001)
        assertEquals(18.0, rows[0].afterScore!!, 0.0001)
        assertTrue("تغییر نمره باید تشخیص داده شود", rows[0].scoreChanged)
        assertFalse("تغییر وضعیت بدون تغییر نمره، تغییر نمره نیست", rows[1].scoreChanged)
        assertEquals(false, rows[1].beforeGraded)
        assertEquals(true, rows[1].afterGraded)
    }

    @Test
    fun historyRowFallsBackWhenNamesMissing() {
        val row = Json.parseToJsonElement("""{"id":3,"answer_id":"a1"}""")
            .let { it as kotlinx.serialization.json.JsonObject }
        val parsed = GradeHistoryRow.fromJson(row)
        assertEquals("دانش‌آموز", parsed.studentName)
        assertEquals("سامانه", parsed.changedByName)
        assertNull(parsed.beforeScore)
    }

    // ------------------------------------------------------------------
    // ۵) پین‌های ساختاری — سیم‌کشی واقعاً انجام شده باشد
    // ------------------------------------------------------------------

    @Test
    fun repositoryWiresAllFiveV354Rpcs() {
        val repo = src("app/src/main/java/ir/exam/app/data/repository/SupabaseGradingRepository.kt")
        listOf(
            "native_appeal_create_v354",
            "native_my_appeals_v354",
            "native_exam_appeals_v354",
            "native_appeal_resolve_v354",
            "native_grade_history_v354"
        ).forEach { assertTrue("تابع در مخزن صدا زده نشده: $it", repo.contains(it)) }
        // پارامترها باید با امضای پایگاه‌داده یکی باشند.
        assertTrue(repo.contains("put(\"p_answer\", answerId)"))
        assertTrue(repo.contains("put(\"p_exam\", examId)"))
        assertTrue(repo.contains("put(\"p_appeal\", appealId)"))
    }

    @Test
    fun gradingModesIncludeNewTabs() {
        val vm = src("app/src/main/java/ir/exam/app/ui/grading/GradingViewModel.kt")
        assertTrue(vm.contains("val GRADING_MODES: Set<String> = setOf(\"grading\", \"question\", \"attendance\", \"appeals\", \"history\")"))
        assertTrue(vm.contains("if (value == \"appeals\") loadAppeals()"))
        assertTrue(vm.contains("if (value == \"history\") loadGradeHistory()"))
        assertTrue("پذیرش نباید نمره را خودکار عوض کند", vm.contains("اگر لازم است نمره را در تب تصحیح اصلاح کنید"))
    }

    @Test
    fun gradingScreenShowsNewTabsAndBlindToggle() {
        val screen = src("app/src/main/java/ir/exam/app/ui/grading/GradingScreen.kt")
        assertTrue(screen.contains("Text(\"اعتراض‌ها\")"))
        assertTrue(screen.contains("Text(\"تاریخچهٔ نمره\")"))
        assertTrue(screen.contains("Text(\"تصحیح ناشناس\")"))
        assertTrue(screen.contains("AppealsContent(state, viewModel)"))
        assertTrue(screen.contains("GradeHistoryContent(state, viewModel)"))
        assertTrue(screen.contains("BlindMarking.apply(state.submissions, state.blindMarking)"))
    }

    @Test
    fun studentResultsScreenShowsAppealCard() {
        val screen = src("app/src/main/java/ir/exam/app/ui/reports/StudentResultsScreen.kt")
        assertTrue(screen.contains("StudentAppealsCard("))
        assertTrue(screen.contains("onSubmit = viewModel::submitAppeal"))
        val vm = src("app/src/main/java/ir/exam/app/ui/reports/StudentResultsViewModel.kt")
        assertTrue(vm.contains("repository.createAppeal(answerId, reason)"))
        // نبود مهاجرت نباید کارنامه را بشکند.
        assertTrue(vm.contains("repository.myAppeals().onSuccess"))
    }

    // ------------------------------------------------------------------
    // ۶) پین مهاجرت SQL — جلوگیری از بازگشت خطای نوع
    // ------------------------------------------------------------------

    @Test
    fun migrationUsesTextIdsNotUuid() {
        val sql = src("supabase/sql/SQL_GRADING_APPEALS_V354.sql")
        // ستون‌های شناسه باید text باشند؛ این همان خطای گزارش‌شدهٔ کاربر است.
        assertTrue(sql.contains("answer_id   text not null"))
        assertTrue(sql.contains("answer_id       text not null"))
        // امضای تابع‌ها باید text بگیرد، مانند بقیهٔ تابع‌های تصحیح.
        assertTrue(sql.contains("p_answer text, p_question integer, p_reason text"))
        assertTrue(sql.contains("native_exam_appeals_v354(p_exam text)"))
        assertTrue(sql.contains("native_grade_history_v354(p_exam text)"))
        // مقایسهٔ شناسه‌ها باید در دو طرف به text تبدیل شود.
        assertTrue(sql.contains("e.id::text = grade_history.exam_id"))
        assertTrue(sql.contains("e.id::text = grade_appeals.exam_id"))
        // سابقه باید فقط‌افزودنی بماند.
        assertTrue(sql.contains("revoke insert, update, delete, truncate on public.grade_history"))
        assertFalse("نباید روی سابقه سیاست نوشتن ساخته شود", sql.contains("for insert on public.grade_history"))
    }

    @Test
    fun checkScriptMatchesMigrationSignatures() {
        val check = src("supabase/sql/SQL_GRADING_APPEALS_V354_CHECK.sql")
        assertTrue(check.contains("native_appeal_create_v354(text,integer,text)"))
        assertTrue(check.contains("native_exam_appeals_v354(text)"))
        assertTrue(check.contains("native_grade_history_v354(text)"))
        assertFalse("امضای uuid نباید در فایل بررسی بماند", check.contains("native_exam_appeals_v354(uuid)"))
    }

    // ------------------------------------------------------------------
    // ۷) شکل بلوک تغییرات و نسخه
    // ------------------------------------------------------------------

    @Test
    fun versionIsBumpedAndChangelogHasAppScope() {
        assertEquals("1.03.71", src("text/APP_VERSION.txt").trim())
        val lines = src("text/CHANGELOG_FA.txt").lines()
        // همان دو بازه‌ای که آزمون‌های V208 و V322 می‌سنجند.
        listOf(40, 60).forEach { take ->
            val top = ReleaseNotes.parse(lines.take(take)).first()
            assertEquals("V354", top.version)
            assertTrue("بلوک بالا باید اپ را پوشش دهد", top.appAffecting)
            assertEquals("1.03.71", top.appVersion)
            assertTrue(top.bullets.isNotEmpty())
            assertTrue(top.bullets.size <= ReleaseNotes.MAX_BULLETS)
            top.bullets.forEach {
                assertFalse("جزئیات داخلی نباید در فهرست تغییرات باشد: $it", it.contains(Regex("\\bV\\d")))
            }
        }
        assertEquals("۱.۰۳.۷۱", PersianDigits.convert("1.03.71"))
    }
}
