#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""V340.1 — نگهبان دیدپذیری کاتلین: هیچ عضو public نباید نوع internal را در امضایش نشان دهد.
همان خطایی که CI در V340 داد (Function public exposes its internal parameter type)؛
چون کامپایلر کاتلین محلی نداریم، ایستا بررسی می‌شود — مثل check_kotlin_braces/escapes.
فقط اعضای سطح فایل و سطح کلاس بررسی می‌شوند (نه متغیر/تابع محلی داخل بدنه)."""
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
MAIN = ROOT / 'app/src/main/java'

MODS = r'(?:abstract|open|final|sealed|data|value|enum|annotation|inner|companion|external|inline|noinline|crossinline|vararg|suspend|operator|infix|tailrec|const|lateinit|override|expect|actual)'
DECL = re.compile(r'^\s*(?:(public|internal|private|protected)\s+)?(?:' + MODS + r'\s+)*(class|object|interface|fun|val|var)\s+([A-Za-z_][\w]*)')
TYPE_DECL = re.compile(r'^\s*(?:(public|internal|private|protected)\s+)?(?:' + MODS + r'\s+)*(class|object|interface|typealias)\s+([A-Za-z_][\w]*)', re.M)


def strip_line(line):
    line = re.sub(r'"(?:[^"\\]|\\.)*"', '""', line)
    line = re.sub(r"'(?:[^'\\]|\\.)*'", "''", line)
    return line.split('//')[0]


files = sorted(MAIN.rglob('*.kt'))
internal_types, public_types = set(), set()
for f in files:
    s = f.read_text(encoding='utf-8', errors='ignore')
    for m in TYPE_DECL.finditer(s):
        vis, name = m.group(1), m.group(3)
        if vis == 'internal':
            internal_types.add(name)
        elif vis in (None, 'public'):
            public_types.add(name)
internal_only = internal_types - public_types

violations = []
for f in files:
    raw = f.read_text(encoding='utf-8', errors='ignore').split('\n')
    depth = 0
    stack = []  # {'depth', 'kind', 'vis'}
    in_block_comment = False
    ctor_vis, ctor_parens = None, 0
    for i, line in enumerate(raw):
        s = strip_line(line)
        if in_block_comment:
            if '*/' in s:
                s = s.split('*/', 1)[1]
                in_block_comment = False
            else:
                continue
        if '/*' in s and '*/' not in s.split('/*', 1)[1]:
            s = s.split('/*', 1)[0]
            in_block_comment = True
        if not s.strip() or s.lstrip().startswith(('*', '@')):
            depth += s.count('{') - s.count('}')
            continue
        while stack and stack[-1]['depth'] > depth:
            stack.pop()
        m = DECL.match(s)
        opens, closes = s.count('{'), s.count('}')
        if m:
            vis, kind, name = m.group(1), m.group(2), m.group(3)
            inner = stack[-1] if stack else None
            is_member = inner is None or inner['kind'] in ('class', 'object', 'interface')
            outer_vis = inner['vis'] if inner else 'public'
            hidden = vis in ('private', 'protected', 'internal') or outer_vis != 'public'
            eff = 'public' if not hidden else 'internal'
            # پارامترهای سازندهٔ یک نوع internal خودشان internal‌اند
            if ctor_vis is not None and kind in ('val', 'var'):
                eff = ctor_vis
            head = s.split(kind)[0]
            if is_member and eff == 'public' and 'override' not in head:
                sig = s
                if '(' in s and s.count('(') > s.count(')'):
                    j = i + 1
                    while j < len(raw) and j < i + 40:
                        nxt = strip_line(raw[j])
                        sig += ' ' + nxt.strip()
                        if ')' in nxt:
                            break
                        j += 1
                sig = sig.split('{')[0].split('=')[0] if kind in ('val', 'var') else sig.split('{')[0]
                used = sorted({t for t in internal_only if re.search(r'\b' + re.escape(t) + r'\b', sig)})
                if used:
                    violations.append('%s:%d %s %s → %s' % (f.relative_to(ROOT), i + 1, kind, name, ', '.join(used)))
            if kind in ('class', 'object', 'interface'):
                bal = s.count('(') - s.count(')')
                if bal > 0:
                    ctor_vis, ctor_parens = eff, bal
            if opens > closes:
                stack.append({'depth': depth + 1, 'kind': kind, 'vis': eff})
        elif opens > closes:
            stack.append({'depth': depth + 1, 'kind': 'block', 'vis': 'internal'})
        if ctor_vis is not None and not (m and m.group(3) in ('class', 'object', 'interface')):
            ctor_parens += s.count('(') - s.count(')')
            if ctor_parens <= 0:
                ctor_vis, ctor_parens = None, 0
        depth += opens - closes
        if depth < 0:
            depth = 0

print('internal-only types: %d | files scanned: %d' % (len(internal_only), len(files)))
for v in violations:
    print('  x', v)
print('KOTLIN_VISIBILITY:', 'FAIL (%d)' % len(violations) if violations else 'PASS')
sys.exit(1 if violations else 0)
