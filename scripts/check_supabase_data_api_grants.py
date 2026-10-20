#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""V324 — نگهبان «گرنت Data API» (تغییر ۳۰ اکتبر Supabase).

از ۳۰ اکتبر، جدول تازه در schema عمومی بدون GRANT صریح برای anon / authenticated /
service_role از PostgREST و GraphQL در دسترس نیست. این اسکریپت جلوی تکرار خطا را می‌گیرد:

  ۱) هر `create table public.X` در مهاجرت‌ها باید GRANT صریح داشته باشد:
       - anon  → select
       - authenticated → select, insert, update, delete
       - service_role  → all
  ۲) جدول‌های schemaهای دیگر (مثل private.push_config) نباید به anon/authenticated گرنت بگیرند.
  ۳) باید یک مهاجرت با `alter default privileges ... in schema public` وجود داشته باشد تا
     جدول‌های آینده (و پروژهٔ تازه / db reset) هم در دسترس بمانند.

کاربرد:  python3 scripts/check_supabase_data_api_grants.py [--quiet]
خروج: کد ۰ = سالم، ۱ = ایراد.
"""
import glob
import os
import re
import sys

MIGRATIONS = sorted(glob.glob(os.path.join("supabase", "migrations", "*.sql")))
EXTRA_SQL = sorted(glob.glob(os.path.join("sql", "**", "*.sql"), recursive=True))
SERVICE_ONLY = set()  # جدول‌های عمومی که آگاهانه فقط service_role می‌گیرند

CREATE = re.compile(
    r"create\s+table\s+(?:if\s+not\s+exists\s+)?(?:(public|private|auth|storage)\s*\.\s*)?([a-zA-Z0-9_]+)",
    re.I,
)
GRANT = re.compile(
    r"grant\s+([a-z, \t\r\n]+?)\s+on\s+(?:table\s+)?(?:(public|private|auth|storage)\s*\.\s*)?([a-zA-Z0-9_]+)\s+to\s+([a-z_,\s]+)",
    re.I,
)
DEFAULT_PRIV = re.compile(r"alter\s+default\s+privileges[\s\S]{0,200}?in\s+schema\s+public", re.I)
# «grant ... on tables/sequences/functions» مربوط به DEFAULT PRIVILEGES است، نه یک جدول مشخص
NOT_A_TABLE = {"tables", "sequences", "functions", "routines", "all", "schema", "database"}


def strip_comments(sql: str) -> str:
    sql = re.sub(r"/\*[\s\S]*?\*/", " ", sql)
    return re.sub(r"--[^\n]*", " ", sql)


def main() -> int:
    quiet = "--quiet" in sys.argv
    files = MIGRATIONS + EXTRA_SQL
    if not MIGRATIONS:
        print("check_data_api_grants: پوشهٔ supabase/migrations پیدا نشد")
        return 1

    public_tables = {}   # table -> file that creates it
    other_tables = {}    # schema.table -> file
    granted = {}         # table -> {(privs, role)}
    has_default = False

    for path in files:
        raw = open(path, encoding="utf-8", errors="replace").read()
        sql = strip_comments(raw)
        low = sql.lower()
        if DEFAULT_PRIV.search(sql):
            has_default = True
        for m in CREATE.finditer(sql):
            schema = (m.group(1) or "public").lower()
            table = m.group(2).lower()
            if schema == "public":
                public_tables.setdefault(table, path)
            else:
                other_tables.setdefault(schema + "." + table, path)
        for m in GRANT.finditer(sql):
            privs = " ".join(m.group(1).lower().split())
            schema = (m.group(2) or "public").lower()
            table = m.group(3).lower()
            roles = {r.strip().strip('"') for r in m.group(4).lower().split(",") if r.strip()}
            if table in NOT_A_TABLE:
                continue  # alter default privileges ... grant ... on tables to anon;
            key = table if schema == "public" else schema + "." + table
            for role in roles:
                granted.setdefault(key, set()).add((role, privs))

    problems = []

    for table, path in sorted(public_tables.items()):
        hits = granted.get(table, set())
        roles = {r for r, _ in hits}
        if table in SERVICE_ONLY:
            if not any(r == "service_role" for r in roles):
                problems.append("%s: باید service_role گرنت داشته باشد (%s)" % (table, path))
            if roles & {"anon", "authenticated"}:
                problems.append("%s: نباید به anon/authenticated گرنت بگیرد (%s)" % (table, path))
            continue
        for need_role, need_priv in (("anon", "select"), ("authenticated", "select"), ("service_role", "all")):
            if need_role not in roles:
                problems.append("%s: گرنت %s ندارد → جدول در Data API خطای permission denied می‌دهد" % (table, need_role))
                continue
            privs = " ".join(p for r, p in hits if r == need_role)
            if need_priv == "all":
                if "all" not in privs and not all(k in privs for k in ("select", "insert", "update", "delete")):
                    problems.append("%s: %s فقط «%s» دارد (باید all یا چهار دسترسی کامل)" % (table, need_role, privs))
            elif need_priv not in privs:
                problems.append("%s: %s دسترسی %s ندارد" % (table, need_role, need_priv))

    for key, hits in sorted(granted.items()):
        if key in public_tables:
            continue
        roles = {r for r, _ in hits}
        if roles & {"anon", "authenticated"}:
            problems.append("%s: جدول غیرعمومی نباید به anon/authenticated گرنت بگیرد" % key)

    if not has_default:
        problems.append("هیچ مهاجرتی «alter default privileges ... in schema public» ندارد؛ "
                        "جدول‌های آینده و db reset بی‌دسترسی می‌شوند")

    if problems:
        print("check_data_api_grants: %d ایراد" % len(problems))
        for p in problems:
            print("  ❌ " + p)
        return 1

    if not quiet:
        print("check_data_api_grants: سالم — %d جدول عمومی، %d جدول غیرعمومی، پیش‌فرض آینده: %s"
              % (len(public_tables), len(other_tables), "بله" if has_default else "خیر"))
    else:
        print("check_data_api_grants: 0 missing/unexpected (%d tables)" % len(public_tables))
    return 0


if __name__ == "__main__":
    sys.exit(main())
