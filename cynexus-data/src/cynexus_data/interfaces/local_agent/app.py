from __future__ import annotations

import os
import logging
from datetime import datetime, timezone
from typing import Any

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.jobs.registry import JOB_REGISTRY
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.runtime.job_runner import JobRunner
from cynexus_data.sources.social.browser import SocialAccessError
from cynexus_data.sources.social.cdp_login import CdpLoginSessionManager
from cynexus_data.sources.social.opencli_login import OpenCliLoginSessionManager
from cynexus_data.sources.social.login import SocialLoginSessionManager
from cynexus_data.storage.postgres.social import SocialRepository


class SocialLoginStartRequest(BaseModel):
    platform: str
    profile_name: str = "default"
    probe_url: str | None = None


class LocalJobTriggerRequest(BaseModel):
    trigger_request_id: str
    triggered_by: str = "social-monitoring-ui"
    dry_run: bool = False
    params: dict[str, Any] = {}


_context = PipelineContext.default(persist_job_state=True)
_backend = os.getenv("CYNEXUS_SOCIAL_BACKEND", "cdp").strip().lower()
if _backend == "opencli":
    _manager = OpenCliLoginSessionManager(
        lambda platform, profile_name: _context.hooks.social.get_client(platform, profile_name=profile_name)
    )
elif _backend == "playwright":
    _manager = SocialLoginSessionManager(
        lambda platform, profile_name: _context.hooks.social.get_client(platform, profile_name=profile_name)
    )
else:
    _manager = CdpLoginSessionManager(
        lambda platform, profile_name: _context.hooks.social.get_client(platform, profile_name=profile_name)
    )
_job_runner = JobRunner(JOB_REGISTRY, context=_context)
_LOCAL_SOCIAL_JOBS = {"social_creator_discovery", "social_comment_sync"}
log = logging.getLogger(__name__)

app = FastAPI(title="Cynexus Local Social Agent", docs_url=None, redoc_url=None)
app.add_middleware(
    CORSMiddleware,
    allow_origin_regex=r"https?://(localhost|127\.0\.0\.1)(:\d+)?$",
    allow_credentials=False,
    allow_methods=["GET", "POST", "DELETE"],
    allow_headers=["*"] ,
)


@app.on_event("startup")
def recover_interrupted_runs() -> None:
    """Release locks left by a previous local-agent process."""
    count = _context.job_store.mark_interrupted_running_jobs(
        "local social agent restarted",
        job_keys=_LOCAL_SOCIAL_JOBS,
    )
    if count:
        log.warning("Marked %s interrupted social job run(s) as expired and released their locks", count)


def _success(data: dict) -> dict:
    return {
        "code": "0",
        "data": data,
        "success": True,
        "failed": False,
        "message": "success",
        "timestamp": int(datetime.now(timezone.utc).timestamp() * 1000),
    }


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "UP", "agent": "social-local"}


@app.get("/local/social/login-status")
def get_login_status(platform: str, profile_name: str = "default") -> dict:
    normalized = str(platform or "").strip().lower()
    if normalized not in {"douyin", "xiaohongshu"}:
        raise HTTPException(status_code=400, detail={"code": "INVALID_PLATFORM", "message": "Unsupported social platform"})
    try:
        if _backend == "cdp":
            client = _context.hooks.social.get_client(normalized, profile_name=profile_name)
            return _success(client.probe_login_status())
        return _success(
            {
                "platform": normalized,
                "profile_name": profile_name,
                "status": "unknown",
                "message": "当前采集后端不支持无会话登录探测。",
            }
        )
    except SocialAccessError as exc:
        if exc.code == "CDP_BROWSER_NOT_CONNECTED":
            return _success(
                {
                    "platform": normalized,
                    "profile_name": profile_name,
                    "status": "blocked",
                    "message": str(exc),
                }
            )
        raise HTTPException(status_code=409, detail={"code": exc.code, "message": str(exc)}) from exc


