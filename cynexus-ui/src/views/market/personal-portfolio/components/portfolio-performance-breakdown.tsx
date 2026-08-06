import { useEffect, useMemo, useRef, useState } from "react"
import { CircleHelp, Grid2X2, List } from "lucide-react"
import * as echarts from "echarts/core"
import { TreemapChart } from "echarts/charts"
import { TooltipComponent } from "echarts/components"
import { CanvasRenderer } from "echarts/renderers"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import type { PortfolioPerformance, PortfolioPerformanceContribution, PortfolioPerformanceRange } from "@/services/market/portfolio/types"

echarts.use([TreemapChart, TooltipComponent, CanvasRenderer])

interface PortfolioPerformanceBreakdownProps {
  performance: PortfolioPerformance | null
  range: PortfolioPerformanceRange
  loading?: boolean
  embedded?: boolean
}

type PerformanceHeatmapNode = {
  name: string
  symbol: string
  securityName: string
  instrumentType: string
  periodPnl: number
  itemStyle: { color: string; borderColor: string; borderWidth: number; gapWidth: number }
  label: { show: boolean; color: string; fontSize: number; fontWeight: number; lineHeight: number; formatter: () => string }
  value: number
}

const EMPTY_CONTRIBUTIONS: PortfolioPerformanceContribution[] = []

export function PortfolioPerformanceBreakdown({ performance, range, loading = false, embedded = false }: PortfolioPerformanceBreakdownProps) {
  const [view, setView] = useState<"heatmap" | "list">("heatmap")
  const heatmapRef = useRef<HTMLDivElement | null>(null)
  const contributions = performance?.contributions ?? EMPTY_CONTRIBUTIONS
  const nodes = useMemo(() => createNodes(contributions), [contributions])
  const gains = contributions.reduce((sum, item) => sum + Math.max(Number(item.periodPnl ?? 0), 0), 0)
  const losses = contributions.reduce((sum, item) => sum + Math.min(Number(item.periodPnl ?? 0), 0), 0)
  const net = contributions.reduce((sum, item) => sum + Number(item.periodPnl ?? 0), 0)
  const viewName = range === "daily" ? "日" : range === "weekly" ? "周" : range === "monthly" ? "月" : range === "yearly" ? "年" : "自定义"

  useEffect(() => {
    if (view !== "heatmap" || !heatmapRef.current || !nodes.length) return
    const chart = echarts.init(heatmapRef.current, undefined, { renderer: "canvas" })
    chart.setOption({
      backgroundColor: "transparent",
      animation: false,
      tooltip: {
        trigger: "item",
        formatter: (params: { data?: PerformanceHeatmapNode }) => {
          const item = params.data
          if (!item) return ""
          return `<div style="font-weight:700;margin-bottom:6px">${item.symbol} ${item.securityName}</div><div>类型：${item.instrumentType}</div><div>视图收益贡献：${signedAmount(item.periodPnl)}</div>`
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
        nodeGap: 4,
        squareRatio: 1.2,
        label: { show: true },
        labelLayout: { hideOverlap: true },
        data: nodes,
      }],
    })
    const observer = new ResizeObserver(() => chart.resize())
    observer.observe(heatmapRef.current)
    return () => { observer.disconnect(); chart.dispose() }
  }, [nodes, view])

  return <section className={embedded ? "min-w-0 overflow-hidden" : "overflow-hidden rounded-lg border border-border/60 bg-card"} aria-label="视图持仓收益贡献">
    {loading ? <div className="space-y-3 p-4"><Skeleton className="h-8 w-2/3" /><Skeleton className="h-[300px] w-full" /></div> : contributions.length ? <div className="p-4">
      <div className="mb-2 flex flex-wrap items-center justify-between gap-3"><div><p className="text-xs font-medium text-muted-foreground">{viewName}视图持仓收益贡献</p><p className="mt-0.5 text-[11px] text-muted-foreground">每个方块代表一个标的，金额是该标的在当前视图区间内累计产生的收益。</p></div><div className="flex items-center gap-1 rounded-md border border-border/60 bg-background p-1" role="group" aria-label="持仓收益贡献展示方式"><Button type="button" size="sm" variant={view === "heatmap" ? "secondary" : "ghost"} className="h-7 px-2.5 text-xs" onClick={() => setView("heatmap")}><Grid2X2 className="mr-1.5 size-3.5" />热力图</Button><Button type="button" size="sm" variant={view === "list" ? "secondary" : "ghost"} className="h-7 px-2.5 text-xs" onClick={() => setView("list")}><List className="mr-1.5 size-3.5" />列表</Button></div></div>
      {view === "heatmap" ? <div ref={heatmapRef} aria-label="视图持仓收益贡献热力图" className="h-[360px] w-full" /> : <PerformanceContributionList contributions={contributions} />}
      <dl className="mt-3 grid gap-2 border-t border-border/60 pt-3 sm:grid-cols-3"><Metric label="上涨贡献" value={gains} detail="正收益标的合计" /><Metric label="下跌贡献" value={losses} detail="负收益标的合计" /><Metric label="视图净收益" value={net} detail={`${contributions.length} 个收益标的`} /></dl>
      <div className="mt-3 flex items-start gap-2 text-xs leading-5 text-muted-foreground"><CircleHelp className="mt-0.5 size-3.5 shrink-0" aria-hidden="true" /><p>贡献金额由视图内各交易日的持仓收益逐日累计；已清仓但在区间内产生收益的标的也会保留。</p></div>
    </div> : <div className="flex min-h-[210px] items-center justify-center px-4 py-10 text-center"><div><p className="text-sm font-medium">暂无视图持仓收益贡献</p><p className="mt-1 max-w-md text-xs leading-5 text-muted-foreground">当前视图区间内没有产生非零收益的标的。</p></div></div>}
  </section>
}

