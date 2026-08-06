import { useEffect, useMemo, useRef, useState } from "react"
import { BrainCircuit, ExternalLink, Loader2, RefreshCw } from "lucide-react"
import * as echarts from "echarts/core"
import { LineChart } from "echarts/charts"
import { DataZoomComponent, GridComponent, LegendComponent, ToolboxComponent, TooltipComponent } from "echarts/components"
import { CanvasRenderer } from "echarts/renderers"

import { Button } from "@/components/ui/button"
import { AiBindingProviderSelect } from "@/components/ai/ai-binding-provider-select"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import DogLoader from "@/components/loader/dog-loader"
import {
  fetchFuturesPositionLatest,
  fetchFuturesPositionHistory,
  fetchFuturesPositionMembers,
  type FuturesPositionHistory,
  type FuturesPositionInstrument,
  type FuturesPositionMember,
  type FuturesPositionLatestResponse,
  type FuturesScopeType,
} from "@/services/market/futures-position"
import { analyzeFuturesPosition, isFuturesPositionAiVariety, type FuturesPositionAiResult } from "@/services/ai/futures-position"
import { cn } from "@/lib/utils"
import { formatDate, formatPosition, formatRatio, formatSigned, toneForValue } from "../lib/format"
import { varietyLabel } from "../lib/variety"

echarts.use([LineChart, DataZoomComponent, GridComponent, LegendComponent, ToolboxComponent, TooltipComponent, CanvasRenderer])

type SeatView = "DUAL" | "NET" | "OPEN" | "CLOSE" | "PRICE" | "HISTORY"
type HistoryRange = 5 | 10 | 20 | 60

const HISTORY_WINDOWS: HistoryRange[] = [5, 10, 20, 60]
const HISTORY_LIMIT = 1100
const HISTORY_ANALYSIS_YEARS = 3

interface ResearchWorkbenchProps {
  payload: FuturesPositionLatestResponse | null
  selected: FuturesPositionInstrument | null
  loading: boolean
  scopeType: FuturesScopeType
  exchangeCode?: string | null
  top: number
  tradeDate?: string | null
  onSelect: (row: FuturesPositionInstrument) => void
  onOpenDetails: () => void
}

interface SeatRow {
  rankNo: number
  memberName: string
  rankValue: number
  rankChange?: number | null
}

const seatTabs: Array<{ value: SeatView; label: string }> = [
  { value: "DUAL", label: "多空对照" },
  { value: "NET", label: "净持仓" },
  { value: "OPEN", label: "建仓席位" },
  { value: "CLOSE", label: "减仓席位" },
  { value: "PRICE", label: "持仓与价格" },
  { value: "HISTORY", label: "历史明细" },
]

const INDEX_SHORTCUTS = [
  { code: "IF", label: "沪深300", varietyCode: "IF" },
  { code: "IH", label: "上证50", varietyCode: "IH" },
  { code: "IM", label: "中证1000", varietyCode: "IM" },
  { code: "IC", label: "中证500", varietyCode: "IC" },
] as const

function netChange(row: FuturesPositionInstrument) {
  return (row.longChange ?? 0) - (row.shortChange ?? 0)
}

function directionLabel(value: number) {
  if (value > 0) return "净多扩大"
  if (value < 0) return "净空扩大"
  return "结构变化有限"
}

function deriveMovers(rows: FuturesPositionInstrument[]) {
  return [...rows]
    .sort((a, b) => Math.abs(netChange(b)) - Math.abs(netChange(a)))
    .slice(0, 4)
}

function historyData(history: FuturesPositionHistory[]) {
  return [...history]
    .sort((a, b) => a.tradeDate.localeCompare(b.tradeDate))
    .map((item) => ({
      ...item,
      volume: item.volume == null ? null : Number(item.volume),
      longPosition: item.longPosition == null ? null : Number(item.longPosition),
      shortPosition: item.shortPosition == null ? null : Number(item.shortPosition),
      longShortRatio: item.longShortRatio == null ? null : Number(item.longShortRatio),
      date: formatDate(item.tradeDate).slice(5),
    }))
}

function analysisHistory(history: FuturesPositionHistory[]) {
  const ordered = [...history].sort((a, b) => a.tradeDate.localeCompare(b.tradeDate))
  const latestDate = ordered.at(-1)?.tradeDate
  if (!latestDate) return ordered
  const cutoff = new Date(`${latestDate}T00:00:00Z`)
  cutoff.setUTCFullYear(cutoff.getUTCFullYear() - HISTORY_ANALYSIS_YEARS)
  return ordered.filter((item) => item.tradeDate >= cutoff.toISOString().slice(0, 10))
}

function metricPercentile(history: FuturesPositionHistory[], current: number | null | undefined, select: (item: FuturesPositionHistory) => number | null | undefined) {
  return percentileRank(current, history.map(select).filter((value): value is number => value != null && Number.isFinite(value)))
}

function buildNetRows(members: FuturesPositionMember[]) {
  const grouped = new Map<string, SeatRow>()
  members.filter((member) => member.rankType === "LONG" || member.rankType === "SHORT").forEach((member) => {
    const name = member.memberName || "未披露"
    const current = grouped.get(name) ?? { rankNo: 0, memberName: name, rankValue: 0, rankChange: 0 }
    const sign = member.rankType === "SHORT" ? -1 : 1
    current.rankValue += sign * member.rankValue
    current.rankChange = (current.rankChange ?? 0) + sign * (member.rankChange ?? 0)
    grouped.set(name, current)
  })
  return [...grouped.values()]
    .sort((a, b) => b.rankValue - a.rankValue)
    .map((row, index) => ({ ...row, rankNo: index + 1 }))
}

function buildFlowRows(members: FuturesPositionMember[], direction: "OPEN" | "CLOSE") {
  const grouped = new Map<string, SeatRow>()
  members.filter((member) => member.rankType === "LONG" || member.rankType === "SHORT").forEach((member) => {
    const name = member.memberName || "未披露"
    const current = grouped.get(name) ?? { rankNo: 0, memberName: name, rankValue: 0, rankChange: 0 }
    current.rankValue += member.rankValue
    current.rankChange = (current.rankChange ?? 0) + (member.rankChange ?? 0)
    grouped.set(name, current)
  })
  return [...grouped.values()]
    .filter((row) => direction === "OPEN" ? (row.rankChange ?? 0) > 0 : (row.rankChange ?? 0) < 0)
    .sort((a, b) => direction === "OPEN" ? (b.rankChange ?? 0) - (a.rankChange ?? 0) : (a.rankChange ?? 0) - (b.rankChange ?? 0))
    .map((row, index) => ({ ...row, rankNo: index + 1 }))
}

function percentileRank(value: number | null | undefined, values: number[]) {
  if (value == null || !Number.isFinite(value) || !values.length) return null
  if (values.length === 1) return 50
  const rank = values.filter((item) => item <= value).length
  return Math.round(((rank - 1) / (values.length - 1)) * 100)
}

