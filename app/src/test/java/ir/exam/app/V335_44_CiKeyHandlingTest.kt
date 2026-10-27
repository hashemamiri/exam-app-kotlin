package ir.exam.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V335.44 — نگهبان «کلید SSH در CI».
 *
 * درس عملی: اجرای Actions با پیام `Load key "/home/runner/.ssh/arvan":
 * error in libcrypto` شکست. سه حالت همین خطا را می‌سازد: خط‌های CRLF،
 * نبودِ newline در انتهای کلید، و مقدار مثله/ناقص.
 *
 * این تست تضمین می‌کند گام CI:
 *  ۱) CRLF را حذف می‌کند؛
 *  ۲) newline پایانی را تضمین می‌کند؛
 *  ۳) هم متن PEM و هم base64 را می‌پذیرد؛
 *  ۴) پیش از ارسال با `ssh-keygen -y` سلامت کلید را می‌سنجد.
 */
class V335_44_CiKeyHandlingTest {

    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: error("ریشهٔ مخزن پیدا نشد")
    }

    private fun src(relative: String): String = File(root(), relative).readText(Charsets.UTF_8)

    private val workflow = src(".github/workflows/site.yml")
    private val snippet = src("docs/ci/site_arvan_sync_step.yml")

    @Test
    fun `ci strips windows line endings from the key`() {
        assertTrue("حذف CR", "tr -d '\\r'" in workflow)
        assertTrue("در قطعهٔ مرجع هم هست", "tr -d '\\r'" in snippet)
    }

    @Test
    fun `ci guarantees a trailing newline`() {
        assertTrue("تضمین خط آخر", "tail -c1" in workflow)
        assertTrue("در قطعهٔ مرجع هم هست", "tail -c1" in snippet)
    }

    @Test
    fun `ci accepts both pem text and base64`() {
        assertTrue("تشخیص PEM", "BEGIN .*PRIVATE KEY" in workflow)
        assertTrue("رمزگشایی base64", "base64 -d" in workflow)
        assertTrue("تشخیص در قطعه", "BEGIN .*PRIVATE KEY" in snippet)
    }

    @Test
    fun `ci verifies the key before rsync`() {
        assertTrue("بررسی سلامت کلید", "ssh-keygen -y -f ~/.ssh/arvan" in workflow)
        assertTrue("پیام راهنمای base64", "base64 -w0 ~/.ssh/arvan" in workflow)
        assertTrue("ریشهٔ وب پیش‌فرض", "/var/www/onlineexam" in workflow)
    }
}
