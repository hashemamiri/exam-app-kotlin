/* =====================================================================
 * V357 — مولد بردارهای مشترک سایت و اپ برای زمان‌بندی و حالت آفلاین.
 * اجرا:  node scripts/gen_examtiming_vectors.mjs
 * خروجی: app/src/test/resources/v357_examtiming_vectors.json
 * بردارها از خودِ site/src/examtiming.js گرفته می‌شوند، نه چشمی.
 * ===================================================================== */
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { createRequire } from 'node:module';

const require = createRequire(import.meta.url);
const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const T = require(path.join(ROOT, 'site', 'src', 'examtiming.js'));

const times = [-5, 0, 1, 999, 1000, 59000, 60000, 65000, 3599000, 3600000, 3723000, 86400000];
const deadlines = [0, -1, 1000, 60000, 1000000];
const nows = [0, 500, 1000, 999999, 1000000, 1060000, 2000000];

const limitTexts = [
  '', '# راهنما\n\n', 'همه = 300', 'همه = ۳۰۰\n۱ = ۶۰\n2 = ٩٠',
  '1 = 60\n2 = 90\nهمه = 300', 'all = 120', '* = 600',
  'بدون مساوی', 'x = 60', '0 = 60', '1 = 5', '1 = 7201', '1 = abc',
  '۰ = ۶۰', 'همه=300\n\n#نظر\n۳ = ۴۵', '1 = 60\n1 = 90'
];

const grants = [
  [0, 'دلیل کافی است'], [15, 'مشکل یادگیری'], [240, 'دلیل کافی است'],
  [241, 'دلیل کافی است'], [-1, 'دلیل کافی است'], [15, 'کم'],
  [15, 'x'.repeat(501)], [15, '   '], [15, null], [1.5, 'دلیل کافی است']
];

const V = {
  latinDigits: ['', '۰۱۲۴۵۶۷۸۹', '٠١٢٤٥٦٧٨٩', 'abc۱۲۳', '۲ = ۳۴۵', 'سؤال ۲'].map(s => ({ in: s, out: T.latinDigits(s) })),
  align: [], remaining: [], clock: times.map(ms => ({ in: ms, out: T.formatClock(ms) })),
  question: [], validate: grants.map(([m, r]) => ({ minutes: m, reason: r, out: T.validateGrant(m, r) })),
  parse: limitTexts.map(t => ({ in: t, out: T.parseLimits(t) })),
  format: [], offline: [], events: []
};

deadlines.forEach(d => nows.forEach(sn => nows.forEach(ln => {
  V.align.push({ expires: d, serverNow: sn, localNow: ln, out: T.alignDeadline(d, sn, ln) });
})));
deadlines.forEach(d => nows.forEach(n => {
  V.remaining.push({ deadline: d, now: n, out: T.remainingMs(d, n) });
}));
[true, false].forEach(limited => deadlines.forEach(d => nows.forEach(n => [true, false].forEach(sl => {
  V.question.push({
    limited, deadline: d, now: n, serverLocked: sl,
    out: T.questionState({ limited }, d, n, sl)
  });
}))));
limitTexts.forEach(t => {
  const p = T.parseLimits(t);
  V.format.push({ limits: p.limits, out: T.formatLimits(p.limits) });
});
[{ token: '' }, { token: 'abc' }, { token: 'abc', used: true }, null].forEach(pack => {
  nows.forEach(n => [0, 1000, 60000].forEach(d => {
    V.offline.push({ pack, now: n, deadline: d, out: T.offlineSubmittable(pack, n, d) });
  }));
});
[['focus', 1000, 'خروج'], ['', 500, 'x'], ['copy', 0, null], ['x'.repeat(60), 99, 'd'.repeat(300)]].forEach(([k, at, d]) => {
  V.events.push({ kind: k, at, detail: d, out: T.pushEvent([], k, at, d) });
});

const out = path.join(ROOT, 'app', 'src', 'test', 'resources', 'v357_examtiming_vectors.json');
fs.mkdirSync(path.dirname(out), { recursive: true });
fs.writeFileSync(out, JSON.stringify(V, null, 1));
const n = Object.values(V).reduce((a, arr) => a + arr.length, 0);
console.log('بردارها نوشته شد: ' + n + ' مورد → ' + path.relative(ROOT, out));
