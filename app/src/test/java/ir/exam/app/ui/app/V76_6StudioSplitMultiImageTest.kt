package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V76.6 — تفکیک چندسؤالهٔ بومی + مدیریت تصویرهای موجودِ سؤال:
 * ۱) ✂️ تفکیک با پیش‌فرض‌های ۲/۳/۴ + کادر دستی (کشیدن = جابه‌جایی، دستگیره = اندازه).
 * ۲) «همه بخش‌ها به همین سؤال» و «هر بخش → سؤال جداگانه» عین استودیو (کپی ساختارِ
 *    سؤال مبدا، متن خالی، یک تصویر برای هر بخش).
 * ۳) زنجیرهٔ کدگذاری مشترک encodeCropped (درج تکی + هر کادر تفکیک).
 * ۴) پل‌های مدیریت: __qmfQuestionImages / __qmfRemoveQuestionImage /
 *    __qmfReplaceQuestionImage (+ «تایید و جایگزینی» برای ویرایشِ دوباره).
 */
class V76_6StudioSplitMultiImageTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val studio by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamImageStudioCore.kt") }
    private val dialog by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintDialog.kt") }
    private val assetText by lazy { File(root(), "app/src/main/assets/print/exam_print.html").readText() }

    @Test
    fun `split mode with preset boxes is native`() {
        assertTrue("✂️ تفکیک چندسؤاله" in studio)
        assertTrue("✂️ ۲ سؤال (بالا / پایین)" in studio)
        assertTrue("✂️ ۳ سؤال ستونی" in studio)
        assertTrue("✂️ ۴ سؤال (۲×۲)" in studio)
        assertTrue("➕ کادر جدید" in studio)
        assertTrue("🗑️ حذف کادر انتخاب‌شده" in studio)
        assertTrue("var splitMode by remember { mutableStateOf(false) }" in studio)
        assertTrue("var splitBoxes by remember" in studio)
        assertTrue("Corner.SPLIT_MOVE" in studio)
        assertTrue("Corner.SPLIT_RESIZE" in studio)
        assertTrue("💾 همه بخش‌ها به همین سؤال" in studio)
        assertTrue("🧩 هر بخش → سؤال جداگانه" in studio)
        // هر دو مسیر اعمال از زنجیرهٔ مشترک می‌گذرند
        assertTrue("splitBoxes.mapNotNull { b -> encodeCropped(base, b, scanOn, threshold, outSize, quality) }" in studio)
    }

    @Test
    fun `shared encode pipeline used by single insert and split`() {
        assertTrue("private fun encodeCropped(" in studio)
        // processAndEncode حالا از helper مشترک استفاده می‌کند
        assertTrue("""encodeCropped(bmp, crop, scanOn, threshold, outSize, quality)""" in studio)
        assertTrue("internal fun decodeDataUrlBounded(dataUrl: String, maxDim: Int): Bitmap?" in studio)
    }

    @Test
    fun `existing image management wired through new bridges`() {
        assertTrue("window.__qmfQuestionImages" in dialog)
        assertTrue("window.__qmfRemoveQuestionImage" in dialog)
        assertTrue("window.__qmfReplaceQuestionImage" in dialog)
        assertTrue("existingImages = parseExistingImages(studioImagesJson)" in dialog)
        assertTrue("internal fun parseExistingImages" in dialog)
        // ویرایشِ دوباره: جایگزینی به‌جای درج
        assertTrue("تایید و جایگزینی" in studio)
        assertTrue("if (editIndex >= 0) onReplaceExisting(editIndex, dataUrl, h) else onInsert(dataUrl, h)" in studio)
        // عکسِ تازه همیشه درج است، نه جایگزینی
        assertTrue("editIndex = -1" in studio)
    }

    @Test
    fun `split to separate questions clones the source question`() {
        assertTrue("window.__qmfSplitQuestion" in dialog)
        assertTrue("""window.__qmfSplitQuestion = function (qid, b64Items) {""" in assetText)
        // عین رفتار استودیو: کپیِ ساختار، متن خالی، یک تصویر، درج بعد از سؤال مبدا
        assertTrue("var cl = JSON.parse(JSON.stringify(src));" in assetText)
        assertTrue("""      cl.text = "";""" in assetText)
        assertTrue("""      cl.qimgImages = [{ src: String(it.d), w: 420, h: Number(it.h) || 0 }];""" in assetText)
        assertTrue("questions.splice(at + 1 + made, 0, cl);" in assetText)
        assertTrue("""    return "ok:" + made;""" in assetText)
    }

    @Test
    fun `split canvas rekeys and manages selection`() {
        assertTrue(".pointerInput(aspect, perspMode, splitMode, splitBoxes.size, boxSize.width, boxSize.height)" in studio)
        assertTrue("var selectedBox by remember { mutableStateOf(0) }" in studio)
        assertTrue("private var dragSplitIndex by androidx.compose.runtime.mutableStateOf(-1)" in studio)
        // حذف کادر فقط وقتی بیش از یک کادر هست
        assertTrue("if (splitBoxes.size > 1) {" in studio)
    }
}
