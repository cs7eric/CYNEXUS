import { useCallback, useMemo, useState } from "react"
import type { KeyboardEvent, ReactNode } from "react"
import { ChevronsDown, ChevronsUp, RefreshCw, SearchX, X } from "lucide-react"

import {
  fetchSectorStocks,
  type SectorStock,
  type SectorStocksResponse,
  type StockSectorEntry,
  type StockSectorsResponse,
} from "@/services/market/stock-chart"
import {
  Drawer,
  DrawerClose,
  DrawerContent,
  DrawerDescription,
  DrawerHeader,
  DrawerTitle,
} from "@/components/ui/drawer"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { cn } from "@/lib/utils"

type SectorKind = "industry" | "concept" | "style"

interface SectorSelection {
  entry: StockSectorEntry
  kind: SectorKind
  title: string
}

interface StockSectorClassificationProps {
  sectors?: StockSectorsResponse | null
  loading?: boolean
  className?: string
  defaultExpanded?: boolean
  expandable?: boolean
  showHeader?: boolean
  stockLimit?: number
  onStockClick?: (stock: SectorStock) => void
}

const KIND_LABEL: Record<SectorKind, string> = {
  industry: "行业",
  concept: "概念",
  style: "风格",
}

export function StockSectorClassification({
  sectors,
  loading = false,
  className,
  defaultExpanded = false,
  expandable,
  showHeader = true,
  stockLimit = 300,
  onStockClick,
}: StockSectorClassificationProps) {
  const [expanded, setExpanded] = useState(defaultExpanded)
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [selected, setSelected] = useState<SectorSelection | null>(null)
  const [stocksPayload, setStocksPayload] = useState<SectorStocksResponse | null>(null)
  const [stocksLoading, setStocksLoading] = useState(false)
  const [stocksError, setStocksError] = useState<string | null>(null)

  const industries = sectors?.industries ?? []
  const concepts = sectors?.concepts ?? []
  const styles = sectors?.styles ?? []
  const sectorCount = industries.length + concepts.length + styles.length
  const canExpand = expandable ?? (industries.length > 2 || concepts.length > 2 || styles.length > 0)

  const selectedQuery = useMemo(() => {
    if (!selected) return null
    return sectorQuery(selected.entry, selected.kind)
  }, [selected])

  const loadStocksForQuery = useCallback(async (query: ReturnType<typeof sectorQuery>) => {
    setStocksLoading(true)
    setStocksError(null)
    try {
      const data = await fetchSectorStocks({ ...query, limit: stockLimit })
      setStocksPayload(data)
    } catch (error) {
      setStocksPayload(null)
      setStocksError(error instanceof Error ? error.message : "读取成分股失败")
    } finally {
      setStocksLoading(false)
    }
  }, [stockLimit])

  const loadStocks = useCallback(() => {
    if (!selectedQuery) return
    void loadStocksForQuery(selectedQuery)
  }, [loadStocksForQuery, selectedQuery])

  const openSector = (entry: StockSectorEntry, kind: SectorKind) => {
    const query = sectorQuery(entry, kind)
    setSelected({ entry, kind, title: KIND_LABEL[kind] })
    setStocksPayload(null)
    setStocksError(null)
    setDrawerOpen(true)
    void loadStocksForQuery(query)
  }

  const handleKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
    if (!canExpand) return
    if (event.key === "Enter" || event.key === " ") {
      event.preventDefault()
      setExpanded((prev) => !prev)
    }
  }

  if (loading) return <StockSectorClassificationSkeleton className={className} />
  if (sectorCount === 0) return null

  return (
    <>
      <div
        className={cn(
          "min-w-0 rounded-xl border border-border/35 bg-background/55 px-3 py-2.5 text-[11px] backdrop-blur-sm transition-colors",
          canExpand && "cursor-pointer hover:border-border/70 hover:bg-background/80",
          className,
        )}
        role={canExpand ? "button" : undefined}
        tabIndex={canExpand ? 0 : undefined}
        onClick={canExpand ? () => setExpanded((prev) => !prev) : undefined}
        onKeyDown={handleKeyDown}
        aria-expanded={canExpand ? expanded : undefined}
      >
        {showHeader && canExpand ? (
          <div className="mb-2 flex items-center justify-between gap-2">
            <div className="min-w-0 truncate text-[10px] font-medium text-muted-foreground/65">
              板块归属 · 行业 {industries.length} · 概念 {concepts.length}
              {styles.length > 0 ? ` · 风格 ${styles.length}` : ""}
            </div>
            <span className="inline-flex shrink-0 items-center gap-1 rounded-full bg-muted/35 px-2 py-0.5 text-[10px] font-medium text-muted-foreground transition-colors">
              {expanded ? <ChevronsUp className="size-3" /> : <ChevronsDown className="size-3" />}
              {expanded ? "收起" : "展开"}
            </span>
          </div>
        ) : null}

        <div className="grid min-w-0 gap-1.5">
          <SectorLine title="行业" items={industries} kind="industry" limit={expanded ? undefined : 2} prominent onOpen={openSector} />
          <SectorLine title="概念" items={concepts} kind="concept" limit={expanded ? undefined : 5} onOpen={openSector} />
          {expanded ? <SectorLine title="风格" items={styles} kind="style" onOpen={openSector} /> : null}
        </div>
      </div>

      <SectorStocksDrawer
        open={drawerOpen}
        onOpenChange={setDrawerOpen}
        selected={selected}
        payload={stocksPayload}
        loading={stocksLoading}
        error={stocksError}
        onRefresh={loadStocks}
        onStockClick={onStockClick}
      />
    </>
  )
}

