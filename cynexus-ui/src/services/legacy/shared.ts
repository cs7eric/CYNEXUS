import { fetchWithRetry } from "@/services/common/http"
import { SERVICE_PREFIX } from "@/services/common/service-prefix"

export const LEGACY_API_BASE = `${SERVICE_PREFIX.legacy}/api`
export const DOWNLOADER_API_BASE =
  (typeof import.meta !== "undefined" && import.meta.env?.VITE_DOWNLOADER_API_BASE) ||
  "https://downloader-api.bhwa233.com"

export function withTimeout(ms: number): { signal: AbortSignal } {
  const controller = new AbortController()
  setTimeout(() => controller.abort(), ms)
  return { signal: controller.signal }
}

export async function readLegacyJson<T>(
  res: Response,
  fallbackMessage: string,
): Promise<T> {
  const data = (await res.json().catch(() => null)) as
    | (T & { error?: string; message?: string })
    | null
  if (!res.ok || !data) {
    throw new Error(data?.error || data?.message || fallbackMessage)
  }
  return data
}

export { fetchWithRetry }
