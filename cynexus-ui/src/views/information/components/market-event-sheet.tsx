import {
  ArrowRight,
  Bot,
  Building2,
  ExternalLink,
  FileCheck2,
  FileText,
  Gauge,
  Network,
  RefreshCw,
  ScanSearch,
  ShieldAlert,
  Target,
} from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { ScrollArea } from "@/components/ui/scroll-area"
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from "@/components/ui/sheet"
import { cn } from "@/lib/utils"
import type { MarketInformationEventDetail } from "@/services/information"

interface EventAnalysis {
  summary?: string
  eventFacts?: string[]
  informationDelta?: string
  whyItMatters?: string
  transmissionPaths?: string[]
  marketExpectationGap?: string
  eventType?: string
  affectedScopes?: string[]
  affectedIndustries?: string[]
  affectedCompanies?: string[]
  entities?: string[]
  marketDirection?: string
  materiality?: string
  timeHorizon?: string
  novelty?: string
  catalysts?: string[]
  scenarios?: { bull?: string; base?: string; bear?: string }
  watchItems?: string[]
  confidence?: number
  contentQuality?: string
  evidence?: Array<{ claim?: string; quote?: string; sourceId?: string }>
  risks?: string[]
  uncertainties?: string[]
  disclaimer?: string
}

function parseAnalysis(value?: string | null): EventAnalysis | null {
  if (!value) return null
  try { return JSON.parse(value) as EventAnalysis } catch { return null }
}

function label(value?: string | null) {
  return (value || "unknown").replaceAll("_", " ")
}

function qualityLabel(value?: string | null) {
  const labels: Record<string, string> = {
    full_text: "Full text",
    summary_only: "Title / summary only",
    mixed: "Mixed source depth",
    ocr: "OCR text",
    unknown: "Unknown source depth",
  }
  return labels[value || "unknown"] ?? label(value)
}

function directionClass(value?: string) {
  if (value === "positive") return "bg-emerald-500/10 text-emerald-700 dark:text-emerald-300"
  if (value === "negative") return "bg-rose-500/10 text-rose-700 dark:text-rose-300"
  if (value === "mixed") return "bg-amber-500/10 text-amber-700 dark:text-amber-300"
  return "bg-muted text-foreground"
}

function AnalysisList({ items, empty = "No supported items." }: { items?: string[]; empty?: string }) {
  if (!items?.length) return <p className="text-sm text-muted-foreground">{empty}</p>
  return <ul className="space-y-2 text-sm leading-6">{items.map((item) => <li key={item} className="flex gap-2"><span className="mt-2 size-1.5 shrink-0 rounded-full bg-muted-foreground/55" /><span>{item}</span></li>)}</ul>
}

function ImpactRow({ label: rowLabel, values }: { label: string; values?: string[] }) {
  return <div className="grid grid-cols-[7.5rem_1fr] gap-3 py-2.5 first:pt-0 last:pb-0"><span className="text-xs font-medium text-muted-foreground">{rowLabel}</span><div className="flex flex-wrap gap-1.5">{values?.length ? values.map((item) => <Badge key={item} variant="secondary" className="border-0 font-normal">{item}</Badge>) : <span className="text-sm text-muted-foreground">Not identified</span>}</div></div>
}

