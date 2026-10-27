import {fileURLToPath} from 'node:url';
const {PGlite}=await import(process.env.PGLITE_MODULE || '@electric-sql/pglite');
import fs from 'node:fs';
const root=fileURLToPath(new URL('.',import.meta.url));
const repo=fileURLToPath(new URL('../../',import.meta.url));const db=new PGlite();
await db.exec(`
CREATE SCHEMA auth;
CREATE FUNCTION auth.uid() RETURNS uuid LANGUAGE sql STABLE AS $$ SELECT nullif(current_setting('test.uid',true),'')::uuid $$;
CREATE TABLE exams(id text primary key,teacher_id uuid,questions jsonb,total_score numeric,neg_marking numeric,is_open boolean default true,duration integer default 60,pick_count integer default 0,title text default 'Synthetic exam',subject text default 'Test');
CREATE TABLE profiles(id uuid primary key,full_name text);
CREATE TABLE answers(id text primary key,exam_id text,student_id uuid,student_name text,responses jsonb,response_images jsonb default '{}',grades jsonb,total_grade numeric,graded boolean default false,auto_graded boolean default false,feedback text,graded_at text,submitted_at timestamptz,meta jsonb default '{}',attempt_no integer,part_grades jsonb default '{}',part_feedback jsonb default '{}');
CREATE TABLE exam_keys(exam_id text primary key,answers jsonb);
CREATE TABLE native_grade_progress(answer_id text,question_index integer,teacher_id uuid,updated_at timestamptz,PRIMARY KEY(answer_id,question_index));
CREATE TABLE exam_attempts(id bigint generated always as identity primary key,exam_id text,student_id uuid,student_name text,attempt_no integer,started_at timestamptz,expires_at timestamptz,submitted_at timestamptz,abandoned boolean default false,shown_q jsonb,section_deadlines jsonb default '{}',section_locked_responses jsonb default '{}',unique(exam_id,student_id,attempt_no));
CREATE TABLE exam_sessions(exam_id text,student_id uuid);
CREATE FUNCTION attempt_state(text,uuid) RETURNS TABLE(can_start boolean,reason text,used integer,max_allowed integer) LANGUAGE sql AS $$SELECT true,''::text,0,5$$;
`);
await db.exec('create role anon; create role authenticated;');
await db.exec(Buffer.from(JSON.parse(fs.readFileSync(root+'fixtures/grading_before_v335_37.json','utf8')).sql_base64,'base64').toString('utf8'));
if(process.env.GRADING_BASELINE==='v33536') await db.exec(Buffer.from(JSON.parse(fs.readFileSync(root+'fixtures/user_v335_36_sql.json','utf8')).sql_base64,'base64').toString('utf8'));
const migration=fs.readFileSync(repo+'supabase/migrations/20261027_native_negative_grading_v335_37.sql','utf8');
await db.exec(migration);
await db.exec(migration); // idempotent, guard accepts its own installed bodies

