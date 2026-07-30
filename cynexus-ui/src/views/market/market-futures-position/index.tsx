import { useCallback, useEffect, useState } from "react"
import { CircleAlert, Info } from "lucide-react"
import { useSearchParams } from "react-router-dom"

import { WorkspaceShell } from "@/layout/workspace-shell"
import { notification } from "@/components/ui/notification"
import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"
import { fetchFuturesPositionLatest, type FuturesPositionInstrument, type FuturesPositionLatestResponse, type FuturesScopeType } from "@/services/market/futures-position"
import { CoveragePanel } from "./components/coverage-panel"
import { FuturesPositionFilters } from "./components/filters"
import { InstrumentTable } from "./components/instrument-table"
import { FuturesPositionPageHeader } from "./components/page-header"
import { PositionDrawer } from "./components/position-drawer"
import { SummaryStrip } from "./components/summary-strip"

const FILTER_STORAGE_KEY = "cynexus.market.futures-position.filters.v1"

interface FuturesPositionFiltersState {
  tradeDate: string
  scopeType: FuturesScopeType
  exchangeCode: string
  varietyCode: string
  top: string
}

const defaultFilters: FuturesPositionFiltersState = {
  tradeDate: "",
  scopeType: "CONTRACT",
  exchangeCode: "ALL",
  varietyCode: "",
  top: "10",
}

function readStoredFilters(): FuturesPositionFiltersState {
  if (typeof window === "undefined") return defaultFilters
  try {
    const stored = JSON.parse(window.localStorage.getItem(FILTER_STORAGE_KEY) || "null") as Partial<FuturesPositionFiltersState> | null
    return { ...defaultFilters, ...stored, scopeType: stored?.scopeType === "VARIETY" ? "VARIETY" : "CONTRACT" }
  } catch {
    return defaultFilters
  }
}

