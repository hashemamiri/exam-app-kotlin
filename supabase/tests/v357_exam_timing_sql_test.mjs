// آزمون V357 — زمان اضافه، محدودیت سؤال، آفلاین روی PostgreSQL واقعی
// اجرا: npm install @electric-sql/pglite  سپس  node supabase/tests/v357_exam_timing_sql_test.mjs
import { PGlite } from '@electric-sql/pglite';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = (() => {
  let d = path.dirname(fileURLToPath(import.meta.url));
  while (d !== path.dirname(d) && !fs.existsSync(path.join(d, 'settings.gradle.kts'))) d = path.dirname(d);
  return d;
})();
const SQL = (n) => fs.existsSync(path.join(ROOT, n)) ? path.join(ROOT, n) : path.join(ROOT, 'supabase', 'sql', n);

process.on('uncaughtException', (e) => { console.log('خطای مرگبار: ' + String(e.message).slice(0,200)); process.exit(1); });
process.on('unhandledRejection', (e) => { console.log('خطای مرگبار: ' + String(e && e.message || e).slice(0,200)); process.exit(1); });
const db = new PGlite();
let pass = 0, fail = 0;
const ok = (n, c, extra) => {
  if (c) { pass++; console.log('  ✓ ' + n); }
  else { fail++; console.log('  ✗ ' + n + (extra ? ' — ' + extra : '')); }
};
const q = async (sql, p) => (await db.query(sql, p)).rows;
async function mustFail(n, sql, p) {
  try { await db.query(sql, p); ok(n + ' (باید رد می‌شد)', false); }
  catch (e) { ok(n, true); }
}
const uid = () => crypto.randomUUID();
const setU = async (u) => { await q(`select set_config('test.uid',$1,false)`, [u || '']); };

await db.exec(`
create schema if not exists auth;
create or replace function auth.uid() returns uuid language sql stable as
$$ select nullif(current_setting('test.uid', true), '')::uuid $$;
do $$ begin
  if not exists (select 1 from pg_roles where rolname='authenticated') then create role authenticated; end if;
end $$;
grant usage on schema public to authenticated;
`);

const T1 = uid(), T2 = uid(), S1 = uid(), S2 = uid();
const E1 = 'exam-' + uid().slice(0, 8);
await db.exec(`
create table public.exams(id text primary key, teacher_id uuid, title text, duration integer default 60);
create table public.exam_attempts(
  id bigserial primary key, exam_id text, student_id uuid, student_name text,
  attempt_no integer default 1, started_at timestamptz, expires_at timestamptz,
  section_deadlines jsonb not null default '{}'::jsonb,
  section_locked_responses jsonb not null default '{}'::jsonb,
  abandoned boolean default false, shown_q jsonb);
create table public.exam_sessions(
  exam_id text, student_id uuid, started_at timestamptz, expires_at timestamptz,
  primary key (exam_id, student_id));
create table public.answers(
  id text primary key, exam_id text, student_id uuid, attempt_no integer default 1,
  responses jsonb, meta jsonb not null default '{}'::jsonb, total_grade numeric default 0);
`);
await q(`insert into public.exams values ($1,$2,'ریاضی',60)`, [E1, T1]);

console.log('\n=== ۱) اجرای مهاجرت ===');
const mig = fs.readFileSync(SQL('SQL_EXAM_TIMING_V357.sql'), 'utf8');
try { await db.exec(mig); ok('مهاجرت کامل اجرا شد', true); }
catch (e) { ok('مهاجرت کامل اجرا شد', false, String(e.message).slice(0, 200)); process.exit(1); }

console.log('\n=== ۲) زمان اضافه پیش از شروع ===');
await setU(T2);
await mustFail('معلم دیگر مجوز نمی‌دهد', `select public.native_time_grant_save_v357($1,$2,15,'دلیل کافی است')`, [E1, S1]);
await setU(T1);
await mustFail('دلیل کوتاه رد شد', `select public.native_time_grant_save_v357($1,$2,15,'کم')`, [E1, S1]);
await mustFail('زمان بیش از سقف رد شد', `select public.native_time_grant_save_v357($1,$2,241,'دلیل کافی است')`, [E1, S1]);
await mustFail('زمان منفی رد شد', `select public.native_time_grant_save_v357($1,$2,-5,'دلیل کافی است')`, [E1, S1]);
let r = await q(`select public.native_time_grant_save_v357($1,$2,15,'مشکل یادگیری تأییدشده') j`, [E1, S1]);
ok('مجوز ۱۵ دقیقه ثبت شد', r[0].j.extra_minutes === 15);

