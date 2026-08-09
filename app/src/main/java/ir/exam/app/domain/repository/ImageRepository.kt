package ir.exam.app.domain.repository
import ir.exam.app.domain.model.ImageEditRequest
import ir.exam.app.domain.model.PreparedImage
/** قرارداد واحد: UI فقط یک بار prepare می‌کند، سپس همان خروجی یک بار upload می‌شود. */
interface ImageRepository { suspend fun prepare(request:ImageEditRequest):Result<PreparedImage>; suspend fun upload(image:PreparedImage,folder:String):Result<String> }
