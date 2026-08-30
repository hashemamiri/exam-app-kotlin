package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V72.0 — خروجی PDF مستقیم با iText 7 for Android + حفظ قابلیت‌های چاپ آزمون.
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
    private val settings by lazy { source("settings.gradle.kts") }

    @Test
    fun `direct pdf exporter uses iText7 with the print template`() {
        assertTrue("class DirectPdfExporter(" in exporter)
        assertTrue("PageSize.A4" in exporter)
        assertTrue("PdfEncodings.IDENTITY_H" in exporter)
        assertTrue("Table(" in exporter)
        assertTrue("fonts/bnazanin.ttf" in exporter)
        assertTrue("fonts/bnazanin_bold.ttf" in exporter)
        assertTrue("addMatching(" in exporter)
        assertTrue("includeAnswerKey" in exporter)
        assertTrue("BaseDirection.RIGHT_TO_LEFT" in exporter)
        assertTrue("PdfFontFactory" in exporter)
        assertTrue("WriterProperties()" in exporter)
        assertTrue("setFullCompressionMode(true)" in exporter)
        assertTrue("PersianTextShaper.shape" in exporter)
    }

    @Test
    fun `official iText7 android dependencies and repository are declared`() {
        assertTrue("com.itextpdf.android:kernel-android:7.2.5" in gradle)
        assertTrue("com.itextpdf.android:layout-android:7.2.5" in gradle)
        assertTrue("https://repo.itextsupport.com/android" in settings)
        assertFalse("com.github.librepdf:openpdf" in gradle)
        assertFalse("com.lowagie" in exporter)
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
