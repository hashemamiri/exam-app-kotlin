#!/usr/bin/env python3
"""Fast offline guardrails for the all-native exam PDF pipeline.

Preview and Android printing must consume one immutable native PDF.  The only
permitted WebView is FormulaHostDialog / formula_editor/formula.html.
"""

from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "app/src/main"
KOTLIN = MAIN / "java"
PRINT = KOTLIN / "ir/exam/app/ui/printing"
CORE_PRINT = KOTLIN / "ir/exam/app/core/printing"
ASSETS = MAIN / "assets"
SCHEMA = ASSETS / "print/header_settings_schema.json"

PDF_DOCUMENT = PRINT / "NativeExamPdfDocument.kt"
PDF_PREVIEW = PRINT / "NativeExamPdfPreviewDialog.kt"
PDF_ENGINE = CORE_PRINT / "OfficialPdfPrintAdapter.kt"
PDF_LAYOUT_STATE = CORE_PRINT / "PrintPreviewLayoutCodec.kt"
PDF_IMAGES = CORE_PRINT / "OfficialExamImageLoader.kt"
HEADER_SETTINGS = PRINT / "PrintHeaderSettings.kt"

errors: list[str] = []


def require(condition: bool, message: str) -> None:
    if not condition:
        errors.append(message)


def read(path: Path) -> str:
    try:
        return path.read_text(encoding="utf-8")
    except OSError as exc:
        errors.append(f"cannot read {path.relative_to(ROOT)}: {exc}")
        return ""


# Both the older document editor and every print-specific WebView asset/host are
# retired.  Keeping this list explicit prevents a second renderer from returning.
retired_files = (
    "app/src/main/java/ir/exam/app/ui/printing/ExamDocumentEditorScreen.kt",
    "app/src/main/java/ir/exam/app/core/printing/WordPageLayout.kt",
    "app/src/main/java/ir/exam/app/data/local/PrintLayoutStore.kt",
    "app/src/main/java/ir/exam/app/ui/printing/ExamBuilder30Windows.kt",
    "app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintDialog.kt",
    "app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintPayload.kt",
    "app/src/main/java/ir/exam/app/ui/printing/ExamPrintAssetRenderer.kt",
    "app/src/main/java/ir/exam/app/ui/printing/ExamHtmlImageInliner.kt",
    "app/src/main/java/ir/exam/app/ui/builder/ExamPrintPreview.kt",
    "app/src/main/java/ir/exam/app/ui/math/QuestionTextFieldWebView.kt",
    "app/src/main/assets/print/exam_print.html",
    "app/src/main/assets/print/exam_print_renderer.html",
    "app/src/main/assets/print/math_editor.html",
)
for relative in retired_files:
    require(not (ROOT / relative).exists(), f"retired file returned: {relative}")

for path in (PDF_DOCUMENT, PDF_PREVIEW, PDF_ENGINE, PDF_LAYOUT_STATE, PDF_IMAGES, HEADER_SETTINGS, SCHEMA):
    require(path.is_file(), f"active native print file missing: {path.relative_to(ROOT)}")

pdf_document = read(PDF_DOCUMENT)
for marker in (
    "NativeExamPdfDocumentFactory",
    "PdfDocument",
    "NativeExamPdfPrintAdapter",
    "FileInputStream(document.pdfFile)",
    "OfficialExamImageLoader.load",
    "PrintAttributes.MediaSize.ISO_A4",
    # شکل در چند page crop ممکن است بریده شود؛ geometry کامل باید بماند.
    "flowBounds",
    "canResize",
):
    require(marker in pdf_document, f"shared native PDF contract missing: {marker}")
for forbidden in ("WebView", "android.webkit", "createPrintDocumentAdapter", "ExamHtml"):
    require(forbidden not in pdf_document, f"retired web-print code in native PDF document: {forbidden}")

pdf_preview = read(PDF_PREVIEW)
for marker in (
    "PdfRenderer",
    "NativeExamPdfDocumentFactory.create",
    "NativeExamPdfPrintAdapter(documentToPrint",
    "NativePdfInteractionOverlay",
    "detectDragGestures",
    "detectTapGestures",
    "PrintPreviewLayoutCodec.updateFigure",
    "ExamFigureToolHost",
    # هیچ gesture نباید از bounds crop‌شدهٔ صفحه به‌عنوان شکل کامل استفاده کند.
    "target.flowBounds",
    "figure.canResize",
    "boundedHorizontally",
):
    require(marker in pdf_preview, f"native preview/gesture contract missing: {marker}")
for forbidden in ("WebView", "AndroidView", "evaluateJavascript", "ExamHtml"):
    require(forbidden not in pdf_preview, f"retired web-preview code in native preview: {forbidden}")

