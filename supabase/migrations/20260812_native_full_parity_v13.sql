-- V13 full Native parity: atomic schedule wrapper, categorized question bank,
-- feedback maintenance and explicit hardened grants. Run after V12.

begin;

-- ============================================================
-- 1) Atomic schedule-aware exam save.
-- The V9 wallet/save function remains the single billing implementation.
-- Any schedule failure raises before/inside this wrapper and rolls back the call.
-- ============================================================
create or replace function public.native_save_exam_v2(p_payload jsonb)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_uid uuid := auth.uid();
    v_opens timestamptz;
    v_closes timestamptz;
    v_result jsonb;
    v_exam text;
    v_count integer;
begin
    if v_uid is null then return jsonb_build_object('error','ابتدا وارد شوید'); end if;
    if p_payload is null or jsonb_typeof(p_payload) <> 'object' then
        return jsonb_build_object('error','داده آزمون نامعتبر است');
    end if;
    begin
        v_opens := nullif(btrim(coalesce(p_payload->>'opens_at','')), '')::timestamptz;
        v_closes := nullif(btrim(coalesce(p_payload->>'closes_at','')), '')::timestamptz;
    exception when others then
        return jsonb_build_object('error','زمان بازشدن یا پایان معتبر نیست');
    end;
    if v_opens is not null and v_closes is not null and v_closes <= v_opens then
        return jsonb_build_object('error','مهلت پایان باید بعد از زمان بازشدن باشد');
    end if;

    v_result := public.native_save_exam_v1(p_payload);
    if v_result ? 'error' then return v_result; end if;
    v_exam := coalesce(v_result->>'id', p_payload->>'id');
    update public.exams
    set opens_at = v_opens,
        closes_at = v_closes
    where id = v_exam and teacher_id = v_uid;
    get diagnostics v_count = row_count;
    if v_count <> 1 then raise exception 'ذخیره زمان‌بندی آزمون کامل نشد'; end if;
    return v_result || jsonb_build_object('opens_at',v_opens,'closes_at',v_closes);
end;
$$;

-- ============================================================
-- 2) Multi-category question bank with no direct table mutation from APK.
-- ============================================================
create table if not exists public.bank_categories(
    id bigserial primary key,
    teacher_id uuid not null references auth.users(id) on delete cascade,
    name text not null,
    created_at timestamptz not null default now()
);
alter table public.bank_categories add column if not exists teacher_id uuid;
alter table public.bank_categories add column if not exists name text;
alter table public.bank_categories add column if not exists created_at timestamptz default now();
create unique index if not exists uq_bank_categories_teacher_name
    on public.bank_categories(teacher_id, lower(name));

create table if not exists public.bank_question_cats(
    question_id bigint not null references public.question_bank(id) on delete cascade,
    category_id bigint not null references public.bank_categories(id) on delete cascade,
    primary key(question_id, category_id)
);

alter table public.bank_categories enable row level security;
alter table public.bank_question_cats enable row level security;
drop policy if exists v13_bank_categories_read on public.bank_categories;
create policy v13_bank_categories_read on public.bank_categories
for select to authenticated using (teacher_id = auth.uid());
drop policy if exists v13_bank_links_read on public.bank_question_cats;
create policy v13_bank_links_read on public.bank_question_cats
for select to authenticated using (
    exists(select 1 from public.question_bank q where q.id=question_id and q.teacher_id=auth.uid())
);
revoke insert,update,delete on public.bank_categories from anon,authenticated;
revoke insert,update,delete on public.bank_question_cats from anon,authenticated;
grant select on public.bank_categories, public.bank_question_cats to authenticated;

create or replace function public.native_bank_snapshot_v1()
returns jsonb
language sql
stable
security definer
set search_path = public, pg_temp
as $$
select jsonb_build_object(
    'ok',true,
    'categories',coalesce((
        select jsonb_agg(jsonb_build_object(
            'id',c.id,'name',c.name,
            'count',(select count(*) from public.bank_question_cats l where l.category_id=c.id)
        ) order by lower(c.name),c.id)
        from public.bank_categories c where c.teacher_id=auth.uid()
    ),'[]'::jsonb),
    'items',coalesce((
        select jsonb_agg(jsonb_build_object(
            'id',q.id,'subject',q.subject,'question',q.question,'created_at',q.created_at,
            'cat_ids',coalesce((select jsonb_agg(l.category_id order by l.category_id)
                from public.bank_question_cats l where l.question_id=q.id),'[]'::jsonb),
            'cat_names',coalesce((select jsonb_agg(c.name order by lower(c.name))
                from public.bank_question_cats l join public.bank_categories c on c.id=l.category_id
                where l.question_id=q.id),'[]'::jsonb)
        ) order by q.created_at desc,q.id desc)
        from public.question_bank q where q.teacher_id=auth.uid()
    ),'[]'::jsonb)
);
$$;

