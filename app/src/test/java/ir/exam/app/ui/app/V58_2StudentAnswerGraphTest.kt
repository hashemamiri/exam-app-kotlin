package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V58.2 (پچ ۳ از ۳) — نمودار پاسخ دانش‌آموز با اجازهٔ معلم:
 * ۱) چیپ «نمودار پاسخ دانش‌آموز» در کارت سؤال معلم (allowAnswerGraph) —
 *    در JSON سؤال با کلید allowAnswerGraph ذخیره و به payload دانش‌آموز
 *    (QuestionPresentation.allowAnswerGraph) می‌رسد.
 * ۲) دانش‌آموز: «رسم نمودار پاسخ» → همان جریان دومرحله‌ای معلم
 *    (FigureTypePickerDialog با kind=GRAPH سپس FigurePickerDialog برای
 *    پارامترها مثل سهمی y=ax²+bx+c)؛ توکن %%FIG:...%% داخل همان TextAnswer
 *    ذخیره می‌شود — بدون تغییر قرارداد سرور؛ معلم در تصحیح همان را می‌بیند.
 * ۳) ویرایش/حذف نمودار پاسخ بدون دست‌زدن به متن آزاد پاسخ.
 */
class V58_2StudentAnswerGraphTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val student by lazy { source("app/src/main/java/ir/exam/app/ui/student/StudentExamScreen.kt") }
    private val builderScreen by lazy { source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt") }
    private val builderVm by lazy { source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderViewModel.kt") }
    private val draft by lazy { source("app/src/main/java/ir/exam/app/ui/builder/QuestionDraft.kt") }
    private val examCodec by lazy { source("app/src/main/java/ir/exam/app/data/repository/ExamQuestionCodec.kt") }
    private val payloadCodec by lazy { source("app/src/main/java/ir/exam/app/data/repository/StudentExamPayloadCodec.kt") }
    private val models by lazy { source("app/src/main/java/ir/exam/app/domain/model/ExamModels.kt") }

    @Test
    fun `teacher can allow the answer graph per question`() {
        assertTrue("val allowAnswerGraph: Boolean = false" in draft)
        assertTrue("نمودار پاسخ دانش‌آموز" in builderScreen)
        assertTrue("fun setAllowAnswerGraph(questionId: String, allowed: Boolean)" in builderVm)
        // ذخیره و خواندن در JSON سؤال
        assertTrue("values[\"allowAnswerGraph\"] = JsonPrimitive(question.allowAnswerGraph)" in examCodec)
        assertTrue("allowAnswerGraph = obj[\"allowAnswerGraph\"]?.asBoolean() ?: false" in examCodec)
        // به دانش‌آموز می‌رسد
        assertTrue("val allowAnswerGraph: Boolean = false" in models)
        assertTrue("allowAnswerGraph = obj.boolean(\"allowAnswerGraph\")" in payloadCodec)
    }

    @Test
    fun `student draws and edits the graph through the native flow`() {
        assertTrue("if (presentation.allowAnswerGraph)" in student)
        assertTrue("fun StudentAnswerGraph(" in student)
        assertTrue("Text(\"رسم نمودار پاسخ\")" in student)
        assertTrue("FigureTypePickerDialog(" in student)
        assertTrue("kind = ir.exam.app.ui.figure.FigureKind.GRAPH" in student)
        assertTrue("FigurePickerDialog(" in student)
        assertTrue("Text(\"ویرایش نمودار\")" in student)
        assertTrue("Text(\"حذف نمودار\")" in student)
    }

    @Test
    fun `graph token lives inside the same text answer`() {
        // درج: جایگزینی توکن قبلی یا افزودن به انتهای پاسخ
        assertTrue("answerText.replaceRange(occ.start, occ.endExclusive, token)" in student)
        assertTrue("else if (answerText.isBlank()) token" in student)
        // حذف: فقط بازهٔ توکن حذف می‌شود
        assertTrue("answerText.removeRange(occ.start, occ.endExclusive)" in student)
        // نمایش زندهٔ نمودار پاسخ با زوم
        assertTrue("NativeMathText(answerText, zoomableFigures = true)" in student)
    }
}