const T='11111111-1111-4111-8111-111111111111',U='22222222-2222-4222-8222-222222222222';
const q=async(sql,args=[]) => (await db.query(sql,args)).rows;
const rpc=async(sql,args=[]) => (await q('select '+sql+' as r',args))[0].r;
const uid=async(x)=>q("select set_config('test.uid',$1,false)",[x]);
const results=[];
async function fixture({questions=[{id:'q0',type:'multiple',score:3}],keys=[{i:0,correctOption:0}],neg=1/3,responses=['1'],grades=[-1],meta={},shown=null}={}){
 await db.exec('truncate exams,answers,exam_keys,profiles,native_grade_progress,exam_attempts,exam_sessions restart identity');
 await q('insert into exams(id,teacher_id,questions,total_score,neg_marking,is_open,duration,pick_count) values($1,$2,$3,$4,$5,true,60,0)',['e',T,JSON.stringify(questions),questions.reduce((s,q)=>s+q.score,0),neg]);
 await q('insert into exam_keys values($1,$2)',['e',JSON.stringify(keys)]);
 await q('insert into profiles values($1,$2)',[U,'Synthetic student']);
 await q('insert into answers(id,exam_id,student_id,responses,grades,total_grade,meta) values($1,$2,$3,$4,$5,$6,$7)',['a','e',U,JSON.stringify(responses),JSON.stringify(grades),grades.reduce((s,g)=>s+g,0),JSON.stringify(meta)]);
 await q('insert into exam_attempts(exam_id,student_id,attempt_no,started_at,expires_at,shown_q) values($1,$2,1,now(),now()+interval \'1 hour\',$3)',['e',U,shown?JSON.stringify(shown):null]);
 await uid(T);
}
async function row(){return (await q("select grades,total_grade,graded,auto_graded from answers where id='a'"))[0]}
async function test(id,area,desired,fn){try{let got=await fn();let pass=typeof desired==='function'?desired(got):JSON.stringify(got)===JSON.stringify(desired);results.push({id,area,meets_requirement:pass,observed:got});}catch(e){results.push({id,area,meets_requirement:false,execution_error:e.message});}}
await test('S01','save legitimate negative score',v=>v.accepted===true,async()=>{await fixture();const r=await rpc("native_save_grade('a','[-1]'::jsonb,'')");return {accepted:!!r.ok,error:r.error}});
await test('S02','parts wrapper preserves negative MCQ',v=>v.accepted,async()=>{await fixture();let r=await rpc("native_save_grade_parts_v1('a','[-1]'::jsonb,'','{}','{}')");return {accepted:!!r.ok,error:r.error}});
await test('S03','bulk save legitimate negative score',v=>v.accepted,async()=>{await fixture();let r=await rpc(`native_bulk_save_question_grades_v1('e',0,'[{"answer_id":"a","score":-1}]')`);return {accepted:!!r.ok,error:r.error}});
await test('S04','automatic approval preserves penalty',v=>Math.abs(v.grade+1)<1e-8,async()=>{await fixture();let r=await rpc("approve_auto_grades('e','auto_only')");let a=await row();return {rpc:r,grade:Number(a.grades[0]),total:Number(a.total_grade)}});
await test('S05','finalization preserves penalty',v=>Math.abs(v.grade+1)<1e-8,async()=>{await fixture();let r=await rpc("native_finalize_bulk_grades_v1('e')");let a=await row();return {rpc:r,grade:Number(a.grades[0]),total:Number(a.total_grade)}});
for(const [id,resp,expected] of [['S06','0',3],['S07','1',-1],['S08','',0]]){
 await test(id,'submit MCQ correct/wrong/blank',v=>Math.abs(v.grade-expected)<1e-8,async()=>{await fixture();await q("delete from answers");await uid(U);let r=await rpc("submit_answer('e',$1::jsonb,'{}','{}')",[JSON.stringify([resp])]);let a=(await q('select grades,total_grade from answers'))[0];return {rpc:r,grade:Number(a?.grades[0]),total:Number(a?.total_grade)}});
}
await test('S09','submit floor total, preserve raw question penalties',v=>v.total===Math.max(0,v.sum),async()=>{await fixture();await q('delete from answers');await uid(U);await rpc(`submit_answer('e','["1"]','{}','{}')`);let a=(await q('select grades,total_grade from answers'))[0];return {sum:Number(a.grades[0]),total:Number(a.total_grade)}});
await test('S10','no penalty when disabled',v=>v.grade===0,async()=>{await fixture({neg:0});let r=await rpc("approve_auto_grades('e')");return {rpc:r,grade:Number((await row()).grades[0])}});
await test('S11','correct score approval',v=>v.grade===3,async()=>{await fixture({responses:['0'],grades:[3]});await rpc("approve_auto_grades('e')");return {grade:Number((await row()).grades[0])}});
await test('S12','blank score approval',v=>v.grade===0,async()=>{await fixture({responses:[''],grades:[0]});await rpc("approve_auto_grades('e')");return {grade:Number((await row()).grades[0])}});
await test('S13','mixed essay saves with existing negative objective',v=>v.accepted,async()=>{await fixture({questions:[{id:'q0',type:'multiple',score:3},{id:'q1',type:'essay',score:5}],keys:[{i:0,correctOption:0}],responses:['1','text'],grades:[-1,4]});let r=await rpc(`native_save_grade_parts_v1('a','[-1,4]','','{}','{}')`);return {accepted:!!r.ok,error:r.error}});
await test('S14','structured essay stays nonnegative',v=>!!v.error,async()=>{await fixture({questions:[{id:'q0',type:'essay',score:5,assessmentParts:[{id:'p',score:5}]}],keys:[],grades:[0]});return rpc(`native_save_grade_parts_v1('a','[-1]','','{"0:0":-1}','{}')`)});
await test('S15','reject score above maximum',v=>!!v.error,async()=>{await fixture();return rpc(`native_save_grade('a','[4]','')`)});
await test('S16','reject foreign teacher',v=>!!v.error,async()=>{await fixture();await uid(U);return rpc(`native_save_grade('a','[2]','')`)});
await test('S17','reject anonymous caller',v=>!!v.error,async()=>{await fixture();await uid('');return rpc(`native_save_grade('a','[2]','')`)});
await test('S18','reject wrong grade array length',v=>!!v.error,async()=>{await fixture();return rpc(`native_save_grade('a','[1,2]','')`)});
await test('S19','reject null grade element',v=>!!v.error,async()=>{await fixture();let r=await rpc(`native_save_grade('a','[null]','')`);return {error:r.error,accepted:!!r.ok,state:await row()}});
await test('S20','negative question displayed, final total floored',v=>Math.abs(v.grade+1)<1e-8&&v.total===0,async()=>{await fixture();await q("update answers set graded=true,total_grade=0 where id='a'");await uid(U);let r=await rpc(`native_my_answer_detail_parts_v1('a')`);return {grade:Number(r.grades?.[0]),total:Number(r.total_grade),error:r.error}});
await test('S21','review hides correct key while ungraded',v=>!v.leaks,async()=>{await fixture();await uid(U);let r=await rpc(`native_my_answer_detail_parts_v1('a')`);return {leaks:JSON.stringify(r.questions).includes('correctOption'),error:r.error}});
await test('S22','manually finalized scores not reapproved',v=>v.grade===2,async()=>{await fixture({grades:[2]});await q("update answers set graded=true,auto_graded=false where id='a'");await rpc(`approve_auto_grades('e')`);return {grade:Number((await row()).grades[0])}});
await test('S23','random subset does not score hidden question',v=>v.hidden===0,async()=>{await fixture({questions:[{id:'q0',type:'multiple',score:3},{id:'q1',type:'multiple',score:3}],keys:[{i:0,correctOption:0},{i:1,correctOption:0}],responses:['0','0'],grades:[3,0],meta:{shown_q:[0],max_score:3},shown:[0]});await rpc(`approve_auto_grades('e')`);return {hidden:Number((await row()).grades[1]),state:await row()}});
await test('S24','atomic bulk rejection changes no earlier row',v=>v.after===v.before&&!!v.error,async()=>{await fixture({grades:[2]});let before=JSON.stringify(await row());let r=await rpc(`native_bulk_save_question_grades_v1('e',0,'[{"answer_id":"a","score":1},{"answer_id":"foreign","score":2}]')`);return {before,after:JSON.stringify(await row()),error:r.error}});
await test('S25','partial finalization does not change progress on error',v=>v.progress===0&&!!v.error,async()=>{await fixture({questions:[{id:'q0',type:'multiple',score:3},{id:'q1',type:'essay',score:5}],keys:[{i:0,correctOption:0}],responses:['1','text'],grades:[-1,0]});let r=await rpc(`native_finalize_bulk_grades_v1('e')`);return {error:r.error,progress:Number((await q('select count(*) as n from native_grade_progress'))[0].n)}});

