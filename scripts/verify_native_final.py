#!/usr/bin/env python3
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
errors: list[str] = []

def require(condition: bool, message: str) -> None:
    if not condition:
        errors.append(message)

main_files = list((ROOT / "app/src/main/java").rglob("*.kt"))
main_text = "\n".join(path.read_text(errors="ignore") for path in main_files)
edge_files = list((ROOT / "supabase/functions").glob("*/index.ts"))
edge_text = "\n".join(path.read_text(errors="ignore") for path in edge_files)
repository_text = "\n".join(
    path.read_text(errors="ignore")
    for path in (ROOT / "app/src/main/java/ir/exam/app/data/repository").glob("*.kt")
)
manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text()
workflow = (ROOT / ".github/workflows/android.yml").read_text()
hardening = (ROOT / "supabase/migrations/20260812_native_final_hardening.sql").read_text()
critical = (ROOT / "supabase/migrations/20260812_native_critical_flows_v12.sql").read_text()
parity = (ROOT / "supabase/migrations/20260812_native_full_parity_v13.sql").read_text()
auth_identifier = (ROOT / "app/src/main/java/ir/exam/app/data/repository/AuthIdentifier.kt").read_text()
student_codec = (ROOT / "app/src/main/java/ir/exam/app/data/repository/StudentExamPayloadCodec.kt").read_text()
database_provider = (ROOT / "app/src/main/java/ir/exam/app/data/local/NativeDatabaseProvider.kt").read_text()
student_results = (ROOT / "app/src/main/java/ir/exam/app/ui/reports/StudentResultsScreen.kt").read_text()
builder_screen=(ROOT/"app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt").read_text()
student_screen=(ROOT/"app/src/main/java/ir/exam/app/ui/student/StudentExamScreen.kt").read_text()
teacher_dock=(ROOT/"app/src/main/java/ir/exam/app/ui/app/TeacherBottomDock.kt").read_text()
app_shell=(ROOT/"app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").read_text()
neumorphic_design=(ROOT/"app/src/main/java/ir/exam/app/ui/app/Neumorphic69Design.kt").read_text()
design69_icons=(ROOT/"app/src/main/java/ir/exam/app/ui/app/Design69Icons.kt").read_text()
design69_menu=(ROOT/"app/src/main/java/ir/exam/app/ui/app/Design69MainMenuScreen.kt").read_text()
design69_add=(ROOT/"app/src/main/java/ir/exam/app/ui/app/Design69QuickAddOverlay.kt").read_text()
design69_cards=(ROOT/"app/src/main/java/ir/exam/app/ui/app/TeacherManagementCardsScreen.kt").read_text()
appearance_preferences=(ROOT/"app/src/main/java/ir/exam/app/core/ui/AppearancePreferences.kt").read_text()
app_theme=(ROOT/"app/src/main/java/ir/exam/app/core/ui/ExamAppTheme.kt").read_text()
profile_settings=(ROOT/"app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsScreen.kt").read_text()
wallet_screen=(ROOT/"app/src/main/java/ir/exam/app/ui/billing/WalletScreen.kt").read_text()
teacher_dashboard=(ROOT/"app/src/main/java/ir/exam/app/ui/dashboard/TeacherDashboardScreen.kt").read_text()
builder_radial=(ROOT/"app/src/main/java/ir/exam/app/ui/builder/BuilderRadialMenuOverlay.kt").read_text()
username_suggester=(ROOT/"app/src/main/java/ir/exam/app/ui/classes/PersianUsernameSuggester.kt").read_text()
formula_native_view=(ROOT/"app/src/main/java/ir/exam/app/ui/math/NativeFormulaView.kt").read_text()
question_bank_screen=(ROOT/"app/src/main/java/ir/exam/app/ui/bank/QuestionBankScreen.kt").read_text()
app_lock_ui=(ROOT/"app/src/main/java/ir/exam/app/ui/security/AppLockUi.kt").read_text()
profile_models=(ROOT/"app/src/main/java/ir/exam/app/domain/model/ProfileModels.kt").read_text()
profile_repository=(ROOT/"app/src/main/java/ir/exam/app/data/repository/SupabaseProfileRepository.kt").read_text()
portability_repository=(ROOT/"app/src/main/java/ir/exam/app/data/repository/SupabasePortabilityRepository.kt").read_text()
v18_migration=(ROOT/"supabase/migrations/20260813_native_navigation_account_v18.sql").read_text()
v18_sql_copy=(ROOT/"SQL_NATIVE_NAVIGATION_ACCOUNT_V18.sql").read_text()
v22_migration=(ROOT/"supabase/migrations/20260814_native_student_class_membership_v22.sql").read_text()
v22_sql_copy=(ROOT/"SQL_NATIVE_STUDENT_MULTI_CLASS_V22.sql").read_text()
v24_guide=(ROOT/"COMPREHENSIVE_UX_V24_FA.md").read_text()
v25_guide=(ROOT/"HEADER_SAFETY_POLISH_V25_FA.md").read_text()
school_repository=(ROOT/"app/src/main/java/ir/exam/app/data/repository/SupabaseSchoolRepository.kt").read_text()
school_screen=(ROOT/"app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt").read_text()
grade_odometer=(ROOT/"app/src/main/java/ir/exam/app/ui/common/GradeOdometerPicker.kt").read_text()
calendar_screen=(ROOT/"app/src/main/java/ir/exam/app/ui/calendar/CalendarScreen.kt").read_text()
date_time_picker=(ROOT/"app/src/main/java/ir/exam/app/ui/builder/JalaliDateTimePicker.kt").read_text()
question_model=(ROOT/"app/src/main/java/ir/exam/app/ui/builder/QuestionDraft.kt").read_text()
local_image_repository=(ROOT/"app/src/main/java/ir/exam/app/data/repository/LocalImageRepository.kt").read_text()
question_media=(ROOT/"app/src/main/java/ir/exam/app/ui/image/QuestionMediaEditor.kt").read_text()
image_editor=(ROOT/"app/src/main/java/ir/exam/app/ui/image/InteractiveImageEditorDialog.kt").read_text()
about_screen=(ROOT/"app/src/main/java/ir/exam/app/ui/update/AboutScreen.kt").read_text()
classes_view_model=(ROOT/"app/src/main/java/ir/exam/app/ui/classes/ClassesViewModel.kt").read_text()
grading_screen=(ROOT/"app/src/main/java/ir/exam/app/ui/grading/GradingScreen.kt").read_text()
formula_editor=(ROOT/"app/src/main/java/ir/exam/app/ui/math/FormulaEditorDialog.kt").read_text()
formula_view=(ROOT/"app/src/main/java/ir/exam/app/ui/math/NativeFormulaView.kt").read_text()
formula_text=(ROOT/"app/src/main/java/ir/exam/app/ui/math/NativeMathText.kt").read_text()
formula_svg=(ROOT/"app/src/main/java/ir/exam/app/core/math/NativeMathSvgRenderer.kt").read_text()
formula_boxes=(ROOT/"app/src/main/java/ir/exam/app/core/math/FormulaBoxEditor.kt").read_text()
formula_reference_loader=(ROOT/"app/src/main/java/ir/exam/app/ui/math/FormulaReferenceLibrary.kt").read_text()
formula_library_dialog=(ROOT/"app/src/main/java/ir/exam/app/ui/math/FormulaLibraryDialog.kt").read_text()
formula_library_nav=(ROOT/"app/src/main/java/ir/exam/app/ui/math/FormulaLibraryNavigator.kt").read_text()
formula_smart=(ROOT/"app/src/main/java/ir/exam/app/ui/math/FormulaSmartHubDialog.kt").read_text()
formula_smart_data=(ROOT/"app/src/main/java/ir/exam/app/ui/math/FormulaSmartReference.kt").read_text()
formula_natural=(ROOT/"app/src/main/java/ir/exam/app/core/math/NativeNaturalMathConverter.kt").read_text()
formula_text_codec=(ROOT/"app/src/main/java/ir/exam/app/core/math/FormulaTextCodec.kt").read_text()
matching_builder=(ROOT/"app/src/main/java/ir/exam/app/ui/builder/QuestionOptionMedia.kt").read_text()
app_gradle=(ROOT/"app/build.gradle.kts").read_text()
formula_library=ROOT/"app/src/main/assets/formula_library_v13.json"

