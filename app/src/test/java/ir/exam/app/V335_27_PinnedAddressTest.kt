package ir.exam.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V335.27 — نگهبان «اتصال با نشانی تثبیت‌شده» (رفع خطای نام دامنه در شبکهٔ همراه اول):
 * ۱) نشانی‌های درست میزبان‌های پل و Supabase باید در برنامه تثبیت شده باشند.
 * ۲) هر دو کلاینت Supabase باید از همان OkHttpClient مشترک استفاده کنند.
 * ۳) سنجش مسیرها و دانلود رسانه هم باید از همان کلاینت رد شوند تا نتیجه با تجربهٔ واقعی یکی باشد.
 * ۴) گزارش پشتیبانی باید میزبان‌های تثبیت‌شده را نشان دهد.
 */
class V335_27_PinnedAddressTest {

    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: error("ریشهٔ مخزن پیدا نشد")
    }

    private fun source(relative: String): String = File(root(), relative).readText(Charsets.UTF_8)

    private val dns by lazy { source("app/src/main/java/ir/exam/app/core/network/RelayDns.kt") }
    private val router by lazy { source("app/src/main/java/ir/exam/app/core/network/SupabaseRouter.kt") }
    private val provider by lazy { source("app/src/main/java/ir/exam/app/data/remote/SupabaseProvider.kt") }
    private val publicProvider by lazy { source("app/src/main/java/ir/exam/app/data/remote/PublicUpdateSupabaseProvider.kt") }
    private val media by lazy { source("app/src/main/java/ir/exam/app/core/media/RemoteMediaBytes.kt") }
    private val gradle by lazy { source("app/build.gradle.kts") }

    @Test
    fun `relay hosts carry pinned addresses`() {
        assertTrue("کلاس حل نام تثبیت‌شده", "object RelayDns : Dns" in dns)
        assertTrue("نشانی‌های مسیر اول", "\"sb.onlineexam.ir\" to listOf(\"104.21.72.122\", \"172.67.184.28\")" in dns)
        assertTrue("نشانی‌های مسیر دوم", "\"relay.onlineexam.ir\" to listOf(\"104.21.72.122\", \"172.67.184.28\")" in dns)
        assertTrue("نشانی‌های Supabase", "\"eazwuyrymsvdkwckdpco.supabase.co\" to listOf(\"104.18.38.10\", \"172.64.149.246\")" in dns)
        assertTrue("پاسخ سیستم به‌عنوان پشتیبان", "val resolved = runCatching { Dns.SYSTEM.lookup(hostname) }" in dns)
        assertTrue("خطای نام دامنه با پیام فارسی", "throw UnknownHostException(\"نام دامنه پیدا نشد: " in dns)
    }

    @Test
    fun `shared okhttp client applies the pinned resolver`() {
        assertTrue("کلاینت مشترک", "object RelayHttp {" in dns)
        assertTrue("اعمال حل‌کنندهٔ تثبیت‌شده", ".dns(RelayDns)" in dns)
        assertTrue("نسخهٔ تایم‌اوت کوتاه برای سنجش", "fun withTimeout(ms: Long): OkHttpClient" in dns)
        assertTrue("وابستگی صریح OkHttp", "com.squareup.okhttp3:okhttp:4.12.0" in gradle)
    }

    @Test
    fun `both supabase clients use the shared engine`() {
        assertTrue(
            "کلاینت اصلی باید از موتور مشترک استفاده کند",
            "httpEngine = OkHttp.create { preconfigured = ir.exam.app.core.network.RelayHttp.client }" in provider
        )
        assertTrue(
            "کلاینت به‌روزرسانی هم باید از موتور مشترک استفاده کند",
            "httpEngine = OkHttp.create { preconfigured = ir.exam.app.core.network.RelayHttp.client }" in publicProvider
        )
        assertTrue("نشانی از مسیریاب", "val baseUrl = SupabaseRouter.currentBase()" in provider)
    }

    @Test
    fun `probes and media downloads share the same client`() {
        assertTrue("سنجش با کلاینت مشترک", "RelayHttp.withTimeout(timeoutMs.toLong()).newCall(request).execute()" in router)
        assertTrue("سنجش دیگر از HttpURLConnection استفاده نکند", "HttpURLConnection" !in router)
        assertTrue("دانلود رسانه با کلاینت مشترک", "ir.exam.app.core.network.RelayHttp.client.newBuilder()" in media)
        assertTrue("هدر نشست برای رسانه حفظ شود", "\"Authorization\" to \"Bearer \$token\", \"apikey\" to BuildConfig.SUPABASE_ANON_KEY" in media)
    }

    @Test
    fun `support report lists pinned hosts`() {
        assertTrue("میزبان‌های تثبیت‌شده در گزارش", "lines += \"نشانی تثبیت‌شده: \" + RelayDns.PINNED.keys.joinToString(\" , \")" in router)
        assertTrue("نسخهٔ اپ در گزارش", "BuildConfig.VERSION_NAME" in router)
    }
}
