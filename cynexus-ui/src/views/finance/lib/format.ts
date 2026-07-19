export function formatMoney(value?: number | null) {
  if (value == null || !Number.isFinite(value)) return "—"
  return value.toLocaleString("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

export function formatDateTime(value?: string | null) {
  if (!value) return "—"
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString("zh-CN", { month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit" })
}

export function today() {
  return localDate(new Date())
}

export function monthBounds() {
  const now = new Date()
  const start = new Date(now.getFullYear(), now.getMonth(), 1)
  const end = new Date(now.getFullYear(), now.getMonth() + 1, 0)
  return { start: localDate(start), end: localDate(end) }
}

function localDate(value: Date) {
  return `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, "0")}-${String(value.getDate()).padStart(2, "0")}`
}

export function accountTypeLabel(value: string) {
  return ({ cash: "现金", bank: "银行卡", e_wallet: "电子钱包", huabei: "花呗", baitiao: "白条", douyin_monthly: "抖音月付", credit_card: "信用卡", investment: "投资账户", other: "其他" } as Record<string, string>)[value] ?? value
}

export function categoryDirectionLabel(value: string) {
  return value === "income" ? "收入" : value === "expense" ? "支出" : "收支"
}