// شبیه‌سازی شروع آزمون: تلاش با مهلت ۶۰ دقیقه
await q(`insert into public.exam_attempts(exam_id,student_id,attempt_no,started_at,expires_at)
         values ($1,$2,1,now(),now()+interval '60 minutes')`, [E1, S1]);
r = await q(`select time_grant_applied g, extract(epoch from (expires_at - started_at))/60 mins
             from public.exam_attempts where student_id=$1`, [S1]);
ok('تریگر ۱۵ دقیقه اضافه کرد', Math.round(Number(r[0].mins)) === 75, String(r[0].mins));
ok('مقدار اعمال‌شده ثبت شد', r[0].g === 15);

console.log('\n=== ۳) بی‌تکرار بودن تریگر ===');
await q(`update public.exam_attempts set expires_at = expires_at where student_id=$1`, [S1]);
r = await q(`select extract(epoch from (expires_at - started_at))/60 mins from public.exam_attempts where student_id=$1`, [S1]);
ok('به‌روزرسانی دوباره زمان را اضافه نکرد', Math.round(Number(r[0].mins)) === 75, String(r[0].mins));
// ادامهٔ آزمون: مهلت از نو نوشته می‌شود
await q(`update public.exam_attempts set expires_at = now()+interval '30 minutes', time_grant_applied=0 where student_id=$1`, [S1]);
r = await q(`select time_grant_applied g from public.exam_attempts where student_id=$1`, [S1]);
ok('پس از بازنشانی، مجوز دوباره اعمال شد', r[0].g === 15);

console.log('\n=== ۴) تغییر و لغو زندهٔ مجوز ===');
await q(`select public.native_time_grant_save_v357($1,$2,30,'تمدید به سی دقیقه')`, [E1, S1]);
r = await q(`select time_grant_applied g from public.exam_attempts where student_id=$1`, [S1]);
ok('مجوز به ۳۰ تغییر کرد', r[0].g === 30);
await q(`select public.native_time_grant_save_v357($1,$2,0,'لغو مجوز زمانی')`, [E1, S1]);
r = await q(`select time_grant_applied g from public.exam_attempts where student_id=$1`, [S1]);
ok('لغو مجوز زمان را پس گرفت', r[0].g === 0);
await q(`select public.native_time_grant_save_v357($1,$2,15,'مشکل یادگیری تأییدشده')`, [E1, S1]);

console.log('\n=== ۵) نشست هم زمان اضافه می‌گیرد ===');
await q(`insert into public.exam_sessions values ($1,$2,now(),now()+interval '60 minutes')`, [E1, S1]);
r = await q(`select time_grant_applied g, extract(epoch from (expires_at - started_at))/60 mins
             from public.exam_sessions where student_id=$1`, [S1]);
ok('نشست ۱۵ دقیقه اضافه گرفت', Math.round(Number(r[0].mins)) === 75 && r[0].g === 15, JSON.stringify(r[0]));
await q(`insert into public.exam_attempts(exam_id,student_id,attempt_no,started_at,expires_at)
         values ($1,$2,1,now(),now()+interval '60 minutes')`, [E1, S2]);
r = await q(`select time_grant_applied g from public.exam_attempts where student_id=$1`, [S2]);
ok('دانش‌آموز بدون مجوز دست‌نخورده ماند', r[0].g === 0);

console.log('\n=== ۶) دسترسی به مجوز ===');
r = await q(`select public.native_time_grants_v357($1) j`, [E1]);
ok('معلم فهرست را می‌بیند', r[0].j.length === 1 && r[0].j[0].extra_minutes === 15);
await setU(T2);
await mustFail('معلم دیگر فهرست را نمی‌بیند', `select public.native_time_grants_v357($1)`, [E1]);

console.log('\n=== ۷) محدودیت زمان سؤال ===');
await setU(T1);
await mustFail('زمان کمتر از ۱۰ ثانیه رد شد', `select public.native_question_limits_save_v357($1,'{"0":5}'::jsonb)`, [E1]);
await mustFail('زمان بیش از سقف رد شد', `select public.native_question_limits_save_v357($1,'{"0":7201}'::jsonb)`, [E1]);
await mustFail('ورودی غیرشیء رد شد', `select public.native_question_limits_save_v357($1,'[]'::jsonb)`, [E1]);
await mustFail('شاخص نامعتبر رد شد', `select public.native_question_limits_save_v357($1,'{"-2":60}'::jsonb)`, [E1]);
r = await q(`select public.native_question_limits_save_v357($1,'{"-1":300,"0":60}'::jsonb) j`, [E1]);
ok('دو محدودیت ذخیره شد', r[0].j.count === 2);
r = await q(`select public.native_question_limits_v357($1) j`, [E1]);
ok('پیش‌فرض و اختصاصی خوانده شد', r[0].j['-1'] === 300 && r[0].j['0'] === 60, JSON.stringify(r[0].j));
await setU(T2);
await mustFail('معلم دیگر محدودیت نمی‌گذارد', `select public.native_question_limits_save_v357($1,'{}'::jsonb)`, [E1]);
await setU(T1);
await q(`select public.native_question_limits_save_v357($1,'{"-1":300,"0":60}'::jsonb)`, [E1]);