for(const [id,type,key,resp,expected] of [
 ['S26','truefalse',{correctAnswer:true},'false',-1],
 ['S27','truefalse',{correctAnswer:true},'',0],
 ['S28','numeric',{answer:8,tolerance:0},'7',0],
 ['S29','fill',{accept:['yes']},'no',0],
 ['S30','matching',{matchAnswer:{0:0,1:1}},{0:0,1:0},1.5]
]) await test(id,'submit penalty eligibility '+type,v=>Math.abs(v.grade-expected)<1e-8,async()=>{
 await fixture({questions:[{id:'q0',type,score:3}],keys:[{i:0,...key}]});await q('delete from answers');await uid(U);
 let r=await rpc(`submit_answer('e',$1::jsonb,'{}','{}')`,[JSON.stringify([resp])]);let a=(await q('select grades,total_grade from answers'))[0];return {rpc:r,grade:Number(a?.grades[0]),total:Number(a?.total_grade)};
});
await test('S31','reject null bulk score',v=>!!v.error,async()=>{await fixture();let r=await rpc(`native_bulk_save_question_grades_v1('e',0,'[{"answer_id":"a","score":null}]')`);return {error:r.error,accepted:!!r.ok,state:await row()}});
await test('S32','reject missing bulk score',v=>!!v.error,async()=>{await fixture();let r=await rpc(`native_bulk_save_question_grades_v1('e',0,'[{"answer_id":"a"}]')`);return {error:r.error,accepted:!!r.ok,state:await row()}});
await test('S33','reject duplicate answer ids',v=>!!v.error,async()=>{await fixture();return rpc(`native_bulk_save_question_grades_v1('e',0,'[{"answer_id":"a","score":1},{"answer_id":"a","score":2}]')`)});
await test('S34','accept zero and maximum boundary',v=>v.zero&&v.max,async()=>{await fixture();let z=await rpc(`native_save_grade('a','[0]','')`);let m=await rpc(`native_save_grade('a','[3]','')`);return {zero:!!z.ok,max:!!m.ok}});
await test('S35','review retains random subset and max score',v=>v.count===1&&v.max===3&&v.index===0,async()=>{await fixture({questions:[{id:'q0',type:'multiple',score:3},{id:'q1',type:'multiple',score:3}],grades:[-1,0],meta:{shown_q:[0],max_score:3}});await q("update answers set graded=true,total_grade=0 where id='a'");await uid(U);let r=await rpc(`native_my_answer_detail_parts_v1('a')`);return {count:r.questions?.length,max:Number(r.total_score),index:r.questions?.[0]?.i}});

