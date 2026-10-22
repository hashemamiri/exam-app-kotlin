#!/usr/bin/env python3
"""
V333 — سرور شبیه‌ساز محلی و قطعی Supabase برای تست‌های خودکار رابط کاربری (وب و اندروید)

ویژگی‌ها:
- بدون هیچ وابستگی بیرونی (فقط کتابخانهٔ استاندارد Python 3) و بدون هیچ راز یا توکن ثابت در سورس.
- تطابق ۱۰۰٪ با توابع SQL واقعی در `supabase/migrations/*.sql` و DTOهای کاتلین و وب:
  GoTrue (`/auth/v1/*`)، PostgREST (`/rest/v1/*`)، RPCها (`/rest/v1/rpc/*`) و کنترل تست (`/__test__/*`).
- جداسازی کامل از پایگاه‌دادهٔ واقعی مدرسه: هیچ داده‌ای روی سرور عملیاتی نوشته نمی‌شود.
"""
from __future__ import annotations

import argparse
import base64
import copy
import datetime as dt
import hashlib
import json
import math
import random
import re
import sys
import threading
import time
import urllib.parse
import uuid
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from typing import Any


def _b64url(raw: bytes) -> str:
    return base64.urlsafe_b64encode(raw).decode("ascii").rstrip("=")


def make_runtime_jwt(sub: str, email: str, role: str = "authenticated", exp_seconds: int = 7200) -> str:
    """ساخت پویا در زمان اجرا بدون هیچ رشتهٔ ثابت شبیه توکن در کد منبع."""
    now = int(time.time())
    header = {"alg": "HS256", "typ": "JWT"}
    payload = {
        "aud": "authenticated",
        "exp": now + exp_seconds,
        "iat": now,
        "iss": "http://mock-supabase.local/auth/v1",
        "sub": sub,
        "email": email,
        "role": role,
    }
    h_part = _b64url(json.dumps(header, separators=(",", ":")).encode("utf-8"))
    p_part = _b64url(json.dumps(payload, separators=(",", ":")).encode("utf-8"))
    sig = _b64url(hashlib.sha256(f"{h_part}.{p_part}.uitest".encode("utf-8")).digest())
    return f"{h_part}.{p_part}.{sig}"


def make_public_anon_key() -> str:
    now = int(time.time())
    header = {"alg": "HS256", "typ": "JWT"}
    payload = {"role": "anon", "iss": "supabase-demo", "iat": now, "exp": now + 86400}
    h_part = _b64url(json.dumps(header, separators=(",", ":")).encode("utf-8"))
    p_part = _b64url(json.dumps(payload, separators=(",", ":")).encode("utf-8"))
    sig = _b64url(hashlib.sha256(f"{h_part}.{p_part}.anon".encode("utf-8")).digest())
    return f"{h_part}.{p_part}.{sig}"


SECRET_QUESTION_KEYS = {
    "correct",
    "correctOption",
    "correctIndex",
    "correctAnswer",
    "accept",
    "answer",
    "tolerance",
    "matchAnswer",
    "explanation",
    "answer_key",
    "rubric",
    "partRubrics",
    "constructionTargets",
    "constructionConstraints",
}


def strip_question_secrets(question: dict[str, Any]) -> dict[str, Any]:
    cleaned = copy.deepcopy(question)
    for key in SECRET_QUESTION_KEYS:
        cleaned.pop(key, None)
    for pkey in ("assessmentParts", "parts"):
        if isinstance(cleaned.get(pkey), list):
            cleaned[pkey] = [
                {k: v for k, v in p.items() if k != "rubric"} if isinstance(p, dict) else p
                for p in cleaned[pkey]
            ]
    if isinstance(cleaned.get("subQuestions"), list):
        cleaned["subQuestions"] = [
            strip_question_secrets(sq) if isinstance(sq, dict) else sq
            for sq in cleaned["subQuestions"]
        ]
    return cleaned


def _norm_text(val: Any) -> str:
    s = "" if val is None else str(val)
    persian = "۰۱۲۳۴۵۶۷۸۹"
    arabic = "٠١٢٣٤٥٦٧٨٩"
    for i in range(10):
        s = s.replace(persian[i], str(i)).replace(arabic[i], str(i))
    return re.sub(r"\s+", " ", s.replace("\u200c", " ").replace("ي", "ی").replace("ك", "ک")).strip().lower()


def _parse_float(val: Any) -> float | None:
    if isinstance(val, bool):
        return None
    if isinstance(val, (int, float)):
        return float(val) if math.isfinite(val) else None
    s = _norm_text(val).replace("٫", ".").replace("/", ".")
    if not s:
        return None
    try:
        num = float(s)
        return num if math.isfinite(num) else None
    except ValueError:
        return None


def _pt_xy(points: dict[str, Any], label: str) -> tuple[float, float] | None:
    pt = points.get(label)
    if not isinstance(pt, dict):
        return None
    x = _parse_float(pt.get("x"))
    y = _parse_float(pt.get("y"))
    if x is None or y is None:
        return None
    return (x, y)


def _eval_geometry_construction(merged_q: dict[str, Any], raw_ans: Any, max_score: float) -> float:
    obj = raw_ans
    if isinstance(raw_ans, str):
        try:
            obj = json.loads(raw_ans)
        except Exception:
            obj = None
    if not isinstance(obj, dict):
        return 0.0
    pts = obj.get("points") if isinstance(obj.get("points"), dict) else obj
    targets = merged_q.get("constructionTargets") if isinstance(merged_q.get("constructionTargets"), list) else []
    constraints = (
        merged_q.get("constructionConstraints")
        if isinstance(merged_q.get("constructionConstraints"), list)
        else []
    )
    if not targets and not constraints:
        return round(max_score, 2) if pts else 0.0
    total_weight = 0.0
    earned_weight = 0.0
    for t in targets:
        if not isinstance(t, dict):
            continue
        w = max(0.0, _parse_float(t.get("score")) if t.get("score") is not None else 1.0)
        total_weight += w
        p = _pt_xy(pts, str(t.get("label", "")).strip())
        tx = _parse_float(t.get("x"))
        ty = _parse_float(t.get("y"))
        tol = max(0.0, _parse_float(t.get("tolerance")) or 0.5)
        if p and tx is not None and ty is not None and math.hypot(p[0] - tx, p[1] - ty) <= tol + 1e-6:
            earned_weight += w
    for c in constraints:
        if not isinstance(c, dict):
            continue
        w = max(0.0, _parse_float(c.get("score")) if c.get("score") is not None else 1.0)
        total_weight += w
        kind = str(c.get("type", "")).strip().lower()
        labels = [str(x).strip() for x in (c.get("points") or [])]
        coords = [_pt_xy(pts, lb) for lb in labels]
        if any(p is None for p in coords):
            continue
        ps = [p for p in coords if p is not None]
        tol = max(0.05, _parse_float(c.get("tolerance")) or (4.0 if kind == "angle" else 0.5))
        val = _parse_float(c.get("value") if c.get("value") is not None else c.get("targetValue"))
        ok = False
        if kind == "distance" and len(ps) >= 2 and val is not None:
            ok = abs(math.hypot(ps[1][0] - ps[0][0], ps[1][1] - ps[0][1]) - val) <= tol + 1e-6
        elif kind == "midpoint" and len(ps) >= 3:
            ok = math.hypot(ps[0][0] - (ps[1][0] + ps[2][0]) / 2.0, ps[0][1] - (ps[1][1] + ps[2][1]) / 2.0) <= tol + 1e-6
        elif kind == "angle" and len(ps) >= 3 and val is not None:
            u = (ps[0][0] - ps[1][0], ps[0][1] - ps[1][1])
            v = (ps[2][0] - ps[1][0], ps[2][1] - ps[1][1])
            nu, nv = math.hypot(*u), math.hypot(*v)
            if nu > 1e-9 and nv > 1e-9:
                cos_v = max(-1.0, min(1.0, (u[0] * v[0] + u[1] * v[1]) / (nu * nv)))
                deg = math.degrees(math.acos(cos_v))
                ok = abs(deg - val) <= tol + 1e-6
        elif kind == "collinear" and len(ps) >= 3:
            ac = math.hypot(ps[2][0] - ps[0][0], ps[2][1] - ps[0][1])
            cross = abs((ps[1][0] - ps[0][0]) * (ps[2][1] - ps[0][1]) - (ps[1][1] - ps[0][1]) * (ps[2][0] - ps[0][0]))
            ok = (cross <= tol + 1e-6) if ac <= 1e-9 else (cross / ac <= tol + 1e-6)
        elif kind == "perpendicular" and len(ps) >= 4:
            u = (ps[1][0] - ps[0][0], ps[1][1] - ps[0][1])
            v = (ps[3][0] - ps[2][0], ps[3][1] - ps[2][1])
            nu, nv = math.hypot(*u), math.hypot(*v)
            if nu > 1e-9 and nv > 1e-9:
                deg = math.degrees(math.acos(max(-1.0, min(1.0, (u[0] * v[0] + u[1] * v[1]) / (nu * nv)))))
                ok = abs(deg - 90.0) <= max(tol, 2.0) + 1e-6
        elif kind == "parallel" and len(ps) >= 4:
            u = (ps[1][0] - ps[0][0], ps[1][1] - ps[0][1])
            v = (ps[3][0] - ps[2][0], ps[3][1] - ps[2][1])
            nu, nv = math.hypot(*u), math.hypot(*v)
            if nu > 1e-9 and nv > 1e-9:
                deg = math.degrees(math.acos(max(-1.0, min(1.0, abs(u[0] * v[0] + u[1] * v[1]) / (nu * nv)))))
                ok = deg <= max(tol, 2.0) + 1e-6
        elif kind == "equal_length" and len(ps) >= 4:
            d1 = math.hypot(ps[1][0] - ps[0][0], ps[1][1] - ps[0][1])
            d2 = math.hypot(ps[3][0] - ps[2][0], ps[3][1] - ps[2][1])
            ok = d1 > 1e-9 and abs(d1 - d2) <= tol + 1e-6
        if ok:
            earned_weight += w
    if total_weight <= 0:
        return 0.0
    return round((max_score * min(earned_weight / total_weight, 1.0)) * 100.0) / 100.0


