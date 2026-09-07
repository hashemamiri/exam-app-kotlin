package ir.exam.app.ui.app

import ir.exam.app.ui.printing.HeaderSchema
import java.io.File
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** تنظیمات سربرگِ فعال که در سازندهٔ بومی و برگهٔ چاپ مشترک است. */
class PrintHeaderSettingsTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String): String = File(root(), path).readText()

    @Test
    fun `all supported header templates remain available`() {
        val schemaFile = File(root(), "app/src/main/assets/print/header_settings_schema.json")
        val schema = json.decodeFromString(
            HeaderSchema.serializer(),
            schemaFile.readText()
        )
        assertEquals(
            listOf("classic", "formal", "sama", "school", "edu", "detailed-school", "ministry"),
            schema.templates.map { it.id }
        )
        assertTrue(schema.templates.all { it.fields.isNotEmpty() })
        assertTrue(schema.templates.flatMap { it.fields }.any { it.id == "h7_ministry" })
    }

    @Test
    fun `native builder and print centre keep the common header settings path`() {
        val builder = source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt")
        val printCentre = source("app/src/main/java/ir/exam/app/ui/printing/ExamPrintCenterScreen.kt")
        val headerSettings = source("app/src/main/java/ir/exam/app/ui/printing/PrintHeaderSettings.kt")
        assertTrue("سازنده تنظیمات سربرگ را باز نمی‌کند", "HeaderSettingsDialog(" in builder)
        assertTrue("حالت چاپ سازنده وجود ندارد", "printMode: Boolean = false" in builder)
        assertTrue("مرکز چاپ تنظیمات ذخیره‌شده را نمی‌خواند", "PrintHeaderStore(context.applicationContext)" in printCentre)
        assertTrue("شِمای سربرگ از asset خوانده نمی‌شود", "print/header_settings_schema.json" in headerSettings)
    }
}
