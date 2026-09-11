package ir.exam.app.ui.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V144/V144.1 — رسانهٔ آزمون روی ذخیره‌ساز S3 (آروان/R2) با fallback به Supabase Storage. */
class V144_MediaS3Test {
    private fun source(path: String): String {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return File(dir ?: File(""), path).readText()
    }

    @Test
    fun `media-upload issues presigned PUT only for authenticated teacher or manager`() {
        val fn = source("supabase/functions/media-upload/index.ts")
        assertTrue("service.auth.getUser(authorization.slice(7))" in fn)
        assertTrue("role !== 'teacher' && role !== 'manager'" in fn)
        assertTrue("'X-Amz-Algorithm', 'AWS4-HMAC-SHA256'" in fn && "UNSIGNED-PAYLOAD" in fn && "content-type;host" in fn)
        assertTrue("env('S3_ENDPOINT')" in fn && "env('S3_REGION') || 'auto'" in fn && "env('S3_PUBLIC_BASE') || env('R2_PUBLIC_BASE')" in fn)
        assertTrue("error: 'r2_not_configured'" in fn && "}, 503)" in fn)
        assertTrue("const MAX_IMAGE = 8 * 1024 * 1024" in fn && "const MAX_AUDIO = 3 * 1024 * 1024" in fn)
        assertTrue("['questions', 'option_images', 'matching_images']" in fn)
        assertTrue("[functions.media-upload]\nverify_jwt = false" in source("supabase/config.toml"))
    }

    @Test
    fun `site uploads via S3 first and falls back to Supabase Storage`() {
        val app = source("site/src/app.js")
        assertTrue("async function uploadMedia(blob, kind, folder, examId, ext, contentType)" in app)
        assertTrue("http('/functions/v1/media-upload'" in app && "fetch(t.upload_url, {method: 'PUT'" in app)
        assertTrue("t.error === 'r2_not_configured') r2Disabled = true" in app && "e.status === 503) r2Disabled = true" in app)
        assertTrue("'/storage/v1/object/public/' + MEDIA_BUCKET + '/' + path" in app)
        assertFalse("service_role" in app)
    }

    @Test
    fun `storage-maintenance also sweeps S3 orphans`() {
        val fn = source("supabase/functions/storage-maintenance/index.ts")
        assertTrue("function r2Config()" in fn && "'list-type': '2'" in fn && "r2PathFromPublicUrl(value, r2.publicBase)" in fn)
        assertTrue("if (r2) await r2Delete(r2, r2Orphans);" in fn && "r2_enabled: !!r2" in fn)
    }
}
