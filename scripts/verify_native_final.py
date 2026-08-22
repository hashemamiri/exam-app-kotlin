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
data_portability=(ROOT/"app/src/main/java/ir/exam/app/ui/portability/DataPortabilitySection.kt").read_text()
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
v18_sql_copy=(ROOT/"sql/manual/SQL_NATIVE_NAVIGATION_ACCOUNT_V18.sql").read_text()
v22_migration=(ROOT/"supabase/migrations/20260814_native_student_class_membership_v22.sql").read_text()
v22_sql_copy=(ROOT/"sql/manual/SQL_NATIVE_STUDENT_MULTI_CLASS_V22.sql").read_text()
v24_guide=(ROOT/"docs/fa/COMPREHENSIVE_UX_V24_FA.md").read_text()
v25_guide=(ROOT/"docs/fa/HEADER_SAFETY_POLISH_V25_FA.md").read_text()
v26_guide=(ROOT/"docs/fa/QUESTION_MEDIA_REORDER_V26_FA.md").read_text()
v27_guide=(ROOT/"docs/fa/DATA_IMAGE_OPTIONS_V27_FA.md").read_text()
v28_migration=(ROOT/"supabase/migrations/20260814_native_field_of_study_v28.sql").read_text()
v28_sql_copy=(ROOT/"sql/manual/SQL_NATIVE_FIELD_OF_STUDY_V28.sql").read_text()
v28_guide=(ROOT/"docs/fa/REORDER_IMAGE_BULK_FIELD_V28_FA.md").read_text()
v29_guide=(ROOT/"docs/fa/BUILDER_MEDIA_BULK_V29_FA.md").read_text()
v30_guide=(ROOT/"docs/fa/SMOOTH_REORDER_CHANGELOG_V30_FA.md").read_text()
v31_guide=(ROOT/"docs/fa/STABLE_REORDER_UPDATE_PROMPT_V31_FA.md").read_text()
v36_migration=(ROOT/"supabase/migrations/20260815_native_school_manager_v36.sql").read_text()
v36_sql_copy=(ROOT/"sql/manual/SQL_NATIVE_SCHOOL_MANAGER_V36.sql").read_text()
v37_migration=(ROOT/"supabase/migrations/20260815_native_school_teacher_management_v37.sql").read_text()
v37_sql_copy=(ROOT/"sql/manual/SQL_NATIVE_SCHOOL_TEACHER_MANAGEMENT_V37.sql").read_text()
v38_migration=(ROOT/"supabase/migrations/20260815_native_manager_wallet_stats_v38.sql").read_text()
v38_sql_copy=(ROOT/"sql/manual/SQL_NATIVE_MANAGER_WALLET_STATS_V38.sql").read_text()
v381_migration=(ROOT/"supabase/migrations/20260815_native_manager_registration_v381_hotfix.sql").read_text()
v381_sql_copy=(ROOT/"sql/manual/SQL_NATIVE_MANAGER_REGISTRATION_V381_HOTFIX.sql").read_text()
v382_migration=(ROOT/"supabase/migrations/20260815_native_invite_digest_v382_hotfix.sql").read_text()
v382_sql_copy=(ROOT/"sql/manual/SQL_NATIVE_INVITE_DIGEST_V382_HOTFIX.sql").read_text()
v39_migration=(ROOT/"supabase/migrations/20260815_native_short_school_invite_v39.sql").read_text()
v39_sql_copy=(ROOT/"sql/manual/SQL_NATIVE_SHORT_SCHOOL_INVITE_V39.sql").read_text()
v40a_migration=(ROOT/"supabase/migrations/20260815_native_teacher_profile_v40a.sql").read_text()
v40a_sql_copy=(ROOT/"sql/manual/SQL_NATIVE_TEACHER_PROFILE_V40A.sql").read_text()
v40b_migration=(ROOT/"supabase/migrations/20260815_native_manager_teacher_cards_v40b.sql").read_text()
v40b_sql_copy=(ROOT/"sql/manual/SQL_NATIVE_MANAGER_TEACHER_CARDS_V40B.sql").read_text()
v40c_migration=(ROOT/"supabase/migrations/20260815_native_manager_class_students_v40c.sql").read_text()
v40c_sql_copy=(ROOT/"sql/manual/SQL_NATIVE_MANAGER_CLASS_STUDENTS_V40C.sql").read_text()
manager_class_screen=(ROOT/"app/src/main/java/ir/exam/app/ui/manager/ManagerTeacherClassScreen.kt").read_text()
school_join_repository=(ROOT/"app/src/main/java/ir/exam/app/data/repository/SupabaseSchoolJoinRepository.kt").read_text()
manager_repository=(ROOT/"app/src/main/java/ir/exam/app/data/repository/SupabaseManagerRepository.kt").read_text()
manager_foundation=(ROOT/"app/src/main/java/ir/exam/app/ui/manager/ManagerFoundationScreens.kt").read_text()
auth_view_model=(ROOT/"app/src/main/java/ir/exam/app/ui/auth/AuthViewModel.kt").read_text()
sign_in_screen=(ROOT/"app/src/main/java/ir/exam/app/ui/auth/SignInScreen.kt").read_text()
image_repository=(ROOT/"app/src/main/java/ir/exam/app/data/repository/LocalImageRepository.kt").read_text()
field_picker=(ROOT/"app/src/main/java/ir/exam/app/ui/common/FieldOfStudyPicker.kt").read_text()
school_screen=(ROOT/"app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt").read_text()
school_repository=(ROOT/"app/src/main/java/ir/exam/app/data/repository/SupabaseSchoolRepository.kt").read_text()
school_repository=(ROOT/"app/src/main/java/ir/exam/app/data/repository/SupabaseSchoolRepository.kt").read_text()
school_screen=(ROOT/"app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt").read_text()
grade_odometer=(ROOT/"app/src/main/java/ir/exam/app/ui/common/GradeOdometerPicker.kt").read_text()
calendar_screen=(ROOT/"app/src/main/java/ir/exam/app/ui/calendar/CalendarScreen.kt").read_text()
date_time_picker=(ROOT/"app/src/main/java/ir/exam/app/ui/builder/JalaliDateTimePicker.kt").read_text()
question_model=(ROOT/"app/src/main/java/ir/exam/app/ui/builder/QuestionDraft.kt").read_text()
local_image_repository=(ROOT/"app/src/main/java/ir/exam/app/data/repository/LocalImageRepository.kt").read_text()
question_media=(ROOT/"app/src/main/java/ir/exam/app/ui/image/QuestionMediaEditor.kt").read_text()
image_editor=(ROOT/"app/src/main/java/ir/exam/app/ui/image/InteractiveImageEditorDialog.kt").read_text()
crop_geometry=(ROOT/"app/src/main/java/ir/exam/app/ui/image/CropGeometry.kt").read_text()
student_password_vault=(ROOT/"app/src/main/java/ir/exam/app/data/local/StudentPasswordVault.kt").read_text()
image_uploader=(ROOT/"app/src/main/java/ir/exam/app/data/repository/SupabaseQuestionImageUploader.kt").read_text()
about_screen=(ROOT/"app/src/main/java/ir/exam/app/ui/update/AboutScreen.kt").read_text()
classes_view_model=(ROOT/"app/src/main/java/ir/exam/app/ui/classes/ClassesViewModel.kt").read_text()
grading_screen=(ROOT/"app/src/main/java/ir/exam/app/ui/grading/GradingScreen.kt").read_text()
formula_editor=(ROOT/"app/src/main/java/ir/exam/app/ui/math/MathEditorWebViewDialog.kt").read_text()
inline_math_editor=(ROOT/"app/src/main/java/ir/exam/app/ui/math/InlineMathTextEditor.kt").read_text()
rich_text=(ROOT/"app/src/main/java/ir/exam/app/core/text/RichText.kt").read_text()
figure_picker=(ROOT/"app/src/main/java/ir/exam/app/ui/figure/FigurePickerDialog.kt").read_text()
formula_view=(ROOT/"app/src/main/java/ir/exam/app/ui/math/NativeFormulaView.kt").read_text()
formula_text=(ROOT/"app/src/main/java/ir/exam/app/ui/math/NativeMathText.kt").read_text()
formula_svg=(ROOT/"app/src/main/java/ir/exam/app/core/math/NativeMathSvgRenderer.kt").read_text()
formula_natural=(ROOT/"app/src/main/java/ir/exam/app/core/math/NativeNaturalMathConverter.kt").read_text()
formula_text_codec=(ROOT/"app/src/main/java/ir/exam/app/core/math/FormulaTextCodec.kt").read_text()
matching_builder=(ROOT/"app/src/main/java/ir/exam/app/ui/builder/QuestionOptionMedia.kt").read_text()
app_gradle=(ROOT/"app/build.gradle.kts").read_text()
math_editor_asset=ROOT/"app/src/main/assets/math_editor_standalone.html"

