package ir.exam.app.ui.image

import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ir.exam.app.data.repository.LocalImageRepository
import ir.exam.app.domain.model.CropRect
import ir.exam.app.domain.model.ImageEditRequest
import kotlinx.coroutines.launch

/** برش تعاملی نرمال‌شده، چرخش دستی و خروجی واقعی bitmap در sandbox. */
@Composable
fun InteractiveImageEditorDialog(
    source:Uri,
    forceSquare:Boolean=false,
    onDismiss:()->Unit,
    onDone:(Uri)->Unit
){
    val context=LocalContext.current
    val repository=remember(context){LocalImageRepository(context)}
    val scope=rememberCoroutineScope()
    var rotation by remember(source){mutableIntStateOf(0)}
    var left by remember(source){mutableFloatStateOf(0f)}
    var top by remember(source){mutableFloatStateOf(0f)}
    var width by remember(source){mutableFloatStateOf(1f)}
    var height by remember(source){mutableFloatStateOf(1f)}
    var busy by remember{mutableStateOf(false)}
    var error by remember{mutableStateOf<String?>(null)}
    fun aspect(mode:String){when(mode){"square"->{width=.8f;height=.8f;left=.1f;top=.1f};"4:3"->{width=.9f;height=.675f;left=.05f;top=.16f};"full"->{left=0f;top=0f;width=1f;height=1f}}}
    AlertDialog(onDismissRequest={if(!busy)onDismiss()},title={Text("برش و چرخش تصویر")},text={
        Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
            BoxWithConstraints(Modifier.fillMaxWidth().height(260.dp).border(1.dp,Color.Gray).clipToBounds()){
                AsyncImage(source,"پیش‌نمایش تصویر",Modifier.fillMaxWidth().height(260.dp).graphicsLayer(rotationZ=rotation.toFloat()))
                Box(Modifier.offset(maxWidth*left,maxHeight*top).size(maxWidth*width,maxHeight*height).border(2.dp,Color.Red))
            }
            Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){
                FilterChip(selected=false,onClick={rotation=(rotation+270)%360},label={Text("↶ ۹۰°")})
                FilterChip(selected=false,onClick={rotation=(rotation+90)%360},label={Text("↷ ۹۰°")})
                FilterChip(selected=width==1f&&height==1f,onClick={aspect("full")},label={Text("کامل")})
                FilterChip(selected=width==height&&width<1f,onClick={aspect("square")},label={Text("مربع")})
                FilterChip(selected=false,onClick={aspect("4:3")},label={Text("۴:۳")})
            }
            Text("شروع افقی ${(left*100).toInt()}٪");Slider(left,{left=it.coerceAtMost(1f-width)},valueRange=0f..1f)
            Text("شروع عمودی ${(top*100).toInt()}٪");Slider(top,{top=it.coerceAtMost(1f-height)},valueRange=0f..1f)
            Text("عرض ${(width*100).toInt()}٪");Slider(width,{width=it.coerceIn(.1f,1f-left)},valueRange=.1f..1f)
            Text("ارتفاع ${(height*100).toInt()}٪");Slider(height,{height=it.coerceIn(.1f,1f-top)},valueRange=.1f..1f)
            error?.let{Text(it,color=Color.Red)}
            if(busy)CircularProgressIndicator()
        }
    },confirmButton={Button(enabled=!busy,onClick={
        busy=true;error=null;scope.launch{
            val side=if(forceSquare)minOf(width,height) else 0f
            repository.prepare(ImageEditRequest(source,if(forceSquare)CropRect(left,top,side,side)else CropRect(left,top,width,height),rotation,forceSquare))
                .onSuccess{onDone(it.uri)}.onFailure{error=it.message;busy=false}
        }
    }){Text("اعمال و ذخیره")}},dismissButton={TextButton(enabled=!busy,onClick=onDismiss){Text("بدون ویرایش / انصراف")}})
}