export default function MarketFuturesPositionPage() {
  const [searchParams] = useSearchParams()
  const [filters, setFilters] = useState<FuturesPositionFiltersState>(readStoredFilters)
  const [payload, setPayload] = useState<FuturesPositionLatestResponse | null>(null)
  const [selected, setSelected] = useState<FuturesPositionInstrument | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [pendingInstrument, setPendingInstrument] = useState<string | null>(null)

  const updateFilter = useCallback(<K extends keyof FuturesPositionFiltersState>(key: K, value: FuturesPositionFiltersState[K]) => {
    setFilters((current) => ({ ...current, [key]: value }))
  }, [])

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const response = await fetchFuturesPositionLatest({
        tradeDate: filters.tradeDate || undefined,
        scopeType: filters.scopeType,
        exchangeCode: filters.exchangeCode === "ALL" ? undefined : filters.exchangeCode,
        varietyCode: filters.varietyCode || undefined,
        top: Number(filters.top),
        sort: "netPosition",
        sortDir: "desc",
        page: 1,
        pageSize: 30,
      })
      setPayload(response)
      if (!response.ok) {
        setError(response.error || "当前没有可用的期货会员持仓数据")
        notification.warn({ title: "期货持仓数据为空", description: response.error || "可切换交易日或交易所重试" })
      }
    } catch (cause) {
      const message = cause instanceof Error ? cause.message : String(cause)
      setError(message)
      setPayload(null)
      notification.danger({ title: "期货持仓加载失败", description: message })
    } finally {
      setLoading(false)
    }
  }, [filters])

  useEffect(() => {
    window.localStorage.setItem(FILTER_STORAGE_KEY, JSON.stringify(filters))
  }, [filters])

  useEffect(() => {
    const scopeType = searchParams.get("scopeType")
    const exchangeCode = searchParams.get("exchangeCode")
    const instrumentCode = searchParams.get("instrumentCode")
    if (scopeType === "CONTRACT" || scopeType === "VARIETY" || exchangeCode || instrumentCode) {
      setFilters((current) => ({
        ...current,
        ...(scopeType === "CONTRACT" || scopeType === "VARIETY" ? { scopeType } : {}),
        ...(exchangeCode ? { exchangeCode } : {}),
      }))
    }
    setPendingInstrument(instrumentCode)
  }, [searchParams])

  useEffect(() => {
    if (!pendingInstrument || !payload?.instruments?.length) return
    const row = payload.instruments.find((item) => item.instrumentCode === pendingInstrument)
    if (row) {
      setSelected(row)
      setPendingInstrument(null)
    }
  }, [payload, pendingInstrument])

  useEffect(() => {
    setSelected(null)
    const timer = window.setTimeout(() => { void load() }, 0)
    return () => window.clearTimeout(timer)
  }, [load])

  const selectedExchange = selected?.exchangeCode || (filters.exchangeCode === "ALL" ? "" : filters.exchangeCode)

  return (
    <WorkspaceShell sectionLabel="market" pageTitle="futures position">
      <FuturesPositionPageHeader tradeDate={payload?.tradeDate} fallback={payload?.fallbackTradeDate} loading={loading} onRefresh={() => void load()} />

      <div className="min-w-0 space-y-4 overflow-hidden">
        <FuturesPositionFilters
          tradeDate={filters.tradeDate}
          scopeType={filters.scopeType}
          exchangeCode={filters.exchangeCode}
          varietyCode={filters.varietyCode}
          top={filters.top}
          onTradeDateChange={(value) => updateFilter("tradeDate", value)}
          onScopeTypeChange={(value) => updateFilter("scopeType", value)}
          onExchangeChange={(value) => updateFilter("exchangeCode", value)}
          onVarietyChange={(value) => updateFilter("varietyCode", value)}
          onTopChange={(value) => updateFilter("top", value)}
        />

        {payload?.warnings?.length ? <div className="flex items-start gap-2 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2.5 text-xs text-amber-800 dark:border-amber-900/60 dark:bg-amber-950/20 dark:text-amber-300"><CircleAlert className="mt-0.5 size-4 shrink-0" /><div className="space-y-1">{payload.warnings.map((warning) => <div key={warning}>{warning}</div>)}</div></div> : null}
        {error && !loading ? <div className="flex items-start gap-2 rounded-lg border border-rose-200 bg-rose-50 px-3 py-2.5 text-sm text-rose-700 dark:border-rose-900/50 dark:bg-rose-950/20 dark:text-rose-300"><CircleAlert className="mt-0.5 size-4 shrink-0" />{error}</div> : null}

        {loading ? <div className="space-y-4"><Skeleton className="h-28 rounded-lg" /><div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_minmax(300px,0.38fr)]"><Skeleton className="h-[480px] rounded-lg" /><Skeleton className="h-[360px] rounded-lg" /></div></div> : <>
          <SummaryStrip aggregate={payload?.aggregate} count={payload?.count ?? 0} top={Number(filters.top)} response={payload} />
          <div className="flex items-start gap-2 text-xs leading-5 text-muted-foreground"><Info className="mt-0.5 size-3.5 shrink-0" /><span>多头、空头和成交量是三张独立的会员排名榜。净值仅用于观察排名样本的结构偏向，不等同于交易信号。</span><Badge variant="outline" className="ml-auto hidden shrink-0 font-mono text-[10px] sm:inline-flex">{payload?.tradeDate || "NO DATE"}</Badge></div>
          <div className="grid min-w-0 gap-4 xl:grid-cols-[minmax(0,1fr)_minmax(320px,0.38fr)]">
            <InstrumentTable rows={payload?.instruments ?? []} loading={false} scopeType={filters.scopeType} selectedCode={selected?.instrumentCode} onSelect={setSelected} />
            <CoveragePanel coverage={payload?.coverage} />
          </div>
        </>}
      </div>

      {selected && selectedExchange ? <PositionDrawer row={selected} scopeType={filters.scopeType} exchangeCode={selectedExchange} top={Number(filters.top)} tradeDate={payload?.tradeDate} onClose={() => setSelected(null)} /> : null}
      {selected && !selectedExchange ? <div className="rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-800 dark:border-amber-900/60 dark:bg-amber-950/20 dark:text-amber-300">全交易所汇总结果需要先选择具体交易所，才能打开会员明细。</div> : null}
    </WorkspaceShell>
  )
}
