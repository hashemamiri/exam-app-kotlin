package ir.exam.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V352.1 — چرا CI سایت شکست و چه چیزی درست شد.
 *
 * گام «انتخاب قالب کارنامه» صفحه را «خالی» گزارش کرد، در حالی که صفحه
 * کاملاً سالم بود. علت: قاعدهٔ V179 هنگام باز بودن موتور چاپ، پوستهٔ
 * زیرین را visibility:hidden می‌کند و innerText متن پنهان را نمی‌شمارد.
 * سنجهٔ قدیمی تنها به این دلیل پاس می‌شد که یک پیام شناور اتفاقاً روی
 * صفحه بود؛ در CI آن پیام محو شده بود.
 *
 * این آزمون پین می‌کند که سنجه موتورآگاه بماند و نگهبان‌ها در CI
 * ثبت شده باشند — تا امروز فقط محلی اجرا می‌شدند.
 */
class V352_1_LayoutHealthTest {

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

    @Test
    fun layoutHealthIsEngineAware() {
        val s = src("scripts/web_ui_e2e.py")
        assertTrue("حالت موتور تشخیص داده می‌شود", s.contains("body.classList.contains('engine-open')"))
        assertTrue("متن اورلی شمرده می‌شود", s.contains("const ov = document.querySelector('.engine-bg')"))
        assertTrue("سند iframe هم شمرده می‌شود", s.contains("if (d && d.body) n += (d.body.innerText || '').trim().length"))
        assertTrue("در نبود موتور همان رفتار پیشین", s.contains("return (body.innerText || '').trim().length"))
        assertTrue("خطای دسترسی مانع نمی‌شود", s.contains("catch (e) { /* اگر هم‌مبدأ نبود"))
        // سنجه باید هنوز صفحهٔ واقعاً خالی را رد کند
        assertTrue("آستانهٔ خالی‌بودن سر جایش است", s.contains("assert metrics[\"bodyTextLen\"] > 20"))
    }

    @Test
    fun theHiddenShellRuleIsTheRootCause() {
        val css = src("site/src/site.css")
        assertTrue("قاعدهٔ V179 پوسته را پنهان می‌کند", css.contains("body.engine-open > #root{visibility:hidden"))
        val js = src("site/src/reportcard.js")
        // ترتیب واقعی: اول متن انتظار حذف می‌شود، بعد کارت‌ها رندر می‌شوند
        val removeAt = js.indexOf("if (wait.parentNode) wait.remove();")
        val openAt = js.indexOf("w.ReportCard.open(payload);")
        assertTrue("حذف متن انتظار هست", removeAt > 0)
        assertTrue("رندر کارت‌ها بعد از آن است", openAt > removeAt)
    }

    @Test
    fun layoutHealthGuardExistsAndCoversFiveCases() {
        val g = src("scripts/check_layout_health.py")
        assertTrue("پنج حالت", g.contains("LAYOUT_HEALTH: PASS — %d حالت"))
        assertTrue("حالت شکست CI بازتولید شده", g.contains("موتور باز با کارت‌های آماده"))
        assertTrue("صفحهٔ واقعاً خالی هم رد می‌شود", g.contains("موتور باز و واقعاً خالی"))
        assertTrue("صفحهٔ عادی پرمتن", g.contains("صفحهٔ عادی پرمتن"))
        assertTrue("صفحهٔ عادی خالی", g.contains("صفحهٔ عادی خالی"))
        assertTrue("حالت انتظار", g.contains("موتور باز در حال آماده‌سازی"))
        // باید هر دو جهت را بسنجد، نه فقط قبول‌کردن
        assertTrue("انتظار قبول/رد مشخص است", g.contains("ok = accepted == should_pass"))
    }

    @Test
    fun guardsAreRegisteredInCi() {
        val android = src(".github/workflows/android.yml")
        listOf(
            "check_kotlin_braces", "check_kotlin_quotes", "check_kotlin_escapes",
            "check_kotlin_imports", "check_missing_imports", "check_kotlin_visibility",
            "check_sealed_when", "check_test_paths", "check_test_pins",
            "check_site_pins", "check_site_sync"
        ).forEach { g -> assertTrue("در CI اپ ثبت نشده: $g", android.contains(g)) }
        assertTrue("حلقهٔ اجرا هست", android.contains("python3 \"scripts/\$g.py\""))
        assertTrue("زودتر از Gradle می‌آید", android.indexOf("نگهبان‌های سریع سورس") < android.indexOf("اجرای تست‌ها و Lint"))

        val site = src(".github/workflows/site.yml")
        assertTrue("نگهبان سلامت در CI سایت", site.contains("python3 scripts/check_layout_health.py"))
        assertTrue(
            "پیش از آزمون سرتاسری می‌آید",
            site.indexOf("check_layout_health.py") < site.indexOf("web_ui_e2e.py --site-dir")
        )
        assertTrue("ماشهٔ سایت نگهبان تازه را می‌بیند", site.contains("'scripts/check_layout_health.py'"))
    }
}