function percentileLabel(value: number | null) {
  if (value == null) return "暂无"
  if (value >= 80) return "历史偏高"
  if (value >= 60) return "略偏高"
  if (value <= 20) return "历史偏低"
  if (value <= 40) return "略偏低"
  return "历史中性"
}

function percentileTone(value: number | null) {
  if (value == null || (value > 40 && value < 60)) return "text-foreground"
  return value >= 60 ? "text-rose-600 dark:text-rose-400" : "text-emerald-600 dark:text-emerald-400"
}

function SparkBar({ value, maxValue }: { value: number; maxValue: number }) {
  const width = Math.min(100, Math.max(8, (Math.abs(value) / Math.max(1, maxValue)) * 100))
  return <div className="mt-2 h-1.5 w-full rounded-full bg-muted"><div className={cn("h-full rounded-full", value >= 0 ? "bg-rose-500/75" : "bg-emerald-500/75")} style={{ width: `${width}%` }} /></div>
}

function IndexSnapshotCard({ item, row, loading, selected, onSelect }: { item: (typeof INDEX_SHORTCUTS)[number]; row?: FuturesPositionInstrument; loading: boolean; selected: boolean; onSelect: (row: FuturesPositionInstrument) => void }) {
  return <button type="button" disabled={loading || !row} onClick={() => { if (row) onSelect(row) }} className={cn("min-w-0 px-3 py-2.5 text-left transition-colors focus-visible:z-10 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring", row && !loading ? "hover:bg-muted/30" : "cursor-not-allowed opacity-70", selected && "bg-information/5")} aria-label={`查看${item.label}持仓分析`} aria-pressed={selected}>
    <div aria-hidden="true">
    <div className="flex items-center gap-2">
      <span className="truncate text-xs font-medium">{item.label}</span>
      <span className="font-mono text-[10px] text-muted-foreground">{item.code}</span>
      <span className="ml-auto text-[10px] text-muted-foreground">CFFEX</span>
    </div>
    {loading ? <div className="mt-2 grid grid-cols-3 gap-2 text-[10px] text-muted-foreground"><span className="h-3 rounded bg-muted" /><span className="h-3 rounded bg-muted" /><span className="h-3 rounded bg-muted" /></div> : row ? <>
      <div className="mt-2 flex items-baseline gap-2">
        <span className="text-[10px] text-muted-foreground">净持仓</span>
        <strong className={cn("font-mono text-sm tabular-nums", toneForValue(row.netPosition))}>{formatSigned(row.netPosition)}</strong>
        <span className={cn("ml-auto font-mono text-[10px] tabular-nums", toneForValue(netChange(row)))}>{formatSigned(netChange(row))} 日变化</span>
      </div>
      <div className="mt-1 flex gap-3 text-[10px] text-muted-foreground"><span>多 <b className="font-mono text-foreground">{formatPosition(row.longPosition)}</b></span><span>空 <b className="font-mono text-foreground">{formatPosition(row.shortPosition)}</b></span><span>比 <b className="font-mono text-foreground">{formatRatio(row.longShortRatio)}</b></span></div>
    </> : <div className="mt-2 text-[10px] text-muted-foreground">暂无当前快照</div>}
    </div>
  </button>
}

function ScanStrip({ rows, indexRows, indexLoading, selected, onSelect }: { rows: FuturesPositionInstrument[]; indexRows: FuturesPositionInstrument[]; indexLoading: boolean; selected: FuturesPositionInstrument | null; onSelect: (row: FuturesPositionInstrument) => void }) {
  const movers = useMemo(() => deriveMovers(rows), [rows])
  const maxDelta = Math.max(1, ...movers.map((row) => Math.abs(netChange(row))))
  return <section className="overflow-hidden rounded-lg border border-border/70 bg-card" aria-label="今日持仓异动">
    <div className="grid grid-cols-1 xl:grid-cols-[176px_minmax(0,1fr)]">
      <div className="border-b border-border/60 bg-muted/25 px-4 py-3 xl:row-span-2 xl:border-b-0 xl:border-r">
        <h2 className="text-sm font-semibold">今日持仓异动</h2>
        <p className="mt-1 text-[11px] leading-4 text-muted-foreground">按净持仓变化排序，同时展示变化方向和增量。</p>
        <span className="mt-3 inline-flex text-[11px] font-medium text-information">查看全部 {rows.length} 个标的</span>
      </div>
      <div className="grid min-h-[132px] grid-cols-2 divide-x divide-border/60 xl:grid-cols-4">
        {movers.map((row) => {
          const delta = netChange(row)
          const isSelected = row.instrumentCode === selected?.instrumentCode
          return <button key={row.instrumentCode} type="button" onClick={() => onSelect(row)} className={cn("relative min-w-0 px-3 py-3 text-left transition-colors hover:bg-muted/30 focus-visible:z-10 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring", isSelected && "bg-information/5") }>
            {isSelected ? <span className="absolute inset-x-0 top-0 h-0.5 bg-information" /> : null}
            <div className="flex items-center gap-1.5"><span className="font-mono text-sm font-semibold">{row.instrumentCode}</span><span className="truncate text-[10px] text-muted-foreground">{varietyLabel(row.varietyCode).split(" · ").at(-1)}</span><span className="ml-auto shrink-0 text-[10px] text-muted-foreground">{row.exchangeCode || "-"}</span></div>
            <div className="mt-2 flex items-baseline gap-2"><strong className={cn("font-mono text-base tabular-nums", toneForValue(delta))}>{formatSigned(delta)}</strong><span className="text-[10px] text-muted-foreground">{directionLabel(delta)}</span></div>
            <div className="mt-1 flex gap-3 text-[10px] text-muted-foreground"><span>多 <b className="font-mono text-foreground">{formatSigned(row.longChange)}</b></span><span>空 <b className="font-mono text-foreground">{formatSigned(row.shortChange)}</b></span></div>
            <SparkBar value={delta} maxValue={maxDelta} />
          </button>
        })}
        {!movers.length ? <div className="col-span-full grid place-items-center px-4 text-xs text-muted-foreground">当前筛选没有可用异动数据</div> : null}
      </div>
      <div className="grid min-h-[72px] grid-cols-2 border-t border-border/60 divide-x divide-border/60 xl:grid-cols-4">
        {INDEX_SHORTCUTS.map((item) => {
          const row = indexRows.find((candidate) => candidate.varietyCode?.toUpperCase() === item.varietyCode)
          return <IndexSnapshotCard key={item.label} item={item} row={row} loading={indexLoading} selected={selected?.exchangeCode === row?.exchangeCode && selected?.instrumentCode === row?.instrumentCode} onSelect={onSelect} />
        })}
      </div>
    </div>
  </section>
}

