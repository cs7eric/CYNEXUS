from __future__ import annotations

import os
from pathlib import Path
from typing import Callable

import numpy as np
import scipy.signal
import soundfile as sf
import torch
from transformers import AutoModelForSpeechSeq2Seq, AutoProcessor


ChunkCallback = Callable[[int, str, bool], None]


class TransformersWhisperTranscriber:
    def __init__(
        self,
        model_path: str | None = None,
        language: str | None = None,
        device: str | None = None,
        use_safetensors: bool = True,
        max_new_tokens: int = 256,
    ) -> None:
        resolved_model_path = Path(model_path or os.getenv("CYNEXUS_MEDIA_TRANSCRIPTION_MODEL_PATH", "")).expanduser()
        if not str(resolved_model_path).strip():
            raise RuntimeError("Whisper model path is required; set CYNEXUS_MEDIA_TRANSCRIPTION_MODEL_PATH or transcriber_params.model_path")
        if not resolved_model_path.exists():
            raise FileNotFoundError(f"Whisper model path not found: {resolved_model_path}")
        self.model_path = resolved_model_path
        self.language = (language or os.getenv("CYNEXUS_MEDIA_TRANSCRIPTION_LANGUAGE") or "zh").strip() or "zh"
        self.device = device or ("cuda" if torch.cuda.is_available() else "cpu")
        self.torch_dtype = torch.float16 if self.device.startswith("cuda") else torch.float32
        self.max_new_tokens = max_new_tokens
        self.processor = AutoProcessor.from_pretrained(str(self.model_path))
        self.model = AutoModelForSpeechSeq2Seq.from_pretrained(
            str(self.model_path),
            dtype=self.torch_dtype,
            low_cpu_mem_usage=True,
            use_safetensors=use_safetensors,
        )
        self.model.to(self.device)

    def transcribe(self, audio_path: str, language: str | None = None) -> str:
        waveform, sample_rate = _load_audio(Path(audio_path))
        return self._transcribe_array(waveform, sample_rate, language or self.language)

    def transcribe_streaming(self, audio_path: str, chunk_duration: int = 30, callback: ChunkCallback | None = None) -> str:
        waveform, sample_rate = _load_audio(Path(audio_path))
        chunk_samples = max(1, int(chunk_duration * sample_rate))
        all_text: list[str] = []
        chunk_index = 0
        for start in range(0, len(waveform), chunk_samples):
            end = min(start + chunk_samples, len(waveform))
            chunk_waveform = waveform[start:end]
            chunk_text = self._transcribe_array(chunk_waveform, sample_rate, self.language).strip()
            if chunk_text:
                all_text.append(chunk_text)
            current_text = " ".join(all_text).strip()
            if callback is not None:
                callback(chunk_index, current_text, end >= len(waveform))
            chunk_index += 1
        return " ".join(all_text).strip()

    def _transcribe_array(self, waveform: np.ndarray, sample_rate: int, language: str) -> str:
        if waveform.size == 0:
            return ""
        inputs = self.processor.feature_extractor(
            raw_speech=waveform,
            sampling_rate=sample_rate,
            return_tensors="pt",
        )
        input_features = inputs.get("input_features")
        if input_features is None:
            raise RuntimeError("Whisper feature extraction returned no input_features")
        input_features = input_features.to(device=self.device, dtype=self.torch_dtype)
        generated_ids = self.model.generate(
            input_features=input_features,
            return_timestamps=False,
            language=language,
            task="transcribe",
            max_new_tokens=self.max_new_tokens,
        )
        text = self.processor.batch_decode(
            generated_ids,
            skip_special_tokens=True,
            clean_up_tokenization_spaces=False,
        )[0]
        return str(text).strip()


def _load_audio(audio_path: Path) -> tuple[np.ndarray, int]:
    if not audio_path.exists():
        raise FileNotFoundError(str(audio_path))
    data, sample_rate = sf.read(str(audio_path), dtype="float32")
    if getattr(data, "ndim", 1) > 1:
        data = data.mean(axis=1)
    if sample_rate != 16000:
        target_samples = int(len(data) * 16000 / sample_rate)
        data = scipy.signal.resample(data, target_samples).astype("float32")
        sample_rate = 16000
    return np.asarray(data, dtype="float32"), sample_rate
