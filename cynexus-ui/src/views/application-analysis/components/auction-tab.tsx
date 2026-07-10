import { useEffect, useMemo, useState, type ReactNode } from "react"
import { Play, RefreshCw, ShieldAlert, Sparkles, Target } from "lucide-react"

import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import {
  fetchAuctionAiAnalysisSnapshot,
  fetchStockAuctionHistory,
  runAuctionAiAnalysis,
  triggerAuctionAiAnalysisScheduler,
  type AuctionAiAnalysisResponse,
} from "@/services/market/stock-chart"
import { cn } from "@/lib/utils"
import { AuctionPanel } from "../../stock-chart/components/auction-panel"
import type {
  StockAdjust,
  StockAuctionSnapshot,
  StockTargetType,
} from "../../stock-chart/lib/types"
import { fmt, safeRecord, safeString, textList, toNumber } from "../lib/format"
import { notification } from "@/components/ui/notification"

export interface AuctionTabProps {
  targetType: StockTargetType
  symbol: string
  name: string
  adjust?: StockAdjust
}

type AuctionAiDateState = {
  response: AuctionAiAnalysisResponse | null
  loading: boolean
  error: string
  actionMessage: string
}

const SCORE_TONE = {
  strong: "border-red-200 bg-red-50 text-red-700",
  mild: "border-amber-200 bg-amber-50 text-amber-700",
  neutral: "border-slate-200 bg-slate-50 text-slate-700",
  weak: "border-emerald-200 bg-emerald-50 text-emerald-700",
}

function scoreTone(value: unknown) {
  const number = toNumber(value)
  if (number === null) return SCORE_TONE.neutral
  if (number >= 75) return SCORE_TONE.strong
  if (number >= 55) return SCORE_TONE.mild
  if (number <= 35) return SCORE_TONE.weak
  return SCORE_TONE.neutral
}

function biasTone(value: unknown) {
  const key = String(value || "").toLowerCase()
  if (key.includes("bullish")) return "border-red-200 bg-red-50 text-red-700"
  if (key.includes("bearish")) return "border-emerald-200 bg-emerald-50 text-emerald-700"
  return "border-slate-200 bg-slate-50 text-slate-700"
}

function riskTone(value: unknown) {
  const key = String(value || "").toLowerCase()
  if (key.includes("extreme") || key.includes("high")) return "border-red-200 bg-red-50 text-red-700"
  if (key.includes("medium")) return "border-amber-200 bg-amber-50 text-amber-700"
  if (key.includes("low")) return "border-emerald-200 bg-emerald-50 text-emerald-700"
  return "border-slate-200 bg-slate-50 text-slate-700"
}

function MetricPill({ label, value, tone }: { label: string; value: unknown; tone?: string }) {
  return (
    <div className={cn("min-w-0 rounded-xl border px-3 py-2", tone || "border-slate-200 bg-white")}>
      <div className="text-[10px] text-slate-500">{label}</div>
      <div className="mt-0.5 break-words text-sm font-semibold sm:truncate">{fmt(value)}</div>
    </div>
  )
}

function SectionList({ title, items }: { title: string; items: string[] }) {
  if (!items.length) return null
  return (
    <div className="min-w-0 rounded-xl border border-slate-200 bg-slate-50/70 p-3">
      <div className="text-[11px] font-medium text-slate-500">{title}</div>
      <div className="mt-2 grid gap-1.5 text-xs leading-5 text-slate-700">
        {items.slice(0, 8).map((item, index) => (
          <div key={`${title}-${index}`} className="break-words rounded-lg bg-white px-2 py-1 shadow-[0_1px_3px_rgba(15,23,42,0.04)]">
            {item}
          </div>
        ))}
      </div>
    </div>
  )
}

