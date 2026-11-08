// آزمون V356 — تصحیح کد فاز صفر روی PostgreSQL واقعی
// اجرا: npm install @electric-sql/pglite  سپس  node supabase/tests/v356_code_grading_sql_test.mjs
import { PGlite } from '@electric-sql/pglite';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
const ROOT = (() => { let d = path.dirname(fileURLToPath(import.meta.url));
  while (d !== path.dirname(d) && !fs.existsSync(path.join(d, 'settings.gradle.kts'))) d = path.dirname(d);
  return d; })();
const SQL = (n) => fs.existsSync(path.join(ROOT, n)) ? path.join(ROOT, n) : path.join(ROOT, 'supabase', 'sql', n);

const db = new PGlite();
let pass = 0, fail = 0;
const ok = (n, c) => { if (c) { pass++; console.log('  ✓ ' + n); } else { fail++; console.log('  ✗ ' + n); } };
async function q(sql, p) {
  try { return (await db.query(sql, p)).rows; }
  catch (e) { throw new Error(String(e.message || e).slice(0, 180)); }
}
async function mustFail(n, sql, p) {
  try { await db.query(sql, p); ok(n + ' (باید رد می‌شد)', false); }
  catch (e) { ok(n, true); }
}
const uid = () => crypto.randomUUID();

await db.exec(`
create schema if not exists auth;
create or replace function auth.uid() returns uuid language sql stable as
$$ select nullif(current_setting('test.uid', true), '')::uuid $$;
do $$ begin
  if not exists (select 1 from pg_roles where rolname='authenticated') then create role authenticated; end if;
  if not exists (select 1 from pg_roles where rolname='anon') then create role anon; end if;
end $$;
grant usage on schema public to authenticated, anon;
`);

// نوع‌های واقعی: exams.id و answers.id متن، teacher_id و student_id uuid
const T1 = uid(), T2 = uid(), S1 = uid();
const E1 = 'exam-' + uid().slice(0, 8), E2 = 'exam-' + uid().slice(0, 8);
await db.exec(`
create table public.profiles(id uuid primary key, full_name text, username text);
create table public.exams(id text primary key, teacher_id uuid, title text);
create table public.answers(id text primary key, exam_id text, student_id uuid, score numeric, graded boolean default true);
`);
await q(`insert into public.profiles values ($1,'معلم الف','t1'),($2,'معلم ب','t2')`, [T1, T2]);
await q(`insert into public.exams values ($1,$2,'برنامه‌نویسی'),($3,$4,'ریاضی')`, [E1, T1, E2, T2]);
await q(`insert into public.answers values ('a1',$1,$2,0),('a2',$1,$2,0)`, [E1, S1]);

console.log('\n=== ۱) اجرای مهاجرت ===');
const mig = fs.readFileSync(SQL('SQL_CODE_GRADING_V356.sql'), 'utf8');
try { await db.exec(mig); ok('مهاجرت کامل اجرا شد', true); }
catch (e) { ok('مهاجرت کامل اجرا شد', false); console.log('    ' + e.message); process.exit(1); }

const setU = async (u) => { await q(`select set_config('test.uid',$1,false)`, [u || '']); };
const TESTS = [
  { label: 'مثال ساده', input: '2 3', expected: '5', weight: 1, hidden: false },
  { label: 'عدد منفی', input: '-2 3', expected: '1', weight: 2, hidden: false },
  { label: 'حالت مرزی', input: '0 0', expected: '0', weight: 1, hidden: true }
];

console.log('\n=== ۲) مشخصات و موارد آزمون ===');
await setU(T2);
await mustFail('معلم دیگر مشخصات را نمی‌بیند', `select public.native_code_spec_get_v356($1)`, [E1]);
await setU(T1);
let r = await q(`select public.native_code_spec_get_v356($1) j`, [E1]);
ok('در ابتدا خالی است', r[0].j.specs.length === 0 && r[0].j.tests.length === 0);
r = await q(`select public.native_code_spec_save_v356($1,0,'python',$2,$3::jsonb) j`,
  [E1, 'def add(a,b):\n    return a+b', JSON.stringify(TESTS)]);
