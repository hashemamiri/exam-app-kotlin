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
    fun `formula editor standalone asset is self contained with V34 and host bridge`() {
        // از V45.8، فایل asset نسخهٔ تک‌فایلی «formula-editor-window» است که
        // خودش شامل core ویرایشگر، installLibV34 و لایهٔ میزبان می‌باشد.
        // دیگر نباید فایل جدا برای install_lib_v34 یا helper FormulaV34Library
        // وجود داشته باشد؛ همه‌چیز در math_editor_standalone.html است.
        val root = root()
        val asset = File(root, "app/src/main/assets/math_editor_standalone.html").readText()
        val dialog = File(root, "app/src/main/java/ir/exam/app/ui/math/MathEditorWebViewDialog.kt").readText()

        // ۱) asset شامل V34 و لایهٔ میزبان است
        assertTrue("installLibV34 missing from asset", "function installLibV34(w)" in asset)
        listOf(
            "school", "type", "bio",
            "v34-math10", "v34-hesaban1", "v34-discrete",
            "v34-accents", "v34-arrows", "v34-special-let",
            "v34-bio", "v34-uni", "v34-stats", "v34-prob"
        ).forEach { token ->
            assertTrue("V34 token missing from self-contained asset: $token", token in asset)
        }
        assertTrue("host-bridge script missing", "host-bridge" in asset)
        // اسکریپت auto-open که در نسخهٔ وب پنجره را دوباره باز می‌کند نباید
        // در بیلد اندروید باشد (ما باز/بسته شدن را کنترل می‌کنیم).
        assertFalse("auto-open script must be stripped from Android asset",
                    "id=\"auto-open\"" in asset)
        // Cloudflare challenge-platform که هنگام دانلود به انتهای HTML
        // چسبیده بود نباید در asset بماند.
        assertFalse("Cloudflare challenge script must be stripped", "cdn-cgi/challenge-platform" in asset)

        // ۲) فایل‌های قدیمی حذف شده باشند
        assertFalse("legacy V34 asset file must be removed",
                    File(root, "app/src/main/assets/formula/install_lib_v34.js").exists())
        assertFalse("FormulaV34Library.kt must be removed",
                    File(root, "app/src/main/java/ir/exam/app/ui/math/FormulaV34Library.kt").exists())
        assertFalse("dialog must not reference removed FormulaV34Library",
                    "FormulaV34Library" in dialog)

        // ۳) پل اندروید: seed متن، wrap mfApply/closeMath، فراخوانی openMath
        assertTrue("qTxt_1 seed missing", "qTxt_1" in dialog)
        val openIdx = dialog.indexOf("window.openMath('qTxt_1')")
        assertTrue("window.openMath('qTxt_1') missing", openIdx >= 0)
        assertTrue("AndroidMathBridge.onApplyResult missing", "onApplyResult" in dialog)
        assertTrue("AndroidMathBridge.onClosed missing", "onClosed" in dialog)

        // ۴) fallback برای WebViewهای قدیمی که 100dvh نمی‌فهمند
        assertTrue("VIEWPORT_FALLBACK_JS missing", "VIEWPORT_FALLBACK_JS" in dialog)
        assertTrue("100vh fallback missing", "100vh" in dialog || "height:100%" in dialog)
        assertTrue("100dvh target missing", "100dvh" in dialog)
        assertTrue("modal pinning fallback missing", "#mfModal{" in dialog && "top:0" in dialog)
        assertTrue("demo-wrap hide rule missing", "body.math-open .demo-wrap" in dialog)
        // V45.8.10: فقط متن خود raw-string مربوط به fallback را بررسی کن.
        // جست‌وجوی قبلی از اولین واژهٔ mfP_box در کل فایل شروع می‌شد؛ پس از
        // اضافه‌شدن همین واژه به KDoc نسخهٔ V45.8.9، display:flex قانونیِ
        // #mfModal را اشتباهاً به #mfP_box نسبت می‌داد و CI false positive داشت.
        val fallbackMarker = "private const val VIEWPORT_FALLBACK_JS = \"\"\""
        val fallbackStart = dialog.indexOf(fallbackMarker)
        assertTrue("VIEWPORT_FALLBACK_JS raw string missing", fallbackStart >= 0)
        val viewportFallback = dialog
            .substring(fallbackStart + fallbackMarker.length)
            .substringBefore("\"\"\"")
        assertFalse("viewport fallback must not target mfP_box", "#mfP_box" in viewportFallback)

        // V45.8.5: چیپ‌های V34 باید مستقیم بایند شوند و پاپ‌آپ mbVar مرکزصفحه
        assertTrue("V34 chip binding missing", "mb-chip[data-v34" in dialog && "mbGroupLibrary" in dialog)
        assertTrue("mbVar centering missing", "translate(-50%,-50%)" in dialog && "mbVarOpen" in dialog)

        // ۵) بستن دیالوگ باید حتماً و با timeout ایمن به onDismiss ختم شود
        assertTrue("dismiss safety timeout missing",
                   "DISMISS_FALLBACK_MS" in dialog && "dismissOnce" in dialog)

        // ۶) تشخیص: لاگ JS به logcat
        assertTrue("diagnostic bridge missing", "AndroidMathBridge.log" in dialog)
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
