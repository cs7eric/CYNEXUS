import { useEffect, useMemo, useState } from "react"
import { ArrowDownRight, ArrowUpRight, CheckCircle2, ChevronRight, CircleDashed, ShieldAlert, Sparkles } from "lucide-react"

import { Button } from "@/components/ui/button"
import type { MarketIndicatorIndustry, MarketIndicatorResponse, MarketIndicatorTransmissionRecord, MarketIndicatorTransmissionResponse } from "@/services/market/indicator"

function value(record: MarketIndicatorTransmissionRecord | undefined, camel: string, snake: string) {
  return record?.[camel] ?? record?.[snake]
}

function numberValue(record: MarketIndicatorTransmissionRecord | undefined, camel: string, snake: string) {
  const raw = value(record, camel, snake)
  return typeof raw === "number" ? raw : null
}

function textValue(record: MarketIndicatorTransmissionRecord | undefined, camel: string, snake: string) {
  const raw = value(record, camel, snake)
  return raw == null ? null : String(raw)
}

function percent(raw: number | null, ratio = false) {
  if (raw == null) return "-"
  return `${(ratio ? raw * 100 : raw).toFixed(1)}%`
}

function formatTime(raw?: string | null) {
  if (!raw) return "-"
  const date = new Date(raw)
  return Number.isNaN(date.getTime()) ? raw : date.toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit", second: "2-digit" })
}

function sectorName(record: MarketIndicatorTransmissionRecord) {
  return textValue(record, "sectorName", "sector_name") ?? textValue(record, "targetName", "target_name") ?? "未命名板块"
}

function stageLabel(stage?: string | null) {
  return ({ emerging: "萌芽", confirmed: "已确认", spreading: "扩散", crowded: "拥挤", decaying: "衰减", failed: "失败" } as Record<string, string>)[String(stage)] ?? "待识别"
}

function reviewStageLabel(stage?: string | null) {
  return ({ emerging: "观察中", confirmed: "确认扩散", spreading: "持续扩散", crowded: "拥挤风险", decaying: "扩散衰减", failed: "复盘失败" } as Record<string, string>)[String(stage)] ?? "待识别"
}

function evidence(record: MarketIndicatorTransmissionRecord) {
  const candidates = value(record, "leaderCandidates", "leader_candidates")
  if (Array.isArray(candidates) && candidates.length) return candidates[0] as Record<string, unknown>
  const rawPayload = record.payload
  try {
    const payload = typeof rawPayload === "string" ? JSON.parse(rawPayload) as Record<string, unknown> : rawPayload
    const nested = payload && typeof payload === "object" ? (payload.leader_candidates ?? payload.leaderCandidates) : null
    return Array.isArray(nested) && nested.length ? nested[0] as Record<string, unknown> : null
  } catch {
    return null
  }
}

function payloadObject(record: MarketIndicatorTransmissionRecord) {
  try {
    return typeof record.payload === "string" ? JSON.parse(record.payload) as Record<string, unknown> : (record.payload ?? {}) as Record<string, unknown>
  } catch {
    return {} as Record<string, unknown>
  }
}

function leaderName(record: MarketIndicatorTransmissionRecord) {
  const leader = evidence(record)
  return String(leader?.name ?? leader?.stock_name ?? leader?.code ?? leader?.stock_code ?? "-")
}

type Observation = { industry: MarketIndicatorIndustry; direction: "rise" | "fall"; score: number }
type SectorMarketStat = { averageChangePct: number | null; amount: number; limitUp: number; limitDown: number; limitUpTouch: number; limitDownTouch: number }

function buildSectorMarketStats(overview?: MarketIndicatorResponse | null) {
  const grouped = new Map<string, { change: number; changeCount: number; amount: number; limitUp: number; limitDown: number; limitUpTouch: number; limitDownTouch: number }>()
  for (const row of overview?.snapshots ?? []) {
    const industry = row.industry?.trim()
    if (!industry) continue
    const current = grouped.get(industry) ?? { change: 0, changeCount: 0, amount: 0, limitUp: 0, limitDown: 0, limitUpTouch: 0, limitDownTouch: 0 }
    if (row.changePct != null) { current.change += row.changePct; current.changeCount += 1 }
    current.amount += row.amount ?? 0
    if (row.limitStatus?.startsWith("limit_up_sealed")) current.limitUp += 1
    else if (row.limitStatus?.startsWith("limit_up")) current.limitUpTouch += 1
    if (row.limitStatus?.startsWith("limit_down_sealed")) current.limitDown += 1
    else if (row.limitStatus?.startsWith("limit_down")) current.limitDownTouch += 1
    grouped.set(industry, current)
  }
  return new Map<string, SectorMarketStat>([...grouped].map(([industry, stat]) => [industry, {
    averageChangePct: stat.changeCount ? stat.change / stat.changeCount : null,
    amount: stat.amount, limitUp: stat.limitUp, limitDown: stat.limitDown,
    limitUpTouch: stat.limitUpTouch, limitDownTouch: stat.limitDownTouch,
  }]))
}