require("android.webkit" not in main_text, "WebView/android.webkit import remains in Native source")
require(not re.search(r"\b(val|var)\s+plain_password\b", main_text), "plain_password model field remains")
require(not re.search(r'\.from\("[^"]+"\)\.(?:insert|update|upsert|delete)\b', main_text),
        "direct public-table mutation remains in APK repository")
require("decodeSingle" not in repository_text,
        "decodeSingle remains in repository; JSONB RPC objects require decodeAs")
require("plain_password:" not in edge_text and "plain_password =" not in edge_text,
        "Edge Function still writes/reads plain_password")
require("npm:@supabase/supabase-js@2.112.2" in edge_text, "Edge dependency is not mature pinned version")
require("minimum-dependency-age=0" not in workflow, "Deno dependency age protection disabled")
require('android:allowBackup="false"' in manifest, "Android backup is not disabled")
require('android:usesCleartextTraffic="false"' in manifest, "Cleartext traffic is enabled")
require("APK retention deleted" in workflow, "APK retention step missing")
require("Release APK signing certificate: VERIFIED" in workflow, "release certificate verification missing")
require("v11_authenticated_upload_exam_images" in hardening, "Storage owner-prefix policy missing")
require("drop column if exists plain_password" in hardening.lower(), "plain_password DROP missing")

require('studentDomain = "student.exam.local"' in auth_identifier,
        "student username is not mapped to managed Auth domain")
require("createUser = false" in repository_text,
        "existing-account OTP must not create accidental users")
require("native_complete_teacher_registration_v1" in repository_text and
        "native_complete_teacher_registration_v1" in critical,
        "verified teacher registration completion path missing")
require("native_update_my_username_v1" in critical,
        "teacher username owner RPC missing")
require(all(value in student_codec for value in ("shuffle_q", "shuffle_opt", "expires_at", "server_now", "teacher_message")),
        "student exam payload does not consume critical server controls")
require("correctOption" in student_codec and "fun sanitize" in student_codec,
        "active exam cache does not explicitly strip answer keys")