function SectorLine({
  title,
  items,
  kind,
  limit,
  prominent = false,
  onOpen,
}: {
  title: string
  items: StockSectorEntry[]
  kind: SectorKind
  limit?: number
  prominent?: boolean
  onOpen: (entry: StockSectorEntry, kind: SectorKind) => void
}) {
  if (items.length === 0) return null
  const visibleItems = limit == null ? items : items.slice(0, limit)
  const hiddenCount = items.length - visibleItems.length

  return (
    <div className="grid min-w-0 grid-cols-[32px_minmax(0,1fr)] items-start gap-2">
      <div className="pt-1 text-[10px] font-semibold text-muted-foreground/65">{title}</div>
      <div className="min-w-0 flex flex-wrap items-center gap-1">
        {visibleItems.map((entry, index) => (
          <button
            key={sectorKey(entry, kind, index)}
            type="button"
            className={cn(
              "inline-flex max-w-full items-center gap-1 rounded-md px-1.5 py-0.5 font-medium outline-none transition-colors",
              "hover:ring-1 hover:ring-border/70 focus-visible:ring-1 focus-visible:ring-ring/50",
              prominent ? "border border-sky-200/80 bg-sky-50/75 text-sky-800 hover:bg-sky-50" : "bg-muted/45 text-muted-foreground hover:bg-muted/70 hover:text-foreground",
              kind === "style" && "bg-violet-50/70 text-violet-700 hover:bg-violet-50",
            )}
            title={[entry.name, entry.topic_id].filter(Boolean).join(" · ") || undefined}
            onClick={(event) => {
              event.stopPropagation()
              onOpen(entry, kind)
            }}
          >
            <span className={cn("truncate", prominent ? "max-w-[138px]" : "max-w-[116px]")}>{entry.name || "—"}</span>
            {entry.changePercent != null ? (
              <span className="tabular-nums text-[10px] opacity-80">
                {entry.changePercent >= 0 ? "+" : ""}
                {entry.changePercent.toFixed(2)}%
              </span>
            ) : null}
          </button>
        ))}
        {hiddenCount > 0 ? (
          <span className="inline-flex items-center rounded-md border border-dashed border-border/60 bg-muted/25 px-1.5 py-0.5 font-medium text-muted-foreground/70">
            +{hiddenCount}
          </span>
        ) : null}
      </div>
    </div>
  )
}