ok('سه مورد آزمون ذخیره شد', r[0].j.ok === true && r[0].j.count === 3);
r = await q(`select public.native_code_spec_get_v356($1) j`, [E1]);
ok('پاسخ مرجع ذخیره شد', r[0].j.specs[0].reference.includes('return a+b'));
ok('مورد پنهان درست ذخیره شد', r[0].j.tests.filter(t => t.hidden).length === 1);
ok('ترتیب موردها حفظ شد', r[0].j.tests.map(t => t.ord).join(',') === '0,1,2');
ok('وزن‌ها درست است', Number(r[0].j.tests[1].weight) === 2);
const ids = r[0].j.tests.map(t => t.id);
// ذخیرهٔ دوباره باید جایگزین کند
await q(`select public.native_code_spec_save_v356($1,0,'javascript',$2,$3::jsonb)`,
  [E1, 'const add=(a,b)=>a+b;', JSON.stringify([TESTS[0]])]);
r = await q(`select public.native_code_spec_get_v356($1) j`, [E1]);
ok('ذخیرهٔ دوباره گروه را جایگزین می‌کند', r[0].j.tests.length === 1 && r[0].j.specs[0].language === 'javascript');
// سؤال دیگر مستقل است
await q(`select public.native_code_spec_save_v356($1,1,'python','pass',$2::jsonb)`, [E1, JSON.stringify(TESTS)]);
r = await q(`select public.native_code_spec_get_v356($1) j`, [E1]);
ok('سؤال‌های دیگر دست‌نخورده می‌مانند', r[0].j.specs.length === 2 && r[0].j.tests.length === 4);
const q1ids = r[0].j.tests.filter(t => t.question_index === 1).map(t => t.id);

console.log('\n=== ۳) اعتبارسنجی ===');
await mustFail('وزن صفر رد شد', `select public.native_code_spec_save_v356($1,0,'python','x','[{"weight":0}]'::jsonb)`, [E1]);
await mustFail('وزن منفی رد شد', `select public.native_code_spec_save_v356($1,0,'python','x','[{"weight":-1}]'::jsonb)`, [E1]);
await mustFail('ورودی غیرآرایه رد شد', `select public.native_code_spec_save_v356($1,0,'python','x','{}'::jsonb)`, [E1]);
await mustFail('شاخص سؤال منفی رد شد', `select public.native_code_spec_save_v356($1,-1,'python','x','[]'::jsonb)`, [E1]);
await mustFail('پاسخ مرجع بلند رد شد', `select public.native_code_spec_save_v356($1,0,'python',$2,'[]'::jsonb)`, [E1, 'x'.repeat(20001)]);
await mustFail('برچسب بلند رد شد', `select public.native_code_spec_save_v356($1,0,'python','x',$2::jsonb)`, [E1, JSON.stringify([{ label: 'x'.repeat(121) }])]);
await mustFail('خروجی بلند رد شد', `select public.native_code_spec_save_v356($1,0,'python','x',$2::jsonb)`, [E1, JSON.stringify([{ expected: 'x'.repeat(4001) }])]);
const many = Array.from({ length: 41 }, (_, i) => ({ label: 't' + i }));
await mustFail('بیش از ۴۰ مورد رد شد', `select public.native_code_spec_save_v356($1,0,'python','x',$2::jsonb)`, [E1, JSON.stringify(many)]);
r = await q(`select public.native_code_spec_get_v356($1) j`, [E1]);
ok('ورودی نامعتبر دادهٔ قبلی را پاک نکرد', r[0].j.tests.length === 4);
await mustFail('معلم دیگر نمی‌تواند ذخیره کند', `select public.native_code_spec_save_v356($1,0,'python','x','[]'::jsonb)`, [E2]);

console.log('\n=== ۴) چک‌لیست معلم ===');
r = await q(`select public.native_code_checks_v356($1) j`, [E1]);
ok('چک‌لیست در ابتدا خالی است', r[0].j.length === 0);
r = await q(`select public.native_code_check_save_v356('a1',1,$1,true,$2,'') j`, [q1ids[0], '5']);
ok('نتیجهٔ قبول ثبت شد', r[0].j.passed === true);
await q(`select public.native_code_check_save_v356('a1',1,$1,false,$2,'خروجی اشتباه')`, [q1ids[1], '9']);
r = await q(`select public.native_code_checks_v356($1) j`, [E1]);
ok('دو ردیف چک‌لیست ثبت شد', r[0].j.length === 2);
ok('خروجی واقعی نگه داشته شد', r[0].j[0].actual === '5');
await q(`select public.native_code_check_save_v356('a1',1,$1,false,'7','')`, [q1ids[0]]);
r = await q(`select public.native_code_checks_v356($1) j`, [E1]);
ok('به‌روزرسانی ردیف تازه نساخت', r[0].j.length === 2 && r[0].j[0].passed === false);
await mustFail('مورد آزمون از سؤال دیگر رد شد', `select public.native_code_check_save_v356('a1',0,$1,true,'','')`, [q1ids[0]]);
await mustFail('مورد آزمون ناموجود رد شد', `select public.native_code_check_save_v356('a1',1,999999,true,'','')`);
await mustFail('پاسخ ناموجود رد شد', `select public.native_code_check_save_v356('nope',1,$1,true,'','')`, [q1ids[0]]);
await mustFail('خروجی واقعی بلند رد شد', `select public.native_code_check_save_v356('a1',1,$1,true,$2,'')`, [q1ids[0], 'x'.repeat(4001)]);
await setU(T2);
await mustFail('معلم دیگر چک‌لیست را نمی‌بیند', `select public.native_code_checks_v356($1)`, [E1]);
await mustFail('معلم دیگر نمی‌تواند نتیجه ثبت کند', `select public.native_code_check_save_v356('a1',1,$1,true,'','')`, [q1ids[0]]);

