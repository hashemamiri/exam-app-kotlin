package ir.exam.app.ui.student
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ir.exam.app.domain.model.*
/** UI مرحلهٔ اول آزمون؛ هر نوع سؤال در مرحله‌های بعد Composable مستقل می‌گیرد. */
@Composable fun StudentExamContent(state:StudentExamUiState,onAnswer:(StudentAnswer)->Unit,onNext:()->Unit,onSubmit:()->Unit){
 val exam=state.exam ?: return
 val q=exam.questions.getOrNull(state.questionIndex) ?: return
 Scaffold(bottomBar={Row(Modifier.fillMaxWidth().padding(12.dp),horizontalArrangement=Arrangement.SpaceBetween){Text("زمان: ${state.remainingSeconds/60}:${(state.remainingSeconds%60).toString().padStart(2,'0')}");Button(onClick=onSubmit,enabled=!state.submitting){Text("ارسال نهایی")}}}){p->Column(Modifier.padding(p).padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text(exam.title,style=MaterialTheme.typography.titleLarge);Text("سؤال ${state.questionIndex+1} از ${exam.questions.size}");Text(q.text);when(q){is EssayQuestion->OutlinedTextField(value=(state.answers[q.id] as? TextAnswer)?.value.orEmpty(),onValueChange={onAnswer(TextAnswer(q.id,it))},label={Text("پاسخ شما")});is MultipleChoiceQuestion->q.options.forEachIndexed{i,o->Row{RadioButton(selected=(state.answers[q.id] as? ChoiceAnswer)?.selectedIndex==i,onClick={onAnswer(ChoiceAnswer(q.id,i))});Text(o)}};else->Text("رابط اختصاصی این نوع سؤال در مرحلهٔ آزمون‌ساز تکمیل می‌شود.")};Button(onClick=onNext){Text("سؤال بعدی")}}}
}
