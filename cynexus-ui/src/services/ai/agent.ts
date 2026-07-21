import { cynexusResult } from "@/services/common/cynexus-result"
import { fetchWithRetry } from "@/services/common/http"
import { SERVICE_PREFIX } from "@/services/common/service-prefix"

export interface AgentRunStep {
  stepNo?: number | null
  kind?: string | null
  toolCode?: string | null
  status?: string | null
  summary?: string | null
  source?: string | null
  sourceUrl?: string | null
  dataDate?: string | null
  inputJson?: string | null
  outputJson?: string | null
  errorMessage?: string | null
}

export interface AgentRun {
  runNo?: string | null
  conversationNo?: string | null
  status?: string | null
  userInput?: string | null
  answer?: string | null
  modelCode?: string | null
  planJson?: string | null
  clarificationJson?: string | null
  evidenceJson?: string | null
  warningsJson?: string | null
  turnCount?: number | null
  toolCallCount?: number | null
  errorMessage?: string | null
  startedAt?: string | null
  finishedAt?: string | null
  steps?: AgentRunStep[] | null
}

export interface AgentConversation {
  conversationNo: string
  title?: string | null
  status?: string | null
  lastRunNo?: string | null
  lastMessage?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export interface AgentMessage {
  sequenceNo?: number | null
  role?: string | null
  content?: string | null
  toolCallId?: string | null
  toolName?: string | null
  toolCallsJson?: string | null
  createdAt?: string | null
}

export interface AgentStreamEvent {
  sequenceNo?: number | null
  type?: string | null
  runNo?: string | null
  status?: string | null
  stepNo?: number | null
  toolCode?: string | null
  summary?: string | null
  answer?: string | null
  delta?: string | null
  errorMessage?: string | null
}

export interface AgentToolDefinition {
  code: string
  name: string
  module: string
  description: string
  inputSchemaJson: string
  outputSchemaJson: string
  readOnly: boolean
  requiresConfirmation: boolean
  freshness: string
  source: string
  sourceUrl?: string | null
}

export async function runAgent(input: {
  conversationNo?: string
  idempotencyKey?: string
  userInput: string
  preferredModelCode?: string
  workspace?: string
  locale?: string
  tradeDate?: string
}): Promise<AgentRun> {
  return cynexusResult<AgentRun>(await fetchWithRetry(`${SERVICE_PREFIX.ai}/agent/runs`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input),
  }))
}

export async function getAgentRun(runNo: string): Promise<AgentRun> {
  return cynexusResult<AgentRun>(await fetchWithRetry(
    `${SERVICE_PREFIX.ai}/agent/runs/${encodeURIComponent(runNo)}`,
  ))
}

export async function fetchAgentTools(): Promise<AgentToolDefinition[]> {
  return cynexusResult<AgentToolDefinition[]>(await fetchWithRetry(`${SERVICE_PREFIX.ai}/agent/tools`))
}

export async function fetchAgentConversations(limit = 30): Promise<AgentConversation[]> {
  return cynexusResult<AgentConversation[]>(await fetchWithRetry(
    `${SERVICE_PREFIX.ai}/agent/conversations?limit=${encodeURIComponent(String(limit))}`,
  ))
}

export async function searchAgentConversations(query: string, limit = 30): Promise<AgentConversation[]> {
  return cynexusResult<AgentConversation[]>(await fetchWithRetry(
    `${SERVICE_PREFIX.ai}/agent/conversations?limit=${encodeURIComponent(String(limit))}&query=${encodeURIComponent(query)}`,
  ))
}

export async function fetchAgentMessages(conversationNo: string): Promise<AgentMessage[]> {
  return cynexusResult<AgentMessage[]>(await fetchWithRetry(
    `${SERVICE_PREFIX.ai}/agent/conversations/${encodeURIComponent(conversationNo)}/messages`,
  ))
}

export interface AgentRunExport {
  format: string
  filename: string
  contentType: string
  content: string
}

export async function exportAgentRun(runNo: string, format: "markdown" | "json" = "markdown"): Promise<AgentRunExport> {
  return cynexusResult<AgentRunExport>(await fetchWithRetry(
    `${SERVICE_PREFIX.ai}/agent/runs/${encodeURIComponent(runNo)}/export?format=${format}`,
  ))
}

