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
formula_editor=(ROOT/"app/src/main/java/ir/exam/app/ui/math/FormulaEditorDialog.kt").read_text()
formula_view=(ROOT/"app/src/main/java/ir/exam/app/ui/math/NativeFormulaView.kt").read_text()
formula_text=(ROOT/"app/src/main/java/ir/exam/app/ui/math/NativeMathText.kt").read_text()
formula_svg=(ROOT/"app/src/main/java/ir/exam/app/core/math/NativeMathSvgRenderer.kt").read_text()
formula_boxes=(ROOT/"app/src/main/java/ir/exam/app/core/math/FormulaBoxEditor.kt").read_text()
formula_reference_loader=(ROOT/"app/src/main/java/ir/exam/app/ui/math/FormulaReferenceLibrary.kt").read_text()
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

require(all((ROOT/"app/src/main/res/font"/name).exists() for name in ("vazirmatn_regular.ttf","shabnam_regular.ttf","sahel_regular.ttf")),"bundled Persian fonts missing")
require("native_save_exam_v2" in parity and "native_bank_snapshot_v1" in parity and "native_feedback_update_v1" in parity,"V13 backend parity RPCs missing")
require("پیش‌نمایش کامل A4" in builder_screen and "تعداد گزینه" in builder_screen and "حساس به حروف" in builder_screen,"builder parity controls missing")
require("مرور پیش از ارسال" in student_screen and "علامت برای مرور" in student_screen,"student navigation/review parity missing")
require((ROOT/"app/src/main/java/ir/exam/app/core/export/XlsxWorkbook.kt").exists(),"real XLSX writer missing")
require((ROOT/"app/src/main/java/ir/exam/app/ui/image/InteractiveImageEditorDialog.kt").exists(),"interactive crop editor missing")
require((ROOT/"app/src/main/java/ir/exam/app/ui/security/AppLockUi.kt").exists(),"PIN/device credential app lock missing")
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
require("also(::validate)" in formula_reference_loader and "پیوند دسته نامعتبر" in formula_reference_loader,
        "formula library links/content are not validated")
require("version = 4" in (ROOT/"app/src/main/java/ir/exam/app/data/local/AppDatabase.kt").read_text(),"Room V4 student notes migration missing")

for match in re.finditer(r"(?im)^\s*(delete\s+from|update\s+)([^;]+);", hardening + "\n" + critical + "\n" + parity):
    statement = match.group(0)
    if not re.search(r"(?i)\bwhere\b", statement):
        errors.append(f"UPDATE/DELETE without WHERE at line {hardening[:match.start()].count(chr(10))+1}")

if errors:
    print("FINAL_NATIVE_VERIFY=FAIL")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print(f"FINAL_NATIVE_VERIFY=PASS kotlin_files={len(main_files)} edge_functions={len(edge_files)}")
