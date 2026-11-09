package ir.exam.app

import ir.exam.app.core.update.ReleaseNotes
import ir.exam.app.domain.exam.ExamTiming
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V357 — بخش ۲: زمان اضافهٔ فردی، محدودیت زمان هر سؤال، آزمون آفلاین.
 *
 * مرزهای اصلی که این کلاس پین می‌کند:
 *  • مهلت را سرور تعیین می‌کند؛ اپ فقط تراز می‌کند و قفل را دور نمی‌زند.
 *  • زمان اضافه دلیل اجباری دارد تا قابل حسابرسی باشد.
 *  • بستهٔ آفلاین توکن یک‌بارمصرف دارد و برگه را «آفلاین» علامت می‌زند،
 *    چون نظارت زنده در حالت آفلاین ممکن نیست.
 *
 * برابری دقیق با جاوااسکریپت در V357_ExamTimingParityTest سنجیده می‌شود.
 */
class V357_ExamTimingTest {

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
    // ۱) تراز مهلت — ساعت دستگاه نباید مهلت را عوض کند
    // ------------------------------------------------------------------

    @Test
    fun serverDeadlineIsAlignedNotTrustedFromLocalClock() {
        // ساعت دستگاه یک ساعت جلوتر است؛ فاصلهٔ سرور باید حفظ شود
        val aligned = ExamTiming.alignDeadline(
            expiresAtMs = 1_000_000L + 60_000L, serverNowMs = 1_000_000L, localNowMs = 4_600_000L)
        assertEquals(4_660_000L, aligned)
        assertEquals(60_000L, ExamTiming.remainingMs(aligned, 4_600_000L))
        // بدون ساعت سرور، همان مهلت برگردانده می‌شود
        assertEquals(5000L, ExamTiming.alignDeadline(5000L, 0L, 999L))
        // بدون مهلت، بی‌نهایت
        assertEquals(0L, ExamTiming.alignDeadline(0L, 100L, 100L))
        assertEquals(-1L, ExamTiming.remainingMs(0L, 100L))
    }

    @Test
    fun remainingNeverGoesNegative() {
        assertEquals(0L, ExamTiming.remainingMs(1000L, 5000L))
        assertEquals(0L, ExamTiming.remainingMs(1000L, 1000L))
        assertEquals(1L, ExamTiming.remainingMs(1000L, 999L))
    }

    @Test
    fun clockFormatsMinutesAndHours() {
        assertEquals("0:00", ExamTiming.formatClock(0))
        assertEquals("1:05", ExamTiming.formatClock(65_000))
        assertEquals("1:02:03", ExamTiming.formatClock(3_723_000))
        assertEquals("∞", ExamTiming.formatClock(-1))
    }

    // ------------------------------------------------------------------
    // ۲) قفل سؤال
    // ------------------------------------------------------------------

    @Test
    fun expiredQuestionIsLockedEvenBeforeServerConfirms() {
        val s = ExamTiming.questionState(true, 1000L, 2000L, false)
        assertTrue("سؤال منقضی باید بی‌درنگ قفل دیده شود", s.locked)
        assertEquals(0L, s.remainingMs)
        assertFalse(s.warn)
    }

    @Test
    fun serverLockIsNeverOverridden() {
        // حتی اگر مهلت هنوز نرسیده، قفل سرور باید بماند
        val s = ExamTiming.questionState(true, 100_000L, 0L, true)
        assertTrue(s.locked)
        assertTrue(s.remainingMs > 0)
    }

    @Test
    fun unlimitedQuestionIsNeverLocked() {
        val s = ExamTiming.questionState(false, 0L, 999_999L, false)
        assertFalse(s.limited)
        assertFalse(s.locked)
        assertEquals("", s.clock)
        assertEquals(-1L, s.remainingMs)
    }

    @Test
    fun warnOnlyInLastThirtySeconds() {
        assertTrue(ExamTiming.questionState(true, 30_000L, 0L, false).warn)
        assertTrue(ExamTiming.questionState(true, 5_000L, 0L, false).warn)
        assertFalse(ExamTiming.questionState(true, 30_001L, 0L, false).warn)
        assertFalse(ExamTiming.questionState(true, 0L, 0L, false).warn)
    }

