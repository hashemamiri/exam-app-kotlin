package ir.exam.app.ui.builder

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * ستون معمولی که جابه‌جایی فرزندانش را دقیقاً با حس کارت سؤال (animateItem)
 * روان می‌کند: موقعیت قبلی هر آیتم به‌خاطر سپرده می‌شود و پس از هر تغییر ترتیب،
 * آیتم‌ها با فنر از جای قبلی به جای جدید سر می‌خورند.
 *
 * - [ids] باید به‌ازای هر آیتم یک شناسهٔ پایدار بدهد تا هویت وسط انیمیشن حفظ شود.
 */
@Composable
fun <T> AnimatedReorderColumn(
    items: List<T>,
    ids: List<String>,
    modifier: Modifier = Modifier,
    content: @Composable (T, Int) -> Unit
) {
    require(items.size == ids.size) { "تعداد شناسه‌ها باید برابر تعداد آیتم‌ها باشد." }
    val placedY = remember { mutableStateMapOf<String, Int>() }
    val anims = remember { mutableStateMapOf<String, Animatable<Float, AnimationVector1D>>() }
    val scope = rememberCoroutineScope()

    Column(modifier) {
        items.forEachIndexed { index, item ->
            val id = ids[index]
            val anim = anims.getOrPut(id) { Animatable(0f) }
            Box(
                Modifier
                    .graphicsLayer { translationY = anim.value }
                    .onGloballyPositioned { coords ->
                        val y = coords.positionInParent().y.roundToInt()
                        val previous = placedY[id]
                        placedY[id] = y
                        if (previous != null && previous != y) {
                            scope.launch {
                                anim.snapTo((previous - y).toFloat())
                                anim.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                )
                            }
                        }
                    }
            ) {
                key(id) { content(item, index) }
            }
        }
    }
}
