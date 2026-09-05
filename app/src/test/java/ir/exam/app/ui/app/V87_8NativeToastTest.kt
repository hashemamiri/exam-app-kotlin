package ir.exam.app.ui.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V87.8 — `alert()` پنجرهٔ خامِ مرورگر را با نشانیِ exam-print.local نشان
 * می‌داد؛ پیام‌ها به اعلانِ بومیِ محوشونده منتقل شدند. همچنین رفعِ ابهامِ
 * scope در `AnimatedVisibility` که کامپایل را شکست.
 */
class V87_8NativeToastTest {

    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val asset by lazy {
        File(root(), "app/src/main/assets/print/exam_print.html").readText()
    }
    private val dialog by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintDialog.kt").readText()
    }

    @Test
    fun `no informational alert is left in the page`() {
        listOf(
            "آزمون ذخیره‌شده بازیابی شد.",
            "خطا در بازیابی آزمون.",
            "آزمون با موفقیت بارگذاری شد.",
            "خطا در خواندن فایل.",
            "ابتدا متن یک سؤال را باز کنید."
        ).forEach {
            assertTrue("این پیام هنوز alert است: $it", "alert('$it')" !in asset)
            assertTrue("این پیام به toast نرفت: $it", "qmfToast('$it')" in asset)
        }
    }

    @Test
    fun `the toast helper falls back to alert outside the app`() {
        assertTrue("window.qmfToast = function" in asset)
        assertTrue("window.ExamPrintNative.toast(text)" in asset)
        // بیرونِ اپ چیزی از دست نمی‌رود
        assertTrue("try { alert(text); } catch (e2) {}" in asset)
    }

    @Test
    fun `the native side exposes the bridge and shows it as a fading message`() {
        assertTrue("fun toast(message: String?)" in dialog)
        assertTrue("onToast = { message ->" in dialog)
        assertTrue("barStatus = message" in dialog)
    }

    @Test
    fun `AnimatedVisibility no longer picks the wrong overload`() {
        // خطای CI: 'fun ColumnScope.AnimatedVisibility' … چون Modifier.align داده شده بود
        val at = dialog.indexOf("androidx.compose.animation.AnimatedVisibility(")
        assertTrue("AnimatedVisibility پیدا نشد", at > 0)
        val head = dialog.substring(at, at + 300)
        assertTrue("modifier = Modifier.align نباید روی خودش باشد", "Modifier.align" !in head)
        // در عوض یک Box آن را وسط می‌گذارد
        assertTrue("contentAlignment = Alignment.Center" in dialog)
    }

    @Test
    fun `the bridge constructor and its call site agree`() {
        val params = Regex("private val (\\w+)\\s*:")
            .findAll(dialog.substringAfter("private class ExamPrintBridge(").substringBefore("\n) {"))
            .map { it.groupValues[1] }.toList()
        assertEquals(7, params.size)
        val call = dialog.substringAfter("ExamPrintBridge(").substringBefore("\"ExamPrintNative\"")
        params.forEach { assertTrue("آرگومانِ $it داده نشده", "$it = " in call) }
    }
}
