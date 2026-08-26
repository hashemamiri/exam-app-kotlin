package ir.exam.app.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import io.github.jan.supabase.postgrest.postgrest
import ir.exam.app.core.calendar.PersianDigits
import ir.exam.app.core.ui.AppFont
import ir.exam.app.core.ui.AppearanceSettings
import ir.exam.app.core.ui.DeviceLayoutMode
import ir.exam.app.core.ui.NeumorphicPalette
import ir.exam.app.core.ui.ThemeMode
import ir.exam.app.core.ui.accentColors
import ir.exam.app.core.ui.resolveTabletLayout
import ir.exam.app.data.repository.LocalImageRepository
import ir.exam.app.domain.model.AppUser
import ir.exam.app.data.repository.SchoolInvitePreview
import ir.exam.app.data.repository.SupabaseSchoolJoinRepository
import ir.exam.app.domain.model.ImageEditRequest
import ir.exam.app.domain.model.NativeProfile
import ir.exam.app.domain.model.UserRole
import ir.exam.app.ui.common.FieldOfStudyPicker
import ir.exam.app.ui.common.GradeOdometerPicker
import ir.exam.app.ui.common.PasswordVisibilityButton
import ir.exam.app.ui.common.passwordTransformation
import ir.exam.app.ui.image.InteractiveImageEditorDialog
import ir.exam.app.ui.portability.DataPortabilitySection
import ir.exam.app.ui.security.AppLockSettings
import kotlinx.coroutines.launch

enum class ProfileSettingsDestination { PROFILE, HEADER, ACCOUNT, DATA, SETTINGS }
enum class SettingsSection { APPEARANCE, ABOUT }

