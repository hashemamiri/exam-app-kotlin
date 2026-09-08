package ir.exam.app.core.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** V108 — تبدیل هوشمند گفتار فارسی/انگلیسی به عدد، نماد و فرمول. */
class SpeechMathConverterTest {
    @Test
    fun `persian and english spoken numbers become digits`() {
        assertEquals("25٪ دانش آموزان", SpeechMathConverter.convert("بیست و پنج درصد دانش آموزان").text)
        assertEquals("1234 نفر", SpeechMathConverter.convert("هزار و دویست و سی و چهار نفر").text)
        assertEquals("3.14", SpeechMathConverter.convert("سه ممیز چهارده").text)
        assertEquals("the answer is 1200", SpeechMathConverter.convert("the answer is one thousand two hundred").text)
        assertEquals("25% of students", SpeechMathConverter.convert("twenty five percent of students").text)
    }

    @Test
    fun `plain symbols are typed without a formula`() {
        val r = SpeechMathConverter.convert("نام پایتخت ایران چیست علامت سوال")
        assertEquals("نام پایتخت ایران چیست؟", r.text)
        assertFalse(r.containsFormula)
    }

    @Test
    fun `math phrases become latex formulas`() {
        assertEquals("\$x^{2} + 3 x = 0\$", SpeechMathConverter.convert("ایکس به توان دو به علاوه سه ایکس مساوی صفر").text)
        assertEquals("\$x^{2} + 3 x = 0\$", SpeechMathConverter.convert("x squared plus three x equals zero").text)
        assertEquals("\$\\sqrt{2}\$", SpeechMathConverter.convert("رادیکال دو").text)
        assertEquals("\$\\sqrt{2}\$", SpeechMathConverter.convert("square root of two").text)
        assertEquals("\$\\frac{1}{2}\$", SpeechMathConverter.convert("کسر یک روی دو").text)
        assertEquals("\$\\int_{0}^{1} x\$", SpeechMathConverter.convert("integral from zero to one x").text)
        assertEquals("\$\\sin x > 0\$", SpeechMathConverter.convert("سینوس ایکس بزرگتر از صفر").text)
        assertEquals("\$\\lim_{x \\to \\infty}\$", SpeechMathConverter.convert("حد ایکس به سمت بی نهایت").text)
        assertEquals("\$\\pi \\times r^{2}\$", SpeechMathConverter.convert("پی ضرب در آر به توان دو").text)
        assertTrue(SpeechMathConverter.convert("رادیکال دو").containsFormula)
    }

    @Test
    fun `common recognizer mistakes are corrected and best candidate is picked`() {
        assertEquals("\$x^{2} + 3 x = 0\$", SpeechMathConverter.convert("اکس بتوان دو بعلاوه سه اکس مساویه صفر").text)
        assertEquals("\$\\sqrt{2}\$", SpeechMathConverter.convert("رادیکاله دو").text)
        assertEquals("25٪", SpeechMathConverter.convert("بیست و پنج در صد").text)
        val best = SpeechMathConverter.pickBest(listOf("اکس به توان دم", "ایکس به توان دو", "عکس به توان دو"))
        assertEquals("ایکس به توان دو", best)
        assertEquals("", SpeechMathConverter.pickBest(emptyList()))
    }

    @Test
    fun `plain conversion never produces formulas`() {
        assertEquals("x به توان 2 به علاوه 3 x مساوی 0", SpeechMathConverter.convertPlain("ایکس به توان دو به علاوه سه ایکس مساوی صفر").replace("ایکس", "x"))
        assertEquals("25٪ دانش آموزان", SpeechMathConverter.convertPlain("بیست و پنج درصد دانش آموزان"))
        assertFalse(SpeechMathConverter.convertPlain("رادیکال دو").contains("$"))
    }
}
