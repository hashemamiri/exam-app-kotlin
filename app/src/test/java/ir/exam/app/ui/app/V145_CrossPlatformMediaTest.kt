package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V145 — رسانهٔ برنامه و سایت در یک جا (S3/آروان) و پشتیبانی data: در اندروید. */
class V145_CrossPlatformMediaTest {
    private fun source(path: String): String {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return File(dir ?: File(""), path).readText()
    }

    @Test
    fun `android opens data URLs for question images and uploads`() {
        val local = source("app/src/main/java/ir/exam/app/data/repository/LocalImageRepository.kt")
        assertTrue("uri.scheme.equals(\"data\", true)" in local && "DataUrlFetcher.decodeBytes(uri.toString())" in local)
        val up = source("app/src/main/java/ir/exam/app/data/repository/SupabaseQuestionImageUploader.kt")
        assertTrue("uri.scheme.equals(\"data\", true)" in up && "DataUrlFetcher.decodeBytes(uri.toString())" in up)
    }

    @Test
    fun `android uploads question media through media-upload with Supabase Storage fallback`() {
        val up = source("app/src/main/java/ir/exam/app/data/repository/SupabaseQuestionImageUploader.kt")
        assertTrue("functions.invoke(\n                    \"media-upload\"" in up)
        assertTrue("\"matching\" -> \"matching_images\"" in up)
        assertTrue("if (code == \"r2_not_configured\")" in up && "HttpStatusCode.ServiceUnavailable.value" in up)
        assertTrue("httpClient.put(uploadUrl)" in up && "header(\"Content-Type\", signedType)" in up)
        assertTrue("uploadBytes(prefix, bytes, \"m4a\", \"audio/mp4\", \"audio\")" in up)
        assertTrue("bucket.publicUrl(path)" in up)
    }

    @Test
    fun `site never stores data URLs in online exams`() {
        val b = source("site/src/builder.js")
        assertTrue("ml[a] = await uploadImage(await dataBlob(ml[a]), 'matching_images', examId)" in b)
        assertTrue("q.audio = await S.uploadAudioBlob(ab, examId)" in b)
        assertTrue("S.uploadAudioBlob = function (blob, examId)" in source("site/src/extras.js"))
    }
}
