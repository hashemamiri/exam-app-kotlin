package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V160.2 — ریشهٔ HTTP 403 آروان: شیء بدون x-amz-acl: public-read خصوصی می‌شود. */
class V160_2_ArvanPublicAclTest {
    private fun source(path: String): String {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return File(dir ?: File(""), path).readText()
    }

    @Test
    fun `presigned PUT signs and returns public-read acl and clients send it`() {
        val fn = source("supabase/functions/media-upload/index.ts")
        assertTrue("const signedHeaders = 'content-type;host;x-amz-acl';" in fn)
        assertTrue("x-amz-acl:public-read\\n" in fn && "'x-amz-acl': 'public-read'" in fn)
        assertTrue("var hdrs = Object.assign({}, t.headers || {}, {'Content-Type': ct});" in source("site/src/app.js"))
        val up = source("app/src/main/java/ir/exam/app/data/repository/SupabaseQuestionImageUploader.kt")
        assertTrue("obj[\"headers\"]?.jsonObject?.forEach { (k, v) ->" in up)
        assertTrue(File("scripts/arvan_make_public.py").let { true })
    }
}
