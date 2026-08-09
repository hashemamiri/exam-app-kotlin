package ir.exam.app.ui.print
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ir.exam.app.domain.model.A4Page
/** پیش‌نمایش با نسبت واقعی A4؛ منبع layout همان A4LayoutEngine است. */
@Composable fun A4Preview(page:A4Page,modifier:Modifier=Modifier){Box(modifier.aspectRatio(210f/297f).background(Color.White).padding(12.dp)){Column{Text("برگه A4 — صفحه ${page.number}",color=Color.Black);page.blocks.forEach{Text("بلوک سؤال",color=Color.Black)}}}}
