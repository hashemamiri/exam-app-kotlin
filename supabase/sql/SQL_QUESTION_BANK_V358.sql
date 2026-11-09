-- =====================================================================
-- V358 — بانک سؤال: درختوارهٔ درس/فصل/مبحث
--
-- اجرا: در Supabase -> SQL Editor، کل این فایل را یک‌جا اجرا کنید.
-- ایمنی: هیچ سؤال، پاسخ یا نمره‌ای بازنویسی یا حذف نمی‌شود. اجرای
--        دوباره بی‌خطر است (idempotent).
--
-- چه چیزی عوض می‌شود:
--   • ستون parent_id روی bank_categories — دسته‌ها درخت می‌شوند.
--   • یکتایی نام از «به‌ازای معلم» به «به‌ازای معلم و والد» تغییر
--     می‌کند، تا «فصل ۱» زیر ریاضی و زیر فیزیک هر دو مجاز باشند.
--   • ژرفا حداکثر ۳: درس <- فصل <- مبحث.
--   • حلقه (والد شدنِ فرزندِ خود) بسته می‌شود.
--
-- پالایش دشواری و انتخاب خودکار سمت کلاینت انجام می‌شود و به SQL
-- تازه‌ای نیاز ندارد؛ snapshot موجود همهٔ داده را برمی‌گرداند.
-- =====================================================================

-- ---------------------------------------------------------------------
-- نگهبان پیش‌نیاز
-- ---------------------------------------------------------------------
do $$
begin
  if to_regclass('public.bank_categories') is null
     or to_regclass('public.question_bank') is null
     or to_regclass('public.bank_question_cats') is null then
    raise exception 'جدول بانک سؤال پیدا نشد. آیا این فایل را در پروژهٔ درست Supabase اجرا می‌کنید؟';
  end if;
end $$;

-- ---------------------------------------------------------------------
-- ۱) ستون والد
--    حذف دسته، زیردسته‌هایش را هم برمی‌دارد؛ ولی «سؤال‌ها» هرگز حذف
--    نمی‌شوند — فقط پیوند دسته از bank_question_cats می‌رود.
-- ---------------------------------------------------------------------
alter table public.bank_categories
  add column if not exists parent_id bigint;

do $$
begin
  if not exists (select 1 from pg_constraint where conname = 'bank_categories_parent_fk') then
    alter table public.bank_categories
      add constraint bank_categories_parent_fk
      foreign key (parent_id) references public.bank_categories(id) on delete cascade;
  end if;
end $$;

create index if not exists ix_bank_categories_parent
  on public.bank_categories(teacher_id, parent_id);

-- ---------------------------------------------------------------------
-- ۲) یکتایی نام در هر سطح
--    ایندکس پیشین سراسری بود و اجازه نمی‌داد دو درس «فصل ۱» داشته
--    باشند. دادهٔ موجود با قاعدهٔ تازه هم سازگار است (قاعده ضعیف‌تر).
-- ---------------------------------------------------------------------
drop index if exists public.uq_bank_categories_teacher_name;
create unique index if not exists uq_bank_categories_parent_name
  on public.bank_categories(teacher_id, coalesce(parent_id, 0), lower(name));

-- ---------------------------------------------------------------------
-- ۳) نگهبان ژرفا و حلقه
--    ژرفا: ریشه=۱، فصل=۲، مبحث=۳. بیشتر از آن رد می‌شود.
-- ---------------------------------------------------------------------
create or replace function public.native_bank_tree_guard_v358()
returns trigger
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  v_depth integer := 1;
  v_cur   bigint  := new.parent_id;
  v_seen  bigint[] := array[new.id];
begin
  if new.parent_id is null then
    return new;
  end if;
  if new.parent_id = new.id then
    raise exception 'یک دسته نمی‌تواند والد خودش باشد.';
  end if;
  if not exists (select 1 from public.bank_categories
                  where id = new.parent_id and teacher_id = new.teacher_id) then
    raise exception 'دستهٔ والد پیدا نشد یا متعلق به شما نیست.';
  end if;

  while v_cur is not null loop
    if v_cur = any(v_seen) then
      raise exception 'این جابه‌جایی حلقه می‌سازد.';
    end if;
    v_seen := v_seen || v_cur;
    v_depth := v_depth + 1;
    if v_depth > 3 then
      raise exception 'ژرفای درخت حداکثر سه سطح است: درس، فصل، مبحث.';
    end if;
    select parent_id into v_cur from public.bank_categories where id = v_cur;
  end loop;

  -- زیردرخت این دسته هم نباید از سه سطح بیرون بزند
  if exists (
    with recursive down as (
      select id, 1 as lv from public.bank_categories where id = new.id
      union all
      select c.id, d.lv + 1 from public.bank_categories c join down d on c.parent_id = d.id
    )
    select 1 from down where lv + v_depth - 1 > 3
  ) then
    raise exception 'با این جابه‌جایی، زیردسته‌ها از سه سطح بیرون می‌زنند.';
  end if;

  return new;
