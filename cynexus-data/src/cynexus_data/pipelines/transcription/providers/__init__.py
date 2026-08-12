from importlib import import_module

__all__ = ["FasterWhisperTranscriber", "TransformersWhisperTranscriber"]


def __getattr__(name: str):
    if name == "FasterWhisperTranscriber":
        return getattr(import_module("cynexus_data.pipelines.transcription.providers.faster_whisper"), name)
    if name == "TransformersWhisperTranscriber":
        return getattr(import_module("cynexus_data.pipelines.transcription.providers.transformers_whisper"), name)
    raise AttributeError(name)
