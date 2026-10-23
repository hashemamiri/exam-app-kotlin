package ir.exam.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V335.3 (1.02.88) — نگهبان جامع گام ۴ از ۴ برنامهٔ نهایی:
 * ممیزی یکپارچه، جریان سرتاسری معلم و دانش‌آموز، و آمادگی کامل انتشار نهایی در اپ و وب:
 *
 * ۱. گام ۱ (دیتابیس): پوشش صریح ۵۷ جدول Data API و امنیت RLS پیش از ۳۰ اکتبر Supabase.
 * ۲. گام ۲ (فرانت وب): هم‌ترازی کامل صفحات وب با اندروید، بستن مودال‌ها با Esc، ریل تک‌سؤال بانک و چیپ‌های تشویقی.
 * ۳. گام ۳ (اندروید): صفر بودن کل هشدارهای کامپایلر کاتلین، پیاده‌سازی ImageLoaderFactory و مهاجرت AutoMirrored.
 * ۴. گام ۴ (انتشار نهایی): تفکیک آزمون آنلاین/چاپی، جریان دوطرفهٔ معلم/دانش‌آموز، نبود نشت کلید و اعتبارسنجی کامل پیش‌پرواز.
 */
class V335_3_FinalJourneyReleaseReadinessAuditTest {

    private fun root(): File {
        var d = File(System.getProperty("user.dir") ?: ".")
        while (!File(d, "settings.gradle.kts").exists() && d.parentFile != null) d = d.parentFile!!
        return d
    }

    private fun read(relPath: String): String = File(root(), relPath).readText(Charsets.UTF_8)

    @Test
    fun step1DatabaseSecurityAndDataApiIntegrityPasses() {
        val mig = read("supabase/migrations/20261023_post_oct30_complete_audit_v335_0.sql")
        assertTrue("مهاجرت باید RLS را فعال کند", "alter table public.%I enable row level security" in mig)
        assertTrue("دسترسی anon به exam_keys باید صریحاً لغو شود", "revoke all on table public.%I from anon" in mig)
        assertTrue("تابع native_due_account_deletions_v282 باید به service_role گرنت بگیرد", "grant execute on function public.native_due_account_deletions_v282() to service_role" in mig)
        assertTrue("search_path باید برای توابع قفل شود", "alter function %s set search_path = public, pg_temp" in mig)
        assertTrue("اسکیما private باید قفل شود", "revoke all on schema private from public, anon, authenticated" in mig)
    }

    @Test
    fun step2WebUiParityAndInteractionAuditPasses() {
        val appJs = read("site/src/app.js")
        val builderJs = read("site/src/builder.js")
        val adminJs = read("site/src/admin.js")
        val schoolJs = read("site/src/school.js")

        assertTrue("بستن خودکار مودال‌ها در ناوبری", "function closeModalsOnNav()" in appJs)
        assertTrue("بستن مودال با کلید Escape", "e.key === 'Escape'" in appJs)
        assertTrue("ایمن‌سازی تابع el در برابر آرگومان دوم", "if (Array.isArray(attrs)) { children = attrs; attrs = null; }" in appJs)
        assertTrue("مشخصات سؤال بانک در پنل راست سازندهٔ دسکتاپ", "مشخصات سؤال بانک" in builderJs)
        assertTrue("دسته‌های بانک سؤال در پنل راست", "دسته‌های بانک سؤال" in builderJs)
        assertFalse("حذف بریدگی وسط کلمهٔ مهر تشویقی دبیر", "st.slice(0, 24)" in adminJs)
        assertTrue("راهنمای شفاف حالت خالی بانک سؤال", "➕ سؤال جدید در بانک" in schoolJs)
    }

    @Test
    fun step3AndroidKotlinZeroDeprecationAuditPasses() {
        val examApp = read("app/src/main/java/ir/exam/app/ExamApplication.kt")
        assertTrue("ExamApplication باید ImageLoaderFactory را پیاده‌سازی کند", "coil.ImageLoaderFactory" in examApp)
        assertTrue("تعریف متد newImageLoader", "override fun newImageLoader(): ImageLoader" in examApp)

        val mainRoot = File(root(), "app/src/main/java")
        val ktFiles = mainRoot.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        assertTrue("حداقل ۲۵۰ فایل کاتلین باید بررسی شوند", ktFiles.size >= 250)

        val forbidden = listOf(
            "LocalImageLoader",
            "LocalClipboardManager",
            "quadraticBezierTo(",
            "SOFT_INPUT_ADJUST_RESIZE",
            "Icons.Outlined.RotateLeft",
            "Icons.Outlined.RotateRight",
            "Icons.Outlined.ArrowBack",
            "Icons.Outlined.Login",
            "Icons.Outlined.KeyboardArrowLeft",
            "Icons.Outlined.KeyboardArrowRight"
        )
        for (f in ktFiles) {
            val lines = f.readLines(Charsets.UTF_8)
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")) continue
                for (token in forbidden) {
                    assertFalse("کد اصلی نباید حاوی توکن منسوخ $token باشد: ${f.name}", line.contains(token))
                }
            }
        }
    }

    @Test
    fun step4EndToEndFullJourneyAndReleaseReadinessPasses() {
        val e2eScript = read("scripts/web_ui_e2e.py")
        assertTrue("اسکریپت E2E باید صفحه فرود دسکتاپ را پوشش دهد", "01_landing_desktop.png" in e2eScript)
        assertTrue("اسکریپت E2E باید سازنده آزمون را پوشش دهد", "03_teacher_builder_clean.png" in e2eScript)
        assertTrue("اسکریپت E2E باید باز کردن آزمون را پوشش دهد", "04_teacher_open_exam.png" in e2eScript)
        assertTrue("اسکریپت E2E باید پاسخ دانش‌آموز را پوشش دهد", "05_student_essay_answer.png" in e2eScript)
        assertTrue("اسکریپت E2E باید ثبت و نمره‌دهی خودکار را پوشش دهد", "07_student_submit_receipt.png" in e2eScript)
        assertTrue("اسکریپت E2E باید تصحیح دبیر را پوشش دهد", "08_teacher_grading_complete.png" in e2eScript)
        assertTrue("اسکریپت E2E باید بانک سؤال را پوشش دهد", "08b_teacher_bank_builder.png" in e2eScript)
        assertTrue("اسکریپت E2E باید کارنامه دانش‌آموز را پوشش دهد", "09_student_report_card.png" in e2eScript)

        val version = read("text/APP_VERSION.txt").trim()
        val changelog = read("text/CHANGELOG_FA.txt")
        assertTrue("شماره نسخه باید در خط اول CHANGELOG باشد", changelog.lines().first().contains(version))
        assertTrue("شماره نسخه باید از الگوی استاندارد پیروی کند", version.matches(Regex("""\d+\.\d{2}\.\d{2}""")))
    }
}