end $$;

drop trigger if exists native_bank_tree_guard_v358 on public.bank_categories;
create trigger native_bank_tree_guard_v358
  before insert or update of parent_id, teacher_id on public.bank_categories
  for each row execute function public.native_bank_tree_guard_v358();

-- ---------------------------------------------------------------------
-- ۴) افزودن دسته با والد
--    نسخهٔ v1 دست‌نخورده می‌ماند تا اپ‌های قدیمی نشکنند.
-- ---------------------------------------------------------------------
create or replace function public.native_bank_category_add_v358(p_name text, p_parent bigint default null)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  v_uid   uuid := auth.uid();
  v_name  text := left(btrim(coalesce(p_name, '')), 100);
  v_id    bigint;
begin
  if v_uid is null then raise exception 'ابتدا وارد شوید.'; end if;
  if v_name = '' then raise exception 'نام دسته را وارد کنید.'; end if;
  if p_parent is not null and not exists (
       select 1 from public.bank_categories where id = p_parent and teacher_id = v_uid) then
    raise exception 'دستهٔ والد پیدا نشد یا متعلق به شما نیست.';
  end if;
  if exists (select 1 from public.bank_categories
              where teacher_id = v_uid and coalesce(parent_id, 0) = coalesce(p_parent, 0)
                and lower(name) = lower(v_name)) then
    raise exception 'در این سطح، دسته‌ای با همین نام هست.';
  end if;

  insert into public.bank_categories(teacher_id, name, parent_id)
  values (v_uid, v_name, p_parent) returning id into v_id;

  return jsonb_build_object('ok', true, 'id', v_id, 'name', v_name, 'parent_id', p_parent);
end $$;

-- ---------------------------------------------------------------------
-- ۵) جابه‌جایی دسته در درخت
-- ---------------------------------------------------------------------
create or replace function public.native_bank_category_move_v358(p_id bigint, p_parent bigint)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare v_uid uuid := auth.uid();
begin
  if v_uid is null then raise exception 'ابتدا وارد شوید.'; end if;
  if not exists (select 1 from public.bank_categories where id = p_id and teacher_id = v_uid) then
    raise exception 'دسته پیدا نشد یا متعلق به شما نیست.';
  end if;
  if p_parent is not null and not exists (
       select 1 from public.bank_categories where id = p_parent and teacher_id = v_uid) then
    raise exception 'دستهٔ والد پیدا نشد یا متعلق به شما نیست.';
  end if;

  -- برخورد نام باید پیام فارسی بدهد، نه خطای خام ایندکس یکتایی
  if exists (select 1 from public.bank_categories c
              where c.teacher_id = v_uid and c.id <> p_id
                and coalesce(c.parent_id, 0) = coalesce(p_parent, 0)
                and lower(c.name) = (select lower(name) from public.bank_categories where id = p_id)) then
    raise exception 'در مقصد، دسته‌ای با همین نام هست. ابتدا نام را عوض کنید.';
  end if;

  update public.bank_categories set parent_id = p_parent where id = p_id;
  return jsonb_build_object('ok', true, 'id', p_id, 'parent_id', p_parent);
end $$;