function HistoryWindowSummary({ history, range, onRangeChange }: { history: FuturesPositionHistory[]; range: HistoryRange; onRangeChange: (range: HistoryRange) => void }) {
  const windows = useMemo(() => {
    const ordered = historyData(history)
    return HISTORY_WINDOWS.map((days) => {
    const points = ordered.slice(-days)
    const first = points[0]
    const latest = points.at(-1)
    return {
      days,
      actual: points.length,
      startDate: first?.tradeDate,
      endDate: latest?.tradeDate,
      netChange: latest && first ? (latest.netPosition ?? 0) - (first.netPosition ?? 0) : null,
      longChange: latest && first ? (latest.longPosition ?? 0) - (first.longPosition ?? 0) : null,
      shortChange: latest && first ? (latest.shortPosition ?? 0) - (first.shortPosition ?? 0) : null,
    }
    })
  }, [history])

  return <aside className="border-t border-border/60 lg:border-l lg:border-t-0 xl:col-span-3 xl:border-l-0 xl:border-t min-[1800px]:col-span-1 min-[1800px]:border-l min-[1800px]:border-t-0" aria-label="历史窗口摘要">
    <div className="divide-y divide-border/60 xl:grid xl:grid-cols-4 xl:divide-x xl:divide-y-0 min-[1800px]:block min-[1800px]:divide-x-0 min-[1800px]:divide-y">
      {windows.map((item) => <button key={item.days} type="button" disabled={!item.actual} onClick={() => onRangeChange(item.days)} aria-pressed={range === item.days} className={cn("block w-full px-4 py-3 text-left transition-colors hover:bg-muted/35 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-55", range === item.days && "bg-information/5") }>
        <div className="flex items-center justify-between gap-2"><span className="text-xs font-medium">近{item.days}个交易日</span><span className="font-mono text-[10px] text-muted-foreground">{item.actual}/{item.days}</span></div>
        <div className="mt-1 font-mono text-[10px] text-muted-foreground">{item.startDate && item.endDate ? `${formatDate(item.startDate).slice(5)}–${formatDate(item.endDate).slice(5)}` : "暂无日期区间"}</div>
        {item.actual ? <><div className="mt-2 flex items-baseline justify-between gap-2"><span className="text-[10px] text-muted-foreground">净持仓变化</span><span className={cn("font-mono text-sm font-semibold tabular-nums", toneForValue(item.netChange))}>{formatSigned(item.netChange)}</span></div><div className="mt-1 flex gap-3 text-[10px] text-muted-foreground"><span>多 <b className={cn("font-mono", toneForValue(item.longChange))}>{formatSigned(item.longChange)}</b></span><span>空 <b className={cn("font-mono", toneForValue(item.shortChange))}>{formatSigned(item.shortChange)}</b></span></div></> : <div className="mt-2 text-[10px] text-muted-foreground">暂无有效交易日</div>}
      </button>)}
    </div>
  </aside>
}