function amountLabel(value?: number | null) {
  if (value == null) return "-"
  if (Math.abs(value) >= 100_000_000) return `${(value / 100_000_000).toFixed(1)}亿`
  if (Math.abs(value) >= 10_000) return `${(value / 10_000).toFixed(0)}万`
  return value.toFixed(0)
}

function buildObservations(overview?: MarketIndicatorResponse | null): Observation[] {
  return (overview?.industries ?? []).map((industry) => {
    const up = industry.advancingCount ?? 0
    const down = industry.decliningCount ?? 0
    const active = Math.max(1, up + down)
    const direction = up >= down ? "rise" as const : "fall" as const
    const imbalance = Math.abs(up - down) / active
    const eventWeight = (industry.limitUpCount ?? 0) + (industry.limitDownCount ?? 0) + (industry.rapidRiseCount ?? 0) + (industry.rapidFallCount ?? 0)
    return { industry, direction, score: imbalance + Math.min(0.5, eventWeight * 0.08) }
  }).filter((item) => item.score >= 0.28).sort((a, b) => b.score - a.score).slice(0, 12)
}

export function TransmissionPanel({ data, overview, loading, mode, onSaveNextDay }: { data: MarketIndicatorTransmissionResponse | null; overview: MarketIndicatorResponse | null; loading: boolean; mode: "radar" | "review"; onSaveNextDay?: () => void }) {
  const episodes = data?.episodes ?? []
  const observations = useMemo(() => buildObservations(overview), [overview])
  const sectorMarketStats = useMemo(() => buildSectorMarketStats(overview), [overview])
  const [selectedKey, setSelectedKey] = useState<string | null>(null)
  const rows = episodes.length ? episodes : observations
  useEffect(() => {
    const valid = episodes.length
      ? episodes.some((item, index) => String(value(item, "episodeId", "episode_id") ?? `episode-${index}`) === selectedKey)
      : observations.some((item, index) => (item.industry.industryCode ?? `observation-${index}`) === selectedKey)
    if (!valid && rows.length) setSelectedKey(episodes.length ? String(value(episodes[0], "episodeId", "episode_id") ?? "episode-0") : observations[0]?.industry.industryCode ?? "observation-0")
  }, [episodes, observations, rows.length, selectedKey])

  const selectedEpisode = episodes.find((item, index) => String(value(item, "episodeId", "episode_id") ?? `episode-${index}`) === selectedKey)
  const selectedObservation = observations.find((item, index) => (item.industry.industryCode ?? `observation-${index}`) === selectedKey)
  const advice = data?.advice ?? []
  const outcomes = data?.outcomes ?? []

  return (
    <section className="overflow-hidden rounded-lg border border-border/70 bg-card" aria-label="板块传导工作台">
      <div className="flex flex-col gap-3 border-b border-border/60 px-4 py-3 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <div className="flex items-center gap-2"><Sparkles className="size-4 text-information" /><h2 className="text-sm font-semibold">{mode === "review" ? "全天传导复盘" : "板块传导排行"}</h2></div>
          <p className="mt-1 text-xs text-muted-foreground">按“首动时间 → 成分股响应 → 去首动股表现 → 持续性”解释板块来源，不再用映射分数代替原因。</p>
        </div>
        <div className="flex items-center gap-2 text-[11px] text-muted-foreground"><span className={`size-1.5 rounded-full ${episodes.length ? "bg-red-500" : "bg-amber-500"}`} />{episodes.length ? `${episodes.length} 个已计算 Episode` : "当前无合格 Episode，下方仅展示板块状态观测"}</div>
      </div>

      <div className="grid min-h-[390px] xl:h-[720px] xl:grid-cols-[minmax(0,3fr)_minmax(0,7fr)]">
        <div className="min-w-0 border-b border-border/60 xl:flex xl:min-h-0 xl:flex-col xl:border-b-0 xl:border-r">
          <div className="sticky top-0 z-10 grid grid-cols-[62px_minmax(120px,1fr)_90px_100px_105px_24px] gap-2 border-b border-border/60 bg-muted/95 px-3 py-2 text-[11px] text-muted-foreground backdrop-blur">
            <span>起始</span><span>板块 / 阶段</span><span>首动</span><span>板块行情</span><span>传导证据</span><span />
          </div>
          <div className="max-h-[520px] overflow-y-auto overscroll-contain xl:min-h-0 xl:max-h-none xl:flex-1">
          {loading ? <div className="p-8 text-center text-xs text-muted-foreground">正在刷新传导事实…</div> : rows.length === 0 ? <div className="m-4 rounded-md border border-dashed p-6 text-center text-xs text-muted-foreground">没有达到展示门槛的板块状态，也没有确认的传导 Episode。</div> : episodes.length ? episodes.map((episode, index) => {
            const key = String(value(episode, "episodeId", "episode_id") ?? `episode-${index}`)
            const rising = String(episode.direction) !== "fall"
            const market = sectorMarketStats.get(sectorName(episode))
            const marketRising = (market?.averageChangePct ?? 0) >= 0
            return <button type="button" key={key} onClick={() => setSelectedKey(key)} className={`grid w-full grid-cols-[62px_minmax(120px,1fr)_90px_100px_105px_24px] items-center gap-2 border-b border-border/50 px-3 py-3 text-left text-xs transition-colors hover:bg-muted/30 ${selectedKey === key ? "bg-information/5" : ""}`}>
              <span className="font-mono text-[11px] text-muted-foreground">{formatTime(textValue(episode, "startedAt", "started_at"))}</span>
              <span className="min-w-0"><span className="flex items-center gap-1.5 font-medium text-foreground">{rising ? <ArrowUpRight className="size-3.5 text-red-500" /> : <ArrowDownRight className="size-3.5 text-emerald-500" />}{sectorName(episode)}</span><span className="mt-1 block text-[11px] text-muted-foreground">{mode === "review" ? (rising ? "上涨方向" : "下跌方向") : (rising ? "拉升" : "打压")} · {mode === "review" ? reviewStageLabel(String(episode.stage ?? "")) : stageLabel(String(episode.stage ?? ""))}</span></span>
              <span className="truncate text-foreground">{leaderName(episode)}</span>
              <span><span className={`block font-mono ${marketRising ? "text-red-500" : "text-emerald-500"}`}>{percent(market?.averageChangePct ?? null)}</span><small className="mt-1 block font-mono text-muted-foreground">额 {amountLabel(market?.amount)}</small></span>
              <span><span className="block font-mono">{String(numberValue(episode, "respondedCount", "responded_count") ?? "-")} / {String(numberValue(episode, "eligibleCount", "eligible_count") ?? "-")} <small className="text-muted-foreground">{percent(numberValue(episode, "coverageRatio", "coverage_ratio"), true)}</small></span><small className={`mt-1 block font-mono ${rising ? "text-red-500" : "text-emerald-500"}`}>去首动 {percent(numberValue(episode, "exLeaderReturnPct", "ex_leader_return_pct"))}</small></span><ChevronRight className="size-4 text-muted-foreground" />
            </button>
          }) : observations.map((item, index) => {
            const key = item.industry.industryCode ?? `observation-${index}`
            const industry = item.industry
            const responded = item.direction === "rise" ? industry.advancingCount ?? 0 : industry.decliningCount ?? 0
            const eligible = Math.max(1, industry.stockCount ?? 0)
            const market = sectorMarketStats.get(industry.industryName)
            const marketRising = (market?.averageChangePct ?? 0) >= 0
            return <button type="button" key={key} onClick={() => setSelectedKey(key)} className={`grid w-full grid-cols-[62px_minmax(120px,1fr)_90px_100px_105px_24px] items-center gap-2 border-b border-border/50 px-3 py-3 text-left text-xs transition-colors hover:bg-muted/30 ${selectedKey === key ? "bg-amber-500/5" : ""}`}>
              <span className="font-mono text-[11px] text-muted-foreground">{formatTime(industry.firstEventAt)}</span><span><span className="flex items-center gap-1.5 font-medium">{item.direction === "rise" ? <ArrowUpRight className="size-3.5 text-red-500" /> : <ArrowDownRight className="size-3.5 text-emerald-500" />}{industry.industryName}</span><span className="mt-1 block text-[11px] text-amber-600">状态观测 · 未确认</span></span><span className="truncate">{industry.firstEventName ?? "无首动事件"}</span><span><span className={`block font-mono ${marketRising ? "text-red-500" : "text-emerald-500"}`}>{percent(market?.averageChangePct ?? null)}</span><small className="mt-1 block font-mono text-muted-foreground">额 {amountLabel(market?.amount)}</small></span><span><span className="block font-mono">{responded} / {eligible} <small className="text-muted-foreground">{percent(responded / eligible, true)}</small></span><small className="mt-1 block text-muted-foreground">去首动 未计算</small></span><ChevronRight className="size-4 text-muted-foreground" />
            </button>
          })}
          </div>
        </div>

        <aside className="min-w-0 p-4 xl:overflow-y-auto">
          {selectedEpisode ? <EpisodeDetail episode={selectedEpisode} market={sectorMarketStats.get(sectorName(selectedEpisode))} review={mode === "review"} /> : selectedObservation ? <ObservationDetail item={selectedObservation} /> : <div className="flex h-full items-center justify-center text-xs text-muted-foreground">选择左侧板块查看推导证据</div>}
          <div className="mt-4 border-t border-border/60 pt-4">
            <div className="flex items-center justify-between gap-2"><div className="flex items-center gap-2 text-xs font-medium"><ShieldAlert className="size-3.5 text-amber-500" />条件化参考</div><Button type="button" variant="outline" size="sm" className="h-7 text-[11px]" onClick={onSaveNextDay} disabled={!advice.length}>保存次日关注</Button></div>
            <p className="mt-2 text-[11px] leading-5 text-muted-foreground">{advice.length ? `${advice.length} 条参考；${outcomes.length} 条已有盘后结果。` : "没有通过证据门槛的参考，不因板块涨跌或单股异动自动生成建议。"}</p>
          </div>
        </aside>
      </div>
    </section>
  )
}

