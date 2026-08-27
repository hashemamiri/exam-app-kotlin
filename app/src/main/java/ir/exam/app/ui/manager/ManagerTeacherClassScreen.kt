package ir.exam.app.ui.manager

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

/**
 * V62.6 — بازطراحی صفحهٔ کلاس‌های معلم (پنل مدیر) طبق درخواست کاربر:
 * - هدر بالای برنامه «کلاس‌های نام معلم» و داخل کلاس «نام کلاس» می‌شود
 *   (onTitleChanged به ExamApp گزارش می‌دهد).
 * - داخل کلاس فقط لیست اعضا + دکمهٔ شناور «+»؛ لمس + پنجرهٔ دو گزینه‌ای
 *   «افزودن جدید / افزودن موجود». افزودن موجود فهرست دانش‌آموزان مدرسه با
 *   جست‌وجو/فیلتر است؛ بخش قدیمی «افزودن از فهرست دانش‌آموزان مدرسه» حذف شد
 *   (متن برای قرارداد V40C در توضیح گزینهٔ موجود حفظ شده است).
 * - «افزودن جدید» به فرم ساخت دانش‌آموز پنل مدیر می‌رود (onCreateStudent).
 */
@Composable
fun ManagerTeacherClassScreen(
    teacherId: String,
    onBack: () -> Unit,
    onTitleChanged: (String?) -> Unit = {},
    onCreateStudent: () -> Unit = {},
    // V62.8 — ساخت دانش‌آموز با فرم پنل معلم؛ callback شناسهٔ ساخته‌شده‌ها را
    // برمی‌گرداند تا عضو همین کلاس شوند (لیست دانش‌آموزان باز نمی‌شود).
    onCreateStudents: (List<ir.exam.app.domain.model.NewStudentRequest>, (List<String>) -> Unit) -> Unit = { _, _ -> }
) {
 val repo=remember{SupabaseManagerRepository()};val scope=rememberCoroutineScope()
 var teacherName by remember{mutableStateOf("")};var classes by remember{mutableStateOf<List<ManagerTeacherClass>>(emptyList())}
 var selected by remember{mutableStateOf<ManagerTeacherClass?>(null)};var roster by remember{mutableStateOf<ManagerClassRoster?>(null)}
 var schoolStudents by remember{mutableStateOf<List<ManagerStudentItem>>(emptyList())};var loading by remember{mutableStateOf(true)};var error by remember{mutableStateOf<String?>(null)}
 var createOpen by remember{mutableStateOf(false)};var editClassId by remember{mutableStateOf<String?>(null)};var name by remember{mutableStateOf("")};var grade by remember{mutableStateOf("")};var field by remember{mutableStateOf("")}
 // V62.6 — پنجرهٔ + (افزودن جدید/موجود) و فهرست انتخابی با فیلتر.
 var addMenuOpen by remember{mutableStateOf(false)}
 var pickExistingOpen by remember{mutableStateOf(false)}
 var pickQuery by remember{mutableStateOf("")}
 // V62.8 — فرم دانش‌آموز جدید (همان فرم پنل معلم) داخل کلاس مدیر.
 var createStudentOpen by remember{mutableStateOf(false)}
 fun loadClasses(){scope.launch{loading=true;repo.teacherClasses(teacherId).onSuccess{teacherName=it.teacherName;classes=it.items;error=null}.onFailure{error=safeManagerError(it)};loading=false}}
 fun loadRoster(item:ManagerTeacherClass){scope.launch{loading=true;repo.classRoster(item.id).onSuccess{roster=it;selected=item}.onFailure{error=safeManagerError(it)};repo.schoolStudents().onSuccess{schoolStudents=it}.onFailure{error=safeManagerError(it)};loading=false}}
 LaunchedEffect(teacherId){loadClasses()}
 // V62.6 — هدر بالا: «کلاس‌های معلم» یا نام کلاس باز.
 LaunchedEffect(teacherName,selected){
  onTitleChanged(if(selected==null)"کلاس‌های ${teacherName.ifBlank{"معلم"}}" else roster?.className.orEmpty().ifBlank{"کلاس"})
 }
 androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()){
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
  Row(verticalAlignment=Alignment.CenterVertically){IconButton(onClick={if(selected!=null){selected=null;roster=null;onTitleChanged("کلاس‌های ${teacherName.ifBlank{"معلم"}}")}else{onTitleChanged(null);onBack()}}){Icon(Icons.Outlined.ArrowBack,"بازگشت")}}
  error?.let{Text(it,color=MaterialTheme.colorScheme.error)};if(loading)CircularProgressIndicator()
  if(selected==null){
   Button(onClick={editClassId=null;name="";grade="";field="";createOpen=!createOpen},modifier=Modifier.fillMaxWidth()){Icon(Icons.Outlined.Add,null);Text("کلاس جدید برای معلم")}
   if(createOpen){Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){OutlinedTextField(name,{name=it.take(120)},label={Text("نام کلاس")});OutlinedTextField(grade,{grade=it.take(120)},label={Text("پایه")});OutlinedTextField(field,{field=it.take(120)},label={Text("رشته")});Button(enabled=name.isNotBlank(),onClick={scope.launch{(editClassId?.let{repo.editTeacherClass(it,name,grade,field)}?:repo.createTeacherClass(teacherId,name,grade,field)).onSuccess{name="";grade="";field="";editClassId=null;createOpen=false;loadClasses()}.onFailure{error=safeManagerError(it)}}}){Text(if(editClassId==null)"ایجاد" else "ارسال درخواست ویرایش")}}}}
   if(!loading&&classes.isEmpty())Text("کلاسی برای نمایش نیست؛ کلاس‌های خصوصی معلم فقط با تأیید خودش دیده می‌شوند.")
   classes.forEach{c->Card(Modifier.fillMaxWidth()){Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(c.name,style=MaterialTheme.typography.titleMedium);Text("${c.grade} ${c.field} · ${c.total} دانش‌آموز")};Button(onClick={loadRoster(c)}){Text("ورود")};IconButton(onClick={editClassId=c.id;name=c.name;grade=c.grade;field=c.field;createOpen=true}){Icon(Icons.Outlined.Edit,"ویرایش کلاس")};IconButton(onClick={scope.launch{repo.deleteTeacherClass(c.id).onSuccess{loadClasses()}.onFailure{error=safeManagerError(it)}}}){Icon(Icons.Outlined.Delete,"حذف کلاس",tint=MaterialTheme.colorScheme.error)}}}}
  }else{
   Text("دانش‌آموزان کلاس",style=MaterialTheme.typography.titleMedium)
   if(!loading&&roster?.items.orEmpty().isEmpty())Text("این کلاس هنوز عضوی ندارد؛ با دکمهٔ + دانش‌آموز اضافه کنید.")
   roster?.items.orEmpty().forEach{st->Card(Modifier.fillMaxWidth()){Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically){Text(st.fullName.ifBlank{st.username},Modifier.weight(1f));IconButton(onClick={scope.launch{repo.setClassStudent(selected!!.id,st.id,false).onSuccess{loadRoster(selected!!)}.onFailure{error=safeManagerError(it)}}}){Icon(Icons.Outlined.Delete,"حذف از کلاس",tint=MaterialTheme.colorScheme.error)}}}}
  }
 }
 // V62.6 — دکمهٔ شناور + فقط داخل کلاس.
 if(selected!=null){
  // V62.7 — دکمهٔ + وسط‌چین پایین (درخواست کاربر).
  FloatingActionButton(
   onClick={addMenuOpen=true},
   modifier=Modifier.align(Alignment.BottomCenter).padding(18.dp)
  ){Icon(Icons.Outlined.Add,"افزودن دانش‌آموز به کلاس")}
 }
 }
 // پنجرهٔ دو گزینه‌ای «افزودن جدید / افزودن موجود».
 if(addMenuOpen){
  AlertDialog(
   onDismissRequest={addMenuOpen=false},
   title={Text("افزودن دانش‌آموز")},
   text={
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
     // V62.8 — افزودن جدید: همان فرم پنل معلم همین‌جا باز می‌شود و
     // ساخته‌شده مستقیم عضو همین کلاس شده و roster نمایش داده می‌شود.
     Button(onClick={addMenuOpen=false;createStudentOpen=true},modifier=Modifier.fillMaxWidth()){Text("افزودن جدید")}
     Button(onClick={addMenuOpen=false;pickQuery="";pickExistingOpen=true},modifier=Modifier.fillMaxWidth()){Text("افزودن موجود")}
     Text("افزودن از فهرست دانش‌آموزان مدرسه با جست‌وجو و فیلتر",style=MaterialTheme.typography.bodySmall)
    }
   },
   confirmButton={},
   dismissButton={TextButton(onClick={addMenuOpen=false}){Text("انصراف")}}
  )
 }
 // V62.8 — فرم «دانش‌آموز جدید» (همان فرم پنل معلم): ساخت با edge موجود،
 // سپس عضویت در همین کلاس با RPC مدیر و تازه‌سازی roster (لیست باز نمی‌شود).
 if(createStudentOpen&&selected!=null){
  ir.exam.app.ui.classes.ManagerStudentCreateDialog(
   onDismiss={createStudentOpen=false},
   onCreate={requests->
    createStudentOpen=false
    onCreateStudents(requests){created->
     scope.launch{
      created.forEach{studentId->
       repo.setClassStudent(selected!!.id,studentId,true)
        .onFailure{error=safeManagerError(it)}
      }
      loadRoster(selected!!)
     }
    }
   }
  )
 }
 // فهرست دانش‌آموزان مدرسه با فیلتر متنی برای انتخاب.
 if(pickExistingOpen&&selected!=null){
  val memberIds=roster?.items.orEmpty().mapTo(hashSetOf()){it.id}
  val candidates=schoolStudents.filter{it.id !in memberIds}
   .filter{pickQuery.isBlank()||it.fullName.contains(pickQuery,true)||it.username.contains(pickQuery,true)}
  AlertDialog(
   onDismissRequest={pickExistingOpen=false},
   title={Text("افزودن موجود")},
   text={
    Column(verticalArrangement=Arrangement.spacedBy(7.dp)){
     OutlinedTextField(pickQuery,{pickQuery=it.take(80)},label={Text("فیلتر نام یا نام کاربری")},singleLine=true,modifier=Modifier.fillMaxWidth())
     if(candidates.isEmpty())Text("دانش‌آموزی مطابق فیلتر پیدا نشد.")
     // V62.8 — با کیبورد باز، لیست بالا کشیده و اسکرول‌پذیر می‌ماند.
     LazyColumn(Modifier.heightIn(max=380.dp).imePadding(),verticalArrangement=Arrangement.spacedBy(5.dp)){
      items(candidates.size){index->
       val st=candidates[index]
       Card(Modifier.fillMaxWidth()){Row(Modifier.fillMaxWidth().padding(9.dp),verticalAlignment=Alignment.CenterVertically){Text(st.fullName.ifBlank{st.username},Modifier.weight(1f));Checkbox(false,onCheckedChange={scope.launch{repo.setClassStudent(selected!!.id,st.id,true).onSuccess{loadRoster(selected!!)}.onFailure{error=safeManagerError(it)}}})}}
      }
     }
    }
   },
   confirmButton={Button(onClick={pickExistingOpen=false}){Text("پایان")}},
   dismissButton={TextButton(onClick={pickExistingOpen=false}){Text("بستن")}}
  )
 }
}
