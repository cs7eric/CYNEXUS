import { useMemo, useState } from "react"
import { Compass, MapPin, RefreshCw, Sparkles, Target } from "lucide-react"

import type { ApplicationAnalysisRecent30FullItem } from "@/services/ai/application-analysis"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"

import { cn } from "@/lib/utils"
import { BIAS_TONE } from "../lib/constants"
import { fmt, fmtPercent, safeRecord, safeString, textList, toNumber } from "../lib/format"

function biasTone(bias: unknown) {
  const key = typeof bias === "string" ? bias.toLowerCase() : ""
  return BIAS_TONE[key] || BIAS_TONE.unclear
}

function DirectionDetails({
  trend,
  situation,
}: {
  trend: Record<string, unknown>
  situation: Record<string, unknown>
}) {
  const stateLabel = fmt(trend.state)
  const bias = biasTone(trend.bias)
  const horizon = fmt(trend.horizon)
  const score = fmt(trend.score)
  const confidence = fmt(trend.confidence)
  const momentum = safeRecord(trend.momentum)
  const maPosition = safeRecord(trend.ma_position)
  const pricePosition = safeRecord(trend.price_position)
  const support = safeRecord(trend.nearby_support)
  const resistance = safeRecord(trend.nearby_resistance)
  const scenario = safeRecord(trend.scenario_analysis)
  const evidence = textList(trend.key_evidence)
  const invalidList = textList(trend.invalid_conditions)

  const position = fmt(situation.position)
  const spaceStructure = fmt(situation.space_structure)
  const positionScore = fmt(situation.position_score)
  const situationConfidence = fmt(situation.confidence)
  const statusTags = textList(situation.status_tags)
  const situationEvidence = textList(situation.key_evidence)
  const situationNote = safeString(situation.note)
  const maStructure = fmt(maPosition.short_ma_structure ?? maPosition.structure)
  const pricePositionLabel = fmt(pricePosition.position_vs_range_20d ?? pricePosition.label)
  const volumePriceState = fmt(trend.volume_price_state)
  const turnoverState = fmt(trend.turnover_state)
  const benchmarkConfirmation = fmt(trend.benchmark_confirmation)
  const sentimentConfirmation = fmt(trend.sentiment_confirmation)

  const zoneText = (zone: Record<string, unknown>) => {
    if (!Object.keys(zone).length) return "—"
    const low = toNumber(zone.price_low)
    const high = toNumber(zone.price_high)
    const distance = toNumber(zone.distance_pct)
    const range = low != null && high != null ? `${fmt(low)}-${fmt(high)}` : "—"
    return `${range} · 距离 ${fmtPercent(distance ?? NaN)} · ${fmt(zone.level)}`
  }

  return (
    <div className="min-w-0 space-y-2 pt-1">
      <div className="grid gap-2 sm:grid-cols-2">
        <div className="rounded-xl bg-slate-50/70 p-3">
          <div className="flex items-center gap-2 text-[10px] uppercase tracking-[0.08em] text-slate-500">
            <Sparkles className="size-3" />短期趋势
          </div>
          <div className="mt-1.5 flex flex-wrap items-center gap-2">
            <div className="min-w-0 break-words text-sm font-semibold text-slate-900 sm:truncate">{stateLabel || "—"}</div>
            <Badge className={`rounded-full border px-2 py-0 text-[10px] font-medium ${bias.cls}`} variant="outline">
              {bias.label}
            </Badge>
          </div>
          <div className="mt-1.5 grid grid-cols-3 gap-1 text-[11px] text-slate-500">
            <div>
              <div className="text-slate-400">周期</div>
              <div className="font-mono text-slate-700">{horizon || "—"}</div>
            </div>
            <div>
              <div className="text-slate-400">分</div>
              <div className="font-mono text-slate-700">{score}</div>
            </div>
            <div>
              <div className="text-slate-400">置信</div>
              <div className="font-mono text-slate-700">{confidence}</div>
            </div>
          </div>
        </div>
        <div className="rounded-xl bg-slate-50/70 p-3">
          <div className="flex items-center gap-2 text-[10px] uppercase tracking-[0.08em] text-slate-500">
            <MapPin className="size-3" />当前所处情况
          </div>
          <div className="mt-1.5 flex flex-wrap items-center gap-2">
            <div className="min-w-0 break-words text-sm font-semibold text-slate-900 sm:truncate">{position || "—"}</div>
            <Badge className="rounded-full border border-slate-200 bg-slate-50 px-2 py-0 text-[10px] text-slate-600" variant="outline">
              {spaceStructure || "—"}
            </Badge>
          </div>
          <div className="mt-1.5 grid grid-cols-2 gap-1 text-[11px] text-slate-500">
            <div>
              <div className="text-slate-400">位置分</div>
              <div className="font-mono text-slate-700">{positionScore}</div>
            </div>
            <div>
              <div className="text-slate-400">置信</div>
              <div className="font-mono text-slate-700">{situationConfidence}</div>
            </div>
          </div>
        </div>
      </div>

      {(statusTags.length || situationNote) ? (
        <div className="rounded-xl bg-slate-50/70 p-3">
          <div className="flex items-center gap-2 text-[10px] uppercase tracking-[0.08em] text-slate-500">
            <Target className="size-3" />状态标签与备注
          </div>
          <div className="mt-1.5 flex flex-wrap gap-1.5">
            {statusTags.length ? (
              statusTags.map((tag) => (
                <Badge key={tag} className="rounded-full border-slate-200 bg-white px-2 py-0 text-[10px] text-slate-700" variant="outline">
                  {tag}
                </Badge>
              ))
            ) : (
              <span className="text-[11px] text-slate-400">无标签</span>
            )}
          </div>
          {situationNote ? <div className="mt-2 break-words text-[11px] leading-5 text-slate-600">{situationNote}</div> : null}
        </div>
      ) : null}

      <div className="rounded-xl bg-slate-50/70 p-3">
        <div className="flex items-center gap-2 text-[10px] uppercase tracking-[0.08em] text-slate-500">
          <Sparkles className="size-3" />动量 / 均线
        </div>
        <div className="mt-1.5 grid grid-cols-1 gap-x-3 gap-y-1 text-[11px] text-slate-600 sm:grid-cols-3">
          <div>
            <span className="text-slate-400">3 日 / 5 日 / 10 日</span>{" "}
            <span className="font-mono text-slate-700">
              {fmtPercent(toNumber(momentum.return_3d) ?? NaN)} · {fmtPercent(toNumber(momentum.return_5d) ?? NaN)} · {fmtPercent(toNumber(momentum.return_10d) ?? NaN)}
            </span>
          </div>
          <div>
            <span className="text-slate-400">5 日涨跌</span>{" "}
            <span className="font-mono text-slate-700">
              {fmt(momentum.up_days_5d)} 阳 / {fmt(momentum.down_days_5d)} 阴
            </span>
          </div>
          <div>
            <span className="text-slate-400">距 20 日高 / 低</span>{" "}
            <span className="font-mono text-slate-700">
              {fmtPercent(toNumber(momentum.near_20d_high_pct) ?? NaN)} · {fmtPercent(toNumber(momentum.near_20d_low_pct) ?? NaN)}
            </span>
          </div>
          <div>
            <span className="text-slate-400">短均线结构</span>{" "}
            <span className="text-slate-700">{maStructure || "—"}</span>
          </div>
          <div>
            <span className="text-slate-400">价格位置</span>{" "}
            <span className="text-slate-700">{pricePositionLabel || "—"}</span>
          </div>
          <div>
            <span className="text-slate-400">量价状态</span>{" "}
            <span className="text-slate-700">{volumePriceState || "—"}</span>
          </div>
          <div>
            <span className="text-slate-400">换手状态</span>{" "}
            <span className="text-slate-700">{turnoverState || "—"}</span>
          </div>
          <div>
            <span className="text-slate-400">指数确认</span>{" "}
            <span className="text-slate-700">{benchmarkConfirmation || "—"}</span>
          </div>
          <div>
            <span className="text-slate-400">情绪确认</span>{" "}
            <span className="text-slate-700">{sentimentConfirmation || "—"}</span>
          </div>
        </div>
      </div>

      {Object.keys(scenario).length ? (
        <div className="rounded-xl bg-slate-50/70 p-3">
          <div className="flex items-center gap-2 text-[10px] uppercase tracking-[0.08em] text-slate-500">
            <Compass className="size-3" />情景分析
          </div>
          <div className="mt-1.5 grid gap-2 sm:grid-cols-3">
            {(["upside", "base", "downside"] as const).map((key) => {
              const block = safeRecord(scenario[key])
              if (!Object.keys(block).length) return null
              const condition = fmt(block.condition ?? block.trigger)
              const observation = fmt(block.observation)
              const confidenceValue = toNumber(block.confidence)
              const tone =
                key === "upside" ? "bg-rose-50/60" : key === "downside" ? "bg-emerald-50/60" : "bg-slate-100/70"
              return (
                <div key={key} className={`rounded-xl p-2 ${tone}`}>
                  <div className="text-[10px] uppercase tracking-[0.08em] text-slate-500">
                    {key === "upside" ? "乐观" : key === "downside" ? "悲观" : "基准"}
                  </div>
                  <div className="mt-1 space-y-1 text-[11px] text-slate-700">
                    {condition !== "—" ? <div className="break-words">条件：{condition}</div> : null}
                    {observation !== "—" ? <div className="break-words">观察：{observation}</div> : null}
                    {confidenceValue != null ? <div className="font-mono text-slate-500">置信：{confidenceValue.toFixed(2)}</div> : null}
                  </div>
                </div>
              )
            })}
          </div>
        </div>
      ) : null}

      {(Object.keys(support).length || Object.keys(resistance).length) ? (
        <div className="rounded-xl bg-slate-50/70 p-3">
          <div className="text-[10px] uppercase tracking-[0.08em] text-slate-500">支撑 / 压力</div>
          <div className="mt-1.5 grid gap-1 text-[11px] text-slate-600 sm:grid-cols-2">
            <div><span className="text-slate-400">支撑：</span>{zoneText(support)}</div>
            <div><span className="text-slate-400">压力：</span>{zoneText(resistance)}</div>
          </div>
        </div>
      ) : null}

      {evidence.length || invalidList.length || situationEvidence.length ? (
        <div className="rounded-xl bg-slate-50/70 p-3">
          {evidence.length ? (
            <div>
              <div className="text-[10px] uppercase tracking-[0.08em] text-slate-500">趋势关键证据</div>
              <ul className="mt-1 list-disc pl-4 text-[11px] leading-5 text-slate-600">
                {evidence.map((line) => (
                  <li key={line}>{line}</li>
                ))}
              </ul>
            </div>
          ) : null}
          {situationEvidence.length ? (
            <div className="mt-2">
              <div className="text-[10px] uppercase tracking-[0.08em] text-slate-500">位置关键证据</div>
              <ul className="mt-1 list-disc pl-4 text-[11px] leading-5 text-slate-600">
                {situationEvidence.map((line) => (
                  <li key={line}>{line}</li>
                ))}
              </ul>
            </div>
          ) : null}
          {invalidList.length ? (
            <div className="mt-2">
              <div className="text-[10px] uppercase tracking-[0.08em] text-slate-500">失效条件</div>
              <ul className="mt-1 list-disc pl-4 text-[11px] leading-5 text-slate-600">
                {invalidList.map((line) => (
                  <li key={line}>{line}</li>
                ))}
              </ul>
            </div>
          ) : null}
        </div>
      ) : null}
    </div>
  )
}

