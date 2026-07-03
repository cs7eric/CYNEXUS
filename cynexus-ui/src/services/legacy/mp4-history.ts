import type { MP4HistoryListItem, MP4HistoryRecord } from "@/lib/history-types"
import { LEGACY_API_BASE, fetchWithRetry, readLegacyJson } from "@/services/legacy/shared"

export async function listMP4History(): Promise<MP4HistoryListItem[]> {
  const res = await fetchWithRetry(`${LEGACY_API_BASE}/reference/mp4-history`, { cache: "no-store" })
  const data = await readLegacyJson<{ items?: MP4HistoryListItem[] }>(res, "获取历史记录失败")
  return data.items ?? []
}

export async function reorderMP4History(orderedIds: string[]): Promise<MP4HistoryListItem[]> {
  const res = await fetchWithRetry(`${LEGACY_API_BASE}/reference/mp4-history/reorder`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ ordered_ids: orderedIds }),
  })
  const data = await readLegacyJson<{ items?: MP4HistoryListItem[]; error?: string }>(
    res,
    "更新历史顺序失败",
  )
  return data.items ?? []
}

export async function deleteMP4History(historyId: string): Promise<{ id: string; title?: string }> {
  const res = await fetchWithRetry(`${LEGACY_API_BASE}/reference/mp4-history/${historyId}`, {
    method: "DELETE",
  })
  const data = await readLegacyJson<{ id?: string; title?: string; error?: string }>(res, "删除历史记录失败")
  if (!data.id) {
    throw new Error(data.error || "删除历史记录失败")
  }
  return { id: data.id, title: data.title }
}

export async function askHistoryQuestion(
  historyId: string,
  question: string,
): Promise<{ id: string; question: string; answer: string; created_at: string }> {
  const res = await fetchWithRetry(`${LEGACY_API_BASE}/reference/mp4-history/${historyId}/ask`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ question }),
  })
  const data = await readLegacyJson<{
    item?: { id: string; question: string; answer: string; created_at: string }
    error?: string
  }>(res, "历史记录 Ask AI 失败")
  if (!data.item) {
    throw new Error(data.error || "历史记录 Ask AI 失败")
  }
  return data.item
}

export async function getMP4History(id: string): Promise<MP4HistoryRecord> {
  const res = await fetchWithRetry(`${LEGACY_API_BASE}/reference/mp4-history/${id}`, { cache: "no-store" })
  return readLegacyJson<MP4HistoryRecord>(res, "获取历史详情失败")
}
