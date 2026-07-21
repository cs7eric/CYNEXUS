import { Bar, CartesianGrid, ComposedChart, Line, XAxis, YAxis } from "recharts"

import type { FinanceOverview } from "@/services/finance/ledger"
import { ChartContainer, ChartTooltip, ChartTooltipContent, type ChartConfig } from "@/components/ui/chart"
import { formatMoney } from "../lib/format"

const chartConfig = {
  income: { label: "收入", color: "hsl(142 71% 45%)" },
  expense: { label: "支出", color: "hsl(350 80% 60%)" },
  net: { label: "净流入", color: "hsl(199 89% 48%)" },
} satisfies ChartConfig

export function CashFlowChart({ overview }: { overview: FinanceOverview }) {
  const activeData = overview.daily.filter((item) => item.income > 0 || item.expense > 0).map((item) => ({ ...item, net: item.income - item.expense }))
  const peak = activeData.reduce<(typeof activeData)[number] | null>((current, item) => !current || Math.max(item.income, item.expense) > Math.max(current.income, current.expense) ? item : current, null)

  return <section className="min-w-0 overflow-hidden rounded-xl border border-border/70 bg-card">
    <div className="flex items-start justify-between gap-3 border-b border-border/60 px-4 py-3"><div><h2 className="text-sm font-semibold">现金流趋势</h2><p className="mt-0.5 text-xs text-muted-foreground">只展示实际发生过账目的日期，避免稀疏数据撑空图表。</p></div><div className="shrink-0 text-right"><div className={overview.net >= 0 ? "font-mono text-sm font-semibold tabular-nums text-emerald-600 dark:text-emerald-400" : "font-mono text-sm font-semibold tabular-nums text-rose-600 dark:text-rose-400"}>{formatMoney(overview.net)}</div><div className="text-[10px] text-muted-foreground">周期净流入</div></div></div>
    <div className="grid grid-cols-3 divide-x divide-border/60 border-b border-border/60"><MiniMetric label="收入" value={formatMoney(overview.income)} tone="text-emerald-600 dark:text-emerald-400" /><MiniMetric label="支出" value={formatMoney(overview.expense)} tone="text-rose-600 dark:text-rose-400" /><MiniMetric label="发生日" value={activeData.length + " 天"} tone="text-sky-600 dark:text-sky-400" /></div>
    {activeData.length ? <div className="px-2 pb-4 pt-4 sm:px-4"><ChartContainer config={chartConfig} className="h-[190px] w-full"><ComposedChart data={activeData} margin={{ top: 8, right: 8, left: -16, bottom: 0 }}><CartesianGrid vertical={false} /><XAxis dataKey="date" tickLine={false} axisLine={false} tickMargin={8} minTickGap={20} tickFormatter={(value) => String(value).slice(5)} /><YAxis tickLine={false} axisLine={false} width={50} tickFormatter={(value) => Number(value).toLocaleString("zh-CN", { notation: "compact", maximumFractionDigits: 1 })} /><ChartTooltip content={<ChartTooltipContent labelFormatter={(value) => String(value)} formatter={(value) => formatMoney(Number(value ?? 0))} />} /><Bar dataKey="income" fill="var(--color-income)" radius={[3, 3, 0, 0]} maxBarSize={16} /><Bar dataKey="expense" fill="var(--color-expense)" radius={[3, 3, 0, 0]} maxBarSize={16} /><Line dataKey="net" type="monotone" stroke="var(--color-net)" strokeWidth={2} dot={{ r: 3, fill: "var(--color-net)", strokeWidth: 0 }} /></ComposedChart></ChartContainer><div className="mt-2 flex items-center justify-between gap-3 text-[10px] text-muted-foreground"><span>{activeData.length === 1 ? "1 个发生日" : "共 " + activeData.length + " 个发生日"}</span><span>{peak ? "最高单日 " + peak.date + " · " + formatMoney(Math.max(peak.income, peak.expense)) : ""}</span></div></div> : <div className="flex min-h-32 items-center justify-center px-4 text-center text-sm text-muted-foreground">这个周期还没有可绘制的现金流。</div>}
  </section>
}

function MiniMetric({ label, value, tone }: { label: string; value: string; tone: string }) {
  return <div className="min-w-0 px-3 py-2.5 sm:px-4"><div className="text-[10px] text-muted-foreground">{label}</div><div className={"mt-1 truncate font-mono text-xs font-semibold tabular-nums " + tone}>{value}</div></div>
}