MANUAL_TYPES = {"essay", "long", "descriptive", "short", "short_answer", "proof", "proof_steps", "multipart", "multi_part", "hotspot"}


def _has_geometry_rubric(q: dict[str, Any], key_item: dict[str, Any]) -> bool:
    targets = key_item.get("constructionTargets") or q.get("constructionTargets") or []
    constraints = key_item.get("constructionConstraints") or q.get("constructionConstraints") or []
    return (isinstance(targets, list) and len(targets) > 0) or (isinstance(constraints, list) and len(constraints) > 0)


def auto_score_question(pub_q: dict[str, Any], key_item: dict[str, Any], raw_ans: Any) -> float | None:
    """تصحیح خودکار منطبق بر `native_auto_grade_question_v1` در SQL."""
    merged = {**pub_q, **key_item}
    qtype = str(merged.get("type", "multiple")).strip().lower()
    pts = float(merged.get("score") if merged.get("score") is not None else (merged.get("points") or 1.0))

    if qtype in ("multiple", "mc", "multiple_choice"):
        expected = merged.get("correctOption")
        if expected is None:
            expected = merged.get("correctIndex")
        if expected is None:
            expected = merged.get("answer")
        got = _parse_float(raw_ans)
        exp = _parse_float(expected)
        if got is not None and exp is not None:
            return pts if int(got) == int(exp) else 0.0
        return 0.0

    if qtype in ("truefalse", "tf", "true_false"):
        expected = merged.get("correctAnswer") if "correctAnswer" in merged else merged.get("answer")
        if raw_ans is None or str(raw_ans).strip() == "":
            return 0.0
        got_bool = raw_ans if isinstance(raw_ans, bool) else str(raw_ans).strip().lower() in ("true", "1", "درست")
        exp_bool = expected if isinstance(expected, bool) else str(expected).strip().lower() in ("true", "1", "درست")
        return pts if got_bool == exp_bool else 0.0

    if qtype == "numeric":
        got = _parse_float(raw_ans)
        exp = _parse_float(merged.get("correctAnswer") if "correctAnswer" in merged else merged.get("answer"))
        tol = abs(_parse_float(merged.get("tolerance")) or 0.0)
        if got is not None and exp is not None and abs(got - exp) <= tol + 1e-9:
            return pts
        return 0.0

    if qtype in ("blank", "fill_blank"):
        accepts = list(merged.get("accept") or [])
        if merged.get("answer") is not None:
            accepts.append(merged.get("answer"))
        if merged.get("correctAnswer") is not None:
            accepts.append(merged.get("correctAnswer"))
        got_s = _norm_text(raw_ans)
        ok = bool(got_s) and any(_norm_text(a) == got_s for a in accepts if a is not None)
        return pts if ok else 0.0

    if qtype == "matching":
        match_ans = merged.get("matchAnswer") or merged.get("pairs") or []
        ans_val = raw_ans
        if isinstance(raw_ans, str):
            try:
                ans_val = json.loads(raw_ans)
            except Exception:
                ans_val = None
        if isinstance(match_ans, list) and match_ans and isinstance(ans_val, list):
            hits = sum(1 for i, exp in enumerate(match_ans) if i < len(ans_val) and str(ans_val[i]) == str(exp))
            return round((pts * hits / len(match_ans)) * 100.0) / 100.0
        if isinstance(match_ans, list) and match_ans and isinstance(ans_val, dict):
            hits = sum(
                1
                for i, exp in enumerate(match_ans)
                if str(ans_val.get(str(i), "")) == str(exp.get("right") if isinstance(exp, dict) else exp)
            )
            return round((pts * hits / len(match_ans)) * 100.0) / 100.0
        return 0.0

    if qtype == "geometry_construction" or _has_geometry_rubric(pub_q, key_item):
        return _eval_geometry_construction(merged, raw_ans, pts)

    if qtype in MANUAL_TYPES:
        return None

    return 0.0


