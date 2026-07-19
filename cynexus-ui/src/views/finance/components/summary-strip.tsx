import { ArrowDownLeft, ArrowUpRight, Scale } from "lucide-react"

import { cn } from "@/lib/utils"
import type { FinanceOverview } from "@/services/finance/ledger"
import { formatMoney } from "../lib/format"

export function FinanceSummaryStrip({ overview }: { overview: FinanceOverview | null }) {
  const items = [
    { label: "收入", value: overview?.income, detail: "已入账", icon: ArrowDownLeft, tone: "text-emerald-600 dark:text-emerald-400", bar: "bg-emerald-500" },
    { label: "支出", value: overview?.expense, detail: "已入账", icon: ArrowUpRight, tone: "text-rose-600 dark:text-rose-400", bar: "bg-rose-500" },
    { label: "结余", value: overview?.net, detail: "收入 - 支出", icon: Scale, tone: (overview?.net ?? 0) >= 0 ? "text-sky-600 dark:text-sky-400" : "text-amber-600 dark:text-amber-400", bar: (overview?.net ?? 0) >= 0 ? "bg-sky-500" : "bg-amber-500" },
  ]
  return <section aria-label="本期概览" className="overflow-hidden rounded-xl border border-border/70 bg-card"><div className="flex items-center justify-between border-b border-border/60 px-4 py-3"><div><h2 className="text-sm font-semibold">本期概览</h2><p className="mt-0.5 text-xs text-muted-foreground">只统计已入账的收入和支出，转账不计入结余。</p></div><span className="text-xs text-muted-foreground">CNY</span></div><div className="grid md:grid-cols-3 md:divide-x md:divide-border/60">{items.map(({ label, value, detail, icon: Icon, tone, bar }) => <div key={label} className="relative px-4 py-4 first:pt-4 last:pb-4 md:py-5"><div className="flex items-center gap-2 text-xs text-muted-foreground"><Icon className={cn("size-4", tone)} />{label}</div><div className={cn("mt-2 text-2xl font-semibold tabular-nums tracking-tight", tone)}>{formatMoney(value)}</div><div className="mt-1 text-xs text-muted-foreground">{detail}</div><div className="absolute inset-x-4 bottom-0 h-0.5 rounded-full bg-muted md:inset-x-4"><div className={cn("h-full w-1/3 rounded-full", bar)} /></div></div>)}</div></section>
}