webview_files=[path for path in main_files if "android.webkit" in path.read_text(errors="ignore")]
require(len(webview_files) == 1 and webview_files[0].name == "MathEditorWebViewDialog.kt",
        "WebView/android.webkit import remains outside the isolated MathEditorWebViewDialog")
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
require("app_version?select=version_code" in workflow and "is_active=eq.true" in workflow,
        "CI update check must test the app_version public read path")
require("check_app_update RPC status (informational)" in workflow,
        "CI must not be blocked by the optional check_app_update RPC")
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
require(all(marker in design69_cards for marker in ("CARD_COUNT = 6","DRAG_THRESHOLD_DP = 52","detectDragGestures","Key.DirectionLeft","Key.DirectionRight","\"آمار\"","بانک سؤال","\"تصحیح\"","\"مانده\"","\"پاسخ\"","cards[activeIndex].subtitle")) and
        "Key.DirectionDown" not in design69_cards and "بکشید" not in design69_cards,
        "six-card horizontal-only management stack/description incomplete")
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
require("if (user.role == UserRole.MANAGER) MainPage.HOME else MainPage.CALENDAR" in app_shell and all(marker in app_shell for marker in (
            "\"تقویم\"","\"کلاس‌ها\"","\"دانش‌آموزان\"","\"سربرگ\"",
            "\"حساب\"","\"داده‌ها\"","\"تنظیمات\"","\"خروج\""
        )), "calendar default or exact hamburger menu order/routes missing")
require(all(marker not in app_shell.split("val menuCards = if (user.role == UserRole.TEACHER)",1)[1].split("} else if (user.role == UserRole.MANAGER)",1)[0]
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
        # V28: کلاینت به v3 مهاجرت کرد و v3 داخل SQL همان v2 را زنجیره می‌کند.
        "native_export_backup_v3" in portability_repository and
        "native_export_backup_v2()" in v28_migration,
        "server/print/backup header grade migration or SQL copy incomplete")
require(all(marker in builder_screen for marker in ("expandedQuestionId","settingsExpanded","bottom = 112.dp","FabPosition.Center","Icons.Outlined.Check","BuilderRadialMenuOverlay","BuilderQuestionBankDialog")) and
        all(label in builder_radial for label in ("تشریحی","چندگزینه‌ای","صحیح/غلط","جای خالی","عددی","جورکردنی","وارد کردن","بانک سؤال")),
        "builder accordion/radial eight actions/bounded floating save incomplete")
require("dottedAlpha" in builder_radial and "progress.animateTo(1f, tween(620" in builder_radial and
        "fun addQuestion(type: QuestionType): String" in (ROOT/"app/src/main/java/ir/exam/app/ui/builder/ExamBuilderViewModel.kt").read_text(),
        "builder synchronized radial motion or auto-open question contract missing")
require("PersianUsernameSuggester.suggest" in school_screen and "BulkStudentDraft" in school_screen and
        "contentAlignment = Alignment.TopCenter" in school_screen and "🎲" in school_screen,
        "compact single/bulk student dialogs or username suggestions incomplete")
require(all("PullToRefreshBox" in text and "تازه‌سازی" not in text for text in (
            school_screen, question_bank_screen,
            (ROOT/"app/src/main/java/ir/exam/app/ui/calendar/CalendarScreen.kt").read_text(), wallet_screen
        )), "manual refresh button remains or pull-to-refresh is missing")
require("LocalLayoutDirection provides LayoutDirection.Ltr" in formula_native_view and
        "horizontal.animateScrollTo(targetX)" in formula_native_view,
        "LTR formula rendering or automatic active-box scroll missing")
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
require("title = primaryTitle" in design69_add and
        design69_add.index("title = primaryTitle") < design69_add.index("title = \"دانش‌آموز جدید\""),
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
        "copyStudentInformation(" in student_card and
        "knownPasswordOf(student.username)" in student_card and
        "Icons.Outlined.Delete" in student_card and
        "add(\"رمز:" in school_screen and "student.password" not in school_screen and
        "شناسه حساب:" not in school_screen and "وضعیت:" not in school_screen,
        "larger student actions or exact secure profile-copy behavior incomplete")
require("رمز جدید اختیاری" in school_screen and "currentPassword: String?" in school_screen and
        "value = currentPassword.orEmpty()" in school_screen and
        "رمز فعلی hash شده و قابل نمایش نیست" not in school_screen and
        "copyOneTimeCredential" in school_screen and "android.content.extra.IS_SENSITIVE" in school_screen and
        "lastCredential = StudentCredential(request.id, request.username, password)" in classes_view_model and
        "request.newPassword.orEmpty()" in school_repository and
        not re.search(r"\b(val|var)\s+plain_password\b", main_text),
        "secure one-time new-password copy path incomplete or recoverable old-password storage returned")
require(v22_sql_copy == v22_migration and all(marker in v22_migration for marker in (
            "native_add_student_to_classes_v22","teacher_id = auth.uid()","on conflict do nothing","revoke all on function"
        )) and "native_add_student_to_classes_v22" in school_repository,
        "atomic owner-scoped multi-class membership migration incomplete")
teacher_menu=app_shell.split("val menuCards = if (user.role == UserRole.TEACHER)",1)[1].split("} else if (user.role == UserRole.MANAGER)",1)[0]
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
require("state.update?.takeIf { it.notesFa.isNotEmpty() }" in about_screen and
        "downloadedApkPath == null && it.notesFa.isNotEmpty()" not in about_screen and
        "نسخه نصب‌شده: ${BuildConfig.VERSION_NAME}" in about_screen and
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
            "CropFrame","CropEdgeKind.LEFT","CropEdgeKind.RIGHT","CropEdgeKind.TOP","CropEdgeKind.BOTTOM",
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
            "BoxWithConstraints","Alignment.TopCenter","heightIn(max = availableHeight)",
            "SOFT_INPUT_ADJUST_RESIZE","title = { Text(\"حذف دانش‌آموز\") }",
            "viewModel.deleteStudent(student.id)"
        )) and "Alignment.BottomCenter" not in school_screen.split("private fun BulkStudentDialog(",1)[1].split("internal fun studentClipboardText",1)[0],
        "top IME-tangent bulk dialog or confirmed student deletion missing")
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
            "decodeSampled(request.source, attempt)","inJustDecodeBounds = true","inSampleSize = sample",
            "MAX_DECODE_PIXELS = 7_000_000L","catch (oom: OutOfMemoryError)"
        )), "large-image sampled decoding crash guard missing")
require(all(marker in v25_guide for marker in (
            "هدر سراسری","امنیت رمز دانش‌آموز","پنجره ساخت گروهی","منوی همبرگری",
            "رفع بسته‌شدن بخش تصویر","SQL جدید: ندارد","Edge deploy: ندارد"
        )), "V25 Persian guide/handoff coverage incomplete")
require("topBar = {\n                        if (!menuOpen)" in app_shell,
        "shared header does not hide while hamburger menu is open")