function SectorStocksDrawer({
  open,
  onOpenChange,
  selected,
  payload,
  loading,
  error,
  onRefresh,
  onStockClick,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  selected: SectorSelection | null
  payload: SectorStocksResponse | null
  loading: boolean
  error: string | null
  onRefresh: () => void
  onStockClick?: (stock: SectorStock) => void
}) {
  const stocks = payload?.stocks ?? []
  const sectorName = payload?.sector?.name || selected?.entry.name || "板块"
  const kindLabel = selected?.title ?? "板块"

  return (
    <Drawer open={open} onOpenChange={onOpenChange} direction="right">
      <DrawerContent className="left-auto right-0 top-0 mt-0 h-screen w-screen max-w-none overflow-hidden rounded-none border-l bg-background p-0 data-[vaul-drawer-direction=right]:w-screen data-[vaul-drawer-direction=right]:sm:w-[min(960px,calc(100vw-24px))] data-[vaul-drawer-direction=right]:sm:max-w-none">
        <DrawerClose asChild>
          <Button type="button" size="icon-sm" variant="ghost" className="absolute right-3 top-3 z-10 rounded-full text-muted-foreground hover:text-foreground" aria-label="关闭成分股列表" title="关闭">
            <X className="size-4" />
          </Button>
        </DrawerClose>
        <DrawerHeader className="border-b border-border/70 px-4 py-4 sm:px-5">
          <div className="flex min-w-0 items-start justify-between gap-3 pr-8">
            <div className="min-w-0">
              <DrawerTitle className="truncate text-base">{sectorName}</DrawerTitle>
              <DrawerDescription className="mt-1 flex flex-wrap items-center gap-x-2 gap-y-1 text-xs">
                <span>{kindLabel}</span>
                {selected?.entry.sector_id ? <span className="font-mono">{selected.entry.sector_id}</span> : null}
                <span>{loading ? "加载中" : `${stocks.length} 只股票`}</span>
                {payload?.sector?.source ? <span>{payload.sector.source}</span> : null}
              </DrawerDescription>
            </div>
            <Button type="button" size="icon-sm" variant="ghost" className="shrink-0 rounded-full" onClick={onRefresh} disabled={loading || !selected} aria-label="刷新成分股" title="刷新">
              <RefreshCw className={cn("size-3.5", loading && "animate-spin")} />
            </Button>
          </div>
        </DrawerHeader>

        <div className="flex min-h-0 flex-1 flex-col overflow-hidden px-3 py-3 sm:px-5">
          {loading ? (
            <StockListSkeleton />
          ) : error ? (
            <DrawerState icon={<SearchX className="size-5" />} title="成分股读取失败" description={error} />
          ) : stocks.length === 0 ? (
            <DrawerState icon={<SearchX className="size-5" />} title="没有成分股" description="当前板块暂时没有匹配到股票列表。" />
          ) : (
            <SectorStockTable stocks={stocks} onStockClick={onStockClick} />
          )}
        </div>
      </DrawerContent>
    </Drawer>
  )
}

