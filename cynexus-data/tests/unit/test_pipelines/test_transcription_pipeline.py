from __future__ import annotations

from pathlib import Path

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.transcription import transcribe_media
from cynexus_data.pipelines.transcription.media import media_kind
from cynexus_data.runtime.context import PipelineContext


class FakeRepo:
    created = 0
    rows = {}

    def __init__(self, client):
        self.client = client

    def create_table(self):
        FakeRepo.created += 1

    def get(self, transcript_id):
        return FakeRepo.rows.get(transcript_id)

    def upsert(self, record):
        FakeRepo.rows[record["transcript_id"]] = record
        return 1


class FakePostgresHook:
    def get_client(self):
        return object()


class FakeHooks:
    postgres = FakePostgresHook()
    postgres_core = FakePostgresHook()


def test_media_kind() -> None:
    assert media_kind(Path("demo.wav")) == "audio"
    assert media_kind(Path("demo.mp4")) == "video"
    assert media_kind(Path("demo.txt")) == "unknown"


def test_transcribe_media_dry_run_skips_external_access(tmp_path) -> None:
    source = tmp_path / "demo.wav"
    result = transcribe_media.run(PipelineContext(), JobRequest(job_key="transcribe_media", dry_run=True, params={"input_uri": str(source)}))

    assert result["rows_written"] == 0
    assert result["media_kind"] == "audio"
    assert result["exists"] is False


def test_transcribe_media_persists_mock_transcript(monkeypatch, tmp_path) -> None:
    FakeRepo.created = 0
    FakeRepo.rows = {}
    source = tmp_path / "demo.wav"
    source.write_bytes(b"fake audio")
    monkeypatch.setattr(transcribe_media, "TranscriptRepository", FakeRepo)

    result = transcribe_media.run(
        PipelineContext(hooks=FakeHooks()),
        JobRequest(
            job_key="transcribe_media",
            dry_run=False,
            params={
                "input_uri": str(source),
                "transcript_id": "t1",
                "language": "zh",
                "mock_transcript": "hello transcript",
                "create_table": True,
            },
        ),
    )

    assert result["rows_read"] == 1
    assert result["rows_written"] == 1
    assert result["transcript"] == "hello transcript"
    assert FakeRepo.created == 1
    assert FakeRepo.rows["t1"]["language"] == "zh"


def test_transcribe_media_uses_cache_without_force(monkeypatch, tmp_path) -> None:
    FakeRepo.created = 0
    FakeRepo.rows = {"t1": {"transcript": "cached"}}
    source = tmp_path / "demo.wav"
    source.write_bytes(b"fake audio")
    monkeypatch.setattr(transcribe_media, "TranscriptRepository", FakeRepo)

    result = transcribe_media.run(
        PipelineContext(hooks=FakeHooks()),
        JobRequest(job_key="transcribe_media", dry_run=False, params={"input_uri": str(source), "transcript_id": "t1"}),
    )

    assert result["from_cache"] is True
    assert result["rows_written"] == 0
    assert result["transcript"] == "cached"
