package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * رگرسیون V55.16 — دو درخواست باقی‌ماندهٔ کاربر (پچ دوم تحویل دومرحله‌ای):
 * ۱) «کادر متن گزینه‌های چندگزینه‌ای و جورکردنی شبیه کادر متن سؤال شود»
 *    (انتخاب ask_user: کادر سریع Native + پیش‌نمایش زیر آن، نه WebView سنگین):
 *    کادرها گرد (RoundedCornerShape(14.dp)) شدند و پیش‌نمایش NativeMathText
 *    علاوه بر فرمول ($)، توکن‌های %%FIG%% (شکل/نمودار/جدول/...) را هم زنده
 *    رندر می‌کند.
 * ۲) «به‌جای آیکن فرمول روی کارت گزینه/جورکردنی دکمهٔ + که پنجرهٔ ۸ آیکن باز
 *    کند»: OptionInsertButton (+) جایگزین Icons.Outlined.Functions شد؛
 *    OptionInsertToolsDialog شبکهٔ ۸ ابزار (همان ۸ ابزار کادر متن سؤال) را
 *    نشان می‌دهد؛ فرمول → FormulaHostDialog همان فیلد؛ بقیه → ویرایشگر Native
 *    همان ابزار و خروجی با appendTokenToField به «همان فیلد» اضافه می‌شود
 *    (fieldInsertTarget)؛ انصراف در همهٔ مسیرها هدف را پاک می‌کند.
 */
class V55_16OptionInsertToolsTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val builder by lazy { source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt") }
    private val matching by lazy { source("app/src/main/java/ir/exam/app/ui/builder/QuestionOptionMedia.kt") }
    private val tools by lazy { source("app/src/main/java/ir/exam/app/ui/builder/OptionInsertTools.kt") }

    @Test
    fun `option and matching text boxes look like the question box with live preview`() {
        val multiple = builder.substringAfter("QuestionType.MULTIPLE_CHOICE ->")
            .substringBefore("QuestionType.TRUE_FALSE ->")
        assertTrue("shape = RoundedCornerShape(14.dp)" in multiple)
        assertTrue("'\$' in option || \"%%FIG:\" in option" in multiple)
        assertTrue("shape = RoundedCornerShape(14.dp)" in matching)
        assertTrue("'\$' in value || \"%%FIG:\" in value" in matching)
    }

    @Test
    fun `plus button replaces the formula icon and opens the eight tool dialog`() {
        assertTrue("fun OptionInsertButton(" in tools)
        assertTrue("Icons.Outlined.Add" in tools)
        assertTrue("fun OptionInsertToolsDialog(" in tools)
        // هر ۸ ابزار همان کادر متن سؤال.
        listOf("FORMULA", "FIGURE", "GRAPH", "TABLE", "ANATOMY", "PERIODIC", "PHYSICS", "CHEMISTRY")
            .forEach { assertTrue("missing tool: $it", it in tools) }
        // آیکن فرمول قدیمی از هر دو کارت رفته است.
        assertFalse("Icons.Outlined.Functions" in builder)
        assertFalse("Icons.Outlined.Functions" in matching)
        assertTrue("OptionInsertButton(optionLabel)" in builder)
        assertTrue("OptionInsertButton(label, onClick = onFormula)" in matching)
    }

    @Test
    fun `selected tool routes to its editor and output lands in the same field`() {
        assertTrue("insertMenuFor = InsertMenuRef(\"option\", index, optionLabel)" in builder)
        assertTrue("OptionInsertToolsDialog(" in builder)
        assertTrue("OptionInsertTool.FORMULA ->" in builder)
        assertTrue("fun appendTokenToField(ref: InsertMenuRef, spec: FigureSpec)" in builder)
        assertTrue("\"%%FIG:\${spec.toJson()}%%\"" in builder)
        // خروجی deliverFigure وقتی هدفِ فیلد فعال است به همان فیلد می‌رود.
        assertTrue("fieldRef != null -> {" in builder)
        // انصراف هدف را پاک می‌کند.
        assertTrue("fieldInsertTarget = null; figureTarget = null" in builder)
        assertTrue("fieldInsertTarget = null; atlasTarget = null" in builder)
    }
}
