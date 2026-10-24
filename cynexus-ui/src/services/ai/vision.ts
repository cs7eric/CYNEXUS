import { cynexusResult } from "@/services/common/cynexus-result"
import { SERVICE_PREFIX } from "@/services/common/service-prefix"

export interface AiVisionImageInput {
  fileName: string
  contentType: string
  dataUrl: string
}

export interface AiVisionExecution {
  content: string
  executionNo: string
  modelCode: string
  providerCode: string
  status: string
}

export interface AiVisionTaskSubmission {
  accepted?: boolean
  processId: string
  status: string
  pollUrl?: string | null
}

export interface AiVisionTaskSnapshot {
  processId?: string | null
  status: string
  content?: string | null
  result?: { content?: string | null } | string | null
  errorMessage?: string | null
  error?: string | null
  message?: string | null
}

export interface AiVisionTaskSummary {
  id: string
  scenarioCode: string
  idempotencyKey?: string | null
  status: string
  progress?: number | null
  attemptCount?: number | null
  maxAttempts?: number | null
  retryable?: boolean
  errorMessage?: string | null
  startedAt?: string | null
  completedAt?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export async function executeVisionScenario(scenarioCode: string, images: AiVisionImageInput[]) {
  return cynexusResult<AiVisionExecution>(await fetch(`${SERVICE_PREFIX.ai}/executions/vision`, {
    method: "POST",
    cache: "no-store",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ scenarioCode, images }),
  }))
}

/** Submit a vision request to the queue and return immediately. */
export async function enqueueVisionScenario(
  scenarioCode: string,
  images: AiVisionImageInput[],
  idempotencyKey?: string,
): Promise<AiVisionTaskSubmission> {
  const headers: HeadersInit = { "Content-Type": "application/json" }
  if (idempotencyKey) headers["Idempotency-Key"] = idempotencyKey
  return cynexusResult<AiVisionTaskSubmission>(await fetch(`${SERVICE_PREFIX.ai}/executions/vision/tasks`, {
    method: "POST",
    cache: "no-store",
    headers,
    body: JSON.stringify({ scenarioCode, images }),
  }))
}

function pollEndpoint(submission: AiVisionTaskSubmission) {
  if (submission.pollUrl) {
    if (/^https?:\/\//i.test(submission.pollUrl)) return submission.pollUrl
    if (submission.pollUrl.startsWith("/")) return `${SERVICE_PREFIX.cynexus}${submission.pollUrl}`
    return `${SERVICE_PREFIX.ai}/${submission.pollUrl.replace(/^\/+/, "")}`
  }
  return `${SERVICE_PREFIX.ai}/executions/vision/tasks/${encodeURIComponent(submission.processId)}`
}

/** Fetch one queued vision execution snapshot. */
export async function fetchVisionTask(submission: AiVisionTaskSubmission): Promise<AiVisionTaskSnapshot> {
  return cynexusResult<AiVisionTaskSnapshot>(await fetch(pollEndpoint(submission), {
    method: "GET",
    cache: "no-store",
  }))
}

export async function listVisionTasks(limit = 100): Promise<AiVisionTaskSummary[]> {
  return cynexusResult<AiVisionTaskSummary[]>(await fetch(
    `${SERVICE_PREFIX.ai}/executions/vision/tasks?limit=${encodeURIComponent(String(limit))}`,
    { method: "GET", cache: "no-store" },
  ))
}

export async function retryVisionTask(taskId: string): Promise<AiVisionTaskSummary> {
  return cynexusResult<AiVisionTaskSummary>(await fetch(
    `${SERVICE_PREFIX.ai}/executions/vision/tasks/${encodeURIComponent(taskId)}/retry`,
    { method: "POST", cache: "no-store" },
  ))
}