require(all(marker in date_time_picker for marker in (
            "selected = today","visibleYear = today.year","minimumIso","minimumInstant",
            "زمان پایان نمی‌تواند قبل از زمان شروع باشد"
        )) and "minimumIso = state.opensAtIso" in builder_screen and
        "instantBefore(value, it)" in (ROOT/"app/src/main/java/ir/exam/app/ui/builder/ExamBuilderViewModel.kt").read_text() and
        "!Instant.parse(state.closesAtIso).isBefore(Instant.parse(state.opensAtIso))" in
            (ROOT/"app/src/main/java/ir/exam/app/data/repository/SupabaseExamBuilderRepository.kt").read_text(),
        "Now date/time or start-before-end validation is incomplete")
require("android:windowSoftInputMode=\"adjustResize\"" in manifest and
        "SOFT_INPUT_ADJUST_RESIZE" in school_screen and "DialogWindowProvider" in school_screen,
        "bulk dialog does not enforce adjustResize above the device keyboard")
require(all(marker in image_editor for marker in (
            "repository.prepare(ImageEditRequest(source))","safeSource = prepared.uri",
            "model = safeSource","!busy && !preparing && safeSource != null",
            "safeImagePixelSize(sourcePixels)","if (size == Size.Unspecified) Size(1f, 1f)"
        )) and "sourcePixels.width" not in image_editor and "sourcePixels.height" not in image_editor,
        "image editor preflight or Size.Unspecified crash guard is incomplete")
require("onDragStarted = { expandedQuestionId = null }" in builder_screen and
        "onDragStarted()" in builder_screen,
        "question drag does not close every accordion")
multiple_choice=builder_screen.split("QuestionType.MULTIPLE_CHOICE ->",1)[1].split("QuestionType.TRUE_FALSE ->",1)[0]
require(all(marker in multiple_choice for marker in (
            "persianOptionLetter(index)","Icons.Outlined.Functions","SingleImagePicker(","ReorderDragButton("
        )) and "↑ گزینه" not in multiple_choice and "↓ گزینه" not in multiple_choice and
        "گزینه ${index + 1}" not in multiple_choice,
        "multiple-choice Persian labels/media tools/drag reorder incomplete")
matching_body=matching_builder.split("fun MatchingQuestionEditor(",1)[1]
require(matching_body.index("Text(\"ستون راست\"") < matching_body.index("Text(\"ستون چپ\"") and
        "val label = persianOptionLetter(index)" in matching_body and
        "val label = PersianDigits.convert(index + 1)" in matching_body and
        "ReorderDragButton" in matching_builder and "Text(\"↑\")" not in matching_builder and
        "Text(\"↓\")" not in matching_builder,
        "matching right-first letters/left-numbers drag layout incomplete")
require("Icons.Outlined.PhotoCamera" in question_media and "Modifier.size(30.dp)" in question_media and
        "Card(Modifier.width(156.dp))" not in question_media and "Slider(" not in question_media and
        "Icons.Outlined.PhotoCamera" in matching_builder and "Modifier.size(30.dp)" in matching_builder and
        "Modifier.size(72.dp)" not in matching_builder,
        "compact icon-sized text/option images beside camera incomplete")
require(all(marker in v26_guide for marker in (
            "امنیت رمز دانش‌آموز","مسیر امن تصویر","چندگزینه‌ای","جورکردنی",
            "SQL جدید: ندارد","Edge deploy: ندارد"
        )), "V26 Persian guide/handoff coverage incomplete")
require("QuestionType.MULTIPLE_CHOICE -> \"چندگزینه‌ای\"" in builder_screen and
        re.search(r"چهار.?گزینه", main_text) is None,
        "four-choice wording remains instead of multiple-choice")
require(all(marker in question_model for marker in (
            "optionIds","matchingLeftIds","matchingRightIds"
        )) and "ensureEditorIds" in (ROOT/"app/src/main/java/ir/exam/app/ui/builder/ExamBuilderViewModel.kt").read_text() and
        "while (abs(accumulated) >= stepPx)" in matching_builder and
        "onMove(dragIndex, delta)" in matching_builder and
        "key(optionId) {" in builder_screen and "key(itemId) {" in matching_builder,
        "live stable-id option/matching reorder is incomplete")
require("safeUris += it.uri.toString()" in question_media and "editQueue.firstOrNull" not in question_media and
        "onSuccess { editing = Uri.parse(it.uri.toString()) }" in matching_builder and
        "onSuccess { avatarEditing = it.uri }" in profile_settings,
        "raw picker images still reach the editor before safe preprocessing")
menu_tile=neumorphic_design.split("fun NeumorphicMenuTile(",1)[1].split("/** Morph واقعی",1)[0]
require("colors.accent.copy(alpha = .14f)" in menu_tile and
        not (".width(18.dp)" in menu_tile and ".height(5.dp)" in menu_tile),
        "selected hamburger card color or removed dash marker is incorrect")
require("fillMaxSize().verticalScroll(rememberScrollState())" in data_portability and
        "ExamPackageCodec.decode(raw)" in data_portability and "importExam.launch" in data_portability and
        "onImportExam = onImportExam" in profile_settings and "onImportExam = { draft ->" in app_shell and
        data_portability.split("Text(\"نگهداری امن Storage\"",1)[1].count("modifier = Modifier.fillMaxWidth()") >= 2,
        "scrollable complete Storage/data exam import path is incomplete")
require(all(marker in grade_odometer for marker in (
            "OtherGradeValue","values + OtherGradeValue","OtherGradeValue -> \"سایر\"",
            "customMode = true","customLabel: String = \"سایر پایه\"","label = { Text(customLabel) }"
        )), "grade wheel Other/custom text path is incomplete")
require(all(marker in v27_guide for marker in (
            "مسیر انتخاب تصویر","داده‌ها و Storage","پایه سایر","امنیت رمز",
            "SQL جدید: ندارد","Edge deploy: ندارد"
        )), "V27 Persian guide/handoff coverage incomplete")
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
require(math_editor_asset.exists() and math_editor_asset.stat().st_size > 500_000,
        "standalone math editor asset missing")
math_editor_text=math_editor_asset.read_text(errors="ignore")
require("function openMath(targetId)" in math_editor_text and "function mfApply()" in math_editor_text,
        "standalone math editor page lost its host contract functions")
require("io.coil-kt:coil-svg:2.7.0" in app_gradle,"Coil SVG decoder dependency missing")
require("SvgDecoder.Factory" in formula_view and "NativeMathSvgRenderer.render" in formula_view,
        "formula UI does not decode generated SVG")
require("segments.forEach" in formula_text and "NativeFormulaView" in formula_text and "mathAnnotated" not in formula_text,
        "simple question/option math segments can still bypass SVG")
require("<svg" in formula_svg and "escapeXml" in formula_svg and "sanitizeColor" in formula_svg,
        "safe self-contained native SVG generator missing")
require(all(marker in formula_svg for marker in ("MathSvgEditBox","<rect","activeBoxColor","radicalBars")),
        "touchable/color-active SVG boxes or stretchable radical metadata missing")
require("detectTapGestures" in formula_view and "animateScrollTo" in formula_view and "verticalScroll" in formula_view,
        "interactive box hit-testing or active formula box auto-scroll missing")
require("rightleftharpoons" in formula_natural and "normalizeChemistry" in formula_natural and "previousMarker" in formula_natural,
        "native natural/chemistry converter missing")
require("FormulaTextCodec" in formula_text_codec and "ExistingFormulaEditor" in builder_screen and "ExistingFormulaEditor" in matching_builder,
        "direct edit/delete of existing question option matching formulas missing")
require("math_editor_standalone.html" in formula_editor and "file:///android_asset" in formula_editor and
        "AndroidMathBridge" in formula_editor and "addJavascriptInterface" in formula_editor and
        "usePlatformDefaultWidth = false" in formula_editor and "AndroidView" in formula_editor,
        "web formula editor dialog is not a full-screen WebView with an Android bridge")
require("openMath('qTxt_1')" in formula_editor and "window.mfApply" in formula_editor and "window.closeMath" in formula_editor and
        "AndroidMathBridge.onApplyResult" in formula_editor and "AndroidMathBridge.onClosed" in formula_editor,
        "web formula editor bridge does not follow the qTxt_1 openMath/mfApply/closeMath contract")
