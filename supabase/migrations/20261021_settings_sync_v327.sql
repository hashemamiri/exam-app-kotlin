-- ═══════════════════════════════════════════════════════════════════════════════
-- V327 — همگام‌سازی تنظیمات بین دستگاه‌ها (ظاهر + سربرگ چاپ + تنظیمات کاغذ)
--
-- پیش از این، هر کلاینت تنظیمات را فقط روی «همان دستگاه» نگه می‌داشت
-- (اپ: DataStore / سایت: localStorage). معلمی که خانه و مدرسه کار می‌کند هر بار
-- از نو پالت، قلم، سربرگ و اندازهٔ کاغذ را می‌چید. این مهاجرت یک ردیف JSON برای
-- هر کاربر می‌سازد و دو RPC امن برای خواندن/نوشتن آن می‌دهد.
--
-- امنیت: RLS فعال است و هر کاربر فقط ردیف خودش را می‌بیند/می‌نویسد.
-- گرنت‌های صریح Data API هم گذاشته شده‌اند (قاعدهٔ V324)؛ چون سیاست RLS
-- شرط auth.uid() = user_id دارد، دسترسی anon هیچ ردیفی برنمی‌گرداند.
--
-- اجرا: SQL Editor پروژه (یک‌بار). تکرارپذیر است.
-- ═══════════════════════════════════════════════════════════════════════════════

create table if not exists public.user_settings (
  user_id uuid primary key references auth.users(id) on delete cascade,
  appearance jsonb not null default '{}'::jsonb,
  print_header jsonb not null default '{}'::jsonb,
  page_setup jsonb not null default '{}'::jsonb,
  updated_at timestamptz not null default now()
);

comment on table public.user_settings is
  'V327 — تنظیمات ظاهر/سربرگ/کاغذ هر کاربر برای همگام‌سازی بین اپ و سایت. فقط ردیف خود کاربر در دسترس است.';

alter table public.user_settings enable row level security;

drop policy if exists user_settings_select on public.user_settings;
create policy user_settings_select on public.user_settings
  for select using (auth.uid() = user_id);

drop policy if exists user_settings_insert on public.user_settings;
create policy user_settings_insert on public.user_settings
  for insert with check (auth.uid() = user_id);

drop policy if exists user_settings_update on public.user_settings;
create policy user_settings_update on public.user_settings
  for update using (auth.uid() = user_id) with check (auth.uid() = user_id);

-- گرنت صریح (قاعدهٔ ۳۰ اکتبر: جدول تازه بدون GRANT از Data API دیده نمی‌شود)
grant select on table public.user_settings to anon;
grant select, insert, update, delete on table public.user_settings to authenticated;
grant all on table public.user_settings to service_role;

-- ── خواندن تنظیمات کاربر جاری ────────────────────────────────────────────────
create or replace function public.native_settings_get_v327()
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_row public.user_settings%rowtype;
begin
  if auth.uid() is null then
    return jsonb_build_object('ok', false, 'error', 'not_authenticated');
  end if;
  select * into v_row from public.user_settings where user_id = auth.uid();
  if not found then
    return jsonb_build_object('ok', true, 'appearance', '{}'::jsonb, 'print_header', '{}'::jsonb,
                              'page_setup', '{}'::jsonb, 'updated_at', null);
  end if;
  return jsonb_build_object(
    'ok', true,
    'appearance', coalesce(v_row.appearance, '{}'::jsonb),
    'print_header', coalesce(v_row.print_header, '{}'::jsonb),
    'page_setup', coalesce(v_row.page_setup, '{}'::jsonb),
    'updated_at', v_row.updated_at
  );
end $$;

-- ── نوشتن (هر بخشی که null نباشد) ───────────────────────────────────────────
create or replace function public.native_settings_put_v327(
  p_appearance jsonb default null,
  p_print_header jsonb default null,
  p_page_setup jsonb default null
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
begin
  if auth.uid() is null then
    return jsonb_build_object('ok', false, 'error', 'not_authenticated');
  end if;
  insert into public.user_settings(user_id, appearance, print_header, page_setup, updated_at)
  values (
    auth.uid(),
    coalesce(p_appearance, '{}'::jsonb),
    coalesce(p_print_header, '{}'::jsonb),
    coalesce(p_page_setup, '{}'::jsonb),
    now()
  )
  on conflict (user_id) do update set
    appearance   = coalesce(p_appearance, public.user_settings.appearance),
    print_header = coalesce(p_print_header, public.user_settings.print_header),
    page_setup   = coalesce(p_page_setup, public.user_settings.page_setup),
    updated_at   = now();
  return jsonb_build_object('ok', true);
end $$;

grant execute on function public.native_settings_get_v327() to anon, authenticated, service_role;
grant execute on function public.native_settings_put_v327(jsonb, jsonb, jsonb) to anon, authenticated, service_role;

notify pgrst, 'reload schema';