console.log('\n=== ۸) همگام‌سازی زمان سؤال ===');
await setU(S1);
r = await q(`select public.native_sync_exam_question_v357($1,0) j`, [E1]);
ok('سؤال ۰ محدودیت اختصاصی گرفت', r[0].j.limited === true && r[0].j.seconds === 60, JSON.stringify(r[0].j));
ok('در ابتدا قفل نیست', r[0].j.locked === false);
const firstExp = r[0].j.expires_at;
r = await q(`select public.native_sync_exam_question_v357($1,0) j`, [E1]);
ok('بازگشت به سؤال زمان را از نو شروع نکرد', r[0].j.expires_at === firstExp);
r = await q(`select public.native_sync_exam_question_v357($1,1) j`, [E1]);
ok('سؤال ۱ پیش‌فرض ۳۰۰ ثانیه گرفت', r[0].j.seconds === 300);
await setU(S2);
r = await q(`select public.native_sync_exam_question_v357($1,5) j`, [E1]);
ok('سؤال بدون محدودیت آزاد است', r[0].j.limited === false || r[0].j.seconds === 300);
await mustFail('شاخص منفی رد شد', `select public.native_sync_exam_question_v357($1,-1)`, [E1]);

console.log('\n=== ۹) قفل شدن پس از انقضا ===');
await setU(T1);
await q(`update public.exam_attempts
            set question_deadlines = jsonb_set(question_deadlines,'{0}', to_jsonb((now()-interval '1 second')::text))
          where student_id=$1`, [S1]);
await setU(S1);
r = await q(`select public.native_sync_exam_question_v357($1,0) j`, [E1]);
ok('سؤال منقضی قفل شد', r[0].j.locked === true, JSON.stringify(r[0].j));
r = await q(`select question_locked from public.exam_attempts where student_id=$1 order by attempt_no desc limit 1`, [S1]);
ok('قفل در پایگاه‌داده ثبت شد', r[0].question_locked['0'] === true);

console.log('\n=== ۱۰) مهلت سؤال از مهلت آزمون جلوتر نمی‌رود ===');
await q(`update public.exam_attempts set expires_at = now()+interval '20 seconds' where student_id=$1`, [S1]);
r = await q(`select public.native_sync_exam_question_v357($1,3) j`, [E1]);
const att = await q(`select expires_at from public.exam_attempts where student_id=$1 order by attempt_no desc limit 1`, [S1]);
ok('مهلت سؤال به مهلت آزمون بریده شد', new Date(r[0].j.expires_at) <= new Date(att[0].expires_at));

console.log('\n=== ۱۱) بستهٔ آفلاین ===');
await setU(T1);
await q(`update public.exam_attempts set expires_at = now()+interval '60 minutes' where student_id=$1`, [S1]);
await setU(S2);
await mustFail('دانش‌آموز دیگر بستهٔ این آزمون را نمی‌گیرد', `select public.native_offline_pack_v357('nope')`);
await setU(S1);
r = await q(`select public.native_offline_pack_v357($1) j`, [E1]);
const token = r[0].j.token;
ok('توکن صادر شد', typeof token === 'string' && token.length === 64, String(token && token.length));
ok('مهلت در بسته هست', !!r[0].j.expires_at);
ok('محدودیت‌ها در بسته هست', r[0].j.limits['0'] === 60);
ok('کلید پاسخ در بسته نیست', !('answer_key' in r[0].j) && !('answers' in r[0].j));
r = await q(`select public.native_offline_pack_v357($1) j`, [E1]);
ok('درخواست دوباره همان توکن را می‌دهد', r[0].j.token === token);

console.log('\n=== ۱۲) ارسال آفلاین ===');
await q(`insert into public.answers(id,exam_id,student_id,responses) values ('a1',$1,$2,'[]'::jsonb)`, [E1, S1]);
await mustFail('توکن اشتباه رد شد', `select public.native_offline_submit_v357($1,'wrong','["x"]'::jsonb,'[]'::jsonb,now())`, [E1]);
await mustFail('پاسخ غیرآرایه رد شد', `select public.native_offline_submit_v357($1,$2,'{}'::jsonb,'[]'::jsonb,now())`, [E1, token]);
await mustFail('زمان پایان پس از مهلت رد شد',
  `select public.native_offline_submit_v357($1,$2,'["x"]'::jsonb,'[]'::jsonb,now()+interval '2 hours')`, [E1, token]);
