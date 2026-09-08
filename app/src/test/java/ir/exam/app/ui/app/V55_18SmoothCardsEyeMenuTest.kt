package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * رگرسیون V55.18 — سه درخواست کاربر:
 * ۱) «اسکرول کارت‌های مانده/پاسخ/تصحیح به چپ نرم است اما به راست نه»:
 *    در کشیدن به راست (direction=-1) کارت فعالِ جدید همان کارت قبلی پشته است
 *    و چون translation فقط روی کارت فعال اعمال می‌شود، snap فوری صفر باعث
 *    «پرش» ورود آن می‌شد. اکنون کارت جدید از همان سمت خروج (targetX) وارد و
 *    نرم (tween 300 + FastOutSlowInEasing) به مرکز می‌آید؛ چپ مثل قبل.
 * ۲) «آیکن چشم هم پیش‌نمایش چاپ این سؤال و هم پیش‌نمایش کامل A4 را باز کند و
 *    بستن یکی دیگری را نیاورد»: این طراحیِ اولیه در V62.7 با یک منوی
 *    ساده‌ترِ تک‌گزینه‌ای جایگزین شد (چشم فقط پیش‌نمایش دانش‌آموزی را باز
 *    می‌کند). سیم‌کشیِ قدیمیِ onPreview/onPreviewAll → previewQuestion/
 *    previewAll → QuestionPrintPreviewDialog/ExamPrintPreviewDialog از
 *    V62.7 دیگر به هیچ دکمه‌ای وصل نبود (پارامترها می‌ماندند ولی صدا زده
 *    نمی‌شدند) — یعنی کدِ کاملاً مرده بود. در V120 این کدِ مرده و فایلِ
 *    ExamPrintPreview.kt به‌طور کامل حذف شدند تا کسی در آینده به‌اشتباه
 *    گمان نکند این پیش‌نمایشِ Compose-native با موتورِ واقعیِ چاپ
 *    (HTML/WebView در ExamHtmlPrintDialog) هم‌خوان یا فعال است.
 * ۳) «صحیح/غلط روی کارت به‌صورت ص/غ + فاصلهٔ کمتر آیکن‌ها»: برچسب فشردهٔ
 *    ص/غ روی سربرگ کارت؛ فاصلهٔ ردیف 6dp→2dp و آیکن‌ها 42dp→38dp
 *    (فقط سربرگ؛ MinimalScoreField طبق قرارداد V25 دست‌نخورده 62x40 ماند).
 */
class V55_18SmoothCardsEyeMenuTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val cards by lazy { source("app/src/main/java/ir/exam/app/ui/app/TeacherManagementCardsScreen.kt") }
    private val builder by lazy { source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt") }

    @Test
    fun `rightward card swipe enters smoothly from the exit side`() {
        assertTrue("if (direction == -1) {" in cards)
        assertTrue("dragX.snapTo(targetX)" in cards)
        assertTrue("dragX.animateTo(0f, tween(300, easing = FastOutSlowInEasing))" in cards)
        // مسیر چپ (direction=1) همان snap فوری قبلی را دارد (شاخهٔ else داخلی).
        val leftBranch = cards.substringAfter("if (direction == -1) {").substringAfter("} else {")
        assertTrue("dragX.snapTo(0f)" in leftBranch)
    }

    @Test
    fun `eye icon opens only the student preview and the old dead preview code is gone`() {
        val editor = builder.substringAfter("private fun QuestionEditor(")
        // V62.7 — منوی چشم حذف شد: چشم فقط پیش‌نمایش دانش‌آموزی را باز می‌کند.
        assertTrue("onStudentPreview" in editor)
        assertTrue("پیش‌نمایش دانش‌آموزی سؤال" in editor)
        // V88.4 — «چیدمان و ظاهر چاپ» از آزمونِ آنلاین برداشته شد؛ همان
        // کنترل‌ها اکنون در آزمون‌سازِ چاپی بومی‌اند.
        // سنجه روی *کد* است نه کامنت: دکمه و Composableِ آن باید رفته باشند.
        assertTrue("QuestionStyleControls(question" !in builder)
        assertTrue("styleExpanded" !in builder)
        assertFalse("VisibilityOff" in builder)
        // V120 — onPreview/onPreviewAll و پیش‌نمایشِ Compose-native مرده
        // (previewQuestion/previewAll/QuestionPrintPreviewDialog/
        // ExamPrintPreviewDialog) به‌طور کامل حذف شدند.
        assertFalse("onPreviewAll: () -> Unit" in builder)
        assertFalse("onPreviewAll = { previewAll = true }" in builder)
        assertFalse("previewQuestion" in builder)
        assertFalse("previewAll" in builder)
        assertFalse("QuestionPrintPreviewDialog" in builder)
        assertFalse("ExamPrintPreviewDialog" in builder)
        assertTrue(!File(root(), "app/src/main/java/ir/exam/app/ui/builder/ExamPrintPreview.kt").exists())
    }

    @Test
    fun `card header is compact with the short true-false label`() {
        val editor = builder.substringAfter("private fun QuestionEditor(")
            .substringBefore("private fun QuestionStyleControls(")
        assertTrue("\"ص/غ\"" in editor)
        assertTrue("Arrangement.spacedBy(6.dp)" in editor) // V107: فاصلهٔ آیکن‌ها ۶dp
        // V62.5 — آیکن‌های سربرگ ۳۸→۳۰dp تا برچسب «چندگزینه‌ای» کامل دیده شود.
        assertTrue(".size(30.dp)" in editor)
        // قرارداد V25: فیلد بارم دست‌نخورده.
        assertTrue("Modifier.width(62.dp).height(40.dp)" in builder)
    }
}