function StyleViewCard({
  title,
  icon,
  data,
  risk,
}: {
  title: string
  icon: ReactNode
  data: Record<string, unknown>
  risk?: boolean
}) {
  const state = fmt(data.state || data.risk_level)
  const score = data.score ?? data.risk_level_score
  const confidence = data.confidence
  const observation = safeString(data.observation) || safeString(data.watch_5min)
  const mainRisks = textList(data.main_risks)
  return (
    <div className="min-w-0 rounded-2xl border border-slate-200 bg-white p-3 shadow-[0_2px_8px_rgba(15,23,42,0.04)]">
      <div className="flex items-center justify-between gap-2">
        <div className="flex items-center gap-2 text-[11px] font-medium text-slate-500">
          {icon}
          {title}
        </div>
        <Badge variant="outline" className={cn("rounded-full px-2 py-0 text-[10px]", risk ? riskTone(data.risk_level) : scoreTone(score))}>
          {fmt(score)}
        </Badge>
      </div>
      <div className="mt-2 break-words text-sm font-semibold text-slate-900">{state}</div>
      <div className="mt-1 text-[11px] text-slate-500">置信 {fmt(confidence)}</div>
      {observation ? <div className="mt-2 break-words text-xs leading-5 text-slate-600">{observation}</div> : null}
      {mainRisks.length ? (
        <div className="mt-2 flex flex-wrap gap-1.5">
          {mainRisks.slice(0, 4).map((item) => (
            <Badge key={item} variant="outline" className="border-red-200 bg-red-50 px-2 py-0 text-[10px] text-red-700">
              {item}
            </Badge>
          ))}
        </div>
      ) : null}
    </div>
  )
}

