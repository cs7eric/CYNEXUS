import type { MP4HistoryListItem } from "@/lib/history-types"
import type { Phase, QAResponse, SSEEvent, TransferProgress } from "@/lib/types"
import { fetchWithRetry } from "@/services/common/http"
import { SERVICE_PREFIX } from "@/services/common/service-prefix"
import type { RemoteParsePayload } from "@/services/legacy/downloader"
import { saveMP4History as saveCoreMP4History } from "@/services/core/mp4-history"

const CORE_TRANSCRIPTION_API_BASE = `${SERVICE_PREFIX.core}/media-transcription`

export interface TaskSnapshot {
  task_id: string
  status: Phase | string
  transcript: string
  polished: string
  summary: string
  metadata: Record<string, unknown>
  file_name: string
  error?: string | null
  download_progress: TransferProgress
  intake_progress: TransferProgress
}

interface CoreTaskRefResponse {
  taskId?: string
  fileName?: string | null
}

interface CoreTaskSnapshotResponse {
  taskId: string
  status: Phase | string
  transcript?: string | null
  polished?: string | null
  summary?: string | null
  metadata?: Record<string, unknown> | null
  fileName?: string | null
  error?: string | null
  downloadProgress?: CoreProgressResponse | null
  intakeProgress?: CoreProgressResponse | null
}

interface CoreProgressResponse {
  phase?: string | null
  progress?: number | null
  downloadedBytes?: number | null
  totalBytes?: number | null
  processedBytes?: number | null
  etaSeconds?: number | null
  speedBytesPerSec?: number | null
}

async function readCoreData<T>(res: Response, fallback: string): Promise<T> {
  const payload = (await res.json().catch(() => null)) as { data?: T; message?: string; error?: string } | T | null
  if (!res.ok || !payload) {
    throw new Error((payload as { message?: string; error?: string } | null)?.message || fallback)
  }
  if (typeof payload === "object" && "data" in payload) {
    if (payload.data == null) {
      throw new Error(payload.message || payload.error || fallback)
    }
    return payload.data as T
  }
  return payload as T
}

function normalizeProgress(progress?: CoreProgressResponse | null): TransferProgress {
  return {
    phase: progress?.phase || undefined,
    progress: progress?.progress ?? undefined,
    downloaded_bytes: progress?.downloadedBytes ?? undefined,
    total_bytes: progress?.totalBytes ?? undefined,
    processed_bytes: progress?.processedBytes ?? undefined,
    eta_seconds: progress?.etaSeconds ?? undefined,
    speed_bytes_per_sec: progress?.speedBytesPerSec ?? undefined,
  }
}

function normalizeTask(snapshot: CoreTaskSnapshotResponse): TaskSnapshot {
  return {
    task_id: snapshot.taskId,
    status: snapshot.status,
    transcript: snapshot.transcript || "",
    polished: snapshot.polished || "",
    summary: snapshot.summary || "",
    metadata: snapshot.metadata || {},
    file_name: snapshot.fileName || "",
    error: snapshot.error || null,
    download_progress: normalizeProgress(snapshot.downloadProgress),
    intake_progress: normalizeProgress(snapshot.intakeProgress),
  }
}

export async function uploadFile(file: File): Promise<string> {
  const formData = new FormData()
  formData.append("file", file)
  const res = await fetchWithRetry(`${CORE_TRANSCRIPTION_API_BASE}/tasks/upload`, {
    method: "POST",
    body: formData,
  })
  const data = await readCoreData<CoreTaskRefResponse>(res, "上传失败")
  if (!data.taskId) throw new Error("上传失败")
  return data.taskId
}

