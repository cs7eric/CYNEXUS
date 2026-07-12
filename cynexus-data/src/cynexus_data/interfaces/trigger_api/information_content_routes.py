from __future__ import annotations

from fastapi import APIRouter, Depends
from pydantic import BaseModel, Field

from cynexus_data.interfaces.trigger_api.auth import require_internal_token
from cynexus_data.sources.stock_information.content import extract_content


router = APIRouter(dependencies=[Depends(require_internal_token)])


class InformationContentRequest(BaseModel):
    url: str | None = None
    title: str | None = None
    inline_text: str | None = Field(default=None, max_length=320_000)


@router.post("/internal/stock-information/content")
def content(request: InformationContentRequest) -> dict:
    return extract_content(url=request.url, title=request.title, inline_text=request.inline_text).as_dict()
