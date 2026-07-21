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

export async function getAgentRun(runNo: string, ownerKey = "anonymous"): Promise<AgentRun> {
  return cynexusResult<AgentRun>(await fetchWithRetry(
    `${SERVICE_PREFIX.ai}/agent/runs/${encodeURIComponent(runNo)}?ownerKey=${encodeURIComponent(ownerKey)}`,
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

export async function fetchAgentMessages(conversationNo: string): Promise<AgentMessage[]> {
  return cynexusResult<AgentMessage[]>(await fetchWithRetry(
    `${SERVICE_PREFIX.ai}/agent/conversations/${encodeURIComponent(conversationNo)}/messages`,
  ))
}
