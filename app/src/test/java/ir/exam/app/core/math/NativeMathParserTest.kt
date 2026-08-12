package ir.exam.app.core.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeMathParserTest {
 @Test fun `parses fraction radical scripts matrix and accents structurally`() {
  val fraction=NativeMathParser.parse("\\frac{x^2}{\\sqrt{y_1}}")
  assertTrue(fraction is MathNode.Fraction)
  val matrix=NativeMathParser.parse("\\begin{bmatrix}a&b\\\\c&d\\end{bmatrix}")
  assertTrue(matrix is MathNode.Matrix);assertEquals(2,(matrix as MathNode.Matrix).rows.size)
  assertTrue(NativeMathParser.parse("\\vec{F}") is MathNode.Accent)
 }
}