require("MIGRATION_2_3" in database_provider and "active_exam_sessions" in database_provider,
        "Room active exam process-death migration missing")
require("native_my_answer_detail_v1" in critical and "مشاهده سؤال‌ها و پاسخ‌ها" in student_results,
        "student answer detail path missing")
require("coalesce(v_answer.graded, false)" in critical and "v_keys" in critical,
        "answer key is not gated by graded state")
for function_name in (
    "native_complete_teacher_registration_v1", "native_update_my_username_v1",
    "native_my_registration_state_v1", "native_my_answers_v1", "native_my_answer_detail_v1"
):
    require(
        re.search(rf"revoke all on function public\.{function_name}", critical, re.I) is not None,
        f"V12 function {function_name} lacks explicit revoke"
    )

require(all((ROOT/"app/src/main/res/font"/name).exists() for name in ("vazirmatn_regular.ttf","vazirmatn_medium.ttf","vazirmatn_bold.ttf","shabnam_regular.ttf","sahel_regular.ttf")),"bundled Persian fonts/weights missing")
require("native_save_exam_v2" in parity and "native_bank_snapshot_v1" in parity and "native_feedback_update_v1" in parity,"V13 backend parity RPCs missing")
require("پیش‌نمایش کامل A4" in builder_screen and "تعداد گزینه" in builder_screen and "حساس به حروف" in builder_screen,"builder parity controls missing")
require("مرور پیش از ارسال" in student_screen and "علامت برای مرور" in student_screen,"student navigation/review parity missing")
require(all(marker in teacher_dock for marker in ("TeacherDockAction.MENU","TeacherDockAction.WALLET","TeacherDockAction.CREATE","TeacherDockAction.EXAMS","TeacherDockAction.CARDS","Design69MorphingMenuIcon","Design69Icons.Wallet","Design69Icons.Exams","Design69Icons.Cards","rippleProgress.animateTo(1f, tween(520))")),
        "teacher bottom dock order/vector icons/micro-motion incomplete")
require("TeacherBottomDock" in app_shell and all(marker in app_shell for marker in ("onToggleMenu","onToggleAdd","onCreateStudent","onCreateExam","onCreateClass","onCards")),
        "teacher design69 dock is not wired to real pages")
require(all(marker in design69_icons for marker in ("Design69MorphingMenuIcon","Design69Wallet","Design69Exams","Design69Cards","Design69Calendar","Design69Settings","Design69Logout")),
        "custom native line-vector icon set or hamburger morph incomplete")
require(all(marker in design69_menu for marker in (
            "PROFILE_HEIGHT_DP = 148","CARD_HEIGHT_DP = 116",
            "TEACHER_CARD_COUNT = 8","STUDENT_CARD_COUNT = 6"
        )) and "slideInHorizontally" in design69_menu and
        "slideInVertically" in design69_menu and
        "val delay = 20 + index * 18" in design69_menu and
        "delay = 120 + index * 40" not in design69_menu and
        "enter = fadeIn(tween(110))" in app_shell and
        "animationSpec = tween(180)" in design69_icons,
        "controlled nested full-page account/data menu contract incomplete")
require("Design69MainMenuScreen" in app_shell and "menuOpen = !menuOpen" in app_shell and
        "BackHandler(enabled = menuOpen" in app_shell and "ModalNavigationDrawer" not in app_shell,
        "menu is not a full-page reversible state")
require(all(marker in design69_add for marker in ("OPEN_ROTATION_DEGREES = 135","ACTION_COUNT = 3","دانش‌آموز جدید","آزمون جدید","کلاس جدید","travel.animateTo")),
        "shared moving plus or three real quick actions incomplete")
require(all(marker in design69_cards for marker in ("CARD_COUNT = 5","DRAG_THRESHOLD_DP = 52","detectDragGestures","Key.DirectionLeft","Key.DirectionRight","\"آمار\"","بانک سؤال","\"تصحیح\"","\"مانده\"","\"پاسخ\"","cards[activeIndex].subtitle")) and
        "Key.DirectionDown" not in design69_cards and "بکشید" not in design69_cards,
        "five-card horizontal-only management stack/description incomplete")
require("cards.forEachIndexed" not in design69_cards,
        "management cards are duplicated as buttons below the stack")
require("ModalBottomSheet" not in teacher_dock,
        "legacy management bottom sheet remains instead of full cards page")
require("SchoolLaunchAction.CREATE_STUDENT" in school_screen and "SchoolLaunchAction.CREATE_CLASS" in school_screen,
        "teacher quick-create school actions missing")
require(all(marker in grading_screen for marker in ("initialPendingOnly","initialGradedOnly","فقط مانده","فقط پاسخ")),
        "pending/graded answer management routes missing")
require(all(marker in neumorphic_design for marker in ("Neumorphic69Provider","setShadowLayer","lightShadow","darkShadow","NeumorphicTopBar","NeumorphicMenuTile")),
        "Neumorphic 69 native primitives incomplete")
require("Neumorphic69Provider(depth = appearance.neumorphicDepth)" in app_shell and
        "widthIn(max = 900.dp)" in app_shell and "TopAppBar(" in app_shell,
        "compact/adaptive Neumorphic shell with shared header is not active")
