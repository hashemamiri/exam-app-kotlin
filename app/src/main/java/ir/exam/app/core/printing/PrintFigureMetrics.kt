package ir.exam.app.core.printing

import ir.exam.app.core.figure.FigureSpec

/**
 * اندازه و جای اختیاری شکل در مسیر PDF رسمی.
 *
 * این داده‌ها در خود توکن شکل می‌مانند تا PDF بومی اندازه و جای شکل را
 * مستقیماً هنگام رسم بخواند.
 */
object PrintFigureMetrics {
    private const val DEFAULT_WIDTH_MM = 95f
    private const val MIN_WIDTH_MM = 40f
    private const val MAX_WIDTH_MM = 180f
    private const val WIDTH_KEY = "wmm"
    private const val POSITION_X_KEY = "fx"
    private const val POSITION_Y_KEY = "fy"

    fun figureWidthMm(spec: FigureSpec): Float =
        spec.xNum(WIDTH_KEY, DEFAULT_WIDTH_MM)
            .takeIf { it.isFinite() }
            ?.coerceIn(MIN_WIDTH_MM, MAX_WIDTH_MM)
            ?: DEFAULT_WIDTH_MM

    /** null یعنی شکل باید در جریان طبیعی متن PDF قرار بگیرد. */
    fun figurePosMm(spec: FigureSpec): Pair<Float, Float>? {
        val x = spec.xStr(POSITION_X_KEY, "").toFloatOrNull() ?: return null
        val y = spec.xStr(POSITION_Y_KEY, "").toFloatOrNull() ?: return null
        if (!x.isFinite() || !y.isFinite()) return null
        return x to y
    }
}
