import type { AgentCitation, AgentConversation, AgentMessage, AgentRun } from "@/services/ai/agent"
import { AI_REASONING_EFFORT_PARAM, DEFAULT_AI_REASONING_EFFORT, normalizeAiReasoningEffort, type AiProviderItem, type AiReasoningEffort } from "@/services/ai/provider"

export function evidence(run: AgentRun) {
  if (!run.evidenceJson) return [] as Array<{ toolCode?: string; source?: string; sourceUrl?: string; dataDate?: string; summary?: string; citations?: AgentCitation[] }>
  try {
    const value = JSON.parse(run.evidenceJson) as unknown
    return Array.isArray(value) ? value as Array<{ toolCode?: string; source?: string; sourceUrl?: string; dataDate?: string; summary?: string; citations?: AgentCitation[] }> : []
  } catch {
    return []
  }
}

export function clarification(run: AgentRun) {
  if (run.status !== "NEEDS_CLARIFICATION" || !run.clarificationJson) return null
  try {
    const value = JSON.parse(run.clarificationJson) as { summary?: string; questions?: string[]; missingFields?: string[] }
    return value.questions?.length ? value : null
  } catch {
    return null
  }
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

export function conversationLabel(conversation: AgentConversation) {
  const source = conversation.lastMessage || conversation.title || conversation.conversationNo
  return markdownTitle(source) || conversation.title || conversation.conversationNo
}


const TERMINAL_RUN_STATUSES = new Set([
  "SUCCEEDED",
  "FAILED",
  "NEEDS_CLARIFICATION",
  "CANCELLED",
])

export function isTerminalRunStatus(status?: string | null) {
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

export function providerReasoningEffort(provider?: AiProviderItem): AiReasoningEffort {
  return normalizeAiReasoningEffort(provider?.extra?.model_reasoning_effort) || DEFAULT_AI_REASONING_EFFORT
}

export function bindingReasoningEffort(params?: Record<string, unknown>): AiReasoningEffort | null {
  return normalizeAiReasoningEffort(params?.[AI_REASONING_EFFORT_PARAM])
}

export function removeCurrentRunFromTranscript(messages: AgentMessage[], run: AgentRun | null, currentRunIsInTranscript = true) {
  const visible = messages.filter(isVisibleTranscriptMessage)
  if (!run || !currentRunIsInTranscript) return visible

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
