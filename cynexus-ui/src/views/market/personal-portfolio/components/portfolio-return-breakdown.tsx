import { useEffect, useMemo, useRef, useState } from "react"
import { ArrowDownRight, ArrowUpRight, CircleHelp, Equal, Grid2X2, List } from "lucide-react"
import * as echarts from "echarts/core"
import { TreemapChart } from "echarts/charts"
import { TooltipComponent } from "echarts/components"
import { CanvasRenderer } from "echarts/renderers"
import { Button } from "@/components/ui/button"
import type { PortfolioOverview, PortfolioPerformance, PortfolioPerformanceRange } from "@/services/market/portfolio/types"

echarts.use([TreemapChart, TooltipComponent, CanvasRenderer])

interface PortfolioReturnBreakdownProps {
  overview: PortfolioOverview | null
  performance?: PortfolioPerformance | null
  range?: PortfolioPerformanceRange
  embedded?: boolean
}

type HoldingHeatmapNode = {
  name: string
  symbol: string
  securityName: string
  value: number
  pnl: number
  returnRate: number
  todayPnl: number | null
  itemStyle: { color: string; borderColor: string; borderWidth: number; gapWidth: number }
  label: { show: boolean; color: string; fontSize: number; fontWeight: number; lineHeight: number; formatter: () => string }
}

function amount(value: number | null | undefined) {
  if (value == null) return "--"
  const numericValue = Number(value)
  return `${numericValue > 0 ? "+" : ""}${numericValue.toFixed(2)}`
}

function tone(value: number | null | undefined) {
  if (value == null || value === 0) return "text-muted-foreground"
  // A-share convention: red indicates a gain, green indicates a loss.
  return value > 0 ? "text-rose-600" : "text-emerald-600"
}

function DirectionIcon({ value }: { value: number | null | undefined }) {
  if (value == null || value === 0) return <Equal className="size-3.5" aria-hidden="true" />
  return value > 0 ? <ArrowUpRight className="size-3.5" aria-hidden="true" /> : <ArrowDownRight className="size-3.5" aria-hidden="true" />
}

