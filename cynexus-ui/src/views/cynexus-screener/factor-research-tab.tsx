import { useCallback, useEffect, useMemo, useState } from "react"
import { useSearchParams } from "react-router-dom"
import { AlertTriangle, CheckCircle2, Eye, FlaskConical, Loader2, Play, Plus, RefreshCw, Save, XCircle } from "lucide-react"

import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Textarea } from "@/components/ui/textarea"
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from "@/components/ui/sheet"
import { AiBindingProviderSelect } from "@/components/ai/ai-binding-provider-select"
import {
  createMarketFactorSet,
  createMarketFactorStudy,
  cancelMarketFactorStudy,
  fetchMarketFactorSets,
  fetchMarketFactorDiscoveries,
  fetchMarketFactorDiscovery,
  fetchMarketFactorClusters,
  synthesizeMarketFactorStudy,
  fetchMarketFactorOutcomes,
  fetchMarketFactorStudySamples,
  fetchMarketFactorStudySequences,
  fetchMarketFactorStudies,
  promoteMarketFactorSet,
  previewMarketFactorSet,
  updateMarketFactorSet,
  validateMarketFactorSet,
  type MarketFactorSet,
  type MarketFactorSetPreview,
  type MarketFactorDiscovery,
  type MarketFactorStudyResponse,
  type MarketFactorStudyOutcome,
  type MarketFactorStudySample,
  type MarketFactorStudySequencePoint,
  type MarketFactorCluster,
  type MarketFactorStudySynthesisResponse,
} from "@/services/market/screener"

  const statusLabel: Record<string, string> = {
  QUEUED: "Queued", RUNNING: "Running", SUCCEEDED: "Success", PARTIAL: "Partial",
  NO_DATA: "No data", FAILED: "Failed", CANCELLED: "Cancelled",
}

