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

const db = new PGlite();
let pass = 0, fail = 0;
const ok = (n, c) => { if (c) { pass++; console.log('  ✓ ' + n); } else { fail++; console.log('  ✗ ' + n); } };
async function q(sql, p) { return (await db.query(sql, p)).rows; }
async function mustFail(n, sql, p) {
  try { await db.query(sql, p); ok(n + ' (باید رد می‌شد)', false); }
  catch (e) { ok(n, true); }
}
const uid = () => crypto.randomUUID();

// ---- auth شبیه‌سازی‌شده ----
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

// ============================================================
// ساختار با نوع‌های واقعی پایگاه‌دادهٔ کاربر
//   answers.id = text ، answers.exam_id = text ، exams.id = text
//   exams.teacher_id = uuid ، answers.student_id = uuid
// ============================================================
const T1 = uid(), T2 = uid(), S1 = uid(), S2 = uid();
const E1 = 'exam-' + uid().slice(0, 8), E2 = 'exam-' + uid().slice(0, 8);
const A1 = 'ans-' + uid().slice(0, 8);
await db.exec(`
create table public.profiles(id uuid primary key, full_name text, username text);
create table public.exams(id text primary key, teacher_id uuid, title text, subject text);
create table public.answers(id text primary key, exam_id text, student_id uuid,
  score numeric, graded boolean default false, responses jsonb, meta jsonb,
  submitted_at timestamptz default now());
`);
await q(`insert into public.profiles values ($1,'معلم الف','t1'),($2,'معلم ب','t2'),($3,'دانش‌آموز یک','s1'),($4,'دانش‌آموز دو','s2')`, [T1, T2, S1, S2]);
await q(`insert into public.exams values ($1,$2,'ریاضی ۱','ریاضی'),($3,$4,'فیزیک ۱','فیزیک')`, [E1, T1, E2, T2]);
await q(`insert into public.answers(id,exam_id,student_id,score,graded,responses) values ($1,$2,$3,15,true,'{}')`, [A1, E1, S1]);

console.log('\n=== ۱) بازتولید خطای گزارش‌شدهٔ کاربر ===');
// همان چیزی که نسخهٔ نخست می‌ساخت: ستون uuid در برابر exams.id از نوع text
await db.exec(`create table public.old_gh(exam_id uuid);`);
let reproduced = null;
try {
  await db.exec(`create policy p_old on public.old_gh for select using (
    exists (select 1 from public.exams e where e.id = old_gh.exam_id and e.teacher_id = auth.uid()))`);
} catch (e) { reproduced = String(e.message).slice(0, 120); }
ok('خطای «text = uuid» بازتولید شد', reproduced && reproduced.includes('text = uuid'));
console.log('    پیام واقعی: ' + reproduced);
await db.exec(`drop table public.old_gh;`);

console.log('\n=== ۲) اجرای مهاجرت اصلاح‌شده روی همان ساختار ===');
const mig = fs.readFileSync(SQL('SQL_GRADING_APPEALS_V354.sql'), 'utf8');
try { await db.exec(mig); ok('مهاجرت کامل اجرا شد', true); }
catch (e) { ok('مهاجرت کامل اجرا شد', false); console.log('    ' + String(e.message).slice(0, 300)); process.exit(1); }

// نوع ستون‌ها باید text شده باشد
const ty = await q(`select column_name, data_type from information_schema.columns
  where table_schema='public' and table_name='grade_history' and column_name in ('answer_id','exam_id','student_id')`);
ok('ستون‌های شناسه text شدند', ty.every(r => r.data_type === 'text'));
const fk = await q(`select 1 from pg_constraint where conname='grade_appeals_answer_fk'`);
ok('کلید خارجی به answers ساخته شد', fk.length === 1);

const setU = async (u) => { await q(`select set_config('test.uid',$1,false)`, [u || '']); };

console.log('\n=== ۳) اعتراض دانش‌آموز ===');
await setU(S1);
let r = await q(`select public.native_appeal_create_v354($1,-1,$2) as j`, [A1, 'نمرهٔ سؤال تشریحی کم داده شده است']);
ok('اعتراض روی برگهٔ خودش ثبت شد', r[0].j.ok === true);
const apid = r[0].j.id;
await mustFail('اعتراض روی پاسخ دیگری رد شد', `select public.native_appeal_create_v354('nonexistent',-1,'دلیل معتبر و کافی')`);
await mustFail('دلیل کوتاه رد شد', `select public.native_appeal_create_v354($1,0,'کم')`, [A1]);
await mustFail('اعتراض باز تکراری رد شد', `select public.native_appeal_create_v354($1,-1,'دلیل دوم و متفاوت')`, [A1]);
r = await q(`select public.native_my_appeals_v354() as j`);
ok('دانش‌آموز اعتراض خودش را می‌بیند', r[0].j.length === 1 && r[0].j[0].exam_title === 'ریاضی ۱');
await setU(S2);
r = await q(`select public.native_my_appeals_v354() as j`);
ok('دانش‌آموز دیگر چیزی نمی‌بیند', r[0].j.length === 0);

