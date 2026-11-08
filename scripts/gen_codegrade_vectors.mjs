/* =====================================================================
 * V356 — مولد بردارهای آزمون مشترک سایت و اپ برای تصحیح کد (فاز صفر).
 *
 * اجرا:  node scripts/gen_codegrade_vectors.mjs
 * خروجی: app/src/test/resources/v356_codegrade_vectors.json
 *
 * بردارها از خودِ site/src/codegrade.js گرفته می‌شوند، پس چشمی نوشته
 * نشده‌اند. اگر الگوریتم سایت عوض شد، این را اجرا کنید و فایل خروجی را
 * با همان تغییر کامیت کنید؛ V356_CodeGradingParityTest آن را می‌سنجد.
 * ===================================================================== */
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { createRequire } from 'node:module';

const require = createRequire(import.meta.url);
const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const C = require(path.join(ROOT, 'site', 'src', 'codegrade.js'));

const outputs = [
  '', '   ', '5', '  5  ', '5\n', '5\r\n', 'a\n\nb', 'a\r\nb', 'a\rb',
  '  a  \n  b  ', 'OK', 'ok', 'a\tb', '\n\n', 'line1\nline2\n\n\nline3',
  '  spaced  out  ', 'MiXeD CaSe\nSecond LINE'
];
const optionSets = [
  { trimLines: true, collapseBlank: true, ignoreCase: false },
  { trimLines: false, collapseBlank: false, ignoreCase: false },
  { trimLines: true, collapseBlank: true, ignoreCase: true },
  { trimLines: false, collapseBlank: true, ignoreCase: true },
  { trimLines: true, collapseBlank: false, ignoreCase: false }
];

const testTexts = [
  '',
  '# فقط توضیح\n\n',
  'مثال ساده | 2 3 => 5',
  'مثال ساده | 2 3 => 5\nعدد منفی | 2 | -2 3 => 1\nحالت مرزی | پنهان | 0 0 => 0',
  'بدون جداکننده',
  'a => 1 => 2',
  'برچسب | => ',
  'x'.repeat(121) + ' | a => b',
  'برچسب | 0 | a => b',
  'برچسب | 101 | a => b',
  '2 | hidden | برچسب | a => b',
  'چندخطی | a\\nb => 1\\n2',
  'تب | a\\tb => ok',
  'بک‌اسلش | a\\\\nb => c\\\\d',
  Array.from({ length: 45 }, (_, i) => `t${i} | ${i} => ${i}`).join('\n')
];

const escapeCases = ['', 'plain', 'a\nb', 'a\tb', 'a\\b', 'a\\nb', '\\', '\\\\', 'a\n\\n\tb', 'x'.repeat(50)];

const scoreCases = [];
const testSets = [
  [],
  [{ id: 1, weight: 1 }],
  [{ id: 1, weight: 1 }, { id: 2, weight: 2 }, { id: 3, weight: 1 }],
  [{ id: 1, weight: 0.5 }, { id: 2, weight: 0.5 }],
  [{ id: 1, weight: 0 }, { id: 2, weight: -3 }]
];
const checkSets = [
  {},
  { 1: { passed: true } },
  { 1: { passed: true }, 2: { passed: false }, 3: { passed: true } },
  { 1: { passed: false }, 2: { passed: false } },
  { 1: { passed: true }, 2: { passed: true }, 3: { passed: true } }
];
testSets.forEach((tests, ti) => checkSets.forEach((checks, ci) => [0, 1, 2, 4, 10, 3].forEach(max => {
  scoreCases.push({ tests, checks, max, out: C.scoreFromTests(tests, checks, max), tag: `${ti}/${ci}/${max}` });
})));

const V = {
  normalize: [],
  matches: [],
  parse: testTexts.map(t => ({ in: t, out: C.parseTests(t) })),
  format: [],
  escape: escapeCases.map(t => ({ in: t, out: C.escapeText(t) })),
  unescape: escapeCases.map(t => ({ in: C.escapeText(t), out: C.unescapeText(C.escapeText(t)) })),
  score: scoreCases,
  summary: []
};

optionSets.forEach((o, oi) => outputs.forEach(t => {
  V.normalize.push({ in: t, opts: o, out: C.normalizeOutput(t, o) });
}));
for (let i = 0; i < outputs.length; i++) {
  for (let j = 0; j < outputs.length; j++) {
    V.matches.push({ a: outputs[i], b: outputs[j], opts: optionSets[0], out: C.outputMatches(outputs[i], outputs[j], optionSets[0]) });
  }
}
testTexts.forEach(t => {
  const p = C.parseTests(t);
  V.format.push({ tests: p.tests, out: C.formatTests(p.tests) });
});
testSets.forEach((tests, ti) => checkSets.forEach((checks, ci) => {
  V.summary.push({ tests, checks, out: C.summary(tests, checks), tag: `${ti}/${ci}` });
}));

const out = path.join(ROOT, 'app', 'src', 'test', 'resources', 'v356_codegrade_vectors.json');
fs.mkdirSync(path.dirname(out), { recursive: true });
fs.writeFileSync(out, JSON.stringify(V, null, 1));
const n = V.normalize.length + V.matches.length + V.parse.length + V.format.length +
  V.escape.length + V.unescape.length + V.score.length + V.summary.length;
console.log('بردارها نوشته شد: ' + n + ' مورد → ' + path.relative(ROOT, out));
