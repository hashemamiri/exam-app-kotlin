package ir.exam.app.ui.image

import ir.exam.app.domain.model.CropRect
import kotlin.math.min

/**
 * هندسهٔ خالص برش مربعی ویرایشگر تصویر؛ بدون هیچ وابستگی به Compose یا Android
 * تا رفتار آن در تست JVM قابل بررسی و تضمین باشد.
 */
object CropGeometry {

    const val MIN_SIDE: Float = .20f
    const val MAX_SIDE: Float = .98f

    /** ضلعی که کادر روی تصویر می‌گیرد: کسری از ضلع کوتاه‌تر تا همیشه مربع بماند. */
    fun shortSide(rotatedWidth: Float, rotatedHeight: Float, cropSide: Float): Float =
        min(rotatedWidth, rotatedHeight) * cropSide

    /** ابعاد تصویر پس از چرخش؛ چرخش ۹۰/۲۷۰ درجه عرض و ارتفاع را جابه‌جا می‌کند. */
    fun rotatedSize(rawWidth: Float, rawHeight: Float, rotationDegrees: Int): Pair<Float, Float> {
        val quarterTurn = ((rotationDegrees % 360) + 360) % 180 != 0
        return if (quarterTurn) rawHeight to rawWidth else rawWidth to rawHeight
    }

    /** کسر هر ضلع کادر نسبت به تصویر چرخیده؛ مربع واقعی در پیکسل. */
    fun sideFractions(cropSide: Float, rotatedWidth: Float, rotatedHeight: Float): Pair<Float, Float> {
        val short = shortSide(rotatedWidth, rotatedHeight, cropSide)
        return (short / rotatedWidth) to (short / rotatedHeight)
    }

    /** مرکز را داخل بازهٔ مجاز هر محور نگه می‌دارد تا کادر از تصویر بیرون نزند. */
    fun clampCenter(center: Float, sideFraction: Float): Float =
        center.coerceIn(sideFraction / 2f, 1f - sideFraction / 2f)

    /** جابه‌جایی آزاد مرکز کادر با حفظ کامل آن داخل تصویر. */
    fun moveCenter(
        centerX: Float,
        centerY: Float,
        dragX: Float,
        dragY: Float,
        displayWidthPx: Float,
        displayHeightPx: Float,
        sideXFraction: Float,
        sideYFraction: Float
    ): Pair<Float, Float> =
        clampCenter(centerX + dragX / displayWidthPx, sideXFraction) to
            clampCenter(centerY + dragY / displayHeightPx, sideYFraction)

    /**
     * drag هر ضلع را به قرارداد مشترک تبدیل می‌کند: حرکت به بیرون همیشه مثبت
     * (بزرگ‌شدن) و حرکت به داخل همیشه منفی است.
     */
    fun resizeDeltaForEdge(edge: CropEdgeKind, dragDeltaPx: Float): Float = when (edge) {
        CropEdgeKind.LEFT, CropEdgeKind.TOP -> -dragDeltaPx
        CropEdgeKind.RIGHT, CropEdgeKind.BOTTOM -> dragDeltaPx
        // V55.14 — گوشه‌ها: مؤلفهٔ غالبِ کشش قبلاً به‌صورت جدا (x,y) به
        // resizeDeltaForCorner داده می‌شود؛ اینجا صدا زده نمی‌شوند.
        else -> dragDeltaPx
    }

    /**
     * V55.14 — کشش از «گوشه»: بردار (dx,dy) به دلتای بزرگ/کوچک‌شدن تبدیل
     * می‌شود؛ کشیدن به سمت بیرونِ همان گوشه = بزرگ‌شدن.
     */
    fun resizeDeltaForCorner(corner: CropEdgeKind, dragXPx: Float, dragYPx: Float): Float {
        val sx = when (corner) {
            CropEdgeKind.TOP_LEFT, CropEdgeKind.BOTTOM_LEFT -> -dragXPx
            CropEdgeKind.TOP_RIGHT, CropEdgeKind.BOTTOM_RIGHT -> dragXPx
            else -> 0f
        }
        val sy = when (corner) {
            CropEdgeKind.TOP_LEFT, CropEdgeKind.TOP_RIGHT -> -dragYPx
            CropEdgeKind.BOTTOM_LEFT, CropEdgeKind.BOTTOM_RIGHT -> dragYPx
            else -> 0f
        }
        // مؤلفهٔ بزرگ‌تر تعیین‌کننده است تا حرکت قطری طبیعی حس شود.
        return if (kotlin.math.abs(sx) >= kotlin.math.abs(sy)) sx else sy
    }

