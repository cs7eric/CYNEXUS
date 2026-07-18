import { Activity, RefreshCw } from "lucide-react"

import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { formatDate, formatDateTime } from "../lib/format"

export function MarketEtfHeader({
  tradeDate,
  fetchedAt,
  source,
  loading,
  onRefresh,
}: {
  tradeDate?: string | null
  fetchedAt?: string | null
  source?: string | null
  loading: boolean
  onRefresh: () => void
}) {
  return (
    <div className="flex min-w-0 flex-col gap-3 overflow-hidden md:flex-row md:items-end md:justify-between">
      <div className="min-w-0 space-y-2">
        <div className="flex flex-wrap items-center gap-2">
          <Badge variant="outline" className="bg-cyan-50 text-cyan-700 dark:bg-cyan-950/30 dark:text-cyan-300">
            <Activity className="size-3" />
            market-etf
          </Badge>
          <span className="text-xs text-muted-foreground">更新 {formatDateTime(fetchedAt)}</span>
          <span className="text-xs text-muted-foreground">来源 {source || "—"}</span>
        </div>
        <div>
          <h1 className="text-2xl font-semibold tracking-tight md:text-3xl">ETF 市场方向</h1>
          <p className="mt-1 max-w-3xl text-sm text-muted-foreground">
            {formatDate(tradeDate)} · 用成交、份额变化、折溢价与市场广度观察宽基和行业资金轮动。
          </p>
        </div>
      </div>
      <Button type="button" onClick={onRefresh} disabled={loading} className="w-fit gap-2">
        <RefreshCw className={loading ? "size-4 animate-spin" : "size-4"} />
        刷新
      </Button>
    </div>
  )
}