pdf_engine = read(PDF_ENGINE)
for marker in (
    "class OfficialPrintLayoutEngine",
    "fun layoutExam(printable: OfficialExamPrintable)",
    "fun layoutExam(printable: OfficialExamPrintable, firstContentTop: Float)",
    "fun drawFlowWindow(",
    "NativeMathCanvasRenderer",
    "FigureSvgRenderer",
    "AtlasBitmapRenderer",
    "PrintPreviewLayoutCodec.decode",
    "InlineFigureMark",
    "imageHeightMm",
    # شکل آزاد ممکن است از slot و page اولیه‌اش پایین‌تر کشیده شده باشد.
    "freeImageIntersects",
    "total = maxOf(total, imageRect.bottom)",
    "separatorQuestionIndex",
):
    require(marker in pdf_engine, f"native PDF engine marker missing: {marker}")
for retired in (
    "WordPageLayout",
    "UnifiedDocumentEngine",
    "layoutExamForEditor",
    "drawEditorPage",
    "editorObjects",
    "ir.exam.app.ui.builder",
    "StyleSpanOps",
):
    require(retired not in pdf_engine, f"retired PDF editor API found: {retired}")

layout_state = read(PDF_LAYOUT_STATE)
for marker in ("NativePrintFigureLayout", "nativePdf", "fun decode", "fun encode", "fun snapshot"):
    require(marker in layout_state, f"native interaction persistence marker missing: {marker}")

image_loader = read(PDF_IMAGES)
for marker in ("PrivateImageLoader", '"https", "content", "file"', "allowHardware(false)"):
    require(marker in image_loader, f"native private-image loading marker missing: {marker}")

header_settings = read(HEADER_SETTINGS)
for marker in ("data class HeaderSchema", "fun HeaderSettingsDialog(", "print/header_settings_schema.json"):
    require(marker in header_settings, f"native header-settings marker missing: {marker}")

# Formula editing is the single WebView exception.  No print/builder source may
# import Android WebKit, and formula.html is the only shipped HTML document.
web_imports = [
    path.relative_to(ROOT).as_posix()
    for path in KOTLIN.rglob("*.kt")
    if "import android.webkit" in read(path)
]
require(
    web_imports == ["app/src/main/java/ir/exam/app/ui/math/FormulaHostDialog.kt"],
    f"unexpected Android WebKit import(s): {', '.join(web_imports) or 'none'}",
)
html_assets = [path.relative_to(ROOT).as_posix() for path in ASSETS.rglob("*.html")]
require(
    html_assets == ["app/src/main/assets/formula_editor/formula.html"],
    f"unexpected HTML asset(s): {', '.join(html_assets) or 'none'}",
)

# Source-wide check for removed print route and old document-editor compatibility.
retired_terms = (
    "ExamHtmlPrint",
    "ExamHtmlImageInliner",
    "HeadlessExamPrinter",
    "createExamPrintWebView",
    "createPrintDocumentAdapter",
    "exam_print_renderer.html",
    "PrintLayoutStore",
    "PrintLayoutMerger",
    "ExamDocumentEditorScreen",
    "WordPageLayout",
    "UnifiedDocumentEngine",
    "layoutExamForEditor",
    "editingDocumentExamId",
    "DOC_EDITOR",
    "questionsOverride",
    "overridePrintLayout",
)
for term in retired_terms:
    offenders = [
        path.relative_to(ROOT).as_posix()
        for path in MAIN.rglob("*.kt")
        if term in read(path)
    ]
    require(not offenders, f"retired source term {term} remains in: {', '.join(offenders)}")

# آزمون فقط یک راه چاپ دارد: dashboard مسیر قدیمی را نگه نمی‌دارد و controller
# فقط برای کارنامه است. Print Center و سازنده هر دو launcher مشترک را صدا می‌زنند.
dashboard = read(KOTLIN / "ir/exam/app/ui/dashboard/TeacherDashboardScreen.kt")
dashboard_state = read(KOTLIN / "ir/exam/app/ui/dashboard/TeacherDashboardViewModel.kt")
print_controller = read(CORE_PRINT / "OfficialPrintController.kt")
print_center = read(PRINT / "ExamPrintCenterScreen.kt")
require("OfficialPrintController" not in dashboard, "dashboard still exposes the retired exam print controller")
require("preparePrint(" not in dashboard_state, "dashboard state still prepares a parallel exam print route")
require("fun printExam" not in print_controller, "OfficialPrintController still renders exams in parallel")
require("NativeExamPrintLauncher.print" in print_center, "Print Center does not use the shared native exam launcher")

# Header schema stays the single source of saved header-field identifiers.
try:
    schema = json.loads(read(SCHEMA))
    ids = [item.get("id") for item in schema.get("templates", [])]
    require(
        ids == ["classic", "formal", "sama", "school", "edu", "detailed-school", "ministry"],
        "header schema template ids changed or are incomplete",
    )
    require(all(template.get("fields") for template in schema.get("templates", [])), "a header template has no fields")
except (json.JSONDecodeError, AttributeError) as exc:
    errors.append(f"invalid header schema: {exc}")

if errors:
    print("Native PDF verification: FAILED")
    for item in errors:
        print(f"- {item}")
    sys.exit(1)

print("Native PDF verification: PASS")
print("- immutable A4 PDF: preview PdfRenderer + Print Framework adapter")
print("- native figure/separator gestures and saved header schema: present")
print("- only FormulaHostDialog/formula.html use WebView/HTML")