function StructureChart({ history, range, row, selectedExchange, top, detailsLoading, activeDetailsError, onOpenDetails }: { history: FuturesPositionHistory[]; range: HistoryRange; row: FuturesPositionInstrument; selectedExchange?: string | null; top: number; detailsLoading: boolean; activeDetailsError: string | null; onOpenDetails: () => void }) {
  const data = useMemo(() => historyData(analysisHistory(history)), [history])
  const visibleData = data.slice(-range)
  const hasSinglePoint = visibleData.length === 1
  const fullStartDate = data[0]?.tradeDate
  const fullEndDate = data.at(-1)?.tradeDate
  const visibleStartDate = visibleData[0]?.tradeDate
  const visibleEndDate = visibleData.at(-1)?.tradeDate
  const chartRef = useRef<HTMLDivElement | null>(null)

  useEffect(() => {
    if (!chartRef.current || !data.length) return
    const chart = echarts.init(chartRef.current, undefined, { renderer: "canvas" })
    const positionFormatter = (value: number) => `${Math.round(value / 1000)}K`
    const compressedMin = ({ min, max }: { min: number; max: number }) => {
      const spread = Math.max(max - min, Math.abs(max) * 0.02, 1)
      return Math.max(0, Math.floor(min - spread * 0.1))
    }
    const compressedMax = ({ min, max }: { min: number; max: number }) => {
      const spread = Math.max(max - min, Math.abs(max) * 0.02, 1)
      return Math.ceil(max + spread * 0.1)
    }
    const dateLabels = data.map((item) => item.tradeDate)
    const zoomStart = data.length <= range ? 0 : ((data.length - range) / data.length) * 100
    const option: echarts.EChartsCoreOption = {
      animation: false,
      grid: { top: 36, right: 48, bottom: 88, left: 42, containLabel: true },
      legend: { top: 6, left: "center", itemWidth: 14, itemHeight: 8, textStyle: { color: "#757575", fontSize: 10 } },
      toolbox: {
        top: 0,
        right: 6,
        itemSize: 13,
        feature: {
          dataZoom: { yAxisIndex: "none", title: { zoom: "框选缩放", back: "还原缩放" } },
          restore: { title: "还原" },
          saveAsImage: { title: "保存图片", name: "期货持仓结构变化" },
        },
      },
      dataZoom: [
        { type: "inside", start: zoomStart, end: 100, filterMode: "filter" },
        { type: "slider", start: zoomStart, end: 100, filterMode: "filter", bottom: 20, height: 20, showDetail: false, borderColor: "transparent", backgroundColor: "#f7f7f7", fillerColor: "rgba(0,113,227,0.12)", handleStyle: { color: "#ffffff", borderColor: "#0071e3" }, dataBackground: { lineStyle: { color: "#a3a3a3" }, areaStyle: { color: "rgba(117,117,117,0.08)" } } },
      ],
      tooltip: {
        trigger: "axis",
        axisPointer: { type: "line" },
        position: (point: number[]) => [point[0], "10%"],
        formatter: (params: unknown) => {
          const items = Array.isArray(params) ? params as Array<{ axisValue?: string; marker?: string; seriesName?: string; value?: number | null }> : []
          const date = items[0]?.axisValue ?? ""
          const rows = items.map((item) => {
            const value = item.value == null ? "-" : item.seriesName === "多空比" ? formatRatio(item.value) : formatPosition(item.value)
            return `<div>${item.marker ?? ""}${item.seriesName ?? ""}<span style="float:right;margin-left:20px;font-family:monospace">${value}</span></div>`
          }).join("")
          return `<div style="min-width:160px"><div style="margin-bottom:6px;color:#757575">交易日 ${formatDate(date)}</div>${rows}</div>`
        },
      },
      xAxis: {
        type: "category",
        data: dateLabels,
        boundaryGap: false,
        axisLine: { lineStyle: { color: "#ebebeb" } },
        axisTick: { show: false },
        axisLabel: {
          color: "#757575",
          fontSize: 10,
          interval: "auto",
          hideOverlap: true,
          formatter: (value: string) => value.slice(5).replace("-", "/"),
        },
      },
      yAxis: [
        { type: "value", name: "持仓", scale: true, min: compressedMin, max: compressedMax, splitNumber: 5, nameTextStyle: { color: "#757575", fontSize: 10 }, axisLabel: { color: "#757575", fontSize: 10, formatter: positionFormatter }, axisLine: { show: false }, axisTick: { show: false }, splitLine: { lineStyle: { color: "#ebebeb" } } },
        { type: "value", name: "比值", scale: true, min: compressedMin, max: compressedMax, splitNumber: 5, nameTextStyle: { color: "#0071e3", fontSize: 10 }, axisLabel: { color: "#0071e3", fontSize: 10, formatter: (value: number) => `${value.toFixed(1)}x` }, axisLine: { show: true, lineStyle: { color: "rgba(0,113,227,0.35)" } }, axisTick: { show: false }, splitLine: { show: false } },
      ],
      series: [
        { name: "多头持仓", type: "line", yAxisIndex: 0, data: data.map((item) => item.longPosition), symbol: "none", sampling: "lttb", connectNulls: true, lineStyle: { width: 2, color: "#e11d48" }, itemStyle: { color: "#e11d48" }, areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: "rgba(225,29,72,0.28)" }, { offset: 1, color: "rgba(225,29,72,0.02)" }]) } },
        { name: "空头持仓", type: "line", yAxisIndex: 0, data: data.map((item) => item.shortPosition), symbol: "none", sampling: "lttb", connectNulls: true, lineStyle: { width: 2, color: "#059669" }, itemStyle: { color: "#059669" }, areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: "rgba(5,150,105,0.22)" }, { offset: 1, color: "rgba(5,150,105,0.02)" }]) } },
        { name: "多空比", type: "line", yAxisIndex: 1, data: data.map((item) => item.longShortRatio), symbol: "none", sampling: "lttb", connectNulls: true, z: 3, lineStyle: { width: 2.5, type: "dashed", color: "#0071e3" }, itemStyle: { color: "#0071e3" }, emphasis: { focus: "series", lineStyle: { width: 3 } } },
      ],
    }
    chart.setOption(option)
    const resizeObserver = new ResizeObserver(() => chart.resize())
    resizeObserver.observe(chartRef.current)
    return () => {
      resizeObserver.disconnect()
      chart.dispose()
    }
  }, [data, range])

  return <div className="min-w-0" aria-busy={detailsLoading}>
    <div className="flex flex-wrap items-start justify-between gap-3 border-b border-border/60 px-4 py-3">
      <div className="min-w-0">
        <div className="flex flex-wrap items-baseline gap-x-3 gap-y-1">
          <div className="truncate text-base font-semibold leading-5 tracking-tight"><span className="font-mono">{row.instrumentCode}</span> · {varietyLabel(row.varietyCode).split(" · ").at(-1)}</div>
          <div className="text-[11px] text-muted-foreground">{row.exchangeCode || selectedExchange || "-"} · 前 {top} 名会员持仓 · 单位：手</div>
        </div>
        <div className="mt-2"><p className="text-[11px] text-muted-foreground">{hasSinglePoint ? "当前仅有 1 个有效交易日，显示快照点；需要至少 2 个交易日才能形成趋势线。" : `完整历史 ${formatDate(fullStartDate)}–${formatDate(fullEndDate)} · 默认窗口 ${formatDate(visibleStartDate)}–${formatDate(visibleEndDate)}（${visibleData.length}个有效交易日）`}</p></div>
      </div>
      <div className="ml-auto flex flex-wrap items-start justify-end gap-2">
        <span className="hidden text-[10px] text-muted-foreground xl:inline">{detailsLoading ? "明细加载中" : activeDetailsError ? "明细加载失败" : "明细已更新"}</span>
        <Button type="button" variant="secondary" size="sm" onClick={onOpenDetails} className="h-7 gap-1.5 text-xs"><ExternalLink className="size-3.5" />会员明细</Button>
      </div>
    </div>
    <div className="relative h-[390px] px-2 pb-2 pt-3">
      {data.length ? <div ref={chartRef} className={cn("h-[378px] w-full transition-opacity duration-200", detailsLoading ? "opacity-35" : "opacity-100")} role="img" aria-label={`持仓结构变化，完整历史从${formatDate(fullStartDate)}到${formatDate(fullEndDate)}，默认展示近${range}个有效交易日`} /> : <div className="grid h-full place-items-center text-xs text-muted-foreground">暂无历史结构数据</div>}
      {detailsLoading ? <div className="absolute inset-2 z-10 grid place-items-center rounded-md bg-card/85 backdrop-blur-[1px]" aria-live="polite">
        <DogLoader size={25} label={`正在切换 ${row.instrumentCode}，加载持仓结构与历史数据...`} />
      </div> : null}
    </div>
  </div>
}

