package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * هات‌فیکس V55.18.1 — گزارش دستگاه پس از V55.18: «همچنان اسکرول کارت‌ها به
 * راست نرم نیست».
 *
 * ریشه (با شبیه‌سازی فریم‌به‌فریم اثبات شد): راه‌حل V55.18 دوفازی بود؛ فاز اول
 * کارت فعال ۲۸۰ میلی‌ثانیه به بیرون صفحه می‌رفت، سپس activeIndex عوض می‌شد.
 * در کشیدن به راست کارت قدیمی بعد از تغییر index هنوز مرئی است
 * (relative=1) اما translation فقط روی کارت فعال اعمال می‌شود؛ پس همان کارت
 * از بیرون صفحه به جایگاه پشته «تلپورت» می‌کرد و کل حرکت هم دوبرابر طول
 * می‌کشید (۲۸۰+۳۰۰ میلی‌ثانیه) — این همان ناهمواری گزارش‌شده است.
 *
 * راه‌حل: کشیدن به راست تک‌فاز شد. کارت فعلی با returnX/returnY از نقطهٔ
 * رهاشدن نرم به جایگاه پشته برمی‌گردد و «هم‌زمان» کارت قبلی از سمت راست
 * وارد می‌شود؛ graphicsLayer برای کارتِ در حال برگشت (returningIndex)
 * translation جداگانه اعمال می‌کند. کشیدن به چپ مثل قبل ماند.
 */
class V55_18_1SmoothRightReturnHotfixTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val cards by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/app/TeacherManagementCardsScreen.kt").readText()
    }

    @Test
    fun `rightward swipe returns the current card to the stack in one phase`() {
        // V316.0 — چرخ‌وفلک کارت‌ها (Ferris Wheel): تمام کارت‌ها سرپا روی مدار بیضوی می‌چرخند.
        assertTrue("private fun wrapFerrisDistance(d: Float, n: Int): Float" in cards)
        assertTrue("val orbitRxPx = with(density) { 112.dp.toPx() }" in cards)
        assertTrue("val orbitRyPx = with(density) { 44.dp.toPx() }" in cards)
        assertTrue("val dragSpanPx = with(density) { 190.dp.toPx() }" in cards)
        assertTrue("rotationZ = 0f" in cards)
    }

    @Test
    fun `no two-phase exit remains inside the rightward branch`() {
        // V316.0 — انیمیشن قدیمیِ پشته/پرواز کامل حذف شد.
        assertFalse("if (direction == -1) {" in cards)
        assertFalse("returningIndex" in cards)
        assertFalse("stackTopPx" in cards)
        assertFalse("stackRotation" in cards)
        assertFalse("leavingStaysVisible" in cards)
    }

    @Test
    fun `graphics layer animates the returning card instead of teleporting it`() {
        assertTrue("val sc = 0.78f + 0.22f * cs" in cards)
        assertTrue("alpha = 0.35f + 0.65f * ((cs + 1f) / 2f)" in cards)
        assertTrue("translationX = -sn * orbitRxPx" in cards)
        assertTrue("translationY = (cs - 1f) * orbitRyPx" in cards)
        assertTrue(".zIndex(100f + cs * 100f)" in cards)
    }
}
