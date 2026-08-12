package ir.exam.app.ui.classes

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import ir.exam.app.core.export.XlsxSheet
import ir.exam.app.core.export.XlsxWorkbook
import ir.exam.app.domain.model.NewStudentRequest
import ir.exam.app.domain.model.SchoolClass
import ir.exam.app.domain.model.StudentCredential
import ir.exam.app.domain.model.StudentProfile
import ir.exam.app.domain.model.UpdateStudentRequest
import kotlin.random.Random

@Composable
fun SchoolManagementScreen() {
    val context=LocalContext.current
    val viewModel=remember(context){ClassesViewModel(context=context.applicationContext)}
    val state by viewModel.state.collectAsState()
    var showStudents by remember { mutableStateOf(false) }
    var classEditor by remember { mutableStateOf<SchoolClass?>(null) }
    var creatingClass by remember { mutableStateOf(false) }
    var deletingClass by remember { mutableStateOf<SchoolClass?>(null) }
    var showMemberPicker by remember { mutableStateOf(false) }
    var showStudentCreator by remember { mutableStateOf(false) }
    var editingStudent by remember { mutableStateOf<StudentProfile?>(null) }
    var resettingStudent by remember { mutableStateOf<StudentProfile?>(null) }
    var deletingStudent by remember { mutableStateOf<StudentProfile?>(null) }
    var noteStudent by remember { mutableStateOf<StudentProfile?>(null) }
    var showBulk by remember { mutableStateOf(false) }
    var pendingXlsx by remember { mutableStateOf<ByteArray?>(null) }
    val xlsxLauncher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")){uri->if(uri!=null)pendingXlsx?.let{bytes->context.contentResolver.openOutputStream(uri)?.use{it.write(bytes)}};pendingXlsx=null}

    LaunchedEffect(Unit) { viewModel.load() }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !showStudents,
                onClick = { showStudents = false; viewModel.closeClass() },
                label = { Text("کلاس‌ها") }
            )
            FilterChip(
                selected = showStudents,
                onClick = { showStudents = true; viewModel.closeClass() },
                label = { Text("همه دانش‌آموزان") }
            )
            OutlinedButton(onClick = viewModel::load, enabled = !state.actionLoading) {
                Text("تازه‌سازی")
            }
        }

        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        if (state.actionLoading) CircularProgressIndicator()

        Box(Modifier.weight(1f)) {
            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.selectedClass != null -> ClassRosterContent(
                    item = state.selectedClass!!,
                    roster = state.roster,
                    onBack = viewModel::closeClass,
                    onAdd = { showMemberPicker = true },
                    onCreate = { showStudentCreator = true },
                    onRemove = viewModel::removeStudent,
                    onToggle = viewModel::setStudentActive,
                    onEdit = { editingStudent = it },
                    onReset = { resettingStudent = it },
                    onDeleteAccount = { deletingStudent = it },
                    notes=state.studentNotes,
                    onNote={noteStudent=it},
                    onBulk={showBulk=true}
                )
                showStudents -> StudentsContent(
                    students = filteredStudents(state.students, state.query),
                    query = state.query,
                    onQuery = viewModel::setQuery,
                    onCreate = { showStudentCreator = true },
                    onToggle = viewModel::setStudentActive,
                    onEdit = { editingStudent = it },
                    onReset = { resettingStudent = it },
                    onDelete = { deletingStudent = it },
                    notes=state.studentNotes,
                    onNote={noteStudent=it},
                    onBulk={showBulk=true},
                    onExport={
                        pendingXlsx=studentWorkbook(state.students);xlsxLauncher.launch("students.xlsx")
                    }
                )
                else -> ClassesContent(
                    classes = state.classes,
                    onOpen = viewModel::selectClass,
                    onCreate = { creatingClass = true },
                    onEdit = { classEditor = it },
                    onDelete = { deletingClass = it }
                )
            }
        }
    }

    if (creatingClass || classEditor != null) {
        ClassEditorDialog(
            item = classEditor,
            onDismiss = { creatingClass = false; classEditor = null },
            onSave = { name, grade ->
                viewModel.saveClass(classEditor?.id, name, grade)
                creatingClass = false
                classEditor = null
            }
        )
    }

    deletingClass?.let { item ->
        AlertDialog(
            onDismissRequest = { deletingClass = null },
            title = { Text("حذف کلاس") },
            text = { Text("کلاس «${item.name}» حذف شود؟ حساب دانش‌آموزان حذف نمی‌شود.") },
            confirmButton = {
                Button(onClick = { viewModel.deleteClass(item.id); deletingClass = null }) {
                    Text("حذف کلاس")
                }
            },
            dismissButton = { TextButton(onClick = { deletingClass = null }) { Text("انصراف") } }
        )
    }

    if (showMemberPicker && state.selectedClass != null) {
        val inClass = state.roster.mapTo(hashSetOf(), StudentProfile::id)
        MemberPickerDialog(
            students = state.students.filterNot { it.id in inClass },
            onDismiss = { showMemberPicker = false },
            onAdd = { ids -> viewModel.addStudents(ids); showMemberPicker = false }
        )
    }

    if (showStudentCreator) {
        StudentCreatorDialog(
            classId = state.selectedClass?.id,
            onDismiss = { showStudentCreator = false },
            onCreate = { request -> viewModel.createStudent(request); showStudentCreator = false }
        )
    }

    editingStudent?.let { student ->
        StudentEditDialog(
            student = student,
            onDismiss = { editingStudent = null },
            onSave = { request -> viewModel.updateStudent(request); editingStudent = null }
        )
    }

    resettingStudent?.let { student ->
        StudentPasswordResetDialog(
            student = student,
            onDismiss = { resettingStudent = null },
            onReset = { password -> viewModel.resetPassword(student.id, password); resettingStudent = null }
        )
    }

    deletingStudent?.let { student ->
        AlertDialog(
            onDismissRequest = { deletingStudent = null },
            title = { Text("حذف کامل حساب دانش‌آموز") },
            text = { Text("حساب «${student.fullName}» از Auth و پروفایل حذف شود؟ این کار برگشت‌پذیر نیست و با خروج از کلاس فرق دارد.") },
            confirmButton = {
                Button(onClick = { viewModel.deleteStudent(student.id); deletingStudent = null }) { Text("حذف کامل") }
            },
            dismissButton = { TextButton(onClick = { deletingStudent = null }) { Text("انصراف") } }
        )
    }

    noteStudent?.let { student ->
        var note by remember(student.id){mutableStateOf(state.studentNotes[student.id].orEmpty())}
        AlertDialog(onDismissRequest={noteStudent=null},title={Text("یادداشت خصوصی ${student.fullName}")},
            text={OutlinedTextField(note,{note=it.take(4000)},label={Text("یادداشت فقط روی این دستگاه")},minLines=5)},
            confirmButton={Button(onClick={viewModel.saveStudentNote(student.id,note);noteStudent=null}){Text("ذخیره")}},
            dismissButton={TextButton(onClick={noteStudent=null}){Text("انصراف")}})
    }
    if(showBulk) BulkStudentDialog(
        classes=state.classes,initialClassId=state.selectedClass?.id,onDismiss={showBulk=false},
        onCreate={classId,rows->viewModel.createStudentsBulk(classId,rows);showBulk=false}
    )
    state.bulkResult?.let { result -> AlertDialog(onDismissRequest=viewModel::clearMessage,title={Text("نتیجه ساخت گروهی")},text={Column{
        Text("موفق: ${result.credentials.size} · ناموفق: ${result.failures.size}");result.failures.take(10).forEach{Text(it)}
    }},confirmButton={Button(onClick={pendingXlsx=credentialWorkbook(result.credentials);xlsxLauncher.launch("student-credentials.xlsx")}){Text("ذخیره Excel رمزها")}},dismissButton={TextButton(onClick=viewModel::clearMessage){Text("بستن")}}) }

    state.lastCredential?.let { credential ->
        AlertDialog(
            onDismissRequest = viewModel::clearMessage,
            title = { Text("حساب ساخته شد") },
            text = {
                Text("نام کاربری: ${credential.username}\nرمز عبور (فقط همین بار نمایش داده می‌شود): ${credential.password}\nاین رمز در دیتابیس ذخیره نمی‌شود؛ همین حالا امن به دانش‌آموز بدهید.")
            },
            confirmButton = { Button(onClick = viewModel::clearMessage) { Text("متوجه شدم") } }
        )
    }
}

