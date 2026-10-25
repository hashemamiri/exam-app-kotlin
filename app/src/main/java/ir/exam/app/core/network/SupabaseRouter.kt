package ir.exam.app.core.network

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import ir.exam.app.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * V335.20 — مسیریابِ اتصال Supabase برای شبکه‌هایی که دسترسی مستقیم به دامنهٔ پروژه را می‌بندند.
 *
 * ریشهٔ مشکل «اپ با اینترنت همراه اول کار نمی‌کند»: سایت روی دامنهٔ خودمان (Cloudflare) باز می‌شود،
 * ولی اپ به `https://<project>.supabase.co` وصل می‌شود و این دامنه از شبکهٔ برخی اپراتورها در دسترس
 * نیست. راه‌حل: یک «مسیر جایگزین» روی دامنهٔ خودمان (Cloudflare Worker یا Caddy روی VPS) که همان
 * درخواست‌ها را به پروژهٔ Supabase می‌رساند. این کلاس:
 *
 * ۱) فهرست مسیرها را می‌سازد: مسیرهای جایگزین (از `BuildConfig.SUPABASE_RELAY_URLS` یا مقدار
 *    پیش‌فرض `https://sb.onlineexam.ir`) و سپس اتصال مستقیم.
 * ۲) در پس‌زمینه با درخواست سبک `/auth/v1/health` هر مسیر را می‌سنجد، مسیر سالم را در حافظهٔ
 *    برنامه نگه می‌دارد و در صورت تغییر مسیر، کلاینت Supabase را بازسازی می‌کند.
 * ۳) با تغییر شبکه (Wi-Fi ↔ دادهٔ همراه) دوباره می‌سنجد؛ بنابراین اگر کاربر از Wi-Fi به همراه اول
 *    برود، برنامه خودکار روی مسیر جایگزین می‌رود.
 * ۴) نشانی‌های مطلقِ ذخیره‌شده در بانک (تصویر/صوت سؤال) را هم به همان مسیر ترجمه می‌کند.
 *
 * هیچ‌کدام از این کارها روی نخ اصلی انجام نمی‌شود؛ `currentBase()` فقط حافظهٔ محلی را می‌خواند.
 */
object SupabaseRouter {
    /** مسیر جایگزینِ پیش‌فرض؛ اگر Worker روی زیردامنهٔ دیگری ساخته شد، با SUPABASE_RELAY_URLS عوض می‌شود. */
    const val DEFAULT_RELAY = "https://sb.onlineexam.ir"

    /**
     * V335.29 — پل روی Netlify (گزینهٔ انتخاب‌شده).
     *
     * چرا Netlify و نه Cloudflare: اندازه‌گیری از دادهٔ همراه اول نشان داد TCP 443 به IPهای Cloudflare
     * (دامنهٔ پروژهٔ Supabase و هر سه زیردامنهٔ خودمان) بسته است، در حالی که `netlify.com` از همان شبکه
     * پاسخ `200` می‌دهد. این پل روی Netlify می‌نشیند و خودش (از دیتاسنتر) درخواست را به Supabase می‌رساند.
     *
     * نشانی پیش‌فرض عمداً ثابت است تا برای فعال شدن مسیر لازم نباشد کاربر کاری کند؛ اگر نام سایت عوض شد،
     * با `SUPABASE_RELAY_URLS` در زمان ساخت جایگزین می‌شود.
     */
    const val DEFAULT_RELAY_NETLIFY = "https://exam-relay-onlineexam.netlify.app"

    /**
     * V335.28 — پل روی میزبانی Vercel (گزینهٔ پشتیبان؛ اگر روزی حساب Vercel هم فعال شد همان‌جا می‌نشیند).
     *
     * چرا: اندازه‌گیری روی دادهٔ همراه اول نشان داد اتصال TCP به IPهای Cloudflare (هم دامنهٔ پروژهٔ
     * Supabase و هم هر دو زیردامنهٔ خودمان) کاملاً بسته است — همان چیزی که هم `curl` و هم اپ
     * تجربه می‌کنند — در حالی که همان لحظه `vercel.com` و نشانی لبهٔ این میزبان با پاسخ سالم
     * برمی‌گردند. پس ترافیک اپ از این پل رد می‌شود و پل (که خودش در دیتاسنتر است) درخواست را به
     * Supabase می‌رساند.
     *
     * نشانی پیش‌فرض عمداً ثابت است تا کاربر برای فعال شدن مسیر لازم نباشد کاری کند؛ اگر روزی نام
     * پروژهٔ پل عوض شد، کافی است با `SUPABASE_RELAY_URLS` در زمان ساخت جایگزین شود.
     */
    const val DEFAULT_RELAY_EDGE = "https://exam-relay-onlineexam.vercel.app/api"

