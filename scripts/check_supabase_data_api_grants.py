#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""V324 / V335.0 — نگهبان جامع «گرنت Data API و امنیت ۳۰ اکتبر Supabase».

این اسکریپت در هر اجرای preflight و تست واحد تضمین می کند:
  ۱) هر `create table public.X` در مهاجرت ها دارای GRANT صریح برای Data API و
     دارای `ENABLE ROW LEVEL SECURITY` باشد.
  ۲) تمام جدول هایی که مستقیماً از کد اپ کاتلین (`.from("...")`)، سایت (`/rest/v1/...`)
     و توابع لبه (`.from('...')`) فراخوانی می شوند (شامل جدول های هسته ای اولیه مثل
     profiles, exams, exam_keys, answers, classes, class_members) دارای پوشش گرنت
     صریح در مهاجرت ها باشند و `exam_keys` هرگز گرنت مستقیم به `anon` نگیرد.
  ۳) هیچ جدول غیرعمومی (مانند `private.*`) به `anon` یا `authenticated` گرنت نگیرد.
  ۴) مهاجرت شامل `alter default privileges ... in schema public` و قفل `search_path`
     توابع عمومی وجود داشته باشد.
"""
import glob
import os
import re
import sys

MIGRATIONS = sorted(glob.glob(os.path.join("supabase", "migrations", "*.sql")))
EXTRA_SQL = sorted(glob.glob(os.path.join("sql", "**", "*.sql"), recursive=True))
SERVICE_ONLY = set()

CREATE = re.compile(
    r"create\s+table\s+(?:if\s+not\s+exists\s+)?(?:(public|private|auth|storage)\s*\.\s*)?([a-zA-Z0-9_]+)",
    re.I,
)
GRANT = re.compile(
    r"grant\s+([a-z, \t\r\n]+?)\s+on\s+(?:table\s+)?(?:(public|private|auth|storage)\s*\.\s*)?([a-zA-Z0-9_]+)\s+to\s+([a-z_,\s]+)",
    re.I,
)
ENABLE_RLS = re.compile(
    r"alter\s+table\s+(?:if\s+exists\s+)?(?:only\s+)?(?:public\s*\.\s*)?([a-zA-Z0-9_]+)\s+enable\s+row\s+level\s+security",
    re.I,
)
DEFAULT_PRIV = re.compile(r"alter\s+default\s+privileges[\s\S]{0,200}?in\s+schema\s+public", re.I)
NOT_A_TABLE = {"tables", "sequences", "functions", "routines", "all", "schema", "database"}


def strip_comments(sql: str) -> str:
    sql = re.sub(r"/\*[\s\S]*?\*/", " ", sql)
    return re.sub(r"--[^\n]*", " ", sql)


def collect_client_direct_tables() -> set[str]:
    tables = set()
    for p in glob.glob(os.path.join("site", "src", "*.js")):
        txt = open(p, encoding="utf-8", errors="replace").read()
        for m in re.finditer(r"/rest/v1/([a-zA-Z0-9_]+)", txt):
            if m.group(1).lower() != "rpc":
                tables.add(m.group(1).lower())
    for p in glob.glob(os.path.join("app", "src", "main", "**", "*.kt"), recursive=True):
        txt = open(p, encoding="utf-8", errors="replace").read()
        for m in re.finditer(r'\.from\(\s*"([a-zA-Z0-9_]+)"\s*\)', txt):
            tables.add(m.group(1).lower())
    for p in glob.glob(os.path.join("supabase", "functions", "**", "*.ts"), recursive=True):
        txt = open(p, encoding="utf-8", errors="replace").read()
        for m in re.finditer(r'\.from\(\s*[\'"]([a-zA-Z0-9_]+)[\'"]\s*\)', txt):
            tables.add(m.group(1).lower())
    return tables


def main() -> int:
    quiet = "--quiet" in sys.argv
    files = MIGRATIONS + EXTRA_SQL
    if not MIGRATIONS:
        print("check_data_api_grants: پوشه supabase/migrations پیدا نشد")
        return 1

    public_tables = {}
    other_tables = {}
    granted = {}
    core_dynamic_tables = set()
    rls_enabled = set()
    has_default = False
    has_search_path_sweep = False
    exam_keys_granted_to_anon = False

    for path in files:
        raw = open(path, encoding="utf-8", errors="replace").read()
        sql = strip_comments(raw)
        if DEFAULT_PRIV.search(sql):
            has_default = True
        if "alter function %s set search_path = public, pg_temp" in sql.lower():
            has_search_path_sweep = True
        arr_match = re.search(r"v_core_tables\s+text\[\]\s*:=\s*array\[([\s\S]*?)\]", sql, re.I)
        if arr_match:
            for item in re.findall(r"'([a-zA-Z0-9_]+)'", arr_match.group(1)):
                core_dynamic_tables.add(item.lower())
                rls_enabled.add(item.lower())
        for m in ENABLE_RLS.finditer(sql):
            rls_enabled.add(m.group(1).lower())
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
                continue
            key = table if schema == "public" else schema + "." + table
            if key == "exam_keys" and "anon" in roles:
                exam_keys_granted_to_anon = True
            for role in roles:
                granted.setdefault(key, set()).add((role, privs))

    problems = []

    for table, path in sorted(public_tables.items()):
        if table not in rls_enabled:
            problems.append("%s: ENABLE ROW LEVEL SECURITY در مهاجرت ها ندارد (%s)" % (table, path))
        hits = granted.get(table, set())
        roles = {r for r, _ in hits}
        if table in SERVICE_ONLY:
            if "service_role" not in roles:
                problems.append("%s: باید service_role گرنت داشته باشد (%s)" % (table, path))
            if roles & {"anon", "authenticated"}:
                problems.append("%s: نباید به anon/authenticated گرنت بگیرد (%s)" % (table, path))
            continue
        for need_role, need_priv in (("anon", "select"), ("authenticated", "select"), ("service_role", "all")):
            if need_role not in roles:
                problems.append("%s: گرنت %s ندارد -> جدول در Data API خطای permission denied می دهد" % (table, need_role))
                continue
            privs = " ".join(p for r, p in hits if r == need_role)
            if need_priv == "all":
                if "all" not in privs and not all(k in privs for k in ("select", "insert", "update", "delete")):
                    problems.append("%s: %s فقط «%s» دارد (باید all یا چهار دسترسی کامل)" % (table, need_role, privs))
            elif need_priv not in privs:
                problems.append("%s: %s دسترسی %s ندارد" % (table, need_role, need_priv))

    for tbl in sorted(collect_client_direct_tables()):
        if tbl not in granted and tbl not in core_dynamic_tables:
            problems.append("%s: در کد کلاینت/تابع لبه مستقیماً فراخوانی شده اما هیچ GRANT صریحی در مهاجرت ها ندارد" % tbl)

    if exam_keys_granted_to_anon:
        problems.append("exam_keys: هرگز نباید به نقش anon دسترسی مستقیم داده شود!")

    for key, hits in sorted(granted.items()):
        if "." in key:
            roles = {r for r, _ in hits}
            if roles & {"anon", "authenticated"}:
                problems.append("%s: جدول غیرعمومی نباید به anon/authenticated گرنت بگیرد" % key)

    if not has_default:
        problems.append("هیچ مهاجرتی alter default privileges ... in schema public ندارد")
    if not has_search_path_sweep:
        problems.append("مهاجرت قفل امنیتی search_path برای توابع public/private یافت نشد")

    total_covered = len(set(public_tables) | core_dynamic_tables)
    if problems:
        print("check_data_api_grants: %d ایراد" % len(problems))
        for p in problems:
            print("  ❌ " + p)
        return 1

    if not quiet:
        print(
            "check_data_api_grants: سالم — %d جدول تحت پوشش صریح (%d جدید + %d هسته ای اولیه)، %d جدول خصوصی قفل شده"
            % (total_covered, len(public_tables), len(core_dynamic_tables), len(other_tables))
        )
    else:
        print("check_data_api_grants: 0 missing/unexpected (%d tables)" % total_covered)
    return 0


if __name__ == "__main__":
    sys.exit(main())
