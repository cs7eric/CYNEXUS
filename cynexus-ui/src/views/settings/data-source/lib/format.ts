import type { DataSourceFreshnessItem, DataSourceStatus } from "@/services/market/data-source"

export const STATUS_LABELS: Record<DataSourceStatus, string> = {
  fresh: "正常",
  lagging: "滞后",
  stale: "过期",
  empty: "空表",
  untracked: "未识别",
  error: "异常",
}

export function formatDate(value: string | null | undefined): string {
  if (!value) return "-"
  return value.slice(0, 10)
}

export function formatDateTime(value: string | null | undefined): string {
  if (!value) return "-"
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString("zh-CN", { hour12: false, timeZone: "Asia/Shanghai" })
}

export function formatInteger(value: number | null | undefined): string {
  if (value === null || value === undefined || Number.isNaN(value)) return "-"
  return new Intl.NumberFormat("zh-CN").format(value)
}

export function freshnessHint(item: DataSourceFreshnessItem): string {
  if (item.status === "empty") return "暂无有效行"
  if (item.status === "untracked") return "未找到业务日期列"
  if (item.status === "error") return item.error || "读取失败"
  if (item.lagDays === null || item.lagDays === undefined) return "-"
  if (item.lagDays <= 0) return "今天"
  return `${item.lagDays} 天前`
}
