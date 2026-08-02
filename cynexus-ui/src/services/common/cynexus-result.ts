interface CynexusResult<T> {
  code?: string
  message?: string
  data?: T
  success?: boolean
  failed?: boolean
}

function parseJsonPreservingLongIds<T>(text: string): T | null {
  if (!text.trim()) return null
  const normalized = text.replace(/"([A-Za-z][A-Za-z0-9_]*)":(-?\d{16,})/g, '"$1":"$2"')
  try {
    return JSON.parse(normalized) as T
  } catch {
    return null
  }
}

function responseBodySummary(text: string): string | null {
  const plainText = text.replace(/<[^>]*>/g, " ").replace(/\s+/g, " ").trim()
  if (!plainText) return null
  return plainText.length <= 400 ? plainText : `${plainText.slice(0, 400)}...`
}

export async function cynexusResult<T>(res: Response): Promise<T> {
  const text = await res.text()
  const data = parseJsonPreservingLongIds<CynexusResult<T>>(text)
  if (!res.ok || !data || data.success === false || data.failed === true) {
    const message = data?.message || (data as { error?: string } | null)?.error || responseBodySummary(text)
    const status = `HTTP ${res.status}${res.statusText ? ` ${res.statusText}` : ""}`
    throw new Error(message ? `${message} (${status})` : `request failed: ${status}`)
  }
  return data.data as T
}
