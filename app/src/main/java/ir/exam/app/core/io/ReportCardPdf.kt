package ir.exam.app.core.io

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.ByteArrayOutputStream

/**
 * V264 — خروجی PDF کارنامهٔ دانش‌آموز بدون WebView (android.graphics.pdf.PdfDocument، A4 عمودی).
 * متن فارسی با Canvas راست‌چین کشیده می‌شود؛ جدول با ستون‌های ثابت؛ صفحه‌بندی خودکار.
 * فقط چیدمان خالص در [layoutRows] است تا تست واحد بدون اندروید بتواند صفحه‌بندی را بررسی کند.
 */
object ReportCardPdf {
    data class Row(val title: String, val subject: String, val score: String, val percent: String, val feedback: String)

    const val PAGE_W = 595
    const val PAGE_H = 842
    private const val MARGIN = 36f
    private const val ROW_H = 22f
    private const val HEADER_BOTTOM = 110f

    /** تعداد ردیف در هر صفحه با توجه به حاشیه‌ها (برای تست). */
    fun rowsPerPage(): Int = ((PAGE_H - MARGIN - HEADER_BOTTOM - ROW_H) / ROW_H).toInt()

    /** صفحه‌بندی ردیف‌ها (خالص). */
    fun layoutRows(rows: List<Row>): List<List<Row>> = if (rows.isEmpty()) listOf(emptyList()) else rows.chunked(rowsPerPage())

    fun build(title: String, subtitle: String, rows: List<Row>, summary: String): ByteArray {
        val doc = PdfDocument()
        val pages = layoutRows(rows)
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f; color = Color.BLACK; textAlign = Paint.Align.RIGHT }
        val bold = Paint(text).apply { isFakeBoldText = true; textSize = 16f }
        val small = Paint(text).apply { textSize = 9f; color = Color.DKGRAY }
        val line = Paint().apply { color = Color.LTGRAY; strokeWidth = 0.8f }
        val fill = Paint().apply { color = Color.rgb(241, 245, 249) }
        // ستون‌ها از راست به چپ: آزمون، درس، نمره، درصد، بازخورد
        val right = PAGE_W - MARGIN
        val widths = floatArrayOf(170f, 90f, 80f, 55f, 128f)
        val xs = FloatArray(widths.size)
        var acc = right
        for (i in widths.indices) { xs[i] = acc - 4f; acc -= widths[i] }
        val headers = listOf("آزمون", "درس", "نمره", "درصد", "بازخورد")
        pages.forEachIndexed { index, chunk ->
            val page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, index + 1).create())
            val c: Canvas = page.canvas
            c.drawText(title, right, MARGIN + 18f, bold)
            c.drawText(subtitle, right, MARGIN + 38f, small)
            if (index == 0 && summary.isNotBlank()) c.drawText(summary, right, MARGIN + 56f, text)
            var y = HEADER_BOTTOM
            c.drawRect(MARGIN, y - 15f, right, y + 6f, fill)
            headers.forEachIndexed { i, h -> c.drawText(h, xs[i], y, text) }
            c.drawLine(MARGIN, y + 6f, right, y + 6f, line)
            chunk.forEach { r ->
                y += ROW_H
                listOf(r.title, r.subject, r.score, r.percent, r.feedback).forEachIndexed { i, v ->
                    c.drawText(ellipsize(v, text, widths[i] - 8f), xs[i], y, text)
                }
                c.drawLine(MARGIN, y + 6f, right, y + 6f, line)
            }
            if (chunk.isEmpty()) c.drawText("هنوز نمره‌ای ثبت نشده است.", right, y + ROW_H, text)
            c.drawText("صفحهٔ ${index + 1} از ${pages.size} · سامانهٔ آزمون آنلاین", right, PAGE_H - MARGIN + 10f, small)
            doc.finishPage(page)
        }
        val out = ByteArrayOutputStream()
        doc.writeTo(out)
        doc.close()
        return out.toByteArray()
    }

    private fun ellipsize(s: String, p: Paint, maxW: Float): String {
        if (p.measureText(s) <= maxW) return s
        var t = s
        while (t.isNotEmpty() && p.measureText("$t…") > maxW) t = t.dropLast(1)
        return "$t…"
    }
}
