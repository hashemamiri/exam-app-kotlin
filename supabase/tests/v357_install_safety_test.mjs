// V357.1 — ایمنی نصب: ترتیب اشتباه، پیش‌نیاز غایب، اجرای دوباره.
// اجرا: npm install --no-save @electric-sql/pglite
//       node supabase/tests/v357_install_safety_test.mjs
import { PGlite } from '@electric-sql/pglite';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
const ROOT = (() => { let d = path.dirname(fileURLToPath(import.meta.url));
  while (d !== path.dirname(d) && !fs.existsSync(path.join(d,'settings.gradle.kts'))) d = path.dirname(d); return d; })();
const S = n => fs.readFileSync(path.join(ROOT,'supabase','sql',n),'utf8');
const ALL = S('SQL_EXAM_TIMING_V357_ALL.sql'), CHK = S('SQL_EXAM_TIMING_V357_CHECK.sql');
process.on('uncaughtException',e=>{console.log('fatal: '+e.message);process.exit(1);});
process.on('unhandledRejection',e=>{console.log('fatal: '+(e&&e.message||e));process.exit(1);});
let pass=0,fail=0;
const ok=(n,c,x)=>{if(c){pass++;console.log('  ✓ '+n);}else{fail++;console.log('  ✗ '+n+(x?' — '+x:''));}};
const BASE=`create schema if not exists auth;
create or replace function auth.uid() returns uuid language sql stable as $$ select nullif(current_setting('test.uid',true),'')::uuid $$;
do $$ begin if not exists (select 1 from pg_roles where rolname='authenticated') then create role authenticated; end if; end $$;
grant usage on schema public to authenticated;`;
const TABLES=`create table public.exams(id text primary key, teacher_id uuid);
create table public.exam_attempts(id bigserial primary key, exam_id text, student_id uuid, attempt_no int default 1, expires_at timestamptz);
create table public.exam_sessions(exam_id text, student_id uuid, expires_at timestamptz);
create table public.answers(id text primary key, exam_id text, student_id uuid, attempt_no int, responses jsonb, meta jsonb default '{}'::jsonb);`;
async function fresh(t=true){const db=new PGlite();await db.exec(BASE);if(t)await db.exec(TABLES);return db;}
const rows=async db=>(await db.query(CHK.slice(CHK.indexOf('with checks'),CHK.indexOf('-- خلاصهٔ شمارش')))).rows;

console.log('\n=== ۱) بررسی روی پایگاهِ نصب‌نشده — بدون سقوط ===');
{ const db=await fresh(); let crashed=false;
  try{await db.exec(CHK);}catch(e){crashed=true;}
  ok('خطای ۴۲P01 نمی‌دهد',!crashed);
  const r=await rows(db);
  ok('ردیف نخست صریح می‌گوید نصب نشده',r[0]['درست']===false&&r[0]['بررسی'].includes('نصب V357'));
  ok('سنجه‌های مثبت false هستند',r.filter(x=>!x['بررسی'].startsWith('هیچ')).every(x=>x['درست']===false));
  ok('سنجهٔ منفی درست true ماند',r.filter(x=>x['بررسی'].startsWith('هیچ')).every(x=>x['درست']===true));
  ok('شمار ردیف‌ها ۲۳ است',r.length===23,String(r.length));
  await db.close(); }

console.log('\n=== ۲ و ۳) نصب یک‌جا + اجرای دوباره ===');
{ const db=await fresh(); await db.exec(ALL);
  const r=await rows(db), bad=r.filter(x=>x['درست']!==true);
  ok('همهٔ ۲۳ بررسی true شد',r.length===23&&bad.length===0,JSON.stringify(bad));
  await db.exec(ALL);
  ok('اجرای دوباره همه‌چیز را true نگه داشت',(await rows(db)).every(x=>x['درست']===true));
  ok('تابع‌ها تکراری نشدند',(await db.query("select count(*)::int n from pg_proc where proname like '%_v357'")).rows[0].n===8);
  ok('تریگرها تکراری نشدند',(await db.query("select count(*)::int n from pg_trigger where tgname like '%_v357'")).rows[0].n===2);
  await db.close(); }

console.log('\n=== ۴) جدول پایه غایب ===');
{ const db=await fresh(false); let m='';
  try{await db.exec(ALL);}catch(e){m=String(e.message);}
  ok('با پیام فارسی ایستاد',m.includes('جدول پایه پیدا نشد'),m.slice(0,80));
  ok('نام جدول غایب را گفت',m.includes('exam_attempts'));
  ok('هیچ جدولی ساخته نشد',(await db.query("select to_regclass('public.exam_time_grants') is null z")).rows[0].z===true);
  await db.close(); }

console.log('\n=== ۵) ستون پایه غایب ===');
{ const db=await fresh(false);
  await db.exec(`create table public.exams(id text primary key, teacher_id uuid);
  create table public.exam_attempts(id bigserial primary key, exam_id text, student_id uuid, expires_at timestamptz);
  create table public.exam_sessions(exam_id text, student_id uuid, expires_at timestamptz);
  create table public.answers(id text primary key, exam_id text, student_id uuid, responses jsonb, meta jsonb default '{}'::jsonb);`);
  let m=''; try{await db.exec(ALL);}catch(e){m=String(e.message);}
  ok('ستون غایب را گرفت',m.includes('ستون پایه پیدا نشد'),m.slice(0,80));
  ok('نام دقیق ستون را گفت',m.includes('attempt_no'));
  ok('نصب نیمه‌کاره نماند',(await db.query("select to_regclass('public.exam_time_grants') is null z")).rows[0].z===true);
  await db.close(); }

console.log('\n=== ۶) کارکرد واقعی زمان اضافه ===');
{ const db=await fresh(); await db.exec(ALL);
  const T=crypto.randomUUID(),S2=crypto.randomUUID(),E='e-'+crypto.randomUUID().slice(0,8);
  await db.query('insert into public.exams values ($1,$2)',[E,T]);
  await db.query(`select set_config('test.uid',$1,false)`,[T]);
  await db.query('select public.native_time_grant_save_v357($1,$2,15,$3)',[E,S2,'مشکل یادگیری تأییدشده']);
  await db.query(`insert into public.exam_attempts(exam_id,student_id,attempt_no,expires_at) values ($1,$2,1,now()+interval '60 minutes')`,[E,S2]);
  ok('مجوز ۱۵ دقیقه اعمال شد',(await db.query('select time_grant_applied t from public.exam_attempts')).rows[0].t===15);
  const m1=(await db.query("select extract(epoch from (expires_at-now()))/60 m from public.exam_attempts")).rows[0].m;
  ok('مهلت حدود ۷۵ دقیقه شد',m1>74&&m1<=75,String(Math.round(m1)));
  await db.query('update public.exam_attempts set expires_at=expires_at');
  const m2=(await db.query("select extract(epoch from (expires_at-now()))/60 m from public.exam_attempts")).rows[0].m;
  ok('ادامهٔ آزمون زمان را دوباره اضافه نکرد',m2>74&&m2<=75,String(Math.round(m2)));
  await db.close(); }

console.log(`\nV357_INSTALL_SAFETY: ${fail===0?'PASS':'FAIL'} (${pass} سنجه، ${fail} خطا)`);
process.exit(fail===0?0:1);
