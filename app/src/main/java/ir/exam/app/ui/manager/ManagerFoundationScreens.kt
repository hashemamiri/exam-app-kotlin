package ir.exam.app.ui.manager

import ir.exam.app.core.cache.SessionCache
import ir.exam.app.core.calendar.PersianDigits
import ir.exam.app.core.network.UserFacingError
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ToggleOff
import androidx.compose.material.icons.outlined.ToggleOn
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.postgrest.postgrest
import ir.exam.app.data.remote.SupabaseProvider
import ir.exam.app.ui.app.Design69Icons
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

private data class TeacherActivity(
    val name: String, val exams: Int, val classes: Int, val students: Int, val walletBalance: Long
)
private data class ManagerSummary(
    val schoolName: String,
    val province: String,
    val city: String,
    val teachers: Int,
    val students: Int,
    val classes: Int,
    val exams: Int,
    val answers: Int = 0,
    val averagePercent: Double = 0.0,
    val distributedToman: Long = 0,
    val teacherActivity: List<TeacherActivity> = emptyList()
)

// V62.8 — FlowRow چیپ‌های منعطف نام مدرسه در دیالوگ کد دعوت.
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ManagerTeachersScreen(
    newTeacherRequested: Int = 0,
    teacherListRequested: Int = 0,
    inviteModeRequested: Boolean = false,
    onManageTeacher: (String) -> Unit = {},
    onInviteModeChanged: (Boolean) -> Unit = {},
    // V62.6 — بازگشت از پنجرهٔ کد دعوت به داشبورد (به‌جای لیست معلم‌ها).
    onInviteBack: (() -> Unit)? = null
) {
    val summaryState = rememberManagerSummary()
    val repository = remember { ir.exam.app.data.repository.SupabaseManagerRepository() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var teachers by remember { mutableStateOf<List<ir.exam.app.data.repository.SchoolTeacherItem>>(emptyList()) }
    var invites by remember { mutableStateOf<List<ir.exam.app.data.repository.ManagerInviteItem>>(emptyList()) }
    // V61.7 — کارت دعوتی که منتظر تأیید حذف است.
    var deleteInviteTarget by remember { mutableStateOf<ir.exam.app.data.repository.ManagerInviteItem?>(null) }
    var expandedTeacher by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var transferDone by remember { mutableStateOf<String?>(null) } // V202
    var loading by remember { mutableStateOf(true) }
    var inviteMode by remember { mutableStateOf(false) }
    var inviteCountDialog by remember { mutableStateOf(false) }
    var inviteCount by remember { mutableStateOf(1) }
    // V62.6 — انتخاب مدرسهٔ مقصد کد دعوت (مدیر چندمدرسه‌ای).
    var inviteSchools by remember { mutableStateOf<List<ir.exam.app.data.repository.ManagerSchoolOption>>(emptyList()) }
    var inviteSchoolId by remember { mutableStateOf<String?>(null) }
    // V336 — فیلتر مدرسه برای لیست معلم‌ها؛ فعال/غیرفعال و حذف فقط روی عضویت همان مدرسه اثر می‌گذارد.
    var schoolFilterId by remember { mutableStateOf<String?>(null) }
    var transferTarget by remember { mutableStateOf<ir.exam.app.data.repository.SchoolTeacherItem?>(null) }
    var transferAmount by remember { mutableStateOf("") }
    var removeTarget by remember { mutableStateOf<ir.exam.app.data.repository.SchoolTeacherItem?>(null) }
    var clockNow by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(inviteMode) {
        while (inviteMode) { clockNow = System.currentTimeMillis(); delay(1_000) }
    }

    fun reloadTeachers() {
        scope.launch {
            loading = true
            val result = schoolFilterId?.let { repository.schoolTeachers(it) } ?: repository.teachers()
            result.onSuccess { teachers = it; error = null }
                .onFailure { error = safeManagerError(it) }
            loading = false
        }
    }
    fun reloadInvites() {
        scope.launch {
            loading = true
            repository.invites().onSuccess { invites = it; error = null }
                .onFailure { error = safeManagerError(it) }
            loading = false
        }
    }
    LaunchedEffect(inviteModeRequested, newTeacherRequested, teacherListRequested) {
        inviteMode = inviteModeRequested
        if (inviteModeRequested) reloadInvites() else reloadTeachers()
        if (!inviteModeRequested && inviteSchools.isEmpty()) {
            repository.managerSchools().onSuccess { list ->
                inviteSchools = list
                if (schoolFilterId == null) {
                    schoolFilterId = list.firstOrNull()?.id
                    reloadTeachers()
                }
            }.onFailure {}
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (inviteMode) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.IconButton(onClick = {
                    inviteMode = false
                    onInviteModeChanged(false)
                    // V62.6 — بازگشت از کد دعوت: داشبورد باز شود.
                    onInviteBack?.invoke() ?: reloadTeachers()
                }) {
                    androidx.compose.material3.Icon(Icons.AutoMirrored.Outlined.ArrowBack, "بازگشت")
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                androidx.compose.material3.Button(onClick = {
                    // V62.6 — قبل از ساخت، لیست مدارس مدیر برای انتخاب مقصد.
                    scope.launch {
                        repository.managerSchools().onSuccess { list ->
                            inviteSchools = list
                            inviteSchoolId = list.firstOrNull()?.id
                        }.onFailure { error = safeManagerError(it) }
                    }
                    inviteCountDialog = true
                }) { Text("ساخت کد دعوت") }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (loading) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            invites.forEach { invite ->
                val remaining = inviteRemainingText(invite.expiresAt, invite.used, invite.revoked, clockNow, invite.usedAt)
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(invite.code, style = MaterialTheme.typography.titleLarge)
                            Text(remaining)
                            val expired = remaining.endsWith("00:00:00")
                            androidx.compose.material3.FilterChip(
                                selected = true,
                                onClick = {},
                                label = {
                                    Text(
                                        when { invite.used -> "استفاده شده"; expired -> "منقضی شده"; else -> "استفاده نشده" },
                                        fontWeight = if (invite.used || expired) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                                    )
                                },
                                colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = when { invite.used -> androidx.compose.ui.graphics.Color(0xFF25A86B); expired -> androidx.compose.ui.graphics.Color(0xFFE5484D); else -> MaterialTheme.colorScheme.surfaceVariant },
                                    selectedLabelColor = if (invite.used || expired) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                        // V61.5 — سطل زباله روی همهٔ کارت‌ها؛ V61.7 — قبل از حذف
                        // دیالوگ تأیید باز می‌شود (deleteInviteTarget).
                        androidx.compose.material3.IconButton(onClick = { deleteInviteTarget = invite }) {
                            androidx.compose.material3.Icon(
                                Icons.Outlined.Delete,
                                contentDescription = "حذف کد دعوت",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        } else {
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            if (inviteSchools.size > 1) {
                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                ) {
                    inviteSchools.forEach { school ->
                        androidx.compose.material3.FilterChip(
                            selected = schoolFilterId == school.id,
                            onClick = { schoolFilterId = school.id; reloadTeachers() },
                            label = { Text(school.name.ifBlank { "مدرسه" }) }
                        )
                    }
                }
            }
            if (loading) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            teachers.forEach { teacher ->
                Card(
                    // V62.6 — گزارش دستگاه: کادر خاکستری با لمس کارت معلم؛ ریپل
                    // پیش‌فرض clickable حذف شد (همان الگوی تب‌های V62.1.4).
                    Modifier.fillMaxWidth().clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) {
                        expandedTeacher = if (expandedTeacher == teacher.id) null else teacher.id
                    }
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(teacher.fullName.ifBlank { "بدون نام" }, style = MaterialTheme.typography.titleMedium)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("کد پرسنلی: ${teacher.employeeCode.ifBlank { "—" }}", Modifier.weight(1f))
                            Text("شماره تلفن: ${teacher.phone.ifBlank { "—" }}", Modifier.weight(1f))
                        }
                        androidx.compose.animation.AnimatedVisibility(expandedTeacher == teacher.id) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                androidx.compose.material3.IconButton(onClick = {
                                    scope.launch {
                                        repository.setTeacherActive(teacher.id, !teacher.active, schoolFilterId)
                                            .onSuccess { reloadTeachers() }
                                            .onFailure { error = safeManagerError(it) }
                                    }
                                }) {
                                    androidx.compose.material3.Icon(
                                        if (teacher.active) Icons.Outlined.ToggleOn
                                        else Icons.Outlined.ToggleOff,
                                        contentDescription = if (teacher.active) "غیرفعال‌کردن" else "فعال‌کردن",
                                        tint = if (teacher.active) androidx.compose.ui.graphics.Color(0xFF25A86B) else androidx.compose.ui.graphics.Color(0xFFE5484D),
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                                androidx.compose.material3.IconButton(onClick = { onManageTeacher(teacher.id) }) {
                                    androidx.compose.material3.Icon(Icons.AutoMirrored.Outlined.Login, "ورود به مدیریت معلم", modifier = Modifier.size(32.dp))
                                }
                                androidx.compose.material3.IconButton(onClick = { transferTarget = teacher; transferAmount = "" }) {
                                    androidx.compose.material3.Icon(Icons.Outlined.AccountBalanceWallet, "شارژ کیف پول", modifier = Modifier.size(32.dp))
                                }
                                androidx.compose.material3.IconButton(onClick = { removeTarget = teacher }) {
                                    androidx.compose.material3.Icon(Icons.Outlined.Delete, "حذف معلم", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (inviteCountDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { inviteCountDialog = false },
            title = { Text("تعداد کد دعوت") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..5).forEach { count ->
                            androidx.compose.material3.FilterChip(
                                selected = inviteCount == count,
                                onClick = { inviteCount = count },
                                label = { Text(count.toString()) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    // V62.6 — انتخاب مدرسهٔ مقصد: معلم با این کد عضو کدام مدرسه شود؟
                    Text("معلم به کدام مدرسه بپیوندد؟")
                    if (inviteSchools.isEmpty()) Text("مدرسه‌ای یافت نشد.")
                    // V62.8 — دکمه‌های منعطف: هر چیپ به اندازهٔ نام مدرسه و
                    // ردیف‌ها وسط‌چین (FlowRow به‌جای چیپ‌های تمام‌عرض).
                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                    ) {
                        inviteSchools.forEach { school ->
                            androidx.compose.material3.FilterChip(
                                selected = inviteSchoolId == school.id,
                                onClick = { inviteSchoolId = school.id },
                                label = { Text(school.name.ifBlank { "مدرسه" }) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    enabled = inviteSchoolId != null,
                    onClick = {
                        inviteCountDialog = false
                        scope.launch {
                            repository.createInvites(inviteCount, inviteSchoolId).onSuccess { reloadInvites() }
                                .onFailure { error = safeManagerError(it) }
                        }
                    }
                ) { Text("تأیید") }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { inviteCountDialog = false }) { Text("انصراف") } }
        )
    }
    transferDone?.let { msg ->
        ir.exam.app.ui.common.CostDeductedDialog(message = msg, onConfirm = { transferDone = null })
    }
    transferTarget?.let { teacher ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { transferTarget = null },
            title = { Text("شارژ کیف پول ${teacher.fullName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("مبلغ باید مضرب ۱٬۰۰۰ تومان باشد؛ مانند ۲۷٬۰۰۰ تومان.")
                    androidx.compose.material3.OutlinedTextField(
                        value = transferAmount,
                        onValueChange = { transferAmount = it.filter(Char::isDigit).take(9) },
                        label = { Text("مبلغ انتقال (تومان)") }, singleLine = true
                    )
                }
            },
            confirmButton = {
                val amount = transferAmount.toLongOrNull()
                androidx.compose.material3.Button(
                    enabled = ir.exam.app.domain.model.ManagerWalletRules.isValidTransfer(amount),
                    onClick = {
                        scope.launch {
                            repository.transferWallet(teacher.id, amount ?: 0).onSuccess { result ->
                                /* V202 — پیام کسر وسط صفحه؛ فقط با «تأیید» بسته می‌شود */
                                transferDone = "کسر ${"%,d".format(java.util.Locale.US, result.amountToman)} تومان از کیف پول شما با موفقیت انجام شد و به ${teacher.fullName} منتقل شد."
                                transferTarget = null; reloadTeachers()
                            }.onFailure { error = safeManagerError(it); transferTarget = null }
                        }
                    }
                ) { Text("انتقال") }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { transferTarget = null }) { Text("انصراف") } }
        )
    }
    removeTarget?.let { teacher ->
        var removeSchoolId by remember(teacher.id) { mutableStateOf<String?>(schoolFilterId) }
        LaunchedEffect(teacher.id) {
            if (inviteSchools.isEmpty()) {
                repository.managerSchools().onSuccess { inviteSchools = it }.onFailure {}
            }
        }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { removeTarget = null },
            title = { Text("حذف معلم از مدرسه") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("عضویت ${teacher.fullName} حذف شود؟ حساب Auth و آزمون‌های شخصی او باقی می‌مانند.")
                    if (inviteSchools.size > 1) {
                        androidx.compose.foundation.layout.FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                        ) {
                            androidx.compose.material3.FilterChip(
                                selected = removeSchoolId == null,
                                onClick = { removeSchoolId = null },
                                label = { Text("همهٔ مدارس") }
                            )
                            inviteSchools.forEach { school ->
                                androidx.compose.material3.FilterChip(
                                    selected = removeSchoolId == school.id,
                                    onClick = { removeSchoolId = school.id },
                                    label = { Text(school.name.ifBlank { "مدرسه" }) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.Button(onClick = {
                    scope.launch {
                        repository.removeTeacher(teacher.id, removeSchoolId).onSuccess { removeTarget = null; reloadTeachers() }
                            .onFailure { error = safeManagerError(it); removeTarget = null }
                    }
                }) { Text("حذف عضویت") }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { removeTarget = null }) { Text("انصراف") } }
        )
    }
    // V61.7 — تأیید حذف کارت کد دعوت؛ کد استفاده‌نشده با حذف بلافاصله باطل می‌شود.
    deleteInviteTarget?.let { invite ->
        val wasUnused = !invite.used && !invite.revoked
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deleteInviteTarget = null },
            title = { Text("حذف کارت کد دعوت") },
            text = {
                Text(
                    if (wasUnused) {
                        "کارت «${invite.code}» حذف شود؟ این کد استفاده نشده و با حذف، بلافاصله منقضی می‌شود."
                    } else "کارت «${invite.code}» از لیست حذف شود؟"
                )
            },
            confirmButton = {
                androidx.compose.material3.Button(onClick = {
                    deleteInviteTarget = null
                    invites = invites.filterNot { it.id == invite.id }
                    message = if (wasUnused) {
                        "کارت حذف شد و کد استفاده‌نشده بلافاصله منقضی شد."
                    } else "کارت حذف شد."
                    scope.launch {
                        // V61.8 — ریشهٔ «کارت حذف نمی‌شود»: revoke فقط کد را باطل
                        // می‌کرد و لیست سرور کارت را برمی‌گرداند؛ حالا سطر واقعاً
                        // حذف می‌شود (برای همهٔ کارت‌ها) و اگر تابع حذف نبود،
                        // repository.revokeInvite حداقل کد را باطل می‌کند.
                        repository.deleteInvite(invite.id)
                            .onFailure { error = "کارت حذف شد؛ حذف سروری ناموفق بود: ${safeManagerError(it)}" }
                    }
                }) { Text("بله، حذف شود") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { deleteInviteTarget = null }) { Text("انصراف") }
            }
        )
    }
}

private fun parseInviteInstant(value: String): Long? {
    // V61.8 — used_at پستگرس با «فاصله» می‌آید (2026-08-26 07:15:00+00) و
    // هر سه parser قبلی شکست می‌خوردند → زمان‌سنج منجمد نمایش داده نمی‌شد.
    // نرمال‌سازی: فاصله→T و offset بدون دقیقه (+00) → +00:00.
    val clean = value.trim().replace(' ', 'T').let {
        if (Regex("[+-]\\d{2}$").containsMatchIn(it)) "$it:00" else it
    }
    return runCatching { java.time.Instant.parse(clean).toEpochMilli() }
        .recoverCatching { java.time.OffsetDateTime.parse(clean).toInstant().toEpochMilli() }
        .recoverCatching { java.time.LocalDateTime.parse(clean).toInstant(java.time.ZoneOffset.UTC).toEpochMilli() }
        .getOrNull()
}

private fun inviteRemainingText(
    expiresAt: String,
    used: Boolean,
    revoked: Boolean,
    now: Long,
    usedAt: String = ""
): String {
    // V61.7 — کدِ استفاده‌شده زمان‌سنجِ «منجمد» را نشان می‌دهد: زمان باقی‌مانده
    // در لحظهٔ استفاده، بدون ادامهٔ شمارش.
    if (used) {
        val expiry = parseInviteInstant(expiresAt)
        val usedMoment = parseInviteInstant(usedAt)
        if (expiry != null && usedMoment != null) {
            val seconds = ((expiry - usedMoment) / 1_000).coerceAtLeast(0)
            val hours = seconds / 3_600
            val minutes = (seconds % 3_600) / 60
            val rest = seconds % 60
            return "زمان‌سنج متوقف شد: %02d:%02d:%02d".format(hours, minutes, rest)
        }
        return "کد استفاده شد؛ زمان‌سنج متوقف شد."
    }
    if (revoked) return "کد باطل شده است."
    val expiry = parseInviteInstant(expiresAt)
    if (expiry == null) return "زمان انقضا نامعتبر است"
    val seconds = ((expiry - now) / 1_000).coerceAtLeast(0)
    if (seconds == 0L) return "زمان باقی‌مانده: 00:00:00"
    val hours = seconds / 3_600
    val minutes = (seconds % 3_600) / 60
    val rest = seconds % 60
    return "زمان باقی‌مانده: %02d:%02d:%02d".format(hours, minutes, rest)
}

// V61.9 — ManagerCardsScreen سادهٔ V61.6 حذف شد؛ کارت‌های مدیر حالا با پشتهٔ
// کارتی مشترک معلم (ManagerManagementCardsScreen در TeacherManagementCardsScreen.kt)
// نمایش داده می‌شوند: مدارس، کارنامه، وضعیت.

@Composable
fun ManagerStatsScreen(
    // V62.6 — دو منوی مستقل: "status" داشبورد با پنل سریع، "report" کارنامه
    // با میان‌برهای گزارش خودش (قبلاً هر دو یک صفحه بودند).
    section: String = "status",
    onQuickTeachers: (() -> Unit)? = null,
    onQuickClasses: (() -> Unit)? = null,
    onQuickStudents: (() -> Unit)? = null,
    onQuickWallet: (() -> Unit)? = null,
    onOpenStatus: (() -> Unit)? = null,
    onOpenReport: (() -> Unit)? = null,
    // V337.1 — میان‌بر مدارس از خود داشبورد، کنار بقیهٔ بخش‌های پرکاربرد.
    onQuickSchools: (() -> Unit)? = null
) {
    var summaryRefreshKey by remember { mutableIntStateOf(0) }
    val state = rememberManagerSummary(summaryRefreshKey)
    val reportMode = section == "report"
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(if (reportMode) "کارنامه مدرسه" else "داشبورد", style = MaterialTheme.typography.headlineSmall)
            Text(
                if (reportMode) "پاسخ‌ها، میانگین‌ها و فعالیت معلم‌ها در همهٔ مدارس فعال شما"
                else "نمای کلی مدارس تحت مدیریت شما",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (state.refreshing) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator(Modifier.size(18.dp))
                Text("به‌روزرسانی اطلاعات…", style = MaterialTheme.typography.bodySmall)
            }
        }
        state.notice?.let { notice ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(notice, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { summaryRefreshKey += 1 }) { Text("تلاش دوباره") }
                }
            }
        }
        when {
            state.loading -> Column(
                Modifier.fillMaxWidth().padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator()
                Text("در حال دریافت اطلاعات مدرسه…", style = MaterialTheme.typography.bodySmall)
            }
            state.error != null -> Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { summaryRefreshKey += 1 }) { Text("تلاش دوباره") }
                }
            }
            else -> {
                val summary = checkNotNull(state.summary)
                if (!reportMode) {
                    ManagerDashboardOverview(
                        summary = summary,
                        onQuickSchools = onQuickSchools,
                        onQuickTeachers = onQuickTeachers,
                        onQuickClasses = onQuickClasses,
                        onQuickStudents = onQuickStudents,
                        onQuickWallet = onQuickWallet,
                        onOpenReport = onOpenReport
                    )
                } else {
                    ManagerReportSection(
                        summary = summary,
                        onOpenStatus = onOpenStatus,
                        onQuickTeachers = onQuickTeachers
                    )
                }
                // V263/V337.1 — مقایسهٔ کلاس‌ها در داشبورد جمع‌شده و در کارنامه باز است.
                ManagerClassComparison(expandedByDefault = reportMode)
            }
        }
    }
}

@Composable
private fun ManagerDashboardOverview(
    summary: ManagerSummary,
    onQuickSchools: (() -> Unit)?,
    onQuickTeachers: (() -> Unit)?,
    onQuickClasses: (() -> Unit)?,
    onQuickStudents: (() -> Unit)?,
    onQuickWallet: (() -> Unit)?,
    onOpenReport: (() -> Unit)?
) {
    ManagerSchoolContextCard(summary)
    Text("آمار کلی", style = MaterialTheme.typography.titleMedium)
    val metrics = listOf(
        Triple("معلم", faManagerNumber(summary.teachers), Design69Icons.Students),
        Triple("دانش‌آموز", faManagerNumber(summary.students), Design69Icons.Students),
        Triple("کلاس", faManagerNumber(summary.classes), Design69Icons.Classes),
        Triple("آزمون", faManagerNumber(summary.exams), Design69Icons.Exams)
    )
    metrics.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { (label, value, icon) ->
                ManagerMetricCard(label = label, value = value, icon = icon, modifier = Modifier.weight(1f))
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
    val actions = listOfNotNull(
        onQuickSchools?.let { ManagerQuickAction("مدارس", "فهرست مدارس و کلاس‌ها", Design69Icons.SchoolAdd, it) },
        onQuickTeachers?.let { ManagerQuickAction("معلم‌ها", "دعوت و مدیریت معلم‌ها", Design69Icons.Students, it) },
        onQuickClasses?.let { ManagerQuickAction("کلاس‌ها", "ساخت کلاس و اعضا", Design69Icons.Classes, it) },
        onQuickStudents?.let { ManagerQuickAction("دانش‌آموزان", "فهرست و عضویت کلاس‌ها", Design69Icons.Students, it) },
        onQuickWallet?.let { ManagerQuickAction("کیف پول", "اعتبار و انتقال به معلم‌ها", Design69Icons.Wallet, it) }
    )
    if (actions.isNotEmpty()) {
        Text("پنل سریع", style = MaterialTheme.typography.titleMedium)
        actions.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { action ->
                    ManagerQuickActionCard(
                        title = action.title,
                        subtitle = action.subtitle,
                        icon = action.icon,
                        onClick = action.onClick,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
    onOpenReport?.let { QuickPanelCard("مشاهده کارنامه مدرسه", it, Modifier.fillMaxWidth()) }
}

@Composable
private fun ManagerReportSection(
    summary: ManagerSummary,
    onOpenStatus: (() -> Unit)?,
    onQuickTeachers: (() -> Unit)?
) {
    Text("پنل کارنامه", style = MaterialTheme.typography.titleMedium)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        onOpenStatus?.let { QuickPanelCard("داشبورد", it, Modifier.weight(1f)) }
        onQuickTeachers?.let { QuickPanelCard("معلم‌ها", it, Modifier.weight(1f)) }
    }
    Text("شاخص‌های پاسخ‌دهی", style = MaterialTheme.typography.titleMedium)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ManagerMetricCard("پاسخ‌ها", faManagerNumber(summary.answers), modifier = Modifier.weight(1f))
        ManagerMetricCard("میانگین نمره", faManagerPercent(summary.averagePercent), modifier = Modifier.weight(1f))
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("مجموع اعتبار توزیع‌شده", style = MaterialTheme.typography.labelMedium)
            Text("${faManagerToman(summary.distributedToman)} تومان", style = MaterialTheme.typography.titleMedium)
            Text(
                "میانگین نمره از پاسخ‌های ثبت‌شده محاسبه می‌شود؛ مقایسهٔ کلاس‌ها فقط پاسخ‌های تصحیح‌شده را نشان می‌دهد.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    Text("فعالیت معلم‌ها", style = MaterialTheme.typography.titleMedium)
    if (summary.teacherActivity.isEmpty()) {
        Text("هنوز فعالیت معلمی در مدارس شما ثبت نشده است.", style = MaterialTheme.typography.bodySmall)
    }
    summary.teacherActivity.forEach { item ->
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(item.name.ifBlank { "معلم" }, style = MaterialTheme.typography.titleSmall)
                Text(
                    "آزمون ${faManagerNumber(item.exams)} · کلاس ${faManagerNumber(item.classes)} · دانش‌آموز ${faManagerNumber(item.students)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text("کیف پول ${faManagerToman(item.walletBalance)} تومان", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ManagerSchoolContextCard(summary: ManagerSummary) {
    val multiSchool = summary.schoolName.contains("، ")
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("مدرسه‌های تحت مدیریت", style = MaterialTheme.typography.labelMedium)
            Text(summary.schoolName.ifBlank { "مدرسه" }, style = MaterialTheme.typography.titleMedium)
            if (!multiSchool) {
                val location = listOf(summary.province, summary.city).filter(String::isNotBlank).joinToString(" · ")
                if (location.isNotBlank()) Text(location, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                "آمار این صفحه مجموع همهٔ مدارس فعال شماست.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class ManagerQuickAction(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
private fun ManagerQuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier.clickable(onClick = onClick)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 84.dp).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ManagerMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Card(modifier) {
        Column(
            Modifier.fillMaxWidth().heightIn(min = 104.dp).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            icon?.let { Icon(it, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            Text(value, style = MaterialTheme.typography.headlineMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun faManagerNumber(value: Any?): String = PersianDigits.convert(value)

private fun faManagerToman(value: Long): String =
    PersianDigits.convert("%,d".format(java.util.Locale.US, value))

private fun faManagerPercent(value: Double?): String {
    if (value == null) return "—"
    val text = if (value % 1.0 == 0.0) value.toInt().toString() else String.format(java.util.Locale.US, "%.1f", value)
    return PersianDigits.convert(text).replace('.', '٫') + "٪"
}

/** V263 — مقایسهٔ کلاس‌های مدرسه: میانگین، پراکندگی، زیر ۵۰٪، روند ماهانه و آزمون‌های برتر (RPC native_manager_class_stats_v1). */
@Composable
private fun ManagerClassComparison(expandedByDefault: Boolean = true) {
    var expanded by rememberSaveable(expandedByDefault) { mutableStateOf(expandedByDefault) }
    var loading by remember { mutableStateOf(expanded) }
    var error by remember { mutableStateOf<String?>(null) }
    var stats by remember { mutableStateOf<ManagerClassStats?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(expanded, refreshKey) {
        if (!expanded) return@LaunchedEffect
        loading = true
        error = null
        runCatching {
            val raw = SupabaseProvider.client.postgrest.rpc("native_manager_class_stats_v1").decodeAs<JsonObject>()
            raw["error"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)?.let { message -> throw IllegalStateException(message) }
            fun arr(key: String) = (raw[key] as? kotlinx.serialization.json.JsonArray).orEmpty().mapNotNull { it as? JsonObject }
            fun JsonObject.str(k: String) = this[k]?.jsonPrimitive?.contentOrNull.orEmpty()
            fun JsonObject.int(k: String) = this[k]?.jsonPrimitive?.intOrNull ?: 0
            fun JsonObject.dbl(k: String) = this[k]?.jsonPrimitive?.doubleOrNull ?: 0.0
            ManagerClassStats(
                classes = arr("classes").map {
                    ManagerClassRow(it.str("name"), it.str("grade"), it.str("field"), it.str("school"), it.str("teacher"), it.int("students"), it.int("exams"),
                        it.int("answers"), it.int("graded"), it.dbl("average_percent"), it.dbl("spread_percent"), it.int("below_half"))
                },
                months = arr("months").map { ManagerMonthRow(it.str("month"), it.int("answers"), it.dbl("average_percent")) },
                topExams = arr("top_exams").map { ManagerTopExamRow(it.str("title"), it.str("teacher"), it.int("answers"), it.dbl("average_percent")) }
            )
        }.onSuccess { stats = it; loading = false }
            .onFailure { error = safeManagerError(it); loading = false }
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("مقایسهٔ کلاس‌ها", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "میانگین کلاس‌ها، روند ماهانه و آزمون‌های پرتکرار",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "بستن" else "نمایش") }
            }
            if (!expanded) {
                Text("برای دیدن جزئیات کلاس‌ها، این بخش را باز کنید.", style = MaterialTheme.typography.bodySmall)
            } else when {
                loading || (stats == null && error == null) -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(Modifier.size(18.dp))
                    Text("در حال دریافت مقایسهٔ کلاس‌ها…", style = MaterialTheme.typography.bodySmall)
                }
                error != null -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { refreshKey += 1 }) { Text("تلاش دوباره") }
                }
                else -> stats?.let { data ->
                    if (data.classes.isEmpty()) {
                        Text("هنوز کلاسی در مدارس شما ثبت نشده است.", style = MaterialTheme.typography.bodySmall)
                    }
                    data.classes.forEach { row ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    listOf(row.name, row.grade, row.field).filter { it.isNotBlank() }.joinToString(" · "),
                                    style = MaterialTheme.typography.titleSmall
                                )
                                if (row.school.isNotBlank()) Text("مدرسه: ${row.school}", style = MaterialTheme.typography.bodySmall)
                                if (row.teacher.isNotBlank()) Text("معلم: ${row.teacher}", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "دانش‌آموز ${faManagerNumber(row.students)} · آزمون ${faManagerNumber(row.exams)} · پاسخ ${faManagerNumber(row.answers)} (تصحیح‌شده ${faManagerNumber(row.graded)})",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (row.answers == 0) {
                                    Text("هنوز پاسخی برای این کلاس ثبت نشده است.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    StatChip("میانگین", faManagerPercent(row.averagePercent), Modifier.weight(1f))
                                    StatChip("پراکندگی", "±${faManagerPercent(row.spreadPercent)}", Modifier.weight(1f))
                                    StatChip("زیر ۵۰٪", faManagerNumber(row.belowHalf), Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    Text("روند ماهانه", style = MaterialTheme.typography.titleSmall)
                    if (data.months.isEmpty()) {
                        Text("هنوز پاسخ تصحیح‌شده‌ای برای روند ماهانه ثبت نشده است.", style = MaterialTheme.typography.bodySmall)
                    } else data.months.forEach { m ->
                        Text("${m.month}: ${faManagerNumber(m.answers)} پاسخ · میانگین ${faManagerPercent(m.averagePercent)}", style = MaterialTheme.typography.bodySmall)
                    }
                    Text("آزمون‌های برتر", style = MaterialTheme.typography.titleSmall)
                    if (data.topExams.isEmpty()) {
                        Text("هنوز آزمونی با پاسخ ثبت نشده است.", style = MaterialTheme.typography.bodySmall)
                    } else data.topExams.forEach { e ->
                        Text(
                            "${e.title}${if (e.teacher.isNotBlank()) " (${e.teacher})" else ""}: ${faManagerNumber(e.answers)} پاسخ · میانگین ${faManagerPercent(e.averagePercent)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.fillMaxWidth().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(value, style = MaterialTheme.typography.titleSmall)
        }
    }
}

internal data class ManagerClassRow(
    val name: String, val grade: String, val field: String, val school: String, val teacher: String,
    val students: Int, val exams: Int, val answers: Int, val graded: Int,
    val averagePercent: Double, val spreadPercent: Double, val belowHalf: Int
)
internal data class ManagerMonthRow(val month: String, val answers: Int, val averagePercent: Double)
internal data class ManagerTopExamRow(val title: String, val teacher: String, val answers: Int, val averagePercent: Double)
internal data class ManagerClassStats(val classes: List<ManagerClassRow>, val months: List<ManagerMonthRow>, val topExams: List<ManagerTopExamRow>)

/** V61.0 — کارت میان‌بر پنل سریع داشبورد. */
@Composable
private fun QuickPanelCard(title: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier.clickable(onClick = onClick)) {
        Column(
            Modifier.fillMaxWidth().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) { Text(title, style = MaterialTheme.typography.titleSmall) }
    }
}

private data class ManagerSummaryState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val summary: ManagerSummary? = null,
    val error: String? = null,
    val notice: String? = null
)

@Composable
private fun rememberManagerSummary(refreshKey: Int = 0): ManagerSummaryState {
    // V214 — خلاصهٔ قبلی (حافظهٔ موقت) فوراً نشان داده می‌شود؛ نسخهٔ تازه جایگزین می‌شود.
    val cachedSummary = remember { SessionCache.get<ManagerSummary>(MANAGER_SUMMARY_CACHE) }
    var state by remember { mutableStateOf(ManagerSummaryState(loading = cachedSummary == null, summary = cachedSummary)) }
    LaunchedEffect(refreshKey) {
        state = if (state.summary != null) state.copy(refreshing = true, error = null, notice = null)
        else state.copy(loading = true, error = null, notice = null)
        runCatching {
            val raw = SupabaseProvider.client.postgrest.rpc("native_manager_school_summary_v36")
                .decodeAs<JsonObject>()
            raw["error"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)?.let(::error)
            ManagerSummary(
                schoolName = raw["school_name"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                province = raw["province"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                city = raw["city"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                teachers = raw["teachers"]?.jsonPrimitive?.intOrNull ?: 0,
                students = raw["students"]?.jsonPrimitive?.intOrNull ?: 0,
                classes = raw["classes"]?.jsonPrimitive?.intOrNull ?: 0,
                exams = raw["exams"]?.jsonPrimitive?.intOrNull ?: 0,
                answers = raw["answers"]?.jsonPrimitive?.intOrNull ?: 0,
                averagePercent = raw["average_percent"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                distributedToman = raw["distributed_toman"]?.jsonPrimitive?.longOrNull ?: 0,
                teacherActivity = (raw["teacher_activity"] as? kotlinx.serialization.json.JsonArray).orEmpty().mapNotNull { element ->
                    val item = element as? JsonObject ?: return@mapNotNull null
                    TeacherActivity(
                        name = item["name"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                        exams = item["exams"]?.jsonPrimitive?.intOrNull ?: 0,
                        classes = item["classes"]?.jsonPrimitive?.intOrNull ?: 0,
                        students = item["students"]?.jsonPrimitive?.intOrNull ?: 0,
                        walletBalance = item["wallet_balance"]?.jsonPrimitive?.longOrNull ?: 0
                    )
                }
            )
        }.onSuccess { SessionCache.put(MANAGER_SUMMARY_CACHE, it); state = ManagerSummaryState(loading = false, summary = it) }
            .onFailure {
                state = if (state.summary != null) {
                    state.copy(loading = false, refreshing = false, notice = "به‌روزرسانی ناموفق بود؛ آخرین اطلاعات دریافت‌شده نمایش داده می‌شود.")
                } else {
                    ManagerSummaryState(loading = false, error = safeManagerError(it))
                }
            }
    }
    return state
}

private const val MANAGER_SUMMARY_CACHE = "manager.summary" // V214

internal fun safeManagerError(error: Throwable): String = UserFacingError.of(error, "عملیات مدیریت مدرسه ناموفق بود.") // V209 — بدون متن فنی
