/* ================================================================
   V159 — استودیوی تصویر سایت (آینهٔ ExamImageStudioDialog در ExamImageStudioCore.kt)
   تمام‌صفحه؛ نوار بالا: ✓ سبز (تأیید و درج) + چیپ‌های تب (🖼️ تصویر و برش / ✏️ طراحی و علامت / ✨ بهبود / 📐 صاف‌سازی) + ✕ قرمز.
   شروع: «تصویر از کجا بیاید؟» → 📷 دوربین / 🖼️ گالری + تصویرهای فعلی همین سؤال (ویرایش/حذف).
   خروجی: dataURL (JPEG) مثل processAndEncode؛ همان زنجیرهٔ چرخش/صاف‌سازی/قرینه/شکل‌ها/برش/اسکن/اندازه.
   OCR آفلاین (Tesseract اپ) در مرورگر موجود نیست و عمداً حذف شده است.
   ================================================================ */
(function () {
  var S = window.ExamSite; if (!S) return;
  var el = S.el, fa = S.fa, toast = S.toast;
  var COLORS = [['🔴', '#DC2626'], ['🔵', '#2563EB'], ['⚫', '#111827'], ['🟢', '#16A34A']];
  var TABS = [['image', '🖼️ تصویر و برش'], ['draw', '✏️ طراحی و علامت'], ['enhance', '✨ بهبود'], ['deskew', '📐 صاف‌سازی']];

  function loadImage(src) { return new Promise(function (res, rej) { var im = new Image(); im.onload = function () { res(im); }; im.onerror = function () { rej(new Error('تصویر قابل خواندن نیست.')); }; im.src = src; }); }
  function fileToUrl(f) { return new Promise(function (res, rej) { var r = new FileReader(); r.onload = function () { res(r.result); }; r.onerror = rej; r.readAsDataURL(f); }); }
  function bounded(im, maxEdge) { var w = im.naturalWidth || im.width, h = im.naturalHeight || im.height, k = Math.min(1, maxEdge / Math.max(w, h)); var c = document.createElement('canvas'); c.width = Math.max(1, Math.round(w * k)); c.height = Math.max(1, Math.round(h * k)); c.getContext('2d').drawImage(im, 0, 0, c.width, c.height); return c; }
  function pick(accept, capture) { return new Promise(function (res) { var i = el('input', {type: 'file', accept: accept || 'image/*', style: 'display:none'}); if (capture) i.setAttribute('capture', 'environment'); i.addEventListener('change', function () { res(i.files && i.files[0] || null); i.remove(); }); document.body.appendChild(i); i.click(); }); }

  /* ---- فیلترها (آینهٔ ExamImageStudioFilters) ---- */
  function lum(r, g, b) { return (r * 299 + g * 587 + b * 114) / 1000; }
  function estimateBackground(d, w, h, cells) {
    cells = cells || 16; var cw = Math.ceil(w / cells), ch = Math.ceil(h / cells), bg = new Float32Array(cells * cells);
    for (var cy = 0; cy < cells; cy++) for (var cx = 0; cx < cells; cx++) {
      var vals = [];
      for (var y = cy * ch; y < Math.min(h, (cy + 1) * ch); y += 2) for (var x = cx * cw; x < Math.min(w, (cx + 1) * cw); x += 2) { var i = (y * w + x) * 4; vals.push(lum(d[i], d[i + 1], d[i + 2])); }
      vals.sort(function (a, b) { return a - b; }); bg[cy * cells + cx] = vals.length ? vals[Math.floor(vals.length * 0.9)] : 255;
    }
    return {bg: bg, cells: cells, cw: cw, ch: ch};
  }
  function flattenShadow(d, w, h) {
    var e = estimateBackground(d, w, h), c = e.cells;
    for (var y = 0; y < h; y++) for (var x = 0; x < w; x++) {
      var i = (y * w + x) * 4, cx = Math.min(c - 1, Math.floor(x / e.cw)), cy = Math.min(c - 1, Math.floor(y / e.ch)), b = Math.max(40, e.bg[cy * c + cx]), k = 255 / b;
      d[i] = Math.min(255, d[i] * k); d[i + 1] = Math.min(255, d[i + 1] * k); d[i + 2] = Math.min(255, d[i + 2] * k);
    }
  }
  function threshold(d, w, h, t) { for (var i = 0; i < d.length; i += 4) { var v = lum(d[i], d[i + 1], d[i + 2]) >= t ? 255 : 0; d[i] = d[i + 1] = d[i + 2] = v; } }
  function despeckle(d, w, h) {
    var src = new Uint8ClampedArray(d);
    for (var y = 1; y < h - 1; y++) for (var x = 1; x < w - 1; x++) {
      var i = (y * w + x) * 4; if (src[i] !== 0) continue; var n = 0;
      for (var dy = -1; dy <= 1; dy++) for (var dx = -1; dx <= 1; dx++) { if (!dx && !dy) continue; if (src[((y + dy) * w + (x + dx)) * 4] === 0) n++; }
      if (n < 2) { d[i] = d[i + 1] = d[i + 2] = 255; }
    }
  }
  function autoCropBounds(c) {
    var w = c.width, h = c.height, d = c.getContext('2d').getImageData(0, 0, w, h).data, minX = w, minY = h, maxX = -1, maxY = -1;
    for (var y = 0; y < h; y += 2) for (var x = 0; x < w; x += 2) { var i = (y * w + x) * 4; if (lum(d[i], d[i + 1], d[i + 2]) < 200) { if (x < minX) minX = x; if (x > maxX) maxX = x; if (y < minY) minY = y; if (y > maxY) maxY = y; } }
    if (maxX < 0) return null; var pad = Math.round(Math.max(w, h) * 0.01);
    return {l: Math.max(0, minX - pad) / w, t: Math.max(0, minY - pad) / h, r: Math.min(w, maxX + pad) / w, b: Math.min(h, maxY + pad) / h};
  }
  function detectSkewAngle(c) {
    /* پروجکشن افقی: زاویه‌ای که واریانس سطرها را بیشینه می‌کند (−8..8 درجه) */
    var small = bounded(c, 600), w = small.width, h = small.height, d = small.getContext('2d').getImageData(0, 0, w, h).data, best = 0, bestScore = -1;
    var dark = new Uint8Array(w * h); for (var i = 0; i < w * h; i++) dark[i] = lum(d[i * 4], d[i * 4 + 1], d[i * 4 + 2]) < 140 ? 1 : 0;
    for (var a = -8; a <= 8; a += 0.5) {
      var rad = a * Math.PI / 180, rows = new Float32Array(h), t = Math.tan(rad);
      for (var y = 0; y < h; y += 2) for (var x = 0; x < w; x += 2) { if (!dark[y * w + x]) continue; var yy = Math.round(y + (x - w / 2) * t); if (yy >= 0 && yy < h) rows[yy]++; }
      var m = 0; for (var r = 0; r < h; r++) m += rows[r]; m /= h; var v = 0; for (r = 0; r < h; r++) v += (rows[r] - m) * (rows[r] - m);
      if (v > bestScore) { bestScore = v; best = a; }
    }
    return -best;
  }

  /* ---- رندر شکل‌ها (آینهٔ bakeShapes) ---- */
  function drawShape(ctx, sp, W, H, selected) {
    var x1 = sp.x1 * W, y1 = sp.y1 * H, x2 = sp.x2 * W, y2 = sp.y2 * H, lw = Math.max(2, Math.min(W, H) * 0.006);
    ctx.save(); ctx.lineCap = 'round'; ctx.lineJoin = 'round'; ctx.strokeStyle = sp.color; ctx.fillStyle = sp.color; ctx.lineWidth = lw;
    function arrowHead(ax, ay, bx, by) { var ang = Math.atan2(by - ay, bx - ax), L = lw * 5; ctx.beginPath(); ctx.moveTo(bx, by); ctx.lineTo(bx - L * Math.cos(ang - 0.45), by - L * Math.sin(ang - 0.45)); ctx.lineTo(bx - L * Math.cos(ang + 0.45), by - L * Math.sin(ang + 0.45)); ctx.closePath(); ctx.fill(); }
    switch (sp.kind) {
      case 'line': ctx.beginPath(); ctx.moveTo(x1, y1); ctx.lineTo(x2, y2); ctx.stroke(); break;
      case 'arrow': ctx.beginPath(); ctx.moveTo(x1, y1); ctx.lineTo(x2, y2); ctx.stroke(); arrowHead(x1, y1, x2, y2); break;
      case 'arrow2': ctx.beginPath(); ctx.moveTo(x1, y1); ctx.lineTo(x2, y2); ctx.stroke(); arrowHead(x1, y1, x2, y2); arrowHead(x2, y2, x1, y1); break;
      case 'curve': { var mx = (x1 + x2) / 2, my = (y1 + y2) / 2, dx = x2 - x1, dy = y2 - y1, cx = mx - dy * 0.35, cy = my + dx * 0.35; ctx.beginPath(); ctx.moveTo(x1, y1); ctx.quadraticCurveTo(cx, cy, x2, y2); ctx.stroke(); arrowHead(cx, cy, x2, y2); break; }
      case 'rect': ctx.strokeRect(Math.min(x1, x2), Math.min(y1, y2), Math.abs(x2 - x1), Math.abs(y2 - y1)); break;
      case 'ellipse': ctx.beginPath(); ctx.ellipse((x1 + x2) / 2, (y1 + y2) / 2, Math.abs(x2 - x1) / 2, Math.abs(y2 - y1) / 2, 0, 0, Math.PI * 2); ctx.stroke(); break;
      case 'highlighter': ctx.globalAlpha = 0.35; ctx.fillRect(Math.min(x1, x2), Math.min(y1, y2), Math.abs(x2 - x1), Math.abs(y2 - y1)); break;
      case 'censor': ctx.fillStyle = '#111'; ctx.fillRect(Math.min(x1, x2), Math.min(y1, y2), Math.abs(x2 - x1), Math.abs(y2 - y1)); break;
      case 'free': if (sp.pts && sp.pts.length) { ctx.beginPath(); sp.pts.forEach(function (p, i) { i ? ctx.lineTo(p[0] * W, p[1] * H) : ctx.moveTo(p[0] * W, p[1] * H); }); ctx.stroke(); } break;
      case 'text': ctx.font = 'bold ' + Math.round(Math.min(W, H) * 0.045) + 'px Vazirmatn, sans-serif'; ctx.textAlign = 'right'; ctx.direction = 'rtl'; ctx.fillText(sp.text || '', x1, y1); break;
    }
    if (selected) { ctx.setLineDash([6, 4]); ctx.strokeStyle = '#2563EB'; ctx.lineWidth = 1.5; var b = shapeBounds(sp); ctx.strokeRect(b.l * W - 6, b.t * H - 6, (b.r - b.l) * W + 12, (b.b - b.t) * H + 12); }
    ctx.restore();
  }
  function shapeBounds(sp) { if (sp.kind === 'free' && sp.pts && sp.pts.length) { var l = 1, t = 1, r = 0, b = 0; sp.pts.forEach(function (p) { l = Math.min(l, p[0]); r = Math.max(r, p[0]); t = Math.min(t, p[1]); b = Math.max(b, p[1]); }); return {l: l, t: t, r: r, b: b}; } if (sp.kind === 'text') return {l: sp.x1 - 0.2, t: sp.y1 - 0.05, r: sp.x1, b: sp.y1 + 0.01}; return {l: Math.min(sp.x1, sp.x2), t: Math.min(sp.y1, sp.y2), r: Math.max(sp.x1, sp.x2), b: Math.max(sp.y1, sp.y2)}; }
  function hitShape(shapes, nx, ny) { for (var i = shapes.length - 1; i >= 0; i--) { var b = shapeBounds(shapes[i]); if (nx >= b.l - 0.02 && nx <= b.r + 0.02 && ny >= b.t - 0.02 && ny <= b.b + 0.02) return i; } return -1; }

  /* ---- استودیو ---- */
  /* opts: {existing: [{uri}], onInsert(dataUrl), onReplace(index, dataUrl), onDelete(index), onSplitToSame([dataUrl]), onSplitToQuestions([dataUrl])} */
  function open(opts) {
    opts = opts || {};
    var st = {orig: null, rot: 0, flip: false, crop: {l: 0, t: 0, r: 1, b: 1}, aspect: 'free', scan: false, thr: 185, deshadow: false, denoise: false, outSize: 420, quality: 92, deskew: 0, grid: false,
      tab: 'image', draw: 'none', color: '#DC2626', shapes: [], redo: [], sel: -1, before: false, split: false, boxes: [], selBox: 0, editIndex: -1, busy: false};
    var root = el('div', {class: 'studio-bg'});
    var bar = el('div', {class: 'studio-bar'}), stage = el('div', {class: 'studio-stage'}), tools = el('div', {class: 'studio-tools'});
    root.appendChild(bar); root.appendChild(stage); root.appendChild(tools); document.body.appendChild(root);
    document.body.style.overflow = 'hidden';
    function close() { root.remove(); document.body.style.overflow = ''; }

    /* --- تصویر پایه (چرخش + صاف‌سازی + قرینه) --- */
    var baseCache = null;
    function base() {
      if (baseCache) return baseCache;
      var im = st.orig, ang = (st.rot + st.deskew) * Math.PI / 180, w = im.width, h = im.height;
      var cw = Math.abs(w * Math.cos(ang)) + Math.abs(h * Math.sin(ang)), ch = Math.abs(w * Math.sin(ang)) + Math.abs(h * Math.cos(ang));
      var c = document.createElement('canvas'); c.width = Math.round(cw); c.height = Math.round(ch); var x = c.getContext('2d');
      x.fillStyle = '#fff'; x.fillRect(0, 0, c.width, c.height); x.translate(c.width / 2, c.height / 2); x.rotate(ang); if (st.flip) x.scale(-1, 1); x.drawImage(im, -w / 2, -h / 2);
      baseCache = c; return c;
    }
    function invalidate() { baseCache = null; }
    /* --- خروجی نهایی (آینهٔ encodeCropped): برش → شکل‌ها → اسکن → اندازه → JPEG --- */
    function render(forExport, cropOverride) {
      var b = base(), cr = cropOverride || st.crop, sx = Math.round(cr.l * b.width), sy = Math.round(cr.t * b.height), sw = Math.max(1, Math.round((cr.r - cr.l) * b.width)), sh = Math.max(1, Math.round((cr.b - cr.t) * b.height));
      var c = document.createElement('canvas'); c.width = sw; c.height = sh; var x = c.getContext('2d'); x.drawImage(b, sx, sy, sw, sh, 0, 0, sw, sh);
      if (!st.before) {
        /* شکل‌ها در مختصات کل تصویر پایه هستند → به مختصات برش تبدیل می‌شوند */
        x.save(); x.translate(-sx, -sy); st.shapes.forEach(function (sp) { drawShape(x, sp, b.width, b.height, false); }); x.restore();
        if (st.scan || st.deshadow || st.denoise) { var id = x.getImageData(0, 0, sw, sh); if (st.deshadow) flattenShadow(id.data, sw, sh); if (st.scan) { threshold(id.data, sw, sh, st.thr); if (st.denoise) despeckle(id.data, sw, sh); } x.putImageData(id, 0, 0); }
      }
      if (forExport && st.outSize > 0 && Math.max(sw, sh) > st.outSize) { var k = st.outSize / Math.max(sw, sh), o = document.createElement('canvas'); o.width = Math.round(sw * k); o.height = Math.round(sh * k); o.getContext('2d').drawImage(c, 0, 0, o.width, o.height); c = o; }
      return c;
    }
    function exportUrl(cropOverride) { return render(true, cropOverride).toDataURL('image/jpeg', st.quality / 100); }

    /* --- صحنه --- */
    var canvas = el('canvas', {class: 'studio-canvas'}), overlay = el('canvas', {class: 'studio-overlay'});
    var view = {ox: 0, oy: 0, dw: 1, dh: 1};
    function paintStage() {
      if (!st.orig) return;
      var b = base(), W = stage.clientWidth, H = stage.clientHeight, k = Math.min((W - 16) / b.width, (H - 16) / b.height);
      view.dw = b.width * k; view.dh = b.height * k; view.ox = (W - view.dw) / 2; view.oy = (H - view.dh) / 2;
      canvas.width = overlay.width = W; canvas.height = overlay.height = H;
      var x = canvas.getContext('2d'); x.clearRect(0, 0, W, H);
      /* پیش‌نمایش با فیلترها روی کل تصویر (کوچک‌شده برای سرعت) */
      var prev = render(false, {l: 0, t: 0, r: 1, b: 1}); x.drawImage(prev, view.ox, view.oy, view.dw, view.dh);
      var o = overlay.getContext('2d'); o.clearRect(0, 0, W, H);
      if (st.grid) { o.strokeStyle = 'rgba(37,99,235,.55)'; o.lineWidth = 1; for (var i = 1; i < 12; i++) { o.beginPath(); o.moveTo(view.ox, view.oy + view.dh * i / 12); o.lineTo(view.ox + view.dw, view.oy + view.dh * i / 12); o.stroke(); o.beginPath(); o.moveTo(view.ox + view.dw * i / 12, view.oy); o.lineTo(view.ox + view.dw * i / 12, view.oy + view.dh); o.stroke(); } }
      if (st.sel >= 0 && st.shapes[st.sel]) { o.save(); o.translate(view.ox, view.oy); drawShape(o, st.shapes[st.sel], view.dw, view.dh, true); o.restore(); }
      if (st.split) { st.boxes.forEach(function (bx, i) { o.strokeStyle = i === st.selBox ? '#F59E0B' : '#22C55E'; o.lineWidth = i === st.selBox ? 3 : 2; o.setLineDash([]); o.strokeRect(view.ox + bx.l * view.dw, view.oy + bx.t * view.dh, (bx.r - bx.l) * view.dw, (bx.b - bx.t) * view.dh); o.fillStyle = '#fff'; o.font = 'bold 16px sans-serif'; o.fillText(fa(i + 1), view.ox + bx.l * view.dw + 6, view.oy + bx.t * view.dh + 20); }); return; }
      /* برش */
      var cr = st.crop, cx = view.ox + cr.l * view.dw, cy = view.oy + cr.t * view.dh, cw = (cr.r - cr.l) * view.dw, ch = (cr.b - cr.t) * view.dh;
      o.fillStyle = 'rgba(0,0,0,.45)'; o.beginPath(); o.rect(view.ox, view.oy, view.dw, view.dh); o.rect(cx, cy, cw, ch); o.fill('evenodd');
      o.strokeStyle = '#fff'; o.lineWidth = 2; o.strokeRect(cx, cy, cw, ch);
      o.fillStyle = '#fff'; [[cx, cy], [cx + cw, cy], [cx, cy + ch], [cx + cw, cy + ch]].forEach(function (p) { o.beginPath(); o.arc(p[0], p[1], 9, 0, Math.PI * 2); o.fill(); });
    }
    stage.appendChild(canvas); stage.appendChild(overlay);
    window.addEventListener('resize', paintStage);

    /* --- ژست‌ها: برش / کشیدن شکل / جابه‌جایی کادر تفکیک --- */
    var drag = null;
    function norm(e) { var r = overlay.getBoundingClientRect(), p = e.touches ? e.touches[0] : e; return {x: (p.clientX - r.left - view.ox) / view.dw, y: (p.clientY - r.top - view.oy) / view.dh}; }
    function clamp01(v) { return Math.max(0, Math.min(1, v)); }
    function down(e) {
      if (!st.orig) return; e.preventDefault(); var p = norm(e);
      if (st.split) { var hit = -1; st.boxes.forEach(function (b, i) { if (p.x >= b.l && p.x <= b.r && p.y >= b.t && p.y <= b.b) hit = i; }); if (hit >= 0) { st.selBox = hit; var b0 = st.boxes[hit]; var nearBR = Math.abs(p.x - b0.r) < 0.05 && Math.abs(p.y - b0.b) < 0.05; drag = {kind: nearBR ? 'boxsize' : 'boxmove', p: p, b: Object.assign({}, b0)}; } paintStage(); return; }
      if (st.tab === 'draw' && st.draw !== 'none') {
        if (st.draw === 'text') { var t = prompt('متن:'); if (t) { push({kind: 'text', x1: clamp01(p.x), y1: clamp01(p.y), x2: p.x, y2: p.y, color: st.color, text: t}); } return; }
        if (st.draw === 'eraser') { var hi = hitShape(st.shapes, p.x, p.y); if (hi >= 0) { st.redo = []; st.shapes.splice(hi, 1); st.sel = -1; invalidateShapes(); } return; }
        if (st.draw === 'eyedropper') { var b = base(), d = b.getContext('2d').getImageData(Math.round(clamp01(p.x) * (b.width - 1)), Math.round(clamp01(p.y) * (b.height - 1)), 1, 1).data; st.color = '#' + [d[0], d[1], d[2]].map(function (v) { return ('0' + v.toString(16)).slice(-2); }).join(''); drawTools(); toast('رنگ برداشته شد.', 'ok'); return; }
        drag = {kind: 'shape', sp: {kind: st.draw, x1: p.x, y1: p.y, x2: p.x, y2: p.y, color: st.color, pts: st.draw === 'free' ? [[p.x, p.y]] : null}}; return;
      }
      if (st.tab === 'draw') { st.sel = hitShape(st.shapes, p.x, p.y); if (st.sel >= 0) drag = {kind: 'move', p: p}; paintStage(); return; }
      /* برش: گوشه‌ها یا جابه‌جایی */
      var cr = st.crop, corners = [['l', 't'], ['r', 't'], ['l', 'b'], ['r', 'b']], best = null, bd = 0.06;
      corners.forEach(function (c) { var d = Math.hypot(p.x - cr[c[0]], p.y - cr[c[1]]); if (d < bd) { bd = d; best = c; } });
      if (best) drag = {kind: 'corner', c: best}; else if (p.x > cr.l && p.x < cr.r && p.y > cr.t && p.y < cr.b) drag = {kind: 'cropmove', p: p, c: Object.assign({}, cr)};
    }
    function move(e) {
      if (!drag) return; e.preventDefault(); var p = norm(e);
      if (drag.kind === 'corner') { var cr = st.crop, c = drag.c; var nx = clamp01(p.x), ny = clamp01(p.y); if (c[0] === 'l') cr.l = Math.min(nx, cr.r - 0.05); else cr.r = Math.max(nx, cr.l + 0.05); if (c[1] === 't') cr.t = Math.min(ny, cr.b - 0.05); else cr.b = Math.max(ny, cr.t + 0.05); if (st.aspect !== 'free') applyAspect(); }
      else if (drag.kind === 'cropmove') { var dx = p.x - drag.p.x, dy = p.y - drag.p.y, c0 = drag.c, w = c0.r - c0.l, h = c0.b - c0.t; var l = clamp01(Math.min(c0.l + dx, 1 - w)), t = clamp01(Math.min(c0.t + dy, 1 - h)); st.crop = {l: l, t: t, r: l + w, b: t + h}; }
      else if (drag.kind === 'shape') { var sp = drag.sp; if (sp.kind === 'free') sp.pts.push([p.x, p.y]); sp.x2 = p.x; sp.y2 = p.y; var o = overlay.getContext('2d'); paintStage(); o.save(); o.translate(view.ox, view.oy); drawShape(o, sp, view.dw, view.dh, false); o.restore(); return; }
      else if (drag.kind === 'move') { var s = st.shapes[st.sel], ddx = p.x - drag.p.x, ddy = p.y - drag.p.y; drag.p = p; s.x1 += ddx; s.x2 += ddx; s.y1 += ddy; s.y2 += ddy; if (s.pts) s.pts = s.pts.map(function (q) { return [q[0] + ddx, q[1] + ddy]; }); invalidateShapes(); return; }
      else if (drag.kind === 'boxmove') { var bb = drag.b, w2 = bb.r - bb.l, h2 = bb.b - bb.t, l2 = clamp01(Math.min(bb.l + p.x - drag.p.x, 1 - w2)), t2 = clamp01(Math.min(bb.t + p.y - drag.p.y, 1 - h2)); st.boxes[st.selBox] = {l: l2, t: t2, r: l2 + w2, b: t2 + h2}; }
      else if (drag.kind === 'boxsize') { var b3 = st.boxes[st.selBox]; b3.r = clamp01(Math.max(b3.l + 0.05, p.x)); b3.b = clamp01(Math.max(b3.t + 0.05, p.y)); }
      paintStage();
    }
    function up(e) { if (!drag) return; if (drag.kind === 'shape') { var sp = drag.sp; if (sp.kind === 'free' ? sp.pts.length > 1 : (Math.abs(sp.x2 - sp.x1) > 0.005 || Math.abs(sp.y2 - sp.y1) > 0.005)) push(sp); } drag = null; paintStage(); }
    overlay.addEventListener('mousedown', down); overlay.addEventListener('mousemove', move); window.addEventListener('mouseup', up);
    overlay.addEventListener('touchstart', down, {passive: false}); overlay.addEventListener('touchmove', move, {passive: false}); overlay.addEventListener('touchend', up);
    function push(sp) { st.redo = []; st.shapes.push(sp); st.sel = st.shapes.length - 1; invalidateShapes(); }
    function invalidateShapes() { paintStage(); drawTools(); }
    function applyAspect() { var r = {r11: 1, r43: 4 / 3, r169: 16 / 9}[st.aspect]; if (!r) return; var b = base(), cr = st.crop, cx = (cr.l + cr.r) / 2, cy = (cr.t + cr.b) / 2, w = cr.r - cr.l, hPx = w * b.width / r, h = hPx / b.height; if (h > 1) { h = 1; w = h * b.height * r / b.width; } var l = clamp01(cx - w / 2), t = clamp01(cy - h / 2); if (l + w > 1) l = 1 - w; if (t + h > 1) t = 1 - h; st.crop = {l: l, t: t, r: l + w, b: t + h}; }

    /* --- نوار بالا --- */
    function drawBar() {
      bar.innerHTML = '';
      var can = !!st.orig && !st.busy;
      bar.appendChild(el('button', {class: 'studio-ok', 'aria-label': st.editIndex >= 0 ? 'تایید و جایگزینی' : 'تایید و درج', disabled: can ? null : 'disabled', onclick: function () { if (!can) return; st.busy = true; drawBar(); setTimeout(function () { try { var url = exportUrl(); if (st.editIndex >= 0 && opts.onReplace) opts.onReplace(st.editIndex, url); else if (opts.onInsert) opts.onInsert(url); close(); } catch (e) { st.busy = false; drawBar(); toast(S.errMsg(e), 'err'); } }, 20); }}, [el('span', {text: '✓'})]));
      var tabs = el('div', {class: 'studio-tabs'});
      if (st.orig) TABS.forEach(function (t) { tabs.appendChild(el('button', {class: 'studio-tab' + (st.tab === t[0] ? ' on' : ''), text: t[1], onclick: function () { st.tab = t[0]; if (t[0] !== 'image') st.split = false; if (t[0] !== 'draw') { st.draw = 'none'; st.sel = -1; } drawBar(); drawTools(); paintStage(); }})); });
      bar.appendChild(tabs);
      bar.appendChild(el('button', {class: 'studio-x', 'aria-label': 'بستن', onclick: close}, [el('span', {text: '✕'})]));
    }

    /* --- ابزارها --- */
    function chipBtn(label, on, fn) { return el('button', {class: 'studio-chip' + (on ? ' on' : ''), text: label, onclick: fn}); }
    function row(children) { return el('div', {class: 'studio-row'}, children); }
    function slider(label, min, max, step, val, fn) { var i = el('input', {type: 'range', min: String(min), max: String(max), step: String(step), value: String(val)}); var l = el('span', {class: 'studio-lbl', text: label + ': ' + fa(val)}); i.addEventListener('input', function () { l.textContent = label + ': ' + fa(Number(i.value)); fn(Number(i.value)); }); return el('div', {class: 'studio-slider'}, [l, i]); }
    function drawTools() {
      tools.innerHTML = '';
      if (!st.orig) return;
      if (st.tab === 'image') {
        if (st.split) {
          tools.appendChild(row([chipBtn('✂️ ۲ سؤال (بالا / پایین)', false, function () { st.boxes = [{l: 0, t: 0, r: 1, b: 0.5}, {l: 0, t: 0.5, r: 1, b: 1}]; st.selBox = 0; paintStage(); }), chipBtn('✂️ ۳ سؤال ستونی', false, function () { st.boxes = [0, 1, 2].map(function (i) { return {l: 0, t: i / 3, r: 1, b: (i + 1) / 3}; }); st.selBox = 0; paintStage(); }), chipBtn('✂️ ۴ سؤال (۲×۲)', false, function () { st.boxes = [{l: 0, t: 0, r: 0.5, b: 0.5}, {l: 0.5, t: 0, r: 1, b: 0.5}, {l: 0, t: 0.5, r: 0.5, b: 1}, {l: 0.5, t: 0.5, r: 1, b: 1}]; st.selBox = 0; paintStage(); }),
            chipBtn('➕ کادر جدید', false, function () { st.boxes.push({l: 0.1, t: 0.1, r: 0.6, b: 0.4}); st.selBox = st.boxes.length - 1; paintStage(); }), chipBtn('🗑️ حذف کادر انتخاب‌شده', false, function () { if (st.boxes.length > 1) { st.boxes.splice(st.selBox, 1); st.selBox = 0; paintStage(); } })]));
          tools.appendChild(row([el('button', {class: 'btn sm', text: '💾 همه بخش‌ها به همین سؤال', onclick: function () { var urls = st.boxes.map(function (b) { return exportUrl(b); }); if (opts.onSplitToSame) opts.onSplitToSame(urls); close(); }}),
            el('button', {class: 'btn light sm', text: '🧩 هر بخش → سؤال جداگانه', onclick: function () { var urls = st.boxes.map(function (b) { return exportUrl(b); }); if (opts.onSplitToQuestions) opts.onSplitToQuestions(urls); close(); }}), chipBtn('انصراف', false, function () { st.split = false; drawTools(); paintStage(); })]));
          tools.appendChild(el('p', {class: 'studio-hint', text: 'کادرها را جابه‌جا کنید؛ گوشهٔ پایین‌چپ هر کادر اندازه را تغییر می‌دهد.'}));
          return;
        }
        tools.appendChild(row([chipBtn('↺ ۹۰° چپ', false, function () { st.rot = (st.rot + 270) % 360; invalidate(); paintStage(); }), chipBtn('↻ ۹۰° راست', false, function () { st.rot = (st.rot + 90) % 360; invalidate(); paintStage(); }), chipBtn('⇄ قرینه', st.flip, function () { st.flip = !st.flip; invalidate(); paintStage(); }),
          chipBtn('🖼️ بدون برش', false, function () { st.crop = {l: 0, t: 0, r: 1, b: 1}; st.aspect = 'free'; drawTools(); paintStage(); }), chipBtn('برش آزاد', st.aspect === 'free', function () { st.aspect = 'free'; drawTools(); }), chipBtn('مربع ۱:۱', st.aspect === 'r11', function () { st.aspect = 'r11'; applyAspect(); drawTools(); paintStage(); }), chipBtn('۴:۳', st.aspect === 'r43', function () { st.aspect = 'r43'; applyAspect(); drawTools(); paintStage(); }), chipBtn('۱۶:۹', st.aspect === 'r169', function () { st.aspect = 'r169'; applyAspect(); drawTools(); paintStage(); }),
          chipBtn('✂️ تفکیک چندسؤاله', false, function () { st.split = true; if (!st.boxes.length) st.boxes = [{l: 0, t: 0, r: 1, b: 0.5}, {l: 0, t: 0.5, r: 1, b: 1}]; drawTools(); paintStage(); })]));
        tools.appendChild(el('div', {class: 'studio-sec'}, [el('b', {text: '📄 سفیدسازی اسکن'}), chipBtn(st.scan ? 'روشن' : 'خاموش', st.scan, function () { st.scan = !st.scan; drawTools(); paintStage(); }), st.scan ? slider('آستانه', 120, 240, 1, st.thr, function (v) { st.thr = v; paintStage(); }) : null]));
        var sizes = [[240, 'S'], [420, 'M'], [640, 'L'], [0, '∞']];
        tools.appendChild(row([el('b', {text: 'اندازه:'})].concat(sizes.map(function (z) { return chipBtn(z[1], st.outSize === z[0], function () { st.outSize = z[0]; drawTools(); }); }))));
        tools.appendChild(slider('کیفیت', 50, 100, 1, st.quality, function (v) { st.quality = v; }));
        return;
      }
      if (st.tab === 'draw') {
        var kinds = [['none', '👆 انتخاب/جابجایی'], ['arrow', '➡️ فلش'], ['arrow2', '↔️ فلش دوسر'], ['line', '📏 خط'], ['rect', '⬜ کادر'], ['ellipse', '⭕ بیضی'], ['free', '✏️ خط آزاد'], ['highlighter', '🖍️ هایلایتر'], ['censor', '🚫 سانسور'], ['text', '🔤 متن'], ['curve', '🪝 فلش منحنی'], ['eyedropper', '💧 قطره‌چکان'], ['eraser', '🧹 پاک‌کن']];
        tools.appendChild(row(kinds.map(function (k) { return chipBtn(k[1], st.draw === k[0], function () { st.draw = k[0]; st.sel = -1; drawTools(); paintStage(); }); })));
        tools.appendChild(row(COLORS.map(function (c) { return chipBtn(c[0], st.color.toUpperCase() === c[1], function () { st.color = c[1]; drawTools(); }); }).concat([
          chipBtn('↩️ بازگردانی', false, function () { if (st.shapes.length) { st.redo.push(st.shapes.pop()); st.sel = -1; invalidateShapes(); } }),
          chipBtn('↪️ انجام مجدد', false, function () { if (st.redo.length) { st.shapes.push(st.redo.pop()); invalidateShapes(); } }),
          chipBtn('🗑️ حذف انتخاب', false, function () { if (st.sel >= 0) { st.shapes.splice(st.sel, 1); st.sel = -1; invalidateShapes(); } }),
          chipBtn('🧹 پاک کردن همه', false, function () { st.redo = st.shapes.slice(); st.shapes = []; st.sel = -1; invalidateShapes(); })])));
        tools.appendChild(el('p', {class: 'studio-hint', text: '🗂 لایهٔ اشیاء (' + fa(st.shapes.length) + ')' + (st.draw === 'none' ? ' — با لمس یک شکل آن را انتخاب و جابه‌جا کنید.' : '')}));
        return;
      }
      if (st.tab === 'enhance') {
        tools.appendChild(row([chipBtn('👁 قبل/بعد', st.before, function () { st.before = !st.before; drawTools(); paintStage(); }), chipBtn('📖 حذف سایه و زردی', st.deshadow, function () { st.deshadow = !st.deshadow; drawTools(); paintStage(); }), chipBtn('🧽 حذف نویز و لکه', st.denoise, function () { st.denoise = !st.denoise; if (st.denoise && !st.scan) st.scan = true; drawTools(); paintStage(); }),
          chipBtn('✂️ برش خودکار حاشیه', false, function () { var b = autoCropBounds(bounded(base(), 800)); if (b) { st.crop = b; st.aspect = 'free'; paintStage(); toast('حاشیه برش خورد.', 'ok'); } else toast('محتوایی برای برش پیدا نشد.', 'info'); })]));
        tools.appendChild(el('p', {class: 'studio-hint', text: 'OCR آفلاین اپ در مرورگر در دسترس نیست؛ متن را با 🎤 گفتار به متن یا تایپ وارد کنید.'}));
        return;
      }
      if (st.tab === 'deskew') {
        tools.appendChild(row([chipBtn('🎯 تشخیص خودکار زاویه', false, function () { st.deskew = detectSkewAngle(st.orig); invalidate(); drawTools(); paintStage(); }), chipBtn('صفر کردن صاف‌سازی', false, function () { st.deskew = 0; invalidate(); drawTools(); paintStage(); }), chipBtn('شبکه', st.grid, function () { st.grid = !st.grid; drawTools(); paintStage(); })]));
        tools.appendChild(slider('↯ صاف‌سازی (درجه)', -15, 15, 0.1, Math.round(st.deskew * 10) / 10, function (v) { st.deskew = v; invalidate(); paintStage(); }));
      }
    }

    /* --- انتخاب منبع (original == null) --- */
    async function setSource(src, editIndex) {
      try { var im = await loadImage(src); st.orig = bounded(im, 2200); st.editIndex = editIndex != null ? editIndex : -1; st.crop = {l: 0, t: 0, r: 1, b: 1}; st.shapes = []; st.redo = []; st.rot = 0; st.deskew = 0; st.flip = false; invalidate(); stage.classList.remove('picker'); stage.innerHTML = ''; stage.appendChild(canvas); stage.appendChild(overlay); drawBar(); drawTools(); requestAnimationFrame(paintStage); }
      catch (e) { toast(S.errMsg(e), 'err'); }
    }
    function drawPicker() {
      stage.classList.add('picker'); stage.innerHTML = '';
      var box = el('div', {class: 'studio-pick'}, [el('h3', {text: 'تصویر از کجا بیاید؟'}),
        el('button', {class: 'btn studio-src', text: '📷 دوربین', onclick: async function () { var f = await pick('image/*', true); if (f) setSource(await fileToUrl(f)); }}),
        el('button', {class: 'btn studio-src', text: '🖼️ گالری', onclick: async function () { var f = await pick('image/*', false); if (f) setSource(await fileToUrl(f)); }})]);
      var ex = opts.existing || [];
      if (ex.length) {
        box.appendChild(el('p', {class: 'studio-lbl', text: 'تصویرهای فعلی این سؤال (' + fa(ex.length) + '):'}));
        ex.forEach(function (m, i) { box.appendChild(el('div', {class: 'studio-ex'}, [el('img', {src: m.uri}), el('span', {class: 'grow', text: 'تصویر ' + fa(i + 1)}), el('button', {class: 'btn light sm', text: '✏️ ویرایش', onclick: function () { setSource(m.uri, i); }}), el('button', {class: 'btn light sm danger-text', text: '🗑️ حذف', onclick: function () { if (opts.onDelete) opts.onDelete(i); close(); }})])); });
      }
      stage.appendChild(box);
    }
    drawBar(); drawPicker();
    return {close: close};
  }
  window.SiteStudio = {open: open};
})();
