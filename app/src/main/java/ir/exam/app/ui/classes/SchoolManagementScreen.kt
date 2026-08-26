package ir.exam.app.ui.classes

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.PersistableBundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ToggleOff
import androidx.compose.material.icons.outlined.ToggleOn
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import ir.exam.app.core.export.XlsxSheet
import ir.exam.app.ui.common.FieldOfStudyPicker
import ir.exam.app.ui.common.GradeOdometerPicker
import ir.exam.app.ui.common.PasswordVisibilityButton
import ir.exam.app.ui.common.passwordTransformation
import ir.exam.app.core.export.XlsxWorkbook
import ir.exam.app.core.calendar.PersianDigits
import ir.exam.app.data.local.StudentPasswordVault
import ir.exam.app.domain.model.NewStudentRequest
import ir.exam.app.domain.model.SchoolClass
import ir.exam.app.domain.model.StudentCredential
import ir.exam.app.domain.model.StudentProfile
import ir.exam.app.domain.model.UpdateStudentRequest
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class SchoolLaunchAction { SHOW_CLASSES, SHOW_STUDENTS, CREATE_STUDENT, CREATE_CLASS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchoolManagementScreen(
    launchAction: SchoolLaunchAction? = null,
    onLaunchActionConsumed: () -> Unit = {},
    // V61.0 — مدیر هنگام ساخت کلاس، معلم را از لیست معلم‌های مدرسه انتخاب می‌کند.
    managerTeacherPicker: Boolean = false
) {
    val context=LocalContext.current
    val viewModel=remember(context){ClassesViewModel(context=context.applicationContext)}
    val passwordVault = remember(context) { StudentPasswordVault(context.applicationContext) }
    val state by viewModel.state.collectAsState()
    var showStudents by remember { mutableStateOf(false) }
    var classEditor by remember { mutableStateOf<SchoolClass?>(null) }
    var creatingClass by remember { mutableStateOf(false) }
    var deletingClass by remember { mutableStateOf<SchoolClass?>(null) }
    var showMemberPicker by remember { mutableStateOf(false) }
    var editingStudent by remember { mutableStateOf<StudentProfile?>(null) }
    var deletingStudent by remember { mutableStateOf<StudentProfile?>(null) }
    var showBulk by remember { mutableStateOf(false) }
    // cache زندهٔ رمزها برای UI؛ منبع پایدار آن StudentPasswordVault رمزنگاری‌شده
    // با Android Keystore است و پس از بازشدن دوبارهٔ برنامه از روی دستگاه پر می‌شود.
    val knownPasswords = remember { mutableStateMapOf<String, String>() }
    LaunchedEffect(state.students, state.roster) {
        val students = (state.students + state.roster).distinctBy(StudentProfile::id)
        val restored = withContext(Dispatchers.IO) {
            students.mapNotNull { student ->
                val username = student.username?.lowercase() ?: return@mapNotNull null
                passwordVault.read(student.id)?.let { username to it }
            }
        }
        restored.forEach { (username, password) -> knownPasswords[username] = password }
    }
    LaunchedEffect(state.lastCredential) {
        state.lastCredential?.let { credential ->
            knownPasswords[credential.username.lowercase()] = credential.password
            withContext(Dispatchers.IO) {
                runCatching { passwordVault.write(credential.id, credential.password) }
            }
        }
    }
    LaunchedEffect(state.bulkResult) {
        state.bulkResult?.credentials.orEmpty().forEach { credential ->
            knownPasswords[credential.username.lowercase()] = credential.password
            withContext(Dispatchers.IO) {
                runCatching { passwordVault.write(credential.id, credential.password) }
            }
        }
    }
    var pendingXlsx by remember { mutableStateOf<ByteArray?>(null) }
    val xlsxLauncher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")){uri->if(uri!=null)pendingXlsx?.let{bytes->context.contentResolver.openOutputStream(uri)?.use{it.write(bytes)}};pendingXlsx=null}

    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(launchAction) {
        when (launchAction) {
            SchoolLaunchAction.SHOW_CLASSES -> {
                showStudents = false
                viewModel.closeClass()
                viewModel.closeSchools()
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
                    onDelete = { viewModel.removeStudent(it.id) },
                    onAddToMyList = viewModel::addClassStudentToMyList,
                    classes = state.classes,
                    membershipOnlyDelete = true,
                    onAddToClasses = viewModel::addStudentToClasses,
                    knownPasswordOf = { username -> knownPasswords[username?.lowercase()] }
                )
                showStudents -> StudentsContent(
                    students = filteredStudents(state.students, state.query),
                    query = state.query,
                    onQuery = viewModel::setQuery,
                    onToggle = viewModel::setStudentActive,
                    onEdit = { editingStudent = it },
                    onDelete = { deletingStudent = it },
                    onBulk = { showBulk = true },
                    onExport = {
                        pendingXlsx = studentWorkbook(state.students)
                        xlsxLauncher.launch("students.xlsx")
                    },
                    classes = state.classes,
                    onAddToClasses = viewModel::addStudentToClasses,
                    knownPasswordOf = { username -> knownPasswords[username?.lowercase()] }
                )
                // V61.0 — نمای «مدارس» بخش کلاس‌ها: مدرسه → کلاس‌های مدرسه → roster.
                state.schoolsOpen && state.selectedSchool != null -> SchoolClassesContent(
                    school = state.selectedSchool!!,
                    classes = state.schoolClasses,
                    onBack = viewModel::closeSchool,
                    onOpen = viewModel::selectClass
                )
                state.schoolsOpen -> SchoolsContent(
                    schools = state.schools,
                    onBack = viewModel::closeSchools,
                    onOpen = viewModel::selectSchool
                )
                else -> ClassesContent(
                    classes = state.classes,
                    onOpen = viewModel::selectClass,
                    onCreate = { creatingClass = true },
                    onSchools = viewModel::openSchools,
                    onEdit = { classEditor = it },
                    onDelete = { deletingClass = it }
                )
            }
        }
    }
    }

    if (creatingClass || classEditor != null) {
        // V61.0 — مدیر: پیش از باز شدن پنجره، معلم‌های مدرسه بارگیری می‌شوند.
        LaunchedEffect(creatingClass, managerTeacherPicker) {
            if (creatingClass && managerTeacherPicker) viewModel.loadSchoolTeachers()
        }
        ClassEditorDialog(
            item = classEditor,
            teachers = if (creatingClass && managerTeacherPicker) state.schoolTeachers else emptyList(),
            onDismiss = { creatingClass = false; classEditor = null },
            onSave = { name, grade, field, teacherId ->
                viewModel.saveClass(classEditor?.id, name, grade, field, teacherId)
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

    deletingStudent?.let { student ->
        AlertDialog(
            onDismissRequest = { deletingStudent = null },
            title = { Text("حذف دانش‌آموز") },
            text = {
                Text(
                    "حساب «${student.fullName.ifBlank { student.username.orEmpty() }}» و همه عضویت‌های کلاس او حذف شود؟ این عملیات برگشت‌پذیر نیست."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteStudent(student.id)
                        deletingStudent = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD63B49))
                ) { Text("تأیید حذف") }
            },
            dismissButton = {
                TextButton(onClick = { deletingStudent = null }) { Text("انصراف") }
            }
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
        val sessionPassword = knownPasswords[student.username?.lowercase()]
        StudentEditDialog(
            student = student,
            currentPassword = sessionPassword,
            onDismiss = { editingStudent = null },
            onSave = { request ->
                // اگر فقط نام کاربری عوض شد، رمز شناخته‌شدهٔ همین نشست با نام جدید
                // نیز در دسترس بماند. رمز جدید فقط پس از موفقیت سرور از
                // state.lastCredential وارد knownPasswords می‌شود.
                if (request.newPassword.isNullOrBlank() && !sessionPassword.isNullOrBlank()) {
                    // Vault با student.id کلید خورده و با تغییر نام کاربری نیاز به
                    // جابه‌جایی ندارد؛ فقط cache نمایشی نام جدید را نیز می‌گیرد.
                    knownPasswords[request.username.lowercase()] = sessionPassword
                }
                viewModel.updateStudent(request)
                editingStudent = null
            }
        )
    }

    if(showBulk) BulkStudentDialog(
        onDismiss={showBulk=false},
        onCreate={requests->
            requests.forEach{knownPasswords[it.username.lowercase()]=it.password}
            viewModel.createStudentsBulk(null,requests);showBulk=false
        }
    )
    state.bulkResult?.let { result -> AlertDialog(onDismissRequest=viewModel::clearMessage,title={Text("نتیجه ساخت گروهی")},text={Column{
        Text("موفق: ${result.credentials.size} · ناموفق: ${result.failures.size}");result.failures.take(10).forEach{Text(it)}
    }},confirmButton={Button(onClick={pendingXlsx=credentialWorkbook(result.credentials);xlsxLauncher.launch("student-credentials.xlsx")}){Text("ذخیره Excel رمزها")}},dismissButton={TextButton(onClick=viewModel::clearMessage){Text("بستن")}}) }

    state.lastCredential?.let { credential ->
        val credentialStudent = state.students.firstOrNull { it.id == credential.id }
            ?: state.roster.firstOrNull { it.id == credential.id }
        AlertDialog(
            onDismissRequest = viewModel::clearMessage,
            title = { Text("رمز یک‌بارنمایش آماده است") },
            text = {
                Text(
                    "نام کاربری: ${credential.username}\n" +
                        "رمز جدید: ${credential.password}\n" +
                        "رمز فقط در حافظه این نشست است و پس از بستن پنجره قابل بازیابی نیست."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        copyOneTimeCredential(context, credential, credentialStudent)
                        viewModel.clearMessage()
                    }
                ) { Text("کپی اطلاعات و رمز جدید") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::clearMessage) { Text("بستن") }
            }
        )
    }
}