@Composable
fun ProfileSettingsScreen(
    user: AppUser,
    appearance: AppearanceSettings,
    destination: ProfileSettingsDestination = ProfileSettingsDestination.SETTINGS,
    initialSettingsSection: SettingsSection = SettingsSection.APPEARANCE,
    onProfileUpdated: () -> Unit,
    // V59.3 — پس از حذف موفق حساب: خروج محلی و بازگشت به صفحهٔ ورود/ثبت‌نام.
    onAccountDeleted: () -> Unit = {},
    onImportExam: (ir.exam.app.ui.builder.ExamImportDraft) -> Unit = {},
    aboutContent: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val imageRepository = remember(appContext) { LocalImageRepository(appContext) }
    val imageScope = rememberCoroutineScope()
    val viewModel = remember(user.id) { ProfileSettingsViewModel(appContext, user.role) }
    val state by viewModel.state.collectAsState()
    var settingsSection by remember(destination, initialSettingsSection) {
        mutableStateOf(initialSettingsSection)
    }
    var confirmRemove by remember { mutableStateOf(false) }
    var avatarEditing by remember { mutableStateOf<Uri?>(null) }
    var avatarPreparing by remember { mutableStateOf(false) }
    var avatarPrepareError by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            avatarPreparing = true
            avatarPrepareError = null
            imageScope.launch {
                imageRepository.prepare(ImageEditRequest(uri))
                    .onSuccess { avatarEditing = it.uri }
                    .onFailure { avatarPrepareError = it.message }
                avatarPreparing = false
            }
        }
    }

    LaunchedEffect(state.savedVersion) {
        if (state.savedVersion > 0) onProfileUpdated()
    }

    Column(Modifier.fillMaxSize()) {
        if (destination == ProfileSettingsDestination.SETTINGS) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
            ) {
                listOf(
                    SettingsSection.APPEARANCE to "ظاهر",
                    SettingsSection.ABOUT to "درباره"
                ).forEach { (item, label) ->
                    FilterChip(
                        selected = settingsSection == item,
                        onClick = { settingsSection = item },
                        label = { Text(label) }
                    )
                }
            }
        }

        if (avatarPreparing) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Text("در حال آماده‌سازی امن تصویر...")
            }
        }
        avatarPrepareError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp))
        }

        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.profile == null -> ErrorPanel(state.error ?: "پروفایل دریافت نشد.", viewModel::load)
            destination == ProfileSettingsDestination.PROFILE -> ProfileSection(
                user = user,
                profile = state.profile!!,
                state = state,
                onDisplayName = viewModel::setDisplayName,
                onFirstName = viewModel::setFirstName,
                onLastName = viewModel::setLastName,
                onEmployeeCode = viewModel::setEmployeeCode,
                onPhone = viewModel::setPhone,
                onAvatarPublic = viewModel::setAvatarPublic,
                onPickAvatar = {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onRemoveAvatar = { confirmRemove = true },
                onSave = viewModel::save
            )
            destination == ProfileSettingsDestination.HEADER && user.role == UserRole.TEACHER -> HeaderSection(
                profile = state.profile!!,
                state = state,
                onProvince = viewModel::setProvince,
                onCity = viewModel::setCity,
                onDistrict = viewModel::setDistrict,
                onSchool = viewModel::setSchool,
                onGrade = viewModel::setGrade,
                onFieldOfStudy = viewModel::setFieldOfStudy,
                onSave = viewModel::save
            )
            destination == ProfileSettingsDestination.HEADER -> ErrorPanel(
                "سربرگ رسمی فقط برای حساب معلم است.",
                retry = {}
            )
            destination == ProfileSettingsDestination.ACCOUNT -> AccountSection(
                user = user,
                profile = state.profile!!,
                state = state,
                onChangePassword = viewModel::changePassword,
                onChangeUsername = viewModel::changeTeacherUsername,
                onChangeEmail = viewModel::changeEmail,
                onDeleteAccount = { viewModel.deleteAccount(onDone = onAccountDeleted) }
            )
            destination == ProfileSettingsDestination.DATA -> Column(
                Modifier.fillMaxSize().padding(horizontal = 16.dp)
            ) {
                when (user.role) {
                    UserRole.TEACHER -> DataPortabilitySection(onImportExam = onImportExam)
                    // V61.5 — متن قدیمی «مرحله V37» حذف شد؛ توضیح واقعی وضعیت.
                    UserRole.MANAGER -> Text(
                        "داده‌های مدرسه (معلم‌ها، کلاس‌ها و دانش‌آموزان) به‌صورت امن روی سرور نگهداری می‌شود و نیازی به پشتیبان‌گیری دستی نیست."
                    )
                    UserRole.STUDENT -> Text("پشتیبان کامل داده‌ها فقط برای کادر مدرسه در دسترس است.")
                }
            }
            settingsSection == SettingsSection.APPEARANCE -> AppearanceSection(appearance, viewModel)
            else -> Box(Modifier.fillMaxSize()) { aboutContent() }
        }
    }

    avatarEditing?.let { uri ->
        InteractiveImageEditorDialog(
            source = uri,
            forceSquare = true,
            onDismiss = { avatarEditing = null },
            onDone = {
                viewModel.uploadAvatar(it)
                avatarEditing = null
            }
        )
    }

    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text("حذف عکس پروفایل") },
            text = { Text("عکس از پروفایل حذف شود؟ فایل قدیمی در پاک‌سازی دوره‌ای Storage حذف خواهد شد.") },
            confirmButton = {
                Button(onClick = { confirmRemove = false; viewModel.removeAvatar() }) { Text("حذف") }
            },
            dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text("انصراف") } }
        )
    }
}

