from __future__ import annotations

from fastapi import APIRouter, Depends

from cynexus_data.interfaces.trigger_api.auth import require_internal_token
from cynexus_data.quality.data_source_health import run_data_source_health_checks


router = APIRouter(prefix="/internal/data-sources", dependencies=[Depends(require_internal_token)])


@router.get("/health")
def data_source_health(include_slow: bool = False) -> dict:
    return run_data_source_health_checks(include_slow=include_slow)
