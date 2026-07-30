export function formatPosition(value: number | null | undefined, digits = 0) {
  if (value == null || !Number.isFinite(value)) return "-"
  return value.toLocaleString("zh-CN", { maximumFractionDigits: digits, minimumFractionDigits: digits })
}

export function formatRatio(value: number | null | undefined) {
  if (value == null || !Number.isFinite(value)) return "-"
  return `${value.toLocaleString("zh-CN", { maximumFractionDigits: 2, minimumFractionDigits: 2 })}x`
}

export function formatSigned(value: number | null | undefined) {
  if (value == null || !Number.isFinite(value)) return "-"
  const formatted = formatPosition(Math.abs(value))
  return value > 0 ? `+${formatted}` : value < 0 ? `-${formatted}` : formatted
}

export function formatDate(value: string | null | undefined) {
  return value ? value.replaceAll("-", "/") : "-"
}

export function formatDateTime(value: string | null | undefined) {
  if (!value) return "-"
  const date = new Date(value)
  return Number.isNaN(date.getTime())
    ? value
    : date.toLocaleString("zh-CN", { month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit" })
}

export function toneForValue(value: number | null | undefined) {
  if (value == null || value === 0) return "text-muted-foreground"
  return value > 0 ? "text-rose-600 dark:text-rose-400" : "text-emerald-600 dark:text-emerald-400"
}