require(all(marker in teacher_dock for marker in (".size(44.dp)",".size(58.dp)","shape = CircleShape","clip = true","if (!expanded)")),
        "smaller active halo/centered plus/no-trace behavior incomplete")
require("PullToRefreshBox" in teacher_dashboard and "onRefresh = viewModel::load" in teacher_dashboard and
        "به‌روزرسانی" not in teacher_dashboard and "LaunchedEffect(refreshKey)" in teacher_dashboard,
        "teacher dashboard pull-to-refresh/active-tab refresh missing or manual button remains")
require("walletRefreshKey += 1" in app_shell and "dashboardRefreshKey += 1" in app_shell and
        "cardsCycleKey += 1" in app_shell and "LaunchedEffect(refreshKey)" in wallet_screen,
        "active dock destination secondary real behavior incomplete")
require("mutableStateOf(MainPage.CALENDAR)" in app_shell and all(marker in app_shell for marker in (
            "\"تقویم\"","\"کلاس‌ها\"","\"دانش‌آموزان\"","\"سربرگ\"",
            "\"حساب\"","\"داده‌ها\"","\"تنظیمات\"","\"خروج\""
        )), "calendar default or exact hamburger menu order/routes missing")
require(all(marker not in app_shell.split("val menuCards = if (user.role == UserRole.TEACHER)",1)[1].split("} else {",1)[0]
            for marker in ("داشبورد معلم","تصحیح و حضور","آمار و گزارش‌ها","درباره و بروزرسانی","آزمون جدید")),
        "removed teacher hamburger cards returned")
require("expandedExamId" in teacher_dashboard and "AnimatedVisibility" in teacher_dashboard and
        "داشبورد معلم" not in teacher_dashboard,
        "collapsible exam cards or compact exams screen missing")
require(all(marker in profile_settings for marker in (
            "SettingsSection.APPEARANCE","SettingsSection.ABOUT",
            "ProfileSettingsDestination.ACCOUNT","ProfileSettingsDestination.DATA",
            "AccountAccordionCard","تغییر ایمیل","AppLockSettings","onGrade"
        )) and "SettingsSection.ACCOUNT" not in profile_settings and
        "SettingsSection.DATA" not in profile_settings,
        "independent accordion account/data routes or compact settings incomplete")
require("مشاهده و ویرایش حساب و تنظیمات" not in design69_menu,
        "removed profile helper sentence returned")
require(all(marker in question_bank_screen for marker in ("جست‌وجوی متن یا درس","دسته جدید","افزودن به آزمون","ویرایش","حذف")) and
        "native_bank_update_question_v1" in v18_migration,
        "standalone full question bank manager missing")
require(all(marker in app_lock_ui for marker in ("BiometricPrompt","DEVICE_CREDENTIAL","قفل امن دستگاه")) and
        "پین جدید" not in app_lock_ui and "androidx.biometric:biometric:1.1.0" in app_gradle,
        "system-only biometric/device credential app lock incomplete")
require("auth.updateUser { email = clean }" in profile_repository,
        "verified Supabase email change path missing")
require(all(marker in v18_migration for marker in ("hdr_grade","native_save_profile","native_bank_update_question_v1","native_export_backup_v2","native_restore_backup_v2")) and
        v18_sql_copy == v18_migration and "val grade: String" in profile_models and
        "native_export_backup_v2" in portability_repository,
        "server/print/backup header grade migration or SQL copy incomplete")
require(all(marker in builder_screen for marker in ("expandedQuestionId","settingsExpanded","bottom = 112.dp","FabPosition.Center","Icons.Outlined.Check","BuilderRadialMenuOverlay","BuilderQuestionBankDialog")) and
        all(label in builder_radial for label in ("تشریحی","چندگزینه‌ای","صحیح/غلط","جای خالی","عددی","جورکردنی","وارد کردن","بانک سؤال")),
        "builder accordion/radial eight actions/bounded floating save incomplete")
require("dottedAlpha" in builder_radial and "progress.animateTo(1f, tween(620" in builder_radial and
        "fun addQuestion(type: QuestionType): String" in (ROOT/"app/src/main/java/ir/exam/app/ui/builder/ExamBuilderViewModel.kt").read_text(),
        "builder synchronized radial motion or auto-open question contract missing")
require("PersianUsernameSuggester.suggest" in school_screen and "BulkStudentDraft" in school_screen and
        "contentAlignment = Alignment.TopCenter" in school_screen and "🎲 رمز" in school_screen,
        "compact single/bulk student dialogs or username suggestions incomplete")
require(all("PullToRefreshBox" in text and "تازه‌سازی" not in text for text in (
            school_screen, question_bank_screen,
            (ROOT/"app/src/main/java/ir/exam/app/ui/calendar/CalendarScreen.kt").read_text(), wallet_screen
        )), "manual refresh button remains or pull-to-refresh is missing")
require("LocalLayoutDirection provides LayoutDirection.Ltr" in formula_native_view and
        "horizontal.animateScrollTo(targetX)" in formula_native_view and
        "LocalLayoutDirection provides LayoutDirection.Ltr" in formula_editor,
        "LTR formula editing or automatic active-box scroll missing")
