import { useCallback, useEffect, useState } from "react"
import { CircleAlert } from "lucide-react"
import { useSearchParams } from "react-router-dom"

import { WorkspaceShell } from "@/layout/workspace-shell"
import { notification } from "@/components/ui/notification"
import { Skeleton } from "@/components/ui/skeleton"
import { fetchFuturesPositionLatest, type FuturesPositionInstrument, type FuturesPositionLatestResponse, type FuturesScopeType } from "@/services/market/futures-position"
import { FuturesPositionFilters } from "./components/filters"
import { FuturesPositionPageHeader } from "./components/page-header"
import { PositionDrawer } from "./components/position-drawer"
import { ResearchWorkbench } from "./components/research-workbench"

const FILTER_STORAGE_KEY = "cynexus.market.futures-position.filters.v5"

interface FuturesPositionFiltersState {
  tradeDate: string
  scopeType: FuturesScopeType
  exchangeCode: string
  varietyCode: string
  top: string
}

const defaultFilters: FuturesPositionFiltersState = {
  tradeDate: "",
  scopeType: "VARIETY",
  exchangeCode: "ALL",
  varietyCode: "",
  top: "20",
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

function readInitialFilters(): FuturesPositionFiltersState {
  const stored = readStoredFilters()
  if (typeof window === "undefined") return stored
  const params = new URLSearchParams(window.location.search)
  const scopeType = params.get("scopeType")
  const exchangeCode = params.get("exchangeCode")
  return {
    ...stored,
    ...(scopeType === "CONTRACT" || scopeType === "VARIETY" ? { scopeType } : {}),
    ...(exchangeCode ? { exchangeCode } : {}),
  }
}

export default function MarketFuturesPositionPage() {
  const [searchParams] = useSearchParams()
  const [filters, setFilters] = useState<FuturesPositionFiltersState>(readInitialFilters)
  const [payload, setPayload] = useState<FuturesPositionLatestResponse | null>(null)
  const [varietyOptions, setVarietyOptions] = useState<FuturesPositionInstrument[]>([])
  const [selectedCode, setSelectedCode] = useState<string | null>(null)
  const [selectedItem, setSelectedItem] = useState<FuturesPositionInstrument | null>(null)
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const updateFilter = useCallback(<K extends keyof FuturesPositionFiltersState>(key: K, value: FuturesPositionFiltersState[K]) => {
    setFilters((current) => ({ ...current, [key]: value }))
  }, [])

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    setSelectedItem(null)
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
    const timer = window.setTimeout(() => { void load() }, 0)
    return () => window.clearTimeout(timer)
  }, [load])

  useEffect(() => {
    let active = true
    void fetchFuturesPositionLatest({
      tradeDate: filters.tradeDate || undefined,
      scopeType: "VARIETY",
      exchangeCode: filters.exchangeCode === "ALL" ? undefined : filters.exchangeCode,
      top: Number(filters.top),
      sort: "instrumentCode",
      sortDir: "asc",
      page: 1,
      pageSize: 100,
    }).then((response) => {
      if (active) setVarietyOptions(response.instruments)
    }).catch(() => {
      if (active) setVarietyOptions([])
    })
    return () => { active = false }
  }, [filters.exchangeCode, filters.top, filters.tradeDate])

  const requestedInstrument = searchParams.get("instrumentCode")
  const selected = selectedItem ?? payload?.instruments.find((item) => item.instrumentCode === (selectedCode || requestedInstrument)) ?? payload?.instruments[0] ?? null
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
          varietyOptions={varietyOptions}
          top={filters.top}
          onTradeDateChange={(value) => updateFilter("tradeDate", value)}
          onScopeTypeChange={(value) => updateFilter("scopeType", value)}
          onExchangeChange={(value) => setFilters((current) => ({ ...current, exchangeCode: value, varietyCode: "" }))}
          onVarietyChange={(value) => updateFilter("varietyCode", value)}
          onTopChange={(value) => updateFilter("top", value)}
        />

        {payload?.warnings?.length ? <div className="flex items-start gap-2 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2.5 text-xs text-amber-800 dark:border-amber-900/60 dark:bg-amber-950/20 dark:text-amber-300"><CircleAlert className="mt-0.5 size-4 shrink-0" /><div className="space-y-1">{payload.warnings.map((warning) => <div key={warning}>{warning}</div>)}</div></div> : null}
        {error && !loading ? <div className="flex items-start gap-2 rounded-lg border border-rose-200 bg-rose-50 px-3 py-2.5 text-sm text-rose-700 dark:border-rose-900/50 dark:bg-rose-950/20 dark:text-rose-300"><CircleAlert className="mt-0.5 size-4 shrink-0" />{error}</div> : null}

        {loading ? <div className="space-y-3"><Skeleton className="h-32 rounded-lg" /><Skeleton className="h-[320px] rounded-lg" /><Skeleton className="h-80 rounded-lg" /></div> : <ResearchWorkbench
          payload={payload}
          selected={selected}
          loading={loading}
          scopeType={filters.scopeType}
          exchangeCode={selectedExchange}
          top={Number(filters.top)}
          tradeDate={payload?.tradeDate}
          onSelect={(row) => { setSelectedCode(row.instrumentCode); setSelectedItem(row); setDrawerOpen(false) }}
          onOpenDetails={() => setDrawerOpen(true)}
        />}
      </div>

      {drawerOpen && selected && selectedExchange ? <PositionDrawer row={selected} scopeType={selected.contractCode ? filters.scopeType : "VARIETY"} exchangeCode={selectedExchange} top={Number(filters.top)} tradeDate={payload?.tradeDate} onClose={() => setDrawerOpen(false)} /> : null}
      {drawerOpen && selected && !selectedExchange ? <div className="rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-800 dark:border-amber-900/60 dark:bg-amber-950/20 dark:text-amber-300">全交易所汇总结果需要先选择具体交易所，才能打开会员明细。</div> : null}
    </WorkspaceShell>
  )
}