create or replace function public.native_bank_category_add_v1(p_name text)
returns jsonb
language plpgsql security definer set search_path=public,pg_temp
as $$
declare v_name text:=left(btrim(coalesce(p_name,'')),100); v_id bigint;
begin
 if auth.uid() is null then return jsonb_build_object('error','ابتدا وارد شوید'); end if;
 if v_name='' then return jsonb_build_object('error','نام دسته را وارد کنید'); end if;
 perform pg_advisory_xact_lock(hashtext(auth.uid()::text||':bank-category:'||lower(v_name)));
 if exists(select 1 from public.bank_categories where teacher_id=auth.uid() and lower(name)=lower(v_name)) then
   return jsonb_build_object('error','این دسته قبلاً ساخته شده است');
 end if;
 insert into public.bank_categories(teacher_id,name) values(auth.uid(),v_name) returning id into v_id;
 return jsonb_build_object('ok',true,'id',v_id,'name',v_name);
end $$;

create or replace function public.native_bank_set_categories_v1(p_id bigint,p_cats jsonb)
returns jsonb
language plpgsql security definer set search_path=public,pg_temp
as $$
declare v_wanted integer; v_inserted integer;
begin
 if auth.uid() is null then return jsonb_build_object('error','ابتدا وارد شوید'); end if;
 if not exists(select 1 from public.question_bank where id=p_id and teacher_id=auth.uid()) then
   return jsonb_build_object('error','سؤال بانک یافت نشد یا دسترسی ندارید');
 end if;
 if p_cats is not null and jsonb_typeof(p_cats)<>'array' then return jsonb_build_object('error','دسته‌ها نامعتبرند'); end if;
 v_wanted:=coalesce(jsonb_array_length(p_cats),0);
 if exists(
   select 1 from jsonb_array_elements_text(coalesce(p_cats,'[]'::jsonb)) x
   where not exists(select 1 from public.bank_categories c where c.id=x.value::bigint and c.teacher_id=auth.uid())
 ) then return jsonb_build_object('error','یک یا چند دسته نامعتبر است'); end if;
 delete from public.bank_question_cats where question_id=p_id;
 insert into public.bank_question_cats(question_id,category_id)
 select p_id,distinct_ids.id from (
   select distinct value::bigint id from jsonb_array_elements_text(coalesce(p_cats,'[]'::jsonb))
 ) distinct_ids;
 get diagnostics v_inserted=row_count;
 if v_inserted<>v_wanted and v_wanted<>(select count(distinct value) from jsonb_array_elements_text(coalesce(p_cats,'[]'::jsonb))) then
   raise exception 'ثبت دسته‌های بانک کامل نشد';
 end if;
 return jsonb_build_object('ok',true,'count',v_inserted);
end $$;

create or replace function public.native_bank_add_v2(p_question jsonb,p_subject text,p_cats jsonb default null)
returns jsonb
language plpgsql security definer set search_path=public,pg_temp
as $$
declare v_id bigint; v_text text; v_result jsonb;
begin
 if auth.uid() is null then return jsonb_build_object('error','ابتدا وارد شوید'); end if;
 if p_question is null or jsonb_typeof(p_question)<>'object' then return jsonb_build_object('error','سؤال نامعتبر است'); end if;
 v_text:=btrim(coalesce(p_question->>'text',''));
 if v_text='' or length(v_text)>10000 then return jsonb_build_object('error','متن سؤال نامعتبر است'); end if;
 perform pg_advisory_xact_lock(hashtext(auth.uid()::text||':bank:'||md5(p_question::text)));
 if exists(select 1 from public.question_bank q where q.teacher_id=auth.uid() and q.question=p_question) then
   return jsonb_build_object('error','این سؤال از قبل در بانک وجود دارد','duplicate',true);
 end if;
 insert into public.question_bank(teacher_id,subject,question)
 values(auth.uid(),left(btrim(coalesce(p_subject,'')),250),p_question) returning id into v_id;
 v_result:=public.native_bank_set_categories_v1(v_id,p_cats);
 if v_result ? 'error' then raise exception '%',v_result->>'error'; end if;
 return jsonb_build_object('ok',true,'id',v_id);
end $$;

create or replace function public.native_bank_delete_question_v1(p_id bigint)
returns jsonb language plpgsql security definer set search_path=public,pg_temp as $$
declare v_count integer;
begin
 delete from public.question_bank where id=p_id and teacher_id=auth.uid();
 get diagnostics v_count=row_count;
 if v_count<>1 then return jsonb_build_object('error','سؤال یافت نشد یا دسترسی ندارید'); end if;
 return jsonb_build_object('ok',true);
end $$;