function AuctionAiPanel({
  response,
  loading,
  error,
  tradeDate,
  embedded = false,
  onRefresh,
  actionMessage,
}: {
  response: AuctionAiAnalysisResponse | null
  loading: boolean
  error: string
  tradeDate?: string
  embedded?: boolean
  onRefresh: () => void
  actionMessage: string
}) {
  const analysis = safeRecord(response?.analysis_result)
  const conclusion = safeRecord(analysis.conclusion)
  const dataQuality = safeRecord(analysis.data_quality)
  const auctionAssessment = safeRecord(analysis.auction_assessment)
  const openingSignal = safeRecord(safeRecord(auctionAssessment.opening_signal))
  const technicalContext = safeRecord(analysis.technical_context)
  const marketAndSector = safeRecord(analysis.market_and_sector)
  const fundamentalRisk = safeRecord(analysis.fundamental_risk)
  const styleViews = safeRecord(analysis.style_views)
  const quantScore = safeRecord(safeRecord(styleViews.quant_score))
  const riskWarning = safeRecord(styleViews.risk_warning)
  const intraday = safeRecord(styleViews.intraday)
  const scenario = safeRecord(analysis.scenario_observation)
  const warnings = textList(dataQuality.warnings)
  const evidence = textList(analysis.key_evidence)
  const limitations = textList(analysis.limitations)

  const hasResult = Object.keys(analysis).length > 0

  return (
    <Card className={cn("min-w-0 border-white/70 bg-white/85 shadow-sm", embedded && "border-0 bg-transparent shadow-none")}>
      {!embedded ? (
      <CardHeader className="pb-3">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <CardTitle className="flex items-center gap-2 text-base">
              <Sparkles className="size-4 text-slate-500" />
              竞价 AI 解读
            </CardTitle>
            <div className="mt-1 text-xs text-slate-500">集合竞价、技术位置、行业市场、财务风险的压缩摘要分析</div>
          </div>
          <Button size="sm" onClick={onRefresh} disabled={loading} className="w-full gap-2 sm:w-auto">
            <RefreshCw className={cn("size-4", loading && "animate-spin")} />
            {loading ? "读取中" : "刷新结果"}
          </Button>
        </div>
      </CardHeader>
      ) : null}
      <CardContent className={cn("space-y-4", embedded && "p-0")}>
        <div className="grid gap-2 rounded-xl border border-slate-200 bg-slate-50/70 p-3 text-[11px] text-slate-600 sm:grid-cols-5">
          <div><span className="font-medium text-slate-900">0-20</span> 极弱/高风险</div>
          <div><span className="font-medium text-slate-900">21-40</span> 偏弱</div>
          <div><span className="font-medium text-slate-900">41-60</span> 中性/分歧</div>
          <div><span className="font-medium text-slate-900">61-80</span> 偏强</div>
          <div><span className="font-medium text-slate-900">81-100</span> 强势</div>
        </div>

        {error ? (
          <div className="rounded-xl border border-amber-200 bg-amber-50 p-3 text-sm text-amber-700">
            {error}
          </div>
        ) : null}

        {actionMessage ? (
          <div className="rounded-xl border border-slate-200 bg-slate-50 p-3 text-sm text-slate-700">
            {actionMessage}
          </div>
        ) : null}

        {!hasResult ? (
          <div className="rounded-xl border border-dashed border-slate-300 bg-slate-50 p-6 text-sm text-slate-500">
            <div>{tradeDate ? `${tradeDate} 暂无持久化竞价 AI 解读。` : "暂无持久化竞价 AI 解读。"}调度器会在工作日 09:26 后生成，盘中读取默认不会再次调用 AI。</div>
          </div>
        ) : (
          <>
            <div className="grid gap-3 lg:grid-cols-[1.4fr_1fr]">
              <div className="rounded-2xl border border-slate-200 bg-slate-50/70 p-4">
                <div className="flex flex-wrap items-center gap-2">
                  <Badge variant="outline" className={cn("rounded-full px-2 py-0", biasTone(conclusion.bias))}>
                    {fmt(conclusion.bias)}
                  </Badge>
                  <Badge variant="outline" className="rounded-full border-slate-200 bg-white px-2 py-0 text-slate-700">
                    {fmt(conclusion.auction_state)}
                  </Badge>
                  <Badge variant="outline" className={cn("rounded-full px-2 py-0", scoreTone(conclusion.confidence))}>
                    置信 {fmt(conclusion.confidence)}
                  </Badge>
                </div>
                <div className="mt-3 break-words text-base font-semibold leading-6 text-slate-900">
                  {safeString(conclusion.summary) || "—"}
                </div>
                <div className="mt-2 break-words text-sm leading-6 text-slate-600">
                  {safeString(conclusion.key_reason) || "—"}
                </div>
              </div>

              <div className="grid grid-cols-2 gap-2">
                <MetricPill label="总分" value={quantScore.total_score} tone={scoreTone(quantScore.total_score)} />
                <MetricPill label="风险惩罚" value={quantScore.risk_penalty} tone={riskTone(riskWarning.risk_level)} />
                <MetricPill label="竞价强度" value={openingSignal.strength_score} tone={scoreTone(openingSignal.strength_score)} />
                <MetricPill label="资金活跃" value={openingSignal.fund_activity_score} tone={scoreTone(openingSignal.fund_activity_score)} />
              </div>
            </div>

            <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
              <StyleViewCard title="超短" icon={<Sparkles className="size-3" />} data={safeRecord(styleViews.ultra_short)} />
              <StyleViewCard title="日内" icon={<Target className="size-3" />} data={intraday} />
              <StyleViewCard title="波段" icon={<Target className="size-3" />} data={safeRecord(styleViews.swing)} />
              <StyleViewCard title="风险" icon={<ShieldAlert className="size-3" />} data={riskWarning} risk />
            </div>

            <div className="grid gap-3 lg:grid-cols-3">
              <MetricPill label="日线位置" value={technicalContext.daily_position} />
              <MetricPill label="量价匹配" value={technicalContext.volume_price_match} />
              <MetricPill label="市场宽度确认" value={marketAndSector.breadth_confirmation} />
              <MetricPill label="行业确认" value={marketAndSector.sector_confirmation} />
              <MetricPill label="题材热度" value={marketAndSector.theme_heat} />
              <MetricPill label="基本面风险" value={fundamentalRisk.risk_level} tone={riskTone(fundamentalRisk.risk_level)} />
            </div>

            <div className="grid gap-3 xl:grid-cols-3">
              {["bullish_case", "base_case", "bearish_case"].map((key) => {
                const item = safeRecord(scenario[key])
                return (
                  <div key={key} className="rounded-xl border border-slate-200 bg-white p-3">
                    <div className="text-[11px] font-medium text-slate-500">
                      {key === "bullish_case" ? "偏强情景" : key === "base_case" ? "基准情景" : "偏弱情景"}
                    </div>
                    <div className="mt-2 break-words text-xs leading-5 text-slate-700">{safeString(item.condition) || "—"}</div>
                    <div className="mt-1 break-words text-xs leading-5 text-slate-500">{safeString(item.meaning) || "—"}</div>
                    <Badge variant="outline" className={cn("mt-2 rounded-full px-2 py-0 text-[10px]", scoreTone(item.confidence))}>
                      置信 {fmt(item.confidence)}
                    </Badge>
                  </div>
                )
              })}
            </div>

            {intraday.watch_15min || intraday.watch_30min ? (
              <div className="rounded-xl border border-slate-200 bg-slate-50/70 p-3">
                <div className="text-[11px] font-medium text-slate-500">日内观察</div>
                <div className="mt-2 grid gap-2 text-xs leading-5 text-slate-700 md:grid-cols-3">
                  <div className="rounded-lg bg-white p-2">5 分钟：{fmt(intraday.watch_5min)}</div>
                  <div className="rounded-lg bg-white p-2">15 分钟：{fmt(intraday.watch_15min)}</div>
                  <div className="rounded-lg bg-white p-2">30 分钟：{fmt(intraday.watch_30min)}</div>
                </div>
              </div>
            ) : null}

            <div className="grid gap-3 xl:grid-cols-3">
              <SectionList title="关键证据" items={evidence} />
              <SectionList title="数据限制" items={limitations} />
              <SectionList title="数据质量提示" items={warnings} />
            </div>
          </>
        )}
      </CardContent>
    </Card>
  )
}

