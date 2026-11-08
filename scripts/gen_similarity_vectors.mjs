/* =====================================================================
 * V355 — مولد بردارهای آزمون مشترک سایت و اپ.
 *
 * اجرا:  node scripts/gen_similarity_vectors.mjs
 * خروجی: app/src/test/resources/v355_similarity_vectors.json
 *
 * بردارها از خودِ پیاده‌سازی سایت گرفته می‌شوند، پس «چشمی» نوشته نشده‌اند.
 * اگر الگوریتم سایت را عوض کردید، این را اجرا کنید و فایل خروجی را با
 * همان تغییر کامیت کنید؛ آزمون V355_SimilarityParityTest آن را می‌سنجد.
 * ===================================================================== */
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { createRequire } from 'node:module';

const require = createRequire(import.meta.url);
const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const S = require(path.join(ROOT, 'site', 'src', 'similarity.js'));

const texts = [
  '', '   ', 'سلام', 'کوتاه',
  'قضیه فیثاغورس در مثلث قائم الزاویه بیان می کند که مجذور وتر برابر است با مجموع مجذور دو ضلع دیگر',
  'قضيه فيثاغورس في مثلث قائم الزاويه بيان مي كند که مجذور وتر برابر است با مجموع مجذور دو ضلع ديگر',
  'می‌روم به مدرسه و کتاب می‌خوانم', 'ميروم به مدرسه و كتاب ميخوانم',
  'اعداد ۱۲۳۴۵ و ٧٨ برابر 12345 و 6789 هستند',
  'Hello, World! This is a TEST — with punctuation; and digits 42.',
  'قیمت ۲۵۰ تومان است! واقعاً؟ بله…',
  'Line one\nLine two\tTabbed\r\nWindows'
];

const codes = [
  '', 'x=1', 'def add(x, y):\n    # جمع\n    return x + y',
  '```python\ndef add(x, y):\n    # جمع دو عدد\n    return x + y\n```',
  '```python\ndef add(first, second):\n    # توضیح کاملاً متفاوت\n    return first + second\n```',
  '```js\nfunction f(a,b){ /* note */ return a+b; }\n```',
  'x = "hi"; y = 3.14; z = \'quoted\';',
  'if (a >= 10 && b != 3) { print("yes"); } else { print("no"); }',
  'SELECT * FROM t WHERE id = 5; -- comment\n',
  'a--b; c++; d = e / f; # trailing'
];

const V = {
  normalize: texts.map(t => ({ in: t, out: S.normalizeText(t) })),
  essayTokens: texts.map(t => ({ in: t, out: S.essayTokens(t) })),
  codeTokens: codes.map(t => ({ in: t, out: S.codeTokens(t) })),
  stripFence: codes.map(t => ({ in: t, out: S.stripFence(t) })),
  stripComments: codes.map(t => ({ in: t, out: S.stripComments(t) })),
  hash: ['', 'a', 'ab', 'a1', 'برگه', '11111111-1111-4111-8111-111111111111', 'def\u0001ID\u0001(\u0001ID\u0001,']
    .map(t => ({ in: t, out: S.hash32(t) })),
  pairs: [],
  assist: []
};

for (let i = 0; i < texts.length; i++) {
  for (let j = i + 1; j < texts.length; j++) {
    V.pairs.push({ a: texts[i], b: texts[j], kind: 'essay', out: S.compare(S.essayTokens(texts[i]), S.essayTokens(texts[j]), 0) });
  }
}
for (let i = 0; i < codes.length; i++) {
  for (let j = i + 1; j < codes.length; j++) {
    V.pairs.push({ a: codes[i], b: codes[j], kind: 'code', out: S.compare(S.codeTokens(codes[i]), S.codeTokens(codes[j]), 0) });
  }
}

const kws = [
  { keyword: 'فیثاغورس', weight: 2 },
  { keyword: 'وتر', weight: 1 },
  { keyword: 'مجذور|توان دو', weight: 1 },
  { keyword: 'قائم الزاویه', weight: 1.5 }
];
[texts[4], texts[6], texts[9], ''].forEach(t => V.assist.push({ answer: t, max: 4, keywords: kws, out: S.essayAssist(kws, t, 4) }));

V.shingles = [
  { tokens: ['a', 'b', 'c', 'd', 'e', 'f', 'g'], k: 5, out: S.shingles(['a', 'b', 'c', 'd', 'e', 'f', 'g'], 5) },
  { tokens: ['a', 'b'], k: 5, out: S.shingles(['a', 'b'], 5) }
];
V.pairKey = [
  { a: 'z', b: 'a', out: S.pairKey('z', 'a') },
  { a: 'a', b: 'z', out: S.pairKey('a', 'z') },
  { a: 'same', b: 'same', out: S.pairKey('same', 'same') }
];

const out = path.join(ROOT, 'app', 'src', 'test', 'resources', 'v355_similarity_vectors.json');
fs.mkdirSync(path.dirname(out), { recursive: true });
fs.writeFileSync(out, JSON.stringify(V, null, 1));
const n = V.normalize.length + V.codeTokens.length + V.pairs.length + V.assist.length;
console.log('بردارها نوشته شد: ' + n + ' مورد → ' + path.relative(ROOT, out));
