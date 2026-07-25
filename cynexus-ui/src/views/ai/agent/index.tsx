import { useEffect, useMemo, useRef, useState, type ReactNode } from "react"
import { Archive, AlertCircle, ArrowUp, Check, CircleDashed, Cpu, Download, ExternalLink, PanelRight, RefreshCw, Sparkles, Square, Wrench } from "lucide-react"
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from "@/components/ui/accordion"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { notification } from "@/components/ui/notification"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { WorkspaceShell } from "@/layout/workspace-shell"
import { archiveAgentConversation, cancelAgent, exportAgentRun, fetchAgentConversations, fetchAgentMessages, fetchAgentTools, getAgentRun, regenerateAgent, retryAgent, runAgentStream, searchAgentConversations, type AgentCancellationResult, type AgentCitation, type AgentConversation, type AgentMessage, type AgentRun, type AgentRunStep, type AgentToolDefinition } from "@/services/ai/agent"
import { Input } from "@/components/ui/input"
import { InputGroup, InputGroupAddon, InputGroupButton, InputGroupTextarea } from "@/components/ui/input-group"
import { Bubble, BubbleContent } from "@/components/ui/bubble"
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from "@/components/ui/sheet"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Marker, MarkerContent, MarkerIcon } from "@/components/ui/marker"
import { Message, MessageContent, MessageFooter, MessageHeader } from "@/components/ui/message"
import { Spinner } from "@/components/ui/spinner"
import { MessageScroller, MessageScrollerButton, MessageScrollerContent, MessageScrollerItem, MessageScrollerProvider, MessageScrollerViewport } from "@/components/ui/message-scroller"
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "@/components/ui/tooltip"
import { AgentActivityTimeline, appendThinkingDelta, appendToolStep, completeThinking, timelineFromSteps, type AgentTimelineEntry } from "./components/agent-activity-timeline"
import { AgentFeedbackPanel } from "./components/agent-feedback-panel"
import { AgentMarkdown } from "./components/agent-markdown"
import { AgentWritePanel } from "./components/agent-write-panel"
import { AgentTaskPanel } from "./components/agent-task-panel"
import { AI_REASONING_EFFORTS, DEFAULT_AI_REASONING_EFFORT, fetchAiBindings, fetchAiProviders, normalizeAiReasoningEffort, type AiProviderItem, type AiReasoningEffort } from "@/services/ai/provider"

