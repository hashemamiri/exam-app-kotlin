package ir.exam.app.ui.math

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.exam.app.core.math.MathNode
import ir.exam.app.core.math.NativeMathParser

@Composable
fun NativeFormulaView(tex:String,modifier:Modifier=Modifier,fontSize:TextUnit=20.sp,color:Color=LocalContentColor.current){
    Row(modifier.horizontalScroll(rememberScrollState()),verticalAlignment=Alignment.CenterVertically){MathNodeView(NativeMathParser.parse(tex),fontSize,color)}
}

@Composable
private fun MathNodeView(node:MathNode,size:TextUnit,color:Color){when(node){
    is MathNode.Symbol->Text(node.value,fontSize=size,color=color,fontWeight=if(node.bold)FontWeight.Bold else null)
    is MathNode.Sequence->Row(verticalAlignment=Alignment.CenterVertically){node.children.forEach{MathNodeView(it,size,color)}}
    is MathNode.Fraction->Column(horizontalAlignment=Alignment.CenterHorizontally){MathNodeView(node.top,size*.82f,color);Box(Modifier.border(0.7.dp,color).padding(horizontal=3.dp)){MathNodeView(node.bottom,size*.82f,color)}}
    is MathNode.Radical->Row(verticalAlignment=Alignment.CenterVertically){node.index?.let{MathNodeView(it,size*.52f,color)};Text("√",fontSize=size*1.25f,color=color);Box(Modifier.border(width=.7.dp,color=color).padding(2.dp)){MathNodeView(node.body,size,color)}}
    is MathNode.Script->Row(verticalAlignment=Alignment.CenterVertically){MathNodeView(node.base,size,color);Column{node.upper?.let{MathNodeView(it,size*.62f,color)};node.lower?.let{MathNodeView(it,size*.62f,color)}}}
    is MathNode.Matrix->Row(verticalAlignment=Alignment.CenterVertically){Text(node.delimiter.toString(),fontSize=size*1.4f,color=color);Column(verticalArrangement=Arrangement.spacedBy(2.dp)){node.rows.forEach{r->Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){r.forEach{MathNodeView(it,size*.8f,color)}}}};Text(when(node.delimiter){'('->")";'{'->"";'|'->"|";else->"]"},fontSize=size*1.4f,color=color)}
    is MathNode.Accent->Column(horizontalAlignment=Alignment.CenterHorizontally){Text(node.mark,fontSize=size*.7f,color=color);MathNodeView(node.body,size,color)}
}}

private operator fun TextUnit.times(value:Float):TextUnit=(this.value*value).sp
