import { DOWNLOADER_API_BASE, fetchWithRetry, readLegacyJson } from "@/services/legacy/shared"

export interface DownloaderPageInfo {
  page: number
  cid: string
  part: string
  duration: number
  downloadAudioUrl: string | null
  downloadVideoUrl: string | null
}

export interface DownloaderEmbeddedVideoInfo {
  id: string
  title: string
  cover?: string | null
  duration?: number
  downloadVideoUrl?: string | null
  downloadAudioUrl?: string | null
}

export interface DownloaderParseData {
  title: string
  desc?: string
  cover?: string | null
  platform: string
  downloadAudioUrl: string | null
  downloadVideoUrl: string | null
  originDownloadAudioUrl?: string | null
  originDownloadVideoUrl?: string | null
  url: string
  duration?: number
  isMultiPart?: boolean
  currentPage?: number
  pages?: DownloaderPageInfo[]
  noteType?: "video" | "image" | "audio"
  images?: Array<string | { index?: number; url?: string | null; downloadUrl?: string | null }>
  videos?: DownloaderEmbeddedVideoInfo[]
}

export interface RemoteParsePayload {
  downloadUrl: string
  title?: string
  sourceUrl?: string
  metadata?: Record<string, unknown>
}

interface DownloaderParseResponse {
  success: boolean
  data?: DownloaderParseData
  error?: string
  message?: string
}

export async function parseDownloaderUrl(url: string): Promise<DownloaderParseData> {
  const params = new URLSearchParams({ url })
  const res = await fetchWithRetry(`${DOWNLOADER_API_BASE}/api/parse?${params.toString()}`, {
    method: "GET",
    cache: "no-store",
  })
  const data = await readLegacyJson<DownloaderParseResponse>(res, "解析失败")
  if (!data.success || !data.data) {
    throw new Error(data.error || data.message || "解析失败")
  }
  return data.data
}