console.log('\n=== ۵) پاک‌سازی چک‌لیست یتیم ===');
await setU(T1);
// replacing question 1 tests must drop its checks
let rr = await q(`select public.native_code_spec_save_v356($1,1,'python','pass',$2::jsonb) j`, [E1, JSON.stringify([TESTS[0]])]);
ok('تعداد چک‌لیست پاک‌شده گزارش شد', rr[0].j.cleared_checks === 2, JSON.stringify(rr[0].j));
r = await q(`select public.native_code_checks_v356($1) j`, [E1]);
ok('چک‌لیست موردهای حذف‌شده پاک شد', r[0].j.length === 0);

console.log('\n=== ۶) نمره‌ها دست‌نخورده ===');
r = await q(`select score from public.answers where id='a1'`);
ok('نمره تغییری نکرد', Number(r[0].score) === 0);
r = await q(`select count(*)::int c from public.answers`);
ok('پاسخی حذف یا اضافه نشد', r[0].c === 2);

console.log('\n=== ۷) محرمانگی از دید دانش‌آموز ===');
await q(`grant select on public.exams, public.profiles, public.answers to authenticated`);
await q(`grant select, insert, update, delete on public.code_specs, public.code_tests, public.code_checks to authenticated`);
await q(`grant usage, select on all sequences in schema public to authenticated`);
await db.exec(`set role authenticated`);
await setU(S1);
r = await q(`select count(*)::int c from public.code_tests`);
ok('دانش‌آموز مورد آزمونی نمی‌بیند', r[0].c === 0);
r = await q(`select count(*)::int c from public.code_specs`);
ok('دانش‌آموز پاسخ مرجع را نمی‌بیند', r[0].c === 0);
await mustFail('دانش‌آموز نمی‌تواند مورد آزمون بنویسد', `insert into public.code_tests(exam_id,question_index) values ('x',0)`);
await db.exec(`reset role`);

console.log('\n=== ۸) اجرای دوباره بی‌خطر ===');
await setU(T1);
try { await db.exec(mig); ok('اجرای دوم بدون خطا', true); }
catch (e) { ok('اجرای دوم بدون خطا', false); console.log('    ' + e.message); }
r = await q(`select public.native_code_spec_get_v356($1) j`, [E1]);
ok('مشخصات و موردها حفظ شدند', r[0].j.specs.length === 2 && r[0].j.tests.length === 2);

console.log('\n=== ۹) فایل بررسی نصب ===');
const chk = fs.readFileSync(SQL('SQL_CODE_GRADING_V356_CHECK.sql'), 'utf8');
const stmts = chk.split('\n').filter(l => !l.trim().startsWith('--')).join('\n')
  .split(';').map(s => s.trim()).filter(Boolean);
let chkFail = 0, chkCount = 0;
for (const s of stmts) {
  try {
    const rows = (await db.query(s)).rows;
    for (const row of rows) for (const [k, v] of Object.entries(row)) {
      if (v === false) { chkFail++; console.log('    ✗ ' + k); }
      if (v === true) chkCount++;
    }
  } catch (e) { chkFail++; console.log('    خطا: ' + String(e.message).slice(0, 160)); }
}
ok('همهٔ بررسی‌های نصب true شدند', chkFail === 0 && chkCount > 0);
console.log(`    بررسی نصب: ${chkCount} مورد true، ${chkFail} خطا`);

console.log(`\n${fail === 0 ? 'SQL356: PASS' : 'SQL356: FAIL'} (${pass} سنجه، ${fail} خطا)`);
process.exit(fail === 0 ? 0 : 1);
