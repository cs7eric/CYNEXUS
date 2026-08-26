from __future__ import annotations

import os
import time
from typing import Any
from urllib.parse import urlparse

import requests


class AliyunParaformerTranscriber:
    """DashScope Paraformer file transcription provider.

    Remote URLs are submitted directly to Alibaba Cloud so the executor does
    not have to download the media first. Local-file transcription remains
    intentionally unsupported by this provider; the existing local provider
    can continue to handle uploads.
    """

    def __init__(
        self,
        api_key: str | None = None,
        endpoint: str | None = None,
        model: str | None = None,
        poll_interval_seconds: float = 2.0,
        timeout_seconds: float = 1800.0,
        request_timeout_seconds: float = 30.0,
        max_retries: int = 3,
    ) -> None:
        self.api_key = (api_key or os.getenv("DASHSCOPE_API_KEY") or "").strip()
        if not self.api_key:
            raise RuntimeError("DASHSCOPE_API_KEY is required for Aliyun Paraformer transcription")
        self.endpoint = (endpoint or os.getenv("DASHSCOPE_ENDPOINT") or "https://dashscope.aliyuncs.com/api/v1").rstrip("/")
        self.model = (model or os.getenv("DASHSCOPE_MODEL") or "paraformer-v2").strip() or "paraformer-v2"
        self.poll_interval_seconds = max(float(poll_interval_seconds), 0.1)
        self.timeout_seconds = max(float(timeout_seconds), self.poll_interval_seconds)
        self.request_timeout_seconds = max(float(request_timeout_seconds), 1.0)
        self.max_retries = max(int(max_retries), 1)
        self._session = requests.Session()

    def transcribe(self, audio_path: str, language: str | None = None) -> str:
        raise RuntimeError(
            "Aliyun Paraformer accepts a publicly reachable media URL; "
            "use transcribe_url or keep the local Faster Whisper provider"
        )

    def transcribe_url(self, media_url: str, language: str | None = None) -> str:
        parsed = urlparse(media_url)
        if parsed.scheme not in {"http", "https"} or not parsed.netloc:
            raise ValueError("Aliyun Paraformer requires a publicly reachable http(s) media URL")

        payload: dict[str, Any] = {
            "model": self.model,
            "input": {"file_urls": [media_url]},
        }
        if language:
            payload["parameters"] = {"language_hints": [language]}

        response = self._request(
            "post",
            f"{self.endpoint}/services/audio/asr/transcription",
            headers={"X-DashScope-Async": "enable"},
            json=payload,
        )
        body = _json_body(response)
        output = body.get("output") if isinstance(body.get("output"), dict) else body
        task_id = str((output or {}).get("task_id") or body.get("task_id") or "").strip()
        if not task_id:
            return _extract_transcript(body)

        status = str((output or {}).get("task_status") or "").upper()
        if status not in {"SUCCEEDED", "SUCCESS", "COMPLETED"}:
            body = self._poll_task(task_id)
        return self._transcript_from_task(body)

    def _poll_task(self, task_id: str) -> dict[str, Any]:
        deadline = time.monotonic() + self.timeout_seconds
        while True:
            response = self._request("get", f"{self.endpoint}/tasks/{task_id}")
            body = _json_body(response)
            output = body.get("output") if isinstance(body.get("output"), dict) else body
            status = str((output or {}).get("task_status") or body.get("task_status") or "").upper()
            if status in {"SUCCEEDED", "SUCCESS", "COMPLETED"}:
                return body
            if status in {"FAILED", "CANCELED", "CANCELLED"}:
                message = _error_message(body) or f"task status: {status}"
                raise RuntimeError(f"Aliyun Paraformer transcription failed: {message}")
            if time.monotonic() >= deadline:
                raise TimeoutError(f"Aliyun Paraformer transcription timed out: {task_id}")
            time.sleep(self.poll_interval_seconds)

    def _transcript_from_task(self, body: dict[str, Any]) -> str:
        transcript = _extract_transcript(body)
        if transcript:
            return transcript
        result_urls = _extract_transcription_urls(body)
        parts = []
        for result_url in result_urls:
            result_response = self._request("get", result_url, authenticated=False)
            parts.append(_extract_transcript(_json_body(result_response)))
        transcript = "\n".join(part for part in parts if part).strip()
        if transcript:
            return transcript
        message = _error_message(body) or "the task returned no transcript"
        raise RuntimeError(f"Aliyun Paraformer returned no transcript: {message}")

    def _request(self, method: str, url: str, *, authenticated: bool = True, **kwargs: Any) -> requests.Response:
        headers = dict(kwargs.pop("headers", {}) or {})
        if authenticated:
            headers["Authorization"] = f"Bearer {self.api_key}"
            headers.setdefault("Content-Type", "application/json")
        last_error: Exception | None = None
        for attempt in range(self.max_retries):
            try:
                response = self._session.request(
                    method,
                    url,
                    headers=headers,
                    timeout=self.request_timeout_seconds,
                    **kwargs,
                )
                if response.status_code >= 400:
                    last_error = RuntimeError(_http_error(response))
                    if attempt + 1 >= self.max_retries:
                        break
                    time.sleep(min(2**attempt, 5))
                    continue
                return response
            except (requests.RequestException, ValueError) as exc:
                last_error = exc
                if attempt + 1 >= self.max_retries:
                    break
                time.sleep(min(2**attempt, 5))
        raise RuntimeError(f"Aliyun Paraformer request failed: {last_error}") from last_error


def _json_body(response: requests.Response) -> dict[str, Any]:
    try:
        body = response.json()
    except ValueError as exc:
        raise RuntimeError("Aliyun Paraformer returned invalid JSON") from exc
    if not isinstance(body, dict):
        raise RuntimeError("Aliyun Paraformer returned an unexpected response")
    return body


def _extract_transcription_urls(value: Any) -> list[str]:
    urls: list[str] = []
    if isinstance(value, dict):
        for key, item in value.items():
            if key == "transcription_url" and isinstance(item, str) and item:
                urls.append(item)
            else:
                urls.extend(_extract_transcription_urls(item))
    elif isinstance(value, list):
        for item in value:
            urls.extend(_extract_transcription_urls(item))
    return list(dict.fromkeys(urls))


def _extract_transcript(value: Any) -> str:
    if isinstance(value, dict):
        for key in ("text", "transcript"):
            item = value.get(key)
            if isinstance(item, str) and item.strip():
                return item.strip()
        for key in ("sentences", "transcripts", "results", "output", "result"):
            if key in value:
                text = _extract_transcript(value[key])
                if text:
                    return text
    elif isinstance(value, list):
        parts = [_extract_transcript(item) for item in value]
        return "\n".join(part for part in parts if part).strip()
    return ""


def _http_error(response: requests.Response) -> str:
    detail = ""
    try:
        body = response.json()
    except ValueError:
        detail = (response.text or "").strip()[:300]
    else:
        if isinstance(body, dict):
            detail = _error_message(body)
    suffix = f": {detail}" if detail else ""
    return f"{response.status_code} {response.reason} for url: {response.url}{suffix}"


def _error_message(body: dict[str, Any]) -> str:
    for key in ("message", "error_message", "code"):
        value = body.get(key)
        if isinstance(value, str) and value.strip():
            return value.strip()
    output = body.get("output")
    if isinstance(output, dict):
        return _error_message(output)
    return ""
