package ir.exam.app.ui.app

import ir.exam.app.ui.classes.PersianUsernameSuggester
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V19InteractionTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt").isFile
    }

    @Test
    fun `persian names produce editable latin underscore suggestions`() {
        assertEquals("ali_ahmadi", PersianUsernameSuggester.suggest("علی", "احمدی"))
        assertEquals("reza_rezaei", PersianUsernameSuggester.suggest("رضا", "رضایی"))
        assertEquals("ali_ahmadi_02", PersianUsernameSuggester.suggest("علی", "احمدی", 2))
        assertTrue(PersianUsernameSuggester.suggest("مهدی", "کاظمی").matches(Regex("[a-z0-9_]{4,20}")))
    }

    @Test
    fun `builder has synchronized radial eight actions accordion cards and floating save`() {
        val root = root()
        val builder = File(root, "app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt").readText()
        val radial = File(root, "app/src/main/java/ir/exam/app/ui/builder/BuilderRadialMenuOverlay.kt").readText()
        val viewModel = File(root, "app/src/main/java/ir/exam/app/ui/builder/ExamBuilderViewModel.kt").readText()

        listOf("تشریحی", "چندگزینه‌ای", "صحیح/غلط", "جای خالی", "عددی", "جورکردنی", "وارد کردن", "بانک سؤال").forEach {
            assertTrue("missing radial action $it", it in radial)
        }
        assertTrue("dottedAlpha" in radial)
        assertTrue("progress.animateTo(1f, tween(620" in radial)
        assertTrue("expandedQuestionId" in builder)
        assertTrue("settingsExpanded" in builder)
        assertTrue("bottom = 112.dp" in builder)
        assertTrue("FabPosition.Center" in builder)
        assertTrue("Icons.Outlined.Check" in builder)
        assertTrue("fun addQuestion(type: QuestionType): String" in viewModel)
        assertTrue("fun applyImport" in viewModel)
    }

    @Test
    fun `student dialogs are compact structured and pull refresh replaces buttons`() {
        val root = root()
        val school = File(root, "app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt").readText()
        val bank = File(root, "app/src/main/java/ir/exam/app/ui/bank/QuestionBankScreen.kt").readText()
        val calendar = File(root, "app/src/main/java/ir/exam/app/ui/calendar/CalendarScreen.kt").readText()
        val wallet = File(root, "app/src/main/java/ir/exam/app/ui/billing/WalletScreen.kt").readText()

        assertTrue("contentAlignment = Alignment.TopCenter" in school)
        assertTrue("PersianUsernameSuggester.suggest" in school)
        assertTrue("BulkStudentDraft" in school)
        assertTrue("🎲" in school)
        listOf(school, bank, calendar, wallet).forEach {
            assertTrue("PullToRefreshBox" in it)
            assertFalse("manual refresh text returned", "تازه‌سازی" in it)
        }
        assertFalse("cross-tab class chip returned", "همه دانش‌آموزان" in school)
    }

    @Test
    fun `formula editor is ltr with active box auto scroll`() {
        val root = root()
        val formula = File(root, "app/src/main/java/ir/exam/app/ui/math/NativeFormulaView.kt").readText()
        assertTrue("LocalLayoutDirection provides LayoutDirection.Ltr" in formula)
        assertTrue("horizontal.animateScrollTo(targetX)" in formula)
        assertTrue("vertical.animateScrollTo(targetY)" in formula)
    }

    @Test
    fun `formula editor dialog hosts the standalone web editor untouched`() {
        val root = root()
        val dialog = File(root, "app/src/main/java/ir/exam/app/ui/math/MathEditorWebViewDialog.kt").readText()
        val asset = File(root, "app/src/main/assets/math_editor_standalone.html").readText()
        assertTrue("math_editor_standalone.html" in dialog)
        assertTrue("AndroidMathBridge" in dialog)
        assertTrue("javaScriptEnabled" in dialog)
        assertTrue("openMath('qTxt_1')" in dialog)
        assertTrue("function openMath(targetId)" in asset)
        assertTrue("function mfApply()" in asset)
    }

    @Test
    fun `formula editor bootstraps the V34 curricular library school type bio`() {
        // در نسخهٔ وب (66.html) تابع میزبان installLibV34 پس از بارگذاری iframe
        // سه گروه «کتاب درسی ریاضی»، «نماد و تزئین»، «زیست و دانشگاه» و حدود
        // ۶۰ دستهٔ درسی تکمیلی را به MB_PAD/MB_GROUPS اضافه می‌کرد. در حالت
        // standalone باید همان بدنه از asset خوانده و قبل از openMath eval شود.
        val root = root()
        val dialog = File(root, "app/src/main/java/ir/exam/app/ui/math/MathEditorWebViewDialog.kt").readText()
        val v34 = File(root, "app/src/main/assets/formula/install_lib_v34.js").readText()
        val loader = File(root, "app/src/main/java/ir/exam/app/ui/math/FormulaV34Library.kt").readText()

        // 1) asset باید بایت‌به‌بایت بدنهٔ installLibV34 باشد
        assertTrue("installLibV34 function missing from V34 asset",
            v34.startsWith("function installLibV34(w)"))
        listOf(
            "school", "type", "bio",
            "v34-math10", "v34-hesaban1", "v34-discrete",
            "v34-accents", "v34-arrows", "v34-special-let",
            "v34-bio", "v34-uni", "v34-stats", "v34-prob"
        ).forEach { token ->
            assertTrue("V34 asset missing token: $token", token in v34)
        }

        // 2) asset باید توسط Android Context در سطح Composable خوانده شود
        assertTrue("FormulaV34Library.load helper missing", "fun load(context: Context)" in loader)
        assertTrue("V34 asset path missing", "formula/install_lib_v34.js" in loader)
        assertTrue("dialog must call FormulaV34Library.load", "FormulaV34Library.load" in dialog)
        assertTrue("dialog must use LocalContext", "LocalContext.current" in dialog)

        // 3) تزریق باید قبل از openMath اجرا شود
        val installIdx = dialog.indexOf("installLibV34(window)")
        val openIdx = dialog.indexOf("window.openMath('qTxt_1')")
        assertTrue("installLibV34(window) call missing", installIdx >= 0)
        assertTrue("window.openMath('qTxt_1') missing", openIdx >= 0)
        assertTrue("V34 must install before openMath", installIdx < openIdx)

        // 4) گارد idempotent بودن حفظ شده باشد (جلوگیری از تزریق دوباره)
        assertTrue("__libV34 idempotency guard missing", "__libV34" in v34)
        assertTrue("__mbV34Installed one-shot guard missing", "__mbV34Installed" in dialog)

        // 5) fallback برای WebViewهای قدیمی که 100dvh نمی‌فهمند
        assertTrue("VIEWPORT_FALLBACK_JS missing", "VIEWPORT_FALLBACK_JS" in dialog)
        assertTrue("100vh fallback missing", "100vh" in dialog)
        assertTrue("100dvh target missing", "100dvh" in dialog)
        // والد مدال نیز با top/right/bottom/left صفر پین شود (inset:0 در
        // WebViewهای خیلی قدیمی ممکن است پشتیبانی نشود).
        assertTrue("modal inset fallback missing", "#mfModal{top:0" in dialog)
        // دموی پشت‌صحنه هنگام باز بودن مدال پنهان شود
        assertTrue("demo-wrap hide rule missing", "body.math-open .demo-wrap" in dialog)
        // بستن دیالوگ باید حتماً و با timeout ایمن به onDismiss ختم شود
        assertTrue("dismiss safety timeout missing",
                   "DISMISS_FALLBACK_MS" in dialog && "dismissOnce" in dialog)
        // V45.7.5: اگر openMath در WebView قدیمی کامل اجرا نشد، خودمان
        // کلاس‌های مدال را force کنیم و خطاها را به logcat بفرستیم
        assertTrue("diagnostic bridge missing", "AndroidMathBridge.log" in dialog)
        assertTrue("force modal classes missing", "classList.add('modal', 'open', 'box-fullscreen')" in dialog)
        assertTrue("WebChromeClient console forward missing", "onConsoleMessage" in dialog)
    }

    @Test
    fun `sandbox credit remains server gated and never direct from apk`() {
        val root = root()
        val edge = File(root, "supabase/functions/wallet-payment/index.ts").readText()
        val billing = File(root, "app/src/main/java/ir/exam/app/ui/billing/BillingViewModel.kt").readText()
        val main = File(root, "app/src/main/java").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .joinToString("\n") { it.readText() }

        assertTrue("provider === 'sandbox'" in edge)
        assertTrue("sandboxAllowed()" in edge)
        assertTrue("native_credit_wallet_payment" in edge)
        assertTrue("credited: true" in edge)
        assertTrue("payment.credited && payment.sandbox" in billing)
        assertFalse("APK must not call server credit RPC", "native_credit_wallet_payment" in main)
    }
}