    /** تغییر اندازهٔ ضلع؛ delta بر حسب پیکسل و نسبت به ضلع کوتاه نمایشی. */
    fun resizeSide(oldSide: Float, signedDeltaPx: Float, minDimensionPx: Float): Float =
        (oldSide + signedDeltaPx / minDimensionPx).coerceIn(MIN_SIDE, MAX_SIDE)

    /** بعد از تغییر اندازه، مرکز باید جابه‌جا شود تا لبه/گوشهٔ مقابل ثابت بماند. */
    fun recenterAfterResize(
        edge: CropEdgeKind,
        pixelChangePx: Float,
        displayWidthPx: Float,
        displayHeightPx: Float,
        centerX: Float,
        centerY: Float
    ): Pair<Float, Float> {
        val halfX = pixelChangePx / 2f / displayWidthPx
        val halfY = pixelChangePx / 2f / displayHeightPx
        return when (edge) {
            CropEdgeKind.LEFT -> (centerX - halfX) to centerY
            CropEdgeKind.RIGHT -> (centerX + halfX) to centerY
            CropEdgeKind.TOP -> centerX to (centerY - halfY)
            CropEdgeKind.BOTTOM -> centerX to (centerY + halfY)
            // V55.14 — گوشه‌ها: هر دو محور جابه‌جا می‌شوند تا گوشهٔ مقابل ثابت بماند.
            CropEdgeKind.TOP_LEFT -> (centerX - halfX) to (centerY - halfY)
            CropEdgeKind.TOP_RIGHT -> (centerX + halfX) to (centerY - halfY)
            CropEdgeKind.BOTTOM_LEFT -> (centerX - halfX) to (centerY + halfY)
            CropEdgeKind.BOTTOM_RIGHT -> (centerX + halfX) to (centerY + halfY)
        }
    }

    /** تبدیل مرکز و ضلع نمایشی به CropRect تصویر چرخیده. */
    fun cropRect(
        centerX: Float,
        centerY: Float,
        cropSide: Float,
        rawWidth: Float,
        rawHeight: Float,
        rotationDegrees: Int
    ): CropRect {
        val (imageWidth, imageHeight) = rotatedSize(rawWidth, rawHeight, rotationDegrees)
        val (widthFraction, heightFraction) = sideFractions(cropSide, imageWidth, imageHeight)
        val safeX = clampCenter(centerX, widthFraction)
        val safeY = clampCenter(centerY, heightFraction)
        return CropRect(
            left = (safeX - widthFraction / 2f).coerceIn(0f, 1f - widthFraction),
            top = (safeY - heightFraction / 2f).coerceIn(0f, 1f - heightFraction),
            width = widthFraction,
            height = heightFraction
        )
    }

    /** کسر مساحت برش نسبت به تصویر چرخیده؛ برای تخمین حجم خروجی. */
    fun areaFraction(cropSide: Float, rawWidth: Float, rawHeight: Float, rotationDegrees: Int): Float {
        val (imageWidth, imageHeight) = rotatedSize(rawWidth, rawHeight, rotationDegrees)
        val short = shortSide(imageWidth, imageHeight, cropSide)
        return (short / imageWidth) * (short / imageHeight)
    }
}

enum class CropEdgeKind {
    LEFT, RIGHT, TOP, BOTTOM,
    // V55.14 — کشش از گوشه‌ها هم ممکن شد.
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
}