require("MathEditorWebViewDialog(" in builder_screen and "MathEditorWebViewDialog" in builder_screen and
        "FormulaEditorDialog" not in main_text and "FormulaBoxEditor" not in main_text and
        "FormulaLibraryNavigator" not in main_text and "FormulaSmartHubDialog" not in main_text,
        "formula editor is not switched to the WebView dialog or native editor remnants remain")

# ---- V45.8: فایل تک‌فایلی formula-editor-window (همه‌چیز داخل asset است) ----
# از این نسخه به بعد، ویرایشگر شامل هسته، V34 و لایهٔ میزبان در یک فایل
# math_editor_standalone.html بسته می‌شود. فایل قدیمی install_lib_v34.js و
# helper مربوطه (FormulaV34Library.kt) نباید وجود داشته باشند.
_asset_path = ROOT / "app/src/main/assets/math_editor_standalone.html"
require(_asset_path.exists() and _asset_path.stat().st_size > 500_000,
        "self-contained formula editor asset (math_editor_standalone.html) missing or truncated")
_asset_text = _asset_path.read_text(encoding="utf-8")
require("function installLibV34(w)" in _asset_text,
        "self-contained editor must embed installLibV34 directly")
for token in (
    "school", "type", "bio",
    "v34-math10", "v34-hesaban1", "v34-discrete",
    "v34-accents", "v34-arrows", "v34-special-let",
    "v34-bio", "v34-uni",
):
    require(token in _asset_text, f"self-contained editor missing V34 token: {token}")
require('id="host-bridge"' in _asset_text,
        "self-contained editor must include the host-bridge script block")
require('id="auto-open"' not in _asset_text,
        "auto-open script must be stripped from the Android asset")
require("cdn-cgi/challenge-platform" not in _asset_text,
        "Cloudflare challenge script must be stripped from the Android asset")
require(not (ROOT / "app/src/main/assets/formula/install_lib_v34.js").exists(),
        "legacy V34 asset file must be removed after switching to self-contained editor")
require(not (ROOT / "app/src/main/java/ir/exam/app/ui/math/FormulaV34Library.kt").exists(),
        "FormulaV34Library.kt helper must be removed after switching to self-contained editor")
require("FormulaV34Library" not in formula_editor,
        "dialog must not reference the removed FormulaV34Library")

# V45.7.3: WebViewهای قدیمی 100dvh را نمی‌فهمند و جعبهٔ تمام‌صفحه ارتفاع صفر می‌گیرد
require("VIEWPORT_FALLBACK_JS" in formula_editor and "100dvh" in formula_editor
        and ("100vh" in formula_editor or "height:100%" in formula_editor),
        "formula dialog must inject a height fallback for 100dvh on old WebViews")
# V45.8.2: فیکس چیدمان با ابعاد پیکسلی واقعی viewport
require("__mbForceLayout" in formula_editor and "innerHeight" in formula_editor,
        "formula dialog must force pixel layout via __mbForceLayout")
# بستن دیالوگ باید تضمینی باشد (fallback تایمر برای onDismiss)
require("DISMISS_FALLBACK_MS" in formula_editor and "dismissOnce" in formula_editor,
        "formula dialog must guarantee dismissal via a timer fallback")
# پل تشخیصی برای لاگ JS در logcat
require("AndroidMathBridge.log" in formula_editor and "onConsoleMessage" in formula_editor,
        "formula dialog must forward JS console/diagnostic logs to logcat")
require("version = 4" in (ROOT/"app/src/main/java/ir/exam/app/data/local/AppDatabase.kt").read_text(),"Room V4 student notes migration missing")

# ---- V28: reorder / image safety / bulk window / field of study ----
require("rememberUpdatedState(currentIndex)" in matching_builder and
        "rememberUpdatedState(itemCount)" in matching_builder and
        "HapticFeedbackType.LongPress" in matching_builder and
        "onDragScroll(amount.y)" in matching_builder and
        "animateColorAsState" in matching_builder,
        "V28 option/matching drag does not match the question-card contract")
require("userScrollEnabled = !innerReorderActive" in builder_screen and
        "onItemDragStarted = { innerReorderActive = true }" in builder_screen and
        "onItemDragEnded = { innerReorderActive = false }" in builder_screen and
        "onItemDragScroll = onDragScroll" in builder_screen,
        "V28 inner reorder does not lock the builder list scroll")
require("catch (oom: OutOfMemoryError)" in image_repository and
        "is OutOfMemoryError -> IllegalStateException" in image_repository and
        "while (attempt < MAX_ATTEMPTS)" in image_repository and
        "runtime.maxMemory()" in image_repository and
        "Bitmap.Config.RGB_565" in image_repository and
        "working?.recycle()" in image_repository,
        "V28 image intake can still kill the process on low memory")
_bulk = school_screen.split("private fun BulkStudentDialog(", 1)[1].split("internal fun studentClipboardText", 1)[0]
require("widthIn(max = 620.dp)" in _bulk and
        "padding(horizontal = 14.dp, vertical = 10.dp)" in _bulk and
        "heightIn(max = availableHeight)" in _bulk and
        "height(maxHeight)" not in _bulk and
        "LazyColumn(" not in _bulk and
        "weight(1f, fill = false)" not in _bulk,
        "V28 bulk window does not match the single student window")
require("activeIndex = rows.lastIndex" in _bulk and
        "rememberLazyListState()" in _bulk and
        "animateScrollToItem(activeIndex)" in _bulk and
        "val row = rows[index]" in _bulk,
        "V28 bulk plus button does not replace the visible card")
require("StandardFieldsOfStudy" in field_picker and
        "customLabel = \"سایر رشته\"" in field_picker and
        "standardValues: List<String> = StandardSchoolGrades" in grade_odometer and
        "customLabel: String = \"سایر پایه\"" in grade_odometer,
        "V28 field-of-study picker or shared wheel parameters incomplete")
require(school_screen.count("FieldOfStudyPicker(") >= 4 and
        "FieldOfStudyPicker" in profile_settings and
        "onFieldOfStudy" in profile_settings,
        "V28 field of study is not reachable in every student/class/header form")
require("native_save_student_extra_v28" in school_repository and
        "native_save_class_v28" in school_repository and
        "native_my_classes_v28" in school_repository and
        '"save_student_extra"' not in school_repository and
        '"create_class"' not in school_repository and
        '"update_class"' not in school_repository,
        "V28 school repository still uses field-less legacy RPCs")
require("native_save_profile_v28" in profile_repository and
        'put("p_hdr_field"' in profile_repository and
        "native_export_backup_v3" in portability_repository and
        "native_restore_backup_v3" in portability_repository and
        "in 1..4" in portability_repository,
        "V28 header field or backup version 4 path incomplete")
require("field_of_study" in v28_migration and "hdr_field" in v28_migration and
        "plain_password" not in v28_migration and
        v28_migration.count("to authenticated") >= 8,
        "V28 migration columns, grants or password policy incorrect")
require(v28_sql_copy.strip() == v28_migration.strip(),
        "sql/manual/SQL_NATIVE_FIELD_OF_STUDY_V28.sql differs from the migration")
require(all(marker in v28_guide for marker in (
            "جابه‌جایی گزینه","انتخاب تصویر","پنجره گروهی","رشته تحصیلی","امنیت رمز"
        )), "V28 Persian guide coverage incomplete")

# ---- V29: reorder parity fix / formula+camera row / full-size viewer / edit-after-pick / single-card bulk ----
require("pointerInput(Unit)" in matching_builder,
        "V29 option/matching drag is still keyed on a label that changes while dragging")
require("ReorderStepDp.dp.toPx()" in builder_screen and "const val ReorderStepDp: Float = 52f" in matching_builder,
        "V29 question-card and option drag thresholds are not the same")
# V45 طراحی عمدی: آیکن فرمول از سطر دوربین به زیر کادر متن سؤال (InlineMathTextEditor)
# منتقل شد؛ قرارداد V29 با طراحی V45 هماهنگ شده است.
require("Icons.Outlined.Functions" in inline_math_editor and
        "onInsertFormula" in inline_math_editor,
        "V45 formula icon is not below the text box in the inline math editor")
