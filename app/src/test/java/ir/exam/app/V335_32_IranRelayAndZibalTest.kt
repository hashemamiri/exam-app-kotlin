package ir.exam.app

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V335.32 — نگهبان «پل ایرانی به‌عنوان مسیر اول» و «درگاه زیبال»:
 *
 * ۱) `https://sb3.onlineexam.ir` (سرور ایرانی خودمان) نخستین مسیر فهرست باشد.
 * ۲) اپ نشانی شروع پرداخت زیبال (`gateway.zibal.ir`) را مجاز بداند.
 * ۳) تابع `wallet-payment` درگاه زیبال را با مرچنت کد سمت سرور و از مسیر پل پشتیبانی کند
 *    (هیچ مرچنت/کلیدی در اپ نباشد).
 * ۴) صفحهٔ بازگشت `site/pay-return.html` پارامترها را به همان تابع بفرستد.
 */
class V335_32_IranRelayAndZibalTest {

    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: error("ریشهٔ مخزن پیدا نشد")
    }

    private fun source(relative: String): String = File(root(), relative).readText(Charsets.UTF_8)

    private val router by lazy { source("app/src/main/java/ir/exam/app/core/network/SupabaseRouter.kt") }
    private val billing by lazy { source("app/src/main/java/ir/exam/app/data/repository/SupabaseBillingRepository.kt") }
    private val walletFn by lazy { source("supabase/functions/wallet-payment/index.ts") }
    private val returnPage by lazy { source("site/pay-return.html") }

    private fun relayOrder(): List<String> {
        val start = router.indexOf("val DEFAULT_RELAYS: List<String> = listOf(")
        assertTrue("فهرست مسیرها پیدا نشد", start >= 0)
        val end = router.indexOf("\n    )", start)
        val body = router.substring(start, if (end > start) end else router.length)
        return body.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("//") }
            .drop(1) // خطِ خودِ listOf(
            .map { it.trimEnd(',') }
            .toList()
    }

    @Test
    fun `iranian relay is the first route`() {
        assertTrue(
            "ثابت پل ایرانی",
            "const val DEFAULT_RELAY_IRAN = \"https://sb3.onlineexam.ir\"" in router
        )
        val order = relayOrder()
        assertEquals("نخستین مسیر باید پل ایرانی باشد: $order", "DEFAULT_RELAY_IRAN", order.first())
        assertTrue("پل Netlify باید در فهرست بماند", "DEFAULT_RELAY_NETLIFY" in order)
        assertTrue("پل روی دامنهٔ خودمان باید در فهرست بماند", "DEFAULT_RELAY_NETLIFY_CUSTOM" in order)
    }

    @Test
    fun `app accepts zibal checkout host`() {
        assertTrue("میزبان زیبال در فهرست مجاز", "host == \"gateway.zibal.ir\"" in billing)
        assertTrue("https اجباری می‌ماند", "uri.scheme.equals(\"https\", true)" in billing)
    }

    @Test
    fun `wallet function supports zibal through the relay`() {
        assertTrue("زیبال در فهرست providerها", "'zarinpal', 'idpay', 'sandbox', 'zibal'" in walletFn)
        assertTrue("مرچنت از سمت سرور", "env('PAY_ZIBAL_MERCHANT')" in walletFn)
        assertTrue("مسیر پل برای زیبال", "\$\{relay}/zibal/\$\{path}" in walletFn)
        assertTrue("نشانی شروع پرداخت زیبال", "https://gateway.zibal.ir/start/\$\{trackId}" in walletFn)
        assertTrue("تأیید با trackId", "trackId: order.authority" in walletFn)
        assertTrue("شناسهٔ بازگشت از trackId", "params.get('trackId')" in walletFn)
        assertTrue("تشخیص لغو زیبال", "success !== '1'" in walletFn)
        assertTrue("پیام فارسی کد ۱۱۵", "IP سرور در پنل زیبال ثبت نشده است." in walletFn)
        assertFalse("هیچ مرچنت کدی در اپ نباشد", "PAY_ZIBAL_MERCHANT" in billing)
    }

    @Test
    fun `callback can return through our own domain`() {
        assertTrue("دامنهٔ بازگشت قابل تنظیم", "env('PAY_CALLBACK_BASE')" in walletFn)
        assertTrue("صفحهٔ بازگشت موجود است", "wallet-payment" in returnPage)
        assertTrue("پارامترها دست‌نخورده پاس می‌شوند", "window.location.search" in returnPage)
        assertTrue("انتقال فوری", "window.location.replace(target)" in returnPage)
    }
}