    // ------------------------------------------------------------------
    // ۳) زمان اضافه — دلیل اجباری
    // ------------------------------------------------------------------

    @Test
    fun grantRequiresReasonAndBoundedMinutes() {
        assertEquals("", ExamTiming.validateGrant(15, "مشکل یادگیری تأییدشده"))
        assertEquals("", ExamTiming.validateGrant(0, "لغو مجوز زمانی"))
        assertEquals("", ExamTiming.validateGrant(240, "دلیل کافی است"))
        assertTrue(ExamTiming.validateGrant(241, "دلیل کافی است").isNotEmpty())
        assertTrue(ExamTiming.validateGrant(-1, "دلیل کافی است").isNotEmpty())
        assertTrue("دلیل کوتاه باید رد شود", ExamTiming.validateGrant(15, "کم").isNotEmpty())
        assertTrue(ExamTiming.validateGrant(15, "").isNotEmpty())
        assertTrue(ExamTiming.validateGrant(15, null).isNotEmpty())
        assertTrue(ExamTiming.validateGrant(15, "x".repeat(501)).isNotEmpty())
    }

    // ------------------------------------------------------------------
    // ۴) قالب محدودیت‌ها
    // ------------------------------------------------------------------

    @Test
    fun limitsAcceptPersianDigitsAndAllKeyword() {
        val p = ExamTiming.parseLimits("همه = ۳۰۰\n۱ = ۶۰\n2 = ٩٠")
        assertTrue(p.errors.isEmpty())
        assertEquals(300, p.limits[-1])
        assertEquals(60, p.limits[0])
        assertEquals(90, p.limits[1])
        assertEquals(3, p.count)
    }

    @Test
    fun limitsRoundTripExactly() {
        val text = "همه = 300\n1 = 60\n2 = 90"
        val p = ExamTiming.parseLimits(text)
        assertEquals(text, ExamTiming.formatLimits(p.limits))
    }

    @Test
    fun limitsRejectOutOfRangeAndBadKeys() {
        assertTrue(ExamTiming.parseLimits("1 = 5").errors.isNotEmpty())
        assertTrue(ExamTiming.parseLimits("1 = 7201").errors.isNotEmpty())
        assertTrue(ExamTiming.parseLimits("0 = 60").errors.isNotEmpty())
        assertTrue(ExamTiming.parseLimits("x = 60").errors.isNotEmpty())
        assertTrue(ExamTiming.parseLimits("بدون مساوی").errors.isNotEmpty())
        assertTrue(ExamTiming.parseLimits("1 = abc").errors.isNotEmpty())
        assertTrue(ExamTiming.parseLimits("").errors.isEmpty())
        assertTrue(ExamTiming.parseLimits("# فقط توضیح\n\n").errors.isEmpty())
    }

    @Test
    fun limitsBoundsMatchServer() {
        assertEquals(10, ExamTiming.MIN_QUESTION_SECONDS)
        assertEquals(7200, ExamTiming.MAX_QUESTION_SECONDS)
        assertEquals(240, ExamTiming.MAX_EXTRA_MINUTES)
    }

    // ------------------------------------------------------------------
    // ۵) آفلاین — توکن و مهلت
    // ------------------------------------------------------------------

    @Test
    fun offlineNeedsUnusedTokenAndValidDeadline() {
        val pack = ExamTiming.OfflinePack("abc", false)
        assertTrue(ExamTiming.offlineSubmittable(pack, 0L, 60_000L).ok)
        assertFalse(ExamTiming.offlineSubmittable(null, 0L, 60_000L).ok)
        assertFalse(ExamTiming.offlineSubmittable(ExamTiming.OfflinePack("", false), 0L, 60_000L).ok)
        assertFalse("توکن مصرف‌شده باید رد شود",
            ExamTiming.offlineSubmittable(ExamTiming.OfflinePack("abc", true), 0L, 60_000L).ok)
        assertFalse(ExamTiming.offlineSubmittable(pack, 0L, 0L).ok)
    }

