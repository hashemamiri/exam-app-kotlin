#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""V360 — بارگذاری شاردهای بانک مرجع به همان ذخیرهٔ S3سازگار پروژه.

این اسکریپت فقط فایلهای پوشهٔ publish/ را میخواند و با امضای AWS SigV4
مستقیم PUT میکند. هیچ کلیدی در کد، چت، گیت یا APK نیست؛ مقادیر فقط از
متغیرهای محیط WSL خوانده میشوند (همانهایی که در Secrets سوپابیس هستند).

متغیرها:
  S3_ENDPOINT           مثلا https://<account>.r2.cloudflarestorage.com
  S3_ACCESS_KEY_ID      یا R2_ACCESS_KEY_ID
  S3_SECRET_ACCESS_KEY  یا R2_SECRET_ACCESS_KEY
  S3_BUCKET             پیشفرض azmoon-media
  S3_REGION             پیشفرض auto
  S3_PUBLIC_BASE        مثلا https://azmoon-media.s3.ir-thr-at1.arvanstorage.ir

استفاده:
  python3 scripts/upload_reference_bank.py --dir /مسیر/publish
  python3 scripts/upload_reference_bank.py --dir /مسیر/publish --apply
  python3 scripts/upload_reference_bank.py --dir /مسیر/publish --env-file ~/.exam_s3 --apply
  python3 scripts/upload_reference_bank.py --dir /مسیر/publish --only questions/g1.json \
      --env-file ~/.exam_s3 --apply

