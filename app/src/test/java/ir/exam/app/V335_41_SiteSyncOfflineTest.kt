package ir.exam.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V335.41 — نگهبان «هم‌گام‌سازی بدون وابستگی به pages.dev».
 *
 * درس عملی: روی شبکهٔ کاربر، `*.pages.dev` فقط IPv6 پاسخ می‌داد و WSL مسیر IPv6
 * نداشت ⇒ `URLError: [Errno 101] Network is unreachable` و `--check` می‌شکست.
 * این تست تضمین می‌کند ابزار هم‌گام‌سازی:
 *  ۱) پیش از هر درخواست IPv4 را ترجیح می‌دهد؛
 *  ۲) اگر منبع منتشرشده نرسید، با اثر انگشت همین مخزن مقایسه می‌کند؛
 *  ۳) دستور ساخت محلی `site_out` را دارد و `--build-local` را می‌پذیرد.
 */
class V335_41_SiteSyncOfflineTest {

    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: error("ریشهٔ مخزن پیدا نشد")
    }

    private fun source(relative: String): String = File(root(), relative).readText(Charsets.UTF_8)

    private val sync = source("scripts/sync_arvan_site.py")
    private val builder = source("scripts/build_site_out.py")

    @Test
    fun `ipv4 is preferred before any request`() {
        assertTrue("تابع ترجیح IPv4", "def prefer_ipv4()" in sync)
        assertTrue("تحمیل خانواده آدرس", "socket.AF_INET" in sync)
        assertTrue("فراخوانی پیش از کار", "prefer_ipv4()  # V335.41" in sync)
    }

    @Test
    fun `offline comparison uses the repository fingerprint`() {
        assertTrue("اثر انگشت محلی", "def local_fingerprint()" in sync)
        assertTrue("مقایسهٔ بدون شبکه", "دامنه با نسخهٔ همین مخزن هم‌تراز است" in sync)
        assertTrue("پیام تشخیص IPv6", "IPv6" in sync)
    }

    @Test
    fun `local site_out build exists and is wired`() {
        assertTrue("جای‌نگهدار کلید در builder", "SUPABASE_ANON_KEY" in builder)
        assertTrue("شکستن پنل‌ها", "window.__PANELS_URLS" in builder)
        assertTrue("نسخهٔ سرویس‌ورکر", "__SW_VERSION__" in builder)
        assertTrue("هدرهای کش", "Cache-Control" in builder)
        assertTrue("گزینهٔ ابزار", "build-local" in sync)
        assertTrue("ساخت پوشهٔ انتشار", "build_site_out.py" in sync)
    }
}
