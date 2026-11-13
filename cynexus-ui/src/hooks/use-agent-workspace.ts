import { useCallback, useEffect, useMemo, useRef, useState } from "react"

import { bindingReasoningEffort, clarification, isTerminalRunStatus, providerReasoningEffort, removeCurrentRunFromTranscript } from "@/lib/agent/model"
import { appendRetry, appendThinkingDelta, appendToolStep, completeThinking, timelineFromSteps, type AgentTimelineEntry } from "@/lib/agent/timeline"
import { archiveAgentConversation, cancelAgent, exportAgentRun, fetchAgentConversations, fetchAgentMessages, fetchAgentTools, getAgentRun, regenerateAgent, retryAgent, runAgentStream, searchAgentConversations, type AgentConversation, type AgentMessage, type AgentRun, type AgentToolDefinition } from "@/services/ai/agent"
import { DEFAULT_AI_REASONING_EFFORT, fetchAiBindings, fetchAiProviders, type AiProviderItem, type AiReasoningEffort } from "@/services/ai/provider"

export interface AgentNotice {
  tone: "success" | "info" | "warn" | "danger"
  title: string
  description?: string
}

interface AgentWorkspaceOptions {
  locale?: string
  onNotice?: (notice: AgentNotice) => void
}

/** Shared conversation/execution controller. Presentation stays with each UI. */
export function useAgentWorkspace({ locale = "zh-CN", onNotice }: AgentWorkspaceOptions = {}) {
  const [input, setInput] = useState("")
  const [run, setRun] = useState<AgentRun | null>(null)
  const [conversationMessages, setConversationMessages] = useState<AgentMessage[]>([])
  const [transcriptRunNo, setTranscriptRunNo] = useState<string | null>(null)
  const [tools, setTools] = useState<AgentToolDefinition[]>([])
  const [providers, setProviders] = useState<AiProviderItem[]>([])
  const [defaultProviderCode, setDefaultProviderCode] = useState("")
  const [bindingDefaultReasoning, setBindingDefaultReasoning] = useState<AiReasoningEffort>(DEFAULT_AI_REASONING_EFFORT)
  const [selectedProviderCode, setSelectedProviderCode] = useState("")
  const [selectedModelCode, setSelectedModelCode] = useState("")
  const [selectedReasoningEffort, setSelectedReasoningEffort] = useState<AiReasoningEffort | "">("")
  const [conversations, setConversations] = useState<AgentConversation[]>([])
  const [historyRefreshing, setHistoryRefreshing] = useState(false)
  const [conversationLoading, setConversationLoading] = useState(false)
  const [historyError, setHistoryError] = useState<string | null>(null)
  const [activeRunNo, setActiveRunNo] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [executionMode, setExecutionMode] = useState<"stream" | "rerun" | null>(null)
  const [historyQuery, setHistoryQuery] = useState("")
  const [archiveLoadingNo, setArchiveLoadingNo] = useState<string | null>(null)
  const [workspaceOpen, setWorkspaceOpen] = useState(false)
  const [workspaceTab, setWorkspaceTab] = useState("history")
  const [timeline, setTimeline] = useState<AgentTimelineEntry[]>([])
  const [configLoading, setConfigLoading] = useState(true)
  const [configError, setConfigError] = useState<string | null>(null)
  const [notice, setNotice] = useState<AgentNotice | null>(null)
  const mounted = useRef(false)
  const abortController = useRef<AbortController | null>(null)
  const operationBusy = useRef(false)
  const openingConversation = useRef(false)
  const archivingConversation = useRef(false)
  const operationId = useRef(0)
  const historyRequestId = useRef(0)
  const configRequestId = useRef(0)
  const onNoticeRef = useRef(onNotice)

  useEffect(() => { onNoticeRef.current = onNotice }, [onNotice])
  useEffect(() => {
    mounted.current = true
    return () => {
      mounted.current = false
      operationId.current += 1
      historyRequestId.current += 1
      configRequestId.current += 1
      abortController.current?.abort()
    }
  }, [])

  const publishNotice = useCallback((nextNotice: AgentNotice) => {
    if (!mounted.current) return
    setNotice(nextNotice)
    onNoticeRef.current?.(nextNotice)
  }, [])

  const reportError = useCallback((title: string, reason: unknown, fallback: string) => {
    const description = reason instanceof Error ? reason.message : fallback
    publishNotice({ tone: "danger", title, description })
    return description
  }, [publishNotice])

  const refreshConversations = useCallback(async (query = "") => {
    const requestId = ++historyRequestId.current
    setHistoryRefreshing(true)
    setHistoryError(null)
    try {
      const next = query.trim() ? await searchAgentConversations(query.trim()) : await fetchAgentConversations()
      if (mounted.current && requestId === historyRequestId.current) setConversations(next)
    } catch (reason: unknown) {
      if (mounted.current && requestId === historyRequestId.current) {
        setHistoryError(reportError("Unable to load conversations", reason, "Conversation history is unavailable."))
      }
    } finally {
      if (mounted.current && requestId === historyRequestId.current) setHistoryRefreshing(false)
    }
  }, [reportError])

  const reloadWorkspace = useCallback(async () => {
    const requestId = ++configRequestId.current
    setConfigLoading(true)
    setConfigError(null)
    try {
      const [nextTools, nextProviders, nextBindings] = await Promise.all([
        fetchAgentTools(), fetchAiProviders(), fetchAiBindings(), refreshConversations(),
      ])
      if (!mounted.current || requestId !== configRequestId.current) return
      const enabledProviders = nextProviders.filter(provider => provider.is_enabled)
      const binding = nextBindings.find(item => item.capability === "autonomous_agent" && item.is_enabled)
      const boundProvider = enabledProviders.find(provider => provider.code === binding?.provider?.code || provider.id === binding?.provider_id)
      const boundProviderCode = boundProvider?.code || ""
      setTools(nextTools)
      setProviders(enabledProviders)
      setDefaultProviderCode(boundProviderCode)
      setSelectedProviderCode(boundProviderCode)
      setSelectedModelCode(binding?.model_override || boundProvider?.default_model || boundProvider?.models[0] || "")
      setBindingDefaultReasoning(bindingReasoningEffort(binding?.params) || providerReasoningEffort(boundProvider))
      setSelectedReasoningEffort("")
    } catch (reason: unknown) {
      if (mounted.current && requestId === configRequestId.current) {
        setConfigError(reportError("Unable to load Agent", reason, "Agent configuration is unavailable."))
      }
    } finally {
      if (mounted.current && requestId === configRequestId.current) setConfigLoading(false)
    }
  }, [refreshConversations, reportError])

  useEffect(() => {
    let disposed = false
    void Promise.resolve().then(() => { if (!disposed) void reloadWorkspace() })
    return () => { disposed = true }
  }, [reloadWorkspace])

  const displayedConversationMessages = useMemo(
    () => removeCurrentRunFromTranscript(conversationMessages, run, Boolean(run?.runNo && transcriptRunNo === run.runNo)),
    [conversationMessages, run, transcriptRunNo],
  )
  const runIsTerminal = isTerminalRunStatus(run?.status) && !loading
  const showAssistantBubble = Boolean(run && (run.answer?.trim() || run.errorMessage?.trim() || clarification(run) || runIsTerminal))
  const showRunActions = Boolean(run?.runNo && runIsTerminal)
  const toolGroups = useMemo(() => {
    const groups = new Map<string, AgentToolDefinition[]>()
    for (const tool of tools) groups.set(tool.module, [...(groups.get(tool.module) ?? []), tool])
    return [...groups.entries()]
  }, [tools])
  const selectedProvider = providers.find(provider => provider.code === selectedProviderCode) ?? providers.find(provider => provider.code === defaultProviderCode)
  const providerDefaultReasoning = !selectedProviderCode || selectedProviderCode === defaultProviderCode ? bindingDefaultReasoning : providerReasoningEffort(selectedProvider)
  const providerDisplayName = selectedProvider?.name || selectedProvider?.code || "Default provider"
  const displayedModelCode = selectedModelCode || run?.modelCode?.trim() || selectedProvider?.default_model || selectedProvider?.models[0] || ""
  const isCurrentOperation = (id: number) => mounted.current && operationId.current === id

  async function loadCompletedConversation(nextRun: AgentRun, id: number) {
    if (nextRun.conversationNo) {
      try {
        const messages = await fetchAgentMessages(nextRun.conversationNo)
        if (isCurrentOperation(id)) {
          setConversationMessages(messages)
          setTranscriptRunNo(nextRun.runNo ?? null)
        }
      } catch (reason: unknown) {
        if (isCurrentOperation(id)) reportError("Unable to refresh conversation", reason, "The result is available, but conversation history could not be refreshed.")
      }
    }
    if (isCurrentOperation(id)) await refreshConversations(historyQuery)
  }

  async function submit() {
    const userInput = input.trim()
    if (!userInput || operationBusy.current || openingConversation.current || archivingConversation.current) return
    operationBusy.current = true
    const id = ++operationId.current
    const conversationNo = run?.conversationNo ?? undefined
    const controller = new AbortController()
    const handledEventSequences = new Set<number>()
    abortController.current = controller
    setInput("")
    setNotice(null)
    setLoading(true)
    setExecutionMode("stream")
    setActiveRunNo(null)
    setTimeline([])
    setRun({ conversationNo, userInput, status: "RUNNING", answer: "" })
    try {
      const nextRun = await runAgentStream({
        conversationNo,
        userInput,
        preferredProviderCode: selectedProviderCode || undefined,
        preferredModelCode: selectedModelCode || undefined,
        preferredReasoningEffort: selectedReasoningEffort || undefined,
        workspace: "agent",
        locale,
      }, event => {
        if (!isCurrentOperation(id)) return
        if (event.sequenceNo != null) {
          if (handledEventSequences.has(event.sequenceNo)) return
          handledEventSequences.add(event.sequenceNo)
        }
        if (event.runNo) setActiveRunNo(event.runNo)
        if (event.type === "THINKING_DELTA" && event.delta) setTimeline(current => appendThinkingDelta(current, event.delta || ""))
        if (event.type === "THINKING") setTimeline(current => completeThinking(current, event.summary))
        if (event.type === "TOOL_STEP") setTimeline(current => appendToolStep(current, event))
        if (event.type === "RETRYING") setTimeline(current => appendRetry(current, event))
        if (event.type === "TOKEN_DELTA" || event.answer) {
          setTimeline(current => completeThinking(current))
          setRun(current => ({
            ...(current ?? {}),
            runNo: event.runNo ?? current?.runNo,
            conversationNo,
            userInput,
            status: event.type === "RUN_COMPLETED" ? event.status ?? current?.status : "RUNNING",
            answer: event.delta ? `${current?.answer ?? ""}${event.delta}` : event.answer ?? current?.answer,
          }))
        }
        if (event.type === "RUN_COMPLETED") setTimeline(current => completeThinking(current))
      }, controller.signal)
      if (!isCurrentOperation(id)) return
      setRun(nextRun)
      if (nextRun.modelCode?.trim()) setSelectedModelCode(nextRun.modelCode.trim())
      setTimeline(current => current.length ? current : timelineFromSteps(nextRun.steps))
      await loadCompletedConversation(nextRun, id)
    } catch (reason: unknown) {
      if (!isCurrentOperation(id)) return
      if (controller.signal.aborted) {
        setRun(current => current ? { ...current, status: "CANCELLED" } : current)
        setTimeline(current => completeThinking(current))
      } else {
        const message = reportError("Agent run failed", reason, "Agent run failed.")
        setRun(current => current ? { ...current, status: "FAILED", errorMessage: message } : current)
      }
    } finally {
      if (isCurrentOperation(id)) {
        abortController.current = null
        operationBusy.current = false
        setActiveRunNo(null)
        setLoading(false)
        setExecutionMode(null)
      }
    }
  }

  async function stopAgent() {
    const id = operationId.current
    if (!operationBusy.current) return
    if (!activeRunNo) { abortController.current?.abort(); return }
    try {
      const result = await cancelAgent(activeRunNo)
      if (!isCurrentOperation(id)) return
      if (result.status === "CANCEL_REQUESTED") {
        setRun(current => current ? { ...current, status: "CANCEL_REQUESTED" } : current)
        publishNotice({ tone: "info", title: "Stop requested", description: "Waiting for the Agent to stop safely." })
      } else if (result.status === "ALREADY_TERMINAL") {
        const terminalRun = await getAgentRun(activeRunNo)
        if (!isCurrentOperation(id)) return
        setRun(terminalRun)
        publishNotice({ tone: "warn", title: "Agent already finished", description: `Run status: ${terminalRun.status ?? result.runStatus ?? "terminal"}.` })
      } else {
        publishNotice({ tone: "danger", title: "Unable to stop Agent", description: "The current run is no longer available." })
      }
    } catch (reason: unknown) {
      if (isCurrentOperation(id)) reportError("Unable to stop Agent", reason, "The stop request could not be completed.")
    }
  }

  async function openConversation(conversation: AgentConversation) {
    if (!conversation.lastRunNo || openingConversation.current || operationBusy.current || archivingConversation.current) return
    openingConversation.current = true
    const id = ++operationId.current
    setConversationLoading(true)
    setNotice(null)
    try {
      const [nextRun, messages] = await Promise.all([getAgentRun(conversation.lastRunNo), fetchAgentMessages(conversation.conversationNo)])
      if (!isCurrentOperation(id)) return
      setRun(nextRun)
      setConversationMessages(messages)
      setTranscriptRunNo(nextRun.runNo ?? null)
      if (nextRun.modelCode?.trim()) setSelectedModelCode(nextRun.modelCode.trim())
      setTimeline(timelineFromSteps(nextRun.steps))
      setWorkspaceOpen(false)
    } catch (reason: unknown) {
      if (isCurrentOperation(id)) reportError("Unable to open conversation", reason, "The conversation could not be reopened.")
    } finally {
      if (isCurrentOperation(id)) {
        openingConversation.current = false
        setConversationLoading(false)
      }
    }
  }

  async function archiveConversation(conversation: AgentConversation) {
    if (archivingConversation.current || operationBusy.current || openingConversation.current) return
    archivingConversation.current = true
    const id = operationId.current
    setArchiveLoadingNo(conversation.conversationNo)
    try {
      const archived = await archiveAgentConversation(conversation.conversationNo)
      if (!mounted.current) return
      if (!archived) throw new Error("Conversation is no longer available")
      // Invalidate searches started before the archive so they cannot restore it.
      historyRequestId.current += 1
      setHistoryRefreshing(false)
      setConversations(current => current.filter(item => item.conversationNo !== conversation.conversationNo))
      if (isCurrentOperation(id) && run?.conversationNo === conversation.conversationNo) {
        setRun(null)
        setConversationMessages([])
        setTranscriptRunNo(null)
        setTimeline([])
      }
      publishNotice({ tone: "success", title: "Conversation archived" })
    } catch (reason: unknown) {
      if (mounted.current) reportError("Unable to archive conversation", reason, "The conversation could not be archived.")
    } finally {
      archivingConversation.current = false
      if (mounted.current) setArchiveLoadingNo(null)
    }
  }

  async function rerun(action: "retry" | "regenerate") {
    if (!run?.runNo || operationBusy.current || openingConversation.current || archivingConversation.current) return
    operationBusy.current = true
    const id = ++operationId.current
    setLoading(true)
    setExecutionMode("rerun")
    setTimeline([])
    setNotice(null)
    try {
      setRun(current => current ? { ...current, status: "RUNNING" } : current)
      const nextRun = action === "retry" ? await retryAgent(run.runNo) : await regenerateAgent(run.runNo)
      if (!isCurrentOperation(id)) return
      setRun(nextRun)
      if (nextRun.modelCode?.trim()) setSelectedModelCode(nextRun.modelCode.trim())
      setTimeline(timelineFromSteps(nextRun.steps))
      await loadCompletedConversation(nextRun, id)
    } catch (reason: unknown) {
      if (!isCurrentOperation(id)) return
      const message = reportError("Unable to rerun Agent", reason, "The run could not be restarted.")
      setRun(current => current ? { ...current, status: "FAILED", errorMessage: message } : current)
    } finally {
      if (isCurrentOperation(id)) { operationBusy.current = false; setLoading(false); setExecutionMode(null) }
    }
  }

  async function exportResult() {
    if (!run?.runNo) return
    try {
      const result = await exportAgentRun(run.runNo)
      if (!mounted.current) return
      const url = URL.createObjectURL(new Blob([result.content], { type: result.contentType }))
      const anchor = document.createElement("a")
      anchor.href = url
      anchor.download = result.filename
      anchor.click()
      URL.revokeObjectURL(url)
    } catch (reason: unknown) {
      if (mounted.current) reportError("Unable to export result", reason, "The Agent result could not be exported.")
    }
  }

  function newConversation() {
    if (operationBusy.current || openingConversation.current || archivingConversation.current) return false
    operationId.current += 1
    setInput("")
    setRun(null)
    setConversationMessages([])
    setTranscriptRunNo(null)
    setTimeline([])
    setNotice(null)
    setWorkspaceOpen(false)
    return true
  }

  return {
    input, setInput, run, setRun, conversationMessages, tools, providers,
    defaultProviderCode, bindingDefaultReasoning, selectedProviderCode, setSelectedProviderCode,
    selectedModelCode, setSelectedModelCode, selectedReasoningEffort, setSelectedReasoningEffort,
    conversations, historyLoading: historyRefreshing || conversationLoading, historyRefreshing, conversationLoading,
    historyQuery, setHistoryQuery, historyError, archiveLoadingNo,
    activeRunNo, loading, canStop: loading && executionMode === "stream", workspaceOpen, setWorkspaceOpen, workspaceTab, setWorkspaceTab, timeline,
    displayedConversationMessages, runIsTerminal, showAssistantBubble, showRunActions,
    toolGroups, selectedProvider, providerDefaultReasoning, providerDisplayName, displayedModelCode,
    submit, stopAgent, openConversation, archiveConversation, rerun, exportResult, refreshConversations,
    newConversation, reloadWorkspace, configLoading, configError, notice, clearNotice: () => setNotice(null),
  }
}
