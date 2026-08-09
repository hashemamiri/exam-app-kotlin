package ir.exam.app.ui.image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import ir.exam.app.core.image.A4ImagePositioner
import ir.exam.app.domain.model.QuestionImage
/** بوم آزاد: هر gesture فقط همان image.id را تغییر می‌دهد؛ تصویرهای دیگر حرکت نمی‌کنند. */
@Composable fun FreeImageDragModifier(image:QuestionImage,pxPerMm:Float,onMoved:(QuestionImage)->Unit):Modifier=Modifier.pointerInput(image.id){
 detectDragGestures{ change,drag->change.consume();onMoved(A4ImagePositioner.move(image,drag.x,drag.y,pxPerMm)) }
}
