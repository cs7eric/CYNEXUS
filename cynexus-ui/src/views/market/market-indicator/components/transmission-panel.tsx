import { ArrowDownRight, ArrowUpRight, Clock3, ShieldAlert, Sparkles } from "lucide-react"
import { Button } from "@/components/ui/button"

import type { MarketIndicatorTransmissionResponse, MarketIndicatorTransmissionRecord } from "@/services/market/indicator"

function value(record: MarketIndicatorTransmissionRecord, camel: string, snake: string) {
  return record[camel] ?? record[snake]
}

function label(record: MarketIndicatorTransmissionRecord) {
  return String(value(record, "sectorName", "sector_name") ?? value(record, "targetName", "target_name") ?? "未命名板块")
}

function score(record: MarketIndicatorTransmissionRecord) {
  const raw = value(record, "confidenceScore", "confidence_score")
  return typeof raw === "number" ? `${(raw * 100).toFixed(0)}%` : "-"
}

function actionLabel(action: unknown) {
  const labels: Record<string, string> = {
    probe_reference: "低仓位试探参考",
    pullback_confirmation_reference: "回踩确认后关注",
    wait_confirmation: "等待确认",
    do_not_chase: "不追高",
    reduce_avoid_reference: "减仓/规避参考",
    invalidation_exit_reference: "失效退出参考",
    observe: "观察",
  }
  return labels[String(action)] ?? String(action ?? "观察")
}

function scopeLabel(record?: MarketIndicatorTransmissionRecord) {
  if (record?.scope === "priority" || record?.universeMode === "historical_limit") return "快速雷达 · 池内线索"
  return "完整行业确认"
}

function EpisodeCard({ episode }: { episode: MarketIndicatorTransmissionRecord }) {
  const direction = String(episode.direction ?? "")
  const rising = direction === "rise"
  return (
    <article className="rounded-md border border-border/60 bg-background/60 p-3">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <div className="flex items-center gap-1.5 text-sm font-medium text-foreground">
            {rising ? <ArrowUpRight className="size-4 text-emerald-500" /> : <ArrowDownRight className="size-4 text-rose-500" />}
            <span className="truncate">{label(episode)}</span>
          </div>
          <p className="mt-1 text-[11px] text-muted-foreground">{scopeLabel(episode)} · 阶段 {String(episode.stage ?? "emerging")} · 置信度 {score(episode)}</p>
        </div>
        <span className="rounded-full bg-muted px-2 py-0.5 text-[10px] text-muted-foreground">覆盖 {String(value(episode, "coverageRatio", "coverage_ratio") ?? "-")}</span>
      </div>
      <p className="mt-2 text-xs leading-5 text-muted-foreground">{episode.fullConfirmationRequired ? "当前为优先池快速线索，尚未达到完整板块确认；等待全市场确认线复核。" : "带头股与去带头股表现已按完整行业成分股响应计算，仍需观察市场环境和反证。"}</p>
    </article>
  )
}

export function TransmissionPanel({ data, loading, onSaveNextDay }: { data: MarketIndicatorTransmissionResponse | null; loading: boolean; onSaveNextDay?: () => void }) {
  const episodes = data?.episodes ?? []
  const advice = data?.advice ?? []
  const outcomes = data?.outcomes ?? []
  return (
    <section className="grid min-w-0 gap-4 xl:grid-cols-[minmax(0,1fr)_minmax(320px,0.7fr)]" aria-label="Sector transmission and replay">
      <div className="rounded-lg border border-border/70 bg-card p-4">
        <div className="flex items-center justify-between gap-3">
          <div>
            <h2 className="text-sm font-semibold">板块传导雷达</h2>
            <p className="mt-1 text-xs text-muted-foreground">谁先动、谁跟随、去掉带头股后是否仍成立。</p>
          </div>
          <Sparkles className="size-4 text-information" />
        </div>
        {loading ? <p className="mt-4 text-xs text-muted-foreground">正在刷新传导证据…</p> : episodes.length === 0 ? <p className="mt-4 text-xs text-muted-foreground">当前没有达到传导门槛的板块事件。</p> : <div className="mt-4 grid gap-2">{episodes.slice(0, 8).map((episode, index) => <EpisodeCard key={String(value(episode, "episodeId", "episode_id") ?? index)} episode={episode} />)}</div>}
      </div>
      <div className="rounded-lg border border-border/70 bg-card p-4">
        <div className="flex items-center justify-between gap-2"><div className="flex items-center gap-2"><ShieldAlert className="size-4 text-amber-500" /><h2 className="text-sm font-semibold">辅助参考与复盘</h2></div><Button type="button" variant="outline" size="sm" className="h-7 text-[11px]" onClick={onSaveNextDay} disabled={!advice.length}>保存次日关注</Button></div>
        <div className="mt-3 space-y-2">
          {advice.length === 0 ? <p className="text-xs text-muted-foreground">暂无条件化参考。</p> : advice.slice(0, 6).map((item, index) => <div key={String(value(item, "adviceId", "advice_id") ?? index)} className="rounded-md border border-border/60 px-3 py-2"><div className="flex items-center justify-between gap-2 text-xs"><span className="font-medium text-foreground">{label(item)}</span><span className="text-muted-foreground">{actionLabel(value(item, "actionClass", "action_class"))}</span></div><div className="mt-1 flex items-center gap-1 text-[11px] text-muted-foreground"><Clock3 className="size-3" />置信度 {score(item)}</div></div>)}
        </div>
        {outcomes.length > 0 ? <p className="mt-3 text-[11px] text-muted-foreground">已保存 {outcomes.length} 条结果评价，可在盘后继续回放。</p> : null}
        <p className="mt-3 text-[11px] leading-5 text-muted-foreground">{data?.disclaimer ?? "仅供辅助参考，不构成无条件买卖指令，不执行自动交易。"}</p>
      </div>
    </section>
  )
}
