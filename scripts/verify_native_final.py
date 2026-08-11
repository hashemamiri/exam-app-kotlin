#!/usr/bin/env python3
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
errors: list[str] = []

def require(condition: bool, message: str) -> None:
    if not condition:
        errors.append(message)

main_files = list((ROOT / "app/src/main/java").rglob("*.kt"))
main_text = "\n".join(path.read_text(errors="ignore") for path in main_files)
edge_files = list((ROOT / "supabase/functions").glob("*/index.ts"))
edge_text = "\n".join(path.read_text(errors="ignore") for path in edge_files)
manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text()
workflow = (ROOT / ".github/workflows/android.yml").read_text()
hardening = (ROOT / "supabase/migrations/20260812_native_final_hardening.sql").read_text()

require("android.webkit" not in main_text, "WebView/android.webkit import remains in Native source")
require(not re.search(r"\b(val|var)\s+plain_password\b", main_text), "plain_password model field remains")
require(not re.search(r'\.from\("[^"]+"\)\.(?:insert|update|upsert|delete)\b', main_text),
        "direct public-table mutation remains in APK repository")
require("plain_password:" not in edge_text and "plain_password =" not in edge_text,
        "Edge Function still writes/reads plain_password")
require("npm:@supabase/supabase-js@2.112.2" in edge_text, "Edge dependency is not mature pinned version")
require("minimum-dependency-age=0" not in workflow, "Deno dependency age protection disabled")
require('android:allowBackup="false"' in manifest, "Android backup is not disabled")
require('android:usesCleartextTraffic="false"' in manifest, "Cleartext traffic is enabled")
require("APK retention deleted" in workflow, "APK retention step missing")
require("Release APK signing certificate: VERIFIED" in workflow, "release certificate verification missing")
require("v11_authenticated_upload_exam_images" in hardening, "Storage owner-prefix policy missing")
require("drop column if exists plain_password" in hardening.lower(), "plain_password DROP missing")

for match in re.finditer(r"(?im)^\s*(delete\s+from|update\s+)([^;]+);", hardening):
    statement = match.group(0)
    if not re.search(r"(?i)\bwhere\b", statement):
        errors.append(f"UPDATE/DELETE without WHERE at line {hardening[:match.start()].count(chr(10))+1}")

if errors:
    print("FINAL_NATIVE_VERIFY=FAIL")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print(f"FINAL_NATIVE_VERIFY=PASS kotlin_files={len(main_files)} edge_functions={len(edge_files)}")
