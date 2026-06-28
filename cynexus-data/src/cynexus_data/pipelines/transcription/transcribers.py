from __future__ import annotations

import importlib
from pathlib import Path
from typing import Any


def transcribe_audio(
    audio_path: Path,
    *,
    language: str | None = None,
    transcript_text: str | None = None,
    transcriber_path: str | None = None,
    transcriber_params: dict[str, Any] | None = None,
) -> str:
    if transcript_text is not None:
        return transcript_text
    if not transcriber_path:
        raise RuntimeError("real transcription requires params.transcriber or params.transcript_text for controlled imports/tests")
    transcriber = load_transcriber(transcriber_path, transcriber_params or {})
    if callable(transcriber) and not hasattr(transcriber, "transcribe"):
        return str(transcriber(str(audio_path), language=language))
    if hasattr(transcriber, "transcribe"):
        return str(transcriber.transcribe(str(audio_path), language=language))
    if hasattr(transcriber, "transcribe_streaming"):
        return str(transcriber.transcribe_streaming(str(audio_path), chunk_duration=30))
    raise TypeError(f"unsupported transcriber object from {transcriber_path}")


def load_transcriber(path: str, params: dict[str, Any]) -> Any:
    module_name, _, attr_name = path.partition(":")
    if not module_name or not attr_name:
        raise ValueError("transcriber must be in module:attribute format")
    module = importlib.import_module(module_name)
    target = getattr(module, attr_name)
    if isinstance(target, type):
        return target(**params)
    if callable(target):
        try:
            return target(**params)
        except TypeError:
            return target
    return target
