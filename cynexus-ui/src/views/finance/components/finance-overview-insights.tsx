import { AlertTriangle, ArrowDownRight, CalendarClock, CheckCircle2, CircleDollarSign, Gauge, Layers3, Percent, ShieldCheck, TrendingUp } from "lucide-react"

import type { FinanceOverview } from "@/services/finance/ledger"
import { cn } from "@/lib/utils"
import { formatMoney } from "../lib/format"

export function FinanceOverviewInsights({ overview }: { overview: FinanceOverview }) {
  const daily = overview.daily.filter((item) => item.activityCount > 0)
  const busiest = daily.reduce<(typeof daily)[number] | null>((current, item) => !current || item.activityCount > current.activityCount ? item : current, null)
  const topExpense = overview.categories.filter((item) => item.direction === "expense").sort((a, b) => b.amount - a.amount)[0]
  const savingsRate = overview.income > 0 ? (overview.net / overview.income) * 100 : null
  const creditRatio = overview.assetTotal + overview.liabilityTotal > 0 ? (overview.liabilityTotal / (overview.assetTotal + overview.liabilityTotal)) * 100 : null
  const creditLimit = overview.accounts.filter((item) => item.nature === "credit").reduce((sum, item) => sum + (item.creditLimit ?? 0), 0)
  const creditUtilization = creditLimit > 0 ? (overview.liabilityTotal / creditLimit) * 100 : null
  const periodDays = calendarDays(overview.startDate, overview.endDate)
  const coverage = periodDays > 0 ? (daily.length / periodDays) * 100 : null
  const largestAsset = overview.accounts.filter((item) => item.nature === "asset").sort((a, b) => (b.balance ?? 0) - (a.balance ?? 0))[0]
  const assetConcentration = largestAsset && overview.assetTotal > 0 ? ((largestAsset.balance ?? 0) / overview.assetTotal) * 100 : null
  const averageDailyExpense = periodDays > 0 ? overview.expense / periodDays : null
  const expenseDays = daily.filter((item) => item.expense > 0)
  const peakExpenseDay = expenseDays.reduce<(typeof expenseDays)[number] | null>((current, item) => !current || item.expense > current.expense ? item : current, null)
  const assetCoverageDays = averageDailyExpense && averageDailyExpense > 0 ? overview.assetTotal / averageDailyExpense : null
  const healthy = savingsRate != null && savingsRate >= 0
  const signals = [
    { icon: busiest ? TrendingUp : CircleDollarSign, title: busiest ? "最活跃的一天" : "等待记账活动", detail: busiest ? busiest.date + " 记录 " + busiest.activityCount + " 笔" : "完成第一笔账目后，这里会显示节奏变化。", tone: "text-sky-600 dark:text-sky-400", dot: "bg-sky-500" },
    { icon: topExpense ? ArrowDownRight : CircleDollarSign, title: topExpense ? "支出集中在" : "支出结构", detail: topExpense ? topExpense.categoryName + " · " + formatMoney(topExpense.amount) + "（" + topExpense.percentage.toFixed(1) + "%）" : "这个周期还没有支出分类。", tone: "text-rose-600 dark:text-rose-400", dot: "bg-rose-500" },
    { icon: CircleDollarSign, title: "信用负债占比", detail: creditRatio == null ? "暂无资产或信用账户。" : creditRatio.toFixed(1) + "% · 当前待还 " + formatMoney(overview.liabilityTotal), tone: "text-violet-600 dark:text-violet-400", dot: "bg-violet-500" },
    { icon: Gauge, title: "信用额度使用", detail: creditUtilization == null ? "信用账户还没有设置额度。" : creditUtilization.toFixed(1) + "% · 总额度 " + formatMoney(creditLimit), tone: "text-orange-600 dark:text-orange-400", dot: "bg-orange-500" },
    { icon: Percent, title: "记账覆盖率", detail: coverage == null ? "无法计算统计周期。" : coverage.toFixed(1) + "% · " + daily.length + " / " + periodDays + " 天有记录", tone: "text-cyan-600 dark:text-cyan-400", dot: "bg-cyan-500" },
    { icon: Layers3, title: "资产集中度", detail: assetConcentration == null ? "暂无资产账户。" : largestAsset?.name + " 占资产 " + assetConcentration.toFixed(1) + "%", tone: "text-indigo-600 dark:text-indigo-400", dot: "bg-indigo-500" },
    { icon: CalendarClock, title: "日均支出", detail: averageDailyExpense == null ? "暂无支出记录。" : formatMoney(averageDailyExpense) + " · 按统计周期日历天数计算", tone: "text-teal-600 dark:text-teal-400", dot: "bg-teal-500" },
    { icon: ArrowDownRight, title: "单日支出峰值", detail: peakExpenseDay == null ? "暂无支出记录。" : peakExpenseDay.date + " · " + formatMoney(peakExpenseDay.expense), tone: "text-red-600 dark:text-red-400", dot: "bg-red-500" },
    { icon: ShieldCheck, title: "资产支出缓冲", detail: assetCoverageDays == null ? "暂无支出记录，暂不估算。" : assetCoverageDays >= 3650 ? "超过 10 年 · 按周期日均支出估算" : Math.floor(assetCoverageDays) + " 天 · 按周期日均支出估算", tone: "text-emerald-600 dark:text-emerald-400", dot: "bg-emerald-500" },
  ]

  return <section className="min-w-0 overflow-hidden rounded-xl border border-border/70 bg-card">
    <div className="border-b border-border/60 px-4 py-3"><div className="flex items-center justify-between gap-3"><div><h2 className="text-sm font-semibold">财务信号</h2><p className="mt-0.5 text-xs text-muted-foreground">从账目节奏和资产负债结构提取的可读结论。</p></div><span className={cn("rounded-full px-2 py-1 text-[11px] font-medium", healthy ? "bg-emerald-500/10 text-emerald-700 dark:text-emerald-300" : "bg-amber-500/10 text-amber-700 dark:text-amber-300")}>{healthy ? "现金流为正" : "需要关注"}</span></div></div>
    <div className="px-4 py-4">
      <div className="flex items-start gap-3"><span className={cn("flex size-9 shrink-0 items-center justify-center rounded-lg", healthy ? "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400" : "bg-amber-500/10 text-amber-600 dark:text-amber-400")}>{healthy ? <CheckCircle2 className="size-5" /> : <AlertTriangle className="size-5" />}</span><div className="min-w-0"><div className="text-xs font-medium text-muted-foreground">收入结余率</div><div className={cn("mt-1 text-2xl font-semibold tracking-tight tabular-nums", healthy ? "text-emerald-600 dark:text-emerald-400" : "text-amber-600 dark:text-amber-400")}>{savingsRate == null ? "—" : savingsRate.toFixed(1) + "%"}</div><p className="mt-1 text-xs leading-5 text-muted-foreground">{savingsRate == null ? "暂无收入，暂不计算。" : healthy ? "当前收入能够覆盖支出，周期结余 " + formatMoney(overview.net) + "。" : "支出已经超过收入，周期差额 " + formatMoney(Math.abs(overview.net)) + "。"}</p></div></div>
    </div>
    <div className="grid border-t border-border/60 sm:grid-cols-2 lg:grid-cols-3">
      {signals.map(({ icon: Icon, title, detail, tone, dot }, index) => <div key={title} className={cn("flex min-w-0 items-start gap-2.5 px-4 py-3.5", index > 0 && "border-t border-border/60 sm:border-l", index === 2 && "sm:border-l-0 lg:border-l", index === 3 && "lg:border-l-0")}><span className={cn("mt-1 size-1.5 shrink-0 rounded-full", dot)} /><div className="min-w-0"><div className="flex items-center gap-1.5 text-xs font-medium"><Icon className={cn("size-3.5", tone)} />{title}</div><div className="mt-1 truncate text-xs leading-5 text-muted-foreground">{detail}</div></div></div>)}
    </div>
  </section>
}

function calendarDays(start: string, end: string) {
  const [startYear, startMonth, startDay] = start.split("-").map(Number)
  const [endYear, endMonth, endDay] = end.split("-").map(Number)
  const startTime = Date.UTC(startYear, startMonth - 1, startDay)
  const endTime = Date.UTC(endYear, endMonth - 1, endDay)
  return Math.max(0, Math.floor((endTime - startTime) / 86_400_000) + 1)
}