function CurrentStructure({ row, history, top }: { row: FuturesPositionInstrument; history: FuturesPositionHistory[]; top: number }) {
  const analysis = useMemo(() => analysisHistory(history), [history])
  const long = row.longPosition ?? 0
  const short = row.shortPosition ?? 0
  const total = Math.max(1, long + short)
  const longWidth = (long / total) * 100
  const shortWidth = 100 - longWidth
  const net = row.netPosition ?? long - short
  const netDelta = netChange(row)
  const longPercentile = metricPercentile(analysis, row.longPosition, (item) => item.longPosition)
  const shortPercentile = metricPercentile(analysis, row.shortPosition, (item) => item.shortPosition)
  const ratioPercentile = metricPercentile(analysis, row.longShortRatio, (item) => item.longShortRatio)
  const netPercentile = metricPercentile(analysis, net, (item) => item.netPosition)
  const volumePercentile = metricPercentile(analysis, row.volume, (item) => item.volume)
  const changeLabel = (value: number | null | undefined, increase: string, decrease: string) => {
    if ((value ?? 0) > 0) return increase
    if ((value ?? 0) < 0) return decrease
    return "变化有限"
  }
  return <aside className="border-l border-border/60" aria-label="当前结构">
    <div className="flex items-center justify-between border-b border-border/60 px-4 py-3"><div className="flex items-center gap-2"><h2 className="text-sm font-semibold">当前结构</h2><span className={cn("inline-flex items-center gap-1 text-[10px] font-medium", net >= 0 ? "text-rose-600 dark:text-rose-400" : "text-emerald-600 dark:text-emerald-400")}><span className="size-1.5 rounded-full bg-current" />{net >= 0 ? "净多结构" : "净空结构"}</span></div><span className="font-mono text-[10px] text-muted-foreground">{formatDate(history.at(-1)?.tradeDate)}</span></div>
    <div className="space-y-4 p-4">
      <div className="grid grid-cols-2 gap-4">
        <div><div className="text-[11px] text-muted-foreground">多头持仓 · Top {top}</div><strong className="mt-1 block font-mono text-lg tabular-nums text-rose-600 dark:text-rose-400">{formatPosition(row.longPosition)}</strong><span className={cn("mt-1 block text-[10px]", percentileTone(longPercentile))}>三年分位 {longPercentile == null ? "暂无" : `${longPercentile}% · ${percentileLabel(longPercentile)}`}</span></div>
        <div className="text-right"><div className="text-[11px] text-muted-foreground">空头持仓 · Top {top}</div><strong className="mt-1 block font-mono text-lg tabular-nums text-emerald-600 dark:text-emerald-400">{formatPosition(row.shortPosition)}</strong><span className={cn("mt-1 block text-[10px]", percentileTone(shortPercentile))}>三年分位 {shortPercentile == null ? "暂无" : `${shortPercentile}% · ${percentileLabel(shortPercentile)}`}</span></div>
      </div>
      <div aria-label={`多头占比 ${longWidth.toFixed(1)}%，空头占比 ${shortWidth.toFixed(1)}%`}>
        <div className="mb-1.5 flex justify-between text-[10px] text-muted-foreground"><span>多头 {longWidth.toFixed(1)}%</span><span>空头 {shortWidth.toFixed(1)}%</span></div>
        <div className="flex h-2 overflow-hidden rounded-full bg-muted"><span className="bg-rose-500" style={{ width: `${longWidth}%` }} /><span className="bg-emerald-500" style={{ width: `${shortWidth}%` }} /></div>
        <div className="mt-2 rounded-md bg-muted/45 px-3 py-2 text-[11px]"><div className="flex items-center justify-between gap-2"><span className="text-muted-foreground">多空比（多头÷空头） <b className="font-mono text-foreground">{formatRatio(row.longShortRatio)}</b></span><span className={cn("text-[10px]", percentileTone(ratioPercentile))}>{ratioPercentile == null ? "暂无分位" : `${ratioPercentile}% · ${percentileLabel(ratioPercentile)}`}</span></div><div className="mt-1.5 flex items-center justify-between gap-2"><span className="text-muted-foreground">净持仓（多头−空头） <b className={cn("font-mono", toneForValue(net))}>{formatSigned(net)}</b></span><span className={cn("text-[10px]", percentileTone(netPercentile))}>{netPercentile == null ? "暂无分位" : `${netPercentile}% · ${percentileLabel(netPercentile)}`}</span></div></div>
      </div>
      <div className="text-[10px] leading-4 text-muted-foreground">日变化表示当前交易日会员排名变化值的合计；正值为增仓，负值为减仓。</div>
      <div className="divide-y divide-border/60 border-y border-border/60">
        {[{ label: "多头日变化", value: row.longChange, percentile: metricPercentile(analysis, row.longChange, (item) => item.longChange), note: changeLabel(row.longChange, "多头增仓", "多头减仓") }, { label: "空头日变化", value: row.shortChange, percentile: metricPercentile(analysis, row.shortChange, (item) => item.shortChange), note: changeLabel(row.shortChange, "空头增仓", "空头减仓") }, { label: "净持仓日变化", value: netDelta, percentile: metricPercentile(analysis, netDelta, (item) => (item.longChange ?? 0) - (item.shortChange ?? 0)), note: directionLabel(netDelta) }].map((item) => <div key={item.label} className="grid grid-cols-[1fr_auto] items-center gap-x-3 py-2.5 text-[11px]"><span className="text-muted-foreground">{item.label}</span><span className={cn("text-right font-mono tabular-nums", toneForValue(item.value))}>{formatSigned(item.value)}</span><span className="text-[10px] text-muted-foreground">{item.note}</span><span className={cn("text-right text-[10px]", percentileTone(item.percentile))}>{item.percentile == null ? "暂无分位" : `历史分位 ${item.percentile}%`}</span></div>)}
      </div>
      <div className="flex items-center justify-between rounded-md bg-muted/45 px-3 py-2 text-[11px]"><span className="text-muted-foreground">成交量排名合计</span><span className="font-mono tabular-nums">{formatPosition(row.volume)}</span><span className={cn("text-[10px]", percentileTone(volumePercentile))}>{volumePercentile == null ? "暂无分位" : `三年分位 ${volumePercentile}%`}</span></div>
    </div>
  </aside>
}

