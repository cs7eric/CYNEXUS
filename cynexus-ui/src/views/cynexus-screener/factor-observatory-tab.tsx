import { useCallback, useState } from "react"
import { AlertTriangle, BarChart3, FlaskConical, Loader2, Play, Sparkles } from "lucide-react"

import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { AiBindingProviderSelect } from "@/components/ai/ai-binding-provider-select"
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import {
  analyzeMarketEventFactors,
  fetchMarketEventFactorObservatory,
  type MarketEventFactorAnalysisResponse,
  type MarketEventCohort,
  type MarketEventFactorEvent,
  type MarketEventFactorFamily,
  type MarketEventFactorObservatoryResponse,
} from "@/services/market/screener"

function FactorFamilyView({ family }: { family: MarketEventFactorFamily }) {
  return (
    <section className="border-b border-border/70 py-3 last:border-b-0">
      <div className="flex items-start justify-between gap-2">
        <div className="min-w-0">
          <div className="text-xs font-medium">{family.label}</div>
          <div className="mt-0.5 truncate font-mono text-[10px] text-muted-foreground">
            {family.asOfDate || "日期不可用"} · {family.source || "来源不可用"}
          </div>
        </div>
        <Badge variant={family.status === "AVAILABLE" ? "secondary" : "outline"} className="shrink-0 text-[10px]">
          {family.status === "AVAILABLE" ? "可用" : family.status === "PARTIAL" ? "部分覆盖" : "不可用"}
        </Badge>
      </div>
      {family.metrics.length > 0 ? (
        <div className="mt-2 grid grid-cols-2 gap-x-3 gap-y-1.5 text-[11px]">
          {family.metrics.map((metric) => (
            <div key={metric.key} className="flex min-w-0 items-baseline justify-between gap-1">
              <span className="truncate text-muted-foreground">{metric.label}</span>
              <span className="shrink-0 font-mono text-foreground">{metric.value ?? "-"}{metric.unit ?? ""}</span>
            </div>
          ))}
        </div>
      ) : null}
      {family.warnings.map((warning) => (
        <div key={warning} className="mt-2 flex gap-1 text-[10px] leading-4 text-warning"><AlertTriangle className="mt-0.5 size-3 shrink-0" />{warning}</div>
      ))}
    </section>
  )
}