function SectorStockTable({
  stocks,
  onStockClick,
}: {
  stocks: SectorStock[]
  onStockClick?: (stock: SectorStock) => void
}) {
  return (
    <div className="min-h-0 flex-1 overflow-hidden rounded-lg border border-border/60 bg-card">
      <div className="h-full overflow-auto">
        <table className="w-full min-w-[1620px] border-collapse text-xs">
          <thead className="sticky top-0 z-10 bg-muted/90 backdrop-blur">
            <tr className="border-b border-border/60 text-[11px] font-medium text-muted-foreground">
              <th className="sticky left-0 z-20 w-[184px] bg-muted/90 px-3 py-2 text-left backdrop-blur">股票</th>
              <th className="px-2 py-2 text-right">收/现</th>
              <th className="px-2 py-2 text-right">涨跌幅</th>
              <th className="px-2 py-2 text-right">5日</th>
              <th className="px-2 py-2 text-right">10日</th>
              <th className="px-2 py-2 text-right">30日</th>
              <th className="px-2 py-2 text-right">60日</th>
              <th className="px-2 py-2 text-right">252日</th>
              <th className="px-2 py-2 text-right">涨跌额</th>
              <th className="px-2 py-2 text-right">今开</th>
              <th className="px-2 py-2 text-right">最高</th>
              <th className="px-2 py-2 text-right">最低</th>
              <th className="px-2 py-2 text-right">昨收</th>
              <th className="px-2 py-2 text-right">成交量</th>
              <th className="px-2 py-2 text-right">成交额</th>
              <th className="px-2 py-2 text-right">总市值</th>
              <th className="px-2 py-2 text-right">流通市值</th>
              <th className="px-2 py-2 text-right">换手</th>
              <th className="px-2 py-2 text-right">振幅</th>
              <th className="px-2 py-2 text-right">关系</th>
              <th className="px-3 py-2 text-right">日期</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-border/50">
            {stocks.map((stock) => {
              const clickable = Boolean(onStockClick)
              return (
                <tr
                  key={stock.symbol}
                  className={cn("transition-colors", clickable && "cursor-pointer hover:bg-muted/35")}
                  onClick={clickable ? () => onStockClick?.(stock) : undefined}
                >
                  <td className="sticky left-0 z-10 bg-card px-3 py-2 shadow-[1px_0_0_var(--border)]">
                    <div className="min-w-0">
                      <div className="flex min-w-0 items-center gap-2">
                        <span className="truncate text-[13px] font-semibold text-foreground">{stock.name || stock.symbol}</span>
                        <span className="font-mono text-[11px] text-muted-foreground">{stock.symbol}</span>
                      </div>
                      <div className="mt-0.5 flex items-center gap-2 text-[10px] text-muted-foreground">
                        <span>{stock.market || "-"}</span>
                        {stock.source ? <span>{stock.source}</span> : null}
                      </div>
                    </div>
                  </td>
                  <MetricCell value={stock.last_price} />
                  <MetricCell value={stock.change_pct} percent signed tone />
                  <MetricCell value={stock.return_pct_5d} percent signed tone />
                  <MetricCell value={stock.return_pct_10d} percent signed tone />
                  <MetricCell value={stock.return_pct_30d} percent signed tone />
                  <MetricCell value={stock.return_pct_60d} percent signed tone />
                  <MetricCell value={stock.return_pct_252d} percent signed tone />
                  <MetricCell value={stock.change} signed tone />
                  <MetricCell value={stock.open} />
                  <MetricCell value={stock.high} />
                  <MetricCell value={stock.low} />
                  <MetricCell value={stock.pre_close} />
                  <td className="px-2 py-2 text-right font-mono text-[11px] text-foreground/85">{formatLots(stock.volume_lots)}</td>
                  <td className="px-2 py-2 text-right font-mono text-[11px] text-foreground/85">{formatAmountWan(stock.amount_wan)}</td>
                  <td className="px-2 py-2 text-right font-mono text-[11px] text-foreground/85">{formatMarketCap(stock.total_market_cap)}</td>
                  <td className="px-2 py-2 text-right font-mono text-[11px] text-foreground/85">{formatMarketCap(stock.float_market_cap)}</td>
                  <MetricCell value={stock.turnover_rate} percent />
                  <MetricCell value={stock.amplitude} percent />
                  <td className="px-2 py-2 text-right font-mono text-[11px] text-muted-foreground">{formatRelationLevel(stock.relation_level)}</td>
                  <td className="px-3 py-2 text-right font-mono text-[11px] text-muted-foreground">{stock.trade_date || "-"}</td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
    </div>
  )
}

function MetricCell({
  value,
  percent = false,
  signed = false,
  tone = false,
}: {
  value?: number | null
  percent?: boolean
  signed?: boolean
  tone?: boolean
}) {
  const color = tone && value != null ? value > 0 ? "text-rose-600" : value < 0 ? "text-emerald-600" : "text-foreground/85" : "text-foreground/85"
  return (
    <td className={cn("px-2 py-2 text-right font-mono text-[11px]", color)}>
      {formatNumber(value, { percent, signed })}
    </td>
  )
}

function DrawerState({ icon, title, description }: { icon: ReactNode; title: string; description: string }) {
  return (
    <div className="grid min-h-[220px] place-items-center rounded-xl border border-dashed border-border/70 bg-muted/15 px-6 text-center">
      <div>
        <div className="mx-auto flex size-10 items-center justify-center rounded-full bg-background text-muted-foreground shadow-sm">{icon}</div>
        <div className="mt-3 text-sm font-semibold text-foreground">{title}</div>
        <p className="mt-1 text-xs leading-5 text-muted-foreground">{description}</p>
      </div>
    </div>
  )
}

function StockListSkeleton() {
  return (
    <div className="min-h-0 flex-1 space-y-2 overflow-hidden">
      <div className="rounded-lg border border-border/60 bg-card px-3 py-3">
        <Skeleton className="h-4 w-40 rounded-full" />
      </div>
      {[0, 1, 2, 3, 4, 5].map((index) => (
        <div key={index} className="grid grid-cols-[160px_repeat(7,minmax(72px,1fr))] items-center gap-3 rounded-lg border border-border/50 bg-card px-3 py-3">
          <Skeleton className="h-4 w-32 rounded-full" />
          {[0, 1, 2, 3, 4, 5, 6].map((cell) => (
            <Skeleton key={cell} className="h-3 w-full rounded-full" />
          ))}
        </div>
      ))}
    </div>
  )
}

function StockSectorClassificationSkeleton({ className }: { className?: string }) {
  return (
    <div className={cn("grid min-w-0 gap-1 rounded-xl border border-border/35 bg-background/55 px-2.5 py-2 backdrop-blur-sm", className)}>
      {[0, 1].map((index) => (
        <div key={`sector-skeleton-${index}`} className="grid grid-cols-[34px_minmax(0,1fr)] items-center gap-2">
          <Skeleton className="h-3 w-7 rounded-full" />
          <div className="flex flex-wrap gap-1.5">
            <Skeleton className="h-6 w-20 rounded-full" />
            <Skeleton className="h-6 w-24 rounded-full" />
            <Skeleton className="h-6 w-16 rounded-full" />
          </div>
        </div>
      ))}
    </div>
  )
}

function sectorQuery(entry: StockSectorEntry, kind: SectorKind) {
  return {
    sectorId: entry.sector_id,
    categoryRaw: entry.category_raw ?? categoryRawForKind(kind),
    topicId: entry.topic_id,
    name: entry.name,
  }
}

function categoryRawForKind(kind: SectorKind) {
  if (kind === "industry") return 0
  if (kind === "concept") return 2
  return 4
}

function sectorKey(entry: StockSectorEntry, kind: SectorKind, index: number) {
  return entry.sector_id || `${kind}-${entry.category_raw ?? ""}-${entry.topic_id ?? ""}-${entry.name ?? index}`
}

function formatNumber(value: number | null | undefined, options: { percent?: boolean; signed?: boolean } = {}) {
  if (value == null || Number.isNaN(value)) return "—"
  const sign = options.signed && value > 0 ? "+" : ""
  return `${sign}${value.toFixed(options.percent ? 2 : 2)}${options.percent ? "%" : ""}`
}

function formatLots(value: number | null | undefined) {
  if (value == null || Number.isNaN(value)) return "—"
  if (Math.abs(value) >= 10000) return `${(value / 10000).toFixed(2)}万手`
  return `${value.toFixed(0)}手`
}

function formatAmountWan(value: number | null | undefined) {
  if (value == null || Number.isNaN(value)) return "—"
  if (Math.abs(value) >= 10000) return `${(value / 10000).toFixed(2)}亿`
  return `${value.toFixed(0)}万`
}

function formatMarketCap(value: number | null | undefined) {
  if (value == null || Number.isNaN(value)) return "—"
  return `${(value / 100000000).toFixed(2)}亿`
}

function formatRelationLevel(value: number | null | undefined) {
  if (value == null || Number.isNaN(value)) return "-"
  return String(value)
}
