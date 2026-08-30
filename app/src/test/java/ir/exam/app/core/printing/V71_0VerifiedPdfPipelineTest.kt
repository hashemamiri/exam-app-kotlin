package ir.exam.app.core.printing

import com.lowagie.text.Document
import com.lowagie.text.PageSize
import com.lowagie.text.Paragraph
import com.lowagie.text.pdf.PdfStream
import com.lowagie.text.pdf.PdfWriter
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** V71.0 — تست واقعی ساخت/Parse/هش و قرارداد ذخیرهٔ تأییدشدهٔ PDF. */
class V71_0VerifiedPdfPipelineTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private fun validPdf(): File {
        val file = File.createTempFile("v71-valid-", ".pdf")
        FileOutputStream(file).use { output ->
            val document = Document(PageSize.A4)
            val writer = PdfWriter.getInstance(document, output)
            writer.setCloseStream(false)
            writer.setFullCompression()
            writer.setCompressionLevel(PdfStream.BEST_COMPRESSION)
            document.open()
            document.add(Paragraph("verified pdf pipeline"))
            document.close()
            output.flush()
            output.fd.sync()
        }
        return file
    }

    @Test
    fun `real openpdf artifact is parsed and fingerprinted`() {
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
        val zero = File.createTempFile("v71-zero-", ".pdf")
        val truncated = File.createTempFile("v71-truncated-", ".pdf").apply {
            writeText("%PDF-1.5\nnot-finished")
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
    fun `exporter enables professional openpdf finalization and compression`() {
        val exporter = source("app/src/main/java/ir/exam/app/core/printing/DirectPdfExporter.kt")
        assertTrue("withContext(Dispatchers.IO)" in exporter)
        assertTrue("writer.setCloseStream(false)" in exporter)
        assertTrue("writer.setFullCompression()" in exporter)
        assertTrue("writer.setCompressionLevel(PdfStream.BEST_COMPRESSION)" in exporter)
        assertTrue("output.channel.force(true)" in exporter)
        assertTrue("document.addTitle(" in exporter)
        assertTrue("document.addCreator(" in exporter)
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
}
