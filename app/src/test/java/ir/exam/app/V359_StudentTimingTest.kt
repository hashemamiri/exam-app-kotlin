package ir.exam.app

import ir.exam.app.domain.exam.ExamTiming
import ir.exam.app.domain.exam.StudentTiming
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V359 — سمت دانش‌آموز: زمان هر سؤال، قفل، و حالت آفلاین.
 *
 * پیشینه: در V357 این قابلیت‌ها فقط در سایت ساخته شد. لایهٔ دادهٔ اپ
 * آماده بود ولی هیچ UI آن را صدا نمی‌زد. V359 آن شکاف را بست.
 */
class V359_StudentTimingTest {

    private fun root(): File {
        var d = File(System.getProperty("user.dir") ?: ".")
        while (!File(d, "settings.gradle.kts").exists() && d.parentFile != null) d = d.parentFile!!
        return d
    }

    private fun src(path: String) = File(root(), path).readText()

    private val now = 1_000_000L

    // ------------------------------------------------------------------
    // ساعت سؤال
    // ------------------------------------------------------------------

    @Test
    fun serverDeadlineIsAlignedToLocalClock() {
        // سرور ۵ ثانیه جلوتر از دستگاه است؛ مهلت باید تراز شود.
        val c = StudentTiming.applySync(
            previous = null, limited = true, locked = false,
            expiresAtMs = now + 65_000L, serverNowMs = now + 5_000L,
            seconds = 60, localNowMs = now
        )
        assertEquals(now + 60_000L, c.deadlineMs)
        assertTrue(c.limited)
        assertFalse(c.locked)
    }

    @Test
    fun unlimitedQuestionHasNoClock() {
        val c = StudentTiming.applySync(
            previous = null, limited = false, locked = false,
            expiresAtMs = 0L, serverNowMs = now, seconds = 0, localNowMs = now
        )
        assertFalse(c.limited)
        assertEquals("", StudentTiming.clockText(c, now))
        assertFalse(StudentTiming.isLocked(c, now))
    }

    @Test
    fun lockNeverReopensEvenIfServerSaysUnlimited() {
        val locked = StudentTiming.QuestionClock(now + 10_000L, true, 60)
        // سرور در پاسخ بعدی می‌گوید محدود نیست؛ قفل نباید باز شود.
        val after = StudentTiming.applySync(
            previous = locked, limited = false, locked = false,
            expiresAtMs = 0L, serverNowMs = now, seconds = 0, localNowMs = now
        )
        assertTrue("قفل یک‌طرفه است", after.locked)
        assertTrue(StudentTiming.isLocked(after, now))
    }

    @Test
    fun lockNeverReopensOnRepeatedSync() {
        val locked = StudentTiming.QuestionClock(now - 1L, true, 60)
        val after = StudentTiming.applySync(
            previous = locked, limited = true, locked = false,
            expiresAtMs = now + 60_000L, serverNowMs = now, seconds = 60, localNowMs = now
        )
        assertTrue("بازگشت به سؤال قفل‌شده آن را باز نمی‌کند", after.locked)
    }

    @Test
    fun expiredDeadlineLocksQuestion() {
        val c = StudentTiming.QuestionClock(now - 1L, false, 60)
        assertTrue(StudentTiming.isLocked(c, now))
        assertFalse(StudentTiming.isLocked(c, now - 5_000L))
    }

    @Test
    fun clockTextCountsDownAndStopsWhenLocked() {
        val c = StudentTiming.QuestionClock(now + 65_000L, false, 90)
        // formatClock دقیقه را صفرپر نمی‌کند (۱:۰۵ نه ۰۱:۰۵) — همان
        // قرارداد سایت. انتظار اولیهٔ این آزمون غلط بود.
        assertEquals("1:05", StudentTiming.clockText(c, now))
        val locked = c.copy(locked = true)
        assertEquals("", StudentTiming.clockText(locked, now))
    }

    @Test
    fun warningAppearsInTheLastQuarter() {
        val c = StudentTiming.QuestionClock(now + 80_000L, false, 80)
        assertFalse("ابتدا هشدار نیست", StudentTiming.isWarning(c, now))
        assertTrue("۲۰ ثانیهٔ آخر از ۸۰ هشدار است", StudentTiming.isWarning(c, now + 61_000L))
    }

