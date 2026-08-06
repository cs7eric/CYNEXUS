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
          <Badge variant="outline" className="bg-muted/45 text-muted-foreground">
            <DatabaseZap className="size-3" />
            会员持仓快照
          </Badge>
          <span className="text-xs text-muted-foreground">交易日 {formatDate(tradeDate)}</span>
          {fallback ? <Badge variant="secondary" className="text-amber-700 dark:text-amber-300">已回退到最近有效日</Badge> : null}
        </div>
        <div>
          <h1 className="text-2xl font-semibold tracking-tight text-foreground">多空持仓</h1>
          <p className="mt-1 max-w-3xl text-sm leading-6 text-muted-foreground">
            快速发现持仓异动，观察多空结构变化，并穿透到具体席位。
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