function AuctionAiDateItem({
  date,
  latest,
  state,
  actionLoading,
  onLoad,
  onRefresh,
  onRunNow,
  onTriggerJob,
}: {
  date: string
  latest: boolean
  state: AuctionAiDateState
  actionLoading: boolean
  onLoad: () => void
  onRefresh: () => void
  onRunNow: () => void
  onTriggerJob: () => void
}) {
  const [expanded, setExpanded] = useState(latest)
  const analysis = safeRecord(state.response?.analysis_result)
  const conclusion = safeRecord(analysis.conclusion)
  const hasResult = Object.keys(analysis).length > 0
  const summary = safeString(conclusion.summary)
  const bias = fmt(conclusion.bias)

  const handleToggle = () => {
    const next = !expanded
    setExpanded(next)
    if (next) onLoad()
  }

  return (
    <div className="min-w-0 rounded-xl bg-white/70">
      <button
        type="button"
        onClick={handleToggle}
        className="sticky top-0 z-10 flex w-full items-center rounded-xl bg-white/95 px-3 py-2.5 text-left transition hover:bg-slate-50/90"
      >
        <div className="flex min-w-0 flex-1 flex-wrap items-center gap-x-3 gap-y-1.5">
          <div className="flex min-w-0 flex-wrap items-center gap-x-2 gap-y-1 text-sm font-semibold text-slate-900">
            <span className="font-mono">{date}</span>
            {latest ? <Badge className="rounded-full border-slate-200 bg-slate-50 px-1.5 py-0 text-[10px] text-slate-600" variant="outline">Latest</Badge> : null}
            {state.loading ? <Badge variant="outline" className="rounded-full bg-white px-1.5 py-0 text-[10px] text-slate-500">Loading</Badge> : null}
          </div>
          <div className="flex min-w-0 flex-wrap items-center gap-x-2 gap-y-1 text-[11px] text-slate-500">
            {hasResult ? (
              <>
                <span className="truncate">{summary || "AI analysis persisted"}</span>
                {bias ? (
                  <>
                    <span className="text-slate-300">·</span>
                    <Badge variant="outline" className={cn("rounded-full px-1.5 py-0 text-[10px]", biasTone(conclusion.bias))}>{bias}</Badge>
                  </>
                ) : null}
              </>
            ) : (
              <span className="text-slate-400">{state.error || "No persisted AI interpretation"}</span>
            )}
          </div>
        </div>
      </button>
      {expanded ? (
        <div className="space-y-3 px-3 pb-3 pt-1">
          <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <div className="flex items-center gap-2 text-sm font-semibold text-slate-900">
                <Sparkles className="size-4 text-slate-500" />
                AI Interpretation
              </div>
              <div className="mt-0.5 text-[11px] text-slate-500">Auction, technical position, sector context and risk summary</div>
            </div>
            <div className="grid grid-cols-1 gap-2 sm:flex sm:items-center">
              <Button size="sm" variant="secondary" onClick={onRunNow} disabled={actionLoading} className="h-7 w-full gap-1.5 px-2.5 text-[11px] shadow-none sm:w-auto">
                <Play className="size-3.5" />
                {actionLoading ? "Generating" : "Generate"}
              </Button>
              <Button size="sm" variant="outline" onClick={onTriggerJob} disabled={actionLoading} className="h-7 w-full gap-1.5 bg-white px-2.5 text-[11px] shadow-none sm:w-auto">
                <RefreshCw className={cn("size-3.5", actionLoading && "animate-spin")} />
                Trigger Job
              </Button>
            </div>
          </div>
          <AuctionAiPanel
            response={state.response}
            loading={state.loading}
            error={state.error}
            tradeDate={date}
            embedded
            onRefresh={onRefresh}
            actionMessage={state.actionMessage}
          />
        </div>
      ) : null}
    </div>
  )
}

