import { useEffect, useMemo, useState } from "react"
import { Brain, CheckCircle2, CircleAlert, Loader2, RotateCcw } from "lucide-react"

import { Button } from "@/components/ui/button"
import {
  explainMarketIndicatorReplay,
  explainMarketIndicatorReplayFallback,
  fetchMarketIndicatorReplayEpisode,
  fetchMarketIndicatorReplaySummary,
  type MarketIndicatorReplayEpisodeResponse,
  type MarketIndicatorReplayExplanation,
  type MarketIndicatorReplaySummary,
} from "@/services/market/indicator"

function text(value: unknown, fallback = "-") {
  return value == null || String(value).trim() === "" ? fallback : String(value)
}

function episodeId(item: Record<string, unknown>) {
  return text(item.episodeId ?? item.episode_id, "")
}

export function ReplayReviewPanel({ tradeDate }: { tradeDate: string }) {
  const [summary, setSummary] = useState<MarketIndicatorReplaySummary | null>(null)
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [episode, setEpisode] = useState<MarketIndicatorReplayEpisodeResponse | null>(null)
  const [explanation, setExplanation] = useState<MarketIndicatorReplayExplanation | null>(null)
  const [loading, setLoading] = useState(false)
  const [detailLoading, setDetailLoading] = useState(false)
  const [explainLoading, setExplainLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const episodes = useMemo(() => (summary?.episodes ?? []) as Array<Record<string, unknown>>, [summary?.episodes])

  async function loadSummary() {
    if (!tradeDate) return
    setLoading(true)
    setError(null)
    try {
      const next = await fetchMarketIndicatorReplaySummary({ tradeDate, limit: 100 })
      setSummary(next)
      const first = (next.episodes ?? [])[0]
      const firstId = first ? episodeId(first as Record<string, unknown>) : ""
      setSelectedId((current) => current && episodes.some((item) => episodeId(item) === current) ? current : firstId || null)
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : String(cause))
      setSummary(null)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { void loadSummary() }, [tradeDate])

  useEffect(() => {
    if (!selectedId) {
      setEpisode(null)
      setExplanation(null)
      return
    }
    setDetailLoading(true)
    void fetchMarketIndicatorReplayEpisode({ tradeDate, episodeId: selectedId })
      .then((next) => { setEpisode(next); setExplanation(null) })
      .catch((cause) => setError(cause instanceof Error ? cause.message : String(cause)))
      .finally(() => setDetailLoading(false))
  }, [selectedId, tradeDate])

  async function explain() {
    if (!selectedId) return
    setExplainLoading(true)
    try {
      setExplanation(await explainMarketIndicatorReplay({
        tradeDate,
        episodeId: selectedId,
        episode,
        timeline: episode?.timeline,
        outcomes: episode?.outcomes,
      }))
    } catch (cause) {
      try {
        const fallback = await explainMarketIndicatorReplayFallback({ tradeDate, episodeId: selectedId })
        setExplanation(fallback)
      } catch (fallbackCause) {
        setError(fallbackCause instanceof Error ? fallbackCause.message : (cause instanceof Error ? cause.message : String(cause)))
      }
    } finally {
      setExplainLoading(false)
    }
  }

  if (loading) return <section className="rounded-lg border border-border/70 bg-card p-4 text-xs text-muted-foreground">正在汇总 Episode、结果评价和 1m 覆盖状态…</section>
  if (error) return <section className="rounded-lg border border-rose-200 bg-rose-50 p-4 text-xs text-rose-700"><div className="flex items-center gap-2"><CircleAlert className="size-4" />复盘摘要读取失败：{error}<Button variant="ghost" size="sm" className="ml-auto h-7" onClick={() => void loadSummary()}><RotateCcw />重试</Button></div></section>

  return <section className="overflow-hidden rounded-lg border border-border/70 bg-card" aria-label="复盘摘要与Episode详情">
    <div className="flex flex-wrap items-start justify-between gap-3 border-b border-border/60 px-4 py-3">
      <div><h2 className="text-sm font-semibold">复盘摘要与传导 Episode</h2><p className="mt-1 text-xs text-muted-foreground">交易日 {tradeDate} · 只统计当日可见证据</p></div>
      <Button variant="outline" size="sm" className="h-8" onClick={() => void loadSummary()}><RotateCcw />刷新摘要</Button>
    </div>
    <div className="grid gap-2 border-b border-border/60 p-4 sm:grid-cols-3 lg:grid-cols-6">
      <Metric label="Episode" value={summary?.episodeCount} />
      <Metric label="有效传导" value={summary?.effectiveEpisodeCount} tone="positive" />
      <Metric label="失败 Episode" value={summary?.failedEpisodeCount} tone="negative" />
      <Metric label="首板行业" value={summary?.firstBoardIndustryCount} />
      <Metric label="结果样本" value={`${summary?.outcomeCount ?? 0}/${summary?.episodeCount ?? 0}`} />
      <Metric label="1m 状态" value={text(summary?.dataStatus, "unknown")} />
    </div>
    {summary?.coverageWarning ? <div className="border-b border-amber-200 bg-amber-50 px-4 py-2 text-xs text-amber-800">{summary.coverageWarning}</div> : null}
    <div className="grid min-w-0 gap-0 lg:grid-cols-[minmax(220px,0.38fr)_minmax(0,0.62fr)]">
      <div className="max-h-72 overflow-y-auto border-b border-border/60 lg:border-b-0 lg:border-r">
        {episodes.length ? episodes.map((item, index) => {
          const id = episodeId(item)
          const active = id === selectedId
          return <button key={id || index} type="button" onClick={() => setSelectedId(id || null)} className={`flex w-full items-start justify-between gap-3 border-b border-border/50 px-4 py-3 text-left text-xs transition-colors hover:bg-muted/50 ${active ? "bg-muted/70" : ""}`}>
            <span className="min-w-0"><span className="block truncate font-medium text-foreground">{text(item.sectorName ?? item.sector_name, "未知板块")}</span><span className="mt-1 block text-[11px] text-muted-foreground">{text(item.stage, "observation")} · {text(item.direction, "unknown")}</span></span>
            {active ? <CheckCircle2 className="mt-0.5 size-3.5 shrink-0 text-information" /> : null}
          </button>
        }) : <div className="p-4 text-xs text-muted-foreground">当前交易日没有已计算的 Episode，先查看上方 1m 事件和涨停锚点。</div>}
      </div>
      <div className="min-w-0 p-4">
        {detailLoading ? <div className="flex items-center gap-2 text-xs text-muted-foreground"><Loader2 className="size-3.5 animate-spin" />正在读取 Episode 证据…</div> : episode ? <>
          <div className="flex flex-wrap items-start justify-between gap-3"><div className="min-w-0"><h3 className="truncate text-sm font-semibold">{text(episode.sectorName ?? episode.sector_name, "未知板块")}</h3><p className="mt-1 text-xs text-muted-foreground">{text(episode.explainability, "暂无确定性解释")}</p></div><Button size="sm" className="h-8" onClick={() => void explain()} disabled={explainLoading}><Brain />{explainLoading ? "生成中" : "生成复盘解释"}</Button></div>
          <div className="mt-3 grid grid-cols-2 gap-2 text-xs sm:grid-cols-4"><Metric label="阶段" value={text(episode.stage, "observation")} /><Metric label="覆盖" value={text(episode.coveragePeak ?? episode.coverage_peak)} /><Metric label="滞后" value={text(episode.medianLagSeconds ?? episode.median_lag_seconds)} /><Metric label="相对大盘" value={text(episode.benchmarkExcessPct ?? episode.benchmark_excess_pct)} /></div>
          {explanation ? <div className="mt-3 rounded-md border border-information/20 bg-information/5 p-3 text-xs"><div className="font-medium text-foreground">{explanation.status === "deterministic_fallback" ? "确定性复盘解释（未调用模型）" : "AI 复盘解释"}</div><p className="mt-1 leading-5 text-muted-foreground">{explanation.summary}</p><ExplanationList title="支持证据" values={explanation.drivers} /><ExplanationList title="反证与限制" values={[...(explanation.counterEvidence ?? []), ...(explanation.uncertainties ?? [])]} /></div> : null}
          <p className="mt-3 text-[11px] text-muted-foreground">Episode ID：<span className="font-mono break-all">{text(episode.episodeId ?? episode.episode_id)}</span></p>
        </> : <div className="text-xs text-muted-foreground">选择左侧 Episode 查看板块首个锚点、跟随结果和失败原因。</div>}
      </div>
    </div>
  </section>
}

function Metric({ label, value, tone }: { label: string; value: unknown; tone?: "positive" | "negative" }) {
  const className = tone === "positive" ? "text-emerald-700" : tone === "negative" ? "text-rose-700" : "text-foreground"
  return <div className="rounded-md bg-muted/35 px-2.5 py-2"><span className="block text-[10px] text-muted-foreground">{label}</span><span className={`mt-0.5 block truncate font-mono text-xs ${className}`}>{text(value, "-")}</span></div>
}

function ExplanationList({ title, values }: { title: string; values?: string[] }) {
  if (!values?.length) return null
  return <div className="mt-2"><span className="text-[10px] font-medium text-muted-foreground">{title}</span><div className="mt-1 flex flex-wrap gap-1.5">{values.map((value, index) => <span key={`${value}-${index}`} className="rounded bg-background px-1.5 py-1 text-[11px] text-foreground">{value}</span>)}</div></div>
}
