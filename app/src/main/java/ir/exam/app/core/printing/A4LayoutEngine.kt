package ir.exam.app.core.printing
import ir.exam.app.domain.model.*
/** یک engine مشترک برای preview و PDF؛ تفاوت نمایش و چاپ را حذف می‌کند. */
class A4LayoutEngine(private val marginMm:Float=10f){
 private val usableHeight=297f-marginMm*2
 fun paginate(blocks:List<PrintBlock>):PrintDocument{val pages=mutableListOf<A4Page>();var current=mutableListOf<PrintBlock>();var used=0f;var n=1
  blocks.forEach{b->if(used+b.estimatedHeightMm>usableHeight&&current.isNotEmpty()){pages+=A4Page(n++,current);current=mutableListOf();used=0f};current+=b;used+=b.estimatedHeightMm};if(current.isNotEmpty())pages+=A4Page(n,current);return PrintDocument(pages)
 }
}
