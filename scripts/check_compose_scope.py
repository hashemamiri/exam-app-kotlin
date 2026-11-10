#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""V360.5 — نگهبان دامنهٔ Compose در ExamApp.kt.

درس V360.3: پنجرهٔ بانک مرجع در `AuthenticatedShell` گذاشته شد، در حالی
که متغیرهای وضعیت (`referenceBankOpen`، `importedExam`، `page`، …) در
`AuthenticatedExamApp` تعریف شدهاند و `quickAddOpen` در پوسته یک `val`
پارامتر است. نگهبانهای محلی این را نگرفتند چون فایلهای Compose را
کامپایل نمیکنند؛ `compileDebugKotlin` در CI با «val cannot be
reassigned» و «Unresolved reference» شکست.

این نگهبان همان خطا را پیش از Gradle میگیرد:
  • پوسته نباید به متغیرهای وضعیت والد دست بزند؛
  • ExamApp نباید میزبان پنجرهٔ بانک مرجع باشد؛
  • دکمهٔ + اپ همچنان چهار کارت داشته باشد و بانک مرجع در آن نباشد؛
  • منوی + سایت هم بانک مرجع نداشته باشد.

اجرا:  python3 scripts/check_compose_scope.py
"""
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
APP = ROOT / "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt"
OVERLAY = ROOT / "app/src/main/java/ir/exam/app/ui/app/Design69QuickAddOverlay.kt"
MOBILE = ROOT / "site/src/mobile.js"
DESKTOP = ROOT / "site/src/app.js"

fails = []


def ok(name, cond, extra=""):
    print(("  ✓ " if cond else "  ✗ ") + name + ((" — " + str(extra)[:200]) if extra and not cond else ""))
    if not cond:
        fails.append(name)


def read(path):
    try:
        return path.read_text(encoding="utf-8")
    except OSError as exc:
        print("فایل خوانده نشد:", path, exc)
        return ""


def main() -> int:
    app = read(APP)
    ok("ExamApp.kt پیدا شد", bool(app))
    if not app:
        return 1

    start = app.find("private fun AuthenticatedShell(")
    ok("AuthenticatedShell پیدا شد", start >= 0)
    end = app.find("\nprivate fun MainPage.sectionTitle", start)
    if end < 0:
        end = len(app)
    shell = app[start:end] if start >= 0 else ""

    print("=== دامنهٔ پوسته ===")
    for needle in (
        "referenceBankOpen",
        "editingExamId =",
        "importedExam =",
        "builderCameFromPrint =",
        "quickAddOpen = false",
        "ReferenceBankHost",
    ):
        ok("پوسته نباید «" + needle + "» داشته باشد", needle not in shell)

    print("\n=== ExamApp ===")
    ok("پنجرهٔ بانک مرجع به والد منتقل شده یا حذف شده", "ReferenceBankHost" not in app)
    ok("منوی + چهار عمل قدیمی را صدا میزند", "Design69QuickAddOverlay(" in app)

    print("\n=== دکمهٔ + اپ ===")
    overlay = read(OVERLAY)
    ok("چهار کارت گوشه برقرار است", "const val ACTION_COUNT = 4" in overlay)
    ok("بانک مرجع در + اپ نیست", "بانک مرجع" not in overlay)

    print("\n=== دکمهٔ + سایت ===")
    mobile = read(MOBILE)
    desktop = read(DESKTOP)
    ok("بانک مرجع در + گوشی نیست", "'بانک مرجع'" not in mobile)
    ok("بانک مرجع در + دسکتاپ نیست", "'بانک مرجع': ['refbank'" not in desktop)
    ok("پنج مورد دسکتاپ برقرار است", "quickAddItems({withPrint: true})" in desktop)

    print()
    if fails:
        print("COMPOSE_SCOPE: FAIL (%d مورد)" % len(fails))
        for f in fails:
            print("  ✗ " + f)
        return 1
    print("COMPOSE_SCOPE: PASS — دامنهٔ وضعیتها و منوی + سالم است")
    return 0


if __name__ == "__main__":
    sys.exit(main())
