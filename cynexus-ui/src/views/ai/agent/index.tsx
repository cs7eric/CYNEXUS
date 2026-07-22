import { useEffect, useMemo, useRef, useState } from "react"
import { AlertCircle, ArrowUpRight, Check, CircleDashed, Download, ExternalLink, History, RefreshCw, Sparkles, Square, Wrench } from "lucide-react"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Textarea } from "@/components/ui/textarea"
import { WorkspaceShell } from "@/layout/workspace-shell"
import { cancelAgent, exportAgentRun, fetchAgentConversations, fetchAgentMessages, fetchAgentTools, getAgentRun, regenerateAgent, retryAgent, runAgentStream, searchAgentConversations, type AgentCancellationResult, type AgentCitation, type AgentConversation, type AgentMessage, type AgentRun, type AgentRunStep, type AgentToolDefinition } from "@/services/ai/agent"
import { Input } from "@/components/ui/input"
import { AgentFeedbackPanel } from "./components/agent-feedback-panel"
import { AgentWritePanel } from "./components/agent-write-panel"

function stepLabel(step: AgentRunStep) {
  if (step.kind === "ANSWER") return "Answer"
  return step.toolCode ?? "Data tool"
}

function statusIcon(status?: string | null) {
  if (status === "SUCCEEDED") return <Check className="size-3.5" />
  if (status === "FAILED") return <AlertCircle className="size-3.5" />
  return <CircleDashed className="size-3.5" />
}

function outputPreview(step: AgentRunStep) {
  if (!step.outputJson) return "No output returned"
  try {
    const parsed = JSON.parse(step.outputJson) as Record<string, unknown>
    return typeof parsed === "object" ? JSON.stringify(parsed, null, 2).slice(0, 680) : String(parsed)
  } catch {
    return step.outputJson.slice(0, 680)
  }
}

function evidence(run: AgentRun) {
  if (!run.evidenceJson) return [] as Array<{ toolCode?: string; source?: string; sourceUrl?: string; dataDate?: string; summary?: string; citations?: AgentCitation[] }>
  try {
    const value = JSON.parse(run.evidenceJson) as unknown
    return Array.isArray(value) ? value as Array<{ toolCode?: string; source?: string; sourceUrl?: string; dataDate?: string; summary?: string; citations?: AgentCitation[] }> : []
  } catch {
    return []
  }
}

function clarification(run: AgentRun) {
  if (run.status !== "NEEDS_CLARIFICATION" || !run.clarificationJson) return null
  try {
    const value = JSON.parse(run.clarificationJson) as { summary?: string; questions?: string[]; missingFields?: string[] }
    return value.questions?.length ? value : null
  } catch {
    return null
  }
}

function CitationList({ citations }: { citations: AgentCitation[] }) {
  return <div className="mt-2 space-y-1.5 border-t border-border/60 pt-2">{citations.map((citation) => <div key={citation.citationId} className="flex items-start justify-between gap-3"><div className="min-w-0"><div className="truncate font-medium text-foreground">{citation.title ?? citation.publisher ?? "Verified source"}</div><div className="mt-0.5 line-clamp-2 text-muted-foreground">{citation.excerpt}</div></div>{citation.url && citation.availability === "AVAILABLE" ? <a href={citation.url} target="_blank" rel="noreferrer" aria-label={`Open ${citation.title ?? "source"}`} className="shrink-0 text-sky-700 hover:text-sky-900"><ExternalLink className="size-3.5" /></a> : <span className="shrink-0 text-[10px] text-muted-foreground">Unlinked</span>}</div>)}</div>
}

