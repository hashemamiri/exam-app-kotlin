-- Isolated regression after V13 migration.
create or replace function pg_temp.assert_true(p_ok boolean,p_msg text) returns void language plpgsql as $$begin if not coalesce(p_ok,false)then raise exception 'ASSERT: %',p_msg;end if;end$$;

insert into auth.users(id,email)values('91000000-0000-0000-0000-000000000001','parity@example.test')on conflict do nothing;
insert into public.profiles(id,full_name,role)values('91000000-0000-0000-0000-000000000001','معلم parity','teacher')on conflict do nothing;
set role authenticated;select set_config('request.jwt.claim.sub','91000000-0000-0000-0000-000000000001',false);

select pg_temp.assert_true((public.native_save_exam_v2('{"operation_id":"91000000-0000-0000-0000-000000000010","id":"parity-exam","code":"PAR123","title":"زمان‌بندی","subject":"ریاضی","duration":30,"opens_at":"2026-08-12T10:00:00Z","closes_at":"2026-08-12T11:00:00Z","questions":[{"id":"q1","type":"essay","text":"سؤال"}],"answer_key":[{"i":0}],"audience":"all","classes":[],"students":[]}'::jsonb)->>'ok')::boolean,'schedule wrapper saves');
select pg_temp.assert_true((select opens_at='2026-08-12T10:00:00Z'::timestamptz and closes_at='2026-08-12T11:00:00Z'::timestamptz from public.exams where id='parity-exam'),'schedule stored');
select pg_temp.assert_true(public.native_save_exam_v2('{"operation_id":"91000000-0000-0000-0000-000000000011","id":"bad","code":"BAD12","title":"بد","questions":[{"text":"x"}],"answer_key":[],"audience":"all","opens_at":"2026-08-12T12:00:00Z","closes_at":"2026-08-12T11:00:00Z"}'::jsonb)?'error','invalid window rejected');

select pg_temp.assert_true((public.native_bank_category_add_v1('فصل اول')->>'ok')::boolean,'category add');
select pg_temp.assert_true((public.native_bank_category_add_v1('فصل دوم')->>'ok')::boolean,'second category add');
do $$declare c1 bigint;c2 bigint;q bigint;r jsonb;begin
 select min(id),max(id) into c1,c2 from public.bank_categories where teacher_id=auth.uid();
 r:=public.native_bank_add_v2('{"id":"bank-q","type":"fill","text":"پایتخت؟","accept":["تهران"],"caseSensitive":true}'::jsonb,'جغرافیا',jsonb_build_array(c1,c2));
 perform pg_temp.assert_true((r->>'ok')::boolean,'categorized bank add');q:=(r->>'id')::bigint;
 perform pg_temp.assert_true(jsonb_array_length(public.native_bank_snapshot_v1()->'items')=1,'snapshot item');
 perform pg_temp.assert_true(public.native_bank_add_v2('{"id":"bank-q","type":"fill","text":"پایتخت؟","accept":["تهران"],"caseSensitive":true}'::jsonb,'جغرافیا',null)?'duplicate','duplicate rejected');
 perform pg_temp.assert_true((public.native_bank_set_categories_v1(q,jsonb_build_array(c1))->>'ok')::boolean,'category move');
end$$;

reset role;
insert into public.feedback_bank(teacher_id,text)values('91000000-0000-0000-0000-000000000001','قدیمی');
set role authenticated;select set_config('request.jwt.claim.sub','91000000-0000-0000-0000-000000000001',false);
do $$declare f bigint:=1;begin perform pg_temp.assert_true((public.native_feedback_update_v1(f,'جدید')->>'ok')::boolean,'feedback update');perform pg_temp.assert_true((public.native_feedback_delete_v1(f)->>'ok')::boolean,'feedback delete');end$$;
select pg_temp.assert_true((public.native_parity_status_v1()->>'schedule_save_ready')::boolean,'parity status');
reset role;
select pg_temp.assert_true(not has_function_privilege('anon','public.native_bank_add_v2(jsonb,text,jsonb)','EXECUTE'),'anon bank denied');
select pg_temp.assert_true(has_function_privilege('authenticated','public.native_bank_add_v2(jsonb,text,jsonb)','EXECUTE'),'authenticated bank allowed');
select 'V13_FULL_PARITY_PASS' result;
