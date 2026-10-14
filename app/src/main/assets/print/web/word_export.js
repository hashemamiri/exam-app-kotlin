/* V285 — خروجی Word (.docx) از همان برگهٔ پیش‌نمایش چاپ.
   مشترک اپ و سایت (موتور چاپ یکی است): DOM رندرشدهٔ #previewArea (یا برگه‌های PGS) پیموده می‌شود و به OOXML تبدیل می‌شود:
   - متن با قلم/اندازه/رنگ/ضخامت/زیرخط از computed style؛ راست‌به‌چپ؛ تراز پاراگراف
   - فرمول‌های موتور (.mathx: کسر، رادیکال، توان/اندیس، پرانتز، ∑∫، بردار، خط بالا، ماتریس) → OMML (فرمول واقعی و قابل ویرایش Word)
   - تصویرها (<img>) و شکل‌های SVG (نمودار/هندسه/جدول تناوبی/…) → PNG داخل سند
   - جدول‌ها (سربرگ، جدول سؤال‌ها، جدول‌های سؤال، کلید) → جدول Word با عرض ستون‌ها و خطوط
   - فضای پاسخ خط‌دار → پاراگراف‌های نقطه‌چین با همان ارتفاع؛ فضای خالی → پاراگراف‌های خالی
   - اندازهٔ کاغذ/جهت/حاشیه از «تنظیمات صفحه»
   بدون کتابخانه: نویسندهٔ ZIP (store + CRC32) همین‌جاست.
   API: window.ExamWord.build() → Promise<Uint8Array>؛ window.ExamWord.buildBase64() → Promise<string> (برای پل اندروید). */
