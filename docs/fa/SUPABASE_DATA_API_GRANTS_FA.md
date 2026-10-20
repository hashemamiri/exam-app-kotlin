# تغییر «Data API» سوپابیس — ۳۰ اکتبر (V324)

## خبر چیست
از ۳۰ اکتبر، سوپابیس دیگر به **جدول‌های تازه** در schema `public` به‌صورت خودکار به `anon`،
`authenticated` و `service_role` دسترسی نمی‌دهد. جدول‌های موجود دست‌نخورده می‌مانند؛ اما از آن
تاریخ هر جدولی که `create table` می‌شود، بدون `grant` صریح، از PostgREST (supabase-js، GraphQL)
و **توابع لبه** با خطای `permission denied` رد می‌شود. این شامل پروژهٔ تازه، شاخهٔ پیش‌نمایش و
`supabase db reset` هم می‌شود.

## وضعیت این پروژه (بررسی‌شده)
| مورد | نتیجه |
|---|---|
| جدول‌های ساخته‌شده در مهاجرت‌ها | **۴۰ جدول عمومی** + ۱ جدول `private.push_config` |
| جدول‌های عمومی که `grant` صریح داشتند | فقط `app_version` |
| RLS | روی هر ۴۰ جدول عمومی **فعال** است (امنیت ردیف‌ها با policy است، نه با grant) |
| دسترسی وب‌سایت | تقریباً همه از طریق **RPC** (`/rest/v1/rpc/…`، ۲۷۳ تابع `security definer`) + یک `select` روی `app_version` |
| توابع لبه | با `service_role` روی `manager_approval_requests`، `push_outbox`، `push_tokens`، `wallet_payment_orders`، `student_admin_audit`، `maintenance_audit`، `schools`، `school_memberships`، `school_students` کار می‌کنند |
| جدول‌های خارج از مهاجرت (`exams`، `answers`، `exam_keys`، `profiles`، `classes`، `class_members`) | دستی در داشبورد ساخته شده‌اند → «جدول موجود» هستند و **تغییر نمی‌کنند** |

خلاصه: امروز چیزی نمی‌شکند. خطر واقعی دو جا است: (۱) **مهاجرت بعدی** که جدول تازه می‌سازد،
(۲) بازسازی/پروژهٔ تازه (`db reset`، شاخهٔ پیش‌نمایش) که در آن هر ۴۰ جدول بدون گرنت می‌مانند و
توابع لبه و `app_version` از کار می‌افتند.

## کاری که این نسخه انجام می‌دهد
1. **مهاجرت `supabase/migrations/20261020_explicit_data_api_grants_v324.sql`**
   - `alter default privileges for role postgres in schema public …` برای جدول‌ها، دنباله‌ها و
     توابع → جدول‌های آینده همان دسترسی قبلی را می‌گیرند.
   - گرنت صریح برای هر ۴۰ جدول عمومی:
     `select` → `anon`، `select/insert/update/delete` → `authenticated`، `all` → `service_role`.
   - `notify pgrst, 'reload schema'` برای تازه‌شدن کش PostgREST.
   - تکرارپذیر و بی‌خطر است؛ `anon` قبلاً `all` داشت و اکنون فقط `select` می‌گیرد (تنگ‌تر، امن‌تر).
   - `private.push_config` بیرون از schema عمومی است و هیچ گرنتی به `anon/authenticated` نمی‌گیرد.
2. **نگهبان `scripts/check_supabase_data_api_grants.py`** — در `preflight.py` هم اجرا می‌شود:
   اگر مهاجرتی `create table` بسازد و `grant` را جا بیندازد، بیلد همان‌جا قرمز می‌شود.
3. **تست `V324_SupabaseDataApiGrantsTest`** — همین قاعده‌ها را در CI تضمین می‌کند (۴۰ جدول،
   پیش‌فرض آینده، خصوصی‌ماندن `push_config`، گرنت جدول‌های مورد استفادهٔ توابع لبه).

## کاری که باید یک‌بار در SQL Editor انجام شود
```
supabase/migrations/20261020_explicit_data_api_grants_v324.sql
```
کل فایل را در **SQL Editor** پروژهٔ اصلی (و اگر پروژهٔ آزمایشی جدا دارید، همان‌جا) اجرا کنید.
بعد از اجرا، بخش Data API → Settings باید همان ۴۰ جدول را «exposed» نشان بدهد.

## قاعده از این پس (برای هر جدول تازه)
در **همان مهاجرتی که جدول را می‌سازد** این سه خط را بگذارید:
```sql
grant select on table public.your_table to anon;
grant select, insert, update, delete on table public.your_table to authenticated;
grant all on table public.your_table to service_role;
```
اگر جدول حساس است و نباید از API دیده شود، آن را در schema `private` بسازید (مثل `push_config`).

اجرای دستی نگهبان: `python3 scripts/check_supabase_data_api_grants.py`
