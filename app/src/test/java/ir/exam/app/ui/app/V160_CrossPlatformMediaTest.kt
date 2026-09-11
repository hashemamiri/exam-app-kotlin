package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V160 — رسانهٔ ساخته‌شده در سایت/برنامه باید در هر دو قابل مشاهده و ویرایش باشد (تصویر https در استودیو، صوت در سایت). */
class V160_CrossPlatformMediaTest {
    private fun source(path: String): String {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return File(dir ?: File(""), path).readText()
    }

    @Test
    fun `app studio and image repository open remote https images`() {
        val core = source("app/src/main/java/ir/exam/app/ui/printing/ExamImageStudioCore.kt")
        assertTrue("internal fun decodeImageRefBounded(context: android.content.Context, ref: String, maxDim: Int, strict: Boolean = false)" in core)
        assertTrue("decodeImageRefBounded(context, ref.dataUrl, 200)" in core && "decodeImageRefBounded(context, ref.dataUrl, 2560, strict = true)" in core)
        val repo = source("app/src/main/java/ir/exam/app/data/repository/LocalImageRepository.kt")
        assertTrue("uri.scheme.equals(\"https\", true) || uri.scheme.equals(\"http\", true)" in repo)
        assertTrue("RemoteMediaBytes.fetchOrNull(appContext, uri.toString())" in repo)
        val rm = source("app/src/main/java/ir/exam/app/core/media/RemoteMediaBytes.kt")
        assertTrue("\"Authorization\" to \"Bearer \$token\", \"apikey\" to BuildConfig.SUPABASE_ANON_KEY" in rm)
        assertTrue("url.startsWith(\"\$base/storage/v1/object/\")" in rm)
    }

    @Test
    fun `site reads private storage media with session and restores audioDuration`() {
        val app = source("site/src/app.js")
        assertTrue("function mediaBlobUrl(u)" in app && "'/storage/v1/object/authenticated/'" in app)
        assertTrue("if (k === 'src' && (tag === 'img' || tag === 'audio') && isOwnStorageUrl(attrs[k]))" in app)
        assertTrue("function inlinePrintImages(payload)" in app && "JSON.stringify({k: 'img', src: u, w: 420})" in app)
        val extras = source("site/src/extras.js")
        assertTrue("function audioDuration(blob)" in extras && "await audioDuration(pending)" in extras)
        val studio = source("site/src/studio.js")
        assertTrue("S.mediaBlobUrl ? S.mediaBlobUrl(src)" in studio && "im.crossOrigin = 'anonymous'" in studio)
    }
}