require("provider === 'sandbox'" in edge_text and "sandboxAllowed()" in edge_text and
        "native_credit_wallet_payment" in edge_text and "credited: true" in edge_text and
        "native_credit_wallet_payment" not in main_text,
        "server-gated sandbox auto-credit missing or direct APK credit introduced")
require("targetX.roundToPx() * progress" in design69_add and
        "travel.value - .88f" in design69_add and "actionsVisible" not in design69_add,
        "quick-add plus/options are not synchronized or dotted lines start too early")
require('android:label="آزمون آنلاین"' in manifest and "آزمون آنلاین" in main_text,
        "application branding was not changed to آزمون آنلاین")
password_visibility=(ROOT/"app/src/main/java/ir/exam/app/ui/common/PasswordVisibility.kt").read_text()
require("PasswordVisibilityButton" in main_text and "نمایش رمز" in password_visibility and
        len(re.findall(r"PasswordVisualTransformation\(\)", main_text)) == 1,
        "password show/hide control is not shared by every password input")
require("title = \"آزمون جدید\"" in design69_add and
        design69_add.index("title = \"آزمون جدید\"") < design69_add.index("title = \"دانش‌آموز جدید\""),
        "quick-add exam/student positions were not swapped")
require("Key.DirectionDown" not in design69_cards and "بکشید" not in design69_cards,
        "management cards still support vertical navigation or show drag helper text")
require(all(marker in builder_screen for marker in ("Alignment.CenterStart","Alignment.CenterEnd","expandedQuestionId = null","scrollQuestionToHeader(index)","animateScrollToItem(questionPrefaceCount + questionIndex, 0)")) and
        builder_screen.count("withFrameNanos") >= 2,
        "builder opposite FABs or exact post-layout question scroll behavior missing")
require("viewport.width * .14f" in formula_native_view and "viewport.width * .62f" in formula_native_view and
        "viewport.height * .12f" in formula_native_view,
        "earlier predictive formula auto-scroll threshold missing")
students_content=school_screen.split("private fun StudentsContent(",1)[1].split("private fun StudentCard(",1)[0]
bulk_content=school_screen.split("private fun BulkStudentDialog(",1)[1].split("private fun studentWorkbook",1)[0]
require("حساب جدید" not in students_content and all(marker in students_content for marker in (
            "Text(\"Excel\")","Icons.Outlined.Search","Icons.Outlined.Close","AnimatedVisibility","Arrangement.Center"
        )), "student list toolbar/search behavior incomplete")
require("Text(\"▦\"" not in bulk_content and "افزودن گروهی دانش‌آموز" not in bulk_content and
        all(marker in bulk_content for marker in ("Text(\"ایجاد\")","Color(0xFF25A86B)","Color(0xFFE5484D)","submitBulk")),
        "bulk top controls or removed title are incorrect")
require("suspend fun scrollQuestionToHeader" in builder_screen and builder_screen.count("withFrameNanos") >= 2 and
        "animateScrollToItem(questionPrefaceCount + questionIndex, 0)" in builder_screen,
        "post-layout exact-under-header question scroll missing")
require("contentAlignment: Alignment = Alignment.Center" in neumorphic_design and
        "horizontalArrangement = Arrangement.Center" in students_content,
        "custom/button toolbar content is not centered")
class_roster=school_screen.split("private fun ClassRosterContent(",1)[1].split("private fun StudentsContent(",1)[0]
student_card=school_screen.split("private fun StudentCard(",1)[1].split("private fun ClassEditorDialog(",1)[0]
member_picker=school_screen.split("private fun MemberPickerDialog(",1)[1].split("private fun StudentEditDialog(",1)[0]
require("showBulk = true" in school_screen.split("SchoolLaunchAction.CREATE_STUDENT ->",1)[1].split("SchoolLaunchAction.CREATE_CLASS",1)[0],
        "main quick-create student does not open bulk dialog")
require(all(marker in class_roster for marker in ("addMenuOpen","افزودن موجود","افزودن جدید")) and
        "حساب جدید" not in class_roster and "ساخت گروهی" not in class_roster,
        "class hanging add menu is incomplete")
require(all(marker in member_picker for marker in ("دختر","پسر","همه پایه‌ها")),
        "existing-student gender/grade filters missing")
require(all(marker in student_card for marker in ("expanded = !expanded","Color(0xFFFF80AB)","Color(0xFF64B5F6)","Icons.Outlined.ToggleOn","Icons.Outlined.Edit","Icons.Outlined.Add","Icons.Outlined.ContentCopy","selectedClasses")),
        "gender-colored accordion student cards or icon actions incomplete")
require(all(marker in builder_screen for marker in (
            "floatingActionButtonPosition = FabPosition.Center","Icons.Outlined.Check",
            "Modifier.align(Alignment.CenterStart).size(56.dp)","modifier = Modifier.size(28.dp)"
        )) and "Text(\"✓\"" not in builder_screen,
        "builder save check is not centered/bounded or clipped text glyph returned")
require(all(marker in class_roster for marker in (
            "modifier = Modifier.align(Alignment.CenterHorizontally)",
            "Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)",
            "expandVertically(expandFrom = Alignment.Top)"
        )), "class plus/hanging actions are not centered")
