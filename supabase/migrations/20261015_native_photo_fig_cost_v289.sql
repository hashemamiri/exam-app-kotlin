-- V289 — «تصویر خودم + فلش‌گذاری» (شکل اطلس با t='photo' که به‌صورت توکن %%FIG:{…}%% داخل متن سؤال/گزینه/جورکردنی ذخیره می‌شود)
-- مثل بقیهٔ تصاویر ۱۰۰۰ تومان حساب می‌شود:
--   • ذخیرهٔ آزمون آنلاین (native_save_exam_v1/v2، کپی به کلاس V264، …) از طریق native_question_media_cost_v135
--   • چاپ (native_print_due_v199 / native_print_pay_v199) از طریق native_print_q_images_v199 — با شناسهٔ پایدار photo:<md5>
--     تا همان تصویر در چاپ‌های بعدیِ همان آزمون دوباره حساب نشود (همان قاعدهٔ URLهای storage).
-- شکل‌های آمادهٔ اطلس (آناتومی/فیزیک/شیمی) رایگان می‌مانند. سؤال‌هایی که از بانک/فایل وارد می‌شوند سؤال «جدید» هستند و
-- با همین فرمول (سؤال + تصویر + صوت) هنگام ذخیره حساب می‌شوند — تغییری لازم نداشت.
begin;
set local lock_timeout = '8s';

-- همهٔ توکن‌های شکلِ «تصویر خودم» داخل هر رشتهٔ سؤال (متن، گزینه‌ها، جورکردنی، توضیح…) → آدرس تصویر هر کدام
create or replace function public.native_photo_fig_images_v289(p_q jsonb)
returns text[]
language plpgsql
immutable
set search_path = public, pg_temp
as $$
declare
    v_out text[] := '{}';
    v_s text;
    v_m text[];
    v_tok jsonb;
    v_img text;
begin
    if p_q is null or jsonb_typeof(p_q) <> 'object' then return v_out; end if;
    for v_s in select j #>> '{}' from jsonb_path_query(p_q, 'strict $.** ? (@.type() == "string")') j loop
        if v_s is null or position('%%FIG:' in v_s) = 0 then continue; end if;
        for v_m in select regexp_matches(v_s, '%%FIG:(\{[^%]*\})%%', 'g') loop
            if position('"photo"' in v_m[1]) = 0 then continue; end if;
            begin
                v_tok := v_m[1]::jsonb;
            exception when others then
                v_tok := null;
            end;
            if v_tok is null then
                -- توکن ناقص ولی از نوع photo: یک تصویر حساب می‌شود
                v_out := v_out || ('photo:' || md5(v_m[1]));
                continue;
            end if;
            if coalesce(v_tok->>'t', '') <> 'photo' then continue; end if;
            v_img := coalesce(v_tok->'X'->>'img', '');
            if btrim(v_img) = '' then continue; end if;
            v_out := v_out || ('photo:' || md5(v_img));
        end loop;
    end loop;
    return v_out;
end;
$$;
revoke all on function public.native_photo_fig_images_v289(jsonb) from public, anon, authenticated;

-- هزینهٔ رسانهٔ یک سؤال (ذخیرهٔ آنلاین): تصاویر فایل + تصاویر «تصویر خودم» + صوت
create or replace function public.native_question_media_cost_v135(p_q jsonb)
returns jsonb
language plpgsql
immutable
set search_path = public, pg_temp
as $$
declare
    v_images integer := 0;
    v_audio_bytes bigint := 0;
    v_audio_cost bigint := 0;
    v_arr jsonb;
