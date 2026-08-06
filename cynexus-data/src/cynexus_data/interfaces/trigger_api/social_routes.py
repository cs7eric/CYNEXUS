from __future__ import annotations

import os

from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel

from cynexus_data.interfaces.trigger_api.auth import require_internal_token
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.sources.social.browser import SocialAccessError
from cynexus_data.sources.social.cdp_login import CdpLoginSessionManager
from cynexus_data.sources.social.login import SocialLoginSessionManager
from cynexus_data.sources.social.opencli_login import OpenCliLoginSessionManager


class SocialLoginStartRequest(BaseModel):
    platform: str
    profile_name: str = "default"


router = APIRouter()
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


@router.post("/internal/social/login-sessions", dependencies=[Depends(require_internal_token)])
def start_login_session(payload: SocialLoginStartRequest) -> dict:
    try:
        return _manager.start(payload.platform, payload.profile_name)
    except SocialAccessError as exc:
        raise HTTPException(status_code=409, detail={"code": exc.code, "message": str(exc)}) from exc
    except Exception as exc:
        raise HTTPException(status_code=502, detail={"code": "LOGIN_SESSION_START_FAILED", "message": str(exc)}) from exc


@router.get("/internal/social/login-sessions/{session_id}", dependencies=[Depends(require_internal_token)])
def get_login_session(session_id: str) -> dict:
    try:
        return _manager.get(session_id)
    except SocialAccessError as exc:
        raise HTTPException(status_code=404, detail={"code": exc.code, "message": str(exc)}) from exc


@router.post("/internal/social/login-sessions/{session_id}/complete", dependencies=[Depends(require_internal_token)])
def complete_login_session(session_id: str) -> dict:
    try:
        return _manager.complete(session_id)
    except SocialAccessError as exc:
        raise HTTPException(status_code=404, detail={"code": exc.code, "message": str(exc)}) from exc


@router.delete("/internal/social/login-sessions/{session_id}", dependencies=[Depends(require_internal_token)])
def close_login_session(session_id: str) -> dict:
    try:
        return _manager.close(session_id)
    except SocialAccessError as exc:
        raise HTTPException(status_code=404, detail={"code": exc.code, "message": str(exc)}) from exc
