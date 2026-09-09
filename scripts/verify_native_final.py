#!/usr/bin/env python3
"""Fast, offline guardrails for the current exam print renderer.

The retired document editor and print-layout compatibility store deliberately do
not belong to this architecture.  This check keeps CI focused on the active
renderer, preview, direct-print and header contracts instead of historical
source snapshots.
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "app/src/main"
PRINT_ASSETS = MAIN / "assets/print"
RENDERER = PRINT_ASSETS / "exam_print_renderer.html"
DIALOG = MAIN / "java/ir/exam/app/ui/printing/ExamHtmlPrintDialog.kt"
NATIVE_MEDIA = MAIN / "java/ir/exam/app/ui/printing/ExamPrintAssetRenderer.kt"
HEADER_SETTINGS = MAIN / "java/ir/exam/app/ui/printing/PrintHeaderSettings.kt"
PDF_ENGINE = MAIN / "java/ir/exam/app/core/printing/OfficialPdfPrintAdapter.kt"
PDF_FIGURE_METRICS = MAIN / "java/ir/exam/app/core/printing/PrintFigureMetrics.kt"
PDF_TEXT_SPANS = MAIN / "java/ir/exam/app/core/printing/PrintTextSpanSegments.kt"
SCHEMA = PRINT_ASSETS / "header_settings_schema.json"

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


# Retired files must not silently return.
for relative in (
    "app/src/main/java/ir/exam/app/ui/printing/ExamDocumentEditorScreen.kt",
    "app/src/main/java/ir/exam/app/core/printing/WordPageLayout.kt",
    "app/src/main/java/ir/exam/app/data/local/PrintLayoutStore.kt",
    "app/src/main/java/ir/exam/app/ui/printing/ExamBuilder30Windows.kt",
    "app/src/main/assets/print/exam_print.html",
    "app/src/main/assets/print/math_editor.html",
):
    require(not (ROOT / relative).exists(), f"retired file returned: {relative}")

require(RENDERER.is_file(), "renderer asset is missing")
# V125 — سندِ اصلی اکنون میزبانِ نازکِ موتورِ وبِ «آزمون‌ساز v20» است؛ موتور (CSS/JS
# عیناً از نسخهٔ وب) در print/web/ و لایهٔ میزبانِ برنامه در print/web/webhost.js است.
WEB_ENGINE = PRINT_ASSETS / "web"
WEBHOST = WEB_ENGINE / "webhost.js"
WEB_ENGINE_FILES = (
    "vazirmatn_embed.css", "main.css", "editor_styles.css", "tools_styles.css", "qmf_styles.css", "ui_styles.css",
    "pgs_style.css", "webhost.css", "host_dom.js", "geo_fig.js", "graph_fig.js", "table_fig.js", "anatomy_atlas_data.js",
    "anatomy_fig.js", "periodic_fig.js", "science_atlas_data.js", "science_fig.js", "math_host.js", "mainscript.js",
    "ui_v2_runtime.js", "qimg_uploader.js", "pgs_engine.js", "webhost.js",
)
if RENDERER.is_file():
    renderer = read(RENDERER)
    require(RENDERER.stat().st_size < 8_000, "renderer entry must stay a thin host (engine lives in print/web/)")
    for marker in (
        "window.__appHost = true;",
        'src="web/host_dom.js"',
        'src="web/mainscript.js"',
        'src="web/pgs_engine.js"',
        'src="web/webhost.js"',
        'href="web/pgs_style.css"',
    ):
        require(marker in renderer, f"renderer host marker missing: {marker}")
    external_urls = re.findall(r"https?://([^/'\"\s<]+)", renderer, flags=re.I)
    if external_urls:
        errors.append(f"renderer has an external URL: {external_urls[0]}")
    for name in WEB_ENGINE_FILES:
        require((WEB_ENGINE / name).is_file(), f"web engine file missing: print/web/{name}")
    engine = read(WEB_ENGINE / "pgs_engine.js") + read(WEB_ENGINE / "mainscript.js")
    for marker in ("function renderPreview()", "function paginate()", "function buildHeader()", "classList.add('pgs-fallback')", "function rebuildPrintRoot()"):
        require(marker in engine or marker in read(WEB_ENGINE / "pgs_engine.js"), f"web engine marker missing: {marker}")
    for name in WEB_ENGINE_FILES:
        body = read(WEB_ENGINE / name)
        found = re.findall(r"https?://([^/'\"\s<)]+)", body, flags=re.I)
        found = [h for h in found if h.lower() != "www.w3.org"]
        require(not found, f"web engine file has an external URL: {name}: {found[:1]}")
        require("cdn-cgi" not in body, f"Cloudflare challenge script leaked into {name}")
    require("qmf_exam_autosave" not in engine, "web autosave/recovery banner must not be bundled")
    # V125 — اطلس‌های آناتومی/علوم به‌جای base64ِ نسخهٔ وب (~3MB) به JPEGهای خودِ برنامه اشاره می‌کنند.
    for name, key, prefix in (("anatomy_atlas_data.js", "window.ATLAS={", "/figure_atlas/anatomy/atlas-"),
                              ("science_atlas_data.js", "window.SCIENCE_ATLAS={", "/figure_atlas/science/")):
        atlas = read(WEB_ENGINE / name)
        require(key in atlas and prefix in atlas, f"{name} must map ids to app figure_atlas files")
        require("data:image" not in atlas, f"{name} must not embed base64 images (use assets/figure_atlas)")
        require((WEB_ENGINE / name).stat().st_size < 20_000, f"{name} is unexpectedly large")
        for ref in re.findall(r"'(/figure_atlas/[^']+)'", atlas):
            require((MAIN / "assets" / ref.lstrip("/")).is_file(), f"atlas file missing: {ref}")
    webhost = read(WEBHOST)
    for marker in (
        "window.setExamData = setExamData;",
        "window.printStudent = function",
        "window.printTeacher = function",
        "window.ExamPrintRenderer = {",
        "showPreview: showPreview, layoutSnapshot: snapshot, figureAt: figureAt, replaceFigure: replaceFigure,",
        "restorePreview: restorePreview, setPageSetup: setPageSetup, getPageSetup: getPageSetup",
        "function requestPrint(mode, opts)",
        "callBridge('print', mode)",
        "callBridge('previewClosed')",
        "callBridge('editFigureTool', String(qid), index)",
        "ev.__appHost = true; window.dispatchEvent(ev)",
        "e.stopImmediatePropagation(); document.body.classList.add('pgs-fallback')",
        "window.openPreviewWindow()",
        "function toWebQuestion(src, index)",
        "if (!window.__appHost) loadSample();",
    ):
        require(marker in webhost or marker in read(WEB_ENGINE / "mainscript.js"), f"web host marker missing: {marker}")
    for forbidden in ("localstorage", "innerhtml", "<iframe", "document.write"):
        require(forbidden not in webhost.lower(), f"forbidden construct in webhost.js: {forbidden}")
    # V126 — رندرر قبلی (V105–V124) و پنجره‌های بومیِ «کادر»/«تنظیمات صفحه»/هدرِ قالب‌بندی کاملاً حذف شدند.
    require(not (PRINT_ASSETS / "exam_print_renderer_legacy.html").exists(), "legacy renderer must be deleted")
    for retired_file in ("app/src/main/java/ir/exam/app/data/local/PrintBoxStyleStore.kt", "app/src/main/java/ir/exam/app/ui/printing/PrintBoxSettingsDialog.kt"):
        require(not (ROOT / retired_file).exists(), f"retired print file still present: {retired_file}")
    require("callBridge('pageSetupChanged', JSON.stringify(readPageSetup()))" in webhost, "webhost must report page-setup changes")
    for retired in ("applyBoxStyle", "applyFormat", "boxStyle"):
        require(retired not in webhost, f"retired host API still in webhost.js: {retired}")

# Kotlin host must point at the new asset and retain the active bridge surface.
dialog = read(DIALOG)
# V126 — هدر/نوارِ قالب‌بندیِ بومی، «کادر» و پنجرهٔ بومیِ «تنظیمات صفحه» حذف شدند؛ تنظیمات صفحه از پنلِ 📐 وب می‌آید.
for retired in ("PrintPreviewHeader", "PrintBoxStyleStore", "PrintBoxSettingsDialog", "applyFormat", "applyBoxStyle", "WEB_ENGINE_PREVIEW", "PrintPageSetupDialog"):
    require(retired not in dialog, f"retired native preview UI still referenced: {retired}")
require("fun pageSetupChanged(json: String?)" in dialog, "web page-setup panel must persist through the bridge")
# V127 — پنل‌های fixedِ موتورِ وب با overviewMode بریده می‌شدند؛ بازه/نسخه در دیالوگِ چاپِ وب.
require("settings.loadWithOverviewMode = false" in dialog, "web engine needs the real device viewport (no overview mode)")
require("function applyRangeAndCopies(opts)" in read(WEBHOST), "print dialog range/copies must be applied to #pgsPrintRoot")
# V128 — نوارِ قالب‌بندیِ متنِ انتخاب‌شده (B/I/U/رنگ/اندازه/فونت) در لایهٔ میزبان + دستگیرهٔ جداکننده دیدنی
_wj = read(WEBHOST)
for marker in ("function installRichTextOverride()", "function ensureFmtBar()", "formatSelection: formatSelection", "spans: (q.__spans || []).map("):
    require(marker in _wj, f"V128 web host marker missing: {marker}")
require("#pgsViewer #previewArea .question-sep-drag{display:flex !important" in read(WEB_ENGINE / "webhost.css"), "separator drag handle must be visible inside the PGS viewer")
# V129 — دلیمترهای SVGِ ویرایشگر در پیش‌نمایش/چاپ، انتخابِ پابرجا، لمسِ اول=انتخاب/دوم=ویرایش، دستگیره‌ها دیدنی، اندازهٔ ۱..۱۰۰
for marker in ("function installDelimOverride()", "P.makeDelimWrap = function (o, c, inner)", "function restoreSelection()", "wasSelected: fig.classList.contains('selected')", "(n >= 1 && n <= 100)"):
    require(marker in _wj, f"V129 web host marker missing: {marker}")
require("#pgsViewer #previewArea .interactive-figure.selected .fig-resize-handle{display:block !important;}" in read(WEB_ENGINE / "webhost.css"), "figure resize handles must be visible inside the PGS viewer")
require(".mdelim-x{display:flex !important" in read(WEB_ENGINE / "webhost.css"), "editor-identical SVG delimiter CSS must be present in the host layer")
require("var pendingDelete by remember" in read(MAIN / "java/ir/exam/app/ui/printing/ExamPrintCenterScreen.kt"), "deleting a print exam must ask for confirmation")
require("it in 1..100" in read(MAIN / "java/ir/exam/app/ui/builder/ExamBuilderViewModel.kt") and "it in 1..100" in read(MAIN / "java/ir/exam/app/data/repository/ExamQuestionCodec.kt"), "span font size range must be 1..100")
require("groups.reversed()" not in read(MAIN / "java/ir/exam/app/ui/figure/PeriodicEditorDialog.kt") and "PAD + LABEL + ci * step" in read(MAIN / "java/ir/exam/app/core/figure/PeriodicSvgRenderer.kt"), "periodic table must be standard LTR (H at left) in editor and native renderer, like the web preview")
# V130 — «همه» در نوار قالب‌بندی، انتخابگرهای بومی اندازه/فونت، پرانتز هلالی مجزا، $ پنهان، پنجره‌های تمام‌صفحه، رنگ کارت‌ها
for marker in ('id="hfAll"', "function patchSpans(q, s, e, patch)", "b.pickSize(", "b.pickFont("):
    require(marker in _wj, f"V130 web host marker missing: {marker}")
require("hf-hint" not in _wj, "the 'select question text' hint must be gone from the formatting bar")
require("fun pickSize(current: String?)" in dialog and "internal fun PreviewFormatPickerDialog(" in dialog and "GridCells.Fixed(5)" in dialog, "native size/font pickers must be wired through the bridge (5-column size grid)")
require('"⟮" -> "paren" to false' in read(MAIN / "java/ir/exam/app/core/math/NativeMathSvgRenderer.kt"), "⟮⟯ must render with the editor's paren shape, distinct from ( )")
require("visualTransformation = DollarHidingTransformation" in read(MAIN / "java/ir/exam/app/ui/builder/QuestionTextWebSection.kt"), "$ must never be visible in the question text box")
for f in ("TableEditorDialog.kt", "PeriodicEditorDialog.kt"):
    _t = read(MAIN / "java/ir/exam/app/ui/figure" / f)
    require("import androidx.compose.material3.AlertDialog" not in _t and "usePlatformDefaultWidth = false" in _t, f"{f} must be a full-screen dialog like FigurePickerDialog")
require("fun cardLevel(done: Int, total: Int): CardLevel" in read(MAIN / "java/ir/exam/app/ui/grading/GradingViewModel.kt") and "state.cardStats[item.id]" in read(MAIN / "java/ir/exam/app/ui/grading/GradingScreen.kt"), "grading cards must be colour-coded by answer/grading progress")
# V130.1 — متنِ سؤالِ بلندتر از صفحه باید به صفحهٔ بعد ادامه یابد (نه clip)
_pe = read(WEB_ENGINE / "pgs_engine.js")
require("function richCanSplit(k)" in _pe and "function splitRichPiece(k, tr2main)" in _pe and "if (!canSplitLines && !richCanSplit(only)) break;" in _pe, "oversize question text must be split across pages")
# V127.1 — پنل 📐 نباید به vh/dvh وابسته باشد (در WebView یک‌سطری می‌شد): top/bottom مطلق در webhost.css
_wc = read(WEB_ENGINE / "webhost.css")
require("#pgsPageSetup{" in _wc and "top:var(--host-top,60px) !important;bottom:0 !important" in _wc and "max-height:none !important" in _wc, "page-setup panel must be pinned between ribbon and bottom without vh units")
require("نام و امضای دبیر:" not in read(MAIN / "java/ir/exam/app/domain/model/OfficialPrintModels.kt"), "teacher/principal signature footer must stay removed")
require("PrintPageSetup.fromJson(json)" in dialog, "page setup JSON from the web panel must be parsed")
for marker in (
    'MAIN_PAGE_URL = "https://exam-print.local/print/exam_print_renderer.html"',
    '"ExamPrintBridge"',
    "fun renderFormula",
    "fun renderFigure",
    "ExamPrintRenderer.layoutSnapshot",
    "ExamPrintRenderer.showPreview",
    "ExamPrintRenderer.replaceFigure",
    "internal class HeadlessExamPrinter",
    "webView = configuredWebView",
):
    require(marker in dialog, f"print host marker missing: {marker}")
for forbidden in ("ExamPrintNative", "__qmf", "qmf-print-mode", "math_editor.html", "exam_print.html"):
    require(forbidden not in dialog, f"retired print host marker found: {forbidden}")

native_media = read(NATIVE_MEDIA)
for marker in ("NativeMathSvgRenderer", "FigureSvgRenderer", "AtlasBitmapRenderer", "data:image/svg+xml;base64"):
    require(marker in native_media, f"native media renderer marker missing: {marker}")

header_settings = read(HEADER_SETTINGS)
for marker in ("data class HeaderSchema", "fun HeaderSettingsDialog(", "print/header_settings_schema.json"):
    require(marker in header_settings, f"native header-settings marker missing: {marker}")

# The native PDF route is still used for official output and must keep its A4,
# formula and figure capabilities independently of the retired editor engine.
pdf_engine = read(PDF_ENGINE)
for marker in (
    "class OfficialPrintLayoutEngine",
    "fun layoutExam(printable: OfficialExamPrintable)",
    "fun layoutReport(report: OfficialGradeReportPrintable)",
    "fun drawFlowWindow(",
    "NativeMathCanvasRenderer",
    "FigureSvgRenderer",
    "AtlasBitmapRenderer",
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

pdf_figure_metrics = read(PDF_FIGURE_METRICS)
for marker in ("object PrintFigureMetrics", "fun figureWidthMm", "fun figurePosMm", "coerceIn(MIN_WIDTH_MM, MAX_WIDTH_MM)"):
    require(marker in pdf_figure_metrics, f"native PDF figure metric marker missing: {marker}")

pdf_text_spans = read(PDF_TEXT_SPANS)
for marker in ("object PrintTextSpanSegments", "fun split(", "PrintTextSpan"):
    require(marker in pdf_text_spans, f"native PDF text-span marker missing: {marker}")

# Source-wide check for the removed route and compatibility layer.
retired_terms = (
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

# Header schema stays the single source of field ids for active print settings.
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
    print("Print renderer verification: FAILED")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print("Print renderer verification: PASS")
print(f"- renderer host: {RENDERER.stat().st_size} bytes; web engine files: {len(WEB_ENGINE_FILES)}")
print("- retired document-editor and layout-store paths: absent")
print("- preview, direct print, native math/figure rendering and header schema: present")
