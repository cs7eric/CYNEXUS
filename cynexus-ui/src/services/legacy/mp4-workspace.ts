import type { MP4HistoryListItem } from "@/lib/history-types"
import type { Phase, QAResponse, SSEEvent, TransferProgress } from "@/lib/types"
import { LEGACY_API_BASE, fetchWithRetry, readLegacyJson } from "@/services/legacy/shared"
import type { RemoteParsePayload } from "@/services/legacy/downloader"

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

export async function uploadFile(file: File): Promise<string> {
  const formData = new FormData()
  formData.append("file", file)
  const res = await fetchWithRetry(`${LEGACY_API_BASE}/transcribe`, {
    method: "POST",
    body: formData,
  })
  const data = await readLegacyJson<{ task_id?: string; error?: string }>(res, "上传失败")
  if (!data.task_id) {
    throw new Error(data.error || "上传失败")
  }
  return data.task_id
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
      const data = JSON.parse(xhr.responseText || "{}") as { task_id?: string; error?: string }
      if (xhr.status >= 200 && xhr.status < 300 && data.task_id) {
        onProgress({
          phase: "done",
          progress: 100,
          processed_bytes: file.size,
          total_bytes: file.size,
          eta_seconds: 0,
        })
        resolve(data.task_id)
        return
      }
      reject(new Error(data.error || "上传失败"))
    }

    xhr.onerror = () => reject(new Error("上传失败"))
    xhr.open("POST", `${LEGACY_API_BASE}/transcribe`)
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
  const es = new EventSource(`${LEGACY_API_BASE}/stream/${taskId}`)

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
  const res = await fetchWithRetry(`${LEGACY_API_BASE}/task/${taskId}`, {
    method: "GET",
    cache: "no-store",
  })
  const data = (await res.json().catch(() => null)) as TaskSnapshot | { error?: string } | null
  if (!res.ok || !data || !("task_id" in data)) {
    throw new Error((data && "error" in data && data.error) || "获取任务状态失败")
  }
  return data
}

export async function askQuestion(taskId: string, question: string): Promise<string> {
  const res = await fetchWithRetry(`${LEGACY_API_BASE}/ask`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ task_id: taskId, question }),
  })
  const data = await readLegacyJson<QAResponse & { error?: string }>(res, "请求失败")
  return data.answer
}

export async function exportMarkdown(taskId: string): Promise<void> {
  const res = await fetchWithRetry(`${LEGACY_API_BASE}/export-markdown/${taskId}`)
  if (!res.ok) throw new Error("导出失败")
  const blob = await res.blob()
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement("a")
  anchor.href = url
  anchor.download = "transcription.md"
  anchor.click()
  URL.revokeObjectURL(url)
}

export async function checkStatus(): Promise<{
  api_configured: boolean
  gpu_available: boolean
  model_loaded: boolean
  status: string
}> {
  const res = await fetchWithRetry(`${LEGACY_API_BASE}/status`)
  return readLegacyJson(res, "检查状态失败")
}

export async function saveMP4History(taskId: string): Promise<MP4HistoryListItem> {
  const res = await fetchWithRetry(`${LEGACY_API_BASE}/reference/mp4-history`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ task_id: taskId }),
  })
  return readLegacyJson<MP4HistoryListItem>(res, "保存历史记录失败")
}

export async function sendDownloaderResultToParse(
  payload: RemoteParsePayload,
): Promise<{ task_id: string; file_name: string }> {
  const res = await fetchWithRetry(`${LEGACY_API_BASE}/parse-video`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      download_url: payload.downloadUrl,
      title: payload.title,
      source_url: payload.sourceUrl,
      metadata: payload.metadata,
    }),
  })
  const data = await readLegacyJson<{ task_id?: string; file_name?: string; error?: string }>(
    res,
    "Send to parse 失败",
  )
  if (!data.task_id || !data.file_name) {
    throw new Error(data.error || "Send to parse 失败")
  }
  return {
    task_id: data.task_id,
    file_name: data.file_name,
  }
}