function DirectionSummaryChip({
  label,
  value,
  tone,
}: {
  label: string
  value: string | number
  tone?: string
}) {
  return (
    <span className="inline-flex items-center gap-1.5 rounded-full bg-slate-50 px-2 py-1 text-[11px] text-slate-500">
      <span>{label}</span>
      <span className={cn("font-mono text-sm font-semibold leading-none", tone ?? "text-slate-700")}>{value}</span>
    </span>
  )
}

function scoreTone(value: number | null) {
  if (value == null || !Number.isFinite(value)) return "text-slate-700"
  if (value >= 70) return "text-red-600"
  if (value >= 45) return "text-amber-600"
  return "text-emerald-600"
}

function returnTone(value: number | null) {
  if (value == null || !Number.isFinite(value)) return "text-slate-700"
  if (value > 0) return "text-red-600"
  if (value < 0) return "text-emerald-600"
  return "text-slate-700"
}

function average(values: Array<number | null>) {
  const valid = values.filter((value): value is number => value != null && Number.isFinite(value))
  if (!valid.length) return null
  return valid.reduce((sum, value) => sum + value, 0) / valid.length
}

function FiveDayAIDirectionOverview({
  items,
}: {
  items: ApplicationAnalysisRecent30FullItem[]
}) {
  const recent = useMemo(
    () => [...items].sort((left, right) => right.date.localeCompare(left.date)).slice(0, 5),
    [items],
  )
  const latest = recent[0]
  const latestTrend = safeRecord(latest?.snapshot?.short_term_trend)
  const latestSituation = safeRecord(latest?.snapshot?.current_situation)
  const latestMomentum = safeRecord(latestTrend.momentum)
  const latestBias = biasTone(latestTrend.bias)
  const latestReturn5d = toNumber(latestMomentum.return_5d)
  const latestState = fmt(latestTrend.state)
  const latestPosition = fmt(latestSituation.position)
  const latestPhase = fmt(latestSituation.phase)

  const scored = recent
    .map((item) => {
      const trend = safeRecord(item.snapshot?.short_term_trend)
      return {
        date: item.date,
        biasKey: typeof trend.bias === "string" ? trend.bias.toLowerCase() : "",
        bias: biasTone(trend.bias),
        score: toNumber(trend.score),
        confidence: toNumber(trend.confidence),
        state: fmt(trend.state),
      }
    })
    .filter((item) => item.state !== "—" || item.score != null || item.confidence != null)

  const bullishCount = scored.filter((item) => item.biasKey.includes("bull") || item.bias.label.includes("多")).length
  const bearishCount = scored.filter((item) => item.biasKey.includes("bear") || item.bias.label.includes("空")).length
  const neutralCount = Math.max(0, scored.length - bullishCount - bearishCount)
  const avgScore = average(scored.map((item) => item.score))
  const avgConfidence = average(scored.map((item) => item.confidence))

  if (!recent.length) return null

  return (
    <div className="rounded-xl bg-white/70 px-3 py-2.5">
      <div className="flex flex-col gap-2 xl:flex-row xl:items-center xl:justify-between">
        <div className="flex min-w-0 flex-wrap items-center gap-x-2 gap-y-1">
          <span className="text-sm font-semibold text-slate-900">近5日趋势</span>
          <Badge className={`rounded-full border px-2 py-0 text-[10px] font-medium ${latestBias.cls}`} variant="outline">
            {latestBias.label}
          </Badge>
          <span className="text-[11px] text-slate-300">·</span>
          <span className="text-[11px] text-slate-500">{latestState}</span>
          {latest?.date ? <span className="font-mono text-[11px] text-slate-400">{latest.date}</span> : null}
        </div>
        <div className="flex min-w-0 flex-wrap items-center gap-1.5">
          <DirectionSummaryChip label="5D" value={latestReturn5d == null ? "—" : fmtPercent(latestReturn5d)} tone={returnTone(latestReturn5d)} />
          <DirectionSummaryChip label="Bias" value={`${bullishCount}/${neutralCount}/${bearishCount}`} />
          <DirectionSummaryChip label="Score" value={avgScore == null ? "—" : avgScore.toFixed(1)} tone={scoreTone(avgScore)} />
          <DirectionSummaryChip label="Confidence" value={avgConfidence == null ? "—" : avgConfidence.toFixed(1)} />
        </div>
      </div>

      <div className="mt-2 flex min-w-0 flex-wrap items-center gap-1.5">
        {scored.map((item) => (
          <span key={item.date} className="inline-flex items-center gap-1 rounded-full bg-slate-50 px-2 py-1 text-[11px] text-slate-500">
            <span className="font-mono">{item.date.slice(5)}</span>
            <span className={cn("font-mono font-semibold", scoreTone(item.score))}>{item.score == null ? "—" : item.score.toFixed(0)}</span>
            <span className={cn("rounded-full border px-1.5 py-0 text-[10px]", item.bias.cls)}>{item.bias.label}</span>
          </span>
        ))}
        {latestPosition !== "—" || latestPhase !== "—" ? (
          <span className="inline-flex items-center gap-1 rounded-full bg-slate-50 px-2 py-1 text-[11px] text-slate-500">
            <MapPin className="size-3 text-slate-400" />
            <span>{latestPosition}</span>
            {latestPhase !== "—" ? <span className="text-slate-300">·</span> : null}
            {latestPhase !== "—" ? <span>{latestPhase}</span> : null}
          </span>
        ) : null}
      </div>
    </div>
  )
}

