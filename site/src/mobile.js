/* V148 — پوستهٔ موبایل پنل معلم، آینهٔ برنامهٔ اندروید (Design69 / Neumorphic):
   داک پایینی (منو · کیف پول · ⊕ · آزمون‌ها · کارت‌ها)، منوی کاشی‌ای ۲ستونه با همان ۸ کارت اپ،
   فهرست آزمون‌ها به‌صورت کارت‌های بازشونده با همان ردیف عملیات، «کارت‌ها» = کارت‌های مدیریتی اپ،
   ⊕ = افزودن سریع (آزمون جدید / دانش‌آموز جدید / کلاس جدید). فقط عرض ≤ 860px و نقش معلم؛
   دسکتاپ/تبلت افقی دست‌نخورده. صفحه‌های داخلی همان صفحه‌های سایت‌اند که داخل پوسته باز می‌شوند. */
(function () {
  'use strict';
  var S = window.ExamSite; if (!S) return;
  var el = S.el, fa = S.fa, esc = S.esc, toast = S.toast, api = S.api, view = S.view;
  var MQ = window.matchMedia('(max-width: 860px)');
  var ui = {menuOpen: false, addOpen: false, expanded: null};

  /* V149 — معلم و دانش‌آموز (مدیر در مرحلهٔ بعد) */
  /* V150 — هر سه نقش */
  function active() { var u = S.user(); return MQ.matches && u && (u.role === 'teacher' || u.role === 'student' || u.role === 'manager'); }
  function isStudent() { return S.user() && S.user().role === 'student'; }
  function isManager() { return S.user() && S.user().role === 'manager'; }
  /* نگاشت داک اپ → پنل‌های سایت */
  function dockSection() {
    if (ui.menuOpen) return 'menu';
    var p = view.panel;
    if (p === 'wallet') return 'wallet';
    if (isManager()) { if (p === 'teachers') return 'exams'; if (p === 'cards' || p === 'school' || p === 'dashboard') return 'cards'; return 'none'; }
    if (p === 'exams' || p === 'dashboard' || p === 'builder') return 'exams';
    if (p === 'cards' || p === 'reports' || p === 'bank' || p === 'grading') return 'cards';
    return 'none';
  }
  function go(panel, arg) { ui.menuOpen = false; ui.addOpen = false; S.go(panel, arg); }

  /* ---------- آیکون‌های خطی (شبیه Design69Icons) ---------- */
  var I = {
    menu: '<svg viewBox="0 0 24 24"><path d="M4 7h16M4 12h16M4 17h16"/></svg>',
    close: '<svg viewBox="0 0 24 24"><path d="M6 6l12 12M18 6L6 18"/></svg>',
    wallet: '<svg viewBox="0 0 24 24"><rect x="3" y="6" width="18" height="13" rx="3"/><path d="M3 10h18M16 14h2"/></svg>',
    exams: '<svg viewBox="0 0 24 24"><rect x="5" y="3" width="14" height="18" rx="2"/><path d="M9 8h6M9 12h6M9 16h4"/></svg>',
    cards: '<svg viewBox="0 0 24 24"><rect x="3" y="4" width="8" height="7" rx="2"/><rect x="13" y="4" width="8" height="7" rx="2"/><rect x="3" y="13" width="8" height="7" rx="2"/><rect x="13" y="13" width="8" height="7" rx="2"/></svg>',
    plus: '<svg viewBox="0 0 24 24"><path d="M12 5v14M5 12h14"/></svg>',
    calendar: '<svg viewBox="0 0 24 24"><rect x="3" y="5" width="18" height="16" rx="3"/><path d="M3 10h18M8 3v4M16 3v4"/></svg>',
    print: '<svg viewBox="0 0 24 24"><path d="M7 8V4h10v4M5 8h14a2 2 0 0 1 2 2v6h-4v4H7v-4H3v-6a2 2 0 0 1 2-2z"/></svg>',
    students: '<svg viewBox="0 0 24 24"><circle cx="9" cy="8" r="3.2"/><path d="M3 20c0-3.3 2.7-6 6-6s6 2.7 6 6"/><circle cx="17" cy="9" r="2.4"/><path d="M15.5 14.5c2.8.2 5.5 2.3 5.5 5.5"/></svg>',
    classes: '<svg viewBox="0 0 24 24"><path d="M3 10l9-5 9 5-9 5-9-5z"/><path d="M7 12v5c0 1.5 2.5 3 5 3s5-1.5 5-3v-5"/></svg>',
    account: '<svg viewBox="0 0 24 24"><circle cx="12" cy="8" r="4"/><path d="M4 21c0-4 3.6-7 8-7s8 3 8 7"/></svg>',
    site: '<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="9"/><path d="M3 12h18M12 3c3 3.5 3 14.5 0 18M12 3c-3 3.5-3 14.5 0 18"/></svg>',
    settings: '<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z"/></svg>',
    logout: '<svg viewBox="0 0 24 24"><path d="M10 4H6a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h4M15 8l4 4-4 4M19 12H9"/></svg>',
    reports: '<svg viewBox="0 0 24 24"><path d="M4 20V10M10 20V4M16 20v-7M22 20H2"/></svg>',
    grading: '<svg viewBox="0 0 24 24"><path d="M5 12l4 4L19 6"/></svg>',
    edit: '<svg viewBox="0 0 24 24"><path d="M4 20h4l10-10-4-4L4 16v4zM13 7l4 4"/></svg>',
    lock: '<svg viewBox="0 0 24 24"><rect x="5" y="11" width="14" height="10" rx="2"/><path d="M8 11V7a4 4 0 0 1 8 0v4"/></svg>',
    unlock: '<svg viewBox="0 0 24 24"><rect x="5" y="11" width="14" height="10" rx="2"/><path d="M8 11V7a4 4 0 0 1 7.5-2"/></svg>',
    copy: '<svg viewBox="0 0 24 24"><rect x="9" y="9" width="11" height="11" rx="2"/><path d="M5 15V5a1 1 0 0 1 1-1h10"/></svg>',
    share: '<svg viewBox="0 0 24 24"><path d="M12 15V4M8 8l4-4 4 4M5 13v6a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-6"/></svg>',
    trash: '<svg viewBox="0 0 24 24"><path d="M4 7h16M9 7V4h6v3M6 7l1 13h10l1-13M10 11v6M14 11v6"/></svg>',
    chevron: '<svg viewBox="0 0 24 24"><path d="M15 6l-6 6 6 6"/></svg>',
    dl: '<svg viewBox="0 0 24 24"><path d="M12 4v11M8 11l4 4 4-4M5 20h14"/></svg>'
  };
  function ic(name, cls) { return el('span', {class: 'mi ' + (cls || ''), html: I[name] || ''}); }

  /* ---------- داک پایین (TeacherBottomDock) ---------- */
  function dock() {
    var sec = dockSection();
    function item(label, icon, on, key) {
      return el('button', {class: 'm-dock-item' + (sec === key ? ' on' : ''), onclick: on, 'aria-label': label}, [ic(icon), el('span', {text: label})]);
    }
    return el('div', {class: 'm-dock', id: 'm-dock'}, [el('div', {class: 'm-dock-panel'}, [
      item('منو', ui.menuOpen ? 'close' : 'menu', function () { ui.menuOpen = !ui.menuOpen; ui.addOpen = false; paint(); }, 'menu'),
      item('کیف پول', 'wallet', function () { go('wallet'); }, 'wallet'),
      el('button', {class: 'm-dock-add' + (ui.addOpen ? ' on' : ''), 'aria-label': 'افزودن سریع', onclick: function () { ui.addOpen = !ui.addOpen; ui.menuOpen = false; paint(); }}, [ic(ui.addOpen ? 'close' : 'plus')]),
      isManager() ? item('معلم‌ها', 'students', function () { go('teachers'); }, 'exams') : item('آزمون‌ها', 'exams', function () { go('exams'); }, 'exams'),
      item('کارت‌ها', 'cards', function () { go('cards'); }, 'cards')
    ])]);
  }

  /* ---------- افزودن سریع (Design69QuickAddOverlay) ---------- */
  function quickAdd() {
    var mgr = isManager();
    var items = [
      mgr ? ['دعوت معلم', 'ساخت کد دعوت برای معلم', 'students', function () { go('teachers'); }] : ['آزمون جدید', 'ساخت آزمون آنلاین', 'exams', function () { go('builder', null); }],
      ['دانش‌آموز جدید', mgr ? 'در کلاس یکی از معلم‌ها' : 'افزودن به کلاس', 'students', async function () { if (mgr) return go('school'); ui.addOpen = false; paint(); if (window.SiteSchool) { var classes = await api.classes().catch(function () { return []; }); window.SiteSchool.studentForm(null, classes, null, function () { go('students'); }); } else go('students'); }],
      ['کلاس جدید', mgr ? 'برای یکی از معلم‌ها' : 'ساخت کلاس', 'classes', function () { mgr ? go('school') : go('classes', {create: true}); }],
      /* V61.5 — عمل چهارم: مدرسه جدید (مدیر می‌سازد؛ معلم با کد دعوت عضو می‌شود) */
      ['مدرسه جدید', mgr ? 'ساخت مدرسه' : 'عضویت با کد دعوت', 'classes', function () { mgr ? go('school') : go('profile'); }]
    ];
    return el('div', {class: 'm-sheet-bg', onclick: function (e) { if (e.target === e.currentTarget) { ui.addOpen = false; paint(); } }}, [el('div', {class: 'm-quick'}, items.map(function (it, i) {
      return el('button', {class: 'm-quick-item', style: 'animation-delay:' + (i * 40) + 'ms', onclick: it[3]}, [ic(it[2], 'm-quick-ic'), el('div', {}, [el('b', {text: it[0]}), el('span', {text: it[1]})])]);
    }))]);
  }

  /* ---------- منوی کاشی‌ای (Design69MainMenuScreen) — همان ۸ کارت معلم به همان ترتیب ---------- */
  function menuScreen() {
    var u = S.user();
    var cards = [
      ['تقویم', 'رویدادها و پیام‌ها', 'calendar', function () { go('calendar'); }],
      ['چاپ آزمون', 'اطلاعات رسمی چاپ آزمون', 'print', function () { go('builder', {mode: 'print'}); }],
      ['دانش‌آموزان', 'فهرست و وضعیت', 'students', function () { go('students'); }],
      ['کلاس‌ها', 'فهرست و مدیریت', 'classes', function () { go('classes'); }],
      ['حساب', 'مشخصات و امنیت حساب', 'account', function () { go('profile'); }],
      ['سایت', 'onlineexam.ir', 'site', function () { ui.menuOpen = false; toast('شما هم‌اکنون در سایت هستید.', 'ok'); paint(); }],
      ['تنظیمات', 'ظاهر، داده و درباره', 'settings', function () { go('tools'); }],
      ['خروج', 'خروج امن و تعویض حساب', 'logout', async function () { if (await S.confirmDlg('خروج از حساب', 'از حساب خارج می‌شوید؟', 'خروج', true)) { ui.menuOpen = false; S.logout(); } }, true]
    ];
    var sel = {calendar: 'calendar', profile: 'account', students: 'students', classes: 'classes', tools: 'settings'}[view.panel];
    return el('div', {class: 'm-menu'}, [
      el('button', {class: 'm-profile neo', onclick: function () { go('profile'); }}, [
        el('div', {class: 'm-avatar'}, [u.avatarUrl ? el('img', {src: u.avatarUrl, alt: ''}) : el('span', {text: (u.name || '?').trim().charAt(0)})]),
        el('div', {class: 'm-profile-t'}, [el('div', {class: 'k', text: 'پروفایل معلم'}), el('div', {class: 'n', text: u.name || 'حساب کاربری من'}), el('div', {class: 'e', text: u.email || 'حساب معلم'})]),
        ic('chevron', 'm-chev')
      ]),
      el('div', {class: 'm-grid'}, cards.map(function (c, i) {
        return el('button', {class: 'm-tile neo' + (sel === c[2] ? ' sel' : '') + (c[4] ? ' danger' : ''), style: 'animation-delay:' + (20 + i * 18) + 'ms', onclick: c[3]}, [ic(c[2], 'm-tile-ic'), el('b', {text: c[0]}), el('span', {text: c[1]})]);
      }))
    ]);
  }

  /* ---------- آزمون‌ها (TeacherDashboardScreen) ---------- */
  async function examsScreen(c) {
    S.loading(c);
    var list;
    try { list = await api.exams(); } catch (e) { S.showErr(c, e); return; }
    c.innerHTML = '';
    var wrap = el('div', {class: 'm-exams'});
    /* V113 — راست: آزمون‌های چاپی؛ وسط: + ؛ چپ: واردکردن */
    wrap.appendChild(el('div', {class: 'm-exams-top'}, [
      el('button', {class: 'm-outline', text: 'آزمون‌های چاپی', onclick: function () { printExamsSheet(); }}),
      el('button', {class: 'm-fab-sm', 'aria-label': 'ساخت آزمون جدید', onclick: function () { go('builder', null); }}, [ic('plus')]),
      el('button', {class: 'm-outline', text: 'واردکردن', onclick: function () { if (window.SiteExtras) window.SiteExtras.importExam(); }})
    ]));
    if (!list.length) { wrap.appendChild(el('p', {class: 'm-note', text: 'هنوز آزمونی برای نمایش وجود ندارد.'})); c.appendChild(wrap); return; }
    list.forEach(function (x) {
      var open = ui.expanded === x.id;
      var card = el('div', {class: 'm-exam neo' + (open ? ' open' : ''), onclick: function (e) { if (e.target.closest('.m-exam-acts')) return; ui.expanded = open ? null : x.id; examsScreen(c); }}, [
        el('div', {class: 'm-exam-h'}, [el('b', {text: x.title || 'بدون عنوان'}), el('span', {class: 'm-exam-state', text: x.is_open ? 'باز' : 'بسته'})]),
        el('div', {class: 'm-exam-m', text: (x.subject || 'بدون درس') + ' · ' + (x.code || '—') + ' · ' + fa(x.duration || 0) + ' دقیقه · بارم ' + fa(S.fmtScore(x.total_score))})
      ]);
      if (open) {
        /* V132 — همهٔ عملیات کارت به‌صورت آیکن در یک سطر: ویرایش، بازکردن/بستن، تکثیر، صادرکردن، حذف */
        card.appendChild(el('div', {class: 'm-exam-acts'}, [
          act('edit', 'ویرایش', function () { go('builder', {examId: x.id}); }),
          act(x.is_open ? 'lock' : 'unlock', x.is_open ? 'بستن' : 'بازکردن', async function () { try { await api.setExamOpen(x.id, !x.is_open); toast(x.is_open ? 'آزمون بسته شد.' : 'آزمون باز شد.', 'ok'); examsScreen(c); } catch (e) { toast(S.errMsg(e), 'err'); } }),
          act('copy', 'تکثیر', async function () { if (!(await S.confirmDlg('تکثیر آزمون', 'کپی آزمون «' + esc(x.title) + '» مثل یک آزمون جدید است و هزینهٔ همهٔ سؤال‌های آن با نرخ فعلی از کیف پول کسر می‌شود.', 'تأیید و تکثیر'))) return; try { var r = await api.duplicateExam(x.id); toast('کپی شد؛ کد جدید: ' + (r.code || ''), 'ok'); examsScreen(c); } catch (e) { toast(S.errMsg(e), 'err'); } }),
          act('share', 'صادرکردن', function () { if (window.SiteExtras) window.SiteExtras.exportExamDlg(x); }),
          act('print', 'چاپ', function () { S.printExam(x); }),
          act('trash', 'حذف', async function () { if (!(await S.confirmDlg('حذف آزمون', 'آزمون «' + esc(x.title) + '» و پاسخ‌ها، تلاش‌ها و مخاطبان وابسته حذف شوند؟ این کار برگشت‌پذیر نیست.', 'حذف کامل', true))) return; try { await api.deleteExam(x.id); toast('حذف شد.', 'ok'); examsScreen(c); } catch (e) { toast(S.errMsg(e), 'err'); } }, true)
        ]));
      }
      wrap.appendChild(card);
    });
    c.appendChild(wrap);
  }
  function act(icon, label, on, danger) { return el('button', {class: 'm-act' + (danger ? ' danger' : ''), title: label, 'aria-label': label, onclick: on}, [ic(icon)]); }
  function printExamsSheet() {
    var list = []; try { list = JSON.parse(localStorage.getItem('examsite.printexams.v1') || '[]') || []; } catch (e) {}
    var bg = el('div', {class: 'm-sheet-bg', onclick: function (e) { if (e.target === e.currentTarget) bg.remove(); }});
    var body = el('div', {class: 'm-sheet'}, [el('h3', {text: 'آزمون‌های چاپی'})]);
    if (!list.length) body.appendChild(el('p', {class: 'm-note', text: 'هنوز آزمون چاپی‌ای ذخیره نشده است. از بخش «چاپ آزمون» بسازید.'}));
    else {
      body.appendChild(el('p', {class: 'm-note', text: 'با انتخاب هر آزمون، ویرایشگر آن باز می‌شود.'}));
      list.forEach(function (r) { body.appendChild(el('button', {class: 'm-row neo', onclick: function () { bg.remove(); go('builder', {mode: 'print', printId: r.id}); }}, [ic('print', 'm-row-ic'), el('div', {}, [el('b', {text: r.title || 'آزمون چاپی'}), el('span', {text: (r.subject || 'بدون درس') + ' · ' + fa((r.questions || []).length) + ' سؤال'})])])); });
    }
    body.appendChild(el('button', {class: 'm-outline', style: 'margin-top:12px;width:100%', text: 'بستن', onclick: function () { bg.remove(); }}));
    bg.appendChild(body); document.body.appendChild(bg);
  }

  /* ---------- کارت‌ها (TeacherManagementCardsScreen) ---------- */
  function cardsScreen(c) {
    c.innerHTML = '';
    var cards = [
      ['آمار', 'نمودارها، میانگین‌ها و تحلیل کیفیت سؤال‌های آزمون را نشان می‌دهد.', 'reports', 'linear-gradient(135deg,#6C63F5,#27C4A8)', function () { go('reports', {section: 'stats'}); }],
      ['کارنامه', 'کارنامه و لیست نمرات کلاس؛ انتخاب آزمون‌ها و خروجی Excel یا PDF.', 'reports', 'linear-gradient(135deg,#0EA5E9,#6366F1)', function () { go('reports', {section: 'grades'}); }],
      ['بانک سؤال', 'جست‌وجو، دسته‌بندی، مشاهده، ویرایش، حذف و افزودن سؤال به آزمون.', 'exams', 'linear-gradient(135deg,#2878DB,#24B8C8)', function () { go('bank'); }],
      ['تصحیح', 'همه پاسخ‌ها، حضور، بازخورد و ثبت یا اصلاح نمره را باز می‌کند.', 'grading', 'linear-gradient(135deg,#25BFA4,#45D7BD)', function () { go('grading'); }],
      ['مانده', 'فقط پاسخ‌های در انتظار تصحیح و پیگیری را نمایش می‌دهد.', 'cards', 'linear-gradient(135deg,#F59E0B,#F97316)', function () { go('grading', {filter: 'pending'}); }],
      ['پاسخ', 'فقط پاسخ‌های تصحیح‌شده دارای نمره و بازخورد نهایی را نمایش می‌دهد.', 'grading', 'linear-gradient(135deg,#10B981,#34D399)', function () { go('grading', {filter: 'graded'}); }],
      ['درخواست‌ها', 'درخواست‌های ویرایش یا حذف مدیر را مشاهده، تأیید یا رد کنید.', 'account', 'linear-gradient(135deg,#8B5CF6,#EC4899)', function () { go('dashboard', {requests: true}); }]
    ];
    c.appendChild(el('div', {class: 'm-cards'}, cards.map(function (k, i) {
      return el('button', {class: 'm-card neo', style: 'animation-delay:' + (i * 30) + 'ms', onclick: k[4]}, [el('span', {class: 'm-card-ic', style: 'background:' + k[3], html: I[k[2]]}), el('div', {}, [el('b', {text: k[0]}), el('span', {text: k[1]})])]);
    })));
  }

  /* ---------- دانش‌آموز (StudentHomeScreen + منوی ۶کارتی؛ در اپ داک ندارد، نوار بالا با ☰) ---------- */
  function studentMenu() {
    var u = S.user();
    var cards = [
      ['آزمون', 'ورود با کد آزمون', 'exams', function () { go('join'); }],
      ['نتایج من', 'پاسخ‌ها و کارنامه', 'reports', function () { go('grades'); }],
      ['تقویم', 'رویدادها و پیام‌ها', 'calendar', function () { go('calendar'); }],
      ['حساب', 'مشخصات و امنیت حساب', 'account', function () { go('profile'); }],
      ['تنظیمات', 'ظاهر، داده و درباره', 'settings', function () { go('tools'); }],
      ['خروج', 'خروج امن و تعویض حساب', 'logout', async function () { if (await S.confirmDlg('خروج از حساب', 'از حساب خارج می‌شوید؟', 'خروج', true)) { ui.menuOpen = false; S.logout(); } }, true]
    ];
    var sel = {join: 'exams', grades: 'reports', calendar: 'calendar', profile: 'account', tools: 'settings'}[view.panel];
    return el('div', {class: 'm-menu'}, [
      el('button', {class: 'm-profile neo', onclick: function () { go('profile'); }}, [
        el('div', {class: 'm-avatar'}, [u.avatarUrl ? el('img', {src: u.avatarUrl, alt: ''}) : el('span', {text: (u.name || '?').trim().charAt(0)})]),
        el('div', {class: 'm-profile-t'}, [el('div', {class: 'k', text: 'پروفایل دانش‌آموز'}), el('div', {class: 'n', text: u.name || 'حساب کاربری من'}), el('div', {class: 'e', text: 'حساب دانش‌آموز'})]),
        ic('chevron', 'm-chev')
      ]),
      el('div', {class: 'm-grid'}, cards.map(function (c, i) {
        return el('button', {class: 'm-tile neo' + (sel === c[2] ? ' sel' : '') + (c[4] ? ' danger' : ''), style: 'animation-delay:' + (20 + i * 18) + 'ms', onclick: c[3]}, [ic(c[2], 'm-tile-ic'), el('b', {text: c[0]}), el('span', {text: c[1]})]);
      }))
    ]);
  }
  async function studentHome(c) {
    c.innerHTML = '';
    var wrap = el('div', {class: 'm-student'});
    wrap.appendChild(el('h2', {class: 'm-title', text: 'داشبورد دانش‌آموز'}));
    /* پیام‌های خوانده‌نشدهٔ تقویم — همان RPC اپ (cal_unseen_v59 / cal_mark_seen_v59) */
    var notesBox = el('div');
    wrap.appendChild(notesBox);
    S.rpcObj('cal_unseen_v59', {}).then(function (u) {
      var notes = (u && u.notes) || []; if (!notes.length) return;
      var panel = el('button', {class: 'm-note-panel neo', onclick: function () { openNote(notes[0]); }}, [el('b', {text: 'پیام جدید دارید'}), el('span', {text: notes.length === 1 ? notes[0].title : fa(notes.length) + ' پیام خوانده‌نشده — برای مشاهده لمس کنید'})]);
      notesBox.appendChild(panel);
      function openNote(n) {
        var bg = el('div', {class: 'm-sheet-bg', onclick: function (e) { if (e.target === e.currentTarget) bg.remove(); }});
        bg.appendChild(el('div', {class: 'm-sheet'}, [el('h3', {text: n.title || 'پیام'}), n.body ? el('p', {text: n.body}) : null, el('p', {class: 'm-note', style: 'text-align:right;padding:4px 0', text: 'تاریخ: ' + (n.date || '')}),
          el('button', {class: 'btn', style: 'width:100%', text: 'خواندم', onclick: function () { bg.remove(); S.rpcObj('cal_mark_seen_v59', {p_note: n.id}).catch(function () {}); notes = notes.filter(function (x) { return x.id !== n.id; }); if (!notes.length) panel.remove(); else panel.querySelector('span').textContent = notes.length === 1 ? notes[0].title : fa(notes.length) + ' پیام خوانده‌نشده — برای مشاهده لمس کنید'; }})]));
        document.body.appendChild(bg);
      }
    }).catch(function () {});
    /* کارت «کد آزمون را وارد کنید» — خودِ صفحهٔ شرکت در آزمون سایت داخل پنل نئومورفیک */
    var joinBox = el('div', {class: 'm-join neo'});
    wrap.appendChild(joinBox);
    c.appendChild(wrap);
    if (window.SiteStudent) await window.SiteStudent.page(joinBox, view.arg);
  }

  /* ---------- مدیر/معاون: منوی ۶کارتی + کارت ویژهٔ «داشبورد» (ExamApp.kt:988-1019,1059) ---------- */
  function managerMenu() {
    var u = S.user();
    var cards = [
      ['کلاس‌ها', 'فهرست و مدیریت', 'classes', function () { go('school'); }],
      ['دانش‌آموزان', 'فهرست و مدیریت', 'students', function () { go('school', {students: true}); }],
      ['حساب', 'مشخصات و امنیت حساب', 'account', function () { go('profile'); }],
      ['سایت', 'onlineexam.ir', 'site', function () { ui.menuOpen = false; toast('شما هم‌اکنون در سایت هستید.', 'ok'); paint(); }],
      ['تنظیمات', 'ظاهر، داده و درباره', 'settings', function () { go('tools'); }],
      ['خروج', 'خروج امن و تعویض حساب', 'logout', async function () { if (await S.confirmDlg('خروج از حساب', 'از حساب خارج می‌شوید؟', 'خروج', true)) { ui.menuOpen = false; S.logout(); } }, true]
    ];
    var sel = {school: 'classes', profile: 'account', tools: 'settings', dashboard: 'dashboard'}[view.panel];
    return el('div', {class: 'm-menu'}, [
      el('button', {class: 'm-profile neo', onclick: function () { go('profile'); }}, [
        el('div', {class: 'm-avatar'}, [u.avatarUrl ? el('img', {src: u.avatarUrl, alt: ''}) : el('span', {text: (u.name || '?').trim().charAt(0)})]),
        el('div', {class: 'm-profile-t'}, [el('div', {class: 'k', text: 'پروفایل مدیر/معاون'}), el('div', {class: 'n', text: u.name || 'حساب کاربری من'}), el('div', {class: 'e', text: u.email || 'حساب مدیر/معاون'})]),
        ic('chevron', 'm-chev')
      ]),
      /* کارت ویژه، وسط‌چین با عرض ۵۲٪ (featuredCard) */
      el('div', {class: 'm-featured'}, [el('button', {class: 'm-tile neo' + (sel === 'dashboard' ? ' sel' : ''), onclick: function () { go('dashboard'); }}, [ic('cards', 'm-tile-ic'), el('b', {text: 'داشبورد'}), el('span', {text: 'اطلاعات مدرسه و آمار'})])]),
      el('div', {class: 'm-grid'}, cards.map(function (c, i) {
        return el('button', {class: 'm-tile neo' + (sel === c[2] ? ' sel' : '') + (c[4] ? ' danger' : ''), style: 'animation-delay:' + (20 + i * 18) + 'ms', onclick: c[3]}, [ic(c[2], 'm-tile-ic'), el('b', {text: c[0]}), el('span', {text: c[1]})]);
      }))
    ]);
  }
  function managerCards(c) {
    c.innerHTML = '';
    var cards = [
      ['مدارس', 'لیست مدرسه‌ها، ساخت مدرسه جدید و کلاس‌های هر مدرسه را باز می‌کند.', 'classes', 'linear-gradient(135deg,#6C63F5,#27C4A8)', function () { go('school'); }],
      ['کارنامه', 'آمار پاسخ‌ها، میانگین نمره و فعالیت معلم‌های مدرسه.', 'reports', 'linear-gradient(135deg,#2878DB,#24B8C8)', function () { go('dashboard'); }],
      ['وضعیت', 'داشبورد مدرسه با اطلاعات، آمار کلی و پنل سریع بخش‌ها.', 'cards', 'linear-gradient(135deg,#25BFA4,#45D7BD)', function () { go('dashboard'); }]
    ];
    c.appendChild(el('div', {class: 'm-cards'}, cards.map(function (k, i) {
      return el('button', {class: 'm-card neo', style: 'animation-delay:' + (i * 30) + 'ms', onclick: k[4]}, [el('span', {class: 'm-card-ic', style: 'background:' + k[3], html: I[k[2]]}), el('div', {}, [el('b', {text: k[0]}), el('span', {text: k[1]})])]);
    })));
  }

  /* ---------- V151: جدول → کارت‌های نئومورفیک (SchoolManagementScreen: Card با عنوان، خط‌های اطلاعات، ردیف عملیات وسط‌چین) ----------
     همهٔ صفحه‌های داخلی سایت (کلاس‌ها، دانش‌آموزان، معلم‌ها، کیف پول، تصحیح، آزمون‌های چاپی…) جدول‌اند؛ در پوستهٔ موبایل هر
     <table.tbl> به فهرست کارت تبدیل می‌شود. عملیات (icon-btn/btn/chip) همان گره‌های اصلی‌اند و منتقل می‌شوند، پس رفتار دست‌نخورده می‌ماند. */
  function tableToCards(t) {
    if (!t.tHead || t.dataset.mCards) return;
    var heads = Array.prototype.map.call(t.tHead.rows[0] ? t.tHead.rows[0].cells : [], function (th) { return th.textContent.trim(); });
    var rows = t.tBodies[0] ? Array.prototype.slice.call(t.tBodies[0].rows) : [];
    if (!rows.length) return;
    var list = el('div', {class: 'm-rows'});
    rows.forEach(function (tr, ri) {
      var cells = Array.prototype.slice.call(tr.cells);
      var card = el('div', {class: 'm-rowcard neo', style: 'animation-delay:' + Math.min(ri, 12) * 25 + 'ms'});
      var head = el('div', {class: 'm-rowcard-h'}), body = el('div', {class: 'm-rowcard-b'}), acts = el('div', {class: 'm-rowcard-acts'});
      cells.forEach(function (td, i) {
        var h = heads[i] || '';
        var hasActs = td.querySelector('.acts, .icon-btn, .btn');
        if (hasActs && (!h || i === cells.length - 1)) { while (td.firstChild) acts.appendChild(td.firstChild); return; }
        if (i === 0) { while (td.firstChild) head.appendChild(td.firstChild); return; }
        if (!td.textContent.trim() && !td.firstElementChild) return;
        var line = el('div', {class: 'm-kv'}, [el('span', {class: 'k', text: h ? h + ':' : ''})]);
        var v = el('span', {class: 'v'}); while (td.firstChild) v.appendChild(td.firstChild); line.appendChild(v);
        body.appendChild(line);
      });
      card.appendChild(head); if (body.childNodes.length) card.appendChild(body); if (acts.childNodes.length) card.appendChild(acts);
      if (tr.onclick) card.onclick = tr.onclick;
      list.appendChild(card);
    });
    t.dataset.mCards = '1';
    t.style.display = 'none';
    t.parentNode.insertBefore(list, t.nextSibling);
  }
  function upgradeContent() {
    if (!active()) return;
    var c = document.getElementById('content'); if (!c) return;
    c.querySelectorAll('table.tbl').forEach(tableToCards);
    /* آزمون‌ها/کارت‌ها/منو خودشان بومی‌اند؛ بقیهٔ صفحه‌ها: نوار ابزار بالای صفحه به سبک اپ (دکمهٔ اصلی پهن) */
    c.querySelectorAll('.card > .row:first-child, #content > .row').forEach(function (r) { r.classList.add('m-toolbar'); });
  }
  if (window.MutationObserver) new MutationObserver(function (muts) {
    for (var i = 0; i < muts.length; i++) { for (var j = 0; j < muts[i].addedNodes.length; j++) { var n = muts[i].addedNodes[j]; if (n.nodeType === 1 && (n.matches && (n.matches('table.tbl') || n.querySelector('table.tbl')))) { upgradeContent(); return; } } }
  }).observe(document.documentElement, {childList: true, subtree: true});

  /* ---------- V152: سازندهٔ آزمون در گوشی (ExamBuilderScreen) ----------
     اپ: نوار بالا «ساخت آزمون/ویرایش آزمون» + بازگشت؛ دکمهٔ «مشخصات آزمون» تمام‌عرض؛ FAB سبز ✓ ذخیره (چپ) و FAB + (راست) که
     منوی شعاعی نوع سؤال را باز می‌کند (تشریحی، چندگزینه‌ای، صحیح/غلط، جای خالی، عددی، جورکردنی، وارد کردن، بانک سؤال) با رنگ‌های پاستلی
     QuestionDraft.pastelColor؛ در حالت چاپ FAB سوم پیش‌نمایش/چاپ. */
  var PASTEL = {essay: '#FFD1DC', multiple: '#AEC6CF', truefalse: '#B4EEB4', fill: '#FDFD96', numeric: '#C3B1E1', matching: '#FFDAB9', import: '#98FF98', bank: '#E6E6FA'};
  var RADIAL = [['essay', 'تشریحی', '✎'], ['multiple', 'چندگزینه‌ای', '◉'], ['truefalse', 'صحیح/غلط', '✓'], ['fill', 'جای خالی', '＿'], ['numeric', 'عددی', '۱۲'], ['matching', 'جورکردنی', '↔'], ['import', 'وارد کردن', '⇩'], ['bank', 'بانک سؤال', '▤']];
  function builderFabs(c) {
    var old = document.getElementById('m-bfab'); if (old) old.remove();
    var top = c.querySelector('.b-top'); if (!top) return;
    var btns = Array.prototype.slice.call(top.querySelectorAll('.row .btn'));
    var saveBtn = btns.filter(function (b) { return /ذخیره/.test(b.textContent); })[0];
    var prevBtn = btns.filter(function (b) { return /پیش‌نمایش/.test(b.textContent); })[0];
    var setBtn = btns.filter(function (b) { return /مشخصات آزمون/.test(b.textContent); })[0];
    var addRow = c.querySelector('.b-add');
    var isPrint = /چاپی/.test(top.textContent);
    /* دکمهٔ «مشخصات آزمون» تمام‌عرض زیر فیلدها (اپ: OutlinedButton fillMaxWidth) */
    if (setBtn && !top.querySelector('.m-settings-btn')) { var sb = el('button', {class: 'm-outline m-settings-btn', text: 'مشخصات آزمون', onclick: function () { setBtn.click(); }}); top.appendChild(sb); }
    var radialOpen = false;
    var bar = el('div', {class: 'm-bfab', id: 'm-bfab'});
    var save = el('button', {class: 'm-fab save', 'aria-label': 'ذخیره آزمون', onclick: function () { if (saveBtn) saveBtn.click(); }}, [ic('grading')]);
    var plus = el('button', {class: 'm-fab add', 'aria-label': 'افزودن سؤال', onclick: function () { radialOpen = !radialOpen; drawRadial(); }}, [el('span', {class: 'm-fab-plus', text: '+'})]);
    var prev = isPrint && prevBtn ? el('button', {class: 'm-fab prev', 'aria-label': 'پیش‌نمایش آزمون', onclick: function () { prevBtn.click(); }}, [ic('print')]) : null;
    bar.appendChild(save); if (prev) bar.appendChild(prev); bar.appendChild(plus);
    var radial = null;
    function drawRadial() {
      if (radial) { radial.remove(); radial = null; }
      plus.classList.toggle('on', radialOpen);
      if (!radialOpen) return;
      radial = el('div', {class: 'm-radial-bg', onclick: function (e) { if (e.target === e.currentTarget) { radialOpen = false; drawRadial(); } }});
      var ring = el('div', {class: 'm-radial'});
      RADIAL.forEach(function (r, i) {
        /* دکمهٔ + در گوشهٔ راست‌پایین است؛ ربع‌دایرهٔ چپ‌بالا در دو حلقه (۴+۴) تا دایره‌ها روی هم نیفتند */
        var ring_i = i < 4 ? 0 : 1, k = i % 4;
        var ang = Math.PI + (Math.PI / 2) * ((k + (ring_i ? 0.5 : 0)) / 3.5);
        var R = ring_i ? 200 : 115, x = Math.cos(ang) * R, y = Math.sin(ang) * R;
        ring.appendChild(el('button', {class: 'm-radial-item', style: 'background:' + PASTEL[r[0]] + ';transform:translate(' + x.toFixed(0) + 'px,' + y.toFixed(0) + 'px);animation-delay:' + (i * 30) + 'ms', onclick: function () {
          radialOpen = false; drawRadial();
          if (r[0] === 'import') { if (window.SiteExtras) window.SiteExtras.importExam(); return; }
          if (r[0] === 'bank') { var bb = c.querySelector('.b-list .btn.soft'); if (bb) bb.click(); return; }
          if (addRow) { var b = Array.prototype.filter.call(addRow.querySelectorAll('button'), function (x) { return x.title === typeLabel(r[0]); })[0]; if (b) b.click(); }
          setTimeout(function () { var ed = c.querySelector('.b-editor'); if (ed && ed.scrollIntoView) ed.scrollIntoView({behavior: 'smooth', block: 'start'}); }, 30);
        }}, [el('span', {class: 'g', text: r[2]}), el('span', {class: 'l', text: r[1]})]));
      });
      radial.appendChild(ring); document.body.appendChild(radial);
    }
    document.body.appendChild(bar);
  }
  function typeLabel(t) { return {multiple: 'چندگزینه‌ای', truefalse: 'صحیح / غلط', fill: 'جای‌خالی', numeric: 'عددی', matching: 'جورکردنی', essay: 'تشریحی'}[t]; }
  function cleanupBuilder() { ['m-bfab'].forEach(function (id) { var n = document.getElementById(id); if (n) n.remove(); }); var r = document.querySelector('.m-radial-bg'); if (r) r.remove(); }

  /* ---------- پوسته ---------- */
  var STUDENT_TITLES = {dashboard: 'خانه دانش‌آموز', join: 'خانه دانش‌آموز', grades: 'نتایج من', calendar: 'تقویم و پیام‌ها', profile: 'حساب', tools: 'تنظیمات'};
  var MANAGER_TITLES = {teachers: 'معلم‌ها', dashboard: 'داشبورد', school: 'مدرسه', wallet: 'کیف پول', profile: 'حساب', tools: 'تنظیمات و ابزارها', calendar: 'تقویم', cards: 'کارت‌ها'};
  var TITLES = {exams: 'آزمون‌ها', dashboard: 'آزمون‌ها', wallet: 'کیف پول', cards: 'کارت‌ها', builder: 'سازندهٔ آزمون', classes: 'کلاس‌ها', students: 'دانش‌آموزان', bank: 'بانک سؤال', reports: 'گزارش‌ها', grading: 'تصحیح', calendar: 'تقویم و پیام‌ها', tools: 'تنظیمات و ابزارها', profile: 'حساب'};
  function paint() {
    cleanupBuilder();
    var root = document.getElementById('root'); if (!root) return;
    var shell = document.getElementById('m-shell');
    if (!shell) { shell = el('div', {class: 'm-shell', id: 'm-shell'}); root.innerHTML = ''; root.appendChild(shell); }
    shell.innerHTML = '';
    var page = ui.menuOpen ? 'menu' : view.panel;
    if (isStudent()) { paintStudent(shell, page); return; }
    var head = null;
    var mgr = isManager();
    var home = mgr ? 'teachers' : 'exams';
    var noHead = mgr ? (page === 'teachers' || page === 'cards') : (page === 'exams' || page === 'dashboard' || page === 'cards');
    if (!ui.menuOpen && !noHead) {
      head = el('div', {class: 'm-head'}, [el('button', {class: 'm-back', 'aria-label': 'بازگشت', onclick: function () { go(home); }}, [ic('chevron')]), el('h1', {text: (mgr ? MANAGER_TITLES : TITLES)[page] || ''})]);
    }
    var content = el('div', {class: 'm-content' + (head ? '' : ' no-head'), id: 'content'});
    if (head) shell.appendChild(head);
    shell.appendChild(content);
    shell.appendChild(dock());
    if (ui.addOpen) shell.appendChild(quickAdd());
    if (ui.menuOpen) content.appendChild(mgr ? managerMenu() : menuScreen());
    else if (mgr && page === 'cards') managerCards(content);
    else if (mgr) S.renderPage(content);
    else if (page === 'exams' || page === 'dashboard') examsScreen(content);
    else if (page === 'cards') cardsScreen(content);
    else S.renderPage(content);
    if (page === 'builder') content.classList.add('m-builder');
  }
  /* سازنده: بعد از اینکه builder.js نوار بالا را ساخت، FABها اضافه می‌شوند */
  if (window.MutationObserver) new MutationObserver(function () {
    if (!active() || view.panel !== 'builder') return;
    var c = document.getElementById('content'); if (!c || !c.querySelector('.b-top') || document.getElementById('m-bfab')) return;
    builderFabs(c);
  }).observe(document.documentElement, {childList: true, subtree: true});

  /* دانش‌آموز: نوار بالا (عنوان + ☰ مثل اپ)، بدون داک؛ در حین آزمون نوار حذف می‌شود (StudentExamScreen تمام‌صفحه) */
  function paintStudent(shell, page) {
    var inExam = window.SiteStudent && window.SiteStudent.inExam();
    var content = el('div', {class: 'm-content m-student-content', id: 'content'});
    if (!inExam) {
      shell.appendChild(el('div', {class: 'm-head m-head-student'}, [
        el('button', {class: 'm-back', 'aria-label': 'منو', onclick: function () { ui.menuOpen = !ui.menuOpen; paint(); }}, [ic(ui.menuOpen ? 'close' : 'menu')]),
        el('h1', {text: ui.menuOpen ? 'منو' : (STUDENT_TITLES[page] || '')}),
        (!ui.menuOpen && page !== 'dashboard' && page !== 'join') ? el('button', {class: 'm-back', 'aria-label': 'خانه', onclick: function () { go('dashboard'); }}, [ic('chevron')]) : null
      ]));
    }
    shell.appendChild(content);
    if (inExam) { if (window.SiteStudent) window.SiteStudent.page(content, view.arg); return; }
    if (ui.menuOpen) content.appendChild(studentMenu());
    else if (page === 'dashboard' || page === 'join') studentHome(content);
    else S.renderPage(content);
  }

  /* اتصال: app.js در render() اگر active() بود paint() را صدا می‌زند؛ تغییر عرض → رندر دوباره */
  var rerender = function () { if (S.user()) S.render(); };
  MQ.addEventListener ? MQ.addEventListener('change', rerender) : MQ.addListener(rerender);
  window.SiteMobile = {paint: paint, active: active, ui: ui};
})();