    @Test
    fun warningFloorIsThirtySecondsForLongQuestions() {
        val c = StudentTiming.QuestionClock(now + 600_000L, false, 600)
        // یک‌چهارم ۶۰۰ ثانیه = ۱۵ ثانیه؛ آستانه باید ۱۵۰ باشد نه ۳۰.
        assertFalse(StudentTiming.isWarning(c, now + 400_000L))
        assertTrue(StudentTiming.isWarning(c, now + 460_000L))
    }

    @Test
    fun nullClockIsNeverLockedAndNeverWarns() {
        assertFalse(StudentTiming.isLocked(null, now))
        assertFalse(StudentTiming.isWarning(null, now))
        assertEquals("", StudentTiming.clockText(null, now))
    }

    // ------------------------------------------------------------------
    // حالت آفلاین
    // ------------------------------------------------------------------

    @Test
    fun offlineStatusGoesNoneReadyUsed() {
        assertEquals(StudentTiming.OfflineStatus.NONE, StudentTiming.offlineStatus(null, now))
        val p = StudentTiming.OfflinePack("tok", now + 60_000L, false, now)
        assertEquals(StudentTiming.OfflineStatus.READY, StudentTiming.offlineStatus(p, now))
        assertEquals(StudentTiming.OfflineStatus.USED, StudentTiming.offlineStatus(p.copy(used = true), now))
    }

    @Test
    fun expiredPackIsNotReady() {
        val p = StudentTiming.OfflinePack("tok", now - 1L, false, now - 60_000L)
        assertEquals(StudentTiming.OfflineStatus.EXPIRED, StudentTiming.offlineStatus(p, now))
        // نکته: «منقضی» برای نمایش است، ولی سرور ۱۲۰ ثانیه رواداری
        // می‌دهد؛ پس یک میلی‌ثانیه پس از مهلت هنوز پذیرفته می‌شود.
        // انتظار اولیهٔ این آزمون (رد فوری) با قرارداد سرور نمی‌خواند.
        assertTrue("در رواداری هنوز مجاز است", StudentTiming.canSubmitOffline(p, now))
        assertFalse("پس از رواداری رد می‌شود",
            StudentTiming.canSubmitOffline(p, now + ExamTiming.OFFLINE_GRACE_MS + 1L))
    }

    @Test
    fun offlineGraceMatchesServerContract() {
        val deadline = now
        val p = StudentTiming.OfflinePack("tok", deadline, false, now - 60_000L)
        // رواداری سرور ۱۲۰ ثانیه است؛ دقیقاً روی مرز مجاز، یک میلی‌ثانیه بعد نه.
        assertTrue(StudentTiming.canSubmitOffline(p, deadline + ExamTiming.OFFLINE_GRACE_MS))
        assertFalse(StudentTiming.canSubmitOffline(p, deadline + ExamTiming.OFFLINE_GRACE_MS + 1L))
    }

    @Test
    fun usedPackCannotBeSubmittedTwice() {
        val p = StudentTiming.OfflinePack("tok", now + 60_000L, true, now)
        assertFalse(StudentTiming.canSubmitOffline(p, now))
    }

    @Test
    fun packWithoutTokenIsNotTaken() {
        val p = StudentTiming.OfflinePack("", now + 60_000L, false, now)
        assertFalse(p.taken)
        assertEquals(StudentTiming.OfflineStatus.NONE, StudentTiming.offlineStatus(p, now))
        assertFalse(StudentTiming.canSubmitOffline(p, now))
    }

    @Test
    fun practiceExamHasNoOfflinePack() {
        assertFalse(StudentTiming.canTakePack(null, practice = true))
        assertFalse(StudentTiming.canTakePack(StudentTiming.OfflinePack("t", now, true, now), practice = true))
    }

    @Test
    fun canRetakeAfterUse() {
        val used = StudentTiming.OfflinePack("tok", now + 1000L, true, now)
        assertTrue(StudentTiming.canTakePack(used, practice = false))
        val ready = used.copy(used = false)
        assertFalse("بستهٔ آماده دوباره گرفته نمی‌شود", StudentTiming.canTakePack(ready, false))
    }

