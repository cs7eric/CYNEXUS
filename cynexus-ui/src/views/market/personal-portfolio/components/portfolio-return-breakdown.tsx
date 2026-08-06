import { useEffect, useMemo, useRef, useState } from "react"
import { ArrowDownRight, ArrowUpRight, CircleHelp, Equal, Grid2X2, List } from "lucide-react"
import * as echarts from "echarts/core"
import { TreemapChart } from "echarts/charts"
import { TooltipComponent } from "echarts/components"
import { CanvasRenderer } from "echarts/renderers"
import { Button } from "@/components/ui/button"
import type { PortfolioOverview } from "@/services/market/portfolio/types"

echarts.use([TreemapChart, TooltipComponent, CanvasRenderer])

interface PortfolioReturnBreakdownProps {
  overview: PortfolioOverview | null
  embedded?: boolean
  mode?: "unrealized" | "today"
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

export function PortfolioReturnBreakdown({ overview, embedded = false, mode = "unrealized" }: PortfolioReturnBreakdownProps) {
  const isTodayMode = mode === "today"
  const [view, setView] = useState<"heatmap" | "list">("heatmap")
  const heatmapRef = useRef<HTMLDivElement | null>(null)
  const hasTrades = (overview?.tradeCount ?? 0) > 0
  const realized = overview?.realizedPnl ?? 0
  const unrealized = overview?.unrealizedPnl ?? 0
  const positions = (overview?.positions ?? []).filter((position) => Number(position.quantity) > 0)
  const displayPositions = isTodayMode ? positions.filter((position) => position.todayPnl != null) : positions
  const heatmapData = useMemo<HoldingHeatmapNode[]>(() => {
    const visiblePositions = displayPositions
      .filter((position) => Number.isFinite(Number(isTodayMode ? position.todayPnl : position.unrealizedPnl)))
      .sort((left, right) => Math.abs(Number(isTodayMode ? right.todayPnl : right.unrealizedPnl ?? 0)) - Math.abs(Number(isTodayMode ? left.todayPnl : left.unrealizedPnl ?? 0)))
    const maxAbs = Math.max(...visiblePositions.map((position) => Math.abs(Number(isTodayMode ? position.todayPnl : position.unrealizedPnl ?? 0))), 0)
    const maxLogMagnitude = Math.log1p(maxAbs) || 1
    return visiblePositions.map((position) => {
      const pnl = Number(isTodayMode ? position.todayPnl : position.unrealizedPnl ?? 0)
      const color = holdingColor(pnl, maxAbs)
      const securityName = position.securityName ?? "名称待补充"
      const label = `${position.symbol}\n${securityName}\n${isTodayMode ? "今日 " : ""}${signedAmount(pnl)}`
      // Raw P&L makes one outlier consume almost the entire treemap. A
      // logarithmic weight keeps the ranking visible while reserving enough
      // area for smaller positions to remain readable and clickable.
      const normalizedMagnitude = Math.log1p(Math.abs(pnl)) / maxLogMagnitude
      const areaWeight = 0.35 + normalizedMagnitude * 0.65
      return {
        name: position.symbol,
        symbol: position.symbol,
        securityName,
        value: Math.max(areaWeight, 0.35),
        pnl,
        returnRate: Number(position.unrealizedReturnRate ?? 0),
        todayPnl: position.todayPnl == null ? null : Number(position.todayPnl),
        itemStyle: { color: color.background, borderColor: "#ffffff", borderWidth: 2, gapWidth: 4 },
        label: { show: true, color: color.foreground, fontSize: 11, fontWeight: 700, lineHeight: 15, formatter: () => label },
      }
    })
  }, [displayPositions, isTodayMode])

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
          return `<div style="font-weight:700;margin-bottom:6px">${item.symbol} ${item.securityName}</div><div>${isTodayMode ? "今日盈亏" : "持仓盈亏"}：${signedAmount(item.pnl)}</div><div>收益率：${signedPercent(item.returnRate)}</div>${isTodayMode ? "" : `<div>今日盈亏：${today}</div>`}`
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
        data: heatmapData,
      }],
    })
    const observer = new ResizeObserver(() => chart.resize())
    observer.observe(heatmapRef.current)
    return () => { observer.disconnect(); chart.dispose() }
  }, [heatmapData, isTodayMode, view])

  return (
    <section className={embedded ? "min-w-0 overflow-hidden" : "overflow-hidden rounded-lg border border-border/60 bg-card"} aria-label={isTodayMode ? "收益涨跌组成" : "收益表现"}>

      {hasTrades ? isTodayMode ? <GainLossComposition positions={displayPositions} /> : <div className="p-4">
        {displayPositions.length ? <div>
          <div className="mb-2 flex flex-wrap items-center justify-between gap-3"><div><p className="text-xs font-medium text-muted-foreground">当前持仓收益贡献</p><p className="mt-0.5 text-[11px] text-muted-foreground">面积按收益金额压缩分配，颜色表示盈亏方向，具体金额见标签或悬停详情。</p></div><div className="flex items-center gap-1 rounded-md border border-border/60 bg-background p-1" role="group" aria-label="持仓收益视图"><Button type="button" size="sm" variant={view === "heatmap" ? "secondary" : "ghost"} className="h-7 px-2.5 text-xs" onClick={() => setView("heatmap")}><Grid2X2 className="mr-1.5 size-3.5" />热力图</Button><Button type="button" size="sm" variant={view === "list" ? "secondary" : "ghost"} className="h-7 px-2.5 text-xs" onClick={() => setView("list")}><List className="mr-1.5 size-3.5" />列表</Button></div></div>
          {view === "heatmap" ? <div ref={heatmapRef} aria-label="当前持仓收益热力图" className="h-[360px] w-full" /> : <HoldingPnlList positions={displayPositions} mode={mode} />}
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

function GainLossComposition({ positions }: { positions: PortfolioOverview["positions"] }) {
  const values = positions.map((position) => Number(position.todayPnl)).filter(Number.isFinite)
  if (!values.length) {
    return <div className="flex min-h-[210px] items-center justify-center px-4 py-10 text-center text-xs text-muted-foreground">暂无可用的收益涨跌数据。</div>
  }
  const gains = values.filter((value) => value > 0).reduce((sum, value) => sum + value, 0)
  const losses = Math.abs(values.filter((value) => value < 0).reduce((sum, value) => sum + value, 0))
  const net = gains - losses
  const gross = gains + losses
  const gainWidth = gross > 0 ? `${(gains / gross) * 100}%` : "50%"
  const lossWidth = gross > 0 ? `${(losses / gross) * 100}%` : "50%"
  return <div className="p-4">
    <div className="mb-4"><p className="text-xs font-medium text-muted-foreground">收益涨跌组成</p><p className="mt-0.5 text-[11px] text-muted-foreground">按今日收益拆分为上涨收益与下跌损失，不按持仓市值分配面积。</p></div>
    <div className="grid gap-3 sm:grid-cols-2">
      <CompositionMetric label="上涨收益" value={gains} toneClass="text-rose-600" detail="正向收益合计" />
      <CompositionMetric label="下跌损失" value={-losses} toneClass="text-emerald-600" detail="负向收益合计" />
    </div>
    <div className="mt-5">
      <div className="flex h-3 overflow-hidden rounded-full bg-muted" aria-label={`上涨收益占比 ${gainWidth}，下跌损失占比 ${lossWidth}`}><span className="bg-rose-500" style={{ width: gainWidth }} /><span className="bg-emerald-500" style={{ width: lossWidth }} /></div>
      <div className="mt-2 flex items-center justify-between gap-3 text-[11px] text-muted-foreground"><span>上涨收益 {gainWidth}</span><span>净收益 <strong className={tone(net)}>{signedAmount(net)}</strong></span><span>下跌损失 {lossWidth}</span></div>
    </div>
  </div>
}

function CompositionMetric({ label, value, toneClass, detail }: { label: string; value: number; toneClass: string; detail: string }) {
  return <div className="rounded-md bg-muted/35 px-3 py-3"><p className="text-xs text-muted-foreground">{label}</p><p className={`mt-1.5 font-mono text-xl font-semibold tabular-nums ${toneClass}`}>{signedAmount(value)}</p><p className="mt-1 text-[11px] text-muted-foreground">{detail}</p></div>
}

function HoldingPnlList({ positions, mode }: { positions: PortfolioOverview["positions"]; mode: "unrealized" | "today" }) {
  const isTodayMode = mode === "today"
  return <div className="max-h-[360px] overflow-auto rounded-md border border-border/60"><table className="w-full min-w-[520px] text-xs"><thead className="sticky top-0 z-10 bg-muted text-left text-muted-foreground"><tr><th className="px-3 py-2 font-medium">标的</th><th className="px-3 py-2 text-right font-medium">{isTodayMode ? "今日盈亏" : "持仓盈亏"}</th><th className="px-3 py-2 text-right font-medium">收益率</th><th className="px-3 py-2 text-right font-medium">今日盈亏</th></tr></thead><tbody className="divide-y divide-border/60">{positions.map((position) => <tr key={`${position.instrumentType}-${position.symbol}`} className="hover:bg-muted/30"><td className="px-3 py-2.5"><p className="font-mono font-medium">{position.symbol}</p><p className="mt-0.5 max-w-[220px] truncate text-[11px] text-muted-foreground">{position.securityName ?? "名称待补充"} · {position.instrumentType === "ETF" ? "ETF" : "股票"}</p></td><td className={`px-3 py-2.5 text-right font-mono font-semibold tabular-nums ${tone(isTodayMode ? position.todayPnl : position.unrealizedPnl)}`}>{isTodayMode ? position.todayPnl == null ? "--" : signedAmount(position.todayPnl) : amount(position.unrealizedPnl)}</td><td className={`px-3 py-2.5 text-right font-mono tabular-nums ${tone(position.unrealizedReturnRate)}`}>{signedPercent(Number(position.unrealizedReturnRate ?? 0))}</td><td className={`px-3 py-2.5 text-right font-mono tabular-nums ${tone(position.todayPnl)}`}>{position.todayPnl == null ? "--" : signedAmount(position.todayPnl)}</td></tr>)}</tbody></table></div>
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
