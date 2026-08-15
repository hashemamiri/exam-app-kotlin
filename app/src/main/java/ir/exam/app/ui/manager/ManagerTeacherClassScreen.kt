package ir.exam.app.ui.manager

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ir.exam.app.data.repository.ManagerClassRoster
import ir.exam.app.data.repository.ManagerStudentItem
import ir.exam.app.data.repository.ManagerTeacherClass
import ir.exam.app.data.repository.SupabaseManagerRepository
import kotlinx.coroutines.launch

@Composable
fun ManagerTeacherClassScreen(teacherId:String,onBack:()->Unit){
 val repo=remember{SupabaseManagerRepository()};val scope=rememberCoroutineScope()
 var teacherName by remember{mutableStateOf("")};var classes by remember{mutableStateOf<List<ManagerTeacherClass>>(emptyList())}
 var selected by remember{mutableStateOf<ManagerTeacherClass?>(null)};var roster by remember{mutableStateOf<ManagerClassRoster?>(null)}
 var schoolStudents by remember{mutableStateOf<List<ManagerStudentItem>>(emptyList())};var loading by remember{mutableStateOf(true)};var error by remember{mutableStateOf<String?>(null)}
 var createOpen by remember{mutableStateOf(false)};var editClassId by remember{mutableStateOf<String?>(null)};var name by remember{mutableStateOf("")};var grade by remember{mutableStateOf("")};var field by remember{mutableStateOf("")}
 fun loadClasses(){scope.launch{loading=true;repo.teacherClasses(teacherId).onSuccess{teacherName=it.teacherName;classes=it.items;error=null}.onFailure{error=safeManagerError(it)};loading=false}}
 fun loadRoster(item:ManagerTeacherClass){scope.launch{loading=true;repo.classRoster(item.id).onSuccess{roster=it;selected=item}.onFailure{error=safeManagerError(it)};repo.schoolStudents().onSuccess{schoolStudents=it}.onFailure{error=safeManagerError(it)};loading=false}}
 LaunchedEffect(teacherId){loadClasses()}
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
  Row(verticalAlignment=Alignment.CenterVertically){IconButton(onClick={if(selected!=null){selected=null;roster=null}else onBack()}){Icon(Icons.Outlined.ArrowBack,"بازگشت")};Text(if(selected==null)"کلاس‌های $teacherName" else roster?.className.orEmpty(),style=MaterialTheme.typography.headlineSmall)}
  error?.let{Text(it,color=MaterialTheme.colorScheme.error)};if(loading)CircularProgressIndicator()
  if(selected==null){
   Button(onClick={editClassId=null;name="";grade="";field="";createOpen=!createOpen},modifier=Modifier.fillMaxWidth()){Icon(Icons.Outlined.Add,null);Text("کلاس جدید برای معلم")}
   if(createOpen){Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){OutlinedTextField(name,{name=it.take(120)},label={Text("نام کلاس")});OutlinedTextField(grade,{grade=it.take(120)},label={Text("پایه")});OutlinedTextField(field,{field=it.take(120)},label={Text("رشته")});Button(enabled=name.isNotBlank(),onClick={scope.launch{(editClassId?.let{repo.editTeacherClass(it,name,grade,field)}?:repo.createTeacherClass(teacherId,name,grade,field)).onSuccess{name="";grade="";field="";editClassId=null;createOpen=false;loadClasses()}.onFailure{error=safeManagerError(it)}}}){Text(if(editClassId==null)"ایجاد" else "ارسال درخواست ویرایش")}}}}
   classes.forEach{c->Card(Modifier.fillMaxWidth()){Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(c.name,style=MaterialTheme.typography.titleMedium);Text("${c.grade} ${c.field} · ${c.total} دانش‌آموز")};Button(onClick={loadRoster(c)}){Text("ورود")};IconButton(onClick={editClassId=c.id;name=c.name;grade=c.grade;field=c.field;createOpen=true}){Icon(Icons.Outlined.Edit,"ویرایش کلاس")};IconButton(onClick={scope.launch{repo.deleteTeacherClass(c.id).onSuccess{loadClasses()}.onFailure{error=safeManagerError(it)}}}){Icon(Icons.Outlined.Delete,"حذف کلاس",tint=MaterialTheme.colorScheme.error)}}}}
  }else{
   Text("دانش‌آموزان کلاس",style=MaterialTheme.typography.titleMedium)
   roster?.items.orEmpty().forEach{st->Card(Modifier.fillMaxWidth()){Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically){Text(st.fullName.ifBlank{st.username},Modifier.weight(1f));IconButton(onClick={scope.launch{repo.setClassStudent(selected!!.id,st.id,false).onSuccess{loadRoster(selected!!)}.onFailure{error=safeManagerError(it)}}}){Icon(Icons.Outlined.Delete,"حذف از کلاس",tint=MaterialTheme.colorScheme.error)}}}}
   Text("افزودن از فهرست دانش‌آموزان مدرسه",style=MaterialTheme.typography.titleMedium)
   val memberIds=roster?.items.orEmpty().mapTo(hashSetOf()){it.id}
   schoolStudents.forEach{st->if(st.id !in memberIds)Card(Modifier.fillMaxWidth()){Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically){Text(st.fullName.ifBlank{st.username},Modifier.weight(1f));Checkbox(false,onCheckedChange={scope.launch{repo.setClassStudent(selected!!.id,st.id,true).onSuccess{loadRoster(selected!!)}.onFailure{error=safeManagerError(it)}}})}}}
  }
 }
}
