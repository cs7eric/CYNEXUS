# Media Transcription URL API

The URL-only entry point accepts the same source-page URL that the MP4-to-Word
page sends to the Downloader parser. It then runs the asynchronous process:

```text
source URL -> Downloader resolve -> media URL -> transcription -> AI polish
           -> summary -> metadata -> MP4 history
```

## Submit

```http
POST /api/core/media-transcription/tasks/parse-url
Content-Type: application/json
Idempotency-Key: optional-client-key
```

```json
{
  "url": "https://www.example.com/video-page",
  "title": "optional title",
  "platform": "optional platform",
  "noteType": "video"
}
```

Only `url` is required. The endpoint only creates the queued process and
returns immediately; it does not wait for Downloader, transcription, or AI
processing. The response is an enqueue acknowledgement:

```json
{
  "accepted": true,
  "message": "已入队",
  "processId": "123",
  "status": "queued"
}
```

Use `data.processId` to poll the process:

```http
GET /api/core/media-transcription/processes/{data.processId}
```

`Idempotency-Key` can be reused by a caller to avoid creating duplicate
processes when a request is retried.