    /**
     * V335.24 — فهرست مسیرهای جایگزین پیش‌فرض.
     *
     * چرا چند مسیر: زیردامنهٔ تازه‌ساخته تا حدود ۳۰ دقیقه ممکن است در برخی مسیریاب‌های DNS
     * «وجود ندارد» (منفی‌کش SOA) پاسخ بگیرد؛ تجربه نشان داد همین باعث می‌شد اپ روی دادهٔ همراه اول
     * یک‌بار وصل شود و بار بعد «نام دامنه پیدا نشد» بدهد. با داشتن یک نام تازهٔ دیگر (که هرگز
     * منفی‌کش نشده) برنامه در همان ثانیه راه سالم را پیدا می‌کند.
     */
    val DEFAULT_RELAYS: List<String> = listOf(
        // V335.29 — اول از همه: پل Netlify (بدون Cloudflare) که شبکهٔ همراه اول باز می‌گذارد.
        DEFAULT_RELAY_NETLIFY,
        // V335.28 — پل Vercel هم به‌عنوان گزینهٔ دوم می‌ماند تا اگر یکی از این دو میزبان روزی کند شد،
        // مسیر دیگر در همان لحظه جواب دهد.
        DEFAULT_RELAY_EDGE,
        DEFAULT_RELAY,
        "https://relay.onlineexam.ir",
        // V335.26 — مسیر روی سرور خودمان با رکورد A ساده (بدون Cloudflare)؛ چون شبکهٔ همراه اول
        // IPهای پروکسی Cloudflare را باز نمی‌کند و هر دو نام بالا از همان IPها سرو می‌شوند.
        "https://sb2.onlineexam.ir"
    )

    private const val PREFS = "supabase_router"
    private const val KEY_BASE = "active_base"
    private const val KEY_AT = "active_at_ms"
    private const val KEY_MANUAL = "manual_base"
    private const val REFRESH_DEBOUNCE_MS = 8_000L

    @Volatile private var prefs: SharedPreferences? = null
    @Volatile private var watcherRegistered = false
    @Volatile private var lastRefreshAt = 0L

    /** نتیجهٔ آخرین سنجش (برای صفحهٔ «تست اتصال به سرور»). */
    @Volatile
    var lastProbe: List<Probe> = emptyList()
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** اتصال مستقیم به پروژهٔ Supabase (نشانی زمان ساخت). */
    val directBase: String get() = BuildConfig.SUPABASE_URL.trim().trimEnd('/')

    /** مسیرهای جایگزین؛ اولویت با مقدار زمان ساخت و در نبود آن پل پیش‌فرض. */
    fun relayBases(): List<String> {
        val configured = runCatching { BuildConfig.SUPABASE_RELAY_URLS }
            .getOrDefault("")
            .split(',', ';', ' ')
            .map { it.trim().trimEnd('/') }
            .filter { it.startsWith("http://", true) || it.startsWith("https://", true) }
            .distinct()
        return configured.ifEmpty { DEFAULT_RELAYS }
    }

    /** ترتیب تلاش: مسیرهای جایگزین، سپس اتصال مستقیم. */
    fun candidates(): List<String> = (relayBases() + directBase)
        .filter { it.isNotBlank() }
        .distinct()

