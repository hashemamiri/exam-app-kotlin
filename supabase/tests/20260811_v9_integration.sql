-- فقط برای PostgreSQL محلی/CI. این فایل را روی پروژه واقعی اجرا نکنید.
-- پیش‌نیاز: schema پایه تست و سپس migration V9 اجرا شده باشد.

create or replace function pg_temp.assert_true(p_condition boolean, p_message text)
returns void language plpgsql as $$
begin
    if not coalesce(p_condition, false) then raise exception 'ASSERT: %', p_message; end if;
end;
$$;

insert into auth.users(id) values
('11111111-1111-1111-1111-111111111111'),
('22222222-2222-2222-2222-222222222222'),
('33333333-3333-3333-3333-333333333333'),
('44444444-4444-4444-4444-444444444444')
on conflict do nothing;
insert into public.profiles(id,full_name,username,role,teacher_id) values
('11111111-1111-1111-1111-111111111111','معلم تست','teacher_test','teacher',null),
('22222222-2222-2222-2222-222222222222','دانش‌آموز عضو','student_one','student','11111111-1111-1111-1111-111111111111'),
('33333333-3333-3333-3333-333333333333','دانش‌آموز بیرون کلاس','student_two','student','11111111-1111-1111-1111-111111111111'),
('44444444-4444-4444-4444-444444444444','معلم دیگر','teacher_other','teacher',null)
on conflict (id) do nothing;
insert into public.classes(id,teacher_id,name,grade) values
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa','11111111-1111-1111-1111-111111111111','کلاس تست','هفتم')
on conflict do nothing;
insert into public.class_members(class_id,student_id) values
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa','22222222-2222-2222-2222-222222222222')
on conflict do nothing;

select set_config('request.jwt.claim.sub','11111111-1111-1111-1111-111111111111',false);

-- پروفایل: مسیر مالک پذیرفته و مسیر حساب دیگر رد شود.
select pg_temp.assert_true(
    (public.native_save_profile(
        'آقای آزمون',
        'https://example.supabase.co/storage/v1/object/public/exam-images/avatars/11111111-1111-1111-1111-111111111111/a.webp',
        true,'تهران','تهران','منطقه ۱','مدرسه تست'
    )->>'ok')::boolean,
    'profile save'
);
select pg_temp.assert_true(
    public.native_save_profile(
        'آقای آزمون',
        'https://example.supabase.co/storage/v1/object/public/exam-images/avatars/22222222-2222-2222-2222-222222222222/x.webp',
        true,'تهران','تهران','منطقه ۱','مدرسه تست'
    ) ? 'error',
    'foreign avatar rejected'
);
select pg_temp.assert_true(public.native_my_profile()->>'hdr_school' = 'مدرسه تست', 'header persisted');

-- تقویم: پیام کلاسی فقط برای عضو کلاس دیده شود.
do $$
declare v_note jsonb; v_id uuid; v_month jsonb;
begin
    v_note := public.cal_save_note(
        date '2026-08-11','یادآوری آزمون','فردا آماده باشید','classes',
        array['aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'::uuid],null,null
    );
    perform pg_temp.assert_true((v_note->>'ok')::boolean, 'calendar note save');
    v_id := (v_note->>'id')::uuid;
    perform pg_temp.assert_true(jsonb_array_length(public.cal_day(v_id)->'classes') = 1, 'calendar audience persisted');

    perform set_config('request.jwt.claim.sub','22222222-2222-2222-2222-222222222222',false);
    v_month := public.cal_month(date '2026-08-01', date '2026-08-31');
    perform pg_temp.assert_true(jsonb_array_length(v_month->'notes') = 1, 'class member sees note');

    perform set_config('request.jwt.claim.sub','33333333-3333-3333-3333-333333333333',false);
    v_month := public.cal_month(date '2026-08-01', date '2026-08-31');
    perform pg_temp.assert_true(jsonb_array_length(v_month->'notes') = 0, 'non member cannot see note');

    perform set_config('request.jwt.claim.sub','11111111-1111-1111-1111-111111111111',false);
    perform pg_temp.assert_true(
        public.cal_save_note(date '2026-08-12','بد','', 'classes',
            array['bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'::uuid],null,null) ? 'error',
        'foreign class rejected'
    );