await mustFail('زمان پایان خالی رد شد',
  `select public.native_offline_submit_v357($1,$2,'["x"]'::jsonb,'[]'::jsonb,null)`, [E1, token]);
r = await q(`select public.native_offline_submit_v357($1,$2,'["پاسخ آفلاین"]'::jsonb,'[{"t":"focus"}]'::jsonb,now()) j`, [E1, token]);
ok('ارسال آفلاین پذیرفته شد', r[0].j.ok === true && r[0].j.offline === true);
r = await q(`select responses, meta from public.answers where id='a1'`);
ok('پاسخ ذخیره شد', r[0].responses[0] === 'پاسخ آفلاین');
ok('برگه آفلاین علامت خورد', r[0].meta.offline === true);
ok('رویدادها ثبت شد', Array.isArray(r[0].meta.offline_events) && r[0].meta.offline_events.length === 1);
await mustFail('توکن یک‌بارمصرف است',
  `select public.native_offline_submit_v357($1,$2,'["دوباره"]'::jsonb,'[]'::jsonb,now())`, [E1, token]);
await mustFail('بستهٔ دوم پس از مصرف صادر نمی‌شود', `select public.native_offline_pack_v357($1)`, [E1]);
r = await q(`select total_grade from public.answers where id='a1'`);
ok('نمره دست‌نخورده ماند', Number(r[0].total_grade) === 0);

console.log('\n=== ۱۳) RLS از دید دانش‌آموز ===');
await q(`grant select, insert, update, delete on public.exam_time_grants, public.exam_question_limits to authenticated`);
await q(`grant select on public.exams, public.exam_attempts to authenticated`);
await db.exec(`set role authenticated`);
await setU(S2);
r = await q(`select count(*)::int c from public.exam_time_grants`);
ok('دانش‌آموز مجوز دیگری را نمی‌بیند', r[0].c === 0, String(r[0].c));
await setU(S1);
r = await q(`select count(*)::int c from public.exam_time_grants`);
ok('دانش‌آموز مجوز خودش را می‌بیند', r[0].c === 1, String(r[0].c));
await mustFail('دانش‌آموز نمی‌تواند مجوز بنویسد',
  `insert into public.exam_time_grants(exam_id,student_id,extra_minutes,reason) values ($1,$2,60,'دلیل ساختگی')`, [E1, S1]);
await db.exec(`reset role`);

console.log('\n=== ۱۴) اجرای دوباره بی‌خطر ===');
await setU(T1);
try { await db.exec(mig); ok('اجرای دوم بدون خطا', true); }
catch (e) { ok('اجرای دوم بدون خطا', false, String(e.message).slice(0, 180)); }
r = await q(`select public.native_time_grants_v357($1) j`, [E1]);
ok('مجوزها حفظ شدند', r[0].j.length === 1);
r = await q(`select public.native_question_limits_v357($1) j`, [E1]);
ok('محدودیت‌ها حفظ شدند', r[0].j['0'] === 60);
r = await q(`select time_grant_applied g from public.exam_attempts where student_id=$1 order by attempt_no desc limit 1`, [S1]);
ok('اجرای دوباره زمان را دوباره اضافه نکرد', r[0].g === 15, String(r[0].g));

console.log('\n=== ۱۵) فایل بررسی نصب ===');
const chk = fs.readFileSync(SQL('SQL_EXAM_TIMING_V357_CHECK.sql'), 'utf8');
// V357.1 — تقسیم روی ';' باید بلوک‌های $$ ... $$ و رشته‌ها را نشکند،
// وگرنه بلوک do تکه‌تکه می‌شود و خطای دروغ می‌دهد.
function splitSql(text) {
  const lines = text.split('\n').filter(l => !l.trim().startsWith('--')).join('\n');
  const out = []; let buf = '', i = 0, dollar = false, quote = false;
  while (i < lines.length) {
    if (!quote && lines.startsWith('$$', i)) { dollar = !dollar; buf += '$$'; i += 2; continue; }
    const c = lines[i];
    if (!dollar && c === "'") quote = !quote;
    if (c === ';' && !dollar && !quote) { if (buf.trim()) out.push(buf.trim()); buf = ''; i++; continue; }
    buf += c; i++;
  }
  if (buf.trim()) out.push(buf.trim());
  return out;
}
const stmts = splitSql(chk);
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

console.log(`\n${fail === 0 ? 'SQL357: PASS' : 'SQL357: FAIL'} (${pass} سنجه، ${fail} خطا)`);
process.exit(fail === 0 ? 0 : 1);