@Composable
private fun ClassesContent(
    classes: List<SchoolClass>,
    onOpen: (SchoolClass) -> Unit,
    onCreate: () -> Unit,
    onSchools: () -> Unit,
    onEdit: (SchoolClass) -> Unit,
    onDelete: (SchoolClass) -> Unit
) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // V61.0 — دکمهٔ «مدارس» کنار ساخت کلاس جدید.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onCreate, modifier = Modifier.weight(2f)) { Text("ساخت کلاس جدید") }
            OutlinedButton(onClick = onSchools, modifier = Modifier.weight(1f)) { Text("مدارس") }
        }
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
                                Text(
                                    "پایه: ${item.grade.orEmpty().ifBlank { "—" }}" +
                                        item.fieldOfStudy?.takeIf(String::isNotBlank)?.let { " · $it" }.orEmpty()
                                )
                            }
                            Text("اعضا: ${item.total} نفر · پسر: ${item.boys} · دختر: ${item.girls}")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
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

/** V61.0 — لیست مدرسه‌های عضو معلم به‌صورت کارت؛ لمس کارت = کلاس‌های آن مدرسه. */
@Composable
private fun SchoolsContent(
    schools: List<TeacherSchoolItem>,
    onBack: () -> Unit,
    onOpen: (TeacherSchoolItem) -> Unit
) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("مدارس من", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onBack) { Text("بازگشت به کلاس‌ها") }
        }
        if (schools.isEmpty()) {
            Text("هنوز عضو مدرسه‌ای نیستید. با کد دعوت مدیر به مدرسه بپیوندید.")
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(schools, key = TeacherSchoolItem::id) { item ->
                    Card(Modifier.fillMaxWidth().clickable { onOpen(item) }) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(item.name.ifBlank { "مدرسه" }, style = MaterialTheme.typography.titleMedium)
                            Text(
                                listOf(item.province, item.city)
                                    .filter(String::isNotBlank)
                                    .joinToString(" · ")
                                    .ifBlank { "—" }
                            )
                            Text("کلاس‌های من در این مدرسه: ${PersianDigits.convert(item.classCount)}")
                        }
                    }
                }
            }
        }
    }
}

