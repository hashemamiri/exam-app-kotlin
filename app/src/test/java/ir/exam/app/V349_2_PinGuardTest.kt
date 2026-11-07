package ir.exam.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V349.2 — نگهبانِ پین‌های کهنه.
 *
 * در V349 سه تست در CI شکستند چون پین‌های متنی‌شان با بازسازی سورس کهنه شده بود
 * (V348 مسیر ذخیره، V306 فهرست حالت‌ها، V218 قاعدهٔ سفیدِ چاپ). این آزمون
 * نگهبان محلیِ همان دسته را پاس می‌دارد و قاعدهٔ سفیدِ چاپ را برای بلوک کد پین می‌کند.
 */
class V349_2_PinGuardTest {

    private fun root(): File {
        var d = File(System.getProperty("user.dir") ?: ".")
        while (!File(d, "settings.gradle.kts").exists() && d.parentFile != null) d = d.parentFile!!
        return d
    }

    @Test
    fun localPinCheckerIsPresent() {
        val script = File(root(), "scripts/check_site_pins.py")
        assertTrue("نگهبان پین‌های متنی باید بماند", script.exists())
        val text = script.readText()
        assertTrue("باید پین‌های منفی را بشناسد", text.contains("assertFalse"))
        assertTrue("باید فایل‌های تست را از منبع جدا کند", text.contains("path.suffix == '.kt'"))
        assertTrue("باید قاعدهٔ سفیدِ چاپ را بسنجد", text.contains("background:#fff"))
    }

    /** کاغذ چاپ سفید است، ولی سفیدِ ثابت باید مثل قاب شکل #FFFFFF نوشته شود. */
    @Test
    fun codePrintBlockUsesTheAllowedWhiteForm() {
        val css = File(root(), "site/src/site.css").readText()
        assertTrue("بلوک کد چاپ باید سفیدِ مجاز داشته باشد", css.contains(".cx-block{background:#FFFFFF"))
        assertTrue(
            "شکل کوتاهِ #fff در هیچ‌جای استایل مجاز نیست (قاعدهٔ V218)",
            Regex("background:#fff\\b").findAll(css).count() == 0
        )
    }

    @Test
    fun updatedPinsMatchTheCurrentSource() {
        val student = File(root(), "site/src/student.js").readText()
        assertTrue(
            "پاسخ کد از مسیر compose ذخیره می‌شود",
            student.contains("save(compose(textPart, C.fence(r.code, r.lang)))")
        )
        val payload = File(root(), "app/src/main/java/ir/exam/app/data/repository/StudentExamPayloadCodec.kt").readText()
        assertTrue(
            "حالت code باید در فهرست حالت‌های پاسخ باشد",
            payload.contains("\"construct\", \"code\"")
        )
        assertTrue(
            "فیلدهای کد باید از JSON عمومی خوانده شوند",
            payload.contains("codeLang = obj.text(\"code_lang\")") &&
                payload.contains("codeWithText = obj.text(\"answer_form\") == \"both\"")
        )
    }
}
