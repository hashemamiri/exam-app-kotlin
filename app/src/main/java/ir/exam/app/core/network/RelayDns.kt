package ir.exam.app.core.network

import okhttp3.Dns
import okhttp3.OkHttpClient
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * V335.27 — «حل‌کنندهٔ نام با نشانی‌های تثبیت‌شده».
 *
 * چرا: در شبکهٔ دادهٔ همراه اول، پرسش نام برای دامنهٔ پل (`sb.onlineexam.ir`) یا پاسخ غلط/مسدود
 * برمی‌گردد یا خودِ پرسش فیلتر می‌شود؛ نتیجه اینکه اپ «نام دامنه پیدا نشد» یا «اتصال برقرار نشد»
 * می‌گیرد، در حالی که همان سرور با نشانی عددی در دسترس است (تست مستقیم IP در مرورگر گوشی).
 *
 * راه‌حل: برای میزبان‌های شناخته‌شده، نشانی‌های درستِ سرور از پیش در برنامه نگه داشته می‌شود و
 * پیش از پاسخ سیستم برگردانده می‌شود. نام دامنه (SNI/Host) همان می‌ماند، پس گواهی HTTPS و مسیر
 * Cloudflare/Supabase دقیقاً مثل حالت عادی کار می‌کند؛ فقط مرحلهٔ «پرسیدن نشانی از DNS» حذف می‌شود.
 * اگر نشانی تثبیت‌شده روزی از کار بیفتد، پاسخ طبیعی سیستم هم در انتهای فهرست اضافه می‌شود تا
 * اتصال از دست نرود.
 */
object RelayDns : Dns {

    /** میزبان → نشانی‌های درست (به ترتیب تلاش). */
    val PINNED: Map<String, List<String>> = mapOf(
        "sb.onlineexam.ir" to listOf("104.21.72.122", "172.67.184.28"),
        "relay.onlineexam.ir" to listOf("104.21.72.122", "172.67.184.28"),
        "eazwuyrymsvdkwckdpco.supabase.co" to listOf("104.18.38.10", "172.64.149.246"),
        // V335.28 — لبهٔ میزبان پل (بدون Cloudflare). اگر روزی نشانی عوض شود، پاسخ سیستم هم در
        // انتهای فهرست می‌آید و اتصال از دست نمی‌رود.
        "exam-relay-onlineexam.vercel.app" to listOf("216.198.79.195", "64.29.17.195")
    )

    override fun lookup(hostname: String): List<InetAddress> {
        val key = hostname.lowercase()
        val pinned = PINNED[key].orEmpty().mapNotNull { ip ->
            runCatching { InetAddress.getByName(ip) }.getOrNull()
        }
        val resolved = runCatching { Dns.SYSTEM.lookup(hostname) }.getOrDefault(emptyList())
        val all = (pinned + resolved).distinctBy { it.hostAddress.orEmpty() }
        if (all.isEmpty()) throw UnknownHostException("نام دامنه پیدا نشد: $hostname")
        return all
    }

    /** آیا این میزبان نشانی تثبیت‌شده دارد؟ (برای گزارش پشتیبانی) */
    fun isPinned(hostname: String): Boolean = PINNED.containsKey(hostname.lowercase())
}

/**
 * V335.27 — یک OkHttpClient مشترک که از [RelayDns] استفاده می‌کند.
 *
 * همهٔ درخواست‌های اپ (کلاینت Supabase و سنجش مسیرها) از همین کلاینت رد می‌شوند تا نتیجهٔ «تست اتصال»
 * دقیقاً همان چیزی باشد که ورود واقعی تجربه می‌کند.
 */
object RelayHttp {

    private const val CONNECT_TIMEOUT_S = 15L
    private const val READ_TIMEOUT_S = 30L

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .dns(RelayDns)
            .connectTimeout(CONNECT_TIMEOUT_S, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_S, TimeUnit.SECONDS)
            .writeTimeout(READ_TIMEOUT_S, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /** همان کلاینت با تایم‌اوت کوتاه‌تر برای سنجش سریع مسیرها. */
    fun withTimeout(ms: Long): OkHttpClient = client.newBuilder()
        .connectTimeout(ms, TimeUnit.MILLISECONDS)
        .readTimeout(ms, TimeUnit.MILLISECONDS)
        .callTimeout(ms + 1_000L, TimeUnit.MILLISECONDS)
        .build()
}
