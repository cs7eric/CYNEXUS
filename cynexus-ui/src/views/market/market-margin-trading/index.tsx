/**
 * Market Margin Trading 页面入口.
 *
 * 数据流:
 *  - UI service: src/services/market/margin.ts
 *  - Java API: /api/market/margin/latest, /summary/history, /stocks
 *  - Storage: PostgreSQL margin_market_summary / margin_stock_detail / margin_account_stats
 *  - Job: margin_sync_daily -> sync_margin
 */
import { useCallback, useEffect, useMemo, useState } from "react"

import { WorkspaceShell } from "@/layout/workspace-shell"
import { notification } from "@/components/ui/notification"
import {
  fetchMarginLatest,
  fetchMarginIndustries,
  fetchMarginStocks,
  fetchMarginSummaryHistory,
  type MarginIndustriesResponse,
  type MarginLatestResponse,
  type MarginStocksResponse,
  type MarginRankingSort,
  type MarginSortDirection,
  type MarginExchange,
  type MarginIndustrySummary,
  type MarginStockDetail,
} from "@/services/market/margin"
import { buildTrend, changeFromPrevious, withMarketAggregate } from "./lib/derive"
import { MarginTradingHeader } from "./components/page-header"
import { SummaryCards } from "./components/summary-cards"
import { MarginTrendChart } from "./components/margin-trend-chart"
import { IndustryAnalysisPanel } from "./components/industry-analysis-panel"
import { ExchangeSummaryTable } from "./components/exchange-summary-table"
import { IndustryRankingTable } from "./components/industry-ranking-table"
import { IndustryMarginDrawer } from "./components/industry-margin-drawer"
import { StockMarginDrawer } from "./components/stock-margin-drawer"
import { DEFAULT_RANKING_PAGE_SIZE } from "./lib/constants"

const DEFAULT_TOP_STOCK_LIMIT = 30

function rangeStart(range: string) {
  if (range === "all") return "2012-09-27"
  const days = range === "1y" ? 365 : range === "3y" ? 365 * 3 : 180
  const date = new Date()
  date.setDate(date.getDate() - days)
  return date.toISOString().slice(0, 10)
}

