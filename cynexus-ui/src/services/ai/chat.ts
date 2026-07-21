import { runAgent } from "@/services/ai/agent"

export const WORKSPACE_CHAT_CAPABILITY = "workspace_chat"

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
  const result = await runAgent({ userInput, workspace: "global-chat", locale: "zh-CN" })
  return normalizeWorkspaceChatContent(result.answer ?? result.errorMessage)
}