create or replace function public.native_bank_category_delete_v1(p_id bigint,p_delete_questions boolean default false)
returns jsonb language plpgsql security definer set search_path=public,pg_temp as $$
declare v_deleted integer:=0;
begin
 if not exists(select 1 from public.bank_categories where id=p_id and teacher_id=auth.uid()) then
   return jsonb_build_object('error','دسته یافت نشد یا دسترسی ندارید');
 end if;
 if coalesce(p_delete_questions,false) then
   with doomed as (
     select l.question_id from public.bank_question_cats l
     join public.question_bank q on q.id=l.question_id
     where l.category_id=p_id and q.teacher_id=auth.uid()
       and not exists(select 1 from public.bank_question_cats x where x.question_id=l.question_id and x.category_id<>p_id)
   ), deleted as (
     delete from public.question_bank q using doomed d
     where q.id=d.question_id and q.teacher_id=auth.uid() returning q.id
   ) select count(*) into v_deleted from deleted;
 end if;
 delete from public.bank_categories where id=p_id and teacher_id=auth.uid();
 return jsonb_build_object('ok',true,'deleted',v_deleted);
end $$;

-- ============================================================
-- 3) Feedback bank edit/delete.
-- ============================================================
create or replace function public.native_feedback_update_v1(p_id bigint,p_text text)
returns jsonb language plpgsql security definer set search_path=public,pg_temp as $$
declare v_count integer; v_text text:=left(btrim(coalesce(p_text,'')),1000);
begin
 if v_text='' then return jsonb_build_object('error','متن بازخورد خالی است'); end if;
 update public.feedback_bank set text=v_text where id=p_id and teacher_id=auth.uid();
 get diagnostics v_count=row_count;
 if v_count<>1 then return jsonb_build_object('error','بازخورد یافت نشد یا دسترسی ندارید'); end if;
 return jsonb_build_object('ok',true);
end $$;

create or replace function public.native_feedback_delete_v1(p_id bigint)
returns jsonb language plpgsql security definer set search_path=public,pg_temp as $$
declare v_count integer;
begin
 delete from public.feedback_bank where id=p_id and teacher_id=auth.uid();
 get diagnostics v_count=row_count;
 if v_count<>1 then return jsonb_build_object('error','بازخورد یافت نشد یا دسترسی ندارید'); end if;
 return jsonb_build_object('ok',true);
end $$;

create or replace function public.native_parity_status_v1()
returns jsonb language sql stable security definer set search_path=public,pg_temp as $$
select jsonb_build_object(
 'ok',true,
 'schedule_save_ready',to_regprocedure('public.native_save_exam_v2(jsonb)') is not null,
 'bank_categories_ready',to_regclass('public.bank_categories') is not null and to_regclass('public.bank_question_cats') is not null,
 'bank_snapshot_ready',to_regprocedure('public.native_bank_snapshot_v1()') is not null,
 'feedback_maintenance_ready',to_regprocedure('public.native_feedback_update_v1(bigint,text)') is not null and to_regprocedure('public.native_feedback_delete_v1(bigint)') is not null
);
$$;

-- Explicit V11-style function grants.
revoke all on function public.native_save_exam_v2(jsonb) from public,anon,authenticated;
revoke all on function public.native_bank_snapshot_v1() from public,anon,authenticated;
revoke all on function public.native_bank_category_add_v1(text) from public,anon,authenticated;
revoke all on function public.native_bank_set_categories_v1(bigint,jsonb) from public,anon,authenticated;
revoke all on function public.native_bank_add_v2(jsonb,text,jsonb) from public,anon,authenticated;
revoke all on function public.native_bank_delete_question_v1(bigint) from public,anon,authenticated;
revoke all on function public.native_bank_category_delete_v1(bigint,boolean) from public,anon,authenticated;
revoke all on function public.native_feedback_update_v1(bigint,text) from public,anon,authenticated;
revoke all on function public.native_feedback_delete_v1(bigint) from public,anon,authenticated;
revoke all on function public.native_parity_status_v1() from public,anon,authenticated;
grant execute on function public.native_save_exam_v2(jsonb) to authenticated;
grant execute on function public.native_bank_snapshot_v1() to authenticated;
grant execute on function public.native_bank_category_add_v1(text) to authenticated;
grant execute on function public.native_bank_set_categories_v1(bigint,jsonb) to authenticated;
grant execute on function public.native_bank_add_v2(jsonb,text,jsonb) to authenticated;
grant execute on function public.native_bank_delete_question_v1(bigint) to authenticated;
grant execute on function public.native_bank_category_delete_v1(bigint,boolean) to authenticated;
grant execute on function public.native_feedback_update_v1(bigint,text) to authenticated;
grant execute on function public.native_feedback_delete_v1(bigint) to authenticated;
grant execute on function public.native_parity_status_v1() to authenticated;

commit;
notify pgrst,'reload schema';
