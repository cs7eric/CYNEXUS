import { fetchWithRetry } from "@/services/common/http"
import { SERVICE_PREFIX } from "@/services/common/service-prefix"

const API_BASE = `${SERVICE_PREFIX.core}/media-transcription/processes`

export type MediaProcessStatus = "queued" | "running" | "succeeded" | "failed" | string

export interface MediaProcessItem {
  id: string
  accepted?: boolean
  sourceUrl: string
  downloadUrl?: string | null
  title?: string | null
  platform?: string | null
  noteType?: string | null
  workerTaskId?: string | null
  historyId?: string | null
  status: MediaProcessStatus
  phase: string
  progress: number
  fileName?: string | null
  transcript?: string | null
  polished?: string | null
  summary?: string | null
  metadata?: Record<string, unknown>
  errorMessage?: string | null
  attemptCount?: number | null
  startedAt?: string | null
  completedAt?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

interface Envelope<T> {
  data?: T
  message?: string
  error?: string
}

interface ProcessListResponse {
  items?: MediaProcessItem[]
}

async function readData<T>(response: Response, fallback: string): Promise<T> {
  const payload = (await response.json().catch(() => null)) as Envelope<T> | T | null
  if (!response.ok || !payload) {
    throw new Error((payload as Envelope<T> | null)?.message || fallback)
  }
  if (typeof payload === "object" && "data" in payload) {
    const envelope = payload as Envelope<T>
    if (envelope.data == null) throw new Error(envelope.message || envelope.error || fallback)
    return envelope.data
  }
  return payload as T
}

export async function listMediaProcesses(limit = 50): Promise<MediaProcessItem[]> {
  const response = await fetchWithRetry(`${API_BASE}?limit=${limit}`, { cache: "no-store" })
  const data = await readData<ProcessListResponse>(response, "获取自动化处理记录失败")
  return data.items || []
}

export async function getMediaProcess(id: string): Promise<MediaProcessItem> {
  const response = await fetchWithRetry(`${API_BASE}/${encodeURIComponent(id)}`, { cache: "no-store" })
  return readData<MediaProcessItem>(response, "获取处理详情失败")
}

export async function retryMediaProcess(id: string): Promise<MediaProcessItem> {
  const response = await fetchWithRetry(`${API_BASE}/${encodeURIComponent(id)}/retry`, { method: "POST" })
  return readData<MediaProcessItem>(response, "重试自动化处理失败")
}