require(all(marker in member_picker for marker in (
            "gender = if (gender == \"female\") null else \"female\"",
            "gender = if (gender == \"male\") null else \"male\"",
            "student.gender?.lowercase() == gender","student.grade?.trim() == grade",
            "GradeOdometerPicker(","includeStandardGrades = false","همه پایه‌ها"
        )) and "grades.forEach" not in member_picker,
        "toggle-off gender filters or single grade odometer missing")
require(school_screen.count("GradeOdometerPicker(") == 4 and
        profile_settings.count("GradeOdometerPicker(") == 1 and
        not re.search(r'label\s*=\s*\{\s*Text\("پایه"\)', school_screen + profile_settings),
        "not every editable school grade uses the shared odometer")
require(all(marker in grade_odometer for marker in (
            "rememberSnapFlingBehavior","GradeWheelDialog","LazyColumn","snapshotFlow",
            "Icons.Outlined.UnfoldMore","فهرست را به بالا یا پایین بکشید"
        )) and "Icons.Outlined.Speed" not in grade_odometer,
        "redesigned compact vertical grade wheel is incomplete")
require(student_card.count("Modifier.weight(1f).height(58.dp)") >= 5 and
        "copyStudentInformation(context, student)" in student_card and
        "Icons.Outlined.Delete" in student_card and
        "قابل بازیابی نیست" in school_screen and "student.password" not in school_screen,
        "larger student actions or secure profile-copy behavior incomplete")
require("رمز جدید اختیاری" in school_screen and "خالی بماند تغییر نمی‌کند" in school_screen and
        "رمز فعلی hash شده و قابل نمایش نیست" in school_screen and
        "copyOneTimeCredential" in school_screen and "android.content.extra.IS_SENSITIVE" in school_screen and
        "lastCredential = StudentCredential(request.id, request.username, password)" in classes_view_model and
        "request.newPassword.orEmpty()" in school_repository and
        not re.search(r"\b(val|var)\s+plain_password\b", main_text),
        "secure one-time new-password copy path incomplete or recoverable old-password storage returned")
require(v22_sql_copy == v22_migration and all(marker in v22_migration for marker in (
            "native_add_student_to_classes_v22","teacher_id = auth.uid()","on conflict do nothing","revoke all on function"
        )) and "native_add_student_to_classes_v22" in school_repository,
        "atomic owner-scoped multi-class membership migration incomplete")
teacher_menu=app_shell.split("val menuCards = if (user.role == UserRole.TEACHER)",1)[1].split("} else {",1)[0]
require(teacher_menu.index("دانش‌آموزان") < teacher_menu.index("\"تقویم\""),
        "student/calendar hamburger card positions were not swapped")
require("requests.size in 1..100" in school_repository,
        "bulk student creation no longer permits a single row")
require("HolidayRow(\"جمعه\")" not in calendar_screen.split("private fun SelectedDayCard(",1)[1].split("private fun HolidayRow(",1)[0] and
        "selected.officialHolidays.forEach" in calendar_screen and "day.isHoliday ->" in calendar_screen,
        "Friday text was not removed while preserving red holiday rendering")
require(all(marker in app_shell for marker in (
            "\"حساب\"","\"داده‌ها\"","Design69Icons.Account","Design69Icons.Data",
            "ProfileSettingsDestination.ACCOUNT","ProfileSettingsDestination.DATA"
        )), "independent account/data hamburger cards missing")
require(all(marker in profile_settings for marker in (
            "expandedCard = if (expandedCard == card) null else card",
            "Icons.Outlined.ExpandLess","Icons.Outlined.ExpandMore","AnimatedVisibility"
        )), "account cards do not independently expand/collapse")
require("state.downloading && it.notesFa.isNotEmpty()" in about_screen and
        "ChangeListCard" in about_screen and "localReleaseNotesFa" not in about_screen and
        all(marker not in about_screen for marker in (
            "AppIdentityCard","شناسه بسته","APK فقط از نشانی HTTPS"
        )), "release notes are not limited to a real in-progress update")
require(all(marker in builder_screen for marker in (
            "JalaliDateTimeField(\n                    \"شروع\"",
            "JalaliDateTimeField(\n                    \"پایان\"",
            "BoldToggleChip","detectDragGesturesAfterLongPress","Icons.Outlined.DragIndicator",
            "PersianDigits.convert(index + 1)","MinimalScoreField(",
            "private fun MinimalScoreField","Modifier.width(62.dp).height(40.dp)",
            "Icons.Outlined.Visibility","visible = styleExpanded"
        )) and "Text(\"↑\")" not in builder_screen and "Text(\"↓\")" not in builder_screen,
        "new exam-window/chip/drag/neon-score/print-eye builder behavior incomplete")
require(all(marker in date_time_picker for marker in (
            "DateMonthGrid","DateWeekHeader","LocalLayoutDirection provides LayoutDirection.Ltr",
            "Text(\"اکنون\"","Color(0xFF19945B)","Color(0xFFD63B49)",
            "Icons.Outlined.Check","Icons.Outlined.Close"
        )), "calendar-style date/time dialog controls incomplete")
require(all(marker in question_media for marker in (
            "LazyRow","items(images, key = MediaDraft::id)","freePlacement"
        )), "question image thumbnails are not laid out side-by-side")
