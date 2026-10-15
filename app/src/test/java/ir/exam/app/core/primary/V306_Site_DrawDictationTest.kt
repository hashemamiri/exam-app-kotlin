package ir.exam.app.core.primary

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V306 — هم‌ترازی سایت با اپ: پاسخ با تخته و املا در student.js / builder.js و SQL نرمال‌سازی. */
class V306_Site_DrawDictationTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists() && !File(dir, "settings.gradle").exists()) dir = dir.parentFile
        return dir ?: File("").absoluteFile
    }

    @Test
    fun `site student and builder support draw and dictation`() {
        val student = File(root(), "site/src/student.js").readText()
        val builder = File(root(), "site/src/builder.js").readText()
        assertTrue(student.contains("if (obj.mode === 'draw' && type === 'essay') q.mode = 'draw';"))
        assertTrue(student.contains("q.dictationText = obj.speak ? String(obj.speak) : ''"))
        assertTrue(student.contains("var drawMode = ex.primary && q.type === 'essay' && q.mode === 'draw';"))
        assertTrue(builder.contains("var isDraw = q.type === 'essay' && q.mode === 'draw', isDict = q.type === 'fill' && q.mode === 'dictation';"))
        assertTrue(builder.contains("['dictation', 'املا (گوش بده و بنویس)']"))
    }

    @Test
    fun `dictation grading normalizes zwnj spaces and arabic letter shapes in sql`() {
        val sql = File(root(), "supabase/migrations/20261015_native_dictation_v306.sql").readText()
        assertTrue(sql.contains("chr(8204)"))
        assertTrue(sql.contains("'[[:space:]'"))
        assertTrue(sql.contains("يكۀة٫،"))
    }
}
