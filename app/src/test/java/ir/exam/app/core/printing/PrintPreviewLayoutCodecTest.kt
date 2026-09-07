package ir.exam.app.core.printing

import ir.exam.app.domain.model.OfficialExamPrintable
import ir.exam.app.domain.model.OfficialPrintHeader
import ir.exam.app.domain.model.OfficialPrintQuestion
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrintPreviewLayoutCodecTest {
    @Test
    fun `legacy html pixel layout migrates to bounded native millimeters`() {
        val layouts = PrintPreviewLayoutCodec.decode(
            """{"0":{"x":38,"y":76,"w":378,"h":189,"free":true}}"""
        )
        val item = layouts.getValue(0)
        assertTrue(item.free)
        assertEquals(100f, item.widthMm, .2f)
        assertEquals(50f, item.heightMm, .2f)
        assertEquals(20.1f, item.yMm, .2f)
        // x پیشین از راستِ محتوای HTML بود؛ در PDF از چپ نگه‌داری می‌شود.
        assertTrue(item.xMm in 71f..73f)
    }

    @Test
    fun `new native layout round trips without css fields`() {
        val original = NativePrintFigureLayout(
            xMm = 14.25f,
            yMm = 83.75f,
            widthMm = 67.5f,
            heightMm = 42.5f,
            free = true
        )
        val encoded = PrintPreviewLayoutCodec.encode(mapOf(2 to original))
        assertTrue("\"nativePdf\":true" in encoded)
        val restored = PrintPreviewLayoutCodec.decode(encoded).getValue(2)
        assertEquals(original.xMm, restored.xMm, .01f)
        assertEquals(original.yMm, restored.yMm, .01f)
        assertEquals(original.widthMm, restored.widthMm, .01f)
        assertEquals(original.heightMm, restored.heightMm, .01f)
        assertTrue(restored.free)
    }

    @Test
    fun `snapshot retains question keyed layout and separator spacing`() {
        val question = OfficialPrintQuestion(
            number = 1,
            text = "سؤال",
            score = 2.0,
            figLayoutsJson = PrintPreviewLayoutCodec.encode(
                mapOf(0 to NativePrintFigureLayout(4f, 12f, 55f, 35f, false))
            ),
            sepExtraPx = 120
        )
        val printable = OfficialExamPrintable(
            documentTitle = "آزمون",
            header = OfficialPrintHeader(),
            subject = "ریاضی",
            durationMinutes = 60,
            questions = listOf(question)
        )
        val result = Json.parseToJsonElement(PrintPreviewLayoutCodec.snapshot(printable)).jsonObject
        val savedQuestion = result.getValue("1").jsonObject
        assertEquals(120, savedQuestion.getValue("sepExtraPx").jsonPrimitive.content.toInt())
        assertTrue("nativePdf" in savedQuestion.getValue("figLayouts").toString())
    }

    @Test
    fun `separator drag conversion allows both increasing and decreasing spacing`() {
        assertEquals(40, PrintPreviewLayoutCodec.separatorPxDeltaFromPdfPoints(30f))
        assertEquals(-40, PrintPreviewLayoutCodec.separatorPxDeltaFromPdfPoints(-30f))
    }
}
