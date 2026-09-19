#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
پلِ پرداخت (V187) — روی یک سرور ایرانی با IP ثابت اجرا می‌شود و فقط دو مسیر زرین‌پال را
از Edge Function «wallet-payment» به payment.zarinpal.com می‌رساند تا IP ثابتِ همین سرور
در پنل زرین‌پال ثبت شود. بدون وابستگی خارجی (کتابخانهٔ استاندارد پایتون ۳).

امنیت: هر درخواست باید هدر  X-Relay-Token  برابر با RELAY_TOKEN داشته باشد؛ فقط POST به
/request و /verify پذیرفته می‌شود؛ بدنه حداکثر ۱۶ کیلوبایت؛ مقصد ثابت است (هیچ URL دلخواهی
ارسال نمی‌شود). لاگ فقط کد وضعیت را می‌نویسد (نه مرچنت/مبلغ).
"""
import json, os, sys, time, hmac, urllib.request, urllib.error
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

TOKEN = os.environ.get('RELAY_TOKEN', '').strip()
PORT = int(os.environ.get('RELAY_PORT', '8787'))
UPSTREAM = {'/request': 'https://payment.zarinpal.com/pg/v4/payment/request.json',
            '/verify': 'https://payment.zarinpal.com/pg/v4/payment/verify.json'}

class H(BaseHTTPRequestHandler):
    server_version = 'PayRelay/1'
    def log_message(self, fmt, *a): sys.stderr.write('%s %s\n' % (time.strftime('%Y-%m-%d %H:%M:%S'), fmt % a))
    def _send(self, status, obj):
        data = json.dumps(obj, ensure_ascii=False).encode('utf-8')
        self.send_response(status); self.send_header('Content-Type', 'application/json; charset=utf-8')
        self.send_header('Content-Length', str(len(data))); self.end_headers(); self.wfile.write(data)
    def do_GET(self):
        if self.path == '/health': return self._send(200, {'ok': True})
        self._send(404, {'error': 'not_found'})
    def do_POST(self):
        if not TOKEN or not hmac.compare_digest(self.headers.get('X-Relay-Token', ''), TOKEN):
            return self._send(401, {'error': 'unauthorized'})
        up = UPSTREAM.get(self.path.split('?')[0])
        if not up: return self._send(404, {'error': 'not_found'})
        n = int(self.headers.get('Content-Length') or 0)
        if n <= 0 or n > 16384: return self._send(413, {'error': 'bad_length'})
        body = self.rfile.read(n)
        try: json.loads(body)
        except Exception: return self._send(400, {'error': 'bad_json'})
        req = urllib.request.Request(up, data=body, method='POST',
              headers={'Content-Type': 'application/json', 'Accept': 'application/json', 'User-Agent': 'exam-pay-relay/1'})
        try:
            with urllib.request.urlopen(req, timeout=25) as r:
                status, out = r.status, r.read()
        except urllib.error.HTTPError as e:
            status, out = e.code, e.read()
        except Exception as e:
            self.log_message('upstream error %s', type(e).__name__); return self._send(502, {'error': 'upstream_unreachable'})
        self.log_message('%s -> %s', self.path, status)
        self.send_response(status); self.send_header('Content-Type', 'application/json; charset=utf-8')
        self.send_header('Content-Length', str(len(out))); self.end_headers(); self.wfile.write(out)

if __name__ == '__main__':
    if len(TOKEN) < 32: sys.exit('RELAY_TOKEN باید حداقل ۳۲ کاراکتر باشد (openssl rand -hex 32).')
    print('pay relay listening on 127.0.0.1:%d' % PORT, flush=True)
    ThreadingHTTPServer(('127.0.0.1', PORT), H).serve_forever()