    @Test
    fun offlineGraceMatchesServer() {
        val pack = ExamTiming.OfflinePack("abc", false)
        assertEquals(120_000L, ExamTiming.OFFLINE_GRACE_MS)
        assertTrue("تا رواداری پذیرفته می‌شود",
            ExamTiming.offlineSubmittable(pack, 60_000L + 120_000L, 60_000L).ok)
        assertFalse("پس از رواداری رد می‌شود",
            ExamTiming.offlineSubmittable(pack, 60_000L + 120_001L, 60_000L).ok)
    }

    @Test
    fun offlineEventsAreCappedAndTrimmed() {
        val one = ExamTiming.pushEvent(emptyList(), "focus", 1234L, "خروج")
        assertEquals(1, one.size)
        assertEquals("focus", one[0].t)
        assertEquals(1234L, one[0].at)
        // گونهٔ خالی ثبت نمی‌شود
        assertEquals(0, ExamTiming.pushEvent(emptyList(), "  ", 1L, "x").size)
        // سقف تعداد
        var list = emptyList<ExamTiming.OfflineEvent>()
        repeat(ExamTiming.MAX_EVENTS + 20) { list = ExamTiming.pushEvent(list, "e$it", it.toLong(), null) }
        assertEquals(ExamTiming.MAX_EVENTS, list.size)
        // طول جزئیات مهار می‌شود
        val long = ExamTiming.pushEvent(emptyList(), "k", 0L, "d".repeat(300))
        assertEquals(200, long[0].d.length)
    }

    // ------------------------------------------------------------------
    // ۶) پین سیم‌کشی و SQL
    // ------------------------------------------------------------------

    @Test
    fun repositoryWiresAllSevenV357Rpcs() {
        val teacher = src("app/src/main/java/ir/exam/app/data/repository/SupabaseGradingRepository.kt")
        listOf("native_time_grants_v357", "native_time_grant_save_v357",
               "native_question_limits_v357", "native_question_limits_save_v357")
            .forEach { assertTrue("در مخزن معلم نیست: $it", teacher.contains(it)) }
        val student = src("app/src/main/java/ir/exam/app/data/repository/SupabaseStudentExamRepository.kt")
        listOf("native_sync_exam_question_v357", "native_offline_pack_v357", "native_offline_submit_v357")
            .forEach { assertTrue("در مخزن دانش‌آموز نیست: $it", student.contains(it)) }
        assertTrue("اعتبارسنجی باید پیش از ارسال باشد", teacher.contains("ExamTiming.validateGrant"))
    }

    @Test
    fun siteAndAppShareTimingModuleAndLimits() {
        val site = src("site/src/examtiming.js")
        val app = src("app/src/main/java/ir/exam/app/domain/exam/ExamTiming.kt")
        listOf("MAX_EXTRA_MINUTES", "MIN_QUESTION_SECONDS", "MAX_QUESTION_SECONDS", "OFFLINE_GRACE_MS")
            .forEach { assertTrue("در سایت نیست: $it", site.contains(it)); assertTrue("در اپ نیست: $it", app.contains(it)) }
        assertTrue("کلید «همه» باید در سایت باشد", site.contains("همه"))
        assertTrue(app.contains("همه"))
        val build = src("site/build_site.py")
        val tm = build.indexOf("\"examtiming.js\"")
        val admin = build.indexOf("\"admin.js\"")
        assertTrue("examtiming.js باید در بسته باشد", tm > 0)
        assertTrue("باید پیش از admin.js بیاید", tm < admin)
    }

    @Test
    fun siteWiresTeacherTabAndStudentLock() {
        val admin = src("site/src/admin.js")
        assertTrue(admin.contains("['timing', '⏱ زمان‌بندی']"))
        assertTrue(admin.contains("tabTiming(body, ctx)"))
        assertTrue(admin.contains("native_time_grant_save_v357"))
        assertTrue(admin.contains("native_question_limits_save_v357"))
        val student = src("site/src/student.js")
        assertTrue(student.contains("native_sync_exam_question_v357"))
        assertTrue(student.contains("run.qLocked"))
        assertTrue(student.contains("زمان این سؤال تمام شد"))
        assertTrue(student.contains("native_offline_pack_v357"))
        assertTrue(student.contains("native_offline_submit_v357"))
        assertTrue(student.contains("offlineSubmittable"))
    }

