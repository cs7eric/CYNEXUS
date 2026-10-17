import { Bot, Braces, Database, FileText, Gauge, Layers3, MessageSquareText, Network, Radio, Route, Send, Server, ShieldCheck, Sparkles, Wrench, type LucideIcon } from "lucide-react"

import { Badge } from "@/components/ui/badge"
import { cn } from "@/lib/utils"

type Tone = "domain" | "framework" | "state" | "external" | "optional"

const tone: Record<Tone, string> = {
  domain: "border-emerald-300 bg-emerald-50/75 text-emerald-950 dark:border-emerald-900 dark:bg-emerald-950/30 dark:text-emerald-100",
  framework: "border-sky-300 bg-sky-50/75 text-sky-950 dark:border-sky-900 dark:bg-sky-950/30 dark:text-sky-100",
  state: "border-violet-300 bg-violet-50/75 text-violet-950 dark:border-violet-900 dark:bg-violet-950/30 dark:text-violet-100",
  external: "border-amber-300 bg-amber-50/75 text-amber-950 dark:border-amber-900 dark:bg-amber-950/30 dark:text-amber-100",
  optional: "border-dashed border-border bg-muted/55 text-foreground",
}

function MapNode({ icon: Icon, title, detail, code, toneKey = "domain", className }: { icon: LucideIcon; title: string; detail: string; code: string; toneKey?: Tone; className: string }) {
  return <article className={cn("absolute rounded-md border p-2.5 shadow-none", tone[toneKey], className)}><div className="flex items-center gap-1.5 border-b border-current/15 pb-1.5"><span className="flex size-5 shrink-0 items-center justify-center rounded-sm bg-white/60 dark:bg-black/20"><Icon className="size-3" /></span><h3 className="min-w-0 text-[11px] font-semibold leading-4">{title}</h3></div><p className="mt-1.5 text-[9px] leading-3.5 opacity-80">{detail}</p><code className="mt-1.5 block border-t border-current/15 pt-1.5 font-mono text-[8px] leading-3 opacity-70">{code}</code></article>
}

function Connector({ x1, y1, x2, y2, label, dashed = false, warm = false }: { x1: number; y1: number; x2: number; y2: number; label?: string; dashed?: boolean; warm?: boolean }) {
  const mx = (x1 + x2) / 2
  const my = (y1 + y2) / 2
  return <g><path d={`M ${x1} ${y1} L ${x2} ${y2}`} fill="none" stroke={warm ? "#d97706" : "currentColor"} strokeWidth="1.5" strokeDasharray={dashed ? "5 4" : undefined} markerEnd="url(#arrow)" /><circle cx={x1} cy={y1} r="2" fill={warm ? "#d97706" : "currentColor"} />{label ? <text x={mx} y={my - 5} textAnchor="middle" className="fill-muted-foreground text-[10px]">{label}</text> : null}</g>
}

