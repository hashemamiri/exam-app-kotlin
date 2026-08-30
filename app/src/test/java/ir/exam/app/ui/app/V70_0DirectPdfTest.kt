package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V70.0 — آیکن پرینتر روی کارت آزمون + پی دی اف مستقیم با iText 5 (openPDF).
 * قابلیت‌های موجود (چاپ برگه/چاپ با کلید/ویرایش سند) نباید دست بخورند.
 */
class V70_0DirectPdfTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val exporter by lazy {
        source("app/src/main/java/ir/exam/app/core/printing/DirectPdfExporter.kt")
    }
    private val printCenter by lazy {
        source("app/src/main/java/ir/exam/app/ui/printing/ExamPrintCenterScreen.kt")
    }
    private val gradle by lazy { source("app/build.gradle.kts") }

    @Test
    fun `direct pdf exporter uses iText 5 openPDF with the print template`() {
        assertTrue("class DirectPdfExporter(" in exporter)
        assertTrue("PageSize.A4" in exporter)
        assertTrue("BaseFont.IDENTITY_H" in exporter)
        assertTrue("PdfPTable" in exporter)
        assertTrue("fonts/bnazanin.ttf" in exporter)
        assertTrue("fonts/bnazanin_bold.ttf" in exporter)
        assertTrue("addMatching(" in exporter)
        assertTrue("includeAnswerKey" in exporter)
        assertTrue("PdfWriter.RUN_DIRECTION_RTL" in exporter)
    }

    @Test
    fun `openpdf dependency is declared`() {
        assertTrue("com.github.librepdf:openpdf:1.3.43" in gradle)
    }

    @Test
    fun `printer icon on the exam card saves a direct pdf`() {
        assertTrue("Icons.Outlined.Print" in printCenter)
        assertTrue("contentDescription = \"پی دی اف مستقیم\"" in printCenter)
        assertTrue("CreateDocument(\"application/pdf\")" in printCenter)
        assertTrue("printableExam(examId, false, header" in printCenter)
        assertTrue("DirectPdfExporter(" in printCenter)
    }

    @Test
    fun `existing print capabilities stay intact`() {
        assertTrue("Text(\"چاپ برگه\")" in printCenter)
        assertTrue("Text(\"چاپ با کلید\")" in printCenter)
        assertTrue("contentDescription = \"ویرایش آزمون\"" in printCenter)
        assertTrue("OfficialPrintController" in printCenter)
    }
}