console.log('\n=== ۴) معلم و مصحح دوم ===');
await setU(T2);
await mustFail('معلم دیگر به اعتراض‌ها دسترسی ندارد', `select public.native_exam_appeals_v354($1)`, [E1]);
await setU(T1);
r = await q(`select public.native_exam_appeals_v354($1) as j`, [E1]);
ok('معلم اعتراض را با نام دانش‌آموز می‌بیند', r[0].j.length === 1 && r[0].j[0].student_name === 'دانش‌آموز یک');
r = await q(`select public.native_appeal_resolve_v354($1,'second',$2,$3) as j`, [apid, 'بررسی دوباره لازم است', 17.5]);
ok('ارجاع به مصحح دوم با نمره', r[0].j.status === 'second');
r = await q(`select second_score, second_by from public.grade_appeals where id=$1`, [apid]);
ok('نمرهٔ مصحح دوم ذخیره شد', Number(r[0].second_score) === 17.5 && r[0].second_by === T1);
r = await q(`select public.native_appeal_resolve_v354($1,'accepted',$2) as j`, [apid, 'حق با دانش‌آموز بود']);
ok('پذیرش اعتراض', r[0].j.status === 'accepted');
await mustFail('رسیدگی دوباره به اعتراض بسته رد شد', `select public.native_appeal_resolve_v354($1,'rejected','تلاش دوباره')`, [apid]);
r = await q(`select score from public.answers where id=$1`, [A1]);
ok('پذیرش، نمره را خودکار عوض نکرد', Number(r[0].score) === 15);
await setU(S1);
r = await q(`select public.native_my_appeals_v354() as j`);
ok('دانش‌آموز نتیجه و نمرهٔ دوم را می‌بیند', r[0].j[0].status === 'accepted' && Number(r[0].j[0].second_score) === 17.5);

console.log('\n=== ۵) تاریخچهٔ تغییر نمره ===');
await setU(T1);
await q(`update public.answers set score=18 where id=$1`, [A1]);
r = await q(`select public.native_grade_history_v354($1) as j`, [E1]);
ok('تغییر نمره ثبت شد', r[0].j.length === 1);
ok('مقدار قبلی و جدید درست است', Number(r[0].j[0].before_score) === 15 && Number(r[0].j[0].after_score) === 18);
ok('نام تغییردهنده ثبت شد', r[0].j[0].changed_by_name === 'معلم الف');
const n1 = (await q(`select count(*)::int c from public.grade_history`))[0].c;
await q(`update public.answers set submitted_at=now() where id=$1`, [A1]);
const n2 = (await q(`select count(*)::int c from public.grade_history`))[0].c;
ok('تغییر ستون بی‌ربط تاریخچه نمی‌سازد', n1 === n2);
await setU(T2);
await mustFail('معلم دیگر تاریخچه را نمی‌بیند', `select public.native_grade_history_v354($1)`, [E1]);

console.log('\n=== ۶) دست‌کاری‌ناپذیری سابقه ===');
await q(`grant select on public.grade_history to authenticated`);  // فقط خواندن، مانند تولید
await q(`grant select, insert, update, delete on public.grade_appeals to authenticated`);
await q(`grant usage, select on all sequences in schema public to authenticated`);
await db.exec(`set role authenticated`);
await setU(S1);
await mustFail('دانش‌آموز نمی‌تواند سابقه را پاک کند', `delete from public.grade_history`);
await mustFail('دانش‌آموز نمی‌تواند سابقه را عوض کند', `update public.grade_history set after='{}'`);
await mustFail('دانش‌آموز نمی‌تواند سابقهٔ جعلی بنویسد', `insert into public.grade_history(answer_id) values ('fake')`);
await db.exec(`reset role`);
await setU(T1);
await q(`update public.answers set score=20 where id=$1`, [A1]);
r = await q(`select count(*)::int c from public.grade_history`);
ok('تریگر با وجود قفل نوشتن، همچنان ثبت می‌کند', r[0].c === 2);

console.log('\n=== ۷) اجرای دوباره بی‌خطر است ===');
try { await db.exec(mig); ok('اجرای دوم بدون خطا', true); }
catch (e) { ok('اجرای دوم بدون خطا', false); console.log('    ' + String(e.message).slice(0, 250)); }
r = await q(`select count(*)::int c from public.grade_history`);
ok('داده‌های تاریخچه حفظ شد', r[0].c === 2);
r = await q(`select count(*)::int c from public.grade_appeals`);
ok('اعتراض‌ها حفظ شدند', r[0].c === 1);

console.log('\n=== ۸) فایل بررسی نصب ===');
const chk = fs.readFileSync(SQL('SQL_GRADING_APPEALS_V354_CHECK.sql'), 'utf8');
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

console.log(`\n${fail === 0 ? 'SQL354: PASS' : 'SQL354: FAIL'} (${pass} سنجه، ${fail} خطا)`);
process.exit(fail === 0 ? 0 : 1);