export function PortfolioReturnBreakdown({ overview, performance, range = "daily", embedded = false }: PortfolioReturnBreakdownProps) {
  const [view, setView] = useState<"heatmap" | "list">("heatmap")
  const heatmapRef = useRef<HTMLDivElement | null>(null)
  const hasTrades = (overview?.tradeCount ?? 0) > 0
  const realized = overview?.realizedPnl ?? 0
  const unrealized = overview?.unrealizedPnl ?? 0
  const positions = (overview?.positions ?? []).filter((position) => Number(position.quantity) > 0)
  const periodPnl = range === "daily" ? Number(performance?.todayPnl ?? performance?.periodPnl ?? overview?.todayPnl ?? 0) : performance?.periodPnl == null ? null : Number(performance.periodPnl)
  const periodLabel = range === "daily" ? "今日收益" : range === "weekly" ? "本周收益" : range === "monthly" ? "本月收益" : "本年收益"
  const heatmapData = useMemo<HoldingHeatmapNode[]>(() => {
    const visiblePositions = positions
      .filter((position) => Number.isFinite(Number(position.unrealizedPnl)))
      .sort((left, right) => Math.abs(Number(right.unrealizedPnl ?? 0)) - Math.abs(Number(left.unrealizedPnl ?? 0)))
    const maxAbs = Math.max(...visiblePositions.map((position) => Math.abs(Number(position.unrealizedPnl ?? 0))), 1)
    return visiblePositions.map((position) => {
      const pnl = Number(position.unrealizedPnl ?? 0)
      const color = holdingColor(pnl, maxAbs)
      const label = `${position.symbol}\n${signedAmount(pnl)}`
      return {
        name: position.symbol,
        symbol: position.symbol,
        securityName: position.securityName ?? "名称待补充",
        value: Math.max(Math.abs(pnl), 0.01),
        pnl,
        returnRate: Number(position.unrealizedReturnRate ?? 0),
        todayPnl: position.todayPnl == null ? null : Number(position.todayPnl),
        itemStyle: { color: color.background, borderColor: "#ffffff", borderWidth: 2, gapWidth: 4 },
        label: { show: true, color: color.foreground, fontSize: 12, fontWeight: 700, lineHeight: 17, formatter: () => label },
      }
    })
  }, [positions])

  useEffect(() => {
    if (view !== "heatmap" || !heatmapRef.current || !heatmapData.length) return
    const chart = echarts.init(heatmapRef.current, undefined, { renderer: "canvas" })
    chart.setOption({
      backgroundColor: "transparent",
      animation: false,
      tooltip: {
        trigger: "item",
        formatter: (params: { data?: HoldingHeatmapNode }) => {
          const item = params.data
          if (!item) return ""
          const today = item.todayPnl == null ? "--" : signedAmount(item.todayPnl)
          return `<div style="font-weight:700;margin-bottom:6px">${item.symbol} ${item.securityName}</div><div>持仓盈亏：${signedAmount(item.pnl)}</div><div>收益率：${signedPercent(item.returnRate)}</div><div>今日盈亏：${today}</div>`
        },
      },
      series: [{
        type: "treemap",
        left: 0,
        top: 0,
        right: 0,
        bottom: 0,
        roam: false,
        nodeClick: false,
        breadcrumb: { show: false },
        nodeGap: 5,
        label: { show: true },
        labelLayout: { hideOverlap: true },
        data: heatmapData,
      }],
    })
    const observer = new ResizeObserver(() => chart.resize())
    observer.observe(heatmapRef.current)
    return () => { observer.disconnect(); chart.dispose() }
  }, [heatmapData, view])

  return (
    <section className={embedded ? "min-w-0 overflow-hidden" : "overflow-hidden rounded-lg border border-border/60 bg-card"} aria-label="收益表现">

      {hasTrades ? <div className="p-4">
        <div className="flex flex-wrap items-end justify-between gap-4 border-b border-border/60 pb-4">
          <div><p className="text-xs text-muted-foreground">{periodLabel}</p><p className={`mt-1 font-mono text-3xl font-semibold tracking-tight tabular-nums ${tone(periodPnl)}`}>{amount(periodPnl)}</p></div>
          <div className="flex items-center gap-2 text-xs text-muted-foreground"><span className="inline-flex size-6 items-center justify-center rounded-full border border-border/70 bg-muted/35" aria-hidden="true"><DirectionIcon value={periodPnl} /></span><span>{range === "daily" ? "从今日开始重新统计，历史总盈亏不在此处展示" : `按${periodLabel}统计，历史总盈亏不在此处展示`}</span></div>
        </div>

        {positions.length ? <div className="mt-4">
          <div className="mb-2 flex flex-wrap items-center justify-between gap-3"><div><p className="text-xs font-medium text-muted-foreground">当前持仓收益贡献</p><p className="mt-0.5 text-[11px] text-muted-foreground">热力图面积代表收益绝对值，颜色深浅代表贡献大小。</p></div><div className="flex items-center gap-1 rounded-md border border-border/60 bg-background p-1" role="group" aria-label="持仓收益视图"><Button type="button" size="sm" variant={view === "heatmap" ? "secondary" : "ghost"} className="h-7 px-2.5 text-xs" onClick={() => setView("heatmap")}><Grid2X2 className="mr-1.5 size-3.5" />热力图</Button><Button type="button" size="sm" variant={view === "list" ? "secondary" : "ghost"} className="h-7 px-2.5 text-xs" onClick={() => setView("list")}><List className="mr-1.5 size-3.5" />列表</Button></div></div>
          {view === "heatmap" ? <div ref={heatmapRef} aria-label="当前持仓收益热力图" className="h-[260px] w-full" /> : <HoldingPnlList positions={positions} />}
        </div> : <div className="flex min-h-[150px] items-center justify-center px-4 text-center text-xs text-muted-foreground">当前没有仍在持有的仓位，收益主要来自已实现盈亏。</div>}

        <dl className="mt-3 grid gap-2 border-t border-border/60 pt-3 sm:grid-cols-3">
          <Metric label="当前持仓盈亏" value={unrealized} description="市值 − 持仓成本" />
          <Metric label="已实现盈亏" value={realized} description="已卖出部分" />
          <Metric label="持仓成本" value={overview?.costBasis ?? null} description="买入成本 + 手续费" />
        </dl>
        <div className="mt-3 flex items-start gap-2 text-xs leading-5 text-muted-foreground"><CircleHelp className="mt-0.5 size-3.5 shrink-0" aria-hidden="true" /><p>收益图展示当前持仓的浮动盈亏贡献；历史总盈亏不作为新的收益起点，后续收益从今日开始记录。</p></div>
      </div> : <div className="flex min-h-[210px] items-center justify-center px-4 py-10 text-center"><div><p className="text-sm font-medium">确认交易后，这里会显示收益表现</p><p className="mt-1 max-w-md text-xs leading-5 text-muted-foreground">导入截图并确认识别结果后，系统会按成交价格、手续费和最新行情计算收益。</p></div></div>}
    </section>
  )
}

