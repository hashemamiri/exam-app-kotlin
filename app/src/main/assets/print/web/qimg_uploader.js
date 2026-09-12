(function () {
  'use strict';
  if (window.__qimgUploader) return;
  window.__qimgUploader = true;

  var MAXW = 1280, JQ = 0.9, DEF_W = 420;
  var SIZES = [['S', 240], ['M', 420], ['L', 640], ['∞', 0]];
  var lastField = null;
  var lastBlock = null;

  /* ---------- ۱) تصویرِ توکارِ قدیمی (داخلِ متن) همچنان درست دیده شود ---------- */
  function specOf(raw) { var x = {}; try { x = JSON.parse(raw || '{}'); } catch (e) {} return x; }

  function imgNode(raw) {
    var spec = specOf(raw);
    if (spec.k !== 'img' || !spec.src) return null;
    var el = document.createElement('span');
    el.className = 'qmf-fig qmf-img';
    el.contentEditable = 'false';
    el.setAttribute('data-fig', raw);
    el.setAttribute('role', 'img');
    el.setAttribute('aria-label', 'تصویرِ سؤال');
    var im = document.createElement('img');
    im.className = 'qmf-img-el';
    im.src = spec.src;
    im.alt = spec.alt || 'تصویرِ سؤال';
    var w = parseInt(spec.w, 10) || 0;
    im.style.cssText = 'display:inline-block;max-width:100%;height:auto;border-radius:6px;background:#fff;' +
      (w ? ('width:' + w + 'px;') : '');
    el.appendChild(im);
    return el;
  }

  function patchDispatcher() {
    var G = window.GeoFig;
    if (!G || typeof G.make !== 'function') return false;
    if (G.__qimgMake === G.make) return true;
    var base = G.make;
    var wrapped = function (raw) {
      try { var n = imgNode(raw); if (n) return n; } catch (e) {}
      return base.apply(G, arguments);
    };
    G.make = wrapped;
    G.__qimgMake = wrapped;
    return true;
  }
  (function keepDispatcher() {
    patchDispatcher();
    var n = 0;
    var iv = setInterval(function () { n++; patchDispatcher(); if (n > 40) clearInterval(iv); }, 250);
  })();

  /* ---------- ۲) ابزارهایِ کوچک ---------- */
  function mk(tag, cls, html) {
    var e = document.createElement(tag);
    if (cls) e.className = cls;
    if (html != null) e.innerHTML = html;
    return e;
  }
  function closestOf(node, sel) {
    var n = node;
    while (n && n.nodeType !== 1) n = n.parentElement;
    while (n && n !== document) {
      if (n.matches && n.matches(sel)) return n;
      n = n.parentElement;
    }
    return null;
  }

  /* ---------- ۳) سؤالِ جاری و فهرستِ تصویرهایش ---------- */
  function questionOf(block) {
    var ta = fieldOf(block);
    var m = ta ? /^q_text_(\d+)$/.exec(String(ta.id || '')) : null;
    var qs = null;
    try { qs = (typeof questions !== 'undefined') ? questions : null; } catch (e) {}
    if (!qs || !m) return null;
    for (var i = 0; i < qs.length; i++) {
      if (String(qs[i].id) === m[1]) return qs[i];
    }
    return null;
  }
  function imgsOf(q) {
    if (!q) return null;
    if (!Array.isArray(q.qimgImages)) q.qimgImages = [];
    return q.qimgImages;
  }

  /* ---------- ۴) ساختِ تصویر در پیش‌نمایش (قابلِ تغییرِ اندازه و جابه‌جایی) ---------- */
  function figHtml(im, i) {
    var w = parseInt(im.w, 10) || 0;
    var al = (im.align === 'right' || im.align === 'left') ? im.align : 'center';
    var rx = parseFloat(im.rx) || 0, oy = parseInt(im.dy, 10) || 0;
    var stl = (rx ? ('left:' + rx + '%;') : '') + (oy ? ('top:' + oy + 'px;') : '');
    var s = '<span class="qimg-wrap" style="display:block;text-align:' + al + '">';
    s += '<span class="qimg-fig" data-qimg="' + i + '"' + (stl ? (' style="' + stl + '"') : '') + '>';
    s += '<img class="qimg-attach-img" src="' + im.src + '" alt="تصویر سؤال" draggable="false"';
    s += ' style="display:block;margin:0;max-width:100%;height:auto;border-radius:6px;background:#fff;';
    s += (w ? ('width:' + w + 'px;') : '') + '">';
    s += '<span class="qimg-h" data-edge="l"></span><span class="qimg-h" data-edge="r"></span>';
    s += '<span class="qimg-tip">دستگیره‌ها = تغییرِ اندازه · کشیدنِ تصویر = جابه‌جایی</span>';
    s += '</span></span>';
    return s;
  }
  function groupHtml(list) {
    var out = '';
    for (var i = 0; i < list.length; i++) {
      if (list[i] && list[i].src) out += figHtml(list[i], i);
    }
    return out;
  }
  function sigOf(list) {
    var s = '';
    for (var k = 0; k < list.length; k++) {
      s += list[k].src.length + ':' + (list[k].w || 0) + ':' + (list[k].align || 'center') +
           ':' + (list[k].pos || 'below') + ':' + (list[k].rx || 0) + ':' + (list[k].dy || 0) + '|';
    }
    return s;
  }
  /* ---------- ۴ب) تزریقِ تصویرها در پیش‌نمایش/چاپ ---------- */
  /* نکته: برنامه هنگامِ خالی‌بودنِ متنِ سؤال renderRichText را صدا نمی‌زند؛
     برای همین تصویرها مستقیماً در DOM و پس از بلوکِ متنِ هر سؤال گذاشته می‌شوند */
  function bindFigs(holder, q) {
    var figs = holder.querySelectorAll('.qimg-fig');
    for (var i = 0; i < figs.length; i++) {
      var idx = parseInt(figs[i].getAttribute('data-qimg'), 10);
      figs[i].__q = q;
      figs[i].__im = (q.qimgImages || [])[idx];
      figs[i].__holder = holder;
    }
  }
  function setHold(block, q, list, pos) {
    var key = (pos === 'above') ? '__qimgHoldA' : '__qimgHoldB';
    var sig = pos + ':' + sigOf(list);
    if (!list.length) {
      if (block[key]) { try { block.parentNode.removeChild(block[key]); } catch (e) {} }
      block[key] = null;
      block[key + 'Sig'] = null;
      return;
    }
    if (!block[key] || !block[key].parentNode) {
      var d = document.createElement('div');
      d.className = 'qimg-attach' + (pos === 'above' ? ' above' : '');
      if (pos === 'above') block.parentNode.insertBefore(d, block);
      else block.parentNode.insertBefore(d, block.nextSibling);
      block[key] = d;
    }
    if (block[key + 'Sig'] !== sig) {
      block[key].innerHTML = groupHtml(list);
      block[key + 'Sig'] = sig;
      bindFigs(block[key], q);
    }
  }
  function injectPreview() {
    if (drag) return;                     /* هنگامِ کشیدن بازسازی ممنوع (پرش نمی‌آید) */
    var area = document.getElementById('previewArea');
    if (!area) return;
    var qs = null;
    try { qs = (typeof questions !== 'undefined') ? questions : null; } catch (e) {}
    if (!qs) return;
    var blocks = [].slice.call(area.querySelectorAll('.q-rich-content'));
    if (blocks.length !== qs.length) return;
    for (var i = 0; i < blocks.length; i++) {
      var q = qs[i], block = blocks[i];
      if (!q || !block || !block.parentNode) continue;
      var list = Array.isArray(q.qimgImages) ? q.qimgImages : [];
      var above = [], below = [];
      for (var k = 0; k < list.length; k++) {
        if (list[k] && list[k].pos === 'above') above.push(list[k]);
        else below.push(list[k]);
      }
      setHold(block, q, above, 'above');
      setHold(block, q, below, 'below');
    }
  }
  function wrapPreview() {
    if (typeof window.renderPreview !== 'function' || window.renderPreview.__qimg) return;
    var base = window.renderPreview;
    var wrapped = function () {
      var r = base.apply(this, arguments);
      try { injectPreview(); } catch (e) {}
      return r;
    };
    wrapped.__qimg = true;
    window.renderPreview = wrapped;
  }
  (function keepPreview() {
    wrapPreview();
    var n = 0;
    var iv = setInterval(function () { n++; wrapPreview(); injectPreview(); if (n > 60) clearInterval(iv); }, 250);
    try {
      new MutationObserver(function () {
        if (injectPreview._t) return;
        injectPreview._t = setTimeout(function () { injectPreview._t = null; injectPreview(); }, 30);
      }).observe(document.documentElement, { childList: true, subtree: true });
    } catch (e) {}
  })();

  /* ---------- ۴ج) تغییرِ اندازه و جابه‌جایی با کشیدن (تنها در پیش‌نمایش) ---------- */
  var drag = null;
  function clearSel() {
    var s = document.querySelectorAll('.qimg-fig.sel');
    for (var i = 0; i < s.length; i++) s[i].classList.remove('sel');
  }
  function selectFig(fig) { clearSel(); if (fig) fig.classList.add('sel'); }
  /* ذخیرهٔ خودکار را بیدار کن (بدون دست‌زدن به متنِ سؤال) */
  function saveNow(q) {
    try {
      var ta = (q && document.getElementById('q_text_' + q.id)) || null;
      if (ta) {
        ta.dispatchEvent(new Event('input', { bubbles: true }));
        if (window.__examBuilderUpdateQuestionText) window.__examBuilderUpdateQuestionText(ta);
      }
    } catch (e) {}
  }
  function findFigOf(im, q) {
    var figs = document.querySelectorAll('#previewArea .qimg-fig');
    for (var i = 0; i < figs.length; i++) {
      if (figs[i].__im === im && figs[i].__q === q) return figs[i];
    }
    return null;
  }
  function refreshEditorLists() {
    var bs = document.querySelectorAll('.qimg-block');
    for (var i = 0; i < bs.length; i++) renderList(bs[i]);
  }
  function onDown(e) {
    if (e.button != null && e.button !== 0) return;
    var t = e.target;
    if (!t || t.nodeType !== 1) return;
    var area = document.getElementById('previewArea');
    if (!area || !area.contains(t)) return;
    var fig = closestOf(t, '.qimg-fig');
    if (!fig || !fig.__im) { clearSel(); return; }
    e.preventDefault();
    e.stopPropagation();
    selectFig(fig);
    var img = fig.querySelector('img');
    var h = closestOf(t, '.qimg-h');
    drag = {
      fig: fig, img: img, edge: h ? h.getAttribute('data-edge') : null,
      x0: e.clientX, y0: e.clientY,
      w0: img ? img.getBoundingClientRect().width : 0,
      moved: false
    };
    fig.classList.add('qimg-dragging');
    try { document.body.classList.add('qimg-noselect'); } catch (err) {}
    window.addEventListener('pointermove', onMove, true);
    window.addEventListener('pointerup', onUp, true);
    window.addEventListener('pointercancel', onUp, true);
  }
  function onMove(e) {
    if (!drag) return;
    var dx = e.clientX - drag.x0, dy = e.clientY - drag.y0;
    if (Math.abs(dx) > 2 || Math.abs(dy) > 2) drag.moved = true;
    if (drag.edge) {
      var holder = drag.fig.__holder;
      var maxW = holder ? holder.getBoundingClientRect().width : 10000;
      var nw = drag.w0 + dx * (drag.edge === 'r' ? 1 : -1);
      nw = Math.max(40, Math.min(maxW, nw));
      drag.img.style.width = Math.round(nw) + 'px';
    } else {
      drag.fig.style.transform = 'translate(' + dx + 'px,' + dy + 'px)';
      drag.fig.style.opacity = '0.85';
    }
  }
  function onUp(e) {
    window.removeEventListener('pointermove', onMove, true);
    window.removeEventListener('pointerup', onUp, true);
    window.removeEventListener('pointercancel', onUp, true);
    if (!drag) return;
    var d = drag; drag = null;
    var fig = d.fig, im = fig.__im, q = fig.__q;
    /* اندازه‌گیری باید پیش از پاک‌کردنِ ترنسفورم انجام شود */
    var dropRect = fig.getBoundingClientRect();
    var holderRect = fig.__holder ? fig.__holder.getBoundingClientRect() : dropRect;
    try { document.body.classList.remove('qimg-noselect'); } catch (err) {}
    fig.classList.remove('qimg-dragging');
    fig.style.opacity = '';
    fig.style.transform = '';
    if (!im || !q) return;
    /* الف) تغییرِ اندازه */
    if (d.edge) {
      var w = parseInt(d.img.style.width, 10);
      if (w) im.w = w;
      saveNow(q);
      refreshEditorLists();
      return;
    }
    if (!d.moved) return;
    /* ب) جابه‌جاییِ آزاد: پایه (بالا/پایینِ متن، چپ/وسط/راست) + لغزشِ دقیق تا همان‌جا که رها شده */
    var holder = fig.__holder;
    var block = holder && holder.parentNode ? holder.parentNode.querySelector('.q-rich-content') : null;
    var fr = dropRect;
    var hr = holderRect;
    var cy = fr.top + fr.height / 2;
    if (block) {
      var br = block.getBoundingClientRect();
      im.pos = (cy < br.top + br.height / 2) ? 'above' : 'below';
    }
    var frac = hr.width ? ((fr.left + fr.width / 2) - hr.left) / hr.width : 0.5;
    im.align = (frac < 0.34) ? 'right' : (frac > 0.66 ? 'left' : 'center');
    var arr = q.qimgImages || [];
    var sibs = [].slice.call(holder.querySelectorAll('.qimg-fig'));
    var target = null;
    for (var i = 0; i < sibs.length; i++) {
      if (sibs[i] === fig || !sibs[i].__im) continue;
      var sr = sibs[i].getBoundingClientRect();
      if (cy < sr.top + sr.height / 2) { target = sibs[i].__im; break; }
    }
    var from = arr.indexOf(im);
    if (from >= 0) arr.splice(from, 1);
    var to = target ? arr.indexOf(target) : arr.length;
    if (to < 0) to = arr.length;
    arr.splice(to, 0, im);
    /* لغزش = تفاوتِ جایِ رها‌شده با جایِ پایه؛ نخست پایه را بی‌لغزش بساز */
    im.rx = 0; im.dy = 0;
    try { if (typeof renderPreview === 'function') renderPreview(); } catch (err) {}
    var nf = findFigOf(im, q);
    if (nf) {
      var nb = nf.getBoundingClientRect();
      var ox = Math.round(fr.left - nb.left);
      var oy = Math.round(fr.top - nb.top);
      /* محدود به سلولِ سؤال، تا از کادر بیرون نرود
         (سلول باید پس از بازسازی دوباره پیدا شود؛ عنصرِ پیشین جدا شده است) */
      var cell2 = nf.__holder && nf.__holder.parentNode ? nf.__holder.parentNode : null;
      if (!cell2 && nf.closest) cell2 = nf.closest('td');
      if (cell2) {
        var cr = cell2.getBoundingClientRect();
        if (cr.width > 0 && cr.height > 0) {
          /* حاشیهٔ ایمنی: چیدمانِ چاپ اندکی باریک‌تر از پیش‌نمایش است،
             پس تصویر را کمی درون‌تر نگه می‌داریم تا در چاپ هم بیرون نزند */
          var PAD = 16;
          var minX = Math.round(cr.left + PAD - nb.left), maxX = Math.round(cr.right - PAD - nb.width - nb.left);
          var minY = Math.round(cr.top + PAD - nb.top), maxY = Math.round(cr.bottom - PAD - nb.height - nb.top);
          if (maxX > minX) ox = Math.max(minX, Math.min(maxX, ox));
          if (maxY > minY) oy = Math.max(minY, Math.min(maxY, oy));
        }
      }
      /* افقی به‌صورتِ درصد از عرضِ سطر ذخیره می‌شود تا در چاپ (که کمی باریک‌تر است)
         همان نسبت حفظ شود و تصویر از سلول بیرون نزند */
      var ww = nf.parentNode ? nf.parentNode.getBoundingClientRect().width : 0;
      im.rx = ww ? (Math.round(ox / ww * 10000) / 100) : 0;
      im.dy = oy;
      nf.style.left = im.rx + '%';
      nf.style.top = oy + 'px';
    }
    saveNow(q);
    refreshEditorLists();
  }
  document.addEventListener('pointerdown', onDown, true);
  document.addEventListener('click', function (e) {
    var t = e.target;
    if (t && t.nodeType === 1 && closestOf(t, '.qimg-h')) { e.preventDefault(); e.stopPropagation(); }
  }, true);

  /* ---------- ۵) ساختِ بلوک: یک دکمهٔ دوربین + جایِ تصویرها ---------- */
  function buildBlock() {
    var b = mk('div', 'qimg-block');
    b.setAttribute('data-qimg-block', '1');

    var cam = mk('button', 'qimg-cam', '📷');
    cam.type = 'button';
    cam.title = 'افزودن تصویر به سؤال (دوربین یا فایل)';
    cam.setAttribute('aria-label', 'افزودن تصویر به سؤال');
    b.appendChild(cam);

    var fi = document.createElement('input');
    fi.type = 'file'; fi.className = 'qimg-file';
    fi.accept = 'image/*';
    fi.style.display = 'none';
    b.appendChild(fi);
    b.appendChild(mk('div', 'qimg-list'));

    return { block: b, cam: cam, file: fi };
  }

  /* ---------- ۶) کادرِ متنِ سؤال ---------- */
  function fieldOf(block) {
    var shell = block.__shell;
    if (lastField && shell && shell.contains(lastField) && document.body.contains(lastField)) return lastField;
    if (shell) {
      var t = shell.querySelector('textarea.qmf-src') || shell.querySelector('textarea');
      if (t) return t;
    }
    if (lastField && document.body.contains(lastField)) return lastField;
    return null;
  }

  /* ذخیره/بازسازیِ پیش‌نمایش — متن تغییر نمی‌کند، فقط تصویرها */
  function touch(block) {
    var ta = fieldOf(block);
    try { if (window.__examBuilderUpdateQuestionText) window.__examBuilderUpdateQuestionText(ta); } catch (e) {}
    try { if (typeof renderPreview === 'function') renderPreview(); } catch (e) {}
    try { if (typeof updatePreview === 'function') updatePreview(); } catch (e) {}
    renderList(block);
  }

  /* ---------- ۷) نمایشِ تصویرها در ویرایشگر ---------- */
  function renderList(block) {
    var list = block.querySelector('.qimg-list');
    if (!list) return;
    var q = questionOf(block);
    var items = imgsOf(q) || [];
    list.innerHTML = '';
    if (!q) {
      list.appendChild(mk('div', 'qimg-empty', 'سؤال شناسایی نشد.'));
      return;
    }
    items.forEach(function (im, idx) {
      var box = mk('div', 'qimg-item');
      var im2 = document.createElement('img');
      im2.src = im.src; im2.alt = 'تصویر سؤال';
      var w = parseInt(im.w, 10) || 0;
      if (w) im2.style.width = w > 200 ? '200px' : (w + 'px');
      box.appendChild(im2);
      var btns = mk('div', 'qimg-item-btns');
      SIZES.forEach(function (s) {
        var bt = mk('button', 'qimg-btn', s[0]);
        bt.type = 'button';
        bt.title = 'اندازهٔ تصویر در برگه';
        if ((parseInt(im.w, 10) || 0) === s[1]) bt.style.borderColor = '#2563eb';
        bt.addEventListener('click', function () { im.w = s[1]; touch(block); });
        btns.appendChild(bt);
      });
      var del = mk('button', 'qimg-btn del', '✕');
      del.type = 'button'; del.title = 'حذف تصویر از سؤال';
      del.addEventListener('click', function () {
        var arr = imgsOf(q); if (!arr) return;
        arr.splice(idx, 1);
        touch(block);
      });
      btns.appendChild(del);
      box.appendChild(btns);
      list.appendChild(box);
    });
  }

  /* ---------- ۸) خواندن، کوچک‌سازی و درج ---------- */
  function readAsDataURL(file) {
    return new Promise(function (res, rej) {
      var fr = new FileReader();
      fr.onload = function () { res(String(fr.result || '')); };
      fr.onerror = function () { rej(new Error('read')); };
      fr.readAsDataURL(file);
    });
  }
  function process(file) {
    return readAsDataURL(file).then(function (url) {
      if (/^data:image\/svg/.test(url)) return { url: url, w: 0, h: 0 };
      return new Promise(function (res) {
        var im = new Image();
        im.onload = function () {
          var w0 = im.naturalWidth || im.width || 0, h0 = im.naturalHeight || im.height || 0;
          if (!w0 || !h0) { res({ url: url, w: 0, h: 0 }); return; }
          var sc = Math.min(1, MAXW / Math.max(w0, h0));
          var w = Math.max(1, Math.round(w0 * sc)), h = Math.max(1, Math.round(h0 * sc));
          if (sc >= 1 && url.length < 350000) { res({ url: url, w: w, h: h }); return; }
          try {
            var cv = document.createElement('canvas');
            cv.width = w; cv.height = h;
            var cx = cv.getContext('2d');
            cx.fillStyle = '#ffffff';
            cx.fillRect(0, 0, w, h);
            cx.drawImage(im, 0, 0, w, h);
            res({ url: cv.toDataURL('image/jpeg', JQ), w: w, h: h });
          } catch (e) { res({ url: url, w: w, h: h }); }
        };
        im.onerror = function () { res({ url: url, w: 0, h: 0 }); };
        im.src = url;
      });
    });
  }

  /* افزودنِ تصویر به سؤال (بیرون از متن) */
  function addImage(block, dataUrl, w, h) {
    var q = questionOf(block);
    var arr = imgsOf(q);
    if (!arr) return false;
    arr.push({ src: dataUrl, w: (w && w > 0) ? Math.min(DEF_W, w) : DEF_W, h: h || 0 });
    touch(block);
    return true;
  }

  /* مسیرِ پشتیبان: اگر شیءِ سؤال در دسترس نباشد، در خودِ متن */
  function insertFallback(block, file) {
    process(file).then(function (r) {
      var ta = fieldOf(block);
      if (!ta) return;
      var token = '%%FIG:' + JSON.stringify({ k: 'img', src: r.url, w: DEF_W, h: r.h || 0, alt: 'تصویرِ سؤال' }) + '%%';
      var v = ta.value || '';
      ta.value = v + (v && !/\n$/.test(v) ? '\n' : '') + token + '\n';
      try { ta.dispatchEvent(new Event('input', { bubbles: true })); } catch (e) {}
      try { if (window.__examBuilderUpdateQuestionText) window.__examBuilderUpdateQuestionText(ta); } catch (e) {}
      try { if (typeof qMathSync === 'function') qMathSync(ta.id); } catch (e) {}
      try { if (typeof renderPreview === 'function') renderPreview(); } catch (e) {}
    }).catch(function () {});
  }

  function addFiles(block, files) {
    lastBlock = block;                 /* استودیو برای کدام سؤال باز شده */
    var arr = [].slice.call(files || []).filter(function (f) {
      return f && /^image\//.test(f.type || '');
    });
    if (!arr.length) return;
    arr.forEach(function (f) {
      var st = window.__qimgStudio;
      if (!st || typeof st.open !== 'function') {
        process(f).then(function (r) {
          if (!addImage(block, r.url, r.w, r.h)) insertFallback(block, f);
        }).catch(function () {});
        return;
      }
      /* استودیو باز می‌شود؛ تنها با «تایید و ذخیره نهایی» تصویر می‌آید */
      st.open(f, function (imgs) {
        if (!imgs || !imgs.length) {
          process(f).then(function (r) {
            if (!addImage(block, r.url, r.w, r.h)) insertFallback(block, f);
          }).catch(function () {});
          return;
        }
        imgs.forEach(function (im) {
          addImage(block, im.dataUrl, im.width, im.height);
        });
      });
    });
  }

  /* ---------- ۹) نصب روی هر ویرایشگرِ سؤال ---------- */
  function mount(shell) {
    if (!shell || !shell.querySelector) return;
    if (shell.__qimgBlock && shell.contains(shell.__qimgBlock)) return;
    var tools = shell.querySelector('.q-tools');
    if (!tools) return;
    var p = buildBlock();
    var block = p.block;
    block.__shell = shell;
    shell.__qimgBlock = block;

    /* کلیکِ دوربین → انتخابگرِ سیستم، بی‌درنگ */
    p.cam.addEventListener('click', function (e) {
      e.preventDefault();
      e.stopPropagation();
      try { p.file.value = ''; } catch (err) {}
      try { p.file.click(); } catch (err) {}
    });
    p.file.addEventListener('change', function () {
      var fs = p.file.files;
      if (fs && fs.length) addFiles(block, fs);
    });

    /* کشیدن و رها کردن روی ویرایشگرِ سؤال */
    function hasFiles(e) {
      var d = e.dataTransfer;
      if (!d) return false;
      if (d.types) {
        for (var i = 0; i < d.types.length; i++) if (d.types[i] === 'Files') return true;
      }
      return false;
    }
    shell.addEventListener('dragover', function (e) {
      if (!hasFiles(e)) return;
      e.preventDefault();
      shell.classList.add('qimg-drag');
    });
    shell.addEventListener('dragleave', function (e) {
      if (!hasFiles(e)) return;
      shell.classList.remove('qimg-drag');
    });
    shell.addEventListener('drop', function (e) {
      if (!hasFiles(e)) return;
      e.preventDefault();
      e.stopPropagation();
      shell.classList.remove('qimg-drag');
      var fs = e.dataTransfer && e.dataTransfer.files;
      if (fs && fs.length) addFiles(block, fs);
    });

    if (tools.nextSibling) shell.insertBefore(block, tools.nextSibling);
    else shell.appendChild(block);
    renderList(block);
  }

  function installAll() {
    var shells = document.querySelectorAll('.exam-question-editor-shell, .q-tools');
    for (var i = 0; i < shells.length; i++) {
      var n = shells[i];
      if (n.classList && n.classList.contains('q-tools')) n = n.parentElement;
      if (n) mount(n);
    }
    /* به‌روزرسانیِ فهرستِ بلوک‌هایِ نصب‌شده */
    var bs = document.querySelectorAll('.qimg-block');
    for (var k = 0; k < bs.length; k++) renderList(bs[k]);
  }

  /* ---------- ۹ب) متنِ استخراج‌شدهٔ OCR از استودیو ---------- */
  function ocrTarget() {
    if (lastBlock && document.body.contains(lastBlock)) {
      var t = fieldOf(lastBlock);
      if (t) return t;
    }
    if (lastField && document.body.contains(lastField)) return lastField;
    return null;
  }
  function ocrNote(msg) {
    try {
      var b = (lastBlock && document.body.contains(lastBlock)) ? lastBlock : null;
      if (!b) return;
      var n = b.querySelector('.qimg-note');
      if (!n) {
        n = document.createElement('div');
        n.className = 'qimg-note';
        b.appendChild(n);
      }
      n.textContent = msg;
      n.style.display = 'block';
      if (n.__t) clearTimeout(n.__t);
      n.__t = setTimeout(function () { try { n.style.display = 'none'; } catch (e) {} }, 6000);
    } catch (e) {}
  }
  function ocrInsert(text) {
    var ta = ocrTarget();
    if (!ta) return false;
    var v = ta.value || '';
    var s = v.length, e2 = v.length;
    try {
      if (ta.selectionStart != null) s = ta.selectionStart;
      if (ta.selectionEnd != null) e2 = ta.selectionEnd;
    } catch (err) {}
    if (s > v.length) s = v.length;
    if (e2 > v.length) e2 = v.length;
    if (e2 < s) e2 = s;
    var before = v.slice(0, s), after = v.slice(e2);
    var pre = (before && !/\n$/.test(before)) ? '\n' : '';
    var post = (after && !/^\n/.test(after)) ? '\n' : '';
    ta.value = before + pre + text + post + after;
    var pos = (before + pre + text).length;
    try { ta.setSelectionRange(pos, pos); } catch (err) {}
    try { ta.dispatchEvent(new Event('input', { bubbles: true })); } catch (err) {}
    try { if (window.__examBuilderUpdateQuestionText) window.__examBuilderUpdateQuestionText(ta); } catch (err) {}
    try { if (typeof qMathSync === 'function') qMathSync(ta.id); } catch (err) {}
    try { if (typeof renderPreview === 'function') renderPreview(); } catch (err) {}
    try { if (typeof updatePreview === 'function') updatePreview(); } catch (err) {}
    lastField = ta;
    return true;
  }
  /* اگر برنامهٔ میزبان (مثلاً اندروید) موتورِ OCR خودش را داده باشد،
     استودیو نخست از آن می‌پرسد تا بی‌نیاز از اینترنت کار کند */
  window.addEventListener('message', function (e) {
    var d = e.data;
    if (!d || d.type !== 'qimg-studio-ocr-need') return;
    var reply = function (txt) {
      try {
        if (e.source) e.source.postMessage({ type: 'qimg-studio-ocr-run', text: String(txt || '') }, '*');
      } catch (err) {}
    };
    if (typeof window.QmfNativeOCR === 'function') {
      try {
        Promise.resolve(window.QmfNativeOCR(d.url)).then(function (t) { reply(t); }).catch(function () { reply(''); });
        return;
      } catch (err) {}
    }
    reply('');
  });
  window.addEventListener('message', function (e) {
    var d = e.data;
    if (!d || d.type !== 'qimg-studio-ocr') return;
    var txt = String(d.text || '').trim();
    if (!txt) return;
    var done = ocrInsert(txt);
    var n = txt.split(/\s+/).filter(function (x) { return x; }).length;
    ocrNote(done ? ('✅ متنِ تصویر در کادرِ سؤال درج شد (' + n + ' واژه)')
                 : 'کادرِ متنِ سؤال پیدا نشد — متن درج نشد.');
  });

  /* ---------- ۱۰) رخدادهایِ سراسری ---------- */
  document.addEventListener('focusin', function (e) {
    var t = e.target;
    if (!t || t.nodeType !== 1) return;
    if (t.tagName === 'TEXTAREA') { lastField = t; return; }
    if (t.classList && t.classList.contains('qmf-surface')) {
      var host = closestOf(t, '.q-math-field, .field, .q-text-wrap');
      var ta = host ? host.querySelector('textarea.qmf-src') : null;
      if (ta) lastField = ta;
    }
  }, true);

  /* چسباندن با Ctrl+V */
  document.addEventListener('paste', function (e) {
    var t = e.target;
    var shell = closestOf(t, '.exam-question-editor-shell');
    if (!shell || !shell.__qimgBlock) return;
    var cd = e.clipboardData;
    if (!cd) return;
    var items = cd.items || [], files = [];
    for (var i = 0; i < items.length; i++) {
      if (items[i].kind === 'file' && /^image\//.test(items[i].type || '')) {
        var f = items[i].getAsFile();
        if (f) files.push(f);
      }
    }
    if (!files.length) return;
    e.preventDefault();
    e.stopPropagation();
    addFiles(shell.__qimgBlock, files);
  }, true);

  /* به‌روزرسانیِ فهرست هنگامِ تغییر */
  var tList = null;
  function renderAllSoon() {
    if (tList) return;
    tList = setTimeout(function () {
      tList = null;
      var bs = document.querySelectorAll('.qimg-block');
      for (var i = 0; i < bs.length; i++) renderList(bs[i]);
    }, 400);
  }
  document.addEventListener('input', renderAllSoon, true);
  document.addEventListener('click', renderAllSoon, true);

  /* نصبِ خودکار روی سؤال‌هایِ تازه */
  var tObs = null;
  function schedule() {
    if (tObs) return;
    tObs = setTimeout(function () { tObs = null; installAll(); }, 120);
  }
  try {
    /* V179 — کارایی: تغییرات داخل بینندهٔ پیش‌نمایش/بندانگشتی‌ها (صفحه‌بندی، clone برگه‌ها) ربطی به ابزار تصویر ندارند */
    new MutationObserver(function (ms) {
      for (var i = 0; i < ms.length; i++) { var t = ms[i].target; if (!(t && t.closest && t.closest('#pgsViewer, #previewArea, .pgs-thumbs'))) { schedule(); return; } }
    }).observe(document.body, { childList: true, subtree: true });
  } catch (e) {}
  document.addEventListener('DOMContentLoaded', installAll);
  window.addEventListener('load', installAll);
  installAll();
  schedule();
})();
