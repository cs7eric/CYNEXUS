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

export async function executeVisionScenario(scenarioCode: string, images: AiVisionImageInput[]) {
  return cynexusResult<AiVisionExecution>(await fetch(`${SERVICE_PREFIX.ai}/executions/vision`, {
    method: "POST",
    cache: "no-store",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ scenarioCode, images }),
  }))
}
