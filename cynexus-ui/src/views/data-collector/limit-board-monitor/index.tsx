import { useCallback, useEffect, useMemo, useState } from "react"
import { Activity, CircleAlert, RefreshCw } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Skeleton } from "@/components/ui/skeleton"
import { Switch } from "@/components/ui/switch"
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { WorkspaceShell } from "@/layout/workspace-shell"
import { currentTradingDayLocal } from "@/lib/date-utils"
import { fetchLimitBoardSessionSummaries, type LimitBoardSessionSummaryResponse } from "@/services/market/indicator"

import { StockBoardCard } from "./components/stock-board-card"
import { calendarDay, percent } from "./lib/format"

type Session = "morning" | "afternoon"
type Direction = "all" | "up" | "down"

const sessionLabels: Record<Session, string> = { morning: "上午盘", afternoon: "下午盘" }

export default function LimitBoardMonitorPage() {
  const [tradeDate, setTradeDate] = useState(() => currentTradingDayLocal())
  const [session, setSession] = useState<Session>("morning")
  const [direction, setDirection] = useState<Direction>("all")
  const [sealedOnly, setSealedOnly] = useState(true)
  const [payload, setPayload] = useState<LimitBoardSessionSummaryResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      setPayload(await fetchLimitBoardSessionSummaries({
        tradeDate: tradeDate || undefined,
        tradeSession: session,
        direction,
        limit: 240,
      }))
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "封板汇总暂时不可用")
    } finally {
      setLoading(false)
    }
  }, [direction, session, tradeDate])

  useEffect(() => {
    const timer = window.setTimeout(() => void load(), 0)
    return () => window.clearTimeout(timer)
  }, [load])

  const rows = useMemo(() => {
    const items = payload?.items ?? []
    if (!sealedOnly) return items
    return items.filter((row) => row.first_sealed_at != null || row.max_sealed_amount != null)
  }, [payload, sealedOnly])
  const hiddenCount = (payload?.items?.length ?? 0) - rows.length
  const sealedCount = rows.filter((row) => row.is_sealed_at_close).length
  const avgCoverage = rows.length ? rows.reduce((sum, row) => sum + (row.coverage_ratio ?? 0), 0) / rows.length : null
  const day = calendarDay(payload?.tradeDate) || tradeDate

  return (
    <WorkspaceShell sectionLabel="Data Collector" pageTitle="Limit Board Monitor">
      <div className="space-y-5">
        <header className="flex flex-col justify-between gap-4 border-b border-border/60 pb-4 lg:flex-row lg:items-end">
          <div>
            <div className="flex items-center gap-2 text-xs font-medium text-muted-foreground">
              <Activity className="size-3.5 text-information" />
              封板额 session 监控
            </div>
            <h1 className="mt-2 text-2xl font-semibold tracking-tight">涨跌停封板监控</h1>
            <p className="mt-1 max-w-3xl text-sm text-muted-foreground">
              每只股票一张卡片：分时价与封单额叠在同一盘中时间轴上。上午 09:30–11:30，下午 13:00–15:00。
            </p>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <Input
              type="date"
              value={tradeDate}
              onChange={(event) => { if (event.target.value) setTradeDate(event.target.value) }}
              className="h-8 w-[145px] text-xs"
              aria-label="选择交易日"
            />
            <Tabs value={session} onValueChange={(value) => setSession(value as Session)}>
              <TabsList variant="line" className="h-8">
                <TabsTrigger value="morning" className="px-3 text-xs">上午盘</TabsTrigger>
                <TabsTrigger value="afternoon" className="px-3 text-xs">下午盘</TabsTrigger>
              </TabsList>
            </Tabs>
            <Button type="button" variant="outline" size="sm" onClick={() => void load()} disabled={loading}>
              <RefreshCw className={loading ? "animate-spin" : undefined} />
              刷新
            </Button>
          </div>
        </header>

        <div className="flex flex-wrap items-center gap-x-5 gap-y-2 rounded-lg border border-border/60 bg-muted/25 px-3 py-2.5 text-xs text-muted-foreground">
          <span>交易日 <strong className="font-mono font-medium text-foreground">{day}</strong></span>
          <span>时段 <strong className="font-medium text-foreground">{sessionLabels[session]}</strong></span>
          <span>已规整 <strong className="font-mono font-medium text-foreground">{rows.length}</strong> 只</span>
          {hiddenCount > 0 ? <span>已隐藏未封 <strong className="font-mono font-medium text-foreground">{hiddenCount}</strong> 只</span> : null}
          <span>收盘封板 <strong className="font-mono font-medium text-foreground">{sealedCount}</strong> 只</span>
          <span>平均覆盖 <strong className="font-mono font-medium text-foreground">{percent(avgCoverage)}</strong></span>
          <span className="ml-auto">{payload?.source || "等待汇总数据"}</span>
        </div>

        {error ? (
          <div className="flex items-start gap-2 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2.5 text-sm text-amber-800 dark:border-amber-900/60 dark:bg-amber-950/20 dark:text-amber-300">
            <CircleAlert className="mt-0.5 size-4 shrink-0" />
            <span>{error}</span>
          </div>
        ) : null}

        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 className="text-sm font-semibold">{sessionLabels[session]}封板卡片</h2>
            <p className="mt-1 text-xs text-muted-foreground">红/绿为分时价，蓝色为封单额。进入视口后才拉取分时。</p>
          </div>
          <div className="flex flex-wrap items-center gap-3">
            <div className="flex items-center gap-2">
              <Switch
                id="sealed-only"
                size="sm"
                checked={sealedOnly}
                onCheckedChange={(checked) => setSealedOnly(checked === true)}
              />
              <Label htmlFor="sealed-only" className="text-xs font-normal text-muted-foreground">
                仅曾封板
              </Label>
            </div>
            <Tabs value={direction} onValueChange={(value) => setDirection(value as Direction)}>
              <TabsList variant="line" className="h-8">
                <TabsTrigger value="all" className="px-3 text-xs">全部</TabsTrigger>
                <TabsTrigger value="up" className="px-3 text-xs">涨停</TabsTrigger>
                <TabsTrigger value="down" className="px-3 text-xs">跌停</TabsTrigger>
              </TabsList>
            </Tabs>
          </div>
        </div>

        {loading ? (
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-4">
            {Array.from({ length: 8 }, (_, index) => (
              <Skeleton key={index} className="h-[292px] w-full rounded-lg" />
            ))}
          </div>
        ) : rows.length === 0 ? (
          <div className="rounded-lg border border-dashed border-border/70 px-4 py-14 text-center text-xs text-muted-foreground">
            {payload?.items?.length ? "当前筛选下没有曾封板的股票" : "当前交易日暂无已规整的 session 汇总"}
          </div>
        ) : (
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-4">
            {rows.map((row) => (
              <StockBoardCard
                key={`${row.trade_date}-${row.trade_session}-${row.code}`}
                row={row}
                tradeDate={day}
              />
            ))}
          </div>
        )}
      </div>
    </WorkspaceShell>
  )
}