begin
    if p_q is null or jsonb_typeof(p_q) <> 'object' then
        return jsonb_build_object('images', 0, 'image_cost', 0, 'audio_cost', 0);
    end if;
    -- تصاویر خودِ سؤال (images یا image قدیمی)
    v_arr := p_q->'images';
    if jsonb_typeof(v_arr) = 'array' then
        select count(*) into v_images from jsonb_array_elements_text(v_arr) t where btrim(coalesce(t,'')) <> '';
    elsif btrim(coalesce(p_q->>'image','')) <> '' then
        v_images := 1;
    end if;
    -- تصاویر گزینه‌ها و جورکردنی
    foreach v_arr in array array[p_q->'optionImages', p_q->'leftImages', p_q->'rightImages', p_q->'matchingLeftImages', p_q->'matchingRightImages'] loop
        if v_arr is not null and jsonb_typeof(v_arr) = 'array' then
            v_images := v_images + (select count(*) from jsonb_array_elements_text(v_arr) t where btrim(coalesce(t,'')) <> '');
        end if;
    end loop;
    -- V289 — «تصویر خودم + فلش‌گذاری» داخل متن‌ها (هر توکن یک تصویر)
    v_images := v_images + coalesce(array_length(public.native_photo_fig_images_v289(p_q), 1), 0);
    -- صوت
    if btrim(coalesce(p_q->>'audio','')) <> '' then
        v_audio_bytes := greatest(1, coalesce((p_q->>'audioBytes')::bigint, 1));
        if v_audio_bytes > 3 * 1024 * 1024 then
            raise exception 'حجم فایل صوتی یکی از سؤال‌ها بیش از ۳ مگابایت است';
        end if;
        v_audio_cost := case
            when v_audio_bytes <= 1 * 1024 * 1024 then 2000
            when v_audio_bytes <= 2 * 1024 * 1024 then 4000
            else 6000 end;
    end if;
    return jsonb_build_object('images', v_images, 'image_cost', v_images * 1000, 'audio_cost', v_audio_cost);
exception when invalid_text_representation then
    raise exception 'حجم فایل صوتی نامعتبر است';
end;
$$;

-- تصاویر یک سؤال چاپی برای محاسبهٔ بدهی چاپ: URLهای storage + شناسهٔ پایدار «تصویر خودم»
create or replace function public.native_print_q_images_v199(q jsonb)
returns text[]
language sql
immutable
set search_path = public, pg_temp
as $$
    select coalesce(array_agg(distinct u), '{}') from (
        select case when jsonb_typeof(e) = 'object' then e->>'uri' else trim(both '"' from e::text) end u
        from (
            select jsonb_array_elements(case when jsonb_typeof(q->'images')='array' then q->'images' else '[]'::jsonb end) e
            union all select to_jsonb(q->>'image') where q ? 'image'
            union all select jsonb_array_elements(case when jsonb_typeof(q->'optionImages')='array' then q->'optionImages' else '[]'::jsonb end)
            union all select jsonb_array_elements(case when jsonb_typeof(q->'leftImages')='array' then q->'leftImages' else '[]'::jsonb end)
            union all select jsonb_array_elements(case when jsonb_typeof(q->'rightImages')='array' then q->'rightImages' else '[]'::jsonb end)
            union all select jsonb_array_elements(case when jsonb_typeof(q->'matchingLeftImages')='array' then q->'matchingLeftImages' else '[]'::jsonb end)
            union all select jsonb_array_elements(case when jsonb_typeof(q->'matchingRightImages')='array' then q->'matchingRightImages' else '[]'::jsonb end)
        ) s where e is not null and jsonb_typeof(e) <> 'null'
        union all
        select unnest(public.native_photo_fig_images_v289(q)) -- V289
    ) t where u is not null and (u ~* '^https?://' or u like 'photo:%');
$$;
revoke all on function public.native_print_q_images_v199(jsonb) from public, anon, authenticated;

commit;

-- بررسی سریع (اختیاری):
-- select public.native_question_media_cost_v135('{"text":"الف %%FIG:{\"k\":\"a\",\"t\":\"photo\",\"X\":{\"img\":\"data:image/png;base64,AAAA\"}}%% ب","options":["%%FIG:{\"k\":\"a\",\"t\":\"photo\",\"X\":{\"img\":\"data:image/png;base64,BBBB\"}}%%","%%FIG:{\"k\":\"a\",\"t\":\"heart\",\"X\":{}}%%"]}'::jsonb);
-- انتظار: images = 2 ، image_cost = 2000
