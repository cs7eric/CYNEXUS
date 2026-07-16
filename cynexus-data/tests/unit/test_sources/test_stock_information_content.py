from __future__ import annotations

from cynexus_data.sources.stock_information.content import extract_content
from cynexus_data.sources.stock_information import content


class FakeResponse:
    url = "https://finance.sina.com.cn/stock/s/2026-07-12/doc-example.shtml"
    encoding = "utf-8"
    headers = {"Content-Type": "text/html; charset=utf-8"}

    def raise_for_status(self) -> None:
        return None

    def iter_content(self, chunk_size: int):
        yield b"<html><head><title>Example</title></head><body><nav>nav</nav><article><p>First fact.</p><p>Second fact.</p></article></body></html>"


class FakeSession:
    def get(self, url: str, **kwargs):
        return FakeResponse()


class LegacyHeaderResponse(FakeResponse):
    encoding = "ISO-8859-1"

    def iter_content(self, chunk_size: int):
        yield "<html><head><title>\u5c0f\u5546\u54c1\u57ce</title></head><body><article>\u4e2d\u6587\u6b63\u6587</article></body></html>".encode("utf-8")


class LegacyHeaderSession:
    def get(self, url: str, **kwargs):
        return LegacyHeaderResponse()


def test_extract_inline_information_content_without_remote_fetch() -> None:
    result = extract_content(url=None, title="IRM", inline_text="Question\n\nAnswer")

    assert result.status == "ready"
    assert result.text == "Question\nAnswer"
    assert result.content_hash


def test_removes_postgres_incompatible_nul_characters() -> None:
    result = extract_content(url=None, title="Document", inline_text="Fact A\x00Fact B")

    assert result.status == "ready"
    assert result.text == "Fact AFact B"


def test_extract_html_content_from_approved_source(monkeypatch) -> None:
    monkeypatch.setattr(content.socket, "getaddrinfo", lambda *args, **kwargs: [(None, None, None, None, ("93.184.216.34", 443))])
    result = extract_content(
        url="https://finance.sina.com.cn/stock/s/2026-07-12/doc-example.shtml",
        session=FakeSession(),
    )

    assert result.status == "ready"
    assert result.title == "Example"
    assert "First fact." in result.text
    assert "nav" not in result.text


def test_rejects_unapproved_content_source() -> None:
    result = extract_content(url="https://example.com/article", title="Example")

    assert result.status == "unsupported"
    assert "not approved" in (result.message or "")


def test_prefers_valid_utf8_when_legacy_header_is_incorrect(monkeypatch) -> None:
    monkeypatch.setattr(content.socket, "getaddrinfo", lambda *args, **kwargs: [(None, None, None, None, ("93.184.216.34", 443))])
    result = extract_content(
        url="https://finance.sina.com.cn/stock/s/2026-07-12/doc-example.shtml",
        session=LegacyHeaderSession(),
    )

    assert result.title == "\u5c0f\u5546\u54c1\u57ce"
    assert result.text == "\u4e2d\u6587\u6b63\u6587"


def test_rejects_approved_hostname_resolving_to_private_address(monkeypatch) -> None:
    monkeypatch.setattr(content.socket, "getaddrinfo", lambda *args, **kwargs: [(None, None, None, None, ("127.0.0.1", 443))])

    result = extract_content(url="https://www.gov.cn/article", title="Policy")

    assert result.status == "unsupported"
