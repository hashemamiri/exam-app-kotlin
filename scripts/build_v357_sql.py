#!/usr/bin/env python3
"""V357 — فایل یک‌جای «نصب + بررسی» را از دو فایل منبع می‌سازد.

چرا تولیدشده؟ چون نگه‌داشتن سه فایل SQL با دست، دیر یا زود از هم
جدا می‌افتد. این اسکریپت تنها راه ساختن _ALL است و نگهبان هم همین
را بازتولید و مقایسه می‌کند.

اجرا:  python3 scripts/build_v357_sql.py [--check]
"""
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
SQLDIR = ROOT / "supabase" / "sql"

HEADER = """-- =====================================================================
-- V357 — نصب و بررسی، در یک فایل
--
-- این تنها فایلی است که باید اجرا کنید. کل آن را در
-- Supabase -> SQL Editor بچسبانید و Run بزنید.
--
-- چرا یک فایل؟ چون اجرای فایل «بررسی» پیش از «نصب» خطای گمراه‌کنندهٔ
--   relation "public.exam_time_grants" does not exist
-- می‌داد. اینجا ترتیب اشتباه ممکن نیست.
--
-- ایمنی: هیچ پاسخ یا نمرهٔ تاریخی بازنویسی نمی‌شود. اجرای دوباره بی‌خطر
--   است. اگر پیش‌نیازی نبود، پیش از هر تغییر با پیام روشن می‌ایستد.
--
-- این فایل تولیدشده است؛ آن را با دست ویرایش نکنید.
--   منبع: SQL_EXAM_TIMING_V357.sql + SQL_EXAM_TIMING_V357_CHECK.sql
--   ساخت: python3 scripts/build_v357_sql.py
-- =====================================================================

"""

GUARD = """-- ---------------------------------------------------------------------
-- نگهبان پیش‌نیاز — پیش از هر تغییر، با پیام روشن
-- ---------------------------------------------------------------------
do $$
declare
  v_missing text;
  v_cols    text;
begin
  select string_agg(t, ', ' order by t) into v_missing
    from (values ('public.exams'), ('public.exam_attempts'),
                 ('public.exam_sessions'), ('public.answers')) v(t)
   where to_regclass(t) is null;
  if v_missing is not null then
    raise exception 'جدول پایه پیدا نشد: %. آیا این فایل را در پروژهٔ درست Supabase اجرا می‌کنید؟', v_missing;
  end if;

  select string_agg(w.tbl || '.' || w.col, ', ' order by w.tbl, w.col) into v_cols
    from (values
      ('exam_attempts','id'), ('exam_attempts','exam_id'), ('exam_attempts','student_id'),
      ('exam_attempts','attempt_no'), ('exam_attempts','expires_at'),
      ('exam_sessions','exam_id'), ('exam_sessions','student_id'), ('exam_sessions','expires_at'),
      ('answers','id'), ('answers','exam_id'), ('answers','student_id'),
      ('answers','responses'), ('answers','meta'),
      ('exams','id'), ('exams','teacher_id')
    ) w(tbl, col)
   where not exists (select 1 from information_schema.columns c
                      where c.table_schema='public' and c.table_name=w.tbl and c.column_name=w.col);
  if v_cols is not null then
    raise exception 'ستون پایه پیدا نشد: %. ساختار پایگاه با آنچه انتظار می‌رفت فرق دارد؛ پیش از نصب گزارش بگیرید.', v_cols;
  end if;
end $$;

"""

VERIFY_HEADER = """
-- =====================================================================
-- بررسی درستی — همهٔ ردیف‌های زیر باید true باشند
-- =====================================================================

"""


def build() -> str:
    mig = (SQLDIR / "SQL_EXAM_TIMING_V357.sql").read_text(encoding="utf-8")
    chk = (SQLDIR / "SQL_EXAM_TIMING_V357_CHECK.sql").read_text(encoding="utf-8")
    start = mig.index("create table if not exists public.exam_time_grants")
    start = mig.rindex("-- ------", 0, start)
    end = mig.index("grant execute on function public.native_offline_submit_v357")
    end = mig.index("\n", end) + 1
    body = mig[start:end]
    verify = chk[chk.index("with checks(label, ok) as ("):]
    return HEADER + GUARD + body + VERIFY_HEADER + verify


def main() -> int:
    out = SQLDIR / "SQL_EXAM_TIMING_V357_ALL.sql"
    text = build()
    if "--check" in sys.argv:
        if not out.exists():
            print("build_v357_sql: فایل یک‌جا ساخته نشده است")
            return 1
        if out.read_text(encoding="utf-8") != text:
            print("build_v357_sql: فایل یک‌جا با منبع‌ها هم‌خوان نیست")
            print("  راه حل: python3 scripts/build_v357_sql.py")
            return 1
        print("build_v357_sql: سالم — فایل یک‌جا با منبع‌ها هم‌خوان است")
        return 0
    out.write_text(text, encoding="utf-8")
    print(f"build_v357_sql: ساخته شد — {len(text.splitlines())} خط")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
