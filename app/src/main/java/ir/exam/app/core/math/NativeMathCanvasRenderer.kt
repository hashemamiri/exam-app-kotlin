package ir.exam.app.core.math

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import kotlin.math.max

data class MathCanvasSize(val width:Float,val height:Float)

/** همان AST فرمول را مستقیماً روی Canvas/PdfDocument به‌صورت دوبعدی رسم می‌کند. */
class NativeMathCanvasRenderer {
 fun measure(node:MathNode,size:Float):MathCanvasSize=when(node){
  is MathNode.Symbol->{val p=paint(size,node.bold);MathCanvasSize(p.measureText(node.value).coerceAtLeast(2f),size*1.35f)}
  is MathNode.Sequence->{val parts=node.children.map{measure(it,size)};MathCanvasSize(parts.sumOf{it.width.toDouble()}.toFloat(),parts.maxOfOrNull{it.height}?:size)}
  is MathNode.Fraction->{val a=measure(node.top,size*.78f);val b=measure(node.bottom,size*.78f);MathCanvasSize(max(a.width,b.width)+8f,a.height+b.height+5f)}
  is MathNode.Radical->{val b=measure(node.body,size);val idx=node.index?.let{measure(it,size*.5f).width}?:0f;MathCanvasSize(b.width+size*.8f+idx,b.height+4f)}
  is MathNode.Script->{val b=measure(node.base,size);val u=node.upper?.let{measure(it,size*.58f)};val l=node.lower?.let{measure(it,size*.58f)};MathCanvasSize(b.width+max(u?.width?:0f,l?.width?:0f),max(b.height,(u?.height?:0f)+(l?.height?:0f)))}
  is MathNode.Matrix->{val cols=node.rows.maxOfOrNull{it.size}?:0;val widths=(0 until cols).map{c->node.rows.maxOfOrNull{r->r.getOrNull(c)?.let{measure(it,size*.75f).width}?:0f}?:0f};MathCanvasSize(widths.sum()+cols*10f+size,node.rows.size*size*1.25f)}
  is MathNode.Accent->{val b=measure(node.body,size);MathCanvasSize(b.width,max(b.height,size*1.6f))}
 }
 fun draw(canvas:Canvas,node:MathNode,x:Float,top:Float,size:Float,color:Int=android.graphics.Color.BLACK):Float{when(node){
  is MathNode.Symbol->{val p=paint(size,node.bold,color);canvas.drawText(node.value,x,top+size,p);return measure(node,size).width}
  is MathNode.Sequence->{var dx=x;node.children.forEach{dx+=draw(canvas,it,dx,top,size,color)};return dx-x}
  is MathNode.Fraction->{val all=measure(node,size);val a=measure(node.top,size*.78f);val b=measure(node.bottom,size*.78f);draw(canvas,node.top,x+(all.width-a.width)/2,top,size*.78f,color);val lineY=top+a.height+1;canvas.drawLine(x,lineY,x+all.width,lineY,paint(1f,false,color));draw(canvas,node.bottom,x+(all.width-b.width)/2,lineY+2,size*.78f,color);return all.width}
  is MathNode.Radical->{val b=measure(node.body,size);val p=paint(size,false,color);val iw=node.index?.let{draw(canvas,it,x,top,size*.5f,color)}?:0f;canvas.drawText("√",x+iw,top+size,p);val bx=x+iw+size*.7f;canvas.drawLine(bx,top+2,bx+b.width,top+2,p);draw(canvas,node.body,bx,top,size,color);return b.width+size*.8f+iw}
  is MathNode.Script->{val b=measure(node.base,size);draw(canvas,node.base,x,top,size,color);node.upper?.let{draw(canvas,it,x+b.width,top,size*.58f,color)};node.lower?.let{draw(canvas,it,x+b.width,top+size*.72f,size*.58f,color)};return measure(node,size).width}
  is MathNode.Matrix->{val all=measure(node,size);canvas.drawText(node.delimiter.toString(),x,top+size,paint(size*1.4f,false,color));var yy=top;node.rows.forEach{row->var xx=x+size*.65f;row.forEach{cell->xx+=draw(canvas,cell,xx,yy,size*.75f,color)+10f};yy+=size*1.25f};val close=when(node.delimiter){'('->")";'{'->"";'|'->"|";else->"]"};canvas.drawText(close,x+all.width-size*.3f,top+size,paint(size*1.4f,false,color));return all.width}
  is MathNode.Accent->{val b=measure(node.body,size);canvas.drawText(node.mark,x+b.width/2-size*.2f,top+size*.45f,paint(size*.7f,false,color));draw(canvas,node.body,x,top+size*.35f,size,color);return b.width}
 }}
 private fun paint(size:Float,bold:Boolean,color:Int=android.graphics.Color.BLACK)=Paint(Paint.ANTI_ALIAS_FLAG).apply{this.textSize=size;this.color=color;typeface=Typeface.create("sans",if(bold)Typeface.BOLD else Typeface.NORMAL);strokeWidth=1f}
}
