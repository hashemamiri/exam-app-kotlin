package ir.exam.app.core.math

object FormulaMatrixFactory {
    fun create(rows: Int, columns: Int, environment: String = "bmatrix"): String {
        val safeRows = rows.coerceIn(1, 10)
        val safeColumns = columns.coerceIn(1, 10)
        val safeEnvironment = environment.takeIf {
            it in setOf("matrix", "bmatrix", "pmatrix", "vmatrix", "Bmatrix", "Vmatrix")
        } ?: "bmatrix"
        var letter = 0
        val body = (0 until safeRows).joinToString(" \\\\ ") {
            (0 until safeColumns).joinToString(" & ") {
                ('a'.code + (letter++ % 26)).toChar().toString()
            }
        }
        return "\\begin{$safeEnvironment}$body\\end{$safeEnvironment}"
    }
}
