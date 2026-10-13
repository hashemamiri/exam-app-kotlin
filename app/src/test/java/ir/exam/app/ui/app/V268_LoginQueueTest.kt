package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** V268 — صف ورود: ۴۲۹ هنگام ورود با رمز تا ۱۲۰ ثانیه با فاصلهٔ تصادفی دوباره تلاش می‌شود (اپ). */
class V268_LoginQueueTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile }
    private val repo = File(root(), "app/src/main/java/ir/exam/app/data/repository/SupabaseAuthRepository.kt").readText()

    @Test
    fun `password sign-in retries only on rate limit within window`() {
        assertTrue("signInQueued(loginEmail, password)" in repo)
        assertTrue("if (!UserFacingError.isRateLimited(e.message) || System.currentTimeMillis() - started > LOGIN_QUEUE_MS) throw e" in repo)
        assertTrue("delay(3000L + Random.nextLong(4000L))" in repo)
        assertTrue("private const val LOGIN_QUEUE_MS = 120_000L" in repo)
        assertTrue("} catch (e: CancellationException) {\n                throw e" in repo)
    }
}
