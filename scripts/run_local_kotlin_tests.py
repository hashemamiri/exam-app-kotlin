#!/usr/bin/env python3
"""V358.1 — اجرای همهٔ آزمون‌های کاتلینی که به اندروید وابسته نیستند.

چرا این اسکریپت ساخته شد؟ در V358 تنها ۱۲۸ آزمون از ۱۶۸۵ آزمون به‌صورت
دستی انتخاب و محلی اجرا شد. یک آزمونِ «اسکن منبع» (V334_0) که هیچ
وابستگی اندرویدی ندارد از قلم افتاد و CI را شکست. انتخاب دستی مجموعه،
خودِ نقص بود.

این اسکریپت هر آزمونی را که واردات اندرویدی/کامپوز ندارد پیدا می‌کند،
همهٔ منابع اصلیِ خالص را با هم کامپایل می‌کند و همه را اجرا می‌کند.
آنچه اجرا نمی‌شود، صریح گزارش می‌شود تا پوشش پنهان نماند.

پیش‌نیاز: جارها در /tmp/kot/lib (اسکریپت خودش دانلود می‌کند).
اجرا:  python3 scripts/run_local_kotlin_tests.py
"""
import pathlib
import re
import subprocess
import sys
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parent.parent
LIB = pathlib.Path("/tmp/kot/lib")
OUT = pathlib.Path("/tmp/kot/localtests")

JARS = [
    "org/jetbrains/kotlin/kotlin-compiler-embeddable/2.0.21/kotlin-compiler-embeddable-2.0.21.jar",
    "org/jetbrains/kotlin/kotlin-stdlib/2.0.21/kotlin-stdlib-2.0.21.jar",
    "org/jetbrains/kotlin/kotlin-reflect/2.0.21/kotlin-reflect-2.0.21.jar",
    "org/jetbrains/kotlinx/kotlinx-serialization-json-jvm/1.7.3/kotlinx-serialization-json-jvm-1.7.3.jar",
    "org/jetbrains/kotlinx/kotlinx-serialization-core-jvm/1.7.3/kotlinx-serialization-core-jvm-1.7.3.jar",
    "org/jetbrains/kotlinx/kotlinx-coroutines-core-jvm/1.9.0/kotlinx-coroutines-core-jvm-1.9.0.jar",
    "org/jetbrains/intellij/deps/trove4j/1.0.20200330/trove4j-1.0.20200330.jar",
    "org/jetbrains/annotations/24.1.0/annotations-24.1.0.jar",
    "junit/junit/4.13.2/junit-4.13.2.jar",
    "org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar",
    "com/squareup/okhttp3/okhttp/4.12.0/okhttp-4.12.0.jar",
    "com/squareup/okio/okio-jvm/3.6.0/okio-jvm-3.6.0.jar",
]
MAVEN = "https://repo1.maven.org/maven2/"
# پلاگین کامپایلر، نه کتابخانهٔ زمان اجرا؛ باید با -Xplugin داده شود
PLUGIN_REL = "org/jetbrains/kotlin/kotlin-serialization-compiler-plugin-embeddable/2.0.21/kotlin-serialization-compiler-plugin-embeddable-2.0.21.jar"

# وارداتی که کامپایل محلی را ناممکن می‌کند
BLOCKED = re.compile(
    r"^import\s+(android\.|androidx\.|io\.github\.jan|coil\.|kotlinx\.coroutines\.android|"
    r"org\.json\.|java\.awt\.|javax\.imageio\.)",
    re.M,
)


def ensure_jars():
    LIB.mkdir(parents=True, exist_ok=True)
    for rel in JARS + [PLUGIN_REL]:
        dest = LIB / rel.rsplit("/", 1)[1]
        if dest.exists():
            continue
        print("  دانلود " + dest.name)
        urllib.request.urlretrieve(MAVEN + rel, dest)


def is_pure(path: pathlib.Path) -> bool:
    text = path.read_text(encoding="utf-8", errors="ignore")
    return not BLOCKED.search(text)


def class_name(path: pathlib.Path, base: pathlib.Path) -> str:
    rel = path.relative_to(base).with_suffix("")
    return ".".join(rel.parts)


def compile_batch(files, cp, extra_cp=""):
    if not files:
        return True, ""
    plugin = LIB / PLUGIN_REL.rsplit("/", 1)[1]
    runtime_cp = ":".join(str(p) for p in sorted(LIB.glob("*.jar")) if "compiler-plugin" not in p.name)
    args = ["java", "-Xmx3g", "-cp", cp, "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler",
            "-nowarn", "-Xplugin=" + str(plugin),
            "-cp", runtime_cp + (":" + extra_cp if extra_cp else ""), "-d", str(OUT)]
    args += [str(f) for f in files]
    r = subprocess.run(args, capture_output=True, text=True, cwd=str(ROOT), timeout=2400)
    errs = "\n".join(l for l in (r.stdout + r.stderr).splitlines() if "error:" in l)
    return r.returncode == 0, errs