require("Icons.Outlined.PhotoCamera" in question_media and
        "Icons.Outlined.Functions" not in question_media,
        "V45 formula icon must not remain in the question media camera row")
require("OutlinedButton(onClick = { formulaTarget = FormulaTarget(\"question\") })" not in builder_screen,
        "V29 text formula button is not converted to an icon beside the camera")
# V45.3: keep editable text slots on both sides of an inline token; the trailing
# empty slot is what lets the user continue typing after inserting a formula.
require("if (start >= cursor)" in rich_text and
        "if (cursor <= source.length)" in rich_text and
        "if (showPlaceholder && part.text.isEmpty())" in inline_math_editor and
        "Box(contentAlignment = Alignment.CenterStart)" in inline_math_editor,
        "inline formula editor does not keep editable text slots around tokens")
# V45.3: figure and graph insertion are separate, type-first flows. The editor
# no longer exposes a shared shape/graph tab after the type picker.
require("FigureTypePickerDialog(" in figure_picker and
        "ابتدا نوع شکل هندسی را انتخاب کنید" in figure_picker and
        "ابتدا نوع نمودار را انتخاب کنید" in figure_picker and
        "GeometryEditorPane" in figure_picker and
        "GraphEditorPane" in figure_picker and
        "FilterChip(" not in figure_picker and
        "chooseType: Boolean = false" in builder_screen and
        "FigureTarget(kind = FigureKind.GEOMETRY, chooseType = true)" in builder_screen and
        "FigureTarget(kind = FigureKind.GRAPH, chooseType = true)" in builder_screen and
        "target.copy(initialSpec = spec, chooseType = false)" in builder_screen,
        "V45.3 type-first figure/graph insertion flow is incomplete")
full_viewer=(ROOT/"app/src/main/java/ir/exam/app/ui/image/FullScreenImageViewer.kt").read_text()
require("FullScreenImageViewer" in question_media and
        "detectTransformGestures" in full_viewer and
        "MAX_ZOOM" in full_viewer and
        "Icons.Outlined.Close" in full_viewer and
        "viewerUri" in question_media and
        "onView" in question_media,
        "V29 full-size zoomable image viewer is not wired to thumbnails")
require("FullScreenImageViewer" in matching_builder and
        "viewing = value" in matching_builder and
        "Icons.Outlined.Edit" in matching_builder and
        "reEditTarget" in question_media,
        "V29 option/thumbnails cannot open the viewer or re-edit")
require("batchQueue = safeUris.map(Uri::parse)" in question_media and
        "InteractiveImageEditorDialog(" in question_media and
        "batchQueue.firstOrNull()" in question_media,
        "V29 picked images do not open the editor before being added")
require("fun replaceImage" in (ROOT/"app/src/main/java/ir/exam/app/ui/builder/ExamBuilderViewModel.kt").read_text(),
        "V29 replace-image path missing in builder ViewModel")
require("activeIndex = rows.lastIndex" in _bulk and
        "rowComplete" in _bulk and
        "rememberLazyListState()" in _bulk and
        "selected = activeIndex == index" in _bulk,
        "V29 bulk single visible card with the numbered chips missing")
require(all(marker in v29_guide for marker in (
            "جابه‌جایی پایدار گزینه","آیکن فرمول","نمایش تمام‌صفحه تصویر","ویرایش پس از انتخاب","پنجره گروهی تک‌کارتی","امنیت رمز دانش‌آموز"
        )), "V29 Persian guide coverage incomplete")

# ---- V30: smooth colored reorder / collapsed settings / Persian changelog / image editor fixes / clean bulk window ----
require("optionDragId == optionId" in builder_screen and
        "label = \"option-card-color\"" in builder_screen and
        "CardDefaults.cardColors(containerColor = optionCardColor)" in builder_screen,
        "V30 dragged option card is not highlighted like the question card")
require("dragActiveId == itemId" in matching_builder and
        "label = \"matching-card-color\"" in matching_builder and
        "onActiveChanged: (Boolean) -> Unit = {}" in matching_builder,
        "V30 matching card highlight is missing")
require("key(optionId) {" in builder_screen and
        "key(itemId) {" in matching_builder and
        "AnimatedReorderColumn(" not in builder_screen and
        "AnimatedReorderColumn(" not in matching_builder,
        "V30 option/matching cards lost their stable keyed list")
require("var settingsExpanded by rememberSaveable { mutableStateOf(false) }" in builder_screen and
        "بازکردن کارت سؤال، کارت مشخصات آزمون را می‌بندد." in builder_screen and
        "settingsExpanded = false" in builder_screen,
        "V30 exam settings card is not collapsed by default or does not close on question open")
require("state.update?.takeIf { it.notesFa.isNotEmpty() }" in about_screen and
        "downloadedApkPath == null && it.notesFa.isNotEmpty()" not in about_screen and
        "removePrefix(\"•\")" in about_screen and
        "LaunchedEffect(Unit) { viewModel.check(BuildConfig.VERSION_CODE) }" in about_screen,
        "V30 about screen still hides the Persian changelog")
require("text/CHANGELOG_FA.txt" in workflow and
        "removeprefix(\"-\")" in workflow and
        (ROOT/"text/CHANGELOG_FA.txt").exists(),
        "V30 CI does not publish real Persian release notes")
require((ROOT/"app/src/main/java/ir/exam/app/ui/image/CropGeometry.kt").exists() and
        "CropGeometry.cropRect(" in image_editor and
        "CropGeometry.resizeSide(" in image_editor and
        "focusManager.clearFocus()" in image_editor and
        "verticalScroll(rememberScrollState())" in image_editor and
        "heightIn(max = maxDialogHeight.dp)" in image_editor,
        "V30 image editor keyboard/scroll/geometry fixes missing")
require("selected = classId == item.id" not in _bulk and
        "horizontalScroll(rememberScrollState())" not in _bulk and
        "DropdownMenu(" not in _bulk and
        "classId" not in _bulk,
        "V30 bulk window still shows the classes row")
require(all(marker in v30_guide for marker in (
            "جابه‌جایی رنگی","مشخصات آزمون","لیست تغییرات","ویرایش تصویر","پنجره گروهی"
        )), "V30 Persian guide coverage incomplete")

# ---- V31: stable reorder / audience in settings / update prompt / upload OOM guard / classless bulk ----
require("key(optionId) {" in builder_screen and
        "key(itemId) {" in matching_builder and
        "AnimatedReorderColumn(" not in builder_screen and
        "AnimatedReorderColumn(" not in matching_builder and
        "optionDragId == optionId" in builder_screen,
        "V31 option reorder is not the stable keyed list with colored cards")
require("AudienceCard(state, viewModel)" in builder_screen.split(
    "visible = settingsExpanded", 1)[1].split("AnimatedVisibility(", 1)[0].split("state.importedBy", 1)[0] and
        "item { AudienceCard(state, viewModel) }" not in builder_screen,
        "V31 audience section is not inside the exam settings card")
require("LaunchedEffect(user.id) { updateViewModel.check(BuildConfig.VERSION_CODE) }" in app_shell and
        "updatePromptDismissed" in app_shell and
        "بروزرسانی جدید" in app_shell and
        "بعداً" in app_shell,
        "V31 app-entry update prompt missing")
require("while (attempt < MAX_ATTEMPTS)" in image_uploader and
        "catch (oom: OutOfMemoryError)" in image_uploader and
        "maxDimension shr attempt" in image_uploader and
        "MAX_DECODE_PIXELS = 7_000_000L" in image_uploader,
        "V31 upload path can still crash on OutOfMemoryError")
require("onCreate: (List<NewStudentRequest>) -> Unit" in _bulk and
        "رمز فعلی" in _bulk and
        "readOnly = true" in _bulk and
        "rememberLazyListState()" in _bulk and
        "classId" not in _bulk,
        "V31 classless bulk window with current-password box incomplete")
require("knownPasswords[it.username.lowercase()]=it.password" in school_screen and
        "knownPasswordOf(student.username)" in school_screen and
        "add(\"رمز:" in school_screen,
        "V31 student-card copy does not read the current-password box")
