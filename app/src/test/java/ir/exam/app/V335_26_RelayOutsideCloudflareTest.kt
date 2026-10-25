package ir.exam.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V335.26 — نگهبان «پل بدون Cloudflare و تست اتصال پیش از ورود»:
 * ۱) مسیر جایگزین سوم (`sb2.onlineexam.ir`) باید در فهرست پیش‌فرض باشد؛ چون شبکهٔ همراه اول
 *    IPهای پروکسی Cloudflare را باز نمی‌کند و دو مسیر قبلی روی همان IPها هستند.
 * ۲) صفحهٔ ورود باید دکمهٔ «تست اتصال به سرور» را پیش از وارد شدن نشان دهد.
 * ۳) گزارش پشتیبانی باید نسخهٔ اپ و فهرست مسیرها را داشته باشد.
 */
class V335_26_RelayOutsideCloudflareTest {

    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: error("ریشهٔ مخزن پیدا نشد")
    }

    private fun source(relative: String): String = File(root(), relative).readText(Charsets.UTF_8)

    private val router by lazy { source("app/src/main/java/ir/exam/app/core/network/SupabaseRouter.kt") }
    private val signIn by lazy { source("app/src/main/java/ir/exam/app/ui/auth/SignInScreen.kt") }

    @Test
    fun `a relay outside cloudflare is part of the default list`() {
        assertTrue("میزبان پل روی سرور خودمان باید در فهرست باشد", "\"https://sb2.onlineexam.ir\"" in router)
        assertTrue("فهرست پیش‌فرض باید حفظ شود", "val DEFAULT_RELAYS: List<String> = listOf(" in router)
        assertTrue("مسیر اول همان sb بماند", "const val DEFAULT_RELAY = \"https://sb.onlineexam.ir\"" in router)
    }

    @Test
    fun `support report carries app version and route list`() {
        assertTrue("نسخهٔ اپ در گزارش", "lines += \"نسخهٔ اپ: \" + runCatching { BuildConfig.VERSION_NAME }" in router)
        assertTrue("فهرست مسیرها در گزارش", "lines += \"فهرست مسیرها: \" + candidates().joinToString(\" , \")" in router)
        assertTrue("مسیر فعال در گزارش", "lines += \"مسیر فعال: \${currentBase()}\"" in router)
    }

    @Test
    fun `sign in screen offers the connection test before login`() {
        assertTrue("دکمهٔ تست اتصال باید در صفحهٔ ورود باشد", "TextButton(onClick = { showNetDiag = true }) { Text(\"تست اتصال به سرور\") }" in signIn)
        assertTrue("کارت تست اتصال باید داخل پنجره باز شود", "NetworkDiagnosticsCard()" in signIn)
        assertTrue("پنجره باید بسته شود", "onDismissRequest = { showNetDiag = false }" in signIn)
        assertTrue("کارت باید اسکرول‌پذیر باشد", "heightIn(max = 440.dp)" in signIn)
    }

    @Test
    fun `existing route resilience stays in place`() {
        assertTrue("تلاش دوبارهٔ DNS", "if (e is UnknownHostException && attempt < 1)" in router)
        assertTrue("نجات مسیر", "suspend fun recover(timeoutMs: Int = 3_500): Boolean" in router)
        assertTrue("انتخاب فوری", "decided.compareAndSet(false, true)" in router)
    }
}