require(all(marker in image_editor for marker in (
            "Icons.Outlined.RotateLeft","Icons.Outlined.RotateRight","Icons.Outlined.Crop",
            "CropFrame","CropEdge.LEFT","CropEdge.RIGHT","CropEdge.TOP","CropEdge.BOTTOM",
            "حجم تقریبی","Icons.Outlined.Check","Icons.Outlined.Close"
        )) and "Slider(" not in image_editor and "۴:۳" not in image_editor,
        "simplified live-size edge-drag crop editor incomplete")
require(all(marker in v24_guide for marker in (
            "امنیت رمز دانش‌آموز","منوی همبرگری","بازه آزمون","مشخصات و کارت سؤال","تصاویر",
            "SQL جدید: ندارد","Edge deploy: ندارد"
        )), "V24 Persian guide/handoff coverage incomplete")
require("TopAppBar(" in app_shell and
        "page.sectionTitle(user.role, profileDestination, schoolStudentsSelected)" in app_shell and
        "private fun MainPage.sectionTitle" in app_shell,
        "shared authenticated section headers are incomplete")
require(all(marker in date_time_picker for marker in (
            "val now = LocalDateTime.now()","hour = now.hour.toString()",
            "minute = now.minute.toString()","Icons.Outlined.Delete","onClear"
        )) and "onConfirm(Instant.now().toString())" not in date_time_picker,
        "Now still commits the boundary or clear-time control is missing")
require(all(marker in school_screen for marker in (
            "BoxWithConstraints","Alignment.BottomCenter","listMaxHeight = (maxHeight - 168.dp)",
            "title = { Text(\"حذف دانش‌آموز\") }","viewModel.deleteStudent(student.id)"
        )), "IME-tangent bulk dialog or confirmed student deletion missing")
require(all(marker in builder_screen for marker in (
            "private fun MinimalScoreField","BasicTextField(",
            "Modifier.width(62.dp).height(40.dp)","Modifier.animateItem(",
            "var dragActive","label = \"question-drag-color\"",
            "label = \"اتمام تلاش در پایان زمان\""
        )) and "val negativeMarking: String = \"\"" in question_model and
        "val attemptCooldown: String = \"\"" in question_model,
        "minimal score, smooth drag, centered chips, or blank defaults incomplete")
require(all(marker in neumorphic_design for marker in (
            "horizontalArrangement = Arrangement.spacedBy(9.dp)",
            "modifier = Modifier.weight(1f)"
        )) and all(marker in app_shell for marker in (
            "\"تقویم\", \"رویدادها و پیام‌ها\"",
            "\"دانش‌آموزان\", \"فهرست و وضعیت\"",
            "\"کلاس‌ها\", \"فهرست و مدیریت\""
        )) and "\"تقویم و پیام‌ها\"" not in app_shell,
        "hamburger icon/title row or compact card labels incomplete")
require(all(marker in local_image_repository for marker in (
            "decodeSampled(request.source)","inJustDecodeBounds = true","inSampleSize = sample",
            "MAX_DECODE_PIXELS = 7_000_000L","catch (_: OutOfMemoryError)"
        )), "large-image sampled decoding crash guard missing")
require(all(marker in v25_guide for marker in (
            "هدر سراسری","امنیت رمز دانش‌آموز","پنجره ساخت گروهی","منوی همبرگری",
            "رفع بسته‌شدن بخش تصویر","SQL جدید: ندارد","Edge deploy: ندارد"
        )), "V25 Persian guide/handoff coverage incomplete")
require(all(marker in appearance_preferences for marker in ("NeumorphicPalette","neumorphicPalette","neumorphicDepth","MIN_NEO_DEPTH","MAX_NEO_DEPTH")),
        "persistent Neumorphic palette/depth settings missing")
require(all(marker in app_theme for marker in ("accentColors","neumorphicLightColorScheme","neumorphicDarkColorScheme","vazirmatn_medium","vazirmatn_bold")),
        "Neumorphic theme or real Vazirmatn weights incomplete")
require("ظاهر نئومورفیک ۶۹" in profile_settings and "setNeumorphicPalette" in profile_settings and "setNeumorphicDepth" in profile_settings,
        "reachable Neumorphic appearance controls missing")
require("NeumorphicPanel" in wallet_screen and "balanceVisible" in wallet_screen and "۱۲٬۴۸۰٬۰۰۰" not in wallet_screen,
        "real wallet was not adapted safely to Neumorphic design")
require("com.example.neumorphic69" not in main_text and "۱۲٬۴۸۰٬۰۰۰" not in main_text,
        "standalone demo package or fake wallet data entered runtime")
