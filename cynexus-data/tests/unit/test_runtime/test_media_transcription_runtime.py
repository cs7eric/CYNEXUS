from __future__ import annotations

from pathlib import Path

from cynexus_data.runtime.context import PipelineContext
from cynexus_data.runtime.media_transcription_runtime import (
    MediaTranscriptionRuntime,
    _build_download_filename,
    _build_storage_filename,
)


class EchoTranscriber:
    def transcribe_streaming(self, audio_path: str, chunk_duration: int = 30, callback=None) -> str:
        if callback is not None:
            callback(0, "第一段转写", False)
            callback(1, "第一段转写 第二段转写", True)
        return "第一段转写 第二段转写"


def test_media_transcription_runtime_completes_task(monkeypatch, tmp_path: Path) -> None:
    monkeypatch.setenv("CYNEXUS_MEDIA_TRANSCRIPTION_TRANSCRIBER", f"{__name__}:EchoTranscriber")
    monkeypatch.setenv("CYNEXUS_MEDIA_TRANSCRIPTION_WORK_ROOT", str(tmp_path))
    runtime = MediaTranscriptionRuntime(context=PipelineContext())
    monkeypatch.setattr(runtime, "_persist_transcript", lambda *args, **kwargs: None)

    source = tmp_path / "demo.mp3"
    source.write_bytes(b"fake audio payload")

    runtime._task_store.create_task_record("task-1", "demo.mp3")
    runtime._run_local_transcription("task-1", source, "demo.mp3", {"title": "Demo", "platform": "Uploader"})

    task = runtime.get_task("task-1")
    assert task is not None
    assert task["status"] == "done"
    assert task["transcript"] == "第一段转写 第二段转写"
    assert [item["delta"] for item in task["transcript_events"]] == ["第一段转写", " 第二段转写"]
    assert task["polished"] == task["transcript"]
    assert "## 核心主题" in task["summary"]

    answer = runtime.ask("task-1", "第二段说了什么？")
    assert "第二段转写" in answer["answer"]

    file_name, content = runtime.export_markdown("task-1")
    assert file_name == "Demo.md"
    assert "## Transcript" in content.decode("utf-8")


def test_download_filename_is_utf8_byte_bounded_and_storage_name_is_task_scoped() -> None:
    file_name = _build_download_filename(
        "https://example.com/video.mp4",
        "配置要分七层" * 80 + "😵‍💫",
        "audio/mpeg",
    )

    assert len(file_name.encode("utf-8")) <= 120
    assert _build_storage_filename("task-123", file_name) == "task-123.mp3"
