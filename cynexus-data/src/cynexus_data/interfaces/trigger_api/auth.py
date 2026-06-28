from __future__ import annotations

import os

from fastapi import Header, HTTPException


def require_internal_token(x_cynexus_internal_token: str | None = Header(default=None)) -> None:
    expected = os.getenv("CYNEXUS_DATA_INTERNAL_TOKEN")
    if not expected:
        return
    if x_cynexus_internal_token != expected:
        raise HTTPException(status_code=401, detail="invalid internal token")

