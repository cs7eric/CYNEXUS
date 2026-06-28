from __future__ import annotations

import re
import shutil
import subprocess
from pathlib import Path


AUDIO_SUFFIXES = {".mp3", ".wav", ".m4a", ".aac", ".ogg", ".flac", ".aiff"}
VIDEO_SUFFIXES = {".mp4", ".mov", ".mkv", ".avi", ".webm"}


def media_kind(path: Path) -> str:
    suffix = path.suffix.lower()
    if suffix in AUDIO_SUFFIXES:
        return "audio"
    if suffix in VIDEO_SUFFIXES:
        return "video"
    return "unknown"


def prepare_audio(input_path: Path, *, media_type: str, work_dir: Path, ffmpeg_path: str | None = None) -> Path:
    if media_type == "audio":
        return input_path
    if media_type != "video":
        raise ValueError(f"unsupported media type for transcription: {input_path.suffix}")
    work_dir.mkdir(parents=True, exist_ok=True)
    audio_path = work_dir / f"{input_path.stem}.transcribe.wav"
    cmd = [
        resolve_ffmpeg(ffmpeg_path),
        "-y",
        "-i",
        str(input_path),
        "-ar",
        "16000",
        "-ac",
        "1",
        "-acodec",
        "pcm_s16le",
        str(audio_path),
    ]
    result = subprocess.run(cmd, capture_output=True, text=True, timeout=600)
    if result.returncode != 0:
        raise RuntimeError(f"ffmpeg conversion failed: {result.stderr[-500:]}")
    return audio_path


def probe_duration_seconds(path: Path, *, ffmpeg_path: str | None = None) -> float | None:
    try:
        result = subprocess.run(
            [resolve_ffmpeg(ffmpeg_path), "-i", str(path), "-f", "null", "-"],
            capture_output=True,
            text=True,
            timeout=30,
        )
    except Exception:
        return None
    match = re.search(r"Duration:\s*(\d+):(\d+):(\d+)\.(\d+)", result.stderr or "")
    if not match:
        return None
    hours, minutes, seconds, frac = match.groups()
    return int(hours) * 3600 + int(minutes) * 60 + int(seconds) + float(f"0.{frac}")


def resolve_ffmpeg(explicit_path: str | None = None) -> str:
    if explicit_path:
        return explicit_path
    found = shutil.which("ffmpeg")
    if found:
        return found
    try:
        import imageio_ffmpeg
    except ImportError as exc:  # pragma: no cover - optional runtime dependency
        raise RuntimeError("ffmpeg is required for video conversion; install ffmpeg or imageio-ffmpeg") from exc
    return str(imageio_ffmpeg.get_ffmpeg_exe())