export function uploadFileWithProgress(
  file: File,
  onProgress: (progress: TransferProgress) => void,
): Promise<string> {
  return new Promise((resolve, reject) => {
    const formData = new FormData()
    formData.append("file", file)
    const xhr = new XMLHttpRequest()
    const startedAt = Date.now()

    xhr.upload.onprogress = (event) => {
      if (!event.lengthComputable) return
      const elapsed = Math.max((Date.now() - startedAt) / 1000, 0.001)
      const speed = event.loaded / elapsed
      onProgress({
        phase: "uploading",
        progress: Math.round((event.loaded / event.total) * 100),
        processed_bytes: event.loaded,
        total_bytes: event.total,
        eta_seconds: speed > 0 ? Math.round((event.total - event.loaded) / speed) : null,
        speed_bytes_per_sec: Math.round(speed),
      })
    }

    xhr.onload = () => {
      const payload = JSON.parse(xhr.responseText || "{}") as { data?: CoreTaskRefResponse; message?: string }
      const taskId = payload.data?.taskId
      if (xhr.status >= 200 && xhr.status < 300 && taskId) {
        onProgress({ phase: "done", progress: 100, processed_bytes: file.size, total_bytes: file.size, eta_seconds: 0 })
        resolve(taskId)
        return
      }
      reject(new Error(payload.message || "上传失败"))
    }

    xhr.onerror = () => reject(new Error("上传失败"))
    xhr.open("POST", `${CORE_TRANSCRIPTION_API_BASE}/tasks/upload`)
    xhr.send(formData)
  })
}

export function createSSEConnection(
  taskId: string,
  callbacks: {
    onEvent: (event: SSEEvent) => void
    onError?: (err: string) => void
    onDone?: () => void
  },
): EventSource {
  const es = new EventSource(`${CORE_TRANSCRIPTION_API_BASE}/tasks/${taskId}/stream`)
  es.onmessage = (event) => {
    try {
      const parsed = JSON.parse(event.data) as SSEEvent
      callbacks.onEvent(parsed)
      if (parsed.type === "done") {
        callbacks.onDone?.()
        es.close()
      }
    } catch {
      return
    }
  }
  es.onerror = () => {
    callbacks.onError?.("SSE disconnected")
    es.close()
  }
  return es
}

export async function fetchTaskSnapshot(taskId: string): Promise<TaskSnapshot> {
  const res = await fetchWithRetry(`${CORE_TRANSCRIPTION_API_BASE}/tasks/${taskId}`, {
    method: "GET",
    cache: "no-store",
  })
  return normalizeTask(await readCoreData<CoreTaskSnapshotResponse>(res, "获取任务状态失败"))
}

export async function askQuestion(taskId: string, question: string): Promise<string> {
  const res = await fetchWithRetry(`${CORE_TRANSCRIPTION_API_BASE}/tasks/${taskId}/ask`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ question }),
  })
  const data = await readCoreData<QAResponse>(res, "请求失败")
  return data.answer
}

export async function exportMarkdown(taskId: string): Promise<void> {
  const res = await fetchWithRetry(`${CORE_TRANSCRIPTION_API_BASE}/tasks/${taskId}/export-markdown`)
  if (!res.ok) throw new Error("导出失败")
  const blob = await res.blob()
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement("a")
  anchor.href = url
  anchor.download = "transcription.md"
  anchor.click()
  URL.revokeObjectURL(url)
}

export async function saveMP4History(taskId: string): Promise<MP4HistoryListItem> {
  return saveCoreMP4History(taskId)
}

export async function sendDownloaderResultToParse(
  payload: RemoteParsePayload,
): Promise<{ task_id: string; file_name: string }> {
  const res = await fetchWithRetry(`${CORE_TRANSCRIPTION_API_BASE}/tasks/remote-parse`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      downloadUrl: payload.downloadUrl,
      title: payload.title,
      sourceUrl: payload.sourceUrl,
      platform: payload.metadata?.platform,
      duration: payload.metadata?.duration,
      noteType: payload.metadata?.noteType,
      audioUrl: payload.metadata?.download_audio_url,
      originalUrl: payload.metadata?.original_url,
    }),
  })
  const data = await readCoreData<CoreTaskRefResponse>(res, "Send to parse 失败")
  if (!data.taskId || !data.fileName) throw new Error("Send to parse 失败")
  return { task_id: data.taskId, file_name: data.fileName }
}
