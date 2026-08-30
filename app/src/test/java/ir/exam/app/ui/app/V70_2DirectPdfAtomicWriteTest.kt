package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V70.2/V71.0 — PDF باید پیش از لمس مقصد کامل شود و موفقیت فقط بعد از
 * بازخوانی و تطبیق قطعی فایل مقصد اعلام شود.
 */
class V70_2DirectPdfAtomicWriteTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val exporter by lazy {
        source("app/src/main/java/ir/exam/app/core/printing/DirectPdfExporter.kt")
    }
    private val safWriter by lazy {
        source("app/src/main/java/ir/exam/app/core/printing/VerifiedSafPdfWriter.kt")
    }

    @Test
    fun `pdf is finalized and validated in private staging before target`() {
        assertTrue("File.createTempFile(" in exporter)
        assertTrue("FileOutputStream(staged).use" in exporter)
        assertTrue("output.fd.sync()" in exporter)
        assertTrue("PdfArtifactVerifier.inspect(staged)" in exporter)
        assertFalse("buildPdf(withImages, buffer)" in exporter)
        assertFalse("buffer.toByteArray()" in exporter)
    }

    @Test
    fun `destination uses durable truncate flush and fsync`() {
        assertTrue("openFileDescriptor(target, \"rwt\")" in safWriter)
        assertTrue("ParcelFileDescriptor.AutoCloseOutputStream" in safWriter)
        assertTrue("output.channel.truncate(0L)" in safWriter)
        assertTrue("output.flush()" in safWriter)
        assertTrue("output.channel.force(true)" in safWriter)
        assertTrue("output.fd.sync()" in safWriter)
        assertTrue("openOutputStream(target, \"wt\")" in safWriter)
    }

    @Test
    fun `success requires destination readback size and sha256 match`() {
        assertTrue("openInputStream(target)" in safWriter)
        assertTrue("PdfArtifactVerifier::fingerprint" in safWriter)
        assertTrue("expected.hasSameBytes" in safWriter)
        assertTrue("if (descriptorAttempt.verified) return" in safWriter)
        assertTrue("if (streamAttempt.verified) return" in safWriter)
    }

    @Test
    fun `zero or incomplete placeholder is deleted on failure`() {
        assertTrue("contentResolver.delete(target, null, null)" in exporter)
        assertTrue("Result.failure(error)" in exporter)
        assertFalse("Result.success(Unit)" in exporter)
    }

    @Test
    fun `export template and fonts stay intact`() {
        assertTrue("PageSize.A4" in exporter)
        assertTrue("BaseFont.IDENTITY_H" in exporter)
        assertTrue("fonts/bnazanin.ttf" in exporter)
        assertTrue("fonts/bnazanin_bold.ttf" in exporter)
        assertTrue("addMatching(" in exporter)
        assertTrue("includeAnswerKey" in exporter)
        assertTrue("PdfWriter.RUN_DIRECTION_RTL" in exporter)
        assertTrue("PersianTextShaper.shape" in exporter)
    }
}
