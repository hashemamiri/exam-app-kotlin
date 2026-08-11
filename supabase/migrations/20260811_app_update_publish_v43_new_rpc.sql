-- Hotfix V4.3: نام RPC کاملاً جدید برای حذف metadata قدیمی PostgREST
-- فراخوانی مستقیم publish_app_update در SQL موفق بود، اما مسیر REST همچنان 21000 داد.
-- این تابع مستقل است و به تابع قبلی وابسته نیست.

begin;

create or replace function public.publish_native_app_release_v1(
    p_version_code integer,
    p_version_name text,
    p_notes_fa jsonb,
    p_apk_url text,
    p_apk_sha256 text,
    p_apk_size_bytes bigint,
    p_is_required boolean default false
)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_inserted_rows integer;
begin
    if auth.role() <> 'service_role' then
        raise exception 'release publisher role is not allowed'
            using errcode = '42501';
    end if;

    perform pg_advisory_xact_lock(208585453);

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

    if exists (
        select 1
        from public.app_version
        where is_active = true
          and version_code > p_version_code
    ) then
        raise exception 'version code must not be lower than active release'
            using errcode = '22023';
    end if;

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
    );

    get diagnostics v_inserted_rows = row_count;
    if v_inserted_rows <> 1 then
        raise exception 'unexpected inserted row count'
            using errcode = 'P0001';
    end if;

    return jsonb_build_object(
        'ok', true,
        'version_code', p_version_code,
        'version_name', btrim(p_version_name)
    );
end;
$$;

revoke all on function public.publish_native_app_release_v1(
    integer, text, jsonb, text, text, bigint, boolean
) from public, anon, authenticated;

grant execute on function public.publish_native_app_release_v1(
    integer, text, jsonb, text, text, bigint, boolean
) to service_role;

commit;

notify pgrst, 'reload schema';
