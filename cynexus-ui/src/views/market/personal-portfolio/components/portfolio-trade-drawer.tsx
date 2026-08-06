import { useCallback, useEffect, useMemo, useState } from "react"
import { ChevronLeft, ChevronRight, CircleAlert, RefreshCw } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from "@/components/ui/sheet"
import { Skeleton } from "@/components/ui/skeleton"
import { fetchPortfolioTrades } from "@/services/market/portfolio/portfolio"
import type { PortfolioTrade } from "@/services/market/portfolio/types"

const PAGE_SIZE_OPTIONS = [10, 20, 50] as const

interface PortfolioTradeDrawerProps {
  open: boolean
  onOpenChange: (open: boolean) => void
}

export function PortfolioTradeDrawer({ open, onOpenChange }: PortfolioTradeDrawerProps) {
  const [trades, setTrades] = useState<PortfolioTrade[]>([])
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState<number>(20)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const nextTrades = await fetchPortfolioTrades()
      setTrades([...nextTrades].sort((left, right) => {
        const timeDifference = tradeTimestamp(right.tradedAt) - tradeTimestamp(left.tradedAt)
        return timeDifference || right.id - left.id
      }))
      setPage(1)
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : String(cause))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    if (open) void Promise.resolve().then(load)
  }, [load, open])

  const totalPages = Math.max(1, Math.ceil(trades.length / pageSize))
  const currentPage = Math.min(page, totalPages)
  const visibleTrades = useMemo(() => {
    const start = (currentPage - 1) * pageSize
    return trades.slice(start, start + pageSize)
  }, [currentPage, pageSize, trades])

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent side="right" className="flex h-full w-full flex-col gap-0 overflow-hidden p-0 md:w-[min(1080px,82vw)] md:max-w-none">
        <SheetHeader className="shrink-0 border-b border-border/60 px-5 py-4 pr-12">
          <div className="flex min-w-0 items-start justify-between gap-4">
            <div className="min-w-0">
              <SheetTitle className="text-xl tracking-tight">总交易记录</SheetTitle>
              <SheetDescription className="mt-1">查看个人持仓中全部已确认的买卖交易。</SheetDescription>
            </div>
            <span className="shrink-0 rounded-full bg-muted px-2.5 py-1 text-xs text-muted-foreground">{trades.length} 笔</span>
          </div>
          <div className="mt-3 flex justify-end">
            <Button size="sm" variant="outline" onClick={() => void load()} disabled={loading}>
              <RefreshCw className={`mr-2 size-4 ${loading ? "animate-spin" : ""}`} />刷新
            </Button>
          </div>
        </SheetHeader>

        <div className="min-h-0 flex-1 overflow-auto bg-muted/20 p-4 sm:p-5">
          {error ? <div className="mb-4 flex items-start gap-2 rounded-lg border border-rose-200 bg-rose-50 px-3 py-2.5 text-sm text-rose-700"><CircleAlert className="mt-0.5 size-4 shrink-0" /><div className="min-w-0"><p className="font-medium">交易记录加载失败</p><p className="mt-1 break-words text-xs">{error}</p></div><Button variant="outline" size="sm" className="ml-auto" onClick={() => void load()}>重试</Button></div> : null}

          {loading ? <div className="space-y-3"><Skeleton className="h-12 rounded-lg" /><Skeleton className="h-12 rounded-lg" /><Skeleton className="h-12 rounded-lg" /><Skeleton className="h-12 rounded-lg" /></div> : !error && !trades.length ? <div className="flex min-h-[260px] items-center justify-center rounded-lg border border-dashed border-border/70 bg-card px-4 py-8 text-center"><div><p className="text-sm font-medium">暂无已确认交易</p><p className="mt-1 text-xs text-muted-foreground">导入并确认交易后，完整记录会显示在这里。</p></div></div> : null}

          {!loading && !error && trades.length ? <section className="overflow-hidden rounded-lg border border-border/60 bg-card" aria-labelledby="portfolio-all-trades-heading">
            <div className="overflow-x-auto">
              <table className="w-full min-w-[900px] text-sm">
                <thead className="bg-muted/45 text-left text-xs text-muted-foreground"><tr><th className="px-4 py-3">成交时间</th><th className="px-4 py-3">标的</th><th className="px-4 py-3">类型</th><th className="px-4 py-3">方向</th><th className="px-4 py-3 text-right">数量</th><th className="px-4 py-3 text-right">成交价</th><th className="px-4 py-3 text-right">成交额</th><th className="px-4 py-3 text-right">手续费</th><th className="px-4 py-3">状态</th></tr></thead>
                <tbody className="divide-y divide-border/60">
                  {visibleTrades.map((trade) => <tr key={trade.id} className="hover:bg-muted/35">
                    <td className="whitespace-nowrap px-4 py-3 font-mono text-xs">{formatDateTime(trade.tradedAt)}</td>
                    <td className="px-4 py-3"><div className="min-w-0"><p className="font-mono font-medium">{trade.symbol}</p><p className="max-w-[180px] truncate text-xs text-muted-foreground">{trade.securityName ?? "名称待补充"}</p></div></td>
                    <td className="px-4 py-3 text-xs text-muted-foreground">{trade.instrumentType === "ETF" ? "ETF" : "股票"}</td>
                    <td className={`px-4 py-3 font-medium ${trade.side === "BUY" ? "text-rose-600" : "text-emerald-600"}`}>{trade.side === "BUY" ? "买入" : "卖出"}</td>
                    <td className="px-4 py-3 text-right font-mono tabular-nums">{trade.quantity}</td>
                    <td className="px-4 py-3 text-right font-mono tabular-nums">{formatNumber(trade.price, 3)}</td>
                    <td className="px-4 py-3 text-right font-mono tabular-nums">{formatNumber(Number(trade.quantity) * Number(trade.price), 2)}</td>
                    <td className="px-4 py-3 text-right font-mono tabular-nums">{formatNumber(trade.totalFee, 2)}</td>
                    <td className="px-4 py-3 text-xs text-muted-foreground">{trade.status === "ACTIVE" ? "有效" : trade.status}</td>
                  </tr>)}
                </tbody>
              </table>
            </div>
          </section> : null}
        </div>

        {!loading && !error && trades.length ? <div className="shrink-0 border-t border-border/60 bg-background px-5 py-3"><div className="flex flex-wrap items-center justify-between gap-3"><div className="flex min-w-0 flex-wrap items-center gap-2 text-xs text-muted-foreground"><span className="font-mono tabular-nums">第 {currentPage} / {totalPages} 页 · 共 {trades.length} 笔交易</span><span className="hidden text-border sm:inline">·</span><span className="hidden sm:inline">每页</span><Select value={String(pageSize)} onValueChange={(value) => { setPageSize(Number(value)); setPage(1) }}><SelectTrigger size="sm" aria-label="每页交易记录数" className="h-7 w-[92px] border-0 bg-muted/45 px-2 text-xs shadow-none"><SelectValue /></SelectTrigger><SelectContent side="top">{PAGE_SIZE_OPTIONS.map((option) => <SelectItem key={option} value={String(option)}>{option} 条</SelectItem>)}</SelectContent></Select></div><div className="flex items-center gap-1"><Button size="icon-sm" variant="outline" aria-label="上一页" title="上一页" onClick={() => setPage((current) => Math.max(1, current - 1))} disabled={currentPage <= 1}><ChevronLeft className="size-4" /></Button><Button size="icon-sm" variant="outline" aria-label="下一页" title="下一页" onClick={() => setPage((current) => Math.min(totalPages, current + 1))} disabled={currentPage >= totalPages}><ChevronRight className="size-4" /></Button></div></div></div> : null}
      </SheetContent>
    </Sheet>
  )
}

function formatDateTime(value: string) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat("zh-CN", { dateStyle: "medium", timeStyle: "short" }).format(date)
}

function tradeTimestamp(value: string) {
  const timestamp = Date.parse(value)
  return Number.isNaN(timestamp) ? 0 : timestamp
}

function formatNumber(value: number | null | undefined, digits: number) {
  if (value == null || !Number.isFinite(Number(value))) return "--"
  return Number(value).toFixed(digits)
}
