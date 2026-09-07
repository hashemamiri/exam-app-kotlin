package ir.exam.app.ui.builder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import ir.exam.app.core.ui.persianFontFamily
import ir.exam.app.ui.math.NativeMathText
import kotlin.math.roundToInt

@Composable
fun QuestionPrintPreviewDialog(question: QuestionDraft,onDismiss:()->Unit) {
    AlertDialog(onDismissRequest=onDismiss,title={Text("پیش‌نمایش سؤال")},
        text={Column(Modifier.fillMaxWidth().heightIn(max=600.dp).background(Color.White).padding(12.dp)) { QuestionPreview(question,1) }},
        confirmButton={Button(onClick=onDismiss){Text("بستن")}})
}

@Composable
fun ExamPrintPreviewDialog(state: ExamBuilderState,onDismiss:()->Unit) {
    AlertDialog(onDismissRequest=onDismiss,title={Text("پیش‌نمایش A4")},
        text={LazyColumn(Modifier.fillMaxWidth().heightIn(max=650.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            item { Column(Modifier.fillMaxWidth().aspectRatio(210f/297f).background(Color.White).border(1.dp,Color.Gray).padding(14.dp)) {
                Text(state.title.ifBlank{"آزمون"},color=Color.Black,fontWeight=FontWeight.Bold,modifier=Modifier.align(Alignment.CenterHorizontally))
                Text("درس: ${state.subject}   مدت: ${state.durationMinutes.ifBlank{"—"}} دقیقه",color=Color.Black)
                state.opensAtIso?.let{Text("بازشدن: ${jalaliDisplay(it)}",color=Color.DarkGray)}
                state.closesAtIso?.let{Text("پایان: ${jalaliDisplay(it)}",color=Color.DarkGray)}
            } }
            itemsIndexed(state.questions,key={_,q->q.id}) { i,q -> Column(Modifier.fillMaxWidth().background(Color.White).border(1.dp,Color.LightGray).padding(10.dp)) { QuestionPreview(q,i+1) } }
        }},confirmButton={Button(onClick=onDismiss){Text("بستن")}})
}

@Composable
private fun QuestionPreview(q:QuestionDraft,number:Int) {
    val align=when(q.textAlign){"center"->TextAlign.Center;"left"->TextAlign.Left;"justify"->TextAlign.Justify;else->TextAlign.Right}
    val images:@Composable ()->Unit = {
        if(q.imagePosition=="free") Box(Modifier.fillMaxWidth().heightIn(min=220.dp).clipToBounds()) {
            q.images.forEach { image -> AsyncImage(image.uri,"تصویر",Modifier.offset{IntOffset((image.xMm*1.5f).roundToInt(),(image.yMm*1.5f).roundToInt())}.size((image.widthMm*1.5f).dp)) }
        } else q.images.forEach { image -> AsyncImage(image.uri,"تصویر",Modifier.fillMaxWidth().heightIn(max=260.dp)) }
    }
    val text:@Composable ()->Unit = {
        NativeMathText(
            "$number. ${q.text}",modifier=Modifier.fillMaxWidth(),fontSize=q.fontSizeSp.sp,
            fontWeight=if(q.bold)FontWeight.Bold else FontWeight.Normal,
            fontStyle=if(q.italic)FontStyle.Italic else FontStyle.Normal,
            fontFamily=persianFontFamily(q.fontFamily),textAlign=align,color=Color.Black
        )
    }
    when(q.imagePosition){
        "above"->{images();text()}
        "right"->Row { Box(Modifier.weight(.4f)){images()};Box(Modifier.weight(.6f)){text()} }
        "left"->Row { Box(Modifier.weight(.6f)){text()};Box(Modifier.weight(.4f)){images()} }
        else->{text();images()}
    }
    if(q.type==QuestionType.MULTIPLE_CHOICE) q.options.forEachIndexed { i,o -> NativeMathText("${i+1}) $o",color=Color.Black,fontFamily=persianFontFamily(q.fontFamily)) }
    repeat(q.answerLines) {
        Text(if(q.answerLineStyle=="lined") "................................................................................................" else " ",color=Color.DarkGray,modifier=Modifier.fillMaxWidth())
    }
}
