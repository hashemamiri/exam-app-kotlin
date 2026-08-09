package ir.exam.app.domain.model
data class GradeItem(val questionId:String,val earned:Double,val max:Double,val feedback:String?=null)
data class GradingResult(val answerId:String,val items:List<GradeItem>){val total:Double get()=items.sumOf{it.earned};val max:Double get()=items.sumOf{it.max}}
data class StudentReport(val studentId:String,val studentName:String,val result:GradingResult,val percent:Double)
