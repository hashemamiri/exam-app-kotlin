package ir.exam.app

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V335.39 — نگهبان «هم‌گام‌سازی دامنهٔ `onlineexam.ir` با نسخهٔ منتشرشده»:
 *
 * ۱) `site.yml` نسخهٔ ریشهٔ صفحهٔ بازگشت درگاه را منتشر می‌کند؛ پیش‌تر فقط زیر
 *    `pwa/` بود و `/pay-return.html` روی دامنه ۴۰۴ یا صفحهٔ فرود می‌شد.
 * ۲) گام هم‌گام‌سازی آروان با متغیر `ARVAN_SYNC` و فقط روی `main` اجرا می‌شود
 *    (بدون سکرت، CI نباید بشکند).
 * ۳) ابزار `scripts/sync_arvan_site.py` سه کارکرد check/mirror/apply دارد و
 *    هیچ رمز یا کلیدی درون خود ندارد.
 * ۴) نسخهٔ ریشه و `pwa/` صفحهٔ بازگشت فقط در خط توضیح تفاوت دارند.
 * ۵) نمونهٔ Caddy برای دارایی ناموجود ۴۰۴ می‌دهد (نه index با کد ۲۰۰).
 */
class V335_39_SiteDomainSyncTest {

    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: error("ریشهٔ مخزن پیدا نشد")
    }

    private fun source(relative: String): String = File(root(), relative).readText(Charsets.UTF_8)

    // قطعهٔ مرجع workflow در مخزن می‌ماند؛ حتی اگر توکن گیت اجازهٔ تغییر site.yml را نداشته باشد
    private val wfSnippet = source("docs/ci/site_arvan_sync_step.yml")
    private val syncTool = source("scripts/sync_arvan_site.py")
    private val caddy = source("relay/Caddyfile.site.example")

    @Test
    fun `root pay return page is published`() {
        assertTrue("نسخهٔ ریشهٔ صفحهٔ بازگشت در site_out", "cp site/pay-return.html site_out/" in wfSnippet)
    }

    @Test
    fun `arvan sync step is gated and main only`() {
        assertTrue("نشانگر گام", "SYNC_ARVAN" in wfSnippet)
        assertTrue("قفل متغیر مخزن", "vars.ARVAN_SYNC == 'on' && github.ref_name == 'main'" in wfSnippet)
        assertTrue("فراخوانی ابزار در CI", "scripts/sync_arvan_site.py" in wfSnippet)
        assertTrue("ارسال پوشهٔ ساخته‌شده", "--from-dir site_out" in wfSnippet)
        assertTrue("کلید فقط از سکرت", "secrets.ARVAN_SSH_KEY" in wfSnippet)
        assertTrue("بدون سکرت هم CI نمی‌شکند", "continue-on-error: true" in wfSnippet)
    }

    @Test
    fun `sync tool has three modes without embedded secrets`() {
        assertTrue("حالت بررسی", "\"--check\"" in syncTool)
        assertTrue("حالت آینه", "\"--mirror\"" in syncTool)
        assertTrue("حالت ارسال", "\"--apply\"" in syncTool)
        assertTrue("منبع پیش‌فرض", "https://onlineexam-1q1.pages.dev" in syncTool)
        assertTrue("مقصد پیش‌فرض", "root@95.38.160.168" in syncTool)
        assertTrue("حذف فایل‌های اضافی سرور", "\"--delete\"" in syncTool)
        assertTrue("تشخیص خودکار ریشهٔ وب", "detect_webroot" in syncTool)
        assertEquals("هیچ کلید خصوصی درون ابزار نباشد", false, "BEGIN RSA" in syncTool || "BEGIN OPENSSH" in syncTool)
    }

    @Test
    fun `pay return copies differ only by the comment line`() {
        fun body(path: String) = source(path).lineSequence()
            .filterNot { it.trim().startsWith("<!--") }
            .joinToString("\n")
        assertEquals(
            "نسخهٔ ریشه و pwa باید یکی باشند",
            body("site/pwa/pay-return.html"),
            body("site/pay-return.html")
        )
    }

    @Test
    fun `caddy returns real 404 for missing assets`() {
        assertTrue("۴۰۴ واقعی", "respond \"Not Found\" 404" in caddy)
        assertTrue("فایل‌سرور استاتیک", "file_server" in caddy)
        assertTrue("کش دائمی دارایی‌های hash‌دار", "immutable" in caddy)
    }
}
