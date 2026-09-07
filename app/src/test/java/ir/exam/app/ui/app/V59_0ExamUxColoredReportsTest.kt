package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V59.0 (پچ ۱ از ۲) — سه درخواست کاربر:
 * ۱) دکمهٔ «پیش‌نمایش کامل A4» زیر کارت‌های سؤال حذف شد (از منوی چشم کارت
 *    سؤال باز می‌شود — V55.18 دست‌نخورده).
 * ۲) شمارهٔ سؤال جاری همیشه خودکار اسکرول و نمایان می‌شود (LazyRow +
 *    animateScrollToItem).
 * ۳) گزارش‌ها: اول کارت‌های رنگی دانش‌آموزان (سبز/زرد/نارنجی/قرمز بر اساس
 *    تعداد رویداد مشکوک)؛ لمس کارت → گزارش کامل همان دانش‌آموز.
 */
class V59_0ExamUxColoredReportsTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val builder by lazy { source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt") }
    private val student by lazy { source("app/src/main/java/ir/exam/app/ui/student/StudentExamScreen.kt") }
    private val grading by lazy { source("app/src/main/java/ir/exam/app/ui/grading/GradingScreen.kt") }

    @Test
    fun `the standalone full a4 preview button is gone but the eye menu remains`() {
        // دکمهٔ تمام‌عرض زیر کارت‌ها حذف شد
        assertFalse("OutlinedButton(onClick = { previewAll = true }, modifier = Modifier.fillMaxWidth())" in builder)
        // مسیر منوی چشم سالم است (قرارداد V55.18)
        assertTrue("onPreviewAll = { previewAll = true }" in builder)
        assertTrue("پیش‌نمایش کامل A4" in builder)
    }

    @Test
    fun `the current question chip auto scrolls into view`() {
        assertTrue("val stripState = rememberLazyListState()" in student)
        assertTrue("LaunchedEffect(state.questionIndex)" in student)
        assertTrue("stripState.animateScrollToItem(state.questionIndex.coerceAtLeast(0))" in student)
        assertTrue("LazyRow(" in student)
        // چیپ دست‌ساز با long-press همچنان برقرار (V58.0.2)
        val chip = student.substringAfter("private fun StripChipCell(")
            .substringBefore("private fun StudentAnswerGraph(")
        assertTrue("combinedClickable(" in chip)
        assertTrue("onLongClick = { onToggleFlag(q.id) }" in chip)
        assertFalse("FilterChip(" in chip)
    }

    @Test
    fun `monitor reports open as colored student cards first`() {
        assertTrue("fun monitorViolationScore(report: JsonObject?): Int" in grading)
        assertTrue("fun monitorSeverityColor(score: Int): Color" in grading)
        // چهار رنگ درخواست‌شده: سبز، زرد، نارنجی، قرمز
        assertTrue("Color(0xFF2E7D32)" in grading)
        assertTrue("Color(0xFFF9A825)" in grading)
        assertTrue("Color(0xFFEF6C00)" in grading)
        assertTrue("Color(0xFFC62828)" in grading)
        // لمس کارت → گزارش کامل؛ بازگشت به لیست
        assertTrue("clickable { selected = index }" in grading)
        assertTrue("بازگشت به لیست" in grading)
        assertTrue("بدون تخلف" in grading)
        assertTrue("رویداد مشکوک" in grading)
    }

    @Test
    fun `violation scoring sums all suspicious events`() {
        // شمارش = مجموع همهٔ شمارنده‌های events (تلاش اسکرین‌شات، خروج و...)
        assertTrue("events.values.sumOf" in grading)
        // آستانه‌های رنگ: ۰ سبز، ۱-۲ زرد، ۳-۵ نارنجی، بیشتر قرمز
        assertTrue("score == 0 -> Color(0xFF2E7D32)" in grading)
        assertTrue("score <= 2 -> Color(0xFFF9A825)" in grading)
        assertTrue("score <= 5 -> Color(0xFFEF6C00)" in grading)
    }
}