(function () {
  'use strict';
  var EMU_PER_PX = 9525, TWIP_PER_PX = 15, TWIP_PER_MM = 56.6929;
  var W_NS = 'xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:m="http://schemas.openxmlformats.org/officeDocument/2006/math" xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture"';
  var SKIP_SEL = '.q-tools,.fig-resize-handle,.fig-size-badge,.fig-move-hint,.question-sep-drag,.q-move,.q-type-badge,button,input,select,textarea,script,style,.pgs-sheet-frame,.pgs-sheet-label,.pgs-page-num';

  /* ---------------------------------------------------------------- کمکی */
  function esc(s) { return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;'); }
  function cleanText(s) { return String(s == null ? '' : s).replace(/[\u0000-\u0008\u000B\u000C\u000E-\u001F]/g, ''); }
  function isRtlText(s) { return /[\u0590-\u08FF\uFB1D-\uFDFF\uFE70-\uFEFF]/.test(s); }
  function cs(el) { try { return window.getComputedStyle(el); } catch (e) { return null; } }
  function px(v) { var n = parseFloat(v); return isFinite(n) ? n : 0; }
  function hexColor(c) {
    var m = /rgba?\((\d+),\s*(\d+),\s*(\d+)(?:,\s*([\d.]+))?\)/.exec(c || '');
    if (!m) return null;
    if (m[4] !== undefined && parseFloat(m[4]) === 0) return null;
    var h = ((1 << 24) + (+m[1] << 16) + (+m[2] << 8) + (+m[3])).toString(16).slice(1).toUpperCase();
    return h === '000000' ? null : h;
  }
  function firstFont(ff) {
    var f = String(ff || '').split(',')[0].trim().replace(/^["']|["']$/g, '');
    if (!f || /^(serif|sans-serif|monospace|inherit|system-ui)$/i.test(f)) return '';
    return f.slice(0, 40);
  }
  function hidden(el) {
    if (!el || el.nodeType !== 1) return false;
    if (el.matches && el.matches(SKIP_SEL)) return true;
    var s = cs(el); if (!s) return false;
    if (s.display === 'none' || s.visibility === 'hidden') return true;
    if (el.classList && (el.classList.contains('fig-resize-handle') || el.classList.contains('no-print'))) return true;
    return false;
  }
  function isBlock(el) {
    var s = cs(el); if (!s) return false;
    var d = s.display || '';
    if (/^inline/.test(d)) return false;
    return /block|flex|grid|list-item|table-caption|flow-root/.test(d) || el.tagName === 'DIV' || el.tagName === 'P' || /^H[1-6]$/.test(el.tagName) || el.tagName === 'LI';
  }
  function cm2twip(cm) { return Math.round(cm * 567); }

  /* ---------------------------------------------------------------- نویسندهٔ ZIP (store) */
  var CRC_T = (function () { var t = new Uint32Array(256); for (var n = 0; n < 256; n++) { var c = n; for (var k = 0; k < 8; k++) c = (c & 1) ? (0xEDB88320 ^ (c >>> 1)) : (c >>> 1); t[n] = c >>> 0; } return t; })();
  function crc32(u8) { var c = 0xFFFFFFFF; for (var i = 0; i < u8.length; i++) c = CRC_T[(c ^ u8[i]) & 0xFF] ^ (c >>> 8); return (c ^ 0xFFFFFFFF) >>> 0; }
  function utf8(s) { return new TextEncoder().encode(s); }
  function zip(files) { /* files: [{name, data(Uint8Array)}] */
    var parts = [], central = [], offset = 0, now = new Date();
    var dosTime = ((now.getHours() << 11) | (now.getMinutes() << 5) | (now.getSeconds() >> 1)) & 0xFFFF;
    var dosDate = (((now.getFullYear() - 1980) << 9) | ((now.getMonth() + 1) << 5) | now.getDate()) & 0xFFFF;
    function le(n, b) { var a = new Uint8Array(b); for (var i = 0; i < b; i++) a[i] = (n >>> (8 * i)) & 0xFF; return a; }
    function cat(arr) { var n = 0; arr.forEach(function (a) { n += a.length; }); var out = new Uint8Array(n), p = 0; arr.forEach(function (a) { out.set(a, p); p += a.length; }); return out; }
    files.forEach(function (f) {
      var name = utf8(f.name), data = f.data, crc = crc32(data);
      var local = cat([le(0x04034b50, 4), le(20, 2), le(0x0800, 2), le(0, 2), le(dosTime, 2), le(dosDate, 2), le(crc, 4), le(data.length, 4), le(data.length, 4), le(name.length, 2), le(0, 2), name, data]);
      central.push(cat([le(0x02014b50, 4), le(20, 2), le(20, 2), le(0x0800, 2), le(0, 2), le(dosTime, 2), le(dosDate, 2), le(crc, 4), le(data.length, 4), le(data.length, 4), le(name.length, 2), le(0, 2), le(0, 2), le(0, 2), le(0, 2), le(0, 4), le(offset, 4), name]));
      parts.push(local); offset += local.length;
    });
    var cd = cat(central);
    var end = cat([le(0x06054b50, 4), le(0, 2), le(0, 2), le(files.length, 2), le(files.length, 2), le(cd.length, 4), le(offset, 4), le(0, 2)]);
    return cat(parts.concat([cd, end]));
  }

  /* ---------------------------------------------------------------- تصویرها */
  function Ctx() { this.body = []; this.media = []; this.rels = []; this.imgId = 0; this.pending = []; this.slots = []; }
  Ctx.prototype.addImage = function (bytes, ext, wPx, hPx) {
    var n = ++this.imgId, rid = 'rIdImg' + n, name = 'image' + n + '.' + ext;
    this.media.push({name: 'word/media/' + name, data: bytes});
    this.rels.push('<Relationship Id="' + rid + '" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="media/' + name + '"/>');
    return {rid: rid, n: n, w: wPx, h: hPx};
  };
  function dataUrlBytes(u) {
    var m = /^data:([^;,]+)?(;base64)?,(.*)$/s.exec(u); if (!m) return null;
    var mime = m[1] || 'application/octet-stream';
    var bin = m[2] ? atob(m[3]) : decodeURIComponent(m[3]);
    var out = new Uint8Array(bin.length); for (var i = 0; i < bin.length; i++) out[i] = bin.charCodeAt(i);
    return {mime: mime, bytes: out};
  }
  function canvasPng(draw, w, h, scale) {
    var c = document.createElement('canvas'); c.width = Math.max(1, Math.round(w * scale)); c.height = Math.max(1, Math.round(h * scale));
    var x = c.getContext('2d'); x.fillStyle = '#fff'; x.fillRect(0, 0, c.width, c.height); x.scale(scale, scale); draw(x);
    return dataUrlBytes(c.toDataURL('image/png')).bytes;
  }
  function loadImg(src) { return new Promise(function (res, rej) { var im = new Image(); im.onload = function () { res(im); }; im.onerror = function () { rej(new Error('img')); }; im.src = src; }); }
  function imgElement(ctx, el, maxW) {
    var src = el.currentSrc || el.src || ''; if (!src) return Promise.resolve(null);
    var w = el.naturalWidth || px(el.getAttribute('width')) || 300, h = el.naturalHeight || px(el.getAttribute('height')) || 200;
    var r = el.getBoundingClientRect(); var dw = r.width > 2 ? r.width : (px(el.style.width) || w); var dh = dw * h / Math.max(1, w);
    if (dw > maxW) { dh = dh * maxW / dw; dw = maxW; }
    var d = /^data:/.test(src) ? dataUrlBytes(src) : null;
    if (d && (d.mime === 'image/png' || d.mime === 'image/jpeg')) return Promise.resolve(ctx.addImage(d.bytes, d.mime === 'image/png' ? 'png' : 'jpg', dw, dh));
    return loadImg(src).then(function (im) { return ctx.addImage(canvasPng(function (x) { x.drawImage(im, 0, 0, w, h); }, w, h, 1), 'png', dw, dh); }).catch(function () { return null; });
  }
  function svgElement(ctx, el, maxW) {
    var r = el.getBoundingClientRect(); var vb = (el.getAttribute('viewBox') || '').split(/[\s,]+/).map(parseFloat);
    var w = r.width > 2 ? r.width : (px(el.getAttribute('width')) || (vb.length === 4 ? vb[2] : 300));
    var h = r.height > 2 ? r.height : (px(el.getAttribute('height')) || (vb.length === 4 ? vb[3] : 200));
    var dw = w, dh = h; if (dw > maxW) { dh = dh * maxW / dw; dw = maxW; }
    var clone = el.cloneNode(true);
    clone.setAttribute('xmlns', 'http://www.w3.org/2000/svg'); clone.setAttribute('width', w); clone.setAttribute('height', h);
    if (!clone.getAttribute('viewBox') && w && h) clone.setAttribute('viewBox', '0 0 ' + w + ' ' + h);
    /* استایل‌های محاسبه‌شدهٔ متن داخل SVG (قلم) تا در canvas هم اعمال شوند */
    try { var texts = clone.querySelectorAll('text'); var orig = el.querySelectorAll('text'); for (var i = 0; i < texts.length && i < orig.length; i++) { var s = cs(orig[i]); if (s) { texts[i].setAttribute('font-family', s.fontFamily); texts[i].setAttribute('font-size', s.fontSize); } } } catch (e) {}
    var xml = new XMLSerializer().serializeToString(clone);
    var url = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(xml);
    return loadImg(url).then(function (im) { return ctx.addImage(canvasPng(function (x) { x.drawImage(im, 0, 0, w, h); }, w, h, 2), 'png', dw, dh); }).catch(function () { return null; });
  }
  function drawingXml(img) {
    var cx = Math.round(img.w * EMU_PER_PX), cy = Math.round(img.h * EMU_PER_PX);
    return '<w:r><w:drawing><wp:inline distT="0" distB="0" distL="0" distR="0"><wp:extent cx="' + cx + '" cy="' + cy + '"/><wp:docPr id="' + img.n + '" name="تصویر ' + img.n + '"/>' +
      '<a:graphic><a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture"><pic:pic><pic:nvPicPr><pic:cNvPr id="' + img.n + '" name="image' + img.n + '"/><pic:cNvPicPr/></pic:nvPicPr>' +
      '<pic:blipFill><a:blip r:embed="' + img.rid + '"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill><pic:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="' + cx + '" cy="' + cy + '"/></a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom></pic:spPr></pic:pic></a:graphicData></a:graphic></wp:inline></w:drawing></w:r>';
  }

  /* ---------------------------------------------------------------- اجرای متن */
  function runProps(el, extra) {
    var s = cs(el), p = '';
    if (s) {
      var f = firstFont(s.fontFamily); if (f) p += '<w:rFonts w:ascii="' + esc(f) + '" w:hAnsi="' + esc(f) + '" w:cs="' + esc(f) + '"/>';
      if (px(s.fontWeight) >= 600 || s.fontWeight === 'bold' || s.fontWeight === 'bolder') p += '<w:b/><w:bCs/>';
      if (s.fontStyle === 'italic' || s.fontStyle === 'oblique') p += '<w:i/><w:iCs/>';
      if (/underline/.test(s.textDecorationLine || s.textDecoration || '')) p += '<w:u w:val="single"/>';
      if (/line-through/.test(s.textDecorationLine || s.textDecoration || '')) p += '<w:strike/>';
      var c = hexColor(s.color); if (c) p += '<w:color w:val="' + c + '"/>';
      var hp = Math.round(px(s.fontSize) * 1.5); if (hp > 0) p += '<w:sz w:val="' + hp + '"/><w:szCs w:val="' + hp + '"/>';
    }
    if (extra) p += extra;
    return p;
  }
  function textRun(text, el, extra) {
    text = cleanText(text); if (!text) return '';
    var rp = runProps(el, extra) + (isRtlText(text) ? '<w:rtl/>' : '');
    return '<w:r><w:rPr>' + rp + '</w:rPr><w:t xml:space="preserve">' + esc(text) + '</w:t></w:r>';
  }

  /* ---------------------------------------------------------------- فرمول → OMML */
  function mT(text) { text = cleanText(text).replace(/\u00a0/g, ' '); if (!text.trim()) return ''; return '<m:r><m:t xml:space="preserve">' + esc(text) + '</m:t></m:r>'; }
  function mNor(text) { text = cleanText(text); if (!text) return ''; return '<m:r><m:rPr><m:nor/></m:rPr><m:t xml:space="preserve">' + esc(text) + '</m:t></m:r>'; }
  function mE(inner) { return '<m:e>' + (inner || '') + '</m:e>'; }
  function mathChildren(node) {
    var out = [];
    for (var c = node.firstChild; c; c = c.nextSibling) {
      if (c.nodeType === 1 && c.classList.contains('mbigop') && !(c.querySelector(':scope > .mbigsym.mword'))) {
        /* ∑ ∫ ∏ … : بدنهٔ عملگر = ادامهٔ همین گروه (مثل TeX) */
        var rest = []; for (var r = c.nextSibling; r; r = r.nextSibling) mathNode(r, rest);
        mathNode(c, out, joinM(rest)); break;
      }
      mathNode(c, out);
    }
    return out;
  }
  function joinM(arr) { return arr.join(''); }
  function popBase(out) {
    if (!out.length) return '';
    var last = out[out.length - 1], m = /^<m:r><m:t xml:space="preserve">([\s\S]*)<\/m:t><\/m:r>$/.exec(last);
    if (m) { /* فقط آخرین نویسهٔ متن پایه است (x در «۲x²») */
      var t = m[1].replace(/\s+$/, ''), un = t.replace(/&lt;|&gt;|&amp;|&quot;/g, 'X');
      if (un.length > 1) { var cut = /&(lt|gt|amp|quot);$/.exec(t); var k = cut ? cut[0].length : 1; out[out.length - 1] = '<m:r><m:t xml:space="preserve">' + t.slice(0, t.length - k) + '</m:t></m:r>'; return '<m:r><m:t xml:space="preserve">' + t.slice(t.length - k) + '</m:t></m:r>'; }
    }
    return out.pop();
  }
  function mathNode(n, out, naryBody) {
    if (n.nodeType === 3) { var t = mT(n.nodeValue); if (t) out.push(t); return; }
    if (n.nodeType !== 1 || hidden(n)) return;
    var cl = n.classList, tag = n.tagName;
    if (cl.contains('mfrac')) {
      var num = n.querySelector(':scope > .mnum'), den = n.querySelector(':scope > .mden');
      out.push('<m:f><m:num>' + joinM(mathChildren(num || n)) + '</m:num><m:den>' + joinM(mathChildren(den || n)) + '</m:den></m:f>'); return;
    }
    if (cl.contains('msfrac')) { /* a/b کج: واحد، «/»، واحد */
      var parts = [], cur = [];
      for (var sc = n.firstChild; sc; sc = sc.nextSibling) { if (sc.nodeType === 1 && sc.classList.contains('mslash')) { parts.push(cur); cur = []; } else mathNode(sc, cur); }
      parts.push(cur);
      out.push('<m:f><m:fPr><m:type m:val="skw"/></m:fPr><m:num>' + joinM(parts[0]) + '</m:num><m:den>' + joinM(parts[1] || []) + '</m:den></m:f>'); return;
    }
    if (cl.contains('mbrk-box')) {
      var bodyB = n.querySelector(':scope > .mpar-body'), kind = /mbrk-(\w+)/.exec(n.className), pair = {brace: ['{', '}'], curly: ['{', '}'], bracket: ['[', ']'], square: ['[', ']'], angle: ['⟨', '⟩'], abs: ['|', '|'], norm: ['‖', '‖'], floor: ['⌊', '⌋'], ceil: ['⌈', '⌉']}[kind ? kind[1] : ''] || ['(', ')'];
      out.push('<m:d><m:dPr><m:begChr m:val="' + esc(pair[0]) + '"/><m:endChr m:val="' + esc(pair[1]) + '"/></m:dPr>' + mE(joinM(mathChildren(bodyB || n))) + '</m:d>'); return;
    }
    if (cl.contains('mubrace')) {
      var ub = n.querySelector(':scope > .mubrace-body'), us = n.querySelector(':scope > .mubrace-sub'), ov = cl.contains('ov');
      var grp = '<m:groupChr><m:groupChrPr><m:chr m:val="' + (ov ? '⏞' : '⏟') + '"/><m:pos m:val="' + (ov ? 'top' : 'bot') + '"/>' + (ov ? '<m:vertJc m:val="bot"/>' : '') + '</m:groupChrPr>' + mE(joinM(mathChildren(ub || n))) + '</m:groupChr>';
      out.push(us ? (ov ? '<m:limUpp>' : '<m:limLow>') + mE(grp) + '<m:lim>' + joinM(mathChildren(us)) + '</m:lim>' + (ov ? '</m:limUpp>' : '</m:limLow>') : grp); return;
    }
    if (cl.contains('mbrk') || cl.contains('mslash')) return;
    if (cl.contains('msqrt')) {
      var idx = n.querySelector(':scope > .mrootidx'), rad = n.querySelector(':scope > .mrad');
      out.push('<m:rad><m:radPr>' + (idx ? '' : '<m:degHide m:val="1"/>') + '</m:radPr><m:deg>' + (idx ? joinM(mathChildren(idx)) : '') + '</m:deg>' + mE(joinM(mathChildren(rad || n))) + '</m:rad>'); return;
    }
    if (cl.contains('msubsup')) {
      var sp = n.querySelector(':scope > sup, :scope > .msup'), sb = n.querySelector(':scope > sub, :scope > .msub'), bs = n.querySelector(':scope > .ss-base');
      out.push('<m:sSubSup>' + mE(bs ? joinM(mathChildren(bs)) : popBase(out)) + '<m:sub>' + (sb ? joinM(mathChildren(sb)) : '') + '</m:sub><m:sup>' + (sp ? joinM(mathChildren(sp)) : '') + '</m:sup></m:sSubSup>'); return;
    }
    if (tag === 'SUP') { out.push('<m:sSup>' + mE(popBase(out)) + '<m:sup>' + joinM(mathChildren(n)) + '</m:sup></m:sSup>'); return; }
    if (tag === 'SUB') { out.push('<m:sSub>' + mE(popBase(out)) + '<m:sub>' + joinM(mathChildren(n)) + '</m:sub></m:sSub>'); return; }
    if (cl.contains('mparbox')) {
      var ds = n.querySelectorAll(':scope > .mdelim'), body = n.querySelector(':scope > .mpar-body');
      var o = ds[0] ? (ds[0].getAttribute('data-dl') || ds[0].getAttribute('data-delim') || ds[0].textContent) : '(', c = ds[1] ? (ds[1].getAttribute('data-dl') || ds[1].getAttribute('data-delim') || ds[1].textContent) : ')';
      if (n.getAttribute('data-kind') === 'abs') { o = '|'; c = '|'; }
      var inner = body ? joinM(mathChildren(body)) : joinM(mathChildren(n));
      if (cl.contains('mmatrix-box')) { var tb = n.querySelector('table'); if (tb) inner = matrixOmml(tb); }
      out.push('<m:d><m:dPr><m:begChr m:val="' + esc(o.trim()) + '"/><m:endChr m:val="' + esc(c.trim()) + '"/></m:dPr>' + mE(inner) + '</m:d>'); return;
    }
    if (cl.contains('mmatrix')) { var tb2 = n.querySelector('table'); out.push(tb2 ? matrixOmml(tb2) : joinM(mathChildren(n))); return; }
    if (cl.contains('mbigop')) {
      var sym = n.querySelector(':scope > .mbigsym'), over = n.querySelector(':scope > .mover'), under = n.querySelector(':scope > .munder');
      var chr = sym ? sym.textContent.trim() : '∑';
      if (sym && sym.classList.contains('mword')) { /* lim/max/min: حد با زیرنویس */
        out.push('<m:limLow>' + mE(mNor(chr)) + '<m:lim>' + (under ? joinM(mathChildren(under)) : '') + '</m:lim></m:limLow>'); return;
      }
      out.push('<m:nary><m:naryPr><m:chr m:val="' + esc(chr) + '"/><m:limLoc m:val="undOvr"/>' + (under && under.textContent.trim() ? '' : '<m:subHide m:val="1"/>') + (over && over.textContent.trim() ? '' : '<m:supHide m:val="1"/>') + '</m:naryPr><m:sub>' + (under ? joinM(mathChildren(under)) : '') + '</m:sub><m:sup>' + (over ? joinM(mathChildren(over)) : '') + '</m:sup>' + mE(naryBody || '') + '</m:nary>'); return;
    }
    if (cl.contains('mover-line')) { out.push('<m:bar><m:barPr><m:pos m:val="top"/></m:barPr>' + mE(joinM(mathChildren(n))) + '</m:bar>'); return; }
    if (cl.contains('munder-line')) { out.push('<m:bar><m:barPr><m:pos m:val="bot"/></m:barPr>' + mE(joinM(mathChildren(n))) + '</m:bar>'); return; }
    var acc = cl.contains('mvec') ? '\u20D7' : (cl.contains('mvec-l') ? (cl.contains('arr-l') ? '\u20D6' : (cl.contains('arr-b') ? '\u20E1' : '\u20D7')) : (cl.contains('mhat') ? '\u0302' : (cl.contains('mtilde') ? '\u0303' : (cl.contains('mdot') ? '\u0307' : (cl.contains('mbar') ? '\u0304' : '')))));
    if (acc) { out.push('<m:acc><m:accPr><m:chr m:val="' + acc + '"/></m:accPr>' + mE(joinM(mathChildren(n))) + '</m:acc>'); return; }
    if (cl.contains('moverset')) {
      var top = n.querySelector(':scope > .moverset-t'), bot = n.querySelector(':scope > .moverset-b');
      out.push('<m:limUpp>' + mE(bot ? joinM(mathChildren(bot)) : '') + '<m:lim>' + (top ? joinM(mathChildren(top)) : '') + '</m:lim></m:limUpp>'); return;
    }
    if (cl.contains('mtext') || cl.contains('mfun')) { out.push(mNor(n.textContent)); return; }
    if (cl.contains('msurd') || cl.contains('mrootidx') || cl.contains('mdelim')) return; /* فقط در ساختار والد معنا دارد */
    if (cl.contains('mspace1') || cl.contains('mspace15') || cl.contains('mspace2') || cl.contains('mspace4') || cl.contains('mhspace')) { out.push(mT(' ')); return; }
    if (tag === 'BR') { return; }
    if (tag === 'TABLE') { out.push(matrixOmml(n)); return; }
    if (tag === 'IMG' || tag === 'svg') { out.push(mNor('[' + (tag === 'IMG' ? 'تصویر' : 'شکل') + ']')); return; }
    /* پیش‌فرض: بچه‌ها را به همین خروجی اضافه کن (span های بی‌معنی، msym، mnum داخل ساختار ناشناخته…) */
    for (var ch = n.firstChild; ch; ch = ch.nextSibling) mathNode(ch, out);
  }
  function matrixOmml(table) {
    var rows = table.querySelectorAll('tr'), out = '<m:m><m:mPr><m:mcs><m:mc><m:mcPr><m:count m:val="' + Math.max(1, (rows[0] ? rows[0].children.length : 1)) + '"/><m:mcJc m:val="center"/></m:mcPr></m:mc></m:mcs></m:mPr>';
    for (var i = 0; i < rows.length; i++) { out += '<m:mr>'; var cells = rows[i].children; for (var j = 0; j < cells.length; j++) out += mE(joinM(mathChildren(cells[j]))); out += '</m:mr>'; }
    return out + '</m:m>';
  }
  function omathXml(el) {
    var inner = joinM(mathChildren(el));
    if (!inner) return '';
    var display = el.classList.contains('mdisp');
    return display ? '<m:oMathPara><m:oMath>' + inner + '</m:oMath></m:oMathPara>' : '<m:oMath>' + inner + '</m:oMath>';
  }
  function isMathRoot(el) { return el.classList && (el.classList.contains('mathx') || el.classList.contains('math-inline') || el.classList.contains('math-display')); }

  /* ---------------------------------------------------------------- پاراگراف‌ها و جدول‌ها */
  function paraProps(el, opts) {
    var s = cs(el), p = '<w:bidi/>';
    var ta = s ? s.textAlign : 'start';
    var jc = ta === 'center' ? 'center' : (ta === 'left' || ta === 'end' ? 'left' : (ta === 'justify' ? 'both' : 'right'));
    p += '<w:jc w:val="' + jc + '"/>';
    if (opts && opts.spacing) p += opts.spacing;
    else p += '<w:spacing w:before="0" w:after="60" w:line="' + Math.round((s && px(s.lineHeight) > 0 ? px(s.lineHeight) : px(s ? s.fontSize : 14) * 1.6) * TWIP_PER_PX) + '" w:lineRule="atLeast"/>';
    if (opts && opts.border) p += opts.border;
    if (opts && opts.ind) p += opts.ind;
    return '<w:pPr>' + p + '</w:pPr>';
  }
  function Para(el, opts) { this.el = el; this.opts = opts || {}; this.runs = []; }
  Para.prototype.xml = function () { if (!this.runs.length && !this.opts.allowEmpty) return ''; return '<w:p>' + paraProps(this.el, this.opts) + this.runs.join('') + '</w:p>'; };

  /* بخش جاری: مجموعه‌ای از بلوک‌ها (xml) + پاراگراف باز */
  function Section(ctx, blockEl) { this.ctx = ctx; this.blockEl = blockEl; this.blocks = []; this.open = null; }
  Section.prototype.para = function (el) { if (!this.open) this.open = new Para(el || this.blockEl); return this.open; };
  Section.prototype.flush = function () { if (this.open) { var x = this.open.xml(); if (x) this.blocks.push(x); this.open = null; } };
  Section.prototype.block = function (xml) { this.flush(); if (xml) this.blocks.push(xml); };
  Section.prototype.xml = function () { this.flush(); return this.blocks.join(''); };

  function walkInline(sec, node, maxW) {
    var ctx = sec.ctx;
    if (node.nodeType === 3) {
      var t = node.nodeValue.replace(/\s+/g, ' ');
      if (t.trim() === '' && !sec.open) return;
      if (t) sec.para(node.parentElement).runs.push(textRun(t, node.parentElement));
      return;
    }
    if (node.nodeType !== 1 || hidden(node)) return;
    var tag = node.tagName, cl = node.classList;
    if (tag === 'BR') { sec.para(node.parentElement).runs.push('<w:r><w:br/></w:r>'); return; }
    if (isMathRoot(node)) { var mx = omathXml(node); if (mx) sec.para(node.parentElement).runs.push(mx); return; }
    if (tag === 'IMG' || tag === 'svg') {
      /* تصویر ناهمگام است؛ جای آن یک نشانه می‌گذاریم و در پایان (پس از لود) با w:drawing جایگزین می‌شود */
      var token = '\u0001IMG' + (ctx.slots.length) + '\u0001'; ctx.slots.push('');
      var idx = ctx.slots.length - 1;
      sec.para(node.parentElement).runs.push(token);
      ctx.pending.push((tag === 'IMG' ? imgElement : svgElement)(ctx, node, maxW).then(function (img) { ctx.slots[idx] = img ? drawingXml(img) : ''; }, function () { ctx.slots[idx] = ''; }));
      return;
    }
    if (cl.contains('choice-mark')) { sec.para(node.parentElement).runs.push(textRun('○ ', node)); return; }
    if (cl.contains('match-dot')) { sec.para(node.parentElement).runs.push(textRun(' ○ ', node)); return; }
    if (cl.contains('match-mid-gap')) { sec.para(node.parentElement).runs.push(textRun(' ………… ', node)); return; }
    if (tag === 'SUP' || tag === 'SUB') {
      var va = tag === 'SUP' ? 'superscript' : 'subscript';
      for (var c = node.firstChild; c; c = c.nextSibling) if (c.nodeType === 3) sec.para(node).runs.push(textRun(c.nodeValue, node, '<w:vertAlign w:val="' + va + '"/>')); else walkInline(sec, c, maxW);
      return;
    }
    if (tag === 'TABLE') { sec.block(tableXml(ctx, node, maxW)); return; }
    if (isBlock(node)) { walkBlock(sec, node, maxW); return; }
    for (var ch = node.firstChild; ch; ch = ch.nextSibling) walkInline(sec, ch, maxW);
  }

  function walkBlock(sec, el, maxW) {
    var ctx = sec.ctx, cl = el.classList;
    if (hidden(el)) return;
    sec.flush();
    /* فضای پاسخ */
    if (cl.contains('answer-space')) {
      if (cl.contains('lined')) {
        var rows = el.querySelectorAll('.answer-line-row');
        for (var i = 0; i < rows.length; i++) {
          var hcm = parseFloat(rows[i].style.height) || 0.75;
          sec.block('<w:p><w:pPr><w:bidi/><w:spacing w:before="0" w:after="0" w:line="' + cm2twip(hcm) + '" w:lineRule="exact"/><w:pBdr><w:bottom w:val="dotted" w:sz="6" w:space="1" w:color="555555"/></w:pBdr></w:pPr></w:p>');
        }
      } else {
        var hc = parseFloat(el.style.minHeight) || 2, n = Math.max(1, Math.round(hc / 0.75));
        for (var j = 0; j < n; j++) sec.block('<w:p><w:pPr><w:bidi/><w:spacing w:before="0" w:after="0" w:line="' + cm2twip(0.75) + '" w:lineRule="exact"/></w:pPr></w:p>');
      }
      return;
    }
    if (cl.contains('question-sep-cell-pad')) { var hpx = px(el.style.height); if (hpx > 4) sec.block('<w:p><w:pPr><w:spacing w:before="0" w:after="0" w:line="' + Math.round(hpx * TWIP_PER_PX) + '" w:lineRule="exact"/></w:pPr></w:p>'); return; }
    if (el.tagName === 'TABLE') { sec.block(tableXml(ctx, el, maxW)); return; }
    if (el.tagName === 'svg' || el.tagName === 'IMG') { walkInline(sec, el, maxW); sec.flush(); return; }
    if (el.tagName === 'HR') { sec.block('<w:p><w:pPr><w:pBdr><w:bottom w:val="single" w:sz="6" w:space="1" w:color="000000"/></w:pBdr></w:pPr></w:p>'); return; }
    /* بلوک با خط زیر/بالا (مثل خط‌های سربرگ) */
    var s = cs(el), border = '';
    if (s && el.tagName !== 'TD' && el.tagName !== 'TH') { /* خطِ خانه‌های جدول را خودِ جدول می‌کشد */
      var bb = px(s.borderBottomWidth) > 0 && s.borderBottomStyle !== 'none', bt = px(s.borderTopWidth) > 0 && s.borderTopStyle !== 'none';
      if (bt) border += '<w:top w:val="single" w:sz="' + Math.max(4, Math.round(px(s.borderTopWidth) * 8)) + '" w:space="1" w:color="' + (hexColor(s.borderTopColor) || '000000') + '"/>';
      if (bb) border += '<w:bottom w:val="single" w:sz="' + Math.max(4, Math.round(px(s.borderBottomWidth) * 8)) + '" w:space="1" w:color="' + (hexColor(s.borderBottomColor) || '000000') + '"/>';
      if (border) border = '<w:pBdr>' + border + '</w:pBdr>';
    }
    var sub = new Section(ctx, el);
    var hasBlockChild = false;
    for (var c = el.firstChild; c; c = c.nextSibling) if (c.nodeType === 1 && !hidden(c) && (isBlock(c) || c.tagName === 'TABLE')) { hasBlockChild = true; break; }
    if (!hasBlockChild) {
      var p = sub.para(el); p.opts.border = border; p.opts.allowEmpty = !!border;
      /* فرمول نمایشی یا CSS-grid بدون متن */
      for (var ch = el.firstChild; ch; ch = ch.nextSibling) walkInline(sub, ch, maxW);
      sub.flush();
      /* فضاهای خالی با ارتفاع صریح (مثل محل امضا) */
      if (!sub.blocks.length && s && px(el.style.minHeight || s.minHeight) > 10) sub.block('<w:p><w:pPr><w:spacing w:before="0" w:after="0" w:line="' + Math.round(px(el.style.minHeight || s.minHeight) * TWIP_PER_PX) + '" w:lineRule="exact"/>' + border + '</w:pPr></w:p>');
    } else {
      /* فرزندان تورفته‌تر: گرید/فلکس با چند ستون → پشت‌سرهم */
      var grid = s && /grid|flex/.test(s.display) && s.flexDirection !== 'column';
      if (grid && gridAsTable(sub, el, maxW)) { /* ستون‌ها به جدول بدون خط */ }
      else for (var c2 = el.firstChild; c2; c2 = c2.nextSibling) {
        if (c2.nodeType === 3) { if (c2.nodeValue.trim()) sub.para(el).runs.push(textRun(c2.nodeValue.replace(/\s+/g, ' '), el)); continue; }
        if (c2.nodeType !== 1 || hidden(c2)) continue;
        if (isBlock(c2) || c2.tagName === 'TABLE') walkBlock(sub, c2, maxW); else walkInline(sub, c2, maxW);
      }
    }
    sec.block(sub.xml());
  }

  /* گرید/فلکس چندستونه (گزینه‌های چندگزینه‌ای، ردیف‌های سربرگ، جورکردنی) → جدول بدون خط با همان تعداد ستون */
  function gridAsTable(sec, el, maxW) {
    var kids = []; for (var c = el.firstChild; c; c = c.nextSibling) if (c.nodeType === 1 && !hidden(c)) kids.push(c);
    if (kids.length < 2) return false;
    /* ردیف‌ها از روی هندسه: عنصری که زیرِ ردیفِ جاری شروع شود، ردیف تازه است */
    var rows = [], cur = null, curBottom = -1e9;
    kids.forEach(function (k) {
      var r = k.getBoundingClientRect();
      if (!cur || r.top >= curBottom - 2) { cur = []; rows.push(cur); curBottom = r.bottom; }
      else curBottom = Math.max(curBottom, r.bottom);
      cur.push(k);
    });
    var ncol = 0, ref = null; rows.forEach(function (r) { if (r.length > ncol) { ncol = r.length; ref = r; } });
    if (ncol < 2 || ncol > 8) return false;
    var cols = ref.map(function (k) { return k.getBoundingClientRect(); });
    if (cols[0].width < 1) return false;
    var total = cols.reduce(function (a, r) { return a + r.width; }, 0) || maxW;
    var tw = Math.min(maxW, total), widths = cols.map(function (r) { return Math.round(tw * r.width / total * TWIP_PER_PX); });
    /* راست‌به‌چپ: ترتیب DOM = راست به چپ؛ bidiVisual خودش برمی‌گرداند */
    var x = '<w:tbl><w:tblPr><w:tblW w:w="' + Math.round(tw * TWIP_PER_PX) + '" w:type="dxa"/><w:bidiVisual/><w:tblLayout w:type="fixed"/><w:tblCellMar><w:left w:w="40" w:type="dxa"/><w:right w:w="40" w:type="dxa"/></w:tblCellMar></w:tblPr><w:tblGrid>' + widths.map(function (w) { return '<w:gridCol w:w="' + w + '"/>'; }).join('') + '</w:tblGrid>';
    rows.forEach(function (row) {
      x += '<w:tr>';
      for (var j = 0; j < ncol; j++) {
        var cell = row[j]; x += '<w:tc><w:tcPr><w:tcW w:w="' + widths[j] + '" w:type="dxa"/></w:tcPr>';
        if (cell) { var cs2 = new Section(sec.ctx, cell); walkBlock(cs2, cell, widths[j] / TWIP_PER_PX); var inner = cs2.xml(); x += inner || '<w:p/>'; } else x += '<w:p/>';
        x += '</w:tc>';
      }
      x += '</w:tr>';
    });
    sec.block(x + '</w:tbl>');
    return true;
  }

  function tableXml(ctx, table, maxW) {
    var trs = []; table.querySelectorAll(':scope > thead > tr, :scope > tbody > tr, :scope > tfoot > tr, :scope > tr').forEach(function (tr) { if (!hidden(tr)) trs.push(tr); });
    if (!trs.length) return '';
    var ncol = 0; trs.forEach(function (tr) { var n = 0; for (var i = 0; i < tr.children.length; i++) n += Math.max(1, parseInt(tr.children[i].getAttribute('colspan') || '1', 10)); ncol = Math.max(ncol, n); });
    var tr0 = trs[0], rect = table.getBoundingClientRect(), tw = rect.width > 2 ? Math.min(rect.width, maxW) : maxW;
    var widths = [], used = 0;
    for (var i = 0; i < tr0.children.length && widths.length < ncol; i++) {
      var td = tr0.children[i], span = Math.max(1, parseInt(td.getAttribute('colspan') || '1', 10)), w = td.getBoundingClientRect().width;
      for (var k = 0; k < span; k++) widths.push(w > 0 ? w / span : 0);
    }
    while (widths.length < ncol) widths.push(0);
    used = widths.reduce(function (a, b) { return a + b; }, 0);
    var scale = used > 0 ? tw / used : 1;
    widths = widths.map(function (w) { return Math.max(560, Math.round((used > 0 ? w * scale : tw / ncol) * TWIP_PER_PX)); });
    var ts = cs(table), bordered = false;
    var firstTd = table.querySelector('td,th'); var tds = firstTd ? cs(firstTd) : null;
    if ((tds && px(tds.borderBottomWidth) > 0 && tds.borderBottomStyle !== 'none') || (ts && px(ts.borderTopWidth) > 0 && ts.borderTopStyle !== 'none')) bordered = true;
    var bcolor = (tds && hexColor(tds.borderBottomColor)) || '000000';
    var tb = bordered ? '<w:tblBorders><w:top w:val="single" w:sz="4" w:color="' + bcolor + '"/><w:left w:val="single" w:sz="4" w:color="' + bcolor + '"/><w:bottom w:val="single" w:sz="4" w:color="' + bcolor + '"/><w:right w:val="single" w:sz="4" w:color="' + bcolor + '"/><w:insideH w:val="single" w:sz="4" w:color="' + bcolor + '"/><w:insideV w:val="single" w:sz="4" w:color="' + bcolor + '"/></w:tblBorders>' : '';
    var x = '<w:tbl><w:tblPr><w:tblW w:w="' + Math.round(tw * TWIP_PER_PX) + '" w:type="dxa"/><w:jc w:val="center"/><w:bidiVisual/>' + tb + '<w:tblLayout w:type="fixed"/><w:tblCellMar><w:left w:w="60" w:type="dxa"/><w:right w:w="60" w:type="dxa"/></w:tblCellMar></w:tblPr><w:tblGrid>' + widths.map(function (w) { return '<w:gridCol w:w="' + w + '"/>'; }).join('') + '</w:tblGrid>';
    trs.forEach(function (tr, ri) {
      var isHead = tr.parentElement && tr.parentElement.tagName === 'THEAD';
      x += '<w:tr>' + (isHead ? '<w:trPr><w:tblHeader/><w:cantSplit/></w:trPr>' : '');
      var ci = 0;
      for (var i = 0; i < tr.children.length; i++) {
        var td = tr.children[i]; if (hidden(td) && td.tagName !== 'TD' && td.tagName !== 'TH') continue;
        var span = Math.max(1, parseInt(td.getAttribute('colspan') || '1', 10)), w = 0; for (var k = 0; k < span && ci + k < widths.length; k++) w += widths[ci + k];
        var st = cs(td), shade = st ? hexColor(st.backgroundColor) : null;
        var va = st && st.verticalAlign === 'middle' ? 'center' : (st && st.verticalAlign === 'bottom' ? 'bottom' : 'top');
        x += '<w:tc><w:tcPr><w:tcW w:w="' + w + '" w:type="dxa"/>' + (span > 1 ? '<w:gridSpan w:val="' + span + '"/>' : '') + (shade && shade !== 'FFFFFF' ? '<w:shd w:val="clear" w:color="auto" w:fill="' + shade + '"/>' : '') + '<w:vAlign w:val="' + va + '"/></w:tcPr>';
        var sec = new Section(ctx, td); walkBlock(sec, td, w / TWIP_PER_PX); var inner = sec.xml();
        x += (inner || '<w:p/>') + '</w:tc>';
        ci += span;
      }
      x += '</w:tr>';
    });
    return x + '</w:tbl>';
  }

  /* دو جدول پشت‌سرهم با یک کلاس (جدول سؤال‌های شکسته بین برگه‌ها) → یکی */
  function mergeTables(blocks) {
    var out = [];
    blocks.forEach(function (b) {
      if (b.cls && out.length && out[out.length - 1].cls === b.cls) {
        var prev = out[out.length - 1];
        var rows = b.xml.slice(b.xml.indexOf('<w:tr'), b.xml.lastIndexOf('</w:tbl>'));
        if (rows.indexOf('<w:tr') >= 0) {
          /* سرستون تکراری حذف می‌شود */
          rows = rows.replace(/<w:tr><w:trPr><w:tblHeader\/>[\s\S]*?<\/w:tr>/, '');
          prev.xml = prev.xml.slice(0, prev.xml.lastIndexOf('</w:tbl>')) + rows + '</w:tbl>';
        }
        return;
      }
      out.push(b);
    });
    return out.map(function (b) { return b.xml; }).join('');
  }

  /* ---------------------------------------------------------------- تنظیمات صفحه */
  var PAPERS = {a4: [210, 297], a5: [148, 210], b5: [176, 250], letter: [215.9, 279.4], f4: [210, 330], legal: [215.9, 355.6]};
  function pageSetup() {
    function v(id, d) { var e = document.getElementById(id); var n = e ? parseFloat(e.value) : NaN; return isFinite(n) ? n : d; }
    var pe = document.getElementById('opt_paper'), key = pe && pe.value || 'a4';
    var W = 210, H = 297;
    if (key === 'custom') { W = v('opt_customW', 210); H = v('opt_customH', 297); } else if (PAPERS[key]) { W = PAPERS[key][0]; H = PAPERS[key][1]; }
    var oe = document.getElementById('opt_orientation'), landscape = oe && oe.value === 'landscape';
    if (landscape && W < H) { var t = W; W = H; H = t; }
    return {W: W, H: H, landscape: landscape, mT: v('opt_mT', 12), mB: v('opt_mB', 12), mR: v('opt_mR', 10), mL: v('opt_mL', 10)};
  }

  /* ---------------------------------------------------------------- اسناد بسته */
  function contentTypes() {
    return '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Default Extension="png" ContentType="image/png"/><Default Extension="jpg" ContentType="image/jpeg"/><Default Extension="jpeg" ContentType="image/jpeg"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/><Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/><Override PartName="/word/settings.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.settings+xml"/><Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/><Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/></Types>';
  }
  function rootRels() { return '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/><Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/></Relationships>'; }
  function docRels(ctx) { return '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rIdStyles" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/><Relationship Id="rIdSettings" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/settings" Target="settings.xml"/>' + ctx.rels.join('') + '</Relationships>'; }
  function stylesXml(baseFont, basePt) {
    var hp = Math.round(basePt * 2);
    return '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><w:styles ' + W_NS + '><w:docDefaults><w:rPrDefault><w:rPr><w:rFonts w:ascii="' + esc(baseFont) + '" w:hAnsi="' + esc(baseFont) + '" w:cs="' + esc(baseFont) + '" w:eastAsia="' + esc(baseFont) + '"/><w:sz w:val="' + hp + '"/><w:szCs w:val="' + hp + '"/><w:lang w:val="fa-IR" w:bidi="fa-IR"/></w:rPr></w:rPrDefault><w:pPrDefault><w:pPr><w:bidi/><w:spacing w:after="60" w:line="276" w:lineRule="auto"/></w:pPr></w:pPrDefault></w:docDefaults><w:style w:type="paragraph" w:default="1" w:styleId="Normal"><w:name w:val="Normal"/><w:qFormat/></w:style><w:style w:type="table" w:default="1" w:styleId="TableNormal"><w:name w:val="Normal Table"/><w:tblPr><w:tblInd w:w="0" w:type="dxa"/><w:tblCellMar><w:top w:w="0" w:type="dxa"/><w:left w:w="108" w:type="dxa"/><w:bottom w:w="0" w:type="dxa"/><w:right w:w="108" w:type="dxa"/></w:tblCellMar></w:tblPr></w:style></w:styles>';
  }
  function settingsXml() { return '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><w:settings ' + W_NS + '><w:defaultTabStop w:val="720"/><w:characterSpacingControl w:val="doNotCompress"/><w:compat><w:compatSetting w:name="compatibilityMode" w:uri="http://schemas.microsoft.com/office/word" w:val="15"/></w:compat><w:themeFontLang w:val="fa-IR" w:bidi="fa-IR"/></w:settings>'; }
  function coreXml(title) { var d = new Date().toISOString().replace(/\.\d+Z$/, 'Z'); return '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:dcmitype="http://purl.org/dc/dcmitype/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"><dc:title>' + esc(title) + '</dc:title><dc:creator>سامانهٔ آزمون آنلاین</dc:creator><dcterms:created xsi:type="dcterms:W3CDTF">' + d + '</dcterms:created><dcterms:modified xsi:type="dcterms:W3CDTF">' + d + '</dcterms:modified></cp:coreProperties>'; }
  function appXml() { return '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties" xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes"><Application>onlineexam.ir</Application></Properties>'; }

  /* ---------------------------------------------------------------- ساخت */
  function sourceRoots() {
    var area = document.getElementById('previewArea'); if (!area) return [];
    var bodies = area.querySelectorAll('.pgs-sheet .pgs-sheet-body');
    if (bodies.length) return Array.prototype.slice.call(bodies);
    return [area];
  }
  function build() {
    return new Promise(function (resolve, reject) {
      try {
        var roots = sourceRoots();
        if (!roots.length) throw new Error('پیش‌نمایش آماده نیست.');
        var ps = pageSetup();
        var textWmm = Math.max(60, ps.W - ps.mL - ps.mR), maxW = textWmm * 3.7795; /* px در 96dpi */
        var ctx = new Ctx(), blocks = [];
        roots.forEach(function (root) {
          for (var c = root.firstChild; c; c = c.nextSibling) {
            if (c.nodeType === 3) { if (c.nodeValue.trim()) { var s0 = new Section(ctx, root); s0.para(root).runs.push(textRun(c.nodeValue, root)); blocks.push({xml: s0.xml()}); } continue; }
            if (c.nodeType !== 1 || hidden(c)) continue;
            var sec = new Section(ctx, c);
            if (c.tagName === 'TABLE') { var tx = tableXml(ctx, c, maxW); if (tx) blocks.push({xml: tx, cls: c.className || 'table'}); continue; }
            if (isBlock(c)) walkBlock(sec, c, maxW); else walkInline(sec, c, maxW);
            var x = sec.xml(); if (x) blocks.push({xml: x});
          }
        });
        var bf = document.getElementById('opt_baseFont'), basePt = bf && parseFloat(bf.value) || 10.5;
        var area = document.getElementById('previewArea'), af = cs(area), baseFont = firstFont(af ? af.fontFamily : '') || 'B Nazanin';
        var title = (window.examTitleForWord && window.examTitleForWord()) || document.title || 'آزمون';
        Promise.all(ctx.pending).then(function () {
          var bodyXml = mergeTables(blocks).replace(/\u0001IMG(\d+)\u0001/g, function (_, i) { return ctx.slots[+i] || ''; });
          var sect = '<w:sectPr><w:pgSz w:w="' + Math.round(ps.W * TWIP_PER_MM) + '" w:h="' + Math.round(ps.H * TWIP_PER_MM) + '"' + (ps.landscape ? ' w:orient="landscape"' : '') + '/><w:pgMar w:top="' + Math.round(ps.mT * TWIP_PER_MM) + '" w:right="' + Math.round(ps.mR * TWIP_PER_MM) + '" w:bottom="' + Math.round(ps.mB * TWIP_PER_MM) + '" w:left="' + Math.round(ps.mL * TWIP_PER_MM) + '" w:header="567" w:footer="567" w:gutter="0"/><w:bidi/><w:cols w:space="708"/></w:sectPr>';
          var doc = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><w:document ' + W_NS + '><w:body>' + (bodyXml || '<w:p/>') + sect + '</w:body></w:document>';
          var files = [
            {name: '[Content_Types].xml', data: utf8(contentTypes())},
            {name: '_rels/.rels', data: utf8(rootRels())},
            {name: 'word/document.xml', data: utf8(doc)},
            {name: 'word/_rels/document.xml.rels', data: utf8(docRels(ctx))},
            {name: 'word/styles.xml', data: utf8(stylesXml(baseFont, basePt))},
            {name: 'word/settings.xml', data: utf8(settingsXml())},
            {name: 'docProps/core.xml', data: utf8(coreXml(title))},
            {name: 'docProps/app.xml', data: utf8(appXml())}
          ].concat(ctx.media);
          resolve(zip(files));
        }, reject);
      } catch (e) { reject(e); }
    });
  }
  function toBase64(u8) { var s = '', CH = 0x8000; for (var i = 0; i < u8.length; i += CH) s += String.fromCharCode.apply(null, u8.subarray(i, i + CH)); return btoa(s); }
  function buildBase64() { return build().then(toBase64); }

  window.ExamWord = {build: build, buildBase64: buildBase64, MIME: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'};
})();
