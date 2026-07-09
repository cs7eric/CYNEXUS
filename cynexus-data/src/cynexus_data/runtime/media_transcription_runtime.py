from __future__ import annotations

import json
import mimetypes
import os
import re
import time
import uuid
from dataclasses import dataclass
from pathlib import Path
from typing import Any
from urllib.parse import urlparse

import requests
from fastapi import UploadFile

from cynexus_data.config.settings import PROJECT_ROOT
from cynexus_data.domain.transcription import answer_transcript_question, build_transcript_summary
from cynexus_data.pipelines.transcription.media import media_kind, prepare_audio, probe_duration_seconds
from cynexus_data.pipelines.transcription.transcribers import load_transcriber
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.runtime.media_transcription_store import MediaTranscriptionTaskStore
from cynexus_data.storage.postgres.transcription import TranscriptRepository


DOWNLOAD_HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
        "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
    ),
    "Accept": "*/*",
    "Referer": "https://www.google.com/",
}


@dataclass(frozen=True)
class MediaTranscriptionConfig:
    work_root: Path
    upload_root: Path
    ffmpeg_path: str | None
    transcriber_path: str | None
    transcriber_params: dict[str, Any]
    language: str | None


class MediaTranscriptionRuntime:
    def __init__(self, context: PipelineContext | None = None, task_store: MediaTranscriptionTaskStore | None = None) -> None:
        self._context = context or PipelineContext.default()
        self._task_store = task_store or MediaTranscriptionTaskStore()

    def upload(self, file: UploadFile) -> dict[str, str]:
        safe_name = _sanitize_filename(file.filename or "upload.bin")
        task_id = str(uuid.uuid4())
        upload_dir = self._config().upload_root
        upload_dir.mkdir(parents=True, exist_ok=True)
        file_path = upload_dir / f"{task_id}_{safe_name}"
        with file_path.open("wb") as target:
            while True:
                chunk = file.file.read(8 * 1024 * 1024)
                if not chunk:
                    break
                target.write(chunk)
        file.file.close()
        task = self._task_store.create_task_record(task_id, safe_name)
        task_metadata = {
            "title": Path(safe_name).stem,
            "source_url": task.get("source_url"),
            "download_url": None,
            "categories": [],
            "tags": [],
        }
        self._task_store.update_task(task_id, status="queued", metadata=task_metadata)
        self._task_store.executor.submit(self._run_local_transcription, task_id, file_path, safe_name, task_metadata)
        return {"task_id": task_id}

    def remote_parse(self, payload: dict[str, Any]) -> dict[str, str]:
        source_url = str(payload.get("download_url") or "").strip()
        title = str(payload.get("title") or "").strip()
        source_page_url = str(payload.get("source_url") or "").strip()
        metadata = payload.get("metadata") if isinstance(payload.get("metadata"), dict) else {}
        if not source_url:
            raise ValueError("download_url is required")
        file_name = _build_download_filename(
            source_url=source_url,
            title=title or str(metadata.get("title") or "").strip(),
            content_type=None,
        )
        task_id = str(uuid.uuid4())
        self._task_store.create_task_record(task_id, file_name, source_page_url or source_url)
        task_metadata = {
            **metadata,
            "title": title or str(metadata.get("title") or Path(file_name).stem),
            "source_url": source_page_url or source_url,
            "download_url": source_url,
            "categories": [str(metadata.get("platform"))] if metadata.get("platform") else [],
            "tags": [str(metadata.get("noteType"))] if metadata.get("noteType") else [],
        }
        self._task_store.update_task(
            task_id,
            status="downloading",
            metadata=task_metadata,
            download_progress={
                "phase": "queued",
                "progress": 0,
                "downloaded_bytes": 0,
                "total_bytes": 0,
                "eta_seconds": None,
                "speed_bytes_per_sec": 0,
            },
            intake_progress={
                "phase": "pending",
                "progress": 0,
                "processed_bytes": 0,
                "total_bytes": 0,
                "eta_seconds": None,
            },
        )
        self._task_store.executor.submit(self._run_remote_download_and_transcription, task_id, source_url, title, source_page_url, task_metadata)
        return {"task_id": task_id, "file_name": file_name}

    def get_task(self, task_id: str) -> dict[str, Any] | None:
        task = self._task_store.get_task(task_id)
        if task is None:
            return None
        return {
            "task_id": task_id,
            "status": task.get("status"),
            "transcript": task.get("transcript", ""),
            "transcript_events": task.get("transcript_events", []),
            "polished": task.get("polished", ""),
            "summary": task.get("summary", ""),
            "metadata": task.get("metadata", {}),
            "file_name": task.get("file_name", ""),
            "error": task.get("error"),
            "download_progress": task.get("download_progress", {}),
            "intake_progress": task.get("intake_progress", {}),
        }

    def ask(self, task_id: str, question: str) -> dict[str, str]:
        task = self._task_store.get_task(task_id)
        if task is None:
            raise KeyError("task not found")
        if task.get("status") != "done":
            raise ValueError("task is not completed yet")
        answer = answer_transcript_question(
            question,
            transcript=str(task.get("polished") or task.get("transcript") or ""),
            summary=str(task.get("summary") or ""),
        )
        qa_items = list(task.get("qa_items") or [])
        qa_items.insert(
            0,
            {
                "id": f"qa-{int(time.time() * 1000)}",
                "question": question,
                "answer": answer,
                "created_at": time.strftime("%Y-%m-%dT%H:%M:%S"),
            },
        )
        self._task_store.update_task(task_id, qa_items=qa_items)
        return {"answer": answer}

    def export_markdown(self, task_id: str) -> tuple[str, bytes]:
        task = self._task_store.get_task(task_id)
        if task is None:
            raise KeyError("task not found")
        if task.get("status") != "done":
            raise ValueError("task is not completed yet")
        metadata = task.get("metadata") or {}
        title = str(metadata.get("title") or Path(str(task.get("file_name") or "")).stem or "transcription").strip() or "transcription"
        file_name = f"{_sanitize_filename(title)}.md"
        categories = metadata.get("categories") or []
        tags = metadata.get("tags") or []
        body = [
            "---",
            f"title: {title}",
            f"date: {time.strftime('%Y/%m/%d %H:%M:%S')}",
            f"categories: {','.join(str(item) for item in categories)}",
            f"tags: {','.join(str(item) for item in tags)}",
            "---",
            "",
            "## Transcript",
            str(task.get("transcript") or "").strip(),
        ]
        polished = str(task.get("polished") or "").strip()
        summary = str(task.get("summary") or "").strip()
        if polished:
            body.extend(["", "## Polished", polished])
        if summary:
            body.extend(["", "## Summary", summary])
        return file_name, "\n".join(body).encode("utf-8")

    def _run_remote_download_and_transcription(
        self,
        task_id: str,
        source_url: str,
        title: str,
        source_page_url: str,
        metadata: dict[str, Any],
    ) -> None:
        session = requests.Session()
        request_headers = {**DOWNLOAD_HEADERS, "Referer": source_page_url or DOWNLOAD_HEADERS["Referer"]}
        try:
            with session.get(source_url, stream=True, timeout=(10, 300), headers=request_headers) as response:
                response.raise_for_status()
                resolved_name = _build_download_filename(source_url, title or str(metadata.get("title") or ""), response.headers.get("Content-Type"))
                upload_dir = self._config().upload_root
                upload_dir.mkdir(parents=True, exist_ok=True)
                file_path = upload_dir / f"{task_id}_{resolved_name}"
                total_size = int(response.headers.get("Content-Length") or 0)
                downloaded = 0
                started_at = time.time()
                self._task_store.update_task(task_id, file_name=resolved_name)
                self._task_store.mutate_task(task_id, lambda task: task["metadata"].update({"title": title or Path(resolved_name).stem}))
                with file_path.open("wb") as target:
                    for chunk in response.iter_content(chunk_size=4 * 1024 * 1024):
                        if not chunk:
                            continue
                        target.write(chunk)
                        downloaded += len(chunk)
                        elapsed = max(time.time() - started_at, 0.001)
                        speed = downloaded / elapsed
                        eta_seconds = int((total_size - downloaded) / speed) if total_size and speed > 0 else None
                        progress = int(downloaded * 100 / total_size) if total_size else min(95, max(5, downloaded // (4 * 1024 * 1024)))
                        self._task_store.update_task(
                            task_id,
                            download_progress={
                                "phase": "running",
                                "progress": min(progress, 99 if total_size else progress),
                                "downloaded_bytes": downloaded,
                                "total_bytes": total_size,
                                "eta_seconds": eta_seconds,
                                "speed_bytes_per_sec": int(speed),
                            },
                        )
            self._task_store.update_task(
                task_id,
                status="transcribing",
                download_progress={
                    "phase": "done",
                    "progress": 100,
                    "downloaded_bytes": downloaded,
                    "total_bytes": total_size,
                    "eta_seconds": 0,
                    "speed_bytes_per_sec": 0,
                },
            )
            self._run_local_transcription(task_id, file_path, resolved_name, metadata)
        except requests.RequestException as exc:
            self._task_store.update_task(task_id, status="error", error=f"下载失败: {exc}")
        except Exception as exc:
            self._task_store.update_task(task_id, status="error", error=f"工作流启动失败: {exc}")
        finally:
            session.close()

    def _run_local_transcription(self, task_id: str, file_path: Path, file_name: str, metadata: dict[str, Any]) -> None:
        try:
            source_size = file_path.stat().st_size if file_path.exists() else 0
            self._task_store.update_task(
                task_id,
                status="transcribing",
                file_name=file_name,
                metadata=metadata,
                intake_progress={
                    "phase": "preparing",
                    "progress": 8,
                    "processed_bytes": 0,
                    "total_bytes": source_size,
                    "eta_seconds": None,
                },
            )
            kind = media_kind(file_path)
            config = self._config()
            audio_path = prepare_audio(file_path, media_type=kind, work_dir=config.work_root, ffmpeg_path=config.ffmpeg_path)
            prepared_size = audio_path.stat().st_size if audio_path.exists() else source_size
            self._task_store.update_task(
                task_id,
                intake_progress={
                    "phase": "ready",
                    "progress": 100,
                    "processed_bytes": prepared_size,
                    "total_bytes": prepared_size,
                    "eta_seconds": 0,
                },
            )
            transcript = self._transcribe_audio(task_id, audio_path, config)
            if not transcript:
                raise RuntimeError("转写结果为空")
            duration = probe_duration_seconds(audio_path, ffmpeg_path=config.ffmpeg_path)
            summary = build_transcript_summary(
                transcript,
                title=str(metadata.get("title") or Path(file_name).stem),
                platform=str(metadata.get("platform") or "") or None,
                duration=duration,
            )
            persistence_warning = self._persist_transcript(task_id, transcript, file_path, audio_path, duration, kind)
            next_metadata = dict(metadata)
            if duration and not next_metadata.get("duration"):
                next_metadata["duration"] = int(duration)
            if persistence_warning:
                next_metadata["persistence_warning"] = persistence_warning
            self._task_store.update_task(
                task_id,
                status="done",
                transcript=transcript,
                polished=transcript,
                summary=summary,
                metadata=next_metadata,
                error=None,
            )
        except Exception as exc:
            self._task_store.update_task(task_id, status="error", error=str(exc))

    def _transcribe_audio(self, task_id: str, audio_path: Path, config: MediaTranscriptionConfig) -> str:
        if not config.transcriber_path:
            raise RuntimeError("media transcription transcriber is not configured; set CYNEXUS_MEDIA_TRANSCRIPTION_TRANSCRIBER")
        transcriber = load_transcriber(config.transcriber_path, config.transcriber_params)
        latest_text = ""

        def on_chunk(*args: Any) -> None:
            nonlocal latest_text
            previous_text = latest_text
            if len(args) >= 2:
                latest_text = str(args[1] or "").strip()
            elif len(args) == 1:
                latest_text = str(args[0] or "").strip()
            else:
                return

            def append_transcript_event(task: dict[str, Any]) -> None:
                current = str(task.get("transcript") or "")
                text = latest_text
                if not text:
                    return
                if len(text) < len(current) and current.startswith(text):
                    return
                if text.startswith(current):
                    delta = text[len(current):]
                elif text.startswith(previous_text):
                    delta = text[len(previous_text):]
                else:
                    delta = text
                task["transcript"] = text
                if delta:
                    events = task.setdefault("transcript_events", [])
                    events.append({"seq": len(events) + 1, "delta": delta, "text": text})

            self._task_store.mutate_task(task_id, append_transcript_event)

        if hasattr(transcriber, "transcribe_streaming"):
            result = transcriber.transcribe_streaming(str(audio_path), chunk_duration=8, callback=on_chunk)
            transcript = str(result or latest_text or "").strip()
        elif hasattr(transcriber, "transcribe"):
            transcript = str(transcriber.transcribe(str(audio_path), language=config.language) or "").strip()
        elif callable(transcriber):
            transcript = str(transcriber(str(audio_path), language=config.language) or "").strip()
        else:
            raise TypeError(f"unsupported transcriber object from {config.transcriber_path}")
        if transcript and transcript != latest_text:
            def append_final_transcript_event(task: dict[str, Any]) -> None:
                current = str(task.get("transcript") or "")
                if len(transcript) < len(current) and current.startswith(transcript):
                    return
                delta = transcript[len(current):] if transcript.startswith(current) else transcript
                task["transcript"] = transcript
                if delta:
                    events = task.setdefault("transcript_events", [])
                    events.append({"seq": len(events) + 1, "delta": delta, "text": transcript})

            self._task_store.mutate_task(task_id, append_final_transcript_event)
        return transcript

    def _persist_transcript(
        self,
        task_id: str,
        transcript: str,
        file_path: Path,
        audio_path: Path,
        duration: float | None,
        kind: str,
    ) -> str | None:
        try:
            repo = TranscriptRepository(self._context.hooks.postgres_core.get_client())
            repo.create_table()
            repo.upsert(
                {
                    "transcript_id": task_id,
                    "source_uri": file_path.resolve().as_uri(),
                    "transcript": transcript,
                    "language": self._config().language,
                    "duration_seconds": duration,
                    "metadata": {
                        "media_kind": kind,
                        "audio_path": str(audio_path),
                        "source_path": str(file_path),
                        "task_id": task_id,
                    },
                }
            )
            return None
        except Exception as exc:
            return str(exc)

    def _config(self) -> MediaTranscriptionConfig:
        work_root = Path(os.getenv("CYNEXUS_MEDIA_TRANSCRIPTION_WORK_ROOT", str(PROJECT_ROOT / "var" / "media-transcription")))
        params_raw = os.getenv("CYNEXUS_MEDIA_TRANSCRIPTION_TRANSCRIBER_PARAMS", "").strip()
        try:
            params = json.loads(params_raw) if params_raw else {}
        except json.JSONDecodeError as exc:
            raise RuntimeError(f"invalid CYNEXUS_MEDIA_TRANSCRIPTION_TRANSCRIBER_PARAMS: {exc}") from exc
        if not isinstance(params, dict):
            raise RuntimeError("CYNEXUS_MEDIA_TRANSCRIPTION_TRANSCRIBER_PARAMS must decode to a JSON object")
        return MediaTranscriptionConfig(
            work_root=work_root,
            upload_root=work_root / "uploads",
            ffmpeg_path=os.getenv("CYNEXUS_MEDIA_TRANSCRIPTION_FFMPEG_PATH") or None,
            transcriber_path=os.getenv("CYNEXUS_MEDIA_TRANSCRIPTION_TRANSCRIBER") or None,
            transcriber_params=params,
            language=os.getenv("CYNEXUS_MEDIA_TRANSCRIPTION_LANGUAGE", "zh") or None,
        )


def _sanitize_filename(name: str) -> str:
    cleaned = re.sub(r'[<>:"/\\\\|?*\x00-\x1f]+', "_", str(name or "").strip())
    cleaned = cleaned.replace(" ", "_")
    return cleaned[:120] or "upload.bin"


def _guess_extension_from_url(url: str) -> str:
    suffix = Path(urlparse(url).path).suffix.lower()
    if suffix in {".mp4", ".mov", ".mkv", ".webm", ".avi", ".mp3", ".wav", ".m4a", ".aac", ".ogg", ".flac", ".aiff"}:
        return suffix
    return ".mp4"


def _build_download_filename(source_url: str, title: str | None, content_type: str | None) -> str:
    ext = _guess_extension_from_url(source_url)
    guessed_ext = mimetypes.guess_extension((content_type or "").split(";")[0].strip()) if content_type else None
    if guessed_ext in {".mp4", ".mov", ".mkv", ".webm", ".avi", ".mp3", ".wav", ".m4a", ".aac", ".ogg", ".flac", ".aiff"}:
        ext = guessed_ext
    parsed = Path(urlparse(source_url).path).stem or "downloaded-media"
    base = _sanitize_filename(title or parsed)
    return f"{base}{ext}"
