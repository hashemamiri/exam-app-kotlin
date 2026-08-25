package ir.exam.app.ui.auth

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * V60.2 — لوگوی رسمی «G» گوگل به‌صورت وکتور (مسیرهای استاندارد برند با
 * چهار رنگ رسمی)؛ بدون فایل asset و وابستگی جدید.
 */
val GoogleLogo: ImageVector by lazy {
    ImageVector.Builder(
        name = "GoogleLogo",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 48f,
        viewportHeight = 48f
    ).apply {
        // آبی
        path(fill = SolidColor(Color(0xFF4285F4))) {
            moveTo(45.12f, 24.5f)
            curveTo(45.12f, 22.98f, 44.98f, 21.52f, 44.73f, 20.12f)
            lineTo(24f, 20.12f)
            lineTo(24f, 28.44f)
            lineTo(35.84f, 28.44f)
            curveTo(35.33f, 31.18f, 33.78f, 33.5f, 31.45f, 35.05f)
            lineTo(31.45f, 40.57f)
            lineTo(38.56f, 40.57f)
            curveTo(42.72f, 36.74f, 45.12f, 31.14f, 45.12f, 24.5f)
            close()
        }
        // سبز
        path(fill = SolidColor(Color(0xFF34A853))) {
            moveTo(24f, 46f)
            curveTo(29.94f, 46f, 34.92f, 44.03f, 38.56f, 40.57f)
            lineTo(31.45f, 35.05f)
            curveTo(29.48f, 36.37f, 26.96f, 37.15f, 24f, 37.15f)
            curveTo(18.27f, 37.15f, 13.42f, 33.28f, 11.69f, 28.08f)
            lineTo(4.34f, 28.08f)
            lineTo(4.34f, 33.78f)
            curveTo(7.96f, 40.98f, 15.4f, 46f, 24f, 46f)
            close()
        }
        // زرد
        path(fill = SolidColor(Color(0xFFFBBC05))) {
            moveTo(11.69f, 28.08f)
            curveTo(11.25f, 26.76f, 11f, 25.35f, 11f, 23.9f)
            curveTo(11f, 22.45f, 11.25f, 21.04f, 11.69f, 19.72f)
            lineTo(11.69f, 14.02f)
            lineTo(4.34f, 14.02f)
            curveTo(2.85f, 16.99f, 2f, 20.34f, 2f, 23.9f)
            curveTo(2f, 27.46f, 2.85f, 30.81f, 4.34f, 33.78f)
            lineTo(11.69f, 28.08f)
            close()
        }
        // قرمز
        path(fill = SolidColor(Color(0xFFEA4335))) {
            moveTo(24f, 10.65f)
            curveTo(27.23f, 10.65f, 30.13f, 11.76f, 32.41f, 13.94f)
            lineTo(38.72f, 7.63f)
            curveTo(34.91f, 4.08f, 29.93f, 1.9f, 24f, 1.9f)
            curveTo(15.4f, 1.9f, 7.96f, 6.92f, 4.34f, 14.02f)
            lineTo(11.69f, 19.72f)
            curveTo(13.42f, 14.52f, 18.27f, 10.65f, 24f, 10.65f)
            close()
        }
    }.build()
}
