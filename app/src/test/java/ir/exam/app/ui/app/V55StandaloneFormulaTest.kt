package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * رگرسیون V55 — پنجرهٔ فرمول = فایل مستقل formula.html (انتخاب صریح کاربر):
 * ۱) asset جدید کامل با پل افزوده (کد مرجع دست‌نخورده؛ auto-open مرجع حفظ).
 * ۲) بستن (✕ یا درج) = برگشت به برنامه: بازگشایی خودکار مرجع فقط در برنامه
 *    خنثی می‌شود و onEditorClosed متن نهایی را برمی‌گرداند.
 * ۳) FormulaHostDialog فقط پوشهٔ formula_editor را سرو می‌کند.
 * صحت رفتاری با تست اجرایی jsdom تأیید شد: begin→باز، درج «1/2 + x^2» →
 * «$\\frac{1}{2} + x^{2}$» در متن + closed، بازگشایی جلسهٔ بعد سالم.
 */
class V55StandaloneFormulaTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val asset by lazy {
        File(root(), "app/src/main/assets/formula_editor/formula.html").readText()
    }
    private val host by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/math/FormulaHostDialog.kt").readText()
    }

    @Test
    fun `standalone asset keeps reference code and adds only the native bridge`() {
        assertTrue(asset.length > 500_000)
        // اجزای مرجع دست‌نخورده
        assertTrue("<script id=\"auto-open\">" in asset)
        assertTrue("function mfApply" in asset)
        assertTrue("id=\"qTxt_1\"" in asset)
        // پل افزوده
        assertTrue("exam-formula-native-bridge" in asset)
        assertTrue("window.ExamFormulaHost" in asset)
        assertTrue("onEditorClosed" in asset)
    }

    @Test
    fun `closing returns to the app instead of auto reopening`() {
        assertTrue("__aoNativeClosing" in asset)
        // ریست پرچم در شروع هر جلسه تا بازکردن دوباره کار کند.
        assertTrue("window.__aoNativeClosing = false" in asset)
        assertTrue("if (window.__aoNativeClosing) return;" in asset)
    }

    @Test
    fun `dialog serves only the formula asset folder and uses bridge events`() {
        assertTrue("formula-editor/formula.html" in host)
        assertTrue("assets.open(\"formula_editor/\$assetPath\")" in host)
        assertTrue("ExamFormulaHost.begin(" in host)
        assertTrue("onEditorClosed" in host)
        assertFalse("question-editor/question_editor.html?formulaHost=1" in host)
    }
}
