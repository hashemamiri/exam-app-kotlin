package ir.exam.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V335.20 — «اپ با اینترنت همراه اول کار نمی‌کند»: مسیر جایگزین Supabase روی دامنهٔ خودمان.
 *
 * نگهبان رگرسیون برای:
 * - وجود مسیریابِ اتصال (SupabaseRouter) با سنجش سلامت، انتخاب خودکار، انتخاب دستی و ترجمهٔ نشانی‌ها
 * - استفادهٔ هر سه لایه (Auth/Postgrest/Storage، بارگذاری تصویر و رسانهٔ راه‌دور) از مسیر فعال
 * - بازسازی کلاینت با تغییر شبکه و کارت «تست اتصال به سرور» در تنظیمات
 * - آماده بودن پل Cloudflare (فایل Worker و راهنمای فارسی)
 */
class V335_20_SupabaseRelayFailoverTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "app/src/main/java/ir/exam/app/ExamApplication.kt").isFile) dir = dir.parentFile
        return dir ?: File("")
    }

    private fun src(path: String): String = File(root(), path).readText()
    private fun exists(path: String): Boolean = File(root(), path).isFile

    @Test
    fun routerProbesBothPathsAndRewritesOwnUrls() {
        val router = src("app/src/main/java/ir/exam/app/core/network/SupabaseRouter.kt")
        assertTrue("object SupabaseRouter {" in router)
        assertTrue("const val DEFAULT_RELAY = \"https://sb.onlineexam.ir\"" in router)
        assertTrue("fun candidates(): List<String>" in router)
        assertTrue("fun currentBase(): String" in router)
        assertTrue("return cached ?: directBase" in router)
        assertTrue("suspend fun probeAll(timeoutMs: Int = 6_000): List<Probe>" in router)
        assertTrue("/auth/v1/health" in router)
        assertTrue("fun rewrite(url: String): String" in router)
        assertTrue("fun isOwnStorageUrl(url: String): Boolean" in router)
        assertTrue("fun selectManual(base: String?)" in router)
        assertTrue("fun bootstrap(context: Context)" in router)
        assertTrue("ConnectivityManager.NetworkCallback()" in router)
        assertTrue("is UnknownHostException -> \"نام دامنه پیدا نشد (DNS)\"" in router)
        // هیچ توکن یا کلید ثابتی در این فایل نیست
        assertFalse("sb_secret_" in router)
    }

    @Test
    fun everyNetworkLayerUsesTheActiveRoute() {
        val provider = src("app/src/main/java/ir/exam/app/data/remote/SupabaseProvider.kt")
        assertTrue("val baseUrl = SupabaseRouter.currentBase()" in provider)
        assertTrue("supabaseUrl = baseUrl" in provider)
        assertTrue("fun reset()" in provider)
        assertTrue("sessionManager = EncryptedSessionManager(context)" in provider)

        val updateProvider = src("app/src/main/java/ir/exam/app/data/remote/PublicUpdateSupabaseProvider.kt")
        assertTrue("val baseUrl = SupabaseRouter.currentBase()" in updateProvider)

        val media = src("app/src/main/java/ir/exam/app/core/media/RemoteMediaBytes.kt")
        assertTrue("val target = ir.exam.app.core.network.SupabaseRouter.rewrite(url)" in media)
        assertTrue("authHeaders(target)" in media)

        val imageInterceptor = src("app/src/main/java/ir/exam/app/ui/image/SupabaseAuthImageInterceptor.kt")
        assertTrue("ir.exam.app.core.network.SupabaseRouter.isOwnStorageUrl(value)" in imageInterceptor)

        val loader = src("app/src/main/java/ir/exam/app/ui/image/PrivateImageLoader.kt")
        assertTrue("add(SupabaseHostRewriteInterceptor())" in loader)
        assertTrue("add(SupabaseAuthImageInterceptor())" in loader)

        val rewrite = src("app/src/main/java/ir/exam/app/ui/image/SupabaseHostRewriteInterceptor.kt")
        assertTrue("class SupabaseHostRewriteInterceptor : Interceptor" in rewrite)
        assertTrue("SupabaseRouter.rewrite(data)" in rewrite)

        val app = src("app/src/main/java/ir/exam/app/ExamApplication.kt")
        assertTrue("SupabaseRouter.bootstrap(this)" in app)
    }

    @Test
    fun settingsExposeConnectionTestCard() {
        val settings = src("app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsScreen.kt")
        assertTrue("SettingsAccordionCard(title = \"تست اتصال به سرور\"" in settings)
        assertTrue("NetworkDiagnosticsCard()" in settings)
        val card = src("app/src/main/java/ir/exam/app/ui/profile/NetworkDiagnostics.kt")
        assertTrue("internal fun NetworkDiagnosticsCard()" in card)
        assertTrue("SupabaseRouter.probeAll()" in card)
        assertTrue("کپی گزارش" in card)
        assertTrue("SupabaseRouter.selectManual(null)" in card)
    }

    @Test
    fun relayWorkerAndGuideAreShipped() {
        assertTrue(exists("relay/cloudflare-worker-supabase.js"))
        assertTrue(exists("docs/fa/RELAY_SUPABASE_CLOUDFLARE_FA.md"))
        val worker = src("relay/cloudflare-worker-supabase.js")
        assertTrue("SUPABASE_ORIGIN" in worker)
        assertTrue("outHeaders.delete(\"content-encoding\")" in worker)
        assertTrue("relay_upstream_unreachable" in worker)
        val guide = src("docs/fa/RELAY_SUPABASE_CLOUDFLARE_FA.md")
        assertTrue("sb.onlineexam.ir" in guide)
        assertTrue("reverse_proxy https://eazwuyrymsvdkwckdpco.supabase.co" in guide)
        val gradle = src("app/build.gradle.kts")
        assertTrue("buildConfigField(\"String\", \"SUPABASE_RELAY_URLS\"" in gradle)
    }
}
