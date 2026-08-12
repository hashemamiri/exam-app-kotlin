package ir.exam.app.core.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeNaturalMathConverterTest {
    @Test fun `reference quick examples and nested powers convert structurally`() {
        assertEquals("\\frac{7}{8}", NativeNaturalMathConverter.toTex("7/8"))
        assertEquals("\\frac{a+b}{2}", NativeNaturalMathConverter.toTex("(a+b)/2"))
        assertEquals("\\sqrt{5}", NativeNaturalMathConverter.toTex("رادیکال ۵"))
        assertEquals("x^{2^{3}}", NativeNaturalMathConverter.toTex("x^2^3"))
        val relations = NativeNaturalMathConverter.toTex("pi >= 3 != 4 * 2 -> x")
        assertTrue("\\pi" in relations)
        assertTrue("\\ge" in relations)
        assertTrue("\\ne" in relations)
        assertTrue("\\times" in relations)
        assertTrue("\\to" in relations)
    }

    @Test fun `chemistry normalization fixes subscripts charges and equilibrium arrow`() {
        assertEquals("H_{2}O", NativeNaturalMathConverter.toTex("H2O", chemistry = true))
        assertEquals("Fe^{3+}", NativeNaturalMathConverter.toTex("Fe3+", chemistry = true))
        val sulfate = NativeNaturalMathConverter.toTex("SO4 2-", chemistry = true)
        assertTrue("SO_{4}" in sulfate)
        assertTrue("^{2-}" in sulfate)
        val equilibrium = NativeNaturalMathConverter.toTex("H2 + O2 ⇌ H2O", chemistry = true)
        assertTrue("\\rightleftharpoons" in equilibrium)
        assertFalse("\\text{rightleftharpoons}" in equilibrium)
    }
}