function PositionBiasIndicator({ row, history, top, aiEnabled, aiLoading, aiError, aiResult, onRunAi }: { row: FuturesPositionInstrument; history: FuturesPositionHistory[]; top: number; aiEnabled: boolean; aiLoading: boolean; aiError: string | null; aiResult: FuturesPositionAiResult | null; onRunAi: () => void }) {
  const data = useMemo(() => analysisHistory(history), [history])
  const netPosition = row.netPosition ?? 0
  const netDelta = netChange(row)
  const netMeaning = netPosition > 0
    ? "多头持仓高于空头持仓，当前是净多结构。"
    : netPosition < 0
      ? "空头持仓高于多头持仓，当前是净空结构。"
      : "多头与空头持仓接近，当前结构相对均衡。"
  const marketReading = netPosition > 0
    ? `会员持仓偏多，多空比 ${formatRatio(row.longShortRatio)}；${netDelta > 0 ? "今日净多继续扩大" : netDelta < 0 ? "今日净多有所收窄" : "今日净多变化有限"}。研究上结合价格与成交量确认，不单独作为买入信号。`
    : netPosition < 0
      ? `会员持仓偏空，多空比 ${formatRatio(row.longShortRatio)}；${netDelta < 0 ? "今日净空继续扩大" : netDelta > 0 ? "今日净空有所收窄" : "今日净空变化有限"}。研究上结合价格与成交量确认，不单独作为卖出信号。`
      : `会员多空持仓接近，当前方向不明显。研究上等待价格与成交量进一步确认，不单独依据持仓分位决策。`
  const levelMetrics = [
    { label: "多头持仓", value: formatPosition(row.longPosition), percentile: metricPercentile(data, row.longPosition, (item) => item.longPosition) },
    { label: "空头持仓", value: formatPosition(row.shortPosition), percentile: metricPercentile(data, row.shortPosition, (item) => item.shortPosition) },
    { label: "多空比", value: formatRatio(row.longShortRatio), percentile: metricPercentile(data, row.longShortRatio, (item) => item.longShortRatio) },
    { label: "净持仓", value: formatSigned(netPosition), percentile: metricPercentile(data, netPosition, (item) => item.netPosition) },
  ]
  const changeMetrics = [
    { label: "多头日变化", value: row.longChange, percentile: metricPercentile(data, row.longChange, (item) => item.longChange), meaning: row.longChange == null ? "暂无" : row.longChange > 0 ? "多头增仓" : row.longChange < 0 ? "多头减仓" : "变化有限" },
    { label: "空头日变化", value: row.shortChange, percentile: metricPercentile(data, row.shortChange, (item) => item.shortChange), meaning: row.shortChange == null ? "暂无" : row.shortChange > 0 ? "空头增仓" : row.shortChange < 0 ? "空头减仓" : "变化有限" },
    { label: "净持仓日变化", value: netDelta, percentile: metricPercentile(data, netDelta, (item) => (item.longChange ?? 0) - (item.shortChange ?? 0)), meaning: directionLabel(netDelta) },
    { label: "成交量", value: row.volume, percentile: metricPercentile(data, row.volume, (item) => item.volume), meaning: "活跃度" },
  ]

  return <aside className="border-t border-border/60 lg:border-l lg:border-t-0" aria-label="多空指标">
    <div className="border-b border-border/60 px-4 py-2.5">
      <div className="flex flex-wrap items-baseline justify-between gap-x-3 gap-y-1">
        <h2 className="text-sm font-semibold">三年历史分位</h2>
        <span className="text-[10px] text-muted-foreground">{data.length} 个有效交易日</span>
      </div>
      <p className="mt-1 text-[10px] leading-4 text-muted-foreground">分位 92% = 当前值高于过去三年约 92% 的有效交易日，不是上涨概率</p>
    </div>
    <div className="space-y-3 px-4 pb-3 pt-0">
      <div className="grid grid-cols-4 divide-x divide-border/60 border-y border-border/60">
        {levelMetrics.map((item) => <div key={item.label} className="min-w-0 px-3 py-2.5 first:pl-0 last:pr-0"><div className="truncate text-[10px] font-medium text-muted-foreground">{item.label}</div><div className="mt-0.5 truncate font-mono text-sm font-semibold tabular-nums">{item.value}</div><div className={cn("mt-0.5 truncate text-[10px] font-medium", percentileTone(item.percentile))}>{item.percentile == null ? "暂无分位" : `${item.percentile}% · ${percentileLabel(item.percentile)}`}</div></div>)}
      </div>

      <div className="border-y border-border/60 text-[10px]">
        <div className="grid grid-cols-[1.2fr_1fr_0.9fr_1.15fr] gap-2 bg-muted/25 px-2 py-1.5 text-muted-foreground"><span>指标</span><span className="text-right">当前值</span><span className="text-right">含义</span><span className="text-right">三年分位</span></div>
        <div className="divide-y divide-border/50">
          {changeMetrics.map((item) => <div key={item.label} className="grid grid-cols-[1.2fr_1fr_0.9fr_1.15fr] items-center gap-2 px-2 py-1.5"><span className="truncate font-medium text-foreground">{item.label}</span><span className={cn("truncate text-right font-mono tabular-nums", toneForValue(item.value))}>{item.label === "成交量" ? formatPosition(item.value) : formatSigned(item.value)}</span><span className="truncate text-right text-muted-foreground">{item.meaning}</span><span className={cn("truncate text-right", percentileTone(item.percentile))}>{item.percentile == null ? "暂无" : `${item.percentile}% · ${percentileLabel(item.percentile)}`}</span></div>)}
        </div>
      </div>

      <p className="text-[10px] leading-4 text-muted-foreground">多空比 = 多头 ÷ 空头 · 净持仓 = 多头 − 空头 · 日变化 = 当日排名变化合计 · Top {top} 口径</p>
      <div className="flex items-start gap-2 rounded-md bg-muted/45 px-3 py-2 text-[10px] leading-4">
        <span className="shrink-0 font-medium text-foreground">当前结构</span>
        <p className="text-muted-foreground">{netMeaning} 仅描述持仓事实，不构成交易建议。</p>
      </div>
      <div className="rounded-md border border-information/20 bg-information/5 px-3 py-2 text-[10px] leading-4">
        <div className="font-medium text-foreground">数据解读</div>
        <p className="mt-0.5 text-muted-foreground">{marketReading}</p>
      </div>
      {aiEnabled ? <div className="border-t border-border/60 pt-3" aria-label="AI 结构分析">
          <div className="flex flex-wrap items-center justify-between gap-2">
           <div className="flex min-w-0 items-center gap-1.5"><BrainCircuit className="size-3.5 text-information" /><span className="text-[11px] font-medium">AI 结构分析</span></div>
           <div className="flex items-center gap-1"><AiBindingProviderSelect capability="market_futures_position_analysis" /><Button type="button" variant="secondary" size="sm" disabled={aiLoading} onClick={onRunAi} className="h-7 gap-1.5 px-2 text-[10px]">{aiLoading ? <Loader2 className="size-3 animate-spin" /> : <BrainCircuit className="size-3" />}{aiLoading ? "分析中" : "生成解读"}</Button></div>
        </div>
        {aiError ? <p className="mt-2 text-[10px] leading-4 text-rose-600 dark:text-rose-400">{aiError}</p> : null}
        {aiResult ? <div className="mt-2 space-y-2 text-[10px] leading-4">
          <p className="font-medium text-foreground">{aiResult.headline || "已生成结构解读"}</p>
          {aiResult.structure ? <p className="text-muted-foreground"><span className="font-medium text-foreground">结构：</span>{aiResult.structure}</p> : null}
          {aiResult.trend ? <p className="text-muted-foreground"><span className="font-medium text-foreground">变化：</span>{aiResult.trend}</p> : null}
          {aiResult.evidence?.length ? <p className="text-muted-foreground"><span className="font-medium text-foreground">证据：</span>{aiResult.evidence.slice(0, 2).join("；")}</p> : null}
          {aiResult.watch?.length ? <p className="text-muted-foreground"><span className="font-medium text-foreground">观察：</span>{aiResult.watch.slice(0, 2).join("；")}</p> : null}
          {aiResult.risk ? <p className="text-muted-foreground"><span className="font-medium text-foreground">限制：</span>{aiResult.risk}</p> : null}
          <p className="border-t border-information/15 pt-1.5 text-muted-foreground">{aiResult.data_quality || "基于当前持仓数据"} · {aiResult.disclaimer || "仅描述持仓事实，不构成交易建议。"}</p>
        </div> : <p className="mt-2 text-[10px] leading-4 text-muted-foreground">基于当前快照、历史序列和会员排名生成事实型解读。</p>}
      </div> : null}
    </div>
  </aside>
}

