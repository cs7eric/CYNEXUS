import { CalendarDays } from "lucide-react"

import type { FinanceOverview } from "@/services/finance/ledger"
import { cn } from "@/lib/utils"

type ActivityDay = {
  date: string
  activityCount: number
  expense: number
  income: number
}

export function DailyActivityHeatmap({ overview, loading = false }: { overview: FinanceOverview | null; loading?: boolean }) {
  if (loading || !overview) return <DailyActivitySkeleton />
  const days = buildDays(overview)
  const activeDays = days.filter((item) => item.activityCount > 0)
  const totalActivity = activeDays.reduce((sum, item) => sum + item.activityCount, 0)
  const maxActivity = Math.max(...days.map((item) => item.activityCount), 0)
  const start = parseDate(overview.startDate)
  const end = parseDate(overview.endDate)
  const gridStart = new Date(start)
  gridStart.setDate(gridStart.getDate() - gridStart.getDay())
  const gridEnd = new Date(end)
  gridEnd.setDate(gridEnd.getDate() + (6 - gridEnd.getDay()))
  const weekCount = Math.max(1, Math.ceil((calendarDayNumber(gridEnd) - calendarDayNumber(gridStart) + 1) / 7))
  const byDate = new Map(days.map((item) => [item.date, item]))
  const weeks = Array.from({ length: weekCount }, (_, week) => Array.from({ length: 7 }, (_, day) => {
    const date = new Date(gridStart)
    date.setDate(gridStart.getDate() + week * 7 + day)
    return byDate.get(formatDate(date)) ?? null
  }))
  const months = weeks.map((_, index) => {
    const date = new Date(gridStart)
    date.setDate(gridStart.getDate() + index * 7)
    const visibleDate = index === 0 && start > date ? start : date
    return index === 0 || visibleDate.getDate() <= 7 ? String(visibleDate.getMonth() + 1) + "月" : ""
  })

  return <section className="min-w-0 overflow-hidden rounded-xl border border-border/70 bg-card">
    <div className="flex flex-wrap items-start justify-between gap-3 border-b border-border/60 px-4 py-3">
      <div className="flex min-w-0 items-start gap-2.5"><CalendarDays className="mt-0.5 size-4 shrink-0 text-emerald-600 dark:text-emerald-400" /><div><h2 className="text-sm font-semibold">日活脉搏</h2><p className="mt-0.5 text-xs text-muted-foreground">按每天发生的记账笔数观察财务记录节奏。</p></div></div>
      <div className="text-right text-xs text-muted-foreground"><span className="hidden sm:inline">过去 12 个月 · </span><span className="font-mono font-medium text-foreground tabular-nums">{totalActivity}</span> 笔 · <span className="font-mono font-medium text-foreground tabular-nums">{activeDays.length}</span> 个活跃日</div>
    </div>
    <div className="overflow-x-auto px-4 py-5">
      <div className={cn(weekCount > 12 ? "min-w-[720px]" : "min-w-[360px]", "w-full")}>
        <div className="grid grid-cols-[28px_minmax(0,1fr)] gap-x-2">
          <div />
          <div className="grid h-5 gap-1 text-[10px] text-muted-foreground" style={{ gridTemplateColumns: `repeat(${weekCount}, minmax(10px, 1fr))` }}>{months.map((label, index) => <span key={label + "-" + index} className="truncate">{label}</span>)}</div>
          <div className="grid grid-rows-7 gap-1 pr-1 text-[10px] leading-[10px] text-muted-foreground"><span>日</span><span /><span>二</span><span /><span>四</span><span /><span>六</span></div>
          <div role="grid" aria-label={`${overview.startDate} 至 ${overview.endDate} 的每日记账活动`} className="grid grid-flow-col grid-rows-7 gap-1">
            {weeks.flatMap((week, weekIndex) => week.map((item, dayIndex) => {
              const date = new Date(gridStart)
              date.setDate(gridStart.getDate() + weekIndex * 7 + dayIndex)
              const cell = item && item.date === formatDate(date) ? item : null
              const label = cell ? cell.date + "，" + cell.activityCount + " 笔记账，支出 " + money(cell.expense) : undefined
              return <span key={formatDate(date)} role="gridcell" aria-label={label} title={label} className={cn("size-3 rounded-[3px] border border-transparent bg-muted/60 sm:size-3.5", cell && activityTone(cell.activityCount, maxActivity), !cell && "opacity-35")} /> 
            }))}
          </div>
        </div>
        <div className="mt-4 flex items-center justify-end gap-2 text-[10px] text-muted-foreground"><span>少</span><span className="size-3 rounded-[3px] bg-muted/60" /><span className="size-3 rounded-[3px] bg-emerald-200 dark:bg-emerald-950" /><span className="size-3 rounded-[3px] bg-emerald-400" /><span className="size-3 rounded-[3px] bg-emerald-600" /><span>多</span></div>
      </div>
    </div>
  </section>
}

function DailyActivitySkeleton() {
  return <section className="min-w-0 overflow-hidden rounded-xl border border-border/70 bg-card"><div className="flex items-center justify-between border-b border-border/60 px-4 py-3"><div><div className="h-4 w-20 animate-pulse rounded bg-muted" /><div className="mt-2 h-3 w-56 animate-pulse rounded bg-muted/70" /></div><div className="h-3 w-28 animate-pulse rounded bg-muted/70" /></div><div className="overflow-hidden px-4 py-5"><div className="h-28 animate-pulse rounded-md bg-muted/45" /></div></section>
}

function buildDays(overview: FinanceOverview): ActivityDay[] {
  const start = parseDate(overview.startDate)
  const end = parseDate(overview.endDate)
  const byDate = new Map(overview.daily.map((item) => [item.date, item]))
  const days: ActivityDay[] = []
  for (const cursor = new Date(start); cursor <= end; cursor.setDate(cursor.getDate() + 1)) {
    const date = formatDate(cursor)
    const item = byDate.get(date)
    days.push({ date, activityCount: item?.activityCount ?? 0, expense: item?.expense ?? 0, income: item?.income ?? 0 })
  }
  return days
}

function activityTone(count: number, max: number) {
  if (!count) return "bg-muted/60"
  const ratio = count / Math.max(max, 1)
  if (ratio <= 0.25) return "bg-emerald-200 dark:bg-emerald-950"
  if (ratio <= 0.5) return "bg-emerald-300 dark:bg-emerald-800"
  if (ratio <= 0.75) return "bg-emerald-500 dark:bg-emerald-600"
  return "bg-emerald-700 dark:bg-emerald-400"
}

function parseDate(value: string) {
  const [year, month, day] = value.split("-").map(Number)
  return new Date(year, (month || 1) - 1, day || 1)
}

function formatDate(value: Date) {
  return value.getFullYear() + "-" + String(value.getMonth() + 1).padStart(2, "0") + "-" + String(value.getDate()).padStart(2, "0")
}

function calendarDayNumber(value: Date) {
  return Math.floor(Date.UTC(value.getFullYear(), value.getMonth(), value.getDate()) / 86_400_000)
}

function money(value: number) {
  return value.toLocaleString("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
