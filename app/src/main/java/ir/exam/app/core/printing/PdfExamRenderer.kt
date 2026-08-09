package ir.exam.app.core.printing
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.OutputStream
import ir.exam.app.domain.model.PrintDocument
/** رندر پایهٔ PDF؛ در توسعهٔ بعدی متن فارسی و تصویرها با Typeface منابع برنامه کامل می‌شوند. */
class PdfExamRenderer {
 fun write(document:PrintDocument, output:OutputStream){val pdf=PdfDocument();val dpi=300;val w=(210/25.4*dpi).toInt();val h=(297/25.4*dpi).toInt();val p=Paint(Paint.ANTI_ALIAS_FLAG).apply{textSize=28f}
  document.pages.forEach{pageModel->val page=pdf.startPage(PdfDocument.PageInfo.Builder(w,h,pageModel.number).create());val c=page.canvas;c.drawText("سامانه آزمون — صفحه ${pageModel.number}",80f,80f,p);var y=140f;pageModel.blocks.forEach{block->c.drawRect(70f,y,w-70f,y+90f,p.apply{style=Paint.Style.STROKE});c.drawText("سؤال",90f,y+45f,p.apply{style=Paint.Style.FILL});y+=100f};pdf.finishPage(page)};pdf.writeTo(output);pdf.close() }
}
