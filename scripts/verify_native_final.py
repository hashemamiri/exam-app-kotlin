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
formula_editor=(ROOT/"app/src/main/java/ir/exam/app/ui/math/FormulaEditorDialog.kt").read_text()
inline_math_editor=(ROOT/"app/src/main/java/ir/exam/app/ui/math/InlineMathTextEditor.kt").read_text()
rich_text=(ROOT/"app/src/main/java/ir/exam/app/core/text/RichText.kt").read_text()
figure_picker=(ROOT/"app/src/main/java/ir/exam/app/ui/figure/FigurePickerDialog.kt").read_text()
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

webview_files = {
    f.name
    for f in (ROOT / "app/src/main/java").rglob("*.kt")
    if "android.webkit" in f.read_text(errors="ignore")
}
# Hybrid migration: WebView is allowed only inside the local formula editor adapter
# and (V53.1, user-approved) the local question text field surface.
approved_webview_files = {
    "FormulaEditorDialog.kt",
    "QuestionEditorWebView.kt",
    "QuestionEditorWebViewDialog.kt",
    "QuestionTextFieldWebView.kt",
    "FormulaHostDialog.kt",
}
require(
    webview_files <= approved_webview_files,
    "WebView/android.webkit import remains outside the approved formula editor"
)
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
# V58.0 — دکمهٔ متنی «علامت برای مرور» عمداً حذف شد؛ نگه‌داشتن ۲ ثانیه‌ای
# شمارهٔ سؤال جایگزین است (onLongClick → onToggleFlag).
require("مرور پیش از ارسال" in student_screen and "onLongClick = { onToggleFlag(q.id) }" in student_screen,"student navigation/review parity missing")
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
# V61.5 — عمل چهارم «مدرسه جدید» اضافه شد (ACTION_COUNT = 4).
require(all(marker in design69_add for marker in ("OPEN_ROTATION_DEGREES = 135","ACTION_COUNT = 4","دانش‌آموز جدید","آزمون جدید","کلاس جدید","مدرسه جدید","travel.animateTo")),
        "shared moving plus or four real quick actions incomplete")
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
# V61.5 — یکی برای فیلتر دانش‌آموزان (school 5) و یکی برای نام مدرسهٔ سربرگ
# (profile 2) به چرخ مشترک اضافه شد.
require(school_screen.count("GradeOdometerPicker(") == 5 and
        profile_settings.count("GradeOdometerPicker(") == 2 and
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
# V58.0.2 — هدر با منوی باز «و» هنگام آزمون فعال دانش‌آموز پنهان می‌شود.
require("if (!menuOpen && !(user.role == UserRole.STUDENT && studentExamActive))" in app_shell,
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
            "persianOptionLetter(index)","OptionInsertButton(","SingleImagePicker(","ReorderDragButton("
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
require(formula_library.exists() and formula_library.stat().st_size > 100_000,"complete formula reference asset missing")
formula_markers=("⭐ موارد پرکاربرد","🔢 اعداد و محاسبات","∫ آنالیز و توابع","𝑥 جبر و معادلات","∿ مثلثات و یونانی","⊆ مجموعه و منطق","📐 هندسه و بردار","🚀 فیزیک","🧪 شیمی","🔍 همهٔ نمادها","⚙ یونیکد (۱۲۰۰)","🕘 اخیر","✨ تبدیل","FixedFormulaKeypad")
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
require("usePlatformDefaultWidth = false" in formula_library_dialog and
        "LazyVerticalGrid" in formula_library_dialog and
        "awaitEachGesture" in formula_library_dialog and
        "withTimeoutOrNull(2000L)" in formula_library_dialog and
        "onToggleFavorite(entry)" in formula_library_dialog,
        "full-screen long-press formula library dialog missing")
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
require(all(marker in formula_editor for marker in ("ماتریس دلخواه ۱ تا ۱۰","onPreviewKeyEvent","نمادهای اخیر")),
        "complete formula editor controls are not reachable")
require("moveSpatialBox" in formula_boxes and "typeCharacter" in formula_boxes and "importText" in formula_boxes,
        "spatial navigation structural typing or safe paste missing")
require("animateScrollTo" in formula_view and "verticalScroll" in formula_view,
        "active formula box auto-scroll missing")
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
# V55.16 — آیکن فرمول گزینه/جورکردنی با دکمهٔ + (OptionInsertButton) جایگزین شد.
require(_v34_multiple.index("OptionInsertButton(") < _v34_multiple.index("ReorderDragButton(") <
        _v34_multiple.index("SingleImagePicker("),
        "V34/V55.16 multiple-choice reorder is not immediately after the insert button")
_v34_matching=matching_builder.split("private fun MatchingItemTools(",1)[1].split("fun MatchingQuestionEditor(",1)[0]
require(_v34_matching.index("OptionInsertButton(") < _v34_matching.index("ReorderDragButton(") <
        _v34_matching.index("SingleImagePicker("),
        "V34/V55.16 matching reorder is not immediately after the insert button")
_v34_thumb=question_media.split("private fun CompactImageThumbnail(",1)[1]
require(all(marker in _v34_thumb for marker in (
            "Row(","Modifier.size(30.dp).clickable(onClick = onView)",
            "IconButton(onClick = onEdit, modifier = Modifier.size(24.dp))",
            "Modifier.size(17.dp).clickable(onClick = onRemove)"
        )), "V34 question image controls are not inline like option images")
# V55.14 — دستگیره‌های مرئی ضلع+گوشه جایگزین نوار نامرئی ۱۸dp شد.
require("resizeDeltaForEdge" in crop_geometry and
        "resizeDeltaForCorner" in crop_geometry and
        "CropGeometry.resizeDeltaForEdge(edge, dx)" in image_editor and
        "CropGeometry.resizeDeltaForCorner(edge, dx, dy)" in image_editor and
        "circular = forceSquare" in image_editor and
        "if (circular) CircleShape" in image_editor and "برش دایره‌ای پروفایل" in image_editor,
        "V34/V55.14 directional crop handles or circular profile frame incomplete")
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
# V55.14 — به درخواست کاربر، دستگیره‌های «مرئی» ضلع+گوشه برگشتند (نامرئی قابل‌کشف نبود).
require("CropGeometry.moveCenter(" in image_editor and
        ".pointerInput(circular)" in image_editor and
        "CropHandle(CropEdgeKind.TOP_LEFT" in image_editor and
        "fun moveCenter(" in crop_geometry,
        "V35/V55.14 free crop movement or visible resize handles incomplete")
require(all(marker in student_card for marker in (
            "listOf(student.grade, student.fieldOfStudy)",'joinToString(" ")',
            "نام پدر: ${student.fatherName", "نام کاربری: ${student.username"
        )) and "رشته: ${student.fieldOfStudy" not in student_card,
        "V35 compact student card rows incomplete")

# V36 — نقش مدیر/معاون، مدرسهٔ مستقل و پوستهٔ مرحله‌ای
require("enum class UserRole { TEACHER, STUDENT, MANAGER }" in
        (ROOT/"app/src/main/java/ir/exam/app/domain/model/AppUser.kt").read_text(),
        "V36 manager role missing")
# V62.1 — نقش‌های ثبت‌نام تب سگمنتی ماژول یخی شدند (معلم اول).
require(all(marker in sign_in_screen for marker in (
            "AuthScreen.REGISTRATION_ROLE","labels = listOf(\"معلم\", \"مدیر/معاون\")",
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
            # V62.6 — امضا مدرسهٔ مقصد اختیاری گرفت؛ بدون مدرسه همان مسیر V40B.
            "createInvites(count: Int, schoolId: String? = null)","native_manager_create_teacher_invites_v40b",
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
# V61.8 — حذف سروری از revokeInvite به deleteInvite تغییر کرد (حذف واقعی سطر).
require('%02d:%02d:%02d' in manager_foundation and manager_foundation.index('invites = invites.filterNot') < manager_foundation.index('repository.deleteInvite'), 'V41B.1 invite countdown/optimistic deletion incomplete')
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


# ---- V53.1: WebView question text field + native tool icons + native table editor ----
web_section=(ROOT/"app/src/main/java/ir/exam/app/ui/builder/QuestionTextWebSection.kt").read_text()
web_field=(ROOT/"app/src/main/java/ir/exam/app/ui/math/QuestionTextFieldWebView.kt").read_text()
tool_icons=(ROOT/"app/src/main/java/ir/exam/app/ui/math/QuestionToolIcons.kt").read_text()
table_editor=(ROOT/"app/src/main/java/ir/exam/app/ui/figure/TableEditorDialog.kt").read_text()
table_renderer=(ROOT/"app/src/main/java/ir/exam/app/core/figure/TableSvgRenderer.kt").read_text()
figure_renderer=(ROOT/"app/src/main/java/ir/exam/app/core/figure/FigureSvgRenderer.kt").read_text()
editor_asset=(ROOT/"app/src/main/assets/question_editor/question_editor.html").read_text(errors="ignore")
require("QuestionTextWebSection(" in builder_screen and "InlineMathTextEditor(" not in builder_screen.split("import ",1)[1],
        "V53.1 question card does not use the WebView text field")
for _lbl in ["درج فرمول","درج شکل","درج نمودار","درج جدول","درج آناتومی بدن","درج جدول تناوبی","درج فیزیک","درج شیمی"]:
    require(_lbl in web_section, f"V53.1 native toolbar is missing: {_lbl}")
require("QuestionToolIcons" in web_section and "ImageVector" in tool_icons,
        "V53.1 toolbar icons are not native ImageVectors")
require("nativeTools=1" in web_field and "nativeToolbarHide" in editor_asset and "exam-editor-native-tools" in editor_asset,
        "V53.1 HTML toolbar hiding bridge is missing")
require("if (spec.isTable) return TableSvgRenderer.render(spec)" in figure_renderer,
        "V53.1 table tokens do not render through the shared SVG path")
require("android.webkit" not in table_editor and "buildTable" in (ROOT/"app/src/main/java/ir/exam/app/core/figure/FigureSpec.kt").read_text(),
        "V53.1 table editor is not fully native")
require("figureBitmap" in (ROOT/"app/src/main/java/ir/exam/app/core/printing/OfficialPdfPrintAdapter.kt").read_text(),
        "V53.1 PDF path does not rasterize %%FIG%% tokens")


# ---- V53.2: native periodic table (data + renderer + editor + wiring) ----
periodic_data=(ROOT/"app/src/main/java/ir/exam/app/core/figure/PeriodicElements.kt").read_text()
periodic_renderer=(ROOT/"app/src/main/java/ir/exam/app/core/figure/PeriodicSvgRenderer.kt").read_text()
periodic_editor=(ROOT/"app/src/main/java/ir/exam/app/ui/figure/PeriodicEditorDialog.kt").read_text()
require(len(re.findall(r"E\(\d+, ", periodic_data)) == 118,
        "V53.2 periodic data must contain exactly 118 elements")
require('if (spec.kind == "p") return PeriodicSvgRenderer.render(spec)' in figure_renderer,
        "V53.2 periodic tokens do not render through the shared SVG path")
require("android.webkit" not in periodic_editor and "WebView" not in periodic_editor,
        "V53.2 periodic editor must be fully native")
for _lbl in ["کامل","گروه اصلی","بدون f","بدون عدد اتمی","حذف عنصر","حذف عدد اتمی","بازگردانی همه"]:
    require(_lbl in periodic_editor, f"V53.2 periodic editor is missing: {_lbl}")
require('openTool("periodic")' not in web_section and "onInsertPeriodic" in web_section,
        "V53.2 periodic icon must open the native editor")
require("<script" not in periodic_renderer and "href=" not in periodic_renderer
        and "<foreignObject" not in periodic_renderer,
        "V53.2 periodic SVG renderer contains unsafe markup")


# ---- V53.3: native anatomy + physics/chemistry atlas + dblclick native editing ----
atlas_catalog=(ROOT/"app/src/main/java/ir/exam/app/core/figure/AtlasCatalog.kt").read_text()
atlas_view=(ROOT/"app/src/main/java/ir/exam/app/ui/figure/AtlasFigureView.kt").read_text()
atlas_editor=(ROOT/"app/src/main/java/ir/exam/app/ui/figure/AtlasEditorDialog.kt").read_text()
atlas_bitmap=(ROOT/"app/src/main/java/ir/exam/app/core/figure/AtlasBitmapRenderer.kt").read_text()
require(len(re.findall(r'AtlasType\("[A-Za-z0-9_]+", "[a-z0-9]+", "[^"]+", "[^"]*"\),', atlas_catalog)) == 67,
        "V53.3 anatomy catalog must contain exactly 67 types")
require(len(re.findall(r'AtlasType\("[A-Za-z0-9_]+", "[a-z0-9]+", "[^"]+"\),', atlas_catalog)) == 70,
        "V53.3 science catalog must contain exactly 70 types")
require(len(list((ROOT/"app/src/main/assets/figure_atlas/anatomy").glob("*"))) == 67,
        "V53.3 anatomy atlas assets incomplete")
require(len(list((ROOT/"app/src/main/assets/figure_atlas/science").glob("*"))) == 70,
        "V53.3 science atlas assets incomplete")
require("android.webkit" not in atlas_view and "android.webkit" not in atlas_editor
        and "android.webkit" not in atlas_bitmap,
        "V53.3 atlas view/editor/renderer must be fully native")
require("AtlasFigureView(" in (ROOT/"app/src/main/java/ir/exam/app/ui/math/NativeMathText.kt").read_text(),
        "V53.3 student view does not render atlas figures natively")
require("AtlasBitmapRenderer.render(context, spec)" in (ROOT/"app/src/main/java/ir/exam/app/core/printing/OfficialPdfPrintAdapter.kt").read_text(),
        "V53.3 PDF path does not render atlas figures natively")
for _bad in ['openTool("anatomy")','openTool("physics")','openTool("chemistry")','openTool("periodic")']:
    require(_bad not in web_section, f"V53.3 webview tool still wired: {_bad}")
require("onEditFigure" in editor_asset and "applyEditedToken" in editor_asset,
        "V53.3 dblclick native-edit bridge missing from asset")
require("marks.size < 12" in atlas_editor and "nextMarkNumber" in atlas_editor,
        "V53.3 atlas editor mark limits do not match the reference")


# ---- V53.4: full-screen WebView formula window + single native frame ----
formula_host=(ROOT/"app/src/main/java/ir/exam/app/ui/math/FormulaHostDialog.kt").read_text()
require("formula-editor/formula.html" in formula_host and "usePlatformDefaultWidth = false" in formula_host,
        "V55 formula host dialog is not full-screen or does not load the standalone formula asset")
formula_asset=(ROOT/"app/src/main/assets/formula_editor/formula.html").read_text(errors="ignore")
require("onOpenFormula" in editor_asset and "ExamFormulaHost" in formula_asset
        and "onEditorClosed" in formula_asset,
        "V55 formula bridges are missing (question field or standalone formula asset)")
require("FormulaHostDialog(" in builder_screen and "QuestionEditorWebViewDialog(" not in builder_screen,
        "V53.4 builder must open the full-screen formula window everywhere")
require("nativeToolbarHide" in editor_asset and ".field>span{display:none" not in editor_asset,
        "V54.4 reference frame/label must stay byte-identical; only the toolbar is hidden")


# ---- V54.1: chart library stage 1 (20 new native chart types) ----
chart_renderer=(ROOT/"app/src/main/java/ir/exam/app/core/figure/ChartSvgRenderer.kt").read_text()
figure_gallery=(ROOT/"app/src/main/java/ir/exam/app/core/figure/FigureGallery.kt").read_text()
_v54_types=["pie","donut","lchr","area","sarea","hbar","cmp","hcmp","stack","st100",
            "scat","bub","hist","pareto","gauge","radar","combo","step","lolli","funn"]
for _t in _v54_types:
    require(f'"{_t}"' in chart_renderer, f"V54.1 chart renderer missing type: {_t}")
    require(f'FigureTemplate("{_t}"' in figure_gallery, f"V54.1 gallery missing type: {_t}")
require("in ChartSvgRenderer.SUPPORTED -> ChartSvgRenderer.body(spec)" in figure_renderer,
        "V54.1 new chart types are not routed through the shared SVG path")
require("<script" not in chart_renderer and "href=" not in chart_renderer
        and "<foreignObject" not in chart_renderer,
        "V54.1 chart SVG renderer contains unsafe markup")


# ---- V54.2: chart library stage 2 (14 more native chart types) ----
chart_stage2=(ROOT/"app/src/main/java/ir/exam/app/core/figure/ChartSvgRendererStage2.kt").read_text()
_v542_types=["box","ohlc","fall","ctrl","venn","tree","sun","waff","pict","heat","hmap","bull","pyra","mekko"]
for _t in _v542_types:
    require(f'"{_t}"' in chart_stage2, f"V54.2 stage2 renderer missing type: {_t}")
    require(f'FigureTemplate("{_t}"' in figure_gallery, f"V54.2 gallery missing type: {_t}")
require("ChartSvgRendererStage2.SUPPORTED" in chart_renderer.split("val SUPPORTED",1)[1].split("fun supports",1)[0],
        "V54.2 stage2 types are not merged into the shared supported set")
require("<script" not in chart_stage2 and "href=" not in chart_stage2
        and "<foreignObject" not in chart_stage2,
        "V54.2 stage2 SVG renderer contains unsafe markup")


# ---- V54.3: chart library final stage (22 types) + full 61/61 reference coverage ----
chart_stage3=(ROOT/"app/src/main/java/ir/exam/app/core/figure/ChartSvgRendererStage3.kt").read_text()
_v543_types=["plot","flow","gantt","time","dumb","slope","spark","stream","viol","strip",
             "stem","smat","dend","sank","chrd","netw","map","bmap","surf","calh","rose","word"]
for _t in _v543_types:
    require(f'"{_t}"' in chart_stage3, f"V54.3 stage3 renderer missing type: {_t}")
    require(f'FigureTemplate("{_t}"' in figure_gallery, f"V54.3 gallery missing type: {_t}")
require("STAGE1 + ChartSvgRendererStage2.SUPPORTED + ChartSvgRendererStage3.SUPPORTED" in chart_renderer,
        "V54.3 stage3 types are not merged into the shared supported set")
_gr_module=editor_asset.split("graph-fig-js",1)[1].split("window.GraphFig",1)[0]
_gr_types=re.findall(r"\{ id: '([A-Za-z0-9_]+)', name: '", _gr_module.split("var TYPES = [",1)[1].split("];",1)[0])
_flat_gallery=re.sub(r"\s+"," ",figure_gallery)
_missing=[i for i in _gr_types
          if f'FigureTemplate( "{i}"' not in _flat_gallery
          and f'FigureTemplate("{i}"' not in _flat_gallery and i != "col"]
require(len(_gr_types) >= 60 and not _missing,
        f"V54.3 native gallery does not cover all reference graph types: {_missing}")
require("<script" not in chart_stage3 and "href=" not in chart_stage3
        and "<foreignObject" not in chart_stage3,
        "V54.3 stage3 SVG renderer contains unsafe markup")

# ---- V54.4: reference-parity question field + formula window fixes ----
require('if (request.isForMainFrame) onError("EDITOR_LOAD_FAILED")' in web_field,
        "V54.4 load error must fire for the main frame only")
require('!path.startsWith("/question-editor/")' in web_field and "emptyResponse()" in web_field,
        "V54.4 foreign paths must get safe empty responses in the field webview")
require('"متن سؤال"' not in web_section and "BorderStroke" not in web_section,
        "V54.4 compose must not draw a duplicate frame/label around the webview")
require("Icons.Outlined.Close" not in formula_host and "0xFFE9EEF5" in formula_host,
        "V54.4 formula window must be pure reference webview without a compose close button")
require("closeOverlays" in web_field and "closeOverlays: function ()" in editor_asset,
        "V54.4 back-button overlay close bridge is missing")
require("ExamEditorNative.onOpenFormula" in editor_asset and "window.ExamFormulaHost" in formula_asset,
        "V55 formula host bridges are missing from the assets")

# ---- V55: standalone formula.html as the formula window ----
require(len(formula_asset) > 500000 and "auto-open" in formula_asset,
        "V55 standalone formula asset is missing or truncated")
require("exam-formula-native-bridge" in formula_asset
        and "__aoNativeClosing" in formula_asset
        and "window.__aoNativeClosing = false" in formula_asset,
        "V55 native bridge or reopen-suppression/reset is missing from formula.html")
require('!path.startsWith("/formula-editor/")' in formula_host
        and 'assets.open("formula_editor/$assetPath")' in formula_host,
        "V55 formula dialog does not serve the formula_editor asset folder")
require("onClosed" in formula_host and "ExamFormulaHost.begin(" in formula_host,
        "V55 dialog is not wired to the standalone bridge events")

# ---- V55.3: paint fix + blank-layout diagnostic ----
require("nativePaintFix" in formula_asset
        and "backdrop-filter:none !important" in formula_asset
        and "will-change:auto !important" in formula_asset,
        "V55.3 in-app compositing paint fix is missing from formula.html")
require("FORMULA_BLANK_LAYOUT" in formula_asset and "getBoundingClientRect" in formula_asset,
        "V55.3 blank-layout diagnostic is missing from formula.html")
require('Color.parseColor("#E9EEF5")' in formula_host and "Color.TRANSPARENT" not in formula_host,
        "V55.3 formula webview must use an opaque theme background, not transparent")

# ---- V55.4: larger library windows only inside the app ----
require("nativeLibrarySize" in formula_asset
        and ".mb-var{min-width:min(340px,88vw) !important" in formula_asset
        and "width:min(96vw,720px) !important" in formula_asset,
        "V55.4 in-app library size boost is missing from formula.html")
require("min-width: 220px; max-width: min(320px, 92vw); max-height: 62vh;" in formula_asset,
        "V55.4 must not edit the reference library CSS itself (in-app override only)")

# ---- V55.5: inline-style enforcement for the category menu / library panel ----
require("nativeMenuEnforce" in formula_asset
        and "setProperty(k, map[k], 'important')" in formula_asset
        and "MENU_RECT" in formula_asset and "PANEL_RECT" in formula_asset,
        "V55.5 inline menu-size enforcement or its diagnostics are missing")
require("unsize(pop)" in formula_asset,
        "V55.5 must restore reference sizes for small variant menus after close")

# ---- V55.6: no on-screen version badges + instant open + formula fit ----
require("bt.id = 'nativeBridgeTag'" not in formula_asset
        and "__nativeBridgeVersion" in formula_asset and "hideBadges" in formula_asset,
        "V55.6 on-screen version badges must be gone (flag + in-app hide only)")
require("wrapNow('mbGroupLibrary', afterMenu)" in formula_asset
        and "fitLibraryItems" in formula_asset,
        "V55.6 instant library enforcement or formula fit is missing")

# ---- V55.7/V55.8: question field reports height; fixed cap with inner scroll ----
require("ExamEditorNative.onContentHeight" in editor_asset
        and "max-height:260px !important" in editor_asset
        and "overflow-y:auto !important" in editor_asset
        and "html,body{background:transparent !important;}" in editor_asset,
        "V55.8 fixed-cap inner-scroll field or transparent background is missing")
require("fun onContentHeight(height: Int)" in web_field
        and "contentHeightDp.coerceIn(150, 4000).dp" in web_section,
        "V55.7 dynamic field height wiring is missing from Kotlin")

# ---- V55.8: boot curtain, 60% inserted previews, keypad menus enlarged ----
require("exam-editor-native-boot" in editor_asset and "nativeBootHide" in editor_asset,
        "V55.8 boot curtain against reference-page flash is missing")
require(".qmf-surface.input .qmf-fig{zoom:.6" in editor_asset
        and ".qmf-surface.input .qmf-atom{zoom:.75" in editor_asset,
        "V55.8 inserted figure/formula shrink is missing")
require("wrapNow('mbVarShow', afterMenu)" in formula_asset
        and "wrapNow('mbParPicker', afterMenu)" in formula_asset
        and "'.mbv-cat, .mbv-i, .mbv-q'" in formula_asset,
        "V55.8 keypad variant/paren menu enlargement is missing")

# ---- V55.9: single-click capture for native tokens + per-question webview ----
require("__nativeFigEdit" in editor_asset and "stopImmediatePropagation" in editor_asset,
        "V55.9 click capture for native token kinds is missing")
require("key(controller)" in web_field,
        "V55.9 per-question webview isolation (key) is missing")

# ---- V55.10: select-then-edit tokens with delete button + inner scroll unlock ----
require("native-fig-x" in editor_asset and "removeToken(fig)" in editor_asset
        and "__nativeAllow" in editor_asset,
        "V55.10 token select/delete behaviour is missing")
require("onScrollableChanged" in editor_asset
        and "requestDisallowInterceptTouchEvent(true)" in web_field,
        "V55.10 inner-scroll gesture unlock is missing")

# ---- V55.11: unclipped delete button (incl. formulas) + caret fix ----
require("function attachX(el2)" in editor_asset
        and "'.qmf-atom.is-on, .qmf-fig.is-on'" in editor_asset,
        "V55.11 unified delete button for figures and formulas is missing")
require("caretRangeFromPoint" in editor_asset,
        "V55.11 surface caret/focus fix is missing")

# ---- V55.12: two-stage atlas flow, arrow-end numbers, no label boxes, 2-col graphs ----
require("fun AtlasTypePickerDialog(" in atlas_editor
        and "presetType: String? = null" in atlas_editor
        and "LazyRow" not in atlas_editor,
        "V55.12 atlas type-first flow or editor type-selection removal is missing")
require('AtlasTarget(kind = "a", chooseType = true)' in builder_screen
        and "AtlasTypePickerDialog(" in builder_screen,
        "V55.12 atlas two-stage wiring is missing from the builder")
require("drawCircle(color, radius, end)" in atlas_editor
        and "canvas.drawCircle(x2, y2, radius, fillAccent)" in
            (ROOT/"app/src/main/java/ir/exam/app/core/figure/AtlasBitmapRenderer.kt").read_text(),
        "V55.12 mark number must sit at the arrow end in every renderer")
require("برچسب/پاسخ نشانه" not in atlas_editor,
        "V55.12 label boxes must be gone from the atlas editor")
require("GridCells.Fixed(2)" in figure_picker
        and "برای ویرایش انتخاب کنید" not in figure_picker,
        "V55.12 two-column graph picker without the edit hint is missing")

# ---- V55.13: geometry/graph tokens edit natively + LTR periodic grid ----
require("kind !== 'g'" in editor_asset
        and '"g", "" -> figureTarget = FigureTarget(' in builder_screen,
        "V55.13 geometry/graph tokens must route to the native editor")
require("CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr)" in
            (ROOT/"app/src/main/java/ir/exam/app/ui/figure/PeriodicEditorDialog.kt").read_text(),
        "V55.13 periodic touch grid must be forced LTR")

# ---- V55.14: trash delete + cuboid/box split + visible crop handles ----
require("Icons.Outlined.Delete" in builder_screen and "برای همیشه حذف شود؟" in builder_screen,
        "V55.14 trash-icon question delete with confirmation is missing")
require('FigureTemplate("cuboid", "مکعب‌مستطیل"' in
            (ROOT/"app/src/main/java/ir/exam/app/core/figure/FigureGallery.kt").read_text()
        and "if (t === 'cuboid') t = 'box';" in editor_asset,
        "V55.14 cuboid/box identifier split is missing")

# ---- V55.15: fresh crop callbacks + graph tokens tagged k='g' ----
require("rememberUpdatedState(onMove)" in image_editor
        and "rememberUpdatedState(onResize)" in image_editor,
        "V55.15 crop stale-lambda fix is missing")
require('root["k"] = JsonPrimitive("g")' in figure_picker,
        "V55.15 graph specs must carry k='g' for the reference chart module")

# ---- V55.16: option/matching boxes like the question box + plus tool menu ----
_option_tools=(ROOT/"app/src/main/java/ir/exam/app/ui/builder/OptionInsertTools.kt").read_text()
require("fun OptionInsertButton(" in _option_tools
        and "fun OptionInsertToolsDialog(" in _option_tools
        and "Icons.Outlined.Add" in _option_tools,
        "V55.16 plus button / eight-tool dialog is missing")
require("Icons.Outlined.Functions" not in builder_screen
        and "Icons.Outlined.Functions" not in matching_builder,
        "V55.16 old option formula icon must be replaced by the plus button")
require("fun appendTokenToField(ref: InsertMenuRef, spec: FigureSpec)" in builder_screen
        and 'shape = RoundedCornerShape(14.dp)' in builder_screen
        and 'shape = RoundedCornerShape(14.dp)' in matching_builder,
        "V55.16 field-token delivery or question-box-like styling is missing")

# ---- V55.17: bank icon, chip display for FIG tokens, LTR crop canvas ----
require("Icons.Outlined.BookmarkAdd" in builder_screen
        and "OutlinedButton(onClick = { viewModel.saveToBank" not in builder_screen,
        "V55.17 bank-save icon next to the trash icon is missing")
require("FigTokenVisuals.transformation" in builder_screen
        and matching_builder.count("FigTokenVisuals.transformation") == 2,
        "V55.17 chip display for FIG tokens is missing from option/matching fields")
require("LocalLayoutDirection provides LayoutDirection.Ltr" in image_editor,
        "V55.17 crop canvas must be forced LTR so dragging follows the finger")

# ---- V55.18: smooth right swipe, eye preview menu, compact card header ----
_mgmt_cards=(ROOT/"app/src/main/java/ir/exam/app/ui/app/TeacherManagementCardsScreen.kt").read_text()
require("if (direction == -1) {" in _mgmt_cards
        and "dragX.animateTo(0f, tween(300, easing = FastOutSlowInEasing))" in _mgmt_cards,
        "V55.18 smooth rightward card entrance is missing")
require("returningIndex = activeIndex" in _mgmt_cards
        and "returnX.animateTo(0f, tween(300, easing = FastOutSlowInEasing))" in _mgmt_cards
        and "returning -> returnX.value" in _mgmt_cards,
        "V55.18.1 single-phase rightward swipe with returning-card animation is missing")
# V62.7 — منوی چشم حذف شد؛ چشم فقط پیش‌نمایش دانش‌آموزی سؤال را باز می‌کند و
# «پیش‌نمایش چاپ این سؤال» داخل چیدمان چاپ کارت ماند.
require("پیش‌نمایش کامل A4" in builder_screen and "onStudentPreview" in builder_screen
        and "onPreviewAll = { previewAll = true }" in builder_screen,
        "V55.18 eye preview menu (question + full A4) is missing")
require('"ص/غ"' in builder_screen and "Arrangement.spacedBy(2.dp)" in builder_screen,
        "V55.18 compact card header with short true/false label is missing")

# ---- V56.0: tablet optimization foundation (device layout mode) ----
_prefs=(ROOT/"app/src/main/java/ir/exam/app/core/ui/AppearancePreferences.kt").read_text()
_device_layout=(ROOT/"app/src/main/java/ir/exam/app/core/ui/DeviceLayout.kt").read_text()
_theme=(ROOT/"app/src/main/java/ir/exam/app/core/ui/ExamAppTheme.kt").read_text()
_settings_screen=(ROOT/"app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsScreen.kt").read_text()
require("enum class DeviceLayoutMode { AUTO, PHONE, TABLET }" in _prefs
        and 'stringPreferencesKey("device_layout")' in _prefs,
        "V56.0 persisted device layout mode is missing")
require("const val TABLET_MIN_SMALLEST_WIDTH_DP = 600" in _device_layout
        and "LocalTabletLayout provides tabletLayout" in _theme,
        "V56.0 tablet layout detection/provider is missing")
require("چیدمان دستگاه" in _settings_screen
        and 'DeviceLayoutMode.AUTO to "خودکار"' in _settings_screen,
        "V56.0 appearance section device layout picker is missing")

# ---- V56.1: tablet layouts for the main screens ----
_menu=(ROOT/"app/src/main/java/ir/exam/app/ui/app/Design69MainMenuScreen.kt").read_text()
require("const val TABLET_COLUMNS = 3" in _menu
        and "cards.chunked(columns)" in _menu,
        "V56.1 tablet main menu columns are missing")
require("widthIn(max = 760.dp)" in builder_screen,
        "V56.1 tablet width cap for exam builder is missing")

# ---- V56.2: tablet grids for the picker dialogs ----
_figure_picker=(ROOT/"app/src/main/java/ir/exam/app/ui/figure/FigurePickerDialog.kt").read_text()
require("if (tabletPicker) GridCells.Fixed(3) else GridCells.Fixed(2)" in _figure_picker,
        "V56.2 tablet figure picker grid is missing")

# ---- V57.0: student row-faithful text, zoomable figures, typed atlas blanks ----
_zoom_dialog=(ROOT/"app/src/main/java/ir/exam/app/ui/figure/ZoomableFigureDialog.kt").read_text()
_blank_codec=(ROOT/"app/src/main/java/ir/exam/app/core/figure/AtlasBlankAnswerCodec.kt").read_text()
require("fun splitRows(source: String): List<List<RichSegment>>" in rich_text,
        "V57.0 row splitting for teacher line breaks is missing")
require("RichTextSplitter.splitRows(source)" in formula_text
        and "zoomableFigures: Boolean = false" in formula_text,
        "V57.0 NativeMathText row rendering/zoom flag is missing")
require("detectTransformGestures" in _zoom_dialog and "نمایش افقی" in _zoom_dialog
        and "rotationZ = 90f" in _zoom_dialog,
        "V57.0 zoom dialog with landscape periodic table is missing")
require("blankAnswers: Map<Int, String>? = null" in atlas_view
        and "OutlinedTextField(" in atlas_view,
        "V57.0 typed atlas naming boxes are missing")
require("AtlasBlankAnswerCodec.merge(" in student_screen
        and "zoomableFigures = true" in student_screen,
        "V57.0 student screen wiring (zoom + atlas answers) is missing")
require("fun merge(blanks: Map<Int, String>, free: String): String" in _blank_codec,
        "V57.0 atlas blank answer codec is missing")

# ---- V58.0: student exam UX + timer start/pause + colored countdown ----
_student_vm=(ROOT/"app/src/main/java/ir/exam/app/ui/student/StudentExamViewModel.kt").read_text()
# V59.0 — سطر شماره‌ها LazyRow با اسکرول خودکار به سؤال جاری شد.
require("animateScrollToItem(state.questionIndex" in student_screen
        and "ExamCountdownText(" in student_screen
        and 'OutlinedButton(onClick = { showExit = true }) { Text("خروج") }' in student_screen,
        "V58.0 student exam scrollable strip/bottom bar is missing")
require("started = true" in _student_vm and "if (state.value.timerPaused)" in _student_vm
        and "deadline + pausedTotalMs - System.currentTimeMillis()" in _student_vm,
        "V58.0 start-gated timer with teacher-edit pause is missing")
require("آزمون توسط معلم ویرایش شد" in student_screen,
        "V58.0 teacher-edit notification dialog is missing")
require('notice = "به بانک سؤال اضافه شد"' in (ROOT/"app/src/main/java/ir/exam/app/ui/builder/ExamBuilderViewModel.kt").read_text(),
        "V58.0 bank save confirmation message is missing")
require(".qmf-surface.input .an-af{display:none !important;}" in editor_asset,
        "V58.0 teacher editor must hide atlas naming boxes")

# ---- V58.1: exam monitor (security events + teacher reports) ----
_monitor_sql=(ROOT/"supabase/migrations/20260825_native_exam_monitor_v58.sql").read_text()
require("ScreenCaptureCallback" in student_screen and "screenshot_attempt" in student_screen,
        "V58.1 screenshot attempt detection is missing")
require("fun monitorReport()" in _student_vm and "native_monitor_upsert_v1" in (ROOT/"app/src/main/java/ir/exam/app/data/repository/SupabaseStudentExamRepository.kt").read_text(),
        "V58.1 monitor report pipeline is missing")
require('Text("گزارش‌ها")' in grading_screen and "MonitorReportsDialog(" in grading_screen,
        "V58.1 teacher reports button/dialog is missing")
require("student_id = auth.uid()" in _monitor_sql and "e.teacher_id = v_uid" in _monitor_sql,
        "V58.1 monitor RLS/ownership is missing")

# ---- V58.2: student answer graph with teacher permission ----
require("fun StudentAnswerGraph(" in student_screen
        and "if (presentation.allowAnswerGraph || questionHasGraph)" in student_screen,
        "V58.2 student answer graph flow is missing")
require('values["allowAnswerGraph"] = JsonPrimitive(question.allowAnswerGraph)' in (ROOT/"app/src/main/java/ir/exam/app/data/repository/ExamQuestionCodec.kt").read_text(),
        "V58.2 allowAnswerGraph persistence is missing")

# ---- V58.0.2: student exam device-report fixes ----
require("fun exitExamScreen()" in _student_vm
        and "kotlin.math.abs(newDeadline - oldDeadline) > 120_000L" in _student_vm,
        "V58.0.2 exit-to-screen / drift-safe teacher-edit diff is missing")
require("Icons.Outlined.KeyboardArrowRight" in student_screen
        and "Icons.AutoMirrored.Outlined.KeyboardArrowRight" not in student_screen,
        "V58.0.2 non-mirrored strip icons are missing")
require("questionHasGraph" in student_screen,
        "V58.0.2 automatic answer-graph unlock is missing")
# V58.0.3 — remember باید در متن Composable باشد نه LazyListScope.
require(student_screen.index("val questionHasGraph = remember(question.id, question.text)")
        < student_screen.index("    Scaffold("),
        "V58.0.3 questionHasGraph remember must stay in composable scope")
require("این سؤال از قبل در بانک سؤال موجود است" in (ROOT/"app/src/main/java/ir/exam/app/ui/builder/ExamBuilderViewModel.kt").read_text(),
        "V58.0.2 duplicate bank notice is missing")
require("onBlankAnswer != null &&" in atlas_view,
        "V58.0.2 atlas blanks must only render as student typing boxes")

# ---- V59.0: exam UX polish + colored monitor cards ----
require("OutlinedButton(onClick = { previewAll = true }, modifier = Modifier.fillMaxWidth())" not in builder_screen
        and "onPreviewAll = { previewAll = true }" in builder_screen,
        "V59.0 standalone A4 preview button must be gone while the eye menu path stays")
require("stripState.animateScrollToItem(state.questionIndex" in student_screen,
        "V59.0 auto-scroll to the current question chip is missing")
require("fun monitorSeverityColor(score: Int): Color" in grading_screen
        and "clickable { selected = index }" in grading_screen,
        "V59.0 colored monitor report cards are missing")

# ---- V59.1: guarded staff account deletion ----
_delete_sql=(ROOT/"supabase/migrations/20260825_native_delete_account_v59.sql").read_text()
_edge_manage=(ROOT/"supabase/functions/manage-student/index.ts").read_text()
_settings_screen2=(ROOT/"app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsScreen.kt").read_text()
require('title = "حذف حساب"' in _settings_screen2 and "بله، حساب حذف شود" in _settings_screen2,
        "V59.1 delete-account card/confirmation is missing")
require("set teacher_id = s.new_owner" in _delete_sql
        and "grant execute on function public.native_prepare_account_deletion_v1(uuid) to service_role" in _delete_sql,
        "V59.1 ownership transfer or service-role-only grant is missing")
require("action === 'delete_account'" in _edge_manage
        and "await service.auth.admin.deleteUser(teacherId)" in _edge_manage,
        "V59.1 edge delete_account action is missing")

# ---- V59.2: calendar notify + rejoin + cleanups ----
_cal_sql=(ROOT/"supabase/migrations/20260825_native_calendar_notify_v59.sql").read_text()
_home=(ROOT/"app/src/main/java/ir/exam/app/ui/student/StudentHomeScreen.kt").read_text()
_cal_screen=(ROOT/"app/src/main/java/ir/exam/app/ui/calendar/CalendarScreen.kt").read_text()
require("هزینه هر سؤال مشمول" not in builder_screen,
        "V59.2 the per-question cost sentence must stay removed")
require("fun rejoinActiveExam()" in _student_vm and "آزمون نیمه‌تمام دارید" in _home,
        "V59.2 rejoin half-finished exam flow is missing")
require("l.student_id = v_uid and l.teacher_id = n.teacher_id" in _cal_sql,
        "V59.2 linked-teacher calendar visibility is missing")
require("برای روزهای گذشته فقط حذف پیام ممکن است" in _cal_sql
        and "if (isTeacher && day != null && !dayIsPast) {" in _cal_screen,
        "V59.2 past-day calendar lock is missing")
require("پیام جدید دارید" in _home and "cal_mark_seen_v59" in _cal_sql,
        "V59.2 student new-message banner is missing")

# ---- V59.2.1: delete-account FK cleanup + calendar class coverage + web field lag ----
_del_sql2=(ROOT/"supabase/migrations/20260825_native_delete_account_v59.sql").read_text()
_web_section=(ROOT/"app/src/main/java/ir/exam/app/ui/builder/QuestionTextWebSection.kt").read_text()
require("delete from public.schools where created_by = v_uid" in _del_sql2
        and "delete from public.school_students where created_by = v_uid" in _del_sql2,
        "V59.2.1 non-cascading auth reference cleanup is missing")
require("join public.classes c on c.id = m.class_id" in _cal_sql,
        "V59.2.1 class-membership calendar coverage is missing")
require("animateContentSize" not in _web_section
        and "alpha = if (webReady || overlayOpen) 1f else 0f" in _web_section,
        "V59.2.1 question web field lag fix is missing")

# ---- V59.3: post-deletion signout + storage image cleanup ----
_storage_sql=(ROOT/"supabase/migrations/20260825_native_storage_cleanup_v59.sql").read_text()
_cleaner=(ROOT/"app/src/main/java/ir/exam/app/data/repository/StorageImageCleaner.kt").read_text()
require("onAccountDeleted = authViewModel::signOut" in (ROOT/"app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").read_text(),
        "V59.3 post-deletion local signout wiring is missing")
require("SignOutScope.LOCAL" in (ROOT/"app/src/main/java/ir/exam/app/data/repository/SupabaseProfileRepository.kt").read_text(),
        "V59.3 delete-account local signout is missing")
require("v59_owner_delete_exam_images" in _storage_sql
        and "delete(*paths.toTypedArray())" in _cleaner,
        "V59.3 storage image cleanup is missing")
require("StorageImageCleaner.removeByPublicUrls" in (ROOT/"app/src/main/java/ir/exam/app/ui/builder/ExamBuilderViewModel.kt").read_text(),
        "V59.3 question deletion storage cleanup is missing")

# ---- V60.0: staff username login + Google registration ----
_auth_repo=(ROOT/"app/src/main/java/ir/exam/app/data/repository/SupabaseAuthRepository.kt").read_text()
_sign_in=(ROOT/"app/src/main/java/ir/exam/app/ui/auth/SignInScreen.kt").read_text()
_provider=(ROOT/"app/src/main/java/ir/exam/app/data/remote/SupabaseProvider.kt").read_text()
require("native_staff_login_email_v1" in _auth_repo,
        "V60.0 staff username login mapping is missing")
require('GoogleRegisterButton(state = state, viewModel = viewModel, role = "teacher")' in _sign_in
        and 'GoogleRegisterButton(state = state, viewModel = viewModel, role = "manager")' in _sign_in,
        "V60.0 Google registration buttons are missing")
# V60.1 — پلاگین compose-auth با Credential Manager مستقیم جایگزین شد
# (callback گم می‌شد)؛ ورود با IDToken در ViewModel انجام می‌شود.
require("CredentialManager.create(context)" in _sign_in
        and "GoogleIdTokenCredential" in _sign_in,
        "V60.1 direct Credential Manager google flow is missing")
require("fun signInWithGoogleIdToken(idToken: String, rawNonce: String, role: String)" in (ROOT/"app/src/main/java/ir/exam/app/ui/auth/AuthViewModel.kt").read_text(),
        "V60.1 IDToken sign-in is missing")
# secret نباید هاردکد شود: مقدار واقعی client id (الگوی apps.googleusercontent.com) در سورس ممنوع
require("googleusercontent.com" not in _provider and "googleusercontent.com" not in _sign_in,
        "V60.0 google client id must come from local.properties, never hardcoded")

# ---- V60.2: google registration role table + official logo ----
_role_sql=(ROOT/"supabase/migrations/20260825_native_registration_role_v60_2.sql").read_text()
_google_logo=(ROOT/"app/src/main/java/ir/exam/app/ui/auth/GoogleLogo.kt").read_text()
require("create table if not exists public.native_registration_roles" in _role_sql
        and "update auth.users" not in _role_sql,
        "V60.2 registration role must live in a public table, not auth.users")
require("acceptAuthenticatedUser(user)" in (ROOT/"app/src/main/java/ir/exam/app/ui/auth/AuthViewModel.kt").read_text(),
        "V60.2 google sign-in must use the shared accept path")
require("val GoogleLogo: ImageVector" in _google_logo
        and "imageVector = GoogleLogo" in _sign_in,
        "V60.2 official google logo is missing")

# ---- V60.3: empty trigger-made teachers must still enter setup ----
_state_sql=(ROOT/"supabase/migrations/20260825_native_google_role_state_v60_3.sql").read_text()
require("coalesce(p.username, '') = ''" in _state_sql
        and "from public.native_registration_roles r" in _state_sql,
        "V60.3 empty-teacher setup detection is missing")

# ---- V60.3.1: the client must also ask the server for teacher profiles ----
# گارد قدیمی realEmailStudent فقط نقش student را می‌پرسید؛ trigger قدیمی نقش
# teacher می‌سازد پس منطق سروری V60.3 هرگز خوانده نمی‌شد.
require("val setupCandidate = role == UserRole.STUDENT ||" in _auth_repo
        and "(role == UserRole.TEACHER && profile.username.isNullOrBlank())" in _auth_repo
        and "if (realEmailAccount && setupCandidate)" in _auth_repo,
        "V60.3.1 client-side setup candidate check is missing")

# ---- V60.4: registration must accept the six-character manager invite code ----
# مدیر از V40B فقط کد کوتاه ۶ حرفی می‌سازد؛ مسیر ثبت‌نام فقط TCH- می‌پذیرفت.
require("if (isShortCode) {" in _auth_repo
        and '"native_join_school_v39"' in _auth_repo
        and "native_complete_teacher_registration_v37" in _auth_repo,
        "V60.4 short invite code registration path is missing")
require("اگر مدیر مدرسه کد ۶ حرفی یا کد TCH داده است، آن را اینجا وارد کنید." in _sign_in,
        "V60.4 invite field hint is missing")

# ---- V61.0: auth landing redesign ----
_sign_in_v61=(ROOT/"app/src/main/java/ir/exam/app/ui/auth/SignInScreen.kt").read_text()
# V62.1 — ورود سه‌نقشه در یک کارت با تب سگمنتی ماژول؛ پنجره‌های اختصاصی
# StaffLoginPane/StudentLoginPane از داخل LoginPane انتخاب می‌شوند.
require("AuthScreen.SIGN_IN -> LandingPane(state, viewModel)" in _sign_in_v61
        and "AuthScreen.LOGIN_STUDENT -> LoginPane(state, viewModel)" in _sign_in_v61
        and 'labels = listOf("مدیر/معاون", "معلم", "دانش‌آموز")' in _sign_in_v61
        and "StaffLoginPane(state, viewModel, managerRole = selectedTab == 0)" in _sign_in_v61
        and 'Text("ورود با گوگل")' in _sign_in_v61
        and "private fun BackButtonRow(" in _sign_in_v61,
        "V61.0 auth landing/login-role redesign is missing")
require("fun showLoginRole() = switchTo(AuthScreen.LOGIN_ROLE)" in
        (ROOT/"app/src/main/java/ir/exam/app/ui/auth/AuthViewModel.kt").read_text(),
        "V61.0 login role navigation is missing")

# ---- V61.1: teacher schools view + schools audience ----
_school_v61=(ROOT/"app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt").read_text()
_v61_sql=(ROOT/"supabase/migrations/20260826_native_schools_audience_v61.sql").read_text()
require('OutlinedButton(onClick = onSchools, modifier = Modifier.weight(1f)) { Text("مدارس") }' in _school_v61
        and "private fun SchoolsContent(" in _school_v61
        and "private fun SchoolClassesContent(" in _school_v61,
        "V61.1 teacher schools drill-down is missing")
require("native_teacher_schools_v61" in _v61_sql
        and "native_exam_school_students_v61" in _v61_sql
        and "calendar_note_schools" in _v61_sql
        and "exam_audience_schools" in _v61_sql,
        "V61.1 schools audience SQL is missing")
# V61.0.1 — exams از V38 ستون school_id دارد؛ در join حتماً پیشوند s لازم است
# وگرنه PostgreSQL خطای «column reference is ambiguous» می‌دهد.
require("jsonb_agg(s.school_id::text)" in _v61_sql
        and "select jsonb_agg(school_id::text)" not in _v61_sql,
        "V61.0.1 ambiguous school_id column returned to the V61 SQL")
require('"schools" to "مدارس"' in builder_screen
        and "CalendarAudience.SCHOOLS" in (ROOT/"app/src/main/java/ir/exam/app/ui/calendar/CalendarScreen.kt").read_text(),
        "V61.1 schools audience UI is missing")

# ---- V61.4: multi-school + centered audience + duplicate v22 overload drop ----
_v61_1_sql=(ROOT/"supabase/migrations/20260826_native_multi_school_v61_1.sql").read_text()
require("drop function if exists public.native_add_student_to_classes_v22(uuid, jsonb);" in _v61_1_sql
        and "drop index if exists public.ux_school_one_active_membership_v36;" in _v61_1_sql
        and "native_manager_create_school_v61" in _v61_1_sql,
        "V61.4 multi-school SQL (overload drop / membership / create school) is missing")
_cal_audience=(ROOT/"app/src/main/java/ir/exam/app/ui/calendar/CalendarScreen.kt").read_text().split(
    "// V61.1 — مخاطبان و دکمه‌ها وسط‌چین",1)[1].split("if (editor.audience == CalendarAudience.SCHOOLS)",1)[0]
# V61.6 — سه دکمه در «یک سطر» وسط‌چین (Row با spacedBy CenterHorizontally).
require("Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)" in _cal_audience
        and "CalendarAudience.STUDENTS" not in _cal_audience,
        "V61.4/V61.6 calendar audience must be one centered row without the students chip")
_builder_audience=builder_screen.split("// V61.1 — عنوان و دکمه‌ها وسط‌چین",1)[1].split(
    'if (state.audienceMode == "schools")',1)[0]
require("Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)" in _builder_audience
        and '"students" to' not in _builder_audience,
        "V61.4 exam audience must be centered without the students chip")
require('Text("ساخت مدرسه جدید")' in (ROOT/"app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt").read_text()
        and "native_manager_create_school_v61" in (ROOT/"app/src/main/java/ir/exam/app/ui/classes/ClassesViewModel.kt").read_text(),
        "V61.4 manager create-school flow is missing")

# ---- V61.7: manager backup + cross quick-add + invite confirm/freeze + filter cards ----
_v61_7_sql=(ROOT/"supabase/migrations/20260826_native_manager_backup_invite_freeze_v61_7.sql").read_text()
require("native_manager_export_backup_v61" in _v61_7_sql
        and "'used_at',coalesce(i.used_at::text,'')" in _v61_7_sql,
        "V61.7 manager backup / frozen invite timer SQL is missing")
require("private fun ManagerBackupSection()" in profile_settings
        and "native_manager_export_backup_v61" in profile_settings,
        "V61.7 manager backup card is missing")
require("targetY = -cornerY" in design69_add and "targetY = cornerY" in design69_add
        and ".forEach { corner ->" in design69_add,
        "V61.7 cross quick-add layout is missing")
require("زمان‌سنج متوقف شد: %02d:%02d:%02d" in manager_foundation
        and "deleteInviteTarget = invite" in manager_foundation
        and "بله، حذف شود" in manager_foundation,
        "V61.7 frozen invite timer or delete confirmation is missing")
require("fun FilterSectionCard(" in school_screen
        and "tint = if (active) Color(0xFFD32F2F) else LocalContentColor.current" in school_screen
        and 'TextButton(onClick = { draft = StudentListFilter() }) { Text("حذف فیلترها") }' in school_screen,
        "V61.7 filter section cards / sticky clear are missing")
# V61.8 — شکل داخل graphicsLayer تا کل انیمیشن مربع گوشه‌گرد باشد.
require("shape = RoundedCornerShape(22.dp)" in builder_radial
        and "scaleX = .6f + .4f * p" in builder_radial,
        "V61.7/V61.8 rounded-square builder radial buttons are missing")

# ---- V62.0: ice auth shell (UI only; auth logic untouched) ----
_ice=(ROOT/"app/src/main/java/ir/exam/app/ui/auth/AuthIceComponents.kt").read_text()
require("internal fun IceBackdrop(" in _ice and "internal fun OtpBoxes(" in _ice
        and "internal fun StepIndicator(" in _ice and "maxLength: Int = 8" in _ice,
        "V62.0 ice auth components are missing")
# V62.1 — هم‌ترازی کامل با ماژول: تب‌های لغزان RoleTabs، خوش‌آمد BrandHero،
# نوار مراحل با برچسب‌های ماژول؛ V62.1.2: ورود پلکانی آیتم‌به‌آیتم ماژول.
require("IceBackdrop(Modifier.fillMaxSize())" in _sign_in_v61
        and "StaggeredItem(0) { Brand() }" in _sign_in_v61
        and "state.otp.length in 6..8" in _sign_in_v61,
        "V62.0 ice shell wiring or 6..8 otp rule is missing")
require("internal fun RoleTabs(" in _ice
        and "internal fun BrandHero()" in _ice
        and "internal fun ScreenHeader(" in _ice
        and "internal fun StaggeredItem(" in _ice,
        "V62.1 module role tabs / welcome hero are missing")
require('private val RecoverySteps = listOf("ایمیل", "کد بازیابی", "رمز جدید")' in _sign_in_v61
        and "steps = RecoverySteps" in _sign_in_v61,
        "V62.1 module recovery step labels are missing")
# V62.1.1 — Int*Dp کامپایل نمی‌شود (CI شکست)؛ ضرب باید از سمت Dp باشد.
# V62.1.2 — offset(x) خودش RTL-آگاه است؛ آینه‌سازی دستی نشانگر را قرینه
# می‌گذاشت (گزارش دستگاه) و حذف شد.
require("targetValue = itemWidth * selected" in _ice
        and "maxWidth - itemWidth - logicalOffset" not in _ice,
        "V62.1.2 RoleTabs must use the logical Dp*Int offset without manual RTL mirroring")
# V62.1.3 — تعویض تب باید stagger را از نو اجرا کند و کارت مات بدون سایه باشد
# (هالهٔ سایه پشت سطح نیمه‌شفاف مثل کادر سفید دوم دیده می‌شد).
require("androidx.compose.runtime.key(selectedTab) {" in _sign_in_v61
        and "androidx.compose.runtime.key(managerTab) {" in _sign_in_v61
        and "border(1.dp, IceStroke, RoundedCornerShape(24.dp))" in _ice,
        "V62.1.3 tab remount keys or opaque shadow-free card are missing")
# V62.1.4 — ریپل خاکستری clickable تب‌ها حذف (کادر خاکستری قبل از نشانگر).
_roletabs_v62=_ice.split("internal fun RoleTabs(",1)[1].split("internal fun OtpBoxes(",1)[0]
require("indication = null" in _roletabs_v62,
        "V62.1.4 role tab ripple must be disabled")
# V62.5 — اکسل دومرحله‌ای (گروه با فیلتر + انتخاب ستون‌ها + رمز Vault)،
# تغییر رمز با رمز فعلی/بازیابی ایمیلی، داک مدیر در داشبورد خاموش،
# آیکن‌های فشرده‌تر سربرگ کارت سؤال.
require("StudentExportColumnsDialog(" in school_screen
        and "internal val StudentExportColumns" in school_screen
        and "exportStep = 2" in school_screen,
        "V62.5 two-step student excel export is missing")
_profile_vm_v62=(ROOT/"app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsViewModel.kt").read_text()
_profile_repo_v62=(ROOT/"app/src/main/java/ir/exam/app/data/repository/SupabaseProfileRepository.kt").read_text()
_profile_ui_v62=(ROOT/"app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsScreen.kt").read_text()
require("repository.verifyCurrentPassword(currentPassword).getOrThrow()" in _profile_vm_v62
        and "رمز فعلی نادرست است." in _profile_repo_v62
        and "کد بازیابی فقط به ایمیل همین حساب ارسال می‌شود." in _profile_repo_v62
        and 'Text("رمز فعلی")' in _profile_ui_v62,
        "V62.5 current-password gate with email recovery is missing")
require("if (managerDashboardActive) TeacherDockSection.NONE" in app_shell,
        "V62.5 manager dashboard must keep the dock stats button unselected")
_editor_v62=builder_screen.split("private fun QuestionEditor(",1)[1].split("private fun QuestionStyleControls(",1)[0]
require(".size(30.dp)" in _editor_v62 and ".size(38.dp)" not in _editor_v62,
        "V62.5 tighter question-card header icons are missing")
# V62.6 — حریم خصوصی معلم + UX مدیر: اشتراک کلاس/دانش‌آموز با تأیید معلم،
# پنجرهٔ + کلاس مدیر، هدر پویا، کد دعوت با مدرسه، فیلتر کلاس مدیر،
# کارت مدارس بدون بازگشت، منوی مستقل کارنامه/وضعیت، بازگشت دعوت→داشبورد.
_v62_6_sql=(ROOT/"supabase/migrations/20260827_native_teacher_privacy_invite_school_v62_6.sql").read_text()
require(_v62_6_sql == (ROOT/"sql/manual/SQL_NATIVE_TEACHER_PRIVACY_INVITE_SCHOOL_V62_6.sql").read_text()
        and all(marker in _v62_6_sql for marker in (
            "shared_with_manager boolean not null default false",
            "native_teacher_share_class_v62","native_teacher_share_student_v62",
            "native_manager_create_teacher_invites_v62","native_manager_school_classes_v62"
        )), "V62.6 teacher privacy SQL or copy incomplete")
# V62.8 — سوییچ اشتراک به آیکن چشم روی کارت کلاس/دانش‌آموز تبدیل شد.
require("fun setClassShared(id: String, shared: Boolean)" in
        (ROOT/"app/src/main/java/ir/exam/app/ui/classes/ClassesViewModel.kt").read_text()
        and "نمایش کلاس به مدیر" in school_screen
        and "نمایش دانش‌آموز به مدیر" in school_screen,
        "V62.6 teacher share switch is missing")
require('Text("افزودن جدید")' in manager_class_screen
        and 'Text("افزودن موجود")' in manager_class_screen
        and "FloatingActionButton(" in manager_class_screen,
        "V62.6 manager class plus dialog is missing")
require("managerClassHeader != null) managerClassHeader" in app_shell
        and "onInviteBack = ::openManagerDashboard" in app_shell
        and 'section = managerCardsSection ?: "status"' in app_shell,
        "V62.6 dynamic manager header / invite-back / stats sections are missing")
require("معلم به کدام مدرسه بپیوندد؟" in manager_foundation
        and 'val reportMode = section == "report"' in manager_foundation,
        "V62.6 invite school picker / report menu are missing")
require("if (managerTeacherPicker) state.managerFilterClasses else state.classes" in school_screen
        and "showBackToClasses = !(schoolsFromDock && managerTeacherPicker)" in school_screen,
        "V62.6 manager filter classes / dock schools back removal are missing")
# V62.7 — چشم=پیش‌نمایش دانش‌آموزی، دکمه‌های وسط‌چین کارت آزمون بدون چاپ،
# صفحهٔ چاپ آزمون با سربرگ رسمی ۵سطری سه‌ستونه + آرم، جریان معلم/کلاس مدیر،
# SQL خلاصهٔ چندمدرسه‌ای.
_preview_v62=(ROOT/"app/src/main/java/ir/exam/app/ui/builder/StudentQuestionPreview.kt").read_text()
_print_center_v62=(ROOT/"app/src/main/java/ir/exam/app/ui/printing/ExamPrintCenterScreen.kt").read_text()
_pdf_v62=(ROOT/"app/src/main/java/ir/exam/app/core/printing/OfficialPdfPrintAdapter.kt").read_text()
_summary_sql_v62=(ROOT/"supabase/migrations/20260827_native_manager_summary_multischool_v62_7.sql").read_text()
require("fun StudentQuestionPreviewDialog(" in _preview_v62
        and "پیش‌نمایش دانش‌آموزی سؤال" in builder_screen
        and "StudentQuestionPreviewDialog(" in builder_screen,
        "V62.7 student-view eye preview is missing")
require('Text("چاپ برگه")' in _print_center_v62
        and 'Text("چاپ با کلید")' in _print_center_v62
        and "چاپ برگه" not in teacher_dashboard,
        "V62.7 print actions must live only in the print center")
require('"چاپ آزمون", "اطلاعات رسمی چاپ آزمون"' in app_shell
        and "MainPage.PRINT" in app_shell
        and 'if (headerOpen) "بستن سربرگ" else "سربرگ"' in _print_center_v62
        and "fun HeaderPreview(" in _print_center_v62,
        "V62.7 print center with centered header dialog is missing")
require("print/emblem.png" in _pdf_v62
        and "drawHeaderCell(" in _pdf_v62
        and "وزارت آموزش و پرورش جمهوری اسلامی ایران" in _pdf_v62
        and (ROOT/"app/src/main/assets/print/emblem.png").exists(),
        "V62.7 official five-row emblem header is missing")
require("managerCreatePickerOpen = true" in school_screen
        and "fun createStudentsBulkForManagerClass(" in
            (ROOT/"app/src/main/java/ir/exam/app/ui/classes/ClassesViewModel.kt").read_text(),
        "V62.7 manager teacher/class-first student creation is missing")
require(_summary_sql_v62 == (ROOT/"sql/manual/SQL_NATIVE_MANAGER_SUMMARY_MULTISCHOOL_V62_7.sql").read_text()
        and "school_id in(select school_id from mine)" in _summary_sql_v62,
        "V62.7 multischool manager summary SQL or copy incomplete")
# V62.8 — چشم اشتراک، فرم معلم داخل کلاس مدیر، چیپ منعطف، انتخاب اختیاری،
# + مستقیم بدون کادر، فونت/فرمت سربرگ، دیالوگ کیبوردپذیر.
require("نمایش دانش‌آموز به مدیر" in school_screen
        and "fun setStudentShared(id: String, shared: Boolean)" in
            (ROOT/"app/src/main/java/ir/exam/app/ui/classes/ClassesViewModel.kt").read_text(),
        "V62.8 student share eye is missing")
require("fun ManagerStudentCreateDialog(" in school_screen
        and "ManagerStudentCreateDialog(" in manager_class_screen
        and "repo.setClassStudent(selected!!.id,studentId,true)" in manager_class_screen,
        "V62.8 teacher-form student creation inside manager class is missing")
require("FlowRow(" in manager_foundation
        and 'else "ساخت بدون کلاس"' in school_screen
        and "suspend fun managerClassRoster(classId: String)" in
            (ROOT/"app/src/main/java/ir/exam/app/ui/classes/ClassesViewModel.kt").read_text(),
        "V62.8 flexible invite chips / optional class flow are missing")
# V62.8.1 — رفع خطای CI: importهای اسکرول پنجرهٔ لیست اعضای کلاس.
require("import androidx.compose.foundation.verticalScroll" in school_screen
        and "import androidx.compose.foundation.rememberScrollState" in school_screen,
        "V62.8.1 scroll imports for manager roster dialog are missing")
# V62.8.2 — تست V62.7 با کد V62.8 هماهنگ ماند (مدت با پسوند، دکمهٔ شرطی).
_v62_7_test=(ROOT/"app/src/test/java/ir/exam/app/ui/app/V62_7PrintCenterStudentPreviewTest.kt").read_text()
require("مدت آزمون: \\${header.examDuration}" not in _v62_7_test
        and 'Text(\\"ادامه و ساخت دانش‌آموز\\")' not in _v62_7_test,
        "V62.8.2 stale V62.7 needles are back")
# V63.0.1 — needle قدیمی امضای بی‌پارامتر صفحهٔ چاپ ممنوع (V63.0 پارامتر مداد داد).
require('"ExamPrintCenterScreen()" in appShell' not in _v62_7_test,
        "V63.0.1 stale print-center signature needle is back")
require("fonts/bnazanin.ttf" in _pdf_v62
        and "$" + "it دقیقه" in _pdf_v62
        and ".imePadding().verticalScroll(" in _print_center_v62
        and 'jalaliDisplay(it).substringBefore(" ")' in _print_center_v62,
        "V62.8 nazanin header font / minute suffix / ime-aware dialog are missing")
# V62.2/V62.4 — صفحهٔ بازیابی نشست: پس‌زمینهٔ یخی لاگین + اسپینر دو کمانهٔ
# بزرگ (V62.4: بدون هالهٔ نئونی و هستهٔ نبض‌دار).
require("internal fun IceSpinner(" in _ice
        and "fun IceSessionLoading(message: String)" in _ice
        and "Brush.sweepGradient(" in _ice
        and "modifier.size(96.dp)" in _ice
        and 'IceSessionLoading(message = "در حال بازیابی نشست ورود...")' in app_shell,
        "V62.2 ice session-loading screen is missing")
# V62.4 — پس‌زمینهٔ یخی سراسری: پوستهٔ اصلی و منو بدون موج؛ قفل برنامه با
# موج و باز شدن خودکار پنجرهٔ قفل امن دستگاه.
require("fun IceAppBackdrop(modifier: Modifier = Modifier, waves: Boolean = false)" in _ice
        and "IceAppBackdrop(Modifier.fillMaxSize(), waves = false)" in app_shell
        and "containerColor = androidx.compose.ui.graphics.Color.Transparent" in app_shell,
        "V62.4 app-wide waveless ice backdrop is missing")
require("IceAppBackdrop(Modifier.fillMaxSize(), waves = true)" in app_lock_ui
        and "LaunchedEffect(locked, prompt)" in app_lock_ui
        and 'Text("تأیید با قفل امن دستگاه")' in app_lock_ui,
        "V62.4 auto-prompting icy app-lock screen is missing")
# V62.3 — تغییر وضعیت قفل برنامه فقط پس از تأیید قفل امن دستگاه ذخیره می‌شود.
require("prompt?.authenticate(togglePromptInfo(target))" in app_lock_ui
        and "private fun togglePromptInfo(enable: Boolean)" in app_lock_ui
        and "pendingToggle = target" in app_lock_ui,
        "V62.3 device-credential gate on the app-lock toggle is missing")

# ---- V61.9: pro icons + manager default dashboard + teacher-style cards + filter order ----
_icons_v61=(ROOT/"app/src/main/java/ir/exam/app/ui/app/Design69Icons.kt").read_text()
_stack_v61=(ROOT/"app/src/main/java/ir/exam/app/ui/app/TeacherManagementCardsScreen.kt").read_text()
require("private fun PathBuilder.addBadge(" in _icons_v61
        and "val SchoolAdd: ImageVector by lazy" in _icons_v61
        and "val TeacherInvite: ImageVector by lazy" in _icons_v61,
        "V61.9 professional quick-add icons are missing")
require("icon = Design69Icons.SchoolAdd" in design69_add
        and "Design69Icons.TeacherInvite else Design69Icons.ExamAdd" in app_shell,
        "V61.9 quick-add icon wiring is missing")
require("mutableStateOf(if (user.role == UserRole.MANAGER) MainPage.CARDS else MainPage.CALENDAR)" in app_shell
        and 'mutableStateOf<String?>("status")' in app_shell
        and "fun openManagerDashboard()" in app_shell,
        "V61.9 manager default dashboard is missing")
require("fun ManagerManagementCardsScreen(" in _stack_v61
        and "private fun ManagementCardsStack(" in _stack_v61
        and "fun ManagerCardsScreen(" not in manager_foundation,
        "V61.9 teacher-style manager cards are missing")
_filter_dialog_v61=school_screen.split("private fun StudentFilterDialog(",1)[1].split("private fun StudentCard(",1)[0]
require("هر مدرسه" not in _filter_dialog_v61
        and _filter_dialog_v61.index('key = "school"') < _filter_dialog_v61.index('key = "unassigned"'),
        "V61.9 filter school list / unassigned-last ordering is missing")

# ---- V61.2: manager dashboard + class teacher picker ----
require('"داشبورد", "اطلاعات مدرسه و آمار", Design69Icons.Dashboard,' in app_shell
        and "featuredCard = if (user.role == UserRole.MANAGER) {" in app_shell,
        "V61.2 manager dashboard card is missing")
require('Text("پنل سریع", style = MaterialTheme.typography.titleMedium)' in manager_foundation
        and "private fun QuickPanelCard(" in manager_foundation,
        "V61.2 dashboard quick panel is missing")
require("native_manager_save_teacher_class_v40c" in
        (ROOT/"app/src/main/java/ir/exam/app/ui/classes/ClassesViewModel.kt").read_text()
        and "managerTeacherPicker = user.role == UserRole.MANAGER" in app_shell,
        "V61.2 manager class teacher picker is missing")

# ---- V61.3: bulk form order + direct-typing custom grade field ----
_bulk_v61=_school_v61.split("private fun BulkStudentDialog(",1)[1].split("internal fun studentClipboardText",1)[0]
require("// V61.0 — ترتیب درخواستی وسط‌چین: چشم، پسر، دختر، تاس." in _bulk_v61
        and _bulk_v61.count("passwordTransformation(row.passwordVisible)") == 2,
        "V61.3 bulk create centered eye/boy/girl/dice row is missing")
require("if (customMode) {" in grade_odometer
        and "بازکردن انتخاب‌گر" in grade_odometer,
        "V61.3 direct-typing custom grade field is missing")

# ---- V58.0.1: the top-level layout.weight import is internal and must never appear ----
require("import androidx.compose.foundation.layout.weight" not in (ROOT/"app/src/main/java/ir/exam/app/ui/figure/ZoomableFigureDialog.kt").read_text(),
        "V58.0.1 internal weight import returned to ZoomableFigureDialog")

# ---- V63.0: word-like document editor (patch 1 of 3) + pencil in print center ----
_word_layout_v63=(ROOT/"app/src/main/java/ir/exam/app/core/printing/WordPageLayout.kt").read_text()
_doc_editor_v63=(ROOT/"app/src/main/java/ir/exam/app/ui/printing/ExamDocumentEditorScreen.kt").read_text()
_v63_test=(ROOT/"app/src/test/java/ir/exam/app/ui/app/V63_0WordDocumentEditorTest.kt").read_text()
require("const val PAGE_WIDTH_MM: Float = 210f" in _word_layout_v63
        and "const val PAGE_HEIGHT_MM: Float = 297f" in _word_layout_v63
        and "fun paginate(blocks: List<WordBlock>" in _word_layout_v63
        and "fun questionHeightMm(question: QuestionDraft): Float" in _word_layout_v63
        and "android." not in _word_layout_v63 and "androidx." not in _word_layout_v63,
        "V63.0 A4 word-layout engine is missing or android-dependent")
require("fun ExamDocumentEditorScreen(" in _doc_editor_v63
        and "WordPageLayout.documentOf(state.questions)" in _doc_editor_v63
        and "contentDescription = \"ویرایش سؤال ${block.row}\"" in _doc_editor_v63
        and "ExamBuilderScreen(" not in _doc_editor_v63
        and "صفحهٔ ${page.number} از $pageCount" in _doc_editor_v63,
        "V63.0 word-like document editor screen is missing")
require("onEditExamDocument: (String) -> Unit" in _print_center_v62
        and "Icons.Outlined.Edit" in _print_center_v62
        and "ویرایش آزمون" in _print_center_v62
        and "onEditExamDocument(exam.id)" in _print_center_v62
        and "Text(\"چاپ برگه\")" in _print_center_v62,
        "V63.0 edit pencil on print-center exam card is missing")
require("DOC_EDITOR" in app_shell
        and "editingDocumentExamId" in app_shell
        and "ExamDocumentEditorScreen(" in app_shell
        and "onEditExamDocument = { examId ->" in app_shell
        and "MainPage.DOC_EDITOR -> \"ویرایش آزمون\"" in app_shell,
        "V63.0 app-shell routing to the document editor is missing")
require("class V63_0WordDocumentEditorTest" in _v63_test
        and "WordPageLayout.documentOf(" in _v63_test
        and "an oversized question occupies its own page" in _v63_test,
        "V63.0 document-editor tests are missing")
# ---- V63.1: درگ/ریسایز اشیا در ویرایشگر سند؛ عرض شکل داخل توکن %%FIG%% ----
_v63_1_test=(ROOT/"app/src/test/java/ir/exam/app/ui/app/V63_1DocObjectDragResizeTest.kt").read_text()
require("fun DraggableQuestionImage(" in _doc_editor_v63
        and "fun ResizableFigure(" in _doc_editor_v63
        and "fun ResizeHandle(" in _doc_editor_v63
        and "onMoveImage = builder::moveImage" in _doc_editor_v63
        and "WordPageLayout.withFigureWidthMm(occ.spec, widthMm)" in _doc_editor_v63,
        "V63.1 drag/resize object controls are missing from the document editor")
require("fun withFigureWidthMm(spec: FigureSpec, widthMm: Float): FigureSpec" in _word_layout_v63
        and "fun figureWidthMm(spec: FigureSpec): Float" in _word_layout_v63
        and 'const val FIGURE_WIDTH_KEY: String = "wmm"' in _word_layout_v63
        and "WordPageLayout.figureWidthMm(rich.spec)" in _pdf_v62
        and "imageWidthMm=95f" not in _pdf_v62,
        "V63.1 persisted figure width (X.wmm) is missing from layout/print")
require("class V63_1DocObjectDragResizeTest" in _v63_1_test
        and "bigger objects really grow the paginated block" in _v63_1_test,
        "V63.1 drag/resize tests are missing")
# ---- V63.2: نوار قالب متن (اندازه/بولد/ایتالیک/تراز) + ترتیب سؤال‌ها ----
_v63_2_test=(ROOT/"app/src/test/java/ir/exam/app/ui/app/V63_2DocFormatReorderTest.kt").read_text()
# V63.3 — نوار واحد DocumentToolbar جایگزین QuestionFormatBar شد.
require("fun DocumentToolbar(" in _doc_editor_v63
        and "builder.setQuestionFontSize(it.id, it.fontSizeSp + delta)" in _doc_editor_v63
        and "builder.moveQuestion(it.id, delta)" in _doc_editor_v63
        and ".clickable(onClick = onSelect)" in _doc_editor_v63
        and "var textDialogQuestionId by remember" in _doc_editor_v63,
        "V63.2/63.3 format toolbar / reorder is missing from the document editor")
require(".horizontalScroll(rememberScrollState())" in _doc_editor_v63
        and "Icons.Outlined.ZoomIn" in _doc_editor_v63
        and "Icons.Outlined.ZoomOut" in _doc_editor_v63
        and "Icons.Outlined.OpenWith" in _doc_editor_v63
        and "var selectedImage by remember" in _doc_editor_v63
        and "var selectedFigure by remember" in _doc_editor_v63
        and "fun resizeFigureBy(" in _doc_editor_v63
        and "QuestionFormatBar" not in _doc_editor_v63,
        "V63.3 unified scrollable toolbar with object selection is missing")
require('contentDescription = "ویرایش آزمون"' in _print_center_v62
        and 'Text("ویرایش سند")' not in _print_center_v62,
        "V63.3 pencil-only edit icon on the print-center exam card is missing")
require("val weight = if (question.bold) FontWeight.Bold else null" in _doc_editor_v63
        and "val style = if (question.italic) FontStyle.Italic else null" in _doc_editor_v63,
        "V63.2 on-page style mirroring is missing")
require("class V63_2DocFormatReorderTest" in _v63_2_test
        and "format actions persist through the builder view-model used by print" in _v63_2_test,
        "V63.2 format/reorder tests are missing")

# V54.3.1 — رفع باگ ساختاری: requireهای بلوک‌های V53.x/V54.x بعد از اولین چک errors
# اجرا می‌شدند و هرگز enforce نمی‌شدند؛ بررسی نهایی الزامی است.
if errors:
    print("FINAL_NATIVE_VERIFY=FAIL")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print(f"FINAL_NATIVE_VERIFY=PASS kotlin_files={len(main_files)} edge_functions={len(edge_files)}")
