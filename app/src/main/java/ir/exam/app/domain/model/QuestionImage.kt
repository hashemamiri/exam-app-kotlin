package ir.exam.app.domain.model
import android.net.Uri
import java.util.UUID
/** تصویر سؤال؛ مختصات میلی‌متری باعث ثبات مکان روی بوم A4 می‌شود. */
data class QuestionImage(
 val id:String=UUID.randomUUID().toString(), val localUri:Uri?=null, val remotePath:String?=null,
 val isEdited:Boolean=false, val xMm:Float=20f, val yMm:Float=30f, val widthMm:Float=70f, val zIndex:Int=0
)
data class CropRect(val left:Float,val top:Float,val width:Float,val height:Float)
data class ImageEditRequest(val source:Uri,val crop:CropRect?=null,val rotationDegrees:Int=0,val forceSquare:Boolean=false)
data class PreparedImage(val uri:Uri,val wasEdited:Boolean,val wasCompressed:Boolean)