@Composable
private fun ClassesContent(
    classes: List<SchoolClass>,
    onOpen: (SchoolClass) -> Unit,
    onCreate: () -> Unit,
    onEdit: (SchoolClass) -> Unit,
    onDelete: (SchoolClass) -> Unit
) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(onClick = onCreate, modifier = Modifier.fillMaxWidth()) { Text("ساخت کلاس جدید") }
        if (classes.isEmpty()) {
            Text("هنوز کلاسی ساخته نشده است.")
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(classes, key = SchoolClass::id) { item ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(item.name, style = MaterialTheme.typography.titleMedium)
                            Text("پایه: ${item.grade.orEmpty().ifBlank { "—" }}")
                            Text("اعضا: ${item.total} نفر · پسر: ${item.boys} · دختر: ${item.girls}")
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(onClick = { onOpen(item) }) { Text("ورود") }
                                OutlinedButton(onClick = { onEdit(item) }) { Text("ویرایش") }
                                TextButton(onClick = { onDelete(item) }) { Text("حذف") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClassRosterContent(
    item: SchoolClass,
    roster: List<StudentProfile>,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onCreate: () -> Unit,
    onRemove: (String) -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onEdit: (StudentProfile) -> Unit,
    onReset: (StudentProfile) -> Unit,
    onDeleteAccount: (StudentProfile) -> Unit,
    notes:Map<String,String>,
    onNote:(StudentProfile)->Unit,
    onBulk:()->Unit
) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack) { Text("بازگشت") }
            Text(item.name, style = MaterialTheme.typography.titleLarge)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onAdd) { Text("افزودن موجود") }
            OutlinedButton(onClick = onCreate) { Text("حساب جدید") }
            OutlinedButton(onClick=onBulk){Text("ساخت گروهی")}
        }
        if (roster.isEmpty()) Text("این کلاس هنوز عضوی ندارد.")
        else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(roster, key = StudentProfile::id) { student ->
                StudentCard(
                    student = student,
                    onToggle = onToggle,
                    onRemove = { onRemove(student.id) },
                    onEdit = { onEdit(student) },
                    onReset = { onReset(student) },
                    onDelete = { onDeleteAccount(student) },
                    note=notes[student.id],onNote={onNote(student)}
                )
            }
        }
    }
}