    @Test
    fun everyStatusHasAPersianLabelAndHint() {
        StudentTiming.OfflineStatus.entries.forEach { s ->
            assertTrue("برچسب خالی: $s", StudentTiming.offlineLabel(s).isNotBlank())
            assertTrue("راهنما خالی: $s", StudentTiming.offlineHint(s).isNotBlank())
        }
        assertEquals("آماده", StudentTiming.offlineLabel(StudentTiming.OfflineStatus.READY))
        assertEquals("مصرف‌شده", StudentTiming.offlineLabel(StudentTiming.OfflineStatus.USED))
    }

    @Test
    fun offlineHintMentionsTeacherSeesOfflineFlag() {
        val hint = StudentTiming.offlineHint(StudentTiming.OfflineStatus.NONE)
        assertTrue("باید بگوید نظارت زنده ممکن نیست", hint.contains("نظارت زنده"))
        assertTrue("باید نشان آفلاین را توضیح دهد", hint.contains("آفلاین"))
    }

    // ------------------------------------------------------------------
    // قالب پاسخ
    // ------------------------------------------------------------------

    @Test
    fun responsesKeepQuestionOrderAndBlanksForUnanswered() {
        val ids = listOf("q1", "q2", "q3")
        val answers = mapOf("q1" to "الف", "q3" to "ج")
        assertEquals(listOf("الف", "", "ج"), StudentTiming.responsesInOrder(ids, answers))
    }

    @Test
    fun emptyExamGivesEmptyResponses() {
        assertTrue(StudentTiming.responsesInOrder(emptyList(), mapOf("x" to "y")).isEmpty())
    }

    // ------------------------------------------------------------------
    // سیم‌کشی: منطق باید واقعاً به UI وصل باشد
    // ------------------------------------------------------------------

    @Test
    fun studentViewModelCallsTheThreeTimingEndpoints() {
        val vm = src("app/src/main/java/ir/exam/app/ui/student/StudentExamViewModel.kt")
        assertTrue("ساعت سؤال گرفته نمی‌شود", vm.contains("exams.syncQuestionTiming("))
        assertTrue("بستهٔ آفلاین گرفته نمی‌شود", vm.contains("exams.offlinePack("))
        assertTrue("ارسال آفلاین وصل نیست", vm.contains("exams.submitOffline("))
        assertTrue("سؤال باید هنگام جابه‌جایی همگام شود", vm.contains("syncCurrentQuestion()"))
    }

    @Test
    fun studentScreenShowsClockLockAndOfflineCard() {
        val screen = src("app/src/main/java/ir/exam/app/ui/student/StudentExamScreen.kt")
        assertTrue("تراشهٔ زمان نیست", screen.contains("QuestionClockChip("))
        assertTrue("کارت قفل نیست", screen.contains("QuestionLockedCard()"))
        assertTrue("کارت آفلاین نیست", screen.contains("OfflineExamCard("))
        val home = src("app/src/main/java/ir/exam/app/ui/student/StudentHomeScreen.kt")
        assertTrue("داشبورد دکمه‌ها را وصل نکرده", home.contains("viewModel::takeOfflinePack"))
        assertTrue("ارسال آفلاین وصل نیست", home.contains("viewModel::submitOffline"))
    }

    @Test
    fun repositoryOverridesTheSharedInterface() {
        val repo = src("app/src/main/java/ir/exam/app/data/repository/SupabaseStudentExamRepository.kt")
        assertTrue(repo.contains("override suspend fun syncQuestionTiming"))
        assertTrue(repo.contains("override suspend fun offlinePack"))
        assertTrue(repo.contains("override suspend fun submitOffline"))
        val iface = src("app/src/main/java/ir/exam/app/domain/repository/ExamRepository.kt")
        assertTrue("اینترفیس باید پیش‌فرض بی‌خطر داشته باشد", iface.contains("StudentQuestionTiming()"))
    }

    @Test
    fun answerEncodingIsSharedWithSectionSync() {
        val vm = src("app/src/main/java/ir/exam/app/ui/student/StudentExamViewModel.kt")
        assertTrue("تابع مشترک نیست", vm.contains("private fun encodeAnswer("))
        // نباید دو پیاده‌سازی جدا بماند و از هم دور بیفتد
        assertEquals("کدگذاری باید یک‌جا باشد", 1,
            Regex("is MatchingAnswer -> JsonObject\\(").findAll(vm).count())
    }

