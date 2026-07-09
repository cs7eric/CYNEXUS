import type { MP4HistoryListItem, MP4HistoryRecord } from "@/lib/history-types"
import { fetchWithRetry } from "@/services/common/http"
import { SERVICE_PREFIX } from "@/services/common/service-prefix"

const CORE_TRANSCRIPTION_API_BASE = `${SERVICE_PREFIX.core}/media-transcription`

interface CoreEnvelope<T> {
  data?: T
  message?: string
  error?: string
}

interface CoreHistoryItem {
  id?: string
  title?: string
  createdAt?: string
  taskId?: string
  status?: string
  fileName?: string | null
  dataFile?: string
}

interface CoreHistoryList {
  items?: CoreHistoryItem[]
}

interface CoreHistoryQaItem {
  id: string
  question: string
  answer: string
  createdAt?: string
  created_at?: string
}

async function readCoreData<T>(res: Response, fallback: string): Promise<T> {
  const payload = (await res.json().catch(() => null)) as CoreEnvelope<T> | T | null
  if (!res.ok || !payload) {
    throw new Error((payload as CoreEnvelope<T> | null)?.message || fallback)
  }
  if (typeof payload === "object" && "data" in payload) {
    const envelope = payload as CoreEnvelope<T>
    if (envelope.data == null) {
      throw new Error(envelope.message || envelope.error || fallback)
    }
    return envelope.data
  }
  return payload as T
}

function normalizeItem(item: CoreHistoryItem): MP4HistoryListItem {
  return {
    id: String(item.id || ""),
    title: item.title || "未命名历史记录",
    created_at: item.createdAt || "",
    task_id: item.taskId || "",
    status: item.status || "",
    file_name: item.fileName || "",
    data_file: item.dataFile || "",
  }
}

function normalizeRecord(record: MP4HistoryRecord): MP4HistoryRecord {
  return record
}

function normalizeQaItem(item: CoreHistoryQaItem) {
  return {
    id: item.id,
    question: item.question,
    answer: item.answer,
    created_at: item.created_at || item.createdAt || "",
  }
}

export async function listMP4History(): Promise<MP4HistoryListItem[]> {
  const res = await fetchWithRetry(`${CORE_TRANSCRIPTION_API_BASE}/history`, { cache: "no-store" })
  const data = await readCoreData<CoreHistoryList>(res, "获取历史记录失败")
  return (data.items ?? []).map(normalizeItem)
}

export async function saveMP4History(taskId: string): Promise<MP4HistoryListItem> {
  const res = await fetchWithRetry(`${CORE_TRANSCRIPTION_API_BASE}/history`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ taskId }),
  })
  return normalizeItem(await readCoreData<CoreHistoryItem>(res, "保存历史记录失败"))
}

export async function reorderMP4History(orderedIds: string[]): Promise<MP4HistoryListItem[]> {
  const res = await fetchWithRetry(`${CORE_TRANSCRIPTION_API_BASE}/history/reorder`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ orderedIds }),
  })
  const data = await readCoreData<CoreHistoryList>(res, "更新历史顺序失败")
  return (data.items ?? []).map(normalizeItem)
}

export async function deleteMP4History(historyId: string): Promise<{ id: string; title?: string }> {
  const res = await fetchWithRetry(`${CORE_TRANSCRIPTION_API_BASE}/history/${encodeURIComponent(historyId)}`, {
    method: "DELETE",
  })
  const data = await readCoreData<{ id?: string; title?: string }>(res, "删除历史记录失败")
  if (!data.id) {
    throw new Error("删除历史记录失败")
  }
  return { id: data.id, title: data.title }
}

export async function askHistoryQuestion(
  historyId: string,
  question: string,
): Promise<{ id: string; question: string; answer: string; created_at: string }> {
  const res = await fetchWithRetry(`${CORE_TRANSCRIPTION_API_BASE}/history/${encodeURIComponent(historyId)}/ask`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ question }),
  })
  const data = await readCoreData<{ item?: CoreHistoryQaItem }>(res, "历史记录 Ask AI 失败")
  if (!data.item) {
    throw new Error("历史记录 Ask AI 失败")
  }
  return normalizeQaItem(data.item)
}

export async function getMP4History(id: string): Promise<MP4HistoryRecord> {
  const res = await fetchWithRetry(`${CORE_TRANSCRIPTION_API_BASE}/history/${encodeURIComponent(id)}`, { cache: "no-store" })
  return normalizeRecord(await readCoreData<MP4HistoryRecord>(res, "获取历史详情失败"))
}
