import { useCallback, useEffect, useState } from "react"
import { AlertCircle, Database } from "lucide-react"

import { Button } from "@/components/ui/button"
import { notification } from "@/components/ui/notification"
import { Skeleton } from "@/components/ui/skeleton"
import { WorkspaceShell } from "@/layout/workspace-shell"
import { fetchMarketEtf, type MarketEtfResponse } from "@/services/market/etf"
import { EtfMarketTape } from "./components/market-tape"
import { MarketEtfHeader } from "./components/page-header"
import { EtfResearchWorkbench } from "./components/research-workbench"

function WorkbenchSkeleton() {
  return <div className="space-y-4"><Skeleton className="h-[70px]" /><div className="grid gap-4 xl:grid-cols-[minmax(0,1.65fr)_minmax(360px,0.75fr)]"><Skeleton className="h-[620px]" /><Skeleton className="h-[620px]" /></div></div>
}

export default function MarketEtfPage() {
  const [data, setData] = useState<MarketEtfResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const response = await fetchMarketEtf({ historyDays: 10, rowLimit: 180 })
      if (!response.ok) throw new Error(response.error || "ETF 数据为空")
      setData(response)
    } catch (cause) {
      const message = cause instanceof Error ? cause.message : String(cause)
      setError(message)
      notification.danger({ title: "ETF 市场数据加载失败", description: message })
    } finally { setLoading(false) }
  }, [])

  useEffect(() => { const timer = window.setTimeout(() => { void load() }, 0); return () => window.clearTimeout(timer) }, [load])

  return <WorkspaceShell sectionLabel="market" pageTitle="market-etf"><MarketEtfHeader tradeDate={data?.tradeDate} fetchedAt={data?.fetchedAt} source={data?.source} loading={loading} onRefresh={() => void load()} />{error && !loading ? <section className="flex items-start justify-between gap-4 rounded-lg border border-destructive/30 bg-destructive/5 px-4 py-3"><div className="flex min-w-0 gap-3"><AlertCircle className="mt-0.5 size-4 shrink-0 text-destructive" /><div><div className="text-sm font-medium">ETF 数据加载失败</div><p className="mt-1 break-words text-xs text-muted-foreground">{error}</p></div></div><Button type="button" size="sm" variant="outline" onClick={() => void load()}>重试</Button></section> : null}{loading && !data ? <WorkbenchSkeleton /> : data ? <><EtfMarketTape summary={data.summary} breadth={data.breadth} rotations={data.rotations} /><EtfResearchWorkbench data={data} /><footer className="flex flex-col gap-2 border-t border-border/60 pt-3 text-[11px] leading-5 text-muted-foreground md:flex-row md:items-start md:justify-between"><div className="flex max-w-4xl items-start gap-2"><Database className="mt-0.5 size-3.5 shrink-0" /><p>份额变化来自交易所份额数据；申赎金额为“份额变化 × IOPV”代理值，主力净额仅反映二级市场分单资金，不代表真实申购赎回。</p></div><div className="shrink-0 font-mono">分类覆盖 {data.summary?.classificationCoveragePct == null ? "—" : `${data.summary.classificationCoveragePct.toFixed(1)}%`}</div></footer></> : !loading ? <div className="grid min-h-[420px] place-items-center rounded-lg border border-dashed border-border/70"><div className="text-center"><Database className="mx-auto size-6 text-muted-foreground" /><div className="mt-3 text-sm font-medium">暂无 ETF 市场数据</div><p className="mt-1 text-xs text-muted-foreground">请确认同步任务和市场数据源状态</p></div></div> : null}</WorkspaceShell>
}
