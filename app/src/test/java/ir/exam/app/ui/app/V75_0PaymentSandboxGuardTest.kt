package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V75.0 — محافظ شارژ آزمایشی کیف پول (بند ۱.۱ گزارش امنیتی):
 * تابع wallet-payment وقتی PAY_PROVIDER=sandbox باشد، بدون هیچ تأیید بانکی
 * مستقیم native_credit_wallet_payment را صدا می‌زد؛ مبلغ هر سفارش محدود بود
 * اما تعداد سفارش نامحدود ⇒ امکان شارژ رایگان و نامحدود برای هر کاربر.
 * اکنون علاوه بر PAY_ALLOW_SANDBOX یک کلید جداگانهٔ سرور (PAY_SANDBOX_TOKEN)
 * در هدر x-sandbox-token لازم است و مسیر callback هم همان را می‌طلبد.
 */
class V75_0PaymentSandboxGuardTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val payment by lazy {
        File(root(), "supabase/functions/wallet-payment/index.ts").readText()
    }

    @Test
    fun `sandbox credit needs a dedicated server token`() {
        assertTrue("function sandboxRequestAllowed(req: Request): boolean" in payment)
        assertTrue("env('PAY_SANDBOX_TOKEN')" in payment)
        assertTrue("req.headers.get('x-sandbox-token')" in payment)
        assertTrue("expected.length < 16" in payment)
        val start = payment.substringAfter("const provider = safeProvider();")
            .substringBefore("const { data: created")
        assertTrue("provider === 'sandbox' && !sandboxRequestAllowed(req)" in start)
        assertTrue("sandbox_token_required" in start)
    }

    @Test
    fun `verify callback refuses sandbox payments without the token`() {
        val branch = payment.substringAfter("if (order.status === 'paid')")
            .substringBefore("if (callbackCanceled(")
        assertTrue("order.provider === 'sandbox' && !sandboxRequestAllowed(req)" in branch)
    }

    @Test
    fun `token comparison is constant time and never leaks the secret`() {
        assertTrue("function timingSafeEqualText(" in payment)
        assertFalse("expected === provided" in payment)
        assertFalse("PAY_SANDBOX_TOKEN" in payment.substringAfter("return json({ ok: true, credited: true"))
    }
}
