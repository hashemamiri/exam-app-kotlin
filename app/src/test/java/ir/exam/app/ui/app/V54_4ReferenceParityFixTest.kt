package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * رگرسیون V54.4 — سه گزارش دستگاه پس از V54.3:
 * ۱) «کادر متن سؤال وب‌ویو نیست»: قاب/برچسب مرجع بایت‌به‌بایت از HTML رندر
 *    می‌شود؛ Compose هیچ قاب/برچسب تکراری نمی‌کشد و CSS دستکاری قاب حذف شد.
 * ۲) «صفحهٔ سفید + پیام بارگیری نشد»: خطای subresource فرعی دیگر پیام کاذب
 *    نمی‌سازد (فقط main frame) و مسیرهای خارج از asset پاسخ خالی امن می‌گیرند.
 * ۳) «پنجرهٔ فرمول دقیقاً مثل مرجع»: پنجرهٔ تمام‌صفحه فقط WebView خالص است؛
 *    بدون X شناور Compose؛ پوستهٔ میزبان مخفی نمی‌شود.
 */
class V54_4ReferenceParityFixTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String): String = File(root(), path).readText()

    private val webSection by lazy { source("app/src/main/java/ir/exam/app/ui/builder/QuestionTextWebSection.kt") }
    private val host by lazy { source("app/src/main/java/ir/exam/app/ui/math/FormulaHostDialog.kt") }

    @Test
    fun `compose draws no duplicate frame or label around the webview`() {
        assertFalse("\"متن سؤال\"" in webSection)
        assertFalse("BorderStroke" in webSection)
        assertFalse("RoundedCornerShape" in webSection)
    }

    @Test
    fun `formula window is pure webview like the reference`() {
        // بدون X شناور و بدون آیکن‌های Compose روی پنجرهٔ فرمول.
        assertFalse("Icons.Outlined.Close" in host)
        assertFalse("IconButton" in host)
        assertTrue("usePlatformDefaultWidth = false" in host)
        // پس‌زمینه همان رنگ صفحهٔ مرجع است تا فریم سفید دیده نشود.
        assertTrue("0xFFE9EEF5" in host)
        // بستن با دکمه‌های خود ویرایشگر مرجع (overlay=false) یا Back سیستم.
        assertTrue("onEditorClosed" in host)
        assertTrue("onDismissRequest = { onResult(latestText); onDismiss() }" in host)
    }

}