function DateItem({
  item,
  defaultExpanded,
}: {
  item: ApplicationAnalysisRecent30FullItem
  defaultExpanded: boolean
}) {
  const [expanded, setExpanded] = useState(defaultExpanded)
  const trend = safeRecord(item.snapshot?.short_term_trend)
  const situation = safeRecord(item.snapshot?.current_situation)
  const hasData = Object.keys(trend).length > 0 || Object.keys(situation).length > 0
  const stateLabel = fmt(trend.state)
  const bias = biasTone(trend.bias)
  const position = fmt(situation.position)
  const phase = fmt(situation.phase)
  const updatedAtText = item.updated_at
    ? new Date(item.updated_at).toLocaleString("zh-CN", { hour12: false })
    : ""

  return (
    <div className="min-w-0 rounded-xl bg-white/70">
      <button
        type="button"
        onClick={() => setExpanded((v) => !v)}
        className="sticky top-0 z-10 flex w-full items-center rounded-xl bg-white/95 px-3 py-2.5 text-left transition hover:bg-slate-50/90"
      >
        <div className="flex min-w-0 flex-1 flex-wrap items-center gap-x-3 gap-y-1.5">
          <div className="flex min-w-0 flex-wrap items-center gap-x-2 gap-y-1 text-sm font-semibold text-slate-900">
            <span className="font-mono">{item.date}</span>
            {defaultExpanded ? (
              <Badge className="rounded-full border-slate-200 bg-slate-50 px-1.5 py-0 text-[10px] text-slate-600" variant="outline">Latest</Badge>
            ) : null}
            {updatedAtText ? (
              <>
                <span className="text-[11px] font-normal text-slate-300">·</span>
                <span className="text-[11px] font-normal text-slate-500">Updated {updatedAtText}</span>
              </>
            ) : null}
          </div>
          <div className="flex min-w-0 flex-wrap items-center gap-x-2 gap-y-1 text-[11px] text-slate-500">
            {hasData ? (
              <>
                <span>{stateLabel || "—"}</span>
                <Badge className={`rounded-full border px-1.5 py-0 text-[10px] ${bias.cls}`} variant="outline">{bias.label}</Badge>
                <span className="text-slate-300">·</span>
                <span>{position || "—"}</span>
                {phase !== "—" ? (
                  <>
                    <span className="text-slate-300">·</span>
                    <span>{phase}</span>
                  </>
                ) : null}
              </>
            ) : (
              <span className="text-slate-400">该日期暂无 AI 整体判断数据</span>
            )}
          </div>
        </div>
      </button>
      {expanded ? <div className="px-3 pb-3"><DirectionDetails trend={trend} situation={situation} /></div> : null}
    </div>
  )
}

