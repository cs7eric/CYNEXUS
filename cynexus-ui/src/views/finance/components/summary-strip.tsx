import { ArrowDownLeft, ArrowUpRight, Scale } from "lucide-react"

import { cn } from "@/lib/utils"
import type { FinanceOverview } from "@/services/finance/ledger"
import { formatMoney } from "../lib/format"

export function FinanceSummaryStrip({ overview }: { overview: FinanceOverview | null }) {
  const items = [
    { label: "本期收入", value: overview?.income, icon: ArrowDownLeft, tone: "text-emerald-600 dark:text-emerald-400", detail: "已入账收入" },
    { label: "本期支出", value: overview?.expense, icon: ArrowUpRight, tone: "text-rose-600 dark:text-rose-400", detail: "已入账支出" },
    { label: "本期结余", value: overview?.net, icon: Scale, tone: (overview?.net ?? 0) >= 0 ? "text-sky-600 dark:text-sky-400" : "text-amber-600 dark:text-amber-400", detail: "收入 - 支出" },
  ]
  return <div className="grid gap-px overflow-hidden rounded-lg border border-border/70 bg-border/70 md:grid-cols-3">{items.map(({ label, value, icon: Icon, tone, detail }) => <div key={label} className="bg-card px-4 py-4"><div className="flex items-center gap-2 text-xs text-muted-foreground"><Icon className={cn("size-4", tone)} />{label}</div><div className={cn("mt-2 text-2xl font-semibold tabular-nums", tone)}>{formatMoney(value)}</div><div className="mt-1 text-[11px] text-muted-foreground">{detail} · CNY</div></div>)}</div>
}
