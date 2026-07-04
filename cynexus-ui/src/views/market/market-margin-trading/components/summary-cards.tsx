import { ArrowDownRight, ArrowUpRight, Landmark, WalletCards } from "lucide-react"

import type { MarginMarketSummary } from "@/services/market/margin"
import { cn } from "@/lib/utils"
import { formatYi } from "../lib/format"
import { summaryByExchange } from "../lib/derive"

interface SummaryCardsProps {
  summaries: MarginMarketSummary[]
  cnChange: number | null
}

function MetricCard({
  label,
  value,
  sub,
  tone,
  icon: Icon,
}: {
  label: string
  value: string
  sub: string
  tone: "blue" | "emerald" | "violet" | "slate"
  icon: typeof WalletCards
}) {
  return (
    <div
      className={cn(
        "min-h-[126px] min-w-0 overflow-hidden rounded-lg bg-muted/45 p-4",
        tone === "blue" && "bg-sky-50/80 dark:bg-sky-950/20",
        tone === "emerald" && "bg-emerald-50/80 dark:bg-emerald-950/20",
        tone === "violet" && "bg-violet-50/80 dark:bg-violet-950/20",
      )}
    >
      <div className="flex items-center justify-between gap-3">
        <span className="text-xs font-medium text-muted-foreground">{label}</span>
        <Icon className="size-4 text-muted-foreground" />
      </div>
      <div className="mt-5 truncate text-2xl font-semibold tabular-nums tracking-tight">{value}</div>
      <div className="mt-2 truncate text-xs text-muted-foreground">{sub}</div>
    </div>
  )
}

export function SummaryCards({ summaries, cnChange }: SummaryCardsProps) {
  const byExchange = summaryByExchange(summaries)
  const cn = byExchange.get("CN")
  const sse = byExchange.get("SSE")
  const szse = byExchange.get("SZSE")
  const isUp = (cnChange ?? 0) >= 0

  return (
    <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
      <MetricCard
        label="全市场两融余额"
        value={formatYi(cn?.marginBalance, 1)}
        sub={`融资余额 ${formatYi(cn?.financingBalance, 1)}`}
        tone="blue"
        icon={WalletCards}
      />
      <MetricCard
        label="单日融资买入"
        value={formatYi(cn?.financingBuyAmount, 1)}
        sub={`较上一交易日 ${cnChange == null ? "-" : `${isUp ? "+" : ""}${formatYi(cnChange, 1)}`}`}
        tone={isUp ? "emerald" : "slate"}
        icon={isUp ? ArrowUpRight : ArrowDownRight}
      />
      <MetricCard
        label="上交所余额"
        value={formatYi(sse?.marginBalance, 1)}
        sub={`融资买入 ${formatYi(sse?.financingBuyAmount, 1)}`}
        tone="violet"
        icon={Landmark}
      />
      <MetricCard
        label="深交所余额"
        value={formatYi(szse?.marginBalance, 1)}
        sub={`融资买入 ${formatYi(szse?.financingBuyAmount, 1)}`}
        tone="slate"
        icon={Landmark}
      />
    </div>
  )
}