require("createStudentsBulk(classId:String?" in school_repository and
        'put("class_id",classId.orEmpty())' in school_repository,
        "V31 bulk creation still requires a class")
require(all(marker in v31_guide for marker in (
            "غیب‌شدن گزینه","مخاطبان آزمون","پیغام آپدیت","کرش آپلود","پنجره گروهی"
        )), "V31 Persian guide coverage incomplete")

for match in re.finditer(
    r"(?im)^\s*(delete\s+from|update\s+)([^;]+);",
    hardening + "\n" + critical + "\n" + parity + "\n" + v18_migration + "\n" + v22_migration
    + "\n" + v28_migration
):
    statement = match.group(0)
    if not re.search(r"(?i)\bwhere\b", statement):
        errors.append(f"UPDATE/DELETE without WHERE at line {hardening[:match.start()].count(chr(10))+1}")

# V34 — ترتیب ابزارها، thumbnail هم‌سطری، Vault محلی و برش جهت‌دار/دایره‌ای
_v34_multiple=builder_screen.split("QuestionType.MULTIPLE_CHOICE ->",1)[1].split("QuestionType.TRUE_FALSE ->",1)[0]
require(_v34_multiple.index("Icons.Outlined.Functions") < _v34_multiple.index("ReorderDragButton(") <
        _v34_multiple.index("SingleImagePicker("),
        "V34 multiple-choice reorder is not immediately after formula")
_v34_matching=matching_builder.split("private fun MatchingItemTools(",1)[1].split("fun MatchingQuestionEditor(",1)[0]
require(_v34_matching.index("Icons.Outlined.Functions") < _v34_matching.index("ReorderDragButton(") <
        _v34_matching.index("SingleImagePicker("),
        "V34 matching reorder is not immediately after formula")
_v34_thumb=question_media.split("private fun CompactImageThumbnail(",1)[1]
require(all(marker in _v34_thumb for marker in (
            "Row(","Modifier.size(30.dp).clickable(onClick = onView)",
            "IconButton(onClick = onEdit, modifier = Modifier.size(24.dp))",
            "Modifier.size(17.dp).clickable(onClick = onRemove)"
        )), "V34 question image controls are not inline like option images")
require("resizeDeltaForEdge" in crop_geometry and
        "CropGeometry.resizeDeltaForEdge(edge, delta)" in image_editor and
        ".padding(18.dp)" in image_editor and "circular = forceSquare" in image_editor and
        "if (circular) CircleShape" in image_editor and "برش دایره‌ای پروفایل" in image_editor,
        "V34 directional crop handles or circular profile frame incomplete")
require(all(marker in student_password_vault for marker in (
            "AndroidKeyStore","AES/GCM/NoPadding","KeyGenParameterSpec.Builder(",
            "cipher.doFinal","cipher.updateAAD(entry.toByteArray","Base64.encodeToString(cipher.iv"
        )) and "passwordVault.read(student.id)" in school_screen and
        "passwordVault.write(credential.id, credential.password)" in school_screen and
        'android:allowBackup="false"' in manifest and
        school_screen.count("Modifier.weight(1f).height(64.dp)") >= 2,
        "V34 encrypted device password vault or equal password fields incomplete")

# V35 — جنسیت رنگی، نوار آیکنی رمز، clipboard دقیق، crop آزاد و کارت فشرده
_edit_v35=school_screen.split("private fun StudentEditDialog(",1)[1].split("private data class BulkStudentDraft",1)[0]
_bulk_v35=school_screen.split("private fun BulkStudentDialog(",1)[1].split("internal fun studentClipboardText",1)[0]
require(_edit_v35.count("genderFilterChipColors(Color(0xFFFF5C9A))") == 1 and
        _edit_v35.count("genderFilterChipColors(Color(0xFF3B9EFF))") == 1 and
        _bulk_v35.count("genderFilterChipColors(Color(0xFFFF5C9A))") == 1 and
        _bulk_v35.count("genderFilterChipColors(Color(0xFF3B9EFF))") == 1,
        "V35 selected gender colors are missing in edit or bulk")
require(all(marker in _edit_v35 for marker in (
            "Icons.Outlined.Close","Icons.Outlined.Check","Icons.Outlined.Visibility",
            "passwordVisible = !passwordVisible","Modifier.weight(1f).height(64.dp)",
            "textStyle = MaterialTheme.typography.titleMedium"
        )) and "trailingIcon" not in _edit_v35 and "currentPasswordVisible" not in _edit_v35,
        "V35 password fields or central eye toolbar incomplete")
_clipboard_v35=school_screen.split("internal fun studentClipboardText(",1)[1].split("private fun copyStudentInformation",1)[0]
require(all(marker in _clipboard_v35 for marker in (
            'add("نام:','add("نام خانوادگی:','add("نام پدر:','add("پایه:',
            'add("رشته:','add("نام کاربری:','add("رمز:','add("کلاس‌ها:'
        )) and all(marker not in _clipboard_v35 for marker in (
            "اطلاعات دانش‌آموز","جنسیت:","وضعیت:","شناسه حساب:"
        )), "V35 clipboard contains extra or missing student fields")
require("CropGeometry.moveCenter(" in image_editor and ".padding(18.dp)" in image_editor and
        ".pointerInput(circular)" in image_editor and "خطوط/میله‌های روی اضلاع حذف شده‌اند" in image_editor and
        "Modifier.width(34.dp).height(5.dp)" not in image_editor and
        "Modifier.width(5.dp).height(34.dp)" not in image_editor and "fun moveCenter(" in crop_geometry,
        "V35 free crop movement or invisible resize edges incomplete")
require(all(marker in student_card for marker in (
            "listOf(student.grade, student.fieldOfStudy)",'joinToString(" ")',
            "نام پدر: ${student.fatherName", "نام کاربری: ${student.username"
        )) and "رشته: ${student.fieldOfStudy" not in student_card,
        "V35 compact student card rows incomplete")

# V36 — نقش مدیر/معاون، مدرسهٔ مستقل و پوستهٔ مرحله‌ای
require("enum class UserRole { TEACHER, STUDENT, MANAGER }" in
        (ROOT/"app/src/main/java/ir/exam/app/domain/model/AppUser.kt").read_text(),
        "V36 manager role missing")
require(all(marker in sign_in_screen for marker in (
            "AuthScreen.REGISTRATION_ROLE","Text(\"معلم\")","Text(\"مدیر/معاون\")",
            "ManagerRegistrationPane","ManagerSetupPane","نام مدرسه","استان","شهر"
        )) and all(marker in auth_view_model for marker in (
            "MANAGER_REGISTER_OTP","MANAGER_REGISTER_SETUP","completeManagerRegistration"
        )), "V36 role-first manager signup incomplete")
require(v36_migration == v36_sql_copy and all(marker in v36_migration for marker in (
            "profiles_role_v36_check","public.schools","public.school_memberships",
            "ux_school_one_active_membership_v36","native_complete_manager_registration_v36",
            "native_manager_school_summary_v36","enable row level security"
        )), "V36 school tenant migration/copy incomplete")
_manager_menu_v36=app_shell.split("} else if (user.role == UserRole.MANAGER) {",1)[1].split("} else {",1)[0]
require(all(marker in _manager_menu_v36 for marker in ("\"کلاس‌ها\"","\"دانش‌آموزان\"","onClick = { select(onClasses) }","onClick = { select(onStudents) }","\"حساب\"","\"داده‌ها\"","\"تنظیمات\"","\"خروج\"")) and
        "\"تقویم\"" not in _manager_menu_v36 and "\"سربرگ\"" not in _manager_menu_v36,
        "V36 manager hamburger still exposes calendar/header")
require(all(marker in app_shell for marker in (
            "ManagerTeachersScreen","ManagerStatsScreen","UserRole.MANAGER -> WalletScreen",
            "primaryLabel = if (user.role == UserRole.MANAGER) \"معلم‌ها\"","createManagerTeacher"
        )) and "MANAGER_CARD_COUNT = 6" in
            (ROOT/"app/src/main/java/ir/exam/app/ui/app/Design69MainMenuScreen.kt").read_text() and
        "ساخت کد دعوت" in manager_foundation and "مبلغ باید مضرب ۱٬۰۰۰ تومان باشد" in manager_foundation,
        "V36 manager dock/stats/staged foundation incomplete")