export default function MarketMarginTradingPage() {
  const [latest, setLatest] = useState<MarginLatestResponse | null>(null)
  const [stocks, setStocks] = useState<MarginStocksResponse | null>(null)
  const [industries, setIndustries] = useState<MarginIndustriesResponse | null>(null)
  const [industryAnalysisRows, setIndustryAnalysisRows] = useState<MarginIndustrySummary[]>([])
  const [historyItems, setHistoryItems] = useState<Awaited<ReturnType<typeof fetchMarginSummaryHistory>>["items"]>([])
  const [loading, setLoading] = useState(true)
  const [stocksLoading, setStocksLoading] = useState(false)
  const [industriesLoading, setIndustriesLoading] = useState(false)
  const [industryAnalysisLoading, setIndustryAnalysisLoading] = useState(false)
  const [range, setRange] = useState("1y")
  const [exchange, setExchange] = useState<"ALL" | MarginExchange>("ALL")
  const [sort, setSort] = useState<MarginRankingSort>("margin_balance")
  const [sortDir, setSortDir] = useState<MarginSortDirection>("desc")
  const [industrySort, setIndustrySort] = useState<MarginRankingSort>("margin_balance")
  const [industrySortDir, setIndustrySortDir] = useState<MarginSortDirection>("desc")
  const [stockPage, setStockPage] = useState(1)
  const [industryPage, setIndustryPage] = useState(1)
  const [stockPageSize, setStockPageSize] = useState(DEFAULT_RANKING_PAGE_SIZE)
  const [industryPageSize, setIndustryPageSize] = useState(DEFAULT_RANKING_PAGE_SIZE)
  const [selectedIndustry, setSelectedIndustry] = useState<MarginIndustrySummary | null>(null)
  const [selectedStock, setSelectedStock] = useState<MarginStockDetail | null>(null)

  const handleIndustrySelect = useCallback((row: MarginIndustrySummary) => {
    setSelectedIndustry(row)
    setSelectedStock(null)
    setExchange("ALL")
    setStockPage(1)
  }, [])

  const closeIndustryDrawer = useCallback(() => {
    setSelectedIndustry(null)
    setSelectedStock(null)
  }, [])

  const loadLatest = useCallback(async () => {
    setLoading(true)
    try {
      const payload = await fetchMarginLatest({ top: DEFAULT_TOP_STOCK_LIMIT })
      setLatest(payload)
      if (!payload.ok) {
        notification.warn({ title: "两融数据为空", description: payload.error ?? "暂无最新数据" })
      }
    } catch (error) {
      notification.danger({ title: "两融数据加载失败", description: error instanceof Error ? error.message : String(error) })
    } finally {
      setLoading(false)
    }
  }, [])

  const loadHistory = useCallback(async (nextRange: string) => {
    try {
      const payload = await fetchMarginSummaryHistory({
        start: rangeStart(nextRange),
        end: latest?.tradeDate ?? undefined,
      })
      setHistoryItems(payload.items ?? [])
    } catch (error) {
      notification.danger({ title: "两融历史加载失败", description: error instanceof Error ? error.message : String(error) })
    }
  }, [latest?.tradeDate])

  const loadStocks = useCallback(async () => {
    if (!selectedIndustry?.industryCode) {
      setStocks(null)
      return
    }
    setStocksLoading(true)
    try {
      const payload = await fetchMarginStocks({
        tradeDate: latest?.tradeDate ?? undefined,
        exchange: exchange === "ALL" ? undefined : exchange,
        industryCode: selectedIndustry?.industryCode,
        sort,
        sortDir,
        page: stockPage,
        pageSize: stockPageSize,
      })
      setStocks(payload)
    } catch (error) {
      notification.danger({ title: "个股两融加载失败", description: error instanceof Error ? error.message : String(error) })
    } finally {
      setStocksLoading(false)
    }
  }, [exchange, latest?.tradeDate, selectedIndustry?.industryCode, sort, sortDir, stockPage, stockPageSize])

  const loadIndustries = useCallback(async () => {
    setIndustriesLoading(true)
    try {
      const payload = await fetchMarginIndustries({
        tradeDate: latest?.tradeDate ?? undefined,
        sort: industrySort,
        sortDir: industrySortDir,
        page: industryPage,
        pageSize: industryPageSize,
      })
      setIndustries(payload)
    } catch (error) {
      notification.danger({ title: "行业两融加载失败", description: error instanceof Error ? error.message : String(error) })
    } finally {
      setIndustriesLoading(false)
    }
  }, [industryPage, industryPageSize, industrySort, industrySortDir, latest?.tradeDate])

  const loadIndustryAnalysis = useCallback(async () => {
    if (!latest?.tradeDate) return
    setIndustryAnalysisLoading(true)
    try {
      const payload = await fetchMarginIndustries({
        tradeDate: latest.tradeDate,
        sort: "margin_balance",
        sortDir: "desc",
        page: 1,
        pageSize: 200,
      })
      setIndustryAnalysisRows(payload.items ?? [])
    } catch (error) {
      notification.danger({ title: "行业分析加载失败", description: error instanceof Error ? error.message : String(error) })
    } finally {
      setIndustryAnalysisLoading(false)
    }
  }, [latest?.tradeDate])

  useEffect(() => {
    void loadLatest()
  }, [loadLatest])

  useEffect(() => {
    if (!latest?.tradeDate) return
    void loadHistory(range)
  }, [latest?.tradeDate, range, loadHistory])

  useEffect(() => {
    if (!latest?.tradeDate || !selectedIndustry?.industryCode) return
    void loadStocks()
  }, [latest?.tradeDate, selectedIndustry?.industryCode, loadStocks])

  useEffect(() => {
    if (!latest?.tradeDate) return
    void loadIndustries()
  }, [latest?.tradeDate, industrySort, industrySortDir, industryPage, loadIndustries])

  useEffect(() => {
    void loadIndustryAnalysis()
  }, [loadIndustryAnalysis])

  const trend = useMemo(() => buildTrend(historyItems), [historyItems])
  const displaySummaries = useMemo(() => withMarketAggregate(latest?.summaries ?? []), [latest?.summaries])
  const financingBuyChange = useMemo(() => changeFromPrevious(trend, "buyCn"), [trend])

  return (
    <WorkspaceShell sectionLabel="market" pageTitle="market-margin-trading">
      <MarginTradingHeader
        tradeDate={latest?.tradeDate}
        updatedAt={latest?.updatedAt}
        loading={loading}
        onRefresh={() => {
          void loadLatest()
          void loadHistory(range)
          void loadStocks()
          void loadIndustries()
          void loadIndustryAnalysis()
        }}
      />

      <div className="min-w-0 space-y-4 overflow-hidden">
        <SummaryCards summaries={displaySummaries} financingBuyChange={financingBuyChange} trend={trend} />

        <div className="flex flex-wrap items-center gap-2 rounded-lg bg-muted/35 p-2">
          {[
            ["180d", "近 180 日"],
            ["1y", "近 1 年"],
            ["3y", "近 3 年"],
            ["all", "全部"],
          ].map(([value, label]) => (
            <button
              key={value}
              type="button"
              onClick={() => setRange(value)}
              className={`rounded-md px-3 py-1.5 text-sm transition ${range === value ? "bg-background font-medium shadow-sm" : "text-muted-foreground hover:bg-background/60 hover:text-foreground"}`}
            >
              {label}
            </button>
          ))}
        </div>

        <div className="grid min-w-0 gap-4 xl:h-[560px] xl:grid-cols-[minmax(0,1.12fr)_minmax(380px,0.88fr)]">
          <MarginTrendChart data={trend} loading={loading} />
          <IndustryAnalysisPanel
            rows={industryAnalysisRows}
            loading={industryAnalysisLoading}
            selectedIndustryCode={selectedIndustry?.industryCode}
            onIndustrySelect={handleIndustrySelect}
          />
        </div>

        <div className="min-w-0 space-y-4">
          <IndustryRankingTable
            rows={industries?.items ?? []}
            count={industries?.count ?? 0}
            loading={industriesLoading}
            page={industryPage}
            pageSize={industryPageSize}
            sort={industrySort}
            sortDir={industrySortDir}
            selectedIndustryCode={selectedIndustry?.industryCode}
            snapshotDate={industries?.constituentSnapshotDate}
            onPageChange={setIndustryPage}
            onPageSizeChange={(value) => {
              setIndustryPageSize(value)
            }}
            onSortChange={(value, direction = "desc") => {
              setIndustrySort(value)
              setIndustrySortDir(direction)
            }}
            onIndustrySelect={handleIndustrySelect}
            onRefresh={() => void loadIndustries()}
          />
        </div>

        <ExchangeSummaryTable rows={displaySummaries} />
      </div>
      <IndustryMarginDrawer
        industry={selectedIndustry}
        stocks={stocks}
        stocksLoading={stocksLoading}
        stockPage={stockPage}
        stockPageSize={stockPageSize}
        exchange={exchange}
        sort={sort}
        sortDir={sortDir}
        onClose={closeIndustryDrawer}
        onPageChange={setStockPage}
        onPageSizeChange={setStockPageSize}
        onExchangeChange={(value) => {
          setExchange(value)
          setStockPage(1)
        }}
        onSortChange={(value, direction = "desc") => {
          setSort(value)
          setSortDir(direction)
          setStockPage(1)
        }}
        onStockSelect={setSelectedStock}
        onRefresh={() => void loadStocks()}
      />
      <StockMarginDrawer stock={selectedStock} onClose={() => setSelectedStock(null)} />
    </WorkspaceShell>
  )
}