/** V61.0 — کلاس‌های معلم در مدرسهٔ انتخابی؛ لمس کلاس = مدیریت دانش‌آموزان همان roster. */
@Composable
private fun SchoolClassesContent(
    school: TeacherSchoolItem,
    classes: List<SchoolClass>,
    onBack: () -> Unit,
    onOpen: (SchoolClass) -> Unit
) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("کلاس‌های ${school.name}", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onBack) { Text("بازگشت به مدارس") }
        }
        if (classes.isEmpty()) {
            Text("در این مدرسه کلاسی ندارید.")
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(classes, key = SchoolClass::id) { item ->
                    Card(Modifier.fillMaxWidth().clickable { onOpen(item) }) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(item.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "پایه: ${item.grade.orEmpty().ifBlank { "—" }}" +
                                        item.fieldOfStudy?.takeIf(String::isNotBlank)?.let { " · $it" }.orEmpty()
                                )
                            }
                            Text("اعضا: ${item.total} نفر · پسر: ${item.boys} · دختر: ${item.girls}")
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
    onDelete: (StudentProfile) -> Unit,
    onAddToMyList: (String) -> Unit,
    classes: List<SchoolClass>,
    membershipOnlyDelete: Boolean,
    onAddToClasses: (String, Set<String>) -> Unit,
    knownPasswordOf: (String?) -> String?
) {
    var addMenuOpen by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack) { Text("بازگشت") }
            Text(item.name, style = MaterialTheme.typography.titleLarge)
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = { addMenuOpen = !addMenuOpen },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(if (addMenuOpen) "×" else "+", style = MaterialTheme.typography.titleLarge)
            }
            AnimatedVisibility(
                visible = addMenuOpen,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    StudentCard(
                        student = student,
                        onToggle = onToggle,
                        onEdit = { onEdit(student) },
                        onDelete = { onDelete(student) },
                        classes = classes,
                        membershipOnlyDelete = membershipOnlyDelete,
                        knownPasswordOf = knownPasswordOf,
                        onAddToClasses = { classIds -> onAddToClasses(student.id, classIds) }
                    )
                    if (!student.inMyList) {
                        OutlinedButton(onClick = { onAddToMyList(student.id) }, modifier = Modifier.fillMaxWidth()) {
                            Text("افزودن به لیست دانش‌آموزان من")
                        }
                    }
                }
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
    onDelete: (StudentProfile) -> Unit,
    onBulk: () -> Unit,
    onExport: () -> Unit,
    classes: List<SchoolClass>,
    onAddToClasses: (String, Set<String>) -> Unit,
    knownPasswordOf: (String?) -> String?
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
                    onDelete = { onDelete(student) },
                    classes = classes,
                    onAddToClasses = { classIds -> onAddToClasses(student.id, classIds) },
                    knownPasswordOf = knownPasswordOf
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
    onDelete: () -> Unit,
    classes: List<SchoolClass>,
    onAddToClasses: (Set<String>) -> Unit,
    knownPasswordOf: (String?) -> String? = { null },
    membershipOnlyDelete: Boolean = false
) {
    val context = LocalContext.current
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
            // نام کامل، سپس با کمی فاصله پایه و رشته در همان سطر.
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    student.fullName.ifBlank { "بدون نام" },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    listOf(student.grade, student.fieldOfStudy)
                        .mapNotNull { it?.trim()?.takeIf(String::isNotBlank) }
                        .joinToString(" ")
                        .ifBlank { "—" },
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    // نام پدر و نام کاربری در یک سطر.
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "نام پدر: ${student.fatherName.orEmpty().ifBlank { "—" }}",
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "نام کاربری: ${student.username.orEmpty().ifBlank { "—" }}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    student.classNames?.takeIf(String::isNotBlank)?.let { Text("کلاس‌ها: $it") }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!membershipOnlyDelete && student.canManageAccount) {
                        IconButton(
                            onClick = { onToggle(student.id, !student.active) },
                            modifier = Modifier.weight(1f).height(58.dp)
                        ) {
                            Icon(
                                imageVector = if (student.active) Icons.Outlined.ToggleOn else Icons.Outlined.ToggleOff,
                                contentDescription = if (student.active) "فعال؛ لمس برای غیرفعال" else "غیرفعال؛ لمس برای فعال",
                                tint = if (student.active) Color(0xFF19945B) else Color(0xFFD63B49),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        IconButton(onClick = onEdit, modifier = Modifier.weight(1f).height(58.dp)) {
                            Icon(
                                Icons.Outlined.Edit,
                                contentDescription = "ویرایش دانش‌آموز",
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        }
                        IconButton(
                            onClick = { classPickerOpen = !classPickerOpen },
                            modifier = Modifier.weight(1f).height(58.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Add,
                                contentDescription = "افزودن به کلاس‌ها",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                copyStudentInformation(
                                    context,
                                    student,
                                    knownPasswordOf(student.username)
                                )
                            },
                            modifier = Modifier.weight(1f).height(58.dp)
                        ) {
                            Icon(
                                Icons.Outlined.ContentCopy,
                                contentDescription = "کپی اطلاعات دانش‌آموز",
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        if (membershipOnlyDelete || student.canManageAccount) {
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier.weight(1f).height(58.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Delete,
                                    contentDescription = if (membershipOnlyDelete) "حذف از کلاس" else "حذف حساب دانش‌آموز",
                                    tint = Color(0xFFD63B49),
                                    modifier = Modifier.size(30.dp)
                                )
                            }
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
    teachers: List<SchoolTeacherPick> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (String, String, String, String?) -> Unit
) {
    var name by remember(item?.id) { mutableStateOf(item?.name.orEmpty()) }
    var grade by remember(item?.id) { mutableStateOf(item?.grade.orEmpty()) }
    var field by remember(item?.id) { mutableStateOf(item?.fieldOfStudy.orEmpty()) }
    // V61.0 — انتخاب معلم (فقط مدیر هنگام ساخت کلاس).
    var teacherId by remember(item?.id) { mutableStateOf<String?>(null) }
    var teacherPickerOpen by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "کلاس جدید" else "ویرایش کلاس") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("نام کلاس") })
                if (teachers.isNotEmpty()) {
                    // کادر وسط‌چین «معلم»: لمس آن لیست معلم‌های عضو مدرسه را باز می‌کند.
                    Card(
                        Modifier.fillMaxWidth().clickable { teacherPickerOpen = true }
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("معلم", style = MaterialTheme.typography.labelSmall)
                            Text(
                                teachers.firstOrNull { it.id == teacherId }?.name
                                    ?: "انتخاب معلم کلاس",
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GradeOdometerPicker(
                        value = grade,
                        onValueChange = { grade = it },
                        modifier = Modifier.weight(1f),
                        emptyLabel = "بدون پایه"
                    )
                    FieldOfStudyPicker(
                        value = field,
                        onValueChange = { field = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, grade, field, teacherId) },
                enabled = name.isNotBlank() && (teachers.isEmpty() || teacherId != null)
            ) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
    if (teacherPickerOpen) {
        AlertDialog(
            onDismissRequest = { teacherPickerOpen = false },
            title = { Text("انتخاب معلم") },
            text = {
                LazyColumn(Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (teachers.isEmpty()) item { Text("معلمی در مدرسه نیست.") }
                    items(teachers, key = SchoolTeacherPick::id) { teacher ->
                        Card(
                            Modifier.fillMaxWidth().clickable {
                                teacherId = teacher.id
                                teacherPickerOpen = false
                            }
                        ) {
                            Text(
                                teacher.name.ifBlank { "معلم" },
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { teacherPickerOpen = false }) { Text("انصراف") } }
        )
    }
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
    var field by remember { mutableStateOf<String?>(null) }
    val grades = remember(students) {
        students.mapNotNull { it.grade?.trim()?.takeIf(String::isNotBlank) }.distinct().sorted()
    }
    val fields = remember(students) {
        students.mapNotNull { it.fieldOfStudy?.trim()?.takeIf(String::isNotBlank) }.distinct().sorted()
    }
    val visible = students.filter { student ->
        (gender == null || student.gender?.lowercase() == gender) &&
            (grade == null || student.grade?.trim() == grade) &&
            (field == null || student.fieldOfStudy?.trim() == field)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("افزودن موجود") },
        text = {
            LazyColumn(Modifier.heightIn(max = 480.dp)) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = gender == null,
                            onClick = { gender = null },
                            label = { Text("همه") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = gender == "female",
                            onClick = {
                                gender = if (gender == "female") null else "female"
                            },
                            label = { Text("دختر") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = gender == "male",
                            onClick = {
                                gender = if (gender == "male") null else "male"
                            },
                            label = { Text("پسر") },
                            modifier = Modifier.weight(1f)
                        )
                        GradeOdometerPicker(
                            value = grade.orEmpty(),
                            onValueChange = { grade = it.takeIf(String::isNotBlank) },
                            availableGrades = grades,
                            includeStandardGrades = false,
                            emptyLabel = "همه پایه‌ها",
                            modifier = Modifier.weight(1.45f)
                        )
                    }
                }
                item {
                    FieldOfStudyPicker(
                        value = field.orEmpty(),
                        onValueChange = { field = it.takeIf(String::isNotBlank) },
                        availableFields = fields,
                        includeStandardFields = false,
                        emptyLabel = "همه رشته‌ها",
                        modifier = Modifier.fillMaxWidth()
                    )
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
                        Text(
                            "${student.fullName} · پایه ${student.grade.orEmpty().ifBlank { "—" }}" +
                                student.fieldOfStudy?.takeIf(String::isNotBlank)?.let { " · $it" }.orEmpty()
                        )
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
    currentPassword: String?,
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
    var field by remember(student.id) { mutableStateOf(student.fieldOfStudy.orEmpty()) }
    var newPassword by remember(student.id) { mutableStateOf("") }
    // یک چشم مرکزی، هر دو کادر رمز جدید و فعلی را همزمان پیدا/پنهان می‌کند.
    var passwordVisible by remember(student.id) { mutableStateOf(false) }

    // پنجرهٔ ویرایش هم‌عرض پنجرهٔ گروهی و از بالا است؛ نوار بالایی شامل ضربدر
    // قرمز، چشم مشترک رمزها و تیک سبز است و فیلدها با اطلاعات دانش‌آموز پیش‌پرند.
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val dialogView = LocalView.current
        DisposableEffect(dialogView) {
            val window = (dialogView.parent as? DialogWindowProvider)?.window
            val previousMode = window?.attributes?.softInputMode
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            onDispose {
                if (previousMode != null) window?.setSoftInputMode(previousMode)
            }
        }
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().imePadding().padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            val availableHeight = maxHeight
            Surface(
                modifier = Modifier.fillMaxWidth().widthIn(max = 620.dp).heightIn(max = availableHeight),
                shape = MaterialTheme.shapes.large,
                tonalElevation = 6.dp
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(color = Color(0xFFE5484D), shape = MaterialTheme.shapes.medium) {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    Icons.Outlined.Close,
                                    contentDescription = "انصراف",
                                    tint = Color.White
                                )
                            }
                        }
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (passwordVisible) "پنهان‌کردن رمزها" else "نمایش رمزها"
                            )
                        }
                        Surface(color = Color(0xFF25A86B), shape = MaterialTheme.shapes.medium) {
                            IconButton(
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
                                            field,
                                            newPassword.takeIf(String::isNotBlank)
                                        )
                                    )
                                }
                            ) {
                                Icon(
                                    Icons.Outlined.Check,
                                    contentDescription = "ذخیره",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                    Card(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // نام و نام خانوادگی در یک سطر
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    first,
                                    { first = it.take(100) },
                                    label = { Text("نام") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    last,
                                    { last = it.take(100) },
                                    label = { Text("نام خانوادگی") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // نام پدر و نام کاربری در یک سطر
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    fatherName,
                                    { fatherName = it.take(100) },
                                    label = { Text("نام پدر") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
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
                            }
                            // پایه و رشته در یک سطر
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                GradeOdometerPicker(
                                    value = grade,
                                    onValueChange = { grade = it.take(100) },
                                    modifier = Modifier.weight(1f),
                                    emptyLabel = "بدون پایه"
                                )
                                FieldOfStudyPicker(
                                    value = field,
                                    onValueChange = { field = it.take(100) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // رمز جدید اختیاری و رمز فعلی همین نشست در یک سطر.
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    newPassword,
                                    { newPassword = it.take(72) },
                                    label = { Text("رمز جدید اختیاری") },
                                    visualTransformation = passwordTransformation(passwordVisible),
                                    textStyle = MaterialTheme.typography.titleMedium,
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).height(64.dp)
                                )
                                OutlinedTextField(
                                    value = currentPassword.orEmpty(),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("رمز فعلی") },
                                    visualTransformation = passwordTransformation(passwordVisible),
                                    textStyle = MaterialTheme.typography.titleMedium,
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).height(64.dp)
                                )
                            }
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                FilterChip(
                                    selected = gender == "female",
                                    onClick = { gender = "female" },
                                    label = { Text("دختر") },
                                    colors = genderFilterChipColors(Color(0xFFFF5C9A)),
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = gender == "male",
                                    onClick = { gender = "male" },
                                    label = { Text("پسر") },
                                    colors = genderFilterChipColors(Color(0xFF3B9EFF)),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedButton(
                                    onClick = { newPassword = generatePassword(10) },
                                    modifier = Modifier.weight(1f)
                                ) { Text("🎲") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun genderFilterChipColors(selectedColor: Color) = FilterChipDefaults.filterChipColors(
    selectedContainerColor = selectedColor,
    selectedLabelColor = Color.White
)

private data class BulkStudentDraft(
    val first: String = "",
    val last: String = "",
    val username: String = "",
    val password: String = generatePassword(10),
    val passwordVisible: Boolean = false,
    val gender: String = "",
    val father: String = "",
    val grade: String = "",
    val field: String = "",
    val usernameEdited: Boolean = false
)

@Composable
private fun BulkStudentDialog(
    onDismiss: () -> Unit,
    onCreate: (List<NewStudentRequest>) -> Unit
) {
    val rows = remember { mutableStateListOf(BulkStudentDraft()) }
    var error by remember { mutableStateOf<String?>(null) }
    // فقط یک کارت در هر لحظه دیده می‌شود؛ «+» کارت تازه را جایگزین کارت قبلی
    // می‌کند و پنجره هرگز بزرگ نمی‌شود. زیر دکمه‌ها فقط شمارهٔ کارت‌ها فهرست می‌شود
    // و دانش‌آموزها بدون نیاز به انتخاب کلاس ثبت می‌شوند.
    var activeIndex by remember { mutableIntStateOf(0) }

    fun rowComplete(row: BulkStudentDraft): Boolean =
        row.first.isNotBlank() && row.username.length >= 4 &&
            row.password.length >= 8 && row.gender in setOf("male", "female")

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
                    row.field,
                    null
                )
            }
            require(requests.map { it.username }.distinct().size == requests.size) {
                "نام کاربری تکراری در ردیف‌ها وجود دارد."
            }
            requests
        }.onSuccess { onCreate(it) }
            .onFailure { error = it.message }
    }

    // پنجره گروهی دقیقاً مانند پنجره تکی: هم‌عرض ۶۲۰dp، از بالا، بدون کشیدن به کل ارتفاع.
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val dialogView = LocalView.current
        DisposableEffect(dialogView) {
            val window = (dialogView.parent as? DialogWindowProvider)?.window
            val previousMode = window?.attributes?.softInputMode
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            onDispose {
                if (previousMode != null) window?.setSoftInputMode(previousMode)
            }
        }
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().imePadding().padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            val availableHeight = maxHeight
            Surface(
                modifier = Modifier.fillMaxWidth().widthIn(max = 620.dp).heightIn(max = availableHeight),
                shape = MaterialTheme.shapes.large,
                tonalElevation = 6.dp
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                                    activeIndex = rows.lastIndex
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
                    // شمارهٔ کارت‌ها به چپ/راست اسکرول می‌شوند و با اسکرول خودکار،
                    // شمارهٔ کارت فعال همیشه در دید قرار می‌گیرد؛ بدون کلاس.
                    val numberListState = rememberLazyListState()
                    LaunchedEffect(activeIndex, rows.size) {
                        if (rows.isNotEmpty()) numberListState.animateScrollToItem(activeIndex)
                    }
                    LazyRow(
                        state = numberListState,
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(rows.size) { index ->
                            FilterChip(
                                selected = activeIndex == index,
                                onClick = { activeIndex = index },
                                label = {
                                    Text(
                                        PersianDigits.convert(index + 1) +
                                            if (rowComplete(rows[index])) " ✓" else ""
                                    )
                                }
                            )
                        }
                    }
                    val index = activeIndex
                    val row = rows[index]
                    Card(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // نام و نام خانوادگی در یک سطر
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
                            // نام پدر و نام کاربری در یک سطر
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    row.father,
                                    { rows[index] = row.copy(father = it.take(100)) },
                                    label = { Text("نام پدر") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
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
                            }
                            // پایه و رشته در یک سطر
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                GradeOdometerPicker(
                                    value = row.grade,
                                    onValueChange = {
                                        rows[index] = row.copy(grade = it.take(100))
                                    },
                                    modifier = Modifier.weight(1f),
                                    emptyLabel = "بدون پایه"
                                )
                                FieldOfStudyPicker(
                                    value = row.field,
                                    onValueChange = {
                                        rows[index] = row.copy(field = it.take(100))
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // رمز و رمز فعلی در یک سطر؛ رمز فعلی همان رمز تعیین‌شده را
                            // نگه می‌دارد و با تغییر رمز خودکار به‌روز می‌شود.
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    row.password,
                                    { rows[index] = row.copy(password = it.take(72)) },
                                    label = { Text("رمز") },
                                    visualTransformation = passwordTransformation(row.passwordVisible),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = row.password,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("رمز فعلی") },
                                    visualTransformation = passwordTransformation(row.passwordVisible),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // V61.0 — ترتیب درخواستی وسط‌چین: چشم، پسر، دختر، تاس.
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PasswordVisibilityButton(
                                    visible = row.passwordVisible,
                                    onToggle = {
                                        rows[index] = row.copy(
                                            passwordVisible = !row.passwordVisible
                                        )
                                    }
                                )
                                FilterChip(
                                    selected = row.gender == "male",
                                    onClick = { rows[index] = row.copy(gender = "male") },
                                    label = { Text("پسر") },
                                    colors = genderFilterChipColors(Color(0xFF3B9EFF)),
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = row.gender == "female",
                                    onClick = { rows[index] = row.copy(gender = "female") },
                                    label = { Text("دختر") },
                                    colors = genderFilterChipColors(Color(0xFFFF5C9A)),
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
                                            activeIndex = (index - 1).coerceAtLeast(0)
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("حذف") }
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

internal fun studentClipboardText(
    student: StudentProfile,
    oneTimePassword: String? = null,
    currentPassword: String? = null
): String {
    val firstName = student.firstName.orEmpty().ifBlank {
        student.fullName.substringBefore(' ').ifBlank { "—" }
    }
    val lastName = student.lastName.orEmpty().ifBlank {
        student.fullName.substringAfter(' ', "").ifBlank { "—" }
    }
    val password = oneTimePassword ?: currentPassword
    // قرارداد V35: فقط همین هشت مورد و دقیقاً با همین ترتیب کپی می‌شوند.
    return buildList {
        add("نام: $firstName")
        add("نام خانوادگی: $lastName")
        add("نام پدر: ${student.fatherName.orEmpty().ifBlank { "—" }}")
        add("پایه: ${student.grade.orEmpty().ifBlank { "—" }}")
        add("رشته: ${student.fieldOfStudy.orEmpty().ifBlank { "—" }}")
        add("نام کاربری: ${student.username.orEmpty().ifBlank { "—" }}")
        add("رمز: ${password?.takeIf(String::isNotBlank) ?: "—"}")
        add("کلاس‌ها: ${student.classNames.orEmpty().ifBlank { "—" }}")
    }.joinToString("\n")
}

private fun copyStudentInformation(
    context: Context,
    student: StudentProfile,
    currentPassword: String? = null
) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    if (clipboard == null) {
        Toast.makeText(context, "حافظه موقت دستگاه در دسترس نیست.", Toast.LENGTH_SHORT).show()
        return
    }
    val text = studentClipboardText(student, currentPassword = currentPassword)
    val clip = ClipData.newPlainText("اطلاعات دانش‌آموز", text)
    if (currentPassword != null) {
        clip.description.extras = PersistableBundle().apply {
            putBoolean("android.content.extra.IS_SENSITIVE", true)
        }
    }
    clipboard.setPrimaryClip(clip)
    Toast.makeText(
        context,
        if (currentPassword != null) "اطلاعات و رمز فعلی به‌صورت حساس کپی شد."
        else "اطلاعات دانش‌آموز کپی شد.",
        Toast.LENGTH_LONG
    ).show()
}

private fun copyOneTimeCredential(
    context: Context,
    credential: StudentCredential,
    student: StudentProfile?
) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    val text = student?.let { studentClipboardText(it, credential.password) }
        ?: "نام کاربری: ${credential.username}\nرمز جدید یک‌بارنمایش: ${credential.password}"
    val clip = ClipData.newPlainText("اطلاعات و رمز جدید دانش‌آموز", text)
    clip.description.extras = PersistableBundle().apply {
        putBoolean("android.content.extra.IS_SENSITIVE", true)
    }
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "اطلاعات و رمز جدید به‌صورت حساس کپی شد.", Toast.LENGTH_LONG).show()
}

private fun studentWorkbook(students:List<StudentProfile>):ByteArray=XlsxWorkbook.build(listOf(XlsxSheet("دانش‌آموزان",listOf(listOf("نام","نام کاربری","جنسیت","پایه","رشته","نام پدر","کلاس","وضعیت"))+students.map{listOf(it.fullName,it.username.orEmpty(),it.gender.orEmpty(),it.grade.orEmpty(),it.fieldOfStudy.orEmpty(),it.fatherName.orEmpty(),it.classNames.orEmpty(),if(it.active)"فعال" else "غیرفعال") })))
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
