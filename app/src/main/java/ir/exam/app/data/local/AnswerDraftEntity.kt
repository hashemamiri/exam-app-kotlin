package ir.exam.app.data.local
import androidx.room.Entity
import androidx.room.PrimaryKey
/** یک رکورد برای کل پاسخ‌های موقت هر آزمون؛ JSON فقط در لایهٔ local نگهداری می‌شود. */
@Entity(tableName="answer_drafts")
data class AnswerDraftEntity(@PrimaryKey val examId:String,val answersJson:String,val updatedAt:Long)
