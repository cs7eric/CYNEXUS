import type { PortfolioPerformancePoint, PortfolioPerformanceRange } from "@/services/market/portfolio/types"

interface PortfolioIncomeCalendarProps {
  points: PortfolioPerformancePoint[]
  range: PortfolioPerformanceRange
}

export function PortfolioIncomeCalendar({ points, range }: PortfolioIncomeCalendarProps) {
  if (range !== "daily") return <PortfolioPeriodGrid points={points} range={range} />

  const lastDate = parseDate(points.at(-1)?.tradeDate ?? new Date().toISOString().slice(0, 10))
  const monthStart = new Date(lastDate.getFullYear(), lastDate.getMonth(), 1)
  const monthEnd = new Date(lastDate.getFullYear(), lastDate.getMonth() + 1, 0)
  const gridStart = new Date(monthStart)
  gridStart.setDate(monthStart.getDate() - gridStart.getDay())
  const gridEnd = new Date(monthEnd)
  gridEnd.setDate(monthEnd.getDate() + (6 - gridEnd.getDay()))
  const pointsByDate = new Map(points.map((point) => [point.tradeDate, point]))
  const days: Date[] = []
  for (const cursor = new Date(gridStart); cursor <= gridEnd; cursor.setDate(cursor.getDate() + 1)) days.push(new Date(cursor))
  const maxAbs = Math.max(...points.map((point) => Math.abs(Number(point.periodPnl ?? 0))), 1)
  const periodLabel = range === "daily" ? "日收益" : range === "weekly" ? "周收益" : range === "monthly" ? "月收益" : range === "yearly" ? "年收益" : "阶段收益"

  return <div className="p-3 sm:p-4">
    <div className="mb-3 flex items-center justify-between gap-3"><div><p className="text-sm font-semibold">{lastDate.getFullYear()}年{lastDate.getMonth() + 1}月</p><p className="mt-0.5 text-[11px] text-muted-foreground">按交易日展示{periodLabel}，空白日期表示无可用行情。</p></div><div className="flex items-center gap-2 text-[10px] text-muted-foreground"><span>亏损</span><span className="size-3 rounded-sm bg-emerald-500" /><span className="size-3 rounded-sm bg-muted" /><span className="size-3 rounded-sm bg-rose-500" /><span>盈利</span></div></div>
    <div className="grid grid-cols-7 gap-1.5 text-center text-[10px] text-muted-foreground">{["日", "一", "二", "三", "四", "五", "六"].map((label) => <span key={label} className="pb-1">{label}</span>)}
      {days.map((date) => {
        const key = formatDate(date)
        const point = pointsByDate.get(key)
        const value = Number(point?.periodPnl ?? 0)
        const inMonth = date.getMonth() === lastDate.getMonth()
        const tone = point ? cellTone(value, maxAbs) : "bg-muted/40 text-muted-foreground"
        return <div key={key} title={point ? `${key} ${signed(value)}` : key} className={`flex min-h-12 flex-col items-center justify-center rounded-sm border border-transparent px-1 py-1 sm:min-h-14 ${tone} ${inMonth ? "opacity-100" : "opacity-35"}`}><span className="text-[10px]">{date.getDate()}</span>{point ? <span className="mt-0.5 font-mono text-[10px] font-semibold tabular-nums">{signed(value)}</span> : <span className="mt-0.5 text-[10px]">-</span>}</div>
      })}
    </div>
  </div>
}

function PortfolioPeriodGrid({ points, range }: PortfolioIncomeCalendarProps) {
  const maxAbs = Math.max(...points.map((point) => Math.abs(Number(point.periodPnl ?? 0))), 1)
  const periodLabel = range === "weekly" ? "周收益" : range === "monthly" ? "月收益" : "年收益"
  return <div className="p-3 sm:p-4">
    <div className="mb-3 flex items-center justify-between gap-3"><div><p className="text-sm font-semibold">{periodLabel}</p><p className="mt-0.5 text-[11px] text-muted-foreground">按{range === "weekly" ? "周" : range === "monthly" ? "月" : "年"}聚合显示，金额为该周期收益。</p></div><div className="flex items-center gap-2 text-[10px] text-muted-foreground"><span>亏损</span><span className="size-3 rounded-sm bg-emerald-500" /><span className="size-3 rounded-sm bg-muted" /><span className="size-3 rounded-sm bg-rose-500" /><span>盈利</span></div></div>
    <div className="grid grid-cols-2 gap-2 sm:grid-cols-3 md:grid-cols-4">{points.map((point) => {
      const value = Number(point.periodPnl ?? 0)
      const label = periodLabelFor(point.tradeDate, range)
      const tone = cellTone(value, maxAbs)
      return <div key={`${range}-${point.tradeDate}`} title={`${label} ${signed(value)}`} className={`flex min-h-20 flex-col items-center justify-center rounded-sm px-2 py-2 ${tone}`}><span className="text-[11px]">{label}</span><span className="mt-1 font-mono text-sm font-semibold tabular-nums">{signed(value)}</span></div>
    })}</div>
  </div>
}

function cellTone(value: number, maxAbs: number) {
  const ratio = Math.min(Math.abs(value) / maxAbs, 1)
  if (value > 0) return ratio > 0.66 ? "bg-rose-500 text-white" : ratio > 0.33 ? "bg-rose-200 text-rose-900" : "bg-rose-100 text-rose-800"
  if (value < 0) return ratio > 0.66 ? "bg-emerald-500 text-white" : ratio > 0.33 ? "bg-emerald-200 text-emerald-900" : "bg-emerald-100 text-emerald-800"
  return "bg-muted text-muted-foreground"
}

function parseDate(value: string) {
  const [year, month, day] = value.split("-").map(Number)
  return new Date(year, (month || 1) - 1, day || 1)
}

function formatDate(value: Date) {
  return `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, "0")}-${String(value.getDate()).padStart(2, "0")}`
}

function periodLabelFor(value: string, range: PortfolioPerformanceRange) {
  const date = parseDate(value)
  if (range === "weekly") {
    const start = new Date(date)
    const end = new Date(start)
    end.setDate(end.getDate() + 6)
    return `${compactDate(start)}-${compactDate(end)}`
  }
  if (range === "monthly") {
    const start = new Date(date.getFullYear(), date.getMonth(), 1)
    const end = new Date(date.getFullYear(), date.getMonth() + 1, 0)
    return `${compactDate(start)}-${compactDate(end)}`
  }
  return `${date.getFullYear()}年`
}

function compactDate(value: Date) {
  return `${String(value.getMonth() + 1).padStart(2, "0")}${String(value.getDate()).padStart(2, "0")}`
}

function signed(value: number) {
  return `${value > 0 ? "+" : ""}${value.toFixed(2)}`
}
