package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V56.1 — چیدمان تبلت صفحه‌های اصلی (پچ ۲ از ۳):
 * ۱) منوی اصلی: در تبلت ۳ستونه و پهنای 840dp؛ ردیف ناقص با Spacer(weight) پر
 *    می‌شود؛ در گوشی همان ۲ ستون و 560dp قبلی (قرارداد COLUMNS=2 دست‌نخورده).
 * ۲) پشتهٔ کارت‌های مدیریت: در تبلت سقف پهنای 620dp تا کارت‌ها کش نیایند.
 * ۳) سازندهٔ آزمون/داشبورد/بانک سؤال/بخش ظاهر: ستون محتوا در تبلت وسط صفحه
 *    با سقف پهنا؛ در گوشی بدون تغییر.
 */
class V56_1TabletScreensLayoutTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val menu by lazy { source("app/src/main/java/ir/exam/app/ui/app/Design69MainMenuScreen.kt") }
    private val cards by lazy { source("app/src/main/java/ir/exam/app/ui/app/TeacherManagementCardsScreen.kt") }
    private val builder by lazy { source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt") }
    private val dashboard by lazy { source("app/src/main/java/ir/exam/app/ui/dashboard/TeacherDashboardScreen.kt") }
    private val bank by lazy { source("app/src/main/java/ir/exam/app/ui/bank/QuestionBankScreen.kt") }
    private val settings by lazy { source("app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsScreen.kt") }

    @Test
    fun `main menu grows to three columns on tablets and keeps two on phones`() {
        assertTrue("const val TABLET_COLUMNS = 3" in menu)
        assertTrue("const val TABLET_MAX_WIDTH_DP = 840" in menu)
        assertTrue("fun columnsFor(tablet: Boolean): Int = if (tablet) TABLET_COLUMNS else COLUMNS" in menu)
        assertTrue("val columns = Design69MenuContract.columnsFor(tablet)" in menu)
        assertTrue("cards.chunked(columns)" in menu)
        // قرارداد گوشی دست‌نخورده
        assertTrue("const val COLUMNS = 2" in menu)
        // ردیف ناقص تبلت با weight خالی پر می‌شود تا کارت‌ها کش نیایند
        assertTrue("repeat(columns - rowCards.size)" in menu)
        assertTrue("Spacer(Modifier.weight(1f))" in menu)
    }

    @Test
    fun `management card stack is width capped on tablets`() {
        assertTrue("val tabletCards = LocalTabletLayout.current" in cards)
        assertTrue("widthIn(max = if (tabletCards) 620.dp else Dp.Unspecified)" in cards)
    }

    @Test
    fun `content columns are centered with a max width on tablets`() {
        assertTrue("widthIn(max = 760.dp)" in builder)
        assertTrue("wrapContentWidth(Alignment.CenterHorizontally)" in builder)
        assertTrue("widthIn(max = 760.dp)" in dashboard)
        assertTrue("widthIn(max = 760.dp)" in bank)
        assertTrue("widthIn(max = 680.dp)" in settings)
        // مسیر گوشی این صفحه‌ها شرطی است و بدون تبلت Modifier اضافه نمی‌شود
        assertTrue("if (tabletBuilder) Modifier" in builder)
        assertTrue("if (tabletDash) Modifier" in dashboard)
        assertTrue("if (tabletBank) Modifier" in bank)
        assertTrue("if (tabletAppearance) Modifier" in settings)
    }
}
