# Transcription Pipeline Design

Only the media/audio to transcript step is in scope.

In scope:

- Resolve a local `file://` URI or configured local path.
- Convert video to 16 kHz mono WAV when needed.
- Run a transcriber implementation provided by `params.transcriber` (`module:attribute`) or accept `params.transcript_text` / `params.mock_transcript` for controlled repair/tests.
- Return transcript text and metadata.
- Write transcript rows to PostgreSQL `cynexus_appl_core.media_transcript`.

## Job

`transcribe_media`

## Parameters

- `input_uri`: required local path or `file://` URI.
- `transcript_id`: optional stable id; defaults to a hash of `input_uri`.
- `biz_id`: optional caller id; used as `transcript_id` if `transcript_id` is absent.
- `language`: optional transcriber language hint.
- `force`: bypass cached transcript row.
- `create_table`: apply the job executor DDL containing `media_transcript`.
- `work_dir`: WAV conversion directory, default `var/transcription`.
- `ffmpeg_path`: optional explicit ffmpeg executable.
- `transcriber`: optional `module:attribute` loader path. The object may expose `transcribe(audio_path, language=...)`, `transcribe_streaming(audio_path, ...)`, or be directly callable.
- `transcript_text` / `mock_transcript`: directly supplied transcript, intended for controlled imports, tests, and manual repair.

## Legacy Boundary

The old service continued into polish, summary, metadata generation, and ask/Q&A. Those surfaces remain out of scope for `cynexus-data`; this job stops once the transcript has been produced and persisted.

Out of scope:

- AI polish.
- Summary.
- Ask/Q&A.
- Prompt management.
- AI provider management.
