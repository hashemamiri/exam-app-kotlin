package ir.exam.app.ui.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp

@Composable
fun NativeLineChart(values:List<Double>,modifier:Modifier=Modifier){
 val primary=MaterialTheme.colorScheme.primary;val grid=MaterialTheme.colorScheme.outlineVariant
 Canvas(modifier.fillMaxWidth().height(190.dp)){if(values.isEmpty())return@Canvas;repeat(5){i->val y=size.height*i/4;drawLine(grid,Offset(0f,y),Offset(size.width,y),1f)};val path=Path();values.forEachIndexed{i,v->val x=if(values.size==1)size.width/2 else size.width*i/(values.size-1);val y=size.height-(v.coerceIn(0.0,100.0)/100.0*size.height).toFloat();if(i==0)path.moveTo(x,y)else path.lineTo(x,y);drawCircle(primary,5f,Offset(x,y))};drawPath(path,primary,style=androidx.compose.ui.graphics.drawscope.Stroke(3f))}
}

@Composable
fun NativeBarChart(values:List<Pair<String,Double>>,modifier:Modifier=Modifier){
 val primary=MaterialTheme.colorScheme.tertiary;val max=(values.maxOfOrNull{it.second}?:1.0).coerceAtLeast(1.0)
 Canvas(modifier.fillMaxWidth().height(190.dp)){if(values.isEmpty())return@Canvas;val slot=size.width/values.size;values.forEachIndexed{i,p->val h=(p.second/max*size.height*.9).toFloat();drawRect(primary,topLeft=Offset(i*slot+slot*.15f,size.height-h),size=androidx.compose.ui.geometry.Size(slot*.7f,h))}}
}