@Composable
private fun StudentsContent(
    students: List<StudentProfile>,
    query: String,
    onQuery: (String) -> Unit,
    onCreate: () -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onEdit: (StudentProfile) -> Unit,
    onReset: (StudentProfile) -> Unit,
    onDelete: (StudentProfile) -> Unit,
    notes:Map<String,String>,
    onNote:(StudentProfile)->Unit,
    onBulk:()->Unit,
    onExport:()->Unit
) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            label = { Text("جست‌وجوی نام، نام کاربری، پایه یا پدر") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
            Button(onClick=onCreate){Text("حساب جدید")}
            OutlinedButton(onClick=onBulk){Text("ساخت گروهی")}
            OutlinedButton(onClick=onExport){Text("Excel")}
        }
        if (students.isEmpty()) Text("دانش‌آموزی یافت نشد.")
        else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(students, key = StudentProfile::id) { student ->
                StudentCard(
                    student = student,
                    onToggle = onToggle,
                    onRemove = null,
                    onEdit = { onEdit(student) },
                    onReset = { onReset(student) },
                    onDelete = { onDelete(student) },
                    note=notes[student.id],onNote={onNote(student)}
                )
            }
        }
    }
}

@Composable
private fun StudentCard(
    student: StudentProfile,
    onToggle: (String, Boolean) -> Unit,
    onRemove: (() -> Unit)?,
    onEdit: () -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit,
    note:String?,
    onNote:()->Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(student.fullName.ifBlank { "بدون نام" }, style = MaterialTheme.typography.titleMedium)
            Text("نام کاربری: ${student.username ?: "—"}")
            Text("پایه: ${student.grade ?: "—"} · نام پدر: ${student.fatherName ?: "—"}")
            student.classNames?.takeIf(String::isNotBlank)?.let { Text("کلاس‌ها: $it") }
            note?.takeIf(String::isNotBlank)?.let{Text("یادداشت خصوصی: ${it.take(80)}")}
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = { onToggle(student.id, !student.active) }) {
                    Text(if (student.active) "غیرفعال" else "فعال")
                }
                OutlinedButton(onClick = onEdit) { Text("ویرایش") }
                OutlinedButton(onClick = onReset) { Text("رمز جدید") }
                OutlinedButton(onClick=onNote){Text("یادداشت")}
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                onRemove?.let { TextButton(onClick = it) { Text("خروج از کلاس") } }
                TextButton(onClick = onDelete) { Text("حذف کامل حساب") }
            }
        }
    }
}

