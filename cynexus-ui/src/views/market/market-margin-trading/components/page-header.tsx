import { RefreshCw, Scale } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { formatDate, formatDateTime } from "../lib/format"

interface MarginTradingHeaderProps {
  tradeDate?: string | null
  updatedAt?: string | null
  loading?: boolean
  onRefresh: () => void
}

export function MarginTradingHeader({ tradeDate, updatedAt, loading, onRefresh }: MarginTradingHeaderProps) {
  return (
    <div className="min-w-0 overflow-hidden flex flex-col gap-3 md:flex-row md:items-end md:justify-between">
      <div className="min-w-0 space-y-2">
        <div className="flex flex-wrap items-center gap-2">
          <Badge variant="outline" className="bg-sky-50 text-sky-700 dark:bg-sky-950/30 dark:text-sky-300">
            <Scale className="size-3" />
            market-margin-trading
          </Badge>
          <span className="text-xs text-muted-foreground">更新 {formatDateTime(updatedAt)}</span>
        </div>
        <div>
          <h1 className="text-2xl font-semibold tracking-tight text-foreground md:text-3xl">两融交易监控</h1>
          <p className="mt-1 max-w-3xl text-sm text-muted-foreground">
            最新交易日 {formatDate(tradeDate)}，跟踪全市场余额、交易所口径与个股融资融券排名。
          </p>
        </div>
      </div>
      <Button onClick={onRefresh} disabled={loading} className="w-fit gap-2">
        <RefreshCw className={loading ? "size-4 animate-spin" : "size-4"} />
        刷新
      </Button>
    </div>
  )
}
