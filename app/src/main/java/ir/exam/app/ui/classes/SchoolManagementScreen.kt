package ir.exam.app.ui.classes

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ToggleOff
import androidx.compose.material.icons.outlined.ToggleOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ir.exam.app.core.export.XlsxSheet
import ir.exam.app.ui.common.PasswordVisibilityButton
import ir.exam.app.ui.common.passwordTransformation
import ir.exam.app.core.export.XlsxWorkbook
import ir.exam.app.domain.model.NewStudentRequest
import ir.exam.app.domain.model.SchoolClass
import ir.exam.app.domain.model.StudentCredential
import ir.exam.app.domain.model.StudentProfile
import ir.exam.app.domain.model.UpdateStudentRequest
import kotlin.random.Random

enum class SchoolLaunchAction { SHOW_CLASSES, SHOW_STUDENTS, CREATE_STUDENT, CREATE_CLASS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchoolManagementScreen(
    launchAction: SchoolLaunchAction? = null,
    onLaunchActionConsumed: () -> Unit = {}
) {
    val context=LocalContext.current
    val viewModel=remember(context){ClassesViewModel(context=context.applicationContext)}
    val state by viewModel.state.collectAsState()
    var showStudents by remember { mutableStateOf(false) }
    var classEditor by remember { mutableStateOf<SchoolClass?>(null) }
    var creatingClass by remember { mutableStateOf(false) }
    var deletingClass by remember { mutableStateOf<SchoolClass?>(null) }
    var showMemberPicker by remember { mutableStateOf(false) }
    var editingStudent by remember { mutableStateOf<StudentProfile?>(null) }
    var showBulk by remember { mutableStateOf(false) }
    var pendingXlsx by remember { mutableStateOf<ByteArray?>(null) }
    val xlsxLauncher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")){uri->if(uri!=null)pendingXlsx?.let{bytes->context.contentResolver.openOutputStream(uri)?.use{it.write(bytes)}};pendingXlsx=null}

    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(launchAction) {
        when (launchAction) {
            SchoolLaunchAction.SHOW_CLASSES -> {
                showStudents = false
                viewModel.closeClass()
                onLaunchActionConsumed()
            }
            SchoolLaunchAction.SHOW_STUDENTS -> {
                showStudents = true
                viewModel.closeClass()
                onLaunchActionConsumed()
            }
            SchoolLaunchAction.CREATE_STUDENT -> {
                showStudents = true
                viewModel.closeClass()
                showBulk = true
                onLaunchActionConsumed()
            }
            SchoolLaunchAction.CREATE_CLASS -> {
                showStudents = false
                viewModel.closeClass()
                creatingClass = true
                onLaunchActionConsumed()
            }
            null -> Unit
        }
    }

    PullToRefreshBox(
        isRefreshing = state.loading || state.actionLoading,
        onRefresh = viewModel::load,
        modifier = Modifier.fillMaxSize()
    ) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
                    onCreate = { showBulk = true },
                    onToggle = viewModel::setStudentActive,
                    onEdit = { editingStudent = it },
                    classes = state.classes,
                    onAddToClasses = viewModel::addStudentToClasses
                )
                showStudents -> StudentsContent(
                    students = filteredStudents(state.students, state.query),
                    query = state.query,
                    onQuery = viewModel::setQuery,
                    onToggle = viewModel::setStudentActive,
                    onEdit = { editingStudent = it },
                    onBulk = { showBulk = true },
                    onExport = {
                        pendingXlsx = studentWorkbook(state.students)
                        xlsxLauncher.launch("students.xlsx")
                    },
                    classes = state.classes,
                    onAddToClasses = viewModel::addStudentToClasses
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


    editingStudent?.let { student ->
        StudentEditDialog(
            student = student,
            onDismiss = { editingStudent = null },
            onSave = { request -> viewModel.updateStudent(request); editingStudent = null }
        )
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
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(item.name, style = MaterialTheme.typography.titleMedium)
                                Text("پایه: ${item.grade.orEmpty().ifBlank { "—" }}")
                            }
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
    onToggle: (String, Boolean) -> Unit,
    onEdit: (StudentProfile) -> Unit,
    classes: List<SchoolClass>,
    onAddToClasses: (String, Set<String>) -> Unit
) {
    var addMenuOpen by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack) { Text("بازگشت") }
            Text(item.name, style = MaterialTheme.typography.titleLarge)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = { addMenuOpen = !addMenuOpen }) {
                Text(if (addMenuOpen) "×" else "+", style = MaterialTheme.typography.titleLarge)
            }
            AnimatedVisibility(
                visible = addMenuOpen,
                modifier = Modifier.padding(top = 6.dp),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Card(Modifier.clickable { addMenuOpen = false; onAdd() }) {
                        Text("افزودن موجود", Modifier.padding(14.dp), textAlign = TextAlign.Center)
                    }
                    Card(Modifier.clickable { addMenuOpen = false; onCreate() }) {
                        Text("افزودن جدید", Modifier.padding(14.dp), textAlign = TextAlign.Center)
                    }
                }
            }
        }
        if (roster.isEmpty()) Text("این کلاس هنوز عضوی ندارد.")
        else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(roster, key = StudentProfile::id) { student ->
                StudentCard(
                    student = student,
                    onToggle = onToggle,
                    onEdit = { onEdit(student) },
                    classes = classes,
                    onAddToClasses = { classIds -> onAddToClasses(student.id, classIds) }
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
    onToggle: (String, Boolean) -> Unit,
    onEdit: (StudentProfile) -> Unit,
    onBulk: () -> Unit,
    onExport: () -> Unit,
    classes: List<SchoolClass>,
    onAddToClasses: (String, Set<String>) -> Unit
) {
    var searchOpen by remember { mutableStateOf(query.isNotBlank()) }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onExport) { Text("Excel") }
            Button(
                onClick = onBulk,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .semantics { contentDescription = "افزودن گروهی دانش‌آموز" }
            ) { Text("+", style = MaterialTheme.typography.titleLarge) }
            if (!searchOpen) {
                IconButton(onClick = { searchOpen = true }) {
                    Icon(Icons.Outlined.Search, contentDescription = "جست‌وجوی دانش‌آموز")
                }
            }
        }
        AnimatedVisibility(
            visible = searchOpen,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQuery,
                label = { Text("جست‌وجوی نام، نام کاربری، پایه یا پدر") },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            onQuery("")
                            searchOpen = false
                        }
                    ) {
                        Icon(Icons.Outlined.Close, contentDescription = "بستن جست‌وجو")
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (students.isEmpty()) Text("دانش‌آموزی یافت نشد.")
        else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(students, key = StudentProfile::id) { student ->
                StudentCard(
                    student = student,
                    onToggle = onToggle,
                    onEdit = { onEdit(student) },
                    classes = classes,
                    onAddToClasses = { classIds -> onAddToClasses(student.id, classIds) }
                )
            }
        }
    }
}

