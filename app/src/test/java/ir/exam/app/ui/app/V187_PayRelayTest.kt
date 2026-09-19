package ir.exam.app.ui.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V187 — پلِ IP ثابت برای زرین‌پال: relay/ + عبور فراخوانی‌های زرین‌پال از PAY_RELAY_URL در wallet-payment. */
class V187_PayRelayTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: File("")
    }
    private fun source(path: String) = File(root(), path).readText()

    @Test
    fun `wallet-payment routes zarinpal through relay when configured`() {
        val f = source("supabase/functions/wallet-payment/index.ts")
        assertTrue("async function zarinpalPost(path: 'request' | 'verify'" in f)
        assertTrue("env('PAY_RELAY_URL')" in f && "env('PAY_RELAY_TOKEN')" in f && "'X-Relay-Token': token" in f)
        assertTrue("zarinpalPost('request', {" in f && "zarinpalPost('verify', {" in f)
        assertEquals(0, Regex("fetch\\('https://payment\\.zarinpal\\.com").findAll(f).count())
        assertTrue("https://payment.zarinpal.com/pg/v4/payment/\${path}.json" in f)
    }

    @Test
    fun `relay only forwards fixed zarinpal endpoints with token`() {
        val r = source("relay/pay_relay.py")
        assertTrue("hmac.compare_digest(self.headers.get('X-Relay-Token', ''), TOKEN)" in r)
        assertTrue("'/request': 'https://payment.zarinpal.com/pg/v4/payment/request.json'" in r)
        assertTrue("'/verify': 'https://payment.zarinpal.com/pg/v4/payment/verify.json'" in r)
        assertTrue("ThreadingHTTPServer(('127.0.0.1', PORT), H)" in r)
        assertFalse("merchant" in r.lowercase().substringAfter("class H"))
        assertTrue(File(root(), "relay/README_FA.md").isFile && File(root(), "relay/pay-relay.service").isFile && File(root(), "relay/Caddyfile").isFile)
    }
}