function HoldingPnlList({ positions }: { positions: PortfolioOverview["positions"] }) {
  return <div className="overflow-x-auto rounded-md border border-border/60"><table className="w-full min-w-[520px] text-xs"><thead className="bg-muted/45 text-left text-muted-foreground"><tr><th className="px-3 py-2 font-medium">标的</th><th className="px-3 py-2 text-right font-medium">持仓盈亏</th><th className="px-3 py-2 text-right font-medium">收益率</th><th className="px-3 py-2 text-right font-medium">今日盈亏</th></tr></thead><tbody className="divide-y divide-border/60">{positions.map((position) => <tr key={`${position.instrumentType}-${position.symbol}`} className="hover:bg-muted/30"><td className="px-3 py-2.5"><p className="font-mono font-medium">{position.symbol}</p><p className="mt-0.5 max-w-[220px] truncate text-[11px] text-muted-foreground">{position.securityName ?? "名称待补充"} · {position.instrumentType === "ETF" ? "ETF" : "股票"}</p></td><td className={`px-3 py-2.5 text-right font-mono font-semibold tabular-nums ${tone(position.unrealizedPnl)}`}>{amount(position.unrealizedPnl)}</td><td className={`px-3 py-2.5 text-right font-mono tabular-nums ${tone(position.unrealizedReturnRate)}`}>{signedPercent(Number(position.unrealizedReturnRate ?? 0))}</td><td className={`px-3 py-2.5 text-right font-mono tabular-nums ${tone(position.todayPnl)}`}>{position.todayPnl == null ? "--" : signedAmount(position.todayPnl)}</td></tr>)}</tbody></table></div>
}

function holdingColor(value: number, maxAbs: number) {
  const ratio = Math.min(Math.abs(value) / Math.max(maxAbs, 0.01), 1)
  if (value > 0) return ratio > 0.66 ? { background: "#dc2626", foreground: "#ffffff" } : ratio > 0.33 ? { background: "#f87171", foreground: "#7f1d1d" } : { background: "#fecaca", foreground: "#7f1d1d" }
  if (value < 0) return ratio > 0.66 ? { background: "#16a34a", foreground: "#ffffff" } : ratio > 0.33 ? { background: "#4ade80", foreground: "#14532d" } : { background: "#bbf7d0", foreground: "#14532d" }
  return { background: "#d4d4d4", foreground: "#404040" }
}

function signedAmount(value: number) {
  return `${value > 0 ? "+" : ""}${value.toFixed(2)}`
}

function signedPercent(value: number) {
  return `${value > 0 ? "+" : ""}${(value * 100).toFixed(2)}%`
}

function Metric({ label, value, description }: { label: string; value: number | null; description: string }) {
  return <div><dt className="text-xs text-muted-foreground">{label}</dt><dd className={`mt-1 inline-flex items-center gap-1 font-mono text-sm font-semibold tabular-nums ${tone(value)}`}><DirectionIcon value={value} />{amount(value)}</dd><p className="mt-0.5 text-[11px] text-muted-foreground">{description}</p></div>
}