function AuctionAiHistoryList({
  dates,
  states,
  actionLoading,
  onLoad,
  onRefresh,
  onRunNow,
  onTriggerJob,
}: {
  dates: string[]
  states: Record<string, AuctionAiDateState>
  actionLoading: boolean
  onLoad: (date: string) => void
  onRefresh: (date: string) => void
  onRunNow: (date: string) => void
  onTriggerJob: (date: string) => void
}) {
  const sortedDates = useMemo(
    () => [...new Set(dates.filter(Boolean))].sort((left, right) => right.localeCompare(left)),
    [dates],
  )
  const latestDate = sortedDates[0]

  return (
    <section className="min-w-0 space-y-2 rounded-2xl bg-muted/50 p-3">
      <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div className="text-sm font-semibold text-slate-900">Auction AI Interpretation</div>
          <div className="text-[11px] text-slate-500">Persisted by trading day · newest first · {sortedDates.length} sessions</div>
        </div>
        {latestDate ? <Badge variant="outline" className="w-fit rounded-full bg-white px-2 py-0 text-[10px]">Latest {latestDate}</Badge> : null}
      </div>

      {sortedDates.length === 0 ? (
        <div className="rounded-xl border border-dashed border-slate-200 bg-white px-4 py-6 text-center text-xs text-slate-500">
          No trading-day auction data yet
        </div>
      ) : (
        <div className="space-y-1.5">
          {sortedDates.map((date) => (
            <AuctionAiDateItem
              key={date}
              date={date}
              latest={date === latestDate}
              state={states[date] ?? { response: null, loading: false, error: "", actionMessage: "" }}
              actionLoading={actionLoading}
              onLoad={() => onLoad(date)}
              onRefresh={() => onRefresh(date)}
              onRunNow={() => onRunNow(date)}
              onTriggerJob={() => onTriggerJob(date)}
            />
          ))}
        </div>
      )}
    </section>
  )
}