export function MarketEventSheet({ open, loading, detail, onOpenChange, onAnalyze }: {
  open: boolean
  loading: boolean
  detail: MarketInformationEventDetail | null
  onOpenChange: (open: boolean) => void
  onAnalyze: () => void
}) {
  const analysis = parseAnalysis(detail?.analysisJson)
  const pending = ["PENDING", "RUNNING"].includes(detail?.analysisStatus ?? "")
  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent className="flex h-full w-[min(94vw,48rem)] flex-col gap-0 p-0 sm:max-w-none">
        <SheetHeader className="shrink-0 bg-muted/35 px-5 py-4 pr-12 text-left">
          <div className="flex flex-wrap items-center gap-1.5">
            <Badge variant="secondary" className="border-0">{detail?.category || "Market"}</Badge>
            {detail?.highValue ? <Badge variant="secondary" className="border-0 bg-primary/10 text-primary">Key event</Badge> : null}
            {analysis?.contentQuality ? <Badge variant="outline" className="font-normal">{qualityLabel(analysis.contentQuality)}</Badge> : null}
          </div>
          <SheetTitle className="text-base leading-6">{detail?.title || "Event Analysis"}</SheetTitle>
          <SheetDescription>{detail ? `${detail.members.length} related source${detail.members.length === 1 ? "" : "s"} · relevance ${detail.priorityScore.toFixed(1)}` : "Loading event details..."}</SheetDescription>
        </SheetHeader>

        <div className="flex shrink-0 items-center justify-between gap-3 border-y border-border/60 px-5 py-2.5">
          <div className="flex min-w-0 items-center gap-2 text-sm">
            <Bot className="size-4 shrink-0 text-primary" />
            <span className="font-medium capitalize">{label(detail?.analysisStatus).toLowerCase()}</span>
            {detail?.providerCode ? <span className="truncate text-xs text-muted-foreground">{detail.providerCode} / {detail.modelCode}{detail.fallbackUsed ? " · fallback" : ""}</span> : null}
          </div>
          <Button variant="secondary" size="sm" onClick={onAnalyze} disabled={pending || loading}>
            <RefreshCw className={cn("size-3.5", pending && "animate-spin")} />{pending ? "Analyzing" : analysis ? "Re-analyze" : "Analyze"}
          </Button>
        </div>

        <ScrollArea className="min-h-0 flex-1">
          <div className="px-5 py-5">
            {loading ? <div className="grid h-48 place-items-center text-sm text-muted-foreground">Loading event...</div> : null}
            {!loading && detail ? (
              <div className="space-y-7">
                {analysis ? (
                  <>
                    <section>
                      <div className="mb-3 flex items-center gap-2"><FileCheck2 className="size-4 text-primary" /><h3 className="text-sm font-semibold">What changed</h3></div>
                      <p className="text-[15px] font-medium leading-6 text-foreground">{analysis.informationDelta || analysis.summary}</p>
                      {analysis.summary && analysis.summary !== analysis.informationDelta ? <p className="mt-2 text-sm leading-6 text-muted-foreground">{analysis.summary}</p> : null}
                      <div className="mt-3 flex flex-wrap gap-1.5">
                        {[analysis.eventType, analysis.marketDirection, analysis.materiality, analysis.timeHorizon, analysis.novelty].filter(Boolean).map((item) => <Badge key={item} variant="secondary" className={cn("border-0 capitalize", item === analysis.marketDirection && directionClass(item))}>{label(item)}</Badge>)}
                        {analysis.marketExpectationGap ? <Badge variant="outline" className="capitalize">Expectation: {label(analysis.marketExpectationGap)}</Badge> : null}
                        {analysis.confidence != null ? <Badge variant="outline">{Math.round(analysis.confidence * 100)}% confidence</Badge> : null}
                      </div>
                    </section>

                    <section className="border-t border-border/60 pt-5">
                      <div className="mb-3 flex items-center gap-2"><ScanSearch className="size-4 text-primary" /><h3 className="text-sm font-semibold">Established facts</h3></div>
                      <AnalysisList items={analysis.eventFacts} empty="The available source does not establish facts beyond its title or summary." />
                    </section>

                    <section className="grid gap-6 border-t border-border/60 pt-5 md:grid-cols-2">
                      <div><div className="mb-3 flex items-center gap-2"><Gauge className="size-4 text-primary" /><h3 className="text-sm font-semibold">Why it matters</h3></div><p className="text-sm leading-6">{analysis.whyItMatters}</p></div>
                      <div><div className="mb-3 flex items-center gap-2"><Network className="size-4 text-primary" /><h3 className="text-sm font-semibold">Transmission path</h3></div><AnalysisList items={analysis.transmissionPaths} empty="No defensible transmission path can be established from the available source." /></div>
                    </section>

                    <section className="border-t border-border/60 pt-5">
                      <div className="mb-3 flex items-center gap-2"><Building2 className="size-4 text-primary" /><h3 className="text-sm font-semibold">Impact map</h3></div>
                      <div className="divide-y divide-border/60"><ImpactRow label="Industries" values={analysis.affectedIndustries} /><ImpactRow label="Companies" values={analysis.affectedCompanies} /><ImpactRow label="Scopes" values={analysis.affectedScopes} /></div>
                    </section>

                    <section className="border-t border-border/60 pt-5">
                      <div className="mb-3 flex items-center gap-2"><Target className="size-4 text-primary" /><h3 className="text-sm font-semibold">Conditional scenarios</h3></div>
                      <div className="grid overflow-hidden rounded-lg bg-muted/35 md:grid-cols-3 md:divide-x md:divide-border/60">
                        {(["bull", "base", "bear"] as const).map((key) => <div key={key} className="p-3.5"><div className="mb-1.5 text-xs font-semibold capitalize text-muted-foreground">{key} case</div><p className="text-sm leading-5">{analysis.scenarios?.[key] || "Insufficient evidence."}</p></div>)}
                      </div>
                    </section>

                    <section className="grid gap-6 border-t border-border/60 pt-5 md:grid-cols-2">
                      <div><h3 className="mb-3 text-sm font-semibold">Catalysts</h3><AnalysisList items={analysis.catalysts} /></div>
                      <div><h3 className="mb-3 text-sm font-semibold">Watch items</h3><AnalysisList items={analysis.watchItems} /></div>
                    </section>

                    <section className="grid gap-6 border-t border-border/60 pt-5 md:grid-cols-2">
                      <div><div className="mb-3 flex items-center gap-2"><ShieldAlert className="size-4 text-rose-600" /><h3 className="text-sm font-semibold">Risks</h3></div><AnalysisList items={analysis.risks} /></div>
                      <div><h3 className="mb-3 text-sm font-semibold">Uncertainties</h3><AnalysisList items={analysis.uncertainties} /></div>
                    </section>

                    {(analysis.evidence ?? []).length ? <section className="border-t border-border/60 pt-5"><h3 className="mb-3 text-sm font-semibold">Evidence trail</h3><div className="space-y-2">{analysis.evidence?.map((item, index) => <div key={`${item.sourceId}-${index}`} className="rounded-lg bg-muted/35 px-3.5 py-3"><p className="text-sm font-medium leading-5">{item.claim}</p><p className="mt-1.5 text-xs leading-5 text-muted-foreground">“{item.quote}”</p><p className="mt-1 text-[11px] text-muted-foreground">{item.sourceId}</p></div>)}</div></section> : null}
                  </>
                ) : <div className="rounded-lg bg-muted/35 px-4 py-4 text-sm leading-6 text-muted-foreground">No structured analysis is available yet. Run analysis to separate established facts from market-impact reasoning.</div>}

                <section className="border-t border-border/60 pt-5">
                  <div className="mb-3 flex items-center gap-2"><Building2 className="size-4 text-primary" /><h3 className="text-sm font-semibold">Deterministic industry mapping</h3></div>
                  <div className="flex flex-wrap gap-1.5">
                    {(detail.impactIndustries ?? []).length ? detail.impactIndustries?.map((item) => <Badge key={`${item.tdxCode}-${item.status}`} variant="secondary" className={cn("border-0", item.status === "Direct" ? "bg-emerald-500/10 text-emerald-700 dark:text-emerald-300" : "bg-amber-500/10 text-amber-700 dark:text-amber-300")} title={item.source ?? undefined}>{item.tdxName} · {item.status}</Badge>) : <span className="text-sm text-muted-foreground">Unmapped from available source identifiers.</span>}
                  </div>
                </section>

                <section className="border-t border-border/60 pt-5">
                  <div className="mb-3 flex items-center gap-2"><FileText className="size-4 text-primary" /><h3 className="text-sm font-semibold">Related sources</h3></div>
                  <div className="divide-y divide-border/60">{detail.members.map((item) => <div key={`${item.source}-${item.sourceRecordId}`} className="py-3 first:pt-0"><div className="flex items-start justify-between gap-3"><div className="min-w-0"><p className="text-sm font-medium leading-5">{item.title}</p><div className="mt-1 flex flex-wrap items-center gap-2 text-xs text-muted-foreground"><span>{item.source}</span><span className="inline-flex items-center gap-1"><ArrowRight className="size-3" />{qualityLabel(item.contentQuality || item.contentStatus)}</span></div></div>{item.url ? <Button variant="ghost" size="icon" className="size-8 shrink-0" asChild><a href={item.url} target="_blank" rel="noreferrer" aria-label="Open source"><ExternalLink className="size-3.5" /></a></Button> : null}</div>{item.summary ? <p className="mt-2 text-xs leading-5 text-muted-foreground">{item.summary}</p> : null}</div>)}</div>
                </section>
              </div>
            ) : null}
          </div>
        </ScrollArea>
      </SheetContent>
    </Sheet>
  )
}