function stepLabel(step: AgentRunStep) {
  if (step.kind === "ANSWER") return "Answer"
  if (step.kind === "THINKING") return "Thought"
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

function notifyAgentError(title: string, reason: unknown, fallback: string) {
  notification.danger({
    title,
    description: reason instanceof Error ? reason.message : fallback,
  })
}

function markdownTitle(markdown?: string | null) {
  return (markdown ?? "")
    .replace(/```[\s\S]*?```/g, " ")
    .replace(/!\[([^\]]*)\]\([^)]*\)/g, "$1")
    .replace(/\[([^\]]+)\]\([^)]*\)/g, "$1")
    .replace(/^\s{0,3}#{1,6}\s*/gm, "")
    .replace(/^\s*[-*+]\s+/gm, "")
    .replace(/^\s*\d+[.)]\s+/gm, "")
    .replace(/^\s*>\s?/gm, "")
    .replace(/<[^>]*>/g, " ")
    .replace(/[|*_~`]/g, "")
    .replace(/\s+/g, " ")
    .trim()
}

function conversationLabel(conversation: AgentConversation) {
  const source = conversation.lastMessage || conversation.title || conversation.conversationNo
  return markdownTitle(source) || conversation.title || conversation.conversationNo
}

function CitationList({ citations }: { citations: AgentCitation[] }) {
  return (
    <div className="mt-2 space-y-1.5 pt-1">
      {citations.map((citation) => (
        <div key={citation.citationId} className="flex items-start justify-between gap-3">
          <div className="min-w-0">
            <div className="truncate text-[11px] font-medium text-foreground">
              {citation.title ?? citation.publisher ?? "Verified source"}
            </div>
            <div className="mt-0.5 line-clamp-2 text-[10px] leading-4 text-muted-foreground">
              {citation.excerpt}
            </div>
          </div>
          {citation.url && citation.availability === "AVAILABLE" ? (
            <a
              href={citation.url}
              target="_blank"
              rel="noreferrer"
              aria-label={`Open ${citation.title ?? "source"}`}
              className="shrink-0 text-sky-700 hover:text-sky-900"
            >
              <ExternalLink className="size-3" />
            </a>
          ) : (
            <span className="shrink-0 text-[10px] text-muted-foreground">Unlinked</span>
          )}
        </div>
      ))}
    </div>
  )
}

function AgentMessageBubble({ align, children, footer }: { align: "start" | "end"; children: ReactNode; footer?: ReactNode }) {
  const isUser = align === "end"
  return (
    <Message align={align}>
      <MessageContent className="w-full min-w-0">
        <Bubble
          variant={isUser ? "muted" : "ghost"}
          className={isUser ? "max-w-[min(100%,42rem)]" : "w-full max-w-full"}
        >
          <BubbleContent className={isUser ? undefined : "w-full max-w-full"}>
            {children}
          </BubbleContent>
        </Bubble>
        {footer ? <MessageFooter className="w-full">{footer}</MessageFooter> : null}
      </MessageContent>
    </Message>
  )
}

const TERMINAL_RUN_STATUSES = new Set([
  "SUCCEEDED",
  "FAILED",
  "NEEDS_CLARIFICATION",
  "CANCELLED",
])

function isTerminalRunStatus(status?: string | null) {
  return TERMINAL_RUN_STATUSES.has(status ?? "")
}

function hasToolCalls(message: AgentMessage) {
  const raw = message.toolCallsJson?.trim()
  return Boolean(raw && raw !== "[]" && raw !== "null")
}

function isVisibleTranscriptMessage(message: AgentMessage) {
  const content = message.content?.trim()
  if (!content) return false
  if (message.role === "user") return true
  // Intermediate model turns that requested tools often contain greetings/preambles.
  // Keep only final assistant answers in the conversation transcript.
  if (message.role === "assistant") return !hasToolCalls(message)
  return false
}

function sameMessageText(left?: string | null, right?: string | null) {
  return (left ?? "").trim() === (right ?? "").trim()
}

function providerReasoningEffort(provider?: AiProviderItem): AiReasoningEffort {
  return normalizeAiReasoningEffort(provider?.extra?.model_reasoning_effort) || DEFAULT_AI_REASONING_EFFORT
}

function removeCurrentRunFromTranscript(messages: AgentMessage[], run: AgentRun | null) {
  const visible = messages.filter(isVisibleTranscriptMessage)
  if (!run) return visible

  const duplicateIndexes = new Set<number>()
  if (run.userInput) {
    const userIndex = [...visible].reverse().findIndex(
      (message) => message.role === "user" && sameMessageText(message.content, run.userInput),
    )
    if (userIndex >= 0) duplicateIndexes.add(visible.length - 1 - userIndex)
  }
  if (run.answer) {
    const answerIndex = [...visible].reverse().findIndex(
      (message) => message.role === "assistant" && sameMessageText(message.content, run.answer),
    )
    if (answerIndex >= 0) duplicateIndexes.add(visible.length - 1 - answerIndex)
  }

  return visible.filter((_, index) => !duplicateIndexes.has(index))
}

export default function AutonomousAgentPage() {
  const [input, setInput] = useState("")
  const [run, setRun] = useState<AgentRun | null>(null)
  const [conversationMessages, setConversationMessages] = useState<AgentMessage[]>([])
  const [tools, setTools] = useState<AgentToolDefinition[]>([])
  const [providers, setProviders] = useState<AiProviderItem[]>([])
  const [defaultProviderCode, setDefaultProviderCode] = useState("")
  const [selectedProviderCode, setSelectedProviderCode] = useState("")
  const [selectedModelCode, setSelectedModelCode] = useState("")
  const [selectedReasoningEffort, setSelectedReasoningEffort] = useState<AiReasoningEffort | "">("")
  const [conversations, setConversations] = useState<AgentConversation[]>([])
  const [historyLoading, setHistoryLoading] = useState(false)
  const [activeRunNo, setActiveRunNo] = useState<string | null>(null)
  const abortController = useRef<AbortController | null>(null)
  const [loading, setLoading] = useState(false)
  const [historyQuery, setHistoryQuery] = useState("")
  const [archiveLoadingNo, setArchiveLoadingNo] = useState<string | null>(null)
  const [workspaceOpen, setWorkspaceOpen] = useState(false)
  const [workspaceTab, setWorkspaceTab] = useState("history")
  const [timeline, setTimeline] = useState<AgentTimelineEntry[]>([])
  const historyRequestId = useRef(0)

  const displayedConversationMessages = useMemo(
    () => removeCurrentRunFromTranscript(conversationMessages, run),
    [conversationMessages, run],
  )

  const runIsTerminal = isTerminalRunStatus(run?.status) && !loading
  const showAssistantBubble = Boolean(
    run && (
      Boolean(run.answer?.trim()) ||
      Boolean(run.errorMessage?.trim()) ||
      Boolean(clarification(run)) ||
      runIsTerminal
    ),
  )
  const showRunActions = Boolean(run?.runNo && runIsTerminal)

  async function refreshConversations(query = "") {
    const requestId = ++historyRequestId.current
    try {
      const nextConversations = query.trim()
        ? await searchAgentConversations(query.trim())
        : await fetchAgentConversations()
      if (requestId === historyRequestId.current) setConversations(nextConversations)
    } catch (reason: unknown) {
      if (requestId === historyRequestId.current) {
        notifyAgentError("加载 Agent 会话失败", reason, "无法加载会话记录")
      }
    }
  }

  useEffect(() => {
    let disposed = false
    async function loadWorkspace() {
      const [nextTools, nextProviders, nextBindings] = await Promise.all([
        fetchAgentTools(),
        fetchAiProviders(),
        fetchAiBindings(),
        refreshConversations(),
      ])
      if (!disposed) {
        const enabledProviders = nextProviders.filter((provider) => provider.is_enabled)
        const binding = nextBindings.find((item) => item.capability === "autonomous_agent" && item.is_enabled)
        const boundProvider = enabledProviders.find((provider) =>
          provider.code === binding?.provider?.code || provider.id === binding?.provider_id,
        )
        const boundProviderCode = boundProvider?.code || ""
        setTools(nextTools)
        setProviders(enabledProviders)
        setDefaultProviderCode(boundProviderCode)
        setSelectedProviderCode(boundProviderCode)
        setSelectedModelCode(binding?.model_override || boundProvider?.default_model || boundProvider?.models[0] || "")
        setSelectedReasoningEffort("")
      }
    }

    void loadWorkspace().catch((reason: unknown) => {
      if (!disposed) notifyAgentError("加载 Agent 工具失败", reason, "无法加载 Agent 工具")
    })
    return () => {
      disposed = true
    }
  }, [])

  const toolGroups = useMemo(() => {
    const groups = new Map<string, AgentToolDefinition[]>()
    for (const tool of tools) groups.set(tool.module, [...(groups.get(tool.module) ?? []), tool])
    return [...groups.entries()]
  }, [tools])

  const selectedProvider = providers.find((provider) => provider.code === selectedProviderCode)
    ?? providers.find((provider) => provider.code === defaultProviderCode)
  const providerDefaultReasoning = providerReasoningEffort(selectedProvider)
  const providerDisplayName = selectedProvider?.name || selectedProvider?.code || "Default provider"

  async function submit() {
    const userInput = input.trim()
    if (!userInput || loading) return
    const conversationNo = run?.conversationNo ?? undefined
    setInput("")
    setLoading(true)
    setActiveRunNo(null)
    setTimeline([])
    setRun({ conversationNo, userInput, status: "RUNNING", answer: "" })
    try {
      const controller = new AbortController()
      abortController.current = controller
      const nextRun = await runAgentStream({
        conversationNo,
        userInput,
        preferredProviderCode: selectedProviderCode || undefined,
        preferredModelCode: selectedModelCode || undefined,
        preferredReasoningEffort: selectedReasoningEffort || undefined,
        workspace: "agent",
        locale: "zh-CN",
      }, (event) => {
        if (event.runNo) setActiveRunNo(event.runNo)
        if (event.type === "THINKING_DELTA" && event.delta) {
          setTimeline((current) => appendThinkingDelta(current, event.delta || ""))
        }
        if (event.type === "THINKING") {
          setTimeline((current) => completeThinking(current, event.summary))
              }
        if (event.type === "TOOL_STEP") {
          setTimeline((current) => appendToolStep(current, {
            stepNo: event.stepNo,
            toolCode: event.toolCode,
            summary: event.summary,
            status: event.status,
          }))
        }
        if (event.type === "TOKEN_DELTA" || event.answer) {
                setTimeline((current) => completeThinking(current))
          setRun((current) => ({
            ...(current ?? {}),
            runNo: event.runNo ?? current?.runNo,
            conversationNo,
            userInput,
            status: event.type === "RUN_COMPLETED" ? event.status ?? current?.status : "RUNNING",
            answer: event.delta ? `${current?.answer ?? ""}${event.delta}` : event.answer ?? current?.answer,
          }))
        }
        if (event.type === "RUN_COMPLETED") {
                setTimeline((current) => completeThinking(current))
        }
      }, controller.signal)
      setRun(nextRun)
      setTimeline(timelineFromSteps(nextRun.steps))
        if (nextRun.conversationNo) setConversationMessages(await fetchAgentMessages(nextRun.conversationNo))
      setConversations(await fetchAgentConversations())
    } catch (reason: unknown) {
      const message = reason instanceof Error ? reason.message : "Agent run failed"
      notifyAgentError("Agent 执行失败", reason, message)
      setRun((current) => current?.userInput === userInput ? { ...current, status: "FAILED", errorMessage: message } : current)
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
          notification.info({ title: "已请求停止 Agent", description: "正在等待 Agent 安全停止" })
        } else if (result.status === "ALREADY_TERMINAL") {
          const terminalRun = await getAgentRun(activeRunNo)
          setRun(terminalRun)
          notification.warn({
            title: "Agent 已结束",
            description: `当前运行已结束（${terminalRun.status ?? result.runStatus ?? "terminal"}）`,
          })
        } else {
          notification.danger({ title: "停止 Agent 失败", description: "找不到当前工作区中的运行记录" })
        }
      } catch (reason: unknown) {
        notifyAgentError("停止 Agent 失败", reason, "无法停止 Agent")
      }
      return
    }
    abortController.current?.abort()
  }

  async function openConversation(conversation: AgentConversation) {
    if (!conversation.lastRunNo || historyLoading) return
    setHistoryLoading(true)
    try {
      const nextRun = await getAgentRun(conversation.lastRunNo)
      setRun(nextRun)
      setTimeline(timelineFromSteps(nextRun.steps))
        setConversationMessages(await fetchAgentMessages(conversation.conversationNo))
      setWorkspaceOpen(false)
    } catch (reason: unknown) {
      notifyAgentError("打开 Agent 会话失败", reason, "无法重新打开会话")
    } finally {
      setHistoryLoading(false)
    }
  }

  async function archiveConversation(conversation: AgentConversation) {
    if (archiveLoadingNo) return
    setArchiveLoadingNo(conversation.conversationNo)
    try {
      const archived = await archiveAgentConversation(conversation.conversationNo)
      if (!archived) throw new Error("Conversation is no longer available")
      setConversations((current) => current.filter((item) => item.conversationNo !== conversation.conversationNo))
      if (run?.conversationNo === conversation.conversationNo) {
        setRun(null)
        setConversationMessages([])
        setTimeline([])
      }
      notification.success({ title: "会话已归档" })
    } catch (reason: unknown) {
      notifyAgentError("归档会话失败", reason, "无法归档此会话")
    } finally {
      setArchiveLoadingNo(null)
    }
  }

  async function rerun(action: "retry" | "regenerate") {
    if (!run?.runNo || loading) return
    setLoading(true)
    try {
      setRun((current) => current ? { ...current, status: "RUNNING" } : current)
      const nextRun = action === "retry" ? await retryAgent(run.runNo) : await regenerateAgent(run.runNo)
      setRun(nextRun)
      if (nextRun.conversationNo) setConversationMessages(await fetchAgentMessages(nextRun.conversationNo))
      setConversations(await fetchAgentConversations())
    } catch (reason: unknown) {
      const message = reason instanceof Error ? reason.message : "Unable to rerun Agent"
      notifyAgentError("重新运行 Agent 失败", reason, message)
      setRun((current) => current ? { ...current, status: "FAILED", errorMessage: message } : current)
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
      notifyAgentError("导出 Agent 结果失败", reason, "无法导出 Agent 结果")
    }
  }

  return (
    <WorkspaceShell
      sectionLabel="Workspace AI"
      pageTitle="Cynexus Agent"
      contentScrollable={false}
      headerActions={
        <div className="flex items-center gap-1.5">
          {run?.runNo ? (
            <Button
              size="sm"
              variant="ghost"
              className="h-8 gap-1.5 text-xs"
              onClick={() => void exportResult()}
            >
              <Download className="size-3.5" />
              Export
            </Button>
          ) : null}
          <Button
            size="sm"
            variant="secondary"
            className="h-8 gap-1.5 text-xs"
            onClick={() => {
              setWorkspaceTab("history")
              setWorkspaceOpen(true)
              void refreshConversations(historyQuery)
            }}
          >
            <PanelRight className="size-3.5" />
            Workspace
          </Button>
        </div>
      }
    >
      <div className="flex h-full min-h-0 flex-col gap-3">
        <Sheet open={workspaceOpen} onOpenChange={setWorkspaceOpen}>
          <SheetContent side="right" className="flex w-full flex-col gap-0 p-0 sm:max-w-xl">
            <SheetHeader className="border-b border-border/70 px-4 py-4">
              <SheetTitle className="flex items-center gap-2 text-sm">
                <PanelRight className="size-3.5 text-sky-600" />
                Agent workspace
              </SheetTitle>
              <SheetDescription className="text-xs">
                History, run trace, write confirmation, scheduled tasks, and available modules.
              </SheetDescription>
            </SheetHeader>
            <Tabs
              value={workspaceTab}
              onValueChange={setWorkspaceTab}
              className="flex min-h-0 flex-1 flex-col gap-0"
            >
              <div className="border-b border-border/70 px-4 py-2">
                <TabsList variant="line" className="h-auto w-full flex-wrap justify-start gap-1 bg-transparent p-0">
                  <TabsTrigger value="history" className="h-8 px-2.5 text-xs">History</TabsTrigger>
                  <TabsTrigger value="trace" className="h-8 px-2.5 text-xs">Trace</TabsTrigger>
                  <TabsTrigger value="write" className="h-8 px-2.5 text-xs">Write</TabsTrigger>
                  <TabsTrigger value="tasks" className="h-8 px-2.5 text-xs">Tasks</TabsTrigger>
                  <TabsTrigger value="modules" className="h-8 px-2.5 text-xs">Modules</TabsTrigger>
                </TabsList>
              </div>
              <div className="min-h-0 flex-1 overflow-y-auto p-4">
                <TabsContent value="history" className="mt-0 space-y-3">
                  <div className="flex items-center gap-2">
                    <Input
                      value={historyQuery}
                      onChange={(event) => {
                        const value = event.target.value
                        setHistoryQuery(value)
                        void refreshConversations(value)
                      }}
                      placeholder="Search history"
                      className="h-8 text-xs"
                    />
                    <Button
                      variant="ghost"
                      size="icon"
                      className="size-8 shrink-0"
                      aria-label="Refresh conversations"
                      onClick={() => void refreshConversations(historyQuery)}
                      disabled={historyLoading}
                    >
                      <RefreshCw className={historyLoading ? "size-3.5 animate-spin" : "size-3.5"} />
                    </Button>
                  </div>
                  <div className="space-y-1.5">
                    {conversations.length ? (
                      conversations.map((conversation) => (
                        <div
                          key={conversation.conversationNo}
                          className="group flex items-center gap-1 rounded-lg border border-transparent px-1 transition-colors hover:border-border hover:bg-muted/50 focus-within:border-border focus-within:bg-muted/50"
                        >
                          <button
                            type="button"
                            className="min-w-0 flex-1 rounded-md px-2 py-2 text-left outline-none"
                            onClick={() => void openConversation(conversation)}
                          >
                            <div className="line-clamp-2 break-words text-xs font-medium leading-5">
                              {conversationLabel(conversation)}
                            </div>
                            <div className="mt-1 flex items-center justify-between gap-2 text-[11px] text-muted-foreground">
                              <span className="truncate">{conversation.conversationNo}</span>
                              <span className="shrink-0">{conversation.updatedAt ? new Date(conversation.updatedAt).toLocaleDateString() : ""}</span>
                            </div>
                          </button>
                          <TooltipProvider>
                            <Tooltip>
                              <TooltipTrigger asChild>
                                <Button
                                  type="button"
                                  variant="ghost"
                                  size="icon"
                                  className="size-8 shrink-0 opacity-0 transition-opacity group-hover:opacity-100 group-focus-within:opacity-100 focus-visible:opacity-100"
                                  aria-label="Archive conversation"
                                  title="Archive conversation"
                                  onClick={() => void archiveConversation(conversation)}
                                  disabled={archiveLoadingNo !== null}
                                >
                                  <Archive className={archiveLoadingNo === conversation.conversationNo ? "size-3.5 animate-pulse" : "size-3.5"} />
                                </Button>
                              </TooltipTrigger>
                              <TooltipContent>Archive conversation</TooltipContent>
                            </Tooltip>
                          </TooltipProvider>
                        </div>
                      ))
                    ) : (
                      <p className="px-1 text-xs leading-5 text-muted-foreground">
                        Completed conversations will be available here for review and follow-up.
                      </p>
                    )}
                  </div>
                </TabsContent>

                <TabsContent value="trace" className="mt-0 space-y-3">
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <h3 className="text-sm font-semibold">Run trace</h3>
                      {run ? (
                        <div className="mt-1 text-[11px] text-muted-foreground">
                          {run.totalTokens ?? 0} tokens · {run.usageSource ?? "ESTIMATED"} · ${(run.estimatedCost ?? 0).toFixed(6)}
                        </div>
                      ) : null}
                    </div>
                    {run ? (
                      <Badge variant={run.status === "SUCCEEDED" ? "secondary" : run.status === "NEEDS_CLARIFICATION" ? "outline" : "destructive"}>
                        {run.status}
                      </Badge>
                    ) : null}
                  </div>
                  {run?.steps?.length ? (
                    <div className="space-y-2">
                      {run.steps.map((step) => (
                        <details key={`${step.stepNo}-${step.toolCode ?? step.kind}`} className="group rounded-lg border border-border/70 bg-background">
                          <summary className="flex cursor-pointer list-none items-center gap-2 px-3 py-2 text-xs">
                            <span className={step.status === "FAILED" ? "text-destructive" : "text-emerald-600"}>{statusIcon(step.status)}</span>
                            <span className="min-w-0 flex-1 truncate font-medium">{stepLabel(step)}</span>
                            <span className="text-muted-foreground">{step.dataDate ?? ""}</span>
                          </summary>
                          <div className="border-t border-border/60 px-3 py-3 text-xs">
                            <p className="text-muted-foreground">{step.summary}</p>
                            <pre className="mt-2 max-h-44 overflow-auto whitespace-pre-wrap rounded-md bg-muted/50 p-2 font-mono text-[10px] leading-5">{outputPreview(step)}</pre>
                          </div>
                        </details>
                      ))}
                    </div>
                  ) : (
                    <p className="text-xs leading-5 text-muted-foreground">Tool calls and source dates will appear here after a run.</p>
                  )}
                </TabsContent>

                <TabsContent value="write" className="mt-0">
                  <AgentWritePanel conversationNo={run?.conversationNo} runNo={run?.runNo} />
                </TabsContent>

                <TabsContent value="tasks" className="mt-0">
                  <AgentTaskPanel />
                </TabsContent>

                <TabsContent value="modules" className="mt-0 space-y-3">
                  <h3 className="text-sm font-semibold">Available modules</h3>
                  <div className="space-y-3">
                    {toolGroups.map(([module, moduleTools]) => (
                      <div key={module}>
                        <div className="mb-1 text-[11px] font-medium uppercase tracking-[0.14em] text-muted-foreground">{module}</div>
                        {moduleTools.map((tool) => (
                          <div key={tool.code} className="flex items-start gap-2 py-1.5">
                            <Wrench className="mt-0.5 size-3 text-sky-600" />
                            <div>
                              <div className="text-xs font-medium">{tool.name}</div>
                              <div className="text-[11px] leading-4 text-muted-foreground">{tool.freshness}</div>
                            </div>
                          </div>
                        ))}
                      </div>
                    ))}
                  </div>
                </TabsContent>
              </div>
            </Tabs>
          </SheetContent>
        </Sheet>

        <div className="min-h-0 flex-1">
          <section className="flex h-full min-h-0 flex-col overflow-hidden bg-transparent">
            <div className="flex min-h-0 flex-1 flex-col overflow-hidden px-40 py-5 md:px-64 lg:px-96">
              <MessageScrollerProvider autoScroll defaultScrollPosition="last-anchor" scrollPreviousItemPeek={48}>
                <MessageScroller className="min-h-0 flex-1">
                  <MessageScrollerViewport aria-label="Agent messages" className="scrollbar-none">
                    <MessageScrollerContent className="gap-6 py-2" aria-busy={loading}>
                      {displayedConversationMessages.map((message, index) => (
                        <MessageScrollerItem key={`${message.sequenceNo ?? index}-${message.role}`} messageId={`transcript-${message.sequenceNo ?? index}`}>
                          <AgentMessageBubble align={message.role === "user" ? "end" : "start"}>
                            {message.role === "assistant" && message.content
                              ? <AgentMarkdown markdown={message.content} />
                              : message.content}
                          </AgentMessageBubble>
                        </MessageScrollerItem>
                      ))}
                      {!run && !loading && !displayedConversationMessages.length ? <MessageScrollerItem messageId="agent-empty"><div className="flex min-h-[18rem] items-center justify-center"><div className="max-w-xl text-center"><div className="mx-auto mb-4 flex size-12 items-center justify-center rounded-2xl bg-sky-50 text-sky-700"><Sparkles className="size-5" /></div><h2 className="text-lg font-semibold">Start with a question</h2><p className="mt-2 text-sm leading-6 text-muted-foreground">Try asking for a market comparison, a stock snapshot, or a data-backed explanation of today&apos;s movement.</p></div></div></MessageScrollerItem> : null}
                      {run ? <>
                        <MessageScrollerItem messageId={`run-user-${run.runNo ?? activeRunNo ?? "pending"}`} scrollAnchor>
                          <AgentMessageBubble align="end">{run.userInput}</AgentMessageBubble>
                        </MessageScrollerItem>
                        {timeline.length ? (
                          <MessageScrollerItem messageId="agent-activity">
                            <AgentActivityTimeline entries={timeline} />
                          </MessageScrollerItem>
                        ) : loading && !showAssistantBubble ? (
                          <MessageScrollerItem messageId="agent-awaiting" scrollAnchor>
                            <Message>
                              <MessageContent className="gap-1.5">
                                <Marker role="status" className="text-xs">
                                  <MarkerIcon>
                                    <Spinner className="size-3.5" />
                                  </MarkerIcon>
                                  <MarkerContent className="shimmer">Awaiting…</MarkerContent>
                                </Marker>
                              </MessageContent>
                            </Message>
                          </MessageScrollerItem>
                        ) : null}
                        {showAssistantBubble ? (
                          <MessageScrollerItem messageId={`run-assistant-${run.runNo ?? activeRunNo ?? "pending"}`}>
                            <Message>
                              <MessageContent className="w-full min-w-0">
                                <MessageHeader className="w-full">
                                  <Sparkles className="mr-1.5 size-3.5 text-sky-600" />
                                  Cynexus Agent
                                </MessageHeader>
                                <Bubble variant="ghost" className="w-full max-w-full">
                                  <BubbleContent className="w-full max-w-full">
                                    {clarification(run) ? (
                                      <div className="rounded-lg border border-amber-300/70 bg-amber-50/70 px-4 py-3 text-sm text-amber-950">
                                        <div className="font-medium">需要补充信息</div>
                                        <div className="mt-1 leading-6">{clarification(run)?.summary}</div>
                                        <ul className="mt-2 list-disc space-y-1 pl-5">
                                          {clarification(run)?.questions?.map((question) => (
                                            <li key={question}>{question}</li>
                                          ))}
                                        </ul>
                                      </div>
                                    ) : null}
                                    <div aria-live={loading ? "polite" : undefined} className="w-full min-w-0 text-sm leading-7 text-foreground">
                                      {run.errorMessage && !run.answer ? (
                                        <div className="whitespace-pre-wrap">{run.errorMessage}</div>
                                      ) : run.answer ? (
                                        <AgentMarkdown markdown={run.answer} />
                                      ) : (
                                        <div className="whitespace-pre-wrap">The Agent did not return an answer.</div>
                                      )}
                                    </div>
                                    {showRunActions ? (
                                      <AgentFeedbackPanel
                                        runNo={run.runNo!}
                                        actions={
                                          <>
                                            <Button size="sm" variant="ghost" className="h-7 gap-1 rounded-md px-2 text-[11px]" onClick={() => void rerun("retry")} disabled={loading}>
                                              <RefreshCw className="size-3.5" /> Retry
                                            </Button>
                                            <Button size="sm" variant="ghost" className="h-7 gap-1 rounded-md px-2 text-[11px]" onClick={() => void rerun("regenerate")} disabled={loading}>
                                              <Sparkles className="size-3.5" /> Regenerate
                                            </Button>
                                          </>
                                        }
                                      />
                                    ) : null}
                                    {evidence(run).length ? (
                                      <div className="mt-5 w-full min-w-0 pt-3">
                                        <Accordion type="single" collapsible defaultValue="" className="w-full px-0">
                                          <AccordionItem value="evidence" className="border-none">
                                            <AccordionTrigger className="mb-1.5 py-1.5 hover:no-underline">
                                              <span className="text-[11px] font-medium tracking-wide text-muted-foreground">
                                                Evidence
                                              </span>
                                            </AccordionTrigger>
                                            <AccordionContent className="text-[11px] leading-5 text-muted-foreground">
                                              <Accordion
                                                type="multiple"
                                                defaultValue={[]}
                                                className="w-full px-0"
                                              >
                                                {evidence(run).map((item, index) => {
                                                  const value = `${item.toolCode ?? "evidence"}-${index}`
                                                  return (
                                                    <AccordionItem key={value} value={value} className="border-none">
                                                      <AccordionTrigger className="py-2 hover:no-underline">
                                                        <div className="min-w-0 flex-1 pr-2">
                                                          <div className="flex items-center justify-between gap-2">
                                                            <span className="truncate text-[11px] font-medium text-foreground">
                                                              {item.toolCode || "Evidence"}
                                                            </span>
                                                            <span className="shrink-0 text-[10px] font-normal text-muted-foreground">
                                                              {item.dataDate || "Date unavailable"}
                                                            </span>
                                                          </div>
                                                          <div className="mt-0.5 line-clamp-1 text-[10px] font-normal leading-4 text-muted-foreground">
                                                            {item.summary || item.source || "No summary"}
                                                          </div>
                                                        </div>
                                                      </AccordionTrigger>
                                                      <AccordionContent className="text-[11px] leading-5 text-muted-foreground">
                                                        <div className="space-y-1.5">
                                                          {item.summary ? (
                                                            <p className="text-[11px] leading-5 text-muted-foreground">{item.summary}</p>
                                                          ) : null}
                                                          <div className="flex flex-wrap items-center gap-x-2 gap-y-1 text-[10px] text-muted-foreground">
                                                            {item.source ? <span>{item.source}</span> : null}
                                                            {item.sourceUrl ? (
                                                              <a
                                                                href={item.sourceUrl}
                                                                target="_blank"
                                                                rel="noreferrer"
                                                                className="inline-flex items-center gap-1 text-sky-700 hover:underline"
                                                              >
                                                                <ExternalLink className="size-3" />
                                                                Source
                                                              </a>
                                                            ) : null}
                                                          </div>
                                                          {item.citations?.length ? <CitationList citations={item.citations} /> : null}
                                                        </div>
                                                      </AccordionContent>
                                                    </AccordionItem>
                                                  )
                                                })}
                                              </Accordion>
                                            </AccordionContent>
                                          </AccordionItem>
                                        </Accordion>
                                      </div>
                                    ) : null}
                                  </BubbleContent>
                                </Bubble>

                              </MessageContent>
                            </Message>
                          </MessageScrollerItem>
                        ) : null}
                      </> : null}
                    </MessageScrollerContent>
                  </MessageScrollerViewport>
                  <MessageScrollerButton />
                </MessageScroller>
              </MessageScrollerProvider>
            </div>
            <div className="px-40 pb-5 pt-2 md:px-64 lg:px-96">
              <form onSubmit={(event) => { event.preventDefault(); void submit() }}>
                <InputGroup className="h-auto min-h-0 flex-col items-stretch">
                  <InputGroupTextarea value={input} onChange={(event) => setInput(event.target.value)} onKeyDown={(event) => { if (event.key !== "Enter" || event.nativeEvent.isComposing || event.ctrlKey || event.metaKey) return; event.preventDefault(); void submit() }} placeholder="Ask about markets, stocks, sectors, or a comparison" aria-label="Ask Cynexus Agent" rows={3} className="!block h-20 min-h-20 max-h-20 min-w-0 max-w-full flex-none resize-none overflow-x-hidden overflow-y-auto whitespace-pre-wrap break-words text-left leading-6 scrollbar-none [&::-webkit-scrollbar]:hidden" />
                  <InputGroupAddon align="block-end" className="flex-wrap">
                    <span className="px-1 text-xs text-muted-foreground">Enter 发送 · Ctrl + Enter 换行</span>
                    <Select value={selectedProviderCode || "auto"} onValueChange={(value) => { const nextProviderCode = value === "auto" ? "" : value; const nextProvider = providers.find((provider) => provider.code === nextProviderCode); setSelectedProviderCode(nextProviderCode); setSelectedModelCode(nextProvider?.default_model || nextProvider?.models[0] || ""); setSelectedReasoningEffort("") }} disabled={loading}>
                      <SelectTrigger size="sm" className="h-7 max-w-[11rem] border-0 bg-transparent px-2 text-xs shadow-none"><Cpu className="size-3.5" /><SelectValue placeholder="Provider">{providerDisplayName}</SelectValue></SelectTrigger>
                      <SelectContent side="top" align="start"><SelectItem value="auto">Default provider</SelectItem>{providers.map((provider) => <SelectItem key={provider.code} value={provider.code}>{provider.name}</SelectItem>)}</SelectContent>
                    </Select>
                    {selectedProviderCode ? <Select value={selectedModelCode || "auto"} onValueChange={(value) => setSelectedModelCode(value === "auto" ? "" : value)} disabled={loading}>
                      <SelectTrigger size="sm" className="h-7 max-w-[13rem] border-0 bg-transparent px-2 text-xs shadow-none"><SelectValue placeholder="Model" /></SelectTrigger>
                      <SelectContent side="top" align="start"><SelectItem value="auto">Provider default</SelectItem>{[...new Set([selectedModelCode, providers.find((provider) => provider.code === selectedProviderCode)?.default_model || "", ...(providers.find((provider) => provider.code === selectedProviderCode)?.models || [])].filter(Boolean))].map((model) => <SelectItem key={model} value={model}>{model}</SelectItem>)}</SelectContent>
                    </Select> : null}
                    <Select value={selectedReasoningEffort || "provider-default"} onValueChange={(value) => setSelectedReasoningEffort((value === "provider-default" ? "" : value) as AiReasoningEffort | "")} disabled={loading}>
                      <SelectTrigger size="sm" title={selectedReasoningEffort || providerDefaultReasoning} className="h-7 min-w-[6rem] shrink-0 border-0 bg-transparent px-2 text-xs shadow-none"><Sparkles className="size-3.5" /><SelectValue placeholder="Reasoning">{selectedReasoningEffort || providerDefaultReasoning}</SelectValue></SelectTrigger>
                      <SelectContent side="top" align="start">
                        <SelectItem value="provider-default">{providerDefaultReasoning ? `Provider default (${providerDefaultReasoning})` : "Provider default"}</SelectItem>
                        {AI_REASONING_EFFORTS.map((effort) => <SelectItem key={effort} value={effort}>{effort}</SelectItem>)}
                      </SelectContent>
                    </Select>
                    {loading ? <InputGroupButton type="button" variant="outline" size="icon-sm" aria-label="Stop Agent" title="Stop Agent" onClick={() => { void stopAgent() }} className="ml-auto border-destructive/30 bg-destructive/5 text-destructive hover:border-destructive/40 hover:bg-destructive/10 hover:text-destructive focus-visible:ring-destructive/30"><Square className="size-3.5" fill="currentColor" /></InputGroupButton> : <InputGroupButton type="submit" variant="default" size="icon-sm" aria-label="Run Agent" title="Run Agent" className="ml-auto" disabled={!input.trim()}><ArrowUp /></InputGroupButton>}
                  </InputGroupAddon>
                </InputGroup>
              </form>
            </div>
          </section>

        </div>
      </div>
    </WorkspaceShell>
  )
}