-- ---------------------------------------------------------------------
-- ۶) درخت با شمارش زیردرخت
--    شمارش هر گره شامل خودش و همهٔ زیردسته‌هایش است، تا معلم بداند
--    با انتخاب یک فصل چند سؤال در دسترس دارد.
-- ---------------------------------------------------------------------
create or replace function public.native_bank_tree_v358()
returns jsonb
language plpgsql
stable
security definer
set search_path = public, pg_temp
as $$
declare v_uid uuid := auth.uid();
begin
  if v_uid is null then raise exception 'ابتدا وارد شوید.'; end if;
  return (
    with recursive mine as (
      select id, name, parent_id from public.bank_categories where teacher_id = v_uid
    ), tree as (
      select m.id, m.name, m.parent_id, 1 as depth, m.name::text as path
        from mine m where m.parent_id is null
      union all
      select m.id, m.name, m.parent_id, t.depth + 1, t.path || ' / ' || m.name
        from mine m join tree t on m.parent_id = t.id
    ), sub as (
      select r.id as root, d.id as node
        from mine r
        join lateral (
          with recursive down as (
            select r.id as id
            union all
            select c.id from mine c join down on c.parent_id = down.id
          ) select id from down
        ) d on true
    )
    select coalesce(jsonb_agg(jsonb_build_object(
             'id', t.id, 'name', t.name, 'parent_id', t.parent_id,
             'depth', t.depth, 'path', t.path,
             'count', (select count(*) from public.bank_question_cats l where l.category_id = t.id),
             'subtree_count', (select count(distinct l.question_id)
                                 from public.bank_question_cats l
                                where l.category_id in (select s.node from sub s where s.root = t.id))
           ) order by t.path, t.id), '[]'::jsonb)
    from tree t
  );
end $$;

-- ---------------------------------------------------------------------
-- ۷) snapshot موجود، غنی‌تر می‌شود
--    فقط فیلد تازه اضافه می‌شود؛ کلاینت قدیمی آن‌ها را نادیده می‌گیرد
--    و نمی‌شکند. امضای تابع عوض نمی‌شود.
-- ---------------------------------------------------------------------
create or replace function public.native_bank_snapshot_v1()
returns jsonb
language sql
stable
security definer
set search_path = public, pg_temp
as $$
select jsonb_build_object(
    'ok', true,
    'categories', coalesce((
        select jsonb_agg(jsonb_build_object(
            'id', c.id, 'name', c.name, 'parent_id', c.parent_id,
            'count', (select count(*) from public.bank_question_cats l where l.category_id = c.id)
        ) order by lower(c.name), c.id)
        from public.bank_categories c where c.teacher_id = auth.uid()
    ), '[]'::jsonb),
    'items', coalesce((
        select jsonb_agg(jsonb_build_object(
            'id', q.id, 'subject', q.subject, 'question', q.question, 'created_at', q.created_at,
            'difficulty', coalesce(nullif(q.question->>'difficulty', '')::integer, 0),
            'topic', coalesce(q.question->>'topic', ''),
            'cat_ids', coalesce((select jsonb_agg(l.category_id order by l.category_id)
                from public.bank_question_cats l where l.question_id = q.id), '[]'::jsonb),
            'cat_names', coalesce((select jsonb_agg(c.name order by lower(c.name))
                from public.bank_question_cats l join public.bank_categories c on c.id = l.category_id
                where l.question_id = q.id), '[]'::jsonb)
        ) order by q.created_at desc, q.id desc)
        from public.question_bank q where q.teacher_id = auth.uid()
    ), '[]'::jsonb)
);
$$;

-- نسخهٔ v1 هم باید در هر سطح یکتا بسنجد، نه سراسری؛ وگرنه ساختن
-- «فصل ۱» به‌عنوان ریشه پس از داشتن آن به‌عنوان فرزند رد می‌شد.
create or replace function public.native_bank_category_add_v1(p_name text)
returns jsonb
language plpgsql security definer set search_path = public, pg_temp
as $$
declare v_name text := left(btrim(coalesce(p_name, '')), 100); v_id bigint;
begin
 if auth.uid() is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
 if v_name = '' then return jsonb_build_object('error', 'نام دسته را وارد کنید'); end if;
 perform pg_advisory_xact_lock(hashtext(auth.uid()::text || ':bank-category:' || lower(v_name)));
 if exists(select 1 from public.bank_categories
            where teacher_id = auth.uid() and parent_id is null and lower(name) = lower(v_name)) then
   return jsonb_build_object('error', 'این دسته قبلاً ساخته شده است');
 end if;
 insert into public.bank_categories(teacher_id, name) values(auth.uid(), v_name) returning id into v_id;
 return jsonb_build_object('ok', true, 'id', v_id, 'name', v_name);
end $$;

-- ---------------------------------------------------------------------
-- ۸) دسترسی اجرا
-- ---------------------------------------------------------------------
grant execute on function public.native_bank_category_add_v358(text, bigint) to authenticated;
grant execute on function public.native_bank_category_move_v358(bigint, bigint) to authenticated;
grant execute on function public.native_bank_tree_v358() to authenticated;

-- =====================================================================
-- پایان. برای بررسی، SQL_QUESTION_BANK_V358_CHECK.sql را اجرا کنید.
-- =====================================================================