function SeatTable({ title, rows, total, top, tone }: { title: string; rows: SeatRow[]; total: number; top: number; tone: "long" | "short" | "neutral" }) {
  const visible = rows.slice(0, 5)
  const max = Math.max(1, ...visible.map((row) => Math.abs(row.rankValue)))
  return <div className="min-w-0">
    <div className="flex h-10 items-center justify-between bg-muted/20 px-3"><strong className={cn("text-xs", tone === "long" ? "text-rose-600 dark:text-rose-400" : tone === "short" ? "text-emerald-600 dark:text-emerald-400" : "text-foreground")}>{title}</strong><span className="font-mono text-[10px] text-muted-foreground">Top {top} 合计 {formatPosition(total)}</span></div>
    <Table className="min-w-[480px]">
      <TableHeader><TableRow className="bg-muted/20 hover:bg-muted/20"><TableHead className="w-[42%]">会员</TableHead><TableHead className="text-right">持仓</TableHead><TableHead className="text-right">日变化</TableHead><TableHead className="pr-3 text-right">占比</TableHead></TableRow></TableHeader>
      <TableBody>{visible.length ? visible.map((row) => <TableRow key={`${title}-${row.memberName}-${row.rankNo}`}><TableCell><div className="flex min-w-0 items-center gap-2"><span className="w-5 shrink-0 font-mono text-[10px] text-muted-foreground">{String(row.rankNo).padStart(2, "0")}</span><div className="min-w-0"><span className="block truncate text-xs font-medium">{row.memberName}</span><span className={cn("mt-1 block h-1 rounded-full", tone === "short" ? "bg-emerald-500/60" : tone === "long" ? "bg-rose-500/60" : "bg-information/60")} style={{ width: `${Math.max(12, (Math.abs(row.rankValue) / max) * 42)}px` }} /></div></div></TableCell><TableCell className={cn("text-right font-mono tabular-nums", tone === "neutral" && toneForValue(row.rankValue))}>{formatSigned(row.rankValue)}</TableCell><TableCell className={cn("text-right font-mono tabular-nums", toneForValue(row.rankChange))}>{formatSigned(row.rankChange)}</TableCell><TableCell className="pr-3 text-right font-mono text-[11px] tabular-nums">{total ? `${((Math.abs(row.rankValue) / Math.abs(total)) * 100).toFixed(1)}%` : "-"}</TableCell></TableRow>) : <TableRow><TableCell colSpan={4} className="h-28 text-center text-xs text-muted-foreground">暂无符合当前视角的席位数据</TableCell></TableRow>}</TableBody>
    </Table>
    <div className="flex h-8 items-center justify-between px-3 text-[10px] text-muted-foreground"><span>{rows.length ? `显示前 ${Math.min(5, rows.length)} 家` : "等待数据"}</span><span className="text-information">查看全部明细 ›</span></div>
  </div>
}

function SeatAttribution({ members, history, top, view, onViewChange, onOpenDetails }: { members: FuturesPositionMember[]; history: FuturesPositionHistory[]; top: number; view: SeatView; onViewChange: (view: SeatView) => void; onOpenDetails: () => void }) {
  const longRows = members.filter((member) => member.rankType === "LONG").map((member) => ({ rankNo: member.rankNo, memberName: member.memberName || "未披露", rankValue: member.rankValue, rankChange: member.rankChange }))
  const shortRows = members.filter((member) => member.rankType === "SHORT").map((member) => ({ rankNo: member.rankNo, memberName: member.memberName || "未披露", rankValue: member.rankValue, rankChange: member.rankChange }))
  const longTotal = longRows.reduce((sum, row) => sum + row.rankValue, 0)
  const shortTotal = shortRows.reduce((sum, row) => sum + row.rankValue, 0)
  const netRows = buildNetRows(members)
  const openRows = buildFlowRows(members, "OPEN")
  const closeRows = buildFlowRows(members, "CLOSE")
  return <section className="overflow-hidden rounded-lg border border-border/70 bg-card" aria-label="席位归因">
    <div className="flex flex-wrap items-center gap-3 border-b border-border/60 px-3 py-3"><div><h2 className="text-sm font-semibold">席位归因</h2><p className="mt-1 text-[11px] text-muted-foreground">多空排名独立计算，排名位置不代表同一会员。</p></div><div className="ml-auto flex max-w-full overflow-x-auto rounded-md bg-muted/35 p-0.5" role="tablist" aria-label="席位分析视角">{seatTabs.map((tab) => <button key={tab.value} type="button" role="tab" aria-selected={view === tab.value} onClick={() => onViewChange(tab.value)} className={cn("shrink-0 rounded px-2 py-1.5 text-[10px] text-muted-foreground", view === tab.value && "bg-background font-medium text-foreground shadow-sm")}>{tab.label}</button>)}</div></div>
    {view === "DUAL" ? <div className="grid divide-y divide-border/60 lg:grid-cols-2 lg:divide-x lg:divide-y-0"><SeatTable title="多头持仓" rows={longRows} total={longTotal} top={top} tone="long" /><SeatTable title="空头持仓" rows={shortRows} total={shortTotal} top={top} tone="short" /></div> : null}
    {view === "NET" ? <div><SeatTable title="净持仓排名" rows={netRows} total={netRows.reduce((sum, row) => sum + row.rankValue, 0)} top={top} tone="neutral" /><button type="button" onClick={onOpenDetails} className="sr-only">查看净持仓明细</button></div> : null}
    {view === "OPEN" || view === "CLOSE" ? <SeatTable title={view === "OPEN" ? "建仓席位" : "减仓席位"} rows={view === "OPEN" ? openRows : closeRows} total={(view === "OPEN" ? openRows : closeRows).reduce((sum, row) => sum + row.rankValue, 0)} top={top} tone="neutral" /> : null}
    {view === "PRICE" ? <div className="grid min-h-32 place-items-center px-4 text-center text-xs text-muted-foreground"><div><p className="font-medium text-foreground">行情价格尚未接入</p><p className="mt-1">当前仅展示会员持仓结构。接入价格序列后，这里会补充“价格变化 × 持仓变化”关系。</p></div></div> : null}
    {view === "HISTORY" ? <div className="divide-y divide-border/60">{history.slice(-5).reverse().map((item) => <div key={item.tradeDate} className="grid grid-cols-[84px_1fr_1fr_90px] items-center gap-3 px-3 py-2 text-[11px]"><span className="font-mono text-muted-foreground">{formatDate(item.tradeDate)}</span><span>多 <b className="font-mono text-rose-600">{formatPosition(item.longPosition)}</b></span><span>空 <b className="font-mono text-emerald-600">{formatPosition(item.shortPosition)}</b></span><span className={cn("text-right font-mono", toneForValue(item.netPosition))}>净 {formatSigned(item.netPosition)}</span></div>)}</div> : null}
    <div className="flex items-center justify-between border-t border-border/60 px-3 py-2 text-[10px] text-muted-foreground"><span>前 {top} 名口径 · 数据来源以交易所状态为准</span><Button type="button" variant="ghost" size="sm" onClick={onOpenDetails} className="h-6 gap-1 px-1.5 text-[10px] text-information"><ExternalLink className="size-3" />打开完整明细</Button></div>
  </section>
}

