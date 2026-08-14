package ir.exam.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import ir.exam.app.domain.model.CropRect
import ir.exam.app.domain.model.ImageEditRequest
import ir.exam.app.domain.model.PreparedImage
import ir.exam.app.domain.repository.ImageRepository
import java.io.File
import java.io.FileOutputStream
import java.io.FileInputStream
import java.io.InputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** ویرایش واقعی bitmap در فضای خصوصی برنامه؛ فایل خروجی تنها یک بار آپلود می‌شود. */
class LocalImageRepository(context: Context) : ImageRepository {
    private val appContext=context.applicationContext
    override suspend fun prepare(request: ImageEditRequest): Result<PreparedImage> = runCatching {
        withContext(Dispatchers.IO) {
            cleanupOldFiles()
            val source = decodeSampled(request.source)
            val exifRotation=runCatching { open(request.source)?.use { input ->
                when(ExifInterface(input).getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90->90
                    ExifInterface.ORIENTATION_ROTATE_180->180
                    ExifInterface.ORIENTATION_ROTATE_270->270
                    else->0
                }
            }?:0 }.getOrDefault(0)
            val rotated=rotate(source,(exifRotation+request.rotationDegrees)%360)
            val crop=request.crop ?: if(request.forceSquare) centerSquare(rotated) else CropRect(0f,0f,1f,1f)
            val left=(crop.left.coerceIn(0f,.95f)*rotated.width).toInt()
            val top=(crop.top.coerceIn(0f,.95f)*rotated.height).toInt()
            val width=(crop.width.coerceIn(.05f,1f-crop.left.coerceIn(0f,.95f))*rotated.width).toInt().coerceAtLeast(1)
            val height=(crop.height.coerceIn(.05f,1f-crop.top.coerceIn(0f,.95f))*rotated.height).toInt().coerceAtLeast(1)
            val cropped=Bitmap.createBitmap(rotated,left,top,width.coerceAtMost(rotated.width-left),height.coerceAtMost(rotated.height-top))
            if(cropped!==rotated)rotated.recycle()
            val largest=maxOf(cropped.width,cropped.height)
            val finalBitmap=if(largest>2200){val scale=2200f/largest;Bitmap.createScaledBitmap(cropped,(cropped.width*scale).toInt(),(cropped.height*scale).toInt(),true).also{if(it!==cropped)cropped.recycle()}}else cropped
            val dir=File(appContext.filesDir,"edited-images").apply{mkdirs()}
            val file=File(dir,"${UUID.randomUUID()}.jpg")
            FileOutputStream(file).use { out -> check(finalBitmap.compress(Bitmap.CompressFormat.JPEG,92,out)){"ذخیره تصویر ناموفق بود."} }
            finalBitmap.recycle()
            PreparedImage(Uri.fromFile(file),wasEdited=request.crop!=null||request.rotationDegrees!=0||request.forceSquare,wasCompressed=true)
        }
    }

    override suspend fun upload(image: PreparedImage, folder: String): Result<String> =
        Result.failure(UnsupportedOperationException("آپلود توسط repository مالک رسانه انجام می‌شود."))

    private fun decodeSampled(uri: Uri): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = open(uri) ?: error("تصویر قابل خواندن نیست.")
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "ابعاد تصویر نامعتبر است." }
        var sample = 1
        while (
            bounds.outWidth / sample > MAX_DECODE_EDGE ||
            bounds.outHeight / sample > MAX_DECODE_EDGE ||
            bounds.outWidth.toLong() / sample * (bounds.outHeight.toLong() / sample) > MAX_DECODE_PIXELS
        ) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return try {
            open(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                ?: error("تصویر قابل خواندن نیست.")
        } catch (_: OutOfMemoryError) {
            error("حافظه دستگاه برای این تصویر کافی نیست؛ تصویر کوچک‌تری انتخاب کنید.")
        }
    }

    private fun open(uri:Uri):InputStream?=if(uri.scheme.equals("file",true))uri.path?.let(::File)?.takeIf(File::isFile)?.let(::FileInputStream)else appContext.contentResolver.openInputStream(uri)
    private fun rotate(bitmap:Bitmap,degrees:Int):Bitmap=if(degrees%360==0)bitmap else Bitmap.createBitmap(bitmap,0,0,bitmap.width,bitmap.height,Matrix().apply{postRotate(degrees.toFloat())},true).also{if(it!==bitmap)bitmap.recycle()}
    private fun centerSquare(bitmap:Bitmap):CropRect=if(bitmap.width>bitmap.height)CropRect((bitmap.width-bitmap.height).toFloat()/bitmap.width/2f,0f,bitmap.height.toFloat()/bitmap.width,1f)else CropRect(0f,(bitmap.height-bitmap.width).toFloat()/bitmap.height/2f,1f,bitmap.width.toFloat()/bitmap.height)
    private fun cleanupOldFiles(){File(appContext.filesDir,"edited-images").listFiles()?.filter{System.currentTimeMillis()-it.lastModified()>14L*24*60*60*1000}?.forEach(File::delete)}

    private companion object {
        const val MAX_DECODE_EDGE = 2_600
        const val MAX_DECODE_PIXELS = 7_000_000L
    }
}
