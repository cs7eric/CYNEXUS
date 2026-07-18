import { useCallback, useEffect, useState } from "react"

import { WorkspaceShell } from "@/layout/workspace-shell"
import { notification } from "@/components/ui/notification"
import { Skeleton } from "@/components/ui/skeleton"
import { fetchMarketEtf, type MarketEtfResponse } from "@/services/market/etf"
import { MarketEtfHeader } from "./components/page-header"
import { EtfSummaryCards } from "./components/summary-cards"
import { EtfTrendChart } from "./components/trend-chart"
import { EtfBreadthPanel } from "./components/breadth-panel"
import { RotationTable } from "./components/rotation-table"
import { EtfTable } from "./components/etf-table"
import { EtfMappingPanel } from "./components/mapping-panel"

function LoadingBlock({ className = "h-40" }: { className?: string }) {
  return <Skeleton className={className} />
}

export default function MarketEtfPage() {
  const [data, setData] = useState<MarketEtfResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    setData(null)
    try {
      const response = await fetchMarketEtf({ historyDays: 10, rowLimit: 180 })
      if (!response.ok) throw new Error(response.error || "ETF 数据为空")
      setData(response)
    } catch (cause) {
      const message = cause instanceof Error ? cause.message : String(cause)
      setError(message)
      notification.danger({ title: "ETF 市场数据加载失败", description: message })
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    const timer = window.setTimeout(() => { void load() }, 0)
    return () => window.clearTimeout(timer)
  }, [load])

  return (
    <WorkspaceShell sectionLabel="market" pageTitle="market-etf">
      <MarketEtfHeader tradeDate={data?.tradeDate} fetchedAt={data?.fetchedAt} source={data?.source} loading={loading} onRefresh={() => void load()} />
      {error && !loading ? <div className="rounded-lg border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700 dark:border-rose-900/50 dark:bg-rose-950/20 dark:text-rose-300">{error}</div> : null}
      {loading ? (
        <div className="space-y-4"><div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">{Array.from({ length: 4 }, (_, index) => <LoadingBlock key={index} className="h-32" />)}</div><div className="grid gap-4 xl:grid-cols-[minmax(0,1.7fr)_minmax(300px,0.8fr)]"><LoadingBlock className="h-[390px]" /><LoadingBlock className="h-[390px]" /></div><LoadingBlock className="h-[390px]" /><div className="grid gap-4 xl:grid-cols-2"><LoadingBlock className="h-[360px]" /><LoadingBlock className="h-[360px]" /></div></div>
      ) : data ? (
        <div className="min-w-0 space-y-4 overflow-hidden">
          <EtfSummaryCards summary={data.summary} />
          <div className="grid min-w-0 gap-4 xl:grid-cols-[minmax(0,1.7fr)_minmax(300px,0.8fr)]"><EtfTrendChart data={data.history} loading={false} /><EtfBreadthPanel breadth={data.breadth} summary={data.summary} /></div>
          <RotationTable rows={data.rotations} loading={false} />
          <EtfMappingPanel mapping={data.mapping ?? null} />
          <div className="grid min-w-0 gap-4 xl:grid-cols-2"><EtfTable title="宽基 ETF" description="宽基指数的成交、价格、折溢价和份额变化" rows={data.broad} loading={false} /><EtfTable title="行业 ETF" description="行业主题的成交活跃度与资金轮动线索" rows={data.sectorRows} loading={false} /></div>
          <div className="border-t border-border/60 pt-3 text-xs leading-5 text-muted-foreground">数据口径：份额变化 = 当日总份额 - 前一交易日总份额；申赎金额代理 = 份额变化 × IOPV。免费行情源不提供完整的申购/赎回毛数据，主力净流入仅作为二级市场分类资金参考。ETF 行情分类覆盖：{data.summary?.classificationAuthorityCount ?? "—"} 只来自基金/指数目录，{data.summary?.classificationFallbackCount ?? "—"} 只使用名称规则兜底。</div>
        </div>
      ) : <div className="rounded-lg bg-muted/35 px-4 py-12 text-center text-sm text-muted-foreground">暂无 ETF 市场数据</div>}
    </WorkspaceShell>
  )
}
