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
        // state برگشت کارت قدیمی به پشته
        assertTrue("var returningIndex by remember { mutableIntStateOf(-1) }" in cards)
        assertTrue("returnX.snapTo(x)" in cards)
        assertTrue("returnY.snapTo(y)" in cards)
        assertTrue("returnX.animateTo(0f, tween(300, easing = FastOutSlowInEasing))" in cards)
        // پس از پایان انیمیشن‌ها آزاد می‌شود
        assertTrue("returningIndex = -1" in cards)
    }

    @Test
    fun `no two-phase exit remains inside the rightward branch`() {
        val rightBranch = cards.substringAfter("if (direction == -1) {").substringBefore("} else {")
        // در شاخهٔ راست هیچ خروج جداگانه‌ای قبل از ورود کارت جدید نیست
        assertFalse("animateTo(targetX" in rightBranch)
        assertTrue("returningIndex = activeIndex" in rightBranch)
        assertTrue("dragX.snapTo(targetX)" in rightBranch)
        // کشیدن به چپ همان خروج انیمیت‌شدهٔ قبلی را دارد
        val leftBranch = cards.substringAfter("if (direction == -1) {").substringAfter("} else {")
        assertTrue("dragX.animateTo(targetX, tween(280))" in leftBranch)
        assertTrue("dragX.snapTo(0f)" in leftBranch)
    }

    @Test
    fun `graphics layer animates the returning card instead of teleporting it`() {
        assertTrue("val returning = index == returningIndex && !active" in cards)
        assertTrue("returning -> returnX.value" in cards)
        assertTrue("returning -> returnY.value" in cards)
    }
}
