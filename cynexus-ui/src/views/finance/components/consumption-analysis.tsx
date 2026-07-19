import { CalendarRange, TrendingDown, TrendingUp } from "lucide-react"

import type { FinanceOverview } from "@/services/finance/ledger"
import { cn } from "@/lib/utils"
import { formatMoney } from "../lib/format"

export function ConsumptionAnalysis({ overview }: { overview: FinanceOverview }) {
  const daily = overview.daily.filter((item) => item.expense > 0 || item.income > 0)
  const peak = daily.reduce<(typeof daily)[number] | null>((current, item) => !current || item.expense > current.expense ? item : current, null)
  const average = daily.length ? overview.expense / daily.length : 0
  const expenseDays = daily.filter((item) => item.expense > 0).length
  const incomeDays = daily.filter((item) => item.income > 0).length
  const maxExpense = Math.max(...daily.map((item) => item.expense), 0)

  return <section className="min-w-0 overflow-hidden rounded-xl border border-border/70 bg-card">
    <div className="flex items-start justify-between gap-3 border-b border-border/60 px-4 py-3">
      <div><h2 className="text-sm font-semibold">消费分析</h2><p className="mt-0.5 text-xs text-muted-foreground">按天观察本期消费节奏，识别支出集中的日期。</p></div>
      <CalendarRange className="size-4 text-primary" />
    </div>
    <div className="grid gap-3 border-b border-border/60 px-4 py-4 sm:grid-cols-3">
      <Insight label="日均支出" value={formatMoney(average)} detail={expenseDays ? `${expenseDays} 天有支出` : "暂无支出"} />
      <Insight label="最高消费日" value={peak ? formatMoney(peak.expense) : "—"} detail={peak ? formatDay(peak.date) : "等待入账"} tone="text-rose-600 dark:text-rose-400" />
      <Insight label="有收入的日期" value={`${incomeDays} 天`} detail={overview.income ? `共 ${formatMoney(overview.income)}` : "暂无收入"} tone="text-emerald-600 dark:text-emerald-400" />
    </div>
    {daily.length ? <div className="px-4 py-4"><div className="flex h-44 items-end gap-1.5 sm:gap-2">{daily.map((item) => { const height = maxExpense ? Math.max(item.expense ? 6 : 2, (item.expense / maxExpense) * 100) : 2; return <div key={item.date} className="group flex min-w-0 flex-1 flex-col items-center justify-end gap-2" title={`${formatDay(item.date)} · 支出 ${formatMoney(item.expense)}`}><div className="relative flex h-32 w-full items-end justify-center"><div className={cn("w-full max-w-8 rounded-t-md bg-rose-400/75 transition-[height] duration-300 group-hover:bg-rose-500", item.expense === 0 && "bg-muted group-hover:bg-muted-foreground/30")} style={{ height: `${height}%` }} /></div><span className="max-w-full truncate text-[10px] text-muted-foreground">{formatDay(item.date)}</span></div> })}</div><div className="mt-3 flex items-center gap-4 text-[11px] text-muted-foreground"><span className="inline-flex items-center gap-1.5"><span className="size-2 rounded-sm bg-rose-400/75" />每日支出</span><span className="inline-flex items-center gap-1.5"><TrendingDown className="size-3" />共 {formatMoney(overview.expense)}</span><span className="ml-auto hidden items-center gap-1.5 sm:inline-flex"><TrendingUp className="size-3 text-emerald-600" />收入 {formatMoney(overview.income)}</span></div></div> : <div className="flex min-h-52 flex-col items-center justify-center px-4 text-center"><CalendarRange className="size-8 text-muted-foreground/40" /><p className="mt-3 text-sm font-medium">本期还没有消费记录</p><p className="mt-1 max-w-xs text-xs leading-5 text-muted-foreground">上传小票或账单图片，AI 解析后审核入账，这里会自动形成消费趋势。</p></div>}
  </section>
}

function Insight({ label, value, detail, tone = "text-foreground" }: { label: string; value: string; detail: string; tone?: string }) { return <div className="min-w-0"><div className="text-xs text-muted-foreground">{label}</div><div className={cn("mt-1 truncate text-lg font-semibold tabular-nums", tone)}>{value}</div><div className="mt-0.5 truncate text-[11px] text-muted-foreground">{detail}</div></div> }

function formatDay(value: string) { const parts = value.split("-"); return parts.length === 3 ? `${parts[1]}/${parts[2]}` : value }
