// آزمون V355 — کلیدواژهٔ تشریحی و بررسی شباهت روی PostgreSQL واقعی
// اجرا: npm install @electric-sql/pglite  سپس  node supabase/tests/v355_similarity_sql_test.mjs
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
  catch (e) { const m = String(e.message || e); const s = m.slice(0, 160); throw new Error(s); }
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

// ساختار با نوع‌های واقعی: exams.id و answers.id متن، teacher_id و student_id uuid
const T1 = uid(), T2 = uid(), S1 = uid();
const E1 = 'exam-' + uid().slice(0, 8), E2 = 'exam-' + uid().slice(0, 8);
await db.exec(`
create table public.profiles(id uuid primary key, full_name text, username text);
create table public.exams(id text primary key, teacher_id uuid, title text);
create table public.answers(id text primary key, exam_id text, student_id uuid, score numeric, graded boolean default true);
`);
await q(`insert into public.profiles values ($1,'معلم الف','t1'),($2,'معلم ب','t2')`, [T1, T2]);
await q(`insert into public.exams values ($1,$2,'ریاضی'),($3,$4,'فیزیک')`, [E1, T1, E2, T2]);
await q(`insert into public.answers values ('a1',$1,$2,10),('a2',$1,$2,12)`, [E1, S1]);

console.log('\n=== ۱) اجرای مهاجرت ===');
const mig = fs.readFileSync(SQL('SQL_GRADING_SIMILARITY_V355.sql'), 'utf8');
try { await db.exec(mig); ok('مهاجرت کامل اجرا شد', true); }
catch (e) { ok('مهاجرت کامل اجرا شد', false); console.log('    ' + String(e.message).slice(0, 300)); process.exit(1); }

const setU = async (u) => { await q(`select set_config('test.uid',$1,false)`, [u || '']); };

console.log('\n=== ۲) کلیدواژه‌ها ===');
await setU(T2);
await mustFail('معلم دیگر به کلیدواژه‌ها دسترسی ندارد', `select public.native_essay_keywords_v355($1)`, [E1]);
await setU(T1);
let r = await q(`select public.native_essay_keywords_v355($1) j`, [E1]);
ok('فهرست خالی در ابتدا', Array.isArray(r[0].j) && r[0].j.length === 0);
const items = [{ keyword: 'قضیه فیثاغورس', weight: 2 }, { keyword: 'وتر', weight: 1 }, { keyword: 'مجذور|توان دو', weight: 1 }];
r = await q(`select public.native_essay_keywords_save_v355($1,0,'',$2::jsonb) j`, [E1, JSON.stringify(items)]);
ok('سه کلیدواژه ذخیره شد', r[0].j.ok === true && r[0].j.count === 3);
r = await q(`select public.native_essay_keywords_v355($1) j`, [E1]);
ok('کلیدواژه‌ها با وزن خوانده می‌شوند', r[0].j.length === 3 && Number(r[0].j[0].weight) === 2);
// ذخیرهٔ دوباره باید جایگزین کند، نه اضافه
r = await q(`select public.native_essay_keywords_save_v355($1,0,'',$2::jsonb) j`, [E1, JSON.stringify([{ keyword: 'وتر', weight: 3 }])]);
r = await q(`select public.native_essay_keywords_v355($1) j`, [E1]);
ok('ذخیرهٔ دوباره گروه را جایگزین می‌کند', r[0].j.length === 1 && Number(r[0].j[0].weight) === 3);
// زیربخش جدا از کل سؤال
await q(`select public.native_essay_keywords_save_v355($1,0,'p2',$2::jsonb)`, [E1, JSON.stringify([{ keyword: 'گام دوم', weight: 1 }])]);
r = await q(`select public.native_essay_keywords_v355($1) j`, [E1]);
ok('کلیدواژهٔ زیربخش جدا نگه داشته می‌شود', r[0].j.length === 2 && r[0].j.some(x => x.part_key === 'p2'));
// سؤال دیگر دست‌نخورده
await q(`select public.native_essay_keywords_save_v355($1,1,'',$2::jsonb)`, [E1, JSON.stringify([{ keyword: 'سؤال دو', weight: 1 }])]);
r = await q(`select public.native_essay_keywords_v355($1) j`, [E1]);
ok('سؤال‌های دیگر دست‌نخورده می‌مانند', r[0].j.length === 3);

