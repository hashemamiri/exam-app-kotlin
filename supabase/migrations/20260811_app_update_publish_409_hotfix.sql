-- Hotfix V4.1 برای HTTP 409 هنگام فعال‌سازی نسخه
-- علت سازگاری: بعضی schemaهای قدیمی app_version یک id ثابت/singleton دارند.
-- این تابع جدول را به‌عنوان «نسخه جاری» نگه می‌دارد و در یک تراکنش جایگزین می‌کند.

begin;

create or replace function public.publish_app_update(
    p_version_code integer,
    p_version_name text,
    p_notes_fa jsonb,
    p_apk_url text,
    p_apk_sha256 text,
    p_apk_size_bytes bigint,
    p_is_required boolean default false
)
returns public.app_version
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_current_code integer;
    v_release public.app_version;
begin
    if auth.role() <> 'service_role' then
        raise exception 'release publisher role is not allowed'
            using errcode = '42501';
    end if;

    if p_version_code is null or p_version_code <= 0 then
        raise exception 'invalid version code' using errcode = '22023';
    end if;

    if nullif(btrim(p_version_name), '') is null or length(p_version_name) > 100 then
        raise exception 'invalid version name' using errcode = '22023';
    end if;

    if p_notes_fa is null or jsonb_typeof(p_notes_fa) <> 'array' then
        raise exception 'release notes must be a JSON array' using errcode = '22023';
    end if;

    if p_apk_url !~ '^https://eazwuyrymsvdkwckdpco[.]supabase[.]co/storage/v1/object/public/app-updates/[A-Za-z0-9._-]+[.]apk$' then
        raise exception 'invalid APK public URL' using errcode = '22023';
    end if;

    if p_apk_sha256 !~ '^[A-Fa-f0-9]{64}$' then
        raise exception 'invalid APK SHA-256' using errcode = '22023';
    end if;

    if p_apk_size_bytes is null or p_apk_size_bytes <= 0 then
        raise exception 'invalid APK size' using errcode = '22023';
    end if;

    select max(version_code)
    into v_current_code
    from public.app_version
    where is_active = true;

    if v_current_code is not null and p_version_code < v_current_code then
        raise exception 'version code must not be lower than active release'
            using errcode = '22023';
    end if;

    -- اگر INSERT شکست بخورد، DELETE نیز به‌علت تراکنش rollback می‌شود.
    delete from public.app_version;

    insert into public.app_version (
        version_code,
        version_name,
        notes_fa,
        apk_url,
        apk_sha256,
        apk_size_bytes,
        is_required,
        is_active,
        published_at
    ) values (
        p_version_code,
        btrim(p_version_name),
        p_notes_fa,
        p_apk_url,
        lower(p_apk_sha256),
        p_apk_size_bytes,
        coalesce(p_is_required, false),
        true,
        now()
    )
    returning * into v_release;

    if not found then
        raise exception 'release activation failed';
    end if;

    return v_release;
end;
$$;

revoke all on function public.publish_app_update(
    integer, text, jsonb, text, text, bigint, boolean
) from public, anon, authenticated;

grant execute on function public.publish_app_update(
    integer, text, jsonb, text, text, bigint, boolean
) to service_role;

commit;

notify pgrst, 'reload schema';
