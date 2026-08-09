package ir.exam.app.data.repository
import ir.exam.app.data.local.AnswerDraftDao
import ir.exam.app.data.local.AnswerDraftEntity
import ir.exam.app.domain.model.*
import ir.exam.app.domain.repository.AnswerDraftRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
/** نگهداری آفلاین پاسخ‌ها؛ تبدیل JSON در یک نقطه تا UI با Room درگیر نشود. */
class RoomAnswerDraftRepository(private val dao:AnswerDraftDao):AnswerDraftRepository{
 override fun observe(examId:String):Flow<Map<String,StudentAnswer>> = dao.observe(examId).map{it?.answersJson?.let(::decode).orEmpty()}
 override suspend fun save(examId:String,answers:Map<String,StudentAnswer>)=dao.upsert(AnswerDraftEntity(examId,encode(answers),System.currentTimeMillis()))
 override suspend fun clear(examId:String)=dao.delete(examId)
 private fun encode(a:Map<String,StudentAnswer>)=JSONObject().apply{a.forEach{(id,v)->put(id,when(v){is TextAnswer->JSONObject().put("type","text").put("value",v.value);is ChoiceAnswer->JSONObject().put("type","choice").put("value",v.selectedIndex);is BooleanAnswer->JSONObject().put("type","boolean").put("value",v.value)})}}.toString()
 private fun decode(raw:String):Map<String,StudentAnswer>{val root=JSONObject(raw);return root.keys().asSequence().associateWith{id->val x=root.getJSONObject(id);when(x.getString("type")){"text"->TextAnswer(id,x.getString("value"));"choice"->ChoiceAnswer(id,x.getInt("value"));else->BooleanAnswer(id,x.getBoolean("value"))}}}
}
