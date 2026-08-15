-- V40A: مشخصات تکمیلی معلم؛ فقط خود معلم می‌خواند/می‌نویسد.
begin;
alter table public.profiles add column if not exists employee_code text;
alter table public.profiles add column if not exists phone text;

create or replace function public.native_my_teacher_details_v40()
returns jsonb language sql stable security definer set search_path=public,pg_temp as $$
 select coalesce((select jsonb_build_object('ok',true,'first_name',coalesce(first_name,''),'last_name',coalesce(last_name,''),'employee_code',coalesce(employee_code,''),'phone',coalesce(phone,'')) from public.profiles where id=auth.uid() and role='teacher'),jsonb_build_object('ok',true,'first_name','','last_name','','employee_code','','phone',''));
$$;

create or replace function public.native_save_teacher_details_v40(p_first_name text,p_last_name text,p_employee_code text,p_phone text)
returns jsonb language plpgsql security definer set search_path=public,pg_temp as $$
declare v_first text:=left(btrim(coalesce(p_first_name,'')),100); v_last text:=left(btrim(coalesce(p_last_name,'')),100); v_code text:=upper(btrim(coalesce(p_employee_code,''))); v_phone text:=btrim(coalesce(p_phone,'')); v_count int;
begin
 if auth.uid() is null then return jsonb_build_object('error','ابتدا وارد شوید'); end if;
 if v_code<>'' and (char_length(v_code)>30 or v_code !~ '^[A-Z0-9_-]+$') then return jsonb_build_object('error','کد پرسنلی حداکثر ۳۰ حرف انگلیسی، عدد، خط تیره یا زیرخط است'); end if;
 if v_phone<>'' and v_phone !~ '^09[0-9]{9}$' then return jsonb_build_object('error','شماره تلفن باید ۱۱ رقم و با 09 شروع شود'); end if;
 update public.profiles set first_name=nullif(v_first,''),last_name=nullif(v_last,''),full_name=coalesce(nullif(concat_ws(' ',nullif(v_first,''),nullif(v_last,'')),''),full_name),employee_code=nullif(v_code,''),phone=nullif(v_phone,'') where id=auth.uid() and role='teacher'; get diagnostics v_count=row_count;
 if v_count<>1 then return jsonb_build_object('error','فقط معلم می‌تواند مشخصات معلم را ذخیره کند'); end if;
 return jsonb_build_object('ok',true,'first_name',v_first,'last_name',v_last,'employee_code',v_code,'phone',v_phone);
end $$;

revoke all on function public.native_my_teacher_details_v40() from public,anon;
revoke all on function public.native_save_teacher_details_v40(text,text,text,text) from public,anon;
grant execute on function public.native_my_teacher_details_v40(),public.native_save_teacher_details_v40(text,text,text,text) to authenticated;
commit;
