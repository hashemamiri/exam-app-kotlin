package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * رگرسیون V55.15 — دو گزارش دستگاه پس از V55.14:
 * ۱) «مربع برش حرکت آزادانه ندارد»: باگ stale-lambda کلاسیک Compose —
 *    pointerInput(circular) هرگز restart نمی‌شود و closure قدیمی onMove
 *    مرکز کهنهٔ اولین composition (safeCenterX/Y) را نگه می‌داشت؛ هر drag از
 *    همان مرکز اولیه حساب می‌شد و کادر عملاً قفل بود (همین برای resize هم).
 *    رفع: rememberUpdatedState(onMove/onResize) داخل CropFrame/CropHandle.
 * ۲) «نمودار جعبه‌ای در متن سؤال به شکل مکعب است»: قرینهٔ باگ V55.14 —
 *    buildGraphSpec خروجی بدون k می‌ساخت و مرجعِ کادر متن، توکن بدون k را
 *    «هندسه» می‌گیرد؛ چون svgOf هندسهٔ مرجع پس از V55.14 نگاشت cuboid→box
 *    دارد، box جعبه‌ای به مکعب‌مستطیل می‌رسید. رفع: buildGraphSpec همیشه
 *    k='g' می‌گذارد (مسیر مرجع GraphFig). تأیید Chromium: توکن k='g',t=box →
 *    ۵ مستطیل نمودار؛ cuboid بدون k → polygon مکعب‌مستطیل.
 */
class V55_15CropMoveBoxChartTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val editor by lazy { source("app/src/main/java/ir/exam/app/ui/image/InteractiveImageEditorDialog.kt") }
    private val picker by lazy { source("app/src/main/java/ir/exam/app/ui/figure/FigurePickerDialog.kt") }

    @Test
    fun `crop frame callbacks always use the latest state`() {
        assertTrue("val currentOnMove by rememberUpdatedState(onMove)" in editor)
        assertTrue("currentOnMove(drag.x, drag.y)" in editor)
        assertTrue("val currentOnResize by rememberUpdatedState(onResize)" in editor)
        assertTrue("currentOnResize(edge, drag.x, drag.y)" in editor)
    }

    @Test
    fun `graph specs carry the reference chart module key`() {
        assertTrue("root[\"k\"] = JsonPrimitive(\"g\")" in picker)
    }
}
