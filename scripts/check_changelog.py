#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""V352.2 — نگهبان «شکل بالاترین بلوک لیست تغییرات».

چرا این فایل هست:
    دو تستِ از پیش موجود، شکل بلوک بالای `text/CHANGELOG_FA.txt` را پین کرده‌اند:
      • app/src/test/java/ir/exam/app/ui/app/V208_ReleaseNotesTest.kt
        → changelogTopBlockHasHeadlineBullets
      • app/src/test/java/ir/exam/app/V322_MathToolsRobustnessTest.kt
        → changelogTopBlockHasNoInternalVersionNumbers
    در V352.1 یک تیتر ۲۲۵ نویسه‌ای نوشته شد (سقف ۲۰۰) و هر دو تست در CI شکستند،
    آن هم پس از ۱۰ دقیقه ساخت. هیچ نگهبان محلی‌ای این را نمی‌گرفت.

این اسکریپت همان قاعده‌ها را در چند میلی‌ثانیه می‌سنجد.

نکتهٔ مهم در آینه‌سازی:
    موتور جاوا/کاتلین به‌صورت پیش‌فرض ASCII است: `\\w` و `\\s` و `\\d` فقط ASCII
    را می‌شناسند. پایتون به‌صورت پیش‌فرض یونیکد است و حرف فارسی را «واژه» می‌داند.
    بدون پرچم `re.ASCII`، مرزِ `\\bV\\d` در متن فارسی متفاوت عمل می‌کند و آینه
    نادرست می‌شود. پس همهٔ الگوها با `re.ASCII` ساخته شده‌اند.

