-- V282 — حذف حساب با مهلت: معلم/مدیر «درخواست حذف» می‌دهد → حساب علامت می‌خورد و ۳۰ روز بعد برای همیشه حذف می‌شود؛
-- تا آن زمان با «انصراف از حذف» برمی‌گردد. دانش‌آموز هرگز نمی‌تواند حساب خود را حذف کند؛ فقط به معلمش «درخواست حذف» می‌فرستد (اعلان).
-- اجرای دستی یک‌بار در SQL Editor. پیش‌نیاز: SQL v232 (اعلان‌ها: private.push_enqueue, private.push_config) و pg_cron/pg_net.

begin;

alter table public.profiles add column if not exists deletion_requested_at timestamptz;

-- وضعیت من
create or replace function public.native_my_deletion_state_v282()
returns jsonb language sql security definer set search_path = public, pg_temp as $$
  select jsonb_build_object(
    'requested_at', p.deletion_requested_at,
    'purge_at', case when p.deletion_requested_at is null then null else p.deletion_requested_at + interval '30 days' end)
  from public.profiles p where p.id = auth.uid();
$$;
revoke all on function public.native_my_deletion_state_v282() from public;
grant execute on function public.native_my_deletion_state_v282() to authenticated;

-- درخواست حذف (فقط معلم/مدیر)
create or replace function public.native_request_account_deletion_v282()
returns jsonb language plpgsql security definer set search_path = public, pg_temp as $$
declare v_role text; v_at timestamptz := now();
begin
  select role into v_role from public.profiles where id = auth.uid();
  if v_role is null then return jsonb_build_object('error', 'ابتدا وارد شوید.'); end if;
  if v_role not in ('teacher', 'manager') then return jsonb_build_object('error', 'حذف حساب دانش‌آموز فقط توسط معلم انجام می‌شود.'); end if;
  update public.profiles set deletion_requested_at = v_at where id = auth.uid();
  return jsonb_build_object('ok', true, 'requested_at', v_at, 'purge_at', v_at + interval '30 days');
end; $$;
revoke all on function public.native_request_account_deletion_v282() from public;
grant execute on function public.native_request_account_deletion_v282() to authenticated;

-- انصراف از حذف
create or replace function public.native_cancel_account_deletion_v282()
returns jsonb language plpgsql security definer set search_path = public, pg_temp as $$
begin
  update public.profiles set deletion_requested_at = null where id = auth.uid();
  return jsonb_build_object('ok', true);
end; $$;
revoke all on function public.native_cancel_account_deletion_v282() from public;
grant execute on function public.native_cancel_account_deletion_v282() to authenticated;

-- دانش‌آموز: درخواست حذف به معلم‌ها (اعلان)؛ خودش چیزی حذف نمی‌کند
create or replace function public.native_student_request_deletion_v282()
returns jsonb language plpgsql security definer set search_path = public, pg_temp as $$
declare v_role text; v_name text; v_n int := 0; r record;
begin
  select role, coalesce(nullif(display_name, ''), nullif(full_name, ''), 'دانش‌آموز') into v_role, v_name from public.profiles where id = auth.uid();
  if v_role is distinct from 'student' then return jsonb_build_object('error', 'این گزینه فقط برای دانش‌آموز است.'); end if;
  for r in
    select distinct t.teacher_id from (
      select teacher_id from public.teacher_student_links where student_id = auth.uid()
      union select teacher_id from public.profiles where id = auth.uid() and teacher_id is not null
    ) t
  loop
    perform private.push_enqueue(r.teacher_id, 'student_delete_request', 'درخواست حذف حساب دانش‌آموز',
      v_name || ' درخواست حذف حساب خود را داده است. در صورت موافقت، از صفحهٔ دانش‌آموزان حذفش کنید.',
      jsonb_build_object('page', 'students', 'student_id', auth.uid()), 'sdel:' || auth.uid() || ':' || r.teacher_id || ':' || to_char(now(), 'YYYYMMDD'));
    v_n := v_n + 1;
  end loop;
  return jsonb_build_object('ok', true, 'teachers', v_n);
end; $$;
revoke all on function public.native_student_request_deletion_v282() from public;
grant execute on function public.native_student_request_deletion_v282() to authenticated;

-- فهرست حساب‌های سررسیدشده (فقط برای Edge Function با service role)
create or replace function public.native_due_account_deletions_v282()
returns setof uuid language sql security definer set search_path = public, pg_temp as $$
  select id from public.profiles
  where role in ('teacher', 'manager') and deletion_requested_at is not null and deletion_requested_at < now() - interval '30 days'
  order by deletion_requested_at limit 50;
$$;
revoke all on function public.native_due_account_deletions_v282() from public, anon, authenticated;

-- زمان‌بند روزانه: pg_net → manage-student با هدر x-push-secret (همان Secret اعلان‌ها)
create or replace function private.account_purge_tick_v282() returns void language plpgsql security definer set search_path = public, pg_temp as $$
declare c record;
begin
  select * into c from private.push_config where id = 1;
  if c.secret is null then return; end if;
  perform net.http_post(url := replace(c.function_url, 'push-dispatch', 'manage-student'),
    headers := jsonb_build_object('Content-Type', 'application/json', 'x-push-secret', c.secret),
    body := '{"action":"purge_due_accounts"}'::jsonb, timeout_milliseconds := 60000);
end; $$;
select cron.unschedule(jobid) from cron.job where jobname = 'account_purge_v282';
select cron.schedule('account_purge_v282', '35 3 * * *', $$select private.account_purge_tick_v282()$$);

commit;