function EpisodeDetail({ episode, market, review }: { episode: MarketIndicatorTransmissionRecord; market?: SectorMarketStat; review: boolean }) {
  const rising = String(episode.direction) !== "fall"
  const payload = payloadObject(episode)
  const coverage = numberValue(episode, "coverageRatio", "coverage_ratio")
  const exLeader = numberValue(episode, "exLeaderReturnPct", "ex_leader_return_pct")
  const lag = numberValue(episode, "medianLagSeconds", "median_lag_seconds")
  const benchmark = numberValue(episode, "benchmarkExcessPct", "benchmark_excess_pct")
  const equalReturn = numberValue(episode, "sectorEqualReturnPct", "sector_equal_return_pct")
  const medianReturn = numberValue(episode, "sectorMedianReturnPct", "sector_median_return_pct")
  const breadthDelta = numberValue(episode, "breadthDeltaPctPoint", "breadth_delta_pct_point")
  const p75Lag = numberValue(episode, "p75LagSeconds", "p75_lag_seconds")
  const confidence = numberValue(episode, "confidenceScore", "confidence_score")
  const quality = textValue(episode, "dataQualityGrade", "data_quality_grade") ?? "-"
  const leader = evidence(episode)
  const members = Array.isArray(payload.members) ? payload.members as Array<Record<string, unknown>> : []
  const firstWave = members.filter((member) => member.role === "first_wave")
  const secondWave = members.filter((member) => member.role === "second_wave")
  const failureReasons = Array.isArray(payload.failure_reasons) ? payload.failure_reasons.map(String) : []
  const confirmed = !episode.fullConfirmationRequired && coverage != null && coverage >= 0.15 && exLeader != null && (rising ? exLeader > 0 : exLeader < 0)
  const reviewConclusion = confirmed ? (rising ? "上涨扩散有效" : "下跌扩散有效") : (rising ? "上涨线索未完成确认" : "下跌线索未完成确认")
  return <div>
    <div className="flex items-start justify-between gap-3"><div><p className="text-xs text-muted-foreground">当前选择 · {review ? (rising ? "上涨方向" : "下跌方向") : (rising ? "板块拉升" : "板块打压")}</p><h3 className="mt-1 text-base font-semibold">{sectorName(episode)}</h3><p className="mt-1 text-[11px] text-muted-foreground">{formatTime(textValue(episode, "startedAt", "started_at"))} 开始 · {review ? reviewStageLabel(String(episode.stage ?? "")) : stageLabel(String(episode.stage ?? ""))} · 数据质量 {quality}</p></div><span className={`rounded-full px-2 py-1 text-[11px] ${confirmed ? "bg-red-500/10 text-red-600" : "bg-amber-500/10 text-amber-600"}`}>{reviewConclusion}</span></div>

    <div className="mt-4 rounded-md border border-border/60 bg-muted/15 p-3"><p className="text-xs font-medium">{review ? "复盘判断依据" : "为什么推导出这个板块"}</p><p className="mt-2 text-xs leading-5 text-muted-foreground">{leaderName(episode)} 首先出现{review ? (rising ? "上涨" : "下跌") : (rising ? "拉升" : "打压")}线索，随后 {String(numberValue(episode, "respondedCount", "responded_count") ?? "-")} / {String(numberValue(episode, "eligibleCount", "eligible_count") ?? "-")} 只完整行业成分股同向响应；行业等权窗口表现 {percent(equalReturn)}，去掉首动候选后仍为 <strong className={rising ? "text-red-500" : "text-emerald-500"}>{percent(exLeader)}</strong>。{confirmed ? "覆盖率、横截面广度和去首动股表现共同通过门槛，因此进入确认排行。" : `尚未形成完整确认${failureReasons.length ? `：${failureReasons.join("、")}` : "。"}`}</p></div>

    <div className="mt-4 rounded-md border border-red-500/15 bg-red-500/[0.03] p-3"><div className="flex items-center justify-between gap-2"><p className="text-xs font-medium">涨跌停与情绪证据</p><span className="text-[11px] text-muted-foreground">来自行业成分股快照</span></div><div className="mt-2 grid grid-cols-2 gap-2 text-xs"><DetailMetric label="涨停 / 跌停" value={`${market?.limitUp ?? 0} / ${market?.limitDown ?? 0}`} /><DetailMetric label="触板 / 触底" value={`${market?.limitUpTouch ?? 0} / ${market?.limitDownTouch ?? 0}`} /><DetailMetric label="板块成交额" value={amountLabel(market?.amount)} /><DetailMetric label="行业平均涨跌" value={percent(market?.averageChangePct ?? null)} /></div><p className="mt-2 text-[11px] leading-5 text-muted-foreground">涨跌停数量只说明情绪强度，不能单独证明传导；需要与响应覆盖、跟随时序和去首动股表现同时成立。</p></div>

    <div className="mt-4 grid grid-cols-2 gap-2 lg:grid-cols-4 xl:grid-cols-2">
      <Evidence label="响应覆盖" value={`${String(numberValue(episode, "respondedCount", "responded_count") ?? "-")} / ${String(numberValue(episode, "eligibleCount", "eligible_count") ?? "-")} · ${percent(coverage, true)}`} pass={coverage != null && coverage >= 0.15} />
      <Evidence label="跟随延迟" value={lag == null ? "缺失" : `中位 ${lag}s / P75 ${p75Lag ?? "-"}s`} pass={lag != null} />
      <Evidence label="行业等权 / 中位" value={`${percent(equalReturn)} / ${percent(medianReturn)}`} pass={equalReturn != null} />
      <Evidence label="去首动股表现" value={percent(exLeader)} pass={exLeader != null && (rising ? exLeader > 0 : exLeader < 0)} />
      <Evidence label="相对基准" value={percent(benchmark)} pass={benchmark != null && (rising ? benchmark > 0 : benchmark < 0)} />
      <Evidence label="广度变化" value={breadthDelta == null ? "缺失" : `${breadthDelta.toFixed(1)}pct`} pass={breadthDelta != null && breadthDelta >= 5} />
      <Evidence label="综合置信度" value={percent(confidence, true)} pass={confidence != null && confidence >= 0.65} />
      <Evidence label="数据质量" value={quality} pass={quality === "A" || quality === "B"} />
    </div>

    <div className="mt-4 rounded-md border border-border/60 p-3"><div className="flex items-center justify-between gap-2"><p className="text-xs font-medium">首动候选证据</p><span className="font-mono text-[11px] text-muted-foreground">{String(leader?.code ?? "-")}</span></div><div className="mt-2 grid grid-cols-2 gap-x-4 gap-y-2 text-[11px]"><DetailMetric label="候选股票" value={String(leader?.name ?? leaderName(episode))} /><DetailMetric label="角色" value={String(leader?.role ?? "待识别")} /><DetailMetric label="时间领先" value={percent(typeof leader?.timingLead === "number" ? leader.timingLead : null, true)} /><DetailMetric label="跟随响应" value={percent(typeof leader?.followerResponse === "number" ? leader.followerResponse : null, true)} /><DetailMetric label="相对强度" value={percent(typeof leader?.relativeStrength === "number" ? leader.relativeStrength : null, true)} /><DetailMetric label="去首动持续" value={percent(typeof leader?.exLeaderPersistence === "number" ? leader.exLeaderPersistence : null, true)} /></div></div>

    <div className="mt-4 rounded-md border border-border/60 p-3"><div className="flex items-center justify-between gap-2"><p className="text-xs font-medium">成分股响应梯队</p><span className="text-[11px] text-muted-foreground">第一批 {firstWave.length} · 第二批 {secondWave.length}</span></div>{members.length ? <div className="mt-2 space-y-1.5">{members.slice(0, 6).map((member, index) => <div key={`${String(member.code)}-${index}`} className="flex items-center justify-between gap-3 rounded bg-muted/25 px-2.5 py-1.5 text-[11px]"><span className="min-w-0 truncate text-foreground">{String(member.name ?? member.code ?? "-")} <span className="font-mono text-muted-foreground">{String(member.code ?? "")}</span></span><span className="shrink-0 text-muted-foreground">{member.role === "first_wave" ? "第一批" : "第二批"} · {String(member.lag_seconds ?? "-")}s · {percent(typeof member.return_at_response_pct === "number" ? member.return_at_response_pct : null)}</span></div>)}</div> : <p className="mt-2 text-[11px] text-muted-foreground">当前记录没有保存可展开的成分股梯队。</p>}</div>
  </div>
}

