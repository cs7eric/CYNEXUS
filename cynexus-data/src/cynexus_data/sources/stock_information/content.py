from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timezone
from hashlib import sha256
from io import BytesIO
from typing import Any
from urllib.parse import urlparse

import requests
from bs4 import BeautifulSoup


ALLOWED_HOSTS = {
    "static.cninfo.com.cn",
    "finance.sina.com.cn",
    "vip.stock.finance.sina.com.cn",
    "data.eastmoney.com",
    "pdf.dfcfw.com",
}
MAX_BYTES = 12 * 1024 * 1024
MAX_TEXT_LENGTH = 320_000
MAX_PDF_PAGES = 120
REQUEST_HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/131 Safari/537.36",
    "Accept": "text/html,application/pdf;q=0.9,*/*;q=0.5",
}


@dataclass(frozen=True)
class ExtractedContent:
    status: str
    title: str | None
    text: str
    source_url: str | None
    content_type: str | None
    content_hash: str | None
    message: str | None
    extracted_at: str

    def as_dict(self) -> dict[str, Any]:
        return {
            "status": self.status,
            "title": self.title,
            "text": self.text,
            "source_url": self.source_url,
            "content_type": self.content_type,
            "content_hash": self.content_hash,
            "message": self.message,
            "extracted_at": self.extracted_at,
        }


def extract_content(*, url: str | None, title: str | None = None, inline_text: str | None = None, session: requests.Session | None = None) -> ExtractedContent:
    """Extract displayable text from an approved information source only."""
    now = datetime.now(timezone.utc).isoformat()
    if _normalize(inline_text):
        text = _truncate(_normalize_text(inline_text))
        return _ready(title, text, url, "text/plain", now)
    if not url:
        return _result("unsupported", title, "", None, None, None, "No readable source URL is available.", now)
    if not _allowed_url(url):
        return _result("unsupported", title, "", url, None, None, "Source host is not approved for content extraction.", now)

    try:
        response = (session or requests.Session()).get(url, headers=REQUEST_HEADERS, timeout=(4, 12), allow_redirects=True, stream=True)
        response.raise_for_status()
        if not _allowed_url(response.url):
            return _result("unsupported", title, "", response.url, None, None, "Redirect target is not an approved source host.", now)
        body = _read_limited(response)
        content_type = (response.headers.get("Content-Type") or "").split(";", 1)[0].lower()
        if _is_pdf(url, content_type, body):
            text = _extract_pdf(body)
            if not text:
                return _result("needs_ocr", title, "", response.url, content_type or "application/pdf", None, "No selectable PDF text was found.", now)
            return _ready(title, text, response.url, content_type or "application/pdf", now)
        text, detected_title = _extract_html(body, response.encoding)
        if not text:
            return _result("failed", title or detected_title, "", response.url, content_type or "text/html", None, "No readable article body was found.", now)
        return _ready(title or detected_title, text, response.url, content_type or "text/html", now)
    except Exception as exc:  # noqa: BLE001 - callers receive a persisted, retryable failure state.
        return _result("failed", title, "", url, None, None, f"Content extraction failed: {type(exc).__name__}: {exc}", now)


def _allowed_url(url: str) -> bool:
    parsed = urlparse(url)
    return parsed.scheme == "https" and (parsed.hostname or "").lower() in ALLOWED_HOSTS


def _read_limited(response: requests.Response) -> bytes:
    chunks: list[bytes] = []
    size = 0
    for chunk in response.iter_content(chunk_size=64 * 1024):
        if not chunk:
            continue
        size += len(chunk)
        if size > MAX_BYTES:
            raise ValueError(f"Source body exceeds {MAX_BYTES // (1024 * 1024)} MB limit")
        chunks.append(chunk)
    return b"".join(chunks)


def _is_pdf(url: str, content_type: str, body: bytes) -> bool:
    return content_type == "application/pdf" or url.lower().split("?", 1)[0].endswith(".pdf") or body.startswith(b"%PDF-")


def _extract_pdf(body: bytes) -> str:
    from pypdf import PdfReader

    reader = PdfReader(BytesIO(body))
    parts = []
    for page in reader.pages[:MAX_PDF_PAGES]:
        text = _normalize(page.extract_text() or "")
        if text:
            parts.append(text)
        if sum(len(item) for item in parts) >= MAX_TEXT_LENGTH:
            break
    return _truncate("\n\n".join(parts))


def _extract_html(body: bytes, encoding: str | None) -> tuple[str, str | None]:
    normalized_encoding = (encoding or "").strip().lower()
    if normalized_encoding in {"", "ascii", "iso-8859-1"}:
        try:
            decoded = body.decode("utf-8")
        except UnicodeDecodeError:
            decoded = body.decode("gb18030", errors="replace")
    else:
        decoded = body.decode(normalized_encoding, errors="replace")
    soup = BeautifulSoup(decoded, "html.parser")
    title = _normalize(soup.title.get_text(" ", strip=True) if soup.title else "") or None
    for node in soup(["script", "style", "noscript", "svg", "iframe", "nav", "footer", "header", "form"]):
        node.decompose()
    main = soup.find("article") or soup.find("main") or soup.find(id=lambda value: isinstance(value, str) and "content" in value.lower()) or soup.body
    text = _trim_boilerplate(_normalize_text(main.get_text("\n", strip=True) if main else ""))
    return _truncate(text), title


def _ready(title: str | None, text: str, url: str | None, content_type: str, extracted_at: str) -> ExtractedContent:
    return _result("ready", title, text, url, content_type, sha256(text.encode("utf-8")).hexdigest(), None, extracted_at)


def _result(status: str, title: str | None, text: str, url: str | None, content_type: str | None, content_hash: str | None, message: str | None, extracted_at: str) -> ExtractedContent:
    return ExtractedContent(status, _normalize(title) or None, text, url, content_type, content_hash, message, extracted_at)


def _normalize(value: str | None) -> str:
    return " ".join((value or "").replace("\u00a0", " ").split())


def _normalize_text(value: str | None) -> str:
    lines = [_normalize(line) for line in (value or "").replace("\r", "").split("\n")]
    return "\n".join(line for line in lines if line)


def _trim_boilerplate(value: str) -> str:
    for marker in ("\u6d77\u91cf\u8d44\u8baf\u3001\u7cbe\u51c6\u89e3\u8bfb\uff0c\u5c3d\u5728\u65b0\u6d6a\u8d22\u7ecfAPP", "VIP\u8bfe\u7a0b\u63a8\u8350", "\u65b0\u6d6a\u8d22\u7ecf\u610f\u89c1\u53cd\u9988\u7559\u8a00\u677f"):
        index = value.find(marker)
        if index >= 0:
            value = value[:index]
    return value.strip()


def _truncate(value: str) -> str:
    return value[:MAX_TEXT_LENGTH]