function PerformanceContributionList({ contributions }: { contributions: PortfolioPerformanceContribution[] }) {
  const gross = contributions.reduce((sum, item) => sum + Math.abs(Number(item.periodPnl ?? 0)), 0)
  return <div className="max-h-[360px] overflow-auto rounded-md border border-border/60"><table className="w-full min-w-[500px] text-xs"><thead className="sticky top-0 z-10 bg-muted text-left text-muted-foreground"><tr><th className="px-3 py-2 font-medium">标的</th><th className="px-3 py-2 text-right font-medium">视图收益贡献</th><th className="px-3 py-2 text-right font-medium">波动占比</th></tr></thead><tbody className="divide-y divide-border/60">{contributions.map((item) => <tr key={`${item.instrumentType}-${item.symbol}`} className="hover:bg-muted/30"><td className="px-3 py-2.5"><p className="font-mono font-medium">{item.symbol}</p><p className="mt-0.5 max-w-[240px] truncate text-[11px] text-muted-foreground">{item.securityName ?? "名称待补充"} · {item.instrumentType === "ETF" ? "ETF" : "股票"}</p></td><td className={`px-3 py-2.5 text-right font-mono font-semibold tabular-nums ${tone(item.periodPnl)}`}>{signedAmount(Number(item.periodPnl ?? 0))}</td><td className="px-3 py-2.5 text-right font-mono tabular-nums text-muted-foreground">{gross > 0 ? `${(Math.abs(Number(item.periodPnl ?? 0)) / gross * 100).toFixed(1)}%` : "0.0%"}</td></tr>)}</tbody></table></div>
}

function createNodes(contributions: PortfolioPerformanceContribution[]): PerformanceHeatmapNode[] {
  const maxAbs = Math.max(...contributions.map((item) => Math.abs(Number(item.periodPnl ?? 0))), 0)
  const maxLogMagnitude = Math.log1p(maxAbs) || 1
  return contributions.map((item) => {
    const periodPnl = Number(item.periodPnl ?? 0)
    const securityName = item.securityName ?? "名称待补充"
    const labelText = `${item.symbol}\n${securityName}\n${signedAmount(periodPnl)}`
    const normalizedMagnitude = Math.log1p(Math.abs(periodPnl)) / maxLogMagnitude
    return { name: item.symbol, symbol: item.symbol, securityName, instrumentType: item.instrumentType, periodPnl, value: 0.35 + normalizedMagnitude * 0.65, itemStyle: { color: heatmapColor(periodPnl, maxAbs), borderColor: "#ffffff", borderWidth: 2, gapWidth: 4 }, label: { show: true, color: periodPnl === 0 ? "#404040" : "#ffffff", fontSize: 11, fontWeight: 700, lineHeight: 15, formatter: () => labelText } }
  })
}

function heatmapColor(value: number, maxAbs: number) {
  const ratio = Math.min(Math.abs(value) / Math.max(maxAbs, 0.01), 1)
  if (value > 0) return ratio > 0.66 ? "#dc2626" : ratio > 0.33 ? "#f87171" : "#fecaca"
  if (value < 0) return ratio > 0.66 ? "#16a34a" : ratio > 0.33 ? "#4ade80" : "#bbf7d0"
  return "#d4d4d4"
}

function signedAmount(value: number) {
  return `${value > 0 ? "+" : ""}${value.toFixed(2)}`
}

function tone(value: number | null | undefined) {
  if (value == null || value === 0) return "text-muted-foreground"
  return value > 0 ? "text-rose-600" : "text-emerald-600"
}

function Metric({ label, value, detail }: { label: string; value: number; detail: string }) {
  return <div><dt className="text-xs text-muted-foreground">{label}</dt><dd className={`mt-1 font-mono text-sm font-semibold tabular-nums ${tone(value)}`}>{signedAmount(value)}</dd><p className="mt-0.5 text-[11px] text-muted-foreground">{detail}</p></div>
}