function DetailMetric({ label, value }: { label: string; value: string }) {
  return <div><span className="text-muted-foreground">{label}</span><span className="ml-2 font-medium text-foreground">{value}</span></div>
}

function ObservationDetail({ item }: { item: Observation }) {
  const industry = item.industry
  const sameDirection = item.direction === "rise" ? industry.advancingCount ?? 0 : industry.decliningCount ?? 0
  const total = Math.max(1, industry.stockCount ?? 0)
  return <div><div className="flex items-start justify-between gap-3"><div><p className="text-xs text-muted-foreground">板块状态观测</p><h3 className="mt-1 text-base font-semibold">{industry.industryName}</h3></div><span className="rounded-full bg-amber-500/10 px-2 py-1 text-[11px] text-amber-600">不是传导结论</span></div><div className="mt-4 rounded-md border border-amber-500/20 bg-amber-500/5 p-3"><p className="text-xs font-medium">为什么出现在候选区</p><p className="mt-2 text-xs leading-5 text-muted-foreground">当前板块有 {sameDirection}/{total} 只股票{item.direction === "rise" ? "上涨" : "下跌"}，并出现涨停 {industry.limitUpCount ?? 0}、跌停 {industry.limitDownCount ?? 0}、快速变化 {(industry.rapidRiseCount ?? 0) + (industry.rapidFallCount ?? 0)} 个。这里只说明横截面状态明显；因为缺少连续事件、响应延迟和去首动股窗口收益，不能推导为“由某只股票带动”。</p></div><div className="mt-4 grid grid-cols-2 gap-2"><Evidence label="首个可见事件" value={industry.firstEventName ?? "缺失"} pass={Boolean(industry.firstEventName)} /><Evidence label="同向覆盖" value={percent(sameDirection / total, true)} pass={sameDirection / total >= 0.6} /><Evidence label="响应时间序列" value="缺失" pass={false} /><Evidence label="去首动股验证" value="缺失" pass={false} /></div></div>
}

function Evidence({ label, value, pass }: { label: string; value: string; pass: boolean }) {
  return <div className="rounded-md border border-border/60 px-3 py-2"><div className="flex items-center gap-1.5 text-[11px] text-muted-foreground">{pass ? <CheckCircle2 className="size-3 text-red-500" /> : <CircleDashed className="size-3 text-amber-500" />}{label}</div><div className="mt-1 truncate text-xs font-medium text-foreground">{value}</div></div>
}
