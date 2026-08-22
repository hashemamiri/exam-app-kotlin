package ir.exam.app.core.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormulaMatrixFactoryTest {
    @Test fun `custom matrix supports every size from one to ten`() {
        for (rows in 1..10) for (columns in 1..10) {
            val tex = FormulaMatrixFactory.create(rows, columns)
            val matrix = NativeMathParser.parse(tex) as MathNode.Matrix
            assertEquals(rows, matrix.rows.size)
            assertTrue(matrix.rows.all { it.size == columns })
        }
    }

    @Test fun `matrix environment controls native delimiter`() {
        assertEquals('(', (NativeMathParser.parse(FormulaMatrixFactory.create(2, 2, "pmatrix")) as MathNode.Matrix).delimiter)
        assertEquals('|', (NativeMathParser.parse(FormulaMatrixFactory.create(2, 2, "vmatrix")) as MathNode.Matrix).delimiter)
        assertEquals('{', (NativeMathParser.parse(FormulaMatrixFactory.create(2, 2, "Bmatrix")) as MathNode.Matrix).delimiter)
    }
}
