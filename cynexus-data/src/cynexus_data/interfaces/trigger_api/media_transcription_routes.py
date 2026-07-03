from __future__ import annotations

import json
import time
from urllib.parse import quote

from fastapi import APIRouter, Depends, File, HTTPException, UploadFile
from fastapi.responses import Response, StreamingResponse
from pydantic import BaseModel, Field

from cynexus_data.interfaces.trigger_api.auth import require_internal_token
from cynexus_data.runtime.media_transcription_runtime import MediaTranscriptionRuntime


router = APIRouter(prefix="/internal/media-transcription", tags=["media-transcription"], dependencies=[Depends(require_internal_token)])
runtime = MediaTranscriptionRuntime()


class RemoteParseMetadataPayload(BaseModel):
    title: str | None = None
    platform: str | None = None
    duration: int | None = None
    noteType: str | None = None
    download_audio_url: str | None = None
    original_url: str | None = None


class RemoteParsePayload(BaseModel):
    download_url: str = Field(min_length=1)
    title: str | None = None
    source_url: str | None = None
    metadata: RemoteParseMetadataPayload | dict[str, object] | None = None


class AskPayload(BaseModel):
    task_id: str = Field(min_length=1)
    question: str = Field(min_length=1)


@router.post("/transcribe")
def transcribe(file: UploadFile = File(...)) -> dict[str, str]:
    try:
        return runtime.upload(file)
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"文件处理失败: {exc}") from exc


@router.post("/parse-video")
def parse_video(payload: RemoteParsePayload) -> dict[str, str]:
    try:
        metadata = payload.metadata.model_dump(exclude_none=True) if isinstance(payload.metadata, BaseModel) else payload.metadata
        return runtime.remote_parse(
            {
                "download_url": payload.download_url,
                "title": payload.title,
                "source_url": payload.source_url,
                "metadata": metadata or {},
            }
        )
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"工作流启动失败: {exc}") from exc


@router.get("/task/{task_id}")
def get_task(task_id: str) -> dict:
    task = runtime.get_task(task_id)
    if task is None:
        raise HTTPException(status_code=404, detail="task not found")
    return task


@router.get("/stream/{task_id}")
def stream(task_id: str) -> StreamingResponse:
    def generate():
        task = runtime.get_task(task_id)
        if task is None:
            yield _sse({"type": "error", "error": "Task not found"})
            return
        last_status = ""
        last_transcript = ""
        last_polished = ""
        last_summary = ""
        download_started = False
        ingest_started = False
        while True:
            task = runtime.get_task(task_id)
            if task is None:
                yield _sse({"type": "error", "error": "Task disappeared"})
                return
            status = str(task.get("status") or "")
            transcript = str(task.get("transcript") or "")
            polished = str(task.get("polished") or "")
            summary = str(task.get("summary") or "")
            metadata = task.get("metadata") or {}
            error = task.get("error")
            download_progress = task.get("download_progress") or {}
            intake_progress = task.get("intake_progress") or {}

            if status == "downloading" and not download_started:
                download_started = True
                yield _sse(
                    {
                        "type": "download_start",
                        "task_id": task_id,
                        "metadata": metadata,
                        "file_name": task.get("file_name"),
                        "progress": download_progress.get("progress", 0),
                        "downloaded_bytes": download_progress.get("downloaded_bytes", 0),
                        "total_bytes": download_progress.get("total_bytes", 0),
                        "eta_seconds": download_progress.get("eta_seconds"),
                        "speed_bytes_per_sec": download_progress.get("speed_bytes_per_sec", 0),
                    }
                )
            if status == "downloading":
                yield _sse(
                    {
                        "type": "download_progress",
                        "task_id": task_id,
                        "progress": download_progress.get("progress", 0),
                        "downloaded_bytes": download_progress.get("downloaded_bytes", 0),
                        "total_bytes": download_progress.get("total_bytes", 0),
                        "eta_seconds": download_progress.get("eta_seconds"),
                        "speed_bytes_per_sec": download_progress.get("speed_bytes_per_sec", 0),
                        "phase": download_progress.get("phase", "running"),
                    }
                )
            if intake_progress and (intake_progress.get("progress", 0) or 0) > 0:
                if not ingest_started:
                    ingest_started = True
                yield _sse(
                    {
                        "type": "ingest_progress",
                        "task_id": task_id,
                        "progress": intake_progress.get("progress", 0),
                        "processed_bytes": intake_progress.get("processed_bytes", 0),
                        "total_bytes": intake_progress.get("total_bytes", 0),
                        "eta_seconds": intake_progress.get("eta_seconds"),
                        "phase": intake_progress.get("phase", "preparing"),
                    }
                )

            if last_status != "transcribing" and status == "transcribing":
                if download_started:
                    yield _sse(
                        {
                            "type": "download_done",
                            "task_id": task_id,
                            "progress": 100,
                            "downloaded_bytes": download_progress.get("downloaded_bytes", 0),
                            "total_bytes": download_progress.get("total_bytes", 0),
                            "metadata": metadata,
                        }
                    )
                if ingest_started:
                    yield _sse(
                        {
                            "type": "ingest_done",
                            "task_id": task_id,
                            "progress": intake_progress.get("progress", 100),
                            "processed_bytes": intake_progress.get("processed_bytes", 0),
                            "total_bytes": intake_progress.get("total_bytes", 0),
                        }
                    )
                yield _sse({"type": "transcribe_start"})

            if transcript and transcript != last_transcript:
                yield _sse({"type": "chunk", "text": transcript})
                last_transcript = transcript

            if polished and polished != last_polished:
                yield _sse({"type": "polish_char", "text": polished})
                last_polished = polished

            if summary and summary != last_summary:
                yield _sse({"type": "summary_char", "text": summary})
                last_summary = summary

            if status == "done":
                if last_status == "transcribing":
                    yield _sse({"type": "transcribe_done"})
                if polished:
                    yield _sse({"type": "polish_done", "polished_text": polished})
                if summary:
                    yield _sse({"type": "summary_done", "summary_text": summary})
                yield _sse(
                    {
                        "type": "done",
                        "raw_text": transcript,
                        "polished_text": polished,
                        "summary_text": summary,
                        "char_count": len(polished or transcript),
                        "metadata": metadata,
                        "task_id": task_id,
                    }
                )
                return

            if status == "error":
                yield _sse({"type": "error", "error": error or "Unknown error"})
                return

            last_status = status
            time.sleep(0.3)

    return StreamingResponse(generate(), media_type="text/event-stream", headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"})


@router.post("/ask")
def ask(payload: AskPayload) -> dict[str, str]:
    try:
        return runtime.ask(payload.task_id, payload.question)
    except KeyError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc


@router.get("/export-markdown/{task_id}")
def export_markdown(task_id: str) -> Response:
    try:
        file_name, content = runtime.export_markdown(task_id)
    except KeyError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    return Response(
        content,
        media_type="text/markdown; charset=utf-8",
        headers={
            "Content-Disposition": f"attachment; filename=\"{file_name.encode('ascii', 'replace').decode('ascii')}\"; filename*=UTF-8''{quote(file_name)}"
        },
    )


def _sse(payload: dict) -> str:
    return f"data: {json.dumps(payload, ensure_ascii=False)}\n\n"
