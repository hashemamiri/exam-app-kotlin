-- V335.57: Word export entitlement is bound to the full exam revision.
-- First export of a revision costs 20,000 toman; the same revision is downloadable
-- without limit. Any content/header/settings change creates a new revision and charge.
begin;

create table if not exists public.native_word_export_entitlements (
    user_id uuid not null references auth.users(id) on delete cascade,
    exam_id text not null,
    revision_hash text not null,
    paid_at timestamptz not null default now(),
    primary key (user_id, exam_id, revision_hash)
);
alter table public.native_word_export_entitlements enable row level security;
revoke all on public.native_word_export_entitlements from public, anon, authenticated;
grant select on public.native_word_export_entitlements to anon, authenticated;
grant all on public.native_word_export_entitlements to service_role;

create or replace function public.native_charge_word_v287(
    p_exam text,
    p_operation uuid,
    p_title text default '',
    p_revision text default ''
) returns jsonb
language plpgsql security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_exam text := coalesce(nullif(p_exam, ''), 'local');
    v_revision text := md5(coalesce(p_revision, ''));
    v_cost bigint := 20000;
    v_balance bigint;
    v_result jsonb;
    v_prior jsonb;
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    if p_operation is null then return jsonb_build_object('error', 'شناسه عملیات لازم است'); end if;
    if length(coalesce(p_revision, '')) < 2 then return jsonb_build_object('error', 'نسخهٔ آزمون برای بررسی پرداخت موجود نیست'); end if;
    perform pg_advisory_xact_lock(hashtextextended(v_uid::text || ':' || v_exam, 0));
    select result into v_prior from public.native_exam_operations where operation_id = p_operation and user_id = v_uid;
    if found then return v_prior || jsonb_build_object('idempotent', true); end if;
    if exists (select 1 from public.native_exam_operations where operation_id = p_operation) then
        return jsonb_build_object('error', 'شناسه عملیات قبلاً مصرف شده است');
    end if;
    if exists (select 1 from public.native_word_export_entitlements where user_id=v_uid and exam_id=v_exam and revision_hash=v_revision) then
        v_result := jsonb_build_object('ok', true, 'cost', 0, 'already_paid', true);
        insert into public.native_exam_operations(operation_id,user_id,exam_id,result) values(p_operation,v_uid,v_exam,v_result);
        return v_result;
    end if;
    insert into public.wallets(user_id,balance) values(v_uid,0) on conflict(user_id) do nothing;
    select balance into v_balance from public.wallets where user_id=v_uid for update;
    if v_balance < v_cost then return jsonb_build_object('error','موجودی کیف پول کافی نیست','balance',v_balance,'required',v_cost); end if;
    update public.wallets set balance=balance-v_cost,updated_at=now() where user_id=v_uid returning balance into v_balance;
    insert into public.wallet_tx(user_id,amount,reason,balance_after,operation_key)
    values(v_uid,-v_cost,'exam:word:'||v_exam||':'||left(coalesce(p_title,''),80),v_balance,p_operation);
    insert into public.native_word_export_entitlements(user_id,exam_id,revision_hash)
    values(v_uid,v_exam,v_revision);
    v_result := jsonb_build_object('ok',true,'cost',v_cost,'balance',v_balance,'already_paid',false);
    insert into public.native_exam_operations(operation_id,user_id,exam_id,result) values(p_operation,v_uid,v_exam,v_result);
    return v_result;
end;
$$;
revoke all on function public.native_charge_word_v287(text,uuid,text,text) from public, anon;
grant execute on function public.native_charge_word_v287(text,uuid,text,text) to authenticated;
commit;
notify pgrst, 'reload schema';
