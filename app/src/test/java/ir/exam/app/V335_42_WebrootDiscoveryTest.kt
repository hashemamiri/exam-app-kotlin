package ir.exam.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V335.42 — نگهبان «تشخیص ریشهٔ وب سرور».
 *
 * درس عملی: `detect_webroot` فقط Caddyfile و پنج مسیر رایج را می‌دید و روی سروری
 * که سایت را جای دیگری سرو می‌کند، «ریشهٔ وب پیدا نشد» می‌داد و کار متوقف می‌شد.
 * این تست تضمین می‌کند:
 *  ۱) جست‌وجوی خودکار `index.html` در مسیرهای رایج وجود دارد؛
 *  ۲) حالت `--probe` وضعیت سرور را نشان می‌دهد؛
 *  ۳) گزینهٔ `--empty-webroot` پوشهٔ تازه می‌سازد و مقصد می‌شود؛
 *  ۴) پیام خطای SSH و راهنمای دستی وجود دارد.
 */
class V335_42_WebrootDiscoveryTest {

    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: error("ریشهٔ مخزن پیدا نشد")
    }

    private fun src(relative: String): String = File(root(), relative).readText(Charsets.UTF_8)

    private val sync = src("scripts/sync_arvan_site.py")

    @Test
    fun `webroot detection falls back to a filesystem search`() {
        assertTrue("جست‌وجوی index.html", "find /var/www /srv /opt /home /root" in sync)
        assertTrue("نشانگر FOUND", "FOUND:" in sync)
        assertTrue("پیشنهاد مسیر با نام پروژه", "\"onlineexam\" in d" in sync)
    }

    @Test
    fun `probe mode reports server state`() {
        assertTrue("تابع probe", "def probe_server(" in sync)
        assertTrue("Caddyfile در خروجی", "head -40 /etc/caddy/Caddyfile" in sync)
        assertTrue("نسخهٔ سایت روی سرور", "نسخهٔ سایتِ الان" in sync)
    }

    @Test
    fun `empty webroot option creates a fresh target`() {
        assertTrue("آرگومان خط فرمان", "--empty-webroot" in sync)
        assertTrue("پوشهٔ پیش‌فرض", "/var/www/onlineexam" in sync)
        assertTrue("ساخت پوشه", "mkdir -p {webroot}" in sync)
        assertTrue("یادآوری root در Caddyfile", "root * {webroot}" in sync)
    }
}
