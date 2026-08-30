package ir.exam.app.core.printing

import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.WriterProperties
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** V72.0 — تست واقعی ساخت/Parse/هش و قرارداد ذخیرهٔ تأییدشدهٔ PDF با iText 7. */
class V71_0VerifiedPdfPipelineTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private fun validPdf(): File {
        val file = File.createTempFile("v72-valid-", ".pdf")
        FileOutputStream(file).use { output ->
            val writer = PdfWriter(
                output,
                WriterProperties()
                    .setFullCompressionMode(true)
                    .setCompressionLevel(9)
            )
            val pdf = PdfDocument(writer)
            val document = Document(pdf, PageSize.A4)
            document.add(Paragraph("verified pdf pipeline"))
            document.close()
        }
        return file
    }

    @Test
    fun `real itext7 artifact is parsed and fingerprinted`() {
        val file = validPdf()
        try {
            val artifact = PdfArtifactVerifier.inspect(file)
            assertEquals(file.length(), artifact.byteCount)
            assertEquals(1, artifact.pageCount)
            assertEquals(64, artifact.sha256.length)
            assertTrue(artifact.byteCount > 100L)
        } finally {
            file.delete()
        }
    }

    @Test
    fun `zero and truncated files are rejected`() {
        val zero = File.createTempFile("v72-zero-", ".pdf")
        val truncated = File.createTempFile("v72-truncated-", ".pdf").apply {
            writeText("%PDF-1.7\nnot-finished")
        }
        try {
            assertTrue(runCatching { PdfArtifactVerifier.inspect(zero) }.exceptionOrNull() is IOException)
            assertTrue(runCatching { PdfArtifactVerifier.inspect(truncated) }.exceptionOrNull() is IOException)
        } finally {
            zero.delete()
            truncated.delete()
        }
    }

    @Test
    fun `stream fingerprints detect every byte mismatch`() {
        val first = PdfArtifactVerifier.fingerprint(ByteArrayInputStream("pdf-one".toByteArray()))
        val same = PdfArtifactVerifier.fingerprint(ByteArrayInputStream("pdf-one".toByteArray()))
        val changed = PdfArtifactVerifier.fingerprint(ByteArrayInputStream("pdf-two".toByteArray()))
        assertEquals(first, same)
        assertNotEquals(first.sha256, changed.sha256)
        assertEquals(7L, first.byteCount)
        val artifact = PdfArtifact(first.byteCount, 1, first.sha256)
        assertTrue(artifact.hasSameBytes(same))
        assertFalse(artifact.hasSameBytes(changed))
    }

    @Test
    fun `exporter enables professional itext7 finalization and compression`() {
        val exporter = source("app/src/main/java/ir/exam/app/core/printing/DirectPdfExporter.kt")
        assertTrue("WriterProperties()" in exporter)
        assertTrue("PdfWriter(" in exporter)
        assertTrue("pdf.setCloseWriter(false)" in exporter)
        assertTrue("setFullCompressionMode(true)" in exporter)
        assertTrue("setCompressionLevel(9)" in exporter)
        assertTrue("output.channel.force(true)" in exporter)
        assertTrue("setTitle(" in exporter)
        assertTrue("setCreator(" in exporter)
        assertTrue("PdfArtifactVerifier.inspect(staged)" in exporter)
        assertTrue("verifiedWriter.commit(staged, target, artifact)" in exporter)
        assertTrue("loadBitmapSafely" in exporter)
    }

    @Test
    fun `ui never claims success before verified receipt`() {
        val screen = source("app/src/main/java/ir/exam/app/ui/printing/ExamPrintCenterScreen.kt")
        assertTrue("در حال ساخت، ذخیره و اعتبارسنجی PDF" in screen)
        assertTrue("receipt.sizeKiB" in screen)
        assertTrue("receipt.pageCount" in screen)
        assertTrue("فایل PDF تأیید و ذخیره شد" in screen)
        assertTrue("pdfStatusIsError" in screen)
        assertTrue("enabled = !pdfExporting" in screen)
        assertTrue("finally {\n                    pdfExporting = false" in screen)
        assertFalse("pdfStatus = \"فایل PDF ساخته شد.\"" in screen)
    }

    @Test
    fun `artifact verifier uses itext7 and no openpdf remains in active printing`() {
        val verifier = source("app/src/main/java/ir/exam/app/core/printing/PdfArtifactVerifier.kt")
        val exporter = source("app/src/main/java/ir/exam/app/core/printing/DirectPdfExporter.kt")
        assertTrue("com.itextpdf.kernel.pdf.PdfReader" in verifier)
        assertTrue("PdfDocument(reader)" in verifier)
        assertTrue("reader.hasRebuiltXref()" in verifier)
        assertFalse("com.lowagie" in verifier)
        assertFalse("com.lowagie" in exporter)
    }
}
