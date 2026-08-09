package ir.exam.app.ui.builder
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
/** صفحهٔ Native ساخت آزمون؛ تصویر، قلم و چاپ در مرحله‌های ۶ و ۷ به همین کارت‌ها افزوده می‌شود. */
@Composable fun ExamBuilderScreen(vm:ExamBuilderViewModel){val s by vm.state.collectAsState();var typeMenu by remember{mutableStateOf(false)}
 Scaffold(topBar={TopAppBar(title={Text("ساخت آزمون")})},floatingActionButton={Box{FloatingActionButton(onClick={typeMenu=true}){Text("+")};DropdownMenu(expanded=typeMenu,onDismissRequest={typeMenu=false}){QuestionType.entries.forEach{t->DropdownMenuItem(text={Text(t.name)},onClick={vm.addQuestion(t);typeMenu=false})}}}}){p->LazyColumn(Modifier.padding(p).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){item{OutlinedTextField(s.title,vm::setTitle,label={Text("عنوان آزمون")},modifier=Modifier.fillMaxWidth());OutlinedTextField(s.subject,vm::setSubject,label={Text("درس")},modifier=Modifier.fillMaxWidth());OutlinedTextField(s.durationMinutes,vm::setDuration,label={Text("مدت (دقیقه)")},modifier=Modifier.fillMaxWidth())};items(s.questions,key={it.id}){q->QuestionEditor(q,vm)}}}}
@Composable private fun QuestionEditor(q:QuestionDraft,vm:ExamBuilderViewModel){Card{Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text("نوع: ${q.type}");OutlinedTextField(q.text,{vm.updateText(q.id,it)},label={Text("متن سؤال")},modifier=Modifier.fillMaxWidth());if(q.type==QuestionType.MULTIPLE_CHOICE)q.options.forEachIndexed{i,v->Row{RadioButton(selected=q.correctIndex==i,onClick={vm.setCorrect(q.id,i)});OutlinedTextField(v,{vm.updateOption(q.id,i,it)},label={Text("گزینه ${i+1}")})}};TextButton(onClick={vm.remove(q.id)}){Text("حذف سؤال")}}}