export default function AutonomousAgentPage() {
  const [input, setInput] = useState("")
  const [run, setRun] = useState<AgentRun | null>(null)
  const [conversationMessages, setConversationMessages] = useState<AgentMessage[]>([])
  const [tools, setTools] = useState<AgentToolDefinition[]>([])
  const [conversations, setConversations] = useState<AgentConversation[]>([])
  const [historyLoading, setHistoryLoading] = useState(false)
  const [activeRunNo, setActiveRunNo] = useState<string | null>(null)
  const abortController = useRef<AbortController | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [historyQuery, setHistoryQuery] = useState("")

  useEffect(() => {
    void Promise.all([fetchAgentTools(), fetchAgentConversations()]).then(([nextTools, nextConversations]) => {
      setTools(nextTools)
      setConversations(nextConversations)
    }).catch((reason: unknown) => setError(reason instanceof Error ? reason.message : "Unable to load Agent workspace"))
  }, [])

  const toolGroups = useMemo(() => {
    const groups = new Map<string, AgentToolDefinition[]>()
    for (const tool of tools) groups.set(tool.module, [...(groups.get(tool.module) ?? []), tool])
    return [...groups.entries()]
  }, [tools])

  async function submit() {
    const userInput = input.trim()
    if (!userInput || loading) return
    setLoading(true)
    setError(null)
    try {
      const controller = new AbortController()
      abortController.current = controller
      const nextRun = await runAgentStream({ conversationNo: run?.conversationNo ?? undefined, userInput, workspace: "agent", locale: "zh-CN" }, (event) => {
        if (event.runNo) setActiveRunNo(event.runNo)
        if (event.type === "TOKEN_DELTA" && event.delta) {
          setRun((current) => ({ ...(current ?? {}), runNo: event.runNo, conversationNo: run?.conversationNo, userInput, status: "RUNNING", answer: `${current?.answer ?? ""}${event.delta}` }))
        }
      }, controller.signal)
      setRun(nextRun)
      if (nextRun.conversationNo) setConversationMessages(await fetchAgentMessages(nextRun.conversationNo))
      setConversations(await fetchAgentConversations())
      setInput("")
    } catch (reason: unknown) {
      setError(reason instanceof Error ? reason.message : "Agent run failed")
    } finally {
      abortController.current = null
      setActiveRunNo(null)
      setLoading(false)
    }
  }

  async function stopAgent() {
    if (activeRunNo) {
      try {
        const result: AgentCancellationResult = await cancelAgent(activeRunNo)
        if (result.status === "CANCEL_REQUESTED") {
          setRun((current) => current ? { ...current, status: "CANCEL_REQUESTED" } : current)
          setError("Cancellation requested. Waiting for the Agent to stop safely…")
        } else if (result.status === "ALREADY_TERMINAL") {
          const terminalRun = await getAgentRun(activeRunNo)
          setRun(terminalRun)
          setError(`This run has already finished (${terminalRun.status ?? result.runStatus ?? "terminal"}).`)
        } else {
          setError("The Agent run was not found for the current workspace.")
        }
      } catch (reason: unknown) {
        setError(reason instanceof Error ? reason.message : "Unable to stop Agent")
      }
      return
    }
    abortController.current?.abort()
  }

  async function openConversation(conversation: AgentConversation) {
    if (!conversation.lastRunNo || historyLoading) return
    setHistoryLoading(true)
    setError(null)
    try {
      const nextRun = await getAgentRun(conversation.lastRunNo)
      setRun(nextRun)
      setConversationMessages(await fetchAgentMessages(conversation.conversationNo))
    } catch (reason: unknown) {
      setError(reason instanceof Error ? reason.message : "Unable to reopen conversation")
    } finally {
      setHistoryLoading(false)
    }
  }

  async function rerun(action: "retry" | "regenerate") {
    if (!run?.runNo || loading) return
    setLoading(true)
    setError(null)
    try {
      const nextRun = action === "retry" ? await retryAgent(run.runNo) : await regenerateAgent(run.runNo)
      setRun(nextRun)
      if (nextRun.conversationNo) setConversationMessages(await fetchAgentMessages(nextRun.conversationNo))
      setConversations(await fetchAgentConversations())
    } catch (reason: unknown) {
      setError(reason instanceof Error ? reason.message : "Unable to rerun Agent")
    } finally {
      setLoading(false)
    }
  }

  async function exportResult() {
    if (!run?.runNo) return
    try {
      const result = await exportAgentRun(run.runNo)
      const url = URL.createObjectURL(new Blob([result.content], { type: result.contentType }))
      const anchor = document.createElement("a")
      anchor.href = url
      anchor.download = result.filename
      anchor.click()
      URL.revokeObjectURL(url)
    } catch (reason: unknown) {
      setError(reason instanceof Error ? reason.message : "Unable to export Agent result")
    }
  }

  return (
    <WorkspaceShell sectionLabel="Workspace AI" pageTitle="Cynexus Agent" contentScrollable={false}>
      <div className="flex h-full min-h-0 flex-col gap-5">
        <header className="flex flex-col gap-3 border-b border-border/70 pb-5 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <div className="mb-2 flex items-center gap-2 text-xs font-medium uppercase tracking-[0.16em] text-muted-foreground">
              <Sparkles className="size-3.5 text-sky-600" /> Autonomous analysis
            </div>
            <h1 className="text-2xl font-semibold tracking-tight text-foreground">Ask Cynexus to investigate</h1>
            <p className="mt-1 max-w-2xl text-sm text-muted-foreground">The Agent chooses relevant read-only data modules, records each call, and returns an evidence-backed answer.</p>
          </div>
          <div className="flex items-center gap-3 text-xs text-muted-foreground"><Wrench className="size-3.5" /> {tools.length} data tools available {run?.runNo ? <Button size="sm" variant="outline" className="h-8 gap-1.5 text-xs" onClick={() => void exportResult()}><Download className="size-3.5" />Export</Button> : null}</div>
        </header>

        {error ? <Alert variant="destructive"><AlertTitle>Agent unavailable</AlertTitle><AlertDescription>{error}</AlertDescription></Alert> : null}

        <div className="grid min-h-0 flex-1 gap-5 xl:grid-cols-[minmax(0,1fr)_20rem]">
          <section className="flex min-h-0 flex-col overflow-hidden rounded-xl border border-border/70 bg-card">
            <div className="flex min-h-0 flex-1 flex-col overflow-y-auto p-5">
              {conversationMessages.length ? <div className="mb-6 space-y-3 border-b border-border/70 pb-5"><div className="text-xs font-medium uppercase tracking-[0.14em] text-muted-foreground">Conversation transcript</div>{conversationMessages.map((message, index) => <div key={`${message.sequenceNo ?? index}-${message.role}`} className={message.role === "user" ? "ml-auto max-w-[85%] rounded-2xl bg-muted px-4 py-3 text-sm leading-6" : "max-w-3xl whitespace-pre-wrap text-sm leading-7 text-foreground"}>{message.content}</div>)}</div> : null}
              {!run ? <div className="m-auto max-w-xl text-center"><div className="mx-auto mb-4 flex size-12 items-center justify-center rounded-2xl bg-sky-50 text-sky-700"><Sparkles className="size-5" /></div><h2 className="text-lg font-semibold">Start with a question</h2><p className="mt-2 text-sm leading-6 text-muted-foreground">Try asking for a market comparison, a stock snapshot, or a data-backed explanation of today&apos;s movement.</p></div> : <div className="space-y-6"><div className="ml-auto max-w-[85%] rounded-2xl bg-muted px-4 py-3 text-sm leading-6">{run.userInput}</div><div className="max-w-3xl"><div className="mb-2 flex items-center gap-2 text-xs font-medium text-muted-foreground"><Sparkles className="size-3.5 text-sky-600" /> Cynexus Agent</div>{clarification(run) ? <div className="mb-4 rounded-lg border border-amber-300/70 bg-amber-50/70 px-4 py-3 text-sm text-amber-950"><div className="font-medium">需要补充信息</div><div className="mt-1 leading-6">{clarification(run)?.summary}</div><ul className="mt-2 list-disc space-y-1 pl-5">{clarification(run)?.questions?.map((question) => <li key={question}>{question}</li>)}</ul></div> : null}<div className="whitespace-pre-wrap text-sm leading-7 text-foreground">{run.answer ?? run.errorMessage ?? "The Agent did not return an answer."}</div>{run.runNo ? <AgentFeedbackPanel runNo={run.runNo} /> : null}{evidence(run).length ? <div className="mt-5 border-t border-border/70 pt-4"><div className="mb-2 text-xs font-medium text-muted-foreground">Evidence</div><div className="space-y-2">{evidence(run).map((item, index) => <div key={`${item.toolCode ?? "evidence"}-${index}`} className="rounded-lg border border-border/70 bg-muted/30 px-3 py-2 text-xs"><div className="flex items-center justify-between gap-2"><span className="font-medium">{item.toolCode}</span><span className="text-muted-foreground">{item.dataDate || "Date unavailable"}</span></div><div className="mt-1 text-muted-foreground">{item.summary} · {item.source}{item.sourceUrl ? <> · <a href={item.sourceUrl} target="_blank" rel="noreferrer" className="inline-flex items-center gap-1 text-sky-700 hover:underline"><ExternalLink className="size-3" />Source</a></> : null}</div>{item.citations?.length ? <CitationList citations={item.citations} /> : null}</div>)}</div></div> : null}<div className="mt-4 flex gap-2"><Button size="sm" variant="outline" onClick={() => void rerun("retry")} disabled={loading}><RefreshCw className="size-3.5" /> Retry</Button><Button size="sm" variant="ghost" onClick={() => void rerun("regenerate")} disabled={loading}><Sparkles className="size-3.5" /> Regenerate</Button></div></div></div>}
            </div>
            <div className="border-t border-border/70 p-4"><div className="rounded-xl border border-border bg-background p-2 shadow-sm"><Textarea value={input} onChange={(event) => setInput(event.target.value)} onKeyDown={(event) => { if ((event.metaKey || event.ctrlKey) && event.key === "Enter") void submit() }} placeholder="Ask about markets, stocks, sectors, or a comparison" className="min-h-24 resize-none border-0 bg-transparent px-2 py-1 text-sm shadow-none focus-visible:ring-0" /><div className="flex items-center justify-between px-2 pt-2"><span className="text-xs text-muted-foreground">Read-only research tools · Ctrl/Cmd + Enter to run</span>{loading ? <Button size="sm" variant="outline" onClick={() => void stopAgent()}><Square className="size-3.5" /> Stop</Button> : <Button size="sm" onClick={() => void submit()} disabled={!input.trim()}><ArrowUpRight className="size-4" /> Run Agent</Button>}</div></div></div>
          </section>

          <aside className="min-h-0 space-y-4 overflow-y-auto">
            <section className="rounded-xl border border-border/70 bg-card p-4">
              <div className="mb-3 flex items-center justify-between">
                <h2 className="flex items-center gap-2 text-sm font-semibold"><History className="size-3.5 text-sky-600" /> Recent conversations</h2>
                <Button variant="ghost" size="icon" className="size-7" aria-label="Refresh conversations" onClick={() => void fetchAgentConversations().then(setConversations)} disabled={historyLoading}>
                  <RefreshCw className={historyLoading ? "size-3.5 animate-spin" : "size-3.5"} />
                </Button>
              </div>
              <Input value={historyQuery} onChange={(event) => { const value = event.target.value; setHistoryQuery(value); void (value.trim() ? searchAgentConversations(value.trim()) : fetchAgentConversations()).then(setConversations) }} placeholder="Search history" className="mb-3 h-8 text-xs" />
              {conversations.length ? <div className="space-y-1.5">{conversations.map((conversation) => <button key={conversation.conversationNo} type="button" className="w-full rounded-lg border border-transparent px-3 py-2 text-left transition-colors hover:border-border hover:bg-muted/50" onClick={() => void openConversation(conversation)}>
                <div className="truncate text-xs font-medium">{conversation.lastMessage?.slice(0, 72) || conversation.title || conversation.conversationNo}</div>
                <div className="mt-1 flex items-center justify-between gap-2 text-[11px] text-muted-foreground"><span className="truncate">{conversation.conversationNo}</span><span>{conversation.updatedAt ? new Date(conversation.updatedAt).toLocaleDateString() : ""}</span></div>
              </button>)}</div> : <p className="text-xs leading-5 text-muted-foreground">Completed conversations will be available here for review and follow-up.</p>}
            </section>
            <section className="rounded-xl border border-border/70 bg-card p-4"><div className="mb-3 flex items-center justify-between"><div><h2 className="text-sm font-semibold">Run trace</h2>{run ? <div className="mt-1 text-[11px] text-muted-foreground">{run.totalTokens ?? 0} tokens · {run.usageSource ?? "ESTIMATED"} · ${(run.estimatedCost ?? 0).toFixed(6)}</div> : null}</div>{run ? <Badge variant={run.status === "SUCCEEDED" ? "secondary" : run.status === "NEEDS_CLARIFICATION" ? "outline" : "destructive"}>{run.status}</Badge> : null}</div>{run?.steps?.length ? <div className="space-y-2">{run.steps.map((step) => <details key={`${step.stepNo}-${step.toolCode ?? step.kind}`} className="group rounded-lg border border-border/70 bg-background"><summary className="flex cursor-pointer list-none items-center gap-2 px-3 py-2 text-xs"><span className={step.status === "FAILED" ? "text-destructive" : "text-emerald-600"}>{statusIcon(step.status)}</span><span className="min-w-0 flex-1 truncate font-medium">{stepLabel(step)}</span><span className="text-muted-foreground">{step.dataDate ?? ""}</span></summary><div className="border-t border-border/60 px-3 py-3 text-xs"><p className="text-muted-foreground">{step.summary}</p><pre className="mt-2 max-h-44 overflow-auto whitespace-pre-wrap rounded-md bg-muted/50 p-2 font-mono text-[10px] leading-5">{outputPreview(step)}</pre></div></details>)}</div> : <p className="text-xs leading-5 text-muted-foreground">Tool calls and source dates will appear here after a run.</p>}</section>
            <AgentWritePanel conversationNo={run?.conversationNo} runNo={run?.runNo} /><section className="rounded-xl border border-border/70 bg-card p-4"><h2 className="mb-3 text-sm font-semibold">Available modules</h2><div className="space-y-3">{toolGroups.map(([module, moduleTools]) => <div key={module}><div className="mb-1 text-[11px] font-medium uppercase tracking-[0.14em] text-muted-foreground">{module}</div>{moduleTools.map((tool) => <div key={tool.code} className="flex items-start gap-2 py-1.5"><Wrench className="mt-0.5 size-3 text-sky-600" /><div><div className="text-xs font-medium">{tool.name}</div><div className="text-[11px] leading-4 text-muted-foreground">{tool.freshness}</div></div></div>)}</div>)}</div></section>
          </aside>
        </div>
      </div>
    </WorkspaceShell>
  )
}
