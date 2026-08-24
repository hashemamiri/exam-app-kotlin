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
 *    بستن یکی دیگری را نیاورد»: چشم اکنون DropdownMenu سه‌گزینه‌ای دارد
 *    (پیش‌نمایش این سؤال / پیش‌نمایش کامل A4 / چیدمان و ظاهر چاپ)؛ دو
 *    پیش‌نمایش state مستقل دارند (previewQuestion و previewAll) و بستن هرکدام
 *    فقط همان را می‌بندد.
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
    fun `eye icon opens both previews independently via a menu`() {
        val editor = builder.substringAfter("private fun QuestionEditor(")
            .substringBefore("private fun QuestionStyleControls(")
        assertTrue("previewMenuOpen" in editor)
        assertTrue("DropdownMenuItem(" in editor)
        assertTrue("پیش‌نمایش چاپ این سؤال" in editor)
        assertTrue("پیش‌نمایش کامل A4" in editor)
        assertTrue("onPreviewAll: () -> Unit" in builder)
        assertTrue("onPreviewAll = { previewAll = true }" in builder)
        // دو پیش‌نمایش state مستقل دارند؛ بستن یکی دیگری را باز نمی‌کند.
        assertTrue("onDismiss = { previewQuestion = null }" in builder)
        assertTrue("onDismiss = { previewAll = false }" in builder)
        assertFalse("VisibilityOff" in builder)
    }

    @Test
    fun `card header is compact with the short true-false label`() {
        val editor = builder.substringAfter("private fun QuestionEditor(")
            .substringBefore("private fun QuestionStyleControls(")
        assertTrue("\"ص/غ\"" in editor)
        assertTrue("Arrangement.spacedBy(2.dp)" in editor)
        assertTrue(".size(38.dp)" in editor)
        // قرارداد V25: فیلد بارم دست‌نخورده.
        assertTrue("Modifier.width(62.dp).height(40.dp)" in builder)
    }
}
