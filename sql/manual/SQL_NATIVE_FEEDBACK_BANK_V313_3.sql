-- V313.3 — تعریف جدول و توابع بانک بازخورد معلم (fb_list, fb_add, native_feedback_update_v1, native_feedback_delete_v1)
begin;

create table if not exists public.feedback_bank (
    id bigserial primary key,
    teacher_id uuid not null references auth.users(id) on delete cascade,
    text text not null,
    created_at timestamptz not null default now()
);
alter table public.feedback_bank enable row level security;

create or replace function public.fb_list()
returns table(id bigint, text text)
language sql
stable
security definer
set search_path = public, pg_temp
as $$
    select f.id, f.text
    from public.feedback_bank f
    where f.teacher_id = auth.uid()
    order by f.id desc;
$$;

create or replace function public.fb_add(p_text text)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_text text := left(btrim(coalesce(p_text, '')), 1000);
    v_id bigint;
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    if v_text = '' then return jsonb_build_object('error', 'متن بازخورد خالی است'); end if;
    insert into public.feedback_bank(teacher_id, text)
    values (v_uid, v_text)
    returning id into v_id;
    return jsonb_build_object('ok', true, 'id', v_id);
end;
$$;

create or replace function public.native_feedback_update_v1(p_id bigint, p_text text)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_count integer;
    v_text text := left(btrim(coalesce(p_text, '')), 1000);
begin
    if v_text = '' then return jsonb_build_object('error', 'متن بازخورد خالی است'); end if;
    update public.feedback_bank
    set text = v_text
    where id = p_id and teacher_id = auth.uid();
    get diagnostics v_count = row_count;
    if v_count <> 1 then return jsonb_build_object('error', 'بازخورد یافت نشد یا دسترسی ندارید'); end if;
    return jsonb_build_object('ok', true);
end;
$$;

create or replace function public.native_feedback_delete_v1(p_id bigint)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_count integer;
begin
    delete from public.feedback_bank
    where id = p_id and teacher_id = auth.uid();
    get diagnostics v_count = row_count;
    if v_count <> 1 then return jsonb_build_object('error', 'بازخورد یافت نشد یا دسترسی ندارید'); end if;
    return jsonb_build_object('ok', true);
end;
$$;

revoke all on function public.fb_list() from public, anon, authenticated;
revoke all on function public.fb_add(text) from public, anon, authenticated;
revoke all on function public.native_feedback_update_v1(bigint, text) from public, anon, authenticated;
revoke all on function public.native_feedback_delete_v1(bigint) from public, anon, authenticated;

grant execute on function public.fb_list() to authenticated;
grant execute on function public.fb_add(text) to authenticated;
grant execute on function public.native_feedback_update_v1(bigint, text) to authenticated;
grant execute on function public.native_feedback_delete_v1(bigint) to authenticated;

commit;