await test('S36','retry after incomplete finalization retains calculated objective score',v=>v.finalGrade===3,async()=>{
 await fixture({questions:[{id:'q0',type:'multiple',score:3},{id:'q1',type:'essay',score:5}],keys:[{i:0,correctOption:0}],responses:['0','text'],grades:[0,0]});
 let first=await rpc(`native_finalize_bulk_grades_v1('e')`);
 let saved=await rpc(`native_bulk_save_question_grades_v1('e',1,'[{"answer_id":"a","score":4}]')`);
 let final=await rpc(`native_finalize_bulk_grades_v1('e')`);
 let a=await row();return {first,saved,final,finalGrade:Number(a.grades[0]),state:a};
});

await test('S37','migration reapply does not rewrite historical answers',v=>v.same,async()=>{await fixture();let before=JSON.stringify(await row());await db.exec(migration);return {same:before===JSON.stringify(await row())}});
await test('S38','reject below allowed negative floor',v=>!!v.error,async()=>{await fixture();return rpc(`native_save_grade('a','[-1.01]','')`)});
await test('S39','reject negatives when penalty disabled',v=>!!v.error,async()=>{await fixture({neg:0});return rpc(`native_save_grade('a','[-1]','')`)});
await test('S40','reject negative essay score',v=>!!v.error,async()=>{await fixture({questions:[{id:'q0',type:'essay',score:3}],keys:[]});return rpc(`native_save_grade('a','[-1]','')`)});
await test('S41','approval floors only total, not individual scores',v=>v.grade<0&&v.total===0,async()=>{await fixture();await rpc(`approve_auto_grades('e')`);let a=await row();return {grade:Number(a.grades[0]),total:Number(a.total_grade)}});
await test('S42','all-answer finalization error makes no writes to other complete answers',v=>!!v.error&&v.same,async()=>{await fixture({questions:[{id:'q0',type:'multiple',score:3},{id:'q1',type:'essay',score:5}],keys:[{i:0,correctOption:0}],responses:['0','text'],grades:[0,0]});await q(`insert into answers(id,exam_id,student_id,responses,grades,total_grade) values('b','e',$1,'["0","text"]','[0,4]',4)`,[U]);await q(`insert into native_grade_progress values('b',1,$1,now())`,[T]);let before=JSON.stringify(await q('select * from answers order by id'));let r=await rpc(`native_finalize_bulk_grades_v1('e')`);return {error:r.error,same:before===JSON.stringify(await q('select * from answers order by id'))}});
await test('S43','approval leaves missing essay ungraded, penalty retained',v=>!v.graded&&Math.abs(v.grade+1)<1e-8,async()=>{await fixture({questions:[{id:'q0',type:'multiple',score:3},{id:'q1',type:'essay',score:5}],keys:[{i:0,correctOption:0}],responses:['1','text'],grades:[-1,0]});let r=await rpc(`approve_auto_grades('e')`);let a=await row();return {rpc:r,graded:a.graded,grade:Number(a.grades[0])}});
await test('S44','manual partial score survives repeated auto approvals',v=>v.grade===2&&v.grade2===2,async()=>{await fixture({questions:[{id:'q0',type:'multiple',score:3},{id:'q1',type:'essay',score:5}],keys:[{i:0,correctOption:0}],responses:['1','text'],grades:[0,0]});await rpc(`native_bulk_save_question_grades_v1('e',0,'[{"answer_id":"a","score":2}]')`);await rpc(`approve_auto_grades('e')`);let a=await row();await rpc(`approve_auto_grades('e')`);let b=await row();return {grade:Number(a.grades[0]),grade2:Number(b.grades[0])}});
await test('S45','valid structured essay parts save with negative MCQ',v=>v.accepted&&v.total===3,async()=>{await fixture({questions:[{id:'q0',type:'multiple',score:3},{id:'q1',type:'essay',score:5,assessmentParts:[{id:'p',score:5}]}],keys:[{i:0,correctOption:0}],responses:['1','text'],grades:[-1,0]});let r=await rpc(`native_save_grade_parts_v1('a','[-1,4]','','{"1:0":4}','{}')`);return {accepted:!!r.ok,total:Number((await row()).total_grade),error:r.error}});
await test('S46','structured part sum mismatch rejected without writes',v=>!!v.error&&v.same,async()=>{await fixture({questions:[{id:'q0',type:'essay',score:5,assessmentParts:[{id:'p',score:5}]}],keys:[],grades:[0]});let before=JSON.stringify(await row());let r=await rpc(`native_save_grade_parts_v1('a','[4]','','{"0:0":3}','{}')`);return {error:r.error,same:before===JSON.stringify(await row())}});
await test('S47','null part score is rejected',v=>!!v.error,async()=>{await fixture({questions:[{id:'q0',type:'essay',score:5,assessmentParts:[{id:'p',score:5}]}],keys:[],grades:[0]});return rpc(`native_save_grade_parts_v1('a','[0]','','{"0:0":null}','{}')`)});
await test('S48','bulk refuses multipart flat score',v=>!!v.error,async()=>{await fixture({questions:[{id:'q0',type:'essay',score:5,assessmentParts:[{id:'p',score:5}]}],keys:[],grades:[0]});return rpc(`native_bulk_save_question_grades_v1('e',0,'[{"answer_id":"a","score":4}]')`)});
await test('S49','hidden question cannot be manually scored',v=>!!v.error,async()=>{await fixture({questions:[{id:'q0',type:'multiple',score:3},{id:'q1',type:'multiple',score:3}],grades:[0,0],meta:{shown_q:[0],max_score:3}});return rpc(`native_save_grade('a','[1,1]','')`)});
await test('S51','auto grade info recognizes essay without geometry',v=>v.desc_q===1&&!v.fully_auto,async()=>{await fixture({questions:[{id:'q0',type:'essay',score:3}],keys:[],grades:[0]});return rpc(`exam_autograde_info('e')`)});
await test('S50','unreviewed live function definition blocks migration',v=>v.blocked&&v.unchanged,async()=>{await fixture();await db.exec(`create or replace function public.native_save_grade(p_answer text,p_grades jsonb,p_feedback text default '') returns jsonb language sql as $$select '{}'::jsonb$$`);let before=(await q(`select md5(prosrc) as h from pg_proc where proname='native_save_grade'`))[0].h;let blocked=false;try{await db.exec(migration)}catch(e){blocked=e.message.includes('safety stop');await db.exec('rollback')};return {blocked,unchanged:before===(await q(`select md5(prosrc) as h from pg_proc where proname='native_save_grade'`))[0].h}});

const report={baseline:process.env.GRADING_BASELINE||'original',engine:'PGlite PostgreSQL WASM, actual PL/pgSQL functions, synthetic minimal schema and auth.uid/attempt_state mocks; not Supabase production',postgres:(await q('select version() as v'))[0].v,results,summary:{total:results.length,pass:results.filter(x=>x.meets_requirement).length,fail:results.filter(x=>!x.meets_requirement).length}};
if(process.env.GRADING_TEST_OUTPUT) fs.writeFileSync(process.env.GRADING_TEST_OUTPUT,JSON.stringify(report,null,2));
if(report.summary.fail) process.exitCode=1;for(const x of results)console.log(x.meets_requirement?'PASS':'FAIL',x.id,x.area,JSON.stringify(x.observed??x.execution_error));console.log(report.summary);await db.close();
