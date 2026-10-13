package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** V266 — سایت: تلاش مجدد فقط برای ۴۲۹/۵۰۳ و تازه‌سازی نشست با پنجرهٔ تصادفی (پین‌های سایت). */
class V266_SiteLoadHardeningTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile }
    private val app = File(root(), "site/src/app.js").readText()

    @Test
    fun `http retries only 429 and 503 with jitter`() {
        assertTrue("var RETRY_DELAYS = [700, 1800, 4000];" in app)
        assertTrue("if ((res.status !== 429 && res.status !== 503) || attempt >= RETRY_DELAYS.length) break;" in app)
        assertTrue("await sleep(RETRY_DELAYS[attempt] + Math.random() * RETRY_DELAYS[attempt]);" in app)
    }

    @Test
    fun `session refresh is spread and retried later`() {
        assertTrue("var REFRESH_LEAD_MS = (2 + Math.random() * 10) * 60 * 1000;" in app)
        assertTrue("if (exp && exp - Date.now() > REFRESH_LEAD_MS) return Promise.resolve();" in app)
        assertTrue("else refreshRetryAt = Date.now() + 20000 + Math.random() * 40000;" in app)
        assertTrue(app.indexOf("var RETRY_DELAYS") < app.indexOf("async function http("))
    }
}
