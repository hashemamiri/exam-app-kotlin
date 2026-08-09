package ir.exam.app.core.image
import ir.exam.app.domain.model.QuestionImage
/** تبدیل مختصات لمس به میلی‌متر؛ مستقل از اندازهٔ صفحه و تعداد سطر متن. */
object A4ImagePositioner {
 const val PAGE_WIDTH_MM=210f; const val PAGE_HEIGHT_MM=297f
 fun move(image:QuestionImage,deltaXpx:Float,deltaYpx:Float,pxPerMm:Float):QuestionImage=image.copy(
  xMm=(image.xMm+deltaXpx/pxPerMm).coerceIn(0f,PAGE_WIDTH_MM-image.widthMm),
  yMm=(image.yMm+deltaYpx/pxPerMm).coerceIn(0f,PAGE_HEIGHT_MM)
 )
}