@Composable
private fun AppearanceSection(settings: AppearanceSettings, viewModel: ProfileSettingsViewModel) {
    // V56.1 — تبلت: کارت‌های تنظیمات ظاهر وسط صفحه با سقف پهنا.
    val tabletAppearance = ir.exam.app.core.ui.LocalTabletLayout.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (tabletAppearance) Modifier.wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = 680.dp)
                else Modifier
            )
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("حالت نمایش", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            ThemeMode.SYSTEM to "دستگاه",
                            ThemeMode.LIGHT to "روشن",
                            ThemeMode.DARK to "تیره"
                        ).forEach { (mode, label) ->
                            FilterChip(
                                selected = settings.themeMode == mode,
                                onClick = { viewModel.setTheme(mode) },
                                label = { Text(label) }
                            )
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("رنگ‌های پویا")
                                Text("هماهنگ با رنگ‌بندی گوشی", style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(settings.dynamicColors, viewModel::setDynamicColors)
                        }
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("چیدمان دستگاه", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "در حالت خودکار، برنامه از روی اندازهٔ صفحه تشخیص می‌دهد که چیدمان گوشی یا تبلت را نمایش دهد.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            DeviceLayoutMode.AUTO to "خودکار",
                            DeviceLayoutMode.PHONE to "گوشی",
                            DeviceLayoutMode.TABLET to "تبلت"
                        ).forEach { (mode, label) ->
                            FilterChip(
                                selected = settings.deviceLayoutMode == mode,
                                onClick = { viewModel.setDeviceLayoutMode(mode) },
                                label = { Text(label) }
                            )
                        }
                    }
                    val tabletNow = resolveTabletLayout(settings.deviceLayoutMode)
                    Text(
                        if (tabletNow) "چیدمان فعلی: تبلت — ستون‌های بیشتر و پهنای بهینه"
                        else "چیدمان فعلی: گوشی",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("ظاهر نئومورفیک ۶۹", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "پالت و عمق سایه در DataStore دستگاه ذخیره می‌شوند و پس از اجرای دوباره باقی می‌مانند.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            NeumorphicPalette.INDIGO_MINT to "نیلی و سبز",
                            NeumorphicPalette.BLUE_CYAN to "آبی و فیروزه‌ای",
                            NeumorphicPalette.PINK_ORANGE to "صورتی و نارنجی",
                            NeumorphicPalette.PURPLE_PINK to "بنفش و صورتی"
                        ).forEach { (palette, label) ->
                            val (first, second) = palette.accentColors()
                            val selected = settings.neumorphicPalette == palette
                            Box(
                                Modifier
                                    .size(if (selected) 44.dp else 38.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(first, second)))
                                    .clickable { viewModel.setNeumorphicPalette(palette) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Icon(
                                        Icons.Outlined.CheckCircle,
                                        contentDescription = "پالت انتخاب‌شده: $label",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("عمق سایه")
                        Text(
                            PersianDigits.convert(settings.neumorphicDepth.toInt()),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = settings.neumorphicDepth,
                        onValueChange = viewModel::setNeumorphicDepth,
                        valueRange = 8f..22f,
                        steps = 13
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
                            .background(
                                Brush.linearGradient(
                                    settings.neumorphicPalette.accentColors().let { listOf(it.first, it.second) }
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("پیش‌نمایش پالت", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    if (settings.dynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Text(
                            "رنگ‌های پویای دستگاه اکنون بر پالت ثابت اولویت دارند؛ برای رنگ دقیق انتخابی، آن گزینه را خاموش کنید.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("قلم فارسی", style = MaterialTheme.typography.titleMedium)
                    listOf(
                        AppFont.SYSTEM to "سیستم",
                        AppFont.VAZIRMATN to "وزیرمتن",
                        AppFont.SHABNAM to "شبنم",
                        AppFont.SAHEL to "ساحل"
                    ).chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { (font, label) ->
                                FilterChip(
                                    selected = settings.appFont == font,
                                    onClick = { viewModel.setAppFont(font) },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                    Text("سه قلم فارسی همراه برنامه و با مجوز OFL ذخیره شده‌اند.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("اندازه متن", style = MaterialTheme.typography.titleMedium)
                    Text("${PersianDigits.convert((settings.fontScale * 100).toInt())} درصد")
                    Slider(
                        value = settings.fontScale,
                        onValueChange = viewModel::setFontScale,
                        valueRange = 0.85f..1.30f,
                        steps = 8
                    )
                    Text("نمونه متن فارسی — آزمون ریاضی فصل یک", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        item {
            OutlinedButton(onClick = viewModel::resetAppearance, modifier = Modifier.fillMaxWidth()) {
                Text("بازگردانی تنظیمات ظاهری")
            }
        }
    }
}

@Composable
private fun ProfileSection(
    user: AppUser,
    profile: NativeProfile,
    state: ProfileSettingsState,
    onDisplayName: (String) -> Unit,
    onFirstName: (String) -> Unit,
    onLastName: (String) -> Unit,
    onEmployeeCode: (String) -> Unit,
    onPhone: (String) -> Unit,
    onAvatarPublic: (Boolean) -> Unit,
    onPickAvatar: () -> Unit,
    onRemoveAvatar: () -> Unit,
    onSave: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProfileAvatar(profile.avatarUrl, profile.shownName, 112)
                    if (state.uploadingAvatar) {
                        CircularProgressIndicator()
                        Text("در حال فشرده‌سازی و آپلود امن عکس...")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onPickAvatar, enabled = !state.uploadingAvatar && !state.saving) {
                            Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                            Text(if (profile.avatarUrl == null) "انتخاب عکس" else "تعویض")
                        }
                        if (profile.avatarUrl != null) {
                            OutlinedButton(onClick = onRemoveAvatar, enabled = !state.uploadingAvatar && !state.saving) {
                                Icon(Icons.Outlined.Delete, contentDescription = null)
                                Text("حذف")
                            }
                        }
                    }
                    if (user.role == UserRole.TEACHER) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("نمایش عکس به دانش‌آموزان")
                                Text("فقط دانش‌آموزان خودتان", style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(profile.avatarPublic, onAvatarPublic)
                        }
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("نام نمایشی", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = profile.displayName,
                        onValueChange = onDisplayName,
                        label = { Text("نام نمایشی") },
                        supportingText = { Text("خالی باشد، نام اصلی حساب نمایش داده می‌شود.") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        if (user.role == UserRole.TEACHER) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("مشخصات معلم", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                profile.firstName, onFirstName,
                                label = { Text("نام") }, singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                profile.lastName, onLastName,
                                label = { Text("نام خانوادگی") }, singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        OutlinedTextField(
                            profile.employeeCode, onEmployeeCode,
                            label = { Text("کد پرسنلی") },
                            supportingText = { Text("اختیاری؛ حداکثر ۳۰ حرف انگلیسی یا عدد") },
                            singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            profile.phone, onPhone,
                            label = { Text("شماره تلفن") },
                            supportingText = { Text("اختیاری؛ ۱۱ رقم و با 09 شروع شود") },
                            singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
        item { SaveStatus(state, onSave, "ذخیره پروفایل") }
    }
}

@Composable
private fun AccountSection(
    user: AppUser,
    profile: NativeProfile,
    state: ProfileSettingsState,
    onChangePassword: (String, String) -> Unit,
    onChangeUsername: (String) -> Unit,
    onChangeEmail: (String) -> Unit,
    onDeleteAccount: () -> Unit = {}
) {
    val role = user.role
    var expandedCard by remember(profile.id) { mutableStateOf<String?>(null) }
    var username by remember(profile.username) { mutableStateOf(profile.username) }
    var newEmail by remember(user.email) { mutableStateOf("") }
    var password by remember(profile.id) { mutableStateOf("") }
    var confirmation by remember(profile.id) { mutableStateOf("") }
    var passwordVisible by remember(profile.id) { mutableStateOf(false) }
    var confirmationVisible by remember(profile.id) { mutableStateOf(false) }
    val schoolJoinRepository = remember { SupabaseSchoolJoinRepository() }
    val schoolJoinScope = rememberCoroutineScope()
    var schoolCode by remember(profile.id) { mutableStateOf("") }
    var schoolPreview by remember(profile.id) { mutableStateOf<SchoolInvitePreview?>(null) }
    var schoolJoinLoading by remember(profile.id) { mutableStateOf(false) }
    var schoolJoinMessage by remember(profile.id) { mutableStateOf<String?>(null) }
    var confirmDeleteAccount by remember(profile.id) { mutableStateOf(false) }

    fun toggle(card: String) {
        expandedCard = if (expandedCard == card) null else card
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AccountAccordionCard(
                title = "مشخصات حساب",
                expanded = expandedCard == "info",
                onToggle = { toggle("info") }
            ) {
                LabeledValue("نام", profile.fullName)
                LabeledValue("نام کاربری", profile.username.ifBlank { "—" })
                LabeledValue(
                    "نقش",
                    when (role) {
                        UserRole.TEACHER -> "معلم"
                        UserRole.MANAGER -> "مدیر/معاون"
                        UserRole.STUDENT -> "دانش‌آموز"
                    }
                )
                if (role != UserRole.STUDENT) {
                    LabeledValue("ایمیل", user.email.orEmpty().ifBlank { "—" })
                }
            }
        }
        if (role == UserRole.TEACHER) {
            item {
                AccountAccordionCard(
                    title = "پیوستن به مدرسه",
                    expanded = expandedCard == "join_school",
                    onToggle = { toggle("join_school") }
                ) {
                    Text("کد دعوت ۶ کاراکتری مدیر/معاون را وارد کنید.")
                    OutlinedTextField(
                        value = schoolCode,
                        onValueChange = {
                            schoolCode = it.uppercase().filter { char -> char in 'A'..'Z' || char.isDigit() }.take(6)
                            schoolPreview = null
                            schoolJoinMessage = null
                        },
                        label = { Text("کد دعوت مدرسه") },
                        trailingIcon = {
                            IconButton(
                                enabled = schoolCode.length == 6 && !schoolJoinLoading,
                                onClick = {
                                    schoolJoinLoading = true
                                    schoolJoinScope.launch {
                                        schoolJoinRepository.preview(schoolCode)
                                            .onSuccess { schoolPreview = it; schoolJoinMessage = null }
                                            .onFailure { schoolJoinMessage = it.message.orEmpty().substringBefore("URL:").substringBefore("Headers:") }
                                        schoolJoinLoading = false
                                    }
                                }
                            ) { Icon(Icons.Outlined.Search, contentDescription = "جست‌وجوی مدرسه") }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (schoolJoinLoading) CircularProgressIndicator()
                    schoolPreview?.let { preview ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(preview.schoolName, style = MaterialTheme.typography.titleMedium)
                                Text(listOf(preview.province, preview.city).filter(String::isNotBlank).joinToString(" · "))
                            }
                        }
                        Button(
                            onClick = {
                                schoolJoinLoading = true
                                schoolJoinScope.launch {
                                    schoolJoinRepository.join(schoolCode)
                                        .onSuccess { schoolJoinMessage = "عضویت در مدرسه «$it» انجام شد."; schoolPreview = null }
                                        .onFailure { schoolJoinMessage = it.message.orEmpty().substringBefore("URL:").substringBefore("Headers:") }
                                    schoolJoinLoading = false
                                }
                            },
                            enabled = !schoolJoinLoading,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("تأیید و پیوستن") }
                    }
                    schoolJoinMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                }
            }
        }
        if (role != UserRole.STUDENT) {
        item {
            AccountAccordionCard(
                title = "تغییر نام کاربری",
                expanded = expandedCard == "username",
                onToggle = { toggle("username") }
            ) {
                if (role != UserRole.STUDENT) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it.lowercase().filter { char ->
                                char in 'a'..'z' || char.isDigit() || char == '_'
                            }.take(20)
                        },
                        label = { Text("نام کاربری انگلیسی") },
                        supportingText = { Text("ورود معلم همچنان با ایمیل انجام می‌شود.") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { onChangeUsername(username) },
                        enabled = !state.accountSaving && username.length >= 4 && username != profile.username,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("ذخیره نام کاربری") }
                } else {
                    Text("تغییر نام کاربری دانش‌آموز فقط توسط معلم انجام می‌شود.")
                }
            }
        }
        item {
            AccountAccordionCard(
                title = "تغییر ایمیل",
                expanded = expandedCard == "email",
                onToggle = { toggle("email") }
            ) {
                if (role != UserRole.STUDENT) {
                    OutlinedTextField(
                        value = newEmail,
                        onValueChange = { newEmail = it.trim().take(254) },
                        label = { Text("ایمیل جدید") },
                        supportingText = {
                            Text("Supabase پیام تأیید می‌فرستد؛ تا تأیید، ایمیل فعلی معتبر می‌ماند.")
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            onChangeEmail(newEmail)
                            newEmail = ""
                        },
                        enabled = !state.accountSaving && '@' in newEmail && newEmail.length >= 5,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("ارسال تأیید به ایمیل جدید") }
                } else {
                    Text("ایمیل ورود دانش‌آموز توسط سامانه مدیریت می‌شود و در برنامه نمایش داده نمی‌شود.")
                }
            }
        }
        item {
            AccountAccordionCard(
                title = "تغییر رمز عبور",
                expanded = expandedCard == "password",
                onToggle = { toggle("password") }
            ) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it.take(72) },
                    label = { Text("رمز جدید ۸ تا ۷۲ کاراکتر") },
                    visualTransformation = passwordTransformation(passwordVisible),
                    trailingIcon = {
                        PasswordVisibilityButton(
                            visible = passwordVisible,
                            onToggle = { passwordVisible = !passwordVisible }
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmation,
                    onValueChange = { confirmation = it.take(72) },
                    label = { Text("تکرار رمز جدید") },
                    visualTransformation = passwordTransformation(confirmationVisible),
                    trailingIcon = {
                        PasswordVisibilityButton(
                            visible = confirmationVisible,
                            onToggle = { confirmationVisible = !confirmationVisible }
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        onChangePassword(password, confirmation)
                        password = ""
                        confirmation = ""
                    },
                    enabled = !state.accountSaving && password.length >= 8 && password == confirmation,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("تغییر رمز") }
                Text("رمز قبلی قابل مشاهده یا بازیابی نیست.", style = MaterialTheme.typography.bodySmall)
            }
        }
        }
        item {
            AccountAccordionCard(
                title = "قفل برنامه",
                expanded = expandedCard == "lock",
                onToggle = { toggle("lock") }
            ) {
                AppLockSettings(profile.id, embedded = true)
            }
        }
        if (role != UserRole.STUDENT) item {
            // V59.1 — حذف کامل حساب معلم/مدیر با تأیید دومرحله‌ای.
            AccountAccordionCard(
                title = "حذف حساب",
                expanded = expandedCard == "delete",
                onToggle = { toggle("delete") }
            ) {
                Text(
                    "با حذف حساب، کلاس‌ها و دانش‌آموزانی که فقط با این حساب ساخته شده‌اند برای همیشه حذف می‌شوند. " +
                        "دانش‌آموزی که به لیست حساب دیگری هم اضافه شده باشد حذف نمی‌شود و کنترل کامل حسابش " +
                        "(شامل مدیریت رمز) به آن حساب منتقل می‌شود. این عمل بازگشت‌ناپذیر است.",
                    style = MaterialTheme.typography.bodySmall
                )
                Button(
                    onClick = { confirmDeleteAccount = true },
                    enabled = !state.accountSaving,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("حذف حساب") }
            }
        }
        if (state.accountSaving) item { CircularProgressIndicator() }
        state.message?.let { item { Text(it, color = MaterialTheme.colorScheme.primary) } }
        state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
    }
    if (confirmDeleteAccount) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAccount = false },
            title = { Text("حذف کامل حساب") },
            text = {
                Text(
                    "آیا مطمئن هستید؟ همهٔ کلاس‌ها و دانش‌آموزانِ فقط-متعلق-به-شما حذف می‌شوند و " +
                        "دانش‌آموزان مشترک به حساب دیگر منتقل می‌شوند. این عمل قابل بازگشت نیست."
                )
            },
            confirmButton = {
                Button(
                    onClick = { confirmDeleteAccount = false; onDeleteAccount() },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("بله، حساب حذف شود") }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteAccount = false }) { Text("انصراف") } }
        )
    }
}

@Composable
private fun AccountAccordionCard(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Icon(
                    imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = if (expanded) "بستن $title" else "بازکردن $title"
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    content = content
                )
            }
        }
    }
}

@Composable
private fun HeaderSection(
    profile: NativeProfile,
    state: ProfileSettingsState,
    onProvince: (String) -> Unit,
    onCity: (String) -> Unit,
    onDistrict: (String) -> Unit,
    onSchool: (String) -> Unit,
    onGrade: (String) -> Unit,
    onFieldOfStudy: (String) -> Unit,
    onSave: () -> Unit
) {
    val header = profile.header
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("اطلاعات سربرگ رسمی امتحان", style = MaterialTheme.typography.titleMedium)
                    Text("این اطلاعات در قالب چاپ رسمی V10 استفاده می‌شود و اکنون در پروفایل امن شما ذخیره خواهد شد.")
                    OutlinedTextField(header.province, onProvince, label = { Text("استان") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(header.city, onCity, label = { Text("شهر / شهرستان") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(header.district, onDistrict, label = { Text("منطقه / ناحیه") }, modifier = Modifier.fillMaxWidth())
                    // V61.5 — نام مدرسه از لیست مدارس عضو انتخاب می‌شود؛ «سایر» همان
                    // فیلد را قابل تایپ می‌کند (چرخ مشترک پایه با دادهٔ مدارس).
                    val memberSchools = remember { mutableStateOf<List<String>>(emptyList()) }
                    LaunchedEffect(Unit) {
                        runCatching {
                            val raw = ir.exam.app.data.remote.SupabaseProvider.client
                                .postgrest.rpc("native_teacher_schools_v61")
                                .decodeAs<kotlinx.serialization.json.JsonObject>()
                            ((raw["items"] as? kotlinx.serialization.json.JsonArray)
                                ?: kotlinx.serialization.json.JsonArray(emptyList())).mapNotNull { element ->
                                ((element as? kotlinx.serialization.json.JsonObject)
                                    ?.get("name") as? kotlinx.serialization.json.JsonPrimitive)?.content
                            }.filter(String::isNotBlank)
                        }.onSuccess { memberSchools.value = it }
                    }
                    GradeOdometerPicker(
                        value = header.school,
                        onValueChange = onSchool,
                        modifier = Modifier.fillMaxWidth(),
                        availableGrades = memberSchools.value,
                        includeStandardGrades = false,
                        emptyLabel = "بدون مدرسه",
                        label = "نام مدرسه",
                        standardValues = emptyList(),
                        customLabel = "سایر مدرسه"
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GradeOdometerPicker(
                            value = header.grade,
                            onValueChange = onGrade,
                            modifier = Modifier.weight(1f),
                            emptyLabel = "بدون پایه"
                        )
                        FieldOfStudyPicker(
                            value = header.fieldOfStudy,
                            onValueChange = onFieldOfStudy,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        item { SaveStatus(state, onSave, "ذخیره سربرگ") }
    }
}

@Composable
private fun SaveStatus(state: ProfileSettingsState, onSave: () -> Unit, label: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = onSave,
            enabled = !state.saving && !state.uploadingAvatar,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.saving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            Text(if (state.saving) "در حال ذخیره..." else label)
        }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
fun ProfileAvatar(url: String?, name: String, sizeDp: Int) {
    if (!url.isNullOrBlank()) {
        AsyncImage(
            model = url,
            contentDescription = "عکس پروفایل $name",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(sizeDp.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
        )
    } else {
        Box(
            Modifier.size(sizeDp.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            val initial = name.trim().firstOrNull()?.toString()
            if (initial == null) Icon(Icons.Outlined.AccountCircle, contentDescription = null, Modifier.size((sizeDp * 0.7).dp))
            else Text(initial, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ErrorPanel(message: String, retry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.padding(6.dp))
        Button(onClick = retry) { Text("تلاش دوباره") }
    }
}
