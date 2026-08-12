from __future__ import annotations

import os
from pathlib import Path
from typing import Callable

from faster_whisper import WhisperModel


SegmentCallback = Callable[[int, str, bool], None]


class FasterWhisperTranscriber:
    def __init__(
        self,
        model_path: str | None = None,
        language: str | None = None,
        device: str | None = None,
        compute_type: str | None = None,
        cpu_threads: int = 4,
        num_workers: int = 1,
        beam_size: int = 1,
        vad_filter: bool = True,
        fallback_to_transformers: bool = True,
        fallback_device: str | None = None,
    ) -> None:
        resolved_model_path = Path(model_path or os.getenv("CYNEXUS_MEDIA_TRANSCRIPTION_MODEL_PATH", "")).expanduser()
        if not str(resolved_model_path).strip():
            raise RuntimeError("Whisper model path is required; set CYNEXUS_MEDIA_TRANSCRIPTION_MODEL_PATH or transcriber_params.model_path")
        if not resolved_model_path.exists():
            raise FileNotFoundError(f"Whisper model path not found: {resolved_model_path}")

        self.model_path = resolved_model_path
        self.language = (language or os.getenv("CYNEXUS_MEDIA_TRANSCRIPTION_LANGUAGE") or "zh").strip() or "zh"
        self.device = device or os.getenv("CYNEXUS_MEDIA_TRANSCRIPTION_DEVICE") or "auto"
        self.compute_type = compute_type or os.getenv("CYNEXUS_MEDIA_TRANSCRIPTION_COMPUTE_TYPE") or "auto"
        self.beam_size = beam_size
        self.vad_filter = vad_filter
        self.model: WhisperModel | object
        self._uses_transformers_fallback = False
        try:
            self.model = WhisperModel(
                str(self.model_path),
                device=self.device,
                compute_type=self.compute_type,
                cpu_threads=cpu_threads,
                num_workers=num_workers,
            )
        except RuntimeError as exc:
            if not fallback_to_transformers or "model.bin" not in str(exc):
                raise
            self._uses_transformers_fallback = True
            from cynexus_data.pipelines.transcription.providers.transformers_whisper import TransformersWhisperTranscriber

            self.model = TransformersWhisperTranscriber(
                model_path=str(self.model_path),
                language=self.language,
                device=fallback_device or "cpu",
            )

    def transcribe(self, audio_path: str, language: str | None = None) -> str:
        return self.transcribe_streaming(audio_path, language=language)

    def transcribe_streaming(
        self,
        audio_path: str,
        chunk_duration: int = 8,
        callback: SegmentCallback | None = None,
        language: str | None = None,
    ) -> str:
        _ = chunk_duration
        if self._uses_transformers_fallback:
            return self.model.transcribe_streaming(
                audio_path,
                chunk_duration=chunk_duration,
                callback=callback,
            )

        segments, _info = self.model.transcribe(
            audio_path,
            language=language or self.language,
            beam_size=self.beam_size,
            vad_filter=self.vad_filter,
        )
        parts: list[str] = []
        for index, segment in enumerate(segments):
            text = str(segment.text or "").strip()
            if not text:
                continue
            parts.append(text)
            current_text = " ".join(parts).strip()
            if callback is not None:
                callback(index, current_text, False)
        transcript = " ".join(parts).strip()
        if callback is not None and transcript:
            callback(len(parts), transcript, True)
        return transcript