    @Test
    fun autoGradeApprovalAsksBeforeActing() {
        val vm = src("app/src/main/java/ir/exam/app/ui/grading/GradingViewModel.kt")
        assertTrue("پیش‌آگاهی خوانده نمی‌شود", vm.contains("repository.autoGradeInfo("))
        assertTrue("پنجرهٔ تأیید نیست", vm.contains("autoGradeNotice"))
        val screen = src("app/src/main/java/ir/exam/app/ui/grading/GradingScreen.kt")
        assertTrue("دکمه باید نخست پیش‌آگاهی بخواهد",
            screen.contains("onClick = viewModel::requestAutoGradeApproval"))
        assertFalse("دکمه نباید بی‌مقدمه تأیید کند",
            screen.contains("OutlinedButton(onClick = viewModel::approveAutoGrades)"))
    }

    @Test
    fun touchTargetsMeetAccessibilityMinimum() {
        val css = src("site/src/site.css")
        assertTrue("تراشهٔ فیلتر باید ۴۴px باشد", Regex("\\.m-fchip\\{[^}]*min-height:44px").containsMatchIn(css))
        assertTrue("دکمهٔ متنی باید ۴۴px باشد", Regex("\\.m-textbtn\\{[^}]*min-height:44px").containsMatchIn(css))
        assertTrue("تصمیم باید مستند شود", css.contains("کمینهٔ ارتفاع هدف لمس"))
    }

    @Test
    fun touchRegressionTestExistsAndUsesRealTouch() {
        val t = src("scripts/check_v359_touch.py")
        assertTrue("باید دستگاه لمسی باشد", t.contains("has_touch=True"))
        assertTrue("باید لمس واقعی بزند", t.contains(".tap()"))
        assertTrue("سنجهٔ غلط پیشین باید مستند شود", t.contains("سنجهٔ") && t.contains("همه پایه‌ها"))
    }

    // ------------------------------------------------------------------
    // V359.2 — نگهبان ارجاع اسکریپت‌های گردش‌کار
    // ------------------------------------------------------------------

    @Test
    fun workflowScriptGuardCatchesLoopBuiltReferences() {
        val guard = src("scripts/check_workflow_scripts.py")
        assertTrue("باید سرِ حلقه را بگیرد", guard.contains("loop_start"))
        assertTrue("باید بدنه را دستی بخواند", guard.contains("text.find(\"; do\""))
        assertTrue("حلقهٔ پایتونی باید رد شود", guard.contains("[](){}"))
        assertTrue("پیام راهنما باید فارسی باشد", guard.contains("بستهٔ ترمیم"))
        // این نگهبان باید نخستین گام باشد، نه آخر حلقه
        val android = src(".github/workflows/android.yml")
        val guardAt = android.indexOf("python3 scripts/check_workflow_scripts.py")
        val loopAt = android.indexOf("for g in check_kotlin_braces")
        assertTrue("نگهبان در android.yml نیست", guardAt > 0)
        assertTrue("باید پیش از حلقه باشد", guardAt < loopAt)
        val site = src(".github/workflows/site.yml")
        assertTrue("در site.yml هم باید باشد", site.contains("check_workflow_scripts.py"))
    }

    @Test
    fun lostGuardFileIsBackInRepository() {
        val f = java.io.File(root(), "scripts/check_v357_sql_sync.py")
        assertTrue("فایل نگهبان V357 باید موجود باشد", f.isFile)
        val builder = java.io.File(root(), "scripts/build_v357_sql.py")
        assertTrue("سازندهٔ SQL یک‌جا باید موجود باشد", builder.isFile)
    }

    @Test
    fun versionAndChangelogAgree() {
        val version = src("text/APP_VERSION.txt").trim()
        assertTrue("شمارهٔ نسخه نباید ثابت شود", version.split(".").size == 3 && version.isNotBlank())
        val changelog = src("text/CHANGELOG_FA.txt")
        val first = changelog.lines().first()
        assertTrue("تیتر باید V360 باشد", first.startsWith("V360"))
        assertTrue("دامنه باید اپ باشد", first.contains("اپ $version"))
    }
}