export function ResearchWorkbench({ payload, selected, loading, scopeType, exchangeCode, top, tradeDate, onSelect, onOpenDetails }: ResearchWorkbenchProps) {
  const [members, setMembers] = useState<FuturesPositionMember[]>([])
  const [history, setHistory] = useState<FuturesPositionHistory[]>([])
  const [indexData, setIndexData] = useState<{ key: string; rows: FuturesPositionInstrument[] }>({ key: "", rows: [] })
  const [loadedKey, setLoadedKey] = useState("")
  const [detailsError, setDetailsError] = useState<{ key: string; message: string } | null>(null)
  const [range, setRange] = useState<HistoryRange>(20)
  const [seatView, setSeatView] = useState<SeatView>("DUAL")
  const [aiState, setAiState] = useState<{ key: string; loading: boolean; error: string | null; result: FuturesPositionAiResult | null }>({ key: "", loading: false, error: null, result: null })
  const selectedExchange = exchangeCode || selected?.exchangeCode || ""
  // Shortcut cards are variety-level rows even when the page filter is set to contracts.
  const selectedScopeType: FuturesScopeType = selected?.contractCode ? "CONTRACT" : "VARIETY"
  const selectedKey = selected && selectedExchange ? `${selectedExchange}:${selected.instrumentCode}` : ""
  const detailsLoading = Boolean(selectedKey && loadedKey !== selectedKey && detailsError?.key !== selectedKey)
  const activeDetailsError = detailsError?.key === selectedKey ? detailsError.message : null
  const indexKey = `${tradeDate ?? ""}:${top}`
  const indexLoading = indexData.key !== indexKey
  const selectedVariety = (selected?.varietyCode || selected?.instrumentCode.slice(0, 2) || "").toUpperCase()
  const aiEnabled = isFuturesPositionAiVariety(selectedVariety)
  const aiResult = aiState.key === selectedKey ? aiState.result : null
  const aiLoading = aiState.key === selectedKey && aiState.loading
  const aiError = aiState.key === selectedKey ? aiState.error : null

  const runAiAnalysis = async () => {
    if (!selected || !selectedExchange || !aiEnabled || !history.length || aiLoading) return
    setAiState({ key: selectedKey, loading: true, error: null, result: null })
    try {
      const result = await analyzeFuturesPosition({
        row: selected,
        history,
        members,
        instrumentName: varietyLabel(selected.varietyCode).split(" · ").at(-1) || selected.instrumentCode,
        top,
        tradeDate,
      })
      setAiState({ key: selectedKey, loading: false, error: null, result: result.analysis_result })
    } catch (cause) {
      setAiState({ key: selectedKey, loading: false, error: cause instanceof Error ? cause.message : String(cause), result: null })
    }
  }

  useEffect(() => {
    let active = true
    fetchFuturesPositionLatest({ tradeDate: tradeDate ?? undefined, scopeType: "VARIETY", exchangeCode: "CFFEX", top, sort: "netPosition", sortDir: "desc", page: 1, pageSize: 30 })
      .then((response) => { if (active) setIndexData({ key: indexKey, rows: response.instruments }) })
      .catch(() => { if (active) setIndexData({ key: indexKey, rows: [] }) })
    return () => { active = false }
  }, [indexKey, top, tradeDate])

  useEffect(() => {
    if (!selected || !selectedExchange) return
    let active = true
    const requestKey = `${selectedExchange}:${selected.instrumentCode}`
    Promise.all([
      fetchFuturesPositionMembers({ tradeDate: tradeDate ?? undefined, scopeType: selectedScopeType, exchangeCode: selectedExchange, instrumentCode: selected.instrumentCode, top }),
      fetchFuturesPositionHistory({ tradeDate: tradeDate ?? undefined, scopeType: selectedScopeType, exchangeCode: selectedExchange, instrumentCode: selected.instrumentCode, top, limit: HISTORY_LIMIT }),
    ]).then(([nextMembers, nextHistory]) => {
      if (!active) return
      setMembers(nextMembers)
      setHistory(nextHistory)
      setLoadedKey(requestKey)
      setDetailsError(null)
    }).catch((cause) => {
      if (!active) return
      setLoadedKey(requestKey)
      setDetailsError({ key: requestKey, message: cause instanceof Error ? cause.message : String(cause) })
    })
    return () => { active = false }
  }, [selected, selectedExchange, selectedScopeType, top, tradeDate])

  return <div className="space-y-3">
    <ScanStrip rows={payload?.instruments ?? []} indexRows={indexData.rows} indexLoading={indexLoading} selected={selected} onSelect={onSelect} />
    {selected ? <>
        <section className="overflow-hidden rounded-lg border border-border/70 bg-card" aria-label={selectedScopeType === "VARIETY" ? "品种工作台" : "合约工作台"}>
          <div className="grid min-w-0 items-start lg:grid-cols-[minmax(0,1fr)_360px] xl:grid-cols-[minmax(260px,1fr)_360px_500px] min-[1800px]:grid-cols-[minmax(320px,1fr)_360px_520px_340px]"><StructureChart history={history} range={range} row={selected} selectedExchange={selectedExchange} top={top} detailsLoading={detailsLoading} activeDetailsError={activeDetailsError} onOpenDetails={onOpenDetails} /><CurrentStructure row={selected} history={history} top={top} /><PositionBiasIndicator row={selected} history={history} top={top} aiEnabled={aiEnabled} aiLoading={aiLoading} aiError={aiError} aiResult={aiResult} onRunAi={() => { void runAiAnalysis() }} /><HistoryWindowSummary history={history} range={range} onRangeChange={setRange} /></div>
      </section>
      {activeDetailsError ? <div className="flex items-center gap-2 rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-xs text-rose-700 dark:border-rose-900/50 dark:bg-rose-950/20 dark:text-rose-300"><RefreshCw className="size-3.5" />会员与历史明细加载失败：{activeDetailsError}</div> : null}
      {detailsLoading ? <div className="flex items-center justify-center gap-2 rounded-lg border border-border/60 bg-muted/20 py-10 text-xs text-muted-foreground"><Loader2 className="size-4 animate-spin" />正在加载席位与历史数据</div> : <SeatAttribution members={members} history={history} top={top} view={seatView} onViewChange={setSeatView} onOpenDetails={onOpenDetails} />}
    </> : <div className="grid min-h-56 place-items-center rounded-lg border border-dashed border-border bg-muted/15 text-center text-sm text-muted-foreground"><div><p className="font-medium text-foreground">当前筛选没有可分析的{scopeType === "VARIETY" ? "品种" : "合约"}</p><p className="mt-1 text-xs">切换交易日、交易所或品种后再试。</p></div></div>}
    <div className="flex flex-wrap items-center gap-4 text-[11px] text-muted-foreground"><span className="inline-flex items-center gap-1.5"><span className="size-1.5 rounded-full bg-emerald-600" />{payload?.coverage?.items?.filter((item) => item.status === "SUCCEEDED" || item.status === "PARTIAL").length ?? 0} / {payload?.coverage?.items?.length ?? 0} 交易所快照可用</span><span>{selected?.source || "AkShare"}</span><span>数据源详情收进“数据状态”</span></div>
    {loading ? <div className="pointer-events-none fixed inset-0 z-10 grid place-items-center bg-background/20"><Loader2 className="size-5 animate-spin text-muted-foreground" /></div> : null}
  </div>
}
