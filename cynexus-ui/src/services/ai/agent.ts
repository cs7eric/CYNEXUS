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