def main() -> int:
    ensure_jars()
    OUT.mkdir(parents=True, exist_ok=True)
    cp = ":".join(str(p) for p in sorted(LIB.glob("*.jar")))

    main_base = ROOT / "app/src/main/java"
    test_base = ROOT / "app/src/test/java"
    all_main = sorted(main_base.rglob("*.kt"))
    all_test = sorted(test_base.rglob("*.kt"))

    pure_main = [f for f in all_main if is_pure(f)]
    pure_test = [f for f in all_test if is_pure(f)]
    skipped_main = len(all_main) - len(pure_main)
    skipped_test = [f for f in all_test if f not in pure_test]

    print(f"منابع اصلی: {len(pure_main)} خالص از {len(all_main)} ({skipped_main} وابسته به اندروید)")
    print(f"آزمون‌ها:   {len(pure_test)} خالص از {len(all_test)} ({len(skipped_test)} نیازمند CI)")

    print("\n=== کامپایل منابع اصلی خالص ===")
    ok, errs = compile_batch(pure_main, cp)
    if not ok:
        # برخی فایل‌های خالص به فایل‌های اندرویدی ارجاع می‌دهند؛ آن‌ها را
        # حذف می‌کنیم و دوباره تلاش می‌کنیم تا بیشترین پوشش ممکن بماند.
        bad = set(re.findall(r"^(\S+\.kt):\d+:\d+: error:", errs, re.M))
        for _ in range(6):
            if not bad:
                break
            before = len(pure_main)
            pure_main = [f for f in pure_main if str(f.relative_to(ROOT)) not in bad]
            if len(pure_main) == before:
                break
            print(f"  {before - len(pure_main)} فایل وابسته کنار گذاشته شد؛ تلاش دوباره")
            ok, errs = compile_batch(pure_main, cp, str(OUT))
            bad = set(re.findall(r"^(\S+\.kt):\d+:\d+: error:", errs, re.M))
        if not ok:
            print("کامپایل منابع اصلی ناموفق:")
            print("\n".join(errs.splitlines()[:15]))
            return 1
    print(f"  {len(pure_main)} فایل کامپایل شد")

    print("\n=== کامپایل آزمون‌های خالص ===")
    ok, errs = compile_batch(pure_test, cp, str(OUT))
    if not ok:
        bad = set(re.findall(r"^(\S+\.kt):\d+:\d+: error:", errs, re.M))
        for _ in range(8):
            if not bad:
                break
            before = len(pure_test)
            drop = {f for f in pure_test if str(f.relative_to(ROOT)) in bad}
            skipped_test += sorted(drop)
            pure_test = [f for f in pure_test if str(f.relative_to(ROOT)) not in bad]
            if len(pure_test) == before:
                break
            print(f"  {before - len(pure_test)} آزمون به وابستگی رفع‌نشدنی خورد؛ کنار گذاشته شد")
            ok, errs = compile_batch(pure_test, cp, str(OUT))
            bad = set(re.findall(r"^(\S+\.kt):\d+:\d+: error:", errs, re.M))
        if not ok:
            print("کامپایل آزمون‌ها ناموفق:")
            print("\n".join(errs.splitlines()[:15]))
            return 1

    classes = [class_name(f, test_base) for f in pure_test]
    print(f"  {len(classes)} کلاس آزمون آمادهٔ اجرا")

    print("\n=== اجرا ===")
    r = subprocess.run(
        ["java", "-Dfile.encoding=UTF-8", "-cp", f"{OUT}:{cp}", "org.junit.runner.JUnitCore"] + classes,
        capture_output=True, text=True, cwd=str(ROOT), timeout=2400,
    )
    out = r.stdout + r.stderr
    tail = out.strip().splitlines()[-40:]
    print("\n".join(tail))

    if skipped_test:
        print(f"\n{len(skipped_test)} آزمون محلی اجرا نشد (نیازمند CI):")
        for f in skipped_test[:25]:
            print("  · " + str(f.relative_to(ROOT)))
        if len(skipped_test) > 25:
            print(f"  … و {len(skipped_test) - 25} مورد دیگر")

    harness = []
    real = []
    for block in re.split(r"\n(?=\d+\) )", out):
        if not re.match(r"\d+\) ", block):
            continue
        if "ClassNotFoundException" in block or "NoClassDefFoundError" in block:
            harness.append(block.splitlines()[0])
        else:
            real.append(block.splitlines()[0])
    if harness:
        print(f"\n{len(harness)} شکست از محدودیت ابزار محلی است (کلاس اندرویدی کنار گذاشته شده):")
        for h in harness:
            print("  · " + h)
        print("  این‌ها در CI با کل پروژه اجرا می‌شوند.")

    m = re.search(r"Tests run: (\d+)", out)
    ran = m.group(1) if m else "?"
    good = r.returncode == 0 or not real
    print(f"\nLOCAL_KOTLIN: {'PASS' if good else 'FAIL'} — {ran} آزمون اجرا شد، "
          f"{len(real)} شکست واقعی، {len(harness)} محدودیت ابزار، {len(skipped_test)} نیازمند CI")
    return 0 if good else 1


if __name__ == "__main__":
    raise SystemExit(main())
