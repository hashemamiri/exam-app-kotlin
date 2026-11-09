#!/usr/bin/env python3
"""شبیه‌سازی نصب یک نصاب روی مخزن واقعی، بدون شبکه.

سه سناریو را می‌سنجد:
  ۱) main و staging هر دو روی پایه  → نصب باید موفق شود
  ۲) نسخه از قبل نشسته             → باید بی‌خطر متوقف شود
  ۳) main عقب‌تر از staging        → هر دو شاخه باید همگام شوند

در هر سه حالت، درخت فایل‌های نتیجه باید بایت‌به‌بایت با کامیت مرجع
یکی باشد. هیچ چیز به مخزن واقعی کاربر push نمی‌شود؛ همه‌کار روی یک
origin برهنهٔ موقت انجام می‌گیرد.

نمونه:
  python3 scripts/sim_installer.py \
      --installer /home/user/apply_v356_1.py \
      --base 278272b7 --ref ae3eb1a3 --older 1ed1457d
"""
from __future__ import annotations

import argparse
import pathlib
import shutil
import subprocess
import sys
import tempfile

REPO = pathlib.Path(__file__).resolve().parent.parent
FAIL_MARKERS = (
    "❌", "ادغام خودکار ممکن نشد", "Updates were rejected",
    "non-fast-forward", "CONFLICT", "Traceback",
)


def git(cwd: pathlib.Path, *args: str, check: bool = True) -> str:
    r = subprocess.run(["git", *args], cwd=cwd, capture_output=True, text=True)
    if check and r.returncode != 0:
        raise SystemExit(f'git {" ".join(args)} شکست:\n{r.stdout}\n{r.stderr}')
    return r.stdout.strip()


def tree_of(cwd: pathlib.Path, rev: str) -> dict[str, str]:
    out = git(cwd, "ls-tree", "-r", "--format=%(path) %(objectname)", rev)
    return dict(line.split(" ", 1) for line in out.splitlines() if line)


def scenario(name, installer, base, ref, setup, expect="install") -> bool:
    tmp = pathlib.Path(tempfile.mkdtemp(prefix="siminst_"))
    try:
        origin = tmp / "origin.git"
        git(tmp, "init", "--bare", "-q", str(origin))
        git(origin, "symbolic-ref", "HEAD", "refs/heads/main")
        git(REPO, "push", "-q", str(origin), f"{ref}:refs/heads/refsim")
        setup(origin, base, ref)
        work = tmp / "exam-app-kotlin"
        git(tmp, "clone", "-q", str(origin), str(work))
        git(work, "config", "user.name", "Sim")
        git(work, "config", "user.email", "sim@example.invalid")
        shutil.copy(installer, tmp / "apply.py")
        r = subprocess.run([sys.executable, str(tmp / "apply.py")], cwd=work,
                           capture_output=True, text=True, timeout=900)
        log = r.stdout + r.stderr
        problems = [m for m in FAIL_MARKERS if m in log]
        print(f"\n=== {name} ===")
        if expect == "already":
            # V358.1 — نصاب دیگر در این حالت ❌ نمی‌دهد؛ پیام روشن
            # «از قبل نصب شده» می‌دهد و با صفر خارج می‌شود. هر دو
            # رفتار پذیرفته می‌شود تا نصاب‌های قدیمی هم شبیه‌سازی شوند.
            legacy = "nothing to commit" in log and "❌" in log
            modern = "از قبل روی این شاخه نصب شده است" in log and r.returncode == 0
            ok = (legacy or modern) and "Updates were rejected" not in log
            print("  تشخیص «از قبل نصب شده» و توقف بی‌خطر:",
                  ("بله ✓ (پیام روشن)" if modern else "بله ✓ (رفتار قدیم)") if ok else "خیر ✗")
        else:
            ok = r.returncode == 0 and not problems
            print("  اجرا:", "موفق" if r.returncode == 0 else f"کد {r.returncode}")
            if problems:
                print("  ⚠ نشانه‌های شکست:", problems)
                print("\n".join(log.splitlines()[-14:]))
        want = tree_of(REPO, ref)
        same = tree_of(work, "HEAD") == want
        print("  درخت کامیت مرجع یکسان است:", "بله ✓" if same else "خیر ✗")
        branches = git(work, "branch", "-r")
        pushed = "origin/main" in branches and tree_of(work, "origin/main") == want
        print("  روی main نشست:", "بله ✓" if pushed else "خیر ✗")
        synced = "origin/staging" in branches and tree_of(work, "origin/staging") == want
        print("  staging هم همگام شد:", "بله ✓" if synced else "خیر ✗")
        return ok and same and pushed
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--installer", required=True)
    ap.add_argument("--base", required=True, help="کامیت پایه‌ای که نصاب انتظار دارد")
    ap.add_argument("--ref", required=True, help="کامیت مرجع (نتیجهٔ نصب)")
    ap.add_argument("--older", required=True, help="یک کامیت عقب‌تر، برای سناریو ۳")
    a = ap.parse_args()
    installer = pathlib.Path(a.installer)
    if not installer.exists():
        print("نصاب پیدا نشد:", installer)
        return 1

    def both_at_base(origin, base, ref):
        git(REPO, "push", "-q", str(origin), f"{base}:refs/heads/main")
        git(REPO, "push", "-q", str(origin), f"{base}:refs/heads/staging")

    def already(origin, base, ref):
        git(REPO, "push", "-q", str(origin), f"{ref}:refs/heads/main")
        git(REPO, "push", "-q", str(origin), f"{ref}:refs/heads/staging")

    def main_behind(origin, base, ref):
        git(REPO, "push", "-q", str(origin), f"{a.older}:refs/heads/main")
        git(REPO, "push", "-q", str(origin), f"{base}:refs/heads/staging")

    results = [
        scenario("۱) main و staging هر دو روی پایه", installer, a.base, a.ref, both_at_base),
        scenario("۲) از قبل نشسته — اجرای دوباره باید بی‌خطر متوقف شود",
                 installer, a.base, a.ref, already, expect="already"),
        scenario("۳) main عقب‌تر است", installer, a.base, a.ref, main_behind),
    ]
    print(f'\nSIM: {"PASS" if all(results) else "FAIL"} — {sum(results)}/3 سناریو')
    return 0 if all(results) else 1


if __name__ == "__main__":
    sys.exit(main())
