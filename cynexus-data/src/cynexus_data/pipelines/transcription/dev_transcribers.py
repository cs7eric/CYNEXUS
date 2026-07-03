from __future__ import annotations

from pathlib import Path


class EchoTranscriber:
    def __init__(self, text: str | None = None) -> None:
        self.text = (text or "").strip() or "这是本地联调用的转写占位结果。第一段：服务链路已经切到 cynexus-data。第二段：后续接真实模型时只需要替换 transcriber 配置。"

    def transcribe(self, audio_path: str, language: str | None = None) -> str:
        _ = language
        return self._render(audio_path)

    def transcribe_streaming(self, audio_path: str, chunk_duration: int = 30, callback=None) -> str:
        _ = chunk_duration
        full_text = self._render(audio_path)
        midpoint = max(1, len(full_text) // 2)
        parts = [full_text[:midpoint].strip(), full_text.strip()]
        for index, part in enumerate(parts):
            if callback is not None and part:
                callback(index, part, index == len(parts) - 1)
        return full_text

    def _render(self, audio_path: str) -> str:
        file_name = Path(audio_path).name
        return f"{self.text} 文件：{file_name}"