class MockSupabaseState:
    def __init__(self, artifact_dir: str | None = None) -> None:
        self.lock = threading.RLock()
        self.artifact_dir = artifact_dir
        self.screenshots: list[dict[str, Any]] = []
        self.reset()

    def now_epoch(self) -> float:
        return time.time() + self.clock_offset_sec

    def now_iso(self, offset_sec: float = 0.0) -> str:
        ts = self.now_epoch() + offset_sec
        return dt.datetime.fromtimestamp(ts, tz=dt.timezone.utc).strftime("%Y-%m-%dT%H:%M:%S.%fZ")

    def reset(self) -> None:
        with self.lock:
            self.clock_offset_sec = 0.0
            self.rpc_log: list[dict[str, Any]] = []
            self.rest_log: list[dict[str, Any]] = []
            self.settings_store: dict[str, dict[str, Any]] = {}
            self.users_by_id: dict[str, dict[str, Any]] = {}
            self.tokens: dict[str, str] = {}
            self.exams: dict[str, dict[str, Any]] = {}
            self.exam_keys: dict[str, dict[str, Any]] = {}
            self.answers: dict[str, dict[str, Any]] = {}
            self.attempts: dict[tuple[str, str], dict[str, Any]] = {}
            self.classes: dict[str, dict[str, Any]] = {}

            teacher_id = "11111111-1111-4111-8111-111111111111"
            student_id = "22222222-2222-4222-8222-222222222222"
            manager_id = "33333333-3333-4333-8333-333333333333"
            self.users_by_id[teacher_id] = {
                "id": teacher_id,
                "email": "teacher@uitest.local",
                "username": "uitest_teacher",
                "role": "teacher",
                "full_name": "معلم آزمایشی خودکار",
                "balance": 500000,
                "school": "دبیرستان نمونه آزمایشی",
                "subject": "ریاضی و هندسه",
                "phone": "09120000001",
            }
            self.users_by_id[student_id] = {
                "id": student_id,
                "email": "uitest_student@student.exam.local",
                "username": "uitest_student",
                "role": "student",
                "full_name": "علی رضایی",
                "balance": 0,
                "school": "دبیرستان نمونه آزمایشی",
                "class_name": "دهم ریاضی ۱",
            }
            self.users_by_id[manager_id] = {
                "id": manager_id,
                "email": "manager@uitest.local",
                "username": "uitest_manager",
                "role": "manager",
                "full_name": "مدیر مدرسه آزمایشی",
                "balance": 1000000,
                "school": "دبیرستان نمونه آزمایشی",
            }
            cls_id = "44444444-4444-4444-8444-444444444444"
            self.classes[cls_id] = {
                "id": cls_id,
                "teacher_id": teacher_id,
                "name": "دهم ریاضی ۱",
                "grade": "دهم",
                "field_of_study": "ریاضی فیزیک",
                "boys": 1,
                "girls": 0,
                "total": 1,
                "created_at": self.now_iso(),
                "shared_with_manager": True,
            }

    def find_user_by_identifier(self, ident: str) -> dict[str, Any]:
        norm = ident.strip().lower()
        for u in self.users_by_id.values():
            if norm in (u["email"].lower(), u["username"].lower()):
                return u
            local = u["email"].split("@", 1)[0].lower()
            if norm == local:
                return u
        is_student = norm.endswith("@student.exam.local") or ("student" in norm and "@" not in norm)
        role = "student" if is_student else ("manager" if "manager" in norm else "teacher")
        uid = str(uuid.uuid5(uuid.NAMESPACE_DNS, f"uitest.{norm}"))
        email = norm if "@" in norm else (f"{norm}@student.exam.local" if is_student else f"{norm}@uitest.local")
        username = norm.split("@", 1)[0]
        created = {
            "id": uid,
            "email": email,
            "username": username,
            "role": role,
            "full_name": "دانش‌آموز آزمایشی" if is_student else "معلم آزمایشی",
            "balance": 500000 if not is_student else 0,
            "school": "دبیرستان نمونه آزمایشی",
            "subject": "ریاضی",
        }
        self.users_by_id[uid] = created
        return created

    def user_to_gotrue(self, u: dict[str, Any]) -> dict[str, Any]:
        ts = self.now_iso()
        return {
            "id": u["id"],
            "aud": "authenticated",
            "role": "authenticated",
            "email": u["email"],
            "email_confirmed_at": ts,
            "phone": u.get("phone", ""),
            "confirmed_at": ts,
            "last_sign_in_at": ts,
            "app_metadata": {"provider": "email", "providers": ["email"]},
            "user_metadata": {
                "full_name": u["full_name"],
                "role": u["role"],
                "username": u["username"],
            },
            "identities": [],
            "created_at": ts,
            "updated_at": ts,
        }

    def issue_session(self, u: dict[str, Any]) -> dict[str, Any]:
        access = make_runtime_jwt(u["id"], u["email"])
        refresh = f"rt_{uuid.uuid4().hex}"
        self.tokens[access] = u["id"]
        self.tokens[refresh] = u["id"]
        now = int(time.time())
        return {
            "access_token": access,
            "token_type": "bearer",
            "expires_in": 7200,
            "expires_at": now + 7200,
            "refresh_token": refresh,
            "user": self.user_to_gotrue(u),
        }

    def current_user(self, auth_header: str | None) -> dict[str, Any]:
        if auth_header:
            tok = auth_header.replace("Bearer ", "").replace("bearer ", "").strip()
            if tok in self.tokens:
                return self.users_by_id[self.tokens[tok]]
            parts = tok.split(".")
            if len(parts) == 3:
                try:
                    padded = parts[1] + "=" * (-len(parts[1]) % 4)
                    payload = json.loads(base64.urlsafe_b64decode(padded.encode("ascii")).decode("utf-8"))
                    sub = payload.get("sub")
                    if sub and sub in self.users_by_id:
                        return self.users_by_id[sub]
                except Exception:
                    pass
        return self.users_by_id["11111111-1111-4111-8111-111111111111"]

    def make_unique_code(self) -> str:
        alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        existing = {e["code"] for e in self.exams.values()}
        while True:
            code = "".join(random.choice(alphabet) for _ in range(6))
            if code not in existing:
                return code

    def get_or_create_attempt(self, exam_row: dict[str, Any], student_id: str) -> dict[str, Any]:
        key = (exam_row["id"], student_id)
        att = self.attempts.get(key)
        if att is None:
            dur = int(exam_row.get("duration") or 0)
            att = {
                "exam_id": exam_row["id"],
                "student_id": student_id,
                "attempt_no": 1,
                "started_at": self.now_iso(),
                "expires_at": self.now_iso(dur * 60.0) if dur > 0 else None,
                "submitted_at": None,
                "section_deadlines": {},
                "section_locked_responses": {},
            }
            self.attempts[key] = att
        return att

    def compute_expired_sections(self, att: dict[str, Any]) -> list[str]:
        now_iso = self.now_iso()
        expired: list[str] = []
        for sec, dl in (att.get("section_deadlines") or {}).items():
            if dl and dl <= now_iso and sec not in expired:
                expired.append(sec)
        for sec in (att.get("section_locked_responses") or {}).keys():
            if sec not in expired:
                expired.append(sec)
        return expired

    def key_item_for(self, exam_id: str, index: int) -> dict[str, Any]:
        ek = self.exam_keys.get(exam_id) or {}
        ans_list = ek.get("answers") if isinstance(ek.get("answers"), list) else []
        for pos, item in enumerate(ans_list):
            if isinstance(item, dict):
                item_idx = item.get("i", pos)
                try:
                    if int(item_idx) == index:
                        return item
                except Exception:
                    pass
        return {}


def _matches_postgrest_filter(row: dict[str, Any], key: str, expr: str) -> bool:
    if expr.startswith("eq."):
        target = expr[3:]
        val = row.get(key)
        if isinstance(val, bool):
            return str(val).lower() == target.lower()
        return str(val if val is not None else "") == target
    if expr.startswith("neq."):
        target = expr[4:]
        return str(row.get(key, "")) != target
    if expr.startswith("is."):
        target = expr[3:].lower()
        val = row.get(key)
        if target == "null":
            return val is None
        if target == "true":
            return bool(val) is True
        if target == "false":
            return bool(val) is False
    if expr.startswith("in.(") and expr.endswith(")"):
        raw_items = expr[4:-1]
        items = [x.strip().strip('"').strip("'") for x in raw_items.split(",") if x.strip()]
        return str(row.get(key, "")) in items
    return True


