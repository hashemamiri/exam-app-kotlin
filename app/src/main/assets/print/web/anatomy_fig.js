(function () {
  var CATS = [
    { id: 'all', name: 'همه' },
    { id: 'body', name: 'بدن' },
    { id: 'bone', name: 'استخوان' },
    { id: 'mus', name: 'ماهیچه' },
    { id: 'circ', name: 'رگ و خون' },
    { id: 'resp', name: 'تنفس' },
    { id: 'dig', name: 'گوارش' },
    { id: 'nerv', name: 'عصب' },
    { id: 'urin', name: 'ادراری' },
    { id: 'endo', name: 'غدد' },
    { id: 'lymph', name: 'لنفاوی' },
    { id: 'sense', name: 'حواس' },
    { id: 'cell', name: 'سلول' },
    { id: 'repro', name: 'تولیدمثل' },
    { id: 'd3', name: 'سه‌بعدی' }
  ];

  var TYPES = [
    { id: 'cell3', cat: 'cell', name: 'سلول', cap: '۳بعدی · ساختار یک سلول انسانی — واحد سازندهٔ بدن (هسته، میتوکندری، ...)' },
    { id: 'dna', cat: 'cell', name: 'دی‌ان‌ای', cap: '۳بعدی · مارپیچ دوگانهٔ DNA — نقشهٔ ساختمان بدن' },
    { id: 'bodyF', cat: 'body', name: 'بدن روبه‌رو', cap: '۲بعدی · نمای روبه‌رو بدن — آناتومی سطحی' },
    { id: 'bodyB', cat: 'body', name: 'بدن پشت', cap: '۲بعدی · نمای پشت — کتف، ستون فقرات و پشت پاها' },
    { id: 'bodyS', cat: 'body', name: 'بدن نیمرخ', cap: '۲بعدی · نیمرخ — قوس طبیعی ستون فقرات' },
    { id: 'head', cat: 'body', name: 'سر و گردن', cap: '۲بعدی · سر و گردن — مغز، حفره‌ها، حلق و حنجره' },
    { id: 'torso', cat: 'bone', name: 'اسکلت', cap: '۲بعدی · رسم آموزشی اسکلت — نمای روبه‌رو' },
    { id: 'body3', cat: 'd3', name: 'اسکلت سه‌بعدی', cap: '۳بعدی · رندر پزشکی اسکلت' },
    { id: 'skull', cat: 'bone', name: 'جمجمه', cap: '۲بعدی · جمجمه — کاسهٔ سر، کاسهٔ چشم و فک' },
    { id: 'spine', cat: 'bone', name: 'ستون فقرات', cap: '۲بعدی · ستون فقرات — مهره‌ها، دیسک‌ها، خاجی و دنباله' },
    { id: 'joint', cat: 'bone', name: 'مفصل زانو', cap: '۲بعدی · مفصل زانو — غضروف، منیسک، رباط و تاندون' },
    { id: 'handB', cat: 'bone', name: 'استخوان دست', cap: '۲بعدی · ۲۷ استخوان مچ و دست — کارپ‌ها، کف و بندانگشتی' },
    { id: 'ribs', cat: 'bone', name: 'قفسه سینه', cap: '۲بعدی · قفسه سینه — ۱۲ جفت دنده + جناغ' },
    { id: 'pelvis', cat: 'bone', name: 'لگن', cap: '۲بعدی · لگن — سرین، نشیمن، پوپیس + خاجی' },
    { id: 'legB', cat: 'bone', name: 'استخوان پا', cap: '۲بعدی · استخوان پا — ران، کشکک، درشت‌نی، نازک‌نی' },
    { id: 'footB', cat: 'bone', name: 'کف پا', cap: '۲بعدی · کف پا — ۲۶ استخوان تارسال، کف و انگشتان' },
    { id: 'skel', cat: 'mus', name: 'ماهیچه‌ها', cap: '۲بعدی · رسم آموزشی ماهیچه‌ها — نمای روبه‌رو' },
    { id: 'musB', cat: 'mus', name: 'ماهیچه پشت ۳د', cap: '۳بعدی · رندر ماهیچه — نمای پشت' },
    { id: 'mus3', cat: 'd3', name: 'ماهیچه روبه‌رو ۳د', cap: '۳بعدی · رندر عضلات — نمای روبه‌رو' },
    { id: 'sarc', cat: 'mus', name: 'تار عضلانی', cap: 'ریزمقیاس · تارهای عضلانی و واحد انقباض (سارکومر)' },
    { id: 'biceps', cat: 'mus', name: 'دوسر بازو', cap: '۲بعدی · دوسر بازو — خم‌کنندهٔ آرنج' },
    { id: 'quad', cat: 'mus', name: 'چهارسر ران', cap: '۲بعدی · چهارسر ران — بازکنندهٔ زانو' },
    { id: 'abs', cat: 'mus', name: 'ماهیچه شکم', cap: '۲بعدی · عضلات شکم — مستقیم (شش‌تکه) و مورب‌ها' },
    { id: 'heartM', cat: 'mus', name: 'بافت عضله قلب', cap: 'بافت عضلهٔ قلب: سلول‌های شاخه‌دار و مخطط — انقباض خودکار و بی‌وقفه' },
    { id: 'circ', cat: 'circ', name: 'شبکه رگ‌ها', cap: '۲بعدی · کل شبکه سرخرگ‌ها (قرمز) و سیاهرگ‌ها (آبی)' },
    { id: 'heart', cat: 'circ', name: 'مقطع قلب', cap: '۲بعدی · مقطع قلب — چهار حفره و دریچه‌ها' },
    { id: 'heart3', cat: 'd3', name: 'قلب سه‌بعدی', cap: '۳بعدی · رندر قلب با سرخرگ‌های اصلی' },
    { id: 'cap', cat: 'circ', name: 'مویرگ', cap: '۳بعدی · ریزمقیاس: گلبول‌های قرمز در حال عبور از مویرگ' },
    { id: 'blood', cat: 'cell', name: 'سلول‌های خون', cap: 'ریزمقیاس · گلبول قرمز، گلبول سفید و پلاکت در پلاسما' },
    { id: 'artery', cat: 'circ', name: 'سرخرگ و سیاهرگ', cap: 'سرخرگ: دیوارهٔ ضخیم و کشسان • سیاهرگ: دیوارهٔ نازک با دریچه • مویرگ: دیوارهٔ یک‌سلولی' },
    { id: 'flow', cat: 'circ', name: 'مدار گردش خون', cap: 'مدار کوچک (ریوی) و مدار بزرگ (سیستمیک) — خون کم‌اکسیژن و پراکسیژن' },
    { id: 'resp', cat: 'resp', name: 'دستگاه تنفس', cap: '۲بعدی · رسم آموزشی — نای، برونش‌ها، ریه‌ها و دیافراگم' },
    { id: 'lungs', cat: 'd3', name: 'ریه سه‌بعدی', cap: '۳بعدی · رندر ریه‌ها و درخت برونشی' },
    { id: 'alve', cat: 'resp', name: 'حبابچه', cap: '۳بعدی · ریزمقیاس: کیسه‌های هوایی و مویرگ‌های اطراف' },
    { id: 'trach', cat: 'resp', name: 'نای و نایژه', cap: 'نای حدود ۱۲ سانتی‌متر با ۱۶ تا ۲۰ حلقهٔ غضروفی — سپس نایژه‌ها و کیسه‌های هوایی' },
    { id: 'dig', cat: 'dig', name: 'دستگاه گوارش', cap: '۲بعدی · رسم آموزشی — مسیر کامل گوارش و اندام‌های کمکی' },
    { id: 'org3', cat: 'd3', name: 'گوارش سه‌بعدی', cap: '۳بعدی · رندر معده، جگر و روده‌ها' },
    { id: 'liver', cat: 'dig', name: 'کبد', cap: '۲بعدی · کبد — بزرگ‌ترین اندام داخلی، حدود ۵۰۰ وظیفه' },
    { id: 'stomach', cat: 'dig', name: 'معده', cap: '۲بعدی · معده — ته‌هسته، بدنه، پیلور و چین‌های مخاط' },
    { id: 'tooth', cat: 'dig', name: 'دندان', cap: 'کودکان ۲۰ دندان شیری، بزرگسالان ۳۲ دندان دائمی — مینا سخت‌ترین مادهٔ بدن' },
    { id: 'panc', cat: 'dig', name: 'لوزالمعده', cap: '۲بعدی · لوزالمعده (پانکراس) — آنزیم گوارشی + کارخانهٔ انسولین' },
    { id: 'brain', cat: 'nerv', name: 'مغز', cap: '۲بعدی · مغز از نمای کنار — مخ، مخچه، ساقه مغز' },
    { id: 'nerv', cat: 'nerv', name: 'دستگاه عصبی', cap: '۲بعدی · مغز، نخاع و شبکهٔ اعصاب کل بدن' },
    { id: 'nerv3', cat: 'd3', name: 'عصب سه‌بعدی', cap: '۳بعدی · رندر مغز، نخاع و شبکهٔ اعصاب در بدن شفاف' },
    { id: 'brain3', cat: 'd3', name: 'مغز سه‌بعدی', cap: '۳بعدی · رندر مغز — مخ، مخچه و ساقهٔ مغز' },
    { id: 'neuron', cat: 'nerv', name: 'نورون', cap: '۲بعدی · ریزمقیاس: ساختار نورون و سیناپس' },
    { id: 'cord', cat: 'nerv', name: 'نخاع', cap: 'نخاع حدود ۴۵ سانتی‌متر درون کانال مهره‌ها — مادهٔ خاکستری در مرکز' },
    { id: 'urin', cat: 'urin', name: 'دستگاه ادراری', cap: '۲بعدی · کلیه‌ها، غدد فوق کلیوی، حالب‌ها و مثانه' },
    { id: 'kidney', cat: 'd3', name: 'کلیه سه‌بعدی', cap: '۳بعدی · رندر کلیه‌ها، حالب‌ها و مثانه' },
    { id: 'neph', cat: 'urin', name: 'نفرون', cap: 'هر کلیه حدود یک میلیون نفرون — روزانه ۱۸۰ لیتر خون فیلتر می‌شود' },
    { id: 'endo', cat: 'endo', name: 'غدد درون‌ریز', cap: '۲بعدی · غدد درون‌ریز: هیپوفیز، تیروئید، آدرنال، پانکراس' },
    { id: 'thyr', cat: 'endo', name: 'تیروئید', cap: 'تیروئید با T3 و T4 سوخت‌وساز را تنظیم می‌کند • پاراتیروئید کلسیم خون را کنترل می‌کند' },
    { id: 'adr', cat: 'endo', name: 'فوق کلیه', cap: 'غدهٔ فوق کلیه — کورتکس کورتیزول و مدولا آدرنالین ترشح می‌کند' },
    { id: 'lymph', cat: 'lymph', name: 'لنفاوی', cap: '۲بعدی · شبکهٔ عروق و گره‌های لنفاوی، طحال و تیموس' },
    { id: 'wbc', cat: 'cell', name: 'گلبول سفید', cap: 'ریزمقیاس · گلبول سفید در حال شکار باکتری' },
    { id: 'spleen', cat: 'lymph', name: 'طحال', cap: '۲بعدی · طحال — فیلتر خون، انبار گلبول و بافت لنفاوی' },
    { id: 'skin', cat: 'body', name: 'پوست', cap: '۲بعدی · مقطع لایه‌های پوست: فولیکول مو، غدد و گیرنده‌ها' },
    { id: 'embryo', cat: 'repro', name: 'رشد جنین', cap: 'توسعهٔ جنین — از سلول تخم تا نوزاد در رحم' },
    { id: 'uterus', cat: 'repro', name: 'رحم و تخمدان', cap: '۲بعدی · رحم، لوله‌های رحم و تخمدان‌ها — خانهٔ جنین' },
    { id: 'testis', cat: 'repro', name: 'بیضه', cap: '۲بعدی · بیضه، اپیدیدیم، واز و پروستات' },
    { id: 'senses', cat: 'sense', name: 'حواس پنج‌گانه', cap: '۲بعدی · اندام‌های حسی: چشم، گوش، بینی و زبان' },
    { id: 'eye', cat: 'sense', name: 'چشم', cap: '۲بعدی · چشم — قرنیه، عدسی، شبکیه و عصب بینایی' },
    { id: 'ear', cat: 'sense', name: 'گوش', cap: '۲بعدی · گوش — پردهٔ صماخ، استخوانچه‌ها، حلزون و تعادل' },
    { id: 'tongue', cat: 'sense', name: 'زبان', cap: '۲بعدی · زبان — حوزه‌های چشایی و غدد بزاقی' },
    { id: 'nose', cat: 'sense', name: 'بینی', cap: '۲بعدی · بینی — حفره، پیچک‌ها، عصب بویایی و سینوس‌ها' },
    { id: 'rbc', cat: 'cell', name: 'گلبول قرمز', cap: 'ریزمقیاس · گلبول‌های قرمز (اریتروسیت) — دیسک مقعر دوطرفه' },
    { id: 'plate', cat: 'cell', name: 'پلاکت', cap: 'ریزمقیاس · پلاکت‌ها (ترومبوسیت) — فعال‌شده با زائده و رشته‌های لخته' }
  ];

  var FILE = {
    cell3: 'anatomy/atlas-01.jpg',
    dna: 'anatomy/atlas-02.jpg',
    bodyF: 'anatomy/atlas-03.jpg',
    bodyB: 'anatomy/atlas-04.jpg',
    bodyS: 'anatomy/atlas-05.jpg',
    head: 'anatomy/atlas-06.jpg',
    torso: 'anatomy/atlas-07.jpg',
    body3: 'anatomy/atlas-08.jpg',
    skull: 'anatomy/atlas-09.jpg',
    spine: 'anatomy/atlas-10.jpg',
    joint: 'anatomy/atlas-11.jpg',
    handB: 'anatomy/atlas-12.jpg',
    ribs: 'anatomy/atlas-13.jpg',
    pelvis: 'anatomy/atlas-14.jpg',
    legB: 'anatomy/atlas-15.jpg',
    footB: 'anatomy/atlas-16.jpg',
    skel: 'anatomy/atlas-17.jpg',
    musB: 'anatomy/atlas-18.jpg',
    mus3: 'anatomy/atlas-19.jpg',
    sarc: 'anatomy/atlas-20.jpg',
    biceps: 'anatomy/atlas-21.jpg',
    quad: 'anatomy/atlas-22.jpg',
    abs: 'anatomy/atlas-23.jpg',
    heartM: 'anatomy/atlas-24.jpg',
    circ: 'anatomy/atlas-25.jpg',
    heart: 'anatomy/atlas-26.jpg',
    heart3: 'anatomy/atlas-27.jpg',
    cap: 'anatomy/atlas-28.jpg',
    blood: 'anatomy/atlas-29.jpg',
    artery: 'anatomy/atlas-30.jpg',
    flow: 'anatomy/atlas-31.jpg',
    resp: 'anatomy/atlas-32.jpg',
    lungs: 'anatomy/atlas-33.jpg',
    alve: 'anatomy/atlas-34.jpg',
    trach: 'anatomy/atlas-35.jpg',
    dig: 'anatomy/atlas-36.jpg',
    org3: 'anatomy/atlas-37.jpg',
    liver: 'anatomy/atlas-38.jpg',
    stomach: 'anatomy/atlas-39.jpg',
    tooth: 'anatomy/atlas-40.jpg',
    panc: 'anatomy/atlas-41.jpg',
    brain: 'anatomy/atlas-42.jpg',
    nerv: 'anatomy/atlas-43.jpg',
    nerv3: 'anatomy/atlas-44.jpg',
    brain3: 'anatomy/atlas-45.jpg',
    neuron: 'anatomy/atlas-46.jpg',
    cord: 'anatomy/atlas-47.jpg',
    urin: 'anatomy/atlas-48.jpg',
    kidney: 'anatomy/atlas-49.jpg',
    neph: 'anatomy/atlas-50.jpg',
    endo: 'anatomy/atlas-51.jpg',
    thyr: 'anatomy/atlas-52.jpg',
    adr: 'anatomy/atlas-53.jpg',
    lymph: 'anatomy/atlas-54.jpg',
    wbc: 'anatomy/atlas-55.jpg',
    spleen: 'anatomy/atlas-56.jpg',
    skin: 'anatomy/atlas-57.jpg',
    embryo: 'anatomy/atlas-58.jpg',
    uterus: 'anatomy/atlas-59.jpg',
    testis: 'anatomy/atlas-60.jpg',
    senses: 'anatomy/atlas-61.jpg',
    eye: 'anatomy/atlas-62.jpg',
    ear: 'anatomy/atlas-63.jpg',
    tongue: 'anatomy/atlas-64.jpg',
    nose: 'anatomy/atlas-65.jpg',
    organs: 'anatomy/atlas-36.jpg',
    musF: 'anatomy/atlas-19.jpg',
    skull3: 'anatomy/atlas-09.jpg',
    eye3: 'anatomy/atlas-62.jpg',
    rbc: 'anatomy/rbc.jpg',
    plate: 'anatomy/plate.jpg',
    vein: 'anatomy/atlas-30.jpg',
    vessel: 'anatomy/atlas-25.jpg',
    blad: 'anatomy/atlas-48.jpg',
    gall: 'anatomy/atlas-38.jpg',
    intest: 'anatomy/atlas-36.jpg',
    armB: 'anatomy/atlas-12.jpg'
  };
  function fileFor(id) {
    if (window.ATLAS && window.ATLAS[id]) return window.ATLAS[id];
    var p = FILE[id] || ('anatomy/' + id + '.svg');
    var m = /atlas-(\d{2})\.jpg/.exec(p);
    if (m && window.ATLAS && window.ATLAS[m[1]]) return window.ATLAS[m[1]];
    return p;
  }
  function esc(s) {
    return String(s == null ? '' : s).replace(/[&<>"']/g, function (c) {
      return ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c];
    });
  }
  function metaOf(id) {
    for (var i = 0; i < TYPES.length; i++) if (TYPES[i].id === id) return TYPES[i];
    if (id === 'photo') return { id: id, name: 'تصویر', cap: '' };
    return { id: id, name: id, cap: '' };
  }
  function sanitizeSvg(txt) {
    txt = String(txt == null ? '' : txt);
    txt = txt.replace(/<\?xml[^>]*>/i, '');
    txt = txt.replace(/<!DOCTYPE[^>]*>/i, '');
    txt = txt.replace(/<script[\s\S]*?<\/script>/gi, '');
    txt = txt.replace(/<svg\b/i, '<svg class="an-svg"');
    return txt;
  }
  function faNum(n) {
    return String(n).replace(/[0-9]/g, function (d) { return '۰۱۲۳۴۵۶۷۸۹'[+d]; });
  }
  function clampPct(v) {
    v = +v;
    if (isNaN(v)) return 50;
    return Math.max(1, Math.min(99, v));
  }
  function normMark(m, i) {
    m = m || {};
    if (m.x1 != null && m.y1 != null) {
      return {
        x1: clampPct(m.x1), y1: clampPct(m.y1),
        x2: clampPct(m.x2 != null ? m.x2 : m.x1),
        y2: clampPct(m.y2 != null ? m.y2 : m.y1),
        n: m.n || (i + 1), lbl: m.lbl || ''
      };
    }
    var x = clampPct(m.x), y = clampPct(m.y);
    var D = { n: [0, -12], e: [12, 0], s: [0, 12], w: [-12, 0], ne: [9, -9], se: [9, 9], sw: [-9, 9], nw: [-9, -9] };
    var d = D[m.d] || D.w;
    return { x1: clampPct(x + d[0]), y1: clampPct(y + d[1]), x2: x, y2: y, n: m.n || (i + 1), lbl: m.lbl || '' };
  }
  function marksOf(spec) {
    var X = (spec && spec.X) || {};
    var raw = Array.isArray(X.marks) ? X.marks : [];
    return raw.map(normMark);
  }
  function nextMarkN(marks) {
    var used = {};
    marks.forEach(function (m) { used[m.n] = 1; });
    var n = 1;
    while (used[n]) n++;
    return n;
  }
  function wrapPlate(spec, mediaHtml, opts) {
    opts = opts || {};
    var id = (spec && spec.t) || 'bodyF';
    var X = (spec && spec.X) || {};
    var show = String(X.lab || '1') !== '0';
    var ttl = show ? String(X.title || metaOf(id).name || '') : '';
    var marks = marksOf(spec);
    var showNames = String(X.mkName || '0') === '1';
    var showBlank = String(X.blank || '1') !== '0' && marks.length;
    var cap = (!opts.noCap && ttl) ? '<div class="tbx-cap">' + esc(ttl) + '</div>' : '';
    var mid = 'anAh' + Math.floor(Math.random() * 1e9);
    var ov = '<svg class="an-ov" viewBox="0 0 100 100" preserveAspectRatio="none" aria-hidden="true">' +
      '<defs><marker id="' + mid + '" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="4.2" markerHeight="4.2" orient="auto">' +
      '<path d="M0,1 L10,5 L0,9 Z" fill="#be185d"/></marker></defs>';
    marks.forEach(function (m) {
      var dx = m.x2 - m.x1, dy = m.y2 - m.y1;
      var len = Math.sqrt(dx * dx + dy * dy) || 1;
      var cut = 3.4;
      var x2 = m.x2 - dx / len * cut;
      var y2 = m.y2 - dy / len * cut;
      ov += '<line x1="' + m.x1 + '" y1="' + m.y1 + '" x2="' + x2 + '" y2="' + y2 +
        '" stroke="#be185d" stroke-width="2" stroke-linecap="round" vector-effect="non-scaling-stroke" marker-end="url(#' + mid + ')"/>';
    });
    ov += '</svg>';
    marks.forEach(function (m, i) {
      var on = (typeof state !== 'undefined' && state._sel === i) ? ' on' : '';
      ov += '<span class="an-tail" data-i="' + i + '" data-end="tail" style="left:' + m.x1 + '%;top:' + m.y1 + '%"></span>';
      ov += '<span class="an-num' + on + '" data-i="' + i + '" data-end="tip" style="left:' + m.x2 + '%;top:' + m.y2 + '%">' +
        faNum(m.n) + '</span>';
    });
    var fields = '';
    if (showBlank && marks.length && opts.fields !== false) {
      fields = '<div class="an-af' + (opts.editable ? '' : ' an-af-paper') + '">';
      marks.forEach(function (m, i) {
        fields += '<div class="an-af-row' + (opts.editable && state._sel === i ? ' on' : '') + '">';
        fields += '<span class="an-af-n">' + faNum(m.n) + '</span>';
        if (opts.editable) {
          fields += '<input class="an-af-in" data-i="' + i + '" placeholder="نام این قسمت را بنویسید" value="' + esc(m.lbl || '') + '">';
          fields += '<button type="button" class="an-del" data-i="' + i + '">حذف</button>';
        } else {
          fields += '<span class="an-af-box"></span>';
        }
        fields += '</div>';
      });
      fields += '</div>';
    }
    return cap + '<div class="an-plate">' +
      '<div class="an-stage"><div class="an-frame">' + mediaHtml + ov + '</div></div>' +
      fields + '</div>';
  }
  function mountSvg(host, spec, done) {
    if (!host) return;
    var id = (spec && spec.t) || 'bodyF';
    /* V134 — t='photo': تصویرِ خودِ کاربر (data-URL در X.img) به‌جای فایلِ اطلس. */
    var userImg = spec && spec.X && typeof spec.X.img === 'string' && /^data:image\//.test(spec.X.img) ? spec.X.img : '';
    var url = userImg || fileFor(id);
    var alt = esc(((spec && spec.X && spec.X.title) || metaOf(id).name || ''));
    var media = '<img class="an-svg" alt="' + alt + '" src="' + url + '">';
    var isPrev = host.id === 'anPreview';
    host.innerHTML = wrapPlate(spec, media, { fields: !isPrev, editable: false, noCap: isPrev });
    if (!/\.svg(\?|$)/i.test(url)) {
      if (done) done(true);
      return;
    }
    fetch(url).then(function (r) {
      if (!r.ok) throw new Error('svg');
      return r.text();
    }).then(function (txt) {
      host.innerHTML = wrapPlate(spec, sanitizeSvg(txt), { fields: host.id !== 'anPreview', editable: host.id === 'anPreview', noCap: host.id === 'anPreview' });
      if (done) done(true);
    }).catch(function () {
      host.innerHTML = svgOf(spec || {});
      if (done) done(false);
    });
  }

  function svgOf(spec) {
    spec = spec || {};
    var t = spec.t || 'bodyF';
    var X = spec.X || {};
    var on = String(X.lab || '1') !== '0';
    var ttl = X.title || '';
    var h = title(ttl);

    if (t === 'bodyF') {
      h += person(180, 42, 1.15);
      h += L(on, 232, 48, 'سر') + L(on, 236, 110, 'تنه') + L(on, 70, 120, 'دست') + L(on, 70, 220, 'پا');
      return wrap(h);
    }
    if (t === 'bodyB') {
      h += person(180, 42, 1.15, '#e8b89a', '#7a4e3a');
      h += '<ellipse cx="180" cy="48" rx="10" ry="8" fill="#c48b6a"/>';
      h += L(on, 232, 48, 'پس‌سر') + L(on, 236, 118, 'پشت') + L(on, 232, 168, 'کمر');
      return wrap(h);
    }
    if (t === 'bodyS') {
      h += '<ellipse cx="168" cy="52" rx="18" ry="20" fill="#f4c7b0" stroke="#8a5a44"/>';
      h += '<path d="M160,72 L150,140 Q168,150 176,140 L184,72 Z" fill="#f4c7b0" stroke="#8a5a44"/>';
      h += '<path d="M176,86 L210,120" stroke="#8a5a44" stroke-width="6" stroke-linecap="round"/>';
      h += '<path d="M154,142 L148,230" stroke="#8a5a44" stroke-width="7" stroke-linecap="round"/>';
      h += L(on, 220, 56, 'نیمرخ') + L(on, 220, 130, 'سینه');
      return wrap(h);
    }
    if (t === 'body3') {
      h += '<ellipse cx="186" cy="50" rx="16" ry="18" fill="#f7d2bc"/><ellipse cx="176" cy="50" rx="16" ry="18" fill="#e8b89a" stroke="#8a5a44"/>';
      h += '<path d="M158,72 L154,145 Q176,156 198,145 L202,74 Q176,66 158,72 Z" fill="#e8b89a" stroke="#8a5a44"/>';
      h += '<path d="M166,74 L170,145 Q176,150 182,145 L186,74 Z" fill="#f4c7b0" opacity=".7"/>';
      h += '<path d="M156,88 L128,128" stroke="#8a5a44" stroke-width="6" stroke-linecap="round"/>';
      h += '<path d="M200,88 L232,124" stroke="#c48b6a" stroke-width="6" stroke-linecap="round"/>';
      h += '<path d="M162,148 L154,228" stroke="#8a5a44" stroke-width="7" stroke-linecap="round"/>';
      h += '<path d="M190,148 L204,226" stroke="#c48b6a" stroke-width="7" stroke-linecap="round"/>';
      h += L(on, 240, 50, 'نما ۳بعدی');
      return wrap(h);
    }
    if (t === 'head') {
      h += '<ellipse cx="180" cy="110" rx="54" ry="66" fill="#f4c7b0" stroke="#8a5a44" stroke-width="1.6"/>';
      h += '<ellipse cx="160" cy="100" rx="7" ry="5" fill="#2c3a50"/><ellipse cx="200" cy="100" rx="7" ry="5" fill="#2c3a50"/>';
      h += '<path d="M180,108 L176,124 L184,124 Z" fill="#d4a08a"/>';
      h += '<path d="M166,140 Q180,150 194,140" fill="none" stroke="#8a5a44"/>';
      h += '<path d="M160,176 Q180,200 200,176" fill="#f4c7b0" stroke="#8a5a44"/>';
      h += L(on, 248, 80, 'جمجمه') + L(on, 248, 110, 'چشم') + L(on, 248, 150, 'دهان') + L(on, 248, 190, 'گردن');
      return wrap(h);
    }
    if (t === 'torso') {
      h += '<path d="M120,40 L240,40 L252,200 Q180,230 108,200 Z" fill="#f4c7b0" stroke="#8a5a44"/>';
      h += '<ellipse cx="150" cy="100" rx="28" ry="36" fill="#f8a5a5" opacity=".7"/>';
      h += '<ellipse cx="210" cy="100" rx="28" ry="36" fill="#f8a5a5" opacity=".7"/>';
      h += '<ellipse cx="180" cy="168" rx="34" ry="22" fill="#e07a5f" opacity=".55"/>';
      h += L(on, 268, 100, 'ریه') + L(on, 268, 170, 'کبد/معده');
      return wrap(h);
    }
    if (t === 'skin') {
      h += '<path d="M80,60 H280 V200 H80 Z" fill="#f4c7b0" stroke="#8a5a44"/>';
      h += '<path d="M80,60 H280 V88 H80 Z" fill="#e8b89a"/>';
      h += '<path d="M80,88 H280 V130 H80 Z" fill="#d4a08a"/>';
      h += L(on, 90, 78, 'روپوست') + L(on, 90, 114, 'درم') + L(on, 90, 168, 'زیرپوست');
      return wrap(h);
    }
    if (t === 'skel') {
      h += '<ellipse cx="180" cy="40" rx="16" ry="18" fill="#f1efe6" stroke="#8a8070"/>';
      h += '<rect x="174" y="56" width="12" height="28" rx="3" fill="#efece3" stroke="#8a8070"/>';
      for (var i = 0; i < 6; i++) h += '<ellipse cx="180" cy="' + (92 + i * 10) + '" rx="' + (28 - i) + '" ry="4" fill="none" stroke="#8a8070"/>';
      h += '<path d="M152,92 L118,150" stroke="#cfc8b6" stroke-width="6"/><path d="M208,92 L242,150" stroke="#cfc8b6" stroke-width="6"/>';
      h += '<path d="M168,154 L154,240" stroke="#cfc8b6" stroke-width="7"/><path d="M192,154 L206,240" stroke="#cfc8b6" stroke-width="7"/>';
      h += '<ellipse cx="180" cy="156" rx="22" ry="10" fill="#efece3" stroke="#8a8070"/>';
      h += L(on, 230, 42, 'جمجمه') + L(on, 250, 110, 'دنده‌ها') + L(on, 250, 200, 'ران');
      return wrap(h);
    }
    if (t === 'skull' || t === 'skull3') {
      var sh = t === 'skull3';
      h += '<ellipse cx="' + (sh ? 186 : 180) + '" cy="120" rx="70" ry="78" fill="#f3f0e6"/>';
      h += '<ellipse cx="180" cy="120" rx="70" ry="78" fill="' + (sh ? '#e6e0d0' : '#f3f0e6') + '" stroke="#8a8070" stroke-width="1.6"/>';
      h += '<ellipse cx="154" cy="112" rx="14" ry="16" fill="#2c3a50"/><ellipse cx="206" cy="112" rx="14" ry="16" fill="#2c3a50"/>';
      h += '<path d="M180,126 L172,150 H188 Z" fill="#d9d2c2"/>';
      h += '<path d="M150,176 Q180,198 210,176" fill="none" stroke="#8a8070" stroke-width="2"/>';
      h += '<path d="M158,176 v10 M166,178 v10 M174,180 v10 M186,180 v10 M194,178 v10 M202,176 v10" stroke="#8a8070"/>';
      h += L(on, 260, 80, 'جمجمه') + L(on, 260, 176, 'فک');
      return wrap(h);
    }
    if (t === 'ribs') {
      h += '<rect x="174" y="36" width="12" height="200" rx="4" fill="#efece3" stroke="#8a8070"/>';
      for (var r = 0; r < 8; r++) {
        var yy = 56 + r * 20, rx = 70 - r * 3;
        h += '<path d="M180,' + yy + ' C' + (180 - rx) + ',' + (yy + 8) + ' ' + (180 - rx) + ',' + (yy + 16) + ' 180,' + (yy + 12) + '" fill="none" stroke="#8a8070" stroke-width="3"/>';
        h += '<path d="M180,' + yy + ' C' + (180 + rx) + ',' + (yy + 8) + ' ' + (180 + rx) + ',' + (yy + 16) + ' 180,' + (yy + 12) + '" fill="none" stroke="#8a8070" stroke-width="3"/>';
      }
      h += L(on, 24, 50, 'جناغ') + L(on, 24, 140, 'دنده');
      return wrap(h);
    }
    if (t === 'spine') {
      for (var s = 0; s < 14; s++) {
        h += '<rect x="164" y="' + (28 + s * 16) + '" width="32" height="12" rx="3" fill="#efece3" stroke="#8a8070"/>';
        if (s < 13) h += '<rect x="172" y="' + (40 + s * 16) + '" width="16" height="4" fill="#d4c9a8"/>';
      }
      h += L(on, 210, 50, 'مهره گردنی') + L(on, 210, 140, 'سینه‌ای') + L(on, 210, 220, 'کمری');
      return wrap(h);
    }
    if (t === 'armB') {
      h += '<rect x="168" y="36" width="24" height="70" rx="8" fill="#efece3" stroke="#8a8070"/>';
      h += '<circle cx="180" cy="112" r="10" fill="#e6e0d0" stroke="#8a8070"/>';
      h += '<rect x="156" y="120" width="18" height="78" rx="7" fill="#efece3" stroke="#8a8070"/>';
      h += '<rect x="186" y="120" width="16" height="74" rx="7" fill="#efece3" stroke="#8a8070"/>';
      h += '<ellipse cx="164" cy="208" rx="16" ry="10" fill="#efece3" stroke="#8a8070"/>';
      h += '<ellipse cx="196" cy="204" rx="14" ry="9" fill="#efece3" stroke="#8a8070"/>';
      h += L(on, 210, 70, 'بازو') + L(on, 220, 150, 'زند زبرین/زیرین') + L(on, 220, 210, 'مچ');
      return wrap(h);
    }
    if (t === 'legB') {
      h += '<rect x="166" y="28" width="28" height="90" rx="10" fill="#efece3" stroke="#8a8070"/>';
      h += '<circle cx="180" cy="126" r="12" fill="#e6e0d0" stroke="#8a8070"/>';
      h += '<rect x="158" y="136" width="18" height="88" rx="8" fill="#efece3" stroke="#8a8070"/>';
      h += '<rect x="184" y="136" width="16" height="82" rx="8" fill="#efece3" stroke="#8a8070"/>';
      h += '<path d="M150,226 H210 L200,248 H158 Z" fill="#efece3" stroke="#8a8070"/>';
      h += L(on, 214, 70, 'ران') + L(on, 220, 180, 'درشت‌نی') + L(on, 220, 240, 'کف پا');
      return wrap(h);
    }
    if (t === 'pelvis') {
      h += '<path d="M90,80 Q180,40 270,80 L250,170 Q180,210 110,170 Z" fill="#efece3" stroke="#8a8070" stroke-width="1.6"/>';
      h += '<ellipse cx="180" cy="150" rx="28" ry="22" fill="#f7f4ea" stroke="#8a8070"/>';
      h += L(on, 40, 90, 'ایلیوم') + L(on, 40, 170, 'عانه');
      return wrap(h);
    }
    if (t === 'handB') {
      h += '<rect x="150" y="70" width="60" height="50" rx="8" fill="#efece3" stroke="#8a8070"/>';
      [[130, 40], [158, 28], [186, 24], [214, 30], [236, 50]].forEach(function (p, i) {
        h += '<line x1="' + (160 + i * 10) + '" y1="80" x2="' + p[0] + '" y2="' + p[1] + '" stroke="#cfc8b6" stroke-width="7" stroke-linecap="round"/>';
      });
      h += L(on, 230, 90, 'کف') + L(on, 230, 40, 'انگشتان');
      return wrap(h);
    }
    if (t === 'footB') {
      h += '<path d="M80,150 Q180,90 300,150 L280,190 Q180,230 90,190 Z" fill="#efece3" stroke="#8a8070"/>';
      for (var f = 0; f < 5; f++) h += '<line x1="' + (120 + f * 28) + '" y1="150" x2="' + (110 + f * 32) + '" y2="100" stroke="#cfc8b6" stroke-width="6"/>';
      h += L(on, 40, 160, 'پاشنه') + L(on, 40, 110, 'انگشت');
      return wrap(h);
    }
    if (t === 'joint') {
      h += '<rect x="166" y="30" width="28" height="90" rx="8" fill="#efece3" stroke="#8a8070"/>';
      h += '<circle cx="180" cy="140" r="22" fill="#f3eee0" stroke="#8a8070" stroke-width="2"/>';
      h += '<rect x="166" y="158" width="28" height="90" rx="8" fill="#efece3" stroke="#8a8070"/>';
      h += '<path d="M150,140 Q180,120 210,140" fill="none" stroke="#c45c26" stroke-width="3"/>';
      h += L(on, 220, 80, 'استخوان') + L(on, 220, 144, 'غضروف') + L(on, 220, 200, 'استخوان');
      return wrap(h);
    }
    if (t === 'musF' || t === 'mus3') {
      h += person(180, 40, 1.1, '#d4574a', '#8a2a22');
      h += '<path d="M162,78 Q180,90 198,78" fill="#c23b32"/>';
      h += L(on, 236, 80, 'سینه‌ای') + L(on, 236, 130, 'مایل شکم') + L(on, 70, 200, 'چهارسر');
      return wrap(h);
    }
    if (t === 'musB') {
      h += person(180, 40, 1.1, '#c23b32', '#8a2a22');
      h += L(on, 236, 80, 'ذوزنقه‌ای') + L(on, 236, 140, 'پشتی بزرگ');
      return wrap(h);
    }
    if (t === 'biceps') {
      h += '<path d="M150,40 L170,40 L176,120 Q180,150 170,200 L150,200 Q140,150 150,40 Z" fill="#efece3" stroke="#8a8070"/>';
      h += '<ellipse cx="200" cy="120" rx="28" ry="40" fill="#d4574a" stroke="#8a2a22"/>';
      h += '<path d="M200,80 L200,40 M200,160 L200,210" stroke="#8a2a22" stroke-width="3"/>';
      h += L(on, 236, 120, 'دوسر') + L(on, 40, 80, 'بازو');
      return wrap(h);
    }
    if (t === 'quad') {
      h += '<rect x="150" y="36" width="28" height="200" rx="10" fill="#efece3" stroke="#8a8070"/>';
      h += '<ellipse cx="210" cy="130" rx="26" ry="56" fill="#d4574a" stroke="#8a2a22"/>';
      h += L(on, 244, 130, 'چهارسر ران');
      return wrap(h);
    }
    if (t === 'abs') {
      h += '<rect x="120" y="50" width="120" height="180" rx="16" fill="#f4c7b0" stroke="#8a5a44"/>';
      for (var a = 0; a < 3; a++) for (var b = 0; b < 2; b++)
        h += '<rect x="' + (140 + b * 46) + '" y="' + (70 + a * 48) + '" width="40" height="40" rx="6" fill="#d4574a" opacity=".8"/>';
      h += L(on, 250, 90, 'راست شکمی');
      return wrap(h);
    }
    if (t === 'heartM' || t === 'heart' || t === 'heart3') {
      var c1 = t === 'heart3' ? '#c81e1e' : '#e11d48';
      var c2 = '#9f1239';
      h += '<path d="M180,230 C80,160 70,90 120,70 C150,58 170,78 180,100 C190,78 210,58 240,70 C290,90 280,160 180,230 Z" fill="' + c1 + '" stroke="' + c2 + '" stroke-width="1.6"/>';
      if (t !== 'heartM') {
        h += '<path d="M150,90 C160,120 170,140 180,150 C190,140 200,120 210,90" fill="none" stroke="#fda4af" stroke-width="2"/>';
        h += '<path d="M168,64 C168,40 188,40 188,58" fill="none" stroke="#9f1239" stroke-width="6"/>';
        h += '<path d="M196,66 C210,40 230,50 220,80" fill="none" stroke="#be123c" stroke-width="5"/>';
      }
      h += L(on, 250, 90, 'دهلیز') + L(on, 250, 170, 'بطن') + L(on, 40, 70, 'آئورت');
      return wrap(h);
    }
    if (t === 'organs' || t === 'org3') {
      h += person(180, 36, 1.05, '#f8d7c4', '#c48b6a');
      h += '<ellipse cx="162" cy="92" rx="16" ry="22" fill="#fb7185" opacity=".9"/>';
      h += '<ellipse cx="198" cy="92" rx="16" ry="22" fill="#fb7185" opacity=".9"/>';
      h += '<ellipse cx="196" cy="128" rx="22" ry="14" fill="#84cc16"/>';
      h += '<ellipse cx="164" cy="136" rx="16" ry="12" fill="#f97316"/>';
      h += '<ellipse cx="180" cy="168" rx="10" ry="14" fill="#a78bfa"/>';
      h += L(on, 236, 92, 'ریه') + L(on, 236, 128, 'کبد') + L(on, 236, 150, 'معده') + L(on, 236, 176, 'کلیه');
      return wrap(h);
    }
    if (t === 'lungs') {
      h += '<path d="M120,50 Q80,80 90,200 Q130,230 170,190 Q160,90 120,50 Z" fill="#fb7185" stroke="#9f1239"/>';
      h += '<path d="M240,50 Q280,80 270,200 Q230,230 190,190 Q200,90 240,50 Z" fill="#fb7185" stroke="#9f1239"/>';
      h += '<rect x="174" y="36" width="12" height="50" rx="4" fill="#fda4af" stroke="#9f1239"/>';
      h += L(on, 40, 80, 'ریه چپ') + L(on, 280, 80, 'ریه راست') + L(on, 196, 40, 'نای');
      return wrap(h);
    }
    if (t === 'liver') {
      h += '<path d="M70,80 Q180,40 300,90 Q310,150 240,190 Q160,210 80,170 Z" fill="#65a30d" stroke="#3f6212" stroke-width="1.6"/>';
      h += '<ellipse cx="230" cy="150" rx="18" ry="12" fill="#84cc16"/>';
      h += L(on, 40, 70, 'لوب راست') + L(on, 250, 70, 'لوب چپ');
      return wrap(h);
    }
    if (t === 'stomach') {
      h += '<path d="M140,50 C90,70 80,160 140,200 C200,230 250,180 240,120 C230,70 190,40 140,50 Z" fill="#fb923c" stroke="#c2410c" stroke-width="1.6"/>';
      h += '<path d="M150,50 C160,30 200,28 210,50" fill="none" stroke="#c2410c" stroke-width="8"/>';
      h += '<path d="M150,198 C140,230 120,240 110,250" fill="none" stroke="#c2410c" stroke-width="8"/>';
      h += L(on, 250, 50, 'مری') + L(on, 250, 130, 'معده') + L(on, 40, 250, 'دوازدهه');
      return wrap(h);
    }
    if (t === 'kidney') {
      h += '<path d="M130,50 Q70,90 80,190 Q120,250 170,210 Q200,150 170,90 Q160,50 130,50 Z" fill="#c084fc" stroke="#7e22ce"/>';
      h += '<path d="M230,50 Q290,90 280,190 Q240,250 190,210 Q160,150 190,90 Q200,50 230,50 Z" fill="#c084fc" stroke="#7e22ce"/>';
      h += '<path d="M170,150 H190 M160,150 Q180,200 180,240" stroke="#7e22ce" stroke-width="3"/>';
      h += L(on, 40, 80, 'کلیه چپ') + L(on, 280, 80, 'کلیه راست') + L(on, 196, 240, 'حالب');
      return wrap(h);
    }
    if (t === 'brain' || t === 'brain3') {
      h += '<ellipse cx="180" cy="130" rx="90" ry="70" fill="#f9a8d4" stroke="#be185d" stroke-width="1.6"/>';
      h += '<path d="M180,62 V198" stroke="#be185d" stroke-width="1.4"/>';
      h += '<path d="M110,100 Q140,90 160,110 Q150,140 120,140 Q100,120 110,100" fill="none" stroke="#9d174d"/>';
      h += '<path d="M250,100 Q220,90 200,110 Q210,140 240,140 Q260,120 250,100" fill="none" stroke="#9d174d"/>';
      h += '<ellipse cx="180" cy="200" rx="24" ry="14" fill="#f472b6"/>';
      h += L(on, 40, 80, 'نیمکره') + L(on, 40, 210, 'مخچه');
      return wrap(h);
    }
    if (t === 'intest') {
      h += '<path d="M80,70 Q180,40 280,80 Q300,140 250,180 Q180,230 100,190 Q60,140 80,70" fill="none" stroke="#fb923c" stroke-width="18" stroke-linecap="round"/>';
      h += '<path d="M110,90 Q180,70 240,100 Q250,140 200,160 Q150,180 120,140 Q100,110 110,90" fill="none" stroke="#fdba74" stroke-width="10"/>';
      h += L(on, 40, 70, 'روده بزرگ') + L(on, 40, 160, 'روده باریک');
      return wrap(h);
    }
    if (t === 'spleen') {
      h += '<ellipse cx="180" cy="140" rx="70" ry="46" fill="#ef4444" stroke="#991b1b" transform="rotate(-20 180 140)"/>';
      h += L(on, 40, 80, 'طحال');
      return wrap(h);
    }
    if (t === 'panc') {
      h += '<path d="M70,140 Q120,90 180,130 Q250,170 310,120" fill="none" stroke="#f59e0b" stroke-width="22" stroke-linecap="round"/>';
      h += L(on, 40, 90, 'سر') + L(on, 170, 90, 'تنه') + L(on, 280, 90, 'دم');
      return wrap(h);
    }
    if (t === 'blad') {
      h += '<ellipse cx="180" cy="150" rx="50" ry="60" fill="#93c5fd" stroke="#1d4ed8"/>';
      h += '<path d="M180,210 V250" stroke="#1d4ed8" stroke-width="6"/>';
      h += L(on, 240, 140, 'مثانه') + L(on, 240, 240, 'میزراه');
      return wrap(h);
    }
    if (t === 'gall') {
      h += '<path d="M70,90 Q180,40 300,100 Q280,180 180,200 Q80,180 70,90 Z" fill="#65a30d" opacity=".35"/>';
      h += '<ellipse cx="220" cy="150" rx="22" ry="16" fill="#84cc16" stroke="#3f6212"/>';
      h += L(on, 40, 80, 'کبد') + L(on, 250, 150, 'صفرا');
      return wrap(h);
    }
    if (t === 'circ') {
      h += person(180, 40, 1.05, '#fee2e2', '#9f1239');
      h += '<path d="M180,90 V40 M180,90 C120,120 110,180 150,220 M180,90 C240,120 250,180 210,220" fill="none" stroke="#e11d48" stroke-width="3"/>';
      h += '<path d="M180,96 C130,130 128,190 160,226 M180,96 C230,130 232,190 200,226" fill="none" stroke="#2563eb" stroke-width="2.2"/>';
      h += L(on, 40, 80, 'سرخرگ') + L(on, 40, 120, 'سیاهرگ');
      return wrap(h);
    }
    if (t === 'artery') {
      h += '<path d="M40,140 C100,80 160,200 220,120 C280,50 320,180 340,140" fill="none" stroke="#e11d48" stroke-width="16" stroke-linecap="round"/>';
      h += '<path d="M40,140 C100,80 160,200 220,120 C280,50 320,180 340,140" fill="none" stroke="#fecdd3" stroke-width="6"/>';
      h += L(on, 40, 70, 'دیواره') + L(on, 40, 220, 'خون روشن');
      return wrap(h);
    }
    if (t === 'vein') {
      h += '<path d="M40,150 C90,210 150,80 210,160 C270,230 320,90 340,150" fill="none" stroke="#2563eb" stroke-width="16" stroke-linecap="round"/>';
      h += '<path d="M150,128 L168,150 L150,160" fill="none" stroke="#93c5fd" stroke-width="3"/>';
      h += L(on, 40, 70, 'سیاهرگ') + L(on, 40, 220, 'دریچه');
      return wrap(h);
    }
    if (t === 'cap') {
      h += '<path d="M40,140 H120" stroke="#e11d48" stroke-width="8"/>';
      h += '<path d="M240,140 H320" stroke="#2563eb" stroke-width="8"/>';
      for (var c = 0; c < 7; c++) {
        h += '<path d="M120,' + (80 + c * 18) + ' Q180,' + (70 + c * 20) + ' 240,' + (90 + c * 16) + '" fill="none" stroke="#f472b6" stroke-width="2"/>';
      }
      h += L(on, 40, 70, 'سرخرگچه') + L(on, 250, 70, 'سیاهرگچه') + L(on, 150, 250, 'مویرگ');
      return wrap(h);
    }
    if (t === 'vessel') {
      h += '<circle cx="180" cy="140" r="8" fill="#e11d48"/>';
      for (var k = 0; k < 10; k++) {
        var ang = k * 36 * Math.PI / 180;
        h += '<line x1="180" y1="140" x2="' + (180 + 90 * Math.cos(ang)) + '" y2="' + (140 + 90 * Math.sin(ang)) + '" stroke="#fb7185" stroke-width="3"/>';
        h += '<circle cx="' + (180 + 90 * Math.cos(ang)) + '" cy="' + (140 + 90 * Math.sin(ang)) + '" r="5" fill="#fda4af"/>';
      }
      h += L(on, 20, 30, 'شبکه رگ');
      return wrap(h);
    }
    if (t === 'flow') {
      h += '<path d="M180,230 C80,160 70,90 120,70 C150,58 170,78 180,100 C190,78 210,58 240,70 C290,90 280,160 180,230 Z" fill="#e11d48"/>';
      h += '<path d="M90,90 L150,110" stroke="#93c5fd" stroke-width="4" marker-end="url(#x)"/>';
      h += '<path d="M210,70 L250,40" stroke="#fecdd3" stroke-width="4"/>';
      h += L(on, 40, 80, 'ورود سیاهرگی') + L(on, 250, 40, 'خروج آئورت');
      return wrap(h);
    }
    if (t === 'resp') {
      h += '<rect x="174" y="30" width="12" height="46" rx="4" fill="#fda4af" stroke="#9f1239"/>';
      h += '<path d="M180,76 L140,110 L110,190 Q150,230 170,170 L180,110 Z" fill="#fb7185" stroke="#9f1239"/>';
      h += '<path d="M180,76 L220,110 L250,190 Q210,230 190,170 L180,110 Z" fill="#fb7185" stroke="#9f1239"/>';
      h += L(on, 200, 40, 'نای') + L(on, 40, 160, 'ریه') + L(on, 40, 110, 'نایژه');
      return wrap(h);
    }
    if (t === 'alve') {
      h += '<path d="M40,140 H160" stroke="#fda4af" stroke-width="10"/>';
      for (var al = 0; al < 6; al++) {
        h += '<circle cx="' + (200 + (al % 3) * 36) + '" cy="' + (110 + Math.floor(al / 3) * 50) + '" r="22" fill="#fecdd3" stroke="#e11d48"/>';
      }
      h += L(on, 40, 80, 'نایژک') + L(on, 200, 80, 'حبابچه');
      return wrap(h);
    }
    if (t === 'trach') {
      for (var tr = 0; tr < 8; tr++) h += '<rect x="166" y="' + (30 + tr * 16) + '" width="28" height="12" rx="3" fill="#fda4af" stroke="#9f1239"/>';
      h += '<path d="M180,158 L130,230 M180,158 L230,230" stroke="#fb7185" stroke-width="10" stroke-linecap="round"/>';
      h += L(on, 210, 70, 'نای') + L(on, 40, 220, 'نایژه');
      return wrap(h);
    }
    if (t === 'nerv') {
      h += person(180, 40, 1.05, '#faf5ff', '#6d28d9');
      h += '<circle cx="180" cy="48" r="12" fill="#c084fc"/>';
      h += '<path d="M180,60 V200" stroke="#7c3aed" stroke-width="4"/>';
      h += '<path d="M180,90 L130,130 M180,90 L230,130 M180,140 L140,190 M180,140 L220,190" stroke="#a78bfa" stroke-width="2"/>';
      h += L(on, 230, 48, 'مغز') + L(on, 230, 120, 'نخاع') + L(on, 40, 160, 'اعصاب');
      return wrap(h);
    }
    if (t === 'neuron') {
      h += '<circle cx="180" cy="140" r="28" fill="#ddd6fe" stroke="#6d28d9" stroke-width="2"/>';
      h += '<circle cx="180" cy="140" r="10" fill="#7c3aed"/>';
      for (var n = 0; n < 7; n++) {
        var an = n * 40 - 80;
        var rad = an * Math.PI / 180;
        h += '<line x1="180" y1="140" x2="' + (180 + 70 * Math.cos(rad)) + '" y2="' + (140 + 70 * Math.sin(rad)) + '" stroke="#8b5cf6" stroke-width="3"/>';
        h += '<circle cx="' + (180 + 70 * Math.cos(rad)) + '" cy="' + (140 + 70 * Math.sin(rad)) + '" r="6" fill="#c4b5fd"/>';
      }
      h += '<path d="M208,140 C280,140 300,80 330,90" fill="none" stroke="#6d28d9" stroke-width="4"/>';
      h += L(on, 20, 40, 'دندریت') + L(on, 20, 250, 'آسه') + L(on, 150, 90, 'جسم سلولی');
      return wrap(h);
    }
    if (t === 'cord') {
      h += '<rect x="160" y="24" width="40" height="230" rx="18" fill="#ddd6fe" stroke="#6d28d9"/>';
      h += '<ellipse cx="180" cy="140" rx="10" ry="16" fill="#7c3aed"/>';
      for (var cd = 0; cd < 8; cd++) {
        h += '<path d="M160,' + (40 + cd * 26) + ' H120 M200,' + (40 + cd * 26) + ' H240" stroke="#8b5cf6" stroke-width="3"/>';
      }
      h += L(on, 40, 40, 'ریشه عصبی') + L(on, 40, 140, 'ماده خاکستری');
      return wrap(h);
    }
    if (t === 'dig') {
      h += '<ellipse cx="180" cy="36" rx="16" ry="10" fill="#f4c7b0" stroke="#8a5a44"/>';
      h += '<path d="M180,46 V70" stroke="#c2410c" stroke-width="6"/>';
      h += '<ellipse cx="200" cy="110" rx="36" ry="28" fill="#fb923c" stroke="#c2410c"/>';
      h += '<path d="M180,136 C120,160 110,220 170,230 C240,240 260,180 210,160" fill="none" stroke="#fdba74" stroke-width="10"/>';
      h += '<ellipse cx="230" cy="150" rx="20" ry="14" fill="#65a30d"/>';
      h += L(on, 40, 40, 'دهان') + L(on, 40, 110, 'معده') + L(on, 40, 180, 'روده') + L(on, 260, 150, 'کبد');
      return wrap(h);
    }
    if (t === 'tooth') {
      h += '<path d="M130,50 H230 V130 Q180,230 130,130 Z" fill="#f8fafc" stroke="#94a3b8" stroke-width="1.6"/>';
      h += '<path d="M130,50 H230 V90 H130 Z" fill="#e2e8f0"/>';
      h += L(on, 240, 70, 'مینا') + L(on, 240, 120, 'عاج') + L(on, 240, 180, 'ریشه');
      return wrap(h);
    }
    if (t === 'urin') {
      h += '<path d="M110,50 Q70,90 80,150 Q110,190 150,160 Q170,110 140,70 Z" fill="#c084fc" stroke="#7e22ce"/>';
      h += '<path d="M250,50 Q290,90 280,150 Q250,190 210,160 Q190,110 220,70 Z" fill="#c084fc" stroke="#7e22ce"/>';
      h += '<path d="M140,150 Q180,210 180,230 M220,150 Q180,210 180,230" stroke="#7e22ce" stroke-width="3"/>';
      h += '<ellipse cx="180" cy="242" rx="22" ry="16" fill="#93c5fd" stroke="#1d4ed8"/>';
      h += L(on, 40, 70, 'کلیه') + L(on, 40, 180, 'حالب') + L(on, 210, 250, 'مثانه');
      return wrap(h);
    }
    if (t === 'neph') {
      h += '<circle cx="120" cy="90" r="34" fill="#e9d5ff" stroke="#7e22ce" stroke-width="2"/>';
      h += '<circle cx="120" cy="90" r="16" fill="#c084fc"/>';
      h += '<path d="M150,100 C200,80 210,140 170,150 C230,160 240,220 180,230 C140,236 130,200 160,190" fill="none" stroke="#a855f7" stroke-width="5"/>';
      h += L(on, 40, 50, 'کپسول بومن') + L(on, 200, 80, 'لوله پیچیده') + L(on, 200, 230, 'جمع‌کننده');
      return wrap(h);
    }
    if (t === 'endo') {
      h += person(180, 40, 1.05, '#fff7ed', '#c2410c');
      h += '<circle cx="180" cy="46" r="6" fill="#f97316"/>';
      h += '<circle cx="180" cy="70" r="5" fill="#fb923c"/>';
      h += '<circle cx="180" cy="118" r="7" fill="#ea580c"/>';
      h += '<circle cx="158" cy="150" r="6" fill="#f97316"/><circle cx="202" cy="150" r="6" fill="#f97316"/>';
      h += L(on, 230, 48, 'هیپوفیز') + L(on, 230, 74, 'تیروئید') + L(on, 230, 120, 'لوزالمعده') + L(on, 230, 156, 'فوق کلیه');
      return wrap(h);
    }
    if (t === 'thyr') {
      h += '<path d="M120,90 Q180,130 240,90 Q250,150 180,170 Q110,150 120,90 Z" fill="#fdba74" stroke="#c2410c"/>';
      h += '<rect x="172" y="40" width="16" height="60" rx="4" fill="#fda4af"/>';
      h += L(on, 40, 80, 'لوب') + L(on, 200, 40, 'نای');
      return wrap(h);
    }
    if (t === 'adr') {
      h += '<path d="M130,80 Q80,130 100,210 Q150,250 180,200 Q200,140 170,90 Z" fill="#c084fc" stroke="#7e22ce"/>';
      h += '<ellipse cx="150" cy="90" rx="28" ry="16" fill="#f97316" stroke="#c2410c"/>';
      h += L(on, 220, 90, 'فوق کلیه') + L(on, 220, 180, 'کلیه');
      return wrap(h);
    }
    if (t === 'eye' || t === 'eye3') {
      h += '<ellipse cx="180" cy="140" rx="110" ry="70" fill="#f8fafc" stroke="#64748b" stroke-width="2"/>';
      h += '<circle cx="180" cy="140" r="40" fill="#38bdf8" stroke="#0369a1"/>';
      h += '<circle cx="180" cy="140" r="18" fill="#0f172a"/>';
      h += '<circle cx="172" cy="132" r="5" fill="#fff"/>';
      h += '<path d="M70,140 C70,80 290,80 290,140" fill="none" stroke="#334155" stroke-width="3"/>';
      h += L(on, 20, 80, 'صلبیه') + L(on, 20, 160, 'عنبیه') + L(on, 20, 200, 'مردمک');
      return wrap(h);
    }
    if (t === 'ear') {
      h += '<path d="M120,40 Q220,20 230,100 Q240,180 160,230 Q90,200 100,120 Q90,70 120,40 Z" fill="#f4c7b0" stroke="#8a5a44"/>';
      h += '<path d="M150,90 Q190,80 200,120 Q190,160 150,150 Q130,130 150,90 Z" fill="#e8b89a"/>';
      h += '<path d="M200,130 C260,130 280,160 270,200" fill="none" stroke="#94a3b8" stroke-width="3"/>';
      h += '<ellipse cx="270" cy="210" rx="16" ry="10" fill="#fdba74"/>';
      h += L(on, 40, 80, 'لاله') + L(on, 240, 120, 'مجرا') + L(on, 240, 220, 'حلزون');
      return wrap(h);
    }
    if (t === 'tongue') {
      h += '<ellipse cx="180" cy="150" rx="80" ry="70" fill="#fb7185" stroke="#9f1239"/>';
      for (var tg = 0; tg < 12; tg++) h += '<circle cx="' + (130 + (tg % 5) * 26) + '" cy="' + (120 + Math.floor(tg / 5) * 28) + '" r="4" fill="#be123c"/>';
      h += L(on, 40, 80, 'جوانه چشایی');
      return wrap(h);
    }
    if (t === 'nose') {
      h += '<path d="M180,40 Q140,80 130,160 Q180,210 230,160 Q220,80 180,40 Z" fill="#f4c7b0" stroke="#8a5a44"/>';
      h += '<ellipse cx="162" cy="170" rx="10" ry="8" fill="#e8b89a"/><ellipse cx="198" cy="170" rx="10" ry="8" fill="#e8b89a"/>';
      h += L(on, 240, 90, 'پل') + L(on, 240, 176, 'سوراخ بینی');
      return wrap(h);
    }
    if (t === 'cell') {
      h += '<ellipse cx="180" cy="140" rx="100" ry="80" fill="#bbf7d0" stroke="#15803d" stroke-width="3"/>';
      h += '<ellipse cx="180" cy="140" rx="88" ry="68" fill="#dcfce7"/>';
      h += '<circle cx="180" cy="140" r="28" fill="#86efac" stroke="#166534" stroke-width="2"/>';
      h += '<circle cx="180" cy="140" r="10" fill="#166534"/>';
      h += '<circle cx="120" cy="110" r="8" fill="#4ade80"/><circle cx="230" cy="160" r="6" fill="#4ade80"/>';
      h += L(on, 20, 50, 'غشا') + L(on, 20, 140, 'هسته') + L(on, 20, 200, 'سیتوپلاسم');
      return wrap(h);
    }
    if (t === 'rbc') {
      h += '<ellipse cx="180" cy="140" rx="70" ry="40" fill="#ef4444" stroke="#991b1b"/>';
      h += '<ellipse cx="180" cy="140" rx="30" ry="16" fill="#fecaca"/>';
      h += L(on, 40, 80, 'گلبول قرمز');
      return wrap(h);
    }
    if (t === 'wbc') {
      h += '<circle cx="180" cy="140" r="60" fill="#f8fafc" stroke="#64748b" stroke-width="2"/>';
      h += '<circle cx="168" cy="130" r="16" fill="#94a3b8"/><circle cx="196" cy="150" r="12" fill="#94a3b8"/>';
      h += L(on, 40, 80, 'گلبول سفید');
      return wrap(h);
    }
    if (t === 'plate') {
      h += '<ellipse cx="140" cy="140" rx="28" ry="16" fill="#fde68a" stroke="#b45309"/>';
      h += '<ellipse cx="200" cy="150" rx="22" ry="12" fill="#fcd34d" stroke="#b45309"/>';
      h += '<ellipse cx="230" cy="120" rx="18" ry="10" fill="#fbbf24" stroke="#b45309"/>';
      h += L(on, 40, 80, 'پلاکت');
      return wrap(h);
    }
    if (t === 'lymph') {
      h += person(180, 40, 1.05, '#ecfdf5', '#047857');
      h += '<circle cx="150" cy="70" r="6" fill="#34d399"/><circle cx="210" cy="70" r="6" fill="#34d399"/>';
      h += '<circle cx="160" cy="130" r="7" fill="#10b981"/><circle cx="200" cy="160" r="6" fill="#10b981"/>';
      h += '<ellipse cx="210" cy="150" rx="12" ry="18" fill="#6ee7b7"/>';
      h += L(on, 230, 70, 'غده لنفاوی') + L(on, 230, 150, 'طحال');
      return wrap(h);
    }
    if (t === 'uterus') {
      h += '<path d="M180,70 Q230,80 240,140 Q220,210 180,230 Q140,210 120,140 Q130,80 180,70 Z" fill="#fda4af" stroke="#be123c"/>';
      h += '<ellipse cx="118" cy="90" rx="18" ry="14" fill="#fb7185"/><ellipse cx="242" cy="90" rx="18" ry="14" fill="#fb7185"/>';
      h += '<path d="M136,90 C150,100 160,90 170,100 M224,90 C210,100 200,90 190,100" fill="none" stroke="#be123c" stroke-width="3"/>';
      h += L(on, 40, 90, 'تخمدان') + L(on, 40, 160, 'رحم');
      return wrap(h);
    }
    if (t === 'testis') {
      h += '<ellipse cx="160" cy="150" rx="28" ry="36" fill="#fdba74" stroke="#c2410c"/>';
      h += '<ellipse cx="210" cy="150" rx="28" ry="36" fill="#fdba74" stroke="#c2410c"/>';
      h += '<path d="M160,116 C160,70 210,70 210,116" fill="none" stroke="#c2410c" stroke-width="3"/>';
      h += L(on, 40, 90, 'طناب') + L(on, 40, 160, 'بیضه');
      return wrap(h);
    }
    return wrap(h + person(180, 50, 1));
  }

  function iconOf(id) {
    return '<img class="an-thumb" alt="" src="' + fileFor(id) + '">';
  }


  function pctOnImg(e, box) {
    var r = box.getBoundingClientRect();
    var cs = window.getComputedStyle(box);
    var bl = parseFloat(cs.borderLeftWidth) || 0;
    var bt = parseFloat(cs.borderTopWidth) || 0;
    var br = parseFloat(cs.borderRightWidth) || 0;
    var bb = parseFloat(cs.borderBottomWidth) || 0;
    var w = r.width - bl - br;
    var h = r.height - bt - bb;
    if (w < 2 || h < 2) return null;
    return {
      x: clampPct((e.clientX - r.left - bl) / w * 100),
      y: clampPct((e.clientY - r.top - bt) / h * 100)
    };
  }
  function setDraftLine(frame, a, b) {
    var svg = frame.querySelector('svg.an-draft');
    if (!svg) {
      svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
      svg.setAttribute('class', 'an-draft');
      svg.setAttribute('viewBox', '0 0 100 100');
      svg.setAttribute('preserveAspectRatio', 'none');
      var ln = document.createElementNS('http://www.w3.org/2000/svg', 'line');
      ln.setAttribute('stroke', '#be185d');
      ln.setAttribute('stroke-width', '2');
      ln.setAttribute('stroke-linecap', 'round');
      ln.setAttribute('vector-effect', 'non-scaling-stroke');
      svg.appendChild(ln);
      frame.appendChild(svg);
    }
    var ln = svg.firstChild;
    ln.setAttribute('x1', a.x); ln.setAttribute('y1', a.y);
    ln.setAttribute('x2', b.x); ln.setAttribute('y2', b.y);
    svg.style.display = '';
  }
  function bindPreviewMarks(box) {
    var frame = box.querySelector('.an-frame');
    var img = box.querySelector('img.an-svg, svg.an-svg');
    if (!frame || !img) return;
    frame.classList.add('is-edit');
    var drawing = null, drag = null, moved = false;
    function onMove(ev) {
      moved = true;
      if (drag) {
        var p = pctOnImg(ev, frame);
        if (!p) return;
        var el = frame.querySelector((drag.end === 'tip' ? '.an-num' : '.an-tail') + '[data-i="' + drag.i + '"]');
        if (el) { el.style.left = p.x + '%'; el.style.top = p.y + '%'; }
        var m = state.X.marks[drag.i];
        if (m) {
          if (drag.end === 'tip') { m.x2 = p.x; m.y2 = p.y; }
          else { m.x1 = p.x; m.y1 = p.y; }
        }
        return;
      }
      if (drawing) {
        var p = pctOnImg(ev, frame);
        if (p) { drawing.b = p; setDraftLine(frame, drawing.a, drawing.b); }
      }
    }
    function onUp(ev) {
      document.removeEventListener('mousemove', onMove);
      document.removeEventListener('mouseup', onUp);
      if (drag) {
        var p = pctOnImg(ev, frame);
        if (p && state.X.marks[drag.i]) {
          if (drag.end === 'tip') { state.X.marks[drag.i].x2 = p.x; state.X.marks[drag.i].y2 = p.y; }
          else { state.X.marks[drag.i].x1 = p.x; state.X.marks[drag.i].y1 = p.y; }
        }
        drag = null;
        paint();
        renderAnswerFields();
        return;
      }
      if (!drawing) return;
      var start = drawing.a;
      var p = pctOnImg(ev, frame) || drawing.b || start;
      var dx = p.x - start.x, dy = p.y - start.y;
      var ok = Math.sqrt(dx * dx + dy * dy) >= 4;
      drawing = null;
      var draft = frame.querySelector('svg.an-draft');
      if (draft) draft.remove();
      if (!ok) return;
      if (!state.X) state.X = {};
      if (!Array.isArray(state.X.marks)) state.X.marks = [];
      if (state.X.marks.length >= 12) return;
      state.X.marks.push({ x1: start.x, y1: start.y, x2: p.x, y2: p.y, n: nextMarkN(state.X.marks), lbl: '' });
      state._sel = state.X.marks.length - 1;
      paint();
      renderAnswerFields();
    }
    frame.addEventListener('mousedown', function (e) {
      if (e.button !== 0) return;
      var handle = e.target.closest('.an-num, .an-tail');
      var p = pctOnImg(e, frame);
      if (handle) {
        e.preventDefault();
        e.stopPropagation();
        moved = false;
        drag = { i: +handle.getAttribute('data-i'), end: handle.getAttribute('data-end') };
        state._sel = drag.i;
        document.addEventListener('mousemove', onMove);
        document.addEventListener('mouseup', onUp);
        return;
      }
      if (!p) return;
      e.preventDefault();
      moved = false;
      drawing = { a: p, b: p };
      setDraftLine(frame, p, p);
      document.addEventListener('mousemove', onMove);
      document.addEventListener('mouseup', onUp);
    });
  }
  function renderAnswerFields() {
    var host = $('anAnswerFields');
    if (!host) return;
    var marks = marksOf(state);
    var show = String((state.X && state.X.blank) || '1') !== '0';
    if (!marks.length || !show) {
      host.innerHTML = '<p class="muted" style="font-size:11px;margin:6px 0 0">هنوز فلشی نیست. روی تصویر کلیک کنید و بکشید.</p>';
      return;
    }
    host.innerHTML = marks.map(function (m, i) {
      return '<div class="an-af-row' + (state._sel === i ? ' on' : '') + '">' +
        '<span class="an-af-n">' + faNum(m.n) + '</span>' +
        '<input class="an-af-in" data-i="' + i + '" placeholder="نام این قسمت را بنویسید" value="' + esc(m.lbl || '') + '">' +
        '<button type="button" class="an-del" data-i="' + i + '">حذف</button></div>';
    }).join('');
    host.querySelectorAll('.an-af-in').forEach(function (inp) {
      inp.addEventListener('input', function () {
        var i = +inp.getAttribute('data-i');
        if (state.X.marks[i]) state.X.marks[i].lbl = inp.value;
      });
      inp.addEventListener('focus', function () {
        state._sel = +inp.getAttribute('data-i');
        var box = $('anPreview');
        if (box) box.querySelectorAll('.an-num').forEach(function (n) {
          n.classList.toggle('on', +n.getAttribute('data-i') === state._sel);
        });
        host.querySelectorAll('.an-af-row').forEach(function (row) {
          var el = row.querySelector('.an-af-in');
          row.classList.toggle('on', el && +el.getAttribute('data-i') === state._sel);
        });
      });
    });
    host.querySelectorAll('.an-del').forEach(function (b) {
      b.onclick = function () {
        var i = +b.getAttribute('data-i');
        state.X.marks.splice(i, 1);
        state._sel = null;
        paint();
      };
    });
  }
    function renderMarkList() { renderAnswerFields(); }

  var state = { k: 'a', t: 'bodyF', X: { title: '', lab: '1', marks: [], blank: '1', mkName: '0' } };
  var replaceEl = null;
  var cat = 'all';
  function $(id) { return document.getElementById(id); }

  function fitPreviewImage() {
    var pane = document.querySelector('#anPreview');
    var img = document.querySelector('#anPreview img.an-svg');
    var frame = document.querySelector('#anPreview .an-frame');
    if (!pane || !img) return;
    function apply() {
      if (!img.isConnected) return;
      var r = pane.getBoundingClientRect();
      var availW = Math.max(80, Math.floor(r.width - 24));
      var availH = Math.max(80, Math.floor(r.height - 24));
      if (r.height < 80) { requestAnimationFrame(apply); return; }
      var nw = img.naturalWidth || 0;
      var nh = img.naturalHeight || 0;
      if (!nw || !nh) { requestAnimationFrame(apply); return; }
      var scale = Math.min(availW / nw, availH / nh);
      if (!isFinite(scale) || scale <= 0) scale = 1;
      var dw = Math.max(1, Math.floor(nw * scale));
      var dh = Math.max(1, Math.floor(nh * scale));
      img.style.width = dw + 'px';
      img.style.height = dh + 'px';
      img.style.maxWidth = dw + 'px';
      img.style.maxHeight = dh + 'px';
      img.style.objectFit = 'contain';
      if (frame) {
        frame.style.width = dw + 'px';
        frame.style.height = dh + 'px';
        frame.style.maxWidth = availW + 'px';
        frame.style.maxHeight = availH + 'px';
        frame.style.overflow = 'hidden';
      }
    }
    if (!img.complete || !img.naturalWidth) {
      img.addEventListener('load', apply, { once: true });
    }
    apply();
  }
  function paint() {
    var box = $('anPreview');
    if (box) { mountSvg(box, state); bindPreviewMarks(box); renderAnswerFields(); requestAnimationFrame(function () { fitPreviewImage(); requestAnimationFrame(fitPreviewImage); }); }
    var tit = $('anTitle');
    if (tit && document.activeElement !== tit) tit.value = (state.X && state.X.title) || '';
    var lab = $('anLab');
    if (lab) lab.checked = String(state.X.lab || '1') !== '0';
    var blank = $('anBlank');
    if (blank) blank.checked = String((state.X && state.X.blank) || '1') !== '0';
    var mkN = $('anMkName');
    if (mkN) mkN.checked = String((state.X && state.X.mkName) || '0') === '1';
  }
  function renderList() {
    var host = $('anShapes');
    if (!host) return;
    host.innerHTML = '';
    TYPES.filter(function (s) { return cat === 'all' || s.cat === cat; }).forEach(function (s) {
      var b = document.createElement('button');
      b.type = 'button'; b.className = 'gf-shape' + (s.id === state.t ? ' on' : '');
      b.setAttribute('data-t', s.id);
      b.title = s.cap || s.name;
      b.innerHTML = iconOf(s.id) + '<span>' + s.name + '</span>';
      b.addEventListener('click', function () { setType(s.id); });
      host.appendChild(b);
    });
  }
  function setType(id) {
    var prev = state.t;
    state.t = id; state.k = 'a';
    if (!state.X) state.X = { title: '', lab: '1' };
    var meta = metaOf(id);
    var prevMeta = metaOf(prev);
    var cur = state.X.title || '';
    if (!replaceEl || !cur || cur === prevMeta.name || cur === prevMeta.cap) {
      state.X.title = meta.name;
    }
    if (prev && prev !== id) {
      state.X.marks = [];
      state._sel = null;
      renderMarkList();
    }
    document.querySelectorAll('#anShapes .gf-shape').forEach(function (b) {
      b.classList.toggle('on', b.getAttribute('data-t') === id);
    });
    document.querySelectorAll('#anCats .an-cat').forEach(function (b) {
      b.classList.toggle('on', b.getAttribute('data-c') === cat);
    });
    paint();
    try {
      var on = document.querySelector('#anShapes .gf-shape.on');
      if (on && on.scrollIntoView) on.scrollIntoView({ block: 'nearest' });
    } catch (e) {}
  }
  function tokenOf(spec) { return '%%FIG:' + JSON.stringify(spec) + '%%'; }
  function targetTextarea() {
    var t = null;
    try { if (window.__qmfActiveField && document.body.contains(window.__qmfActiveField)) t = window.__qmfActiveField; } catch (e) {}
    try { if (!t && typeof activeMathTextarea !== 'undefined' && activeMathTextarea && document.body.contains(activeMathTextarea)) t = activeMathTextarea; } catch (e) {}
    try { if (!t && typeof extractedActiveTextarea !== 'undefined' && extractedActiveTextarea && document.body.contains(extractedActiveTextarea)) t = extractedActiveTextarea; } catch (e) {}
    return t || document.getElementById('qTxt_main') || document.querySelector('.screen[data-view="builder"] textarea');
  }
  function insertToken(token) {
    var t = targetTextarea();
    if (!t) return;
    try { if (window.QMF) QMF.upgrade(t); } catch (e) {}
    var start = t.selectionStart != null ? t.selectionStart : (t.value || '').length;
    var end = t.selectionEnd != null ? t.selectionEnd : start;
    var v = t.value || '';
    var left = v.slice(0, start);
    var next = (left.length && !/[\n]$/.test(left) ? '\n' : '') + token + '\n';
    t.value = left + next + v.slice(end);
    t.dispatchEvent(new Event('input', { bubbles: true }));
    try { if (window.__examBuilderUpdateQuestionText) window.__examBuilderUpdateQuestionText(t); } catch(e) {}
    if (typeof qMathSync === 'function') qMathSync(t.id);
  }
  function apply() {
    var tit = $('anTitle');
    if (!state.X) state.X = {};
    if (tit) state.X.title = tit.value;
    var lab = $('anLab');
    state.X.lab = lab && !lab.checked ? '0' : '1';
    var blank = $('anBlank');
    state.X.blank = blank && !blank.checked ? '0' : '1';
    var mkN = $('anMkName');
    state.X.mkName = mkN && mkN.checked ? '1' : '0';
    if (!Array.isArray(state.X.marks)) state.X.marks = [];
    state.k = 'a';
    if (replaceEl && replaceEl.classList && replaceEl.classList.contains('qmf-fig')) {
      replaceEl.dataset.fig = JSON.stringify(state);
      replaceEl.setAttribute('data-fig', JSON.stringify(state));
      mountSvg(replaceEl, state);
      var t = targetTextarea();
      try { if (t && window.QMF) QMF.syncFromSurface(t); } catch (e) {}
      try { if (t && window.__examBuilderUpdateQuestionText) window.__examBuilderUpdateQuestionText(t); } catch (e) {}
      try { if (typeof renderPreview === 'function') renderPreview(); } catch (e) {}
    } else insertToken(tokenOf(state));
    close();
  }
  function ensureModal() {
    if ($('anOverlay')) return;
    var ov = document.createElement('div');
    ov.id = 'anOverlay';
    ov.className = 'gf-overlay';
    ov.innerHTML =
      '<div class="gf-modal an-modal" role="dialog" aria-label="درج آناتومی">' +
        '<div class="gf-head"><h3>آناتومی بدن انسان</h3><button type="button" class="gf-x" id="anClose">×</button></div>' +
        '<div class="gf-split an-split">' +
          '<aside class="gf-types">' +
            '<div class="gf-types-h">نوع شکل</div>' +
            '<div class="an-cats" id="anCats"></div>' +
            '<div class="gf-shapes" id="anShapes"></div>' +
          '</aside>' +
          '<section class="an-mid">' +
            '<div class="gf-preview" id="anPreview"></div>' +
          '</section>' +
          '<aside class="an-side">' +
            '<div class="an-side-h">شماره‌گذاری</div>' +
            '<div id="anAnswerFields" class="an-af"></div>' +
            '<div class="gf-fields">' +
              '<label class="gf-span">عنوان<input id="anTitle" placeholder="عنوان تصویر"></label>' +
              '<label class="gf-span" style="display:flex;align-items:center;gap:8px;flex-direction:row">' +
                '<input id="anLab" type="checkbox" checked style="width:auto">نمایش عنوان زیر تصویر' +
              '</label>' +
              '<div class="an-tools">' +
                '<div class="an-tools-h">علامت‌گذاری با فلش</div>' +
                '<p class="muted" style="font-size:11px;margin:0 0 6px">روی تصویر کلیک کنید و بکشید؛ هر جا رها کنید نوک پیکان شماره‌گذاری می‌شود. فیلد نام همان شماره زیر کادر تصویر می‌آید.</p>' +
                '<div class="an-tools-row">' +
                  '<label><input id="anBlank" type="checkbox" checked> نمایش فیلد نام زیر تصویر</label>' +
                  '<button type="button" class="gf-btn ghost" id="anClearMarks">پاک‌کردن فلش‌ها</button>' +
                '</div>' +
              '</div>' +
              '<p class="muted" style="font-size:11px;margin:0">۶۵ تصویر و عنوان اطلس بدن انسان + گلبول قرمز و پلاکت.</p>' +
            '</div>' +
          '</aside>' +
        '</div>' +
        '<div class="gf-foot"><button type="button" class="gf-btn ghost" id="anCancel">انصراف</button>' +
        '<button type="button" class="gf-btn ok" id="anApply">درج در سؤال</button></div>' +
      '</div>';
    document.body.appendChild(ov);
    CATS.forEach(function (c) {
      var b = document.createElement('button');
      b.type = 'button'; b.className = 'an-cat' + (c.id === 'all' ? ' on' : '');
      b.setAttribute('data-c', c.id); b.textContent = c.name;
      b.addEventListener('click', function () { cat = c.id; renderList(); setType(state.t); });
      $('anCats').appendChild(b);
    });
    renderList();
    ov.addEventListener('click', function (e) { if (e.target === ov) close(); });
    $('anClose').onclick = close;
    $('anCancel').onclick = close;
    $('anApply').onclick = apply;
    $('anTitle').addEventListener('input', function () {
      if (!state.X) state.X = {};
      state.X.title = this.value;
      paint();
    });
    $('anLab').addEventListener('change', function () {
      if (!state.X) state.X = {};
      state.X.lab = this.checked ? '1' : '0';
      paint();
    });
    $('anBlank').addEventListener('change', function () {
      if (!state.X) state.X = {};
      state.X.blank = this.checked ? '1' : '0';
      paint();
    });
    $('anClearMarks').addEventListener('click', function () {
      if (!state.X) state.X = {};
      state.X.marks = [];
      state._sel = null;
      paint();
      renderMarkList();
    });
    if (!window._anFitBound) {
      window._anFitBound = true;
      window.addEventListener('resize', function () { fitPreviewImage(); });
    }
    renderMarkList();
  }
  function open(spec, el) {
    replaceEl = el || null;
    try { state = spec ? JSON.parse(JSON.stringify(spec)) : { k: 'a', t: 'bodyF', X: { title: metaOf('bodyF').name, lab: '1' } }; }
    catch (e) { state = { k: 'a', t: 'bodyF', X: { title: 'بدن روبه‌رو', lab: '1' } }; }
    state.k = 'a';
    if (!state.t) state.t = 'bodyF';
    if (!state.X) state.X = { title: metaOf(state.t).name, lab: '1', marks: [], blank: '1', mkName: '0' };
    if (!Array.isArray(state.X.marks)) state.X.marks = [];
    if (!replaceEl && !state.X.title) state.X.title = metaOf(state.t).name;
    ensureModal();
    $('anOverlay').classList.add('open');
    var ok = $('anApply');
    if (ok) ok.textContent = replaceEl ? 'اعمال تغییرات' : 'درج در سؤال';
    renderList();
    setType(state.t);
    renderMarkList();
  }
  function openFromEl(fig) {
    if (!fig) return false;
    var spec = {};
    try { spec = JSON.parse(fig.getAttribute('data-fig') || fig.dataset.fig || '{}'); }
    catch (e) { spec = { k: 'a', t: 'bodyF', X: { lab: '1' } }; }
    open(spec, fig);
    return true;
  }
  function close() {
    var ov = $('anOverlay');
    if (ov) ov.classList.remove('open');
    replaceEl = null;
  }
  function make(raw) {
    var el = document.createElement('span');
    el.className = 'qmf-fig';
    el.contentEditable = 'false';
    el.setAttribute('data-fig', raw);
    el.dataset.fig = raw;
    el.setAttribute('role', 'img');
    el.setAttribute('aria-label', 'آناتومی');
    el.title = 'برای ویرایش دوبار کلیک کنید';
    try {
      var spec = JSON.parse(raw);
      mountSvg(el, spec);
    } catch (e) { el.textContent = '[آناتومی]'; }
    return el;
  }
  function bind() {
    var btn = $('openAnatomyEditor');
    if (btn && !btn._anBound) {
      btn._anBound = true;
      btn.addEventListener('click', function (e) { e.preventDefault(); open(null, null); });
    }
  }

  window.AnatomyFig = { svg: svgOf, make: make, open: open, openFromEl: openFromEl, close: close };

  if (window.GeoFig) {
    var oldMake = window.GeoFig.make;
    var oldOpen = window.GeoFig.openFromEl;
    window.GeoFig.make = function (raw) {
      try {
        var s = JSON.parse(raw);
        if (s && s.k === 'a') return make(raw);
      } catch (e) {}
      return oldMake ? oldMake(raw) : make(raw);
    };
    window.GeoFig.openFromEl = function (fig) {
      try {
        var s = JSON.parse(fig.getAttribute('data-fig') || fig.dataset.fig || '{}');
        if (s && s.k === 'a') return openFromEl(fig);
      } catch (e) {}
      return oldOpen ? oldOpen(fig) : openFromEl(fig);
    };
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', bind);
  else bind();
})();
