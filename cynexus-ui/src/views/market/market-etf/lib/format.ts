export function formatDate(value?: string | null) {
  if (!value) return "—"
  return value.length >= 10 ? value.slice(5, 10).replace("-", "/") : value
}

export function formatDateTime(value?: string | null) {
  if (!value) return "—"
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString("zh-CN", { month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit" })
}

export function formatWan(value?: number | null, digits = 1) {
  if (value == null || !Number.isFinite(value)) return "—"
  const abs = Math.abs(value)
  const sign = value < 0 ? "-" : ""
  if (abs >= 100_000_000) return `${sign}${(abs / 100_000_000).toFixed(digits)}亿`
  if (abs >= 10_000) return `${sign}${(abs / 10_000).toFixed(digits)}万`
  return `${value.toFixed(digits)}`
}

export function formatPct(value?: number | null, digits = 2) {
  if (value == null || !Number.isFinite(value)) return "—"
  return `${value >= 0 ? "+" : ""}${value.toFixed(digits)}%`
}

export function formatNumber(value?: number | null, digits = 2) {
  if (value == null || !Number.isFinite(value)) return "—"
  return value.toLocaleString("zh-CN", { minimumFractionDigits: digits, maximumFractionDigits: digits })
}
