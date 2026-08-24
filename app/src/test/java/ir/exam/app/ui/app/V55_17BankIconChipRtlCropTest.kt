package ir.exam.app.ui.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import ir.exam.app.ui.builder.FigTokenVisuals
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * رگرسیون V55.17 — سه گزارش/درخواست کاربر پس از V55.16:
 * ۱) «دکمهٔ ذخیره در بانک حذف؛ آیکن کنار سطل زباله»: OutlinedButton متنی حذف و
 *    IconButton با Icons.Outlined.BookmarkAdd کنار آیکن حذف اضافه شد.
 * ۲) «کادر متن گزینه پر از کد می‌شود»: مقدار واقعی همان توکن %%FIG%% می‌ماند
 *    (منبع حقیقت) ولی VisualTransformation جدید (FigTokenVisuals) توکن را در
 *    نمایش به تراشهٔ کوتاه ⟦نوع⟧ تبدیل می‌کند؛ نگاشت offset اتمی (caret داخل
 *    توکن به مرز انتهای تراشه می‌چسبد) و یکنواختی آن با تست اجرایی JVM.
 * ۳) «حرکت آزاد مربع برش در جهت مخالف»: برنامه RTL است و Modifier.offset
 *    جهت‌آگاه است (x مثبت در RTL به چپ می‌رود) درحالی‌که هندسهٔ برش LTR
 *    است؛ کل بوم برش (BoxWithConstraints) در LTR اجباری پیچیده شد.
 */
class V55_17BankIconChipRtlCropTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val builder by lazy { source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt") }
    private val matching by lazy { source("app/src/main/java/ir/exam/app/ui/builder/QuestionOptionMedia.kt") }
    private val imageEditor by lazy { source("app/src/main/java/ir/exam/app/ui/image/InteractiveImageEditorDialog.kt") }

    @Test
    fun `bank save is an icon next to the trash icon`() {
        assertTrue("Icons.Outlined.BookmarkAdd" in builder)
        assertTrue("viewModel.saveToBank(question.id)" in builder)
        assertFalse("OutlinedButton(onClick = { viewModel.saveToBank(question.id) })" in builder)
        // آیکن بانک قبل از سطل زباله در همان ردیف بارم.
        assertTrue(builder.indexOf("Icons.Outlined.BookmarkAdd") < builder.indexOf("Icons.Outlined.Delete"))
    }

    @Test
    fun `fig tokens render as short chips in option fields`() {
        assertTrue("visualTransformation = FigTokenVisuals.transformation" in builder)
        val count = Regex("visualTransformation = FigTokenVisuals\\.transformation").findAll(matching).count()
        assertEquals(2, count)
    }

    @Test
    fun `chip transformation is monotonic and atomic over tokens`() {
        val src = "الف %%FIG:{\"k\":\"t\",\"X\":{}}%% وسط %%FIG:{\"k\":\"a\",\"X\":{}}%% پایان"
        val transformed = FigTokenVisuals.transformation(Color.Black).filter(AnnotatedString(src))
        val out = transformed.text.text
        assertTrue("⟦جدول⟧" in out)
        assertTrue("⟦آناتومی⟧" in out)
        assertFalse("%%FIG:" in out)
        // یکنواختی نگاشت در هر دو جهت + مرزها.
        var prev = -1
        for (i in 0..src.length) {
            val v = transformed.offsetMapping.originalToTransformed(i)
            assertTrue("o2t not monotonic at $i", v >= prev)
            prev = v
        }
        assertEquals(out.length, transformed.offsetMapping.originalToTransformed(src.length))
        prev = -1
        for (i in 0..out.length) {
            val v = transformed.offsetMapping.transformedToOriginal(i)
            assertTrue("t2o not monotonic at $i", v >= prev)
            prev = v
        }
        assertEquals(src.length, transformed.offsetMapping.transformedToOriginal(out.length))
    }

    @Test
    fun `crop canvas is forced left-to-right so drag follows the finger`() {
        assertTrue("CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr)" in imageEditor)
        // بوم برش داخل بلوک LTR است.
        val ltrBlock = imageEditor.substringAfter("LocalLayoutDirection provides LayoutDirection.Ltr")
        assertTrue("BoxWithConstraints(" in ltrBlock)
        assertTrue("CropFrame(" in ltrBlock)
    }
}
