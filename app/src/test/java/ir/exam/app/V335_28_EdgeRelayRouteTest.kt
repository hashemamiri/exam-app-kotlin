package ir.exam.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V335.28 — نگهبان «پل روی میزبانی باز برای شبکهٔ همراه اول»:
 *
 * ۱) اندازه‌گیری روی دادهٔ همراه اول نشان داد TCP به IPهای Cloudflare (دامنهٔ پروژهٔ Supabase و هر دو
 *    زیردامنهٔ خودمان) بسته است، ولی لبهٔ میزبان Vercel پاسخ سالم می‌دهد. پس نخستین مسیر جایگزین
 *    باید همان پل باشد و هیچ ارتباطی با دامنه‌های Cloudflare نداشته باشد.
 * ۲) نشانی لبهٔ پل باید تثبیت‌شده باشد تا خطای گذرای DNS همان شبکه، مسیر سالم را از دست ندهد.
 * ۳) فایل پل باید فقط مسیرهای مجاز Supabase را عبور دهد و هیچ کلید یا رمزی در خود نداشته باشد.
 */
class V335_28_EdgeRelayRouteTest {

    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: error("ریشهٔ مخزن پیدا نشد")
    }

    private fun source(relative: String): String = File(root(), relative).readText(Charsets.UTF_8)

    private val router by lazy { source("app/src/main/java/ir/exam/app/core/network/SupabaseRouter.kt") }
    private val dns by lazy { source("app/src/main/java/ir/exam/app/core/network/RelayDns.kt") }
    private val proxy by lazy { source("relay/vercel/api/[...path].js") }

    @Test
    fun `edge relay is declared and listed first`() {
        assertTrue(
            "ثابت پل لبه",
            "const val DEFAULT_RELAY_EDGE = \"https://exam-relay-onlineexam.vercel.app/api\"" in router
        )
        assertTrue(
            "پل لبه باید نخستین گزینهٔ فهرست باشد",
            "val DEFAULT_RELAYS: List<String> = listOf(\n" in router
        )
        val listStart = router.indexOf("val DEFAULT_RELAYS: List<String> = listOf(")
        assertTrue("فهرست مسیرها پیدا نشد", listStart >= 0)
        val firstEntry = router.substring(listStart).lineSequence()
            .drop(1)
            .firstOrNull { it.trim().isNotEmpty() && !it.trim().startsWith("//") }
            .orEmpty()
        assertTrue("نخستین مسیر باید پل لبه باشد: $firstEntry", firstEntry.trim().startsWith("DEFAULT_RELAY_EDGE"))
    }

    @Test
    fun `edge relay is not routed through cloudflare`() {
        assertFalse(
            "پل لبه نباید روی دامنهٔ Cloudflare خودمان باشد",
            "const val DEFAULT_RELAY_EDGE = \"https://sb.onlineexam.ir\"" in router
        )
        assertTrue("دامنهٔ پل لبه", "exam-relay-onlineexam.vercel.app" in router)
    }

    @Test
    fun `edge host keeps pinned addresses with system fallback`() {
        assertTrue(
            "نشانی تثبیت‌شدهٔ لبه برای جلوگیری از خطای گذرای DNS",
            "\"exam-relay-onlineexam.vercel.app\" to listOf(\"216.198.79.195\", \"64.29.17.195\")" in dns
        )
        assertTrue("پاسخ سیستم به‌عنوان پشتیبان", "val resolved = runCatching { Dns.SYSTEM.lookup(hostname) }" in dns)
    }

    @Test
    fun `edge proxy forwards only supabase paths`() {
        assertTrue(
            "مسیرهای مجاز",
            "const ALLOWED_PREFIXES = [\"/rest/v1\", \"/auth/v1\", \"/storage/v1\", \"/functions/v1\", \"/realtime/v1\"];" in proxy
        )
        assertTrue("پاسخ سلامت برای تست سریع", "pathname === \"/\" || pathname === \"/health\"" in proxy)
        assertTrue("بستن مسیرهای ناشناخته", "error: \"path_not_allowed\"" in proxy)
        assertTrue("خودداری از انتقال کوکی", "\"set-cookie\"" in proxy)
    }

    @Test
    fun `edge proxy carries no credentials`() {
        assertFalse("هیچ توکن JWT در پل نباشد", "eyJ" in proxy)
        assertFalse("هیچ کلید service_role در پل نباشد", "service_role" in proxy)
        assertTrue("کلید عبوری از سمت اپ", "apikey" in proxy)
    }
}
