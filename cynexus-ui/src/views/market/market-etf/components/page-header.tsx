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
          <Badge variant="outline" className="bg-sky-50 text-sky-700 dark:bg-sky-950/30 dark:text-sky-300">
            <Activity className="size-3" />
            ETF 研究
          </Badge>
          <span className="text-xs text-muted-foreground">更新 {formatDateTime(fetchedAt)}</span>
          <span className="text-xs text-muted-foreground">来源 {source || "—"}</span>
        </div>
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">ETF 研究工作台</h1>
          <p className="mt-1 max-w-3xl text-sm text-muted-foreground">
            {formatDate(tradeDate)} · 从市场方向进入候选筛选，并在同一上下文内研究单只 ETF。
          </p>
        </div>
      </div>
      <Button type="button" variant="outline" onClick={onRefresh} disabled={loading} className="w-fit gap-2">
        <RefreshCw className={loading ? "size-4 animate-spin" : "size-4"} />
        刷新
      </Button>
    </div>
  )
}