اجرا:  python3 scripts/check_changelog.py
"""
import pathlib
import re
import sys

MAX_BULLETS = 6
MAX_BULLET_LEN = 200
CHANGELOG = "text/CHANGELOG_FA.txt"

A = re.ASCII
HEADER = re.compile(r"^(V\d+(?:\.\d+)*)\s*(?:\(([^)]*)\))?\s*(?:[—\-–:]\s*(.*))?$", A)
APP_VERSION = re.compile(r"\d+\.\d+(?:\.\d+)*", A)
INTERNAL_VERSION = re.compile(r"\bV\d+(?:\.\d+)*\b", A)
EMPTY_PARENS = re.compile(r"\(\s*\)", A)
MULTI_SPACE = re.compile(r"\s{2,}", A)
TRAILING_SCOPE = re.compile(
    r"[\s،;؛.]*(?:\(?\s*(?:اپ و سایت|سایت و اپ|اپ \+ سایت|فقط اپ|فقط در اپ)\s*\)?)[\s.]*$", A
)


def scope_includes_app(scope):
    s = scope.strip()
    if not s:
        return True
    if s.startswith("فقط") and "اپ" not in s:
        return False
    return "اپ" in s


def clean_bullet(raw):
    """آینهٔ ReleaseNotes.cleanBullet — ترتیب مرحله‌ها عیناً حفظ شده است."""
    t = raw.replace("`", "").strip()
    t = INTERNAL_VERSION.sub("", t)
    t = EMPTY_PARENS.sub("", t)
    t = MULTI_SPACE.sub(" ", t).strip()
    t = TRAILING_SCOPE.sub("", t).strip()
    return t.rstrip("،;؛").strip()


def split_summary(summary):
    """آینهٔ ReleaseNotes.splitSummary — شکستن روی «؛» و کوتاه‌سازی به ۹۰."""
    out = []
    for part in summary.split("؛"):
        p = part.strip().rstrip(".،")
        if not p:
            continue
        out.append(p[:88].rstrip() + "…" if len(p) > 90 else p)
    return out


def parse(lines):
    """آینهٔ ReleaseNotes.parse — فقط چیزهایی که تست‌ها می‌سنجند."""
    blocks = []
    state = {"version": None, "scope": "", "summary": "", "bullets": []}

    def flush():
        if state["version"] is None:
            return
        raw = state["bullets"] if state["bullets"] else split_summary(state["summary"])
        cleaned = [c for c in (clean_bullet(b) for b in raw) if c]
        blocks.append({
            "version": state["version"],
            "summary": state["summary"],
            "bullets": cleaned[:MAX_BULLETS],
            "scope": state["scope"],
            "appAffecting": scope_includes_app(state["scope"]),
        })

    for raw in lines:
        line = raw.strip().replace("`", "")
        if not line:
            continue
        m = HEADER.search(line)
        if m:
            flush()
            state = {
                "version": m.group(1),
                "scope": (m.group(2) or "").strip(),
                "summary": (m.group(3) or "").strip(),
                "bullets": [],
            }
        elif line.startswith("-") or line.startswith("•"):
            state["bullets"].append(line.lstrip("-•").strip())
        elif state["version"] is not None:
            state["summary"] = (state["summary"] + " " + line).strip()
        else:
            blocks.append({
                "version": "", "summary": line, "bullets": [clean_bullet(line)],
                "scope": "", "appAffecting": True,
            })
    flush()
    return blocks


def repo_root():
    here = pathlib.Path(__file__).resolve().parent
    for cand in [here] + list(here.parents):
        if (cand / "settings.gradle.kts").is_file():
            return cand
    return here.parent


def check_text(text):
    """قاعده‌های هر دو تست را روی متن برمی‌گرداند: (خطاها، اطلاعات)."""
    problems = []
    lines = text.split("\n")
    if not lines or not lines[0].strip():
        return ["لیست تغییرات خالی است"], {}
    if not lines[0].strip().startswith("V"):
        problems.append("خط اول باید عنوان نسخه باشد و با V شروع شود")
        return problems, {}

    # V322 روی ۶۰ سطر اول و V208 روی ۴۰ سطر اول تجزیه می‌کند؛ هر دو باید پاس شوند.
    for take in (40, 60):
        blocks = parse(lines[:take])
        if not blocks:
            problems.append("هیچ بلوک نسخه‌ای در %d سطر اول نیست" % take)
            continue
        top = blocks[0]
        tag = "%d سطر" % take
        if not top["appAffecting"]:
            problems.append("[%s] بلوک بالا مربوط به اپ نیست (دامنه: %r)" % (tag, top["scope"]))
        n = len(top["bullets"])
        if n < 1:
            problems.append("[%s] بلوک بالا هیچ تیتری ندارد" % tag)
        if n > MAX_BULLETS:
            problems.append("[%s] %d تیتر بیش از سقف %d است" % (tag, n, MAX_BULLETS))
        for b in top["bullets"]:
            if len(b) > MAX_BULLET_LEN:
                problems.append("[%s] تیتر %d نویسه است (سقف %d): %s…" % (tag, len(b), MAX_BULLET_LEN, b[:60]))
            if re.search(r"\bV\d", b, A):
                problems.append("[%s] شمارهٔ نسخهٔ داخلی در تیتر: %s" % (tag, b[:60]))
    top = parse(lines[:60])[0] if parse(lines[:60]) else {}
    return problems, top


def selftest():
    """آزمون منفی/مثبت نگهبان روی متن‌های ساختگی — تا خودِ نگهبان بی‌آزمون نماند."""
    good = "V900 (اپ 1.09.00) — عنوان\n- تیتر کوتاه یک\n- تیتر کوتاه دو\n\nV899 (اپ 1.08.99) — قبلی\n- مورد\n"
    cases = [
        ("سالم", good, True),
        ("تیتر بلند", good.replace("تیتر کوتاه یک", "ب" * 201), False),
        ("تیتر ۲۰۰ نویسه (مرز)", good.replace("تیتر کوتاه یک", "ب" * 200), True),
        ("بدون تیتر", "V900 (اپ 1.09.00) — \n\nV899 (اپ 1.08.99) — قبلی\n- مورد\n", False),
        ("فقط سایت", good.replace("(اپ 1.09.00)", "(فقط سایت)"), False),
        ("شمارهٔ داخلی", good.replace("تیتر کوتاه یک", "بهبود " + "V" + "901 در کد"), True),
        ("خط اول عنوان نیست", "سلام\n- تیتر\n", False),
        ("خالی", "", False),
        ("هفت تیتر → بُرش به ۶", "V900 (اپ 1.09.00) — عنوان\n" + "".join("- مورد %d\n" % i for i in range(7)), True),
    ]
    failed = []
    for name, text, should_pass in cases:
        problems, _ = check_text(text)
        passed = not problems
        ok = passed == should_pass
        print("  [%s] %s → %s" % ("OK" if ok else "FAIL", name, "قبول" if passed else "رد"))
        if not ok:
            failed.append(name)
    # دو حالت که عمدتاً «قبول»اند و نباید اشتباهاً رد شوند:
    # • شمارهٔ داخلی: cleanBullet آن را حذف می‌کند، پس هرگز به کاربر نمی‌رسد.
    # • بیش از ۶ تیتر: خودِ parse با take(6) بُرش می‌زند، پس تست‌ها ۶ می‌بینند و پاس می‌شوند.
    #   یعنی سقف تعداد در عمل توسط پارسر تضمین می‌شود، نه توسط این نگهبان.
    return failed


def main():
    if "--selftest" in sys.argv:
        failed = selftest()
        print("CHANGELOG_GUARD_SELFTEST: " + ("PASS" if not failed else "FAIL " + str(failed)))
        return 1 if failed else 0
    root = repo_root()
    path = root / CHANGELOG
    if not path.is_file():
        print("❌ فایل یافت نشد: %s" % CHANGELOG)
        return 1
    problems, top = check_text(path.read_text(encoding="utf-8"))
    if problems:
        print("❌ check_changelog: شکل بلوک بالای لیست تغییرات درست نیست")
        for p in problems:
            print("   • " + p)
        print("\nراهنما: هر تیتر با «- » شروع شود، حداکثر %d تیتر، هر تیتر حداکثر %d نویسه،"
              % (MAX_BULLETS, MAX_BULLET_LEN))
        print("       و شمارهٔ داخلی نسخه (مثل V352) داخل تیتر نوشته نشود.")
        return 1
    lengths = ", ".join(str(len(b)) for b in top.get("bullets", []))
    print("check_changelog: سالم — %d تیتر (نویسه: %s)، دامنه %r"
          % (len(top.get("bullets", [])), lengths, top.get("scope", "")))
    return 0


if __name__ == "__main__":
    sys.exit(main())
