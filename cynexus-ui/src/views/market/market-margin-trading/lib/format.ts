export function formatYi(value: number | null | undefined, digits = 1) {
  if (value == null || !Number.isFinite(value)) return "-"
  return `${value.toLocaleString("zh-CN", {
    maximumFractionDigits: digits,
    minimumFractionDigits: value >= 1000 ? 0 : Math.min(digits, 1),
  })} 亿`
}

export function formatYuanAsYi(value: number | null | undefined, digits = 1) {
  if (value == null || !Number.isFinite(value)) return "-"
  return formatYi(value / 100000000, digits)
}

export function formatNumber(value: number | null | undefined) {
  if (value == null || !Number.isFinite(value)) return "-"
  return value.toLocaleString("zh-CN", { maximumFractionDigits: 0 })
}

export function formatDate(value: string | null | undefined) {
  if (!value) return "-"
  return value.replaceAll("-", "/")
}

export function formatDateTime(value: string | null | undefined) {
  if (!value) return "-"
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString("zh-CN", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  })
}