function AIDirectionSkeleton() {
  return (
    <div className="space-y-3">
      <div className="flex flex-col gap-2 rounded-xl bg-white/70 px-3 py-2 sm:flex-row sm:flex-wrap sm:items-center sm:justify-between">
        <Skeleton className="h-4 w-3/4 max-w-[520px]" />
        <Skeleton className="h-7 w-32 rounded-lg" />
      </div>
      <Skeleton className="h-16 rounded-xl bg-white/70" />
      <div className="space-y-1.5">
        {[0, 1, 2, 3, 4].map((index) => (
          <div key={index} className="rounded-xl bg-white/70 px-3 py-2.5">
            <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
              <Skeleton className="h-4 w-36" />
              <Skeleton className="h-4 w-64" />
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

export function AIDirectionCard({
  dailySnapshotsFull,
  dailySnapshotsLoading,
  dailyRefreshing,
  dailyLastRefreshAt,
  onRefreshDaily,
}: {
  collapsed?: boolean
  onToggle?: () => void
  dailySnapshotsFull: ApplicationAnalysisRecent30FullItem[]
  dailySnapshotsLoading: boolean
  dailyRefreshing: boolean
  dailyLastRefreshAt: string | null
  onRefreshDaily: () => void
}) {
  const lastRefreshText = dailyLastRefreshAt
    ? new Date(dailyLastRefreshAt).toLocaleString("zh-CN", { hour12: false })
    : "尚未手动触发"

  const latestDate = dailySnapshotsFull[0]?.date

  return (
    <section className="min-w-0 space-y-2 rounded-2xl bg-muted/50 p-3">
      <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div className="text-sm font-semibold text-slate-900">AI Direction</div>
          <div className="text-[11px] text-slate-500">Persisted by trading day · newest first · {dailySnapshotsFull.length} sessions</div>
        </div>
        {latestDate ? <Badge variant="outline" className="w-fit rounded-full bg-white px-2 py-0 text-[10px]">Latest {latestDate}</Badge> : null}
      </div>

      {dailySnapshotsLoading && dailySnapshotsFull.length === 0 ? (
        <AIDirectionSkeleton />
      ) : (
      <div className="space-y-3">
        <div className="flex flex-col gap-2 rounded-xl bg-white/70 px-3 py-2 sm:flex-row sm:flex-wrap sm:items-center sm:justify-between">
          <div className="min-w-0 break-words text-[11px] text-slate-500">
            AI 方向按日写入 <span className="font-mono">cynexus_appl_ai.application_analysis_daily_snapshots</span>，由 Java 侧从行情 K 线构造输入并调用 cynexus-ai
          </div>
          <div className="grid grid-cols-1 gap-2 sm:flex sm:items-center">
            <span className="text-[10px] text-slate-400">上次刷新 {lastRefreshText}</span>
            <Button
              size="sm"
              variant="secondary"
              className="h-8 w-full rounded-lg px-2.5 text-[11px] sm:h-7 sm:w-auto"
              onClick={onRefreshDaily}
              disabled={dailyRefreshing}
            >
              <RefreshCw className={`mr-1 size-3 ${dailyRefreshing ? "animate-spin" : ""}`} />
              {dailyRefreshing ? "生成中" : "重新生成 AI 方向"}
            </Button>
          </div>
        </div>

        <FiveDayAIDirectionOverview items={dailySnapshotsFull} />

        {dailySnapshotsFull.length === 0 ? (
          <div className="rounded-2xl border border-dashed border-slate-200 bg-slate-50/60 px-4 py-6 text-center text-[11px] text-slate-500">
            {dailySnapshotsLoading ? "加载中..." : "还没有 AI 方向数据。点击上方「重新生成 AI 方向」触发一次。"}
          </div>
        ) : (
          <div className="space-y-1.5">
            {dailySnapshotsFull.map((item) => (
              <DateItem
                key={item.date}
                item={item}
                defaultExpanded={item.date === latestDate}
              />
            ))}
          </div>
        )}
      </div>
      )}
    </section>
  )
}
