package ir.exam.app.ui.figure

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.CachePolicy
import coil.request.ImageRequest
import ir.exam.app.core.figure.FigureSpec
import ir.exam.app.core.figure.FigureSvgRenderer

/** نمایش برداری یک شکل/نمودار به‌صورت SVG مستقل، بدون WebView. */
@Composable
fun InlineFigureView(
    spec: FigureSpec,
    modifier: Modifier = Modifier,
    contentDescription: String = "شکل"
) {
    val context = LocalContext.current
    val document = remember(spec) { FigureSvgRenderer.render(spec) }
    val request = remember(context, document.cacheKey) {
        ImageRequest.Builder(context)
            .data(document.xml.toByteArray(Charsets.UTF_8))
            .decoderFactory(SvgDecoder.Factory())
            .memoryCacheKey(document.cacheKey)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.DISABLED)
            .networkCachePolicy(CachePolicy.DISABLED)
            .crossfade(false)
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}
