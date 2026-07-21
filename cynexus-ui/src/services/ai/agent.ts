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

export async function runAgentStream(
  input: { conversationNo?: string; idempotencyKey?: string; userInput: string; preferredModelCode?: string; workspace?: string; locale?: string; tradeDate?: string },
  onEvent: (event: AgentStreamEvent) => void,
  signal?: AbortSignal,
): Promise<AgentRun> {
  const response = await fetch(`${SERVICE_PREFIX.ai}/agent/runs/stream`, {
    method: "POST",
    headers: { "Content-Type": "application/json", Accept: "text/event-stream" },
    body: JSON.stringify(input),
    signal,
  })
  if (!response.ok || !response.body) throw new Error(`Agent stream failed (${response.status})`)
  const reader = response.body.getReader()
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
  if (!runNo) throw new Error("Agent stream completed without a run id")
  return getAgentRun(runNo)
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