@Composable
private fun StudentCard(
    student: StudentProfile,
    onToggle: (String, Boolean) -> Unit,
    onEdit: () -> Unit,
    classes: List<SchoolClass>,
    onAddToClasses: (Set<String>) -> Unit
) {
    var expanded by remember(student.id) { mutableStateOf(false) }
    var classPickerOpen by remember(student.id) { mutableStateOf(false) }
    var selectedClasses by remember(student.id) { mutableStateOf(emptySet<String>()) }
    val tint = if (student.gender.equals("female", true)) {
        Color(0xFFFF80AB).copy(alpha = .22f)
    } else {
        Color(0xFF64B5F6).copy(alpha = .22f)
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = tint)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    student.fullName.ifBlank { "بدون نام" },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Text("پایه: ${student.grade.orEmpty().ifBlank { "—" }}")
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("نام کاربری: ${student.username ?: "—"}")
                    Text("نام پدر: ${student.fatherName ?: "—"}")
                    student.classNames?.takeIf(String::isNotBlank)?.let { Text("کلاس‌ها: $it") }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onToggle(student.id, !student.active) }) {
                            Icon(
                                imageVector = if (student.active) Icons.Outlined.ToggleOn else Icons.Outlined.ToggleOff,
                                contentDescription = if (student.active) "فعال؛ لمس برای غیرفعال" else "غیرفعال؛ لمس برای فعال",
                                tint = if (student.active) Color(0xFF19945B) else Color(0xFFD63B49)
                            )
                        }
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Outlined.Edit, contentDescription = "ویرایش دانش‌آموز")
                        }
                        IconButton(onClick = { classPickerOpen = !classPickerOpen }) {
                            Icon(Icons.Outlined.Add, contentDescription = "افزودن به کلاس‌ها")
                        }
                    }
                    AnimatedVisibility(
                        visible = classPickerOpen,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("انتخاب یک یا چند کلاس")
                            classes.chunked(3).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    row.forEach { item ->
                                        FilterChip(
                                            selected = item.id in selectedClasses,
                                            onClick = {
                                                selectedClasses = if (item.id in selectedClasses) {
                                                    selectedClasses - item.id
                                                } else selectedClasses + item.id
                                            },
                                            label = { Text(item.name) }
                                        )
                                    }
                                }
                            }
                            Button(
                                onClick = {
                                    onAddToClasses(selectedClasses)
                                    selectedClasses = emptySet()
                                    classPickerOpen = false
                                },
                                enabled = selectedClasses.isNotEmpty(),
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) { Text("افزودن") }
                        }
                    }
                }
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
    var gender by remember { mutableStateOf<String?>(null) }
    var grade by remember { mutableStateOf<String?>(null) }
    val grades = remember(students) {
        students.mapNotNull { it.grade?.trim()?.takeIf(String::isNotBlank) }.distinct().sorted()
    }
    val visible = students.filter { student ->
        (gender == null || student.gender == gender) &&
            (grade == null || student.grade == grade)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("افزودن موجود") },
        text = {
            LazyColumn(Modifier.heightIn(max = 480.dp)) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        FilterChip(
                            selected = gender == null,
                            onClick = { gender = null },
                            label = { Text("همه") }
                        )
                        FilterChip(
                            selected = gender == "female",
                            onClick = { gender = "female" },
                            label = { Text("دختر") }
                        )
                        FilterChip(
                            selected = gender == "male",
                            onClick = { gender = "male" },
                            label = { Text("پسر") }
                        )
                    }
                    if (grades.isNotEmpty()) {
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            FilterChip(
                                selected = grade == null,
                                onClick = { grade = null },
                                label = { Text("همه پایه‌ها") }
                            )
                            grades.forEach { item ->
                                FilterChip(
                                    selected = grade == item,
                                    onClick = { grade = item },
                                    label = { Text(item) }
                                )
                            }
                        }
                    }
                }
                if (visible.isEmpty()) item { Text("دانش‌آموزی با این فیلتر یافت نشد.") }
                items(visible, key = StudentProfile::id) { student ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = student.id in selected,
                            onCheckedChange = { checked ->
                                if (checked) selected.add(student.id) else selected.remove(student.id)
                            }
                        )
                        Text("${student.fullName} · پایه ${student.grade.orEmpty().ifBlank { "—" }}")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(selected.toList()) },
                enabled = selected.isNotEmpty()
            ) { Text("افزودن") }
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
    var first by remember(student.id) {
        mutableStateOf(student.firstName.orEmpty().ifBlank { student.fullName.substringBefore(' ') })
    }
    var last by remember(student.id) {
        mutableStateOf(student.lastName.orEmpty().ifBlank { student.fullName.substringAfter(' ', "") })
    }
    var username by remember(student.id) { mutableStateOf(student.username.orEmpty()) }
    var gender by remember(student.id) { mutableStateOf(student.gender.orEmpty()) }
    var fatherName by remember(student.id) { mutableStateOf(student.fatherName.orEmpty()) }
    var grade by remember(student.id) { mutableStateOf(student.grade.orEmpty()) }
    var newPassword by remember(student.id) { mutableStateOf("") }
    var passwordVisible by remember(student.id) { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            Modifier.fillMaxSize().imePadding().padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().widthIn(max = 620.dp),
                shape = MaterialTheme.shapes.large,
                tonalElevation = 6.dp
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ویرایش دانش‌آموز", style = MaterialTheme.typography.titleLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            first, { first = it.take(100) }, label = { Text("نام") },
                            singleLine = true, modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            last, { last = it.take(100) }, label = { Text("نام خانوادگی") },
                            singleLine = true, modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            fatherName, { fatherName = it.take(100) }, label = { Text("نام پدر") },
                            singleLine = true, modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            grade, { grade = it.take(100) }, label = { Text("پایه") },
                            singleLine = true, modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            username,
                            {
                                username = it.lowercase().filter { c ->
                                    c in 'a'..'z' || c.isDigit() || c == '_'
                                }.take(20)
                            },
                            label = { Text("نام کاربری") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            newPassword,
                            { newPassword = it.take(72) },
                            label = { Text("رمز جدید اختیاری") },
                            supportingText = {
                                Text("رمز قبلی قابل بازیابی نیست؛ خالی بماند تغییر نمی‌کند.")
                            },
                            visualTransformation = passwordTransformation(passwordVisible),
                            trailingIcon = {
                                PasswordVisibilityButton(
                                    visible = passwordVisible,
                                    onToggle = { passwordVisible = !passwordVisible }
                                )
                            },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        FilterChip(
                            selected = gender == "female",
                            onClick = { gender = "female" },
                            label = { Text("دختر") }
                        )
                        FilterChip(
                            selected = gender == "male",
                            onClick = { gender = "male" },
                            label = { Text("پسر") },
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        OutlinedButton(onClick = { newPassword = generatePassword(10) }) {
                            Text("🎲 رمز")
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                            Text("انصراف")
                        }
                        Button(
                            enabled = first.isNotBlank() && username.length >= 4 &&
                                gender in setOf("male", "female") &&
                                (newPassword.isBlank() || newPassword.length in 8..72),
                            onClick = {
                                onSave(
                                    UpdateStudentRequest(
                                        student.id,
                                        first,
                                        last,
                                        username,
                                        gender,
                                        fatherName,
                                        grade,
                                        newPassword.takeIf(String::isNotBlank)
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("ذخیره") }
                    }
                }
            }
        }
    }
}