end;
$$;
select pg_temp.assert_true(
    exists(select 1 from public.holidays where on_date = date '2026-08-12' and jy=1405 and jm=5 and jd=21),
    'current official holiday exists'
);
select pg_temp.assert_true((public.holidays_for(date '2026-08-01',date '2026-08-31')->'years'->>'1405')::boolean, '1405 exact');

-- موجودی اولیه برای ذخیره آزمون.
insert into public.wallets(user_id,balance) values('11111111-1111-1111-1111-111111111111',20000)
on conflict (user_id) do update set balance=excluded.balance;

-- ساخت: دو سؤال = ۲۰۰۰ تومان.
do $$
declare p jsonb; r jsonb; again jsonb;
begin
    p := jsonb_build_object(
      'operation_id','90000000-0000-0000-0000-000000000001',
      'id','exam-v9','code','V9TEST','title','آزمون V9','subject','ریاضی','duration',30,
      'total_score',2,'shuffle_q',false,'shuffle_opt',false,'neg_marking',0,
      'teacher_message','موفق باشید','attempts_allowed',1,'attempt_on_timeout',true,
      'grade_policy','last','attempt_cooldown',0,
      'questions',jsonb_build_array(
        jsonb_build_object('id','q1','type','essay','text','سؤال ۱','score',1),
        jsonb_build_object('id','q2','type','essay','text','سؤال ۲','score',1)
      ),
      'answer_key',jsonb_build_array(jsonb_build_object('i',0),jsonb_build_object('i',1)),
      'audience','all','classes','[]'::jsonb,'students','[]'::jsonb
    );
    r := public.native_save_exam_v1(p);
    perform pg_temp.assert_true((r->>'ok')::boolean and (r->>'cost')::bigint=2000 and (r->>'balance')::bigint=18000, 'create charge');
    again := public.native_save_exam_v1(p);
    perform pg_temp.assert_true((again->>'idempotent')::boolean and (again->>'balance')::bigint=18000, 'create idempotency');
end;
$$;

-- بدون پاسخ: تغییر سؤال قبلی رایگان، فقط سؤال افزوده = ۱۰۰۰.
do $$
declare p jsonb; r jsonb;
begin
    p := jsonb_build_object(
      'operation_id','90000000-0000-0000-0000-000000000002',
      'id','exam-v9','code','V9TEST','title','آزمون V9','subject','ریاضی','duration',30,'total_score',3,
      'shuffle_q',false,'shuffle_opt',false,'neg_marking',0,'teacher_message','',
      'attempts_allowed',1,'attempt_on_timeout',true,'grade_policy','last','attempt_cooldown',0,
      'questions',jsonb_build_array(
        jsonb_build_object('id','q1','type','essay','text','سؤال ۱ ویرایش رایگان','score',1),
        jsonb_build_object('id','q2','type','essay','text','سؤال ۲','score',1),
        jsonb_build_object('id','q3','type','essay','text','سؤال ۳ جدید','score',1)
      ),
      'answer_key',jsonb_build_array(jsonb_build_object('i',0),jsonb_build_object('i',1),jsonb_build_object('i',2)),
      'audience','all','classes','[]'::jsonb,'students','[]'::jsonb
    );
    r := public.native_save_exam_v1(p);
    perform pg_temp.assert_true((r->>'cost')::bigint=1000 and (r->>'balance')::bigint=17000, 'only added charged before answers');
end;
$$;

-- پس از پاسخ: سؤال تغییرکرده مشمول هزینه است.
insert into public.answers(id,exam_id,student_id) values('answer-v9','exam-v9','22222222-2222-2222-2222-222222222222');
do $$
declare p jsonb; r jsonb;
begin
    p := jsonb_build_object(
      'operation_id','90000000-0000-0000-0000-000000000003',
      'id','exam-v9','code','V9TEST','title','آزمون V9','subject','ریاضی','duration',30,'total_score',3,
      'shuffle_q',false,'shuffle_opt',false,'neg_marking',0,'teacher_message','',
      'attempts_allowed',1,'attempt_on_timeout',true,'grade_policy','last','attempt_cooldown',0,
      'questions',jsonb_build_array(
        jsonb_build_object('id','q1','type','essay','text','سؤال ۱ تغییر پس از پاسخ','score',1),
        jsonb_build_object('id','q2','type','essay','text','سؤال ۲','score',1),
        jsonb_build_object('id','q3','type','essay','text','سؤال ۳ جدید','score',1)
      ),
      'answer_key',jsonb_build_array(jsonb_build_object('i',0),jsonb_build_object('i',1),jsonb_build_object('i',2)),
      'audience','all','classes','[]'::jsonb,'students','[]'::jsonb
    );
    r := public.native_save_exam_v1(p);
    perform pg_temp.assert_true((r->>'cost')::bigint=1000 and (r->>'balance')::bigint=16000, 'changed charged after answers');