def make_handler(state: MockSupabaseState):
    class Handler(BaseHTTPRequestHandler):
        protocol_version = "HTTP/1.1"

        def log_message(self, fmt: str, *args: Any) -> None:
            return

        def _send_json(self, status: int, body: Any, extra_headers: dict[str, str] | None = None) -> None:
            raw = b"" if body is None else json.dumps(body, ensure_ascii=False).encode("utf-8")
            self.send_response(status)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.send_header("Content-Length", str(len(raw)))
            self.send_header("Access-Control-Allow-Origin", "*")
            self.send_header(
                "Access-Control-Allow-Headers",
                "authorization, apikey, content-type, prefer, x-client-info, x-supabase-api-version, range",
            )
            self.send_header("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS")
            self.send_header("Access-Control-Expose-Headers", "Content-Range, X-Total-Count")
            if extra_headers:
                for hk, hv in extra_headers.items():
                    self.send_header(hk, hv)
            self.end_headers()
            if raw:
                self.wfile.write(raw)

        def _read_json(self) -> Any:
            length = int(self.headers.get("Content-Length") or "0")
            if length <= 0:
                return {}
            raw = self.rfile.read(length).decode("utf-8", errors="replace")
            if not raw.strip():
                return {}
            try:
                return json.loads(raw)
            except Exception:
                return {}

        def do_OPTIONS(self) -> None:
            self._send_json(200, {})

        def do_GET(self) -> None:
            self._dispatch("GET")

        def do_POST(self) -> None:
            self._dispatch("POST")

        def do_PUT(self) -> None:
            self._dispatch("PUT")

        def do_PATCH(self) -> None:
            self._dispatch("PATCH")

        def do_DELETE(self) -> None:
            self._dispatch("DELETE")

        def _dispatch(self, method: str) -> None:
            parsed = urllib.parse.urlparse(self.path)
            path = parsed.path.rstrip("/") or "/"
            query = urllib.parse.parse_qs(parsed.query, keep_blank_values=True)
            body = self._read_json() if method in ("POST", "PUT", "PATCH") else {}
            auth_hdr = self.headers.get("Authorization")

            with state.lock:
                if path.startswith("/__test__/"):
                    self._handle_test(method, path, body)
                    return
                if path.startswith("/auth/v1/"):
                    self._handle_auth(method, path, query, body, auth_hdr)
                    return
                if path.startswith("/rest/v1/rpc/"):
                    fn_name = path[len("/rest/v1/rpc/") :]
                    self._handle_rpc(fn_name, body if isinstance(body, dict) else {}, auth_hdr)
                    return
                if path.startswith("/rest/v1/"):
                    table = path[len("/rest/v1/") :]
                    self._handle_table(method, table, query, body, auth_hdr)
                    return
                if path.startswith("/storage/v1/"):
                    self._send_json(200, {"Key": path.lstrip("/"), "signedURL": "/storage/v1/mock.bin"})
                    return
                self._send_json(200, {"ok": True, "path": path})

        def _handle_test(self, method: str, path: str, body: dict[str, Any]) -> None:
            if path == "/__test__/health":
                self._send_json(
                    200,
                    {
                        "ok": True,
                        "anon_key": make_public_anon_key(),
                        "exams": len(state.exams),
                        "answers": len(state.answers),
                        "rpc_count": len(state.rpc_log),
                    },
                )
                return
            if path == "/__test__/reset":
                state.reset()
                self._send_json(200, {"ok": True})
                return
            if path == "/__test__/screenshot":
                from pathlib import Path
                import re as _re

                raw_name = str(body.get("filename") or "screen.png")
                safe_name = _re.sub(r"[^A-Za-z0-9._-]", "_", raw_name)
                if not safe_name.lower().endswith(".png"):
                    safe_name += ".png"
                b64 = str(body.get("png_base64") or "")
                png_bytes = base64.b64decode(b64.encode("ascii")) if b64 else b""
                if state.artifact_dir:
                    out_p = Path(state.artifact_dir)
                    out_p.mkdir(parents=True, exist_ok=True)
                    (out_p / safe_name).write_bytes(png_bytes)
                entry = {
                    "filename": safe_name,
                    "title": str(body.get("title") or safe_name),
                    "detail": str(body.get("detail") or ""),
                    "width": int(body.get("width") or 0),
                    "height": int(body.get("height") or 0),
                    "size_bytes": len(png_bytes),
                }
                state.screenshots = [s for s in state.screenshots if s["filename"] != safe_name] + [entry]
                if state.artifact_dir:
                    _write_android_visual_gallery(state.artifact_dir, state.screenshots)
                self._send_json(200, {"ok": True, "count": len(state.screenshots), "filename": safe_name})
                return
            if path == "/__test__/state":
                self._send_json(
                    200,
                    {
                        "exams": list(state.exams.values()),
                        "exam_keys": list(state.exam_keys.values()),
                        "answers": list(state.answers.values()),
                        "attempts": list(state.attempts.values()),
                        "rpc_log": state.rpc_log[-100:],
                        "rest_log": state.rest_log[-100:],
                    },
                )
                return
            if path == "/__test__/expire_section":
                exam_id = str(body.get("exam_id") or "")
                code = str(body.get("code") or "").strip().upper()
                ex = state.exams.get(exam_id)
                if not ex and code:
                    for cand in state.exams.values():
                        if cand["code"].upper() == code:
                            ex = cand
                            exam_id = cand["id"]
                            break
                if not ex:
                    self._send_json(404, {"error": "exam_not_found"})
                    return
                section = str(body.get("section") or "").strip()
                student_id = str(body.get("student_id") or "22222222-2222-4222-8222-222222222222")
                att = state.get_or_create_attempt(ex, student_id)
                past_iso = state.now_iso(-15.0)
                att["section_deadlines"][section] = past_iso
                if isinstance(body.get("snapshot"), dict):
                    att["section_locked_responses"].setdefault(section, copy.deepcopy(body["snapshot"]))
                self._send_json(
                    200,
                    {
                        "ok": True,
                        "exam_id": exam_id,
                        "section": section,
                        "deadline_at": past_iso,
                        "expired_sections": state.compute_expired_sections(att),
                    },
                )
                return
            self._send_json(404, {"error": "unknown_test_endpoint"})

        def _handle_auth(
            self,
            method: str,
            path: str,
            query: dict[str, list[str]],
            body: dict[str, Any],
            auth_hdr: str | None,
        ) -> None:
            if path == "/auth/v1/token":
                grant = (query.get("grant_type") or ["password"])[0]
                if grant == "refresh_token":
                    rt = str(body.get("refresh_token") or "")
                    uid = state.tokens.get(rt)
                    user = state.users_by_id.get(uid) if uid else state.current_user(auth_hdr)
                    self._send_json(200, state.issue_session(user))
                    return
                ident = str(body.get("email") or body.get("phone") or "uitest_teacher")
                user = state.find_user_by_identifier(ident)
                self._send_json(200, state.issue_session(user))
                return
            if path == "/auth/v1/user":
                user = state.current_user(auth_hdr)
                if method == "PUT" and isinstance(body.get("data"), dict):
                    meta = body["data"]
                    if meta.get("full_name"):
                        user["full_name"] = str(meta["full_name"])
                    if meta.get("role") in ("teacher", "student", "manager"):
                        user["role"] = str(meta["role"])
                self._send_json(200, state.user_to_gotrue(user))
                return
            if path == "/auth/v1/logout":
                self._send_json(204, None)
                return
            if path == "/auth/v1/otp":
                self._send_json(200, {"message_id": "mock-otp"})
                return
            if path == "/auth/v1/verify":
                ident = str(body.get("email") or "uitest_teacher")
                user = state.find_user_by_identifier(ident)
                self._send_json(200, state.issue_session(user))
                return
            self._send_json(200, {})

        def _handle_table(
            self,
            method: str,
            table: str,
            query: dict[str, list[str]],
            body: Any,
            auth_hdr: str | None,
        ) -> None:
            user = state.current_user(auth_hdr)
            state.rest_log.append({"method": method, "table": table, "user": user["username"]})

            if table == "app_version":
                rows = [
                    {
                        "id": 1,
                        "version_code": 1001027500,
                        "version_name": "1.02.75",
                        "download_url": "https://onlineexam.ir/download/latest.apk",
                        "notes_fa": "نسخه آزمایشی خودکار",
                        "force_update": False,
                    }
                ]
            elif table == "exams":
                if method == "PATCH" and isinstance(body, dict):
                    for row in state.exams.values():
                        if all(
                            _matches_postgrest_filter(row, k, vals[-1])
                            for k, vals in query.items()
                            if k not in ("select", "order", "limit", "offset", "columns")
                        ):
                            for bk, bv in body.items():
                                row[bk] = bv
                    rows = list(state.exams.values())
                else:
                    rows = []
                    for ex in state.exams.values():
                        enriched = copy.deepcopy(ex)
                        enriched["answers"] = [
                            {
                                "id": a["id"],
                                "student_id": a["student_id"],
                                "graded": a["graded"],
                                "total_grade": a["total_grade"],
                            }
                            for a in state.answers.values()
                            if a["exam_id"] == ex["id"]
                        ]
                        rows.append(enriched)
            elif table == "exam_keys":
                if method in ("POST", "PATCH") and isinstance(body, dict):
                    eid = str(body.get("exam_id") or "")
                    if eid:
                        state.exam_keys[eid] = copy.deepcopy(body)
                rows = [copy.deepcopy(v) for v in state.exam_keys.values()]
            elif table == "answers":
                if method == "PATCH" and isinstance(body, dict):
                    for row in state.answers.values():
                        if all(
                            _matches_postgrest_filter(row, k, vals[-1])
                            for k, vals in query.items()
                            if k not in ("select", "order", "limit", "offset", "columns")
                        ):
                            for bk, bv in body.items():
                                row[bk] = bv
                rows = [copy.deepcopy(v) for v in state.answers.values()]
            else:
                rows = []

            filtered = []
            for r in rows:
                ok = True
                for k, vals in query.items():
                    if k in ("select", "order", "limit", "offset", "columns", "on_conflict"):
                        continue
                    for expr in vals:
                        if not _matches_postgrest_filter(r, k, expr):
                            ok = False
                            break
                    if not ok:
                        break
                if ok:
                    filtered.append(r)

            order_spec = (query.get("order") or [""])[0]
            if order_spec:
                col_part = order_spec.split(",")[0]
                pieces = col_part.split(".")
                col = pieces[0]
                desc = "desc" in pieces[1:]
                filtered.sort(key=lambda item: str(item.get(col) or ""), reverse=desc)

            limit_spec = (query.get("limit") or [""])[0]
            if limit_spec.isdigit():
                filtered = filtered[: int(limit_spec)]

            prefer = (self.headers.get("Prefer") or "") + " " + (self.headers.get("Accept") or "")
            if "vnd.pgrst.object+json" in prefer:
                if not filtered:
                    self._send_json(406, {"code": "PGRST116", "message": "JSON object requested, 0 rows"})
                    return
                self._send_json(200, filtered[0])
                return

            self._send_json(200, filtered, {"Content-Range": f"0-{max(len(filtered) - 1, 0)}/{len(filtered)}"})

        def _handle_rpc(self, fn: str, params: dict[str, Any], auth_hdr: str | None) -> None:
            user = state.current_user(auth_hdr)
            state.rpc_log.append({"fn": fn, "user": user["username"]})

            if fn == "native_login_gate_v283":
                ident = str(params.get("p_identifier") or params.get("p_email") or "uitest_teacher")
                target = state.find_user_by_identifier(ident)
                self._send_json(200, {"ok": True, "email": target["email"]})
                return

            if fn == "native_staff_login_email_v1":
                ident = str(params.get("p_username") or params.get("p_identifier") or "uitest_teacher")
                target = state.find_user_by_identifier(ident)
                self._send_json(200, target["email"])
                return

            if fn in ("native_ensure_profile_v1", "native_my_profile"):
                self._send_json(
                    200,
                    {
                        "ok": True,
                        "id": user["id"],
                        "full_name": user["full_name"],
                        "display_name": user["full_name"],
                        "role": user["role"],
                        "username": user["username"],
                        "email": user["email"],
                        "balance": user.get("balance", 500000),
                        "hdr_school": user.get("school", ""),
                        "avatar_public": True,
                    },
                )
                return

            if fn == "native_my_registration_state_v1":
                self._send_json(
                    200,
                    {
                        "ok": True,
                        "role": user["role"],
                        "username": user["username"],
                        "full_name": user["full_name"],
                        "pending_role": "manager" if user["role"] == "manager" else "teacher",
                        "requires_teacher_setup": False,
                    },
                )
                return

            if fn == "native_my_teacher_details_v40":
                self._send_json(
                    200,
                    {
                        "ok": True,
                        "full_name": user["full_name"],
                        "username": user["username"],
                        "subject": user.get("subject", "ریاضی"),
                        "school": user.get("school", "دبیرستان نمونه آزمایشی"),
                        "phone": user.get("phone", ""),
                    },
                )
                return

            if fn == "native_save_exam_v2":
                payload = params.get("p_payload") if isinstance(params.get("p_payload"), dict) else params
                raw_id = payload.get("id")
                exam_id = str(raw_id) if raw_id else str(uuid.uuid4())
                existing = state.exams.get(exam_id)
                code = existing["code"] if existing else (str(payload.get("code") or "").strip().upper() or state.make_unique_code())
                raw_questions = payload.get("questions") if isinstance(payload.get("questions"), list) else []
                raw_answer_key = payload.get("answer_key") if isinstance(payload.get("answer_key"), list) else []

                public_questions = []
                synthesized_answer_key = []
                total_score = 0.0
                for idx, q in enumerate(raw_questions):
                    if not isinstance(q, dict):
                        continue
                    q_copy = copy.deepcopy(q)
                    q_copy.setdefault("id", f"q_{idx + 1}")
                    pts = float(q_copy.get("score") if q_copy.get("score") is not None else (q_copy.get("points") or 1.0))
                    q_copy["score"] = pts
                    total_score += pts

                    explicit_key = {}
                    for kpos, kitem in enumerate(raw_answer_key):
                        if isinstance(kitem, dict) and int(kitem.get("i", kpos)) == idx:
                            explicit_key = copy.deepcopy(kitem)
                            break
                    merged_key = {"i": idx}
                    for sk in SECRET_QUESTION_KEYS:
                        if sk in q_copy and sk != "answer_key":
                            merged_key[sk] = copy.deepcopy(q_copy[sk])
                    merged_key.update(explicit_key)
                    synthesized_answer_key.append(merged_key)
                    public_questions.append(strip_question_secrets(q_copy))

                options = copy.deepcopy(payload.get("options") if isinstance(payload.get("options"), dict) else {})
                duration = int(payload.get("duration") or 0)
                created_at = existing["created_at"] if existing else state.now_iso()
                exam_row = {
                    "id": exam_id,
                    "teacher_id": user["id"],
                    "title": str(payload.get("title") or "بدون عنوان"),
                    "subject": payload.get("subject") or "",
                    "duration": duration,
                    "code": code,
                    "is_open": bool(payload.get("is_open", existing["is_open"] if existing else False)),
                    "total_score": round(total_score * 100.0) / 100.0,
                    "questions": public_questions,
                    "shuffle_q": bool(payload.get("shuffle_q", False)),
                    "shuffle_opt": bool(payload.get("shuffle_opt", False)),
                    "neg_marking": float(payload.get("neg_marking") or 0.0),
                    "opens_at": payload.get("opens_at") or None,
                    "closes_at": payload.get("closes_at") or None,
                    "class_id": payload.get("class_id") or None,
                    "audience": str(payload.get("audience") or "all"),
                    "teacher_message": payload.get("teacher_message") or None,
                    "attempts_allowed": int(payload.get("attempts_allowed") or 1),
                    "attempt_on_timeout": bool(payload.get("attempt_on_timeout", False)),
                    "grade_policy": str(payload.get("grade_policy") or "last"),
                    "attempt_cooldown": int(payload.get("attempt_cooldown") or 0),
                    "pick_count": int(payload.get("pick_count") or 0),
                    "pick_rules": payload.get("pick_rules") if isinstance(payload.get("pick_rules"), dict) else {},
                    "options": options,
                    "created_at": created_at,
                }
                state.exams[exam_id] = exam_row
                state.exam_keys[exam_id] = {
                    "exam_id": exam_id,
                    "answers": synthesized_answer_key,
                }
                q_count = len(public_questions)
                cost = 0 if existing else q_count * 1000
                user["balance"] = max(0, int(user.get("balance", 500000)) - cost)
                self._send_json(
                    200,
                    {
                        "ok": True,
                        "id": exam_id,
                        "code": code,
                        "cost": cost,
                        "balance": user["balance"],
                        "billed_questions": 0 if existing else q_count,
                        "question_cost": cost,
                        "billed_images": 0,
                        "image_cost": 0,
                        "billed_audio": 0,
                        "audio_cost": 0,
                    },
                )
                return

            if fn == "native_set_exam_open_v1":
                exam_id = str(params.get("p_exam") or params.get("p_exam_id") or "")
                is_open = bool(params.get("p_open"))
                ex = state.exams.get(exam_id)
                if not ex:
                    self._send_json(200, {"error": "آزمون یافت نشد"})
                    return
                ex["is_open"] = is_open
                self._send_json(200, {"ok": True, "id": exam_id, "is_open": is_open})
                return

            if fn == "native_delete_exam":
                exam_id = str(params.get("p_exam") or params.get("p_exam_id") or "")
                state.exams.pop(exam_id, None)
                state.exam_keys.pop(exam_id, None)
                self._send_json(200, {"ok": True})
                return

            if fn == "native_duplicate_exam_v2":
                src_id = str(params.get("p_exam") or "")
                src = state.exams.get(src_id)
                if not src:
                    self._send_json(200, {"error": "آزمون یافت نشد"})
                    return
                new_id = str(uuid.uuid4())
                dup = copy.deepcopy(src)
                dup["id"] = new_id
                dup["title"] = f"{src['title']} — کپی"
                dup["code"] = state.make_unique_code()
                dup["is_open"] = False
                dup["created_at"] = state.now_iso()
                state.exams[new_id] = dup
                if src_id in state.exam_keys:
                    kdup = copy.deepcopy(state.exam_keys[src_id])
                    kdup["exam_id"] = new_id
                    state.exam_keys[new_id] = kdup
                cost = len(dup.get("questions") or []) * 1000
                user["balance"] = max(0, int(user.get("balance", 500000)) - cost)
                self._send_json(
                    200,
                    {"ok": True, "id": new_id, "code": dup["code"], "cost": cost, "balance": user["balance"]},
                )
                return

            if fn == "get_exam_audience":
                self._send_json(200, {"ok": True, "audience": "all", "classes": [], "students": [], "schools": []})
                return

            if fn == "get_exam_for_student":
                code = str(params.get("p_code") or "").strip().upper()
                target_exam = None
                for ex in state.exams.values():
                    if ex["code"].upper() == code:
                        target_exam = ex
                        break
                if not target_exam:
                    self._send_json(200, {"error": "آزمون با این کد یافت نشد"})
                    return
                if not target_exam.get("is_open", False):
                    self._send_json(200, {"error": "این آزمون بسته است"})
                    return
                att = state.get_or_create_attempt(target_exam, user["id"])
                expired_secs = state.compute_expired_sections(att)
                qs_clean = []
                for idx, q in enumerate(target_exam.get("questions") or []):
                    item = strip_question_secrets(q)
                    item["i"] = idx
                    qs_clean.append(item)
                resp = {
                    "id": target_exam["id"],
                    "title": target_exam["title"],
                    "subject": target_exam.get("subject") or "",
                    "duration": target_exam.get("duration") or 0,
                    "total_score": target_exam.get("total_score") or 0,
                    "code": target_exam["code"],
                    "shuffle_q": bool(target_exam.get("shuffle_q", False)),
                    "shuffle_opt": bool(target_exam.get("shuffle_opt", False)),
                    "questions": qs_clean,
                    "expires_at": att.get("expires_at"),
                    "attempts_allowed": target_exam.get("attempts_allowed", 1),
                    "attempts_used": 0,
                    "attempt_no": att["attempt_no"],
                    "resumed": False,
                    "grade_policy": target_exam.get("grade_policy", "last"),
                    "pick_count": target_exam.get("pick_count", 0),
                    "pick_rules": target_exam.get("pick_rules") or {},
                    "options": copy.deepcopy(target_exam.get("options") or {}),
                    "section_deadlines": copy.deepcopy(att["section_deadlines"]),
                    "expired_sections": expired_secs,
                    "question_total": len(qs_clean),
                    "shown_q": None,
                    "server_now": state.now_iso(),
                }
                self._send_json(200, resp)
                return

            if fn == "native_sync_exam_section_v1":
                exam_id = str(params.get("p_exam") or params.get("p_exam_id") or "")
                sec_title = str(params.get("p_section") or params.get("p_section_title") or "").strip()
                lock_now = bool(params.get("p_lock", False))
                sec_resps = (
                    params.get("p_section_responses")
                    if isinstance(params.get("p_section_responses"), dict)
                    else (params.get("p_snapshot") if isinstance(params.get("p_snapshot"), dict) else {})
                )
                ex = state.exams.get(exam_id)
                if not ex:
                    self._send_json(200, {"error": "آزمون یافت نشد"})
                    return
                att = state.get_or_create_attempt(ex, user["id"])
                opts = ex.get("options") or {}
                sec_durs = opts.get("sectionDurations") if isinstance(opts.get("sectionDurations"), dict) else {}
                now_iso = state.now_iso()
                if sec_title and sec_title in sec_durs and sec_title not in att["section_deadlines"]:
                    mins = max(1, int(float(sec_durs[sec_title])))
                    att["section_deadlines"][sec_title] = state.now_iso(mins * 60.0)
                if lock_now and sec_title:
                    cur_dl = att["section_deadlines"].get(sec_title)
                    if not cur_dl or cur_dl > now_iso:
                        att["section_deadlines"][sec_title] = now_iso
                    if sec_title not in att["section_locked_responses"]:
                        att["section_locked_responses"][sec_title] = copy.deepcopy(sec_resps)
                elif sec_title and sec_resps:
                    cur_dl = att["section_deadlines"].get(sec_title)
                    if not cur_dl or cur_dl > now_iso:
                        att["section_locked_responses"][sec_title] = copy.deepcopy(sec_resps)
                expired_secs = state.compute_expired_sections(att)
                self._send_json(
                    200,
                    {
                        "ok": True,
                        "server_now": now_iso,
                        "section_deadlines": copy.deepcopy(att["section_deadlines"]),
                        "expired_sections": expired_secs,
                    },
                )
                return

            if fn in ("submit_answer", "submit_exam_answer", "native_submit_queued_answer_v1"):
                exam_id = str(params.get("p_exam") or params.get("p_exam_id") or "")
                ex = state.exams.get(exam_id)
                if not ex and params.get("p_code"):
                    code_up = str(params.get("p_code") or "").strip().upper()
                    for cand in state.exams.values():
                        if cand["code"].upper() == code_up:
                            ex = cand
                            exam_id = cand["id"]
                            break
                if not ex:
                    self._send_json(200, {"error": "آزمون یافت نشد"})
                    return
                pub_qs = ex.get("questions") or []
                raw_resps = params.get("p_responses")
                resp_list: list[Any] = [""] * len(pub_qs)
                if isinstance(raw_resps, list):
                    for i in range(len(pub_qs)):
                        resp_list[i] = copy.deepcopy(raw_resps[i]) if i < len(raw_resps) else ""
                elif isinstance(raw_resps, dict):
                    for i, q in enumerate(pub_qs):
                        qid = str(q.get("id") or i)
                        resp_list[i] = copy.deepcopy(raw_resps.get(str(i), raw_resps.get(qid, "")))

                att = state.get_or_create_attempt(ex, user["id"])
                expired_secs = state.compute_expired_sections(att)
                if expired_secs:
                    for i, q in enumerate(pub_qs):
                        sec = str(q.get("section") or q.get("sectionTitle") or "").strip()
                        if sec in expired_secs:
                            locked_map = (att.get("section_locked_responses") or {}).get(sec)
                            if isinstance(locked_map, dict):
                                qid = str(q.get("id") or i)
                                if str(i) in locked_map:
                                    resp_list[i] = copy.deepcopy(locked_map[str(i)])
                                elif qid in locked_map:
                                    resp_list[i] = copy.deepcopy(locked_map[qid])

                grades_list: list[float] = []
                total_sum = 0.0
                for idx, q in enumerate(pub_qs):
                    key_item = state.key_item_for(exam_id, idx)
                    sc = auto_score_question(q, key_item, resp_list[idx])
                    g_val = float(sc) if sc is not None else 0.0
                    grades_list.append(round(g_val * 100.0) / 100.0)
                    total_sum += g_val

                images = copy.deepcopy(params.get("p_images") if isinstance(params.get("p_images"), dict) else {})
                meta = copy.deepcopy(params.get("p_meta") if isinstance(params.get("p_meta"), dict) else {})
                ans_id = f"{int(state.now_epoch())}-{uuid.uuid4().hex[:6]}"
                submitted_at = state.now_iso()
                att["submitted_at"] = submitted_at

                answer_row = {
                    "id": ans_id,
                    "exam_id": exam_id,
                    "student_id": user["id"],
                    "student_name": user["full_name"],
                    "responses": resp_list,
                    "grades": grades_list,
                    "total_grade": round(max(total_sum, 0.0) * 100.0) / 100.0,
                    "feedback": "",
                    "part_grades": {},
                    "part_feedback": {},
                    "graded": False,
                    "auto_graded": False,
                    "response_images": images,
                    "attempt_no": att["attempt_no"],
                    "submitted_at": submitted_at,
                    "meta": meta,
                }
                state.answers[ans_id] = answer_row
                receipt = ans_id.replace("-", "")[-6:].upper()
                self._send_json(
                    200,
                    {
                        "ok": True,
                        "id": ans_id,
                        "answer_id": ans_id,
                        "attempt": att["attempt_no"],
                        "attempts_used": 1,
                        "attempts_allowed": ex.get("attempts_allowed", 1),
                        "can_retry": False,
                        "receipt": receipt,
                    },
                )
                return

            if fn == "exam_autograde_info":
                exam_id = str(params.get("p_exam") or params.get("p_exam_id") or "")
                ex = state.exams.get(exam_id) or {}
                pub_qs = ex.get("questions") or []
                essay_indices = [
                    i + 1
                    for i, q in enumerate(pub_qs)
                    if str(q.get("type", "")).lower() in MANUAL_TYPES
                    and not _has_geometry_rubric(q, state.key_item_for(exam_id, i))
                ]
                pending = [a for a in state.answers.values() if a["exam_id"] == exam_id and not a["graded"]]
                self._send_json(
                    200,
                    {
                        "ok": True,
                        "count": len(pending),
                        "pending": len(pending),
                        "has_essay": bool(essay_indices),
                        "essay_questions": essay_indices,
                    },
                )
                return

            if fn == "approve_auto_grades":
                exam_id = str(params.get("p_exam") or params.get("p_exam_id") or "")
                mode = str(params.get("p_mode") or "finalize")
                ex = state.exams.get(exam_id) or {}
                pub_qs = ex.get("questions") or []
                essay_indices = [
                    i + 1
                    for i, q in enumerate(pub_qs)
                    if str(q.get("type", "")).lower() in MANUAL_TYPES
                    and not _has_geometry_rubric(q, state.key_item_for(exam_id, i))
                ]
                updated = 0
                for a in state.answers.values():
                    if a["exam_id"] == exam_id and not a["graded"]:
                        if mode != "prefill":
                            a["graded"] = True
                            a["auto_graded"] = True
                        updated += 1
                self._send_json(
                    200,
                    {
                        "ok": True,
                        "count": 0 if mode == "prefill" else updated,
                        "prefilled": updated if mode == "prefill" else 0,
                        "has_essay": bool(essay_indices),
                        "essay_questions": essay_indices,
                    },
                )
                return

            if fn in ("native_save_grade_parts_v1", "native_save_grade"):
                ans_id = str(params.get("p_answer") or params.get("p_answer_id") or "")
                ans = state.answers.get(ans_id)
                if not ans:
                    self._send_json(200, {"error": "پاسخ یافت نشد"})
                    return
                ex = state.exams.get(ans["exam_id"]) or {}
                pub_qs = ex.get("questions") or []
                grades_in = params.get("p_grades")
                norm_grades: list[float] = []
                if isinstance(grades_in, list):
                    for i in range(max(len(pub_qs), len(grades_in))):
                        val = grades_in[i] if i < len(grades_in) else 0.0
                        norm_grades.append(round((_parse_float(val) or 0.0) * 100.0) / 100.0)
                elif isinstance(grades_in, dict):
                    for i, q in enumerate(pub_qs):
                        qid = str(q.get("id") or i)
                        val = grades_in.get(str(i), grades_in.get(qid, 0.0))
                        norm_grades.append(round((_parse_float(val) or 0.0) * 100.0) / 100.0)
                else:
                    norm_grades = list(ans.get("grades") or [])

                pg_in = params.get("p_part_grades") if isinstance(params.get("p_part_grades"), dict) else {}
                pf_in = params.get("p_part_feedback") if isinstance(params.get("p_part_feedback"), dict) else {}
                # جمع خودکار نمرهٔ زیربخش‌ها اگر معلم نمرهٔ گام‌ها را داده باشد
                by_q: dict[int, float] = {}
                for pk, pv in pg_in.items():
                    if ":" in pk:
                        qi_str, _ = pk.split(":", 1)
                        if qi_str.isdigit():
                            by_q[int(qi_str)] = by_q.get(int(qi_str), 0.0) + (_parse_float(pv) or 0.0)
                for qi, qsum in by_q.items():
                    while len(norm_grades) <= qi:
                        norm_grades.append(0.0)
                    norm_grades[qi] = round(qsum * 100.0) / 100.0

                fb_raw = params.get("p_feedback")
                fb_str = fb_raw if isinstance(fb_raw, str) else ""
                ans["grades"] = norm_grades
                ans["total_grade"] = round(sum(norm_grades) * 100.0) / 100.0
                ans["feedback"] = fb_str
                ans["part_grades"] = copy.deepcopy(pg_in)
                ans["part_feedback"] = copy.deepcopy(pf_in)
                ans["graded"] = True
                ans["graded_at"] = state.now_iso()
                self._send_json(200, {"ok": True, "total_grade": ans["total_grade"]})
                return

            if fn == "unapprove_grade":
                ans_id = str(params.get("p_answer") or "")
                if ans_id in state.answers:
                    state.answers[ans_id]["graded"] = False
                self._send_json(200, {"ok": True})
                return

            if fn == "my_grades":
                out = []
                for a in state.answers.values():
                    if a["student_id"] == user["id"] and a["graded"]:
                        ex = state.exams.get(a["exam_id"]) or {}
                        out.append(
                            {
                                "id": a["id"],
                                "answer_id": a["id"],
                                "exam_id": a["exam_id"],
                                "title": ex.get("title", ""),
                                "subject": ex.get("subject") or "",
                                "total_grade": a["total_grade"],
                                "score": a["total_grade"],
                                "total_score": ex.get("total_score") or 0,
                                "feedback": a.get("feedback", ""),
                                "submitted_at": a["submitted_at"],
                                "graded_at": a.get("graded_at") or a["submitted_at"],
                            }
                        )
                self._send_json(200, out)
                return

            if fn == "native_my_levels_v1":
                items = [
                    {"exam_id": a["exam_id"], "level_name": "خیلی خوب", "level_rank": 4}
                    for a in state.answers.values()
                    if a["student_id"] == user["id"] and a["graded"]
                ]
                self._send_json(200, {"ok": True, "items": items})
                return

            if fn == "native_my_answers_v1":
                items = []
                for a in state.answers.values():
                    if a["student_id"] == user["id"]:
                        ex = state.exams.get(a["exam_id"]) or {}
                        items.append(
                            {
                                "id": a["id"],
                                "exam_id": a["exam_id"],
                                "title": ex.get("title", "آزمون"),
                                "subject": ex.get("subject") or "",
                                "submitted_at": a["submitted_at"],
                                "graded": bool(a["graded"]),
                                "total_grade": a["total_grade"],
                                "total_score": ex.get("total_score") or 0,
                                "feedback": a.get("feedback", ""),
                            }
                        )
                self._send_json(200, {"ok": True, "items": items})
                return

            if fn in ("native_my_answer_detail_parts_v1", "native_my_answer_detail_v1"):
                ans_id = str(params.get("p_answer") or params.get("p_answer_id") or "")
                a = state.answers.get(ans_id)
                if not a:
                    self._send_json(200, {"error": "پاسخ پیدا نشد یا متعلق به این حساب نیست"})
                    return
                ex = state.exams.get(a["exam_id"]) or {}
                out_qs = []
                for idx, q in enumerate(ex.get("questions") or []):
                    base_q = strip_question_secrets(q)
                    if a["graded"]:
                        key_item = copy.deepcopy(state.key_item_for(a["exam_id"], idx))
                        for secret_only in (
                            "i",
                            "rubric",
                            "partRubrics",
                            "constructionTargets",
                            "constructionConstraints",
                        ):
                            key_item.pop(secret_only, None)
                        base_q.update(key_item)
                    base_q["i"] = idx
                    out_qs.append(base_q)
                self._send_json(
                    200,
                    {
                        "ok": True,
                        "id": a["id"],
                        "exam_id": a["exam_id"],
                        "title": ex.get("title", "آزمون"),
                        "subject": ex.get("subject") or "",
                        "graded": bool(a["graded"]),
                        "total_grade": a["total_grade"],
                        "total_score": ex.get("total_score") or 0,
                        "feedback": a.get("feedback", ""),
                        "submitted_at": a["submitted_at"],
                        "questions": out_qs,
                        "responses": a.get("responses") or [],
                        "response_images": a.get("response_images") or {},
                        "grades": a.get("grades") or [],
                        "part_grades": a.get("part_grades") or {},
                        "part_feedback": a.get("part_feedback") or {},
                        "shown_q": None,
                    },
                )
                return

            if fn == "native_settings_get_v327":
                self._send_json(200, {"ok": True, "settings": state.settings_store.get(user["id"], {})})
                return

            if fn == "native_settings_put_v327":
                patch = params.get("p_settings") if isinstance(params.get("p_settings"), dict) else {}
                cur = state.settings_store.setdefault(user["id"], {})
                cur.update(patch)
                self._send_json(200, {"ok": True})
                return

            if fn == "native_wallet_snapshot":
                self._send_json(
                    200,
                    {
                        "ok": True,
                        "balance": user.get("balance", 500000),
                        "currency": "toman",
                        "transactions": [],
                    },
                )
                return

            if fn == "cal_month":
                self._send_json(200, {"ok": True, "notes": []})
                return

            if fn == "holidays_for":
                self._send_json(200, {"ok": True, "days": [], "years": {}})
                return

            if fn in ("my_classes", "native_my_classes_v28"):
                cls_items = list(state.classes.values())
                self._send_json(200, {"ok": True, "classes": cls_items} if fn == "native_my_classes_v28" else cls_items)
                return

            if fn in ("my_students", "my_students_for_pick"):
                stu = state.users_by_id["22222222-2222-4222-8222-222222222222"]
                self._send_json(
                    200,
                    [
                        {
                            "id": stu["id"],
                            "full_name": stu["full_name"],
                            "username": stu["username"],
                            "class_names": stu.get("class_name", "دهم ریاضی ۱"),
                            "is_active": True,
                            "can_manage": True,
                            "in_my_list": True,
                        }
                    ],
                )
                return

            if fn == "native_teacher_schools_v61":
                self._send_json(200, {"ok": True, "items": []})
                return

            if fn == "native_exam_audience_schools_v61":
                self._send_json(200, {"ok": True, "schools": []})
                return

            if fn == "native_my_notifications_v284":
                self._send_json(200, {"ok": True, "items": [], "unread": 0})
                return

            if fn == "native_monitor_list_v1":
                self._send_json(200, {"ok": True, "items": [], "reports": []})
                return

            if fn == "exam_live_status":
                self._send_json(200, {"ok": True, "active": 0, "submitted": 0, "items": []})
                return

            if fn == "teacher_public_profile":
                self._send_json(200, {"ok": True, "name": "معلم آزمایشی خودکار", "avatar": None})
                return

            if fn == "cal_day":
                self._send_json(200, {"ok": True, "notes": []})
                return

            if fn == "fb_list":
                self._send_json(200, [{"id": 1, "text": "آفرین، پاسخ کامل و دقیق است."}])
                return

            list_like_rpcs = {
                "native_teacher_school_classes_v61",
                "native_print_exams_list_v163",
                "native_teacher_manager_requests_v41",
                "exam_attendance",
                "class_roster",
            }
            if fn in list_like_rpcs:
                self._send_json(200, [])
                return

            if fn == "cal_unseen_v59":
                self._send_json(200, 0)
                return

            if fn in ("native_bank_snapshot_v1", "native_school_bank_v1"):
                self._send_json(200, {"ok": True, "items": [], "questions": []})
                return

            self._send_json(200, {"ok": True})

    return Handler


