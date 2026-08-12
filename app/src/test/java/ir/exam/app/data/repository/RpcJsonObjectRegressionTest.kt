package ir.exam.app.data.repository

import ir.exam.app.data.dto.CalendarMonthResponseDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertTrue
import org.junit.Test

class RpcJsonObjectRegressionTest {
    @Test
    fun `calendar jsonb rpc payload decodes as object not row array`() {
        val raw = """{"ok":true,"notes":[]}"""
        val decoded = Json.decodeFromString<CalendarMonthResponseDto>(raw)
        assertTrue(decoded.ok)
        assertTrue(decoded.notes.isEmpty())
    }
}
