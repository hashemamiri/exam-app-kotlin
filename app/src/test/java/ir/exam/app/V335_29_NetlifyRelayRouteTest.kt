package ir.exam.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V335.29 — نگهبان «پل روی Netlify»:
 *
 * ۱) اندازه‌گیری روی دادهٔ همراه اول نشان داد TCP به IPهای Cloudflare (دامنهٔ پروژهٔ Supabase و هر سه
 *    زیردامنهٔ خودمان) بسته است، ولی `netlify.com` از همان شبکه پاسخ سالم می‌دهد. پس نخستین مسیر
 *    جایگزین باید پل Netlify باشد.
 * ۲) پل Vercel هم به‌عنوان گزینهٔ دوم می‌ماند (پشتیبان) و نباید حذف شود.
 * ۳) فایل‌های پل Netlify باید فقط مسیرهای مجاز Supabase را عبور دهند و هیچ کلید یا رمزی نداشته باشند.
 */
class V335_29_NetlifyRelayRouteTest {

    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: error("ریشهٔ مخزن پیدا نشد")
    }

    private fun source(relative: String): String = File(root(), relative).readText(Charsets.UTF_8)

    private val router by lazy { source("app/src/main/java/ir/exam/app/core/network/SupabaseRouter.kt") }
    private val fn by lazy { source("relay/netlify/functions/relay.js") }
    private val toml by lazy { source("relay/netlify/netlify.toml") }

    @Test
    fun `netlify relay is declared and comes before cloudflare routes`() {
        assertTrue(
            "ثابت پل Netlify",
            "const val DEFAULT_RELAY_NETLIFY = \"https://exam-relay-onlineexam.netlify.app\"" in router
        )
        // V335.32 — از این نسخه، پل ایرانی (sb3) نخستین مسیر است؛ پس ترتیب درست این است که
        // هر دو پل Netlify **پیش از** مسیرهای Cloudflare (`sb`/`relay`/`sb2`) و اتصال مستقیم بیایند.
        val listStart = router.indexOf("val DEFAULT_RELAYS: List<String> = listOf(")
        assertTrue("فهرست مسیرها پیدا نشد", listStart >= 0)
        val listEnd = router.indexOf("\n    )", listStart)
        val body = router.substring(listStart, if (listEnd > listStart) listEnd else router.length)
        val posNetlify = body.indexOf("DEFAULT_RELAY_NETLIFY,")
        val posCustom = body.indexOf("DEFAULT_RELAY_NETLIFY_CUSTOM")
        val posSb = body.indexOf("DEFAULT_RELAY,")
        assertTrue("پل Netlify در فهرست باشد", posNetlify > 0)
        assertTrue("نام اختصاصی پیش از نام netlify.app", posCustom in 1 until posNetlify)
        assertTrue("Netlify باید پیش از مسیر Cloudflare بیاید", posSb < 0 || posNetlify < posSb)
    }

    @Test
    fun `vercel relay stays as a secondary option`() {
        assertTrue(
            "پل Vercel پشتیبان بماند",
            "const val DEFAULT_RELAY_EDGE = \"https://exam-relay-onlineexam.vercel.app/api\"" in router
        )
        val listStart = router.indexOf("val DEFAULT_RELAYS: List<String> = listOf(")
        assertTrue("فهرست مسیرها پیدا نشد", listStart >= 0)
        // پایان فهرست با الگوی خطِ بستنِ پرانتز پیدا می‌شود؛ جست‌وجوی سادهٔ «)» به پرانتزهای داخل
        // کامنت‌های فارسی («(بدون Cloudflare)») می‌خورد و متن را زودتر از موعد می‌برد.
        val listEnd = router.indexOf("\n    )", listStart)
        val body = router.substring(listStart, if (listEnd > listStart) listEnd else router.length)
        assertTrue("پل Vercel در فهرست بماند", "DEFAULT_RELAY_EDGE" in body)
        assertTrue("مسیر قدیمی sb هم بماند", "DEFAULT_RELAY," in body)
    }

    @Test
    fun `netlify function forwards only supabase paths`() {
        assertTrue(
            "مسیرهای مجاز",
            "const ALLOWED_PREFIXES = [\"/rest/v1\", \"/auth/v1\", \"/storage/v1\", \"/functions/v1\", \"/realtime/v1\"];" in fn
        )
        assertTrue("پاسخ سلامت برای تست سریع", "pathname === \"/\" || pathname === \"/health\" || pathname === \"/api/health\"" in fn)
        assertTrue("بستن مسیرهای ناشناخته", "error: \"path_not_allowed\"" in fn)
        assertTrue("خودداری از انتقال کوکی", "\"set-cookie\"" in fn)
        assertTrue("پاسخ دودویی برای فایل‌ها", "isBase64Encoded: true" in fn)
        assertTrue("مسیر اصلی از rawUrl", "const raw = event.rawUrl || \"\"" in fn)
    }

    @Test
    fun `netlify routing sends every path to the relay`() {
        assertTrue("ابعاد تابع", "functions = \"functions\"" in toml)
        assertTrue("پوشهٔ انتشار", "publish = \"public\"" in toml)
        assertTrue("بازنویسی همهٔ مسیرها", "to = \"/.netlify/functions/relay\"" in toml)
        assertTrue("اعمال اجباری بازنویسی", "force = true" in toml)
    }

    @Test
    fun `netlify relay carries no credentials`() {
        assertFalse("هیچ توکن JWT در پل نباشد", "eyJ" in fn)
        assertFalse("هیچ کلید service_role در پل نباشد", "service_role" in fn)
        assertTrue("کلید عبوری از سمت اپ", "apikey" in fn)
        assertFalse("هیچ توکن JWT در تنظیمات نباشد", "eyJ" in toml)
    }
}
