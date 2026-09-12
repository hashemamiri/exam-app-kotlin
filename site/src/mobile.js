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
    if (p === 'exams' || p === 'dashboard' || p === 'builder' || p === 'print') return 'exams';
    if (p === 'cards' || p === 'reports' || p === 'bank' || p === 'grading') return 'cards';
    return 'none';
  }
  /* V163 — با باز شدن منوی همبرگری، هر پنجره/شیت باز (modal-bg, m-sheet-bg) بسته می‌شود تا منو زیر آن نماند */
  function closeOverlays() { document.querySelectorAll('.modal-bg, .m-sheet-bg, .m-radial-bg').forEach(function (n) { n.remove(); }); document.body.style.overflow = ''; }
  function go(panel, arg) { ui.menuOpen = false; ui.addOpen = false; S.go(panel, arg); }
  /* ---------- V158: دکمهٔ برگشت گوشی مثل BackHandler اپ (ExamApp.kt:358–364) ----------
     هر ناوبری در حالت گوشی یک ورودی history می‌سازد؛ برگشت: اول پنجره/شیت باز → بسته می‌شود، بعد منو/افزودن سریع،
     بعد صفحهٔ قبلی؛ اگر صفحهٔ قبلی نبود (صفحهٔ خانه) پرسش «از سایت خارج می‌شوید؟» و با تأیید خروج از حساب. */
  var histDepth = 0, backGuard = false;
  function homePanel() { var u = S.user(); return !u ? 'dashboard' : (u.role === 'manager' ? 'teachers' : (u.role === 'student' ? 'dashboard' : 'exams')); }
  function pushHist() { if (!MQ.matches || backGuard) return; try { history.pushState({m: ++histDepth}, ''); } catch (e) {} }
  function closeTopOverlay() {
    var sel = ['.m-sheet-bg', '.modal-bg', '.engine-bg', '.m-radial-bg', '.m-qa-bg'];
    for (var i = 0; i < sel.length; i++) { var all = document.querySelectorAll(sel[i]); if (all.length) { var n = all[all.length - 1]; if (n.classList.contains('engine-bg')) { var x = n.querySelector('.engine-bar .btn'); if (x) x.click(); else n.remove(); } else if (n.classList.contains('m-qa-bg')) { ui.addOpen = false; paint(); } else n.remove(); return true; } }
    return false;
  }
  function onBack() {
    if (!active() && !authActive()) return;
    if (closeTopOverlay()) { pushHist(); return; }
    if (ui.addOpen || ui.menuOpen) { ui.addOpen = false; ui.menuOpen = false; paint(); pushHist(); return; }
    if (window.SiteStudent && window.SiteStudent.inExam && window.SiteStudent.inExam()) { pushHist(); toast('برای خروج از آزمون از دکمهٔ پایان/خروج داخل آزمون استفاده کنید.', 'info'); return; }
    if (view.panel === 'builder' && view.arg && view.arg.mode === 'print') { pushHist(); go('print'); return; }
    var home = homePanel();
    if (S.user() && view.panel !== home) { pushHist(); go(home); return; }
    /* صفحهٔ قبلی وجود ندارد → پرسش خروج */
    pushHist();
    S.confirmDlg('خروج', S.user() ? 'از سایت خارج می‌شوید؟' : 'از سایت خارج می‌شوید؟', 'خروج', true).then(function (ok) {
      if (!ok) return;
      backGuard = true;
      var leave = function () { try { history.go(-(histDepth + 1)); } catch (e) {} setTimeout(function () { try { window.close(); } catch (e) {} backGuard = false; }, 300); };
      if (S.user()) { S.logout().then(leave, leave); } else leave();
    });
  }
  window.addEventListener('popstate', function () { onBack(); });
  (function () { var _go = S.go; S.go = function (panel, arg) { if (MQ.matches && (panel !== view.panel || (arg && arg.mode === 'print') || panel === 'builder')) pushHist(); return _go.apply(this, arguments); }; if (MQ.matches) { try { history.replaceState({m: 0}, ''); } catch (e) {} } })();

  /* ---------- آیکون‌های خطی (شبیه Design69Icons) ---------- */
  var I = {
    menu: '<svg viewBox="0 0 24 24"><path d="M4 7h16M4 12h16M4 17h16"/></svg>',
    close: '<svg viewBox="0 0 24 24"><path d="M6 6l12 12M18 6L6 18"/></svg>',
    check: '<svg viewBox="0 0 24 24"><path d="M5 12l5 5L20 7"/></svg>',
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
    school: '<svg viewBox="0 0 24 24"><path d="M2 9l10-5 10 5-10 5z"/><path d="M6 11.5V16c0 1.5 3 3 6 3s6-1.5 6-3v-4.5"/><path d="M22 9v6"/></svg>',
    lock: '<svg viewBox="0 0 24 24"><rect x="5" y="11" width="14" height="10" rx="2"/><path d="M8 11V7a4 4 0 018 0v4"/></svg>',
    mail: '<svg viewBox="0 0 24 24"><rect x="3" y="5" width="18" height="14" rx="2"/><path d="M3 7l9 6 9-6"/></svg>',
    eye: '<svg viewBox="0 0 24 24"><path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6S2 12 2 12z"/><circle cx="12" cy="12" r="3"/></svg>',
    search: '<svg viewBox="0 0 24 24"><circle cx="11" cy="11" r="6"/><path d="M20 20l-4.5-4.5"/></svg>',
    filter: '<svg viewBox="0 0 24 24"><path d="M4 6h16M7 12h10M10 18h4"/></svg>',
    eyeoff: '<svg viewBox="0 0 24 24"><path d="M3 3l18 18M10.5 10.6A2 2 0 0 0 13.4 13.5M9.9 5.2A10 10 0 0 1 22 12a10.6 10.6 0 0 1-3.2 3.7M6.6 6.6A10.6 10.6 0 0 0 2 12s3.5 6 10 6a9.6 9.6 0 0 0 4.2-.9"/></svg>',
    toggleon: '<svg viewBox="0 0 24 24"><rect x="2" y="7" width="20" height="10" rx="5"/><circle cx="16" cy="12" r="3" fill="currentColor"/></svg>',
    toggleoff: '<svg viewBox="0 0 24 24"><rect x="2" y="7" width="20" height="10" rx="5"/><circle cx="8" cy="12" r="3"/></svg>',
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
      item('منو', ui.menuOpen ? 'close' : 'menu', function () { ui.menuOpen = !ui.menuOpen; ui.addOpen = false; if (ui.menuOpen) closeOverlays(); paint(); }, 'menu'),
      item('کیف پول', 'wallet', function () { go('wallet'); }, 'wallet'),
      el('button', {class: 'm-dock-add' + (ui.addOpen ? ' on' : ''), 'aria-label': 'افزودن سریع', onclick: function () { ui.addOpen = !ui.addOpen; ui.menuOpen = false; if (ui.addOpen) pushHist(); paint(); }}, [ic(ui.addOpen ? 'close' : 'plus')]),
      isManager() ? item('معلم‌ها', 'students', function () { go('teachers'); }, 'exams') : item('آزمون‌ها', 'exams', function () { go('exams'); }, 'exams'),
      item('کارت‌ها', 'cards', function () { if (view.panel === 'cards' && !ui.menuOpen && !ui.addOpen) ui.cycle = true; go('cards'); }, 'cards')
    ])]);
  }

  /* ---------- افزودن سریع (Design69QuickAddOverlay) ---------- */
  function quickAdd() {
    var mgr = isManager();
    var items = [
      mgr ? ['دعوت معلم', 'ساخت کد دعوت برای معلم', 'students', function () { go('teachers'); }] : ['آزمون جدید', 'ساخت آزمون آنلاین', 'exams', function () { go('builder', null); }],
      ['دانش‌آموز جدید', mgr ? 'در کلاس یکی از معلم‌ها' : 'افزودن به کلاس', 'students', async function () { ui.addOpen = false; paint(); if (mgr) return managerStudentPicker(); var classes = await S.rpc('native_my_classes_v28', {}).catch(function () { return []; }); window.SiteSchool.studentForm(null, classes || [], null, function () { if (view.panel === 'students') paint(); }); }],
      ['کلاس جدید', mgr ? 'برای یکی از معلم‌ها' : 'ساخت کلاس', 'classes', function () { mgr ? go('school') : go('classes', {create: true}); }],
      /* V61.5 — عمل چهارم: مدرسه جدید (مدیر می‌سازد؛ معلم با کد دعوت عضو می‌شود) */
      /* V158 — مثل SchoolLaunchAction.CREATE_SCHOOL: مدیر «ساخت مدرسه جدید»، معلم «عضویت در مدرسه جدید» با کد ۶ حرفی */
      ['مدرسه جدید', mgr ? 'ساخت مدرسه' : 'عضویت با کد دعوت', 'classes', function () { ui.addOpen = false; paint(); mgr ? createSchoolDialog() : joinSchoolDialog(); }]
    ];
    /* V153 — چیدمان ضربدری اپ: پنل فرورفته، ۴ کارت ۸۸dp در چهار گوشه، خط‌چین از مرکز، دکمهٔ ✕ گرادیانی وسط */
    var close = function () { ui.addOpen = false; paint(); };
    var pos = [[1, -1], [-1, -1], [1, 1], [-1, 1]]; /* x: راست/چپ (RTL: مثبت = راست)، y: بالا/پایین */
    var xs = [];
    var stage = el('div', {class: 'm-qa-stage'}, [
      el('div', {class: 'm-qa-panel neo pressed'}),
      el('svg', {class: 'm-qa-lines'}),
      el('button', {class: 'm-qa-x', 'aria-label': 'بستن افزودن سریع', onclick: close}, [ic('plus')])
    ].concat(items.map(function (it, i) {
      var b = el('button', {class: 'm-qa-item', style: '--qx:' + (pos[i][0] * -1) + ';--qy:' + pos[i][1] + ';animation-delay:' + (i * 40) + 'ms', onclick: it[3]}, [ic(it[2], 'm-qa-ic'), el('span', {text: it[0]})]);
      xs.push(b); return b;
    })));
    var svg = stage.querySelector('svg'); svg.setAttribute('viewBox', '-160 -165 320 330'); svg.innerHTML = pos.map(function (p) { return '<line x1="0" y1="0" x2="' + (p[0] * -108) + '" y2="' + (p[1] * 108) + '"/>'; }).join('');
    return el('div', {class: 'm-sheet-bg m-qa-bg', onclick: function (e) { if (e.target === e.currentTarget) close(); }}, [stage]);
  }

  /* ---------- منوی کاشی‌ای (Design69MainMenuScreen) — همان ۸ کارت معلم به همان ترتیب ---------- */
  function menuScreen() {
    var u = S.user();
    var cards = [
      ['تقویم', 'رویدادها و پیام‌ها', 'calendar', function () { go('calendar'); }],
      ['چاپ آزمون', 'اطلاعات رسمی چاپ آزمون', 'print', function () { go('print'); }],
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
        el('div', {class: 'm-avatar'}, [(localAvatar(u.id) || u.avatarUrl) ? el('img', {src: localAvatar(u.id) || u.avatarUrl, alt: ''}) : el('span', {text: (u.name || '?').trim().charAt(0)})]),
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
      var card = el('div', {class: 'm-exam neo' + (open ? ' open' : ''), 'data-exam': x.id, onclick: function (e) { if (e.target.closest('.m-exam-acts')) return; ui.expanded = open ? null : x.id; ui.scrollTo = open ? null : x.id; examsScreen(c); }}, [
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
    /* V163 — کارت بازشده (با دکمه‌هایش) در دید بماند؛ قبلاً دکمه‌ها زیر داک می‌رفتند و باید اسکرول می‌شد */
    if (ui.scrollTo) { var tgt = wrap.querySelector('[data-exam="' + ui.scrollTo + '"]'); ui.scrollTo = null; if (tgt) requestAnimationFrame(function () { tgt.scrollIntoView({block: 'nearest', behavior: 'smooth'}); }); }
  }
  function act(icon, label, on, danger) { return el('button', {class: 'm-act' + (danger ? ' danger' : ''), title: label, 'aria-label': label, onclick: on}, [ic(icon)]); }
  function printExamsSheet() {
    var bg = el('div', {class: 'm-sheet-bg', onclick: function (e) { if (e.target === e.currentTarget) bg.remove(); }});
    var body = el('div', {class: 'm-sheet'}, [el('h3', {text: 'آزمون‌های چاپی'})]);
    var box = el('div'); body.appendChild(box); S.loading(box);
    body.appendChild(el('button', {class: 'm-outline', style: 'margin-top:12px;width:100%', text: 'بستن', onclick: function () { bg.remove(); }}));
    bg.appendChild(body); document.body.appendChild(bg);
    (async function () {
      try {
        var list = await window.SiteBuilder.printExamsList(); box.innerHTML = '';
        if (!list.length) box.appendChild(el('p', {class: 'm-note', text: 'هنوز آزمون چاپی‌ای ذخیره نشده است. از بخش «چاپ آزمون» بسازید.'}));
        else {
          box.appendChild(el('p', {class: 'm-note', text: 'با انتخاب هر آزمون، ویرایشگر آن باز می‌شود.'}));
          list.forEach(function (r) { box.appendChild(el('button', {class: 'm-row neo', onclick: function () { bg.remove(); go('builder', {mode: 'print', printId: r.id}); }}, [ic('print', 'm-row-ic'), el('div', {}, [el('b', {text: r.title || 'آزمون چاپی'}), el('span', {text: (r.subject || 'بدون درس') + ' · ' + fa(r.question_count || 0) + ' سؤال'})])])); });
        }
      } catch (e) { box.innerHTML = ''; box.appendChild(el('p', {class: 'm-note', text: S.errMsg(e)})); }
    })();
  }

  /* V153 — دستهٔ کارت‌ها مثل اپ: ۳ کارت روی هم (۹۰٪ عرض × ۱۹۰dp، گوشهٔ ۲۹dp)، کشیدن افقی ≥۵۲px کارت را رد می‌کند، نقطه‌ها، پنل توضیح؛ ضربهٔ دوباره روی داک = کارت بعدی */
  var deckIndex = {};
  function cardsDeck(c, key, cards) {
    c.innerHTML = '';
    if (ui.cycle) { deckIndex[key] = ((deckIndex[key] || 0) + 1) % cards.length; ui.cycle = false; }
    var idx = deckIndex[key] || 0;
    var stage = el('div', {class: 'm-deck-stage'});
    var dots = el('div', {class: 'm-deck-dots'});
    var info = el('div', {class: 'm-deck-info neo pressed'}, [el('b'), el('span')]);
    function draw(dir) {
      stage.innerHTML = ''; dots.innerHTML = '';
      for (var rel = 2; rel >= 0; rel--) (function (rel) {
        var k = cards[(idx + rel) % cards.length];
        var card = el('button', {class: 'm-deck-card r' + rel + (dir && rel === 0 ? ' enter' : ''), style: 'background:' + k[3], onclick: function () { if (rel === 0) k[4](); }}, [
          el('div', {class: 'm-deck-top'}, [el('span', {class: 'm-deck-ic', html: I[k[2]]}), el('small', {text: 'آزمون آنلاین'})]),
          el('b', {text: k[0]})
        ]);
        if (rel === 0) swipe(card);
        stage.appendChild(card);
      })(rel);
      cards.forEach(function (_, i) { dots.appendChild(el('i', {class: i === idx ? 'on' : ''})); });
      info.querySelector('b').textContent = cards[idx][0]; info.querySelector('span').textContent = cards[idx][1];
    }
    function swipe(card) {
      var sx = 0, sy = 0, dx = 0, dy = 0, on = false;
      card.addEventListener('pointerdown', function (e) { sx = e.clientX; sy = e.clientY; dx = dy = 0; on = true; card.setPointerCapture(e.pointerId); card.style.transition = 'none'; });
      card.addEventListener('pointermove', function (e) { if (!on) return; dx = e.clientX - sx; dy = e.clientY - sy; card.style.transform = 'translate(' + dx + 'px,' + dy + 'px) rotate(' + (dx / 42 + dy / 75) + 'deg)'; });
      function end() {
        if (!on) return; on = false; card.style.transition = '';
        if (Math.abs(dx) >= Math.abs(dy) && Math.abs(dx) > 52) {
          card.classList.add('fly'); card.style.transform = 'translate(' + (dx < 0 ? -520 : 520) + 'px,' + (dy * 1.2 - 36) + 'px) rotate(' + (dx < 0 ? 14 : -14) + 'deg) scale(.92)';
          idx = (idx + 1) % cards.length; deckIndex[key] = idx;
          setTimeout(function () { draw(true); }, 260);
        } else card.style.transform = '';
      }
      card.addEventListener('pointerup', end); card.addEventListener('pointercancel', end);
      card.addEventListener('click', function (e) { if (Math.abs(dx) > 6 || Math.abs(dy) > 6) { e.stopImmediatePropagation(); e.preventDefault(); } }, true);
    }
    draw(false);
    c.appendChild(el('div', {class: 'm-deck'}, [stage, dots, info]));
  }
  /* ---------- کارت‌ها (TeacherManagementCardsScreen) ---------- */
  /* V167 — فهرست کارت‌ها مشترک با صفحهٔ «کارت‌ها»ی دسکتاپ (app.js → pageCards) */
  function teacherCards() {
    var cards = [
      ['آمار', 'نمودارها، میانگین‌ها و تحلیل کیفیت سؤال‌های آزمون را نشان می‌دهد.', 'reports', 'linear-gradient(135deg,#6C63F5,#27C4A8)', function () { go('reports', {section: 'stats'}); }],
      ['کارنامه', 'کارنامه و لیست نمرات کلاس؛ انتخاب آزمون‌ها و خروجی Excel یا PDF.', 'reports', 'linear-gradient(135deg,#0EA5E9,#6366F1)', function () { go('reports', {section: 'grades'}); }],
      ['بانک سؤال', 'جست‌وجو، دسته‌بندی، مشاهده، ویرایش، حذف و افزودن سؤال به آزمون.', 'exams', 'linear-gradient(135deg,#2878DB,#24B8C8)', function () { go('bank'); }],
      ['تصحیح', 'همه پاسخ‌ها، حضور، بازخورد و ثبت یا اصلاح نمره را باز می‌کند.', 'grading', 'linear-gradient(135deg,#25BFA4,#45D7BD)', function () { go('grading'); }],
      ['مانده', 'فقط پاسخ‌های در انتظار تصحیح و پیگیری را نمایش می‌دهد.', 'cards', 'linear-gradient(135deg,#E0587F,#7D6CF4)', function () { go('grading', {filter: 'pending'}); }],
      ['پاسخ', 'فقط پاسخ‌های تصحیح‌شده دارای نمره و بازخورد نهایی را نمایش می‌دهد.', 'grading', 'linear-gradient(135deg,#4D5B74,#273247)', function () { go('grading', {filter: 'graded'}); }],
      ['درخواست‌ها', 'درخواست‌های ویرایش یا حذف مدیر را مشاهده، تأیید یا رد کنید.', 'account', 'linear-gradient(135deg,#7D6CF4,#E0587F)', function () { go('dashboard', {requests: true}); }]
    ];
    return cards;
  }
  function cardsScreen(c) { cardsDeck(c, 'teacher', teacherCards()); }

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
        el('div', {class: 'm-avatar'}, [(localAvatar(u.id) || u.avatarUrl) ? el('img', {src: localAvatar(u.id) || u.avatarUrl, alt: ''}) : el('span', {text: (u.name || '?').trim().charAt(0)})]),
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
        el('div', {class: 'm-avatar'}, [(localAvatar(u.id) || u.avatarUrl) ? el('img', {src: localAvatar(u.id) || u.avatarUrl, alt: ''}) : el('span', {text: (u.name || '?').trim().charAt(0)})]),
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
    var cards = [
      ['مدارس', 'لیست مدرسه‌ها، ساخت مدرسه جدید و کلاس‌های هر مدرسه را باز می‌کند.', 'classes', 'linear-gradient(135deg,#6C63F5,#27C4A8)', function () { go('school'); }],
      ['کارنامه', 'آمار پاسخ‌ها، میانگین نمره و فعالیت معلم‌های مدرسه.', 'reports', 'linear-gradient(135deg,#2878DB,#24B8C8)', function () { go('dashboard'); }],
      ['وضعیت', 'داشبورد مدرسه با اطلاعات، آمار کلی و پنل سریع بخش‌ها.', 'cards', 'linear-gradient(135deg,#25BFA4,#45D7BD)', function () { go('dashboard'); }]
    ];
    cardsDeck(c, 'manager', cards);
  }


  /* ---------- V155: چاپ آزمون (ExamPrintCenterScreen) — «آزمون جدید» · «آزمون‌های آنلاین» + کارت‌های آزمون چاپی محلی ---------- */
  /* V163 — مرکز چاپ: آزمون‌های چاپی از سرور (print_exams) — یکی برای اپ، دسکتاپ و گوشی */
  async function printCenter(c) {
    c.innerHTML = '';
    var wrap = el('div', {class: 'm-print'});
    var status = el('p', {class: 'm-note', style: 'display:none;padding:4px 0'});
    wrap.appendChild(el('div', {class: 'm-print-top'}, [
      el('button', {class: 'btn m-btn', text: 'آزمون جدید', onclick: function () { go('builder', {mode: 'print', fresh: true}); }}),
      el('button', {class: 'm-outline', text: 'آزمون‌های آنلاین', onclick: onlineSheet})
    ]));
    wrap.appendChild(status);
    var listBox = el('div'); wrap.appendChild(listBox); c.appendChild(wrap);
    S.loading(listBox);
    var list = [];
    try { await window.SiteBuilder.migrateLocalPrintExams(); list = await window.SiteBuilder.printExamsList(); } catch (e) { S.showErr(listBox, e); return; }
    listBox.innerHTML = '';
    if (!list.length) listBox.appendChild(el('p', {class: 'm-note', text: 'هنوز آزمون چاپی‌ای نیست. «آزمون جدید» بزنید یا از «آزمون‌های آنلاین» نسخهٔ چاپی بسازید.'}));
    list.forEach(function (r) {
      listBox.appendChild(el('div', {class: 'm-pcard neo'}, [
        el('div', {class: 'm-pcard-h'}, [el('b', {text: r.title || 'آزمون چاپی'}), el('span', {class: 'm-chip', text: 'چاپی'})]),
        el('div', {class: 'm-pcard-m', text: 'درس: ' + (r.subject || '—') + ' · ' + fa(r.question_count || 0) + ' سؤال'}),
        el('div', {class: 'm-pcard-acts'}, [
          act('edit', 'ویرایش آزمون چاپی', function () { go('builder', {mode: 'print', printId: r.id}); }),
          act('trash', 'حذف آزمون چاپی', async function () { if (!(await S.confirmDlg('حذف آزمون چاپی', 'آزمون «' + esc(r.title || 'آزمون چاپی') + '» برای همیشه حذف شود؟ این کار برگشت‌پذیر نیست.', 'حذف', true))) return; try { await window.SiteBuilder.printExamDelete(r.id); printCenter(c); } catch (e) { toast(S.errMsg(e), 'err'); } }, true)
        ])
      ]));
    });
    /* V163 — پنجرهٔ آزمون‌های آنلاین بدون پرش: ارتفاع ثابت از ابتدا، بارگذاری داخل همان جعبه، فهرست یک‌باره ساخته می‌شود */
    async function onlineSheet() {
      var bg = el('div', {class: 'm-sheet-bg', onclick: function (e) { if (e.target === e.currentTarget) bg.remove(); }});
      var box = el('div', {class: 'm-sheet-box'}); S.loading(box);
      var body = el('div', {class: 'm-sheet m-sheet-fixed'}, [el('h3', {text: 'آزمون‌های آنلاین'}), box, el('button', {class: 'm-outline', style: 'margin-top:12px;width:100%', text: 'بستن', onclick: function () { bg.remove(); }})]);
      bg.appendChild(body); document.body.appendChild(bg);
      var exams = [], err = null; try { exams = await api.exams(); } catch (e) { err = e; }
      var frag = document.createDocumentFragment();
      if (err) frag.appendChild(el('p', {class: 'm-note', text: S.errMsg(err)}));
      else if (!exams.length) frag.appendChild(el('p', {class: 'm-note', text: 'آزمون آنلاینی ندارید.'}));
      else {
        frag.appendChild(el('p', {class: 'm-note', style: 'padding:4px 0 10px', text: 'با انتخاب هر آزمون، نسخهٔ چاپی آن ساخته و باز می‌شود (روی سرور؛ در اپ و سایت یکی است).'}));
        exams.forEach(function (x) {
          var has = list.some(function (r) { return r.source_exam_id === x.id; });
          frag.appendChild(el('button', {class: 'm-row neo', onclick: function () { bg.remove(); openPrintCopy(x); }}, [ic('print', 'm-row-ic'), el('div', {}, [el('b', {text: x.title || 'بدون عنوان'}), el('span', {text: 'درس: ' + (x.subject || '—') + (has ? ' · نسخهٔ چاپی دارد' : '')})])]));
        });
      }
      box.innerHTML = ''; box.appendChild(frag);
    }
    /* مثل openPrintCopy اپ: اگر قبلاً نسخهٔ چاپی ساخته شده همان باز می‌شود؛ وگرنه سؤال‌ها (با پاسخ‌نامه) خوانده و به‌صورت آزمون چاپی روی سرور ذخیره می‌شوند */
    async function openPrintCopy(x) {
      var existing = list.filter(function (r) { return r.source_exam_id === x.id; })[0];
      if (existing) return go('builder', {mode: 'print', printId: existing.id});
      status.style.display = ''; status.style.color = ''; status.textContent = 'در حال آماده‌سازی نسخهٔ چاپی...';
      try {
        var exam = await api.examDetail(x.id);
        var keys = {}; (Array.isArray(exam.__answers) ? exam.__answers : []).forEach(function (k, i) { if (k && typeof k === 'object') keys[k.i != null ? k.i : i] = k; });
        var qs = (Array.isArray(exam.questions) ? exam.questions : []).map(function (q, i) { return window.SiteBuilder.decodeQuestion(q, keys[i]); });
        if (!qs.length) throw new Error('برای نسخهٔ چاپی سؤالی در این آزمون پیدا نشد.');
        var rec = {id: S.uuid(), title: exam.title || x.title || '', subject: exam.subject || x.subject || '', duration: exam.duration != null ? String(exam.duration) : '', questions: qs, sourceExamId: x.id};
        var pr = await window.SiteBuilder.printExamSave(rec, {silent: true}); /* تصاویر آنلاین قبلاً URL هستند → هزینهٔ تازه‌ای ندارد */
        if (!pr) { status.style.display = 'none'; return; }
        go('builder', {mode: 'print', printId: rec.id});
      } catch (e) { status.style.color = 'var(--m-danger)'; status.textContent = S.errMsg(e); }
    }
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
    var hdrBtn = btns.filter(function (b) { return /تنظیمات سربرگ/.test(b.textContent); })[0];
    var addRow = c.querySelector('.b-add');
    var isPrint = /چاپی/.test(top.textContent);
    /* دکمهٔ «مشخصات آزمون» تمام‌عرض زیر فیلدها (اپ: OutlinedButton fillMaxWidth) */
    if (setBtn && !top.querySelector('.m-settings-btn')) { var sb = el('button', {class: 'm-outline m-settings-btn', text: 'مشخصات آزمون', onclick: function () { setBtn.click(); }}); top.appendChild(sb); }
    /* V157 — حالت چاپ: OutlinedButton تمام‌عرض «تنظیمات سربرگ» (ExamBuilderScreen printMode) */
    if (hdrBtn && !top.querySelector('.m-settings-btn')) { var hb = el('button', {class: 'm-outline m-settings-btn', text: 'تنظیمات سربرگ', onclick: function () { hdrBtn.click(); }}); top.appendChild(hb); }
    var radialOpen = false;
    var bar = el('div', {class: 'm-bfab', id: 'm-bfab'});
    var save = el('button', {class: 'm-fab save', 'aria-label': 'ذخیره آزمون', onclick: function () { if (saveBtn) saveBtn.click(); }}, [ic('grading')]);
    var plus = el('button', {class: 'm-fab add', 'aria-label': 'افزودن سؤال', onclick: function () { radialOpen = !radialOpen; drawRadial(); }}, [el('span', {class: 'm-fab-plus', text: '+'})]);
    /* V156 — مثل ExamBuilderScreen: در حالت چاپ FAB پیش‌نمایش (چشم) + FAB چاپ (منوی «چاپ آزمون (دانش‌آموز)» / «چاپ با کلید (پاسخ‌نامه)»)؛ آنلاین فقط ✓ و + */
    var prev = isPrint && prevBtn ? el('button', {class: 'm-fab prev', 'aria-label': 'پیش‌نمایش آزمون', onclick: function () { prevBtn.click(); }}, [ic('eye')]) : null;
    var printFab = isPrint && prevBtn ? el('button', {class: 'm-fab print', 'aria-label': 'چاپ آزمون', onclick: function () {
      var bg = el('div', {class: 'm-sheet-bg', onclick: function (e) { if (e.target === e.currentTarget) bg.remove(); }});
      var body = el('div', {class: 'm-sheet'}, [el('h3', {text: 'چاپ آزمون'})]);
      [['student', 'چاپ آزمون (دانش‌آموز)', 'print'], ['teacher', 'چاپ با کلید (پاسخ‌نامه)', 'grading']].forEach(function (o) {
        body.appendChild(el('button', {class: 'm-row neo', onclick: function () { bg.remove(); if (window.__builderPreview) window.__builderPreview(o[0]); else prevBtn.click(); }}, [ic(o[2], 'm-row-ic'), el('div', {}, [el('b', {text: o[1]})])]));
      });
      bg.appendChild(body); document.body.appendChild(bg);
    }}, [ic('print')]) : null;
    bar.appendChild(save); if (prev) bar.appendChild(prev); if (printFab) bar.appendChild(printFab); bar.appendChild(plus);
    var radial = null;
    function drawRadial() {
      if (radial) { radial.remove(); radial = null; }
      plus.classList.toggle('on', radialOpen); bar.classList.toggle('radial-open', radialOpen);
      if (!radialOpen) return;
      radial = el('div', {class: 'm-radial-bg', onclick: function (e) { if (e.target === e.currentTarget) { radialOpen = false; drawRadial(); } }});
      /* V153 — مثل BuilderRadialMenuOverlay: دایرهٔ کامل وسط صفحه، شعاع ۳۱٪ عرض (۱۰۴..۱۳۸)، حلقهٔ خط‌چین، ۸ مربع گوشه‌گرد ۶۶dp از ساعت ۱۲ هر ۴۵°، دکمهٔ ✕ گرادیانی→قرمز در مرکز */
      var R = Math.max(104, Math.min(138, window.innerWidth * 0.31));
      var ring = el('div', {class: 'm-radial', style: '--r:' + R + 'px'});
      ring.appendChild(el('span', {class: 'm-radial-ring'}));
      ring.appendChild(el('button', {class: 'm-radial-x', 'aria-label': 'بستن', onclick: function () { radialOpen = false; drawRadial(); }}, [el('span', {class: 'm-fab-plus', text: '+'})]));
      RADIAL.forEach(function (r, i) {
        var ang = (-90 + i * 45) * Math.PI / 180;
        var x = Math.cos(ang) * R, y = Math.sin(ang) * R;
        ring.appendChild(el('button', {class: 'm-radial-item', style: 'background:' + PASTEL[r[0]] + ';--tx:' + x.toFixed(0) + 'px;--ty:' + y.toFixed(0) + 'px;animation-delay:' + (i * 30) + 'ms', onclick: function () {
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
    /* V155 — سؤال‌ها مثل کارت‌های QuestionEditor اپ: کارت تمام‌عرض با رنگ پاستلی نوع، ویرایشگر همان سؤال درست زیر کارت انتخاب‌شده (بازشونده) */
    var list = c.querySelector('.b-list'), edRef = c.querySelector('.b-editor');
    function typeOf(row) { var ch = row.querySelector('.chip:not(.brand)'); var lab = ch ? ch.textContent.trim() : ''; return Object.keys(PASTEL).filter(function (k) { return typeLabel(k) === lab; })[0] || 'essay'; }
    function tint(node, t) { var h = PASTEL[t] || '#FFFFFF', r = parseInt(h.slice(1, 3), 16), g = parseInt(h.slice(3, 5), 16), b = parseInt(h.slice(5, 7), 16); node.style.background = 'linear-gradient(rgba(' + r + ',' + g + ',' + b + ',.38),rgba(' + r + ',' + g + ',' + b + ',.38)),var(--m-bg)'; }
    function placeEditor() {
      if (!list || !edRef) return;
      var rows = list.querySelectorAll('.b-q'), on = null;
      rows.forEach(function (r) { var t = typeOf(r); r.setAttribute('data-t', t); tint(r, t); if (r.classList.contains('on')) on = r; });
      if (on) { if (on.nextSibling !== edRef) on.after(edRef); tint(edRef, on.getAttribute('data-t')); edRef.classList.add('m-open'); }
      else if (!edRef.parentNode || edRef.parentNode === list || edRef.closest('.b-qs')) { list.after(edRef); edRef.style.background = ''; edRef.classList.remove('m-open'); }
    }
    placeEditor();
    if (list && window.MutationObserver) new MutationObserver(function () { placeEditor(); }).observe(list, {childList: true});
  }
  function typeLabel(t) { return {multiple: 'چندگزینه‌ای', truefalse: 'صحیح / غلط', fill: 'جای‌خالی', numeric: 'عددی', matching: 'جورکردنی', essay: 'تشریحی'}[t]; }

  /* ---------- V157: «دانش‌آموزان» در گوشی مثل StudentsContent اپ (SchoolManagementScreen.kt ~964–1093) ----------
     نوار وسط‌چین: Excel (OutlinedButton) / + / 🔍 / فیلتر (قرمز وقتی فعال)؛ جست‌وجو بازشونده با ✕؛ StudentCard بازشونده با
     ردیف آیکن‌ها (فعال/غیرفعال، ویرایش، افزودن به کلاس، کپی، حذف، چشم اشتراک با مدیر)؛ StudentFilterDialog با کارت‌های بخش
     (پایه/کلاس/جنسیت/مدرسه/عضو نشده) و سطر «حذف فیلترها / اعمال فیلتر / ✕»؛ BulkStudentDialog با چیپ‌های شماره؛
     StudentExportColumnsDialog («اطلاعات ورودی اکسل»). */
  var STUDENT_EXPORT_COLUMNS = [['نام', function (s) { return s.full_name || ''; }], ['نام کاربری', function (s) { return s.username || ''; }], ['جنسیت', function (s) { return s.gender === 'female' ? 'دختر' : (s.gender === 'male' ? 'پسر' : ''); }], ['پایه', function (s) { return s.grade || ''; }], ['رشته', function (s) { return s.field_of_study || ''; }], ['نام پدر', function (s) { return s.father_name || ''; }], ['کلاس', function (s) { return s.class_names || ''; }], ['وضعیت', function (s) { return s.is_active !== false ? 'فعال' : 'غیرفعال'; }]];
  var GRADES_M = ['اول', 'دوم', 'سوم', 'چهارم', 'پنجم', 'ششم', 'هفتم', 'هشتم', 'نهم', 'دهم', 'یازدهم', 'دوازدهم'];
  var FIELDS_M = ['ریاضی', 'تجربی', 'انسانی', 'فنی و حرفه‌ای', 'کاردانش', 'معارف', 'هنر', 'عمومی'];
  function sheet(children, cls) { var bg = el('div', {class: 'm-sheet-bg m-center' + (cls ? ' ' + cls : ''), onclick: function (e) { if (e.target === e.currentTarget) bg.remove(); }}); var body = el('div', {class: 'm-sheet m-dlg'}, children); bg.appendChild(body); document.body.appendChild(bg); return bg; }
  function chip(label, on, onclick) { return el('button', {class: 'm-fchip' + (on ? ' on' : ''), text: label, onclick: onclick}); }
  function filterActive(f) { return !!(f.grade || f.classId || f.gender || f.unassigned || f.schoolId); }
  /* آینهٔ applyStudentFilter */
  function applyFilter(list, f, classes, meta) {
    if (!filterActive(f)) return list;
    var cn = f.classId ? (classes.filter(function (k) { return k.id === f.classId; })[0] || {}).name : null;
    return list.filter(function (s) { var m = meta[s.id] || {}; return (!f.grade || (s.grade || '').trim() === f.grade) && (!f.gender || (s.gender || '').toLowerCase() === f.gender) && (!cn || String(s.class_names || '').indexOf(cn) >= 0) && (!f.unassigned || !String(s.class_names || '').trim()) && (!f.schoolId || (m.schools || []).indexOf(f.schoolId) >= 0); });
  }
  function studentFilterDialog(filter, classes, schools, meta, onApply) {
    var draft = Object.assign({}, filter), open = null, bg;
    var body = el('div');
    function section(key, title, active, value, content) {
      var card = el('div', {class: 'm-fsec neo'});
      card.appendChild(el('button', {class: 'm-fsec-h', onclick: function () { open = open === key ? null : key; draw(); }}, [el('span', {class: 'm-fsec-t'}, [ic('filter', active ? 'red' : ''), el('b', {text: title})]), el('span', {class: 'muted', text: value})]));
      if (open === key) card.appendChild(el('div', {class: 'm-fsec-b'}, content()));
      return card;
    }
    function draw() {
      body.innerHTML = '';
      body.appendChild(el('div', {class: 'm-fbar'}, [
        el('button', {class: 'm-textbtn', text: 'حذف فیلترها', onclick: function () { draft = {}; draw(); }}),
        el('button', {class: 'btn m-btn', text: 'اعمال فیلتر', onclick: function () { bg.remove(); onApply(draft); }}),
        el('button', {class: 'm-iconbtn red', 'aria-label': 'انصراف', onclick: function () { bg.remove(); }}, [ic('close')])
      ]));
      body.appendChild(section('grade', 'پایه', !!draft.grade, draft.grade || 'همه', function () { return [el('div', {class: 'm-chips'}, [chip('همه پایه‌ها', !draft.grade, function () { draft.grade = null; draw(); })].concat(GRADES_M.map(function (g) { return chip(g, draft.grade === g, function () { draft.grade = g; draw(); }); })))]; }));
      body.appendChild(section('class', 'کلاس', !!draft.classId, (classes.filter(function (k) { return k.id === draft.classId; })[0] || {}).name || 'همه', function () { return classes.length ? classes.map(function (k) { return chip(k.name, draft.classId === k.id, function () { draft.classId = draft.classId === k.id ? null : k.id; draw(); }); }) : [el('p', {class: 'muted', text: 'کلاسی نیست.'})]; }));
      body.appendChild(section('gender', 'جنسیت', !!draft.gender, draft.gender === 'female' ? 'دختر' : (draft.gender === 'male' ? 'پسر' : 'همه'), function () { return [el('div', {class: 'm-chips'}, [chip('دختر', draft.gender === 'female', function () { draft.gender = draft.gender === 'female' ? null : 'female'; draw(); }), chip('پسر', draft.gender === 'male', function () { draft.gender = draft.gender === 'male' ? null : 'male'; draw(); })])]; }));
      body.appendChild(section('school', 'مدرسه', !!draft.schoolId, (schools.filter(function (x) { return x.id === draft.schoolId; })[0] || {}).name || 'همه', function () { return schools.length ? schools.map(function (x) { return chip(x.name || 'مدرسه', draft.schoolId === x.id, function () { draft.schoolId = draft.schoolId === x.id ? null : x.id; draw(); }); }) : [el('p', {class: 'muted', text: 'مدرسه‌ای یافت نشد.'})]; }));
      body.appendChild(section('unassigned', 'عضو نشده', !!draft.unassigned, draft.unassigned ? 'فعال' : 'خیر', function () { return [chip('فقط دانش‌آموزانی که عضو هیچ کلاسی نیستند', !!draft.unassigned, function () { draft.unassigned = !draft.unassigned; draw(); })]; }));
    }
    draw(); bg = sheet([body]); return bg;
  }
  function exportColumnsDialog(students, onExport) {
    var sel = {}; STUDENT_EXPORT_COLUMNS.forEach(function (c) { sel[c[0]] = true; });
    var rows = STUDENT_EXPORT_COLUMNS.map(function (c) { var cb = el('input', {type: 'checkbox'}); cb.checked = true; cb.addEventListener('change', function () { sel[c[0]] = cb.checked; }); return el('label', {class: 'm-check'}, [cb, el('span', {text: c[0]})]); });
    var bg = sheet([el('h3', {text: 'اطلاعات ورودی اکسل'}), el('p', {text: 'تعداد دانش‌آموزان گروه انتخابی: ' + fa(students.length)}), el('div', {class: 'm-checks'}, rows),
      el('p', {class: 'muted', style: 'font-size:12px', text: 'رمز حساب‌ها روی سرور نگهداری نمی‌شود و در خروجی سایت قرار نمی‌گیرد.'}),
      el('div', {class: 'row', style: 'gap:8px;margin-top:8px'}, [el('button', {class: 'btn m-btn grow', text: 'ذخیره Excel', onclick: function () { bg.remove(); onExport(STUDENT_EXPORT_COLUMNS.filter(function (c) { return sel[c[0]]; })); }}), el('button', {class: 'm-textbtn', text: 'انصراف', onclick: function () { bg.remove(); }})])]);
  }
  function studentCardM(s, classes, refresh) {
    var open = false, canManage = s.can_manage !== false;
    var card = el('div', {class: 'm-rowcard neo m-stcard'});
    function draw() {
      card.innerHTML = '';
      var gradeField = [s.grade, s.field_of_study].filter(function (x) { return x && String(x).trim(); }).join(' ') || '—';
      card.appendChild(el('button', {class: 'm-stcard-h', onclick: function () { open = !open; draw(); }}, [el('b', {text: s.full_name || 'بدون نام'}), el('span', {text: gradeField})]));
      if (!open) return;
      var b = el('div', {class: 'm-stcard-b'});
      b.appendChild(el('div', {class: 'm-stcard-kv'}, [el('span', {text: 'نام پدر: ' + (s.father_name || '—')}), el('span', {text: 'نام کاربری: ' + (s.username || '—')})]));
      if (s.class_names) b.appendChild(el('div', {text: 'کلاس‌ها: ' + s.class_names}));
      var acts = el('div', {class: 'm-stcard-acts'});
      var on = s.is_active !== false;
      acts.appendChild(el('button', {class: 'm-iconbtn' + (on ? ' ok' : ''), 'aria-label': on ? 'فعال؛ لمس برای غیرفعال' : 'غیرفعال؛ لمس برای فعال', onclick: async function () { try { chk(await S.rpcObj('set_student_active', {p_student: s.id, p_active: !on})); refresh(); } catch (e) { toast(S.errMsg(e), 'err'); } }}, [ic(on ? 'toggleon' : 'toggleoff')]));
      if (canManage) acts.appendChild(el('button', {class: 'm-iconbtn', 'aria-label': 'ویرایش دانش‌آموز', onclick: function () { window.SiteSchool.studentForm(s, classes, null, refresh); }}, [ic('edit')]));
      acts.appendChild(el('button', {class: 'm-iconbtn', 'aria-label': 'افزودن به کلاس‌ها', onclick: function () { window.SiteSchool.classPickDlg(s, classes, refresh); }}, [ic('plus')]));
      acts.appendChild(el('button', {class: 'm-iconbtn', 'aria-label': 'کپی اطلاعات دانش‌آموز', onclick: function () { var t = 'نام: ' + (s.full_name || '') + '\nنام کاربری: ' + (s.username || '') + (s.father_name ? '\nنام پدر: ' + s.father_name : '') + (s.grade ? '\nپایه: ' + s.grade : '') + (s.class_names ? '\nکلاس‌ها: ' + s.class_names : ''); try { navigator.clipboard.writeText(t); toast('کپی شد.', 'ok'); } catch (e) { toast('کپی نشد.', 'err'); } }}, [ic('copy')]));
      if (canManage) acts.appendChild(el('button', {class: 'm-iconbtn red', 'aria-label': 'حذف حساب دانش‌آموز', onclick: async function () { if (!(await S.confirmDlg('حذف دانش‌آموز', 'حساب «' + S.esc(s.full_name || '') + '» و پاسخ‌هایش برای همیشه حذف می‌شود.', 'حذف کامل', true))) return; try { await window.SiteSchool.manageStudent({action: 'delete', id: s.id}); toast('حذف شد.', 'ok'); refresh(); } catch (e) { toast(S.errMsg(e), 'err'); } }}, [ic('trash')]));
      acts.appendChild(el('button', {class: 'm-iconbtn', 'aria-label': s.shared_with_manager ? 'اشتراک با مدیر فعال؛ لمس برای برداشتن' : 'اشتراک با مدیر', onclick: async function () { try { var r = chk(await S.rpcObj('native_teacher_share_student_v136', {p_student: s.id, p_share: !s.shared_with_manager})); var eff = r.shared != null ? String(r.shared) === 'true' : !s.shared_with_manager; toast(eff ? 'با مدیر به اشتراک گذاشته شد.' : 'اشتراک برداشته شد.', 'ok'); refresh(); } catch (e) { toast(S.errMsg(e), 'err'); } }}, [ic(s.shared_with_manager ? 'eye' : 'eyeoff')]));
      b.appendChild(acts); card.appendChild(b);
    }
    draw(); return card;
  }
  function chk(r) { if (r && typeof r === 'object' && r.error) throw new Error(String(r.error)); return r || {}; }
  var stUi = {query: '', searchOpen: false, filter: {}};
  async function studentsScreen(c) {
    S.loading(c);
    try {
      var r = await Promise.all([S.rpc('my_students', {}), S.rpc('native_my_classes_v28', {}), S.rpcObj('native_teacher_schools_v61', {}).catch(function () { return {}; }), S.rpcObj('native_student_filter_meta_v61', {}).catch(function () { return {}; })]);
      var list = r[0] || [], classes = r[1] || [], schools = (r[2] && r[2].items) || [], meta = {};
      ((r[3] && r[3].items) || []).forEach(function (m) { if (m && m.id) meta[m.id] = {schools: m.schools || [], teacherId: m.teacher_id || ''}; });
      c.innerHTML = '';
      function refresh() { studentsScreen(c); }
      var listBox = el('div', {class: 'm-rows'});
      var searchWrap = el('div', {class: 'm-stsearch'});
      var q = el('input', {type: 'search', value: stUi.query, placeholder: 'جست‌وجوی نام، نام کاربری، پایه یا پدر'});
      q.addEventListener('input', function () { stUi.query = q.value; draw(); });
      searchWrap.appendChild(el('div', {class: 'field'}, [q, el('button', {class: 'm-iconbtn x', 'aria-label': 'بستن جست‌وجو', onclick: function () { stUi.query = ''; stUi.searchOpen = false; drawBar(); draw(); }}, [ic('close')])]));
      var bar = el('div', {class: 'm-sttools'});
      function filtered() { var s = stUi.query.trim().toLowerCase(); return applyFilter(list, stUi.filter, classes, meta).filter(function (x) { return !s || [x.full_name, x.username, x.grade, x.father_name].join(' ').toLowerCase().indexOf(s) >= 0; }); }
      function drawBar() {
        bar.innerHTML = '';
        bar.appendChild(el('button', {class: 'm-outline', text: 'Excel', onclick: function () {
          studentFilterDialog({}, classes, schools, meta, function (f) {
            var st = applyFilter(list, f, classes, meta);
            exportColumnsDialog(st, function (cols) { var rows = [cols.map(function (c) { return c[0]; })].concat(st.map(function (s) { return cols.map(function (c) { return c[1](s); }); })); window.SiteExtras.download('students.xlsx', window.SiteExtras.xlsx([{name: 'دانش‌آموزان', rows: rows}]), 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'); });
          });
        }}));
        /* V163 — «افزودن گروهی» از سایت گوشی حذف شد (درخواست کاربر)؛ + = دانش‌آموز جدید */
        bar.appendChild(el('button', {class: 'm-iconbtn big', 'aria-label': 'دانش‌آموز جدید', text: '+', onclick: function () { window.SiteSchool.studentForm(null, classes, null, refresh); }}));
        if (!stUi.searchOpen) bar.appendChild(el('button', {class: 'm-iconbtn', 'aria-label': 'جست‌وجوی دانش‌آموز', onclick: function () { stUi.searchOpen = true; drawBar(); searchWrap.style.display = ''; q.focus(); }}, [ic('search')]));
        bar.appendChild(el('button', {class: 'm-iconbtn' + (filterActive(stUi.filter) ? ' red' : ''), 'aria-label': 'فیلتر دانش‌آموزان', onclick: function () { studentFilterDialog(stUi.filter, classes, schools, meta, function (f) { stUi.filter = f; drawBar(); draw(); }); }}, [ic('filter')]));
        searchWrap.style.display = stUi.searchOpen ? '' : 'none';
      }
      function draw() { var f = filtered(); listBox.innerHTML = ''; if (!f.length) listBox.appendChild(el('p', {class: 'muted', text: 'دانش‌آموزی یافت نشد.'})); f.forEach(function (s) { listBox.appendChild(studentCardM(s, classes, refresh)); }); }
      c.appendChild(bar); c.appendChild(searchWrap); c.appendChild(listBox); drawBar(); draw();
    } catch (e) { S.showErr(c, e); }
  }


  /* ---------- V158: پنجره‌های «افزودن سریع» مثل SchoolManagementScreen ---------- */
  function dlgBtn(text, cls, on, dis) { return el('button', {class: cls, text: text, disabled: dis ? 'disabled' : null, onclick: on}); }
  /* آینهٔ AlertDialog «عضویت در مدرسه جدید» (SchoolManagementScreen.kt:416) */
  function joinSchoolDialog() {
    var code = el('input', {type: 'text', style: 'direction:ltr;text-transform:uppercase', maxlength: '6', autocomplete: 'off'});
    var msg = el('p', {class: 'm-err'}); var bg;
    var ok = dlgBtn('عضویت', 'btn m-btn', async function () {
      var cd = code.value.trim().toUpperCase(); if (cd.length !== 6) return;
      ok.disabled = true; ok.textContent = 'در حال عضویت...'; msg.textContent = '';
      try { var r = chk(await S.rpcObj('native_join_school_v39', {p_code: cd})); bg.remove(); toast('به مدرسهٔ «' + (r.school_name || '') + '» پیوستید.', 'ok'); if (view.panel === 'profile' || view.panel === 'classes') paint(); }
      catch (e) { msg.textContent = S.errMsg(e) || 'عضویت ناموفق بود.'; ok.disabled = false; ok.textContent = 'عضویت'; }
    }, true);
    code.addEventListener('input', function () { code.value = code.value.toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 6); ok.disabled = code.value.length !== 6; msg.textContent = ''; });
    bg = sheet([el('h3', {text: 'عضویت در مدرسه جدید'}), el('p', {text: 'کد دعوت ۶ حرفی مدیر مدرسه را وارد کنید.'}), el('div', {class: 'field'}, [el('label', {text: 'کد دعوت مدرسه'}), code]), msg,
      el('div', {class: 'row m-dlg-actions'}, [ok, dlgBtn('انصراف', 'm-textbtn', function () { bg.remove(); })])]);
    setTimeout(function () { code.focus(); }, 50);
  }
  /* آینهٔ AlertDialog «ساخت مدرسه جدید» (SchoolManagementScreen.kt:372) */
  function createSchoolDialog() {
    var name = el('input', {type: 'text', maxlength: '160'}), prov = el('input', {type: 'text', maxlength: '100'}), city = el('input', {type: 'text', maxlength: '100'});
    var msg = el('p', {class: 'm-err'}); var bg;
    var ok = dlgBtn('ساخت مدرسه', 'btn m-btn', async function () {
      ok.disabled = true; msg.textContent = '';
      try { chk(await S.rpcObj('native_manager_create_school_v61', {p_name: name.value.trim(), p_province: prov.value.trim(), p_city: city.value.trim()})); bg.remove(); toast('مدرسه ساخته شد.', 'ok'); go('school'); }
      catch (e) { msg.textContent = S.errMsg(e); ok.disabled = false; }
    }, true);
    name.addEventListener('input', function () { ok.disabled = name.value.trim().length < 2; });
    bg = sheet([el('h3', {text: 'ساخت مدرسه جدید'}), el('div', {class: 'field'}, [el('label', {text: 'نام مدرسه'}), name]),
      el('div', {class: 'grid2 m-grid2'}, [el('div', {class: 'field'}, [el('label', {text: 'استان'}), prov]), el('div', {class: 'field'}, [el('label', {text: 'شهر'}), city])]), msg,
      el('div', {class: 'row m-dlg-actions'}, [ok, dlgBtn('انصراف', 'm-textbtn', function () { bg.remove(); })])]);
    setTimeout(function () { name.focus(); }, 50);
  }
  /* آینهٔ AlertDialog «انتخاب معلم و کلاس» مدیر (SchoolManagementScreen.kt:590) → سپس BulkStudentDialog */
  async function managerStudentPicker() {
    var teachers = [], classes = [], tId = null, cId = null, bg;
    try { teachers = (chk(await S.rpcObj('native_manager_teachers_v37', {})).items || []); } catch (e) { teachers = []; }
    var body = el('div');
    function draw() {
      body.innerHTML = '';
      body.appendChild(el('p', {text: 'دانش‌آموز جدید به کلاس کدام معلم اضافه شود؟'}));
      if (!teachers.length) body.appendChild(el('p', {class: 'muted', text: 'معلمی در مدرسه یافت نشد.'}));
      body.appendChild(el('div', {class: 'm-chips'}, teachers.map(function (t) { return chip(t.full_name || 'معلم', tId === t.id, async function () { tId = t.id; cId = null; classes = []; draw(); try { classes = chk(await S.rpcObj('native_manager_teacher_classes_v40c', {p_teacher: t.id})).items || []; } catch (e) { classes = []; } draw(); }); })));
      if (tId) {
        body.appendChild(el('p', {text: 'کلاس معلم:'}));
        if (!classes.length) body.appendChild(el('p', {class: 'muted', text: 'کلاس قابل مشاهده‌ای ندارد.'}));
        body.appendChild(el('div', {class: 'm-chips'}, classes.map(function (k) { return chip(k.name || 'کلاس', cId === k.id, function () { cId = cId === k.id ? null : k.id; draw(); }); })));
      }
      body.appendChild(el('div', {class: 'row m-dlg-actions'}, [
        dlgBtn(cId ? 'ادامه و ساخت دانش‌آموز' : 'ساخت بدون کلاس', 'btn m-btn', function () { var target = cId; bg.remove(); window.SiteSchool.studentForm(null, [], null, function () { if (view.panel === 'school') paint(); }, target ? function (id) { return S.rpcObj('native_manager_set_class_student_v40c', {p_class: target, p_student: id, p_add: true}).catch(function () {}); } : null); }),
        dlgBtn('انصراف', 'm-textbtn', function () { bg.remove(); })
      ]));
    }
    draw(); bg = sheet([el('h3', {text: 'انتخاب معلم و کلاس'}), body]);
  }

  function cleanupBuilder() { ['m-bfab'].forEach(function (id) { var n = document.getElementById(id); if (n) n.remove(); }); var r = document.querySelector('.m-radial-bg'); if (r) r.remove(); }


  /* ---------- V154: ورود/ثبت‌نام به سبک SignInScreen اپ (پوستهٔ یخی AuthIceComponents) ---------- */
  function authActive() { return MQ.matches && !S.user(); }
  var A = {screen: 'landing', tab: 1, regTab: 0, email: '', step: 'form', fullName: '', username: null};
  function paintAuth(root) {
    var recovery = A.screen === 'recovery' || (A.screen === 'otp' && A.otpKind === 'recovery');
    var shell = el('div', {class: 'ice' + (recovery ? ' ice-snow' : '')}, [el('div', {class: 'ice-bg'}, [el('div', {class: 'ice-disc'}), el('div', {class: 'ice-waves'}, [el('i'), el('i'), el('i')])])]);
    if (recovery) { var sn = el('div', {class: 'ice-snowfall'}); for (var i = 0; i < 26; i++) sn.appendChild(el('b', {style: '--x:' + Math.random() * 100 + '%;--d:' + (7 + Math.random() * 9) + 's;--s:' + (2 + Math.random() * 4) + 'px;--o:' + (-Math.random() * 12) + 's'})); shell.appendChild(sn); }
    var wrap = el('div', {class: 'ice-wrap'});
    shell.appendChild(wrap);
    if (!S.auth.keyReady) wrap.appendChild(el('div', {class: 'ice-err', text: 'کلید اتصال (SUPABASE_ANON_KEY) در فایل سایت وارد نشده؛ ورود ممکن نیست.'}));
    var msg = el('div', {class: 'ice-err', style: 'display:none'});
    function setMsg(t) { msg.style.display = t ? '' : 'none'; msg.textContent = t ? 'خطا: ' + t : ''; }
    function busy(b, on) { b.disabled = on; b.classList.toggle('loading', !!on); }
    if (A.screen === 'landing') wrap.appendChild(landing());
    else { var card = el('div', {class: 'ice-card'}); wrap.appendChild(card); ({login: loginPane, register: registerPane, otp: otpPane, setup: setupPane, recovery: recoveryPane})[A.screen](card); }
    wrap.appendChild(msg);
    root.appendChild(shell);

    function goA(scr, patch) { A.screen = scr; Object.keys(patch || {}).forEach(function (k) { A[k] = patch[k]; }); S.render(); }
    function stag(i, node) { if (node) node.style.animationDelay = (i * 55) + 'ms'; if (node) node.classList.add('ice-in'); return node; }
    function field(hint, opts) { opts = opts || {}; var inp = el('input', {type: opts.type || 'text', placeholder: hint, value: opts.value || '', autocomplete: opts.type === 'password' ? 'current-password' : 'on', inputmode: opts.type === 'email' ? 'email' : undefined}); if (opts.ltr) inp.style.direction = 'ltr'; var w = el('div', {class: 'ice-field'}, [inp]); if (opts.type === 'password') { var eye = el('button', {type: 'button', class: 'ice-eye', 'aria-label': 'نمایش رمز', onclick: function () { inp.type = inp.type === 'password' ? 'text' : 'password'; eye.classList.toggle('on'); }}, [ic('eye')]); w.appendChild(eye); } var box = el('div', {class: 'ice-fw'}, [w, opts.supporting ? el('div', {class: 'ice-sup', text: opts.supporting}) : null]); box.input = inp; return box; }
    function btn(text, on, cls) { var b = el('button', {class: 'ice-btn ' + (cls || ''), text: text}); b.addEventListener('click', function () { on(b); }); return b; }
    function outline(text, on) { return btn(text, on, 'outline'); }
    function link(text, on, gray) { return el('button', {class: 'ice-link' + (gray ? ' gray' : ''), text: text, onclick: on}); }
    function brand() { return el('div', {class: 'ice-brand', text: 'آزمون آنلاین'}); }
    function h(t) { return el('h2', {class: 'ice-h', text: t}); }
    function tabs(labels, sel, on) { var t = el('div', {class: 'ice-tabs', style: '--n:' + labels.length + ';--i:' + sel}, [el('span', {class: 'ice-tab-pill'})].concat(labels.map(function (l, i) { return el('button', {class: i === sel ? 'on' : '', text: l, onclick: function () { on(i); }}); }))); return t; }
    function header(title, sub) { return el('div', {class: 'ice-head'}, [el('span', {class: 'ice-head-ic'}, [ic('lock')]), el('h2', {class: 'ice-h', text: title}), sub ? el('p', {class: 'ice-sub', text: sub}) : null]); }
    function steps(cur) { var names = ['ایمیل', 'کد بازیابی', 'رمز جدید']; return el('div', {class: 'ice-steps'}, names.map(function (n, i) { return el('div', {class: 'ice-step' + (i < cur ? ' done' : i === cur ? ' cur' : '')}, [el('span', {class: 'd', text: i < cur ? '✓' : fa(i + 1)}), el('small', {text: n})]); })); }
    function google(role, label) { if (!window.SiteExtras) return null; var g = window.SiteExtras.googleButton(role); g.className = 'ice-btn outline ice-google'; g.style.marginTop = ''; var t = g.childNodes[g.childNodes.length - 1]; if (t && t.nodeType === 3) t.textContent = ' ' + label; return g; }
    function otpBoxes(onChange) { var n = 6, hidden = el('input', {type: 'tel', inputmode: 'numeric', autocomplete: 'one-time-code', maxlength: 8, class: 'ice-otp-hidden'}); var row = el('div', {class: 'ice-otp', onclick: function () { hidden.focus(); }}); function draw() { var v = hidden.value.replace(/\D/g, '').slice(0, 8); hidden.value = v; var count = Math.max(6, Math.min(8, v.length + (v.length >= 6 ? 1 : 0))); row.innerHTML = ''; for (var i = 0; i < count; i++) row.appendChild(el('span', {class: 'ice-otp-b' + (i === v.length ? ' focus' : '') + (v[i] ? ' filled' : ''), text: v[i] ? fa(v[i]) : ''})); onChange(v); } hidden.addEventListener('input', draw); draw(); var w = el('div', {}, [hidden, row]); w.value = function () { return hidden.value; }; w.focus = function () { hidden.focus(); }; return w; }

    function landing() {
      return el('div', {class: 'ice-landing'}, [
        stag(0, el('div', {class: 'ice-hero'}, [ic('school')])),
        stag(1, el('div', {class: 'ice-title', text: 'آزمون آنلاین'})),
        stag(2, el('div', {class: 'ice-sub', text: 'به سامانهٔ آزمون و ارزشیابی خوش آمدید'})),
        stag(3, btn('ورود به حساب', function () { goA('login', {tab: 1}); })),
        stag(4, outline('ساخت حساب جدید', function () { goA('register', {regTab: 0, step: 'form'}); })),
        stag(5, el('p', {class: 'ice-note', text: 'حساب دانش‌آموز را معلم می‌سازد؛ نام کاربری و رمز را از معلم خود دریافت کنید.'}))
      ]);
    }
    function loginPane(card) {
      card.appendChild(stag(0, brand()));
      card.appendChild(stag(1, tabs(['مدیر/معاون', 'معلم', 'دانش‌آموز'], A.tab, function (i) { goA('login', {tab: i}); })));
      var student = A.tab === 2, manager = A.tab === 0;
      card.appendChild(stag(2, h(student ? 'ورود دانش‌آموز' : manager ? 'ورود مدیر/معاون' : 'ورود معلم')));
      var id = field(student ? 'نام کاربری دانش‌آموز' : manager ? 'ایمیل یا نام کاربری مدیر/معاون' : 'ایمیل یا نام کاربری معلم', {type: student ? 'text' : 'email', ltr: true, supporting: student ? 'همان نام کاربری تحویلی از معلم را وارد کنید.' : 'می‌توانید به‌جای ایمیل، نام کاربری انگلیسی که هنگام ثبت‌نام انتخاب کردید را وارد کنید.'});
      var pw = field('رمز عبور', {type: 'password', ltr: true});
      var go = btn(student ? 'ورود' : 'ورود با رمز عبور', async function (b) { setMsg(''); busy(b, true); try { var u = await api.signInWithPassword(id.input.value, pw.input.value); S.auth.login(u); } catch (e) { setMsg(S.errMsg(e)); } busy(b, false); });
      function chk() { go.disabled = !(id.input.value.trim() && pw.input.value); } id.input.addEventListener('input', chk); pw.input.addEventListener('input', chk); chk();
      pw.input.addEventListener('keydown', function (e) { if (e.key === 'Enter' && !go.disabled) go.click(); });
      card.appendChild(stag(3, id)); card.appendChild(stag(4, pw)); card.appendChild(stag(5, go));
      if (!student) {
        var role = manager ? 'manager' : 'teacher';
        var otp = outline('ورود با کد ایمیل', async function (b) { setMsg(''); busy(b, true); try { A.email = S.auth.requireEmail(id.input.value); await api.sendLoginOtp(A.email); goA('otp', {otpKind: 'login'}); } catch (e) { setMsg(S.errMsg(e)); busy(b, false); } });
        function chk2() { otp.disabled = id.input.value.indexOf('@') < 0; } id.input.addEventListener('input', chk2); chk2();
        card.appendChild(stag(6, otp));
        card.appendChild(stag(7, google(role, 'ورود با گوگل')));
        card.appendChild(stag(8, link('رمز را فراموش کرده‌ام', function () { goA('recovery', {step: 'email', email: ''}); })));
      }
      card.appendChild(stag(9, link('بازگشت', function () { goA('landing'); }, true)));
    }
    function registerPane(card) {
      var manager = A.regTab === 1, role = manager ? 'manager' : 'teacher';
      card.appendChild(stag(0, brand()));
      card.appendChild(stag(1, tabs(['معلم', 'مدیر/معاون'], A.regTab, function (i) { goA('register', {regTab: i, step: 'form'}); })));
      card.appendChild(stag(2, h(manager ? 'ثبت‌نام مدیر/معاون' : 'ثبت‌نام معلم')));
      card.appendChild(stag(3, el('p', {class: 'ice-sub right', text: manager ? 'مدیر یا معاون مدرسه با ایمیل ثبت‌نام می‌کند و پس از تأیید، مدرسهٔ خود را می‌سازد.' : 'دانش‌آموز نباید از این بخش ثبت‌نام کند؛ حساب دانش‌آموز را معلم می‌سازد.'})));
      var name = field('نام و نام خانوادگی', {value: A.fullName}), em = field(manager ? 'ایمیل مدیر/معاون' : 'ایمیل معلم', {type: 'email', ltr: true, value: A.email});
      var send = btn('ارسال کد تأیید', async function (b) { setMsg(''); busy(b, true); try { A.fullName = name.input.value; A.email = S.auth.requireEmail(em.input.value); await api.sendRegistrationOtp(A.email, A.fullName, role); goA('otp', {otpKind: 'register', regRole: role}); } catch (e) { setMsg(S.errMsg(e)); busy(b, false); } });
      function chk() { send.disabled = !(name.input.value.trim().length >= 2 && em.input.value.indexOf('@') >= 0); } name.input.addEventListener('input', chk); em.input.addEventListener('input', chk); chk();
      card.appendChild(stag(4, name)); card.appendChild(stag(5, em)); card.appendChild(stag(6, send));
      card.appendChild(stag(7, google(role, 'ثبت‌نام با گوگل')));
      card.appendChild(stag(8, link('بازگشت', function () { goA('landing'); }, true)));
    }
    function otpPane(card) {
      var kind = A.otpKind;
      var title = kind === 'login' ? 'ورود با کد یک‌بارمصرف' : kind === 'recovery' ? 'بررسی کد بازیابی' : (A.regRole === 'manager' ? 'تأیید ایمیل مدیر/معاون' : 'تأیید ایمیل معلم');
      if (kind === 'recovery') card.appendChild(stag(0, steps(1)));
      var hd = header(title, 'کد ارسال‌شده به ' + A.email + ' را وارد کنید.'); hd.querySelector('.ice-head-ic').innerHTML = ''; hd.querySelector('.ice-head-ic').appendChild(ic('mail'));
      card.appendChild(stag(1, hd));
      card.appendChild(stag(2, el('p', {class: 'ice-note', text: 'کد یک‌بارمصرف ۶ تا ۸ رقم'})));
      var verify = btn('تأیید کد', async function (b) {
        setMsg(''); busy(b, true);
        try {
          var code = boxes.value();
          if (kind === 'login') { var u = await api.verifyLoginOtp(A.email, code); S.auth.login(u); }
          else if (kind === 'recovery') { A.username = await api.verifyRecoveryOtp(A.email, code); goA('recovery', {step: 'pw'}); }
          else { await api.verifyRegistrationOtp(A.email, code); goA('setup'); }
        } catch (e) { setMsg(S.errMsg(e)); busy(b, false); }
      });
      var boxes = otpBoxes(function (v) { verify.disabled = !(v.length >= 6 && v.length <= 8); });
      card.appendChild(stag(3, boxes)); card.appendChild(stag(4, verify));
      card.appendChild(stag(5, outline('ارسال دوباره کد', async function (b) { setMsg(''); busy(b, true); try { if (kind === 'login') await api.sendLoginOtp(A.email); else if (kind === 'recovery') await api.sendRecoveryOtp(A.email); else await api.sendRegistrationOtp(A.email, A.fullName, A.regRole); toast('کد دوباره ارسال شد.', 'ok'); } catch (e) { setMsg(S.errMsg(e)); } busy(b, false); })));
      card.appendChild(stag(6, link('بازگشت', function () { goA(kind === 'login' ? 'login' : kind === 'recovery' ? 'recovery' : 'register', kind === 'recovery' ? {step: 'email'} : {step: 'form'}); }, true)));
      setTimeout(function () { boxes.focus(); }, 350);
    }
    function setupPane(card) {
      var manager = A.regRole === 'manager';
      card.appendChild(stag(0, h(manager ? 'تکمیل حساب مدیر/معاون' : 'تکمیل حساب معلم')));
      card.appendChild(stag(1, el('p', {class: 'ice-sub right', text: manager ? 'ایمیل تأیید شد. مدرسه و اطلاعات ورود را تعیین کنید.' : 'ایمیل تأیید شد. نام کاربری نمایشی و رمز ورود را تعیین کنید.'})));
      var name = field('نام و نام خانوادگی', {value: A.fullName});
      var un = field('نام کاربری انگلیسی', {ltr: true, supporting: '۴ تا ۲۰ حرف انگلیسی، عدد یا زیرخط؛ با همین نام کاربری (یا ایمیل) وارد می‌شوید.'});
      var pw = field('رمز عبور (حداقل ۸ کاراکتر)', {type: 'password', ltr: true});
      var extra = {};
      if (manager) { extra.school = field('نام مدرسه'); extra.province = field('استان'); extra.city = field('شهر'); card.appendChild(stag(2, extra.school)); card.appendChild(stag(3, el('div', {class: 'ice-two'}, [extra.province, extra.city]))); }
      card.appendChild(stag(4, name)); card.appendChild(stag(5, un)); card.appendChild(stag(6, pw));
      if (!manager) { extra.invite = field('کد دعوت مدرسه (اختیاری)', {ltr: true, supporting: 'اگر مدیر مدرسه کد ۶ حرفی یا کد TCH داده است، آن را اینجا وارد کنید.'}); card.appendChild(stag(7, extra.invite)); }
      var done = btn(manager ? 'ساخت مدرسه و ورود' : 'تکمیل ثبت‌نام و ورود', async function (b) {
        setMsg(''); busy(b, true);
        try { var u = manager ? await api.completeManager(name.input.value, un.input.value, pw.input.value, extra.school.input.value, extra.province.input.value, extra.city.input.value) : await api.completeTeacher(name.input.value, un.input.value, pw.input.value, extra.invite.input.value); S.auth.login(u); }
        catch (e) { setMsg(S.errMsg(e)); busy(b, false); }
      });
      function chk() { done.disabled = !(un.input.value.length >= 4 && pw.input.value.length >= 8 && (!manager || extra.school.input.value.trim().length >= 2)); }
      card.querySelectorAll('input').forEach(function (i) { i.addEventListener('input', chk); }); chk();
      card.appendChild(stag(8, done));
      card.appendChild(stag(9, link('انصراف و خروج', function () { S.auth.logout().then(function () { goA('landing'); }); }, true)));
    }
    function recoveryPane(card) {
      if (A.step === 'pw') {
        card.appendChild(stag(0, steps(2)));
        card.appendChild(stag(1, header('تعیین رمز تازه', A.username ? 'نام کاربری حساب: ' + A.username : null)));
        var p1 = field('رمز جدید ۸ تا ۷۲ کاراکتر', {type: 'password', ltr: true}), p2 = field('تکرار رمز جدید', {type: 'password', ltr: true});
        var save = btn('ذخیره رمز و ورود', async function (b) { setMsg(''); busy(b, true); try { await api.changePassword(p1.input.value); var u = await S.auth.currentProfile(); S.auth.login(u); } catch (e) { setMsg(S.errMsg(e)); busy(b, false); } });
        function chk() { save.disabled = !(p1.input.value.length >= 8 && p1.input.value === p2.input.value); } p1.input.addEventListener('input', chk); p2.input.addEventListener('input', chk); chk();
        card.appendChild(stag(2, p1)); card.appendChild(stag(3, p2)); card.appendChild(stag(4, save));
        card.appendChild(stag(5, link('انصراف و خروج', function () { S.auth.logout().then(function () { goA('landing'); }); }, true)));
        return;
      }
      card.appendChild(stag(0, steps(0)));
      card.appendChild(stag(1, header('بازیابی رمز عبور', 'ایمیل حساب خود را وارد کنید تا کد بازیابی برایتان ارسال شود. پس از تأیید کد، نام کاربری را می‌بینید و رمز تازه می‌گذارید.')));
      var em = field('ایمیل حساب', {type: 'email', ltr: true, value: A.email});
      var send = btn('ارسال کد بازیابی', async function (b) { setMsg(''); busy(b, true); try { A.email = S.auth.requireEmail(em.input.value); await api.sendRecoveryOtp(A.email); goA('otp', {otpKind: 'recovery'}); } catch (e) { setMsg(S.errMsg(e)); busy(b, false); } });
      function chk() { send.disabled = em.input.value.indexOf('@') < 0; } em.input.addEventListener('input', chk); chk();
      card.appendChild(stag(2, em)); card.appendChild(stag(3, send));
      card.appendChild(stag(4, link('بازگشت', function () { goA('login', {tab: A.tab === 2 ? 1 : A.tab}); }, true)));
    }
  }

  /* ---------- پوسته ---------- */
  var STUDENT_TITLES = {dashboard: 'خانه دانش‌آموز', join: 'خانه دانش‌آموز', grades: 'نتایج من', calendar: 'تقویم و پیام‌ها', profile: 'حساب', tools: 'تنظیمات'};
  var MANAGER_TITLES = {teachers: 'معلم‌ها', dashboard: 'داشبورد', school: 'مدرسه', wallet: 'کیف پول', profile: 'حساب', tools: 'تنظیمات', calendar: 'تقویم', cards: 'کارت‌ها'};
  var TITLES = {exams: 'آزمون‌ها', dashboard: 'آزمون‌ها', wallet: 'کیف پول', cards: 'کارت‌ها', builder: 'ساخت آزمون', print: 'چاپ آزمون', classes: 'کلاس‌ها', students: 'دانش‌آموزان', bank: 'بانک سؤال', reports: 'گزارش‌ها', grading: 'تصحیح', calendar: 'تقویم و پیام‌ها', tools: 'تنظیمات', profile: 'حساب'};
  /* ================================================================ V161 — «حساب» گوشی مثل ProfileSettingsScreen اپ
     چیپ‌های بالا: پروفایل / حساب / سربرگ (معلم). پروفایل = ProfileSection (عکس محلی، نام نمایشی، مشخصات معلم، ذخیره).
     حساب = AccountSection: آکاردئون‌های «مشخصات حساب»، «پیوستن به مدرسه» (معلم)، «تغییر نام کاربری»، «تغییر ایمیل»،
     «تغییر رمز عبور» (با رمز فعلی یا کد بازیابی)، «حذف حساب». سربرگ = HeaderSection. */
  var profileTab = 'profile', accOpen = 'info';
  /* ================================================================ V162 — «تنظیمات» مثل ProfileSettingsScreen(destination=SETTINGS):
     چیپ‌های ظاهر / داده‌ها / درباره؛ ظاهر روی همین دستگاه ذخیره می‌شود (AppearancePreferences) */
  var LS_APPEAR = 'examsite.appearance.v1';
  var PALETTES = {INDIGO_MINT: ['#6C63F5', '#27C4A8', 'نیلی و سبز'], BLUE_CYAN: ['#1877D2', '#32B7C6', 'آبی و فیروزه‌ای'], PINK_ORANGE: ['#E96D8A', '#FFA14E', 'صورتی و نارنجی'], PURPLE_PINK: ['#8C5AD7', '#EC6DA7', 'بنفش و صورتی']};
  var APPEAR_DEF = {themeMode: 'SYSTEM', fontScale: 1, appFont: 'VAZIRMATN', palette: 'INDIGO_MINT', depth: 14, persianDigits: false};
  function appearance() { try { return Object.assign({}, APPEAR_DEF, JSON.parse(localStorage.getItem(LS_APPEAR) || '{}')); } catch (e) { return Object.assign({}, APPEAR_DEF); } }
  function setAppearance(patch) { var a = Object.assign(appearance(), patch); try { localStorage.setItem(LS_APPEAR, JSON.stringify(a)); } catch (e) {} applyAppearance(); return a; }
  function applyAppearance() {
    var a = appearance(), root = document.documentElement, st = root.style;
    var dark = a.themeMode === 'DARK' || (a.themeMode === 'SYSTEM' && window.matchMedia('(prefers-color-scheme: dark)').matches);
    root.classList.toggle('m-dark', !!dark);
    var pal = PALETTES[a.palette] || PALETTES.INDIGO_MINT;
    st.setProperty('--m-acc', pal[0]); st.setProperty('--m-acc2', pal[1]);
    var d = Math.max(8, Math.min(22, +a.depth || 14)); st.setProperty('--m-depth', d + 'px'); st.setProperty('--m-depth2', Math.round(d * 2.2) + 'px');
    st.setProperty('--m-scale', String(Math.max(0.85, Math.min(1.3, +a.fontScale || 1))));
    var fonts = {SYSTEM: 'Tahoma, "Segoe UI", sans-serif', VAZIRMATN: "'Vazirmatn', Tahoma, sans-serif", SHABNAM: "'Shabnam', 'Vazirmatn', Tahoma, sans-serif", SAHEL: "'Sahel', 'Vazirmatn', Tahoma, sans-serif"};
    st.setProperty('--m-font', fonts[a.appFont] || fonts.VAZIRMATN);
  }
  applyAppearance();
  try { window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', applyAppearance); } catch (e) {}
  var settingsTab = 'appearance';
  function chipRow(items, cur, on) { return el('div', {class: 'm-chips'}, items.map(function (it) { return el('button', {class: 'm-chip' + (cur === it[0] ? ' on' : ''), text: it[1], onclick: function () { on(it[0]); }}); })); }
  function switchRow(title, sub, on, cb) {
    var sw = el('button', {class: 'm-switch' + (on ? ' on' : ''), 'aria-pressed': on ? 'true' : 'false', onclick: function () { on = !on; sw.classList.toggle('on', on); sw.setAttribute('aria-pressed', on ? 'true' : 'false'); cb(on); }}, [el('i')]);
    return el('div', {class: 'm-switch-row'}, [el('div', {class: 'grow'}, [el('div', {text: title}), sub ? el('small', {class: 'muted', text: sub}) : null]), sw]);
  }
  function settingsScreen(c) {
    var u = S.user(); c.innerHTML = '';
    var tabs = [['appearance', 'ظاهر'], ['data', 'داده‌ها'], ['about', 'درباره']];
    c.appendChild(chipRow(tabs, settingsTab, function (k) { settingsTab = k; settingsScreen(c); }));
    if (settingsTab === 'appearance') return appearanceSection(c);
    if (settingsTab === 'data') {
      if (u.role === 'teacher') c.appendChild(SiteExtras.backupCard());
      else if (u.role === 'manager') c.appendChild(managerBackupCard());
      else c.appendChild(el('div', {class: 'card m-pcard', text: 'پشتیبان کامل داده‌ها فقط برای کادر مدرسه در دسترس است.'}));
      return;
    }
    aboutSection(c);
  }
  function appearanceSection(c) {
    var a = appearance();
    function card(title, kids) { return el('div', {class: 'card m-pcard'}, [el('h3', {text: title})].concat(kids)); }
    var rerun = function () { appearanceSection.__redo && appearanceSection.__redo(); };
    appearanceSection.__redo = function () { c.innerHTML = ''; c.appendChild(chipRow([['appearance', 'ظاهر'], ['data', 'داده‌ها'], ['about', 'درباره']], settingsTab, function (k) { settingsTab = k; settingsScreen(c); })); appearanceSection(c); };
    c.appendChild(card('حالت نمایش', [
      chipRow([['SYSTEM', 'دستگاه'], ['LIGHT', 'روشن'], ['DARK', 'تیره']], a.themeMode, function (v) { setAppearance({themeMode: v}); rerun(); }),
      switchRow('اعداد فارسی در ابزارها', 'محورها، نمودارها، شکل‌ها و جدول‌ها', a.persianDigits, function (v) { setAppearance({persianDigits: v}); })
    ]));
    c.appendChild(card('چیدمان دستگاه', [el('p', {class: 'muted', text: 'در وب، چیدمان گوشی/دسکتاپ از روی پهنای صفحه تشخیص داده می‌شود؛ روی تبلت مرورگر را عریض‌تر کنید تا نمای دسکتاپ نمایش داده شود.'}), el('p', {class: 'muted', style: 'color:var(--m-acc)', text: 'چیدمان فعلی: گوشی'})]));
    var palRow = el('div', {class: 'm-pal-row'}, Object.keys(PALETTES).map(function (k) { var p = PALETTES[k]; return el('button', {class: 'm-pal' + (a.palette === k ? ' on' : ''), 'aria-label': 'پالت ' + p[2], style: 'background:linear-gradient(135deg,' + p[0] + ',' + p[1] + ')', onclick: function () { setAppearance({palette: k}); rerun(); }}, [a.palette === k ? ic('check') : null]); }));
    c.appendChild(card('ظاهر نئومورفیک — پالت رنگ', [el('p', {class: 'muted', text: 'پالت و عمق سایه در دستگاه ذخیره می‌شوند و پس از اجرای دوباره باقی می‌مانند.'}), palRow]));
    var dv = el('b', {style: 'color:var(--m-acc)', text: fa(Math.round(a.depth))});
    var dr = el('input', {type: 'range', min: '8', max: '22', step: '1', value: String(a.depth), class: 'm-range'});
    dr.addEventListener('input', function () { dv.textContent = fa(dr.value); document.documentElement.style.setProperty('--m-depth', dr.value + 'px'); document.documentElement.style.setProperty('--m-depth2', Math.round(dr.value * 2.2) + 'px'); });
    dr.addEventListener('change', function () { setAppearance({depth: +dr.value}); });
    c.appendChild(card('ظاهر نئومورفیک — عمق سایه', [el('div', {class: 'm-lv'}, [el('span', {text: 'عمق سایه'}), dv]), dr]));
    c.appendChild(card('ظاهر نئومورفیک — پیش‌نمایش', [el('div', {class: 'm-pal-preview', text: 'پیش‌نمایش پالت'})]));
    c.appendChild(card('قلم فارسی', [chipRow([['SYSTEM', 'سیستم'], ['VAZIRMATN', 'وزیرمتن'], ['SHABNAM', 'شبنم'], ['SAHEL', 'ساحل']], a.appFont, function (v) { setAppearance({appFont: v}); rerun(); }), el('p', {class: 'muted', text: 'وزیرمتن همراه سایت است؛ شبنم و ساحل در صورت نصب روی دستگاه اعمال می‌شوند.'})]));
    var sv = el('p', {text: fa(Math.round(a.fontScale * 100)) + ' درصد'});
    var sr = el('input', {type: 'range', min: '85', max: '130', step: '5', value: String(Math.round(a.fontScale * 100)), class: 'm-range'});
    sr.addEventListener('input', function () { sv.textContent = fa(sr.value) + ' درصد'; document.documentElement.style.setProperty('--m-scale', String(sr.value / 100)); });
    sr.addEventListener('change', function () { setAppearance({fontScale: sr.value / 100}); });
    c.appendChild(card('اندازه متن', [sv, sr, el('p', {text: 'نمونه متن فارسی — آزمون ریاضی فصل یک'})]));
    c.appendChild(el('button', {class: 'm-outline', style: 'width:100%;margin-bottom:12px', text: 'بازگردانی تنظیمات ظاهری', onclick: function () { try { localStorage.removeItem(LS_APPEAR); } catch (e) {} applyAppearance(); rerun(); }}));
  }
  function managerBackupCard() {
    var msg = el('div');
    return el('div', {class: 'card m-pcard'}, [el('h3', {text: 'پشتیبان داده‌های مدرسه'}), el('p', {class: 'muted', text: 'مدرسه‌ها، معلم‌ها، کلاس‌ها و دانش‌آموزان در یک فایل JSON ذخیره می‌شوند.'}), msg,
      el('button', {class: 'btn', text: 'ساخت پشتیبان مدرسه', onclick: async function () { msg.innerHTML = ''; try { var raw = await S.rpcObj('native_manager_export_backup_v61', {}); if (raw && raw.error) throw new Error(String(raw.error)); SiteExtras.download('school-backup.json', JSON.stringify(raw, null, 2), 'application/json'); toast('پشتیبان مدرسه ذخیره شد.', 'ok'); } catch (e) { msg.appendChild(el('div', {class: 'alert error', text: S.errMsg(e)})); } }})]);
  }
  function aboutSection(c) {
    var b = S.config || {};
    c.appendChild(el('div', {class: 'card m-pcard'}, [el('h3', {text: 'دربارهٔ آزمون‌ساز — نسخهٔ وب'}),
      el('div', {class: 'm-lv'}, [el('span', {text: 'نشانی'}), el('b', {text: location.host || 'onlineexam.ir'})]),
      el('div', {class: 'm-lv'}, [el('span', {text: 'حالت'}), el('b', {text: window.matchMedia('(display-mode: standalone)').matches ? 'نصب‌شده (PWA)' : 'مرورگر'})]),
      el('p', {class: 'muted', text: 'سایت با هر انتشار خودکار به‌روز می‌شود؛ اگر پیام «نسخهٔ جدید آماده است» دیدید، صفحه را دوباره باز کنید.'}),
      el('button', {class: 'btn light', style: 'width:100%', text: 'بررسی به‌روزرسانی (بارگذاری دوباره)', onclick: function () { location.reload(); }})]));
    c.appendChild(el('div', {class: 'card m-pcard'}, [el('h3', {text: 'برنامهٔ اندروید'}), el('p', {class: 'muted', text: 'نسخهٔ اندروید همان امکانات را با تختهٔ سفید و ابزارهای بیشتر دارد.'}), el('a', {class: 'btn light', style: 'width:100%;display:block;text-align:center', href: 'https://github.com/hashemamiri/exam-app-kotlin/releases/latest', target: '_blank', rel: 'noopener', text: 'دریافت آخرین APK'})]));
  }
  var LS_AVATAR = 'examsite.avatar.';
  function localAvatar(uid) { try { return localStorage.getItem(LS_AVATAR + uid); } catch (e) { return null; } }
  function acc(key, title, body) {
    var open = accOpen === key;
    var card = el('div', {class: 'm-acc neo' + (open ? ' open' : '')});
    card.appendChild(el('button', {class: 'm-acc-h', onclick: function () { accOpen = open ? '' : key; paint(); }}, [el('b', {text: title}), ic('chevron', 'm-acc-chev')]));
    if (open) card.appendChild(el('div', {class: 'm-acc-b'}, body()));
    return card;
  }
  function field(label, val, opts) { opts = opts || {}; var i = el('input', Object.assign({type: opts.type || 'text', value: val || ''}, opts.attrs || {})); return {i: i, row: el('div', {class: 'field' + (opts.ltr ? ' ltr' : '')}, [el('label', {text: label}), i, opts.hint ? el('small', {class: 'muted', text: opts.hint}) : null])}; }
  async function profileScreen(c) {
    S.loading(c);
    var u = S.user(), p;
    try { p = await S.api.profile(); } catch (e) { S.showErr(c, e); return; }
    c.innerHTML = '';
    var tabs = [['profile', 'پروفایل'], ['account', 'حساب']]; if (p.role === 'teacher') tabs.push(['header', 'سربرگ']);
    c.appendChild(el('div', {class: 'm-chips'}, tabs.map(function (t) { return el('button', {class: 'm-chip' + (profileTab === t[0] ? ' on' : ''), text: t[1], onclick: function () { profileTab = t[0]; paint(); }}); })));
    var msg = el('div');
    if (profileTab === 'profile') {
      /* --- ProfileSection --- */
      var av = localAvatar(p.id);
      var avBox = el('div', {class: 'm-avatar xl'}, [av ? el('img', {src: av, alt: ''}) : el('span', {text: (p.displayName || p.fullName || '?').trim().charAt(0)})]);
      var pick = el('button', {class: 'btn sm', text: av ? '📷 تعویض' : '📷 انتخاب عکس', onclick: async function () {
        var f = await new Promise(function (res) { var i = el('input', {type: 'file', accept: 'image/*', style: 'display:none'}); i.addEventListener('change', function () { res(i.files && i.files[0] || null); i.remove(); }); document.body.appendChild(i); i.click(); });
        if (!f) return;
        var url = await new Promise(function (res) { var r = new FileReader(); r.onload = function () { res(String(r.result)); }; r.readAsDataURL(f); });
        var im = await new Promise(function (res, rej) { var x = new Image(); x.onload = function () { res(x); }; x.onerror = rej; x.src = url; });
        var sz = Math.min(im.width, im.height), cv = document.createElement('canvas'); cv.width = cv.height = 256; cv.getContext('2d').drawImage(im, (im.width - sz) / 2, (im.height - sz) / 2, sz, sz, 0, 0, 256, 256);
        try { localStorage.setItem(LS_AVATAR + p.id, cv.toDataURL('image/jpeg', 0.85)); } catch (e) {}
        toast('عکس روی همین دستگاه ذخیره شد.', 'ok'); paint();
      }});
      var rm = av ? el('button', {class: 'btn light sm', text: '🗑 حذف', onclick: async function () { if (await S.confirmDlg('حذف عکس پروفایل', 'عکس پروفایل از این دستگاه حذف شود؟', 'حذف', true)) { try { localStorage.removeItem(LS_AVATAR + p.id); } catch (e) {} paint(); } }}) : null;
      c.appendChild(el('div', {class: 'card neo m-pcard center'}, [avBox, el('div', {class: 'row', style: 'justify-content:center'}, [pick, rm]), el('p', {class: 'muted', style: 'font-size:12px', text: 'عکس فقط روی همین دستگاه می‌ماند و به سرور فرستاده نمی‌شود.'})]));
      var dn = field('نام نمایشی', p.displayName, {hint: 'خالی باشد، نام اصلی حساب نمایش داده می‌شود.'});
      c.appendChild(el('div', {class: 'card neo m-pcard'}, [el('h3', {text: 'نام نمایشی'}), dn.row]));
      var fn, ln, ec, ph;
      if (p.role === 'teacher') {
        fn = field('نام', p.firstName); ln = field('نام خانوادگی', p.lastName); ec = field('کد پرسنلی', p.employeeCode, {hint: 'اختیاری؛ حداکثر ۳۰ حرف انگلیسی یا عدد', ltr: true}); ph = field('شماره تلفن', p.phone, {hint: 'اختیاری؛ ۱۱ رقم و با 09 شروع شود', ltr: true});
        c.appendChild(el('div', {class: 'card neo m-pcard'}, [el('h3', {text: 'مشخصات معلم'}), el('div', {class: 'grid2'}, [fn.row, ln.row]), ec.row, ph.row]));
      }
      var save = el('button', {class: 'btn', style: 'width:100%', text: 'ذخیره پروفایل', onclick: async function () {
        save.disabled = true; msg.innerHTML = '';
        try { await S.api.saveProfile({role: p.role, displayName: dn.i.value, firstName: fn ? fn.i.value : '', lastName: ln ? ln.i.value : '', employeeCode: ec ? ec.i.value : '', phone: ph ? ph.i.value : '', avatarUrl: p.avatarUrl, avatarPublic: p.avatarPublic, header: p.header || {province: '', city: '', district: '', school: '', grade: '', fieldOfStudy: ''}}); u.name = dn.i.value.trim() || p.fullName; toast('پروفایل ذخیره شد.', 'ok'); }
        catch (e) { msg.appendChild(el('div', {class: 'alert error', text: S.errMsg(e)})); }
        save.disabled = false;
      }});
      c.appendChild(msg); c.appendChild(save);
      return;
    }
    if (profileTab === 'header') {
      var h = p.header || {}, hf = {};
      var hcard = el('div', {class: 'card neo m-pcard'}, [el('h3', {text: 'اطلاعات سربرگ رسمی امتحان'}), el('p', {class: 'muted', style: 'font-size:12px', text: 'این اطلاعات در قالب چاپ رسمی استفاده می‌شود.'})]);
      [['province', 'استان'], ['city', 'شهر'], ['district', 'منطقه'], ['school', 'مدرسه / واحد'], ['grade', 'پایه'], ['fieldOfStudy', 'رشته']].forEach(function (x) { hf[x[0]] = field(x[1], h[x[0]]); hcard.appendChild(hf[x[0]].row); });
      var hs = el('button', {class: 'btn', style: 'width:100%', text: 'ذخیره سربرگ', onclick: async function () {
        hs.disabled = true; msg.innerHTML = '';
        try { var hd = {}; Object.keys(hf).forEach(function (k) { hd[k] = hf[k].i.value; }); await S.api.saveProfile({role: p.role, displayName: p.displayName, firstName: p.firstName, lastName: p.lastName, employeeCode: p.employeeCode, phone: p.phone, avatarUrl: p.avatarUrl, avatarPublic: p.avatarPublic, header: hd}); toast('سربرگ ذخیره شد.', 'ok'); }
        catch (e) { msg.appendChild(el('div', {class: 'alert error', text: S.errMsg(e)})); }
        hs.disabled = false;
      }});
      c.appendChild(hcard); c.appendChild(msg); c.appendChild(hs);
      return;
    }
    /* --- AccountSection --- */
    var roleFa = p.role === 'teacher' ? 'معلم' : p.role === 'manager' ? 'مدیر/معاون' : 'دانش‌آموز';
    function lv(l, v) { return el('div', {class: 'm-lv'}, [el('span', {class: 'muted', text: l}), el('b', {text: v || '—'})]); }
    c.appendChild(acc('info', 'مشخصات حساب', function () { var rows = [lv('نام', p.fullName), lv('نام کاربری', p.username), lv('نقش', roleFa)]; if (p.role !== 'student') rows.push(lv('ایمیل', u.email)); return rows; }));
    if (p.role === 'teacher' && window.SiteSchool) c.appendChild(acc('join_school', 'پیوستن به مدرسه', function () { var k = window.SiteSchool.joinSchoolCard(function () { paint(); }); k.className = ''; return [k]; }));
    c.appendChild(acc('username', 'تغییر نام کاربری', function () {
      if (p.role === 'student') return [el('p', {class: 'muted', text: 'تغییر نام کاربری دانش‌آموز فقط توسط معلم انجام می‌شود.'})];
      var un = field('نام کاربری انگلیسی', p.username, {ltr: true, hint: 'ورود معلم همچنان با ایمیل انجام می‌شود.'}), m2 = el('div');
      return [un.row, m2, el('button', {class: 'btn', style: 'width:100%', text: 'ذخیره نام کاربری', onclick: async function () { m2.innerHTML = ''; try { var v = un.i.value.trim().toLowerCase(); if (!/^[a-z][a-z0-9_]{3,19}$/.test(v)) throw new Error('نام کاربری باید ۴ تا ۲۰ حرف انگلیسی، عدد یا زیرخط باشد.'); var r = await S.api.updateUsername(v); if (r && r.error) throw new Error(r.error); u.username = v; toast('نام کاربری ذخیره شد.', 'ok'); paint(); } catch (e) { m2.appendChild(el('div', {class: 'alert error', text: S.errMsg(e)})); } }})];
    }));
    c.appendChild(acc('email', 'تغییر ایمیل', function () {
      if (p.role === 'student') return [el('p', {class: 'muted', text: 'ایمیل ورود دانش‌آموز توسط سامانه مدیریت می‌شود و در برنامه نمایش داده نمی‌شود.'})];
      var em = field('ایمیل جدید', '', {ltr: true, type: 'email', hint: 'Supabase پیام تأیید می‌فرستد؛ تا تأیید، ایمیل فعلی معتبر می‌ماند.'}), m3 = el('div');
      return [em.row, m3, el('button', {class: 'btn', style: 'width:100%', text: 'ارسال تأیید به ایمیل جدید', onclick: async function () { m3.innerHTML = ''; try { var v = em.i.value.trim().toLowerCase(); if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(v)) throw new Error('ایمیل معتبر نیست.'); await S.api.updateEmail(v); toast('پیام تأیید به ایمیل جدید ارسال شد.', 'ok'); } catch (e) { m3.appendChild(el('div', {class: 'alert error', text: S.errMsg(e)})); } }})];
    }));
    c.appendChild(acc('password', 'تغییر رمز عبور', function () {
      var recovery = !!accPwRecovery, m4 = el('div');
      var cur = field('رمز فعلی', '', {type: 'password', ltr: true}), code = field('کد بازیابی ۶ تا ۸ رقم', '', {ltr: true}), n1 = field('رمز جدید ۸ تا ۷۲ کاراکتر', '', {type: 'password', ltr: true}), n2 = field('تکرار رمز جدید', '', {type: 'password', ltr: true});
      var rows = [];
      if (!recovery) rows.push(cur.row);
      else rows.push(el('p', {class: 'muted', text: 'کد بازیابی به ایمیل حساب ارسال می‌شود و بدون رمز قبلی رمز تازه ثبت می‌شود.'}), el('button', {class: 'btn light', style: 'width:100%', text: 'ارسال کد بازیابی به ایمیل', onclick: async function () { m4.innerHTML = ''; try { if (!u.email) throw new Error('ایمیل حساب در دسترس نیست.'); await S.api.sendLoginOtp(u.email); toast('کد بازیابی به ایمیل حساب ارسال شد.', 'ok'); } catch (e) { m4.appendChild(el('div', {class: 'alert error', text: S.errMsg(e)})); } }}), code.row);
      rows.push(n1.row, n2.row, m4,
        el('button', {class: 'btn', style: 'width:100%', text: recovery ? 'تأیید کد و ثبت رمز جدید' : 'تغییر رمز عبور', onclick: async function () {
          m4.innerHTML = '';
          try {
            if (n1.i.value.length < 8 || n1.i.value.length > 72) throw new Error('رمز عبور باید ۸ تا ۷۲ کاراکتر باشد.');
            if (n1.i.value !== n2.i.value) throw new Error('تکرار رمز عبور یکسان نیست.');
            if (!recovery) { if (!cur.i.value) throw new Error('رمز فعلی را وارد کنید.'); try { await S.api.verifyCurrentPassword(u.email, cur.i.value); } catch (e) { throw new Error('رمز فعلی نادرست است.'); } }
            else { await S.api.verifyLoginOtp(u.email, code.i.value); }
            await S.api.changePassword(n1.i.value); toast(recovery ? 'رمز عبور بازیابی و تغییر کرد.' : 'رمز عبور با موفقیت تغییر کرد.', 'ok'); accPwRecovery = false; paint();
          } catch (e) { m4.appendChild(el('div', {class: 'alert error', text: S.errMsg(e)})); }
        }}),
        el('button', {class: 'btn light', style: 'width:100%', text: recovery ? 'بازگشت به تغییر با رمز فعلی' : 'رمز فعلی را فراموش کرده‌ام', onclick: function () { accPwRecovery = !recovery; paint(); }}));
      return rows;
    }));
    if (p.role !== 'student' && window.SiteExtras) c.appendChild(acc('delete', 'حذف حساب', function () { var k = window.SiteExtras.deleteAccountCard(async function () { S.logout(); }); k.className = ''; return [k]; }));
  }
  var accPwRecovery = false;
  function paint() {
    cleanupBuilder();
    var root = document.getElementById('root'); if (!root) return;
    /* V170 — وقتی صفحه‌های حساب/تنظیمات روی دسکتاپ (بدون پوستهٔ گوشی) استفاده می‌شوند، بازنقاشی = رندر پنل دسکتاپ */
    if (!MQ.matches && !document.getElementById('m-shell')) { S.render(); return; }
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
      var ttl = (mgr ? MANAGER_TITLES : TITLES)[page] || '';
      if (page === 'builder' && view.arg && view.arg.examId) ttl = 'ویرایش آزمون';
      if (page === 'builder' && view.arg && view.arg.mode === 'print') ttl = 'ساخت آزمون';
      var backTo = (page === 'builder' && view.arg && view.arg.mode === 'print') ? 'print' : home;
      head = el('div', {class: 'm-head'}, [el('button', {class: 'm-back', 'aria-label': 'بازگشت', onclick: function () { go(backTo); }}, [ic('chevron')]), el('h1', {text: ttl})]);
    }
    var content = el('div', {class: 'm-content' + (head ? '' : ' no-head'), id: 'content'});
    if (head) shell.appendChild(head);
    shell.appendChild(content);
    /* V156 — مثل ExamApp.kt (page == BUILDER تمام‌صفحه و بدون bottomBar): در سازنده داک پایین نمایش داده نمی‌شود */
    if (page !== 'builder') shell.appendChild(dock());
    if (ui.addOpen && page !== 'builder') shell.appendChild(quickAdd());
    if (ui.menuOpen) content.appendChild(mgr ? managerMenu() : menuScreen());
    else if (mgr && page === 'cards') managerCards(content);
    else if (mgr && (page === 'profile' || page === 'account')) profileScreen(content);
    else if (mgr && (page === 'tools' || page === 'settings')) settingsScreen(content);
    else if (mgr) S.renderPage(content);
    else if (page === 'exams' || page === 'dashboard') examsScreen(content);
    else if (page === 'cards') cardsScreen(content);
    else if (page === 'print') printCenter(content);
    else if (page === 'students' && !mgr) studentsScreen(content);
    else if (page === 'profile' || page === 'account') profileScreen(content);
    else if (page === 'tools' || page === 'settings') settingsScreen(content);
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
        el('button', {class: 'm-back', 'aria-label': 'منو', onclick: function () { ui.menuOpen = !ui.menuOpen; if (ui.menuOpen) closeOverlays(); paint(); }}, [ic(ui.menuOpen ? 'close' : 'menu')]),
        el('h1', {text: ui.menuOpen ? 'منو' : (STUDENT_TITLES[page] || '')}),
        (!ui.menuOpen && page !== 'dashboard' && page !== 'join') ? el('button', {class: 'm-back', 'aria-label': 'خانه', onclick: function () { go('dashboard'); }}, [ic('chevron')]) : null
      ]));
    }
    shell.appendChild(content);
    if (inExam) { if (window.SiteStudent) window.SiteStudent.page(content, view.arg); return; }
    if (ui.menuOpen) content.appendChild(studentMenu());
    else if (page === 'dashboard' || page === 'join') studentHome(content);
    else if (page === 'profile' || page === 'account') profileScreen(content);
    else if (page === 'tools' || page === 'settings') settingsScreen(content);
    else S.renderPage(content);
  }

  /* اتصال: app.js در render() اگر active() بود paint() را صدا می‌زند؛ تغییر عرض → رندر دوباره */
  var rerender = function () { if (S.user()) S.render(); };
  MQ.addEventListener ? MQ.addEventListener('change', rerender) : MQ.addListener(rerender);
  window.SiteMobile = {paint: paint, active: active, ui: ui, authActive: authActive, paintAuth: paintAuth, teacherCards: teacherCards, icons: I,
    /* V170 — همان صفحه‌های اپ برای دسکتاپ: «حساب» = profileScreen با تب حساب، «تنظیمات» = settingsScreen */
    profileScreen: profileScreen, settingsScreen: settingsScreen, setProfileTab: function (t) { profileTab = t; }};
})();