export function FactorResearchTab() {
  type WorkspaceView = "events" | "studies" | "discoveries" | "sequences" | "validation" | "factor-set" | "quality"
  const [searchParams, setSearchParams] = useSearchParams()
  const requestedWorkspaceView = searchParams.get("researchView")
  const [workspaceView, setWorkspaceView] = useState<WorkspaceView>(requestedWorkspaceView === "events" || requestedWorkspaceView === "discoveries" || requestedWorkspaceView === "sequences" || requestedWorkspaceView === "validation" || requestedWorkspaceView === "factor-set" || requestedWorkspaceView === "quality" ? requestedWorkspaceView : "studies")
  const [eventType, setEventType] = useState<"LIMIT_UP_CLOSE" | "LIMIT_UP_TOUCH" | "LARGE_MOVE_CLOSE">("LIMIT_UP_CLOSE")
  const [startDate, setStartDate] = useState("")
  const [endDate, setEndDate] = useState("")
  const [studies, setStudies] = useState<MarketFactorStudyResponse[]>([])
  const [studyOffset, setStudyOffset] = useState(0)
  const [selected, setSelected] = useState<MarketFactorStudyResponse | null>(null)
  const [discoveries, setDiscoveries] = useState<MarketFactorDiscovery[]>([])
  const [clusters, setClusters] = useState<MarketFactorCluster[]>([])
  const [discoveryScope, setDiscoveryScope] = useState<"ALL" | MarketFactorDiscovery["scope"]>("ALL")
  const [outcomes, setOutcomes] = useState<MarketFactorStudyOutcome[]>([])
  const [samples, setSamples] = useState<MarketFactorStudySample[]>([])
  const [sequences, setSequences] = useState<MarketFactorStudySequencePoint[]>([])
  const [selectedDiscovery, setSelectedDiscovery] = useState<MarketFactorDiscovery | null>(null)
  const [discoveryLoading, setDiscoveryLoading] = useState(false)
  const [factorSets, setFactorSets] = useState<MarketFactorSet[]>([])
  const [selectedFactorSetCode, setSelectedFactorSetCode] = useState<string>("")
  const [factorSetCode, setFactorSetCode] = useState("")
  const [factorSetName, setFactorSetName] = useState("")
  const [expressionOperator, setExpressionOperator] = useState<"AND" | "OR">("AND")
  const [expressionField, setExpressionField] = useState("discoveryCode")
  const [expressionValue, setExpressionValue] = useState("")
  const [expressionWeight, setExpressionWeight] = useState("1")
  const [factorSetSaving, setFactorSetSaving] = useState(false)
  const [factorSetPreview, setFactorSetPreview] = useState<MarketFactorSetPreview | null>(null)
  const [studySynthesis, setStudySynthesis] = useState<MarketFactorStudySynthesisResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true); setError(null)
    try {
      const next = await fetchMarketFactorStudies(studyOffset, 50)
      setStudies(next)
      const sets = await fetchMarketFactorSets()
      setFactorSets(sets)
      const requestedStudyId = searchParams.get("studyId")
      setSelected((current) => next.find((item) => item.studyId === requestedStudyId) ?? next.find((item) => item.studyId === current?.studyId) ?? next[0] ?? null)
    } catch (caught) { setError(caught instanceof Error ? caught.message : "Unable to load research studies.") }
    finally { setLoading(false) }
  }, [searchParams, studyOffset])
  useEffect(() => { void Promise.resolve().then(load) }, [load])
  useEffect(() => {
    const next = new URLSearchParams(searchParams)
    if (next.get("researchView") !== workspaceView) { next.set("researchView", workspaceView); setSearchParams(next, { replace: true }) }
  }, [searchParams, setSearchParams, workspaceView])
  useEffect(() => {
    if (!selected) return
    let active = true
    void Promise.all([fetchMarketFactorDiscoveries(selected.studyId), fetchMarketFactorOutcomes(selected.studyId, 0, 100), fetchMarketFactorStudySamples(selected.studyId), fetchMarketFactorStudySequences(selected.studyId), fetchMarketFactorClusters(selected.studyId)])
      .then(([nextDiscoveries, nextOutcomes, nextSamples, nextSequences, nextClusters]) => { if (active) { setDiscoveries(nextDiscoveries); setOutcomes(nextOutcomes); setSamples(nextSamples); setSequences(nextSequences); setClusters(nextClusters) } })
      .catch((caught) => { if (active) setError(caught instanceof Error ? caught.message : "Unable to load Study evidence.") })
    return () => { active = false }
  }, [selected])
  useEffect(() => {
    if (!selected || !["QUEUED", "RUNNING"].includes(selected.status)) return
    const id = window.setInterval(() => void load(), 5000)
    return () => window.clearInterval(id)
  }, [load, selected])

  const openDiscovery = useCallback(async (item: MarketFactorDiscovery) => {
    if (!selected) return
    setDiscoveryLoading(true); setError(null)
    try { setSelectedDiscovery(await fetchMarketFactorDiscovery(selected.studyId, item.discoveryCode)) }
    catch (caught) { setError(caught instanceof Error ? caught.message : "Unable to load discovery detail.") }
    finally { setDiscoveryLoading(false) }
  }, [selected])

  const selectedFactorSet = useMemo(
    () => factorSets.find((item) => item.code === selectedFactorSetCode) ?? null,
    [factorSets, selectedFactorSetCode],
  )
  const latestVersion = selectedFactorSet?.activeVersion ?? null
  const sequenceGroups = useMemo(() => {
    const groups = new Map<string, MarketFactorStudySequencePoint[]>()
    for (const point of sequences) {
      const key = `${point.sequenceCode} · ${point.sampleType}`
      groups.set(key, [...(groups.get(key) ?? []), point])
    }
    return [...groups.entries()]
  }, [sequences])
  const horizonValidation = useMemo(() => {
    const buckets = new Map<number, { total: number; available: number; excessTotal: number }>()
    for (const sample of outcomes) {
      if (sample.sampleType !== "EVENT") continue
      for (const outcome of Array.isArray(sample.outcomes) ? sample.outcomes : []) {
        const bucket = buckets.get(outcome.horizon) ?? { total: 0, available: 0, excessTotal: 0 }
        bucket.total += 1
        if (outcome.status === "AVAILABLE" && typeof outcome.excessReturnPct === "number") {
          bucket.available += 1
          bucket.excessTotal += outcome.excessReturnPct
        }
        buckets.set(outcome.horizon, bucket)
      }
    }
    return [...buckets.entries()].sort(([left], [right]) => left - right).map(([horizon, bucket]) => ({
      horizon,
      coveragePct: bucket.total === 0 ? 0 : bucket.available / bucket.total * 100,
      averageExcessPct: bucket.available === 0 ? null : bucket.excessTotal / bucket.available,
    }))
  }, [outcomes])
  const validationRows = useMemo(() => discoveries.map((item) => ({
    discoveryCode: item.discoveryCode,
    train: String(item.validation?.trainStatus ?? item.validation?.chronological ?? "UNAVAILABLE"),
    regime: String(item.validation?.regime ?? item.validation?.regimeStatus ?? "UNAVAILABLE"),
    monthly: String(item.validation?.monthly ?? item.validation?.monthlyStatus ?? "UNAVAILABLE"),
    outcome: String(item.validation?.outcome ?? item.validation?.outcomeStatus ?? "UNAVAILABLE"),
  })), [discoveries])

  const hydrateFactorSet = useCallback((factorSet: MarketFactorSet | null) => {
    setSelectedFactorSetCode(factorSet?.code ?? "")
    setFactorSetCode(factorSet?.code ?? "")
    setFactorSetName(factorSet?.name ?? "")
    const expression = factorSet?.activeVersion?.expression
    const clause = Array.isArray(expression?.clauses) ? expression.clauses[0] as Record<string, unknown> | undefined : undefined
    setExpressionOperator(expression?.operator === "OR" ? "OR" : "AND")
    setExpressionField(typeof clause?.field === "string" ? clause.field : "discoveryCode")
    setExpressionValue(typeof clause?.value === "string" ? clause.value : "")
    setExpressionWeight(clause?.weight == null ? "1" : String(clause.weight))
  }, [])

  const startFactorSet = useCallback(() => {
    hydrateFactorSet(null)
    setExpressionValue(discoveries[0]?.discoveryCode ?? "")
  }, [discoveries, hydrateFactorSet])

  const saveFactorSet = useCallback(async () => {
    const code = factorSetCode.trim().toUpperCase()
    const name = factorSetName.trim()
    const value = expressionValue.trim()
    const weight = Number(expressionWeight)
    if (!code || !name || !value || !Number.isFinite(weight) || weight <= 0) {
      setError("Factor Set code, name, condition value and a positive weight are required.")
      return
    }
    const expression = {
      operator: expressionOperator,
      clauses: [{ field: expressionField, operator: "EQUALS", value, weight }],
      schemaVersion: 1,
    }
    setFactorSetSaving(true); setError(null)
    try {
      const sourceStudyId = selected?.studyId
      const saved = selectedFactorSet
        ? await updateMarketFactorSet(selectedFactorSet.code, { name, expression, sourceStudyId, expectedVersion: selectedFactorSet.version })
        : await createMarketFactorSet({ code, name, expression, sourceStudyId })
      await load()
      hydrateFactorSet(saved)
    } catch (caught) { setError(caught instanceof Error ? caught.message : "Unable to save the Factor Set version.") }
    finally { setFactorSetSaving(false) }
  }, [expressionField, expressionOperator, expressionValue, expressionWeight, factorSetCode, factorSetName, hydrateFactorSet, load, selected, selectedFactorSet])

  const validateFactorSet = useCallback(async () => {
    if (!selectedFactorSet || !latestVersion) return
    setFactorSetSaving(true); setError(null)
    try {
      await validateMarketFactorSet(selectedFactorSet.code, latestVersion.versionNo, {
        sourceStudyId: selected?.studyId ?? null,
        discoveryCode: expressionValue.trim() || null,
        reviewedAt: new Date().toISOString(),
      })
      await load()
    } catch (caught) { setError(caught instanceof Error ? caught.message : "Unable to validate this Factor Set version.") }
    finally { setFactorSetSaving(false) }
  }, [expressionValue, latestVersion, load, selected, selectedFactorSet])

  const promoteFactorSet = useCallback(async () => {
    if (!selectedFactorSet || !latestVersion) return
    setFactorSetSaving(true); setError(null)
    try {
      await promoteMarketFactorSet(selectedFactorSet.code, latestVersion.versionNo)
      await load()
    } catch (caught) { setError(caught instanceof Error ? caught.message : "Only a validated Factor Set version can be promoted.") }
    finally { setFactorSetSaving(false) }
  }, [latestVersion, load, selectedFactorSet])

  const previewFactorSet = useCallback(async () => {
    if (!selectedFactorSet || !latestVersion) return
    setFactorSetSaving(true); setError(null)
    try { setFactorSetPreview(await previewMarketFactorSet(selectedFactorSet.code, latestVersion.versionNo, endDate || undefined)) }
    catch (caught) { setError(caught instanceof Error ? caught.message : "Unable to preview this Factor Set.") }
    finally { setFactorSetSaving(false) }
  }, [endDate, latestVersion, selectedFactorSet])

  const create = useCallback(async () => {
    if (!startDate || !endDate) { setError("Choose a complete historical research window."); return }
    setLoading(true); setError(null)
    try {
      const idempotencyKey = `market-factor-study:${eventType}:${startDate}:${endDate}:${crypto.randomUUID()}`
      const study = await createMarketFactorStudy({ eventType, startDate, endDate, controlsPerEvent: 2, dailyPreWindowSessions: 60, minutePreWindowMinutes: 120, idempotencyKey })
      setStudies((current) => [study, ...current]); setSelected(study)
      const next = new URLSearchParams(searchParams); next.set("studyId", study.studyId); setSearchParams(next, { replace: true })
    } catch (caught) { setError(caught instanceof Error ? caught.message : "Unable to queue research study.") }
    finally { setLoading(false) }
  }, [endDate, eventType, searchParams, setSearchParams, startDate])

  const cancelSelectedStudy = useCallback(async () => {
    if (!selected) return
    setLoading(true); setError(null)
    try { await cancelMarketFactorStudy(selected.studyId); await load() }
    catch (caught) { setError(caught instanceof Error ? caught.message : "Unable to cancel the study.") }
    finally { setLoading(false) }
  }, [load, selected])
  const synthesizeStudy = useCallback(async () => {
    if (!selected) return
    setLoading(true); setError(null)
    try { setStudySynthesis(await synthesizeMarketFactorStudy(selected.studyId)) }
    catch (caught) { setError(caught instanceof Error ? caught.message : "Unable to synthesize the persisted Study evidence.") }
    finally { setLoading(false) }
  }, [selected])

  return <div className="flex min-h-0 flex-1 flex-col">
    <div className="space-y-2 border-b border-border/70 p-3">
      <div className="grid grid-cols-2 gap-2">
        <Input type="date" aria-label="Research start date" value={startDate} onChange={(event) => setStartDate(event.target.value)} className="h-8 text-xs" />
        <Input type="date" aria-label="Research end date" value={endDate} onChange={(event) => setEndDate(event.target.value)} className="h-8 text-xs" />
      </div>
      <div className="flex gap-2"><Select value={eventType} onValueChange={(value) => setEventType(value as typeof eventType)}><SelectTrigger size="sm" className="min-w-0 flex-1"><SelectValue /></SelectTrigger><SelectContent><SelectItem value="LIMIT_UP_CLOSE">Limit-up close</SelectItem><SelectItem value="LIMIT_UP_TOUCH">Limit-up touch</SelectItem><SelectItem value="LARGE_MOVE_CLOSE">Large move close</SelectItem></SelectContent></Select><Button size="sm" onClick={() => void create()} disabled={loading}>{loading ? <Loader2 className="animate-spin" /> : <Play />}Run study</Button></div>
      <p className="text-[10px] leading-4 text-muted-foreground">Studies freeze an as-of data cutoff, matched controls and outcome windows. Promotion remains an explicit reviewed action.</p>
    </div>
    {error ? <div className="flex gap-1 p-3 text-[11px] text-destructive"><AlertTriangle className="size-3.5 shrink-0" />{error}</div> : null}
    {selected?.status === "RUNNING" && selected.progress ? <div className="border-b border-border/70 bg-muted/20 px-3 py-2 text-[10px] text-muted-foreground"><div className="flex items-center justify-between gap-2"><span>Study stage: {selected.progress.stage ?? "RUNNING"}</span><span className="font-mono">{selected.progress.completedSteps ?? 0}/{selected.progress.totalSteps ?? 0}</span></div><div className="mt-1 h-1 overflow-hidden rounded-full bg-muted"><div className="h-full bg-primary transition-[width]" style={{ width: `${Math.min(100, Math.max(0, ((selected.progress.completedSteps ?? 0) / Math.max(1, selected.progress.totalSteps ?? 1)) * 100))}%` }} /></div></div> : null}
    <div className="flex min-h-0 flex-1 flex-col overflow-hidden">
      <div className="flex shrink-0 gap-1 overflow-x-auto border-b border-border/70 p-2" role="tablist" aria-label="Research workspace views">{([['events','Events'],['studies','Study jobs'],['discoveries','Discoveries'],['sequences','Sequences'],['validation','Validation'],['factor-set','Factor Set'],['quality','Data quality']] as const).map(([value, label]) => <button key={value} type="button" role="tab" aria-selected={workspaceView === value} onClick={() => setWorkspaceView(value)} className={`whitespace-nowrap rounded-md px-2 py-1 text-[10px] font-medium focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring ${workspaceView === value ? "bg-primary text-primary-foreground" : "text-muted-foreground hover:bg-muted"}`}>{label}</button>)}</div>
      <div className="min-h-0 flex-1 overflow-y-auto p-1">
      {workspaceView === "events" ? <section className="rounded-md border border-dashed border-border/70 p-4 text-center text-xs text-muted-foreground">Select an event from the Observatory tab to keep its replay context while the study is configured.</section> : null}
      {workspaceView === "sequences" ? <section className="space-y-2 p-3"><div className="text-xs font-medium">Materialized sequences</div>{sequences.length === 0 ? <div className="rounded-md border border-dashed border-border/70 p-4 text-center text-xs text-muted-foreground">No point-in-time sequence snapshots are available for this study.</div> : <div className="space-y-2">{sequenceGroups.slice(0, 12).map(([key, points]) => <div key={key} className="rounded-md border border-border/60 bg-muted/30 p-2"><div className="flex justify-between gap-2 text-[10px]"><span className="truncate font-medium">{key}</span><span className="font-mono text-muted-foreground">{points[0]?.code}</span></div><div className="mt-2 flex h-10 items-end gap-px" aria-label={`${key} sequence sparkline`}>{points.slice(0, 60).map((point) => <span key={`${point.code}-${point.offset}`} title={`offset ${point.offset}: ${point.normalizedValue ?? "unavailable"}`} className="min-w-1 flex-1 bg-primary/50" style={{ height: `${Math.min(100, Math.max(4, Math.abs(Number(point.normalizedValue ?? 0)) * 22 + 10))}%` }} />)}</div><div className="mt-1 font-mono text-[10px] text-muted-foreground">{points.length} points · as-of {points[0]?.availableAt ?? "unavailable"}</div></div>)}</div>}</section> : null}
      {workspaceView === "validation" ? <section className="space-y-3 p-3"><div><div className="text-xs font-medium">Historical validation</div><p className="mt-1 text-[10px] leading-4 text-muted-foreground">All figures are computed from persisted point-in-time outcomes. Coverage is shown separately from excess return.</p></div>{horizonValidation.length === 0 ? <div className="rounded-md border border-dashed border-border/70 p-4 text-center text-xs text-muted-foreground">No persisted outcome snapshots are available for this study.</div> : <><div className="grid gap-2 sm:grid-cols-2">{horizonValidation.map((item) => <div key={item.horizon} className="rounded-md border border-border/60 bg-muted/25 p-2"><div className="flex items-baseline justify-between gap-2"><span className="font-mono text-[11px]">D{item.horizon}</span><span className="font-mono text-[10px] text-muted-foreground">{item.coveragePct.toFixed(0)}% coverage</span></div><div className="mt-2 h-1.5 overflow-hidden rounded-full bg-muted"><div className="h-full bg-primary" style={{ width: `${Math.min(100, item.coveragePct)}%` }} /></div><p className={`mt-2 font-mono text-[11px] ${item.averageExcessPct == null ? "text-muted-foreground" : item.averageExcessPct >= 0 ? "text-[#c8332c]" : "text-[#14804a]"}`}>{item.averageExcessPct == null ? "Excess unavailable" : `${item.averageExcessPct >= 0 ? "+" : ""}${item.averageExcessPct.toFixed(2)}% average excess`}</p></div>)}</div><div className="overflow-x-auto rounded-md border border-border/60"><table className="min-w-[580px] w-full text-left text-[10px]"><thead className="bg-muted/45 text-muted-foreground"><tr><th className="px-2 py-1.5 font-medium">Discovery</th><th className="px-2 py-1.5 font-medium">Chronological split</th><th className="px-2 py-1.5 font-medium">Monthly</th><th className="px-2 py-1.5 font-medium">Regime</th><th className="px-2 py-1.5 font-medium">Outcome</th></tr></thead><tbody className="divide-y divide-border/60">{validationRows.map((row) => <tr key={row.discoveryCode}><td className="max-w-52 truncate px-2 py-1.5 font-mono" title={row.discoveryCode}>{row.discoveryCode}</td><td className="px-2 py-1.5">{row.train}</td><td className="px-2 py-1.5">{row.monthly}</td><td className="px-2 py-1.5">{row.regime}</td><td className="px-2 py-1.5">{row.outcome}</td></tr>)}</tbody></table></div><div className="border-t border-border/70 pt-3"><AiBindingProviderSelect capability="market_factor_study_synthesis" label="Provider" modelLabel="Model" /><Button className="mt-2" variant="secondary" size="sm" onClick={() => void synthesizeStudy()} disabled={!selected || loading}><FlaskConical />Synthesize study</Button>{studySynthesis ? <div className="mt-2 rounded-md bg-muted/45 p-2 text-[10px]"><p className="font-medium">{studySynthesis.synthesis_result.summary}</p>{studySynthesis.synthesis_result.findings.map((finding) => <p key={finding.label} className="mt-1 text-muted-foreground">{finding.label}: {finding.interpretation}</p>)}</div> : null}</div></>}</section> : null}
      {workspaceView === "quality" ? <section className="space-y-3 p-3"><div><div className="text-xs font-medium">Data quality dashboard</div><p className="mt-1 text-[10px] leading-4 text-muted-foreground">Quality gates are derived from persisted study metadata and materialized evidence; failed gates remain visible as alerts.</p></div>{selected ? <div className="grid gap-2 sm:grid-cols-2"><QualityMetric label="Study status" value={statusLabel[selected.status] ?? selected.status} alert={selected.status === "FAILED"} /><QualityMetric label="Leakage gate" value={selected.leakageCheckPassed ? "Passed" : "Failed"} alert={!selected.leakageCheckPassed} /><QualityMetric label="Event samples" value={String(selected.eventSampleCount)} alert={selected.eventSampleCount === 0} /><QualityMetric label="Control samples" value={String(selected.controlSampleCount)} alert={selected.controlSampleCount === 0} /><QualityMetric label="Feature version" value={selected.featureRegistryVersion || "Unavailable"} /><QualityMetric label="Data cutoff" value={selected.dataCutoffAt || "Unavailable"} /></div> : <div className="rounded-md border border-dashed border-border/70 p-4 text-center text-xs text-muted-foreground">Select a persisted study to inspect quality gates.</div>}{selected?.warnings.length ? <div className="space-y-1 border-t border-border/70 pt-3" role="alert"><div className="text-[11px] font-medium text-amber-700">Active quality alerts</div>{selected.warnings.map((warning) => <div key={warning} className="text-[10px] leading-4 text-amber-700">{warning}</div>)}</div> : null}</section> : null}
      {workspaceView === "studies" ? <section className="min-h-0 overflow-y-auto p-3"><div className="mb-2 flex items-center justify-between"><span className="text-xs font-medium">Research jobs</span><div className="flex gap-1">{selected && ["QUEUED", "RUNNING"].includes(selected.status) ? <Button variant="ghost" size="icon-sm" title="Cancel study" aria-label="Cancel study" onClick={() => void cancelSelectedStudy()}><XCircle className="size-3.5" /></Button> : null}<Button variant="ghost" size="icon-sm" title="Refresh studies" aria-label="Refresh studies" onClick={() => void load()}><RefreshCw className="size-3.5" /></Button></div></div>{studies.length === 0 && !loading ? <div className="py-6 text-center text-[11px] text-muted-foreground"><FlaskConical className="mx-auto mb-2 size-4" />No research studies yet.</div> : <div className="space-y-1">{studies.map((study) => <button key={study.studyId} type="button" onClick={() => { setSelected(study); const next = new URLSearchParams(searchParams); next.set("studyId", study.studyId); setSearchParams(next, { replace: true }) }} className="block w-full rounded-sm border border-transparent p-2 text-left hover:border-border hover:bg-muted/50"><div className="flex justify-between gap-2"><span className="truncate text-xs font-medium">{study.eventType}</span><Badge variant="secondary" className="text-[10px]">{statusLabel[study.status] ?? study.status}</Badge></div><div className="mt-1 truncate font-mono text-[10px] text-muted-foreground">{study.startDate} → {study.endDate}</div></button>)}</div>}<div className="mt-3 flex items-center justify-between border-t border-border/70 pt-2"><Button variant="ghost" size="sm" disabled={studyOffset === 0 || loading} onClick={() => setStudyOffset((value) => Math.max(0, value - 50))}>Previous</Button><span className="font-mono text-[10px] text-muted-foreground">{studyOffset + 1}–{studyOffset + studies.length}</span><Button variant="ghost" size="sm" disabled={studies.length < 50 || loading} onClick={() => setStudyOffset((value) => value + 50)}>Next</Button></div></section> : null}
      {workspaceView === "discoveries" ? <section className="min-h-0 overflow-y-auto p-3"><div className="mb-1 flex items-center justify-between gap-2"><div className="text-xs font-medium">Discoveries {selected ? `· ${selected.studyId}` : ""}</div><Select value={discoveryScope} onValueChange={(value) => setDiscoveryScope(value as typeof discoveryScope)}><SelectTrigger size="sm" className="h-7 w-28 text-[10px]"><SelectValue /></SelectTrigger><SelectContent><SelectItem value="ALL">All scopes</SelectItem><SelectItem value="COMMON">Common</SelectItem><SelectItem value="SUBGROUP">Subgroup</SelectItem><SelectItem value="RARE_SPECIAL">Rare special</SelectItem><SelectItem value="UNEXPLAINED">Unexplained</SelectItem></SelectContent></Select></div>{discoveries.length === 0 ? <p className="text-[11px] leading-5 text-muted-foreground">No persisted discoveries. A completed study shows evidence, controls, validation verdicts and caveats here.</p> : <div className="divide-y divide-border/70 border-y border-border/70">{discoveries.filter((item) => discoveryScope === "ALL" || item.scope === discoveryScope).map((item) => <button type="button" key={item.discoveryCode} onClick={() => { setExpressionField("discoveryCode"); setExpressionValue(item.discoveryCode); void openDiscovery(item) }} className="block w-full py-2 text-left hover:bg-muted/50"><div className="flex justify-between gap-2"><span className="truncate text-[11px] font-medium">{item.discoveryCode}</span><Badge variant="secondary" className="text-[10px]">{item.verdict}</Badge></div><div className="mt-1 flex gap-1 font-mono text-[10px] text-muted-foreground"><span>{item.scope}</span><span>·</span><span>{item.method}</span><span>· lift {String(item.statistics?.lift ?? "—")}</span></div></button>)}</div>}<div className="mt-3"><div className="mb-1 text-[11px] font-medium">Discovered clusters</div>{clusters.length === 0 ? <p className="text-[10px] text-muted-foreground">No clusters persisted.</p> : <div className="grid gap-1 sm:grid-cols-2">{clusters.map((cluster) => <div key={cluster.clusterCode} className="rounded-sm bg-muted/45 px-2 py-1.5"><div className="flex justify-between text-[10px]"><span className="font-medium">{cluster.clusterCode}</span><span className="font-mono text-muted-foreground">{cluster.memberCount} members</span></div><div className="mt-1 text-[10px] text-muted-foreground">{cluster.method} · {cluster.discoveryCodes.slice(0, 3).join(", ")}</div></div>)}</div>}</div><div className="mt-3"><div className="mb-1 text-[11px] font-medium">Study samples</div>{samples.length === 0 ? <p className="text-[10px] leading-4 text-muted-foreground">No point-in-time event/control samples are available.</p> : <div className="space-y-1">{samples.slice(0, 5).map((item) => <div key={`${item.sampleType}-${item.market}-${item.code}-${item.eventTradeDate}`} className="rounded-sm bg-muted/45 px-2 py-1.5"><div className="flex justify-between font-mono text-[10px]"><span>{item.code}</span><span>{item.sampleType}</span></div><div className="mt-1 text-[10px] text-muted-foreground">{item.eventTradeDate} · {String(item.matchingMetadata.industry ?? "industry unavailable")}</div></div>)}</div>}</div><div className="mt-3"><div className="mb-1 text-[11px] font-medium">Outcome validation</div>{outcomes.length === 0 ? <p className="text-[10px] leading-4 text-muted-foreground">No persisted post-event outcomes are available for this Study.</p> : <div className="space-y-1">{outcomes.filter((item) => item.sampleType === "EVENT").slice(0, 5).map((item) => <div key={`${item.market}-${item.code}-${item.eventTradeDate}`} className="rounded-sm bg-muted/45 px-2 py-1.5"><div className="flex justify-between font-mono text-[10px]"><span>{item.code}</span><span>{item.eventTradeDate}</span></div><div className="mt-1 flex flex-wrap gap-x-2 gap-y-1 font-mono text-[10px] text-muted-foreground">{(Array.isArray(item.outcomes) ? item.outcomes : []).filter((outcome) => outcome.status === "AVAILABLE").map((outcome) => <span key={outcome.horizon}>D{outcome.horizon} excess {outcome.excessReturnPct == null ? "—" : `${Number(outcome.excessReturnPct).toFixed(2)}%`}</span>)}</div></div>)}</div>}</div></section> : null}
      {workspaceView === "factor-set" ? <section className="min-h-0 overflow-y-auto p-3"><div className="mb-2 flex items-center justify-between"><span className="text-xs font-medium">Factor Set editor</span><Button variant="ghost" size="icon-sm" title="Create Factor Set" aria-label="Create Factor Set" onClick={startFactorSet}><Plus className="size-3.5" /></Button></div>
        <div className="space-y-2 rounded-md bg-muted/45 p-2">
          <Input aria-label="Factor Set code" value={factorSetCode} disabled={Boolean(selectedFactorSet)} onChange={(event) => setFactorSetCode(event.target.value)} placeholder="Code, e.g. VOLUME_BREAKOUT" className="h-8 text-xs font-mono" />
          <Input aria-label="Factor Set name" value={factorSetName} onChange={(event) => setFactorSetName(event.target.value)} placeholder="Factor Set name" className="h-8 text-xs" />
          <div className="grid grid-cols-2 gap-2"><Select value={expressionOperator} onValueChange={(value) => setExpressionOperator(value as "AND" | "OR")}><SelectTrigger size="sm" className="text-xs"><SelectValue /></SelectTrigger><SelectContent><SelectItem value="AND">AND</SelectItem><SelectItem value="OR">OR</SelectItem></SelectContent></Select><Select value={expressionField} onValueChange={setExpressionField}><SelectTrigger size="sm" className="text-xs"><SelectValue /></SelectTrigger><SelectContent><SelectItem value="discoveryCode">Discovery recipe</SelectItem><SelectItem value="industry">Industry</SelectItem><SelectItem value="regime">Market regime</SelectItem></SelectContent></Select></div>
          <div className="grid grid-cols-[minmax(0,1fr)_64px] gap-2"><Input aria-label="Condition value" value={expressionValue} onChange={(event) => setExpressionValue(event.target.value)} placeholder="Value or discovery code" className="h-8 text-xs" /><Input aria-label="Condition weight" type="number" min="0.01" step="0.01" value={expressionWeight} onChange={(event) => setExpressionWeight(event.target.value)} className="h-8 text-xs font-mono" /></div>
          <Textarea readOnly aria-label="Generated typed expression" value={JSON.stringify({ operator: expressionOperator, clauses: [{ field: expressionField, operator: "EQUALS", value: expressionValue, weight: Number(expressionWeight) || 0 }], schemaVersion: 1 }, null, 2)} className="min-h-20 resize-y rounded-md bg-background font-mono text-[10px]" />
          <div className="flex flex-wrap gap-1"><Button variant="default" size="sm" onClick={() => void saveFactorSet()} disabled={factorSetSaving}>{factorSetSaving ? <Loader2 className="animate-spin" /> : <Save />} {selectedFactorSet ? "Save vNext" : "Create draft"}</Button>{selectedFactorSet && latestVersion?.status === "DRAFT" ? <Button variant="secondary" size="sm" onClick={() => void validateFactorSet()} disabled={factorSetSaving}><CheckCircle2 />Validate</Button> : null}{selectedFactorSet && latestVersion?.status === "VALIDATED" ? <Button variant="secondary" size="sm" onClick={() => void promoteFactorSet()} disabled={factorSetSaving}>Promote</Button> : null}{selectedFactorSet && latestVersion?.status === "PROMOTED" ? <Button variant="secondary" size="sm" onClick={() => void previewFactorSet()} disabled={factorSetSaving}><Eye />Preview</Button> : null}</div>
          <p className="text-[10px] leading-4 text-muted-foreground">Save creates an immutable new version. Validation and promotion are explicit; this editor never creates a strategy automatically.</p>
          {factorSetPreview ? <div className="space-y-1 border-t border-border/70 pt-2 text-[10px]"><div className="flex justify-between gap-2"><span className="font-medium">Screen preview</span><Badge variant="secondary" className="text-[10px]">{factorSetPreview.status}</Badge></div><p className="font-mono text-muted-foreground">{factorSetPreview.count} matches · {factorSetPreview.snapshotTradeDate ?? "no snapshot"}</p>{factorSetPreview.items.slice(0, 5).map((item) => <div key={`${item.market}-${item.code}`} className="flex justify-between gap-2 rounded-sm bg-background px-2 py-1"><span className="truncate font-mono">{item.code} {item.name ?? ""}</span><span className="shrink-0 text-muted-foreground">{item.reason}</span></div>)}{factorSetPreview.warnings.map((warning) => <p key={warning} className="text-amber-700">{warning}</p>)}</div> : null}
        </div>
        <div className="mt-3"><div className="mb-1 text-[11px] font-medium">Saved versions</div>{factorSets.length === 0 ? <p className="text-[11px] leading-5 text-muted-foreground">No Factor Sets yet. Start from a reviewed discovery or enter a typed condition.</p> : <div className="divide-y divide-border/70 border-y border-border/70">{factorSets.map((item) => <button type="button" key={item.code} onClick={() => hydrateFactorSet(item)} className="block w-full py-2 text-left hover:bg-muted/50"><div className="flex justify-between gap-2"><span className="truncate text-[11px] font-medium">{item.name}</span><Badge variant="secondary" className="text-[10px]">{item.activeVersion?.status ?? item.status}</Badge></div><div className="mt-1 font-mono text-[10px] text-muted-foreground">{item.code} · v{item.activeVersion?.versionNo ?? "—"}</div></button>)}</div>}</div>
      </section> : null}
      </div>
    </div>
    <Sheet open={selectedDiscovery !== null || discoveryLoading} onOpenChange={(open) => { if (!open) setSelectedDiscovery(null) }}><SheetContent className="w-full overflow-y-auto sm:max-w-lg"><SheetHeader><SheetTitle>{selectedDiscovery?.discoveryCode ?? "Discovery detail"}</SheetTitle><SheetDescription>Persisted recipe, learned bounds, evidence, and validation only.</SheetDescription></SheetHeader>{discoveryLoading ? <div className="py-6 text-sm text-muted-foreground">Loading discovery…</div> : selectedDiscovery ? <div className="space-y-4 py-4 text-xs"><DetailBlock title="Recipe" value={selectedDiscovery.recipe} /><DetailBlock title="Learned bounds" value={selectedDiscovery.learnedBounds} /><DetailBlock title="Validation" value={selectedDiscovery.validation} /><DetailBlock title="Evidence" value={selectedDiscovery.evidence} /></div> : null}</SheetContent></Sheet>
  </div>
}

function DetailBlock({ title, value }: { title: string; value: Record<string, unknown> }) { return <section className="space-y-1"><h3 className="text-xs font-semibold">{title}</h3><pre className="overflow-x-auto rounded-md bg-muted/50 p-3 font-mono text-[10px] leading-4 text-foreground">{JSON.stringify(value, null, 2)}</pre></section> }

function QualityMetric({ label, value, alert = false }: { label: string; value: string; alert?: boolean }) {
  return <div className={`rounded-md border p-2 ${alert ? "border-amber-300/70 bg-amber-500/5" : "border-border/60 bg-muted/25"}`}><div className="text-[10px] text-muted-foreground">{label}</div><div className="mt-1 flex items-center justify-between gap-2"><span className="truncate font-mono text-[11px]">{value}</span>{alert ? <AlertTriangle className="size-3.5 shrink-0 text-amber-600" aria-label="Quality alert" /> : <CheckCircle2 className="size-3.5 shrink-0 text-emerald-600" aria-label="Quality passed" />}</div></div>
}
