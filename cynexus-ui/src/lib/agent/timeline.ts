import type { AgentRunStep } from "@/services/ai/agent"

export type AgentTimelineEntry =
  | {
      id: string
      type: "thinking"
      text: string
      done: boolean
    }
  | {
      id: string
      type: "tool"
      stepNo?: number | null
      toolCode: string
      summary: string
      status: string
    }
  | {
      id: string
      type: "retry"
      attempt?: number | null
      summary: string
    }

export function lastMeaningfulLine(text: string) {
  const lines = text
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
  return lines.at(-1) ?? ""
}

function thinkingTextFromStep(step: AgentRunStep) {
  if (step.outputJson) {
    try {
      const parsed = JSON.parse(step.outputJson) as { content?: string }
      if (parsed?.content) return parsed.content
    } catch {
      // fall through
    }
  }
  return step.summary ?? ""
}

export function timelineFromSteps(steps?: AgentRunStep[] | null): AgentTimelineEntry[] {
  if (!steps?.length) return []
  const entries: AgentTimelineEntry[] = []
  for (const step of steps) {
    if (step.kind === "THINKING") {
      const text = thinkingTextFromStep(step)
      if (!text.trim()) continue
      entries.push({
        id: `thinking-${step.stepNo ?? entries.length}`,
        type: "thinking",
        text,
        done: true,
      })
      continue
    }
    if (step.kind === "TOOL" || step.toolCode) {
      entries.push({
        id: `tool-${step.stepNo ?? entries.length}-${step.toolCode ?? "tool"}`,
        type: "tool",
        stepNo: step.stepNo,
        toolCode: step.toolCode ?? "tool",
        summary: step.summary ?? "",
        status: step.status ?? "SUCCEEDED",
      })
    }
  }
  return entries
}

export function appendThinkingDelta(entries: AgentTimelineEntry[], delta: string): AgentTimelineEntry[] {
  if (!delta) return entries
  const next = [...entries]
  const last = next.at(-1)
  if (last?.type === "thinking" && !last.done) {
    next[next.length - 1] = { ...last, text: `${last.text}${delta}` }
    return next
  }
  next.push({
    id: `thinking-live-${Date.now()}`,
    type: "thinking",
    text: delta,
    done: false,
  })
  return next
}

export function completeThinking(entries: AgentTimelineEntry[], summary?: string | null): AgentTimelineEntry[] {
  const next = [...entries]
  const last = next.at(-1)
  if (last?.type === "thinking" && !last.done) {
    next[next.length - 1] = {
      ...last,
      text: last.text.trim() || summary || last.text,
      done: true,
    }
    return next
  }
  if (summary?.trim()) {
    next.push({
      id: `thinking-${Date.now()}`,
      type: "thinking",
      text: summary,
      done: true,
    })
  }
  return next
}

export function appendToolStep(
  entries: AgentTimelineEntry[],
  tool: { stepNo?: number | null; toolCode?: string | null; summary?: string | null; status?: string | null },
): AgentTimelineEntry[] {
  const toolCode = tool.toolCode?.trim() || "tool"
  const id = `tool-${tool.stepNo ?? Date.now()}-${toolCode}`
  const existingIndex = entries.findIndex((entry) => entry.id === id)
  if (existingIndex >= 0) {
    const next = [...entries]
    const existing = next[existingIndex]
    if (existing.type === "tool") {
      next[existingIndex] = {
        ...existing,
        stepNo: tool.stepNo ?? existing.stepNo,
        summary: tool.summary ?? existing.summary,
        status: tool.status ?? existing.status,
      }
    }
    return next
  }
  return [...entries, {
    id,
    type: "tool",
    stepNo: tool.stepNo,
    toolCode,
    summary: tool.summary ?? "",
    status: tool.status ?? "SUCCEEDED",
  }]
}

export function appendRetry(
  entries: AgentTimelineEntry[],
  retry: { sequenceNo?: number | null; summary?: string | null },
): AgentTimelineEntry[] {
  const id = `retry-${retry.sequenceNo ?? Date.now()}`
  if (entries.some((entry) => entry.id === id)) return entries
  const summary = retry.summary?.trim() || "Retrying the AI provider request."
  const attempt = summary.match(/attempt\s+(\d+)/i)?.[1]
  return [
    ...entries,
    {
      id,
      type: "retry",
      attempt: attempt ? Number(attempt) : null,
      summary,
    },
  ]
}
