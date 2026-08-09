package ir.exam.app.domain.model
enum class AppFont { VAZIR, VAZIRMATN, SHABNAM, SAMIM, PARASTOO, TANHA, NAZANIN, LALEZAR, MARKAZI, NASTALIQ }
data class A4Page(val number:Int,val blocks:List<PrintBlock>)
sealed interface PrintBlock { val estimatedHeightMm:Float }
data class QuestionPrintBlock(val questionId:String,val row:Int,val score:Double,val htmlFreeText:String,override val estimatedHeightMm:Float):PrintBlock
data class PrintDocument(val pages:List<A4Page>)