export function UnifiedArchitectureCanvas() {
  return <section className="overflow-hidden rounded-lg border border-border bg-card"><div className="border-b border-border px-4 py-3"><div className="flex gap-3"><div className="flex size-8 shrink-0 items-center justify-center rounded-md bg-muted"><Network className="size-4" /></div><div><h2 className="text-base font-semibold">Cynexus AI architecture</h2><p className="mt-0.5 text-xs leading-5 text-muted-foreground">A single connected technical map. Solid lines are active calls or data paths; dashed lines are isolated/optional paths. Scroll horizontally on narrow screens.</p></div></div></div><div className="border-b border-border bg-muted/20 px-4 py-2"><div className="flex flex-wrap gap-4 text-[10px] text-muted-foreground"><span className="flex items-center gap-1.5"><i className="size-2.5 rounded-sm border border-emerald-300 bg-emerald-50" />Cynexus policy and lifecycle</span><span className="flex items-center gap-1.5"><i className="size-2.5 rounded-sm border border-sky-300 bg-sky-50" />Framework runtime</span><span className="flex items-center gap-1.5"><i className="size-2.5 rounded-sm border border-violet-300 bg-violet-50" />Shared/durable infrastructure</span><span className="flex items-center gap-1.5"><i className="size-2.5 rounded-sm border border-amber-300 bg-amber-50" />External endpoint</span><span className="flex items-center gap-1.5"><i className="size-2.5 rounded-sm border border-dashed border-border bg-muted" />Disabled or isolated</span></div></div><div className="overflow-x-auto"><div className="relative h-[1050px] w-[1560px] bg-[linear-gradient(to_right,hsl(var(--border)/0.32)_1px,transparent_1px),linear-gradient(to_bottom,hsl(var(--border)/0.32)_1px,transparent_1px)] bg-[size:32px_32px]">
    <svg className="pointer-events-none absolute inset-0 h-full w-full text-muted-foreground" viewBox="0 0 1560 1050" aria-hidden="true"><defs><marker id="arrow" markerWidth="7" markerHeight="7" refX="6" refY="3.5" orient="auto"><path d="M0,0 L7,3.5 L0,7 z" fill="currentColor" /></marker></defs>
      <Connector x1={170} y1={285} x2={240} y2={285} label="HTTPS / Feign" />
      <Connector x1={680} y1={215} x2={700} y2={285} label="command" />
      <Connector x1={910} y1={320} x2={925} y2={320} label="context" />
      <Connector x1={1055} y1={320} x2={1080} y2={320} label="typed call" />
      <Connector x1={1210} y1={320} x2={1240} y2={285} label="ChatModel" />
      <Connector x1={1370} y1={285} x2={1450} y2={285} label="HTTPS" warm />
      <Connector x1={1280} y1={125} x2={765} y2={185} label="binding / Prompt / Provider config" />
      <Connector x1={990} y1={385} x2={985} y2={505} label="typed failure" dashed />
      <Connector x1={1085} y1={550} x2={1180} y2={550} label="eligible 429/5xx/timeout" dashed />
      <Connector x1={1340} y1={550} x2={1450} y2={310} label="same routed model" dashed warm />
      <Connector x1={445} y1={425} x2={445} y2={600} label="AI enrichment result" />
      <Connector x1={570} y1={650} x2={665} y2={650} label="document changed" />
      <Connector x1={785} y1={650} x2={870} y2={650} label="AMQP" />
      <Connector x1={1000} y1={650} x2={1090} y2={650} label="chunks" />
      <Connector x1={1210} y1={650} x2={1300} y2={670} label="vectors" />
      <Connector x1={1410} y1={730} x2={1210} y2={810} label="filtered search" />
      <Connector x1={1090} y1={840} x2={970} y2={840} label="authorized sources" />
      <Connector x1={850} y1={840} x2={710} y2={840} label="generic Spring AI answer" />
      <Connector x1={940} y1={770} x2={940} y2={900} label="Agent run" />
      <Connector x1={1060} y1={970} x2={1210} y2={970} label="lease / rate / circuit" />
      <Connector x1={940} y1={930} x2={620} y2={930} label="registered tools" />
      <Connector x1={620} y1={970} x2={510} y2={970} label="preview -> confirm -> audit" />
      <Connector x1={1060} y1={940} x2={1180} y2={580} label="Responses / SSE" />
      <Connector x1={270} y1={785} x2={270} y2={920} label="attempt logs" />
      <Connector x1={390} y1={970} x2={390} y2={1020} label="best effort" dashed />
      <Connector x1={1370} y1={980} x2={1480} y2={980} label="bounded HTTP only" dashed />
    </svg>

    <div className="absolute left-[210px] top-[70px] h-[720px] w-[1040px] rounded-xl border-2 border-emerald-300 bg-background/70 p-4 dark:border-emerald-900"><div className="flex items-center gap-2"><Layers3 className="size-4 text-emerald-700" /><span className="text-xs font-semibold">Cynexus services</span><Badge variant="secondary" className="rounded-md text-[9px]">business governance boundary</Badge></div><p className="mt-1 text-[9px] text-muted-foreground">Capability meaning, route/Pprompt policy, tenant/principal boundaries, semantic validation, lifecycle writes and audit stay here.</p></div>
    <div className="absolute left-[1280px] top-[55px] w-[180px] text-[10px] font-medium text-muted-foreground">CONTROL PLANE</div><div className="absolute left-[1265px] top-[525px] w-[180px] text-[10px] font-medium text-muted-foreground">KNOWLEDGE PROJECTION</div><div className="absolute left-[245px] top-[895px] w-[180px] text-[10px] font-medium text-muted-foreground">OBSERVABILITY</div>

    <MapNode icon={MessageSquareText} title="Users / integrations" detail="Cynexus UI, REST/Feign consumers and scheduled workflows." code="cynexus-ui · API clients" toneKey="external" className="left-[20px] top-[225px] w-[150px]" />
    <MapNode icon={FileText} title="Core media process" detail="Upload/transcription state, AI client call, history and knowledge draft orchestration." code="MediaTranscriptionProcessDomainServiceImpl" className="left-[255px] top-[140px] w-[190px]" />
    <MapNode icon={MessageSquareText} title="AI Application" detail="Controllers and app services map commands and coordinate feature adapters." code="cynexus-ai-application" className="left-[470px] top-[140px] w-[210px]" />
    <MapNode icon={ShieldCheck} title="AI Domain" detail="Route candidates, Prompt/runtime resolution, execution context, structured/business validation." code="ApplicationAnalysisRoutingDomainServiceImpl" className="left-[700px] top-[245px] w-[210px]" />
    <MapNode icon={Wrench} title="AgentExecutor" detail="Ordinary text/vision bridge. Calls LangChain4j first; preserves execution context." code="completeDetailed · completeVisionDetailed" className="left-[925px] top-[245px] w-[125px]" />
    <MapNode icon={Bot} title="LangChain4j" detail="Typed AI Service + POJO facade. No routing, repositories or business writes." code="LangChain4jCompletionAdapter" toneKey="framework" className="left-[1080px] top-[245px] w-[130px]" />
    <MapNode icon={Braces} title="Spring AI runtime" detail="LangChain bridge and dynamic OpenAI-compatible ChatModel." code="SpringBackedLangChainChatModel · SpringAiModelFactory" toneKey="framework" className="left-[1240px] top-[220px] w-[130px]" />
    <MapNode icon={Send} title="Model providers" detail="Chat/vision/Responses: Xhhai, Grok, compatible endpoints." code="OpenAI-compatible HTTPS" toneKey="external" className="left-[1450px] top-[225px] w-[90px]" />

    <MapNode icon={Database} title="PostgreSQL configuration" detail="Binding routes provider/model/fallback; Prompt rows supply active template/schema/version; provider rows supply endpoint/secrets/defaults." code="ai_usage_bindings · ai_prompt · ai_providers" toneKey="state" className="left-[1265px] top-[85px] w-[225px]" />
    <MapNode icon={Route} title="Spring generic fallback" detail="After a typed-call failure, generic Spring AI attempts the already selected model." code="SpringAiCompletionGateway -> SpringAiModelExecutor" toneKey="framework" className="left-[875px] top-[500px] w-[210px]" />
    <MapNode icon={Server} title="Legacy adapter fallback" detail="Only eligible transport failures use the old HTTP adapter; Agent calls use it directly." code="AiCompletionGatewayImpl -> OpenAiCompatibleChatAdapter" toneKey="optional" className="left-[1180px] top-[500px] w-[160px]" />

    <MapNode icon={FileText} title="Knowledge Base" detail="Document lifecycle persists drafts/updates, resolves metadata and emits index event after successful write." code="KnowledgeBaseDocumentWriterImpl · KnowledgeDocumentLifecycleDomainServiceImpl" className="left-[350px] top-[600px] w-[220px]" />
    <MapNode icon={Radio} title="RabbitMQ" detail="Changed/deleted document event; manual ack and retry on downstream failures." code="knowledge.document.changed.v1" toneKey="state" className="left-[665px] top-[600px] w-[120px]" />
    <MapNode icon={Wrench} title="Indexing service" detail="Validates identity, deletes old chunks, indexes published content at 1200/120." code="KnowledgeIndexRabbitListener -> KnowledgeIndexingService" toneKey="framework" className="left-[870px] top-[600px] w-[130px]" />
    <MapNode icon={Sparkles} title="Embedding" detail="Spring AI OpenAiEmbeddingModel against DashScope text-embedding-v4." code="SpringAiPgVectorConfiguration" toneKey="framework" className="left-[1090px] top-[600px] w-[120px]" />
    <MapNode icon={Database} title="PGVector" detail="vector_store keeps tenant, owner, scope, status and chunk metadata." code="SpringAiKnowledgeRagAdapter" toneKey="state" className="left-[1300px] top-[600px] w-[110px]" />
    <MapNode icon={MessageSquareText} title="RAG Domain" detail="Authorized retrieval occurs before Prompt build and generic answer call. LangChain4j is not used here." code="KnowledgeRagAnswerDomainService" className="left-[850px] top-[790px] w-[200px]" />
    <MapNode icon={Sparkles} title="RAG answer model" detail="Generic AiModelExecutor -> Spring AI -> routed provider response with citations." code="SpringAiModelExecutor" toneKey="framework" className="left-[600px] top-[790px] w-[110px]" />

    <MapNode icon={Database} title="ai_execution_log" detail="Feature services persist bounded request/response, Prompt/provider/model, tokens, latency and error." code="AiExecutionLogRepository" toneKey="state" className="left-[170px] top-[920px] w-[200px]" />
    <MapNode icon={Server} title="Langfuse" detail="Optional telemetry only. Reporting cannot change a business result or fallback." code="LangfuseGenerationReporter" toneKey="external" className="left-[300px] top-[985px] w-[140px]" />
    <MapNode icon={Bot} title="Native Agent Run" detail="Owns Run/Step/Message, history, quotas, cancellation, cache telemetry and tool loop." code="AgentRunExecutionDomainService" className="left-[830px] top-[900px] w-[230px]" />
    <MapNode icon={Gauge} title="Redis" detail="Active lease, cancellation flag, rate limit and provider circuit breaker. No durable state." code="RedisAgentRuntimeLease" toneKey="state" className="left-[1210px] top-[920px] w-[165px]" />
    <MapNode icon={Wrench} title="Tool confirmation" detail="Registered tools only. Writes: preview -> PENDING_CONFIRMATION -> confirm -> execute -> audit." code="AgentToolRegistry · AgentWriteDomainService" className="left-[420px] top-[920px] w-[200px]" />
    <MapNode icon={Network} title="AgentScope" detail="Disabled by default; restricted bounded HTTP adapter for market_research*. No repository, writer or Agent Run handle." code="AgentScopeOrchestrator" toneKey="optional" className="left-[1370px] top-[910px] w-[165px]" />
  </div></div></section>
}