export async function runAgentStream(
  input: { conversationNo?: string; idempotencyKey?: string; userInput: string; preferredModelCode?: string; workspace?: string; locale?: string; tradeDate?: string },
  onEvent: (event: AgentStreamEvent) => void,
  signal?: AbortSignal,
): Promise<AgentRun> {
  const idempotencyKey = input.idempotencyKey ?? globalThis.crypto?.randomUUID?.()
  let runNo = ""
  let afterEventNo = 0
  let completed = false
  const maxReconnects = 3

  for (let attempt = 0; attempt <= maxReconnects; attempt += 1) {
    try {
      const endpoint = runNo
        ? `${SERVICE_PREFIX.ai}/agent/runs/${encodeURIComponent(runNo)}/stream?afterEventNo=${encodeURIComponent(String(afterEventNo))}`
        : `${SERVICE_PREFIX.ai}/agent/runs/stream`
      const response = await fetch(endpoint, {
        method: runNo ? "GET" : "POST",
        headers: runNo ? { Accept: "text/event-stream" } : { "Content-Type": "application/json", Accept: "text/event-stream" },
        body: runNo ? undefined : JSON.stringify({ ...input, idempotencyKey }),
        signal,
      })
      if (!response.ok || !response.body) throw new Error(`Agent stream failed (${response.status})`)
      const result = await consumeAgentStream(response, (event) => {
        if (event.runNo) runNo = event.runNo
        if (event.sequenceNo != null) afterEventNo = Math.max(afterEventNo, event.sequenceNo)
        if (event.type === "RUN_COMPLETED") completed = true
        onEvent(event)
      })
      if (result.runNo) runNo = result.runNo
      if (completed) return getAgentRun(runNo)
      if (!runNo) throw new Error("Agent stream completed without a run id")
    } catch (reason: unknown) {
      if (signal?.aborted) throw reason
      if (!runNo || attempt >= maxReconnects) throw reason
      await new Promise((resolve) => setTimeout(resolve, 250 * (attempt + 1)))
    }
  }
  throw new Error("Agent stream reconnect failed")
}

async function consumeAgentStream(
  response: Response,
  onEvent: (event: AgentStreamEvent) => void,
): Promise<{ runNo: string }> {
  const reader = response.body?.getReader()
  if (!reader) throw new Error("Agent stream has no body")
  const decoder = new TextDecoder()
  let buffer = ""
  let runNo = ""
  const dispatch = (block: string) => {
    const data = block.split(/\r?\n/).filter((line) => line.startsWith("data:")).map((line) => line.slice(5).trim()).join("\n")
    if (!data) return
    const event = JSON.parse(data) as AgentStreamEvent
    if (event.runNo) runNo = event.runNo
    onEvent(event)
  }
  while (true) {
    const { value, done } = await reader.read()
    buffer += decoder.decode(value ?? new Uint8Array(), { stream: !done })
    const blocks = buffer.split(/\r?\n\r?\n/)
    buffer = blocks.pop() ?? ""
    blocks.forEach(dispatch)
    if (done) break
  }
  if (buffer.trim()) dispatch(buffer)
  return { runNo }
}

export async function cancelAgent(runNo: string): Promise<boolean> {
  return cynexusResult<boolean>(await fetchWithRetry(`${SERVICE_PREFIX.ai}/agent/runs/${encodeURIComponent(runNo)}/cancel`, {
    method: "POST",
  }))
}

export async function retryAgent(runNo: string): Promise<AgentRun> {
  return cynexusResult<AgentRun>(await fetchWithRetry(`${SERVICE_PREFIX.ai}/agent/runs/${encodeURIComponent(runNo)}/retry`, { method: "POST" }))
}

export async function regenerateAgent(runNo: string): Promise<AgentRun> {
  return cynexusResult<AgentRun>(await fetchWithRetry(`${SERVICE_PREFIX.ai}/agent/runs/${encodeURIComponent(runNo)}/regenerate`, { method: "POST" }))
}