@app.get("/local/social/posts")
def list_social_posts(platform: str, active_post_days: int = 30, limit: int = 100, today_only: bool = True) -> dict:
    normalized = str(platform or "").strip().lower()
    if normalized not in {"douyin", "xiaohongshu"}:
        raise HTTPException(status_code=400, detail={"code": "INVALID_PLATFORM", "message": "Unsupported social platform"})
    repository = SocialRepository(_context.hooks.postgres.get_client())
    posts = repository.list_active_posts(
        normalized,
        active_post_days=max(1, min(int(active_post_days), 365)),
        limit=max(1, min(int(limit), 200)),
        today_only=today_only,
    )
    return _success({"platform": normalized, "count": len(posts), "posts": posts})


@app.get("/local/social/posts/{post_key}/comments")
def list_social_post_comments(platform: str, post_key: str, limit: int | None = None) -> dict:
    normalized = str(platform or "").strip().lower()
    if normalized not in {"douyin", "xiaohongshu"}:
        raise HTTPException(status_code=400, detail={"code": "INVALID_PLATFORM", "message": "Unsupported social platform"})
    repository = SocialRepository(_context.hooks.postgres.get_client())
    post = repository.get_post(normalized, post_key)
    if post is None:
        raise HTTPException(status_code=404, detail={"code": "POST_NOT_FOUND", "message": "Social post not found"})
    comments = repository.list_comments(normalized, post_key, limit=limit)
    return _success({"post": post, "count": len(comments), "comments": comments})


@app.post("/local/social/login-sessions")
def start_login_session(payload: SocialLoginStartRequest) -> dict:
    try:
        if _backend == "opencli":
            return _success(_manager.start(payload.platform, payload.profile_name, payload.probe_url))
        if _backend == "playwright":
            return _success(_manager.start(payload.platform, payload.profile_name, headless_override=False))
        return _success(_manager.start(payload.platform, payload.profile_name))
    except SocialAccessError as exc:
        raise HTTPException(status_code=409, detail={"code": exc.code, "message": str(exc)}) from exc
    except Exception as exc:
        raise HTTPException(status_code=502, detail={"code": "LOGIN_SESSION_START_FAILED", "message": str(exc)}) from exc


@app.get("/local/social/login-sessions/{session_id}")
def get_login_session(session_id: str) -> dict:
    try:
        return _success(_manager.get(session_id))
    except SocialAccessError as exc:
        raise HTTPException(status_code=404, detail={"code": exc.code, "message": str(exc)}) from exc


@app.post("/local/social/login-sessions/{session_id}/complete")
def complete_login_session(session_id: str) -> dict:
    try:
        return _success(_manager.complete(session_id))
    except SocialAccessError as exc:
        raise HTTPException(status_code=404, detail={"code": exc.code, "message": str(exc)}) from exc


@app.delete("/local/social/login-sessions/{session_id}")
def close_login_session(session_id: str) -> dict:
    try:
        return _success(_manager.close(session_id))
    except SocialAccessError as exc:
        raise HTTPException(status_code=404, detail={"code": exc.code, "message": str(exc)}) from exc


@app.post("/local/jobs/{job_key}/trigger")
def trigger_local_job(job_key: str, payload: LocalJobTriggerRequest) -> dict:
    if job_key not in _LOCAL_SOCIAL_JOBS:
        raise HTTPException(status_code=404, detail={"code": "JOB_NOT_FOUND", "message": "Local agent only runs social jobs"})
    request = JobRequest(
        job_key=job_key,
        trigger_request_id=payload.trigger_request_id,
        triggered_by=payload.triggered_by,
        dry_run=payload.dry_run,
        params=payload.params,
    )
    result = _job_runner.run(request)
    return _success(
        {
            "accepted": result.status not in {"unknown_job", "locked"},
            "run_id": result.run_id,
            "job_key": result.job_key,
            "status": result.status,
            "result": result.to_dict(),
        }
    )