private data class BulkStudentDraft(
    val first: String = "",
    val last: String = "",
    val username: String = "",
    val password: String = generatePassword(10),
    val passwordVisible: Boolean = false,
    val gender: String = "",
    val father: String = "",
    val grade: String = "",
    val usernameEdited: Boolean = false
)

@Composable
private fun BulkStudentDialog(
    classes: List<SchoolClass>,
    initialClassId: String?,
    onDismiss: () -> Unit,
    onCreate: (String, List<NewStudentRequest>) -> Unit
) {
    var classId by remember { mutableStateOf(initialClassId ?: classes.firstOrNull()?.id.orEmpty()) }
    val rows = remember { mutableStateListOf(BulkStudentDraft()) }
    var error by remember { mutableStateOf<String?>(null) }

    fun recomputeSuggestions() {
        val seen = mutableMapOf<String, Int>()
        rows.indices.forEach { index ->
            val row = rows[index]
            val base = PersianUsernameSuggester.suggest(row.first, row.last)
            val occurrence = seen.getOrDefault(base, 0)
            seen[base] = occurrence + 1
            if (!row.usernameEdited) {
                rows[index] = row.copy(
                    username = if (occurrence == 0) base
                    else PersianUsernameSuggester.suggest(row.first, row.last, occurrence + 1)
                )
            }
        }
    }

    fun submitBulk() {
        runCatching {
            require(classId.isNotBlank()) { "کلاس را انتخاب کنید." }
            val requests = rows.map { row ->
                require(row.first.isNotBlank()) { "نام همه ردیف‌ها لازم است." }
                require(row.username.length >= 4) {
                    "نام کاربری همه ردیف‌ها باید حداقل ۴ نویسه باشد."
                }
                require(row.password.length in 8..72) {
                    "رمز همه ردیف‌ها باید ۸ تا ۷۲ نویسه باشد."
                }
                require(row.gender in setOf("male", "female")) {
                    "جنسیت همه ردیف‌ها را انتخاب کنید."
                }
                NewStudentRequest(
                    row.first,
                    row.last,
                    row.username,
                    row.password,
                    row.gender,
                    row.father,
                    row.grade,
                    classId
                )
            }
            require(requests.map { it.username }.distinct().size == requests.size) {
                "نام کاربری تکراری در ردیف‌ها وجود دارد."
            }
            requests
        }.onSuccess { onCreate(classId, it) }
            .onFailure { error = it.message }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            Modifier.fillMaxSize().imePadding().padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp),
                shape = MaterialTheme.shapes.large,
                tonalElevation = 6.dp
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (rows.size < 100) {
                                    rows.add(BulkStudentDraft())
                                    recomputeSuggestions()
                                }
                            },
                            enabled = rows.size < 100,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25A86B)),
                            modifier = Modifier
                                .weight(1f)
                                .semantics { contentDescription = "ردیف جدید" }
                        ) { Text("+", style = MaterialTheme.typography.titleLarge) }
                        Button(
                            onClick = ::submitBulk,
                            modifier = Modifier.weight(2f)
                        ) { Text("ایجاد") }
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5484D)),
                            modifier = Modifier
                                .weight(1f)
                                .semantics { contentDescription = "انصراف" }
                        ) { Text("×", style = MaterialTheme.typography.titleLarge) }
                    }
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        classes.forEach { item ->
                            FilterChip(
                                selected = classId == item.id,
                                onClick = { classId = item.id },
                                label = { Text(item.name) }
                            )
                        }
                    }
                    LazyColumn(
                        Modifier.heightIn(max = 480.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(rows.size) { index ->
                            val row = rows[index]
                            Card(Modifier.fillMaxWidth()) {
                                Column(
                                    Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedTextField(
                                            row.first,
                                            {
                                                rows[index] = row.copy(first = it.take(100))
                                                recomputeSuggestions()
                                            },
                                            label = { Text("نام") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            row.last,
                                            {
                                                rows[index] = row.copy(last = it.take(100))
                                                recomputeSuggestions()
                                            },
                                            label = { Text("نام خانوادگی") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedTextField(
                                            row.father,
                                            { rows[index] = row.copy(father = it.take(100)) },
                                            label = { Text("نام پدر") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            row.grade,
                                            { rows[index] = row.copy(grade = it.take(100)) },
                                            label = { Text("پایه") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedTextField(
                                            row.username,
                                            {
                                                rows[index] = row.copy(
                                                    username = it.lowercase().filter { c ->
                                                        c in 'a'..'z' || c.isDigit() || c == '_'
                                                    }.take(20),
                                                    usernameEdited = true
                                                )
                                            },
                                            label = { Text("نام کاربری") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            row.password,
                                            { rows[index] = row.copy(password = it.take(72)) },
                                            label = { Text("رمز") },
                                            visualTransformation = passwordTransformation(row.passwordVisible),
                                            trailingIcon = {
                                                PasswordVisibilityButton(
                                                    visible = row.passwordVisible,
                                                    onToggle = {
                                                        rows[index] = row.copy(
                                                            passwordVisible = !row.passwordVisible
                                                        )
                                                    }
                                                )
                                            },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        FilterChip(
                                            selected = row.gender == "female",
                                            onClick = { rows[index] = row.copy(gender = "female") },
                                            label = { Text("دختر") },
                                            modifier = Modifier.weight(1f)
                                        )
                                        FilterChip(
                                            selected = row.gender == "male",
                                            onClick = { rows[index] = row.copy(gender = "male") },
                                            label = { Text("پسر") },
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedButton(
                                            onClick = {
                                                rows[index] = row.copy(password = generatePassword(10))
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) { Text("🎲") }
                                        if (rows.size > 1) {
                                            TextButton(
                                                onClick = {
                                                    rows.removeAt(index)
                                                    recomputeSuggestions()
                                                },
                                                modifier = Modifier.weight(1f)
                                            ) { Text("حذف") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
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