    fun attach(context: Context) {
        if (prefs == null) prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    /** مسیر دستیِ انتخاب‌شده توسط کاربر در صفحهٔ تست اتصال (خالی = انتخاب خودکار). */
    fun manualBase(): String? = prefs?.getString(KEY_MANUAL, null)?.takeIf { it.isNotBlank() }

    /**
     * نشانی فعال؛ فقط از حافظهٔ محلی خوانده می‌شود و روی نخ اصلی هم امن است.
     * تا پیش از نخستین سنجش پس‌زمینه، اتصال مستقیم (رفتار همهٔ نسخه‌های قبلی) استفاده می‌شود تا هیچ
     * نصبی که پل را هنوز نساخته، با آدرس پلِ ناساخته روبه‌رو نشود.
     */
    fun currentBase(): String {
        manualBase()?.let { return it }
        val cached = prefs?.getString(KEY_BASE, null)?.takeIf { it.isNotBlank() }
        return cached ?: directBase
    }

    /** نقطهٔ آغاز: ثبت شنوندهٔ شبکه و سنجش پس‌زمینه. */
    fun bootstrap(context: Context) {
        attach(context)
        registerNetworkWatcher(context)
        refreshAsync(force = true)
    }

    /**
     * سنجش همهٔ مسیرها در پس‌زمینه و انتخاب بهترین؛ `onDone` روی نخ IO صدا زده می‌شود.
     *
     * V335.23 — «نخستین پاسخ سالم برنده است»: تا پیش از این نسخه، انتخاب مسیر تا پایان سنجش همهٔ
     * مسیرها (تا ۶ ثانیه، چون سنجش اتصال بستهٔ مستقیم تا تایم‌اوت معطل می‌ماند) عقب می‌افتاد و در همین
     * فاصله درخواست‌های ورود روی مسیر بسته می‌رفتند. اکنون هر مسیری که اول سالم پاسخ دهد همان لحظه
     * انتخاب و کلاینت بازسازی می‌شود.
     */
    fun refreshAsync(force: Boolean = false, onDone: (List<Probe>) -> Unit = {}) {
        val now = System.currentTimeMillis()
        if (!force && now - lastRefreshAt < REFRESH_DEBOUNCE_MS) return
        lastRefreshAt = now
        scope.launch {
            val results = java.util.Collections.synchronizedList(mutableListOf<Probe>())
            val decided = java.util.concurrent.atomic.AtomicBoolean(false)
            runCatching {
                coroutineScope {
                    candidates().map { base ->
                        async {
                            val outcome = probe(base)
                            results += outcome
                            if (outcome.ok && manualBase() == null && decided.compareAndSet(false, true)) {
                                remember(outcome.base)
                            }
                        }
                    }.awaitAll()
                }
            }
            val list = results.toList()
            lastProbe = list
            runCatching { onDone(list) }
            if (!decided.get()) {
                val best = list.firstOrNull { it.ok } ?: return@launch
                if (manualBase() == null) remember(best.base)
            }
        }
    }

    /**
     * V335.23 — «نجات فوری مسیر»: وقتی درخواستی با خطای شبکه شکست خورد، بی‌درنگ همهٔ مسیرها را
     * می‌سنجد و اگر مسیر سالمی هست آن را فعال می‌کند. خروجی `true` یعنی دست‌کم یک مسیر سالم وجود دارد
     * و تکرار همان درخواست منطقی است (مثلاً یک بار تلاش دوبارهٔ ورود).
     */
    suspend fun recover(timeoutMs: Int = 3_500): Boolean {
        val results = runCatching { probeAll(timeoutMs) }.getOrDefault(emptyList())
        if (results.isNotEmpty()) lastProbe = results
        val best = results.firstOrNull { it.ok } ?: return false
        if (manualBase() == null) remember(best.base)
        return true
    }

    /** ذخیرهٔ مسیر سالم؛ اگر مسیر عوض شود، کلاینت Supabase بازسازی می‌شود. */
    fun remember(base: String) {
        val p = prefs ?: return
        val old = p.getString(KEY_BASE, null)
        p.edit().putString(KEY_BASE, base).putLong(KEY_AT, System.currentTimeMillis()).apply()
        if (old != base) resetClient()
    }

    /** انتخاب دستی از صفحهٔ تست اتصال؛ `null` = بازگشت به انتخاب خودکار. */
    fun selectManual(base: String?) {
        val p = prefs ?: return
        if (base.isNullOrBlank()) p.edit().remove(KEY_MANUAL).apply() else p.edit().putString(KEY_MANUAL, base).apply()
        resetClient()
    }

    private fun resetClient() {
        runCatching { ir.exam.app.data.remote.SupabaseProvider.reset() }
    }

    /** سنجش همهٔ مسیرها به‌صورت موازی. */
    suspend fun probeAll(timeoutMs: Int = 6_000): List<Probe> = withContext(Dispatchers.IO) {
        coroutineScope { candidates().map { base -> async { probe(base, timeoutMs) } }.awaitAll() }
    }

    /** یک درخواست سبک به `/auth/v1/health` (بدون توکن کاربر) و اندازه‌گیری زمان. */
    fun probe(base: String, timeoutMs: Int = 6_000): Probe = probe(base, timeoutMs, 0)

    /**
     * V335.27 — سنجش با همان کلاینتی انجام می‌شود که ورود واقعی از آن استفاده می‌کند
     * (`RelayHttp` + `RelayDns`)، تا نتیجهٔ «تست اتصال» با تجربهٔ واقعی یکی باشد.
     */
    private fun probe(base: String, timeoutMs: Int, attempt: Int): Probe {
        val started = System.currentTimeMillis()
        return try {
            val request = okhttp3.Request.Builder()
                .url(base.trimEnd('/') + "/auth/v1/health")
                .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
                .header("Accept", "application/json")
                .get()
                .build()
            RelayHttp.withTimeout(timeoutMs.toLong()).newCall(request).execute().use { response ->
                val ms = System.currentTimeMillis() - started
                Probe(base, response.isSuccessful, ms, if (response.isSuccessful) "سالم" else "پاسخ سرور: ${response.code}")
            }
        } catch (e: Throwable) {
            // V335.24 — خطای گذرای «نام دامنه پیدا نشد» یک‌بار دیگر (پس از ۲۵۰ میلی‌ثانیه) آزمایش می‌شود.
            if (e is UnknownHostException && attempt < 1) {
                runCatching { Thread.sleep(250) }
                return probe(base, timeoutMs, attempt + 1)
            }
            Probe(base, false, System.currentTimeMillis() - started, describe(e))
        }
    }

    /** ترجمهٔ خطای شبکه به توضیح کوتاه فارسی (بدون افشای آدرس/توکن). */
    private fun describe(error: Throwable): String = when (error) {
        is UnknownHostException -> "نام دامنه پیدا نشد (DNS)"
        is SocketTimeoutException -> "پاسخی نداد (قطع/کندی شبکه)"
        is java.io.InterruptedIOException -> "پاسخی نداد (قطع/کندی شبکه)"
        is SSLException -> "خطای اتصال امن (TLS)"
        is ConnectException -> "اتصال برقرار نشد"
        is IOException -> "خطای شبکه"
        else -> "خطای نامشخص"
    }

    /**
     * نشانی مطلق Supabase (تصویر/صوت ذخیره‌شده در بانک) را به مسیر فعال ترجمه می‌کند؛
     * اگر نشانی از همین پروژه نباشد، دست‌نخورده برمی‌گردد.
     */
    fun rewrite(url: String): String {
        val trimmed = url.trim()
        if (!trimmed.startsWith("http://", true) && !trimmed.startsWith("https://", true)) return url
        val base = currentBase()
        val own = candidates() + directBase
        val match = own.firstOrNull { trimmed.startsWith("$it/", ignoreCase = true) } ?: return url
        if (match.equals(base, ignoreCase = true)) return url
        return base.trimEnd('/') + trimmed.substring(match.length)
    }

    /** آیا نشانی به فضای ذخیره‌سازی همین پروژه اشاره می‌کند (برای فرستادن توکن نشست)؟ */
    fun isOwnStorageUrl(url: String): Boolean {
        if (!url.contains("/storage/v1/object/")) return false
        val own = candidates() + directBase
        return own.any { url.startsWith("$it/storage/v1/object/", ignoreCase = true) }
    }

    /** گزارش یک‌خطی برای دکمهٔ «کپی گزارش» در صفحهٔ تست اتصال. */
    fun report(): String {
        val lines = mutableListOf<String>()
        // V335.26 — نسخهٔ اپ و زمان در گزارش، برای تشخیص اینکه گزارش از کدام ساخت آمده است.
        lines += "نسخهٔ اپ: " + runCatching { BuildConfig.VERSION_NAME }.getOrDefault("?")
        lines += "مسیر فعال: ${currentBase()}"
        lines += "فهرست مسیرها: " + candidates().joinToString(" , ")
        lines += "نشانی تثبیت‌شده: " + RelayDns.PINNED.keys.joinToString(" , ")
        lines += "نوع انتخاب: " + (manualBase()?.let { "دستی" } ?: "خودکار")
        lastProbe.forEach { p ->
            val kind = if (p.base.equals(directBase, true)) "مستقیم" else "جایگزین"
            lines += "$kind | ${p.ok.let { if (it) "سالم" else "ناموفق" }} | ${p.ms}ms | ${p.detail} | ${p.base}"
        }
        return lines.joinToString("\n")
    }

    data class Probe(val base: String, val ok: Boolean, val ms: Long, val detail: String)

    private fun registerNetworkWatcher(context: Context) {
        if (watcherRegistered) return
        val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { refreshAsync() }
            override fun onLost(network: Network) { refreshAsync() }
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) { refreshAsync() }
        }
        runCatching { cm.registerNetworkCallback(request, callback); watcherRegistered = true }
    }
}
