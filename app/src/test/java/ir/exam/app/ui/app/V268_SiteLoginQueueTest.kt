package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** V268 — سایت: صف ورود برای ۴۲۹ (پین‌های سایت). */
class V268_SiteLoginQueueTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile }
    private val app = File(root(), "site/src/app.js").readText()

    @Test
    fun `sign-in queues on 429`() {
        assertTrue("var LOGIN_QUEUE_MS = 120000;" in app)
        assertTrue("if (e.status !== 429 || Date.now() - started > LOGIN_QUEUE_MS) throw e;" in app)
        assertTrue("toast('ورود هم‌زمان زیاد است؛ در صف ورود هستید، لطفاً صبر کنید…', 'info');" in app)
        assertTrue("await sleep(3000 + Math.random() * 4000);" in app)
    }
}