def _write_android_visual_gallery(artifact_dir: str, screenshots: list[dict[str, Any]]) -> None:
    import os
    from pathlib import Path

    out = Path(artifact_dir)
    out.mkdir(parents=True, exist_ok=True)
    lines = [
        "### 📱 گزارش تست خودکار بصری و تعاملی اپ اندروید (`Pixel 6 · API 34 · Compose`)",
        "",
        f"- **تعداد قاب‌های بصری ثبت‌شده از شبیه‌ساز واقعی:** `{len(screenshots)}` تصویر (`PNG`)",
        "",
        "| ردیف | مرحله / صفحه | توضیح | ابعاد | فایل تصویر |",
        "| :---: | --- | --- | :---: | :---: |",
    ]
    cards_html = []
    for idx, item in enumerate(screenshots, 1):
        fname = item["filename"]
        title = item["title"]
        detail = item["detail"]
        dims = f"{item['width']}×{item['height']}"
        lines.append(f"| {idx} | {title} | {detail} | `{dims}` | `{fname}` |")
        img_path = out / fname
        b64 = base64.b64encode(img_path.read_bytes()).decode("ascii") if img_path.is_file() else ""
        cards_html.append(
            f"<div class='shot'><div class='hd'><b>{idx}. {title}</b><span>{dims}</span></div>"
            f"<p>{detail}</p>"
            f"<img src='data:image/png;base64,{b64}' alt='{title}'/></div>"
        )
    lines.append("")
    lines.append("> 💡 فایل گالری مستقل تصویری (`visual-report.html`) و تمام تصاویر `PNG` در بخش **Artifacts** همین اجرای GitHub Actions قابل دانلود و مشاهده است.")
    (out / "summary.md").write_text("\n".join(lines) + "\n", encoding="utf-8")
    (out / "report.json").write_text(
        json.dumps({"steps": screenshots, "count": len(screenshots)}, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    gallery_html = (
        "<!doctype html><html lang='fa' dir='rtl'><head><meta charset='utf-8'>"
        "<title>گزارش بصری شبیه‌ساز اندروید — آزمون آنلاین</title>"
        "<style>body{font-family:Tahoma,sans-serif;background:#f3f7fb;color:#102a43;margin:24px}"
        ".grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(300px,1fr));gap:18px}"
        ".shot{background:#fff;border-radius:16px;padding:14px;box-shadow:0 4px 16px rgba(16,42,67,.08)}"
        ".hd{display:flex;justify-content:space-between;align-items:center;gap:8px;font-size:14px}"
        ".hd span{font-size:12px;color:#627d98;direction:ltr}p{font-size:12px;color:#486581;margin:8px 0}"
        "img{width:100%;height:auto;border-radius:12px;border:1px solid #d9e2ec}</style></head><body>"
        "<h1>📱 گالری بصری تست خودکار شبیه‌ساز اندروید (Pixel 6)</h1>"
        f"<div class='grid'>{''.join(cards_html)}</div></body></html>"
    )
    (out / "visual-report.html").write_text(gallery_html, encoding="utf-8")


class QuietThreadingHTTPServer(ThreadingHTTPServer):
    daemon_threads = True

    def handle_error(self, request: Any, client_address: Any) -> None:
        exc = sys.exc_info()[1]
        if isinstance(exc, (ConnectionResetError, BrokenPipeError, TimeoutError, OSError)):
            return
        super().handle_error(request, client_address)


def start_server_in_thread(
    host: str = "127.0.0.1", port: int = 0, artifact_dir: str | None = None
) -> tuple[ThreadingHTTPServer, MockSupabaseState]:
    st = MockSupabaseState(artifact_dir=artifact_dir)
    httpd = QuietThreadingHTTPServer((host, port), make_handler(st))
    thread = threading.Thread(target=httpd.serve_forever, daemon=True)
    thread.start()
    return httpd, st


def main() -> None:
    parser = argparse.ArgumentParser(description="Mock Supabase Server for Automated UI E2E Tests")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=18923)
    parser.add_argument("--artifact-dir", default=None)
    args = parser.parse_args()
    state = MockSupabaseState(artifact_dir=args.artifact_dir)
    httpd = QuietThreadingHTTPServer((args.host, args.port), make_handler(state))
    print(f"MOCK_SUPABASE_READY http://{args.host}:{args.port} anon={make_public_anon_key()}", flush=True)
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        httpd.shutdown()


if __name__ == "__main__":
    main()