require((ROOT/"app/src/main/java/ir/exam/app/core/export/XlsxWorkbook.kt").exists(),"real XLSX writer missing")
require((ROOT/"app/src/main/java/ir/exam/app/ui/image/InteractiveImageEditorDialog.kt").exists(),"interactive crop editor missing")
require((ROOT/"app/src/main/java/ir/exam/app/ui/security/AppLockUi.kt").exists(),"system credential app lock missing")
require((ROOT/"app/src/main/java/ir/exam/app/core/math/NativeMathAst.kt").exists(),"structured native math parser missing")
require(formula_library.exists() and formula_library.stat().st_size > 100_000,"complete formula reference asset missing")
formula_markers=("🖱️ جعبه‌ای","⌨️ تایپ سریع","📚 آماده","⭐ موارد پرکاربرد","🔢 اعداد و محاسبات","∫ آنالیز و توابع","𝑥 جبر و معادلات","∿ مثلثات و یونانی","⊆ مجموعه و منطق","📐 هندسه و بردار","🚀 فیزیک","🧪 شیمی","🔍 همهٔ نمادها","⚙ یونیکد (۱۲۰۰)","🕘 اخیر","✨ تبدیل","FixedFormulaKeypad")
formula_asset_text=formula_library.read_text(errors="ignore") if formula_library.exists() else ""
require(all(marker in formula_editor+formula_asset_text for marker in formula_markers),"formula editor order/reference controls incomplete")
require("۱۲۰۰" in formula_asset_text and "cur-phys-atomic" in formula_asset_text,"formula symbols/library reference incomplete")
require("io.coil-kt:coil-svg:2.7.0" in app_gradle,"Coil SVG decoder dependency missing")
require("SvgDecoder.Factory" in formula_view and "NativeMathSvgRenderer.render" in formula_view,
        "formula UI does not decode generated SVG")
require("NativeFormulaIcon" in formula_editor and "SvgFormulaEditorSurface" in formula_editor,
        "formula library/buttons/editor are not all routed through SVG")
require("segments.forEach" in formula_text and "NativeFormulaView" in formula_text and "mathAnnotated" not in formula_text,
        "simple question/option math segments can still bypass SVG")
require("Text(entry.tex" not in formula_editor,
        "raw TeX is still printed in formula library/menu")
require("<svg" in formula_svg and "escapeXml" in formula_svg and "sanitizeColor" in formula_svg,
        "safe self-contained native SVG generator missing")
require(all(marker in formula_svg for marker in ("MathSvgEditBox","<rect","activeBoxColor","radicalBars")),
        "touchable/color-active SVG boxes or stretchable radical metadata missing")
require("NativeFormulaEditorView" in formula_editor and "detectTapGestures" in formula_view and ".size(1.dp)" in formula_editor,
        "interactive box hit-testing is missing or blocked by the hidden input")
require("replaceActiveBoxWhenCollapsed" in formula_boxes and "moveActiveBox" in formula_boxes and
        "replaceActiveBox = true" in formula_editor,
        "formula libraries do not target the active box safely")
require("also(::validate)" in formula_reference_loader and "پیوند دسته نامعتبر" in formula_reference_loader and
        "fun decode" in formula_reference_loader,
        "formula library links/content are not validated")
require("usePlatformDefaultWidth = false" in formula_library_dialog and "LazyVerticalGrid" in formula_library_dialog and
        "Text(\"درج\")" in formula_library_dialog,
        "full-screen clickable formula library dialog missing")
require(all(marker in formula_editor for marker in ("openLibrary(\"common\")","openLibrary(\"__all\")","openLibrary(\"unicode\")","openLibrary(link.id")),
        "main formula library routes do not open visibly")
require("fun entries" in formula_library_nav and "fun search" in formula_library_nav,
        "formula library navigator missing")
require(all(marker in formula_smart for marker in ("کتابخانهٔ درس‌به‌درس","قالب‌های آماده","بسته‌های آماده","کلیدهای درشت","فرمول آخر")),
        "reachable Native Smart Hub is incomplete")
require(all(marker in formula_smart_data for marker in ("physics","chemistry","FormulaSmartPack","FormulaDelimiterPreset","bigKeyLabels")),
        "Smart Hub reference datasets are incomplete")
require("rightleftharpoons" in formula_natural and "normalizeChemistry" in formula_natural and "previousMarker" in formula_natural,
        "native natural/chemistry converter missing")
require("FormulaTextCodec" in formula_text_codec and "ExistingFormulaEditor" in builder_screen and "ExistingFormulaEditor" in matching_builder,
        "direct edit/delete of existing question option matching formulas missing")
require(all(marker in formula_editor for marker in ("ماتریس دلخواه ۱ تا ۱۰","onPreviewKeyEvent","combinedClickable","نمادهای اخیر","مرکز هوشمند")),
        "complete formula editor controls are not reachable")
require("moveSpatialBox" in formula_boxes and "typeCharacter" in formula_boxes and "importText" in formula_boxes,
        "spatial navigation structural typing or safe paste missing")
require("animateScrollTo" in formula_view and "verticalScroll" in formula_view,
        "active formula box auto-scroll missing")
require("version = 4" in (ROOT/"app/src/main/java/ir/exam/app/data/local/AppDatabase.kt").read_text(),"Room V4 student notes migration missing")

for match in re.finditer(
    r"(?im)^\s*(delete\s+from|update\s+)([^;]+);",
    hardening + "\n" + critical + "\n" + parity + "\n" + v18_migration + "\n" + v22_migration
):
    statement = match.group(0)
    if not re.search(r"(?i)\bwhere\b", statement):
        errors.append(f"UPDATE/DELETE without WHERE at line {hardening[:match.start()].count(chr(10))+1}")

if errors:
    print("FINAL_NATIVE_VERIFY=FAIL")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print(f"FINAL_NATIVE_VERIFY=PASS kotlin_files={len(main_files)} edge_functions={len(edge_files)}")
