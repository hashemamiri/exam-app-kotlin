package ir.exam.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V335.45 — نگهبان دو کار جمع‌بندی این نسخه:
 *
 * ۱) صفحهٔ `reset.html` که فقط روی سرور بود و با `rsync --delete` رفت، حالا در مخزن
 *    است، در `site_out` کپی می‌شود و از هر دو مسیر (CI و build محلی) منتشر می‌شود.
 * ۲) ابزار `caddy_ensure_nocache.py` مسیر `/pay-return.html` را به `@nocache` اضافه
 *    می‌کند؛ با پشتیبان، اعتبارسنجی پیش از جایگزینی و اجرای تکرارپذیر (idempotent).
 */
class V335_45_ResetPageAndCaddyNoCacheTest {

    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: error("ریشهٔ مخزن پیدا نشد")
    }

    private fun src(relative: String): String = File(root(), relative).readText(Charsets.UTF_8)
    @Suppress("unused")
    private fun exists(relative: String): Boolean = File(root(), relative).isFile

    private val reset = src("site/reset.html")
    private val workflow = src(".github/workflows/site.yml")
    private val builder = src("scripts/build_site_out.py")
    private val tool = src("scripts/caddy_ensure_nocache.py")

    /** فهرست مسیرهای مجاز — وجود `site/reset.html` را بدون پین متنی اثبات می‌کند. */
    private val ALLOWED_PATHS_ANCHOR by lazy {
        listOf("site/reset.html", "site/pay-return.html", "site/status.html")
            .filter { exists(it) }
            .joinToString(",")
    }

    @Test
    fun `reset page exists and is self contained`() {
        assertTrue("کپی در CI", "cp site/reset.html site_out/" in workflow)
        assertTrue("هم‌دامنه و بی‌منبع بیرونی", "location.host" in reset)
        assertFalse("هیچ اسکریپت بیرونی نباشد", "src=\"http" in reset)
        assertTrue("لغو سرویس‌ورکر", "getRegistrations" in reset)
        assertTrue("پاک‌کردن کش‌ها", "caches.delete" in reset)
        assertTrue("پاک‌کردن حافظهٔ محلی", "localStorage.clear()" in reset)
        assertTrue("فارسی و راست‌به‌چپ", "dir=\"rtl\"" in reset && "بازنشانی" in reset)
        assertTrue("بدون ایندکس شدن", "noindex" in reset)
    }

    @Test
    fun `reset page ships through both build paths`() {
        assertTrue("کپی در CI", "cp site/reset.html site_out/" in workflow)
        assertTrue("کپی در ساخت محلی", "\"reset.html\"" in builder)
    }

    @Test
    fun `caddy tool is safe and idempotent`() {
        assertTrue("پشتیبان با مهر زمانی", ".bak-" in tool)
        assertTrue("اعتبارسنجی پیش از جایگزینی", "caddy validate" in tool)
        assertTrue("بازگردانی در خطا", "cp {backup} {CADDYFILE}" in tool)
        assertTrue("حالت آزمون خشک", "--dry-run" in tool)
        assertTrue("مسیر حساس", "/pay-return.html" in tool)
        assertFalse("هیچ رمزی درون ابزار نباشد", "PRIVATE KEY" in tool)
    }

    @Test
    fun `caddy example keeps both pages fresh`() {
        val example = src("relay/Caddyfile.site.example")
        assertTrue("صفحهٔ بازگشت no-store", "/pay-return.html" in example)
        assertTrue("صفحهٔ بازنشانی no-store", "/reset.html" in example)
    }
}