function AuctionDateItem({
  auction,
  defaultExpanded,
}: {
  auction: StockAuctionSnapshot
  defaultExpanded: boolean
}) {
  const [expanded, setExpanded] = useState(defaultExpanded)
  const openingCount = auction.details?.openingPoints?.length ?? 0
  const closingCount = auction.details?.closingPoints?.length ?? 0
  const totalCount = auction.details?.allPoints?.length ?? 0
  const hasData = totalCount > 0

  return (
    <div className="min-w-0 rounded-xl bg-white/70">
      <button
        type="button"
        onClick={() => setExpanded((value) => !value)}
        className="sticky top-0 z-10 flex w-full items-center rounded-xl bg-white/95 px-3 py-2.5 text-left transition hover:bg-slate-50/90"
      >
        <div className="flex min-w-0 flex-1 flex-wrap items-center gap-x-3 gap-y-1.5">
          <div className="flex min-w-0 flex-wrap items-center gap-x-2 gap-y-1 text-sm font-semibold text-slate-900">
            <span className="font-mono">{auction.trade_date || "--"}</span>
            {defaultExpanded ? (
              <Badge className="rounded-full border-slate-200 bg-slate-50 px-1.5 py-0 text-[10px] text-slate-600" variant="outline">Latest</Badge>
            ) : null}
            <span className="text-[11px] font-normal text-slate-300">·</span>
            <Badge variant={auction.details?.auction0925?.has_auction_0925 ? "secondary" : "outline"} className="rounded-full px-1.5 py-0 text-[10px]">
              09:25 {auction.details?.auction0925?.has_auction_0925 ? "Hit" : "Missing"}
            </Badge>
            <Badge variant={auction.closing?.anchorExact ? "secondary" : "outline"} className="rounded-full px-1.5 py-0 text-[10px]">
              15:00 {auction.closing?.anchorExact ? "Hit" : "Approx"}
            </Badge>
          </div>
          <div className="flex min-w-0 flex-wrap items-center gap-x-2 gap-y-1 text-[11px] text-slate-500">
            {hasData ? (
              <>
                <span>Opening {openingCount}</span>
                <span className="text-slate-300">·</span>
                <span>Closing {closingCount}</span>
                <span className="text-slate-300">·</span>
                <span>Total {totalCount}</span>
              </>
            ) : (
              <span className="text-slate-400">{auction.error || "No auction detail for this trading day"}</span>
            )}
          </div>
        </div>
      </button>
      {expanded ? (
        <div className="pb-3 pt-1">
          <AuctionPanel auction={auction} hideHeader />
        </div>
      ) : null}
    </div>
  )
}

function AuctionHistoryList({
  items,
  loading,
  error,
}: {
  items: StockAuctionSnapshot[]
  loading: boolean
  error: string
}) {
  const sortedItems = useMemo(
    () => [...items].sort((left, right) => (right.trade_date || "").localeCompare(left.trade_date || "")),
    [items],
  )
  const latestDate = sortedItems[0]?.trade_date

  return (
    <section className="min-w-0 space-y-2 rounded-2xl bg-muted/50 p-3">
      <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div className="text-sm font-semibold text-slate-900">Auction Detail</div>
          <div className="text-[11px] text-slate-500">Persisted by trading day · newest first · {sortedItems.length} sessions</div>
        </div>
        {latestDate ? <Badge variant="outline" className="w-fit rounded-full bg-white px-2 py-0 text-[10px]">Latest {latestDate}</Badge> : null}
      </div>

      {error ? (
        <div className="rounded-xl border border-dashed border-red-300 bg-red-50/70 p-6 text-sm text-red-600">
          加载集合竞价失败：{error}
        </div>
      ) : sortedItems.length === 0 ? (
        <div className="rounded-xl border border-dashed border-slate-200 bg-white px-4 py-6 text-center text-xs text-slate-500">
          {loading ? "Loading..." : "No auction detail yet"}
        </div>
      ) : (
        <div className="space-y-1.5">
          {sortedItems.map((item) => (
            <AuctionDateItem
              key={`${item.symbol}-${item.trade_date || item.source}`}
              auction={item}
              defaultExpanded={item.trade_date === latestDate}
            />
          ))}
        </div>
      )}
    </section>
  )
}

