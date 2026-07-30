import { CheckCircle2, CircleAlert, Clock3, Database, XCircle } from "lucide-react"

import { Badge } from "@/components/ui/badge"
import type { FuturesPositionCoverage } from "@/services/market/futures-position"
import { formatDateTime } from "../lib/format"

function StatusIcon({ status }: { status: string }) {
  if (status === "SUCCEEDED") return <CheckCircle2 className="size-4 text-emerald-600 dark:text-emerald-400" />
  if (status === "PARTIAL" || status === "EMPTY") return <CircleAlert className="size-4 text-amber-600 dark:text-amber-400" />
  if (status === "RUNNING") return <Clock3 className="size-4 text-sky-600 dark:text-sky-400" />
  return <XCircle className="size-4 text-rose-600 dark:text-rose-400" />
}

interface CoveragePanelProps {
  coverage?: FuturesPositionCoverage | null
}

export function CoveragePanel({ coverage }: CoveragePanelProps) {
  const items = coverage?.items ?? []
  return (
    <section className="min-w-0 rounded-lg border border-border/70 bg-card" aria-label="数据覆盖">
      <div className="flex items-center justify-between gap-3 border-b border-border/60 px-4 py-3"><div><h2 className="text-sm font-semibold">来源覆盖</h2><p className="mt-1 text-xs text-muted-foreground">只展示当前交易日的抓取状态，空数据不会覆盖已有快照。</p></div><Database className="size-4 text-muted-foreground" /></div>
      <div className="divide-y divide-border/60">
        {items.length > 0 ? items.map((item) => <div key={`${item.exchangeCode}-${item.provider}`} className="flex items-center gap-3 px-4 py-2.5 text-xs">
          <StatusIcon status={item.status} />
          <span className="w-12 shrink-0 font-mono font-semibold">{item.exchangeCode}</span>
          <span className="min-w-0 flex-1 truncate text-muted-foreground">{item.provider}</span>
          <span className="font-mono tabular-nums text-muted-foreground">{item.actualInstrumentCount}/{item.expectedInstrumentCount || "-"}</span>
          {item.selected ? <Badge variant="secondary" className="h-5 px-1.5 text-[10px]">已选</Badge> : <span className="w-7" />}
          <span className="hidden w-20 text-right text-muted-foreground sm:inline">{formatDateTime(item.completedAt)}</span>
        </div>) : <div className="flex flex-col items-center gap-2 px-4 py-10 text-center text-sm text-muted-foreground"><Database className="size-5" />暂无覆盖记录</div>}
      </div>
    </section>
  )
}