end;
$$;

-- کسری موجودی هیچ تغییری در آزمون نمی‌دهد.
update public.wallets set balance=0 where user_id='11111111-1111-1111-1111-111111111111';
do $$
declare p jsonb; r jsonb;
begin
    p := jsonb_build_object(
      'operation_id','90000000-0000-0000-0000-000000000004',
      'id','exam-v9','code','V9TEST','title','نباید ذخیره شود','subject','ریاضی','duration',30,'total_score',4,
      'shuffle_q',false,'shuffle_opt',false,'neg_marking',0,'teacher_message','',
      'attempts_allowed',1,'attempt_on_timeout',true,'grade_policy','last','attempt_cooldown',0,
      'questions',jsonb_build_array(
        jsonb_build_object('id','q1','type','essay','text','تغییر دیگر','score',1),
        jsonb_build_object('id','q2','type','essay','text','سؤال ۲','score',1),
        jsonb_build_object('id','q3','type','essay','text','سؤال ۳ جدید','score',1),
        jsonb_build_object('id','q4','type','essay','text','سؤال ۴','score',1)
      ),
      'answer_key','[]'::jsonb,'audience','all','classes','[]'::jsonb,'students','[]'::jsonb
    );
    r := public.native_save_exam_v1(p);
    perform pg_temp.assert_true(r ? 'error', 'insufficient rejected');
    perform pg_temp.assert_true((select title='آزمون V9' from public.exams where id='exam-v9'), 'failed save preserved exam');
end;
$$;

-- تکثیر اتمیک و idempotent: سه سؤال = ۳۰۰۰.
update public.wallets set balance=10000 where user_id='11111111-1111-1111-1111-111111111111';
do $$
declare r jsonb; again jsonb;
begin
    r := public.native_duplicate_exam_v2('exam-v9','90000000-0000-0000-0000-000000000005');
    perform pg_temp.assert_true((r->>'cost')::bigint=3000 and (r->>'balance')::bigint=7000, 'duplicate charged');
    again := public.native_duplicate_exam_v2('exam-v9','90000000-0000-0000-0000-000000000005');
    perform pg_temp.assert_true((again->>'idempotent')::boolean and (select count(*)=2 from public.exams), 'duplicate idempotency');
end;
$$;

-- پرداخت: اعتبار فقط یک بار و از تابع service-only ثبت می‌شود.
do $$
declare r jsonb; oid bigint; credit jsonb; again jsonb;
begin
    r := public.native_create_wallet_payment_order(
      '11111111-1111-1111-1111-111111111111',100000,'sandbox');
    perform pg_temp.assert_true((r->>'ok')::boolean, 'payment order create');
    oid := (r->>'id')::bigint;
    perform pg_temp.assert_true((public.native_set_wallet_payment_authority(oid,'SB-TEST')->>'ok')::boolean, 'authority set');
    credit := public.native_credit_wallet_payment(oid,'REF-TEST-1');
    perform pg_temp.assert_true((credit->>'balance')::bigint=107000, 'payment credited');
    again := public.native_credit_wallet_payment(oid,'REF-TEST-1');
    perform pg_temp.assert_true((again->>'already_paid')::boolean and (again->>'balance')::bigint=107000, 'payment idempotency');
end;
$$;

-- grants حساس.
select pg_temp.assert_true(not has_function_privilege('authenticated','public.wallet_topup(bigint)','execute'), 'direct topup revoked');
select pg_temp.assert_true(not has_function_privilege('authenticated','public.wallet_refund(bigint,text)','execute'), 'direct refund revoked');
select pg_temp.assert_true(not has_function_privilege('authenticated','public.native_credit_wallet_payment(bigint,text)','execute'), 'credit hidden from app');
select pg_temp.assert_true(has_function_privilege('service_role','public.native_credit_wallet_payment(bigint,text)','execute'), 'service can credit');
select pg_temp.assert_true(has_function_privilege('authenticated','public.native_save_exam_v1(jsonb)','execute'), 'native save granted');

select 'V9_SQL_INTEGRATION_PASS' as result;