export function AuctionTab({
  targetType,
  symbol,
  name,
  adjust = "qfq",
}: AuctionTabProps) {
  const [auctionItems, setAuctionItems] = useState<StockAuctionSnapshot[]>([])
  const [auctionLoading, setAuctionLoading] = useState(false)
  const [error, setError] = useState<string>("")
  const [aiByDate, setAiByDate] = useState<Record<string, AuctionAiDateState>>({})
  const [actionLoading, setActionLoading] = useState(false)

  useEffect(() => {
    let active = true

    void (async () => {
      try {
        if (!active) return
        setAuctionLoading(true)
        setError("")
        const auctionResult = await fetchStockAuctionHistory(symbol, 30)
        if (!active) return
        setAuctionItems(auctionResult.items ?? [])
      } catch (err) {
        if (!active) return
        const msg = err instanceof Error ? err.message : "加载集合竞价失败"
        setAuctionItems([])
        setError(msg)
        notification.danger({ title: "加载集合竞价失败", description: msg })
      } finally {
        if (active) setAuctionLoading(false)
      }
    })()

    return () => {
      active = false
    }
  }, [symbol, name])

  useEffect(() => {
    setAiByDate({})
  }, [targetType, symbol, name, adjust])

  const canRunAi = useMemo(() => Boolean(symbol && name), [symbol, name])
  const auctionDates = useMemo(
    () => auctionItems.map((item) => item.trade_date).filter(Boolean),
    [auctionItems],
  )
  const latestAuctionDate = auctionDates[0]

  const updateAiDateState = (date: string, patch: Partial<AuctionAiDateState>) => {
    setAiByDate((previous) => ({
      ...previous,
      [date]: {
        response: previous[date]?.response ?? null,
        loading: previous[date]?.loading ?? false,
        error: previous[date]?.error ?? "",
        actionMessage: previous[date]?.actionMessage ?? "",
        ...patch,
      },
    }))
  }

  const handleRefreshAiSnapshot = async (date: string) => {
    if (!canRunAi) return
    try {
      updateAiDateState(date, { loading: true, error: "" })
      const response = await fetchAuctionAiAnalysisSnapshot({
        targetType,
        symbol,
        date,
      })
      updateAiDateState(date, { response, loading: false, error: "" })
    } catch (err) {
      const msg = err instanceof Error ? err.message : "读取竞价 AI 分析结果失败"
      updateAiDateState(date, { response: null, loading: false, error: msg })
      notification.danger({ title: "读取竞价 AI 分析结果失败", description: msg })
    }
  }

  const handleRunNow = async (date: string) => {
    if (!canRunAi) return
    try {
      setActionLoading(true)
      updateAiDateState(date, { actionMessage: "", error: "" })
      const response = await runAuctionAiAnalysis({
        targetType,
        symbol,
        name,
        adjust,
        date,
        maxChars: 1000000,
      })
      updateAiDateState(date, { response, actionMessage: "已立即生成当前标的的竞价 AI 分析，并写入持久化结果。", error: "" })
      notification.success({
        title: "竞价 AI 分析已生成",
        description: `${name} · ${symbol} · ${date}`,
      })
    } catch (err) {
      const msg = err instanceof Error ? err.message : "立即生成竞价 AI 分析失败"
      updateAiDateState(date, { error: msg })
      notification.danger({ title: "立即生成竞价 AI 分析失败", description: msg })
    } finally {
      setActionLoading(false)
    }
  }

  const handleTriggerJob = async (date: string) => {
    try {
      setActionLoading(true)
      updateAiDateState(date, { actionMessage: "", error: "" })
      const result = await triggerAuctionAiAnalysisScheduler()
      if (result.error) {
        throw new Error(result.error)
      }
      updateAiDateState(date, { actionMessage: `已触发批量任务：成功 ${result.succeeded ?? 0}，失败 ${result.failed ?? 0}。正在读取当前标的结果。` })
      notification.info({
        title: "已触发批量任务",
        description: `成功 ${result.succeeded ?? 0}，失败 ${result.failed ?? 0}`,
      })
      await handleRefreshAiSnapshot(date)
    } catch (err) {
      const msg = err instanceof Error ? err.message : "触发今日批量任务失败"
      updateAiDateState(date, { error: msg })
      notification.danger({ title: "触发今日批量任务失败", description: msg })
    } finally {
      setActionLoading(false)
    }
  }

  useEffect(() => {
    if (latestAuctionDate) void handleRefreshAiSnapshot(latestAuctionDate)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [targetType, symbol, latestAuctionDate])

  return (
    <div className="min-w-0 space-y-4">
      <AuctionAiHistoryList
        dates={auctionDates}
        states={aiByDate}
        actionLoading={actionLoading}
        onLoad={(date) => void handleRefreshAiSnapshot(date)}
        onRefresh={(date) => void handleRefreshAiSnapshot(date)}
        onRunNow={(date) => void handleRunNow(date)}
        onTriggerJob={(date) => void handleTriggerJob(date)}
      />

      <AuctionHistoryList items={auctionItems} loading={auctionLoading} error={error} />
    </div>
  )
}