console.log('\n=== ۳) اعتبارسنجی ورودی ===');
await mustFail('وزن صفر رد شد', `select public.native_essay_keywords_save_v355($1,0,'','[{"keyword":"x","weight":0}]'::jsonb)`, [E1]);
await mustFail('وزن منفی رد شد', `select public.native_essay_keywords_save_v355($1,0,'','[{"keyword":"x","weight":-2}]'::jsonb)`, [E1]);
await mustFail('کلیدواژهٔ خالی رد شد', `select public.native_essay_keywords_save_v355($1,0,'','[{"keyword":"   "}]'::jsonb)`, [E1]);
await mustFail('کلیدواژهٔ بلندتر از ۱۲۰ رد شد', `select public.native_essay_keywords_save_v355($1,0,'',$2::jsonb)`, [E1, JSON.stringify([{ keyword: 'x'.repeat(121) }])]);
await mustFail('ورودی غیرآرایه رد شد', `select public.native_essay_keywords_save_v355($1,0,'','{}'::jsonb)`, [E1]);
await mustFail('شاخص سؤال منفی رد شد', `select public.native_essay_keywords_save_v355($1,-1,'','[]'::jsonb)`, [E1]);
const many = Array.from({ length: 61 }, (_, i) => ({ keyword: 'k' + i }));
await mustFail('بیش از ۶۰ کلیدواژه رد شد', `select public.native_essay_keywords_save_v355($1,0,'',$2::jsonb)`, [E1, JSON.stringify(many)]);
// اعتبارسنجی باید پیش از حذف انجام شود؛ دادهٔ قبلی نباید از دست برود
r = await q(`select public.native_essay_keywords_v355($1) j`, [E1]);
ok('ورودی نامعتبر دادهٔ قبلی را پاک نکرد', r[0].j.length === 3);
await mustFail('معلم دیگر نمی‌تواند ذخیره کند', `select public.native_essay_keywords_save_v355($1,0,'','[]'::jsonb)`, [E2]);

console.log('\n=== ۴) بررسی شباهت ===');
r = await q(`select public.native_similarity_reviews_v355($1) j`, [E1]);
ok('فهرست بررسی خالی در ابتدا', r[0].j.length === 0);
r = await q(`select public.native_similarity_review_save_v355($1,'a1|a2',0,'','checked',$2) j`, [E1, 'پاسخ‌ها شبیه بود ولی مستقل نوشته شده']);
ok('تصمیم «بررسی شد» ثبت شد', r[0].j.status === 'checked');
r = await q(`select public.native_similarity_review_save_v355($1,'a1|a2',0,'','confirmed',$2) j`, [E1, 'کپی محرز است']);
ok('تصمیم به «تأیید تخلف» به‌روز شد', r[0].j.status === 'confirmed');
r = await q(`select public.native_similarity_reviews_v355($1) j`, [E1]);
ok('به‌روزرسانی ردیف تازه نساخت', r[0].j.length === 1 && r[0].j[0].status === 'confirmed');
await mustFail('تأیید تخلف بدون توضیح رد شد', `select public.native_similarity_review_save_v355($1,'a1|a2',1,'','confirmed','')`, [E1]);
await mustFail('وضعیت نامعتبر رد شد', `select public.native_similarity_review_save_v355($1,'a1|a2',0,'','cheating','x')`, [E1]);
await mustFail('شناسهٔ جفت خالی رد شد', `select public.native_similarity_review_save_v355($1,'',0,'','checked','')`, [E1]);
await setU(T2);
await mustFail('معلم دیگر تصمیم‌ها را نمی‌بیند', `select public.native_similarity_reviews_v355($1)`, [E1]);
await mustFail('معلم دیگر نمی‌تواند تصمیم ثبت کند', `select public.native_similarity_review_save_v355($1,'a1|a2',0,'','checked','')`, [E1]);

console.log('\n=== ۵) نمره‌ها دست‌نخورده ماندند ===');
await setU(T1);
r = await q(`select score from public.answers where id='a1'`);
ok('نمرهٔ پاسخ تغییری نکرد', Number(r[0].score) === 10);
r = await q(`select count(*)::int c from public.answers`);
ok('هیچ پاسخی حذف یا اضافه نشد', r[0].c === 2);

console.log('\n=== ۶) RLS در برابر کاربر عادی ===');
await q(`grant select on public.exams, public.profiles, public.answers to authenticated`);  // مانند تولید
await q(`grant select, insert, update, delete on public.essay_keywords to authenticated`);
await q(`grant select, insert, update, delete on public.similarity_reviews to authenticated`);
await q(`grant usage, select on all sequences in schema public to authenticated`);
await db.exec(`set role authenticated`);
await setU(T2);
r = await q(`select count(*)::int c from public.essay_keywords`);
ok('معلم دیگر از راه جدول مستقیم هم چیزی نمی‌بیند', r[0].c === 0);
await db.exec(`reset role`);

console.log('\n=== ۷) اجرای دوباره بی‌خطر ===');
await setU(T1);
try { await db.exec(mig); ok('اجرای دوم بدون خطا', true); }
catch (e) { ok('اجرای دوم بدون خطا', false); console.log('    ' + String(e.message).slice(0, 250)); }
r = await q(`select public.native_essay_keywords_v355($1) j`, [E1]);
ok('کلیدواژه‌ها حفظ شدند', r[0].j.length === 3);
r = await q(`select public.native_similarity_reviews_v355($1) j`, [E1]);
ok('تصمیم‌ها حفظ شدند', r[0].j.length === 1);

console.log('\n=== ۸) فایل بررسی نصب ===');
const chk = fs.readFileSync(SQL('SQL_GRADING_SIMILARITY_V355_CHECK.sql'), 'utf8');
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

console.log(`\n${fail === 0 ? 'SQL355: PASS' : 'SQL355: FAIL'} (${pass} سنجه، ${fail} خطا)`);
process.exit(fail === 0 ? 0 : 1);
