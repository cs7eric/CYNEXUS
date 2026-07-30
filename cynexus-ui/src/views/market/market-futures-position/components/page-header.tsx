import { DatabaseZap, RefreshCw } from "lucide-react"

import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { formatDate } from "../lib/format"

interface FuturesPositionPageHeaderProps {
  tradeDate?: string | null
  fallback?: boolean
  loading?: boolean
  onRefresh: () => void
}

export function FuturesPositionPageHeader({ tradeDate, fallback, loading, onRefresh }: FuturesPositionPageHeaderProps) {
  return (
    <header className="flex min-w-0 flex-col gap-3 md:flex-row md:items-end md:justify-between">
      <div className="min-w-0 space-y-2">
        <div className="flex flex-wrap items-center gap-2">
          <Badge variant="outline" className="bg-sky-50 text-sky-700 dark:bg-sky-950/30 dark:text-sky-300">
            <DatabaseZap className="size-3" />
            EOD member positions
          </Badge>
          <span className="text-xs text-muted-foreground">交易日 {formatDate(tradeDate)}</span>
          {fallback ? <Badge variant="secondary" className="text-amber-700 dark:text-amber-300">已回退到最近有效日</Badge> : null}
        </div>
        <div>
          <h1 className="text-2xl font-semibold tracking-tight text-foreground md:text-3xl">期货多空持仓监控</h1>
          <p className="mt-1 max-w-3xl text-sm leading-6 text-muted-foreground">
            查看交易所披露的会员成交量、多头和空头排名。数值是选定 Top-N 会员汇总，不代表全市场账户净持仓。
          </p>
        </div>
      </div>
      <Button type="button" variant="outline" onClick={onRefresh} disabled={loading} className="w-fit gap-2">
        <RefreshCw className={loading ? "size-4 animate-spin" : "size-4"} />
        刷新数据
      </Button>
    </header>
  )
}