# V37 — دعوت امن و مدیریت عضویت معلم
require(v37_migration == v37_sql_copy and all(marker in v37_migration for marker in (
            "school_teacher_invites","digest(v_token,'sha256')","expires_at>now()",
            "native_complete_teacher_registration_v37","native_manager_teachers_v37",
            "native_manager_disable_teacher_v37","school_students","school_admin_audit_v37"
        )), "V37 invitation/membership SQL or copy incomplete")
require("کد دعوت مدرسه (اختیاری)" in sign_in_screen and
        "teacherInviteCode" in auth_view_model and "completeInvitedTeacherRegistration" in auth_view_model and
        "native_complete_teacher_registration_v37" in
            (ROOT/"app/src/main/java/ir/exam/app/data/repository/SupabaseAuthRepository.kt").read_text(),
        "V37 invited teacher registration flow incomplete")
require(all(marker in manager_foundation for marker in (
            "ساخت کد دعوت","زمان باقی‌مانده:","حذف معلم از مدرسه"
        )) and all(marker in manager_repository for marker in (
            "native_manager_teachers_v37","native_manager_create_teacher_invites_v40b",
            "native_manager_remove_teacher_v40b"
        )) and "deleteUser" not in manager_repository,
        "V37 manager teacher UI/repository incomplete or deletes Auth")
require((ROOT/"supabase/functions/manage-student/index.ts").read_text().count("native_attach_created_student_v37") == 2,
        "V37 newly created students are not attached to their school")

# V38 — کیف پول مدیر و آمار کامل
require("TRANSFER_STEP_TOMAN = 1_000L" in
        (ROOT/"app/src/main/java/ir/exam/app/domain/model/BillingModels.kt").read_text() and
        "ManagerWalletRules.validateTransfer" in manager_repository and
        "ManagerWalletRules.isValidTransfer(amount)" in manager_foundation,
        "V38 1000-toman transfer rule incomplete")
require(v38_migration == v38_sql_copy and all(marker in v38_migration for marker in (
            "native_scope_new_school_row_v38","trg_scope_new_class_v38","trg_scope_new_exam_v38",
            "manager_wallet_transfers_v38","p_amount_toman%1000<>0","for update",
            "balance=balance-p_amount_toman","balance=balance+p_amount_toman",
            "school_transfer_to_teacher","school_transfer_from_manager","already_applied"
        )), "V38 atomic/idempotent wallet SQL or copy incomplete")
require("role in('teacher','manager')" in v38_migration and
        "UserRole.MANAGER -> WalletScreen" in app_shell and "ManagerWalletFoundationScreen" not in app_shell,
        "V38 manager secure top-up path incomplete")
require("native_manager_transfer_wallet_v38" in manager_repository and
        all(marker in manager_foundation for marker in (
            "مبلغ باید مضرب ۱٬۰۰۰ تومان باشد","Icons.Outlined.AccountBalanceWallet","شارژ کیف پول","پاسخ‌ها",
            "میانگین نمره","مجموع اعتبار توزیع‌شده","فعالیت معلم‌ها"
        )) and all(marker in v38_migration for marker in (
            "average_percent","distributed_toman","teacher_activity","wallet_balance"
        )), "V38 transfer UI or complete manager statistics missing")

# V38.1 — profile موقت teacher در ثبت‌نام مدیر
require(v381_migration == v381_sql_copy and all(marker in v381_migration for marker in (
            "v_profile.role not in ('student','teacher','manager')","v_profile.role = 'teacher'",
            "from public.classes c where c.teacher_id = v_uid","from public.exams e where e.teacher_id = v_uid",
            "s.teacher_id = v_uid and s.role = 'student'","sm.user_id = v_uid and sm.status = 'active'",
            "این ایمیل قبلاً حساب معلم فعال دارد"
        )), "V38.1 safe provisional-teacher manager conversion hotfix incomplete")

# V38.2 — digest schema/type + redaction خطای manager
require(v382_migration == v382_sql_copy and
        "extensions.digest(convert_to(v_token,'UTF8'),'sha256')" in v382_migration and
        "extensions.digest(convert_to(btrim(coalesce(p_invite_code,'')),'UTF8'),'sha256')" in v382_migration and
        "encode(digest(" not in v382_migration,
        "V38.2 schema-qualified bytea invite digest incomplete")
require(all(marker in manager_foundation for marker in (
            'substringBefore("URL:")','substringBefore("Headers:")',
            'Regex("(?i)authorization','Regex("(?i)apikey','Regex("(?i)bearer'
        )), "V38.2 manager error redaction missing")

require(r'Regex("(?i)bearer\\s+' in manager_foundation, "V38.3 Bearer regex escape is not Kotlin-safe")

# V39 — کد کوتاه مدرسه، quick-add نقش‌محور و کارت آزمون دانش‌آموز
require(v39_migration == v39_sql_copy and all(marker in v39_migration for marker in (
            "alter column email drop not null","1,6","interval '24 hours'","used_at is null",
            "native_manager_create_teacher_invite_v39","native_school_invite_preview_v39",
            "native_join_school_v39","school_invite_attempts_v39",">=10"
        )), "V39 short no-email invite SQL/copy incomplete")
require(all(marker in profile_settings for marker in (
            'title = "پیوستن به مدرسه"',"Icons.Outlined.Search","schoolJoinRepository.preview","تأیید و پیوستن"
        )) and "native_school_invite_preview_v39" in school_join_repository and
        "native_join_school_v39" in school_join_repository,
        "V39 account join-school preview/confirm incomplete")
require("primaryTitle: String = \"آزمون جدید\"" in design69_add and
        "primaryTitle = if (user.role == UserRole.MANAGER) \"دعوت معلم\" else \"آزمون جدید\"" in app_shell and
        "quickAddOpen && user.role != UserRole.STUDENT" in app_shell and
        "teacher?.role !== 'teacher' && teacher?.role !== 'manager'" in
            (ROOT/"supabase/functions/manage-student/index.ts").read_text(),
        "V39 manager/teacher quick-add actions or manager create permission incomplete")
require("\"آزمون\", \"ورود با کد آزمون\"" in app_shell and "studentJoinRequestKey += 1" in app_shell and
        "initialJoinCode" in (ROOT/"app/src/main/java/ir/exam/app/ui/student/StudentHomeScreen.kt").read_text(),
        "V39 student exam card/dialog/preview flow incomplete")

require("import androidx.compose.material3.IconButton" in profile_settings and "schoolJoinRepository.preview" in profile_settings, "V39.1 school join IconButton import missing")

require("school_id,email,token_hash,created_by,expires_at" in v39_migration and "values(v_school,null,v_hash,v_uid" in v39_migration, "V39.2 short invite null-email SQL contract missing")

# V40A — پروفایل معلم و ساده‌سازی منوی دانش‌آموز
require(v40a_migration == v40a_sql_copy and all(marker in v40a_migration for marker in (
            "employee_code","phone","native_my_teacher_details_v40","native_save_teacher_details_v40",
            "id=auth.uid() and role='teacher'","^09[0-9]{9}$"
        )), "V40A teacher details SQL/copy incomplete")
require(all(marker in profile_settings for marker in (
            "نام نمایشی","مشخصات معلم","کد پرسنلی","شماره تلفن",
            "Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)"
        )) and all(marker in
            (ROOT/"app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsViewModel.kt").read_text()
            for marker in ("setFirstName","setLastName","setEmployeeCode","setPhone")) and
        "native_save_teacher_details_v40" in
            (ROOT/"app/src/main/java/ir/exam/app/data/repository/SupabaseProfileRepository.kt").read_text(),
        "V40A teacher profile cards/save path incomplete")
