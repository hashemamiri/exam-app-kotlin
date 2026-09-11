#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""V160.2 — یک‌بار اجرا: همهٔ اشیاء صندوقچهٔ آروان را public-read می‌کند.
(اشیائی که قبل از V160.2 آپلود شده‌اند ACL خصوصی دارند و با HTTP 403 خوانده نمی‌شوند.)

اجرا در WSL:
  pip3 install boto3
  S3_ACCESS_KEY_ID=... S3_SECRET_ACCESS_KEY=... python3 scripts/arvan_make_public.py
کلیدها را فقط در همین دستور موقت بدهید؛ در هیچ فایلی ذخیره نکنید.
"""
import os, sys
try:
    import boto3
except ImportError:
    print("✗ ابتدا: pip3 install boto3"); sys.exit(1)
endpoint = os.environ.get("S3_ENDPOINT", "https://s3.ir-thr-at1.arvanstorage.ir")
bucket = os.environ.get("S3_BUCKET", "azmoon-media")
ak, sk = os.environ.get("S3_ACCESS_KEY_ID"), os.environ.get("S3_SECRET_ACCESS_KEY")
if not ak or not sk:
    print("✗ S3_ACCESS_KEY_ID و S3_SECRET_ACCESS_KEY را در محیط بدهید."); sys.exit(1)
s3 = boto3.client("s3", endpoint_url=endpoint, aws_access_key_id=ak, aws_secret_access_key=sk, region_name="default")
n = 0
for page in s3.get_paginator("list_objects_v2").paginate(Bucket=bucket):
    for o in page.get("Contents", []):
        s3.put_object_acl(Bucket=bucket, Key=o["Key"], ACL="public-read"); n += 1
        print("  ✓", o["Key"])
print(f"\n✅ {n} فایل عمومی شد.")
