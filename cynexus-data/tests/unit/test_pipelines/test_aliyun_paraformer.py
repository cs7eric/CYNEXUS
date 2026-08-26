from __future__ import annotations

from typing import Any

from cynexus_data.pipelines.transcription.providers.aliyun_paraformer import AliyunParaformerTranscriber


class FakeResponse:
    def __init__(self, body: dict[str, Any]) -> None:
        self.body = body
        self.status_code = 200
        self.reason = "OK"
        self.url = "https://example.com/api/v1"
        self.text = ""

    def json(self) -> dict[str, Any]:
        return self.body

    def raise_for_status(self) -> None:
        return None


class FakeSession:
    def __init__(self) -> None:
        self.calls: list[tuple[str, str, dict[str, Any]]] = []
        self.responses = [
            FakeResponse({"output": {"task_id": "task-1", "task_status": "RUNNING"}}),
            FakeResponse({"output": {"task_id": "task-1", "task_status": "SUCCEEDED", "results": [{"transcription_url": "https://result.example/transcript.json"}]}}),
            FakeResponse({"transcripts": [{"sentences": [{"text": "阿里转写结果"}]}]}),
        ]

    def request(self, method: str, url: str, **kwargs: Any) -> FakeResponse:
        self.calls.append((method, url, kwargs))
        return self.responses.pop(0)


def test_aliyun_paraformer_submits_url_polls_and_reads_result() -> None:
    provider = AliyunParaformerTranscriber(
        api_key="test-key",
        endpoint="https://example.com/api/v1",
        poll_interval_seconds=0.01,
        request_timeout_seconds=1,
    )
    session = FakeSession()
    provider._session = session

    transcript = provider.transcribe_url("https://media.example/demo.mp3", language="zh")

    assert transcript == "阿里转写结果"
    assert session.calls[0][0:2] == ("post", "https://example.com/api/v1/services/audio/asr/transcription")
    assert session.calls[0][2]["headers"]["Authorization"] == "Bearer test-key"
    assert session.calls[0][2]["json"] == {
        "model": "paraformer-v2",
        "input": {"file_urls": ["https://media.example/demo.mp3"]},
        "parameters": {"language_hints": ["zh"]},
    }
    assert session.calls[1][0:2] == ("get", "https://example.com/api/v1/tasks/task-1")
    assert session.calls[2][0:2] == ("get", "https://result.example/transcript.json")
    assert "Authorization" not in session.calls[2][2]["headers"]


class ForbiddenResponse:
    def __init__(self) -> None:
        self.status_code = 403
        self.reason = "Forbidden"
        self.url = "https://example.com/api/v1/services/audio/asr/transcription"
        self.text = '{"code":"AllocationQuota.FreeTierOnly","message":"Free quota exhausted"}'

    def json(self) -> dict[str, Any]:
        return {"code": "AllocationQuota.FreeTierOnly", "message": "Free quota exhausted"}


class ForbiddenSession:
    def request(self, method: str, url: str, **kwargs: Any) -> ForbiddenResponse:
        return ForbiddenResponse()


def test_aliyun_paraformer_includes_provider_error_body() -> None:
    provider = AliyunParaformerTranscriber(
        api_key="test-key",
        endpoint="https://example.com/api/v1",
        max_retries=1,
        request_timeout_seconds=1,
    )
    provider._session = ForbiddenSession()

    try:
        provider.transcribe_url("https://media.example/demo.mp3")
    except RuntimeError as exc:
        message = str(exc)
        assert "403 Forbidden" in message
        assert "Free quota exhausted" in message
    else:  # pragma: no cover - assertion branch
        raise AssertionError("provider error body should be raised")


def test_aliyun_paraformer_rejects_local_path() -> None:
    provider = AliyunParaformerTranscriber(api_key="test-key")

    try:
        provider.transcribe_url("C:/tmp/demo.mp3")
    except ValueError as exc:
        assert "http(s)" in str(exc)
    else:  # pragma: no cover - assertion branch
        raise AssertionError("local path should be rejected")
