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
  val indexedRoot=NativeMathParser.parse("\\sqrt[3]{x}") as MathNode.Radical
  assertTrue(indexedRoot.index!=null)
  val symbols=NativeMathParser.parse("\\iint \\rightleftharpoons \\subseteq") as MathNode.Sequence
  assertTrue(symbols.children.filterIsInstance<MathNode.Symbol>().any{it.value=="∬"})
  assertTrue(NativeMathParser.parse("\\left(x+1\\right)") is MathNode.Delimited)
  val lines=NativeMathParser.parse("x\\\\y") as MathNode.Sequence
  assertTrue(lines.children.any{it==MathNode.LineBreak})
 }

 @Test fun `keeps exact source ranges for touchable boxes and supplementary unicode`() {
  val tex="\\frac{12}{\\sqrt{x}}"
  val ranges=NativeMathParser.editableRanges(tex)
  val numberStart=tex.indexOf("12")
  val xStart=tex.indexOf('x')
  assertTrue(MathSourceRange(numberStart,numberStart+2) in ranges)
  assertTrue(MathSourceRange(xStart,xStart+1) in ranges)
  val supplementary="𝑥"
  val symbol=NativeMathParser.parse(supplementary) as MathNode.Symbol
  assertEquals(supplementary,symbol.value)
  assertEquals(0,symbol.sourceStart)
  assertEquals(supplementary.length,symbol.sourceEnd)
  val matrixTex="\\begin{bmatrix}a&b\\\\c&d\\end{bmatrix}"
  assertEquals(listOf("a","b","c","d"),NativeMathParser.editableRanges(matrixTex).map{matrixTex.substring(it.start,it.endExclusive)})
 }

 @Test fun `supports every extra display command in editable native ast`() {
  assertTrue(NativeMathParser.parse("\\sfrac{1}{2}") is MathNode.Fraction)
  assertTrue(NativeMathParser.parse("\\nicefrac{1}{2}") is MathNode.Fraction)
  assertTrue(NativeMathParser.parse("\\root{3}{x}") is MathNode.Radical)
  assertTrue(NativeMathParser.parse("\\underline{x}") is MathNode.Accent)
  assertTrue(NativeMathParser.parse("\\widehat{x}") is MathNode.Accent)
  assertTrue(NativeMathParser.unsupportedCommands("\\sfrac{1}{2} \\root{3}{x} \\underline{x} \\widehat{x} \\quad").isEmpty())
 }
}
