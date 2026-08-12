import { useEffect, useMemo, useState } from "react"
import { AlertTriangle, Clock3, Database, Pause, Play, SkipBack } from "lucide-react"

import { Button } from "@/components/ui/button"
import type { MarketIndicatorReplayTimelineResponse } from "@/services/market/indicator"

function formatTime(value?: string | null) {
  if (!value) return "-"
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value.slice(11, 16) : date.toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit" })
}

function statusLabel(status?: string | null) {
  return ({ ready: "1m 已覆盖", partial: "1m 部分覆盖", pending: "1m 等待补齐", unavailable: "1m 不可用" } as Record<string, string>)[status ?? ""] ?? "数据状态未知"
}

export function ReplayTimeline({ data, industryName }: { data: MarketIndicatorReplayTimelineResponse | null; industryName?: string | null }) {
  const points = data?.indexPoints ?? []
  const [cursor, setCursor] = useState(0)
  const [playing, setPlaying] = useState(false)

  useEffect(() => {
    setCursor(Math.max(0, points.length - 1))
    setPlaying(false)
  }, [data?.tradeDate, points.length])

  useEffect(() => {
    if (!playing || points.length < 2) return
    const timer = window.setInterval(() => {
      setCursor((current) => {
        if (current >= points.length - 1) {
          setPlaying(false)
          return current
        }
        return current + 1
      })
    }, 350)
    return () => window.clearInterval(timer)
  }, [playing, points.length])

  const current = points[cursor]
  const currentTimestamp = current?.timestamp ?? null
  const visibleEvents = useMemo(() => (data?.events ?? []).filter((event) => {
    if (!currentTimestamp || !event.eventAt) return true
    const timestamp = new Date(event.eventAt).getTime()
    return Number.isNaN(timestamp) || timestamp <= currentTimestamp
  }), [currentTimestamp, data?.events])
  const visibleIndustry = useMemo(() => (data?.industryPoints ?? []).filter((row) => {
    if (!currentTimestamp || !row.capturedAt) return true
    const timestamp = new Date(row.capturedAt).getTime()
    return Number.isNaN(timestamp) || timestamp <= currentTimestamp
  }).at(-1), [currentTimestamp, data?.industryPoints])
  const maxClose = Math.max(...points.map((point) => point.close ?? 0), 0)
  const minClose = Math.min(...points.map((point) => point.close ?? maxClose), maxClose)
  const currentChange = current?.close != null && minClose > 0 ? ((current.close - minClose) / minClose) * 100 : null

  return (
    <section className="overflow-hidden rounded-lg border border-border/70 bg-card" aria-label="1m盘后回放">
      <div className="flex flex-col gap-3 border-b border-border/60 px-4 py-3 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <div className="flex items-center gap-2"><Clock3 className="size-4 text-information" /><h2 className="text-sm font-semibold">1m 事件回放</h2></div>
          <p className="mt-1 text-xs text-muted-foreground">游标之前只展示已经发生的异动和行业状态；分钟数据补齐后自动可回放。</p>
        </div>
        <div className="flex items-center gap-2 text-[11px] text-muted-foreground">
          <span className={`inline-flex items-center gap-1 rounded-full px-2 py-1 ${data?.dataStatus === "ready" ? "bg-emerald-500/10 text-emerald-700" : "bg-amber-500/10 text-amber-700"}`}><Database className="size-3" />{statusLabel(data?.dataStatus)}</span>
          {data?.dataTradeDate && data.dataTradeDate !== data.tradeDate ? <span>最新覆盖 {data.dataTradeDate}</span> : null}
        </div>
      </div>

      {data?.message ? <div className="flex items-start gap-2 border-b border-amber-200 bg-amber-50 px-4 py-2.5 text-xs text-amber-800 dark:border-amber-900/60 dark:bg-amber-950/20 dark:text-amber-300"><AlertTriangle className="mt-0.5 size-3.5 shrink-0" /><span>{data.message}</span></div> : null}

      <div className="space-y-4 p-4">
        <div className="flex flex-wrap items-center gap-2">
          <Button type="button" variant="outline" size="sm" className="h-8" onClick={() => setPlaying((value) => !value)} disabled={points.length < 2} aria-label={playing ? "暂停回放" : "播放回放"}>
            {playing ? <Pause /> : <Play />}{playing ? "暂停" : "播放"}
          </Button>
          <Button type="button" variant="ghost" size="icon-sm" onClick={() => { setCursor(0); setPlaying(false) }} disabled={!points.length} aria-label="回到开盘" title="回到开盘"><SkipBack /></Button>
          <span className="font-mono text-xs text-foreground">{formatTime(current?.time)}</span>
          <span className="text-xs text-muted-foreground">{industryName ? `${industryName} · ` : "市场基准 · "}{current?.close == null ? "暂无指数点" : `收 ${current.close.toFixed(2)}`}</span>
          {currentChange != null ? <span className={`font-mono text-xs ${currentChange >= 0 ? "text-red-500" : "text-emerald-600"}`}>{currentChange >= 0 ? "+" : ""}{currentChange.toFixed(2)}%</span> : null}
        </div>

        <input className="w-full accent-slate-700 disabled:opacity-40" type="range" min={0} max={Math.max(0, points.length - 1)} value={Math.min(cursor, Math.max(0, points.length - 1))} onChange={(event) => { setPlaying(false); setCursor(Number(event.target.value)) }} disabled={!points.length} aria-label="回放时间游标" />
        <div className="flex justify-between text-[10px] font-mono text-muted-foreground"><span>{formatTime(points[0]?.time) || "09:30"}</span><span>{formatTime(points.at(-1)?.time) || "15:00"}</span></div>

        <div className="grid gap-3 lg:grid-cols-[minmax(0,1.4fr)_minmax(260px,0.6fr)]">
          <div className="rounded-md border border-border/60 bg-muted/15 p-3">
            <div className="flex items-center justify-between gap-3"><span className="text-xs font-medium">指数 1m 轨迹</span><span className="text-[11px] text-muted-foreground">{data?.indexPointCount ?? 0} 点</span></div>
            {points.length ? <div className="mt-4 flex h-24 items-end gap-px overflow-hidden" aria-label="指数分钟收盘轨迹">{points.map((point, index) => { const value = point.close ?? minClose; const height = maxClose > minClose ? ((value - minClose) / (maxClose - minClose)) * 78 + 8 : 50; return <span key={`${point.timestamp ?? point.time}-${index}`} className={`min-w-[2px] flex-1 ${index <= cursor ? "bg-slate-700" : "bg-border"}`} style={{ height: `${height}%` }} title={`${formatTime(point.time)} ${point.close?.toFixed(2) ?? "-"}`} /> })}</div> : <div className="mt-4 flex h-24 items-center justify-center text-xs text-muted-foreground">等待 1m K 线覆盖</div>}
          </div>
          <div className="rounded-md border border-border/60 p-3">
            <div className="flex items-center justify-between gap-3"><span className="text-xs font-medium">游标时刻状态</span><span className="font-mono text-[11px] text-muted-foreground">{visibleEvents.length} 个事件</span></div>
            <div className="mt-3 grid grid-cols-2 gap-2 text-xs"><Metric label="行业上涨" value={visibleIndustry?.advancingCount} /><Metric label="行业下跌" value={visibleIndustry?.decliningCount} /><Metric label="涨停" value={visibleIndustry?.limitUpCount} /><Metric label="跌停" value={visibleIndustry?.limitDownCount} /></div>
            <p className="mt-3 text-[11px] leading-5 text-muted-foreground">行业快照按游标前最后一条记录展示；没有对应分钟记录时保留缺失状态。</p>
          </div>
        </div>

        <div className="grid gap-3 lg:grid-cols-2">
          <div className="rounded-md border border-border/60 p-3"><div className="flex items-center justify-between gap-2"><span className="text-xs font-medium">已发生异动</span><span className="text-[11px] text-muted-foreground">按时间排序</span></div>{visibleEvents.length ? <div className="mt-2 max-h-40 space-y-1.5 overflow-y-auto">{visibleEvents.slice(-12).map((event, index) => <div key={`${event.id ?? event.code}-${index}`} className="flex items-center justify-between gap-3 rounded bg-muted/25 px-2.5 py-1.5 text-[11px]"><span className="font-mono text-muted-foreground">{formatTime(event.eventAt)}</span><span className="min-w-0 flex-1 truncate text-foreground">{event.name ?? event.code}</span><span className="shrink-0 text-muted-foreground">{event.eventType}</span></div>)}</div> : <p className="mt-3 text-[11px] text-muted-foreground">当前游标之前没有保存的异动事件。</p>}</div>
          <div className="rounded-md border border-border/60 p-3"><div className="flex items-center justify-between gap-2"><span className="text-xs font-medium">复盘边界</span><span className="font-mono text-[11px] text-muted-foreground">{data?.requestedFrom ? formatTime(data.requestedFrom) : "09:30"} - {data?.requestedTo ? formatTime(data.requestedTo) : "15:00"}</span></div><p className="mt-3 text-[11px] leading-5 text-muted-foreground">本视图只使用交易日 {data?.tradeDate ?? "-"} 的证据。1m 数据覆盖状态为“{statusLabel(data?.dataStatus)}”；覆盖不足时不会生成有效传导结论。</p></div>
        </div>
      </div>
    </section>
  )
}

function Metric({ label, value }: { label: string; value?: number | null }) {
  return <div className="rounded bg-muted/30 px-2 py-1.5"><span className="block text-[10px] text-muted-foreground">{label}</span><span className="font-mono text-xs text-foreground">{value == null ? "-" : value}</span></div>
}
