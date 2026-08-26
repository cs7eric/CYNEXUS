# Media Transcription URL API

The URL-only entry point accepts a source-page URL or a share clipboard
`text` (for example a Douyin share card). It extracts the first http(s) URL,
calls the same Downloader parse API as the MP4-to-Word “解析链接” control,
and only then submits the resolved video/audio URL to transcription.

```text
share text / page URL -> extract URL -> Downloader parse
  -> origin audio/video URL -> Aliyun / local transcription
  -> AI polish -> summary -> metadata -> MP4 history
```

If Downloader does not return a media URL, the process fails. The page URL is
not sent to Aliyun. A direct `.mp3` / `.mp4` / `.wav` URL may skip parse.

This enqueue API is not exposed to the autonomous Agent: it starts paid
external download and ASR work and is a mutation, not a bounded read.

## Submit

```http
POST /api/workflows/media-transcription
Content-Type: application/json
Idempotency-Key: optional-client-key
```

The legacy path `POST /api/core/media-transcription/tasks/parse-url` remains valid.

```json
{
  "text": "复制打开抖音，看看【作品】标题 https://v.douyin.com/xxxx/",
  "url": "https://v.douyin.com/xxxx/",
  "title": "optional title",
  "platform": "optional platform",
  "noteType": "video"
}
```

`text` or `url` is required. `url` wins when both are present. The endpoint
only creates the queued process and returns immediately; it does not wait for
Downloader, transcription, or AI processing. The response is an enqueue
acknowledgement:

```json
{
  "accepted": true,
  "message": "queued",
  "processId": "123",
  "status": "queued",
  "workflowType": "MEDIA_TRANSCRIPTION",
  "workflowKey": "media.transcription",
  "pollUrl": "/api/core/media-transcription/processes/123"
}
```

Use `data.processId` to poll the process:

```http
GET /api/core/media-transcription/processes/{data.processId}
```

`Idempotency-Key` can be reused by a caller to avoid creating duplicate
processes when a request is retried.