    @Test
    fun appWiresTimingTab() {
        val vm = src("app/src/main/java/ir/exam/app/ui/grading/GradingViewModel.kt")
        assertTrue(vm.contains("if (value == \"timing\") loadTiming()"))
        assertTrue(vm.contains("ExamTiming.parseLimits"))
        assertTrue(vm.contains("\"similarity\", \"timing\""))
        val screen = src("app/src/main/java/ir/exam/app/ui/grading/GradingScreen.kt")
        assertTrue(screen.contains("GradingTimingContent(state = state, viewModel = viewModel)"))
        assertTrue(screen.contains("Text(\"زمان‌بندی\")"))
        val ui = src("app/src/main/java/ir/exam/app/ui/grading/GradingTimingContent.kt")
        assertTrue(ui.contains("قفل سمت سرور اعمال می‌شود"))
        assertTrue(ui.contains("دلیل (اجباری)"))
    }

    @Test
    fun migrationKeepsExistingStartFunctionsUntouched() {
        val sql = src("supabase/sql/SQL_EXAM_TIMING_V357.sql")
        assertTrue("زمان اضافه باید با تریگر اعمال شود", sql.contains("native_time_grant_apply_v357"))
        assertTrue(sql.contains("before insert or update of expires_at on public.exam_attempts"))
        assertTrue(sql.contains("before insert or update of expires_at on public.exam_sessions"))
        assertTrue("تریگر باید بی‌تکرار باشد", sql.contains("time_grant_applied"))
        assertFalse("تابع شروع موجود نباید بازنویسی شود", sql.contains("native_start_exam"))
        assertTrue(sql.contains("e.id::text = exam_time_grants.exam_id"))
        assertTrue("دانش‌آموز فقط مجوز خودش", sql.contains("v357 student reads own grant"))
        assertFalse("سیاست نوشتن نباید ساخته شود", sql.contains("for insert on public.exam_time_grants"))
        assertTrue("توکن نباید به افزونه بند باشد", sql.contains("gen_random_uuid"))
        assertFalse(sql.contains("gen_random_bytes"))
        assertTrue("مهلت سؤال از مهلت آزمون جلوتر نرود", sql.contains("v_exp > v_att.expires_at"))
        assertTrue("برگه باید آفلاین علامت بخورد", sql.contains("'offline', true"))
        assertTrue("توکن یک‌بارمصرف", sql.contains("offline_used_at"))
        val check = src("supabase/sql/SQL_EXAM_TIMING_V357_CHECK.sql")
        assertTrue(check.contains("native_offline_submit_v357(text,text,jsonb,jsonb,timestamptz)"))
    }

    @Test
    fun noCodeExecutionOrNetworkInTimingModule() {
        val site = src("site/src/examtiming.js")
        val app = src("app/src/main/java/ir/exam/app/domain/exam/ExamTiming.kt")
        listOf("fetch(", "eval(", "new Function(", "XMLHttpRequest").forEach {
            assertFalse("پیمانه باید خالص بماند: $it", site.contains(it))
        }
        listOf("ProcessBuilder", "Runtime.getRuntime", "SupabaseProvider").forEach {
            assertFalse("پیمانه باید خالص بماند: $it", app.contains(it))
        }
    }

    @Test
    fun versionAndChangelogAgree() {
        val version = src("text/APP_VERSION.txt").trim()
        assertTrue("قالب شمارهٔ نسخه نامعتبر است: $version", version.matches(Regex("\\d+\\.\\d+\\.\\d+")))
        val lines = src("text/CHANGELOG_FA.txt").lines()
        listOf(40, 60).forEach { take ->
            val top = ReleaseNotes.parse(lines.take(take)).first()
            assertTrue(top.appAffecting)
            assertEquals("فایل نسخه و بلوک بالای فهرست تغییرات باید یکی باشند", version, top.appVersion)
            assertTrue(top.bullets.size in 1..ReleaseNotes.MAX_BULLETS)
            top.bullets.forEach { assertFalse(it.contains(Regex("\\bV\\d"))) }
        }
        val block = ReleaseNotes.parse(lines).firstOrNull { it.version == "V357" }
        assertTrue("بلوک V357 نیست", block != null)
        assertTrue("وعدهٔ نظارت آفلاین باید صادقانه باشد",
            block!!.bullets.any { it.contains("آفلاین") })
    }
}