@Composable
private fun ClassEditorDialog(
    item: SchoolClass?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember(item?.id) { mutableStateOf(item?.name.orEmpty()) }
    var grade by remember(item?.id) { mutableStateOf(item?.grade.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "کلاس جدید" else "ویرایش کلاس") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("نام کلاس") })
                OutlinedTextField(grade, { grade = it }, label = { Text("پایه") })
            }
        },
        confirmButton = { Button(onClick = { onSave(name, grade) }, enabled = name.isNotBlank()) { Text("ذخیره") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
private fun MemberPickerDialog(
    students: List<StudentProfile>,
    onDismiss: () -> Unit,
    onAdd: (List<String>) -> Unit
) {
    val selected = remember { mutableStateListOf<String>() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("افزودن به کلاس") },
        text = {
            if (students.isEmpty()) Text("دانش‌آموز آزاد دیگری وجود ندارد.")
            else LazyColumn(Modifier.heightIn(max = 420.dp)) {
                items(students, key = StudentProfile::id) { student ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = student.id in selected,
                            onCheckedChange = { checked -> if (checked) selected.add(student.id) else selected.remove(student.id) }
                        )
                        Text(student.fullName)
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onAdd(selected.toList()) }, enabled = selected.isNotEmpty()) { Text("افزودن") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
private fun StudentCreatorDialog(
    classId: String?,
    onDismiss: () -> Unit,
    onCreate: (NewStudentRequest) -> Unit
) {
    var first by remember { mutableStateOf("") }
    var last by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf(generatePassword()) }
    var father by remember { mutableStateOf("") }
    var grade by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("حساب دانش‌آموز جدید") },
        text = {
            LazyColumn(Modifier.heightIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { OutlinedTextField(first, { first = it }, label = { Text("نام") }) }
                item { OutlinedTextField(last, { last = it }, label = { Text("نام خانوادگی") }) }
                item { OutlinedTextField(username, { username = it.lowercase().filter { c -> c in 'a'..'z' || c.isDigit() || c == '_' }.take(20) }, label = { Text("نام کاربری انگلیسی") }) }
                item { OutlinedTextField(password, { password = it }, label = { Text("رمز عبور") }, visualTransformation = PasswordVisualTransformation()) }
                item { OutlinedTextField(father, { father = it }, label = { Text("نام پدر") }) }
                item { OutlinedTextField(grade, { grade = it }, label = { Text("پایه") }) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = gender == "male", onClick = { gender = "male" }, label = { Text("پسر") })
                        FilterChip(selected = gender == "female", onClick = { gender = "female" }, label = { Text("دختر") })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = first.isNotBlank() && username.length >= 4 && password.length in 8..72 && gender.isNotBlank(),
                onClick = {
                    onCreate(NewStudentRequest(first, last, username, password, gender, father, grade, classId))
                }
            ) { Text("ساخت حساب") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
private fun StudentEditDialog(
    student: StudentProfile,
    onDismiss: () -> Unit,
    onSave: (UpdateStudentRequest) -> Unit
) {
    var first by remember(student.id) { mutableStateOf(student.firstName.orEmpty().ifBlank { student.fullName.substringBefore(' ') }) }
    var last by remember(student.id) { mutableStateOf(student.lastName.orEmpty().ifBlank { student.fullName.substringAfter(' ', "") }) }
    var username by remember(student.id) { mutableStateOf(student.username.orEmpty()) }
    var gender by remember(student.id) { mutableStateOf(student.gender.orEmpty()) }
    var fatherName by remember(student.id) { mutableStateOf(student.fatherName.orEmpty()) }
    var grade by remember(student.id) { mutableStateOf(student.grade.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ویرایش دانش‌آموز") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(first, { first = it.take(100) }, label = { Text("نام") })
                OutlinedTextField(last, { last = it.take(100) }, label = { Text("نام خانوادگی") })
                OutlinedTextField(
                    username,
                    { username = it.lowercase().filter { c -> c in 'a'..'z' || c.isDigit() || c == '_' }.take(20) },
                    label = { Text("نام کاربری") }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = gender == "male", onClick = { gender = "male" }, label = { Text("پسر") })
                    FilterChip(selected = gender == "female", onClick = { gender = "female" }, label = { Text("دختر") })
                }
                OutlinedTextField(fatherName, { fatherName = it.take(100) }, label = { Text("نام پدر") })
                OutlinedTextField(grade, { grade = it.take(100) }, label = { Text("پایه") })
                Text("رمز در این فرم نمایش یا بازیابی نمی‌شود.")
            }
        },
        confirmButton = {
            Button(
                enabled = first.isNotBlank() && username.length >= 4 && gender in setOf("male", "female"),
                onClick = { onSave(UpdateStudentRequest(student.id, first, last, username, gender, fatherName, grade)) }
            ) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
private fun StudentPasswordResetDialog(
    student: StudentProfile,
    onDismiss: () -> Unit,
    onReset: (String) -> Unit
) {
    var password by remember(student.id) { mutableStateOf(generatePassword(10)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعیین رمز جدید برای ${student.fullName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it.take(72) },
                    label = { Text("رمز جدید ۸ تا ۷۲ کاراکتر") },
                    visualTransformation = PasswordVisualTransformation()
                )
                Text("رمز قبلی قابل مشاهده نیست. رمز جدید فقط پس از موفقیت همین عملیات یک بار نمایش داده می‌شود.")
            }
        },
        confirmButton = {
            Button(onClick = { onReset(password) }, enabled = password.length in 8..72) { Text("تغییر رمز") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
private fun BulkStudentDialog(
    classes:List<SchoolClass>,initialClassId:String?,onDismiss:()->Unit,onCreate:(String,List<NewStudentRequest>)->Unit
){
    var classId by remember{mutableStateOf(initialClassId?:classes.firstOrNull()?.id.orEmpty())}
    var raw by remember{mutableStateOf("علی,رضایی,ali_reza,pass12345,male,حسن,هفتم")}
    var error by remember{mutableStateOf<String?>(null)}
    AlertDialog(onDismissRequest=onDismiss,title={Text("ساخت گروهی دانش‌آموز")},text={LazyColumn(Modifier.heightIn(max=520.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
        item{Text("هر خط: نام،نام خانوادگی،نام کاربری،رمز،male/female،نام پدر،پایه")}
        item{Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){classes.forEach{c->FilterChip(selected=classId==c.id,onClick={classId=c.id},label={Text(c.name)})}}}
        item{OutlinedTextField(raw,{raw=it},label={Text("حداکثر ۱۰۰ ردیف")},minLines=10,modifier=Modifier.fillMaxWidth())}
        error?.let{item{Text(it,color=MaterialTheme.colorScheme.error)}}
    }},confirmButton={Button(onClick={runCatching{
        require(classId.isNotBlank()){ "کلاس را انتخاب کنید." };raw.lines().filter{it.isNotBlank()}.map{line->val p=line.split(',').map(String::trim);require(p.size>=5){"هر خط حداقل پنج ستون لازم دارد."};NewStudentRequest(p[0],p.getOrElse(1){""},p[2].lowercase(),p[3],p[4].lowercase(),p.getOrElse(5){""},p.getOrElse(6){""},classId)}.also{require(it.size in 1..100)}
    }.onSuccess{onCreate(classId,it)}.onFailure{error=it.message}}){Text("ساخت حساب‌ها")}},dismissButton={TextButton(onClick=onDismiss){Text("انصراف")}})
}

private fun studentWorkbook(students:List<StudentProfile>):ByteArray=XlsxWorkbook.build(listOf(XlsxSheet("دانش‌آموزان",listOf(listOf("نام","نام کاربری","جنسیت","پایه","نام پدر","کلاس","وضعیت"))+students.map{listOf(it.fullName,it.username.orEmpty(),it.gender.orEmpty(),it.grade.orEmpty(),it.fatherName.orEmpty(),it.classNames.orEmpty(),if(it.active)"فعال" else "غیرفعال") })))
private fun credentialWorkbook(items:List<StudentCredential>):ByteArray=XlsxWorkbook.build(listOf(XlsxSheet("حساب‌ها",listOf(listOf("نام کاربری","رمز یک‌بارنمایش"))+items.map{listOf(it.username,it.password)})))

private fun filteredStudents(items: List<StudentProfile>, query: String): List<StudentProfile> {
    val value = query.trim().lowercase()
    if (value.isEmpty()) return items
    return items.filter { student ->
        listOf(
            student.fullName,
            student.username,
            student.grade,
            student.fatherName,
            student.classNames
        ).any { it?.lowercase()?.contains(value) == true }
    }
}

private fun generatePassword(length: Int = 8): String {
    val chars = "abcdefghjkmnpqrstuvwxyz23456789"
    return buildString { repeat(length) { append(chars[Random.nextInt(chars.length)]) } }
}
