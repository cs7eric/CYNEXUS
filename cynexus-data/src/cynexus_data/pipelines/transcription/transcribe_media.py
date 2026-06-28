from __future__ import annotations

import hashlib
from pathlib import Path
from typing import Any

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.pipelines.transcription.media import media_kind, prepare_audio, probe_duration_seconds
from cynexus_data.pipelines.transcription.transcribers import transcribe_audio
from cynexus_data.storage.postgres.transcription import TranscriptRepository


def run(context: PipelineContext, request: JobRequest) -> dict:
    input_uri = str(request.params.get("input_uri") or "").strip()
    if not input_uri:
        raise ValueError("params.input_uri is required")
    path = _path_from_uri(input_uri)
    kind = media_kind(path)
    transcript_id = str(request.params.get("transcript_id") or request.params.get("biz_id") or _stable_transcript_id(input_uri))
    language = request.params.get("language")
    force = _truthy(request.params.get("force"))
    create_table = _truthy(request.params.get("create_table"))
    work_dir = Path(str(request.params.get("work_dir") or "var/transcription"))
    metadata = {
        "pipeline": "transcription.transcribe_media",
        "dry_run": request.dry_run,
        "input_uri": input_uri,
        "transcript_id": transcript_id,
        "media_kind": kind,
        "exists": path.exists(),
        "rows_read": 0,
        "rows_written": 0,
    }
    if request.dry_run:
        metadata["transcript"] = ""
        metadata["notes"] = ["dry_run skipped media conversion, transcriber, and Postgres access"]
        return metadata
    if not path.exists():
        raise FileNotFoundError(str(path))
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")
    repo = TranscriptRepository(context.hooks.postgres_core.get_client())
    if create_table:
        repo.create_table()
    if not force:
        cached = repo.get(transcript_id)
        if cached is not None:
            return {**metadata, "rows_read": 1, "rows_written": 0, "from_cache": True, "transcript": cached.get("transcript") or ""}
    audio_path = prepare_audio(path, media_type=kind, work_dir=work_dir, ffmpeg_path=_optional_str(request.params.get("ffmpeg_path")))
    transcript = transcribe_audio(
        audio_path,
        language=_optional_str(language),
        transcript_text=_optional_str(request.params.get("transcript_text") or request.params.get("mock_transcript")),
        transcriber_path=_optional_str(request.params.get("transcriber")),
        transcriber_params=request.params.get("transcriber_params") if isinstance(request.params.get("transcriber_params"), dict) else None,
    ).strip()
    if not transcript:
        raise RuntimeError("transcription result is empty")
    duration = probe_duration_seconds(audio_path, ffmpeg_path=_optional_str(request.params.get("ffmpeg_path")))
    rows_written = repo.upsert(
        {
            "transcript_id": transcript_id,
            "source_uri": input_uri,
            "transcript": transcript,
            "language": _optional_str(language),
            "duration_seconds": duration,
            "metadata": {
                "media_kind": kind,
                "audio_path": str(audio_path),
                "source_path": str(path),
                "biz_id": request.params.get("biz_id"),
            },
        }
    )
    return {
        **metadata,
        "rows_read": 1,
        "rows_written": rows_written,
        "audio_path": str(audio_path),
        "duration_seconds": duration,
        "transcript": transcript,
    }


def _path_from_uri(input_uri: str) -> Path:
    if input_uri.startswith("file:///"):
        return Path(input_uri.removeprefix("file:///"))
    if input_uri.startswith("file://"):
        return Path(input_uri.removeprefix("file://"))
    return Path(input_uri)


def _stable_transcript_id(input_uri: str) -> str:
    return "transcript_" + hashlib.sha256(input_uri.encode("utf-8")).hexdigest()[:24]


def _optional_str(value: Any) -> str | None:
    if value in (None, ""):
        return None
    return str(value)


def _truthy(value: Any) -> bool:
    if isinstance(value, bool):
        return value
    if value is None:
        return False
    return str(value).lower() in {"1", "true", "yes", "y", "on"}
