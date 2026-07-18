import { BarChart3, CircleDollarSign, Layers3, WalletCards } from "lucide-react"

import type { MarketEtfSummary } from "@/services/market/etf"
import { cn } from "@/lib/utils"
import { formatPct, formatWan } from "../lib/format"

function Metric({ label, value, sub, icon: Icon, tone }: { label: string; value: string; sub: string; icon: typeof WalletCards; tone: string }) {
  return (
    <div className={cn("min-h-[128px] min-w-0 overflow-hidden rounded-lg bg-muted/40 p-4", tone)}>
      <div className="flex items-center justify-between gap-3">
        <span className="text-xs font-medium text-muted-foreground">{label}</span>
        <Icon className="size-4 text-muted-foreground" />
      </div>
      <div className="mt-5 truncate text-2xl font-semibold tabular-nums tracking-tight">{value}</div>
      <div className="mt-2 truncate text-xs text-muted-foreground">{sub}</div>
    </div>
  )
}

export function EtfSummaryCards({ summary }: { summary: MarketEtfSummary | null }) {
  return (
    <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
      <Metric label="宽基份额净变化" value={formatWan(summary?.broadShareDelta, 1)} sub={`申赎金额代理 ${formatWan(summary?.broadNetSubscriptionProxy, 1)}`} icon={Layers3} tone="bg-sky-50/80 dark:bg-sky-950/20" />
      <Metric label="行业份额净变化" value={formatWan(summary?.sectorShareDelta, 1)} sub={`申赎金额代理 ${formatWan(summary?.sectorNetSubscriptionProxy, 1)}`} icon={WalletCards} tone="bg-emerald-50/80 dark:bg-emerald-950/20" />
      <Metric label="ETF 成交额" value={formatWan(summary?.totalAmount, 1)} sub={`${summary?.totalEtfs ?? "—"} 只纳入宽基/行业 ETF`} icon={CircleDollarSign} tone="bg-amber-50/80 dark:bg-amber-950/20" />
      <Metric label="平均折溢价率" value={formatPct(summary?.averagePremiumPct)} sub="现价相对 IOPV 的平均偏离" icon={BarChart3} tone="bg-violet-50/80 dark:bg-violet-950/20" />
    </div>
  )
}
