-- V59.1 — حذف کامل حساب معلم/مدیر با انتقال مالکیت دانش‌آموزان مشترک.
-- قاعدهٔ کاربر: دانش‌آموزی که «توسط این حساب ساخته شده» ولی در لیست حساب
-- دیگری هم هست (teacher_student_links)، حذف نمی‌شود؛ مالکیت کامل
-- (profiles.teacher_id — و در نتیجه کنترل رمز از مسیر manage-student) به آن
-- حساب منتقل می‌شود. دانش‌آموزی که فقط مال همین حساب است حذف می‌شود.
-- خود ردیف auth.users متقاضی و دانش‌آموزانِ حذف‌شدنی را Edge function با
-- service role پاک می‌کند؛ این تابع «آماده‌سازی» را اتمیک انجام می‌دهد و
-- فهرست شناسه‌های حذف‌شدنی را برمی‌گرداند.

create or replace function public.native_prepare_account_deletion_v1(p_actor uuid default null)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    -- Edge function با service role صدا می‌زند (auth.uid() = null) و p_actor
    -- می‌فرستد؛ فراخوان مستقیم کاربر p_actor را نادیده می‌گیرد.
    v_uid uuid := coalesce(auth.uid(), p_actor);
    v_role text;
    v_transferred integer := 0;
    v_deletable uuid[];
begin
    if v_uid is null then return jsonb_build_object('error', 'ابتدا وارد شوید'); end if;
    select role into v_role from public.profiles where id = v_uid;
    if v_role not in ('teacher', 'manager') then
        return jsonb_build_object('error', 'حذف حساب فقط برای کادر مدرسه فعال است');
    end if;

    -- ۱) انتقال مالکیت: دانش‌آموزانِ ساختهٔ این حساب که در لیست حساب دیگری هستند.
    --    مالک جدید = قدیمی‌ترین لینک فعال غیر از خود متقاضی.
    with shared as (
        select p.id as student_id,
               (select l.teacher_id from public.teacher_student_links l
                 where l.student_id = p.id and l.teacher_id <> v_uid
                 order by l.created_at asc limit 1) as new_owner
        from public.profiles p
        where p.role = 'student' and p.teacher_id = v_uid
    ), moved as (
        update public.profiles p
        set teacher_id = s.new_owner
        from shared s
        where p.id = s.student_id and s.new_owner is not null
        returning p.id
    )
    select count(*) into v_transferred from moved;

    -- لینک‌های متقاضی از دانش‌آموزان منتقل‌شده پاک می‌شود.
    delete from public.teacher_student_links
    where teacher_id = v_uid;

    -- ۲) دانش‌آموزانی که هنوز مال متقاضی‌اند (لیست دیگری نداشتند) حذف‌شدنی‌اند.
    select coalesce(array_agg(p.id), '{}') into v_deletable
    from public.profiles p
    where p.role = 'student' and p.teacher_id = v_uid;

    -- ۳) کلاس‌های ساختهٔ متقاضی حذف می‌شوند (اول عضویت‌ها، بعد خود کلاس‌ها).
    delete from public.class_members cm
    using public.classes c
    where cm.class_id = c.id and c.teacher_id = v_uid;
    delete from public.classes where teacher_id = v_uid;

    return jsonb_build_object(
        'ok', true,
        'transferred', v_transferred,
        'deletable_students', to_jsonb(coalesce(v_deletable, '{}'))
    );
end;
$$;

-- نکتهٔ امنیتی: بدون نشست، p_actor فقط از service role (Edge) پذیرفته می‌شود.
revoke all on function public.native_prepare_account_deletion_v1(uuid) from public, anon, authenticated;
grant execute on function public.native_prepare_account_deletion_v1(uuid) to service_role;
