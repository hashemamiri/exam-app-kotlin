package ir.exam.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V335.46 — نگهبان مسیرهای دلخواه در ابزار `caddy_ensure_nocache.py`.
 *
 * یافتهٔ میدانی V335.45: `/pay-return.html` روی دامنهٔ اصلی `no-store` گرفت،
 * ولی نسخهٔ PWA آن (`/pwa/pay-return.html`) بی‌کش نشد، چون در بلوک دامنهٔ اصلی
 * در فهرست `@nocache` نبود. این نسخه:
 *   ۱) `/pwa/pay-return.html` را به فهرست پیش‌فرض اضافه می‌کند؛
 *   ۲) `--path` قابل تکرار می‌دهد تا مسیرهای آینده بدون تغییر کد اضافه شوند؛
 *   ۳) مسیرها را نرمال و یکتا می‌کند (بدون تکرار در خط Caddyfile).
 */
class V335_46_CaddyExtraPathsTest {

    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: error("ریشهٔ مخزن پیدا نشد")
    }

    private fun src(relative: String): String = File(root(), relative).readText(Charsets.UTF_8)

    private val tool = src("scripts/caddy_ensure_nocache.py")
    private val docs = src("docs/fa/ARVAN_SITE_SYNC_FA.md")

    @Test
    fun `pwa pay return page is covered by default`() {
        assertTrue("مسیر PWA در فهرست پیش‌فرض", "\"/pwa/pay-return.html\"" in tool)
        assertTrue("مسیر اصلی هم می‌ماند", "\"/pay-return.html\"" in tool)
    }

    @Test
    fun `custom paths can be added repeatably`() {
        assertTrue("پارامتر قابل تکرار", "action=\"append\"" in tool)
        assertTrue("راهنمای پارامتر", "مسیر اضافه برای no-cache" in tool)
        assertTrue("ادغام با پیش‌فرض", "WANTED + list(a.path)" in tool)
    }

    @Test
    fun `paths are normalized and unique`() {
        assertTrue("تابع نرمال‌سازی", "def normalize(paths)" in tool)
        assertTrue("یکتاسازی", "if p not in out:" in tool)
        assertTrue("اسلش ابتدایی", "startswith(\"/\")" in tool)
        assertTrue("نمایش مسیرهای تضمین‌شده", "مسیرهای تضمین‌شده" in tool)
    }

    @Test
    fun `apply order is validate then replace then reload`() {
        assertTrue("اعتبارسنجی هست", "caddy validate" in tool)
        assertTrue("reload هست", "systemctl reload caddy" in tool)
        assertTrue("پشتیبان‌گیری", ".bak-" in tool)
        assertTrue("بازگردانی در خطا", "cp {backup} {CADDYFILE}" in tool)
        assertFalse("هیچ کلید خصوصی", "PRIVATE KEY" in tool)
    }

    @Test
    fun `docs describe the tool and the custom path flag`() {
        assertTrue("نام ابزار در مستند", "caddy_ensure_nocache.py" in docs)
        assertTrue("پرچم مسیر دلخواه", "--path /exam.html" in docs)
        assertTrue("تأیید با curl", "cache-control" in docs)
    }
}