بدون --apply فقط گزارش میدهد و هیچ چیز آپلود نمیشود.
"""
import argparse
import datetime
import hashlib
import hmac
import json
import os
import pathlib
import sys
import urllib.parse
import urllib.request

PREFIX = "reference-bank"
EXPIRES = 300

CT = {
    ".json": "application/json; charset=utf-8",
    ".jsonl": "application/x-ndjson; charset=utf-8",
    ".csv": "text/csv; charset=utf-8",
    ".webp": "image/webp",
    ".png": "image/png",
    ".jpg": "image/jpeg",
    ".jpeg": "image/jpeg",
    ".html": "text/html; charset=utf-8",
}


def env(name, default=""):
    return os.environ.get(name, default).strip()


def load_env_file(path):
    if not path:
        return
    for line in pathlib.Path(path).read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        k, v = line.split("=", 1)
        os.environ[k.strip()] = v.strip().strip('"').strip("'")


def cfg():
    ep = env("S3_ENDPOINT")
    if not ep and env("R2_ACCOUNT_ID"):
        ep = "https://" + env("R2_ACCOUNT_ID") + ".r2.cloudflarestorage.com"
    return {
        "endpoint": ep.rstrip("/"),
        "ak": env("S3_ACCESS_KEY_ID") or env("R2_ACCESS_KEY_ID"),
        "sk": env("S3_SECRET_ACCESS_KEY") or env("R2_SECRET_ACCESS_KEY"),
        "bucket": env("S3_BUCKET") or env("R2_BUCKET") or "azmoon-media",
        "region": env("S3_REGION") or "auto",
        "public": (env("S3_PUBLIC_BASE") or env("R2_PUBLIC_BASE")).rstrip("/"),
    }


def sha256_hex(b):
    return hashlib.sha256(b).hexdigest()


def hmac_sha256(key, msg):
    return hmac.new(key, msg.encode("utf-8"), hashlib.sha256).digest()


def enc(s):
    return urllib.parse.quote(str(s), safe="-_.~")


def presigned_put(c, key, content_type, size):
    host = urllib.parse.urlparse(c["endpoint"]).netloc
    now = datetime.datetime.now(datetime.timezone.utc)
    amz_date = now.strftime("%Y%m%dT%H%M%SZ")
    date = amz_date[:8]
    scope = date + "/" + c["region"] + "/s3/aws4_request"
    canonical_uri = "/" + c["bucket"] + "/" + "/".join(enc(x) for x in key.split("/"))
    signed_headers = "content-type;host;x-amz-acl"
    query = [
        ("X-Amz-Algorithm", "AWS4-HMAC-SHA256"),
        ("X-Amz-Credential", c["ak"] + "/" + scope),
        ("X-Amz-Date", amz_date),
        ("X-Amz-Expires", str(EXPIRES)),
        ("X-Amz-SignedHeaders", signed_headers),
    ]
    query.sort()
    canonical_query = "&".join(enc(k) + "=" + enc(v) for k, v in query)
    canonical_headers = (
        "content-type:" + content_type + "\n"
        + "host:" + host + "\n"
        + "x-amz-acl:public-read\n"
    )
    canonical_request = "\n".join([
        "PUT", canonical_uri, canonical_query, canonical_headers,
        signed_headers, "UNSIGNED-PAYLOAD",
    ])
    string_to_sign = "\n".join([
        "AWS4-HMAC-SHA256", amz_date, scope, sha256_hex(canonical_request.encode("utf-8")),
    ])
    k = hmac_sha256(("AWS4" + c["sk"]).encode("utf-8"), date)
    k = hmac_sha256(k, c["region"])
    k = hmac_sha256(k, "s3")
    k = hmac_sha256(k, "aws4_request")
    signature = hmac.new(k, string_to_sign.encode("utf-8"), hashlib.sha256).hexdigest()
    url = c["endpoint"] + canonical_uri + "?" + canonical_query + "&X-Amz-Signature=" + signature
    headers = {
        "Content-Type": content_type,
        "x-amz-acl": "public-read",
        "x-amz-content-sha256": "UNSIGNED-PAYLOAD",
    }
    return url, headers


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--dir", required=True)
    ap.add_argument("--apply", action="store_true")
    ap.add_argument("--env-file")
    ap.add_argument("--base", help="آدرس عمومی رسانه، فقط برای مانیفست خروجی")
    ap.add_argument("--only", help="فقط یک فایل مشخص برحسب مسیر نسبی به publish بارگذاری شود")
    args = ap.parse_args()
    load_env_file(args.env_file)
    c = cfg()
    root = pathlib.Path(args.dir)
    if not root.is_dir():
        print("REFERENCE_BANK_UPLOAD: FAIL — پوشه پیدا نشد:", root)
        return 1
    files = sorted(p for p in root.rglob("*") if p.is_file() and p.name != "upload_manifest.json")
    if args.only:
        wanted = args.only.lstrip("/")
        files = [p for p in files if p.relative_to(root).as_posix() == wanted]
        if not files:
            print("REFERENCE_BANK_UPLOAD: FAIL — فایل پیدا نشد:", wanted)
            return 1
        print("فقط یک فایل:", wanted)
    if not files:
        print("REFERENCE_BANK_UPLOAD: FAIL — پوشه خالی است")
        return 1
    total = sum(p.stat().st_size for p in files)
    print("فایلها:", len(files), "| حجم:", round(total / 1024 / 1024, 2), "مگابایت")
    print("پیشوند:", PREFIX + "/", "| مقصد:", c["bucket"])
    if not args.apply:
        print("حالت گزارش: هیچ چیز آپلود نشد. برای اجرا --apply بدهید.")
        return 0
    missing = [k for k in ("endpoint", "ak", "sk") if not c[k]]
    if missing:
        print("REFERENCE_BANK_UPLOAD: FAIL — تنظیمات ناقص:", ", ".join(missing))
        print("مقادیر را فقط در WSL یا --env-file بگذارید؛ هرگز در چت نفرستید.")
        return 1
    public_base = (args.base or c["public"]).rstrip("/")
    if not public_base:
        public_base = c["endpoint"] + "/" + c["bucket"]
    out, done = [], 0
    for p in files:
        rel = p.relative_to(root).as_posix()
        key = PREFIX + "/" + rel
        ct = CT.get(p.suffix.lower(), "application/octet-stream")
        data = p.read_bytes()
        url, headers = presigned_put(c, key, ct, len(data))
        req = urllib.request.Request(url, data=data, headers=headers, method="PUT")
        try:
            with urllib.request.urlopen(req, timeout=60) as r:
                if r.status not in (200, 201, 204):
                    raise RuntimeError("HTTP " + str(r.status))
        except Exception as e:
            print("REFERENCE_BANK_UPLOAD: FAIL —", rel, "→", str(e)[:160])
            return 1
        out.append({"key": key, "bytes": len(data), "sha256": sha256_hex(data),
                    "url": public_base + "/" + key})
        done += 1
        if done % 250 == 0:
            print("  آپلود شد:", done, "از", len(files))
    manifest = {"version": "V360", "prefix": PREFIX, "public_base": public_base,
                "files": len(out), "bytes": total, "objects": out}
    (root / "upload_manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=1), encoding="utf-8")
    print("REFERENCE_BANK_UPLOAD: PASS —", done, "فایل آپلود شد")
    print("مانیفست:", root / "upload_manifest.json")
    return 0


if __name__ == "__main__":
    sys.exit(main())
