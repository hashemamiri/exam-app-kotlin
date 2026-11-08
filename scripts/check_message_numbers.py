#!/usr/bin/env python3
"""V356 — نگهبان رقم فارسی در پیام‌های کاربر.

چرا این نگهبان لازم شد: در چند نسخه، رقم فارسی داخل پیام‌ها هنگام نوشتن
می‌افتاد («۱۲» به‌جای «۲۰»، «۰۰» به‌جای «۲۰۰»). چون ترمینال هم این
رقم‌ها را گاهی ناقص نشان می‌دهد، خطا با چشم دیده نمی‌شد. این نگهبان
رقم‌ها را از **کدپوینت** می‌خواند و با کرانهٔ اعلام‌شده مقایسه می‌کند.

قاعده: هر پیامی که کلیدواژهٔ یکی از ردیف‌های جدول زیر را دارد، باید
دنبالهٔ رقم‌های فارسی‌اش دقیقاً برابر «انتظار» باشد.
"""
from __future__ import annotations

import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
FA_OFFSET = 0x06F0
FA = "".join(chr(FA_OFFSET + i) for i in range(10))
AR = "".join(chr(0x0660 + i) for i in range(10))
DIGIT = {ord(FA[i]): str(i) for i in range(10)}
DIGIT.update({ord(AR[i]): str(i) for i in range(10)})


def numeral_runs(text: str) -> list[str]:
    """دنباله‌های رقم فارسی/عربی را جدا جدا برمی‌گرداند (نه چسبیده)."""
    out: list[str] = []
    cur = ""
    for ch in text:
        if ord(ch) in DIGIT:
            cur += DIGIT[ord(ch)]
        elif cur:
            out.append(cur)
            cur = ""
    if cur:
        out.append(cur)
    return out


# (مسیر، کلیدواژهٔ پیام، دنبالهٔ مورد انتظار)
RULES: list[tuple[str, str, list[str]]] = [
    ("supabase/sql/SQL_GRADING_SIMILARITY_V355.sql", "هر کلیدواژه باید بین", ["1", "120"]),
    ("supabase/sql/SQL_GRADING_SIMILARITY_V355.sql", "بیش از ۶۰ کلیدواژه", ["60"]),
    ("supabase/sql/SQL_GRADING_SIMILARITY_V355.sql", "برای تأیید تخلف، توضیح دست‌کم", ["3"]),
    ("supabase/sql/SQL_CODE_GRADING_V356.sql", "برچسب مورد آزمون بلندتر از", ["120"]),
    ("supabase/sql/SQL_CODE_GRADING_V356.sql", "برای هر سؤال بیش از ۴۰ مورد", ["40"]),
    ("supabase/sql/SQL_GRADING_APPEALS_V354.sql", "دلیل اعتراض را کامل‌تر بنویسید", ["5"]),
    ("site/src/codegrade.js", "برچسب بلندتر از", ["120"]),
    ("site/src/codegrade.js", "بیش از ۴۰ مورد آزمون", ["40"]),
    ("app/src/main/java/ir/exam/app/domain/grading/CodeGrading.kt", "برچسب بلندتر از", ["120"]),
    ("app/src/main/java/ir/exam/app/domain/grading/CodeGrading.kt", "بیش از ۴۰ مورد آزمون", ["40"]),
    ("app/src/main/java/ir/exam/app/data/repository/SupabaseGradingRepository.kt", "هر کلیدواژه باید بین", ["1", "120"]),
    ("app/src/main/java/ir/exam/app/data/repository/SupabaseGradingRepository.kt", "برای تأیید تخلف، توضیح دست‌کم", ["3"]),
    ("app/src/main/java/ir/exam/app/domain/model/GradeAppealModels.kt", "دلیل اعتراض را کامل‌تر بنویسید", []),
    ("scripts/mock_supabase_server.py", "متن اعتراض باید بین", ["5", "2000"]),
    ("scripts/mock_supabase_server.py", "هر کلیدواژه باید بین", ["1", "120"]),
    ("scripts/mock_supabase_server.py", "برچسب مورد آزمون بلندتر از", ["120"]),
]


def main() -> int:
    problems: list[str] = []
    checked = 0
    for rel, key, want in RULES:
        path = ROOT / rel
        if not path.exists():
            problems.append(f"فایل پیدا نشد: {rel}")
            continue
        lines = [l for l in path.read_text(encoding="utf-8").split("\n") if key in l]
        if not lines:
            problems.append(f"پیام پیدا نشد: {rel} :: {key}")
            continue
        for line in lines:
            got = numeral_runs(line)
            checked += 1
            # پیام‌هایی که رقم را از ثابت می‌سازند ([]) باید رقم ثابت نداشته باشند
            if got != want:
                problems.append(f"{rel} :: {key}\n    رقم‌ها {got} ولی انتظار {want}")
    if problems:
        print("check_message_numbers: FAIL")
        for p in problems:
            print("  ✗ " + p)
        return 1
    print(f"check_message_numbers: سالم — {checked} پیام بررسی شد")
    return 0


if __name__ == "__main__":
    sys.exit(main())
