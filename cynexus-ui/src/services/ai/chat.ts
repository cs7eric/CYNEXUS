import { cynexusResult } from "@/services/common/cynexus-result"
import { fetchWithRetry } from "@/services/common/http"
import { SERVICE_PREFIX } from "@/services/common/service-prefix"

export const WORKSPACE_CHAT_CAPABILITY = "workspace_chat"

interface AiExecuteResponse {
  content?: string | null
  status?: string | null
  executionNo?: string | null
}

function normalizeWorkspaceChatContent(content: string | null | undefined): string {
  const value = content?.trim() ?? ""
  const emptyResponse = "AI returned an empty response."
  if (!value) return emptyResponse

  try {
    const parsed: unknown = JSON.parse(value)
    if (typeof parsed === "string") return parsed.trim() || emptyResponse
    if (parsed && typeof parsed === "object") {
      for (const key of ["response", "content", "message", "text"] as const) {
        const response = (parsed as Record<string, unknown>)[key]
        if (typeof response === "string" && response.trim()) return response.trim()
      }
      return emptyResponse
    }
  } catch {
    // Providers may return normal plain text, which does not need decoding.
  }

  return value
}

export async function executeWorkspaceChat(userInput: string): Promise<string> {
  const result = await cynexusResult<AiExecuteResponse>(
    await fetchWithRetry(`${SERVICE_PREFIX.ai}/executions`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        scenarioCode: WORKSPACE_CHAT_CAPABILITY,
        userInput,
        variables: {},
      }),
    }),
  )

  return normalizeWorkspaceChatContent(result.content)
}