_student_menu_v40a=app_shell.split("// ترتیب دقیق دانش‌آموز:",1)[1].split("Neumorphic69Provider",1)[0]
require(all(_student_menu_v40a.index(a) < _student_menu_v40a.index(b) for a,b in zip(
            ("\"آزمون\"","\"نتایج من\"","\"تقویم\"","\"حساب\"","\"تنظیمات\""),
            ("\"نتایج من\"","\"تقویم\"","\"حساب\"","\"تنظیمات\"","\"خروج\"")
        )) and "\"داده‌ها\"" not in _student_menu_v40a,
        "V40A student menu order or data-card removal incomplete")
require("if (role != UserRole.STUDENT)" in profile_settings and
        "حساب مدیریت‌شده توسط معلم" not in profile_settings and
        "horizontalScroll(rememberScrollState())" not in profile_settings,
        "V40A student credential cards/email or centered settings incomplete")

# V40B — کارت معلم و دعوت دسته‌ای
require(v40b_migration == v40b_sql_copy and all(marker in v40b_migration for marker in (
            "p_count not between 1 and 5","for i in 1..p_count","display_code","interval '24 hours'",
            "native_manager_invites_v40b","native_manager_revoke_invite_v40b",
            "native_manager_set_teacher_active_v40b","native_manager_remove_teacher_v40b"
        )), "V40B batch invite/teacher status SQL or copy incomplete")
require(all(marker in manager_foundation for marker in (
            "کد پرسنلی:","شماره تلفن:","expandedTeacher == teacher.id","Icons.Outlined.ToggleOn",
            "Icons.Outlined.Login","Icons.Outlined.AccountBalanceWallet","Icons.Outlined.Delete",
            "if (inviteMode)","(1..5).forEach","زمان باقی‌مانده:","حذف کد دعوت"
        )), "V40B teacher card icons or invite-only mode incomplete")
require(all(marker in manager_repository for marker in (
            "createInvites(count: Int)","native_manager_create_teacher_invites_v40b",
            "native_manager_invites_v40b","native_manager_revoke_invite_v40b",
            "native_manager_set_teacher_active_v40b","native_manager_remove_teacher_v40b"
        )) and "deleteUser" not in manager_repository,
        "V40B manager repository incomplete or deletes Auth")

# V40C — مدیریت کلاس/دانش‌آموز و جداسازی حذف عضویت از حذف حساب
require(v40c_migration == v40c_sql_copy and all(marker in v40c_migration for marker in (
            "can_manage boolean","public.school_students","native_manager_teacher_classes_v40c",
            "native_manager_save_teacher_class_v40c","native_manager_delete_teacher_class_v40c",
            "native_manager_class_roster_v40c","native_manager_school_students_v40c",
            "native_manager_set_class_student_v40c"
        )), "V40C manager class/student SQL or copy incomplete")
require(all(marker in manager_class_screen for marker in (
            "کلاس جدید برای معلم","حذف کلاس","دانش‌آموزان کلاس",
            "افزودن از فهرست دانش‌آموزان مدرسه","حذف از کلاس"
        )) and "ManagerTeacherClassScreen" in app_shell,
        "V40C manager teacher context UI incomplete")
require("onDelete = { viewModel.removeStudent(it.id) }" in school_screen and
        "membershipOnlyDelete = true" in school_screen and "حذف از کلاس" in school_screen and
        "حذف حساب دانش‌آموز" in school_screen and "student.canManageAccount" in school_screen,
        "V40C roster membership-delete/account-delete separation incomplete")
require("teacher?.role === 'manager'" in (ROOT/"supabase/functions/manage-student/index.ts").read_text() and
        "school_memberships" in (ROOT/"supabase/functions/manage-student/index.ts").read_text() and
        "school_students" in (ROOT/"supabase/functions/manage-student/index.ts").read_text(),
        "V40C manager student mutation is not school scoped")

require(all(marker in manager_foundation for marker in ("delay(1_000)","clockNow","invites = invites.filterNot","FilterChipDefaults.filterChipColors","Modifier.size(34.dp)")) and "managerInviteHeader" in app_shell, "V41A teacher/invite polish incomplete")

# V41B — server-enforced manager approval for teacher-owned data
v41_sql=(ROOT/'supabase/migrations/20260816_native_manager_teacher_approval_v41.sql').read_text()
v41_copy=(ROOT/'sql/manual/SQL_NATIVE_MANAGER_TEACHER_APPROVAL_V41.sql').read_text()
v41_edge=(ROOT/'supabase/functions/manage-student/index.ts').read_text()
v41_dashboard=(ROOT/'app/src/main/java/ir/exam/app/ui/dashboard/TeacherManagerRequestsScreen.kt').read_text()
require(v41_sql==v41_copy and all(x in v41_sql for x in ('manager_approval_requests',"interval '24 hours'",'native_teacher_decide_manager_request_v41','executed_at','security definer')), 'V41 approval SQL/copy incomplete')
require('manager_approval_requests' in v41_edge and 'current.teacher_id !== teacherId' in v41_edge and 'approval_id: approvalId' in v41_edge, 'V41 student Edge approval enforcement incomplete')
require('درخواست‌های مدیر' in v41_dashboard and 'decide(request.id, true)' in v41_dashboard and 'decide(request.id, false)' in v41_dashboard, 'V41 teacher approval inbox incomplete')

# V41B.1 — profile/invite grants, live countdown, requests-only card destination
v41b1_sql=(ROOT/'supabase/migrations/20260816_native_profile_grant_invite_requests_v41b1.sql').read_text()
v41b1_copy=(ROOT/'sql/manual/SQL_NATIVE_PROFILE_GRANT_INVITE_REQUESTS_V41B1.sql').read_text()
v41b1_requests=(ROOT/'app/src/main/java/ir/exam/app/ui/dashboard/TeacherManagerRequestsScreen.kt').read_text()
require(v41b1_sql==v41b1_copy and 'grant execute on function public.native_my_profile() to authenticated' in v41b1_sql and 'native_manager_revoke_invite_v40b(uuid)' in v41b1_sql, 'V41B.1 RPC grants incomplete')
require('%02d:%02d:%02d' in manager_foundation and manager_foundation.index('invites = invites.filterNot') < manager_foundation.index('repository.revokeInvite'), 'V41B.1 invite countdown/optimistic deletion incomplete')
require('درخواست‌های مدیر' in v41b1_requests and '"درخواست‌ها"' in design69_cards and 'MainPage.REQUESTS' in app_shell, 'V41B.1 requests card destination incomplete')

# V42.2 — manager teacher dock must close invite/teacher contexts
require(all(marker in app_shell for marker in ('managerTeacherListKey += 1','managerTeacherId = null','teacherListRequested = managerTeacherListKey','inviteModeRequested = managerInviteHeader')) and all(marker in manager_foundation for marker in ('teacherListRequested: Int','inviteModeRequested: Boolean','LaunchedEffect(inviteModeRequested, newTeacherRequested, teacherListRequested)','inviteMode = inviteModeRequested','if (inviteModeRequested) reloadInvites() else reloadTeachers()')), 'V42.2/V42.4 manager teacher/invite routing is not deterministic')

# V43 — explicit teacher/student list links without ownership transfer
v43_sql=(ROOT/'supabase/migrations/20260816_native_teacher_student_links_v43.sql').read_text()
v43_copy=(ROOT/'sql/manual/SQL_NATIVE_TEACHER_STUDENT_LINKS_V43.sql').read_text()
v43_school=(ROOT/'app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt').read_text()
require(v43_sql==v43_copy and all(x in v43_sql for x in ('teacher_student_links','native_teacher_add_class_student_to_list_v43','in_my_list boolean','add_students_to_class')) and 'update profiles set teacher_id' not in v43_sql, 'V43 teacher student-link SQL incomplete or transfers ownership')
require('!student.inMyList' in v43_school and 'افزودن به لیست دانش‌آموزان من' in v43_school and 'onAddToMyList(student.id)' in v43_school, 'V43 roster add-to-my-list UI incomplete')

if errors:
    print("FINAL_NATIVE_VERIFY=FAIL")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print(f"FINAL_NATIVE_VERIFY=PASS kotlin_files={len(main_files)} edge_functions={len(edge_files)}")