export function FactorObservatoryTab({
  tradeDate,
  onTradeDateChange,
  onApplyHints,
  onSelectEvent,
}: {
  tradeDate: string
  onTradeDateChange: (value: string) => void
  onApplyHints: (hints: MarketEventFactorObservatoryResponse["screeningHints"]) => void
  onSelectEvent: (event: MarketEventFactorEvent, date: string) => void
}) {
  const [cohort, setCohort] = useState<MarketEventCohort>("LIMIT_UP")
  const [minLargeMovePct, setMinLargeMovePct] = useState("7")
  const [result, setResult] = useState<MarketEventFactorObservatoryResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [analysis, setAnalysis] = useState<MarketEventFactorAnalysisResponse | null>(null)
  const [analysisLoading, setAnalysisLoading] = useState(false)
  const [analysisError, setAnalysisError] = useState<string | null>(null)
  const run = useCallback(async () => {
    setLoading(true)
    setError(null)
    setAnalysis(null)
    setAnalysisError(null)
    try {
      setResult(await fetchMarketEventFactorObservatory({
        tradeDate: tradeDate || undefined,
        cohort,
        minLargeMovePct: cohort === "LARGE_MOVE" && minLargeMovePct ? Number(minLargeMovePct) : undefined,
        limit: 40,
      }))
    } catch (caught) {
      setResult(null)
      setError(caught instanceof Error ? caught.message : "无法读取事件因子观察结果。")
    } finally {
      setLoading(false)
    }
  }, [cohort, minLargeMovePct, tradeDate])

  const runAnalysis = useCallback(async () => {
    if (!result) return
    setAnalysisLoading(true)
    setAnalysisError(null)
    try {
      setAnalysis(await analyzeMarketEventFactors({
        tradeDate: result.tradeDate || tradeDate || undefined,
        cohort,
        minLargeMovePct: cohort === "LARGE_MOVE" && minLargeMovePct ? Number(minLargeMovePct) : undefined,
        maxEvents: Math.min(result.sampledCount || 12, 12),
      }))
    } catch (caught) {
      setAnalysis(null)
      setAnalysisError(caught instanceof Error ? caught.message : "AI 因子综合失败。")
    } finally {
      setAnalysisLoading(false)
    }
  }, [cohort, minLargeMovePct, result, tradeDate])

  return (
    <div className="flex min-h-0 flex-1 flex-col">
      <div className="grid shrink-0 grid-cols-[1fr_1fr_auto] gap-2 border-b border-border/70 p-3">
        <Input type="date" value={tradeDate} className="h-8 text-xs" onChange={(event) => onTradeDateChange(event.target.value)} />
        <Select value={cohort} onValueChange={(value) => setCohort(value as MarketEventCohort)}>
          <SelectTrigger size="sm" className="min-w-0"><SelectValue /></SelectTrigger>
          <SelectContent>
            <SelectItem value="LIMIT_UP">涨停池</SelectItem>
            <SelectItem value="LARGE_MOVE">大涨池</SelectItem>
          </SelectContent>
        </Select>
        <Button size="icon-sm" aria-label="执行事件观察" title="执行事件观察" onClick={() => void run()} disabled={loading}>
          {loading ? <Loader2 className="animate-spin" /> : <Play />}
        </Button>
        {cohort === "LARGE_MOVE" ? <Input className="col-span-2 h-8 text-xs" type="number" min="0" max="30" step="0.5" value={minLargeMovePct} onChange={(event) => setMinLargeMovePct(event.target.value)} placeholder="大涨阈值（%）" /> : null}
      </div>
      <div className="min-h-0 flex-1 overflow-y-auto">
        {error ? <div className="p-4 text-xs text-destructive">{error}</div> : null}
        {!result && !loading && !error ? <div className="p-5 text-center text-xs leading-5 text-muted-foreground"><FlaskConical className="mx-auto mb-2 size-4" />按日期观察涨停或大涨样本的多源因子。结果保留覆盖范围与不可用项，不输出单一归因。</div> : null}
        {result ? <>
          <div className="border-b border-border/70 p-3">
            <div className="flex items-center justify-between gap-2">
              <div className="text-xs font-medium">市场与样本上下文</div>
              <Badge variant="secondary" className="text-[10px]">{result.sampledCount}/{result.populationCount} 样本</Badge>
            </div>
            <div className="mt-1 text-[10px] text-muted-foreground">实际日期 {result.tradeDate || "-"} · 仅为观察证据，不构成因果或交易结论</div>
            {result.marketFactors.map((family) => <FactorFamilyView key={family.code} family={family} />)}
          </div>
          <section className="border-b border-border/70 p-3">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div className="min-w-0">
                <div className="text-xs font-medium">AI 因子综合</div>
                <div className="mt-0.5 text-[10px] text-muted-foreground">仅组织已加载的证据引用，输出研究假设，不生成因果或交易结论。</div>
              </div>
              <div className="flex items-center gap-1">
                <AiBindingProviderSelect capability="market_event_factor_analysis" className="max-w-[13rem]" />
                <Button size="sm" variant="secondary" className="shrink-0 text-xs" onClick={() => void runAnalysis()} disabled={analysisLoading}>
                  {analysisLoading ? <Loader2 className="animate-spin" /> : <Sparkles />}综合
                </Button>
              </div>
            </div>
            {analysisError ? <div className="mt-2 text-[11px] leading-4 text-destructive">{analysisError}</div> : null}
            {analysis ? <div className="mt-3 divide-y divide-border/70 border-y border-border/70">
              <div className="py-2.5 text-[11px] leading-5 text-foreground">{analysis.analysis_result.summary}</div>
              {analysis.analysis_result.hypotheses.map((hypothesis, index) => <div key={`${hypothesis.label}-${index}`} className="py-2.5">
                <div className="flex items-baseline justify-between gap-2"><span className="text-xs font-medium">{hypothesis.label}</span><span className="shrink-0 font-mono text-[10px] text-muted-foreground">{hypothesis.scope}{hypothesis.event_code ? ` · ${hypothesis.event_code}` : ""}</span></div>
                <div className="mt-1.5 space-y-1 text-[10px] leading-4 text-muted-foreground">
                  {hypothesis.evidence_refs.map((ref) => <div key={ref}>依据：{analysis.evidence_refs[ref] || ref}</div>)}
                  {hypothesis.counter_evidence_refs.map((ref) => <div key={ref}>反证：{analysis.evidence_refs[ref] || ref}</div>)}
                  {hypothesis.research_conditions.map((condition) => <div key={condition}>观察：{condition}</div>)}
                </div>
              </div>)}
              {analysis.analysis_result.caveats.map((caveat) => <div key={caveat} className="py-2 text-[10px] leading-4 text-warning">{caveat}</div>)}
              <div className="py-2 font-mono text-[10px] text-muted-foreground">{analysis.provider_code || "-"} · {analysis.model_code || "-"} · {analysis.prompt_version || "-"} · {analysis.data_quality}</div>
            </div> : null}
          </section>
          {result.screeningHints.length > 0 ? <div className="flex items-center justify-between gap-2 border-b border-border/70 p-3">
            <div className="min-w-0 text-[11px] text-muted-foreground">{result.screeningHintsDeprecated ? "旧版兼容预设，不是研究发现；正式选股仅使用已验证的 Factor Set。" : "可转为现有可编辑指标条件，不生成 SQL 或黑箱规则。"}</div>
            <Button size="sm" variant="secondary" className="shrink-0 text-xs" onClick={() => onApplyHints(result.screeningHints)}>{result.screeningHintsDeprecated ? "应用兼容预设" : "应用条件"}</Button>
          </div> : null}
          <div className="p-3">
            <div className="mb-2 flex items-center gap-1.5 text-xs font-medium"><BarChart3 className="size-3.5" />事件样本</div>
            <div className="divide-y divide-border/70 border-y border-border/70">
              {result.events.map((event) => <button key={event.code} type="button" onClick={() => onSelectEvent(event, result.tradeDate || tradeDate)} className="block w-full px-1 py-2.5 text-left hover:bg-muted/50">
                <div className="flex items-baseline justify-between gap-2"><span className="truncate text-xs font-medium">{event.name || event.code}</span><span className="font-mono text-xs text-positive">{event.changePct == null ? "-" : `${event.changePct}%`}</span></div>
                <div className="mt-0.5 truncate text-[10px] text-muted-foreground">{event.code} · {event.thsIndustryName || event.tdxIndustryName || "行业未映射"}{event.streak ? ` · ${event.streak} 连板` : ""}</div>
              </button>)}</div>
          </div>
          {result.warnings.length > 0 ? <div className="border-t border-border/70 p-3 text-[10px] leading-4 text-warning">{result.warnings.map((warning) => <div key={warning}>{warning}</div>)}</div> : null}
        </> : null}
      </div>
    </div>
  )
}
