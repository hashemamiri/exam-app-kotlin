// آزمون V354 — اعتراض به نمره و تاریخچهٔ نمره
// اجرا: npm install @electric-sql/pglite  سپس  node supabase/tests/<این فایل>
// این آزمون یک PostgreSQL واقعی (WASM) بالا می‌آورد و مهاجرت را اجرا می‌کند.
// مسیر فایل SQL از ریشهٔ مخزن پیدا می‌شود، پس از هر پوشه‌ای قابل اجراست.
import { PGlite } from '@electric-sql/pglite';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
const ROOT = (() => { let d = path.dirname(fileURLToPath(import.meta.url));
  while (d !== path.dirname(d) && !fs.existsSync(path.join(d, 'settings.gradle.kts'))) d = path.dirname(d);
  return d; })();
const SQL = (n) => fs.existsSync(path.join(ROOT, n)) ? path.join(ROOT, n)
  : path.join(ROOT, 'supabase', 'sql', n);
const mig = fs.readFileSync(SQL('SQL_GRADING_APPEALS_V354.sql'), 'utf8');
let fail = 0;
const ok = (n,c) => { console.log((c?'  ✓ ':'  ✗ ')+n); if(!c) fail++; };

async function scenario(name, ddl, seed, leftover) {
  const db = new PGlite();
  await db.exec(`create schema if not exists auth;
    create or replace function auth.uid() returns uuid language sql stable as
    $$ select nullif(current_setting('test.uid',true),'')::uuid $$;
    do $$ begin if not exists (select 1 from pg_roles where rolname='authenticated') then create role authenticated; end if;
    if not exists (select 1 from pg_roles where rolname='anon') then create role anon; end if; end $$;
    grant usage on schema public to authenticated, anon;`);
  await db.exec(ddl);
  const T='11111111-1111-4111-8111-111111111111', S='22222222-2222-4222-8222-222222222222';
  await seed(db, T, S);
  if (leftover) await db.exec(leftover);
  console.log('\n=== ' + name + ' ===');
  try { await db.exec(mig); ok('مهاجرت اجرا شد', true); }
  catch(e){ ok('مهاجرت اجرا شد', false); console.log('    '+String(e.message).slice(0,220)); await db.close(); return; }
  await db.query(`select set_config('test.uid',$1,false)`, [S]);
  const A = (await db.query(`select id::text i from public.answers limit 1`)).rows[0].i;
  try {
    const r = (await db.query(`select public.native_appeal_create_v354($1,-1,$2) j`, [A,'دلیل کافی برای اعتراض'])).rows[0].j;
    ok('ثبت اعتراض کار کرد', r.ok === true);
  } catch(e){ ok('ثبت اعتراض کار کرد', false); console.log('    '+String(e.message).slice(0,200)); }
  await db.query(`select set_config('test.uid',$1,false)`, [T]);
  await db.query(`update public.answers set score=99 where id::text=$1`, [A]);
  try {
    const E = (await db.query(`select exam_id::text e from public.answers limit 1`)).rows[0].e;
    const h = (await db.query(`select public.native_grade_history_v354($1) j`, [E])).rows[0].j;
    ok('تاریخچه ثبت و خوانده شد', h.length === 1 && Number(h[0].after_score) === 99);
  } catch(e){ ok('تاریخچه ثبت و خوانده شد', false); console.log('    '+String(e.message).slice(0,200)); }
  const bad = (await db.query(`select count(*)::int c from information_schema.columns
    where table_schema='public' and table_name in ('grade_history','grade_appeals')
      and column_name in ('answer_id','exam_id','student_id') and data_type<>'text'`)).rows[0].c;
  ok('ستون‌های شناسه text هستند', bad === 0);
  try { await db.exec(mig); ok('اجرای دوباره بی‌خطر', true); } catch(e){ ok('اجرای دوباره بی‌خطر', false); }
  await db.close();
}

// حالت الف) همه‌چیز uuid (ساختار متفاوت از کاربر)
await scenario('ساختار کاملاً uuid',
 `create table public.profiles(id uuid primary key, full_name text, username text);
  create table public.exams(id uuid primary key, teacher_id uuid, title text);
  create table public.answers(id uuid primary key default gen_random_uuid(), exam_id uuid,
    student_id uuid, score numeric, graded boolean default true, responses jsonb, meta jsonb,
    submitted_at timestamptz default now());`,
 async (db,T,S) => {
   await db.query(`insert into public.profiles values ($1,'معلم','t'),($2,'دانش‌آموز','s')`,[T,S]);
   await db.query(`insert into public.exams values ('33333333-3333-4333-8333-333333333333',$1,'آزمون')`,[T]);
   await db.query(`insert into public.answers(exam_id,student_id,score) values ('33333333-3333-4333-8333-333333333333',$1,10)`,[S]);
 });

// حالت ب) جدول‌های بازمانده از اجرای ناقص با نوع uuid
await scenario('بازماندهٔ اجرای ناقص (ستون uuid)',
 `create table public.profiles(id uuid primary key, full_name text, username text);
  create table public.exams(id text primary key, teacher_id uuid, title text);
  create table public.answers(id text primary key, exam_id text, student_id uuid, score numeric,
    graded boolean default true, responses jsonb, meta jsonb, submitted_at timestamptz default now());`,
 async (db,T,S) => {
   await db.query(`insert into public.profiles values ($1,'معلم','t'),($2,'دانش‌آموز','s')`,[T,S]);
   await db.query(`insert into public.exams values ('e1',$1,'آزمون')`,[T]);
   await db.query(`insert into public.answers values ('a1','e1',$1,10)`,[S]);
 },
 `create table public.grade_history(id bigserial primary key, answer_id uuid not null, exam_id uuid,
    student_id uuid, changed_by uuid, before jsonb, after jsonb, created_at timestamptz default now());
  create table public.grade_appeals(id uuid primary key default gen_random_uuid(), answer_id uuid not null,
    exam_id uuid not null, student_id uuid not null, question_index int default -1, reason text,
    status text default 'open', second_score numeric, second_by uuid, second_note text,
    resolution_note text, resolved_by uuid, created_at timestamptz default now(), resolved_at timestamptz);`);

console.log(`\n${fail===0?'TYPE-INDEPENDENCE: PASS':'TYPE-INDEPENDENCE: FAIL'} (${fail} خطا)`);
process.exit(fail===0?0:1);
